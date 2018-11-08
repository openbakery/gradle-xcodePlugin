package org.openbakery.bundle

import org.openbakery.xcode.Type

class ApplicationBundle {

	File applicationPath
	Type type
	boolean simulator

	ApplicationBundle(File applicationPath, Type type, boolean simulator) {
		this.applicationPath = applicationPath
		this.type = type
		this.simulator = simulator
	}

	List<Bundle> getBundles() {
		ArrayList<Bundle> bundles = new ArrayList<Bundle>()

		addPluginsToAppBundle(applicationPath, bundles)

		if (isDeviceBuildOf(Type.iOS)) {
			addWatchToAppBundle(bundles)
		}
		bundles.add(new Bundle(applicationPath))
		return bundles
	}

	String getBundleName() {
		return applicationPath.getName()
	}

	File getPayloadDirectory() {
		if (applicationPath.parentFile.getName().toLowerCase() == "payload") {
			return applicationPath.parentFile
		}
		return applicationPath
	}

	File getBaseDirectory() {
		return getPayloadDirectory().parentFile
	}


	private void addPluginsToAppBundle(File appBundle, ArrayList<Bundle> bundles) {
		File plugins
		if (isDeviceBuildOf(Type.iOS)) {
			plugins = new File(appBundle, "PlugIns")
		} else if (this.type == Type.macOS) {
			plugins = new File(appBundle, "Contents/PlugIns")
		} else {
			return
		}

		if (plugins.exists()) {
			for (File pluginBundle : plugins.listFiles()) {
				if (pluginBundle.isDirectory()) {

					if (pluginBundle.name.endsWith(".framework")) {
						// Frameworks have to be signed with this path
						File path = new File(pluginBundle, "/Versions/Current")
						bundles.add(new Bundle(path))
					} else if (pluginBundle.name.endsWith(".appex")) {

						for (File appexBundle : pluginBundle.listFiles()) {
							if (appexBundle.isDirectory() && appexBundle.name.endsWith(".app")) {
								bundles.add(new Bundle(appexBundle))
							}
						}
						bundles.add(new Bundle(pluginBundle))
					} else if (pluginBundle.name.endsWith(".app")) {
						bundles.add(new Bundle(pluginBundle))
					}
				}
			}
		}
	}

	private void addWatchToAppBundle(ArrayList<Bundle> bundles) {
		def watchAppBundle = getWatchAppBundle()
		if (watchAppBundle != null) {
			addPluginsToAppBundle(watchAppBundle.applicationPath, bundles)
			bundles.add(new Bundle(watchAppBundle.applicationPath))
		}
	}

	boolean isDeviceBuildOf(Type expectedType) {
		if (type != expectedType) {
			return false
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

	File getPlugInsPath() {
		switch (type) {
			case Type.macOS:
			case Type.iOS:
			case Type.watchOS:
				return new File(applicationPath, "PlugIns")
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

	ArrayList<File> getAppExtensionBundles() {
		File pluginsDirectory
		File appBundle = this.applicationPath

		if (this.type == Type.iOS || this.type == Type.watchOS) {
			pluginsDirectory = new File(appBundle, "PlugIns")
		} else if (this.type == Type.macOS) {
			pluginsDirectory = new File(appBundle, "Contents/PlugIns")
		} else {
			return []
		}

		if (pluginsDirectory.exists()) {
			return pluginsDirectory.listFiles().findAll { it.isDirectory() && it.name.endsWith(".appex") }
		}

		return []
	}


	@Override
	String toString() {
		return "ApplicationBundle{" +
			"applicationPath=" + applicationPath +
			", type=" + type +
			", simulator=" + simulator +
			'}';
	}
}
