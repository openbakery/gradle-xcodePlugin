package org.openbakery.carthage

import groovy.transform.CompileStatic
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.output.ConsoleOutputAppender

@CompileStatic
class CarthageBootStrapTask extends AbstractCarthageTaskBase {

	CarthageBootStrapTask() {
		super()
		setDescription "Check out and build the Carthage project dependencies"
	}

	@TaskAction
	void update() {
		if (hasCartFile()) {
			logger.info('Boostrap Carthage for platform ' + carthagePlatformName)
			def output = services.get(StyledTextOutputFactory)
					.create(CarthageBootStrapTask)

			commandRunner.run(
					project.projectDir.absolutePath,
					[getCarthageCommand(),
					 ACTION_BOOTSTRAP,
					 ARG_PLATFORM,
					 carthagePlatformName,
					 ARG_CACHE_BUILDS],
					xcode.getXcodeSelectEnvValue(getRequiredXcodeVersion()),
					new ConsoleOutputAppender(output))
		}
	}
}
