package org.openbakery.carthage

import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.AbstractXcodeTask
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

		File carthageDirectory = new File(project.projectDir, "Carthage")
		if (carthageDirectory.exists()) {
			return
		}

		def carthageCommand = getCarthageCommand()

		def output = services.get(StyledTextOutputFactory).create(CarthageUpdateTask)
		commandRunner.run(project.projectDir.absolutePath, [carthageCommand, "update"], new ConsoleOutputAppender(output))

	}

	String getCarthageCommand() {
		try {
			return commandRunner.runWithResult("which", "carthage")
		} catch (CommandRunnerException) {
			// ignore, because try again with full path below

		}

		try {
			def fullPath = "/usr/local/bin/carthage"
			commandRunner.runWithResult("ls", fullPath)
			return fullPath
		} catch (CommandRunnerException) {
			// ignore, because blow an exception is thrown
		}
		throw new IllegalStateException("The carthage command was not found. Make sure that Carthage is installed")
	}

	boolean hasCartfile() {
		File cartfile = new File(project.projectDir, "Cartfile")
		return cartfile.exists()
	}
}
