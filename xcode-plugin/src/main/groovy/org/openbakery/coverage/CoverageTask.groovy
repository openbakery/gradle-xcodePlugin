package org.openbakery.coverage

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask
import org.openbakery.xcode.Destination

class CoverageTask extends AbstractXcodeTask {

	@Internal
	Report report = new Report()

	@InputFile
	@Optional
	File binary

	@InputFile
	@Optional
	File profileData

	@Input
	@Optional
	String include

	@Input
	@Optional
	String exclude

	@Input
	@Optional
	String type

	CoverageTask() {
		super()
		//dependsOn(XcodePlugin.XCODE_TEST_TASK_NAME)
		this.description = "Runs the gcovr code coverage for the project"
	}

	@TaskAction
	def coverage() {

		if (!project.coverage.outputDirectory.exists()) {
			project.coverage.outputDirectory.mkdirs();
		}


		if (getProfileData() != null) {
			report.profileData = getProfileData()
			report.binary = getBinary()
			report.include = getInclude()
			report.exclude = getExclude()
			report.type = getReportType()
			report.title = FilenameUtils.getBaseName(project.xcodebuild.projectFile.path)
			report.destinationPath = project.coverage.outputDirectory
			logger.debug("Report to create with data: {}", report)
			report.create()
			return
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

	void setProfileData(def data) {
		if (data instanceof File) {
			this.profileData = data
		} else {
			this.profileData = new File(data)
		}
	}

	File getProfileData() {
		if (this.profileData != null) {
			return this.profileData
		}

		def possibleDirectories = [
				"Build/Intermediates/CodeCoverage/" + project.xcodebuild.target + "/Coverage.profdata",
				"Build/Intermediates/CodeCoverage/Coverage.profdata"
		]

		for (Destination destination : project.coverage.testResultDestinations) {
			possibleDirectories.add("Build/ProfileData/" + destination.id + "/Coverage.profdata")
		}

		for (String directory : possibleDirectories) {
			this.profileData = new File(project.xcodebuild.derivedDataPath, directory)
			if (this.profileData.exists()) {
				return this.profileData
			}

		}
		return null
	}

	void setBinary(def data) {
		if (data instanceof File) {
			this.binary = data
		} else {
			this.binary = new File(data)
		}
	}

	File getBinary() {
		if (this.binary != null) {
			return this.binary
		}
		this.binary = project.xcodebuild.binary
		return this.binary
	}

	String getInclude() {
		if (this.include != null) {
			return this.include
		}
		return project.coverage.include
	}

	String getExclude() {
		if (this.exclude != null) {
			return this.exclude
		}
		return project.coverage.exclude
	}


	@Internal
	Report.Type getReportType() {
		if (this.type != null) {
			return Report.Type.typeFromString(this.type)
		}
		return Report.Type.typeFromString(project.coverage.outputFormat)
	}
}
