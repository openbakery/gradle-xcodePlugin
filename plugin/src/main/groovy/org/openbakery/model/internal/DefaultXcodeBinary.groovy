package org.openbakery.model.internal

import org.gradle.platform.base.binary.BaseBinarySpec
import org.openbakery.model.XcodeBinary

/**
 * Created by rene on 12.11.15.
 */
class DefaultXcodeBinary extends BaseBinarySpec implements XcodeBinary {

	String name
	String version

	@Override
	String toString() {
		return "DefaultXcodeBinary[name=$name, version=$version]"
	}
}
