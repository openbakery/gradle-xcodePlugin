package org.openbakery.model.internal

import org.gradle.platform.base.component.BaseComponentSpec
import org.openbakery.model.XcodeComponent

/**
 * Created by rene on 12.11.15.
 */
class DefaultXcodeComponent  extends BaseComponentSpec implements XcodeComponent {
	String name
	String version

	@Override
	String toString() {
		return "DefaultXcodeComponent[name=$name, version=$version]"
	}

}
