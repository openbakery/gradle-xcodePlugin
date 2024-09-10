package org.openbakery

import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Internal
import org.openbakery.test.TestResult
import org.openbakery.test.TestResultParser

class AbstractXcodeTestTask extends AbstractXcodeBuildTask {

	@Internal
	Logger printLogger = this.logger

	void processTestResult(File testLogsDirectory) {
		TestResultParser testResultParser = new TestResultParser(testLogsDirectory, xcode.getXCResultTool(), destinations)
		testResultParser.parseAndStore(outputDirectory)
		int numberSuccess = testResultParser.number(TestResult.State.Passed)
		int numberErrors = testResultParser.number(TestResult.State.Failed)
		int numberSkipped = testResultParser.number(TestResult.State.Skipped)


		String message = ""

		if (numberErrors == 0 && numberSkipped == 0) {
			message = "All "
		}

		if (numberSuccess < 2) {
			message += numberSuccess + " test was successful"
		} else {
			message += numberSuccess + " tests were successful"
		}

		if (numberErrors > 0) {
			message += ", and " + numberErrors + " failed"
		}
		if (numberSkipped > 0) {
			message += ", and " + numberSkipped + " skipped"
		}

		print(message)

		if (numberErrors != 0) {
			throw new Exception("Not all unit tests are successful!")
		}
	}


	void print(String message) {
		printLogger.lifecycle(message)
	}
}
