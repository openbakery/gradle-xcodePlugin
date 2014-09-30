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
import org.openbakery.appledoc.AppledocCleanTask
import org.openbakery.appledoc.AppledocTask
import org.openbakery.cocoapods.CocoapodsTask
import org.openbakery.coverage.CoverageCleanTask
import org.openbakery.coverage.CoveragePluginExtension
import org.openbakery.coverage.CoverageTask
import org.openbakery.deploygate.DeployGateCleanTask
import org.openbakery.deploygate.DeployGatePluginExtension
import org.openbakery.deploygate.DeployGatePrepareTask
import org.openbakery.deploygate.DeployGateUploadTask
import org.openbakery.hockeykit.HockeyAppCleanTask
import org.openbakery.hockeykit.HockeyAppPluginExtension
import org.openbakery.hockeykit.HockeyAppPrepareTask
import org.openbakery.hockeykit.HockeyAppUploadTask
import org.openbakery.hockeykit.HockeyKitArchiveTask
import org.openbakery.hockeykit.HockeyKitCleanTask
import org.openbakery.hockeykit.HockeyKitImageTask
import org.openbakery.hockeykit.HockeyKitManifestTask
import org.openbakery.hockeykit.HockeyKitPluginExtension
import org.openbakery.hockeykit.HockeyKitReleaseNotesTask
import org.openbakery.signing.CodesignTask
import org.openbakery.signing.KeychainCleanupTask
import org.openbakery.signing.KeychainCreateTask
import org.openbakery.signing.ProvisioningCleanupTask
import org.openbakery.signing.ProvisioningInstallTask
import org.openbakery.sparkle.SparkleArchiveTask
import org.openbakery.sparkle.SparkleCleanTask
import org.openbakery.sparkle.SparklePluginExtension
import org.openbakery.sparkle.SparkleReleaseNotesTask
import org.openbakery.testflight.TestFlightCleanTask
import org.openbakery.testflight.TestFlightPluginExtension
import org.openbakery.testflight.TestFlightPrepareTask
import org.openbakery.testflight.TestFlightUploadTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class XcodePlugin implements Plugin<Project> {

	private static Logger logger = LoggerFactory.getLogger(XcodePlugin.class)

	public static final String XCODE_GROUP_NAME = "Xcode"
	public static final String HOCKEYKIT_GROUP_NAME = "HockeyKit"
	public static final String HOCKEYAPP_GROUP_NAME = "HockeyApp"
	public static final String TESTFLIGHT_GROUP_NAME = "TestFlight"
	public static final String DEPLOYGATE_GROUP_NAME = "DeployGate"
	public static final String SPARKLE_GROUP_NAME = "sparkle"
	public static final String APPLE_DOC_GROUP_NAME = "Appledoc"
	public static final String COVERAGE_GROUP_NAME = "Coverage"
	public static final String COCOAPODS_GROUP_NAME = "Cocoapods"

	public static final String BUILD_TASK_NAME = "build";
	public static final String TEST_TASK_NAME = "test"
	public static final String ARCHIVE_TASK_NAME = "archive"
	public static final String LIST_SIMULATORS_TASK_NAME = "list-simulators"
	public static final String XCODE_BUILD_TASK_NAME = "xcodebuild"
	public static final String XCODE_CLEAN_TASK_NAME = "xcodebuild-clean"
	public static final String HOCKEYKIT_MANIFEST_TASK_NAME = "hockeykit-manifest"
	public static final String HOCKEYKIT_ARCHIVE_TASK_NAME = "hockeykit-archive"
	public static final String HOCKEYKIT_NOTES_TASK_NAME = "hockeykit-notes"
	public static final String HOCKEYKIT_IMAGE_TASK_NAME = "hockeykit-image"
	public static final String HOCKEYKIT_CLEAN_TASK_NAME = "hockeykit-clean"
	public static final String HOCKEYKIT_TASK_NAME = "hockeykit"
	public static final String KEYCHAIN_CREATE_TASK_NAME = "keychain-create"
	public static final String KEYCHAIN_CLEAN_TASK_NAME = "keychain-clean"
	public static final String INFOPLIST_MODIFY_TASK_NAME = 'infoplist-modify'
	public static final String PROVISIONING_INSTALL_TASK_NAME = 'provisioning-install'
	public static final String PROVISIONING_CLEAN_TASK_NAME = 'provisioning-clean'
	public static final String CODESIGN_TASK_NAME = 'codesign'
	public static final String TESTFLIGHT_PREPARE_TASK_NAME = 'testflight-prepare'
	public static final String TESTFLIGHT_TASK_NAME = 'testflight'
	public static final String TESTFLIGHT_CLEAN_TASK_NAME = 'testflight-clean'
	public static final String HOCKEYAPP_CLEAN_TASK_NAME = 'hockeyapp-clean'
	public static final String HOCKEYAPP_PREPARE_TASK_NAME = 'hockeyapp-prepare'
	public static final String HOCKEYAPP_TASK_NAME = 'hockeyapp'
	public static final String DEPLOYGATE_PREPARE_TASK_NAME = 'deploygate-prepare'
	public static final String DEPLOYGATE_TASK_NAME = 'deploygate'
	public static final String DEPLOYGATE_CLEAN_TASK_NAME = 'deploygate-clean'
	public static final String SPARKLE_TASK_NAME = 'sparkle'
	public static final String SPARKLE_ARCHIVE_TASK_NAME = 'sparkle-archive'
	public static final String SPARKLE_NOTES_TASK_NAME = 'sparkle-notes'
	public static final String SPARKLE_CLEAN_TASK_NAME = 'sparkle-clean'
	public static final String COCOAPODS_TASK_NAME = 'cocoapods'

	public static final String APPLEDOC_TASK_NAME = 'appledoc'
	public static final String APPLEDOC_CLEAN_TASK_NAME = 'appledoc-clean'

	public static final COVERAGE_TASK_NAME = 'coverage'
	public static final COVERAGE_CLEAN_TASK_NAME = 'coverage-clean'


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
		configureTestflight(project)
		configureHockeyApp(project)
		configureDeployGate(project)
		configureCodesign(project)
		configureSparkle(project)
		configureAppledoc(project)
		configureCoverage(project)
		configureCocoapods(project)
		configureInfo(project)

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

			if (project.hasProperty('xcodebuild.additionalParameters')) {
				project.xcodebuild.additionalParameters = project['xcodebuild.additionalParameters']
			}
			if (project.hasProperty('xcodebuild.bundleNameSuffix')) {
				project.xcodebuild.bundleNameSuffix = project['xcodebuild.bundleNameSuffix']
			}
			if (project.hasProperty('xcodebuild.arch')) {
				project.xcodebuild.arch = project['xcodebuild.arch']
			}
			if (project.hasProperty('xcodebuild.version')) {
				project.xcodebuild.version = project['xcodebuild.version']
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


			if (project.hasProperty('testflight.outputDirectory')) {
				project.testflight.outputDirectory = project['testflight.outputDirectory']
			}

			if (project.hasProperty('testflight.apiToken')) {
				project.testflight.apiToken = project['testflight.apiToken']
			}
			if (project.hasProperty('testflight.teamToken')) {
				project.testflight.teamToken = project['testflight.teamToken']
			}
			if (project.hasProperty('testflight.notes')) {
				project.testflight.notes = project['testflight.notes']
			}
			if (project.hasProperty('testflight.distributionLists')) {
				project.testflight.distributionLists = project['testflight.distributionLists']
			}
			if (project.hasProperty('testflight.notifyDistributionList')) {
				project.testflight.notifyDistributionList = project['testflight.notifyDistributionList']
			}
			if (project.hasProperty('testflight.replaceBuild')) {
				project.testflight.replaceBuild = project['testflight.replaceBuild']
			}
			
			if (project.hasProperty('sparkle.outputDirectory')) {
				project.sparkle.output = project['sparkle.outputDirectory']
			}
			if (project.hasProperty('sparkle.appName')) {
				project.sparkle.appname = project['sparkle.appName']
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

			if (project.hasProperty('coverage.outputFormat')) {
				project.coverage.outputFormat = project['coverage.outputFormat']
			}
			if (project.hasProperty('coverage.exclude')) {
				project.coverage.exclude = project['coverage.exclude']
			}


			project.xcodebuild.finishConfiguration(project);

		}


	}

	def void configureExtensions(Project project) {
		project.extensions.create("xcodebuild", XcodeBuildPluginExtension, project)
		project.extensions.create("infoplist", InfoPlistExtension)
		project.extensions.create("hockeykit", HockeyKitPluginExtension, project)
		project.extensions.create("testflight", TestFlightPluginExtension, project)
		project.extensions.create("hockeyapp", HockeyAppPluginExtension, project)
		project.extensions.create("deploygate", DeployGatePluginExtension, project)
		project.extensions.create("sparkle", SparklePluginExtension, project)
		project.extensions.create("coverage", CoveragePluginExtension, project)
	}

	private void configureBuild(Project project) {
		XcodeBuildTask buildTask = project.getTasks().create(BUILD_TASK_NAME, XcodeBuildTask.class);
		buildTask.setGroup(BasePlugin.BUILD_GROUP);
		buildTask.dependsOn(BasePlugin.ASSEMBLE_TASK_NAME);

		DefaultTask xcodebuildTask = project.getTasks().create(XCODE_BUILD_TASK_NAME, DefaultTask.class);
		xcodebuildTask.setDescription(buildTask.description);
		xcodebuildTask.setGroup(XCODE_GROUP_NAME);
		xcodebuildTask.dependsOn(buildTask);

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

	private void configureInfo(Project project) {
		ListSimulators  listSimulators = project.getTasks().create(LIST_SIMULATORS_TASK_NAME, ListSimulators.class);
		listSimulators.setGroup(XCODE_GROUP_NAME);
	}

	private void configureHockeyKit(Project project) {
		project.task(HOCKEYKIT_MANIFEST_TASK_NAME, type: HockeyKitManifestTask, group: HOCKEYKIT_GROUP_NAME)
		HockeyKitArchiveTask hockeyKitArchiveTask = project.task(HOCKEYKIT_ARCHIVE_TASK_NAME, type: HockeyKitArchiveTask, group: HOCKEYKIT_GROUP_NAME)
		hockeyKitArchiveTask.dependsOn(ARCHIVE_TASK_NAME)
		project.task(HOCKEYKIT_NOTES_TASK_NAME, type: HockeyKitReleaseNotesTask, group: HOCKEYKIT_GROUP_NAME)
		project.task(HOCKEYKIT_IMAGE_TASK_NAME, type: HockeyKitImageTask, group: HOCKEYKIT_GROUP_NAME)
		project.task(HOCKEYKIT_CLEAN_TASK_NAME, type: HockeyKitCleanTask, group: HOCKEYKIT_GROUP_NAME)

		DefaultTask hockeykitTask = project.task(HOCKEYKIT_TASK_NAME, type: DefaultTask, description: "Creates a build that can be deployed on a hockeykit Server", group: HOCKEYKIT_GROUP_NAME);
		hockeykitTask.dependsOn(HOCKEYKIT_ARCHIVE_TASK_NAME, HOCKEYKIT_MANIFEST_TASK_NAME, HOCKEYKIT_IMAGE_TASK_NAME, HOCKEYKIT_NOTES_TASK_NAME)
	}

	private void configureKeychain(Project project) {
		project.task(KEYCHAIN_CREATE_TASK_NAME, type: KeychainCreateTask, group: XCODE_GROUP_NAME)
		project.task(KEYCHAIN_CLEAN_TASK_NAME, type: KeychainCleanupTask, group: XCODE_GROUP_NAME)
	}

	private void configureTest(Project project) {
		project.task(TEST_TASK_NAME, type: XcodeTestTask, group: XCODE_GROUP_NAME)

	}

	private configureInfoPlist(Project project) {
		project.task(INFOPLIST_MODIFY_TASK_NAME, type: InfoPlistModifyTask, group: XCODE_GROUP_NAME)
	}

	private configureProvisioning(Project project) {
		project.task(PROVISIONING_INSTALL_TASK_NAME, type: ProvisioningInstallTask, group: XCODE_GROUP_NAME)
		project.task(PROVISIONING_CLEAN_TASK_NAME, type: ProvisioningCleanupTask, group: XCODE_GROUP_NAME)
	}

	private configureCodesign(Project project) {
		CodesignTask codesignTask = project.task(CODESIGN_TASK_NAME, type: CodesignTask, group: XCODE_GROUP_NAME)

		if (project.xcodebuild.signing.mobileProvisionURI != null) {
			logger.debug("added cleanup for provisioning profile")
			codesignTask.doLast {
				logger.debug("run provisioning cleanup")
				ProvisioningCleanupTask provisioningCleanup = project.getTasks().getByName(PROVISIONING_CLEAN_TASK_NAME)
				provisioningCleanup.clean()
			}
		}

		if (project.xcodebuild.signing != null && project.xcodebuild.signing.certificateURI != null) {
			logger.debug("added cleanup for certificate")
			codesignTask.doLast {
				logger.quiet("run certificate cleanup")
				KeychainCleanupTask keychainCleanup = project.getTasks().getByName(KEYCHAIN_CLEAN_TASK_NAME)
				keychainCleanup.clean()
			}
		}
	}

	private configureTestflight(Project project) {
		project.task(TESTFLIGHT_PREPARE_TASK_NAME, type: TestFlightPrepareTask, group: TESTFLIGHT_GROUP_NAME)
		project.task(TESTFLIGHT_TASK_NAME, type: TestFlightUploadTask, group: TESTFLIGHT_GROUP_NAME)
		project.task(TESTFLIGHT_CLEAN_TASK_NAME, type: TestFlightCleanTask, group: TESTFLIGHT_GROUP_NAME)
	}


	private void configureHockeyApp(Project project) {
		project.task(HOCKEYAPP_CLEAN_TASK_NAME, type: HockeyAppCleanTask, group: HOCKEYAPP_GROUP_NAME)
		project.task(HOCKEYAPP_PREPARE_TASK_NAME, type: HockeyAppPrepareTask, group: HOCKEYAPP_GROUP_NAME)
		project.task(HOCKEYAPP_TASK_NAME, type: HockeyAppUploadTask, group: HOCKEYAPP_GROUP_NAME)
	}
	
	private void configureSparkle(Project project) {
		project.task(SPARKLE_ARCHIVE_TASK_NAME, type: SparkleArchiveTask, group: SPARKLE_GROUP_NAME)
		project.task(SPARKLE_NOTES_TASK_NAME, type: SparkleReleaseNotesTask, group: SPARKLE_GROUP_NAME)
		project.task(SPARKLE_CLEAN_TASK_NAME, type: SparkleCleanTask, group: SPARKLE_GROUP_NAME)
			
		DefaultTask sparkleTask = project.task(SPARKLE_TASK_NAME, type: DefaultTask, description: "Creates a build that is compressed to ZIP including Sparkle framework", group: SPARKLE_GROUP_NAME);
		sparkleTask.dependsOn(SPARKLE_ARCHIVE_TASK_NAME)
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
		project.task(DEPLOYGATE_PREPARE_TASK_NAME, type: DeployGatePrepareTask, group: DEPLOYGATE_GROUP_NAME)
		project.task(DEPLOYGATE_TASK_NAME, type: DeployGateUploadTask, group: DEPLOYGATE_GROUP_NAME)
	}

	private void configureCocoapods(Project project) {

		//project.task(COCOAPODS_CLEAN_TASK_NAME, type: CocoapodsCleanTask, group: COCOAPODS_GROUP_NAME)
		CocoapodsTask task = project.task(COCOAPODS_TASK_NAME, type: CocoapodsTask, group: COCOAPODS_GROUP_NAME)
		if (task.hasPodfile()) {
			addDependencyToBuild(project, task);
		}
	}

	private void addDependencyToBuild(Project project, Task task) {
		XcodeBuildTask buildTask = project.getTasks().getByName(BUILD_TASK_NAME)
		buildTask.dependsOn(task)

		XcodeTestTask testTask = project.getTasks().getByName(TEST_TASK_NAME)
		testTask.dependsOn(task)
	}
}


