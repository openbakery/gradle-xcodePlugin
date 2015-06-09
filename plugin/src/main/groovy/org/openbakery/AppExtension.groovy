package org.openbakery

/**
 * Created by chaitanyar on 2/27/15.
 */

import org.openbakery.InfoPlistExtension;

class AppExtension {
	// These paths will be read from the xcode project file
	String infoPlistPath
	String entitlementsPath

	// The configurations should be specified by the user as needed
	def infoPlistConfig
	def entitlementsConfig
	def provisioningProfilePath
	String name

	public AppExtension(name,infoPlistConfig = null, entitlementsConfig = null, provisioningProfilePath = null) {
		super()
		this.infoPlistConfig = infoPlistConfig
		this.entitlementsConfig = entitlementsConfig
		this.name = name
		this.provisioningProfilePath = provisioningProfilePath
	}
}
