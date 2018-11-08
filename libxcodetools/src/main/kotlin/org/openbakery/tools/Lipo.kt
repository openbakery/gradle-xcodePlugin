package org.openbakery.tools

import org.openbakery.CommandRunner
import org.openbakery.xcode.Xcode

open class Lipo(xcode: Xcode, commandRunner: CommandRunner) {

	var xcode: Xcode = xcode
	var commandRunner: CommandRunner = commandRunner


	open fun getArchs(binaryName: String): List<String> {
		val commandList = listOf(
			xcode.lipo,
			"-info",
			binaryName
		)
		var result = commandRunner.runWithResult(commandList)
		if (result != null) {
			var resultArray = result.split(" are: ")
			if (resultArray.size == 2) {
				return resultArray[1].split(" ")
			}
		}
		return listOf("armv7", "arm64")
	}

	open fun removeArch(binaryName: String, arch: String) {
		commandRunner.run(
			xcode.lipo,
			binaryName,
			"-remove",
			arch,
			"-output",
			binaryName
		)
	}

	open fun removeUnsupportedArchs(binaryName: String, supportedArchs: List<String>) {

		val archs = getArchs(binaryName).toMutableList()
		archs.removeAll(supportedArchs)

		archs.iterator().forEach {
			removeArch(binaryName, it)
		}


	}

}
