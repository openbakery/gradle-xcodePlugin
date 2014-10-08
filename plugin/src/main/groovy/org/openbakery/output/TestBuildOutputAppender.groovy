package org.openbakery.output

import org.apache.commons.collections.Buffer
import org.apache.commons.collections.buffer.CircularFifoBuffer
import org.gradle.api.Project
import org.gradle.logging.StyledTextOutput
import org.openbakery.Destination
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.regex.Matcher

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 09.10.13
 * Time: 18:14
 * To change this template use File | Settings | File Templates.
 */
class TestBuildOutputAppender extends XcodeBuildOutputAppender {

	private static Logger logger = LoggerFactory.getLogger(TestBuildOutputAppender.class)

	def TEST_CASE_FINISH_PATTERN = ~/^Test Case '(.*)'\s(\w+)\s\((\d+\.\d+)\sseconds\)\./

	def TEST_CASE_START_PATTERN = ~/^Test Case '(.*)' started./

	def TEST_SUITE_START_PATTERN = ~/^Test Suite '(.*)' started.*/
	def TEST_SUITE_SUCCESS_END_PATTERN = ~/^Test Suite '(.*)' passed.*/
	def TEST_SUITE_FAILED_END_PATTERN = ~/^Test Suite '(.*)' failed.*/

	boolean testsRunning = false
	int testRun = 0
	Project project

	String currentTestCase = null;
	Buffer fifoBuffer = new CircularFifoBuffer(100);

	List<String> testSuites


	TestBuildOutputAppender(StyledTextOutput output, Project project) {
		super(output)
		this.project = project
	}



	void append(String line) {

		checkTestSuite(line);

		if (currentTestCase == null) {
			currentTestCase = checkTestStart(line);
		}

		if (currentTestCase != null) {
			checkTestFinished(line);
		}

		checkAllTestsFinished(line);

		if (!testsRunning) {
			super.append(line)
		}

	}

	void checkTestSuite(String line) {

		def startMatcher = TEST_SUITE_START_PATTERN.matcher(line)
		if (startMatcher.matches()) {
			if (testSuites == null) {
				testSuites = new ArrayList<String>()
			}
			testSuites.add(startMatcher[0][1].trim())
			return;
		}

		def endMatcher = TEST_SUITE_SUCCESS_END_PATTERN.matcher(line)
		if (endMatcher.matches()) {
			testSuites.remove(endMatcher[0][1].trim())
			return;
		}


		endMatcher = TEST_SUITE_FAILED_END_PATTERN.matcher(line)
		if (endMatcher.matches()) {
			testSuites.remove(endMatcher[0][1].trim())
			for (String cachedLine in fifoBuffer) {
				output.println(cachedLine);
			}
			output.println();
			output.println();
			output.append("TESTS FAILED");
			output.println();
			output.println();
		}


	}

	void checkAllTestsFinished(String line) {
		fifoBuffer.add(line);

		if (this.testSuites != null && this.testSuites.isEmpty()) {

			if (currentTestCase) {
				// current test case was not properly finished, so some other error occurred, so fail it
				printTestResult(currentTestCase, true, "(unknown)");
			}

			currentTestCase = null
			testsRunning = false

			Destination destination = project.xcodebuild.destinations[testRun]
			output.append("\n")
			output.append("Tests finished: ")
			output.append(destination.toPrettyString());
			output.println();
			output.println();
			testRun++;

			this.testSuites = null;
		}
	}

	void checkTestFinished(String line) {
		def finishMatcher = TEST_CASE_FINISH_PATTERN.matcher(line)
		if (finishMatcher.matches()) {
			String result = finishMatcher[0][2].trim()
			String duration = finishMatcher[0][3].trim()

			boolean failed = result.equals("failed");

			printTestResult(currentTestCase, failed, duration);

			currentTestCase = null;
		}
		return;
	}



	String checkTestStart(String line) {
		Matcher startMatcher = TEST_CASE_START_PATTERN.matcher(line)
		if (startMatcher.matches()) {
			if (!testsRunning) {
				Destination destination = project.xcodebuild.destinations[testRun]
				if (destination) {
					output.append("\nPerform unit tests for: ")
					output.append(destination.toPrettyString());
					output.println();
					output.println();
				}

				testsRunning = true;
			}
			return startMatcher[0][1].trim()
		}
		return null;
	}

	void printTestResult(String testCase, boolean failed, String duration) {
		if (!failed) {
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
