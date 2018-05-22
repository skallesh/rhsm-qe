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
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.ProductCert;
import rhsm.data.Repo;
import rhsm.data.SubscriptionPool;
import rhsm.data.YumRepo;

import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

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
@Test(groups={"ReposTests"})
public class ReposTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19972", "RHEL7-51011"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: subscribe to a pool and verify that the newly entitled content namespaces are represented in the repos list",
			groups={"Tier1Tests","blockedByBug-807407","blockedByBug-962520","blockedByBug-1034649"},
			//dataProvider="getAvailableSubscriptionPoolsData",	// very thorough, but takes too long to execute and rarely finds more bugs
			dataProvider="getRandomSubsetOfAvailableSubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposListReportsGrantedContentNamespacesAfterSubscribingToPool(SubscriptionPool pool) throws JSONException, Exception {

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
				
				// TEMPORARY WORKAROUND FOR Bug 1246636 - an expected entitled content set is not reflected in subscription-manager repos --list
				if (expectedRepo.repoName.equals("content") && !actualRepos.contains(expectedRepo)) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="1246636"; 
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
					if (invokeWorkaroundWhileBugIsOpen) {
						log .warning("The newly entitled contentNamespace '"+contentNamespace+"' is NOT represented in the subscription-manager repos --list by: "+expectedRepo);
						continue;
					}
				}
				// END OF WORKAROUND
				
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


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20295", "RHEL7-51691"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: subscribe to a future pool and verify that NO content namespaces are represented in the repos list",
			groups={"Tier3Tests","blockedByBug-768983","blockedByBug-1440180","testReposListReportsNoContentNamespacesAfterSubscribingToFuturePool"},
			dataProvider="getAllFutureSystemSubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposListReportsNoContentNamespacesAfterSubscribingToFuturePool(SubscriptionPool pool) throws Exception {
//if (!pool.productId.equals("awesomeos-virt-unlmtd-phys")) throw new SkipException("debugTesting productId="+pool.productId);
		
		// subscribe to the future SubscriptionPool
		SSHCommandResult subscribeResult = clienttasks.subscribe(null,null,pool.poolId,null,null,null,null,null,null,null, null, null, null);
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
	@BeforeGroups(groups={"setup"}, value={"testReposListReportsNoContentNamespacesAfterSubscribingToFuturePool"})
	public void unsubscribeAllBeforeGroups() {
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20397", "RHEL7-51690"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: after subscribing to all pools, verify that manual edits to enable repos in redhat.repo are preserved.",
			groups={"Tier3Tests","blockedByBug-905546","blockedByBug-1098891","blockedByBug-1101571","blockedByBug-1101584"},
			dataProvider="getRandomSubsetOfYumReposData",	// dataProvider="getYumReposData", takes too long to execute
			enabled=true)	// with the implementation of RFE Bug 803746, manual edits to the enablement of redhat repos is now forbidden.  This test is being disabled in favor of ManualEditsToEnablementOfRedhatReposIsForbidden_Test
	//@ImplementsNitrateTest(caseId=)
	public void testReposListPreservesManualEditsToEnablementOfRedhatRepos(YumRepo yumRepo){
		verifyTogglingTheEnablementOfRedhatRepo(yumRepo,true);
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20379", "RHEL7-51674"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: verify that manual edits to enable repos in redhat.repo are forbidden by documented warning.",
			groups={"Tier3Tests","blockedByBug-1032243"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testManualEditsToEnablementOfRedhatReposIsDiscouraged(){	// replacement for ReposListPreservesManualEditsToEnablementOfRedhatRepos_Test
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
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


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20386", "RHEL7-51689"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: after subscribing to all pools, verify that edits (using subscription-manager --enable --disable options) to repos in redhat.repo are preserved.",
			groups={"Tier3Tests","blockedByBug-905546"},
			dataProvider="getRandomSubsetOfYumReposData",	// dataProvider="getYumReposData", takes too long to execute
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposListPreservesEnablementOfRedhatRepos(YumRepo yumRepo){
		verifyTogglingTheEnablementOfRedhatRepo(yumRepo,false);
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19971", "RHEL7-51010"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: after subscribing to all pools, verify that edits (using subscription-manager --enable --disable options specified multiple times in a single call) to repos in redhat.repo are preserved.",
			groups={"Tier1Tests","blockedByBug-843915","blockedByBug-962520","blockedByBug-1034649","blockedByBug-1121272","blockedByBug-1366301"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposListPreservesSimultaneousEnablementOfRedhatRepos(){
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
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
		clienttasks.repos(null, null, null, enableRepoIds, disableRepoIds, null, null, null, null);
		
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
		clienttasks.refresh(null, null, null, null);
		
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


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20396", "RHEL7-51687"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: repos --list reports no entitlements when not registered",
			groups={"Tier3Tests","blockedByBug-724809","blockedByBug-807360","blockedByBug-837447"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposListIsEmptyWhenNotRegistered(){
		clienttasks.unregister(null,null,null, null);		
		Assert.assertEquals(clienttasks.getCurrentlySubscribedRepos().size(),0, "No repos are reported by subscription-manager repos --list when not registered.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20371", "RHEL7-51694"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: repos --list reports no entitlements when not registered",
			groups={"Tier3Tests","blockedByBug-837447"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposListWhenNotRegistered(){
		clienttasks.unregister(null,null,null, null);
		SSHCommandResult result = clienttasks.repos(true, null, null, (String)null, (String)null, null, null, null, null);
		//Assert.assertEquals(result.getStdout().trim(), "The system is not entitled to use any repositories.");
		Assert.assertEquals(result.getStdout().trim(), "This system has no repositories available through subscriptions.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20366", "RHEL7-51692"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: repos --list should not fail when config rhsm.manage_repos is blank.",
			groups={"Tier3Tests","ReposListWhenManageReposIsBlank_Test","blockedByBug-1251853"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposListWhenManageReposIsBlank(){
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		// config rhsm.manage_repos to a blank value
		//clienttasks.config(null, null, true, new String[]{"rhsm", "manage_repos", ""});
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "manage_repos", "");
		
		SSHCommandResult result = clienttasks.repos(true, null, null, (String)null, (String)null, null, null, null, null);
		
		// as discussed in https://bugzilla.redhat.com/show_bug.cgi?id=1251853#c12
		// when rhsm.manage_repos is blank, behavior should assume the default value [1] which means repos will be managed
		Assert.assertEquals(result.getStdout().trim(), "This system has no repositories available through subscriptions.", "Stdout when calling repos with rhsm.manage_repos configured to nothing.  Behavior should default to managed behavior (as if set to [1]).");
		Assert.assertEquals(result.getStderr().trim(), "", "Stderr when calling repos with rhsm.manage_repos configured to nothing.");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0), "ExitCode when calling repos with rhsm.manage_repos configured to nothing.");
	}
	@AfterGroups(groups={"setup"}, value={"ReposListWhenManageReposIsBlank_Test","ReposListWhenManageReposIsOff_Test"})
	public void restoreRhsmManageReposAfterGroups() {
		//clienttasks.config(null, null, true, new String[]{"rhsm", "manage_repos", "1"});
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "manage_repos", "1");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20365", "RHEL7-51693"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: repos --list should provide feedback when config rhsm.manage_repos is off.",
			groups={"Tier3Tests","ReposListWhenManageReposIsOff_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposListWhenManageReposIsOff(){
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		// config rhsm.manage_repos to an off value
		//clienttasks.config(null, null, true, new String[]{"rhsm", "manage_repos", "0"});
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "manage_repos", "0");
		
		SSHCommandResult result = clienttasks.repos(true, null, null, (String)null, (String)null, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "Repositories disabled by configuration.", "Stdout when calling repos with rhsm.manage_repos configured to 0.");
		Assert.assertEquals(result.getStderr().trim(), "", "Stderr when calling repos with rhsm.manage_repos configured to 0.");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0), "ExitCode when calling repos with rhsm.manage_repos configured to 0.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20378", "RHEL7-51695"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: repos (without any options) reports no entitlements when not registered (rhel63 and rhel58 previously reported 'Error: No options provided. Please see the help comand.')",
			groups={"Tier3Tests","blockedByBug-837447"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposWhenNotRegistered(){
		clienttasks.unregister(null,null,null, null);
		SSHCommandResult result = clienttasks.repos(null, null, null, (String)null, (String)null, null, null, null, null); // defaults to --list behavior starting in rhel59
		//Assert.assertEquals(result.getStdout().trim(), "The system is not entitled to use any repositories.");
		Assert.assertEquals(result.getStdout().trim(), "This system has no repositories available through subscriptions.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19970", "RHEL7-51009"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: set manage_repos to 0 and assert redhat.repo is removed.",
			groups={"Tier1Tests","ManageReposTests","blockedByBug-767620","blockedByBug-797996","blockedByBug-895462","blockedByBug-1034649"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposListIsDisabledByConfigurationAfterRhsmManageReposIsConfiguredOff() throws JSONException, Exception{
		
		// TEMPORARY WORKAROUND FOR BUG
		if (clienttasks.redhatReleaseX.equals("8")) {
			String bugId = "1581445";	// Bug 1581445 - rhsm configuration manage_repos is not working on RHEL8
			boolean invokeWorkaroundWhileBugIsOpen = true;
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				// TODO Determine if this is also be blocked on RHEL7.6
				throw new SkipException("This test is blockedByBug-"+bugId+" on RHEL8");
			}
		}
		// END OF WORKAROUND
		
		// manually set the manage_repos to 1
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "manage_repos", "1");
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		// assert that the repos list is enabled.
		//Assert.assertTrue(clienttasks.repos(true,(String)null,(String)null,null,null,null).getStdout().trim().equals("The system is not entitled to use any repositories."), "The system is not entitled to use any repositories while the rhsm.manage_repos configuration value is 1.");
		Assert.assertEquals(clienttasks.repos(true,null,null,(String)null,(String)null,null, null, null, null).getStdout().trim(),"This system has no repositories available through subscriptions.", "This system has no repositories available through subscriptions while the rhsm.manage_repos configuration value is 1.");

		// assert that the redhat.repo exists before and after a yum transaction
		//Assert.assertEquals(RemoteFileTasks.testFileExists(client, clienttasks.redhatRepoFile),1,"When rhsm.manage_repos is configured on, the redhat.repo should exist after registration.");
		clienttasks.getYumRepolist(null);
		Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"When rhsm.manage_repos configuration value is non-zero, the redhat.repo should exist after yum transaction.");

		// assert that the repos list is not entitled to use any repositories, but is enabled by configuration!
		//Assert.assertTrue(clienttasks.repos(true,(String)null,(String)null,null,null,null).getStdout().trim().equals("The system is not entitled to use any repositories."), "The system is not entitled to use any repositories while the rhsm.manage_repos configuration value is 1.");
		Assert.assertEquals(clienttasks.repos(true,null,null,(String)null,(String)null,null, null, null, null).getStdout().trim(),"This system has no repositories available through subscriptions.", "This system has no repositories available through subscriptions while the rhsm.manage_repos configuration value is 1.");

		// NOW DISABLE THE rhsm.manage_repos CONFIGURATION FILE PARAMETER
		clienttasks.config(null, null, true, new String[]{"rhsm","manage_repos","0"});
		
		// assert that the repos list is disabled by configuration!
		SSHCommandResult reposListResult = clienttasks.repos(true,null,null,(String)null,(String)null,null, null, null, null);
		//Assert.assertTrue(reposListResult.getStdout().contains("Repositories disabled by configuration."), "Repositories disabled by configuration since the rhsm.manage_repos configuration value is 0.");
		Assert.assertEquals(reposListResult.getStdout().trim(),"Repositories disabled by configuration.", "Repositories disabled by configuration since the rhsm.manage_repos configuration value is 0.");
		//Assert.assertTrue(reposListResult.getStdout().contains("The system is not entitled to use any repositories."), "The system is not entitled to use any repositories since the rhsm.manage_repos configuration value is 0.");
		//Assert.assertTrue(reposListResult.getStdout().contains("This system has no repositories available through subscriptions."), "This system has no repositories available through subscriptions since the rhsm.manage_repos configuration value is 0.");

		// trigger a yum transaction and assert that the redhat.repo no longer exists
		clienttasks.getYumRepolist(null);
		Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"When rhsm.manage_repos configuration value is 0, the redhat.repo file should not exist anymore.");
		
		// subscribe to all subscriptions and re-assert that the repos list remains disabled.
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		reposListResult = clienttasks.repos(true,null,null,(String)null,(String)null,null, null, null, null);
		//Assert.assertTrue(reposListResult.getStdout().contains("Repositories disabled by configuration."), "Repositories disabled by configuration even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		Assert.assertEquals(reposListResult.getStdout().trim(),"Repositories disabled by configuration.", "Repositories disabled by configuration even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		//Assert.assertTrue(reposListResult.getStdout().contains("The system is not entitled to use any repositories."), "The system is not entitled to use any repositories even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		//Assert.assertTrue(reposListResult.getStdout().contains("This system has no repositories available through subscriptions."), "This system has no repositories available through subscriptions even after subscribing to all pools while the rhsm.manage_repos configuration value is 0.");
		Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"Even after subscribing to all subscription pools while the rhsm.manage_repos configuration value is 0, the redhat.repo is not generated.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20377", "RHEL7-51688"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: set manage_repos to 1 and assert redhat.repo is restored.",
			groups={"Tier3Tests","blockedByBug-767620","blockedByBug-797996","blockedByBug-895462","ManageReposTests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposListIsEnabledByConfigurationAfterRhsmManageReposIsConfiguredOn() throws JSONException, Exception{
		
		// manually set the manage_repos to 0
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "manage_repos", "0");
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);

		// assert that the repos list is disabled.
		SSHCommandResult reposListResult = clienttasks.repos(true,null,null,(String)null,(String)null,null, null, null, null);
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
		reposListResult = clienttasks.repos(true,null,null,(String)null,(String)null,null, null, null, null);
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


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20375", "RHEL7-51681"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: attempt to enable an invalid repo id",
			groups={"Tier3Tests","blockedByBug-846207"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposEnableInvalidRepo(){
		String invalidRepo = "invalid-repo-id";
		SSHCommandResult result = clienttasks.repos_(null, null, null, invalidRepo, null, null, null, null, null);
		//Assert.assertEquals(result.getExitCode(), new Integer(0), "ExitCode from an attempt to enable an invalid-repo-id.");	// valid in RHEL59
		//Assert.assertEquals(result.getStdout().trim(), "Error: A valid repo id is required. Use --list option to see valid repos.", "Stdout from an attempt to enable an invalid-repo-id.");	// valid in RHEL59
		Assert.assertEquals(result.getExitCode(), new Integer(1), "ExitCode from an attempt to enable an invalid-repo-id.");
		//Assert.assertEquals(result.getStdout().trim(), String.format("Error: %s is not a valid repo id. Use --list option to see valid repos.",invalidRepo), "Stdout from an attempt to enable an invalid-repo-id.");	// changed by bug 878634
		String expectedStdout = String.format("Error: %s is not a valid repo ID. Use --list option to see valid repos.",invalidRepo);
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.6-1")) expectedStdout = String.format("Error: %s is not a valid repository ID. Use --list option to see valid repositories.",invalidRepo);	// bug 1122530 commit add5a9b746f9f2af147a7e4622b897a46b5ef132
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.10-1")) expectedStdout = String.format("Error: '%s' does not match a valid repository ID. Use \"subscription-manager repos --list\" to see valid repositories.",invalidRepo);	// bug 1351009 subscription-manager RHEL7.3 commit eaa748187bee110a19184864b1775705b653f629
		Assert.assertEquals(result.getStdout().trim(), expectedStdout, "Stdout from an attempt to enable an invalid-repo-id.");
		Assert.assertEquals(result.getStderr().trim(), "", "Stderr from an attempt to enable an invalid-repo-id.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20372", "RHEL7-51675"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: attempt to disable an invalid repo id",
			groups={"Tier3Tests","blockedByBug-846207"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposDisableInvalidRepo(){
		String invalidRepo = "invalid-repo-id";
		SSHCommandResult result = clienttasks.repos_(null, null, null, null, invalidRepo, null, null, null, null);
		//Assert.assertEquals(result.getExitCode(), new Integer(0), "ExitCode from an attempt to disable an invalid-repo-id.");	// valid in RHEL59
		//Assert.assertEquals(result.getStdout().trim(), "Error: A valid repo id is required. Use --list option to see valid repos.", "Stdout from an attempt to disable an invalid-repo-id.");	// valid in RHEL59
		Assert.assertEquals(result.getExitCode(), new Integer(1), "ExitCode from an attempt to disable an invalid-repo-id.");
		//Assert.assertEquals(result.getStdout().trim(), String.format("Error: %s is not a valid repo id. Use --list option to see valid repos.",invalidRepo), "Stdout from an attempt to disable an invalid-repo-id.");	// changed by bug 878634
		String expectedStdout = String.format("Error: %s is not a valid repo ID. Use --list option to see valid repos.",invalidRepo);
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.6-1")) expectedStdout = String.format("Error: %s is not a valid repository ID. Use --list option to see valid repositories.",invalidRepo);	// bug 1122530 commit add5a9b746f9f2af147a7e4622b897a46b5ef132
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.10-1")) expectedStdout = String.format("Error: '%s' does not match a valid repository ID. Use \"subscription-manager repos --list\" to see valid repositories.",invalidRepo);	// bug 1351009 subscription-manager RHEL7.3 commit eaa748187bee110a19184864b1775705b653f629
		Assert.assertEquals(result.getStdout().trim(), expectedStdout, "Stdout from an attempt to disable an invalid-repo-id.");
		Assert.assertEquals(result.getStderr().trim(), "", "Stderr from an attempt to disable an invalid-repo-id.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20382", "RHEL7-51678"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: attempt multiple enable/disable invalid repo ids",
			groups={"Tier3Tests","blockedByBug-846207","blockedByBug-918746"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposEnableDisableMultipleInvalidRepos(){
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
		SSHCommandResult result = clienttasks.repos_(null, null, null, enableRepos, disableRepos, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), new Integer(1), "ExitCode from an attempt to disable an invalid-repo-id.");
		Assert.assertEquals(result.getStderr().trim(), "", "Stderr from an attempt to disable an invalid-repo-id.");
		String expectedStdoutMsgFormat = "Error: %s is not a valid repo ID. Use --list option to see valid repos.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.6-1")) expectedStdoutMsgFormat = "Error: %s is not a valid repository ID. Use --list option to see valid repositories.";	// bug 1122530 commit add5a9b746f9f2af147a7e4622b897a46b5ef132
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.10-1")) expectedStdoutMsgFormat = "Error: '%s' does not match a valid repository ID. Use \"subscription-manager repos --list\" to see valid repositories.";	// bug 1351009 subscription-manager RHEL7.3 commit eaa748187bee110a19184864b1775705b653f629
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


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20395", "RHEL7-51677"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: attempt enable/disable all repos (using wildcard *)",
			groups={"Tier3Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposEnableDisableAllReposUsingWildcard() throws JSONException, Exception{
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

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		
		String expectedStdoutMsgFormat = "Repo %s is %s for this system.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.7-1")) expectedStdoutMsgFormat = "Repo '%s' is %s for this system.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.6-1")) expectedStdoutMsgFormat = "Repository '%s' is %s for this system.";	// bug 1122530 commit add5a9b746f9f2af147a7e4622b897a46b5ef132
		
		// test: subscription-manager repos --enablerepo=* --disablerepo=*
		SSHCommandResult result = clienttasks.repos_(null, null, null, "*", "*", null, null, null, null);
		Assert.assertEquals(result.getExitCode(), new Integer(0), "ExitCode from an attempt to enable/disable all repos (using wildcard *).");
		// verify the feedback
		for (Repo subscribedRepo : subscribedRepos) {
			String expectedEnableStdoutMsg = String.format(expectedStdoutMsgFormat,subscribedRepo.repoId,"enabled");
			String expectedDisableStdoutMsg = String.format(expectedStdoutMsgFormat,subscribedRepo.repoId,"disabled");
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.12.8-1")) {
				Assert.assertTrue(!result.getStdout().contains(expectedEnableStdoutMsg), "Stdout from an attempt to enable/disable all repos (using wildcard *) does NOT contain message: "+expectedEnableStdoutMsg);		
			} else {
				Assert.assertTrue(result.getStdout().contains(expectedEnableStdoutMsg), "Stdout from an attempt to enable/disable all repos (using wildcard *) contains expected message: "+expectedEnableStdoutMsg);		
			}
			Assert.assertTrue(result.getStdout().contains(expectedDisableStdoutMsg), "Stdout from an attempt to enable/disable all repos (using wildcard *) contains expected message: "+expectedDisableStdoutMsg);		
		}
		// verify the actual yum repolist disabled
		List<String> yumRepoListDisabled = clienttasks.getYumRepolist("disabled");
		for (Repo subscribedRepo : subscribedRepos) {
			Assert.assertTrue(yumRepoListDisabled.contains(subscribedRepo.repoId), "After using wildcard * to disable all repos using subscription-manager, entitled repo '"+subscribedRepo.repoId+"' appears on the yum repolist disabled.");
		}
		
		// test: subscription-manager repos --enablerepo=*
		result = clienttasks.repos_(null, null, null, "*", null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), new Integer(0), "ExitCode from an attempt to enable all repos (using wildcard *).");
		// verify the feedback
		for (Repo subscribedRepo : subscribedRepos) {
			String expectedEnableStdoutMsg = String.format(expectedStdoutMsgFormat,subscribedRepo.repoId,"enabled");
			String expectedDisableStdoutMsg = String.format(expectedStdoutMsgFormat,subscribedRepo.repoId,"disabled");
			Assert.assertTrue(result.getStdout().contains(expectedEnableStdoutMsg), "Stdout from an attempt to enable all repos (using wildcard *) contains expected message: "+expectedEnableStdoutMsg);		
			Assert.assertTrue(!result.getStdout().contains(expectedDisableStdoutMsg), "Stdout from an attempt to enable all repos (using wildcard *) does NOT contain message: "+expectedDisableStdoutMsg);		
		}
		// verify the actual yum repolist enabled
		List<String> yumRepoListEnabled = clienttasks.getYumRepolist("enabled");
		for (Repo subscribedRepo : subscribedRepos) {
			Assert.assertTrue(yumRepoListDisabled.contains(subscribedRepo.repoId), "After using wildcard * to enable all repos using subscription-manager, entitled repo '"+subscribedRepo.repoId+"' appears on the yum repolist enabled.");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20370", "RHEL7-51680"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: attempt enable/disable some repos (using wildcard ?)",
			groups={"Tier3Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposEnableDisableSomeReposUsingWildcard() throws JSONException, Exception{
		//	[root@jsefler-os6 ~]# subscription-manager repos --disable=awesomeos-i???
		//	Repository 'awesomeos-i686' is disabled for this system.
		//	Repository 'awesomeos-ia64' is disabled for this system.
		//	[root@jsefler-6 ~]# echo $?
		//	0

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		
		String expectedStdoutMsgFormat = "Repo %s is %s for this system.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.7-1")) expectedStdoutMsgFormat = "Repo '%s' is %s for this system.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.6-1")) expectedStdoutMsgFormat = "Repository '%s' is %s for this system.";	// bug 1122530 commit add5a9b746f9f2af147a7e4622b897a46b5ef132
		
		// get a random subscribedRepo
		Repo randomSubscribedRepo = getRandomListItem(subscribedRepos);
		String randomSubscribedRepoId = randomSubscribedRepo.repoId;
		String wildcardedRepo = randomSubscribedRepoId.replaceAll(String.valueOf(randomSubscribedRepoId.charAt(randomGenerator.nextInt(randomSubscribedRepoId.length()))),"?");	// e.g. "awesomeos-s390x" to "awes?me?s-s390x"
		String regexRepo = wildcardedRepo.replaceAll("\\?",".");
		
		// test: subscription-manager repos --disablerepo=awes?me?s-s390x
		clienttasks.repos_(null, null, null, "*", null, null, null, null, null);
		SSHCommandResult result = clienttasks.repos_(null, null, null, null, wildcardedRepo, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), new Integer(0), "ExitCode from an attempt to enable/disable some repos (using wildcard ?).");
		// verify the feedback
		for (Repo subscribedRepo : subscribedRepos) {
			String expectedEnableStdoutMsg = String.format(expectedStdoutMsgFormat,subscribedRepo.repoId,"enabled");
			String expectedDisableStdoutMsg = String.format(expectedStdoutMsgFormat,subscribedRepo.repoId,"disabled");
			if (subscribedRepo.repoId.matches(regexRepo)) {
				Assert.assertTrue(result.getStdout().contains(expectedDisableStdoutMsg), "Stdout from an attempt to disable repos '"+wildcardedRepo+"' (using wildcard ?) contains expected message: "+expectedDisableStdoutMsg);		
			} else {
				Assert.assertTrue(!result.getStdout().contains(expectedDisableStdoutMsg), "Stdout from an attempt to disable repos '"+wildcardedRepo+"' (using wildcard ?) does NOT contain message: "+expectedDisableStdoutMsg);		
			}
		}
		// verify the actual yum repolist disabled
		List<String> yumRepoListDisabled = clienttasks.getYumRepolist("disabled");
		for (Repo subscribedRepo : subscribedRepos) {
			if (subscribedRepo.repoId.matches(regexRepo)) {
				Assert.assertTrue(yumRepoListDisabled.contains(subscribedRepo.repoId), "After calling subscription-manager repos to disable '"+wildcardedRepo+"', entitled repo '"+subscribedRepo.repoId+"' appears on the yum repolist disabled.");
			} else {
				Assert.assertTrue(!yumRepoListDisabled.contains(subscribedRepo.repoId), "After calling subscription-manager repos to disable '"+wildcardedRepo+"', entitled repo '"+subscribedRepo.repoId+"' does NOT appear on the yum repolist disabled.");	
			}
		}
		
		// test: subscription-manager repos --enablerepo=*
		clienttasks.repos_(null, null, null, null, "*", null, null, null, null);
		result = clienttasks.repos_(null, null, null, wildcardedRepo, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), new Integer(0), "ExitCode from an attempt to enable/disable some repos (using wildcard ?).");
		// verify the feedback
		for (Repo subscribedRepo : subscribedRepos) {
			String expectedEnableStdoutMsg = String.format(expectedStdoutMsgFormat,subscribedRepo.repoId,"enabled");
			String expectedDisableStdoutMsg = String.format(expectedStdoutMsgFormat,subscribedRepo.repoId,"disabled");
			if (subscribedRepo.repoId.matches(regexRepo)) {
				Assert.assertTrue(result.getStdout().contains(expectedEnableStdoutMsg), "Stdout from an attempt to enable repos '"+wildcardedRepo+"' (using wildcard ?) contains expected message: "+expectedEnableStdoutMsg);		
			} else {
				Assert.assertTrue(!result.getStdout().contains(expectedEnableStdoutMsg), "Stdout from an attempt to enable repos '"+wildcardedRepo+"' (using wildcard ?) does NOT contain message: "+expectedEnableStdoutMsg);		
			}
		}
		// verify the actual yum repolist enabled
		yumRepoListDisabled = clienttasks.getYumRepolist("enabled");
		for (Repo subscribedRepo : subscribedRepos) {
			if (subscribedRepo.repoId.matches(regexRepo)) {
				Assert.assertTrue(yumRepoListDisabled.contains(subscribedRepo.repoId), "After calling subscription-manager repos to enable '"+wildcardedRepo+"', entitled repo '"+subscribedRepo.repoId+"' appears on the yum repolist enabled.");
			} else {
				Assert.assertTrue(!yumRepoListDisabled.contains(subscribedRepo.repoId), "After calling subscription-manager repos to enable '"+wildcardedRepo+"', entitled repo '"+subscribedRepo.repoId+"' does NOT appear on the yum repolist enabled.");	
			}
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20387", "RHEL7-51679"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: attempt enable/disable/enable/disable repos in an order",
			groups={"Tier3Tests","blockedByBug-1115499"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposEnableDisableReposInOrder() throws JSONException, Exception{
		//	[root@jsefler-6 ~]# subscription-manager repos --enable=awesomeos-ppc64 --enable=awesomeos-x86_64 --disable=awesomeos-x86_64 --disable=awesomeos-ia64 --enable=awesomeos-ia64 --disable=awesomeos-ppc64
		//	Repo 'awesomeos-ppc64' is disabled for this system.
		//	Repo 'awesomeos-x86_64' is disabled for this system.
		//	Repo 'awesomeos-ia64' is enabled for this system.

		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.12.8-1")) throw new SkipException("Bugzilla 1115499 was not implemented in this version of subscription-manager.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		//List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		List<Repo> subscribedRepos = getRandomSubsetOfList(clienttasks.getCurrentlySubscribedRepos(),5);
		
		String expectedStdoutMsgFormat = "Repo %s is %s for this system.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.7-1")) expectedStdoutMsgFormat = "Repo '%s' is %sd for this system.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.6-1")) expectedStdoutMsgFormat = "Repository '%s' is %sd for this system.";	// bug 1122530 commit add5a9b746f9f2af147a7e4622b897a46b5ef132
		String command = clienttasks.reposCommand(null, null, null, null, null, null, null, null, null);
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
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20369", "RHEL7-51682"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: enable a repo.",
			enabled=true,
			groups={"Tier3Tests"})
	//@ImplementsNitrateTest(caseId=)
	public void testReposEnable() throws JSONException, Exception {
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		subscribedRepos = getRandomSubsetOfList(subscribedRepos, 5);	// reduce the runtime of this test by randomly reducing the subscribedRepos tested
		if (subscribedRepos.isEmpty()) throw new SkipException("There are no entitled repos available for this test.");
				
		for(Repo repo:subscribedRepos) {
			SSHCommandResult result = clienttasks.repos_(false,null,null,repo.repoId,null,null, null, null, null);
			String expectedStdout = String.format("Repo %s is enabled for this system.",repo.repoId);
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.7-1")) expectedStdout = String.format("Repo '%s' is enabled for this system.",repo.repoId);	// subscription-manager commit b9e7f7abb949bc007f2db02662e2abba76528082
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.6-1")) expectedStdout = String.format("Repository '%s' is enabled for this system.",repo.repoId);	// bug 1122530 commit add5a9b746f9f2af147a7e4622b897a46b5ef132
			Assert.assertEquals(result.getStdout().trim(),expectedStdout);
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20388", "RHEL7-51676"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: disable a repo.",
			groups={"Tier3Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposDisable() throws JSONException, Exception {
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		subscribedRepos = getRandomSubsetOfList(subscribedRepos, 5);	// reduce the runtime of this test by randomly reducing the subscribedRepos tested
		if (subscribedRepos.isEmpty()) throw new SkipException("There are no entitled repos available for this test.");
		
		for(Repo repo:subscribedRepos) {
			SSHCommandResult result = clienttasks.repos_(false,null,null,null,repo.repoId,null, null, null, null);
			String expectedStdout = String.format("Repo %s is disabled for this system.",repo.repoId);
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.7-1")) expectedStdout = String.format("Repo '%s' is disabled for this system.",repo.repoId);	// subscription-manager commit b9e7f7abb949bc007f2db02662e2abba76528082
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.6-1")) expectedStdout = String.format("Repository '%s' is disabled for this system.",repo.repoId);	// bug 1122530 commit add5a9b746f9f2af147a7e4622b897a46b5ef132
			Assert.assertEquals(result.getStdout().trim(),expectedStdout);
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19974", "RHEL7-51013"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: manually add more yum repository options to redhat.repo and assert persistence.",
			groups={"Tier1Tests","blockedByBug-845349","blockedByBug-834806","blockedByBug-1098891","blockedByBug-1101571","blockedByBug-1101584","blockedByBug-1366301"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumRepoListPreservesAdditionalOptionsToRedhatReposUsingManualEdits() throws JSONException, Exception {
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
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
		clienttasks.refresh(null,null,null, null);
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
		List<String> releases = clienttasks.getCurrentlyAvailableReleases(null,null,null, null);
		if (!releases.isEmpty()) {
			clienttasks.release(null, null, releases.get(randomGenerator.nextInt(releases.size())), null, null, null, null, null);
			yumRepoAfterUpdate = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", yumRepo.id, clienttasks.getCurrentlySubscribedYumRepos());
			Assert.assertNotNull(yumRepoAfterUpdate, "Found yum repo ["+yumRepo.id+"] after we manually altered it, set a release, and issued a yum transaction.");
			Assert.assertEquals(yumRepoAfterUpdate.exclude, yumRepo.exclude, "Yum repo ["+yumRepo.id+"] has persisted the manually added \"exclude\" option and its value even after setting a release and issuing a yum transaction.");
			Assert.assertEquals(yumRepoAfterUpdate.priority, yumRepo.priority, "Yum repo ["+yumRepo.id+"] has persisted the manually added \"priority\" option and its value even after setting a release and issuing a yum transaction.");
			// THIS WILL LIKELY NOT BE EQUAL WHEN THE yumRepoAfterUpdate.baseurl POINTS TO RHEL CONTENT SINCE IT WILL CONTAIN THE RELEASE PREFERENCE SUBSTITUTED FOR $releasever	//Assert.assertEquals(yumRepoAfterUpdate, yumRepo, "Yum repo ["+yumRepo.id+"] has persisted all of its repository option values even after setting a release and issuing a yum transaction.");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19975", "RHEL7-51014"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: add more yum repository options to redhat.repo and assert persistence using repo-override module.",
			groups={"Tier1Tests","blockedByBug-845349","blockedByBug-834806","blockedByBug-803746","blockedByBug-1086316","blockedByBug-1069230","blockedByBug-1366301"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumRepoListPreservesAdditionalOptionsToRedhatReposUsingRepoOverride() throws JSONException, Exception {
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
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
			clienttasks.repo_override(null, null, yumRepo.id, null, addNameValueMap, null, null, null, null);			
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
		clienttasks.refresh(null,null,null, null);
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
		List<String> releases = clienttasks.getCurrentlyAvailableReleases(null,null,null, null);
		if (!releases.isEmpty()) {
			clienttasks.release(null, null, releases.get(randomGenerator.nextInt(releases.size())), null, null, null, null, null);
			yumRepoAfterUpdate = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", yumRepo.id, clienttasks.getCurrentlySubscribedYumRepos());
			Assert.assertNotNull(yumRepoAfterUpdate, "Found yum repo ["+yumRepo.id+"] after we manually altered it, set a release, and issued a yum transaction.");
			Assert.assertEquals(yumRepoAfterUpdate.exclude, yumRepo.exclude, "Yum repo ["+yumRepo.id+"] has persisted the manually added \"exclude\" option and its value even after setting a release and issuing a yum transaction.");
			Assert.assertEquals(yumRepoAfterUpdate.priority, yumRepo.priority, "Yum repo ["+yumRepo.id+"] has persisted the manually added \"priority\" option and its value even after setting a release and issuing a yum transaction.");
			// THIS WILL LIKELY NOT BE EQUAL WHEN THE yumRepoAfterUpdate.baseurl POINTS TO RHEL CONTENT SINCE IT WILL CONTAIN THE RELEASE PREFERENCE SUBSTITUTED FOR $releasever	//Assert.assertEquals(yumRepoAfterUpdate, yumRepo, "Yum repo ["+yumRepo.id+"] has persisted all of its repository option values even after setting a release and issuing a yum transaction.");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20298", "RHEL7-51698"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: verify that overrides take precedence to manually edited repository options in redhat.repo and assert persistence.",
			groups={"Tier3Tests","blockedByBug-1098891","blockedByBug-1101571","blockedByBug-1101584"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumRepoListPreservesAdditionalOptionsToRedhatReposUsingManualEditsButRepoOverridesTakePrecedence() throws JSONException, Exception {
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
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
		clienttasks.repos(null, null, null, null, yumRepo.id, null, null, null, null);
		// assert that the yum repo was disabled and a repo-override was created that takes precedence over the manually enabled yum repo
		client.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin");	// issue a new yum transaction
		yumRepoAfterUpdate = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", yumRepo.id, clienttasks.getCurrentlySubscribedYumRepos());	// getCurrentlySubscribedYumRepos() includes a yum transaction: "yum -q repolist --disableplugin=rhnplugin"	(NOT ANYMORE after bug 1008016)
		Assert.assertNotNull(yumRepoAfterUpdate, "Found yum repo ["+yumRepo.id+"] in '"+clienttasks.redhatRepoFile+"'.");
		Assert.assertFalse(yumRepoAfterUpdate.enabled, "Yum repo ["+yumRepo.id+"] has persisted the disablement by subscription-manager.");
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(clienttasks.repo_override(true,null,(String)null,(String)null,null,null,null,null, null).getStdout(), String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,yumRepo.id,"enabled","0")),"After a calling subscription-manager repos --enable, the subscription-manager repo-override list includes an override for repo='"+yumRepo.id+"' name='"+"enabled"+"' value='"+"0"+"'.");
		
		// now attempt to manually re-enable yum repo
		clienttasks.updateYumRepo(clienttasks.redhatRepoFile, yumRepo);
		// assert that the attempt to manually re-enable the yum repo fails since the subscription-manager repo override takes precedence over manual edits.
		client.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin");	// issue a new yum transaction
		yumRepoAfterUpdate = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", yumRepo.id, clienttasks.getCurrentlySubscribedYumRepos());	// getCurrentlySubscribedYumRepos() includes a yum transaction: "yum -q repolist --disableplugin=rhnplugin"	(NOT ANYMORE after bug 1008016)
		Assert.assertNotNull(yumRepoAfterUpdate, "Found yum repo ["+yumRepo.id+"] in '"+clienttasks.redhatRepoFile+"'.");
		Assert.assertFalse(yumRepoAfterUpdate.enabled, "Yum repo ["+yumRepo.id+"] is still disabled after an attempt to manually enable it (because repo-override should take precedence).");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19973", "RHEL7-51012"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Verify that the redhat.repo file is refreshed with changes to the entitlements (yum transactions are no longer required to update the redhat.repo)",
			groups={"Tier1Tests","blockedByBug-1008016","blockedByBug-1090206","blockedByBug-1034429"},	// TODO: review all tests and tasks that issue yum transactions simply to re-populate the redhat.repo
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumTransactionsAreNoLongerRequiredToTriggerUpdatesToRedhatRepo() {
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null);	// shut off auto-healing
		List<ProductCert> productCerts = clienttasks.getCurrentProductCerts();

		// Subscribe to a randomly available pool...
		log.info("Subscribe to a randomly available pool...");
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		if (pools.size()<=0) throw new SkipException("No susbcriptions were available which blocks this test from executing.");
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool

		// testing the subscribe case...
		clienttasks.subscribe_(null, null, pool.poolId, null, null, null, null, null, null, null, null, null, null);
		log.info("Immediately after attaching a subscription, we will now assert that all of the content repos from the currently attached entitlements are currently present in the redhat.repo without having triggered a yum transaction...");
		verifyCurrentEntitlementCertsAreReflectedInCurrentlySubscribedYumRepos(productCerts);
		
		// testing the unsubscribe case...
		clienttasks.unsubscribe_(true,(BigInteger)null,null,null,null, null, null);
		log.info("Immediately after removing a subscription, we will now assert that the redhat.repo is empty without having triggered a yum transaction...");
		verifyCurrentEntitlementCertsAreReflectedInCurrentlySubscribedYumRepos(productCerts);
		
		// testing the autosubscribe case...
		clienttasks.subscribe_(true, null, (String)null, null, null, null, null, null, null, null, null, null, null);
		log.info("Immediately after autoattaching subscription(s), we will now assert that all of the content repos from the currently attached entitlements are currently present in the redhat.repo without having triggered a yum transaction...");
		verifyCurrentEntitlementCertsAreReflectedInCurrentlySubscribedYumRepos(productCerts);
		
		// testing the unregister case...
		clienttasks.unregister_(null, null, null, null);
		log.info("Immediately after unregistering, we will now assert that the redhat.repo is empty without having triggered a yum transaction...");
		verifyCurrentEntitlementCertsAreReflectedInCurrentlySubscribedYumRepos(productCerts);
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20381", "RHEL7-51697"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Verify that redhat.repo file is unnecessarily re-written with every yum transaction",
			groups={"Tier3Tests","blockedByBug-1035440"/*,"blockedByBug-1090206","blockedByBug-1008016"*/,"blockedByBug-1104731","blockedByBug-1104777"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumTransactionsDoNotCauseUnnessarilyRewritesOfRedHatRepoFile() {
		clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		clienttasks.autoheal_(null, null, true, null, null, null, null);
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
		clienttasks.repos(null, null, null, defaultDisabledRepos, defaultEnabledRepos, null, null, null, null);
		String finalModTime = client.runCommandAndWait("stat -c %y "+clienttasks.redhatRepoFile).getStdout().trim();
		Assert.assertTrue(!finalModTime.equals(initialModTime), "The modification time of '"+clienttasks.redhatRepoFile+"' before and after repos enablement should be different.");
		
		// finally let's trigger the subscription-manager yum plugin and assert the modification time remains unchanged
		clienttasks.getYumRepolist("all -q");
		Assert.assertEquals(client.runCommandAndWait("stat -c %y "+clienttasks.redhatRepoFile).getStdout().trim(), finalModTime, "The modification time of '"+clienttasks.redhatRepoFile+"' before and after a yum transaction.");
		clienttasks.yumClean("all");
		Assert.assertEquals(client.runCommandAndWait("stat -c %y "+clienttasks.redhatRepoFile).getStdout().trim(), finalModTime, "The modification time of '"+clienttasks.redhatRepoFile+"' before and after a yum transaction.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20385", "RHEL7-51686"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: subscribe to a pool and verify that repos list --list-enabled reports only the enabled repos from the newly entitled content namespaces",
			groups={"Tier3Tests","blockedByBug-1119648"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposListEnabledReportsOnlyEnabledContentNamespaces() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.4-1")) throw new SkipException("The repos --list-enabled function was not implemented until version subscription-manager-1.13.4-1");
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<YumRepo> subscribedYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		if (subscribedYumRepos.isEmpty()) throw new SkipException("There are no entitled yum repos available for this test.");
		List<YumRepo> enabledYumRepos = YumRepo.findAllInstancesWithMatchingFieldFromList("enabled", Boolean.TRUE, subscribedYumRepos);
		
		// get the list of currently enabled repos
		SSHCommandResult sshCommandResult = clienttasks.repos(null, true, null, (String)null,(String)null,null, null, null, null);
		List<Repo> listEnabledRepos = Repo.parse(sshCommandResult.getStdout());
		
		// verify that the enabledYumRepos are all present in the listEnabledRepos
		for (YumRepo yumRepo : enabledYumRepos) {
			Repo enabledRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", yumRepo.id, listEnabledRepos);
			Assert.assertNotNull(enabledRepo, "Enabled yum repo ["+yumRepo.id+"] is included in the report of repos --list-enabled.");
		}
		Assert.assertEquals(listEnabledRepos.size(), enabledYumRepos.size(),"The number of --list-enabled repos matches the number of enabled yum repos in '"+clienttasks.redhatRepoFile+"'.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20368", "RHEL7-51684"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: subscribe to a pool and verify that repos list --list-disabled reports only the disabled repos from the newly entitled content namespaces",
			groups={"Tier3Tests","blockedByBug-1119648"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposListDisabledReportsOnlyDisabledContentNamespaces() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.4-1")) throw new SkipException("The repos --list-disabled function was not implemented until version subscription-manager-1.13.4-1");
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<YumRepo> subscribedYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		if (subscribedYumRepos.isEmpty()) throw new SkipException("There are no entitled yum repos available for this test.");
		List<YumRepo> disabledYumRepos = YumRepo.findAllInstancesWithMatchingFieldFromList("enabled", Boolean.FALSE, subscribedYumRepos);
		
		// get the list of currently disabled repos
		SSHCommandResult sshCommandResult = clienttasks.repos(null, null, true, (String)null,(String)null,null, null, null, null);
		List<Repo> listDisabledRepos = Repo.parse(sshCommandResult.getStdout());
		
		// verify that the disabledYumRepos are all present in the listDisabledRepos
		for (YumRepo yumRepo : disabledYumRepos) {
			Repo disabledRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", yumRepo.id, listDisabledRepos);
			Assert.assertNotNull(disabledRepo, "Disabled yum repo ["+yumRepo.id+"] is included in the report of repos --list-disabled.");
		}
		Assert.assertEquals(listDisabledRepos.size(), disabledYumRepos.size(),"The number of --list-disabled repos matches the number of disabled yum repos in '"+clienttasks.redhatRepoFile+"'.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20300", "RHEL7-51683"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: repos --list-disabled should give feedback when there are no disabled repos",
			groups={"Tier3Tests","blockedByBug-1151925"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposListDisabledReportsNoMatchesFoundWhenNoThereAreNoDisabledRepos() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.4-1")) throw new SkipException("The repos --list-disabled function was not implemented until version subscription-manager-1.13.4-1");
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<YumRepo> subscribedYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		if (subscribedYumRepos.isEmpty()) throw new SkipException("There are no entitled yum repos available for this test.");
		
		// use a wildcard to enable all the repos
		clienttasks.repos(null, null, null, "*", null, null, null, null, null);
				
		// get the list of currently disabled repos (list should be empty)
		SSHCommandResult sshCommandResult = clienttasks.repos(null, false, true, (String)null,(String)null,null, null, null, null);
		List<Repo> listDisabledRepos = Repo.parse(sshCommandResult.getStdout());
		Assert.assertTrue(listDisabledRepos.isEmpty(),"After using a wildcard to enable all repos, the repos --list-disabled should be empty.");
		
		// assert the feedback when no repos are enabled
		String expectedStdout = "There were no available repositories matching the specified criteria.";
		Assert.assertEquals(sshCommandResult.getStdout().trim(),expectedStdout,"After using a wildcard to enable all repos, the repos --list-disabled should report feedback indicating no matches.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20389", "RHEL7-51685"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: repos --list-enabled should give feedback when there are no enabled repos",
			groups={"Tier3Tests","blockedByBug-1151925"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReposListEnabledReportsNoMatchesFoundWhenNoThereAreNoEnabledRepos() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.4-1")) throw new SkipException("The repos --list-enabled function was not implemented until version subscription-manager-1.13.4-1");
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<YumRepo> subscribedYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		if (subscribedYumRepos.isEmpty()) throw new SkipException("There are no entitled yum repos available for this test.");
		
		// use a wildcard to disable all the repos
		clienttasks.repos(null, null, null, null, "*", null, null, null, null);
				
		// get the list of currently disabled repos (list should be empty)
		SSHCommandResult sshCommandResult = clienttasks.repos(null, true, false, (String)null,(String)null,null, null, null, null);
		List<Repo> listDisabledRepos = Repo.parse(sshCommandResult.getStdout());
		Assert.assertTrue(listDisabledRepos.isEmpty(),"After using a wildcard to disable all repos, the repos --list-enabled should be empty.");
		
		// assert the feedback when no repos are disabled
		String expectedStdout = "There were no available repositories matching the specified criteria.";
		Assert.assertEquals(sshCommandResult.getStdout().trim(),expectedStdout,"After using a wildcard to disable all repos, the repos --list-enabled should report feedback indicating no matches.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20398", "RHEL7-51696"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Verify that RepoActionInvoker.is_managed('some_repo') returns False when 'some_repo' does not exist and True when it is entitled",
			groups={"Tier3Tests","blockedByBug-1223038"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testFixForBug1223038() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.14.9-1")) throw new SkipException("The fix for this bug was not implemented until version subscription-manager-1.14.9-1");
		
		// copy the ismanagedtest.py script to the client
		File ismanagedtestFile = new File(System.getProperty("automation.dir", null)+"/scripts/ismanagedtest.py");
		if (!ismanagedtestFile.exists()) Assert.fail("Failed to find expected script: "+ismanagedtestFile);
		RemoteFileTasks.putFile(client, ismanagedtestFile.toString(), "/usr/local/bin/", "0755");

		RemoteFileTasks.runCommandAndAssert(client, "ismanagedtest.py some_repo", 0, "False", null);
		
		// while registered and subscribed to a repo (either enabled or disabled), call ismanagedtest.py repo should be true
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		List<Repo> repos = clienttasks.getCurrentlySubscribedRepos();
		//for (Repo repo : repos) {
		for (Repo repo : getRandomSubsetOfList(repos,5)) {
			RemoteFileTasks.runCommandAndAssert(client, "ismanagedtest.py "+repo.repoId, 0, "True", null);
		}
	}
	
	
	
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47936", "RHEL7-99418"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify that the subscription-manager yum plugin will be invoked by yum-config-manager thereby populating the redhat.repo file (eliminating the need to run a seemingly no-op yum repolist just to populate an empty/missing redhat.repo file.)  Reference RFE Bugzilla 1329349",
			groups={"Tier1Tests","blockedByBug-1329349","blockedByBug-1486326","blockedByBug-1480659","blockedByBug-1486338"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumConfigManagerTransactionsWillNowGenerateRedhatRepo() throws JSONException, Exception {
		
		// register with auto-subscribe to get an entitlement to at least one yum repo
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		List<YumRepo> subscribedYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		if (subscribedYumRepos.isEmpty()) throw new SkipException("There are no entitled yum repos available for this test.");
		
		// get all the disabled yum repos for this test
		List<YumRepo> disabledYumRepos = YumRepo.findAllInstancesWithMatchingFieldFromList("enabled", false, subscribedYumRepos);
		if (disabledYumRepos.isEmpty()) throw new SkipException("Could not find any entitled yum repos that were disabled for this test.");
		List<String> disabledRepos = new ArrayList<String>();
		for (YumRepo disabledYumRepo : disabledYumRepos) disabledRepos.add(disabledYumRepo.id);
		
		// manually delete or truncate the redhat.repo files
		client.runCommandAndWait("rm -f "+clienttasks.redhatRepoFile);
		client.runCommandAndWait("rm -f "+clienttasks.redhatRepoServerValueFile);
		Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile));
		
		// use yum-config-manager to enable the repos that were disabled by default
		clienttasks.yumConfigManagerEnableRepos(disabledRepos, null);
		
		// assert that the redhat.repo was regenerated by the new subscription-manager plugin to yum-config-manager (blocked by RFE Bug 1329349)
		Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"File '"+clienttasks.redhatRepoFile+"' has been automatically generated by a subscription-manager plugin to yum-config-manager.");
		
		// assert that the yum repos within redhat.repo are now enabled
		subscribedYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		for (String disabledRepo : disabledRepos) {
			YumRepo yumRepo = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", disabledRepo, subscribedYumRepos);
			Assert.assertTrue(yumRepo.enabled, "Entitled yum repo '"+disabledRepo+"' (which was disabled by default) appears in '"+clienttasks.redhatRepoFile+"' as enabled after using yum-config-manager to enable it.");
		}
		
		// run yum repolist enabled and assert that all the repos are enabled (blocked by Bug 1480659)
		ArrayList<String> yumRepoListEnabledRepos = clienttasks.getYumRepolist("enabled");
		for (String disabledRepo : disabledRepos) {
			Assert.assertTrue(yumRepoListEnabledRepos.contains(disabledRepo), "Entitled yum repo '"+disabledRepo+"' which was disabled by default, appears in 'yum repolist enabled' as enabled after using yum-config-manager to enable it.");
		}
	}
	
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47937", "RHEL7-99476"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: manually enable (using sed) all yum repositories in redhat.repo and assert persistence in 'yum repolist enabled' (but make sure /var/lib/rhsm/repo_server_val/redhat.repo is truncated first).",
			groups={"Tier1Tests","blockedByBug-1480659","blockedByBug-1486338"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumRepoListPreservesManuallyEnabledRedhatReposAfterDeletingVarLibRhsmRepoServerValRedHatRepo() throws JSONException, Exception {
		
		// STEP 1: ENSURE SYSTEM IS UNREGISTERED
		clienttasks.unregister(null, null, null, null);
		
		// STEP 2: DELETE/TRUNCATE THE /var/lib/rhsm/repo_server_val/redhat.repo FILE
		client.runCommandAndWait("truncate --size=0 "+clienttasks.redhatRepoServerValueFile);
		
		// STEP 3: REGISTER A RHEL SYSTEM WITH AUTO-ATTACH TO GET AN ENTITLEMENT TO ACCESS REPOS ON THE CDN
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		// get all the entitled yum repos that are disabled by default
		List<YumRepo> subscribedYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		if (subscribedYumRepos.isEmpty()) throw new SkipException("There are no entitled yum repos available for this test.");
		List<YumRepo> disabledYumRepos = YumRepo.findAllInstancesWithMatchingFieldFromList("enabled", false, subscribedYumRepos);
		if (disabledYumRepos.isEmpty()) throw new SkipException("Could not find any entitled yum repos that were disabled for this test.");
		List<String> disabledRepos = new ArrayList<String>();
		for (YumRepo disabledYumRepo : disabledYumRepos) disabledRepos.add(disabledYumRepo.id);
		
		// STEP 4: MANUALLY EDIT (USING sed OR vi) /etc/yum.repos.d/redhat.repo FILE TO ENABLE ONE OR MORE REPOS (NORMALLY DISABLED BY DEFAULT)
		client.runCommandAndWait("sed -i 's/enabled\\s*=\\s*0/enabled = 1/' "+clienttasks.redhatRepoFile);
		Assert.assertEquals(client.runCommandAndWait("egrep 'enabled\\s*=\\s*0' "+clienttasks.redhatRepoFile+" | wc -l").getStdout().trim(),"0", "The number of disabled repos in '"+clienttasks.redhatRepoFile+"' after using sed to enable all entitled repos.");
		
		// STEP 5: RUN yum repolist all AND VERIFY THAT ALL THE ENTITLED REPOS ARE ENABLED
		ArrayList<String> yumRepoListEnabledRepos = clienttasks.getYumRepolist("enabled");
		for (String disabledRepo : disabledRepos) {
			Assert.assertTrue(yumRepoListEnabledRepos.contains(disabledRepo), "Entitled yum repo '"+disabledRepo+"' which was disabled by default, appears in 'yum repolist enabled' as enabled after using sed to manually enable it in '"+clienttasks.redhatRepoFile+"'.");
		}
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"", ""}, importReady=false,
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="After setting a new rhsm.conf setting for repomd_gpg_url, verify that the url (or baseurl/repomd_gpg_url when repomd_gpg_url does not start with http) gets included in a comma separated list for gpgkey in the /etc/yum.repos.d/redhat.repo entries",
			groups={"Tier2Tests","testYumRepoListIncludesConfiguredRepomdGpgUrlInGpgKey"/*,"blockedByBug-1410638"*/},
			dataProvider="getYumRepoListIncludesConfiguredRepomdGpgUrlInGpgKeyData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumRepoListIncludesConfiguredRepomdGpgUrlInGpgKey(Object bugzilla, String repomd_gpg_url) throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.21.1-1")) { // commit 8236fefe942e4a32cb2c2565c63b15d3a9464855 Support configuration of a repo metadata signing key
			throw new SkipException("This test applies a newer version of subscription manager that includes fixes for RFE Bugs 1410638 - [RFE] Give Satellite the ability to select if repo metadata should be signed with the key provided for rpm verification");
		}
		
		// configure rhsm.conf with repomd_gpg_url
		clienttasks.config(null, null, true, new String[]{"rhsm","repomd_gpg_url",repomd_gpg_url});

//NOT NEEDED
//		// refresh the entitlement cert (to ensure redhat.repo is up-to-date)
//		clienttasks.refresh(null, null, null, null);
		
		// what gpgkey URL value should we expect for repomd_gpg_url
		String expectedGpgKey = null;
		String baseurl= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "baseurl");
		// RULE: if repomd_gpg_url does NOT start with "http", prepend the baseurl
		// RULE: expect a single "/" between baseurl and repomd_gpg_url to create a valid url syntax.  eg. https://baseurl/repomd_gpg_url
		// RULE: if repomd_gpg_url is empty, then gpgkey should not include an empty string
		if (!repomd_gpg_url.isEmpty()) expectedGpgKey = repomd_gpg_url;
		if (!repomd_gpg_url.startsWith("http")) expectedGpgKey = baseurl.replaceFirst("/+$","")+"/"+repomd_gpg_url.replaceFirst("^/+","");
		if (repomd_gpg_url.isEmpty()) expectedGpgKey = null;
		log.info("Expecting the gpgkey to contain '"+expectedGpgKey+"' when repomd_gpg_url=\""+repomd_gpg_url+"\" and baseurl=\""+baseurl+"\".  If this is not correct, contact the automator of this testcase to fix the rules for expectedGpgKey.");
		
		// assert that all of the yum repos have the correct value for repomd_gpg_url included in the gpgkey
		List <YumRepo> yumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		if (yumRepos.isEmpty()) throw new SkipException("This test requires at least one entitled yum repo in '"+clienttasks.redhatRepoFile+"'"); 
		for (YumRepo yumRepo : yumRepos) {
			List<String> gpgKeys = new ArrayList<String>();
			gpgKeys.addAll(Arrays.asList(yumRepo.gpgkey.trim().split(" *, *")));	// Note: the gpgkey property can be a comma separated list of values
			if (expectedGpgKey==null) {
				// gpgkey should NOT contain an empty value
				Assert.assertTrue(!gpgKeys.contains(repomd_gpg_url), "Repo '"+yumRepo.id+"' does NOT contain expected gpgkey '"+expectedGpgKey+"' when repomd_gpg_url in '"+clienttasks.rhsmConfFile+"' is configured with value '"+repomd_gpg_url+"'.");
			} else {
				// gpgkey should contain the expectedGpgKey
				Assert.assertTrue(gpgKeys.contains(expectedGpgKey), "Repo '"+yumRepo.id+"' contains expected gpgkey '"+expectedGpgKey+"' when repomd_gpg_url in '"+clienttasks.rhsmConfFile+"' is configured with value '"+repomd_gpg_url+"'.");
			}
		}
	}
	@AfterGroups(groups={"setup"}, value={"testYumRepoListIncludesConfiguredRepomdGpgUrlInGpgKey"})
	public void unconfigureRepomdGpgUrl() {
		clienttasks.config(null, null, true, new String[]{"rhsm","repomd_gpg_url",""});
		//clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "manage_repos", "1");
	}
	@DataProvider(name="getYumRepoListIncludesConfiguredRepomdGpgUrlInGpgKeyData")
	public Object[][] getYumRepoListIncludesConfiguredRepomdGpgUrlInGpgKeyDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getYumRepoListIncludesConfiguredRepomdGpgUrlInGpgKeyDataAsListOfLists());
	}
	protected List<List<Object>>getYumRepoListIncludesConfiguredRepomdGpgUrlInGpgKeyDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// register with auto-subscribe to populate redhat.repo
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, false, null, null, null, null);

		BlockedByBzBug blockedByBug=null;
		// Object bugzilla, String repomd_gpg_url
		ll.add(Arrays.asList(new Object[] {blockedByBug,	"http://non-cdn-path/to/a/gpgkey"}));	
		ll.add(Arrays.asList(new Object[] {blockedByBug,	"https://non-cdn-path/to/a/secure/gpgkey"}));
		ll.add(Arrays.asList(new Object[] {blockedByBug,	"cdn-gpgkey"}));
		ll.add(Arrays.asList(new Object[] {blockedByBug,	"/cdn-path/to/a/gpgkey"}));
		ll.add(Arrays.asList(new Object[] {blockedByBug,	""}));
		
		return ll;
	}
	
	
	
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 797243 - manual changes to redhat.repo are too sticky https://github.com/RedHatQE/rhsm-qe/issues/188
	// TODO	Bug 846207 - multiple specifications of --enable doesnot throw error when repo id is invalid https://github.com/RedHatQE/rhsm-qe/issues/189
	// TODO Bug 886604 - etc/yum.repos.d/ does not exist, turning manage_repos off. https://github.com/RedHatQE/rhsm-qe/issues/190
	//      Bug 886992 - redhat.repo is not being created https://github.com/RedHatQE/rhsm-qe/issues/191
	
	
	
	
	
	@BeforeClass(groups={"setup"})
	public void setupBeforeClass() throws JSONException, Exception {
		currentProductCerts = clienttasks.getCurrentProductCerts();
		if (clienttasks.getCurrentConsumerId()==null) clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, false, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		modifierSubscriptionData = getModifierSubscriptionDataAsListOfLists(null);
	}
	
	@AfterMethod(groups={"setup"})
	//@AfterGroups(groups={"setup"}, value={"ManageReposTests"})	// did not work the way I wanted it to.  Other @Tests were getting launched during ManageReposTests
	public void setManageReposConfiguration() {
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "manage_repos", "1");
	}
	
	@AfterClass(groups="setup")
	public void unregisterAfterClass() {
		clienttasks.unregister(null, null, null, null);
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
			clienttasks.repos(null, null, null, yumRepo.id, null, null, null, null, null);
		} else {	// toggle the enabled flag using subscription-manager repos --disable
			clienttasks.repos(null, null, null, null, yumRepo.id, null, null, null, null);
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
		clienttasks.refresh(null, null, null, null);
		
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
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
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
