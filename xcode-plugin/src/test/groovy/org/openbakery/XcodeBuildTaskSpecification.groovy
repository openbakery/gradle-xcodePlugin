package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.testdouble.SimulatorControlFake
import org.openbakery.xcode.Destination
import org.openbakery.xcode.DestinationResolver
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcode
import org.openbakery.testdouble.XcodeDummy
import spock.lang.Specification


class XcodeBuildTaskSpecification extends Specification {

	Project project
	File temporaryDirectory

	XcodeBuildTask xcodeBuildTask
	XcodeDummy xcodeDummy

	File xcode7_3_1
	File xcode8


	CommandRunner commandRunner = Mock(CommandRunner);

	Destination createDestination(String name, String id) {
		Destination destination = new Destination()
		destination.platform = XcodePlugin.SDK_IPHONESIMULATOR
		destination.name = name
		destination.arch = "i386"
		destination.id = id
		destination.os = "iOS"
		return destination
	}


	def setup() {
		project = ProjectBuilder.builder().build()
		temporaryDirectory = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		temporaryDirectory.mkdirs()

		project.buildDir = new File(temporaryDirectory, "build")

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeBuildTask = project.getTasks().getByPath(XcodePlugin.XCODE_BUILD_TASK_NAME)
		xcodeBuildTask.commandRunner = commandRunner
		xcodeBuildTask.destinationResolver = new DestinationResolver(new SimulatorControlFake("simctl-list-xcode7.txt"))

		xcodeDummy = new XcodeDummy()

	}

	def cleanup() {

		xcodeDummy.cleanup()
		FileUtils.deleteDirectory(temporaryDirectory)

		if (xcode7_3_1 != null) {
			FileUtils.deleteDirectory(xcode7_3_1)
		}
		if (xcode8 != null) {
			FileUtils.deleteDirectory(xcode8)
		}
	}

	def expectedDefaultDirectories() {
		return [
			"DSTROOT=" + new File(project.buildDir, "dst").absolutePath,
			"OBJROOT=" + new File(project.buildDir, "obj").absolutePath,
			"SYMROOT=" + new File(project.buildDir, "sym").absolutePath,
			"SHARED_PRECOMPS_DIR=" + new File(project.buildDir, "shared").absolutePath
		]
	}

	def expectedDisableIndex() {
		return [
			'COMPILER_INDEX_STORE_ENABLE=NO'
		]
	}

	def expectedCodesignSettings() {
		return [
			"CODE_SIGN_IDENTITY=",
			"CODE_SIGNING_REQUIRED=NO",
			"CODE_SIGNING_ALLOWED=NO"
		]
	}

	def expectedDerivedDataPath() {
		return [
			"-derivedDataPath", new File(project.buildDir, "derivedData").absolutePath
		]

	}

	def createCommand(String... commands) {
		def command = []
		command.addAll(commands)
		return command
	}

	def createCommandWithDefaultDirectories(String... commands) {
		def command = createCommand(commands)
		command.addAll(expectedDerivedDataPath())
		command.addAll(expectedDefaultDirectories())
		command.addAll(expectedDisableIndex())
		return command
	}

	def "has xcode"() {
		expect:
		xcodeBuildTask.xcode != null
	}


	def "IllegalArgumentException_when_no_scheme_or_target_given"() {
		when:
		xcodeBuildTask.build()

		then:
		thrown(IllegalArgumentException)
	}

	def "run command with expected scheme and expected default directories"() {
		String command

		project.xcodebuild.type = Type.iOS
		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.simulator = false


		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> command = arguments[1].join(" ") }
		command.startsWith("xcodebuild -scheme myscheme")
		command.contains("-workspace myworkspace")
		command.contains("-configuration Debug")
		command.contains("CODE_SIGN_IDENTITY")
		command.contains("CODE_SIGNING_REQUIRED=NO")
		command.contains("CODE_SIGNING_ALLOWED=NO")
	}


	def "run command with expected scheme and expected directories"() {
		String command

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.type = Type.iOS

		project.xcodebuild.derivedDataPath = new File("build/myDerivedData").absoluteFile
		project.xcodebuild.dstRoot = new File("build/myDst").absoluteFile
		project.xcodebuild.objRoot = new File("build/myObj").absoluteFile
		project.xcodebuild.symRoot = new File("build/mySym").absoluteFile
		project.xcodebuild.sharedPrecompsDir = new File("build/myShared").absoluteFile

		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> command = arguments[1].join(" ") }

		command.startsWith("xcodebuild -scheme myscheme")
		command.contains("-workspace myworkspace")
		command.contains("-configuration Debug")
		command.contains("-derivedDataPath " + new File("build/myDerivedData").absolutePath)
		command.contains("DSTROOT=" + new File("build/myDst").absolutePath)
		command.contains("OBJROOT=" + new File("build/myObj").absolutePath)
		command.contains("SYMROOT=" + new File("build/mySym").absolutePath)
		command.contains("SHARED_PRECOMPS_DIR=" + new File("build/myShared").absolutePath)
		command.contains("-destination platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61")
	}


	def "run command with expected target and expected defaults"() {
		String command

		def target = 'mytarget'
		project.xcodebuild.target = target

		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> command = arguments[1].join(" ") }
		command.startsWith("xcodebuild -configuration Debug")
		command.contains(" -target mytarget")
		command.contains("-destination platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61")
	}

	def "run command with -project "() {
		String command

		def target = 'mytarget'
		project.xcodebuild.target = target

		def projectFile = 'myproject.xcodeproj'
		project.xcodebuild.projectFile = projectFile

		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> command = arguments[1].join(" ") }
		command.contains("-project")
		command.contains(projectFile)
	}


	def "run command without signIdentity"() {
		def commandList
		def expectedCommandList

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.simulator = false

		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = ['xcodebuild',
														 "-scheme", 'myscheme',
														 "-workspace", 'myworkspace',
														 "-configuration", "Debug",
														 "CODE_SIGN_IDENTITY=",
														 "CODE_SIGNING_REQUIRED=NO",
														 "CODE_SIGNING_ALLOWED=NO"
			]
			expectedCommandList.addAll(expectedDerivedDataPath())
			expectedCommandList.addAll(expectedDefaultDirectories())
		}
		Collections.indexOfSubList(commandList, expectedCommandList) == 0
	}

	def "run command without signIdentity osx"() {
		def commandList
		def expectedCommandList

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.type = Type.macOS

		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = ['xcodebuild',
														 "-scheme", 'myscheme',
														 "-workspace", 'myworkspace',
														 "-configuration", "Debug",
			]
			expectedCommandList.addAll(expectedCodesignSettings())
			expectedCommandList.addAll(expectedDerivedDataPath())
			expectedCommandList.addAll(expectedDefaultDirectories())
			expectedCommandList << "-destination" << "platform=OS X,arch=x86_64"

		}
		Collections.indexOfSubList(commandList, expectedCommandList) == 0
	}


	def "run command with arch"() {
		String command

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.arch = ['myarch']


		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> command = arguments[1].join(" ") }
		command.startsWith("xcodebuild -scheme myscheme")
		command.contains("-workspace myworkspace")
		command.contains("-configuration Debug")
		command.contains("ARCHS=myarch")
		command.contains(expectedDerivedDataPath().join(" "))
		command.contains(expectedDefaultDirectories().join(" "))
		command.contains("-destination platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61")
	}


	def "run command with multiple arch"() {

		def commandList
		def expectedCommandList

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.simulator = false

		project.xcodebuild.arch = ['armv', 'armv7s']


		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = ['xcodebuild',
														 "-scheme", 'myscheme',
														 "-workspace", 'myworkspace',
														 "-configuration", "Debug",
			]
			expectedCommandList.addAll(expectedCodesignSettings())
			expectedCommandList.add("ARCHS=armv armv7s")
			expectedCommandList.addAll(expectedDerivedDataPath())
			expectedCommandList.addAll(expectedDefaultDirectories())
		}
		Collections.indexOfSubList(commandList, expectedCommandList) == 0
	}


	def "run command with workspace"() {
		String command

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'

		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> command = arguments[1].join(" ") }
		command.startsWith("xcodebuild -scheme myscheme")
		command.contains("-workspace myworkspace")
		command.contains("-configuration Debug")
		command.contains(expectedDerivedDataPath().join(" "))
		command.contains(expectedDefaultDirectories().join(" "))
		command.contains("-destination platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61")
	}


	def "run command with workspace but without scheme"() {
		String command

		project.xcodebuild.target = 'mytarget'
		project.xcodebuild.workspace = 'myworkspace'

		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> command = arguments[1].join(" ") }
		command.startsWith("xcodebuild -configuration Debug -target mytarget")
		command.contains(expectedDefaultDirectories().join(" "))
		command.contains("-destination platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61")
	}


	def "run command scheme and simulatorbuild"() {
		String command

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.simulator = true

		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> command = arguments[1].join(" ") }
		command.startsWith("xcodebuild -scheme myscheme -workspace myworkspace -configuration Debug")
		command.contains(expectedDerivedDataPath().join(" "))
		command.contains(expectedDefaultDirectories().join(" "))
		command.contains("-destination platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61")
	}


	def "run command scheme and simulatorbuild and arch"() {
		String command

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.simulator = true
		project.xcodebuild.arch = ['i386'];

		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> command = arguments[1].join(" ") }
		command.startsWith("xcodebuild -scheme myscheme -workspace myworkspace -configuration Debug ")
		command.contains(" ARCHS=i386 ")
		command.contains(expectedDerivedDataPath().join(" "))
		command.contains(expectedDefaultDirectories().join(" "))
		command.contains(" -destination platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61")
	}


	def "run command xcodeversion"() {
		String command

		project.xcodebuild.commandRunner = commandRunner
		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >> xcodeDummy.xcodeDirectory.absolutePath

		commandRunner.runWithResult(xcodeDummy.xcodebuild.absolutePath, "-version") >> "Xcode 5.1.1\nBuild version 5B1008"

		project.xcodebuild.target = 'mytarget'

		when:
		xcodeBuildTask.xcode = new Xcode(commandRunner, "5B1008")

		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> command = arguments[1].join(" ") }
		command.startsWith(xcodeDummy.xcodebuild.absolutePath + " -configuration Debug -target mytarget")
		command.contains(expectedDefaultDirectories().join(" "))
		command.contains(" -destination platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61")
	}


	def "depends on"() {
		when:
		def dependsOn = xcodeBuildTask.getDependsOn()

		then:
		dependsOn.contains(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		dependsOn.contains(XcodePlugin.INFOPLIST_MODIFY_TASK_NAME)
		dependsOn.contains(XcodePlugin.COCOAPODS_INSTALL_TASK_NAME)
		dependsOn.contains(XcodePlugin.CARTHAGE_BOOTSTRAP_TASK_NAME)
	}


	def "xcodebuild fails"() {

		given:
		project.xcodebuild.target = "Test"
		commandRunner.run(_, _, _, _) >> {
			throw new CommandRunnerException()
		}

		when:
		xcodeBuildTask.build()

		then:
		thrown(CommandRunnerException)
	}

	def "output file was set"() {
		def givenOutputFile
		project.xcodebuild.target = "Test"

		when:
		xcodeBuildTask.build()


		then:
		1 * commandRunner.setOutputFile(_) >> { arguments -> givenOutputFile = arguments[0] }
		givenOutputFile.absolutePath.endsWith("xcodebuild-output.txt")
		givenOutputFile == new File(project.getBuildDir(), "xcodebuild-output.txt")

	}

	def "build directory is created"() {
		project.xcodebuild.target = "Test"

		when:
		xcodeBuildTask.build()

		then:
		project.getBuildDir().exists()
	}


	def "set target"() {
		when:
		xcodeBuildTask.target = "target"

		then:
		xcodeBuildTask.parameters.target == "target"
	}


	def "set scheme"() {
		when:
		xcodeBuildTask.scheme = "scheme"

		then:
		xcodeBuildTask.parameters.scheme == "scheme"
	}


	def "set simulator"() {
		when:
		xcodeBuildTask.simulator = true

		then:
		xcodeBuildTask.parameters.simulator == true
	}

	def "set simulator false"() {
		when:
		xcodeBuildTask.simulator = false

		then:
		xcodeBuildTask.parameters.simulator == false
	}

	def "set type"() {
		when:
		xcodeBuildTask.type = Type.iOS

		then:
		xcodeBuildTask.parameters.type == Type.iOS
	}

	def "set workspace"() {
		when:
		xcodeBuildTask.workspace = "workspace"

		then:
		xcodeBuildTask.parameters.workspace == "workspace"
	}

	def "set additionalParameters"() {
		when:
		xcodeBuildTask.additionalParameters = "additionalParameters"

		then:
		xcodeBuildTask.parameters.additionalParameters == "additionalParameters"
	}

	def "set additionalParameters List"() {
		when:
		def list = ["First", "Second"]
		xcodeBuildTask.additionalParameters = list

		then:
		list instanceof List
		xcodeBuildTask.parameters.additionalParameters instanceof List
	}


	def "set configuration"() {
		when:
		xcodeBuildTask.configuration = "configuration"

		then:
		xcodeBuildTask.parameters.configuration == "configuration"
	}

	def "set arch"() {
		when:
		xcodeBuildTask.arch = ["i386"]

		then:
		xcodeBuildTask.parameters.arch == ["i386"]
	}


	def "set configuredDestinations"() {
		when:
		Destination destination = new Destination()
		Set<Destination> destinations = [] as Set
		destinations.add(destination)
		xcodeBuildTask.configuredDestinations = destinations

		then:
		xcodeBuildTask.parameters.configuredDestinations.size() == 1
		xcodeBuildTask.parameters.configuredDestinations[0] == destination
	}


	def "create xcode lazy"() {
		given:

		xcode7_3_1 = new File(System.getProperty("java.io.tmpdir"), "Xcode-7.3.1.app")
		xcode8 = new File(System.getProperty("java.io.tmpdir"), "Xcode-8.app")

		new File(xcode7_3_1, "Contents/Developer/usr/bin").mkdirs()
		new File(xcode8, "Contents/Developer/usr/bin").mkdirs()
		new File(xcode7_3_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcode8, "Contents/Developer/usr/bin/xcodebuild").createNewFile()


		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >> xcode7_3_1.absolutePath + "\n" + xcode8.absolutePath
		commandRunner.runWithResult(xcode7_3_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 7.3.1\nBuild version 7D1014"
		commandRunner.runWithResult(xcode8.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 8.0\nBuild version 8A218a"
		commandRunner.runWithResult("xcodebuild", "-version") >> "Xcode 8.0\nBuild version 8A218a"

		when:
		project.xcodebuild.version = "7"

		then:
		xcodeBuildTask.xcode != null
		xcodeBuildTask.xcode.version.major == 7

	}
}
