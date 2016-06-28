package org.openbakery.tools

import org.openbakery.CommandRunner
import org.openbakery.Destination
import org.openbakery.Type
import org.openbakery.XcodeBuildPluginExtension
import org.openbakery.output.OutputAppender

/**
 * Created by rene on 27.06.16.
 */
class Xcodebuild {

	CommandRunner commandRunner

	String xcodePath
	Xcode xcode

	String scheme
	String target
	boolean simulator
	Type type
	String workspace
	String configuration
	File dstRoot
	File objRoot
	File symRoot
	File sharedPrecompsDir
	File derivedDataPath
	List<String> arch
	def additionalParameters

	List<Destination> destinations



	public Xcodebuild(CommandRunner commandRunner, Xcode xcode, XcodeBuildPluginExtension extension) {
		this.commandRunner = commandRunner
		this.xcode = xcode

		scheme = extension.scheme
		target = extension.target
		simulator = extension.simulator
		type = extension.type
		workspace = extension.workspace
		configuration = extension.configuration
		dstRoot = extension.dstRoot
		objRoot = extension.objRoot
		symRoot = extension.symRoot
		sharedPrecompsDir = extension.sharedPrecompsDir
		derivedDataPath = extension.derivedDataPath
		destinations = extension.availableDestinations
		arch = extension.arch
		additionalParameters = extension.additionalParameters
	}

	def validateParameters(String directory, OutputAppender outputAppender, Map<String, String> environment) {
		if (directory == null) {
			throw new IllegalArgumentException("directory must not be null");
		}

		if (outputAppender == null) {
			throw new IllegalArgumentException("outputAppender must not be null");
		}

		if (scheme == null && target == null) {
			throw new IllegalArgumentException("No 'scheme' or 'target' specified, so do not know what to build");
		}
	}

	def execute(String directory, OutputAppender outputAppender, Map<String, String> environment) {
		validateParameters(directory, outputAppender, environment)
		def commandList = []
		addBuildSettings(commandList)
		addDestinationSettingsForBuild(commandList)
		commandRunner.run(directory, commandList, environment, outputAppender)
	}


	def executeTest(String directory, OutputAppender outputAppender, Map<String, String> environment) {
		validateParameters(directory, outputAppender, environment)
		def commandList = []
		commandList << 'script' << '-q' << '/dev/null'
		addBuildSettings(commandList)
		addDestinationSettingsForTest(commandList)
		addCoverageSettings(commandList)
		commandList << "test"
		commandRunner.run(directory, commandList, environment, outputAppender)
	}

	def addBuildSettings(ArrayList commandList) {

		commandList << xcode.xcodebuild

		if (scheme) {
			commandList.add("-scheme");
			commandList.add(scheme);

			if (workspace != null) {
				commandList.add("-workspace")
				commandList.add(workspace)
			}

			if (configuration != null) {
				commandList.add("-configuration")
				commandList.add(configuration)
			}


		} else {
			commandList.add("-configuration")
			commandList.add(configuration)

			if (type == Type.OSX) {
				commandList.add("-sdk")
				commandList.add("macosx")
			}

			commandList.add("-target")
			commandList.add(target)
		}

		if (!isSimulatorBuildOf(Type.iOS)) {
			// disable codesign when building for OS X and iOS device
			commandList.add("CODE_SIGN_IDENTITY=")
			commandList.add("CODE_SIGNING_REQUIRED=NO")
		}

		if (arch != null) {
			StringBuilder archs = new StringBuilder("ARCHS=");
			for (String singleArch in arch) {
				if (archs.length() > 7) {
					archs.append(" ");
				}
				archs.append(singleArch);
			}
			commandList.add(archs.toString());
		}


		commandList.add("-derivedDataPath")
		commandList.add(derivedDataPath.absolutePath)
		commandList.add("DSTROOT=" + dstRoot.absolutePath)
		commandList.add("OBJROOT=" + objRoot.absolutePath)
		commandList.add("SYMROOT=" + symRoot.absolutePath)
		commandList.add("SHARED_PRECOMPS_DIR=" + sharedPrecompsDir.absolutePath)


		if (additionalParameters instanceof List) {
			for (String value in additionalParameters) {
				commandList.add(value)
			}
		} else {
			if (additionalParameters != null) {
				commandList.add(additionalParameters)
			}
		}
	}


	def addDestinationSettingsForBuild(ArrayList commandList) {
		if (isSimulatorBuildOf(Type.iOS)) {
			Destination destination = destinations.last()
			commandList.add("-destination")
			commandList.add(getDestinationCommandParameter(destination))
		}
		if (type == Type.OSX) {
			commandList.add("-destination")
			commandList.add("platform=OS X,arch=x86_64")
		}
	}

	def addDestinationSettingsForTest(ArrayList commandList) {
		if (isSimulatorBuildOf(Type.iOS)) {
			destinations.each { destination ->
				commandList.add("-destination")
				commandList.add(getDestinationCommandParameter(destination))
			}
		}
		if (type == Type.OSX) {
			commandList.add("-destination")
			commandList.add("platform=OS X,arch=x86_64")
		}
	}


	void addCoverageSettings(ArrayList commandList) {
		if (xcode.version.major < 7) {
			commandList.add("GCC_INSTRUMENT_PROGRAM_FLOW_ARCS=YES")
			commandList.add("GCC_GENERATE_TEST_COVERAGE_FILES=YES")
		} else {
			commandList.add("-enableCodeCoverage")
			commandList.add("yes")
		}
	}


	boolean isSimulatorBuildOf(Type expectedType) {
		if (type != expectedType) {
			return false;
		}
		return simulator;
	}


	protected String getDestinationCommandParameter(Destination destination) {
		def destinationParameters = []

		if (destination.platform != null) {
			destinationParameters << "platform=" + destination.platform
		}
		if (destination.id != null) {
			destinationParameters << "id=" + destination.id
		} else {
			if (destination.name != null) {
				destinationParameters << "name=" + destination.name
			}
			if (destination.arch != null && destination.platform.equals("OS X")) {
				destinationParameters << "arch=" + destination.arch
			}

			if (destination.os != null && destination.platform.equals("iOS Simulator")) {
				destinationParameters << "OS=" + destination.os
			}
		}
		return destinationParameters.join(",")
	}
}
