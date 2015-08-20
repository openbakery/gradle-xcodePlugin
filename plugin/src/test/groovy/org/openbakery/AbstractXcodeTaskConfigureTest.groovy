package org.openbakery

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test
import org.openbakery.internal.XcodeBuildSpec

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by rene on 18.08.15.
 */
class AbstractXcodeTaskConfigureTest {

	AbstractXcodeTask xcodeTask

	Project project

	@Before
	void setup() {

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


	@Test
	void with() {
		XcodeBuildSpec newParent = new XcodeBuildSpec(project)
		xcodeTask.with(newParent)
		assertThat(xcodeTask.buildSpec.parent, is(newParent))
	}


}
