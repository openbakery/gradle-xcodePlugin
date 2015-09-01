package org.openbakery.simulators

import groovy.mock.interceptor.MockFor
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.PlistHelper
import org.openbakery.XcodePlugin
import org.openbakery.stubs.PlistHelperStub
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by rene on 01.09.15.
 */
class SimulatorsRunAppTaskTest {

	SimulatorsRunAppTask task
	Project project
	File projectDir


	@BeforeMethod
	void setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.tasks.findByName(XcodePlugin.SIMULATORS_RUN_APP_TASK_NAME)

	}


	@Test
	void create() {
		assert task instanceof SimulatorsRunAppTask
		assert task.simulatorControl instanceof SimulatorControl
	}


	@Test(expectedExceptions = IllegalArgumentException)
	void hasNoInfoPlist() {
		task.run()
	}


	@Test(expectedExceptions = IllegalArgumentException)
	void noSimulatorSDK() {
		project.xcodebuild.infoPlist =  "Info.plist"
		project.xcodebuild.sdk = XcodePlugin.SDK_MACOSX
		task.run()
	}

	@Test(expectedExceptions = IllegalArgumentException)
	void bundleIdentifierIsNull() {
		project.xcodebuild.infoPlist =  "Info.plist"

		task.run()

	}

	@Test
	void run() {
		project.xcodebuild.infoPlist =  "Info.plist"

		def mock = new MockFor(SimulatorControl)
		mock.demand.simctl { p1, p2, p3 ->
			assertThat(p1, is("launch"));
			assertThat(p2, is("booted"));
			assertThat(p3, is("com.example.Example"));
		}

		task.simulatorControl = mock.proxyInstance()
		task.plistHelper = new PlistHelperStub([
						CFBundleIdentifier: "com.example.Example"
		])
		task.run()

		mock.verify task.simulatorControl
	}


}
