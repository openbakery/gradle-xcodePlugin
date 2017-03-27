package org.openbakery.simulators

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodePlugin
import org.openbakery.codesign.Codesign
import spock.lang.Specification

/**
 * Created by rene on 27.03.17.
 */
class SimulatorInstallAppTaskSpecification extends Specification {


	SimulatorInstallAppTask task
	Project project
	File projectDir

	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.tasks.findByName(XcodePlugin.SIMULATORS_INSTALL_APP_TASK_NAME)

	}
	def "create"() {
		expect:
		task instanceof SimulatorInstallAppTask
		task.simulatorControl instanceof SimulatorControl
	}

	def "depends on"() {
		when:
		def dependsOn  = task.getDependsOn()
		then:
		dependsOn.size() == 3
		dependsOn.contains(XcodePlugin.XCODE_BUILD_TASK_NAME)
		dependsOn.contains(XcodePlugin.SIMULATORS_START_TASK_NAME)
	}

	def "run"() {
		given:
		task.codesign = Mock(Codesign)
		SimulatorControl simulatorControl = Mock(SimulatorControl)
		task.simulatorControl = simulatorControl

		project.xcodebuild.bundleName = "MyApp"

		when:
		task.run()

		then:
		1* simulatorControl.simctl(["install", "booted", project.xcodebuild.applicationBundle.absolutePath])
	}

	def "codesign is not null"() {
		expect:
		task.codesign instanceof Codesign
	}

	def "sign before install"() {
		given:
		Codesign codesign = Mock(Codesign)
		task.codesign = codesign

		SimulatorControl simulatorControl = Mock(SimulatorControl)
		task.simulatorControl = simulatorControl

		project.xcodebuild.bundleName = "MyApp"

		when:
		task.run()

		then:
		1 * codesign.sign(project.xcodebuild.applicationBundle)


	}


}
