package org.openbakery.appcenter

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.openbakery.CommandRunner
import org.openbakery.appcenter.models.CommitRequest
import org.openbakery.appcenter.models.CommitResponse
import org.openbakery.appcenter.models.InitDebugSymbolRequest
import org.openbakery.appcenter.models.InitDebugSymbolResponse
import org.openbakery.http.HttpUtil
import org.openbakery.util.ZipArchive

class AppCenterDsymUploadTask extends AbstractAppCenterTask {

	private static final String HEADER_BLOB_TYPE = "x-ms-blob-type"
	private static final String MEDIA_TYPE_MULTI_FORM = "multipart/form-data"
	private static final String BLOB_TYPE_BLOCK_BLOB = "BlockBlob"
	private static final String PART_KEY_IPA = "ipa"
	private static final String PATH_SYMBOL_UPLOAD = "symbol_uploads"

	AppCenterDsymUploadTask() {
		super()
		this.description = "Uploads the debug symbols (.dsym) to App Center"
	}

	@TaskAction
	def upload() throws IOException{
		File dsymDestinationFile = getDestinationFile(project.appcenter.outputDirectory, ".app.dSYM.zip")

		def zipArchive = new ZipArchive(dsymDestinationFile, dsymDirectory, new CommandRunner())
		dsymDirectory.eachFileRecurse { file ->
			if (file.toString().toLowerCase().endsWith(".dsym")) {
				zipArchive.add(file)
			}
		}
		zipArchive.create()

		String dsymUploadId
		String dsymUploadUrl
		(dsymUploadId, dsymUploadUrl) = initDebugSymbolUpload()
		uploadDebugSymbols(dsymDestinationFile, dsymUploadUrl)
		commitUpload(PATH_SYMBOL_UPLOAD, dsymUploadId)
	}

	@Internal
	File getDsymDirectory() {
		File dsymDirectory = new File(getArchiveDirectory(), "dSYMs")
		if (!dsymDirectory.exists()) {
			throw new IllegalStateException("dSYM bundle not found: " + dsymDirectory.absolutePath)
		}
		return dsymDirectory
	}

	def initDebugSymbolUpload() {
		String requestBody = new JsonBuilder(new InitDebugSymbolRequest()).toPrettyString()
		String response = httpUtil.sendJson(HttpUtil.HttpVerb.POST, "${baseUploadUrl}/${PATH_SYMBOL_UPLOAD}", headers, null, requestBody)
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

	def commitUpload(String path, String uploadId) {
		logger.info("App Center: Commit debug symbol upload...")

		String json = new JsonBuilder(new CommitRequest()).toPrettyString()
		String response = httpUtil.sendJson(HttpUtil.HttpVerb.PATCH, "${baseUploadUrl}/${path}/${uploadId}", headers, null, json)

		def commitResponse = new JsonSlurper().parseText(response) as CommitResponse
		return commitResponse.release_url
	}
}
