/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbakery.hockeyapp

import org.gradle.api.Project


class HockeyAppPluginExtension {
	def Object outputDirectory
	def String apiToken = null
	def String appID = null
	def String notes = "This build was uploaded using the gradle xcodePlugin"
	def String status = 2
	def String notify = 1
	def String notesType = 0
	def String[] tags = null
	def String[] teams = null
	def String[] users = null
	def String releaseType = null
	def String mandatory = 0
	def String privatePage = false
	def String commitSha = null
	def String buildServerUrl = null
	def String repositoryUrl = null

	private final Project project

	public HockeyAppPluginExtension(Project project) {
		this.project = project
		this.outputDirectory = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("hockeyapp")
		}
	}

	File getOutputDirectory() {
		return project.file(outputDirectory)
	}

	void setOutputDirectory(Object outputDirectory) {
		this.outputDirectory = outputDirectory
	}

}
