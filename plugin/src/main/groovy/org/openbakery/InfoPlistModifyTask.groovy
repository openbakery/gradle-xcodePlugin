package org.openbakery

import org.gradle.api.tasks.TaskAction


class InfoPlistModifyTask extends AbstractXcodeTask {

    @TaskAction
    def prepare() {
        def infoPlist = getInfoPlist()
        println "Updating " + infoPlist


        if (project.infoplist.bundleIdentifier != null) {
            runCommand([
                    "/usr/libexec/PlistBuddy",
                    infoPlist,
                    "-c",
                    "Set :CFBundleIdentifier " + project.infoplist.bundleIdentifier
            ])
        }

        if (project.infoplist.versionExtension != null) {


            def currentVersion = runCommandWithResult([
                    "/usr/libexec/PlistBuddy",
                    infoPlist,
                    "-c",
                    "Print :CFBundleVersion"])

            println "Modify CFBundleVersion from " + currentVersion + " to " + currentVersion + project.infoplist.versionExtension

            runCommand([
                    "/usr/libexec/PlistBuddy",
                    infoPlist,
                    "-c",
                    "Set :CFBundleVersion " + currentVersion + project.infoplist.versionExtension])

        }

        // /usr/libexec/PlistBuddy "$INFO_PLIST.plist" -c "Set :CFBundleIdentifier $BUNDLE_IDENTIFIER"

        // plutil -convert xml1 "$INFO_PLIST".plist

    }

}