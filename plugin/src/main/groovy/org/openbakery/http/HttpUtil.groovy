package org.openbakery.http

import okhttp3.*
import java.util.concurrent.TimeUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HttpUtil {
	private static final String MEDIA_TYPE_JSON = "application/json"
	private static final String MEDIA_TYPE_OCTET = "application/octet-stream"

	enum HttpVerb {
		PUT,
		POST,
		PATCH
	}

	private OkHttpClient okHttpClient
	private static Logger logger = LoggerFactory.getLogger(HttpUtil.class)

	HttpUtil(timeout = 120) {
		okHttpClient = new OkHttpClient.Builder()
			.readTimeout(timeout, TimeUnit.SECONDS)
			.build()
	}

	String sendJson(HttpVerb httpVerb, String url, Map<String, String> headers, String json) {
		return sendRequest(httpVerb, url, headers, null, json)
	}

	String sendForm(HttpVerb httpVerb, String url, Map<String, String> headers, Map<String, Object> parameters) {
		return sendRequest(httpVerb, url, headers, parameters, null)
	}

	private String sendRequest(HttpVerb httpVerb, String url, Map<String, String> headers, Map<String, Object> parameters, String json) {
		logger.debug("using URL {}", url)
		logger.debug("http headers {}", headers)
		logger.debug("http parameters {}", parameters)
		logger.debug("http parameters {}", json)

		RequestBody requestBody

		Request.Builder requestBuilder = new Request.Builder()
			.url(url)

		if (json == null) {
			MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)

			parameters.each() { key, value ->
				if (value instanceof String) {
					bodyBuilder.addFormDataPart(key, value)
				} else if (value instanceof File) {
					bodyBuilder.addFormDataPart(key, (value as File).name,
						RequestBody.create(
							value as File, MediaType.parse(MEDIA_TYPE_OCTET)))
				}
			}

			requestBody = bodyBuilder.build()
		} else {
			requestBody = RequestBody.create(json, MediaType.parse(MEDIA_TYPE_JSON))
		}

		switch (httpVerb) {
			case HttpVerb.PUT:
				requestBuilder.put(requestBody)
				break
			case HttpVerb.POST:
				requestBuilder.post(requestBody)
				break
			case HttpVerb.PATCH:
				requestBuilder.patch(requestBody)
				break
		}

		headers.each() { key, value ->
			requestBuilder.addHeader(key, value)
		}

		Request request = requestBuilder.build()

		Response response = okHttpClient.newCall(request).execute()

		if (response.code >= 400) {
			throw new IllegalStateException("Http request failed: " + response.code + " " + response.message + ": " + response.body.string())
		}

		return response.body().string()
	}
}
