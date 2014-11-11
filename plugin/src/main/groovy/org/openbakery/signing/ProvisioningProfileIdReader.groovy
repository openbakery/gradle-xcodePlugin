/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbakery.signing

import org.slf4j.Logger
import org.slf4j.LoggerFactory


class ProvisioningProfileIdReader {

	private static Logger logger = LoggerFactory.getLogger(ProvisioningProfileIdReader.class)


	def readProvisioningProfileUUID(def source) {
		logger.debug("destinationRoot: {}", source);
		if (!(source instanceof File)) {
			source = new File(source.toString())
		}
		if (source.exists()) {
			def mobileprovisionContent = source.text
			def matcher = mobileprovisionContent =~ "<key>UUID</key>\\s*\\n\\s*<string>(.*?)</string>"
			def uuid = matcher[0][1]
			return uuid;
		}
		return null;
	}

}
