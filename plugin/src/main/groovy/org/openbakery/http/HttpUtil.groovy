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
		PATCH,
		GET
	}

	private OkHttpClient okHttpClient
	private static Logger logger = LoggerFactory.getLogger(HttpUtil.class)
	public Integer readTimeoutInSeconds

	HttpUtil(timeout = 120) {
		readTimeoutInSeconds = timeout
		okHttpClient = new OkHttpClient.Builder()
			.readTimeout(timeout, TimeUnit.SECONDS)
			.build()
	}

	String sendJson(HttpVerb httpVerb, String url, Map<String, String> headers, Map<String, Object> parameters, String json) {
		logger.debug("http json {}", json)
		def requestBuilder = create(url, httpVerb, headers, parameters, RequestBody.create(json, MediaType.parse(MEDIA_TYPE_JSON)))
		return execute(requestBuilder)
	}

	String sendForm(HttpVerb httpVerb, String url, Map<String, String> headers, Map<String, Object> parameters) {
		logger.debug("http form parameters {}", parameters)
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

		def requestBody = bodyBuilder.build()
		return execute(create(url, httpVerb, headers, null, requestBody))
	}

	String sendFile(HttpVerb httpVerb, String url, Map<String, String> headers, Map<String, Object> parameters, byte[] file) {
		logger.debug("http byte size {}", file.size())
		def requestBody = RequestBody.create(file, MediaType.parse(MEDIA_TYPE_OCTET))
		def requestBuilder = create(url, httpVerb, headers, null, requestBody)
		return execute(requestBuilder)
	}

	String getJson(String url, Map<String, String> headers, Map<String, Object> parameters) {
		def requestBuilder = create(url, HttpVerb.GET, headers, parameters, null)
		return execute(requestBuilder)
	}

	private static Request.Builder create(String url, HttpVerb httpVerb, Map<String, String> headers, Map<String, Object> parameters, RequestBody requestBody) {
		logger.debug("using URL {}", url)
		logger.debug("http headers {}", headers)
		logger.debug("http parameters {}", parameters)

		HttpUrl.Builder httpBuilder = HttpUrl.parse(url).newBuilder()
		if(parameters) {
			parameters.each {key, value ->
				httpBuilder.addQueryParameter(key, value.toString())
			}
		}

		Request.Builder requestBuilder = new Request.Builder()
			.url(httpBuilder.build())

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
			case HttpVerb.GET:
				requestBuilder.get()
				break
		}

		headers.each() { key, value ->
			requestBuilder.addHeader(key, value)
		}

		return requestBuilder
	}

	private String execute(Request.Builder requestBuilder) {
		Request request = requestBuilder.build()

		Response response = okHttpClient.newCall(request).execute()
		if (response.code() >= 400) {
			throw new IllegalStateException("Http request failed: " + response.code + " " + response.message + ": " + response.body.string())
		}

		return response.body().string()
	}
}
