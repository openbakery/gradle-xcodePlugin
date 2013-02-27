package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction


class XcodeBuildCleanTask extends DefaultTask {

	XcodeBuildCleanTask() {
		super()
		this.description = "Cleans up the generated files from the previous build"
	}

	@TaskAction
	def clean() {
		project.xcodebuild.dstRoot.deleteDir()
		project.xcodebuild.objRoot.deleteDir()
		project.xcodebuild.symRoot.deleteDir()
		project.xcodebuild.sharedPrecompsDir.deleteDir()
		project.buildDir.deleteDir()
	}

}