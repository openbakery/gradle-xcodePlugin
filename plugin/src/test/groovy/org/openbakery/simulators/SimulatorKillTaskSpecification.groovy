package org.openbakery.simulators

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.XcodePlugin
import spock.lang.Specification

class SimulatorKillTaskSpecification extends Specification {

	SimulatorKillTask task
	Project project
	File projectDir

	def setup() {
		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.tasks.findByName(XcodePlugin.SIMULATORS_KILL_TASK_NAME)

	}


	def "create"() {
		expect:
		task instanceof SimulatorKillTask
		task.simulatorControl instanceof SimulatorControl
	}

	def "depends on nothing"() {
		when:
		def dependsOn = task.getDependsOn()
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
	}

	def "not enabled on OS X"() {
		when:
		project.xcodebuild.type = 'macOS'

		then:
		!task.getOnlyIf().isSatisfiedBy(task)
	}

	def "enabled on iOS"() {
		when:
		project.xcodebuild.type = 'iOS'
		project.xcodebuild.simulator = true

		then:
		task.getOnlyIf().isSatisfiedBy(task)
	}

	def "disabled on iOS Device"() {
		when:
		project.xcodebuild.type = 'iOS'
		project.xcodebuild.simulator = false

		then:
		!task.getOnlyIf().isSatisfiedBy(task)
	}

	def "enabled on tvOS"() {
		when:
		project.xcodebuild.type = 'tvOS'
		project.xcodebuild.simulator = true

		then:
		task.getOnlyIf().isSatisfiedBy(task)
	}

	def "disabled on tvOS"() {
		when:
		project.xcodebuild.type = 'tvOS'
		project.xcodebuild.simulator = false

		then:
		!task.getOnlyIf().isSatisfiedBy(task)
	}

	def "test command for iOS kill failed"() {

		given:
		def commandRunner = Mock(CommandRunner)
		task.simulatorControl.commandRunner = commandRunner

		commandRunner.run("killall", "iOS Simulator") >> { throw new CommandRunnerException("failed") }
		commandRunner.run("killall", "Simulator")


		when:
		task.run()

		then:
		true // no exception should be thrown

	}


}
