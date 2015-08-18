package org.openbakery.coverage

import org.apache.commons.lang.StringUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin

/**
 * Created by rene on 22.07.14.
 */
class CoverageTask extends DefaultTask {

	CommandRunner commandRunner

	CoverageTask() {
		super()
		dependsOn(XcodePlugin.XCODE_TEST_TASK_NAME)
		this.description = "Runs the gcovr code coverage for the project"
		commandRunner = new CommandRunner()
	}

	@TaskAction
	def coverage() {


		if (!project.coverage.outputDirectory.exists()) {
			project.coverage.outputDirectory.mkdirs();
		}


		String version = "3.2"
		def zipFilename = version + ".zip"
		def zip = new File(project.coverage.outputDirectory, zipFilename)
		def url = 'https://github.com/gcovr/gcovr/archive/' + zipFilename
		ant.get(src: url, dest: project.coverage.outputDirectory, verbose:true)
		ant.unzip(src: zip,  dest:project.coverage.outputDirectory)


		def gcovrCommand = new File(project.coverage.outputDirectory, 'gcovr-' + version + '/scripts/gcovr').absolutePath

		def commandList = [
						'python',
						gcovrCommand,
						'-r',
						'.'
		]

		String exclude = project.coverage.exclude
		if (StringUtils.isNotEmpty(exclude)) {
			commandList.add('-e')
			commandList.add(exclude)
		}

		String outputFilename = "coverage.txt"
		if (StringUtils.isNotEmpty(project.coverage.outputFormat)) {
			commandList.addAll(project.coverage.getOutputParameter())
			outputFilename = "coverage." + project.coverage.outputFormat.toLowerCase()
		}

		commandList.add("-o")
		commandList.add(new File(project.coverage.outputDirectory, outputFilename).absolutePath)

		commandRunner.run(commandList)


	}
}
