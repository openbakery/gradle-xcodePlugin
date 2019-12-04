package org.openbakery.rome

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.AbstractXcodeTask
import org.openbakery.XcodePlugin
import org.openbakery.carthage.CarthageUpdateTask
import org.openbakery.output.ConsoleOutputAppender

class RomeUploadTask extends AbstractRomeTask {

	RomeUploadTask() {
		super()
		setDescription "Update the dependencies using Rome"
		this.setOnlyIf {
			getRomeCommand() != null && romefileExists()
		}
		dependsOn(
			XcodePlugin.CARTHAGE_BOOTSTRAP_TASK_NAME
		)
	}


	@TaskAction
	void upload() {
		def output = services.get(StyledTextOutputFactory).create(RomeUploadTask)

		List<String> listCommands = [getRomeCommand(), "list", "--platform", getPlatformName()]
		def list = commandRunner.runWithResult(project.projectDir.canonicalPath, listCommands)

		if (list == null) {
			logger.debug("nothing to upload")
			return
		}

		def lines = list.split("\n")
		for (line in lines) {
			if (line == "") {
				// skip empty lines
				continue
			}
			def tokens = line.split(" ")
			if (tokens.length > 0 ) {
				logger.info("Uploading " + tokens[0])
				def commands = ["upload", "--platform", getPlatformName(), tokens[0]]
				run(commands, output)
			}
		}
	}



}
