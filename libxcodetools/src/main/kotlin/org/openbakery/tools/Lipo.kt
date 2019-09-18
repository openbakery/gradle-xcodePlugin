package org.openbakery.tools

import org.openbakery.CommandRunner
import org.openbakery.xcode.Xcode
import org.slf4j.LoggerFactory
import java.io.File

open class Lipo(xcode: Xcode, commandRunner: CommandRunner) {

	companion object {
		val logger = LoggerFactory.getLogger("Lipo")!!
	}

	var xcode: Xcode = xcode
	var commandRunner: CommandRunner = commandRunner


	open fun getArchs(binary: File): List<String> {
		val commandList = listOf(
			xcode.lipo,
			"-info",
			binary.absolutePath
		)
		var result = commandRunner.runWithResult(commandList)
		if (result != null) {
			var archsString = result.split(":").last().trim()
			return archsString.split(" ")
		}
		return listOf("armv7", "arm64")
	}

	open fun removeArch(binary: File, arch: String) {
		commandRunner.run(
			xcode.lipo,
			binary.absolutePath,
			"-remove",
			arch,
			"-output",
			binary.absolutePath
		)
	}

	/**
	 Remove all the unsupported architecture from the app binary.
	 If not, then appstore connect will reject the ipa
	 */
	open fun removeUnsupportedArchs(binary: File, supportedArchs: List<String>) {
		logger.debug("removeUnsupportedArchs at {}", binary)
		logger.debug("supportedArchs are {}", supportedArchs)
		val archs = getArchs(binary).toMutableList()
		archs.removeAll(supportedArchs)
		archs.iterator().forEach {
			removeArch(binary, it)
		}
	}
}
