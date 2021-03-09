package org.openbakery.simulators

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.testdouble.XcodeFake
import org.openbakery.xcode.Type
import org.openbakery.xcode.Version
import org.openbakery.xcode.Xcode
import spock.lang.Specification

class SimulatorControl_XCode12_Specification extends Specification {

	File projectDir
	SimulatorControl simulatorControl
	CommandRunner commandRunner = Mock(CommandRunner)
	Xcode xcode

	def SIMCTL = "/Applications/Xcode.app/Contents/Developer/usr/bin/simctl"

	def setup() {
		xcode = new XcodeFake("12.0")
		//xcode.getSimctl() >> SIMCTL
		//xcode.getPath() >> "/Applications/Xcode.app"
		simulatorControl = new SimulatorControl(commandRunner, xcode)

	}

	def cleanup() {
		simulatorControl = null
		commandRunner = null
		xcode = null
	}

	void mockSimctlList() {
		String json = FileUtils.readFileToString(new File("../libtest/src/main/Resource/simctl-list-xcode12-full.json"))
		commandRunner.runWithResult([xcode.getSimctl(), "list", "--json"]) >> json
	}

	def "list uses json format when xcode 12"() {
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
		0     | "iOS 14.4"    | new Version("14.4") | "18D46"     | "com.apple.CoreSimulator.SimRuntime.iOS-14-4"    | "iOS-14-4"      | true      | Type.iOS
		1     | "tvOS 14.3"   | new Version("14.3") | "18K559"    | "com.apple.CoreSimulator.SimRuntime.tvOS-14-3"   | "tvOS-14-3"     | true      | Type.tvOS
		2     | "watchOS 7.2" | new Version("7.2")  | "18S561"    | "com.apple.CoreSimulator.SimRuntime.watchOS-7-2" | "watchOS-7-2"   | true      | Type.watchOS
	}


	def "parse result has 36 iOS devices"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.iOS)

		then:
		simulatorControl.getDevices(runtime).size() == 36
	}


	def "parse result has 3 tvOS devices"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.tvOS)

		then:
		simulatorControl.getDevices(runtime).size() == 3
	}

	def "parse result has 10 watchOS devices"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.watchOS)

		then:
		simulatorControl.getDevices(runtime).size() == 10
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
		0     | "iPhone 6s"                             | "DB4FB61B-6A35-4D3F-BA3E-8041E42CAE85" | "Shutdown" | true
		1     | "iPhone 6s Plus"                        | "A3047E64-65FD-4A12-BC4F-C1BCCB5E7382" | "Booted"   | true
		2     | "iPhone SE (1st generation)"            | "D475FFC7-C11B-4999-B3AC-91574A2BB718" | "Shutdown" | true
		3     | "iPhone 7"                              | "CC7E349F-043B-474C-8755-B052F673D0FE" | "Shutdown" | true
		4     | "iPhone 7 Plus"                         | "86CCE3A4-1C03-4F23-B829-35C08B093B6E" | "Shutdown" | true
		5     | "iPhone 8"                              | "C962F0C8-F67A-4EFD-A951-A0947F6488B1" | "Shutdown" | true
		6     | "iPhone 8 Plus"                         | "3CA11092-DA30-43BE-9596-6458CF6E876E" | "Shutdown" | true
		7     | "iPhone X"                              | "C44E8BA0-C24F-4C9E-B7FE-2AD1E75E6BF4" | "Shutdown" | true
		8     | "iPhone Xs"                             | "923D2FE6-6E68-44BF-AB9B-F5D9B8A46128" | "Shutdown" | true
		9     | "iPhone Xs Max"                         | "63BF7F27-9C59-43E3-8D6D-6A372E9A73DF" | "Shutdown" | true
		10    | "iPhone Xʀ"                             | "D8B44976-3AE3-4CC8-B1A7-D5FFC9F0E556" | "Shutdown" | true
		11    | "iPhone 11"                             | "903CEC28-F7E2-47ED-AC80-3C54CC79F117" | "Shutdown" | true
		12    | "iPhone 11 Pro"                         | "FCE5A775-9044-46EA-8F0B-03D94D3BD78E" | "Shutdown" | true
		13    | "iPhone 11 Pro Max"                     | "6BEC0E18-24BF-4E47-8957-745C2925CF70" | "Shutdown" | true
		14    | "iPhone SE (2nd generation)"            | "ACCAA130-3CCF-4ACD-A469-63A51359EAB4" | "Shutdown" | true
		15    | "iPhone 12 mini"                        | "870044FE-3FF1-4984-A95B-2F46CF04BAB0" | "Shutdown" | true
		16    | "iPhone 12"                             | "CCDBE676-F230-430A-BA18-E6122A291572" | "Shutdown" | true
		17    | "iPhone 12 Pro"                         | "996BF83A-74B2-4DF3-8ED7-2FD867C4608D" | "Shutdown" | true
		18    | "iPhone 12 Pro Max"                     | "94F067EA-00BE-4EC2-A9C9-E9EEFCB3AC47" | "Shutdown" | true
		19    | "iPad mini 4"                           | "D9CC14DD-C513-4CF3-99E1-B3FDC77DDDF6" | "Shutdown" | true
		20    | "iPad Air 2"                            | "30AF79BF-C959-47C6-8E61-BD9385FCE4CF" | "Shutdown" | true
		21    | "iPad Pro (9.7-inch)"                   | "9210D665-00A8-4037-80E1-72E1B44A3DF6" | "Shutdown" | true
		22    | "iPad Pro (12.9-inch) (1st generation)" | "E6BBC60C-5176-449C-A837-A408E3BF46C4" | "Shutdown" | true
		23    | "iPad (5th generation)"                 | "941CA0CC-BECD-4B14-8E41-D561AFFEBA67" | "Shutdown" | true
		24    | "iPad Pro (12.9-inch) (2nd generation)" | "A1EB9F65-8BE0-443F-9E30-1F5CB41793D3" | "Shutdown" | true
		25    | "iPad Pro (10.5-inch)"                  | "922394E2-D5AE-473C-BFF6-96C1BD563354" | "Shutdown" | true
		26    | "iPad (6th generation)"                 | "72B54B94-7B87-4F23-BD12-8E15DB703D47" | "Shutdown" | true
		27    | "iPad (7th generation)"                 | "F4DC82F9-AAB8-4A82-B5F8-591B00923867" | "Shutdown" | true
		28    | "iPad Pro (11-inch) (1st generation)"   | "5667C3D3-95F8-49C1-924E-74A680D422D3" | "Shutdown" | true
		29    | "iPad Pro (12.9-inch) (3rd generation)" | "00A265E8-9D4B-4B3C-A55C-0281B1342A60" | "Shutdown" | true
		30    | "iPad Pro (11-inch) (2nd generation)"   | "E1EA6964-9EE9-4B92-929D-5A378C8F7BC6" | "Booted"   | true
		31    | "iPad Pro (12.9-inch) (4th generation)" | "90555774-692B-471A-93FC-863B9613B20F" | "Shutdown" | true
		32    | "iPad mini (5th generation)"            | "E7AD42CD-1C74-4BCB-83EF-06BE902BA1A9" | "Shutdown" | true
		33    | "iPad Air (3rd generation)"             | "F3C226B5-20BC-4C58-B914-060D5A09E512" | "Shutdown" | true
		34    | "iPad (8th generation)"                 | "6A14C413-1A86-457A-9EAF-530006566C76" | "Shutdown" | true
		35    | "iPad Air (4th generation)"             | "8CE9D844-2D15-43D5-B392-10BE249AE30A" | "Shutdown" | true
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
		index | name                     | identifier                             | state      | available
		0     | "Apple TV"               | "8C0D3467-D5EA-464F-8F71-7144763B01FD" | "Shutdown" | true
		1     | "Apple TV 4K"            | "FFE00484-096C-4DB2-BAA6-5EBC1EB9B719" | "Shutdown" | true
		2     | "Apple TV 4K (at 1080p)" | "02599420-FFE9-4728-9E90-94061F380356" | "Shutdown" | true
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
		0     | "Apple Watch Series 3 - 38mm" | "1D53C6C4-78FA-46B3-B513-F71BBE7E091A" | "Shutdown" | true
		1     | "Apple Watch Series 3 - 42mm" | "EA883D28-13AC-4A39-A09A-4DF5C4888ED3" | "Shutdown" | true
		2     | "Apple Watch Series 4 - 40mm" | "1D459A74-3E59-4A5A-A01C-6659E8E4D30C" | "Shutdown" | true
		3     | "Apple Watch Series 4 - 44mm" | "08E3C57C-9DAE-4379-B989-02F4EA4EC3BC" | "Shutdown" | true
		4     | "Apple Watch Series 5 - 40mm" | "3D1028F3-D08F-4509-A5D9-B6DE883BDF45" | "Shutdown" | true
		5     | "Apple Watch Series 5 - 44mm" | "214C3EDF-B3E0-4385-B984-41D49255AF37" | "Shutdown" | true
		6     | "Apple Watch SE - 40mm"       | "B73771A9-BDDB-4379-B5EF-5C74D780F14A" | "Shutdown" | true
		7     | "Apple Watch SE - 44mm"       | "8D3C8CD8-273A-4330-8C3C-4B14BA855F1F" | "Shutdown" | true
		8     | "Apple Watch Series 6 - 40mm" | "5B1C6FB9-B24F-420E-A2E5-1CF3C1E4F0DF" | "Shutdown" | true
		9     | "Apple Watch Series 6 - 44mm" | "868033F9-BC38-4DED-AAD0-0D7022A8C627" | "Shutdown" | true
	}

	def "has 4 pairs"() {
		given:
		mockSimctlList()

		when:
		simulatorControl.parse()
		def runtime = simulatorControl.getMostRecentRuntime(Type.watchOS)

		then:
		simulatorControl.getDevicePairs().size() == 4
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
		0     | "49F92B2D-653A-4EF3-8FC0-D36FA59072A9" | "868033F9-BC38-4DED-AAD0-0D7022A8C627" | "94F067EA-00BE-4EC2-A9C9-E9EEFCB3AC47"
		1     | "57ED86B0-4E4C-40C1-B055-363F997F5048" | "5B1C6FB9-B24F-420E-A2E5-1CF3C1E4F0DF" | "996BF83A-74B2-4DF3-8ED7-2FD867C4608D"
		2     | "E69B935F-ACE1-4B6E-BCAC-0B2569391628" | "214C3EDF-B3E0-4385-B984-41D49255AF37" | "CCDBE676-F230-430A-BA18-E6122A291572"
		3     | "6EA69788-B203-4A34-A088-067560A060F2" | "3D1028F3-D08F-4509-A5D9-B6DE883BDF45" | "870044FE-3FF1-4984-A95B-2F46CF04BAB0"

	}

}
