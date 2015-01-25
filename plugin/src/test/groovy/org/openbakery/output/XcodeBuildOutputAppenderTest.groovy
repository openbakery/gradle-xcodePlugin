package org.openbakery.output


import org.testng.annotations.Test

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 02.10.13
 * Time: 11:15
 * To change this template use File | Settings | File Templates.
 */
class XcodeBuildOutputAppenderTest {

	def data = "CompileC build/obj/FOO.build/Debug-iphonesimulator/FOO-DMS.build/Objects-normal/i386/ClipboardListViewController.o FOO-iPad/Source/View\\ Controllers/ClipboardListViewController.m normal i386 objective-c com.apple.compilers.llvm.clang.1_0.compiler\n" +
					"    cd /Users/dummy/workspace/FOO/bar-ios\n" +
					"    setenv LANG en_US.US-ASCII\n" +
					"    setenv PATH \"/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/usr/bin:/Applications/Xcode.app/Contents/Developer/usr/bin:/usr/local/Cellar/ruby/1.9.3-p125/bin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/usr/local/git/bin:/usr/local/git/bin:/Users/rene/Java/gradle/bin:/Applications/Xcode.app/Contents/Developer/usr/bin/:/Users/rene/.rvm/bin\"\n" +
					"     /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/clang -x objective-c -arch i386 -fmessage-length=0 -fdiagnostics-show-note-include-stack -fmacro-backtrace-limit=0 -std=gnu99 -fobjc-arc -Wno-trigraphs -fpascal-strings -O0 -Werror -Wno-missing-field-initializers -Wmissing-prototypes -Wno-implicit-atomic-properties -Wno-receiver-is-weak -Wno-arc-repeated-use-of-weak -Wno-missing-braces -Wparentheses -Wswitch -Wno-unused-function -Wno-unused-label -Wno-unused-parameter -Wunused-variable -Wunused-value -Wno-empty-body -Wno-uninitialized -Wno-unknown-pragmas -Wno-shadow -Wno-four-char-constants -Wno-conversion -Wno-constant-conversion -Wno-int-conversion -Wno-bool-conversion -Wno-enum-conversion -Wno-shorten-64-to-32 -Wpointer-sign -Wno-newline-eof -Wno-selector -Wno-strict-selector-match -Wundeclared-selector -Wno-deprecated-implementations -DDEBUG=1 -DCOCOAPODS=1 -isysroot /Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.0.sdk -fexceptions -fasm-blocks -fstrict-aliasing -fprofile-arcs -ftest-coverage -Wprotocol -Wdeprecated-declarations -g -Wno-sign-conversion -fobjc-abi-version=2 -fobjc-legacy-dispatch -mios-simulator-version-min=5.0 -iquote /Users/dummy/workspace/FOO/bar-ios/build/obj/FOO.build/Debug-iphonesimulator/FOO-DMS.build/FOO-DMS-generated-files.hmap -I/Users/dummy/workspace/FOO/bar-ios/build/obj/FOO.build/Debug-iphonesimulator/FOO-DMS.build/FOO-DMS-own-target-headers.hmap -I/Users/dummy/workspace/FOO/bar-ios/build/obj/FOO.build/Debug-iphonesimulator/FOO-DMS.build/FOO-DMS-all-target-headers.hmap -iquote /Users/dummy/workspace/FOO/bar-ios/build/obj/FOO.build/Debug-iphonesimulator/FOO-DMS.build/FOO-DMS-project-headers.hmap -I/Users/dummy/workspace/FOO/bar-ios/build/sym/Debug-iphonesimulator/include -I/Users/dummy/workspace/FOO/bar-ios/Pods/Headers -I/Users/dummy/workspace/FOO/bar-ios/Pods/Headers/CocoaLumberjack -I/Users/dummy/workspace/FOO/bar-ios/Pods/Headers/DTDownload -I/Users/dummy/workspace/FOO/bar-ios/Pods/Headers/DTFoundation -I/Users/dummy/workspace/FOO/bar-ios/Pods/Headers/OCHamcrest -I/Users/dummy/workspace/FOO/bar-ios/Pods/Headers/OCMockito -I/Users/dummy/workspace/FOO/bar-ios/build/obj/FOO.build/Debug-iphonesimulator/FOO-DMS.build/DerivedSources/i386 -I/Users/dummy/workspace/FOO/bar-ios/build/obj/FOO.build/Debug-iphonesimulator/FOO-DMS.build/DerivedSources -F/Users/dummy/workspace/FOO/bar-ios/build/sym/Debug-iphonesimulator -include /Users/dummy/workspace/FOO/bar-ios/build/shared/FOO-iPad-Prefix-dbsijklqfrjkmqhbrolekcsnxuiq/FOO-iPad-Prefix.pch -MMD -MT dependencies -MF /Users/dummy/workspace/FOO/bar-ios/build/obj/FOO.build/Debug-iphonesimulator/FOO-DMS.build/Objects-normal/i386/ClipboardListViewController.d --serialize-diagnostics /Users/dummy/workspace/FOO/bar-ios/build/obj/FOO.build/Debug-iphonesimulator/FOO-DMS.build/Objects-normal/i386/ClipboardListViewController.dia -c /Users/dummy/workspace/FOO/bar-ios/FOO-iPad/Source/View\\ Controllers/ClipboardListViewController.m -o /Users/dummy/workspace/FOO/bar-ios/build/obj/FOO.build/Debug-iphonesimulator/FOO-DMS.build/Objects-normal/i386/ClipboardListViewController.o\n" +
					"\n" +
					"ProcessPCH /Users/dummy/workspace/FOO/bar-ios/build/shared/Pods-CocoaLumberjack-prefix-crkolbbjqcaxufaqpbbmhpjgletu/Pods-CocoaLumberjack-prefix.pch.pch Pods-CocoaLumberjack-prefix.pch normal i386 objective-c com.apple.compilers.llvm.clang.1_0.compiler"


	@Test
	void testCompile() {
		StyledTextOutputStub output = new StyledTextOutputStub()

		XcodeBuildOutputAppender appender =  new XcodeBuildOutputAppender(output)

		for (String line in data.split("\n")) {
			appender.append(line)
		}

		String expected = "      OK - Compile: FOO-iPad/Source/View\\ Controllers/ClipboardListViewController.m\n";
		assert output.toString().equals(expected) :  "Expected: " + expected  + " but was " + output.toString()

	}


	def errorData = "CompileC /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/FOO-DMS.build/Objects-normal/i386/UIService.o Core/Source/Services/UIService.m normal i386 objective-c com.apple.compilers.llvm.clang.1_0.compiler\n" +
					"    cd /Users/dummy/workspace/FOO/bar-ios\n" +
					"    setenv LANG en_US.US-ASCII\n" +
					"    setenv PATH \"/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/usr/bin:/Applications/Xcode.app/Contents/Developer/usr/bin:/usr/local/Cellar/ruby/1.9.3-p125/bin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/usr/local/git/bin:/usr/local/git/bin:/Users/dummy/Java/gradle/bin:/Applications/Xcode.app/Contents/Developer/usr/bin/:/Users/dummy/.rvm/bin\"\n" +
					"    /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/clang -x objective-c -arch i386 -fmessage-length=254 -fdiagnostics-show-note-include-stack -fmacro-backtrace-limit=0 -fcolor-diagnostics -std=gnu99 -fobjc-arc -Wno-trigraphs -fpascal-strings -O0 -Werror -Wno-missing-field-initializers -Wmissing-prototypes -Wno-implicit-atomic-properties -Wno-receiver-is-weak -Wno-arc-repeated-use-of-weak -Wno-missing-braces -Wparentheses -Wswitch -Wno-unused-function -Wno-unused-label -Wno-unused-parameter -Wunused-variable -Wunused-value -Wno-empty-body -Wno-uninitialized -Wno-unknown-pragmas -Wno-shadow -Wno-four-char-constants -Wno-conversion -Wno-constant-conversion -Wno-int-conversion -Wno-bool-conversion -Wno-enum-conversion -Wno-shorten-64-to-32 -Wpointer-sign -Wno-newline-eof -Wno-selector -Wno-strict-selector-match -Wundeclared-selector -Wno-deprecated-implementations -DDEBUG=1 -DCOCOAPODS=1 -isysroot /Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.0.sdk -fexceptions -fasm-blocks -fstrict-aliasing -fprofile-arcs -ftest-coverage -Wprotocol -Wdeprecated-declarations -g -Wno-sign-conversion -fobjc-abi-version=2 -fobjc-legacy-dispatch -mios-simulator-version-min=5.0 -iquote /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/FOO-DMS.build/FOO-DMS-generated-files.hmap -I/Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/FOO-DMS.build/FOO-DMS-own-target-headers.hmap -I/Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/FOO-DMS.build/FOO-DMS-all-target-headers.hmap -iquote /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/FOO-DMS.build/FOO-DMS-project-headers.hmap -I/Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Products/Debug-iphonesimulator/include -I/Users/dummy/workspace/FOO/bar-ios/Pods/Headers -I/Users/dummy/workspace/FOO/bar-ios/Pods/Headers/CocoaLumberjack -I/Users/dummy/workspace/FOO/bar-ios/Pods/Headers/DTDownload -I/Users/dummy/workspace/FOO/bar-ios/Pods/Headers/DTFoundation -I/Users/dummy/workspace/FOO/bar-ios/Pods/Headers/OCHamcrest -I/Users/dummy/workspace/FOO/bar-ios/Pods/Headers/OCMockito -I/Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/FOO-DMS.build/DerivedSources/i386 -I/Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/FOO-DMS.build/DerivedSources -F/Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Products/Debug-iphonesimulator -include /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/PrecompiledHeaders/FOO-iPad-Prefix-cnhpnnzqjidzttbdtgesbtpehhsf/FOO-iPad-Prefix.pch -MMD -MT dependencies -MF /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/FOO-DMS.build/Objects-normal/i386/UIService.d --serialize-diagnostics /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/FOO-DMS.build/Objects-normal/i386/UIService.dia -c /Users/dummy/workspace/FOO/bar-ios/Core/Source/Services/UIService.m -o /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/FOO-DMS.build/Objects-normal/i386/UIService.o\n" +
					"/Users/dummy/workspace/FOO/bar-ios/Core/Source/Services/UIService.m:180:2: error: use of undeclared identifier 'asdf'\n" +
					"        asdf\n" +
					"        ^\n" +
					"/Users/dummy/workspace/FOO/bar-ios/Core/Source/Services/UIService.m:183:7: error: use of undeclared identifier 'cell'\n" +
					"                if (cell == nil) {\n" +
					"                    ^\n" +
					"/Users/dummy/workspace/FOO/bar-ios/Core/Source/Services/UIService.m:185:4: error: use of undeclared identifier 'cell'\n" +
					"                        cell = [nib objectAtIndex:0];\n" +
					"                        ^\n" +
					"/Users/dummy/workspace/FOO/bar-ios/Core/Source/Services/UIService.m:186:4: error: use of undeclared identifier 'cell'\n" +
					"                        cell.selectionStyle = UITableViewCellSelectionStyleNone;\n" +
					"                        ^\n" +
					"/Users/dummy/workspace/FOO/bar-ios/Core/Source/Services/UIService.m:189:7: error: use of undeclared identifier 'cell'\n" +
					"                if (cell == nil) {\n" +
					"                    ^\n" +
					"/Users/dummy/workspace/FOO/bar-ios/Core/Source/Services/UIService.m:190:4: error: use of undeclared identifier 'cell'\n" +
					"                        cell = [[TaskTableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:identifier];\n" +
					"                        ^\n" +
					"/Users/dummy/workspace/FOO/bar-ios/Core/Source/Services/UIService.m:193:2: error: use of undeclared identifier 'cell'\n" +
					"        cell.name.font = [self fontForHeadline];\n" +
					"        ^\n" +
					"/Users/dummy/workspace/FOO/bar-ios/Core/Source/Services/UIService.m:194:2: error: use of undeclared identifier 'cell'\n" +
					"        cell.accessoryType = UITableViewCellAccessoryNone;\n" +
					"        ^\n" +
					"/Users/dummy/workspace/FOO/bar-ios/Core/Source/Services/UIService.m:195:9: error: use of undeclared identifier 'cell'\n" +
					"        return cell;\n" +
					"               ^\n" +
					"9 errors generated.\n" +
					"\n" +
					"ProcessPCH /Users/dummy/workspace/FOO/bar-ios/build/shared/Pods-CocoaLumberjack-prefix-crkolbbjqcaxufaqpbbmhpjgletu/Pods-CocoaLumberjack-prefix.pch.pch Pods-CocoaLumberjack-prefix.pch normal i386 objective-c com.apple.compilers.llvm.clang.1_0.compiler"


	def warningData = "CompileC /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/UnitTests.build/Objects-normal/i386/MyDetailViewControllerTest_iPad.o UnitTests/iPad/ViewControllers/MyDetailViewControllerTest_iPad.m normal i386 objective-c com.apple.compilers.llvm.clang.1_0.compiler\n" +
					"    cd /Users/dummy/workspace/coconatics/FOO/bar-ios\n" +
					"    setenv LANG en_US.US-ASCII\n" +
					"    setenv PATH \"/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/usr/bin:/Applications/Xcode.app/Contents/Developer/usr/bin:/usr/local/Cellar/ruby/1.9.3-p125/bin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/usr/local/git/bin:/usr/local/git/bin:/Users/dummy/Java/gradle/bin:/Applications/Xcode.app/Contents/Developer/usr/bin/:/Users/dummy/.rvm/bin\"\n" +
					"    /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/clang -x objective-c -arch i386 -fmessage-length=316 -fdiagnostics-show-note-include-stack -fmacro-backtrace-limit=0 -fcolor-diagnostics -std=gnu99 -fobjc-arc -Wno-trigraphs -fpascal-strings -O0 -Wno-missing-field-initializers -Wmissing-prototypes -Wno-implicit-atomic-properties -Wno-receiver-is-weak -Wno-arc-repeated-use-of-weak -Wno-missing-braces -Wparentheses -Wswitch -Wno-unused-function -Wno-unused-label -Wno-unused-parameter -Wunused-variable -Wunused-value -Wno-empty-body -Wno-uninitialized -Wno-unknown-pragmas -Wno-shadow -Wno-four-char-constants -Wno-conversion -Wno-constant-conversion -Wno-int-conversion -Wno-bool-conversion -Wno-enum-conversion -Wno-shorten-64-to-32 -Wpointer-sign -Wno-newline-eof -Wno-selector -Wno-strict-selector-match -Wundeclared-selector -Wno-deprecated-implementations -DDEBUG=1 -DCOCOAPODS=1 -isysroot /Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.0.sdk -fexceptions -fasm-blocks -fstrict-aliasing -fprofile-arcs -Wprotocol -Wdeprecated-declarations -g -Wno-sign-conversion -fobjc-abi-version=2 -fobjc-legacy-dispatch -mios-simulator-version-min=5.0 -iquote /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/UnitTests.build/UnitTests-generated-files.hmap -I/Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/UnitTests.build/UnitTests-own-target-headers.hmap -I/Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/UnitTests.build/UnitTests-all-target-headers.hmap -iquote /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/UnitTests.build/UnitTests-project-headers.hmap -iquote/Users/dummy/workspace/coconatics/FOO/bar-ios/External/DTFoundation -iquotePods -iquotePods/BuildHeaders -iquotePods/CocoaLumberjack -iquotePods/DTDownload -iquotePods/DTFoundation -iquotePods/Headers -iquotePods/OCHamcrest -iquotePods/OCMockito -iquotePods/BuildHeaders/CocoaLumberjack -iquotePods/BuildHeaders/DTDownload -iquotePods/BuildHeaders/DTFoundation -iquotePods/BuildHeaders/OCHamcrest -iquotePods/BuildHeaders/OCMockito -iquotePods/CocoaLumberjack/Lumberjack -iquotePods/DTDownload/Core -iquotePods/DTFoundation/Core -iquotePods/Headers/CocoaLumberjack -iquotePods/Headers/DTDownload -iquotePods/Headers/DTFoundation -iquotePods/Headers/OCHamcrest -iquotePods/Headers/OCMockito -iquotePods/OCHamcrest/Source -iquotePods/OCMockito/Source -iquotePods/DTDownload/Core/Source -iquotePods/DTFoundation/Core/Source -iquotePods/OCHamcrest/Source/Core -iquotePods/OCHamcrest/Source/Library -iquotePods/OCMockito/Source/OCMockito -iquotePods/DTFoundation/Core/Source/DTUTI -iquotePods/DTFoundation/Core/Source/iOS -iquotePods/OCHamcrest/Source/Core/Helpers -iquotePods/OCHamcrest/Source/Library/Collection -iquotePods/OCHamcrest/Source/Library/Decorator -iquotePods/OCHamcrest/Source/Library/Logical -iquotePods/OCHamcrest/Source/Library/Number -iquotePods/OCHamcrest/Source/Library/Object -iquotePods/OCHamcrest/Source/Library/Text -iquotePods/DTFoundation/Core/Source/iOS/BlocksAdditions -iquotePods/DTFoundation/Core/Source/iOS/DTSidePanel -I/Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Products/Debug-iphonesimulator/include -I/Users/dummy/workspace/coconatics/FOO/bar-ios/Pods/Headers -I/Users/dummy/workspace/coconatics/FOO/bar-ios/Pods/Headers/CocoaLumberjack -I/Users/dummy/workspace/coconatics/FOO/bar-ios/Pods/Headers/DTDownload -I/Users/dummy/workspace/coconatics/FOO/bar-ios/Pods/Headers/DTFoundation -I/Users/dummy/workspace/coconatics/FOO/bar-ios/Pods/Headers/OCHamcrest -I/Users/dummy/workspace/coconatics/FOO/bar-ios/Pods/Headers/OCMockito -I/Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/UnitTests.build/DerivedSources/i386 -I/Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/UnitTests.build/DerivedSources -F/Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Products/Debug-iphonesimulator -F/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.0.sdk/Developer/Library/Frameworks -F/Applications/Xcode.app/Contents/Developer/Library/Frameworks -F/Users/dummy/workspace/coconatics/FOO/bar-ios -include /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/PrecompiledHeaders/UnitTests-Prefix-bkxdakvojutvzpbmmqocpesyehdo/UnitTests-Prefix.pch -MMD -MT dependencies -MF /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/UnitTests.build/Objects-normal/i386/MyDetailViewControllerTest_iPad.d --serialize-diagnostics /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/UnitTests.build/Objects-normal/i386/MyDetailViewControllerTest_iPad.dia -c /Users/dummy/workspace/coconatics/FOO/bar-ios/UnitTests/iPad/ViewControllers/MyDetailViewControllerTest_iPad.m -o /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Intermediates/FOO.build/Debug-iphonesimulator/UnitTests.build/Objects-normal/i386/MyDetailViewControllerTest_iPad.o\n" +
					"/Users/dummy/workspace/coconatics/FOO/bar-ios/UnitTests/iPad/ViewControllers/MyDetailViewControllerTest_iPad.m:163:41: warning: incompatible pointer types assigning to 'FOOServer *' from 'FOOServerStub *' [-Wincompatible-pointer-types]\n" +
					"        myDetailViewController.barServer = barServerStub;\n" +
					"                                               ^ ~~~~~~~~~~~~~\n" +
					"/Users/dummy/workspace/coconatics/FOO/bar-ios/UnitTests/iPad/ViewControllers/MyDetailViewControllerTest_iPad.m:186:41: warning: incompatible pointer types assigning to 'FOOServer *' from 'FOOServerStub *' [-Wincompatible-pointer-types]\n" +
					"        myDetailViewController.barServer = barServerStub;\n" +
					"                                               ^ ~~~~~~~~~~~~~\n" +
					"2 warnings generated.\n" +
					"\n" +
					"Ld /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Products/Debug-iphonesimulator/UnitTests.octest/UnitTests normal i386"

	def linkData = "\n" +
					"Ld build/DUMMY.build/Release-iphoneos/MyApp.build/Objects-normal/armv7/MyApp normal armv7\n" +
					"    cd /Users/dummy/workspace/Dummy/Dummy-ios\n" +
					"    setenv IPHONEOS_DEPLOYMENT_TARGET 5.0\n" +
					"    setenv PATH \"/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin:/Applications/Xcode.app/Contents/Developer/usr/bin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/usr/local/git/bin:/usr/local/git/bin:/Users/rene/Java/gradle/bin:/Applications/Xcode.app/Contents/Developer/usr/bin/:/Users/rene/.rvm/bin\"\n" +
					"    /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/clang -arch armv7 -isysroot /Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS7.0.sdk -L/Users/dummy/workspace/Dummy/Dummy-ios/build/Release-iphoneos -F/Users/dummy/workspace/Dummy/Dummy-ios/build/Release-iphoneos -filelist /Users/dummy/workspace/Dummy/Dummy-ios/build/Dummy.build/Release-iphoneos/MyApp.build/Objects-normal/armv7/MyApp.LinkFileList -dead_strip -ObjC -framework CoreData -framework MobileCoreServices -framework QuartzCore -fobjc-arc -fobjc-link-runtime -fprofile-arcs -ftest-coverage -miphoneos-version-min=5.0 -lz -lxml2 -framework CoreData -framework UIKit -framework Foundation -framework CoreGraphics -framework QuartzCore -framework MessageUI -framework Security -framework SystemConfiguration -framework QuickLook -framework ImageIO -framework MobileCoreServices -lPods -framework AssetsLibrary -framework Accelerate -Xlinker -dependency_info -Xlinker /Users/dummy/workspace/Dummy/Dummy-ios/build/Dummy.build/Release-iphoneos/MyApp.build/Objects-normal/armv7/MyApp_dependency_info.dat -o /Users/dummy/workspace/Dummy/Dummy-ios/build/Dummy.build/Release-iphoneos/MyApp.build/Objects-normal/armv7/MyApp\n" +
					"\n" +
					"Ld /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Products/Debug-iphonesimulator/UnitTests.octest/UnitTests normal i386"

	def linkErrorData = "\n" +
					"Ld build/DUMMY.build/Release-iphoneos/MyApp.build/Objects-normal/armv7/MyApp normal armv7\n" +
					"    cd /Users/dummy/workspace/Dummy/Dummy-ios\n" +
					"    setenv IPHONEOS_DEPLOYMENT_TARGET 5.0\n" +
					"    setenv PATH \"/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin:/Applications/Xcode.app/Contents/Developer/usr/bin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/usr/local/git/bin:/usr/local/git/bin:/Users/rene/Java/gradle/bin:/Applications/Xcode.app/Contents/Developer/usr/bin/:/Users/rene/.rvm/bin\"\n" +
					"    /Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/clang -arch armv7 -isysroot /Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS7.0.sdk -L/Users/dummy/workspace/Dummy/Dummy-ios/build/Release-iphoneos -F/Users/dummy/workspace/Dummy/Dummy-ios/build/Release-iphoneos -filelist /Users/dummy/workspace/Dummy/Dummy-ios/build/Dummy.build/Release-iphoneos/MyApp.build/Objects-normal/armv7/MyApp.LinkFileList -dead_strip -ObjC -framework CoreData -framework MobileCoreServices -framework QuartzCore -fobjc-arc -fobjc-link-runtime -fprofile-arcs -ftest-coverage -miphoneos-version-min=5.0 -lz -lxml2 -framework CoreData -framework UIKit -framework Foundation -framework CoreGraphics -framework QuartzCore -framework MessageUI -framework Security -framework SystemConfiguration -framework QuickLook -framework ImageIO -framework MobileCoreServices -lPods -framework AssetsLibrary -framework Accelerate -Xlinker -dependency_info -Xlinker /Users/dummy/workspace/Dummy/Dummy-ios/build/Dummy.build/Release-iphoneos/MyApp.build/Objects-normal/armv7/MyApp_dependency_info.dat -o /Users/dummy/workspace/Dummy/Dummy-ios/build/Dummy.build/Release-iphoneos/MyApp.build/Objects-normal/armv7/MyApp\n" +
					"ld: library not found for -lPods\n" +
					"clang: error: linker command failed with exit code 1 (use -v to see invocation)\n" +
					"\n" +
					"Ld /Users/dummy/Library/Developer/Xcode/DerivedData/FOO-fbukaldjlcdhljciwtwjdjdwjfqy/Build/Products/Debug-iphonesimulator/UnitTests.octest/UnitTests normal i386"

	def codesignErrorData = "\n" +
					"Check dependencies\n" +
					"Code Sign error: No code signing identities found: No valid signing identities (i.e. certificate and private key pair) matching the team ID “Z7L2YBTH45” were found.\n" +
					"CodeSign error: code signing is required for product type 'App Extension' in SDK 'iOS 8.1'\n" +
					"\n" +
					"** BUILD FAILED **"


	@Test
	void testError() {
		StyledTextOutputStub output = new StyledTextOutputStub()

		XcodeBuildOutputAppender appender =  new XcodeBuildOutputAppender(output)

		for (String line in errorData.split("\n")) {
			appender.append(line)
		}

		String expected = "   ERROR - Compile: Core/Source/Services/UIService.m\n"
		assert output.toString().startsWith(expected) : "Expected: " + expected  + " but was " + output.toString()
	}


	@Test
	void testWarning() {
		StyledTextOutputStub output = new StyledTextOutputStub()

		XcodeBuildOutputAppender appender = new XcodeBuildOutputAppender(output)

		for (String line in warningData.split("\n")) {
			appender.append(line)
		}

		String expected = "WARNINGS - Compile: UnitTests/iPad/ViewControllers/MyDetailViewControllerTest_iPad.m\n"
		assert output.toString().startsWith(expected): "Expected: " + expected + " but was " + output.toString()
	}


	@Test
	void testLinking() {
		StyledTextOutputStub output = new StyledTextOutputStub()

		XcodeBuildOutputAppender appender = new XcodeBuildOutputAppender(output)

		for (String line in linkData.split("\n")) {
			appender.append(line)
		}

		String expected = "      OK - Linking: build/DUMMY.build/Release-iphoneos/MyApp.build/Objects-normal/armv7/MyApp\n"
		assert output.toString().startsWith(expected): "Expected: " + expected + " but was " + output.toString()

	}

	@Test
	void testLinkingError() {
		StyledTextOutputStub output = new StyledTextOutputStub()

		XcodeBuildOutputAppender appender = new XcodeBuildOutputAppender(output)

		for (String line in linkErrorData.split("\n")) {
			appender.append(line)
		}

		String expected = "   ERROR - Linking: build/DUMMY.build/Release-iphoneos/MyApp.build/Objects-normal/armv7/MyApp\n"
		assert output.toString().startsWith(expected): "Expected: " + expected + " but was " + output.toString()

	}

	@Test
	void testCodesignError() {
		StyledTextOutputStub output = new StyledTextOutputStub()

		XcodeBuildOutputAppender appender = new XcodeBuildOutputAppender(output)

		for (String line in codesignErrorData.split("\n")) {
			appender.append(line)
		}

		String expected = "   ERROR - CodeSign\n"
		assert output.toString().startsWith(expected): "Expected: " + expected + " but was " + output.toString()

	}
}
