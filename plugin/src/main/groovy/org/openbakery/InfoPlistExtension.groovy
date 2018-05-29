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

import org.gradle.api.Project
import org.gradle.api.provider.Property

class InfoPlistExtension {
	String bundleIdentifier = null
	String bundleIdentifierSuffix = null
	String bundleName = null
	String bundleDisplayName = null
	String bundleDisplayNameSuffix = null
	String version = null
	String versionSuffix = null
	String versionPrefix = null
	String shortVersionString = null
	String shortVersionStringSuffix = null
	String shortVersionStringPrefix = null
	List<String> commands = null

	final Property<String> configurationBundleIdentifier

	InfoPlistExtension(Project project) {
		this.configurationBundleIdentifier = project.objects.property(String)
	}

	void setCommands(Object commands) {
		if (commands instanceof List) {
			this.commands = commands;
		} else {
			this.commands = new ArrayList<String>();
			this.commands.add(commands.toString());
		}
	}

	boolean hasValuesToModify() {
		return bundleIdentifier != null ||
				bundleIdentifierSuffix != null ||
				bundleName != null ||
				bundleDisplayName != null ||
				bundleDisplayNameSuffix != null ||
				version != null ||
				versionSuffix != null ||
				versionPrefix != null ||
				shortVersionString != null ||
				shortVersionStringSuffix != null ||
				shortVersionStringPrefix != null ||
				commands != null;
	}
}
