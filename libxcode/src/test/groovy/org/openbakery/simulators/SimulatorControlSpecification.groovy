package org.openbakery.simulators

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.testdouble.XcodeFake
import org.openbakery.xcode.Destination
import org.openbakery.xcode.Type
import org.openbakery.xcode.Version
import org.openbakery.xcode.Xcode
import spock.lang.Specification

class SimulatorControlSpecification extends Specification {

	File projectDir
	SimulatorControl simulatorControl
	CommandRunner commandRunner = Mock(CommandRunner);
	Xcode xcode

	String simctlCommand

	def setup() {
		xcode = new XcodeFake("11.0")
		simctlCommand = xcode.getSimctl()
		simulatorControl = new SimulatorControl(commandRunner, xcode)
	}

	def cleanup() {
	}

	String loadSimctlList(String filename) {
		return FileUtils.readFileToString(new File("../libtest/src/main/Resource/", filename))
	}

	void mockXcode7() {
		commandRunner.runWithResult([simctlCommand, "list"]) >> loadSimctlList("simctl-list-xcode7.txt")
	}

	void mockXcode8() {
		commandRunner.runWithResult([simctlCommand, "list"]) >> loadSimctlList("simctl-list-xcode8.txt")
	}

	void mockXcode9() {
		commandRunner.runWithResult([simctlCommand, "list"]) >> loadSimctlList("simctl-list-xcode9.txt")
	}

	void mockXcode9_1() {
		commandRunner.runWithResult([simctlCommand, "list"]) >> loadSimctlList("simctl-list-xcode9_1.txt")
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
		commandRunner.runWithResult([simctlCommand, "list"]) >> loadSimctlList("simctl-list-xcode7_1.txt")

		when:
		SimulatorRuntime runtime = simulatorControl.getMostRecentRuntime(Type.iOS)

		then:
		runtime != null
		runtime.version == new Version("9.1")
	}

	def "get most recent tvOS runtime 7.1"() {

		given:
		commandRunner.runWithResult(["/Applications/Xcode.app/Contents/Developer/usr/bin/xcrun", "-sdk", "iphoneos", "-find", "simctl"]) >> "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
		commandRunner.runWithResult([simctlCommand, "list"]) >> loadSimctlList("simctl-list-xcode7_1.txt")

		when:
		SimulatorRuntime runtime = simulatorControl.getMostRecentRuntime(Type.tvOS)

		then:
		runtime != null
		runtime.version == new Version("9.0")
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
		commandRunner.runWithResult([simctlCommand, "list"]) >> loadSimctlList("simctl-list-xcode7_1.txt")

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
		1 * commandRunner.run([simctlCommand, "create", "iPhone 4s", "com.apple.CoreSimulator.SimDeviceType.iPhone-4s", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 5", "com.apple.CoreSimulator.SimDeviceType.iPhone-5", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 5s", "com.apple.CoreSimulator.SimDeviceType.iPhone-5s", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 6 Plus", "com.apple.CoreSimulator.SimDeviceType.iPhone-6-Plus", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 6", "com.apple.CoreSimulator.SimDeviceType.iPhone-6", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 6s Plus", "com.apple.CoreSimulator.SimDeviceType.iPhone-6s-Plus", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 6s", "com.apple.CoreSimulator.SimDeviceType.iPhone-6s", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPad 2", "com.apple.CoreSimulator.SimDeviceType.iPad-2", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPad Retina", "com.apple.CoreSimulator.SimDeviceType.iPad-Retina", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPad Air", "com.apple.CoreSimulator.SimDeviceType.iPad-Air", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPad Air 2", "com.apple.CoreSimulator.SimDeviceType.iPad-Air-2", "com.apple.CoreSimulator.SimRuntime.iOS-9-0"])
		1 * commandRunner.run([simctlCommand, "create", "Apple Watch - 38mm", "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-38mm", "com.apple.CoreSimulator.SimRuntime.watchOS-2-0"])
		1 * commandRunner.run([simctlCommand, "create", "Apple Watch - 42mm", "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-42mm", "com.apple.CoreSimulator.SimRuntime.watchOS-2-0"])

	}

	def "pair iPhone and Watch"() {
		given:
		mockXcode7()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.run([simctlCommand, "pair", "86895139-2FA4-4E97-A91A-C088A02F7BCD", "FE4BE76C-A3A1-4FB0-8BD4-7B87B4ACEDB2"])
		1 * commandRunner.run([simctlCommand, "pair", "2A40D83C-EF8E-46AB-9C50-7DA01DA0B01F", "6F866EE0-55E8-439C-95F4-3FF19DAF553F"])
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
		commandRunner.runWithResult([simctlCommand, "list"]) >> loadSimctlList("simctl-list-xcode7_1.txt")
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
		commandRunner.runWithResult([simctlCommand, "list"]) >> loadSimctlList("simctl-list-xcode7_1.txt")
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
		commandRunner.runWithResult([simctlCommand, "list"]) >> loadSimctlList("simctl-list-xcode7_1.txt")

		when:
		List<Destination> allDestinations = simulatorControl.getAllDestinations(Type.iOS)

		then:
		allDestinations.size() == 22
		allDestinations[0].name == 'iPhone 4s'
		allDestinations[0].platform == 'iOS Simulator'
		allDestinations[0].id == '8C8C43D3-B53F-4091-8D7C-6A4B38051389'
	}


	def "get all iOS simulator destinations of runtime"() {
		given:
		commandRunner.runWithResult(["/Applications/Xcode.app/Contents/Developer/usr/bin/xcrun", "-sdk", "iphoneos", "-find", "simctl"]) >> "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
		commandRunner.runWithResult([simctlCommand, "list"]) >> loadSimctlList("simctl-list-xcode7_1.txt")

		SimulatorRuntime runtime = simulatorControl.getMostRecentRuntime(Type.iOS)
		when:
		List<Destination> allDestinations = simulatorControl.getAllDestinations(Type.iOS, runtime)

		then:
		allDestinations.size() == 12
		allDestinations[0].name == 'iPhone 4s'
		allDestinations[0].platform == 'iOS Simulator'
		allDestinations[0].id == '8C8C43D3-B53F-4091-8D7C-6A4B38051389'
	}

	def "get all tvOS simulator destinations"() {
		given:
		commandRunner.runWithResult(["/Applications/Xcode.app/Contents/Developer/usr/bin/xcrun", "-sdk", "iphoneos", "-find", "simctl"]) >> "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"
		commandRunner.runWithResult([simctlCommand, "list"]) >> loadSimctlList("simctl-list-xcode7_1.txt")

		when:
		List<Destination> allDestinations = simulatorControl.getAllDestinations(Type.tvOS)

		then:
		allDestinations.size() == 1
		allDestinations[0].name == 'Apple TV 1080p'
		allDestinations[0].platform == 'tvOS Simulator'
		allDestinations[0].id == '4395107C-169C-43D7-A403-C9030B6A205D'
	}

	def "get xcode 8 runtimes"() {
		given:
		mockXcode8()


		expect:
		List<Runtime> runtimes = simulatorControl.getRuntimes()

		runtimes != null
		runtimes.size() == 3

	}

	def "get xcode 8 has TV runtime"() {
		given:
		mockXcode8()


		expect:
		SimulatorRuntime runtime = simulatorControl.getMostRecentRuntime(Type.tvOS)
		runtime != null
	}


	def "xcode 8 has Apple TV device"() {
		given:
		mockXcode8()


		expect:
		SimulatorRuntime runtime = simulatorControl.getMostRecentRuntime(Type.tvOS)

		List<SimulatorDevice> devices = simulatorControl.getDevices(runtime);

		devices != null
		devices.size() == 1

	}



	def "xcode 8 has Apple TV device type "() {
		given:
		mockXcode8()


		expect:
		List<SimulatorDeviceType> deviceTypes = simulatorControl.getDeviceTypes()

		deviceTypes != null
		deviceTypes.size() == 21

		deviceTypes[16].shortIdentifier == "Apple-TV-1080p"


	}


	def "can create Apple TV device"() {
		given:
		mockXcode8()

		expect:
		SimulatorRuntime appleTVRuntime = simulatorControl.getMostRecentRuntime(Type.tvOS)

		List<SimulatorDeviceType> deviceTypes = simulatorControl.getDeviceTypes()
		SimulatorDeviceType deviceType = deviceTypes[16]
		deviceType.canCreateWithRuntime(appleTVRuntime) == true

	}


	def "create all iOS10 Simulators"() {
		given:
		mockXcode8()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.run([simctlCommand, "create", "iPhone 5", "com.apple.CoreSimulator.SimDeviceType.iPhone-5", "com.apple.CoreSimulator.SimRuntime.iOS-10-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 5s", "com.apple.CoreSimulator.SimDeviceType.iPhone-5s", "com.apple.CoreSimulator.SimRuntime.iOS-10-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 6 Plus", "com.apple.CoreSimulator.SimDeviceType.iPhone-6-Plus", "com.apple.CoreSimulator.SimRuntime.iOS-10-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 6", "com.apple.CoreSimulator.SimDeviceType.iPhone-6", "com.apple.CoreSimulator.SimRuntime.iOS-10-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 6s Plus", "com.apple.CoreSimulator.SimDeviceType.iPhone-6s-Plus", "com.apple.CoreSimulator.SimRuntime.iOS-10-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 6s", "com.apple.CoreSimulator.SimDeviceType.iPhone-6s", "com.apple.CoreSimulator.SimRuntime.iOS-10-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPad 2", "com.apple.CoreSimulator.SimDeviceType.iPad-2", "com.apple.CoreSimulator.SimRuntime.iOS-10-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPad Retina", "com.apple.CoreSimulator.SimDeviceType.iPad-Retina", "com.apple.CoreSimulator.SimRuntime.iOS-10-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPad Air", "com.apple.CoreSimulator.SimDeviceType.iPad-Air", "com.apple.CoreSimulator.SimRuntime.iOS-10-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPad Air 2", "com.apple.CoreSimulator.SimDeviceType.iPad-Air-2", "com.apple.CoreSimulator.SimRuntime.iOS-10-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPad Pro (9.7-inch)", "com.apple.CoreSimulator.SimDeviceType.iPad-Pro--9-7-inch-", "com.apple.CoreSimulator.SimRuntime.iOS-10-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPad Pro (12.9-inch)", "com.apple.CoreSimulator.SimDeviceType.iPad-Pro", "com.apple.CoreSimulator.SimRuntime.iOS-10-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 7 Plus", "com.apple.CoreSimulator.SimDeviceType.iPhone-7-Plus", "com.apple.CoreSimulator.SimRuntime.iOS-10-0"])
		1 * commandRunner.run([simctlCommand, "create", "iPhone 7", "com.apple.CoreSimulator.SimDeviceType.iPhone-7", "com.apple.CoreSimulator.SimRuntime.iOS-10-0"])
		1 * commandRunner.run([simctlCommand, "create", "Apple Watch - 38mm", "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-38mm", "com.apple.CoreSimulator.SimRuntime.watchOS-3-0"])
		1 * commandRunner.run([simctlCommand, "create", "Apple Watch - 42mm", "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-42mm", "com.apple.CoreSimulator.SimRuntime.watchOS-3-0"])
		1 * commandRunner.run([simctlCommand, "create", "Apple Watch Series 2 - 38mm", "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-2-38mm", "com.apple.CoreSimulator.SimRuntime.watchOS-3-0"])
		1 * commandRunner.run([simctlCommand, "create", "Apple Watch Series 2 - 42mm", "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-2-42mm", "com.apple.CoreSimulator.SimRuntime.watchOS-3-0"])
		1 * commandRunner.run([simctlCommand, "create", "Apple TV 1080p", "com.apple.CoreSimulator.SimDeviceType.Apple-TV-1080p", "com.apple.CoreSimulator.SimRuntime.tvOS-10-0"])

	}


	def "devices iOS10"() {
		given:
		mockXcode8()

		when:
		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(0));

		then:
		devices != null
		devices.size() == 14
		devices.get(13).name.equals("iPad Pro (12.9 inch)")
		devices.get(13).identifier.equals("C538D7F8-E581-44FF-9B17-5391F84642FB")
		devices.get(13).state.equals("Shutdown")
	}


	def "device types iOS10"() {
		given:
		mockXcode8()

		when:
		List<SimulatorDeviceType> deviceTypes = simulatorControl.getDeviceTypes()

		then:
		deviceTypes != null
		deviceTypes.size() == 21
		deviceTypes.get(14).name.equals("iPad Pro (9.7-inch)")
		deviceTypes.get(14).identifier.equals("com.apple.CoreSimulator.SimDeviceType.iPad-Pro--9-7-inch-")
	}

	def "devices iOS11"() {
		given:
		mockXcode9()

		when:
		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(0))

		then:
		devices != null
		devices.size() == 15
	}


	def "devices iOS11.1 runtimes" () {
		given:
		mockXcode9_1()

		when:
		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()

		then:
		runtimes != null
		runtimes.size() == 3
		runtimes.get(0).name == "iOS 11.1"
	}

	def "devices iOS11.1"() {
		given:
		mockXcode9_1()

		when:
		List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
		SimulatorRuntime runtime = runtimes.get(0)
		List<SimulatorDevice> devices = simulatorControl.getDevices(runtimes.get(0))

		then:
		devices != null
		devices.size() == 18
		runtime.name == "iOS 11.1"
	}

	def "pair iOS 11 watchOS Simulator with iOS Simulator"() {
		given:
		mockXcode9_1()

		when:
		simulatorControl.pair()

		then:
		1 * commandRunner.run([simctlCommand, "pair", phoneIdentifier, watchIdentifier])

		where:
		phoneIdentifier                        | watchIdentifier
		"BE09415B-6BAC-494A-B915-3E7132EDF882" | "C29A4DF8-0111-4310-9734-62C1ABE934B9"
		"7AE1CDE1-9F6F-474E-BF0C-E7A20B0A7130" | "3AAF60B7-E186-40AE-AC3D-4393ABB6DEC8"
		"AB24286A-F08B-4E85-BC75-3C30D047E57E" | "8AB20D83-995F-4668-8152-8D82464BDC71"
		"0786AEDF-911D-4388-B482-9F4C2D92BF43" | "777188FF-91ED-4E19-9964-172248E85AA1"

	}


}


