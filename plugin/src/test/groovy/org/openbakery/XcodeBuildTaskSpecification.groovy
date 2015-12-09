package org.openbakery

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.stubs.SimulatorControlStub
import spock.lang.Specification



/**
 * Created by rene on 01.10.15.
 */
class XcodeBuildTaskSpecification extends Specification {

	Project project

	XcodeBuildTask xcodeBuildTask


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
		project.buildDir = new File('build').absoluteFile


		project.apply plugin: org.openbakery.XcodePlugin

		xcodeBuildTask = project.getTasks().getByPath(XcodePlugin.XCODE_BUILD_TASK_NAME)
		xcodeBuildTask.commandRunner = commandRunner
		project.xcodebuild.simulatorControl = new SimulatorControlStub("simctl-list-xcode7.txt");


	}

	def expectedDefaultDirectories() {
		return [
						"-derivedDataPath", new File("build/derivedData").absolutePath,
						"DSTROOT=" + new File("build/dst").absolutePath,
						"OBJROOT=" + new File("build/obj").absolutePath,
						"SYMROOT=" + new File("build/sym").absolutePath,
						"SHARED_PRECOMPS_DIR=" + new File("build/shared").absolutePath
		]
	}

	def createCommand(String... commands) {
		def command = []
		command.addAll(commands)
		return command
	}

	def createCommandWithDefaultDirectories(String... commands) {
		def command = createCommand(commands)
		command.addAll(expectedDefaultDirectories())
		return command
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
														 "-sdk", "macosx",
														 "-configuration", "Debug",
														 "CODE_SIGN_IDENTITY=",
														 "CODE_SIGNING_REQUIRED=NO"
			]
			expectedCommandList.addAll(expectedDefaultDirectories())
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
														 "ARCHS=myarch"
			]
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
			]
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
			]
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
														 "ARCHS=i386"
			]
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
		project.xcodebuild.version = '5B1008';

		xcodeBuildTask.build()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = ['/Applications/Xcode.app/Contents/Developer/usr/bin/xcodebuild',
														 "-configuration", 'Debug',
														 "-target", 'mytarget',
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


}
