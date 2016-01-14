package org.openbakery.signing

import org.apache.commons.lang.StringUtils
import org.openbakery.AbstractXcodeTask
import org.openbakery.XcodeBuildPluginExtension

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 23.08.13
 * Time: 11:39
 * To change this template use File | Settings | File Templates.
 */
abstract class AbstractKeychainTask extends AbstractXcodeTask {


	List<String> getKeychainList() {
		String keychainList = commandRunner.runWithResult(["security", "list-keychains"])

		List<String> result = []

		for (String keychain in keychainList.split("\n")) {
			String trimmedKeychain = keychain.replaceAll(/^\s*\"|\"$/, "")
			if (!trimmedKeychain.equals("/Library/Keychains/System.keychain")) {
				File keychainFile = new File(trimmedKeychain)
				if (keychainFile.exists()) {
					result.add(trimmedKeychain);
				}
			}
		}
		return result;
	}

	def setKeychainList(keychainList) {
		def commandList = [
						"security",
						"list-keychains",
						"-s"
		]
		for (String keychain in keychainList) {
			commandList.add(keychain);
		}
		commandRunner.run(commandList)
	}

	/**
	 * remove all gradle keychains from the keychain search list
	 * @return
	 */
	def removeGradleKeychainsFromSearchList() {
		List<String>keychainList = getKeychainList();
		if (project.xcodebuild.signing.keychain != null) {
			keychainList.remove(project.xcodebuild.signing.keychain.absolutePath)
		}
		setKeychainList(keychainList)
	}
}
