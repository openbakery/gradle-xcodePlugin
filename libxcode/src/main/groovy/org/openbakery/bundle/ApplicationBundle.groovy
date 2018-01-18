package org.openbakery.bundle

import org.openbakery.xcode.Type

public class ApplicationBundle {

	File applicationPath
	Type type
	boolean simulator

	public ApplicationBundle(File applicationPath, Type type, boolean simulator) {
		this.applicationPath = applicationPath
		this.type = type
		this.simulator = simulator
	}

	List<File> getBundles() {
		ArrayList<File> bundles = new ArrayList<File>();

		addPluginsToAppBundle(applicationPath, bundles)

		if (isDeviceBuildOf(Type.iOS)) {
			addWatchToAppBundle(bundles)
		}
		bundles.add(applicationPath)
		return bundles;
	}

	private void addPluginsToAppBundle(File appBundle, ArrayList<File> bundles) {
		File plugins
		if (isDeviceBuildOf(Type.iOS)) {
			plugins = new File(appBundle, "PlugIns")
		}	else if (this.type == Type.macOS) {
			plugins = new File(appBundle, "Contents/PlugIns")
		} else {
			return
		}

		if (plugins.exists()) {
			for (File pluginBundle : plugins.listFiles()) {
				if (pluginBundle.isDirectory()) {

					if (pluginBundle.name.endsWith(".framework")) {
						// Frameworks have to be signed with this path
						bundles.add(new File(pluginBundle, "/Versions/Current"))
					}	else if (pluginBundle.name.endsWith(".appex")) {

						for (File appexBundle : pluginBundle.listFiles()) {
							if (appexBundle.isDirectory() && appexBundle.name.endsWith(".app")) {
								bundles.add(appexBundle)
							}
						}
						bundles.add(pluginBundle)
					} else if (pluginBundle.name.endsWith(".app")) {
						bundles.add(pluginBundle)
					}
				}
			}
		}
	}

	private void addWatchToAppBundle(ArrayList<File> bundles) {
		def watchAppBundle = getWatchAppBundle()
		if (watchAppBundle != null) {
			addPluginsToAppBundle(watchAppBundle.applicationPath, bundles)
			bundles.add(watchAppBundle.applicationPath)
		}
	}

	boolean isDeviceBuildOf(Type expectedType) {
		if (type != expectedType) {
			return false;
		}
		return !this.simulator
	}

	String getPlatformName() {
		switch (type) {
			case Type.iOS: return "iphoneos"
			case Type.watchOS: return "watchos"
			default: return null
		}
	}

	File getFrameworksPath() {
		switch (type) {
			case Type.macOS:
			case Type.iOS:
			case Type.watchOS:
				return new File(applicationPath, "Frameworks")
            default:
				return null
		}
	}

	ApplicationBundle getWatchAppBundle() {
		if (type != Type.iOS) {
			return null
		}

		File watchDirectory = new File(applicationPath, "Watch")
		if (!watchDirectory.exists()) {
			return null
		}

		File watchAppBundle = watchDirectory.listFiles().find { it.isDirectory() && it.name.endsWith(".app") }
		if (!watchAppBundle) {
			// TODO: print error, throw exception
			return null
		}

		return new ApplicationBundle(watchAppBundle, Type.watchOS, simulator)
	}
}
