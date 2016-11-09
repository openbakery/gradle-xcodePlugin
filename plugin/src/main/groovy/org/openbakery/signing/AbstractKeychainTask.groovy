package org.openbakery.signing

import org.apache.commons.lang.StringUtils
import org.openbakery.AbstractXcodeTask
import org.openbakery.XcodeBuildPluginExtension

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 23.08.13
 * Time: 11:39
 * To change this template use File | Settings | File Templates.
 */
abstract class AbstractKeychainTask extends AbstractXcodeTask {

    static alreadyResolvedLoginKC_path;
    public static final String COMMON_SYSTEM_KC_NAME = '/Library/Keychains/login.keychain'

    public static String loginKeychainPath(){

        if (! alreadyResolvedLoginKC_path){
            String userHome = System.getProperty("user.home")
            String loginKeychain = userHome + COMMON_SYSTEM_KC_NAME
            File commonogin_kc = new File(loginKeychain)

            if (! commonogin_kc.exists()){
                // on my MacOSX, keychain has this bizarre extension. There is no way to rename it.
                alreadyResolvedLoginKC_path = loginKeychain + '-db'
            }else{
                alreadyResolvedLoginKC_path = loginKeychain
            }
        }


        return alreadyResolvedLoginKC_path
    }


	List<String> getKeychainList() {
		String keychainList = commandRunner.runWithResult(["security", "list-keychains"])
		List<String> result = []

		for (String keychain in keychainList.split("\n")) {
			String trimmedKeychain = keychain.replaceAll(/^\s*\"|\"$/, "")
			if (!trimmedKeychain.equals("/Library/Keychains/System.keychain")) {

				File keychainFile = new File(trimmedKeychain)
				if (keychainFile.exists()) {
					result.add(trimmedKeychain);
				}
			}
		}
		return result;
	}

	def setKeychainList(keychainList) {
		def commandList = [
						"security",
						"list-keychains",
						"-s"
		]
		for (String keychain in keychainList) {
			commandList.add(keychain);
		}
		commandRunner.run(commandList)
	}

	/**
	 * remove all gradle keychains from the keychain search list
	 * @return
	 */
	def removeGradleKeychainsFromSearchList() {
		List<String> keychainList = getKeychainList();
        logger.debug('getKeychains returned : {}', keychainList)
        Signing signing = project.xcodebuild.signing

        if (signing.keychainPathInternal != null) {
			keychainList.remove(signing.keychainPathInternal.absolutePath)
		}

		setKeychainList(keychainList)
	}
}
