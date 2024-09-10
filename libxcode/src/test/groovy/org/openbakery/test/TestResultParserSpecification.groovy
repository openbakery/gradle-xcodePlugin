package org.openbakery.test

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.testdouble.SimulatorControlFake
import org.openbakery.xcode.Destination
import org.openbakery.xcode.DestinationResolver
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcode
import org.openbakery.xcode.XcodebuildParameters
import spock.lang.Specification

class TestResultParserSpecification extends Specification {

	TestResultParser testResultParser;
	File outputDirectory
	XCResultTool xcresulttool

	def setup() {

		outputDirectory = new File(System.getProperty("java.io.tmpdir"), 'gradle-xcodebuild/outputDirectory').absoluteFile
		outputDirectory.mkdirs();

		xcresulttool = new Xcode(new CommandRunner()).getXCResultTool()

		File testSummaryDirectory = new File("../xcode-plugin/src/test/Resource/TestLogs/xcresult/Success")
		testResultParser = new TestResultParser(testSummaryDirectory, xcresulttool, getDestinations("simctl-list-xcode11.txt"))
	}

	List<Destination> getDestinations(String simctlList) {
		SimulatorControlFake simulatorControl = new SimulatorControlFake(simctlList)
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

	def cleanup() {
		FileUtils.deleteDirectory(outputDirectory)
	}

	def "parse with no result"() {
		when:
		testResultParser.store(outputDirectory)
		then:
		true // no exception should be raised
	}

	def "parse new xcresult test summary has result"() {
		given:
		File testSummaryDirectory = new File("../xcode-plugin/src/test/Resource/TestLogs/xcresult/Success")
		testResultParser = new TestResultParser(testSummaryDirectory,xcresulttool, getDestinations("simctl-list-xcode11.txt"))

		when:
		testResultParser.parse(outputDirectory)

		then:
		testResultParser.testResults != null
		testResultParser.testResults.size() > 0
	}

	//
	def "parse new xcresult scheme and verify result count"() {
		given:
		File testSummaryDirectory = new File("../xcode-plugin/src/test/Resource/TestLogs/xcresult/Success")
		testResultParser = new TestResultParser(testSummaryDirectory,xcresulttool, getDestinations("simctl-list-xcode11.txt"))

		when:
		testResultParser.parse()

		then:
		testResultParser.testResults.size() == 1
		testResultParser.testResults.keySet()[0].name == "iPhone 8"
		testResultParser.number(TestResult.State.Passed) == 4
	}

	def "parse test summary and verify result count"() {
		when:
		testResultParser.parse()

		then:
		testResultParser.testResults.size() == 1
		testResultParser.testResults.keySet()[0].name == "iPhone 8"
	}

	def "parse test summary and verify number test results"() {
		when:
		testResultParser.parse()

		def firstKey = testResultParser.testResults.keySet()[0]
		then:
		testResultParser.testResults.get(firstKey).size() == 2
		testResultParser.number(TestResult.State.Passed) == 4

	}

	def "parse test summary that has failure"() {
		given:
		File testSummaryDirectory = new File("../xcode-plugin/src/test/Resource/TestLogs/xcresult/Failure")
		testResultParser = new TestResultParser(testSummaryDirectory,xcresulttool, getDestinations("simctl-list-xcode11.txt"))

		when:
		testResultParser.parse()

		def firstKey = testResultParser.testResults.keySet()[0]
		then:
		testResultParser.testResults.get(firstKey).size() == 2
		testResultParser.number(TestResult.State.Passed) == 3
		testResultParser.number(TestResult.State.Failed) == 1

	}

	def "parse test summary that has attachment"() {
		given:
		File testSummaryDirectory = new File("../xcode-plugin/src/test/Resource/TestLogs/xcresult/Attachment")
		Destination destination = new Destination("iPhone X")
		destination.id = "9F93F05E-3450-43BD-92FE-0F99212DB8B6"
		testResultParser = new TestResultParser(testSummaryDirectory, xcresulttool, [destination])

		expect:
		outputDirectory.listFiles().size() == 0

		when:
		testResultParser.parse(outputDirectory)

		def firstKey = testResultParser.testResults.keySet()[0]
		then:
		testResultParser.testResults.get(firstKey).size() == 1
		testResultParser.number(TestResult.State.Passed) == 2
		testResultParser.number(TestResult.State.Failed) == 0
		outputDirectory.listFiles().size() == 1
		outputDirectory.listFiles().first().name.contains(".html")
	}


	def "test xcresults - has duration"() {
		when:
		testResultParser.parse()

		def firstKey = testResultParser.testResults.keySet()[0]
		then:
		testResultParser.testResults.get(firstKey).size() == 2

		TestClass testClass = testResultParser.testResults.get(firstKey)[0]
		testClass.results.size() == 2
		testClass.results[0].duration == 0.00077807903.toFloat()
	}

	def "parse xcresult scheme with skipped test"() {
		given:
		File testSummaryDirectory = new File("../xcode-plugin/src/test/Resource/TestLogs/xcresult/Skipped")
		Destination destination = new Destination("iPhone X")
		destination.id = "7B40DCDA-3380-4BB9-AB92-1E3D1AC7B3BB"

		testResultParser = new TestResultParser(testSummaryDirectory,xcresulttool, [destination])

		when:
		testResultParser.parse()

		then:
		testResultParser.testResults.size() == 1
		testResultParser.testResults.keySet()[0].name == "iPhone X"
		testResultParser.number(TestResult.State.Passed) == 1
		testResultParser.number(TestResult.State.Skipped) == 1
	}

	def "parse xcresult scheme with expected test failure"() {
		given:
		File testSummaryDirectory = new File("../xcode-plugin/src/test/Resource/TestLogs/xcresult/ExpectFailure")
		Destination destination = new Destination("iPhone 12 Pro")
		destination.id = "3BFE2E8C-CABE-4C8F-AE96-DC9C5F792289"

		testResultParser = new TestResultParser(testSummaryDirectory, xcresulttool, [destination])

		when:
		testResultParser.parse()

		then:
		testResultParser.testResults.size() == 1
		testResultParser.testResults.keySet()[0].name == "iPhone 12 Pro"
		testResultParser.number(TestResult.State.Failed) == 0
		testResultParser.number(TestResult.State.Passed) == 2
		testResultParser.number(TestResult.State.Skipped) == 1
	}
}
