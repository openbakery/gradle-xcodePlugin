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
package org.openbakery.appstore

import org.gradle.api.tasks.TaskAction

class AppstoreUploadTask extends AbstractAppstoreTask {

	AppstoreUploadTask() {
		super()
		this.description = "Distributes the build to Apples Appstore"
	}


	@TaskAction
	def upload() {
		if (xcode.version.major > 12 && project.appstore.useNewUpload) {
			if (project.appstore.publicId == null) {
				throw new IllegalArgumentException("Appstore Public Id is missing. Parameter: appstore.publicId")
			}
			if (project.appstore.appleId == null) {
				throw new IllegalArgumentException("Appstore Apple Id is missing. Parameter: appstore.appleId")
			}
			if (project.appstore.bundleVersion == null) {
				throw new IllegalArgumentException("Appstore Bundle Version is missing. Parameter: appstore.bundleVersion")
			}
			if (project.appstore.shortBundleVersion == null) {
				throw new IllegalArgumentException("Appstore Short Bundle Version is missing. Parameter: appstore.shortBundleVersion")
			}
			if (project.appstore.bundleIdentifier == null) {
				throw new IllegalArgumentException("Appstore Bundle Identifier is missing. Parameter: appstore.bundleIdentifier")
			}

			String[] parameters = [
				"--asc-public-id",
				project.appstore.publicId,
				"--apple-id",
				project.appstore.appleId,
				"--bundle-version",
				project.appstore.bundleVersion,
				"--bundle-short-version-string",
				project.appstore.shortBundleVersion,
				"--bundle-id",
				project.appstore.bundleIdentifier
			]

			runAltool("--upload-package", parameters)
		} else {
			runAltool("--upload-app")
		}
	}

}
