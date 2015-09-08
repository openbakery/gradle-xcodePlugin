package org.openbakery.simulators

import groovy.mock.interceptor.MockFor
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodePlugin
import org.junit.Before
import org.junit.Test

/**
 * Created by rene on 30.04.15.
 */
class SimulatorsCreateTaskTest {


	SimulatorsCreateTask task
	Project project
	File projectDir

	@Before
	void setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.tasks.findByName(XcodePlugin.SIMULATORS_CREATE_TASK_NAME)

	}


	@Test
	void create() {
		assert task instanceof SimulatorsCreateTask
		assert task.simulatorControl instanceof SimulatorControl
	}

	@Test
	void dependsOn() {
		def dependsOn  = task.getDependsOn()
		assert dependsOn.contains(XcodePlugin.XCODE_CONFIG_TASK_NAME)
	}

	@Test
	void run() {

		def mock = new MockFor(SimulatorControl)
		mock.demand.deleteAll{}
		mock.demand.createAll{}

		task.simulatorControl = mock.proxyInstance()


		task.run()

		mock.verify task.simulatorControl


	}
}
