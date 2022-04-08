package org.openbakery.deploygate

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodeBuildArchiveTask
import spock.lang.Specification

/**
 * User: rene
 * Date: 11/11/14
 */
class DeployGateUploadTaskSpecification extends Specification {

	Project project
	DeployGateUploadTask deployGateUploadTask;

	File infoPlist

	def setup() {

		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productName = 'Test'

		deployGateUploadTask = project.getTasks().getByPath('deploygate')

		File ipaBundle = new File(project.getBuildDir(), "package/Test.ipa")
		FileUtils.writeStringToFile(ipaBundle, "dummy")

		File archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Test.xcarchive")
		archiveDirectory.mkdirs()

		infoPlist = new File(archiveDirectory, "Products/Applications/Test.app/Info.plist");
		infoPlist.parentFile.mkdirs();

	}


	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	def "archive"() {
		when:
		deployGateUploadTask.prepare()
		File expectedIpa = new File(project.buildDir, "deploygate/Test.ipa")

		then:
		expectedIpa.exists()
	}

	def "archive with suffix"() {
		given:
		project.xcodebuild.bundleNameSuffix = '-SUFFIX'

		when:
		deployGateUploadTask.prepare()
		File expectedIpa = new File(project.buildDir, "deploygate/Test-SUFFIX.ipa")

		then:
		expectedIpa.exists()
	}
}
