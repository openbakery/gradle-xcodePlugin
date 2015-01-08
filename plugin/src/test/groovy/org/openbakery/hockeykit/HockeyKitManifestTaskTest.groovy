package org.openbakery.hockeykit

import ch.qos.logback.core.util.FileUtil
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
 * Created by rene on 12.11.14.
 */
class HockeyKitManifestTaskTest {


	Project project
	HockeyKitManifestTask hockeyKitManifestTask;

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

		hockeyKitManifestTask = project.getTasks().getByPath('hockeykitManifest')

		hockeyKitManifestTask.setProperty("commandRunner", commandRunnerMock)


		File archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Test.xcarchive")
		archiveDirectory.mkdirs()

		infoPlist = new File(archiveDirectory, "Products/Applications/Test.app/Info.plist");
		infoPlist.parentFile.mkdirs();
		FileUtils.writeStringToFile(infoPlist, "dummy")

	}

	void mockValueFromPlist(String key, String value) {
		def commandList = ["/usr/libexec/PlistBuddy", infoPlist.absolutePath, "-c", "Print :" + key]
		commandRunnerMock.runWithResult(commandList).returns(value).atLeastOnce()

	}


	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	@Test
	void createManifest() {

		project.xcodebuild.bundleNameSuffix = '-b1234'
		project.hockeykit.versionDirectoryName = "1234"


		mockValueFromPlist("CFBundleIdentifier", "com.example.Test")
		mockValueFromPlist("CFBundleDisplayName", "Test")
		mockValueFromPlist("CFBundleVersion", "1.0.0-b1234")
		mockValueFromPlist("CFBundleShortVersionString", "1.0.0")

		mockControl.play {
			hockeyKitManifestTask.createManifest()
		}

		File manifestFile = new File(project.buildDir, "hockeykit/com.example.Test/1234/Test-b1234.plist")
		assert manifestFile.exists()

		String xmlContent = FileUtils.readFileToString(manifestFile, "UTF-8")
		assert xmlContent.contains("com.example.Test")
		assert xmlContent.contains("1.0.0-b1234")

	}

}
