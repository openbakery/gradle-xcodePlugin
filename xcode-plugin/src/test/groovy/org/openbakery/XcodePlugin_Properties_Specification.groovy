package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class XcodePlugin_Properties_Specification extends Specification {

	File testProjectDir
 	File buildFile

	Project project

	void setup() {
		project = ProjectBuilder.builder().build()

		testProjectDir = new File(System.getProperty("java.io.tmpdir"), "gxp-test")
		testProjectDir.mkdirs()

		buildFile = new File(testProjectDir, "build.gradle")

		buildFile << """
		            plugins {
		                id 'org.openbakery.xcode-plugin'
		            }
		        """

	}

	def cleanup() {
		FileUtils.deleteDirectory(testProjectDir)
		buildFile = null
		project = null
	}

	BuildResult run(String parameter)  {
		return GradleRunner.create()
					.withProjectDir(testProjectDir)
					.withArguments('showProperty', parameter)
					.withPluginClasspath()
					.build()
	}


	def "configure properties"(String key, String value) {
		given:
		buildFile << """
			tasks.register('showProperty') {
				doLast {
					println "VALUE: " + project.${key}
    		}
			}
     """

		when:
		def result = run("-P${key}=${value}")

		then:
		result.output.contains("VALUE: " + value)
		result.task(":showProperty").outcome == SUCCESS

		where:
		key                                      | value
		"appstore.apiKey"                        | "key"
		"appstore.apiIssuer"                     | "issuer"
		"appstore.username"                      | "user"
		"appstore.password"                      | "password"
		"appstore.publicId"                      | "publicId"
		"appstore.appleId"                       | "appleId"
		"appstore.ascProvider"                   | "ascProvider"
		"appstore.bundleVersion"                 | "bundleVersion"
		"appstore.shortBundleVersion"            | "shortBundleVersion"
		"appstore.bundleIdentifier"              | "bundleIdentifier"
		"appstore.useNewUpload"                  | true
		"infoplist.bundleIdentifier"             | "bundleIdentifier"
		"infoplist.bundleIdentifierSuffix"       | "bundleIdentifierSuffix"
		"infoplist.bundleDisplayName"            | "bundleDisplayName"
		"infoplist.bundleDisplayNameSuffix"      | "bundleDisplayNameSuffix"
		"infoplist.version"                      | "version"
		"infoplist.versionPrefix"                | "versionPrefix"
		"infoplist.versionSuffix"                | "versionSuffix"
		"infoplist.shortVersionString"           | "shortVersionString"
		"infoplist.shortVersionStringSuffix"     | "shortVersionStringSuffix"
		"infoplist.shortVersionStringPrefix"     | "shortVersionStringPrefix"
		"xcodebuild.scheme"                      | "scheme"
		"xcodebuild.infoPlist"                   | "infoPlist"
		"xcodebuild.configuration"               | "configuration"
		"xcodebuild.target"                      | "target"
		"xcodebuild.signing.identity"            | "identity"
		"xcodebuild.signing.certificateURI"      | "certificateURI"
		"xcodebuild.signing.certificatePassword" | "certificatePassword"
		"xcodebuild.additionalParameters"        | "additionalParameters"
		"xcodebuild.bundleNameSuffix"            | "bundleNameSuffix"
		"deploygate.outputDirectory"             | new File("outputDirectory").absolutePath
		"deploygate.apiToken"                    | "apiToken"
		"deploygate.userName"                    | "userName"
		"deploygate.message"                     | "message"
		"crashlytics.submitCommand"              | "submitCommand"
		"crashlytics.apiKey"                     | "apiKey"
		"crashlytics.buildSecret"                | "buildSecret"
		"crashlytics.notesPath"                  | "notesPath"
		"coverage.outputFormat"                  | "outputFormat"
		"coverage.exclude"                       | "exclude"
		"appcenter.appOwner"                     | "appOwner"
		"appcenter.appName"                      | "appName"
		"appcenter.apiToken"                     | "apiToken"

	}



}
