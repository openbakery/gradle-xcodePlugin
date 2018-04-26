package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskDependency
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.appstore.AppstorePluginExtension
import org.openbakery.appstore.AppstoreUploadTask
import org.openbakery.appstore.AppstoreValidateTask
import org.openbakery.carthage.CarthageCleanTask
import org.openbakery.carthage.CarthageUpdateTask
import org.openbakery.cocoapods.CocoapodsBootstrapTask
import org.openbakery.cocoapods.CocoapodsInstallTask
import org.openbakery.cocoapods.CocoapodsUpdateTask
import org.openbakery.cpd.CpdTask
import org.openbakery.hockeykit.HockeyKitArchiveTask
import org.openbakery.hockeykit.HockeyKitImageTask
import org.openbakery.hockeykit.HockeyKitManifestTask
import org.openbakery.hockeykit.HockeyKitPluginExtension
import org.openbakery.oclint.OCLintPluginExtension
import org.openbakery.oclint.OCLintTask
import org.openbakery.signing.KeychainCreateTask
import org.openbakery.signing.ProvisioningInstallTask
import spock.lang.Specification

import static org.hamcrest.Matchers.hasItem

class XcodePluginSpecification extends Specification {

	Project project


	void setup() {
		project = ProjectBuilder.builder().build()
		project.apply plugin: org.openbakery.XcodePlugin
	}


	def "not contain unknown task"() {
		expect:
		project.tasks.findByName('unknown-task') == null
	}


	def "contain task archive"() {
		expect:
		project.tasks.findByName('archive') instanceof XcodeBuildArchiveTask
	}


	def "contain task clean"() {
		expect:
		project.tasks.findByName('xcodebuildClean') instanceof XcodeBuildCleanTask
	}


	def "contain task infoplist modify"() {
		expect:
		project.tasks.findByName('infoplistModify') instanceof InfoPlistModifyTask
	}


	def "contain task keychain create"() {
		expect:
		project.tasks.findByName('keychainCreate') instanceof KeychainCreateTask
	}


	def "contain task provisioning install"() {
		expect:
		project.tasks.findByName('provisioningInstall') instanceof ProvisioningInstallTask
	}


	def "contain task xcodebuild"() {
		expect:
		project.tasks.findByName('xcodebuild') instanceof XcodeBuildTask
	}

	def "contain task xcodetest"() {
		expect:
		project.tasks.findByName('xcodetest') instanceof XcodeTestTask
	}

	def "contain task xcodebuildForTest"() {
		expect:
		project.tasks.findByName('xcodebuildForTest') instanceof XcodeBuildForTestTask
	}

	def "contain task xcodetestrun"() {
		expect:
		project.tasks.findByName('xcodetestrun') instanceof XcodeTestRunTask
	}

	def "appstoreUploadTask"() {
		expect:
		project.tasks.findByName('appstoreUpload') instanceof AppstoreUploadTask
	}



	def "appstoreValidateTask"() {
		expect:
		project.tasks.findByName('appstoreValidate') instanceof AppstoreValidateTask
	}



	def "contain task hockeykit"() {
		expect:
		project.tasks.findByName('hockeykit') instanceof DefaultTask
	}


	def "contain task hockeykit archive"() {
		expect:
		project.tasks.findByName('hockeykitArchive') instanceof HockeyKitArchiveTask
	}


	def "contain task hockeykit image"() {
		expect:
		project.tasks.findByName('hockeykitImage') instanceof HockeyKitImageTask
	}


	def "contain task hockeykit manifest"() {
		expect:
		project.tasks.findByName('hockeykitManifest') instanceof HockeyKitManifestTask
	}


	def "group tasks"() {
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


	def "contain extension xcodebuild"() {
		expect:
		project.extensions.findByName('xcodebuild') instanceof XcodeBuildPluginExtension
	}



	def "contain extension infoplist"() {
		expect:
		project.extensions.findByName('infoplist') instanceof InfoPlistExtension
	}


	def "contain extension hockeykit"() {
		expect:
		project.extensions.findByName('hockeykit') instanceof HockeyKitPluginExtension
	}


	def "contain extension appstore"() {
		expect:
		project.extensions.findByName('appstore') instanceof AppstorePluginExtension
	}



	def "contain extension oclint"() {
		expect:
		project.extensions.findByName('oclint') instanceof OCLintPluginExtension
	}


	def "oclint task"() {
		when:

		Task reportTask = project.tasks.findByName(XcodePlugin.OCLINT_REPORT_TASK_NAME)
		Task ocLintTask = project.tasks.findByName(XcodePlugin.OCLINT_TASK_NAME)
		TaskDependency dependency = reportTask.mustRunAfter

		then:
		reportTask instanceof OCLintTask
		ocLintTask instanceof Task
		ocLintTask.group == XcodePlugin.ANALYTICS_GROUP_NAME

		ocLintTask.dependsOn hasItem(project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME))
		ocLintTask.dependsOn hasItem(project.getTasks().getByName(XcodePlugin.XCODE_BUILD_TASK_NAME))
		ocLintTask.dependsOn hasItem(reportTask)

		dependency.getDependencies(reportTask) contains(project.getTasks().getByName(XcodePlugin.XCODE_BUILD_TASK_NAME))
	}

	def "cpd Tasks"() {
		when:
		CpdTask cpdTask = project.tasks.findByName(XcodePlugin.CPD_TASK_NAME)

		then:
		cpdTask instanceof CpdTask
		cpdTask.group == XcodePlugin.ANALYTICS_GROUP_NAME
	}

	def "has cocoapods update task"() {
		expect:
		project.tasks.findByName('cocoapodsUpdate') instanceof CocoapodsUpdateTask

	}

	def "has cocoapods install task"() {
		expect:
		project.tasks.findByName('cocoapodsInstall') instanceof CocoapodsInstallTask
	}

	def "xcodebuild has cocoapods dependency"() {
		when:
		File projectDir = new File("../example/iOS/SwiftExample")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin
		project.evaluate()

		XcodeBuildTask task = project.tasks.findByName('xcodebuild')

		then:

		task.getTaskDependencies().getDependencies() contains(project.getTasks().getByName(XcodePlugin.COCOAPODS_INSTALL_TASK_NAME))
	}

	def "xcodetest has cocoapods dependency"() {
		when:
		File projectDir = new File("../example/iOS/SwiftExample")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin
		project.evaluate()

		def task = project.tasks.findByName(XcodePlugin.XCODE_TEST_TASK_NAME)

		then:

		task.getTaskDependencies().getDependencies() contains(project.getTasks().getByName(XcodePlugin.COCOAPODS_INSTALL_TASK_NAME))
	}

	def "xcodebuildForTest has cocoapods dependency"() {
		when:
		File projectDir = new File("../example/iOS/SwiftExample")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin
		project.evaluate()

		def task = project.tasks.findByName(XcodePlugin.XCODE_BUILD_FOR_TEST_TASK_NAME)

		then:

		task.getTaskDependencies().getDependencies() contains(project.getTasks().getByName(XcodePlugin.COCOAPODS_INSTALL_TASK_NAME))
	}


	def "has cocoapods bootstrap task"() {
		expect:
		project.tasks.findByName('cocoapodsBootstrap') instanceof CocoapodsBootstrapTask
	}

	/*
	def "cocoapodsUpdate has boostrap dependency if cocoapods was not found"() {
		CommandRunner commandRunner = Mock(CommandRunner)
		project.commandRunner = commandRunner

		when:
		CocoapodsBootstrapTask task = project.tasks.findByName('cocoapodsBootstrap')
		project.apply plugin: org.openbakery.XcodePlugin

		then:
		task.commandRunner == commandRunner
	}
	*/

	def "has carthage task"() {
		expect:
		project.tasks.findByName('carthageUpdate') instanceof CarthageUpdateTask
	}

	def "xcodebuild has carthage bootstrap dependency"() {
		when:
		File projectDir = new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin

		project.evaluate()

		XcodeBuildTask task = project.tasks.findByName('xcodebuild')

		then:

		task.getTaskDependencies().getDependencies() contains(project.getTasks().getByName(XcodePlugin.CARTHAGE_BOOTSTRAP_TASK_NAME))
	}


	def "has carthage clean task"() {
		expect:
		project.tasks.findByName('carthageClean') instanceof CarthageCleanTask
	}

	def "has carthage update task"() {
		expect:
		project.tasks.findByName('carthageUpdate') instanceof CarthageUpdateTask
	}

	def "clean has carthage clean dependency"() {
		when:
		File projectDir = new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin
		project.evaluate()

		Task cleanTask = project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME)

		then:

		cleanTask.getTaskDependencies().getDependencies() contains(project.getTasks().getByName(XcodePlugin.CARTHAGE_CLEAN_TASK_NAME))
	}


}
