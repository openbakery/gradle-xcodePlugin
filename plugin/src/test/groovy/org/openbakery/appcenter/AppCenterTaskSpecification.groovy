package org.openbakery.appcenter

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodeBuildArchiveTask
import spock.lang.Specification

class AppCenterTaskSpecification extends Specification {
	Project project
	AppCenterUploadTask appCenterUploadTask;

	CommandRunner commandRunner = Mock(CommandRunner)

	File infoPlist

	def setup() {
		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productType = 'app'
		project.xcodebuild.productName = 'Test'

		appCenterUploadTask = project.getTasks().getByPath('appcenter')

		appCenterUploadTask.commandRunner = commandRunner


		File ipaBundle = new File(project.getBuildDir(), "package/Test.ipa")
		FileUtils.writeStringToFile(ipaBundle, "dummy")

		File archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Test.xcarchive")
		archiveDirectory.mkdirs()

		infoPlist = new File(archiveDirectory, "Products/Applications/Test.app/Info.plist");
		infoPlist.parentFile.mkdirs();

		File dsymBundle = new File(archiveDirectory, "dSYMs/Test.app.dSYM")
		FileUtils.writeStringToFile(dsymBundle, "dummy")

	}


	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	def "archive"() {
		when:
		appCenterUploadTask.prepareFiles()

		File expectedIpa = new File(project.buildDir, "appcenter/Test.ipa")
		File expectedDSYM = new File(project.buildDir, "appcenter/Test.app.dSYM.zip")

		then:
		expectedIpa.exists()
		expectedDSYM.exists()
	}

	def "archive with bundleSuffix"() {
		given:
		project.xcodebuild.bundleNameSuffix = '-SUFFIX'

		when:
		appCenterUploadTask.prepareFiles()

		File expectedIpa = new File(project.buildDir, "appcenter/Test-SUFFIX.ipa")
		File expectedZip = new File(project.buildDir, "appcenter/Test-SUFFIX.app.dSYM.zip")

		then:
		expectedIpa.exists()
		expectedZip.exists()

	}
}
