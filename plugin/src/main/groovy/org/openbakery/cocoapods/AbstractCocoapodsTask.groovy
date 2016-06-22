package org.openbakery.cocoapods

import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.AbstractXcodeTask
import org.openbakery.output.ConsoleOutputAppender

/**
 * Created by rene on 04.02.16.
 */
class AbstractCocoapodsTask extends AbstractXcodeTask {

	String podCommand = null


	public Boolean hasPodfile() {
		File podFile = new File(project.projectDir, "Podfile")
		podFile.exists()
	}

	void runPod(String parameter) {

		if (podCommand == null) {
			String podPath = commandRunner.runWithResult("ruby", "-rubygems", "-e", "puts Gem.user_dir")
			podCommand = podPath + "/bin/pod"
		}
		logger.lifecycle "Run pod " + parameter

		def output = services.get(StyledTextOutputFactory).create(CocoapodsInstallTask)

		ArrayList<String> commandList = []
		commandList.add podCommand
		commandList.add parameter
		commandRunner.run commandList, new ConsoleOutputAppender(output)
	}

	def runInstallCocoapods() {
		logger.lifecycle "Bootstrap cocoapods"
		commandRunner.run("gem", "install", "-N", "--user-install", "cocoapods")

		runPod("setup")
	}
}
