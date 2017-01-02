package org.openbakery.oclint

import org.apache.commons.io.FilenameUtils
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask

/**
 * Created by rene on 22.07.15.
 */
class OCLintTask extends AbstractXcodeTask {

	File outputDirectory

	String oclintDirectoryName = "oclint-0.11"


	File oclintBinDirectory

	OCLintTask() {
		super()
		this.description = "Create a OCLint report for the given project"
	}


	def download() {
		File tmpDirectory = getTemporaryDirectory("oclint")
		if (!tmpDirectory.exists()) {
			tmpDirectory.mkdirs()
		}

		String downloadURL = downloadURL()
		ant.get(src: downloadURL, dest: tmpDirectory, verbose:true)
		String archiveName = FilenameUtils.getName(downloadURL)


		def command = [
						'tar',
						'xzf',
						new File(tmpDirectory, archiveName).absolutePath,
						'-C',
						tmpDirectory.absolutePath
		]
		commandRunner.run(command)

		return tmpDirectory
	}


	def downloadURL() {
		if (getOSVersion().minor >= 12) {
			return "https://github.com/oclint/oclint/releases/download/v0.11/oclint-0.11-x86_64-darwin-16.0.0.tar.gz"
		}
		return "https://github.com/oclint/oclint/releases/download/v0.11/oclint-0.11-x86_64-darwin-15.6.0.tar.gz"
	}

	@TaskAction
	def run() {
		outputDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("report/oclint")
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}
		def tmpDirectory = download()

		oclintBinDirectory = new File(tmpDirectory, oclintDirectoryName + "/bin")
		def oclintXcodebuild = new File(oclintBinDirectory, 'oclint-xcodebuild').absolutePath

		commandRunner.run([oclintXcodebuild, 'build/xcodebuild-output.txt'])

		def oclint = new File(oclintBinDirectory, 'oclint-json-compilation-database').absolutePath
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

		for (String rule : project.oclint.disableRules) {
			ocLintParameters << "-disable-rule=" + rule
		}

		for (String rule : project.oclint.rules) {
			ocLintParameters << "-rc=" + rule
		}


		ocLintParameters << "-o"
		ocLintParameters << report
		commandRunner.run(ocLintParameters)
	}


}
