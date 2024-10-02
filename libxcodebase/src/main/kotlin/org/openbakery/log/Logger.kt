package org.openbakery.log

interface Logger {

	fun debug(message: String, vararg parameters: Any)


	fun info(message: String, vararg parameters: Any)
}
