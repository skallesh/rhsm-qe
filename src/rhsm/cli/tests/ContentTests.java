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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.LinkedItem;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;

import rhsm.base.CandlepinType;
import rhsm.base.ConsumerType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.ProductCert;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;
import rhsm.data.YumRepo;

import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 */
@Test(groups={"ContentTests"})
public class ContentTests extends SubscriptionManagerCLITestScript{

	
	// Test methods ***********************************************************************
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-25987", "RHEL7-51485"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier2")
	@Test(	description="subscription-manager Yum plugin: enable/disable",
			groups={"Tier2Tests","FipsTests","EnableDisableManageReposAndVerifyContentAvailable_Test","blockedByBug-804227","blockedByBug-871146","blockedByBug-905546","blockedByBug-1017866"},
			//dataProvider="getAvailableSubscriptionPoolsData",	// very thorough, but takes too long to execute and rarely finds more bugs
			dataProvider="getRandomSubsetOfAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41696,fromPlan=2479)
	public void testEnableDisableManageReposAndVerifyContentAvailable(SubscriptionPool pool) throws JSONException, Exception {

		// get the currently installed product certs to be used when checking for conditional content tagging
		List<ProductCert> currentProductCerts = clienttasks.getCurrentProductCerts();

		log.info("Before beginning this test, we will stop the rhsmcertd so that it does not interfere with this test and make sure we are not subscribed...");
		clienttasks.stop_rhsmcertd();
		clienttasks.unsubscribe_(true,(BigInteger)null,null,null,null, null, null);
		
		// Enable rhsm manage_repos configuration
		clienttasks.config(null, null, true, new String[]{"rhsm","manage_repos","1"});
		
		log.info("Subscribe to the pool and start testing that yum repolist reports the expected repo id/labels...");
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPool_(pool);
		Assert.assertNotNull(entitlementCertFile, "Found the entitlement cert file that was granted after subscribing to pool: "+pool);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		
		// 1. Run a 'yum repolist' and get a list of all of the available repositories corresponding to your entitled products
		// 1. Repolist contains repositories corresponding to your entitled products
		ArrayList<String> repolist = clienttasks.getYumRepolist("enabled");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;
			if (!clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, currentProductCerts)) {
				Assert.assertFalse(repolist.contains(contentNamespace.label),
					"Yum repolist enabled excludes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with the manage_repos configuration enabled because not all requiredTags ("+contentNamespace.requiredTags+") in the contentNamespace are provided by the currently installed productCerts.");
				continue;
			}
			if (contentNamespace.enabled) {
				Assert.assertTrue(repolist.contains(contentNamespace.label),
					"Yum repolist enabled includes enabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with manage_repos configuration enabled.");
			} else {
				Assert.assertFalse(repolist.contains(contentNamespace.label),
					"Yum repolist enabled excludes disabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with manage_repos configuration enabled.");
			}
		}
		repolist = clienttasks.getYumRepolist("disabled");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;
			if (!clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, currentProductCerts)) {
				Assert.assertFalse(repolist.contains(contentNamespace.label),
					"Yum repolist disabled excludes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with manage_repos configuration enabled because not all requiredTags ("+contentNamespace.requiredTags+") in the contentNamespace are provided by the currently installed productCerts.");
				continue;
			}
			if (contentNamespace.enabled) {
				Assert.assertFalse(repolist.contains(contentNamespace.label),
					"Yum repolist disabled excludes enabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with manage_repos configuration enabled.");
			} else {
				Assert.assertTrue(repolist.contains(contentNamespace.label),
					"Yum repolist disabled includes disabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with manage_repos configuration enabled.");
			}
		}
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;
			if (clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, currentProductCerts)) {
				Assert.assertTrue(repolist.contains(contentNamespace.label),
					"Yum repolist all includes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with manage_repos configuration enabled.");
			} else {
				Assert.assertFalse(repolist.contains(contentNamespace.label),
					"Yum repolist all excludes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with manage_repos configuration enabled because not all requiredTags ("+contentNamespace.requiredTags+") in the contentNamespace are provided by the currently installed productCerts.");
			}
		}

		log.info("Unsubscribe from the pool and verify that yum repolist no longer reports the expected repo id/labels...");
		clienttasks.unsubscribeFromSerialNumber(entitlementCert.serialNumber);
		
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;
			Assert.assertFalse(repolist.contains(contentNamespace.label),
				"Yum repolist all excludes repo id/label '"+contentNamespace.label+"' after having unsubscribed from Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with manage_repos configuration enabled.");
		}
	
		// Disable rhsm manage_repos configuration
		log.info("Now we will disable the rhsm manage_repos configuration with enabled=0..");
		clienttasks.config(null, null, true, new String[]{"rhsm","manage_repos","0"});
		
		log.info("Again let's subscribe to the same pool and verify that yum repolist does NOT report any of the entitled repo id/labels since the manage_repos has been disabled...");
		entitlementCertFile = clienttasks.subscribeToSubscriptionPool_(pool);
		Assert.assertNotNull(entitlementCertFile, "Found the entitlement cert file that was granted after subscribing to pool: "+pool);
		entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
	
		// 2. Run a 'yum repolist' and get a list of all of the available repositories corresponding to your entitled products
		// 2. Repolist does not contain repositories corresponding to your entitled products
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;
			Assert.assertFalse(repolist.contains(contentNamespace.label),
				"Yum repolist all excludes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' with the manage_repos configuration disabled.");
		}
		
		log.info("Now we will enable manage_repos and expect the repo list to be updated");
		clienttasks.config(null, null, true, new String[]{"rhsm","manage_repos","1"});
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;
			if (clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, currentProductCerts)) {
				Assert.assertTrue(repolist.contains(contentNamespace.label),
					"Yum repolist all now includes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' followed by manage_repos configuration enabled.");
			} else {
				Assert.assertFalse(repolist.contains(contentNamespace.label),
					"Yum repolist all still excludes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' followed by manage_repos configuration enabled because not all requiredTags ("+contentNamespace.requiredTags+") in the contentNamespace are provided by the currently installed productCerts.");		
			}
		}
	}
	@AfterGroups(value="EnableDisableManageReposAndVerifyContentAvailable_Test", alwaysRun=true)
	protected void afterEnableDisableManageReposAndVerifyContentAvailable_Test() {
		clienttasks.config(null, null, true, new String[]{"rhsm","manage_repos","1"});
		clienttasks.restart_rhsmcertd(Integer.valueOf(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, /*"certFrequency" WAS CHANGED BY BUG 882459 TO */ "certCheckInterval")), null, null);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-20085", "RHEL7-51099"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier1")
	@Test(	description="subscription-manager content flag : Default content flag should enable",
			groups={"Tier1Tests","blockedByBug-804227","blockedByBug-871146","blockedByBug-924919","blockedByBug-962520"},
	        enabled=true)
	@ImplementsNitrateTest(caseId=47578,fromPlan=2479)
	public void testYumRepoListsEnabledContent() throws JSONException, Exception{
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
		
		List<ProductCert> currentProductCerts = clienttasks.getCurrentProductCerts();
		
		clienttasks.unregister(null, null, null, null);
	    clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
	    if (clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively().size()<=0)
	    	throw new SkipException("No available subscriptions were found.  Therefore we cannot perform this test.");
	    List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
	    Assert.assertTrue(!entitlementCerts.isEmpty(),"After subscribing to all available subscription pools, there must be some entitlements."); // or maybe we should skip when nothing is consumed 
		ArrayList<String> repolist = clienttasks.getYumRepolist("enabled");
		for (EntitlementCert entitlementCert : entitlementCerts) {
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;
				if (contentNamespace.enabled) {
					if (clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, currentProductCerts)) {
						Assert.assertTrue(repolist.contains(contentNamespace.label),
								"Yum repolist enabled includes enabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"'.");
					} else {
						Assert.assertFalse(repolist.contains(contentNamespace.label),
								"Yum repolist enabled excludes enabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"' because not all requiredTags ("+contentNamespace.requiredTags+") in the contentNamespace are provided by the currently installed productCerts.");
					}
				} else {
					Assert.assertFalse(repolist.contains(contentNamespace.label),
						"Yum repolist enabled excludes disabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.orderNamespace.productId+"'.");
				}
			}
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-20082", "RHEL7-51098"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier1")
	@Test(	description="subscription-manager content flag : gpgcheck value in redhat.repo should be disabled when gpg_url is empty or null",
			groups={"Tier1Tests","blockedByBug-741293","blockedByBug-805690","blockedByBug-962520"},
	        enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testGpgCheckValuesInYumRepos() throws JSONException, Exception {
		//	[root@jsefler-r63-server ~]# cat /etc/yum.repos.d/redhat.repo 
		//	#
		//	# Certificate-Based Repositories
		//	# Managed by (rhsm) subscription-manager
		//	#
		//	# If this file is empty and this system is subscribed consider 
		//	# a "yum repolist" to refresh available repos
		//	#
		//
		//	[content-label]
		//	name = content
		//	baseurl = https://cdn.redhat.com/foo/path
		//	enabled = 1
		//	gpgcheck = 1
		//	gpgkey = https://cdn.redhat.com/foo/path/gpg/
		//	sslverify = 1
		//	sslcacert = /etc/rhsm/ca/redhat-uep.pem
		//	sslclientkey = /etc/pki/entitlement/5488047145460852736-key.pem
		//	sslclientcert = /etc/pki/entitlement/5488047145460852736.pem
		//	metadata_expire = 0
		
		//	1.3.6.1.4.1.2312.9.2 (Content Namespace)
		//	1.3.6.1.4.1.2312.9.2.<content_hash> (Red Hat Enterprise Linux (core server))
		//	  1.3.6.1.4.1.2312.9.2.<content_hash>.1 (Yum repo type))
		//	    1.3.6.1.4.1.2312.9.2.<content_hash>.1.1 (Name) : Red Hat Enterprise Linux (core server)
		//	    1.3.6.1.4.1.2312.9.2.<content_hash>.1.2 (Label) : rhel-server
		//	    1.3.6.1.4.1.2312.9.2.<content_hash>.1.5 (Vendor ID): %Red_Hat_Id% or %Red_Hat_Label%
		//	    1.3.6.1.4.1.2312.9.2.<content_hash>.1.6 (Download URL): content/rhel-server/$releasever/$basearch
		//	    1.3.6.1.4.1.2312.9.2.<content_hash>.1.7 (GPG Key URL): file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release
		//	    1.3.6.1.4.1.2312.9.2.<content_hash>.1.8 (Enabled): 1
		//	    1.3.6.1.4.1.2312.9.2.<content_hash>.1.9 (Metadata Expire Seconds): 604800
		//	    1.3.6.1.4.1.2312.9.2.<content_hash>.1.10 (Required Tags): TAG1,TAG2,TAG3
		
		List<ProductCert> currentProductCerts = clienttasks.getCurrentProductCerts();
		
	    clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
	    if (clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively().size()<=0)
	    	throw new SkipException("No available subscriptions were found.  Therefore we cannot perform this test.");
	    List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
	    Assert.assertTrue(!entitlementCerts.isEmpty(),"After subscribing to all available subscription pools, there must be some entitlements."); // or maybe we should skip when nothing is consumed 

	    
	    ArrayList<String> repolist = clienttasks.getYumRepolist("enabled");
	    List<YumRepo> yumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		for (EntitlementCert entitlementCert : entitlementCerts) {
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;
				if (contentNamespace.enabled) {
					if (!clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, currentProductCerts)) continue;
					YumRepo yumRepo = YumRepo.findFirstInstanceWithMatchingFieldFromList("id"/*label*/, contentNamespace.label, yumRepos);
					Assert.assertNotNull(yumRepo, "Found the yum repo within '"+clienttasks.redhatRepoFile+"' corresponding to the entitled content namespace label '"+contentNamespace.label+"'.");
					
					// case 1: contentNamespace.gpgKeyUrl==null
					if (contentNamespace.gpgKeyUrl==null) {
						Assert.assertFalse(yumRepo.gpgcheck,
								"gpgcheck is False for Yum repo '"+yumRepo.id+"' when corresponding entitlement contentNamespace has a null gpgKeyUrl: contentNamespace: "+contentNamespace);
						Assert.assertNull(yumRepo.gpgkey,
								"gpgkey is not set for Yum repo '"+yumRepo.id+"' when corresponding entitlement contentNamespace has a null gpgKeyUrl: contentNamespace: "+contentNamespace);
					
					// case 2: contentNamespace.gpgKeyUrl==""
					} else if (contentNamespace.gpgKeyUrl.equals("")) {
						Assert.assertFalse(yumRepo.gpgcheck,
								"gpgcheck is False for Yum repo '"+yumRepo.id+"' when corresponding entitlement contentNamespace has an empty gpgKeyUrl: contentNamespace: "+contentNamespace);
						Assert.assertNull(yumRepo.gpgkey,
								"gpgkey is not set for Yum repo '"+yumRepo.id+"' when corresponding entitlement contentNamespace has an empty gpgKeyUrl: contentNamespace: "+contentNamespace);

					// case 3: contentNamespace.gpgKeyUrl.startsWith("http")
					} else if (contentNamespace.gpgKeyUrl.startsWith("http:") || contentNamespace.gpgKeyUrl.startsWith("https:")) {
						Assert.assertTrue(yumRepo.gpgcheck,
								"gpgcheck is True for Yum repo '"+yumRepo.id+"' when corresponding entitlement contentNamespace has a non-null/empty gpgKeyUrl: contentNamespace: "+contentNamespace);
						Assert.assertEquals(yumRepo.gpgkey, contentNamespace.gpgKeyUrl,
								"gpgkey is set for Yum repo '"+yumRepo.id+"' when corresponding entitlement contentNamespace has a non-null/empty gpgKeyUrl: contentNamespace: "+contentNamespace);

					// case 4: contentNamespace.gpgKeyUrl.startsWith("file:")
					} else if (contentNamespace.gpgKeyUrl.startsWith("file:")) {
						Assert.assertTrue(yumRepo.gpgcheck,
								"gpgcheck is True for Yum repo '"+yumRepo.id+"' when corresponding entitlement contentNamespace has a non-null/empty gpgKeyUrl: contentNamespace: "+contentNamespace);
						Assert.assertEquals(yumRepo.gpgkey, contentNamespace.gpgKeyUrl,
								"gpgkey is set for Yum repo '"+yumRepo.id+"' when corresponding entitlement contentNamespace has a non-null/empty gpgKeyUrl: contentNamespace: "+contentNamespace);

					// case 5: contentNamespace.gpgKeyUrl is a relative path   
					} else {
						Assert.assertTrue(yumRepo.gpgcheck,
								"gpgcheck is True for Yum repo '"+yumRepo.id+"' when corresponding entitlement contentNamespace has a non-null/empty gpgKeyUrl: contentNamespace: "+contentNamespace);
						Assert.assertEquals(yumRepo.gpgkey, clienttasks.baseurl+contentNamespace.gpgKeyUrl,
								"gpgkey is set for Yum repo '"+yumRepo.id+"' when corresponding entitlement contentNamespace has a non-null/empty gpgKeyUrl: contentNamespace: "+contentNamespace);
					}
				}
			}
		}
		if (yumRepos.isEmpty()) throw new SkipException("Since no Red Hat repos were found in '"+clienttasks.redhatRepoFile+"', there are no gpgcheck values to verify.");
	}
	
	
	@Test(	description="subscription-manager Yum plugin: ensure content can be downloaded/installed/removed",
			groups={"Tier1Tests","FipsTests","blockedByBug-701425","blockedByBug-871146","blockedByBug-962520"},
			dataProvider="getPackageFromEnabledRepoAndSubscriptionPoolData",
			enabled=false)	// disabled in favor of replacement InstallAndRemoveAnyPackageFromEnabledRepoAfterSubscribingToPool_Test
	@ImplementsNitrateTest(caseId=41695,fromPlan=2479)
	public void testInstallAndRemovePackageFromEnabledRepoAfterSubscribingToPool(String pkg, String repoLabel, SubscriptionPool pool, String quantity) throws JSONException, Exception {
		if (pkg==null) throw new SkipException("Could NOT find a unique available package from repo '"+repoLabel+"' after subscribing to SubscriptionPool: "+pool);
		
		// to avoid interference from an already enabled repo from a prior attached subscription that also
		// contains this same pkg (e.g. -htb- repos) it would be best to remove all previously attached subscriptions
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		
		// subscribe to this pool
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPool_(pool,quantity);
		Assert.assertNotNull(entitlementCertFile, "Found the entitlement cert file that was granted after subscribing to pool: "+pool);

		// install the package and assert that it is successfully installed
		SSHCommandResult yumInstallPackageResult = clienttasks.yumInstallPackageFromRepo(pkg, repoLabel, null); //pkgInstalled = true;
		
		// now remove the package
		clienttasks.yumRemovePackage(pkg);
		
		// also remove any dependencies that were installed with pkg
// FIXME: This will fail if we do not remove the dependent packages in the correct order - suppose dep-pkg1 depends on dep-pkg2 and remove dep-pkg2 first, then dep-pkg1 is already gone
// for (String depPkg : getYumDependencyPackagesInstalledFromYumInstallPackageResult(yumInstallPackageResult)) clienttasks.yumRemovePackage(depPkg);
// committing untested FIXME: ...
		List<String> depPkgsAlreadyRemoved = new ArrayList<String>();
		for (String depPkg : getYumDependencyPackagesInstalledFromYumInstallPackageResult(yumInstallPackageResult)) {
			if (!depPkgsAlreadyRemoved.contains(depPkg)) {
				depPkgsAlreadyRemoved.addAll(getYumDependencyPackagesRemovedFromYumRemovePackageResult(clienttasks.yumRemovePackage(depPkg)));
			}
		}
	}
	
	
	protected SubscriptionPool lastSubscriptionPool = null;
	protected File lastEntitlementCertFile = null;
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-22222", "RHEL7-55190"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier1")
	@Test(	description="subscription-manager Yum plugin: ensure content can be downloaded/installed/removed",
			groups={"Tier1Tests","FipsTests","blockedByBug-701425","blockedByBug-871146","blockedByBug-962520","testInstallAndRemoveAnyPackageFromEnabledRepoAfterSubscribingToPool"},
			dataProvider="getEnabledRepoAndSubscriptionPoolData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41695,fromPlan=2479)
	public void testInstallAndRemoveAnyPackageFromEnabledRepoAfterSubscribingToPool(String repoLabel, SubscriptionPool pool, String quantity) throws JSONException, Exception {
		File entitlementCertFile;
		
		// save some time when the last row of data subscribed to the same pool...
		if (lastSubscriptionPool==null || !lastSubscriptionPool.poolId.equals(pool.poolId)) {
			// to avoid interference from an already enabled repo from a prior attached subscription that also
			// contains this same packages (e.g. -htb- repos versus non -htb- repos) it would be best to remove
			// all previously attached subscriptions.  actually this will speed up the test
			SSHCommandResult result = clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
			if (result.getStderr().trim().equals("Access rate limit exceeded")) { // ExitCode: 70	Stderr: "Access rate limit exceeded"	Stdout: ""
				// Encountered a RateLimitExceededException, recover by re-registering a new consumer
				clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, ConsumerType.system, null, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null);
			}

			// subscribe to this pool
			entitlementCertFile = clienttasks.subscribeToSubscriptionPool_(pool,quantity);
			lastSubscriptionPool = pool;	// remember for the next row of data
			lastEntitlementCertFile = entitlementCertFile;	// remember for the next row of data
			Assert.assertNotNull(entitlementCertFile, "Found the entitlement cert file that was granted after subscribing to pool: "+pool);
		} else {
			entitlementCertFile = lastEntitlementCertFile;	// take from the last row of data
		}
		
		// find an available package that is uniquely provided by repo
		String pkg = clienttasks.findUniqueAvailablePackageFromRepo(repoLabel);
		if (pkg==null) {
			throw new SkipException("Could NOT find a unique available package from repo '"+repoLabel+"' after subscribing to SubscriptionPool: "+pool);
		}
		
		// install the package and assert that it is successfully installed
		SSHCommandResult yumInstallPackageResult = clienttasks.yumInstallPackageFromRepo(pkg, repoLabel, null); //pkgInstalled = true;
		
		// now remove the package
		clienttasks.yumRemovePackage(pkg);
		
		// also remove any dependencies that were installed with pkg
// FIXME: This will fail if we do not remove the dependent packages in the correct order - suppose dep-pkg1 depends on dep-pkg2 and remove dep-pkg2 first, then dep-pkg1 is already gone
// for (String depPkg : getYumDependencyPackagesInstalledFromYumInstallPackageResult(yumInstallPackageResult)) clienttasks.yumRemovePackage(depPkg);
// committing untested FIXME: ...
		List<String> depPkgsAlreadyRemoved = new ArrayList<String>();
		for (String depPkg : getYumDependencyPackagesInstalledFromYumInstallPackageResult(yumInstallPackageResult)) {
			if (!depPkgsAlreadyRemoved.contains(depPkg)) {
				depPkgsAlreadyRemoved.addAll(getYumDependencyPackagesRemovedFromYumRemovePackageResult(clienttasks.yumRemovePackage(depPkg)));
			}
		}
	}
	
	
	
	@Test(	description="subscription-manager Yum plugin: ensure content can be downloaded/installed/removed after subscribing to a personal subpool",
			groups={"Tier2Tests","InstallAndRemovePackageAfterSubscribingToPersonalSubPool_Test"},
			dataProvider="getPackageFromEnabledRepoAndPersonalSubscriptionSubPoolData",
			enabled=false)	// registered consumers type of "person" was originally intended for entitling people to training.  Red Hat Learning Management systems never made use if it, and candlepin has no active requirements for it.  Disabling the personal tests...  Reference https://bugzilla.redhat.com/show_bug.cgi?id=967160#c1
	//@ImplementsNitrateTest(caseId=) //TODO Find a tcms caseId for
	public void testInstallAndRemovePackageAfterSubscribingToPersonalSubPool(String pkg, String repoLabel, SubscriptionPool pool) throws JSONException, Exception {
		testInstallAndRemovePackageFromEnabledRepoAfterSubscribingToPool(pkg, repoLabel, pool, null);
	}
	
	
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-25986", "RHEL7-56655"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier2")
	@Test(	description="subscription-manager Yum plugin: ensure yum groups can be installed/removed",
			groups={"Tier2Tests","FipsTests","testInstallAndRemoveYumGroupFromEnabledRepoAfterSubscribingToPool"},
			dataProvider="getYumAvailableGroupFromEnabledRepoAndSubscriptionPoolData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=) //TODO Find a tcms caseId for
	public void testInstallAndRemoveYumGroupFromEnabledRepoAfterSubscribingToPool(String availableGroup, String repoLabel, SubscriptionPool pool) throws JSONException, Exception {
		if (availableGroup==null) throw new SkipException("No yum groups corresponding to enabled repo '"+repoLabel+" were found after subscribing to pool: "+pool);
		
		// remove any previously attached subscriptions
		// avoid throttling RateLimitExceededException from IT-Candlepin
		if (CandlepinType.hosted.equals(sm_serverType)) {	// strategically  get a new consumer to avoid 60 repeated API calls from the same consumer
			// re-register as a new consumer
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		} else clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
		
		// subscribe to this pool (and remember it)
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPool_(pool);
		Assert.assertNotNull(entitlementCertFile, "Found the entitlement cert file that was granted after subscribing to pool: "+pool);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		
		
		// avoid htb repos that have no content (due to snapshot timing - see Bug 1360491 - no packages available from https://cdn.redhat.com/content/htb/rhel/computenode/7/x86_64/os 
		if (clienttasks.getYumListOfAvailablePackagesFromRepo(repoLabel).isEmpty()) {
			if (repoLabel.contains("-htb-")) {
				throw new SkipException("Skipping an attempt to groupinstall from repo '"+repoLabel+"' because it is empty.  Assuming we are not within a RHEL Snapshot phase, HTB repositories are cleared out after GA and before Snapshot 1.  See https://bugzilla.redhat.com/show_bug.cgi?id=1360491#c1");
			} else {
				//	201607261756:37.637 - FINE: ssh root@hp-dl380pgen8-02-vm-7.lab.bos.redhat.com yum -y groupinstall "Compatibility Libraries" --disableplugin=rhnplugin
				//	201607261756:39.737 - FINE: Stdout: 
				//	Loaded plugins: product-id, search-disabled-repos, subscription-manager
				//	No packages in any requested group available to install or update
				//	201607261756:39.739 - FINE: Stderr: 
				//	Warning: Group compat-libraries does not have any packages to install.
				//	Maybe run: yum groups mark install (see man yum)
				//	201607261756:39.741 - FINE: ExitCode: 0
				String expectedStdout = "No packages in any requested group available to install or update";
				log.warning("This test will likely fail with '"+expectedStdout+"' because there are no available packages from repo '"+repoLabel+"'.");
			}
		}
		
		// install availableGroup
		clienttasks.yumInstallGroup(availableGroup);
		
		// remove availableGroup
		clienttasks.yumRemoveGroup(availableGroup);
		
		// TODO: add asserts for the products that get installed or deleted in stdout as a result of yum group install/remove: 
		// deleting: /etc/pki/product/7.pem
		// installing: 7.pem
		// assert the list --installed "status" for the productNamespace name that corresponds to the ContentNamespace from where this repolabel came from.
		
		// clean up
		clienttasks.unsubscribeFromSerialNumber(entitlementCert.serialNumber);
	}
	@Test(	description="subscription-manager Yum plugin: ensure yum groups can be removed/re-installed",
			groups={"Tier2Tests"},
			dataProvider="getYumInstalledGroupFromEnabledRepoAndSubscriptionPoolData",
			enabled=false)	// jsefler - I don't like this test because groups installed from the latest compose will have newer packages and required packages than available from the subscription CDN content
	//@ImplementsNitrateTest(caseId=) //TODO Find a tcms caseId for
	public void testRemoveAndInstallYumGroupFromEnabledRepoAfterSubscribingToPool(String installedGroup, String repoLabel, SubscriptionPool pool) throws JSONException, Exception {
		if (installedGroup==null) throw new SkipException("No yum groups corresponding to enabled repo '"+repoLabel+" were found after subscribing to pool: "+pool);
				
		// unsubscribe from this pool
		if (pool.equals(lastSubscribedSubscriptionPool)) clienttasks.unsubscribeFromSerialNumber(clienttasks.getSerialNumberFromEntitlementCertFile(lastSubscribedEntitlementCertFile));
		
		// before subscribing to the pool, assert that the yum groupinfo does not exist
		RemoteFileTasks.runCommandAndAssert(client, "yum groupinfo \""+installedGroup+"\" --disableplugin=rhnplugin", Integer.valueOf(0), null, "Warning: Group "+installedGroup+" does not exist.");
		
		// subscribe to this pool (and remember it)
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPool_(pool);
		Assert.assertNotNull(entitlementCertFile, "Found the entitlement cert file that was granted after subscribing to pool: "+pool);
		lastSubscribedEntitlementCertFile = entitlementCertFile;
		lastSubscribedSubscriptionPool = pool;
		
		// remove and install installedGroup
		clienttasks.yumRemoveGroup(installedGroup);
		clienttasks.yumInstallGroup(installedGroup);

		// TODO: add asserts for the products that get installed or deleted in stdout as a result of yum group install/remove: 
		// deleting: /etc/pki/product/7.pem
		// installing: 7.pem
		// assert the list --installed "status" for the productNamespace name that corresponds to the ContentNamespace from where this repolabel came from.
	}
	protected SubscriptionPool lastSubscribedSubscriptionPool = null;
	protected File lastSubscribedEntitlementCertFile = null;
	
	
	
	@Test(	description="verify redhat.repo file does not contain an excessive (more than two) number of successive blank lines",
			groups={"Tier2Tests","blockedByBug-737145"},
			enabled=false) // Disabling... this test takes too long to execute.  VerifyRedHatRepoFileIsPurgedOfBlankLinesByYumPlugin_Test effectively provides the same test coverage.
	@Deprecated
	//@ImplementsNitrateTest(caseId=)
	public void testRedHatRepoFileDoesNotContainExcessiveBlankLines_DEPRECATED() {
		
		// successive blank lines in redhat.repo must not exceed N
		int N=2; String regex = "(\\n\\s*){"+(N+2)+",}"; 	//  (\n\s*){4,}
		String redhatRepoFileContents = "";
	    
	    // check for excessive blank lines after a new register
	    clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
	    client.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		redhatRepoFileContents = client.runCommandAndWait("cat "+clienttasks.redhatRepoFile).getStdout();
		Assert.assertContainsNoMatch(redhatRepoFileContents,regex,null,"At most '"+N+"' successive blank are acceptable inside "+clienttasks.redhatRepoFile);

		// check for excessive blank lines after subscribing to each pool
	    for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
    		clienttasks.subscribe_(null,null,pool.poolId,null,null,null,null,null,null,null, null, null, null);
    		client.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError		
		}
		redhatRepoFileContents = client.runCommandAndWait("cat "+clienttasks.redhatRepoFile).getStdout();
		Assert.assertContainsNoMatch(redhatRepoFileContents,regex,null,"At most '"+N+"' successive blank are acceptable inside "+clienttasks.redhatRepoFile);

		// check for excessive blank lines after unsubscribing from each serial
		List<BigInteger> serialNumbers = new ArrayList<BigInteger>();
	    for (ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
	    	if (serialNumbers.contains(productSubscription.serialNumber)) continue;	// save some time by avoiding redundant unsubscribes
    		clienttasks.unsubscribe_(null, productSubscription.serialNumber, null, null, null, null, null);
    		serialNumbers.add(productSubscription.serialNumber);
    		client.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError		
		}
		redhatRepoFileContents = client.runCommandAndWait("cat "+clienttasks.redhatRepoFile).getStdout();
		Assert.assertContainsNoMatch(redhatRepoFileContents,regex,null,"At most '"+N+"' successive blank are acceptable inside "+clienttasks.redhatRepoFile);
		
		// assert the comment heading is present
		//Assert.assertContainsMatch(redhatRepoFileContents,"^# Red Hat Repositories$",null,"Comment heading \"Red Hat Repositories\" was found inside "+clienttasks.redhatRepoFile);
		Assert.assertContainsMatch(redhatRepoFileContents,"^# Certificate-Based Repositories$",null,"Comment heading \"Certificate-Based Repositories\" was found inside "+clienttasks.redhatRepoFile);
		Assert.assertContainsMatch(redhatRepoFileContents,"^# Managed by \\(rhsm\\) subscription-manager$",null,"Comment heading \"Managed by (rhsm) subscription-manager\" was found inside "+clienttasks.redhatRepoFile);		
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-20083", "RHEL7-51096"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier1")
	@Test(	description="verify redhat.repo file is purged of successive blank lines by subscription-manager yum plugin",
			groups={"Tier1Tests","blockedByBug-737145","blockedByBug-838113","blockedByBug-924919","blockedByBug-979492","blockedByBug-1017969","blockedByBug-1035440"},	/* yum stdout/stderr related bugs 872310 901612 1017354 1017969 */
			enabled=true)
	//@ImplementsNitrateTest(caseId=) //TODO Find a tcms caseId for
	public void testRedHatRepoFileIsPurgedOfBlankLinesByYumPlugin() {
		
		// TEMPORARY WORKAROUND
		if (clienttasks.arch.equals("ppc64le")) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1156638"; // Bug 1156638 - "Red Hat Enterprise Linux for IBM POWER" subscriptions need to provide content for arch "ppc64le"
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("Skipping this test on arch '"+clienttasks.arch+"' while blocking bug '"+bugId+"' is open.");
			}
		}
		// END OF WORKAROUND
		
		// successive blank lines in redhat.repo must not exceed N
		int N=2; String regex = "(\\n\\s*){"+(N+2)+",}"; 	//  (\n\s*){4,}
		String redhatRepoFileContents = null;
	    
		// adding the following call to login and yum repolist to compensate for change of behavior introduced by Bug 781510 - 'subscription-manager clean' should delete redhat.repo
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null,(List<String>)null, null, null, null, null, null, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();	// TODO subscribing to all is overkill; only one content providing pool is sufficient
	    if (clienttasks.isPackageVersion("subscription-manager", "<", "1.10.3-1")) {	// yum trigger is automatic after Bug 1008016 - [RFE] The redhat.repo file should be refreshed after a successful subscription
	    	client.runCommandAndWait("yum --quiet repolist --disableplugin=rhnplugin --disablerepo=*"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
	    }
	    redhatRepoFileContents = client.runCommandAndWait("cat "+clienttasks.redhatRepoFile).getStdout();
		Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"Expecting the redhat repo file '"+clienttasks.redhatRepoFile+"' to exist after unregistering.");
		Assert.assertContainsNoMatch(redhatRepoFileContents,regex,null,"At most '"+N+"' successive blank are acceptable inside "+clienttasks.redhatRepoFile);
	
	    // check for excessive blank lines after unregister
	    clienttasks.unregister(null,null,null, null);
    	client.runCommandAndWait("yum --quiet repolist --disableplugin=rhnplugin --disablerepo=*"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
	    //Assert.assertTrue(client.getStderr().contains("Unable to read consumer identity"),"Yum repolist should not touch redhat.repo when there is no consumer and state in stderr 'Unable to read consumer identity'.");	// TODO 8/9/2012 FIND OUT WHAT BUG CAUSED THIS CHANGE IN EXPECTED STDERR
	    //Assert.assertEquals(client.getStderr().trim(),"","Stderr from prior command");	// changed by Bug 901612 - Subscription-manager-s yum plugin prints warning to stdout instead of stderr.
//	    String expectedStderr = "This system is not registered to Red Hat Subscription Management. You can use subscription-manager to register.";
//	    Assert.assertTrue(client.getStderr().contains(expectedStderr),"Stderr from prior command should show subscription-manager plugin warning '"+expectedStderr+"'.  See https://bugzilla.redhat.com/show_bug.cgi?id=901612 ");	// 901612 was reverted by 1017354
	    if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.12-1")) {	// Bug 1017354 - yum subscription-manager plugin puts non-error information on stderr; subscription-manager commit 39eadae14eead4bb79978e52d38da2b3e85cba57
	    	
			// TEMPORARY WORKAROUND
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1282961"; // Bug 1282961 - Plugin "search-disabled-repos" requires API 2.7. Supported API is 2.6.
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen && clienttasks.redhatReleaseX.equals("6") && clienttasks.isPackageVersion("subscription-manager", ">=", "1.15")) {
			    Assert.assertEquals((client.getStdout()+client.getStderr()).replace("Plugin \"search-disabled-repos\" requires API 2.7. Supported API is 2.6.", "").trim(),"","Ignoring bug '"+bugId+"', Stdout+Stderr from prior command should be blank due to --quiet option.");
			} else
			// END OF WORKAROUND
	    	Assert.assertEquals((client.getStdout()+client.getStderr()).trim(),"","Stdout+Stderr from prior command should be blank due to --quiet option.");	// Bug 1017969 - subscription manager plugin ignores yum --quiet
	    }
	    Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"Expecting the redhat repo file '"+clienttasks.redhatRepoFile+"' to exist after unregistering.");
		redhatRepoFileContents = client.runCommandAndWait("cat "+clienttasks.redhatRepoFile).getStdout();
		Assert.assertContainsNoMatch(redhatRepoFileContents,regex,null,"At most '"+N+"' successive blank are acceptable inside '"+clienttasks.redhatRepoFile+"' after unregistering.");

		log.info("Inserting blank lines into the redhat.repo for testing purposes...");
		client.runCommandAndWait("for i in `seq 1 10`; do echo \"\" >> "+clienttasks.redhatRepoFile+"; done; echo \"# test for bug 737145\" >> "+clienttasks.redhatRepoFile);
		redhatRepoFileContents = client.runCommandAndWait("cat "+clienttasks.redhatRepoFile).getStdout();
		Assert.assertContainsMatch(redhatRepoFileContents,regex,null,"File "+clienttasks.redhatRepoFile+" has been infiltrated with excessive blank lines.");
	    client.runCommandAndWait("yum --quiet repolist --disableplugin=rhnplugin --disablerepo=*"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
	    //Assert.assertTrue(client.getStderr().contains("Unable to read consumer identity"),"Yum repolist should not touch redhat.repo when there is no consumer and state in stderr 'Unable to read consumer identity'.");	// TODO 8/9/2012 FIND OUT WHAT BUG CAUSED THIS CHANGE IN EXPECTED STDERR
	    //Assert.assertEquals(client.getStderr().trim(),"","Stderr from prior command");	// changed by Bug 901612 - Subscription-manager-s yum plugin prints warning to stdout instead of stderr.
//		Assert.assertTrue(client.getStderr().contains(expectedStderr),"Stderr from prior command should show subscription-manager plugin warning '"+expectedStderr+"'.  See https://bugzilla.redhat.com/show_bug.cgi?id=901612 ");	// 901612 was reverted by 1017354
	    if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.12-1")) {	// Bug 1017354 - yum subscription-manager plugin puts non-error information on stderr; subscription-manager commit 39eadae14eead4bb79978e52d38da2b3e85cba57
			// TEMPORARY WORKAROUND
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1282961"; // Bug 1282961 - Plugin "search-disabled-repos" requires API 2.7. Supported API is 2.6.
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen && clienttasks.redhatReleaseX.equals("6") && clienttasks.isPackageVersion("subscription-manager", ">=", "1.15")) {
			    Assert.assertEquals((client.getStdout()+client.getStderr()).replace("Plugin \"search-disabled-repos\" requires API 2.7. Supported API is 2.6.", "").trim(),"","Ignoring bug '"+bugId+"', Stdout+Stderr from prior command should be blank due to --quiet option.");
			} else
			// END OF WORKAROUND
	    	Assert.assertEquals((client.getStdout()+client.getStderr()).trim(),"","Stdout+Stderr from prior command should be blank due to --quiet option.");	// Bug 1017969 - subscription manager plugin ignores yum --quiet
	    }
		String redhatRepoFileContents2 = client.runCommandAndWait("cat "+clienttasks.redhatRepoFile).getStdout();
		Assert.assertContainsMatch(redhatRepoFileContents2,regex,null,"File "+clienttasks.redhatRepoFile+" is still infiltrated with excessive blank lines.");
		Assert.assertEquals(redhatRepoFileContents2, redhatRepoFileContents,"File "+clienttasks.redhatRepoFile+" remains unchanged when there is no consumer.");

		// trigger the yum plugin for subscription-manager (after registering again)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null,(List<String>)null, null, null, null, null, null, null, null, null, null);
	    // inject additional assertion logic after fix for Bug 1035440 - subscription-manager yum plugin makes yum refresh all RHSM repos. on every command.
	    if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.11.3-5")) {	// Bug 1017354 - yum subscription-manager plugin puts non-error information on stderr; subscription-manager commit 39eadae14eead4bb79978e52d38da2b3e85cba57
	    	// assert redhatRepoFileContents2 equals redhatRepoFileContents because redhat.repo content should not have changed after implementation of bug 1025440
	    	Assert.assertEquals(redhatRepoFileContents, redhatRepoFileContents2, "Contents of "+clienttasks.redhatRepoFile+" should remain unchanged despite new registration and yum trigger because the lack of entitlements do not necessitate writing a new repo file.");
	    	// now add entitlements which should cause a purge of blank spaces while writing the redhat.repo file
			clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();	// TODO subscribing to all is overkill; only one content providing pool is sufficient
	    } else {
		log.info("Triggering the yum plugin for subscription-manager..."/* which will purge the blank lines from redhat.repo..."*/);
		client.runCommandAndWait("yum --quiet repolist --disableplugin=rhnplugin --disablerepo=*"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
	    }
		redhatRepoFileContents = client.runCommandAndWait("cat "+clienttasks.redhatRepoFile).getStdout();
	    Assert.assertContainsNoMatch(redhatRepoFileContents,regex,null,"At most '"+N+"' successive blank are acceptable inside '"+clienttasks.redhatRepoFile+"' after reregistering.");
		
		// assert the comment heading is present
		//Assert.assertContainsMatch(redhatRepoFileContents,"^# Red Hat Repositories$",null,"Comment heading \"Red Hat Repositories\" was found inside "+clienttasks.redhatRepoFile);
		Assert.assertContainsMatch(redhatRepoFileContents,"^# Certificate-Based Repositories$",null,"Comment heading \"Certificate-Based Repositories\" was found inside "+clienttasks.redhatRepoFile);
		Assert.assertContainsMatch(redhatRepoFileContents,"^# Managed by \\(rhsm\\) subscription-manager$",null,"Comment heading \"Managed by (rhsm) subscription-manager\" was found inside "+clienttasks.redhatRepoFile);		
		Assert.assertContainsMatch(redhatRepoFileContents,"^# test for bug 737145$",null,"User defined comment remains inside "+clienttasks.redhatRepoFile+" after triggering the yum plugin for subscription-manager.");		
	}
	@Test(	description="verify redhat.repo file is purged of successive blank lines by subscription-manager yum plugin",
			groups={"Tier1Tests","blockedByBug-737145"},
			enabled=false)	// was valid before bug fix 781510
	@Deprecated
	//@ImplementsNitrateTest(caseId=) //TODO Find a tcms caseId for
	public void testRedHatRepoFileIsPurgedOfBlankLinesByYumPlugin_DEPRECATED() {
		
		// successive blank lines in redhat.repo must not exceed N
		int N=2; String regex = "(\\n\\s*){"+(N+2)+",}"; 	//  (\n\s*){4,}
		String redhatRepoFileContents = "";
	    
	    // check for excessive blank lines after unregister
	    clienttasks.unregister(null,null,null, null);
	    client.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		redhatRepoFileContents = client.runCommandAndWait("cat "+clienttasks.redhatRepoFile).getStdout();
		Assert.assertContainsNoMatch(redhatRepoFileContents,regex,null,"At most '"+N+"' successive blank are acceptable inside "+clienttasks.redhatRepoFile);

		log.info("Inserting blank lines into the redhat.repo for testing purposes...");
		client.runCommandAndWait("for i in `seq 1 10`; do echo \"\" >> "+clienttasks.redhatRepoFile+"; done; echo \"# test for bug 737145\" >> "+clienttasks.redhatRepoFile);
		redhatRepoFileContents = client.runCommandAndWait("cat "+clienttasks.redhatRepoFile).getStdout();
		Assert.assertContainsMatch(redhatRepoFileContents,regex,null,"File "+clienttasks.redhatRepoFile+" has been infiltrated with excessive blank lines.");

		// trigger the yum plugin for subscription-manager
		log.info("Triggering the yum plugin for subscription-manager which will purge the blank lines from redhat.repo...");
	    client.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		redhatRepoFileContents = client.runCommandAndWait("cat "+clienttasks.redhatRepoFile).getStdout();
		Assert.assertContainsNoMatch(redhatRepoFileContents,regex,null,"At most '"+N+"' successive blank are acceptable inside "+clienttasks.redhatRepoFile);
		
		// assert the comment heading is present
		//Assert.assertContainsMatch(redhatRepoFileContents,"^# Red Hat Repositories$",null,"Comment heading \"Red Hat Repositories\" was found inside "+clienttasks.redhatRepoFile);
		Assert.assertContainsMatch(redhatRepoFileContents,"^# Certificate-Based Repositories$",null,"Comment heading \"Certificate-Based Repositories\" was found inside "+clienttasks.redhatRepoFile);
		Assert.assertContainsMatch(redhatRepoFileContents,"^# Managed by \\(rhsm\\) subscription-manager$",null,"Comment heading \"Managed by (rhsm) subscription-manager\" was found inside "+clienttasks.redhatRepoFile);		
	}





	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-36652", "RHEL7-51484"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier2")
	@Test(	description="Verify that a 185 content set product subscription is always subscribable",
			groups={"Tier2Tests","SubscribabilityOfContentSetProduct_Tests","blockedByBug-871146","blockedByBug-905546"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testSubscribabilityOfSKUProvidingA185ContentSetProduct() {

		Map<String,String> factsMap = new HashMap<String,String>();
		File entitlementCertFile;
		EntitlementCert entitlementCert;
		String systemCertificateVersionFactValue;
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", subscriptionSKUProvidingA185ContentSetProduct, clienttasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNotNull(pool,"Found an available pool to subscribe to productId '"+subscriptionSKUProvidingA185ContentSetProduct+"': "+pool);
		
		// test that it IS subscribable when system.certificate_version: None
		factsMap.put("system.certificate_version", null);
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		Assert.assertEquals(clienttasks.getFactValue("system.certificate_version"), "None", "When the system.certificate_version fact is null, its fact value is reported as 'None'.");
		//entitlementCertFile = clienttasks.subscribeToProductId(skuTo185ContentSetProduct);
		//entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null, null, null);
		entitlementCert = clienttasks.getEntitlementCertCorrespondingToSubscribedPool(pool);
		entitlementCertFile = clienttasks.getEntitlementCertFileFromEntitlementCert(entitlementCert);
		Assert.assertEquals(entitlementCert.version,"1.0","When the system.certificate_version fact is null, the version of the entitlement certificate granted by candlepin is '1.0'.");
		Assert.assertEquals(entitlementCert.contentNamespaces.size(), 185, "The number of content sets provided in the version 1.0 entitlement cert parsed using the rct cat-cert tool.");
		entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFileUsingOpensslX509(entitlementCertFile);
		Assert.assertEquals(entitlementCert.contentNamespaces.size(), 185, "The number of content sets provided in this version '"+entitlementCert.version+"' entitlement cert parsed using the openssl x509 tool.");
		clienttasks.assertEntitlementCertsInYumRepolist(Arrays.asList(entitlementCert), true);
		//clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.unsubscribeFromSerialNumber(clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCertFile));
		
		// test that it IS subscribable when system.certificate_version: 1.0
		factsMap.put("system.certificate_version", "1.0");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		Assert.assertEquals(clienttasks.getFactValue("system.certificate_version"), "1.0", "When the system.certificate_version fact is 1.0, its fact value is reported as '1.0'.");
		//entitlementCertFile = clienttasks.subscribeToProductId(skuTo185ContentSetProduct);
		//entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null, null, null);
		entitlementCert = clienttasks.getEntitlementCertCorrespondingToSubscribedPool(pool);
		entitlementCertFile = clienttasks.getEntitlementCertFileFromEntitlementCert(entitlementCert);
		Assert.assertEquals(entitlementCert.version,"1.0","When the system.certificate_version fact is 1.0, the version of the entitlement certificate granted by candlepin is '1.0'.");
		Assert.assertEquals(entitlementCert.contentNamespaces.size(), 185, "The number of content sets provided in the version 1.0 entitlement cert parsed using the rct cat-cert tool.");
		entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFileUsingOpensslX509(entitlementCertFile);
		Assert.assertEquals(entitlementCert.contentNamespaces.size(), 185, "The number of content sets provided in this version '"+entitlementCert.version+"' entitlement cert parsed using the openssl x509 tool.");
		clienttasks.assertEntitlementCertsInYumRepolist(Arrays.asList(entitlementCert), true);
		//clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.unsubscribeFromSerialNumber(clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCertFile));

		// test that it IS subscribable when system.certificate_version is the system's default value (should be >=3.0)
		clienttasks.deleteFactsFileWithOverridingValues();
		systemCertificateVersionFactValue = clienttasks.getFactValue("system.certificate_version");
		Assert.assertTrue(Float.valueOf(systemCertificateVersionFactValue)>=3.0, "The actual default system.certificate_version fact '"+systemCertificateVersionFactValue+"' is >= 3.0.");
		//entitlementCertFile = clienttasks.subscribeToProductId(skuTo185ContentSetProduct);
		//entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null, null, null);
		entitlementCert = clienttasks.getEntitlementCertCorrespondingToSubscribedPool(pool);
		entitlementCertFile = clienttasks.getEntitlementCertFileFromEntitlementCert(entitlementCert);	
		// TOO ASSERTIVE  Assert.assertTrue(Float.valueOf(entitlementCert.version)<=Float.valueOf(systemCertificateVersionFactValue),"The version of the entitlement certificate '"+entitlementCert.version+"' granted by candlepin is less than or equal to the system.certificate_version '"+systemCertificateVersionFactValue+"' which indicates the maximum certificate version this system knows how to handle.");	// This assert was too assertive according to https://bugzilla.redhat.com/show_bug.cgi?id=1425236#c2
		Assert.assertEquals(Float.valueOf(entitlementCert.version).intValue(),Float.valueOf(systemCertificateVersionFactValue).intValue(),"The major value of the entitlement certificate '"+entitlementCert.version+"' granted by candlepin matches the major value of the system.certificate_version '"+systemCertificateVersionFactValue+"' which indicates certificate compatibility.");	// Reference: https://bugzilla.redhat.com/show_bug.cgi?id=1425236#c2
		Assert.assertEquals(entitlementCert.contentNamespaces.size(), 185, "The number of content sets provided in this version '"+entitlementCert.version+"' entitlement cert parsed using the rct cat-cert tool.");
		clienttasks.assertEntitlementCertsInYumRepolist(Arrays.asList(entitlementCert), true);
		//clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.unsubscribeFromSerialNumber(clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCertFile));
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-36653", "RHEL7-51486"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier2")
	@Test(	description="Verify that a 186 content set product subscription is subscribable only when system.certificate_version >= 3.0",
			groups={"Tier2Tests","SubscribabilityOfContentSetProduct_Tests","blockedByBug-871146","blockedByBug-905546"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testSubscribabilityOfSKUProvidingA186ContentSetProduct() {
		VerifySubscribabilityOfSKUProvidingTooManyContentSets(subscriptionSKUProvidingA186ContentSetProduct,186);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-36654", "RHEL7-51487"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier2")
	@Test(	description="Verify that a subscription providing two 93 content set products is subscribable only when system.certificate_version >= 3.0",
			groups={"Tier2Tests","SubscribabilityOfContentSetProduct_Tests","blockedByBug-879022","blockedByBug-905546"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testSubscribabilityOfSKUProvidingTwo93ContentSetProducts() {
		VerifySubscribabilityOfSKUProvidingTooManyContentSets(subscriptionSKUProvidingTwo93ContentSetProducts,93*2);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-20086", "RHEL7-51097"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier1")
	@Test(	description="Verify that yum vars used in a baseurl are listed in a yum repo parameter called ui_repoid_vars",
			groups={"Tier1Tests","blockedByBug-906554"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumRepoUiRepoIdVars() throws JSONException, Exception {
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);

		// subscribe to available subscriptions
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		
		// process each of the yum repos granted and assert the yum vars contained in the baseurl are listed in the ui_repoid_vars
		boolean UiRepoIdVarsTested=false;
		for (YumRepo yumRepo : clienttasks.getCurrentlySubscribedYumRepos()) {
			log.info("Asserting Yum Repo: "+yumRepo);
			Pattern p = Pattern.compile("\\$\\w+");	// baseurl = https://cdn.redhat.com/content/dist/rhel/server/5/$releasever/$basearch/jbeap/5/os
			Matcher matcher = p.matcher(yumRepo.baseurl);
			
			// first, check if the baseurl has any yum vars
			if (!matcher.find()) {
				// assert that this yumRepo has no ui_repoid_vars configuration
				Assert.assertNull(yumRepo.ui_repoid_vars, "When baseurl '"+yumRepo.baseurl+"' of yumRepo '"+yumRepo.id+"' contains no yum vars, then configuration ui_repoid_vars is not required.");
				continue;
			}
			
			// now make sure all of the yum vars in the baseurl are present in the ui_repoid_vars configuration
			matcher.reset();
			List<String> actualUiRepoidVars = Arrays.asList(yumRepo.ui_repoid_vars.trim().split("\\s+"));
			while (matcher.find()) {
				UiRepoIdVarsTested = true;
				String yumVar = matcher.group();
				
				// assert that the configured ui_repoid_vars contains this yum var
				Assert.assertTrue(actualUiRepoidVars.contains(yumVar.replaceFirst("^\\$","")), "The ui_repoid_vars configuration in repo id '"+yumRepo.id+"' contains yum var '"+yumVar+"' used in its baseurl '"+yumRepo.baseurl+"'.");
			}
			
			// TODO on RHEL7 we should learn how the yum vars get propagated to the ui when calling yum repolist  (dgregor probably knows)
			//	[root@jsefler-7 ~]# yum repolist
			//	Loaded plugins: langpacks, product-id, security, subscription-manager
			//	This system is receiving updates from Red Hat Subscription Management.
			//	repo id                                                                                           repo name                                                                               status
			//	always-enabled-content/6.92Server                                                                 always-enabled-content                                                                  0
			//	awesomeos/6.92Server/x86_64                                                                       awesomeos                                                                               0
			//	repolist: 0
			// Notice the repoid labels are appended with /$releasever and /$releasever/$basearch
		}
		if (!UiRepoIdVarsTested) throw new SkipException("Could not find any YumRepos containing yum vars to assert this test.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-20080", "RHEL7-33076"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier1")
	@Test(	description="Verify that all content sets granted from a subscription pool that are restricted to specific arches satisfy the current system's arch.",
			groups={"Tier1Tests","blockedByBug-706187","blockedByBug-975520","VerifyArchRestrictedContentSetsEntitledAfterSubscribeAllSatisfiesTheSystemArch_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testArchRestrictedContentSetsEntitledAfterSubscribeAllSatisfiesTheSystemArch() throws JSONException, Exception {
		// get a list of all of the available poolIds that provide arch-based content sets
		List<List<Object>> subscriptionPoolsDataList = getAllAvailableSubscriptionPoolsProvidingArchBasedContentDataAsListOfLists();
		List<String> archBasedSubscriptionPoolIds = new ArrayList<String>();
		for (List<Object> subscriptionPoolsData: subscriptionPoolsDataList) {
			SubscriptionPool pool = (SubscriptionPool)subscriptionPoolsData.get(0);
			archBasedSubscriptionPoolIds.add(pool.poolId);
		}
		if (archBasedSubscriptionPoolIds.isEmpty()) throw new SkipException("No subscriptions were found providing non-empty arch-based content.");
		
		// iterate over several possible system arches
		Map<String, String> factsMap = new HashMap<String, String>();
		for (String systemArch : Arrays.asList(new String[]{"i386","i586","i686","x86_64","ppc","ppc64","ia64","arm","s390","s390x"})) {
			
			// avoid throttling RateLimitExceededException from IT-Candlepin
			if (CandlepinType.hosted.equals(sm_serverType)) {	// strategically get a new consumer to avoid 60 repeated API calls from the same consumer
				// re-register as a new consumer
				clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
			} else // clienttasks.unsubscribe(true, (BigInteger)null, null, null, null);
			
			// return all current entitlements (Note: system is already registered by getAllAvailableSubscriptionPoolsProvidingArchBasedContentDataAsListOfLists())
 			clienttasks.unsubscribe(true, (BigInteger)null, null, null, null, null, null);
			
			// fake the system's arch and update the facts
			log.info("Manipulating the system facts into thinking this is a '"+systemArch+"' system...");
			factsMap.put("uname.machine", String.valueOf(systemArch));
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null, null);
			
			// subscribe to all the arch-based content set pools
			clienttasks.subscribe(false, null, archBasedSubscriptionPoolIds, null, null, null, null, null, null, null, null, null, null);
			
			// iterate over all of the granted entitlements
			for (EntitlementCert entitlementCert : clienttasks.getCurrentEntitlementCerts()) {
				for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
					if (contentNamespace.arches==null) Assert.fail("This version of subscription-manager does not appear to parse arch restricted content.  Upgrade to a newer build of subscription-manager.");
					List<String> arches = new ArrayList<String>();
					if (!contentNamespace.arches.trim().isEmpty()) arches.addAll(Arrays.asList(contentNamespace.arches.trim().split(" *, *")));	// Note: the arches field can be a comma separated list of values
					if (arches.contains("x86")) {arches.addAll(Arrays.asList("i386","i486","i586","i686"));}  // Note: x86 is a general arch to cover all 32-bit intel microprocessors 
					Assert.assertTrue(arches.isEmpty() || arches.contains("ALL") || arches.contains("noarch") || arches.contains(systemArch), "Content label '"+contentNamespace.label+"' restricted to arches '"+contentNamespace.arches+"' granted by entitlement cert '"+entitlementCert.orderNamespace.productName+"' matches the system's arch '"+systemArch+"'.");
				}
			}
		}
	}
	@AfterGroups(groups={"setup"}, value={"VerifyArchRestrictedContentSetsEntitledAfterSubscribeAllSatisfiesTheSystemArch_Test"})
	@AfterClass(groups={"setup"})	// insurance; not really needed
	public void deleteFactsFileWithOverridingValues() {
		if (clienttasks!=null) clienttasks.deleteFactsFileWithOverridingValues();
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-20081", "RHEL7-50720"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier1")
	@Test(	description="Verify that all content sets granted from a subscription pool satisfy the system arch and subset the provided product's arch",
			groups={"Tier1Tests","blockedByBug-706187","blockedByBug-975520"},
			dataProvider="getAllAvailableSubscriptionPoolsProvidingArchBasedContentData",//"getAvailableSubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testContentSetsEntitledFromSubscriptionPoolSatisfyTheSystemArch(SubscriptionPool pool) throws JSONException, Exception {
		List<String> providedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername,sm_clientPassword,sm_serverUrl,pool.poolId);
		if (providedProductIds.isEmpty()) throw new SkipException("This test is not applicable for a pool that provides no products.");
		JSONObject jsonStatus = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(/*authenticator*/null,/*password*/null,sm_serverUrl,"/status"));
		JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+pool.poolId));

		// maintain a list of expected content sets
		Set<ContentNamespace> expectedContentNamespaceSet = new HashSet<ContentNamespace>();
		// maintain a list of unexpected content sets
		Set<ContentNamespace> unexpectedContentNamespaceSet = new HashSet<ContentNamespace>();

		for (String providedProductId : providedProductIds) {
			
			// get the product
			String path = "/products/"+providedProductId;
			if (SubscriptionManagerTasks.isVersion(jsonStatus.getString("version"),">=","2.0.11")) path = jsonPool.getJSONObject("owner").getString("href")+path;	// starting with candlepin-2.0.11 /products/<ID> are requested by /owners/<KEY>/products/<ID> OR /products/<UUID>
			JSONObject jsonProduct = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,path));	
			
			// get the product supported arches
			JSONArray jsonProductAttributes = jsonProduct.getJSONArray("attributes");
			List<String> productSupportedArches = new ArrayList<String>();
			for (int j = 0; j < jsonProductAttributes.length(); j++) {
				JSONObject jsonProductAttribute = (JSONObject) jsonProductAttributes.get(j);
				String attributeName = jsonProductAttribute.getString("name");
				String attributeValue = jsonProductAttribute.isNull("value")? null:jsonProductAttribute.getString("value");
				if (attributeName.equals("arch")) {
					productSupportedArches.addAll(Arrays.asList(attributeValue.trim().split("\\s*,\\s*")));	// Note: the arch attribute can be a comma separated list of values
					if (productSupportedArches.contains("x86")) {productSupportedArches.addAll(Arrays.asList("i386","i486","i586","i686"));}  // Note: x86 is a general arch to cover all 32-bit intel microprocessors 
				}
			}
			
			// get the provided product contents
			JSONArray jsonProductContents = jsonProduct.getJSONArray("productContent");
			for (int j = 0; j < jsonProductContents.length(); j++) {
				JSONObject jsonProductContent = (JSONObject) jsonProductContents.get(j);
				JSONObject jsonContent = jsonProductContent.getJSONObject("content");
				Map<String,String> certData = new HashMap<String,String>();
				//certData.put("", jsonContent.getString("id"));
				if (jsonContent.has("type") && !jsonContent.isNull("type")) certData.put("type",jsonContent.getString("type"));
				if (jsonContent.has("label") && !jsonContent.isNull("label")) certData.put("label",jsonContent.getString("label"));
				if (jsonContent.has("name") && !jsonContent.isNull("name")) certData.put("name",jsonContent.getString("name"));
				if (jsonContent.has("vendorId") && !jsonContent.isNull("vendorId")) certData.put("vendorId",jsonContent.getString("vendor"));
				if (jsonContent.has("downloadUrl") && !jsonContent.isNull("downloadUrl")) certData.put("downloadUrl",jsonContent.getString("contentUrl"));
				if (jsonContent.has("requiredTags") && !jsonContent.isNull("requiredTags")) certData.put("requiredTags",jsonContent.getString("requiredTags"));
				//certData.put("", jsonContent.getString("releaseVer"));
				if (jsonContent.has("gpgKeyUrl") && !jsonContent.isNull("gpgKeyUrl")) certData.put("gpgKeyUrl",jsonContent.getString("gpgUrl"));
				if (jsonContent.has("metadataExpire") && !jsonContent.isNull("metadataExpire")) certData.put("metadataExpire",String.valueOf(jsonContent.getInt("metadataExpire")));
				//certData.put("", jsonContent.getString("modifiedProductIds"));
				if (jsonContent.has("arches") && !jsonContent.isNull("arches")) certData.put("arches",jsonContent.getString("arches"));
				ContentNamespace contentNamespace = new ContentNamespace(certData);

//				// get modifiedProductIds for each of the productContents
//				JSONArray jsonModifiedProductIds = jsonContent.getJSONArray("modifiedProductIds");
//				for (int k = 0; k < jsonModifiedProductIds.length(); k++) {
//					String modifiedProductId = (String) jsonModifiedProductIds.get(k);
//				}
				
				// get this content supported arches
				Set<String> contentSupportedArches = new HashSet<String>();
				String jsonContentArches = null;
				if (jsonContent.has("arches") && !jsonContent.isNull("arches") && !jsonContent.getString("arches").isEmpty()) {
					jsonContentArches = jsonContent.getString("arches");
					contentSupportedArches.addAll(Arrays.asList(jsonContentArches.split("\\s*,\\s*")));
					if (contentSupportedArches.contains("x86")) contentSupportedArches.addAll(Arrays.asList("i386","i486","i586","i686"));  // Note: x86 is a general arch to cover all 32-bit intel microprocessors 
					/* NOPE: THIS CONCEPT IS NOT WHAT RELEASE ENGINEERING WANTS.  DO NOT TOLERATE THIS BEHAVIOR; SEE Bug 975520 - content availability based on arches is currently too tolerant
					if (contentSupportedArches.contains("i386")) contentSupportedArches.add("x86_64");  // Note: all i386 packages are capable of running on an x86_64 system
					if (contentSupportedArches.contains("i486")) contentSupportedArches.add("x86_64");  // Note: all i486 packages are capable of running on an x86_64 system
					if (contentSupportedArches.contains("i586")) contentSupportedArches.add("x86_64");  // Note: all i586 packages are capable of running on an x86_64 system
					if (contentSupportedArches.contains("i686")) contentSupportedArches.add("x86_64");  // Note: all i686 packages are capable of running on an x86_64 system
					*/
					
					// when arches have been defined on the content set, then add contentNamespace to the expectedContentNamespaces, but only if
					// it contains an arch that matches the system
					if (contentSupportedArches.contains("ALL") || contentSupportedArches.contains("noarch") || contentSupportedArches.contains(clienttasks.arch)) {
						expectedContentNamespaceSet.add(contentNamespace);
					} else {
						unexpectedContentNamespaceSet.add(contentNamespace);
					}
					
				} else {
					// when no arches have been defined on the content set, then add it to the expectedContentLabels, but only if
					// it's providedProduct also matches the system  (we are effectively inheriting the arches defined by the product to which this content was added)
					
					// TODO: NOT SURE HOW TOLERANT WE WANT TO BE FOR CONTENT SETS THAT INHERIT FROM THEIR PRODUCTS
					if (productSupportedArches.contains("ALL") || productSupportedArches.contains(clienttasks.arch)) {
						expectedContentNamespaceSet.add(contentNamespace);
					} else {
						unexpectedContentNamespaceSet.add(contentNamespace);
					}
				}
			}
		}
		
		// conflict situation: if a subscription provides more than one product that both provide the same content but whose product's arch differs, then it is possible to have the content in both expectedContentLabels and unexpectedContentLabels; expectedContentLabels wins!
		for (ContentNamespace expectedContentNamespace : expectedContentNamespaceSet) {
			List<ContentNamespace> unexpectedContentNamespaceList = new ArrayList<ContentNamespace>();
			unexpectedContentNamespaceList.addAll(unexpectedContentNamespaceSet);
			ContentNamespace unexpectedContentNamespace = ContentNamespace.findFirstInstanceWithMatchingFieldFromList("label", expectedContentNamespace.label, unexpectedContentNamespaceList);
			if (unexpectedContentNamespace!=null) {
				log.warning("Based on multiple products '"+providedProductIds+"' from subscription '"+pool.subscriptionName+"' with conflicting arches, content label '"+expectedContentNamespace.label+"' defined for arches '"+expectedContentNamespace.arches+"' will be provided."); 
				unexpectedContentNamespaceSet.remove(unexpectedContentNamespace);
			}
		}
		
		if (expectedContentNamespaceSet.isEmpty() && unexpectedContentNamespaceSet.isEmpty()) throw new SkipException("This test is not applicable for a pool whose provided products have no content sets.");
		
		// avoid throttling RateLimitExceededException from IT-Candlepin
		if (!poolIds.contains(pool.poolId) && CandlepinType.hosted.equals(sm_serverType)) {	// strategically get a new consumer to avoid 60 repeated API calls from the same consumer
			// re-register as a new consumer
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		}
		poolIds.add(pool.poolId);
		
		clienttasks.unsubscribe(true,(BigInteger)null,null,null,null, null, null);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.subscribeToSubscriptionPool(pool,/*sm_serverAdminUsername*/sm_clientUsername,/*sm_serverAdminPassword*/sm_clientPassword,sm_serverUrl));
		
		// adjust the expectedContentNamespaces for modified product ids that are not installed
		//List<ProductCert> installedProductCerts = clienttasks.getCurrentProductCerts();
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		for (String providedProductId : providedProductIds) {
			
			// get the product
			String path = "/products/"+providedProductId;
			if (SubscriptionManagerTasks.isVersion(jsonStatus.getString("version"),">=","2.0.11")) path = jsonPool.getJSONObject("owner").getString("href")+path;	// starting with candlepin-2.0.11 /products/<ID> are requested by /owners/<KEY>/products/<ID> OR /products/<UUID>
			JSONObject jsonProduct = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,path));	
			
			// get the provided product contents
			JSONArray jsonProductContents = jsonProduct.getJSONArray("productContent");
			for (int j = 0; j < jsonProductContents.length(); j++) {
				JSONObject jsonProductContent = (JSONObject) jsonProductContents.get(j);
				JSONObject jsonContent = jsonProductContent.getJSONObject("content");
				
				// get modifiedProductIds for each of the productContents
				JSONArray jsonModifiedProductIds = jsonContent.getJSONArray("modifiedProductIds");
				for (int k = 0; k < jsonModifiedProductIds.length(); k++) {
					String modifiedProductId = (String) jsonModifiedProductIds.get(k);
					String contentLabel = jsonContent.getString("label");
					
// TODO: I do not believe this should check the installed products or all the current subscriptions' providedProductIds for this modifiedProductId
//					// if modifiedProductId is not installed, then the modifier jsonContent should NOT be among the expectedContentNamespaceSet
//					if (InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", modifiedProductId, installedProductCerts)==null) {
//						ContentNamespace contentNamespace = ContentNamespace.findFirstInstanceWithMatchingFieldFromList("label", jsonContent.getString("label"), new ArrayList<ContentNamespace>(expectedContentNamespaceSet));
//						if (contentNamespace!=null) { 
//							log.warning("ContentNamespace label '"+contentNamespace.label+"' modifies product id '"+modifiedProductId+"' which is NOT installed and should therefore not be among the entitled content namespaces no matter what its arch ("+contentNamespace.arches+") may be.");
//							unexpectedContentNamespaceSet.add(contentNamespace);
//							expectedContentNamespaceSet.remove(contentNamespace);
//						}
//					}
// DONE: Implemented the second thought by the following test block
					
					// if modifiedProductId is not provided by the currently consumed subscriptions, then the modifier jsonContent should NOT be among the expectedContentNamespaceSet
					Set<String> providedProductIdsByCurrentlyConsumedProductSubscriptions = new HashSet<String>();
					for (ProductSubscription productSubscription : consumedProductSubscriptions) {
						for (String providedProductIdByCurrentlyConsumedProductSubscription : CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername,sm_clientPassword,sm_serverUrl,pool.poolId)) {
							providedProductIdsByCurrentlyConsumedProductSubscriptions.add(providedProductIdByCurrentlyConsumedProductSubscription);
						}
					}
					if (!providedProductIdsByCurrentlyConsumedProductSubscriptions.contains(modifiedProductId)) {
						ContentNamespace contentNamespace = ContentNamespace.findFirstInstanceWithMatchingFieldFromList("label", jsonContent.getString("label"), new ArrayList<ContentNamespace>(expectedContentNamespaceSet));
						if (contentNamespace!=null) { 
							log.warning("ContentNamespace label '"+contentNamespace.label+"' modifies product id '"+modifiedProductId+"' which is NOT provided by the currently consumed subscriptions and should therefore not be among the entitled content namespaces no matter what its arch ("+contentNamespace.arches+") may be.");
							unexpectedContentNamespaceSet.add(contentNamespace);
							expectedContentNamespaceSet.remove(contentNamespace);
							break;	// to the next contentNamespace/jsonProductContent/jsonContent
						}
					}
					
				}
			}
		}
		
		// entitlement asserts
		List<String> actualEntitledContentLabels = new ArrayList<String>();
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) actualEntitledContentLabels.add(contentNamespace.label);
		for (ContentNamespace contentNamespace : expectedContentNamespaceSet) {
			Assert.assertTrue(actualEntitledContentLabels.contains(contentNamespace.label), "As expected, contentNamespace label '"+contentNamespace.label+"' defined for arches '"+contentNamespace.arches+"' requiredTags '"+contentNamespace.requiredTags+"' is included in the entitlement after subscribing to '"+pool.subscriptionName+"' on a '"+clienttasks.arch+"' system.");
		}
		for (ContentNamespace contentNamespace : unexpectedContentNamespaceSet) {
			Assert.assertTrue(!actualEntitledContentLabels.contains(contentNamespace.label), "As expected, contentNamespace label '"+contentNamespace.label+"' defined for arches '"+contentNamespace.arches+"' requiredTags '"+contentNamespace.requiredTags+"' is NOT included in the entitlement after subscribing to '"+pool.subscriptionName+"' on a '"+clienttasks.arch+"' system.");
		}
		
		// adjust the expectedContentNamespaces for requiredTags that are not provided by the installed productCerts' providedTags before checking the YumRepos
		List<ProductCert> installedProductCerts = clienttasks.getCurrentProductCerts();
		for (ContentNamespace contentNamespace : new HashSet<ContentNamespace>(expectedContentNamespaceSet)) {
			if (!clienttasks.areAllRequiredTagsProvidedByProductCerts(contentNamespace.requiredTags, installedProductCerts)) {
				log.warning("Entitled contentNamespace label '"+contentNamespace.label+"' defined for arches '"+contentNamespace.arches+"' has requiredTags '"+contentNamespace.requiredTags+"' that are NOT provided by the currently installed product certs.  This expected contentNamespace will be moved to the unexpected list when asserting the YumRepos next.");
				unexpectedContentNamespaceSet.add(contentNamespace);
				expectedContentNamespaceSet.remove(contentNamespace);
			}
		}
		
		// adjust the expectedContentNamespaces for type that does not equal "yum" before checking the YumRepos
		for (ContentNamespace contentNamespace : new HashSet<ContentNamespace>(expectedContentNamespaceSet)) {
			if (!contentNamespace.type.equals("yum")) {	// "file", "kickstart"
				log.warning("Entitled contentNamespace label '"+contentNamespace.label+"' defined for arches '"+contentNamespace.arches+"' has type '"+contentNamespace.type+"'.  This expected contentNamespace will be moved to the unexpected list when asserting the YumRepos next.");
				unexpectedContentNamespaceSet.add(contentNamespace);
				expectedContentNamespaceSet.remove(contentNamespace);
			}
		}
		
		// YumRepo asserts
		List<String> actualYumRepoLabels = new ArrayList<String>();
		for (YumRepo yumRepo : clienttasks.getCurrentlySubscribedYumRepos()) actualYumRepoLabels.add(yumRepo.id);
		for (ContentNamespace contentNamespace : expectedContentNamespaceSet) {
			Assert.assertTrue(actualYumRepoLabels.contains(contentNamespace.label), "As expected, yum repo label '"+contentNamespace.label+"' defined for arches '"+contentNamespace.arches+"' requiredTags '"+contentNamespace.requiredTags+"' is included in "+clienttasks.redhatRepoFile+" after subscribing to '"+pool.subscriptionName+"' on a '"+clienttasks.arch+"' system.");
		}
		for (ContentNamespace contentNamespace : unexpectedContentNamespaceSet) {
			Assert.assertTrue(!actualYumRepoLabels.contains(contentNamespace.label), "As expected, yum repo label '"+contentNamespace.label+"' defined for arches '"+contentNamespace.arches+"' requiredTags '"+contentNamespace.requiredTags+"' is NOT included in in "+clienttasks.redhatRepoFile+" after subscribing to '"+pool.subscriptionName+"' on a '"+clienttasks.arch+"' system.");
		}
	}
	protected Set<String> poolIds = new HashSet<String>();


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-20084", "RHEL7-55189"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier1")
	@Test(	description="Verify that all there is at least one available RHEL subscription and that yum content is available for the installed RHEL product cert",
			groups={"Tier1Tests","FipsTests","blockedByBug-1156638"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRhelSubscriptionContentIsAvailable() throws JSONException, Exception {
		
		// TODO Move this workaround to the end of this test
		// TEMPORARY WORKAROUND
		if (clienttasks.arch.equals("ppc64le")) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1156638"; // Bug 1156638 - "Red Hat Enterprise Linux for IBM POWER" subscriptions need to provide content for arch "ppc64le"
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("Skipping this test on arch '"+clienttasks.arch+"' while blocking bug '"+bugId+"' is open.");
			}
		}
		// END OF WORKAROUND
		
		// get the currently installed RHEL product cert
		ProductCert rhelProductCert = clienttasks.getCurrentRhelProductCert();
		Assert.assertNotNull(rhelProductCert, "Expecting a RHEL Product Cert to be installed.");
		log.info("RHEL product cert installed: "+rhelProductCert);
		
		// register and make sure autoheal is off
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null);
		
		// verify that NO yum content is available since no entitlements have been granted
		Integer yumRepolistPackageCount = clienttasks.getYumRepolistPackageCount("enabled");
		if (yumRepolistPackageCount>0) clienttasks.list_(null, null, true, null, null, null, null, null, null, null, null, null, null, null, null);	// added only for debugging a failure
		Assert.assertEquals(yumRepolistPackageCount,new Integer(0),"Expecting no available packages (actual='"+yumRepolistPackageCount+"') because no RHEL subscription have been explicitly attached.");
		
		// loop through the available pools looking for those that provide content for this rhelProductCert
		boolean rhelYumContentIsAvailable = true;
		boolean rhelSubscriptionIsAvailable = false;
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if (CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername,sm_clientPassword,sm_serverUrl,pool.poolId).contains(rhelProductCert.productId)) {
				
				// subscribe
				EntitlementCert rhelEntitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.subscribeToSubscriptionPool(pool, sm_clientUsername, sm_clientPassword, sm_serverUrl));
				
				// TEMPORARY WORKAROUND FOR BUG: 1174966 - No repositories enabled after subscribing aarch64 ARM Development Preview using subscription-manager
				//	Product:
				//		ID: 261
				//		Name: Red Hat Enterprise Linux Server for ARM Development Preview
				//		Version: Snapshot
				//		Arch: aarch64
				//		Tags: rhsa-dp-server,rhsa-dp-server-7
				//		Brand Type: 
				//		Brand Name: 
				if (rhelProductCert.productId.equals("261") && clienttasks.arch.equals("aarch64")) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="1174966"; 
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
					if (invokeWorkaroundWhileBugIsOpen) {
						String enablerepo = "rhel-server-for-arm-development-preview-rpms";
						log.info("Explicitly enabling repo '"+enablerepo+"' to gain access to ARM content.");
						clienttasks.repos(null, null, null, enablerepo, null, null, null, null, null);
					}
				}
				// END OF WORKAROUND
				
				// TEMPORARY WORKAROUND
				if (clienttasks.redhatReleaseX.equals("6") && rhelProductCert.productId.equals("155")) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="1573573"; // Bug 1573573 - EngId 155 repo rhel-6-workstation-htb-rpms should be enabled by default 
					bugId="1571077"; // Bug 1571077 - [HTB] Repo "rhel-6-workstation-htb-rpms" should be enabled by default for workstation x86_64.
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
					if (invokeWorkaroundWhileBugIsOpen) {
						WORKAROUND_BUG_1571077 = true;
						String enablerepo = "rhel-6-workstation-htb-rpms";
						log.info("Explicitly enabling repo '"+enablerepo+"' to gain access to HTB Workstation content on RHEL6.");
						clienttasks.repos(null, null, null, enablerepo, null, null, null, null, null);
					}
				}
				// END OF WORKAROUND
				
				// WORKAROUND FOR RHEL-ALT-7.5 aarch64
				if (clienttasks.redhatReleaseXY.equals("7.5") && rhelProductCert.productId.equals("433")) { // Red Hat Enterprise Linux for IBM System z (Structure A) Beta
					String repo="rhel-7-for-system-z-a-beta-rpms";
					log.info("WORKAROUND: Enabling beta repo '"+repo+"' for installed product '"+rhelProductCert.productName+"' ("+rhelProductCert.productId+") because this is the debut release for this product.  No GA content from repo rhel-7-for-system-z-a-rpms is available yet.");
					clienttasks.repos(null, null, null, repo, null, null, null, null, null);
				}
				// END OF WORKAROUND
				
				// verify that rhel yum content is available
				yumRepolistPackageCount = clienttasks.getYumRepolistPackageCount("enabled");
				if (yumRepolistPackageCount>0) {
					Assert.assertTrue(yumRepolistPackageCount>0,"Expecting many available packages (actual='"+yumRepolistPackageCount+"') of enabled repo content because RHEL subscription '"+pool.subscriptionName+"' SKU '"+pool.productId+"' was just attached.");
				} else {
					log.warning("No enabled yum repo content packages are available after attaching RHEL subscription '"+pool.subscriptionName+"'. (This can happen when the RHEL product is brand new and content has not yet been pushed to '"+clienttasks.baseurl+"'.  It can also happen when testing Snapshot-1 prior to public release of HTB content.)");
					rhelYumContentIsAvailable = false;
				}
				
				// unsubscribe
				clienttasks.unsubscribe(null, rhelEntitlementCert.serialNumber, null, null, null, null, null);
				
				rhelSubscriptionIsAvailable = true;
				if (rhelYumContentIsAvailable) break;
			}
		}
		if (!rhelSubscriptionIsAvailable && sm_serverType.equals(CandlepinType.standalone)) throw new SkipException("Skipping this test against a standalone Candlepin server that has no RHEL subscriptions available.");
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1090058
		if (clienttasks.redhatReleaseX.equals("5") && clienttasks.arch.startsWith("ppc")) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1090058"; 
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				if (!rhelSubscriptionIsAvailable) throw new SkipException("skipping this test while bug '"+bugId+"' is open");
			}
		}
		// END OF WORKAROUND
		
		if (!rhelSubscriptionIsAvailable) {
			clienttasks.facts_(true, null, null, null, null, null);
			log.warning("This test is about to fail and may be due to the lack of an available subscription with enough socket/ram/core support to cover this system.  Visually confirm by reviewing the system facts above.");
		}
		
		Assert.assertTrue(rhelSubscriptionIsAvailable,"Successfully subscribed to at least one available RHEL subscription that provided for our installed RHEL product cert: "+rhelProductCert);
		Assert.assertTrue(rhelYumContentIsAvailable,"All of the RHEL subscriptions subscribed provided at least one enabled yum content package applicable for our installed RHEL product cert: "+rhelProductCert+" (See WARNINGS logged above for failed subscriptions)");
	}
	protected boolean WORKAROUND_BUG_1571077 = false;


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-20087", "RHEL7-55191"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier1")
	@Test(	description="Verify that yum install does not fail when service rsyslog is stopped",
			groups={"Tier1Tests","FipsTests","blockedByBug-1211557","testYumInstallSucceedsWhenServiceRsyslogIsStopped"},
			dependsOnMethods={"testRhelSubscriptionContentIsAvailable"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumInstallSucceedsWhenServiceRsyslogIsStopped() throws JSONException, Exception {
		// assume a RHEL subscription is available from dependent VerifyRhelSubscriptionContentIsAvailable_Test
		
		// register and attach a RHEL subscription via autosubscribe
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		
		String pkg = "rcs";	// not available on rhel72 Client
		pkg = "zsh";
		if (clienttasks.isPackageInstalled(pkg)) clienttasks.yumRemovePackage(pkg);
		
		// stop the rsyslog service
		if (Integer.valueOf(clienttasks.redhatReleaseX) >= 7) {
			RemoteFileTasks.runCommandAndAssert(client,"systemctl stop rsyslog.service",Integer.valueOf(0),"","");	
		} else {
			RemoteFileTasks.runCommandAndAssert(client,"service rsyslog stop",Integer.valueOf(0),"^Shutting down system logger: *\\[  OK  \\]$",null);	
		}
		
		// TEMPORARY WORKAROUND
		if (WORKAROUND_BUG_1571077) {
			String enablerepo = "rhel-6-workstation-htb-rpms";
			log.info("Explicitly enabling repo '"+enablerepo+"' to gain access to HTB Workstation content on RHEL6.");
			clienttasks.repos(null, null, null, enablerepo, null, null, null, null, null);
		}
		// END OF WORKAROUND
		
		// yum install the package
		//  Failure From Bug 1211557 - subscription-manager causes failure of yum 
		//	[root@jsefler-os6 ~]# yum -y install rcs
		//	Loaded plugins: product-id, refresh-packagekit, rhnplugin, security, subscription-manager
		//	Traceback (most recent call last):
		//	  File "/usr/bin/yum", line 29, in <module>
		//	    yummain.user_main(sys.argv[1:], exit_code=True)
		//	  File "/usr/share/yum-cli/yummain.py", line 300, in user_main
		//	    errcode = main(args)
		//	  File "/usr/share/yum-cli/yummain.py", line 115, in main
		//	    base.getOptionsConfig(args)
		//	  File "/usr/share/yum-cli/cli.py", line 229, in getOptionsConfig
		//	    self.conf
		//	  File "/usr/lib/python2.6/site-packages/yum/__init__.py", line 911, in <lambda>
		//	    conf = property(fget=lambda self: self._getConfig(),
		//	  File "/usr/lib/python2.6/site-packages/yum/__init__.py", line 348, in _getConfig
		//	    self.plugins.run('postconfig')
		//	  File "/usr/lib/python2.6/site-packages/yum/plugins.py", line 184, in run
		//	    func(conduitcls(self, self.base, conf, **kwargs))
		//	  File "/usr/lib/yum-plugins/subscription-manager.py", line 129, in postconfig_hook
		//	    logutil.init_logger_for_yum()
		//	  File "/usr/share/rhsm/subscription_manager/logutil.py", line 136, in init_logger_for_yum
		//	    init_logger()
		//	  File "/usr/share/rhsm/subscription_manager/logutil.py", line 132, in init_logger
		//	    file_config(logging_config=LOGGING_CONFIG)
		//	  File "/usr/share/rhsm/subscription_manager/logutil.py", line 118, in file_config
		//	    disable_existing_loggers=False)
		//	  File "/usr/lib64/python2.6/logging/config.py", line 84, in fileConfig
		//	    handlers = _install_handlers(cp, formatters)
		//	  File "/usr/lib64/python2.6/logging/config.py", line 162, in _install_handlers
		//	    h = klass(*args)
		//	  File "/usr/lib64/python2.6/logging/handlers.py", line 721, in __init__
		//	    self._connect_unixsocket(address)
		//	  File "/usr/lib64/python2.6/logging/handlers.py", line 737, in _connect_unixsocket
		//	    self.socket.connect(address)
		//	  File "<string>", line 1, in connect
		//	socket.error: [Errno 2] No such file or directory
		//	[root@jsefler-os6 ~]# echo $?
		//	1
		
		// Note: On ComputeNode, package rcs is under rhel-6-hpc-node-optional-rpms
		// Note: On Client, package rcs is under rhel-6-client-optional-rpms
		//clienttasks.yumInstallPackage(pkg);
		clienttasks.yumInstallPackage(pkg,"--enablerepo=*-optional-rpms");
		clienttasks.yumRemovePackage(pkg,"--enablerepo=*-optional-rpms");
	}
	@AfterGroups(groups={"setup"}, value={"testYumInstallSucceedsWhenServiceRsyslogIsStopped"})
	@AfterClass(groups={"setup"})	// insurance; not really needed
	public void restartRsyslogAfterGroup() {
		if (client!=null) RemoteFileTasks.runCommandAndAssert(client, "service rsyslog start", 0);
	}
	
	
	
	
	
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47914", "RHEL7-97069"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28570",	// RHSM-REQ : Modifier Subscriptions (EUS)
					project= Project.RHEL6,
					role= DefTypes.Role.RELATES_TO),
				@LinkedItem(
					workitemId= "RHEL7-84951",	// RHSM-REQ : Modifier Subscriptions (EUS)
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.RELATES_TO)},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Verify Extended Update Support content set repos (identified as containing '-eus-') have a non-empty list of modifiedProductIds",
			groups={"Tier1Tests"},
			dataProvider="getAllEUSProductContentSetData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testEUSProductContentSetsAssertingNonEmptyModifiesProducts(Object bugzilla, String eusProductName, String eusProductId, String eusContentSetName, String eusContentSetId, String eusContentSetLabel, List<String> modifiedProductIds) throws JSONException, Exception {
		
		log.info("The following curl request can be used to fetch the Candlepin representation for EUS Content Set '"+eusContentSetName+"': "+eusContentSetLabel);
		JSONObject jsonContent = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/owners/"+getAllEUSProductContentSetDataAsListOfListsOwnerKey+"/content/"+eusContentSetId));
		//	[root@jsefler-rhel7 ~]# curl --stderr /dev/null --insecure --user stage_ha_testuser:redhat --request GET 'https://subscription.rhsm.stage.redhat.com:443/subscription/owners/10992327/content/4180' | python -m json/tool
		//	{
		//	    "arches": "x86_64",
		//	    "contentUrl": "/content/eus/rhel/server/7/$releasever/$basearch/highavailability/os",
		//	    "created": "2017-05-31T17:29:04+0000",
		//	    "gpgUrl": "file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release",
		//	    "id": "4180",
		//	    "label": "rhel-ha-for-rhel-7-server-eus-rpms",
		//	    "metadataExpire": 86400,
		//	    "modifiedProductIds": [],
		//	    "name": "Red Hat Enterprise Linux High Availability (for RHEL 7 Server) - Extended Update Support (RPMs)",
		//	    "releaseVer": null,
		//	    "requiredTags": "rhel-7-server",
		//	    "type": "yum",
		//	    "updated": "2017-05-31T17:29:04+0000",
		//	    "uuid": "8a99f9825c5f84ae015c5f8dc05f0a77",
		//	    "vendor": "Red Hat"
		//	}
		log.info("EUS Engineering Product:           "+eusProductName);
		log.info("EUS Engineering ProductId:         "+eusProductId);
		log.info("EUS ContentSet Repo Name:          "+eusContentSetName);
		log.info("EUS ContentSet Repo Label:         "+eusContentSetLabel);
		log.info("EUS ContentSet id:                 "+eusContentSetId);
		log.info("EUS ContentSet modifiedProductIds: "+modifiedProductIds);
		
		Assert.assertEquals(jsonContent.get("name"), eusContentSetName);	// if this does not pass, then the Candlepin API for /owners/{owner_key}/content/{content_id} is returning different values than returned from /owners/{owner_key}/products
		Assert.assertEquals(jsonContent.get("label"), eusContentSetLabel);	// if this does not pass, then the Candlepin API for /owners/{owner_key}/content/{content_id} is returning different values than returned from /owners/{owner_key}/products
		
		// skip Red Hat Software Collections
		if (modifiedProductIds.isEmpty() && eusContentSetLabel.contains("-rhscl-") && eusProductName.startsWith("Red Hat Software Collections")) {
			throw new SkipException("Skipping '"+eusProductName+"' content set '"+eusContentSetLabel+"' because both the eus and non-eus rhscl repos are both provided by the same engineering product.  Hence a subscription to a Red Hat Software Collection includes extended update support.");
		}
		
		// does this pool contain productContents that modify other products?
		Assert.assertTrue(!modifiedProductIds.isEmpty(), "EUS Product '"+eusProductName+"' (id="+eusProductId+") content set repository '"+eusContentSetLabel+"' (id="+eusContentSetId+") modifies a NON-EMPTY list of engineering productIds (actualModifiedProductIds="+modifiedProductIds+")");
	}
	@DataProvider(name="getAllEUSProductContentSetData")
	public Object[][] getAllEUSProductContentSetDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAllEUSProductContentSetDataAsListOfLists());
	}
	protected List<List<Object>> getAllEUSProductContentSetDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// register and get the owner_key
		clienttasks.unregister(null, null, null, null);
		// NOTE: The most thorough way to test this is using an account with access to all products via the Employee SKU ES0113909
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		getAllEUSProductContentSetDataAsListOfListsOwnerKey = clienttasks.getCurrentlyRegisteredOwnerKey();
		Map<String,String> eusLabelToInfoMap = new HashMap<String,String>();
		
		//	[root@jsefler-rhel7 ~]# curl --stderr /dev/null --insecure --user REDACTED:REDACTED --request GET 'https://subscription.rhsm.stage.redhat.com:443/subscription/owners/10992327/products/84' | python -m json/tool
		//	{
		//	    "attributes": [
		//	        {
		//	            "name": "arch",
		//	            "value": "ia64,ppc,ppc64,ppc64le,x86,x86_64"
		//	        },
		//	        {
		//	            "name": "name",
		//	            "value": "Red Hat Enterprise Linux High Availability (for RHEL Server) - Extended Update Support"
		//	        },
		//	        {
		//	            "name": "type",
		//	            "value": "SVC"
		//	        }
		//	    ],
		//	    "created": "2017-05-31T17:30:20+0000",
		//	    "dependentProductIds": [],
		//	    "href": "/products/8a99f9835c5f85d2015c5f8eebc504e3",
		//	    "id": "84",
		//	    "multiplier": 1,
		//	    "name": "Red Hat Enterprise Linux High Availability (for RHEL Server) - Extended Update Support",
		//	    "productContent": [
		//	        {
		//	            "content": {
		//	                "arches": "x86,x86_64",
		//	                "contentUrl": "/content/eus/rhel/server/6/$releasever/$basearch/highavailability/os",
		//	                "created": "2017-05-31T17:30:17+0000",
		//	                "gpgUrl": "file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release",
		//	                "id": "1352",
		//	                "label": "rhel-ha-for-rhel-6-server-eus-rpms",
		//	                "metadataExpire": 86400,
		//	                "modifiedProductIds": [],
		//	                "name": "Red Hat Enterprise Linux High Availability (for RHEL 6 Server) - Extended Update Support (RPMs)",
		//	                "releaseVer": null,
		//	                "requiredTags": "rhel-6-server",
		//	                "type": "yum",
		//	                "updated": "2017-05-31T17:30:17+0000",
		//	                "uuid": "8a99f9835c5f85d2015c5f8eddb20236",
		//	                "vendor": "Red Hat"
		//	            },
		//	            "enabled": false
		//	        },
		// <SNIP FOR BREVITY>
		//	        {
		//	            "content": {
		//	                "arches": "x86,x86_64",
		//	                "contentUrl": "/content/eus/rhel/server/6/$releasever/$basearch/highavailability/debug",
		//	                "created": "2017-05-31T17:30:17+0000",
		//	                "gpgUrl": "file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release",
		//	                "id": "1351",
		//	                "label": "rhel-ha-for-rhel-6-server-eus-debug-rpms",
		//	                "metadataExpire": 86400,
		//	                "modifiedProductIds": [
		//	                    "83"
		//	                ],
		//	                "name": "Red Hat Enterprise Linux High Availability (for RHEL 6 Server) - Extended Update Support (Debug RPMs)",
		//	                "releaseVer": null,
		//	                "requiredTags": "rhel-6-server",
		//	                "type": "yum",
		//	                "updated": "2017-05-31T17:30:17+0000",
		//	                "uuid": "8a99f9835c5f85d2015c5f8eddb20237",
		//	                "vendor": "Red Hat"
		//	            },
		//	            "enabled": false
		//	        }
		//	    ],
		//	    "updated": "2017-05-31T17:30:20+0000",
		//	    "uuid": "8a99f9835c5f85d2015c5f8eebc504e3"
		//	}
		
		// loop through all of the owner's products
		JSONArray jsonProducts = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/owners/"+getAllEUSProductContentSetDataAsListOfListsOwnerKey+"/products"));
		for (int i = 0; i < jsonProducts.length(); i++) {
			JSONObject jsonProduct = (JSONObject) jsonProducts.get(i);
			
			String productId = jsonProduct.getString("id");
			String productName = CandlepinTasks.getResourceAttributeValue(jsonProduct, "name");
			
			// loop through all of the product content sets
			JSONArray jsonProductContents = jsonProduct.getJSONArray("productContent");
			for (int j = 0; j < jsonProductContents.length(); j++) {
				JSONObject jsonProductContent = (JSONObject) jsonProductContents.get(j);
				JSONObject jsonContent = jsonProductContent.getJSONObject("content");
				
				// get the label and modifiedProductIds for each of the productContents
				String label = jsonContent.getString("label");	// "rhel-ha-for-rhel-6-server-eus-rpms",
				String name = jsonContent.getString("name");	// "Red Hat Enterprise Linux High Availability (for RHEL 6 Server) - Extended Update Support (RPMs)",
				String id = jsonContent.getString("id");		// "1351"
				String requiredTags = jsonContent.isNull("requiredTags")? null:jsonContent.getString("requiredTags"); // comma separated string
				String type = jsonContent.getString("type");
				JSONArray jsonModifiedProductIds = jsonContent.getJSONArray("modifiedProductIds");
				List<String> modifiedProductIds = new ArrayList<String>();
				for (int k = 0; k < jsonModifiedProductIds.length(); k++) {
					String modifiedProductId = (String) jsonModifiedProductIds.get(k);
					modifiedProductIds.add(modifiedProductId);
				}
				
				// is this an EUS content set?
				if (label.contains("-eus-")) {	// only add rows for eus repo labels
					if (label.contains("rhel-4")) {
						log.info("Skipping this test for rhel-4 eus content set repository '"+label+"' because subscription-manager was never delivered on rhel-4.");
						continue;	// skip RHEL4 REPOS
					}
					eusLabelToInfoMap.put(label, productId+";"+productName+";"+id+";"+modifiedProductIds);
					Set<String> bugIds = new HashSet<String>();
					
					if (label.contains("-rhui-")) {
						log.info("Skipping this test for rhui eus content set repository '"+label+"' because of a NEEDINFO on RHUI eng product 157 - How is RHUI special? There is no product cert 157 on rcm-metadata.git.");
						continue;	// skip RHUI REPOS
					}
					
					// Bug 1471998 - content set mappings for "Red Hat S-JIS Support (for RHEL Server) - Extended Update Support" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-sjis-for-rhel-6-server-eus-debug-rpms")) bugIds.add("1471998");
					if (label.equals("rhel-sjis-for-rhel-6-server-eus-rpms")) bugIds.add("1471998");
					if (label.equals("rhel-sjis-for-rhel-6-server-eus-source-rpms")) bugIds.add("1471998");
					if (label.equals("rhel-sjis-for-rhel-7-server-eus-debug-rpms")) bugIds.add("1471998");
					if (label.equals("rhel-sjis-for-rhel-7-server-eus-rpms")) bugIds.add("1471998");
					if (label.equals("rhel-sjis-for-rhel-7-server-eus-source-rpms")) bugIds.add("1471998");
					
					// Bug 1472001 - content set mappings for "Red Hat Enterprise Linux Resilient Storage (for RHEL Server) - Extended Update Support" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-rs-for-rhel-5-for-power-eus-debug-rpms")) bugIds.add("1472001");
					if (label.equals("rhel-rs-for-rhel-5-for-power-eus-rpms")) bugIds.add("1472001");
					if (label.equals("rhel-rs-for-rhel-5-for-power-eus-source-rpms")) bugIds.add("1472001");
					if (label.equals("rhel-rs-for-rhel-5-server-eus-rpms")) bugIds.add("1472001");
					if (label.equals("rhel-rs-for-rhel-6-server-eus-rpms")) bugIds.add("1472001");
					if (label.equals("rhel-rs-for-rhel-7-server-eus-debug-rpms")) bugIds.add("1472001");
					if (label.equals("rhel-rs-for-rhel-7-server-eus-rpms")) bugIds.add("1472001");
					if (label.equals("rhel-rs-for-rhel-7-server-eus-source-rpms")) bugIds.add("1472001");
					
					// Bug 1472004 - content set mappings for "Red Hat Enterprise Linux High Availability (for RHEL Server) - Extended Update Support" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-ha-for-rhel-5-for-power-eus-debug-rpms")) bugIds.add("1472004");
					if (label.equals("rhel-ha-for-rhel-5-for-power-eus-rpms")) bugIds.add("1472004");
					if (label.equals("rhel-ha-for-rhel-5-for-power-eus-source-rpms")) bugIds.add("1472004");
					if (label.equals("rhel-ha-for-rhel-5-server-eus-rpms")) bugIds.add("1472004");
					if (label.equals("rhel-ha-for-rhel-6-server-eus-rpms")) bugIds.add("1472004");
					if (label.equals("rhel-ha-for-rhel-7-server-eus-debug-rpms")) bugIds.add("1472004");
					if (label.equals("rhel-ha-for-rhel-7-server-eus-rpms")) bugIds.add("1472004");
					if (label.equals("rhel-ha-for-rhel-7-server-eus-source-rpms")) bugIds.add("1472004");
					
					// Bug 1472005 - content set mappings for "Oracle Java (for RHEL Server) - Extended Update Support" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-5-server-eus-thirdparty-oracle-java-isos")) bugIds.add("1472005");
					if (label.equals("rhel-5-server-eus-thirdparty-oracle-java-rpms")) bugIds.add("1472005");
					if (label.equals("rhel-5-server-eus-thirdparty-oracle-java-source-rpms")) bugIds.add("1472005");
					if (label.equals("rhel-6-server-eus-thirdparty-oracle-java-isos")) bugIds.add("1472005");
					if (label.equals("rhel-6-server-eus-thirdparty-oracle-java-rpms")) bugIds.add("1472005");
					if (label.equals("rhel-6-server-eus-thirdparty-oracle-java-source-rpms")) bugIds.add("1472005");
					if (label.equals("rhel-7-server-eus-thirdparty-oracle-java-isos")) bugIds.add("1472005");
					if (label.equals("rhel-7-server-eus-thirdparty-oracle-java-rpms")) bugIds.add("1472005");
					if (label.equals("rhel-7-server-eus-thirdparty-oracle-java-source-rpms")) bugIds.add("1472005");
					
					// Bug 1472007 - content set mappings for "Red Hat Enterprise Linux Server - Extended Update Support" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-5-server-eus-rh-common-debuginfo")) bugIds.add("1472007");
					if (label.equals("rhel-5-server-eus-rh-common-isos")) bugIds.add("1472007");
					if (label.equals("rhel-5-server-eus-rh-common-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-5-server-eus-rh-common-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-5-server-eus-rhn-tools-debug-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-5-server-eus-rhn-tools-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-5-server-eus-rhn-tools-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-5-server-eus-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-5-server-eus-satellite-tools-6.1-debuginfo")) bugIds.add("1472007");
					if (label.equals("rhel-5-server-eus-satellite-tools-6.1-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-5-server-eus-satellite-tools-6.1-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-rhn-tools-debug-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-rhn-tools-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-rhn-tools-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-satellite-tools-6.1-debug-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-satellite-tools-6.1-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-satellite-tools-6.1-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-satellite-tools-6.2-debug-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-satellite-tools-6.2-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-satellite-tools-6.2-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-satellite-tools-6.3-debug-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-satellite-tools-6.3-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-satellite-tools-6.3-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-satellite-tools-6.3-puppet4-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-satellite-tools-6.3-puppet4-debug-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-6-server-eus-satellite-tools-6.3-puppet4-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-debug-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-isos")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-optional-debug-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-optional-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-optional-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-rh-common-debug-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-rh-common-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-rh-common-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-rhn-tools-debug-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-rhn-tools-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-rhn-tools-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-satellite-tools-6.1-debug-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-satellite-tools-6.1-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-satellite-tools-6.1-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-satellite-tools-6.2-debug-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-satellite-tools-6.2-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-satellite-tools-6.2-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-satellite-tools-6.3-debug-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-satellite-tools-6.3-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-satellite-tools-6.3-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-satellite-tools-6.3-puppet4-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-satellite-tools-6.3-puppet4-debug-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-satellite-tools-6.3-puppet4-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-source-isos")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-source-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-supplementary-debuginfo")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-supplementary-isos")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-supplementary-rpms")) bugIds.add("1472007");
					if (label.equals("rhel-7-server-eus-supplementary-source-rpms")) bugIds.add("1472007");
					
					//	Bug 1491304 - content set mappings for "Oracle Java (for RHEL Compute Node) - Extended Update Support" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-7-hpc-node-eus-thirdparty-oracle-java-rpms")) bugIds.add("1491304");
					if (label.equals("rhel-7-hpc-node-eus-thirdparty-oracle-java-source-rpms")) bugIds.add("1491304");
					if (label.equals("rhel-hpc-node-6-eus-thirdparty-oracle-java-rpms")) bugIds.add("1491304");
					if (label.equals("rhel-hpc-node-6-eus-thirdparty-oracle-java-source-rpms")) bugIds.add("1491304");
					
					//	Bug 1491308 - content set mappings for "Red Hat Developer Toolset (for RHEL Server EUS)" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-server-dts-5-eus-rpms")) bugIds.add("1491308");
					if (label.equals("rhel-server-dts-6-eus-rpms")) bugIds.add("1491308");
					if (label.equals("rhel-server-dts2-5-eus-debug-rpms")) bugIds.add("1491308");
					if (label.equals("rhel-server-dts2-5-eus-rpms")) bugIds.add("1491308");
					if (label.equals("rhel-server-dts2-5-eus-source-rpms")) bugIds.add("1491308");
					if (label.equals("rhel-server-dts2-6-eus-debug-rpms")) bugIds.add("1491308");
					if (label.equals("rhel-server-dts2-6-eus-rpms")) bugIds.add("1491308");
					if (label.equals("rhel-server-dts2-6-eus-source-rpms")) bugIds.add("1491308");
					// Bug 1491308 was CLOSED WONTFIX
					if ((label.startsWith("rhel-server-dts-")||label.startsWith("rhel-server-dts2-")) && (label.contains("-eus-"))) {
						log.info("Skipping this test for content set repository '"+label+"' because bug 1491308 was CLOSED WONTFIX for already not supported(EOL) products DTS and DTS2");
						continue;	// skip dts and dts2 eus REPOS
					}
					
					//	Bug 1491319 - content set mappings for "Red Hat Enterprise Linux EUS Compute Node" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-6-for-hpc-node-eus-satellite-tools-6.1-debug-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-6-for-hpc-node-eus-satellite-tools-6.1-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-6-for-hpc-node-eus-satellite-tools-6.1-source-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-6-for-hpc-node-eus-satellite-tools-6.2-debug-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-6-for-hpc-node-eus-satellite-tools-6.2-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-6-for-hpc-node-eus-satellite-tools-6.2-source-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-for-hpc-node-eus-rh-common-debug-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-for-hpc-node-eus-rh-common-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-for-hpc-node-eus-rh-common-source-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-for-hpc-node-eus-satellite-tools-6.1-debug-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-for-hpc-node-eus-satellite-tools-6.1-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-for-hpc-node-eus-satellite-tools-6.1-source-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-for-hpc-node-eus-satellite-tools-6.2-debug-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-for-hpc-node-eus-satellite-tools-6.2-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-for-hpc-node-eus-satellite-tools-6.2-source-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-hpc-node-eus-debug-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-hpc-node-eus-optional-debug-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-hpc-node-eus-optional-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-hpc-node-eus-optional-source-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-hpc-node-eus-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-hpc-node-eus-source-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-hpc-node-eus-supplementary-debug-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-hpc-node-eus-supplementary-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-7-hpc-node-eus-supplementary-source-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-6-eus-debug-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-6-eus-optional-debug-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-6-eus-optional-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-6-eus-optional-source-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-6-eus-rhn-tools-debug-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-6-eus-rhn-tools-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-6-eus-rhn-tools-source-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-6-eus-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-6-eus-source-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-6-eus-supplementary-debug-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-6-eus-supplementary-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-6-eus-supplementary-source-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-7-eus-rhn-tools-debug-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-7-eus-rhn-tools-rpms")) bugIds.add("1491319");
					if (label.equals("rhel-hpc-node-7-eus-rhn-tools-source-rpms")) bugIds.add("1491319");
					
					//	Bug 1491325 - content set mappings for "Red Hat Enterprise Linux EUS Compute Node High Performance Networking" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-hpc-node-6-eus-hpn-debug-rpms")) bugIds.add("1491325");
					if (label.equals("rhel-hpc-node-6-eus-hpn-rpms")) bugIds.add("1491325");
					if (label.equals("rhel-hpc-node-6-eus-hpn-source-rpms")) bugIds.add("1491325");
					
					//	Bug 1491334 - content set mappings for "Red Hat Enterprise Linux EUS Compute Node Scalable File System" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-hpc-node-6-eus-sfs-debug-rpms")) bugIds.add("1491334");
					if (label.equals("rhel-hpc-node-6-eus-sfs-rpms")) bugIds.add("1491334");
					if (label.equals("rhel-hpc-node-6-eus-sfs-source-rpms")) bugIds.add("1491334");
					
					//	Bug 1491348 - content set mappings for "Red Hat Enterprise Linux for IBM z Systems - Extended Update Support" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-5-for-system-z-eus-rh-common-debuginfo")) bugIds.add("1491348");
					if (label.equals("rhel-5-for-system-z-eus-rh-common-isos")) bugIds.add("1491348");
					if (label.equals("rhel-5-for-system-z-eus-rh-common-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-5-for-system-z-eus-rh-common-source-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-5-for-system-z-eus-rhn-tools-debug-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-5-for-system-z-eus-rhn-tools-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-5-for-system-z-eus-rhn-tools-source-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-5-for-system-z-eus-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-5-for-system-z-eus-satellite-tools-6.1-debuginfo")) bugIds.add("1491348");
					if (label.equals("rhel-5-for-system-z-eus-satellite-tools-6.1-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-5-for-system-z-eus-satellite-tools-6.1-source-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-6-for-system-z-eus-rhn-tools-debug-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-6-for-system-z-eus-rhn-tools-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-6-for-system-z-eus-rhn-tools-source-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-6-for-system-z-eus-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-6-for-system-z-eus-satellite-tools-6.1-debug-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-6-for-system-z-eus-satellite-tools-6.1-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-6-for-system-z-eus-satellite-tools-6.1-source-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-6-for-system-z-eus-satellite-tools-6.2-debug-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-6-for-system-z-eus-satellite-tools-6.2-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-6-for-system-z-eus-satellite-tools-6.2-source-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-debug-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-isos")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-optional-debug-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-optional-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-optional-source-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-rh-common-debug-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-rh-common-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-rh-common-source-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-rhn-tools-debug-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-rhn-tools-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-rhn-tools-source-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-satellite-tools-6.1-debug-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-satellite-tools-6.1-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-satellite-tools-6.1-source-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-satellite-tools-6.2-debug-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-satellite-tools-6.2-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-satellite-tools-6.2-source-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-source-isos")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-source-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-supplementary-debuginfo")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-supplementary-isos")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-supplementary-rpms")) bugIds.add("1491348");
					if (label.equals("rhel-7-for-system-z-eus-supplementary-source-rpms")) bugIds.add("1491348");
					
					//	Bug 1491351 - content set mappings for "Red Hat Enterprise Linux for Power, big endian - Extended Update Support" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-5-for-power-eus-debug-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-isos")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-rh-common-debuginfo")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-rh-common-isos")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-rh-common-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-rh-common-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-rhn-tools-debug-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-rhn-tools-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-rhn-tools-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-satellite-tools-6.1-debuginfo")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-satellite-tools-6.1-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-satellite-tools-6.1-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-source-isos")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-supplementary-debuginfo")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-supplementary-isos")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-supplementary-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-5-for-power-eus-supplementary-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-6-for-power-eus-rhn-tools-debug-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-6-for-power-eus-rhn-tools-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-6-for-power-eus-rhn-tools-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-6-for-power-eus-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-6-for-power-eus-satellite-tools-6.1-debug-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-6-for-power-eus-satellite-tools-6.1-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-6-for-power-eus-satellite-tools-6.1-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-6-for-power-eus-satellite-tools-6.2-debug-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-6-for-power-eus-satellite-tools-6.2-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-6-for-power-eus-satellite-tools-6.2-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-debug-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-isos")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-optional-debug-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-optional-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-optional-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-rh-common-debug-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-rh-common-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-rh-common-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-rhn-tools-debug-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-rhn-tools-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-rhn-tools-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-satellite-tools-6.1-debug-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-satellite-tools-6.1-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-satellite-tools-6.1-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-satellite-tools-6.2-debug-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-satellite-tools-6.2-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-satellite-tools-6.2-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-satellite-tools-6.3-debug-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-satellite-tools-6.3-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-satellite-tools-6.3-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-source-isos")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-source-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-supplementary-debuginfo")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-supplementary-isos")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-supplementary-rpms")) bugIds.add("1491351");
					if (label.equals("rhel-7-for-power-eus-supplementary-source-rpms")) bugIds.add("1491351");
					
					//	Bug 1491356 - content set mappings for "Red Hat Enterprise Linux for Power, little endian - Extended Update Support" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-7-for-power-le-eus-debug-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-isos")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-optional-debug-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-optional-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-optional-source-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-rhn-tools-debug-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-rhn-tools-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-rhn-tools-source-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-satellite-tools-6.1-debug-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-satellite-tools-6.1-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-satellite-tools-6.1-source-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-satellite-tools-6.2-debug-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-satellite-tools-6.2-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-satellite-tools-6.2-source-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-satellite-tools-6.3-debug-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-satellite-tools-6.3-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-satellite-tools-6.3-source-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-satellite-tools-6.3-puppet4-debug-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-satellite-tools-6.3-puppet4-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-satellite-tools-6.3-puppet4-source-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-source-isos")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-source-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-supplementary-debug-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-supplementary-rpms")) bugIds.add("1491356");
					if (label.equals("rhel-7-for-power-le-eus-supplementary-source-rpms")) bugIds.add("1491356");
					
					//	Bug 1491359 - content set mappings for "Red Hat Enterprise Linux Load Balancer (for RHEL Server) - Extended Update Support" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-lb-for-rhel-6-server-eus-rpms")) bugIds.add("1491359");
					
					//	Bug 1491365 - content set mappings for "Red Hat Enterprise Linux Scalable File System (for RHEL Server) - Extended Update Support" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-scalefs-for-rhel-5-server-eus-rpms")) bugIds.add("1491365");
					if (label.equals("rhel-sfs-for-rhel-6-server-eus-rpms")) bugIds.add("1491365");
					
					//	Bug 1491369 - content set mappings for "RHEL for SAP - Extended Update Support" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-sap-for-rhel-6-server-eus-debug-rpms")) bugIds.add("1491369");
					if (label.equals("rhel-sap-for-rhel-6-server-eus-rpms")) bugIds.add("1491369");
					if (label.equals("rhel-sap-for-rhel-6-server-eus-source-rpms")) bugIds.add("1491369");
					if (label.equals("rhel-sap-for-rhel-7-server-eus-debug-rpms")) bugIds.add("1491369");
					if (label.equals("rhel-sap-for-rhel-7-server-eus-rpms")) bugIds.add("1491369");
					if (label.equals("rhel-sap-for-rhel-7-server-eus-source-rpms")) bugIds.add("1491369");
					
					//	Bug 1491373 - content set mappings for "RHEL for SAP Applications for Power BE EUS" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-sap-for-rhel-7-for-power-eus-debug-rpms")) bugIds.add("1491373");
					if (label.equals("rhel-sap-for-rhel-7-for-power-eus-rpms")) bugIds.add("1491373");
					if (label.equals("rhel-sap-for-rhel-7-for-power-eus-source-rpms")) bugIds.add("1491373");
					
					//	Bug 1491376 - content set mappings for "RHEL for SAP Applications for Power LE EUS" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-sap-for-rhel-7-for-power-le-eus-debug-rpms")) bugIds.add("1491376");
					if (label.equals("rhel-sap-for-rhel-7-for-power-le-eus-rpms")) bugIds.add("1491376");
					if (label.equals("rhel-sap-for-rhel-7-for-power-le-eus-source-rpms")) bugIds.add("1491376");
					
					//	Bug 1491380 - content set mappings for "RHEL for SAP Applications for System Z EUS" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-sap-for-rhel-7-for-system-z-eus-debug-rpms")) bugIds.add("1491380");
					if (label.equals("rhel-sap-for-rhel-7-for-system-z-eus-rpms")) bugIds.add("1491380");
					if (label.equals("rhel-sap-for-rhel-7-for-system-z-eus-source-rpms")) bugIds.add("1491380");
					
					//	Bug 1491382 - content set mappings for "RHEL for SAP HANA - Extended Update Support" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-sap-hana-for-rhel-6-server-eus-debug-rpms")) bugIds.add("1491382");
					if (label.equals("rhel-sap-hana-for-rhel-6-server-eus-rpms")) bugIds.add("1491382");
					if (label.equals("rhel-sap-hana-for-rhel-6-server-eus-source-rpms")) bugIds.add("1491382");
					if (label.equals("rhel-sap-hana-for-rhel-7-server-eus-debug-rpms")) bugIds.add("1491382");
					if (label.equals("rhel-sap-hana-for-rhel-7-server-eus-rpms")) bugIds.add("1491382");
					if (label.equals("rhel-sap-hana-for-rhel-7-server-eus-source-rpms")) bugIds.add("1491382");
					
					//	Bug 1491384 - content set mappings for "RHEL for SAP HANA for Power LE EUS" is missing from cdn/cs_mappings-prod.csv
					if (label.equals("rhel-sap-hana-for-rhel-7-for-power-le-eus-debug-rpms")) bugIds.add("1491384");
					if (label.equals("rhel-sap-hana-for-rhel-7-for-power-le-eus-rpms")) bugIds.add("1491384");
					if (label.equals("rhel-sap-hana-for-rhel-7-for-power-le-eus-source-rpms")) bugIds.add("1491384");
					
					// Bug 1521181 - content set mappings for "Red Hat Enterprise Linux High Availability (for IBM Power LE) - Extended Update Support" is missing from cdn/cs_mappings-prod.csv 
					if (label.equals("rhel-ha-for-rhel-7-server-for-power-le-eus-debug-rpms")) bugIds.add("1521181");
					if (label.equals("rhel-ha-for-rhel-7-server-for-power-le-eus-rpms")) bugIds.add("1521181");
					if (label.equals("rhel-ha-for-rhel-7-server-for-power-le-eus-source-rpms")) bugIds.add("1521181");
					
					
					BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
					// Object bugzilla, String eusProductName, String eusProductId, String eusContentSetName, String eusContentSetId, String eusContentSetLabel, List<String> modifiedProductIds
					ll.add(Arrays.asList(new Object[]{blockedByBzBug,  productName, productId, name, id, label, modifiedProductIds}));
				}
			}
		}
		
		// logging a semi-colon delimited map of repo label to information for debugging purposes importing into a spreadsheet
		log.info(" (DELETE COLUMN);ContentSet/Repo Label;Eng Prod ID;Eng Product Name;Content Set ID;Actual: Modifies Eng Prod IDs\rAssert that this list is not empty;");
		for (String key : eusLabelToInfoMap.keySet()) {
			// eusRepoLabel, fromEngProductId, fromEngProductName, whoseContentIdIs, modifiesTheseEngProductIds
			log.info(";"+key+";"+eusLabelToInfoMap.get(key)+";");
			// WAS IMPORTED TO: https://docs.google.com/a/redhat.com/spreadsheets/d/1oFCJ0KI2CjV1bOavNkKzZRJl-CZ6CopxngJi4EJbii0/edit?usp=sharing
		}
		return ll;
	}
	protected String getAllEUSProductContentSetDataAsListOfListsOwnerKey=null;
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 689031 - nss needs to be able to use pem files interchangeably in a single process https://github.com/RedHatQE/rhsm-qe/issues/127
	// TODO Bug 701425 - NSS issues with more than one susbcription https://github.com/RedHatQE/rhsm-qe/issues/128
	// TODO Bug 687970 - Currently no way to delete a content source from a product https://github.com/RedHatQE/rhsm-qe/issues/129
	// how to create content (see Bug 687970): [jsefler@jsefler ~]$ curl -u admin:admin -k --request POST https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/content --header "Content-type: application/json" -d '{"contentUrl":"/foo/path","label":"foolabel","type":"yum","gpgUrl":"/foo/path/gpg","id":"fooid","name":"fooname","vendor":"Foo Vendor"}' | python -m json.tool
	// how to delete content (see Bug 687970): [jsefler@jsefler ~]$ curl -u admin:admin -k --request DELETE https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/content/fooid
	// how to get content    (see Bug 687970): [jsefler@jsefler ~]$ curl -u admin:admin -k --request GET https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/content/fooid
	// how to associate content with product   (see Bug 687970): [jsefler@jsefler ~]$ curl -u admin:admin -k --request POST https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/product/productid/content/fooid&enabled=false
	
	
	
	
	
	
	
	
	
	
	// Configuration Methods ***********************************************************************
	
	@BeforeClass(groups={"setup"})
	public void removeYumBeakerRepos() {
		client.runCommandAndWait("mkdir -p /tmp/beaker.repos; mv -f /etc/yum.repos.d/beaker*.repo /tmp/beaker.repos");
	}
	
	@BeforeClass(groups={"setup"})
	public void setManageRepos() {
		clienttasks.config(null, null, true, new String[]{"rhsm","manage_repos","1"});
	}
	
	@AfterClass(groups={"setup"})
	public void restoreYumBeakerRepos() {
		client.runCommandAndWait("mv -f /tmp/beaker.repos/beaker*.repo /etc/yum.repos.d");
	}
	
	@AfterClass(groups={"setup"},dependsOnMethods={"restoreYumBeakerRepos"})
	public void restoreRedHatRelease() {
		// TEMPORARY WORKAROUND
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1526622"; // Bug 1526622 - the productid plugin should never delete a /etc/pki/product-default/<ID>.pem cert provided by the redhat-release-<VARIANT>.rpm
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			client.runCommandAndWait("yum reinstall redhat-release* --assumeyes --enablerepo=beaker*");
		}
		// END OF WORKAROUND
	}
	
	@AfterGroups(groups={"setup"}, value={
			"testInstallAndRemoveAnyPackageFromEnabledRepoAfterSubscribingToPool",
			"testInstallAndRemoveYumGroupFromEnabledRepoAfterSubscribingToPool",
			"testYumInstallSucceedsWhenServiceRsyslogIsStopped"},
			alwaysRun=true)
	public void restoreRedHatReleaseAfterGroups() {
		if (clienttasks==null) return;
		
		// might also have to re-install the default-product
		// TEMPORARY WORKAROUND
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1526622"; // Bug 1526622 - the productid plugin should never delete a /etc/pki/product-default/<ID>.pem cert provided by the redhat-release-<VARIANT>.rpm
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			restoreYumBeakerRepos();
			restoreRedHatRelease();
			removeYumBeakerRepos();
		}
		// END OF WORKAROUND
	}
	
	@AfterClass(groups={"setup"})
	@AfterGroups(groups={"setup"},value="InstallAndRemovePackageAfterSubscribingToPersonalSubPool_Test", alwaysRun=true)
	public void unregisterAfterGroupsInstallAndRemovePackageAfterSubscribingToPersonalSubPool_Test() {
		// first, unregister client1 since it is a personal subpool consumer
		client1tasks.unregister_(null,null,null, null);
		// second, unregister client2 since it is a personal consumer
		if (client2tasks!=null) {
			client2tasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, personalConsumerId, null, null, null, (String)null, null, null, null, Boolean.TRUE, null, null, null, null, null);
			client2tasks.unsubscribe_(true,(BigInteger)null, null, null, null, null, null);
			client2tasks.unregister_(null,null,null, null);
		}
	}

	@AfterGroups(groups={"setup"},value="SubscribabilityOfContentSetProduct_Tests")
	public void deleteFactsFileWithOverridingValuesAfterGroups() {
		if (clienttasks==null) return;
		clienttasks.deleteFactsFileWithOverridingValues();
	}
	
	
	@BeforeClass(groups="setup")
	public void createSubscriptionsWithVariationsOnContentSizes() throws JSONException, Exception {
		String marketingProductName,engineeringProductName,marketingProductId,engineeringProductId;
		Map<String,String> attributes = new HashMap<String,String>();
		if (server==null) {
			log.warning("Skipping createSubscriptionsWithVariationsOnContentSizes() when server is null.");
			return;	
		}
		
		// recreate a lot of content sets
		String contentIdStringFormat = "777%04d";
		for (int i = 1; i <= 200; i++) {
			String contentName = "Content Name "+i;
			String contentId = String.format(contentIdStringFormat,i);	// must be numeric (and unique)
			String contentLabel = "content-label-"+i;
			// include some "yum var"iability for testing bug 906554
			String yumVarPath="";
			if (i%5 == 0) yumVarPath+="$basearch/";
			if (i%10 == 0) yumVarPath+="$releasever/";
			if (i%15 == 0) yumVarPath+="$arch/";
			if (i%20 == 0) yumVarPath+="$uuid/";
			
			String resourcePath = "/content/"+contentId;
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
			CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
			CandlepinTasks.createContentUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, contentName, contentId, contentLabel, "yum", "Red Hat QE, Inc.", "/content/path/to/"+yumVarPath+contentLabel, "/gpg/path/to/"+yumVarPath+contentLabel, "3600", null, null, null);
		}
		
		// recreate a lot of arch-based content sets
		String archBasedContentIdStringFormat = "888%04d";
		for (int i = 1; i <= 200; i++) {
			String contentName = "Content Set "+i;
			String contentId = String.format(archBasedContentIdStringFormat,i);	// must be numeric (and unique)
			String contentLabel = "content-set-"+i;
			// include some "yum var"iability for testing bug 906554
			String yumVarPath="";
			if (i%5 == 0) yumVarPath+="$basearch/";
			if (i%10 == 0) yumVarPath+="$releasever/";
			if (i%15 == 0) yumVarPath+="$arch/";
			if (i%20 == 0) yumVarPath+="$uuid/";
			// include some required tags for the fun of it
			String requiredTags=null;
			if (i%4 == 0) requiredTags="rhel-5";
			if (i%8 == 0) requiredTags="rhel-6";
			if (i%16 == 0) requiredTags="rhel-7";
			if (i%44 == 0) requiredTags="";
			if (i%92 == 0) requiredTags="None";	// this is a legitimate and real tag and is interpreted as a real tag - None has no special meaning here
			// include some arches for the fun of it
			//	candlepin=# select * from cp_arch ;
			//	 id | label  | created | updated
			//	----+--------+---------+---------
			//	 0  | ALL    |         |
			//	 1  | x86_64 |         |
			//	 2  | i386   |         |
			//	 3  | i486   |         |
			//	 4  | i586   |         |
			//	 5  | i686   |         |
			//	 6  | ppc    |         |
			//	 7  | ppc64  |         |
			//	 8  | ia64   |         |
			//	 9  | arm    |         |
			//	 10 | s390   |         |
			//	 11 | s390x  |         |
			// ORIGINAL CANDLEPIN DESIGN REQUIRED THAT I SPECIFY A LIST OF ARCH OBJECT IDS AS FOLLOWS: "arches":[{"id":"1"},{"id":"2"}]}
			//[jsefler@jseflerT510 ~]$ curl --stderr /dev/null --insecure --user admin:admin -/path/to/content-label-199-arch","contentUrl":"/content/path/to/content-label-199-arch","vendor":"Red Hat QE, Inc.","name":"Content Name 199 arch","label":"content-label-199-arch","type":"yum","metadataExpire":"3600","arches":[{"id":"1"},{"id":"2"}]}' --header 'accept: application/json' --header 'content-type: application/json' https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/content
			//{"created":"2013-05-31T16:47:03.342+0000","updated":"2013-05-31T16:47:03.342+0000","id":"27770199","type":"yum","label":"content-label-199-arch","name":"Content Name 199 arch","vendor":"Red Hat QE, Inc.","contentUrl":"/content/path/to/content-label-199-arch","requiredTags":null,"releaseVer":null,"gpgUrl":"/gpg/path/to/content-label-199-arch","metadataExpire":3600,"modifiedProductIds":[],"arches":[{"id":"2","label":"i386"},{"id":"1","label":"x86_64"}]}
			//[jsefler@jseflerT510 ~]$ 
			// CURRENT DESIGN IS SIMPLIFIED TO A COMMA SEPARATED STRING
			String arches=null;
			if (i%2 == 0) arches="ppc64,arm,s390x,x86_64";
			if (i%6 == 0) arches="ia64,x86,ppc,s390";
			if (i%9 == 0) arches="ALL";
			if (i%18 == 0) arches="noarch";
			if (i%27 == 0) arches="ALL,noarch";
			if (i%16 == 0) arches="i386,i686";
			if (i%44 == 0) arches="";
			if (i%92 == 0) arches="None";	// this is a legitimate and real tag and is interpreted as a real arch - None has no special meaning here
			
			String resourcePath = "/content/"+contentId;
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
			CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
			CandlepinTasks.createContentUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, contentName, contentId, contentLabel, "yum", "Red Hat QE, Inc.", "/content/path/to/"+yumVarPath+contentLabel, "/gpg/path/to/"+yumVarPath+contentLabel, "3600", requiredTags, arches, null);
		}
	
		// recreate Subscription SKUs: subscriptionSKUProvidingA185ContentSetProduct, subscriptionSKUProvidingA186ContentSetProduct
		for (int N : new ArrayList<Integer>(Arrays.asList(185,186))) {	// 185 is the maximum number of content sets tolerated in a system.certificate_version < 3.0
			marketingProductName = String.format("Subscription providing a %s ContentSet Product",N);
			marketingProductId = "mktProductId-"+N;
			engineeringProductName = String.format("%s ContentSet Product",N);
			engineeringProductId = String.valueOf(N);	// must be numeric (and unique)
			attributes.clear();
			attributes.put("requires_consumer_type", "system");
			//attributes.put("sockets", "0");
			attributes.put("version", N+".0");
			//attributes.put("variant", "server");
			attributes.put("arch", "ALL");
			//attributes.put("warning_period", "30");
			// delete already existing subscription and products
			CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, marketingProductId);
			String resourcePath = "/products/"+marketingProductId;
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
			CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
			resourcePath = "/products/"+engineeringProductId;
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
			CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
			// create a new marketing product (MKT), engineering product (SVC), content for the engineering product, and a subscription to the marketing product that provides the engineering product
			attributes.put("type", "MKT");
			CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, marketingProductName, marketingProductId, 1, attributes, null);
			attributes.put("type", "SVC");
			CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, engineeringProductName, engineeringProductId, 1, attributes, null);
			for (int i = 1; i <= N; i++) {
				String contentId = String.format(contentIdStringFormat,i);	// must be numeric (and unique) defined above
				CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, engineeringProductId, contentId, /*randomGenerator.nextBoolean()*/i%3==0?true:false);	// WARNING: Be careful with the enabled flag! If the same content is enabled under one product and then disabled in another product, the tests to assert enabled or disabled will both fail due to conflict of interest.  Therefore use this flag with some pseudo-randomness 
			}
			CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, 20, -1*24*60/*1 day ago*/, 15*24*60/*15 days from now*/, getRandInt(), getRandInt(), marketingProductId, Arrays.asList(engineeringProductId), null);
		}
		
		// recreate Subscription SKU: subscriptionSKUProvidingTwo93ContentSetProducts
		marketingProductName = "Subscription providing two 93 ContentSet Products";
		marketingProductId = subscriptionSKUProvidingTwo93ContentSetProducts;
		String engineeringProductNameA = "93 ContentSet Product A";
		String engineeringProductNameB = "93 ContentSet Product B";
		String engineeringProductIdA = "931";	// must be numeric (and unique)
		String engineeringProductIdB = "932";	// must be numeric (and unique)
		attributes.clear();
		attributes.put("requires_consumer_type", "system");
		//attributes.put("sockets", "0");
		attributes.put("version", "93.0");
		//attributes.put("variant", "server");
		attributes.put("arch", "ALL");
		//attributes.put("warning_period", "30");
		// delete already existing subscription and products
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, marketingProductId);
		String resourcePath = "/products/"+marketingProductId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		resourcePath = "/products/"+engineeringProductIdA;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		resourcePath = "/products/"+engineeringProductIdB;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		// create a new marketing product (MKT), engineering product (SVC), content for the engineering product, and a subscription to the marketing product that provides the engineering product
		attributes.put("type", "MKT");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, marketingProductName, marketingProductId, 1, attributes, null);
		attributes.put("type", "SVC");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, engineeringProductNameA, engineeringProductIdA, 1, attributes, null);
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, engineeringProductNameB, engineeringProductIdB, 1, attributes, null);
		for (int i = 1; i <= 93; i++) {
			String contentId = String.format(contentIdStringFormat,i);	// must be numeric (and unique) defined above
			CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, engineeringProductIdA, contentId, /*randomGenerator.nextBoolean()*/i%3==0?true:false);	// WARNING: Be careful with the enabled flag! If the same content is enabled under one product and then disabled in another product, the tests to assert enabled or disabled will both fail due to conflict of interest.  Therefore use this flag with some pseudo-randomness 
			CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, engineeringProductIdB, contentId, /*randomGenerator.nextBoolean()*/i%3==0?true:false);	// WARNING: Be careful with the enabled flag! If the same content is enabled under one product and then disabled in another product, the tests to assert enabled or disabled will both fail due to conflict of interest.  Therefore use this flag with some pseudo-randomness 
		}
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, 20, -1*24*60/*1 day ago*/, 15*24*60/*15 days from now*/, getRandInt(), getRandInt(), marketingProductId, Arrays.asList(engineeringProductIdA,engineeringProductIdB), null);

		
		// recreate Subscription SKU: subscriptionSKUProvidingArchBasedContentSets
		int N = 200;
		marketingProductName = String.format("Subscription providing a Product with Arch-Based ContentSets",N);
		marketingProductId = "mktProductId-"+N;
		engineeringProductName = String.format("Product with various Arch-Based ContentSets",N);
		engineeringProductId = String.valueOf(N);	// must be numeric (and unique)
		attributes.clear();
		attributes.put("requires_consumer_type", "system");
		//attributes.put("sockets", "0");
		attributes.put("version", N+".0");
		//attributes.put("variant", "server");
		//attributes.put("arch", "ALL");
		attributes.put("arch", "x86_64,ppc64,ia64");	// give the product an arch too
		//attributes.put("warning_period", "30");
		// delete already existing subscription and products
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, marketingProductId);
		resourcePath = "/products/"+marketingProductId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		resourcePath = "/products/"+engineeringProductId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		// create a new marketing product (MKT), engineering product (SVC), content for the engineering product, and a subscription to the marketing product that provides the engineering product
		attributes.put("type", "MKT");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, marketingProductName, marketingProductId, 1, attributes, null);
		attributes.put("type", "SVC");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, engineeringProductName, engineeringProductId, 1, attributes, null);
		for (int i = 1; i <= N; i++) {
			String contentId = String.format(archBasedContentIdStringFormat,i);	// must be numeric (and unique) defined above
			CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, engineeringProductId, contentId, /*randomGenerator.nextBoolean()*/i%3==0?true:false);	// WARNING: Be careful with the enabled flag! If the same content is enabled under one product and then disabled in another product, the tests to assert enabled or disabled will both fail due to conflict of interest.  Therefore use this flag with some pseudo-randomness 
		}
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, 20, -1*24*60/*1 day ago*/, 15*24*60/*15 days from now*/, getRandInt(), getRandInt(), marketingProductId, Arrays.asList(engineeringProductId), null);

		
		// NOTE: To get the product certs, use the CandlepinTasks REST API:
        //"url": "/products/{product_uuid}/certificate", 
        //"GET"
	}
	
	// Protected Methods ***********************************************************************
	protected String personalConsumerId = null;
	protected String subscriptionSKUProvidingA185ContentSetProduct = "mktProductId-185";
	protected String subscriptionSKUProvidingA186ContentSetProduct = "mktProductId-186";
	protected String subscriptionSKUProvidingTwo93ContentSetProducts = "mktProductId-93x2";
	protected String subscriptionSKUProvidingArchBasedContent = "mktProductId-200";
	
	protected void VerifySubscribabilityOfSKUProvidingTooManyContentSets(String sku, int totalContentSets) {	//TODO remove parameter totalContentSets and make calls to CandepinTasks to find the totalContentSets from the sku
		
		Map<String,String> factsMap = new HashMap<String,String>();
		File entitlementCertFile;
		EntitlementCert entitlementCert;
		String systemCertificateVersionFactValue;
		SSHCommandResult sshCommandResult;
		String tooManyContentSetsMsgFormat = "Too many content sets for certificate. Please upgrade to a newer client to use subscription: %s";	// this msgid comes from Candlepin
		tooManyContentSetsMsgFormat = "Too many content sets for certificate %s. A newer client may be available to address this problem. See kbase https://access.redhat.com/knowledge/node/129003 for more information.";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.10-1"/*TODO FIXME ACTUAL IS "2.0.11-1"*/)) {	// Bug 1190814 - typos in candlepin msgid	// candlepin commit 18ede4fe2943f70b59f14a066a5ebd7b81525fad
			tooManyContentSetsMsgFormat = "Too many content sets for certificate %s. A newer client may be available to address this problem. See knowledge database https://access.redhat.com/knowledge/node/129003 for more information.";
		}
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", sku, clienttasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNotNull(pool,"Found an available pool to subscribe to productId '"+sku+"': "+pool);
	
		// test that it is NOT subscribable when system.certificate_version: None
		factsMap.put("system.certificate_version", null);
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		Assert.assertEquals(clienttasks.getFactValue("system.certificate_version"), "None", "When the system.certificate_version fact is null, its fact value is reported as 'None'.");
		sshCommandResult = clienttasks.subscribe_(null, null, pool.poolId, null, null, null, null, null, null, null, null, null, null);
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(70);	// EX_SOFTWARE	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		Assert.assertEquals(sshCommandResult.getStderr().trim(), String.format(tooManyContentSetsMsgFormat, pool.subscriptionName), "Stderr from an attempt to subscribe to '"+pool.subscriptionName+"' that provides product(s) with many content sets (totalling >185) when system.certificate_version is null");
		Assert.assertEquals(sshCommandResult.getStdout().trim(), "", "Stdout from an attempt to subscribe to '"+pool.subscriptionName+"' that provides product(s) with many content sets (totalling >185) when system.certificate_version is null");
		Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode, "Exitcode from an attempt to subscribe to '"+pool.subscriptionName+"' that provides product(s) with many content sets (totalling >185) when system.certificate_version is null");
		Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().isEmpty(), "No entitlements should be consumed after attempt to subscribe to '"+pool.subscriptionName+"' that provides product(s) with many content sets (totalling >185) when system.certificate_version is null");
		
		// test that it is NOT subscribable when system.certificate_version: 1.0
		factsMap.put("system.certificate_version", "1.0");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		Assert.assertEquals(clienttasks.getFactValue("system.certificate_version"), "1.0", "When the system.certificate_version fact is 1.0, its fact value is reported as '1.0'.");
		sshCommandResult = clienttasks.subscribe_(null, null, pool.poolId, null, null, null, null, null, null, null, null, null, null);
		Assert.assertEquals(sshCommandResult.getStderr().trim(), String.format(tooManyContentSetsMsgFormat, pool.subscriptionName), "Stderr from an attempt to subscribe to '"+pool.subscriptionName+"' that provides product(s) with many content sets (totalling >185) when system.certificate_version is 1.0");
		Assert.assertEquals(sshCommandResult.getStdout().trim(), "", "Stdout from an attempt to subscribe to '"+pool.subscriptionName+"' that provides product(s) with many content sets (totalling >185) when system.certificate_version is 1.0");
		Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode, "Exitcode from an attempt to subscribe to '"+pool.subscriptionName+"' that provides product(s) with many content sets (totalling >185) when system.certificate_version is 1.0");
		Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().isEmpty(), "No entitlements should be consumed after attempt to subscribe to '"+pool.subscriptionName+"' that provides product(s) with many content sets (totalling >185) when system.certificate_version is 1.0");

		// test that it is subscribable when system.certificate_version is the system's default value (should be >=3.0)
		clienttasks.deleteFactsFileWithOverridingValues();
		systemCertificateVersionFactValue = clienttasks.getFactValue("system.certificate_version");
		Assert.assertTrue(Float.valueOf(systemCertificateVersionFactValue)>=3.0, "The actual default system.certificate_version fact '"+systemCertificateVersionFactValue+"' is >= 3.0.");
		clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null, null, null);
		entitlementCert = clienttasks.getEntitlementCertCorrespondingToSubscribedPool(pool);
		entitlementCertFile = clienttasks.getEntitlementCertFileFromEntitlementCert(entitlementCert);
		// TOO ASSERTIVE Assert.assertTrue(Float.valueOf(entitlementCert.version)<=Float.valueOf(systemCertificateVersionFactValue),"The version of the entitlement certificate '"+entitlementCert.version+"' granted by candlepin is less than or equal to the system.certificate_version '"+systemCertificateVersionFactValue+"' which indicates the maximum certificate version this system knows how to handle.");	// This assert was too assertive according to https://bugzilla.redhat.com/show_bug.cgi?id=1425236#c2
		Assert.assertEquals(Float.valueOf(entitlementCert.version).intValue(),Float.valueOf(systemCertificateVersionFactValue).intValue(),"The major value of the entitlement certificate '"+entitlementCert.version+"' granted by candlepin matches the major value of the system.certificate_version '"+systemCertificateVersionFactValue+"' which indicates certificate compatibility.");	// Reference: https://bugzilla.redhat.com/show_bug.cgi?id=1425236#c2
		Assert.assertEquals(entitlementCert.contentNamespaces.size(), totalContentSets, "The number of content sets provided in this version '"+entitlementCert.version+"' entitlement cert parsed using the rct cat-cert tool.");
		clienttasks.assertEntitlementCertsInYumRepolist(Arrays.asList(entitlementCert), true);
		clienttasks.unsubscribeFromSerialNumber(clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCertFile));
	}
	
	protected List<String> getYumDependencyPackagesInstalledFromYumInstallPackageResult(SSHCommandResult yumInstallPackageResult) {
		
		// partial stdout from yumInstallPackageResult
		//	Installed:
		//	  cman.x86_64 0:3.0.12.1-19.el6
		//
		//	Dependency Installed:
		//	  clusterlib.x86_64 0:3.0.12.1-19.el6 corosync.x86_64 0:1.4.1-3.el6
		//	  corosynclib.x86_64 0:1.4.1-3.el6 fence-agents.x86_64 0:3.1.5-9.el6
		//	  fence-virt.x86_64 0:0.2.3-4.el6 ipmitool.x86_64 0:1.8.11-12.el6
		//	  libibverbs.x86_64 0:1.1.5-3.el6 librdmacm.x86_64 0:1.0.14.1-3.el6
		//	  lm_sensors-libs.x86_64 0:3.1.1-10.el6 modcluster.x86_64 0:0.16.2-13.el6
		//	  net-snmp-libs.x86_64 1:5.5-37.el6 net-snmp-utils.x86_64 1:5.5-37.el6
		//	  nss-tools.x86_64 0:3.12.10-4.el6 oddjob.x86_64 0:0.30-5.el6
		//	  openais.x86_64 0:1.1.1-7.el6 openaislib.x86_64 0:1.1.1-7.el6
		//	  perl-Net-Telnet.noarch 0:3.03-11.el6 pexpect.noarch 0:2.3-6.el6
		//	  python-suds.noarch 0:0.4.1-3.el6 ricci.x86_64 0:0.16.2-42.el6
		//	  sg3_utils.x86_64 0:1.28-4.el6 telnet.x86_64 1:0.17-47.el6
		//
		//	Complete!
		
		// partial stdout from yumInstallPackageResult
		//	Installed:
		//	  saslwrapper-devel.x86_64 0:0.10-5.el5                                                  
		//
		//	Dependency Installed:
		//	  saslwrapper.x86_64 0:0.10-5.el5                                                        
		//
		//	Complete!
		
		// partial stdout from yumInstallPackageResult
		//	Installed:
		//	  zsh-html.x86_64 0:4.2.6-9.el5                                                          
		//
		//	Complete!
		
		String regex = "Dependency Installed:(\n.*?)+Complete!";
		List<String> matches = getSubstringMatches(yumInstallPackageResult.getStdout(), regex);
		if (matches.size()>1) Assert.fail("Unexpectedly encountered more than one match to '"+regex+"' during a former call to yum install package.");	// no dependencies installed
		if (matches.isEmpty()) return new ArrayList<String>();	// return empty list
			
		return Arrays.asList(matches.get(0).replaceFirst("Dependency Installed:", "").replaceFirst("Complete!", "").trim().split("\\s*\\n\\s*"));
	}
	
	protected List<String> getYumDependencyPackagesRemovedFromYumRemovePackageResult(SSHCommandResult yumRemovePackageResult) {
		
		//	ssh root@hp-xw9300-01.rhts.eng.bos.redhat.com yum -y remove libibverbs-rocee.x86_64 0:1.1.7-1.1.el6_5 --disableplugin=rhnplugin
		//	Stdout:
		//	Loaded plugins: product-id, security, subscription-manager
		//	No plugin match for: rhnplugin
		//	Setting up Remove Process
		//	Resolving Dependencies
		//	--> Running transaction check
		//	---> Package libibverbs-rocee.x86_64 0:1.1.7-1.1.el6_5 will be erased
		//	--> Processing Dependency: libibverbs.so.1()(64bit) for package: libmlx4-rocee-1.0.5-1.1.el6_5.x86_64
		//	--> Processing Dependency: libibverbs.so.1(IBVERBS_1.0)(64bit) for package: libmlx4-rocee-1.0.5-1.1.el6_5.x86_64
		//	--> Processing Dependency: libibverbs.so.1(IBVERBS_1.1)(64bit) for package: libmlx4-rocee-1.0.5-1.1.el6_5.x86_64
		//	--> Running transaction check
		//	---> Package libmlx4-rocee.x86_64 0:1.0.5-1.1.el6_5 will be erased
		//	--> Finished Dependency Resolution
		//
		//	Dependencies Resolved
		//
		//	================================================================================
		//	Package Arch Version Repository Size
		//	================================================================================
		//	Removing:
		//	libibverbs-rocee
		//	x86_64 1.1.7-1.1.el6_5 @rhel-hpn-for-rhel-6-hpc-node-rpms 100 k
		//	Removing for dependencies:
		//	libmlx4-rocee x86_64 1.0.5-1.1.el6_5 @rhel-hpn-for-rhel-6-hpc-node-rpms 50 k
		//
		//	Transaction Summary
		//	================================================================================
		//	Remove 2 Package(s)
		//
		//	Installed size: 150 k
		//	Downloading Packages:
		//	Running rpm_check_debug
		//	Running Transaction Test
		//	Transaction Test Succeeded
		//	Running Transaction
		//
		//	Erasing : libmlx4-rocee-1.0.5-1.1.el6_5.x86_64 1/2
		//
		//	Erasing : libibverbs-rocee-1.1.7-1.1.el6_5.x86_64 2/2
		//
		//	Verifying : libmlx4-rocee-1.0.5-1.1.el6_5.x86_64 1/2
		//
		//	Verifying : libibverbs-rocee-1.1.7-1.1.el6_5.x86_64 2/2
		//
		//	Removed:
		//	libibverbs-rocee.x86_64 0:1.1.7-1.1.el6_5
		//
		//	Dependency Removed:
		//	libmlx4-rocee.x86_64 0:1.0.5-1.1.el6_5
		//
		//	Complete!
		//	Stderr: No Match for argument: 0:1.1.7-1.1.el6_5
		//	ExitCode: 0
		
		String regex = "Dependency Removed:(\n.*?)+Complete!";
		List<String> matches = getSubstringMatches(yumRemovePackageResult.getStdout(), regex);
		if (matches.size()>1) Assert.fail("Unexpectedly encountered more than one match to '"+regex+"' during a former call to yum remove package.");
		if (matches.isEmpty()) return new ArrayList<String>();	// return empty list
			
		return Arrays.asList(matches.get(0).replaceFirst("Dependency Removed:", "").replaceFirst("Complete!", "").trim().split("\\s*\\n\\s*"));
	}
	
	
	
	// Data Providers ***********************************************************************

	
	@DataProvider(name="getPackageFromEnabledRepoAndSubscriptionPoolData")
	public Object[][] getPackageFromEnabledRepoAndSubscriptionPoolDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getPackageFromEnabledRepoAndSubscriptionPoolDataAsListOfLists());
	}
	protected List<List<Object>> getPackageFromEnabledRepoAndSubscriptionPoolDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (clienttasks==null) return ll;
		if (sm_clientUsername==null) return ll;
		if (sm_clientPassword==null) return ll;
		
		// get the currently installed product certs to be used when checking for conditional content tagging
		List<ProductCert> currentProductCerts = clienttasks.getCurrentProductCerts();
		
		// assure we are freshly registered and process all available subscription pools
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, ConsumerType.system, null, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null);
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			String quantity = null;
			/*if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.3-1"))*/ if (pool.suggested!=null) if (pool.suggested<1) quantity = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "instance_multiplier"); 	// when the Suggested quantity is 0, let's specify a quantity to avoid Stdout: Quantity '1' is not a multiple of instance multiplier '2'
			File entitlementCertFile = clienttasks.subscribeToSubscriptionPool_(pool,quantity);
			Assert.assertNotNull(entitlementCertFile, "Found the entitlement cert file that was granted after subscribing to pool: "+pool);
			EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;
				if (contentNamespace.enabled && clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, currentProductCerts)) {
					String repoLabel = contentNamespace.label;
					
					// find an available package that is uniquely provided by repo
					String pkg = clienttasks.findUniqueAvailablePackageFromRepo(repoLabel);
					if (pkg==null) {
						log.warning("Could NOT find a unique available package from repo '"+repoLabel+"' after subscribing to SubscriptionPool: "+pool);
					}

					// String availableGroup, String installedGroup, String repoLabel, SubscriptionPool pool, String quantity
					ll.add(Arrays.asList(new Object[]{pkg, repoLabel, pool, quantity}));
				}
			}
			clienttasks.unsubscribeFromSerialNumber(clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCertFile));

			// minimize the number of dataProvided rows (useful during automated testcase development)
			if (Boolean.valueOf(getProperty("sm.debug.dataProviders.minimize","false"))) break;
		}
		
		return ll;
	}
	
	
	@DataProvider(name="getEnabledRepoAndSubscriptionPoolData")
	public Object[][] getEnabledRepoAndSubscriptionPoolDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getEnabledRepoAndSubscriptionPoolDataAsListOfLists());
	}
	protected List<List<Object>> getEnabledRepoAndSubscriptionPoolDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (clienttasks==null) return ll;
		if (sm_clientUsername==null) return ll;
		if (sm_clientPassword==null) return ll;
		
		// get the currently installed product certs to be used when checking for conditional content tagging
		List<ProductCert> currentProductCerts = clienttasks.getCurrentProductCerts();
		
		// assure we are freshly registered and process all available subscription pools
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, ConsumerType.system, null, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null);
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
///*debugTesting*/if (!pool.productId.equals("RH2501844")) continue; 
			String quantity = null;
			/*if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.3-1"))*/ if (pool.suggested!=null) if (pool.suggested<1) quantity = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "instance_multiplier"); 	// when the Suggested quantity is 0, let's specify a quantity to avoid Stdout: Quantity '1' is not a multiple of instance multiplier '2'
			File entitlementCertFile = clienttasks.subscribeToSubscriptionPool_(pool,quantity);
			Assert.assertNotNull(entitlementCertFile, "Found the entitlement cert file that was granted after subscribing to pool: "+pool);
			EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;
				if (contentNamespace.enabled && clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, currentProductCerts)) {
					String repoLabel = contentNamespace.label;
					
					// String availableGroup, String installedGroup, String repoLabel, SubscriptionPool pool, String quantity
					ll.add(Arrays.asList(new Object[]{repoLabel, pool, quantity}));
				}
			}
			
			// minimize the number of dataProvided rows (useful during automated testcase development)
			if (Boolean.valueOf(getProperty("sm.debug.dataProviders.minimize","false"))) break;
		}
		
		// no reason to remain subscribed to any subscriptions
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		
		return ll;
	}
	
	
	@DataProvider(name="getYumGroupFromEnabledRepoAndSubscriptionPoolData")
	public Object[][] getYumGroupFromEnabledRepoAndSubscriptionPoolDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getYumGroupFromEnabledRepoAndSubscriptionPoolDataAsListOfLists());
	}
	protected List<List<Object>> getYumGroupFromEnabledRepoAndSubscriptionPoolDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (clienttasks==null) return ll;
		if (sm_clientUsername==null) return ll;
		if (sm_clientPassword==null) return ll;
		
		// get the currently installed product certs to be used when checking for conditional content tagging
		List<ProductCert> currentProductCerts = clienttasks.getCurrentProductCerts();
		
		// assure we are freshly registered and process all available subscription pools
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, ConsumerType.system, null, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null);
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
///*debugTesting*/ if (!pool.productId.equals("RH2501844")) continue;			
			// avoid throttling RateLimitExceededException from IT-Candlepin
			if (CandlepinType.hosted.equals(sm_serverType)) {	// strategically get a new consumer to avoid 60 repeated API calls from the same consumer
				// re-register as a new consumer
				clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, ConsumerType.system, null, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null);
			}
			
			File entitlementCertFile = clienttasks.subscribeToSubscriptionPool_(pool);
			Assert.assertNotNull(entitlementCertFile, "Found the entitlement cert file that was granted after subscribing to pool: "+pool);
			EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;
				
				if (contentNamespace.enabled && clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, currentProductCerts)) {
					String repoLabel = contentNamespace.label;
					
					// find first available group provided by this repo
					String availableGroup = clienttasks.findAnAvailableGroupFromRepo(repoLabel);
					// find first installed group provided by this repo
					String installedGroup = clienttasks.findAnInstalledGroupFromRepo(repoLabel);
					
					// String availableGroup, String installedGroup, String repoLabel, SubscriptionPool pool
					ll.add(Arrays.asList(new Object[]{availableGroup, installedGroup, repoLabel, pool}));
				}
			}
			clienttasks.unsubscribeFromSerialNumber(clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCertFile));
			
			// minimize the number of dataProvided rows (useful during automated testcase development)
			if (Boolean.valueOf(getProperty("sm.debug.dataProviders.minimize","false")) && !ll.isEmpty()) break;
		}
		
		return ll;
	}
	@DataProvider(name="getYumAvailableGroupFromEnabledRepoAndSubscriptionPoolData")
	public Object[][] getYumAvailableGroupFromEnabledRepoAndSubscriptionPoolDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getYumAvailableGroupFromEnabledRepoAndSubscriptionPoolDataAsListOfLists());
	}
	protected List<List<Object>> getYumAvailableGroupFromEnabledRepoAndSubscriptionPoolDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		for (List<Object> list : getYumGroupFromEnabledRepoAndSubscriptionPoolDataAsListOfLists()) {
			// list contains:  String availableGroup, String installedGroup, String repoLabel, SubscriptionPool pool
			ll.add(Arrays.asList(new Object[]{list.get(0), /*exclude installedGroup,*/ list.get(2), list.get(3)}));
		}
		
		return ll;
	}
	@DataProvider(name="getYumInstalledGroupFromEnabledRepoAndSubscriptionPoolData")
	public Object[][] getYumInstalledGroupFromEnabledRepoAndSubscriptionPoolDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getYumInstalledGroupFromEnabledRepoAndSubscriptionPoolDataAsListOfLists());
	}
	protected List<List<Object>> getYumInstalledGroupFromEnabledRepoAndSubscriptionPoolDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		for (List<Object> list : getYumGroupFromEnabledRepoAndSubscriptionPoolDataAsListOfLists()) {
			// list contains:  String availableGroup, String installedGroup, String repoLabel, SubscriptionPool pool
			ll.add(Arrays.asList(new Object[]{/* exclude availableGroup*/ list.get(1), list.get(2), list.get(3)}));
		}
		
		return ll;
	}
	
	
	@DataProvider(name="getPackageFromEnabledRepoAndPersonalSubscriptionSubPoolData")
	public Object[][] getPackageFromEnabledRepoAndPersonalSubscriptionSubPoolDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getPackageFromEnabledRepoAndPersonalSubscriptionSubPoolDataAsListOfLists());
	}
	protected List<List<Object>> getPackageFromEnabledRepoAndPersonalSubscriptionSubPoolDataAsListOfLists() throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (client1tasks==null) return ll;
		if (client2tasks==null) return ll;
		
		// assure we are registered (as a person on client2 and a system on client1)
		
		// register client1 as a system under rhpersonalUsername
		client1tasks.register(sm_rhpersonalUsername, sm_rhpersonalPassword, sm_rhpersonalOrg, null, ConsumerType.system, null, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null);
		
		// register client2 as a person under rhpersonalUsername
		client2tasks.register(sm_rhpersonalUsername, sm_rhpersonalPassword, sm_rhpersonalOrg, null, ConsumerType.person, null, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null);
		
		// subscribe to the personal subscription pool to unlock the subpool
		personalConsumerId = client2tasks.getCurrentConsumerId();
		for (int j=0; j<sm_personSubscriptionPoolProductData.length(); j++) {
			JSONObject poolProductDataAsJSONObject = (JSONObject) sm_personSubscriptionPoolProductData.get(j);
			String personProductId = poolProductDataAsJSONObject.getString("personProductId");
			JSONObject subpoolProductDataAsJSONObject = poolProductDataAsJSONObject.getJSONObject("subPoolProductData");
			String systemProductId = subpoolProductDataAsJSONObject.getString("systemProductId");

			SubscriptionPool personPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId",personProductId,client2tasks.getCurrentlyAvailableSubscriptionPools());
			Assert.assertNotNull(personPool,"Personal productId '"+personProductId+"' is available to user '"+sm_rhpersonalUsername+"' registered as a person.");
			File entitlementCertFile = client2tasks.subscribeToSubscriptionPool_(personPool);
			Assert.assertNotNull(entitlementCertFile, "Found the entitlement cert file that was granted after subscribing to personal pool: "+personPool);

			
			// now the subpool is available to the system
			SubscriptionPool systemPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId",systemProductId,client1tasks.getCurrentlyAvailableSubscriptionPools());
			Assert.assertNotNull(systemPool,"Personal subPool productId'"+systemProductId+"' is available to user '"+sm_rhpersonalUsername+"' registered as a system.");
			//client1tasks.subscribeToSubscriptionPool(systemPool);
			
			entitlementCertFile = client1tasks.subscribeToSubscriptionPool_(systemPool);
			Assert.assertNotNull(entitlementCertFile, "Found the entitlement cert file that was granted after subscribing to system pool: "+systemPool);
			EntitlementCert entitlementCert = client1tasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;
				if (contentNamespace.enabled) {
					String repoLabel = contentNamespace.label;
					
					// find an available package that is uniquely provided by repo
					String pkg = client1tasks.findUniqueAvailablePackageFromRepo(repoLabel);
					if (pkg==null) {
						log.warning("Could NOT find a unique available package from repo '"+repoLabel+"' after subscribing to SubscriptionSubPool: "+systemPool);
					}

					// String availableGroup, String installedGroup, String repoLabel, SubscriptionPool pool
					ll.add(Arrays.asList(new Object[]{pkg, repoLabel, systemPool}));
				}
			}
			client1tasks.unsubscribeFromSerialNumber(client1tasks.getSerialNumberFromEntitlementCertFile(entitlementCertFile));
		}
		return ll;
	}
	
	
	
	@DataProvider(name="getAllAvailableSubscriptionPoolsProvidingArchBasedContentData")
	public Object[][] getAllAvailableSubscriptionPoolsProvidingArchBasedContentDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAllAvailableSubscriptionPoolsProvidingArchBasedContentDataAsListOfLists());
	}
	protected List<List<Object>> getAllAvailableSubscriptionPoolsProvidingArchBasedContentDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		String ownerKey = null;
		JSONObject jsonStatus = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(/*authenticator*/null,/*password*/null,sm_serverUrl,"/status"));


		for (List<Object> l : getAllAvailableSubscriptionPoolsDataAsListOfLists()) {
			SubscriptionPool pool = (SubscriptionPool)l.get(0);
			if (ownerKey==null) ownerKey = clienttasks.getCurrentlyRegisteredOwnerKey();
			
			for (String providedProductId : CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername,sm_clientPassword,sm_serverUrl,pool.poolId)) {
	
				// get the product
				String path = "/products/"+providedProductId;
				if (SubscriptionManagerTasks.isVersion(jsonStatus.getString("version"),">=","2.0.11")) path = "/owners/"+ownerKey+path;	// starting with candlepin-2.0.11 /products/<ID> are requested by /owners/<KEY>/products/<ID> OR /products/<UUID>
				JSONObject jsonProduct = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,path));	
				
				// get the provided product contents
				JSONArray jsonProductContents = jsonProduct.getJSONArray("productContent");
				for (int j = 0; j < jsonProductContents.length(); j++) {
					JSONObject jsonProductContent = (JSONObject) jsonProductContents.get(j);
					JSONObject jsonContent = jsonProductContent.getJSONObject("content");
					
					// is this arch-based content?
					if (jsonContent.has("arches") && !jsonContent.isNull("arches")) {
						if (!ll.contains(l)) {	// add this row only once
							ll.add(l);
						}
						break;
					}
				}
			}
		}
		
		return ll;
	}
}
