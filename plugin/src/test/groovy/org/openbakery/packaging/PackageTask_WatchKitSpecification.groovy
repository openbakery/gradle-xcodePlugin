package org.openbakery.packaging

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.Type
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.XcodePlugin
import org.openbakery.stubs.PlistHelperStub
import spock.lang.Specification

/**
 * Created by rene on 09.10.15.
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

	void setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productName = 'ExampleWatchKit'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.type = Type.iOS
		project.xcodebuild.simulator = false
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"


		packageTask = project.getTasks().getByPath(XcodePlugin.PACKAGE_TASK_NAME)
		packageTask.plistHelper = plistHelperStub

		packageTask.commandRunner = commandRunner

		provisionLibraryPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/");

		archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Example.xcarchive")

		File payloadDirectory = new File(packageTask.outputPath, "Payload")
		payloadAppDirectory = new File(payloadDirectory, "ExampleWatchKit.app");

		project.xcodebuild.signing.identity = "iPhone Developer: Firstname Surename (AAAAAAAAAA)"
	}


	void createExampleApp() {

		def applicationBundle = new File(archiveDirectory, "Products/Applications/ExampleWatchKit.app")

		File appDirectory = applicationBundle
		if (!appDirectory.exists()) {
			appDirectory.mkdirs();
		}

		FileUtils.writeStringToFile(new File(appDirectory, "ExampleWatchKit"), "dummy");
		FileUtils.writeStringToFile(new File(appDirectory, "Info.plist"), "dummy");

		File infoPlist = new File(payloadAppDirectory, "Info.plist")
		plistHelperStub.setValueForPlist(infoPlist.absolutePath, "CFBundleIdentifier", "org.openbakery.Example")


		String watchkitAppPath = "Watch/ExampleWatchkit WatchKit App.app"
		File watchkitDirectory = new File(applicationBundle, watchkitAppPath)
		FileUtils.writeStringToFile(new File(watchkitDirectory, "ExampleWatchkit WatchKit App"), "dummy");

		File infoPlistWatchkit = new File(payloadAppDirectory, watchkitAppPath + "/Info.plist");
		plistHelperStub.setValueForPlist(infoPlistWatchkit.absolutePath, "CFBundleIdentifier", "org.openbakery.Example.watchkitapp")


		String watchkitExtensionPath = "Watch/ExampleWatchkit WatchKit App.app/PlugIns/ExampleWatchkit WatchKit Extension.appex"
		File watchkitExtensionDirectory = new File(applicationBundle, watchkitExtensionPath)
		FileUtils.writeStringToFile(new File(watchkitExtensionDirectory, "ExampleWatchkit WatchKit Extension"), "dummy");

		File infoPlistWatchkitExtension = new File(payloadAppDirectory, watchkitExtensionPath + "/Info.plist");
		plistHelperStub.setValueForPlist(infoPlistWatchkitExtension.absolutePath, "CFBundleIdentifier", "org.openbakery.Example.watchkitapp.watchkitextension")

		//File infoPlist = new File(payloadAppDirectory, "Info.plist")
		//		plistHelperStub.setValueForPlist(infoPlist.absolutePath, "CFBundleIdentifier", "org.openbakery.Example")

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
						"/var/tmp/gradle.keychain"

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
						"/var/tmp/gradle.keychain"

		]

		return commandList
	}



	def "codesign watchkit app"() {
		//def commandList
		def codesignAppCommand = codesignCommand("Payload/ExampleWatchKit.app", "entitlements_test.plist")
		def codesignWatchKitAppCommand = codesignCommand("Payload/ExampleWatchKit.app/Watch/ExampleWatchkit WatchKit App.app", "entitlements_exampleWatchkit.plist")
		def codesignWatchKitExtensionCommand = codesignCommand("Payload/ExampleWatchKit.app/Watch/ExampleWatchkit WatchKit App.app/PlugIns/ExampleWatchkit WatchKit Extension.appex", "entitlements_exampleWatchkitExtension.plist")

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

}
