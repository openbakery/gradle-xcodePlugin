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
import org.openbakery.XcodePlugin

class KeychainCreateTask extends AbstractKeychainTask {


	KeychainCreateTask() {
		super()
		this.description = "Create a keychain that is used for signing the app"

		dependsOn(XcodePlugin.KEYCHAIN_CLEAN_TASK_NAME)

		this.setOnlyIf {
			return !this.buildSpec.isSdk(XcodePlugin.SDK_IPHONESIMULATOR)
		}
	}

	void executeTask() {


		if (project.xcodebuild.isSdk(XcodePlugin.SDK_IPHONESIMULATOR)) {
			logger.lifecycle("The simulator build does not need a provisioning profile");
			return
		}

		if (this.buildSpec.signing.keychain) {
			if (!this.buildSpec.signing.keychain.exists()) {
				throw new IllegalStateException("Keychain not found: " + this.buildSpec.signing.keychain.absolutePath)
			}
			logger.debug("Using keychain {}", this.buildSpec.signing.keychain)
			logger.debug("Internal keychain {}", this.buildSpec.signing.keychainPathInternal)
			return
		}

		if (this.buildSpec.signing.certificateURI == null) {
			logger.debug("not certificateURI specifed so do not create the keychain");
			return
		}


		if (this.buildSpec.signing.certificatePassword == null) {
			throw new InvalidUserDataException("Property signing.certificatePassword is missing")
		}

		def certificateFile = download(this.buildSpec.signing.signingDestinationRoot, this.buildSpec.signing.certificateURI)

		def keychainPath = this.buildSpec.signing.keychainPathInternal.absolutePath

		logger.debug("Create Keychain: {}", keychainPath)

		if (!new File(keychainPath).exists()) {
			commandRunner.run(["security", "create-keychain", "-p", this.buildSpec.signing.keychainPassword, keychainPath])
		}
		commandRunner.run(["security", "-v", "import", certificateFile, "-k", keychainPath, "-P", this.buildSpec.signing.certificatePassword, "-T", "/usr/bin/codesign"])


		if (getOSVersion().minor >= 9) {

			def keychainList = getKeychainList()
			keychainList.add(keychainPath)
			setKeychainList(keychainList)
		}

		// Set a custom timeout on the keychain if requested
		if (this.buildSpec.signing.timeout != null) {
			commandRunner.run(["security", "-v", "set-keychain-settings", "-lut", this.buildSpec.signing.timeout.toString(), keychainPath])
		}
	}



}