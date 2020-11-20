package org.openbakery.xcode

import jdk.jfr.events.FileReadEvent
import org.apache.commons.io.FileUtils
import spock.lang.Specification

class XCConfigSpecification extends Specification {

	File temporaryDirectory
	File xcconfigFile
	XCConfig xcConfig

	def setup() {
		temporaryDirectory = new File(System.getProperty("java.io.tmpdir"), "gxp-test")
		xcconfigFile = new File(temporaryDirectory, "My.xcconfig")
		xcConfig = new XCConfig(xcconfigFile)
	}

	def cleanup() {
		xcconfigFile.delete()
		temporaryDirectory.delete()
		temporaryDirectory = null
		xcconfigFile = null
		xcConfig = null
	}



	def "create creates XCConfig file"() {
		when:
		xcConfig.create()

		then:
		xcconfigFile.exists()
	}

	List<String> readContents() {
		String result = FileUtils.readFileToString(xcconfigFile)
		return result.split("\n")
	}

	def "add parameter to xconfig and create, write the data to the file"() {
		given:
		xcConfig.set("KEY", "VALUE")

		when:
		xcConfig.create()

		then:
		def contents = readContents()
		contents.size() == 1
		contents.get(0) == "KEY = VALUE"
	}

	def "add two parameters ot xcconfig and create, write the data to the file"() {
		given:
		xcConfig.set("one", "1")
		xcConfig.set("two", "2")

		when:
		xcConfig.create()

		then:
		def contents = readContents()
		contents.size() == 2
		contents.get(0) == "one = 1"
		contents.get(1) == "two = 2"
	}


	def "open xcconfig parses the entries"() {
		given:
		FileUtils.writeStringToFile(xcconfigFile, "first = 1\nsecond = 2\nthird = 3")

		when:
		def xcConfig = new XCConfig(xcconfigFile)

		then:
		xcConfig.entries != null
		xcConfig.entries.size() == 3
		xcConfig.entries["first"] == "1"
		xcConfig.entries["second"] == "2"
		xcConfig.entries["third"] == "3"
	}


}
