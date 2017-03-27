package org.openbakery.codesign

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.openbakery.CommandRunner
import spock.lang.Specification

import java.security.cert.CertificateException
import java.text.ParseException

class SecuritySpecification extends Specification {


	CommandRunner commandRunner
	Security security
	File tmpDirectory
	File loginKeychain
	File certificateFile
	File pkcs12File

	def setup() {
		tmpDirectory = new File(System.getProperty("java.io.tmpdir"), 'gradle-xcodebuild')

		commandRunner = Mock(CommandRunner)
		security = new Security(commandRunner)
		loginKeychain = new File(tmpDirectory, "login.keychain")
		FileUtils.writeStringToFile(loginKeychain, "dummy")
		certificateFile = new File(tmpDirectory, "my.certificate")
		FileUtils.writeStringToFile(certificateFile, "dummy")

		pkcs12File = new File(System.getProperty("java.io.tmpdir"), "pkcs12File_" + FilenameUtils.getBaseName(certificateFile.path) + ".pfx")


	}

	def tearDown() {
		security = null
		commandRunner = null
		FileUtils.deleteDirectory(tmpDirectory)
	}

	String getSecurityList() {
		return  "    \""+ loginKeychain.absolutePath  + "\n" +
						"    \"/Users/me/Go/pipelines/Build-Appstore/build/codesign/gradle-1431356246879.keychain\"\n" +
						"    \"/Users/me/Go/pipelines/Build-Test/build/codesign/gradle-1431356877451.keychain\"\n" +
						"    \"/Users/me/Go/pipelines/Build-Continuous/build/codesign/gradle-1431419900260.keychain\"\n" +
						"    \"/Library/Keychains/System.keychain\""

	}


	static String DEFAULT_OPENSSL_OUTPUT = "MAC verified OK\n" +
			"notAfter=Mar 20 10:16:40 2118 GMT"


	def mockOpensslCertificate(String result = DEFAULT_OPENSSL_OUTPUT, String password = "certificatePassword") {
		commandRunner.run(["openssl",  "pkcs12",  "-in",  certificateFile.absolutePath, "-nodes",  "-passin", "pass:" + password, "-out", pkcs12File.absolutePath])
		commandRunner.runWithResult(["openssl",  "x509", "-in", pkcs12File.absolutePath, "-noout",  "-enddate"]) >> result
	}

	def "get keychain list"() {
		given:
		commandRunner.runWithResult(["security", "list-keychains"]) >> getSecurityList()

		when:
		List<String> keychainList = security.getKeychainList()

		then:
		keychainList.size == 1
	}


	def "set keychain list"() {
		File keychain = new File(tmpDirectory, "test.keychain")
		FileUtils.writeStringToFile(keychain, "foobar")

		when:
		security.setKeychainList([keychain])

		then:
		1 * commandRunner.run(["security", "list-keychains", "-s", keychain.absolutePath])
	}

	def "set keychain list with multiple items"() {
		File first = new File(tmpDirectory, "first.keychain")
		FileUtils.writeStringToFile(first, "first")
		File second = new File(tmpDirectory, "second.keychain")
		FileUtils.writeStringToFile(second, "second")

		def keychainList = [first, second]

		when:
		security.setKeychainList(keychainList)

		then:
		1 * commandRunner.run(["security", "list-keychains", "-s", first.absolutePath, second.absolutePath])

		cleanup:
		first.delete()
		second.delete()
	}


	def "set keychain list is null will throw exception"() {
		when:
		security.setKeychainList(null)

		then:
		def exception = thrown(IllegalArgumentException)
		exception.message == "Given keychain list is null"
	}

	def "set keychain list is empty will do nothing"() {
		when:
		security.setKeychainList([])

		then:
		0 * commandRunner.run(["security", "list-keychains", "-s"])
	}


	def "create keychain"() {
		File keychain = new File(tmpDirectory, "test.keychain")
		if (keychain.exists()) {
			keychain.delete()
		}

		when:
		security.createKeychain(keychain, "password")

		then:
		1 * commandRunner.run(["security", "create-keychain", "-p", "password", keychain.absolutePath])
	}

	def "do not create keychain if keychain is null will throw exception"() {
		given:
		File keychain = new File(tmpDirectory, "test.keychain")

		when:
		security.createKeychain(null, "password")

		then:
		def exception = thrown(IllegalArgumentException)
		exception.message == "Given keychain is null"

		0 * commandRunner.run(["security", "create-keychain", "-p", "password", keychain.absolutePath])
	}

	def "do not create keychain if file exists"() {
		given:
		File keychain = new File(tmpDirectory, "test.keychain")
		FileUtils.writeStringToFile(keychain, "foobar")

		when:
		security.createKeychain(keychain, "password")

		then:
		0 * commandRunner.run(["security", "create-keychain", "-p", "password", keychain.absolutePath])

		cleanup:
		keychain.delete()
	}

	def "import certificate into keychain"() {
		given:
		mockOpensslCertificate()
		File keychain = new File(tmpDirectory, "test.keychain")
		FileUtils.writeStringToFile(keychain, "keychain")

		when:
		security.importCertificate(certificateFile, "certificatePassword", keychain)

		then:
		1 * commandRunner.run(["security", "-v", "import", certificateFile.absolutePath, "-k", keychain.absolutePath, "-P", "certificatePassword", "-T", "/usr/bin/codesign"])

		cleanup:
		keychain.delete()

	}


	def "do not import certificate into keychain if keychain does not exists will throw exception"() {
		given:
		File keychain = new File(tmpDirectory, "test.keychain")
		mockOpensslCertificate()

		when:
		security.importCertificate(certificateFile, "certificatePassword", keychain)

		then:
		def exception = thrown(IllegalArgumentException)
		exception.message == "Given keychain does not exist"
		0 * commandRunner.run(["security", "-v", "import", certificateFile.absolutePath, "-k", keychain.absolutePath, "-P", "certificatePassword", "-T", "/usr/bin/codesign"])
	}

	def "do not import certificate into keychain if certificate does not exists will throw exception"() {
		given:
		File keychain = new File(tmpDirectory, "test.keychain")
		FileUtils.writeStringToFile(keychain, "keychain")
		File certificate = new File(tmpDirectory, "certificate.p12")

		when:
		security.importCertificate(certificate, "certificatePassword", keychain)

		then:
		def exception = thrown(IllegalArgumentException)
		exception.message == "Given certificate does not exist"
		0 * commandRunner.run(["security", "-v", "import", certificate.absolutePath, "-k", keychain.absolutePath, "-P", "certificatePassword", "-T", "/usr/bin/codesign"])

		cleanup:
		keychain.delete()
	}

	def "do not import certificate into keychain if certificate is null will throw exception"() {
		given:
		File keychain = new File(tmpDirectory, "test.keychain")
		FileUtils.writeStringToFile(keychain, "keychain")

		when:
		security.importCertificate(null, "certificatePassword", keychain)

		then:
		def exception = thrown(IllegalArgumentException)
		exception.message == "Given certificate is null"


		cleanup:
		keychain.delete()
	}

	def "do not import certificate into keychain if keychain is null will throw exception"() {
		when:
		security.importCertificate(new File("test"), "certificatePassword", null)

		then:
		def exception = thrown(IllegalArgumentException)
		exception.message == "Given keychain is null"
	}

	def "get identity from keychain that is null will throw exception"() {
		when:
		security.getIdentity(null)

		then:
		def exception = thrown(IllegalArgumentException)
		exception.message == "Given keychain is null"
	}

	def "get identity from keychain that does not exist will throw exception"() {
		given:
		File keychain = new File(tmpDirectory, "test.keychain")

		when:
		security.getIdentity(keychain)

		then:
		def exception = thrown(IllegalArgumentException)
		exception.message == "Given keychain does not exist"
	}

	def "get identity from keychain runs find-identity command"() {
		given:
		File keychain = new File(tmpDirectory, "test.keychain")
		FileUtils.writeStringToFile(keychain, "keychain")

		when:
		security.getIdentity(keychain)

		then:
		1 * commandRunner.runWithResult(["security", "find-identity", "-v", "-p", "codesigning", keychain.absolutePath])

		cleanup:
		keychain.delete()
	}

	def "get identity from keychain returns identity"() {
		given:
		File keychain = new File(tmpDirectory, "test.keychain")
		FileUtils.writeStringToFile(keychain, "keychain")

		String data = FileUtils.readFileToString(new File("src/test/Resource/security-find-identity-single.txt"))
		commandRunner.runWithResult(["security", "find-identity", "-v", "-p", "codesigning", keychain.absolutePath]) >> data

		when:
		String identity = security.getIdentity(keychain)

		then:

		identity == "1111222233334444555566667777888899990000"

		cleanup:
		keychain.delete()
	}


	def "get identity from keychain returns null if multiple identies are found"() {
		given:
		File keychain = new File(tmpDirectory, "test.keychain")
		FileUtils.writeStringToFile(keychain, "keychain")

		String data = FileUtils.readFileToString(new File("src/test/Resource/security-find-identity-multiple.txt"))
		commandRunner.runWithResult(["security", "find-identity", "-v", "-p", "codesigning", keychain.absolutePath]) >> data

		when:
		String identity = security.getIdentity(keychain)

		then:
		identity == null

		cleanup:
		keychain.delete()
	}


	def "set timeout"() {
		given:
		File keychain = new File(tmpDirectory, "test.keychain")
		FileUtils.writeStringToFile(keychain, "keychain")

		when:
		security.setTimeout(3600, keychain)

		then:
		1 * commandRunner.run(["security", "-v", "set-keychain-settings", "-lut", "3600", keychain.absolutePath])

		cleanup:
		keychain.delete()
	}

	def "set timeout keychain null throws exception"() {
		when:
		security.setTimeout(3600, null)

		then:
		def exception = thrown(IllegalArgumentException)
		exception.message == "Given keychain is null"
	}

	def "set timeout keychain does not exists throws exception"() {
		File keychain = new File(tmpDirectory, "test.keychain")
		when:
		security.setTimeout(3600, keychain)

		then:
		def exception = thrown(IllegalArgumentException)
		exception.message == "Given keychain does not exist"
	}

	def "set partition list keychain is null"() {
		when:
		security.setPartitionList(null, "")

		then:
		def exception = thrown(IllegalArgumentException)
		exception.message == "Given keychain is null"
	}

	def "set partition list keychain does not exists throws exception"() {
		File keychain = new File(tmpDirectory, "test.keychain")
		when:
		security.setPartitionList(keychain, "keychain password")

		then:
		def exception = thrown(IllegalArgumentException)
		exception.message == "Given keychain does not exist"
	}

	def "set partition list"() {
		given:
		File keychain = new File(tmpDirectory, "test.keychain")
		FileUtils.writeStringToFile(keychain, "keychain")

		String data = FileUtils.readFileToString(new File("src/test/Resource/security-find-identity-single.txt"))
		commandRunner.runWithResult(["security", "find-identity", "-v", "-p", "codesigning", keychain.absolutePath]) >> data

		when:
		security.setPartitionList(keychain, "keychain password")

		then:
		1 * commandRunner.run(["security", "set-key-partition-list", "-S", "apple:", "-k", "keychain password", "-D", "1111222233334444555566667777888899990000", "-t", "private", keychain.absolutePath])
	}

	def "certificate does not exist throws exception"() {
		given:
		File keychain = new File(tmpDirectory, "test.keychain")

		when:
		security.importCertificate(new File("this file does not exist"), "", keychain)

		then:
		def exception = thrown(IllegalArgumentException)
		exception.message == "Given certificate does not exist"
	}

	def "run openssl command to get expired date"() {
		when:
		security.checkIfCertificateIsValid(certificateFile, "mypassword")


		then:
		interaction {
			1 * commandRunner.run(["openssl", "pkcs12", "-in", certificateFile.absolutePath, "-nodes", "-passin", "pass:mypassword", "-out", pkcs12File.absolutePath])
			1 * commandRunner.runWithResult(["openssl", "x509", "-in", pkcs12File.absolutePath, "-noout", "-enddate"])
		}


		def exception = thrown(CertificateException)
		exception.message == "openssl command returned no result."
	}



	def "check if certificate is valid returns valid"() {
		given:
		String opensslOutput = "MAC verified OK\n" +
				"notAfter=Mar 20 10:16:40 2118 GMT"
		mockOpensslCertificate(opensslOutput)

		expect:
		security.checkIfCertificateIsValid(certificateFile, "certificatePassword")
	}

	def "certificate is expired then throws exception"() {
		given:
		String opensslOutput = "MAC verified OK\n" +
				"notAfter=Mar 20 10:16:40 2016 GMT"

		mockOpensslCertificate(opensslOutput)

		when:
		security.checkIfCertificateIsValid(certificateFile, "certificatePassword")

		then:
		def exception = thrown(CertificateException)
		exception.message.startsWith("Given certificate has expired on:")
	}

	def "when openssl gives wrong output then throw exception"() {
		given:
		String opensslOutput = "openssl Wrong Output"
		mockOpensslCertificate(opensslOutput)

		when:
		security.checkIfCertificateIsValid(certificateFile, "certificatePassword")

		then:
		def exception = thrown(CertificateException)
		exception.message == "Output from openssl command could not be parsed."
	}

	def "using wrong password to open certificate throw exception" () {
		given:
		String opensslOutput = "Mac verify error: invalid password?"
		mockOpensslCertificate(opensslOutput, "wrongpassword")

		when:
		security.checkIfCertificateIsValid(certificateFile, "wrongpassword")

		then:
		def exception = thrown(CertificateException)
		exception.message == "Wrong password to open certificate."
	}
/*
	def "test" () {
		given:
		String opensslOutput = "Mac verify error: invalid password?"
		security.commandRunner = new CommandRunner()

		certificateFile = new File("/Users/stefangugarel/Projekte/ICS/Proofics/Proofmaker/build/codesign/Application_Drobnik_KG.p12")
		expect:
		security.checkIfCertificateIsValid(certificateFile, "magic123")

	}
	*/
}
