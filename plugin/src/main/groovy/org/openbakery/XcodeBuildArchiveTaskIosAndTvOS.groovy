package org.openbakery

import groovy.transform.CompileStatic
import org.gradle.api.tasks.*
import org.openbakery.util.PathHelper
import org.openbakery.xcode.Xcodebuild

@CompileStatic
@CacheableTask
class XcodeBuildArchiveTaskIosAndTvOS extends AbstractXcodeBuildTask {
	
	public static final String TASK_NAME = "archive"

	XcodeBuildArchiveTaskIosAndTvOS() {
		super()

		dependsOn(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)
		dependsOn(PrepareXcodeArchivingTask.NAME)

		this.description = "Prepare the app bundle that it can be archive"
	}

	@InputFile
	File getXcConfigFile() {
		return PathHelper.resolveXcConfigFile(project)
	}

	@Input
	String getScheme() {
		return getXcodeExtension().scheme
	}

	@OutputFile
	File getOutputTextFile() {
		return PathHelper.resolveArchivingLogFile(project)
	}

	@OutputDirectory
	File getOutputDirectory() {
		File archiveDirectory = PathHelper.resolveArchiveFolder(project)
		archiveDirectory.mkdirs()
		return archiveDirectory
	}

	@TaskAction
	private void archive() {
		Xcodebuild xcodeBuild = new Xcodebuild(project.projectDir,
				commandRunner,
				xcode,
				parameters,
				getDestinations())

		commandRunner.setOutputFile(getOutputTextFile())

		xcodeBuild.archive(getScheme(),
				PathHelper.resolveArchiveFile(project, scheme),
				getXcConfigFile())
	}
}
