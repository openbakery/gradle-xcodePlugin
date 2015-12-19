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

class InfoPlistExtension {
	def String bundleIdentifier = null
	def String bundleIdentifierSuffix = null
	def String bundleName = null
	def String bundleDisplayName = null
	def String bundleDisplayNameSuffix = null
	def String version = null
	def String versionSuffix = null
	def String versionPrefix = null
	def String shortVersionString = null
	def String shortVersionStringSuffix = null
	def String shortVersionStringPrefix = null
	def List<String> commands = null



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