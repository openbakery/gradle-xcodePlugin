package org.openbakery

import org.gradle.api.tasks.TaskAction
import org.gradle.api.InvalidUserDataException

class ProvisioningInstallTask extends AbstractXcodeTask {

	@TaskAction
	def install() {
		if (project.provisioning.mobileprovisionUri == null) {
			throw new InvalidUserDataException("Property project.provisioning.mobileprovisionUri is missing")
		}

		def mobileprovisionFile = download(project.provisioning.destinationRoot, project.provisioning.mobileprovisionUri)
		println "Installing " + mobileprovisionFile

		def mobileprovisionContent = new File(mobileprovisionFile).getText()
		def matcher = mobileprovisionContent =~ "<key>UUID</key>\\s*\\n\\s*<string>(.*?)</string>"
		def uuid = matcher[0][1]

		File mobileprovisionPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles")
		if (!mobileprovisionPath.exists()) {
			mobileprovisionPath.mkdir()
		}

		File sourceFile = new File(mobileprovisionFile)
		File destinationFile = new File(mobileprovisionPath, uuid + ".mobileprovision")

		project.provisioning.mobileprovisionFile = destinationFile.absolutePath

		copy(sourceFile, destinationFile)
	}
}