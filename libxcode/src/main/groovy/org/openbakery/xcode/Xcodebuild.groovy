package org.openbakery.xcode

import org.openbakery.CommandRunner
import org.openbakery.output.OutputAppender

class Xcodebuild {

	CommandRunner commandRunner

	HashMap<String, String> buildSettings = null

	File projectDirectory
	String xcodePath
	Xcode xcode
	XcodebuildParameters parameters
	List<Destination> destinations



	public Xcodebuild(File projectDirectory, CommandRunner commandRunner, Xcode xcode, XcodebuildParameters parameters, List<Destination> destinations) {
		this.projectDirectory = projectDirectory
		this.commandRunner = commandRunner
		this.xcode = xcode
		this.parameters = parameters
		this.destinations = destinations
	}

	def validateParameters(OutputAppender outputAppender, Map<String, String> environment) {
		if (this.projectDirectory == null) {
			throw new IllegalArgumentException("projectDirectory must not be null");
		}

		if (outputAppender == null) {
			throw new IllegalArgumentException("outputAppender must not be null");
		}

		if (parameters.scheme == null && parameters.target == null) {
			throw new IllegalArgumentException("No 'scheme' or 'target' specified, so do not know what to build");
		}
	}

	def execute(OutputAppender outputAppender, Map<String, String> environment) {
		validateParameters(outputAppender, environment)
		def commandList = []
		addBuildSettings(commandList)
		addDisableCodeSigning(commandList)
		addAdditionalParameters(commandList)
		addBuildPath(commandList)
		addDestinationSettingsForBuild(commandList)

		commandRunner.run(projectDirectory.absolutePath, commandList, environment, outputAppender)
	}

	def commandListForTest(OutputAppender outputAppender, Map<String, String> environment) {
		validateParameters(outputAppender, environment)
		def commandList = []
		commandList << 'script' << '-q' << '/dev/null'
		addBuildSettings(commandList)
		addDisableCodeSigning(commandList)
		addDestinationSettingsForTest(commandList)
		addAdditionalParameters(commandList)
		addBuildPath(commandList)
		addCoverageSettings(commandList)

		if (xcode.version.major > 9) {
			commandList << "-disable-concurrent-destination-testing"
		}

		return commandList
	}

	def executeTest(OutputAppender outputAppender, Map<String, String> environment) {
		def commandList = commandListForTest(outputAppender, environment)
		commandList << "test"
		commandRunner.run(this.projectDirectory.absolutePath, commandList, environment, outputAppender)
	}

	def executeBuildForTesting(OutputAppender outputAppender, Map<String, String> environment) {
		def commandList = commandListForTest(outputAppender, environment)
		commandList << "build-for-testing"
		commandRunner.run(this.projectDirectory.absolutePath, commandList, environment, outputAppender)
	}

	def executeTestWithoutBuilding(OutputAppender outputAppender, Map<String, String> environment) {

		parameters.xctestrun.each {
			def commandList = []
			commandList << 'script' << '-q' << '/dev/null'
			commandList << xcode.xcodebuild
			addDestinationSettingsForTest(commandList)
			commandList.add("-derivedDataPath")
			commandList.add(parameters.derivedDataPath.absolutePath)
			addAdditionalParameters(commandList)
			addCoverageSettings(commandList)

			commandList << "-xctestrun" << it.absolutePath
			commandList << "test-without-building"
			commandRunner.run(this.projectDirectory.absolutePath, commandList, environment, outputAppender)


		}

	}

	def executeArchive(OutputAppender outputAppender, Map<String, String> environment, String archivePath) {
		validateParameters(outputAppender, environment)
		def commandList = []
		addBuildSettings(commandList)
		addDisableCodeSigning(commandList)
		addAdditionalParameters(commandList)
		addBuildPath(commandList)
		addDestinationSettingsForBuild(commandList)
		commandList << "archive"
		commandList << '-archivePath'
		commandList << archivePath
		commandRunner.run(this.projectDirectory.absolutePath, commandList, environment, outputAppender)
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

			if (parameters.type == Type.macOS) {
				commandList.add("-sdk")
				commandList.add("macosx")
			}

			commandList.add("-target")
			commandList.add(parameters.target)
		}

		if (parameters.bitcode) {
			commandList.add('OTHER_CFLAGS="-fembed-bitcode"')
			commandList.add('BITCODE_GENERATION_MODE=bitcode')
		}
	}

	def addDisableCodeSigning(ArrayList commandList) {
		if (parameters.type != Type.macOS && parameters.simulator) {
			// if it is a simulator build then do not add the parameters below
			return
		}

		commandList.add("CODE_SIGN_IDENTITY=")
		commandList.add("CODE_SIGNING_REQUIRED=NO")
		//commandList.add("CODE_SIGN_ENTITLEMENTS=")
		//commandList.add("CODE_SIGNING_ALLOWED=NO")
	}

	private boolean isSimulator() {
		isSimulatorBuildOf(Type.iOS) || isSimulatorBuildOf(Type.tvOS)
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
		if (isSimulator()) {
			Destination destination = this.destinations.last()
			commandList.add("-destination")
			commandList.add(getDestinationCommandParameter(destination))
		}
		if (parameters.type == Type.macOS) {
			commandList.add("-destination")
			commandList.add("platform=OS X,arch=x86_64")
		}
	}

	def addDestinationSettingsForTest(ArrayList commandList) {
		switch (parameters.type) {
			case Type.iOS:
			case Type.tvOS:
				this.destinations.each { destination ->
					commandList.add("-destination")
					commandList.add(getDestinationCommandParameter(destination))
				}
				break;

			case Type.macOS:
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
			return false
		}
		if (parameters.type != Type.macOS) {
			// os x does not have a simulator
			return parameters.simulator
		}
		return false
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
			/*
			if (destination.arch != null && parameters.availableDestinations.platform.equals("OS X")) {
				destinationParameters << "arch=" + destination.arch
			}

			if (destination.os != null && parameters.availableDestinations.platform.equals("iOS Simulator")) {
				destinationParameters << "OS=" + destination.os
			}
			*/
		}
		return destinationParameters.join(",")
	}


	String getToolchainDirectory() {
		String buildSetting = getBuildSetting("TOOLCHAIN_DIR")
		if (buildSetting != null) {
			return buildSetting
		}
		return "/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain"
	}

	String getPlatformDirectory() {
		return getBuildSetting("PLATFORM_DIR")
	}

	String getWatchOSPlatformDirectory() {
		File platformsDirectory = new File(platformDirectory).getParentFile()
		File platformDirectory = new File(platformsDirectory, "WatchOS.platform")
		return platformDirectory.getAbsolutePath()
	}

	String loadBuildSettings() {
		def commandList = [xcode.xcodebuild, "clean", "-showBuildSettings"]

		if (parameters.scheme != null && parameters.workspace != null) {
			commandList.add("-scheme");
			commandList.add(parameters.scheme)
			commandList.add("-workspace")
			commandList.add(parameters.workspace)
		}

		return commandRunner.runWithResult(this.projectDirectory.absolutePath, commandList)
	}

	private String getBuildSetting(String key) {
		if (buildSettings == null) {
			buildSettings = new HashMap<>()
			String[] buildSettingsData = loadBuildSettings().split("\n")
			for (line in buildSettingsData) {
				int index = line.indexOf("=")
				if (index > 0) {
					String settingsKey = line.substring(0, index).trim()
					String settingsValue = line.substring(index + 1, line.length()).trim()
					buildSettings.put(settingsKey, settingsValue)
				}
			}
		}
		return buildSettings.get(key)
	}

	@Override
	public String toString() {
		return "Xcodebuild{" +
				"xcodePath='" + xcodePath + '\'' +
				parameters +
				'}'
	}
}
