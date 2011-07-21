package com.redhat.qe.sm.cli.tests;

import org.json.JSONException;
import org.testng.annotations.Test;

import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"OrgsTests"})
public class OrgsTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager: orgs",
			groups={"blockedByBug-719739"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void Orgs_Test() throws JSONException, Exception {
//		Bug 719739 - subscription-manager orgs output isn't very descriptive 
//		List<String> orgs = CandlepinTasks.getOrgsKeyValueForUser(sm_serverHostname, sm_serverPort, sm_serverPrefix, sm_clientUsername, sm_clientPassword, "key");

//		// Start fresh by unregistering and registering...
//		log.info("Start fresh by unregistering and registering...");
//		clienttasks.unregister(null, null, null);
//		consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null, null, null, null, null, null));
//		
//		// Subscribe to a randomly available pool...
//		log.info("Subscribe to a randomly available pool...");
//		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
//		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
//		clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
//
//		// Clean
//		log.info("Clean...");
//		clienttasks.clean(null, null, null);
//		
//		// Assert the entitlements are removed
//		// this was already tested within clienttasks.clean() method
//		
//		// Assert that because we have run clean, rhsm no longer has an identity and therefore requires us to register to run commands 
//		log.info("After running clean, assert that the identity is unknown thereby requiring that we be registered...");
//		SSHCommandResult result = clienttasks.identity_(null,null,null, null, null, null, null);
//		// Consumer not registered. Please register using --username and --password
//		//Assert.assertTrue(result.getStdout().startsWith("Consumer not registered."), "Consumer identity has been removed after clean, therefore we must register to restore our identity.");
//		Assert.assertEquals(result.getStdout().trim(),"Consumer not registered. Please register using --username and --password", "Consumer identity has been removed after clean, therefore we must register to restore our identity.");
	}
	
	
	
	// Configuration methods ***********************************************************************
	
	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
