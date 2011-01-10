package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandRunner;

@Test(groups="expiration")
public class ExpirationTests extends SubscriptionManagerCLITestScript {

	protected String owner = "";

	
	
	
	// Configuration methods ***********************************************************************
	
	
	@BeforeClass(groups="setup")
	public void skipIfHosted() {
		if (!isServerOnPremises) throw new SkipException("These tests are only valid for on-premises candlepin servers.");
	}
	
	@BeforeClass(dependsOnMethods="skipIfHosted", groups="setup")
	public void registerBeforeClass() throws Exception {
		clienttasks.unregister(null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null));
		
		owner = CandlepinTasks.getOwnerOfConsumerId(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, consumerId).getString("key");
	}
	
	@BeforeClass(groups="setup", dependsOnMethods="registerBeforeClass")
	public void checkTimeBeforeClass() throws Exception{
		checkTime("candlepin server", server);
		checkTime("client", client);
		clienttasks.restart_rhsmcertd(1, false);
	}
	
//	@BeforeMethod
//	public void createTestPoolsBeforeMethod() throws Exception{
//		
//		Calendar cal = new GregorianCalendar();
//		cal.add(Calendar.MINUTE, 2); 	// pool will expire in 2 min from now
//		Date _3min = cal.getTime();
//		cal.add(Calendar.DATE, -21);
//		Date _3weeksago = cal.getTime();
//		/*List<InstalledProduct> clientProds = clienttasks.getCurrentlyInstalledProducts();
//		String product = clientProds.get(0).productName;*/
//		String[] providedProducts = {"37068", "37069", "37060"};
//		String requestBody = CandlepinTasks.createPoolRequest(10, _3weeksago, _3min, "MKT-rhel-server", 123, providedProducts).toString();
//		CandlepinTasks.postResourceUsingRESTfulAPI(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, "/owners/" + owner + "/subscriptions", requestBody);
//		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, owner);
//		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,serverAdminUsername,serverAdminPassword, jobDetail, "FINISHED", 5*1000, 1);
//
//	}
	

	
	
	// Test methods ***********************************************************************
	
	@Test(	description="subscribe to a pool that will expire soon and assert the entitlements are removed after it expires",
			groups={/*"blockedByBug-660713"*/}, dependsOnGroups={},
			enabled=true)
	public void VerifyEntitlementsAreRemovedAfterSubscriptionExpires_Test() throws Exception{

		// create a subscription pool that will expire 2 minutes from now
		int endingMinutesFromNow = 2;
		String expiringPoolId = createTestPool(-60*24,endingMinutesFromNow);
		
		// assert the expiring pool is currently available
		SubscriptionPool expiringPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", expiringPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(expiringPool,"The expiring SubscriptionPool is currently available for subscribing: "+expiringPool);

		// subscribe
		File expiringCertFile = clienttasks.subscribeToSubscriptionPool(expiringPool);
		EntitlementCert expiringCert = clienttasks.getEntitlementCertFromEntitlementCertFile(expiringCertFile);
		List <ProductSubscription> expiringProductSubscriptions = ProductSubscription.findAllInstancesWithMatchingFieldFromList("serialNumber", expiringCert.serialNumber, clienttasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertMore(expiringProductSubscriptions.size(),0, "Found ProductSubscriptions corresponding to the just subscribed SubscriptionPool: "+expiringPool);

		// wait for pool to expire
		sleep(endingMinutesFromNow*60*1000 + 30*1000); // plus a 30 sec buffer;
		
		// verify that that the pool expired
		SubscriptionPool expiredPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", expiringPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(expiredPool,"The expired TestPool is no longer available for subscribing: "+expiringPoolId);
		
		//verify that the expired product subscriptions are gone
		List <ProductSubscription> currentProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		for (ProductSubscription p : expiringProductSubscriptions) {
			Assert.assertTrue(!currentProductSubscriptions.contains(p),"The expired ProductSubscription '"+p+"' has been removed by rhsmd.");
		}
		
		// verify that the expired entitlement cert file is gone
		Assert.assertTrue(RemoteFileTasks.testFileExists(client,expiringCertFile.getPath())==0,"The expired entitlement cert file has been removed by rhsm");
	}
	
	
	@Test(	description="assert that a soon to expire pool is removed from availablity after it expires",
			groups={}, dependsOnGroups={},
			enabled=true)
	public void VerifyPoolIsRemovedAfterSubscriptionExpires_Test() throws Exception {
		
		// create a subscription pool that will expire 2 minutes from now
		int endingMinutesFromNow = 2;
		String expiringPoolId = createTestPool(-60*24,endingMinutesFromNow);
		
		// assert the expiring pool is currently available
		SubscriptionPool expiringPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", expiringPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(expiringPool,"The expiring SubscriptionPool is currently available for subscribing: "+expiringPool);

		// wait for pool to expire
		sleep(endingMinutesFromNow*60*1000 + 30*1000); // plus a 30 sec buffer;
		
		// assert pool is no longer available
		SubscriptionPool expiredPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", expiringPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(expiredPool,"The expired SubscriptionPool is no longer available for subscribing");
	}
	
	
	@Test(	description="assert that a soon to be available pool is not yet available",
			groups={}, dependsOnGroups={},
			enabled=true)
	public void VerifyPoolIsNotYetAvailableUntilTheStartDate_Test() throws Exception {
		
		// create a subscription pool that will start 2 minutes from now
		int startingMinutesFromNow = 3;
		String aboutToStartPoolId = createTestPool(startingMinutesFromNow,60*24);
	
		// assert the starting pool is NOT currently available
		SubscriptionPool startingPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", aboutToStartPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(startingPool,"The about to start SubscriptionPool with id '"+aboutToStartPoolId+"' is currently NOT YET available for subscribing.");

		// wait for pool to start
		sleep((startingMinutesFromNow-1)*60*1000 + 30*1000); // plus a 30 sec buffer;
	
		// assert the starting pool is still NOT currently available (1 minute before its startDate)
		startingPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", aboutToStartPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(startingPool,"The about to start SubscriptionPool with id '"+aboutToStartPoolId+"' is still NOT YET available for subscribing (1 minute before its startDate).");

		// wait one more minute for pool to start
		sleep(1*60*1000 + 30*1000); // plus a 30 sec buffer;

		// assert the starting pool is NOW currently available
		startingPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", aboutToStartPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(startingPool,"The about to start SubscriptionPool with id '"+aboutToStartPoolId+"' is now available for subscribing: "+startingPool);

	}
	
	
	// TODO Candidates for an automated Test:
	//		https://bugzilla.redhat.com/show_bug.cgi?id=655835
	
	
	
	
	
	
	// protected methods ***********************************************************************
	

	
	protected void checkTime(String host, SSHCommandRunner runner)throws Exception {
		//make sure local clock and server clock are synced
		Date localTime = Calendar.getInstance().getTime();
		Date remoteTime; 
		//SSHCommandRunner runner = new SSHCommandRunner("jweiss-rhel6-1.usersys.redhat.com", sshUser, sshKeyPrivate, sshkeyPassphrase, null);
		runner.runCommandAndWait("date");
		String serverDateStr = runner.getStdout();
		SimpleDateFormat unixFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
		remoteTime = unixFormat.parse(serverDateStr);
		long timeDiffms = Math.abs(localTime.getTime() - remoteTime.getTime());
		Assert.assertLess(timeDiffms, 60000L, "Time difference with " + host + " is less than 1 minute");
	}
	


	protected String createTestPool(int startingMinutesFromNow, int endingMinutesFromNow) throws JSONException, Exception  {
		
		// set the start and end dates
		Calendar endCalendar = new GregorianCalendar();
		endCalendar.add(Calendar.MINUTE, endingMinutesFromNow);
		Date endDate = endCalendar.getTime();
		Calendar startCalendar = new GregorianCalendar();
		startCalendar.add(Calendar.MINUTE, startingMinutesFromNow);
		Date startDate = startCalendar.getTime();

		
		// choose a contract number
		Integer contractNumber = Integer.valueOf(getRandInt());
		
		// choose a product id for the subscription
		JSONArray jsonProducts = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,serverAdminUsername,serverAdminPassword,"/products"));	
		//String  productId =  "MKT-rhel-server";
		String productId =  ((JSONObject) jsonProducts.get(randomGenerator.nextInt(jsonProducts.length()))).getString("id");
		
		// choose providedProducts for the subscription
		//String[] providedProducts = {"37068", "37069", "37060"};
		/*List<InstalledProduct> clientProds = clienttasks.getCurrentlyInstalledProducts();
		String product = clientProds.get(0).productName;*/
		String[] providedProducts = {
//				((JSONObject) jsonProducts.get(randomGenerator.nextInt(jsonProducts.length()))).getString("id"),
//				((JSONObject) jsonProducts.get(randomGenerator.nextInt(jsonProducts.length()))).getString("id"),
//				((JSONObject) jsonProducts.get(randomGenerator.nextInt(jsonProducts.length()))).getString("id")
				};
		
		// create the subscription
		String requestBody = CandlepinTasks.createPoolRequest(3, startDate, endDate, productId, contractNumber, providedProducts).toString();
		JSONObject jsonSubscription = new JSONObject(CandlepinTasks.postResourceUsingRESTfulAPI(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, "/owners/" + owner + "/subscriptions", requestBody));
		
		// refresh the pools
		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, owner);
		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,serverAdminUsername,serverAdminPassword, jobDetail, "FINISHED", 5*1000, 1);
		
		// loop through all pools available to owner and find the newly created poolid corresponding to the new subscription contract number
		String poolId = null;
		JSONArray jsonPools = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,serverAdminUsername,serverAdminPassword,"/owners/"+owner+"/pools"));	
		for (int i = 0; i < jsonPools.length(); i++) {
			JSONObject jsonPool = (JSONObject) jsonPools.get(i);
			//if (contractNumber.equals(jsonPool.getInt("contractNumber"))) {
			if (jsonPool.getString("subscriptionId").equals(jsonSubscription.getString("id"))) {
				poolId = jsonPool.getString("id");
				break;
			}
		}
		Assert.assertNotNull(poolId,"Found newly created pool corresponding to the newly created subscription with contractNumber: "+contractNumber);
		log.info("The newly created subscription pool with id '"+poolId+"' will start in '"+startingMinutesFromNow+"' minutes from now.");
		log.info("The newly created subscription pool with id '"+poolId+"' will expire in '"+endingMinutesFromNow+"' minutes from now.");
		return poolId; // return poolId to the newly available SubscriptionPool
		
	}
	
	
//	protected Predicate<SubscriptionPool> expToday = new Predicate<SubscriptionPool>(){
//		public boolean apply(SubscriptionPool pool){
//			Calendar cal = pool.endDate;
//			Calendar today = new GregorianCalendar();
//			return (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) && (cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) ;
//		}
//	};
//
//	class ByIdPredicate implements Predicate<SubscriptionPool> {
//		String id;
//		public ByIdPredicate(String id) {
//			this.id=id;
//		}
//		public boolean apply(SubscriptionPool pool) {
//			return this.id.equals(pool.poolId);
//		}
//	}
//	
//	protected SubscriptionPool getPool() {
//		Collection<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
//		
//		Collection<SubscriptionPool> expiresToday = Collections2.filter(pools, expToday);
//		
//		//choose first pool
//		return  expiresToday.iterator().next();
//	}
	
}
