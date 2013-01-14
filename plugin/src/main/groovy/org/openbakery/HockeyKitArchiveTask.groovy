package org.openbakery

import org.gradle.api.tasks.TaskAction
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.FileUtils

class HockeyKitArchiveTask extends AbstractXcodeTask{

	HockeyKitArchiveTask() {
		super()
		this.description = "Prepare the app bundle so that it can be uploaded to the Hockeykit Server"
	}


	@TaskAction
	def archive() {
		if (project.hockeykit.versionDirectoryName == null) {
			throw new IllegalArgumentException("hockeykit.versionDirectoryName is missing")
		}

		def title = getValueFromPlist(getAppBundleInfoPlist(), "CFBundleIdentifier")

		File outputDirectory = new File(project.hockeykit.outputDirectory + "/" + title + "/" + project.hockeykit.versionDirectoryName)
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}

		def appName = getAppBundleName()
		def baseName =  appName.substring(0, appName.size()-4)

		File sourceIpa = new File(baseName + ".ipa")
		if (!sourceIpa.exists()) {
			throw new IllegalArgumentException("cannot find ipa: " + sourceIpa)
		}
		File destinationIpa = new File(outputDirectory, FilenameUtils.getBaseName(appName) + ".ipa")
		FileUtils.copyFile(sourceIpa, destinationIpa)

		println "Created hockeykit archive in " + outputDirectory
	}
}
