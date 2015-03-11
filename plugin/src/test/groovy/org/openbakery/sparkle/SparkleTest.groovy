package org.openbakery.sparkle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by Stefan on 25/02/15.
 */
class SparkleTest {

	File projectDir
	Project project
	SparklePluginExtension sparklePluginExtension

	@BeforeMethod
	void setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		sparklePluginExtension = new SparklePluginExtension(project)
		sparklePluginExtension.appName = "Test App"
	}

	@Test
	void testFullAppName() {

		assert sparklePluginExtension.getFullAppName().equals("Test App.app")
	}

	@Test
	void testAppDirectory() {

		File appDirectory = new File(projectDir.absolutePath + "/build/codesign/Test App.app")

		assert sparklePluginExtension.getAppDirectory().equals(appDirectory)
	}
}