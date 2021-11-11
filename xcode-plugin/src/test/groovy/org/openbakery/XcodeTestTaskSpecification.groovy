package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.simulators.SimulatorControl
import org.openbakery.test.TestResultParser
import org.openbakery.testdouble.SimulatorControlFake
import org.openbakery.xcode.Destination
import org.openbakery.xcode.Type
import spock.lang.Specification

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

		xcodeTestTask.xcode.commandRunner = commandRunner

		xcodeTestTask.destinationResolver.simulatorControl = new SimulatorControlFake("simctl-list-xcode7.txt");

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

	def mockXcodeVersionAndPath() {
		commandRunner.runWithResult("xcodebuild", "-version") >> ("Xcode 7.2.1\nBuild version 7C1002")
		commandRunner.runWithResult("xcode-select", "-p") >> ("/Applications/Xcode.app/Contents/Developer")
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


	def expectedCodesignSettings() {
		return [
						"CODE_SIGN_IDENTITY=",
						"CODE_SIGNING_REQUIRED=NO",
						"CODE_SIGNING_ALLOWED=NO"
		]
	}

	def "has xcode"() {
		expect:
		xcodeTestTask.xcode != null
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
		dependsOn.contains(XcodePlugin.COCOAPODS_INSTALL_TASK_NAME)
		dependsOn.contains(XcodePlugin.CARTHAGE_BOOTSTRAP_TASK_NAME)
	}



	def "test command without simulator"() {
		project.xcodebuild.commandRunner = commandRunner

		def commandList
		def expectedCommandList

		project.xcodebuild.type = 'macOS'
		project.xcodebuild.target = 'Test'
		mockXcodeVersionAndPath()


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
														 ]
			expectedCommandList.addAll(expectedCodesignSettings())
			expectedCommandList.addAll([
														 "-destination",
														 "platform=OS X,arch=x86_64",
														 "DSTROOT=" + new File(project.buildDir, "dst").absolutePath,
														 "OBJROOT=" + new File(project.buildDir, "obj").absolutePath,
														 "SYMROOT=" + new File(project.buildDir, "sym").absolutePath,
														 "SHARED_PRECOMPS_DIR=" + new File(project.buildDir, "shared").absolutePath
			])
			expectedCommandList << "-enableCodeCoverage" << "yes"
		}
		Collections.indexOfSubList(commandList, expectedCommandList) == 0
		commandList.removeLast() == "test"

	}

	def setup_iOS_SimulatorBuild(String... commands) {
		project.xcodebuild.type = Type.iOS

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'

		def expectedCommandList = ['script', '-q', '/dev/null',
															 "xcodebuild",
															 "-scheme", 'myscheme',
															 "-workspace", "myworkspace",
															 "-configuration", 'Debug',
		]
		//expectedCommandList.addAll(expectedCodesignSettings())
		expectedCommandList.addAll(commands)
		expectedCommandList.addAll(expectedDefaultDirectories())
		return expectedCommandList
	}

	def "test command for iOS simulator"() {
		project.xcodebuild.commandRunner = commandRunner
		def commandList
		def expectedCommandList = setup_iOS_SimulatorBuild(
						"-destination", "platform=iOS Simulator,id=83384347-6976-4E70-A54F-1CFECD1E02B1",
						"-destination", "platform=iOS Simulator,id=5C8E1FF3-47B7-48B8-96E9-A12740DBC58A"
		)
		mockXcodeVersionAndPath()

		when:
		xcodeTestTask.test()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		interaction {
			expectedCommandList << "-enableCodeCoverage" << "yes"
		}
		Collections.indexOfSubList(commandList, expectedCommandList) == 0
		commandList.removeLast() == "test"
	}


	def "test command with coverage settings Xcode 6"() {
		project.xcodebuild.commandRunner = commandRunner
		def commandList
		def expectedCommandList = setup_iOS_SimulatorBuild(
						"-destination", "platform=iOS Simulator,id=83384347-6976-4E70-A54F-1CFECD1E02B1",
						"-destination", "platform=iOS Simulator,id=5C8E1FF3-47B7-48B8-96E9-A12740DBC58A"
		)
		commandRunner.runWithResult("xcodebuild", "-version") >> ("Xcode 6.4\nBuild version 6E35b")
		mockXcodeVersionAndPath()

		when:
		xcodeTestTask.test()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		interaction {
			expectedCommandList << "GCC_INSTRUMENT_PROGRAM_FLOW_ARCS=YES"
			expectedCommandList << "GCC_GENERATE_TEST_COVERAGE_FILES=YES"
		}
		Collections.indexOfSubList(commandList, expectedCommandList) == 0
		commandList.removeLast() == "test"

	}


	def setupOSXBuild(String... commands) {
		project.xcodebuild.type = Type.macOS
		project.xcodebuild.target = 'Test';

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'

		def expectedCommandList = ['script', '-q', '/dev/null',
															 "xcodebuild",
															 "-scheme", 'myscheme',
															 "-workspace", "myworkspace",
															 "-configuration", 'Debug',
		]
		expectedCommandList.addAll(expectedCodesignSettings())
		expectedCommandList.addAll(commands)
		expectedCommandList.addAll(expectedDefaultDirectories())
		return expectedCommandList
	}

	def "test command with coverage for OS X"() {
		project.xcodebuild.commandRunner = commandRunner
		def commandList
		def expectedCommandList = setupOSXBuild("-destination","platform=OS X,arch=x86_64")
		mockXcodeVersionAndPath()


		when:
		xcodeTestTask.test()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		interaction {
			expectedCommandList << "-enableCodeCoverage" << "yes"
		}
		Collections.indexOfSubList(commandList, expectedCommandList) == 0
		commandList.removeLast() == "test"

	}

	def "test command with coverage for OSX using Xcode 6"() {
		project.xcodebuild.commandRunner = commandRunner
		def commandList
		def expectedCommandList = setupOSXBuild(
						"-destination", "platform=OS X,arch=x86_64"
		)

		commandRunner.runWithResult("xcodebuild", "-version") >> ("Xcode 6.4\nBuild version 6E35b")
		mockXcodeVersionAndPath()

		when:
		xcodeTestTask.test()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		interaction {
			expectedCommandList << "GCC_INSTRUMENT_PROGRAM_FLOW_ARCS=YES"
			expectedCommandList << "GCC_GENERATE_TEST_COVERAGE_FILES=YES"
		}
		Collections.indexOfSubList(commandList, expectedCommandList) == 0
		commandList.removeLast() == "test"

	}


	def "output file was set"() {
		def givenOutputFile
		project.xcodebuild.target = "Test"
		mockXcodeVersionAndPath()
		commandRunner.runWithResult("xcodebuild", "-version") >> ("Xcode 6.4\nBuild version 6E35b")

		when:
		xcodeTestTask.test()


		then:
		1 * commandRunner.setOutputFile(_) >> { arguments -> givenOutputFile = arguments[0] }
		givenOutputFile.absolutePath.endsWith("xcodebuild-output.txt")
		givenOutputFile == new File(project.getBuildDir(), "test/xcodebuild-output.txt")

	}

	def "build directory is created"() {
		project.xcodebuild.target = "Test"
		mockXcodeVersionAndPath()
		commandRunner.runWithResult("xcodebuild", "-version") >> ("Xcode 6.4\nBuild version 6E35b")

		when:
		xcodeTestTask.test()

		then:
		project.getBuildDir().exists()
	}

	def "set target"() {
		when:
		xcodeTestTask.target = "target"

		then:
		xcodeTestTask.parameters.target == "target"
	}


	def "set scheme"() {
		when:
		xcodeTestTask.scheme = "scheme"

		then:
		xcodeTestTask.parameters.scheme == "scheme"
	}


	def "set simulator"() {
		when:
		xcodeTestTask.simulator = true

		then:
		xcodeTestTask.parameters.simulator == true
	}

	def "set simulator false"() {
		when:
		xcodeTestTask.simulator = false

		then:
		xcodeTestTask.parameters.simulator == false
	}

	def "set type"() {
		when:
		xcodeTestTask.type = Type.iOS

		then:
		xcodeTestTask.parameters.type == Type.iOS
	}

	def "set workspace"() {
		when:
		xcodeTestTask.workspace = "workspace"

		then:
		xcodeTestTask.parameters.workspace == "workspace"
	}

	def "set additionalParameters"() {
		when:
		xcodeTestTask.additionalParameters = "additionalParameters"

		then:
		xcodeTestTask.parameters.additionalParameters == "additionalParameters"
	}

	def "set configuration"() {
		when:
		xcodeTestTask.configuration = "configuration"

		then:
		xcodeTestTask.parameters.configuration == "configuration"
	}

	def "set arch"() {
		when:
		xcodeTestTask.arch = ["i386"]

		then:
		xcodeTestTask.parameters.arch == ["i386"]
	}


	def "set configuredDestinations"() {
		when:
		Destination destination = new Destination()
		Set<Destination> destinations = [] as Set
		destinations.add(destination)
		xcodeTestTask.configuredDestinations = destinations

		then:
		xcodeTestTask.parameters.configuredDestinations.size() == 1
		xcodeTestTask.parameters.configuredDestinations[0] == destination
	}


	def "test command for iOS simulator with override target and destination"() {
		project.xcodebuild.commandRunner = commandRunner
		def commandList

		project.xcodebuild.type = Type.iOS
		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		xcodeTestTask.scheme = "Foobar"
		xcodeTestTask.destination {
			name = "iPad 2"
		}

		def expectedCommandList = ['script', '-q', '/dev/null',
															 "xcodebuild",
															 "-scheme", "Foobar",
															 "-workspace", "myworkspace",
															 "-configuration", 'Debug',
		]
		expectedCommandList.addAll(["-destination", "platform=iOS Simulator,id=83384347-6976-4E70-A54F-1CFECD1E02B1"])
		expectedCommandList.addAll(expectedDefaultDirectories())

		mockXcodeVersionAndPath()



		when:
		xcodeTestTask.test()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		interaction {
			expectedCommandList << "-enableCodeCoverage" << "yes"
		}
		Collections.indexOfSubList(commandList, expectedCommandList) == 0
		commandList.removeLast() == "test"

		expectedCommandList.contains("Foobar")
	}




	def "delete derivedData/Logs/Test before test is executed"() {
		mockXcodeVersionAndPath()
		project.xcodebuild.target = "Test"

		def testDirectory = new File(project.xcodebuild.derivedDataPath, "Logs/Test")
		FileUtils.writeStringToFile(new File(testDirectory, "foobar"), "dummy");

		when:
		xcodeTestTask.test()

		then:
		!testDirectory.exists()
	}


	def "parse test-result.xml gets stored"() {
		given:

		mockXcodeVersionAndPath()
		project.xcodebuild.target = "Test"

		when:
		xcodeTestTask.test()

		def testResult = new File(outputDirectory, "test-results.xml")
		then:
		testResult.exists()
	}


}
