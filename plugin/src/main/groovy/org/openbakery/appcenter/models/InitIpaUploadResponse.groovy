package org.openbakery.appcenter.models

class InitIpaUploadResponse extends Expando {
	String upload_id
	String upload_url

	def propertyMissing(name, value) {}
}
