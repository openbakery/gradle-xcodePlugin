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

import com.google.gson.Gson
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.gradle.api.tasks.TaskAction
import org.jetbrains.annotations.NotNull
import org.openbakery.AbstractDistributeTask
import org.openbakery.CommandRunner
import org.openbakery.appcenter.models.CommitRequest
import org.openbakery.appcenter.models.CommitResponse
import org.openbakery.appcenter.models.DistributionRequest
import org.openbakery.appcenter.models.InitDebugSymbolRequest
import org.openbakery.appcenter.models.InitDebugSymbolResponse
import org.openbakery.appcenter.models.InitIpaUploadResponse
import org.openbakery.util.ZipArchive

class AppCenterUploadTask extends AbstractDistributeTask {

	private static final String BASE_URL = "https://api.appcenter.ms"
	private static final String PATH_BASE_API = "v0.1/apps"
	private static final String PATH_RELEASE_UPLOAD = "release_uploads"
	private static final String PATH_SYMBOL_UPLOAD = "symbol_uploads"
	private static final String HEADER_CONTENT_TYPE = "Content-Type"
	private static final String HEADER_BLOB_TYPE = "x-ms-blob-type"
	private static final String HEADER_ACCEPT = "Accept"
	private static final String HEADER_TOKEN = "X-API-Token"
	private static final String MEDIA_TYPE_JSON = "application/json"
	private static final String MEDIA_TYPE_MULTI_FORM = "multipart/form-data"
	private static final String MEDIA_TYPE_OCTET = "application/octet-stream"
	private static final String BLOB_TYPE_BLOCK_BLOB = "BlockBlob"
	private static final String PART_KEY_IPA = "ipa"

	private OkHttpClient okHttpClient
	private HttpUrl baseUrl

	AppCenterUploadTask() {
		super()
		this.description = "Uploads the app (.ipa, .dsym) to App Center"

		okHttpClient = new OkHttpClient()
		baseUrl = HttpUrl.get(BASE_URL)
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

		(ipaFile, dSYMFile) = prepareFiles()
		(ipaUploadId, ipaUploadUrl) = initIpaUpload()
		uploadIpa(ipaFile, ipaUploadUrl)
		releaseUrl = commitUpload(PATH_RELEASE_UPLOAD, ipaUploadId)
		distributeIpa(releaseUrl)

		(dsymUploadId, dsymUploadUrl) = initDebugSymbolUpload()

		if (dSYMFile.exists()) {
			uploadDebugSymbols(dSYMFile, dsymUploadUrl)
			commitUpload(PATH_SYMBOL_UPLOAD, dsymUploadId)
		} else {
			logger.warn("Debug symbols not found at '" + dSYMFile.getPath() + "'. Skipping upload.")
		}
	}

	def initIpaUpload() {
		Request request = new Request.Builder()
			.url(baseUrl.newBuilder()
				.addEncodedPathSegments(PATH_BASE_API)
				.addEncodedPathSegments(project.appcenter.appOwner)
				.addEncodedPathSegments(project.appcenter.appName)
				.addEncodedPathSegments(PATH_RELEASE_UPLOAD)
				.build())
			.addHeader(HEADER_CONTENT_TYPE, MEDIA_TYPE_JSON)
			.addHeader(HEADER_ACCEPT, MEDIA_TYPE_JSON)
			.addHeader(HEADER_TOKEN, project.appcenter.apiToken)
			.post(RequestBody.create("", MediaType.parse(MEDIA_TYPE_JSON)))
			.build()

		Response response = okHttpClient.newCall(request).execute()

		if (!response.isSuccessful()) {
			throw new IllegalStateException(response.message())
		}

		InitIpaUploadResponse initUploadResponse = new Gson().fromJson(response.body().string(), InitIpaUploadResponse)

		logger.info("App Center: IPA upload initialized.")

		return [initUploadResponse.upload_id, initUploadResponse.upload_url]
	}

	def uploadIpa(File ipaFile, String uploadUrl) {
		logger.info("App Center: Uploading IPA...")
		Request request = new Request.Builder()
			.url(uploadUrl)
			.addHeader(HEADER_CONTENT_TYPE, MEDIA_TYPE_MULTI_FORM)
			.post(new MultipartBody.Builder()
				.addFormDataPart(PART_KEY_IPA, ipaFile.getName(),
					RequestBody.create(ipaFile, MediaType.parse(MEDIA_TYPE_OCTET)))
				.build())
			.build()

		Response response = okHttpClient.newCall(request).execute()

		if (!response.isSuccessful()) {
			throw new IllegalStateException(response.message())
		}

		logger.info("App Center: IPA upload completed.")
	}

	def commitUpload(String path, String uploadId) {
		Request request = new Request.Builder()
			.url(baseUrl.newBuilder()
				.addEncodedPathSegments(PATH_BASE_API)
				.addEncodedPathSegments(project.appcenter.appOwner)
				.addEncodedPathSegments(project.appcenter.appName)
				.addEncodedPathSegments(path)
				.addEncodedPathSegments(uploadId)
				.build())
			.addHeader(HEADER_CONTENT_TYPE, MEDIA_TYPE_JSON)
			.addHeader(HEADER_ACCEPT, MEDIA_TYPE_JSON)
			.addHeader(HEADER_TOKEN, project.appcenter.apiToken)
			.patch(RequestBody.create(new Gson().toJson(new CommitRequest()), MediaType.parse(MEDIA_TYPE_JSON)))
			.build()

		Response response = okHttpClient.newCall(request).execute()

		if (!response.isSuccessful()) {
			throw new IllegalStateException(response.message())
		}

		CommitResponse commitResponse = new Gson().fromJson(response.body().string(), CommitResponse)

		logger.info("App Center: IPA upload committed.")

		return commitResponse.release_url
	}

	def distributeIpa(String releaseUrl) {
		Request request = new Request.Builder()
			.url(baseUrl.newBuilder()
				.addEncodedPathSegments(releaseUrl)
				.build())
			.addHeader(HEADER_CONTENT_TYPE, MEDIA_TYPE_JSON)
			.addHeader(HEADER_ACCEPT, MEDIA_TYPE_JSON)
			.addHeader(HEADER_TOKEN, project.appcenter.apiToken)
			.patch(RequestBody.create(
				new Gson().toJson(new DistributionRequest(project.appcenter.destination, project.appcenter.releaseNotes,
					project.appcenter.notifyTesters, project.appcenter.mandatoryUpdate)),
				MediaType.parse(MEDIA_TYPE_JSON)))
			.build()

		Response response = okHttpClient.newCall(request).execute()

		if (!response.isSuccessful()) {
			throw new IllegalStateException(response.message())
		}

		logger.info("App Center: IPA upload distributed to: '" + project.appcenter.destination + "'")
	}

	def initDebugSymbolUpload() {
		Request request = new Request.Builder()
			.url(baseUrl.newBuilder()
				.addEncodedPathSegments(PATH_BASE_API)
				.addEncodedPathSegments(project.appcenter.appOwner)
				.addEncodedPathSegments(project.appcenter.appName)
				.addEncodedPathSegments(PATH_SYMBOL_UPLOAD)
				.build())
			.addHeader(HEADER_CONTENT_TYPE, MEDIA_TYPE_JSON)
			.addHeader(HEADER_ACCEPT, MEDIA_TYPE_JSON)
			.addHeader(HEADER_TOKEN, project.appcenter.apiToken)
			.post(RequestBody.create(new Gson().toJson(new InitDebugSymbolRequest()), MediaType.parse(MEDIA_TYPE_JSON)))
			.build()

		Response response = okHttpClient.newCall(request).execute()

		if (!response.isSuccessful()) {
			throw new IllegalStateException(response.message())
		}

		InitDebugSymbolResponse initDebugSymbolResponse = new Gson().fromJson(response.body().string(), InitDebugSymbolResponse)

		logger.info("App Center: Debug symbol upload initialized.")

		return [initDebugSymbolResponse.symbol_upload_id, initDebugSymbolResponse.upload_url]
	}

	def uploadDebugSymbols(File dSYMFile, String uploadUrl) {
		logger.info("App Center: Uploading debug symbols...")

		Request request = new Request.Builder()
			.url(uploadUrl)
			.addHeader(HEADER_CONTENT_TYPE, MEDIA_TYPE_MULTI_FORM)
			.addHeader(HEADER_BLOB_TYPE, BLOB_TYPE_BLOCK_BLOB)
			.put(new MultipartBody.Builder()
				.addFormDataPart(PART_KEY_IPA, dSYMFile.getName(),
					RequestBody.create(dSYMFile, MediaType.parse(MEDIA_TYPE_OCTET)))
				.build())
			.build()

		Response response = okHttpClient.newCall(request).execute()

		if (!response.isSuccessful()) {
			throw new IllegalStateException(response.message())
		}

		logger.info("App Center: Debug symbol upload completed.")
	}
}
