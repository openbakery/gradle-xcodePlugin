package org.openbakery.appcenter

import groovy.json.JsonBuilder
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.appcenter.models.FinishUploadResponse
import org.openbakery.appcenter.models.InitIpaUploadResponse
import org.openbakery.appcenter.models.ReleaseUploadMetadataResponse
import org.openbakery.appcenter.models.UploadChunkResponse
import org.openbakery.appcenter.models.UploadReleaseResponse
import org.openbakery.http.HttpUtil
import spock.lang.Specification

import java.util.zip.ZipFile

class AppCenterTaskSpecification extends Specification {
	Project project
	AppCenterUploadTask appCenterUploadTask

	CommandRunner commandRunner = Mock(CommandRunner)
	HttpUtil httpUtil = Mock(HttpUtil)

	File infoPlist

	InitIpaUploadResponse initResponse
	ReleaseUploadMetadataResponse metadataResponse
	UploadChunkResponse chunkResponse
	FinishUploadResponse finishResponse
	UploadReleaseResponse releaseResponse

	def setup() {
		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productType = 'app'
		project.xcodebuild.productName = 'Test'

		appCenterUploadTask = project.getTasks().getByName(XcodePlugin.APPCENTER_IPA_UPLOAD_TASK_NAME)

		appCenterUploadTask.commandRunner = commandRunner
		appCenterUploadTask.httpUtil = httpUtil

		File ipaBundle = new File(project.getBuildDir(), "package/Test.ipa")
		FileUtils.writeByteArrayToFile(ipaBundle, [0] * 10 as byte[])

		File archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Test.xcarchive")
		archiveDirectory.mkdirs()

		infoPlist = new File(archiveDirectory, "Products/Applications/Test.app/Info.plist");
		infoPlist.parentFile.mkdirs();

		initResponse = new InitIpaUploadResponse()
		initResponse.id = "1"
		initResponse.upload_domain = "mock://init.co"
		initResponse.package_asset_id = "11"
		initResponse.token = "initToken"

		metadataResponse = new ReleaseUploadMetadataResponse()
		metadataResponse.chunk_size = 5

		chunkResponse = new UploadChunkResponse()
		chunkResponse.error = false

		finishResponse = new FinishUploadResponse()
		finishResponse.error = false

		releaseResponse = new UploadReleaseResponse()
		releaseResponse.upload_status = "readyToBePublished"
		releaseResponse.release_distinct_id = "2"
	}


	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	def jsonString(Object object) {
		new JsonBuilder(object).toPrettyString()
	}

	def "upload"() {
		given:
		project.appcenter.apiToken = "123"

		when:
		appCenterUploadTask.upload()

		then:
		File expectedIpa = new File(project.buildDir, "appcenter/Test.ipa")
		expectedIpa.exists()

		3 * httpUtil.sendJson(HttpUtil.HttpVerb.POST, _, _, _, _) >>> [
			jsonString(initResponse),
			jsonString(metadataResponse),
			jsonString(finishResponse)
		]
		2 * httpUtil.sendFile(HttpUtil.HttpVerb.POST, _, _, _, _, _) >> jsonString(chunkResponse)
		2 * httpUtil.sendJson(HttpUtil.HttpVerb.PATCH, _, _, _, _) >> jsonString([test:"test"])
		2 * httpUtil.getJson(_, _, _) >>> [
			jsonString(releaseResponse),
			jsonString([test: "test"])
		]
	}

	def "upload with bundleSuffix"() {
		given:
		project.appcenter.apiToken = "123"
		project.xcodebuild.bundleNameSuffix = '-SUFFIX'
		httpUtil.sendJson(HttpUtil.HttpVerb.POST, _, _, _, _) >>> [
			jsonString(initResponse),
			jsonString(metadataResponse),
			jsonString(finishResponse)
		]
		httpUtil.sendFile(HttpUtil.HttpVerb.POST, _, _, _, _, _) >> jsonString(chunkResponse)
		httpUtil.sendJson(HttpUtil.HttpVerb.PATCH, _, _, _, _) >> jsonString([test:"test"])
		httpUtil.getJson(_, _, _) >>> [
			jsonString(releaseResponse),
			jsonString([test: "test"])
		]

		when:
		appCenterUploadTask.upload()

		then:
		File expectedIpa = new File(project.buildDir, "appcenter/Test-SUFFIX.ipa")
		expectedIpa.exists()
	}
}
