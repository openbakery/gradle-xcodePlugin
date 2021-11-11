package org.openbakery.output

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.testdouble.ProgressLoggerStub
import org.openbakery.xcode.Destination
import org.openbakery.xcode.DestinationResolver
import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.startsWith

class XcodeBuildOutputAppenderSpecification extends Specification {

	Project project
	List<Destination> destinations
	StyledTextOutputStub output
	ProgressLoggerStub progress


	def setup() {
		File projectDirectory = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDirectory).build()
		project.apply plugin: org.openbakery.XcodePlugin

		output = new StyledTextOutputStub()
		progress = new ProgressLoggerStub()
	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	def process(String outputPath) {
		String xcodeOutput = FileUtils.readFileToString(new File(outputPath))
		XcodeBuildOutputAppender appender = new XcodeBuildOutputAppender(progress, output)
		for (String line : xcodeOutput.split("\n")) {
			appender.append(line)
		}
	}


	def "detect codesign"() {
		when:
		process("src/test/Resource/xcodebuild-output/codesign-success.txt")

		then:
		progress.progress.contains("CodeSign /Users/user/workspace/build/sym/Debug-iphonesimulator/Project-FileProviderExtension.appex")
	}



	def "detect swift compile"() {
		when:
		process("src/test/Resource/xcodebuild-output/swift-compile.txt")

		then:
		assertThat(progress.progress, hasItem(startsWith("Compile")))
	}


	def "detect swift compile with multiple files"() {
		when:
		process("src/test/Resource/xcodebuild-output/swift-compile.txt")

		then:
		progress.progress.contains("Compile <multiple files>")
	}

	def "detect swift compile error"() {
		when:
		process("src/test/Resource/xcodebuild-output/swift-compile.txt")

		then:
		output.toString().contains("/Users/me/workspace/Project/Test/CellModelTest.swift:16:10")
	}


	def "detect swift compile error shows full error"() {
		when:
		process("src/test/Resource/xcodebuild-output/swift-compile.txt")

		then:
		output.toString().contains("\n                return UIDocumentInteractionControllerFake(url: url)")
	}

	def "detect swift compile error shows full error, finished in \n"() {
		when:
		process("src/test/Resource/xcodebuild-output/swift-compile.txt")

		then:
		output.toString().contains("\n                return UIDocumentInteractionControllerFake(url: url)")
		!output.toString().contains("cd /Users/me/workspace/")
	}



	def "detect swift compile warning"() {
		when:
		process("src/test/Resource/xcodebuild-output/swift-compile.txt")

		then:
		output.toString().contains("/Users/me/workspace/Project/Test/CellModelTest.swift:10:18: warning: ")
	}


	def "detect swift compile warning shows full error, finished in \n"() {
		when:
		process("src/test/Resource/xcodebuild-output/swift-compile.txt")

		then:
		output.toString().contains("@testable import Project")
		!output.toString().contains("cd /Users/me/workspace/")
	}


}
