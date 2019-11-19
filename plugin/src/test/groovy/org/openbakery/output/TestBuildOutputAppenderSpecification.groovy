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


	def processFile(String outputPath) {
		String xcodeOutput = FileUtils.readFileToString(new File(outputPath))
		processString(xcodeOutput)
	}

	def processString(String xcodeOutput) {
		TestBuildOutputAppender appender = new TestBuildOutputAppender(progress, output, destinations)
		for (String line : xcodeOutput.split("\n")) {
			appender.append(line)
		}
	}


	def "detect unit test start"() {
		when:
		processFile("src/test/Resource/xcodebuild-output/test-start-xcode-10.txt")

		then:
		assertThat(progress.progress, hasItem("Starting Tests"))
	}


	def "Xcode 11 text output progress is shown"() {
		given:
		def output = '''
Test suite 'EditTextTableViewCellTest' started on 'iPhone 8\'
Test case '-[EditTextTableViewCellTest test_clear_button_is_shown]' passed on 'iPhone 8' (0.011 seconds)
Test case '-[EditTextTableViewCellTest test_keyLabel]' passed on 'iPhone 8' (0.008 seconds)
Test case '-[EditTextTableViewCellTest test_keyLabel_Layout]' passed on 'iPhone 8' (0.005 seconds)
Test case '-[EditTextTableViewCellTest test_reuse_resets_the_delegate]' passed on 'iPhone 8' (0.006 seconds)
Test case '-[EditTextTableViewCellTest test_set_editiable_enables_textField]' passed on 'iPhone 8' (0.005 seconds)
Test case '-[EditTextTableViewCellTest test_set_not_editiable_disables_textField]' passed on 'iPhone 8' (0.005 seconds)
Test case '-[EditTextTableViewCellTest test_textField]' passed on 'iPhone 8' (0.005 seconds)
Test case '-[EditTextTableViewCellTest test_textFieldLayout]' passed on 'iPhone 8' (0.007 seconds)
'''

		when:
		processString(output)

		then:
		assertThat(progress.progress, hasItem('0 tests completed, running \'EditTextTableViewCellTest\''))
		assertThat(progress.progress, hasItem('1 tests completed, running \'EditTextTableViewCellTest\''))


	}


	def "showProgress appends current finished test case"() {
		given:
		def appender = new TestBuildOutputAppender(progress, output, destinations)
		appender.fullProgress = true

		when:
		appender.append("Test case '-[EditTextTableViewCellTest test_keyLabel_Layout]' passed on 'iPhone 8' (0.005 seconds)")

		then:
		appender.output.toString() == "      OK -[EditTextTableViewCellTest test_keyLabel_Layout] - (0.005 seconds)\n"
	}



}
