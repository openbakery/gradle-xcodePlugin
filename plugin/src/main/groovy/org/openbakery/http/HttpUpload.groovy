package org.openbakery.http

import org.apache.http.Consts
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by rene on 17.02.15.
 */
class HttpUpload {

	private static Logger logger = LoggerFactory.getLogger(HttpUpload.class)

	private static final ContentType contentType = ContentType.create("application/x-www-form-urlencoded", Consts.UTF_8)

	private String url;

	public HttpUpload() {
		this(null)
	}

	public HttpUpload(String url) {
		this.url = url
	}

	void postRequest(Map<String, Object> parameters) {
		postRequest(null, parameters)
	}

	void postRequest(Map<String, String> headers, Map<String, Object> parameters) {

		logger.debug("using URL {}", url)
		logger.debug("http post headers {}", headers)
		logger.debug("http post parameters {}", parameters)

		HttpPost httpPost = new HttpPost(url);

		MultipartEntityBuilder requestEntityBuilder = MultipartEntityBuilder.create()

		parameters.each() { key, value ->
			if (value instanceof String) {
				requestEntityBuilder.addPart(key, new StringBody(value, contentType))
			} else if (value instanceof File) {
				requestEntityBuilder.addBinaryBody(key, value)
			}
		}
		HttpEntity entity = requestEntityBuilder.build();
		logger.debug("entity {}", entity)

		httpPost.setEntity(entity)

		headers.each() { key, value ->
			httpPost.addHeader(key, value)
		}

		execute(httpPost)
	}


	void execute(HttpRequestBase request) {
		CloseableHttpClient httpClient = HttpClients.createDefault();


		CloseableHttpResponse response = httpClient.execute(request);
		try {
			logger.debug("{}", response.getStatusLine());
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				logger.debug("Response content length: {}", entity.getContentLength());
			}
			String responseString = EntityUtils.toString(entity, "UTF-8");
			logger.debug("{}", responseString);
			EntityUtils.consume(entity);

			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode >= 400) {
				throw new IllegalStateException("Http request failed: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase() + ": " + responseString);
			}
		} finally {
			httpClient.close();
			response.close();
		}
	}

}
