package org.openbakery

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.output.XcodeBuildOutputAppender
import org.openbakery.testdouble.SimulatorControlStub
import org.openbakery.testdouble.XcodeFake
import org.openbakery.xcode.DestinationResolver
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcodebuild
import spock.lang.Specification

/**
 * Created by rene on 25.10.16.
 */
class XcodeBuildForTestTaskSpecification extends Specification {

	Project project
	CommandRunner commandRunner = Mock(CommandRunner);


	XcodeBuildForTestTask xcodeBuildForTestTask

	def setup() {
		project = ProjectBuilder.builder().build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild/build")

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeBuildForTestTask = project.getTasks().getByPath(XcodePlugin.XCODE_BUILD_FOR_TEST_TASK_NAME)
		xcodeBuildForTestTask.commandRunner = commandRunner
		xcodeBuildForTestTask.xcode = new XcodeFake()
		xcodeBuildForTestTask.destinationResolver = new DestinationResolver(new SimulatorControlStub("simctl-list-xcode7.txt"))
	}



	def "instance is of type XcodeBuildForTestTask"() {
		expect:
		xcodeBuildForTestTask instanceof  XcodeBuildForTestTask

	}


	def "depends on"() {
		when:
		def dependsOn = xcodeBuildForTestTask.getDependsOn()

		then:
		dependsOn.size() == 3
		dependsOn.contains(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		dependsOn.contains(XcodePlugin.SIMULATORS_KILL_TASK_NAME)
	}


	def "has xcodebuild"() {
		expect:
		xcodeBuildForTestTask.xcodebuild instanceof Xcodebuild
	}


	def "xcodebuild has merged parameters"() {
		when:
		project.xcodebuild.target = "Test"

		xcodeBuildForTestTask.buildForTest()

		then:
		xcodeBuildForTestTask.parameters.target == "Test"
	}

	def "output directory is build-for-testing"() {
		def givenOutputFile

		when:
		project.xcodebuild.target = "Test"
		xcodeBuildForTestTask.buildForTest()

		then:
		1 * commandRunner.setOutputFile(_) >> { arguments -> givenOutputFile = arguments[0] }
		givenOutputFile == new File(project.getBuildDir(), "for-testing/xcodebuild-output.txt")
	}


	def "IllegalArgumentException_when_no_scheme_or_target_given"() {
		when:
		xcodeBuildForTestTask.buildForTest()

		then:
		thrown(IllegalArgumentException)
	}


	def "xcodebuild is exectued"() {
		def commandList
		project.xcodebuild.scheme = 'myscheme'

		when:
		xcodeBuildForTestTask.buildForTest()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		commandList.contains("build-for-testing")
		commandList.contains("myscheme")

	}

	def "has output appender"() {
		def outputAppender
		project.xcodebuild.scheme = 'myscheme'

		when:
		xcodeBuildForTestTask.buildForTest()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> outputAppender = arguments[3] }
		outputAppender instanceof XcodeBuildOutputAppender
	}
}
