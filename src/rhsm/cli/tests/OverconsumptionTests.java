package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.jul.TestRecords;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author jsefler
 *
 * dgoodwin wrote a comparable test that will also test overconsumption.
 * it is located on candlepin in server/bin/concurrency-test.rb
 *
 */


@Test(groups={"OverconsumptionTests"})
public class OverconsumptionTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36655", "RHEL7-51495"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: Basic attempt to oversubscribe the pool quantity",
			groups={"Tier2Tests"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testBasicAttemptToOversubscribe() throws JSONException, Exception {
	
		// find the pool with the least positive quantity available >= 2
		client1tasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, registereeName, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null);
		int quantity = 1000000;
		for (SubscriptionPool pool: client1tasks.getCurrentlyAvailableSubscriptionPools()) {
//debugging if (pool.productId.equals("awesomeos-virt-4") && pool.quantity.equals("5")) {testPool=pool;break;}
//debugging if (pool.productId.equals("awesomeos-all-just-86_64-cont")) {testPool=pool;quantity = Integer.valueOf(pool.quantity);break;} else {if (true) continue;}
			if (pool.quantity.equalsIgnoreCase("unlimited")) continue;
			int pool_quantity = Integer.valueOf(pool.quantity);
			if (pool_quantity < quantity && pool_quantity >= 2) {
				quantity = pool_quantity;
				testPool = pool;
			}
		}
		client1tasks.unregister(null, null, null, null);
		Assert.assertNotNull(testPool, "Found an available pool with a quantity of available subscriptions >= 2.");
		
		// consume each quantity available as a new consumer
		log.info("Now we will register and subscribe new consumers until we exhaust all of the available subscriptions...");
		systemConsumerIds.clear();
		for (int i=quantity; i>0; i--) {
			// register a new system consumer
			client1tasks.clean();
			systemConsumerIds.add(client1tasks.getCurrentConsumerId(client1tasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, registereeName, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null)));

			// subscribe to the pool
			// client1tasks.subscribeToSubscriptionPool(testPool);	// do not do this anymore because it could attach a quantity greater than 1 as a result of new feature bug 1008647
			client1tasks.subscribe(null, null, testPool.poolId,null,null,"1",null,null,null,null,null, null, null);
			testPool.quantity = String.valueOf(Integer.valueOf(testPool.quantity)-1);	// decrement this pool's quantity since we just consumed one
		}
		
		log.info("Now we will register and subscribe the final subscriber as an attempt to oversubscribe to original pool: "+testPool);
		client1tasks.clean();
		systemConsumerIds.add(client1tasks.getCurrentConsumerId(client1tasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, registereeName, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null)));
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client1tasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(pool, "The test pool is no longer in the --all --available list after having consumed all of its available subscriptions.");
		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client1tasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNull(pool, "The test pool is no longer in the --available list after having consumed all of its available subscriptions.");
		
		// assert the consumed quantity
		JSONObject jsonTestPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+testPool.poolId));
		Assert.assertEquals(jsonTestPool.getInt("consumed"), jsonTestPool.getInt("quantity"),
				"Asserting the test pool's consumed attribute matches it's original total quantity after having consumed all of its available entitlements.");

		// now attempt to oversubscribe
		log.info("Now we will attempt to oversubscribe to original pool: "+testPool);
		// No entitlements are available from the pool with id '8a90f8143611c33f013611c4797b0456'.
		Assert.assertNull(client1tasks.subscribeToSubscriptionPool(testPool,/*sm_serverAdminUsername*/sm_clientUsername,/*sm_serverAdminPassword*/sm_clientPassword,sm_serverUrl),"No entitlement cert is granted when the pool is already fully subscribed.");
		// try again
		String expectedStdout = String.format("No entitlements are available from the pool with id '%s'.",testPool.poolId); // expected string changed by bug 876758
		expectedStdout = String.format("No subscriptions are available from the pool with id '%s'.",testPool.poolId);
		if (!clienttasks.workaroundForBug876764(sm_serverType)) expectedStdout = String.format("No subscriptions are available from the pool with ID '%s'.",testPool.poolId);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
			expectedStdout = String.format("No subscriptions are available from the pool with ID \"%s\".",testPool.poolId);
		}
		Assert.assertEquals(client1tasks.subscribe_(null, null, testPool.poolId, null, null, null, null, null, null, null, null, null, null).getStdout().trim(),expectedStdout);
		// assert the consumed quantity again
		jsonTestPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+testPool.poolId));
		Assert.assertEquals(jsonTestPool.getInt("consumed"), jsonTestPool.getInt("quantity"),
				"Asserting the test pool's consumed attribute has not overconsumed it's total quantity after attempting a basic overconsumption of its entitlements.");

	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37718", "RHEL7-51496"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: Concurrent attempt to subscribe",
			groups={"Tier2Tests"},
			dependsOnMethods={"testBasicAttemptToOversubscribe"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testConcurrentAttemptToSubscribe() throws JSONException, Exception {
		if (client1==null || client2==null) throw new SkipException("This test requires two clients.");

		// reregister the first systemConsumerId and unsubscribe from the test pool
		client1tasks.clean();
		client1tasks.register(sm_clientUsername,sm_clientPassword,null,null,null,registereeName,systemConsumerIds.get(0), null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		client1tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		
		// reregister the second systemConsumerId and unsubscribe from the test pool
		client2tasks.clean();
		client2tasks.register(sm_clientUsername,sm_clientPassword,null,null,null,registereeName,systemConsumerIds.get(1), null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		client2tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		
		// assert that each client has no entitlements
		List<File> client1EntitlementCerts = client1tasks.getCurrentEntitlementCertFiles();
		Assert.assertEquals(client1EntitlementCerts.size(), 0, "Registered client on '"+client1tasks.hostname+"' should have NO entitlements.");
		List<File> client2EntitlementCerts = client2tasks.getCurrentEntitlementCertFiles();
		Assert.assertEquals(client2EntitlementCerts.size(), 0, "Registered client on '"+client2tasks.hostname+"' should have NO entitlements.");
		
		// assert that the test pool now has a quantity of 2 available
		SubscriptionPool pool;
		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client1tasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNotNull(pool, "Found the test pool after having consumed available subscriptions.");
		Assert.assertEquals(pool.quantity, "2", "Asserting the test pool quantity after having consumed almost all of its available subscriptions.");
		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client2tasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNotNull(pool, "Found the test pool after having consumed available subscriptions.");
		Assert.assertEquals(pool.quantity, "2", "Asserting the test pool quantity after having consumed almost all of its available subscriptions.");

		// attempt to concurrently subscribe to the test pool
		log.info("Now we will attempt to subscribe both clients concurrently to pool: "+pool);
		String client1command = String.format("%s subscribe --pool=%s", client1tasks.command, pool.poolId);
		String client2command = String.format("%s subscribe --pool=%s", client2tasks.command, pool.poolId);
		client1.runCommand(client1command, TestRecords.action());
		client2.runCommand(client2command, TestRecords.action());
		client1.waitForWithTimeout(new Long(10*60*1000)); // timeout after 10 min
		client2.waitForWithTimeout(new Long(10*60*1000)); // timeout after 10 min
		SSHCommandResult result1 = client1.getSSHCommandResult();
		SSHCommandResult result2 = client2.getSSHCommandResult();
		
		// assert the results
		log.info("SSHCommandResult from '"+client1tasks.hostname+"': "+result1);
//		Assert.assertEquals(result1.getStdout().trim(), "","The lack of information in stdout from the subscribe command on '"+client1tasks.hostname+"' indicates that it successfully subscribed to poolid: "+testPool.poolId);
//		Assert.assertEquals(result1.getStdout().trim(), String.format("Successfully consumed a subscription from the pool with id %s.",testPool.poolId),"On '"+client1tasks.hostname+"' we successfully subscribed to poolid: "+testPool.poolId);	// Bug 812410 - Subscription-manager subscribe CLI feedback 
//		Assert.assertEquals(result1.getStdout().trim(), String.format("Successfully consumed a subscription for: %s",testPool.subscriptionName),"On '"+client1tasks.hostname+"' we successfully subscribed to poolid: "+testPool.poolId);	// changed by Bug 874804 Subscribe -> Attach 
		Assert.assertEquals(result1.getStdout().trim(), String.format("Successfully attached a subscription for: %s",testPool.subscriptionName),"On '"+client1tasks.hostname+"' we successfully subscribed to poolid: "+testPool.poolId);
		Assert.assertEquals(result1.getStderr().trim(), "","No stderr information is expected on '"+client1tasks.hostname+"'.");
		Assert.assertEquals(result1.getExitCode(), Integer.valueOf(0),"The exit code from the subscribe command on '"+client1tasks.hostname+"' indicates the subscribe attempt was handled gracefully.");
		log.info("SSHCommandResult from '"+client2tasks.hostname+"': "+result2);
//		Assert.assertEquals(result2.getStdout().trim(), "","The lack of information in stdout from the subscribe command on '"+client2tasks.hostname+"' indicates that it successfully subscribed to poolid: "+testPool.poolId);
//		Assert.assertEquals(result2.getStdout().trim(), String.format("Successfully consumed a subscription from the pool with id %s.",testPool.poolId),"On '"+client2tasks.hostname+"' we successfully subscribed to poolid: "+testPool.poolId);	// Bug 812410 - Subscription-manager subscribe CLI feedback 
//		Assert.assertEquals(result2.getStdout().trim(), String.format("Successfully consumed a subscription for: %s",testPool.subscriptionName),"On '"+client2tasks.hostname+"' we successfully subscribed to poolid: "+testPool.poolId);	// changed by Bug 874804 Subscribe -> Attach 
		Assert.assertEquals(result2.getStdout().trim(), String.format("Successfully attached a subscription for: %s",testPool.subscriptionName),"On '"+client2tasks.hostname+"' we successfully subscribed to poolid: "+testPool.poolId);
		Assert.assertEquals(result2.getStderr().trim(), "","No stderr information is expected on '"+client2tasks.hostname+"'.");
		Assert.assertEquals(result2.getExitCode(), Integer.valueOf(0),"The exit code from the subscribe command on '"+client2tasks.hostname+"' indicates the subscribe attempt was handled gracefully.");
		
		// assert that the test pool has dropped by 2
//		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client1tasks.getCurrentlyAllAvailableSubscriptionPools());
//		Assert.assertNotNull(pool, "Found the test pool amongst --all --available after having consumed all of its available entitlements.");
//		Assert.assertEquals(pool.quantity, "0", "Asserting the test pool quantity has dropped by 2 due to the concurrent call to subscribe.");
//		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client2tasks.getCurrentlyAllAvailableSubscriptionPools());
//		Assert.assertNotNull(pool, "Found the test pool amongst --all --available after having consumed all of its available entitlements.");
//		Assert.assertEquals(pool.quantity, "0", "Asserting the test pool quantity has dropped by 2 due to the concurrent call to subscribe.");
		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client1tasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(pool, "The test pool is no longer in the --all --available list after having consumed all of its available subscriptions.");
		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client2tasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(pool, "The test pool is no longer in the --all --available list after having consumed all of its available subscriptions.");
		JSONObject jsonTestPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+testPool.poolId));
		Assert.assertEquals(jsonTestPool.getInt("consumed"), jsonTestPool.getInt("quantity"), "Asserting the test pool's consumed attribute matches it's original total quantity after having consumed all of its available entitlements.");

		// make sure both clients got individualized entitlement certs
		client1EntitlementCerts = client1tasks.getCurrentEntitlementCertFiles();
		Assert.assertEquals(client1EntitlementCerts.size(), 1, "Registered client on '"+client1tasks.hostname+"' should have 1 entitlement from the attempt to subscribe to poolid: "+testPool.poolId);
		client2EntitlementCerts = client2tasks.getCurrentEntitlementCertFiles();
		Assert.assertEquals(client2EntitlementCerts.size(), 1, "Registered client on '"+client2tasks.hostname+"' should have 1 entitlement from the attempt to subscribe to poolid: "+testPool.poolId);
		EntitlementCert client1EntitlementCert = client1tasks.getEntitlementCertFromEntitlementCertFile(client1EntitlementCerts.get(0));
		EntitlementCert client2EntitlementCert = client2tasks.getEntitlementCertFromEntitlementCertFile(client2EntitlementCerts.get(0));
		Assert.assertFalse(client1EntitlementCert.serialNumber.equals(client2EntitlementCert.serialNumber), "Asserting that the entitlement serials granted to each of the concurent subscribers are unique to each other.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37719", "RHEL7-51497"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: Concurrent attempt to oversubscribe the pool quantity",
			groups={"Tier2Tests","blockedByBug-671195","blockedByBug-1336054"},
			dependsOnMethods={"testConcurrentAttemptToSubscribe"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testConcurrentAttemptToOversubscribe() throws JSONException, Exception {
		if (client1==null || client2==null) throw new SkipException("This test requires two clients.");
		
		// reregister the first systemConsumerId and unsubscribe from the test pool
		client1tasks.clean();
		client1tasks.register(sm_clientUsername,sm_clientPassword,null,null,null,registereeName,systemConsumerIds.get(0), null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		client1tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();

		// register from a second client too
		client2tasks.clean();
		systemConsumerIds.add(client2tasks.getCurrentConsumerId(client2tasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, registereeName, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null)));

		// assert that the test pool has a quantity of 1 available
		SubscriptionPool pool;
		pool= SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client1tasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNotNull(pool, "Found the test pool after having consumed available subscriptions.");
		Assert.assertEquals(pool.quantity, "1", "Asserting the test pool quantity after having consumed almost all of its available subscriptions.");
		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client2tasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNotNull(pool, "Found the test pool after having consumed available subscriptions.");
		Assert.assertEquals(pool.quantity, "1", "Asserting the test pool quantity after having consumed almost all of its available subscriptions.");

		// attempt to concurrently subscribe to the test pool
		log.info("Now we will attempt to subscribe both clients (only one should succeed) concurrently to pool: "+pool);
		String client1command = String.format("%s subscribe --pool=%s", client1tasks.command, pool.poolId);
		String client2command = String.format("%s subscribe --pool=%s", client2tasks.command, pool.poolId);
		client1.runCommand(client1command, TestRecords.action());
		client2.runCommand(client2command, TestRecords.action());
		client1.waitForWithTimeout(new Long(10*60*1000)); // timeout after 10 min
		client2.waitForWithTimeout(new Long(10*60*1000)); // timeout after 10 min
		SSHCommandResult result1 = client1.getSSHCommandResult();
		SSHCommandResult result2 = client2.getSSHCommandResult();
		
		// assert that the test pool does NOT fall below zero
//		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client1tasks.getCurrentlyAllAvailableSubscriptionPools());
//		Assert.assertNotNull(pool, "Found the test pool amongst --all --available after having consumed all of its available entitlements.");
//		Assert.assertEquals(pool.quantity, "0", "Asserting the test pool quantity does not fall below zero after attempting a concurrent subscribe.");
//		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client2tasks.getCurrentlyAllAvailableSubscriptionPools());
//		Assert.assertNotNull(pool, "Found the test pool amongst --all --available after having consumed all of its available entitlements.");
//		Assert.assertEquals(pool.quantity, "0", "Asserting the test pool quantity does not fall below zero after attempting a concurrent subscribe.");
		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client1tasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(pool, "The test pool is no longer in the --all --available list after having consumed all of its available subscriptions.");
		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client2tasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(pool, "The test pool is no longer in the --all --available list after having consumed all of its available subscriptions.");
		JSONObject jsonTestPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+testPool.poolId));
		Assert.assertEquals(jsonTestPool.getInt("consumed"), jsonTestPool.getInt("quantity"), "Asserting the consumed attribute of test pool '"+testPool.poolId+"' matches it's original total quantity after having consumed all of its available entitlements.");
		
		// one of these command should have succeeded and one should have failed with "No entitlements are available"...
		// decide who was the winner and who must have been the loser
		SSHCommandResult sshWinner, sshLoser;
		SubscriptionManagerTasks smtWinner, smtLoser;
//		if (result1.getStdout().equals("")) {	// client1 appears to be the winner, therefore client2 must be the loser
		if (result1.getStdout().startsWith("Success")) {	// client1 appears to be the winner, therefore client2 must be the loser
			sshWinner =	result1;
			smtWinner = client1tasks;
			sshLoser =	result2;
			smtLoser =	client2tasks;
		} else {	// client2 must be the winner and client1 is the loser
			sshWinner =	result2;
			smtWinner = client2tasks;
			sshLoser =	result1;
			smtLoser =	client1tasks;
		}
		
		// assert the Winner and Loser
		log.info("SSHCommandResult from '"+smtWinner.hostname+"': "+sshWinner);
//		Assert.assertEquals(sshWinner.getStdout().trim(), "","The lack of information in stdout from the subscribe command on '"+smtWinner.hostname+"' indicates that it won the subscribe race to the subscription pool's final entitlement.");
//		Assert.assertEquals(sshWinner.getStdout().trim(), String.format("Successfully consumed a subscription from the pool with id %s.",testPool.poolId),"On '"+smtWinner.hostname+"' we successfully subscribed to poolid '"+testPool.poolId+"' indicating that it won the subscribe race to the subscription pool's final entitlement.");	// Bug 812410 - Subscription-manager subscribe CLI feedback 
//		Assert.assertEquals(sshWinner.getStdout().trim(), String.format("Successfully consumed a subscription for: %s",testPool.subscriptionName),"On '"+smtWinner.hostname+"' we successfully subscribed to poolid '"+testPool.poolId+"' indicating that it won the subscribe race to the subscription pool's final entitlement.");	// changed by Bug 874804 Subscribe -> Attach
		Assert.assertEquals(sshWinner.getStdout().trim(), String.format("Successfully attached a subscription for: %s",testPool.subscriptionName),"On '"+smtWinner.hostname+"' we successfully subscribed to poolid '"+testPool.poolId+"' indicating that it won the subscribe race to the subscription pool's final entitlement.");
		Assert.assertEquals(sshWinner.getStderr().trim(), "","No stderr information is expected on '"+smtWinner.hostname+"'.");
//		Assert.assertEquals(sshWinner.getExitCode(), Integer.valueOf(0),"The exit code from the subscribe command on '"+smtWinner.hostname+"' indicates the subscribe attempt was handled gracefully.");	// assertion valid prior to RHEL63 fix for bug 689608
		Assert.assertEquals(sshWinner.getExitCode(), Integer.valueOf(0),"The exit code from the subscribe command on '"+smtWinner.hostname+"' indicates the subscribe attempt successfully granted an entitlement.");
		log.info("SSHCommandResult from '"+smtLoser.hostname+"': "+sshLoser);
		String expectedStdout = String.format("No entitlements are available from the pool with id '%s'.",testPool.poolId); // expected string changed by bug 876758
		expectedStdout = String.format("No subscriptions are available from the pool with id '%s'.",testPool.poolId);
		if (!clienttasks.workaroundForBug876764(sm_serverType)) expectedStdout = String.format("No subscriptions are available from the pool with ID '%s'.",testPool.poolId);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
			expectedStdout = String.format("No subscriptions are available from the pool with ID \"%s\".",testPool.poolId);
		}
		Assert.assertEquals(sshLoser.getStdout().trim(), expectedStdout, "Stdout must indicate to system '"+smtLoser.hostname+"' that there are no free entitlements left from poolId '"+testPool.poolId+"'.");
		Assert.assertEquals(sshLoser.getStderr().trim(), "","No stderr information is expected on '"+smtLoser.hostname+"'.");
//		Assert.assertEquals(sshLoser.getExitCode(), Integer.valueOf(0),"The exit code from the subscribe command on '"+smtLoser.hostname+"' indicates the subscribe attempt was handled gracefully.");	// assertion valid prior to RHEL63 fix for bug 689608
		Assert.assertEquals(sshLoser.getExitCode(), Integer.valueOf(1),"The exit code from the subscribe command on '"+smtLoser.hostname+"' indicates the subscribe attempt did not grant an entitlement.");
	}

	
	
	
	// Candidates for an automated Test:
	
	
	
	
	
	
	
	// Protected Class Variables ***********************************************************************
	
	protected List<String> systemConsumerIds = new ArrayList<String>();
	protected SubscriptionPool testPool = null;
	protected final String registereeName = "Overconsumer";
	
	
	
	// Protected Methods ***********************************************************************

	
	

	
	// Configuration Methods ***********************************************************************

	@AfterClass(groups={"setup"},alwaysRun=true)
	public void unsubscribeAndUnregisterMultipleSystemsAfterClass() {
		if (client2tasks!=null) {
			client2tasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
			client2tasks.unregister_(null, null, null, null);
		}

		if (client1tasks!=null) {
			
			for (String systemConsumerId : systemConsumerIds) {
				client1tasks.register_(sm_clientUsername,sm_clientPassword,null,null,null,null,systemConsumerId, null, null, null, (String)null, null, null, null, Boolean.TRUE, null, null, null, null, null);
				client1tasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
				client1tasks.unregister_(null, null, null, null);
			}
			systemConsumerIds.clear();
		}
	}
	
	
	
	// Data Providers ***********************************************************************

	

}

