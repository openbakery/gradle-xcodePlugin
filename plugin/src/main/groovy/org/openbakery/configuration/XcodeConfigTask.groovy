package org.openbakery.configuration

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.filefilter.SuffixFileFilter
import org.apache.commons.lang.StringUtils
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask
import org.openbakery.CommandRunnerException
import org.openbakery.Destination
import org.openbakery.XcodePlugin
import org.openbakery.XcodeProjectFile
import org.openbakery.simulators.SimulatorControl

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
		def projectFile = new File(project.xcodebuild.projectFile, "project.pbxproj")
		xcodeProjectFile = new XcodeProjectFile(project, projectFile)
		xcodeProjectFile.parse()
		project.xcodebuild.projectSettings = xcodeProjectFile.getProjectSettings()
	}

}
