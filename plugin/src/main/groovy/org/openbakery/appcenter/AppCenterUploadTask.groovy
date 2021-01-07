package org.openbakery.appcenter

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.gradle.api.tasks.TaskAction
import org.openbakery.appcenter.models.*
import org.openbakery.http.HttpUtil

class AppCenterUploadTask extends AbstractAppCenterTask {

	private static final String PATH_RELEASE_UPLOAD = "uploads/releases"
	private static final String PATH_SET_METADATA = "upload/set_metadata"
	private static final String PATH_CHUNK_UPLOAD = "upload/upload_chunk"
	private static final String PATH_UPLOAD_FINISHED = "upload/finished"
	private static final String PATH_UPLOAD_RELEASES = "uploads/releases"
	private static final String PATH_RELEASES = "releases"

	AppCenterUploadTask() {
		super()
		this.description = "Uploads the app (.ipa) to App Center"
	}

	@TaskAction
	def upload() throws IOException {
		String ipaUploadId
		String ipaUploadDomain
		String assetId
		String token

		File ipaFile = copyIpaToDirectory(project.appcenter.outputDirectory)
		(ipaUploadId, ipaUploadDomain, assetId, token) = initIpaUpload()
		uploadFile(ipaUploadDomain, assetId, token, ipaFile)
		updateReleaseUpload(ipaUploadId, "uploadFinished")
		def releaseId = pollForReleaseId(ipaUploadId)
		getRelease(releaseId)
		distributeIpa(releaseId)
	}

	private def initIpaUpload() {
		logger.info("App Center: Initialize upload...")
		String response = httpUtil.sendJson(HttpUtil.HttpVerb.POST, "${baseUploadUrl}/${PATH_RELEASE_UPLOAD}", headers, null, "")

		def initIpaUploadResponse = new JsonSlurper().parseText(response) as InitIpaUploadResponse
		return [initIpaUploadResponse.id, initIpaUploadResponse.upload_domain, initIpaUploadResponse.package_asset_id, initIpaUploadResponse.token]
	}

	private void uploadFile(String domain, String assetId, String token, File binary) {
		def chunkSize = setReleaseUploadMetadata(domain, assetId, token, binary)
		uploadChunks(domain, assetId, token, binary, chunkSize)
		finishUpload(domain, assetId, token)
	}

	private Integer setReleaseUploadMetadata(String domain, String assetId, String token, File binary) {
		def parameters = [
			"file_name"   : binary.name,
			"file_size"   : binary.size().toString(),
			"token"       : token,
			"content_type": "application/octet-stream"
		]

		def response = httpUtil.sendJson(HttpUtil.HttpVerb.POST, "${domain}/${PATH_SET_METADATA}/${assetId}", headers, parameters, "")
		def releaseUploadMetadataResponse = new JsonSlurper().parseText(response) as ReleaseUploadMetadataResponse

		return releaseUploadMetadataResponse.chunk_size
	}

	private void uploadChunks(String domain, String assetId, String token, File binary, Integer chunkSize) {
		def blockNumber = 1
		def s = binary.newDataInputStream()
		s.eachByte(chunkSize) { byte[] bytes, Integer size ->
			def parameters = [
				"token"       : token,
				"block_number": blockNumber.toString()
			]
			def response = httpUtil.sendFile(HttpUtil.HttpVerb.POST, "${domain}/${PATH_CHUNK_UPLOAD}/${assetId}", [:], parameters, bytes, size)

			def uploadChunkResponse = new JsonSlurper().parseText(response) as UploadChunkResponse
			if (!uploadChunkResponse.error) {
				blockNumber = blockNumber + 1
			} else {
				throw new IllegalStateException("AppCenter Chunk Upload Error: " + uploadChunkResponse.toString())
			}
		}
	}

	private void finishUpload(String domain, String assetId, String token) {
		def parameters = ["token": token]
		def response = httpUtil.sendJson(HttpUtil.HttpVerb.POST, "${domain}/${PATH_UPLOAD_FINISHED}/${assetId}", headers, parameters, "")

		def finishUploadResponse = new JsonSlurper().parseText(response) as FinishUploadResponse
		logger.info("Finish Upload: " + finishUploadResponse.toString())
		if (finishUploadResponse.error) {
			throw new IllegalStateException("AppCenter Finish Upload Error: " + finishUploadResponse.toString())
		}
	}

	private Map updateReleaseUpload(String uploadId, String status) {
		def updateReleaseRequest = new UpdateReleaseUploadRequest(uploadId, status)
		String json = new JsonBuilder(updateReleaseRequest).toPrettyString()
		def response = httpUtil.sendJson(HttpUtil.HttpVerb.PATCH, "${baseUploadUrl}/${PATH_UPLOAD_RELEASES}/$uploadId", headers, null, json)

		return new JsonSlurper().parseText(response) as Map
	}

	private String pollForReleaseId(String uploadId) {
		logger.info("AppCenter: Polling...")

		while (true) {
			def response = httpUtil.getJson("${baseUploadUrl}/${PATH_UPLOAD_RELEASES}/${uploadId}", headers, [:])
			def uploadReleaseResponse = new JsonSlurper().parseText(response) as UploadReleaseResponse

			switch (uploadReleaseResponse.upload_status) {
				case "readyToBePublished":
					return uploadReleaseResponse.release_distinct_id
				case "error":
					throw new IllegalStateException("AppCenter Poll Upload Error: " + uploadReleaseResponse.toString())
					break
				default:
					sleep(1000)
					break
			}
		}
	}

	private Map getRelease(String releaseId) {
		logger.info("AppCenter: Get Release...")
		def response = httpUtil.getJson("${baseUploadUrl}/${PATH_RELEASES}/${releaseId}", headers, [:])
		return new JsonSlurper().parseText(response) as Map
	}

	def distributeIpa(String releaseId) {
		logger.info("AppCenter: Distribute...")
		def distributionRequest = new DistributionRequest(
			project.appcenter.destination,
			project.appcenter.releaseNotes,
			project.appcenter.notifyTesters,
			project.appcenter.mandatoryUpdate
		)
		String json = new JsonBuilder(distributionRequest).toPrettyString()
		httpUtil.sendJson(HttpUtil.HttpVerb.PATCH, "${baseUploadUrl}/${PATH_RELEASES}/${releaseId}", headers, null, json)

		logger.info("App Center: IPA upload distributed to: '" + project.appcenter.destination + "'")
	}
}
