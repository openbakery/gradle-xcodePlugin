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

	XcodebuildParameters parameters
	List<Destination> destinations




	public Xcodebuild(CommandRunner commandRunner, Xcode xcode, XcodebuildParameters parameters, 	List<Destination> destinations) {
		this.commandRunner = commandRunner
		this.xcode = xcode
		this.parameters = parameters
		this.destinations = destinations
	}

	def validateParameters(String directory, OutputAppender outputAppender, Map<String, String> environment) {
		if (directory == null) {
			throw new IllegalArgumentException("directory must not be null");
		}

		if (outputAppender == null) {
			throw new IllegalArgumentException("outputAppender must not be null");
		}

		if (parameters.scheme == null && parameters.target == null) {
			throw new IllegalArgumentException("No 'scheme' or 'target' specified, so do not know what to build");
		}
	}

	def execute(String directory, OutputAppender outputAppender, Map<String, String> environment) {
		validateParameters(directory, outputAppender, environment)
		def commandList = []
		addBuildSettings(commandList)
		addDisableCodeSigning(commandList)
		addAdditionalParameters(commandList)
		addBuildPath(commandList)
		addDestinationSettingsForBuild(commandList)
		commandRunner.run(directory, commandList, environment, outputAppender)
	}


	def executeTest(String directory, OutputAppender outputAppender, Map<String, String> environment) {
		validateParameters(directory, outputAppender, environment)
		def commandList = []
		commandList << 'script' << '-q' << '/dev/null'
		addBuildSettings(commandList)
		addDisableCodeSigning(commandList)
		addDestinationSettingsForTest(commandList)
		addAdditionalParameters(commandList)
		addBuildPath(commandList)
		addCoverageSettings(commandList)
		commandList << "test"
		commandRunner.run(directory, commandList, environment, outputAppender)
	}

	def executeArchive(String directory, OutputAppender outputAppender, Map<String, String> environment, String archivePath) {
		validateParameters(directory, outputAppender, environment)
		def commandList = []
		addBuildSettings(commandList)
		addDisableCodeSigning(commandList)
		addAdditionalParameters(commandList)
		addBuildPath(commandList)
		addDestinationSettingsForBuild(commandList)
		commandList << "archive"
		commandList << '-archivePath'
		commandList << archivePath
		commandRunner.run(directory, commandList, environment, outputAppender)
	}


	def addBuildSettings(ArrayList commandList) {

		commandList << xcode.xcodebuild

		if (parameters.scheme) {
			commandList.add("-scheme");
			commandList.add(parameters.scheme);

			if (parameters.workspace != null) {
				commandList.add("-workspace")
				commandList.add(parameters.workspace)
			}

			if (parameters.configuration != null) {
				commandList.add("-configuration")
				commandList.add(parameters.configuration)
			}


		} else {
			commandList.add("-configuration")
			commandList.add(parameters.configuration)

			if (parameters.type == Type.OSX) {
				commandList.add("-sdk")
				commandList.add("macosx")
			}

			commandList.add("-target")
			commandList.add(parameters.target)
		}


	}


	def addDisableCodeSigning(ArrayList commandList) {
		if (!isSimulatorBuildOf(Type.iOS)) {
			// disable codesign when building for OS X and iOS device
			commandList.add("CODE_SIGN_IDENTITY=")
			commandList.add("CODE_SIGNING_REQUIRED=NO")
		}
	}

	def addBuildPath(ArrayList commandList) {
		if (parameters.scheme) {
			// add this parameter only if a scheme is set
			commandList.add("-derivedDataPath")
			commandList.add(parameters.derivedDataPath.absolutePath)
		}
		commandList.add("DSTROOT=" + parameters.dstRoot.absolutePath)
		commandList.add("OBJROOT=" + parameters.objRoot.absolutePath)
		commandList.add("SYMROOT=" + parameters.symRoot.absolutePath)
		commandList.add("SHARED_PRECOMPS_DIR=" + parameters.sharedPrecompsDir.absolutePath)
	}

	def addAdditionalParameters(ArrayList commandList) {
		if (parameters.arch != null) {
			StringBuilder archs = new StringBuilder("ARCHS=");
			for (String singleArch in parameters.arch) {
				if (archs.length() > 7) {
					archs.append(" ");
				}
				archs.append(singleArch);
			}
			commandList.add(archs.toString());
		}

		if (parameters.additionalParameters instanceof List) {
			for (String value in parameters.additionalParameters) {
				commandList.add(value)
			}
		} else {
			if (parameters.additionalParameters != null) {
				commandList.add(parameters.additionalParameters)
			}
		}
	}


	def addDestinationSettingsForBuild(ArrayList commandList) {
		if (isSimulatorBuildOf(Type.iOS)) {
			Destination destination = this.destinations.last()
			commandList.add("-destination")
			commandList.add(getDestinationCommandParameter(destination))
		}
		if (parameters.type == Type.OSX) {
			commandList.add("-destination")
			commandList.add("platform=OS X,arch=x86_64")
		}
	}

	def addDestinationSettingsForTest(ArrayList commandList) {
		switch (parameters.type) {
			case Type.iOS:
				this.destinations.each { destination ->
					commandList.add("-destination")
					commandList.add(getDestinationCommandParameter(destination))
				}
				break;

			case Type.OSX:
				commandList.add("-destination")
				commandList.add("platform=OS X,arch=x86_64")
				break;

			default:
				break;

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
		if (parameters.type != expectedType) {
			return false;
		}
		if (parameters.type != Type.OSX) {
			// os x does not have a simulator
			return parameters.simulator
		}
		return false;
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
			if (destination.arch != null && parameters.availableDestinations.platform.equals("OS X")) {
				destinationParameters << "arch=" + destination.arch
			}

			if (destination.os != null && parameters.availableDestinations.platform.equals("iOS Simulator")) {
				destinationParameters << "OS=" + destination.os
			}
		}
		return destinationParameters.join(",")
	}


	@Override
	public String toString() {
		return "Xcodebuild{" +
						"xcodePath='" + xcodePath + '\'' +
						parameters +
						'}';
	}
}
