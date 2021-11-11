package org.openbakery.appstore

import org.gradle.api.tasks.TaskAction

class AppstoreValidateTask extends AbstractAppstoreTask {

	AppstoreValidateTask() {
		super()
		this.description = "Validates the created ipa"
	}


	@TaskAction
	def validate() {
		runAltool("--validate-app")
	}


}
