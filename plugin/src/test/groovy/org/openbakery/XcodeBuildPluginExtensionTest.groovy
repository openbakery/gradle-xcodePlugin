package org.openbakery
import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static java.util.Arrays.asList

/**
 * Created by rene on 24.07.14.
 */
class XcodeBuildPluginExtensionTest {


	Project project
	XcodeBuildPluginExtension extension;
	GMockController mockControl
	CommandRunner commandRunnerMock

	File xcodebuild6_1
	File xcodebuild6_0
	File xcodebuild5_1

	@BeforeMethod
	def setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)

		File projectDir =  new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()


		extension = new XcodeBuildPluginExtension(project)
		extension.commandRunner = commandRunnerMock
		extension.infoPlist = "Info.plist";


		xcodebuild6_1 = new File(System.getProperty("java.io.tmpdir"), "Xcode6-1.app")
		xcodebuild6_0 = new File(System.getProperty("java.io.tmpdir"), "Xcode6.app")
		xcodebuild5_1 = new File(System.getProperty("java.io.tmpdir"), "Xcode5.app")

		new File(xcodebuild6_1, "Contents/Developer/usr/bin").mkdirs()
		new File(xcodebuild6_0, "Contents/Developer/usr/bin").mkdirs()
		new File(xcodebuild5_1, "Contents/Developer/usr/bin").mkdirs()

		new File(xcodebuild6_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcodebuild6_0, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcodebuild5_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()


	}

	@AfterMethod
	def cleanup() {
		FileUtils.deleteDirectory(xcodebuild6_1)
		FileUtils.deleteDirectory(xcodebuild6_0)
		FileUtils.deleteDirectory(xcodebuild5_1)
		File projectDir =  new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		FileUtils.deleteDirectory(projectDir)
	}




	@Test
	void xcodeVersion() {

		commandRunnerMock.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode").returns( xcodebuild5_1.absolutePath + "\n"  + xcodebuild6_0.absolutePath + "\n" + xcodebuild6_1.absolutePath).times(1)

		commandRunnerMock.runWithResult(xcodebuild5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version").returns("Xcode 5.1.1\nBuild version 5B1008").times(1)



		mockControl.play {
			extension.version = '5B1008';
		}

		assert extension.getXcodebuildCommand().endsWith("Xcode5.app/Contents/Developer/usr/bin/xcodebuild")
	}


	@Test
	void xcodeVersion_select_last() {


		commandRunnerMock.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode").returns( xcodebuild6_1.absolutePath + "\n"  + xcodebuild6_0.absolutePath + "\n" + xcodebuild5_1.absolutePath).times(1)


		commandRunnerMock.runWithResult(xcodebuild6_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version").returns("Xcode 6.0\nBuild version 6A000").times(1)
		commandRunnerMock.runWithResult(xcodebuild6_0.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version").returns("Xcode 6.0\nBuild version 6A000").times(1)
		commandRunnerMock.runWithResult(xcodebuild5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version").returns("Xcode 5.1.1\nBuild version 5B1008").times(1)



		mockControl.play {
			extension.version = '5B1008';
		}

		assert extension.getXcodebuildCommand().endsWith("Xcode5.app/Contents/Developer/usr/bin/xcodebuild")
	}


	@Test(expectedExceptions = IllegalStateException.class)
	void xcodeVersion_select_not_found() {

		commandRunnerMock.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode").returns( xcodebuild6_1.absolutePath + "\n"  + xcodebuild6_0.absolutePath + "\n" + xcodebuild5_1.absolutePath).times(1)


		commandRunnerMock.runWithResult(xcodebuild6_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version").returns("Xcode 6.0\nBuild version 6A000").times(1)
		commandRunnerMock.runWithResult(xcodebuild6_0.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version").returns("Xcode 6.0\nBuild version 6A000").times(1)
		commandRunnerMock.runWithResult(xcodebuild5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version").returns("Xcode 5.1.1\nBuild version 5B1008").times(1)


		mockControl.play {
			extension.version = '5B1009';
		}
	}


	@Test
	void testWorkspaceNil() {
		assert extension.workspace == null;
	}

	@Test
	void testWorkspace() {

		File workspace = new File(project.projectDir , "Test.xcworkspace")
		workspace.mkdirs()
		assert extension.workspace == "Test.xcworkspace";

	}



	@Test
	void testApplicationBundleForWidget() {
		extension.commandRunner = new CommandRunner()
		File projectDir =  new File("../example/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)
		extension.sdk = "iphoneos"
		extension.productName = "ExampleTodayWidget"
		extension.productType = "appex"
		extension.infoPlist = "../../example/Example/ExampleTodayWidget/Info.plist"

		String applicationBundle = extension.getApplicationBundle().absolutePath;
		assert applicationBundle.endsWith("build/sym/Debug-iphoneos/ExampleTodayWidget.appex")

	}

	@Test
	void testApplicationBundle() {
		File projectDir =  new File("../example/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)
		extension.commandRunner = new CommandRunner()

		extension.sdk = "iphoneos"
		extension.target = "Example"
		extension.productName = "Example"
		extension.infoPlist = "../../example/Example/Example/Example-Info.plist"

		String applicationBundle = extension.getApplicationBundle().absolutePath;
		assert applicationBundle.endsWith("build/sym/Debug-iphoneos/Example.app")

	}

	/*
	@Test
	void testIpaBundleForIpa() {

		extension.commandRunner = new CommandRunner()

		File projectDir =  new File("../example/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)
		extension.sdk = "iphoneos"
		extension.productName = "ExampleTodayWidget"
		extension.infoPlist = "../../example/Example/ExampleTodayWidget/Info.plist"

		String ipaBundle = extension.getIpaBundle().absolutePath;
		//assert ipaBundle.endsWith("build/sym/Debug-iphoneos/ExampleTodayWidget.ipa")
		assert ipaBundle.endsWith("package/debug/ExampleTodayWidget.ipa")

	}
*/

	void mockValueFromPlist(String key, String value) {
		File infoPlist = new File(project.projectDir, "Info.plist")

		def commandList = ["/usr/libexec/PlistBuddy", infoPlist.absolutePath, "-c", "Print :" + key]
		commandRunnerMock.runWithResult(commandList).returns(value).atLeastOnce()
	}



	@Test
	void testDefaultBundleNameEmpty() {
		extension.productName = "TestApp1"

		mockValueFromPlist("CFBundleName", "");

		mockControl.play {
			String bundleName = extension.getBundleName();
			assert bundleName.equals("TestApp1")
		}
	}

	@Test
	void testDefaultBundleNameValue() {

		mockValueFromPlist("CFBundleName", "TestApp2");

		mockControl.play {
			String bundleName = extension.getBundleName();
			assert bundleName.equals("TestApp2")
		}
	}


	@Test
	void xcodeVersionSimple() {

		commandRunnerMock.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode").returns( xcodebuild5_1.absolutePath + "\n"  + xcodebuild6_0.absolutePath + "\n" + xcodebuild6_1.absolutePath).times(1)

		commandRunnerMock.runWithResult(xcodebuild5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version").returns("Xcode 5.1.1\nBuild version 5B1008").times(1)

		mockControl.play {
			extension.version = '5.1';
		}

		assert extension.getXcodebuildCommand().endsWith("Xcode5.app/Contents/Developer/usr/bin/xcodebuild")
	}

	@Test
	void xcodeVersionSimple_1() {

		commandRunnerMock.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode").returns(xcodebuild5_1.absolutePath + "\n" + xcodebuild6_0.absolutePath + "\n" + xcodebuild6_1.absolutePath).times(1)

		commandRunnerMock.runWithResult(xcodebuild5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version").returns("Xcode 5.1.1\nBuild version 5B1008").times(1)

		mockControl.play {
			extension.version = '5.1.1';
		}

		assert extension.getXcodebuildCommand().endsWith("Xcode5.app/Contents/Developer/usr/bin/xcodebuild")
	}

	@Test(expectedExceptions = IllegalStateException.class)
	void xcodeVersionSimple_not_found() {

		commandRunnerMock.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode").returns(xcodebuild6_1.absolutePath + "\n" + xcodebuild6_0.absolutePath + "\n" + xcodebuild5_1.absolutePath).times(1)


		commandRunnerMock.runWithResult(xcodebuild6_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version").returns("Xcode 6.0\nBuild version 6A000").times(1)
		commandRunnerMock.runWithResult(xcodebuild6_0.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version").returns("Xcode 6.0\nBuild version 6A000").times(1)
		commandRunnerMock.runWithResult(xcodebuild5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version").returns("Xcode 5.1.1\nBuild version 5B1008").times(1)


		mockControl.play {
			extension.version = '5.1.2';
		}
	}

	@Test
	void testXcodePath_notSet() {

		extension.xcodePath = null

		commandRunnerMock.runWithResult("xcode-select", "-p").returns("/Applications/Xcode.app/Contents/Developer").times(1)

		mockControl.play {
			assert extension.xcodePath.equals("/Applications/Xcode.app")
		}
	}

}
