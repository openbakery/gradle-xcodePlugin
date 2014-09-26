package org.openbakery.cocoapods;

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


	public CocoapodsTask() {
		super();
		setDescription("Installs the pods for the given project");
	}


	public Boolean hasPodfile() {
		File podFile = new File(getProject().getProjectDir(), "Podfile");
		return podFile.exists();
 	}


	@TaskAction
	void install() throws IOException {
		// first install or update cocoapods

		File podsDirectory = new File(getProject().getProjectDir(), "Pods");
		if (podsDirectory.exists() && podsDirectory.isDirectory()) {
			getLogger().lifecycle("Skipping installing pods, because the Pods directory already exists");
			return;
		}


		getLogger().lifecycle("Install/Update cocoapods");
		commandRunner.run("gem", "install", "--user-install", "cocoapods");

		String result = commandRunner.runWithResult("ruby", "-rubygems", "-e", "puts Gem.user_dir");

		getLogger().lifecycle("Run pod install");

		StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(CocoapodsTask.class);

	 	ArrayList<String>commandList = new ArrayList<String>();
		commandList.add(result + "/bin/pod");
		commandList.add("install");

		commandRunner.run(commandList, new ConsoleOutputAppender(output));

	}
}
