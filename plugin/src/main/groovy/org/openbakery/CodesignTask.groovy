package org.openbakery

import org.gradle.api.tasks.TaskAction

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 22.08.12
 * Time: 15:07
 * To change this template use File | Settings | File Templates.
 */
class CodesignTask extends AbstractXcodeTask {

    CodesignTask() {
        super()
        dependsOn("keychain-create");
        dependsOn("provisioning-install");
        dependsOn("xcodebuild");
        this.description = "Signs the app bundle that was created by xcodebuild"
    }



    @TaskAction
    def codesign() {


        if (!project.xcodebuild.sdk.startsWith("iphoneos")) {
            throw new IllegalArgumentException("Can only sign 'iphoneos' builds but the given sdk is '" + project.xcodebuild.sdk + "'");
        }

        if (project.xcodebuild.signIdentity == null) {
            throw new IllegalArgumentException("cannot signed with unknown signidentity");
        }

        println project.xcodebuild.symRoot
        def buildOutputDirectory = new File(project.xcodebuild.symRoot + "/" + project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)
        def fileList = buildOutputDirectory.list(
                [accept: {d, f -> f ==~ /.*app/ }] as FilenameFilter
        ).toList();
        if (fileList.count == 0) {
            throw new IllegalStateException("No App Found in directory " + buildOutputDirectory.absolutePath);
        }
        def appName = buildOutputDirectory.absolutePath + "/" + fileList[0];
        def ipaName = appName.substring(0, appName.size()-4) + ".ipa"
        println "Signing " + appName + " to create " + ipaName;

        def commandList = [
            "xcrun",
            "-sdk",
            project.xcodebuild.sdk,
            "PackageApplication",
            "-v",
            appName,
            "-o",
            ipaName,
            "--sign",
            project.xcodebuild.signIdentity,
            "--embed",
            project.provisioning.mobileprovisionFile
        ]
/*
        if [ ! $CODESIGN_ALLOCATE ];
        then
        export CODESIGN_ALLOCATE=$(xcrun -find codesign_allocate)
        fi
        */

        def codesignAllocateCommand = runCommandWithResult(["xcrun", "-find", "codesign_allocate"])
        def environment = [CODESIGN_ALLOCATE:codesignAllocateCommand]
        runCommand(".", commandList, environment);
    }
}
