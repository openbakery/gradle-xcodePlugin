package org.openbakery.simulators

import groovy.mock.interceptor.MockFor
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodePlugin
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by rene on 01.09.15.
 */
class SimulatorsStartTaskTest {


	SimulatorsStartTask task
	Project project
	File projectDir


	@BeforeMethod
	void setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.tasks.findByName(XcodePlugin.SIMULATORS_START_TASK_NAME)

	}


	@Test
	void create() {
		assert task instanceof SimulatorsStartTask
		assert task.simulatorControl instanceof SimulatorControl
	}


	@Test
	void run() {

		def mock = new MockFor(SimulatorControl)
		mock.demand.getRuntimes{ [ new SimulatorRuntime("iOS 8.0 (8.0 - 12A4331d) (com.apple.CoreSimulator.SimRuntime.iOS-8-0)")  ]}
		SimulatorDevice device = new SimulatorDevice("iPhone 4s (355CCB20-9CC7-4430-AD15-FC494B7EC465) (Shutdown)")
		mock.demand.getDevices{ [ device ]}
		mock.demand.killAll{}
		mock.demand.runDevice { parameters ->
			assertThat(parameters, is(equalTo(device)));
		}
		mock.demand.waitForDevice{}

		task.simulatorControl = mock.proxyInstance()
		task.run()

		mock.verify task.simulatorControl
	}



	@Test
	void hasNoRuntime() {
		def mock = new MockFor(SimulatorControl)
		mock.demand.getRuntimes{[]}

		task.simulatorControl = mock.proxyInstance()
		task.run()

		mock.verify task.simulatorControl
	}


	@Test
	void hasNoDevice() {

		def mock = new MockFor(SimulatorControl)
		mock.demand.getRuntimes{ [ new SimulatorRuntime("iOS 8.0 (8.0 - 12A4331d) (com.apple.CoreSimulator.SimRuntime.iOS-8-0)")  ]}
		mock.demand.getDevices{ []}

		task.simulatorControl = mock.proxyInstance()
		task.run()

		mock.verify task.simulatorControl
	}

}
