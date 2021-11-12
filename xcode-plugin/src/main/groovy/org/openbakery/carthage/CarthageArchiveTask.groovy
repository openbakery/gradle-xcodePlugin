package org.openbakery.carthage

import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutputFactory

class CarthageArchiveTask extends AbstractCarthageTaskBase {

	CarthageArchiveTask() {
		super()
		setDescription "Create a binary framework that can be used a Carthage project dependencies"
		this.setOnlyIf {
			hasCartfile()
		}
		serializeDebugging = true
		println "HERE!!!!"
		println "parameters: " + parameters
		parameters.cache = true
		println "parameters: " + parameters
	}


	@TaskAction
	void archive() {
		def output = services.get(StyledTextOutputFactory).create(CarthageUpdateTask)
		run([ACTION_BUILD, ARGUMENT_ARCHIVE], output)
	}

}
