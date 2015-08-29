/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.openbakery.appledoc.AppledocCleanTask
import org.openbakery.appledoc.AppledocTask
import org.openbakery.appstore.AppstorePluginExtension
import org.openbakery.appstore.AppstoreValidateTask
import org.openbakery.appstore.AppstoreUploadTask

import org.openbakery.cocoapods.CocoapodsTask
import org.openbakery.configuration.XcodeConfigTask
import org.openbakery.coverage.CoverageCleanTask
import org.openbakery.coverage.CoveragePluginExtension
import org.openbakery.coverage.CoverageTask
import org.openbakery.crashlytics.CrashlyticsPluginExtension
import org.openbakery.crashlytics.CrashlyticsUploadTask
import org.openbakery.deploygate.DeployGateCleanTask
import org.openbakery.deploygate.DeployGatePluginExtension
import org.openbakery.deploygate.DeployGateUploadTask
import org.openbakery.hockeyapp.HockeyAppCleanTask
import org.openbakery.hockeyapp.HockeyAppPluginExtension
import org.openbakery.hockeyapp.HockeyAppUploadTask
import org.openbakery.hockeykit.HockeyKitArchiveTask
import org.openbakery.hockeykit.HockeyKitCleanTask
import org.openbakery.hockeykit.HockeyKitImageTask
import org.openbakery.hockeykit.HockeyKitManifestTask
import org.openbakery.hockeykit.HockeyKitPluginExtension
import org.openbakery.hockeykit.HockeyKitReleaseNotesTask
import org.openbakery.oclint.OCLintPluginExtension
import org.openbakery.oclint.OCLintTask
import org.openbakery.packaging.ReleaseNotesTask
import org.openbakery.signing.KeychainCleanupTask
import org.openbakery.signing.KeychainCreateTask
import org.openbakery.packaging.PackageTask
import org.openbakery.signing.KeychainRemoveFromSearchListTask
import org.openbakery.signing.ProvisioningCleanupTask
import org.openbakery.signing.ProvisioningInstallTask
import org.openbakery.simulators.SimulatorsCleanTask
import org.openbakery.simulators.SimulatorsCreateTask
import org.openbakery.simulators.SimulatorsListTask
import org.openbakery.simulators.SimulatorsStartTask
import org.openbakery.simulators.SimulatorsRunAppTask
import org.openbakery.simulators.SimulatorsInstallAppTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class XcodePlugin implements Plugin<Project> {

	private static Logger logger = LoggerFactory.getLogger(XcodePlugin.class)

	public static final String XCODE_GROUP_NAME = "Xcode"
	public static final String HOCKEYKIT_GROUP_NAME = "HockeyKit"
	public static final String HOCKEYAPP_GROUP_NAME = "HockeyApp"
	public static final String APPSTORE_GROUP_NAME = "AppStore"
	public static final String DEPLOYGATE_GROUP_NAME = "DeployGate"
	public static final String CRASHLYTICS_GROUP_NAME = "Crashlytics"
	public static final String APPLE_DOC_GROUP_NAME = "Appledoc"
	public static final String COVERAGE_GROUP_NAME = "Coverage"
	public static final String COCOAPODS_GROUP_NAME = "Cocoapods"
	public static final String SIMULATORS_GROUP_NAME = "Simulators"
	public static final String ANALYTICS_GROUP_NAME = "Analytics"


	public static final String XCODE_TEST_TASK_NAME = "xcodetest"
	public static final String ARCHIVE_TASK_NAME = "archive"
	public static final String SIMULATORS_LIST_TASK_NAME = "simulatorsList"
	public static final String SIMULATORS_CREATE_TASK_NAME = "simulatorsCreate"
	public static final String SIMULATORS_CLEAN_TASK_NAME = "simulatorsClean"
	public static final String SIMULATORS_START_TASK_NAME = "simulatorsStart"
	public static final String SIMULATORS_INSTALL_APP_TASK_NAME = "simulatorsInstallApp"
	public static final String SIMULATORS_RUN_APP_TASK_NAME = "simulatorsRunApp"
	public static final String XCODE_BUILD_TASK_NAME = "xcodebuild"
	public static final String XCODE_CLEAN_TASK_NAME = "xcodebuildClean"
	public static final String XCODE_CONFIG_TASK_NAME = "xcodebuildConfig"
	public static final String HOCKEYKIT_MANIFEST_TASK_NAME = "hockeykitManifest"
	public static final String HOCKEYKIT_ARCHIVE_TASK_NAME = "hockeykitArchive"
	public static final String HOCKEYKIT_NOTES_TASK_NAME = "hockeykitNotes"
	public static final String HOCKEYKIT_IMAGE_TASK_NAME = "hockeykitImage"
	public static final String HOCKEYKIT_CLEAN_TASK_NAME = "hockeykitClean"
	public static final String HOCKEYKIT_TASK_NAME = "hockeykit"
	public static final String KEYCHAIN_CREATE_TASK_NAME = "keychainCreate"
	public static final String KEYCHAIN_CLEAN_TASK_NAME = "keychainClean"
	public static final String KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME = "keychainRemove"
	public static final String INFOPLIST_MODIFY_TASK_NAME = 'infoplistModify'
	public static final String PROVISIONING_INSTALL_TASK_NAME = 'provisioningInstall'
	public static final String PROVISIONING_CLEAN_TASK_NAME = 'provisioningClean'
	public static final String PACKAGE_TASK_NAME = 'package'
	public static final String PACKAGE_RELEASE_NOTES_TASK_NAME = 'packageReleaseNotes'
	public static final String APPSTORE_UPLOAD_TASK_NAME = 'appstoreUpload'
	public static final String APPSTORE_VALIDATE_TASK_NAME = 'appstoreValidate'
	public static final String HOCKEYAPP_CLEAN_TASK_NAME = 'hockeyappClean'
	public static final String HOCKEYAPP_TASK_NAME = 'hockeyapp'
	public static final String DEPLOYGATE_TASK_NAME = 'deploygate'
	public static final String DEPLOYGATE_CLEAN_TASK_NAME = 'deploygateClean'
	public static final String CRASHLYTICS_TASK_NAME = 'crashlytics'
	public static final String COCOAPODS_TASK_NAME = 'cocoapods'
	public static final String OCLINT_TASK_NAME = 'oclint'
	public static final String OCLINT_REPORT_TASK_NAME = 'oclintReport'

	public static final String APPLEDOC_TASK_NAME = 'appledoc'
	public static final String APPLEDOC_CLEAN_TASK_NAME = 'appledocClean'

	public static final COVERAGE_TASK_NAME = 'coverage'
	public static final COVERAGE_CLEAN_TASK_NAME = 'coverageClean'
	public static final String SDK_MACOSX = "macosx"
	public static final String SDK_IPHONEOS = "iphoneos"
	public static final String SDK_IPHONESIMULATOR = "iphonesimulator"




	void apply(Project project) {
		project.getPlugins().apply(BasePlugin.class);

		System.setProperty("java.awt.headless", "true");

		configureExtensions(project)
		configureClean(project)
		configureBuild(project)
		configureTest(project)
		configureArchive(project)
		configureHockeyKit(project)
		configureKeychain(project)
		configureInfoPlist(project)
		configureProvisioning(project)
		configureAppstore(project)
		configureHockeyApp(project)
		configureDeployGate(project)
		configureCrashlytics(project)
		configurePackage(project)
		configureAppledoc(project)
		configureCoverage(project)
		configureCocoapods(project)
		configureOCLint(project)
		configureSimulatorTasks(project)
		configureProperties(project)
	}


	void configureProperties(Project project) {

		project.afterEvaluate {

			if (project.hasProperty('infoplist.bundleIdentifier')) {
				project.infoplist.bundleIdentifier = project['infoplist.bundleIdentifier']
			}
			if (project.hasProperty('infoplist.bundleIdentifierSuffix')) {
				project.infoplist.bundleIdentifierSuffix = project['infoplist.bundleIdentifierSuffix']
			}
			if (project.hasProperty('infoplist.bundleDisplayName')) {
				project.infoplist.bundleDisplayName = project['infoplist.bundleDisplayName']
			}
			if (project.hasProperty('infoplist.bundleDisplayNameSuffix')) {
				project.infoplist.bundleDisplayNameSuffix = project['infoplist.bundleDisplayNameSuffix']
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

			if (project.hasProperty('xcodebuild.signing.identity')) {
				project.xcodebuild.signing.identity = project['xcodebuild.signing.identity']
			}
			if (project.hasProperty('xcodebuild.signing.certificateURI')) {
				project.xcodebuild.signing.certificateURI = project['xcodebuild.signing.certificateURI']
			}
			if (project.hasProperty('xcodebuild.signing.certificatePassword')) {
				project.xcodebuild.signing.certificatePassword = project['xcodebuild.signing.certificatePassword']
			}
			if (project.hasProperty('xcodebuild.signing.mobileProvisionURI')) {
				project.xcodebuild.signing.mobileProvisionURI = project['xcodebuild.signing.mobileProvisionURI']
			}
			if (project.hasProperty('xcodebuild.signing.keychain')) {
				project.xcodebuild.signing.keychain = project['xcodebuild.signing.keychain']
			}
			if (project.hasProperty('xcodebuild.signing.keychainPassword')) {
				project.xcodebuild.signing.keychainPassword = project['signing.keychainPassword']
			}
			if (project.hasProperty('xcodebuild.signing.timeout')) {
				project.xcodebuild.signing.timeout = project['signing.timeout']
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
			if (project.hasProperty('xcodebuild.environment')) {
				project.xcodebuild.environment = project['xcodebuild.environment']
			}
			if (project.hasProperty('xcodebuild.version')) {
				project.xcodebuild.version = project['xcodebuild.version']
			}
			if (project.hasProperty('xcodebuild.ipaFileName')) {
				project.xcodebuild.ipaFileName = project['xcodebuild.ipaFileName']
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
			if (project.hasProperty('hockeykit.notes')) {
				project.hockeykit.notes = project['hockeykit.notes']
			}


			if (project.hasProperty('hockeyapp.apiToken')) {
				project.hockeyapp.apiToken = project['hockeyapp.apiToken']
			}
			if (project.hasProperty('hockeyapp.appID')) {
				project.hockeyapp.appID = project['hockeyapp.appID']
			}
			if (project.hasProperty('hockeyapp.notes')) {
				project.hockeyapp.notes = project['hockeyapp.notes']
			}
			if (project.hasProperty('hockeyapp.status')) {
				project.hockeyapp.status = project['hockeyapp.status']
			}
			if (project.hasProperty('hockeyapp.notify')) {
				project.hockeyapp.notify = project['hockeyapp.notify']
			}
			if (project.hasProperty('hockeyapp.notesType')) {
				project.hockeyapp.notesType = project['hockeyapp.notesType']
			}
			if (project.hasProperty('hockeyapp.teams')) {
				project.hockeyapp.teams = project['hockeyapp.teams']
			}
			if (project.hasProperty('hockeyapp.tags')) {
				project.hockeyapp.tags = project['hockeyapp.tags']
			}
			if (project.hasProperty('hockeyapp.releaseType')) {
				project.hockeyapp.releaseType = project['hockeyapp.releaseType']
			}
			if (project.hasProperty('hockeyapp.privatePage')) {
				project.hockeyapp.privatePage = project['hockeyapp.privatePage']
			}
			if (project.hasProperty('hockeyapp.commitSha')) {
				project.hockeyapp.commitSha = project['hockeyapp.commitSha']
			}
			if (project.hasProperty('hockeyapp.buildServerUrl')) {
				project.hockeyapp.buildServerUrl = project['hockeyapp.buildServerUrl']
			}
			if (project.hasProperty('hockeyapp.repositoryUrl')) {
				project.hockeyapp.repositoryUrl = project['hockeyapp.repositoryUrl']
			}

			if (project.hasProperty('deploygate.outputDirectory')) {
				project.deploygate.outputDirectory = project['deploygate.outputDirectory']
			}

			if (project.hasProperty('deploygate.apiToken')) {
				project.deploygate.apiToken = project['deploygate.apiToken']
			}
			if (project.hasProperty('deploygate.userName')) {
				project.deploygate.userName = project['deploygate.userName']
			}
			if (project.hasProperty('deploygate.message')) {
				project.deploygate.message = project['deploygate.message']
			}

			if (project.hasProperty('crashlytics.submitCommand')) {
				project.crashlytics.submitCommand = project['crashlytics.submitCommand']
			}
			if (project.hasProperty('crashlytics.apiKey')) {
				project.crashlytics.apiKey = project['crashlytics.apiKey']
			}
			if (project.hasProperty('crashlytics.buildSecret')) {
				project.crashlytics.buildSecret = project['crashlytics.buildSecret']
			}
			if (project.hasProperty('crashlytics.emails')) {
				project.crashlytics.emails = project['crashlytics.emails']
			}
			if (project.hasProperty('crashlytics.groupAliases')) {
				project.crashlytics.groupAliases = project['crashlytics.groupAliases']
			}
			if (project.hasProperty('crashlytics.notesPath')) {
				project.crashlytics.notesPath = project['crashlytics.notesPath']
			}
			if (project.hasProperty('crashlytics.notifications')) {
				project.crashlytics.notifications = project['crashlytics.notifications']
			}

			if (project.hasProperty('coverage.outputFormat')) {
				project.coverage.outputFormat = project['coverage.outputFormat']
			}
			if (project.hasProperty('coverage.exclude')) {
				project.coverage.exclude = project['coverage.exclude']
			}

			if (project.hasProperty('appstore.username')) {
				project.appstore.username = project['appstore.username']
			}
			if (project.hasProperty('appstore.password')) {
				project.appstore.password = project['appstore.password']
			}


			Task testTask = (Test) project.getTasks().findByPath(JavaPlugin.TEST_TASK_NAME)
			if (testTask == null) {
				testTask = project.getTasks().create(JavaPlugin.TEST_TASK_NAME)
			}
			testTask.dependsOn(XCODE_TEST_TASK_NAME)

		}

	}

	def void configureExtensions(Project project) {
		project.extensions.create("xcodebuild", XcodeBuildPluginExtension, project)
		project.extensions.create("infoplist", InfoPlistExtension)
		project.extensions.create("hockeykit", HockeyKitPluginExtension, project)
		project.extensions.create("appstore", AppstorePluginExtension, project)
		project.extensions.create("hockeyapp", HockeyAppPluginExtension, project)
		project.extensions.create("deploygate", DeployGatePluginExtension, project)
		project.extensions.create("crashlytics", CrashlyticsPluginExtension, project)
		project.extensions.create("coverage", CoveragePluginExtension, project)
		project.extensions.create("oclint", OCLintPluginExtension, project)
	}

	private void configureBuild(Project project) {
		XcodeBuildTask xcodebuildTask = project.getTasks().create(XCODE_BUILD_TASK_NAME, XcodeBuildTask.class);
		xcodebuildTask.setGroup(XCODE_GROUP_NAME);

		XcodeConfigTask configTask = project.getTasks().create(XCODE_CONFIG_TASK_NAME, XcodeConfigTask.class);
		configTask.setGroup(XCODE_GROUP_NAME);

		project.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(xcodebuildTask);
	}


	private void configureClean(Project project) {
		XcodeBuildCleanTask xcodeBuildCleanTask = project.getTasks().create(XCODE_CLEAN_TASK_NAME, XcodeBuildCleanTask.class);
		xcodeBuildCleanTask.setGroup(XCODE_GROUP_NAME);

		project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME).dependsOn(xcodeBuildCleanTask);
	}

	private void configureArchive(Project project) {
		XcodeBuildArchiveTask xcodeBuildArchiveTask = project.getTasks().create(ARCHIVE_TASK_NAME, XcodeBuildArchiveTask.class);
		xcodeBuildArchiveTask.setGroup(XCODE_GROUP_NAME);

		//xcodeBuildArchiveTask.dependsOn(project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME));
	}

	private void configureSimulatorTasks(Project project) {
		project.task(SIMULATORS_LIST_TASK_NAME, type: SimulatorsListTask, group: SIMULATORS_LIST_TASK_NAME)
		project.task(SIMULATORS_CREATE_TASK_NAME, type: SimulatorsCreateTask, group: SIMULATORS_LIST_TASK_NAME)
		project.task(SIMULATORS_CLEAN_TASK_NAME, type: SimulatorsCleanTask, group: SIMULATORS_LIST_TASK_NAME)
		project.task(SIMULATORS_START_TASK_NAME, type: SimulatorsStartTask, group: SIMULATORS_LIST_TASK_NAME)
		project.task(SIMULATORS_RUN_APP_TASK_NAME, type: SimulatorsRunAppTask, group: SIMULATORS_LIST_TASK_NAME)
		project.task(SIMULATORS_INSTALL_APP_TASK_NAME, type: SimulatorsInstallAppTask, group: SIMULATORS_LIST_TASK_NAME)
	}

	private void configureHockeyKit(Project project) {
		project.task(HOCKEYKIT_MANIFEST_TASK_NAME, type: HockeyKitManifestTask, group: HOCKEYKIT_GROUP_NAME)
		HockeyKitArchiveTask hockeyKitArchiveTask = project.task(HOCKEYKIT_ARCHIVE_TASK_NAME, type: HockeyKitArchiveTask, group: HOCKEYKIT_GROUP_NAME)
		project.task(HOCKEYKIT_NOTES_TASK_NAME, type: HockeyKitReleaseNotesTask, group: HOCKEYKIT_GROUP_NAME)
		project.task(HOCKEYKIT_IMAGE_TASK_NAME, type: HockeyKitImageTask, group: HOCKEYKIT_GROUP_NAME)
		project.task(HOCKEYKIT_CLEAN_TASK_NAME, type: HockeyKitCleanTask, group: HOCKEYKIT_GROUP_NAME)

		DefaultTask hockeykitTask = project.task(HOCKEYKIT_TASK_NAME, type: DefaultTask, description: "Creates a build that can be deployed on a hockeykit Server", group: HOCKEYKIT_GROUP_NAME);
		hockeykitTask.dependsOn(HOCKEYKIT_ARCHIVE_TASK_NAME, HOCKEYKIT_MANIFEST_TASK_NAME, HOCKEYKIT_IMAGE_TASK_NAME, HOCKEYKIT_NOTES_TASK_NAME)
	}

	private void configureKeychain(Project project) {
		project.task(KEYCHAIN_CREATE_TASK_NAME, type: KeychainCreateTask, group: XCODE_GROUP_NAME)
		project.task(KEYCHAIN_CLEAN_TASK_NAME, type: KeychainCleanupTask, group: XCODE_GROUP_NAME)
		project.task(KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME, type: KeychainRemoveFromSearchListTask, group: XCODE_GROUP_NAME)
	}

	private void configureTest(Project project) {
		project.task(XCODE_TEST_TASK_NAME, type: XcodeTestTask, group: XCODE_GROUP_NAME)

	}

	private configureInfoPlist(Project project) {
		project.task(INFOPLIST_MODIFY_TASK_NAME, type: InfoPlistModifyTask, group: XCODE_GROUP_NAME)
	}

	private configureProvisioning(Project project) {
		project.task(PROVISIONING_INSTALL_TASK_NAME, type: ProvisioningInstallTask, group: XCODE_GROUP_NAME)
		project.task(PROVISIONING_CLEAN_TASK_NAME, type: ProvisioningCleanupTask, group: XCODE_GROUP_NAME)
	}

	private configurePackage(Project project) {
		PackageTask packageTask = project.task(PACKAGE_TASK_NAME, type: PackageTask, group: XCODE_GROUP_NAME)


		project.task(PACKAGE_RELEASE_NOTES_TASK_NAME, type: ReleaseNotesTask, group: XCODE_GROUP_NAME)

		//ProvisioningCleanupTask provisioningCleanup = project.getTasks().getByName(PROVISIONING_CLEAN_TASK_NAME)

		//KeychainCleanupTask keychainCleanupTask = project.getTasks().getByName(KEYCHAIN_CLEAN_TASK_NAME)

/*  // disabled clean because of #115
		packageTask.doLast {
			provisioningCleanup.clean()
			keychainCleanupTask.clean()
		}
*/
		XcodeBuildTask xcodeBuildTask = project.getTasks().getByName(XCODE_BUILD_TASK_NAME)
		packageTask.shouldRunAfter(xcodeBuildTask)
	}

	private configureAppstore(Project project) {
		project.task(APPSTORE_UPLOAD_TASK_NAME, type: AppstoreUploadTask, group: APPSTORE_GROUP_NAME)
		project.task(APPSTORE_VALIDATE_TASK_NAME, type: AppstoreValidateTask, group: APPSTORE_GROUP_NAME)
	}


	private void configureHockeyApp(Project project) {
		project.task(HOCKEYAPP_CLEAN_TASK_NAME, type: HockeyAppCleanTask, group: HOCKEYAPP_GROUP_NAME)
		project.task(HOCKEYAPP_TASK_NAME, type: HockeyAppUploadTask, group: HOCKEYAPP_GROUP_NAME)
	}

	private void configureAppledoc(Project project) {
		project.task(APPLEDOC_TASK_NAME, type: AppledocTask, group: APPLE_DOC_GROUP_NAME)
		project.task(APPLEDOC_CLEAN_TASK_NAME, type: AppledocCleanTask, group: APPLE_DOC_GROUP_NAME)

	}

	private void configureCoverage(Project project) {
		project.task(COVERAGE_TASK_NAME, type: CoverageTask, group: COVERAGE_GROUP_NAME)
		project.task(COVERAGE_CLEAN_TASK_NAME, type: CoverageCleanTask, group: COVERAGE_GROUP_NAME)

	}


	private void configureDeployGate(Project project) {
		project.task(DEPLOYGATE_CLEAN_TASK_NAME, type: DeployGateCleanTask, group: DEPLOYGATE_GROUP_NAME)
		project.task(DEPLOYGATE_TASK_NAME, type: DeployGateUploadTask, group: DEPLOYGATE_GROUP_NAME)
	}

	private void configureCrashlytics(Project project) {
		project.task(CRASHLYTICS_TASK_NAME, type: CrashlyticsUploadTask, group: CRASHLYTICS_GROUP_NAME)
	}

	private void configureCocoapods(Project project) {

		//project.task(COCOAPODS_CLEAN_TASK_NAME, type: CocoapodsCleanTask, group: COCOAPODS_GROUP_NAME)
		CocoapodsTask task = project.task(COCOAPODS_TASK_NAME, type: CocoapodsTask, group: COCOAPODS_GROUP_NAME)
		if (task.hasPodfile()) {
			addDependencyToBuild(project, task);
		}
	}

	private void configureOCLint(Project project) {
		OCLintTask reportTask = project.task(OCLINT_REPORT_TASK_NAME, type: OCLintTask, group: ANALYTICS_GROUP_NAME)

		Task ocLintTask = project.getTasks().create(OCLINT_TASK_NAME);
		ocLintTask.group = ANALYTICS_GROUP_NAME
		ocLintTask.description = "Runs: " +  BasePlugin.CLEAN_TASK_NAME + " " + XCODE_BUILD_TASK_NAME + " " + OCLINT_REPORT_TASK_NAME
		ocLintTask.dependsOn(project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME))

		XcodeBuildTask xcodeBuildTask = project.getTasks().getByName(XcodePlugin.XCODE_BUILD_TASK_NAME)
		reportTask.mustRunAfter(xcodeBuildTask)

		ocLintTask.dependsOn(xcodeBuildTask)
		ocLintTask.dependsOn(reportTask)

	}


	private void addDependencyToBuild(Project project, Task task) {
		XcodeBuildTask buildTask = project.getTasks().getByName(XCODE_BUILD_TASK_NAME)
		buildTask.dependsOn(task)

		XcodeTestTask testTask = project.getTasks().getByName(XCODE_TEST_TASK_NAME)
		testTask.dependsOn(task)
	}
}


