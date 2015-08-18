package org.openbakery.cocoapods

import org.gradle.StartParameter
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.logging.StyledTextOutputFactory
import org.openbakery.CommandRunner
import org.openbakery.output.ConsoleOutputAppender

/**
 * Install cocoapods in a project
 *
 * @since 05.08.14
 * @author rene
 * @author rahul
 */
public class CocoapodsTask extends DefaultTask {

	String podCommand = null
	CommandRunner commandRunner

	public CocoapodsTask() {
		super()
		setDescription "Installs the pods for the given project"
		commandRunner = new CommandRunner()
	}


	public Boolean hasPodfile() {
		File podFile = new File(project.projectDir, "Podfile")
		podFile.exists()
	}

	void runPod(String parameter) {

		if (podCommand == null) {
			String podPath = commandRunner.runWithResult("ruby", "-rubygems", "-e", "puts Gem.user_dir")
			podCommand = podPath + "/bin/pod"
		}
		logger.lifecycle "Run pod install"

		def output = services.get(StyledTextOutputFactory).create(CocoapodsTask)

		ArrayList<String> commandList = []
		commandList.add podCommand
		commandList.add parameter
		commandRunner.run commandList, new ConsoleOutputAppender(output)

	}

	@TaskAction
	void install() throws IOException {

		File manifestFile = new File(project.projectDir, 'Pods/Manifest.lock')
		File podLock = new File(project.projectDir, 'Podfile.lock')

		StartParameter startParameter = project.gradle.startParameter

		if (!startParameter.isRefreshDependencies()) { // refresh dependencies should always reinstall the pods
			if (manifestFile.exists() && podLock.exists() && manifestFile.text == podLock.text) {
				logger.debug "Skipping installing pods, because Manifest.lock and Podfile.lock are identical"
				return
			}
		}

		logger.lifecycle "Install/Update cocoapods"
		commandRunner.run("gem", "install", "-N", "--user-install", "cocoapods")

		runPod("setup")
		runPod("install")

	}
}
