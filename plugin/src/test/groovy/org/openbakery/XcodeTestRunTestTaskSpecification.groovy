package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.output.TestBuildOutputAppender
import org.openbakery.output.XcodeBuildOutputAppender
import org.openbakery.testdouble.SimulatorControlStub
import org.openbakery.testdouble.XcodeFake
import org.openbakery.xcode.DestinationResolver
import spock.lang.Specification

/**
 * User: rene
 * Date: 25/10/16
 */
class XcodeTestRunTestTaskSpecification extends Specification {

	Project project
	CommandRunner commandRunner = Mock(CommandRunner);


	XcodeTestRunTestTask xcodeTestRunTestTask

	def setup() {
		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-projectDir")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeTestRunTestTask = project.getTasks().getByPath(XcodePlugin.XCODE_TEST_RUN_TASK_NAME)
		xcodeTestRunTestTask.commandRunner = commandRunner
		xcodeTestRunTestTask.xcode = new XcodeFake()
		xcodeTestRunTestTask.destinationResolver = new DestinationResolver(new SimulatorControlStub("simctl-list-xcode8.txt"))

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	def "instance is of type XcodeBuildForTestTask"() {
		expect:
		xcodeTestRunTestTask instanceof  XcodeTestRunTestTask
	}


	def "depends on nothing "() {
		when:
		def dependsOn = xcodeTestRunTestTask.getDependsOn()
		then:
		dependsOn.size() == 2
		dependsOn.contains(XcodePlugin.SIMULATORS_KILL_TASK_NAME)
	}


	def "set destinations"() {
		when:
		xcodeTestRunTestTask.destination = [
						"iPhone 6"
		]

		then:
		xcodeTestRunTestTask.parameters.configuredDestinations.size() == 1
		xcodeTestRunTestTask.parameters.configuredDestinations[0].name == "iPhone 6"
	}


	def "set destination global"() {
		when:
		project.xcodebuild.destination = [
						"iPhone 6"
		]

		xcodeTestRunTestTask.testRun()

		then:
		xcodeTestRunTestTask.parameters.configuredDestinations.size() == 1
		xcodeTestRunTestTask.parameters.configuredDestinations[0].name == "iPhone 6"

	}


	def "has bundle directory"() {
		when:
		xcodeTestRunTestTask.bundleDirectory  = "test"

		then:
		xcodeTestRunTestTask.bundleDirectory instanceof File
	}


	def createTestBundle(String directoryName) {
		File bundleDirectory = new File(project.getProjectDir(), directoryName)
		File testBundle = new File(bundleDirectory, "Example.testbundle")
		testBundle.mkdirs()
		File xctestrun = new File("src/test/Resource/Example_iphonesimulator.xctestrun")
		FileUtils.copyFile(xctestrun, new File(testBundle, "Example_iphonesimulator.xctestrun"))

	}


	def "set configure xctestrun"() {
		given:
		createTestBundle("test")

		when:
		xcodeTestRunTestTask.bundleDirectory  = "test"
		xcodeTestRunTestTask.testRun()

		then:
		xcodeTestRunTestTask.parameters.xctestrun instanceof List
		xcodeTestRunTestTask.parameters.xctestrun.size() == 1
		xcodeTestRunTestTask.parameters.xctestrun[0].path.endsWith("Example.testbundle/Example_iphonesimulator.xctestrun")
	}


	def "run xcodebuild executeTestWithoutBuilding"() {
		given:
		def commandList
		createTestBundle("test")

		when:
		xcodeTestRunTestTask.testRun()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		commandList.contains("test-without-building")
		commandList.contains("-xctestrun")

	}

	def "has output appender"() {
		def outputAppender

		when:
		xcodeTestRunTestTask.testRun()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> outputAppender = arguments[3] }
		outputAppender instanceof TestBuildOutputAppender
	}



}
