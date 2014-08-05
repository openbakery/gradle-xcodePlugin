package org.openbakery.appledoc

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by rene on 21.07.14.
 */
class AppledocCleanTask extends DefaultTask {

	AppledocCleanTask() {
		super()
		this.description = "Cleans up the generated files from appledoc"
	}

	@TaskAction
	def clean() {
		def documentationDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("documentation")
    project.file(documentationDirectory).deleteDir()
	}
}
