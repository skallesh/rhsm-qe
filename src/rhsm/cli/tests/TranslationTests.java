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
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.Translation;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.LinkedItem;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;

/**
 * @author jsefler
 * References:
 *   Engineering Localization Services: https://home.corp.redhat.com/node/53593
 *   http://git.fedorahosted.org/git/?p=subscription-manager.git;a=blob;f=po/pt.po;h=0854212f4fab348a25f0542625df343653a4a097;hb=RHEL6.3
 *   Here is the raw rhsm.po file for LANG=pt
 *   http://git.fedorahosted.org/git/?p=subscription-manager.git;a=blob;f=po/pt.po;hb=RHEL6.3
 *   
 *   Slave Configurations:
 *     Launch method Launch slave agents on Unix machines vis SSH
 *       JVM Options  -Dfile.encoding=UTF-8
 *     Node Properties: 
 *       Environment variables:   LC_CTYPE  en_US.UTF-8
 *   
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
 *     Subscription-manager:
 *       MSGID="shows pools which provide products that are not already covered"; for L in `rpm -ql subscription-manager | grep rhsm.mo`; do echo ""; echo "Verifying translation for '$MSGID' in LANG file '$L'..."; msgunfmt --no-wrap $L | grep -i "$MSGID" -A1; done;
 *       for L in en_US de_DE es_ES fr_FR it_IT ja_JP ko_KR pt_BR ru_RU zh_CN zh_TW as_IN bn_IN hi_IN mr_IN gu_IN kn_IN ml_IN or_IN pa_IN ta_IN te_IN; do echo ""; echo "# LANG=$L.UTF-8 subscription-manager --help | grep -- --help"; LANG=$L.UTF-8 subscription-manager  --help | grep -- --help; done;
 *     Candlepin:
 *       MSGID="uber"; for L in `ls ~/candlepin/common/po/*.po`; do echo ""; echo "Searching for '$MSGID' in LANG file '$L'..."; grep -i "$MSGID" -A1 $L; done;
 *   
 *   Passing LANG through curl:
 *   Use a hyphenated lang like  de-DE  it-IT  pt-BR  zh-CN
 *   [root@jsefler-os7 ~]# curl -k -u admin:admin --stderr /dev/null --header "Accept-Language: pt-BR" --request GET https://jsefler-os-candlepin.usersys.redhat.com:8443/candlepin/products/foo
 *   {"displayMessage":"Product com UUID 'foo'  não foram encontrados.","requestUuid":"8ae81ad0-059e-46c7-9088-5db8425faef0"}
 *   
 **/
@Test(groups={"TranslationTests"})
public class TranslationTests extends SubscriptionManagerCLITestScript {
	
	
	// Test Methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21766", "RHEL7-32176"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: assert help commands return translated text",
			groups={"Tier2Tests","blockedByBug-756156","blockedByBug-906124","blockedByBug-919584"},
			dataProvider="getTranslatedCommandLineHelpData")
	//@ImplementsNitrateTest(caseId=)
	public void testTranslatedCommandLineHelp(Object bugzilla, String lang, String command, Integer exitCode, List<String> stdoutRegexs) {
		// Bug 969608 - [kn_IN][mr_IN][fr_FR][as_IN] missing usage translations for rhsmcertd tool
		if ((lang.equals("kn_IN")||lang.equals("mr_IN")||lang.equals("fr_FR")||lang.equals("as_IN")) && bugzilla!=null) {
			if (Arrays.asList(((BlockedByBzBug)bugzilla).getBugIds()).contains("969608")) {
				throw new SkipException("Skipping Test since Bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=969608 was CLOSED WONTFIX.");
			}
		}
		
		clienttasks.runCommandWithLangAndAssert(lang,command,exitCode,stdoutRegexs,null);
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19944", "RHEL7-68143"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28485", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : subscription-manager cli registration and deregistration
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84906",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : subscription-manager cli registration and deregistration
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager-cli: attempt to register to a Candlepin server using bogus credentials and check for localized strings results",
			groups={"Tier1Tests","blockedByBug-919584"},
			dataProvider="getInvalidRegistrationWithLocalizedStringsData")
	@ImplementsNitrateTest(caseId=41691)
	public void testLocalizedRegistrationWithInvalidCredentials(Object bugzilla, String lang, String username, String password, Integer exitCode, String stdoutRegex, String stderrRegex) {

		// ensure we are unregistered
		clienttasks.unregister(null, null, null, null);
		
		log.info("Attempting to register to a candlepin server using invalid credentials and expecting output in language "+(lang==null?"DEFAULT":lang));
		String command = String.format("%s register --username=%s --password=%s", clienttasks.command, username, password);
		clienttasks.runCommandWithLangAndAssert(lang,command,exitCode, stdoutRegex, stderrRegex);
		
		// assert that the consumer cert and key have NOT been dropped
		Assert.assertTrue(!RemoteFileTasks.testExists(client,clienttasks.consumerKeyFile()), "Consumer key file '"+clienttasks.consumerKeyFile()+"' does NOT exist after an attempt to register with invalid credentials.");
		Assert.assertTrue(!RemoteFileTasks.testExists(client,clienttasks.consumerCertFile()), "Consumer cert file '"+clienttasks.consumerCertFile()+" does NOT exist after an attempt to register with invalid credentials.");
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21765", "RHEL7-32170"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28485", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : subscription-manager cli registration and deregistration
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84906",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : subscription-manager cli registration and deregistration
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify subscription-manager register will succeed with fallback locale LANG=C; also verify the system.default_locale fact",
			groups={"Tier2Tests","blockedByBug-729988"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithFallbackCLocale() {
		
		// Bug 729988
		//	[root@rhsm-compat-rhel61 ~]# LANG=C subscription-manager register --username stage_test_12 --password redhat 1>/tmp/stdout 2>/tmp/stderr
		//	[root@rhsm-compat-rhel61 ~]# echo $?
		//	255
		//	[root@rhsm-compat-rhel61 ~]# cat /tmp/stdout 
		//	[root@rhsm-compat-rhel61 ~]# cat /tmp/stderr
		//	'NoneType' object has no attribute 'lower'
		//	[root@rhsm-compat-rhel61 ~]# 
		
		String lang="C";
		clienttasks.unregister(null, null, null, null);
		String command = String.format("%s register --username %s --password %s", clienttasks.command,sm_clientUsername,sm_clientPassword);
		if (sm_clientOrg!=null) command += String.format(" --org %s", sm_clientOrg);
		//SSHCommandResult sshCommandResult = clienttasks.runCommandWithLang(lang,clienttasks.command+" register --username "+sm_clientUsername+" --password "+sm_clientPassword+" "+(sm_clientOrg!=null?"--org "+sm_clientOrg:""));
		SSHCommandResult sshCommandResult = client.runCommandAndWait("LANG="+lang+" "+clienttasks.command+" register --username "+sm_clientUsername+" --password "+sm_clientPassword+" "+(sm_clientOrg!=null?"--org "+sm_clientOrg:""));
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0),"ExitCode after register with LANG="+lang+" fallback locale.");
		
		// also test the system.default_locale fact for an unknown locale
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.3-1")) {	// commit 0670d70540a24a8e173d347e2240dcfb7535608a Bug 1425922: System locale in facts
			String systemDefaultLocaleFact = "system.default_locale";
			String systemDefaultLocaleFactValue = clienttasks.getFactValue(systemDefaultLocaleFact);
			String localeLANG = client.runCommandAndWait("locale | grep LANG").getStdout().trim().split("=")[1];
			// UTF-8 and UTF8 are interchangeable (UTF8 appears as the fact value on RHEL6)
			Assert.assertEquals(systemDefaultLocaleFactValue.replace("UTF8", "UTF-8"), localeLANG.replace("UTF8", "UTF-8"), "The system's value for fact '"+systemDefaultLocaleFact+"' matches the locale LANG. (Note: UTF-8 and UTF8 are interchangeable)");
		}
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47920", "RHEL7-97321"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28485", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : subscription-manager cli registration and deregistration
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84906",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : subscription-manager cli registration and deregistration
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify subscription-manager register will succeed with an unknown locale LANG=foo; also verify the system.default_locale fact reports Unknown",
			groups={"Tier2Tests","blockedByBug-729988","blockedByBug-1449824"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithUnknownLocale() {
		
		String lang="foo";
		clienttasks.unregister(null, null, null, null);
		String command = String.format("%s register --username %s --password %s", clienttasks.command,sm_clientUsername,sm_clientPassword);
		if (sm_clientOrg!=null) command += String.format(" --org %s", sm_clientOrg);
		SSHCommandResult sshCommandResult = client.runCommandAndWait("LANG="+lang+" "+clienttasks.registerCommand(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null, null));
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0),"ExitCode after register with LANG="+lang+" fallback locale.");
		Assert.assertEquals(clienttasks.getCurrentConsumerId(sshCommandResult),clienttasks.getCurrentConsumerId(),"The registered consumer's identity should be interpreted correctly from the stdout when registering with LANG="+lang+" fallback locale.");
		
		// also test the system.default_locale fact for an unknown locale
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.3-1")) {	// commit 0670d70540a24a8e173d347e2240dcfb7535608a Bug 1425922: System locale in facts
			String systemDefaultLocaleFact = "system.default_locale";
			String systemDefaultLocale = "Unknown";
			sshCommandResult = client.runCommandAndWait("LANG="+lang+" "+clienttasks.factsCommand(true, null, null, null, null, null)+" | grep "+systemDefaultLocaleFact);
			Assert.assertEquals(sshCommandResult.getStdout().trim(), String.format("%s: %s", systemDefaultLocaleFact,systemDefaultLocale), "The system's fact for the default locale when run with locale LANG="+lang+".");
		}
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47921", "RHEL7-97322"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28485", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : subscription-manager cli registration and deregistration
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84906",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : subscription-manager cli registration and deregistration
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Verify that registering with a LANG (without specifying a UTF-8 encoding) will succeed.  For example: LANG=fr_FR subscription-manager register",
			groups={"Tier2Tests"},
			dataProvider="getSupportedLangsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithDefaultEncoding(Object bugzilla, String lang) {
		lang = lang.replaceAll("\\.UTF-8", "");	// make sure lang does not have any encoding (no ".UTF-8" suffix)
		clienttasks.unregister(null, null, null, null);
		String registerCommandWithLang = String.format("%s %s","LANG="+lang, clienttasks.registerCommand(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (List<String>)null, null, null, null, null, null, null, null, null, null));
		SSHCommandResult sshCommandResult = client.runCommandAndWait(registerCommandWithLang);
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0),"ExitCode after register with LANG='"+lang+"' where no encoding was specified.");
	}
	
	
	@Test(	description="subscription-manager: attempt redeem without --email option using LANG",
			groups={"Tier1Tests","blockedByBug-766577"},
			enabled=false)	// TODO PASSES ON THE COMMAND LINE BUT FAILS WHEN RUN THROUGH AUTOMATION - NOTE STDOUT DISPLAYS DOUBLE BYTE BUT NOT STDERR
	//@ImplementsNitrateTest(caseId=)
	public void testRedeemWithoutEmailUsingLang() {
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
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
			groups={"Tier1Tests"},
			dataProvider="getSupportedLocalesData",
			enabled=false)	// replaced by VerifyOnlyExpectedTranslationFilesAreInstalled_Test
	@Deprecated
	//@ImplementsNitrateTest(caseId=)
	public void testTranslationFileIsInstalled_DEPRECATED(Object bugzilla, String locale) {
		File localeFile = localeFile(locale);
		Assert.assertTrue(RemoteFileTasks.testExists(client, localeFile.getPath()),"Supported locale file '"+localeFile+"' is installed.");
		if (!translationFileMapForSubscriptionManager.keySet().contains(localeFile)) Assert.fail("Something went wrong in TranslationTests.buildTranslationFileMap().  File '"+localeFile+"' was not found in the translationFileMap.keySet().");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19945", "RHEL7-68144"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify that only the expected rhsm.mo tranlation files are installed for each of the supported locales",
			groups={"Tier1Tests", "blockedByBug-824100", "blockedByBug-1594891"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testOnlyExpectedTranslationFilesAreInstalled() {
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


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-25823", "RHEL7-68145"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify that only the expected rhsm.mo tranlation files are installed for each of the supported locales",
			groups={"Tier1Tests","blockedByBug-1057532",
					"blockedByBug-1569464",	// RHEL7.6 subscription-manager-1.21 [subscription-manager] Tier 0 Localization
					"blockedByBug-1481232",	// RHEL7.5 subscription-manager-1.20 [subscription-manager] Tier 0 Localization
					"blockedByBug-1441739",	// Zanata translations for subscription-manager 1.19 are not 100%
					"blockedByBug-1391681",	// Zanata translations for subscription-manager 1.18 are not 100%
					"blockedByBug-1340135",	// Zanata translations for subscription-manager 1.17 are not 100%
					"blockedByBug-1303768",	// Zanata 1.16.X NOT 100%		// see Skip on Known Issue
					"blockedByBug-1195824",	// Zanata 1.14.X NOT 100%		// see Skip on Known Issue
					"blockedByBug-1166333",	// Zanata 1.13.X NOT 100%		// see Skip on Known Issue
					"blockedByBug-1118020",	// Zanata 1.12.X NOT 100%		// see Skip on Known Issue
					"blockedByBug-1093201",	// Zanata 1.11.X NOT 100%		// see Skip on Known Issue
					"blockedByBug-1020474",	// Zanata 1.10.X NOT 100%
					"blockedByBug-1003017",	// Zanata 1.9.X NOT 100%
					"blockedByBug-912460",	// Zanata 1.8.X NOT 100%
					"blockedByBug-871152"	// Zanata 1.1.X NOT 100%
					},
			dataProvider="getTranslationFileData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testTranslationFileContainsAllMsgids(Object bugzilla, File translationFile) {
		List<Translation> translationList = translationFileMapForSubscriptionManager.get(translationFile);
		boolean translationFilePassed=true;
		for (String msgid : translationMsgidSetForSubscriptionManager) {
			int numMsgidOccurances=0;
			for (Translation translation : translationList) {
				if (translation.msgid.equals(msgid)) numMsgidOccurances++;
			}
			if (numMsgidOccurances!=1) {
				log.warning("Expected 1 occurance (actual='"+numMsgidOccurances+"') of the following msgid in translation file '"+translationFile+"':  msgid \""+msgid+"\"");
				translationFilePassed=false;
			}
		}
		
		// Skip on Known Issue: https://bugzilla.redhat.com/show_bug.cgi?id=1093201#c5
		if (translationFile.getPath().contains("/pa/") && clienttasks.isPackageVersion("subscription-manager","==", "1.11")) {
			if (!translationFilePassed) {
				throw new SkipException("Missing translations for Punjabi /pa/ is a Known Issue for subscription-manager-1.11.  See https://bugzilla.redhat.com/show_bug.cgi?id=1093201#c5");
			}
		}
		
		// Skip on Known Issue: https://bugzilla.redhat.com/show_bug.cgi?id=1118020#c3
		if (translationFile.getPath().contains("/pa/") && clienttasks.isPackageVersion("subscription-manager","==", "1.12")) {
			if (!translationFilePassed) {
				throw new SkipException("Missing translations for Punjabi /pa/ is a Known Issue for subscription-manager-1.12.  See https://bugzilla.redhat.com/show_bug.cgi?id=1118020#c3");
			}
		}
		
		// Skip on Known Issue: https://bugzilla.redhat.com/show_bug.cgi?id=1166333#c1
		if (translationFile.getPath().contains("/pa/") && clienttasks.isPackageVersion("subscription-manager","==", "1.13")) {
			if (!translationFilePassed) {
				throw new SkipException("Missing translations for Punjabi /pa/ is a Known Issue for subscription-manager-1.13.  See https://bugzilla.redhat.com/show_bug.cgi?id=1166333#c1");
			}
		}
		if (translationFile.getPath().contains("/ko/") && clienttasks.isPackageVersion("subscription-manager","==", "1.13")) {
			if (!translationFilePassed) {
				throw new SkipException("Missing translations for Korean /ko/ is a Known Issue for subscription-manager-1.13.  See https://bugzilla.redhat.com/show_bug.cgi?id=1166333#c1");
			}
		}
		if (translationFile.getPath().contains("/as/") && clienttasks.isPackageVersion("subscription-manager","==", "1.13")) {
			if (!translationFilePassed) {
				throw new SkipException("Missing translations for Assamese /as/ is a Known Issue for subscription-manager-1.13.  See https://bugzilla.redhat.com/show_bug.cgi?id=1166333#c1");
			}
		}
		
		// Skip on Known Issue: https://bugzilla.redhat.com/show_bug.cgi?id=1195824#c2 (reduction to supportedLocales10)
		if (!translationFilePassed && clienttasks.isPackageVersion("subscription-manager",">=", "1.14")) {
			for (String unsupportedLocale : unsupportedLocales) {
				if (translationFile.getPath().contains("/"+unsupportedLocale+"/")) {
					throw new SkipException("Missing translations for '"+unsupportedLocale+"' in '"+translationFile.getPath()+"' is a Known Issue for subscription-manager-1.14+.  See https://bugzilla.redhat.com/show_bug.cgi?id=1195824#c2 stating the reduction in supported language translations.");
				}
			}
		}
		
		// TEMPORARY WORKAROUND FOR BUG:
		if (!translationFilePassed && clienttasks.isPackageVersion("subscription-manager","==", "1.16")) {
			if (translationFile.getPath().contains("/it/")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1318404"; // Bug 1318404 - [IT] Zanata translations for subscription-manager 1.16 are not 100%
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Missing translations for Lang '"+translationFile.getPath()+"' is a Known Issue for subscription-manager-1.16.  Skipping test while bug '"+bugId+"' is open.");
				}
			}

		}
		// END OF WORKAROUND
		
		// Skip on Known Issue: https://bugzilla.redhat.com/show_bug.cgi?id=1441739#c1
		if (false) // skip this SkipException due to https://bugzilla.redhat.com/show_bug.cgi?id=1441739#c2
		if (!translationFile.getPath().contains("/ja/") && clienttasks.isPackageVersion("subscription-manager","==", "1.19")) {
			if (!translationFilePassed) {
				throw new SkipException("Missing translations from "+translationFile+" is a Known Issue for subscription-manager-1.19.  New translations for RHEL7.4 are Japanese only.  See https://bugzilla.redhat.com/show_bug.cgi?id=1441739#c1 and https://bugzilla.redhat.com/show_bug.cgi?id=1449667");
			}
		}
		
		Assert.assertTrue(translationFilePassed,"Exactly 1 occurance of all the expected translation msgids ("+translationMsgidSetForSubscriptionManager.size()+") were found in translation file '"+translationFile+"'.");
	}
	
	@Test(	description="run pofilter translate tests on the translation file",
			groups={"Tier2Tests"},
			dataProvider="getTranslationFilePofilterTestData",
			enabled=false)	// 07/12/2012 this was the initial test created for the benefit of fsharath who further developed the test in PofilterTranslationTests.java; disabling this test in favor of his
	@Deprecated	
	//@ImplementsNitrateTest(caseId=)
	public void testPofilter_DEPRECATED(Object bugzilla, String pofilterTest, File translationFile) {
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


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21768", "RHEL7-51284"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.LOW, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="verify that msgid \"Deprecated, see attach\" did NOT translate the command line module \"attach\" for all languages",
			groups={"Tier3Tests","blockedByBug-891375","blockedByBug-891378","blockedByBug-891380","blockedByBug-891383","blockedByBug-891384","blockedByBug-891386","blockedByBug-891391","blockedByBug-891394","blockedByBug-891398","blockedByBug-891402","blockedByBug-1061381"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testMsgIdDeprecatedSeeAttach() {
		verifyTranslatedMsgIdContainsSubStringForAllLangs("Deprecated, see attach","attach");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21770", "RHEL7-51286"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.LOW, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="verify that msgid \"Deprecated, see remove\" did NOT translate the command line module \"remove\" for all languages",
			groups={"Tier3Tests","blockedByBug-891375","blockedByBug-891378","blockedByBug-891380","blockedByBug-891383","blockedByBug-891384","blockedByBug-891386","blockedByBug-891391","blockedByBug-891394","blockedByBug-891398","blockedByBug-891402"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testMsgIdDeprecatedSeeRemove() {
		verifyTranslatedMsgIdContainsSubStringForAllLangs("Deprecated, see remove","remove");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21769", "RHEL7-51285"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.LOW, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="verify that msgid \"deprecated, see auto-attach-interval\" did NOT translate the command line option \"auto-attach-interval\" for all languages",
			groups={"Tier3Tests","blockedByBug-891375","blockedByBug-891434","blockedByBug-891377","blockedByBug-928073","blockedByBug-928082"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testMsgIdDeprecatedSeeAutoAttachInterval() {
		//verifyTranslatedMsgIdContainsSubStringForAllLangs("deprecated, see auto-attach-interval","auto-attach-interval");	// was valid prior to bug 891377 implementation
		verifyTranslatedMsgIdContainsSubStringForAllLangs("deprecated, see --auto-attach-interval","--auto-attach-interval");	// now that bug 891377 is fixed, this test is effectively now a duplicate of pofilter -t options
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36513", "RHEL7-32165"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify that translation msgstr does NOT contain paragraph character ¶ unless also in msgid",
			groups={"Tier2Tests"},
			dataProvider="getTranslationFileDataForVerifyTranslationsDoNotUseParagraphCharacter_Test",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testTranslationsDoNotUseParagraphCharacter(Object bugzilla, File translationFile) {
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
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-51160", "RHEL-133207"}, //importReady=false,
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify that a colon character has not been appended to the end of the translated msgstr unless msgid also ends in a colon",
			groups={"Tier2Tests"},
			dataProvider="getTranslationFileDataToTestTranslationsDoNotEndInUnwantedColon",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testTranslationsDoNotEndInUnwantedColon(Object bugzilla, File translationFile) {
		boolean warningsFound = false;
		List<String> colonChars = Arrays.asList(":","："/*from zh_CN char set*/);
		for (String colonChar : colonChars) {
			for (Translation translation: translationFileMapForSubscriptionManager.get(translationFile)) {
				if (translation.msgstr.trim().endsWith(colonChar) && !translation.msgid.trim().endsWith(":")) {
					log.warning("Colon character \""+colonChar+"\" should not be randomly appended to the end of "+translationFile+" translation: "+translation);
					warningsFound = true;
				}
			}
		}
		Assert.assertTrue(!warningsFound,"No translations found containing unexpected trailing colon characters.  (Details for failed translations are listed in the WARNING messages above.)");
	}
	@DataProvider(name="getTranslationFileDataToTestTranslationsDoNotEndInUnwantedColon")
	public Object[][] getTranslationFileDataToTestTranslationsDoNotEndInUnwantedColonAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getTranslationFileDataToTestTranslationsDoNotEndInUnwantedColonAsListOfLists());
	}
	protected List<List<Object>> getTranslationFileDataToTestTranslationsDoNotEndInUnwantedColonAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (translationFileMapForSubscriptionManager==null) return ll;
		for (File translationFile : translationFileMapForSubscriptionManager.keySet()) {
			Set<String> bugIds = new HashSet<String>();
			
			// Bug 1545442 - [zh_CN][fr][de][ru][pt_BR][pa][kn] unwanted trailing colon characters have been appended to some translations
			if (translationFile.getPath().contains("/zh_CN/")) bugIds.add("1545442");
			if (translationFile.getPath().contains("/pt_BR/")) bugIds.add("1545442");
			if (translationFile.getPath().contains("/fr/")) bugIds.add("1545442");
			if (translationFile.getPath().contains("/de/")) bugIds.add("1545442");
			if (translationFile.getPath().contains("/ru/")) bugIds.add("1545442");
			if (translationFile.getPath().contains("/pa/")) bugIds.add("1545442");
			if (translationFile.getPath().contains("/kn/")) bugIds.add("1545442");
			
			// Bug 1544335 - [zh_CN][RHSM GUI][RHSM Initial Setup] Redundant colon next to 'Manually attach subscriptions after registration' check box in System Registration dialog.
			if (translationFile.getPath().contains("/zh_CN/")) bugIds.add("1544335");
			
			// Bug 1544327 - [zh_CN][RHSM GUI][RHSM Initial Setup] Redundant colon in Attach button text in Subscription Attachment dialog.
			if (translationFile.getPath().contains("/zh_CN/")) bugIds.add("1544327");
		
			// Bug 1544324 - [zh_CN][RHSM GUI][RHSM Initial Setup] Redundant colon in Next button text in System Registration dialog.
			if (translationFile.getPath().contains("/zh_CN/")) bugIds.add("1544324");
			
			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			ll.add(Arrays.asList(new Object[] {blockedByBzBug, translationFile}));
		}
		return ll;
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21771", "RHEL7-32172"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.MEDIUM, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="verify that translation msgstr does NOT contain over-escaped newline character \\n (should be \n)",
			groups={"Tier3Tests"},
			dataProvider="getTranslationFileDataForVerifyTranslationsDoNotContainOverEscapedNewlineCharacter_Test",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testTranslationsDoNotContainOverEscapedNewlineCharacter(Object bugzilla, File translationFile) {
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


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21767", "RHEL7-51283"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.LOW, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="verify that candlepin translation msgstr does NOT contain unescaped single quotes in the msgid",
			groups={"Tier3Tests"},
			dataProvider="getTranslationFileDataForVerifyCandlepinTranslationsDoNotContainUnescapedSingleQuotes_Test",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testCandlepinTranslationsDoNotContainUnescapedSingleQuotes(Object bugzilla, File translationFile) {
		boolean warningsFound = false;
		//for (File translationFile: translationFileMapForCandlepin.keySet()) {	// use dataProvider="getTranslationFileData",
			for (Translation translation: translationFileMapForCandlepin.get(translationFile)) {
				
				// Note: from ngoswami@redhat.com regarding use of apostrophes in the /as.po translations
				//		> Yes, the single quote (') is correct and these are expected characters
				//		> in the translated string.  It is a phonetic requirement to depict a
				//		> nasal pronunciation. If Zanata is unable to render it, I shall add
				//		> preceding single quote as suggested by you.
				
				// TEMPORARY WORKAROUND FOR BUG:
				if (translation.msgid.trim().equals("Cannot add pools that are restricted to unit type 'person' to activation keys.") ||
					translation.msgid.trim().equals("A unit type of 'person' cannot be used with activation keys") ||
					translation.msgid.trim().equals("Unit type with id '") ||
					translation.msgid.trim().equals("The activation key name ''{0}'' must be alphanumeric or include the characters '-' or '_'") ||
					translation.msgid.trim().equals("couldn't read rules file") ||
					translation.msgid.trim().equals("couldn't generate statistics") ||
					translation.msgid.trim().equals("the order parameter must be either 'ascending' or 'descending'")) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="1167856"; // Bug 1167856 - candlepin msgids with unescaped single quotes will not print the single quotes
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
					if (invokeWorkaroundWhileBugIsOpen) {
						log.warning("Skipping test for unescaped single quotes on this translation while bug '"+bugId+"' is open.  Translation: "+translation);
						continue;
					}
				}
				// END OF WORKAROUND
				
				if (doesStringContainMatches(translation.msgstr,"(^|[^'])'([^']|$)")) {	// this regex is used to find solo single quotes in the translated msgstr
					log.warning("Found a translation containing an unescaped single quote in the "+translationFile+" translation: "+translation);
					warningsFound = true;
				}
			}
		//}
		Assert.assertTrue(!warningsFound,"No candlepin translations found containing unescaped single quotes.");
	}
	@DataProvider(name="getTranslationFileDataForVerifyCandlepinTranslationsDoNotContainUnescapedSingleQuotes_Test")
	public Object[][] getTranslationFileDataForVerifyCandlepinTranslationsDoNotContainUnescapedSingleQuotes_Test_TestAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getTranslationFileDataForVerifyCandlepinTranslationsDoNotContainUnescapedSingleQuotes_Test_TestAsListOfLists());
	}
	protected List<List<Object>> getTranslationFileDataForVerifyCandlepinTranslationsDoNotContainUnescapedSingleQuotes_Test_TestAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (translationFileMapForCandlepin==null) return ll;
		for (File translationFile : translationFileMapForCandlepin.keySet()) {
			Set<String> bugIds = new HashSet<String>();
			
			// Bug 1167490 - [fr_FR] translations for candlepin are not properly escaping apostrophe / single quotes
			if (translationFile.getPath().contains("/fr.po")) bugIds.add("1167490");
			if (translationFile.getPath().contains("/it.po")) bugIds.add("1167490");
			
			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			ll.add(Arrays.asList(new Object[] {blockedByBzBug, translationFile}));
		}
		return ll;
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21772", "RHEL7-32178"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify that Red Hat product names (e.g. 'Red Hat','RHN') remain untranslated",
			groups={"Tier2Tests"},
			dataProvider="getTranslationFileDataForVerifyTranslationsDoNotTranslateSubStrings_Test",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testTranslationsDoNotTranslateSubStrings(Object bugzilla, File translationFile) {
		boolean warningsFound = false;
		List<String> doNotTranslateSubStrings =  new ArrayList<String>();
		doNotTranslateSubStrings.add("Red Hat");
		doNotTranslateSubStrings.add("Dell");
		//doNotTranslateSubStrings.addAll(Arrays.asList(new String[]{"RHN","RHN Classic"}));	// TRANSLATORS CHOICE see https://bugzilla.redhat.com/show_bug.cgi?id=950099#c7
		//doNotTranslateSubStrings.addAll(Arrays.asList(new String[]{"Red Hat Subscription Manager","Red Hat Subscription Management", "Red Hat Global Support Services" "Red Hat Customer Portal", "RHN Satellite"}));	// TODO CONSIDER ADDING THESE TOO
		doNotTranslateSubStrings.addAll(Arrays.asList(new String[]{"subscription-manager-migration-data","subscription-manager","python-rhsm","rhn-migrate-classic-to-rhsm","firstboot"}));
		doNotTranslateSubStrings.addAll(Arrays.asList(new String[]{"consumer_types","consumer_export","proxy_hostname:proxy_port"}));
		doNotTranslateSubStrings.addAll(Arrays.asList(new String[]{"%(mappingfile)s","%(package)s"}));	// from key: Unable to read mapping file: %(mappingfile)s.\nDo you have the %(package)s package installed?
		doNotTranslateSubStrings.addAll(Arrays.asList(new String[]{"{dateexample}"}));	// from key: Date entered is invalid. Date should be in YYYY-MM-DD format (example: {dateexample})
		doNotTranslateSubStrings.addAll(Arrays.asList(new String[]{"&#x2022;"}));	// Unicode bullet character	// msgid "&#x2022; A network connection"	// msgid "&#x2022; The address of a subscription management service (optional)"	// msgid "&#x2022; Your account login"
		doNotTranslateSubStrings.addAll(Arrays.asList(new String[]{"subscription-manager register --help"}));	// msgid "This system is not yet registered. Try 'subscription-manager register --help' for more information."
		doNotTranslateSubStrings.addAll(Arrays.asList(new String[]{"subscription-manager plugins"}));	// msgid "View and configure subscription-manager plugins"
		doNotTranslateSubStrings.addAll(Arrays.asList(new String[]{"subscription-manager config --rhsm.auto_enable_yum_plugins=0"}));	// msgid "The yum plugins: %s were automatically enabled for the benefit of Red Hat Subscription Management. If not desired, use \"subscription-manager config --rhsm.auto_enable_yum_plugins=0\" to block this behavior."
		doNotTranslateSubStrings.add("%s:%s%s");	// msgid "Unregistering from: %s:%s%s"	// msgid "Registering to: %s:%s%s"
		
		List<String> ignoreTheseExceptionalCases = new ArrayList<String>();
		//ignoreTheseExceptionalCases.add("View and configure subscription-manager plugins");	// 12/8/2015 stop ignoring... will be changed by bug 1061407 in https://github.com/candlepin/subscription-manager/pull/1343/
		ignoreTheseExceptionalCases.add("list subscription-manager plugin hooks");
		ignoreTheseExceptionalCases.add("list subscription-manager plugin slots");
		ignoreTheseExceptionalCases.add("list subscription-manager plugins");
		ignoreTheseExceptionalCases.add("Warning - subscription-manager plugins were automatically enabled");
		
		//for (File translationFile: translationFileMapForSubscriptionManager.keySet()) {	// use dataProvider="getTranslationFileData",
			for (Translation translation: translationFileMapForSubscriptionManager.get(translationFile)) {
				for (String subString : doNotTranslateSubStrings) {
					if (translation.msgid.contains(subString) && !translation.msgstr.contains(subString)) {
						
						// ignore exceptional cases listed above
						if (ignoreTheseExceptionalCases.contains(translation.msgid)) {
							log.info("Exceptional case: Ignoring translated substring \""+subString+"\" in translation: "+translation);
							continue;
						}
						
						// tolerate cases...
						// WARNING: Substring "Red Hat" should remain untranslated in the /usr/share/locale/de/LC_MESSAGES/rhsm.mo translation: msgid='Registration with Red Hat Subscription Management is Done!' msgstr='Die Registrierung bei der Subskriptionsverwaltung ist fertig!'
						if (translationFile.getPath().contains("/de/") && subString.equals("Red Hat") && translation.msgid.equals("Registration with Red Hat Subscription Management is Done!") && translation.msgstr.equals("Die Registrierung bei der Subskriptionsverwaltung ist fertig!")) {
							log.info("Exceptional case: Tolerating translated substring \""+subString+"\" in translation "+translation+" because https://translate.google.com/ indicates the reverse translation to be \"Subscription management registration is ready!\" which ommits substring \\\"Red Hat\\\"");  // https://translate.google.com/#auto/en/Die%20Registrierung%20bei%20der%20Subskriptionsverwaltung%20ist%20fertig!
							continue;
						}
						
						// WARNING: Substring "Red Hat" should remain untranslated in the /usr/share/locale/ru/LC_MESSAGES/rhsm.mo translation: msgid='This system appears to already be registered to Red Hat Subscription Management.' msgstr='Система уже зарегистрирована в RHSM.'
						if (translationFile.getPath().contains("/ru/") && subString.equals("Red Hat") && translation.msgid.equals("This system appears to already be registered to Red Hat Subscription Management.") && translation.msgstr.equals("Система уже зарегистрирована в RHSM.")) {
							log.info("Exceptional case: Tolerating translated substring \""+subString+"\" in translation "+translation+" because https://translate.google.com/ indicates the reverse translation to be \"The system is already registered in RHSM.\" which ommits substring \"Red Hat\".");  // https://translate.google.com/#auto/en/%D0%A1%D0%B8%D1%81%D1%82%D0%B5%D0%BC%D0%B0%20%D1%83%D0%B6%D0%B5%20%D0%B7%D0%B0%D1%80%D0%B5%D0%B3%D0%B8%D1%81%D1%82%D1%80%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B0%20%D0%B2%20RHSM.
							continue;
						}
						// WARNING: Substring "Red Hat" should remain untranslated in the /usr/share/locale/ru/LC_MESSAGES/rhsm.mo translation: msgid='This system has already been registered with Red Hat using RHN Classic.' msgstr='Система уже зарегистрирована в RHN Classic.'
						if (translationFile.getPath().contains("/ru/") && subString.equals("Red Hat") && translation.msgid.equals("This system has already been registered with Red Hat using RHN Classic.") && translation.msgstr.equals("Система уже зарегистрирована в RHN Classic.")) {
							log.info("Exceptional case: Tolerating translated substring \""+subString+"\" in translation "+translation+" because https://translate.google.com/ indicates the reverse translation to be \"The system is already registered to RHN Classic.\" which ommits substring \"Red Hat\".");  // https://translate.google.com/#auto/en/%D0%A1%D0%B8%D1%81%D1%82%D0%B5%D0%BC%D0%B0%20%D1%83%D0%B6%D0%B5%20%D0%B7%D0%B0%D1%80%D0%B5%D0%B3%D0%B8%D1%81%D1%82%D1%80%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B0%20%D0%B2%20RHN%20Classic.
							continue;
						}
						
						// WARNING: Substring "Red Hat" should remain untranslated in the /usr/share/locale/ru/LC_MESSAGES/rhsm.mo translation: msgid='Registration with Red Hat Subscription Management is Done!' msgstr='Система успешно зарегистрирована.'
						if (translationFile.getPath().contains("/ru/") && subString.equals("Red Hat") && translation.msgid.equals("Registration with Red Hat Subscription Management is Done!") && translation.msgstr.equals("Система успешно зарегистрирована.")) {
							log.info("Exceptional case: Tolerating translated substring \""+subString+"\" in translation "+translation+" because https://translate.google.com/ indicates the reverse translation to be \"The system is successfully registered.\" which ommits substring \"Red Hat\".");  // https://translate.google.com/#auto/en/%D0%A1%D0%B8%D1%81%D1%82%D0%B5%D0%BC%D0%B0%20%D1%83%D1%81%D0%BF%D0%B5%D1%88%D0%BD%D0%BE%20%D0%B7%D0%B0%D1%80%D0%B5%D0%B3%D0%B8%D1%81%D1%82%D1%80%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B0.
							continue;
						}
						
						// Substring "Red Hat" should remain untranslated in the /usr/share/locale/ru/LC_MESSAGES/rhsm.mo translation: msgid='Your system is being registered again using Red Hat Subscription Management. Red Hat recommends that customers only register once.' msgstr='Попытка повторной регистрации системы с помощью subscription-manager.'
						if (translationFile.getPath().contains("/ru/") && subString.equals("Red Hat") && translation.msgid.equals("Your system is being registered again using Red Hat Subscription Management. Red Hat recommends that customers only register once.") && translation.msgstr.equals("Попытка повторной регистрации системы с помощью subscription-manager.")) {
							log.info("Exceptional case: Tolerating translated substring \""+subString+"\" in translation "+translation+" because https://translate.google.com/ indicates the reverse translation to be \"Trying to re-register system via subscription-manager.\" which ommits substring \"Red Hat\".");  // https://translate.google.com/#auto/en/%D0%9F%D0%BE%D0%BF%D1%8B%D1%82%D0%BA%D0%B0%20%D0%BF%D0%BE%D0%B2%D1%82%D0%BE%D1%80%D0%BD%D0%BE%D0%B9%20%D1%80%D0%B5%D0%B3%D0%B8%D1%81%D1%82%D1%80%D0%B0%D1%86%D0%B8%D0%B8%20%D1%81%D0%B8%D1%81%D1%82%D0%B5%D0%BC%D1%8B%20%D1%81%20%D0%BF%D0%BE%D0%BC%D0%BE%D1%89%D1%8C%D1%8E%20subscription-manager.
							continue;
						}
						
						// WARNING: Substring "Red Hat" should remain untranslated in the /usr/share/locale/de_DE/LC_MESSAGES/rhsm.mo translation: msgid='Registration with Red Hat Subscription Management is Done!' msgstr='Die Registrierung bei der Subskriptionsverwaltung ist fertig!'
						if (translationFile.getPath().contains("/de_DE/") && subString.equals("Red Hat") && translation.msgid.equals("Registration with Red Hat Subscription Management is Done!") && translation.msgstr.equals("Die Registrierung bei der Subskriptionsverwaltung ist fertig!")) {
							log.info("Exceptional case: Tolerating translated substring \""+subString+"\" in translation "+translation+" because https://translate.google.com/ indicates the reverse translation to be \"Registration with the subscription management is ready!\" which ommits substring \"Red Hat\".");  // https://translate.google.com/#auto/en/Die%20Registrierung%20bei%20der%20Subskriptionsverwaltung%20ist%20fertig!
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
						
						// tolerate firstboot => Firstboot translations, for example..
						// msgid='You will need to use Red Hat Subscription Manager to manually attach subscriptions to this system after completing firstboot.'
						// msgstr='Sie müssen dieses System nach Abschluss von Firstboot mithilfe des Red Hat Subscription Managers manuell mit Subskriptionen verknüpfen.'
						if (subString.equals("firstboot") && translation.msgstr.contains("Firstboot")) {
							log.info("Exceptional case: Ignoring punctuation of translated substring \""+subString+"\" in translation: "+translation);
							continue;
						}
						
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
			
			// Bug 1372837 - [zh_CN] msgids containing "Red Hat" should NOT translate this substring
			if (translationFile.getPath().contains("/zh_CN/")) bugIds.add("1372837");
			
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
			
			// Bug 1372839 - [pt_BR][it] msgids containing "subscription-manager" should NOT translate this substring
			if (translationFile.getPath().contains("/pt_BR/")) bugIds.add("1372839");
			if (translationFile.getPath().contains("/it/")) bugIds.add("1372839");
			
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
			
			// Bug 1189950 - [ko_KR] bad translation for "{dateexample}" prevents error message from rendering
			if (translationFile.getPath().contains("/ko/")) bugIds.add("1189950");
			
			// Bug 1463765 - fixes needed for a few pofilter test failures after pulling in new translations for rhel7.4	// this bug covers the unwanted translation of substring "%s:%s%s"
			if (translationFile.getPath().contains("/as/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/bn/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/de_DE/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/fr/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/gu/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/as/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/hi/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/ja/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/kn/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/ko/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/pa/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/ta_IN/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/te/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/as/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/zh_CN/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/zh_TW/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/it/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/pt_BR/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/ru/")) bugIds.add("1463765");
			if (translationFile.getPath().contains("/es_ES/")) bugIds.add("1463765");
			
			// Bug 1525621 - [es_ES] missing a space character in one of the translated strings
			if (translationFile.getPath().contains("/es_ES/")) bugIds.add("1525621");
			
			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			ll.add(Arrays.asList(new Object[] {blockedByBzBug, translationFile}));
		}
		return ll;
	}
	
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19946", "RHEL7-55158"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28563", role= DefTypes.Role.RELATES_TO),	// RHSM-REQ : Yum installation of entitled content and product certificates
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84940",role= DefTypes.Role.RELATES_TO),	// RHSM-REQ : Yum installation of entitled content and product certificates
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify that \"'ascii' codec can't decode byte\" errors do not occur with yum search",
			groups={"Tier1Tests","blockedByBug-1199597","VerifyYumSearchDoesNotThrowAsciiCodecError_Test"},
			dataProvider="getSupportedLangsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumSearchDoesNotThrowAsciiCodecError(Object bugzilla, String lang) {
		// skip the test when we do not have access to RHEL content
		if (!isRhelProductCertSubscribedForVerifyYumSearchDoesNotThrowAsciiCodecError) throw new SkipException("Cannot perform this test until an available RHEL subscription has been attached.");

		// attempt to search for the zsh package using the lang
		String command = "yum search zsh";
		SSHCommandResult result = clienttasks.runCommandWithLang(lang, command);
		
		//	201505281651:59.086 - FINE: ssh root@jsefler-os6.usersys.redhat.com LANG=as.UTF-8 yum search zsh
		//	201505281652:05.593 - FINE: Stdout: 
		//	Loaded plugins: product-id, refresh-packagekit, rhnplugin, security,
		//	              : subscription-manager
		//	=============================== N/S Matched: zsh ===============================
		//	zsh.x86_64 : A powerful interactive shell
		//
		//	  Name and summary matches only, use "search all" for everything.
		//	201505281652:05.599 - FINE: Stderr: 
		//	Failed to set locale, defaulting to C
		//	This system is not registered with RHN Classic or RHN Satellite.
		//	You can use rhn_register to register.
		//	RHN Satellite or RHN Classic support will be disabled.
		//	201505281652:05.603 - FINE: ExitCode: 0 
		String errorMsg = "Failed to set locale, defaulting to C";
		Assert.assertTrue(!result.getStdout().toLowerCase().contains(errorMsg.toLowerCase()), "Stdout from running '"+command+"' in locale '"+lang+"' does not contain error '"+errorMsg+"'.");
		Assert.assertTrue(!result.getStderr().toLowerCase().contains(errorMsg.toLowerCase()), "Stderr from running '"+command+"' in locale '"+lang+"' does not contain error '"+errorMsg+"'.");
		
		//	201505281705:36.206 - FINE: ssh root@jsefler-os6.usersys.redhat.com LANG=de_DE.UTF-8 yum search zsh (com.redhat.qe.tools.SSHCommandRunner.run)
		//	201505281705:42.591 - FINE: Stdout: 
		//	Geladene Plugins: product-id, refresh-packagekit, rhnplugin, security,
		//	                : subscription-manager
		//	=============================== N/S Matched: zsh ===============================
		//	zsh.x86_64 : A powerful interactive shell
		//
		//	  Name and summary matches only, use "search all" for everything.
		//	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201505281705:42.592 - FINE: Stderr: 
		//	'ascii' codec can't decode byte 0xc3 in position 8: ordinal not in range(128)
		//	This system is not registered with RHN Classic or RHN Satellite.
		//	You can use rhn_register to register.
		//	RHN Satellite oder RHN Classic Unterstützung wird deaktiviert.
		//	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201505281705:42.592 - FINE: ExitCode: 0
		errorMsg = "'ascii' codec can't decode byte";
		Assert.assertTrue(!result.getStdout().toLowerCase().contains(errorMsg.toLowerCase()), "Stdout from running '"+command+"' in locale '"+lang+"' does not contain error '"+errorMsg+"'.");
		Assert.assertTrue(!result.getStderr().toLowerCase().contains(errorMsg.toLowerCase()), "Stderr from running '"+command+"' in locale '"+lang+"' does not contain error '"+errorMsg+"'.");
	}
	@BeforeGroups(groups="setup",value={"VerifyYumSearchDoesNotThrowAsciiCodecError_Test"})
	public void beforeTestYumSearchDoesNotThrowAsciiCodecError() {
		if (clienttasks==null) return;
		// register with auto-subscribe
		SSHCommandResult result = clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		if (result.getStdout().contains("No products installed.")) return;	// this will cause VerifyYumSearchDoesNotThrowAsciiCodecError_Test to skip because isRhelProductCertSubscribedForVerifyYumSearchDoesNotThrowAsciiCodecError is false
		isRhelProductCertSubscribedForVerifyYumSearchDoesNotThrowAsciiCodecError = clienttasks.isRhelProductCertSubscribed();
		
		// remove python-simplejson
		String pkg="python-simplejson";
		if (clienttasks.isPackageInstalled(pkg)) clienttasks.yumRemovePackage(pkg);
	}
	protected boolean isRhelProductCertSubscribedForVerifyYumSearchDoesNotThrowAsciiCodecError = false;
	
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47934", "RHEL7-98520"},
			linkedWorkItems= {
				@LinkedItem(project= Project.RHEL6, workitemId= "RHEL6-28564", role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
				@LinkedItem(project= Project.RedHatEnterpriseLinux7, workitemId= "RHEL7-84942",role= DefTypes.Role.VERIFIES),	// RHSM-REQ : Translation
			},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify that \"UnicodeDecodeError\" and \"Traceback\" errors do not occur when /usr/libexec/rhsmcertd-worker is executed",
			groups={"Tier2Tests","blockedByBug-1480395","blockedByBug-1484265","groupRhsmcertdworkerUsingLang"},
			dataProvider="getSupportedLangsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRhsmcertdworkerUsingLang(Object bugzilla, String lang) {
		String command = clienttasks.rhsmCertDWorker;
		SSHCommandResult result = clienttasks.runCommandWithLang(lang, clienttasks.rhsmCertDWorker);
		//	201708171549:54.571 - FINE: ssh root@jsefler-rhel7.usersys.redhat.com LANG=de_DE.UTF-8 /usr/libexec/rhsmcertd-worker
		//	201708171549:55.810 - FINE: Stdout: 
		//	Berechtigungszertifikate & Repositorys aktualisieren
		//	Berechtigungszertifikate und Repositorys konnten nicht aktualisiert werden
		//	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201708171549:55.812 - FINE: Stderr: 
		//	Traceback (most recent call last):
		//	  File "/usr/lib64/python2.7/logging/__init__.py", line 851, in emit
		//	    msg = self.format(record)
		//	  File "/usr/lib64/python2.7/logging/__init__.py", line 724, in format
		//	    return fmt.format(record)
		//	  File "/usr/lib64/python2.7/logging/__init__.py", line 467, in format
		//	    s = self._fmt % record.__dict__
		//	UnicodeDecodeError: 'ascii' codec can't decode byte 0xc3 in position 117: ordinal not in range(128)
		//	Logged from file repolib.py, line 328
		//	201708171549:55.817 - FINE: ExitCode: 255
		String errorMsg = "Traceback";
		Assert.assertTrue(!result.getStdout().toLowerCase().contains(errorMsg.toLowerCase()), "Stdout from running '"+command+"' in locale '"+lang+"' does not contain error '"+errorMsg+"'.");
		Assert.assertTrue(!result.getStderr().toLowerCase().contains(errorMsg.toLowerCase()), "Stderr from running '"+command+"' in locale '"+lang+"' does not contain error '"+errorMsg+"'.");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0), "ExitCode from running '"+command+"' in locale '"+lang+"'.");
		errorMsg = "UnicodeDecodeError";
		Assert.assertTrue(!result.getStdout().toLowerCase().contains(errorMsg.toLowerCase()), "Stdout from running '"+command+"' in locale '"+lang+"' does not contain error '"+errorMsg+"'.");
		Assert.assertTrue(!result.getStderr().toLowerCase().contains(errorMsg.toLowerCase()), "Stderr from running '"+command+"' in locale '"+lang+"' does not contain error '"+errorMsg+"'.");
	}
	@BeforeGroups(groups="setup",value={"groupRhsmcertdworkerUsingLang"})
	public void beforeGroupRhsmcertdworkerUsingLang() {
		if (clienttasks==null) return;
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
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
	public void initializeSupportedLocalesBeforeClass() {
		supportedLocales = Arrays.asList(		"as",	"bn_IN","de_DE","es_ES","fr",	"gu",	"hi",	"it",	"ja",	"kn",	"ko",	"ml",	"mr",	"or",	"pa",	"pt_BR","ru",	"ta_IN","te",	"zh_CN","zh_TW"); 
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.20.10-1"/*RHEL-7.5 branch*/ /*"1.21.2-1" master branch*/)) {	// commit 8b512b06526ed3387fa0c3481670c88bab15b9d1 Update zanata config // Bug 1520648 - Subscription manager ignores variants of locale
			supportedLocales = Arrays.asList(	"as",	"bn_IN","de",	"es",	"fr",	"gu",	"hi",	"it",	"ja",	"kn",	"ko",	"ml",	"mr",	"or",	"pa",	"pt_BR","ru",	"ta_IN","te",	"zh_CN","zh_TW"); 
		}
	}
	protected List<String> supportedLocales;
	static final List<String> unsupportedLocales = Arrays.asList(	"as",	"bn_IN",	/*"de_DE",*/	/*"es_ES",*/	/*"fr",*/	"gu",	"hi",	/*"it",*/	/*"ja",*/	"kn",	/*"ko",*/	"ml",	"mr",	"or",	"pa",	/*"pt_BR",*/	/*"ru",*/	"ta_IN",	"te"	/*"zh_CN",*/	/*"zh_TW"*/);	// comes from https://bugzilla.redhat.com/show_bug.cgi?id=1195824#c2
	static final List<String> supportedLangs = Arrays.asList(	"as_IN","bn_IN","de_DE","es_ES","fr_FR","gu_IN","hi_IN","it_IT","ja_JP","kn_IN","ko_KR","ml_IN","mr_IN","or_IN","pa_IN","pt_BR","ru_RU","ta_IN","te_IN","zh_CN","zh_TW"); 
	static final List<String> unsupportedLangs = Arrays.asList(	"as_IN","bn_IN",/*"de_DE","es_ES","fr_FR",*/"gu_IN","hi_IN",/*"it_IT","ja_JP",*/"kn_IN",/*"ko_KR",*/"ml_IN","mr_IN","or_IN","pa_IN",/*"pt_BR","ru_RU",*/"ta_IN","te_IN"/*,"zh_CN","zh_TW"*/); 
	
	@BeforeClass (groups="setup")
	public void buildTranslationFileMapsBeforeClass() {
		translationFileMapForSubscriptionManager = buildTranslationFileMapForSubscriptionManager();
		translationFileMapForCandlepin = buildTranslationFileMapForCandlepin();
	}
	Map<File,List<Translation>> translationFileMapForSubscriptionManager = null;
	Map<File,List<Translation>> translationFileMapForCandlepin = null;

	@BeforeClass (groups="setup",dependsOnMethods={"buildTranslationFileMapsBeforeClass"})
	public void buildTranslationMsgidSets() {
		if (clienttasks==null) return;
		
		// assemble a unique set of msgids (by taking the union of all the msgids from all of the translation files.)
		// TODO: My assumption that the union of all the msgids from the translation files completely matches
		//       the currently extracted message ids from the source code is probably incorrect.
		//       There could be extra msgids in the translation files that were left over from the last round
		//       of translations and are no longer applicable (should be excluded from this union algorithm).
		// UPDATE: The block below that skips the unsupportedLocales should alleviate the TODO concern above.
		for (File translationFile : translationFileMapForSubscriptionManager.keySet()) {
			
			// does this translationFile provide translations to an unsupportedLocale?
			boolean isTranslationFileSupported = true;
			for (String unsupportedLocale : unsupportedLocales) {
				if (translationFile.getPath().contains(String.format("/%s/",unsupportedLocale))) isTranslationFileSupported=false;
			}
			if (!isTranslationFileSupported) continue; // skip the unsupportedLocales (so that the msgids from abandoned translations of prior releases do not interfere with expected msgids of this release)
			
			List<Translation> translationList = translationFileMapForSubscriptionManager.get(translationFile);
			for (Translation translation : translationList) {
				translationMsgidSetForSubscriptionManager.add(translation.msgid);
			}
		}
		
		for (File translationFile : translationFileMapForCandlepin.keySet()) {
			List<Translation> translationList = translationFileMapForCandlepin.get(translationFile);
			for (Translation translation : translationList) {
				translationMsgidSetForCandlepin.add(translation.msgid);
			}
		}
	}
	Set<String> translationMsgidSetForSubscriptionManager = new HashSet<String>(500);  // 500 is an estimated size
	Set<String> translationMsgidSetForCandlepin = new HashSet<String>(500);  // 500 is an estimated size

	// Protected Methods ***********************************************************************
	
	protected List<String> newList(String item) {
		List <String> newList = new ArrayList<String>();
		newList.add(item);
		return newList;
	}
	
	protected File localeFile(String locale) {
		return new File("/usr/share/locale/"+locale+"/LC_MESSAGES/rhsm.mo");
	}
	
	protected void verifyTranslatedMsgIdContainsSubStringForAllLangs(String msgid, String subString) {
		if (!translationMsgidSetForSubscriptionManager.contains(msgid)) Assert.fail("Could not find expected msgid \""+msgid+"\".  Has this msgid changed?");
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
	
	
	
	@DataProvider(name="getSupportedLangsData")
	public Object[][] getSupportedLangsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getSupportedLangsDataAsListOfLists());
	}
	protected List<List<Object>> getSupportedLangsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (String lang : supportedLangs) {
			// skip the unsupportedLangs
			if (unsupportedLangs.contains(lang)) continue;
			
			// bugzillas
			Object bugzilla = null;
			if (lang.equals("fr_FR")) bugzilla = new BlockedByBzBug(new String[]{"1449839","1450210"});
			if (lang.equals("ja_JP")) bugzilla = new BlockedByBzBug(new String[]{"1449839","1450210"});
			
			// Object bugzilla, String locale
			ll.add(Arrays.asList(new Object[] {bugzilla,	lang}));
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
		String command,output,lang,module;
		List<String> helpOptions = Arrays.asList(new String[]{"-h","--help"});
		String helpOption;
		
		// [root@jsefler-r63-server ~]# for L in en_US de_DE es_ES fr_FR it_IT ja_JP ko_KR pt_BR ru_RU zh_CN zh_TW as_IN bn_IN hi_IN mr_IN gu_IN kn_IN ml_IN or_IN pa_IN ta_IN te_IN; do echo ""; echo "# LANG=$L.UTF-8 subscription-manager --help | grep -- --help"; LANG=$L.UTF-8 subscription-manager  --help | grep -- --help; done;
		// subscription-manager (-h or --help option is randomly chosen for each lang)
		command = clienttasks.command;
		lang = "en_US"; output = "Usage: subscription-manager MODULE-NAME [MODULE-OPTIONS] [--help]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "de_DE"; output = "(Verwendung|Verbrauch): subscription-manager MODUL-NAME [MODUL-OPTIONEN] [--help]";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "es_ES"; output = "Uso: subscription-manager MÓDULO-NOMBRE [MÓDULO-OPCIONES] [--help]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "fr_FR"; output = "Utilisation.*: subscription-manager MODULE-NAME [MODULE-OPTIONS] [--help]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null/*new BlockedByBzBug(new String[]{"707080","743734","743732"})*/, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "it_IT"; output = "Utilizzo: subscription-manager NOME-MODULO [OPZIONI-MODULO] [--help]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ja_JP"; output = "使い方: subscription-manager モジュール名 [モジュールオプション] [--help]";						if (Float.valueOf(clienttasks.redhatReleaseXY)>=7.6f) output = "使用方法: subscription-manager モジュール名 [モジュールオプション] [--help]";
																															helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"912466"}), lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ko_KR"; output = "사용법: subscription-manager 모듈-이름 [모듈-옵션] [--help]";										helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "pt_BR"; output = "Uso: subscription-manager MÓDULO-NOME [MÓDULO-OPÇÕES] [--help]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ru_RU"; output = "Формат: subscription-manager ДЕЙСТВИЕ [ПАРАМЕТРЫ] [--help]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "zh_CN"; output = "使用: subscription-manager 模块名称 [模块选项] [--help]";										helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"912466"})/*new BlockedByBzBug(new String[]{"707080","743732"})*/, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "zh_TW"; output = "使用方法：subscription-manager 模組名稱 [模組選項] [--help]";									helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "as_IN"; output = "ব্যৱহাৰ: subscription-manager মডিউল-নাম [মডিউল-বিকল্পসমূহ] [--help]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null/*new BlockedByBzBug(new String[]{"743732","750807"})*/, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "bn_IN"; output = "ব্যবহারপ্রণালী: subscription-manager মডিউল-নাম [মডিউল-বিকল্প] [--help]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "hi_IN"; output = "प्रयोग: subscription-manager मॉड्यूल्स-नाम [मॉड्यूल्स-नाम] [--help]";										helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "mr_IN"; output = "वापर: subscription-manager मॉड्युल-नाव [मॉड्युल-पर्याय] [--help]";										helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "gu_IN"; output = "વપરાશ: subscription-manager મોડ્યુલ-નામ [મોડ્યુલ-વિકલ્પો] [--help]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "kn_IN"; output = "ಬಳಕೆ: subscription-manager ಮಾಡ್ಯೂಲ್-ಹೆಸರು [ಮಾಡ್ಯೂಲ್-ಆಯ್ಕೆಗಳು] [--help]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("811294"), lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ml_IN"; output = "ഉപയോഗിയ്ക്കേണ്ട വിധം: subscription-manager ഘടകത്തിന്റെ പേരു് [ഘടകത്തിനുള്ള ഐച്ഛികങ്ങള്‍] [--help]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "or_IN"; output = "ବ୍ୟବହାର ବିଧି: subscription-manager ମଡ୍ୟୁଲ-ନାମ [ମଡ୍ୟୁଲ-ବିକଳ୍ପଗୁଡିକ] [--help]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "pa_IN"; output = "ਵਰਤੋਂ: subscription-manager ਮੌਡਿਊਲ-ਨਾਂ [ਮੌਡਿਊਲ-ਚੋਣਾਂ] [--help]";									helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ta_IN"; output = "பயன்பாடு: subscription-manager தொகுதிக்கூறு-பெயர் [தொகுதிக்கூறு-விருப்பங்கள்] [--help]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("811301"), lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "te_IN"; output = "వాడుక: subscription-manager మాడ్యూల్-పేరు [మాడ్యూల్-ఐచ్చికాలు] [--help]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, helpOption.equals("--help")?0:1, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		
		// MODULE: clean
		// [root@jsefler-6server ~]# for L in en_US de_DE es_ES fr_FR it_IT ja_JP ko_KR pt_BR ru_RU zh_CN zh_TW as_IN bn_IN hi_IN mr_IN gu_IN kn_IN ml_IN or_IN pa_IN ta_IN te_IN; do echo ""; echo "# LANG=$L.UTF-8 subscription-manager clean --help | grep -B1 -- '-h, --help'"; LANG=$L.UTF-8 subscription-manager clean --help | grep -B1 -- '-h, --help'; done;
		module = "clean";		// accept either capitalized or noncapitalized
		lang = "en_US"; output = "(O|o)ptions:";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "de_DE"; output = "(O|o)ptionen:";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "es_ES"; output = "(O|o)pciones:";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "fr_FR"; output = "(O|o)ptions:";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "it_IT"; output = "(O|o)pzioni:";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ja_JP"; output = "オプション:";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ko_KR"; output = "옵션:";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "pt_BR"; output = "(O|o)pções:";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ru_RU"; output = "(П|п)араметры:";helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "zh_CN"; output = "选项:";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "zh_TW"; output = "選項:";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "as_IN"; output = "বিকল্পসমূহ:";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "bn_IN"; output = "বিকল্প:";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "hi_IN"; output = "विकल्प:";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "mr_IN"; output = "पर्याय:";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "gu_IN"; output = "વિકલ્પો:";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "kn_IN"; output = "ಆಯ್ಕೆಗಳು:";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ml_IN"; output = "ഐച്ഛികങ്ങള്‍:";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "or_IN"; output = "ବିକଳ୍ପଗୁଡିକ:";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "pa_IN"; output = "ਚੋਣਾਂ:";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ta_IN"; output = "விருப்பங்கள்:|விருப்பங்கள் :";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "te_IN"; output = "ఐచ్చికాలు:|ఐచ్చికములు:";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568","1117374"}), lang, command+" "+module+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		
		// TODO MODULE: activate
		// TODO MODULE: facts
		// TODO MODULE: identity
		// TODO MODULE: list
		// TODO MODULE: refresh
		
		// MODULE: register
		// [root@jsefler-r63-server ~]# for L in en_US de_DE es_ES fr_FR it_IT ja_JP ko_KR pt_BR ru_RU zh_CN zh_TW as_IN bn_IN hi_IN mr_IN gu_IN kn_IN ml_IN or_IN pa_IN ta_IN te_IN; do echo ""; echo "# LANG=$L.UTF-8 subscription-manager register --help | grep -- 'subscription-manager register'"; LANG=$L.UTF-8 subscription-manager register --help | grep -- 'subscription-manager register'; done;
		module = "register";
		lang = "en_US"; output = "Usage: subscription-manager register [OPTIONS]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "de_DE"; output = "(Verwendung|Verbrauch): subscription-manager register [OPTIONEN]";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"693527","839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "es_ES"; output = "Uso: subscription-manager register [OPCIONES]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "fr_FR"; output = "Utilisation.*subscription-manager register [OPTIONS]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "it_IT"; output = "Utilizzo: subscription-manager register [OPZIONI]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ja_JP"; output = "使用法: subscription-manager register [オプション]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ko_KR"; output = "사용법: subscription-manager register [옵션]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "pt_BR"; output = "Uso: subscription-manager register [OPÇÕES]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ru_RU"; output = "Формат: subscription-manager register [ПАРАМЕТРЫ]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "zh_CN"; output = "使用：subscription-manager register [选项]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "zh_TW"; output = "使用方法：subscription-manager register [選項]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "as_IN"; output = "ব্যৱহাৰ: subscription-manager register [বিকল্পসমূহ]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"743732","839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "bn_IN"; output = "ব্যবহারপ্রণালী: subscription-manager register [বিকল্প]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "hi_IN"; output = "प्रयोग: subscription-manager register [विकल्प]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "mr_IN"; output = "वापर: subscription-manager register [पर्याय]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "gu_IN"; output = "વપરાશ: subscription-manager register [વિકલ્પો]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "kn_IN"; output = "ಬಳಕೆ: subscription-manager register [ಆಯ್ಕೆಗಳು]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"811294","839807","845304","886901"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ml_IN"; output = "ഉപയോഗിയ്ക്കേണ്ട വിധം: subscription-manager register [ഐച്ഛികങ്ങള്‍]";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "or_IN"; output = "ଉପଯୋଗ: subscription-manager register [ବିକଳ୍ପଗୁଡିକ]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "pa_IN"; output = "ਵਰਤੋਂ: subscription-manager register [ਚੋਣਾਂ]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ta_IN"; output = "பயன்பாடு: subscription-manager register [விருப்பங்கள்]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "te_IN"; output = "వా‍డుక: subscription-manager register [ఐచ్చికాలు]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"839807","845304"}), lang, command+" "+helpOption+" "+module, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		
		// TODO MODULE: subscribe
		// TODO MODULE: unregister
		// TODO MODULE: unsubscribe
		
		// rhn-migrate-classic-to-rhsm --help|-h
		command = "rhn-migrate-classic-to-rhsm";
		lang = "en_US"; output = "Usage: rhn-migrate-classic-to-rhsm [OPTIONS]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "de_DE"; output = "(Verwendung|Verbrauch): rhn-migrate-classic-to-rhsm [OPTIONEN]";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"707080"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "es_ES"; output = "Uso: rhn-migrate-classic-to-rhsm [OPCIONES]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "fr_FR"; output = "Utilisation.*rhn-migrate-classic-to-rhsm [OPTIONS]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "it_IT"; output = "Utilizzo: rhn-migrate-classic-to-rhsm [OPZIONI]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ja_JP"; output = "使用法: rhn-migrate-classic-to-rhsm [オプション]";						if (Float.valueOf(clienttasks.redhatReleaseXY)>=7.6f) output = "使用方法: rhn-migrate-classic-to-rhsm [オプション]";
																										helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ko_KR"; output = "사용법: rhn-migrate-classic-to-rhsm [옵션]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "pt_BR"; output = "Uso: rhn-migrate-classic-to-rhsm [OPÇÕES]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ru_RU"; output = "Формат: rhn-migrate-classic-to-rhsm [ПАРАМЕТРЫ]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "zh_CN"; output = "使用：rhn-migrate-classic-to-rhsm [选项]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "zh_TW"; output = "使用方法：rhn-migrate-classic-to-rhsm [選項]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "as_IN"; output = "ব্যৱহাৰ: rhn-migrate-classic-to-rhsm [বিকল্পসমূহ]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "bn_IN"; output = "ব্যবহারপ্রণালী: rhn-migrate-classic-to-rhsm [বিকল্প]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "hi_IN"; output = "प्रयोग: rhn-migrate-classic-to-rhsm [विकल्प]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "mr_IN"; output = "वापर: rhn-migrate-classic-to-rhsm [पर्याय]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "gu_IN"; output = "વપરાશ: rhn-migrate-classic-to-rhsm [વિકલ્પો]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "kn_IN"; output = "ಬಳಕೆ: rhn-migrate-classic-to-rhsm [ಆಯ್ಕೆಗಳು]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ml_IN"; output = "ഉപയോഗിയ്ക്കേണ്ട വിധം: rhn-migrate-classic-to-rhsm [ഐച്ഛികങ്ങള്‍]";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "or_IN"; output = "ଉପଯୋଗ: rhn-migrate-classic-to-rhsm [ବିକଳ୍ପଗୁଡିକ]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "pa_IN"; output = "ਵਰਤੋਂ: rhn-migrate-classic-to-rhsm [ਚੋਣਾਂ]";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "ta_IN"; output = "பயன்பாடு: rhn-migrate-classic-to-rhsm [விருப்பங்கள்]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		lang = "te_IN"; output = "వా‍డుక: rhn-migrate-classic-to-rhsm [ఐచ్చికాలు]";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[(.+)\\]", /*"\\\\[($1|OPTIONS)\\\\]"*/"\\\\[$1\\\\]")+"$")}));
		
		
		// rct stat-cert --help|-h
		// [root@jsefler-6server ~]# for L in en_US de_DE es_ES fr_FR it_IT ja_JP ko_KR pt_BR ru_RU zh_CN zh_TW as_IN bn_IN hi_IN mr_IN gu_IN kn_IN ml_IN or_IN pa_IN ta_IN te_IN; do echo ""; echo "# LANG=$L.UTF-8 rct stat-cert --help | grep -B1 -- '-h, --help'"; LANG=$L.UTF-8 rct stat-cert --help | grep -B1 -- '-h, --help'; done;
		command = "rct stat-cert";
		lang = "en_US"; output = "show this help message and exit";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "de_DE"; output = "Zeige diese Hilfenachricht und beende|Diese Hilfenachricht anzeigen und beenden"/* >= 1.12*/;				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "es_ES"; output = "mostrar este mensaje de ayuda y salir";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "fr_FR"; output = "afficher ce message d'aide et quitter";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "it_IT"; output = "mostra questo messaggio d'aiuto ed esci";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ja_JP"; output = "このヘルプメッセージを表示して終了";							if (Float.valueOf(clienttasks.redhatReleaseXY)>=7.6f) output = "ヘルプメッセージを表示して終了します";
																						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ko_KR"; output = "도움말 보기 및 종료";										helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "pt_BR"; output = "apresenta esta mensagem de ajuda e encerramento";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "ru_RU"; output = "показать это сообщение и выйти";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "zh_CN"; output = "显示此帮助信息并退出";									helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "zh_TW"; output = "顯示此協助訊息並退出";									helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$")}));
		lang = "as_IN"; output = "এই সহায় বাৰ্তা দেখুৱাওক আৰু প্ৰস্থান কৰক";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll(" ", "\\\\s+")+"$")}));	// replace space with \s* to accommodate word wrapping from optparse  Bug 878089: Add line wrapping when listing subscription-manager modules
		lang = "bn_IN"; output = "এই বার্তা প্রদর্শন করে প্রস্থান করা হবে";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll(" ", "\\\\s+")+"$")}));
		lang = "hi_IN"; output = "इस मदद संदेश को दिखाएँ और बाहर हों";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll(" ", "\\\\s+")+"$")}));
		lang = "mr_IN"; output = "हे मदत संदेश दाखवा व बाहेर पडा";									helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll(" ", "\\\\s+")+"$")}));
		lang = "gu_IN"; output = "આ મદદ સંદેશાને બતાવો અને બહાર નીકળો";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll(" ", "\\\\s+")+"$")}));
		lang = "kn_IN"; output = "ಈ ನೆರವಿನ ಸಂದೇಶವನ್ನು ತೋರಿಸಿ ನಿರ್ಗಮಿಸು";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll(" ", "\\\\s+")+"$")}));
		lang = "ml_IN"; output = "ഈ സഹായ സന്ദേശം കാണിച്ചു് പുറത്തു് കടക്കുക";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll(" ", "\\\\s+")+"$")}));
		lang = "or_IN"; output = "ଏହି ସହାୟତା ସନ୍ଦେଶ ଦେଖାନ୍ତୁ ଏବଂ ପ୍ରସ୍ଥାନ କରନ୍ତୁ";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll(" ", "\\\\s+")+"$")}));
		lang = "pa_IN"; output = "ਇਹ ਮੱਦਦ ਸੁਨੇਹਾ ਵਿਖਾਉ ਅਤੇ ਬਾਹਰ ਜਾਉ";							helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll(" ", "\\\\s+")+"$")}));
		lang = "ta_IN"; output = "இந்த உதவி செய்தியை காட்டி வெளியேறு";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll(" ", "\\\\s+")+"$")}));
		lang = "te_IN"; output = "ఈ సహయ సందేశం చూపి నిష్క్రమించు";								helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1074568"}), lang, command+" "+helpOption, 0, newList(output.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll(" ", "\\\\s+")+"$")}));		
		
		// rhsmcertd --help|-h
		// the following logic is also needed in HelpTests.getTranslatedCommandLineHelpDataAsListOfLists()
		if (clienttasks.redhatReleaseX.equals("5"))	helpOptions = Arrays.asList(new String[]{"-?","--help"});	// rhel5	// this logic is also needed in HelpTests.getExpectedCommandLineOptionsDataAsListOfLists()
		// NOTE: THESE TRANSLATIONS ACTUALLY COME FROM /usr/share/locale/*/LC_MESSAGES/glib20.mo (package glib2 NOT subscription-manager)
		command = "rhsmcertd";
		lang = "en_US"; output = "Usage:\\n  rhsmcertd [OPTION...]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "de_DE"; output = "Aufruf:\\n  rhsmcertd [OPTION …]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "es_ES"; output = "Uso:\\n  rhsmcertd [OPCIÓN…]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "fr_FR"; output = "Utilisation :\\n  rhsmcertd [OPTION...]";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {clienttasks.redhatReleaseX.equals("5")? new BlockedByBzBug("969608"):null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "it_IT"; output = "Uso:\\n  rhsmcertd [OPZIONE...]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "ja_JP"; output = "用法:\\n  rhsmcertd [オプション...]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "ko_KR"; output = "사용법:\\n  rhsmcertd [옵션...]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "pt_BR"; output = "Uso:\\n  rhsmcertd [(OPÇÃO|OPÇÕES)...]";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "ru_RU"; output = "Использование:\\n  rhsmcertd [ПАРАМЕТР(…|...)]";	helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "zh_CN"; output = "用法：\\n  rhsmcertd [选项(…|...)]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "zh_TW"; output = "用法：\\n  rhsmcertd [選項(…|...)]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "as_IN"; output = "(ব্যৱহাৰ:|ব্যৱহাৰপ্ৰণালী:)\\n  rhsmcertd [OPTION...]";		helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {clienttasks.redhatReleaseX.equals("5")? new BlockedByBzBug("969608"):null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "bn_IN"; output = "ব্যবহারপ্রণালী:\\n  rhsmcertd [OPTION...]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "hi_IN"; output = "प्रयोग:\\n  rhsmcertd [विकल्प...]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "mr_IN"; output = "वापर:\\n  rhsmcertd [OPTION...]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {clienttasks.redhatReleaseX.equals("5")? new BlockedByBzBug("969608"):null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "gu_IN"; output = "વપરાશ:\\n  rhsmcertd [OPTION...]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "kn_IN"; output = "ಬಳಕೆ:\\n  rhsmcertd [OPTION...]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {clienttasks.redhatReleaseX.equals("5")? new BlockedByBzBug("969608"):null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "ml_IN"; output = "ഉപയോഗിക്കേണ്ട വിധം:\\n  rhsmcertd [OPTION...]";			helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "or_IN"; output = "ବ୍ଯବହାର:\\n  rhsmcertd [ପସନ୍ଦ...]";					helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "pa_IN"; output = "ਵਰਤੋਂ:\\n  rhsmcertd [ਚੋਣ...]";						helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "ta_IN"; output = "பயன்பாடு:\\n  rhsmcertd [OPTION...]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		lang = "te_IN"; output = "వినిమయం:\\n  rhsmcertd [ఇచ్చాపూర్వరకం...]";				helpOption=helpOptions.get(randomGenerator.nextInt(helpOptions.size())); ll.add(Arrays.asList(new Object[] {null, lang, command+" "+helpOption, 0, newList(output.replaceAll("\\.","\\\\.").replaceAll("\\[(.+)\\]", "\\\\[($1|OPTION\\\\.\\\\.\\\\.)\\\\]")+"\\s*$")}));
		
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
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-1")) {	// post commit 630e1a2eb06e6bfacac669ce11f38e228c907ea9 1507030: RestlibExceptions should show they originate server-side
			uErrMsg = ("^"+"HTTP error (401 - Unauthorized): "+servertasks.invalidCredentialsMsg()+"$").replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)");	// HTTP error (401 - Unauthorized): Invalid Credentials
		}
		
		// Object bugzilla, String lang, String username, String password, Integer exitCode, String stdoutRegex, String stderrRegex
		
		// registration test for a user who is invalid
		ll.add(Arrays.asList(new Object[]{null, "en_US.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, uErrMsg}));
		
		// registration test for a user with "invalid credentials" (translated)
		if (sm_serverType.equals(CandlepinType.standalone)) {
			ll.add(Arrays.asList(new Object[]{null,								"en_US.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Invalid Credentials"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("615362"),		"de_DE.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Ungültige Berechtigungsnachweise"}));
			ll.add(Arrays.asList(new Object[]{null,								"es_ES.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Credenciales inválidas"}));
			ll.add(Arrays.asList(new Object[]{null,								"fr_FR.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Informations d’identification non valides"}));	// "Informations d’identification invalides"
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
			String url = "https://www.redhat.com/wapps/ugc/register.html";
			ll.add(Arrays.asList(new Object[]{null,																"en_US.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Invalid username or password. To create a login, please visit {0}".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"706197","1095389"}),				"de_DE.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Ungültiger Benutzername oder Passwort. Um ein Login anzulegen, besuchen Sie bitte {0}".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{null,																"es_ES.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "El nombre de usuario o contraseña es inválido. Para crear un nombre de usuario, por favor visite {0}".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1095389","1292276"}),			"fr_FR.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Nom d'utilisateur ou mot de passe non valide. Pour créer une connexion, veuillez visiter {0}".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{null,																"it_IT.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Nome utente o password non valide. Per creare un login visitare {0}".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1095389"}),						"ja_JP.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "ユーザー名かパスワードが無効です。ログインを作成するには、{0} に進んでください".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"706197","1095389"}),				"ko_KR.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "사용자 이름 또는 암호가 잘못되었습니다. 로그인을 만들려면, {0}으로 이동해 주십시오.".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1095389","1224204"}),			"pt_BR.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, /*"Nome do usuário e senha incorretos. Por favor visite {0} para a criação do logon."*/"Username e senha inválidos. Para criar um login, visite {0}".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),										"ru_RU.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "Неверное имя пользователя или пароль. Для создания учётной записи перейдите к {0}".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1095389","1224204"}),			"zh_CN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, /*"无效用户名或者密码。要创建登录，请访问 {0}"*/"无效用户名或者密码。要生成注册帐户请访问 {0}".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"706197","1095389","1224204"}),	"zh_TW.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, /*"無效的使用者名稱或密碼。若要建立登錄帳號，請至 {0}"*/"無效的使用者名稱或密碼。若要建立一組帳號，請至 {0}".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"706197","1095389"}),				"as_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, /*"অবৈধ ব্যৱহাৰকাৰী নাম অথবা পাছৱাৰ্ড। এটা লগিন সৃষ্টি কৰিবলে, অনুগ্ৰহ কৰি চাওক {0}"*/"অবৈধ ব্যৱহাৰকাৰী নাম অথবা পাছৱৰ্ড। এটা লগিন সৃষ্টি কৰিবলে, অনুগ্ৰহ কৰি চাওক {0}".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"706197","1095389","1224204"}),	"bn_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, /*"ব্যবহারকারীর নাম অথবা পাসওয়ার্ড বৈধ নয়। লগ-ইন প্রস্তুত করার জন্য অনুগ্রহ করে {0} পরিদর্শন করুন"*/"ব্যবহারকারীর নাম অথবা পাসওয়ার্ড বৈধ নয়। লগ-ইন প্রস্তুত করার জন্য, অনুগ্রহ করে {0} পরিদর্শন করুন।".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1095389"}),						"hi_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "अवैध उपयोक्तानाम या कूटशब्द. लॉगिन करने के लिए, कृपया {0} भ्रमण करें".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),										"mr_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "अवैध वापरकर्तानाव किंवा पासवर्ड. प्रवेश निर्माण करण्यासाठी, कृपया {0} येथे भेट द्या".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"706197","1095389"}),				"gu_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "અયોગ્ય વપરાશકર્તાનામ અથવા પાસવર્ડ. લૉગિનને બનાવવા માટે, મહેરબાની કરીને {0} મુલાકાત લો".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1095389"}),						"kn_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "ಅಮಾನ್ಯವಾದ ಬಳಕೆದಾರ ಹೆಸರು ಅಥವ ಗುಪ್ತಪದ. ಒಂದು ಲಾಗಿನ್ ಅನ್ನು ರಚಿಸಲು, ದಯವಿಟ್ಟು {0} ಗೆ ತೆರಳಿ".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),										"ml_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "തെറ്റായ ഉപയോക്തൃനാമം അല്ലെങ്കില്‍ രഹസ്യവാക്ക്. പ്രവേശനത്തിനായി, ദയവായി {0} സന്ദര്‍ശിയ്ക്കുക".replaceFirst("\\{0\\}", url)}));	// "തെറ്റായ ഉപയോക്തൃനാമം അല്ലെങ്കില്<200d> രഹസ്യവാക്ക്. പ്രവേശനത്തിനായി, ദയവായി https://www.redhat.com/wapps/ugc/register.html സന്ദര്<200d>ശിയ്ക്കുക"
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),										"or_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "ଅବୈଧ ଚାଳକନାମ କିମ୍ବା ପ୍ରବେଶ ସଂକେତ। ଗୋଟିଏ ଲଗଇନ ନିର୍ମାଣ କରିବା ପାଇଁ, ଦୟାକରି {0} କୁ ପରିଦର୍ଶନ କରନ୍ତୁ".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),										"pa_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "ਗਲਤ ਯੂਜ਼ਰ-ਨਾਂ ਜਾਂ ਪਾਸਵਰਡ। ਲਾਗਇਨ ਬਣਾਉਣ ਲਈ, ਕਿਰਪਾ ਕਰਕੇ ਇਹ ਵੇਖੋ {0}".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),										"ta_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "தவறான பயனர்பெயர் அல்லது கடவுச்சொல். ஒரு உட்புகுவை உருவாக்குவதற்கு, {0} பார்வையிடவும்".replaceFirst("\\{0\\}", url)}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("706197"),										"te_IN.UTF-8", sm_clientUsername+getRandInt(), sm_clientPassword+getRandInt(), 255, null, "చెల్లని వాడుకరిపేరు లేదా సంకేతపదము. లాగిన్ సృష్టించుటకు, దయచేసి {0} దర్శించండి".replaceFirst("\\{0\\}", url)}));
		}
		// registration test for a user who has not accepted Red Hat's Terms and conditions (translated)  Man, why did you do something?
		if (!sm_usernameWithUnacceptedTC.equals("")) {
			String expectedStderr = "Mensch, warum hast du auch etwas zu tun?? Bitte besuchen https://www.redhat.com/wapps/ugc!!!!!!!!!!!!!!!!!!";
			expectedStderr = "Sie müssen zuerst die allgemeinen Geschäftsbedingungen von Red Hat akzeptieren. Bitte besuchen Sie https://www.redhat.com/wapps/ugc. Sie müssen sich gegebenenfalls vom Kundenportal abmelden und anschließend wieder anmelden, um die allgemeinen Geschäftsbedingungen zu sehen.";
			expectedStderr = "Sie müssen zuerst die allgemeinen Geschäftsbedingungen von Red Hat akzeptieren. Bitte besuchen Sie https://www.redhat.com/wapps/tnc/termsack?event[]=signIn. Sie müssen sich gegebenenfalls vom Kundenportal abmelden und anschließend wieder anmelden, um die allgemeinen Geschäftsbedingungen zu sehen.".replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?");	// after IT Adapter fix from Bug 1458423 
			if (sm_serverType.equals(CandlepinType.hosted))	ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"615362","642805","1089034","1095389","1458423"}),"de_DE.UTF-8", sm_usernameWithUnacceptedTC, sm_passwordWithUnacceptedTC, 255, null, expectedStderr}));
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
		
		// for all rows, change the expected exitCode when testing post subscription-manager-1.13.8-1
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			for (List<Object> l : ll) {
				// Object bugzilla, String lang, String username, String password, Integer exitCode, String stdoutRegex, String stderrRegex
				BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);	// get the existing BlockedByBzBug
				List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
				bugIds.add("1119688");	// Bug 1119688 - [RFE] subscription-manager better usability for scripts
				blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
				l.set(0, blockedByBzBug);
				l.set(4, new Integer(70));	// EX_SOFTWARE
			}
		}
		
		return ll;
	}

}
