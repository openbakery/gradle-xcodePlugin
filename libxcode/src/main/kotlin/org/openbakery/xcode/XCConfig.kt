package org.openbakery.xcode

import java.io.File

public class XCConfig(public val file: File) {

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
