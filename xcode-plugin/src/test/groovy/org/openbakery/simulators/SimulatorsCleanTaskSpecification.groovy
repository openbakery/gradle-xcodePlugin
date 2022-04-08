package org.openbakery.simulators

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodePlugin
import spock.lang.Specification

class SimulatorsCleanTaskSpecification extends Specification {


	SimulatorsCleanTask task
	Project project
	File projectDir

	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.tasks.findByName(XcodePlugin.SIMULATORS_CLEAN_TASK_NAME)

	}

	def "create"() {
		expect:
		task instanceof SimulatorsCleanTask
		task.simulatorControl instanceof SimulatorControl
	}



	def "depends on noting"() {
		when:
		def dependsOn  = task.getDependsOn()
		then:
		dependsOn.size() == 0
	}

	def "run"() {
		given:
		SimulatorControl simulatorControl = Mock(SimulatorControl)
		task.simulatorControl = simulatorControl

		when:
		task.run()

		then:
		1* simulatorControl.eraseAll()
	}
}
