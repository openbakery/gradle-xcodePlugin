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

import java.util.regex.Pattern

class HockeyAppUploadTask extends AbstractDistributeTask {


	public static final String HOCKEY_APP_API_URL = "https://rink.hockeyapp.net/api/2/apps/"

	private static final ContentType contentType = ContentType.create("application/x-www-form-urlencoded", Consts.UTF_8)

	HockeyAppUploadTask() {
		super()
		dependsOn("hockeyapp-prepare")
		this.description = "Uploades the app (.ipa, .dsym) to HockeyApp"
	}



	@TaskAction
	def upload() throws IOException {

		if (project.hockeyapp.apiToken == null) {
			throw new IllegalArgumentException("Cannot upload to HockeyApp because API Token is missing")
		}

		def ipaFile = getIpaFile(project.hockeyapp.outputDirectory)
		def dSYMFile = getDsymZipFile(project.hockeyapp.outputDirectory)

		logger.debug("ipaFile: {}", ipaFile.absolutePath)
		logger.debug("dSYMFile: {}",  dSYMFile.absolutePath)
		logger.debug("api_token: {}", project.hockeyapp.apiToken)
		logger.debug("notes: {} ", project.hockeyapp.notes)
		logger.debug("file: {} ", ipaFile)
		logger.debug("dsym: {} ", dSYMFile)
		logger.debug("status: {} ", project.hockeyapp.status)
		logger.debug("notify: {} ", project.hockeyapp.notify)
		logger.debug("notes_type: {} ", project.hockeyapp.notesType)


		uploadIPAandDSYM(ipaFile, dSYMFile)
		uploadProvisioningProfile()

	}

	def void uploadIPAandDSYM(File ipaFile, File dSYMFile) {

		CloseableHttpClient httpClient = HttpClients.createDefault();

		try {
			HttpPost httpPost = new HttpPost(HOCKEY_APP_API_URL + project.hockeyapp.appID + "/app_versions/upload");

			HttpEntity requestEntity = MultipartEntityBuilder.create()
							.addPart("status", new StringBody(project.hockeyapp.status, contentType))
							.addPart("notify",  new StringBody(project.hockeyapp.notify, contentType))
							.addPart("notes",  new StringBody(project.hockeyapp.notes, contentType))
							.addPart("notes_type",  new StringBody(project.hockeyapp.notesType, contentType))
							.addBinaryBody("ipa", ipaFile)
							.addBinaryBody("dsym", dSYMFile)
							.build()

			httpPost.setEntity(requestEntity);

			executePost(httpClient, httpPost)

		} finally {
			httpClient.close();
		}

	}

	private void executePost(CloseableHttpClient httpClient, HttpPost httpPost) {
		/*
					HttpHost proxy = new HttpHost("localhost", 8888);
					RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
					httpPost.setConfig(config);
		*/

		httpPost.addHeader("X-HockeyAppToken", project.hockeyapp.apiToken)

		CloseableHttpResponse response = httpClient.execute(httpPost);
		try {
			logger.debug("{}", response.getStatusLine());
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				logger.debug("Response content length: {}", entity.getContentLength());
			}
			String responseString = EntityUtils.toString(entity, "UTF-8");
			logger.debug("{}", responseString);
			EntityUtils.consume(entity);

			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode >= 400) {
				throw new IllegalStateException("file upload failed: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase() + ": " + responseString);
			}

		} finally {
			response.close();
		}
	}

	def void uploadProvisioningProfile() {

		if (project.xcodebuild.signing.mobileProvisionFile.size() != 1) {
			return;
		}

		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpPost httpPost = new HttpPost(HOCKEY_APP_API_URL + project.hockeyapp.appID + "/provisioning_profiles");

			HttpEntity requestEntity = MultipartEntityBuilder.create()
							.addBinaryBody("mobileprovision", project.xcodebuild.signing.mobileProvisionFile.get(0))
							.build()

			httpPost.setEntity(requestEntity);
			executePost(httpClient, httpPost)

		} finally {
			httpClient.close();
		}

	}

}
