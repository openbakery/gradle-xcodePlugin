package org.openbakery

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

	@BeforeMethod
	def setup() {
		/*
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)


*/

		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin


		xcodeTestTask = project.tasks.findByName('test')
		//xcodeTestTask.setProperty("commandRunner", commandRunnerMock)

	}


	@Test
	void testXMLOuput() {


		Destination destination = new Destination()
		destination.platform = "iPhoneSimulator"
		destination.name = "iPad"
		destination.arch = "i386"
		destination.id = "iPad Retina"
		destination.os = "iOS"

		project.xcodebuild.destinations = []
		project.xcodebuild.destinations << destination;


		TestResult testResult = new TestResult()
		testResult.success = true
		testResult.duration = 0.1
		testResult.method = "testSomething"


		TestClass testClass = new TestClass();
		testClass.name = "HelloWorldTest"
		testClass.results << testResult

		def allResults = [:]
		def resultList = []
		resultList << testClass

		allResults.put(destination, resultList)


		xcodeTestTask.store(allResults)


	}


}
