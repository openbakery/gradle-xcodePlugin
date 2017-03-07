package org.openbakery.cocoapods

import org.gradle.api.tasks.TaskAction

class CocoapodsUpdateTask extends AbstractCocoapodsTask {

	public CocoapodsUpdateTask() {
		super()
		addBootstrapDependency()
		setDescription "Updates the pods for the given project"
	}

	@TaskAction
	void update() throws IOException {
		runPod("setup")
		runPod("update")
	}
}
