package org.openbakery.configuration

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.openbakery.internal.XcodeBuildSpec
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created by rene on 10.03.15.
 */
class XcodeConfigOSXTest {

	XcodeConfig xcodeConfig
	Project project
	GMockController mockControl
	CommandRunner commandRunnerMock
	XcodeBuildSpec buildSpec



	@Before
	void setup() {
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

	@After
	void cleanup() {
		FileUtils.deleteDirectory(new File("build/Platforms"))
		FileUtils.deleteDirectory(new File("build/Contents"))
	}


	@Test
	void testOSX() {
		xcodeConfig.configuration()
		assert xcodeConfig.xcodeProjectFile.isOSX
	}

}
