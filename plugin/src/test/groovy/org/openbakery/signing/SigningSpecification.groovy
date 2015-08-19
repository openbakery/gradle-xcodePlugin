package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.contains

/**
 * Created by rene on 19.08.15.
 */
class SigningSpecification extends Specification {

	Signing signing
	Signing parentSigning
	Project project
	File projectDir


	def commandRunner = Mock(CommandRunner)


	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		parentSigning = new Signing(project)
		parentSigning.commandRunner = commandRunner
		signing = new Signing(project, parentSigning)
		signing.commandRunner = commandRunner


	}


	def "test identity"() {
		given:
		signing.identity = "Me!"

		expect:
		signing.identity.equals("Me!")
	}


	def "single identity fetch"() {

		when:
		signing.getIdentity()

		then:
		1 * commandRunner.runWithResult(*_) >> { arguments ->
			assertThat(arguments[0], contains(
							'security',
							"find-identity",
							"-v",
							"-p",
							"codesigning",
							signing.keychainPathInternal.absolutePath
			))
			return ""
		}
	}

	def "single identity"() {

		setup:
		commandRunner.runWithResult(_) >> FileUtils.readFileToString(new File("src/test/Resource/security-find-identity-single.txt"))

		expect:
		signing.identity.equals("Developer ID Application: MyCompany")
	}


	def "multiple identities"() {
		setup:
		commandRunner.runWithResult(_) >> FileUtils.readFileToString(new File("src/test/Resource/security-find-identity-multiple.txt"))

		expect:
		signing.identity == null
	}


	def "merge identity"() {
		setup:
		parentSigning.identity = "Me"

		expect:
		signing.identity.equals("Me")
	}


	def "merge certificateURI"() {
		setup:
		parentSigning.certificateURI = "http://localhost"

		expect:
		signing.certificateURI.equals("http://localhost")
	}

	def "merge certificatePassword"() {
		setup:
		parentSigning.certificatePassword = "password"

		expect:
		signing.certificatePassword.equals("password")
	}


	def "merge mobileProvisionURI"() {
		setup:
		parentSigning.mobileProvisionURI = "http://localhost"

		expect:
		signing.mobileProvisionURI.contains("http://localhost")

	}

	def "merge keychainPassword"() {
		setup:
		parentSigning.keychainPassword = "keychainPassword"

		expect:
		signing.keychainPassword.equals("keychainPassword")
	}

	def "default keychainPassword"() {
		expect:
		signing.keychainPassword.equals("This_is_the_default_keychain_password")
	}

	def "merge timeout"() {
		setup:
		parentSigning.timeout = 500

		expect:
		signing.timeout.equals(500)

	}


	def "merge keychain"() {
		setup:
		parentSigning.keychain = "my.keychain"

		expect:
		signing.keychain.absolutePath.endsWith("my.keychain")
	}

}
