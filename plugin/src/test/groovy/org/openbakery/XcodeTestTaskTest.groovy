package org.openbakery

import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import static org.hamcrest.Matchers.anything
import static org.hamcrest.core.IsAnything.anything

/**
 * Created by rene on 01.07.14.
 */
class XcodeTestTaskTest {


	GMockController mockControl
	CommandRunner commandRunnerMock

	Project project
	XcodeTestTask xcodeTestTask

	Destination destinationPad
	Destination destinationPhone

	List<String> expectedCommandList

	File outputDirectory

	Destination createDestination(String name, String id) {
		Destination destination = new Destination()
		destination.platform = XcodePlugin.SDK_IPHONESIMULATOR
		destination.name = name
		destination.arch = "i386"
		destination.id = id
		destination.os = "iOS"
		return destination
	}

	@Before
	void setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)

		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: 'org.openbakery.xcode-plugin'

		xcodeTestTask = project.tasks.findByName(XcodePlugin.XCODE_TEST_TASK_NAME);
		xcodeTestTask.setProperty("commandRunner", commandRunnerMock)


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


		outputDirectory = new File("build/test");
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}
		xcodeTestTask.setOutputDirectory(outputDirectory);
		File xcodebuildOutput = new File(outputDirectory, 'xcodebuild-output.txt')
		commandRunnerMock.setOutputFile(xcodebuildOutput.absoluteFile)
		FileUtils.writeStringToFile(xcodebuildOutput, "dummy")

		expectedCommandList?.clear()
		expectedCommandList = ['script', '-q', '/dev/null', "xcodebuild"]


	}

	@After
	void cleanup() {
		FileUtils.deleteDirectory(outputDirectory)
	}

	def void addExpectedScheme() {
		project.xcodebuild.scheme = 'myscheme'
		expectedCommandList.add("-scheme")
		expectedCommandList.add('myscheme')

		project.xcodebuild.workspace = 'myworkspace'
		expectedCommandList.add("-workspace")
		expectedCommandList.add('myworkspace')

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
	void parseFailureResultWithPartialSuite() {
		assert !xcodeTestTask.parseResult(new File("src/test/Resource/xcodebuild-output-test-failed-partial.txt"))

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


	@Test
	void compileErrorTest() {

		String result = xcodeTestTask.getFailureFromLog(new File("src/test/Resource/xcodebuild-output-test-compile-error.txt"))

		assert result.startsWith("Testing failed:");

		assert result.split("\n").length == 8

	}

	@Test
	void parseSuccessResultForTestsWrittenInSwiftUsingXcode_6_1() {
		assert xcodeTestTask.parseResult(new File("src/test/Resource/xcodebuild-output-swift-tests-xcode6_1.txt"))

		assert xcodeTestTask.numberSuccess() == 2
		assert xcodeTestTask.numberErrors() == 0
	}


	@Test
	void testCommandWithoutSimulator() {

		project.xcodebuild.sdk = 'macosx'
		project.xcodebuild.target = 'Test';

		addExpectedScheme()

		expectedCommandList.add("-sdk")
		expectedCommandList.add(XcodePlugin.SDK_MACOSX)

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")

		expectedCommandList.add("CODE_SIGN_IDENTITY=")
		expectedCommandList.add("CODE_SIGNING_REQUIRED=NO")

		addExpectedDefaultDirs()

		expectedCommandList.add("test")

		commandRunnerMock.run(project.projectDir.absolutePath, expectedCommandList, null, anything()).times(1)

		mockControl.play {
			xcodeTestTask.executeTask()
		}

	}



	@Test
	void testCommandForIOS() {

		commandRunnerMock.run("killall", "iOS Simulator")
		commandRunnerMock.run("killall", "Simulator")

		project.xcodebuild.sdk = 'iphonesimulator'
		project.xcodebuild.target = 'Test';

		addExpectedScheme()

		expectedCommandList.add("-sdk")
		expectedCommandList.add("iphonesimulator")

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")

		addExpectedDefaultDirs()

		expectedCommandList.add('-destination')
		expectedCommandList.add('platform=' + XcodePlugin.SDK_IPHONESIMULATOR + ',id=iPad Air')
		expectedCommandList.add('-destination')
		expectedCommandList.add('platform=' + XcodePlugin.SDK_IPHONESIMULATOR + ',id=iPhone 4s')

		expectedCommandList.add("test")

		commandRunnerMock.run(project.projectDir.absolutePath, expectedCommandList, null, anything()).times(1)

		mockControl.play {
			xcodeTestTask.executeTask()
		}

	}


	@Test
	void testCommandForIOS_killFailed() {

		commandRunnerMock.run("killall", "iOS Simulator").raises(new CommandRunnerException("failed"))
		commandRunnerMock.run("killall", "Simulator")


		project.xcodebuild.sdk = 'iphonesimulator'
		project.xcodebuild.target = 'Test';

		addExpectedScheme()

		expectedCommandList.add("-sdk")
		expectedCommandList.add("iphonesimulator")

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")

		addExpectedDefaultDirs()

		expectedCommandList.add('-destination')
		expectedCommandList.add('platform=' + XcodePlugin.SDK_IPHONESIMULATOR + ',id=iPad Air')
		expectedCommandList.add('-destination')
		expectedCommandList.add('platform=' + XcodePlugin.SDK_IPHONESIMULATOR + ',id=iPhone 4s')

		expectedCommandList.add("test")

		commandRunnerMock.run(project.projectDir.absolutePath, expectedCommandList, null, anything()).times(1)

		mockControl.play {
			xcodeTestTask.executeTask()
		}

	}

}
