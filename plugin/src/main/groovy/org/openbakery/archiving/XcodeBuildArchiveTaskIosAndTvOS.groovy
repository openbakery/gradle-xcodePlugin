package org.openbakery.archiving

import groovy.transform.CompileStatic
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.openbakery.AbstractXcodeBuildTask
import org.openbakery.CommandRunner
import org.openbakery.PrepareXcodeArchivingTask
import org.openbakery.signing.ProvisioningInstallTask
import org.openbakery.util.PathHelper
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcodebuild

@CompileStatic
@CacheableTask
class XcodeBuildArchiveTaskIosAndTvOS extends AbstractXcodeBuildTask {

	@Input
	final Provider<String> scheme = project.objects.property(String)

	@InputFile
	final Property<RegularFile> xcConfigFile = newInputFile()

	@OutputFile
	final Provider<RegularFile> outputArchiveFile = newOutputFile()

	final Property<CommandRunner> commandRunnerProperty = project.objects.property(CommandRunner)

	public static final String NAME = "archiveXcodeBuild"

	XcodeBuildArchiveTaskIosAndTvOS() {
		super()

		dependsOn(ProvisioningInstallTask.TASK_NAME)
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
				commandRunnerProperty.get(),
				xcode,
				parameters,
				getDestinations())

		commandRunner.setOutputFile(getOutputTextFile())

		xcodeBuild.archive(scheme.get(),
				outputArchiveFile.get().asFile,
				xcConfigFile.get().asFile)
	}
}
