package org.openbakery.cocoapods;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.openbakery.AbstractXcodeTask;

import java.io.File;
import java.util.List;

/**
 * Created by rene on 05.08.14.
 */
public class CocoapodsTask extends AbstractXcodeTask {


	public CocoapodsTask() {
		super();
		setDescription("Installs the pods for the given project");
	}


	@InputFiles
	@SkipWhenEmpty
 	public FileCollection getSource() {

		File podFile = new File(getProject().getProjectDir(), "Podfile");
		if (podFile.exists()) {
			return getProject().files(podFile);
		}

		return getProject().files();
 	}


	@TaskAction
	void install() {
		// first install or update cocoapods

		getLogger().quiet("Install/Update cocoapods");
		commandRunner.run("gem", "install", "--user-install", "cocoapods");

		String result = commandRunner.runWithResult("ruby", "-rubygems", "-e", "puts Gem.user_dir");

		getLogger().quiet("Run pod install");
		commandRunner.runWithResult(result + "/bin/pod", "install");

	}
}
