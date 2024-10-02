package org.openbakery

import org.apache.commons.collections.buffer.CircularFifoBuffer
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.output.NullWriter
import org.openbakery.output.OutputAppender
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CommandRunner {

	BaseCommandRunner baseCommandRunner = new BaseCommandRunner()

	private static Logger logger = LoggerFactory.getLogger(CommandRunner.class)

	Collection<String> commandOutputBuffer = null;

	private File outputFile = null
	String defaultBaseDirectory = "."

	Thread readerThread

	public CommandRunner() {

	}


	void setOutputFile(File outputFile) {
		if (outputFile.exists()) {

			String outputFileAsString = outputFile.getAbsolutePath()
			String basename = FilenameUtils.getBaseName(outputFileAsString)
			String extension = "." + FilenameUtils.getExtension(outputFileAsString)
			String path = FilenameUtils.getFullPath(outputFileAsString)

			File movedOutputFile = File.createTempFile(basename, extension, new File(path))
			FileUtils.copyFile(outputFile, movedOutputFile)
			outputFile.delete()
			logger.debug("moved existing file '{}' to '{}", outputFile, movedOutputFile)
		}
		this.outputFile = outputFile
	}


	private def commandListToString(List<String> commandList) {
		return commandList.join(" ")
	}

	def run(String directory, List<String> commandList, Map<String, String> environment, OutputAppender outputAppender) {

		logger.info("Run command: {}", commandListToString(commandList))
		logger.debug("with working directory: {} ({})", directory, new File(directory).absoluteFile);
		if (environment != null) {
			logger.debug("with additional environment variables: {}", environment)
		}

		if (commandOutputBuffer == null) {
			commandOutputBuffer = new CircularFifoBuffer(40);
		}

		def commandsAsStrings = commandList.collect { it.toString() } // GStrings don't play well with ProcessBuilder
		def processBuilder = new ProcessBuilder(commandsAsStrings)
		processBuilder.redirectErrorStream(true)
		processBuilder.directory(new File(directory))
		if (environment != null) {
			Map<String, String> env = processBuilder.environment()
			env.putAll(environment)
		}
		def process = processBuilder.start()


		processInputStream(process.inputStream, outputAppender)


		process.waitFor()
		readerThread.join()
		if (process.exitValue() > 0) {
			logger.debug("Exit Code: {}", process.exitValue())
			def lastLines = commandOutputBuffer.toArray().join("\n")
			throw new CommandRunnerException("Command failed to run (exit code " + process.exitValue() + "): " + commandListToString(commandList) + "\nTail of output:\n" + lastLines)
		}

	}

	void processInputStream(InputStream inputStream, OutputAppender outputAppender) {

		Runnable runnable = new Runnable() {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			Writer writer = null


			public void run() {
				if (outputFile != null) {
					writer = new FileWriter(outputFile)
				} else {
					writer = new NullWriter()
				}

				boolean running = true;
				while (running) {
					String line = reader.readLine();
					if (line == null) {
						running = false;
					} else {

						if (outputAppender) {

							// remove ansi colors:
							outputAppender.append(line.replaceAll(/\u001B\[[\d]*m/, ""))
						}

						commandOutputBuffer.add(line)

						writer.write(line)
						writer.write("\n")
					}
				}
				reader.close()
				writer.close()
			}
		};

		readerThread = new Thread(runnable)
		readerThread.start()
	}

	def run(String directory, List<String> commandList, OutputAppender outputAppender) {
		run(directory, commandList, null, outputAppender)
	}

	def run(String directory, List<String> commandList) {
		run(directory, commandList, null, null)
	}

	def run(List<String> commandList, OutputAppender outputAppender) {
		run(defaultBaseDirectory, commandList, null, outputAppender)
	}

	def run(List<String> commandList) {
		run(defaultBaseDirectory, commandList, null, null)
	}

	def run(String... commandList) {
		run(Arrays.asList(commandList));
	}

	def run(List<String> commandList, Map<String, String> environment) {
		run(defaultBaseDirectory, commandList, environment, null)
	}

	def run(List<String> commandList, Map<String, String> environment, OutputAppender outputAppender) {
		run(defaultBaseDirectory, commandList, environment, outputAppender)
	}

	String runWithResult(String... commandList) {
		return runWithResult(Arrays.asList(commandList))
	}

	String runWithResult(Map<String, String> environmentValues,
						 String... commandList) {
		return runWithResult(defaultBaseDirectory,
				commandList.toList(),
				environmentValues,
				null)
	}

	String runWithResult(List<String> commandList) {
		return runWithResult(defaultBaseDirectory, commandList)
	}

	String runWithResult(String directory,
						 List<String> commandList) {
		return runWithResult(directory,
				commandList,
				null,
				null)
	}

	String runWithResult(String directory,
						 List<String> commandList,
						 Map<String, String> environment,
						 OutputAppender outputAppender) {
		commandOutputBuffer = new ArrayList<>();
		run(directory, commandList, environment, outputAppender)
		String result = commandOutputBuffer.join("\n")
		commandOutputBuffer = null;
		return result
	}


}
