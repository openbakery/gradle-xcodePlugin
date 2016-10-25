package org.openbakery

import org.apache.commons.io.FileUtils

/**
 * Created by rene on 25.10.16.
 */
class TestHelper {


	static File createDummyApp(File destinationDirectory, String name) {
		File appDirectory = new File(destinationDirectory,  "${name}.app")
		appDirectory.mkdirs()

		File app = new File(appDirectory, "Example")
		FileUtils.writeStringToFile(app, "dummy")


		File dSymDirectory = new File(destinationDirectory, "${name}.app.dSym")
		dSymDirectory.mkdirs()


		File infoPlist = new File("../example/iOS/Example/Example/Example-Info.plist")
		FileUtils.copyFile(infoPlist, new File(appDirectory, "Info.plist"))

		FileUtils.writeStringToFile(new File(destinationDirectory, "${name}.app/Icon.png"), "dummy")
		FileUtils.writeStringToFile(new File(destinationDirectory, "${name}.app/Icon-72.png"), "dummy")

		return appDirectory
	}
}
