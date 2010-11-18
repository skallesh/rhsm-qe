package com.redhat.qe.sm.cli.tests;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.SubscriptionPool;

public class ExpirationTests extends SubscriptionManagerCLITestScript {

	@BeforeClass(groups="setup")
	public void checkTime() throws Exception{
		//make sure local clock and server clock are synced
		Date localTime = Calendar.getInstance().getTime();
		Date remoteTime; 
		//SSHCommandRunner runner = new SSHCommandRunner("jweiss-rhel6-1.usersys.redhat.com", sshUser, sshKeyPrivate, sshkeyPassphrase, null);
		server.runCommandAndWait("date");
		String serverDateStr = server.getStdout();
		SimpleDateFormat unixFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
		remoteTime = unixFormat.parse(serverDateStr);
		long timeDiffms = Math.abs(localTime.getTime() - remoteTime.getTime());
		Assert.assertLess(timeDiffms, 60000L, "Time difference with candlepin server is less than 1 minute");
	}
	
	@BeforeMethod
	public void createTestPools() throws Exception{
		String owner = "donaldduck";
		String ownerpw = "password";
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, 3); //FIXME = minutes
		Date _3min = cal.getTime();
		cal.add(Calendar.DATE, -21);
		Date _3weeksago = cal.getTime();
		/*List<InstalledProduct> clientProds = clienttasks.getCurrentlyInstalledProducts();
		String product = clientProds.get(0).productName;*/
		String[] providedProducts = {"37068", "37069", "37060"};
		String requestBody = CandlepinTasks.createPoolRequest(10, _3weeksago, _3min, "MKT-rhel-server", 123, providedProducts).toString();
		CandlepinTasks.postResourceUsingRESTfulAPI(serverHostname, serverPort, serverPrefix, clientOwnerUsername, clientOwnerPassword, "/owners/" + owner + "/subscriptions", requestBody);
		CandlepinTasks.refreshPoolsUsingRESTfulAPI(serverHostname, serverPort, serverPrefix, clientOwnerUsername, clientOwnerPassword, owner);
	}
	
	
	@Test 
	public void SubscribeToAboutToExpirePool_Test(){
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
		
		Collection<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		Predicate<SubscriptionPool> expToday = new Predicate<SubscriptionPool>(){
			public boolean apply(SubscriptionPool pool){
				Calendar cal = pool.endDate;
				Calendar today = new GregorianCalendar();
				//cal.getTime().
				return (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) && (cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) ;
			}
		};
		Collection<SubscriptionPool> expiresToday = Collections2.filter(pools, expToday);
		
		//choose first pool
		SubscriptionPool testPool = expiresToday.iterator().next();
		
		//subscribe
		clienttasks.subscribeToSubscriptionPoolUsingPoolId(testPool);

	}
	
	public static void main(String... args) throws Exception{
		ExpirationTests test = new ExpirationTests();
		test.checkTime();
	}
}
