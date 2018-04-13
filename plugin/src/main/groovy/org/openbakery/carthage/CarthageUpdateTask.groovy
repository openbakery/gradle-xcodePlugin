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
    File cartFileResolved = project.rootProject.file("Cartfile.resolved")

    @OutputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    File carthageDirectory = project.rootProject.file("Carthage")

    static final String CARTHAGE_USR_BIN_PATH = "/usr/local/bin/carthage"
    static final String ACTION_UPDATE = "update"
    static final String ARG_PLATFORM = "--platform"
    static final String ARG_CACHE_BUILDS = "--cache-builds"
    static final String CARTHAGE_PLATFORM_IOS = "iOS"
    static final String CARTHAGE_PLATFORM_TVOS = "tvOS"
    static final String CARTHAGE_PLATFORM_MACOS = "Mac"
    static final String CARTHAGE_PLATFORM_WATCHOS = "watchOS"

    public CarthageUpdateTask() {
        super()
        setDescription "Installs the carthage dependencies for the given project"
    }

    @TaskAction
    void update() {
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
                [carthageCommand, ACTION_UPDATE, ARG_PLATFORM, carthagePlatform, ARG_CACHE_BUILDS],
                new ConsoleOutputAppender(output))
    }

    String getCarthagePlatform() {
        switch (project.xcodebuild.type) {
            case Type.iOS: return CARTHAGE_PLATFORM_IOS
            case Type.tvOS: return CARTHAGE_PLATFORM_TVOS
            case Type.macOS: return CARTHAGE_PLATFORM_MACOS
            case Type.watchOS: return CARTHAGE_PLATFORM_WATCHOS
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
            commandRunner.runWithResult("ls", CARTHAGE_USR_BIN_PATH)
            return CARTHAGE_USR_BIN_PATH
        } catch (CommandRunnerException) {
            // ignore, because blow an exception is thrown
        }
        throw new IllegalStateException("The carthage command was not found. Make sure that Carthage is installed")
    }

    boolean hasCartFile() {
        return cartFile.exists()
    }
}
