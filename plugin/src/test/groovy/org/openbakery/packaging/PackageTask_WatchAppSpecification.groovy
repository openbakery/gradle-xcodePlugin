package org.openbakery.packaging

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.testdouble.PlistHelperStub
import org.openbakery.testdouble.XcodeFake
import org.openbakery.util.PathHelper
import org.openbakery.xcode.Type
import spock.lang.Specification

class PackageTask_WatchAppSpecification extends Specification {


	Project project
	PackageLegacyTask packageTask;

	CommandRunner commandRunner = Mock(CommandRunner)

	PlistHelperStub plistHelperStub = new PlistHelperStub()

	File provisionLibraryPath
	File projectDir
	File infoPlist
	File payloadAppDirectory
	File archiveDirectory
	File keychain

	File applicationBundle
	File watchkitExtensionBundle
	String watchkitExtensionPath
	File tmpDir
	File outputPath

	void setup() {
		tmpDir = new File(System.getProperty("java.io.tmpdir"))

		projectDir = new File(tmpDir, "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productName = 'ExampleWatchKit'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.type = Type.iOS
		project.xcodebuild.simulator = false
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"


		packageTask = project.getTasks().getByPath(PackageTask.NAME)
		packageTask.plistHelper = plistHelperStub

		packageTask.commandRunner = commandRunner
		packageTask.xcode = new XcodeFake()

		provisionLibraryPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/");

		archiveDirectory = new File(PathHelper.resolveArchiveFolder(project), "Example.xcarchive")

		outputPath = PathHelper.resolvePackageFolder(project)

		File payloadDirectory = new File(outputPath, "Payload")
		payloadAppDirectory = new File(payloadDirectory, "ExampleWatchKit.app");

		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"
		keychain = new File(projectDir, "gradle.keychain")
		FileUtils.writeStringToFile(keychain, "dummy");
		project.xcodebuild.signing.keychain = keychain.absolutePath

		applicationBundle = new File(archiveDirectory, "Products/Applications/ExampleWatchKit.app")
		watchkitExtensionPath = "Watch/ExampleWatchkit WatchKit App.app/PlugIns/ExampleWatchkit WatchKit Extension.appex"
		watchkitExtensionBundle = new File(applicationBundle, watchkitExtensionPath)

	}

	def cleanup() {
		FileUtils.deleteDirectory(projectDir)
	}

	void createFrameworkIn(File directory) {

		def frameworksDirectory = new File(directory, "Frameworks/MyFramework.framework")
		if (!frameworksDirectory.exists()) {
			frameworksDirectory.mkdirs()
		}
		FileUtils.writeStringToFile(new File(frameworksDirectory, "MyFramework"), "dummy");

	}

	/* use ApplicationDummy from libtest */
	void createExampleApp() {


		File appDirectory = applicationBundle
		if (!appDirectory.exists()) {
			appDirectory.mkdirs();
		}

		FileUtils.writeStringToFile(new File(appDirectory, "ExampleWatchKit"), "dummy");
		FileUtils.writeStringToFile(new File(appDirectory, "Info.plist"), "dummy");

		File infoPlist = new File(payloadAppDirectory, "Info.plist")
		plistHelperStub.setValueForPlist(infoPlist, "CFBundleIdentifier", "org.openbakery.test.Example")


		String watchkitAppPath = "Watch/ExampleWatchkit WatchKit App.app"
		File watchkitDirectory = new File(applicationBundle, watchkitAppPath)
		FileUtils.writeStringToFile(new File(watchkitDirectory, "ExampleWatchkit WatchKit App"), "dummy");

		File infoPlistWatchkit = new File(payloadAppDirectory, watchkitAppPath + "/Info.plist");
		plistHelperStub.setValueForPlist(infoPlistWatchkit, "CFBundleIdentifier", "org.openbakery.test.Example.watchkitapp")


		FileUtils.writeStringToFile(new File(watchkitExtensionBundle, "ExampleWatchkit WatchKit Extension"), "dummy");

		File infoPlistWatchkitExtension = new File(payloadAppDirectory, watchkitExtensionPath + "/Info.plist");
		plistHelperStub.setValueForPlist(infoPlistWatchkitExtension, "CFBundleIdentifier", "org.openbakery.test.Example.watchkitapp.watchkitextension")

		//File infoPlist = new File(payloadAppDirectory, "Info.plist")
		//		plistHelperStub.setValueForPlist(infoPlist.absolutePath, "CFBundleIdentifier", "org.openbakery.test.Example")

		project.xcodebuild.outputPath.mkdirs()


		project.xcodebuild.signing.addMobileProvisionFile( new File("../libtest/src/main/Resource/test.mobileprovision"))
		project.xcodebuild.signing.addMobileProvisionFile( new File("src/test/Resource/exampleWatchkit.mobileprovision"))
		project.xcodebuild.signing.addMobileProvisionFile( new File("src/test/Resource/exampleWatchkitExtension.mobileprovision"))

	}

	void includeLibswiftRemoteMirrorIntoExampleApp() {
		FileUtils.writeStringToFile(new File(applicationBundle, "libswiftRemoteMirror.dylib"), "dummy");
		FileUtils.writeStringToFile(new File(watchkitExtensionBundle, "libswiftRemoteMirror.dylib"), "dummy");
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



	def "codesign watchkit app"() {
		given:
		createExampleApp()
		//def commandList
		def codesignAppCommand = codesignCommand("Payload/ExampleWatchKit.app", "entitlements_test.plist")
		def codesignWatchKitAppCommand = codesignCommand("Payload/ExampleWatchKit.app/Watch/ExampleWatchkit WatchKit App.app", "entitlements_exampleWatchkit.plist")
		def codesignWatchKitExtensionCommand = codesignCommand("Payload/ExampleWatchKit.app/Watch/ExampleWatchkit WatchKit App.app/PlugIns/ExampleWatchkit WatchKit Extension.appex", "entitlements_exampleWatchkitExtension.plist")

		when:
		packageTask.packageApplication()

		then:
		1 * commandRunner.run(codesignAppCommand, _)
		1 * commandRunner.run(codesignWatchKitAppCommand, _)
		1 * commandRunner.run(codesignWatchKitExtensionCommand, _)
		//1 * commandRunner.run(_, _) >> { arguments -> commandList = arguments[0] }
		//commandList == codesignWatchKitExtensionCommand
	}


	def "copy frameworks"() {

		given:
		createExampleApp()
		createFrameworkIn(applicationBundle)

		when:
		packageTask.packageApplication()

		then:
		new File(outputPath, "Payload/ExampleWatchKit.app/Frameworks/MyFramework.framework").exists()
	}

	def "do not copy frameworks in app extension"() {
		given:
		createExampleApp()
		createFrameworkIn(applicationBundle)
		createFrameworkIn(watchkitExtensionBundle)

		when:
		packageTask.packageApplication()

		then:
		!(new File(outputPath, "Payload/ExampleWatchKit.app/" + watchkitExtensionPath + "/Frameworks").exists())

	}

	def "remove libswiftRemoteMirror.dylib from app"() {
		given:
		createExampleApp()
		includeLibswiftRemoteMirrorIntoExampleApp()

		when:
		packageTask.packageApplication()

		then:
		!(new File(outputPath, "Payload/ExampleWatchKit.app/libswiftRemoteMirror.dylib").exists())

	}

	def "remove libswiftRemoteMirror.dylib from app extenstion"() {
		given:
		createExampleApp()
		includeLibswiftRemoteMirrorIntoExampleApp()

		when:
		packageTask.packageApplication()

		then:
		!(new File(outputPath, "Payload/ExampleWatchKit.app/Watch/ExampleWatchkit WatchKit App.app/PlugIns/ExampleWatchkit WatchKit Extension.appex/libswiftRemoteMirror.dylib").exists())

	}
}
