package org.openbakery.tools

import org.openbakery.CommandRunner
import org.openbakery.xcode.Xcode
import java.io.File

open class Lipo(xcode: Xcode, commandRunner: CommandRunner) {

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

	open fun removeUnsupportedArchs(binary: File, supportedArchs: List<String>) {

		val archs = getArchs(binary).toMutableList()
		archs.removeAll(supportedArchs)
		archs.iterator().forEach {
			removeArch(binary, it)
		}
	}
}
