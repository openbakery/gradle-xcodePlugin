package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.Task

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 22.08.12
 * Time: 17:36
 * To change this template use File | Settings | File Templates.
 */
class XcodebuildArchiveTask extends AbstractXcodeTask {

    XcodebuildArchiveTask() {
        super();
        this.description = "Prepare the app bundle that it can be archive"
    }


    @TaskAction
    def archive() {
        def buildOutputDirectory = new File(project.xcodebuild.symRoot + "/" + project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)
        def appName = getAppBundleName();
        def baseName =  appName.substring(0, appName.size()-4);
        def ipaName =  baseName + ".ipa"
        def dsynName = baseName + ".app.dSYM"
        println(baseName);
        println(ipaName);
        println(dsynName);

        def zipFileName = baseName

        if (project.xcodebuild.archiveVersion != null) {
            println "Rename App";

            File appFile = new File(appName);
            if (appFile.exists()) {
                appFile.renameTo(baseName + project.xcodebuild.archiveVersion + ".app")
            }

            File ipaFile = new File(ipaName);
            if (ipaFile.exists()) {
                ipaFile.renameTo(baseName + project.xcodebuild.archiveVersion + ".ipa")
            }

            File dsymFile = new File(dsynName);
            if (dsymFile.exists()) {
                dsymFile.renameTo(baseName + project.xcodebuild.archiveVersion + ".app.dSYM")
            }
            zipFileName += project.xcodebuild.archiveVersion

        }

        def ant = new AntBuilder()
        ant.zip(destfile: zipFileName + ".zip",
                basedir: buildOutputDirectory,
                includes: "*.app*")

    }
}
