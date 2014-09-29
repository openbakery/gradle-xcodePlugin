package org.openbakery

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 29.09.14.
 */
class AbstractXcodeBuildTaskTest {

	Project project
	AbstractXcodeBuildTask xcodeBuildTask;


	@BeforeMethod
	def setup() {

		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin:org.openbakery.XcodePlugin

		xcodeBuildTask = project.getTasks().getByPath('build')
	}

	@Test
	void escapePath() {


		assert xcodeBuildTask.escapePath("/abc/test").equals("/abc/test");

		assert xcodeBuildTask.escapePath("/a b c/test").equals("/a\\\\\\ b\\\\\\ c/test");

	}

}
