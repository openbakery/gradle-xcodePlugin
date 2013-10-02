package org.openbakery

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 23.08.13
 * Time: 11:39
 * To change this template use File | Settings | File Templates.
 */
abstract class AbstractKeychainTask extends AbstractXcodeTask {


	def getKeychainList() {
		String keychainList = commandRunner.runWithResult(["security", "list-keychains"])

		def result = []

		for (String keychain in keychainList.split("\n")) {
			String trimmedKeychain = keychain.replaceAll(/^\s*\"|\"$/, "")
			if (!trimmedKeychain.equals("/Library/Keychains/System.keychain")) {
				result.add(trimmedKeychain);
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
			if (new File(keychain).exists()) {
				commandList.add(keychain);
			}
		}
		commandRunner.run(commandList)
	}

}
