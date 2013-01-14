package org.openbakery


class ProvisioningProfileIdReader {

	def readProvisioningProfileIdFromDestinationRoot(def destinationRoot) {
		File provisionDestinationFile = new File(destinationRoot)
		println provisionDestinationFile
		if (!provisionDestinationFile.exists()) {
			return
		}

		def fileList = provisionDestinationFile.list(
						[accept: {d, f -> f ==~ /.*mobileprovision/ }] as FilenameFilter
		).toList()

		if (fileList.size() > 0) {
			def mobileprovisionContent = new File(provisionDestinationFile, fileList[0]).text
			def matcher = mobileprovisionContent =~ "<key>UUID</key>\\s*\\n\\s*<string>(.*?)</string>"
			uuid = matcher[0][1]
			return uuid;
		}
		return null;
	}

}
