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
import org.openbakery.AbstractXcodeTask


class ProvisioningCleanupTask extends AbstractXcodeTask {

	ProvisioningCleanupTask() {
	}

	@TaskAction
	def clean() {
		if (!project.xcodebuild.signing.mobileProvisionDestinationRoot.exists()) {
			logger.lifecycle("Provisioning cleanup skipped because the destination directory does not exit")
			return
		}

		logger.debug("deleting {}", project.xcodebuild.signing.mobileProvisionDestinationRoot)
		project.xcodebuild.signing.mobileProvisionDestinationRoot.deleteDir()

		if (project.xcodebuild.signing.mobileProvisionDestinationRoot.exists()) {
			logger.error("error deleting provisioning: {}", project.xcodebuild.signing.mobileProvisionDestinationRoot)
		}

		File mobileprovisionPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/")

		if (!mobileprovisionPath.exists()) {
			logger.debug("nothing to cleanup")
		}

		// find all the broken profile links that where created by this plugin
		String profileLinksToDelete = commandRunner.runWithResult(["find", "-L", mobileprovisionPath.absolutePath, "-name", ProvisioningInstallTask.PROVISIONING_NAME_BASE+"*", "-type", "l"]);
		String[] profiles = profileLinksToDelete.split("\n")
		for (String profile : profiles) {
			logger.debug("profile to delete {}", profile)
			new File(profile).delete();
		}


	}
}