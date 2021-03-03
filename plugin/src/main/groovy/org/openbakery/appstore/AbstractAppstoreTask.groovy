package org.openbakery.appstore

import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.AbstractDistributeTask
import org.openbakery.output.ConsoleOutputAppender

class AbstractAppstoreTask extends AbstractDistributeTask {

	public AbstractDistributeTask() {
	}

	def runAltool(String action) {
		File ipa = getIpaBundle()

		if (project.appstore.apiKey == null) {
			throw new IllegalArgumentException("Appstore apiKey is missing. Parameter: appstore.apiKey")
		}

		if (project.appstore.apiIssuer == null) {
			throw new IllegalArgumentException("Appstore apiIssuer is missing. Parameter: appstore.apiIssuer")
		}

		StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(AbstractAppstoreTask.class)

		commandRunner.run([xcode.getAltool(), action, "--apiKey", project.appstore.apiKey, "--apiIssuer",  project.appstore.apiIssuer, "--file", ipa.getAbsolutePath()], new ConsoleOutputAppender(output))
	}
}
