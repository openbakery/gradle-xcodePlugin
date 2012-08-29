package org.openbakery

import org.gradle.api.Task
import org.gradle.api.Project
import org.gradle.api.Plugin


class XcodePlugin implements Plugin<Project> {

    private static final String GROUP_NAME = "Xcode"

    void apply(Project project) {
        project.extensions.create("xcodebuild", XcodeBuildPluginExtension)
		project.extensions.create("keychain", KeychainPluginExtension)
		project.extensions.create("provisioning", ProvisioningPluginExtension)
        project.extensions.create("infoplist", InfoPlistExtension)
        project.extensions.create("hockeykit", HockeyKitPluginExtension)

        if (project.hasProperty('hockeykit.appName')) {
            project.hockeykit.appName = project['hockeykit.appName']
        }

        Task keychainCreate = project.task('keychain-create', type: KeychainCreateTask)
		Task xcodebuild = project.task('xcodebuild', type: XcodeBuildTask)
		Task infoplistModify = project.task('infoplist-modify', type: InfoPlistModifyTask)
		Task provisioningInstall = project.task('provisioning-install', type: ProvisioningInstallTask)
        Task archive = project.task("archive", type: XcodeBuildArchiveTask)
        Task hockeyKitManifest = project.task("hockeykit-manifest", type: HockeyKitManifestTask)
        Task hockeyKitArchiveTask = project.task("hockeykit-archive", type: HockeyKitArchiveTask)
        Task hockeyKitImageTask = project.task("hockeykit-image", type: HockeyKitImageTask)

        Task hockey = project.task("hockeykit")
        hockey.description = "Creates a build that can be deployed on a hockeykit Server"
        hockey.dependsOn(hockeyKitArchiveTask, hockeyKitManifest, hockeyKitImageTask)
        hockey.setGroup(GROUP_NAME)

        keychainCreate.setGroup(GROUP_NAME)
        xcodebuild.setGroup(GROUP_NAME)
        infoplistModify.setGroup(GROUP_NAME)
        provisioningInstall.setGroup(GROUP_NAME)
        archive.setGroup(GROUP_NAME)
        hockeyKitManifest.setGroup(GROUP_NAME)
        hockeyKitArchiveTask.setGroup(GROUP_NAME)
        hockeyKitImageTask.setGroup(GROUP_NAME)

        hockeyKitArchiveTask.dependsOn(archive)
        archive.dependsOn("clean")


        Task keychainCleanup =     project.task('keychain-clean', type: KeychainCleanupTask)
		Task xcodebuildCleanup =   project.task('clean', type: XcodeBuildCleanTask)
		Task provisioningCleanup = project.task('provisioning-clean', type: ProvisioningCleanupTask)
		Task hockeyKitCleanTask =  project.task('hockeykit-clean', type: HockeyKitCleanTask)

		xcodebuildCleanup.dependsOn(keychainCleanup)
        xcodebuildCleanup.dependsOn(provisioningCleanup)
        xcodebuildCleanup.dependsOn(hockeyKitCleanTask)
        xcodebuildCleanup.setGroup(GROUP_NAME)

        Task codesign = project.task('codesign', type: CodesignTask)
        codesign.setGroup(GROUP_NAME)

        project.afterEvaluate {

            if (project.hasProperty('infoplist.bundleIdentifier')) {
                project.infoplist.bundleIdentifier = project['infoplist.bundleIdentifier']
            }
            if (project.hasProperty('infoplist.versionExtension')) {
                project.infoplist.versionExtension = project['infoplist.versionExtension']
            }

            if (project.hasProperty('xcodebuild.archiveVersion')) {
                project.xcodebuild.archiveVersion = project['xcodebuild.archiveVersion']
            }
            if (project.hasProperty('xcodebuild.infoPlist')) {
                project.xcodebuild.infoPlist = project['xcodebuild.infoPlist']
            }
            if (project.hasProperty('xcodebuild.configuration')) {
                project.xcodebuild.configuration = project['xcodebuild.configuration']
            }
            if (project.hasProperty('xcodebuild.sdk')) {
                project.xcodebuild.sdk = project['xcodebuild.sdk']
            }
            if (project.hasProperty('xcodebuild.target')) {
                project.xcodebuild.target = project['xcodebuild.target']
            }
            if (project.hasProperty('xcodebuild.buildRoot')) {
                project.xcodebuild.buildRoot = project['xcodebuild.buildRoot']
            }
            if (project.hasProperty('xcodebuild.dstRoot')) {
                project.xcodebuild.dstRoot = project['xcodebuild.dstRoot']
            }
            if (project.hasProperty('xcodebuild.objRoot')) {
                project.xcodebuild.objRoot = project['xcodebuild.objRoot']
            }
            if (project.hasProperty('xcodebuild.symRoot')) {
                project.xcodebuild.symRoot = project['xcodebuild.symRoot']
            }
            if (project.hasProperty('xcodebuild.sharedPrecompsDir')) {
                project.xcodebuild.sharedPrecompsDir = project['xcodebuild.sharedPrecompsDir']
            }
            if (project.hasProperty('xcodebuild.sourceDirectory')) {
                project.xcodebuild.sourceDirectory = project['xcodebuild.sourceDirectory']
            }
            if (project.hasProperty('xcodebuild.signIdentity')) {
                project.xcodebuild.signIdentity = project['xcodebuild.signIdentity']
            }
            if (project.hasProperty('xcodebuild.additionalParameters')) {
                project.xcodebuild.additionalParameters = project['xcodebuild.additionalParameters']
            }
            if (project.hasProperty('xcodebuild.archiveVersion')) {
                project.xcodebuild.archiveVersion = project['xcodebuild.archiveVersion']
            }

            if (project.hasProperty('hockeykit.appName')) {
                project.hockeykit.appName = project['hockeykit.appName']
            }
            if (project.hasProperty('hockeykit.version')) {
                project.hockeykit.version = project['hockeykit.version']
            }
            if (project.hasProperty('hockeykit.outputDirectory')) {
                project.hockeykit.outputDirectory = project['hockeykit.outputDirectory']
            }

            if (project.hasProperty('keychain.certificateUri')) {
                project.keychain.certificateUri = project['keychain.certificateUri']
            }
            if (project.hasProperty('keychain.certificatePassword')) {
                project.keychain.certificatePassword = project['keychain.certificatePassword']
            }
            if (project.hasProperty('keychain.keychainPassword')) {
                project.keychain.keychainPassword = project['keychain.keychainPassword']
            }
            if (project.hasProperty('keychain.destinationRoot')) {
                project.keychain.destinationRoot = project['keychain.destinationRoot']
            }
            if (project.hasProperty('keychain.keychainName')) {
                project.keychain.keychainName = project['keychain.keychainName']
            }

            if (project.hasProperty('keychain.mobileprovisionUri')) {
                project.keychain.mobileprovisionUri = project['keychain.mobileprovisionUri']
            }
            if (project.hasProperty('keychain.destinationRoot')) {
                project.keychain.destinationRoot = project['keychain.destinationRoot']
            }

            if (project.xcodebuild.sdk.startsWith("iphoneos") &&
                    project.xcodebuild.signIdentity != null) {
                archive.dependsOn(codesign)
            } else {
                archive.dependsOn(xcodebuild)
            }

            if (project.infoplist.bundleIdentifier != null ||
                    project.infoplist.versionExtension != null) {
                xcodebuild.dependsOn(infoplistModify)
            }

        }

    }

}


