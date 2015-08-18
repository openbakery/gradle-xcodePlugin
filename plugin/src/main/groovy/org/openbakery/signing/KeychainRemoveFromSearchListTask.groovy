package org.openbakery.signing

import org.gradle.api.tasks.TaskAction

/**
 * Created by rene on 13.05.15.
 */
class KeychainRemoveFromSearchListTask extends AbstractKeychainTask {

	KeychainRemoveFromSearchListTask() {
		super()
		this.description = "Removes the gradle keychain from the search list"
	}



	void executeTask() {
		removeGradleKeychainsFromSearchList()
	}
}
