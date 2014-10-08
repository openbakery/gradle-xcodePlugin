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

	Destination destinationPad
	Destination destinationPhone

	@BeforeMethod
	def setup() {


		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin


		xcodeTestTask = project.tasks.findByName('test')

		xcodeTestTask.setOutputDirectory(new File("build/test"));

		destinationPad = new Destination()
		destinationPad.platform = "iPhoneSimulator"
		destinationPad.name = "iPad"
		destinationPad.arch = "i386"
		destinationPad.id = "iPad Air"
		destinationPad.os = "iOS"

		destinationPhone = new Destination()
		destinationPhone.platform = "iPhoneSimulator"
		destinationPhone.name = "iPhone"
		destinationPhone.arch = "i386"
		destinationPhone.id = "iPhone 4s"
		destinationPhone.os = "iOS"


		project.xcodebuild.availableDevices = []
		project.xcodebuild.availableSimulators << destinationPad
		project.xcodebuild.availableSimulators << destinationPhone

		project.xcodebuild.destination {
			name = "iPad"
		}
		project.xcodebuild.destination {
			name = "iPhone"
		}

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

		allResults.put(destinationPad, resultList)
		allResults.put(destinationPhone, resultList)


		xcodeTestTask.store(allResults)

		String testXML = new File('build/test/test-results.xml').text

		assert StringUtils.countMatches(testXML, "<testcase") == 16

		assert StringUtils.countMatches(testXML, "<error type='failure'") == 6


	}

	@Test
	void parseWithNoResult() {
		def allResults = [:]
		allResults.put(destinationPad, null)
		xcodeTestTask.store(allResults)

	}

	@Test
	void parseSuccessResult() {
		assert xcodeTestTask.parseResult(new File("src/test/Resource/xcodebuild-output.txt"))

		assert xcodeTestTask.numberSuccess(xcodeTestTask.allResults) == 2
		assert xcodeTestTask.numberErrors(xcodeTestTask.allResults) == 0


	}

	@Test
	void parseFailureResult() {
		assert !xcodeTestTask.parseResult(new File("src/test/Resource/xcodebuild-output-test-failed.txt"))

		assert xcodeTestTask.numberSuccess(xcodeTestTask.allResults) == 0
		assert xcodeTestTask.numberErrors(xcodeTestTask.allResults) == 2
	}


}
