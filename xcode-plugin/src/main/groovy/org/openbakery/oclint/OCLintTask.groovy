package org.openbakery.oclint

import org.apache.commons.io.FilenameUtils
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask

class OCLintTask extends AbstractXcodeTask {

	@OutputDirectory
	File outputDirectory

	@Internal
	String oclintDirectoryName = "oclint-0.13"


	OCLintTask() {
		super()
		this.description = "Create a OCLint report for the given project"
	}


	def download() {
		File tmpDirectory = getTemporaryDirectory()

		String downloadURL = getDownloadURL()
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
	}


	private def getDownloadURL() {
		if (getOSVersion().minor >= 12) {
			return "https://github.com/oclint/oclint/releases/download/v0.13/oclint-0.13-x86_64-darwin-17.0.0.tar.gz"
		}
		return "https://github.com/oclint/oclint/releases/download/v0.13/oclint-0.13-x86_64-darwin-16.7.0.tar.gz"
	}

	private File getTemporaryDirectory() {
		File tmpDirectory = getTemporaryDirectory("oclint")
		if (!tmpDirectory.exists()) {
			tmpDirectory.mkdirs()
		}
		return tmpDirectory
	}

	private String getFilename() {
		return FilenameUtils.getName(new URL(getDownloadURL()).getPath())
	}

	File oclintBinDirectory() {
		String filename = getFilename()
	  int endIndex = filename.indexOf("-x86_64")
		String directoryName = "oclint-0.13"
		if (endIndex > 0) {
			directoryName = filename.substring(0, endIndex)
		}
		return new File(getTemporaryDirectory(), directoryName + "/bin")
	}

	@TaskAction
	def run() {
		outputDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("report/oclint")
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}
		download()

		File oclintBinDirectory = oclintBinDirectory()
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
