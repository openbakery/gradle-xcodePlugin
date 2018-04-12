package org.openbakery.carthage

import org.gradle.api.tasks.*
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.AbstractXcodeTask
import org.openbakery.output.ConsoleOutputAppender
import org.openbakery.xcode.Type

@CacheableTask
class CarthageUpdateTask extends AbstractXcodeTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    File cartFile = project.rootProject.file("Cartfile")

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    File cartFileResolved

    @OutputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    File carthageDirectory

    public CarthageUpdateTask() {
        super()
        setDescription "Installs the carthage dependencies for the given project"
    }

    @TaskAction
    void update() {
        cartFile = project.rootProject.file("Cartfile")
        cartFileResolved = project.rootProject.file("Cartfile.resolved")
        carthageDirectory = project.rootProject.file("Carthage")

        println "cartFile : " + cartFile.absolutePath
        println "carthageDirectory : " + carthageDirectory.absolutePath

        def carthagePlatform = getCarthagePlatform()
        logger.info('Update Carthage for platform ' + carthagePlatform)

        File platformDirectory = carthagePlatform != 'all' ? new File(new File(carthageDirectory, 'Build'), carthagePlatform) : null
        if (carthageDirectory.exists() && (platformDirectory != null && platformDirectory.exists())) {
            logger.info('Skip Carthage update')
            return
        }

        def carthageCommand = getCarthageCommand()

        def output = services.get(StyledTextOutputFactory).create(CarthageUpdateTask)
        commandRunner.run(
                project.projectDir.absolutePath,
                [carthageCommand, "update", "--platform", carthagePlatform, "--cache-builds"],
                new ConsoleOutputAppender(output))
    }

    String getCarthagePlatform() {
        switch (project.xcodebuild.type) {
            case Type.iOS: return 'iOS'
            case Type.tvOS: return 'tvOS'
            case Type.macOS: return 'Mac'
            case Type.watchOS: return 'watchOS'
            default: return 'all'
        }
    }

    String getCarthageCommand() {
        try {
            return commandRunner.runWithResult("which", "carthage")
        } catch (CommandRunnerException) {
            // ignore, because try again with full path below

        }

        try {
            def fullPath = "/usr/local/bin/carthage"
            commandRunner.runWithResult("ls", fullPath)
            return fullPath
        } catch (CommandRunnerException) {
            // ignore, because blow an exception is thrown
        }
        throw new IllegalStateException("The carthage command was not found. Make sure that Carthage is installed")
    }

    boolean hasCartfile() {
        File cartfile = new File(project.projectDir, "Cartfile")
        return cartfile.exists()
    }
}
