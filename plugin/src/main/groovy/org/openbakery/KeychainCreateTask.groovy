package org.openbakery

import org.gradle.api.tasks.TaskAction
import org.gradle.api.InvalidUserDataException


class KeychainCreateTask extends AbstractXcodeTask {

	KeychainCreateTask() {
		super()
		this.description = "Create a propery keychain that is used for signing the app"
	}

	@TaskAction
	def create() {
		if (project.keychain.certificateUri == null) {
			throw new InvalidUserDataException("Property project.keychain.certificateUri is missing")
		}
		if (project.keychain.certificatePassword == null) {
			throw new InvalidUserDataException("Property project.keychain.certificatePassword is missing")
		}

		def certificateFile = download(project.keychain.destinationRoot, project.keychain.certificateUri)

		def keychainPath = System.getProperty("user.home") + "/Library/Keychains/" + project.keychain.keychainName

		println "Create Keychain '" + project.keychain.keychainName + "'"

		if (!new File(keychainPath).exists()) {
			runCommand(["security", "create-keychain", "-p", project.keychain.keychainPassword, keychainPath])
		}

		//runCommand(["security", "default-keychain", "-s", project.keychain.keychainName])
		runCommand(["security", "unlock-keychain", "-p", project.keychain.keychainPassword, keychainPath])

		runCommand(["security", "-v", "import", certificateFile, "-k", keychainPath, "-P", project.keychain.certificatePassword, "-T", "/usr/bin/codesign"])

		runCommand(["security", "list"])
	}


}