package org.openbakery.output


import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.testdouble.ProgressLoggerStub
import org.openbakery.testdouble.SimulatorControlStub
import org.openbakery.xcode.Destination
import org.openbakery.xcode.DestinationResolver
import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasItem


class TestBuildOutputAppenderSpecification extends Specification {

	Project project
	List<Destination> destinations
	StyledTextOutputStub output
	ProgressLoggerStub progress


	def setup() {
		File projectDirectory = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDirectory).build()
		project.apply plugin: org.openbakery.XcodePlugin

		SimulatorControlStub simulatorControl = new SimulatorControlStub("simctl-list-xcode10.txt")

		project.xcodebuild.destination {
			name = "iPad Air"
		}
		project.xcodebuild.destination {
			name = "iPhone X"
		}

		DestinationResolver destinationResolver = new DestinationResolver(simulatorControl)
		destinations = destinationResolver.getDestinations(project.xcodebuild.getXcodebuildParameters())

		output = new StyledTextOutputStub()
		progress = new ProgressLoggerStub()
	}


	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	def process(String outputPath) {
		String xcodeOutput = FileUtils.readFileToString(new File(outputPath))
		TestBuildOutputAppender appender = new TestBuildOutputAppender(progress, output, destinations)
		for (String line : xcodeOutput.split("\n")) {
			appender.append(line)
		}
	}


	def "detect unit test start"() {
		when:
		process("src/test/Resource/xcodebuild-output/test-start-xcode-10.txt")

		then:
		assertThat(progress.progress, hasItem("Starting Tests"))
	}




}
