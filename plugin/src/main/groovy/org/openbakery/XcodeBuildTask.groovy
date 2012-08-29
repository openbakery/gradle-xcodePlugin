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
        def commandList = [
                "xcodebuild",
                "-configuration",
                project.xcodebuild.configuration,
                "-sdk",
                project.xcodebuild.sdk,
                "-target",
                project.xcodebuild.target,
                "DSTROOT=" + new File(project.xcodebuild.dstRoot).absolutePath,
                "OBJROOT=" + new File(project.xcodebuild.objRoot).absolutePath,
                "SYMROOT=" + new File(project.xcodebuild.symRoot).absolutePath,
                "SHARED_PRECOMPS_DIR=" + new File(project.xcodebuild.sharedPrecompsDir).absolutePath
        ]

        if (project.xcodebuild.signIdentity != null) {
            commandList.add("CODE_SIGN_IDENTITY=" + project.xcodebuild.signIdentity)
        }

        if (project.xcodebuild.arch != null) {
            commandList.add("-arch")
            commandList.add(project.xcodebuild.arch)
        }
  
        if (project.xcodebuild.additionalParameters != null) {
            commandList.add(project.xcodebuild.additionalParameters)
        }
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
