package org.openbakery

import org.gradle.api.DefaultTask
import org.openbakery.tools.Xcode

/**
 * User: rene
 * Date: 15.07.13
 * Time: 11:57
 */
abstract class AbstractXcodeBuildTask extends DefaultTask {

	CommandRunner commandRunner
	Xcode xcode

	AbstractXcodeBuildTask() {
		super()
		commandRunner = new CommandRunner()
		xcode = new Xcode(commandRunner, project.xcodebuild.xcodeVersion)
	}

	def createCommandList() {

		def commandList = [
					project.xcodebuild.xcodebuildCommand
		]

		if (project.xcodebuild.scheme) {
			commandList.add("-scheme");
			commandList.add(project.xcodebuild.scheme);

			if (project.xcodebuild.workspace != null) {
				commandList.add("-workspace")
				commandList.add(project.xcodebuild.workspace)
			}

			if (project.xcodebuild.configuration != null) {
				commandList.add("-configuration")
				commandList.add(project.xcodebuild.configuration)
			}


		} else {
			commandList.add("-configuration")
			commandList.add(project.xcodebuild.configuration)

			if (project.xcodebuild.type == Type.OSX) {
				commandList.add("-sdk")
				commandList.add("macosx")
			}

			commandList.add("-target")
			commandList.add(project.xcodebuild.target)
		}

		if (!project.xcodebuild.isSimulatorBuildOf(Type.iOS)) {
			// disable codesign when building for OS X and iOS device
			commandList.add("CODE_SIGN_IDENTITY=")
			commandList.add("CODE_SIGNING_REQUIRED=NO")
		}

		if (project.xcodebuild.arch != null) {
			StringBuilder archs = new StringBuilder("ARCHS=");
			for (String singleArch in project.xcodebuild.arch) {
				if (archs.length() > 7) {
					archs.append(" ");
				}
				archs.append(singleArch);
			}
			commandList.add(archs.toString());

		}

		commandList.add("-derivedDataPath")
		commandList.add(project.xcodebuild.derivedDataPath.absolutePath)
		commandList.add("DSTROOT=" + project.xcodebuild.dstRoot.absolutePath)
		commandList.add("OBJROOT=" + project.xcodebuild.objRoot.absolutePath)
		commandList.add("SYMROOT=" + project.xcodebuild.symRoot.absolutePath)
		commandList.add("SHARED_PRECOMPS_DIR=" + project.xcodebuild.sharedPrecompsDir.absolutePath)



		if (project.xcodebuild.additionalParameters instanceof List) {
			for (String value in project.xcodebuild.additionalParameters) {
				commandList.add(value)
			}
		} else {
			if (project.xcodebuild.additionalParameters != null) {
				commandList.add(project.xcodebuild.additionalParameters)
			}
		}

		return commandList;
	}

/*
	String getFailureFromLog(File outputFile) {

		ReversedLinesFileReader reversedLinesFileReader = new ReversedLinesFileReader(outputFile);

		ArrayList<String> result = new ArrayList<>(100);

		for (int i=0; i<100; i++) {
			String line = reversedLinesFileReader.readLine()

			if (line == null) {
				// no more input so we are done;
				break;
			}

			result.add(line);

			if (line.startsWith("Testing failed:")) {
				break
			}

		}

		Collections.reverse(result)
		StringBuilder builder = new StringBuilder()
		for (String line : result) {
		  builder.append(line)
			builder.append("\n")
		}

		return builder.toString()
	}
	*/

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
