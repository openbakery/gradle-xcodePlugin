package org.openbakery.simulators

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.testdouble.XcodeFake
import org.openbakery.xcode.Type
import org.openbakery.xcode.Version
import org.openbakery.xcode.Xcode

class SimulatorControl_XCode13_Specification extends spock.lang.Specification {


	File projectDir
	SimulatorControl simulatorControl
	CommandRunner commandRunner = Mock(CommandRunner)
	Xcode xcode

	def SIMCTL = "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"

	def setup() {
		xcode = new XcodeFake("13.0")
		simulatorControl = new SimulatorControl(commandRunner, xcode)

	}

	def cleanup() {
		simulatorControl = null
		commandRunner = null
		xcode = null
	}

	void mockSimctlList() {
		String json = FileUtils.readFileToString(new File("../libtest/src/main/Resource/simctl-list-xcode13-full.json"))
		commandRunner.runWithResult([xcode.getSimctl(), "list", "--json"]) >> json
	}


	def "list uses json format when xcode 13"() {
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


	def "parse result has 3 runtimes"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()

		then:
		simulatorControl.getRuntimes().size() == 3
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
		0     | "iOS 15.0"    | new Version("15.0") | "19A339"    | "com.apple.CoreSimulator.SimRuntime.iOS-15-0"    | "iOS-15-0"      | true      | Type.iOS
		1     | "tvOS 15.0"   | new Version("15.0") | "19J344"    | "com.apple.CoreSimulator.SimRuntime.tvOS-15-0"   | "tvOS-15-0"     | true      | Type.tvOS
		2     | "watchOS 8.0" | new Version("8.0")  | "19R344"    | "com.apple.CoreSimulator.SimRuntime.watchOS-8-0" | "watchOS-8-0"   | true      | Type.watchOS
	}



	def "parse result has 24 iOS devices"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.iOS)

		then:
		simulatorControl.getDevices(runtime).size() == 24
	}

	def "parse result has 5 tvOS devices"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.tvOS)

		then:
		simulatorControl.getDevices(runtime).size() == 5
	}




	def "parse result has 6 watchOS devices"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.watchOS)

		then:
		simulatorControl.getDevices(runtime).size() == 6
	}

	/*
	    iPhone 8 (64394BD8-30A4-49F3-ACF8-8C456DD08322) (Shutdown)
    iPhone 8 Plus (AE03EDA8-13E2-4286-9B56-73D4F45BE783) (Shutdown)
    iPhone 11 (F68E4B5F-CA8D-4B64-9E36-1008CF0DB16D) (Shutdown)
    iPhone 11 Pro (39B20CE9-AA10-47E4-942C-4CB4682A67BB) (Shutdown)
    iPhone 11 Pro Max (57EADD07-5FA4-4015-860B-EB46E49CD87D) (Shutdown)
    iPhone SE (2nd generation) (BBF4D1BA-6D54-4D52-A975-EEFDBD05F1E6) (Shutdown)
    iPhone 12 mini (DF9EECAA-FAC3-40E1-A9C4-BF823F9F5FB5) (Shutdown)
    iPhone 12 (D0597B26-9A0A-4287-ABD4-EE5EAE7433A4) (Shutdown)
    iPhone 12 Pro (58D64770-D9DD-41B0-AB67-C2917F5A1DED) (Shutdown)
    iPhone 12 Pro Max (FCC4A417-449B-4FC7-A37F-AE1A070FEAC9) (Shutdown)
    iPhone 13 Pro (99385A35-826B-45C9-989A-3CDBEF111FB0) (Shutdown)
    iPhone 13 Pro Max (2D0F3CE3-D6B7-4E7C-AB40-FEFB70E003A6) (Shutdown)
    iPhone 13 mini (880F04A4-BE3A-43E0-84A1-CB39C51E0D5E) (Shutdown)
    iPhone 13 (ECBAA332-FC5C-4036-87BA-4B795EC967E5) (Shutdown)
    iPod touch (7th generation) (D3C1017F-1A2A-44BB-9EEC-881A7754D557) (Shutdown)
    iPad Pro (9.7-inch) (C963E09D-75C9-480F-96B2-DD67B42DFC54) (Shutdown)
    iPad Pro (11-inch) (2nd generation) (BECAFCBC-9CC1-495A-9822-8EDF7F6311C5) (Shutdown)
    iPad Pro (12.9-inch) (4th generation) (5ACEF9B5-176D-4AEE-BCD6-703312AA2BC0) (Shutdown)
    iPad (8th generation) (FCB10AFC-8310-4518-98D3-349B99C07321) (Shutdown)
    iPad (9th generation) (C4CC18AD-0451-4B2A-9DC6-3D59BAF55364) (Shutdown)
    iPad Air (4th generation) (99ECF37E-8CE6-47B7-B102-B88B5EBFE495) (Shutdown)
    iPad Pro (11-inch) (3rd generation) (A43BF4BB-32D0-4138-BC07-7AE742DF0FD1) (Shutdown)
    iPad Pro (12.9-inch) (5th generation) (E1A91BCB-70E6-4E77-B704-4645111109F4) (Shutdown)
    iPad mini (6th generation) (1D29E530-5FB0-4E14-90E3-5682DD1AF9EB) (Shutdown)



	    Apple Watch Series 5 - 40mm (8EFC1EBA-38B9-4A19-BEC6-5FC44B5FC2BD) (Shutdown)
    Apple Watch Series 5 - 44mm (25257328-33D4-4C1B-8E51-0FB4BA9362E5) (Shutdown)
    Apple Watch Series 6 - 40mm (D704A504-517B-4A22-BC66-FA432CAE6945) (Shutdown)
    Apple Watch Series 6 - 44mm (C331A0C8-0321-4708-8D8E-4D61A062E33B) (Shutdown)
    Apple Watch Series 7 - 41mm (8068FCBA-A888-46EF-93F8-7580A2340707) (Shutdown)
    Apple Watch Series 7 - 45mm (98780666-AB3C-44FA-98B0-440ABF51B469) (Shutdown)
	 */



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
		0     | "iPhone 8"                              | "F2EFE70F-6235-41BB-96C5-1E3D51AF2C8D" | "Shutdown" | true
		1     | "iPhone 8 Plus"                         | "BF4BC663-D21C-416F-B430-A113964DDCD8" | "Shutdown" | true
		2     | "iPhone 11"                             | "ED05EB13-5A68-46F3-9853-047B553AEFE4" | "Shutdown" | true
		3     | "iPhone 11 Pro"                         | "BEEBAD96-AA35-47B4-A3E7-6938DF192C55" | "Shutdown" | true
		4     | "iPhone 11 Pro Max"                     | "BF2E75B2-F57B-47DC-8D88-22F0DC62BE58" | "Shutdown" | true
		5     | "iPhone SE (2nd generation)"            | "62263648-5968-4F0F-973D-701BA21ED134" | "Shutdown" | true
		6     | "iPhone 12 mini"                        | "F29AB610-A10A-4F48-891F-790F6FF51481" | "Shutdown" | true
		7     | "iPhone 12"                             | "BE556B2F-A207-4952-B8D3-406A246FD6BB" | "Shutdown" | true
		8     | "iPhone 12 Pro"                         | "76B1BC6B-1C86-4F84-9B5A-6035F98EEFFF" | "Shutdown" | true
		9     | "iPhone 12 Pro Max"                     | "C88274D8-543E-4599-9082-D26CFDC289BA" | "Shutdown" | true
		10    | "iPhone 13 Pro"                         | "06CADAEE-23CD-44FD-8B60-C03896A781BD" | "Shutdown" | true
		11    | "iPhone 13 Pro Max"                     | "08AABA64-ED83-4261-918E-D0B77CFD18A5" | "Shutdown" | true
		12    | "iPhone 13 mini"                        | "ACCF31A0-7D3C-4422-B037-FDE5BEB32F97" | "Shutdown" | true
		13    | "iPhone 13"                             | "17410E3F-DFCF-4DB4-937E-A7C703137361" | "Shutdown" | true
		14    | "iPod touch (7th generation)"           | "C9B07696-FC48-49DF-AD90-E3AF31C95A0B" | "Shutdown" | true
		15    | "iPad Pro (9.7-inch)"                   | "75C6F493-DC67-45F8-9DCA-CF3C4EB11878" | "Shutdown" | true
		16    | "iPad Pro (11-inch) (2nd generation)"   | "80DD5A1E-1EB1-4A76-B008-5019CC53E0D8" | "Shutdown" | true
		17    | "iPad Pro (12.9-inch) (4th generation)" | "B8EC3937-554D-475A-9E5D-F45F05A5BE9E" | "Shutdown" | true
		18    | "iPad (8th generation)"                 | "CBC2B19C-BEAE-489E-923B-04EE746509B3" | "Shutdown" | true
		19    | "iPad (9th generation)"                 | "510175B5-FF8D-4844-BAEF-B28B203343AF" | "Shutdown" | true
		20    | "iPad Air (4th generation)"             | "02776893-0EEA-4616-8514-3B8A84EF618D" | "Shutdown" | true
		21    | "iPad Pro (11-inch) (3rd generation)"   | "CB421107-5C3C-4DE3-8879-814D289887A8" | "Shutdown" | true
		22    | "iPad Pro (12.9-inch) (5th generation)" | "04B66423-1C70-46A1-BA37-61B9F7E67E27" | "Shutdown" | true
		23    | "iPad mini (6th generation)"            | "B7421C19-006D-4AAB-8AC9-291678209FF3" | "Shutdown" | true

	}


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


	def "has 6 pairs"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.watchOS)

		then:
		simulatorControl.getDevicePairs().size() == 6
	}



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


	def "has 76 devices types"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()

		then:
		simulatorControl.deviceTypes.size() == 76
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
		0     | "com.apple.CoreSimulator.SimDeviceType.iPhone-4s"                   | "iPhone 4s"                   | "iPhone-4s"
		10    | "com.apple.CoreSimulator.SimDeviceType.iPhone-8"                    | "iPhone 8"                    | "iPhone-8"
		28    | "com.apple.CoreSimulator.SimDeviceType.iPod-touch--7th-generation-" | "iPod touch (7th generation)" | "iPod-touch--7th-generation-"
		57    | "com.apple.CoreSimulator.SimDeviceType.Apple-TV-4K-1080p"           | "Apple TV 4K (at 1080p)"      | "Apple-TV-4K-1080p"
		73    | "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-6-44mm"   | "Apple Watch Series 6 - 44mm" | "Apple-Watch-Series-6-44mm"

	}


	def "create creates 34 Xcode13 devices"() {
		// given
		def runtime = "com.apple.CoreSimulator.SimRuntime.iOS-15-0"
		mockSimctlList()
		simulatorControl.parse()

		when:
		simulatorControl.createAll()

		then:
		34 * commandRunner.run(_)
	}


	def "create creates only Xcode13 devices"() {
		// given
		def runtime = "com.apple.CoreSimulator.SimRuntime.iOS-15-0"
		mockSimctlList()
		simulatorControl.parse()

		when:
		simulatorControl.createAll()

		then:
		1 * commandRunner.run([xcode.getSimctl(), "create", name, identifier, runtime])

		where:
		name                                    | identifier
		"iPhone 8"                              | "com.apple.CoreSimulator.SimDeviceType.iPhone-8"
		"iPhone 8 Plus"                         | "com.apple.CoreSimulator.SimDeviceType.iPhone-8-Plus"
		"iPhone 11"                             | "com.apple.CoreSimulator.SimDeviceType.iPhone-11"
		"iPhone 11 Pro"                         | "com.apple.CoreSimulator.SimDeviceType.iPhone-11-Pro"
		"iPhone 11 Pro Max"                     | "com.apple.CoreSimulator.SimDeviceType.iPhone-11-Pro-Max"
		"iPhone SE (2nd generation)"            | "com.apple.CoreSimulator.SimDeviceType.iPhone-SE--2nd-generation-"
		"iPhone 12 mini"                        | "com.apple.CoreSimulator.SimDeviceType.iPhone-12-mini"
		"iPhone 12"                             | "com.apple.CoreSimulator.SimDeviceType.iPhone-12"
		"iPhone 12 Pro"                         | "com.apple.CoreSimulator.SimDeviceType.iPhone-12-Pro"
		"iPhone 12 Pro Max"                     | "com.apple.CoreSimulator.SimDeviceType.iPhone-12-Pro-Max"
		"iPhone 13 mini"                        | "com.apple.CoreSimulator.SimDeviceType.iPhone-13-mini"
		"iPhone 13"                             | "com.apple.CoreSimulator.SimDeviceType.iPhone-13"
		"iPhone 13 Pro"                         | "com.apple.CoreSimulator.SimDeviceType.iPhone-13-Pro"
		"iPhone 13 Pro Max"                     | "com.apple.CoreSimulator.SimDeviceType.iPhone-13-Pro-Max"
		"iPod touch (7th generation)"           | "com.apple.CoreSimulator.SimDeviceType.iPod-touch--7th-generation-"
		"iPad Pro (9.7-inch)"                   | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro--9-7-inch-"
		"iPad Pro (11-inch) (2nd generation)"   | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro--11-inch---2nd-generation-"
		"iPad Pro (12.9-inch) (4th generation)" | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro--12-9-inch---4th-generation-"
		"iPad (8th generation)"                 | "com.apple.CoreSimulator.SimDeviceType.iPad--8th-generation-"
		"iPad Air (4th generation)"             | "com.apple.CoreSimulator.SimDeviceType.iPad-Air--4th-generation-"
		"iPad Pro (11-inch) (3rd generation)"   | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro-11-inch-3rd-generation"
		"iPad Pro (12.9-inch) (5th generation)" | "com.apple.CoreSimulator.SimDeviceType.iPad-Pro-12-9-inch-5th-generation"
		"iPad mini (6th generation)"            | "com.apple.CoreSimulator.SimDeviceType.iPad-mini-6th-generation"

	}


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

}
