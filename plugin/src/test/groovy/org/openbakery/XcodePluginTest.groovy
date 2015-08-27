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
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskDependency
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.appstore.AppstorePluginExtension
import org.openbakery.appstore.AppstoreValidateTask
import org.openbakery.cpd.CpdTask
import org.openbakery.hockeykit.HockeyKitArchiveTask
import org.openbakery.hockeykit.HockeyKitImageTask
import org.openbakery.hockeykit.HockeyKitManifestTask
import org.openbakery.hockeykit.HockeyKitPluginExtension
import org.openbakery.oclint.OCLintPluginExtension
import org.openbakery.oclint.OCLintTask
import org.openbakery.signing.KeychainCreateTask
import org.openbakery.signing.ProvisioningInstallTask
import org.openbakery.appstore.AppstoreUploadTask
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*

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
		assert project.tasks.findByName('xcodebuildClean') instanceof XcodeBuildCleanTask
	}


	@Test
	void contain_task_infoplist_modify() {
		assert project.tasks.findByName('infoplistModify') instanceof InfoPlistModifyTask
	}

    @Test
    void contain_task_entitlements_modify() {
        assert project.tasks.findByName('entitlementsModify') instanceof EntitlementsModifyTask
    }

	@Test
	void contain_task_keychain_create() {
		assert project.tasks.findByName('keychainCreate') instanceof KeychainCreateTask
	}

	@Test
	void contain_task_provisioning_install() {
		assert project.tasks.findByName('provisioningInstall') instanceof ProvisioningInstallTask
	}

	@Test
	void contain_task_xcodebuild() {
		assert project.tasks.findByName('xcodebuild') instanceof XcodeBuildTask
	}

	@Test
	void appstoreUploadTask() {
		assert project.tasks.findByName('appstoreUpload') instanceof AppstoreUploadTask
	}


	@Test
	void appstoreValidateTask() {
		assert project.tasks.findByName('appstoreValidate') instanceof AppstoreValidateTask
	}


	@Test
	void contain_task_hockeykit() {
		assert project.tasks.findByName('hockeykit') instanceof DefaultTask
	}

	@Test
	void contain_task_hockeykit_archive() {
		assert project.tasks.findByName('hockeykitArchive') instanceof HockeyKitArchiveTask
	}

	@Test
	void contain_task_hockeykit_image() {
		assert project.tasks.findByName('hockeykitImage') instanceof HockeyKitImageTask
	}

	@Test
	void contain_task_hockeykit_manifest() {
		assert project.tasks.findByName('hockeykitManifest') instanceof HockeyKitManifestTask
	}

	@Test
	void group_tasks() {
		project.tasks.each { task ->
			if (task.getClass().getName().startsWith("org.openbakery.XcodeBuildTask")) {
				assert task.group == XcodePlugin.XCODE_GROUP_NAME
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
	void contain_extension_appstore() {
		assert project.extensions.findByName('appstore') instanceof AppstorePluginExtension
	}


	@Test
	void contain_extension_oclint() {
		assertThat(project.extensions.findByName('oclint'), is(instanceOf(OCLintPluginExtension.class)));
	}

	@Test
	void oclintTasks() {
		OCLintTask reportTask = project.tasks.findByName(XcodePlugin.OCLINT_REPORT_TASK_NAME)

		assertThat(reportTask, is(instanceOf(OCLintTask)))

		Task ocLintTask = project.tasks.findByName(XcodePlugin.OCLINT_TASK_NAME)
		assertThat(ocLintTask, is(instanceOf(Task.class)))
		assertThat(ocLintTask.group, is(XcodePlugin.ANALYTICS_GROUP_NAME))

		assertThat(ocLintTask.dependsOn, hasItem(project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME)))
		assertThat(ocLintTask.dependsOn, hasItem(project.getTasks().getByName(XcodePlugin.XCODE_BUILD_TASK_NAME)))
		assertThat(ocLintTask.dependsOn, hasItem(reportTask))


		TaskDependency dependency = reportTask.mustRunAfter

		assertThat(dependency.getDependencies(reportTask), hasItem(project.getTasks().getByName(XcodePlugin.XCODE_BUILD_TASK_NAME)))
	}


	@Test
	void cpdTasks() {
		CpdTask cpdTask = project.tasks.findByName(XcodePlugin.CPD_TASK_NAME)
		assertThat(cpdTask, is(instanceOf(CpdTask)))

		assertThat(cpdTask.group, is(XcodePlugin.ANALYTICS_GROUP_NAME))


	}
}
