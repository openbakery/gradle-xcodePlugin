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
			new File(project.xcodebuild.dstRoot).deleteDir()
			new File(project.xcodebuild.objRoot).deleteDir()
			new File(project.xcodebuild.symRoot).deleteDir()
			new File(project.xcodebuild.sharedPrecompsDir).deleteDir()
			new File(project.xcodebuild.buildRoot).deleteDir()
		}

}