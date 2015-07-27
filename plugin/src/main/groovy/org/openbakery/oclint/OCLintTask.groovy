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
		File downloadDirectory = new File(outputDirectory, "download")
		if (!downloadDirectory.exists()) {
			downloadDirectory.mkdirs()
		}
		ant.get(src: 'http://archives.oclint.org/releases/0.8/oclint-0.8.1-x86_64-darwin-14.0.0.tar.gz', dest: downloadDirectory, verbose:true)


		def command = [
						'tar',
						'xzf',
						new File(downloadDirectory, "oclint-0.8.1-x86_64-darwin-14.0.0.tar.gz").absolutePath,
						'-C',
						outputDirectory.absolutePath
		]
		commandRunner.run(command)
	}


	@TaskAction
	def run() {
		outputDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("oclint")

		download()

		def oclintXcodebuild = new File(outputDirectory, 'oclint-0.8.1/bin/oclint-xcodebuild').absolutePath

		commandRunner.run([oclintXcodebuild, 'build/xcodebuild-output.txt'])

		def oclint = new File(outputDirectory, 'oclint-0.8.1/bin/oclint-json-compilation-database').absolutePath
		def report = new File(outputDirectory, 'oclint.html').absolutePath

		def ocLintParameters = [oclint]


		for (String exclude : project.oclint.excludes) {
			ocLintParameters << "-e"
			ocLintParameters << exclude
		}
		ocLintParameters << '--'
		ocLintParameters << "-max-priority-1=" + project.oclint.maxPriority1
		ocLintParameters << "-max-priority-2=" + project.oclint.maxPriority2
		ocLintParameters << "-max-priority-3=" + project.oclint.maxPriority3
		ocLintParameters << '-report-type'

		ocLintParameters << project.oclint.reportType

		for (String rule : project.oclint.rules) {
			ocLintParameters << "-rc=" + rule
		}


		ocLintParameters << "-o"
		ocLintParameters << report
		commandRunner.run(ocLintParameters)
	}


}
