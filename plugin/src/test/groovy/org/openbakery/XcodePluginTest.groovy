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
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.hockeykit.HockeyKitArchiveTask
import org.openbakery.hockeykit.HockeyKitImageTask
import org.openbakery.hockeykit.HockeyKitManifestTask
import org.openbakery.hockeykit.HockeyKitPluginExtension
import org.openbakery.signing.CodesignTask
import org.openbakery.signing.KeychainCreateTask
import org.openbakery.signing.ProvisioningInstallTask
import org.openbakery.testflight.TestFlightPluginExtension
import org.openbakery.testflight.TestFlightPrepareTask
import org.openbakery.testflight.TestFlightUploadTask
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
		assert project.tasks.findByName('xcodebuild-clean') instanceof XcodeBuildCleanTask
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
		assert project.tasks.findByName('build') instanceof XcodeBuildTask
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
			if (task.getClass().getName().startsWith("org.openbakery.XcodeBuildTask")) {
				assert task.group == 'build'
			} else if (task.getClass().getName().startsWith("org.openbakery.Xcode")) {
				assert task.group == XcodePlugin.XCODE_GROUP_NAME
			} else if (task.getClass().getName().startsWith("org.openbakery.HockeyKit")) {
				assert task.group == XcodePlugin.HOCKEYKIT_GROUP_NAME
			}
			if (task.getClass().getName().startsWith("org.openbakery.HockeyApp")) {
				assert task.group == XcodePlugin.HOCKEYAPP_GROUP_NAME
			}
		}
	}

	@Test
	void contain_extension_xcodebuild() {
		assert project.extensions.findByName('xcodebuild') instanceof XcodeBuildPluginExtension
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
