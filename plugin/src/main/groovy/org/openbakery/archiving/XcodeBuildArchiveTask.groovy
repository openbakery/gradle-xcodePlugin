package org.openbakery.archiving

import org.openbakery.AbstractXcodeBuildTask

class XcodeBuildArchiveTask extends AbstractXcodeBuildTask {

	public static final String NAME = "archive"

	XcodeBuildArchiveTask() {
		super()

		dependsOn(XcodeBuildArchiveTaskIosAndTvOS.NAME,
				XcodeBuildLegacyArchiveTask.NAME)

		setDescription("Archive and export the project")
	}
}
