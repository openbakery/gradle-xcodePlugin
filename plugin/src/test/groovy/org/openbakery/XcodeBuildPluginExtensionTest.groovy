package org.openbakery

import ch.qos.logback.core.util.FileUtil
import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 24.07.14.
 */
class XcodeBuildPluginExtensionTest {


	Project project
	XcodeBuildPluginExtension extension;
	GMockController mockControl
	CommandRunner commandRunnerMock

	@BeforeMethod
	def setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)

		project = ProjectBuilder.builder().build()
		extension = new XcodeBuildPluginExtension(project)
	}

	@AfterMethod
	def cleanup() {
		FileUtils.deleteDirectory(new File("build/Platforms"))
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
			extension.createDeviceList(commandRunnerMock)
		}

		assert extension.availableDevices.size() == 14 : "expected 14 elements in the availableDevices list but was: " + extension.availableDevices.size()

	}



	@Test
	void testDestinationFilterPhone() {
		mockFindSimctl()
		mockSimctlList()



		mockControl.play {
			extension.createDeviceList(commandRunnerMock)
		}


		extension.destination {
			platform = 'iOS Simulator'
			name = 'iPhone 4s'
		}

		assert extension.destinations.size() == 2 : "expected 2 elements in the availableDevices list but was: " + extension.destinations.size()

		assert extension.destinations.asList()[0].id != null: "id of the destination should not be null"
	}

	@Test
	void testDestinationFilterPhoneiOS7() {
		mockFindSimctl()
		mockSimctlList()



		mockControl.play {
			extension.createDeviceList(commandRunnerMock)
		}


		extension.destination {
			platform = 'iOS Simulator'
			name = 'iPhone 4s'
			os = '7.0'
		}

		assert extension.destinations.size() == 1 : "expected 1 elements in the availableDevices list but was: " + extension.destinations.size()

		assert extension.destinations.asList()[0].id != null: "id of the destination should not be null"
	}

	@Test
	void testDestinationFilteriOS7() {
		mockFindSimctl()
		mockSimctlList()



		mockControl.play {
			extension.createDeviceList(commandRunnerMock)
		}


		extension.destination {
			platform = 'iOS Simulator'
			os = '7.0'
		}

		assert extension.destinations.size() == 6 : "expected 6 elements in the availableDevices list but was: " + extension.destinations.size()

		assert extension.destinations.asList()[0].id != null: "id of the destination should not be null"
	}



	@Test
	void testDestinationFilterPhoneWildcard() {
		mockFindSimctl()
		mockSimctlList()



		mockControl.play {
			extension.createDeviceList(commandRunnerMock)
		}


		extension.destination {
			platform = 'iOS Simulator'
			name = '.*iPhone.*'
		}

		assert extension.destinations.size() == 7 : "expected 7 elements in the availableDevices list but was: " + extension.destinations.size()

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
			extension.createDeviceList(commandRunnerMock)
		}

		assert extension.destinations.size() == 1 : "expected 1 elements in the availableDevices list but was: " + extension.destinations.size()


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
			extension.createXcode5DeviceList(commandRunnerMock)
		}

		assert extension.destinations.size() == 1 : "expected 1 elements in the availableDevices list but was: " + extension.destinations.size()

		Destination destination =  extension.destinations[0];

		assert destination.name.equals("iPad");
		assert destination.platform.equals("iOS Simulator")
		assert destination.os.equals("7.0")

	}


	@Test
	void testDeviceListXcode5_mutibleSimualtors() {

		mockXcodePath();
		mockDisplayName();
		mockNewerEquivalentDevice(null);

		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.0.sdk").mkdirs()
		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.1.sdk").mkdirs()
		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator6.0.sdk").mkdirs()

		mockControl.play {
			extension.createXcode5DeviceList(commandRunnerMock)
		}

		assert extension.destinations.size() == 3 : "expected 1 elements in the availableDevices list but was: " + extension.destinations.size()

		Destination destination =  extension.destinations[0];

		assert destination.name.equals("iPad");
		assert destination.platform.equals("iOS Simulator")
		assert destination.os.equals("7.0")

		assert extension.destinations[1].os.equals("7.1");
		assert extension.destinations[2].os.equals("6.0");

	}


	@Test
	void testDeviceListXcode5_skipDevice() {

		mockNewerEquivalentDevice("iPhone Retina (3.5-inch)");
		mockXcodePath();
		mockDisplayName();

		new File("build/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator6.0.sdk").mkdirs()

		mockControl.play {
			extension.createXcode5DeviceList(commandRunnerMock)
		}

		assert extension.destinations.size() == 0


	}
}
