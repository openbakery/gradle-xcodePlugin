package org.openbakery.carthage

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by rene on 15.09.16.
 */
class CarthageCleanTask extends DefaultTask {



	CarthageCleanTask() {
		super()
		this.description = "Cleans up the Carthage directory"
	}

	@TaskAction
	def clean() {
		project.file("Carthage").deleteDir()
	}


}
