package org.openbakery.output

import org.apache.commons.lang.StringUtils
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


	def ALL_TESTS_SUCCEEDED = "** TEST SUCCEEDED **";
	def ALL_TESTS_FAILED = "** TEST FAILED **";

	boolean testsRunning = false
	int testRun = 0
	Project project

	String currentTestCase = null;


	TestBuildOutputAppender(StyledTextOutput output, Project project) {
		super(output)
		this.project = project
	}



	void append(String line) {

		if (currentTestCase == null) {
			currentTestCase = checkTestStart(line);
		}

		if (currentTestCase != null) {
			checkTestFinished(line);
		}

		checkAllTestsEnded(line);

		super.append(line)

	}

	void checkAllTestsEnded(String line) {
		if (line.startsWith(ALL_TESTS_SUCCEEDED) || line.startsWith(ALL_TESTS_FAILED)) {

			if (currentTestCase) {
				// current test case was not properly finished, so some other error occurred, so fail it
				printTestResult(currentTestCase, true, "(unknown)");
			}

			Destination destination = project.xcodebuild.destinations[testRun]
			output.append("\n")
			output.append("Tests finished: ")
			output.append(destination.name)
			output.append(" ")
			output.append(destination.platform)
			if (!StringUtils.isEmpty(destination.os)) {
				output.append("/")
				output.append(destination.os)
			}
			output.println();
			output.println();
			testRun++;
			if (line.startsWith(ALL_TESTS_FAILED)) {
				output.append("TESTS FAILED");
				output.println();
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

			printTestResult(currentTestCase, failed, duration);

			currentTestCase = null;
			return;
		}
		return;
	}



	String checkTestStart(String line) {
		Matcher startMatcher = TEST_CASE_START_PATTERN.matcher(line)
		if (startMatcher.matches()) {
			if (!testsRunning) {
				output.println("\nPerform Unit Tests\n")
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
