package org.openbakery.appstore

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner

class NotarizeTaskSpecification extends spock.lang.Specification {


	Project project
	NotarizeTask task
	File infoPlist

	CommandRunner commandRunner = Mock(CommandRunner)
	File zipBundle;

	def setup() {

		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		task = (NotarizeTask)project.tasks.findByName('notarize')
		if (task != null) {
			task.commandRunner = commandRunner
		}

		zipBundle = new File(project.getBuildDir(), "package/Test.zip")
		FileUtils.writeStringToFile(zipBundle, "dummy")

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	def "notarize task is present"() {
		expect:
		task != null
	}


	/*
	def "zip is missing throws exception"() {
		given:
		FileUtils.deleteDirectory(project.projectDir)

		when:
		task.upload()

		then:
		thrown(IllegalStateException.class)

	}
*/

	// xcrun altool --notarize-app --primary-bundle-id app.marmota.presentation --asc-provider RenePirringer160775877 --username rene.pirringer@ciqua.com   --file marmota.zip

}
