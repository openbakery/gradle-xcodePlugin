package org.openbakery

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.signing.CodesignTask
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 27.06.14.
 */
class CodesignTaskTest {

	Project project
	CodesignTask codesignTask

	GMockController mockControl
	CommandRunner commandRunnerMock

	@BeforeMethod
	def setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)


		project = ProjectBuilder.builder().build()
		project.apply plugin: org.openbakery.XcodePlugin
		project.buildDir = new File('build').absoluteFile
		project.xcodebuild.sdk = 'iphoneos'
		project.xcodebuild.configuration = 'release'
		project.xcodebuild.symRoot = project.buildDir
		project.xcodebuild.productName = 'My'
		project.xcodebuild.projectType = 'app'


		codesignTask = project.tasks.findByName('codesign')
		codesignTask.setProperty("commandRunner", commandRunnerMock)

	}


	void mockFindPackageApplication() {
		List<String> commandList
		commandList?.clear()
		commandList = ["xcrun", "-sdk", "iphoneos", "--find", "PackageApplication"]
			commandRunnerMock.runWithResult(commandList).returns("src/test/Resource/PackageApplication\n").times(1)
	}

	@Test
	void testCopyPackageApplication() {

		mockFindPackageApplication()

		mockControl.play {
			assert codesignTask.preparePackageApplication().endsWith("build/codesign/PackageApplication")
		}

		File destinationFile = new File("build/codesign/PackageApplication")

		assert destinationFile.exists()

		String fileContents = FileUtils.readFileToString(destinationFile)

		String keychainEntry = "             \"output|o=s\",\n             \"keychain|k=s\","
		assert fileContents.contains(keychainEntry)


		String codeSignKeychainParameter = "    if ( \$opt{keychain} ) {\n" +
						"      push(@codesign_args, '--keychain');\n" +
						"      push(@codesign_args, \$opt{keychain});\n" +
						"    }";

		assert fileContents.contains(codeSignKeychainParameter)

	}

	@Test
	void testCodesign() {
		mockFindPackageApplication()


		FileUtils.writeStringToFile(project.xcodebuild.getApplicationBundle(), "dummy");


		List<String> commandList
		commandList?.clear()
		def packageApplicationScript = new File("build/codesign/PackageApplication").absolutePath

		commandRunnerMock.runWithResult(["xcrun", "-find", "codesign_allocate"]).returns("MYENV");

		commandList = [
						packageApplicationScript,
						"-v",
						project.xcodebuild.getOutputPath().absolutePath + "/My.app",
						"-o",
						project.xcodebuild.getOutputPath().absolutePath + "/My.ipa",
						"--keychain",
						project.xcodebuild.signing.keychainPathInternal.absolutePath
		]

		def environment = ["CODESIGN_ALLOCATE":"MYENV"]


		commandRunnerMock.run(".", commandList, environment, null).times(1)

		mockControl.play {
			codesignTask.codesign()
		}

	}
}
