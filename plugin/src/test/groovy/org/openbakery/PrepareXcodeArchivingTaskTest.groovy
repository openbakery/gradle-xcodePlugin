package org.openbakery

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.extension.Signing
import org.openbakery.util.PlistHelper
import spock.lang.Specification

import static org.openbakery.PrepareXcodeArchivingTask.*

class PrepareXcodeArchivingTaskTest extends Specification {

	PrepareXcodeArchivingTask subject
	Signing signing
	Project project
	File outputFile
	File entitlementsFile

	CommandRunner commandRunner = Mock(CommandRunner)
	ProvisioningProfileReader provisioningProfileReader = Mock(ProvisioningProfileReader)
	PlistHelper plistHelper = Mock(PlistHelper)

	private static final String FAKE_TEAM_ID = "Team Identifier"
	private static final String FAKE_BUNDLE_IDENTIFIER = "co.test.test"
	private static final String FAKE_FRIENDLY_NAME = "Fake Certificate FriendlyName (12345)"
	private static final String FAKE_UUID = "FAKE_UUID"
	private static final String FAKE_PROV_NAME = "Provisioning Name"

	@Rule
	final TemporaryFolder tmpDirectory = new TemporaryFolder()

	def setup() {
		this.project = ProjectBuilder.builder().withProjectDir(tmpDirectory.root).build()
		this.project.apply plugin: XcodePlugin

		this.entitlementsFile = tmpDirectory.newFile("test.entitlements")
		this.outputFile = tmpDirectory.newFile("test.xcconfig")
		this.signing = project.extensions.findByType(Signing)

		configureSubject()

		provisioningProfileReader.getTeamIdentifierPrefix() >> FAKE_TEAM_ID
		provisioningProfileReader.getUUID() >> FAKE_UUID
		provisioningProfileReader.getName() >> FAKE_PROV_NAME
	}

	def configureSubject() {
		subject = project.tasks.findByName(NAME) as PrepareXcodeArchivingTask
		subject.certificateFriendlyName.set(FAKE_FRIENDLY_NAME)
		subject.commandRunnerProperty.set(commandRunner)
		subject.configurationBundleIdentifier.set(FAKE_BUNDLE_IDENTIFIER)
		subject.outputFile.set(outputFile)
		subject.plistHelperProperty.set(plistHelper)
		subject.provisioningReader.set(provisioningProfileReader)
	}

	def "The generation should be executed without exception"() {
		when:
		subject.generate()

		then:
		noExceptionThrown()

		and: "The generate file content should be valid"
		String text = outputFile.text
		text.contains("${KEY_CODE_SIGN_IDENTITY} = ${FAKE_FRIENDLY_NAME}")
		text.contains("${KEY_DEVELOPMENT_TEAM} = ${FAKE_TEAM_ID}")
		text.contains("${KEY_PROVISIONING_PROFILE_ID} = ${FAKE_UUID}")
		text.contains("${KEY_PROVISIONING_PROFILE_SPEC} = ${FAKE_PROV_NAME}")

		and: "And no entitlements information should be present"
		!text.contains("${KEY_CODE_SIGN_ENTITLEMENTS} = ")
	}

	def "The generate file should refer the entitlements file is present"() {
		when:
		subject.entitlementsFile.set(entitlementsFile)
		subject.generate()

		then:
		noExceptionThrown()

		and:
		String text = outputFile.text
		text.contains("${KEY_CODE_SIGN_IDENTITY} = ${FAKE_FRIENDLY_NAME}")
		text.contains("${KEY_DEVELOPMENT_TEAM} = ${FAKE_TEAM_ID}")
		text.contains("${KEY_PROVISIONING_PROFILE_ID} = ${FAKE_UUID}")
		text.contains("${KEY_PROVISIONING_PROFILE_SPEC} = ${FAKE_PROV_NAME}")

		and: "No entitlements information should be present"
		text.contains("${KEY_CODE_SIGN_ENTITLEMENTS} = ${entitlementsFile.absolutePath}")
	}
}
