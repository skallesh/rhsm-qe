package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;
import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.Translation;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 * References:
 *   Engineering Localization Services: https://home.corp.redhat.com/node/53593
 *   http://git.fedorahosted.org/git/?p=subscription-manager.git;a=blob;f=po/pt.po;h=0854212f4fab348a25f0542625df343653a4a097;hb=RHEL6.3
 *   Here is the raw rhsm.po file for LANG=pt
 *   http://git.fedorahosted.org/git/?p=subscription-manager.git;a=blob;f=po/pt.po;hb=RHEL6.3
 *   
 *   https://translate.zanata.org/zanata/project/view/subscription-manager/iter/0.99.X/stats
 *   
 *   https://fedora.transifex.net/projects/p/fedora/
 *   
 *   http://translate.sourceforge.net/wiki/
 *   http://translate.sourceforge.net/wiki/toolkit/index
 *   http://translate.sourceforge.net/wiki/toolkit/pofilter
 *   http://translate.sourceforge.net/wiki/toolkit/pofilter_tests
 *   http://translate.sourceforge.net/wiki/toolkit/installation
 *   
 *   https://github.com/translate/translate
 *   
 *   Translation Bug Reporting Process
 *   https://engineering.redhat.com/trac/LocalizationServices/wiki/L10nBugReportingProcess
 *   
 *   RHEL5
 *   Table 2.1. Red Hat Enterprise Linux 5 International Languages
 *   http://docs.redhat.com/docs/en-US/Red_Hat_Enterprise_Linux/5/html/International_Language_Support_Guide/Red_Hat_Enterprise_Linux_International_Language_Support_Guide-Installing_and_supporting_languages.html
 *   notice Sri Lanka 	Sinhala 	si_LK.UTF-8)
 *   
 *   RHEL6
 *   https://engineering.redhat.com/trac/LocalizationServices
 *   https://engineering.redhat.com/trac/LocalizationServices/wiki/L10nRHEL6LanguageSupportCriteria
 *   
 *   
 *   Samples for looping through the translation files:
 *   MSGID="shows pools which provide products that are not already covered"; for L in `rpm -ql subscription-manager | grep rhsm.mo`; do echo ""; echo "Verifying translation for '$MSGID' in LANG file '$L'..."; msgunfmt --no-wrap $L | grep -i "$MSGID" -A1; done;
 *   for L in en_US de_DE es_ES fr_FR it_IT ja_JP ko_KR pt_BR ru_RU zh_CN zh_TW as_IN bn_IN hi_IN mr_IN gu_IN kn_IN ml_IN or_IN pa_IN ta_IN te_IN; do echo ""; echo "# LANG=$L.UTF-8 subscription-manager --help | grep -- --help"; LANG=$L.UTF-8 subscription-manager  --help | grep -- --help; done;
 *   
 **/
@Test(groups={"TranslationTests"})
public class TranslationTests extends SubscriptionManagerCLITestScript {
	
	
	// Test Methods ***********************************************************************
	
	@Test(	description="subscription-manager-cli: assert help commands return translated text",
			groups={"blockedByBug-756156","blockedByBug-906124","blockedByBug-919584"},
			dataProvider="getTranslatedCommandLineHelpData")
	//@ImplementsNitrateTest(caseId=)
	public void TranslatedCommandLineHelp_Test(Object bugzilla, String lang, String command, Integer exitCode, List<String> stdoutRegexs) {
		// Bug 969608 - [kn_IN][mr_IN][fr_FR][as_IN] missing usage translations for rhsmcertd tool
		if ((lang.equals("kn_IN")||lang.equals("mr_IN")||lang.equals("fr_FR")||lang.equals("as_IN")) && bugzilla!=null) {
			if (Arrays.asList(((BlockedByBzBug)bugzilla).getBugIds()).contains("969608")) {
				throw new SkipException("Skipping Test since Bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=969608 was CLOSED WONTFIX.");
			}
		}
		
		clienttasks.runCommandWithLangAndAssert(lang,command,exitCode,stdoutRegexs,null);
	}
	
	
	@Test(	description="subscription-manager-cli: attempt to register to a Candlepin server using bogus credentials and check for localized strings results",
			groups={"blockedByBug-919584","AcceptanceTests"},
			dataProvider="getInvalidRegistrationWithLocalizedStringsData")
	@ImplementsNitrateTest(caseId=41691)
	public void AttemptLocalizedRegistrationWithInvalidCredentials_Test(Object bugzilla, String lang, String username, String password, Integer exitCode, String stdoutRegex, String stderrRegex) {

		// ensure we are unregistered
		clienttasks.unregister(null, null, null);
		
		log.info("Attempting to register to a candlepin server using invalid credentials and expecting output in language "+(lang==null?"DEFAULT":lang));
		String command = String.format("%s register --username=%s --password=%s", clienttasks.command, username, password);
		clienttasks.runCommandWithLangAndAssert(lang,command,exitCode, stdoutRegex, stderrRegex);
		
		// assert that the consumer cert and key have NOT been dropped
		Assert.assertTrue(!RemoteFileTasks.testExists(client,clienttasks.consumerKeyFile()), "Consumer key file '"+clienttasks.consumerKeyFile()+"' does NOT exist after an attempt to register with invalid credentials.");
		Assert.assertTrue(!RemoteFileTasks.testExists(client,clienttasks.consumerCertFile()), "Consumer cert file '"+clienttasks.consumerCertFile()+" does NOT exist after an attempt to register with invalid credentials.");
	}
	
	
	@Test(	description="attempt LANG=C subscription-manager register",
			groups={"blockedByBug-729988"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void RegisterWithFallbackCLocale_Test() {

		//	[root@rhsm-compat-rhel61 ~]# LANG=C subscription-manager register --username stage_test_12 --password redhat 1>/tmp/stdout 2>/tmp/stderr
		//	[root@rhsm-compat-rhel61 ~]# echo $?
		//	255
		//	[root@rhsm-compat-rhel61 ~]# cat /tmp/stdout 
		//	[root@rhsm-compat-rhel61 ~]# cat /tmp/stderr
		//	'NoneType' object has no attribute 'lower'
		//	[root@rhsm-compat-rhel61 ~]# 
		
		for(String lang: new String[]{"C","us"}) {
			clienttasks.unregister(null, null, null);
			String command = String.format("%s register --username %s --password %s", clienttasks.command,sm_clientUsername,sm_clientPassword);
			if (sm_clientOrg!=null) command += String.format(" --org %s", sm_clientOrg);
			//SSHCommandResult sshCommandResult = clienttasks.runCommandWithLang(lang,clienttasks.command+" register --username "+sm_clientUsername+" --password "+sm_clientPassword+" "+(sm_clientOrg!=null?"--org "+sm_clientOrg:""));
			SSHCommandResult sshCommandResult = client.runCommandAndWait("LANG="+lang+" "+clienttasks.command+" register --username "+sm_clientUsername+" --password "+sm_clientPassword+" "+(sm_clientOrg!=null?"--org "+sm_clientOrg:""));
			Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0),"ExitCode after register with LANG="+lang+" fallback locale.");
			//Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), expectedStdoutRegex,"Stdout after register with LANG="+lang+" fallback locale.");
			//Assert.assertContainsMatch(sshCommandResult.getStderr().trim(), expectedStderrRegex,"Stderr after register with LANG="+lang+" fallback locale.");
		}
	}
	
	
	@Test(	description="subscription-manager: attempt redeem without --email option using LANG",
			groups={"blockedByBug-766577","AcceptanceTests"},
			enabled=false)	// TODO PASSES ON THE COMMAND LINE BUT FAILS WHEN RUN THROUGH AUTOMATION - NOTE STDOUT DISPLAYS DOUBLE BYTE BUT NOT STDERR
	//@ImplementsNitrateTest(caseId=)
	public void AttemptRedeemWithoutEmailUsingLang_Test() {
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		//SSHCommandResult redeemResult = clienttasks.redeem_(null,null,null,null,null)
		String lang = "de_DE";
		log.info("Attempting to redeem without specifying email expecting output in language "+(lang==null?"DEFAULT":lang));
		String command = String.format("%s %s redeem", lang==null?"":"LANG="+lang+".UTF-8", clienttasks.command);
		client.runCommandAndWait(command+" --help");
		SSHCommandResult redeemResult = client.runCommandAndWait(command);

		// bug766577
		// 201112191709:14.807 - FINE: ssh root@jsefler-onprem-5server.usersys.redhat.com LANG=de_DE subscription-manager redeem
		// 201112191709:17.276 - FINE: Stdout: 
		// 201112191709:17.277 - FINE: Stderr: 'ascii' codec can't encode character u'\xf6' in position 20: ordinal not in range(128)
		// 201112191709:17.277 - FINE: ExitCode: 255
		
		// [root@jsefler-onprem-5server ~]# LANG=de_DE.UTF-8 subscription-manager redeem
		// E-Mail-Adresse ist nötig zur Benachrichtigung

		// assert redemption results
		//Assert.assertEquals(redeemResult.getStdout().trim(), "email and email_locale are required for notification","Redeem should require that the email option be specified.");
		Assert.assertEquals(redeemResult.getStderr().trim(), "");
		Assert.assertEquals(redeemResult.getStdout().trim(), "E-Mail-Adresse ist nötig zur Benachrichtigung","Redeem should require that the email option be specified.");
		Assert.assertEquals(redeemResult.getExitCode(), Integer.valueOf(255),"Exit code from redeem when executed without an email option.");
	}
	
	
	@Test(	description="verify that rhsm.mo is installed for each of the supported locales",
			groups={"AcceptanceTests"},
			dataProvider="getSupportedLocalesData",
			enabled=false)	// replaced by VerifyOnlyExpectedTranslationFilesAreInstalled_Test
	@Deprecated
	//@ImplementsNitrateTest(caseId=)
	public void VerifyTranslationFileIsInstalled_Test_DEPRECATED(Object bugzilla, String locale) {
		File localeFile = localeFile(locale);
		Assert.assertTrue(RemoteFileTasks.testExists(client, localeFile.getPath()),"Supported locale file '"+localeFile+"' is installed.");
		if (!translationFileMapForSubscriptionManager.keySet().contains(localeFile)) Assert.fail("Something went wrong in TranslationTests.buildTranslationFileMap().  File '"+localeFile+"' was not found in the translationFileMap.keySet().");
	}
	
	@Test(	description="verify that only the expected rhsm.mo tranlation files are installed for each of the supported locales",
			groups={"AcceptanceTests", "blockedByBug-824100"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyOnlyExpectedTranslationFilesAreInstalled_Test() {
		List<File> supportedTranslationFiles = new ArrayList<File>();
		for (String supportedLocale : supportedLocales) supportedTranslationFiles.add(localeFile(supportedLocale));
		log.info("Expected locales include: "+supportedLocales);
		
		// assert no unexpected translation files are installed
		boolean unexpectedTranslationFilesFound = false;
		for (File translationFile : translationFileMapForSubscriptionManager.keySet()) {
			if (!supportedTranslationFiles.contains(translationFile)) {
				unexpectedTranslationFilesFound = true;
				log.warning("Unexpected translation file '"+translationFile+"' is installed.");
			}
		}
		// assert that all expected translation files are installed
		boolean allExpectedTranslationFilesFound = true;
		for (File translationFile : supportedTranslationFiles) {
			if (!translationFileMapForSubscriptionManager.keySet().contains(translationFile)) {
				log.warning("Expected translation file '"+translationFile+"' is NOT installed.");
				allExpectedTranslationFilesFound = false;
			} else {
				log.info("Expected translation file '"+translationFile+"' is installed.");
			}
		}
		Assert.assertTrue(!unexpectedTranslationFilesFound, "No unexpected translation files were found installed.");
		Assert.assertTrue(allExpectedTranslationFilesFound, "All expected translation files were found installed.");
	}
	
	@Test(	description="verify that only the expected rhsm.mo tranlation files are installed for each of the supported locales",
			groups={"AcceptanceTests", "blockedByBug-871152", "blockedByBug-912460", "blockedByBug-1003017", "blockedByBug-1020474", "blockedByBug-1057532"},
			dataProvider="getTranslationFileData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyTranslationFileContainsAllMsgids_Test(Object bugzilla, File translationFile) {
		List<Translation> translationList = translationFileMapForSubscriptionManager.get(translationFile);
		boolean translationFilePassed=true;
		for (String msgid : translationMsgidSet) {
			int numMsgidOccurances=0;
			for (Translation translation : translationList) {
				if (translation.msgid.equals(msgid)) numMsgidOccurances++;
			}
			if (numMsgidOccurances!=1) {
				log.warning("Expected 1 occurance (actual='"+numMsgidOccurances+"') of the following msgid in translation file '"+translationFile+"':  msgid \""+msgid+"\"");
				translationFilePassed=false;
			}
		}
		Assert.assertTrue(translationFilePassed,"Exactly 1 occurance of all the expected translation msgids ("+translationMsgidSet.size()+") were found in translation file '"+translationFile+"'.");
	}
	
	@Test(	description="run pofilter translate tests on the translation file",
			groups={},
			dataProvider="getTranslationFilePofilterTestData",
			enabled=false)	// 07/12/2012 this was the initial test created for the benefit of fsharath who further developed the test in PofilterTranslationTests.java; disabling this test in favor of his
	@Deprecated	
	//@ImplementsNitrateTest(caseId=)
	public void pofilter_Test(Object bugzilla, String pofilterTest, File translationFile) {
		log.info("For an explanation of pofilter test '"+pofilterTest+"', see: http://translate.sourceforge.net/wiki/toolkit/pofilter_tests");
		File translationPoFile = new File(translationFile.getPath().replaceFirst(".mo$", ".po"));
		
		// execute the pofilter test
		String pofilterCommand = "pofilter --gnome -t "+pofilterTest;
		SSHCommandResult pofilterResult = client.runCommandAndWait(pofilterCommand+" "+translationPoFile);
		Assert.assertEquals(pofilterResult.getExitCode(), new Integer(0), "Successfully executed the pofilter tests.");
		
		// convert the pofilter test results into a list of failed Translation objects for simplified handling of special cases 
		List<Translation> pofilterFailedTranslations = Translation.parse(pofilterResult.getStdout());
		
		// remove the first translation which contains only meta data
		if (!pofilterFailedTranslations.isEmpty() && pofilterFailedTranslations.get(0).msgid.equals("")) pofilterFailedTranslations.remove(0);
		
		// ignore the following special cases of acceptable results..........
		List<String> ignorableMsgIds = Arrays.asList();
		if (pofilterTest.equals("accelerators")) {
			if (translationFile.getPath().contains("/hi/")) ignorableMsgIds = Arrays.asList("proxy url in the form of proxy_hostname:proxy_port");
			if (translationFile.getPath().contains("/ru/")) ignorableMsgIds = Arrays.asList("proxy url in the form of proxy_hostname:proxy_port");
		}
		if (pofilterTest.equals("newlines")) {
			ignorableMsgIds = Arrays.asList(
					"Optional language to use for email notification when subscription redemption is complete. Examples: en-us, de-de",
					"\n"+"Unable to register.\n"+"For further assistance, please contact Red Hat Global Support Services.",
					"Tip: Forgot your login or password? Look it up at http://red.ht/lost_password",
					"Unable to perform refresh due to the following exception: %s",
					""+"This migration script requires the system to be registered to RHN Classic.\n"+"However this system appears to be registered to '%s'.\n"+"Exiting.",
					"The tool you are using is attempting to re-register using RHN Certificate-Based technology. Red Hat recommends (except in a few cases) that customers only register with RHN once.",
					// bug 825397	""+"Redeeming the subscription may take a few minutes.\n"+"Please provide an email address to receive notification\n"+"when the redemption is complete.",	// the Subscription Redemption dialog actually expands to accommodate the message, therefore we could ignore it	// bug 825397 should fix this
					// bug 825388	""+"We have detected that you have multiple service level\n"+"agreements on various products. Please select how you\n"+"want them assigned.", // bug 825388 or 825397 should fix this
					"\n"+"This machine appears to be already registered to Certificate-based RHN.  Exiting.",
					"\n"+"This machine appears to be already registered to Red Hat Subscription Management.  Exiting.");	
		}
		if (pofilterTest.equals("unchanged")) {
			ignorableMsgIds = Arrays.asList("close_button","facts_view","register_button","register_dialog_main_vbox","registration_dialog_action_area\n","prod 1, prod2, prod 3, prod 4, prod 5, prod 6, prod 7, prod 8");
		}

		
		// pluck out the ignorable pofilter test results
		for (String msgid : ignorableMsgIds) {
			Translation ignoreTranslation = Translation.findFirstInstanceWithMatchingFieldFromList("msgid", msgid, pofilterFailedTranslations);
			if (ignoreTranslation!=null) {
				log.info("Ignoring result of pofiliter test '"+pofilterTest+"' for msgid: "+ignoreTranslation.msgid);
				pofilterFailedTranslations.remove(ignoreTranslation);
			}
		}
		
		// assert that there are no failed pofilter translation test results
		Assert.assertEquals(pofilterFailedTranslations.size(),0, "Discounting the ignored test results, the number of failed pofilter '"+pofilterTest+"' tests for translation file '"+translationFile+"'.");
	}
	
	@Test(	description="verify that msgid \"Deprecated, see attach\" did NOT translate the command line module \"attach\" for all languages",
			groups={"blockedByBug-891375","blockedByBug-891378","blockedByBug-891380","blockedByBug-891383","blockedByBug-891384","blockedByBug-891386","blockedByBug-891391","blockedByBug-891394","blockedByBug-891398","blockedByBug-891402","blockedByBug-1061381"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyMsdIdDeprecatedSeeAttach_Test() {
		verifyTranslatedMsdIdContainsSubStringForAllLangs("Deprecated, see attach","attach");
	}
	
	@Test(	description="verify that msgid \"Deprecated, see remove\" did NOT translate the command line module \"remove\" for all languages",
			groups={"blockedByBug-891375","blockedByBug-891378","blockedByBug-891380","blockedByBug-891383","blockedByBug-891384","blockedByBug-891386","blockedByBug-891391","blockedByBug-891394","blockedByBug-891398","blockedByBug-891402"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyMsdIdDeprecatedSeeRemove_Test() {
		verifyTranslatedMsdIdContainsSubStringForAllLangs("Deprecated, see remove","remove");
	}
	
	@Test(	description="verify that msgid \"deprecated, see auto-attach-interval\" did NOT translate the command line option \"auto-attach-interval\" for all languages",
			groups={"blockedByBug-891375","blockedByBug-891434","blockedByBug-891377","blockedByBug-928073","blockedByBug-928082"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyMsdIdDeprecatedSeeAutoAttachInterval_Test() {
		//verifyTranslatedMsdIdContainsSubStringForAllLangs("deprecated, see auto-attach-interval","auto-attach-interval");	// was valid prior to bug 891377 implementation
		verifyTranslatedMsdIdContainsSubStringForAllLangs("deprecated, see --auto-attach-interval","--auto-attach-interval");	// now that bug 891377 is fixed, this test is effectively now a duplicate of pofilter -t options
	}
	
	@Test(	description="verify that translation msgstr does NOT contain paragraph character ¶ unless also in msgid",
			groups={},
			dataProvider="getTranslationFileDataForVerifyTranslationsDoNotUseParagraphCharacter_Test",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyTranslationsDoNotUseParagraphCharacter_Test(Object bugzilla, File translationFile) {
		boolean warningsFound = false;
		String paragraphChar = "¶";
		//for (File translationFile: translationFileMapForSubscriptionManager.keySet()) {	// use dataProvider="getTranslationFileData",
			for (Translation translation: translationFileMapForSubscriptionManager.get(translationFile)) {
				if (translation.msgstr.contains(paragraphChar) && !translation.msgid.contains(paragraphChar)) {
					log.warning("Paragraph character \""+paragraphChar+"\" should not be used in the "+translationFile+" translation: "+translation);
					warningsFound = true;
				}
			}
		//}
		Assert.assertTrue(!warningsFound,"No translations found containing unexpected paragraph character \""+paragraphChar+"\".");
	}
	@DataProvider(name="getTranslationFileDataForVerifyTranslationsDoNotUseParagraphCharacter_Test")
	public Object[][] getTranslationFileDataForVerifyTranslationsDoNotUseParagraphCharacter_TestAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getTranslationFileDataForVerifyTranslationsDoNotUseParagraphCharacter_TestAsListOfLists());
	}
	protected List<List<Object>> getTranslationFileDataForVerifyTranslationsDoNotUseParagraphCharacter_TestAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (translationFileMapForSubscriptionManager==null) return ll;
		for (File translationFile : translationFileMapForSubscriptionManager.keySet()) {
			Set<String> bugIds = new HashSet<String>();
			
			// Bug 825393 - [ml_IN][es_ES] translations should not use character ¶ for a new line.
			if (translationFile.getPath().contains("/ml/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/es_ES/")) bugIds.add("844369");
			
			// Bug 893120 - [hi][it][ml][ru] character ¶ should not appear in translated msgstr
			// Bug 908037 - [hi][it][ml][ru] character ¶ should not appear in translated msgstr
			if (translationFile.getPath().contains("/hi/")) bugIds.addAll(Arrays.asList("893120","908037"));
			if (translationFile.getPath().contains("/it/")) bugIds.addAll(Arrays.asList("893120","908037"));
			if (translationFile.getPath().contains("/ml/")) bugIds.addAll(Arrays.asList("893120","908037"));
			if (translationFile.getPath().contains("/ru/")) bugIds.addAll(Arrays.asList("893120","908037"));
			
			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			ll.add(Arrays.asList(new Object[] {blockedByBzBug, translationFile}));
		}
		return ll;
	}
	
	@Test(	description="verify that translation msgstr does NOT contain over-escaped newline character \\n (should be \n)",
			groups={},
			dataProvider="getTranslationFileDataForVerifyTranslationsDoNotContainOverEscapedNewlineCharacter_Test",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyTranslationsDoNotContainOverEscapedNewlineCharacter_Test(Object bugzilla, File translationFile) {
		boolean warningsFound = false;
		String overEscapedNewlineChar = "\\\n";
		//for (File translationFile: translationFileMapForSubscriptionManager.keySet()) {	// use dataProvider="getTranslationFileData",
			for (Translation translation: translationFileMapForSubscriptionManager.get(translationFile)) {
				if (translation.msgstr.contains(overEscapedNewlineChar) && !translation.msgid.contains(overEscapedNewlineChar)) {
					log.warning("Over-escaped newline character \""+overEscapedNewlineChar.replaceAll("\\n", "\\\\n")+"\" should not be used in the "+translationFile+" translation: "+translation);
					warningsFound = true;
				}
			}
		//}
		Assert.assertTrue(!warningsFound,"No translations found containing over-escaped newline character \""+overEscapedNewlineChar+"\".");
	}
	@DataProvider(name="getTranslationFileDataForVerifyTranslationsDoNotContainOverEscapedNewlineCharacter_Test")
	public Object[][] getTranslationFileDataForVerifyTranslationsDoNotContainOverEscapedNewlineCharacter_TestAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getTranslationFileDataForVerifyTranslationsDoNotContainOverEscapedNewlineCharacter_TestAsListOfLists());
	}
	protected List<List<Object>> getTranslationFileDataForVerifyTranslationsDoNotContainOverEscapedNewlineCharacter_TestAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (translationFileMapForSubscriptionManager==null) return ll;
		for (File translationFile : translationFileMapForSubscriptionManager.keySet()) {
			Set<String> bugIds = new HashSet<String>();
			
			// Bug 888971 - [pt_BR] pofilter newlines test failed
			if (translationFile.getPath().contains("/pt_BR/")) bugIds.add("888971");
			
			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			ll.add(Arrays.asList(new Object[] {blockedByBzBug, translationFile}));
		}
		return ll;
	}
	
	
	@Test(	description="verify that Red Hat product names (e.g. 'Red Hat','RHN') remain untranslated",
			groups={},
			dataProvider="getTranslationFileDataForVerifyTranslationsDoNotTranslateSubStrings_Test",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyTranslationsDoNotTranslateSubStrings_Test(Object bugzilla, File translationFile) {
		boolean warningsFound = false;
		List<String> doNotTranslateSubStrings = Arrays.asList("Red Hat",/*"RHN","RHN Classic", TRANSLATORS CHOICE see https://bugzilla.redhat.com/show_bug.cgi?id=950099#c7 */"subscription-manager-migration-data","subscription-manager","python-rhsm","consumer_types","consumer_export","proxy_hostname:proxy_port","firstboot");
		// TODO CONSIDER ADDING THESE TOO List<String> doNotTranslateSubStrings = Arrays.asList("Red Hat Subscription Manager","Red Hat Subscription Management", "Red Hat Global Support Services" "Red Hat Customer Portal", "RHN Satellite");
		
		List<String> ignoreTheseExceptionalCases = new ArrayList<String>();
		ignoreTheseExceptionalCases.add("View and configure subscription-manager plugins");
		ignoreTheseExceptionalCases.add("list subscription-manager plugin hooks");
		ignoreTheseExceptionalCases.add("list subscription-manager plugin slots");
		ignoreTheseExceptionalCases.add("list subscription-manager plugins");
		
		//for (File translationFile: translationFileMapForSubscriptionManager.keySet()) {	// use dataProvider="getTranslationFileData",
			for (Translation translation: translationFileMapForSubscriptionManager.get(translationFile)) {
				for (String subString : doNotTranslateSubStrings) {
					if (translation.msgid.contains(subString) && !translation.msgstr.contains(subString)) {
						
						// ignore exceptional cases listed above
						if (ignoreTheseExceptionalCases.contains(translation.msgid)) {
							log.info("Exceptional case: Ignoring translated substring \""+subString+"\" in translation: "+translation);
							continue;
						}
						
						// ignore the translated word when it is used at the start of a sentence
						/* cool idea, but I'm not going to use it
						String SubString = subString.substring(0,1).toUpperCase()+subString.substring(1,subString.length());
						if (translation.msgstr.startsWith(SubString)) {
							log.info("Exceptional case: Ignoring translated substring \""+subString+"\" when it starts the sentence in translation: "+translation);
							continue;
						}
						*/
						
						log.warning("Substring \""+subString+"\" should remain untranslated in the "+translationFile+" translation: "+translation);
						warningsFound = true;
					}
				}
			}
		//}
		Assert.assertTrue(!warningsFound,"No translations found with substrings that should remain untranslated.");
	}
	@DataProvider(name="getTranslationFileDataForVerifyTranslationsDoNotTranslateSubStrings_Test")
	public Object[][] getTranslationFileDataForVerifyTranslationsDoNotTranslateSubStrings_TestAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getTranslationFileDataForVerifyTranslationsDoNotTranslateSubStrings_TestAsListOfLists());
	}
	protected List<List<Object>> getTranslationFileDataForVerifyTranslationsDoNotTranslateSubStrings_TestAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (translationFileMapForSubscriptionManager==null) return ll;
		for (File translationFile : translationFileMapForSubscriptionManager.keySet()) {
			Set<String> bugIds = new HashSet<String>();
			
			// Bug 906567 - [hi][zh_CN][ru][ko][it]msgids containing "Red Hat" should NOT translate this substring
			if (translationFile.getPath().contains("/hi/")) bugIds.add("906567");
			if (translationFile.getPath().contains("/zh_CN/")) bugIds.add("906567");
			if (translationFile.getPath().contains("/ru/")) bugIds.add("906567");
			if (translationFile.getPath().contains("/ko/")) bugIds.add("906567");
			if (translationFile.getPath().contains("/it/")) bugIds.add("906567");
			
			// Bug 906552 - [es_ES][gu][kn][or][pa][pt_BR][ta_IN][te][zh_CN] msgids containing "subscription-manager" should NOT translate this substring
			if (translationFile.getPath().contains("/es_ES/")) bugIds.add("906552");
			if (translationFile.getPath().contains("/gu/")) bugIds.add("906552");
			if (translationFile.getPath().contains("/kn/")) bugIds.add("906552");
			if (translationFile.getPath().contains("/or/")) bugIds.add("906552");
			if (translationFile.getPath().contains("/pa/")) bugIds.add("906552");
			if (translationFile.getPath().contains("/pt_BR/")) bugIds.add("906552");
			if (translationFile.getPath().contains("/ta_IN/")) bugIds.add("906552");
			if (translationFile.getPath().contains("/te/")) bugIds.add("906552");
			if (translationFile.getPath().contains("/zh_CN/")) bugIds.add("906552");
			
			// Bug 1061393 - [pa] msgids containing "subscription-manager" should NOT translate this substring
			if (translationFile.getPath().contains("/pa/")) bugIds.add("1061393");		
			
			// Bug 906967 - [as][or][ta_IN][ml][pt_BR][gu][kn][mr][it][hi][zh_CN][te][ru][pa][ko] msgids containing "RHN", "RHN Classic" should NOT translate this substring
			if (translationFile.getPath().contains("/as/")) bugIds.add("906967");
			if (translationFile.getPath().contains("/or/")) bugIds.add("906967");
			if (translationFile.getPath().contains("/ta_IN/")) bugIds.add("906967");
			if (translationFile.getPath().contains("/ml/")) bugIds.add("906967");
			if (translationFile.getPath().contains("/pt_BR/")) bugIds.add("906967");
			if (translationFile.getPath().contains("/gu/")) bugIds.add("906967");
			if (translationFile.getPath().contains("/kn/")) bugIds.add("906967");
			if (translationFile.getPath().contains("/mr/")) bugIds.add("906967");
			if (translationFile.getPath().contains("/it/")) bugIds.add("906967");
			if (translationFile.getPath().contains("/hi/")) bugIds.add("906967");
			if (translationFile.getPath().contains("/zh_CN/")) bugIds.add("906967");
			if (translationFile.getPath().contains("/te/")) bugIds.add("906967");
			if (translationFile.getPath().contains("/ru/")) bugIds.add("906967");
			if (translationFile.getPath().contains("/pa/")) bugIds.add("906967");
			if (translationFile.getPath().contains("/ko/")) bugIds.add("906967");
			
			// Bug 950099 - [ml][zh_CN][ru][pt_BR] locales should not translate "RHN"
			if (translationFile.getPath().contains("/ml/")) bugIds.add("950099");
			if (translationFile.getPath().contains("/zh_CN/")) bugIds.add("950099");
			if (translationFile.getPath().contains("/ru/")) bugIds.add("950099");
			if (translationFile.getPath().contains("/pt_BR/")) bugIds.add("950099");
			
			// Bug 957195 - [or] bad translation for msgid "proxy URL in the form of proxy_hostname:proxy_port" 
			if (translationFile.getPath().contains("/or/")) bugIds.add("957195");
			
			// Bug 984203 - [de_DE] [es_ES] do not translate "subscription-manager-migration-data"
			if (translationFile.getPath().contains("/es_ES/")) bugIds.add("984203");
			if (translationFile.getPath().contains("/de_DE/")) bugIds.add("984203");
			
			// Bug 1071022 - the word "firstboot" should remain untranslated in subscription-manager-1.10
			if (translationFile.getPath().contains("/bn_IN/")) bugIds.add("1071022");
			if (translationFile.getPath().contains("/de_DE/")) bugIds.add("1071022");
			if (translationFile.getPath().contains("/es_ES/")) bugIds.add("1071022");
			if (translationFile.getPath().contains("/hi/")) bugIds.add("1071022");
			if (translationFile.getPath().contains("/kn/")) bugIds.add("1071022");
			if (translationFile.getPath().contains("/mr/")) bugIds.add("1071022");
			if (translationFile.getPath().contains("/pa/")) bugIds.add("1071022");
			if (translationFile.getPath().contains("/ta_IN/")) bugIds.add("1071022");
			if (translationFile.getPath().contains("/zh_CN/")) bugIds.add("1071022");
			
			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			ll.add(Arrays.asList(new Object[] {blockedByBzBug, translationFile}));
		}
		return ll;
	}
	
	
	
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 752321 - [ALL LANG] [RHSM CLI] Word [OPTIONS] is unlocalized and some message translation is still not complete https://github.com/RedHatQE/rhsm-qe/issues/213
	//      TODO NESTED LANG LOOP...  for L in en_US de_DE es_ES fr_FR it_IT ja_JP ko_KR pt_BR ru_RU zh_CN zh_TW as_IN bn_IN hi_IN mr_IN gu_IN kn_IN ml_IN or_IN pa_IN ta_IN te_IN; do for C in list refresh register subscribe unregister unsubscribe clean config environments facts identity import orgs redeem repos; do echo ""; echo "# LANG=$L.UTF8 subscription-manager $C --help | grep OPTIONS"; LANG=$L.UTF8 subscription-manager $C --help | grep OPTIONS; done; done;
	// TODO Create an equivalent test for candlepin    VerifyOnlyExpectedTranslationFilesAreInstalled_Test https://github.com/RedHatQE/rhsm-qe/issues/214
	// TODO Create an equivalent test for candlepin    VerifyTranslationFileContainsAllMsgids_Test https://github.com/RedHatQE/rhsm-qe/issues/214
	// TODO Bug 856419 - [ALL LANG][RHSM CLI] Unlocalized strings for Subscription Manager CLI https://github.com/RedHatQE/rhsm-qe/issues/215
	// TODO Bug 885145 - some langs are missing a translations for "The system is unable to redeem the requested subscription: %s" https://github.com/RedHatQE/rhsm-qe/issues/216
	// TODO Create an equivalent test for candlepin VerifyTranslationsDoNotTranslateSubStrings_Test https://github.com/RedHatQE/rhsm-qe/issues/214
	// TODO NEED TO FIGURE OUT HOW TO TEST msgid_plural.  THIS SEESM TO BREAK AFTER RUNNING THROUGH msgunfmt https://github.com/RedHatQE/rhsm-qe/issues/217
	// LANG FILE /usr/share/locale/ko/LC_MESSAGES/rhsm.mo...
	//		msgid "Covered by contract %s through %s"
	//		msgid_plural "Covered by contracts %s through %s"
	//		msgstr[0] "契約 %s で %s まで適用"
	//	LANG FILE /usr/share/locale/ru/LC_MESSAGES/rhsm.mo...
	//		msgid "Covered by contract %s through %s"
	//		msgid_plural "Covered by contracts %s through %s"
	//		msgstr[0] "По контракту %s до %s"
	//		msgstr[1] "По контрактам %s до %s"
	//		msgstr[2] "По контрактам %s до %s"
	//	LANG FILE /usr/share/locale/zh_CN/LC_MESSAGES/rhsm.mo...
	//		msgid "Covered by contract %s through %s"
	//		msgid_plural "Covered by contracts %s through %s"
	//		msgstr[0] "由合同 %s 到 %s 覆盖"
	//	LANG FILE /usr/share/locale/zh_TW/LC_MESSAGES/rhsm.mo...
	//		msgid "Covered by contract %s through %s"
	//		msgid_plural "Covered by contracts %s through %s"
	//		msgstr[0] "合約有效日期乃 %s 至 %s"
	
	
	
	
	
	// Configuration Methods ***********************************************************************
	@BeforeClass (groups="setup")
	public void buildTranslationFileMapForSubscriptionManagerBeforeClass() {
		translationFileMapForSubscriptionManager = buildTranslationFileMapForSubscriptionManager();
	}
	Map<File,List<Translation>> translationFileMapForSubscriptionManager = null;

//	@BeforeClass (groups="setup")
//	public void buildTranslationFileMapForCandlepinBeforeClass() {
//		translationFileMapForCandlepin = buildTranslationFileMapForCandlepin();
//	}
//	Map<File,List<Translation>> translationFileMapForCandlepin;

	@BeforeClass (groups="setup",dependsOnMethods={"buildTranslationFileMapForSubscriptionManagerBeforeClass"})
	public void buildTranslationMsgidSet() {
		if (clienttasks==null) return;
		
		// assemble a unique set of msgids (by taking the union of all the msgids from all of the translation files.)
		// TODO: My assumption that the union of all the msgids from the translation files completely matches
		//       the currently extracted message ids from the source code is probably incorrect.
		//       There could be extra msgids in the translation files that were left over from the last round
		//       of translations and are no longer applicable (should be excluded from this union algorithm).
		for (File translationFile : translationFileMapForSubscriptionManager.keySet()) {
			List<Translation> translationList = translationFileMapForSubscriptionManager.get(translationFile);
			for (Translation translation : translationList) {
				translationMsgidSet.add(translation.msgid);
			}
		}
	}
	Set<String> translationMsgidSet = new HashSet<String>(500);  // 500 is an estimated size

	// Protected Methods ***********************************************************************
	List<String> supportedLocales = Arrays.asList(	"as",	"bn_IN","de_DE","es_ES","fr",	"gu",	"hi",	"it",	"ja",	"kn",	"ko",	"ml",	"mr",	"or",	"pa",	"pt_BR","ru",	"ta_IN","te",	"zh_CN","zh_TW"); 
	List<String> supportedLangs = Arrays.asList(	"as_IN","bn_IN","de_DE","es_ES","fr_FR","gu_IN","hi_IN","it_IT","ja_JP","kn_IN","ko_KR","ml_IN","mr_IN","or_IN","pa_IN","pt_BR","ru_RU","ta_IN","te_IN","zh_CN","zh_TW"); 

	
	protected List<String> newList(String item) {
		List <String> newList = new ArrayList<String>();
		newList.add(item);
		return newList;
	}
	protected File localeFile(String locale) {
		return new File("/usr/share/locale/"+locale+"/LC_MESSAGES/rhsm.mo");
	}
	protected void verifyTranslatedMsdIdContainsSubStringForAllLangs(String msgid, String subString) {
		if (!translationMsgidSet.contains(msgid)) Assert.fail("Could not find expected msgid \""+msgid+"\".  Has this msgid changed?");
		boolean warningsFound = false;
		for (File translationFile: translationFileMapForSubscriptionManager.keySet()) {
			Translation translation = Translation.findFirstInstanceWithMatchingFieldFromList("msgid", msgid, translationFileMapForSubscriptionManager.get(translationFile));
			if (translation==null) {log.warning("Translation file '"+translationFile+"' does not yet have a translation for msgid '"+msgid+"'."); continue;}
			if (translation.msgstr.contains(subString)) {
				Assert.assertTrue(translation.msgstr.contains(subString),"\""+subString+"\" remains correctly untranslated in "+translationFile+" translation: "+translation);
			} else {
				log.warning("Expected \""+subString+"\" to remain untranslated in "+translationFile+" translation: "+translation);
				warningsFound = true;
			}
		}
		Assert.assertFalse(warningsFound,"No errors were found in the translations for msgid \""+msgid+"\".");
	}
	
	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="getSupportedLocalesData")
	public Object[][] getSupportedLocalesDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getSupportedLocalesDataAsListOfLists());
	}
	protected List<List<Object>> getSupportedLocalesDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (String locale : supportedLocales) {
			
			// bugzillas
			Object bugzilla = null;
			if (locale.equals("kn")) bugzilla = new BlockedByBzBug("811294");
			
			// Object bugzilla, String locale
			ll.add(Arrays.asList(new Object[] {bugzilla,	locale}));
		}
		return ll;
	}
	
	
	@DataProvider(name="getTranslationFileData")
	public Object[][] getTranslationFileDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getTranslationFileDataAsListOfLists());
	}
	protected List<List<Object>> getTranslationFileDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (translationFileMapForSubscriptionManager==null) return ll;
		for (File translationFile : translationFileMapForSubscriptionManager.keySet()) {
			Set<String> bugIds = new HashSet<String>();
			
			// Bug 824100 - pt_BR translations are outdated for subscription-manager 
			if (translationFile.getPath().contains("/pt_BR/")) bugIds.add("824100");
			
			// Bug 824184 - [ta_IN] translations for subscription-manager are missing (95% complete)
			if (translationFile.getPath().contains("/ta/")) bugIds.add("824184");
			if (translationFile.getPath().contains("/ta_IN/")) bugIds.add("824184");
			
			// Bug 844369 - msgids translations are missing for several languages
			if (translationFile.getPath().contains("/es_ES/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/ja/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/as/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/it/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/ru/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/zh_TW/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/de_DE/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/mr/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/ko/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/fr/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/or/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/te/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/zh_CN/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/hi/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/gu/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/pt_BR/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/pa/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/ml/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/bn_IN/")) bugIds.add("844369");
			if (translationFile.getPath().contains("/kn/")) bugIds.add("844369");
			
			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			ll.add(Arrays.asList(new Object[] {blockedByBzBug, translationFile}));
		}
		return ll;
	}

	@Deprecated	// 07/12/2012 this was the initial test created for the benefit of fsharath who further developed the test in PofilterTranslationTests.java
	@DataProvider(name="getTranslationFilePofilterTestData")
	public Object[][] getTranslationFilePofilterTestDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getTranslationFilePofilterTestDataAsListOfLists());
	}
	@Deprecated	// 07/12/2012 this was the initial test created for the benefit of fsharath who further developed the test in PofilterTranslationTests.java
	protected List<List<Object>> getTranslationFilePofilterTestDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (translationFileMapForSubscriptionManager==null) return ll;
		// see http://translate.sourceforge.net/wiki/toolkit/pofilter_tests
		//	Critical -- can break a program
		//    	accelerators, escapes, newlines, nplurals, printf, tabs, variables, xmltags, dialogsizes
		//	Functional -- may confuse the user
		//    	acronyms, blank, emails, filepaths, functions, gconf, kdecomments, long, musttranslatewords, notranslatewords, numbers, options, purepunc, sentencecount, short, spellcheck, urls, unchanged
		//	Cosmetic -- make it look better
		//    	brackets, doublequoting, doublespacing, doublewords, endpunc, endwhitespace, puncspacing, simplecaps, simpleplurals, startcaps, singlequoting, startpunc, startwhitespace, validchars
		//	Extraction -- useful mainly for extracting certain types of string
		//    	compendiumconflicts, credits, hassuggestion, isfuzzy, isreview, untranslated

		List<String> pofilterTests = Arrays.asList(
				//	Critical -- can break a program
				"accelerators","escapes","newlines","nplurals","printf","tabs","variables","xmltags",
				//	Functional -- may confuse the user
				"blank","emails","filepaths","gconf","long","notranslatewords","numbers","options","short","urls","unchanged",
				//	Cosmetic -- make it look better
				"doublewords",
				//	Extraction -- useful mainly for extracting certain types of string
				"untranslated");
// debugTesting pofilterTests = Arrays.asList("newlines");
		for (File translationFile : translationFileMapForSubscriptionManager.keySet()) {
			for (String pofilterTest : pofilterTests) {
				BlockedByBzBug bugzilla = null;
				// Bug 825362	[es_ES] failed pofilter accelerator tests for subscription-manager translations 
				if (pofilterTest.equals("accelerators") && translationFile.getPath().contains("/es_ES/")) bugzilla = new BlockedByBzBug("825362");
				// Bug 825367	[zh_CN] failed pofilter accelerator tests for subscription-manager translations 
				if (pofilterTest.equals("accelerators") && translationFile.getPath().contains("/zh_CN/")) bugzilla = new BlockedByBzBug("825367");
				// Bug 825397	Many translated languages fail the pofilter newlines test
				if (pofilterTest.equals("newlines") && !(translationFile.getPath().contains("/zh_CN/")||translationFile.getPath().contains("/ru/")||translationFile.getPath().contains("/ja/"))) bugzilla = new BlockedByBzBug("825397");			
				// Bug 825393	[ml_IN][es_ES] translations should not use character ¶ for a new line. 
				if (pofilterTest.equals("newlines") && translationFile.getPath().contains("/ml/")) bugzilla = new BlockedByBzBug("825393");
				if (pofilterTest.equals("newlines") && translationFile.getPath().contains("/es_ES/")) bugzilla = new BlockedByBzBug("825393");

				ll.add(Arrays.asList(new Object[] {bugzilla,	pofilterTest,	translationFile}));
			}
		}
		return ll;
	}
	
	
	@DataProvider(name="getTranslatedCommandLineHelpData")
	public Object[][] getTranslatedCommandLineHelpDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getTranslatedCommandLineHelpDataAsListOfLists());
	}
	protected List<List<Object>> getTranslatedCommandLineHelpDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		String command,usage,lang,module;
		List<String> helpOptions = Arrays.asList(new String[]{"-h","--help"});
		String helpOption;
		
		// [root@jsefler-r63-server ~]# for L in en_US de_DE es_ES fr_FR it_IT ja_JP ko_KR pt_BR ru_RU zh_CN zh_TW as_IN bn_IN hi_IN mr_IN gu_IN kn_IN ml_IN or_IN pa_IN ta_IN te_IN; do echo ""; echo "# LANG=$L.UTF-8 subscription-manager --help | grep -- --help"; LANG=$L.UTF-8 subscription-manager  --help | grep -- --help; done;
		// subscription-manager (-h or --help option is randomly chosen for each lang)
		command = clienttasks.command;
		lang = "en_US"; usage = "Usage: subscription-manager MODULE-NAME [MODULE-OPTIONS] [--help]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "de_DE"; usage = "(Verwendung|Verbrauch): subscription-manager MODUL-NAME [MODUL-OPTIONEN] [--help]";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "es_ES"; usage = "Uso: subscription-manager MÓDULO-NOMBRE [MÓDULO-OPCIONES] [--help]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "fr_FR"; usage = "Utilisation.*: subscription-manager MODULE-NAME [MODULE-OPTIONS] [--help]";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null/*new BlockedByBzBug(new String[]{"707080","743734","743732"})*/, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "it_IT"; usage = "Utilizzo: subscription-manager NOME-MODULO [OPZIONI-MODULO] [--help]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ja_JP"; usage = "使い方: subscription-manager モジュール名 [モジュールオプション] [--help]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"912466"}), lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ko_KR"; usage = "사용법: subscription-manager 모듈-이름 [모듈-옵션] [--help]";										helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "pt_BR"; usage = "Uso: subscription-manager MÓDULO-NOME [MÓDULO-OPÇÕES] [--help]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ru_RU"; usage = "Формат: subscription-manager ДЕЙСТВИЕ [ПАРАМЕТРЫ] [--help]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "zh_CN"; usage = "使用: subscription-manager 模块名称 [模块选项] [--help]";									helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"912466"})/*new BlockedByBzBug(new String[]{"707080","743732"})*/, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "zh_TW"; usage = "使用方法：subscription-manager 模塊 -名稱  [模塊 -選項] [--help]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "as_IN"; usage = "ব্যৱহাৰ: subscription-manager মডিউল-নাম [মডিউল-বিকল্পসমূহ] [--help]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null/*new BlockedByBzBug(new String[]{"743732","750807"})*/, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "bn_IN"; usage = "ব্যবহারপ্রণালী: subscription-manager মডিউল-নাম [মডিউল-বিকল্প] [--help]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "hi_IN"; usage = "प्रयोग: subscription-manager मॉड्यूल्स-नाम [मॉड्यूल्स-नाम] [--help]";									helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "mr_IN"; usage = "वापर: subscription-manager मॉड्युल-नाव [मॉड्युल-पर्याय] [--help]";									helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "mr_IN"; usage = "वापर: subscription-manager मॉड्युल-नाव [मॉड्युल-पर्याय] [--help]";									helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "gu_IN"; usage = "વપરાશ: subscription-manager મોડ્યુલ-નામ [મોડ્યુલ-વિકલ્પો] [--help]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "kn_IN"; usage = "ಬಳಕೆ: subscription-manager ಮಾಡ್ಯೂಲ್-ಹೆಸರು [ಮಾಡ್ಯೂಲ್-ಆಯ್ಕೆಗಳು] [--help]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("811294"), lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ml_IN"; usage = "ഉപയോഗിയ്ക്കേണ്ട വിധം: subscription-manager ഘടകത്തിന്റെ പേരു് [ഘടകത്തിനുള്ള ഐച്ഛികങ്ങള്‍] [--help]";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "or_IN"; usage = "ବ୍ୟବହାର ବିଧ: subscription-manager ମଡ୍ୟୁଲଗୁଡ଼ିକ-ନାମ [ମଡ୍ୟୁଲଗୁଡ଼ିକ-ବିକଳ୍ପଗୁଡିକ] [--help]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "pa_IN"; usage = "ਵਰਤੋਂ: subscription-manager ਮੌਡਿਊਲ-ਨਾਂ [ਮੌਡਿਊਲ-ਚੋਣਾਂ] [--help]";									helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ta_IN"; usage = "பயன்பாடு: subscription-manager தொகுதிக்கூறு-பெயர் [தொகுதிக்கூறு-விருப்பங்கள்] [--help]";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("811301"), lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "te_IN"; usage = "వాడుక: subscription-manager మాడ్యూళ్ళు-పేరు [మాడ్యూళ్ళు-ఐచ్చికములు] [--help]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		
		// TODO MODULE: clean
		// TODO MODULE: activate
		// TODO MODULE: facts
		// TODO MODULE: identity
		// TODO MODULE: list
		// TODO MODULE: refresh
		
		// MODULE: register
		// [root@jsefler-r63-server ~]# for L in en_US de_DE es_ES fr_FR it_IT ja_JP ko_KR pt_BR ru_RU zh_CN zh_TW as_IN bn_IN hi_IN mr_IN gu_IN kn_IN ml_IN or_IN pa_IN ta_IN te_IN; do echo ""; echo "# LANG=$L.UTF-8 subscription-manager register --help | grep -- 'subscription-manager register'"; LANG=$L.UTF-8 subscription-manager register --help | grep -- 'subscription-manager register'; done;
		module = "register";
		lang = "en_US"; usage = "Usage: subscription-manager register [OPTIONS]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "de_DE"; usage = "(Verwendung|Verbrauch): subscription-manager register [OPTIONEN]";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"693527","839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "es_ES"; usage = "Uso: subscription-manager register [OPCIONES]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "fr_FR"; usage = "Utilisation.*subscription-manager register [OPTIONS]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "it_IT"; usage = "Utilizzo: subscription-manager register [OPZIONI]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ja_JP"; usage = "使用法: subscription-manager register [オプション]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ko_KR"; usage = "사용법: subscription-manager register [옵션]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "pt_BR"; usage = "Uso: subscription-manager register [OPÇÕES]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ru_RU"; usage = "Формат: subscription-manager register [ПАРАМЕТРЫ]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "zh_CN"; usage = "使用：subscription-manager register [选项]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "zh_TW"; usage = "使用方法：subscription-manager register [選項]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "as_IN"; usage = "ব্যৱহাৰ: subscription-manager register [বিকল্পসমূহ]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"743732","839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "bn_IN"; usage = "ব্যবহারপ্রণালী: subscription-manager register [বিকল্প]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "hi_IN"; usage = "प्रयोग: subscription-manager register [विकल्प]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "mr_IN"; usage = "वापर: subscription-manager register [पर्याय]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "gu_IN"; usage = "વપરાશ: subscription-manager register [વિકલ્પો]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "kn_IN"; usage = "ಬಳಕೆ: subscription-manager register [ಆಯ್ಕೆಗಳು]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"811294","839807","845304","886901"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ml_IN"; usage = "ഉപയോഗിയ്ക്കേണ്ട വിധം: subscription-manager register [ഐച്ഛികങ്ങള്‍]";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "or_IN"; usage = "ଉପଯୋଗ: subscription-manager register [ବିକଳ୍ପଗୁଡିକ]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "pa_IN"; usage = "ਵਰਤੋਂ: subscription-manager register [ਚੋਣਾਂ]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ta_IN"; usage = "பயன்பாடு: subscription-manager register [விருப்பங்கள்]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "te_IN"; usage = "వా‍డుక: subscription-manager register [ఐచ్చికాలు]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));

		// TODO MODULE: subscribe
		// TODO MODULE: unregister
		// TODO MODULE: unsubscribe
		
		// rhn-migrate-classic-to-rhsm (-h or --help option is randomly chosen for each lang)
		command = "rhn-migrate-classic-to-rhsm";
		lang = "en_US"; usage = "Usage: rhn-migrate-classic-to-rhsm [OPTIONS]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "de_DE"; usage = "(Verwendung|Verbrauch): rhn-migrate-classic-to-rhsm [OPTIONEN]";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"707080"}), lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "es_ES"; usage = "Uso: rhn-migrate-classic-to-rhsm [OPCIONES]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "fr_FR"; usage = "Utilisation.*rhn-migrate-classic-to-rhsm [OPTIONS]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "it_IT"; usage = "Utilizzo: rhn-migrate-classic-to-rhsm [OPZIONI]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ja_JP"; usage = "使用法: rhn-migrate-classic-to-rhsm [オプション]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ko_KR"; usage = "사용법: rhn-migrate-classic-to-rhsm [옵션]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "pt_BR"; usage = "Uso: rhn-migrate-classic-to-rhsm [OPÇÕES]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ru_RU"; usage = "Формат: rhn-migrate-classic-to-rhsm [ПАРАМЕТРЫ]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "zh_CN"; usage = "使用：rhn-migrate-classic-to-rhsm [选项]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "zh_TW"; usage = "使用方法：rhn-migrate-classic-to-rhsm [選項]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "as_IN"; usage = "ব্যৱহাৰ: rhn-migrate-classic-to-rhsm [বিকল্পসমূহ]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "bn_IN"; usage = "ব্যবহারপ্রণালী: rhn-migrate-classic-to-rhsm [বিকল্]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "hi_IN"; usage = "प्रयोग: rhn-migrate-classic-to-rhsm [विकल्प]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "mr_IN"; usage = "वापर: rhn-migrate-classic-to-rhsm [पर्याय]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "gu_IN"; usage = "વપરાશ: rhn-migrate-classic-to-rhsm [વિકલ્પો]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "kn_IN"; usage = "ಬಳಕೆ: rhn-migrate-classic-to-rhsm [ಆಯ್ಕೆಗಳು]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ml_IN"; usage = "ഉപയോഗിയ്ക്കേണ്ട വിധം: rhn-migrate-classic-to-rhsm [ഐച്ഛികങ്ങള്‍]";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "or_IN"; usage = "ଉପଯୋଗ: rhn-migrate-classic-to-rhsm [ବିକଳ୍ପଗୁଡିକ]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "pa_IN"; usage = "ਵਰਤੋਂ: rhn-migrate-classic-to-rhsm [ਚੋਣਾਂ]";									helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ta_IN"; usage = "பயன்பாடு: rhn-migrate-classic-to-rhsm [விருப்பங்கள்]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "te_IN"; usage = "వా‍డుక: rhn-migrate-classic-to-rhsm [ఐచ్చికాలు]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));

		
		// rhsmcertd (-h or --help option is randomly chosen for each lang)
		// the following logic is also needed in HelpTests.getTranslatedCommandLineHelpDataAsListOfLists()
		if (clienttasks.redhatReleaseX.equals("5"))	helpOptions = Arrays.asList(new String[]{"-?","--help"});	// rhel5	// this logic is also needed in HelpTests.getExpectedCommandLineOptionsDataAsListOfLists()
		else										helpOptions = Arrays.asList(new String[]{"-h","--help"});	// rhel6
		// NOTE: THESE TRANSLATIONS ACTUALLY COME FROM /usr/share/locale/*/LC_MESSAGES/glib20.mo (package glib2 NOT subscription-manager)
		command = "rhsmcertd";
		lang = "en_US"; usage = "Usage:\\n  rhsmcertd [OPTION...]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "de_DE"; usage = "Aufruf:\\n  rhsmcertd [OPTION …]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "es_ES"; usage = "Uso:\\n  rhsmcertd [OPCIÓN…]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "fr_FR"; usage = "Utilisation :\\n  rhsmcertd [OPTION...]";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {clienttasks.redhatReleaseX.equals("5")? new BlockedByBzBug("969608"):null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "it_IT"; usage = "Uso:\\n  rhsmcertd [OPZIONE...]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "ja_JP"; usage = "用法:\\n  rhsmcertd [オプション...]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "ko_KR"; usage = "사용법:\\n  rhsmcertd [옵션...]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "pt_BR"; usage = "Uso:\\n  rhsmcertd [(OPÇÃO|OPÇÕES)...]";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "ru_RU"; usage = "Использование:\\n  rhsmcertd [ПАРАМЕТР(…|...)]";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "zh_CN"; usage = "用法：\\n  rhsmcertd [选项...]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "zh_TW"; usage = "用法：\\n  rhsmcertd [選項(…|...)]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "as_IN"; usage = "(ব্যৱহাৰ:|ব্যৱহাৰপ্ৰণালী:)\\n  rhsmcertd [OPTION...]";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {clienttasks.redhatReleaseX.equals("5")? new BlockedByBzBug("969608"):null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "bn_IN"; usage = "ব্যবহারপ্রণালী:\\n  rhsmcertd [OPTION...]";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "hi_IN"; usage = "प्रयोग:\\n  rhsmcertd [विकल्प...]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "mr_IN"; usage = "वापर:\\n  rhsmcertd [OPTION...]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {clienttasks.redhatReleaseX.equals("5")? new BlockedByBzBug("969608"):null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "gu_IN"; usage = "વપરાશ:\\n  rhsmcertd [OPTION...]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "kn_IN"; usage = "ಬಳಕೆ:\\n  rhsmcertd [OPTION...]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {clienttasks.redhatReleaseX.equals("5")? new BlockedByBzBug("969608"):null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "ml_IN"; usage = "ഉപയോഗിക്കേണ്ട വിധം:\\n  rhsmcertd [OPTION...]";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "or_IN"; usage = "ବ୍ଯବହାର:\\n  rhsmcertd [ପସନ୍ଦ...]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "pa_IN"; usage = "ਵਰਤੋਂ:\\n  rhsmcertd [ਚੋਣ...]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "ta_IN"; usage = "பயன்பாடு:\\n  rhsmcertd [OPTION...]";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "te_IN"; usage = "వినిమయం:\\n  rhsmcertd [ఇచ్చాపూర్వరకం...]";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(usage.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));

		return ll;
	}
	
	
	@DataProvider(name="getInvalidRegistrationWithLocalizedStringsData")
	public Object[][] getInvalidRegistrationWithLocalizedStringsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getInvalidRegistrationWithLocalizedStringsAsListOfLists());
	}
	protected List<List<Object>> getInvalidRegistrationWithLocalizedStringsAsListOfLists(){
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (servertasks==null) return ll;
		if (clienttasks==null) return ll;
		
		String uErrMsg = servertasks.invalidCredentialsRegexMsg();

		// String lang, String username, String password, Integer exitCode, String stdoutRegex, String stderrRegex
		
		// registration test for a user who is invalid
		ll.add(Arrays.asList(new Object[]{null, "en_US.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, uErrMsg}));
		
		// registration test for a user who with "invalid credentials" (translated)
		//if (!isServerOnPremises)	ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"615362","642805"}),	"de_DE.UTF-8", clientusername+getRandInt(), clientpassword+getRandInt(), 255, null, isServerOnPremises? "Ungültige Berechtigungnachweise"/*"Ungültige Mandate"*//*"Ungültiger Benutzername oder Kennwort"*/:"Ungültiger Benutzername oder Kennwort. So erstellen Sie ein Login, besuchen Sie bitte https://www.redhat.com/wapps/ugc"}));
		//else 						ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("615362"),                      	"de_DE.UTF-8", clientusername+getRandInt(), clientpassword+getRandInt(), 255, null, isServerOnPremises? "Ungültige Berechtigungnachweise"/*"Ungültige Mandate"*//*"Ungültiger Benutzername oder Kennwort"*/:"Ungültiger Benutzername oder Kennwort. So erstellen Sie ein Login, besuchen Sie bitte https://www.redhat.com/wapps/ugc"}));
		if (sm_serverType.equals(CandlepinType.standalone)) {
			ll.add(Arrays.asList(new Object[]{null,								"en_US.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Invalid Credentials"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("615362"),		"de_DE.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Ungültige Berechtigungnachweise"}));
			ll.add(Arrays.asList(new Object[]{null,								"es_ES.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Credenciales inválidas"}));
			ll.add(Arrays.asList(new Object[]{null,								"fr_FR.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Informations d’identification invalides"}));
			ll.add(Arrays.asList(new Object[]{null,								"it_IT.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Credenziali invalide"}));
			ll.add(Arrays.asList(new Object[]{null,								"ja_JP.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "無効な識別情報"}));
			ll.add(Arrays.asList(new Object[]{null,								"ko_KR.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "잘못된 인증 정보"}));
			ll.add(Arrays.asList(new Object[]{null,								"pt_BR.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Credenciais inválidas"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("839805"),		"ru_RU.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Недопустимые реквизиты"}));	// "Недопустимые реквизиты" google translates to "Illegal details";  "Недопустимые учетные данные" google translates to "Invalid Credentials"
			ll.add(Arrays.asList(new Object[]{null,								"zh_CN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "无效证书"}));
			ll.add(Arrays.asList(new Object[]{null,								"zh_TW.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "無效的認證"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("683914"),		"as_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "অবৈধ পৰিচয়"}));
			ll.add(Arrays.asList(new Object[]{null,								"bn_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "অবৈধ পরিচয়"}));
			ll.add(Arrays.asList(new Object[]{null,								"hi_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "अवैध श्रेय"}));
			ll.add(Arrays.asList(new Object[]{null,								"mr_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "अवैध श्रेय"}));
			ll.add(Arrays.asList(new Object[]{null,								"gu_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "અયોગ્ય શ્રેય"}));
			ll.add(Arrays.asList(new Object[]{null,								"kn_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "ಅಮಾನ್ಯವಾದ ಪರಿಚಯಪತ್ರ"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("683914"),		"ml_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "തെറ്റായ ആധികാരികതകള്‍"}));
			ll.add(Arrays.asList(new Object[]{null,								"or_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "ଅବୈଧ ପ୍ରାଧିକରଣ"}));
			ll.add(Arrays.asList(new Object[]{null,								"pa_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "ਗਲਤ ਕਰੀਡੈਂਸ਼ੀਅਲ"}));	// former value ਗਲਤ ਕਰੀਡੈਂਸ਼ੀਅਲਸ	// former value ਗਲਤ ਕਰੀਡੈਂਸ਼ਲ
			ll.add(Arrays.asList(new Object[]{null,								"ta_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "தவறான சான்றுகள்"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("683914"),		"te_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "చెల్లని ప్రమాణాలు"}));
		} else {
			ll.add(Arrays.asList(new Object[]{null,								"en_US.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Invalid username or password. To create a login, please visit https://www.redhat.com/wapps/ugc/register.html"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),		"de_DE.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Ungültiger Benutzername oder Passwort. Um ein Login anzulegen, besuchen Sie bitte https://www.redhat.com/wapps/ugc/register.html"}));
			ll.add(Arrays.asList(new Object[]{null,								"es_ES.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "El nombre de usuario o contraseña es inválido. Para crear un nombre de usuario, por favor visite https://www.redhat.com/wapps/ugc/register.html"}));
			ll.add(Arrays.asList(new Object[]{null,								"fr_FR.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Nom d'utilisateur ou mot de passe non valide. Pour créer une connexion, veuillez visiter https://www.redhat.com/wapps/ugc/register.html"}));
			ll.add(Arrays.asList(new Object[]{null,								"it_IT.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Nome utente o password non valide. Per creare un login visitare https://www.redhat.com/wapps/ugc/register.html"}));
			ll.add(Arrays.asList(new Object[]{null,								"ja_JP.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "ユーザー名かパスワードが無効です。ログインを作成するには、https://www.redhat.com/wapps/ugc/register.html に進んでください"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),		"ko_KR.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "사용자 이름 또는 암호가 잘못되었습니다. 로그인을 만들려면, https://www.redhat.com/wapps/ugc/register.html으로 이동해 주십시오."}));
			ll.add(Arrays.asList(new Object[]{null,								"pt_BR.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Nome do usuário e senha incorretos. Por favor visite https://www.redhat.com/wapps/ugc/register.html para a criação do logon."}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),		"ru_RU.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Неверное имя пользователя или пароль. Для создания учётной записи перейдите к https://www.redhat.com/wapps/ugc/register.html"}));
			ll.add(Arrays.asList(new Object[]{null,								"zh_CN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "无效用户名或者密码。要创建登录，请访问 https://www.redhat.com/wapps/ugc/register.html"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),		"zh_TW.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "無效的使用者名稱或密碼。若要建立登錄帳號，請至 https://www.redhat.com/wapps/ugc/register.html"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),		"as_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "অবৈধ ব্যৱহাৰকাৰী নাম অথবা পাছৱাৰ্ড। এটা লগিন সৃষ্টি কৰিবলে, অনুগ্ৰহ কৰি চাওক https://www.redhat.com/wapps/ugc/register.html"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),		"bn_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "ব্যবহারকারীর নাম অথবা পাসওয়ার্ড বৈধ নয়। লগ-ইন প্রস্তুত করার জন্য অনুগ্রহ করে https://www.redhat.com/wapps/ugc/register.html পরিদর্শন করুন"}));
			ll.add(Arrays.asList(new Object[]{null,								"hi_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "अवैध उपयोक्तानाम या कूटशब्द. लॉगिन करने के लिए, कृपया https://www.redhat.com/wapps/ugc/register.html भ्रमण करें"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),		"mr_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "अवैध वापरकर्तानाव किंवा पासवर्ड. प्रवेश निर्माण करण्यासाठी, कृपया https://www.redhat.com/wapps/ugc/register.html येथे भेट द्या"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),		"gu_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "અયોગ્ય વપરાશકર્તાનામ અથવા પાસવર્ડ. લૉગિનને બનાવવા માટે, મહેરબાની કરીને https://www.redhat.com/wapps/ugc/register.html મુલાકાત લો"}));
			ll.add(Arrays.asList(new Object[]{null,								"kn_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "ಅಮಾನ್ಯವಾದ ಬಳಕೆದಾರ ಹೆಸರು ಅಥವ ಗುಪ್ತಪದ. ಒಂದು ಲಾಗಿನ್ ಅನ್ನು ರಚಿಸಲು, ದಯವಿಟ್ಟು https://www.redhat.com/wapps/ugc/register.html ಗೆ ತೆರಳಿ"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),		"ml_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "തെറ്റായ ഉപയോക്തൃനാമം അല്ലെങ്കില്<200d> രഹസ്യവാക്ക്. പ്രവേശനത്തിനായി, ദയവായി https://www.redhat.com/wapps/ugc/register.html സന്ദര്<200d>ശിയ്ക്കുക"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),		"or_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "ଅବୈଧ ଚାଳକନାମ କିମ୍ବା ପ୍ରବେଶ ସଂକେତ। ଗୋଟିଏ ଲଗଇନ ନିର୍ମାଣ କରିବା ପାଇଁ, ଦୟାକରି https://www.redhat.com/wapps/ugc/register.html କୁ ପରିଦର୍ଶନ କରନ୍ତୁ"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),		"pa_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "ਗਲਤ ਯੂਜ਼ਰ-ਨਾਂ ਜਾਂ ਪਾਸਵਰਡ। ਲਾਗਇਨ ਬਣਾਉਣ ਲਈ, ਕਿਰਪਾ ਕਰਕੇ ਇਹ ਵੇਖੋ https://www.redhat.com/wapps/ugc/register.html"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),		"ta_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "தவறான பயனர்பெயர் அல்லது கடவுச்சொல். ஒரு உட்புகுவை உருவாக்குவதற்கு, https://www.redhat.com/wapps/ugc/register.html பார்வையிடவும்"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),		"te_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "చెల్లని వాడుకరిపేరు లేదా సంకేతపదము. లాగిన్ సృష్టించుటకు, దయచేసి https://www.redhat.com/wapps/ugc/register.html దర్శించండి"}));
		}
		// registration test for a user who has not accepted Red Hat's Terms and conditions (translated)  Man, why did you do something?
		if (!sm_usernameWithUnacceptedTC.equals("")) {
			if (sm_serverType.equals(CandlepinType.hosted))	ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"615362","642805"}),"de_DE.UTF-8", sm_usernameWithUnacceptedTC, sm_passwordWithUnacceptedTC, 255, null, "Mensch, warum hast du auch etwas zu tun?? Bitte besuchen https://www.redhat.com/wapps/ugc!!!!!!!!!!!!!!!!!!"}));
			else											ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("615362"),                       "de_DE.UTF-8", sm_usernameWithUnacceptedTC, sm_passwordWithUnacceptedTC, 255, null, "Mensch, warum hast du auch etwas zu tun?? Bitte besuchen https://www.redhat.com/wapps/ugc!!!!!!!!!!!!!!!!!!"}));
		}
		
		// registration test for a user who has been disabled (translated)
		if (!sm_disabledUsername.equals("")) {
			ll.add(Arrays.asList(new Object[]{null, "en_US.UTF-8", sm_disabledUsername, sm_disabledPassword, 255, null,"The user has been disabled, if this is a mistake, please contact customer service."}));
		}
		// [root@jsefler-onprem-server ~]# for l in en_US de_DE es_ES fr_FR it_IT ja_JP ko_KR pt_BR ru_RU zh_CN zh_TW as_IN bn_IN hi_IN mr_IN gu_IN kn_IN ml_IN or_IN pa_IN ta_IN te_IN; do echo ""; echo ""; echo "# LANG=$l.UTF-8 subscription-manager clean --help"; LANG=$l.UTF-8 subscription-manager clean --help; done;
		/* TODO reference for locales
		[root@jsefler-onprem03 ~]# rpm -lq subscription-manager | grep locale
		/usr/share/locale/as_IN/LC_MESSAGES/rhsm.mo
		/usr/share/locale/bn_IN/LC_MESSAGES/rhsm.mo
		/usr/share/locale/de_DE/LC_MESSAGES/rhsm.mo
		/usr/share/locale/en_US/LC_MESSAGES/rhsm.mo
		/usr/share/locale/es_ES/LC_MESSAGES/rhsm.mo
		/usr/share/locale/fr_FR/LC_MESSAGES/rhsm.mo
		/usr/share/locale/gu_IN/LC_MESSAGES/rhsm.mo
		/usr/share/locale/hi_IN/LC_MESSAGES/rhsm.mo
		/usr/share/locale/it_IT/LC_MESSAGES/rhsm.mo
		/usr/share/locale/ja_JP/LC_MESSAGES/rhsm.mo
		/usr/share/locale/kn_IN/LC_MESSAGES/rhsm.mo
		/usr/share/locale/ko_KR/LC_MESSAGES/rhsm.mo
		/usr/share/locale/ml_IN/LC_MESSAGES/rhsm.mo
		/usr/share/locale/mr_IN/LC_MESSAGES/rhsm.mo
		/usr/share/locale/or_IN/LC_MESSAGES/rhsm.mo
		/usr/share/locale/pa_IN/LC_MESSAGES/rhsm.mo
		/usr/share/locale/pt_BR/LC_MESSAGES/rhsm.mo
		/usr/share/locale/ru_RU/LC_MESSAGES/rhsm.mo
		/usr/share/locale/ta_IN/LC_MESSAGES/rhsm.mo
		/usr/share/locale/te_IN/LC_MESSAGES/rhsm.mo
		/usr/share/locale/zh_CN/LC_MESSAGES/rhsm.mo
		/usr/share/locale/zh_TW/LC_MESSAGES/rhsm.mo
		*/
		
		// TODO HERE IS A COMMAND FOR GETTING THE EXPECTED TRANSLATION MESSAGE STRINGS
		/* msgunfmt /usr/share/locale/de/LC_MESSAGES/rhsm.mo
		msgid "%prog [options]"
		msgstr "%prog [Optionen]"

		msgid "%s (first date of invalid entitlements)"
		msgstr "%s (erster Tag mit ungültigen Berechtigungen)"
		*/
		
		/* python script that alikins wrote to pad a language strings with _
	    #!/usr/bin/python
	     
	    import polib
	    import sys
	     
	    path = sys.argv[1]
	    pofile = polib.pofile(path)
	     
	    for entry in pofile:
	    orig = entry.msgstr
	    new = orig + "_"*40
	    entry.msgstr = new
	     
	    pofile.save(path)
	    */

		/* TODO Here is a script from alikins that will report untranslated strings
		#!/usr/bin/python
		
		# NEEDS polib from http://pypi.python.org/pypi/polib
		# or easy_install polib
		 
		import glob
		import polib
		 
		#FIXME
		PO_PATH = "po/"
		 
		po_files = glob.glob("%s/*.po" % PO_PATH)
		 
		for po_file in po_files:
		  print
		  print po_file
		  p = polib.pofile(po_file)
		  for entry in p.untranslated_entries():
		    for line in entry.occurrences:
		      print "%s:%s" % (line[0], line[1])
		    print "\t%s" % entry.msgid
		 
		  for entry in p.fuzzy_entries():
		    for line in entry.occurrences:
		      print "%s:%s" % (line[0], line[1])
		    print "\t%s" % entry.msgid
		 */
		return ll;
	}

}
