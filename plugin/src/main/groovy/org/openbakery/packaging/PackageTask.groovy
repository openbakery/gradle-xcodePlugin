package org.openbakery.packaging

import org.gradle.api.DefaultTask

class PackageTask extends DefaultTask {

	public static final String NAME = "package"

	PackageTask() {
		super()

		dependsOn(PackageLegacyTask.NAME)
		dependsOn(PackageTaskIosAndTvOS.NAME)
	}
}
