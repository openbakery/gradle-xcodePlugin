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
class SimulatorsCleanTaskTest {


	SimulatorsCleanTask task
	Project project
	File projectDir

	@Before
	void setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.tasks.findByName(XcodePlugin.SIMULATORS_CLEAN_TASK_NAME)

	}

	@Test
	void create() {
		assert task instanceof SimulatorsCleanTask
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
		mock.demand.eraseAll{}

		task.simulatorControl = mock.proxyInstance()

		task.run()

		mock.verify task.simulatorControl


	}
}
