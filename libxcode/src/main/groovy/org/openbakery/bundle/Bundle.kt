package org.openbakery.bundle

import java.io.File

/**
 * This class should hold the information for a Bundle within the application like
 * the path, bundleIdentifier and also the provisioning profile that is used for this bundle
 */
class Bundle(path: File) {

	val path : File = path

	//var identifier: String? = null
	//var provisioningProfile: File? = null


	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as Bundle

		if (path != other.path) return false

		return true
	}

	override fun hashCode(): Int {
		return path.hashCode()
	}

	override fun toString(): String {
		return "Bundle(path=$path)"
	}


}
