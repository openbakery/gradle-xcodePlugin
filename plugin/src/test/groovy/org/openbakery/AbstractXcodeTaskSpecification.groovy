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

	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		xcodeTask = project.getTasks().getByPath(XcodePlugin.XCODE_CONFIG_TASK_NAME)

		xcodeTask.plistHelper = new PlistHelper(project, commandRunner)
	}


	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
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
}
