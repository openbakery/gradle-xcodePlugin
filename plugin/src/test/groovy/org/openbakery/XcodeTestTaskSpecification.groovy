package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.simulators.SimulatorControl
import org.openbakery.stubs.SimulatorControlStub
import spock.lang.Specification

/**
 * Created by rene on 07.10.15.
 */
class XcodeTestTaskSpecification extends Specification {

	Project project

	XcodeTestTask xcodeTestTask


	CommandRunner commandRunner = Mock(CommandRunner)
	SimulatorControl simulatorControl = Mock(SimulatorControl)
	File outputDirectory
	Destination destinationPad
	Destination destinationPhone


	def setup() {

		project = ProjectBuilder.builder().build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), 'gradle-xcodebuild/build').absoluteFile


		project.apply plugin: org.openbakery.XcodePlugin


		xcodeTestTask = project.tasks.findByName(XcodePlugin.XCODE_TEST_TASK_NAME);
		xcodeTestTask.commandRunner = commandRunner
		xcodeTestTask.simulatorControl = simulatorControl

		project.xcodebuild.simulatorControl = new SimulatorControlStub("simctl-list-xcode7.txt");


		project.xcodebuild.destination {
			name = "iPad 2"
		}
		project.xcodebuild.destination {
			name = "iPhone 4s"
		}

		outputDirectory = new File(project.buildDir, "test");
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}
		xcodeTestTask.setOutputDirectory(outputDirectory);
		File xcodebuildOutput = new File(project.buildDir, 'test/xcodebuild-output.txt')
		FileUtils.writeStringToFile(xcodebuildOutput, "dummy")

/*
		expectedCommandList?.clear()
		expectedCommandList = ['script', '-q', '/dev/null', "xcodebuild"]
 */

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.buildDir)
	}

	def expectedDefaultDirectories() {
		return [
						"-derivedDataPath", new File(project.buildDir, "/derivedData").absolutePath,
						"DSTROOT=" + new File(project.buildDir, "dst").absolutePath,
						"OBJROOT=" + new File(project.buildDir, "obj").absolutePath,
						"SYMROOT=" + new File(project.buildDir, "sym").absolutePath,
						"SHARED_PRECOMPS_DIR=" + new File(project.buildDir, "shared").absolutePath
		]
	}


	/*


	def void addExpectedScheme() {
		project.xcodebuild.scheme = 'myscheme'
		expectedCommandList.add("-scheme")
		expectedCommandList.add(project.xcodebuild.scheme)

		project.xcodebuild.workspace = 'myworkspace'
		expectedCommandList.add("-workspace")
		expectedCommandList.add(project.xcodebuild.workspace)
	}

	def void addExpectedDefaultDirs() {
		String currentDir = new File('').getAbsolutePath()
		expectedCommandList.add("-derivedDataPath")
		expectedCommandList.add(currentDir + "${File.separator}build${File.separator}derivedData")
		expectedCommandList.add("DSTROOT=" + currentDir + "${File.separator}build${File.separator}dst")
		expectedCommandList.add("OBJROOT=" + currentDir + "${File.separator}build${File.separator}obj")
		expectedCommandList.add("SYMROOT=" + currentDir + "${File.separator}build${File.separator}sym")
		expectedCommandList.add("SHARED_PRECOMPS_DIR=" + currentDir + "${File.separator}build${File.separator}shared")
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


		File junitXmlSchema = new File('src/test/Resource/junit-4.xsd')
		def factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
		def schema = factory.newSchema(new StreamSource(new FileReader(junitXmlSchema)))
		def validator = schema.newValidator()
		validator.validate(new StreamSource(new StringReader(testXML)))

	}
	*/


	def "depends on"() {
		when:
		def dependsOn  = xcodeTestTask.getDependsOn()
		then:
		dependsOn.contains(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		dependsOn.contains(XcodePlugin.SIMULATORS_KILL_TASK_NAME)
	}

	def "parse with no result"() {
		def allResults = [:]
		SimulatorControlStub simulatorControl = new SimulatorControlStub("simctl-list-xcode7.txt");
		def destination = simulatorControl.getAllDestinations(Type.iOS)[0]
		allResults.put(destination, null)
		xcodeTestTask.allResults = allResults
		when:
		xcodeTestTask.store()
		then:
		true // no exception should be raised
	}



	def "parse success result"() {
		when:
		boolean success = xcodeTestTask.parseResult(new File("src/test/Resource/xcodebuild-output.txt"))

		then:
		success == true
		xcodeTestTask.numberSuccess() == 2
		xcodeTestTask.numberErrors() == 0
	}


	def "parse failure result"() {
		when:
		boolean success = xcodeTestTask.parseResult(new File("src/test/Resource/xcodebuild-output-test-failed.txt"))

		then:
		success == false
		xcodeTestTask.numberSuccess() == 0
		xcodeTestTask.numberErrors() == 2
	}




	def "parse failure result with partial suite"() {
		when:
		boolean success = xcodeTestTask.parseResult(new File("src/test/Resource/xcodebuild-output-test-failed-partial.txt"))

		then:
		success == false
		xcodeTestTask.numberSuccess() == 0
		xcodeTestTask.numberErrors() == 2
	}


	def "parse success result xcode 6.1"() {
		when:
		boolean success = xcodeTestTask.parseResult(new File("src/test/Resource/xcodebuild-output-xcode6_1.txt"))

		then:
		success == true
		xcodeTestTask.numberSuccess() == 8
		xcodeTestTask.numberErrors() == 0
	}

	def "parse complex test output"() {

		when:
		boolean success = xcodeTestTask.parseResult(new File("src/test/Resource/xcodebuild-output-complex-test.txt"))

		then:
		success == true
		xcodeTestTask.numberErrors() == 0
	}

/*
	def "compile error test"() {

		when:
		String result = xcodeTestTask.getFailureFromLog(new File("src/test/Resource/xcodebuild-output-test-compile-error.txt"))

		then:
		result.startsWith("Testing failed:");
		result.split("\n").length == 8

	}
*/


	def "parse success result for tests written in swift using Xcode 6.1"() {
		when:
		boolean success = xcodeTestTask.parseResult(new File("src/test/Resource/xcodebuild-output-swift-tests-xcode6_1.txt"))

		then:
		success == true
		xcodeTestTask.numberSuccess() == 2
		xcodeTestTask.numberErrors() == 0
	}




	def "test command without simulator"() {
		def commandList
		def expectedCommandList

		project.xcodebuild.type = 'OSX'
		project.xcodebuild.target = 'Test';


		when:
		xcodeTestTask.test()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		0 * simulatorControl.killAll()

		interaction {
			expectedCommandList = ['script', '-q', '/dev/null',
														 "xcodebuild",
														 "-configuration", 'Debug',
														 "-sdk", "macosx",
														 "-target", 'Test',
														 "CODE_SIGN_IDENTITY=",
														 "CODE_SIGNING_REQUIRED=NO"]
			expectedCommandList.addAll(expectedDefaultDirectories())
			expectedCommandList << "test"
		}
		commandList == expectedCommandList

	}

	def setup_iOS_SimualtorBuild() {
		project.xcodebuild.type = Type.iOS
		project.xcodebuild.target = 'Test';

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'

		def expectedCommandList = ['script', '-q', '/dev/null',
																 "xcodebuild",
																 "-scheme", 'myscheme',
																 "-workspace", "myworkspace",
																 "-configuration", 'Debug',
					]
					expectedCommandList.addAll(expectedDefaultDirectories())
		return expectedCommandList
	}

	def "test command for iOS simulator"() {
		def commandList
		def expectedCommandList = setup_iOS_SimualtorBuild()

		when:
		xcodeTestTask.test()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		interaction {
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=83384347-6976-4E70-A54F-1CFECD1E02B1"
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=5C8E1FF3-47B7-48B8-96E9-A12740DBC58A"
			expectedCommandList << "-enableCodeCoverage" << "yes"
			expectedCommandList << "test"
		}
		commandList == expectedCommandList
	}


	def "test command with coverage settings Xcode 6"() {
		project.xcodebuild.commandRunner = commandRunner
		def commandList
		def expectedCommandList = setup_iOS_SimualtorBuild()
		commandRunner.runWithResult("xcodebuild", "-version") >> ("Xcode 6.4\nBuild version 6E35b")


		when:
		xcodeTestTask.test()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		interaction {
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=83384347-6976-4E70-A54F-1CFECD1E02B1"
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=5C8E1FF3-47B7-48B8-96E9-A12740DBC58A"
			expectedCommandList << "GCC_INSTRUMENT_PROGRAM_FLOW_ARCS=YES"
			expectedCommandList << "GCC_GENERATE_TEST_COVERAGE_FILES=YES"
			expectedCommandList << "test"
		}
		commandList == expectedCommandList

	}



}
