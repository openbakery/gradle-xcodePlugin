package org.openbakery.coverage

import org.apache.commons.lang.StringUtils
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask

/**
 * Created by rene on 22.07.14.
 */
class CoverageTask extends AbstractXcodeTask {

	CoverageTask() {
		super()
		dependsOn('test')
		this.description = "Runs the gcovr code coverage for the project"
	}

	@TaskAction
	def coverage() {


		if (!project.coverage.outputDirectory.exists()) {
			project.coverage.outputDirectory.mkdirs();
		}


		def ant = new groovy.util.AntBuilder()

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
		String outputParameter = project.coverage.getOutputParameter()
		if (StringUtils.isNotEmpty(outputParameter)) {
			commandList.add(outputParameter);
			outputFilename = "coverage." + project.coverage.outputFormat.toLowerCase()

		}

		println(commandList)

		commandRunner.setOutputFile(new File(project.coverage.outputDirectory, outputFilename));
		commandRunner.run(commandList)


	}
}
