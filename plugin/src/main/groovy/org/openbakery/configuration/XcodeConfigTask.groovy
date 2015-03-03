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
import org.openbakery.XcodeProjectFile
import org.testng.annotations.AfterMethod
import org.openbakery.AppExtension;


/**
 * User: rene
 * Date: 25/11/14
 */
class XcodeConfigTask extends AbstractXcodeTask {


	private XcodeProjectFile xcodeProjectFile;

	XcodeConfigTask() {
		super()
		this.description = "Parses the xcodeproj file and setups the configuration for the build"
	}


	@TaskAction
	void configuration() {


		def projectFileDirectory = project.projectDir.list(new SuffixFileFilter(".xcodeproj"))[0]
		def xcodeProjectDir = new File(project.projectDir, projectFileDirectory) // prepend project dir to support multi-project build
		def projectFile = new File(xcodeProjectDir, "project.pbxproj")

		xcodeProjectFile = new XcodeProjectFile(project, projectFile)
		xcodeProjectFile.parse()


		if (xcodeProjectFile.isOSX) {
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



	void createXcode5DeviceList() {

		//logger.debug("xcodePath is {}", project.xcodebuild.xcodePath);
		String xcodeDeveloperPath = project.xcodebuild.xcodePath + "/Contents/Developer";


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
