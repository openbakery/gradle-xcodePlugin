package org.openbakery.signing

import de.undercouch.gradle.tasks.download.Download
import groovy.transform.CompileStatic
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.file.Directory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.openbakery.CommandRunner
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.util.PlistHelper

@CompileStatic
class ProvisioningInstallTask extends Download {

	@Input
	final Provider<List<String>> mobileProvisioningList = project.objects.listProperty(String)

	@OutputFiles
	final ListProperty<File> registeredProvisioning = project.objects.listProperty(File)

	final Property<CommandRunner> commandRunnerProperty = project.objects.property(CommandRunner)
	final Property<PlistHelper> plistHelperProperty = project.objects.property(PlistHelper)
	final Provider<Directory> outputDirectory = project.layout.directoryProperty()

	public static final String PROVISIONING_NAME_BASE = "gradle-"
	public static final String TASK_NAME = "provisioningInstall"
	public static final String TASK_DESCRIPTION = "Installs the given provisioning profile"

	static final File PROVISIONING_DIR = new File(System.getProperty("user.home") +
			"/Library/MobileDevice/Provisioning Profiles/")

	ProvisioningInstallTask() {
		super()
		this.description = TASK_DESCRIPTION

		this.onlyIf { !mobileProvisioningList.get().empty }
	}

	@TaskAction
	@Override
	void download() throws IOException {
		outputDirectory.get().asFile.mkdirs()
		configureDownload()
		super.download()
		postDownload()
	}

	private void configureDownload() {
		this.src(mobileProvisioningList.get().asList())
		this.dest(outputDirectory.get().asFile)
		this.acceptAnyCertificate(true)
	}

	private void postDownload() {
		final List<File> files = rename()
		deleteFilesOnExit(files)

		final List<File> libraryFiles = files.each { registeredProvisioning.add(it) }
				.collect { linkToUserlibraryOfProvisioning(it) }

		deleteFilesOnExit(libraryFiles)
	}

	private List<File> rename() {
		return mobileProvisioningList.get()
				.collect { new File(outputDirectory.get().asFile, FilenameUtils.getName(it)) }
				.collect { return renameProvisioningFile(it) }
	}

	private void deleteFilesOnExit(final List<File> files) {
		project.gradle.buildFinished {
			files.each {
				logger.debug("Delete file : " + it.absolutePath)
				it.delete()
			}
		}
	}

	private File renameProvisioningFile(File file) {
		assert file.exists(): "Cannot rename a non existing file"

		ProvisioningProfileReader reader = new ProvisioningProfileReader(file,
				commandRunnerProperty.get(),
				plistHelperProperty.get())

		String fileName = PROVISIONING_NAME_BASE + reader.getUUID() + "." + FilenameUtils.getExtension(file.getName())

		File result = new File(outputDirectory.get().asFile, fileName)
		assert file.renameTo(result): "Failed to rename the provisioning file"
		return result
	}

	private File linkToUserlibraryOfProvisioning(File file) {
		if (!PROVISIONING_DIR.exists()) {
			PROVISIONING_DIR.mkdirs()
		}

		File destinationFile = new File(PROVISIONING_DIR, file.name)
		FileUtils.copyFile(file, destinationFile)
		return destinationFile
	}
}
