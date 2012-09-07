package org.openbakery

import org.gradle.api.Task
import org.gradle.api.Project
import org.gradle.api.Plugin


class XcodePlugin implements Plugin<Project> {

	private static final String GROUP_NAME = "Xcode"

	void apply(Project project) {
		System.setProperty("java.awt.headless", "true");

		project.extensions.create("xcodebuild", XcodeBuildPluginExtension)
		project.extensions.create("keychain", KeychainPluginExtension)
		project.extensions.create("provisioning", ProvisioningPluginExtension)
		project.extensions.create("infoplist", InfoPlistExtension)
		project.extensions.create("hockeykit", HockeyKitPluginExtension)
		project.extensions.create("testflight", TestFlightPluginExtension)

		Task keychainCreate = project.task('keychain-create', type: KeychainCreateTask)
		Task xcodebuild = project.task('xcodebuild', type: XcodeBuildTask)
		Task infoplistModify = project.task('infoplist-modify', type: InfoPlistModifyTask)
		Task provisioningInstall = project.task('provisioning-install', type: ProvisioningInstallTask)
		Task archive = project.task("archive", type: XcodeBuildArchiveTask)
		Task hockeyKitManifest = project.task("hockeykit-manifest", type: HockeyKitManifestTask)
		Task hockeyKitArchiveTask = project.task("hockeykit-archive", type: HockeyKitArchiveTask)
		Task hockeyKitImageTask = project.task("hockeykit-image", type: HockeyKitImageTask)
		Task testFlightPrepare = project.task("testflight-prepare", type: TestFlightPrepareTask)
		Task testFlightUpload = project.task("testflight", type: TestFlightUploadTask);

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
		testFlightPrepare.setGroup(GROUP_NAME)
		testFlightUpload.setGroup(GROUP_NAME)

		hockeyKitArchiveTask.dependsOn(archive)
		archive.dependsOn("clean")


		Task keychainCleanup = project.task('keychain-clean', type: KeychainCleanupTask)
		Task xcodebuildCleanup = project.task('clean', type: XcodeBuildCleanTask)
		Task provisioningCleanup = project.task('provisioning-clean', type: ProvisioningCleanupTask)
		Task hockeyKitCleanTask = project.task('hockeykit-clean', type: HockeyKitCleanTask)
		Task testFlightClean = project.task("testflight-clean", type: TestFlightCleanTask);

		xcodebuildCleanup.dependsOn(keychainCleanup)
		xcodebuildCleanup.dependsOn(provisioningCleanup)
		xcodebuildCleanup.dependsOn(hockeyKitCleanTask)
		xcodebuildCleanup.dependsOn(testFlightClean);
		xcodebuildCleanup.setGroup(GROUP_NAME)


		Task codesign = project.task('codesign', type: CodesignTask)
		codesign.setGroup(GROUP_NAME)

		project.afterEvaluate {

			if (project.hasProperty('infoplist.bundleIdentifier')) {
				project.infoplist.bundleIdentifier = project['infoplist.bundleIdentifier']
			}
			if (project.hasProperty('infoplist.bundleIdentifierSuffix')) {
				project.infoplist.bundleIdentifierSuffix = project['infoplist.bundleIdentifierSuffix']
			}
			if (project.hasProperty('infoplist.version')) {
				project.infoplist.version = project['infoplist.version']
			}
			if (project.hasProperty('infoplist.versionPrefix')) {
				project.infoplist.versionPrefix = project['infoplist.versionPrefix']
			}
			if (project.hasProperty('infoplist.versionSuffix')) {
				project.infoplist.versionSuffix = project['infoplist.versionSuffix']
			}


			if (project.hasProperty('infoplist.shortVersionString')) {
				project.infoplist.shortVersionString = project['infoplist.shortVersionString']
			}
			if (project.hasProperty('infoplist.shortVersionStringSuffix')) {
				project.infoplist.shortVersionStringSuffix = project['infoplist.shortVersionStringSuffix']
			}
			if (project.hasProperty('infoplist.shortVersionStringPrefix')) {
				project.infoplist.shortVersionStringPrefix = project['infoplist.shortVersionStringPrefix']
			}

			if (project.hasProperty('infoplist.iconPath')) {
				project.infoplist.iconPath = project['infoplist.iconPath']
			}

			if (project.hasProperty('xcodebuild.scheme')) {
				project.xcodebuild.scheme = project['xcodebuild.scheme']
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
			if (project.hasProperty('xcodebuild.bundleNameSuffix')) {
				project.xcodebuild.bundleNameSuffix = project['xcodebuild.bundleNameSuffix']
			}
			if (project.hasProperty('xcodebuild.arch')) {
				project.xcodebuild.arch = project['xcodebuild.arch']
			}
			if (project.hasProperty('xcodebuild.unitTestTarget')) {
				project.xcodebuild.unitTestTarget = project['xcodebuild.unitTestTarget']
			}



			if (project.hasProperty('hockeykit.displayName')) {
				project.hockeykit.displayName = project['hockeykit.displayName']
			}
			if (project.hasProperty('hockeykit.versionDirectoryName')) {
				project.hockeykit.versionDirectoryName = project['hockeykit.versionDirectoryName']
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

			if (project.hasProperty('provisioning.mobileprovisionUri')) {
				project.provisioning.mobileprovisionUri = project['provisioning.mobileprovisionUri']
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

			if (project.infoplist.bundleIdentifier != null || project.infoplist.bundleIdentifierSuffix || project.infoplist.versionSuffix != null) {
				xcodebuild.dependsOn(infoplistModify)
			}

			if (project.provisioning.mobileprovisionUri != null) {
				println "added cleanup for provisioning profile"
				codesign.doLast {
					println "run provisioning cleanup"
					provisioningCleanup.execute()
				}
			}

			if (project.keychain.certificateUri != null) {
				println "added cleanup for certificate"
				codesign.doLast {
					println "run certificate cleanup"
					keychainCleanup.execute()
				}
			}
		}
	}
}


