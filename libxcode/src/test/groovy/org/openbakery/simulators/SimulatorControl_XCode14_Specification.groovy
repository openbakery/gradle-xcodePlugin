package org.openbakery.simulators

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.testdouble.XcodeFake
import org.openbakery.xcode.Type
import org.openbakery.xcode.Version
import org.openbakery.xcode.Xcode
import java.nio.*

import java.nio.charset.Charset
import java.nio.file.Files

class SimulatorControl_XCode14_Specification extends spock.lang.Specification {

	File projectDir
	SimulatorControl simulatorControl
	CommandRunner commandRunner = Mock(CommandRunner)
	Xcode xcode

	def setup() {
		xcode = new XcodeFake("14.0")
		simulatorControl = new SimulatorControl(commandRunner, xcode)

	}

	def cleanup() {
		simulatorControl = null
		commandRunner = null
		xcode = null
	}

	void mockSimctlList() {
		def file = new File("../libtest/src/main/Resource/simctl-list-xcode14-full.json")
		String json= Files.readString(file.toPath())
		commandRunner.runWithResult([xcode.getSimctl(), "list", "--json"]) >> json
	}


	def "list uses json format when xcode 14"() {
		given:
		def commandList

		when:
		try {
			simulatorControl.parse()
		} catch (Exception ignored) {
		}

		then:
		1 * commandRunner.runWithResult(_) >> { arguments -> commandList = arguments[0] }
		commandList == [
			xcode.getSimctl(),
			"list",
			"--json"
		]
	}


	def "parse result has one runtimes"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()

		then:
		simulatorControl.getRuntimes().size() == 1
	}


	def "parse result proper runtime data"() {
		given:
		mockSimctlList()
		simulatorControl.parse()

		expect:
		simulatorControl.getRuntimes()[index].name == name
		simulatorControl.getRuntimes()[index].version == version
		simulatorControl.getRuntimes()[index].version == version
		simulatorControl.getRuntimes()[index].buildNumber == buildNumber
		simulatorControl.getRuntimes()[index].identifier == identifier
		simulatorControl.getRuntimes()[index].shortIdentifier == shortIdentifier
		simulatorControl.getRuntimes()[index].available == available
		simulatorControl.getRuntimes()[index].type == type

		where:
		index | name          | version             | buildNumber | identifier                                       | shortIdentifier | available | type
		0     | "iOS 16.4"    | new Version("16.4") | "20E247" | "com.apple.CoreSimulator.SimRuntime.iOS-16-4" | "iOS-16-4" | true | Type.iOS
	}



	def "parse result has 10 iOS devices"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.iOS)

		then:
		simulatorControl.getDevices(runtime).size() == 10
	}

	def "parse result has 0 tvOS devices"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.tvOS)

		then:
		runtime == null
//		simulatorControl.getDevices(runtime).size() == 0
	}


	def "parse result has 0 watchOS devices"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.watchOS)

		then:
		runtime == null
//		simulatorControl.getDevices(runtime).size() == 0
	}



	def "parse creates the iOS devices with the proper data"() {
		given:
		mockSimctlList()
		simulatorControl.parse()

		expect:
		SimulatorRuntime runtime = simulatorControl.getMostRecentRuntime(Type.iOS)
		simulatorControl.getDevices(runtime)[index].name == name
		simulatorControl.getDevices(runtime)[index].identifier == identifier
		simulatorControl.getDevices(runtime)[index].state == state
		simulatorControl.getDevices(runtime)[index].available == available

		where:
		index | name                                    | identifier                             | state      | available
		0     | "iPhone SE (3rd generation)"            | "45014C4D-3B34-48FF-819D-53AAC93D5375" | "Shutdown" | true
		1     | "iPhone 14"                             | "47E61DE9-D808-44F3-8371-F59215E0C596" | "Shutdown" | true
		8     | "iPad Pro (11-inch) (4th generation)"   | "4A3DCA16-2669-4EB3-8F9D-CF211EA9363F" | "Booted" | true
	}

/*
	def "parse creates the tvOS devices with the proper data"() {
		given:
		mockSimctlList()
		simulatorControl.parse()

		expect:
		SimulatorRuntime runtime = simulatorControl.getMostRecentRuntime(Type.tvOS)
		simulatorControl.getDevices(runtime)[index].name == name
		simulatorControl.getDevices(runtime)[index].identifier == identifier
		simulatorControl.getDevices(runtime)[index].state == state
		simulatorControl.getDevices(runtime)[index].available == available

		where:
		index | name                                      | identifier                             | state      | available
		0     | "Apple TV"                                | "DE95CAAC-EE74-43F0-82A0-AB38E1D58A7C" | "Shutdown" | true
		1     | "Apple TV 4K"                             | "3F9EC5D2-E2A3-4728-AE50-5B4817D6B092" | "Shutdown" | true
		2     | "Apple TV 4K (at 1080p)"                  | "279134C6-4DAD-4E5A-8EA7-85AB9FAEED5C" | "Shutdown" | true
		3     | "Apple TV 4K (2nd generation)"            | "11577EE2-0384-408C-9386-5FF30E35A6F3" | "Shutdown" | true
		4     | "Apple TV 4K (at 1080p) (2nd generation)" | "B85C966D-EA43-4633-855B-5AAE9CD455CF" | "Shutdown" | true

	}

	def "parse creates the watchOS devices with the proper data"() {
		given:
		mockSimctlList()
		simulatorControl.parse()

		expect:
		SimulatorRuntime runtime = simulatorControl.getMostRecentRuntime(Type.watchOS)
		simulatorControl.getDevices(runtime)[index].name == name
		simulatorControl.getDevices(runtime)[index].identifier == identifier
		simulatorControl.getDevices(runtime)[index].state == state
		simulatorControl.getDevices(runtime)[index].available == available

		where:
		index | name                          | identifier                             | state      | available
		0     | "Apple Watch Series 5 - 40mm" | "54AB5E4C-DFE9-4EE0-B686-81A9C91B3269" | "Shutdown" | true
		1     | "Apple Watch Series 5 - 44mm" | "078785A5-E779-47C8-BA4B-D2E7143AC31F" | "Shutdown" | true
		2     | "Apple Watch Series 6 - 40mm" | "E458252B-9B5F-41EB-BE0F-8534D8A081C7" | "Shutdown" | true
		3     | "Apple Watch Series 6 - 44mm" | "DD2B2E03-F9EC-4950-A214-09A9CAF9F716" | "Shutdown" | true
		4     | "Apple Watch Series 7 - 41mm" | "E037C092-0951-4336-B05C-F18446E05E1A" | "Shutdown" | true
		5     | "Apple Watch Series 7 - 45mm" | "DB1F5A3C-CE41-47C6-B300-5220BC76A868" | "Shutdown" | true

	}


*/
	def "has no pairs"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.watchOS)

		then:
		simulatorControl.getDevicePairs().size() == 0
	}

/*

	def "parse pairs with proper data"() {
		given:
		mockSimctlList()
		simulatorControl.parse()

		expect:
		simulatorControl.devicePairs[index].identifier == identifier
		simulatorControl.devicePairs[index].watch.identifier == watch
		simulatorControl.devicePairs[index].phone.identifier == phone

		where:
		index | identifier                             | watch                                  | phone
		0     | "CA7C8735-C0C6-40B5-A33F-461403DC4A5F" | "54AB5E4C-DFE9-4EE0-B686-81A9C91B3269" | "76B1BC6B-1C86-4F84-9B5A-6035F98EEFFF"
		1     | "3CD28F7C-F95D-4BFD-A59F-BB4D606DAC7E" | "E037C092-0951-4336-B05C-F18446E05E1A" | "ACCF31A0-7D3C-4422-B037-FDE5BEB32F97"
		2     | "67EDCC25-3B6B-4C0A-8D1B-C3305547C4C8" | "DB1F5A3C-CE41-47C6-B300-5220BC76A868" | "17410E3F-DFCF-4DB4-937E-A7C703137361"
		3     | "2CED6E2C-1FDC-4D7C-A3A5-7BA0D21377AC" | "078785A5-E779-47C8-BA4B-D2E7143AC31F" | "C88274D8-543E-4599-9082-D26CFDC289BA"
		4     | "14ECC156-E9A6-4D05-A4B5-82364DBD080B" | "E458252B-9B5F-41EB-BE0F-8534D8A081C7" | "06CADAEE-23CD-44FD-8B60-C03896A781BD"
		5     | "1ACDDE95-6372-4FC4-BAD3-DE77E173B324" | "DD2B2E03-F9EC-4950-A214-09A9CAF9F716" | "08AABA64-ED83-4261-918E-D0B77CFD18A5"
	}


 */

	def "has 82 devices types"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()

		then:
		simulatorControl.deviceTypes.size() == 82
	}



	def "has devices types values"() {
		given:
		mockSimctlList()

		expect:
		simulatorControl.deviceTypes[index].identifier == identifier
		simulatorControl.deviceTypes[index].name == name
		simulatorControl.deviceTypes[index].shortIdentifier == shortIdentifier

		where:
		index | identifier                                                          | name                          | shortIdentifier
		0     | "com.apple.CoreSimulator.SimDeviceType.iPhone-6s"                   | "iPhone 6s"                   | "iPhone-6s"
		5     | "com.apple.CoreSimulator.SimDeviceType.iPhone-8"                    | "iPhone 8"                    | "iPhone-8"
		28    | "com.apple.CoreSimulator.SimDeviceType.iPod-touch--7th-generation-" | "iPod touch (7th generation)" | "iPod-touch--7th-generation-"
		58    | "com.apple.CoreSimulator.SimDeviceType.Apple-TV-4K-1080p"           | "Apple TV 4K (at 1080p)"      | "Apple-TV-4K-1080p"
		74    | "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-6-44mm"   | "Apple Watch Series 6 (44mm)" | "Apple-Watch-Series-6-44mm"

	}


	def "create creates 14 Xcode14 devices"() {
		// given
		mockSimctlList()
		simulatorControl.parse()

		when:
		simulatorControl.createAll()

		then:
		14 * commandRunner.run(_)
	}


	def "create creates only Xcode14 devices"() {
		// given
		def runtime = "com.apple.CoreSimulator.SimRuntime.iOS-16-4"
		mockSimctlList()
		simulatorControl.parse()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.run([xcode.getSimctl(), "create", name, identifier, runtime])

		where:
		name                                    | identifier
		"iPad Air (5th generation)"             | "com.apple.CoreSimulator.SimDeviceType.iPad-Air-5th-generation"
		"iPad Pro (11-inch) (4th generation)"   | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro-11-inch-4th-generation-8GB"
		"iPad Pro (12.9-inch) (2nd generation)" | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro--12-9-inch---2nd-generation-"
		"iPad Pro (12.9-inch) (6th generation)" | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro-12-9-inch-6th-generation-8GB"
		"iPad mini (6th generation)"            | "com.apple.CoreSimulator.SimDeviceType.iPad-mini-6th-generation"
		"iPhone 8"                              | "com.apple.CoreSimulator.SimDeviceType.iPhone-8"
		"iPhone 8 Plus"                         | "com.apple.CoreSimulator.SimDeviceType.iPhone-8-Plus"
		"iPhone 11 Pro Max"                     | "com.apple.CoreSimulator.SimDeviceType.iPhone-11-Pro-Max"
		"iPhone 12 Pro Max"                     | "com.apple.CoreSimulator.SimDeviceType.iPhone-12-Pro-Max"
		"iPhone 14"                             | "com.apple.CoreSimulator.SimDeviceType.iPhone-14"
		"iPhone 14 Plus"                        | "com.apple.CoreSimulator.SimDeviceType.iPhone-14-Plus"
		"iPhone 14 Pro"                         | "com.apple.CoreSimulator.SimDeviceType.iPhone-14-Pro"
		"iPhone 14 Pro Max"                     | "com.apple.CoreSimulator.SimDeviceType.iPhone-14-Pro-Max"
		"iPhone SE (3rd generation)"            | "com.apple.CoreSimulator.SimDeviceType.iPhone-SE-3rd-generation"
	}

/*
	def "create creates only Xcode13 TV devices"() {
		// given
		def runtime = "com.apple.CoreSimulator.SimRuntime.tvOS-15-0"
		mockSimctlList()
		simulatorControl.parse()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.run([xcode.getSimctl(), "create", name, identifier, runtime])

		where:
		name                                      | identifier
		"Apple TV"                                | "com.apple.CoreSimulator.SimDeviceType.Apple-TV-1080p"
		"Apple TV 4K"                             | "com.apple.CoreSimulator.SimDeviceType.Apple-TV-4K-4K"
		"Apple TV 4K (at 1080p)"                  | "com.apple.CoreSimulator.SimDeviceType.Apple-TV-4K-1080p"
		"Apple TV 4K (2nd generation)"            | "com.apple.CoreSimulator.SimDeviceType.Apple-TV-4K-2nd-generation-4K"
		"Apple TV 4K (at 1080p) (2nd generation)" | "com.apple.CoreSimulator.SimDeviceType.Apple-TV-4K-2nd-generation-1080p"
	}



	def "pair iPhone and Watch"() {
		// given
		def runtime = "com.apple.CoreSimulator.SimRuntime.iOS-15-0"
		mockSimctlList()
		simulatorControl.parse()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.run([xcode.getSimctl(), "pair", phone, watch])

		where:
		phone                                  | watch
		"76B1BC6B-1C86-4F84-9B5A-6035F98EEFFF" | "54AB5E4C-DFE9-4EE0-B686-81A9C91B3269"
		"ACCF31A0-7D3C-4422-B037-FDE5BEB32F97" | "E037C092-0951-4336-B05C-F18446E05E1A"
		"17410E3F-DFCF-4DB4-937E-A7C703137361" | "DB1F5A3C-CE41-47C6-B300-5220BC76A868"
		"C88274D8-543E-4599-9082-D26CFDC289BA" | "078785A5-E779-47C8-BA4B-D2E7143AC31F"
		"06CADAEE-23CD-44FD-8B60-C03896A781BD" | "E458252B-9B5F-41EB-BE0F-8534D8A081C7"
		"08AABA64-ED83-4261-918E-D0B77CFD18A5" | "DD2B2E03-F9EC-4950-A214-09A9CAF9F716"
	}

 */
}
