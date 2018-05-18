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
import org.gradle.api.tasks.OutputDirectory
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

	@OutputDirectory
	final Provider<Directory> outputDirectory = project.layout.directoryProperty()

	final ListProperty<ProvisioningFile> registeredProvisioningFiles = project.objects.listProperty(ProvisioningFile)

	final Property<CommandRunner> commandRunnerProperty = project.objects.property(CommandRunner)
	final Property<PlistHelper> plistHelperProperty = project.objects.property(PlistHelper)

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

	void registerProvisioning(ProvisioningFile provisioningFile) {
		registeredProvisioning.add(provisioningFile.getFile())
		registeredProvisioningFiles.add(provisioningFile)
	}

	File registerProvisioningInToUserLibrary(ProvisioningFile provisioningFile) {
		PROVISIONING_DIR.mkdirs()

		File destinationFile = new File(PROVISIONING_DIR, provisioningFile.getFormattedName())
		FileUtils.copyFile(provisioningFile.getFile(), destinationFile)
		return destinationFile
	}

	private void postDownload() {
		// For convenience we rename the mobile provisioning file in to a formatted name
		List<ProvisioningFile> files = rename()
		deleteFilesOnExit(files.collect { it.file })

		// Register it
		files.each(this.&registerProvisioning)

		// Register into the user library
		List<File> registeredFiles = files.collect(this.&registerProvisioningInToUserLibrary)
		deleteFilesOnExit(registeredFiles)
	}

	File fileFromPath(String path) {
		return new File(outputDirectory.get().asFile, FilenameUtils.getName(path))
	}

	ProvisioningFile toProvisioningFile(File file) {
		ProvisioningProfileReader reader = new ProvisioningProfileReader(file,
				commandRunnerProperty.get(),
				plistHelperProperty.get())

		File renamedFile = new File(file.parentFile,
				ProvisioningFile.formattedName(reader.getUUID(), file))
		file.renameTo(renamedFile)

		return new ProvisioningFile(renamedFile,
				reader.getApplicationIdentifier(),
				reader.getUUID(),
				reader.getTeamIdentifierPrefix(),
				reader.getTeamName(),
				reader.getName())
	}

	private List<ProvisioningFile> rename() {
		return mobileProvisioningList.get()
				.collect(this.&fileFromPath)
				.collect(this.&toProvisioningFile)
	}

	private void deleteFilesOnExit(final List<File> files) {
		project.gradle.buildFinished {
			files.each {
				logger.debug("Delete file : " + it.absolutePath)
				it.delete()
			}
		}
	}
}
