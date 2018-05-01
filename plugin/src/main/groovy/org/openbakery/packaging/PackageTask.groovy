package org.openbakery.packaging

import org.openbakery.AbstractXcodeBuildTask

class PackageTask extends AbstractXcodeBuildTask {

	public static final String NAME = "package"

	PackageTask() {
		super()

		dependsOn(PackageLegacyTask.NAME)
		dependsOn(PackageTaskIosAndTvOS.NAME)
	}
}
