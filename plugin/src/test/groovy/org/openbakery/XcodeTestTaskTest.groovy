package org.openbakery

import org.apache.commons.lang.StringUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 01.07.14.
 */
class XcodeTestTaskTest {



	Project project
	XcodeTestTask xcodeTestTask

	GMockController mockControl
	CommandRunner commandRunnerMock
	Destination destination

	@BeforeMethod
	def setup() {
		/*
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)


*/

		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin


		xcodeTestTask = project.tasks.findByName('test-xcode')

		xcodeTestTask.setOutputDirectory(new File("build/test"));

		//xcodeTestTask.setProperty("commandRunner", commandRunnerMock)

		destination = new Destination()
		destination.platform = "iPhoneSimulator"
		destination.name = "iPad"
		destination.arch = "i386"
		destination.id = "iPad Retina"
		destination.os = "iOS"


		project.xcodebuild.availableDevices = []
		project.xcodebuild.availableSimulators << destination
		project.xcodebuild.destinations << destination

	}

	TestResult testResult(String name, boolean success) {
		TestResult testResult = new TestResult()
		testResult.success = success
		testResult.duration = 0.1
		testResult.method = name
		return testResult;
	}

	@Test
	void createXMLOuput() {


		TestClass testClass = new TestClass();
		testClass.name = "HelloWorldTest"
		for (int i=0; i<5; i++) {
			testClass.results << testResult("testSuccess_" + i, true)
		}
		for (int i=0; i<3; i++) {
			testClass.results << testResult("testError_" + i, false)
		}

		def allResults = [:]
		def resultList = []
		resultList << testClass

		allResults.put(destination, resultList)


		xcodeTestTask.store(allResults)

		String testXML = new File('build/test/test-results.xml').text

		assert StringUtils.countMatches(testXML, "<testcase") == 8

		assert StringUtils.countMatches(testXML, "<error type='failure'") == 3


	}

	@Test
	void parseWithNoResult() {
		def allResults = [:]
		allResults.put(destination, null)
		xcodeTestTask.store(allResults)

	}


}
