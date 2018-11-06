package org.openbakery.codesign

import org.openbakery.CommandRunner
import org.openbakery.xcode.Xcode
import org.openbakery.xcode.XcodeFake
import spock.lang.Specification

class LipoArchSpecification extends Specification {

	Lipo lipo
	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {

		lipo = new Lipo(new XcodeFake(), commandRunner)
	}

	def tearDown() {
		lipo = null
		commandRunner = null
	}

	def "lipo instance is present"() {
		expect:
		lipo != null
	}

	def "lipo has xcode"() {
		expect:
		lipo.xcode instanceof Xcode
	}

	def "lipo has a command runner"() {
		expect:
		lipo.commandRunner instanceof CommandRunner
	}

	def "get default archs from binary"() {
		given:
		commandRunner

		when:
		def archs = lipo.getArchs("")

		then:
		archs.contains("arm64")
		archs.contains("armv7")
		!archs.contains("arm64e")
	}

	def "get archs executes lipo"() {
		def commandList

		when:
		lipo.getArchs("Dummy")
		then:
		1 * commandRunner.runWithResult(_) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList == [
			"/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
			"-info",
			"Dummy"
		]
	}


	def "get archs executes lipo with propery binary name"() {
		def commandList

		when:
		lipo.getArchs("A/Binary")
		then:
		1 * commandRunner.runWithResult(_) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList == [
			"/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
			"-info",
			"A/Binary"
		]
	}


	def mockInfo(String binary, String archs) {
		def commandList = ["/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
						   "-info",
						   binary]
		String result = "Architectures in the fat file: Dummy are: " + archs
		commandRunner.runWithResult(commandList) >> result
	}

	def "get archs executes lipo and process the result properly"() {
		given:
		mockInfo("Dummy", "armv7 arm64")

		when:
		def archs = lipo.getArchs("Dummy")

		then:
		archs.contains("arm64")
		archs.contains("armv7")
		!archs.contains("arm64e")

	}




	def "get archs executes lipo and process the result properly with arm64e"() {

		given:
		mockInfo("Dummy", "armv7 arm64 arm64e")


		when:
		def archs = lipo.getArchs("Dummy")

		then:
		archs.contains("arm64")
		archs.contains("armv7")
		archs.contains("arm64e")
	}

}
