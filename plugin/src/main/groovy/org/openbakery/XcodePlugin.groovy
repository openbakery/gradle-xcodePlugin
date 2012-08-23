package org.openbakery

import org.gradle.api.Task
import org.gradle.api.Project
import org.gradle.api.Plugin


class XcodePlugin implements Plugin<Project> {


    private static final String GROUP_NAME = "Xcode"

    void apply(Project project) {
        project.extensions.create("xcodebuild", XcodebuildPluginExtension)
		project.extensions.create("keychain", KeychainPluginExtension)
		project.extensions.create("provisioning", ProvisioningPluginExtension)
        project.extensions.create("infoplist", InfoPlistExtension)
        project.extensions.create("hockeykit", HockeyKitPluginExtension)


        project.properties.put("xcodebuild.keychain.name", "foobar");
		
		Task keychainCreate = project.task('keychain-create', type: KeychainCreateTask)
		Task xcodebuild = project.task('xcodebuild', type: XcodeBuildTask)
		Task infoplistModify = project.task('infoplist-modify', type: InfoPlistModifyTask)
		Task provisioningInstall = project.task('provisioning-install', type: ProvisioningInstallTask)
        Task archive = project.task("archive", type: XcodebuildArchiveTask)
        Task hockeyKitManifest = project.task("hockeykit-manifest", type: HockeyKitManifestTask);
        Task hockeyKitArchiveTask = project.task("hockeykit-archive", type: HockeyKitArchiveTask);

        Task hockey = project.task("hockeykit");
        hockey.description = "Creates a build that can be deployed on a hockeykit Server"
        hockey.dependsOn(hockeyKitArchiveTask, hockeyKitManifest);
        hockey.setGroup(GROUP_NAME);


        keychainCreate.setGroup(GROUP_NAME);
        xcodebuild.setGroup(GROUP_NAME);
        infoplistModify.setGroup(GROUP_NAME)
        provisioningInstall.setGroup(GROUP_NAME)
        archive.setGroup(GROUP_NAME)
        hockeyKitManifest.setGroup(GROUP_NAME)
        hockeyKitArchiveTask.setGroup(GROUP_NAME)

        hockeyKitArchiveTask.dependsOn(archive);
        archive.dependsOn("clean")


        Task keychainCleanup =     project.task('keychain-clean', type: KeychainCleanupTask)
		Task xcodebuildCleanup =   project.task('clean', type: XcodeBuildCleanTask)
		Task provisioningCleanup = project.task('provisioning-clean', type: ProvisioningCleanupTask)
		Task hockeyKitCleanTask =  project.task('hockeykit-clean', type: HockeyKitCleanTask);

		xcodebuildCleanup.dependsOn(keychainCleanup);
        xcodebuildCleanup.dependsOn(provisioningCleanup);
        xcodebuildCleanup.dependsOn(hockeyKitCleanTask);
        xcodebuildCleanup.setGroup(GROUP_NAME)

        Task codesign = project.task('codesign', type: CodesignTask)
        codesign.setGroup(GROUP_NAME)


        project.afterEvaluate {
            //println project.xcodebuild.sdk
            //println project.xcodebuild.signIdentity

            if (project.xcodebuild.sdk.startsWith("iphoneos") &&
                    project.xcodebuild.signIdentity != null) {

                archive.dependsOn(codesign)
            } else {
                archive.dependsOn(xcodebuild)
            }

            if (project.infoplist.bundleIdentifier != null ||
                project.infoplist.versionExtension != null) {
                xcodebuild.dependsOn(infoplistModify);
            }
        }






    }

}


