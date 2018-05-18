package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.openbakery.util.PathHelper
import spock.lang.Specification

import java.nio.file.Paths

class PrepareXcodeArchivingFunctionalTest extends Specification {

	@Rule
	final TemporaryFolder testProjectDir = new TemporaryFolder()

	List<File> pluginClasspath

	File buildFile


	File provisioningFileWildCard

	def setup() {
		pluginClasspath = findResource("plugin-classpath.txt")
				.readLines()
				.collect { new File(it) }

		FileUtils.copyDirectory(findResource("TestProject"), testProjectDir.getRoot())

		provisioningFileWildCard = findResource("test1.mobileprovision")
	}

	def setupBuildFile() {
		buildFile = testProjectDir.newFile('build.gradle')
		buildFile << """
            plugins {
                id 'org.openbakery.xcode-plugin'
            }
            
            xcodebuild {
				target = 'TestProject'
				scheme = "TestScheme"
            	signing {
            		mobileProvisionURI = "${provisioningFileWildCard.toURI().toString()}"
            	}
            }
            
        """
	}

	def "The task list should contain the task"() {
		given:
		setupBuildFile()

		when:
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments('tasks')
				.withPluginClasspath(pluginClasspath)
				.build()

		then:
		result.output.contains(PrepareXcodeArchivingTask.NAME
				+ " - "
				+ PrepareXcodeArchivingTask.DESCRIPTION)
	}

	def "The task should complete without error and generate the xcconfig file"() {
		setup:
		setupBuildFile()

		when:
		buildFile << """
			xcodebuild {
				infoplist {
					bundleIdentifier = "org.openbakery.test.ExampleWidget"
				}
			}
			"""

		final File certificate = findResource("fake_distribution.p12")
		assert certificate.exists()
		buildFile << """
			xcodebuild {
				infoplist {
					bundleIdentifier = "org.openbakery.test.ExampleWidget"
				}

				signing {
					certificateURI = "${certificate.toURI().toString()}"
					certificatePassword = "p4ssword"
				}
			}
			"""

		BuildResult result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments(PrepareXcodeArchivingTask.NAME)
				.withPluginClasspath(pluginClasspath)
				.build()

		then: "The task should complete without error"

		result.task(":" + PrepareXcodeArchivingTask.NAME)
				.outcome == TaskOutcome.SUCCESS

		and: "The archive xcconfig provisioningFile1 should be properly generated and populated from configured values"

		File outputFile = new File(testProjectDir.root, "build/"
				+ PathHelper.FOLDER_ARCHIVE
				+ "/" + PathHelper.GENERATED_XCARCHIVE_FILE_NAME)

		outputFile.exists()

		String text = outputFile.text
		text.contains("PRODUCT_BUNDLE_IDENTIFIER = org.openbakery.test.ExampleWidget")
		text.contains("iPhone Distribution: Test Company Name (12345ABCDE)")
		text.contains("PROVISIONING_PROFILE = XXXXFFFF-AAAA-BBBB-CCCC-DDDDEEEEFFFF")
		text.contains("PROVISIONING_PROFILE_SPECIFIER = ad hoc")
		text.contains("DEVELOPMENT_TEAM = XXXYYYZZZZ")

		and: "Should no contain any entitlements information"
		!text.contains("CODE_SIGN_ENTITLEMENTS =")
	}

	def "If present the entitlements file should be present into the xcconfig file"() {
		setup:
		setupBuildFile()

		when:
		buildFile << """
			xcodebuild {
				infoplist {
					bundleIdentifier = "org.openbakery.test.ExampleWidget"
				}
			}
			"""

		final File certificate = findResource("fake_distribution.p12")
		assert certificate.exists()

		final File entitlementsFile = findResource("fake.entitlements")
		assert entitlementsFile.exists()

		buildFile << """
			xcodebuild {
				infoplist {
					bundleIdentifier = "org.openbakery.test.ExampleWidget"
				}

				signing {
					certificateURI = "${certificate.toURI().toString()}"
					certificatePassword = "p4ssword"
					entitlementsFile = project.provisioningFile1("${entitlementsFile.absolutePath}")
				}
			}
			"""

		BuildResult result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments(PrepareXcodeArchivingTask.NAME)
				.withPluginClasspath(pluginClasspath)
				.build()

		then: "The task should complete without error"

		result.task(":" + PrepareXcodeArchivingTask.NAME)
				.outcome == TaskOutcome.SUCCESS

		and: "The archive xcconfig should contains the path to the entitlements provisioningFile1"

		File outputFile = new File(testProjectDir.root, "build/"
				+ PathHelper.FOLDER_ARCHIVE
				+ "/" + PathHelper.GENERATED_XCARCHIVE_FILE_NAME)

		outputFile.exists()

		String text = outputFile.text
		text.contains("CODE_SIGN_ENTITLEMENTS = ${entitlementsFile.absolutePath}")
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
