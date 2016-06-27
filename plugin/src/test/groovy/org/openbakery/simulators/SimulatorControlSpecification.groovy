package org.openbakery.simulators

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.Destination
import org.openbakery.Type
import org.openbakery.Version
import spock.lang.Specification

/**
 * Created by rene on 07.09.15.
 */
class SimulatorControlSpecification extends Specification {

	Project project
	File projectDir
	SimulatorControl simulatorControl
	CommandRunner commandRunner = Mock(CommandRunner);

	def SIMCTL = "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"

	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin:org.openbakery.XcodePlugin


		projectDir.mkdirs()

		simulatorControl = new SimulatorControl(project, commandRunner)

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	void mockXcode6() {
		commandRunner.runWithResult(["/Applications/Xcode.app/Contents/Developer/usr/bin/xcrun", "-sdk", "iphoneos", "-find", "simctl"]) >> "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
		commandRunner.runWithResult([SIMCTL, "list"]) >> FileUtils.readFileToString(new File("src/test/Resource/simctl-list-unavailable.txt"))
	}

	void mockXcode7() {
		commandRunner.runWithResult(["/Applications/Xcode.app/Contents/Developer/usr/bin/xcrun", "-sdk", "iphoneos", "-find", "simctl"]) >> "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
		commandRunner.runWithResult([SIMCTL, "list"]) >> FileUtils.readFileToString(new File("src/test/Resource/simctl-list-xcode7.txt"))
	}

	def "get runtimes"() {
		given:
		mockXcode6()


		expect:
		List<Runtime> runtimes = simulatorControl.getRuntimes()

		runtimes != null
		runtimes.size() == 2


		SimulatorRuntime runtime = runtimes.get(0)
		runtime.name.equals("iOS 8.2")
		runtime.version.toString().equals("8.2")
		runtime.buildNumber.equals("12D508")
		runtime.identifier.equals("com.apple.CoreSimulator.SimRuntime.iOS-8-2")

		SimulatorRuntime runtime1 = runtimes.get(1)
		runtime1.name.equals("iOS 7.1")
		runtime1.version.toString().equals("7.1")
		runtime1.buildNumber.equals("11D167")
		runtime1.identifier.equals("com.apple.CoreSimulator.SimRuntime.iOS-7-1")
	}


	def "get most recent iOS runtime"() {

		given:
		mockXcode7()

		when:
		SimulatorRuntime runtime = simulatorControl.getMostRecentRuntime(Type.iOS)

		then:
		runtime != null
		runtime.version == new Version("9.0")
	}

	def "get most recent iOS runtime 7.1"() {

		given:
		commandRunner.runWithResult(["/Applications/Xcode.app/Contents/Developer/usr/bin/xcrun", "-sdk", "iphoneos", "-find", "simctl"]) >> "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
		commandRunner.runWithResult([SIMCTL, "list"]) >> FileUtils.readFileToString(new File("src/test/Resource/simctl-list-xcode7_1.txt"))

		when:
		SimulatorRuntime runtime = simulatorControl.getMostRecentRuntime(Type.iOS)

		then:
		runtime != null
		runtime.version == new Version("9.1")
	}

	def "get most recent tvOS runtime 7.1"() {

		given:
		commandRunner.runWithResult(["/Applications/Xcode.app/Contents/Developer/usr/bin/xcrun", "-sdk", "iphoneos", "-find", "simctl"]) >> "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
		commandRunner.runWithResult([SIMCTL, "list"]) >> FileUtils.readFileToString(new File("src/test/Resource/simctl-list-xcode7_1.txt"))

		when:
		SimulatorRuntime runtime = simulatorControl.getMostRecentRuntime(Type.tvOS)

		then:
		runtime != null
		runtime.version == new Version("9.0")
	}

	def "launch simulator"() {
		given:
		mockXcode6()

		when:
		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(0));
		simulatorControl.runDevice(devices.get(0))

		then:
		1 * commandRunner.run(["/Applications/Xcode.app/Contents/Developer/usr/bin/instruments", "-w", "E06E8144-D4AB-4616-A19E-9A489FB0CC17"])

	}


	def "device types"() {
		given:
		mockXcode6()

		when:
		List<SimulatorDeviceType> deviceTypes = simulatorControl.getDeviceTypes()

		then:
		deviceTypes != null
		deviceTypes.size() == 10
		deviceTypes.get(0).name.equals("iPhone 4s")
		deviceTypes.get(0).identifier.equals("com.apple.CoreSimulator.SimDeviceType.iPhone-4s")
	}



	def "devices iOS7"() {
		given:
		mockXcode6()

		when:
		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(1));

		then:
		devices != null
		devices.size() == 6
		devices.get(0).name.equals("iPhone 4s")
		devices.get(0).identifier.equals("73C126C8-FD53-44EA-80A3-84F5F19508C0")
		devices.get(0).state.equals("Shutdown")
	}



	def "devices iOS8"() {
		given:
		mockXcode6()

		when:
		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(0));

		then:
		devices != null
		devices.size() == 10
		devices.get(9).name.equals("Resizable iPad")
		devices.get(9).identifier.equals("50D9CBF1-608C-4866-9B5F-234D7FACBC16")
		devices.get(9).state.equals("Shutdown")
	}




	def "delete All"() {
		given:
		mockXcode6()

		when:
		simulatorControl.deleteAll()

		then:
		1* commandRunner.runWithResult([SIMCTL, "delete", "73C126C8-FD53-44EA-80A3-84F5F19508C0"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "15F68098-3B21-411D-B553-1C3161C100E7"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "545260B4-C6B8-4D3A-9348-AD3B882D8D17"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "454F3900-7B07-422E-A731-D46C821888B5"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "F60A8735-97D9-48A8-9728-3CC53394F7FC"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "B8278DAC-97EE-4097-88CA-5650960882A5"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "E06E8144-D4AB-4616-A19E-9A489FB0CC17"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "0560469A-813F-4AF7-826C-4598802A7FFD"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "F029A31F-3CBF-422D-AEF4-D05675BAEDEF"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "6F2558A0-A789-443B-B142-7BA707E3C9E8"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "075026D3-C77E-40F9-944C-EBCB565E17D5"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "5C4434E1-81AC-4448-8237-26029A57E594"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "E85B0A4D-6B82-4F7C-B4CF-3C00E4EFF3D1"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "A7400DB8-CDF3-4E6F-AF87-EB2B296D82C5"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "29C34492-7006-41D7-B634-8703972F725C"])
		1* commandRunner.runWithResult([SIMCTL, "delete", "50D9CBF1-608C-4866-9B5F-234D7FACBC16"])
	}


	def "create All"() {
		given:
		mockXcode6()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 4s", "com.apple.CoreSimulator.SimDeviceType.iPhone-4s", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 5", "com.apple.CoreSimulator.SimDeviceType.iPhone-5", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 5s", "com.apple.CoreSimulator.SimDeviceType.iPhone-5s", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 6 Plus", "com.apple.CoreSimulator.SimDeviceType.iPhone-6-Plus", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 6", "com.apple.CoreSimulator.SimDeviceType.iPhone-6", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPad 2", "com.apple.CoreSimulator.SimDeviceType.iPad-2", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPad Retina", "com.apple.CoreSimulator.SimDeviceType.iPad-Retina", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPad Air", "com.apple.CoreSimulator.SimDeviceType.iPad-Air", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "Resizable iPhone", "com.apple.CoreSimulator.SimDeviceType.Resizable-iPhone", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "Resizable iPad", "com.apple.CoreSimulator.SimDeviceType.Resizable-iPad", "com.apple.CoreSimulator.SimRuntime.iOS-7-1"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 4s", "com.apple.CoreSimulator.SimDeviceType.iPhone-4s", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 5", "com.apple.CoreSimulator.SimDeviceType.iPhone-5", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 5s", "com.apple.CoreSimulator.SimDeviceType.iPhone-5s", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 6 Plus", "com.apple.CoreSimulator.SimDeviceType.iPhone-6-Plus", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 6", "com.apple.CoreSimulator.SimDeviceType.iPhone-6", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPad 2", "com.apple.CoreSimulator.SimDeviceType.iPad-2", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPad Retina", "com.apple.CoreSimulator.SimDeviceType.iPad-Retina", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPad Air", "com.apple.CoreSimulator.SimDeviceType.iPad-Air", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "Resizable iPhone", "com.apple.CoreSimulator.SimDeviceType.Resizable-iPhone", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "Resizable iPad", "com.apple.CoreSimulator.SimDeviceType.Resizable-iPad", "com.apple.CoreSimulator.SimRuntime.iOS-8-2"])

	}


	def "erase All"() {
		given:
		mockXcode6()

		when:
		simulatorControl.eraseAll()

		then:
		1 * commandRunner.runWithResult([SIMCTL, "erase", "73C126C8-FD53-44EA-80A3-84F5F19508C0"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "15F68098-3B21-411D-B553-1C3161C100E7"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "545260B4-C6B8-4D3A-9348-AD3B882D8D17"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "454F3900-7B07-422E-A731-D46C821888B5"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "F60A8735-97D9-48A8-9728-3CC53394F7FC"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "B8278DAC-97EE-4097-88CA-5650960882A5"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "E06E8144-D4AB-4616-A19E-9A489FB0CC17"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "0560469A-813F-4AF7-826C-4598802A7FFD"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "F029A31F-3CBF-422D-AEF4-D05675BAEDEF"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "6F2558A0-A789-443B-B142-7BA707E3C9E8"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "075026D3-C77E-40F9-944C-EBCB565E17D5"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "5C4434E1-81AC-4448-8237-26029A57E594"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "E85B0A4D-6B82-4F7C-B4CF-3C00E4EFF3D1"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "A7400DB8-CDF3-4E6F-AF87-EB2B296D82C5"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "29C34492-7006-41D7-B634-8703972F725C"])
		1 * commandRunner.runWithResult([SIMCTL, "erase", "50D9CBF1-608C-4866-9B5F-234D7FACBC16"])

	}

	def launchSimulator() {
		given:
		mockXcode6()

		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(0));

		when:
		simulatorControl.runDevice(devices.get(0))


		then:
		1 * commandRunner.run(["/Applications/Xcode.app/Contents/Developer/usr/bin/instruments", "-w", "E06E8144-D4AB-4616-A19E-9A489FB0CC17"])
	}


	def "devices iOS9"() {
		given:
		mockXcode7()

		when:
		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(0));

		then:
		runtimes != null
		runtimes.size() == 2
		runtimes.get(0).name.equals("iOS 9.0")

		devices != null
		devices.size() == 11
		devices.get(0).name.equals("iPhone 4s")
		devices.get(0).identifier.equals("5C8E1FF3-47B7-48B8-96E9-A12740DBC58A")
		devices.get(0).state.equals("Shutdown")
	}

	def "devices iOS9.1"() {
		given:
		commandRunner.runWithResult(["/Applications/Xcode.app/Contents/Developer/usr/bin/xcrun", "-sdk", "iphoneos", "-find", "simctl"]) >> "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
		commandRunner.runWithResult([SIMCTL, "list"]) >> FileUtils.readFileToString(new File("src/test/Resource/simctl-list-xcode7_1.txt"))

		when:
		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(0));

		then:
		runtimes != null
		runtimes.size() == 4
		runtimes.get(0).name.equals("iOS 9.1")

		devices != null
		devices.size() == 12
		devices.get(11).name.equals("iPad Pro")
		devices.get(11).identifier.equals("744F7B28-373D-4666-B4DF-8438D1109663")
		devices.get(11).state.equals("Shutdown")
	}


	def "create all iOS9"() {
		given:
		mockXcode7()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 4s", "com.apple.CoreSimulator.SimDeviceType.iPhone-4s", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 5", "com.apple.CoreSimulator.SimDeviceType.iPhone-5", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 5s", "com.apple.CoreSimulator.SimDeviceType.iPhone-5s", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 6 Plus", "com.apple.CoreSimulator.SimDeviceType.iPhone-6-Plus", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 6", "com.apple.CoreSimulator.SimDeviceType.iPhone-6", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 6s Plus", "com.apple.CoreSimulator.SimDeviceType.iPhone-6s-Plus", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPhone 6s", "com.apple.CoreSimulator.SimDeviceType.iPhone-6s", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPad 2", "com.apple.CoreSimulator.SimDeviceType.iPad-2", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPad Retina", "com.apple.CoreSimulator.SimDeviceType.iPad-Retina", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPad Air", "com.apple.CoreSimulator.SimDeviceType.iPad-Air", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "iPad Air 2", "com.apple.CoreSimulator.SimDeviceType.iPad-Air-2", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])

		1 * commandRunner.runWithResult([SIMCTL, "create", "Apple Watch - 38mm", "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-38mm", "com.apple.CoreSimulator.SimRuntime.watchOS-2-0"])
		1 * commandRunner.runWithResult([SIMCTL, "create", "Apple Watch - 42mm", "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-42mm", "com.apple.CoreSimulator.SimRuntime.watchOS-2-0"])

	}

	def "pair iPhone and Watch"() {
		given:
		mockXcode7()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.runWithResult([SIMCTL, "pair", "86895139-2FA4-4E97-A91A-C088A02F7BCD", "FE4BE76C-A3A1-4FB0-8BD4-7B87B4ACEDB2"])
		1 * commandRunner.runWithResult([SIMCTL, "pair", "2A40D83C-EF8E-46AB-9C50-7DA01DA0B01F", "6F866EE0-55E8-439C-95F4-3FF19DAF553F"])

	}


	def "device pairs"() {
		given:
		mockXcode7()

		when:
		List<SimulatorDevicePair> pairs = simulatorControl.getDevicePairs();

		then:
		pairs.size() == 2
		pairs.get(0).identifier == "291E69F5-A889-47EA-87FA-7581E610E570"
		pairs.get(1).identifier == "E42CA2D8-FCDF-4D70-9640-1C265045A32B"

		pairs.get(0).watch != null
		pairs.get(0).phone != null

		pairs.get(0).watch.identifier == "FE4BE76C-A3A1-4FB0-8BD4-7B87B4ACEDB2"
		pairs.get(0).phone.identifier == "86895139-2FA4-4E97-A91A-C088A02F7BCD"


	}


	def "get device for destination"() {
		given:
		mockXcode7()
		Destination destination = new Destination()
		destination.name = "iPhone 4s"
		destination.platform = 'iOS Simulator'
		destination.os = '9.0'

		when:
		SimulatorDevice device = simulatorControl.getDevice(destination)

		then:
		device.name == "iPhone 4s"
		device.identifier == "5C8E1FF3-47B7-48B8-96E9-A12740DBC58A"
	}

	def "get 8.4 device for destination xcode 7.1"() {
		given:
		commandRunner.runWithResult(["/Applications/Xcode.app/Contents/Developer/usr/bin/xcrun", "-sdk", "iphoneos", "-find", "simctl"]) >> "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
		commandRunner.runWithResult([SIMCTL, "list"]) >> FileUtils.readFileToString(new File("src/test/Resource/simctl-list-xcode7_1.txt"))
		Destination destination = new Destination()
		destination.name = "iPad 2"
		destination.platform = 'iOS Simulator'
		destination.os = '8.4'

		when:
		SimulatorDevice device = simulatorControl.getDevice(destination)

		then:
		device.name == "iPad 2"
		device.identifier == "E5089648-1CE4-40D5-8295-8E026BDDFF52"
	}

	def "get 9.1 device for destination xcode 7.1"() {
		given:
		commandRunner.runWithResult(["/Applications/Xcode.app/Contents/Developer/usr/bin/xcrun", "-sdk", "iphoneos", "-find", "simctl"]) >> "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
		commandRunner.runWithResult([SIMCTL, "list"]) >> FileUtils.readFileToString(new File("src/test/Resource/simctl-list-xcode7_1.txt"))
		Destination destination = new Destination()
		destination.name = "iPad 2"
		destination.platform = 'iOS Simulator'
		destination.os = '9.1'

		when:
		SimulatorDevice device = simulatorControl.getDevice(destination)

		then:
		device.name == "iPad 2"
		device.identifier == "D72F7CC6-8426-4E0A-A234-34747B1F30DD"
	}


	def "get all iOS simulator destinations"() {
		given:
		commandRunner.runWithResult(["/Applications/Xcode.app/Contents/Developer/usr/bin/xcrun", "-sdk", "iphoneos", "-find", "simctl"]) >> "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
		commandRunner.runWithResult([SIMCTL, "list"]) >> FileUtils.readFileToString(new File("src/test/Resource/simctl-list-xcode7_1.txt"))

		when:
		List<Destination> allDestinations = simulatorControl.getAllDestinations(Type.iOS)

		then:
		allDestinations.size() == 22
		allDestinations[0].name == 'iPhone 4s'
		allDestinations[0].platform == 'iOS Simulator'
		allDestinations[0].id == '8C8C43D3-B53F-4091-8D7C-6A4B38051389'
	}

	def "get all tvOS simulator destinations"() {
		given:
		commandRunner.runWithResult(["/Applications/Xcode.app/Contents/Developer/usr/bin/xcrun", "-sdk", "iphoneos", "-find", "simctl"]) >> "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
		commandRunner.runWithResult([SIMCTL, "list"]) >> FileUtils.readFileToString(new File("src/test/Resource/simctl-list-xcode7_1.txt"))

		when:
		List<Destination> allDestinations = simulatorControl.getAllDestinations(Type.tvOS)

		then:
		allDestinations.size() == 1
		allDestinations[0].name == 'Apple TV 1080p'
		allDestinations[0].platform == 'tvOS Simulator'
		allDestinations[0].id == '4395107C-169C-43D7-A403-C9030B6A205D'
	}




}


