//
//

package org.openbakery

import org.gradle.api.tasks.TaskAction
import org.apache.commons.io.FileUtils

class XcodeUniversalLibraryTask extends AbstractXcodeTask {

    String configurationPathForTarget(String target) {
        return (project.xcodebuild.symRoot.path + "/" + project.xcodebuild.configuration + "-" + target)
    }

    String libPathForTarget(String target) {
        return (configurationPathForTarget(target) + "/" + "lib" + project.xcodebuild.target + ".a")
    }

    @TaskAction
    def universalLibrary() {
        def iosLib = new File(libPathForTarget("iphoneos"))
        def simLib = new File(libPathForTarget("iphonesimulator")) 

        if(iosLib.exists() && simLib.exists()) {
            try {
                def uniLib = new File(project.xcodebuild.symRoot.path + "/" + project.xcodebuild.configuration + "-universal")
                uniLib.exists() ? uniLib.deleteDir() : uniLib.mkdirs()

                def iosHeaders = new File(configurationPathForTarget("iphoneos") + "/include")
                def uniHeaders = new File(uniLib.path + "/include")

                FileUtils.copyDirectory(iosHeaders, uniHeaders)

                runCommand(["xcrun", "-sdk", "iphoneos",
                        "lipo", "-create", iosLib.path, simLib.path, "-output", uniLib.path + "/lib" + project.xcodebuild.target + ".a"])
            } catch (Exception e) {
                println "----- RUN COMMAND FAIL:" + e.printStackTrace()
            }
        }
        else {
            println "----- UNABLE TO CREATE UNIVERSAL LIBRARY"

            if(!iosLib.exists()) {
                println "----- ios not exist:" + iosLib.path
            }

            if(!simLib.exists()) {
                println "----- simulator not exist:" + simLib.path
            }
        }
    }
}
