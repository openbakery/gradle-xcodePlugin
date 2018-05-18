package org.openbakery.signing

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.openbakery.CommandRunner
import org.openbakery.XcodeBuildPluginExtension
import org.openbakery.XcodePlugin
import org.openbakery.codesign.ProvisioningProfileReader
import spock.lang.Specification

class ProvisioningInstallTaskSpecification extends Specification {

	@Rule
	final TemporaryFolder tmpDirectory = new TemporaryFolder()

	File testMobileProvision1
	File testMobileProvision2
	Project project
	ProvisioningInstallTask subject
	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {
		project = ProjectBuilder.builder().withProjectDir(tmpDirectory.root).build()
		project.apply plugin: XcodePlugin

		subject = project.tasks.findByName(ProvisioningInstallTask.TASK_NAME) as ProvisioningInstallTask

		testMobileProvision1 = new File("../libtest/src/main/Resource/test.mobileprovision")
		testMobileProvision2 = new File("src/test/Resource/test1.mobileprovision")

		assert subject != null
		assert testMobileProvision1.exists()
		assert testMobileProvision2.exists()
	}

	private String formatProvisionFileName(File file) {
		assert file.exists()

		ProvisioningProfileReader provisioningProfileIdReader = new ProvisioningProfileReader(file, commandRunner)
		String uuid = provisioningProfileIdReader.getUUID()
		String name = "gradle-" + uuid + ".mobileprovision"

		return name
	}

	def "Should process without error a single provisioning file"() {
		setup:
		project.xcodebuild.signing.mobileProvisionURI = testMobileProvision2.toURI().toString()

		String name2 = formatProvisionFileName(testMobileProvision2)

		File downloadedFile2 = new File(tmpDirectory.root, "build/provision/" + name2)
		File libraryFile2 = new File(ProvisioningInstallTask.PROVISIONING_DIR, name2)

		when:
		subject.download()

		then:
		noExceptionThrown()

		and:
		downloadedFile2.text == testMobileProvision2.text
		downloadedFile2.exists()

		libraryFile2.text == testMobileProvision2.text
		libraryFile2.exists()
	}

	def "Should process without error a change of build folder"() {
		setup:
		String alternateBuildFolderName = "alternativeBuildFolder"
		project.buildDir = tmpDirectory.newFolder(alternateBuildFolderName)
		project.xcodebuild.signing.mobileProvisionURI = testMobileProvision2.toURI().toString()

		String name2 = formatProvisionFileName(testMobileProvision2)

		File downloadedFile2 = new File(tmpDirectory.root, "${alternateBuildFolderName}/provision/" + name2)
		File libraryFile2 = new File(ProvisioningInstallTask.PROVISIONING_DIR, name2)

		when:
		subject.download()

		then:
		noExceptionThrown()

		and:
		downloadedFile2.text == testMobileProvision2.text
		downloadedFile2.exists()

		libraryFile2.text == testMobileProvision2.text
		libraryFile2.exists()
	}

	def "Should process without error multiple provisioning files by using the deprecated `setMobileProvisionURI` property"() {

		setup:
		project.extensions
				.findByType(XcodeBuildPluginExtension)
				.signing
				.setMobileProvisionURI(testMobileProvision1.toURI().toString(),
				testMobileProvision2.toURI().toString())

		String name1 = formatProvisionFileName(testMobileProvision1)
		String name2 = formatProvisionFileName(testMobileProvision2)

		File downloadedFile1 = new File(tmpDirectory.root, "build/provision/" + name1)
		File libraryFile1 = new File(ProvisioningInstallTask.PROVISIONING_DIR, name1)

		File downloadedFile2 = new File(tmpDirectory.root, "build/provision/" + name2)
		File libraryFile2 = new File(ProvisioningInstallTask.PROVISIONING_DIR, name2)

		when:
		subject.download()

		then:
		noExceptionThrown()

		and:
		downloadedFile1.text == testMobileProvision1.text
		downloadedFile1.exists()

		libraryFile1.text == testMobileProvision1.text
		libraryFile1.exists()

		and:
		downloadedFile2.text == testMobileProvision2.text
		downloadedFile2.exists()

		libraryFile2.text == testMobileProvision2.text
		libraryFile2.exists()
	}

	def "Should process without error multiple provisioning files by using directly the `mobileProvisionList` property"() {

		setup:
		project.extensions
				.findByType(XcodeBuildPluginExtension)
				.signing
				.mobileProvisionList.set([testMobileProvision1.toURI().toString(),
										  testMobileProvision2.toURI().toString()])

		String name1 = formatProvisionFileName(testMobileProvision1)
		String name2 = formatProvisionFileName(testMobileProvision2)

		File downloadedFile1 = new File(tmpDirectory.root, "build/provision/" + name1)
		File libraryFile1 = new File(ProvisioningInstallTask.PROVISIONING_DIR, name1)

		File downloadedFile2 = new File(tmpDirectory.root, "build/provision/" + name2)
		File libraryFile2 = new File(ProvisioningInstallTask.PROVISIONING_DIR, name2)

		when:
		subject.download()

		then:
		noExceptionThrown()

		and:
		downloadedFile1.text == testMobileProvision1.text
		downloadedFile1.exists()

		libraryFile1.text == testMobileProvision1.text
		libraryFile1.exists()

		and:
		downloadedFile2.text == testMobileProvision2.text
		downloadedFile2.exists()

		libraryFile2.text == testMobileProvision2.text
		libraryFile2.exists()
	}
}
