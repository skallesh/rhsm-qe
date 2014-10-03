package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.ProductCert;
import rhsm.data.ProductSubscription;
import rhsm.data.Repo;
import rhsm.data.SubscriptionPool;
import rhsm.data.YumRepo;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"ReposTests","Tier3Tests"})
public class ReposTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager: subscribe to a pool and verify that the newly entitled content namespaces are represented in the repos list",
			groups={"AcceptanceTests","Tier1Tests","blockedByBug-807407","blockedByBug-962520","blockedByBug-1034649"},
			//dataProvider="getAvailableSubscriptionPoolsData",	// very thorough, but takes too long to execute and rarely finds more bugs
			dataProvider="getRandomSubsetOfAvailableSubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposListReportsGrantedContentNamespacesAfterSubscribingToPool_Test(SubscriptionPool pool) throws JSONException, Exception {

		log.info("Following is a list of previously subscribed repos...");
		List<Repo> priorRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// choose a quantity before subscribing to avoid Stdout: Quantity '1' is not a multiple of instance multiplier '2'
		String quantity = null;
		/*if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.3-1"))*/ if (pool.suggested!=null) {
			if (pool.suggested<1) quantity = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "instance_multiplier"); 	// when the Suggested quantity is 0, let's specify a quantity to avoid Stdout: Quantity '1' is not a multiple of instance multiplier '2'
			if (pool.suggested>1 && quantity==null) quantity = pool.suggested.toString();
		}
		
		// subscribe and get the granted entitlement
		//File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool);	// for this test, we can skip the exhaustive asserts done by this call to clienttasks.subscribeToSubscriptionPool(pool)
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPool_(pool,quantity);
		Assert.assertTrue(RemoteFileTasks.testExists(client, entitlementCertFile.getPath()), "Found the EntitlementCert file ("+entitlementCertFile+") that was granted after subscribing to pool id '"+pool.poolId+"'.");
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		
		
		// the following block of code was added to account for prior subscribed modifier pools that could provide more repos than expected once this pool is subscribed
		// check the modifierSubscriptionData for SubscriptionPools that may already have been subscribed too and will modify this pool thereby enabling more repos than expected 
		for (List<Object> row : modifierSubscriptionData) {
			// ll.add(Arrays.asList(new Object[]{modifierPool, label, modifiedProductIds, requiredTags, providingPools}));
			SubscriptionPool modifierPool = (SubscriptionPool)row.get(0);
			String label = (String)row.get(1);
			List<String> modifiedProductIds = (List<String>)row.get(2);
			String requiredTags = (String)row.get(3);
			List<SubscriptionPool> poolsModified = (List<SubscriptionPool>)row.get(4);
			if (poolsModified.contains(pool)) {
				if (priorSubscribedPoolIds.contains(modifierPool.poolId)) {
					// the modifier's content should now be available in the repos too
					EntitlementCert modifierEntitlementCert = clienttasks.getEntitlementCertCorrespondingToSubscribedPool(modifierPool);						

					// simply add the contentNamespaces (if not already there) from the modifier to the entitlement cert's contentNamespaces so they will be accounted for in the repos list test below
					for (ContentNamespace contentNamespace : modifierEntitlementCert.contentNamespaces) {
						if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;
						if (!entitlementCert.contentNamespaces.contains(contentNamespace)) {
							log.warning("Due to a previously subscribed modifier subscription pool ("+modifierPool.subscriptionName+"), the new repos listed should also include ContentNamespace: "+contentNamespace);
							entitlementCert.contentNamespaces.add(contentNamespace);
						}
					}
				}
			}
		}
		priorSubscribedPoolIds.add(pool.poolId);
		
		log.info("Following is the new list of subscribed repos after subscribing to pool: "+pool);			
		List<Repo> actualRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// assert that the new contentNamespaces from the entitlementCert are listed in repos
		int numNewRepos=0;
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;

			// instantiate the expected Repo that represents this contentNamespace
			String expectedRepoUrl;	// the expected RepoUrl is set by joining the rhsm.conf baseurl with the downloadUrl in the contentNamespace which is usually a relative path.  When it is already a full path, leave it!
			if (contentNamespace.downloadUrl.contains("://")) {
				expectedRepoUrl = contentNamespace.downloadUrl;
			} else {
				expectedRepoUrl = clienttasks.baseurl.replaceFirst("//+$","//")+contentNamespace.downloadUrl.replaceFirst("^//+","");	// join baseurl to downloadUrl with "/"
			}
			Repo expectedRepo = new Repo(contentNamespace.name,contentNamespace.label,expectedRepoUrl,contentNamespace.enabled);
			
			// assert the subscription-manager repos --list reports the expectedRepo (unless it requires tags that are not found in the installed product certs)
			if (clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace,currentProductCerts)) {
				Assert.assertTrue(actualRepos.contains(expectedRepo),"The newly entitled contentNamespace '"+contentNamespace+"' is represented in the subscription-manager repos --list by: "+expectedRepo);
				
				if (!priorRepos.contains(expectedRepo)) numNewRepos++;	// also count the number of NEW contentNamespaces
				
			} else {
				Assert.assertFalse(actualRepos.contains(expectedRepo),"The newly entitled contentNamespace '"+contentNamespace+"' is NOT represented in the subscription-manager repos --list because it requires tags ("+contentNamespace.requiredTags+") that are not provided by the currently installed product certs.");
			}
		}

		
		// assert that the number of repos reported has increased by the number of contentNamespaces in the new entitlementCert (unless the 
		Assert.assertEquals(actualRepos.size(), priorRepos.size()+numNewRepos, "The number of entitled repos has increased by the number of NEW contentNamespaces ("+numNewRepos+") from the newly granted entitlementCert (including applicable contentNamespaces from a previously subscribed modifier pool).");
		
		// randomly decide to unsubscribe from the pool only for the purpose of saving on accumulated logging and avoid a java heap memory error
		//if (randomGenerator.nextInt(2)==1) clienttasks.unsubscribe(null, entitlementCert.serialNumber, null, null, null); AND ALSO REMOVE pool FROM priorSubscribedPools
	}
	protected Set<String> priorSubscribedPoolIds=new HashSet<String>();
	
	
	@Test(	description="subscription-manager: subscribe to a future pool and verify that NO content namespaces are represented in the repos list",
			groups={"blockedByBug-768983","unsubscribeAllBeforeThisTest"},
			dataProvider="getAllFutureSystemSubscriptionPoolsData",
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void ReposListReportsNoContentNamespacesAfterSubscribingToFuturePool_Test(SubscriptionPool pool) throws Exception {
//if (!pool.productId.equals("awesomeos-virt-unlmtd-phys")) throw new SkipException("debugTesting productId="+pool.productId);
		
		// subscribe to the future SubscriptionPool
		SSHCommandResult subscribeResult = clienttasks.subscribe(null,null,pool.poolId,null,null,null,null,null,null,null, null);
		// Pool is restricted to virtual guests: '8a90f85734205a010134205ae8d80403'.
		// Pool is restricted to physical systems: '8a9086d3443c043501443c052aec1298'.
		if (subscribeResult.getStdout().startsWith("Pool is restricted")) {
			throw new SkipException("Subscribing to this future subscription is not applicable to this test: "+pool);
		}
		
		// assert that the granted EntitlementCert and its corresponding key exist
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertCorrespondingToSubscribedPool(pool);
		File entitlementCertFile = clienttasks.getEntitlementCertFileFromEntitlementCert(entitlementCert);
		File entitlementCertKeyFile = clienttasks.getEntitlementCertKeyFileFromEntitlementCert(entitlementCert);
		Assert.assertTrue(RemoteFileTasks.testExists(client, entitlementCertFile.getPath()), "EntitlementCert file exists after subscribing to future SubscriptionPool.");
		Assert.assertTrue(RemoteFileTasks.testExists(client, entitlementCertKeyFile.getPath()), "EntitlementCert key file exists after subscribing to future SubscriptionPool.");
		
		// assuming that we are not subscribed to a non-future subscription pool, assert that there are NO subscribed repos 
		Assert.assertEquals(clienttasks.getCurrentlySubscribedRepos().size(),0,"Assuming that we are not currently subscribed to a non-future subscription pool, then there should NOT be any repos reported after subscribing to future subscription pool '"+pool.poolId+"'.");
		
		// TODO we may want to randomly unsubscribe from serial number without asserting to save some computation of the accumulating entitlement certs
	}
	
	
	@Test(	description="subscription-manager: after subscribing to all pools, verify that manual edits to enable repos in redhat.repo are preserved.",
			groups={"blockedByBug-905546"/*UNCOMMENT FOR RHEL71,"blockedByBug-1098891"*/,"blockedByBug-1101571","blockedByBug-1101584"},
			dataProvider="getRandomSubsetOfYumReposData",	// dataProvider="getYumReposData", takes too long to execute
			enabled=true)	// with the implementation of RFE Bug 803746, manual edits to the enablement of redhat repos is now forbidden.  This test is being disabled in favor of ManualEditsToEnablementOfRedhatReposIsForbidden_Test
	//@ImplementsNitrateTest(caseId=)
	public void ReposListPreservesManualEditsToEnablementOfRedhatRepos_Test(YumRepo yumRepo){
		verifyTogglingTheEnablementOfRedhatRepo(yumRepo,true);
	}
	@Test(	description="subscription-manager: verify that manual edits to enable repos in redhat.repo are forbidden by documented warning.",
			groups={"blockedByBug-1032243"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ManualEditsToEnablementOfRedhatReposIsForbidden_Test(){	// replacement for ReposListPreservesManualEditsToEnablementOfRedhatRepos_Test
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null);
		clienttasks.yumClean("all");	// to trip the subscription-manager yum plugin
		
		SSHCommandResult result = client.runCommandAndWaitWithoutLogging("cat "+clienttasks.redhatRepoFile);
		//	[root@jsefler-7 ~]# cat /etc/yum.repos.d/redhat.repo  | head -10
		//	#
		//	# Certificate-Based Repositories
		//	# Managed by (rhsm) subscription-manager
		//	#
		//	# *** This file is auto-generated.  Changes made here will be over-written. ***
		//	# *** Use "subscription-manager override" if you wish to make changes. ***
		//	#
		//	# If this file is empty and this system is subscribed consider 
		//	# a "yum repolist" to refresh available repos
		//	#

		String expectedMessage;
		expectedMessage = "*** This file is auto-generated.  Changes made here will be over-written. ***";
		Assert.assertTrue(result.getStdout().contains(expectedMessage),"File '"+clienttasks.redhatRepoFile+"' warns the user with expected message '"+expectedMessage+"'.");
		expectedMessage = "*** Use \"subscription-manager override\" if you wish to make changes. ***";	// value prior to bug 1032243
		expectedMessage = "*** Use \"subscription-manager repo-override --help\" if you wish to make changes. ***";
		Assert.assertTrue(result.getStdout().contains(expectedMessage),"File '"+clienttasks.redhatRepoFile+"' warns the user with expected message '"+expectedMessage+"'.");
	}

	
	@Test(	description="subscription-manager: after subscribing to all pools, verify that edits (using subscription-manager --enable --disable options) to repos in redhat.repo are preserved.",
			groups={"blockedByBug-905546"},
			dataProvider="getRandomSubsetOfYumReposData",	// dataProvider="getYumReposData", takes too long to execute
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposListPreservesEnablementOfRedhatRepos_Test(YumRepo yumRepo){
		verifyTogglingTheEnablementOfRedhatRepo(yumRepo,false);
	}
	
	
	@Test(	description="subscription-manager: after subscribing to all pools, verify that edits (using subscription-manager --enable --disable options specified multiple times in a single call) to repos in redhat.repo are preserved.",
			groups={"AcceptanceTests","Tier1Tests","blockedByBug-843915","blockedByBug-962520","blockedByBug-1034649"/*,"blockedByBug-1121272" uncomment for rhel66*/},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposListPreservesSimultaneousEnablementOfRedhatRepos_Test(){
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		
		// subscribe to all available subscription so as to populate the redhat.repo file
		clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();
		
		// get the current listing of repos
		List<Repo> originalRepos = clienttasks.getCurrentlySubscribedRepos();	// determined by calling subscription-manager repos --list
		//List<YumRepo> yumRepos = clienttasks.getCurrentlySubscribedYumRepos();	// determined by parsing /etc/yum.repos.d/redhat.repo
		
		// verify the repos listed and yumRepos are in sync
		//for (Repo repo : originalRepos) {
		//	Assert.assertNotNull(YumRepo.findFirstInstanceWithMatchingFieldFromList("id", repo.repoId, yumRepos),"Found yum repo id ["+repo.repoId+"] matching current repos --list item: "+repo);
		//}
		
		// assemble lists of the current repoIds to be collectively toggled
		List<String> enableRepoIds = new ArrayList<String>();
		List<String> disableRepoIds = new ArrayList<String>();
		for (Repo repo : originalRepos) {
			if (repo.enabled) {
				disableRepoIds.add(repo.repoId);
			} else {
				enableRepoIds.add(repo.repoId);
			}
		}
		
		// collectively toggle the enablement of the current repos
		clienttasks.repos(null, null, null, enableRepoIds, disableRepoIds, null, null, null);
		
		// verify that the change is preserved by subscription-manager repos --list
		List<Repo> toggledRepos = clienttasks.getCurrentlySubscribedRepos();	// determined by calling subscription-manager repos --list
		//List<YumRepo> toggledYumRepos = clienttasks.getCurrentlySubscribedYumRepos();	// determined by parsing /etc/yum.repos.d/redhat.repo
		
		// assert enablement of all the original repos have been toggled
		Assert.assertEquals(toggledRepos.size(), originalRepos.size(), "The count of repos listed should remain the same after collectively toggling their enablement.");
		for (Repo originalRepo : originalRepos) {
			Repo toggledRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", originalRepo.repoId, toggledRepos);
			Assert.assertTrue(toggledRepo.enabled.equals(!originalRepo.enabled), "Repo ["+originalRepo.repoId+"] enablement has been toggled from '"+originalRepo.enabled+"' to '"+!originalRepo.enabled+"'.");
		}
		
		// now remove and refresh entitlement certificates and again assert the toggled enablement is preserved
		log.info("Remove and refresh entitlement certs...");
		clienttasks.removeAllCerts(false,true, false);
		clienttasks.refresh(null, null, null);
		
		// verify that the change is preserved by subscription-manager repos --list
		toggledRepos = clienttasks.getCurrentlySubscribedRepos();	// determined by calling subscription-manager repos --list
		//List<YumRepo> toggledYumRepos = clienttasks.getCurrentlySubscribedYumRepos();	// determined by parsing /etc/yum.repos.d/redhat.repo
		
		// assert enablement of all the original repos have been toggled
		Assert.assertEquals(toggledRepos.size(), originalRepos.size(), "Even after refreshing certificates, the count of repos listed should remain the same after collectively toggling their enablement.");
		for (Repo originalRepo : originalRepos) {
			Repo toggledRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", originalRepo.repoId, toggledRepos);
			Assert.assertTrue(toggledRepo.enabled.equals(!originalRepo.enabled), "Even after refreshing certificates, repo ["+originalRepo.repoId+"] enablement has been toggled from '"+originalRepo.enabled+"' to '"+!originalRepo.enabled+"'.");
		}
	}
	
	
	@Test(	description="subscription-manager: repos --list reports no entitlements when not registered",
			groups={"blockedByBug-724809","blockedByBug-807360","blockedByBug-837447"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposListIsEmptyWhenNotRegistered_Test(){
		clienttasks.unregister(null,null,null);		
		Assert.assertEquals(clienttasks.getCurrentlySubscribedRepos().size(),0, "No repos are reported by subscription-manager repos --list when not registered.");
	}
	
	
	@Test(	description="subscription-manager: repos --list reports no entitlements when not registered",
			groups={"blockedByBug-837447"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposListWhenNotRegistered_Test(){
		clienttasks.unregister(null,null,null);
		SSHCommandResult result = clienttasks.repos(true, null, null, (String)null, (String)null, null, null, null);
		//Assert.assertEquals(result.getStdout().trim(), "The system is not entitled to use any repositories.");
		Assert.assertEquals(result.getStdout().trim(), "This system has no repositories available through subscriptions.");
	}
	
	
	@Test(	description="subscription-manager: repos (without any options) reports no entitlements when not registered (rhel63 and rhel58 previously reported 'Error: No options provided. Please see the help comand.')",
			groups={"blockedByBug-837447"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposWhenNotRegistered_Test(){
		clienttasks.unregister(null,null,null);
		SSHCommandResult result = clienttasks.repos(null, null, null, (String)null, (String)null, null, null, null); // defaults to --list behavior starting in rhel59
		//Assert.assertEquals(result.getStdout().trim(), "The system is not entitled to use any repositories.");
		Assert.assertEquals(result.getStdout().trim(), "This system has no repositories available through subscriptions.");
	}
	
	
	@Test(	description="subscription-manager: set manage_repos to 0 and assert redhat.repo is removed.",
			groups={"ManageReposTests","AcceptanceTests","Tier1Tests","blockedByBug-767620","blockedByBug-797996","blockedByBug-895462","blockedByBug-1034649"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposListIsDisabledByConfigurationAfterRhsmManageReposIsConfiguredOff_Test() throws JSONException, Exception{
		
		// manually set the manage_repos to 1
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "manage_repos", "1");
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		
		// assert that the repos list is enabled.
		//Assert.assertTrue(clienttasks.repos(true,(String)null,(String)null,null,null,null).getStdout().trim().equals("The system is not entitled to use any repositories."), "The system is not entitled to use any repositories while the rhsm.manage_repos configuration value is 1.");
		Assert.assertEquals(clienttasks.repos(true,null,null,(String)null,(String)null,null, null, null).getStdout().trim(),"This system has no repositories available through subscriptions.", "This system has no repositories available through subscriptions while the rhsm.manage_repos configuration value is 1.");

		// assert that the redhat.repo exists before and after a yum transaction
		//Assert.assertEquals(RemoteFileTasks.testFileExists(client, clienttasks.redhatRepoFile),1,"When rhsm.manage_repos is configured on, the redhat.repo should exist after registration.");
		clienttasks.getYumRepolist(null);
		Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"When rhsm.manage_repos configuration value is non-zero, the redhat.repo should exist after yum transaction.");

		// assert that the repos list is not entitled to use any repositories, but is enabled by configuration!
		//Assert.assertTrue(clienttasks.repos(true,(String)null,(String)null,null,null,null).getStdout().trim().equals("The system is not entitled to use any repositories."), "The system is not entitled to use any repositories while the rhsm.manage_repos configuration value is 1.");
		Assert.assertEquals(clienttasks.repos(true,null,null,(String)null,(String)null,null, null, null).getStdout().trim(),"This system has no repositories available through subscriptions.", "This system has no repositories available through subscriptions while the rhsm.manage_repos configuration value is 1.");

		// NOW DISABLE THE rhsm.manage_repos CONFIGURATION FILE PARAMETER
		clienttasks.config(null, null, true, new String[]{"rhsm","manage_repos","0"});
		
		// assert that the repos list is disabled by configuration!
		SSHCommandResult reposListResult = clienttasks.repos(true,null,null,(String)null,(String)null,null, null, null);
		//Assert.assertTrue(reposListResult.getStdout().contains("Repositories disabled by configuration."), "Repositories disabled by configuration since the rhsm.manage_repos configuration value is 0.");
		Assert.assertEquals(reposListResult.getStdout().trim(),"Repositories disabled by configuration.", "Repositories disabled by configuration since the rhsm.manage_repos configuration value is 0.");
		//Assert.assertTrue(reposListResult.getStdout().contains("The system is not entitled to use any repositories."), "The system is not entitled to use any repositories since the rhsm.manage_repos configuration value is 0.");
		//Assert.assertTrue(reposListResult.getStdout().contains("This system has no repositories available through subscriptions."), "This system has no repositories available through subscriptions since the rhsm.manage_repos configuration value is 0.");

		// trigger a yum transaction and assert that the redhat.repo no longer exists
		clienttasks.getYumRepolist(null);
		Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"When rhsm.manage_repos configuration value is 0, the redhat.repo file should not exist anymore.");
		
		// subscribe to all subscriptions and re-assert that the repos list remains disabled.
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		reposListResult = clienttasks.repos(true,null,null,(String)null,(String)null,null, null, null);
		//Assert.assertTrue(reposListResult.getStdout().contains("Repositories disabled by configuration."), "Repositories disabled by configuration even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		Assert.assertEquals(reposListResult.getStdout().trim(),"Repositories disabled by configuration.", "Repositories disabled by configuration even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		//Assert.assertTrue(reposListResult.getStdout().contains("The system is not entitled to use any repositories."), "The system is not entitled to use any repositories even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		//Assert.assertTrue(reposListResult.getStdout().contains("This system has no repositories available through subscriptions."), "This system has no repositories available through subscriptions even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"Even after subscribing to all subscription pools while the rhsm.manage_repos configuration value is 0, the redhat.repo is not generated.");
	}
	
	
	@Test(	description="subscription-manager: set manage_repos to 1 and assert redhat.repo is restored.",
			groups={"blockedByBug-767620","blockedByBug-797996","blockedByBug-895462","ManageReposTests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposListIsEnabledByConfigurationAfterRhsmManageReposIsConfiguredOn_Test() throws JSONException, Exception{
		
		// manually set the manage_repos to 0
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "manage_repos", "0");
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);

		// assert that the repos list is disabled.
		SSHCommandResult reposListResult = clienttasks.repos(true,null,null,(String)null,(String)null,null, null, null);
		//Assert.assertTrue(reposListResult.getStdout().contains("Repositories disabled by configuration."), "Repositories disabled by configuration remains even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		Assert.assertEquals(reposListResult.getStdout().trim(),"Repositories disabled by configuration.", "Repositories disabled by configuration remains even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		//Assert.assertTrue(reposListResult.getStdout().contains("The system is not entitled to use any repositories."), "The system is not entitled to use any repositories even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		//Assert.assertTrue(reposListResult.getStdout().contains("This system has no repositories available through subscriptions."), "This system has no repositories available through subscriptions even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");

		// assert that the redhat.repo does NOT exist before and after a yum transaction
		//Assert.assertEquals(RemoteFileTasks.testFileExists(client, clienttasks.redhatRepoFile),0,"When rhsm.manage_repos is configured off, the redhat.repo should NOT exist after registration.");
		clienttasks.getYumRepolist(null);
		Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"When the rhsm.manage_repos configuration value is set to 0, the redhat.repo should NOT exist after yum transaction.");

		// subscribe to all subscriptions and re-assert that the repos list remains disabled.
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		reposListResult = clienttasks.repos(true,null,null,(String)null,(String)null,null, null, null);
		//Assert.assertTrue(reposListResult.getStdout().contains("Repositories disabled by configuration."), "Repositories disabled by configuration remains even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		Assert.assertEquals(reposListResult.getStdout().trim(),"Repositories disabled by configuration.", "Repositories disabled by configuration remains even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		//Assert.assertTrue(reposListResult.getStdout().contains("The system is not entitled to use any repositories."), "The system is not entitled to use any repositories even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		//Assert.assertTrue(reposListResult.getStdout().contains("This system has no repositories available through subscriptions."), "This system has no repositories available through subscriptions even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"Even after subscribing to all subscription pools while the rhsm.manage_repos configuration value is set to 0, the redhat.repo is not generated.");

		// NOW ENABLE THE rhsm.manage_repos CONFIGURATION FILE PARAMETER
		clienttasks.config(null, null, true, new String[]{"rhsm","manage_repos","1"});
			
		// assert that the repos list is not entitled to use any repositories, but is enabled by configuration!
		Assert.assertTrue(!clienttasks.getCurrentlySubscribedRepos().isEmpty(), "Now that the system's rhsm.manage_repos is enabled, the entitled content (assuming>0) is displayed in the repos --list.");

		// trigger a yum transaction and assert that the redhat.repo now exists
		clienttasks.getYumRepolist(null);
		Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"When the rhsm.manage_repos configuration value is non-zero, the redhat.repo file should now exist.");
	}
	
	
	@Test(	description="subscription-manager: attempt to enable an invalid repo id",
			groups={"blockedByBug-846207"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposEnableInvalidRepo_Test(){
		String invalidRepo = "invalid-repo-id";
		SSHCommandResult result = clienttasks.repos_(null, null, null, invalidRepo, null, null, null, null);
		//Assert.assertEquals(result.getExitCode(), new Integer(0), "ExitCode from an attempt to enable an invalid-repo-id.");	// valid in RHEL59
		//Assert.assertEquals(result.getStdout().trim(), "Error: A valid repo id is required. Use --list option to see valid repos.", "Stdout from an attempt to enable an invalid-repo-id.");	// valid in RHEL59
		Assert.assertEquals(result.getExitCode(), new Integer(1), "ExitCode from an attempt to enable an invalid-repo-id.");
		//Assert.assertEquals(result.getStdout().trim(), String.format("Error: %s is not a valid repo id. Use --list option to see valid repos.",invalidRepo), "Stdout from an attempt to enable an invalid-repo-id.");	// changed by bug 878634
		Assert.assertEquals(result.getStdout().trim(), String.format("Error: %s is not a valid repo ID. Use --list option to see valid repos.",invalidRepo), "Stdout from an attempt to enable an invalid-repo-id.");
		Assert.assertEquals(result.getStderr().trim(), "", "Stderr from an attempt to enable an invalid-repo-id.");
	}
	
	
	@Test(	description="subscription-manager: attempt to disable an invalid repo id",
			groups={"blockedByBug-846207"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposDisableInvalidRepo_Test(){
		String invalidRepo = "invalid-repo-id";
		SSHCommandResult result = clienttasks.repos_(null, null, null, null, invalidRepo, null, null, null);
		//Assert.assertEquals(result.getExitCode(), new Integer(0), "ExitCode from an attempt to disable an invalid-repo-id.");	// valid in RHEL59
		//Assert.assertEquals(result.getStdout().trim(), "Error: A valid repo id is required. Use --list option to see valid repos.", "Stdout from an attempt to disable an invalid-repo-id.");	// valid in RHEL59
		Assert.assertEquals(result.getExitCode(), new Integer(1), "ExitCode from an attempt to disable an invalid-repo-id.");
		//Assert.assertEquals(result.getStdout().trim(), String.format("Error: %s is not a valid repo id. Use --list option to see valid repos.",invalidRepo), "Stdout from an attempt to disable an invalid-repo-id.");	// changed by bug 878634
		Assert.assertEquals(result.getStdout().trim(), String.format("Error: %s is not a valid repo ID. Use --list option to see valid repos.",invalidRepo), "Stdout from an attempt to disable an invalid-repo-id.");
		Assert.assertEquals(result.getStderr().trim(), "", "Stderr from an attempt to disable an invalid-repo-id.");
	}
	
	
	@Test(	description="subscription-manager: attempt multiple enable/disable invalid repo ids",
			groups={"blockedByBug-846207","blockedByBug-918746"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposEnableDisableMultipleInvalidRepo_Test(){
		//	[root@jsefler-6 ~]# subscription-manager repos --enable=invalid-repo-A --enable=invalid-repo-B --disable=invalid-repo-C --disable=invalid-repo-D
		//	Error: invalid-repo-A is not a valid repo ID. Use --list option to see valid repos.
		//	Error: invalid-repo-B is not a valid repo ID. Use --list option to see valid repos.
		//	Error: invalid-repo-C is not a valid repo ID. Use --list option to see valid repos.
		//	Error: invalid-repo-D is not a valid repo ID. Use --list option to see valid repos.
		//	[root@jsefler-6 ~]# echo $?
		//	1
		
		List<String> enableRepos = Arrays.asList(new String[]{"invalid-repo-A","invalid-repo-B"});
		List<String> disableRepos = Arrays.asList(new String[]{"invalid-repo-C","invalid-repo-D"});
		List<String> invalidRepos = new ArrayList<String>(); invalidRepos.addAll(enableRepos); invalidRepos.addAll(disableRepos);
		SSHCommandResult result = clienttasks.repos_(null, null, null, enableRepos, disableRepos, null, null, null);
		Assert.assertEquals(result.getExitCode(), new Integer(1), "ExitCode from an attempt to disable an invalid-repo-id.");
		Assert.assertEquals(result.getStderr().trim(), "", "Stderr from an attempt to disable an invalid-repo-id.");
		String expectedStdoutMsgFormat = "Error: %s is not a valid repo ID. Use --list option to see valid repos.";
		for (String invalidRepo : invalidRepos) {
			String expectedStdoutMsg = String.format(expectedStdoutMsgFormat,invalidRepo);
			Assert.assertTrue(result.getStdout().contains(expectedStdoutMsg), "Stdout from an attempt to enable/disable multiple invalid repos contains expected message: "+expectedStdoutMsg);		
		}
		/* TODO: USE THIS BLOCK OF CODE IF BUG 918746 IS REJECTED
		for (String invalidRepo : enableRepos) {
			String expectedStdoutMsg = String.format(expectedStdoutMsgFormat,invalidRepo);
			Assert.assertTrue(result.getStdout().contains(expectedStdoutMsg), "Stdout from an attempt to enable multiple invalid repos contains expected message: "+expectedStdoutMsg);		
		}
		for (String invalidRepo : disableRepos) {
			String expectedStdoutMsg = String.format(expectedStdoutMsgFormat,invalidRepo);
			Assert.assertTrue(!result.getStdout().contains(expectedStdoutMsg), "Stdout from an attempt to disable multiple invalid repos does NOT contain expected message (because an invalid repo is effectively always disabled): "+expectedStdoutMsg);		
		}
		*/
	}
	
	
	@Test(	description="subscription-manager: attempt enable/disable all repos (using wildcard *)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposEnableDisableWildcardRepos_Test() throws JSONException, Exception{
		//	[root@jsefler-6 ~]# subscription-manager repos --enable=* --disable=*
		//	Repo awesomeos is enabled for this system.
		//	Repo awesomeos-x86_64 is enabled for this system.
		//	Repo awesomeos-modifier is enabled for this system.
		//	Repo awesomeos-ppc is enabled for this system.
		//	Repo never-enabled-content is enabled for this system.
		//	Repo awesomeos-ppc64 is enabled for this system.
		//	Repo content-label is enabled for this system.
		//	Repo content-label-empty-gpg is enabled for this system.
		//	Repo awesomeos-s390x is enabled for this system.
		//	Repo content-label-no-gpg is enabled for this system.
		//	Repo awesomeos-ia64 is enabled for this system.
		//	Repo awesomeos-i686 is enabled for this system.
		//	Repo awesomeos-x86_64-i386-content is enabled for this system.
		//	Repo awesomeos-x86_64-only-content is enabled for this system.
		//	Repo awesomeos is disabled for this system.
		//	Repo awesomeos-x86_64 is disabled for this system.
		//	Repo awesomeos-modifier is disabled for this system.
		//	Repo awesomeos-ppc is disabled for this system.
		//	Repo never-enabled-content is disabled for this system.
		//	Repo awesomeos-ppc64 is disabled for this system.
		//	Repo content-label is disabled for this system.
		//	Repo content-label-empty-gpg is disabled for this system.
		//	Repo awesomeos-s390x is disabled for this system.
		//	Repo content-label-no-gpg is disabled for this system.
		//	Repo awesomeos-ia64 is disabled for this system.
		//	Repo awesomeos-i686 is disabled for this system.
		//	Repo awesomeos-x86_64-i386-content is disabled for this system.
		//	Repo awesomeos-x86_64-only-content is disabled for this system.
		//	[root@jsefler-6 ~]# echo $?
		//	0

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();

		SSHCommandResult result = clienttasks.repos_(null, null, null, "*", "*", null, null, null);
		Assert.assertEquals(result.getExitCode(), new Integer(0), "ExitCode from an attempt to enable/disable all repos (using wildcard *).");
		String expectedStdoutMsgFormat = "Repo %s is %s for this system.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.7-1")) expectedStdoutMsgFormat = "Repo '%s' is %s for this system.";
		for (Repo subscribedRepo : subscribedRepos) {
			String expectedEnableStdoutMsg = String.format(expectedStdoutMsgFormat,subscribedRepo.repoId,"enabled");
			String expectedDisableStdoutMsg = String.format(expectedStdoutMsgFormat,subscribedRepo.repoId,"disabled");
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.12.8-1")) {
				Assert.assertFalse(result.getStdout().contains(expectedEnableStdoutMsg), "Stdout from an attempt to enable/disable all repos (using wildcard *) contains expected message: "+expectedEnableStdoutMsg);		
			} else {
				Assert.assertTrue(result.getStdout().contains(expectedEnableStdoutMsg), "Stdout from an attempt to enable/disable all repos (using wildcard *) contains expected message: "+expectedEnableStdoutMsg);		
			}
			Assert.assertTrue(result.getStdout().contains(expectedDisableStdoutMsg), "Stdout from an attempt to enable/disable all repos (using wildcard *) contains expected message: "+expectedDisableStdoutMsg);		
		}
	}
	
	
	@Test(	description="subscription-manager: attempt enable/disable/enable/disable repos in an order",
			groups={"blockedByBug-1115499"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposEnableDisableReposInOrder_Test() throws JSONException, Exception{
		//	[root@jsefler-6 ~]# subscription-manager repos --enable=awesomeos-ppc64 --enable=awesomeos-x86_64 --disable=awesomeos-x86_64 --disable=awesomeos-ia64 --enable=awesomeos-ia64 --disable=awesomeos-ppc64
		//	Repo 'awesomeos-ppc64' is disabled for this system.
		//	Repo 'awesomeos-x86_64' is disabled for this system.
		//	Repo 'awesomeos-ia64' is enabled for this system.

		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.12.8-1")) throw new SkipException("Bugzilla 1115499 was not implemented in this version of subscription-manager.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		//List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		List<Repo> subscribedRepos = getRandomSubsetOfList(clienttasks.getCurrentlySubscribedRepos(),5);
		
		String expectedStdoutMsgFormat = "Repo %s is %s for this system.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.7-1")) expectedStdoutMsgFormat = "Repo '%s' is %sd for this system.";
		String command = clienttasks.reposCommand(null, null, null, null, null, null, null, null);
		Map<String,String> repoEnablements = new HashMap<String,String>();
		List<String> enablements = Arrays.asList("enable","disable");
		for (Repo subscribedRepo : subscribedRepos) {
			for (int i=0; i<4; i++) {
				String enablement = (getRandomListItem(enablements)); // enable or disable
				command += String.format(" --%s=%s",enablement,subscribedRepo.repoId);
				repoEnablements.put(subscribedRepo.repoId, enablement);
			}
		}
		SSHCommandResult sshCommandResult = client.runCommandAndWait(command);
		Assert.assertEquals(sshCommandResult.getExitCode(), new Integer(0), "ExitCode from an attempt to enable/disable multiple valid repos.");
		for (Repo subscribedRepo : subscribedRepos) {
			String expectedEnableStdoutMsg = String.format(expectedStdoutMsgFormat, subscribedRepo.repoId,repoEnablements.get(subscribedRepo.repoId));
			Assert.assertTrue(sshCommandResult.getStdout().contains(expectedEnableStdoutMsg), "Stdout from an attempt to enable/disable repos in order contains expected message: "+expectedEnableStdoutMsg);			
		}
		List<YumRepo> currentlySubscribedYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		for (Repo subscribedRepo : subscribedRepos) {
			YumRepo yumRepo = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", subscribedRepo.repoId, currentlySubscribedYumRepos);
			if (repoEnablements.get(subscribedRepo.repoId).equals("enable")) {
				Assert.assertTrue(yumRepo.enabled, "Enablement of yum repo "+yumRepo.id);
			} else {
				Assert.assertFalse(yumRepo.enabled, "Enablement of yum repo "+yumRepo.id);
			}
		}
	}
	
	
	/**
	 * @author skallesh
	 * @throws JSONException
	 * @throws Exception
	 */
	@Test(	description="subscription-manager: enable a repo.",
			enabled=true,
			groups={})
	//@ImplementsNitrateTest(caseId=)
	public void ReposEnable_Test() throws JSONException, Exception {
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		subscribedRepos = getRandomSubsetOfList(subscribedRepos, 5);	// reduce the runtime of this test by randomly reducing the subscribedRepos tested
		if (subscribedRepos.isEmpty()) throw new SkipException("There are no entitled repos available for this test.");
				
		for(Repo repo:subscribedRepos) {
			SSHCommandResult result = clienttasks.repos_(false,null,null,repo.repoId,null,null, null, null);
			String expectedStdout = String.format("Repo %s is enabled for this system.",repo.repoId);
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.7-1")) expectedStdout = String.format("Repo '%s' is enabled for this system.",repo.repoId);	// subscription-manager commit b9e7f7abb949bc007f2db02662e2abba76528082
			Assert.assertEquals(result.getStdout().trim(),expectedStdout);
		}
	}
	@Test(	description="subscription-manager: disable a repo.",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposDisable_Test() throws JSONException, Exception {
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		subscribedRepos = getRandomSubsetOfList(subscribedRepos, 5);	// reduce the runtime of this test by randomly reducing the subscribedRepos tested
		if (subscribedRepos.isEmpty()) throw new SkipException("There are no entitled repos available for this test.");
				
		for(Repo repo:subscribedRepos) {
			SSHCommandResult result = clienttasks.repos_(false,null,null,null,repo.repoId,null, null, null);
			String expectedStdout = String.format("Repo %s is disabled for this system.",repo.repoId);
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.7-1")) expectedStdout = String.format("Repo '%s' is disabled for this system.",repo.repoId);	// subscription-manager commit b9e7f7abb949bc007f2db02662e2abba76528082
			Assert.assertEquals(result.getStdout().trim(),expectedStdout);
		}
	}
	
	
	@Test(	description="subscription-manager: manually add more yum repository options to redhat.repo and assert persistence.",
			groups={"AcceptanceTests","Tier1Tests","blockedByBug-845349","blockedByBug-834806"/*UNCOMMENT FOR RHEL71,"blockedByBug-1098891"*/,"blockedByBug-1101571","blockedByBug-1101584"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void YumRepoListPreservesAdditionalOptionsToRedhatReposUsingManualEdits_Test() throws JSONException, Exception {
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<YumRepo> subscribedYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		if (subscribedYumRepos.isEmpty()) throw new SkipException("There are no entitled yum repos available for this test.");
		
		// randomly choose one of the YumRepos, set new option values, and manually update the yum repo
		YumRepo yumRepo = subscribedYumRepos.get(randomGenerator.nextInt(subscribedYumRepos.size()));
		//yumRepo = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", "awesomeos-modifier",subscribedYumRepos);	// debugTesting case when yum repo comes from a modifier entitlement
		//yumRepo = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", "content-label-no-gpg",subscribedYumRepos);	// debugTesting case when sslclientcert appears to change.
		yumRepo.exclude = "my-test-pkg my-test-pkg-* my-test-pkgversion-?";
		yumRepo.priority = new Integer(10);
		clienttasks.updateYumRepo(clienttasks.redhatRepoFile, yumRepo);
		
		// assert that the manually added repository options do not get clobbered by a new yum transaction
		client.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin");	// issue a new yum transaction
		YumRepo yumRepoAfterUpdate = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", yumRepo.id, clienttasks.getCurrentlySubscribedYumRepos());	// getCurrentlySubscribedYumRepos() includes a yum transaction: "yum -q repolist --disableplugin=rhnplugin"	(NOT ANYMORE after bug 1008016)
		Assert.assertNotNull(yumRepoAfterUpdate, "Found yum repo ["+yumRepo.id+"] after we manually altered it and issued a yum transaction.");
		Assert.assertEquals(yumRepoAfterUpdate.exclude, yumRepo.exclude, "Yum repo ["+yumRepo.id+"] has persisted the manually added \"exclude\" option and its value.");
		Assert.assertEquals(yumRepoAfterUpdate.priority, yumRepo.priority, "Yum repo ["+yumRepo.id+"] has persisted the manually added \"priority\" option and its value.");
		Assert.assertEquals(yumRepoAfterUpdate, yumRepo, "Yum repo ["+yumRepo.id+"] has persisted all of its repository option values after running a yum transaction.");
		
		// also assert that the repository values persist even after refresh
		clienttasks.removeAllCerts(false,true,false);
		clienttasks.refresh(null,null,null);
		yumRepoAfterUpdate = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", yumRepo.id, clienttasks.getCurrentlySubscribedYumRepos());
		
		/* temporary idea to figure how why an sslclientcert would change; did not prove to be true; keeping for future
		BigInteger bi = clienttasks.getSerialNumberFromEntitlementCertFile(new File(yumRepoAfterUpdate.sslclientcert));
		SubscriptionPool sp = clienttasks.getSubscriptionPoolFromProductSubscription(ProductSubscription.findFirstInstanceWithMatchingFieldFromList("serialNumber", bi, clienttasks.getCurrentlyConsumedProductSubscriptions()), sm_clientUsername, sm_clientPassword);
		if (CandlepinTasks.isPoolAModifier(sm_clientUsername, sm_clientPassword, sp.poolId, sm_serverUrl)) {
			yumRepoAfterUpdate.sslclientcert = null;
			yumRepoAfterUpdate.sslclientkey = null;
			yumRepo.sslclientcert = null;
			yumRepo.sslclientkey = null;
			Assert.assertEquals(yumRepoAfterUpdate, yumRepo, "Discounting the sslclientcert and sslclientkey, yum repo ["+yumRepo.id+"] still persists all of its repository option values after running refresh and a yum transaction.");
		
		} else {
			Assert.assertEquals(yumRepoAfterUpdate, yumRepo, "Yum repo ["+yumRepo.id+"] still persists all of its repository option values after running refresh and a yum transaction.");
		}
		*/
		//Assert.assertEquals(yumRepoAfterUpdate, yumRepo, "Yum repo ["+yumRepo.id+"] still persists all of its repository option values after running refresh and a yum transaction.");
		yumRepoAfterUpdate.sslclientcert = null; yumRepo.sslclientcert = null; 
		yumRepoAfterUpdate.sslclientkey	= null; yumRepo.sslclientkey = null;
		Assert.assertEquals(yumRepoAfterUpdate, yumRepo, "Discounting changes to the sslclientcert and sslclientkey, yum repo ["+yumRepo.id+"] still persists all of its repository option values after running refresh and a yum transaction.");
		
		// also assert (when possible) that the repository values persist even after setting a release preference
		List<String> releases = clienttasks.getCurrentlyAvailableReleases(null,null,null);
		if (!releases.isEmpty()) {
			clienttasks.release(null, null, releases.get(randomGenerator.nextInt(releases.size())), null, null, null, null);
			yumRepoAfterUpdate = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", yumRepo.id, clienttasks.getCurrentlySubscribedYumRepos());
			Assert.assertNotNull(yumRepoAfterUpdate, "Found yum repo ["+yumRepo.id+"] after we manually altered it, set a release, and issued a yum transaction.");
			Assert.assertEquals(yumRepoAfterUpdate.exclude, yumRepo.exclude, "Yum repo ["+yumRepo.id+"] has persisted the manually added \"exclude\" option and its value even after setting a release and issuing a yum transaction.");
			Assert.assertEquals(yumRepoAfterUpdate.priority, yumRepo.priority, "Yum repo ["+yumRepo.id+"] has persisted the manually added \"priority\" option and its value even after setting a release and issuing a yum transaction.");
			// THIS WILL LIKELY NOT BE EQUAL WHEN THE yumRepoAfterUpdate.baseurl POINTS TO RHEL CONTENT SINCE IT WILL CONTAIN THE RELEASE PREFERENCE SUBSTITUTED FOR $releasever	//Assert.assertEquals(yumRepoAfterUpdate, yumRepo, "Yum repo ["+yumRepo.id+"] has persisted all of its repository option values even after setting a release and issuing a yum transaction.");
		}
	}
	@Test(	description="subscription-manager: add more yum repository options to redhat.repo and assert persistence using repo-override module.",
			groups={"AcceptanceTests","Tier1Tests","blockedByBug-845349","blockedByBug-834806","blockedByBug-803746","blockedByBug-1086316","blockedByBug-1069230"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void YumRepoListPreservesAdditionalOptionsToRedhatReposUsingRepoOverride_Test() throws JSONException, Exception {
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<YumRepo> subscribedYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		if (subscribedYumRepos.isEmpty()) throw new SkipException("There are no entitled yum repos available for this test.");
		
		// randomly choose one of the YumRepos, set new option values, and manually update the yum repo
		YumRepo yumRepo = subscribedYumRepos.get(randomGenerator.nextInt(subscribedYumRepos.size()));
		//yumRepo = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", "awesomeos-modifier",subscribedYumRepos);	// debugTesting case when yum repo comes from a modifier entitlement
		//yumRepo = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", "content-label-no-gpg",subscribedYumRepos);	// debugTesting case when sslclientcert appears to change.
		yumRepo.exclude = "my-test-pkg my-test-pkg-* my-test-pkgversion-?";
		yumRepo.priority = new Integer(10);
		if (false) {
			// previously supported manual edits to redhat.repos
			clienttasks.updateYumRepo(clienttasks.redhatRepoFile, yumRepo);
		} else {
			// newly supported process for adding redhat.repo edits
			Map<String,String> addNameValueMap = new HashMap<String,String>();
			addNameValueMap.put("exclude",  "my-test-pkg my-test-pkg-* my-test-pkgversion-?");
			addNameValueMap.put("priority",  "10");
			clienttasks.repo_override(null, null, yumRepo.id, null, addNameValueMap, null, null, null);			
		}
		
		// assert that the manually added repository options do not get clobbered by a new yum transaction
		client.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin");	// issue a new yum transaction
		YumRepo yumRepoAfterUpdate = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", yumRepo.id, clienttasks.getCurrentlySubscribedYumRepos());	// getCurrentlySubscribedYumRepos() includes a yum transaction: "yum -q repolist --disableplugin=rhnplugin"	(NOT ANYMORE after bug 1008016)
		Assert.assertNotNull(yumRepoAfterUpdate, "Found yum repo ["+yumRepo.id+"] after we manually altered it and issued a yum transaction.");
		Assert.assertEquals(yumRepoAfterUpdate.exclude, yumRepo.exclude, "Yum repo ["+yumRepo.id+"] has persisted the manually added \"exclude\" option and its value.");
		Assert.assertEquals(yumRepoAfterUpdate.priority, yumRepo.priority, "Yum repo ["+yumRepo.id+"] has persisted the manually added \"priority\" option and its value.");
		Assert.assertEquals(yumRepoAfterUpdate, yumRepo, "Yum repo ["+yumRepo.id+"] has persisted all of its repository option values after running a yum transaction.");
		
		// also assert that the repository values persist even after refresh
		clienttasks.removeAllCerts(false,true,false);
		clienttasks.refresh(null,null,null);
		yumRepoAfterUpdate = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", yumRepo.id, clienttasks.getCurrentlySubscribedYumRepos());
		
		/* temporary idea to figure how why an sslclientcert would change; did not prove to be true; keeping for future
		BigInteger bi = clienttasks.getSerialNumberFromEntitlementCertFile(new File(yumRepoAfterUpdate.sslclientcert));
		SubscriptionPool sp = clienttasks.getSubscriptionPoolFromProductSubscription(ProductSubscription.findFirstInstanceWithMatchingFieldFromList("serialNumber", bi, clienttasks.getCurrentlyConsumedProductSubscriptions()), sm_clientUsername, sm_clientPassword);
		if (CandlepinTasks.isPoolAModifier(sm_clientUsername, sm_clientPassword, sp.poolId, sm_serverUrl)) {
			yumRepoAfterUpdate.sslclientcert = null;
			yumRepoAfterUpdate.sslclientkey = null;
			yumRepo.sslclientcert = null;
			yumRepo.sslclientkey = null;
			Assert.assertEquals(yumRepoAfterUpdate, yumRepo, "Discounting the sslclientcert and sslclientkey, yum repo ["+yumRepo.id+"] still persists all of its repository option values after running refresh and a yum transaction.");
		
		} else {
			Assert.assertEquals(yumRepoAfterUpdate, yumRepo, "Yum repo ["+yumRepo.id+"] still persists all of its repository option values after running refresh and a yum transaction.");
		}
		*/
		//Assert.assertEquals(yumRepoAfterUpdate, yumRepo, "Yum repo ["+yumRepo.id+"] still persists all of its repository option values after running refresh and a yum transaction.");
		yumRepoAfterUpdate.sslclientcert = null; yumRepo.sslclientcert = null; 
		yumRepoAfterUpdate.sslclientkey	= null; yumRepo.sslclientkey = null;
		Assert.assertEquals(yumRepoAfterUpdate, yumRepo, "Discounting changes to the sslclientcert and sslclientkey, yum repo ["+yumRepo.id+"] still persists all of its repository option values after running refresh and a yum transaction.");
		
		// also assert (when possible) that the repository values persist even after setting a release preference
		List<String> releases = clienttasks.getCurrentlyAvailableReleases(null,null,null);
		if (!releases.isEmpty()) {
			clienttasks.release(null, null, releases.get(randomGenerator.nextInt(releases.size())), null, null, null, null);
			yumRepoAfterUpdate = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", yumRepo.id, clienttasks.getCurrentlySubscribedYumRepos());
			Assert.assertNotNull(yumRepoAfterUpdate, "Found yum repo ["+yumRepo.id+"] after we manually altered it, set a release, and issued a yum transaction.");
			Assert.assertEquals(yumRepoAfterUpdate.exclude, yumRepo.exclude, "Yum repo ["+yumRepo.id+"] has persisted the manually added \"exclude\" option and its value even after setting a release and issuing a yum transaction.");
			Assert.assertEquals(yumRepoAfterUpdate.priority, yumRepo.priority, "Yum repo ["+yumRepo.id+"] has persisted the manually added \"priority\" option and its value even after setting a release and issuing a yum transaction.");
			// THIS WILL LIKELY NOT BE EQUAL WHEN THE yumRepoAfterUpdate.baseurl POINTS TO RHEL CONTENT SINCE IT WILL CONTAIN THE RELEASE PREFERENCE SUBSTITUTED FOR $releasever	//Assert.assertEquals(yumRepoAfterUpdate, yumRepo, "Yum repo ["+yumRepo.id+"] has persisted all of its repository option values even after setting a release and issuing a yum transaction.");
		}
	}
	@Test(	description="subscription-manager: verify that overrides take precedence to manually edited repository options in redhat.repo and assert persistence.",
			groups={/*UNCOMMENT FOR RHEL71"blockedByBug-1098891",*/"blockedByBug-1101571","blockedByBug-1101584"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void YumRepoListPreservesAdditionalOptionsToRedhatReposUsingManualEditsButRepoOverridesTakePrecedence_Test() throws JSONException, Exception {
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<YumRepo> subscribedYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		if (subscribedYumRepos.isEmpty()) throw new SkipException("There are no entitled yum repos available for this test.");
		
		// choose the first disabled yum repo for this test
		YumRepo yumRepo = YumRepo.findFirstInstanceWithMatchingFieldFromList("enabled", false, subscribedYumRepos);
		if (yumRepo == null) throw new SkipException("Could not find a disabled yum repo available for this test.");
		
		// manually enable the yum repo
		yumRepo.enabled = true;
		clienttasks.updateYumRepo(clienttasks.redhatRepoFile, yumRepo);
		// assert that the manually added repository options do not get clobbered by a new yum transaction
		client.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin");	// issue a new yum transaction
		YumRepo yumRepoAfterUpdate = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", yumRepo.id, clienttasks.getCurrentlySubscribedYumRepos());	// getCurrentlySubscribedYumRepos() includes a yum transaction: "yum -q repolist --disableplugin=rhnplugin"	(NOT ANYMORE after bug 1008016)
		Assert.assertNotNull(yumRepoAfterUpdate, "Found yum repo ["+yumRepo.id+"] in '"+clienttasks.redhatRepoFile+"'.");
		Assert.assertTrue(yumRepoAfterUpdate.enabled, "Yum repo ["+yumRepo.id+"] has persisted the manual enablement.");
		
		// use subscription-manager repos to disable the repo
		clienttasks.repos(null, null, null, null, yumRepo.id, null, null, null);
		// assert that the yum repo was disabled and a repo-override was created that takes precedence over the manually enabled yum repo
		client.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin");	// issue a new yum transaction
		yumRepoAfterUpdate = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", yumRepo.id, clienttasks.getCurrentlySubscribedYumRepos());	// getCurrentlySubscribedYumRepos() includes a yum transaction: "yum -q repolist --disableplugin=rhnplugin"	(NOT ANYMORE after bug 1008016)
		Assert.assertNotNull(yumRepoAfterUpdate, "Found yum repo ["+yumRepo.id+"] in '"+clienttasks.redhatRepoFile+"'.");
		Assert.assertFalse(yumRepoAfterUpdate.enabled, "Yum repo ["+yumRepo.id+"] has persisted the disablement by subscription-manager.");
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(clienttasks.repo_override(true,null,(String)null,(String)null,null,null,null,null).getStdout(), String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,yumRepo.id,"enabled","0")),"After a calling subscription-manager repos --enable, the subscription-manager repo-override list includes an override for repo='"+yumRepo.id+"' name='"+"enabled"+"' value='"+"0"+"'.");
		
		// now attempt to manually re-enable yum repo
		clienttasks.updateYumRepo(clienttasks.redhatRepoFile, yumRepo);
		// assert that the attempt to manually re-enable the yum repo fails since the subscription-manager repo override takes precedence over manual edits.
		client.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin");	// issue a new yum transaction
		yumRepoAfterUpdate = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", yumRepo.id, clienttasks.getCurrentlySubscribedYumRepos());	// getCurrentlySubscribedYumRepos() includes a yum transaction: "yum -q repolist --disableplugin=rhnplugin"	(NOT ANYMORE after bug 1008016)
		Assert.assertNotNull(yumRepoAfterUpdate, "Found yum repo ["+yumRepo.id+"] in '"+clienttasks.redhatRepoFile+"'.");
		Assert.assertFalse(yumRepoAfterUpdate.enabled, "Yum repo ["+yumRepo.id+"] is still disabled after an attempt to manually enable it (because repo-override should take precedence).");
	}
	
	
	@Test(	description="Verify that the redhat.repo file is refreshed with changes to the entitlements (yum transactions are no longer required to update the redhat.repo)",
			enabled=true,
			groups={"AcceptanceTests","Tier1Tests","blockedByBug-1008016","blockedByBug-1090206","blockedByBug-1034429"})	// TODO: review all tests and tasks that issue yum transactions simply to re-populate the redhat.repo
	//@ImplementsNitrateTest(caseId=)
	public void VerifyYumTransactionsAreNoLongerRequiredToTriggerUpdatesToRedhatRepo_Test() {
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null);	// shut off auto-healing
		List<ProductCert> productCerts = clienttasks.getCurrentProductCerts();

		// Subscribe to a randomly available pool...
		log.info("Subscribe to a randomly available pool...");
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		if (pools.size()<=0) throw new SkipException("No susbcriptions were available which blocks this test from executing.");
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool

		// testing the subscribe case...
		clienttasks.subscribe_(null, null, pool.poolId, null, null, null, null, null, null, null, null);
		log.info("Immediately after attaching a subscription, we will now assert that all of the content repos from the currently attached entitlements are currently present in the redhat.repo without having triggered a yum transaction...");
		verifyCurrentEntitlementCertsAreReflectedInCurrentlySubscribedYumRepos(productCerts);
		
		// testing the unsubscribe case...
		clienttasks.unsubscribe_(true,(BigInteger)null,null,null,null);
		log.info("Immediately after removing a subscription, we will now assert that the redhat.repo is empty without having triggered a yum transaction...");
		verifyCurrentEntitlementCertsAreReflectedInCurrentlySubscribedYumRepos(productCerts);
		
		// testing the autosubscribe case...
		clienttasks.subscribe_(true, null, (String)null, null, null, null, null, null, null, null, null);
		log.info("Immediately after autoattaching subscription(s), we will now assert that all of the content repos from the currently attached entitlements are currently present in the redhat.repo without having triggered a yum transaction...");
		verifyCurrentEntitlementCertsAreReflectedInCurrentlySubscribedYumRepos(productCerts);
		
		// testing the unregister case...
		clienttasks.unregister_(null, null, null);
		log.info("Immediately after unregistering, we will now assert that the redhat.repo is empty without having triggered a yum transaction...");
		verifyCurrentEntitlementCertsAreReflectedInCurrentlySubscribedYumRepos(productCerts);
	}
	
	
	@Test(	description="Verify that redhat.repo file is unnecessarily re-written with every yum transaction",
			groups={"blockedByBug-1035440"/*,"blockedByBug-1090206","blockedByBug-1008016"*//*UNCOMMENT DURING RHEL71 CYCLE,"blockedByBug-1104731"*/,"blockedByBug-1104777"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyYumTransactionsDoNotCauseUnnessarilyRewritesOfRedHatRepoFile_Test() {
		clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, null, null, null, null);
		clienttasks.autoheal_(null, null, true, null, null, null);
		List<Repo> repos = clienttasks.getCurrentlySubscribedRepos();
		//if (repos.isEmpty()) clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		if (repos.isEmpty()) Assert.fail("Expected subscription-manager registration with autosubscribe to grant access to at least one repository.  If failed, check installed product certs and available subscriptions.");
		List<String> defaultEnabledRepos = new ArrayList<String>();
		List<String> defaultDisabledRepos = new ArrayList<String>();
		for (Repo repo : repos) if (repo.enabled) defaultEnabledRepos.add(repo.repoId); else defaultDisabledRepos.add(repo.repoId);
		
		// get the initial modification time for the redhat.repo file
		String initialModTime = client.runCommandAndWait("stat -c %y "+clienttasks.redhatRepoFile).getStdout().trim();
		
		// trigger the subscription-manager yum plugin and assert the modification time remains unchanged
		clienttasks.yumClean("all");
		Assert.assertEquals(client.runCommandAndWait("stat -c %y "+clienttasks.redhatRepoFile).getStdout().trim(), initialModTime, "The modification time of '"+clienttasks.redhatRepoFile+"' before and after a yum transaction.");
		clienttasks.getYumRepolist("all -q");
		Assert.assertEquals(client.runCommandAndWait("stat -c %y "+clienttasks.redhatRepoFile).getStdout().trim(), initialModTime, "The modification time of '"+clienttasks.redhatRepoFile+"' before and after a yum transaction.");
		
		// toggle the enablement of repos which should force an immediate modification to redhat.repo
		clienttasks.repos(null, null, null, defaultDisabledRepos, defaultEnabledRepos, null, null, null);
		String finalModTime = client.runCommandAndWait("stat -c %y "+clienttasks.redhatRepoFile).getStdout().trim();
		Assert.assertTrue(!finalModTime.equals(initialModTime), "The modification time of '"+clienttasks.redhatRepoFile+"' before and after repos enablement should be different.");
		
		// finally let's trigger the subscription-manager yum plugin and assert the modification time remains unchanged
		clienttasks.getYumRepolist("all -q");
		Assert.assertEquals(client.runCommandAndWait("stat -c %y "+clienttasks.redhatRepoFile).getStdout().trim(), finalModTime, "The modification time of '"+clienttasks.redhatRepoFile+"' before and after a yum transaction.");
		clienttasks.yumClean("all");
		Assert.assertEquals(client.runCommandAndWait("stat -c %y "+clienttasks.redhatRepoFile).getStdout().trim(), finalModTime, "The modification time of '"+clienttasks.redhatRepoFile+"' before and after a yum transaction.");
	}
	
	
	@Test(	description="subscription-manager: subscribe to a pool and verify that repos list --list-enabled reports only the enabled repos from the newly entitled content namespaces",
			groups={"blockedByBug-1119648"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposListEnabledReportsOnlyEnabledContentNamespaces_Test() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.4-1")) throw new SkipException("The repos --list-enabled function was not implemented until version subscription-manager-1.13.4-1");
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<YumRepo> subscribedYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		if (subscribedYumRepos.isEmpty()) throw new SkipException("There are no entitled yum repos available for this test.");
		List<YumRepo> enabledYumRepos = YumRepo.findAllInstancesWithMatchingFieldFromList("enabled", Boolean.TRUE, subscribedYumRepos);
		
		// get the list of currently enabled repos
		SSHCommandResult sshCommandResult = clienttasks.repos(null, true, null, (String)null,(String)null,null, null, null);
		List<Repo> listEnabledRepos = Repo.parse(sshCommandResult.getStdout());
		
		// verify that the enabledYumRepos are all present in the listEnabledRepos
		for (YumRepo yumRepo : enabledYumRepos) {
			Repo enabledRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", yumRepo.id, listEnabledRepos);
			Assert.assertNotNull(enabledRepo, "Enabled yum repo ["+yumRepo.id+"] is included in the report of repos --list-enabled.");
		}
		Assert.assertEquals(listEnabledRepos.size(), enabledYumRepos.size(),"The number of --list-enabled repos matches the number of enabled yum repos in '"+clienttasks.redhatRepoFile+"'.");
	}
	
	
	@Test(	description="subscription-manager: subscribe to a pool and verify that repos list --list-disabled reports only the disabled repos from the newly entitled content namespaces",
			groups={"blockedByBug-1119648"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReposListDisabledReportsOnlyDisabledContentNamespaces_Test() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.4-1")) throw new SkipException("The repos --list-disabled function was not implemented until version subscription-manager-1.13.4-1");
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<YumRepo> subscribedYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		if (subscribedYumRepos.isEmpty()) throw new SkipException("There are no entitled yum repos available for this test.");
		List<YumRepo> disabledYumRepos = YumRepo.findAllInstancesWithMatchingFieldFromList("enabled", Boolean.FALSE, subscribedYumRepos);
		
		// get the list of currently disabled repos
		SSHCommandResult sshCommandResult = clienttasks.repos(null, null, true, (String)null,(String)null,null, null, null);
		List<Repo> listDisabledRepos = Repo.parse(sshCommandResult.getStdout());
		
		// verify that the disabledYumRepos are all present in the listDisabledRepos
		for (YumRepo yumRepo : disabledYumRepos) {
			Repo disabledRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", yumRepo.id, listDisabledRepos);
			Assert.assertNotNull(disabledRepo, "Disabled yum repo ["+yumRepo.id+"] is included in the report of repos --list-disabled.");
		}
		Assert.assertEquals(listDisabledRepos.size(), disabledYumRepos.size(),"The number of --list-disabled repos matches the number of disabled yum repos in '"+clienttasks.redhatRepoFile+"'.");
	}
	
	
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 797243 - manual changes to redhat.repo are too sticky https://github.com/RedHatQE/rhsm-qe/issues/188
	// TODO	Bug 846207 - multiple specifications of --enable doesnot throw error when repo id is invalid https://github.com/RedHatQE/rhsm-qe/issues/189
	// TODO Bug 886604 - etc/yum.repos.d/ does not exist, turning manage_repos off. https://github.com/RedHatQE/rhsm-qe/issues/190
	//      Bug 886992 - redhat.repo is not being created https://github.com/RedHatQE/rhsm-qe/issues/191
	
	
	
	
	
	@BeforeClass(groups={"setup"})
	public void setupBeforeClass() throws JSONException, Exception {
		currentProductCerts = clienttasks.getCurrentProductCerts();
		modifierSubscriptionData = getModifierSubscriptionDataAsListOfLists(null);
	}
	
	@BeforeGroups(groups={"setup"}, value={"unsubscribeAllBeforeThisTest"})
	public void unsubscribeAllBeforeGroups() {
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
	}
	
	@AfterMethod(groups={"setup"})
	//@AfterGroups(groups={"setup"}, value={"ManageReposTests"})	// did not work the way I wanted it to.  Other @Tests were getting launched during ManageReposTests
	public void setManageReposConfiguration() {
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "manage_repos", "1");
	}

	
	
	// Protected methods ***********************************************************************

	List<ProductCert> currentProductCerts=new ArrayList<ProductCert>();
	List<List<Object>> modifierSubscriptionData = null;

	/**
	 * @param yumRepo
	 * @param manually - if true, then toggling the enabled flag in yumRepo is performed manually using sed, otherwise subscription-manager repos --enable or --disable option is used
	 */
	protected void verifyTogglingTheEnablementOfRedhatRepo(YumRepo yumRepo, boolean manually){

		Repo repo = new Repo(yumRepo.name,yumRepo.id,yumRepo.baseurl,yumRepo.enabled);

		// assert that the yumRepo is reported in the subscription-manager repos
		List<Repo> currentlySubscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		Assert.assertTrue(currentlySubscribedRepos.contains(repo),"The yumRepo '"+yumRepo+"' is represented in the subscription-manager repos --list by: "+repo);
		
		// also verify that yumRepo is reported in the yum repolist
		Assert.assertTrue(clienttasks.getYumRepolist(yumRepo.enabled?"enabled":"disabled").contains(yumRepo.id), "yum repolist properly reports the enablement of yumRepo id '"+yumRepo.id+"' before manually changing its enabled value.");
	
		// edit the redhat.repo and change the enabled parameter for this yumRepo
		Boolean newEnabledValue = yumRepo.enabled? false:true;	// toggle the value
		if (manually) {	// toggle the enabled flag manually
			clienttasks.updateYumRepoParameter(clienttasks.redhatRepoFile,yumRepo.id,"enabled",newEnabledValue.toString());
		} else if (newEnabledValue) {	// toggle the enabled flag using subscription-manager repos --enable
			clienttasks.repos(null, null, null, yumRepo.id, null, null, null, null);
		} else {	// toggle the enabled flag using subscription-manager repos --disable
			clienttasks.repos(null, null, null, null, yumRepo.id, null, null, null);
		}
		Repo newRepo = new Repo(yumRepo.name,yumRepo.id,yumRepo.baseurl,newEnabledValue);

		// verify that the change is preserved by subscription-manager repos --list
		currentlySubscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		Assert.assertTrue(currentlySubscribedRepos.contains(newRepo),"yumRepo id '"+yumRepo.id+"' was manually changed to enabled="+newEnabledValue+" and the subscription-manager repos --list reflects the change as: "+newRepo);
		Assert.assertFalse(currentlySubscribedRepos.contains(repo),"The original repo ("+repo+") is no longer found in subscription-manager repos --list.");
		
		// also verify the change is reflected in yum repolist
		Assert.assertTrue(clienttasks.getYumRepolist(newEnabledValue?"enabled":"disabled").contains(yumRepo.id), "yum repolist properly reports the enablement of yumRepo id '"+yumRepo.id+"' which was manually changed to '"+newEnabledValue+"'.");
		Assert.assertFalse(clienttasks.getYumRepolist(!newEnabledValue?"enabled":"disabled").contains(yumRepo.id), "yum repolist properly reports the enablement of yumRepo id '"+yumRepo.id+"' which was manually changed to '"+newEnabledValue+"'.");
	
		// now remove and refresh entitlement certificates and again assert the manual edits are preserved
		log.info("Remove and refresh entitlement certs...");
		clienttasks.removeAllCerts(false,true, false);
		clienttasks.refresh(null, null, null);
		
		// verify that the change is preserved by subscription-manager repos --list (even after deleting and refreshing entitlement certs)
		currentlySubscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		Assert.assertTrue(currentlySubscribedRepos.contains(newRepo),"yumRepo id '"+yumRepo.id+"' was manually changed to enabled="+newEnabledValue+" and even after deleting and refreshing certs, the subscription-manager repos --list reflects the change as: "+newRepo);
		Assert.assertFalse(currentlySubscribedRepos.contains(repo),"The original repo ("+repo+") is no longer found in subscription-manager repos --list.");
		
		// also verify the change is reflected in yum repolist (even after deleting and refreshing entitlement certs)
		Assert.assertTrue(clienttasks.getYumRepolist(newEnabledValue?"enabled":"disabled").contains(yumRepo.id), "even after deleting and refreshing certs, yum repolist properly reports the enablement of yumRepo id '"+yumRepo.id+"' which was manually changed to '"+newEnabledValue+"'.");
		Assert.assertFalse(clienttasks.getYumRepolist(!newEnabledValue?"enabled":"disabled").contains(yumRepo.id), "even after deleting and refreshing certs, yum repolist properly reports the enablement of yumRepo id '"+yumRepo.id+"' which was manually changed to '"+newEnabledValue+"'.");
	}

	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="getYumReposData")
	public Object[][] getYumReposDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getYumReposDataAsListOfLists());
	}
	protected List<List<Object>> getYumReposDataAsListOfLists() throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		
		// subscribe to all available subscription so as to populate the redhat.repo file
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		
		clienttasks.getYumRepolist("all");	// trigger a yum transaction so that subscription-manager plugin will refresh redhat.repo
		for (YumRepo yumRepo : clienttasks.getCurrentlySubscribedYumRepos()) {
			ll.add(Arrays.asList(new Object[]{yumRepo}));
			
			// minimize the number of dataProvided rows (useful during automated testcase development)
			if (Boolean.valueOf(getProperty("sm.debug.dataProviders.minimize","false"))) break;
		}

		return ll;
	}

	
	/**
	 * @return a random subset of data rows from getYumReposDataAsListOfLists() (maximum of 3 rows) (useful to help reduce excessive test execution time)
	 * @throws Exception
	 */
	@DataProvider(name="getRandomSubsetOfYumReposData")
	public Object[][] getRandomSubsetYumReposDataAs2dArray() throws Exception {
		int subMax = 3;	// maximum subset count of data rows to return
		List<List<Object>> allData = getYumReposDataAsListOfLists();
		if (allData.size() <= subMax) return TestNGUtils.convertListOfListsTo2dArray(allData);
		List<List<Object>> subData = new ArrayList<List<Object>>();
		for (int i = 0; i < subMax; i++) subData.add(allData.remove(randomGenerator.nextInt(allData.size())));
		return TestNGUtils.convertListOfListsTo2dArray(subData);
	}
}
