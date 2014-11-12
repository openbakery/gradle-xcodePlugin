package org.openbakery.hockeyapp

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * User: rene
 * Date: 11/11/14
 */
class HockeyAppPrepareTaskTest {
	Project project
	HockeyAppPrepareTask hockeyAppPrepareTask;

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
		project.xcodebuild.infoPlist = 'Info.plist'

		hockeyAppPrepareTask = project.getTasks().getByPath('hockeyapp-prepare')

		hockeyAppPrepareTask.setProperty("commandRunner", commandRunnerMock)

		infoPlist = new File(project.buildDir, "Info.plist")
		FileUtils.writeStringToFile(infoPlist, "dummy")

		FileUtils.writeStringToFile(project.xcodebuild.getIpaBundle(), "dummy")
		FileUtils.writeStringToFile(project.xcodebuild.getDSymBundle(), "dummy")

	}


	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	@Test
	void testArchive() {

		hockeyAppPrepareTask.archive()

		File expectedIpa = new File(project.buildDir, "hockeyapp/Test.ipa")
		assert expectedIpa.exists()

		File expectedDSYM = new File(project.buildDir, "hockeyapp/Test.app.dSYM")
		assert expectedDSYM.exists()

		File expectedZip = new File(project.buildDir, "hockeyapp/Test.app.dSYM.zip")
		assert expectedZip.exists()

	}

	@Test
	void testArchiveWithBundleSuffix() {

		project.xcodebuild.bundleNameSuffix = '-SUFFIX'

		hockeyAppPrepareTask.archive()

		File expectedIpa = new File(project.buildDir, "hockeyapp/Test-SUFFIX.ipa")
		assert expectedIpa.exists()

		File expectedZip = new File(project.buildDir, "hockeyapp/Test-SUFFIX.app.dSYM.zip")
		assert expectedZip.exists()

	}
}
