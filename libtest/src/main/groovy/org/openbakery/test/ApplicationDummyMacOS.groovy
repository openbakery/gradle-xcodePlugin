package org.openbakery.test

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.codesign.ProvisioningProfileType
import org.openbakery.util.PlistHelper

class ApplicationDummyMacOS {


	File directory
	File applicationBundle


	public ApplicationDummyMacOS(File directory) {
		this.directory = directory
		this.applicationBundle = new File(directory, "Example.app")
	}

	void cleanup() {
		FileUtils.deleteDirectory(directory)
	}

	File create() {
		// create dummy app

		String bundleIdentifier =  "org.openbakery.macOS.Example"
		File contentsDirectory = new File(applicationBundle, "Contents")
		File executeableDirectory = new File(contentsDirectory, "MacOS")
		if (!executeableDirectory.exists()) {
			executeableDirectory.mkdirs()
		}

		FileUtils.writeStringToFile(new File(executeableDirectory, "application"), "dummy")

		File infoPlist = new File(contentsDirectory, "Info.plist")

		PlistHelper helper = new PlistHelper(new CommandRunner())
		helper.create(infoPlist)
		helper.addValueForPlist(infoPlist, "CFBundleIdentifier", bundleIdentifier)

		return applicationBundle
	}

}
