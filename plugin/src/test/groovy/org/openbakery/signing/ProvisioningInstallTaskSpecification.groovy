package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.XcodePlugin
import spock.lang.Specification


class ProvisioningInstallTaskSpecification extends Specification {

	Project project
	ProvisioningInstallTask provisioningInstallTask;

	CommandRunner commandRunner = Mock(CommandRunner)

	File provisionLibraryPath
	File projectDir


	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		project.xcodebuild.simulator = false

		provisioningInstallTask = project.getTasks().getByPath(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)

		provisioningInstallTask.commandRunner = commandRunner

		provisionLibraryPath = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/");

	}

	def cleanup() {
		FileUtils.deleteDirectory(projectDir)
	}


	def "single ProvisioningProfile"() {

		File testMobileprovision = new File("../libtest/src/main/Resource/test.mobileprovision")
		project.xcodebuild.signing.mobileProvisionURI = testMobileprovision.toURI().toString()

		ProvisioningProfileReader provisioningProfileIdReader = new ProvisioningProfileReader(testMobileprovision, commandRunner)
		String uuid = provisioningProfileIdReader.getUUID()
		String name =  "gradle-" + uuid + ".mobileprovision";

		File source = new File(projectDir, "build/provision/" + name)
		File destination = new File(provisionLibraryPath, name)

		File sourceFile = new File(projectDir, "build/provision/" + name)

		when:
		provisioningInstallTask.install()

		then:
		sourceFile.exists()
		1 * commandRunner.run(["/bin/ln", "-s", source.absolutePath , destination.absolutePath])

	}

	def "multiple ProvisioningProfiles"() {

		File firstMobileprovision = new File("../libtest/src/main/Resource/test.mobileprovision")
		File secondMobileprovision = new File("src/test/Resource/test1.mobileprovision")
		project.xcodebuild.signing.mobileProvisionURI = [firstMobileprovision.toURI().toString(), secondMobileprovision.toURI().toString() ]

		String firstName = "gradle-" + new ProvisioningProfileReader(firstMobileprovision, new CommandRunner()).getUUID() + ".mobileprovision";
		String secondName = "gradle-" + new ProvisioningProfileReader(secondMobileprovision, new CommandRunner()).getUUID() + ".mobileprovision";

		File firstSource = new File(projectDir, "build/provision/" + firstName)
		File firstDestination = new File(provisionLibraryPath, firstName)

		File secondSource = new File(projectDir, "build/provision/" + secondName)
		File secondDestination = new File(provisionLibraryPath, secondName)

		File firstFile = new File(projectDir, "build/provision/" + firstName)
		File secondFile = new File(projectDir, "build/provision/" + secondName)

		when:
			provisioningInstallTask.install()


		then:
		firstFile.exists()
		secondFile.exists()
		1 * commandRunner.run(["/bin/ln", "-s", firstSource.absolutePath, firstDestination.absolutePath])
		1 * commandRunner.run(["/bin/ln", "-s", secondSource.absolutePath, secondDestination.absolutePath])
	}


	def "mobileProvisionFile has mobileprovision extension"() {
		given:
		File testMobileprovision = new File("../libtest/src/main/Resource/test.mobileprovision")
		project.xcodebuild.signing.mobileProvisionURI = testMobileprovision.toURI().toString()

		ProvisioningProfileReader provisioningProfileIdReader = new ProvisioningProfileReader(testMobileprovision, commandRunner)
		String uuid = provisioningProfileIdReader.getUUID()

		when:
		provisioningInstallTask.install()

		then:
		project.xcodebuild.signing.mobileProvisionFile.size == 1
		project.xcodebuild.signing.mobileProvisionFile[0].toString().endsWith(uuid + ".mobileprovision")
	}

	def "has provisionprofile extension"() {
		given:
		File testMobileprovision = new File("../plugin/src/test/Resource/test-wildcard-mac.provisionprofile")
		project.xcodebuild.signing.mobileProvisionURI = testMobileprovision.toURI().toString()

		ProvisioningProfileReader provisioningProfileIdReader = new ProvisioningProfileReader(testMobileprovision, commandRunner)
		String uuid = provisioningProfileIdReader.getUUID()

		when:
		provisioningInstallTask.install()

		then:
		project.xcodebuild.signing.mobileProvisionFile.size == 1
		project.xcodebuild.signing.mobileProvisionFile[0].toString().endsWith(uuid + ".provisionprofile")
	}
}
