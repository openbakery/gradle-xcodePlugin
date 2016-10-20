package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.xcode.Type
import org.openbakery.XcodePlugin
import org.openbakery.packaging.PackageTask
import org.openbakery.util.PlistHelper
import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is


/**
 * Created by Stefan Gugarel on 05/02/15.
 */
class ProvisioningProfileReaderSpecification extends Specification {

	Project project

	PackageTask packageTask;

	File projectDir
	File buildOutputDirectory
	File appDirectory
	CommandRunner commandRunner = Mock(CommandRunner)
	PlistHelper plistHelper

	def setup() {
		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.infoPlist = 'Info.plist'
		project.xcodebuild.productName = 'Example'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.type = Type.OSX
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"

		packageTask = project.getTasks().getByPath(XcodePlugin.PACKAGE_TASK_NAME)


		buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration)
		buildOutputDirectory.mkdirs()

		appDirectory = new File(buildOutputDirectory, "Example.app")
		appDirectory.mkdirs()

		File infoPlist = new File("../example/OSX/ExampleOSX/ExampleOSX/Info.plist")
		FileUtils.copyFile(infoPlist, new File(appDirectory, "" + "Contents/Info.plist"))

		plistHelper = new PlistHelper(project, new CommandRunner())
	}

	def cleanup() {
		FileUtils.deleteDirectory(projectDir)
	}

	def "read UUID from file"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader("src/test/Resource/test.mobileprovision", project, new CommandRunner())

		then:
		reader.getUUID() == "FFFFFFFF-AAAA-BBBB-CCCC-DDDDEEEEFFFF"
	}

	def "read application identifier prefix"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader("src/test/Resource/test.mobileprovision", project, new CommandRunner())
		then:
		reader.getApplicationIdentifierPrefix().equals("AAAAAAAAAAA")
	}


	def "read application identifier"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader("src/test/Resource/test.mobileprovision", project, new CommandRunner())
		then:
		reader.getApplicationIdentifier() == "org.openbakery.Example"
	}


	def "profile has expired" () {
		when:
		new ProvisioningProfileReader("src/test/Resource/expired.mobileprovision", project, new CommandRunner())

		then:
		thrown(IllegalArgumentException.class)
	}

	def "read Mac Provisioning Profile"() {

		given:
		File wildcardMacProfile = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")

		when:
		ProvisioningProfileReader provisioningProfileReader = new ProvisioningProfileReaderIgnoreExpired(wildcardMacProfile, project, new CommandRunner())

		def applicationIdentifier = provisioningProfileReader.getApplicationIdentifier()

		then:

		assertThat(applicationIdentifier, is(equalTo("*")))

	}

	def "extract Entitlements"() {
		given:
		String expectedContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
				"<plist version=\"1.0\">\n" +
				"<dict>\n" +
				"\t<key>com.apple.application-identifier</key>\n" +
				"\t<string>Z7L2YCUH45.org.openbakery.Example</string>\n" +
				"\t<key>com.apple.developer.aps-environment</key>\n" +
				"\t<string>development</string>\n" +
				"\t<key>com.apple.developer.icloud-container-development-container-identifiers</key>\n" +
				"\t<array/>\n" +
				"\t<key>com.apple.developer.icloud-container-environment</key>\n" +
				"\t<array>\n" +
				"\t\t<string>Development</string>\n" +
				"\t\t<string>Production</string>\n" +
				"\t</array>\n" +
				"\t<key>com.apple.developer.icloud-container-identifiers</key>\n" +
				"\t<array/>\n" +
				"\t<key>com.apple.developer.icloud-services</key>\n" +
				"\t<string>*</string>\n" +
				"\t<key>com.apple.developer.team-identifier</key>\n" +
				"\t<string>Z7L2YCUH45</string>\n" +
				"\t<key>com.apple.developer.ubiquity-container-identifiers</key>\n" +
				"\t<array/>\n" +
				"\t<key>com.apple.developer.ubiquity-kvstore-identifier</key>\n" +
				"\t<string>Z7L2YCUH45.org.openbakery.Example</string>\n" +
				"</dict>\n" +
				"</plist>\n"

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReaderIgnoreExpired("src/test/Resource/test-wildcard-mac-development.provisionprofile", project, new CommandRunner())


		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.Example", null)

		then:
		entitlementsFile.exists()
		entitlementsFile.text == expectedContents
	}


	def "extract Entitlements with keychain access group"() {
		given:
		String expectedContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
				"<plist version=\"1.0\">\n" +
				"<dict>\n" +
				"\t<key>com.apple.application-identifier</key>\n" +
				"\t<string>Z7L2YCUH45.org.openbakery.Example</string>\n" +
				"\t<key>com.apple.developer.aps-environment</key>\n" +
				"\t<string>development</string>\n" +
				"\t<key>com.apple.developer.icloud-container-development-container-identifiers</key>\n" +
				"\t<array/>\n" +
				"\t<key>com.apple.developer.icloud-container-environment</key>\n" +
				"\t<array>\n" +
				"\t\t<string>Development</string>\n" +
				"\t\t<string>Production</string>\n" +
				"\t</array>\n" +
				"\t<key>com.apple.developer.icloud-container-identifiers</key>\n" +
				"\t<array/>\n" +
				"\t<key>com.apple.developer.icloud-services</key>\n" +
				"\t<string>*</string>\n" +
				"\t<key>com.apple.developer.team-identifier</key>\n" +
				"\t<string>Z7L2YCUH45</string>\n" +
				"\t<key>com.apple.developer.ubiquity-container-identifiers</key>\n" +
				"\t<array/>\n" +
				"\t<key>com.apple.developer.ubiquity-kvstore-identifier</key>\n" +
				"\t<string>Z7L2YCUH45.org.openbakery.Example</string>\n" +
				"\t<key>keychain-access-groups</key>\n" +
				"\t<array>\n" +
				"\t\t<string>Z7L2YCUH45.org.openbakery.Example</string>\n" +
				"\t\t<string>Z7L2YCUH45.org.openbakery.Test</string>\n" +
				"\t\t<string>AAAAAAAAAA.com.example.Test</string>\n" +
				"\t</array>\n" +
				"</dict>\n" +
				"</plist>\n"

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReaderIgnoreExpired("src/test/Resource/test-wildcard-mac-development.provisionprofile", project, new CommandRunner())

		def keychainAccessGroups = [
				"Z7L2YCUH45.org.openbakery.Example",
				"Z7L2YCUH45.org.openbakery.Test",
				"AAAAAAAAAA.com.example.Test",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.Example", keychainAccessGroups)

		then:
		entitlementsFile.exists()
		entitlementsFile.text == expectedContents
	}


	def "extract Entitlements test application identifier"() {
		given:
		String mobileprovision = "src/test/Resource/openbakery.mobileprovision"
		commandRunner.runWithResult(_) >> FileUtils.readFileToString(new File("src/test/Resource/entitlements.plist"))

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, project, commandRunner, new PlistHelper(project, new CommandRunner()))

		def keychainAccessGroups = [
				ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.Example",
				ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.Test",
				"CCCCCCCCCC.com.example.Test",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.Example", keychainAccessGroups)

		then:
		entitlementsFile.exists()
		entitlementsFile.text.contains("AAAAAAAAAA.org.openbakery.Example")
		entitlementsFile.text.contains("AAAAAAAAAA.org.openbakery.Test")
		entitlementsFile.text.contains("CCCCCCCCCC.com.example.Test")
	}


	String getEntitlementWithApplicationIdentifier(String applicationIdentifier) {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
						"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
						"<plist version=\"1.0\">\n" +
						"<dict>\n" +
						"    <key>keychain-access-groups</key>\n" +
						"    <array>\n" +
						"        <string>AAAAAAAAAA.*</string>\n" +
						"    </array>\n" +
						"    <key>get-task-allow</key>\n" +
						"    <false/>\n" +
						"    <key>application-identifier</key>\n" +
						"    <string>" + applicationIdentifier + "</string>\n" +
						"    <key>com.apple.developer.team-identifier</key>\n" +
						"    <string>AAAAAAAAAA</string>\n" +
						"    <key>com.apple.developer.ubiquity-kvstore-identifier</key>\n" +
						"    <string>ABCDE12345.*</string>\n" +
						"    <key>com.apple.developer.ubiquity-container-identifiers</key>\n" +
						"    <string>ABCDE12345.*</string>\n" +
						"</dict>\n" +
						"</plist>"
	}

	def "extract Entitlements with wildcard application identifier that does not match"() {
		given:
		String mobileprovision = "src/test/Resource/openbakery.mobileprovision"

		commandRunner.runWithResult(_) >> getEntitlementWithApplicationIdentifier("DDDDDDDDDD.com.mycompany.*")

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, project, commandRunner, new PlistHelper(project, new CommandRunner()))

		def keychainAccessGroups = [
						ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.Example",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.Example", keychainAccessGroups)

		then:
		thrown(IllegalStateException.class)
	}


	def "extract Entitlements with wildcard application identifier"() {
		given:
		String mobileprovision = "src/test/Resource/openbakery.mobileprovision"

		commandRunner.runWithResult(_) >> getEntitlementWithApplicationIdentifier("DDDDDDDDDD.org.openbakery.*")

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, project, commandRunner, new PlistHelper(project, new CommandRunner()))

		def keychainAccessGroups = [
						ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.Example",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.Example", keychainAccessGroups)

		then:
		entitlementsFile.exists()
		entitlementsFile.text.contains("AAAAAAAAAAA.org.openbakery.Example")
		plistHelper.getValueFromPlist(entitlementsFile, "application-identifier").startsWith("AAAAAAAAAAA.org.openbakery.Example")

	}



	def "extract Entitlements with wildcard application identifier that does match"() {
		given:
		String mobileprovision = "src/test/Resource/openbakery.mobileprovision"

		commandRunner.runWithResult(_) >> getEntitlementWithApplicationIdentifier("AAAAAAAAAAA.org.openbakery.Example.*")

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, project, commandRunner, new PlistHelper(project, new CommandRunner()))

		def keychainAccessGroups = [
						ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.Example",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.Example.widget", keychainAccessGroups)

		then:
		entitlementsFile.exists()
		entitlementsFile.text.contains("AAAAAAAAAAA.org.openbakery.Example.widget")
	}


	def "is ad-hoc profile"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader("src/test/Resource/test.mobileprovision", project, new CommandRunner())

		then:
		reader.isAdHoc() == true
	}


	def "is not ad-hoc profile"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader("src/test/Resource/Appstore.mobileprovision", project, new CommandRunner())

		then:
		reader.isAdHoc() == false
	}


	def "extract Entitlements with wildcard and kvstore should start with team id"() {
		given:
		String mobileprovision = "src/test/Resource/openbakery-team.mobileprovision"

		commandRunner.runWithResult(_) >> getEntitlementWithApplicationIdentifier("AAAAAAAAAAA.org.openbakery.Example.*")

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, project, commandRunner, new PlistHelper(project, new CommandRunner()))

		def keychainAccessGroups = [
						ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.Example",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.Example.widget", keychainAccessGroups)

		then:
		entitlementsFile.exists()
		plistHelper.getValueFromPlist(entitlementsFile, "com.apple.developer.ubiquity-kvstore-identifier").startsWith("XXXXXZZZZZ.")
	}

	def "extract Entitlements with wildcard and container-identifiers should start with team id"() {
		given:
		String mobileprovision = "src/test/Resource/openbakery-team.mobileprovision"

		commandRunner.runWithResult(_) >> getEntitlementWithApplicationIdentifier("AAAAAAAAAAA.org.openbakery.Example.*")

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, project, commandRunner, new PlistHelper(project, new CommandRunner()))

		def keychainAccessGroups = [
						ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.Example",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.Example.widget", keychainAccessGroups)

		then:
		entitlementsFile.exists()
		plistHelper.getValueFromPlist(entitlementsFile, "com.apple.developer.ubiquity-container-identifiers").startsWith("XXXXXZZZZZ.")
	}


	def "get provisioning profile from plist"() {
		def commandList

		File mobileprovision = new File("src/test/Resource/openbakery-team.mobileprovision")
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision.absolutePath, project, commandRunner, new PlistHelper(project, new CommandRunner()))
		def expectedProvisioningPlist = new File(project.buildDir, "tmp/provision_openbakery-team.plist")

		when:
		reader.getPlistFromProvisioningProfile()

		then:
		1 * commandRunner.run(_) >> { arguments -> commandList = arguments[0] }
		commandList == ["security","cms","-D","-i", mobileprovision.absolutePath, "-o", expectedProvisioningPlist.absolutePath]


	}

}
