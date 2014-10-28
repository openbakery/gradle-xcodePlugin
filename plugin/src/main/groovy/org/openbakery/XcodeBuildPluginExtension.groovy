/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbakery

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.filefilter.SuffixFileFilter
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.openbakery.signing.Signing
import org.slf4j.Logger
import org.slf4j.LoggerFactory

enum Devices {
	UNIVERSAL,
	PHONE,
	PAD
}

class XcodeBuildPluginExtension {
	public final static KEYCHAIN_NAME_BASE = "gradle-"


	private static Logger logger = LoggerFactory.getLogger(XcodeBuildPluginExtension.class)


	String infoPlist = null
	String scheme = null
	String configuration = 'Debug'
	String sdk = 'iphonesimulator'
	String target = null
	Object dstRoot
	Object objRoot
	Object symRoot
	Object sharedPrecompsDir
	String sourceDirectory = '.'
	Signing signing = null
	def additionalParameters = null
	String bundleNameSuffix = null
	List<String> arch = null
	String workspace = null
	String version = null

	boolean isOSX = false;
	Devices devices = Devices.UNIVERSAL;
	List<Destination> availableSimulators = []

	Set<Destination> destinations = null

	private String xcodePath = null
	CommandRunner commandRunner

	/**
	 * internal parameters
	 */
	private final Project project

	public XcodeBuildPluginExtension(Project project) {
		this.project = project;
		this.signing = new Signing(project)
		commandRunner = new CommandRunner()


		this.dstRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("dst")
		}

		this.objRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("obj")
		}

		this.symRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("sym")
		}

		this.sharedPrecompsDir = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("shared")
		}

	}

	void setDstRoot(File dstRoot) {
		this.dstRoot = dstRoot
	}

	void setObjRoot(File objRoot) {
		this.objRoot = objRoot
	}

	void setSymRoot(File symRoot) {
		this.symRoot = symRoot
	}

	void setSharedPrecompsDir(File sharedPrecompsDir) {
		this.sharedPrecompsDir = sharedPrecompsDir
	}

	File getDstRoot() {
		return project.file(dstRoot)
	}

	File getObjRoot() {
		return project.file(objRoot)
	}

	File getSymRoot() {
		return project.file(symRoot)
	}

	File getSharedPrecompsDir() {
		return project.file(sharedPrecompsDir)
	}


	void signing(Closure closure) {
		ConfigureUtil.configure(closure, this.signing)
	}


	boolean isDeviceBuild() {
		return this.sdk.startsWith("iphoneos")
	}

	boolean isSimulatorBuild() {
		return this.sdk.startsWith("iphonesimulator")
	}


	void destination(Closure closure) {
		Destination destination = new Destination()
		ConfigureUtil.configure(closure, destination)
		if (destinations == null) {
			destinations = [] as Set
		}


		if (isSimulatorBuild()) {
			// filter only on simulator builds
			destinations.addAll(findMatchingDestinations(destination))
		} else {
			destinations << destination
		}
	}

	boolean matches(String first, String second) {
		if (first != null && second == null) {
			return true;
		}

		if (first == null && second != null) {
			return true;
		}

		if (first.equals(second)) {
			return true;
		}

		if (second.matches(first)) {
			return true;
		}

		return false;

	}

	List<Destination> findMatchingDestinations(Destination destination) {
		def result = [];

		logger.debug("finding matching destination for: {}", destination)

		for (Destination device in availableSimulators) {
			if (!matches(destination.platform, device.platform)) {
				//logger.debug("{} does not match {}", device.platform, destination.platform);
				continue
			}
			if (!matches(destination.name, device.name)) {
				//logger.debug("{} does not match {}", device.name, destination.name);
				continue
			}
			if (!matches(destination.arch, device.arch)) {
				//logger.debug("{} does not match {}", device.arch, destination.arch);
				continue
			}
			if (!matches(destination.id, device.id)) {
				//logger.debug("{} does not match {}", device.id, destination.id);
				continue
			}
			if (!matches(destination.os, device.os)) {
				//logger.debug("{} does not match {}", device.os, destination.os);
				continue
			}

			logger.debug("FOUND matching destination: {}", device)

			result << device

		}


		return result.asList();
	}

	List<Destination> getDestinations() {

		if (!this.destinations) {
			logger.info("There was no destination configured that matches the available. Therefor all available destinations where taken.")
			this.destinations = []
			switch (this.devices) {
				case Devices.PHONE:
					this.destinations = availableSimulators.findAll {
						d -> d.name.contains("iPhone");
					};
					break;
				case Devices.PAD:
					this.destinations = availableSimulators.findAll {
						d -> d.name.contains("iPad");
					};
					break;
				default:
					this.destinations.addAll(availableSimulators);
					break;
			}
		}

		logger.debug("this.destination: " + this.destinations);

		return this.destinations.asList();
	}

	void setArch(Object arch) {
		if (arch instanceof List) {
			logger.debug("Arch is List: " + arch + " - " + arch.getClass().getName())
			this.arch = arch;
		} else {
			logger.debug("Arch is string: " + arch + " - " + arch.getClass().getName())
			this.arch = new ArrayList<String>();
			this.arch.add(arch.toString());
		}
	}


	void createXcode5DeviceList() {

		logger.debug("xcodePath is {}", xcodePath);
		String xcodeDeveloperPath;
		if (xcodePath != null) {
			xcodeDeveloperPath = xcodePath + "/Contents/Developer";
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

				availableSimulators << destination;
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

	void createDeviceList() {
		String simctlCommand = commandRunner.runWithResult([getXcrunCommand(), "-sdk", "iphoneos", "-find", "simctl"]);
		String simctlList = commandRunner.runWithResult([simctlCommand, "list"]);

		String iOSVersion = null
		for (String line in simctlList.split("\n")) {


			if (line.startsWith("--")) {
				String[] tokens = line.split(" ");
				if (tokens.length > 2) {
					iOSVersion = tokens[2];
				}
			} else 	if (iOSVersion != null) {
				// now we are in the devices section
				Destination destination = new Destination();
				destination.platform = 'iOS Simulator'
				destination.os = iOSVersion

				def pattern = ~/^\s+([^\(]+)\(([^\)]+)/
				def matcher = ( line =~ pattern )
				if (matcher.getCount() && matcher[0].size() == 3) {
					destination.name = matcher[0][1].trim()
					destination.id = matcher[0][2].trim()
					availableSimulators << destination;
				}
			}
		}
	}

	void finishConfiguration(Project project) {

		parseInfoFromProjectFile(project)

		if (isOSX) {
			return;
		}

		String version = commandRunner.runWithResult([xcodebuildCommand		, "-version"])
		boolean isXcode5 = version.startsWith("Xcode 5");
		logger.debug("isXcode5 {}", isXcode5);


		if (isXcode5) {
			createXcode5DeviceList()
		} else {
			createDeviceList()
		}

		logger.debug("availableSimulators: {}", availableSimulators)

	}



	void parseInfoFromProjectFile(Project project) {

		logger.debug("using target: {}", this.target)
		def projectFileDirectory = project.projectDir.list(new SuffixFileFilter(".xcodeproj"))[0]
    def xcodeProjectDir = new File(project.projectDir, projectFileDirectory) // prepend project dir to support multi-project build
    def projectFile = new File(xcodeProjectDir, "project.pbxproj")

		def buildRoot = project.buildDir
		if (!buildRoot.exists()) {
			buildRoot.mkdirs()
		}

		def projectPlist = new File(buildRoot, "project.plist").absolutePath

		// convert ascii plist to xml so that commons configuration can parse it!
		commandRunner.run(["plutil", "-convert", "xml1", projectFile.absolutePath, "-o", projectPlist])

		XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(new File(projectPlist))
		def rootObjectKey = config.getString("rootObject")
		logger.debug("rootObjectKey {}", rootObjectKey);

		List<String> list = config.getList("objects." + rootObjectKey + ".targets")

		for (target in list) {

			def buildConfigurationList = config.getString("objects." + target + ".buildConfigurationList")
			logger.debug("buildConfigurationList={}", buildConfigurationList)
			def targetName = config.getString("objects." + target + ".name")
			logger.debug("targetName: {}", targetName)


			if (targetName.equals(this.target)) {
				def buildConfigurations = config.getList("objects." + buildConfigurationList + ".buildConfigurations")
				for (buildConfigurationsItem in buildConfigurations) {
					def buildName = config.getString("objects." + buildConfigurationsItem + ".name")

					logger.debug("buildName: {} equals {}", buildName, this.configuration)

					if (buildName.equals(this.configuration)) {
						//String productName = config.getString("objects." + buildConfigurationsItem + ".buildSettings.PRODUCT_NAME")
						String sdkRoot = config.getString("objects." + buildConfigurationsItem + ".buildSettings.SDKROOT")
						if (StringUtils.isNotEmpty(sdkRoot) && sdkRoot.equalsIgnoreCase("macosx")) {
							// is os x build
							this.isOSX = true
						} else {
							String devicesString = config.getString("objects." + buildConfigurationsItem + ".buildSettings.TARGETED_DEVICE_FAMILY")

							if (devicesString.equals("1")) {
								this.devices = Devices.PHONE;
							} else if (devicesString.equals("2")) {
								this.devices = Devices.PAD;
							}

						}

						if (this.infoPlist == null) {
							infoPlist = config.getString("objects." + buildConfigurationsItem + ".buildSettings.INFOPLIST_FILE")
							logger.info("infoPlist: {}", infoPlist)
						}

						logger.info("devices: {}", this.devices)
						logger.info("isOSX: {}", this.isOSX)
						return;
					}
				}
			}
		}
	}


	void setVersion(String version) {
		this.version = version
		String installedXcodes = commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode")


		for (String xcode : installedXcodes.split("\n")) {


			File xcodeBuildFile = new File(xcode, "Contents/Developer/usr/bin/xcodebuild");
			if (xcodeBuildFile.exists()) {

				String xcodeVersion = commandRunner.runWithResult(xcodeBuildFile.absolutePath, "-version");

				if (xcodeVersion.endsWith(version)) {
					xcodePath = xcode
					return
				}
			}
		}

		throw new IllegalStateException("No Xcode found with build number " + version);
	}




	String getXcodebuildCommand() {
		if (xcodePath != null) {
			return xcodePath + "/Contents/Developer/usr/bin/xcodebuild"
		}
		return "xcodebuild"
	}

	String getXcrunCommand() {
		if (xcodePath != null) {
			return xcodePath + "/Contents/Developer/usr/bin/xcrun"
		}
		return "xcrun"
	}

}