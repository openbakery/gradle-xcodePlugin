package org.openbakery.log

class EmptyLogger: Logger {

	override fun debug(message: String, vararg parameters: Any) {
	}

	override fun info(message: String, vararg parameters: Any) {
	}

}
