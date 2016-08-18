package org.openbakery.carthage

import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.AbstractXcodeTask
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.cocoapods.CocoapodsInstallTask
import org.openbakery.output.ConsoleOutputAppender

/**
 * Created by rene on 17.08.16.
 */
class CarthageUpdateTask extends AbstractXcodeTask {


	public CarthageUpdateTask() {
		super()
		setDescription "Installs the carthage dependencies for the given project"
	}

	@TaskAction
	void update() {

		checkCarthageInstallation()

		def output = services.get(StyledTextOutputFactory).create(CarthageUpdateTask)
		commandRunner.run(["carthage", "update"], new ConsoleOutputAppender(output))

	}

	void checkCarthageInstallation() {
		try {
			commandRunner.run("which", "carthage")
		} catch (CommandRunnerException) {
			throw new IllegalStateException("The carthage command was not found. Make sure that Carthage is installed")
		}
	}

	boolean hasCartfile() {
		File cartfile = new File(project.projectDir, "Cartfile")
		return cartfile.exists()
	}
}
