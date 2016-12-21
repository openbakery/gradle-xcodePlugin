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
import org.gradle.api.InvalidUserDataException
import org.openbakery.xcode.Type
import org.openbakery.XcodePlugin

class KeychainCreateTask extends AbstractKeychainTask {


	KeychainCreateTask() {
		super()
		this.description = "Create a keychain that is used for signing the app"
	}

	@TaskAction
	def create() {



		if (project.xcodebuild.signing.keychain) {
			if (!project.xcodebuild.signing.keychain.exists()) {
				throw new IllegalStateException("Keychain not found: " + project.xcodebuild.signing.keychain.absolutePath)
			}
			logger.debug("Using keychain {}", project.xcodebuild.signing.keychain)
			logger.debug("Internal keychain {}", project.xcodebuild.signing.keychainPathInternal)
			return
		}

		if (project.xcodebuild.signing.certificateURI == null) {
			logger.debug("not certificateURI specifed so do not create the keychain");
			return
		}


		if (project.xcodebuild.signing.certificatePassword == null) {
			throw new InvalidUserDataException("Property project.xcodebuild.signing.certificatePassword is missing")
		}

		// first cleanup old keychain
		cleanupKeychain()

		def certificateFile = download(project.xcodebuild.signing.signingDestinationRoot, project.xcodebuild.signing.certificateURI)

		File keychain = project.xcodebuild.signing.keychainPathInternal

		security.createKeychain(keychain, project.xcodebuild.signing.keychainPassword)
		security.importCertificate(new File(certificateFile), project.xcodebuild.signing.certificatePassword, keychain)


		if (getOSVersion().minor >= 9) {
			List<File> keychainList = getKeychainList()
			keychainList.add(keychain)
			setKeychainList(keychainList)
		}

		if (getOSVersion().minor >= 12) {
			security.setPartitionList(keychain, project.xcodebuild.signing.keychainPassword)
		}

		// Set a custom timeout on the keychain if requested
		if (project.xcodebuild.signing.timeout != null) {
			security.setTimeout(project.xcodebuild.signing.timeout, keychain)
		}
	}



}