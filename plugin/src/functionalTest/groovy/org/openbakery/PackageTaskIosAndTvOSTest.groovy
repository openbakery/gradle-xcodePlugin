package org.openbakery

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.openbakery.packaging.PackageTaskIosAndTvOS
import spock.lang.Specification

class PackageTaskIosAndTvOSTest extends Specification {

	@Rule
	final TemporaryFolder testProjectDir = new TemporaryFolder()

	List<File> pluginClasspath

	File buildFile

	def setup() {
		buildFile = testProjectDir.newFile('build.gradle')

		def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
		if (pluginClasspathResource == null) {
			throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
		}

		pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }
	}

	def "The task list should contain the task"() {
		given:
		buildFile << """
            plugins {
                id 'org.openbakery.xcode-plugin'
            }
        """

		when:
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments('tasks')
				.withPluginClasspath(pluginClasspath)
				.build()

		then:
		result.output.contains(PackageTaskIosAndTvOS.NAME
				+ " - "
				+ PackageTaskIosAndTvOS.DESCRIPTION)
	}
}
