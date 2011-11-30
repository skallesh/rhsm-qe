package com.redhat.qe.sm.cli.tests;

import org.json.JSONObject;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"HealingTests"})
public class HealingTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************
	
	@Test(	description="a new system consumer's autoheal attribute defaults to true (on)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutohealAttributeDefaultsToTrueForNewSystemConsumer_Test() throws Exception {
		
		// register a new consumer
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,(String)null,true,null,null,null,null));
		
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertTrue(jsonConsumer.getBoolean("autoheal"), "A new system consumer's autoheal attribute value defaults to true.");
	}
	
	@Test(	description="using the candlepin api, a consumer's autoheal attribute can be toggled off/on",
			groups={},
			dependsOnMethods={"VerifyAutohealAttributeDefaultsToTrueForNewSystemConsumer_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutohealAttributeCanBeToggledOffForConsumer_Test() throws Exception {
		
		// get the current registered consumer's id
		String consumerId = clienttasks.getCurrentConsumerId();
		
		JSONObject jsonConsumer = CandlepinTasks.setAutohealForConsumer(sm_clientUsername,sm_clientPassword, sm_serverUrl, consumerId,false);
		Assert.assertFalse(jsonConsumer.getBoolean("autoheal"), "A consumer's autoheal attribute value can be toggled off (expected value=false).");
		jsonConsumer = CandlepinTasks.setAutohealForConsumer(sm_clientUsername,sm_clientPassword, sm_serverUrl, consumerId,true);
		Assert.assertTrue(jsonConsumer.getBoolean("autoheal"), "A consumer's autoheal attribute value can be toggled on (expected value=true).");
	}
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 744654 - [ALL LANG] [RHSM CLI]config module_ config Server port with blank or incorrect text produces traceback.
	// TODO Cases in Bug 710172 - [RFE] Provide automated healing of expiring subscriptions
	// TODO   subcase Bug 746088 - autoheal is not super-subscribing on the day the current entitlement cert expires
	// TODO   subcase Bug 746218 - auto-heal isn't working for partial subscription
	// TODO Cases in Bug 726411 - [RFE] Support for certificate healing
	
	
	// Configuration methods ***********************************************************************
	
	
	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
