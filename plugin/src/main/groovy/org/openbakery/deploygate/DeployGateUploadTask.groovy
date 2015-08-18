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
package org.openbakery.deploygate


import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractDistributeTask
import org.openbakery.http.HttpUpload


class DeployGateUploadTask extends AbstractDistributeTask {

	File ipaFile
	HttpUpload httpUpload = new HttpUpload()


	DeployGateUploadTask() {
		super()
		dependsOn()
		this.description = "Distributes the build to DeployGate"
	}


	def prepare() {
		ipaFile =  copyIpaToDirectory(project.deploygate.outputDirectory)
	}

	void executeTask() {

		if (project.deploygate.apiToken == null) {
			throw new IllegalArgumentException("Cannot upload to DeployGate because API Token is missing")
		}

		if (project.deploygate.userName == null) {
			throw new IllegalArgumentException("Cannot upload to DeployGate because User Name is missing")
		}

		prepare()


		def parameters = new HashMap<String, Object>()

		parameters.put("token", project.deploygate.apiToken)
		parameters.put("message", project.deploygate.message)
		parameters.put("file", ipaFile)

		httpUpload.url = "https://deploygate.com/api/users/" + project.deploygate.userName + "/apps"

		httpUpload.postRequest(parameters)

	}

}
