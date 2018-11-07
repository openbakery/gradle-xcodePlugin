package org.openbakery.util

import org.apache.commons.io.FilenameUtils
import org.openbakery.CommandRunner
import org.slf4j.LoggerFactory
import java.io.File



class ZipArchive(archiveFile: File, baseDirectory: File, commandRunner: CommandRunner = CommandRunner()) {

	companion object {
		val logger = LoggerFactory.getLogger("ZipArchive")!!

		@JvmStatic fun archive(fileToZip: File, commandRunner: CommandRunner = CommandRunner()): File {
			var archiveFile = File(fileToZip.parentFile, FilenameUtils.getBaseName(fileToZip.name) + ".zip")
			var zipArchive = ZipArchive(archiveFile, commandRunner)
			zipArchive.add(fileToZip)
			zipArchive.create()
			return archiveFile
		}


	}

	private var commandRunner: CommandRunner = commandRunner
	private var archiveFile: File = archiveFile
	private var baseDirectory: File = baseDirectory


	constructor(archiveFile: File, commandRunner: CommandRunner = CommandRunner()) :
		this(archiveFile, archiveFile.absoluteFile.parentFile, commandRunner)


	private val filesToAdd = ArrayList<File>()

	fun add(file: File) {
		filesToAdd.add(file)
	}

	fun create() {
		logger.debug("create zip: {}", archiveFile)

		if (!archiveFile.parentFile.exists()) {
			archiveFile.parentFile.mkdirs()
		}

		var command = mutableListOf<String>(
			"/usr/bin/zip",
			"--symlinks",
			"--recurse-paths"
		)
		command.add(archiveFile.absolutePath)

		for (file in filesToAdd) {
			var relativePath = file.absoluteFile.relativeTo(baseDirectory.absoluteFile)
			command.add(relativePath.path)
		}

		commandRunner.run(baseDirectory.path, command)

	}

}
