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
package org.openbakery

import org.gradle.api.Project
import org.gradle.api.Plugin

class KeychainPluginExtension {
	public final static KEYCHAIN_NAME_BASE = "gradle-"


	String mobileprovisionUri = null
	String certificateUri = null
	String certificatePassword = null
	String keychainPassword = "This_is_the_default_keychain_password"
	Object destinationRoot
	File keychain


	Object internalKeychainPath

	final Project project
	final String keychainName =  KEYCHAIN_NAME_BASE + System.currentTimeMillis() +  ".keychain"



	public KeychainPluginExtension(Project project) {
		this.project = project;

		this.destinationRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("keychain")
		}

		this.internalKeychainPath = {
			if (this.keychain != null) {
				return this.keychain
			}
			return new File(this.destinationRoot, keychainName)
		}

	}

	File getDestinationRoot() {
		return project.file(destinationRoot)
	}

	void setDestinationRoot(Object destinationRoot) {
		this.destinationRoot = destinationRoot
	}

	void setKeychain(Object keychain) {
		this.keychain = project.file(keychain)
	}

	File getInternalKeychainPath() {
		return project.file(internalKeychainPath)
	}
}