package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.util.PlistHelper
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipFile


/**
 * Created by rene on 01.12.14.
 */
class AbstractXcodeTaskSpecification extends Specification{

	Project project
	AbstractXcodeTask xcodeTask;

	CommandRunner commandRunner = Mock(CommandRunner)

	File projectDir

	File xcode7_3_1
	File xcode8


	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		xcodeTask = project.getTasks().getByPath(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		xcodeTask.commandRunner = commandRunner

		xcodeTask.plistHelper = new PlistHelper(project, commandRunner)
	}


	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)

		if (xcode7_3_1 != null) {
			FileUtils.deleteDirectory(xcode7_3_1)
		}
		if (xcode8 != null) {
			FileUtils.deleteDirectory(xcode8)
		}

	}


	def getInfoListValue() {
		given:
		def commandList = ["/usr/libexec/PlistBuddy", "Info.plist", "-c", "Print :CFBundleIdentifier"]
		commandRunner.runWithResult(commandList) >> "com.example.Example"

		when:
		String result;
		result = xcodeTask.plistHelper.getValueFromPlist("Info.plist", "CFBundleIdentifier")

		then:
		result.equals("com.example.Example");
	}

	def "getArrayFromInfoListValue"() {
		given:
		def commandList = ["/usr/libexec/PlistBuddy", "Info.plist", "-c", "Print :CFBundleIdentifier"]
		commandRunner.runWithResult(commandList) >> "Array {\n" +
						"    AppIcon29x29\n" +
						"    AppIcon40x40\n" +
						"    AppIcon57x57\n" +
						"    AppIcon60x60\n" +
						"}"

		when:
		def result = xcodeTask.plistHelper.getValueFromPlist("Info.plist", "CFBundleIdentifier")

		then:
		result instanceof List
		result.size() == 4
		result.get(0).equals("AppIcon29x29")
	}


	def "InfoPlist Value Missing"() {
		given:
		def commandList = ["/usr/libexec/PlistBuddy", "Info.plist", "-c", "Print :CFBundleIconFiles"]
		commandRunner.runWithResult(commandList) >> { throw new CommandRunnerException() }

		when:
		def result = xcodeTask.plistHelper.getValueFromPlist("Info.plist", "CFBundleIconFiles")

		then:
		result == null
	}


	List<String> getZipEntries(File file) {
		ZipFile zipFile = new ZipFile(file);

		List<String> entries = new ArrayList<String>()
		for (ZipEntry entry : zipFile.entries()) {
			entries.add(entry.getName())
		}
		return entries;
	}

	def "zip"() {
		given:
		File tmpFile = new File(projectDir, "FileToZip.txt")
		FileUtils.writeStringToFile(tmpFile, "dummy")

		when:
		xcodeTask.createZip(tmpFile)

		File zipFile = new File(projectDir, "FileToZip.zip")
		List<String> zipEntries = getZipEntries(zipFile);

		then:
		zipEntries.contains("FileToZip.txt")


	}

	def "Zip Directory"() {
		given:
		File tmpDirectory = new File(projectDir, "DirectroyToZip")
		tmpDirectory.mkdirs()

		FileUtils.writeStringToFile(new File(tmpDirectory, "first.txt"), "dummy")
		FileUtils.writeStringToFile(new File(tmpDirectory, "second.txt"), "dummy")


		File zipFile = new File(projectDir, "Test.zip")

		when:
		xcodeTask.createZip(zipFile, zipFile.parentFile,  tmpDirectory)

		List<String> zipEntries = getZipEntries(zipFile);

		then:
		zipEntries.contains("DirectroyToZip/first.txt")
		zipEntries.contains("DirectroyToZip/second.txt")

	}


	def "Zip Multiple Files"() {
		given:
		File firstFile = new File(projectDir, "first.txt")
		FileUtils.writeStringToFile(firstFile, "dummy")
		File secondFile = new File(projectDir, "second.txt")
		FileUtils.writeStringToFile(secondFile, "dummy")

		File zipFile = new File(projectDir, "MultipleTest.zip")

		when:
		xcodeTask.createZip(zipFile, zipFile.parentFile,  firstFile, secondFile)

		List<String> zipEntries = getZipEntries(zipFile);

		then:
		zipEntries.contains("first.txt")
		zipEntries.contains("second.txt")

	}


	def "create xcode lazy"() {
		given:

		xcode7_3_1 = new File(System.getProperty("java.io.tmpdir"), "Xcode-7.3.1.app")
		xcode8 = new File(System.getProperty("java.io.tmpdir"), "Xcode-8.app")

		new File(xcode7_3_1, "Contents/Developer/usr/bin").mkdirs()
		new File(xcode8, "Contents/Developer/usr/bin").mkdirs()
		new File(xcode7_3_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcode8, "Contents/Developer/usr/bin/xcodebuild").createNewFile()


		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >>  xcode7_3_1.absolutePath + "\n"  + xcode8.absolutePath
		commandRunner.runWithResult(xcode7_3_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 7.3.1\nBuild version 7D1014"
		commandRunner.runWithResult(xcode8.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 8.0\nBuild version 8A218a"
		commandRunner.runWithResult("xcodebuild", "-version") >> "Xcode 8.0\nBuild version 8A218a"

		when:
		project.xcodebuild.version = "7"

		then:
		xcodeTask.xcode != null
		xcodeTask.xcode.version.major == 7

	}
}
