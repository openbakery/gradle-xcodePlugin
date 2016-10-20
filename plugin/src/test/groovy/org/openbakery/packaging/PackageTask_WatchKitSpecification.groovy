package org.openbakery.packaging

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.xcode.Type
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.XcodePlugin
import org.openbakery.testdouble.PlistHelperStub
import org.openbakery.testdouble.XcodeFake
import spock.lang.Specification

/**
 * Created by rene on 22.10.15.
 */
class PackageTask_WatchKitSpecification extends Specification {


	Project project
	PackageTask packageTask;

	CommandRunner commandRunner = Mock(CommandRunner)

	PlistHelperStub plistHelperStub = new PlistHelperStub()

	File provisionLibraryPath
	File projectDir
	File infoPlist
	File payloadAppDirectory
	File archiveDirectory
	File keychain

	void setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

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
		packageTask.xcode = new XcodeFake()

		provisionLibraryPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/");

		archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Example.xcarchive")

		File payloadDirectory = new File(packageTask.outputPath, "Payload")
		payloadAppDirectory = new File(payloadDirectory, "Example.app");

		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"
		keychain = new File(projectDir, "gradle.keychain")
		FileUtils.writeStringToFile(keychain, "dummy");
		project.xcodebuild.signing.keychain = keychain.absolutePath

	}

	def cleanup() {
		FileUtils.deleteDirectory(projectDir)
	}


	void createExampleApp() {

		def applicationBundle = new File(archiveDirectory, "Products/Applications/Example.app")

		File appDirectory = applicationBundle
		if (!appDirectory.exists()) {
			appDirectory.mkdirs();
		}

		FileUtils.writeStringToFile(new File(appDirectory, "Example"), "dummy");
		FileUtils.writeStringToFile(new File(appDirectory, "Info.plist"), "dummy");

		File infoPlist = new File(payloadAppDirectory, "Info.plist")
		plistHelperStub.setValueForPlist(infoPlist.absolutePath, "CFBundleIdentifier", "org.openbakery.Example")


		String watchkitExtensionPath = "PlugIns/Example WatchKit Extension.appex"
		File watchkitExtensionDirectory = new File(applicationBundle, watchkitExtensionPath)
		FileUtils.writeStringToFile(new File(watchkitExtensionDirectory, "Example WatchKit App.app/Example WatchKit App"), "dummy");
		FileUtils.writeStringToFile(new File(watchkitExtensionDirectory, "Example WatchKit Extension"), "dummy");
		File infoPlistWatchkit = new File(payloadAppDirectory, watchkitExtensionPath + "/Info.plist");
		plistHelperStub.setValueForPlist(infoPlistWatchkit.absolutePath, "CFBundleIdentifier", "org.openbakery.Example.watchkitapp.watchkitextension")


		File infoPlistWatchkitExtension = new File(payloadAppDirectory, watchkitExtensionPath + "/Example WatchKit App.app/Info.plist");
		plistHelperStub.setValueForPlist(infoPlistWatchkitExtension.absolutePath, "CFBundleIdentifier", "org.openbakery.Example.watchkitapp")


		project.xcodebuild.outputPath.mkdirs()


		project.xcodebuild.signing.mobileProvisionFile = new File("src/test/Resource/test.mobileprovision")
		project.xcodebuild.signing.mobileProvisionFile = new File("src/test/Resource/exampleWatchkit.mobileprovision")
		project.xcodebuild.signing.mobileProvisionFile = new File("src/test/Resource/exampleWatchkitExtension.mobileprovision")

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



	def "codesign watchkit app"() {
		//def commandList
		def codesignAppCommand = codesignCommand("Payload/Example.app", "entitlements_test.plist")
		def codesignWatchKitAppCommand = codesignCommand("Payload/Example.app/PlugIns/Example WatchKit Extension.appex/Example WatchKit App.app", "entitlements_exampleWatchkit.plist")
		def codesignWatchKitExtensionCommand = codesignCommand("Payload/Example.app/PlugIns/Example WatchKit Extension.appex", "entitlements_exampleWatchkitExtension.plist")

		given:
		createExampleApp()

		when:
		packageTask.packageApplication()

		then:
		1 * commandRunner.run(codesignAppCommand, _)
		1 * commandRunner.run(codesignWatchKitAppCommand, _)
		1 * commandRunner.run(codesignWatchKitExtensionCommand, _)

		//1 * commandRunner.run(_, _) >> { arguments -> commandList = arguments[0] }
		//commandList == codesignWatchKitExtensionCommand


	}


	def "embed provisioning profiles"() {

		given:
		createExampleApp()

		when:
		packageTask.packageApplication()

		then:
		new File(packageTask.outputPath, "Payload/Example.app/embedded.mobileprovision").exists()
		new File(packageTask.outputPath, "Payload/Example.app/PlugIns/Example WatchKit Extension.appex/Example WatchKit App.app/embedded.mobileprovision").exists()
		new File(packageTask.outputPath, "Payload/Example.app/PlugIns/Example WatchKit Extension.appex/embedded.mobileprovision").exists()

	}
}
