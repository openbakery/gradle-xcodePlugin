package org.openbakery.log

import org.jetbrains.annotations.NotNull

class SLF4JLogger implements org.openbakery.log.Logger {

	private org.slf4j.Logger logger

	SLF4JLogger(org.slf4j.Logger logger)	 {
		this.logger = logger
	}

	@Override
	void debug(@NotNull String message, @NotNull Object... parameters) {
		logger.debug(message, parameters)

	}

	@Override
	void info(@NotNull String message, @NotNull Object... parameters) {
		logger.info(message, parameters)
	}
}
