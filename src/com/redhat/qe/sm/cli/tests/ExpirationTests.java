package com.redhat.qe.sm.cli.tests;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandRunner;

@Test(groups="expiration")
public class ExpirationTests extends SubscriptionManagerCLITestScript {

	protected String owner = "donaldduck";

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
			return pool.poolId.equals(this.id);
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
	
	@BeforeClass(groups="setup")
	public void checkTime() throws Exception{
		checkTime("candlepin server", server);
		checkTime("client", client);
	}
	
	
	
	@BeforeMethod
	public void createTestPools() throws Exception{
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.MINUTE, 2); 
		Date _3min = cal.getTime();
		cal.add(Calendar.DATE, -21);
		Date _3weeksago = cal.getTime();
		/*List<InstalledProduct> clientProds = clienttasks.getCurrentlyInstalledProducts();
		String product = clientProds.get(0).productName;*/
		String[] providedProducts = {"37068", "37069", "37060"};
		String requestBody = CandlepinTasks.createPoolRequest(10, _3weeksago, _3min, "MKT-rhel-server", 123, providedProducts).toString();
		CandlepinTasks.postResourceUsingRESTfulAPI(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, "/owners/" + owner + "/subscriptions", requestBody);
		CandlepinTasks.refreshPoolsUsingRESTfulAPI(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, owner);
	}
	
	
	@Test 
	public void SubscribeToAboutToExpirePool_Test() throws Exception{
		clienttasks.unregister(null, null, null);
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
		
		Collection<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		
		Collection<SubscriptionPool> expiresToday = Collections2.filter(pools, expToday);
		
		//choose first pool
		SubscriptionPool testPool = expiresToday.iterator().next();
		String poolid = testPool.poolId;
		//String contract = testPool.
		
		//subscribe
		clienttasks.subscribeToSubscriptionPoolUsingPoolId(testPool);

		//wait for pools to expire
		Thread.sleep(120000);
		
		//verify that that the pool expired
		Collection<SubscriptionPool> matchedId = Collections2.filter(clienttasks.getCurrentlyAllAvailableSubscriptionPools(), new ByIdPredicate(poolid));
		Assert.assertEquals(matchedId.size(), 0, "Zero pools match id " + poolid);
		
		//verify that the subscription expired
		Collection<SubscriptionPool> matchedPools = Collections2.transform(clienttasks.getCurrentlyConsumedProductSubscriptions(),
				new Function<ProductSubscription, SubscriptionPool>(){
					public SubscriptionPool apply(ProductSubscription ps){	
						try {
							return clienttasks.getSubscriptionPoolFromProductSubscription(ps, owner, clientpassword);
						}catch (Exception e) {
							return null;
						} 	
					}});
		matchedId = Collections2.filter(matchedPools, new ByIdPredicate(poolid));
		Assert.assertEquals(matchedId.size(), 0, "Zero pools match id " + poolid);

	}
	
	
}
