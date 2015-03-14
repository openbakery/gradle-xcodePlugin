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

import org.apache.commons.io.FilenameUtils
import org.apache.http.Consts
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractDistributeTask
import org.openbakery.http.HttpUpload

import java.util.regex.Pattern

class HockeyAppUploadTask extends AbstractDistributeTask {


	public static final String HOCKEY_APP_API_URL = "https://rink.hockeyapp.net/api/2/apps/"

	private static final ContentType contentType = ContentType.create("application/x-www-form-urlencoded", Consts.UTF_8)

	File ipaFile;
	File dSYMFile;
	HttpUpload httpUpload = new HttpUpload()


	HockeyAppUploadTask() {
		super()
		this.description = "Uploades the app (.ipa, .dsym) to HockeyApp"

	}


	def prepare() {
		ipaFile =  copyIpaToDirectory(project.hockeyapp.outputDirectory)

		File dSymBundle = getDSymBundle();

		dSYMFile = getDestinationFile(project.hockeyapp.outputDirectory, ".app.dSYM.zip")
		createZip(dSYMFile, dSymBundle.parentFile, dSymBundle);
	}


	@TaskAction
	def upload() throws IOException {

		if (project.hockeyapp.apiToken == null) {
			throw new IllegalArgumentException("Cannot upload to HockeyApp because API Token is missing")
		}

		prepare();

		uploadIPAandDSYM()
		uploadProvisioningProfile()

	}


	void uploadIPAandDSYM() {

		httpUpload.url = HOCKEY_APP_API_URL + project.hockeyapp.appID + "/app_versions/upload"

		def parameters = new HashMap<String, Object>()

		parameters.put("status", project.hockeyapp.status)
		parameters.put("notify", project.hockeyapp.notify)
		parameters.put("notes", project.hockeyapp.notes)
		parameters.put("notes_type", project.hockeyapp.notesType)
		parameters.put("mandatory", project.hockeyapp.mandatory)
		parameters.put("private", project.hockeyapp.privatePage)
		if (project.hockeyapp.teams != null) {
			parameters.put("teams", project.hockeyapp.teams.join(","))
		}
		if (project.hockeyapp.tags != null) {
			parameters.put("tags", project.hockeyapp.tags.join(","))
		}
		if (project.hockeyapp.users != null) {
			parameters.put("users", project.hockeyapp.users.join(","))
		}
		if (project.hockeyapp.releaseType != null) {
			parameters.put("release_type", project.hockeyapp.releaseType)
		}
		if (project.hockeyapp.commitSha != null) {
			parameters.put("commit_sha", project.hockeyapp.commitSha)
		}
		if (project.hockeyapp.buildServerUrl != null) {
			parameters.put("build_server_url", project.hockeyapp.buildServerUrl)
		}
		if (project.hockeyapp.repositoryUrl != null) {
			parameters.put("repository_url", project.hockeyapp.repositoryUrl)
		}


		parameters.put("ipa", ipaFile)
		parameters.put("dsym", dSYMFile)

		httpUpload.postRequest(getHttpHeaders(), parameters)

	}

	def void uploadProvisioningProfile() {

		httpUpload.url = HOCKEY_APP_API_URL + project.hockeyapp.appID + "/provisioning_profiles"

		if (project.xcodebuild.signing.mobileProvisionFile.size() != 1) {
			logger.debug("mobileProvisionFile not found");
			return;
		}

		if (project.hockeyapp.releaseType == '1') {
			logger.debug("releaseType is appstore so do not upload the provisioning profile");
			return;
		}

		httpUpload.postRequest(getHttpHeaders(),
			["mobileprovision": project.xcodebuild.signing.mobileProvisionFile.get(0)]
		)

	}


	def getHttpHeaders() {
		return ["X-HockeyAppToken": project.hockeyapp.apiToken ]
	}

}
