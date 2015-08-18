package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * Created by rene on 18.08.15.
 */
class InfoPlistModifyTaskSpecification extends Specification {

	Project project
	File projectDir
	InfoPlistModifyTask task
	File infoPlist

	def plistHelper = Mock(PlistHelper)

	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin


		projectDir.mkdirs()


		project.xcodebuild.infoPlist = "App-Info.plist"

		task = project.tasks.findByName('infoplistModify')
		task.plistHelper = plistHelper

		infoPlist = new File(task.project.projectDir, "App-Info.plist")
		FileUtils.writeStringToFile(infoPlist, "dummy")


	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	def "set bundle identifier"() {

		setup:
		project.infoplist.bundleIdentifier = 'org.openbakery.Example'


		when:
		task.executeTask()

		then:
		1 * plistHelper.setValueForPlist(infoPlist, "CFBundleIdentifier", 'org.openbakery.Example')

	}


	def "modify single command"() {
		setup:
		project.infoplist.commands = "Add CFBundleURLTypes:0:CFBundleURLName string"

		when:
		task.executeTask()

		then:
		1 * plistHelper.setValueForPlist(infoPlist, "Add CFBundleURLTypes:0:CFBundleURLName string")

	}

	def "modify muliple commands"() {
		setup:
		project.infoplist.commands = ["Add CFBundleURLTypes:0:CFBundleURLName string", "Add CFBundleURLTypes:0:CFBundleURLSchemes array" ]

		when:
		task.executeTask()

		then:
		1 * plistHelper.setValueForPlist(infoPlist, "Add CFBundleURLTypes:0:CFBundleURLName string")
		1 * plistHelper.setValueForPlist(infoPlist, "Add CFBundleURLTypes:0:CFBundleURLSchemes array")
	}

	def "modify version"() {

		setup:
		project.infoplist.version = '1.0.0'


		when:
		task.executeTask()

		then:
		1 * plistHelper.setValueForPlist(infoPlist, "CFBundleVersion", "1.0.0")

	}


	def "test modify short version"() {
		setup:
		project.infoplist.shortVersionString = '1.2.3'

		when:
		task.executeTask()

		then:
		1 * plistHelper.getValueFromPlist(infoPlist, "CFBundleShortVersionString")
		1 * plistHelper.setValueForPlist(infoPlist, "CFBundleShortVersionString", "1.2.3")




	}

}
