package org.openbakery.appcenter

import org.gradle.api.tasks.Internal
import org.openbakery.AbstractHttpDistributeTask

class AbstractAppCenterTask extends AbstractHttpDistributeTask {

	static final String HEADER_CONTENT_TYPE = "Content-Type"
	private static final String APP_CENTER_URL = "https://api.appcenter.ms"
	private static final String PATH_BASE_API = "v0.1/apps"
	private static final String HEADER_ACCEPT = "Accept"
	private static final String HEADER_TOKEN = "X-API-Token"
	private static final String MEDIA_TYPE_JSON = "application/json"

	@Internal
	def getBaseUploadUrl() {
		return "${APP_CENTER_URL}/${PATH_BASE_API}/${project.appcenter.appOwner}/${project.appcenter.appName}"
	}

	@Internal
	def getHeaders() {
		String apiToken = project.appcenter.apiToken
		if (apiToken == null) {
			throw new IllegalArgumentException("Cannot upload to App Center because API Token is missing")
		}

		def headers = new HashMap<String, String>()
		headers.put(HEADER_CONTENT_TYPE, MEDIA_TYPE_JSON)
		headers.put(HEADER_ACCEPT, MEDIA_TYPE_JSON)
		headers.put(HEADER_TOKEN, apiToken)

		return headers
	}
}
