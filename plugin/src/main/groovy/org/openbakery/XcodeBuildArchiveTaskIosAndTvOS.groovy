package org.openbakery

import groovy.transform.CompileStatic
import org.gradle.api.Task
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.openbakery.util.PathHelper
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcodebuild

@CompileStatic
@CacheableTask
class XcodeBuildArchiveTaskIosAndTvOS extends AbstractXcodeBuildTask {

	public static final String NAME = "xcodeBuildArchive"

	XcodeBuildArchiveTaskIosAndTvOS() {
		super()

		dependsOn(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)
		dependsOn(PrepareXcodeArchivingTask.NAME)

		onlyIf(new Spec<Task>() {
			@Override
			boolean isSatisfiedBy(Task task) {
				return getXcodeExtension().getType() == Type.iOS ||
						getXcodeExtension().getType() == Type.tvOS
			}
		})

		this.description = "Use the xcodebuild archiver to create the project archive"
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
