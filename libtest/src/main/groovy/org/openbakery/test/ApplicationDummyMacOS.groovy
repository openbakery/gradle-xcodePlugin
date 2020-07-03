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


	void createFramework(String version = "A", String dylibName = null, String resourcesFile = null) {
		File frameworkVersion = new File(applicationBundle, "Contents/Frameworks/My.framework/Versions/${version}")
		frameworkVersion.mkdirs()
		File frameworkFile = new File(frameworkVersion, "My Framework")
		FileUtils.writeStringToFile(frameworkFile, "dummy")

		if (dylibName != null) {
			File libraries = new File(frameworkVersion, "Libraries")
			libraries.mkdirs()
			File dylib = new File(libraries, "${dylibName}.dylib")
			FileUtils.writeStringToFile(dylib, "dummy")
		}

		if (resourcesFile != null) {
			File resources = new File(frameworkVersion, "Resources")
			resources.mkdirs()
			File executable = new File(resources, "${resourcesFile}")
			FileUtils.writeStringToFile(executable, "dummy")
			executable.setExecutable(true)
		}

	}



	void createEmbeddedApp(String name = "HelperApp") {
		File embeddedApp = new File(applicationBundle, "Contents/Frameworks/${name}.app/Contents/MacOS/")
		embeddedApp.mkdirs()
		File executable = new File(embeddedApp, name)
		FileUtils.writeStringToFile(executable, "dummy")
	}



}
