package org.openbakery.signing

import org.gradle.api.tasks.TaskAction
import org.openbakery.XcodePlugin

class KeychainRemoveFromSearchListTask extends AbstractKeychainTask {

	KeychainRemoveFromSearchListTask() {
		super()
		mustRunAfter(XcodePlugin.CRASHLYTICS_TASK_NAME)
		this.description = "Removes the gradle keychain from the search list"
	}



	@TaskAction
	def remove() {
		removeGradleKeychainsFromSearchList()
	}
}
