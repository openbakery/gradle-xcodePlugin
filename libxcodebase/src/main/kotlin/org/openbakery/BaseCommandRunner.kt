package org.openbakery

import org.openbakery.log.LoggerFactory
import java.io.File

open class BaseCommandRunner(var defaultBaseDirectory: String = ".") {

	val logger = LoggerFactory.getInstance().getLogger(BaseCommandRunner::class.toString())

	open var outputFile: File? = null
		set(value) {
			value?.let { file ->
				val tokens = file.name.split(".")
				if (tokens.count() == 2) {
					val basename = tokens.first() + "-"
					val extension = "." + tokens.last()
					val newFile = File.createTempFile(basename, extension, file.parentFile)
					file.renameTo(newFile)
					logger.debug("moved existing file '{}' to '{}", file, newFile)
				}
				if (file.exists()) {
					file.delete()
				}
			}
			field = value
		}


}
