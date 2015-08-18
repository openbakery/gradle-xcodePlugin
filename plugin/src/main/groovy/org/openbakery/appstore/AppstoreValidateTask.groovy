package org.openbakery.appstore

import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractDistributeTask

/**
 * Created by rene on 08.01.15.
 */
class AppstoreValidateTask extends AbstractAppstoreTask {

	AppstoreValidateTask() {
		super()
		this.description = "Validates the created ipa"
	}


	void executeTask() {
		runAltool("--validate-app")
	}


}
