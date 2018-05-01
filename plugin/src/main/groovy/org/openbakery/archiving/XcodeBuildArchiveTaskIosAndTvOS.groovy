package org.openbakery.archiving

import groovy.transform.CompileStatic
import org.gradle.api.Task
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.openbakery.AbstractXcodeBuildTask
import org.openbakery.PrepareXcodeArchivingTask
import org.openbakery.XcodePlugin
import org.openbakery.util.PathHelper
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcodebuild

@CompileStatic
@CacheableTask
class XcodeBuildArchiveTaskIosAndTvOS extends AbstractXcodeBuildTask {

	public static final String NAME = "archiveXcodeBuild"

	XcodeBuildArchiveTaskIosAndTvOS() {
		super()

		dependsOn(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)
		dependsOn(PrepareXcodeArchivingTask.NAME)

		this.description = "Use the xcodebuild archiver to create the project archive"

		onlyIf(new Spec<Task>() {
			@Override
			boolean isSatisfiedBy(Task task) {
				return getXcodeExtension().getType() == Type.iOS ||
						getXcodeExtension().getType() == Type.tvOS
			}
		})
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
