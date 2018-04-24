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

			List<String> args = [getCarthageCommand(),
								 ACTION_BOOTSTRAP,
								 ARG_PLATFORM,
								 carthagePlatformName,
								 ARG_CACHE_BUILDS]

			commandRunner.run(project.projectDir.absolutePath,
					args,
					getRequiredXcodeVersion() != null ? xcode.getXcodeSelectEnvValue(getRequiredXcodeVersion()) : null,
					new ConsoleOutputAppender(output))
		}
	}
}
