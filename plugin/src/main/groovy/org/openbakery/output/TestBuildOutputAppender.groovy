package org.openbakery.output

import org.gradle.logging.StyledTextOutput
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

	def TEST_CASE_PATTERN = ~/^Test Case '(.*)'\s(\w+)\s\((\d+\.\d+)\sseconds\)\./


	boolean testsRunning = false


	TestBuildOutputAppender(StyledTextOutput output) {
		super(output)
	}


//GenerateDSYMFile

	void append(String line) {


		def matcher = TEST_CASE_PATTERN.matcher(line)
		if (matcher.matches()) {

			if (!testsRunning) {
				output.println("\nPerform Unit Tests\n");
				testsRunning = true;
			}

			String test = matcher[0][1].trim()
			String result = matcher[0][2].trim()
			String duration = matcher[0][3].trim()
			if (result.equals("passed")) {
				output.withStyle(StyledTextOutput.Style.Identifier).text("      OK")
			} else if (result.equals("failed")) {
				output.withStyle(StyledTextOutput.Style.Failure).text("  FAILED")
			}
			output.append(" ")
			output.append(test);

			output.append(" - (")
			output.append(duration)
			output.append(" seconds)")
			output.println();
		} else if (!testsRunning) {
			super.append(line)
		}

	}
}
