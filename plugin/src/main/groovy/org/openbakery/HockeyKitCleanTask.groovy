package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 23.08.12
 * Time: 11:47
 * To change this template use File | Settings | File Templates.
 */
class HockeyKitCleanTask extends DefaultTask {

    HockeyKitCleanTask() {
        super();
        this.description = "Cleans up the generated files from the hockey target"
    }

    @TaskAction
    def clean() {

        def outputDirectory = new File(project.hockeykit.outputDirectory)
        outputDirectory.deleteDir()

    }
}
