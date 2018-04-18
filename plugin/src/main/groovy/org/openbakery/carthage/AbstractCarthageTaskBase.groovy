package org.openbakery.carthage

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.openbakery.AbstractXcodeTask
import org.openbakery.xcode.Type

abstract class AbstractCarthageTaskBase extends AbstractXcodeTask {

	static final String ACTION_BOOTSTRAP = "bootstrap"
	static final String ACTION_UPDATE = "update"
	static final String ARG_CACHE_BUILDS = "--cache-builds"
	static final String ARG_PLATFORM = "--platform"
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

	boolean hasCartFile() {
		return project.rootProject
				.file(CARTHAGE_FILE)
				.exists()
	}
}
