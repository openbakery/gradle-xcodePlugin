package org.openbakery.testflight

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
 * Created by rene on 16.02.15.
 */
class TestFlightUploadTaskTest {

	Project project
	TestFlightUploadTask testFlightUploadTask;

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

		testFlightUploadTask = project.getTasks().getByPath('testflight')

		testFlightUploadTask.setProperty("commandRunner", commandRunnerMock)


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

		testFlightUploadTask.prepare()

		File expectedIpa = new File(project.buildDir, "testflight/Test.ipa")
		assert expectedIpa.exists()

		File expectedDSYM = new File(project.buildDir, "testflight/Test.app.dSYM.zip")
		assert expectedDSYM.exists()


	}

	@Test
	void testArchiveWithBundleSuffix() {

		project.xcodebuild.bundleNameSuffix = '-SUFFIX'

		testFlightUploadTask.prepare()

		File expectedIpa = new File(project.buildDir, "testflight/Test-SUFFIX.ipa")
		assert expectedIpa.exists()

		File expectedZip = new File(project.buildDir, "testflight/Test-SUFFIX.app.dSYM.zip")
		assert expectedZip.exists()

	}

}
