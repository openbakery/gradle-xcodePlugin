package org.openbakery.packaging

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.RandomStringUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.openbakery.output.StyledTextOutputStub
import org.openbakery.test.ApplicationDummy
import org.openbakery.testdouble.PlistHelperStub
import org.openbakery.testdouble.XcodeFake
import org.openbakery.util.PathHelper
import org.openbakery.xcode.Extension
import org.openbakery.xcode.Type
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class PackageTaskSpecification extends Specification {


	Project project
	PackageTask packageTask

	ApplicationDummy applicationDummy
	CommandRunner commandRunner = Mock(CommandRunner)

	PlistHelperStub plistHelperStub = new PlistHelperStub()

	File projectDir
	File infoPlist
	File payloadAppDirectory
	File archiveDirectory
	File keychain
	File tmpDir
	File outputPath

	void setup() {

		tmpDir = new File(System.getProperty("java.io.tmpdir"))

		String tmpName =  "gradle-xcodebuild-" + RandomStringUtils.randomAlphanumeric(5)
		projectDir = new File(tmpDir, tmpName)

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



		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"


		keychain = new File(tmpDir, "gradle.keychain")
		FileUtils.writeStringToFile(keychain, "dummy");

		project.xcodebuild.signing.keychain = keychain.absolutePath
		project.xcodebuild.target = "Example"

		packageTask.xcode = new XcodeFake()

	}

	def cleanup() {
		if (applicationDummy != null) {
			applicationDummy.cleanup()
		}
		FileUtils.deleteDirectory(project.buildDir)
		FileUtils.deleteDirectory(projectDir)
		keychain.delete()
	}

	void mockExampleApp(boolean withPlugin, boolean withSwift, boolean withFramework = false, boolean adHoc = true, boolean bitcode = false) {
		outputPath = PathHelper.resolvePackageFolder(project)

		archiveDirectory = new File(PathHelper.resolveArchiveFolder(project), "Example.xcarchive")

		File payloadDirectory = new File(outputPath, "Payload")
		payloadAppDirectory = new File(payloadDirectory, "Example.app");

		applicationDummy = new ApplicationDummy(archiveDirectory)
        applicationDummy.plistHelperStub = plistHelperStub
        applicationDummy.payloadAppDirectory = payloadAppDirectory

		def appDirectory = applicationDummy.create(adHoc)

		if (withPlugin) {
			applicationDummy.createPlugin()
		}

		File infoPlist = new File(payloadAppDirectory, "Info.plist")
		plistHelperStub.setValueForPlist(infoPlist, "CFBundleIdentifier", "org.openbakery.test.Example")

		project.xcodebuild.outputPath.mkdirs()

		if (withSwift) {
            applicationDummy.createSwiftLibs()
		}

		if (withFramework) {
            applicationDummy.createFramework()
		}

		for (File mobileProvision in applicationDummy.mobileProvisionFile) {
			project.xcodebuild.signing.addMobileProvisionFile(mobileProvision)
			mockEntitlementsFromPlist(mobileProvision)
		}

		// onDemandResources
		FileUtils.writeStringToFile(new File(appDirectory, "OnDemandResources.plist"), "dummy")
		File onDemandResources = new File(archiveDirectory, "Products/OnDemandResources/org.openbakery.test.Example.SampleImages.assetpack")
		onDemandResources.mkdirs()
		FileUtils.writeStringToFile(new File(onDemandResources, "Info.plist"), "dummy")

		if (bitcode) {
            applicationDummy.createBCSymbolMaps()
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
		File payloadApp = new File(outputPath, path)

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
		File payloadApp = new File(outputPath, path)
		File entitlements = new File(tmpDir, entitlementsName)

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
		String result = new File('../libtest/src/main/Resource/entitlements.plist').text
		commandRunner.runWithResult(commandList) >> result
		String basename = FilenameUtils.getBaseName(provisioningProfile.path)
		File plist = new File(System.getProperty("java.io.tmpdir") + "/provision_" + basename + ".plist")
		commandList = ['/usr/libexec/PlistBuddy', '-x', plist.absolutePath, '-c', 'Print Entitlements']
		commandRunner.runWithResult(commandList) >> result
	}


	List<String> ipaEntries() {
		File ipaBundle = new File(project.getBuildDir(), "package/Example.ipa")
		assert ipaBundle.exists()

		ZipFile zipFile = new ZipFile(ipaBundle)
		List<String> entries = new ArrayList<String>()
		for (ZipEntry entry : zipFile.entries()) {
			entries.add(entry.getName())
		}
		return entries
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

		List<String> entries = ipaEntries()

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

		List<String> entries = ipaEntries()

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
		List<String> entries = ipaEntries()

		then:
		!entries.contains("SwiftSupport/")
	}

	def "test create payload"() {
		given:
		mockExampleApp(false, false)

		when:
		packageTask.packageApplication()

		File payloadDirectory = new File(outputPath, "Payload")

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

		File mobileprovision = applicationDummy.mobileProvisionFile.first()

		when:
		packageTask.packageApplication()

		File embedProvisioningProfile = new File(outputPath, "Payload/Example.app/embedded.mobileprovision")

		then:
		embedProvisioningProfile.exists()
		FileUtils.checksumCRC32(embedProvisioningProfile) == FileUtils.checksumCRC32(mobileprovision)
	}


	def embedMultipleProvisioningProfile() {
		given:
		mockExampleApp(true, false)

		File firstMobileprovision = applicationDummy.mobileProvisionFile[0]
		File secondMobileprovision = applicationDummy.mobileProvisionFile[1]

		when:
		packageTask.packageApplication()

		File firstEmbedProvisioningProfile = new File(outputPath, "Payload/Example.app/embedded.mobileprovision")
		File secondEmbedProvisioningProfile = new File(outputPath, "Payload/Example.app/PlugIns/ExampleTodayWidget.appex/embedded.mobileprovision")

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
		given:
		mockExampleApp(true, false)
		def codesignAppCommand = codesignCommand("Payload/Example.app", "entitlements_test.plist")
		def codesignWidgetCommand = codesignCommand("Payload/Example.app/PlugIns/ExampleTodayWidget.appex", "entitlements_extension.plist")

		println(codesignWidgetCommand)
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

		List<String> entries = ipaEntries()

		then:
		entries.contains("Payload/Example.app/Example")
	}




	def "swift Codesign Libs"() {
		given:
		mockExampleApp(false, true)
		def codesignAppCommand = codesignCommand("Payload/Example.app", "entitlements_test.plist")
		def codesignLibCore = codesignLibCommand("Payload/Example.app/Frameworks/libswiftCore.dylib")
		def codesignLibCoreGraphics = codesignLibCommand("Payload/Example.app/Frameworks/libswiftCoreGraphics.dylib")

		when:
		packageTask.packageApplication()

		then:
		1 * commandRunner.run(codesignAppCommand, _)
		1 * commandRunner.run(codesignLibCore, _)
		1 * commandRunner.run(codesignLibCoreGraphics, _)
	}


	def "codesign Framework"() {
		given:
		mockExampleApp(false, true, true, true)
		def codesignAppCommand = codesignCommand("Payload/Example.app", "entitlements_test.plist")
		def codesignFramework = codesignLibCommand("Payload/Example.app/Frameworks/My.framework")

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
		dependsOn.contains(XcodePlugin.KEYCHAIN_CREATE_TASK_NAME)
		dependsOn.contains(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)

	}

	def "finalized"() {
		when:
		def finalized = packageTask.finalizedBy.getDependencies()
		def keychainRemoveTask = project.getTasks().getByPath(XcodePlugin.KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME)
		then:
		finalized.contains(keychainRemoveTask)
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


	def "change build path"() {
		project.buildDir = new File(projectDir, 'foobar').absoluteFile
		mockExampleApp(false, false)

		when:
		packageTask.packageApplication()

		then:

		packageTask.outputPath.absolutePath.contains("foobar")
	}


	def "copy OnDemandResources"() {
		given:
		mockExampleApp(false, false)

		when:
		packageTask.packageApplication()

		List<String> entries = ipaEntries()

		then:
		entries.contains("Payload/Example.app/OnDemandResources.plist")
		entries.contains("Payload/Example.app/OnDemandResources/org.openbakery.test.Example.SampleImages.assetpack/Info.plist")
	}



	def "set identity"() {
		when:
		packageTask.signingIdentity = "Me"

		then:
		packageTask.codesignParameters.signingIdentity == "Me"
	}

	def "set type is passed to codesignParameters"() {
		given:
		mockExampleApp(false, false)
		when:

		project.xcodebuild.type = Type.macOS

		packageTask.packageApplication()

		then:
		packageTask.codesignParameters.type == Type.macOS

	}

	def "copy bcsymbolsmaps"() {
		given:
		mockExampleApp(false, false, false, false, true)

		when:
		packageTask.packageApplication()
		List<String> entries = ipaEntries()

		then:
		entries.contains("BCSymbolMaps/")
		entries.contains("BCSymbolMaps/14C60358-AC0B-35CF-A079-042050D404EE.bcsymbolmap")
		entries.contains("BCSymbolMaps/2154C009-2AC2-3241-9E2E-D8B8046B03C8.bcsymbolmap")
		entries.contains("BCSymbolMaps/23CFBC47-4B7D-391C-AB95-48408893A14A.bcsymbolmap")
	}

	def "bcsymbolsmaps does not exists, to BCSymbolMaps is not created"() {
		given:
		mockExampleApp(false, false, false, false, false)

		when:
		packageTask.packageApplication()
		List<String> entries = ipaEntries()

		then:
		!entries.contains("BCSymbolMaps/")
	}

	def "copy sticker extension support directory"() {
		given:
		mockExampleApp(false, false)
		applicationDummy.createPlugin(Extension.sticker)
		project.xcodebuild.signing.addMobileProvisionFile(applicationDummy.mobileProvisionFile.last())

        when:
		packageTask.packageApplication()
		List<String> entries = ipaEntries()

		then:
		entries.contains("MessagesApplicationExtensionSupport/MessagesApplicationExtensionStub")
	}
}
