package org.openbakery

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * Created by rene on 18.08.15.
 */
class XcodeBuildPluginExtensionSpecification extends Specification {


	Project project
	XcodeBuildPluginExtension extension;
	CommandRunner commandRunner = Mock(CommandRunner);

	void setup() {
		File projectDir =  new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin

		extension = new XcodeBuildPluginExtension(project)
		extension.commandRunner = commandRunner
		extension.buildSpec.commandRunner = commandRunner;
		extension.infoPlist = "Info.plist";
	}


	def "available macosx destinations"() {

		given:
		extension.sdk = XcodePlugin.SDK_MACOSX

		expect:
		extension.availableDestinations.size() == 1
		extension.availableDestinations[0].name.equals("OS X")

	}
}
