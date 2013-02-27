package org.openbakery


class ProvisioningProfileIdReader {

	def readProvisioningProfileIdFromDestinationRoot(def destinationRoot) {
		println destinationRoot
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
