package org.openbakery

import groovy.transform.CompileStatic
import org.gradle.api.tasks.*
import org.openbakery.util.PathHelper
import org.openbakery.xcode.Xcodebuild

@CompileStatic
class XcodeBuildArchiveTaskIosAndTvOS extends AbstractXcodeBuildTask {

    public static final String NAME = "archive"

    XcodeBuildArchiveTaskIosAndTvOS() {
        super()

        dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
        dependsOn(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)
        dependsOn(PrepareXcodeArchivingTask.NAME)

        this.description = "Prepare the app bundle that it can be archive"
    }

    @InputFile
    File getXcConfigFile() {
        return new File(PathHelper.resolveArchiveFolder(project),
                PrepareXcodeArchivingTask.FILE_NAME)
    }

    @Input
    String getScheme() {
        return getXcodeExtension().scheme
    }

    @OutputFile
    File getOutputTextFile() {
        return new File(project.getBuildDir(), "xcodebuild-archive-output.txt")
    }

    @OutputDirectory
    File getOutputDirectory() {
        File archiveDirectory = new File(PathHelper.resolveArchiveFolder(project),
                getScheme() + ".xcarchive")
        archiveDirectory.mkdirs()
        return archiveDirectory
    }

    @TaskAction
    private void archive() {
        Xcodebuild xcodeBuild = new Xcodebuild(project.projectDir,
                commandRunner,
                xcode,
                parameters,
                getDestinations())

        commandRunner.setOutputFile(getOutputTextFile())

        xcodeBuild.archive(getScheme(),
                getOutputDirectory(),
                getXcConfigFile())
    }
}
