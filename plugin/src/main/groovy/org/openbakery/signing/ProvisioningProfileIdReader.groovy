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

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.lang.time.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.DateFormat
import java.text.SimpleDateFormat


class ProvisioningProfileIdReader {

	private static Logger logger = LoggerFactory.getLogger(ProvisioningProfileIdReader.class)

	XMLPropertyListConfiguration config;

	ProvisioningProfileIdReader(def provisioningProfile) {
		String text = load(provisioningProfile)
		config = new XMLPropertyListConfiguration()
		config.load(new StringReader(text))
		checkExpired();
	}

	String load(def provisioningProfile) {
		if (!(provisioningProfile instanceof File)) {
			provisioningProfile = new File(provisioningProfile.toString())
		}
		if (!provisioningProfile.exists()) {
			return nil;
		}

		StringBuffer result = new StringBuffer();

		boolean append = false;
		for (String line : provisioningProfile.text.split("\n")) {
			if (line.startsWith("<!DOCTYPE plist PUBLIC")) {
				append = true;
			}

			if (line.startsWith("</plist>")) {
				result.append("</plist>")
				return result.toString();
			}

			if (append) {
				result.append(line)
				result.append("\n");
			}


		}
		return ""
	}


	boolean checkExpired() {

		Date expireDate = config.getProperty("ExpirationDate")
		if (expireDate.before(new Date())) {
			DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.getDefault());
			throw new IllegalArgumentException("The Provisioning Profile has expired on " + formatter.format(expireDate) );
		}
	}


	String getUUID() {
		return config.getString("UUID")
	}

	String getApplicationIdentifierPrefix() {
		return config.getString("ApplicationIdentifierPrefix")

	}

	String getApplicationIdentifier() {
		String value = config.getString("Entitlements.application-identifier")
		String prefix = getApplicationIdentifierPrefix() + "."
		if (value.startsWith(prefix)) {
			return value.substring(prefix.length())
		}
		return value;
	}
}
