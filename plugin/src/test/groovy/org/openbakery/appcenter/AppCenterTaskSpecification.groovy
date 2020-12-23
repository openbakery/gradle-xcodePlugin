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

import java.util.zip.ZipFile

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

		File appDsym = new File(archiveDirectory, "dSYMs/Test.app.dSYM")
		FileUtils.writeStringToFile(appDsym, "dummy")

		File frameworkDsym = new File(archiveDirectory, "dSYMs/framework.dSYM")
		FileUtils.writeStringToFile(frameworkDsym, "dummy")
	}


	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	def "timeout"() {
		when:
		appCenterUploadTask.readTimeout(150)

		then:
		appCenterUploadTask.httpUtil.readTimeoutInSeconds == 150
	}

	def "archive"() {
		when:
		appCenterUploadTask.prepareFiles()

		File expectedIpa = new File(project.buildDir, "appcenter/Test.ipa")
		File expectedDSYMZip = new File(project.buildDir, "appcenter/Test.app.dSYM.zip")
		ZipFile dsymZipFile = new ZipFile(expectedDSYMZip)

		then:
		expectedIpa.exists()
		expectedDSYMZip.exists()
		dsymZipFile.entries().toList().size() == 2
	}

	def "archive with bundleSuffix"() {
		given:
		project.xcodebuild.bundleNameSuffix = '-SUFFIX'

		when:
		appCenterUploadTask.prepareFiles()

		File expectedIpa = new File(project.buildDir, "appcenter/Test-SUFFIX.ipa")
		File expectedDSYMZip = new File(project.buildDir, "appcenter/Test-SUFFIX.app.dSYM.zip")
		ZipFile dsymZipFile = new ZipFile(expectedDSYMZip)

		then:
		expectedIpa.exists()
		expectedDSYMZip.exists()
		dsymZipFile.entries().toList().size() == 2
	}

	def "init IPA Upload"() {
		setup:
		project.appcenter.apiToken = "123"

		final String expectedId = "1"
		final String expectedDomain = "https://www.someurl.com/initipaupload"
		final String expectedToken = "2"
  	final String expectedPackageAssetId = "3"

		def initIpaUploadResponse = new InitIpaUploadResponse()
		initIpaUploadResponse.id = expectedId
		initIpaUploadResponse.upload_domain = expectedDomain
		initIpaUploadResponse.package_asset_id = expectedPackageAssetId
		initIpaUploadResponse.token = expectedToken

		String json = new JsonBuilder(initIpaUploadResponse).toPrettyString()

		httpUtil.sendJson(HttpUtil.HttpVerb.POST, _, _, _, _) >> json

		String id
		String domain
		String token
		String packageAssetId

		when:
		(id, domain, packageAssetId, token) = appCenterUploadTask.initIpaUpload()

		then:
		id == expectedId
		domain == expectedDomain
		token == expectedToken
		packageAssetId == expectedPackageAssetId
	}

	def "init IPA Upload with unknown json fields"() {
		setup:
		project.appcenter.apiToken = "123"

		final String expectedId = "1"
		final String expectedDomain = "https://www.someurl.com/initipaupload"
		final String expectedToken = "2"
		final String expectedPackageAssetId = "3"

		JsonBuilder builder = new JsonBuilder()
		builder {
			id expectedId
			upload_domain expectedDomain
			package_asset_id expectedPackageAssetId
			token expectedToken
			unknownField 'Test'
		}

		httpUtil.sendJson(HttpUtil.HttpVerb.POST, _, _, _, _) >> builder.toString()

		String id
		String domain
		String token
		String packageAssetId

		when:
		(id, domain, packageAssetId, token) = appCenterUploadTask.initIpaUpload()

		then:
		id == expectedId
		domain == expectedDomain
		token == expectedToken
		packageAssetId == expectedPackageAssetId
	}

	def "commit Upload"() {
		setup:
		final String expectedReleaseId = "2"
		final String expectedReleaseUrl = "https://www.someurl.com/commitUpload"

		def commitResponse = new CommitResponse()
		commitResponse.release_id = expectedReleaseId
		commitResponse.release_url = expectedReleaseUrl

		String json = new JsonBuilder(commitResponse).toPrettyString()

		httpUtil.sendJson(HttpUtil.HttpVerb.PATCH, _, _, _, _) >> json

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

		httpUtil.sendJson(HttpUtil.HttpVerb.POST, _, _, _, _) >> json

		String uploadId
		String uploadUrl

		when:
		(uploadId, uploadUrl) = appCenterUploadTask.initDebugSymbolUpload()

		then:
		uploadId == expectedUploadId
		uploadUrl == expectedUploadUrl
	}
}
