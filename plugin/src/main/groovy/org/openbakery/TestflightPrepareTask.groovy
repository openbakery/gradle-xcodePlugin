package org.openbakery

import org.gradle.api.tasks.TaskAction
import org.apache.commons.io.FilenameUtils
import org.apache.ivy.util.FileUtil
import org.apache.commons.io.FileUtils

class TestFlightPrepareTask extends AbstractXcodeTask {

	TestFlightPrepareTask() {
		super()
		dependsOn("codesign")
		this.description = "Prepare the app bundle and dSYM to publish with using testflight"
	}


	@TaskAction
	def archive() {
		def buildOutputDirectory = new File(project.xcodebuild.symRoot + "/" + project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)

		def appName = getAppBundleName()
		def baseName = appName.substring(0, appName.size() - 4)
		def ipaName = baseName + ".ipa"
		def dsymName = baseName + ".app.dSYM"

		def zipFileName = baseName

		if (!project.testflight.outputDirectory.exists()) {
			project.testflight.outputDirectory.mkdirs()
		}


		if (project.xcodebuild.bundleNameSuffix != null) {
			println "Rename App"

			File ipaFile = new File(ipaName)
			if (ipaFile.exists()) {
				ipaName = baseName + project.xcodebuild.bundleNameSuffix + ".ipa";
				ipaFile.renameTo(ipaName)
			}

			File dsymFile = new File(dsymName)
			if (dsymFile.exists()) {
				dsymFile.renameTo(baseName + project.xcodebuild.bundleNameSuffix + ".app.dSYM")
			}
			zipFileName += project.xcodebuild.bundleNameSuffix

		}


		println "project.testflight.outputDirectory " + project.testflight.outputDirectory
		int index = zipFileName.lastIndexOf('/')
		def baseZipName = zipFileName.substring(index+1, zipFileName.length());

		println "baseZipName " + baseZipName
		println "buildOutputDirectory " + buildOutputDirectory;


		def ant = new AntBuilder()
		ant.zip(destfile: project.testflight.outputDirectory.absolutePath + "/" + baseZipName + ".app.dSYM.zip",
						basedir: buildOutputDirectory.absolutePath,
						includes: "*dSYM*/**")

		FileUtils.copyFileToDirectory(new File(ipaName), project.testflight.outputDirectory)


	}
}
