package org.openbakery

import org.gradle.api.tasks.TaskAction
import java.io.File

class XcodeBuildTask extends AbstractXcodeTask {


	XcodeBuildTask() {
		super()
		this.description = "Builds the Xcode project"
	}

	@TaskAction
	def xcodebuild() {


        if (project.xcodebuild.scheme == null && project.xcodebuild.target == null) {
            throw new IllegalArgumentException("No 'scheme' or 'target' specified, so do not know what to build");
        }

        def commandList = [
                "xcodebuild"
        ]

        if (project.xcodebuild.scheme) {
            commandList.add("-scheme");
            commandList.add(project.xcodebuild.scheme);
        } else {
            commandList.add("-configuration")
            commandList.add(project.xcodebuild.configuration)
            commandList.add("-sdk")
            commandList.add(project.xcodebuild.sdk)
            commandList.add("-target")
            commandList.add(project.xcodebuild.target)
        }

		if (project.xcodebuild.signIdentity != null) {
			commandList.add("CODE_SIGN_IDENTITY=" + project.xcodebuild.signIdentity)
		}

		if (project.xcodebuild.arch != null) {
			commandList.add("-arch")
			commandList.add(project.xcodebuild.arch)
		}


		if (project.xcodebuild.additionalParameters instanceof List) {
			for (String value in project.xcodebuild.additionalParameters) {
				commandList.add(value)
			}
		} else {
			if (project.xcodebuild.additionalParameters != null) {
				commandList.add(project.xcodebuild.additionalParameters)
			}
		}

		def uuid = getProvisioningProfileId()
		if (uuid != null) {
			commandList.add("PROVISIONING_PROFILE=" + uuid);
		}
		commandList.add("DSTROOT=" + new File(project.xcodebuild.dstRoot).absolutePath)
		commandList.add("OBJROOT=" + new File(project.xcodebuild.objRoot).absolutePath)
		commandList.add("SYMROOT=" + new File(project.xcodebuild.symRoot).absolutePath)
		commandList.add("SHARED_PRECOMPS_DIR=" + new File(project.xcodebuild.sharedPrecompsDir).absolutePath)

/*
                if (project.xcodebuild.sdk.startsWith("iphoneos")) {
                        def keychainPath = System.getProperty("user.home") + "/Library/Keychains/" + project.keychain.keychainName
                        File keychainFile = new File(keychainPath)
                        if (keychainFile.exists()) {
                                commandList.add("OTHER_CODE_SIGN_FLAGS=--keychain " + keychainPath)
                        }
                }
*/
		runCommand(commandList)

	}


}
