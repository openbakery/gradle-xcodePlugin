package org.openbakery

/**
 * User: rene
 * Date: 16.07.13
 * Time: 11:50
 */
class CommandRunnerException extends IllegalStateException {

	CommandRunnerException() {
		super()
	}

	CommandRunnerException(String s) {
		super(s)
	}

	CommandRunnerException(String s, Throwable throwable) {
		super(s, throwable)
	}

	CommandRunnerException(Throwable throwable) {
		super(throwable)
	}
}
