package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class XcodePlugin_Dependencies_Specification extends Specification {

	Project project
	File projectDir

	void setup() {
		projectDir = File.createTempDir()

		project = ProjectBuilder.builder()
				.withProjectDir(projectDir)
				.build()
		project.apply plugin: org.openbakery.XcodePlugin
		project.evaluate()

	}

	def cleanup() {
		FileUtils.deleteDirectory(projectDir)
	}


	def "not contain unknown task"() {
		expect:
		project.tasks.findByName('unknown-task') == null
	}


	def "clean has carthage clean dependency"() {
		when:
		Task cleanTask = project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME)
		Task carthageCleanTask = project.getTasks().getByName(XcodePlugin.CARTHAGE_CLEAN_TASK_NAME)
		then:
		cleanTask.getTaskDependencies().getDependencies() contains(carthageCleanTask)

	}



}
