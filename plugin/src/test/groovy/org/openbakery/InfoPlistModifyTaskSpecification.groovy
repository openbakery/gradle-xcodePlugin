package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.testdouble.PlistHelperStub
import spock.lang.Specification
import spock.lang.Unroll

class InfoPlistModifyTaskSpecification extends Specification {


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

	def "add suffix"() {
		given:
		project.infoplist.bundleIdentifier = 'org.openbakery.test.Example'
		project.infoplist.bundleIdentifierSuffix = '.suffix'

		when:
		task.prepare()

		then:
		plistHelper.getValueFromPlist(infoPlist, "CFBundleIdentifier") == 'org.openbakery.test.Example.suffix'
	}

	@Unroll
	def "add suffix `#bundleId` suffix : `#suffix`"() {
		given:
		BuildConfiguration bcRelease = new BuildConfiguration("Target")
		bcRelease.bundleIdentifier = bundleId

		BuildTargetConfiguration btc = new BuildTargetConfiguration()
		btc.buildSettings[configuration] = bcRelease

		HashMap<String, BuildTargetConfiguration> projectSettings = new HashMap<>()
		projectSettings.put(scheme, btc)

		def extension = project.extensions.getByType(XcodeBuildPluginExtension)
		extension.projectSettings = projectSettings
		extension.scheme = scheme
		extension.configuration = configuration

		project.infoplist.bundleIdentifierSuffix = suffix

		when:
		task.prepare()

		then:
		noExceptionThrown()
		plistHelper.getValueFromPlist(infoPlist, "CFBundleIdentifier") == expectedResult

		where:
		configuration | scheme | suffix   | bundleId        | expectedResult
		"Release"     | "Test" | "suffix" | "he.lllo.world" | "he.lllo.worldsuffix"
		"Release"     | "Test" | ""       | "he.lllo.world" | "he.lllo.world"
		"Release"     | "Test" | null     | "he.lllo.world" | null
		"Debug"       | "Test" | null     | "he.lllo.world" | null
	}

	def "modify BundleIdentifier"() {
		given:
		project.infoplist.bundleIdentifier = 'org.openbakery.test.Example'

		when:
		task.prepare()

		then:
		plistHelper.getValueFromPlist(infoPlist, "CFBundleIdentifier") == 'org.openbakery.test.Example'

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
		project.infoplist.commands = ["Add CFBundleURLTypes:0:CFBundleURLName string", "Add CFBundleURLTypes:0:CFBundleURLSchemes array"]

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

	def "nothing to modify"() {
		given:
		project.xcodebuild.infoPlist = null

		when:
		task.prepare()

		then:
		true // should not fail
	}

	def "has entry to modify but no info plist"() {
		given:
		project.xcodebuild.infoPlist = null
		project.infoplist.shortVersionString = '1.2.3'

		when:
		task.prepare()

		then:
		thrown(IllegalArgumentException.class)
	}

}
