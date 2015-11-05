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
		1* simulatorControl.eraseAll()


	}
}
