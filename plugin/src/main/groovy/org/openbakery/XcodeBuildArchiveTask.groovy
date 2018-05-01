package org.openbakery

class XcodeBuildArchiveTask extends AbstractXcodeBuildTask {

	public static final String NAME = "archive"

	XcodeBuildArchiveTask() {
		super()

		dependsOn(XcodeBuildArchiveTaskIosAndTvOS.NAME,
				XcodeBuildLegacyArchiveTask.NAME)

		setDescription("Archive and export the project")
	}
}
