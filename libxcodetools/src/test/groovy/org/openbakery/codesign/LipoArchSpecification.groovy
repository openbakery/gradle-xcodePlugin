package org.openbakery.codesign

import org.openbakery.CommandRunner
import org.openbakery.testdouble.XcodeFake
import org.openbakery.tools.Lipo
import org.openbakery.xcode.Xcode
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
		def archs = lipo.getArchs(new File(""))

		then:
		archs.contains("arm64")
		archs.contains("armv7")
		!archs.contains("arm64e")
	}

	def "get archs executes lipo"() {
		def commandList

		def file = new File("Dummy")

		when:
		lipo.getArchs(file)
		then:
		1 * commandRunner.runWithResult(_) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList == [
			"/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
			"-info",
			file.absolutePath
		]
	}


	def "get archs executes lipo with propery binary name"() {
		def commandList

		when:
		lipo.getArchs(new File("A/Binary"))
		then:
		1 * commandRunner.runWithResult(_) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList == [
			"/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
			"-info",
			new File("A/Binary").absolutePath
		]
	}


	def mockInfo(File binary, String archs) {
		def commandList = ["/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
						   "-info",
						   binary.absolutePath]
		String result = "Architectures in the fat file: Dummy are: " + archs
		commandRunner.runWithResult(commandList) >> result
	}

	def "get archs executes lipo and process the result properly"() {
		given:
		mockInfo(new File("Dummy"), "armv7 arm64")

		when:
		def archs = lipo.getArchs(new File("Dummy"))

		then:
		archs.contains("arm64")
		archs.contains("armv7")
		!archs.contains("arm64e")

	}




	def "get archs executes lipo and process the result properly with arm64e"() {

		given:
		mockInfo(new File("Dummy"), "armv7 arm64 arm64e")


		when:
		def archs = lipo.getArchs(new File("Dummy"))

		then:
		archs.contains("arm64")
		archs.contains("armv7")
		archs.contains("arm64e")
	}

}
