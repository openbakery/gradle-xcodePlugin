package org.openbakery.carthage

import groovy.util.logging.Slf4j
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.output.OutputAppender

import javax.inject.Inject

@Slf4j
class CarthageBootstrapRunnable implements Runnable {

	static final String ACTION_BOOTSTRAP = "bootstrap"
	static final String ARG_PLATFORM = "--platform"
	static final String ARG_CACHE_BUILDS = "--cache-builds"
	static final String CARTHAGE_USR_BIN_PATH = "/usr/local/bin/carthage"

	final File projectDir
	final OutputAppender outputAppender
	final String platform
	final String source
	final CommandRunner commandRunner

	@Inject
	CarthageBootstrapRunnable(File projectDir,
							  String source,
							  String platform,
							  CommandRunner commandRunner) {
		println "platform : " + platform
		assert false
		this.projectDir = projectDir
		this.source = source
		this.platform = platform
		this.outputAppender = outputAppender
		this.commandRunner = commandRunner
	}

	@Override
	void run() {
		commandRunner.run([getCarthageCommand(),
						   ACTION_BOOTSTRAP,
						   "--color", "always",
						   "--project-directory", "${projectDir.absolutePath}",
						   ARG_CACHE_BUILDS,
						   ARG_PLATFORM, platform,
						   source],
				new OutputAppender() {
					@Override
					void append(String output) {
						log.debug(output)
					}
				})
	}

	String getCarthageCommand() {
		try {
			return commandRunner.runWithResult("which", "carthage")
		} catch (CommandRunnerException exception) {
			// ignore, because try again with full path below
		}

		try {
			commandRunner.runWithResult("ls", CARTHAGE_USR_BIN_PATH)
			return CARTHAGE_USR_BIN_PATH
		} catch (CommandRunnerException exception) {
			// ignore, because blow an exception is thrown
		}
		throw new IllegalStateException("The carthage command was not found. Make sure that Carthage is installed")
	}
}
