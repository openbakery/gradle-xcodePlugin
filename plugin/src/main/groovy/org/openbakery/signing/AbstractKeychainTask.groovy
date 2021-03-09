package org.openbakery.signing

import org.apache.commons.lang.StringUtils
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories
import org.openbakery.AbstractXcodeTask
import org.openbakery.XcodeBuildPluginExtension
import org.openbakery.codesign.Security


abstract class AbstractKeychainTask extends AbstractXcodeTask {

	Security security

	AbstractKeychainTask() {
		security = new Security(commandRunner)
	}


	@Input
	@Optional
	List<File> getKeychainList() {
		return security.getKeychainList()
	}

	def setKeychainList(List<File> keychainList) {
		security.setKeychainList(keychainList)
	}

	/**
	 * remove all gradle keychains from the keychain search list
	 * @return
	 */
	def removeGradleKeychainsFromSearchList() {
		List<File>keychainList = getKeychainList()
		logger.debug("project.xcodebuild.signing.keychain should be removed: {}", project.xcodebuild.signing.keychainPathInternal)
		if (project.xcodebuild.signing.keychainPathInternal != null) {
			keychainList.remove(project.xcodebuild.signing.keychainPathInternal)
		}
		setKeychainList(keychainList)
	}

	def cleanupKeychain() {
		project.xcodebuild.signing.signingDestinationRoot.deleteDir()
		removeGradleKeychainsFromSearchList()
	}



}
