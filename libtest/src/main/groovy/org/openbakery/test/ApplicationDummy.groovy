package org.openbakery.test

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.openbakery.CommandRunner
import org.openbakery.testdouble.PlistHelperStub

/**
 * Created by rene on 08.11.16.
 */
class ApplicationDummy {


	File directory
	File payloadAppDirectory
	File applicationBundle
	PlistHelperStub plistHelperStub = new PlistHelperStub()


	List<File>mobileProvisionFile = []

	public ApplicationDummy(File directory) {
		this.directory = directory
		File payloadDirectory = new File(directory, "Payload")
		payloadAppDirectory = new File(payloadDirectory, "Example.app");
		applicationBundle = new File(directory, "Products/Applications/Example.app")
	}

	void cleanup() {
		FileUtils.deleteDirectory(directory)
	}


	void create(boolean adHoc = true) {
		// create dummy app
		File appDirectory = applicationBundle
		if (!appDirectory.exists()) {
			appDirectory.mkdirs();
		}

		FileUtils.writeStringToFile(new File(appDirectory, "Example"), "dummy");
		FileUtils.writeStringToFile(new File(appDirectory, "ResourceRules.plist"), "dummy");
		FileUtils.writeStringToFile(new File(appDirectory, "Info.plist"), "dummy");

		File infoPlist = new File(payloadAppDirectory, "Info.plist")
		plistHelperStub.setValueForPlist(infoPlist, "CFBundleIdentifier", "org.openbakery.Example")

		File mobileprovision = null
		if (adHoc) {
			mobileProvisionFile.add(new File("../libtest/src/main/Resource/test.mobileprovision"))
		} else {
			mobileProvisionFile.add(new File("../libtest/src/main/Resource/Appstore.mobileprovision"))
		}
	}


	void createPlugin() {
		String widgetPath = "PlugIns/ExampleTodayWidget.appex"
		File widgetsDirectory = new File(directory, widgetPath)
		FileUtils.writeStringToFile(new File(widgetsDirectory, "ExampleTodayWidget"), "dummy");

		File infoPlistWidget = new File(payloadAppDirectory, widgetPath + "/Info.plist");
		plistHelperStub.setValueForPlist(infoPlistWidget, "CFBundleIdentifier", "org.openbakery.ExampleWidget")


		File widgetMobileprovision = new File("src/test/Resource/test1.mobileprovision")
		mobileProvisionFile.add(widgetMobileprovision)
	}

	void createSwiftLibs() {
		File libSwiftCore = new File(applicationBundle, "Frameworks/libswiftCore.dylib")
		FileUtils.writeStringToFile(libSwiftCore, "dummy")
		File libSwiftCoreArchive = new File(directory, "SwiftSupport/libswiftCore.dylib")
		FileUtils.writeStringToFile(libSwiftCoreArchive, "dummy")

		File libswiftCoreGraphics = new File(applicationBundle, "Frameworks/libswiftCoreGraphics.dylib")
		FileUtils.writeStringToFile(libswiftCoreGraphics, "dummy")
	}

	void createFramework() {
		File framework = new File(applicationBundle, "Frameworks/My.framework")
		framework.mkdirs()
		File frameworkFile = new File(applicationBundle, "Frameworks/My.framework/My")
		FileUtils.writeStringToFile(frameworkFile, "dummy")
	}


}
