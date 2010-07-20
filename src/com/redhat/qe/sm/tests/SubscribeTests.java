package com.redhat.qe.sm.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.abstractions.EntitlementCert;
import com.redhat.qe.sm.abstractions.SubscriptionPool;
import com.redhat.qe.sm.abstractions.ProductSubscription;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.tools.RemoteFileTasks;

public class SubscribeTests extends SubscriptionManagerTestScript{
	
	@Test(description="subscription-manager-cli: subscribe client to an entitlement using product ID",
			dependsOnGroups={"sm_stage3"},
			groups={"sm_stage4", "blockedByBug-584137"},
			dataProvider="getAllAvailableSubscriptionPoolData")
	@ImplementsTCMS(id="41680")
	public void SubscribeToValidSubscriptionsByProductID_Test(SubscriptionPool pool){
//		sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		sm1.subscribeToSubscriptionPoolUsingProductId(pool);
	}
	
	
	@Test(description="subscription-manager-cli: subscribe client to an entitlement using product ID",
			dependsOnGroups={"sm_stage3"},
			groups={"sm_stage4", "blockedByBug-584137", "not_implemented"})
	public void SubscribeToASingleEntitlementByProductID_Test(){
		sm1.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		SubscriptionPool MCT0696 = new SubscriptionPool("MCT0696", "biteme");
		MCT0696.addProductID("Red Hat Directory Server");
		sm1.subscribeToSubscriptionPoolUsingProductId(MCT0696);
		//this.refreshSubscriptions();
		for (ProductSubscription pid:MCT0696.associatedProductIDs){
			Assert.assertTrue(sm1.getCurrentlyConsumedProductSubscriptions().contains(pid),
					"ProductID '"+pid.productName+"' consumed from Pool '"+MCT0696.subscriptionName+"'");
		}
	}
	
	
	@Test(description="subscription-manager-cli: subscribe client to an entitlement using pool ID",
			dependsOnGroups={"sm_stage3"},
			groups={"sm_stage4", "blockedByBug-584137"},
			dataProvider="getAllAvailableSubscriptionPoolData")
	@ImplementsTCMS(id="41686")
	public void SubscribeToValidSubscriptionsByPoolID_Test(SubscriptionPool pool){
		// non-dataProvided test procedure
		//sm.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		//sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		sm1.subscribeToSubscriptionPoolUsingPoolId(pool);
	}
	
	
	@Test(description="subscription-manager-cli: subscribe client to an entitlement using registration token",
			dependsOnGroups={"sm_stage8"},
			groups={"sm_stage9", "blockedByBug-584137", "not_implemented"})
	@ImplementsTCMS(id="41681")
	public void SubscribeToRegToken_Test(){
		sm1.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		sm1.subscribeToRegToken(regtoken);
	}
	
	
	@Test(description="Subscribed for Already subscribed Entitlement.",
			dependsOnGroups={"sm_stage3"},
			groups={"sm_stage4", "blockedByBug-584137", "not_implemented"})
	@ImplementsTCMS(id="41897")
	public void SubscribeAndSubscribeAgain_Test(){
		//sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		sm1.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		for(SubscriptionPool pool : sm1.getCurrentlyAvailableSubscriptionPools()) {
			sm1.subscribeToSubscriptionPoolUsingProductId(pool);
			sm1.subscribeToProduct(pool.subscriptionName);
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
	@Test(description="subscription-manager Yum plugin: enable/disable",
			dependsOnGroups={"sm_stage5"},
			groups={"sm_stage6", "not_implemented"})
	@ImplementsTCMS(id="41696")
	public void EnableYumRepoAndVerifyContentAvailable_Test() {

		sm1.register(client1username, client1password, null, null, null, null);	// assure we are registered
		sm1.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		
		// Edit /etc/yum/pluginconf.d/rhsmplugin.conf and ensure that the enabled directive is set to 1
		this.adjustRHSMYumRepo(true);

		// 1. Run a 'yum repolist' and get a list of all of the available repositories corresponding to your entitled products
		// 1. Repolist contains repositories corresponding to your entitled products
//		for(ProductSubscription sub:sm.getCurrentlyConsumedProductSubscriptions()){
//			ArrayList<String> repos = this.getYumRepolist();
//			Assert.assertTrue(repos.contains(sub.productId),
//					"Yum reports product subscribed to repo: " + sub.productId);
//		}
		
		// Edit /etc/yum/pluginconf.d/rhsmplugin.conf and ensure that the enabled directive is set to 0
		this.adjustRHSMYumRepo(false);
		
		// 2. Run a 'yum repolist' and get a list of all of the available repositories corresponding to your entitled products
		// 2. Repolist does not contain repositories corresponding to your entitled products
throw new SkipException("THIS TESTCASE IS UNDER CONSTRUCTION. IMPLEMENTATION OF https://tcms.engineering.redhat.com/case/41696/?search=41696");		

	}
	
	
	@Test(description="subscription-manager Yum plugin: ensure ...",
	        dependsOnGroups={"sm_stage6"},
	        groups={"sm_stage7"})
	@ImplementsTCMS(id="47578")
	public void VerifyReposAvailableForEnabledContent(){
	    ArrayList<String> repos = this.getYumRepolist();
	    
	    for (EntitlementCert cert:sm1.getCurrentEntitlementCerts()){
	    	if(cert.enabled.contains("1"))
	    		Assert.assertTrue(repos.contains(cert.label),
	    				"Yum reports enabled content subscribed to repo: " + cert.label);
	    	else
	    		Assert.assertFalse(repos.contains(cert.label),
	    				"Yum reports enabled content subscribed to repo: " + cert.label);
	    }
	    // FIXME: Untested Alternative to above procedure is:
//	    sm.register(username, password, null, null, null, null);
//	    sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
//	    sm.assertEntitlementCertsAreReportedInYumRepolist(sm.getCurrentEntitlementCerts());
	}
	
	
	@Test(description="subscription-manager Yum plugin: ensure content can be downloaded/installed",
			dependsOnGroups={"sm_stage6"},
			groups={"sm_stage7", "not_implemented"})
	@ImplementsTCMS(id="41695")
	public void InstallPackageFromRHSMYumRepo_Test(){
		HashMap<String, String[]> pkgList = sm1.getPackagesCorrespondingToSubscribedRepos();
		for(ProductSubscription productSubscription : sm1.getCurrentlyConsumedProductSubscriptions()){
			String pkg = pkgList.get(productSubscription.productName)[0];
			log.info("Attempting to install first pkg '"+pkg+"' from product subscription: "+productSubscription);
			log.info("timeout of two minutes for next three commands");
			RemoteFileTasks.runCommandExpectingNoTracebacks(cl1,
					"yum repolist");
			RemoteFileTasks.runCommandExpectingNoTracebacks(cl1,
					"yum install -y "+pkg);
			RemoteFileTasks.runCommandExpectingNoTracebacks(cl1,
					"rpm -q "+pkg);
		}
	}
	
	
	@Test(description="subscription-manager Yum plugin: enable/disable",
			dependsOnGroups={"sm_stage7"},
			groups={"sm_stage8", "not_implemented"})
	@ImplementsTCMS(id="41696")
	public void DisableYumRepoAndVerifyContentNotAvailable_Test(){
		this.adjustRHSMYumRepo(false);
		for(SubscriptionPool sub:sm1.getCurrentlyAvailableSubscriptionPools())
			for(String repo:this.getYumRepolist())
				if(repo.contains(sub.subscriptionName))
					Assert.fail("After unsubscribe, Yum still has access to repo: "+repo);
	}
	
	
	@Test(description="rhsmcertd: change certFrequency",
			dependsOnGroups={"sm_stage3"},
			groups={"sm_stage4"})
	@ImplementsTCMS(id="41692")
	public void certFrequency_Test(){
		this.changeCertFrequency(cl1,"1");
		this.sleep(70*1000);
		Assert.assertEquals(RemoteFileTasks.grepFile(cl1,
				rhsmcertdLogFile,
				"certificates updated"),
				0,
				"rhsmcertd reports that certificates have been updated at new interval");
	}
	
	
	@Test(description="rhsmcertd: ensure certificates synchronize",
			dependsOnGroups={"sm_stage3"},
			groups={"sm_stage4"})
	@ImplementsTCMS(id="41694")
	public void refreshCerts_Test(){
		sm1.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		//SubscribeToASingleEntitlementByProductID_Test();
		cl1.runCommandAndWait("rm -f /etc/pki/entitlement/*");
		cl1.runCommandAndWait("rm -f /etc/pki/entitlement/product/*");
		cl1.runCommandAndWait("rm -f /etc/pki/product/*");
		cl1.runCommandAndWait("cat /dev/null > "+rhsmcertdLogFile);
		//sshCommandRunner.runCommandAndWait("rm -f "+rhsmcertdLogFile);
		//sshCommandRunner.runCommandAndWait("/etc/init.d/rhsmcertd restart");
		this.sleep(70*1000);
		
		Assert.assertEquals(RemoteFileTasks.grepFile(cl1,
				rhsmcertdLogFile,
				"certificates updated"),
				0,
				"rhsmcertd reports that certificates have been updated");
		
		//verify that PEM files are present in all certificate directories
		RemoteFileTasks.runCommandAndAssert(cl1, 
				"ls /etc/pki/entitlement | grep pem",
				0,
				"pem", 
				null);
		RemoteFileTasks.runCommandAndAssert(cl1, 
				"ls /etc/pki/entitlement/product | grep pem", 
				0,
				"pem", 
				null);
		// this directory will only be populated if you upload ur own license, not while working w/ candlepin
		/*RemoteFileTasks.runCommandAndAssert(sshCommandRunner, 
				"ls /etc/pki/product", 
				0,
				"pem", 
				null);*/
	}
	
	
//	@Test(description="bind/unbind with two users/consumers",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4"})
//	@ImplementsTCMS(id="53217")
//	public void MultiClientSubscribeToSameSubscriptionPool_Test() {
//
//	}
	
	
	// Protected Methods ***********************************************************************

	protected ArrayList<String> getYumRepolist(){
		ArrayList<String> repos = new ArrayList<String>();
		cl1.runCommandAndWait("killall -9 yum");
		
		cl1.runCommandAndWait("yum repolist");
		String[] availRepos = cl1.getStdout().split("\\n");
		
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
	
	protected void adjustRHSMYumRepo(boolean enabled){
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(cl1, 
						rhsmYumRepoFile, 
						"^enabled=.*$", 
						"enabled="+(enabled?'1':'0')),
						0,
						"Adjusted RHSM Yum Repo config file, enabled="+(enabled?'1':'0')
				);
	}
	
	
	// Data Providers ***********************************************************************

	
	@DataProvider(name="getAllAvailableSubscriptionPoolData")
	public Object[][] getAllAvailableSubscriptionPoolDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getAllAvailableSubscriptionPoolDataAsListOfLists());
	}
	protected List<List<Object>> getAllAvailableSubscriptionPoolDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		// unsubscribe from all consumed product subscriptions and then assemble a list of all SubscriptionPools
		sm1.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		for (SubscriptionPool pool : sm1.getCurrentlyAvailableSubscriptionPools()) {
			ll.add(Arrays.asList(new Object[]{pool}));		
		}
		
		return ll;
	}
}
