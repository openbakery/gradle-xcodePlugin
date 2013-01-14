package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class HockeyKitCleanTask extends DefaultTask {

	HockeyKitCleanTask() {
		super()
		this.description = "Cleans up the generated files from the hockey target"
	}

	@TaskAction
	def clean() {
		new File(project.hockeykit.outputDirectory).deleteDir()
	}
}
