package org.openbakery.appstore

import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.AbstractDistributeTask
import org.openbakery.output.ConsoleOutputAppender
import org.openbakery.tools.Xcode

/**
 * Created by rene on 08.01.15.
 */
class AbstractAppstoreTask extends AbstractDistributeTask {


	public AbstractDistributeTask() {
	}

	def runAltool(String action) {
		File ipa = getIpaBundle()

		if (project.appstore.username == null) {
			throw new IllegalArgumentException("Appstore username is missing. Parameter: appstore.username")
		}

		if (project.appstore.password == null) {
			throw new IllegalArgumentException("Appstore password is missing. Parameter: appstore.password")
		}

		String command = xcode.getPath() + "/Contents/Applications/Application Loader.app/Contents/Frameworks/ITunesSoftwareService.framework/Support/altool"

		StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(AbstractAppstoreTask.class)

		commandRunner.run([command, action, "--username", project.appstore.username, "--password",  project.appstore.password, "--file", ipa.getAbsolutePath()], new ConsoleOutputAppender(output))
	}
}
