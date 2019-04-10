package org.openbakery.carthage

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.AbstractXcodeTask
import org.openbakery.output.ConsoleOutputAppender
import org.openbakery.xcode.Type

abstract class AbstractCarthageTaskBase extends AbstractXcodeTask {

	static final String ACTION_BOOTSTRAP = "bootstrap"
	static final String ACTION_UPDATE = "update"
	static final String ARGUMENT_CACHE_BUILDS = "--cache-builds"
	static final String ARGUMENT_PLATFORM = "--platform"
	static final String ARGUMENT_DERIVED_DATA = "--derived-data"
	static final String CARTHAGE_FILE = "Cartfile"
	static final String CARTHAGE_FILE_RESOLVED = "Cartfile.resolved"
	static final String CARTHAGE_PLATFORM_IOS = "iOS"
	static final String CARTHAGE_PLATFORM_MACOS = "Mac"
	static final String CARTHAGE_PLATFORM_TVOS = "tvOS"
	static final String CARTHAGE_PLATFORM_WATCHOS = "watchOS"
	static final String CARTHAGE_USR_BIN_PATH = "/usr/local/bin/carthage"

	AbstractCarthageTaskBase() {
		super()
	}

	@Input
	@Optional
	String getRequiredXcodeVersion() {
		return getProjectXcodeVersion()
	}

	@InputFile
	@Optional
	@PathSensitive(PathSensitivity.RELATIVE)
	Provider<File> getCartFile() {
		// Cf https://github.com/gradle/gradle/issues/2016
		File file = project.rootProject.file(CARTHAGE_FILE)
		return project.provider {
			file.exists() ? file
					: File.createTempFile(CARTHAGE_FILE, "")
		}
	}

	@InputFile
	@Optional
	@PathSensitive(PathSensitivity.RELATIVE)
	Provider<File> getCartResolvedFile() {
		// Cf https://github.com/gradle/gradle/issues/2016
		File file = project.rootProject.file(CARTHAGE_FILE_RESOLVED)
		return project.provider {
			file.exists() ? file
					: File.createTempFile(CARTHAGE_FILE_RESOLVED, "resolved")
		}
	}

	@Input
	String getCarthagePlatformName() {
		switch (project.xcodebuild.type) {
			case Type.iOS: return CARTHAGE_PLATFORM_IOS
			case Type.tvOS: return CARTHAGE_PLATFORM_TVOS
			case Type.macOS: return CARTHAGE_PLATFORM_MACOS
			case Type.watchOS: return CARTHAGE_PLATFORM_WATCHOS
			default: return 'all'
		}
	}

	@OutputDirectory
	Provider<File> getOutputDirectory() {
		return project.provider {
			project.rootProject.file("Carthage/Build/" + getCarthagePlatformName())
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

	boolean hasCartfile() {
		return project.rootProject
				.file(CARTHAGE_FILE)
				.exists()
	}

	void run(String command, StyledTextOutput output) {

		if (hasCartfile()) {
			logger.info('Update Carthage for platform ' + carthagePlatformName)

			def derivedDataPath = new File(project.xcodebuild.derivedDataPath, "carthage")

			List<String> args = [getCarthageCommand(),
													 command,
													 ARGUMENT_PLATFORM,
													 carthagePlatformName,
													 ARGUMENT_CACHE_BUILDS,
													 ARGUMENT_DERIVED_DATA,
													 derivedDataPath.absolutePath
			]

			commandRunner.run(project.projectDir.absolutePath,
				args,
				getEnvironment(),
				new ConsoleOutputAppender(output))
		}

	}

	Map<String, String> getEnvironment() {
		if (getRequiredXcodeVersion() != null) {
			return xcode.getXcodeSelectEnvironmentValue(getRequiredXcodeVersion())
		}
		return null
	}

}
