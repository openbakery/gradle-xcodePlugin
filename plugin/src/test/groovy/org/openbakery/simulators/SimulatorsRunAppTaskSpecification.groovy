package org.openbakery.simulators

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.xcode.Type
import org.openbakery.XcodePlugin
import org.openbakery.testdouble.PlistHelperStub
import spock.lang.Specification

class SimulatorsRunAppTaskSpecification extends Specification {

	SimulatorRunAppTask task
	Project project
	File projectDir


	def "setup"() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.tasks.findByName(XcodePlugin.SIMULATORS_RUN_APP_TASK_NAME)

	}


	def create() {
		expect:
		task instanceof SimulatorRunAppTask
		task.simulatorControl instanceof SimulatorControl
	}


	def "hasNoInfoPlist"() {
		when:
		task.run()

		then:
		thrown(IllegalArgumentException.class)

	}


	def "no simulator SDK"() {
		given:
		project.xcodebuild.infoPlist =  "Info.plist"
		project.xcodebuild.type = Type.macOS
		when:
		task.run()

		then:
		thrown(IllegalArgumentException.class)

	}

	def "bundleIdentifier is null"() {
		given:
		project.xcodebuild.infoPlist =  "Info.plist"

		when:
		task.run()

		then:
		thrown(IllegalArgumentException.class)
	}

	def "run"() {
		given:
		project.xcodebuild.infoPlist =  "Info.plist"

		SimulatorControl simulatorControl = Mock(SimulatorControl)
		task.simulatorControl = simulatorControl

		task.plistHelper = new PlistHelperStub([
						CFBundleIdentifier: "com.example.Example"
		])

		when:
		task.run()

		then:
		1 * simulatorControl.simctlWithResult("launch", "booted", "com.example.Example")
	}


}
