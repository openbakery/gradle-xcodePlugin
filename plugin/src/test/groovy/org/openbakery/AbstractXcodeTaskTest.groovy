package org.openbakery

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Created by rene on 01.12.14.
 */
class AbstractXcodeTaskTest {

	Project project
	AbstractXcodeTask xcodeTask;

	GMockController mockControl
	CommandRunner commandRunnerMock

	File projectDir

	@BeforeMethod
	def setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		xcodeTask = project.getTasks().getByPath(XcodePlugin.XCODE_CONFIG_TASK_NAME)

		xcodeTask.setProperty("commandRunner", commandRunnerMock)

	}


	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	@Test
	void getInfoListValue() {
		def commandList = ["/usr/libexec/PlistBuddy", "Info.plist", "-c", "Print :CFBundleIdentifier"]
		commandRunnerMock.runWithResult(commandList).returns("com.example.Example")

		String result;
		mockControl.play {
			result = xcodeTask.getValueFromPlist("Info.plist", "CFBundleIdentifier")
		}

		assert result.equals("com.example.Example");
	}



	@Test
	void getArrayFromInfoListValue() {
		def commandList = ["/usr/libexec/PlistBuddy", "Info.plist", "-c", "Print :CFBundleIdentifier"]
		commandRunnerMock.runWithResult(commandList).returns("Array {\n" +
						"    AppIcon29x29\n" +
						"    AppIcon40x40\n" +
						"    AppIcon57x57\n" +
						"    AppIcon60x60\n" +
						"}")

		def result;
		mockControl.play {
			result = xcodeTask.getValueFromPlist("Info.plist", "CFBundleIdentifier")
		}


		assert result instanceof List
		assert result.size() == 4
		assert result.get(0).equals("AppIcon29x29")
	}


	@Test
	void getInfoPlistValue_Missing() {
		def commandList = ["/usr/libexec/PlistBuddy", "Info.plist", "-c", "Print :CFBundleIconFiles"]
		commandRunnerMock.runWithResult(commandList).raises(new CommandRunnerException())

		def result;
		mockControl.play {
			result = xcodeTask.getValueFromPlist("Info.plist", "CFBundleIconFiles")
		}
		assert result == null
	}

	List<String> getZipEntries(File file) {
		ZipFile zipFile = new ZipFile(file);

		List<String> entries = new ArrayList<String>()
		for (ZipEntry entry : zipFile.entries()) {
			entries.add(entry.getName())
		}
		return entries;
	}

	@Test
	void testZip() {
		File tmpFile = new File(projectDir, "FileToZip.txt")
		FileUtils.writeStringToFile(tmpFile, "dummy")

		xcodeTask.createZip(tmpFile)

		File zipFile = new File(projectDir, "FileToZip.zip")

		assert zipFile.exists()

		List<String> zipEntries = getZipEntries(zipFile);

		assert zipEntries.contains("FileToZip.txt")


	}

	@Test
	void testZipDirectory() {
		File tmpDirectory = new File(projectDir, "DirectroyToZip")
		tmpDirectory.mkdirs()

		FileUtils.writeStringToFile(new File(tmpDirectory, "first.txt"), "dummy")
		FileUtils.writeStringToFile(new File(tmpDirectory, "second.txt"), "dummy")


		File zipFile = new File(projectDir, "Test.zip")

		xcodeTask.createZip(zipFile, zipFile.parentFile,  tmpDirectory)

		assert zipFile.exists()

		List<String> zipEntries = getZipEntries(zipFile);

		assert zipEntries.contains("DirectroyToZip/first.txt")
		assert zipEntries.contains("DirectroyToZip/second.txt")

	}



	@Test
	void testZipMultipleFiles() {
		File firstFile = new File(projectDir, "first.txt")
		FileUtils.writeStringToFile(firstFile, "dummy")
		File secondFile = new File(projectDir, "second.txt")
		FileUtils.writeStringToFile(secondFile, "dummy")

		File zipFile = new File(projectDir, "MultipleTest.zip")

		xcodeTask.createZip(zipFile, zipFile.parentFile,  firstFile, secondFile)

		assert zipFile.exists()

		List<String> zipEntries = getZipEntries(zipFile);

		assert zipEntries.contains("first.txt")
		assert zipEntries.contains("second.txt")


	}
}
