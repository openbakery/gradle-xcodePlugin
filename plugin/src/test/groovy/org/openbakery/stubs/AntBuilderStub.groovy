package org.openbakery.stubs

import org.gradle.api.AntBuilder
import org.gradle.api.Transformer

/**
 * Created by rene on 22.07.15.
 */
class AntBuilderStub extends AntBuilder {

	def get = []
	def gunzip = []
	def untar = []

	public get(def parameters) {
		get << parameters
	}

	public gunzip(def parameters) {
		gunzip << parameters
	}

	public untar(def parameters) {
		untar << parameters
	}


	@Override
	Map<String, Object> getProperties() {
		return null
	}

	@Override
	Map<String, Object> getReferences() {
		return null
	}

	@Override
	void importBuild(Object o) {

	}

	@Override
	void importBuild(Object o, Transformer<? extends String, ? super String> transformer) {

	}
}
