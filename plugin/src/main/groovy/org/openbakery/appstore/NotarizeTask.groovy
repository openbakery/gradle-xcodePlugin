package org.openbakery.appstore


import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.AbstractDistributeTask
import org.openbakery.bundle.Bundle
import org.openbakery.output.ConsoleOutputAppender
import org.openbakery.xcode.Type

class NotarizeTask extends AbstractDistributeTask {

	NotarizeTask() {
		super()
		this.description = "Notarize the build macOS app zip file"
	}



	@TaskAction
	def notarize() {
		File zipBundle = getBundlePathByExtension("zip")


		File applicationPath = getBundlePathByExtension("app")
		Bundle applicationBundle = new Bundle(applicationPath, Type.macOS, this.plistHelper)


		if (project.appstore.username == null) {
			throw new IllegalArgumentException("Appstore username is missing. Parameter: appstore.username")
		}

		if (project.appstore.password == null) {
			throw new IllegalArgumentException("Appstore password is missing. Parameter: appstore.password")
		}

		if (project.appstore.ascProvider == null) {
			throw new IllegalArgumentException("asc-provider is missing. Parameter: appstore.ascProvider")
		}


		StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(NotarizeTask.class)

		commandRunner.run([xcode.getAltool(),
											 "--notarize-app",
											 "--primary-bundle-id", applicationBundle.bundleIdentifier,
											 "--asc-provider", project.appstore.ascProvider,
											 "--username", project.appstore.username,
											 "--password",  project.appstore.password,
											 "--file", zipBundle.getAbsolutePath()],
			new ConsoleOutputAppender(output))


	}
}
