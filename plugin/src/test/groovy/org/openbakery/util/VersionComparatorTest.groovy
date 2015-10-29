package org.openbakery.util

import spock.lang.Specification

/**
 * Created by rene on 29.10.15.
 */
class VersionComparatorTest extends Specification {


	VersionComparator comparator = new VersionComparator()


	def "compare"() {
		expect:
		comparator.compare("1", "1") == 0
		comparator.compare("1", "2") == -1
		comparator.compare("2", "1") == 1
		comparator.compare("1.1", "1.2") == -1
		comparator.compare("1.2", "1.1") == 1
		comparator.compare("1.0.1", "1.0.2") == -1
		comparator.compare("1.0.2", "1.0.1") == 1
		comparator.compare("1.1.1", "1.1.1") == 0
		comparator.compare("1.a", "1.b") == -1
		comparator.compare("1.a", "1.a") == 0
		comparator.compare("1.c", "1.b") == 1

		comparator.compare("1", "1.b") == -1
		comparator.compare("1.b", "1") == 1


	}
}
