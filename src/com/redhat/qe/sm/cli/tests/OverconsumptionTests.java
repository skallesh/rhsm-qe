package com.redhat.qe.sm.cli.tests;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.LogMessageUtil;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */


@Test(groups={"overconsumption"})
public class OverconsumptionTests extends SubscriptionManagerCLITestScript{
	protected List<String> systemConsumerIds = new ArrayList<String>();
	SubscriptionPool testPool = null;

	
	
	// Test Methods ***********************************************************************
	
	@Test(	description="subscription-manager: Basic attempt to oversubscribe the pool quantity",
			groups={},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void BasicAttemptToOversubscribe_Test() {
	
		// register the first consumer
		systemConsumerIds.clear();
		systemConsumerIds.add(client1tasks.getCurrentConsumerId(client1tasks.register(clientusername, clientpassword, null, null, null, null, Boolean.TRUE, null, null, null)));
		
		// find the pool with the least positive quantity available
		int quantity = 10000000;
		for (SubscriptionPool pool: client1tasks.getCurrentlyAvailableSubscriptionPools()) {
			int pool_quantity = Integer.valueOf(pool.quantity);
			if (pool_quantity < quantity && pool_quantity > 0) {
				quantity = pool_quantity;
				testPool = pool;
			}
		}
		Assert.assertNotNull(testPool, "Found an available pool with a positive quantity of available subscriptions.");
		
		// consume each quantity available as a new consumer
		log.info("Now we will register and subscribe new consumers until we exhaust all of the available subscriptions...");
		for (int i=quantity; i>0; i--) {
			// register a new system consumer
			client1tasks.clean(null,null,null);
			systemConsumerIds.add(client1tasks.getCurrentConsumerId(client1tasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null)));

			// subscribe to the pool
			client1tasks.subscribeToSubscriptionPool(testPool);
		}
		
		log.info("Now we will register and subscribe the final subscriber as an attempt to oversubscribe to original pool: "+testPool);
		client1tasks.clean(null,null,null);
		systemConsumerIds.add(client1tasks.getCurrentConsumerId(client1tasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null)));
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client1tasks.getCurrentlyAvailableSubscriptionPools());
		//Assert.assertNotNull(pool, "Found the test pool after having consumed all of its available subscriptions.");
		//Assert.assertEquals(pool.quantity, "0", "Asserting the test pool quantity after having consumed all of its available subscriptions.");
		Assert.assertNull(pool, "The test pool after having consumed all of its available subscriptions is no longer available.");

		// now attempt to oversubscribe
		log.info("Now we will attempt to oversubscribe to original pool: "+testPool);
		// No free entitlements are available for the pool with id	'8a9b90882df297d5012df31def5e00bb'
		Assert.assertNull(client1tasks.subscribeToSubscriptionPool(testPool),"No entitlement cert is granted when the pool is already fully subscribed.");
		// try again
		Assert.assertEquals(client1tasks.subscribe_(testPool.poolId, null, null, null, null, null, null, null).getStdout(),"No free entitlements are available for the pool with id '"+testPool.poolId+"'");
	}
	
	
	@Test(	description="subscription-manager: Concurrent attempt to oversubscribe the pool quantity",
			groups={"blockedByBug-671195"},
			dependsOnMethods={"BasicAttemptToOversubscribe_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void ConcurrentAttemptToOversubscribe_Test() {
	
		// reregister the first systemConsumerId and unsubscribe from the test pool
		client1tasks.register(clientusername,clientpassword,null,null,systemConsumerIds.get(0),null, Boolean.TRUE, null, null, null);
		client1tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		
		// assert that the test pool has a quantity if 1 available
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client1tasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNotNull(pool, "Found the test pool after having consumed available subscriptions.");
		Assert.assertEquals(pool.quantity, "1", "Asserting the test pool quantity after having consumed almost all of its available subscriptions.");

		// register from a second client too
		systemConsumerIds.add(client2tasks.getCurrentConsumerId(client2tasks.register(clientusername, clientpassword, null, null, null, null, Boolean.TRUE, null, null, null)));

		// attempt to concurrently subscribe to the test pool
		log.info("Now we will attempt to subscribe both clients (only one should succeed) concurrently to pool: "+pool);
		String client1command = String.format("%s subscribe --pool=%s", client1tasks.command, pool.poolId);
		String client2command = String.format("%s subscribe --pool=%s", client2tasks.command, pool.poolId);
		client1.runCommand(client1command, LogMessageUtil.action());
		client2.runCommand(client2command, LogMessageUtil.action());
		client1.waitForWithTimeout(new Long(5*60*1000)); // timeout after 5 min
		client2.waitForWithTimeout(new Long(5*60*1000)); // timeout after 5 min
		SSHCommandResult result1 = client1.getSSHCommandResult();
		SSHCommandResult result2 = client2.getSSHCommandResult();
		
		// now one of these command should have succeeded and one should have failed with "No free entitlements..."
		if (result1.getExitCode().intValue()==0) {
			// assert client1 successfully subscribed and client2 was blocked
			Assert.assertEquals(result1.getExitCode(), Integer.valueOf(0),"The exit code from the subscribe command on '"+client1tasks.hostname+"' indicates that it won the subscribe race to the subscription pool's final entitlement.");
			Assert.assertEquals(result1.getStdout(), "","Stdout is blank on a successful subscribe.");
			Assert.assertNotSame(result2.getExitCode(), new Integer(0),"The exit code from the subscribe command on '"+client2tasks.hostname+"' must indicate that it lost the subscribe race to the subscription pool's final entitlement.");
			Assert.assertEquals(result2.getStdout(), "No free entitlements are available for the pool with id '"+pool.poolId+"'", "Stdout must indicate to system '"+client2tasks.hostname+"' that there are no free entitlements left for pool '"+pool.poolId+"'.");
		} else {
			// assert client2 successfully subscribed and client1 was blocked
			Assert.assertEquals(result2.getExitCode(), Integer.valueOf(0),"The exit code from the subscribe command on '"+client2tasks.hostname+"' indicates that it won the subscribe race to the subscription pool's final entitlement.");
			Assert.assertEquals(result2.getStdout(), "","Stdout is blank on a successful subscribe.");
			Assert.assertNotSame(result1.getExitCode(), new Integer(0),"The exit code from the subscribe command on '"+client1tasks.hostname+"' must indicate that it lost the subscribe race to the subscription pool's final entitlement.");
			Assert.assertEquals(result1.getStdout(), "No free entitlements are available for the pool with id '"+pool.poolId+"'", "Stdout must indicate to system '"+client1tasks.hostname+"' that there are no free entitlements left for pool '"+pool.poolId+"'.");
		}
	}

	
	
	
	
	
	
	// TODO Candidates for an automated Test:
	
	
	
	// Configuration Methods ***********************************************************************
	
	
	@AfterClass(groups={"setup"},alwaysRun=true)
	public void unsubscribeAndUnregisterMultipleSystemsAfterClass() {
		if (client2tasks!=null) {
			client2tasks.unsubscribe_(Boolean.TRUE,null, null, null, null);
			client2tasks.unregister_(null, null, null);
		}

		if (client1tasks!=null) {
			
			for (String systemConsumerId : systemConsumerIds) {
				client1tasks.register_(clientusername,clientpassword,null,null,systemConsumerId,null, Boolean.TRUE, null, null, null);
				client1tasks.unsubscribe_(Boolean.TRUE, null, null, null, null);
				client1tasks.unregister_(null, null, null);
			}
			systemConsumerIds.clear();
		}
	}
	


	
	// Protected Methods ***********************************************************************


	
	// Data Providers ***********************************************************************

	

}

