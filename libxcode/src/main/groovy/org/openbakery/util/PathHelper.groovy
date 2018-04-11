package org.openbakery.util

import org.openbakery.xcode.Type

class PathHelper {
	public static final String APPLE_TV_OS = "appletvos"
	public static final String APPLE_TV_SIMULATOR = "appletvsimulator"
	public static final String IPHONE_SIMULATOR = "iphonesimulator"
	public static final String IPHONE_OS = "iphoneos"

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
}
