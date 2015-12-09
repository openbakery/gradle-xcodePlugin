package org.openbakery

import org.apache.commons.collections.buffer.CircularFifoBuffer
import spock.lang.Specification

/**
 * Created by rene on 24.07.14.
 */
class CommandRunnerSpecification extends Specification {

	CommandRunner commandRunner;


	void setup() {
		commandRunner = new CommandRunner()
	}


	def "Run With Result"() {
		when:
		String result = commandRunner.runWithResult(["echo", "test"])

		then:
		result == "test"

	}


	def "Run With Error"() {
		given:
		def cpErrorOutput = "Command failed to run (exit code 64): 'cp'\n" +
				"Tail of output:\n" +
				"usage: cp [-R [-H | -L | -P]] [-fi | -n] [-apvX] source_file target_file\n" +
				"       cp [-R [-H | -L | -P]] [-fi | -n] [-apvX] source_file ... target_directory"

		when:
		commandRunner.run(["cp"])


		then:
		def exception = thrown(CommandRunnerException.class)
		exception.message == cpErrorOutput

	}

	def "Run With Error on line"() {
		given:
		def cpErrorOutput = "Command failed to run (exit code 64): 'cp'\n" +
				"Tail of output:\n" +
				"       cp [-R [-H | -L | -P]] [-fi | -n] [-apvX] source_file ... target_directory"

		when:
		commandRunner.commandOutputBuffer = new CircularFifoBuffer(1)
		commandRunner.run(["cp"])

		then:
		def exception = thrown(CommandRunnerException.class)
		exception.message == cpErrorOutput

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
		commandRunner.commandOutputBuffer.size() == 20

	}


}
