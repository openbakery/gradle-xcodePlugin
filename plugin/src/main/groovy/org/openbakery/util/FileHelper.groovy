package org.openbakery.util

/**
 * Created by rene on 23.10.15.
 */
class FileHelper {


	public static boolean isSymlink(File file) throws IOException {
		if (file == null) {
			throw new NullPointerException("File must not be null");
		}
		File canonicalFile

		if (file.getParent() == null) {
			canonicalFile = file;
		} else {
			File canonicalDirectory = file.getParentFile().getCanonicalFile();
			canonicalFile = new File(canonicalDirectory, file.getName());
		}
		return !canonicalFile.getCanonicalFile().equals(canonicalFile.getAbsoluteFile())
	}
}
