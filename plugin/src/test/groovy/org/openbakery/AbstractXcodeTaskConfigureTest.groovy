package org.openbakery

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by rene on 18.08.15.
 */
class AbstractXcodeTaskConfigureTest {

	AbstractXcodeTask xcodeTask

	Project project

	@BeforeMethod
	def setup() {

		File projectDir = new File("../example/iOS/Example")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		xcodeTask = project.getTasks().getByPath(XcodePlugin.ARCHIVE_TASK_NAME)

	}

	@Test
	void hasInfoPlistSet() {
		xcodeTask.buildSpec.target = "Example"
		xcodeTask.configureTask()
		assertThat(xcodeTask.buildSpec.getInfoPlistFile().absolutePath, endsWith("example/iOS/Example/Example/Example-Info.plist"))


	}


}
