package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.SubscriptionManagerTasks;
import com.redhat.qe.sm.data.ContentNamespace;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

/**
 * @author ssalevan
 * @author jsefler
 *
 */
@Test(groups={"subscribe"})
public class SubscribeTests extends SubscriptionManagerCLITestScript{
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using product ID",
			enabled=false,	// Subscribing to a Subscription Pool using --product Id has been removed in subscription-manager-0.71-1.el6.i686.
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4", "blockedByBug-584137"},
			groups={"blockedByBug-584137"},
			dataProvider="getAvailableSubscriptionPoolsData")
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
		SubscriptionPool MCT0696 = new SubscriptionPool("MCT0696", "696");
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
			enabled=true,
			dataProvider="getAvailableSubscriptionPoolsData")
	@ImplementsTCMS(id="41686")
	public void SubscribeToValidSubscriptionsByPoolID_Test(SubscriptionPool pool){
// non-dataProvided test procedure
//		sm.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to each available subscription pool using pool ID",
			groups={"blockedByBug-584137"},
			dataProvider="getGoodRegistrationData")
	@ImplementsTCMS(id="41686")
	public void SubscribeConsumerToEachAvailableSubscriptionPoolUsingPoolId_Test(String username, String password){
		clienttasks.unregister();
		clienttasks.register(username, password, ConsumerType.system, null, null, Boolean.FALSE, Boolean.FALSE);
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
			dataProvider="getAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsTCMS(id="41897")
	public void SubscribeAndSubscribeAgain_Test(SubscriptionPool pool){
// non-dataProvided test procedure
//		//sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
//		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
//		for(SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
//			clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
//			clienttasks.subscribeToProduct(pool.subscriptionName);
//		}
		clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
		SSHCommandResult result = clienttasks.subscribe_(pool.poolId,null,null,null,null);
		Assert.assertEquals(result.getStdout().trim(), "This consumer is already subscribed to the product matching pool with id '"+pool.poolId+"'",
				"subscribe command returns proper message when already subscribed to the requested pool");
	}
	


	
	
// FIXME: THIS ORIGINAL ssalevan TEST WAS NOT COMPLETE.  REPLACEMENT IS BELOW
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
			groups={"EnableDisableYumRepoAndVerifyContentAvailable_Test"},
			dataProvider="getAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsTCMS(id="41696")
	public void EnableDisableYumRepoAndVerifyContentAvailable_Test(SubscriptionPool pool) {

		log.info("Before beginning this test, we will stop the rhsmcertd so that it does not interfere with this test..");
		clienttasks.stop_rhsmcertd();
		
		// Edit /etc/yum/pluginconf.d/rhsmplugin.conf and ensure that the enabled directive is set to 1
		log.info("Making sure that the rhsm plugin conf file '"+clienttasks.rhsmPluginConfFile+"' is enabled with enabled=1..");
		clienttasks.updateConfFileParameter(clienttasks.rhsmPluginConfFile, "enabled", "1");
		
		log.info("Subscribe to the pool and start testing that yum repolist reports the expected repo id/labels...");
		clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
		
		// 1. Run a 'yum repolist' and get a list of all of the available repositories corresponding to your entitled products
		// 1. Repolist contains repositories corresponding to your entitled products
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.getCurrentEntitlementCertFiles("-t").get(0)); // newest entitlement cert
		ArrayList<String> repolist = clienttasks.getYumRepolist("enabled");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (contentNamespace.enabled.equals("1")) {
				Assert.assertTrue(repolist.contains(contentNamespace.label),
					"Yum repolist enabled includes enabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
			} else {
				Assert.assertFalse(repolist.contains(contentNamespace.label),
					"Yum repolist enabled excludes disabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
			}
		}
		repolist = clienttasks.getYumRepolist("disabled");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (contentNamespace.enabled.equals("1")) {
				Assert.assertFalse(repolist.contains(contentNamespace.label),
					"Yum repolist disabled excludes enabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
			} else {
				Assert.assertTrue(repolist.contains(contentNamespace.label),
					"Yum repolist disabled includes disabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
			}
		}
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertTrue(repolist.contains(contentNamespace.label),
				"Yum repolist all includes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
		}

		log.info("Unsubscribe from the pool and verify that yum repolist no longer reports the expected repo id/labels...");
		clienttasks.unsubscribeFromSerialNumber(entitlementCert.serialNumber);
		
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertFalse(repolist.contains(contentNamespace.label),
				"Yum repolist all excludes repo id/label '"+contentNamespace.label+"' after having unsubscribed from Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
		}
	
		// Edit /etc/yum/pluginconf.d/rhsmplugin.conf and ensure that the enabled directive is set to 0
		log.info("Now we will disable the rhsm plugin conf file '"+clienttasks.rhsmPluginConfFile+"' with enabled=0..");
		clienttasks.updateConfFileParameter(clienttasks.rhsmPluginConfFile, "enabled", "0");
		
		log.info("Again let's subscribe to the same pool and verify that yum repolist does NOT report any of the entitled repo id/labels since the plugin has been disabled...");
		clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
		
		// 2. Run a 'yum repolist' and get a list of all of the available repositories corresponding to your entitled products
		// 2. Repolist does not contain repositories corresponding to your entitled products
		entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.getCurrentEntitlementCertFiles("-t").get(0));
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertFalse(repolist.contains(contentNamespace.label),
				"Yum repolist all excludes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' disabled.");
		}
		
		log.info("Now we will restart the rhsmcertd and expect the repo list to be updated");
		int minutes = 2;
		clienttasks.restart_rhsmcertd(minutes, false);
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertTrue(repolist.contains(contentNamespace.label),
				"Yum repolist all now includes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' disabled and run an update with rhsmcertd.");
		}
		
		log.info("Now we will unsubscribe from the pool and verify that yum repolist continues to report the repo id/labels until the next refresh from the rhsmcertd runs...");
		clienttasks.unsubscribeFromSerialNumber(entitlementCert.serialNumber);
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertTrue(repolist.contains(contentNamespace.label),
				"Yum repolist all still includes repo id/label '"+contentNamespace.label+"' despite having unsubscribed from Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' disabled.");
		}
		log.info("Wait for the next refresh by rhsmcertd to remove the repos from the yum repo file '"+clienttasks.redhatRepoFile+"'...");
		sleep(minutes*60*1000);
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertFalse(repolist.contains(contentNamespace.label),
				"Yum repolist all finally excludes repo id/label '"+contentNamespace.label+"' after having unsubscribed from Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' disabled AND waiting for the next refresh by rhsmcertd.");
		}
	}
	@AfterGroups(value="EnableDisableYumRepoAndVerifyContentAvailable_Test", alwaysRun=true)
	protected void teardownAfterEnableDisableYumRepoAndVerifyContentAvailable_Test() {
		clienttasks.updateConfFileParameter(clienttasks.rhsmPluginConfFile, "enabled", "1");
		clienttasks.restart_rhsmcertd(Integer.valueOf(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "certFrequency")), false);
	}
	

	
	@Test(	description="subscription-manager content flag : Default content flag should enable",
//	        dependsOnGroups={"sm_stage6"},
//	        groups={"sm_stage7"},
			groups={},
	        enabled=true)
	@ImplementsTCMS(id="47578")
	public void VerifyYumRepoListsEnabledContent(){
// Original code from ssalevan
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
		
// DELETEME: Alternative to above procedure is:
//		clienttasks.unregister();
//	    clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
//	    clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools(ConsumerType.system);
//	    List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
//	    Assert.assertTrue(!entitlementCerts.isEmpty(),"After subscribing to all available subscription pools, there must be some entitlements."); // or maybe we should skip when nothing is consumed 
//	    clienttasks.assertEntitlementCertsInYumRepolist(entitlementCerts,true);
	    
		clienttasks.unregister();
	    clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
	    clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools(ConsumerType.system);
	    List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
	    Assert.assertTrue(!entitlementCerts.isEmpty(),"After subscribing to all available subscription pools, there must be some entitlements."); // or maybe we should skip when nothing is consumed 
		ArrayList<String> repolist = clienttasks.getYumRepolist("enabled");
		for (EntitlementCert entitlementCert : entitlementCerts) {
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				if (contentNamespace.enabled.equals("1")) {
					Assert.assertTrue(repolist.contains(contentNamespace.label),
						"Yum repolist enabled includes enabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"'.");
				} else {
					Assert.assertFalse(repolist.contains(contentNamespace.label),
						"Yum repolist enabled excludes disabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"'.");
				}
			}
		}
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to multiple/duplicate/bad pools in one call",
			groups={"blockedByBug-622851"},
			enabled=true)
	public void SubscribeToMultipleDuplicateAndBadPools_Test() {
		
		// begin the test with a cleanly registered system
		clienttasks.unregister();
	    clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
	    
		// assemble a list of all the available SubscriptionPool ids with duplicates and bad ids
		List <String> poolIds = new ArrayList<String>();
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			poolIds.add(pool.poolId);
			poolIds.add(pool.poolId); // add a duplicate poolid
		}
		String badPoolId1 = "bad123", badPoolId2 = "bad_POOLID"; 
		poolIds.add(0, badPoolId1); // insert a bad poolid
		poolIds.add(badPoolId2); // append a bad poolid
		
		// subscribe to all pool ids
		log.info("Attempting to subscribe to multiple pools with duplicate and bad pool ids...");
		SSHCommandResult result = clienttasks.subscribe(poolIds, null, null, null, null);
		
		// assert the results
		for (String poolId : poolIds) {
			if (poolId.equals(badPoolId1)) continue; if (poolId.equals(badPoolId2)) continue;
			Assert.assertContainsMatch(result.getStdout(),"^This consumer is already subscribed to the product matching pool with id '"+poolId+"'$","Asserting that already subscribed to pools is noted and skipped during a multiple pool binding.");
		}
		Assert.assertContainsMatch(result.getStdout(),"^No such entitlement pool: "+badPoolId1+"$","Asserting that an invalid pool is noted and skipped during a multiple pool binding.");
		Assert.assertContainsMatch(result.getStdout(),"^No such entitlement pool: "+badPoolId2+"$","Asserting that an invalid pool is noted and skipped during a multiple pool binding.");
		clienttasks.assertNoAvailableSubscriptionPoolsToList("Asserting that no available subscription pools remain after simultaneously subscribing to them all including duplicates and bad pool ids.");
	}
	
	
	@Test(	description="subscription-manager Yum plugin: ensure content can be downloaded/installed/removed",
//			dependsOnGroups={"sm_stage6"},
//			groups={"sm_stage7", "not_implemented"},
			groups={},
			dataProvider="getAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsTCMS(id="41695")
	public void InstallAndRemovePackageAfterSubscribingToPool_Test(SubscriptionPool pool) {
		// original implementation by ssalevan
//		HashMap<String, String[]> pkgList = clienttasks.getPackagesCorrespondingToSubscribedRepos();
//		for(ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()){
//			String pkg = pkgList.get(productSubscription.productName)[0];
//			log.info("Attempting to install first pkg '"+pkg+"' from product subscription: "+productSubscription);
//			log.info("timeout of two minutes for next three commands");
//			RemoteFileTasks.runCommandExpectingNoTracebacks(client,
//					"yum repolist");
//			RemoteFileTasks.runCommandExpectingNoTracebacks(client,
//					"yum install -y "+pkg);
//			RemoteFileTasks.runCommandExpectingNoTracebacks(client,
//					"rpm -q "+pkg);
//		}
	
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		boolean pkgInstalled = false;
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (contentNamespace.enabled.equals("1")) {
				String repoLabel = contentNamespace.label;

				// find an available package that is uniquely provided by repo
				String pkg = clienttasks.findUniqueAvailablePackageFromRepo(repoLabel);
				if (pkg==null) {
					log.warning("Could NOT find a unique available package from repo '"+repoLabel+"' after subscribing to SubscriptionPool: "+pool);
					continue;
				}
				
				// install the package and assert that it is successfully installed
				clienttasks.installPackageUsingYumFromRepo(pkg, repoLabel); pkgInstalled = true;
				
				// now remove the package
				clienttasks.removePackageUsingYum(pkg);
			}
		}
		Assert.assertTrue(pkgInstalled,"At least one package was found and installed from entitled repos after subscribing to SubscriptionPool: "+pool);
	}
	
	
	@Test(	description="rhsmcertd: change certFrequency",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4"},
			dataProvider="getCertFrequencyData",
			groups={"blockedByBug-617703"},
			enabled=true)
	@ImplementsTCMS(id="41692")
	public void rhsmcertdChangeCertFrequency_Test(int minutes) {
		String errorMsg = "Either the consumer is not registered with candlepin or the certificates are corrupted. Certificate updation using daemon failed.";
		errorMsg = "Either the consumer is not registered or the certificates are corrupted. Certificate update using daemon failed.";
		
		log.info("First test with an unregistered user and verify that the rhsmcertd actually fails since it cannot self-identify itself to the candlepin server.");
		clienttasks.unregister();
		clienttasks.restart_rhsmcertd(minutes, false); sleep(10000); // allow 10sec for the initial update
		log.info("Appending a marker in the '"+SubscriptionManagerTasks.rhsmcertdLogFile+"' so we can assert that the certificates are being updated every '"+minutes+"' minutes");
		String marker = "Testing rhsm.conf certFrequency="+minutes+" when unregistered..."; // https://tcms.engineering.redhat.com/case/41692/
		RemoteFileTasks.runCommandAndAssert(client,"echo \""+marker+"\" >> "+SubscriptionManagerTasks.rhsmcertdLogFile,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+SubscriptionManagerTasks.rhsmcertdLogFile,Integer.valueOf(0),marker,null);
		sleep(minutes*60*1000);	// give the rhsmcertd a chance to check in with the candlepin server and update the certs
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+SubscriptionManagerTasks.rhsmcertdLogFile,Integer.valueOf(0),"update failed \\(\\d+\\), retry in "+minutes+" minutes",null);
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+SubscriptionManagerTasks.rhsmLogFile,Integer.valueOf(0),errorMsg,null);
		
		
		log.info("Now test with a registered user whose identity is corrupt and verify that the rhsmcertd actually fails since it cannot self-identify itself to the candlepin server.");
		String consumerid = clienttasks.getCurrentConsumerId(clienttasks.register(clientusername, clientpassword, null, null, null, null, null));
		log.info("Corrupting the identity cert by borking its content...");
		RemoteFileTasks.runCommandAndAssert(client, "openssl x509 -noout -text -in "+clienttasks.consumerCertFile+" > /tmp/stdout; mv /tmp/stdout -f "+clienttasks.consumerCertFile, 0);
		clienttasks.restart_rhsmcertd(minutes, false); sleep(10000); // allow 10sec for the initial update
		log.info("Appending a marker in the '"+SubscriptionManagerTasks.rhsmcertdLogFile+"' so we can assert that the certificates are being updated every '"+minutes+"' minutes");
		marker = "Testing rhsm.conf certFrequency="+minutes+" when identity is corrupted...";
		RemoteFileTasks.runCommandAndAssert(client,"echo \""+marker+"\" >> "+SubscriptionManagerTasks.rhsmcertdLogFile,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+SubscriptionManagerTasks.rhsmcertdLogFile,Integer.valueOf(0),marker,null);
		sleep(minutes*60*1000);	// give the rhsmcertd a chance to check in with the candlepin server and update the certs
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+SubscriptionManagerTasks.rhsmcertdLogFile,Integer.valueOf(0),"update failed \\(\\d+\\), retry in "+minutes+" minutes",null);
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+SubscriptionManagerTasks.rhsmLogFile,Integer.valueOf(0),errorMsg,null);

		
		log.info("Finally test with a registered user and verify that the rhsmcertd succeeds because he can identify himself to the candlepin server.");
	    clienttasks.register(clientusername, clientpassword, null, null, consumerid, null, Boolean.TRUE);
		clienttasks.restart_rhsmcertd(minutes, false); sleep(10000); // allow 10sec for the initial update
		log.info("Appending a marker in the '"+SubscriptionManagerTasks.rhsmcertdLogFile+"' so we can assert that the certificates are being updated every '"+minutes+"' minutes");
		marker = "Testing rhsm.conf certFrequency="+minutes+" when registered..."; // https://tcms.engineering.redhat.com/case/41692/
		RemoteFileTasks.runCommandAndAssert(client,"echo \""+marker+"\" >> "+SubscriptionManagerTasks.rhsmcertdLogFile,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+SubscriptionManagerTasks.rhsmcertdLogFile,Integer.valueOf(0),marker,null);
		sleep(minutes*60*1000);	// give the rhsmcertd a chance to check in with the candlepin server and update the certs
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+SubscriptionManagerTasks.rhsmcertdLogFile,Integer.valueOf(0),"certificates updated",null);

		/* tail -f /var/log/rhsm/rhsm.log
		 * 2010-09-10 12:05:06,338 [ERROR] main() @certmgr.py:75 - Either the consumer is not registered with candlepin or the certificates are corrupted. Certificate updation using daemon failed.
		 */
		
		/* tail -f /var/log/rhsm/rhsmcertd.log
		 * Fri Sep 10 11:59:50 2010: started: interval = 1 minutes
		 * Fri Sep 10 11:59:51 2010: update failed (255), retry in 1 minutes
		 * testing rhsm.conf certFrequency=1 when unregistered.
		 * Fri Sep 10 12:00:51 2010: update failed (255), retry in 1 minutes
		 * Fri Sep 10 12:01:04 2010: started: interval = 1 minutes
		 * Fri Sep 10 12:01:05 2010: certificates updated
		 * testing rhsm.conf certFrequency=1 when registered.
		 * Fri Sep 10 12:02:05 2010: certificates updated
		*/
	}
	
	
	@Test(	description="rhsmcertd: ensure certificates synchronize",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4"},
			groups={"blockedByBug-617703"},
			enabled=true)
	@ImplementsTCMS(id="41694")
	public void rhsmcertdEnsureCertificatesSynchronize_Test(){
//FIXME Replacing ssalevan's original implementation of this test... 10/5/2010 jsefler
//		clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools(ConsumerType.system);
//		//SubscribeToASingleEntitlementByProductID_Test();
//		client.runCommandAndWait("rm -rf "+clienttasks.entitlementCertDir+"/*");
//		client.runCommandAndWait("rm -rf "+clienttasks.productCertDir+"/*");
//		//certFrequency_Test(1);
//		clienttasks.restart_rhsmcertd(1,true);
////		client.runCommandAndWait("cat /dev/null > "+rhsmcertdLogFile);
////		//sshCommandRunner.runCommandAndWait("rm -f "+rhsmcertdLogFile);
////		//sshCommandRunner.runCommandAndWait("/etc/init.d/rhsmcertd restart");
////		this.sleep(70*1000);
////		
////		Assert.assertEquals(RemoteFileTasks.grepFile(client,
////				rhsmcertdLogFile,
////				"certificates updated"),
////				0,
////				"rhsmcertd reports that certificates have been updated");
//		
//		//verify that PEM files are present in all certificate directories
//		RemoteFileTasks.runCommandAndAssert(client, "ls "+clienttasks.entitlementCertDir+" | grep pem", 0, "pem", null);
//		RemoteFileTasks.runCommandAndAssert(client, "ls "+clienttasks.entitlementCertDir+"/product | grep pem", 0, "pem", null);
//		// this directory will only be populated if you upload ur own license, not while working w/ candlepin
//		/*RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "ls /etc/pki/product", 0, "pem", null);*/
		
		// start with a cleanly unregistered system
		clienttasks.unregister();
		
		// register a clean user
	    clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
	    
	    // subscribe to all the available pools
	    clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools(ConsumerType.system);
	    
	    // get all of the current entitlement product certs and remember them
	    List<File> entitlementCertFiles = clienttasks.getCurrentEntitlementCertFiles();
	    
	    // delete all of the entitlement cert files
	    client.runCommandAndWait("rm -rf "+clienttasks.entitlementCertDir+"/*");
	    Assert.assertEquals(clienttasks.getCurrentEntitlementCertFiles().size(), 0,
	    		"All the entitlement product certs have been deleted.");
		
	    // restart the rhsmcertd to run every 1 minute and wait for a refresh
		clienttasks.restart_rhsmcertd(1, true);
		
		// assert that rhsmcertd has refreshed the entitled product certs back to the original
	    Assert.assertEquals(clienttasks.getCurrentEntitlementCertFiles(), entitlementCertFiles,
	    		"All the deleted entitlement product certs have been re-synchronized by rhsm cert deamon.");
	}
	
	
	
	// Protected Methods ***********************************************************************
	

	
	// Data Providers ***********************************************************************

	

	
	@DataProvider(name="getCertFrequencyData")
	public Object[][] getCertFrequencyDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getCertFrequencyDataAsListOfLists());
	}
	protected List<List<Object>> getCertFrequencyDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		// int minutes
		ll.add(Arrays.asList(new Object[]{2}));
		ll.add(Arrays.asList(new Object[]{1}));
		
		return ll;
	}
}
