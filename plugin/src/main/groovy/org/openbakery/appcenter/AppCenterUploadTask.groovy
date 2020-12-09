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
import groovy.json.JsonOutput
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
	private static final String PATH_RELEASE_UPLOAD = "uploads/releases"
	private static final String PATH_SET_METADATA = "upload/set_metadata"
	private static final String PATH_CHUNK_UPLOAD = "upload/upload_chunk"
	private static final String PATH_UPLOAD_FINISHED = "upload/finished"
	private static final String PATH_UPLOAD_RELEASES = "uploads/releases"
	private static final String PATH_RELEASES = "releases"
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

		File dSymBundle = new File(getArchiveDirectory(), "dSYMs")
		File dsymDestinationFile = getDestinationFile(project.appcenter.outputDirectory, ".app.dSYM.zip")

		if (dSymBundle.exists()) {
			def zipArchive = new ZipArchive(dsymDestinationFile, dSymBundle, new CommandRunner())
			dSymBundle.eachFileRecurse { file ->
				if (file.toString().toLowerCase().endsWith(".dsym")) {
					zipArchive.add(file)
				}
			}
			zipArchive.create()
		}

		return [ipaFile, dsymDestinationFile]
	}

	@TaskAction
	def upload() throws IOException {
		File ipaFile
		File dSYMFile
		String ipaUploadId
		String ipaUploadDomain
		String assetId
		String token
		String dsymUploadId
		String dsymUploadUrl

		if (project.appcenter.apiToken == null) {
			throw new IllegalArgumentException("Cannot upload to App Center because API Token is missing")
		}

		this.baseUploadUrl = "${APP_CENTER_URL}/${PATH_BASE_API}/${project.appcenter.appOwner}/${project.appcenter.appName}"

		(ipaFile, dSYMFile) = prepareFiles()
		(ipaUploadId, ipaUploadDomain, assetId, token) = initIpaUpload()
		uploadFile(assetId, token, ipaFile)
		updateReleaseUpload(ipaUploadId, "uploadFinished")
		def releaseId = pollForReleaseId(ipaUploadId)
		def release = getRelease(releaseId)
		logger.info("download_url")
		distributeIpa(releaseId)
		(dsymUploadId, dsymUploadUrl) = initDebugSymbolUpload()

		logger.info("download_url: ${release["download_url"]}")
		logger.info("install_url: ${release["release_url"]}")

		if (dSYMFile.exists()) {
			uploadDebugSymbols(dSYMFile, dsymUploadUrl)
			commitUpload(PATH_SYMBOL_UPLOAD, dsymUploadId)
		} else {
			logger.warn("Debug symbols not found at '" + dSYMFile.getPath() + "'. Skipping upload.")
		}
	}

	def initIpaUpload() {
		def headers = getHeaders()

		String response = httpUtil.sendJson(HttpUtil.HttpVerb.POST, "${baseUploadUrl}/${PATH_RELEASE_UPLOAD}", headers, null, "")

		def initIpaUploadResponse = new JsonSlurper().parseText(response) as InitIpaUploadResponse
		logger.info("App Center: IPA upload initialized.")
		return [initIpaUploadResponse.id, initIpaUploadResponse.upload_domain, initIpaUploadResponse.package_asset_id, initIpaUploadResponse.token]
	}

	private void uploadFile(String assetId, String token, File binary) {
		def chunkSize = setReleaseUploadMetadata(assetId, token, binary)
		uploadChunks(assetId, token, binary, chunkSize)
		uploadFinish(assetId, token)
	}

	private Integer setReleaseUploadMetadata(String assetId, String token, File binary) {
		def parameters = new HashMap<String, String>()
		parameters.put("file_name", binary.name)
		parameters.put("file_size", binary.size().toString())
		parameters.put("token", token)
		parameters.put("content_type", "application/octet-stream")
		String uploadUrl = "${baseUploadUrl}/${PATH_SET_METADATA}/${assetId}"
		def response = httpUtil.sendForm(HttpUtil.HttpVerb.POST, uploadUrl, headers, parameters)

		def json = new JsonSlurper().parseText(response)

		return json.chunk_size
	}

	private void uploadChunks(String assetId, String token, File binary, Integer chunksize) {
		def blockNumber = 1
		def s = binary.newDataInputStream()
		String url = "${baseUploadUrl}/${PATH_CHUNK_UPLOAD}/${assetId}"
		s.eachByte(chunksize) { byte[] bytes, Integer size ->
			logger.info("upload chunk ${blockNumber}".toString())

			def parameters = new HashMap<String, String>()
			parameters.put("token", token)
			parameters.put("block_number", blockNumber.toString())

			def response = httpUtil.sendFile(HttpUtil.HttpVerb.POST, url, [:], parameters, bytes)

			def json = new JsonSlurper().parseText(response)
			if (json.error == false) {
				blockNumber = blockNumber + 1
			}
		}
	}

	private void uploadFinish(String assetId, String token) {
		def url = "${baseUploadUrl}/${PATH_UPLOAD_FINISHED}/${assetId}"
		def parameters = ["token": token]

		def response = httpUtil.sendJson(HttpUtil.HttpVerb.POST, url, headers, parameters, null)

		def json = new JsonSlurper().parseText(response)
		if (json.error == true) {
			logger.error("Error finishing upload")
		}
	}

	private Map updateReleaseUpload(String uploadId, String status) {
		def url = "${baseUploadUrl}/${PATH_UPLOAD_RELEASES}/$uploadId"
		def body = [
			"upload_status": status,
			"id"           : uploadId
		]

		def response = httpUtil.sendJson(HttpUtil.HttpVerb.PATCH, url, headers, null, JsonOutput.toJson(body))

		return new JsonSlurper().parseText(response) as Map
	}

	private String pollForReleaseId(String uploadId) {
		def url = "${baseUploadUrl}/${PATH_UPLOAD_RELEASES}/${uploadId}"

		while (true) {
			def response = httpUtil.getson(url, headers, [:])
			def json = new JsonSlurper().parseText(response)

			switch(json.upload_status) {
				case "readyToBePublished":
					def releaseId = json.release_distinct_id
					return releaseId
				case "error":
					throw new Exception()
					break
				default:
					sleep(1000)
					break
			}
		}
	}

	private Map getRelease(String releaseId) {
		def url = "${baseUploadUrl}/${PATH_UPLOAD_RELEASES}/${releaseId}"
		def response = httpUtil.getJson(url, headers, [:])
		return new JsonSlurper().parseText(response) as Map
	}

	def commitUpload(String path, String uploadId) {
		def headers = getHeaders()

		String json = new JsonBuilder(new CommitRequest()).toPrettyString()

		String response = httpUtil.sendJson(HttpUtil.HttpVerb.PATCH, "${baseUploadUrl}/${path}/${uploadId}", headers, null, json)

		def commitResponse = new JsonSlurper().parseText(response) as CommitResponse

		logger.info("App Center: IPA upload committed.")
		return commitResponse.release_url

	}

	def distributeIpa(String releaseId) {
		def headers = getHeaders()
		def url = "${baseUploadUrl}/${PATH_RELEASES}/${releaseId}"

		def distributionRequest = new DistributionRequest(
			project.appcenter.destination,
			project.appcenter.releaseNotes,
			project.appcenter.notifyTesters,
			project.appcenter.mandatoryUpdate
		)

		String json = new JsonBuilder(distributionRequest).toPrettyString()

		httpUtil.sendJson(HttpUtil.HttpVerb.PATCH, url, headers, null, json)

		logger.info("App Center: IPA upload distributed to: '" + project.appcenter.destination + "'")
	}

	def initDebugSymbolUpload() {
		def headers = getHeaders()

		String json = new JsonBuilder(new InitDebugSymbolRequest()).toPrettyString()

		String response = httpUtil.sendJson(HttpUtil.HttpVerb.POST, "${baseUploadUrl}/${PATH_SYMBOL_UPLOAD}", headers, null, json)
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
