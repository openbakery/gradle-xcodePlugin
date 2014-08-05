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

	def deleteKeychain() {
		for (String keychain : getKeychainList()) {
			File keychainFile = new File(keychain)
			if (!keychainFile.exists()) {
				if (keychainFile.name.startsWith(XcodeBuildPluginExtension.KEYCHAIN_NAME_BASE)) {
					logger.quiet("Deleting keychain: {}", keychainFile)
					try {
						commandRunner.run(["security", "delete-keychain", keychainFile.absolutePath])
					} catch (IllegalStateException ex) {
						// ignore because delete-keychain results in an error because the file does not exists
						// but the entry is deleted properly
					}
				} else {
					logger.debug("keychain was not created by this plugin so leave it: {}", keychainFile)
				}

			} else if (keychainFile.name.equals("gradle.keychain")) {
				// gradle.keychain is the xcodelugin version 0.7 keychain that also needs to be cleaned
				logger.debug("deleting old 0.7 xcodeplugin keychain file")
				commandRunner.run(["security", "delete-keychain", keychainFile.absolutePath])
			} else {
				logger.debug("keychain exists so leave it: {}", keychainFile)
			}

		}
	}

	@TaskAction
	def clean() {
		if (project.xcodebuild.signing.keychain) {
			logger.debug("Nothing to cleanup")
			return;
		}

		project.xcodebuild.signing.signingDestinationRoot.deleteDir()

		if (getOSVersion().minor >= 9) {
			setKeychainList(getKeychainList())
		} else {
			deleteKeychain()
		}

	}

}