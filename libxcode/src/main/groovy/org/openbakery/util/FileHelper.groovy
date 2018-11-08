package org.openbakery.util

import org.openbakery.CommandRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FileHelper {

	private static Logger logger = LoggerFactory.getLogger(FileHelper.class)

	private CommandRunner commandRunner


	FileHelper(CommandRunner commandRunner) {
		this.commandRunner = commandRunner
	}


	def copyTo(File source, File destinationDirectory) {
		logger.debug("Copy file '{}' to directory '{}'", source, destinationDirectory)

		if (!source.exists()) {
			logger.info("source does not exist " + source)
			return
		}
		File destination = new File(destinationDirectory, source.getName())

		commandRunner.run([
			"ditto",
			source.absolutePath,
			destination.absolutePath
		])
	}

}
