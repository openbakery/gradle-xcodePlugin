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
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.DateFormat

class ProvisioningProfileIdReader {

    protected CommandRunner commandRunner


	private static Logger logger = LoggerFactory.getLogger(ProvisioningProfileIdReader.class)

	XMLPropertyListConfiguration config;

    private File provisioningProfile

	ProvisioningProfileIdReader(def provisioningProfile) {

        super()

		String text = load(provisioningProfile)
		config = new XMLPropertyListConfiguration()

		config.load(new StringReader(text))

        commandRunner = new CommandRunner()


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

            // write temporary plist to disk
            FileUtils.writeStringToFile(new File("build/tmp/tmp.plist"), extractedPlist)

            // read temporary plist file
            File tempPlist = new File("build/tmp/tmp.plist")

            value = this.getValueFromPlist(tempPlist, "Entitlements:com.apple.application-identifier")
        }

		String prefix = getApplicationIdentifierPrefix() + "."
		if (value.startsWith(prefix)) {
			return value.substring(prefix.length())
		}
		return value;
	}

    /**
     * Reads the value for the given key from the given plist
     *
     * @param plist
     * @param key
     * @return returns the value for the given key
     */
    def getValueFromPlist(plist, key) {
        if (plist instanceof File) {
            plist = plist.absolutePath
        }

        try {
            String result = commandRunner.runWithResult([
                    "/usr/libexec/PlistBuddy",
                    plist,
                    "-c",
                    "Print :" + key])

            if (result.startsWith("Array {")) {

                ArrayList<String> resultArray = new ArrayList<String>();

                String[] tokens = result.split("\n");

                for (int i = 1; i < tokens.length - 1; i++) {
                    resultArray.add(tokens[i].trim());
                }
                return resultArray;
            }
            return result;
        } catch (IllegalStateException ex) {
            return null
        } catch (CommandRunnerException ex) {
            return null
        }
    }

}
