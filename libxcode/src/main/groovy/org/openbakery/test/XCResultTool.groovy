package org.openbakery.test

import groovy.json.JsonSlurper
import org.openbakery.CommandRunner
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class XCResultTool {
	private static Logger logger = LoggerFactory.getLogger(XCResultTool.class)

	private String path;

	public XCResultTool(String path) {
		this.path = path
	}

	Map<String, Object> getObject(File file, String id = null) {
		logger.info("load result file {}", file)
		def runner = new CommandRunner()
		def commandList = [path, "get",
											 "--format", "json",
											 "--path", file.absolutePath]

		if(id) {
			commandList.addAll(["--id", id])
		}

		def result = runner.runWithResult(commandList)


		def json = new JsonSlurper()
		def object = json.parseText(result)
		return object
	}

	void exportAttachment(File from, File to, String id, String name) {
		def runner = new CommandRunner()
		runner.run(path, "export",
			"--path", from.absolutePath,
			"--id", id,
			"--output-path", "${to.absolutePath}/${name}",
			"--type", "file"
		)
	}
}
