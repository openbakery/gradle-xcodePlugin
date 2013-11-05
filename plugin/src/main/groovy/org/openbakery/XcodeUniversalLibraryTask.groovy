//
//

package org.openbakery

import org.gradle.api.tasks.TaskAction

class XcodeUniversalLibraryTask extends AbstractXcodeTask {
    
    String libPathForTarget(String target) {
        return (project.xcodebuild.symRoot.path + "/" + project.xcodebuild.configuration + "-" + target + "/" + "lib" + project.xcodebuild.target + ".a")
    }

    @TaskAction
    def universalLibrary() {
        def iosLib = new File(libPathForTarget("iphoneos"))
        def simLib = new File(libPathForTarget("iphonesimulator")) 

        if(iosLib.exists() && simLib.exists()) {
            try {
                runCommand(["lipo", "-create", iosLib.path, simLib.path, "-output", project.xcodebuild.buildRoot.path + "/lib" + project.xcodebuild.target + ".a"])
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
