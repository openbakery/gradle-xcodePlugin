package org.openbakery.oclint

import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask

/**
 * Created by rene on 22.07.15.
 */
class OCLintTask extends AbstractXcodeTask {

	File outputDirectory

	OCLintTask() {
		super()
		this.description = "Runs OCLint for the given project"
	}


	def download() {
		outputDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("oclint")
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}
		ant.get(src: 'http://archives.oclint.org/releases/0.8/oclint-0.8.1-x86_64-darwin-14.0.0.tar.gz', dest: outputDirectory, verbose:true)

		def oclintXcodebuild = new File(outputDirectory, 'oclint-0.8.1/bin/oclint-xcodebuild').absolutePath


		ant.gunzip(src: new File(outputDirectory, "oclint-0.8.1-x86_64-darwin-14.0.0.tar.gz").absolutePath )
		ant.untar(src : new File(outputDirectory, "oclint-0.8.1-x86_64-darwin-14.0.0.tar").absolutePath, dest:outputDirectory.absolutePath)

	}
	@TaskAction
	def run() {
		download()

		def oclintXcodebuild = new File(outputDirectory, 'oclint-0.8.1/bin/oclint-xcodebuild').absolutePath

		commandRunner.run([oclintXcodebuild, 'build/xcodebuild-output.txt'])

		def oclint = new File(outputDirectory, 'oclint-0.8.1/bin/oclint-json-compilation-database').absolutePath
		def report = new File(outputDirectory, 'oclint.html').absolutePath

		def ocLintParameters = [oclint, '--', "-report-type"]
		ocLintParameters << project.oclint.reportType

		for (String rule : project.oclint.rules) {
			ocLintParameters << "-rc"
			ocLintParameters << rule
		}

		ocLintParameters << "-o"
		ocLintParameters << report
		commandRunner.run(ocLintParameters)
	}


}
