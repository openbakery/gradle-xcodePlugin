package org.openbakery.configuration

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created by rene on 10.03.15.
 */
class XcodeConfigTaskOSXTest {

	XcodeConfigTask xcodeConfigTask
	Project project
	GMockController mockControl
	CommandRunner commandRunnerMock



	@Before
	void setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)


		File projectDir = new File("../example/OSX/ExampleOSX")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeConfigTask = project.getTasks().getByName(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		xcodeConfigTask.setProperty("commandRunner", commandRunnerMock)

		project.xcodebuild.target = "ExampleOSX"

	}

	@After
	void cleanup() {
		FileUtils.deleteDirectory(new File("build/Platforms"))
		FileUtils.deleteDirectory(new File("build/Contents"))
	}


	@Test
	void testOSX() {
		xcodeConfigTask.configuration()
		assert xcodeConfigTask.xcodeProjectFile.isOSX
	}

}
