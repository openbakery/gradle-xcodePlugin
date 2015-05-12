/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbakery.signing

import org.gradle.api.tasks.TaskAction
import org.openbakery.XcodeBuildPluginExtension

class KeychainCleanupTask extends AbstractKeychainTask {

	KeychainCleanupTask() {
		super()
		this.description = "Cleanup the keychain"
	}

	/**
	 * remove all gradle keychains from the keychain search list
	 * @return
	 */
	def removeGradleKeychainsFromSearchList() {
		List<String> keychainFiles = new ArrayList<>();
		getKeychainList().each {
			File keychainFile = new File(it)
			if (!keychainFile.name.startsWith(XcodeBuildPluginExtension.KEYCHAIN_NAME_BASE)) {
				keychainFiles.add(it)
			}
		}
		setKeychainList(keychainFiles)
	}

	@TaskAction
	def clean() {
		if (project.xcodebuild.signing.keychain) {
			logger.debug("Nothing to cleanup")
			return;
		}

		project.xcodebuild.signing.signingDestinationRoot.deleteDir()

		removeGradleKeychainsFromSearchList()


	}

}