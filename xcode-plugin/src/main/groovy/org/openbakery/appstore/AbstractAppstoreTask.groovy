package org.openbakery.appstore

import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.AbstractDistributeTask
import org.openbakery.output.ConsoleOutputAppender

class AbstractAppstoreTask extends AbstractDistributeTask {

	public AbstractDistributeTask() {
	}

	def runAltool(String action, String[] parameters = []) {

		if (project.appstore.apiIssuer != null && project.appstore.apiKey == null) {
			throw new IllegalArgumentException("Appstore apiKey is missing. Parameter: appstore.apiKey")
		}

		if (project.appstore.apiIssuer == null && project.appstore.apiKey != null) {
			throw new IllegalArgumentException("Appstore apiIssuer is missing. Parameter: appstore.apiIssuer")
		}

		if (project.appstore.apiIssuer == null && project.appstore.apiKey == null) {
			if (project.appstore.username != null && project.appstore.password == null) {
				throw new IllegalArgumentException("Appstore password is missing. Parameter: appstore.password")
			}
			if (project.appstore.username == null && project.appstore.password != null) {
				throw new IllegalArgumentException("Appstore username is missing. Parameter: appstore.username")
			}
		}

		File ipa = getIpaBundle()
		if (ipa.exists()) {
			runAltoolForIpa(ipa, action, parameters)
		} else {
			throw new IllegalStateException("IPA not found. Only uploading IPA is suppored")
		}

	}

	def runAltoolForIpa(File ipa, String action, String[] parameters) {

		def commandList = [
			xcode.getAltool(),
			action
		]

		if (action == "--upload-package") {
			commandList << ipa.getAbsolutePath()
		}

		commandList.addAll(parameters)

		if (xcode.version.major > 12) {
			commandList << "--type"
			commandList << "ios"
		}



		if (project.appstore.apiIssuer != null && project.appstore.apiKey != null) {
			commandList << "--apiKey"
			commandList << project.appstore.apiKey
			commandList << "--apiIssuer"
			commandList << project.appstore.apiIssuer
		} else if (project.appstore.username != null && project.appstore.password != null) {
			commandList << "--username"
			commandList << project.appstore.username
			commandList << "--password"
			commandList << project.appstore.password
		} else {
			throw new IllegalArgumentException("Credentials are missing. Either apiKey/apiIssuer of username/password")
		}


		if (action != "--upload-package") {
			commandList << "--file"
			commandList << ipa.getAbsolutePath()
		}


		StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(AbstractAppstoreTask.class)

		commandRunner.run(commandList, new ConsoleOutputAppender(output))
	}

}
