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

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Created by rene on 23.10.15.
 */
class PackageTask_OSXSpecification  extends Specification {

	Project project
	PackageTask packageTask;

	CommandRunner commandRunner = Mock(CommandRunner)

	File provisionLibraryPath
	File projectDir
	File infoPlist
	File appDirectory
	File archiveDirectory
	File provisionProfile
	File keychain
	File applicationBundle

	PlistHelperStub plistHelperStub = new PlistHelperStub()


	void setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		FileUtils.deleteDirectory(projectDir)
		projectDir.mkdirs()
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productName = 'Example'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.type = Type.OSX

		keychain = new File(projectDir, "gradle.keychain")
		FileUtils.writeStringToFile(keychain, "dummy");

		project.xcodebuild.signing.keychain = keychain.absolutePath
		project.xcodebuild.signing.identity = 'iPhone Developer: Firstname Surename (AAAAAAAAAA)'

		project.xcodebuild.xcodePath = '/Applications/Xcode.app'

		packageTask = project.getTasks().getByPath(XcodePlugin.PACKAGE_TASK_NAME)
		packageTask.plistHelper = plistHelperStub

		packageTask.commandRunner = commandRunner

		provisionLibraryPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/");

		archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Example.xcarchive")

		appDirectory = new File(packageTask.outputPath, "Example.app");

		provisionProfile = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")

	}

	def cleanup() {
		FileUtils.deleteDirectory(projectDir)
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

	List<String> codesignCommand(String path) {
		File payloadApp = new File(packageTask.outputPath, path)
		File entitlements = new File(project.buildDir.absolutePath, "package/entitlements_test-wildcard-mac-development.plist")

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

	void mockExampleApp() {
		mockExampleApp(null)
	}

	void mockExampleApp(String frameworkPath) {
		// create dummy app

		applicationBundle = new File(archiveDirectory, "Products/Applications/Example.app")

		File appDirectory = applicationBundle
		if (!appDirectory.exists()) {
			appDirectory.mkdirs();
		}

		FileUtils.writeStringToFile(new File(appDirectory, "Example"), "dummy");
		FileUtils.writeStringToFile(new File(appDirectory, "ResourceRules.plist"), "dummy");
		FileUtils.writeStringToFile(new File(appDirectory, "Contents/Info.plist"), "dummy");

		if (frameworkPath != null) {
			File frameworkFile = new File(appDirectory, frameworkPath)
			frameworkFile.mkdirs()
		}

		File infoPlist = new File(this.appDirectory, "Contents/Info.plist")
		plistHelperStub.setValueForPlist(infoPlist.absolutePath, "CFBundleIdentifier", "org.openbakery.Example")

		plistHelperStub.setValueForPlist(infoPlist.absolutePath, "Delete CFBundleResourceSpecification")
		plistHelperStub.setValueForPlist(infoPlist.absolutePath, "CFBundleIdentifier", "org.openbakery.Example")


		File mobileprovision = new File("src/test/Resource/test-wildcard-mac-development.provisionprofile")
		project.xcodebuild.signing.mobileProvisionFile = mobileprovision

		String basename = FilenameUtils.getBaseName(mobileprovision.path)
		File plist = new File(project.buildDir.absolutePath + "/tmp/provision_" + basename + ".plist")
		plistHelperStub.setValueForPlist(plist.absolutePath, "Entitlements:com.apple.application-identifier", "org.openbakery.Example")



		project.xcodebuild.outputPath.mkdirs()
	}


	def "create package path"() {
		given:
		mockExampleApp()

		when:
		packageTask.packageApplication()

		then:
		// has to be same folder as signing for MacOSX
		packageTask.outputPath.exists()
	}


	def "copy app"() {
		given:
		mockExampleApp()

		when:
		packageTask.packageApplication()

		then:
		appDirectory.exists()
	}



	def "remove ResourceRules"() {
		given:
		mockExampleApp()

		when:
		packageTask.packageApplication()

		then:
		!(new File(appDirectory, "ResourceRules.plist")).exists()
	}


	def "codesign MacApp only"() {
		def expectedCodesignCommand = codesignCommand("Example.app")

		given:
		mockExampleApp()

		when:
		project.xcodebuild.signing.mobileProvisionFile = provisionProfile
		packageTask.packageApplication()

		then:
		1 * commandRunner.run(expectedCodesignCommand, _)
		/*
		1 * commandRunner.run(_, _) >> { arguments -> commandList = arguments[0] }
		commandList == expectedCodesignCommand
*/
	}




	def "codesign MacApp with Framework"() {
		def commandList
		def expectedCodesignCommand = codesignCommand("Example.app")
		def expectedCodesignCommandLib = codesignLibCommand("Example.app/Contents/Frameworks/Sparkle.framework/Versions/Current")

		given:

		mockExampleApp("Contents/Frameworks/Sparkle.framework")
		project.xcodebuild.signing.mobileProvisionFile = provisionProfile

		when:
		packageTask.packageApplication()

		then:
		1 * commandRunner.run(expectedCodesignCommand, _)

		1 * commandRunner.run(_, _) >> { arguments -> commandList = arguments[0] }
		commandList == expectedCodesignCommandLib


	}



	def "codesign Mac App with with embedded app"() {
		def expectedCodesignCommand = codesignCommand("Example.app")
		def unexpectedCodesignCommandApp = codesignLibCommand("Example.app/Contents/Resources/Autoupdate.app")
		def unexpectedCodesignCommandSecondApp = codesignLibCommand("Example.app/Contents/Resources/Autoupdate.app/Contents/Resources/AnotherApp.app")

		given:
		mockExampleApp("Contents/Resources/Autoupdate.app/Contents/Resources/AnotherApp.app")
		project.xcodebuild.signing.mobileProvisionFile = provisionProfile

		when:
		packageTask.packageApplication()

		then:
		0 * commandRunner.run(unexpectedCodesignCommandSecondApp, _)

		then:
		0 * commandRunner.run(unexpectedCodesignCommandApp, _)

		then:
		1 * commandRunner.run(expectedCodesignCommand, _)

	}

	def "codesign Mac App with framework that contains an app with symlink"() {
		def commandList
		def expectedCodesignCommand = codesignCommand("Example.app")
		def expectedCodesignCommandLib = codesignLibCommand("Example.app/Contents/Frameworks/Sparkle.framework/Versions/Current")
		def unexpectedCodesignCommandLibApp = codesignLibCommand("Example.app/Contents/Frameworks/Sparkle.framework/Versions/A/Resources/Autoupdate.app")
		def unexpectedCodesignCommandLibAppWithSymlink = codesignLibCommand("Example.app/Contents/Frameworks/Sparkle.framework/Versions/Current/Resources/Autoupdate.app")

		given:
		mockExampleApp("Contents/Frameworks/Sparkle.framework/Versions/A/Resources/Autoupdate.app")

		File sourceFile = new File(packageTask.outputPath, "Example.app/Contents/Frameworks/Sparkle.framework/Versions/A/Resources/Autoupdate.app")
		File symLinkTo = new File(packageTask.outputPath, "Example.app/Contents/Frameworks/Sparkle.framework/Versions/Current/Resources/")
		symLinkTo.mkdirs()
		CommandRunner c = new CommandRunner()

		c.run("ln", "-s", sourceFile.absolutePath, symLinkTo.absolutePath)

		project.xcodebuild.signing.mobileProvisionFile = provisionProfile

		when:
		packageTask.packageApplication()

		then:
		0 * commandRunner.run(unexpectedCodesignCommandLibAppWithSymlink, _)

		then:
		0 * commandRunner.run(unexpectedCodesignCommandLibApp, _)

		then:
		1 * commandRunner.run(expectedCodesignCommandLib, _)

		then:
		1 * commandRunner.run(expectedCodesignCommand, _)

	}


	def "embed ProvisioningProfile"() {
		given:
		mockExampleApp()

		project.xcodebuild.signing.mobileProvisionFile = provisionProfile

		when:
		packageTask.packageApplication()
		File embedProvisioningProfile = new File(packageTask.outputPath, "/Example.app/Contents/embedded.provisionprofile")

		then:
		!embedProvisioningProfile.exists()

	}


	def "embed ProvisioningProfile with Framework"() {
		given:
		mockExampleApp("Contents/Frameworks/Sparkle.framework")

		project.xcodebuild.signing.mobileProvisionFile = provisionProfile

		when:
		packageTask.packageApplication()

		File embedProvisioningProfile = new File(packageTask.outputPath, "/Example.app/Contents/embedded.provisionprofile")

		then:
		!embedProvisioningProfile.exists()
	}




	List<String> getZipEntries(File file) {
		ZipFile zipFile = new ZipFile(file);

		List<String> entries = new ArrayList<String>()
		for (ZipEntry entry : zipFile.entries()) {
			entries.add(entry.getName())
		}
		return entries;
	}

	def "output file"() {
		List<String> zipEntries

		given:
		mockExampleApp("Contents/Frameworks/Sparkle.framework")
		project.xcodebuild.signing.mobileProvisionFile = provisionProfile

		when:
		packageTask.packageApplication()

		File outputFile = new File(packageTask.outputPath, "Example.zip")
		zipEntries = getZipEntries(outputFile)

		then:
		zipEntries.contains("Example.app/Example")
	}

}
