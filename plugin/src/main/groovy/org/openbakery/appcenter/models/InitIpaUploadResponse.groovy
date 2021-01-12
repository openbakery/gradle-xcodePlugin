package org.openbakery.appcenter.models

class InitIpaUploadResponse extends Expando {
	String id
	String upload_domain
	String package_asset_id
	String token

	def propertyMissing(name, value) {}
}
