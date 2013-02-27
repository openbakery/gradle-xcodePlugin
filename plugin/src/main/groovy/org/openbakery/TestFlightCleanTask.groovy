package org.openbakery

import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask


class TestFlightCleanTask extends DefaultTask{

	TestFlightCleanTask() {
		super()
		this.description = "Cleans up the generated files from the testflight target"
	}

	@TaskAction
	def clean() {
		project.testflight.outputDirectory.deleteDir()
	}
}
