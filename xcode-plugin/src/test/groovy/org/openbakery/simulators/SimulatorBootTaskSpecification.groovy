package org.openbakery.simulators

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodePlugin
import org.openbakery.codesign.Codesign
import org.openbakery.testdouble.SimulatorControlFake

class SimulatorBootTaskSpecification extends spock.lang.Specification {

	SimulatorBootTask task
	Project project
	File projectDir
	SimulatorControlFake simulatorControlFake

	def setup() {
		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.tasks.findByName(XcodePlugin.SIMULATORS_BOOT_TASK_NAME) as SimulatorBootTask
		simulatorControlFake = new SimulatorControlFake()
		task.simulatorControl = simulatorControlFake
	}

	def "task was created"() {
		expect:
		task instanceof SimulatorBootTask
		task.simulatorControl instanceof SimulatorControl
	}


	def "execute task runs the proper simctl command for the iPhone 12 simulator"() {
		given:
		project.xcodebuild.destination = "iPhone 12"

		when:
		task.run()

		then:
		simulatorControlFake.lastExecutedCommand == ["boot", "CCDBE676-F230-430A-BA18-E6122A291572"]
	}


	def "execute task runs the proper simctl command for the iPad Pro (11-inch) simulator"() {
		given:
		project.xcodebuild.destination = "iPad Pro (11-inch) (2nd generation)"

		when:
		task.run()

		then:
		simulatorControlFake.lastExecutedCommand == ["boot", "E1EA6964-9EE9-4B92-929D-5A378C8F7BC6"]
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

}
