package org.openbakery.configuration

import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask
import org.openbakery.XcodeProjectFile

/**
 * User: rene
 * Date: 25/11/14
 */
class XcodeConfigTask extends AbstractXcodeTask {

	XcodeProjectFile xcodeProjectFile

	XcodeConfigTask() {
		super()
		this.description = "Parses the xcodeproj file and setups the configuration for the build"
	}


	@TaskAction
	void configuration() {
		String absolutePath = "${project.projectDir.absolutePath}/${project.xcodebuild.projectFile}"
		this.project.logger.debug "projectFile: ${project.xcodebuild.projectFile}"
		this.project.logger.debug "absolutePath: " + absolutePath
		def projectFile = new File(absolutePath, "project.pbxproj")
		xcodeProjectFile = new XcodeProjectFile(project, projectFile)
		xcodeProjectFile.parse()
		project.xcodebuild.projectSettings = xcodeProjectFile.getProjectSettings()
	}

}
