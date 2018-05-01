package org.openbakery.util

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.openbakery.xcode.Type

@CompileStatic
class PathHelper {
	public static final String APPLE_TV_OS = "appletvos"
	public static final String APPLE_TV_SIMULATOR = "appletvsimulator"
	public static final String IPHONE_SIMULATOR = "iphonesimulator"
	public static final String IPHONE_OS = "iphoneos"
	public static final String FOLDER_ARCHIVE = "archive"

	private static final String ARCHIVE_FILE_NAME = "archive.xcconfig"
	private static final String FOLDER_PACKAGE = "package"
	private static final String EXTENSION_XC_ARCHIVE = ".xcarchive"

	static File resolvePath(Type type,
							boolean simulator,
							File symRoot,
							String configuration) {
		File result
		switch (type) {
			case Type.iOS:
				result = resolveIosSymRoot(simulator,
						symRoot,
						configuration)
				break

			case Type.tvOS:
				result = resolveAppleTvSymRoot(simulator,
						symRoot,
						configuration)
				break

			case Type.macOS:
				result = resolveMacOsSymRoot(symRoot,
						configuration)
				break

			default:
				throw new IllegalStateException("WatchOs not implemeted")
				break

		}

		return result
	}

	static File resolveAppleTvSymRoot(boolean simulator,
									  File symRoot,
									  String configuration) {
		return resolveSymRoot(symRoot,
				configuration,
				simulator ? APPLE_TV_SIMULATOR : APPLE_TV_OS)
	}

	static File resolveIosSymRoot(boolean simulator,
								  File symRoot,
								  String configuration) {
		return resolveSymRoot(symRoot,
				configuration,
				simulator ? IPHONE_SIMULATOR : IPHONE_OS)
	}

	static File resolveMacOsSymRoot(File symRoot,
									String configuration) {
		return new File(symRoot,
				configuration)
	}

	private static File resolveSymRoot(File symRoot,
									   String configuration,
									   String destination) {
		return new File(symRoot,
				"${configuration}-${destination}")
	}

	static File resolveArchiveFolder(Project project) {
		return new File(project.getBuildDir(), FOLDER_ARCHIVE)
	}

	static File resolveArchiveFile(Project project,
								   String scheme) {
		return new File(resolveArchiveFolder(project), scheme + EXTENSION_XC_ARCHIVE)
	}

	static File resolveArchivingLogFile(Project project) {
		return new File(resolveArchiveFolder(project), "xcodebuild-archive-output.txt")
	}

	static File resolveXcConfigFile(Project project) {
		return new File(resolveArchiveFolder(project), ARCHIVE_FILE_NAME)
	}

	static File resolvePackageFolder(Project project) {
		return new File(project.getBuildDir(), FOLDER_PACKAGE)
	}
}
