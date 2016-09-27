package org.openbakery.cocoapods

import org.gradle.StartParameter
import org.gradle.api.tasks.TaskAction

/**
 * Install cocoapods in a project
 *
 * @since 05.08.14
 * @author rene
 * @author rahul
 */
public class CocoapodsInstallTask extends AbstractCocoapodsTask {


	public CocoapodsInstallTask() {
		super()
		setDescription "Installs the pods for the given project"
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
		runPod("setup")
		runPod("install")
	}


}
