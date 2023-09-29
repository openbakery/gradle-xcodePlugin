package org.openbakery.codesign

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.openbakery.CommandRunner
import org.openbakery.util.DateHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.security.cert.CertificateException
import java.text.ParseException

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

		checkIfCertificateIsValid(certificate, certificatePassword)

		if (!keychain.exists()) {
			logger.debug("cannot import certificate because keychain does no exist: {}", keychain.absolutePath)
			throw new IllegalArgumentException("Given keychain does not exist")
		}
		logger.debug("importCertificate")
		commandRunner.run(["security", "-v", "import", certificate.absolutePath, "-k", keychain.absolutePath, "-P", certificatePassword, "-T", "/usr/bin/codesign"])
	}

	void checkIfCertificateIsValid(File certificate, String certificatePassword) {

		logger.debug("checkIfCertificateIsValid {}", certificate)


		File tmpDir = new File(System.getProperty("java.io.tmpdir"))
		def pkcs12File = new File(tmpDir, "pkcs12File_" + FilenameUtils.getBaseName(certificate.path) + ".pfx")
		pkcs12File.deleteOnExit()

		def result = commandRunner.runWithResult(["openssl",  "pkcs12" ,  "-in", certificate.absolutePath, "-nodes",  "-passin", "pass:" + certificatePassword, "-out", pkcs12File.absolutePath])
		if (result != null && result != "") {
			commandRunner.run(["openssl",  "pkcs12" ,  "-in", certificate.absolutePath, "-nodes", "-legacy",  "-passin", "pass:" + certificatePassword, "-out", pkcs12File.absolutePath])
		}
		result = commandRunner.runWithResult(["openssl",  "x509",  "-in", pkcs12File.absolutePath , "-noout",  "-enddate"])

		logger.debug("checkIfCertificateIsValid enddate: {}", result)

		if (result == null) {
			throw new  CertificateException("openssl command returned no result.")
		}

		if (result.startsWith("Mac verify error: invalid password?")) {

			throw new  CertificateException("Wrong password to open certificate.")
		}

		String[] parts = result.split("notAfter=")

		if (parts.length > 1) {

			def dateHelper = new DateHelper()
			def certificateExpiration = dateHelper.parseOpenSSLDate(parts[1])

			if (certificateExpiration.after(new Date())) {

				logger.debug("checkIfCertificateIsValid certificate is valid")

				return
			}

			throw new CertificateException("Given certificate has expired on: " + certificateExpiration.toString())
		}

		throw new CertificateException("Output from openssl command could not be parsed.")
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
		commandRunner.run(["security", "set-key-partition-list", "-S", "apple:,apple-tool:,codesign:", "-k", keychainPassword, "-D", identity, "-t", "private", keychainFile.absolutePath])
	}
}
