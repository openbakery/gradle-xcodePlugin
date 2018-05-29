package org.openbakery.signing

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import org.openbakery.CommandRunner
import org.openbakery.XcodeBuildPluginExtension
import org.openbakery.XcodePlugin
import org.openbakery.codesign.CodesignParameters
import org.openbakery.configuration.ConfigurationFromMap
import org.openbakery.extension.Signing
import spock.lang.Specification
import spock.lang.Unroll

class SigningSpecification extends Specification {

	Signing signing
	Project project
	File projectDir

	@Rule
	final TemporaryFolder testProjectDir = new TemporaryFolder()

	@Rule
	public ExpectedException exception = ExpectedException.none()

	private Signing signingExtension

	def setup() {
		projectDir = testProjectDir.root

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: XcodePlugin

		signing = new Signing(project, new CommandRunner())

		this.signingExtension = project.extensions
				.findByType(XcodeBuildPluginExtension)
				.signing
	}

	def cleanup() {
		projectDir.delete()
	}

	def "has identity"() {
		when:
		signing.identity = "Me!"
		then:
		signing.identity == "Me!"
	}

	@Unroll
	def "the signing method can be resolved from a valid string value for method: #method"() {
		when:
		signing.setMethod(string)

		then:
		noExceptionThrown()
		signing.signingMethod.get() == method

		where:
		string        | method
		"ad-hoc"      | SigningMethod.AdHoc
		"app-store"   | SigningMethod.AppStore
		"development" | SigningMethod.Dev
		"enterprise"  | SigningMethod.Enterprise
	}

	def "Should throw an expection when trying to parse an invalid signature method"() {
		when:
		signing.setMethod(string)

		then:
		thrown(IllegalArgumentException.class)

		where:
		string     | _
		"ad-hc"    | _
		"app-stre" | _
		null       | _
	}

	def "The XcConfig file path can be configured and is by default buildDir dependant"() {
		when:
		File file = signing.xcConfigFile.asFile.get()

		then:
		noExceptionThrown()
		file.absoluteFile == new File(project.buildDir, "archive/archive.xcconfig")

		when:
		File altBuildDir = testProjectDir.newFolder("build-dir-alt")
		assert project.buildDir != altBuildDir
		project.buildDir = altBuildDir

		and:
		File file2 = signing.xcConfigFile.asFile.get()

		then:
		noExceptionThrown()
		file2.absoluteFile == new File(altBuildDir, "archive/archive.xcconfig")
	}

	def "entitlements data set via closure using xcodebuild"() {

		when:
		assert signingExtension != null
		signingExtension.entitlements 'com.apple.security.application-groups': ['group.com.example.App']

		then:
		signingExtension.entitlementsMap.get() instanceof Map<String, Object>
		signingExtension.entitlementsMap.get().containsKey("com.apple.security.application-groups")
	}

	def "entitlements data set via closure"() {
		when:
		assert signingExtension != null
		signingExtension.entitlements('com.apple.security.application-groups': ['group.com.example.App'])

		then:
		signingExtension.entitlementsMap.get() instanceof Map<String, Object>
		signingExtension.entitlementsMap.get().containsKey("com.apple.security.application-groups")
		signingExtension.entitlementsMap.get()["com.apple.security.application-groups"] == ['group.com.example.App']
	}

	def "entitlements data set via closure converted to Configuration"() {
		when:
		signing.entitlements('com.apple.security.application-groups': ['group.com.example.App'])

		def configuration = new ConfigurationFromMap(signing.entitlementsMap.get())

		then:
		configuration.getStringArray("com.apple.security.application-groups") == ['group.com.example.App']
	}

	def "codesignParameters is not null"() {
		when:
		signing.identity = "Me"

		then:
		signing.codesignParameters instanceof CodesignParameters
	}

	def "codesignParameters has identity"() {
		when:
		signing.identity = "Me"

		then:
		signing.codesignParameters.signingIdentity == "Me"
	}

	def "codesignParameters has keychain"() {
		when:
		signing.keychain = new File("my.keychain").absoluteFile
		then:
		signing.codesignParameters.keychain == new File("my.keychain").absoluteFile
	}


	def "codesignParameters has entitlements"() {
		when:
		signing.entitlements(['key': 'value'])

		then:
		signing.codesignParameters.entitlements == ['key': 'value']
	}

	def "codesignParameters has entitlementsFile"() {
		setup:
		File entitlementsFile = testProjectDir.newFile("test.entitlements")

		when:
		signing.entitlementsFile = entitlementsFile

		then:
		signing.codesignParameters.entitlementsFile == entitlementsFile
	}

	def "When defining the certificate the friendlyName should be updated"() {
		setup:
		File cert = new File("src/test/Resource/fake_distribution.p12")
		assert cert.exists()
		signing.certificate.set(cert.absoluteFile)

		when: "If not password is defined, should trow an exception"
		signing.certificateFriendlyName.get()

		then:
		thrown IllegalStateException.class

		when: "Defining password"
		signing.certificatePassword.set("p4ssword")

		and:
		signing.certificateFriendlyName.get() == "iPhone Distribution: Test Company Name (12345ABCDE)"

		then:
		noExceptionThrown()
	}

	def "codesignParameter entitlementsFile as String"() {
		when:
		signing.entitlementsFile = "entitlements/test.entitlements"

		then:
		noExceptionThrown()
		with(signing.entitlementsFile
				.get()
				.asFile) {
			name == "test.entitlements"
			parent.endsWith("/entitlements")
		}
	}

	def "codesignParameter entitlementsFile as String full path"() {
		when:
		signing.entitlementsFile = "file:///entitlements"

		then:
		noExceptionThrown()
		signing.codesignParameters.entitlementsFile instanceof File
		signing.codesignParameters.entitlementsFile.path.endsWith("entitlements")
	}
}
