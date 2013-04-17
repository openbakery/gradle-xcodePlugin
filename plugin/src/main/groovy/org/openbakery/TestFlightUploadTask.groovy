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
package org.openbakery

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
import java.util.regex.Pattern
import org.apache.http.util.EntityUtils
import org.apache.http.HttpHost
import org.apache.http.conn.params.ConnRoutePNames

class TestFlightUploadTask extends DefaultTask {

	TestFlightUploadTask() {
		super()
		dependsOn("testflight-prepare")
		this.description = "Distributes the build to TestFlight"
	}

	def getFile(String extension) {
		def buildOutputDirectory = project.testflight.outputDirectory
		def pattern = Pattern.compile(".*" + extension)
		def fileList = buildOutputDirectory.list(
						[accept: {d, f -> f ==~ pattern }] as FilenameFilter
		).toList()
		if (fileList == null || fileList.size() == 0) {
			throw new IllegalStateException("No *" + extension + " file found in directory " + buildOutputDirectory.absolutePath)
		}
		return new File(buildOutputDirectory, fileList[0])
	}

	@TaskAction
	def upload() throws IOException {

		if (project.testflight.apiToken == null) {
			throw new IllegalArgumentException("Cannot upload to TestFlight because API Token is missing")
		}

		if (project.testflight.teamToken == null) {
			throw new IllegalArgumentException("Cannot upload to TestFlight because Team Token is missing")
		}


		HttpClient httpClient = new DefaultHttpClient()

		// for testing only
		//HttpHost proxy = new HttpHost("localhost", 8888);
		//httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

		HttpPost httpPost = new HttpPost("http://testflightapp.com/api/builds.json")

		/*
		api_token - Required (Get your API token)
		team_token - Required, token for the team being uploaded to. (Get your team token)
		file - Required, file data for the build
		notes - Required, release notes for the build
		dsym - Optional, the zipped .dSYM corresponding to the build
		distribution_lists - Optional, comma separated distribution list names which will receive access to the build
		notify - Optional, notify permitted teammates to install the build (defaults to False)
		replace - Optional, replace binary for an existing build if one is found with the same name/bundle version (defaults to False)

*/

		def ipaFile = getFile("ipa");
		def dSYMFile = getFile("dSYM.zip");

		println ipaFile.absolutePath
		println dSYMFile.absolutePath

		MultipartEntity entity = new MultipartEntity();


		println "api_token " + project.testflight.apiToken
		println "team_token " + project.testflight.teamToken
		println "distribution_lists" + project.testflight.distributionLists
		println "notes " + project.testflight.notes
		println "file " + ipaFile
		println "dsym " + dSYMFile


		entity.addPart("api_token", new StringBody(project.testflight.apiToken))
		entity.addPart("team_token", new StringBody(project.testflight.teamToken))
		entity.addPart("notes", new StringBody(project.testflight.notes))
		if (project.testflight.distributionLists != null){
			entity.addPart("distribution_lists", new StringBody(project.testflight.distributionLists))
			entity.addPart("notify", new StringBody("True"))
		}
		entity.addPart("file", new FileBody(ipaFile))
		entity.addPart("dsym", new FileBody(dSYMFile))

		httpPost.setEntity(entity);

		HttpResponse response = httpClient.execute(httpPost)
		HttpEntity responseEntity = response.getEntity()
		def entityString = EntityUtils.toString(responseEntity)
		println "response " + entityString
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new IllegalStateException("upload failed: " + response.getStatusLine().getReasonPhrase());
		}

	}

}
