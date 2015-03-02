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
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.openbakery.CommandRunner
import org.openbakery.PlistHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.DateFormat

class ProvisioningProfileIdReader {

    protected CommandRunner commandRunner

	private PlistHelper plistHelper


	private static Logger logger = LoggerFactory.getLogger(ProvisioningProfileIdReader.class)

	XMLPropertyListConfiguration config;

	public Project project

    private File provisioningProfile

	ProvisioningProfileIdReader(def provisioningProfile, def project) {

        super()

		String text = load(provisioningProfile)
		config = new XMLPropertyListConfiguration()

		config.load(new StringReader(text))

        commandRunner = new CommandRunner()

		plistHelper = new PlistHelper(project)

		this.project = project

		checkExpired();
	}

	String load(def provisioningProfile) {
		if (!(provisioningProfile instanceof File)) {
			this.provisioningProfile = new File(provisioningProfile.toString())
		} else {
            this.provisioningProfile = provisioningProfile
        }


		if (!this.provisioningProfile.exists()) {
			return null;
		}



		StringBuffer result = new StringBuffer();

		boolean append = false;
		for (String line : this.provisioningProfile.text.split("\n")) {
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

        String value

        if (this.provisioningProfile.path.endsWith(".mobileprovision")) {
            value = config.getProperty("Entitlements.application-identifier")
        } else {

            // unpack provisioning profile to plain plist
            String extractedPlist = commandRunner.runWithResult([   "security",
                                                                    "cms" ,
                                                                    "-D",
                                                                    "-i",
                                                                    provisioningProfile.path]);

			// read temporary plist file
			File tempPlist = new File(project.buildDir.absolutePath + "/tmp/tmp.plist")

            // write temporary plist to disk
            FileUtils.writeStringToFile(tempPlist, extractedPlist)

			value = plistHelper.getValueFromPlist(tempPlist, "Entitlements:com.apple.application-identifier", commandRunner)
        }

		String prefix = getApplicationIdentifierPrefix() + "."
		if (value.startsWith(prefix)) {
			return value.substring(prefix.length())
		}
		return value;
	}



}
