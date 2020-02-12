package org.openbakery

import org.openbakery.http.HttpUtil

/**
 * User: awjones76
 * Date: 9/17/19
 */
class AbstractHttpDistributeTask extends AbstractDistributeTask {

	public HttpUtil httpUtil

	AbstractHttpDistributeTask() {
		httpUtil = new HttpUtil()
	}

	def readTimeout(Integer timeout) {
		httpUtil = new HttpUtil(timeout)
	}
}
