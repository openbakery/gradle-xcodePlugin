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

	CommandRunnerException(String message, Throwable cause) {
		super(message, cause)
	}

	CommandRunnerException(Throwable cause) {
		super(cause)
	}
}
