package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class XcodePlugin implements Plugin<Project> {

	def static final String XCODE_GROUP_NAME = "Xcode"
    def static final String HOCKEYKIT_GROUP_NAME = "HockeyKit"
    def static final String HOCKEYAPP_GROUP_NAME = "HockeyApp"
    def static final String TESTFLIGHT_GROUP_NAME = "TestFlight"

	private Project project

	void apply(Project project) {
		this.project = project
		System.setProperty("java.awt.headless", "true"); // TODO: what is that for?

		defineExtensions()
		defineTasks()

		// TODO s.th. like defineTaskDependencies
		Task xcodebuild = project.tasks.'xcodebuild'
		Task infoplistModify = project.tasks.'infoplist-modify'
		Task archive = project.tasks."archive"
		Task hockeyKitManifest = project.tasks.'hockeykit-manifest'
		Task hockeyKitArchiveTask = project.tasks.'hockeykit-archive'
		Task hockeyKitImageTask = project.tasks.'hockeykit-image'

		Task hockey = project.tasks.'hockeykit'
		hockey.dependsOn(hockeyKitArchiveTask, hockeyKitManifest, hockeyKitImageTask)


		hockeyKitArchiveTask.dependsOn(archive)
		archive.dependsOn("clean")

		Task keychainCleanup = project.tasks.'keychain-clean'
		Task xcodebuildCleanup = project.tasks.'clean'
		Task provisioningCleanup = project.tasks.'provisioning-clean'
		Task hockeyKitCleanTask = project.tasks.'hockeykit-clean'
		Task testFlightClean = project.tasks.'testflight-clean'
        Task hockeyAppClean = project.tasks.'hockeyapp-clean'

		xcodebuildCleanup.dependsOn(keychainCleanup)
		xcodebuildCleanup.dependsOn(provisioningCleanup)
		xcodebuildCleanup.dependsOn(hockeyKitCleanTask)
		xcodebuildCleanup.dependsOn(testFlightClean);
        xcodebuildCleanup.dependsOn(hockeyAppClean);



		project.afterEvaluate {

			if (project.hasProperty('infoplist.bundleIdentifier')) {
				project.infoplist.bundleIdentifier = project['infoplist.bundleIdentifier']
			}
			if (project.hasProperty('infoplist.bundleIdentifierSuffix')) {
				project.infoplist.bundleIdentifierSuffix = project['infoplist.bundleIdentifierSuffix']
			}
            if (project.hasProperty('infoplist.bundleDisplayName')) {
                project.infoplist.bundleIdentifier = project['infoplist.bundleDisplayName']
            }
            if (project.hasProperty('infoplist.bundleDisplayNameSuffix')) {
                project.infoplist.bundleIdentifier = project['infoplist.bundleDisplayNameSuffix']
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

			Task codesign = project.tasks.'codesign'
			if (project.xcodebuild.sdk.startsWith("iphoneos") &&
							project.xcodebuild.signIdentity != null) {
				archive.dependsOn(codesign)
			} else {
				archive.dependsOn(xcodebuild)
			}

			if (project.infoplist.bundleIdentifier != null || project.infoplist.bundleIdentifierSuffix != null
                    || project.infoplist.bundleDisplayName != null || project.infoplist.bundleDisplayNameSuffix != null
                    || project.infoplist.versionSuffix != null) {
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

	def void defineExtensions() {
		project.extensions.create("xcodebuild", XcodeBuildPluginExtension)
		project.extensions.create("keychain", KeychainPluginExtension)
		project.extensions.create("provisioning", ProvisioningPluginExtension)
		project.extensions.create("infoplist", InfoPlistExtension)
		project.extensions.create("hockeykit", HockeyKitPluginExtension)
		project.extensions.create("testflight", TestFlightPluginExtension)
        project.extensions.create("hockeyapp", HockeyAppPluginExtension)
	}

	def void defineTasks() {
		project.task('keychain-create', type: KeychainCreateTask, group: XCODE_GROUP_NAME)
		project.task('xcodebuild', type: XcodeBuildTask, group: XCODE_GROUP_NAME)
		project.task('infoplist-modify', type: InfoPlistModifyTask, group: XCODE_GROUP_NAME)
		project.task('provisioning-install', type: ProvisioningInstallTask, group: XCODE_GROUP_NAME)
		project.task('archive', type: XcodeBuildArchiveTask, group: XCODE_GROUP_NAME)
        project.task('keychain-clean', type: KeychainCleanupTask, group: XCODE_GROUP_NAME)
        project.task('clean', type: XcodeBuildCleanTask, group: XCODE_GROUP_NAME)
        project.task('provisioning-clean', type: ProvisioningCleanupTask, group: XCODE_GROUP_NAME)
        project.task('codesign', type: CodesignTask, group: XCODE_GROUP_NAME)

        //
		project.task('hockeykit', type: DefaultTask, description: "Creates a build that can be deployed on a hockeykit Server", group: HOCKEYKIT_GROUP_NAME);
		project.task('hockeykit-manifest', type: HockeyKitManifestTask, group: HOCKEYKIT_GROUP_NAME)
		project.task('hockeykit-archive', type: HockeyKitArchiveTask, group: HOCKEYKIT_GROUP_NAME)
		project.task('hockeykit-image', type: HockeyKitImageTask, group: HOCKEYKIT_GROUP_NAME)
        project.task('hockeykit-clean', type: HockeyKitCleanTask, group: HOCKEYKIT_GROUP_NAME)

        //
		project.task('testflight-prepare', type: TestFlightPrepareTask, group: TESTFLIGHT_GROUP_NAME)
		project.task('testflight', type: TestFlightUploadTask, group: TESTFLIGHT_GROUP_NAME)
		project.task('testflight-clean', type: TestFlightCleanTask, group: TESTFLIGHT_GROUP_NAME)

        //
        project.task('hockeyapp-clean', type: HockeyAppCleanTask, group: HOCKEYAPP_GROUP_NAME)
        project.task('hockeyapp-prepare', type: HockeyAppPrepareTask, group: HOCKEYAPP_GROUP_NAME)
        project.task('hockeyapp', type: HockeyAppUploadTask, group: HOCKEYAPP_GROUP_NAME)

    }
}


