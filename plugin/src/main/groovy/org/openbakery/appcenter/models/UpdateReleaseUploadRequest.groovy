package org.openbakery.appcenter.models

class UpdateReleaseUploadRequest {
	String id
	String upload_status

	UpdateReleaseUploadRequest(String id, String status) {
		this.id = id
		this.upload_status = status
	}
}
