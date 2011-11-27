package com.redhat.qe.sm.cli.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.BlockedByBzBug;
import com.redhat.qe.auto.testng.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.CandlepinType;
import com.redhat.qe.sm.base.SubscriptionManagerBaseTestScript;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * Reference: flowchart https://docspace.corp.redhat.com/docs/DOC-65246
 */
@Test(groups={"ActivateTests"})
public class ActivateTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager: verify activate requires registration",
			groups={},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void AttemptActivateWithoutBeingRegistered_Test() {
		
		clienttasks.unregister(null,null,null);
		SSHCommandResult activateResult = clienttasks.activate_(null,null,null,null,null);
		
		// assert redemption results
		Assert.assertEquals(activateResult.getStdout().trim(), "Error: You need to register this system by running `register` command before using this option.","Activate should require that the system be registered.");
		Assert.assertEquals(activateResult.getExitCode(), Integer.valueOf(1),"Exit code from activate when executed against a standalone candlepin server.");
	}
	
	@Test(	description="subscription-manager: attempt activate without --email option",
			groups={"blockedByBug-727600"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptActivateWithoutEmail_Test() {
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, (String)null, true, false, null, null, null);
		SSHCommandResult activateResult = clienttasks.activate_(null,null,null,null,null);
		
		// assert redemption results
		//Assert.assertEquals(activateResult.getStdout().trim(), "email and email_locale are required for notification","Activate should require that the email option be specified.");
		Assert.assertEquals(activateResult.getStdout().trim(), "email is required for notification","Activate should require that the email option be specified.");
		Assert.assertEquals(activateResult.getExitCode(), Integer.valueOf(255),"Exit code from activate when executed against without an email option.");

	}
	
	@Test(	description="subscription-manager: attempt activate with --email option (against a standalone candlepin server)",
			groups={"blockedByBug-726791"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ActivateWithEmail_Test() {
		String warning = "This test was authored for execution against a standalone candlepin server.";
		if (!sm_serverType.equals(CandlepinType.standalone)) throw new SkipException(warning);
		log.warning(warning);
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, (String)null, true, false, null, null, null);
		SSHCommandResult activateResult = clienttasks.activate("tester@redhat.com",null,null,null,null);
		
		// assert redemption results
		//Assert.assertEquals(activateResult.getStdout().trim(), "Standalone candlepin does not support activation.","Standalone candlepin does not support activation.");
		Assert.assertEquals(activateResult.getStdout().trim(), "Standalone candlepin does not support redeeming a subscription.","Standalone candlepin does not support redeeming a subscription.");
		Assert.assertEquals(activateResult.getExitCode(), Integer.valueOf(255),"Exit code from activate when executed against a standalone candlepin server.");

	}
	
	@Test(	description="subscription-manager: attempt activate against an onpremises candlepin server that has been patched for mock testing",
			groups={"MockActivateTests", "blockedByBug-727978"},
			dataProvider="getOnPremisesMockAttemptToActivateData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void OnPremisesMockAttemptToActivate_Test(Object blockedByBug, String testDescription, String serialNumber, Integer expectedExitCode, String expectedStdout, String expectedStderr) {
		String warning = "This mock test was authored for execution against an on-premises candlepin server.";
		if (!sm_serverType.equals(CandlepinType.standalone)) throw new SkipException(warning);
		log.warning(warning);
		log.info(testDescription);

		// create a facts file with a serialNumber that will clobber the true system facts
		Map<String,String> facts = new HashMap<String,String>();
		facts.put("dmi.system.manufacturer", "Dell Inc.");
		facts.put("dmi.system.serial_number", serialNumber);
		clienttasks.createFactsFileWithOverridingValues(facts);
		
		// update the facts
		clienttasks.facts(null,true, null, null, null);
		
		// attempt activate
		SSHCommandResult activateResult = clienttasks.activate("tester@redhat.com",null,null,null,null);
		
		// assert the activateResult here
		if (expectedExitCode!=null) Assert.assertEquals(activateResult.getExitCode(), expectedExitCode);
		if (expectedStdout!=null) Assert.assertEquals(activateResult.getStdout().trim(), expectedStdout.replaceFirst("\\{0\\}", serialNumber));
		if (expectedStderr!=null) Assert.assertEquals(activateResult.getStderr().trim(), expectedStderr.replaceFirst("\\{0\\}", serialNumber));

	}
	
	
	@Test(	description="subscription-manager: attempt change a consumers canActivate attribute",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptToChangeConsumersCanActivateAttribute_Test() throws Exception {

		// register and attempt to update the consumer by forcing its canActivate attribute to true
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, (String)null, true, null, null, null, null));
		CandlepinTasks.setAttributeForConsumer(sm_clientUsername, sm_clientPassword, SubscriptionManagerBaseTestScript.sm_serverUrl, consumerId, "canActivate", true);
		log.warning("Beacuse the consumer's canActivate attribute is black-listed from changes, that^ attempt to change it to true should have been ignored.  Let's verify...");
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertFalse(jsonConsumer.getBoolean("canActivate"), "The attempt to change the consumer's canActivate attribute to true should have been ignored leaving the consumer with a value of false.");
	}
	
	// This test is the hosted equivalent for CASE 2 from getOnPremisesMockAttemptToActivateData
	@Test(	description="subscription-manager: attempt activate against a hosted candlepin server using a non-found Dell service tag",
			groups={"AcceptanceTests","MockActivateTests", "blockedByBug-688806"},
			enabled=false)	// THIS TEST IS BLOCKED SINCE WE CANNOT CHANGE THE "canActivate" ATTRIBUTE
	//@ImplementsNitrateTest(caseId=)
	public void HostedMockAttemptToActivateUsingNonFoundDellServiceTag_Test() throws Exception {
		String warning = "This mock test was authored for execution against a hosted candlepin server.";
		if (!sm_serverType.equals(CandlepinType.hosted)) throw new SkipException(warning);
		//log.warning(warning);

		// create a facts file with a serialNumber that will clobber the true system facts
		Map<String,String> facts = new HashMap<String,String>();
		facts.put("dmi.system.manufacturer", "Dell Inc.");
		facts.put("dmi.system.serial_number", "0000000");
		clienttasks.createFactsFileWithOverridingValues(facts);
		
		// register and attempt to update the consumer by forcing its canActivate attribute to true
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, (String)null, true, null, null, null, null));
		CandlepinTasks.setAttributeForConsumer(sm_clientUsername, sm_clientPassword, SubscriptionManagerBaseTestScript.sm_serverUrl, consumerId, "canActivate", true);

		// attempt to activate
		SSHCommandResult activateResult = clienttasks.activate("tester@redhat.com",null,null,null,null);
		
		// assert the activateResult here
		Assert.assertEquals(activateResult.getExitCode(), new Integer(0));
		Assert.assertEquals(activateResult.getStdout().trim(), "");
		Assert.assertEquals(activateResult.getStderr().trim(), "A subscription was not found for the given Dell service tag: {0}".replaceFirst("\\{0\\}", facts.get("dmi.system.serial_number")));
	}
	
	
	// This test is the hosted equivalent for CASE 4 from getOnPremisesMockAttemptToActivateData
	@Test(	description="subscription-manager: attempt activate against a hosted candlepin server when consumer's canActivate attribute is false",
			groups={"AcceptanceTests","MockActivateTests", "blockedByBug-688806"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void HostedMockAttemptToActivateWhenCanActivateIsFalse_Test() throws JSONException, Exception {
		String warning = "This mock test was authored for execution against a hosted candlepin server.";
		if (!sm_serverType.equals(CandlepinType.hosted)) throw new SkipException(warning);
		//log.warning(warning);

		// create a facts file with a serialNumber that could not possible match a hocked regtoken on hosted
		Map<String,String> facts = new HashMap<String,String>();
		facts.put("dmi.system.serial_number", "0000000");
		clienttasks.createFactsFileWithOverridingValues(facts);
		
		// register and attempt activate
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, (String)null, true, null, null, null, null));

		// assert that the consumer's can_activate attribute is false
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertFalse(jsonConsumer.getBoolean("canActivate"), "Upon registering a system with a dmi.system.serial_number of '"+facts.get("dmi.system.serial_number")+"', the consumer's canActivate attribute should be false.");
		
		// attempt to activate
		SSHCommandResult activateResult = clienttasks.activate("tester@redhat.com",null,null,null,null);
		
		// assert the activateResult here
		Assert.assertEquals(activateResult.getExitCode(), new Integer(255));
		Assert.assertEquals(activateResult.getStdout().trim(), "The system is unable to redeem the requested subscription: {0}".replaceFirst("\\{0\\}", facts.get("dmi.system.serial_number")));
		Assert.assertEquals(activateResult.getStderr().trim(), "");
//RHEL62
//		Assert.assertEquals(activateResult.getExitCode(), new Integer(0));
//		Assert.assertEquals(activateResult.getStdout().trim(), "");
//		Assert.assertEquals(activateResult.getStderr().trim(), "The system is unable to redeem the requested subscription: {0}".replaceFirst("\\{0\\}", facts.get("dmi.system.serial_number")));
	}
	
	
	
	
	
	
	// Candidates for an automated Test:
	
	
	
	
	// Configuration methods ***********************************************************************
	
	@AfterGroups(value={"MockActivateTests"}, groups={"setup"}, alwaysRun=true)
	public void deleteMockAssetFactsFile () {
		clienttasks.deleteFactsFileWithOverridingValues();
	}
	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="getOnPremisesMockAttemptToActivateData")
	public Object[][] getOnPremisesMockAttemptToActivateDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getOnPremisesMockAttemptToActivateDataAsListOfLists());
	}
	protected List<List<Object>> getOnPremisesMockAttemptToActivateDataAsListOfLists() throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, (String)null, true, false, null, null, null);
		
		
		// String testDescription, String serialNumber, Integer expectedExitCode, String expectedStdout, String expectedStderr
		ll.add(Arrays.asList(new Object[]{null,	"This mocked activate test attempts to activate a subscription against a standalone candlepin server.",											"0ABCDEF",	new Integer(255),	"Standalone candlepin does not support redeeming a subscription.",	null}));
		ll.add(Arrays.asList(new Object[]{null,	"This mocked activate test attempts to activate a subscription when the system's asset tag has already been used to redeem a subscription.",	"1ABCDEF",	new Integer(0),		null,	"The Dell service tag: {0}, has already been used to activate a subscription"}));
		ll.add(Arrays.asList(new Object[]{null,	"This mocked activate test attempts to activate a subscription for which the system's asset tag will not be found for redemption.",				"2ABCDEF",	new Integer(0),		null,	"A subscription was not found for the given Dell service tag: {0}"}));
		ll.add(Arrays.asList(new Object[]{null,	"This mocked activate test attempts to activate a subscription for which the system's  service tag is expired.",								"3ABCDEF",	new Integer(0),		null,	"The Dell service tag: {0}, is expired"}));
		ll.add(Arrays.asList(new Object[]{null,	"This mocked activate test attempts to activate a subscription at a time when the system is unable to process the request.",					"4ABCDEF",	new Integer(0),		null,	"The system is unable to process the requested subscription activation {0}"}));
		ll.add(Arrays.asList(new Object[]{null,	"This mocked activate test attempts to activate a subscription from a system with a valid asset tag.",											"5ABCDEF",	new Integer(0),		null,	"Your subscription activation is being processed and should be available soon. You will be notified via email once it is available. If you have any questions, additional information can be found here: https://access.redhat.com/kb/docs/DOC-53864."}));
		ll.add(Arrays.asList(new Object[]{null,	"This mocked activate test attempts to activate a subscription at a time when the system is unable to process the request.",					"6ABCDEF",	new Integer(0),		null,	"The system is unable to process the requested subscription activation {0}"}));

		return ll;
	}
}
