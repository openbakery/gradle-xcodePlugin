package org.openbakery.signing

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Paths

class ProvisioningInstallTaskFunctionalTest extends Specification {
	@Rule
	final TemporaryFolder testProjectDir = new TemporaryFolder()

	List<File> pluginClasspath

	File buildFile
	File provisioningFile1

	def setup() {
		buildFile = testProjectDir.newFile('build.gradle')

		buildFile << """
            plugins {
                id 'org.openbakery.xcode-plugin'
            }
        """

		provisioningFile1 = findResource("test1.mobileprovision")
		assert provisioningFile1.exists()

		def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
		if (pluginClasspathResource == null) {
			throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
		}

		pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }
	}

	def "The task list should contain the task"() {
		when:
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments('tasks')
				.withPluginClasspath(pluginClasspath)
				.build()

		then:
		result.output.contains(ProvisioningInstallTask.TASK_NAME
				+ " - "
				+ ProvisioningInstallTask.TASK_DESCRIPTION)
	}

	def "If no provisioning is defined, then the task should be skipped"() {
		when:
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments(ProvisioningInstallTask.TASK_NAME)
				.withPluginClasspath(pluginClasspath)
				.build()

		then:
		result.task(":" + ProvisioningInstallTask.TASK_NAME)
				.outcome == TaskOutcome.SKIPPED
	}

	def "If provisioning list is empty, then the task should be skipped"() {
		setup:
		buildFile << """
			xcodebuild {
				signing {
					mobileProvisionURI = []
				}
			}
		"""

		when:
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments(ProvisioningInstallTask.TASK_NAME)
				.withPluginClasspath(pluginClasspath)
				.build()

		then:
		result.task(":" + ProvisioningInstallTask.TASK_NAME)
				.outcome == TaskOutcome.SKIPPED
	}

	def "The provisioning can be configured via the mobileProvisionList list"() {
		setup:
		buildFile << """
			xcodebuild {
				signing {
					mobileProvisionList = ["${provisioningFile1.toURI().toString()}"]
				}
			}
		"""

		when:
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments(ProvisioningInstallTask.TASK_NAME)
				.withPluginClasspath(pluginClasspath)
				.withDebug(true)
				.build()

		then:
		result.task(":" + ProvisioningInstallTask.TASK_NAME)
				.outcome == TaskOutcome.SUCCESS
	}

	@Unroll
	def "With gradle version : #gradleVersion If provisioning list is present, then the task should be skipped"() {
		setup:
		buildFile << """
			xcodebuild {
				signing {
					mobileProvisionURI = "${provisioningFile1.toURI().toString()}"
				}
			}
		"""

		when:
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments(ProvisioningInstallTask.TASK_NAME)
				.withPluginClasspath(pluginClasspath)
				.withGradleVersion(gradleVersion)
				.build()

		then:
		result.task(":" + ProvisioningInstallTask.TASK_NAME)
				.outcome == TaskOutcome.SUCCESS

		and: "The temporary provisioning provisioningFile1 should be deleted"
		new File(testProjectDir.root, "build/provision")
				.listFiles().size() == 0

		where:
		gradleVersion | _
		"4.4"         | _
		"4.5"         | _
		"4.6"         | _
		"4.7"         | _
	}

	private File findResource(String name) {
		ClassLoader classLoader = getClass().getClassLoader()
		return (File) Optional.ofNullable(classLoader.getResource(name))
				.map { URL url -> url.toURI() }
				.map { URI uri -> Paths.get(uri).toFile() }
				.filter { File file -> file.exists() }
				.orElseThrow { new Exception("Resource $name cannot be found") }
	}
}
