package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

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

	@BeforeClass(groups="setup")
	public void skipIfHosted() {
		if (!isServerOnPremises) throw new SkipException("These tests are only valid for on-premises candlepin servers.");
	}
	
	@BeforeClass(dependsOnMethods="skipIfHosted", groups="setup")
	public void getOwner() throws Exception {
		clienttasks.unregister(null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null));
		
		owner = CandlepinTasks.getOwnerOfConsumerId(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, consumerId).getString("key");
	}
	
	@BeforeClass(groups="setup", dependsOnMethods="getOwner")
	public void checkTime() throws Exception{
		checkTime("candlepin server", server);
		checkTime("client", client);
		clienttasks.restart_rhsmcertd(1, false);
	}
	
	

	
	protected Predicate<SubscriptionPool> expToday = new Predicate<SubscriptionPool>(){
		public boolean apply(SubscriptionPool pool){
			Calendar cal = pool.endDate;
			Calendar today = new GregorianCalendar();
			return (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) && (cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) ;
		}
	};
	
	class ByIdPredicate implements Predicate<SubscriptionPool> {
		String id;
		public ByIdPredicate(String id) {
			this.id=id;
		}
		public boolean apply(SubscriptionPool pool) {
			return this.id.equals(pool.poolId);
		}
	}
	
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
	

	@BeforeMethod
	public void createTestPools() throws Exception{
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.MINUTE, 2); 	// pool will expire in 2 min from now
		Date _3min = cal.getTime();
		cal.add(Calendar.DATE, -21);
		Date _3weeksago = cal.getTime();
		/*List<InstalledProduct> clientProds = clienttasks.getCurrentlyInstalledProducts();
		String product = clientProds.get(0).productName;*/
		String[] providedProducts = {"37068", "37069", "37060"};
		String requestBody = CandlepinTasks.createPoolRequest(10, _3weeksago, _3min, "MKT-rhel-server", 123, providedProducts).toString();
		CandlepinTasks.postResourceUsingRESTfulAPI(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, "/owners/" + owner + "/subscriptions", requestBody);
		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, owner);
		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,serverAdminUsername,serverAdminPassword, jobDetail, "FINISHED", 5*1000, 1);

	}
	
	protected SubscriptionPool getPool() {
		Collection<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		
		Collection<SubscriptionPool> expiresToday = Collections2.filter(pools, expToday);
		
		//choose first pool
		return  expiresToday.iterator().next();
	}
	
	@Test(	description="subscribe to a pool that will expire soon and assert the entitlements are removed after it expires",
			groups={"blockedByBug-660713"}, dependsOnGroups={},
			enabled=true)
	public void SubscribeToAboutToExpirePool_Test() throws Exception{

		
		SubscriptionPool expiringPool = getPool();
		String expiringPoolId = expiringPool.poolId;
		//String contract = testPool.
		
		//subscribe
		File expiringCertFile = clienttasks.subscribeToSubscriptionPool(expiringPool);
		EntitlementCert expiringCert = clienttasks.getEntitlementCertFromEntitlementCertFile(expiringCertFile);
		List <ProductSubscription> expiringProductSubscriptions = ProductSubscription.findAllInstancesWithMatchingFieldFromList("serialNumber", expiringCert.serialNumber, clienttasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertMore(expiringProductSubscriptions.size(),0, "Found ProductSubscriptions corresponding to the just subscribed SubscriptionPool: "+expiringPool);
//		clienttasks.subscribeToSubscriptionPool(testPool);

		//wait for pools to expire
		sleep(3*60*1000 + 30*1000); // 3 min plus a 30 sec buffer;
		
		//verify that that the pool expired
		SubscriptionPool expiredPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", expiringPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(expiredPool,"The expired TestPool is no longer available for subscribing: "+expiringPoolId);
//		Collection<SubscriptionPool> matchedId = Collections2.filter(clienttasks.getCurrentlyAllAvailableSubscriptionPools(), new ByIdPredicate(poolid));
//		Assert.assertEquals(matchedId.size(), 0, "Zero pools match id " + poolid);
		
		//verify that the expired product subscriptions are gone
		List <ProductSubscription> currentProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		for (ProductSubscription p : expiringProductSubscriptions) {
			Assert.assertTrue(!currentProductSubscriptions.contains(p),"The expired ProductSubscription '"+p+"' has been removed by rhsmd.");
		}

//		Collection<SubscriptionPool> matchedPools = Collections2.transform(clienttasks.getCurrentlyConsumedProductSubscriptions(),
//				new Function<ProductSubscription, SubscriptionPool>(){
//					public SubscriptionPool apply(ProductSubscription ps){	
//						try {
//							return clienttasks.getSubscriptionPoolFromProductSubscription(ps, owner, clientpassword);
//						}catch (Exception e) {
//							log.warning("Could not getSubscriptionPoolFromProductSubscription: "+ e);
//							return new SubscriptionPool();
//						} 	
//					}});
//		matchedId = Collections2.filter(matchedPools, new ByIdPredicate(poolid));
//		Assert.assertEquals(matchedId.size(), 0, "Zero pools match id " + poolid);

		
		// verify that the expired entitlement cert file is gone
		Assert.assertTrue(RemoteFileTasks.testFileExists(client,expiringCertFile.getPath())==1,"The expired entitlement cert file has been removed by rhsm");
	}
	
	@Test 
	public void PoolExpiresEvenWhenNotSubscribedTo() throws Exception {
		SubscriptionPool testPool = getPool();
		String expiringPoolId = testPool.poolId;
		//String contract = testPool.
	
		// assert pool still exists
		SubscriptionPool expiringPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", expiringPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(expiringPool,"The expiring SubscriptionPool is currently available for subscribing: "+expiringPool);

		//wait for pools to expire
		sleep(3*60*1000 + 30*1000); // 3 min plus a 30 sec buffer;
		
		// assert pool no longer exists
		SubscriptionPool expiredPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", expiringPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(expiredPool,"The expired SubscriptionPool is no longer available for subscribing");
		
//		//verify that that the pool expired
//		Collection<SubscriptionPool> matchedId = Collections2.filter(clienttasks.getCurrentlyAllAvailableSubscriptionPools(), new ByIdPredicate(poolid));
//		Assert.assertEquals(matchedId.size(), 0, "Zero pools match id " + poolid);
	}
	
	
	// TODO Candidates for an automated Test:
	//		https://bugzilla.redhat.com/show_bug.cgi?id=655835
}
