package org.openbakery.output

import org.gradle.api.Project
import org.gradle.logging.ProgressLogger
import org.gradle.logging.StyledTextOutput
import org.openbakery.Destination

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 09.10.13
 * Time: 18:14
 * To change this template use File | Settings | File Templates.
 */
class TestBuildOutputAppender extends XcodeBuildOutputAppender {

	def TEST_CASE_FINISH_PATTERN = ~/^Test Case '(.*)'\s(\w+)\s\((\d+\.\d+)\sseconds\)\./
	def TEST_CASE_START_PATTERN = ~/^Test Case '(.*)' started./
	def TEST_SUITE_START_PATTERN = ~/.*Test Suite '(.*)' started.*/
	def FAILED_TESTS_PATTERN = ~/^Failing tests:/
	def TEST_FAILED_PATTERN = ~/.*\*\* TEST FAILED \*\*/
	def TEST_SUCCEEDED_PATTERN = ~/.*\*\* TEST SUCCEEDED \*\*/

	int testsCompleted = 0
	int testsFailed = 0
	boolean testsRunning = false
	boolean outputLine = false
	int testRun = 0
	int startedDestination = -1
	Project project
	String currentTestCase = null;

	TestBuildOutputAppender(ProgressLogger progressLogger, StyledTextOutput output, Project project) {
		super(progressLogger, output)
		this.project = project
	}

	TestBuildOutputAppender(StyledTextOutput output, Project project) {
		this(null, output, project)
	}

	@Override
	void append(String line) {

		checkTestSuite(line);



		def startedTest = checkTestStart(line)
		if (currentTestCase == null) {
			currentTestCase = startedTest;
		} else if (startedTest != null) {
			// current test case was not properly finished, so some other error occurred, so fail it
			printTestResult(currentTestCase, true, "(unknown)");
			currentTestCase = startedTest
		} else {
			checkTestFinished(line);
		}
		checkAllTestsFinished(line);

		if (outputLine) {
			output.append("\n")
			output.append(line)
		} else if (!testsRunning) {
			super.append(line)
		}
	}

	void checkTestSuite(String line) {
		def startMatcher = TEST_SUITE_START_PATTERN.matcher(line)
		if (startMatcher.matches()) {
			if (!testsRunning) {
				output.append("\nStart unit tests\n\n")
			}
			testsRunning = true
			startDestination()
		}
	}

	void checkAllTestsFinished(String line) {
		def successMatcher = TEST_SUCCEEDED_PATTERN.matcher(line)
		def failedMatcher = TEST_FAILED_PATTERN.matcher(line)
		if (successMatcher.matches() || failedMatcher.matches()) {
			finishDestination()
			testsRunning = false
			outputLine = false
		} else {
			def failingTestsMatcher = FAILED_TESTS_PATTERN.matcher(line)
			if (failingTestsMatcher.matches()) {
				testsRunning = false
				outputLine = true
				output.println();
				output.append("TESTS FAILED");
				output.println();
			}
		}
	}

	void checkTestFinished(String line) {
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
		}
	}

	String checkTestStart(String line) {
		def startMatcher = TEST_CASE_START_PATTERN.matcher(line)
		if (startMatcher.matches()) {
			testsRunning = true
			startDestination()
			String testCase =  startMatcher[0][1].trim()
			if (progressLogger != null) {

				//0 tests completed, Test Suite 'DTActionPanelTest_iPhone'
				int endIndex = testCase.indexOf(' ')
				int startIndex = testCase.indexOf('[')
				if (startIndex > 0 && endIndex > 0) {
					String message = getTestInfoMessage()
					message += ", running '" + testCase.substring(startIndex+1, endIndex) + "'"
					progressLogger.progress(message)
				}

			}
			return testCase;
		}
		return null;
	}

	private String getTestInfoMessage() {
		String message = testsCompleted + " tests completed"
		if (testsFailed) {
			message += ", " + testsFailed + " failed "
		}
		return message
	}

	void startDestination() {
		if (startedDestination != testRun) {
			Destination destination = project.xcodebuild.availableDestinations[testRun]
			if (destination) {
				startedDestination = testRun
				output.append("\nPerform unit tests for: ")
				output.append(destination.toPrettyString());
				output.println();
				output.println();
			}
		}
	}

	void finishDestination() {
		Destination destination = project.xcodebuild.availableDestinations[testRun]
		if (destination != null) {
			if (progressLogger != null) {
				progressLogger.progress("Tests finished: " + 	destination.toPrettyString())
			}
			output.append(getTestInfoMessage())
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
	}
}
