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


	void createFramework(String version = "A") {
		File frameworkVersion = new File(applicationBundle, "Contents/Frameworks/My.framework/Versions/${version}")
		frameworkVersion.mkdirs()
		File frameworkFile = new File(frameworkVersion, "My Framework")
		FileUtils.writeStringToFile(frameworkFile, "dummy")
	}

	void createEmbeddedApp(String name = "HelperApp") {
		File embeddedApp = new File(applicationBundle, "Contents/Frameworks/${name}.app/Contents/MacOS/")
		embeddedApp.mkdirs()
		File executable = new File(embeddedApp, name)
		FileUtils.writeStringToFile(executable, "dummy")
	}



}
