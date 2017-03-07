package org.openbakery.cocoapods

class CocoapodsBootstrapTask extends AbstractCocoapodsTask{

	public CocoapodsBootstrapTask() {
		super()
		setDescription "Bootstraps cocoapods"
	}

	def bootstrap() {
		logger.lifecycle "Bootstrap cocoapods"
		commandRunner.run("gem", "install", "-N", "--user-install", "cocoapods")

		runPod("setup")

	}
}
