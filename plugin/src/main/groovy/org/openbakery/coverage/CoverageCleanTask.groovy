package org.openbakery.coverage

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by rene on 22.07.14.
 */
class CoverageCleanTask extends DefaultTask {


	CoverageCleanTask() {
		super()
		this.description = "Cleans up the generated files from the coverage"
	}

	@TaskAction
	def clean() {
		project.file(project.coverage.outputDirectory).deleteDir()
	}
}
