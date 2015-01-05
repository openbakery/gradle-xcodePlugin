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

import org.gradle.api.DefaultTask
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.HttpResponse
import org.apache.http.HttpEntity
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractDistributeTask

import java.util.regex.Pattern
import org.apache.http.util.EntityUtils
import org.apache.http.HttpHost
import org.apache.http.conn.params.ConnRoutePNames

class DeployGateUploadTask extends AbstractDistributeTask {

	File ipaFile;

	DeployGateUploadTask() {
		super()
		dependsOn("package")
		this.description = "Distributes the build to DeployGate"
	}


	def prepare() {
		ipaFile =  copyIpaToDirectory(project.deploygate.outputDirectory)
	}

	@TaskAction
	def upload() throws IOException {

		if (project.deploygate.apiToken == null) {
			throw new IllegalArgumentException("Cannot upload to DeployGate because API Token is missing")
		}

		if (project.deploygate.userName == null) {
			throw new IllegalArgumentException("Cannot upload to DeployGate because User Name is missing")
		}


		HttpClient httpClient = new DefaultHttpClient()

		// for testing only
		//HttpHost proxy = new HttpHost("localhost", 8888);
		//httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

		HttpPost httpPost = new HttpPost("https://deploygate.com/api/users/" + project.deploygate.userName + "/apps")

		/*
		apiToken - Required (Get your API token)
		userName - Required, user name
		file - Required, file data for the build
		message - Optional, release notes for the build
*/

		logger.debug("ipaFile: {}", ipaFile.absolutePath)

		MultipartEntity entity = new MultipartEntity();


		logger.debug("token {}", project.deploygate.apiToken)
		logger.debug("user name {}", project.deploygate.userName)
		logger.debug("message {}", project.deploygate.message)
		logger.debug("file {}", ipaFile)

		entity.addPart("token", new StringBody(project.deploygate.apiToken))
		entity.addPart("message", new StringBody(project.deploygate.message))
		entity.addPart("file", new FileBody(ipaFile))

		httpPost.setEntity(entity);

		HttpResponse response = httpClient.execute(httpPost)
		HttpEntity responseEntity = response.getEntity()
		def entityString = EntityUtils.toString(responseEntity)
		logger.debug("response {}", entityString)
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new IllegalStateException("upload failed: " + response.getStatusLine().getReasonPhrase());
		}

	}

}
