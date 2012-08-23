package org.openbakery

import org.gradle.api.tasks.TaskAction

class ProvisioningCleanupTask extends AbstractXcodeTask {
	
	@TaskAction
	def clean() {


        File provisionDestinationFile = new File(project.provisioning.destinationRoot)
        if (!provisionDestinationFile.exists()) {
            return
        }

		def p = ~/.*\.mobileprovision/
		provisionDestinationFile.eachFileMatch(p) {
			f ->
			def mobileprovisionContent = f.getText();
			def matcher = mobileprovisionContent =~ "<key>UUID</key>\\s*\\n\\s*<string>(.*?)</string>"
			def uuid = matcher[0][1]

			File mobileprovisionPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/" + uuid + ".mobileprovision")
			if (mobileprovisionPath.exists()) {
				println "Deleting " + mobileprovisionPath
				mobileprovisionPath.delete();
			}
		}

	}
}