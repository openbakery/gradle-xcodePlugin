package org.openbakery.hockeykit

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
		project.xcodebuild.infoPlist = 'Info.plist'

		hockeyKitArchiveTask = project.getTasks().getByPath('hockeykitArchive')

		hockeyKitArchiveTask.setProperty("commandRunner", commandRunnerMock)
		//PlistHelper.commandRunner = commandRunnerMock


		File ipaBundle = new File(project.getBuildDir(), "package/Test.ipa")
		FileUtils.writeStringToFile(ipaBundle, "dummy")

		File archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Test.xcarchive")
		archiveDirectory.mkdirs()

		infoPlist = new File(archiveDirectory, "Products/Applications/Test.app/Info.plist");
		infoPlist.parentFile.mkdirs();
		FileUtils.writeStringToFile(infoPlist, "dummy")


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

		hockeyKitArchiveTask.archive()

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
