package org.openbakery

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.bundle.Bundle
import org.openbakery.codesign.Codesign
import org.openbakery.output.TestBuildOutputAppender
import org.openbakery.test.TestResultParser
import org.openbakery.testdouble.SimulatorControlStub
import org.openbakery.testdouble.XcodeFake
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.DestinationResolver
import org.openbakery.xcode.Type
import spock.lang.Specification

/**
 * User: rene
 * Date: 25/10/16
 */
class XcodeTestRunTaskSpecification extends Specification {

	Project project
	CommandRunner commandRunner = Mock(CommandRunner);

	XcodeTestRunTask xcodeTestRunTestTask
	File outputDirectory
	File tmpDir


	def setup() {
		tmpDir = new File(System.getProperty("java.io.tmpdir"), "gxp")
		File projectDir = new File(tmpDir, "gradle-projectDir")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeTestRunTestTask = project.getTasks().getByPath(XcodePlugin.XCODE_TEST_RUN_TASK_NAME)
		xcodeTestRunTestTask.commandRunner = commandRunner
		xcodeTestRunTestTask.xcode = new XcodeFake()
		xcodeTestRunTestTask.destinationResolver = new DestinationResolver(new SimulatorControlStub("simctl-list-xcode8.txt"))
		project.xcodebuild.signing.identity = "my identity"


		outputDirectory = new File(project.buildDir, "test")
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}


	}

	def cleanup() {
		FileUtils.deleteDirectory(tmpDir)
	}



	def createTestBundle(String directoryName) {
		File bundleDirectory = new File(project.getProjectDir(), directoryName)
		File testBundle = new File(bundleDirectory, "Example.testbundle")
		testBundle.mkdirs()
		File xctestrun = new File("src/test/Resource/Example_iphonesimulator.xctestrun")
		FileUtils.copyFile(xctestrun, new File(testBundle, "Example_iphonesimulator.xctestrun"))
	}

	def "instance is of type XcodeBuildForTestTask"() {
		expect:
		xcodeTestRunTestTask instanceof  XcodeTestRunTask
	}


	def "depends on nothing "() {
		when:
		def dependsOn = xcodeTestRunTestTask.getDependsOn()
		then:
		dependsOn.size() == 1
		dependsOn.contains(XcodePlugin.SIMULATORS_KILL_TASK_NAME)
	}


	def "set destinations"() {
		when:
		xcodeTestRunTestTask.destination = [
						"iPhone 6"
		]

		then:
		xcodeTestRunTestTask.parameters.configuredDestinations.size() == 1
		xcodeTestRunTestTask.parameters.configuredDestinations[0].name == "iPhone 6"
	}


	def "set destination global"() {
		when:
		project.xcodebuild.destination = [
						"iPhone 6"
		]
		createTestBundle("test")
		xcodeTestRunTestTask.testRun()

		then:
		xcodeTestRunTestTask.parameters.configuredDestinations.size() == 1
		xcodeTestRunTestTask.parameters.configuredDestinations[0].name == "iPhone 6"

	}


	def "has bundle directory"() {
		when:
		xcodeTestRunTestTask.bundleDirectory  = "test"

		then:
		xcodeTestRunTestTask.bundleDirectory instanceof File
	}

	def "has default bundle directory in project folder"() {

		expect:
		xcodeTestRunTestTask.bundleDirectory instanceof File
		xcodeTestRunTestTask.bundleDirectory == project.file(".")
	}




	def "set configure xctestrun"() {
		given:
		createTestBundle("test")

		when:
		xcodeTestRunTestTask.bundleDirectory  = "test"
		xcodeTestRunTestTask.testRun()

		then:
		xcodeTestRunTestTask.parameters.xctestrun instanceof List
		xcodeTestRunTestTask.parameters.xctestrun.size() == 1
		xcodeTestRunTestTask.parameters.xctestrun[0].path.endsWith("Example.testbundle/Example_iphonesimulator.xctestrun")
	}


	def "use xctestrun with absolute path"() {
		given:
		createTestBundle("test")

		when:
		def xctestrun =  xcodeTestRunTestTask.getXcruntestFiles()

		then:
		xctestrun instanceof List
		xctestrun.size() == 1
		xctestrun[0].isAbsolute() == true
	}



	def "run xcodebuild executeTestWithoutBuilding"() {
		given:
		def commandList
		createTestBundle("test")

		when:
		xcodeTestRunTestTask.testRun()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		commandList.contains("test-without-building")
		commandList.contains("-xctestrun")

	}

	def "has output appender"() {
		def outputAppender
		given:
		createTestBundle("test")

		when:

		xcodeTestRunTestTask.testRun()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> outputAppender = arguments[3] }
		outputAppender instanceof TestBuildOutputAppender
	}

	def "has output appender with full progress"() {
		def outputAppender
		given:
		createTestBundle("test")

		when:
		xcodeTestRunTestTask.fullProgress = true
		xcodeTestRunTestTask.testRun()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> outputAppender = arguments[3] }
		outputAppender instanceof TestBuildOutputAppender
		((TestBuildOutputAppender)outputAppender).fullProgress == true
	}

	def "delete derivedData/Logs/Test before test is executed"() {
		given:
		createTestBundle("test")
		project.xcodebuild.target = "Test"


		def testDirectory = new File(project.xcodebuild.derivedDataPath, "Logs/Test")
		FileUtils.writeStringToFile(new File(testDirectory, "foobar"), "dummy");

		when:
		xcodeTestRunTestTask.testRun()

		then:
		!testDirectory.exists()
	}


	def fakeTestRun() {
		xcodeTestRunTestTask.destinationResolver.simulatorControl = new SimulatorControlStub("simctl-list-xcode7.txt");

		project.xcodebuild.destination {
			name = "iPad 2"
		}
		project.xcodebuild.destination {
			name = "iPhone 4s"
		}


		xcodeTestRunTestTask.setOutputDirectory(outputDirectory);
		File xcodebuildOutput = new File(project.buildDir, 'test/xcodebuild-output.txt')
		FileUtils.writeStringToFile(xcodebuildOutput, "dummy")
	}

	def "parse test-result.xml gets stored"() {
		given:
		createTestBundle("test")
		project.xcodebuild.target = "Test"

		when:
		xcodeTestRunTestTask.testRun()

		def testResult = new File(outputDirectory, "test-results.xml")
		then:
		testResult.exists()
	}


	def "has TestResultParser"() {
		given:
		createTestBundle("test")

		project.xcodebuild.target = "Test"

		when:
		fakeTestRun()
		xcodeTestRunTestTask.testRun()

		then:
		xcodeTestRunTestTask.testResultParser instanceof TestResultParser
		xcodeTestRunTestTask.testResultParser.testSummariesDirectory == new File(project.buildDir, "derivedData/Logs/Test")
		xcodeTestRunTestTask.testResultParser.destinations.size() == 2

	}

	def "output file was set"() {
		given:
		createTestBundle("test")

		def givenOutputFile
		project.xcodebuild.target = "Test"

		when:
		xcodeTestRunTestTask.testRun()

		then:
		1 * commandRunner.setOutputFile(_) >> { arguments -> givenOutputFile = arguments[0] }
		givenOutputFile.absolutePath.endsWith("xcodebuild-output.txt")
		givenOutputFile == new File(project.getBuildDir(), "test/xcodebuild-output.txt")

	}



	def "simulator build has no keychain dependency"() {
		when:
		xcodeTestRunTestTask = project.getTasks().getByPath(XcodePlugin.XCODE_TEST_RUN_TASK_NAME)
		xcodeTestRunTestTask.destination {
			platform = "iOS Simulator"
			name = "iPad Air"
		}
		xcodeTestRunTestTask.simulator = true
		project.evaluate()

		then:
		!xcodeTestRunTestTask.getTaskDependencies().getDependencies().contains(project.getTasks().getByName(XcodePlugin.KEYCHAIN_CREATE_TASK_NAME))
		!xcodeTestRunTestTask.getTaskDependencies().getDependencies().contains(project.getTasks().getByName(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME))
	}

	def "when keychain dependency then also has finalized keychain remove"() {
		when:
		def bundleDirectory = createTestBundleForDeviceBuild()
		xcodeTestRunTestTask = project.getTasks().getByPath(XcodePlugin.XCODE_TEST_RUN_TASK_NAME)
		xcodeTestRunTestTask.setBundleDirectory(bundleDirectory)
		project.evaluate()

		def finalized = xcodeTestRunTestTask.finalizedBy.getDependencies()
		def keychainRemoveTask = project.getTasks().getByPath(XcodePlugin.KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME)
		then:
		finalized.contains(keychainRemoveTask)
	}


	def "has keychain dependency if device run"() {
		when:
		def bundleDirectory = createTestBundleForDeviceBuild()
		xcodeTestRunTestTask = project.getTasks().getByPath(XcodePlugin.XCODE_TEST_RUN_TASK_NAME)
		xcodeTestRunTestTask.setBundleDirectory(bundleDirectory)
		project.evaluate()

		then:
		xcodeTestRunTestTask.getTaskDependencies().getDependencies().contains(project.getTasks().getByName(XcodePlugin.KEYCHAIN_CREATE_TASK_NAME))
		xcodeTestRunTestTask.getTaskDependencies().getDependencies().contains(project.getTasks().getByName(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME))
	}


	def "has codesign"() {
		when:
		def bundleDirectory = createTestBundleForDeviceBuild()
		xcodeTestRunTestTask = project.getTasks().getByPath(XcodePlugin.XCODE_TEST_RUN_TASK_NAME)
		xcodeTestRunTestTask.setBundleDirectory(bundleDirectory)
		project.evaluate()

		then:
		xcodeTestRunTestTask.getCodesign() != null
	}

	def "codesign type was set"() {
		when:

		project.xcodebuild.type = Type.macOS
		def bundleDirectory = createTestBundleForDeviceBuild()
		xcodeTestRunTestTask = project.getTasks().getByPath(XcodePlugin.XCODE_TEST_RUN_TASK_NAME)
		xcodeTestRunTestTask.setBundleDirectory(bundleDirectory)
		project.evaluate()

		then:
		xcodeTestRunTestTask.getCodesign().codesignParameters.type == Type.macOS
	}

	def "simulator has no codesign"() {
		when:
		xcodeTestRunTestTask = project.getTasks().getByPath(XcodePlugin.XCODE_TEST_RUN_TASK_NAME)
		xcodeTestRunTestTask.simulator = true
		xcodeTestRunTestTask.destination {
			platform = "iOS Simulator"
			name = "iPad Air"
		}
		project.evaluate()

		then:
		xcodeTestRunTestTask.getCodesign() == null
	}


	def createTestBundleForDeviceBuild() {
		File bundleDirectory = new File(project.getProjectDir(), "for-testing")
		File testBundle = new File(bundleDirectory, "DemoApp-iOS.testbundle")
		File appBundle = new File(testBundle, "Debug-iphoneos/DemoApp.app")
		appBundle.mkdirs()

		def frameworks = ["IDEBundleInjection.framework", "OBTableViewController.framework", "XCTest.framework"]
		for (String framework : frameworks) {
			File frameworkBundle = new File(appBundle, "Frameworks/" + framework)
			frameworkBundle.mkdirs()
		}
		File xctestrun = new File("../libtest/src/main/Resource/DemoApp_iphoneos10.1-arm64.xctestrun")
		FileUtils.copyFile(xctestrun, new File(testBundle, "DemoApp_iphoneos10.1-arm64.xctestrun"))

		File infoPlist = new File(appBundle, "Info.plist")
		PlistHelper helper = new PlistHelper(new CommandRunner())
		helper.create(infoPlist)
		helper.addValueForPlist(infoPlist, "CFBundleIdentifier", "org.openbakery.test.Example")

		return bundleDirectory
	}


	void mockEntitlementsFromPlist(File provisioningProfile) {
		def commandList = ['security', 'cms', '-D', '-i', provisioningProfile.absolutePath]
		String result = new File('../libtest/src/main/Resource/entitlements.plist').text
		commandRunner.runWithResult(commandList) >> result
		String basename = FilenameUtils.getBaseName(provisioningProfile.path)
		File plist = new File(tmpDir, "/provision_" + basename + ".plist")
		commandList = ['/usr/libexec/PlistBuddy', '-x', plist.absolutePath, '-c', 'Print Entitlements']
		commandRunner.runWithResult(commandList) >> result
	}


	def "bundle is codesigned"() {
		given:
		def commandList
		def bundleDirectory = createTestBundleForDeviceBuild()
		def mobileprovision = new File("../libtest/src/main/Resource/test.mobileprovision")
		mockEntitlementsFromPlist(mobileprovision)
		project.xcodebuild.signing.addMobileProvisionFile(mobileprovision)

		xcodeTestRunTestTask = project.getTasks().getByPath(XcodePlugin.XCODE_TEST_RUN_TASK_NAME)
		xcodeTestRunTestTask.destination {
			platform = "iOS"
			name = "Dummy"
		}
		xcodeTestRunTestTask.simulator = false
		project.evaluate()

		xcodeTestRunTestTask.setBundleDirectory(bundleDirectory )

		when:
		xcodeTestRunTestTask.testRun()

		then:

		5 * commandRunner.run(_, _) >> { arguments -> commandList = arguments[0] }
		commandList.contains("/usr/bin/codesign")
	}



	def "sign test bundle path"() {
		given:
		def plistHelper = new PlistHelper(new CommandRunner())
		def bundleDirectory = createTestBundleForDeviceBuild()
		def mobileprovision = new File("../libtest/src/main/Resource/test.mobileprovision")
		mockEntitlementsFromPlist(mobileprovision)
		project.xcodebuild.signing.addMobileProvisionFile(mobileprovision)
		xcodeTestRunTestTask = project.getTasks().getByPath(XcodePlugin.XCODE_TEST_RUN_TASK_NAME)
		xcodeTestRunTestTask.destination {
			platform = "iOS"
			name = "Dummy"
		}
		xcodeTestRunTestTask.simulator = false
		project.evaluate()
		xcodeTestRunTestTask.setBundleDirectory(bundleDirectory )

		def codesign = Mock(Codesign)
		xcodeTestRunTestTask.codesign = codesign


		when:
		xcodeTestRunTestTask.testRun()


		then:
		1 * codesign.sign(new Bundle(new File(bundleDirectory, "DemoApp-iOS.testbundle/Debug-iphoneos/DemoApp.app"), Type.iOS, plistHelper))
		1 * codesign.sign(new Bundle(new File(bundleDirectory, "DemoApp-iOS.testbundle/Debug-iphoneos/DemoApp.app/PlugIns/Tests.xctest"), Type.iOS, plistHelper))

	}


	def "executing xctestrun with project that has no tests throws an exception"() {
		given:

		when:
		def xctestrun =  xcodeTestRunTestTask.getXcruntestFiles()
		xcodeTestRunTestTask.testRun()

		then:
		xctestrun instanceof List
		xctestrun.size() == 0
		def exception =  thrown(IllegalStateException)
		exception.message == "No tests found!"
	}
}
