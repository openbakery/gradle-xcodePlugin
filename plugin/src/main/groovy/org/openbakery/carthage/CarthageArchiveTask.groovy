package org.openbakery.carthage

import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.AbstractXcodeTask
import org.openbakery.XcodePlugin

class CarthageArchiveTask extends AbstractCarthageTaskBase {

	CarthageArchiveTask() {
		super()
		setDescription "Create a binary framework that can be used a Carthage project dependencies"
		this.setOnlyIf {
			cartfileExists()
		}
	}


	@TaskAction
	void archive() {
		def output = services.get(StyledTextOutputFactory).create(CarthageUpdateTask)
		run(ACTION_ARCHIVE, output, false)
	}

}
