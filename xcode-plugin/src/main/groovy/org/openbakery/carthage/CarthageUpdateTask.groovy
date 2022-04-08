package org.openbakery.carthage

import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.output.ConsoleOutputAppender

class CarthageUpdateTask extends AbstractCarthageTaskBase {

	CarthageUpdateTask() {
		super()
		setDescription "Update and rebuild the Carthage project dependencies"
	}

	@TaskAction
	void update() {
		def output = services.get(StyledTextOutputFactory).create(CarthageUpdateTask)
		run(ACTION_UPDATE, output)
	}
}
