package rhsm.cli.tests;

import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.EntitlementCert;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"RefreshTests"})
public class RefreshTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20023", "RHEL7-51040"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier1")
	@Test(	description="subscription-manager-cli: refresh and verify entitlements are updated",
			groups={"Tier1Tests","RefreshEntitlements_Test","blockedByBug-907638","blockedByBug-962520","blockedByBug-1366301"},
			enabled=true)
	@ImplementsNitrateTest(caseId=64182)	// http://gibson.usersys.redhat.com/agilo/ticket/4022
	public void testRefreshEntitlements() {
		
		// Start fresh by unregistering and registering...
		log.info("Start fresh by unregistering and registering...");
		clienttasks.unregister(null, null, null, null);
		clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null));
		
		// make sure the certFrequency will not affect the results of this test
		log.info("Change the certFrequency to a large value to assure the rhsmcertd does not interfere with this test.");
		clienttasks.restart_rhsmcertd(60, null, true);
		
		// Subscribe to a randomly available pool...
		log.info("Subscribe to a randomly available pool...");
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("There are no available pools at all to get entitlements from.  Cannot attempt this test.");
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribeToSubscriptionPool(pool);
		
		// remember the currently consumed product subscriptions (and entitlement certs)
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();

		// remove your entitlements
		log.info("Removing the entitlement certs...");
		clienttasks.removeAllCerts(false,true, false);
		Assert.assertEquals(clienttasks.getCurrentEntitlementCerts().size(),0,"Entitlements have been removed.");
		Assert.assertEquals(clienttasks.getCurrentlyConsumedProductSubscriptions().size(),0,"Consumed subscription pools do NOT exist after entitlements have been removed.");
		
		// mark the rhsm.log file
		String rhsmLogMarker = System.currentTimeMillis()+" Testing RefreshEntitlements_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, rhsmLogMarker);
		// The following was implemented by to temporarily avoid an IT blocked candlepin end point as described by https://bugzilla.redhat.com/show_bug.cgi?id=1366301#c2
		// python-rhsm-1.17.7-1 commits...
		// python-rhsm RHEL7.3 commit 2debbb619cba05727f44f6823ea60a2aa6b3e269 1366301: Entitlement regeneration no longer propagates server errors
		// python-rhsm RHEL7.3 commit cd43496585d5990500c4b272af9c4fa5e28661aa Update fix to include BadStatusLine responses from the server
		// python-rhsm RHEL7.3 commit c021772a21c34d07542b8e9a01dcfadfc223624b Ensure both cert regen methods succeed despite BadStatusLine from server
		// subscription-manager RHEL7.3 commit a07c9d7b890034a2e8e7e39fe32782b69def1736 1366301: Entitlement regeneration failure no longer aborts refresh
		//	2016-08-30 08:23:19,642 [DEBUG] subscription-manager:20105:MainThread @connection.py:573 - Making request: PUT /subscription/consumers/b29e0643-44b4-4d75-9912-47cf127be7ca/certificates?lazy_regen=true
		//	2016-08-30 08:23:20,070 [DEBUG] subscription-manager:20105:MainThread @connection.py:602 - Response: status=404
		//	2016-08-30 08:23:20,071 [DEBUG] subscription-manager:20105:MainThread @connection.py:1365 - Unable to refresh entitlement certificates: Service currently unsupported.
		//	2016-08-30 08:23:20,071 [DEBUG] subscription-manager:20105:MainThread @connection.py:1366 - Server error attempting a PUT to /subscription/consumers/b29e0643-44b4-4d75-9912-47cf127be7ca/certificates?lazy_regen=true returned status 404
		//	2016-08-30 08:23:20,072 [DEBUG] subscription-manager:20105:MainThread @managercli.py:652 - Warning: Unable to refresh entitlement certificates; service likely unavailable
		String warningMsg = "Warning: Unable to refresh entitlement certificates; service likely unavailable";	// 	subscription-manager RHEL7.3 commit a07c9d7b890034a2e8e7e39fe32782b69def1736 1366301: Entitlement regeneration failure no longer aborts refresh
		// "Unable to refresh entitlement certificates: Service currently unsupported."	// python-rhsm RHEL7.3 commit 2debbb619cba05727f44f6823ea60a2aa6b3e269 1366301: Entitlement regeneration no longer propagates server errors
		
		// refresh
		log.info("Refresh...");
		clienttasks.refresh(null, null, null, null);
		
		// was subscription-manager "Unable to refresh" as a result of a 404 trying to PUT?
		String rhsmLogTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, rhsmLogMarker, "Unable to refresh");
		boolean unableToRefresh = rhsmLogTail.contains(warningMsg);
		// if yes, then refresh behavior prior to Bug 1360909 will be tested
		if (unableToRefresh) log.warning("Due to encountered warning '"+warningMsg+"', the original refresh behavior prior to Bug 1360909 will be asserted...");
		
		// Assert the entitlement certs are restored after the refresh
		log.info("After running refresh, assert that the entitlement certs are restored...");
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.10-1") && !unableToRefresh) {	// 1360909: Added functionality for regenerating entitlement certificates
			// after the for Bug 1360909 - Clients unable to access newly released content (Satellite 6.2 GA)
			// subscription-manager refresh will grant a new entitlement serial, but the contents are usually identical (unless a content set has been updated)
			// assert nearly the same entitlement was restored (should only differ by serial number)
			List<EntitlementCert> entitlementCertsAfterRefresh = clienttasks.getCurrentEntitlementCerts();
			List<ProductSubscription> consumedProductSubscriptionsAfterRefresh = clienttasks.getCurrentlyConsumedProductSubscriptions();
			Assert.assertEquals(entitlementCertsAfterRefresh.size(), entitlementCerts.size(), "The number of entitlement certs granted after refresh should match the number before refresh.");
			Assert.assertEquals(consumedProductSubscriptionsAfterRefresh.size(), consumedProductSubscriptions.size(), "The number of consumed Product Subscriptions after refresh should match the number before refresh.");
			// The following asserts assume that one one entitlement was granted from a single pool
			Assert.assertEquals(entitlementCertsAfterRefresh.size(),1,"The remaining assertions in this test assume that an entitlement from only one pool was subscribed.  (If this test fails, then this test requires logic updates.)");
			Assert.assertTrue(!entitlementCertsAfterRefresh.get(0).serialString.equals(entitlementCerts.get(0).serialString),"Serial '"+entitlementCertsAfterRefresh.get(0).serialString+"' in the refreshed entitlement should NOT match serial '"+entitlementCerts.get(0).serialString+"' from the original entitlement.");
			Assert.assertEquals(entitlementCertsAfterRefresh.get(0).version,entitlementCerts.get(0).version,"Version in the refreshed entitlement should match the version from the original entitlement.");
			Assert.assertEquals(entitlementCertsAfterRefresh.get(0).validityNotBefore,entitlementCerts.get(0).validityNotBefore,"ValidityNotBefore in the refreshed entitlement should match the ValidityNotBefore from the original entitlement.");
			Assert.assertEquals(entitlementCertsAfterRefresh.get(0).validityNotAfter,entitlementCerts.get(0).validityNotAfter,"ValidityNotAfter in the refreshed entitlement should match the ValidityNotAfter from the original entitlement.");
			Assert.assertEquals(entitlementCertsAfterRefresh.get(0).orderNamespace,entitlementCerts.get(0).orderNamespace,"Order information in the refreshed entitlement should match the order information from the original entitlement.");
			Assert.assertEquals(entitlementCertsAfterRefresh.get(0).productNamespaces,entitlementCerts.get(0).productNamespaces,"Product information in the refreshed entitlement should match the product information from the original entitlement. (IF THIS FAILS, THEN THERE WAS AN UPSTREAM PROVIDED PRODUCT MODIFICATION LIKE THE SCENARIO IN BUG 1360909. HIGHLY UNLIKELY BETWEEN NOW AND THE START OF THIS TEST.)");
			Assert.assertEquals(entitlementCertsAfterRefresh.get(0).contentNamespaces,entitlementCerts.get(0).contentNamespaces,"Content information in the refreshed entitlement should match the content information from the original entitlement. (IF THIS FAILS, THEN THERE WAS AN UPSTREAM CONTENT MODIFICATION LIKE THE SCENARIO IN BUG 1360909. HIGHLY UNLIKELY BETWEEN NOW AND THE START OF THIS TEST.)");
			Assert.assertEquals(consumedProductSubscriptionsAfterRefresh.size(),1,"The remaining assertions in this test assume that an only one pool was subscribed.  (If this test fails, then this test requires logic updates.)");
			Assert.assertTrue(!consumedProductSubscriptionsAfterRefresh.get(0).serialNumber.equals(consumedProductSubscriptions.get(0).serialNumber),"Serial Number '"+consumedProductSubscriptionsAfterRefresh.get(0).serialNumber+"' in the refreshed product subscription should NOT match serial number '"+consumedProductSubscriptions.get(0).serialNumber+"' from the original product subscription.");
			Assert.assertEquals(consumedProductSubscriptionsAfterRefresh.get(0).poolId,consumedProductSubscriptions.get(0).poolId,"PoolId in the refreshed consumed product subscription should match the pool id from the original consumed product subscription.");
			// TODO continue asserting equality between fields of the consumedProductSubscriptions (all should be equal except serialNumber which was already asserted above)
		} else {
			// assert the exact same entitlement was restored
			Assert.assertEquals(clienttasks.getCurrentEntitlementCerts(),entitlementCerts,"Original entitlements have been restored.");
			Assert.assertEquals(clienttasks.getCurrentlyConsumedProductSubscriptions(),consumedProductSubscriptions,"Original consumed product subscriptions have been restored.");
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36619", "RHEL7-51430"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier2")
	@Test(	description="[abrt] subscription-manager-0.95.17-1.el6_1: Process /usr/bin/rhsmcertd was killed by signal 11 (SIGSEGV)",
			groups={"Tier2Tests","blockedByBug-725535","blockedByBug-907638","VerificationFixForBug725535_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	// http://gibson.usersys.redhat.com/agilo/ticket/4022
	public void testFixForBug725535() {
		
		// assert that rhsmcertd restart successfully before actually running this test
		clienttasks.restart_rhsmcertd(null,null,null);
		
		// block the ability of subscription-manager to write to /var/run/rhsm/update by creating a directory in its place
		removeRhsmUpdateFileAfterGroups();
		client.runCommandAndWait("mkdir "+clienttasks.rhsmUpdateFile);
		
		// mark the /var/log/messages so we can search for an abrt afterwards
		String marker = "SM TestClass marker "+String.valueOf(System.currentTimeMillis());	// using a timestamp on the class marker will help identify the test class during which a denial is logged
		RemoteFileTasks.markFile(client, clienttasks.messagesLogFile, marker);
		clienttasks.restart_rhsmcertd(null,null,null);
		
		// ON RHEL6...
		//	[root@jsefler-onprem-62server ~]# tail /var/log/rhsm/rhsmcertd.log
		//	Tue Sep 27 17:36:32 2011: started: interval = 240 minutes
		//	Tue Sep 27 17:36:32 2011: started: interval = 1440 minutes
		//	Tue Sep 27 17:36:32 2011: certificates updated
		//	Tue Sep 27 17:36:32 2011: error opening /var/run/rhsm/update to write
		//	timestamp: Is a directory
		//	Tue Sep 27 17:36:32 2011: certificates updated
		//	Tue Sep 27 17:36:32 2011: error opening /var/run/rhsm/update to write
		//	timestamp: Is a directory
		
		// ON RHEL6...
		//	[root@jsefler-onprem-62server ~]# tail -f /var/log/messages
		//	Sep 27 14:58:42 jsefler-onprem-62server kernel: rhsmcertd[7117]: segfault at 0 ip 00000039f7a665be sp 00007fff37437d40 error 4 in libc-2.12.so[39f7a00000+197000]
		//	Sep 27 14:58:42 jsefler-onprem-62server abrt[7174]: saved core dump of pid 7117 (/usr/bin/rhsmcertd) to /var/spool/abrt/ccpp-2011-09-27-14:58:42-7117.new/coredump (323584 bytes)
		//	Sep 27 14:58:42 jsefler-onprem-62server abrtd: Directory 'ccpp-2011-09-27-14:58:42-7117' creation detected
		//	Sep 27 14:58:42 jsefler-onprem-62server abrtd: Package 'subscription-manager' isn't signed with proper key
		//	Sep 27 14:58:42 jsefler-onprem-62server abrtd: Corrupted or bad dump /var/spool/abrt/ccpp-2011-09-27-14:58:42-7117 (res:2), deleting
		//	Sep 27 14:58:43 jsefler-onprem-62server kernel: rhsmcertd[7119]: segfault at 0 ip 00000039f7a665be sp 00007fff37437d40 error 4 in libc-2.12.so[39f7a00000+197000]
		//	Sep 27 14:58:43 jsefler-onprem-62server abrt[7201]: not dumping repeating crash in '/usr/bin/rhsmcertd'
		
		// ON RHEL7...
		//	[root@jsefler-7 ~]# tail -f /var/log/messages
		//	SM TestClass marker 1383686002365
		//	Nov  5 16:13:49 jsefler-7 systemd: Stopping Enable periodic update of entitlement certificates....
		//	Nov  5 16:13:49 jsefler-7 systemd: Starting Enable periodic update of entitlement certificates....
		//	Nov  5 16:13:49 jsefler-7 systemd: Started Enable periodic update of entitlement certificates..
		
		// verify that no subscription-manager abrt was logged to /var/log/messages 
		Assert.assertTrue(RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.messagesLogFile, marker, "abrt").trim().equals(""), "No segfault was logged in '"+clienttasks.messagesLogFile+"' on "+client.getConnection().getRemoteHostname()+" while regression testing bug 725535.");
		//Assert.assertTrue(RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.varLogMessagesFile, marker, null).trim().equals(""), "No segfault was logged in '"+clienttasks.varLogMessagesFile+"' on "+client.getConnection().getRemoteHostname()+" while regression testing bug 725535.");
		//Assert.assertTrue(RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.varLogMessagesFile, marker, clienttasks.hostname.split("\\.")[0]+"' | grep -v 'Enable periodic update of entitlement certificates").trim().equals(""), "No segfault was logged in '"+clienttasks.varLogMessagesFile+"' on "+client.getConnection().getRemoteHostname()+" while regression testing bug 725535.");
	}
	
	
	// Candidates for an automated Test:
	// TODO Bug 665118 - Refresh pools will not notice change in provided products https://github.com/RedHatQE/rhsm-qe/issues/180
	
	
	
	// Configuration methods ***********************************************************************

	@BeforeGroups(value="VerificationFixForBug725535_Test",groups={"setup"})
	@AfterGroups(value="VerificationFixForBug725535_Test",groups={"setup"})
	public void removeRhsmUpdateFileAfterGroups () {
		if (clienttasks==null) return;
		client.runCommandAndWait("rm -f "+clienttasks.rhsmUpdateFile+"; rmdir "+clienttasks.rhsmUpdateFile);
		//client.runCommandAndWait("rm -f "+clienttasks.rhsmUpdateFile);
		//client.runCommandAndWait("rmdir "+clienttasks.rhsmUpdateFile);
		Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.rhsmUpdateFile), "rhsm update file '"+clienttasks.rhsmUpdateFile+"' has been removed.");
	}
	
	@AfterClass(groups={"setup"})
	public void rhsmcertdServiceRestartAfterClass () {
		if (clienttasks==null) return;
		clienttasks.restart_rhsmcertd(null,null,null);
	}
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************


}
