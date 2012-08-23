package org.openbakery

import org.gradle.api.tasks.TaskAction


class XcodeBuildTask extends AbstractXcodeTask {


    XcodeBuildTask() {
        super();
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
                "-target",
                project.xcodebuild.target,
                "DSTROOT=" + project.xcodebuild.dstRoot,
                "OBJROOT=" + project.xcodebuild.objRoot,
                "SYMROOT=" + project.xcodebuild.symRoot,
                "SHARED_PRECOMPS_DIR=" + project.xcodebuild.sharedPrecompsDir
        ];

        if (project.xcodebuild.signIdentity != null) {
            commandList.add("CODE_SIGN_IDENTITY=" + project.xcodebuild.signIdentity);
        }

        if (project.xcodebuild.additionalParameters != null) {
            commandList.add(project.xcodebuild.additionalParameters);
        }
/*
        if (project.xcodebuild.sdk.startsWith("iphoneos")) {
            def keychainPath = System.getProperty("user.home") + "/Library/Keychains/" + project.keychain.keychainName;
            File keychainFile = new File(keychainPath)
            if (keychainFile.exists()) {
                commandList.add("OTHER_CODE_SIGN_FLAGS=--keychain " + keychainPath);
            }
        }
*/
        runCommand(commandList);

    }


}
