package org.openbakery

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class XcodePlugin_Properties_Specification extends Specification {


	@Rule TemporaryFolder testProjectDir = new TemporaryFolder()
 	File buildFile

	Project project

	void setup() {
		project = ProjectBuilder.builder().build()

		buildFile = testProjectDir.newFile('build.gradle')
		        buildFile << """
		            plugins {
		                id 'org.openbakery.xcode-plugin'
		            }
		        """

	}

	BuildResult run(String parameter)  {
		return GradleRunner.create()
					.withProjectDir(testProjectDir.root)
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
		//"xcodebuild.sdk"                     | "sdk"
		"xcodebuild.target"                      | "target"
//		"xcodebuild.dstRoot"                     | new File("dstRoot")
//		"xcodebuild.objRoot"                     | "objRoot"
//		"xcodebuild.symRoot"                     | "symRoot"
//		"xcodebuild.sharedPrecompsDir"           | "sharedPrecompsDir"
		"xcodebuild.signing.identity"            | "identity"
		"xcodebuild.signing.certificateURI"      | "certificateURI"
		"xcodebuild.signing.certificatePassword" | "certificatePassword"
//		"xcodebuild.signing.mobileProvisionURI"  | "mobileProvisionURI"
//		"xcodebuild.signing.keychain"            | "keychain"
//		"xcodebuild.signing.keychainPassword"    | "keychainPassword"
//		"xcodebuild.signing.timeout"             | "timeout"
		"xcodebuild.additionalParameters"        | "additionalParameters"
		"xcodebuild.bundleNameSuffix"            | "bundleNameSuffix"
//		"xcodebuild.arch"                        | "arch"
//		"xcodebuild.environment"                 | "environment"
//		"xcodebuild.version"                     | "version"
//		"xcodebuild.ipaFileName"                 | "ipaFileName"
//		"xcodebuild.destination"                 | "destination"
		"deploygate.outputDirectory"             | new File("outputDirectory").absolutePath
		"deploygate.apiToken"                    | "apiToken"
		"deploygate.userName"                    | "userName"
		"deploygate.message"                     | "message"
		"crashlytics.submitCommand"              | "submitCommand"
		"crashlytics.apiKey"                     | "apiKey"
		"crashlytics.buildSecret"                | "buildSecret"
//		"crashlytics.groupAliases"               | "groupAliases"
		"crashlytics.notesPath"                  | "notesPath"
//		"crashlytics.notifications"              | "notifications"
		"coverage.outputFormat"                  | "outputFormat"
		"coverage.exclude"                       | "exclude"
		"appcenter.appOwner"                     | "appOwner"
		"appcenter.appName"                      | "appName"
		"appcenter.apiToken"                     | "apiToken"
//		"appcenter.readTimeout"                  | "readTimeout"
//		"appcenter.destination"                  | "destination"
//		"appcenter.notifyTesters"                | "notifyTesters"
//		"appcenter.mandatoryUpdate"              | "mandatoryUpdate"

	}



}
