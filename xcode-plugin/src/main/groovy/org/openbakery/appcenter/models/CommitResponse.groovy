package org.openbakery.appcenter.models

class CommitResponse extends Expando {
	String release_id
	String release_url

	def propertyMissing(name, value) {}
}
