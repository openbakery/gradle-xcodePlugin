package org.openbakery.simulators

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.openbakery.bundle.Bundle
import org.openbakery.codesign.Codesign
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Type
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
		dependsOn.size() == 2
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
		1* simulatorControl.simctlWithResult(["install", "booted", project.xcodebuild.applicationBundle.absolutePath])
	}

	def "codesign is not null"() {
		expect:
		task.codesign instanceof Codesign
	}

	def "codesign parameters is iOS"() {
		given:
		project.xcodebuild.type = Type.iOS

		when:
		def codesign = task.getCodesign()

		then:
		codesign.codesignParameters.type == Type.iOS
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
		1 * codesign.sign(new Bundle(project.xcodebuild.applicationBundle, Type.iOS, new PlistHelper(new CommandRunner())))
	}



}
