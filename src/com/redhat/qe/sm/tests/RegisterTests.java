package com.redhat.qe.sm.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testng.BlockedByBzBug;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.abstractions.ProductSubscription;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandRunner;

@Test(groups={"register"})
public class RegisterTests extends SubscriptionManagerTestScript {
	
	@Test(	description="subscription-manager-cli: register to a Candlepin server using various options and data",
			dataProvider="getRegistrationData")
	@ImplementsTCMS(id="41677, 41691, 47918")
	public void Registration_Test(String username, String password, String type, String consumerId, Boolean autosubscribe, Boolean force, String debug, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex) {
		log.info("Testing registration to a Candlepin using various options and data and asserting various expected results.");
		
		// assure we are unregistered
//		clienttasks.unregister();
		
		// attempt the registration
		Integer actualExitCode = null;
		if (expectedExitCode!=null || expectedStdoutRegex!=null || expectedStderrRegex!=null) {
			try {
				actualExitCode = clienttasks.register(username, password, type, consumerId, autosubscribe, force);
			} catch (AssertionError ae) {
				// TODO: handle exception
			}
		} else {
			actualExitCode = clienttasks.register(username, password, type, consumerId, autosubscribe, force);
		}
		
		if (expectedExitCode!=null)		Assert.assertEquals(actualExitCode, expectedExitCode);
		if (expectedStdoutRegex!=null)	Assert.assertContainsMatch(client.getStdout().trim(), expectedStdoutRegex);
		if (expectedStderrRegex!=null)	Assert.assertContainsMatch(client.getStderr().trim(), expectedStderrRegex);
	}
	
	@Test(	description="subscription-manager-cli: register to a Candlepin server using bogus credentials",
//			groups={"sm_stage1"},
			dataProvider="getInvalidRegistrationData")
	@ImplementsTCMS(id="41691, 47918")
	public void InvalidRegistration_Test(String username, String password, String type, String consumerId, Boolean autosubscribe, Boolean force, String debug, String stdoutRegex, String stderrRegex) {
		log.info("Testing registration to a Candlepin server using bogus credentials.");
		clienttasks.register(username, password, type, consumerId, autosubscribe, force);
		if (stdoutRegex!=null) Assert.assertContainsMatch(client.getStdout().trim(), stdoutRegex);
		if (stderrRegex!=null) Assert.assertContainsMatch(client.getStderr().trim(), stderrRegex);
	}
	
	
	@Test(	description="subscription-manager-cli: attempt to register to a Candlepin server using bogus credentials and check for localized strings results",
//			groups={"sm_stage1", "sprint9-script"},
			dataProvider="getInvalidRegistrationWithLocalizedStringsData")
	public void InvalidRegistrationWithLocalizedStrings_Test(String lang, String username, String password, Integer exitCode, String stdoutRegex, String stderrRegex) {
//		sm.unregister();
//		this.runRHSMCallAsLang(lang,"subscription-manager-cli register --force --username="+username+" --password="+password);
//		String stdErr = sshCommandRunner.getStderr();
//		Assert.assertTrue(stdErr.contains(expectedMessage),
//				"Actual localized error message from failed registration: "+stdErr+" as language "+lang+" matches: "+expectedMessage);
		
		log.info("Testing the registration to a candlepin server using invalid credentials and expecting results in language "+lang+". ");
		RemoteFileTasks.runCommandAndAssert(client, "export LANG="+lang+"; subscription-manager-cli register --force --username="+username+" --password="+password, exitCode, stdoutRegex, stderrRegex);
//		Assert.assertEquals(sshCommandRunner.getStderr().trim(),expectedMessage,
//				"Testing the registration to a candlepin server using invalid credentials and expecting results in language "+lang+". ");
	}
	
// FIXME DELETEME
//	@Test(	description="subscription-manager-cli: register to a Candlepin server using a user who hasn't accepted terms and conditions, check for localized strings",
//			dataProvider="invalidRegistrationTermsAndConditionsLocalizedTest",
//			groups={"sm_stage1", "sprint9-script", "only-IT"})
//	public void InvalidRegistrationTermsAndConditionsLocalized_Test(String username, String password, String lang, Integer exitCode, String stdoutRegex, String stderrRegex) {
//		sm.unregister();
////		this.runRHSMCallAsLang(lang, "subscription-manager-cli register --force --username="+username+" --password="+password);
////		String stdErr = sshCommandRunner.getStderr();
////		Assert.assertTrue(stdErr.contains(expectedMessage),
////				"Actual localized error message from unaccepted T&Cs registration: "+stdErr+" as language "+lang+" matches: "+expectedMessage);
//		log.info("Testing the registration to a candlepin server using bogus credentials and expecting results in language "+lang+". ");
//		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "export LANG="+lang+"; subscription-manager-cli register --force --username="+username+" --password="+password, exitCode, stdoutRegex, stderrRegex);
////		Assert.assertEquals(sshCommandRunner.getStderr().trim(),expectedMessage,
////				"Testing the registration to a candlepin server using bogus credentials and expecting results in language "+lang+". ");
//	}
	

// FIXME DELETEME	Replaced by Registration_Test
//	@Test(	description="subscription-manager-cli: register to a Candlepin server",
////			dependsOnGroups={"sm_stage1"},
////			groups={"sm_stage2"},
////			alwaysRun=true,
//			enabled=true)
//	@ImplementsTCMS(id="41677")
//	public void ValidRegistration_Test() {
//		clienttasks.register(clientusername, clientpassword, null, null, null, Boolean.TRUE);
//		
//		// assert certificate files are dropped into /etc/pki/consumer
//		Assert.assertEquals(client.runCommandAndWait("stat /etc/pki/consumer/key.pem").intValue(), 0,
//				"/etc/pki/consumer/key.pem is present after register");
//		Assert.assertEquals(client.runCommandAndWait("stat /etc/pki/consumer/cert.pem").intValue(),0,
//				"/etc/pki/consumer/cert.pem is present after register");
//	}
	
	String autosubscribeProdCertFile =  "/etc/pki/product/"+"autosubscribeProdCert-"+/*getRandInt()+*/".pem";
	@Test(	description="subscription-manager-cli: register to a Candlepin server using autosubscribe functionality",
//			groups={"sm_stage1", "sprint9-script", "blockedByBug-602378", "blockedByBug-616137"},
//			alwaysRun=true,
			groups={"ValidRegistrationAutosubscribe_Test","blockedByBug-602378", "blockedByBug-616137"},
			enabled=true)
	public void ValidRegistrationAutosubscribe_Test() {
		if (isServerOnPremises) throw new SkipException("This testcase was designed for an IT candlepin server, not a standalone candlepin server.");
		clienttasks.unregister();
//		String autoProdCert = "/etc/pki/product/autoProdCert-"+getRandInt()+".pem";
		String autoProdCert = autosubscribeProdCertFile;
//		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/product/"+autoProdCert);
		teardownAfterValidRegistrationAutosubscribe_Test(); 	// will remove the autosubscribeProdCertFile
		client.runCommandAndWait("wget -O "+autoProdCert+" "+prodCertLocation);
		clienttasks.register(clientusername, clientpassword, null, null, Boolean.TRUE, null);
		// assert that the stdout from the registration includes: Bind Product  Red Hat Directory Server 75822
		Assert.assertContainsMatch(client.getStdout(),
				"Bind Product  "+prodCertProduct, "Stdout from the register command contains binding to the expected product.");
		// assert the bound product is reported in the consumed product listing
		Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().contains(new ProductSubscription(prodCertProduct, null)),
				"Expected product "+this.prodCertProduct+" appears in list --consumed call after autosubscribe");
//		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/product/"+autoProdCert);
	}
	@AfterGroups (value={"ValidRegistrationAutosubscribe_Test"}, alwaysRun=true)
	public void teardownAfterValidRegistrationAutosubscribe_Test() {
		client.runCommandAndWait("rm -f "+autosubscribeProdCertFile);
	}
	
	// protected methods ***********************************************************************

	protected void checkInvalidRegistrationStrings(SSHCommandRunner sshCommandRunner, String username, String password){
		sshCommandRunner.runCommandAndWait("subscription-manager-cli register --username="+username+this.getRandInt()+" --password="+password+this.getRandInt()+" --force");
		Assert.assertContainsMatch(sshCommandRunner.getStdout(),
				"Invalid username or password. To create a login, please visit https:\\/\\/www.redhat.com\\/wapps\\/ugc\\/register.html");
	}
	
	// Data Providers ***********************************************************************

	@DataProvider(name="getRegistrationData")
	public Object[][] getRegistrationDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRegistrationDataAsListOfLists());
	}
	protected List<List<Object>> getRegistrationDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		// String username, String password, String type, String consumerId, Boolean autosubscribe, Boolean force, String debug, Integer exitCode, String stdoutRegex, String stderrRegex
		// 									username,			password,						type,	consumerId,	autosubscribe,	force,			debug,	exitCode,	stdoutRegex,																	stderrRegex
		ll.add(Arrays.asList(new Object[]{	"",					"",								null,	null,		null,			Boolean.TRUE,	null,	null,		"Error: username and password are required to register, try register --help.",	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,		"",								null,	null,		null,			Boolean.TRUE,	null,	null,		"Error: username and password are required to register, try register --help.",	null}));
		ll.add(Arrays.asList(new Object[]{	"",					clientpassword,					null,	null,		null,			Boolean.TRUE,	null,	null,		"Error: username and password are required to register, try register --help.",	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,		String.valueOf(getRandInt()),	null,	null,		null,			Boolean.TRUE,	null,	null,		null,																			"Invalid username or password"}));
		ll.add(Arrays.asList(new Object[]{	clientusername+"X",	String.valueOf(getRandInt()),	null,	null,		null,			Boolean.TRUE,	null,	null,		null,																			"Invalid username or password"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,		String.valueOf(getRandInt()),	null,	null,		null,			Boolean.TRUE,	null,	null,		null,																			"Invalid username or password"}));

		// force a successful registration, and then...
		// FIXME: https://bugzilla.redhat.com/show_bug.cgi?id=616065
		ll.add(Arrays.asList(new Object[]{	clientusername,		clientpassword,					null,	null,		null,			Boolean.TRUE,	null,	null,		"[a-f,0-9,\\-]{36} "+clientusername,											null}));	// https://bugzilla.redhat.com/show_bug.cgi?id=616065

		// ... try to register again even though the system is already registered
		ll.add(Arrays.asList(new Object[]{	clientusername,		clientpassword,					null,	null,		null,			Boolean.FALSE,	null,	null,		"This system is already registered. Use --force to override",					null}));

		if (isServerOnPremises) {
			ll.add(Arrays.asList(new Object[]{	"admin",					"admin",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
		}
		else {	// user data comes from https://engineering.redhat.com/trac/IntegratedMgmtQE/wiki/imanage_qe_IT_data_spec
			ll.add(Arrays.asList(new Object[]{	"ewayte",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"sehuffman",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"epgyadmin",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"onthebus",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"epgy_bsears",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"Dadaless",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"emmapease",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"aaronwen",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"davidmcmath",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"cfairman2",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"macfariman",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"isu-ardwin",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"isu-paras",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"isuchaos",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"isucnc",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"isu-thewags",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"isu-sukhoy",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"isu-debrm",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"isu-acoster",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"isunpappas",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"isujdwarn",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"pascal.catric@a-sis.com",	"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"xeops",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"xeops-js",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"xeop-stenjoa",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"tmgedp",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"jmarra",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"nisadmin",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"darkrider1",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"test5",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"amy_redhat2",				"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"test_1",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"test2",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));
			ll.add(Arrays.asList(new Object[]{	"test3",					"redhat",			null,	null,		null,			Boolean.TRUE,	null,	null,		null,					null}));

		}
		return ll;
	}
	
// FIXME DELETEME	
//	@DataProvider(name="invalidRegistrationTermsAndConditionsLocalizedTest")
//	public Object[][] getInvalidRegistrationTermsAndConditionsLocalizedDataAs2dArray() {
//		return TestNGUtils.convertListOfListsTo2dArray(getInvalidRegistrationTermsAndConditionsLocalizedDataAsListOfLists());
//	}
//	protected List<List<Object>> getInvalidRegistrationTermsAndConditionsLocalizedDataAsListOfLists(){
//		List<List<Object>> ll = new ArrayList<List<Object>>();
//		// String username, String password, String lang, String expectedMessage
//		// String username, String password, String lang, Integer exitCode, String stdoutRegex, String stderrRegex
//		ll.add(Arrays.asList(new Object[]{tcUnacceptedUsername, tcUnacceptedPassword, 255, null, "en_US.UTF8","You must first accept Red Hat's Terms and conditions. Please visit https://www.redhat.com/wapps/ugc"}));
//		ll.add(Arrays.asList(new Object[]{"ssalevan", "redhat", "en_US.UTF8", 255, null,"The user has been disabled, if this is a mistake, please contact customer service."}));
//		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("615362",tcUnacceptedUsername, tcUnacceptedPassword, 255, null, "de_DE.UTF8","Mensch, warum hast du auch etwas zu tun?? Bitte besuchen https://www.redhat.com/wapps/ugc!!!!!!!!!!!!!!!!!!")}));
//		return ll;
//	}
	

	@DataProvider(name="getInvalidRegistrationData")
	public Object[][] getInvalidRegistrationDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getInvalidRegistrationDataAsListOfLists());
	}
	protected List<List<Object>> getInvalidRegistrationDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		// String username, String password, String type, String consumerId, Boolean autosubscribe, Boolean force, String debug, String stdoutRegex, String stderrRegex
		ll.add(Arrays.asList(new Object[]{"",					"",								null,	null,	null,	Boolean.TRUE,	null,	"Error: username and password are required to register, try register --help.",	null}));
		ll.add(Arrays.asList(new Object[]{clientusername,		"",								null,	null,	null,	Boolean.TRUE,	null,	"Error: username and password are required to register, try register --help.",	null}));
		ll.add(Arrays.asList(new Object[]{"",					clientpassword,					null,	null,	null,	Boolean.TRUE,	null,	"Error: username and password are required to register, try register --help.",	null}));
		ll.add(Arrays.asList(new Object[]{clientusername,		String.valueOf(getRandInt()),	null,	null,	null,	Boolean.TRUE,	null,	null,																	"Invalid username or password"}));
		ll.add(Arrays.asList(new Object[]{clientusername+"X",	String.valueOf(getRandInt()),	null,	null,	null,	Boolean.TRUE,	null,	null,																	"Invalid username or password"}));
		ll.add(Arrays.asList(new Object[]{clientusername,		String.valueOf(getRandInt()),	null,	null,	null,	Boolean.TRUE,	null,	null,																	"Invalid username or password"}));

		// force a successful registration, and then...
		// FIXME: https://bugzilla.redhat.com/show_bug.cgi?id=616065
		ll.add(Arrays.asList(new Object[]{clientusername,		clientpassword,						null,	null,	null,	Boolean.TRUE,	null,	"[a-f,0-9,\\-]{36} "+clientusername,								null}));	// https://bugzilla.redhat.com/show_bug.cgi?id=616065

		// ... try to register again even though the system is already registered
		ll.add(Arrays.asList(new Object[]{clientusername,		clientpassword,						null,	null,	null,	Boolean.FALSE,	null,	"This system is already registered. Use --force to override",		null}));

		return ll;
	}
	
	@DataProvider(name="getInvalidRegistrationWithLocalizedStringsData")
	public Object[][] getInvalidRegistrationWithLocalizedStringsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getInvalidRegistrationWithLocalizedStringsAsListOfLists());
	}
	protected List<List<Object>> getInvalidRegistrationWithLocalizedStringsAsListOfLists(){
		List<List<Object>> ll = new ArrayList<List<Object>>();

		// String lang, String username, String password, Integer exitCode, String stdoutRegex, String stderrRegex
		if (!isServerOnPremises) ll.add(Arrays.asList(new Object[]{"en_US.UTF8", clientusername+getRandInt(), clientpassword+getRandInt(), 255, null, "Invalid username or password. To create a login, please visit https://www.redhat.com/wapps/ugc/register.html"}));
		if (isServerOnPremises)  ll.add(Arrays.asList(new Object[]{"en_US.UTF8", clientusername+getRandInt(), clientpassword+getRandInt(), 255, null, "Invalid username or password"}));	// FIXME: I don't know why this message is dependent if server is On Premises - jsefler
		if (!isServerOnPremises) ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("615362","de_DE.UTF8", clientusername+getRandInt(), clientpassword+getRandInt(), 255, null, "Ungültiger Benutzername oder Kennwort. So erstellen Sie ein Login, besuchen Sie bitte https://www.redhat.com/wapps/ugc")}));
		
		// registration test for a user who has no accepted Red Hat's Terms and conditions
		if (!isServerOnPremises) ll.add(Arrays.asList(new Object[]{"en_US.UTF8", tcUnacceptedUsername, tcUnacceptedPassword, 255, null, "You must first accept Red Hat's Terms and conditions. Please visit https://www.redhat.com/wapps/ugc"}));

		// registration test for a user who has been disabled
		if (!isServerOnPremises) ll.add(Arrays.asList(new Object[]{"en_US.UTF8", "xeops-js", "redhat", 255, null,"The user has been disabled, if this is a mistake, please contact customer service."}));
		if (!isServerOnPremises) ll.add(Arrays.asList(new Object[]{"en_US.UTF8", "ssalevan", "redhat", 255, null,"The user has been disabled, if this is a mistake, please contact customer service."}));
		if (!isServerOnPremises) ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("615362","de_DE.UTF8", tcUnacceptedUsername, tcUnacceptedPassword, 255, null, "Mensch, warum hast du auch etwas zu tun?? Bitte besuchen https://www.redhat.com/wapps/ugc!!!!!!!!!!!!!!!!!!")}));

		return ll;
	}
	
}
