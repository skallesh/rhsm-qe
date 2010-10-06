package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;

/**
 * @author ssalevan
 * @author jsefler
 *
 */
@Test(groups={"unsubscribe"})
public class UnsubscribeTests extends SubscriptionManagerCLITestScript{
	
	@Test(description="subscription-manager-cli: unsubscribe consumer to an entitlement using product ID",
//			dependsOnGroups={"sm_stage4"},
//			groups={"sm_stage5", "blockedByBug-584137", "blockedByBug-602852"},
			groups={"blockedByBug-584137", "blockedByBug-602852"},
			dataProvider="getAllConsumedProductSubscriptionsData")
	@ImplementsTCMS(id="41688")
	public void UnsubscribeFromValidProductIDs_Test(ProductSubscription productSubscription){
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
//		sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.unsubscribeFromProductSubscription(productSubscription);
	}
	
	
	@Test(description="Unsubscribe product entitlement and re-subscribe",
//			dependsOnGroups={"sm_stage4"},
//			groups={"sm_stage5", "blockedByBug-584137", "blockedByBug-602852"},
			groups={"blockedByBug-584137", "blockedByBug-602852"},
			dataProvider="getAllConsumedProductSubscriptionsData")
	@ImplementsTCMS(id="41898")
	public void ResubscribeAfterUnsubscribe_Test(ProductSubscription productSubscription) throws Exception{
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
//		sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		
//		// first make sure we are subscribed to all pools
//		sm.register(username,password,null,null,null,null);
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		
		// now loop through each consumed product subscription and unsubscribe/re-subscribe
		SubscriptionPool pool = clienttasks.getSubscriptionPoolFromProductSubscription(productSubscription,serverHostname,serverPort,clientOwnerUsername,clientOwnerPassword);
		if (clienttasks.unsubscribeFromProductSubscription(productSubscription))
			clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);	// only re-subscribe when unsubscribe was a success
	}
	
	
// FIXME DELETEME This test was re-designed as a data-provided test below
//	@Test(description="Copy entitlement certificates into /etc/pki/entitlement/product after unsubscribe",
//			dependsOnGroups={"sm_stage4"},
//			groups={"sm_stage5", "blockedByBug-584137", "blockedByBug-602852"})
//	@ImplementsTCMS(id="41903")
//	public void UnsubscribeAndReplaceCert_Test(){
//		sshCommandRunner.runCommandAndWait("killall -9 yum");
//		String randDir = "/tmp/sm-certs-"+Integer.toString(this.getRandInt());
//		
//		// make sure we are registered and then subscribe to each available subscription pool
//		sm.register(username,password,null,null,null,null);
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
//		
//		// copy certs to temp dir
//		sshCommandRunner.runCommandAndWait("rm -rf "+randDir);
//		sshCommandRunner.runCommandAndWait("mkdir -p "+randDir);
//		sshCommandRunner.runCommandAndWait("cp /etc/pki/entitlement/product/* "+randDir);
//		
////		sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
//		sm.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
//		
//		sshCommandRunner.runCommandAndWait("cp -f "+randDir+"/* /etc/pki/entitlement/product");
//		RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,
//				"yum repolist");
//		
//		/* FIXME Left off development here:
//201007152042:41.317 - FINE: ssh root@jsefler-rhel6-clientpin.usersys.redhat.com yum repolist (com.redhat.qe.tools.SSHCommandRunner.run)
//201007152042:42.353 - FINE: Stdout: 
//Loaded plugins: refresh-packagekit, rhnplugin, rhsmplugin
//Updating Red Hat repositories.
//repo id                              repo name                            status
//never-enabled-content                never-enabled-content                0
//rhel-latest                          Latest RHEL 6                        0
//repolist: 0
// (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
//201007152042:42.354 - FINE: Stderr: 
//This system is not registered with RHN.
//RHN support will be disabled.
//http://redhat.com/foo/path/never/repodata/repomd.xml: [Errno 14] HTTP Error 404 : http://www.redhat.com/foo/path/never/repodata/repomd.xml 
//Trying other mirror.
// (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
//201007152042:42.360 - SEVERE: Test Failed: UnsubscribeAndReplaceCert_Test (com.redhat.qe.auto.testng.TestNGListener.onTestFailure)
//java.lang.AssertionError: Command 'yum repolist' returns nonzero error code: 0 expected:<true> but was:<false>
//		 */
//	}
	
	@Test(description="Entitlement, malicious negative tests",
//			dependsOnGroups={"sm_stage4"},
//			groups={"sm_stage5", "blockedByBug-584137", "blockedByBug-602852"},
			groups={"blockedByBug-584137", "blockedByBug-602852"},
			dataProvider="getAvailableSubscriptionPoolsData")
	@ImplementsTCMS(id="41903")
	public void EntitlementMaliciousNegative_Test(SubscriptionPool subscriptionPool){
		client.runCommandAndWait("killall -9 yum");
		
		// subscribe to a pool
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPoolUsingPoolId(subscriptionPool);
		List <EntitlementCert> entitlementCerts = clienttasks.getEntitlementCertsFromEntitlementCertFile(entitlementCertFile);

		// assert all of the entitlement certs are reported in the "yum repolist all"
		clienttasks.assertEntitlementCertsInYumRepolist(entitlementCerts,true);
 
		// copy entitlement certificate from location /etc/pki/entitlement/product/ to /tmp
		String randDir = "/tmp/sm-certForSubscriptionPool-"+subscriptionPool.poolId;
		client.runCommandAndWait("rm -rf "+randDir);
		client.runCommandAndWait("mkdir -p "+randDir);
		client.runCommandAndWait("cp "+entitlementCertFile.getPath()+" "+randDir);
		
		// unsubscribe from the pool (Note: should be the only one subscribed too
		clienttasks.unsubscribeFromSerialNumber(clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCertFile));
		
		// assert all of the entitlement certs are no longer reported in the "yum repolist all"
		clienttasks.assertEntitlementCertsInYumRepolist(entitlementCerts,false);

		// restart the rhsm cert deamon
		int certFrequency = 1;
		clienttasks.restart_rhsmcertd(certFrequency, false);
		
		// move the copied entitlement certificate from /tmp to location /etc/pki/entitlement/product
		// Note: this is malicious activity
		client.runCommandAndWait("cp -f "+randDir+"/* "+clienttasks.entitlementCertDir+"/product");
		
		// assert all of the entitlement certs are reported in the "yum repolist all" again
		clienttasks.assertEntitlementCertsInYumRepolist(entitlementCerts,true);
		
		// assert that the rhsmcertd will clean up the malicious activity
		log.info("Now let's restart the rhsmcertd and assert that the deamon deletes the entitlement certificate since it was put on candlepins certificate revocation list during the unsubscribe.");
		SubscriptionManagerCLITestScript.sleep(certFrequency*60*1000);
		Assert.assertTrue(RemoteFileTasks.testFileExists(client, entitlementCertFile.getPath())==0,"Entitlement certificate '"+entitlementCertFile+"' was deleted by the rhsm certificate deamon.");
		clienttasks.assertEntitlementCertsInYumRepolist(entitlementCerts,false);
	}

	
	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="getAllConsumedProductSubscriptionsData")
	public Object[][] getAllConsumedProductSubscriptionsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getAllConsumedProductSubscriptionsDataAsListOfLists());
	}
	protected List<List<Object>> getAllConsumedProductSubscriptionsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
		// first make sure we are subscribed to all pools
		clienttasks.unregister();
		clienttasks.register(clientusername,clientpassword,null,null,null,null);
//		c1sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools(null);

		
		// then assemble a list of all consumed ProductSubscriptions
		for (ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			ll.add(Arrays.asList(new Object[]{productSubscription}));		
		}
		
		return ll;
	}
	

}
