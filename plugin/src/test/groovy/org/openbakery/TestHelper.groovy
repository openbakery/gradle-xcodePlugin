package org.openbakery

import org.apache.commons.io.FileUtils
import org.openbakery.util.PlistHelper

class TestHelper {


	static File createDummyApp(File destinationDirectory, String name) {
		File appDirectory = new File(destinationDirectory,  "${name}.app")
		appDirectory.mkdirs()

		File app = new File(appDirectory, "Example")
		FileUtils.writeStringToFile(app, "dummy")


		File dSymDirectory = new File(destinationDirectory, "${name}.app.dSym")
		dSymDirectory.mkdirs()


		File infoPlist = new File("../example/iOS/Example/Example/Example-Info.plist")
		File destinationInfoPlist = new File(appDirectory, "Info.plist")
		FileUtils.copyFile(infoPlist, destinationInfoPlist)

		PlistHelper plistHelper = new PlistHelper(new CommandRunner())
		plistHelper.setValueForPlist(destinationInfoPlist, "CFBundleIdentifier", "org.openbakery.test.Example")


		FileUtils.writeStringToFile(new File(destinationDirectory, "${name}.app/Icon.png"), "dummy")
		FileUtils.writeStringToFile(new File(destinationDirectory, "${name}.app/Icon-72.png"), "dummy")

		return appDirectory
	}

	static def createFile(File file, String content) {
		FileUtils.writeStringToFile(file, content)
	}

	static def createOnDemandResources(File destinationDirectory) {
		File onDemandResourcesPlist = new File(destinationDirectory, "OnDemandResources.plist")
		FileUtils.writeStringToFile(onDemandResourcesPlist, "dummy")

		File onDemandResourcesDirectory = new File(destinationDirectory.parentFile, "OnDemandResources/org.openbakery.test.Example.SampleImages.assetpack")
		onDemandResourcesDirectory.mkdirs()
		File infoPlist_onDemandResourcesDirectory = new File(onDemandResourcesDirectory, "Info.plist")
		FileUtils.writeStringToFile(infoPlist_onDemandResourcesDirectory, "dummy")
	}
}
