package org.openbakery.appcenter

import groovy.json.JsonBuilder
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.appcenter.models.CommitResponse
import org.openbakery.appcenter.models.InitDebugSymbolResponse
import org.openbakery.appcenter.models.InitIpaUploadResponse
import org.openbakery.http.HttpUtil
import spock.lang.Specification

class AppCenterTaskSpecification extends Specification {
	Project project
	AppCenterUploadTask appCenterUploadTask

	CommandRunner commandRunner = Mock(CommandRunner)
	HttpUtil httpUtil = Mock(HttpUtil)

	File infoPlist

	def setup() {
		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productType = 'app'
		project.xcodebuild.productName = 'Test'

		appCenterUploadTask = project.getTasks().getByPath('appcenter')

		appCenterUploadTask.commandRunner = commandRunner
		appCenterUploadTask.httpUtil = httpUtil


		File ipaBundle = new File(project.getBuildDir(), "package/Test.ipa")
		FileUtils.writeStringToFile(ipaBundle, "dummy")

		File archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Test.xcarchive")
		archiveDirectory.mkdirs()

		infoPlist = new File(archiveDirectory, "Products/Applications/Test.app/Info.plist");
		infoPlist.parentFile.mkdirs();

		File dsymBundle = new File(archiveDirectory, "dSYMs/Test.app.dSYM")
		FileUtils.writeStringToFile(dsymBundle, "dummy")
	}


	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	def "archive"() {
		when:
		appCenterUploadTask.prepareFiles()

		File expectedIpa = new File(project.buildDir, "appcenter/Test.ipa")
		File expectedDSYM = new File(project.buildDir, "appcenter/Test.app.dSYM.zip")

		then:
		expectedIpa.exists()
		expectedDSYM.exists()
	}

	def "archive with bundleSuffix"() {
		given:
		project.xcodebuild.bundleNameSuffix = '-SUFFIX'

		when:
		appCenterUploadTask.prepareFiles()

		File expectedIpa = new File(project.buildDir, "appcenter/Test-SUFFIX.ipa")
		File expectedZip = new File(project.buildDir, "appcenter/Test-SUFFIX.app.dSYM.zip")

		then:
		expectedIpa.exists()
		expectedZip.exists()

	}

	def "init IPA Upload"() {
		setup:
		final String expectedUploadId = "1"
		final String expectedUploadUrl = "https://www.someurl.com/initipaupload"

		def initIpaUploadResponse = new InitIpaUploadResponse()
		initIpaUploadResponse.upload_id = expectedUploadId
		initIpaUploadResponse.upload_url = expectedUploadUrl

		String json = new JsonBuilder(initIpaUploadResponse).toPrettyString()

		httpUtil.sendJson(HttpUtil.HttpVerb.POST, _, _, _) >> json

		String uploadId
		String uploadUrl

		when:
		(uploadId, uploadUrl) = appCenterUploadTask.initIpaUpload()

		then:
		uploadId == expectedUploadId
		uploadUrl == expectedUploadUrl
	}

	def "commit Upload"() {
		setup:
		final String expectedReleaseId = "2"
		final String expectedReleaseUrl = "https://www.someurl.com/commitUpload"

		def commitResponse = new CommitResponse()
		commitResponse.release_id = expectedReleaseId
		commitResponse.release_url = expectedReleaseUrl

		String json = new JsonBuilder(commitResponse).toPrettyString()

		httpUtil.sendJson(HttpUtil.HttpVerb.PATCH, _, _, _) >> json

		String releaseUrl

		when:
		releaseUrl = appCenterUploadTask.commitUpload("", "")

		then:
		releaseUrl == expectedReleaseUrl
	}

	def "init Debug Symbol Upload"() {
		setup:
		final String expectedUploadId = "3"
		final String expectedUploadUrl = "https://www.someurl.com/initdebugupload"

		def initDebugSymbolResponse = new InitDebugSymbolResponse()
		initDebugSymbolResponse.symbol_upload_id = expectedUploadId
		initDebugSymbolResponse.upload_url = expectedUploadUrl

		String json = new JsonBuilder(initDebugSymbolResponse).toPrettyString()

		httpUtil.sendJson(HttpUtil.HttpVerb.POST, _, _, _) >> json

		String uploadId
		String uploadUrl

		when:
		(uploadId, uploadUrl) = appCenterUploadTask.initDebugSymbolUpload()

		then:
		uploadId == expectedUploadId
		uploadUrl == expectedUploadUrl
	}
}
