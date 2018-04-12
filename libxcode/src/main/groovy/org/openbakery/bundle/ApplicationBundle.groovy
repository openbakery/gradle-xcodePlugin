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

		if (isDeviceBuildOf(Type.iOS) || isDeviceBuildOf(Type.tvOS)) {
			addWatchToAppBundle(applicationPath, bundles)
		}
		bundles.add(applicationPath)
		return bundles;
	}

	private void addPluginsToAppBundle(File appBundle, ArrayList<File> bundles) {
		File plugins
		if (isDeviceBuildOf(Type.iOS) || isDeviceBuildOf(Type.tvOS)) {
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
						bundles.add(new File(pluginBundle, "/Versions/Current"))
					} else if (pluginBundle.name.endsWith(".appex")) {

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

	private void addWatchToAppBundle(File appBundle, ArrayList<File> bundles) {
		File watchDirectory
		watchDirectory = new File(appBundle, "Watch")
		if (watchDirectory.exists()) {
			for (File bundle : watchDirectory.listFiles()) {
				if (bundle.isDirectory()) {
					if (bundle.name.endsWith(".app")) {
						addPluginsToAppBundle(bundle, bundles)
						bundles.add(bundle)
					}
				}
			}
		}
	}

	boolean isDeviceBuildOf(Type expectedType) {
		if (type != expectedType) {
			return false;
		}
		return !this.simulator
	}

}
