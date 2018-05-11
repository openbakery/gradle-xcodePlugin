package org.openbakery.signing

import de.undercouch.gradle.tasks.download.Download
import groovy.transform.CompileStatic
import org.apache.commons.io.FilenameUtils
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.openbakery.CommandRunner
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.util.PlistHelper

@CompileStatic
class ProvisioningInstallTask extends Download {

	@Input
	final Provider<List<String>> mobileProvisiongList = project.objects.listProperty(String)

	final Property<CommandRunner> commandRunnerProperty = project.objects.property(CommandRunner)
	final Property<PlistHelper> plistHelperProperty = project.objects.property(PlistHelper)

	final DirectoryProperty outputDirectory = newOutputDirectory()

	public final static PROVISIONING_NAME_BASE = "gradle-"

	static final String TASK_NAME = "provisioningInstall"
	static final String TASK_DESCRIPTION = "Installs the given provisioning profile"

	ProvisioningInstallTask() {
		super()
		this.description = TASK_DESCRIPTION

		this.onlyIf { !mobileProvisiongList.get().empty }
	}
//
//	void linkToLibraray(File mobileProvisionFile) {
//		File provisionPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/");
//		if (!provisionPath.exists()) {
//			provisionPath.mkdirs()
//		}
//
//		File mobileProvisionFileLinkToLibrary = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/" + mobileProvisionFile.getName());
//		if (mobileProvisionFileLinkToLibrary.exists()) {
//			mobileProvisionFileLinkToLibrary.delete()
//		}
//
//		commandRunner.run(["/bin/ln", "-s", mobileProvisionFile.absolutePath, mobileProvisionFileLinkToLibrary.absolutePath])
//	}

	@TaskAction
	@Override
	public void download() throws IOException {
		this.src(mobileProvisiongList.get().asList())
		this.dest(outputDirectory.asFile.get())
		this.acceptAnyCertificate(true)

		super.download()

		final List<File> files = rename()

		deleteFilesOnExit(files)

//
//		for (String mobileProvisionURI : project.xcodebuild.signing.mobileProvisionURI) {
//			def mobileProvisionFile = FileUtil.download(project,
//					project.xcodebuild.signing.mobileProvisionDestinationRoot,
//					mobileProvisionURI).absolutePath
//
//			ProvisioningProfileReader provisioningProfileIdReader = new ProvisioningProfileReader(new File(mobileProvisionFile), this.commandRunner, this.plistHelper)
//
//			String uuid = provisioningProfileIdReader.getUUID()
//
//
//			String extension = FilenameUtils.getExtension(mobileProvisionFile)
//			String mobileProvisionName
//			mobileProvisionName = PROVISIONING_NAME_BASE + uuid + "." + extension
//
//
//			File downloadedFile = new File(mobileProvisionFile)
//			File renamedProvisionFile = new File(downloadedFile.getParentFile(), mobileProvisionName)
//			downloadedFile.renameTo(renamedProvisionFile)
//
//			project.xcodebuild.signing.addMobileProvisionFile(renamedProvisionFile)
//
//			linkToLibraray(renamedProvisionFile)
//		}

	}

	private List<File> rename() {
		return  mobileProvisiongList.get()
				.collect { new File(outputDirectory.get().asFile, FilenameUtils.getName(it)) }
				.collect { return renameProvisioningFile(it) }
	}

	private void deleteFilesOnExit(final List<File> files) {
		project.gradle.buildFinished {
			files.each { it.delete() }
		}

		return files
	}

	private File renameProvisioningFile(File file) {
		ProvisioningProfileReader reader = new ProvisioningProfileReader(file,
				commandRunnerProperty.get(),
				plistHelperProperty.get())

		String fileName = reader.getUUID() + "." + FilenameUtils.getExtension(file.getName())

		File result = new File(outputDirectory.get().asFile, fileName)
		assert file.renameTo(result): "Failed to rename the provisioning file"
		return result
	}
}
