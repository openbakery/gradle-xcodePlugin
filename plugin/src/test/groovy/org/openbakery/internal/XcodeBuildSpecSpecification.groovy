package org.openbakery.internal

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.PlistHelper
import org.openbakery.XcodePlugin
import spock.lang.Specification

/**
 * Created by rene on 18.08.15.
 */
class XcodeBuildSpecSpecification extends Specification {


	XcodeBuildSpec buildSpec
	XcodeBuildSpec parentBuildSpec
	Project project

	PlistHelper plistHelper = Mock(PlistHelper)

	void setup() {
		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin


		parentBuildSpec = new XcodeBuildSpec(project);
		parentBuildSpec.plistHelper = plistHelper

		buildSpec = new XcodeBuildSpec(project, parentBuildSpec);
		buildSpec.plistHelper = plistHelper
		buildSpec.productName = "child"
	}


	def "bundle name from info plist"() {

		setup:
		buildSpec.infoPlist = "Info.plist"
		plistHelper.getValueFromPlist(new File(project.projectDir, "Info.plist"), "CFBundleName") >> "Test"

		expect:
		buildSpec.bundleName.equals("Test")
	}


}
