package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.testdouble.SimulatorControlStub
import org.openbakery.xcode.Destination
import org.openbakery.xcode.DestinationResolver
import org.openbakery.xcode.Devices
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcode
import spock.lang.Specification



/**
 * Created by rene on 01.10.15.
 */
class XcodeBuildTaskSpecification extends Specification {

	Project project

	XcodeBuildTask xcodeBuildTask


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
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild/build")

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeBuildTask = project.getTasks().getByPath(XcodePlugin.XCODE_BUILD_TASK_NAME)
		xcodeBuildTask.commandRunner = commandRunner
		xcodeBuildTask.destinationResolver = new DestinationResolver(new SimulatorControlStub("simctl-list-xcode7.txt"))
	}

	def cleanup() {
		FileUtils.deleteDirectory(project.buildDir)

		if (xcode7_3_1 != null) {
			FileUtils.deleteDirectory(xcode7_3_1)
		}
		if (xcode8 != null) {
			FileUtils.deleteDirectory(xcode8)
		}
	}

	def expectedDefaultDirectories() {
		return [
						"DSTROOT=" + new File(project.buildDir,"dst").absolutePath,
						"OBJROOT=" + new File(project.buildDir,"obj").absolutePath,
						"SYMROOT=" + new File(project.buildDir,"sym").absolutePath,
						"SHARED_PRECOMPS_DIR=" + new File(project.buildDir,"shared").absolutePath
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
		def commandList

		project.xcodebuild.type = Type.iOS
		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.simulator = false


		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_,_,_,_) >> {arguments-> commandList=arguments[1]}
		commandList == createCommandWithDefaultDirectories('xcodebuild',
										"-scheme", 'myscheme',
										"-workspace", 'myworkspace',
										"-configuration", "Debug",
										"CODE_SIGN_IDENTITY=",
										"CODE_SIGNING_REQUIRED=NO"
		)

	}


	def "run command with expected scheme and expected directories"() {
		def commandList
		def expectedCommandList

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
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = ['xcodebuild',
														 "-scheme", 'myscheme',
														 "-workspace", 'myworkspace',
														 "-configuration", "Debug",
														 "CODE_SIGN_IDENTITY=",
															"CODE_SIGNING_REQUIRED=NO",
														 "-derivedDataPath", new File("build/myDerivedData").absolutePath,
														 "DSTROOT=" + new File("build/myDst").absolutePath,
														 "OBJROOT=" + new File("build/myObj").absolutePath,
														 "SYMROOT=" + new File("build/mySym").absolutePath,
														 "SHARED_PRECOMPS_DIR=" + new File("build/myShared").absolutePath,
														 "-destination", "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
			]
		}
		commandList == expectedCommandList

	}


	def "run command with expected target and expected defaults"() {
		def commandList
		def expectedCommandList

		def target = 'mytarget'
		project.xcodebuild.target = target

		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_,_,_,_) >> {arguments-> commandList=arguments[1]}
		interaction {
			expectedCommandList = ['xcodebuild',
														 "-configuration", "Debug",
														 "-target", 'mytarget',
														 "CODE_SIGN_IDENTITY=",
														 "CODE_SIGNING_REQUIRED=NO"
			]
			expectedCommandList.addAll(expectedDefaultDirectories())
			expectedCommandList <<  "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
		}
		commandList == expectedCommandList

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
			1 * commandRunner.run(_,_,_,_) >> {arguments-> commandList=arguments[1]}
			interaction {
				expectedCommandList = ['xcodebuild',
															 "-scheme", 'myscheme',
															 "-workspace", 'myworkspace',
															 "-configuration", "Debug",
															 "CODE_SIGN_IDENTITY=",
															 "CODE_SIGNING_REQUIRED=NO"
				]
				expectedCommandList.addAll(expectedDerivedDataPath())
				expectedCommandList.addAll(expectedDefaultDirectories())
			}
			commandList == expectedCommandList
	}

	def "run command without signIdentity osx"() {
		def commandList
		def expectedCommandList

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.type = Type.OSX

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
														 "CODE_SIGNING_REQUIRED=NO"
			]
			expectedCommandList.addAll(expectedDerivedDataPath())
			expectedCommandList.addAll(expectedDefaultDirectories())
			expectedCommandList << "-destination" << "platform=OS X,arch=x86_64"

		}
		commandList == expectedCommandList
	}


	def "run command with arch"() {

		def commandList
		def expectedCommandList

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.arch = ['myarch']


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
														 "ARCHS=myarch"
			]
			expectedCommandList.addAll(expectedDerivedDataPath())
			expectedCommandList.addAll(expectedDefaultDirectories())
			expectedCommandList <<  "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"

		}
		commandList == expectedCommandList
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
														 "CODE_SIGN_IDENTITY=",
														 "CODE_SIGNING_REQUIRED=NO",
														 "ARCHS=armv armv7s"
			]
			expectedCommandList.addAll(expectedDerivedDataPath())
			expectedCommandList.addAll(expectedDefaultDirectories())
		}
		commandList == expectedCommandList
	}



	def "run command with workspace"() {
		def commandList
		def expectedCommandList

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'

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
														 "CODE_SIGNING_REQUIRED=NO"
			]
			expectedCommandList.addAll(expectedDerivedDataPath())
			expectedCommandList.addAll(expectedDefaultDirectories())
			expectedCommandList <<  "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"

		}
		commandList == expectedCommandList
	}



	def "run command with workspace but without scheme"() {

		def commandList
		def expectedCommandList

		project.xcodebuild.target = 'mytarget'
		project.xcodebuild.workspace = 'myworkspace'

		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = ['xcodebuild',
														 "-configuration", "Debug",
														 "-target", 'mytarget',
														 "CODE_SIGN_IDENTITY=",
														 "CODE_SIGNING_REQUIRED=NO"
			]
			expectedCommandList.addAll(expectedDefaultDirectories())
			expectedCommandList <<  "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
		}
		commandList == expectedCommandList

	}


	def "run command scheme and simulatorbuild"() {
		def commandList
		def expectedCommandList

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.simulator = true

		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = ['xcodebuild',
														 "-scheme", 'myscheme',
														 "-workspace", 'myworkspace',
														 "-configuration", 'Debug',
														 "CODE_SIGN_IDENTITY=",
														 "CODE_SIGNING_REQUIRED=NO"
			]
			expectedCommandList.addAll(expectedDerivedDataPath())
			expectedCommandList.addAll(expectedDefaultDirectories())
			expectedCommandList <<  "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
		}
		commandList == expectedCommandList
	}



	def "run command scheme and simulatorbuild and arch"() {
		def commandList
		def expectedCommandList

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.simulator = true
		project.xcodebuild.arch = ['i386'];

		when:
		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = ['xcodebuild',
														 "-scheme", 'myscheme',
														 "-workspace", 'myworkspace',
														 "-configuration", 'Debug',
														 "CODE_SIGN_IDENTITY=",
														 "CODE_SIGNING_REQUIRED=NO",
														 "ARCHS=i386"
			]
			expectedCommandList.addAll(expectedDerivedDataPath())
			expectedCommandList.addAll(expectedDefaultDirectories())
			expectedCommandList <<  "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
		}
		commandList == expectedCommandList
	}


	def "run command xcodeversion"() {
		def commandList
		def expectedCommandList

		project.xcodebuild.commandRunner = commandRunner
		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >> "/Applications/Xcode.app"
		commandRunner.runWithResult("/Applications/Xcode.app/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 5.1.1\nBuild version 5B1008"

		project.xcodebuild.target = 'mytarget'

		when:
		xcodeBuildTask.xcode = new Xcode(commandRunner, "5B1008")

		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = ['/Applications/Xcode.app/Contents/Developer/usr/bin/xcodebuild',
														 "-configuration", 'Debug',
														 "-target", 'mytarget',
														 "CODE_SIGN_IDENTITY=",
														 "CODE_SIGNING_REQUIRED=NO"
			]
			expectedCommandList.addAll(expectedDefaultDirectories())
			expectedCommandList <<  "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
		}
		commandList == expectedCommandList
	}


	def "depends on"() {
		when:
		def dependsOn  = xcodeBuildTask.getDependsOn()

		then:
		dependsOn.contains(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		dependsOn.contains(XcodePlugin.INFOPLIST_MODIFY_TASK_NAME)
	}


	def "xcodebuild fails"() {

		given:
		project.xcodebuild.target = "Test"
		commandRunner.run(_,_,_,_) >> {
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

	def "set devices"() {
		when:
		xcodeBuildTask.devices = Devices.WATCH

		then:
		xcodeBuildTask.parameters.devices == Devices.WATCH
	}



	def "create xcode lazy"() {
		given:

		xcode7_3_1 = new File(System.getProperty("java.io.tmpdir"), "Xcode-7.3.1.app")
		xcode8 = new File(System.getProperty("java.io.tmpdir"), "Xcode-8.app")

		new File(xcode7_3_1, "Contents/Developer/usr/bin").mkdirs()
		new File(xcode8, "Contents/Developer/usr/bin").mkdirs()
		new File(xcode7_3_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcode8, "Contents/Developer/usr/bin/xcodebuild").createNewFile()


		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >>  xcode7_3_1.absolutePath + "\n"  + xcode8.absolutePath
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
