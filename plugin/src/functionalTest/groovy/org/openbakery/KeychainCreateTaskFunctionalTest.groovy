package org.openbakery

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.openbakery.signing.KeychainCreateTask
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Paths

class KeychainCreateTaskFunctionalTest extends Specification {

	@Rule
	final TemporaryFolder testProjectDir = new TemporaryFolder()

	List<File> pluginClasspath

	@Shared
	File certificate

	File buildFile

	def setup() {
		def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
		if (pluginClasspathResource == null) {
			throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
		}

		pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }

		certificate = findResource("fake_distribution.p12")
		assert certificate.exists()

		buildFile = testProjectDir.newFile('build.gradle')
		buildFile << """
            plugins {
                id 'org.openbakery.xcode-plugin'
            }
        """
	}

	def "The task list should contain the task"() {
		when:
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments('tasks')
				.withPluginClasspath(pluginClasspath)
				.build()

		then:
		result.output.contains(KeychainCreateTask.TASK_NAME
				+ " - "
				+ KeychainCreateTask.TASK_DESCRIPTION)
	}

	def "The task should be skipped if invalid configuration"() {
		when:
		BuildResult result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments(KeychainCreateTask.TASK_NAME)
				.withPluginClasspath(pluginClasspath)
				.build()

		then:
		result.output.contains("No signing certificate defined, will skip the keychain creation")
		result.output.contains("No signing certificate password defined, will skip the keychain creation")
		result.task(":" + KeychainCreateTask.TASK_NAME).outcome == TaskOutcome.SKIPPED
	}

	def "The task should be skipped if not certificate password is provided"() {
		setup:
		buildFile << """
			xcodebuild {
            	signing {
            		certificate = project.file("$certificate")  
            	}
			}            
			"""

		when:
		BuildResult result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments(KeychainCreateTask.TASK_NAME)
				.withPluginClasspath(pluginClasspath)
				.build()

		then:
		result.task(":" + KeychainCreateTask.TASK_NAME).outcome == TaskOutcome.SKIPPED
	}

	def "The task should be executed if configuration is valid"() {
		setup:
		buildFile << """
			xcodebuild {
            	signing {
            		certificate = project.file("$certificate")
					certificatePassword = "p4ssword"
            	}
			}            
			"""

		when:
		BuildResult result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments(KeychainCreateTask.TASK_NAME)
				.withPluginClasspath(pluginClasspath)
				.build()

		then:
		result.task(":" + KeychainCreateTask.TASK_NAME).outcome == TaskOutcome.SUCCESS
	}

	def "The task should automatically delete the temporary keychain file"() {
		setup:
		buildFile << """
			xcodebuild {
            	signing {
            		certificate = project.file("$certificate")
					certificatePassword = "p4ssword"
            	}
			}            
			"""

		when:
		BuildResult result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments(KeychainCreateTask.TASK_NAME)
				.withPluginClasspath(pluginClasspath)
				.build()

		then:
		result.task(":" + KeychainCreateTask.TASK_NAME).outcome == TaskOutcome.SUCCESS

		and: "The temporary certificate file should be deleted automatically"
		new File(testProjectDir.root, "build/codesign")
			.listFiles()
			.toList()
			.findAll {it.name.endsWith(".p12")}
			.empty

		and: "The temporary keychain file should be deleted automatically"
		new File(testProjectDir.root, "build/codesign")
				.listFiles()
				.toList()
				.findAll {it.name.endsWith(".keychain")}
				.empty

		println result.output
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
