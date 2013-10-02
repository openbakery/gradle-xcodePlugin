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
package org.openbakery



class ProvisioningProfileIdReader {

	def readProvisioningProfileIdFromDestinationRoot(def destinationRoot) {
		logger.debug("destinationRoot: {}", destinationRoot);
		if (!destinationRoot.exists()) {
			return
		}

		def fileList = destinationRoot.list(
						[accept: {d, f -> f ==~ /.*mobileprovision/ }] as FilenameFilter
		).toList()

		if (fileList.size() > 0) {
			def mobileprovisionContent = new File(destinationRoot, fileList[0]).text
			def matcher = mobileprovisionContent =~ "<key>UUID</key>\\s*\\n\\s*<string>(.*?)</string>"
			def uuid = matcher[0][1]
			return uuid;
		}
		return null;
	}

}
