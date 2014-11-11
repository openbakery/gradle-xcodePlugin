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

import org.apache.commons.lang.time.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.DateFormat
import java.text.SimpleDateFormat


class ProvisioningProfileIdReader {

	private static Logger logger = LoggerFactory.getLogger(ProvisioningProfileIdReader.class)


	boolean checkExpired(String source) {
		def matcher = source =~ "<key>ExpirationDate</key>\\s*\\n\\s*<date>(.*?)</date>"
		if (matcher.find()) {
			String[] dateParsePatterns = ["yyyy-MM-dd'T'HH:mm:ssX"];
			Date date = DateUtils.parseDate(matcher[0][1], dateParsePatterns);
			if (date.before(new Date())) {
				DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.getDefault());
				throw new IllegalArgumentException("The Provisioning Profile has expired on " + formatter.format(date) );
			}
		}
	}


	String readProvisioningProfileUUID(def source) {
		logger.debug("destinationRoot: {}", source);
		if (!(source instanceof File)) {
			source = new File(source.toString())
		}
		if (source.exists()) {
			def mobileprovisionContent = source.text
			checkExpired(mobileprovisionContent);

			def matcher = mobileprovisionContent =~ "<key>UUID</key>\\s*\\n\\s*<string>(.*?)</string>"
			if (matcher.find()) {
				return matcher[0][1];
			}
		}
		return null;
	}


}
