package org.openbakery.appcenter.models

class UploadReleaseResponse extends Expando {
	String upload_status
	String release_distinct_id

	def propertyMissing(name, value) {}
}
