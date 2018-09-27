package org.openbakery.test

import org.openbakery.testdouble.SimulatorControlFake
import org.openbakery.xcode.Destination
import org.openbakery.xcode.DestinationResolver
import org.openbakery.xcode.Type
import org.openbakery.xcode.XcodebuildParameters
import spock.lang.Specification

class TestResultParser_Xcode10_Specification extends Specification {

	TestResultParser testResultParser
	File outputDirectory

	def setup() {

		outputDirectory = new File(System.getProperty("java.io.tmpdir"), 'gradle-xcodebuild/outputDirectory').absoluteFile
		outputDirectory.mkdirs()

		File manifest = new File("src/Test/Resource/Logs/Test/LogStoreManifest.plist")
		testResultParser = new TestResultParser(manifest)
	}


	def "parse with no result"() {
		when:
		testResultParser.store(outputDirectory)
		then:
		true // no exception should be raised
	}

	/*
	def "parse success result"() {
		when:
		def result = testResultParser.parseResult(new File("../plugin/src/test/Resource/xcodebuild-output.txt"))

		then:
		testResultParser.numberSuccess(result) == 2
		testResultParser.numberErrors(result) == 0
	}
	*/
}
