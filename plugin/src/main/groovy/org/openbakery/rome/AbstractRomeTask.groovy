package org.openbakery.rome

import org.gradle.api.tasks.Input
import org.gradle.internal.logging.text.StyledTextOutput
import org.openbakery.AbstractXcodeTask
import org.openbakery.output.ConsoleOutputAppender
import org.openbakery.xcode.Type

class AbstractRomeTask extends AbstractXcodeTask {

	static final String ROME_PLATFORM_IOS = "iOS"
	static final String ROME_PLATFORM_MACOS = "Mac"
	static final String ROME_PLATFORM_TVOS = "tvOS"
	static final String ROME_PLATFORM_WATCHOS = "watchOS"

	@Input
	String getPlatformName() {
		switch (project.xcodebuild.type) {
			case Type.iOS: return ROME_PLATFORM_IOS
			case Type.tvOS: return ROME_PLATFORM_TVOS
			case Type.macOS: return ROME_PLATFORM_MACOS
			case Type.watchOS: return ROME_PLATFORM_WATCHOS
			default: return 'all'
		}
	}

	String getRomeCommand() {
		try {
			return commandRunner.runWithResult("which", "rome")
		} catch (Exception ignored) {
		}
		return null
	}

	boolean romefileExists() {
		File romefile = new File(project.projectDir, "Romefile")
		return romefile.exists()
	}


	void run(List<String> commands, StyledTextOutput output) {
		logger.info('Rome upload')

		List<String> arguments = [getRomeCommand()]
		arguments.addAll(commands)

		commandRunner.run(project.projectDir.canonicalPath,
			arguments,
			new ConsoleOutputAppender(output))

	}
}
