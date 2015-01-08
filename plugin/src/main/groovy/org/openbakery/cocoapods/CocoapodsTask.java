package org.openbakery.cocoapods;

import org.apache.commons.io.FileUtils;
import org.gradle.StartParameter;
import org.gradle.api.tasks.TaskAction;
import org.gradle.logging.StyledTextOutput;
import org.gradle.logging.StyledTextOutputFactory;
import org.openbakery.AbstractXcodeTask;
import org.openbakery.output.ConsoleOutputAppender;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by rene on 05.08.14.
 */
public class CocoapodsTask extends AbstractXcodeTask {

	String podCommand = null;

	public CocoapodsTask() {
		super();
		setDescription("Installs the pods for the given project");
	}


	public Boolean hasPodfile() {
		File podFile = new File(getProject().getProjectDir(), "Podfile");
		return podFile.exists();
 	}

	void runPod(String parameter) {

		if (podCommand == null) {

			String podPath = commandRunner.runWithResult("ruby", "-rubygems", "-e", "puts Gem.user_dir");

			podCommand = podPath + "/bin/pod";

		}
		getLogger().lifecycle("Run pod install");

		StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(CocoapodsTask.class);


	 	ArrayList<String>commandList = new ArrayList<String>();
		commandList.add(podCommand);
		commandList.add(parameter);
		commandRunner.run(commandList, new ConsoleOutputAppender(output));

	}

	@TaskAction
	void install() throws IOException {


		File podsDirectory = new File(getProject().getProjectDir(), "Pods");

		StartParameter startParameter = getProject().getGradle().getStartParameter();
		if (startParameter.isRefreshDependencies()) {
			if (podsDirectory.exists()) {
				getLogger().lifecycle("Deleting pods directory");
				FileUtils.deleteDirectory(podsDirectory);
			}
		}

		if (podsDirectory.exists() && podsDirectory.isDirectory()) {
			getLogger().lifecycle("Skipping installing pods, because the Pods directory already exists");
			return;
		}

		getLogger().lifecycle("Install/Update cocoapods");
		commandRunner.run("gem", "install", "-N", "--user-install", "cocoapods");


		runPod("setup");
		runPod("install");

	}
}
