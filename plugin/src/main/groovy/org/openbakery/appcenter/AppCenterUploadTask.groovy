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
package org.openbakery.appcenter

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractHttpDistributeTask
import org.openbakery.CommandRunner
import org.openbakery.appcenter.models.*
import org.openbakery.http.HttpUtil
import org.openbakery.util.ZipArchive

class AppCenterUploadTask extends AbstractHttpDistributeTask {

	private static final String APP_CENTER_URL = "https://api.appcenter.ms"
	private static final String PATH_BASE_API = "v0.1/apps"
	private static final String PATH_RELEASE_UPLOAD = "release_uploads"
	private static final String PATH_SYMBOL_UPLOAD = "symbol_uploads"
	private static final String HEADER_CONTENT_TYPE = "Content-Type"
	private static final String HEADER_BLOB_TYPE = "x-ms-blob-type"
	private static final String HEADER_ACCEPT = "Accept"
	private static final String HEADER_TOKEN = "X-API-Token"
	private static final String MEDIA_TYPE_JSON = "application/json"
	private static final String MEDIA_TYPE_MULTI_FORM = "multipart/form-data"
	private static final String BLOB_TYPE_BLOCK_BLOB = "BlockBlob"
	private static final String PART_KEY_IPA = "ipa"

	private String baseUploadUrl

	AppCenterUploadTask() {
		super()
		logger.debug("default read timeout {}", project.appcenter.readTimeout)
		readTimeout(project.appcenter.readTimeout)
		this.description = "Uploads the app (.ipa, .dsym) to App Center"
	}

	def prepareFiles() {
		File ipaFile =  copyIpaToDirectory(project.appcenter.outputDirectory)

		File dSymBundle = new File(getArchiveDirectory(), "dSYMs/" + getApplicationNameFromArchive() + ".app.dSYM")
		File dSYMFile = getDestinationFile(project.appcenter.outputDirectory, ".app.dSYM.zip")

		if (dSymBundle.exists()) {
			def zipArchive = new ZipArchive(dSYMFile, dSymBundle.parentFile, new CommandRunner())
			zipArchive.add(dSymBundle)
			zipArchive.create()
		}

		return [ipaFile, dSYMFile]
	}

	@TaskAction
	def upload() throws IOException {
		File ipaFile
		File dSYMFile
		String ipaUploadId
		String ipaUploadUrl
		String releaseUrl
		String dsymUploadId
		String dsymUploadUrl

		if (project.appcenter.apiToken == null) {
			throw new IllegalArgumentException("Cannot upload to App Center because API Token is missing")
		}

		this.baseUploadUrl = "${APP_CENTER_URL}/${PATH_BASE_API}/${project.appcenter.appOwner}/${project.appcenter.appName}"

		(ipaFile, dSYMFile) = prepareFiles()
		(ipaUploadId, ipaUploadUrl) = initIpaUpload()
		uploadIpa(ipaFile, ipaUploadUrl)
		releaseUrl = commitUpload(PATH_RELEASE_UPLOAD, ipaUploadId)
		distributeIpa("${APP_CENTER_URL}/${releaseUrl}")

		(dsymUploadId, dsymUploadUrl) = initDebugSymbolUpload()

		if (dSYMFile.exists()) {
			uploadDebugSymbols(dSYMFile, dsymUploadUrl)
			commitUpload(PATH_SYMBOL_UPLOAD, dsymUploadId)
		} else {
			logger.warn("Debug symbols not found at '" + dSYMFile.getPath() + "'. Skipping upload.")
		}
	}

	def initIpaUpload() {
		def headers = getHeaders()

		String response = httpUtil.sendJson(HttpUtil.HttpVerb.POST, "${baseUploadUrl}/${PATH_RELEASE_UPLOAD}", headers, "")

		def initIpaUploadResponse = new JsonSlurper().parseText(response) as InitIpaUploadResponse

		logger.info("App Center: IPA upload initialized.")
		return [initIpaUploadResponse.upload_id, initIpaUploadResponse.upload_url]
	}

	def uploadIpa(File ipaFile, String uploadUrl) {
		logger.info("App Center: Uploading IPA...")

		def headers = new HashMap<String, String>()
		def parameters = new HashMap<String, Object>()

		headers.put(HEADER_CONTENT_TYPE, MEDIA_TYPE_MULTI_FORM)
		parameters.put(PART_KEY_IPA, ipaFile)

		httpUtil.sendForm(HttpUtil.HttpVerb.POST, uploadUrl, headers, parameters)

		logger.info("App Center: IPA upload completed.")
	}

	def commitUpload(String path, String uploadId) {
		def headers = getHeaders()

		String json = new JsonBuilder(new CommitRequest()).toPrettyString()

		String response = httpUtil.sendJson(HttpUtil.HttpVerb.PATCH, "${baseUploadUrl}/${path}/${uploadId}", headers, json)

		def commitResponse = new JsonSlurper().parseText(response) as CommitResponse

		logger.info("App Center: IPA upload committed.")
		return commitResponse.release_url

	}

	def distributeIpa(String releaseUrl) {
		def headers = getHeaders()

		def distributionRequest = new DistributionRequest(project.appcenter.destination, project.appcenter.releaseNotes,
			project.appcenter.notifyTesters, project.appcenter.mandatoryUpdate)

		String json = new JsonBuilder(distributionRequest).toPrettyString()

		httpUtil.sendJson(HttpUtil.HttpVerb.PATCH, releaseUrl, headers, json)

		logger.info("App Center: IPA upload distributed to: '" + project.appcenter.destination + "'")
	}

	def initDebugSymbolUpload() {
		def headers = getHeaders()

		String json = new JsonBuilder(new InitDebugSymbolRequest()).toPrettyString()

		String response = httpUtil.sendJson(HttpUtil.HttpVerb.POST, "${baseUploadUrl}/${PATH_SYMBOL_UPLOAD}", headers, json)
		def initDebugSymbolResponse = new JsonSlurper().parseText(response) as InitDebugSymbolResponse

		logger.info("App Center: Debug symbol upload initialized.")
		return [initDebugSymbolResponse.symbol_upload_id, initDebugSymbolResponse.upload_url]
	}

	def uploadDebugSymbols(File dSYMFile, String uploadUrl) {
		def headers = new HashMap<String, String>()
		def parameters = new HashMap<String, Object>()

		headers.put(HEADER_CONTENT_TYPE, MEDIA_TYPE_MULTI_FORM)
		headers.put(HEADER_BLOB_TYPE, BLOB_TYPE_BLOCK_BLOB)
		parameters.put(PART_KEY_IPA, dSYMFile)

		httpUtil.sendForm(HttpUtil.HttpVerb.PUT, uploadUrl, headers, parameters)

		logger.info("App Center: Debug symbol upload completed.")
	}

	@Internal
	def getHeaders() {
		def headers = new HashMap<String, String>()

		headers.put(HEADER_CONTENT_TYPE, MEDIA_TYPE_JSON)
		headers.put(HEADER_ACCEPT, MEDIA_TYPE_JSON)
		headers.put(HEADER_TOKEN, project.appcenter.apiToken)

		return headers
	}
}
