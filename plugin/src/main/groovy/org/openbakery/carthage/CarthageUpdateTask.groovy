package org.openbakery.carthage

import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.output.ConsoleOutputAppender

class CarthageUpdateTask extends AbstractCarthageTaskBase {

	CarthageUpdateTask() {
		super()
		setDescription "Update and rebuild the Carthage project dependencies"
	}

	@TaskAction
	void update() {
		if (hasCartFile()) {
			logger.info('Update Carthage for platform ' + carthagePlatformName)

			def output = services.get(StyledTextOutputFactory)
					.create(CarthageUpdateTask)

			commandRunner.run(
					project.projectDir.absolutePath,
					[getCarthageCommand(),
					 ACTION_UPDATE,
					 ARG_PLATFORM,
					 carthagePlatformName,
					 ARG_CACHE_BUILDS],
					new ConsoleOutputAppender(output))
		}
	}
}
