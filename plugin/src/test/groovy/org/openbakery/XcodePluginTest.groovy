package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

class XcodePluginTest {

	Project project

	@BeforeClass
	def setup() {
		project = ProjectBuilder.builder().build()
		project.apply plugin: org.openbakery.XcodePlugin
	}

	@Test
	void not_contain_unknown_task() {
		assert project.tasks.findByName('unknown-task') == null
	}

	@Test
	void contain_task_archive() {
		assert project.tasks.findByName('archive') instanceof XcodeBuildArchiveTask
	}

	@Test
	void contain_task_clean() {
		assert project.tasks.findByName('clean') instanceof XcodeBuildCleanTask
	}

	@Test
	void contain_task_codesign() {
		assert project.tasks.findByName('codesign') instanceof CodesignTask
	}

	@Test
	void contain_task_infoplist_modify() {
		assert project.tasks.findByName('infoplist-modify') instanceof InfoPlistModifyTask
	}

	@Test
	void contain_task_keychain_create() {
		assert project.tasks.findByName('keychain-create') instanceof KeychainCreateTask
	}

	@Test
	void contain_task_provisioning_install() {
		assert project.tasks.findByName('provisioning-install') instanceof ProvisioningInstallTask
	}

	@Test
	void contain_task_xcodebuild() {
		assert project.tasks.findByName('xcodebuild') instanceof XcodeBuildTask
	}

	/* TODO clarify if makes sense to exclude deploy tasks into another xcodeplugin?
e.g. xcodeplugin-deploy-testflight and xcodeplugin-deploy-hockeykit. */

	@Test
	void contain_task_testflight() {
		assert project.tasks.findByName('testflight') instanceof TestFlightUploadTask
	}

	@Test
	void contain_task_testflight_prepare() {
		assert project.tasks.findByName('testflight-prepare') instanceof TestFlightPrepareTask
	}

	@Test
	void contain_task_hockeykit() {
		assert project.tasks.findByName('hockeykit') instanceof DefaultTask
	}

	@Test
	void contain_task_hockeykit_archive() {
		assert project.tasks.findByName('hockeykit-archive') instanceof HockeyKitArchiveTask
	}

	@Test
	void contain_task_hockeykit_image() {
		assert project.tasks.findByName('hockeykit-image') instanceof HockeyKitImageTask
	}

	@Test
	void contain_task_hockeykit_manifest() {
		assert project.tasks.findByName('hockeykit-manifest') instanceof HockeyKitManifestTask
	}

	@Test
	void group_tasks() {
		project.tasks.each { task ->
			assert task.group == XcodePlugin.GROUP_NAME
		}
	}

	@Test
	void contain_extension_xcodebuild() {
		assert project.extensions.findByName('xcodebuild') instanceof XcodeBuildPluginExtension
	}

	@Test
	void contain_extension_keychain() {
		assert project.extensions.findByName('keychain') instanceof KeychainPluginExtension
	}

	@Test
	void contain_extension_provisioning() {
		assert project.extensions.findByName('provisioning') instanceof ProvisioningPluginExtension
	}

	@Test
	void contain_extension_infoplist() {
		assert project.extensions.findByName('infoplist') instanceof InfoPlistExtension
	}

	@Test
	void contain_extension_hockeykit() {
		assert project.extensions.findByName('hockeykit') instanceof HockeyKitPluginExtension
	}

	@Test
	void contain_extension_testflight() {
		assert project.extensions.findByName('testflight') instanceof TestFlightPluginExtension
	}

}
