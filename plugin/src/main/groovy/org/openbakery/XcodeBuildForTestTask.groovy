package org.openbakery

import org.gradle.api.tasks.TaskAction
import org.openbakery.xcode.Xcodebuild

/**
 * Created by rene on 25.10.16.
 */
class XcodeBuildForTestTask extends AbstractXcodeBuildTask {

	XcodeBuildForTestTask() {
		super()
		dependsOn(
			XcodePlugin.XCODE_CONFIG_TASK_NAME,
			XcodePlugin.SIMULATORS_KILL_TASK_NAME
		)
		this.description = "Create a build for test of the Xcode project"
	}

	Xcodebuild getXcodebuild() {
		return new Xcodebuild(commandRunner, xcode, parameters, destinations)
	}

	@TaskAction
	def buildForTest() {
		parameters = project.xcodebuild.xcodebuildParameters.merge(parameters)

		if (parameters.scheme == null && parameters.target == null) {
			throw new IllegalArgumentException("No 'scheme' or 'target' specified, so do not know what to build");
		}

		File outputDirectory = new File(project.getBuildDir(), "for-testing");
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}

		File outputFile = new File(outputDirectory, "xcodebuild-output.txt")
		commandRunner.setOutputFile(outputFile);

		xcodebuild.executeBuildForTesting(project.projectDir.absolutePath, createXcodeBuildOutputAppender("XcodeBuildForTestTask") , project.xcodebuild.environment)
	}
}
