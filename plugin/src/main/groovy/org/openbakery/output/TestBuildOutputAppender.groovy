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
	Project project

	Buffer fifoBuffer = new CircularFifoBuffer(100);

	List<String> runningTestSuites
    List<String> runningTestCases

	TestBuildOutputAppender(StyledTextOutput output, Project project) {
		super(output)
		this.project = project
	}



	void append(String line) {

		checkTestSuite(line);
		checkTestStart(line);
		checkTestFinished(line);
		checkAllTestsFinished(line);

		if (!testsRunning) {
			super.append(line)
		}

	}

	void checkTestSuite(String line) {

		def startMatcher = TEST_SUITE_START_PATTERN.matcher(line)
		if (startMatcher.matches()) {
			if (runningTestSuites == null) {
				runningTestSuites = new ArrayList<String>()
			}
			runningTestSuites.add(startMatcher[0][1].trim())
            testsRunning = true;
			return;
		}

		def endMatcher = TEST_SUITE_SUCCESS_END_PATTERN.matcher(line)
		if (endMatcher.matches()) {
			runningTestSuites?.remove(endMatcher[0][1].trim())
			return;
		}


		endMatcher = TEST_SUITE_FAILED_END_PATTERN.matcher(line)
		if (endMatcher.matches()) {
			runningTestSuites?.remove(endMatcher[0][1].trim())
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

		if (runningTestSuites != null && runningTestSuites.isEmpty() &&
                runningTestCases != null && runningTestCases.isEmpty()) {
			testsRunning = false
		}
	}

	void checkTestFinished(String line) {
		def finishMatcher = TEST_CASE_FINISH_PATTERN.matcher(line)
		if (finishMatcher.matches()) {
            String finishedTestCase = finishMatcher[0][1].trim();
			String result = finishMatcher[0][2].trim()
			String duration = finishMatcher[0][3].trim()

			boolean failed = result.equals("failed");

			printTestResult(finishedTestCase, failed, duration);

			runningTestCases.remove(finishedTestCase);
		}
	}



	void checkTestStart(String line) {
		Matcher startMatcher = TEST_CASE_START_PATTERN.matcher(line)
		if (startMatcher.matches()) {
            if (runningTestCases == null) {
                runningTestCases = new ArrayList<String>()
            }
			runningTestCases.add(startMatcher[0][1].trim())
            testsRunning = true;
		}
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
