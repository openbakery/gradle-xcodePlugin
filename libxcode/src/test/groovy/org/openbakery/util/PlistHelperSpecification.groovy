package org.openbakery.util

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import spock.lang.Specification

class PlistHelperSpecification extends Specification {


	CommandRunner commandRunner = Mock(CommandRunner)
	PlistHelper plistHelper
	File plist

	def setup() {
		plistHelper = new PlistHelper(commandRunner)

		plist = new File(System.getProperty("java.io.tmpdir"), "test.plist")
		FileUtils.writeStringToFile(plist, "")
	}

	def cleanup() {
		plist.delete()
	}

	def "plistHelper addValueForPlist for String"() {
		when:
		plistHelper.addValueForPlist(plist, "key", "value")

		then:
		1 * commandRunner.run([
						"/usr/libexec/PlistBuddy",
						plist.absolutePath,
						"-c",
						"Add :key string value"
		])
	}

	def "plistHelper addValueForPlist for integer"() {
		when:
		plistHelper.addValueForPlist(plist, "key", 5)

		then:
		1 * commandRunner.run([
						"/usr/libexec/PlistBuddy",
						plist.absolutePath,
						"-c",
						"Add :key integer 5"
		])
	}

	def "plistHelper addValueForPlist for boolean"() {
		when:
		plistHelper.addValueForPlist(plist, "key", true)

		then:
		1 * commandRunner.run([
						"/usr/libexec/PlistBuddy",
						plist.absolutePath,
						"-c",
						"Add :key bool true"
		])
	}

	def "plistHelper addValueForPlist for boolean false"() {
		when:
		plistHelper.addValueForPlist(plist, "key", false)

		then:
		1 * commandRunner.run([
						"/usr/libexec/PlistBuddy",
						plist.absolutePath,
						"-c",
						"Add :key bool false"
		])
	}


	def "plistHelper addValueForPlist add float point number value"() {
		when:
		plistHelper.addValueForPlist(plist, "key", 1.2)

		then:
		1 * commandRunner.run([
						"/usr/libexec/PlistBuddy",
						plist.absolutePath,
						"-c",
						"Add :key real 1.2"
		])
	}
}
