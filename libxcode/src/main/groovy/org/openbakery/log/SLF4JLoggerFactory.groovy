package org.openbakery.log

import org.jetbrains.annotations.NotNull;

class SLF4JLoggerFactory extends LoggerFactory {


	@Override
	Logger getLogger(@NotNull String name) {
		org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(name)
		return new SLF4JLogger(logger)
	}
}
