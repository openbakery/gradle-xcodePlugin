package org.openbakery.simulators

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodePlugin
import spock.lang.Specification

class SimulatorsCreateTaskSpecification extends Specification {


	SimulatorsCreateTask task
	Project project
	File projectDir

	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.tasks.findByName(XcodePlugin.SIMULATORS_CREATE_TASK_NAME)

	}


	def "create"() {
		expect:
		task instanceof SimulatorsCreateTask
		task.simulatorControl instanceof SimulatorControl
	}

	def "depends on nothing"() {
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
		1 * simulatorControl.killAll()
		1 * simulatorControl.deleteAll()
		1 * simulatorControl.createAll()
	}
}
