package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.ContentNamespace;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.SubscriptionPool;

/**
 * @author jsefler
 *
 */
@Test(groups={"content"})
public class ContentTests extends SubscriptionManagerCLITestScript{
	
	
	// Test methods ***********************************************************************

	@Test(	description="subscription-manager Yum plugin: enable/disable",
			groups={"EnableDisableYumRepoAndVerifyContentAvailable_Test"},
			dataProvider="getAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41696,fromPlan=2479)
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
					"Yum repolist enabled includes enabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
			} else {
				Assert.assertFalse(repolist.contains(contentNamespace.label),
					"Yum repolist enabled excludes disabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
			}
		}
		repolist = clienttasks.getYumRepolist("disabled");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (contentNamespace.enabled.equals("1")) {
				Assert.assertFalse(repolist.contains(contentNamespace.label),
					"Yum repolist disabled excludes enabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
			} else {
				Assert.assertTrue(repolist.contains(contentNamespace.label),
					"Yum repolist disabled includes disabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
			}
		}
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertTrue(repolist.contains(contentNamespace.label),
				"Yum repolist all includes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
		}

		log.info("Unsubscribe from the pool and verify that yum repolist no longer reports the expected repo id/labels...");
		clienttasks.unsubscribeFromSerialNumber(entitlementCert.serialNumber);
		
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertFalse(repolist.contains(contentNamespace.label),
				"Yum repolist all excludes repo id/label '"+contentNamespace.label+"' after having unsubscribed from Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
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
				"Yum repolist all excludes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' disabled.");
		}
		
		log.info("Now we will restart the rhsmcertd and expect the repo list to be updated");
		int minutes = 2;
		clienttasks.restart_rhsmcertd(minutes, false);
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertTrue(repolist.contains(contentNamespace.label),
				"Yum repolist all now includes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' disabled and run an update with rhsmcertd.");
		}
		
		log.info("Now we will unsubscribe from the pool and verify that yum repolist continues to report the repo id/labels until the next refresh from the rhsmcertd runs...");
		clienttasks.unsubscribeFromSerialNumber(entitlementCert.serialNumber);
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertTrue(repolist.contains(contentNamespace.label),
				"Yum repolist all still includes repo id/label '"+contentNamespace.label+"' despite having unsubscribed from Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' disabled.");
		}
		log.info("Wait for the next refresh by rhsmcertd to remove the repos from the yum repo file '"+clienttasks.redhatRepoFile+"'...");
		sleep(minutes*60*1000);
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertFalse(repolist.contains(contentNamespace.label),
				"Yum repolist all finally excludes repo id/label '"+contentNamespace.label+"' after having unsubscribed from Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' disabled AND waiting for the next refresh by rhsmcertd.");
		}
	}
	@AfterGroups(value="EnableDisableYumRepoAndVerifyContentAvailable_Test", alwaysRun=true)
	protected void teardownAfterEnableDisableYumRepoAndVerifyContentAvailable_Test() {
		clienttasks.updateConfFileParameter(clienttasks.rhsmPluginConfFile, "enabled", "1");
		clienttasks.restart_rhsmcertd(Integer.valueOf(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "certFrequency")), false);
	}
	

	
	@Test(	description="subscription-manager content flag : Default content flag should enable",
			groups={},
	        enabled=true)
	@ImplementsNitrateTest(caseId=47578,fromPlan=2479)
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
		
		clienttasks.unregister();
	    clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
	    clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools(ConsumerType.system);
	    List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
	    Assert.assertTrue(!entitlementCerts.isEmpty(),"After subscribing to all available subscription pools, there must be some entitlements."); // or maybe we should skip when nothing is consumed 
		ArrayList<String> repolist = clienttasks.getYumRepolist("enabled");
		for (EntitlementCert entitlementCert : entitlementCerts) {
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				if (contentNamespace.enabled.equals("1")) {
					Assert.assertTrue(repolist.contains(contentNamespace.label),
						"Yum repolist enabled includes enabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"'.");
				} else {
					Assert.assertFalse(repolist.contains(contentNamespace.label),
						"Yum repolist enabled excludes disabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"'.");
				}
			}
		}
	}
	
	
	@Test(	description="subscription-manager Yum plugin: ensure content can be downloaded/installed/removed",
			groups={},
			dataProvider="getAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41695,fromPlan=2479)
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
		if (!pkgInstalled && isServerOnPremises) throw new SkipException("Because we are currently testing against an OnPremises candlepin server ("+serverHostname+") that has imported data from '"+serverImportDir+"', we don't actually expect that an entitlement from this subscription pool ("+pool.subscriptionName+") to provide real content from "+rhsmBaseUrl);
		Assert.assertTrue(pkgInstalled,"At least one package was found and installed from entitled repos after subscribing to SubscriptionPool: "+pool);
	}
	
	
	
	// Configuration Methods ***********************************************************************
	
	
	
	// Protected Methods ***********************************************************************
	

	
	// Data Providers ***********************************************************************


}
