package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.testdouble.SimulatorControlFake
import org.openbakery.testdouble.SimulatorControlStub
import org.openbakery.testdouble.XcodeFake
import org.openbakery.xcode.Destination
import org.openbakery.xcode.DestinationResolver
import org.openbakery.xcode.Type
import org.openbakery.xcode.XcodebuildParameters
import spock.lang.Specification

class XcodeTestRunTask_TestResult_Specification extends Specification {

	Project project
	CommandRunner commandRunner = Mock(CommandRunner);

	XcodeTestRunTask xcodeTestRunTestTask
	File tmpDir
	File outputDirectory



	def setup() {
		tmpDir = new File(System.getProperty("java.io.tmpdir"), "gxp")
		File projectDir = new File(tmpDir, "gradle-projectDir")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeTestRunTestTask = project.getTasks().getByPath(XcodePlugin.XCODE_TEST_RUN_TASK_NAME)

		outputDirectory = new File(project.buildDir, "test")
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}

		xcodeTestRunTestTask.setOutputDirectory(outputDirectory);
		xcodeTestRunTestTask.destination = getDestinations("simctl-list-xcode7.txt")

		project.xcodebuild.target = "Test"
	}

	def cleanup() {
		FileUtils.deleteDirectory(tmpDir)
	}


	List<Destination> getDestinations(String simctlList) {
		File file = new File("../libxcode/src/test/Resource/", simctlList)
		SimulatorControlFake simulatorControl = new SimulatorControlFake(file)
		XcodebuildParameters parameters = new XcodebuildParameters()
		parameters.simulator = true
		parameters.type = Type.iOS
		parameters.configuredDestinations = [
			new Destination("iPad 2"),
			new Destination("iPhone 4s"),
			new Destination("iPhone 8"),
		]

		DestinationResolver destinationResolver = new DestinationResolver(simulatorControl)
		return destinationResolver.getDestinations(parameters)
	}


	def "Run test prints success result"() {
		given:
		def message
		File testSummaryDirectory = new File("src/test/Resource/TestLogs/xcresult/Legacy/Success")
		def printLogger = Mock(org.gradle.api.logging.Logger)
		xcodeTestRunTestTask.printLogger = printLogger

		when:
		xcodeTestRunTestTask.processTestResult(testSummaryDirectory)

		then:
		1 * printLogger.lifecycle(_) >> {
				arguments ->
					message = arguments[0]
			}
		message ==	"All 37 tests were successful"
	}

	def "Run test prints failure result"() {
		given:
		def message
		File testSummaryDirectory = new File("src/test/Resource/TestLogs/xcresult/Legacy/Failure")
		def printLogger = Mock(org.gradle.api.logging.Logger)
		xcodeTestRunTestTask.printLogger = printLogger

		when:
		try {
			xcodeTestRunTestTask.processTestResult(testSummaryDirectory)
		} catch (Exception exception) {
			// expected
		}

		then:
		1 * printLogger.lifecycle(_) >> {
				arguments ->
					message = arguments[0]
			}
		message ==	"36 tests were successful, and 1 failed"
	}


	def "Run test prints skipped result"() {
		given:
		def message
		File testSummaryDirectory = new File("src/test/Resource/TestLogs/xcresult/Skipped")
		def printLogger = Mock(org.gradle.api.logging.Logger)
		xcodeTestRunTestTask.printLogger = printLogger

		Destination destination = new Destination("iPhone X")
		destination.id = "7B40DCDA-3380-4BB9-AB92-1E3D1AC7B3BB"
		xcodeTestRunTestTask.destination = destination

		when:
		try {
			xcodeTestRunTestTask.processTestResult(testSummaryDirectory)
		} catch (Exception exception) {
			// expected
		}

		then:
		1 * printLogger.lifecycle(_) >> {
				arguments ->
					message = arguments[0]
			}
		message ==	"1 test was successful, and 1 skipped"
	}

}
