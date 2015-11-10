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
	def unzip = []

	public get(def parameters) {
		get << parameters
	}

	public gunzip(def parameters) {
		gunzip << parameters
	}

	public untar(def parameters) {
		untar << parameters
	}

	public unzip(def parameters) {
		unzip << parameters
	}

	Map<String, Object> getProperties() {
		return null
	}

	Map<String, Object> getReferences() {
		return null
	}

	void importBuild(Object o) {

	}

	void importBuild(Object o, Transformer<? extends String, ? super String> transformer) {

	}
}
