package org.openbakery

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.signing.PackageTask
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 01.12.14.
 */
class XcodeBuildArchiveTaskTest {

	Project project

	XcodeBuildArchiveTask xcodeBuildArchiveTask;

	File projectDir

	@BeforeMethod
	void setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.infoPlist = 'Info.plist'
		project.xcodebuild.productName = 'Example'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.sdk = "iphonesimulator"
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"

		xcodeBuildArchiveTask = project.getTasks().getByPath(XcodePlugin.ARCHIVE_TASK_NAME)

		def buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)
		buildOutputDirectory.mkdirs();

		File appDirectory = new File(buildOutputDirectory, "Example.app");
		appDirectory.mkdirs();

		File app = new File(appDirectory, "Example")
		FileUtils.writeStringToFile(app, "dummy")


	}

	@AfterMethod
	void cleanUp() {
		//FileUtils.deleteDirectory(project.projectDir)
	}

	@Test
	void createArchive() {
		xcodeBuildArchiveTask.archive();

		File zipFile = new File(projectDir, "build/Example.zip");
		assert zipFile.exists() : "Zipfile does not exist: " + zipFile.absolutePath

	}

	@Test
	void createArchiveWithBundleSuffix() {

		project.xcodebuild.bundleNameSuffix = "-1.2.3"
		project.xcodebuild.sdk = "iphonesimulator"

		xcodeBuildArchiveTask.archive();

		File zipFile = new File(projectDir, "build/Example-1.2.3.zip");
		assert zipFile.exists() : "Zipfile does not exist: " + zipFile.absolutePath

	}


	@Test
	void createDeviceArchive() {
		project.xcodebuild.sdk = "iphoneos"
		def buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)

		new File(buildOutputDirectory, "Example.app").mkdirs()
		new File(buildOutputDirectory, "Example.ipa").mkdirs()
		new File(buildOutputDirectory, "Example.app.dSym").mkdirs()

		xcodeBuildArchiveTask.archive();

		File zipFile = new File(projectDir, "build/Example.zip");
		assert zipFile.exists() : "Zipfile does not exist: " + zipFile.absolutePath

	}
}
