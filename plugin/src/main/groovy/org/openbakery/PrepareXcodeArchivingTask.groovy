package org.openbakery

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.util.PathHelper

@CompileStatic
class PrepareXcodeArchivingTask extends AbstractXcodeBuildTask {

    private ProvisioningProfileReader reader

    public static final String NAME = "prepareArchiving"

    private static final String FILE_NAME = "archive.xcconfig"
    private static final String KEY_BUNDLE_IDENTIFIER = "PRODUCT_BUNDLE_IDENTIFIER"
    private static final String KEY_CODE_SIGN_IDENTITY = "CODE_SIGN_IDENTITY"
    private static final String KEY_DEVELOPMENT_TEAM = "DEVELOPMENT_TEAM"
    private static final String KEY_PROVISIONING_PROFILE_ID = "PROVISIONING_PROFILE"
    private static final String KEY_PROVISIONING_PROFILE_SPEC = "PROVISIONING_PROFILE_SPECIFIER"

    PrepareXcodeArchivingTask() {
        super()
        dependsOn(XcodePlugin.XCODE_CONFIG_TASK_NAME)
        dependsOn(XcodePlugin.INFOPLIST_MODIFY_TASK_NAME)

        this.description = "Prepare the archive configuration file"
    }

    @Input
    List<String> getProvisioningUriList() {
        return getXcodeExtension().signing.mobileProvisionURI
    }

    @Input
    String getBundleIdentifier() {
        File plistFile = new File(project.projectDir, getXcodeExtension().getInfoPlist())
        return plistHelper.getValueFromPlist(plistFile, "CFBundleIdentifier")
    }

    Optional<File> getProvisioningFile() {
        List<File> provisioningList = getProvisioningUriList()
                .collect { it -> new File(new URI(it)) }

        return Optional.ofNullable(ProvisioningProfileReader.getProvisionFileForIdentifier(bundleIdentifier,
                provisioningList,
                commandRunner,
                plistHelper))
    }

    @OutputFile
    File getXcConfigFile() {
        return new File(PathHelper.resolveArchiveFolder(project), FILE_NAME)
    }

    @TaskAction
    void generate() {
        getXcConfigFile().text = "//:configuration = GradleXcode"
        computeProvisioningFile(getProvisioningFile()
                .orElseThrow { new IllegalArgumentException() })
    }

    private void computeProvisioningFile(File file) {
        reader = new ProvisioningProfileReader(file, commandRunner)
        append(KEY_CODE_SIGN_IDENTITY, getCodeSignIdentity())
        append(KEY_DEVELOPMENT_TEAM, reader.getTeamIdentifierPrefix())
        append(KEY_BUNDLE_IDENTIFIER, reader.getApplicationIdentifier())
        append(KEY_PROVISIONING_PROFILE_ID, reader.getUUID())
        append(KEY_PROVISIONING_PROFILE_SPEC, reader.getName())
    }

    private String getCodeSignIdentity() {
        return "iPhone Distribution: " +
                reader.getTeamName() +
                " (" + reader.getTeamIdentifierPrefix() + ")"
    }

    private void append(String key, String value) {
        getXcConfigFile()
                .append(System.getProperty("line.separator") + key + " = " + value)
    }
}
