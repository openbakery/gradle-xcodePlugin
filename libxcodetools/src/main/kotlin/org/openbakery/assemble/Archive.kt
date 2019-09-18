package org.openbakery.assemble

import org.openbakery.CommandRunner
import org.openbakery.tools.CommandLineTools
import org.openbakery.util.FileHelper
import org.slf4j.LoggerFactory
import java.io.File

class Archive(applicationBundleFile: File, archiveName: String, tools: CommandLineTools) {

	companion object {
		val logger = LoggerFactory.getLogger("AppPackage")!!
	}

	private val applicationBundleFile: File = applicationBundleFile
	private val tools: CommandLineTools = tools
	private val fileHelper: FileHelper = FileHelper(CommandRunner())
	private val archiveName: String = archiveName


	fun create(destinationDirectory: File) {
		copyApplication(destinationDirectory)
	}

	private fun copyApplication(destinationDirectory: File) {
		val archiveDirectory = getArchiveDirectory(destinationDirectory)
		fileHelper.copyTo(applicationBundleFile, archiveDirectory)
	}

	private fun getArchiveDirectory(destinationDirectory: File): File {
		val archiveDirectory = File(destinationDirectory, this.archiveName + ".xcarchive")
		archiveDirectory.mkdirs()
		return archiveDirectory
	}


}
