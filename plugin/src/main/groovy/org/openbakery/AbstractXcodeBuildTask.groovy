package org.openbakery

import org.gradle.api.DefaultTask

/**
 * User: rene
 * Date: 15.07.13
 * Time: 11:57
 */
class AbstractXcodeBuildTask extends DefaultTask {

	CommandRunner commandRunner
	ProvisioningProfileIdReader provisioningProfileIdReader

	AbstractXcodeBuildTask() {
		super()
		commandRunner = new CommandRunner()
		provisioningProfileIdReader = new ProvisioningProfileIdReader()
	}

	def createCommandList() {
		def commandList = [
						"xcodebuild"
		]

		if (project.xcodebuild.scheme) {
			commandList.add("-scheme");
			commandList.add(project.xcodebuild.scheme);

			// workspace makes only sense when using scheme
			if (project.xcodebuild.workspace != null) {
				commandList.add("-workspace")
				commandList.add(project.xcodebuild.workspace)
			}

			if (project.xcodebuild.sdk != null) {
				commandList.add("-sdk")
				commandList.add(project.xcodebuild.sdk)
				if (project.xcodebuild.sdk.equals("iphonesimulator") && project.xcodebuild.arch == null) {
					commandList.add("ONLY_ACTIVE_ARCH=NO")
					commandList.add("-arch")
					commandList.add("i386")
				}
			}

			if (project.xcodebuild.configuration != null) {
				commandList.add("-configuration")
				commandList.add(project.xcodebuild.configuration)
			}


		} else {
			commandList.add("-configuration")
			commandList.add(project.xcodebuild.configuration)
			commandList.add("-sdk")
			commandList.add(project.xcodebuild.sdk)
			commandList.add("-target")
			commandList.add(project.xcodebuild.target)
		}

		if (project.xcodebuild.signing != null && project.xcodebuild.signing.identity != null) {
			commandList.add("CODE_SIGN_IDENTITY=" + project.xcodebuild.signing.identity)
		}

		if (project.xcodebuild.arch != null) {
			commandList.add("-arch")
			commandList.add(project.xcodebuild.arch)
		}


		commandList.add("DSTROOT=" + project.xcodebuild.dstRoot.absolutePath)
		commandList.add("OBJROOT=" + project.xcodebuild.objRoot.absolutePath)
		commandList.add("SYMROOT=" + project.xcodebuild.symRoot.absolutePath)
		commandList.add("SHARED_PRECOMPS_DIR=" + project.xcodebuild.sharedPrecompsDir.absolutePath)


		for (Destination destination in project.xcodebuild.destinations) {

			StringBuilder destinationBuilder = new StringBuilder();
			if (destination.platform != null) {
				destinationBuilder.append("platform=");
				destinationBuilder.append(destination.platform)
			}
			if (destination.name != null && destination.platform.startsWith("iOS")) {
				if (destinationBuilder.length() > 0) {
					destinationBuilder.append(",")
				}
				destinationBuilder.append("name=");
				destinationBuilder.append(destination.name)
			}
			if (destination.arch != null && destination.platform.equals("OS X")) {
				if (destinationBuilder.length() > 0) {
					destinationBuilder.append(",")
				}
				destinationBuilder.append("arch=");
				destinationBuilder.append(destination.arch)
			}

			if (destination.os != null && destination.platform.equals("iOS Simulator")) {
				if (destinationBuilder.length() > 0) {
					destinationBuilder.append(",")
				}
				destinationBuilder.append("os=");
				destinationBuilder.append(destination.os)
			}

			commandList.add("-destination")
			commandList.add(destinationBuilder.toString());
		}

		if (project.xcodebuild.signing.keychainPathInternal.exists()) {
			commandList.add('OTHER_CODE_SIGN_FLAGS=--keychain ' + project.xcodebuild.signing.keychainPathInternal.path);
		}

		return commandList;
	}
}
