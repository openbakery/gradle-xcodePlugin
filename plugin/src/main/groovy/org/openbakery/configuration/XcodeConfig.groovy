package org.openbakery.configuration

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.filefilter.SuffixFileFilter
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.Destination
import org.openbakery.XcodePlugin
import org.openbakery.XcodeProjectFile
import org.openbakery.internal.XcodeBuildSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * User: rene
 * Date: 25/11/14
 */
class XcodeConfig {

	private static Logger logger = LoggerFactory.getLogger(XcodeConfig.class)

	XcodeProjectFile xcodeProjectFile;

	Project project
	XcodeBuildSpec buildSpec
	CommandRunner commandRunner

	XcodeConfig(Project project, XcodeBuildSpec buildSpec) {
		super()
		this.project = project;
		this.buildSpec = buildSpec;
		commandRunner = new CommandRunner()
	}


	void configuration() {

		String[] projectFileList = project.projectDir.list(new SuffixFileFilter(".xcodeproj"))
		if (projectFileList.length == 0) {
			throw new Exception("No project file found in " + project.projectDir)
		}
		def xcodeProjectDir = new File(project.projectDir, projectFileList[0]) // prepend project dir to support multi-project build
		def projectFile = new File(xcodeProjectDir, "project.pbxproj")

		xcodeProjectFile = new XcodeProjectFile(projectFile, this.project.buildDir,  this.buildSpec);
		xcodeProjectFile.parse()


		if (xcodeProjectFile.isOSX) {
			return;
		}

		String version = commandRunner.runWithResult([project.xcodebuild.xcodebuildCommand, "-version"])
		boolean isXcode5 = version.startsWith("Xcode 5");
		logger.debug("isXcode5 {}", isXcode5);


		if (this.buildSpec.isSdk(XcodePlugin.SDK_IPHONESIMULATOR)) {

			if (isXcode5) {
				createXcode5DeviceList()
			} else {
				createDeviceList()
			}

			logger.debug("availableSimulators: {}", project.xcodebuild.availableSimulators)
		}
	}

	void createXcode5DeviceList() {

		//logger.debug("xcodePath is {}", project.xcodebuild.xcodePath);
		String xcodeDeveloperPath = project.xcodebuild.xcodePath + "/Contents/Developer";


		File sdksDirectory = new File(xcodeDeveloperPath, "Platforms/iPhoneSimulator.platform/Developer/SDKs")
		logger.debug("investigating sdk directory {}", sdksDirectory);
		def versions = [];
		for (String sdk in sdksDirectory.list()) {
			String basename = FilenameUtils.getBaseName(sdk)
			versions << StringUtils.removeStartIgnoreCase(basename, XcodePlugin.SDK_IPHONESIMULATOR)
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
		String simctlCommand = commandRunner.runWithResult([project.xcodebuild.xcrunCommand, "-sdk", XcodePlugin.SDK_IPHONEOS, "-find", "simctl"]);
		String simctlList = commandRunner.runWithResult([simctlCommand, "list"]);

		String iOSVersion = null
		for (String line in simctlList.split("\n")) {


			if (line.startsWith("--")) {
				String[] tokens = line.split(" ");
				if (tokens.length > 2) {
					if (tokens[1].equalsIgnoreCase("iOS")) {
						iOSVersion = tokens[2]
					} else {
						iOSVersion = null
					}

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
