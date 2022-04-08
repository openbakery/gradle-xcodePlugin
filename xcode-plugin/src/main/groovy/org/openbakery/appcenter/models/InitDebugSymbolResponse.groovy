package org.openbakery.appcenter.models

class InitDebugSymbolResponse extends Expando {
	String symbol_upload_id
	String upload_url

	def propertyMissing(name, value) {}
}
