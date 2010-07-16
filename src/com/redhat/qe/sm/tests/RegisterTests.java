package com.redhat.qe.sm.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.abstractions.ProductSubscription;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.tools.RemoteFileTasks;

public class RegisterTests extends SubscriptionManagerTestScript {
	
//	@Test(	description="subscription-manager-cli: register to a Candlepin server using bogus credentials",
//			dataProvider="invalidRegistrationTest",
//			groups={"sm_stage1"})
//	@ImplementsTCMS(id="41691, 47918")
//	public void InvalidRegistration_Test(String username, String password, String type, String consumerId, Boolean autosubscribe, Boolean force, String debug, String stdoutRegex, String stderrRegex) {
//		log.info("Testing registration to a Candlepin server using bogus credentials.");
//		sm.registerToCandlepin(username, password, type, consumerId, autosubscribe, force);
//		if (stdoutRegex!=null) Assert.assertContainsMatch(sshCommandRunner.getStdout(), stdoutRegex);
//		if (stderrRegex!=null) Assert.assertContainsMatch(sshCommandRunner.getStderr(), stderrRegex);
//	}
	
	
	@Test(	description="subscription-manager-cli: register to a Candlepin server using bogus credentials, check for localized strings",
			dataProvider="invalidRegistrationLocalizedTest",
			groups={"sm_stage1", "sprint9-script", "only-IT"})
	public void InvalidRegistrationLocalized_Test(String lang, String expectedMessage) {
		sm.unregisterFromCandlepin();
		this.runRHSMCallAsLang(lang,"subscription-manager-cli register --force --username="+username+getRandInt()+" --password="+password+getRandInt());
		String stdErr = sshCommandRunner.getStderr();
		Assert.assertTrue(stdErr.contains(expectedMessage),
				"Actual localized error message from failed registration: "+stdErr+" as language "+lang+" matches: "+expectedMessage);
	}
	
	
	@Test(	description="subscription-manager-cli: register to a Candlepin server using a user who hasn't accepted terms and conditions, check for localized strings",
			dataProvider="invalidRegistrationTermsAndConditionsLocalizedTest",
			groups={"sm_stage1", "sprint9-script", "only-IT"})
	public void InvalidRegistrationTermsAndConditionsLocalized_Test(String lang, String expectedMessage) {
		sm.unregisterFromCandlepin();
		this.runRHSMCallAsLang(lang, "subscription-manager-cli register --force --username="+tcUnacceptedUsername+" --password="+tcUnacceptedPassword);
		String stdErr = sshCommandRunner.getStderr();
		Assert.assertTrue(stdErr.contains(expectedMessage),
				"Actual localized error message from unaccepted T&Cs registration: "+stdErr+" as language "+lang+" matches: "+expectedMessage);
	}
	
	
	@Test(	description="subscription-manager-cli: register to a Candlepin server",
			dependsOnGroups={"sm_stage1"},
			groups={"sm_stage2"},
			alwaysRun=true)
	@ImplementsTCMS(id="41677")
	public void ValidRegistration_Test() {
		sm.registerToCandlepin(username, password, null, null, null, Boolean.TRUE);
		
		// assert certificate files are dropped into /etc/pki/consumer
		Assert.assertEquals(
					sshCommandRunner.runCommandAndWait("stat /etc/pki/consumer/key.pem").intValue(), 0,
							"/etc/pki/consumer/key.pem is present after register");
		Assert.assertEquals(
					sshCommandRunner.runCommandAndWait("stat /etc/pki/consumer/cert.pem").intValue(),0,
							"/etc/pki/consumer/cert.pem is present after register");
	}
	
	
	@Test(	description="subscription-manager-cli: register to a Candlepin server using autosubscribe functionality",
			groups={"sm_stage1", "sprint9-script", "only-IT", "blockedByBug-602378"},
			alwaysRun=true)
	public void ValidRegistrationAutosubscribe_Test() {
		sm.unregisterFromCandlepin();
		String autoProdCert = "autoProdCert-"+getRandInt()+".pem";
		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/product/"+autoProdCert);
		sshCommandRunner.runCommandAndWait("wget -O /etc/pki/product/"+autoProdCert+" "+this.prodCertLocation);
		sm.registerToCandlepin(username, password, null, null, Boolean.TRUE, null);
		Assert.assertTrue(sm.getCurrentlyConsumedProductSubscriptions().contains(new ProductSubscription(this.prodCertProduct, null)),
				"Expected product "+this.prodCertProduct+" appears in list --consumed call after autosubscribe");
		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/product/"+autoProdCert);
	}
	
	
	
	// Data Providers ***********************************************************************

	@DataProvider(name="invalidRegistrationTermsAndConditionsLocalizedTest")
	public Object[][] getInvalidRegistrationTermsAndConditionsLocalizedDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getInvalidRegistrationTermsAndConditionsLocalizedDataAsListOfLists());
	}
	protected List<List<Object>> getInvalidRegistrationTermsAndConditionsLocalizedDataAsListOfLists(){
		List<List<Object>> ll = new ArrayList<List<Object>>();
		ll.add(Arrays.asList(new Object[]{"en_US.UTF8","You must first accept Red Hat's Terms and conditions. Please visit https://www.redhat.com/wapps/ugc"}));
		ll.add(Arrays.asList(new Object[]{"de_DE.UTF8","Mensch, warum hast du auch etwas zu tun?? Bitte besuchen https://www.redhat.com/wapps/ugc!!!!!!!!!!!!!!!!!!"}));
		return ll;
	}
	
	@DataProvider(name="invalidRegistrationLocalizedTest")
	public Object[][] getInvalidRegistrationLocalizedDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getInvalidRegistrationLocalizedDataAsListOfLists());
	}
	protected List<List<Object>> getInvalidRegistrationLocalizedDataAsListOfLists(){
		List<List<Object>> ll = new ArrayList<List<Object>>();
		ll.add(Arrays.asList(new Object[]{"en_US.UTF8","Invalid username or password. To create a login, please visit https://www.redhat.com/wapps/ugc/register.html"}));
		ll.add(Arrays.asList(new Object[]{"de_DE.UTF8","Ungültiger Benutzername oder Kennwort. So erstellen Sie ein Login, besuchen Sie bitte https://www.redhat.com/wapps/ugc"}));
		return ll;
	}
	
	@DataProvider(name="invalidRegistrationTest")
	public Object[][] getInvalidRegistrationDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getInvalidRegistrationDataAsListOfLists());
	}
	protected List<List<Object>> getInvalidRegistrationDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		// String username, String password, String type, String consumerId, Boolean autosubscribe, Boolean force, String debug, String stdoutRegex, String stderrRegex
		ll.add(Arrays.asList(new Object[]{"",			"",				null,	null,	null,	Boolean.TRUE,	null,	"Error: username and password are required to register,try --help.",	null}));
		ll.add(Arrays.asList(new Object[]{username,		"",				null,	null,	null,	Boolean.TRUE,	null,	"Error: username and password are required to register,try --help.",	null}));
		ll.add(Arrays.asList(new Object[]{"",			password,		null,	null,	null,	Boolean.TRUE,	null,	"Error: username and password are required to register,try --help.",	null}));
		ll.add(Arrays.asList(new Object[]{username+"X",	password+"X",	null,	null,	null,	Boolean.TRUE,	null,	null,																	"Invalid username or password"}));

		// force a successful registration, and then...
		ll.add(Arrays.asList(new Object[]{username,		password,		null,	null,	null,	Boolean.TRUE,	null,	"([a-z,0-9,\\-]*) "+username+" "+username,								null}));

		// ... try to register again even though the system is already registered
		ll.add(Arrays.asList(new Object[]{username,		password,		null,	null,	null,	Boolean.FALSE,	null,	"This system is already registered. Use --force to override",			null}));

		return ll;
	}
}
