package org.openbakery.util

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project

class FileUtil {

	static File download(Project project,
						 File toDirectory,
						 String address) throws IllegalArgumentException {

		if (StringUtils.isEmpty(address)) {
			throw new IllegalArgumentException("Cannot download, because no address was given")
		}

		if (!toDirectory.exists()) {
			toDirectory.mkdirs()
		}

		try {
			project.ant.get(src: address,
					dest: toDirectory.getPath(),
					verbose: true)
		} catch (Exception ex) {
			project.logger.error("cannot download file from the given location: {}", address)
			throw ex
		}

		File file = new File(toDirectory, FilenameUtils.getName(address))
		return file
	}
}
