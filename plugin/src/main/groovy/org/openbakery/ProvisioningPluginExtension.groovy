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

class ProvisioningPluginExtension {

	public final static PROVISIONING_NAME_BASE = "gradle-"

	/**
	 * public parameter
	 */
	String mobileprovisionUri = null

	/**
	 * internal parameters
	 */
	Object destinationRoot
	String mobileprovisionFile
	Object mobileprovisionFileLinkToLibrary


	private Project project
	final String uniqueFileName =  PROVISIONING_NAME_BASE + System.currentTimeMillis() +  ".mobileprovision"


	public ProvisioningPluginExtension(Project project) {
		this.project = project;

		this.destinationRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("provisioning")
		}

		this.mobileprovisionFileLinkToLibrary = {
			return new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/" + uniqueFileName);
		}

	}

	File getDestinationRoot() {
		return project.file(destinationRoot)
	}

	void setDestinationRoot(Object destinationRoot) {
		this.destinationRoot = destinationRoot
	}

	File getMobileprovisionFileLinkToLibrary() {
		return project.file(mobileprovisionFileLinkToLibrary)
	}
}