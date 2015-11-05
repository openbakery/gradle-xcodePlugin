package org.openbakery.simulators

import groovy.mock.interceptor.MockFor
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodePlugin
import org.junit.Before
import org.junit.Test
import spock.lang.Specification

/**
 * Created by rene on 30.04.15.
 */
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

	def "depends on"() {
		when:
		def dependsOn  = task.getDependsOn()
		then:
		dependsOn.contains(XcodePlugin.XCODE_CONFIG_TASK_NAME)
	}

	def "run"() {
		given:
		SimulatorControl simulatorControl = Mock(SimulatorControl)
		task.simulatorControl = simulatorControl

		when:
		task.run()

		then:
		1 * simulatorControl.deleteAll()
		1 * simulatorControl.createAll()
	}
}
