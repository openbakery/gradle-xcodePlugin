package org.openbakery.codesign

import org.apache.commons.lang.StringUtils
import org.openbakery.CommandRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Security {
	private static Logger logger = LoggerFactory.getLogger(Security.class)

	private CommandRunner commandRunner

	Security(CommandRunner commandRunner) {
		this.commandRunner = commandRunner
	}

	/**
	 * Get all available keychains. The System.keychain is excluded
	 * @return
	 */
	List<File> getKeychainList() {
		String keychainList = commandRunner.runWithResult(["security", "list-keychains"])
		List<File> result = []
		for (String keychain in keychainList.split("\n")) {
			String trimmedKeychain = keychain.replaceAll(/^\s*\"|\"$/, "")
			if (!trimmedKeychain.equals("/Library/Keychains/System.keychain")) {
				File keychainFile = new File(trimmedKeychain)
				if (keychainFile.exists()) {
					result.add(keychainFile)
				}
			}
		}
		return result
	}

	/**
	 * set the keychain list
	 * @param keychainList
	 */
	void setKeychainList(List<File> keychainList) {
		if (keychainList == null) {
			throw new IllegalArgumentException("Given keychain list is null")
		}

		if (keychainList.size() == 0) {
			return
		}

		def commandList = [
						"security",
						"list-keychains",
						"-s"
		]
		for (File keychain in keychainList) {
			commandList.add(keychain.absolutePath)
		}
		commandRunner.run(commandList)
	}

	/**
	 * create the keychain if it does not exists
	 * @param keychainFile
	 * @param keychainPassword
	 */
	void createKeychain(File keychainFile, String keychainPassword) {
		if (keychainFile == null) {
			throw new IllegalArgumentException("Given keychain is null")
		}

		if (keychainFile.exists()) {
			logger.debug("Keychain file exists")
			return
		}
		logger.debug("creating keychain: {}", keychainFile.absolutePath)
		commandRunner.run(["security", "create-keychain", "-p", keychainPassword, keychainFile.absolutePath])
	}

	/**
	 * imports the given certificate into the given keychain
	 * @param certificate
	 * @param certificatePassword
	 * @param keychain
	 * @return
	 */
	void importCertificate(File certificate, String certificatePassword, File keychain) {
		if (certificate == null) {
			throw new IllegalArgumentException("Given certificate is null")
		}
		if (keychain == null) {
			throw new IllegalArgumentException("Given keychain is null")
		}

		if (!certificate.exists()) {
			logger.debug("cannot import certificate because certificate does no exist: {}", certificate.absolutePath)
			throw new IllegalArgumentException("Given certificate does not exist")
		}
		if (!keychain.exists()) {
			logger.debug("cannot import certificate because keychain does no exist: {}", keychain.absolutePath)
			throw new IllegalArgumentException("Given keychain does not exist")
		}
		logger.debug("importCertificate")
		commandRunner.run(["security", "-v", "import", certificate.absolutePath, "-k", keychain.absolutePath, "-P", certificatePassword, "-T", "/usr/bin/codesign"])
	}

	String getIdentity(File keychain) {
		logger.debug("get identity from keychain {}", keychain)
		if (keychain == null) {
			throw new IllegalArgumentException("Given keychain is null")
		}
		if (!keychain.exists()) {
			logger.debug("Given keychain does no exist: {}", keychain.absolutePath)
			throw new IllegalArgumentException("Given keychain does not exist")
		}

		def IDENTITY_PATTERN = ~/\s*\d+\)\s*(\w+)\s*\"(.*)\"/

		String identities = commandRunner.runWithResult(["security", "find-identity", "-v", "-p", "codesigning", keychain.absolutePath])

		if (StringUtils.isEmpty(identities)) {
			return null
		}
		def matcher = IDENTITY_PATTERN.matcher(identities)
		String identity = null
		if (matcher.find()) {
			identity = matcher[0][1]
		}
		if (!matcher.find()) {
			// only use the identify if only one was found!!!
			// otherwise leave it to the default value null
			logger.debug("identity found: {}", identity)
			return identity
		}
		logger.debug("multiple identies found so return null")
		return null
	}

	void setTimeout(int timeout, File keychainFile) {
		logger.debug("get identity from keychain {}", keychainFile)
		if (keychainFile == null) {
			throw new IllegalArgumentException("Given keychain is null")
		}
		if (!keychainFile.exists()) {
			logger.debug("Given keychain does no exist: {}", keychainFile.absolutePath)
			throw new IllegalArgumentException("Given keychain does not exist")
		}

		commandRunner.run(["security", "-v", "set-keychain-settings", "-lut", Integer.toString(timeout), keychainFile.absolutePath])
	}

	def setPartitionList(File keychainFile, String keychainPassword) {
		logger.debug("set partition list for keychain {}", keychainFile)
		if (keychainFile == null) {
			throw new IllegalArgumentException("Given keychain is null")
		}
		if (!keychainFile.exists()) {
			logger.debug("Given keychain does no exist: {}", keychainFile.absolutePath)
			throw new IllegalArgumentException("Given keychain does not exist")
		}
		String identity = getIdentity(keychainFile)
		commandRunner.run(["security", "set-key-partition-list", "-S", "apple:", "-k", keychainPassword, "-D", identity, "-t", "private", keychainFile.absolutePath])
	}
}
