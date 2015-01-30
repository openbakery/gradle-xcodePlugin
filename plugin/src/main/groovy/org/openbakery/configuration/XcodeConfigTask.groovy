package org.openbakery.configuration

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.filefilter.SuffixFileFilter
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask
import org.openbakery.CommandRunnerException
import org.openbakery.Destination
import org.openbakery.Devices
import org.testng.annotations.AfterMethod

/**
 * User: rene
 * Date: 25/11/14
 */
class XcodeConfigTask extends AbstractXcodeTask {

	boolean isOSX = false;


	XcodeConfigTask() {
		super()
		this.description = "Parses the xcodeproj file and setups the configuration for the build"
	}


	@TaskAction
	void configuration() {

		parseInfoFromProjectFile()

		if (isOSX) {
			return;
		}

		String version = commandRunner.runWithResult([project.xcodebuild.xcodebuildCommand, "-version"])
		boolean isXcode5 = version.startsWith("Xcode 5");
		logger.debug("isXcode5 {}", isXcode5);


		if (isXcode5) {
			createXcode5DeviceList()
		} else {
			createDeviceList()
		}

		logger.debug("availableSimulators: {}", project.xcodebuild.availableSimulators)

	}


	void parseInfoFromProjectFile() {
		logger.debug("using target: {}", project.xcodebuild.target)
		def projectFileDirectory = project.projectDir.list(new SuffixFileFilter(".xcodeproj"))[0]
		def xcodeProjectDir = new File(project.projectDir, projectFileDirectory) // prepend project dir to support multi-project build
		def projectFile = new File(xcodeProjectDir, "project.pbxproj")

		def buildRoot = project.buildDir
		if (!buildRoot.exists()) {
			buildRoot.mkdirs()
		}

		File projectPlistFile = new File(buildRoot, "project.plist")

		if (projectPlistFile.exists()) {
			projectPlistFile.delete()
		}

		// convert ascii plist to xml so that commons configuration can parse it!
		commandRunner.run(["plutil", "-convert", "xml1", projectFile.absolutePath, "-o", projectPlistFile.absolutePath])

		XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(projectPlistFile)
		def rootObjectKey = config.getString("rootObject")
		logger.debug("rootObjectKey {}", rootObjectKey);

		List<String> list = config.getList("objects." + rootObjectKey + ".targets")

		for (target in list) {

			def buildConfigurationList = config.getString("objects." + target + ".buildConfigurationList")
			logger.debug("buildConfigurationList={}", buildConfigurationList)
			def targetName = config.getString("objects." + target + ".name")
			logger.debug("targetName: {}", targetName)


			if (targetName.equals(project.xcodebuild.target)) {

				if (StringUtils.isEmpty(project.xcodebuild.productName)) {
					project.xcodebuild.productName = config.getString("objects." + target + ".productName")
				}
				String type = config.getString("objects." + target + ".productType")
				if (type.equalsIgnoreCase("com.apple.product-type.app-extension")) {
					project.xcodebuild.productType = "appex"
				}

				def buildConfigurations = config.getList("objects." + buildConfigurationList + ".buildConfigurations")
				for (buildConfigurationsItem in buildConfigurations) {
					def buildName = config.getString("objects." + buildConfigurationsItem + ".name")

					logger.debug("buildName: {} equals {}", buildName, project.xcodebuild.configuration)

					if (buildName.equals(project.xcodebuild.configuration)) {
						//String productName = config.getString("objects." + buildConfigurationsItem + ".buildSettings.PRODUCT_NAME")
						String sdkRoot = config.getString("objects." + buildConfigurationsItem + ".buildSettings.SDKROOT")
						if (StringUtils.isNotEmpty(sdkRoot) && sdkRoot.equalsIgnoreCase("macosx")) {
							// is os x build
							this.isOSX = true
						} else {
							String devicesString = config.getString("objects." + buildConfigurationsItem + ".buildSettings.TARGETED_DEVICE_FAMILY")

							if (devicesString.equals("1")) {
								project.xcodebuild.devices = Devices.PHONE;
							} else if (devicesString.equals("2")) {
								project.xcodebuild.devices = Devices.PAD;
							}

						}

						if (project.xcodebuild.infoPlist == null) {
							project.xcodebuild.infoPlist = config.getString("objects." + buildConfigurationsItem + ".buildSettings.INFOPLIST_FILE")
							logger.info("infoPlist: {}", project.xcodebuild.infoPlist)
						}

						logger.info("devices: {}", project.xcodebuild.devices)
						logger.info("isOSX: {}", this.isOSX)
						return;
					}
				}
			}

		}
		logger.lifecycle("WARNING: given target '" + project.xcodebuild.target + "' in the xcode project file")
	}


	void createXcode5DeviceList() {

		logger.debug("xcodePath is {}", project.xcodebuild.xcodePath);
		String xcodeDeveloperPath;
		if (project.xcodebuild.xcodePath != null) {
			xcodeDeveloperPath = project.xcodebuild.xcodePath + "/Contents/Developer";
		} else {
			xcodeDeveloperPath = commandRunner.runWithResult(["xcode-select", "-p"])
		}


		File sdksDirectory = new File(xcodeDeveloperPath, "Platforms/iPhoneSimulator.platform/Developer/SDKs")
		logger.debug("investigating sdk directory {}", sdksDirectory);
		def versions = [];
		for (String sdk in sdksDirectory.list()) {
			String basename = FilenameUtils.getBaseName(sdk)
			versions << StringUtils.removeStart(basename, "iPhoneSimulator")
		}


		File simulatorDirectory = new File(xcodeDeveloperPath, "Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks/SimulatorHost.framework/Versions/A/Resources/Devices")
		String[] simulators = simulatorDirectory.list()
		for (String simulator in simulators) {

			File infoPlistFile = new File(simulatorDirectory, simulator + "/Info.plist")
			String name = commandRunner.runWithResult([
							"/usr/libexec/PlistBuddy",
							infoPlistFile.absolutePath,
							"-c",
							"Print :displayName"
			])


			if (hasNewerEquivalentDevice(infoPlistFile)) {
				continue;
			}


			for (String version in versions) {
				Destination destination = new Destination();
				destination.platform = 'iOS Simulator'
				destination.name = name
				destination.os = version

				project.xcodebuild.availableSimulators << destination;
			}
		}
	}


	void createDeviceList() {
		String simctlCommand = commandRunner.runWithResult([project.xcodebuild.xcrunCommand, "-sdk", "iphoneos", "-find", "simctl"]);
		String simctlList = commandRunner.runWithResult([simctlCommand, "list"]);

		String iOSVersion = null
		for (String line in simctlList.split("\n")) {


			if (line.startsWith("--")) {
				String[] tokens = line.split(" ");
				if (tokens.length > 2) {
					iOSVersion = tokens[2];
				}
			} else if (iOSVersion != null) {
				// now we are in the devices section
				Destination destination = new Destination();
				destination.platform = 'iOS Simulator'
				destination.os = iOSVersion

				def pattern = ~/^\s+([^\(]+)\(([^\)]+)/
				def matcher = (line =~ pattern)
				if (matcher.getCount() && matcher[0].size() == 3) {
					destination.name = matcher[0][1].trim()
					destination.id = matcher[0][2].trim()
					project.xcodebuild.availableSimulators << destination;
				}
			}
		}
	}

	boolean hasNewerEquivalentDevice(File infoPlistFile) {
		try {
			commandRunner.runWithResult([
							"/usr/libexec/PlistBuddy",
							infoPlistFile.absolutePath,
							"-c",
							"Print :newerEquivalentDevice"
			])
			return true
			// if the "Print :newerEquivalentDevice" is found, then to not add the simulator
		} catch (CommandRunnerException ex) {
			return false
		}
	}



}
