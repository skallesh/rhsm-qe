package com.redhat.qe.sm.tests;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testng.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.abstractions.CandlepinAbstraction;
import com.redhat.qe.sm.abstractions.EntitlementCert;
import com.redhat.qe.sm.abstractions.ProductSubscription;
import com.redhat.qe.sm.abstractions.SubscriptionPool;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.sm.tasks.SubscriptionManagerTasks;
import com.redhat.qe.tools.RemoteFileTasks;

@Test(groups={"subscribe"})
public class SubscribeTests extends SubscriptionManagerTestScript{
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using product ID",
			enabled=false,	// Subscribing to a Subscription Pool using --product Id has been removed in subscription-manager-0.71-1.el6.i686.
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4", "blockedByBug-584137"},
			groups={"blockedByBug-584137"},
			dataProvider="getAllAvailableSubscriptionPoolsData")
	@ImplementsTCMS(id="41680")
	public void SubscribeToValidSubscriptionsByProductID_Test(SubscriptionPool pool){
//		sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using product ID",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4", "blockedByBug-584137", "not_implemented"},
			groups={"blockedByBug-584137"},
			enabled=false)
	public void SubscribeToASingleEntitlementByProductID_Test(){
		clienttasks.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		SubscriptionPool MCT0696 = new SubscriptionPool("MCT0696", "biteme");
		MCT0696.addProductID("Red Hat Directory Server");
		clienttasks.subscribeToSubscriptionPoolUsingProductId(MCT0696);
		//this.refreshSubscriptions();
		for (ProductSubscription pid:MCT0696.associatedProductIDs){
			Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().contains(pid),
					"ProductID '"+pid.productName+"' consumed from Pool '"+MCT0696.subscriptionName+"'");
		}
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using pool ID",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4", "blockedByBug-584137"},
			groups={"blockedByBug-584137"},
			dataProvider="getAllAvailableSubscriptionPoolsData")
	@ImplementsTCMS(id="41686")
	public void SubscribeToValidSubscriptionsByPoolID_Test(SubscriptionPool pool){
// non-dataProvided test procedure
//		sm.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to each available subscription pool using pool ID",
			groups={"blockedByBug-584137"},
			dataProvider="getValidConsumerData")
	@ImplementsTCMS(id="41686")
	public void SubscribeConsumerToEachAvailableSubscriptionPoolUsingPoolId_Test(String username, String password, String type, String consumerId){
		clienttasks.unregister();
		clienttasks.register(username, password, type, consumerId, Boolean.FALSE, Boolean.FALSE);
		clienttasks.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using registration token",
//			dependsOnGroups={"sm_stage8"},
//			groups={"sm_stage9", "blockedByBug-584137", "not_implemented"},
			groups={"blockedByBug-584137"},
			enabled=false)
	@ImplementsTCMS(id="41681")
	public void SubscribeToRegToken_Test(){
		clienttasks.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.subscribeToRegToken(regtoken);
	}
	
	
	@Test(	description="Subscribed for Already subscribed Entitlement.",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4", "blockedByBug-584137", "not_implemented"},
			groups={"blockedByBug-584137"},
			enabled=false)
	@ImplementsTCMS(id="41897")
	public void SubscribeAndSubscribeAgain_Test(){
		//sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		for(SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
			clienttasks.subscribeToProduct(pool.subscriptionName);
		}
	}
	

// FIXME: THIS ORIGINAL TEST WAS NOT COMPLETE.  REPLACEMENT BELOW IS WORK IN PROGRESS
//	@Test(description="subscription-manager Yum plugin: enable/disable",
//			dependsOnGroups={"sm_stage5"},
//			groups={"sm_stage6"})
//	@ImplementsTCMS(id="41696")
//	public void EnableYumRepoAndVerifyContentAvailable_Test(){
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
//		this.adjustRHSMYumRepo(true);
//		/*for(ProductID sub:this.consumedProductIDs){
//			ArrayList<String> repos = this.getYumRepolist();
//			Assert.assertTrue(repos.contains(sub.productId),
//					"Yum reports product subscribed to repo: " + sub.productId);
//		}*/
//	}
	@Test(	description="subscription-manager Yum plugin: enable/disable",
//			dependsOnGroups={"sm_stage5"},
//			groups={"sm_stage6", "not_implemented"},
			enabled=false)
	@ImplementsTCMS(id="41696")
	public void EnableYumRepoAndVerifyContentAvailable_Test() {
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null);
		clienttasks.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		
		// Edit /etc/yum/pluginconf.d/rhsmplugin.conf and ensure that the enabled directive is set to 1
		clienttasks.adjustRHSMYumRepo(true);

		// 1. Run a 'yum repolist' and get a list of all of the available repositories corresponding to your entitled products
		// 1. Repolist contains repositories corresponding to your entitled products
//		for(ProductSubscription sub:sm.getCurrentlyConsumedProductSubscriptions()){
//			ArrayList<String> repos = this.getYumRepolist();
//			Assert.assertTrue(repos.contains(sub.productId),
//					"Yum reports product subscribed to repo: " + sub.productId);
//		}
		
		// Edit /etc/yum/pluginconf.d/rhsmplugin.conf and ensure that the enabled directive is set to 0
		clienttasks.adjustRHSMYumRepo(false);
		
		// 2. Run a 'yum repolist' and get a list of all of the available repositories corresponding to your entitled products
		// 2. Repolist does not contain repositories corresponding to your entitled products
throw new SkipException("THIS TESTCASE IS UNDER CONSTRUCTION. IMPLEMENTATION OF https://tcms.engineering.redhat.com/case/41696/?search=41696");		

	}
	
	
	@Test(	description="subscription-manager Yum plugin: ensure ...",
//	        dependsOnGroups={"sm_stage6"},
//	        groups={"sm_stage7"},
	        enabled=true)
	@ImplementsTCMS(id="47578")
	public void VerifyReposAvailableForEnabledContent(){
//	    ArrayList<String> repos = this.getYumRepolist();
//	    
//	    for (EntitlementCert cert:clienttasks.getCurrentEntitlementCerts()){
//	    	if(cert.enabled.contains("1"))
//	    		Assert.assertTrue(repos.contains(cert.label),
//	    				"Yum reports enabled content subscribed to repo: " + cert.label);
//	    	else
//	    		Assert.assertFalse(repos.contains(cert.label),
//	    				"Yum reports enabled content subscribed to repo: " + cert.label);
//	    }
// FIXME: Untested Alternative to above procedure is:
		clienttasks.unregister();
	    clienttasks.register(clientusername, clientpassword, null, null, null, null);
	    clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools();
	    List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
	    Assert.assertTrue(!entitlementCerts.isEmpty(),"After subscribing to all available subscription pools, there must be some entitlements."); // or maybe we should skip when nothing is consumed 
	    clienttasks.assertEntitlementCertsAreReportedInYumRepolist(entitlementCerts);
	}
	
	
	@Test(	description="subscription-manager Yum plugin: ensure content can be downloaded/installed",
//			dependsOnGroups={"sm_stage6"},
//			groups={"sm_stage7", "not_implemented"},
			enabled=false)
	@ImplementsTCMS(id="41695")
	public void InstallPackageFromRHSMYumRepo_Test(){
		HashMap<String, String[]> pkgList = clienttasks.getPackagesCorrespondingToSubscribedRepos();
		for(ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()){
			String pkg = pkgList.get(productSubscription.productName)[0];
			log.info("Attempting to install first pkg '"+pkg+"' from product subscription: "+productSubscription);
			log.info("timeout of two minutes for next three commands");
			RemoteFileTasks.runCommandExpectingNoTracebacks(client,
					"yum repolist");
			RemoteFileTasks.runCommandExpectingNoTracebacks(client,
					"yum install -y "+pkg);
			RemoteFileTasks.runCommandExpectingNoTracebacks(client,
					"rpm -q "+pkg);
		}
	}
	
	
	@Test(	description="subscription-manager Yum plugin: enable/disable",
//			dependsOnGroups={"sm_stage7"},
//			groups={"sm_stage8", "not_implemented"},
			enabled=false)
	@ImplementsTCMS(id="41696")
	public void DisableYumRepoAndVerifyContentNotAvailable_Test(){
		clienttasks.adjustRHSMYumRepo(false);
		for(SubscriptionPool sub:clienttasks.getCurrentlyAvailableSubscriptionPools())
			for(String repo:this.getYumRepolist())
				if(repo.contains(sub.subscriptionName))
					Assert.fail("After unsubscribe, Yum still has access to repo: "+repo);
	}
	
	
	@Test(	description="rhsmcertd: change certFrequency",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4"},
//			groups={"blockedByBug-617703"},
			enabled=true)
	@ImplementsTCMS(id="41692")
	public void certFrequency_Test() {
		int minutes = 1;
		clienttasks.changeCertFrequency(minutes);
		
		log.info("Appending a marker in the '"+SubscriptionManagerTasks.rhsmcertdLogFile+"' so we can assert that the certificates are being updated every '"+minutes+"' minutes");
		String marker = "testing rhsm.conf certFrequency="+minutes; // https://tcms.engineering.redhat.com/case/41692/
		RemoteFileTasks.runCommandAndAssert(client,"echo \""+marker+"\" >> "+SubscriptionManagerTasks.rhsmcertdLogFile,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+SubscriptionManagerTasks.rhsmcertdLogFile,Integer.valueOf(0),marker,null);

		sleep(minutes*60*1000 + 500);	// give the rhsmcertd a chance check in with the candlepin server and update the certs

//		Assert.assertEquals(RemoteFileTasks.grepFile(client,
//				rhsmcertdLogFile,
//				"certificates updated"),
//				0,
//				"rhsmcertd reports that certificates have been updated at new interval");
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+SubscriptionManagerTasks.rhsmcertdLogFile,Integer.valueOf(0),"certificates updated",null);
		
		/* FIXME: Notice from this output that the update may fail a few times before it starts working.  Hence this test often fails as written above.
		Tue Jul 27 15:33:23 2010: started: interval = 1 minutes
		testing rhsm.conf certFrequency=1
		Tue Jul 27 15:33:24 2010: update failed (1), retry in 1 minutes
		Tue Jul 27 15:34:24 2010: update failed (1), retry in 1 minutes
		Tue Jul 27 15:35:24 2010: update failed (1), retry in 1 minutes
		Tue Jul 27 15:36:24 2010: certificates updated
		Tue Jul 27 15:37:25 2010: certificates updated
		Tue Jul 27 15:38:25 2010: certificates updated
		*/

	}
	
	
	@Test(	description="rhsmcertd: ensure certificates synchronize",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4"},
//			groups={"blockedByBug-617703"},
			enabled=true)
	@ImplementsTCMS(id="41694")
	public void refreshCerts_Test(){
		clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools();
		//SubscribeToASingleEntitlementByProductID_Test();
		client.runCommandAndWait("rm -f /etc/pki/entitlement/*");
		client.runCommandAndWait("rm -f /etc/pki/entitlement/product/*");
		client.runCommandAndWait("rm -f /etc/pki/product/*");
		certFrequency_Test();
//		client.runCommandAndWait("cat /dev/null > "+rhsmcertdLogFile);
//		//sshCommandRunner.runCommandAndWait("rm -f "+rhsmcertdLogFile);
//		//sshCommandRunner.runCommandAndWait("/etc/init.d/rhsmcertd restart");
//		this.sleep(70*1000);
//		
//		Assert.assertEquals(RemoteFileTasks.grepFile(client,
//				rhsmcertdLogFile,
//				"certificates updated"),
//				0,
//				"rhsmcertd reports that certificates have been updated");
		
		//verify that PEM files are present in all certificate directories
		RemoteFileTasks.runCommandAndAssert(client, "ls /etc/pki/entitlement | grep pem", 0, "pem", null);
		RemoteFileTasks.runCommandAndAssert(client, "ls /etc/pki/entitlement/product | grep pem", 0, "pem", null);
		// this directory will only be populated if you upload ur own license, not while working w/ candlepin
		/*RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "ls /etc/pki/product", 0, "pem", null);*/
	}
	
	
	@Test(	description="bind/unbind with two users/consumers",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4"},
			groups={},
			dataProvider="getAllAvailableSubscriptionPoolsData")
	@ImplementsTCMS(id="53217")
	public void MultiClientSubscribeToSameSubscriptionPool_Test(SubscriptionPool pool) {
		if (client2==null) throw new SkipException("This test requires a second consumer.");
		
		// assert that the subscriptionPool is available to both consumers with the same quantity
		List<SubscriptionPool> cl1SubscriptionPools = client1tasks.getCurrentlyAvailableSubscriptionPools();
		Assert.assertTrue(cl1SubscriptionPools.contains(pool),"Subscription pool "+pool+" is available to consumer1 ("+client1username+").");
		SubscriptionPool cl1SubscriptionPool = cl1SubscriptionPools.get(cl1SubscriptionPools.indexOf(pool));
		
		List<SubscriptionPool> cl2SubscriptionPools = client2tasks.getCurrentlyAvailableSubscriptionPools();
		Assert.assertTrue(cl2SubscriptionPools.contains(pool),"Subscription pool "+pool+" is available to consumer2 ("+client2username+").");
		SubscriptionPool cl2SubscriptionPool = cl2SubscriptionPools.get(cl2SubscriptionPools.indexOf(pool));

		Assert.assertEquals(cl1SubscriptionPool.quantity, cl2SubscriptionPool.quantity, "The quantity of entitlements from subscription pool id '"+pool.poolId+"' available to both consumers is the same.");

		// subscribe consumer1 to the pool and assert that the available quantity to consumer2 has decremented by one
		client1tasks.subscribeToSubscriptionPoolUsingPoolId(pool);
		cl2SubscriptionPools = client2tasks.getCurrentlyAvailableSubscriptionPools();
		Assert.assertTrue(cl2SubscriptionPools.contains(pool),"Subscription pool id "+pool.poolId+" is still available to consumer2 ("+client2username+").");
		cl2SubscriptionPool = cl2SubscriptionPools.get(cl2SubscriptionPools.indexOf(pool));

		Assert.assertEquals(Integer.valueOf(cl2SubscriptionPool.quantity).intValue(), Integer.valueOf(cl1SubscriptionPool.quantity).intValue()-1, "The quantity of entitlements from subscription pool id '"+pool.poolId+"' available to consumer2 ("+client2username+") has decremented by one.");
	}
	
	protected static String rhelPersonalProductName = "RHEL Personal";
	protected static String rhelPersonalBitsProductName = "RHEL Personal Bits";
	/*
	 * REFERENCE MATERIAL:
	 * https://tcms.engineering.redhat.com/case/55702/
	 * https://tcms.engineering.redhat.com/case/55718/
	 * https://engineering.redhat.com/trac/IntegratedMgmtQE/wiki/RH-Personal_dev_testplan
	 * 
Data prep
==========================================================
(in candlepin/client/ruby - assuming that the cp_product_utils product data has been imported)

./cpc create_owner john
./cpc create_user 10 john password   (10 happened to be the id returned from creating the john owner)
./cpc create_subscription 10 RH09XYU34  (defaults to a quantity of 1)
./cpc refresh_pools john

RHSM
===========================================================
# Simulating a person accepting RHEL Personal
sudo ./subscription-manager-cli register --username=john --password=password --type=person
sudo ./subscription-manager-cli list --available     # (Should see RHEL Personal)
sudo ./subscription-manager-cli subscribe --pool=13  # (or whatever the pool id is for RHEL Personal)

# AT THIS POINT YOU WILL NOT HAVE ANY ENTITLEMENT CERTS
# THIS DOES NOT MATTER, AS THIS IS NOT HOW A RHEL PERSONAL CUSTOMER WILL ACTUALLY CONSUME THIS ENTITLEMENT

# Stash the consumer certs - this really just simulates using a different machine
sudo mv /etc/pki/consumer/*.pem /tmp

# Now register a system consumer under john's username
sudo ./subscription-manager-cli register --username=john --password=password
sudo ./subscription-manager-cli list --available    # (Should see RHEL Personal Bits - this product actually should create a real entitlement cert and allow this machine access to the actual content, hence the silly name)

sudo ./subscription-manager-cli subscribe --pool=14  # (RHEL Personal Bits pool)
sudo ./subscription-manager-cli list                 # (This should actually show entitlement cert information)

I don't think that we have valid content data currently baked into the certs, so I don't think that you can use this to access protected yum repos yet.

	 */
	@Test(	description="subscription-manager-cli: Ensure RHEL Personal Bits are available and unlimited after a person has subscribed to RHEL Personal",
			groups={"myDevGroup","MultiClientEnsureRHELPersonalBitsAreAvailableAfterRegisteredPersonSubscribesToRHELPersonal_Test", "MultiClientRHELPersonal"},
			enabled=true)
	@ImplementsTCMS(id="55702")
	public void MultiClientEnsureRHELPersonalBitsAreAvailableAfterRegisteredPersonSubscribesToRHELPersonal_Test() {
		if (!isServerOnPremises) throw new SkipException("Currently this test is designed only for on-premises.");	//TODO Make this work for IT too.  jsefler 8/12/2010 
		if (client2==null) throw new SkipException("This test requires a second consumer.");
		if (clientusername.equals("admin")) throw new SkipException("This test requires that the client user ("+clientusername+") is NOT admin.");
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=623657 - jsefler 8/12/2010
		Boolean invokeWorkaroundWhileBugIsOpen = false;
		try {String bugId="624423"; if (BzChecker.getInstance().isBugOpen(bugId)&&invokeWorkaroundWhileBugIsOpen) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla bug "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			servertasks.restartTomcat();
		} // END OF WORKAROUND
		
		
		SubscriptionPool rhelPersonalPool = null;
		
		
		log.info("Register client2 under username '"+clientusername+"' as a system and assert that RHEL Personal Bits are NOT yet available...");
		client2tasks.unregister();
		client2tasks.register(clientusername, clientpassword, "system", null, null, null);
		List<SubscriptionPool> client2BeforeSubscriptionPools = client2tasks.getCurrentlyAvailableSubscriptionPools();
		rhelPersonalPool = client2tasks.findSubscriptionPoolWithMatchingFieldFromList("subscriptionName",rhelPersonalBitsProductName,client2BeforeSubscriptionPools);
		Assert.assertTrue(rhelPersonalPool==null,rhelPersonalBitsProductName+" is NOT yet available to client2 system '"+client2.getConnection().getHostname()+"' registered under user '"+clientusername+"'.");

		
		log.info("Now register client1 under username '"+clientusername+"' as a person and subscribe to the RHEL Personal subscription pool...");
		client1tasks.unregister();
		client1tasks.register(clientusername, clientpassword, "person", null, null, null);
		rhelPersonalPool = client1tasks.findSubscriptionPoolWithMatchingFieldFromList("subscriptionName",rhelPersonalProductName,client1tasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertTrue(rhelPersonalPool!=null,rhelPersonalProductName+" is available to user '"+clientusername+"' registered as a person.");
		List<String> beforeEntitlementCertFiles = client1tasks.getCurrentEntitlementCertFiles();
		client1tasks.subscribe(rhelPersonalPool.poolId, null, null, null, null);
		Assert.assertTrue(!client1tasks.getCurrentlyAvailableSubscriptionPools().contains(rhelPersonalPool),
				"The available subscription pools no longer contains the just subscribed to pool: "+rhelPersonalPool);
		List<String> afterEntitlementCertFiles = client1tasks.getCurrentEntitlementCertFiles();
		Assert.assertTrue(afterEntitlementCertFiles.equals(beforeEntitlementCertFiles),
				"Subscribing to subscription pool '"+rhelPersonalProductName+"' does NOT drop a new entitlement certificate when registered as a person.");

		
		log.info("Now client2 (already registered as a system under username '"+clientusername+"') should now have RHEL Personal Bits available with unlimited quantity...");
		List<SubscriptionPool> client2AfterSubscriptionPools = client2tasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool rhelPersonalBitsPool = client2tasks.findSubscriptionPoolWithMatchingFieldFromList("subscriptionName",rhelPersonalBitsProductName,client2AfterSubscriptionPools);
		Assert.assertTrue(rhelPersonalBitsPool!=null,rhelPersonalBitsProductName+" is now available to client2 system '"+client2.getConnection().getHostname()+"' registered under user '"+clientusername+"'.");
		Assert.assertEquals(rhelPersonalBitsPool.quantity.toLowerCase(),"unlimited","An unlimited quantity of entitlements is available to "+rhelPersonalBitsProductName+".");
		
		
		log.info("Verifying that the available subscription pools available to client2 has increased by only the "+rhelPersonalBitsProductName+" pool...");
		client2BeforeSubscriptionPools.add(rhelPersonalBitsPool);  // manually add the rhelPersonalBitsPool to the List
		Assert.assertEquals(client2AfterSubscriptionPools,client2BeforeSubscriptionPools,
				"The list of available subscription pools seen by client2 increases only by RHEL Personal Bits pool: "+rhelPersonalBitsPool);
	
	}
	@Test(	description="subscription-manager-cli: Ensure RHEL Personal Bits are consumable after a person has subscribed to RHEL Personal",
			groups={"myDevGroup","MultiClientEnsureRHELPersonalBitsAreConsumableAfterRegisteredPersonSubscribesToRHELPersonal_Test","MultiClientRHELPersonal"},
			dependsOnGroups={"MultiClientEnsureRHELPersonalBitsAreAvailableAfterRegisteredPersonSubscribesToRHELPersonal_Test"},
			enabled=true)
	@ImplementsTCMS(id="55702")
	public void MultiClientEnsureRHELPersonalBitsAreConsumableAfterRegisteredPersonSubscribesToRHELPersonal_Test() {
				
		log.info("Now client2 (already registered as a system under username '"+clientusername+"') can now consume "+rhelPersonalBitsProductName+"...");
		SubscriptionPool rhelPersonalBitsPool = client2tasks.findSubscriptionPoolWithMatchingFieldFromList("subscriptionName",rhelPersonalBitsProductName,client2tasks.getCurrentlyAvailableSubscriptionPools());
		client2tasks.subscribeToSubscriptionPoolUsingPoolId(rhelPersonalBitsPool);
		
		
		log.info("Now client2 should be consuming the Product Subscription "+rhelPersonalBitsProductName+"...");
		ProductSubscription rhelPersonalBitsProduct = client2tasks.findProductSubscriptionWithMatchingFieldFromList("productName",rhelPersonalBitsProductName,client2tasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertTrue(rhelPersonalBitsProduct!=null,rhelPersonalBitsProductName+" is now consumed on client2 system '"+client2.getConnection().getHostname()+"' registered under user '"+clientusername+"'.");
		
	}
	@Test(	description="subscription-manager-cli: Ensure that availability of RHEL Personal Bits is revoked once the person unsubscribes from RHEL Personal",
			groups={"myDevGroup","MultiClientEnsureAvailabilityOfRHELPersonalBitsIsRevokedOncePersonUnsubscribesFromRHELPersonal_Test","MultiClientRHELPersonal"},
			dependsOnGroups={"MultiClientEnsureRHELPersonalBitsAreConsumableAfterRegisteredPersonSubscribesToRHELPersonal_Test"},
			enabled=true)
	//@ImplementsTCMS(id="55702")
	public void MultiClientEnsureAvailabilityOfRHELPersonalBitsIsRevokedOncePersonUnsubscribesFromRHELPersonal_Test() {
		
		log.info("Unsubscribe client2 (already registered as a system under username '"+clientusername+"') from all currently consumed product subscriptions...");
		client2tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		
		
		log.info("Unsubscribe client1 (already registered as a person under username '"+clientusername+"') from product subscription "+rhelPersonalProductName+"...");
		client1tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();

		
		log.info("Now verify that client2 (already registered as a system under username '"+clientusername+"') can no longer subscribe to the '"+rhelPersonalBitsProductName+"' pool...");
		SubscriptionPool rhelPersonalBitsPool = client2tasks.findSubscriptionPoolWithMatchingFieldFromList("subscriptionName",rhelPersonalBitsProductName,client2tasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertTrue(rhelPersonalBitsPool==null,rhelPersonalBitsProductName+" is no longer available on client2 system '"+client2.getConnection().getHostname()+"' registered under user '"+clientusername+"'.");
	}
	@Test(	description="subscription-manager-cli: Ensure that the entitlement cert for RHEL Personal Bits is revoked once the person unsubscribes from RHEL Personal",
			groups={"myDevGroup","MultiClientEnsureEntitlementCertForRHELPersonalBitsIsRevokedOncePersonUnsubscribesFromRHELPersonal_Test","MultiClientRHELPersonal"},
			dependsOnGroups={"MultiClientEnsureAvailabilityOfRHELPersonalBitsIsRevokedOncePersonUnsubscribesFromRHELPersonal_Test"},
			enabled=true)
	//@ImplementsTCMS(id="55702")
	public void MultiClientEnsureEntitlementCertForRHELPersonalBitsIsRevokedOncePersonUnsubscribesFromRHELPersonal_Test() {
		// setup... make sure the clients are not subscribed to anything
		client2tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		client1tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		
		log.info("Subscribe client1 (already registered as a person under username '"+clientusername+"') to product subscription "+rhelPersonalProductName+"...");
		SubscriptionPool rhelPersonalPool = client1tasks.findSubscriptionPoolWithMatchingFieldFromList("subscriptionName",rhelPersonalProductName,client1tasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertTrue(rhelPersonalPool!=null,rhelPersonalProductName+" is available to user '"+clientusername+"' registered as a person.");
		client1tasks.subscribe(rhelPersonalPool.poolId, null, null, null, null);

		
		log.info("Subscribe client2 (already registered as a system under username '"+clientusername+"') to product subscription "+rhelPersonalBitsProductName+"...");
		SubscriptionPool rhelPersonalBitsPool = client2tasks.findSubscriptionPoolWithMatchingFieldFromList("subscriptionName",rhelPersonalBitsProductName,client2tasks.getCurrentlyAvailableSubscriptionPools());
		client2tasks.subscribeToSubscriptionPoolUsingPoolId(rhelPersonalBitsPool);
		ProductSubscription rhelPersonalBitsProduct = client2tasks.findProductSubscriptionWithMatchingFieldFromList("productName",rhelPersonalBitsProductName,client2tasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertTrue(rhelPersonalBitsProduct!=null,rhelPersonalBitsProductName+" is now consumed on client2 system '"+client2.getConnection().getHostname()+"' registered under user '"+clientusername+"'.");

		
		log.info("Now, unsubscribe the person on client 1 from the "+rhelPersonalProductName+" pool and update the rhsmcertd frequency to 1 minute on client2.  Then assert that the "+rhelPersonalBitsProductName+" gets revoked from client2.");
		client1tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		int certFrequencyMinutes = 1;
		client2tasks.changeCertFrequency(certFrequencyMinutes);
		sleep(certFrequencyMinutes*60*1000 + 500);	// give the rhsmcertd a chance check in with the candlepin server and update the certs
		rhelPersonalBitsProduct = client2tasks.findProductSubscriptionWithMatchingFieldFromList("productName",rhelPersonalBitsProductName,client2tasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertTrue(rhelPersonalBitsProduct==null,rhelPersonalBitsProductName+" was revoked on client2 system '"+client2.getConnection().getHostname()+"' registered under user '"+clientusername+"' after the certFrquency of '"+certFrequencyMinutes+"' minutes since this same person has unsubscribed from the "+rhelPersonalProductName+" on client1");
	}
	@Test(	description="subscription-manager-cli: Ensure that the entitlement cert for RHEL Personal Bits is revoked once the person unregisters",
			groups={"myDevGroup","MultiClientEnsureEntitlementCertForRHELPersonalBitsIsRevokedOncePersonUnregisters_Test","MultiClientRHELPersonal", "blockedByBug-624063"},
			dependsOnGroups={"MultiClientEnsureAvailabilityOfRHELPersonalBitsIsRevokedOncePersonUnsubscribesFromRHELPersonal_Test"},
			enabled=true)
	//@ImplementsTCMS(id="55702")
	public void MultiClientEnsureEntitlementCertForRHELPersonalBitsIsRevokedOncePersonUnregisters_Test() {
		// setup... make sure the clients are not subscribed to anything
		client2tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		client1tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		
		log.info("Subscribe client1 (already registered as a person under username '"+clientusername+"') to product subscription "+rhelPersonalProductName+"...");
		SubscriptionPool rhelPersonalPool = client1tasks.findSubscriptionPoolWithMatchingFieldFromList("subscriptionName",rhelPersonalProductName,client1tasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertTrue(rhelPersonalPool!=null,rhelPersonalProductName+" is available to user '"+clientusername+"' registered as a person.");
		client1tasks.subscribe(rhelPersonalPool.poolId, null, null, null, null);

		
		log.info("Subscribe client2 (already registered as a system under username '"+clientusername+"') to product subscription "+rhelPersonalBitsProductName+"...");
		SubscriptionPool rhelPersonalBitsPool = client2tasks.findSubscriptionPoolWithMatchingFieldFromList("subscriptionName",rhelPersonalBitsProductName,client2tasks.getCurrentlyAvailableSubscriptionPools());
		client2tasks.subscribeToSubscriptionPoolUsingPoolId(rhelPersonalBitsPool);
		ProductSubscription rhelPersonalBitsProduct = client2tasks.findProductSubscriptionWithMatchingFieldFromList("productName",rhelPersonalBitsProductName,client2tasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertTrue(rhelPersonalBitsProduct!=null,rhelPersonalBitsProductName+" is now consumed on client2 system '"+client2.getConnection().getHostname()+"' registered under user '"+clientusername+"'.");

		
		log.info("Now, unregister the person on client 1 from the "+rhelPersonalProductName+" pool and update the rhsmcertd frequency to 1 minute on client2.  Then assert that the "+rhelPersonalBitsProductName+" gets revoked from client2.");
		client1tasks.unregister();
		int certFrequencyMinutes = 1;
		client2tasks.changeCertFrequency(certFrequencyMinutes);
		sleep(certFrequencyMinutes*60*1000 + 500);	// give the rhsmcertd a chance check in with the candlepin server and update the certs
		rhelPersonalBitsProduct = client2tasks.findProductSubscriptionWithMatchingFieldFromList("productName",rhelPersonalBitsProductName,client2tasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertTrue(rhelPersonalBitsProduct==null,rhelPersonalBitsProductName+" was revoked on client2 system '"+client2.getConnection().getHostname()+"' registered under user '"+clientusername+"' after the certFrquency of '"+certFrequencyMinutes+"' minutes since this same person has unregistered from the "+rhelPersonalProductName+" on client1");
	}
	@AfterGroups(groups={"myDevGroup"}, value={"MultiClientRHELPersonal"}, alwaysRun=true)
	public void teardownAfterMultiClientEnsureRHELPersonalBitsAreAvailableAfterRegisteredPersonSubscribesToRHELPersonal_Test() {
		if (client2tasks!=null) client2tasks.unregister_();
		if (client1tasks!=null) client1tasks.unregister_();
	}
	
	
	
	@Test(	description="subscription-manager-cli: change subscription pool start/end dates and refresh subscription pools",
			groups={"ChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test"},
			dependsOnGroups={},
			dataProvider="getAllAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsTCMS(id="56025")
	public void ChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test(SubscriptionPool pool) {
//		https://tcms.engineering.redhat.com/case/56025/?from_plan=2634
//		Actions:
//
//		    * In the db list the subscription pools, find a unique pool to work with
//		    * On a sm client register, subscribe to the pool
//		    * In the db changed the start/end dates for the subscription pool (cp_subscription)
//		    * using the server api, refresh the subscription pools
//		    * on the client check the entitlement certificates ls /etc/pki/entitlement/product
//		    * use openssl x509 to inspect the certs, notice the start / end dates 
//		    * on the client restart the rhsmcertd service
//		    * on the client check the entitlement certificates ls /etc/pki/entitlement/product
//		    * use openssl x509 to inspect the certs, notice the start / end dates
//		    * check the crl list on the server and verify the original entitlement cert serials are present 
//
//		Expected Results:
//
//			* the original entitlement certificates on the client should be removed
//		   	* new certs should be dropped to the client
//			* the crl list on the server should be poplulated w/ the old entitlement cert serials

		if (dbConnection==null) throw new SkipException("This testcase requires a connection to the candlepin database.");
		
		log.info("Subscribe client (already registered as a system under username '"+clientusername+"') to subscription pool "+pool+"...");
		clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);

		log.info("Verify that the currently consumed product subscriptions that came from this subscription pool have the same start and end date as the pool...");
		List<ProductSubscription> products = new ArrayList<ProductSubscription>();
		for (ProductSubscription product : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			if (clienttasks.getSubscriptionPoolFromProductSubscription(product).equals(pool)) {
//FIXME Available Subscriptions	does not display start date			Assert.assertEquals(product.startDate, pool.startDate, "The original start date ("+product.startDate+") for the subscribed product '"+product.productName+"' matches the start date ("+pool.startDate+") of the subscription pool '"+pool.subscriptionName+"' from where it was entitled.");
				Assert.assertTrue(product.endDate.equals(pool.endDate), "The original end date ("+ProductSubscription.formatDateString(product.endDate)+") for the subscribed product '"+product.productName+"' matches the end date ("+SubscriptionPool.formatDateString(pool.endDate)+") of the subscription pool '"+pool.subscriptionName+"' from where it was entitled.");
				products.add(product);
			}
		}
		Calendar originalStartDate = products.get(0).startDate;
		Calendar originalEndDate = products.get(0).endDate;
		String originalCertFile = "/etc/pki/entitlement/product/"+products.get(0).serialNumber+".pem";
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, originalCertFile),1,"Original certificate file '"+originalCertFile+"' exists.");
		
		log.info("Now we will change the start and end date of the subscription pool adding one month to enddate and subtracting one month from startdate...");
		Calendar newStartDate = originalStartDate; newStartDate.add(Calendar.MONTH, -1);
		Calendar newEndDate = originalEndDate; newEndDate.add(Calendar.MONTH, 1);
		updateSubscriptionPoolDatesOnDatabase(pool,newStartDate,newEndDate);
		
		log.info("Now let's refresh the subscription pools...");
		servertasks.refreshSubscriptionPools(serverHostname,serverPort,clientOwnerUsername,clientOwnerPassword);
		
		log.info("Now let's update the certFrequency to 1 minutes so that the rhcertd will pull down the new certFiles");
		clienttasks.changeCertFrequency(1);
		sleep(1*60*1000 + 500);	// give the rhsmcertd a chance check in with the candlepin server and update the certs

		log.info("The updated certs should now be on the client...");

		log.info("First, let's assert that subscription pool reflects the new end date...");
		List<SubscriptionPool> allSubscriptionPools = client1tasks.getCurrentlyAllAvailableSubscriptionPools();
		Assert.assertContains(allSubscriptionPools, pool);
		for (SubscriptionPool newPool : allSubscriptionPools) {
			if (newPool.equals(pool)) {
				Assert.assertEquals(SubscriptionPool.formatDateString(newPool.endDate), SubscriptionPool.formatDateString(newEndDate),
						"As seen by the client, the enddate of the subscribed to pool '"+pool.poolId+"' has been changed from '"+SubscriptionPool.formatDateString(originalEndDate)+"' to '"+SubscriptionPool.formatDateString(newEndDate)+"'.");
				break;
			}
		}

		log.info("Second, let's assert that the original cert file '"+originalCertFile+"' is gone...");
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, originalCertFile),0,"Original certificate file '"+originalCertFile+"' has been removed.");

		log.info("Third, let's assert that consumed product certs have been updated...");
		String newCertFile = "";
		for (ProductSubscription product : products) {
			ProductSubscription newProduct = client1tasks.findProductSubscriptionWithMatchingFieldFromList("productName",product.productName,clienttasks.getCurrentlyConsumedProductSubscriptions());
			Assert.assertEquals(ProductSubscription.formatDateString(newProduct.startDate), ProductSubscription.formatDateString(newStartDate),
					"Rhsmcertd has updated the entitled startdate to '"+ProductSubscription.formatDateString(newStartDate)+"' for consumed product: "+newProduct.productName);
			Assert.assertEquals(ProductSubscription.formatDateString(newProduct.endDate), ProductSubscription.formatDateString(newEndDate),
					"Rhsmcertd has updated the entitled enddate to '"+ProductSubscription.formatDateString(newEndDate)+"' for consumed product: "+newProduct.productName);

			log.info("And, let's assert that consumed product cert serial has been updated...");
			Assert.assertTrue(!newProduct.serialNumber.equals(product.serialNumber), 
					"The consumed product cert serial has been updated from '"+product.serialNumber+"' to '"+newProduct.serialNumber+"' for product: "+newProduct.productName);
			newCertFile = "/etc/pki/entitlement/product/"+newProduct.serialNumber+".pem";
		}
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, newCertFile),1,"New certificate file '"+newCertFile+"' exists.");

		// TODO check the crl list on the server and verify the original entitlement cert serials are present
		log.info("//TODO check the crl list on the server and verify the original entitlement cert serials are present");
	}
	
	
	
	// Protected Methods ***********************************************************************

	
	/**
	 * On the connected candlepin server database, update the startdate and enddate in the cp_subscription table on rows where the pool id is a match.
	 * @param pool
	 * @param startDate
	 * @param endDate
	 */
	public void updateSubscriptionPoolDatesOnDatabase(SubscriptionPool pool, Calendar startDate, Calendar endDate) {
		//DateFormat dateFormat = new SimpleDateFormat(CandlepinAbstraction.dateFormat);
		String updateSubscriptionPoolEndDateSql = "";
		String updateSubscriptionPoolStartDateSql = "";
		if (endDate!=null) {
			updateSubscriptionPoolEndDateSql = "update cp_subscription set enddate='"+CandlepinAbstraction.formatDateString(endDate)+"' where id=(select pool.subscriptionid from cp_pool pool where pool.id='"+pool.poolId+"');";
		}
		if (startDate!=null) {
			updateSubscriptionPoolStartDateSql = "update cp_subscription set startdate='"+CandlepinAbstraction.formatDateString(startDate)+"' where id=(select pool.subscriptionid from cp_pool pool where pool.id='"+pool.poolId+"');";
		}
		
		try {
			Statement s = dbConnection.createStatement();
			if (endDate!=null) {
				Assert.assertEquals(s.executeUpdate(updateSubscriptionPoolEndDateSql), 1, "Updated one row of the cp_subscription table with sql: "+updateSubscriptionPoolEndDateSql);
			}
			if (startDate!=null) {
				Assert.assertEquals(s.executeUpdate(updateSubscriptionPoolStartDateSql), 1, "Updated one row of the cp_subscription table with sql: "+updateSubscriptionPoolStartDateSql);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected ArrayList<String> getYumRepolist(){
		ArrayList<String> repos = new ArrayList<String>();
		client.runCommandAndWait("killall -9 yum");
		
		client.runCommandAndWait("yum repolist");
		String[] availRepos = client.getStdout().split("\\n");
		
		int repolistStartLn = 0;
		int repolistEndLn = 0;
		
		for(int i=0;i<availRepos.length;i++)
			if (availRepos[i].contains("repo id"))
				repolistStartLn = i + 1;
			else if (availRepos[i].contains("repolist:"))
				repolistEndLn = i;
		
		for(int i=repolistStartLn;i<repolistEndLn;i++)
			repos.add(availRepos[i].split(" ")[0]);
		
		return repos;
	}
	
//	protected void adjustRHSMYumRepo(boolean enabled){
//		Assert.assertEquals(
//				RemoteFileTasks.searchReplaceFile(client, 
//						rhsmYumRepoFile, 
//						"^enabled=.*$", 
//						"enabled="+(enabled?'1':'0')),
//						0,
//						"Adjusted RHSM Yum Repo config file, enabled="+(enabled?'1':'0')
//				);
//	}
	
	
	// Data Providers ***********************************************************************

	
	@DataProvider(name="getValidConsumerData")
	public Object[][] getValidConsumerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getValidConsumerDataAsListOfLists());
	}
	protected List<List<Object>> getValidConsumerDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		for (List<Object> registrationDataList : getRegistrationDataAsListOfLists()) {
			// pull out all of the valid registration data (indicated by an Integer exitCode of 0)
			if (registrationDataList.contains(Integer.valueOf(0))) {
				// String username, String password, String type, String consumerId
				ll.add(registrationDataList.subList(0, 4));
			}
			
		}
		
		return ll;
	}
}
