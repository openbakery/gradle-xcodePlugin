package org.openbakery.cocoapods

import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.AbstractXcodeTask
import org.openbakery.CommandRunnerException
import org.openbakery.XcodePlugin
import org.openbakery.output.ConsoleOutputAppender

class AbstractCocoapodsTask extends AbstractXcodeTask {

	String podCommand = null

	public void addBootstrapDependency() {

		try {
			commandRunner.runWithResult("which", "pod")
		} catch (CommandRunnerException ex) {
			// pod does not exist so add the dependency
			dependsOn(XcodePlugin.COCOAPODS_BOOTSTRAP_TASK_NAME)
		}

	}

	public Boolean hasPodfile() {
		File podFile = new File(project.projectDir, "Podfile")
		podFile.exists()
	}

	void runPod(String parameter) {

		if (podCommand == null) {
			if (getDependsOn().contains(XcodePlugin.COCOAPODS_BOOTSTRAP_TASK_NAME)) {
				String podPath = commandRunner.runWithResult("ruby", "-rubygems", "-e", "puts Gem.user_dir")
				podCommand = podPath + "/bin/pod"
			} else {
				// use global install cocoapods
				try {
					podCommand = commandRunner.runWithResult("which", "pod")
				} catch (CommandRunnerException ex) {
					// try to use the global install cocoapods if all went wrong
					podCommand = "/usr/local/bin/pod"
				}

			}
		}
		logger.lifecycle "Run pod " + parameter

		def output = services.get(StyledTextOutputFactory).create(CocoapodsInstallTask)

		ArrayList<String> commandList = []
		commandList.add podCommand
		commandList.add parameter
		commandRunner.run commandList, new ConsoleOutputAppender(output)
	}


}
