package org.openbakery

import org.apache.commons.collections.buffer.CircularFifoBuffer
import org.apache.commons.io.FileUtils
import spock.lang.Specification

class CommandRunnerSpecification extends Specification {

	CommandRunner commandRunner

	File outputDirectory

	void setup() {
		commandRunner = new CommandRunner()
		File temporaryDirectory = new File(System.getProperty("java.io.tmpdir"))

		outputDirectory = new File(temporaryDirectory, "gradle-xcodebuild")
		outputDirectory.mkdirs()
	}

	def cleanup() {
		FileUtils.deleteDirectory(outputDirectory)
	}


	def "Run With Error"() {
		given:
		def cpErrorOutput = "Command failed to run (exit code 64): cp\n"

		when:
		commandRunner.run(["cp"])

		then:
		def exception = thrown(CommandRunnerException.class)
		exception.message.toString().startsWith(cpErrorOutput)

	}

	def "Run With Error on line"() {
		given:
		def cpErrorOutput = "Command failed to run (exit code 64): cp\n"

		when:
		commandRunner.commandOutputBuffer = new CircularFifoBuffer(1)
		commandRunner.run(["cp"])

		then:
		def exception = thrown(CommandRunnerException.class)
		exception.message.toString().startsWith(cpErrorOutput)

	}


	def "Run with long result after normal run"() {
		when:
		commandRunner.commandOutputBuffer = new CircularFifoBuffer(1)
		commandRunner.run("ls")
		def result = commandRunner.runWithResult(["find", "/usr/bin"])

		then:
		result.split("\n").length > 20
		result.contains("/usr/bin/find")

	}


	def "Run with result and than normal run"() {
		when:
		commandRunner.commandOutputBuffer = new CircularFifoBuffer(1)
		commandRunner.runWithResult(["find", "/usr/bin"])
		commandRunner.run(["find", "/usr/bin"])

		then:
		commandRunner.commandOutputBuffer.size() == 40

	}


	def "set outputFile"() {
		given:
		File output = new File(outputDirectory, "test.log")

		when:
		commandRunner.outputFile = output

		then:
		commandRunner.outputFile == output
	}

	def "exist"() {
		given:
		File output = new File(outputDirectory, "test.log")

		when:
		output.createNewFile()

		then:
		output.exists()
	}


	def "set outputFile using existing file"() {
		given:
		File output = new File(outputDirectory, "test.log")
		output.write("foo")


		when:
		commandRunner.outputFile = output

		then:
		commandRunner.outputFile == output
		outputDirectory.listFiles().size() == 1
		outputDirectory.listFiles()[0].name.startsWith("test-")
		outputDirectory.listFiles()[0].name.endsWith(".log")
		!output.exists()
	}


	def "set outputFile is deleted when it cannot be renamed"() {
		given:
		File output = new File(outputDirectory, "test")
		output.write("foo")


		when:
		commandRunner.outputFile = output

		then:
		!output.exists()
	}
}
