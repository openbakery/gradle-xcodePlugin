package org.openbakery.xcode

import java.io.File
import org.openbakery.log.LoggerFactory
import org.openbakery.log.Logger

public class XCConfig(public val file: File) {

	private val logger = LoggerFactory.getInstance().getLogger(XCConfig::class.toString())

	private val entries = mutableMapOf<String, String>()

	init {
		if (file.exists()) {
			val list = file.readLines()
			val map = list.associate {
				it.split(" = ").let { (key, value) -> key to value }
			}
			entries.putAll(map)
		}
	}

	fun create() {
		val string = entries.toList().joinToString(separator = "\n") {
			it.first + " = " + it.second
		}
		file.parentFile.mkdirs()
		file.writeText(string)
	}

	fun set(key: String, value: String) {
		entries[key] = value
	}
}
