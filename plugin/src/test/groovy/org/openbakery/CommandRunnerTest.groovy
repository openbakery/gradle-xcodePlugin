package org.openbakery

import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 24.07.14.
 */
class CommandRunnerTest {

	CommandRunner commandRunner;


	@BeforeMethod
	def setup() {
		commandRunner = new CommandRunner()
	}


	@Test
	void testRunWithResult() {
		String result = commandRunner.runWithResult(["echo", "test"])

		assert "test".equals(result) : "the result should be test, but was: " + result

	}

}
