package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.OstreeRepo;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

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
 
7. Does some name matching to see if content is the right one for currently running system.
   Currently, thats matching content label to 'b' from orgin and any existing 'remote' in /ostree/repo/config
 
 *
 *  8. /ostree/repo/config is updated with new "remotes" that map to each content of type 'ostree'
 *     (current cases should be 1 ostree content, but could change)
 
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
			groups={"subscribeAndUnsubscribeTests","AcceptanceTests"},
			dataProvider="getOstreeSubscriptionPools",
			priority=10,
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyOstreeConfigurationsAreSetAfterSubscribing_Test(Object bugzilla, SubscriptionPool osTreeSubscriptionPool) {
		String baseurl = clienttasks.getConfParameter("baseurl");
		String repo_ca_cert = clienttasks.getConfParameter("repo_ca_cert");
		
		// get a list of the ostree repos from the ostree repo config file before attaching an ostree subscription
		List<OstreeRepo> ostreeReposBefore = getCurrentlyConfiguredOstreeRepos();
		
		// get the ostree origin refspec before attaching an ostree subscription
		String ostreeOriginRefspecBefore = clienttasks.getConfFileParameter(ostreeOriginFile.getPath(),"origin","refspec");
		
		// attach the subscription that provides ostree content
		File file = clienttasks.subscribeToSubscriptionPool(osTreeSubscriptionPool, sm_clientUsername, sm_clientPassword, sm_serverUrl);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(file);
		
		// get a list of the ostree repos from the ostree repo config file after attaching an ostree subscription
		List<OstreeRepo> ostreeReposAfter = getCurrentlyConfiguredOstreeRepos();
		
		// get the ostree origin refspec after attaching an ostree subscription
		String ostreeOriginRefspecAfter = clienttasks.getConfFileParameter(ostreeOriginFile.getPath(),"origin","refspec");
		
		// assert that ostree repos have been added for each ostree content namespace
		List<ContentNamespace> osTreeContentNamespaces = ContentNamespace.findAllInstancesWithMatchingFieldFromList("type", "ostree", entitlementCert.contentNamespaces);
		for (ContentNamespace osTreeContentNamespace : osTreeContentNamespaces) {
			
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
			
			OstreeRepo ostreeRepo = OstreeRepo.findFirstInstanceWithMatchingFieldFromList("remote", osTreeContentNamespace.label, ostreeReposAfter);
			Assert.assertNotNull(ostreeRepo, "Found an OSTree repo configuration in '"+ostreeRepoConfigFile+"' after attaching subscription '"+osTreeSubscriptionPool.subscriptionName+"' originating from entitlement content: "+osTreeContentNamespace);
			Assert.assertEquals(ostreeRepo.url, baseurl+osTreeContentNamespace.downloadUrl, "OSTree repo remote '"+ostreeRepo.remote+"' config for url. (maps to content downloadUrl)");
			Assert.assertEquals(ostreeRepo.gpg_verify, Boolean.valueOf(!osTreeContentNamespace.gpgKeyUrl.replaceFirst("https?://","").trim().isEmpty()), "OSTree repo remote '"+ostreeRepo.remote+"' config for gpg-verify. (maps to TRUE when content contains gpgKeyUrl)");
			Assert.assertEquals(ostreeRepo.tls_client_cert_path, entitlementCert.file.getPath(), "OSTree repo remote '"+ostreeRepo.remote+"' config for tls-client-cert-path. (maps to path of the entitlement cert)");
			Assert.assertEquals(ostreeRepo.tls_client_key_path, clienttasks.getEntitlementCertKeyFileFromEntitlementCert(entitlementCert).getPath(), "OSTree repo remote '"+ostreeRepo.remote+"' config for tls-client-key-path. (maps to path of the entitlement cert key)");
			Assert.assertEquals(ostreeRepo.tls_ca_path, repo_ca_cert, "OSTree repo remote '"+ostreeRepo.remote+"' config for tls-ca-path. (maps to path of the candlepin CA cert)");
		}
		
		// assert that other ostree repos remain configured
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1152734
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1152734"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping the assertion that other remotes in '"+ostreeRepoConfigFile+"' remain unchanged when attaching an atomic subscription.");
		} else
		// END OF WORKAROUND
		for (OstreeRepo ostreeRepoBefore : ostreeReposBefore) {
			if (ContentNamespace.findFirstInstanceWithMatchingFieldFromList("label", ostreeRepoBefore.remote, osTreeContentNamespaces)==null) {
				OstreeRepo ostreeRepoAfter = OstreeRepo.findFirstInstanceWithMatchingFieldFromList("remote", ostreeRepoBefore.remote, ostreeReposAfter);
				Assert.assertNotNull(ostreeRepoAfter, "OSTree repo configuration in '"+ostreeRepoConfigFile+"' remote '"+ostreeRepoBefore.remote+"' remains configured after attaching subscription '"+osTreeSubscriptionPool.subscriptionName+"' (because it was not among the ostree content sets).");
				Assert.assertEquals(ostreeRepoAfter.url, ostreeRepoBefore.url,"Remote '"+ostreeRepoBefore.remote+"' url");
				Assert.assertEquals(ostreeRepoAfter.gpg_verify, ostreeRepoBefore.gpg_verify,"Remote '"+ostreeRepoBefore.remote+"' gpg-verify");
				Assert.assertEquals(ostreeRepoAfter.tls_client_cert_path, ostreeRepoBefore.tls_client_cert_path,"Remote '"+ostreeRepoBefore.remote+"' tls-client-cert-path");
				Assert.assertEquals(ostreeRepoAfter.tls_client_key_path, ostreeRepoBefore.tls_client_key_path,"Remote '"+ostreeRepoBefore.remote+"' tls-client-key-path");
				Assert.assertEquals(ostreeRepoAfter.tls_ca_path, ostreeRepoBefore.tls_ca_path,"Remote '"+ostreeRepoBefore.remote+"' tls-ca-path");
			}
		}
		
		// throw a failure on the subscription if there is more than one ostree repo
		// if there is more than one ostree repo, then it is undefined which repo remote to set in the ostree origin file
		Assert.assertEquals(osTreeContentNamespaces.size(), 1, "The number of ostree content sets provided by atomic subscription '"+osTreeSubscriptionPool.subscriptionName+"'.  (Greater than 1 is undefined)");
		
		// assert that the ostree origin file has been updated to the newly entitled ostree repo remote
		//	-bash-4.2# python /usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py --deployed-origin
		//	/ostree/deploy/rhel-atomic-host/deploy/7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1.0.origin
		//	-bash-4.2# cat /ostree/deploy/rhel-atomic-host/deploy/7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1.0.origin
		//	[origin]
		//	refspec=rhel-atomic-preview-ostree:rhel-atomic-host/7/x86_64/standard
		Assert.assertEquals(ostreeOriginRefspecAfter.split(":")[1], ostreeOriginRefspecBefore.split(":")[1],"The remote path portion of the refspec in the ostree origin file '"+ostreeOriginFile+"' should remain unchanged after attaching atomic subscription '"+osTreeSubscriptionPool.subscriptionName+"'.");
		Assert.assertEquals(ostreeOriginRefspecAfter.split(":")[0], osTreeContentNamespaces.get(0).label,"The remote label portion of the refspec in the ostree origin file '"+ostreeOriginFile+"' should be updated to the newly entitled ostree content label after attaching atomic subscription '"+osTreeSubscriptionPool.subscriptionName+"'.");
		
		// TODO run some ostree commands to assert that ostree is happy with the new remote
		
/*		

WITHOUT ATTACHING AN ATOMIC SUBSCRIPTION
-bash-4.2# rpm-ostree status
  TIMESTAMP (UTC)         ID             OSNAME               REFSPEC                                                 
* 2014-10-08 00:11:04     d9ec78161b     rhel-atomic-host     rhel-atomic-host:rhel-atomic-host/7/x86_64/standard     
-bash-4.2# ostree admin status
* rhel-atomic-host d9ec78161bc5a6a571337cdfc4ce807e974e4536a4d4e796ec27f602ea9fc8da.0
    origin refspec: rhel-atomic-host:rhel-atomic-host/7/x86_64/standard
-bash-4.2# 


-bash-4.2# cat /ostree/repo/config 
[core]
repo_version=1
mode=bare

[remote "rhel-atomic-host"]
url=file:///install/ostree
gpg-verify=false
-bash-4.2# ostree admin upgrade


error: Error opening file: No such file or directory
-bash-4.2# echo $?
1
-bash-4.2# 


AFTER ATTACHING AN ATOMIC SUBSCRIPTION



 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
		-bash-4.2# rpm-ostree status
		  TIMESTAMP (UTC)         ID             OSNAME               REFSPEC                                                           
		* 2014-09-23 13:37:34     7ea291ddce     rhel-atomic-host     rhel-atomic-preview-ostree:rhel-atomic-host/7/x86_64/standard     



		
		-bash-4.2# rpm-ostree upgrade
		Updating from: rhel-atomic-preview-ostree:rhel-atomic-host/7/x86_64/standard

		850 metadata, 3623 content objects fetched; 94159 KiB transferred in 827 seconds
		error: Upgrade target revision 'ae072611b137b6cb3b3fc2e77225c58ff7e8328b2eaf2d287c362602b2f9b898' with timestamp 'Thu 28 Aug 2014 03:47:40 PM UTC' is chronologically older than current revision '7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1' with timestamp 'Tue 23 Sep 2014 01:37:34 PM UTC'; use --allow-downgrade to permit
		-bash-4.2# echo $?
		1
		
				-bash-4.2# rpm-ostree upgrade --check-diff
Updating from: rhel-atomic-preview-ostree:rhel-atomic-host/7/x86_64/standard

Requesting /content/preview/rhel/atomic/7/x86_64/ostree/repo/refs/heads/rhel-atomic-host/7/x86_64/standard
error: Upgrade target revision 'ae072611b137b6cb3b3fc2e77225c58ff7e8328b2eaf2d287c362602b2f9b898' with timestamp 'Thu 28 Aug 2014 03:47:40 PM UTC' is chronologically older than current revision '7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1' with timestamp 'Tue 23 Sep 2014 01:37:34 PM UTC'; use --allow-downgrade to permit

*/

//MOVED TO SUBSEQUENT TEST
//		// randomly choose to remove the ostree subscription and assert...
//		if (getRandomListItem(Arrays.asList(new Boolean[]{Boolean.TRUE,Boolean.FALSE}))) {
//			clienttasks.unsubscribe(null, clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCert.file), null, null, null);
//			
//			// when removing the entitlement, assert its corresponding ostree repos are removed
//			for (ContentNamespace osTreeContentNamespace : osTreeContentNamespaces) {
//				OstreeRepo ostreeRepo = OstreeRepo.findFirstInstanceWithMatchingFieldFromList("remote", osTreeContentNamespace.label, getCurrentlyConfiguredOstreeRepos());
//				Assert.assertNull(ostreeRepo, "Should no longer find an OSTree repo configuration for remote '"+osTreeContentNamespace.label+"' in '"+ostreeRepoConfigFile+"' after removing subscription '"+osTreeSubscriptionPool.subscriptionName+"'.");
//			}
//			
//			// when removing the entitlement, assert the ostree origin respec remains unchanged
//			Assert.assertEquals(clienttasks.getConfFileParameter(ostreeOriginFile.getPath(),"origin","refspec"), ostreeOriginRefspecAfter, "The OSTree origin refspec in '"+ostreeOriginFile+"' should remain unchanged after removing subscription '"+osTreeSubscriptionPool.subscriptionName+"'.");
//			
//			
//			// when removing the entitlement, assert that other ostree repos remain configured
//			ostreeReposAfter = getCurrentlyConfiguredOstreeRepos();
//			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1152734
//			invokeWorkaroundWhileBugIsOpen = true;
//			bugId="1152734"; 
//			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
//			if (invokeWorkaroundWhileBugIsOpen) {
//				log.warning("Skipping the assertion that other remotes in '"+ostreeRepoConfigFile+"' remain unchanged when removing an atomic subscription.");
//			} else
//			// END OF WORKAROUND
//			for (OstreeRepo ostreeRepoBefore : ostreeReposBefore) {
//				if (ContentNamespace.findFirstInstanceWithMatchingFieldFromList("label", ostreeRepoBefore.remote, osTreeContentNamespaces)==null) {
//					OstreeRepo ostreeRepoAfter = OstreeRepo.findFirstInstanceWithMatchingFieldFromList("remote", ostreeRepoBefore.remote, ostreeReposAfter);
//					Assert.assertNotNull(ostreeRepoAfter, "OSTree repo configuration in '"+ostreeRepoConfigFile+"' remote '"+ostreeRepoBefore.remote+"' remains configured after removing subscription '"+osTreeSubscriptionPool.subscriptionName+"' (because it was not among the ostree content sets).");
//					Assert.assertEquals(ostreeRepoAfter.url, ostreeRepoBefore.url,"Remote '"+ostreeRepoBefore.remote+"' url");
//					Assert.assertEquals(ostreeRepoAfter.gpg_verify, ostreeRepoBefore.gpg_verify,"Remote '"+ostreeRepoBefore.remote+"' gpg-verify");
//					Assert.assertEquals(ostreeRepoAfter.tls_client_cert_path, ostreeRepoBefore.tls_client_cert_path,"Remote '"+ostreeRepoBefore.remote+"' tls-client-cert-path");
//					Assert.assertEquals(ostreeRepoAfter.tls_client_key_path, ostreeRepoBefore.tls_client_key_path,"Remote '"+ostreeRepoBefore.remote+"' tls-client-key-path");
//					Assert.assertEquals(ostreeRepoAfter.tls_ca_path, ostreeRepoBefore.tls_ca_path,"Remote '"+ostreeRepoBefore.remote+"' tls-ca-path");
//				}
//			}
//		}
		
		
/*		
		-bash-4.2# subscription-manager  remove --all
		1 subscription removed at the server.
		1 local certificate has been deleted.
		-bash-4.2# rpm-ostree upgrade --check-diff
		Updating from: rhel-atomic-preview-ostree:rhel-atomic-host/7/x86_64/standard


		error: No remote 'remote "rhel-atomic-preview-ostree"' found in /etc/ostree/remotes.d
		-bash-4.2# ls /etc/ostree/remotes.d/
		-bash-4.2# 
*/
	}
	@DataProvider(name="getOstreeSubscriptionPools")
	public Object[][] getOstreeSubscriptionPoolsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getOstreeSubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getOstreeSubscriptionPoolsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;

		// register the host
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null, null, true, false, null, null, null);
		
		// attach each available pool in search of ones that provide content of type="ostree"
		List <SubscriptionPool> currentlyAvailableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
/*debugTesting*/ if (sm_serverType.equals(CandlepinType.standalone)) currentlyAvailableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools("37091", sm_serverUrl);
		for (SubscriptionPool subscriptionPool : currentlyAvailableSubscriptionPools) {
			File serialPemFile = clienttasks.subscribeToSubscriptionPool_(subscriptionPool);
			EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(serialPemFile);
			if (ContentNamespace.findFirstInstanceWithMatchingFieldFromList("type", "ostree", entitlementCert.contentNamespaces)!=null) {
				
				BlockedByBzBug blockedByBzBug = null;
				
				// Bug 1153366 - SKU RH00004 should not provide more than one Atomic product
				if (subscriptionPool.productId.equals("RH00004")) blockedByBzBug = new BlockedByBzBug("1153366");
				
				// Object bugzilla, SubscriptionPool osTreeSubscriptionPool
				ll.add(Arrays.asList(new Object[]{blockedByBzBug, subscriptionPool}));
			}
		}
		
		// remove all entitlements
		clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null);
		
		return ll;
	}
	
	
	@Test(	description="Verify that the ostree config and origin files are and that when in container mode, attempts to run subscription-manager are blocked",
			groups={"subscribeAndUnsubscribeTests","AcceptanceTests"},
			dataProvider="getOstreeProductSubscriptions",
			priority=20,
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyOstreeConfigurationsAfterUnsubscribing_Test(Object bugzilla, ProductSubscription osTreeProductSubscription) {
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertCorrespondingToProductSubscription(osTreeProductSubscription);
		List<ContentNamespace> osTreeContentNamespaces = ContentNamespace.findAllInstancesWithMatchingFieldFromList("type", "ostree", entitlementCert.contentNamespaces);
		
		// get a list of the ostree repos from the ostree repo config file before removing an ostree subscription
		List<OstreeRepo> ostreeReposBefore = getCurrentlyConfiguredOstreeRepos();
		
		// get the ostree origin refspec before removing an ostree subscription
		String ostreeOriginRefspecBefore = clienttasks.getConfFileParameter(ostreeOriginFile.getPath(),"origin","refspec");

		// remove the ostree subscription and assert...
		clienttasks.unsubscribe(null, clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCert.file), null, null, null);
		
		// get a list of the ostree repos from the ostree repo config file after removing an ostree subscription
		List<OstreeRepo> ostreeReposAfter = getCurrentlyConfiguredOstreeRepos();
		
		// when removing the entitlement, assert its corresponding ostree repos are removed
		for (ContentNamespace osTreeContentNamespace : osTreeContentNamespaces) {
			OstreeRepo ostreeRepo = OstreeRepo.findFirstInstanceWithMatchingFieldFromList("remote", osTreeContentNamespace.label, getCurrentlyConfiguredOstreeRepos());
			Assert.assertNull(ostreeRepo, "After removing subscription '"+osTreeProductSubscription.productName+"', the OSTree repo configuration for remote '"+osTreeContentNamespace.label+"' should be removed from '"+ostreeRepoConfigFile+"'.");
		}
		
		// when removing the entitlement, assert the ostree origin respec remains unchanged
		Assert.assertEquals(clienttasks.getConfFileParameter(ostreeOriginFile.getPath(),"origin","refspec"), ostreeOriginRefspecBefore, "The OSTree origin refspec in '"+ostreeOriginFile+"' should remain unchanged after removing subscription '"+osTreeProductSubscription.productName+"'.");
		
		
		// when removing the entitlement, assert that other ostree repos remain configured
		ostreeReposAfter = getCurrentlyConfiguredOstreeRepos();
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1152734
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1152734"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping the assertion that other remotes in '"+ostreeRepoConfigFile+"' remain unchanged when removing an atomic subscription.");
		} else
		// END OF WORKAROUND
		for (OstreeRepo ostreeRepoBefore : ostreeReposBefore) {
			if (ContentNamespace.findFirstInstanceWithMatchingFieldFromList("label", ostreeRepoBefore.remote, osTreeContentNamespaces)==null) {
				OstreeRepo ostreeRepoAfter = OstreeRepo.findFirstInstanceWithMatchingFieldFromList("remote", ostreeRepoBefore.remote, ostreeReposAfter);
				Assert.assertNotNull(ostreeRepoAfter, "OSTree repo configuration in '"+ostreeRepoConfigFile+"' remote '"+ostreeRepoBefore.remote+"' remains configured after removing subscription '"+osTreeProductSubscription.productName+"' (because it was not among the ostree content sets).");
				Assert.assertEquals(ostreeRepoAfter.url, ostreeRepoBefore.url,"Remote '"+ostreeRepoBefore.remote+"' url");
				Assert.assertEquals(ostreeRepoAfter.gpg_verify, ostreeRepoBefore.gpg_verify,"Remote '"+ostreeRepoBefore.remote+"' gpg-verify");
				Assert.assertEquals(ostreeRepoAfter.tls_client_cert_path, ostreeRepoBefore.tls_client_cert_path,"Remote '"+ostreeRepoBefore.remote+"' tls-client-cert-path");
				Assert.assertEquals(ostreeRepoAfter.tls_client_key_path, ostreeRepoBefore.tls_client_key_path,"Remote '"+ostreeRepoBefore.remote+"' tls-client-key-path");
				Assert.assertEquals(ostreeRepoAfter.tls_ca_path, ostreeRepoBefore.tls_ca_path,"Remote '"+ostreeRepoBefore.remote+"' tls-ca-path");
			}
		}
	}
	@DataProvider(name="getOstreeProductSubscriptions")
	public Object[][] getOstreeProductSubscriptionsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getOstreeProductSubscriptionsDataAsListOfLists());
	}
	protected List<List<Object>> getOstreeProductSubscriptionsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;

		// system should already be registered and consuming
		
		// find consumed entitlements that provide content of type="ostree"
		for (ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			EntitlementCert entitlementCert = clienttasks.getEntitlementCertCorrespondingToProductSubscription(productSubscription);
			if (ContentNamespace.findFirstInstanceWithMatchingFieldFromList("type", "ostree", entitlementCert.contentNamespaces)!=null) {
				// Object bugzilla, ProductSubscription osTreeProductSubscription
				ll.add(Arrays.asList(new Object[]{null, productSubscription}));
			}
		}
		
		return ll;
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
	@BeforeGroups(groups={"setup"}, value={"subscribeAndUnsubscribeTests"})
	protected void setupOstreeRepoConfigFile() {
		if (clienttasks!=null) {
			if (!clienttasks.isPackageInstalled("ostree")) {	// file /ostree/repo/config is not owned by any package	// create a fake /ostree/repo/config
				if (!RemoteFileTasks.testExists(client, ostreeRepoConfigFile.getPath())) {
					client.runCommandAndWait("mkdir -p "+ostreeRepoConfigFile.getParent());
					//client.runCommandAndWait("echo -e '[core]\nrepo_version=1\nmode=bare' > "+ostreeRepoConfigFile.getPath());
					client.runCommandAndWait("echo -e '[core]\nrepo_version=1\nmode=bare\n\n[remote \"REMOTE\"]\nurl=file:///install/ostree\ngpg-verify=false' > "+ostreeRepoConfigFile.getPath());
				}
			}
			//	-bash-4.2# cat /ostree/repo/config
			//	[core]
			//	repo_version=1
			//	mode=bare
			
			// AFTER INSTALLING A NEW ISO...
			//	-bash-4.2# cat /ostree/repo/config 
			//	[core]
			//	repo_version=1
			//	mode=bare
			//
			//	[remote "rhel-atomic-host"]
			//	url=file:///install/ostree
			//	gpg-verify=false

			
		}
		Assert.assertTrue(RemoteFileTasks.testExists(client, ostreeRepoConfigFile.getPath()), "Expected ostree config file '"+ostreeRepoConfigFile+"' exists.");
	}
	// TODO might want to implement a teardownOstreeRepoConfigFile @AfterGroups
	
	@BeforeGroups(groups={"setup"}, value={"subscribeAndUnsubscribeTests"})
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
			
	@BeforeGroups(groups={"setup"}, value={"subscribeAndUnsubscribeTests"}, dependsOnMethods={"setupGiWrapperTool"})
	protected void setupOstreeOriginFile() {
		if (clienttasks!=null) {
			if (!clienttasks.isPackageInstalled("ostree")) {
				if (!RemoteFileTasks.testExists(client, ostreeOriginFile.getPath())) {
					client.runCommandAndWait("mkdir -p "+ostreeOriginFile.getParent());
					client.runCommandAndWait("echo -e '[origin]\nrefspec=REMOTE:OSNAME/7/x86_64/standard' > "+ostreeOriginFile);

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
	protected File ostreeOriginFile = new File("/ostree/deploy/OSNAME/deploy/CHECKSUM.0.origin");
	
	
	
	protected List<OstreeRepo> getCurrentlyConfiguredOstreeRepos() {
				
		return OstreeRepo.parse(client.runCommandAndWait("cat "+ostreeRepoConfigFile).getStdout());
	}
	

}
