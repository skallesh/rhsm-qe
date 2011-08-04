package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.ContentNamespace;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.Repo;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.sm.data.YumRepo;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"ReposTests"})
public class ReposTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager: subscribe to a pool and verify that the newly entitled content namespaces are represented in the repos list",
			enabled=true,
			groups={},
			dataProvider="getAvailableSubscriptionPoolsData")
	//@ImplementsNitrateTest(caseId=)
	public void ReposListReportsGrantedContentNamespacesAfterSubscribing_Test(SubscriptionPool pool) throws JSONException, Exception{
		List<Repo> priorRepos = clienttasks.getCurrentlySubscribedRepos();
		
		//File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool);	// for this test, we can skip the exhaustive asserts done by this call to clienttasks.subscribeToSubscriptionPool(pool)
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPool_(pool);
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, entitlementCertFile.getPath()),1, "Found the EntitlementCert file ("+entitlementCertFile+") that was granted after subscribing to pool id '"+pool.poolId+"'.");

		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		
		List<Repo> actualRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// assert that the new contentNamespaces from the entitlementCert are listed in repos
		int numNewRepos=0;
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			
			// instantiate the expected Repo that represents this contentNamespace
			String expectedRepoUrl;	// the expected RepoUrl is set by joining the rhsm.conf baseurl with the downloadUrl in the contentNamespace which is usually a relative path.  When it is already a full path, leave it!
			if (contentNamespace.downloadUrl.contains("://")) {
				expectedRepoUrl = contentNamespace.downloadUrl;
			} else {
				expectedRepoUrl = clienttasks.baseurl.replaceFirst("//+$","//")+contentNamespace.downloadUrl.replaceFirst("^//+","");	// join baseurl to downloadUrl with "/"
			}
			Repo expectedRepo = new Repo(contentNamespace.name,contentNamespace.label,expectedRepoUrl,contentNamespace.enabled.trim().equals("1")?true:false);
			
			// assert the subscription-manager repos --list reports the expectedRepo (unless it requires tags that are not found in the installed product certs)
			if (clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace,currentProductCerts)) {
				Assert.assertTrue(actualRepos.contains(expectedRepo),"The newly entitled contentNamespace '"+contentNamespace+"' is represented in the subscription-manager repos --list by: "+expectedRepo);
				
				if (!priorRepos.contains(expectedRepo)) numNewRepos++;	// also count the number of NEW contentNamespaces
				
			} else {
				Assert.assertFalse(actualRepos.contains(expectedRepo),"The newly entitled contentNamespace '"+contentNamespace+"' is NOT represented in the subscription-manager repos --list because it requires tags ("+contentNamespace.requiredTags+") that are not provided by the currently installed product certs.");
			}
		}
		
		// assert that the number of repos reported has increased by the number of contentNamespaces in the new entitlementCert (unless the 
		Assert.assertEquals(actualRepos.size(), priorRepos.size()+numNewRepos, "The number of entitled repos has increased by the number of NEW contentNamespaces ("+numNewRepos+") from the newly granted entitlementCert.");
		
		// randomly decide to unsubscribe from the pool only for the purpose of saving on accumulated logging and avoid a java heap memory error
		//if (randomGenerator.nextInt(2)==1) clienttasks.unsubscribe(null, entitlementCert.serialNumber, null, null, null);
	}
	
	
	@Test(	description="subscription-manager: after subscribing to all pools, verify that manual edits to enable repos in redhat.repo are preserved.",
			enabled=true,
			groups={},
			dataProvider="getYumReposData")
	//@ImplementsNitrateTest(caseId=)
	public void ReposListPreservesManualEditsToEnablementOfRedhatRepos_Test(YumRepo yumRepo){

		Repo repo = new Repo(yumRepo.name,yumRepo.id,yumRepo.baseurl,yumRepo.enabled);

		// assert that the yumRepo is reported in the subscription-manager repos
		List<Repo> currentlySubscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		Assert.assertTrue(currentlySubscribedRepos.contains(repo),"The yumRepo '"+yumRepo+"' is represented in the subscription-manager repos --list by: "+repo);
		
		// also verify that yumRepo is reported in the yum repolist
		Assert.assertTrue(clienttasks.getYumRepolist(yumRepo.enabled?"enabled":"disabled").contains(yumRepo.id), "yum repolist properly reports the enablement of yumRepo id '"+yumRepo.id+"' before manually changing its enabled value.");
	
		// manually edit the redhat.repo and change the enabled parameter for this yumRepo
		Boolean newEnabledValue = yumRepo.enabled? false:true;	// toggle the value
		clienttasks.updateYumRepoParameter(clienttasks.redhatRepoFile,yumRepo.id,"enabled",newEnabledValue.toString());
		Repo newRepo = new Repo(yumRepo.name,yumRepo.id,yumRepo.baseurl,newEnabledValue);

		// verify that the change is preserved by subscription-manager repos --list
		currentlySubscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		Assert.assertTrue(currentlySubscribedRepos.contains(newRepo),"yumRepo id '"+yumRepo.id+"' was manually changed to enabled="+newEnabledValue+" and the subscription-manager repos --list reflects the change as: "+newRepo);
		Assert.assertFalse(currentlySubscribedRepos.contains(repo),"The original repo ("+repo+") is no longer found in subscription-manager repos --list.");
		
		// also verify the change is reflected in yum repolist
		Assert.assertTrue(clienttasks.getYumRepolist(newEnabledValue?"enabled":"disabled").contains(yumRepo.id), "yum repolist properly reports the enablement of yumRepo id '"+yumRepo.id+"' which was manually changed to '"+newEnabledValue+"'.");
		Assert.assertFalse(clienttasks.getYumRepolist(!newEnabledValue?"enabled":"disabled").contains(yumRepo.id), "yum repolist properly reports the enablement of yumRepo id '"+yumRepo.id+"' which was manually changed to '"+newEnabledValue+"'.");
	}

	
	@Test(	description="subscription-manager: repos --list reports no entitlements when not registered",
			enabled=true,
			groups={"blockedByBug-724809"})
	//@ImplementsNitrateTest(caseId=)
	public void ReposListIsEmptyWhenNotRegistered_Test(){
		
		clienttasks.unregister(null,null,null);		
		
		Assert.assertEquals( clienttasks.getCurrentlySubscribedRepos().size(),0, "No repos are reported by subscription-manager repos --list when not registered.");
	}
		
		
	// Configuration methods ***********************************************************************

	@BeforeClass(groups={"setup"})
	public void setupBeforeClass() {
		currentProductCerts = clienttasks.getCurrentProductCerts();
	}
	
	
	// Protected methods ***********************************************************************

	List<ProductCert> currentProductCerts=new ArrayList<ProductCert>();

	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="getYumReposData")
	public Object[][] getYumReposDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getYumReposDataAsListOfLists());
	}
	protected List<List<Object>> getYumReposDataAsListOfLists() throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, true, null, null, null);
		
		// subscribe to all available subscription so as to populate the redhat.repo file
		clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools();
		
		clienttasks.getYumRepolist("all");	// trigger a yum transaction so that subscription-manager plugin will refresh redhat.repo
		for (YumRepo yumRepo : clienttasks.getCurrentlySubscribedYumRepos()) {
			ll.add(Arrays.asList(new Object[]{yumRepo}));
			
			// minimize the number of dataProvided rows (useful during automated testcase development)
			if (Boolean.valueOf(getProperty("sm.debug.dataProviders.minimize","false"))) break;
		}

		return ll;
	}

}
