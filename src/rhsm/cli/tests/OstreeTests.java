package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.OstreeRepo;
import rhsm.data.ProductCert;
import rhsm.data.SubscriptionPool;
import rhsm.data.YumRepo;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 * 
 *  Setting up atomic: host https://mojo.redhat.com/docs/DOC-967002
 *  Quick start guide: http://file.brq.redhat.com/~bexelbie/atomic.html
 *
 * DEV Sprint 75 Demo
 *  Subscription Manager Content Plugins (alikins) 
 *  Subscription Manager OSTree Content Plugin (dgoodwin)
 *    Video: https://sas.elluminate.com/p.jnlp?psid=2014-05-21.0648.M.F65D5B6FD8876C925F34AFEC9FB7E7.vcr&sid=819
 *    
 * Basic scenario:
 *  See step 3a and 3b to fake an atomic system....   TODO NOTES FROM http://pastebin.test.redhat.com/238880
 *  
 *  1. Install package subscription-manager-plugin-ostree
 *  2. Ensure /etc/rhsm/pluginconf.d/ostree_content.OstreeContentPlugin.conf is enabled
 *  3. Ensure we are on a real Atomic system with ostree and rpm-ostree packages installed.
 *     If not, we can fake an atomic system as follows...
 *  3a. Ensure an ostree repo config file is installed (TODO not sure what package provides it - file /ostree/repo/config is not owned by any package)
 *  
 *  -bash-4.2# cat /ostree/repo/config              <<<< EXAMPLE BEFORE ATOMIC SUB IS ATTACHED
 *  [core]
 *  repo_version=1
 *  mode=bare
 *  
 *  -bash-4.2# cat /ostree/repo/config              <<<< EXAMPLE AFTER ATOMIC SUB IS ATTACHED
 *  [core]
 *  repo_version=1
 *  mode=bare
 *  
 *  [remote "rhel-atomic-preview-ostree"]
 *  url = https://cdn.redhat.com/content/preview/rhel/atomic/7/x86_64/ostree/repo
 *  gpg-verify = false
 *  tls-client-cert-path = /etc/pki/entitlement/6474260223991696217.pem
 *  tls-client-key-path = /etc/pki/entitlement/6474260223991696217-key.pem
 *  tls-ca-path = /etc/rhsm/ca/redhat-uep.pem
 *  
 *  3b. Ensure gi_wrapper.py tool is returning a known path to the ostree origin file
 *      help="Print the path to the current deployed OSTree origin file."
 *  
 *  -bash-4.2# python /usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py --deployed-origin
 *  /ostree/deploy/rhel-atomic-host/deploy/7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1.0.origin
 *  -bash-4.2# 
 *  -bash-4.2# cat /ostree/deploy/rhel-atomic-host/deploy/7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1.0.origin
 *  [origin]
 *  refspec=rhel-atomic-preview-ostree:rhel-atomic-host/7/x86_64/standard
 *  -bash-4.2# 
 *  
 *  If gi_wrapper.py is not returning a path to the origin because TODO ostree is not installed, then create a faux file like this
 *  -bash-4.2# cat /usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py
 *  #!/usr/bin/python
 *  # This fake tool provides the path to the ostree 'origin' file.
 *  print "/ostree/deploy/rhel-atomic-host/deploy/a3f91cda6db91c1628ee865fdac75e5348dc7fabb78a73d2e33ab544bbc41f8c.0.origin"
 *  
 *  3c. Ensure the path to the ostree 'origin' file exists (Note the format of refspec=TODO A:TODO B/7/x86_64/standard.
 *  -bash-4.2# cat /ostree/deploy/rhel-atomic-host/deploy/7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1.0.origin
 *  [origin]
 *  refspec=rhel-atomic-preview-ostree:rhel-atomic-host/7/x86_64/standard
 *  
 *  4. Attach an atomic subscription (one that provides content of type="ostree"
 *  
 *  5. The subscription-manager content plugin for ostree is invoked when it sees the "ostree" content type
 *  
 *  6. ostree content plugin runs gi_wrapper.py to figure out what ostree TODO 'A' is currently running system is
 *   (running code from the 'ostree' and 'rpm-ostree' rpms to do that)
 *   
6. ostree content plugin runs gi_wrapper.py to figure out what ostree 'b' currently running system is
   (running code from the 'ostree' and 'rpm-ostree' rpms to do that)
 
7. Does some name matching to see if content is the right one for currently running system.
   Currently, thats matching content label to 'b' from orgin and any existing 'remote' in /ostree/repo/config
 
8. /ostree/repo/config is updated with new "remotes" that map to each content of type 'ostree'
    (current cases should be 1 ostree content, but could change)
 
9. Update the "origin" "b" if needed
  9a. If the new content/remote is the first content added to /ostree/repo/config from an ent cert,
      then origin 'b' becomes the new remote 'name' (first paid RH product installed gets to be origin)
 
# if on a real ostree system
 
10. run 'atomic upgrade' (formerly known as 'rpm-ostree')
   <stuff downloads from cdn to somewhere...>
 
11. reboot

 *  
 *  
 *  
 */
@Test(groups={"debugTest","OstreeTests","Tier3Tests"})
public class OstreeTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="Verify that the ostree config and origin files are and that when in container mode, attempts to run subscription-manager are blocked",
			groups={"VerifyOstreeConfigurationsAreSetAfterSubscribing_Test","AcceptanceTests"},
			dataProvider="getOstreeSubscriptions",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyOstreeConfigurationsAreSetAfterSubscribing_Test(Object bugzilla, SubscriptionPool osTreeSubscriptionPool) {

		// get a list of the ostree repos from the ostree repo config file before attaching an ostree subscription
		List<OstreeRepo> ostreeReposBefore = getCurrentlyConfiguredOstreeRepos();
		
		// attach the subscription that provides ostree content
		File file = clienttasks.subscribeToSubscriptionPool(osTreeSubscriptionPool, sm_clientUsername, sm_clientPassword, sm_serverUrl);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(file);
		
		// get a list of the ostree repos from the ostree repo config file after attaching an ostree subscription
		List<OstreeRepo> ostreeReposAfter = getCurrentlyConfiguredOstreeRepos();

		// assert that ostree repos have been added for each ostree content namespace
		for (ContentNamespace osTreeContentNamespace : ContentNamespace.findAllInstancesWithMatchingFieldFromList("type", "ostree", entitlementCert.contentNamespaces)) {
			
			log.info("Asserting the following ostree contentNamespace is added to ostree config file '"+ostreeRepoConfigFile+"' : "+osTreeContentNamespace);
			//	-bash-4.2# rct cat-cert /etc/pki/entitlement/6474260223991696217.pem | grep "Type: ostree" -A10 -B1
			//	Content:
			//		Type: ostree
			//		Name: Red Hat Enterprise Linux Atomic Host Preview (Trees)
			//		Label: rhel-atomic-preview-ostree
			//		Vendor: Red Hat
			//		URL: /content/preview/rhel/atomic/7/x86_64/ostree/repo
			//		GPG: http://
			//		Enabled: False
			//		Expires: 86400
			//		Required Tags: 
			//		Arches: x86_64
			//
			//	-bash-4.2# cat /ostree/repo/config
			//	[core]
			//	repo_version=1
			//	mode=bare
			
			//	[remote "rhel-atomic-preview-ostree"]
			//	url = https://cdn.redhat.com/content/preview/rhel/atomic/7/x86_64/ostree/repo
			//	gpg-verify = false
			//	tls-client-cert-path = /etc/pki/entitlement/6474260223991696217.pem
			//	tls-client-key-path = /etc/pki/entitlement/6474260223991696217-key.pem
			//	tls-ca-path = /etc/rhsm/ca/redhat-uep.pem
			

			//	[root@jsefler-os7 ~]# rct cat-cert /etc/pki/entitlement/1174650091385378526.pem | grep -A10 Content:
			//		Content:
			//			Type: ostree
			//			Name: awesomeos-ostree
			//			Label: awesomeos-ostree
			//			Vendor: Red Hat
			//			URL: /path/to/awesomeos-ostree
			//			GPG: /path/to/awesomeos/gpg/
			//			Enabled: True
			//			Expires: 
			//			Required Tags: 
			//			Arches: ALL
			
			//	[root@jsefler-os7 ~]# cat /ostree/repo/config
			//	[core]
			//	repo_version=1
			//	mode=bare
			//
			//	[remote "awesomeos-ostree"]
			//	url = https://cdn.qa.redhat.com/path/to/awesomeos-ostree
			//	gpg-verify = true
			//	tls-client-cert-path = /etc/pki/entitlement/1174650091385378526.pem
			//	tls-client-key-path = /etc/pki/entitlement/1174650091385378526-key.pem
			//	tls-ca-path = /etc/rhsm/ca/redhat-uep.pem
			
			
			

		}
		
		// throw a failure on the subscription if there is more than one ostree repo
		// if there is more than one ostree repo, then it is undefined which repo remote to set in the ostree origin file
		
		
		
		
		// assert that the ostree origin file has been updated to the newly entitled ostree repo remote
		
		//	-bash-4.2# python /usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py --deployed-origin
		//	/ostree/deploy/rhel-atomic-host/deploy/7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1.0.origin
		//	-bash-4.2# cat /ostree/deploy/rhel-atomic-host/deploy/7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1.0.origin
		//	[origin]
		//	refspec=rhel-atomic-preview-ostree:rhel-atomic-host/7/x86_64/standard

		
		
		// TODO run some ostree commands to assert that ostree is happy with the new remote
		
		
		// randomly choose to unsubscribe and assert...
		
		
		// when removing the entitlement, assert it's corresponding ostree repos are removed
		
		
		// when removing the entitlement, assert the ostree origin file remains unchanged
	}
	
	
	
//	@AfterClass(groups={"setup"})	// insurance
//	@AfterGroups(groups={"setup"}, value={"VerifySubscriptionManagementCommandIsDisabledInContainerMode_Test"})
//	public void teardownContainerMode() {
//		if (clienttasks!=null) {
//			client.runCommandAndWait("rm -rf "+rhsmHostDir);
//			client.runCommandAndWait("rm -rf "+entitlementHostDir);	// although it would be okay to leave this behind
//		}
//	}
//	
	@BeforeGroups(groups={"setup"}, value={"VerifyOstreeConfigurationsAreSetAfterSubscribing_Test"})
	protected void setupOstreeRepoConfigFile() {
		if (clienttasks!=null) {
			if (!clienttasks.isPackageInstalled("ostree")) {	// file /ostree/repo/config is not owned by any package	// create a fake /ostree/repo/config
				if (!RemoteFileTasks.testExists(client, ostreeRepoConfigFile.getPath())) {
					client.runCommandAndWait("mkdir -p "+ostreeRepoConfigFile.getParent());
					client.runCommandAndWait("echo -e '[core]\nrepo_version=1\nmode=bare' > "+ostreeRepoConfigFile.getPath());
				}
			}
			//	-bash-4.2# cat /ostree/repo/config
			//	[core]
			//	repo_version=1
			//	mode=bare
			
		}
		Assert.assertTrue(RemoteFileTasks.testExists(client, ostreeRepoConfigFile.getPath()), "Expected ostree config file '"+ostreeRepoConfigFile+"' exists.");
	}
	// TODO might want to implement a teardownOstreeRepoConfigFile @AfterGroups
	
	@BeforeGroups(groups={"setup"}, value={"VerifyOstreeConfigurationsAreSetAfterSubscribing_Test"})
	protected void setupGiWrapperTool() {
		if (clienttasks!=null) {
			if (!clienttasks.isPackageInstalled("ostree")) {	// create a fake /usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py
				// backup gi_wrapper.py
				if (!RemoteFileTasks.testExists(client, giWrapperFile+".bak")) {
					client.runCommandAndWait("cp -n "+giWrapperFile+" "+giWrapperFile+".bak");
				}
				// create a fake gi_wrapper.py tool that simply prints the path to an ostree origin file
				client.runCommandAndWait("echo -e '#!/usr/bin/python\n# Print the path to the current deployed FAKE OSTree origin file.\nprint \""+ostreeOriginFile+"\"' > "+giWrapperFile);
				
			}
		}
		
		// get the real location of the ostreeOriginFile and save it
		
		// -bash-4.2# python /usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py --deployed-origin
		// /ostree/deploy/rhel-atomic-host/deploy/7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1.0.origin
		SSHCommandResult gi_wrapperResult = client.runCommandAndWait("python /usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py --deployed-origin");
		Assert.assertEquals(gi_wrapperResult.getExitCode(), new Integer(0),"Exit Code from running gi_wrapper.py");
		Assert.assertEquals(gi_wrapperResult.getStderr(),"","Stderr from running gi_wrapper.py");
		Assert.assertTrue(!gi_wrapperResult.getStdout().trim().isEmpty(),"Stdout from running gi_wrapper.py is not empty");
		
		// Path to the current deployed OSTree origin file.
		ostreeOriginFile = new File(gi_wrapperResult.getStdout().trim());
	}
	// TODO might want to implement a setupGiWrapperTool @AfterGroups
			
	@BeforeGroups(groups={"setup"}, value={"VerifyOstreeConfigurationsAreSetAfterSubscribing_Test"}, dependsOnMethods={"setupGiWrapperTool"})
	protected void setupOstreeOriginFile() {
		if (clienttasks!=null) {
			if (!clienttasks.isPackageInstalled("ostree")) {
				if (!RemoteFileTasks.testExists(client, ostreeOriginFile.getPath())) {
					client.runCommandAndWait("mkdir -p "+ostreeOriginFile.getParent());
					client.runCommandAndWait("echo -e '[origin]\nrefspec=fake-rhel-atomic-preview-ostree:fake-rhel-atomic-host/7/x86_64/standard' > "+ostreeOriginFile);

				}
			}
			//	-bash-4.2# cat /ostree/deploy/rhel-atomic-host/deploy/7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1.0.origin
			//	[origin]
			//	refspec=rhel-atomic-preview-ostree:rhel-atomic-host/7/x86_64/standard
			
		}
		// assert the ostreeOriginFile exists
		Assert.assertTrue(RemoteFileTasks.testExists(client, ostreeOriginFile.getPath()), "Current deployed OSTree origin file '"+ostreeOriginFile+"' exists.");

	}

	
	
	
	
	
	
	
	
	
	
	// Candidates for an automated Test:
	
	
	
	// Configuration methods ***********************************************************************
	@BeforeClass(groups={"setup"})
	public void checkPackageVersionBeforeClass() {
		if (clienttasks!=null) {
			if (!clienttasks.isPackageInstalled("subscription-manager-plugin-ostree")) {
				throw new SkipException("Subscription Management compatibility with ostree requires subscription-manager-plugin-ostree.");
			}
		}
	}
	
	
	// Protected methods ***********************************************************************
	protected final File ostreeRepoConfigFile = new File("/ostree/repo/config");
	protected final File giWrapperFile = new File("/usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py");	// provided by subscription-manager-plugin-ostree
	protected File ostreeOriginFile = new File("/ostree/deploy/rhel-atomic-host/deploy/FAKE-HASH.0.origin");
//	protected final String entitlementHostDir = "/etc/pki/entitlement-host";
	
	
	
	// Data Providers ***********************************************************************
	@DataProvider(name="getOstreeSubscriptions")
	public Object[][] getOstreeSubscriptionsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getOstreeSubscriptionsDataAsListOfLists());
	}
	protected List<List<Object>> getOstreeSubscriptionsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;

		// register the host
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null, null, true, false, null, null, null);
		
		// attach each available pool in search of ones that provide content of type="ostree"

		/*debugTesting*/for (SubscriptionPool subscriptionPool : clienttasks.getCurrentlyAvailableSubscriptionPools("37091", sm_serverUrl)) {
		//for (SubscriptionPool subscriptionPool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			File serialPemFile = clienttasks.subscribeToSubscriptionPool_(subscriptionPool);
			EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(serialPemFile);
			if (ContentNamespace.findFirstInstanceWithMatchingFieldFromList("type", "ostree", entitlementCert.contentNamespaces)!=null) {
				ll.add(Arrays.asList(new Object[]{null, subscriptionPool}));
			}
		}
		
		// remove all entitlements
		clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null);
		
		return ll;
	}
	
	
	
	
	protected List<OstreeRepo> getCurrentlyConfiguredOstreeRepos() {
				
		return OstreeRepo.parse(client.runCommandAndWait("cat "+ostreeRepoConfigFile).getStdout());
	}
	

}
