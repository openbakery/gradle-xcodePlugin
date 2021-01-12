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
import org.openbakery.appcenter.AppCenterCleanTask
import org.openbakery.appcenter.AppCenterDsymUploadTask
import org.openbakery.appcenter.AppCenterPluginExtension
import org.openbakery.appcenter.AppCenterUploadTask
import org.openbakery.appledoc.AppledocCleanTask
import org.openbakery.appledoc.AppledocTask
import org.openbakery.appstore.AppstorePluginExtension
import org.openbakery.appstore.AppstoreValidateTask
import org.openbakery.appstore.AppstoreUploadTask
import org.openbakery.appstore.NotarizeTask
import org.openbakery.carthage.CarthageArchiveTask
import org.openbakery.carthage.CarthageBootstrapTask
import org.openbakery.carthage.CarthageCleanTask
import org.openbakery.carthage.CarthageUpdateTask
import org.openbakery.carthage.CarthagePluginExtension
import org.openbakery.cocoapods.CocoapodsBootstrapTask
import org.openbakery.cocoapods.CocoapodsInstallTask
import org.openbakery.cocoapods.CocoapodsUpdateTask
import org.openbakery.configuration.XcodeConfigTask
import org.openbakery.coverage.CoverageCleanTask
import org.openbakery.coverage.CoveragePluginExtension
import org.openbakery.coverage.CoverageTask
import org.openbakery.cpd.CpdTask
import org.openbakery.crashlytics.CrashlyticsPluginExtension
import org.openbakery.crashlytics.CrashlyticsUploadTask
import org.openbakery.deploygate.DeployGateCleanTask
import org.openbakery.deploygate.DeployGatePluginExtension
import org.openbakery.deploygate.DeployGateUploadTask
import org.openbakery.oclint.OCLintPluginExtension
import org.openbakery.oclint.OCLintTask
import org.openbakery.signing.KeychainCleanupTask
import org.openbakery.signing.KeychainCreateTask
import org.openbakery.packaging.PackageTask
import org.openbakery.signing.KeychainRemoveFromSearchListTask
import org.openbakery.signing.ProvisioningCleanupTask
import org.openbakery.signing.ProvisioningInstallTask
import org.openbakery.simulators.SimulatorKillTask
import org.openbakery.simulators.SimulatorsCleanTask
import org.openbakery.simulators.SimulatorsCreateTask
import org.openbakery.simulators.SimulatorsListTask
import org.openbakery.simulators.SimulatorStartTask
import org.openbakery.simulators.SimulatorRunAppTask
import org.openbakery.simulators.SimulatorInstallAppTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class XcodePlugin implements Plugin<Project> {

	private static Logger logger = LoggerFactory.getLogger(XcodePlugin.class)

	public static final String XCODE_GROUP_NAME = "Xcode"
	public static final String APPSTORE_GROUP_NAME = "AppStore"
	public static final String DEPLOYGATE_GROUP_NAME = "DeployGate"
	public static final String CRASHLYTICS_GROUP_NAME = "Crashlytics"
	public static final String APPLE_DOC_GROUP_NAME = "Appledoc"
	public static final String COVERAGE_GROUP_NAME = "Coverage"
	public static final String COCOAPODS_GROUP_NAME = "Cocoapods"
	public static final String CARTHAGE_GROUP_NAME = "Carthage"
	public static final String SIMULATORS_GROUP_NAME = "Simulators"
	public static final String ANALYTICS_GROUP_NAME = "Analytics"
	public static final String APPCENTER_GROUP_NAME = "AppCenter"


	public static final String XCODE_TEST_TASK_NAME = "xcodetest"
	public static final String XCODE_BUILD_FOR_TEST_TASK_NAME = "xcodebuildForTest"
	public static final String XCODE_TEST_RUN_TASK_NAME = "xcodetestrun"
	public static final String ARCHIVE_TASK_NAME = "archive"
	public static final String SIMULATORS_LIST_TASK_NAME = "simulatorsList"
	public static final String SIMULATORS_CREATE_TASK_NAME = "simulatorsCreate"
	public static final String SIMULATORS_CLEAN_TASK_NAME = "simulatorsClean"
	public static final String SIMULATORS_START_TASK_NAME = "simulatorStart"
	public static final String SIMULATORS_INSTALL_APP_TASK_NAME = "simulatorInstallApp"
	public static final String SIMULATORS_RUN_APP_TASK_NAME = "simulatorRunApp"
	public static final String SIMULATORS_KILL_TASK_NAME = "simulatorKill"
	public static final String XCODE_BUILD_TASK_NAME = "xcodebuild"
	public static final String XCODE_CLEAN_TASK_NAME = "xcodebuildClean"
	public static final String XCODE_CONFIG_TASK_NAME = "xcodebuildConfig"
	public static final String KEYCHAIN_CREATE_TASK_NAME = "keychainCreate"
	public static final String KEYCHAIN_CLEAN_TASK_NAME = "keychainClean"
	public static final String KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME = "keychainRemove"
	public static final String INFOPLIST_MODIFY_TASK_NAME = 'infoplistModify'
	public static final String PROVISIONING_INSTALL_TASK_NAME = 'provisioningInstall'
	public static final String PROVISIONING_CLEAN_TASK_NAME = 'provisioningClean'
	public static final String PACKAGE_TASK_NAME = 'package'
	public static final String APPSTORE_UPLOAD_TASK_NAME = 'appstoreUpload'
	public static final String APPSTORE_VALIDATE_TASK_NAME = 'appstoreValidate'
	public static final String NOTARIZE_TASK_NAME = 'notarize'
	public static final String DEPLOYGATE_TASK_NAME = 'deploygate'
	public static final String DEPLOYGATE_CLEAN_TASK_NAME = 'deploygateClean'
	public static final String CRASHLYTICS_TASK_NAME = 'crashlytics'
	public static final String COCOAPODS_INSTALL_TASK_NAME = 'cocoapodsInstall'
	public static final String COCOAPODS_UPDATE_TASK_NAME = 'cocoapodsUpdate'
	public static final String COCOAPODS_BOOTSTRAP_TASK_NAME = 'cocoapodsBootstrap'
	public static final String OCLINT_TASK_NAME = 'oclint'
	public static final String OCLINT_REPORT_TASK_NAME = 'oclintReport'
	public static final String CPD_TASK_NAME = 'cpd'
	public static final String CARTHAGE_BOOTSTRAP_TASK_NAME = 'carthageBootstrap'
	public static final String CARTHAGE_UPDATE_TASK_NAME = 'carthageUpdate'
	public static final String CARTHAGE_CLEAN_TASK_NAME = 'carthageClean'
	public static final String CARTHAGE_ARCHIVE_TASK_NAME = 'carthageArchive'
	public static final String APPCENTER_CLEAN_TASK_NAME = 'appCenterClean'
	public static final String APPCENTER_TASK_NAME = 'appcenter'
	public static final String APPCENTER_IPA_UPLOAD_TASK_NAME = 'appcenterIpaUpload'
	public static final String APPCENTER_DSYM_UPLOAD_TASK_NAME = 'appcenterDsymUpload'

	public static final String APPLEDOC_TASK_NAME = 'appledoc'
	public static final String APPLEDOC_CLEAN_TASK_NAME = 'appledocClean'

	public static final String COVERAGE_TASK_NAME = 'coverage'
	public static final String COVERAGE_CLEAN_TASK_NAME = 'coverageClean'


	public static final String SDK_IPHONESIMULATOR = "iphonesimulator"


	void apply(Project project) {
		project.getPlugins().apply(BasePlugin.class);

		System.setProperty("java.awt.headless", "true");

		configureExtensions(project)
		configureClean(project)
		configureBuild(project)
		configureTest(project)
		configureArchive(project)
		configureKeychain(project)
		configureInfoPlist(project)
		configureProvisioning(project)
		configureAppstore(project)
		configureDeployGate(project)
		configureCrashlytics(project)
		configurePackage(project)
		configureAppledoc(project)
		configureCoverage(project)
		configureCpd(project)
		configureCocoapods(project)
		configureCarthage(project)
		configureOCLint(project)
		configureSimulatorTasks(project)
		configureProperties(project)
		configureAppCenter(project)
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
			if (project.hasProperty('xcodebuild.destination')) {
				project.xcodebuild.destination = project['xcodebuild.destination']
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


			if (project.hasProperty('oclint.reportType')) {
				project.oclint.reportType = project['oclint.reportType'];
			}
			if (project.hasProperty('oclint.rules')) {
				project.oclint.rules = project['oclint.rules'];
			}
			if (project.hasProperty('oclint.disableRules')) {
				project.oclint.disableRules = project['oclint.disableRules'];
			}
			if (project.hasProperty('oclint.excludes')) {
				project.oclint.excludes = project['oclint.excludes'];
			}
			if (project.hasProperty('oclint.maxPriority1')) {
				project.oclint.maxPriority1 = project['oclint.maxPriority1'];
			}
			if (project.hasProperty('oclint.maxPriority2')) {
				project.oclint.maxPriority2 = project['oclint.maxPriority2'];
			}
			if (project.hasProperty('oclint.maxPriority3')) {
				project.oclint.maxPriority3 = project['oclint.maxPriority3'];
			}

			if (project.hasProperty('appcenter.appOwner')) {
				project.appcenter.appOwner = project['appcenter.appOwner']
			}

			if (project.hasProperty('appcenter.appName')) {
				project.appcenter.appName = project['appcenter.appName']
			}

			if (project.hasProperty('appcenter.apiToken')) {
				project.appcenter.apiToken = project['appcenter.apiToken']
			}

			if (project.hasProperty("appcenter.readTimeout")) {
				project.appcenter.readTimeout = project['appcenter.readTimeout']
			}

			if (project.hasProperty('appcenter.destination')) {
				project.appcenter.destination = project['appcenter.destination']
			}

			if (project.hasProperty('appcenter.notifyTesters')) {
				project.appcenter.notifyTesters = project['appcenter.notifyTesters']
			}

			if (project.hasProperty('appcenter.mandatoryUpdate')) {
				project.appcenter.mandatoryUpdate = project['appcenter.mandatoryUpdate']
			}

			Task testTask = (Test) project.getTasks().findByPath(JavaPlugin.TEST_TASK_NAME)
			if (testTask == null) {
				testTask = project.getTasks().create(JavaPlugin.TEST_TASK_NAME)
			}
			testTask.dependsOn(XCODE_TEST_TASK_NAME)


			configureCarthageDependencies(project)
			configureTestRunDependencies(project)
		}

	}


	void configureExtensions(Project project) {
		project.extensions.create("xcodebuild", XcodeBuildPluginExtension, project)
		project.extensions.create("infoplist", InfoPlistExtension)
		project.extensions.create("appstore", AppstorePluginExtension, project)
		project.extensions.create("deploygate", DeployGatePluginExtension, project)
		project.extensions.create("crashlytics", CrashlyticsPluginExtension, project)
		project.extensions.create("coverage", CoveragePluginExtension, project)
		project.extensions.create("oclint", OCLintPluginExtension, project)
		project.extensions.create("carthage", CarthagePluginExtension, project)
		project.extensions.create("appcenter", AppCenterPluginExtension, project)
	}


	private void configureTestRunDependencies(Project project) {
		for (XcodeTestRunTask xcodeTestRunTask : project.getTasks().withType(XcodeTestRunTask.class)) {
			if (xcodeTestRunTask.runOnDevice()) {
				xcodeTestRunTask.dependsOn(XcodePlugin.KEYCHAIN_CREATE_TASK_NAME, XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)
				xcodeTestRunTask.finalizedBy(XcodePlugin.KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME)
			}
		}
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
		project.task(SIMULATORS_START_TASK_NAME, type: SimulatorStartTask, group: SIMULATORS_LIST_TASK_NAME)
		project.task(SIMULATORS_RUN_APP_TASK_NAME, type: SimulatorRunAppTask, group: SIMULATORS_LIST_TASK_NAME)
		project.task(SIMULATORS_INSTALL_APP_TASK_NAME, type: SimulatorInstallAppTask, group: SIMULATORS_LIST_TASK_NAME)
		project.task(SIMULATORS_KILL_TASK_NAME, type: SimulatorKillTask, group: SIMULATORS_LIST_TASK_NAME)
	}


	private void configureKeychain(Project project) {
		project.task(KEYCHAIN_CREATE_TASK_NAME, type: KeychainCreateTask, group: XCODE_GROUP_NAME)
		project.task(KEYCHAIN_CLEAN_TASK_NAME, type: KeychainCleanupTask, group: XCODE_GROUP_NAME)
		project.task(KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME, type: KeychainRemoveFromSearchListTask, group: XCODE_GROUP_NAME)
	}

	private void configureTest(Project project) {
		project.task(XCODE_TEST_TASK_NAME, type: XcodeTestTask, group: XCODE_GROUP_NAME)
		project.task(XCODE_BUILD_FOR_TEST_TASK_NAME, type: XcodeBuildForTestTask, group: XCODE_GROUP_NAME)
		project.task(XCODE_TEST_RUN_TASK_NAME, type: XcodeTestRunTask, group: XCODE_GROUP_NAME)

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

		XcodeBuildTask xcodeBuildTask = project.getTasks().getByName(XCODE_BUILD_TASK_NAME)
		packageTask.shouldRunAfter(xcodeBuildTask)
	}

	private configureAppstore(Project project) {
		project.task(APPSTORE_UPLOAD_TASK_NAME, type: AppstoreUploadTask, group: APPSTORE_GROUP_NAME)
		project.task(APPSTORE_VALIDATE_TASK_NAME, type: AppstoreValidateTask, group: APPSTORE_GROUP_NAME)
		project.task(NOTARIZE_TASK_NAME, type: NotarizeTask, group: APPSTORE_GROUP_NAME)
	}

	private void configureAppledoc(Project project) {
		project.task(APPLEDOC_TASK_NAME, type: AppledocTask, group: APPLE_DOC_GROUP_NAME)
		project.task(APPLEDOC_CLEAN_TASK_NAME, type: AppledocCleanTask, group: APPLE_DOC_GROUP_NAME)

	}

	private void configureCoverage(Project project) {
		project.task(COVERAGE_TASK_NAME, type: CoverageTask, group: COVERAGE_GROUP_NAME)
		project.task(COVERAGE_CLEAN_TASK_NAME, type: CoverageCleanTask, group: COVERAGE_GROUP_NAME)

	}

	private void configureCpd(Project project) {
		project.task(CPD_TASK_NAME, type: CpdTask, group: ANALYTICS_GROUP_NAME)
	}

	private void configureDeployGate(Project project) {
		project.task(DEPLOYGATE_CLEAN_TASK_NAME, type: DeployGateCleanTask, group: DEPLOYGATE_GROUP_NAME)
		project.task(DEPLOYGATE_TASK_NAME, type: DeployGateUploadTask, group: DEPLOYGATE_GROUP_NAME)
	}

	private void configureCrashlytics(Project project) {
		project.task(CRASHLYTICS_TASK_NAME, type: CrashlyticsUploadTask, group: CRASHLYTICS_GROUP_NAME)
	}

	private void configureCocoapods(Project project) {
		project.task(COCOAPODS_INSTALL_TASK_NAME, type: CocoapodsInstallTask, group: COCOAPODS_GROUP_NAME)
		project.task(COCOAPODS_BOOTSTRAP_TASK_NAME, type: CocoapodsBootstrapTask, group: COCOAPODS_GROUP_NAME)
		project.task(COCOAPODS_UPDATE_TASK_NAME, type: CocoapodsUpdateTask, group: COCOAPODS_GROUP_NAME)
	}

	private void configureCarthage(Project project) {
		project.task(CARTHAGE_CLEAN_TASK_NAME, type: CarthageCleanTask, group: CARTHAGE_GROUP_NAME)
		project.task(CARTHAGE_UPDATE_TASK_NAME, type: CarthageUpdateTask, group: CARTHAGE_GROUP_NAME)
		project.task(CARTHAGE_BOOTSTRAP_TASK_NAME, type: CarthageBootstrapTask, group: CARTHAGE_GROUP_NAME)
		project.task(CARTHAGE_ARCHIVE_TASK_NAME, type: CarthageArchiveTask, group: CARTHAGE_GROUP_NAME)
	}

	private configureCarthageDependencies(Project project) {
		project.getTasks()
				.getByName(BasePlugin.CLEAN_TASK_NAME)
				.dependsOn(project.getTasks().getByName(CARTHAGE_CLEAN_TASK_NAME))
	}


	private void configureOCLint(Project project) {
		OCLintTask reportTask = project.task(OCLINT_REPORT_TASK_NAME, type: OCLintTask, group: ANALYTICS_GROUP_NAME)

		Task ocLintTask = project.getTasks().create(OCLINT_TASK_NAME);
		ocLintTask.group = ANALYTICS_GROUP_NAME
		ocLintTask.description = "Runs: " + BasePlugin.CLEAN_TASK_NAME + " " + XCODE_BUILD_TASK_NAME + " " + OCLINT_REPORT_TASK_NAME
		ocLintTask.dependsOn(project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME))

		XcodeBuildTask xcodeBuildTask = project.getTasks().getByName(XcodePlugin.XCODE_BUILD_TASK_NAME)
		reportTask.mustRunAfter(xcodeBuildTask)

		ocLintTask.dependsOn(xcodeBuildTask)
		ocLintTask.dependsOn(reportTask)

	}

	private void configureAppCenter(Project project) {
		project.task(APPCENTER_CLEAN_TASK_NAME, type: AppCenterCleanTask, group: APPCENTER_GROUP_NAME)
		Task uploadIpaTask = project.task(APPCENTER_IPA_UPLOAD_TASK_NAME, type: AppCenterUploadTask, group: APPCENTER_GROUP_NAME)
		Task dsymUploadTask = project.task(APPCENTER_DSYM_UPLOAD_TASK_NAME, type: AppCenterDsymUploadTask, group: APPCENTER_GROUP_NAME)

		Task uploadWithDsymTask = project.getTasks().create(APPCENTER_TASK_NAME)
		uploadWithDsymTask.group = APPCENTER_GROUP_NAME
		uploadWithDsymTask.description = "Runs: " + APPCENTER_IPA_UPLOAD_TASK_NAME + " " + APPCENTER_DSYM_UPLOAD_TASK_NAME
		dsymUploadTask.mustRunAfter(uploadIpaTask)
		uploadWithDsymTask.dependsOn(uploadIpaTask)
		uploadWithDsymTask.dependsOn(dsymUploadTask)
	}

}


