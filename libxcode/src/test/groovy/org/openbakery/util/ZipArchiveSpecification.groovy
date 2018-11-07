package org.openbakery.util

import org.openbakery.CommandRunner
import spock.lang.Specification

class ZipArchiveSpecification extends Specification {


	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {
	}

	def tearDown() {
		commandRunner = null
	}


	def "zip file"() {
		given:
		def commandList
		def file = new File("Test.txt")

		when:
		ZipArchive.archive(file, commandRunner)

		then:
		1 * commandRunner.run(_, _) >> {
			arguments ->
				commandList = arguments[1]
		}
		commandList == [
			"/usr/bin/zip",
			"--symlinks",
			"--recurse-paths",
			new File("Test.zip").absolutePath,
			"Test.txt"
		]
	}


	def "zip file with archive name"() {
		given:
		def commandList
		def file = new File("Test.txt")
		def archive = new File("archive.zip")

		when:

		def zipArchive = new ZipArchive(archive, commandRunner)
		zipArchive.add(file)
		zipArchive.create()

		then:
		1 * commandRunner.run(_, _) >> {
			arguments ->
				commandList = arguments[1]
		}
		commandList == [
			"/usr/bin/zip",
			"--symlinks",
			"--recurse-paths",
			archive.absolutePath,
			"Test.txt"
		]
	}


	def "zip file with base directory"() {
		given:
		def baseDirectory
		def file = new File("Test.txt")
		def archive = new File("archive.zip")

		when:
		def zipArchive = new ZipArchive(archive, new File("/tmp/base"), commandRunner)
		zipArchive.add(file)
		zipArchive.create()

		then:
		1 * commandRunner.run(_, _) >> {
			arguments ->
				baseDirectory = arguments[0]
		}
		baseDirectory == "/tmp/base"
	}



	def "zip multiple files"() {
		given:
		def archive = new File("archive.zip")
		def commandList

		when:
		def zipArchive = new ZipArchive(archive, commandRunner)
		zipArchive.add(new File("first.txt"))
		zipArchive.add(new File("second.txt"))
		zipArchive.create()


		then:
		1 * commandRunner.run(_, _) >> {
			arguments ->
				commandList = arguments[1]
		}
		commandList == [
			"/usr/bin/zip",
			"--symlinks",
			"--recurse-paths",
			archive.absolutePath,
			"first.txt",
			"second.txt"
		]
	}


	def "zip returns zip file"() {
		given:
		def file = new File("Test.txt")

		when:
		def zipFile = ZipArchive.archive(file, commandRunner)

		then:
		zipFile == new File("Test.zip")
	}

	def "zip file honours base directory"() {
		given:
		def commandList
		def file = new File("basedirectory/path/Test.txt")
		def archive = new File("archive.zip")

		when:
		def zipArchive = new ZipArchive(archive, new File("basedirectory"), commandRunner)
		zipArchive.add(file)
		zipArchive.create()

		then:
		1 * commandRunner.run(_, _) >> {
			arguments ->
				commandList = arguments[1]
		}
		commandList == [
			"/usr/bin/zip",
			"--symlinks",
			"--recurse-paths",
			archive.absolutePath,
			"path/Test.txt"
		]
	}

}
