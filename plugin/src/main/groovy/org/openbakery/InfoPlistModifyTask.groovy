package org.openbakery

import org.gradle.api.tasks.TaskAction


class InfoPlistModifyTask extends AbstractXcodeTask {

	@TaskAction
	def prepare() {
		def infoPlist = getInfoPlist()
		println "Updating " + infoPlist

		if (project.infoplist.bundleIdentifier != null) {

			runCommand([
					  "/usr/libexec/PlistBuddy",
					  infoPlist,
					  "-c",
					  "Set :CFBundleIdentifier " + project.infoplist.bundleIdentifier
			])
		}

		// add suffix to bundleIdentifier
		if (project.infoplist.bundleIdentifierSuffix != null) {
			def bundleIdentifier = getValueFromPlist(infoPlist, "CFBundleIdentifier")

			runCommand([
					  "/usr/libexec/PlistBuddy",
					  infoPlist,
					  "-c",
					  "Set :CFBundleIdentifier " + bundleIdentifier + project.infoplist.bundleIdentifierSuffix
			])
		}

		println "project.infoplist.version: " + project.infoplist.version
		def version;
		if (project.infoplist.version != null) {
			version = project.infoplist.version
		} else {
			version = runCommandWithResult([
					  "/usr/libexec/PlistBuddy",
					  infoPlist,
					  "-c",
					  "Print :CFBundleVersion"])
		}

		if (project.infoplist.versionSuffix) {
			version = version + project.infoplist.versionSuffix
		}

		if (project.infoplist.versionPrefix) {
			version = project.infoplist.versionPrefix + version
		}

		println "Modify CFBundleVersion to " + version
		runCommand([
				  "/usr/libexec/PlistBuddy",
				  infoPlist,
				  "-c",
				  "Set :CFBundleVersion " + version])


		def shortVersionString
		try {
			shortVersionString = runCommandWithResult([
					  "/usr/libexec/PlistBuddy",
					  infoPlist,
					  "-c",
					  "Print :CFBundleShortVersionString"])
		} catch (IllegalStateException ex) {
			// no CFBundleShortVersionString exists so noting can be modified!
			return;
		}

		if (project.infoplist.shortVersionString != null) {
			shortVersionString = project.infoplist.shortVersionString
		}
		if (project.infoplist.shortVersionStringSuffix) {
			shortVersionString = shortVersionString + project.infoplist.shortVersionStringSuffix
		}

		if (project.infoplist.shortVersionStringPrefix) {
			shortVersionString = project.infoplist.shortVersionStringPrefix + shortVersionString
		}

		println "Modify CFBundleShortVersionString to " + shortVersionString
		runCommand([
				  "/usr/libexec/PlistBuddy",
				  infoPlist,
				  "-c",
				  "Set :CFBundleShortVersionString " + shortVersionString])

	}

}