package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class HockeyAppCleanTask extends DefaultTask{

	HockeyAppCleanTask() {
		super()
		this.description = "Cleans up the generated files from the hockeyapp target"
	}

	@TaskAction
	def clean() {
		project.hockeyapp.outputDirectory.deleteDir()
	}
}
