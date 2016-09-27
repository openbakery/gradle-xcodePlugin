package org.openbakery.cocoapods

/**
 * Created by rene on 27.09.16.
 */
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
