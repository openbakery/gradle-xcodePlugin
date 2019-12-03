package org.openbakery.rome

import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutputFactory

class RomeDownloadTask extends AbstractRomeTask {

	RomeDownloadTask() {
		super()
		setDescription "Download the dependencies using Rome"
		this.setOnlyIf {
			getRomeCommand() != null && romefileExists()
		}
	}

	@TaskAction
	void download() {
		def output = services.get(StyledTextOutputFactory).create(RomeUploadTask)
		run(["download"], output)
	}
}
