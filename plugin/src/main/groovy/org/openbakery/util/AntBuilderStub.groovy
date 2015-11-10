package org.openbakery.util

import org.gradle.api.Transformer

/**
 * Created by rene on 10.11.15.
 */
class AntBuilderStub extends org.gradle.api.AntBuilder {

	def commands = [:]


	@Override
	Map<String, Object> getProperties() {
		return null
	}

	@Override
	Map<String, Object> getReferences() {
		return null
	}

	@Override
	void importBuild(Object antBuildFile) {

	}

	@Override
	void importBuild(Object antBuildFile, Transformer<? extends String, ? super String> taskNamer) {

	}

	def get(def map) {
		commands["get"] = map
	}

	def unzip(def map) {
		commands["unzip"] = map
	}
}
