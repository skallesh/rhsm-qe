package com.redhat.qe.sm.cli.tests;

import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"CleanTests"})
public class CleanTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager-cli: clean and verify the identity is removed",
			groups={"AcceptanceTests","Clean_Test","blockedByBug-654429"},
			enabled=true)
	@ImplementsNitrateTest(caseId=64178)	// http://gibson.usersys.redhat.com/agilo/ticket/4020
	public void Clean_Test() {
		
		// Start fresh by unregistering and registering...
		log.info("Start fresh by unregistering and registering...");
		clienttasks.unregister(null, null, null);
		consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,(String)null,null,false,null,null, null));
		
		// Subscribe to a randomly available pool...
		log.info("Subscribe to a randomly available pool...");
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.size()<=0) throw new SkipException("No susbcriptions were available for which an entitlement could be granted and subsequently cleaned by this test.");
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);

		// Clean
		log.info("Clean...");
		clienttasks.clean(null, null, null);
		
		// Assert the entitlements are removed
		// this was already tested within clienttasks.clean() method
		
		// Assert that because we have run clean, rhsm no longer has an identity and therefore requires us to register to run commands 
		log.info("After running clean, assert that the identity is unknown thereby requiring that we be registered...");
		SSHCommandResult result = clienttasks.identity_(null,null,null, null, null, null, null);
		// Consumer not registered. Please register using --username and --password
		//Assert.assertTrue(result.getStdout().startsWith("Consumer not registered."), "Consumer identity has been removed after clean, therefore we must register to restore our identity.");
		Assert.assertEquals(result.getStdout().trim(),"Consumer not registered. Please register using --username and --password", "Consumer identity has been removed after clean, therefore we must register to restore our identity.");
	}
	
	
	
	// Configuration methods ***********************************************************************
	
	String consumerId = null;
	@AfterGroups(value="Clean_Test", alwaysRun=true)
	public void teardownAfterClass() {
		if (consumerId!=null) {
			clienttasks.clean(null, null, null);
			clienttasks.register(sm_clientUsername, sm_clientPassword, null, null, null, null, consumerId, null, (String)null, Boolean.TRUE, false, null, null, null);
			clienttasks.unregister(null, null, null);
		}
		
	}
	
	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
