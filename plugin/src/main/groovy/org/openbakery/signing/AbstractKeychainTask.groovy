package org.openbakery.signing

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.OutputFile
import org.openbakery.codesign.Security

@CompileStatic
abstract class AbstractKeychainTask extends DefaultTask {

	@OutputFile
	final RegularFileProperty keyChainFile = newOutputFile()

	final Property<Security> security = project.objects.property(Security)
	final DirectoryProperty outputDirectory = newOutputDirectory()

	AbstractKeychainTask() {
		setupGarbageCleaner()
	}

	List<File> getKeychainList() {
		return security.get().getKeychainList()
	}

	void setKeychainList(List<File> keychainList) {
		security.get().setKeychainList(keychainList)
	}

	void removeGradleKeychainsFromSearchList() {
		if (keyChainFile.present) {
			logger.info("Remove the temporary keychain from search list")
			List<File> list = getKeychainList()
			list.remove(keyChainFile.get())
			setKeychainList(list)
		}
	}

	void deleteTemporaryKeyChainFile() {
		if (keyChainFile.present) {
			logger.info("Delete the temporary keychain file")
			keyChainFile.get().asFile.delete()
		}
	}

	private void setupGarbageCleaner() {
		project.gradle.buildFinished {
			removeGradleKeychainsFromSearchList()
			deleteTemporaryKeyChainFile()
		}
	}
}
