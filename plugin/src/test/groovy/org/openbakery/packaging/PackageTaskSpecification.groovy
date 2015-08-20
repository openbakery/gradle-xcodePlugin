package org.openbakery.packaging

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.AbstractXcodeTask
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.XcodePlugin
import org.openbakery.configuration.XcodeConfig
import org.openbakery.internal.XcodeBuildSpec
import spock.lang.Specification

/**
 * Created by rene on 20.08.15.
 */
class PackageTaskSpecification extends Specification {

	Project project
	PackageTask packageTask;


	File projectDir

	void setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		/*
		project.xcodebuild.productName = 'Example'
		project.xcodebuild.productType = 'app'
		project.xcodebuild.sdk = XcodePlugin.SDK_IPHONEOS
		project.xcodebuild.signing.keychain = "/var/tmp/gradle.keychain"
*/

		packageTask = project.getTasks().getByPath(XcodePlugin.PACKAGE_TASK_NAME)

	}



	def "depends on archive"() {

		expect:
		Set<? extends Task> dependencies = packageTask.getTaskDependencies().getDependencies();
		dependencies.size() == 1
		Task task = dependencies.getAt(0)
		task instanceof XcodeBuildArchiveTask

	}

	def "build spec was added on depends on "() {

		when:
		packageTask.config = Mock(XcodeConfig)
		packageTask.configureTask()

		then:
		Set<? extends Task> dependencies = packageTask.getTaskDependencies().getDependencies(packageTask);
		AbstractXcodeTask task = dependencies.getAt(0)
		task.buildSpec.parent == packageTask.buildSpec


	}

}
