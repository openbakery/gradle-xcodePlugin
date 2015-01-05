package org.openbakery.hockeyapp

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodeBuildArchiveTask
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * User: rene
 * Date: 11/11/14
 */
class HockeyAppTaskTest {
	Project project
	HockeyAppUploadTask hockeyAppUploadTask;

	GMockController mockControl
	CommandRunner commandRunnerMock

	File infoPlist

	@BeforeMethod
	void setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)

		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productType = 'app'
		project.xcodebuild.productName = 'Test'

		hockeyAppUploadTask = project.getTasks().getByPath('hockeyapp')

		hockeyAppUploadTask.setProperty("commandRunner", commandRunnerMock)


		File ipaBundle = new File(project.getBuildDir(), "package/Test.ipa")
		FileUtils.writeStringToFile(ipaBundle, "dummy")

		File archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Test.xcarchive")
		archiveDirectory.mkdirs()

		infoPlist = new File(archiveDirectory, "Products/Applications/Test.app/Info.plist");
		infoPlist.parentFile.mkdirs();


		File dsymBundle = new File(archiveDirectory, "dSYMs/Test.app.dSYM")
		FileUtils.writeStringToFile(dsymBundle, "dummy")

	}


	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	@Test
	void testArchive() {

		hockeyAppUploadTask.prepare()

		File expectedIpa = new File(project.buildDir, "hockeyapp/Test.ipa")
		assert expectedIpa.exists()

		File expectedDSYM = new File(project.buildDir, "hockeyapp/Test.app.dSYM.zip")
		assert expectedDSYM.exists()


	}

	@Test
	void testArchiveWithBundleSuffix() {

		project.xcodebuild.bundleNameSuffix = '-SUFFIX'

		hockeyAppUploadTask.prepare()

		File expectedIpa = new File(project.buildDir, "hockeyapp/Test-SUFFIX.ipa")
		assert expectedIpa.exists()

		File expectedZip = new File(project.buildDir, "hockeyapp/Test-SUFFIX.app.dSYM.zip")
		assert expectedZip.exists()

	}
}
