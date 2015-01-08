package org.openbakery.deploygate

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodeBuildArchiveTask
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * User: rene
 * Date: 11/11/14
 */
class DeployGateUploadTaskTest {

	Project project
	DeployGateUploadTask deployGateUploadTask;

	GMockController mockControl

	File infoPlist

	@BeforeMethod
	void setup() {
		mockControl = new GMockController()

		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productName = 'Test'

		deployGateUploadTask = project.getTasks().getByPath('deploygate')

		File ipaBundle = new File(project.getBuildDir(), "package/Test.ipa")
		FileUtils.writeStringToFile(ipaBundle, "dummy")

		File archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Test.xcarchive")
		archiveDirectory.mkdirs()

		infoPlist = new File(archiveDirectory, "Products/Applications/Test.app/Info.plist");
		infoPlist.parentFile.mkdirs();


	}


	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	@Test
	void testArchive() {

		mockControl.play {
			deployGateUploadTask.prepare()
		}

		File expectedIpa = new File(project.buildDir, "deploygate/Test.ipa")
		assert expectedIpa.exists()
	}

	@Test
	void testArchiveWithSuffix() {

		project.xcodebuild.bundleNameSuffix = '-SUFFIX'


		mockControl.play {
			deployGateUploadTask.prepare()
		}

		File expectedIpa = new File(project.buildDir, "deploygate/Test-SUFFIX.ipa")
		assert expectedIpa.exists()
	}
}
