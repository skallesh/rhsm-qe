package com.redhat.qe.sm.cli.tests;

import java.util.List;

import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"clean"})
public class CleanTests extends SubscriptionManagerCLITestScript {

	
	
	@Test(	description="subscription-manager-cli: clean",
			groups={"Clean_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")	// http://gibson.usersys.redhat.com/agilo/ticket/4020
	public void Clean_Test() {
		
		// Start fresh by unregistering and registering...
		log.info("Start fresh by unregistering and registering...");
		clienttasks.unregister();
		consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(clientusername,clientpassword,null,null,null,null));
		
		// Subscribe to a randomly available pool...
		log.info("Subscribe to a randomly available pool...");
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);

		// Clean
		log.info("Clean...");
		clienttasks.clean();
		
		// Assert the current identity does not exist
		log.info("After running clean, assert that the identity does not exist...");
		SSHCommandResult result = clienttasks.identity_(null,null,null);
		Assert.assertTrue(result.getStdout().startsWith("Consumer identity either does not exist or is corrupted."), "Consumer identity does not exist after clean.");
	}
	
	
	
	
	
	// Configuration methods ***********************************************************************
	String consumerId = null;
	@AfterGroups(value="Clean_Test", alwaysRun=true)
	public void teardownAfterClass() {
		if (consumerId!=null) {
			clienttasks.register(clientusername, clientpassword, null, consumerId, null, Boolean.TRUE);
			clienttasks.unregister();
		}
		
	}
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************


}
