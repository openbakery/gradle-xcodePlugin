package org.openbakery.packaging

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.RandomStringUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.xcode.Type
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.XcodePlugin
import org.openbakery.output.StyledTextOutputStub
import org.openbakery.testdouble.PlistHelperStub
import org.openbakery.testdouble.XcodeFake
import org.openbakery.helpers.PlistHelper
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Created by rene on 09.10.15.
 */
class PackageTaskSpecification extends Specification {


	Project project
	PackageTask packageTask;

	CommandRunner commandRunner = Mock(CommandRunner)

	PlistHelperStub plistHelperStub = new PlistHelperStub()

	File projectDir
	File infoPlist
	File payloadAppDirectory
	File archiveDirectory
	File keychain

	void setup() {

		String tmpName =  "gradle-xcodebuild-" + RandomStringUtils.randomAlphanumeric(5)
		projectDir = new File(System.getProperty("java.io.tmpdir"), tmpName)

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productName = 'Example'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.type = Type.iOS
		project.xcodebuild.simulator = false
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"


		packageTask = project.getTasks().getByPath(XcodePlugin.PACKAGE_TASK_NAME)
		packageTask.plistHelper = plistHelperStub

		packageTask.commandRunner = commandRunner

		archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Example.xcarchive")

		File payloadDirectory = new File(packageTask.outputPath, "Payload")
		payloadAppDirectory = new File(payloadDirectory, "Example.app");

		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"

		keychain = new File(projectDir, "gradle.keychain")
		FileUtils.writeStringToFile(keychain, "dummy");
		project.xcodebuild.signing.keychain = keychain.absolutePath
		project.xcodebuild.target = "Example"

		File entitlementsFile = new File(payloadAppDirectory, "archived-expanded-entitlements.xcent")

		PlistHelper helper = new PlistHelper(project.projectDir, new CommandRunner())
		helper.createForPlist(entitlementsFile)
		helper.addValueForPlist(entitlementsFile, "application-identifier", "AAAAAAAAAA.org.openbakery.Example")
		helper.addValueForPlist(entitlementsFile, "keychain-access-groups", ["AAAAAAAAAA.org.openbakery.Example", "AAAAAAAAAA.org.openbakery.ExampleWidget", "BBBBBBBBBB.org.openbakery.Foobar"])

		packageTask.xcode = new XcodeFake()
		//FileUtils.writeStringToFile(entitlementsFile, "")

	}

	def cleanup() {
		FileUtils.deleteDirectory(archiveDirectory)
		FileUtils.deleteDirectory(project.buildDir)
		FileUtils.deleteDirectory(projectDir)
	}

	void mockExampleApp(boolean withPlugin, boolean withSwift) {
		mockExampleApp(withPlugin, withSwift, false, true)
	}



	void mockExampleApp(boolean withPlugin, boolean withSwift, boolean withFramework, boolean adHoc) {
		String widgetPath = "PlugIns/ExampleTodayWidget.appex"
		// create dummy app


		def applicationBundle = new File(archiveDirectory, "Products/Applications/Example.app")

		File appDirectory = applicationBundle
		if (!appDirectory.exists()) {
			appDirectory.mkdirs();
		}

		FileUtils.writeStringToFile(new File(appDirectory, "Example"), "dummy");
		FileUtils.writeStringToFile(new File(appDirectory, "ResourceRules.plist"), "dummy");
		FileUtils.writeStringToFile(new File(appDirectory, "Info.plist"), "dummy");

		if (withPlugin) {
			File widgetsDirectory = new File(applicationBundle, widgetPath)
			FileUtils.writeStringToFile(new File(widgetsDirectory, "ExampleTodayWidget"), "dummy");
		}

		File infoPlist = new File(payloadAppDirectory, "Info.plist")
		plistHelperStub.setValueForPlist(infoPlist.absolutePath, "CFBundleIdentifier", "org.openbakery.Example")

		if (withPlugin) {
			File infoPlistWidget = new File(payloadAppDirectory, widgetPath + "/Info.plist");
			plistHelperStub.setValueForPlist(infoPlistWidget.absolutePath, "CFBundleIdentifier", "org.openbakery.ExampleWidget")
		}


		project.xcodebuild.outputPath.mkdirs()

		if (withSwift) {


			File libSwiftCore = new File(applicationBundle, "Frameworks/libswiftCore.dylib")
			FileUtils.writeStringToFile(libSwiftCore, "dummy")
			File libSwiftCoreArchive = new File(archiveDirectory, "SwiftSupport/libswiftCore.dylib")
			FileUtils.writeStringToFile(libSwiftCoreArchive, "dummy")

			File libswiftCoreGraphics = new File(applicationBundle, "Frameworks/libswiftCoreGraphics.dylib")
			FileUtils.writeStringToFile(libswiftCoreGraphics, "dummy")


		}

		if (withFramework) {
			File framework = new File(applicationBundle, "Frameworks/My.framework")
			framework.mkdirs()
			File frameworkFile = new File(applicationBundle, "Frameworks/My.framework/My")
			FileUtils.writeStringToFile(frameworkFile, "dummy")
		}

		File mobileprovision = null
		if (adHoc) {
			mobileprovision = new File("src/test/Resource/test.mobileprovision")
		} else {
			mobileprovision = new File("src/test/Resource/Appstore.mobileprovision")
		}
		project.xcodebuild.signing.mobileProvisionFile = mobileprovision
		mockEntitlementsFromPlist(mobileprovision)

		if (withPlugin) {
			File widgetMobileprovision = new File("src/test/Resource/test1.mobileprovision")
			project.xcodebuild.signing.mobileProvisionFile = widgetMobileprovision
			mockEntitlementsFromPlist(widgetMobileprovision)
		}


	}

	def mockXcodeVersion() {
		project.xcodebuild.commandRunner = commandRunner
		File xcodebuild7_1_1 = new File(projectDir, "Xcode7.1.1.app")
		File xcodebuild6_1 = new File(projectDir, "Xcode6-1.app")
		new File(xcodebuild7_1_1, "Contents/Developer/usr/bin").mkdirs()
		new File(xcodebuild6_1, "Contents/Developer/usr/bin").mkdirs()
		new File(xcodebuild7_1_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcodebuild6_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >> xcodebuild7_1_1.absolutePath + "\n"  + xcodebuild6_1.absolutePath
		commandRunner.runWithResult(xcodebuild7_1_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 7.1.1\nBuild version 7B1005"
		commandRunner.runWithResult(xcodebuild6_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 6.0\nBuild version 6A000"
	}


	List<String> codesignLibCommand(String path) {
		File payloadApp = new File(packageTask.outputPath, path)

		def commandList = [
						"/usr/bin/codesign",
						"--force",
						"--sign",
						"iPhone Developer: Firstname Surename (AAAAAAAAAA)",
						"--verbose",
						payloadApp.absolutePath,
						"--keychain",
						keychain.absolutePath

		]

		return commandList
	}

	List<String> codesignCommand(String path, String entitlementsName) {
		File payloadApp = new File(packageTask.outputPath, path)
		File entitlements = new File(project.buildDir.absolutePath, "package/" + entitlementsName)

		def commandList = [
						"/usr/bin/codesign",
						"--force",
						"--entitlements",
						entitlements.absolutePath,
						"--sign",
						"iPhone Developer: Firstname Surename (AAAAAAAAAA)",
						"--verbose",
						payloadApp.absolutePath,
						"--keychain",
						keychain.absolutePath

		]
		return commandList
	}


	void mockEntitlementsFromPlist(File provisioningProfile) {
		def commandList = ['security', 'cms', '-D', '-i', provisioningProfile.absolutePath]
		String result = new File('src/test/Resource/entitlements.plist').text
		commandRunner.runWithResult(commandList) >> result
		String basename = FilenameUtils.getBaseName(provisioningProfile.path)
		File plist = new File(project.buildDir.absolutePath + "/tmp/provision_" + basename + ".plist")
		commandList = ['/usr/libexec/PlistBuddy', '-x', plist.absolutePath, '-c', 'Print Entitlements']
		commandRunner.runWithResult(commandList) >> result
	}



	def "swift Framework xcode 6"() {
		given:
		mockXcodeVersion()
		project.xcodebuild.version = 6
		FileUtils.deleteDirectory(project.projectDir)
		mockExampleApp(false, true, false, false)

		when:
		File ipaBundle = new File(project.getBuildDir(), "package/Example.ipa")
		assert !ipaBundle.exists()
		packageTask.packageApplication()

		ZipFile zipFile = new ZipFile(ipaBundle);

		List<String> entries = new ArrayList<String>()

		for (ZipEntry entry : zipFile.entries()) {
			entries.add(entry.getName())
		}

		then:
		entries.contains("SwiftSupport/libswiftCore.dylib")
	}

	def "SwiftSupport should be added for Appstore IPA"() {
		given:
		mockXcodeVersion()
		project.xcodebuild.version = 7
		FileUtils.deleteDirectory(project.projectDir)
		mockExampleApp(false, true, false, false)

		when:
		File ipaBundle = new File(project.getBuildDir(), "package/Example.ipa")
		assert !ipaBundle.exists()
		packageTask.packageApplication()

		ZipFile zipFile = new ZipFile(ipaBundle);

		List<String> entries = new ArrayList<String>()

		for (ZipEntry entry : zipFile.entries()) {
			entries.add(entry.getName())
		}

		then:
		entries.contains("SwiftSupport/")
	}

	def "SwiftSupport should not be added for AdHoc IPA"() {
		given:
		mockXcodeVersion()
		project.xcodebuild.version = 7
		FileUtils.deleteDirectory(project.projectDir)
		mockExampleApp(false, true)

		when:
		File ipaBundle = new File(project.getBuildDir(), "package/Example.ipa")
		assert !ipaBundle.exists()
		packageTask.packageApplication()

		ZipFile zipFile = new ZipFile(ipaBundle);

		List<String> entries = new ArrayList<String>()

		for (ZipEntry entry : zipFile.entries()) {
			entries.add(entry.getName())
		}

		then:
		!entries.contains("SwiftSupport/")
	}

	def "test create payload"() {
		given:
		mockExampleApp(false, false)

		when:
		packageTask.packageApplication()

		File payloadDirectory = new File(packageTask.outputPath, "Payload")

		then:
		payloadDirectory.exists()
	}


	def "test copy app"() {
		given:
		mockExampleApp(false, false)

		when:
		packageTask.packageApplication()

		then:
		payloadAppDirectory.exists()
	}


	def "remove resource rules"() {
		given:
		mockExampleApp(false, false)

		when:
		packageTask.packageApplication()

		then:
		!(new File(payloadAppDirectory, "ResourceRules.plist")).exists()
	}


	def "embed provisioning profile"() {
		given:
		mockExampleApp(false, false)

		File mobileprovision = new File("src/test/Resource/test.mobileprovision")
		project.xcodebuild.signing.mobileProvisionFile = mobileprovision

		when:
		packageTask.packageApplication()

		File embedProvisioningProfile = new File(packageTask.outputPath, "Payload/Example.app/embedded.mobileprovision")

		then:
		embedProvisioningProfile.exists()
		FileUtils.checksumCRC32(embedProvisioningProfile) == FileUtils.checksumCRC32(mobileprovision)
	}


	def embedMultipleProvisioningProfile() {
		given:
		mockExampleApp(true, false)

		File firstMobileprovision = new File("src/test/Resource/test.mobileprovision")
		File secondMobileprovision = new File("src/test/Resource/test1.mobileprovision")
		project.xcodebuild.signing.mobileProvisionFile = firstMobileprovision
		project.xcodebuild.signing.mobileProvisionFile = secondMobileprovision

		when:
		packageTask.packageApplication()

		File firstEmbedProvisioningProfile = new File(packageTask.outputPath, "Payload/Example.app/embedded.mobileprovision")
		File secondEmbedProvisioningProfile = new File(packageTask.outputPath, "Payload/Example.app/PlugIns/ExampleTodayWidget.appex/embedded.mobileprovision")

		then:
		firstEmbedProvisioningProfile.exists()
		FileUtils.checksumCRC32(firstEmbedProvisioningProfile) == FileUtils.checksumCRC32(firstMobileprovision)
		secondEmbedProvisioningProfile.exists()
		FileUtils.checksumCRC32(secondEmbedProvisioningProfile) == FileUtils.checksumCRC32(secondMobileprovision)

	}


	def testSign() {
		def expectedCommandList
		def commandList

		given:
		mockExampleApp(false, false)

		when:
		packageTask.packageApplication()

		then:
		1 * commandRunner.run(_, _) >> { arguments -> commandList = arguments[0] }
		interaction {
			expectedCommandList = codesignCommand("Payload/Example.app", "entitlements_test.plist")
		}
		commandList == expectedCommandList
	}

	def testSignMultiple() {
		def codesignAppCommand = codesignCommand("Payload/Example.app", "entitlements_test.plist")
		def codesignWidgetCommand = codesignCommand("Payload/Example.app/PlugIns/ExampleTodayWidget.appex", "entitlements_test1.plist")

		given:
		mockExampleApp(true, false)

		when:
		packageTask.packageApplication()

		then:
		1 * commandRunner.run(codesignAppCommand, _)
		1 * commandRunner.run(codesignWidgetCommand, _)
	}


	def "create Ipa"() {
		given:
		mockExampleApp(false, false)

		when:
		packageTask.packageApplication()


		File ipaBundle = new File(project.getBuildDir(), "package/Example.ipa")

		assert ipaBundle.exists()

		ZipFile zipFile = new ZipFile(ipaBundle);

		List<String> entries = new ArrayList<String>()

		for (ZipEntry entry : zipFile.entries()) {
			entries.add(entry.getName())
		}

		then:
		entries.contains("Payload/Example.app/Example")
	}


	def "provisioning match"() {
		given:
		File appMobileprovision = new File("src/test/Resource/test.mobileprovision")
		File widgetMobileprovision = new File("src/test/Resource/test1.mobileprovision")
		File wildcardMobileprovision = new File("src/test/Resource/test-wildcard.mobileprovision")

		when:
		project.xcodebuild.signing.mobileProvisionFile = appMobileprovision
		project.xcodebuild.signing.mobileProvisionFile = widgetMobileprovision
		project.xcodebuild.signing.mobileProvisionFile = wildcardMobileprovision


		then:
		packageTask.getProvisionFileForIdentifier("org.openbakery.Example") == appMobileprovision
		packageTask.getProvisionFileForIdentifier("org.openbakery.ExampleWidget") == widgetMobileprovision
		packageTask.getProvisionFileForIdentifier("org.openbakery.Test") == wildcardMobileprovision
		packageTask.getProvisionFileForIdentifier("org.Test") == wildcardMobileprovision

	}


	def "provisioning Match more"() {
		given:
		File appMobileprovision = new File("src/test/Resource/openbakery.mobileprovision")
		File wildcardMobileprovision = new File("src/test/Resource/openbakery-wildcard.mobileprovision")

		when:
		project.xcodebuild.signing.mobileProvisionFile = appMobileprovision
		project.xcodebuild.signing.mobileProvisionFile = wildcardMobileprovision

		then:
		packageTask.getProvisionFileForIdentifier("org.openbakery.Example") == appMobileprovision
		packageTask.getProvisionFileForIdentifier("org.openbakery.Example.widget") == wildcardMobileprovision
		packageTask.getProvisionFileForIdentifier("org.openbakery.Example.extension") == wildcardMobileprovision

	}


	def "swift Codesign Libs"() {
		def codesignAppCommand = codesignCommand("Payload/Example.app", "entitlements_test.plist")
		def codesignLibCore = codesignLibCommand("Payload/Example.app/Frameworks/libswiftCore.dylib")
		def codesignLibCoreGraphics = codesignLibCommand("Payload/Example.app/Frameworks/libswiftCoreGraphics.dylib")

		given:

		mockExampleApp(false, true)

		when:
		packageTask.packageApplication()

		then:
		1 * commandRunner.run(codesignAppCommand, _)
		1 * commandRunner.run(codesignLibCore, _)
		1 * commandRunner.run(codesignLibCoreGraphics, _)
	}


	def "codesign Framework"() {
		def codesignAppCommand = codesignCommand("Payload/Example.app", "entitlements_test.plist")
		def codesignFramework = codesignLibCommand("Payload/Example.app/Frameworks/My.framework")

		given:
		mockExampleApp(false, true, true, true)

		when:
		packageTask.packageApplication()

		then:
		1 * commandRunner.run(codesignAppCommand, _)
		1 * commandRunner.run(codesignFramework, _)
	}




	def "has No Signing Identity"() {
		def message
		given:
		mockExampleApp(false, false)
		keychain.delete()
		StyledTextOutputStub textOutputStub = new StyledTextOutputStub()
		packageTask.output = textOutputStub

		when:
		packageTask.packageApplication()

		then:
		textOutputStub.toString().startsWith("Bundle not signed")
	}

	def "depends on"() {
		when:
		def dependsOn = packageTask.getDependsOn()
		then:
		dependsOn.size() == 3

		dependsOn.contains(XcodePlugin.KEYCHAIN_CREATE_TASK_NAME)
		dependsOn.contains(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)

	}

	def "finalized"() {
		when:
		def finalized = packageTask.finalizedBy.values
		then:
		finalized.contains(XcodePlugin.KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME)
	}


	def "delete CFBundleResourceSpecification"() {
		given:
		mockExampleApp(false, true)

		when:
		packageTask.packageApplication()

		then:
		plistHelperStub.plistCommands.size() > 0
		plistHelperStub.plistCommands.get(0).equals("Delete CFBundleResourceSpecification")
	}


	def "getKeychainAccessGroupFromEntitlements"() {
		given:
		packageTask.plistHelper = new PlistHelper(project.projectDir, new CommandRunner())

		when:
		List<String> keychainAccessGroup = packageTask.getKeychainAccessGroupFromEntitlements(payloadAppDirectory)

		then:
		keychainAccessGroup.size() == 3
		keychainAccessGroup[0] == "\$(AppIdentifierPrefix)org.openbakery.Example"
		keychainAccessGroup[1] == "\$(AppIdentifierPrefix)org.openbakery.ExampleWidget"
		keychainAccessGroup[2] == "BBBBBBBBBB.org.openbakery.Foobar"
	}

	def "create entitlements with keychain access groups"() {
		given:
		mockExampleApp(false, false)
		packageTask.plistHelper = new PlistHelper(project.projectDir, new CommandRunner())

		when:
		File entitlementsFile = packageTask.createEntitlementsFile(payloadAppDirectory, "org.openbakery.Example")

		then:
		entitlementsFile.exists()
		entitlementsFile.text.contains("AAAAAAAAAA.org.openbakery.Example")
		entitlementsFile.text.contains("AAAAAAAAAA.org.openbakery.ExampleWidget")

	}


	def "use custom entitlements file"() {
		given:

		project.xcodebuild.signing.entitlementsFile = "MyCustomEntitlements.plist"

		mockExampleApp(false, false)
		packageTask.plistHelper = new PlistHelper(project.projectDir, new CommandRunner())

		when:
		File entitlementsFile = packageTask.createEntitlementsFile(payloadAppDirectory, "org.openbakery.Example")

		then:
		entitlementsFile.path.endsWith("MyCustomEntitlements.plist")


	}

}
