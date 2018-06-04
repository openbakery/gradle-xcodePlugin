package org.openbakery.carthage

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class CarthageBootStrapTaskFunctionalTest extends Specification {
	@Rule
	final TemporaryFolder testProjectDir = new TemporaryFolder()

	List<File> pluginClasspath
	File buildFile
	GradleRunner gradleRunner
	File carthageFolder

	void setup() {
		buildFile = testProjectDir.newFile('build.gradle')

		buildFile << """
            plugins {
                id 'org.openbakery.xcode-plugin'
            }
        """

		def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
		if (pluginClasspathResource == null) {
			throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
		}

		pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }

		gradleRunner = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments(CarthageBootStrapTask.NAME)
				.withPluginClasspath(pluginClasspath)

		carthageFolder = new File(testProjectDir.root, "Carthage")
	}

	def "The task list should contain the task"() {
		when:
		BuildResult result = gradleRunner.build()

		then:
		result.output.contains(CarthageBootStrapTask.NAME)
	}

	def "The task should be skipped if no cartfile is present"() {
		when:
		BuildResult result = gradleRunner.build()

		then:
		result.task(":" + CarthageBootStrapTask.NAME)
				.outcome == TaskOutcome.SKIPPED
	}

	def "The task should be executed with success if a `cartfile.resolved` file is present"() {
		setup:
		testProjectDir.newFile(CarthageBootStrapTask.CARTHAGE_FILE)

		when:
		BuildResult result = gradleRunner.build()

		then:
		result.task(":" + CarthageBootStrapTask.NAME)
				.outcome == TaskOutcome.SUCCESS
	}

	def "The task should resolve the defined carthage dependencies"() {
		setup:
		File carthageFile = testProjectDir.newFile("Cartfile")
		carthageFile << """
            github "ashleymills/Reachability.swift"
        """

		File carthageResolvedFile = testProjectDir.newFile(CarthageBootStrapTask.CARTHAGE_FILE)
		carthageResolvedFile << """
            github "ashleymills/Reachability.swift" "v4.1.0"
        """

		when:
		BuildResult result = gradleRunner
				.build()

		then:
		result.task(":" + CarthageBootStrapTask.NAME)
				.outcome == TaskOutcome.SUCCESS

		and: "The resolved framework should be existing only for iOS (default target)"
		carthageFolder.exists()
		new File(carthageFolder, "Build/iOS/Reachability.framework").exists()
		!new File(carthageFolder, "Build/tvOS/Reachability.framework").exists()

		when: "Force reset and build with cache enabled"
		assert carthageFolder.deleteDir()
		result = gradleRunner.withArguments('--build-cache', CarthageBootStrapTask.NAME)
				.build()

		then: "Should resolve the carthage dependencies from cache"
		result.task(":" + CarthageBootStrapTask.NAME)
				.outcome == TaskOutcome.FROM_CACHE

		new File(carthageFolder, "Build/iOS/Reachability.framework").exists()
		!new File(carthageFolder, "Build/tvOS/Reachability.framework").exists()
	}

	def "The task should resolve the defined carthage dependencies depending of the configured target"() {
		setup:
		File carthageFile = testProjectDir.newFile("Cartfile")
		carthageFile << """
            github "ashleymills/Reachability.swift"
        """

		File carthageResolvedFile = testProjectDir.newFile(CarthageBootStrapTask.CARTHAGE_FILE)
		carthageResolvedFile << """
            github "ashleymills/Reachability.swift" "v4.1.0"
        """

		buildFile << """
            xcodebuild {
                type = "tvOS"
            }
        """

		when:
		BuildResult result = gradleRunner.build()

		then:
		result.task(":" + CarthageBootStrapTask.NAME)
				.outcome == TaskOutcome.SUCCESS

		and: "The resolved framework should be existing only for iOS (default target)"
		carthageFolder.exists()
		!new File(carthageFolder, "Build/iOS/Reachability.framework").exists()
		new File(carthageFolder, "Build/tvOS/Reachability.framework").exists()
	}
}
