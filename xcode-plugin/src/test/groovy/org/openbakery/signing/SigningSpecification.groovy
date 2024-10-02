package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.codesign.CodesignParameters
import org.openbakery.configuration.ConfigurationFromMap
import spock.lang.Specification

class SigningSpecification extends Specification {

	Signing signing
	Project project
	File projectDir


	def setup() {
		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		signing = new Signing(project)
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

	def "entitlements data set via closure using xcodebuild"() {

		when:
		project.xcodebuild.signing {
			entitlements 'com.apple.security.application-groups': ['group.com.example.App']
		}

		then:
		project.xcodebuild.signing.entitlements instanceof Map<String, Object>
		project.xcodebuild.signing.entitlements.containsKey("com.apple.security.application-groups")

	}

	def "entitlements data set via closure"() {
		when:
		signing.entitlements('com.apple.security.application-groups': ['group.com.example.App'])

		then:
		signing.entitlements instanceof Map<String, Object>
		signing.entitlements.containsKey("com.apple.security.application-groups")
		signing.entitlements["com.apple.security.application-groups"] == ['group.com.example.App']
	}


	def "entitlements data set via closure converted to Configuration"() {
		when:
		signing.entitlements('com.apple.security.application-groups': ['group.com.example.App'])

		def configuration = new ConfigurationFromMap(signing.entitlements)

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

	def "codesignParameters has mobileProvisionFiles"() {
		when:

		File first = new File(projectDir, "first")
		FileUtils.write(first, "first")
		File second = new File(projectDir, "second")
		FileUtils.write(second, "second")

		signing.addMobileProvisionFile(first)
		signing.addMobileProvisionFile(second)

		then:
		signing.codesignParameters.mobileProvisionFiles instanceof List<File>
		signing.codesignParameters.mobileProvisionFiles == [first, second]
	}

	def "codesignParameters has keychain"() {
		when:
		signing.keychain = new File("my.keychain").absoluteFile
		then:
		signing.codesignParameters.keychain == new File("my.keychain").absoluteFile
	}


	def "codesignParameters has entitlements"() {
		when:
		signing.entitlements = ['key': 'value']
		then:
		signing.codesignParameters.entitlements == ['key': 'value']
	}

	def "codesignParameters has entitlementsFile"() {
		when:
		signing.entitlementsFile = new File("entitlements")
		then:
		signing.codesignParameters.entitlementsFile == new File("entitlements")
	}


	def "codesignParameter entitlementsFile as String"() {
		when:
		signing.entitlementsFile = "entitlements"
		then:
		signing.codesignParameters.entitlementsFile instanceof File
		signing.codesignParameters.entitlementsFile.path.endsWith("entitlements")
	}

	def "codesignParameter entitlementsFile as String full path"() {
		when:
		signing.entitlementsFile = "file:///entitlements"
		then:
		signing.codesignParameters.entitlementsFile instanceof File
		signing.codesignParameters.entitlementsFile.path.endsWith("entitlements")
	}


	def "bundleEntitlements data set via closure using xcodebuild"() {

		when:
		project.xcodebuild.signing {
			bundleEntitlements 'com.example.app': ['com.apple.security.application-groups': ['group.com.example.App']]
		}

		then:
		project.xcodebuild.signing.bundleEntitlements instanceof Map<String, Object>
		project.xcodebuild.signing.bundleEntitlements != null
		project.xcodebuild.signing.bundleEntitlements.containsKey("com.example.app")
		project.xcodebuild.signing.bundleEntitlements["com.example.app"] instanceof Map<String, Object>
		project.xcodebuild.signing.bundleEntitlements["com.example.app"]["com.apple.security.application-groups"] instanceof List<String>
	}

	def "bundleEntitlements data set via closure"() {
		when:
		signing.bundleEntitlements('com.example.app': ['com.apple.security.application-groups': ['group.com.example.App']])

		then:
		signing.bundleEntitlements instanceof Map<String, Object>
		signing.bundleEntitlements != null
		signing.bundleEntitlements.containsKey("com.example.app")
	}


	def "bundleEntitlements data set via closure converted to Configuration"() {
		when:
		signing.bundleEntitlements('com.example.app': ['com.apple.security.application-groups': ['group.com.example.App']])

		def configuration = new ConfigurationFromMap(signing.bundleEntitlements, "com.example.app")

		then:
		configuration.getStringArray("com.apple.security.application-groups") == ['group.com.example.App']

	}
}
