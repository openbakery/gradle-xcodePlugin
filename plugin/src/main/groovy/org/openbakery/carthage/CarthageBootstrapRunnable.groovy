package org.openbakery.carthage

import groovy.util.logging.Log4j
import org.openbakery.CommandRunner
import org.openbakery.output.OutputAppender

import javax.inject.Inject

@Log4j
class CarthageBootstrapRunnable implements Runnable {



	final CommandRunner commandRunner
	final String carthageCommand
	final File projectDir
	final Map<String, String> environmentValues
	final OutputAppender outputAppender
	final String platform
	final String source

	@Inject
	CarthageBootstrapRunnable(File projectDir,
							  String carthageCommand,
							  String source,
							  String platform,
							  CommandRunner commandRunner,
							  Map<String, String> environmentValues) {
		this.carthageCommand = carthageCommand
		this.projectDir = projectDir
		this.source = source
		this.platform = platform
		this.outputAppender = outputAppender
		this.commandRunner = commandRunner
		this.environmentValues = environmentValues
	}

	@Override
	void run() {
		log.debug("Carthage bootstrap source : " + source)
		commandRunner.run([carthageCommand,
						   ACTION_BOOTSTRAP,
						   ARG_CACHE_BUILDS,
						   "--new-resolver",
						   "--color", "always",
						   "--project-directory", "${projectDir.absolutePath}",
						   ARG_PLATFORM, platform,
						   source],
				environmentValues,
				new OutputAppender() {
					@Override
					void append(String output) {
						log.debug(output)
					}
				})
	}
}
