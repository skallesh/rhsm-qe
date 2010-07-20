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

public class RegisterTests extends SubscriptionManagerTestScript {
	
	@Test(	description="subscription-manager-cli: register to a Candlepin server using bogus credentials",
			dataProvider="getInvalidRegistrationData",
			groups={"sm_stage1"})
	@ImplementsTCMS(id="41691, 47918")
	public void InvalidRegistration_Test(String username, String password, String type, String consumerId, Boolean autosubscribe, Boolean force, String debug, String stdoutRegex, String stderrRegex) {
		log.info("Testing registration to a Candlepin server using bogus credentials.");
		sm1.register(username, password, type, consumerId, autosubscribe, force);
		if (stdoutRegex!=null) Assert.assertContainsMatch(cl1.getStdout(), stdoutRegex);
		if (stderrRegex!=null) Assert.assertContainsMatch(cl1.getStderr(), stderrRegex);
	}
	
	
	@Test(	description="subscription-manager-cli: attempt to register to a Candlepin server using bogus credentials and check for localized strings results",
			dataProvider="getInvalidRegistrationWithLocalizedStringsData",
			groups={"sm_stage1", "sprint9-script"})
	public void InvalidRegistrationWithLocalizedStrings_Test(String lang, String username, String password, Integer exitCode, String stdoutRegex, String stderrRegex) {
		if (serverStandalone) throw new SkipException("This testcase was designed for an IT candlepin server, not a standalone candlepin server.");
//		sm.unregister();
//		this.runRHSMCallAsLang(lang,"subscription-manager-cli register --force --username="+username+" --password="+password);
//		String stdErr = sshCommandRunner.getStderr();
//		Assert.assertTrue(stdErr.contains(expectedMessage),
//				"Actual localized error message from failed registration: "+stdErr+" as language "+lang+" matches: "+expectedMessage);
		
		log.info("Testing the registration to a candlepin server using invalid credentials and expecting results in language "+lang+". ");
		RemoteFileTasks.runCommandAndAssert(cl1, "export LANG="+lang+"; subscription-manager-cli register --force --username="+username+" --password="+password, exitCode, stdoutRegex, stderrRegex);
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
	
	
	@Test(	description="subscription-manager-cli: register to a Candlepin server",
			dependsOnGroups={"sm_stage1"},
			groups={"sm_stage2"},
			alwaysRun=true)
	@ImplementsTCMS(id="41677")
	public void ValidRegistration_Test() {
		sm1.register(client1username, client1password, null, null, null, Boolean.TRUE);
		
		// assert certificate files are dropped into /etc/pki/consumer
		Assert.assertEquals(
					cl1.runCommandAndWait("stat /etc/pki/consumer/key.pem").intValue(), 0,
							"/etc/pki/consumer/key.pem is present after register");
		Assert.assertEquals(
					cl1.runCommandAndWait("stat /etc/pki/consumer/cert.pem").intValue(),0,
							"/etc/pki/consumer/cert.pem is present after register");
	}
	
	String autosubscribeProdCertFile =  "/etc/pki/product/"+"autosubscribeProdCert-"+/*getRandInt()+*/".pem";
	@Test(	description="subscription-manager-cli: register to a Candlepin server using autosubscribe functionality",
			groups={"sm_stage1", "sprint9-script", "blockedByBug-602378", "blockedByBug-616137"},
			alwaysRun=true)
	public void ValidRegistrationAutosubscribe_Test() {
		if (serverStandalone) throw new SkipException("This testcase was designed for an IT candlepin server, not a standalone candlepin server.");
		sm1.unregister();
//		String autoProdCert = "/etc/pki/product/autoProdCert-"+getRandInt()+".pem";
		String autoProdCert = autosubscribeProdCertFile;
//		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/product/"+autoProdCert);
		teardownAfterGroups_sm_stage1(); 	// will remove the autosubscribeProdCertFile
		cl1.runCommandAndWait("wget -O "+autoProdCert+" "+prodCertLocation);
		sm1.register(client1username, client1password, null, null, Boolean.TRUE, null);
		// assert that the stdout from the registration includes: Bind Product  Red Hat Directory Server 75822
		Assert.assertContainsMatch(cl1.getStdout(),
				"Bind Product  "+prodCertProduct, "Stdout from the register command contains binding to the expected product.");
		// assert the bound product is reported in the consumed product listing
		Assert.assertTrue(sm1.getCurrentlyConsumedProductSubscriptions().contains(new ProductSubscription(prodCertProduct, null)),
				"Expected product "+this.prodCertProduct+" appears in list --consumed call after autosubscribe");
//		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/product/"+autoProdCert);
	}
	@AfterGroups (value={"sm_stage1"}, alwaysRun=true)
	public void teardownAfterGroups_sm_stage1() {
		cl1.runCommandAndWait("rm -f "+autosubscribeProdCertFile);
	}
	
	
	
	// Data Providers ***********************************************************************

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
		ll.add(Arrays.asList(new Object[]{"",			"",								null,	null,	null,	Boolean.TRUE,	null,	"Error: username and password are required to register,try --help.",	null}));
		ll.add(Arrays.asList(new Object[]{client1username,		"",								null,	null,	null,	Boolean.TRUE,	null,	"Error: username and password are required to register,try --help.",	null}));
		ll.add(Arrays.asList(new Object[]{"",			client1password,						null,	null,	null,	Boolean.TRUE,	null,	"Error: username and password are required to register,try --help.",	null}));
		ll.add(Arrays.asList(new Object[]{client1username,		String.valueOf(getRandInt()),	null,	null,	null,	Boolean.TRUE,	null,	null,																	"Invalid username or password"}));
		ll.add(Arrays.asList(new Object[]{client1username+"X",	String.valueOf(getRandInt()),	null,	null,	null,	Boolean.TRUE,	null,	null,																	"Invalid username or password"}));
		ll.add(Arrays.asList(new Object[]{client1username,		String.valueOf(getRandInt()),	null,	null,	null,	Boolean.TRUE,	null,	null,																	"Invalid username or password"}));

		// force a successful registration, and then...
		// FIXME: https://bugzilla.redhat.com/show_bug.cgi?id=616065
		ll.add(Arrays.asList(new Object[]{client1username,		client1password,						null,	null,	null,	Boolean.TRUE,	null,	"([a-z,0-9,\\-]*) "+client1username+" "+client1username,								null}));

		// ... try to register again even though the system is already registered
		ll.add(Arrays.asList(new Object[]{client1username,		client1password,						null,	null,	null,	Boolean.FALSE,	null,	"This system is already registered. Use --force to override",			null}));

		return ll;
	}
	
	@DataProvider(name="getInvalidRegistrationWithLocalizedStringsData")
	public Object[][] getInvalidRegistrationWithLocalizedStringsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getInvalidRegistrationWithLocalizedStringsAsListOfLists());
	}
	protected List<List<Object>> getInvalidRegistrationWithLocalizedStringsAsListOfLists(){
		List<List<Object>> ll = new ArrayList<List<Object>>();

		// String lang, String username, String password, Integer exitCode, String stdoutRegex, String stderrRegex
		ll.add(Arrays.asList(new Object[]{"en_US.UTF8", client1username+getRandInt(), client1password+getRandInt(), 255, null, "Invalid username or password. To create a login, please visit https://www.redhat.com/wapps/ugc/register.html"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("615362","de_DE.UTF8", client1username+getRandInt(), client1password+getRandInt(), 255, null, "Ungültiger Benutzername oder Kennwort. So erstellen Sie ein Login, besuchen Sie bitte https://www.redhat.com/wapps/ugc")}));
		
		// registration test for a user who has no accepted Red Hat's Terms and conditions
		ll.add(Arrays.asList(new Object[]{"en_US.UTF8", tcUnacceptedUsername, tcUnacceptedPassword, 255, null, "You must first accept Red Hat's Terms and conditions. Please visit https://www.redhat.com/wapps/ugc"}));

		// registration test for a user who has been disabled
		ll.add(Arrays.asList(new Object[]{"en_US.UTF8", "ssalevan", "redhat", 255, null,"The user has been disabled, if this is a mistake, please contact customer service."}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("615362","de_DE.UTF8", tcUnacceptedUsername, tcUnacceptedPassword, 255, null, "Mensch, warum hast du auch etwas zu tun?? Bitte besuchen https://www.redhat.com/wapps/ugc!!!!!!!!!!!!!!!!!!")}));

		return ll;
	}
	
}
