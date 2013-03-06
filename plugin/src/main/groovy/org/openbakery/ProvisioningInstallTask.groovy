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

import org.gradle.api.tasks.TaskAction
import org.gradle.api.InvalidUserDataException

class ProvisioningInstallTask extends AbstractXcodeTask {

	@TaskAction
	def install() {
		if (project.xcodebuild.signing.mobileProvisionURI == null) {
			throw new InvalidUserDataException("Property project.xcodebuild.signing.mobileProvisionURI is missing")
		}

		def mobileProvisionFile = download(project.xcodebuild.signing.mobileProvisionDestinationRoot, project.xcodebuild.signing.mobileProvisionURI)
		project.xcodebuild.signing.mobileProvisionFile = new File(mobileProvisionFile)


		runCommand(["/bin/ln", "-s", project.xcodebuild.signing.mobileProvisionName, project.xcodebuild.signing.mobileProvisionFileLinkToLibrary.absolutePath])
	}
}