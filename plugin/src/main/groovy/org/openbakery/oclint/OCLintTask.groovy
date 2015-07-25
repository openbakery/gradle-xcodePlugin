package org.openbakery.oclint

import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask

/**
 * Created by rene on 22.07.15.
 */
class OCLintTask extends AbstractXcodeTask {

	File outputDirectory
	File downloadDirectory

	OCLintTask() {
		super()
		this.description = "Runs OCLint for the given project"
	}


	def download() {
		outputDirectory = new File("${project.buildDir.absolutePath}/oclint")
		downloadDirectory = new File("${project.gradle.gradleUserHomeDir.absolutePath}/ios")
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}
		if (!downloadDirectory.exists()) {
			downloadDirectory.mkdirs()
		}
		ant.get(src: 'http://archives.oclint.org/releases/0.8/oclint-0.8.1-x86_64-darwin-14.0.0.tar.gz', dest: downloadDirectory, verbose:true)

		commandRunner.run(downloadDirectory.absolutePath, ["tar", "xzvf", "oclint-0.8.1-x86_64-darwin-14.0.0.tar.gz"])

	}
	@TaskAction
	def run() {
		OCLintPluginExtension oclintExtension = project.oclint
		download()

		def oclintXcodebuild = new File(downloadDirectory, 'oclint-0.8.1/bin/oclint-xcodebuild').absolutePath

		commandRunner.run([oclintXcodebuild, 'build/xcodebuild-output.txt'])

		def oclint = new File(downloadDirectory, 'oclint-0.8.1/bin/oclint-json-compilation-database').absolutePath

		def report = new File(outputDirectory, "oclint.${oclintExtension.reportType}").absolutePath

		def ocLintParameters = [oclint]

		if (oclintExtension.excludePods) {
			ocLintParameters.addAll(["-e", "Pods"])
		}

		ocLintParameters.addAll(['--', "-report-type"])
		ocLintParameters << oclintExtension.reportType

		for (String rule : oclintExtension.rules) {
			ocLintParameters << "-rc"
			ocLintParameters << rule
		}

		ocLintParameters << "-o"
		ocLintParameters << report
		commandRunner.run(ocLintParameters)
	}


}
