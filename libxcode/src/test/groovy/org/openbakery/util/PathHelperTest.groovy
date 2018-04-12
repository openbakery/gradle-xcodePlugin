package org.openbakery.util

import org.openbakery.xcode.Type
import spock.lang.Specification

class PathHelperTest extends Specification {
	def "test iOS and TvOS symroot path resolution"() {
		setup:
		File tempSymRoot = File.createTempDir()

		expect:
		File file = PathHelper.resolvePath(type,
				simulator,
				tempSymRoot,
				configuration)

        println "${configuration}-${outputPath}"

		file == new File(tempSymRoot, "${configuration}-${outputPath}")

		where:
		simulator | configuration | type      | outputPath
		false     | "debug"       | Type.iOS  | PathHelper.IPHONE_OS
		false     | "release"     | Type.iOS  | PathHelper.IPHONE_OS
		true      | "debug"       | Type.iOS  | PathHelper.IPHONE_SIMULATOR
		true      | "release"     | Type.iOS  | PathHelper.IPHONE_SIMULATOR
		false     | "debug"       | Type.tvOS | PathHelper.APPLE_TV_OS
		false     | "release"     | Type.tvOS | PathHelper.APPLE_TV_OS
		true      | "debug"       | Type.tvOS | PathHelper.APPLE_TV_SIMULATOR
		true      | "release"     | Type.tvOS | PathHelper.APPLE_TV_SIMULATOR
	}

	def "Osx symroot path resolution"() {
		setup:
		File tempSymRoot = File.createTempDir()

		expect:
		File file = PathHelper.resolveMacOsSymRoot(tempSymRoot,
				configuration)

		file == new File(tempSymRoot, configuration)

		where:
		configuration | outputPath
		"debug"       | _
		"release"     | _
	}
}
