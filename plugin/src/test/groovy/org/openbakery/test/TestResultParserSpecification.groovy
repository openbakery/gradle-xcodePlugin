package org.openbakery.test

import org.apache.commons.io.FileUtils
import org.openbakery.testdouble.SimulatorControlStub
import org.openbakery.xcode.Destination
import org.openbakery.xcode.DestinationResolver
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcodebuild
import org.openbakery.xcode.XcodebuildParameters
import spock.lang.Specification

/**
 * Created by rene on 27.10.16.
 */
class TestResultParserSpecification extends Specification {

	TestResultParser testResultParser;
	File outputDirectory

	def setup() {

		outputDirectory = new File(System.getProperty("java.io.tmpdir"), 'gradle-xcodebuild/outputDirectory').absoluteFile
		outputDirectory.mkdirs();

		File testSummaryDirectory = new File("src/test/Resource/TestLogs/Success")
		testResultParser = new TestResultParser(testSummaryDirectory, getDestinations())
	}


	List<Destination> getDestinations() {
		SimulatorControlStub simulatorControl = new SimulatorControlStub("simctl-list-xcode7.txt")
		XcodebuildParameters parameters = new XcodebuildParameters()
		parameters.simulator = true
		parameters.type = Type.iOS
		parameters.configuredDestinations = [
			new Destination("iPad 2"),
			new Destination("iPhone 4s")
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



	def "parse success result"() {
		when:
		def result = testResultParser.parseResult(new File("src/test/Resource/xcodebuild-output.txt"))

		then:
		testResultParser.numberSuccess(result) == 2
		testResultParser.numberErrors(result) == 0
	}


	def "parse failure result"() {
		when:
		def result = testResultParser.parseResult(new File("src/test/Resource/xcodebuild-output-test-failed.txt"))

		then:
		testResultParser.numberSuccess(result) == 0
		testResultParser.numberErrors(result) == 2
	}


	def "parse failure result with partial suite"() {
		when:
		def result = testResultParser.parseResult(new File("src/test/Resource/xcodebuild-output-test-failed-partial.txt"))

		then:
		testResultParser.numberSuccess(result) == 0
		testResultParser.numberErrors(result) == 2
	}


	def "parse success result xcode 6.1"() {
		when:
		def result = testResultParser.parseResult(new File("src/test/Resource/xcodebuild-output-xcode6_1.txt"))

		then:
		testResultParser.numberSuccess(result) == 8
		testResultParser.numberErrors(result) == 0
	}

	def "parse complex test output"() {
		when:
		def result = testResultParser.parseResult(new File("src/test/Resource/xcodebuild-output-complex-test.txt"))

		then:
		testResultParser.numberErrors(result) == 0
	}

	def "parse success result for tests written in swift using Xcode 6.1"() {
		when:
		def result = testResultParser.parseResult(new File("src/test/Resource/xcodebuild-output-swift-tests-xcode6_1.txt"))

		then:
		testResultParser.numberSuccess(result) == 2
		testResultParser.numberErrors(result) == 0
	}


	def "parse test summary has result"() {
		when:
		testResultParser.parse()

		then:
		testResultParser.testResults != null
		testResultParser.testResults.size() > 0
	}

	def "parse test summary and verify result count"() {
		when:
		testResultParser.parse()

		then:
		testResultParser.testResults.size() == 1
		testResultParser.testResults.keySet()[0].name == "iPad 2"
	}


	def "parse test summary and verify number test results"() {
		when:
		testResultParser.parse()

		def firstKey = testResultParser.testResults.keySet()[0]
		then:
		testResultParser.testResults.get(firstKey).size() == 5
		testResultParser.numberSuccess() == 37

	}


	def "parse test summary that has failure"() {
		given:
		File testSummaryDirectory = new File("src/test/Resource/TestLogs/Failure")
		testResultParser = new TestResultParser(testSummaryDirectory, destinations)

		when:
		testResultParser.parse()

		def firstKey = testResultParser.testResults.keySet()[0]
		then:
		testResultParser.testResults.get(firstKey).size() == 5
		testResultParser.numberSuccess() == 36
		testResultParser.numberErrors() == 1

	}


	HashMap<Destination, ArrayList<TestClass>> getMergedResult() {

		testResultParser.parse()

		def resultFromOutput = testResultParser.parseResult(new File("src/test/Resource/TestLogs/Success/xcodebuild-output.txt"))
		testResultParser.mergeResult(testResultParser.testResults, resultFromOutput)
		return testResultParser.testResults
	}


	def "test merged results"() {
		when:
		def mergedResult = getMergedResult()

		def firstKey = mergedResult.keySet()[0]
		then:
		mergedResult.get(firstKey).size() == 5
		testResultParser.numberSuccess() == 37

	}

	def "test merged results - has duration"() {
		when:
		def mergedResult = getMergedResult()

		def firstKey = mergedResult.keySet()[0]
		then:
		mergedResult.get(firstKey).size() == 5

		TestClass testClass = mergedResult.get(firstKey)[0]
		testClass.results.size() == 1
		testClass.results[0].duration == 0.01.toFloat()
		testClass.results[0].output.startsWith("Test Case")

	}
}
