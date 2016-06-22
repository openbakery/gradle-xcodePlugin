package org.openbakery.output

import org.gradle.api.Project
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.text.StyledTextOutput
import org.openbakery.Destination

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 09.10.13
 * Time: 18:14
 * To change this template use File | Settings | File Templates.
 */
class TestBuildOutputAppender extends XcodeBuildOutputAppender {

	enum TestState {
		Unknown,
		Compile,
		Started, // test suite has started
		Running, // test case is running
		Finished, // test case is finished
		Done // all tests are finished
	}

	def TEST_CASE_FINISH_PATTERN = ~/^Test Case '(.*)'\s(\w+)\s\((\d+\.\d+)\sseconds\)\./
	def TEST_CASE_START_PATTERN = ~/^Test Case '(.*)' started./
	def TEST_SUITE_START_PATTERN = ~/.*Test Suite '(.*)' started.*/
	def FAILED_TESTS_PATTERN = ~/^Failing tests:/
	def TEST_FAILED_PATTERN = ~/.*\*\* TEST FAILED \*\*/
	def TEST_SUCCEEDED_PATTERN = ~/.*\*\* TEST SUCCEEDED \*\*/

	int testsCompleted = 0
	int testsFailed = 0
	TestState state = TestState.Unknown;
	int testRun = 0
	int startedDestination = -1
	Project project
	String currentTestCase = null;

	StringBuilder currentOutput = new StringBuilder();

	TestBuildOutputAppender(ProgressLogger progressLogger, StyledTextOutput output, Project project) {
		super(progressLogger, output)
		this.project = project
	}

	TestBuildOutputAppender(StyledTextOutput output, Project project) {
		this(null, output, project)
	}

	@Override
	void append(String line) {

		if (line.startsWith("Compile")) {
			state = TestState.Compile
			super.append(line)
		} else if (checkTestSuite(line)) {
			state = TestState.Started
			currentOutput.setLength(0) // deletes the buffer
		} else if (checkTestStart(line)) {
			state = TestState.Running
		} else if (checkTestFinished(line)) {
			state = TestState.Finished
			currentOutput.setLength(0) // deletes the buffer
		}	else if (checkAllTestsFinished(line)) {
			state = TestState.Done
		} else {

			// there was no state change
			if (state == TestState.Started && currentTestCase != null) {
				printTestResult(currentTestCase, true, "(unknown)");
			}


			if (state == TestState.Running || state == TestState.Compile) {
				currentOutput.append(line)
				currentOutput.append("\n")
			}
			if (state == TestState.Unknown || state == TestState.Compile) {
				super.append(line)
			}

		}





	}

	boolean checkTestSuite(String line) {
		def startMatcher = TEST_SUITE_START_PATTERN.matcher(line)
		if (startMatcher.matches()) {
			startDestination()
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
		project.getLogger().debug("printFailureOutput")
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
			String result = finishMatcher[0][2].trim()
			String duration = finishMatcher[0][3].trim()
			boolean failed = result.equals("failed");
			if (failed) {
				testsFailed++
			}
			printTestResult(currentTestCase, failed, duration);
			currentTestCase = null;
			testsCompleted++;
			return true
		}
		return false
	}

	boolean checkTestStart(String line) {

		if (line.startsWith("Touch") && line.endsWith("xctest")) {
			progress("Starting Tests")
		}


		def startMatcher = TEST_CASE_START_PATTERN.matcher(line)
		if (startMatcher.matches()) {
			startDestination()
			String testCase = startMatcher[0][1].trim()

			//0 tests completed, Test Suite 'DTActionPanelTest_iPhone'
			int endIndex = testCase.indexOf(' ')
			int startIndex = testCase.indexOf('[')
			if (startIndex > 0 && endIndex > 0) {
				String message = getTestInfoMessage()
				message += ", running '" + testCase.substring(startIndex + 1, endIndex) + "'"
				progress(message)
			}


			currentTestCase = testCase;
			return true;
		}
		return false;
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
			Destination destination = project.xcodebuild.availableDestinations[testRun]
			if (destination) {
				startedDestination = testRun
				output.append("\nRun tests for: ")
				output.append(destination.toPrettyString());
				output.println();
			}
		}
	}

	void finishDestination() {
		Destination destination = project.xcodebuild.availableDestinations[testRun]
		if (destination != null) {
			progress("Tests finished: " + destination.toPrettyString())
			output.append(getTestInfoMessage())
			output.append("\n")
			testRun++;
			testsFailed = 0;
			testsCompleted = 0;
		}
	}

	void printTestResult(String testCase, boolean failed, String duration) {
		if (!failed) {
			if (!fullProgress) {
				return;
			}
			output.withStyle(StyledTextOutput.Style.Identifier).text("      OK")
		} else {
			output.withStyle(StyledTextOutput.Style.Failure).text("  FAILED")
		}
		output.append(" ")
		output.append(testCase);
		output.append(" - (")
		output.append(duration)
		output.append(" seconds)")
		output.println();
		output.println();
		if (failed) {
			output.withStyle(StyledTextOutput.Style.Identifier).text(currentOutput.toString())
			output.println();
			output.println();
		}
	}



	void progress(String message) {
		if (progressLogger == null) {
			return
		}
		progressLogger.progress(message)
	}
}
