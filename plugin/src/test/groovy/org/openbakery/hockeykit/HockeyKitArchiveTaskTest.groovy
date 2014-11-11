package org.openbakery.hockeykit

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
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
class HockeyKitArchiveTaskTest {

	Project project
	HockeyKitArchiveTask hockeyKitArchiveTask;

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
		project.xcodebuild.productName = 'Test'

		hockeyKitArchiveTask = project.getTasks().getByPath('hockeykit-archive')

		hockeyKitArchiveTask.setProperty("commandRunner", commandRunnerMock)

		infoPlist = new File(project.buildDir, "Info.plist")
		FileUtils.writeStringToFile(infoPlist, "dummy")

		FileUtils.writeStringToFile(project.xcodebuild.getIpaBundle(), "dummy")

	}


	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	@Test
	void testArchive() {

		project.hockeykit.versionDirectoryName = "123"

		def commandList = ["/usr/libexec/PlistBuddy", infoPlist.absolutePath, "-c", "Print :CFBundleIdentifier"]
		commandRunnerMock.runWithResult(commandList).returns("com.example.Test")

		mockControl.play {
			hockeyKitArchiveTask.archive()
		}


		File expectedIpa = new File(project.buildDir, "hockeykit/com.example.test/123/Test.ipa")
		assert expectedIpa.exists()

	}

	@Test
	void testArchiveWithBundleSuffix() {

		project.xcodebuild.bundleNameSuffix = '-SUFFIX'

		project.hockeykit.versionDirectoryName = "123"

		def commandList = ["/usr/libexec/PlistBuddy", infoPlist.absolutePath, "-c", "Print :CFBundleIdentifier"]
		commandRunnerMock.runWithResult(commandList).returns("com.example.Test")

		mockControl.play {
			hockeyKitArchiveTask.archive()
		}

		File expectedIpa = new File(project.buildDir, "hockeykit/com.example.test/123/Test-SUFFIX.ipa")
		assert expectedIpa.exists()

	}
}
