package org.openbakery.output

import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.text.StyledTextOutput
import org.openbakery.xcode.Destination
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 09.10.13
 * Time: 18:14
 * To change this template use File | Settings | File Templates.
 */
class TestBuildOutputAppender extends XcodeBuildOutputAppender {

	private static Logger logger = LoggerFactory.getLogger(TestBuildOutputAppender.class)


	enum TestState {
		Unknown,
		Compile,
		Started, // test suite has started
		Running, // test case is running
		Finished, // test case is finished
		Done // all tests are finished
	}

	// Matches
	// Xcode 11: "Test case '-[EditTextTableViewCellTest test_keyLabel]' passed on 'iPhone 8' (0.008 seconds)"
	// Xcode < 11: "Test Case '-[OBFoundationTests.AlertDialogHandlerTest test_dialog_builder]' passed (0.007 seconds)."
	def TEST_CASE_FINISH_PATTERN = ~/^[Tt]est [Cc]ase '(.*)'\s(\w+)(\son\s'.*?')?\s\((\d+\.\d+)\sseconds\)\.?/


	def TEST_CASE_START_PATTERN = ~/^[Tt]est [Cc]ase '(.*)' started./
	def TEST_SUITE_START_PATTERN = ~/.*[Tt]est [Ss]uite '(.*)' started.*/
	def TEST_FAILED_PATTERN = ~/.*\*\* TEST FAILED \*\*/
	def TEST_SUCCEEDED_PATTERN = ~/.*\*\* TEST SUCCEEDED \*\*/

	int testsCompleted = 0
	int testsFailed = 0
	TestState state = TestState.Unknown;
	int testRun = 0
	int startedDestination = -1
	List<Destination> destinations
	String currentTestCase = null;

	StringBuilder currentOutput = new StringBuilder();

	TestBuildOutputAppender(ProgressLogger progressLogger, StyledTextOutput output, List<Destination> destinations) {
		super(progressLogger, output)
		this.destinations = destinations
	}

	TestBuildOutputAppender(StyledTextOutput output, List<Destination> destinations) {
		this(null, output, destinations)
	}


	@Override
	boolean checkLine(String line) {
		if (super.checkLine(line)) {
			state = TestState.Compile
			return true
		}

		if (checkTestSuite(line)) {
			state = TestState.Started
			currentOutput.setLength(0) // deletes the buffer
			return true
		}


		if (checkTestSuite(line)) {
			state = TestState.Started
			currentOutput.setLength(0) // deletes the buffer
			return true
		}

		if (checkTestStart(line)) {
			state = TestState.Running
			return true
		}

		if (checkTestFinished(line)) {
			state = TestState.Finished
			currentOutput.setLength(0) // deletes the buffer
			return true
		}

		if (checkAllTestsFinished(line)) {
			state = TestState.Done
			return true
		}

		if (state == TestState.Running || state == TestState.Compile) {
			currentOutput.append(line)
			currentOutput.append("\n")
		}

		return false
	}


	boolean checkTestSuite(String line) {
		def startMatcher = TEST_SUITE_START_PATTERN.matcher(line)
		if (startMatcher.matches()) {
			startDestination()
			def name = startMatcher[0][1].trim()
			progress("${getTestInfoMessage()}, running '${name}'")
			return true
		}
		return false
	}

	boolean checkAllTestsFinished(String line) {
		def successMatcher = TEST_SUCCEEDED_PATTERN.matcher(line)
		def failedMatcher = TEST_FAILED_PATTERN.matcher(line)

		if (failedMatcher.matches()) {
			printFailureOutput()
			finishDestination()
			return true
		}

		if (successMatcher.matches()) {
			finishDestination()
			return true
		}
		return false
	}

	private void printFailureOutput() {
		logger.debug("printFailureOutput")
		def failureOutput = []
		def startFound = false;
		currentOutput.toString().split("\n").reverse().any {
			failureOutput << it

			if (it.startsWith("Testing failed:")) {
				startFound = true
			}
			return startFound
		}
		if (startFound) {
			output.withStyle(StyledTextOutput.Style.Identifier).text(failureOutput.reverse().join("\n"))
			output.withStyle(StyledTextOutput.Style.Normal).text("\n")
		}
	}

	boolean checkTestFinished(String line) {
		def finishMatcher = TEST_CASE_FINISH_PATTERN.matcher(line)
		if (finishMatcher.matches()) {
			def matchingGroups = finishMatcher[0]
			String result = matchingGroups[2].trim()
			String duration = matchingGroups[matchingGroups.size-1].trim()
			boolean failed = (result == "failed")
			if (failed) {
				testsFailed++
			}
			String testCase = finishMatcher[0][1].trim()
			int endIndex = testCase.indexOf(' ')
			int startIndex = testCase.indexOf('[')
			if (startIndex > 0 && endIndex > 0) {
				String message = getTestInfoMessage()
				message += ", running '" + testCase.substring(startIndex + 1, endIndex) + "'"
				progress(message)
			}

			printTestResult(testCase, failed, duration)
			currentTestCase = null
			testsCompleted++
			return true
		}
		return false
	}

	boolean isStartTest(String line) {
		return line.startsWith("Testing started on")
	}

	boolean isStartTestLegacy(String line) {
		return line.startsWith("Touch") && line.endsWith("xctest")
	}

	boolean checkTestStart(String line) {

		if (isStartTest(line) || isStartTestLegacy(line)) {
			output.withStyle(StyledTextOutput.Style.Normal).text("Tests started!\n")
			progress("Starting Tests")
		}

		def startMatcher = TEST_CASE_START_PATTERN.matcher(line)
		if (startMatcher.matches()) {
			startDestination()
			String testCase = startMatcher[0][1].trim()
			currentTestCase = testCase
			return true
		}
		return false
	}

	private String getTestInfoMessage() {
		String message = testsCompleted + " tests completed"
		if (testsFailed) {
			message += ", " + testsFailed + " failed"
		}
		return message
	}

	void startDestination() {
		if (startedDestination != testRun) {
			Destination destination = this.destinations[testRun]
			if (destination) {
				startedDestination = testRun
				output.append("\nRun tests for: ")
				output.append(destination.toPrettyString())
				output.println()
			}
		}
	}

	void finishDestination() {
		Destination destination = this.destinations[testRun]
		if (destination != null) {
			progress("Tests finished: " + destination.toPrettyString())
			output.append(getTestInfoMessage())
			output.append("\n")
			testRun++
			testsFailed = 0
			testsCompleted = 0
		}
	}

	void printTestResult(String testCase, boolean failed, String duration) {
		if (!failed) {
			if (!fullProgress) {
				return
			}
			output.withStyle(StyledTextOutput.Style.Identifier).text("      OK")
		} else {
			output.withStyle(StyledTextOutput.Style.Failure).text("  FAILED")
		}
		output.append(" ")
		output.append(testCase)
		output.append(" - (")
		output.append(duration)
		output.append(" seconds)")
		output.println()
		if (failed) {
			output.withStyle(StyledTextOutput.Style.Identifier).text(currentOutput.toString())
			output.println()
			output.println()
		}
	}



	void progress(String message) {
		if (progressLogger == null) {
			return
		}
		progressLogger.progress(message)
	}
}
