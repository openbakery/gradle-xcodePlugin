package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.stubs.PlistHelperStub
import spock.lang.Specification

/**
 * Created by rene on 25.07.14.
 */
class InfoPlistModifyTaskSpecification extends Specification{


	Project project
	File projectDir
	InfoPlistModifyTask task
	File infoPlist
	PlistHelperStub plistHelper = new PlistHelperStub()

	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin


		projectDir.mkdirs()


		project.xcodebuild.infoPlist = "App-Info.plist"

		task = project.tasks.findByName('infoplistModify')
		task.commandRunner = commandRunner
		task.plistHelper = plistHelper

		infoPlist = new File(task.project.projectDir, "App-Info.plist")
		FileUtils.writeStringToFile(infoPlist, "dummy")


	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	def "modify BundleIdentifier"() {
		given:
		project.infoplist.bundleIdentifier = 'org.openbakery.Example'

		when:
		task.prepare()

		then:
		plistHelper.getValueFromPlist(infoPlist, "CFBundleIdentifier") == 'org.openbakery.Example'

	}

	def "modifyCommand single"() {
		given:
		project.infoplist.commands = "Add CFBundleURLTypes:0:CFBundleURLName string"

		when:
		task.prepare()

		then:
		plistHelper.plistCommands[0] == "Add CFBundleURLTypes:0:CFBundleURLName string"

	}

	def "modify command multiple"() {
		project.infoplist.commands = ["Add CFBundleURLTypes:0:CFBundleURLName string", "Add CFBundleURLTypes:0:CFBundleURLSchemes array" ]

		when:
		task.prepare()

		then:
		plistHelper.plistCommands[0] == "Add CFBundleURLTypes:0:CFBundleURLName string"
		plistHelper.plistCommands[1] == "Add CFBundleURLTypes:0:CFBundleURLSchemes array"

	}

	def "modify version"() {
		given:
		project.infoplist.version = '1.0.0'

		when:
		task.prepare()

		then:
		plistHelper.getValueFromPlist(infoPlist, "CFBundleVersion") == "1.0.0"
	}

	def "modify ShortVersion"() {
		given:
		project.infoplist.shortVersionString = '1.2.3'

		when:
		task.prepare()

		then:
		plistHelper.getValueFromPlist(infoPlist, "CFBundleShortVersionString") == "1.2.3"

	}

}
