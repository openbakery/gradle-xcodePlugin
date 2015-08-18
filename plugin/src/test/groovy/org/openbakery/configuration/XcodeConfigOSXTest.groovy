package org.openbakery.configuration

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.openbakery.internal.XcodeBuildSpec
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 10.03.15.
 */
class XcodeConfigOSXTest {

	XcodeConfig xcodeConfig
	Project project
	GMockController mockControl
	CommandRunner commandRunnerMock
	XcodeBuildSpec buildSpec



	@BeforeMethod
	def setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)


		File projectDir = new File("../example/OSX/ExampleOSX")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")


		buildSpec = new XcodeBuildSpec(project, buildSpec)
		buildSpec.target = "ExampleOSX"

		xcodeConfig = new XcodeConfig(project, buildSpec)
		xcodeConfig.setProperty("commandRunner", commandRunnerMock)


	}

	@AfterMethod
	def cleanup() {
		FileUtils.deleteDirectory(new File("build/Platforms"))
		FileUtils.deleteDirectory(new File("build/Contents"))
	}


	@Test
	void testOSX() {
		xcodeConfig.configuration()
		assert xcodeConfig.xcodeProjectFile.isOSX
	}

}
