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

class ProvisioningCleanupTask extends AbstractXcodeTask {
	ProvisioningProfileIdReader provisioningProfileIdReader

	ProvisioningCleanupTask() {
		provisioningProfileIdReader = new ProvisioningProfileIdReader()
	}

	@TaskAction
	def clean() {
		project.provisioning.destinationRoot.deleteDir()

		def uuid = provisioningProfileIdReader.readProvisioningProfileIdFromDestinationRoot(project.provisioning.destinationRoot)
		if (uuid != null) {
			File mobileprovisionPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/" + uuid + ".mobileprovision")
			if (mobileprovisionPath.exists()) {
				println "Deleting " + mobileprovisionPath
				mobileprovisionPath.delete()
			}
		}
	}
}