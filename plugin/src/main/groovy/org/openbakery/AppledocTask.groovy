package org.openbakery

import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.TaskAction

/**
 * Created by rene on 21.07.14.
 */
class AppledocTask extends AbstractXcodeTask {

	AppledocTask() {
		super()
		this.description = "Runs the appledoc for the given project"
	}

	@TaskAction
	def documentation() {

		def appledocSettings = new File('AppledocSettings.plist')
		if (!appledocSettings.exists()) {
			throw new InvalidUserDataException("The AppledocSettings.plist is missing.")
		}


		def documentationDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("documentation")
		if (!documentationDirectory.exists()) {
			documentationDirectory.mkdirs()
		}

		def appledocCommand = new File(documentationDirectory, 'appledoc')

		def ant = new groovy.util.AntBuilder()

		def zip = new File(documentationDirectory, 'appledoc.zip')
		ant.get(src: 'https://github.com/tomaz/appledoc/releases/download/v2.2-963/appledoc.zip', dest: documentationDirectory, verbose:true)
		ant.unzip(src: zip,  dest:documentationDirectory)
		ant.chmod(file: appledocCommand, perm:"+x")

		try {
			commandRunner.run([appledocCommand.absolutePath, "--print-settings", "--output", documentationDirectory.absolutePath, '--ignore', project.getBuildDir().absolutePath, "."])
		} catch (CommandRunnerException ex) {
			logger.quiet(commandRunner.getResult())
			throw ex
		}

	}


}
