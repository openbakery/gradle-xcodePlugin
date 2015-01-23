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
		FileUtils.deleteDirectory(new File("build/Platforms"))
		FileUtils.deleteDirectory(xcodebuild6_1)
		FileUtils.deleteDirectory(xcodebuild6_0)
		FileUtils.deleteDirectory(xcodebuild5_1)
		FileUtils.deleteDirectory(project.projectDir)
	}




	void mockFindSimctl() {
		def commandList = ["xcrun", "-sdk", "iphoneos", "-find", "simctl"]
		commandRunnerMock.runWithResult(commandList).returns("/Applications/Xcode.app/Contents/Developer/usr/bin/simctl").times(1)
	}

	void mockSimctlList() {
		def commandList = ["/Applications/Xcode.app/Contents/Developer/usr/bin/simctl", "list"]

		String simctlOutput = FileUtils.readFileToString(new File("src/test/Resource/simctl-output.txt"))

		commandRunnerMock.runWithResult(commandList).returns(simctlOutput).times(1)
	}



	@Test
	void testCreateDeviceList_parseDevices() {

		mockFindSimctl()
		mockSimctlList()

		mockControl.play {
			extension.createDeviceList()
		}

		assert extension.availableSimulators.size() == 14 : "expected 14 elements in the availableSimulators list but was: " + extension.availableSimulators.size()

	}



	@Test
	void testDestinationFilterPhone() {
		mockFindSimctl()
		mockSimctlList()



		mockControl.play {
			extension.createDeviceList()
		}


		extension.destination {
			platform = 'iOS Simulator'
			name = 'iPhone 4s'
		}

		assert extension.destinations.size() == 2 : "expected 2 elements in the availableSimulators list but was: " + extension.destinations.size()

		assert extension.destinations.asList()[0].id != null: "id of the destination should not be null"
	}

	@Test
	void testDestinationFilterPhoneiOS7() {
		mockFindSimctl()
		mockSimctlList()



		mockControl.play {
			extension.createDeviceList()
		}


		extension.destination {
			platform = 'iOS Simulator'
			name = 'iPhone 4s'
			os = '7.0'
		}

		assert extension.destinations.size() == 1 : "expected 1 elements in the availableSimulators list but was: " + extension.destinations.size()

		assert extension.destinations.asList()[0].id != null: "id of the destination should not be null"
	}

	@Test
	void testDestinationFilteriOS7() {
		mockFindSimctl()
		mockSimctlList()



		mockControl.play {
			extension.createDeviceList()
		}


		extension.destination {
			platform = 'iOS Simulator'
			os = '7.0'
		}

		assert extension.destinations.size() == 6 : "expected 6 elements in the availableSimulators list but was: " + extension.destinations.size()

		assert extension.destinations.asList()[0].id != null: "id of the destination should not be null"
	}



	@Test
	void testDestinationFilterPhoneWildcard() {
		mockFindSimctl()
		mockSimctlList()



		mockControl.play {
			extension.createDeviceList()
		}


		extension.destination {
			platform = 'iOS Simulator'
			name = '.*iPhone.*'
		}

		assert extension.destinations.size() == 7 : "expected 7 elements in the availableSimulators list but was: " + extension.destinations.size()

		assert extension.destinations.asList()[0].id != null: "id of the destination should not be null"
	}


	@Test
	void testDestinations_iPhoneOS_Build() {

		mockFindSimctl()
		mockSimctlList()

		extension.sdk = 'iphoneos'

		extension.destination {
			platform = 'iphoneos'
			name = 'iPhone 5s'
			id = '60B5BBDA-6485-44B4-AB87-9C0421EF5D8F'
		}



		mockControl.play {
			extension.createDeviceList()
		}

		assert extension.destinations.size() == 1 : "expected 1 elements in the availableSimulators list but was: " + extension.destinations.size()


		assert extension.destinations.asList()[0].id.equals("60B5BBDA-6485-44B4-AB87-9C0421EF5D8F")

	}


	void mockDisplayName() {
		def commandList = [
											"/usr/libexec/PlistBuddy",
											new File("build/Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks/SimulatorHost.framework/Versions/A/Resources/Devices/iPad/Info.plist").getAbsolutePath(),
											"-c",
											"Print :displayName"
							]
		commandRunnerMock.runWithResult(commandList).returns("iPad").times(1)


	}

	void mockNewerEquivalentDevice(String result) {
		def commandList = [
						"/usr/libexec/PlistBuddy",
						new File("build/Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks/SimulatorHost.framework/Versions/A/Resources/Devices/iPad/Info.plist").getAbsolutePath(),
						"-c",
						"Print :newerEquivalentDevice"
		]

		if (result == null) {
			commandRunnerMock.runWithResult(commandList).raises(new CommandRunnerException(""));
		} else {
			commandRunnerMock.runWithResult(commandList).returns(result)
		}


	}


	void mockXcodePath() {
		def commandList = ["xcode-select", "-p"]
		commandRunnerMock.runWithResult(commandList).returns("build").times(1)

		File simulatorDirectory = new File("build/Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks/SimulatorHost.framework/Versions/A/Resources/Devices/iPad");
		simulatorDirectory.mkdirs()
	}



	@Test
	void testDeviceListXcode5() {

		mockXcodePath();
		mockDisplayName();
		mockNewerEquivalentDevice(null);

		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.0.sdk").mkdirs()


		mockControl.play {
			extension.createXcode5DeviceList()
		}

		assert extension.destinations.size() == 1 : "expected 1 elements in the availableSimulators list but was: " + extension.destinations.size()

		Destination destination =  extension.destinations[0];

		assert destination.name.equals("iPad");
		assert destination.platform.equals("iOS Simulator")
		assert destination.os.equals("7.0")

	}


	@Test
	void testDeviceListXcode5_multipleSimulators() {
		mockXcodePath();
		mockDisplayName();
		mockNewerEquivalentDevice(null);

		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.0.sdk").mkdirs()
		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.1.sdk").mkdirs()
		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator6.0.sdk").mkdirs()

		mockControl.play {
			extension.createXcode5DeviceList()
		}

		assert extension.destinations.size() == 3 : "expected 3 elements in the availableSimulators list but was: " + extension.destinations.size()
		assert extension.destinations.containsAll(asList(iPadOnSimulatorWith("7.0"), iPadOnSimulatorWith("7.1"), iPadOnSimulatorWith("6.0")))
	}


	@Test
	void testDeviceListXcode5_skipDevice() {

		mockNewerEquivalentDevice("iPhone Retina (3.5-inch)");
		mockXcodePath();
		mockDisplayName();

		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator6.0.sdk").mkdirs()

		mockControl.play {
			extension.createXcode5DeviceList()
		}

		assert extension.destinations.size() == 0


	}


	@Test
	void testDestinationFilter_PhoneAndPad() {
		mockFindSimctl()
		mockSimctlList()

		mockControl.play {
			extension.createDeviceList()
		}


		extension.destination {
			platform = 'iOS Simulator'
			name = 'iPad 2'
			os = '7.0'
		}

		extension.destination {
			platform = 'iOS Simulator'
			name = 'iPhone 4s'
			os = '7.0'
		}


		assert extension.destinations.size() == 2 : "expected 2 elements in the availableSimulators list but was: " + extension.destinations.size()

		assert extension.destinations.asList()[0].id != null: "id of the destination should not be null"
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

	private static Destination iPadOnSimulatorWith(String osVersion) {
		return new Destination("iOS Simulator", "iPad", osVersion)
	}

}
