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

	Destination createDestination(String name, String id) {
		Destination destination = new Destination()
		destination.platform = "iPhoneSimulator"
		destination.name = name
		destination.arch = "i386"
		destination.id = id
		destination.os = "iOS"
		return destination
	}

	@BeforeMethod
	def setup() {


		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin


		xcodeTestTask = project.tasks.findByName('test-xcode')

		xcodeTestTask.setOutputDirectory(new File("build/test"));

		destinationPad = createDestination("iPad", "iPad Air")
		destinationPhone = createDestination("iPhone", "iPhone 4s")


		project.xcodebuild.availableSimulators << destinationPad
		project.xcodebuild.availableSimulators << destinationPhone

		project.xcodebuild.destination {
			name = destinationPad.name
		}
		project.xcodebuild.destination {
			name = destinationPhone.name
		}


		File outputDirectory = new File("build/test");
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
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

		xcodeTestTask.allResults = allResults
		xcodeTestTask.store()

		String testXML = new File('build/test/test-results.xml').text

		assert StringUtils.countMatches(testXML, "<testcase") == 16

		assert StringUtils.countMatches(testXML, "<error type='failure'") == 6


	}

	@Test
	void parseWithNoResult() {
		def allResults = [:]
		allResults.put(destinationPad, null)
		xcodeTestTask.allResults = allResults
		xcodeTestTask.store()

	}

	@Test
	void parseSuccessResult() {
		assert xcodeTestTask.parseResult(new File("src/test/Resource/xcodebuild-output.txt"))

		assert xcodeTestTask.numberSuccess() == 2
		assert xcodeTestTask.numberErrors() == 0


	}

	@Test
	void parseFailureResult() {
		assert !xcodeTestTask.parseResult(new File("src/test/Resource/xcodebuild-output-test-failed.txt"))

		assert xcodeTestTask.numberSuccess() == 0
		assert xcodeTestTask.numberErrors() == 2
	}


	@Test
	void parseSuccessResult_6_1() {
		assert xcodeTestTask.parseResult(new File("src/test/Resource/xcodebuild-output-xcode6_1.txt"))

		assert xcodeTestTask.numberSuccess() == 8
		assert xcodeTestTask.numberErrors() == 0


	}

	@Test
	void parseComplexTestOutput() {


		assert xcodeTestTask.parseResult(new File("src/test/Resource/xcodebuild-output-complex-test.txt"))

		assert xcodeTestTask.numberSuccess() == 60
		assert xcodeTestTask.numberErrors() == 0


	}

}
