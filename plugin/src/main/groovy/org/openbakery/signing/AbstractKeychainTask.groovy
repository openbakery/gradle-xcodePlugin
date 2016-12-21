package org.openbakery.signing

import org.apache.commons.lang.StringUtils
import org.openbakery.AbstractXcodeTask
import org.openbakery.XcodeBuildPluginExtension
import org.openbakery.codesign.Security

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 23.08.13
 * Time: 11:39
 * To change this template use File | Settings | File Templates.
 */
abstract class AbstractKeychainTask extends AbstractXcodeTask {

	Security security

	AbstractKeychainTask() {
		security = new Security(commandRunner)
	}


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
		logger.debug("project.xcodebuild.signing.keychain should not be removed: {}", project.xcodebuild.signing.keychainPathInternal)
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
