package org.openbakery.model

import org.gradle.platform.base.BinarySpec

/**
 * Created by rene on 12.11.15.
 */
interface XcodeBinary extends BinarySpec {
	String getName()
	String getVersion()
}
