package org.openbakery

import org.gradle.api.tasks.TaskAction

class ProvisioningCleanupTask extends AbstractXcodeTask {
	
	@TaskAction
	def clean() {
		new File(project.provisioning.destinationRoot).deleteDir()

		def uuid = getProvisioningProfileId()
		if (uuid != null) {
			File mobileprovisionPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/" + uuid + ".mobileprovision")
			if (mobileprovisionPath.exists()) {
				println "Deleting " + mobileprovisionPath
				mobileprovisionPath.delete()
			}
		}
	}
}