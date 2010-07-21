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

@Test(groups={"subscribe"})
public class SubscribeTests extends SubscriptionManagerTestScript{
	
	@Test(description="subscription-manager-cli: subscribe consumer to an entitlement using product ID",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4", "blockedByBug-584137"},
			groups={"blockedByBug-584137"},
			dataProvider="getAllAvailableSubscriptionPoolData")
	@ImplementsTCMS(id="41680")
	public void SubscribeToValidSubscriptionsByProductID_Test(SubscriptionPool pool){
//		sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		c1sm.subscribeToSubscriptionPoolUsingProductId(pool);
	}
	
	
	@Test(description="subscription-manager-cli: subscribe consumer to an entitlement using product ID",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4", "blockedByBug-584137", "not_implemented"},
			groups={"blockedByBug-584137"},
			enabled=false)
	public void SubscribeToASingleEntitlementByProductID_Test(){
		c1sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		SubscriptionPool MCT0696 = new SubscriptionPool("MCT0696", "biteme");
		MCT0696.addProductID("Red Hat Directory Server");
		c1sm.subscribeToSubscriptionPoolUsingProductId(MCT0696);
		//this.refreshSubscriptions();
		for (ProductSubscription pid:MCT0696.associatedProductIDs){
			Assert.assertTrue(c1sm.getCurrentlyConsumedProductSubscriptions().contains(pid),
					"ProductID '"+pid.productName+"' consumed from Pool '"+MCT0696.subscriptionName+"'");
		}
	}
	
	
	@Test(description="subscription-manager-cli: subscribe consumer to an entitlement using pool ID",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4", "blockedByBug-584137"},
			groups={"blockedByBug-584137"},
			dataProvider="getAllAvailableSubscriptionPoolData")
	@ImplementsTCMS(id="41686")
	public void SubscribeToValidSubscriptionsByPoolID_Test(SubscriptionPool pool){
		// non-dataProvided test procedure
		//sm.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		//sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		c1sm.subscribeToSubscriptionPoolUsingPoolId(pool);
	}
	
	
	@Test(description="subscription-manager-cli: subscribe consumer to an entitlement using registration token",
//			dependsOnGroups={"sm_stage8"},
//			groups={"sm_stage9", "blockedByBug-584137", "not_implemented"},
			groups={"blockedByBug-584137"},
			enabled=false)
	@ImplementsTCMS(id="41681")
	public void SubscribeToRegToken_Test(){
		c1sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		c1sm.subscribeToRegToken(regtoken);
	}
	
	
	@Test(description="Subscribed for Already subscribed Entitlement.",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4", "blockedByBug-584137", "not_implemented"},
			groups={"blockedByBug-584137"},
			enabled=false)
	@ImplementsTCMS(id="41897")
	public void SubscribeAndSubscribeAgain_Test(){
		//sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		c1sm.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		for(SubscriptionPool pool : c1sm.getCurrentlyAvailableSubscriptionPools()) {
			c1sm.subscribeToSubscriptionPoolUsingProductId(pool);
			c1sm.subscribeToProduct(pool.subscriptionName);
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
//			dependsOnGroups={"sm_stage5"},
//			groups={"sm_stage6", "not_implemented"},
			enabled=false)
	@ImplementsTCMS(id="41696")
	public void EnableYumRepoAndVerifyContentAvailable_Test() {

		c1sm.register(consumer1username, consumer1password, null, null, null, null);	// assure we are registered
		c1sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		
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
//	        dependsOnGroups={"sm_stage6"},
//	        groups={"sm_stage7"},
	        enabled=true)
	@ImplementsTCMS(id="47578")
	public void VerifyReposAvailableForEnabledContent(){
	    ArrayList<String> repos = this.getYumRepolist();
	    
	    for (EntitlementCert cert:c1sm.getCurrentEntitlementCerts()){
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
//			dependsOnGroups={"sm_stage6"},
//			groups={"sm_stage7", "not_implemented"},
			enabled=false)
	@ImplementsTCMS(id="41695")
	public void InstallPackageFromRHSMYumRepo_Test(){
		HashMap<String, String[]> pkgList = c1sm.getPackagesCorrespondingToSubscribedRepos();
		for(ProductSubscription productSubscription : c1sm.getCurrentlyConsumedProductSubscriptions()){
			String pkg = pkgList.get(productSubscription.productName)[0];
			log.info("Attempting to install first pkg '"+pkg+"' from product subscription: "+productSubscription);
			log.info("timeout of two minutes for next three commands");
			RemoteFileTasks.runCommandExpectingNoTracebacks(c1,
					"yum repolist");
			RemoteFileTasks.runCommandExpectingNoTracebacks(c1,
					"yum install -y "+pkg);
			RemoteFileTasks.runCommandExpectingNoTracebacks(c1,
					"rpm -q "+pkg);
		}
	}
	
	
	@Test(description="subscription-manager Yum plugin: enable/disable",
//			dependsOnGroups={"sm_stage7"},
//			groups={"sm_stage8", "not_implemented"},
			enabled=false)
	@ImplementsTCMS(id="41696")
	public void DisableYumRepoAndVerifyContentNotAvailable_Test(){
		this.adjustRHSMYumRepo(false);
		for(SubscriptionPool sub:c1sm.getCurrentlyAvailableSubscriptionPools())
			for(String repo:this.getYumRepolist())
				if(repo.contains(sub.subscriptionName))
					Assert.fail("After unsubscribe, Yum still has access to repo: "+repo);
	}
	
	
	@Test(description="rhsmcertd: change certFrequency",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4"},
			enabled=true)
	@ImplementsTCMS(id="41692")
	public void certFrequency_Test(){
		this.changeCertFrequency(c1,"1");
		this.sleep(70*1000);
		Assert.assertEquals(RemoteFileTasks.grepFile(c1,
				rhsmcertdLogFile,
				"certificates updated"),
				0,
				"rhsmcertd reports that certificates have been updated at new interval");
	}
	
	
	@Test(description="rhsmcertd: ensure certificates synchronize",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4"},
			enabled=true)
	@ImplementsTCMS(id="41694")
	public void refreshCerts_Test(){
		c1sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		//SubscribeToASingleEntitlementByProductID_Test();
		c1.runCommandAndWait("rm -f /etc/pki/entitlement/*");
		c1.runCommandAndWait("rm -f /etc/pki/entitlement/product/*");
		c1.runCommandAndWait("rm -f /etc/pki/product/*");
		c1.runCommandAndWait("cat /dev/null > "+rhsmcertdLogFile);
		//sshCommandRunner.runCommandAndWait("rm -f "+rhsmcertdLogFile);
		//sshCommandRunner.runCommandAndWait("/etc/init.d/rhsmcertd restart");
		this.sleep(70*1000);
		
		Assert.assertEquals(RemoteFileTasks.grepFile(c1,
				rhsmcertdLogFile,
				"certificates updated"),
				0,
				"rhsmcertd reports that certificates have been updated");
		
		//verify that PEM files are present in all certificate directories
		RemoteFileTasks.runCommandAndAssert(c1, 
				"ls /etc/pki/entitlement | grep pem",
				0,
				"pem", 
				null);
		RemoteFileTasks.runCommandAndAssert(c1, 
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
	
	
	@Test(description="bind/unbind with two users/consumers",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4"},
			dataProvider="getAllAvailableSubscriptionPoolData")
	@ImplementsTCMS(id="53217")
	public void MultiClientSubscribeToSameSubscriptionPool_Test(SubscriptionPool pool) {
		if (c2==null) throw new SkipException("This test requires a second consumer.");
		
		// assert that the subscriptionPool is available to both consumers with the same quantity
		List<SubscriptionPool> cl1SubscriptionPools = c1sm.getCurrentlyAvailableSubscriptionPools();
		Assert.assertTrue(cl1SubscriptionPools.contains(pool),"Subscription pool "+pool+" is available to consumer1 ("+consumer1username+").");
		SubscriptionPool cl1SubscriptionPool = cl1SubscriptionPools.get(cl1SubscriptionPools.indexOf(pool));
		
		List<SubscriptionPool> cl2SubscriptionPools = c2sm.getCurrentlyAvailableSubscriptionPools();
		Assert.assertTrue(cl2SubscriptionPools.contains(pool),"Subscription pool "+pool+" is available to consumer2 ("+consumer2username+").");
		SubscriptionPool cl2SubscriptionPool = cl2SubscriptionPools.get(cl2SubscriptionPools.indexOf(pool));

		Assert.assertEquals(cl1SubscriptionPool.quantity, cl2SubscriptionPool.quantity, "The quantity of entitlements from subscription pool id '"+pool.poolId+"' available to both consumers is the same.");

		// subscribe consumer1 to the pool and assert that the available quantity has decremented by one
		c1sm.subscribeToSubscriptionPoolUsingPoolId(pool);
		cl2SubscriptionPools = c2sm.getCurrentlyAvailableSubscriptionPools();
		Assert.assertTrue(cl2SubscriptionPools.contains(pool),"Subscription pool id "+pool.poolId+" is still available to consumer2 ("+consumer2username+").");
		cl2SubscriptionPool = cl2SubscriptionPools.get(cl2SubscriptionPools.indexOf(pool));

		Assert.assertEquals(cl2SubscriptionPool.quantity.intValue(), cl1SubscriptionPool.quantity.intValue()-1, "The quantity of entitlements from subscription pool id '"+pool.poolId+"' available to consumer2 ("+consumer2username+") has decremented by one.");
	}
	
	
	// Protected Methods ***********************************************************************

	protected ArrayList<String> getYumRepolist(){
		ArrayList<String> repos = new ArrayList<String>();
		c1.runCommandAndWait("killall -9 yum");
		
		c1.runCommandAndWait("yum repolist");
		String[] availRepos = c1.getStdout().split("\\n");
		
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
				RemoteFileTasks.searchReplaceFile(c1, 
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
		
		// assure we are registered
						c1sm.register(consumer1username, consumer1password, null, null, null, null);
		if (c2sm!=null)	c2sm.register(consumer2username, consumer2password, null, null, null, null);
		
		// unsubscribe from all consumed product subscriptions and then assemble a list of all SubscriptionPools
						c1sm.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		if (c2sm!=null)	c2sm.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();

		// populate a list of all available SubscriptionPools
		for (SubscriptionPool pool : c1sm.getCurrentlyAvailableSubscriptionPools()) {
			ll.add(Arrays.asList(new Object[]{pool}));		
		}
		
		return ll;
	}
}
