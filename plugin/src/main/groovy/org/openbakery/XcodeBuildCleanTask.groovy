package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction


class XcodeBuildCleanTask extends DefaultTask {

    XcodeBuildCleanTask() {
        super();
        this.description = "Cleans up the generated files from the previous build"
    }

    @TaskAction
    def clean() {

        def dstRoot = new File(project.xcodebuild.dstRoot)
        dstRoot.deleteDir()

        def objRoot = new File(project.xcodebuild.objRoot)
        objRoot.deleteDir()

        def symRoot = new File(project.xcodebuild.symRoot)
        symRoot.deleteDir()

        def sharedPrecompsDir = new File(project.xcodebuild.sharedPrecompsDir)
        sharedPrecompsDir.deleteDir()

    }


}