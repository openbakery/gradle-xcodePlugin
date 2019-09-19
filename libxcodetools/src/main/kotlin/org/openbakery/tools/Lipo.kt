package org.openbakery.tools

import org.openbakery.CommandRunner
import org.openbakery.xcode.Xcode
import org.openbakery.xcode.Xcodebuild
import org.slf4j.LoggerFactory
import java.io.File

open class Lipo(xcodebuild: Xcodebuild) {

	companion object {
		val logger = LoggerFactory.getLogger("Lipo")!!
	}

	var xcodebuild: Xcodebuild = xcodebuild


	open fun getArchs(binary: File): List<String> {
		val commandList = listOf(
			getLipoCommand(),
			"-info",
			binary.absolutePath
		)
		var result = xcodebuild.commandRunner.runWithResult(commandList)
		if (result != null) {
			var archsString = result.split(":").last().trim()
			return archsString.split(" ")
		}
		return listOf("armv7", "arm64")
	}

	open fun removeArch(binary: File, arch: String) {
		xcodebuild.commandRunner.run(
			getLipoCommand(),
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


	open fun getLipoCommand() : String {
		return xcodebuild.getToolchainDirectory() + "/usr/bin/lipo"
	}

}
