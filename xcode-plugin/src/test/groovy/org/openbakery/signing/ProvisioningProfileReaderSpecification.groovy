package org.openbakery.signing

import org.apache.commons.configuration2.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.codesign.ProvisioningProfileType
import org.openbakery.configuration.ConfigurationFromMap
import org.openbakery.configuration.ConfigurationFromPlist
import org.openbakery.xcode.Type
import org.openbakery.XcodePlugin
import org.openbakery.packaging.PackageTask
import org.openbakery.util.PlistHelper
import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is


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
		project.xcodebuild.type = Type.macOS
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"

		packageTask = project.getTasks().getByPath(XcodePlugin.PACKAGE_TASK_NAME)


		buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration)
		buildOutputDirectory.mkdirs()

		appDirectory = new File(buildOutputDirectory, "Example.app")
		appDirectory.mkdirs()

		File infoPlist = new File("../example/OSX/ExampleOSX/ExampleOSX/Info.plist")
		FileUtils.copyFile(infoPlist, new File(appDirectory, "" + "Contents/Info.plist"))

		plistHelper = new PlistHelper(new CommandRunner())
	}

	def cleanup() {
		FileUtils.deleteDirectory(projectDir)
	}

	def "read UUID from file"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(new File("../libtest/src/main/Resource/test.mobileprovision"), new CommandRunner())

		then:
		reader.getUUID() == "FFFFFFFF-AAAA-BBBB-CCCC-DDDDEEEEFFFF"
	}

	def "read application identifier prefix"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(new File("../libtest/src/main/Resource/test.mobileprovision"), new CommandRunner())
		then:
		reader.getApplicationIdentifierPrefix().equals("AAAAAAAAAAA")
	}


	def "read application identifier"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(new File("../libtest/src/main/Resource/test.mobileprovision"), new CommandRunner())
		then:
		reader.getApplicationIdentifier() == "org.openbakery.test.Example"
	}


	def "profile has expired" () {
		when:
		new ProvisioningProfileReader(new File("src/test/Resource/expired.mobileprovision"), new CommandRunner())

		then:
		thrown(IllegalArgumentException.class)
	}

	def "read Mac Provisioning Profile"() {

		given:
		File wildcardMacProfile = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")

		when:
		ProvisioningProfileReader provisioningProfileReader = new ProvisioningProfileReaderIgnoreExpired(wildcardMacProfile, new CommandRunner())

		def applicationIdentifier = provisioningProfileReader.getApplicationIdentifier()

		then:

		assertThat(applicationIdentifier, is(equalTo("*")))

	}

	def "extract Entitlements has nothing to extract"() {
		File provisioningProfile = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")
		ProvisioningProfileReader reader = new ProvisioningProfileReaderIgnoreExpired(provisioningProfile, commandRunner, new PlistHelper(new CommandRunner()))

		commandRunner.runWithResult([
								"/usr/libexec/PlistBuddy",
								"-x",
								provisioningProfile.absolutePath,
								"-c",
								"Print Entitlements"]) >> null

		File entitlementsFile = new File(projectDir, "entitlements.plist")

		expect:
		// no exception should be thrown!
		reader.extractEntitlements(entitlementsFile, "org.openbakery.test.Example", null, null)


	}

	def "extract Entitlements"() {
		given:
		String expectedContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
				"<plist version=\"1.0\">\n" +
				"<dict>\n" +
				"\t<key>com.apple.application-identifier</key>\n" +
				"\t<string>Z7L2YCUH45.org.openbakery.test.Example</string>\n" +
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
				"\t<string>Z7L2YCUH45.org.openbakery.test.Example</string>\n" +
				"</dict>\n" +
				"</plist>\n"

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReaderIgnoreExpired(new File("src/test/Resource/test-wildcard-mac-development.provisionprofile"), new CommandRunner())


		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.test.Example", null, null)

		then:
		entitlementsFile.exists()
		entitlementsFile.text == expectedContents
	}

	def "extract Entitlements 2"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReaderIgnoreExpired(new File("src/test/Resource/test-wildcard-mac-development.provisionprofile"), new CommandRunner())
		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.test.Example", null, null)

		XMLPropertyListConfiguration entitlements = new XMLPropertyListConfiguration()
		entitlements.read(new FileReader(entitlementsFile))

		then:
		entitlementsFile.exists()
		entitlements.getString("com..apple..application-identifier") == "Z7L2YCUH45.org.openbakery.test.Example"
	}


	def "extract Entitlements with keychain access group"() {
		given:
		String expectedContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
				"<plist version=\"1.0\">\n" +
				"<dict>\n" +
				"\t<key>com.apple.application-identifier</key>\n" +
				"\t<string>Z7L2YCUH45.org.openbakery.test.Example</string>\n" +
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
				"\t<string>Z7L2YCUH45.org.openbakery.test.Example</string>\n" +
				"\t<key>keychain-access-groups</key>\n" +
				"\t<array>\n" +
				"\t\t<string>Z7L2YCUH45.org.openbakery.test.Example</string>\n" +
				"\t\t<string>Z7L2YCUH45.org.openbakery.Test</string>\n" +
				"\t\t<string>AAAAAAAAAA.com.example.Test</string>\n" +
				"\t</array>\n" +
				"</dict>\n" +
				"</plist>\n"

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReaderIgnoreExpired(new File("src/test/Resource/test-wildcard-mac-development.provisionprofile"), new CommandRunner())

		def keychainAccessGroups = [
				"Z7L2YCUH45.org.openbakery.test.Example",
				"Z7L2YCUH45.org.openbakery.Test",
				"AAAAAAAAAA.com.example.Test",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.test.Example", keychainAccessGroups, null)

		then:
		entitlementsFile.exists()
		entitlementsFile.text == expectedContents
	}

	def "extract Entitlements with keychain access group 2"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReaderIgnoreExpired(new File("src/test/Resource/test-wildcard-mac-development.provisionprofile"), new CommandRunner())

		def keychainAccessGroups = [
				"Z7L2YCUH45.org.openbakery.test.Example",
				"Z7L2YCUH45.org.openbakery.Test",
				"AAAAAAAAAA.com.example.Test",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.test.Example", keychainAccessGroups, null)

		XMLPropertyListConfiguration entitlements = new XMLPropertyListConfiguration()
		entitlements.read(new FileReader(entitlementsFile))

		then:
		entitlementsFile.exists()
		entitlements.getList("keychain-access-groups").contains("Z7L2YCUH45.org.openbakery.test.Example")
		entitlements.getList("keychain-access-groups").contains("Z7L2YCUH45.org.openbakery.Test")
		entitlements.getList("keychain-access-groups").contains("AAAAAAAAAA.com.example.Test")
	}


	def "extract Entitlements test application identifier"() {
		given:
		File mobileprovision = new File("src/test/Resource/openbakery.mobileprovision")
		commandRunner.runWithResult(_) >> FileUtils.readFileToString(new File("../libtest/src/main/Resource/entitlements.plist"))

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, commandRunner, new PlistHelper(new CommandRunner()))

		def keychainAccessGroups = [
				ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.test.Example",
				ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.Test",
				"CCCCCCCCCC.com.example.Test",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.test.Example", keychainAccessGroups, null)

		then:
		entitlementsFile.exists()
		entitlementsFile.text.contains("AAAAAAAAAA.org.openbakery.test.Example")
		entitlementsFile.text.contains("AAAAAAAAAA.org.openbakery.Test")
		entitlementsFile.text.contains("CCCCCCCCCC.com.example.Test")
	}


	String getEntitlementWithApplicationIdentifier(String applicationIdentifier, String ubiquityContainerIdentifiers = "ABCDE12345.*" ) {
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
						"    <array><string>" + ubiquityContainerIdentifiers + "</string></array>\n" +
						"</dict>\n" +
						"</plist>"
	}

	def "extract Entitlements with wildcard application identifier that does not match"() {
		given:
		File mobileprovision = new File("src/test/Resource/openbakery.mobileprovision")

		commandRunner.runWithResult(_) >> getEntitlementWithApplicationIdentifier("DDDDDDDDDD.com.mycompany.*")

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, commandRunner)

		def keychainAccessGroups = [
						ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.test.Example",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.test.Example", keychainAccessGroups, null)

		then:
		thrown(IllegalStateException.class)
	}


	def "extract Entitlements with wildcard application identifier"() {
		given:
		File mobileprovision = new File("src/test/Resource/openbakery.mobileprovision")

		commandRunner.runWithResult(_) >> getEntitlementWithApplicationIdentifier("DDDDDDDDDD.org.openbakery.*")

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, commandRunner, new PlistHelper(new CommandRunner()))

		def keychainAccessGroups = [
						ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.test.Example",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.test.Example", keychainAccessGroups, null)

		then:
		entitlementsFile.exists()
		entitlementsFile.text.contains("AAAAAAAAAAA.org.openbakery.test.Example")
		plistHelper.getValueFromPlist(entitlementsFile, "application-identifier").startsWith("AAAAAAAAAAA.org.openbakery.test.Example")

	}



	def "extract Entitlements with wildcard application identifier that does match"() {
		given:
		File mobileprovision = new File("src/test/Resource/openbakery.mobileprovision")

		commandRunner.runWithResult(_) >> getEntitlementWithApplicationIdentifier("AAAAAAAAAAA.org.openbakery.test.Example.*")

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, commandRunner, new PlistHelper(new CommandRunner()))

		def keychainAccessGroups = [
						ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.test.Example",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.test.Example.widget", keychainAccessGroups, null)

		then:
		entitlementsFile.exists()
		entitlementsFile.text.contains("AAAAAAAAAAA.org.openbakery.test.Example.widget")
	}


	def "is ad-hoc profile"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(new File("../libtest/src/main/Resource/test.mobileprovision"), new CommandRunner())

		then:
		reader.isAdHoc() == true
	}


	def "is not ad-hoc profile"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(new File("../libtest/src/main/Resource/Appstore.mobileprovision"), new CommandRunner())

		then:
		reader.isAdHoc() == false
	}

	def "is AppStore profile type"() {
        when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(new File("../libtest/src/main/Resource/Appstore.mobileprovision"), new CommandRunner())

		then:
		reader.profileType == ProvisioningProfileType.AppStore
	}

	def "is enterprise profile type"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(new File("../libtest/src/main/Resource/Enterprise.mobileprovision"), new CommandRunner())

		then:
		reader.profileType == ProvisioningProfileType.Enterprise
	}

	def "is ad-hoc profile type"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(new File("../libtest/src/main/Resource/test.mobileprovision"), new CommandRunner())

		then:
		reader.profileType == ProvisioningProfileType.AdHoc
	}

	def "is development profile type"() {
		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(new File("../libtest/src/main/Resource/Development.mobileprovision"), new CommandRunner())

		then:
		reader.profileType == ProvisioningProfileType.Development
	}

	def "extract Entitlements with wildcard and kvstore should start with team id"() {
		given:
		File mobileprovision = new File("src/test/Resource/openbakery-team.mobileprovision")

		commandRunner.runWithResult(_) >> getEntitlementWithApplicationIdentifier("AAAAAAAAAAA.org.openbakery.test.Example.*")

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, commandRunner, new PlistHelper(new CommandRunner()))

		def keychainAccessGroups = [
						ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.test.Example",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.test.Example.widget", keychainAccessGroups, null)

		then:
		entitlementsFile.exists()
		plistHelper.getValueFromPlist(entitlementsFile, "com.apple.developer.ubiquity-kvstore-identifier").startsWith("XXXXXZZZZZ.")
	}

	def "extract Entitlements with wildcard and container-identifiers should start with team id"() {
		given:
		File mobileprovision = new File("src/test/Resource/openbakery-team.mobileprovision")

		commandRunner.runWithResult(_) >> getEntitlementWithApplicationIdentifier("AAAAAAAAAAA.org.openbakery.test.Example.*")

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, commandRunner, new PlistHelper(new CommandRunner()))

		def keychainAccessGroups = [
						ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.test.Example",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.test.Example.widget", keychainAccessGroups, null)

		then:
		entitlementsFile.exists()
		plistHelper.getValueFromPlist(entitlementsFile, "com.apple.developer.ubiquity-container-identifiers")[0].startsWith("XXXXXZZZZZ.")
	}


	def "extract Entitlements with ubiquity-container-identifiers that has no wildcard should keep current value"() {
		given:
		File mobileprovision = new File("src/test/Resource/openbakery-team.mobileprovision")

		def applicationIdentifier = "AAAAAAAAAAA.org.openbakery.test.Example.*"
		def containerIdentifier = "iCloud.org.openkery.test.Example"
		commandRunner.runWithResult(_) >> getEntitlementWithApplicationIdentifier(applicationIdentifier, containerIdentifier)

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, commandRunner, new PlistHelper(new CommandRunner()))

		def keychainAccessGroups = [
						ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX + "org.openbakery.test.Example",
		]

		File entitlementsFile = new File(projectDir, "entitlements.plist")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.test.Example.widget", keychainAccessGroups, null)

		then:
		entitlementsFile.exists()
		plistHelper.getValueFromPlist(entitlementsFile, "com.apple.developer.ubiquity-container-identifiers")[0].equals(containerIdentifier)
	}


	def "get provisioning profile from plist"() {
		def commandList

		File mobileprovision = new File("src/test/Resource/openbakery-team.mobileprovision")
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, commandRunner, new PlistHelper(new CommandRunner()))

		def expectedProvisioningPlist = new File(System.getProperty("java.io.tmpdir") + "/provision_openbakery-team.plist")

		when:
		reader.getPlistFromProvisioningProfile()

		then:
		1 * commandRunner.run(_) >> { arguments -> commandList = arguments[0] }
		commandList == ["security","cms","-D","-i", mobileprovision.absolutePath, "-o", expectedProvisioningPlist.absolutePath]


	}


	def "provisioning match"() {
		given:
		File appMobileprovision = new File("../libtest/src/main/Resource/test.mobileprovision")
		File widgetMobileprovision = new File("src/test/Resource/test1.mobileprovision")
		File wildcardMobileprovision = new File("src/test/Resource/test-wildcard.mobileprovision")

		def list = [
						appMobileprovision,
						widgetMobileprovision,
						wildcardMobileprovision
		]

		expect:
		ProvisioningProfileReader.getReaderForIdentifier(identifier, list, commandRunner, plistHelper).provisioningProfile == filename

		where:
		identifier                          | filename
		"org.openbakery.test.Example"       | new File("../libtest/src/main/Resource/test.mobileprovision")
		"org.openbakery.test.ExampleWidget" | new File("src/test/Resource/test1.mobileprovision")
		"org.openbakery.Test"               | new File("src/test/Resource/test-wildcard.mobileprovision")
		"org.Test"                          | new File("src/test/Resource/test-wildcard.mobileprovision")

	}


	def "provisioning Match more"() {
		given:
		File appMobileprovision = new File("src/test/Resource/openbakery.mobileprovision")
		File wildcardMobileprovision = new File("src/test/Resource/openbakery-wildcard.mobileprovision")

		def list = [
						appMobileprovision,
						wildcardMobileprovision
		]

		expect:
		ProvisioningProfileReader.getReaderForIdentifier(identifier, list, commandRunner, plistHelper).provisioningProfile == filename

		where:
		identifier                              | filename
		"org.openbakery.gxp.Example"           | new File("src/test/Resource/openbakery.mobileprovision")
		"org.openbakery.gxp.Example.widget"    | new File("src/test/Resource/openbakery-wildcard.mobileprovision")
		"org.openbakery.gxp.Example.extension" | new File("src/test/Resource/openbakery-wildcard.mobileprovision")

	}


	def "extract Entitlements and merge Example.entitlements"() {
		given:
		File mobileprovision = new File("src/test/Resource/openbakery.mobileprovision")
		commandRunner.runWithResult(_) >> FileUtils.readFileToString(new File("../libtest/src/main/Resource/entitlements.plist"))

		when:
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, commandRunner, new PlistHelper(new CommandRunner()))


		File entitlementsFile = new File(projectDir, "entitlements.plist")
		File xcent = new File("src/test/Resource/archived-expanded-entitlements.xcent")
		reader.extractEntitlements(entitlementsFile, "org.openbakery.test.Example", null, new ConfigurationFromPlist(xcent))

		XMLPropertyListConfiguration entitlements = new XMLPropertyListConfiguration()
		entitlements.read(new FileReader(entitlementsFile))

		then:
		entitlementsFile.exists()
		entitlements.getList("com..apple..developer..associated-domains").contains("webcredentials:example.com")
		entitlements.getString("com..apple..developer..default-data-protection") == "NSFileProtectionComplete"
		entitlements.getBoolean("com..apple..developer..siri") == true

	}


	def setupForEntitlementTest(Map<String, Object> data) {
		File mobileprovision = new File("src/test/Resource/openbakery.mobileprovision")

		File sourceFile = new File("../libtest/src/main/Resource/entitlementsForReplacementTest.plist")
		File destinationFile = new File(projectDir, "testEntitlements.plist")
		FileUtils.copyFile(sourceFile, destinationFile)

		commandRunner.runWithResult(_) >> FileUtils.readFileToString(new File("../libtest/src/main/Resource/entitlementsForReplacementTest.plist"))
		ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileprovision, commandRunner, new PlistHelper(new CommandRunner()))
		//File entitlementsFile = new File(projectDir, "entitlements.plist")

		reader.extractEntitlements(destinationFile, "org.openbakery.test.Example", null, new ConfigurationFromMap(data))
		return destinationFile
	}


	def "extract Entitlements and replace com.apple.developer.icloud-container-identifiers"() {
		given:
		Map<String, Object> data = ["com.apple.developer.icloud-container-identifiers": ["iCloud.com.example.Test"]]
		File entitlementsFile = setupForEntitlementTest(data)

		when:
		XMLPropertyListConfiguration entitlements = new XMLPropertyListConfiguration()
		entitlements.read(new FileReader(entitlementsFile))
		then:
		entitlementsFile.exists()
		entitlements.getList("com..apple..developer..icloud-container-identifiers").contains("iCloud.com.example.Test")
	}


	def "extract Entitlements and replace ubiquity-container-identifiers"() {
		given:
		Map<String, Object> data = ["com.apple.developer.ubiquity-container-identifiers": ["com.example.Test"]]
		File entitlementsFile = setupForEntitlementTest(data)

		when:
		XMLPropertyListConfiguration entitlements = new XMLPropertyListConfiguration()
		entitlements.read(new FileReader(entitlementsFile))

		then:
		entitlementsFile.exists()
		entitlements.getList("com..apple..developer..ubiquity-container-identifiers").contains("com.example.Test")
	}

	def "extract Entitlements and replace default-data-protection"() {
		given:
		Map<String, Object> data = ["com.apple.developer.default-data-protection": "NSFileProtectionComplete"]
		File entitlementsFile = setupForEntitlementTest(data)

		when:
		XMLPropertyListConfiguration entitlements = new XMLPropertyListConfiguration()
		entitlements.read(new FileReader(entitlementsFile))

		then:
		entitlementsFile.exists()
		entitlements.getString("com..apple..developer..default-data-protection") == "NSFileProtectionComplete"
	}


	def "extract Entitlements and replace icloud-services"() {
		given:
		Map<String, Object> data = ["com.apple.developer.icloud-services": "com.example.test"]
		File entitlementsFile = setupForEntitlementTest(data)

		when:
		XMLPropertyListConfiguration entitlements = new XMLPropertyListConfiguration()
		entitlements.read(new FileReader(entitlementsFile))

		then:
		entitlementsFile.exists()
		entitlements.getString("com..apple..developer..icloud-services") == "com.example.test"
	}

	def "extract Entitlements and delete key when signing.entitlements contains null value"() {
		given:
		Map<String, Object> data = ["com.apple.developer.icloud-services": null]
		File entitlementsFile = setupForEntitlementTest(data)

		when:
		XMLPropertyListConfiguration entitlements = new XMLPropertyListConfiguration()
		entitlements.read(new FileReader(entitlementsFile))

		then:
		entitlementsFile.exists()
		!entitlements.containsKey("com..apple..developer..icloud-services")
	}

	def "extract Entitlements key os only added once replace icloud-services"() {
		given:
		Map<String, Object> data = [
				"com.apple.developer.icloud-services": "com.example.test"
		]
		File entitlementsFile = setupForEntitlementTest(data)

		when:
		XMLPropertyListConfiguration entitlements = new XMLPropertyListConfiguration()
		entitlements.read(new FileReader(entitlementsFile))

		then:
		entitlementsFile.exists()
		entitlements.containsKey("com..apple..developer..icloud-services")
		entitlements.getString("com..apple..developer..icloud-services") == "com.example.test"
	}


	def "extract Entitlements set keychain access group with configuration key and replace AppIdentiferPrefix variable"() {
		given:
		Map<String, Object> data = [
				"keychain-access-groups": [ "\$(AppIdentifierPrefix)com.example.Test" ]
		]
		File entitlementsFile = setupForEntitlementTest(data)

		when:
		XMLPropertyListConfiguration entitlements = new XMLPropertyListConfiguration()
		entitlements.read(new FileReader(entitlementsFile))

		then:
		entitlementsFile.exists()
		entitlements.containsKey("keychain-access-groups")
		entitlements.getList("keychain-access-groups").contains("AAAAAAAAAAA.com.example.Test")

	}


}

