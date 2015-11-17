package org.openbakery.model

import org.gradle.platform.base.ComponentSpec

/**
 * Created by rene on 12.11.15.
 */
interface XcodeComponent extends ComponentSpec {
	String name
	String version
}
