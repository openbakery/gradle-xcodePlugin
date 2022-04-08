package org.openbakery.configuration

import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask
import org.openbakery.XcodeProjectFile

/**
 * User: rene
 * Date: 25/11/14
 */
class XcodeConfigTask extends AbstractXcodeTask {

	@Internal XcodeProjectFile xcodeProjectFile

	XcodeConfigTask() {
		super()
		this.description = "Parses the xcodeproj file and setups the configuration for the build"
	}


	@TaskAction
	void configuration() {
		def projectFile = new File(project.xcodebuild.projectFile, "project.pbxproj")
		xcodeProjectFile = new XcodeProjectFile(project, projectFile)
		xcodeProjectFile.parse()
		project.xcodebuild.projectSettings = xcodeProjectFile.getProjectSettings()
	}

}
