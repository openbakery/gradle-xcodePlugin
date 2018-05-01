package org.openbakery.hockeyapp

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.util.PathHelper
import spock.lang.Specification
/**
 * User: rene
 * Date: 11/11/14
 */
class HockeyAppTaskSpecification extends Specification {
	Project project
	HockeyAppUploadTask hockeyAppUploadTask;

	CommandRunner commandRunner = Mock(CommandRunner)

	File infoPlist

	def setup() {
		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productType = 'app'
		project.xcodebuild.productName = 'Test'

		hockeyAppUploadTask = project.getTasks().getByPath('hockeyapp')

		hockeyAppUploadTask.commandRunner = commandRunner


		File ipaBundle = new File(project.getBuildDir(), "package/Test.ipa")
		FileUtils.writeStringToFile(ipaBundle, "dummy")

		File archiveDirectory = new File(PathHelper.resolveArchiveFolder(project), "Test.xcarchive")
		archiveDirectory.mkdirs()

		infoPlist = new File(archiveDirectory, "Products/Applications/Test.app/Info.plist");
		infoPlist.parentFile.mkdirs();

		File dsymBundle = new File(archiveDirectory, "dSYMs/Test.app.dSYM")
		FileUtils.writeStringToFile(dsymBundle, "dummy")

	}


	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	def "archive"() {
		when:
		hockeyAppUploadTask.prepare()

		File expectedIpa = new File(project.buildDir, "hockeyapp/Test.ipa")
		File expectedDSYM = new File(project.buildDir, "hockeyapp/Test.app.dSYM.zip")

		then:
		expectedIpa.exists()
		expectedDSYM.exists()
	}

	def "archive with bundleSuffix"() {
		given:
		project.xcodebuild.bundleNameSuffix = '-SUFFIX'

		when:
		hockeyAppUploadTask.prepare()

		File expectedIpa = new File(project.buildDir, "hockeyapp/Test-SUFFIX.ipa")
		File expectedZip = new File(project.buildDir, "hockeyapp/Test-SUFFIX.app.dSYM.zip")

		then:
		expectedIpa.exists()
		expectedZip.exists()

	}
}
