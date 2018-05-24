package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.OstreeRepo;
import rhsm.data.ProductCert;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
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
 *  Setting up atomic: host https://mojo.redhat.com/docs/DOC-967002
 *  Quick start guide: http://file.brq.redhat.com/~bexelbie/atomic.html
 *  VERY INFORMATIVE:  http://file.brq.redhat.com/~bexelbie/appinfra-docs/Atomic_OSTree_Get_Started.pdf
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
 *  3a. Ensure an ostree repo config file is installed (file /ostree/repo/config is not owned by any package)
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
 *  3a UPDATE: Bug 1152734 was used to move the /ostree/repo/config contents to /etc/ostree/remotes.d/redhat.conf file
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
 *  4. Attach an atomic subscription (one that provides content of type="ostree")
 *  
 *  5. The subscription-manager content plugin for ostree is invoked when it sees the "ostree" content type
 *  
 *  6. ostree content plugin runs gi_wrapper.py to figure out what ostree TODO 'A' is currently running system is
 *   (running code from the 'ostree' and 'rpm-ostree' rpms to do that)
 *   
 *  7. Does some name matching to see if content is the right one for currently running system.
 *     Currently, thats matching content label to 'b' from origin and any existing 'remote' in /ostree/repo/config  <== Not sure what alikins is referring to here
 *
 *
 *  8. /ostree/repo/config is updated with new "remotes" that map to each content of type 'ostree'
 *     (current cases should be 1 ostree content, but could change - will be changed to use provides/required tags like the yum model)
 *
 *  9. Update the "origin" "b" if needed
 *  9a. If the new content/remote is the first content added to /ostree/repo/config from an ent cert,
 *     then origin 'b' becomes the new remote 'name' (first paid RH product installed gets to be origin)  <== Not sure what alikins is referring to here
 *
 * 10. if on a real ostree system run 'atomic upgrade' (formerly known as 'rpm-ostree')
 *     stuff downloads from cdn to somewhere...
 *     run 'atomic status' to see the ostree boot order
 *
 * 11. reboot
 *  
 *  
 */
@Test(groups={"OstreeTests"})
public class OstreeTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-22238", "RHEL7-51753"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Verify that the ostree config and origin files are set after attaching an atomic subscription; attempt an atomic upgrade; unsubscribe and verify ostree config files are unset",
			groups={"Tier1Tests","subscribeAndUnsubscribeTests"},
			dataProvider="getOstreeSubscriptionPools",
			priority=10,
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testOstreeConfigurationsAreSetAfterSubscribingAndUnsubscribing(Object bugzilla, SubscriptionPool osTreeSubscriptionPool) {
		clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);	// this test is designed to be entitled to one subscription at a time. 
		String baseurl = clienttasks.getConfParameter("baseurl");
		String repo_ca_cert = clienttasks.getConfParameter("repo_ca_cert");
		
		// get a list of the current Product Certs installed on the system
		List<ProductCert> currentProductCerts = clienttasks.getCurrentProductCerts();
		
		// get a list of the ostree repos from the ostree repo config file before attaching an ostree subscription
		List<OstreeRepo> ostreeReposBefore = getCurrentlyConfiguredOstreeRepos(ostreeRepoConfigFile);
		
		// get the ostree origin refspec before attaching an ostree subscription
		// UPDATE: After subscription-manager-plugin-ostree-1.18.5-1 RFE Bug 1378495, the ostree origin refspec file is no longer touched
		String ostreeOriginRefspecBefore = ostreeOriginFile==null?null:clienttasks.getConfFileParameter(ostreeOriginFile.getPath(),"origin","refspec");
		
		// also setup an old /ostree/repo/config file to test the migration clean up scenario described in https://bugzilla.redhat.com/show_bug.cgi?id=1152734#c5 
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.9-1")) {	// post committ 11b377f78dcb06d8dbff5645750791b729e20a0e
			if (!clienttasks.isPackageInstalled("ostree")) {
				client.runCommandAndWait("mkdir -p "+oldOstreeRepoConfigFile.getParent());
				client.runCommandAndWait("echo -e '[core]\nrepo_version=1\nmode=bare\n\n[remote \"REMOTE\"]\nurl=file:///install/ostree\ngpg-verify=false' > "+oldOstreeRepoConfigFile.getPath());
			}
		}
		
		// attach the subscription that provides ostree content
		File file = clienttasks.subscribeToSubscriptionPool(osTreeSubscriptionPool, sm_clientUsername, sm_clientPassword, sm_serverUrl);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(file);
		
		// get a list of the ostree repos from the ostree repo config file after attaching an ostree subscription
		List<OstreeRepo> ostreeReposAfter = getCurrentlyConfiguredOstreeRepos(ostreeRepoConfigFile);
		
		// get the ostree origin refspec after attaching an ostree subscription
		// UPDATE: After subscription-manager-plugin-ostree-1.18.5-1 RFE Bug 1378495, the ostree origin refspec file is no longer touched
		String ostreeOriginRefspecAfter = ostreeOriginFile==null?null:clienttasks.getConfFileParameter(ostreeOriginFile.getPath(),"origin","refspec");
		
		// also assert the clean up of remotes from the old /ostree/repo/config file 
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.9-1")) {	// post committ 11b377f78dcb06d8dbff5645750791b729e20a0e
			if (RemoteFileTasks.testExists(client, oldOstreeRepoConfigFile.getPath())) {
				Assert.assertTrue(getCurrentlyConfiguredOstreeRepos(oldOstreeRepoConfigFile).isEmpty(),"Subscription-manager should have cleaned out the old remotes from '"+oldOstreeRepoConfigFile+"'.");
			}
		}
		
		// assert that ostree repos have been added for each ostree content namespace
		List<ContentNamespace> osTreeContentNamespaces = ContentNamespace.findAllInstancesWithMatchingFieldFromList("type", "ostree", entitlementCert.contentNamespaces);
		List<ContentNamespace> osTreeContentNamespacesMatchingInstalledProducts = new ArrayList<ContentNamespace>();
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
			
			// if the Required Tags osTreeContentNamespace are not provided by an installed product, then the OSTree repo configuration should NOT contain this osTreeContentNamespace are not
			// THIS TAGGING DECISION WAS IMPLEMENTED AS A SOLUTION TO https://bugzilla.redhat.com/show_bug.cgi?id=1153366#c5
			if (clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(osTreeContentNamespace, currentProductCerts)) {
				OstreeRepo ostreeRepo = OstreeRepo.findFirstInstanceWithMatchingFieldFromList("remote", osTreeContentNamespace.label, ostreeReposAfter);
				Assert.assertNotNull(ostreeRepo, "Found an OSTree repo configuration in '"+ostreeRepoConfigFile+"' after attaching subscription '"+osTreeSubscriptionPool.subscriptionName+"' originating from entitlement content: "+osTreeContentNamespace);
				Assert.assertEquals(ostreeRepo.url, baseurl+osTreeContentNamespace.downloadUrl, "OSTree repo remote '"+ostreeRepo.remote+"' config for url. (maps to content downloadUrl)");
				Assert.assertEquals(ostreeRepo.gpg_verify, Boolean.valueOf(!osTreeContentNamespace.gpgKeyUrl.replaceFirst("https?://","").trim().isEmpty()), "OSTree repo remote '"+ostreeRepo.remote+"' config for gpg-verify. (maps to TRUE when content contains gpgKeyUrl)");
				Assert.assertEquals(ostreeRepo.tls_client_cert_path, entitlementCert.file.getPath(), "OSTree repo remote '"+ostreeRepo.remote+"' config for tls-client-cert-path. (maps to path of the entitlement cert)");
				Assert.assertEquals(ostreeRepo.tls_client_key_path, clienttasks.getEntitlementCertKeyFileFromEntitlementCert(entitlementCert).getPath(), "OSTree repo remote '"+ostreeRepo.remote+"' config for tls-client-key-path. (maps to path of the entitlement cert key)");
				Assert.assertEquals(ostreeRepo.tls_ca_path, repo_ca_cert, "OSTree repo remote '"+ostreeRepo.remote+"' config for tls-ca-path. (maps to path of the candlepin CA cert)");
				
				if (!osTreeContentNamespacesMatchingInstalledProducts.contains(osTreeContentNamespace)) osTreeContentNamespacesMatchingInstalledProducts.add(osTreeContentNamespace);
			} else {
				OstreeRepo ostreeRepo = OstreeRepo.findFirstInstanceWithMatchingFieldFromList("remote", osTreeContentNamespace.label, ostreeReposAfter);
				Assert.assertNull(ostreeRepo, "Should NOT find an OSTree repo configuration for remote '"+osTreeContentNamespace.label+"' in '"+ostreeRepoConfigFile+"' after attaching subscription '"+osTreeSubscriptionPool.subscriptionName+"' because the Required Tags '"+osTreeContentNamespace.requiredTags+"' from the entitlement content are not found among the current product certs installed.");
			}
		}
		
		// assert that other ostree repos remain configured
		// TEMPORARY WORKAROUND FOR BUG: 1152734 - Update subman ostree content plugin to use ostree cli for manipulating 'remote' configs
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1152734"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
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
		
/* replacing this with assertion that there is only osTreeContentNamespaceToTest
		// throw a failure on the subscription if there is more than one ostree repo
		// if there is more than one ostree repo, then it is undefined which repo remote to set in the ostree origin file
		Assert.assertEquals(osTreeContentNamespaces.size(), 1, "The number of ostree content sets provided by atomic subscription '"+osTreeSubscriptionPool.subscriptionName+"'.  (Greater than 1 is undefined)");
*/
		// assert that there is only one entitled osTreeContentNamespaceMatchingInstalledProducts
		// TEMPORARY WORKAROUND FOR BUG: 1160771 - Missing provides/requires tags in ostree content sets (not product cert) 
		invokeWorkaroundWhileBugIsOpen = true;
		invokeWorkaroundWhileBugIsOpen = false; // Status: 	MODIFIED 
		bugId="1160771"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			if (osTreeContentNamespacesMatchingInstalledProducts.size()>1) log.warning("Found more than one entitled ostree ContentNamespace whose RequiredTags match the installed productCerts.  Blocked by Jira request https://projects.engineering.redhat.com/browse/APPINFRAT-246");
			log.warning("Skipping the assertion for more than one entitled ostree ContentNamespace whose RequiredTags match the installed productCerts due to open bug '"+bugId+"'.");
		}
		// END OF WORKAROUND
		if (clienttasks.isPackageVersion("subscription-manager-plugin-ostree", "<", "1.13.9-1")) {	// commit 11b377f78dcb06d8dbff5645750791b729e20a0e
			// Bug 1152734 - Update subman ostree content plugin to use ostree cli for manipulating 'remote' configs
			Assert.fail("This version of subscription-manager-plugin-ostree is blocked by bug 1152734.");
		}
		ContentNamespace osTreeContentNamespaceMatchingInstalledProducts = null;
		if (osTreeContentNamespacesMatchingInstalledProducts.isEmpty()) {
			log.warning("This is probably NOT an atomic system.");
			if (ostreeOriginRefspecBefore!=null) Assert.assertEquals(ostreeOriginRefspecAfter, ostreeOriginRefspecBefore, "When there are no installed products whose tags match the ostree ContentNamespace tags, then the ostree origin refspec in file '"+ostreeOriginFile+"' should remain unchanged after attaching subscription '"+osTreeSubscriptionPool.subscriptionName+"'.");
		} else {
			Assert.assertEquals(osTreeContentNamespacesMatchingInstalledProducts.size(), 1, "At most there should only be one entitled ostree ContentNamespace that matches the installed product certs.");
			osTreeContentNamespaceMatchingInstalledProducts = osTreeContentNamespacesMatchingInstalledProducts.get(0);
			
			// assert that the ostree origin file has been updated to the newly entitled ostree repo remote
			//	-bash-4.2# python /usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py --deployed-origin
			//	/ostree/deploy/rhel-atomic-host/deploy/7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1.0.origin
			//	-bash-4.2# cat /ostree/deploy/rhel-atomic-host/deploy/7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1.0.origin
			//	[origin]
			//	refspec=rhel-atomic-preview-ostree:rhel-atomic-host/7/x86_64/standard
			// UPDATE: After subscription-manager-plugin-ostree-1.18.5-1 RFE Bug 1378495, the ostree origin refspec file is no longer touched
			if (ostreeOriginRefspecAfter!=null) Assert.assertEquals(ostreeOriginRefspecAfter.split(":")[1], ostreeOriginRefspecBefore.split(":")[1],"The remote path portion of the refspec in the ostree origin file '"+ostreeOriginFile+"' should remain unchanged after attaching atomic subscription '"+osTreeSubscriptionPool.subscriptionName+"'.");
			if (ostreeOriginRefspecAfter!=null) Assert.assertEquals(ostreeOriginRefspecAfter.split(":")[0], osTreeContentNamespaceMatchingInstalledProducts.label,"The remote label portion of the refspec in the ostree origin file '"+ostreeOriginFile+"' should be updated to the newly entitled ostree content label after attaching atomic subscription '"+osTreeSubscriptionPool.subscriptionName+"'.");
		}
		
		// attempt to do an atomic upgrade
		String pkg = "rpm-ostree-client";
		if (!clienttasks.isPackageInstalled(pkg)) {log.warning("Skipping assertion attempt to do an atomic upgrade after attaching the atomic subscription since package '"+pkg+"' is not installed.");} else {
			SSHCommandResult atomicUpgradeResultAfterSubscribe = client.runCommandAndWait("atomic upgrade");
			//	-bash-4.2# atomic upgrade
			//	Updating from: rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard
			//
			//	Requesting /content/beta/rhel/atomic/7/x86_64/ostree/repo/refs/heads/rhel-atomic-host/7/x86_64/standard
			//	Copying /etc changes: 13 modified, 4 removed, 42 added
			//	Transaction complete; bootconfig swap: no deployment count change: 0
			//	Changed:
			//	  docker-1.2.0-1.8.el7.x86_64
			//	  kubernetes-0.4+-0.9.git8e1d416.el7.x86_64
			//	  tzdata-2014i-1.el7.noarch
			//	Updates prepared for next boot; run "systemctl reboot" to start a reboot
			//	-bash-4.2# 
			String stdoutStartsWith = "Updating from: "+ostreeOriginRefspecBefore;
			String stdoutEndsWith = "Updates prepared for next boot; run \"systemctl reboot\" to start a reboot";
			Assert.assertEquals(atomicUpgradeResultAfterSubscribe.getExitCode(), new Integer(0) ,"Exitcode after attempting to do an atomic upgrade after attaching an atomic subscription.");
			Assert.assertTrue(atomicUpgradeResultAfterSubscribe.getStdout().trim().startsWith(stdoutStartsWith), "Stdout starts with '"+stdoutStartsWith+"' after attempting to do an atomic upgrade after attaching an atomic subscription.");
			Assert.assertTrue(atomicUpgradeResultAfterSubscribe.getStdout().trim().endsWith(stdoutEndsWith), "Stdout ends with '"+stdoutEndsWith+"' after attempting to do an atomic upgrade after attaching an atomic subscription.");
			Assert.assertEquals(atomicUpgradeResultAfterSubscribe.getStderr().trim(), "" ,"Exitcode after attempting to do an atomic upgrade after attaching an atomic subscription.");
			
			//	-bash-4.2# atomic status
			//	  VERSION   ID             OSNAME               REFSPEC                                                             
			//	  7.0.4     0fc676bdec     rhel-atomic-host     rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard     
			//	* 7.0.2     9a0dbc159e     rhel-atomic-host     rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard     
			//	-bash-4.2# 
			//	This sample output shows that rhel-atomic-host 0fc676bdec... will be booted into on the next restart. The version to be booted on
			//	the next restart is printed first.
			//	This sample also shows that rhel-atomic-host 9a0dbc159e... is the currently running version. The currently running version is
			//	marked with an asterisk (*). This output was created just after the atomic upgrade command was executed, therefore a new
			//	version has been staged to be applied at the next restart.
			SSHCommandResult atomicStatusResultAfterSubscribe = client.runCommandAndWait("atomic status | grep -v VERSION");
			Assert.assertTrue(!atomicStatusResultAfterSubscribe.getStdout().trim().split("\n")[0].startsWith("*"), "Stdout from atomic status after doing an atomic upgrade indicates that the upgraded version is listed first and will be booted on the next systemctl reboot.");
			Assert.assertTrue(atomicStatusResultAfterSubscribe.getStdout().trim().split("\n")[1].startsWith("*"), "Stdout from atomic status after doing an atomic upgrade indicates that the currently running version (listed second) is marked with an asterisk (*)");

		}

//MOVED TO SUBSEQUENT TEST
//		// randomly choose to remove the ostree subscription and assert...
//		if (getRandomBoolean()) {
//MOVED BACK
			clienttasks.unsubscribe(null, clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCert.file), null, null, null, null, null);
			
			// after removing the entitlement, assert its corresponding ostree repos are removed
			ostreeReposAfter = getCurrentlyConfiguredOstreeRepos(ostreeRepoConfigFile);
			for (ContentNamespace osTreeContentNamespace : osTreeContentNamespaces) {
				OstreeRepo ostreeRepo = OstreeRepo.findFirstInstanceWithMatchingFieldFromList("remote", osTreeContentNamespace.label, ostreeReposAfter);
				Assert.assertNull(ostreeRepo, "Should no longer find an OSTree repo configuration for remote '"+osTreeContentNamespace.label+"' in '"+ostreeRepoConfigFile+"' after removing subscription '"+osTreeSubscriptionPool.subscriptionName+"'.");
			}
			
			// when removing the entitlement, assert the ostree origin refspec remains unchanged
			// TODO: This assertion may be changed by Bug 1193208 - 'atomic host upgrade' gives incorrect error after unregistering with subscription-manager
			if (ostreeOriginFile!=null) Assert.assertEquals(clienttasks.getConfFileParameter(ostreeOriginFile.getPath(),"origin","refspec"), ostreeOriginRefspecAfter, "The OSTree origin refspec in '"+ostreeOriginFile+"' should remain unchanged after removing subscription '"+osTreeSubscriptionPool.subscriptionName+"'.");
			
			
			// when removing the entitlement, assert that other ostree repos remain configured
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1152734
			invokeWorkaroundWhileBugIsOpen = true;
			bugId="1152734"; 
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Skipping the assertion that other remotes in '"+ostreeRepoConfigFile+"' remain unchanged when removing an atomic subscription.");
			} else
			// END OF WORKAROUND
			for (OstreeRepo ostreeRepoBefore : ostreeReposBefore) {
				if (ContentNamespace.findFirstInstanceWithMatchingFieldFromList("label", ostreeRepoBefore.remote, osTreeContentNamespaces)==null) {
					OstreeRepo ostreeRepoAfter = OstreeRepo.findFirstInstanceWithMatchingFieldFromList("remote", ostreeRepoBefore.remote, ostreeReposAfter);
					Assert.assertNotNull(ostreeRepoAfter, "OSTree repo configuration in '"+ostreeRepoConfigFile+"' remote '"+ostreeRepoBefore.remote+"' remains configured after removing subscription '"+osTreeSubscriptionPool.subscriptionName+"' (because it was not among the ostree content sets).");
					Assert.assertEquals(ostreeRepoAfter.url, ostreeRepoBefore.url,"Remote '"+ostreeRepoBefore.remote+"' url");
					Assert.assertEquals(ostreeRepoAfter.gpg_verify, ostreeRepoBefore.gpg_verify,"Remote '"+ostreeRepoBefore.remote+"' gpg-verify");
					Assert.assertEquals(ostreeRepoAfter.tls_client_cert_path, ostreeRepoBefore.tls_client_cert_path,"Remote '"+ostreeRepoBefore.remote+"' tls-client-cert-path");
					Assert.assertEquals(ostreeRepoAfter.tls_client_key_path, ostreeRepoBefore.tls_client_key_path,"Remote '"+ostreeRepoBefore.remote+"' tls-client-key-path");
					Assert.assertEquals(ostreeRepoAfter.tls_ca_path, ostreeRepoBefore.tls_ca_path,"Remote '"+ostreeRepoBefore.remote+"' tls-ca-path");
				}
			}
//		}
//MOVED BACK
		
		
		// attempt to run the atomic upgrade without an entitlement
		// /usr/bin/atomic is provided by rpm-ostree-client-2014.109-2.atomic.el7.x86_64
		pkg = "rpm-ostree-client";
		if (!clienttasks.isPackageInstalled(pkg)) {log.warning("Skipping assertion attempt to do an atomic upgrade after removing the atomic subscription since package '"+pkg+"' is not installed.");} else {
			SSHCommandResult atomicUpgradeResultAfterUnsubscribe = client.runCommandAndWait("atomic upgrade");
			//	-bash-4.2# atomic upgrade
			//	Updating from: rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard
			//
			//
			//	error: No remote 'remote "rhel-atomic-host-beta-ostree"' found in /etc/ostree/remotes.d
			//	-bash-4.2# 
			String stdoutStartsWith = "Updating from: "+ostreeOriginRefspecAfter;	// TODO: need to be run on Atomic and updated to make a better assert after the changes from subscription-manager-1.18.5-1 Bug 1378495 - [RFE] Do not change OStree origin refspec
			Assert.assertEquals(atomicUpgradeResultAfterUnsubscribe.getExitCode(), new Integer(1) ,"Exitcode after attempting to do an atomic upgrade after removing the atomic subscription.");
			Assert.assertTrue(atomicUpgradeResultAfterUnsubscribe.getStdout().trim().startsWith(stdoutStartsWith), "Stdout starts with '"+stdoutStartsWith+"' after attempting to do an atomic upgrade after removing the atomic subscription.");
			Assert.assertEquals(atomicUpgradeResultAfterUnsubscribe.getStderr().trim(), "error: No remote 'remote \""+osTreeContentNamespaceMatchingInstalledProducts.label+"\"' found in /etc/ostree/remotes.d" ,"Exitcode after attempting to do an atomic upgrade after removing the atomic subscription.");
		}
		

	}
	@DataProvider(name="getOstreeSubscriptionPools")
	public Object[][] getOstreeSubscriptionPoolsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getOstreeSubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getOstreeSubscriptionPoolsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (!checkPackageVersionBeforeClass()) return ll;
		
		// disable /etc/rhsm/pluginconf.d/ostree_content.OstreeContentPlugin.conf while this dataProvider runs
		clienttasks.updateConfFileParameter(ostreeContentPluginFile.getPath(), "enabled", "0");
		
		// register the host
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,false,null,null,(String)null,null,null, null, true, false, null, null, null, null);
		
		// attach each available pool in search of ones that provide content of type="ostree"
		List <SubscriptionPool> currentlyAvailableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
/*debugTesting*/ if (sm_serverType.equals(CandlepinType.standalone)) currentlyAvailableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools("37091", sm_serverUrl);	// 37091 Product Name:   Awesome OS OStree Bits
///*debugTesting DO NOT COMMIT THIS LINE UNCOMMENTED */ if (sm_serverType.equals(CandlepinType.hosted)) currentlyAvailableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools("272", sm_serverUrl);	// 272 Product Name:   Red Hat Enterprise Linux Atomic Host Beta
		for (SubscriptionPool subscriptionPool : currentlyAvailableSubscriptionPools) {
			File serialPemFile = clienttasks.subscribeToSubscriptionPool_(subscriptionPool);
			EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(serialPemFile);
			if (ContentNamespace.findFirstInstanceWithMatchingFieldFromList("type", "ostree", entitlementCert.contentNamespaces)!=null) {
				
				BlockedByBzBug blockedByBzBug = null;
				
				// Bug 1153366 - SKU RH00004 should not provide more than one Atomic product
				if (subscriptionPool.productId.equals("RH00004")) blockedByBzBug = new BlockedByBzBug("1153366");
				if (subscriptionPool.productId.equals("RH00003")) blockedByBzBug = new BlockedByBzBug("1153366");
				
				// Object bugzilla, SubscriptionPool osTreeSubscriptionPool
				ll.add(Arrays.asList(new Object[]{blockedByBzBug, subscriptionPool}));
			}
		}
		
		// remove all entitlements
		clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
		
		// re-register the host to reduce the chance of a RateLimitExceededException
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,false,null,null,(String)null,null,null, null, true, false, null, null, null, null);
		
		// re-enable /etc/rhsm/pluginconf.d/ostree_content.OstreeContentPlugin.conf
		clienttasks.updateConfFileParameter(ostreeContentPluginFile.getPath(), "enabled", "1");
		
		return ll;
	}
	
	
//	@Test(	description="Verify that the ostree config and origin files are and that when in container mode, attempts to run subscription-manager are blocked",
//			groups={"Tier1Tests","subscribeAndUnsubscribeTests"},
//			dataProvider="getOstreeProductSubscriptions",
//			priority=20,
//			enabled=true)
//	//@ImplementsNitrateTest(caseId=)
//	public void VerifyOstreeConfigurationsAfterUnsubscribing_Test(Object bugzilla, ProductSubscription osTreeProductSubscription) {
//		EntitlementCert entitlementCert = clienttasks.getEntitlementCertCorrespondingToProductSubscription(osTreeProductSubscription);
//		List<ContentNamespace> osTreeContentNamespaces = ContentNamespace.findAllInstancesWithMatchingFieldFromList("type", "ostree", entitlementCert.contentNamespaces);
//		
//		// get a list of the ostree repos from the ostree repo config file before removing an ostree subscription
//		List<OstreeRepo> ostreeReposBefore = getCurrentlyConfiguredOstreeRepos();
//		
//		// get the ostree origin refspec before removing an ostree subscription
//		String ostreeOriginRefspecBefore = clienttasks.getConfFileParameter(ostreeOriginFile.getPath(),"origin","refspec");
//
//		// remove the ostree subscription and assert...
//		clienttasks.unsubscribe(null, clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCert.file), null, null, null);
//		
//		// get a list of the ostree repos from the ostree repo config file after removing an ostree subscription
//		List<OstreeRepo> ostreeReposAfter = getCurrentlyConfiguredOstreeRepos();
//		
//		// when removing the entitlement, assert its corresponding ostree repos are removed
//		for (ContentNamespace osTreeContentNamespace : osTreeContentNamespaces) {
//			OstreeRepo ostreeRepo = OstreeRepo.findFirstInstanceWithMatchingFieldFromList("remote", osTreeContentNamespace.label, getCurrentlyConfiguredOstreeRepos());
//			Assert.assertNull(ostreeRepo, "After removing subscription '"+osTreeProductSubscription.productName+"', the OSTree repo configuration for remote '"+osTreeContentNamespace.label+"' should be removed from '"+ostreeRepoConfigFile+"'.");
//		}
//		
//		// when removing the entitlement, assert the ostree origin respec remains unchanged
//		Assert.assertEquals(clienttasks.getConfFileParameter(ostreeOriginFile.getPath(),"origin","refspec"), ostreeOriginRefspecBefore, "The OSTree origin refspec in '"+ostreeOriginFile+"' should remain unchanged after removing subscription '"+osTreeProductSubscription.productName+"'.");
//		
//		
//		// when removing the entitlement, assert that other ostree repos remain configured
//		ostreeReposAfter = getCurrentlyConfiguredOstreeRepos();
//		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1152734
//		boolean invokeWorkaroundWhileBugIsOpen = true;
//		String bugId="1152734"; 
//		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
//		if (invokeWorkaroundWhileBugIsOpen) {
//			log.warning("Skipping the assertion that other remotes in '"+ostreeRepoConfigFile+"' remain unchanged when removing an atomic subscription.");
//		} else
//		// END OF WORKAROUND
//		for (OstreeRepo ostreeRepoBefore : ostreeReposBefore) {
//			if (ContentNamespace.findFirstInstanceWithMatchingFieldFromList("label", ostreeRepoBefore.remote, osTreeContentNamespaces)==null) {
//				OstreeRepo ostreeRepoAfter = OstreeRepo.findFirstInstanceWithMatchingFieldFromList("remote", ostreeRepoBefore.remote, ostreeReposAfter);
//				Assert.assertNotNull(ostreeRepoAfter, "OSTree repo configuration in '"+ostreeRepoConfigFile+"' remote '"+ostreeRepoBefore.remote+"' remains configured after removing subscription '"+osTreeProductSubscription.productName+"' (because it was not among the ostree content sets).");
//				Assert.assertEquals(ostreeRepoAfter.url, ostreeRepoBefore.url,"Remote '"+ostreeRepoBefore.remote+"' url");
//				Assert.assertEquals(ostreeRepoAfter.gpg_verify, ostreeRepoBefore.gpg_verify,"Remote '"+ostreeRepoBefore.remote+"' gpg-verify");
//				Assert.assertEquals(ostreeRepoAfter.tls_client_cert_path, ostreeRepoBefore.tls_client_cert_path,"Remote '"+ostreeRepoBefore.remote+"' tls-client-cert-path");
//				Assert.assertEquals(ostreeRepoAfter.tls_client_key_path, ostreeRepoBefore.tls_client_key_path,"Remote '"+ostreeRepoBefore.remote+"' tls-client-key-path");
//				Assert.assertEquals(ostreeRepoAfter.tls_ca_path, ostreeRepoBefore.tls_ca_path,"Remote '"+ostreeRepoBefore.remote+"' tls-ca-path");
//			}
//		}
//	}
//	@DataProvider(name="getOstreeProductSubscriptions")
//	public Object[][] getOstreeProductSubscriptionsDataAs2dArray() throws JSONException, Exception {
//		return TestNGUtils.convertListOfListsTo2dArray(getOstreeProductSubscriptionsDataAsListOfLists());
//	}
//	protected List<List<Object>> getOstreeProductSubscriptionsDataAsListOfLists() throws JSONException, Exception {
//		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
//
//		// system should already be registered and consuming
//		
//		// find consumed entitlements that provide content of type="ostree"
//		for (ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
//			EntitlementCert entitlementCert = clienttasks.getEntitlementCertCorrespondingToProductSubscription(productSubscription);
//			if (ContentNamespace.findFirstInstanceWithMatchingFieldFromList("type", "ostree", entitlementCert.contentNamespaces)!=null) {
//				// Object bugzilla, ProductSubscription osTreeProductSubscription
//				ll.add(Arrays.asList(new Object[]{null, productSubscription}));
//			}
//		}
//		
//		return ll;
//	}
	
	
	@BeforeGroups(groups={"setup"}, value={"subscribeAndUnsubscribeTests"})
	protected void setupOstreeRepoConfigFile() {
		if (clienttasks!=null) {
			if (!clienttasks.isPackageInstalled("ostree")) {	// the ostree repo config is not owned by any package, create a fake one
				if (!RemoteFileTasks.testExists(client, ostreeRepoConfigFile.getPath())) {
					// create the parent directory
					client.runCommandAndWait("mkdir -p "+ostreeRepoConfigFile.getParent());
					
					if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.9-1")) {	// post committ 11b377f78dcb06d8dbff5645750791b729e20a0e
						// no need to create a conf file, subscription-manager-ostree-plugin will create it 
						
						// [root@jsefler-os7 ~]# cat /etc/ostree/remotes.d/redhat.conf
						// cat: /etc/ostree/remotes.d/redhat.conf: No such file or directory
						
					} else {
						//client.runCommandAndWait("echo -e '[core]\nrepo_version=1\nmode=bare' > "+ostreeRepoConfigFile.getPath());
						client.runCommandAndWait("echo -e '[core]\nrepo_version=1\nmode=bare\n\n[remote \"REMOTE\"]\nurl=file:///install/ostree\ngpg-verify=false' > "+ostreeRepoConfigFile.getPath());
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
						
						Assert.assertTrue(RemoteFileTasks.testExists(client, ostreeRepoConfigFile.getPath()), "Expected ostree config file '"+ostreeRepoConfigFile+"' exists.");
					}
				}
			}
		}
	}
	// TODO might want to implement a teardownOstreeRepoConfigFile @AfterGroups
	
	@BeforeGroups(groups={"setup"}, value={"subscribeAndUnsubscribeTests"})
	protected void setupGiWrapperTool() {
		if (clienttasks==null) return;
		
		// determine the path to giWrapperFilename (because it changed by commit 655b81b5271cba98143b36aaa33938b0abaf1820 )
		String giWrapperFilename = "gi_wrapper.py";	// provided by subscription-manager-plugin-ostree
		giWrapperFile = new File(client.runCommandAndWait("rpm -ql subscription-manager-plugin-ostree | grep -e "+giWrapperFilename+"$").getStdout().trim());
		
		// UPDATE: As of RFE Bug 1378495, subscription-manager will no longer touch the ostree origin file after attaching a subscription that provides content of type "ostree"
		if (clienttasks.isPackageVersion("subscription-manager-plugin-ostree", ">=", "1.18.5-1")) {	//commit 689a7a8d8c0ee2f59781273e49fa6d9942bea5e9 1378495: Do not touch OSTree Origin files.	// Bug 1378495 - [RFE] Do not change OStree origin refspec
			log.warning("Due to RFE Bug 1378495, this version of subscription-manager-plugin-ostree will no longer make any modifications to the ostree origin file.");
			Assert.assertTrue(giWrapperFile.getName().isEmpty(),"Asserting that subscription-manager-plugin-ostree no longer provides the '"+giWrapperFilename+"' tool formerly used to find the path to the true ostreeOriginFile.");
			giWrapperFile=null;	// was used to find the path to the true ostreeOriginFile
			return;
		}
		
		// assert that the giWrapperFilename (provided by subscription-manager-plugin-ostree) was determined
		Assert.assertTrue(!giWrapperFile.getPath().isEmpty(),"Determined full path to source code filename '"+giWrapperFilename+"' (determined value '"+giWrapperFile.getPath()+"')");

		if (!clienttasks.isPackageInstalled("ostree")) {	// create a fake /usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py
			// backup gi_wrapper.py
			if (!RemoteFileTasks.testExists(client, giWrapperFile+".bak")) {
				client.runCommandAndWait("cp -n "+giWrapperFile+" "+giWrapperFile+".bak");
			}
			// create a fake gi_wrapper.py tool that simply prints the path to an ostree origin file
			client.runCommandAndWait("echo -e '#!/usr/bin/python\n# Print the path to the current deployed FAKE OSTree origin file.\nprint \""+ostreeOriginFile+"\"' > "+giWrapperFile);
			
		}
		
		// get the real location of the ostreeOriginFile and save it
		
		// -bash-4.2# python /usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py --deployed-origin
		// /ostree/deploy/rhel-atomic-host/deploy/7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1.0.origin
		SSHCommandResult gi_wrapperResult = client.runCommandAndWait("python "+giWrapperFile+" --deployed-origin");
		Assert.assertEquals(gi_wrapperResult.getExitCode(), new Integer(0),"Exit Code from running gi_wrapper.py");
		Assert.assertEquals(gi_wrapperResult.getStderr(),"","Stderr from running gi_wrapper.py");
		Assert.assertTrue(!gi_wrapperResult.getStdout().trim().isEmpty(),"Stdout from running gi_wrapper.py is not empty");
		
		// Path to the current deployed OSTree origin file.
		ostreeOriginFile = new File(gi_wrapperResult.getStdout().trim());
	}
	// TODO might want to implement a setupGiWrapperTool @AfterGroups
			
	@BeforeGroups(groups={"setup"}, value={"subscribeAndUnsubscribeTests"}, dependsOnMethods={"setupGiWrapperTool"})
	protected void setupOstreeOriginFile() {
		if (clienttasks==null) return;
		
		// UPDATE: As of RFE Bug 1378495, subscription-manager will no longer touch the ostree origin file after attaching a subscription that provides content of type "ostree"
		if (clienttasks.isPackageVersion("subscription-manager-plugin-ostree", ">=", "1.18.5-1")) {	//commit 689a7a8d8c0ee2f59781273e49fa6d9942bea5e9 1378495: Do not touch OSTree Origin files.	// Bug 1378495 - [RFE] Do not change OStree origin refspec
			log.warning("Due to RFE Bug 1378495, this version of subscription-manager-plugin-ostree will no longer make any modifications to the ostree origin file.");
			ostreeOriginFile=null;	// set to null since this will no longer be touched by subscription-manager-plugin-ostree
			return;
		}
		
		// setup a faux ostree origin file to test subscription-manager-plugin-ostree functionality even when ostree is not installed.
		if (!clienttasks.isPackageInstalled("ostree")) {
			if (!RemoteFileTasks.testExists(client, ostreeOriginFile.getPath())) {
				client.runCommandAndWait("mkdir -p "+ostreeOriginFile.getParent());
				client.runCommandAndWait("echo -e '[origin]\nrefspec=REMOTE:OSNAME/7/x86_64/standard' > "+ostreeOriginFile);
			}
		}
		//	-bash-4.2# cat /ostree/deploy/rhel-atomic-host/deploy/7ea291ddcec9e2451616f77808386794a62befb274642e07e932bc4f817dd6a1.0.origin
		//	[origin]
		//	refspec=rhel-atomic-preview-ostree:rhel-atomic-host/7/x86_64/standard
			
		// assert the ostreeOriginFile exists
		Assert.assertTrue(RemoteFileTasks.testExists(client, ostreeOriginFile.getPath()), "Current deployed OSTree origin file '"+ostreeOriginFile+"' exists.");

	}

	
	
	
	
	
	
	
	
	
	
	// Candidates for an automated Test:
	
	
	
	// Configuration methods ***********************************************************************
	@BeforeClass(groups={"setup"})
	public boolean checkPackageVersionBeforeClass() {
		if (clienttasks!=null) {
			// skip test class when subscription-manager-plugin-ostree is not installed
			String pkg = "subscription-manager-plugin-ostree";
			if (!clienttasks.isPackageInstalled(pkg)) {
				//throw new SkipException("Subscription Management compatibility with ostree requires package '"+pkg+"'.");	// this shows up in Jenkins as a failure... TODO figure out why
				log.warning("Subscription Management compatibility with ostree requires package '"+pkg+"'.");
				return false;
			}
			// where is the ostree repo config file located that will be managed by subscription-manager
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.9-1")) {	// post committ 11b377f78dcb06d8dbff5645750791b729e20a0e
				ostreeRepoConfigFile = new File("/etc/ostree/remotes.d/redhat.conf");
			} else {
				ostreeRepoConfigFile = oldOstreeRepoConfigFile;
			}
			return true;
		}
		return false;
	}
	
	
	// Protected methods ***********************************************************************
	protected final File oldOstreeRepoConfigFile = new File("/ostree/repo/config");
	protected File ostreeRepoConfigFile = null;
	protected File giWrapperFile = null;
	protected File ostreeOriginFile = new File("/ostree/deploy/OSNAME/deploy/CHECKSUM.0.origin");	// OSNAME and CHECKSUM are placeholders
	protected final File ostreeContentPluginFile = new File("/etc/rhsm/pluginconf.d/ostree_content.OstreeContentPlugin.conf");
	
	
	protected List<OstreeRepo> getCurrentlyConfiguredOstreeRepos(File fromConfigFile) {
				
		return OstreeRepo.parse(client.runCommandAndWait("cat "+fromConfigFile).getStdout());
	}
	

}


/* dress rehearsal notes:

Upgrade the system using the instructions from the getting started guide above, see: http://file.brq.redhat.com/~bexelbie/appinfra-docs/Atomic_OSTree_Get_Started.pdf

-bash-4.2# rpm -q docker ostree
docker-1.2.0-1.7.el7.x86_64
ostree-2014.9-3.atomic.el7.x86_64

-bash-4.2# rpm -qa | grep subscr
subscription-manager-plugin-ostree-1.13.7-1.el7.x86_64
subscription-manager-1.13.7-1.el7.x86_64
subscription-manager-plugin-container-1.13.7-1.el7.x86_64

BEFORE REGISTER AND ATTACH
-bash-4.2# cat /ostree/repo/config 
[core]
repo_version=1
mode=bare
-bash-4.2# python /usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py --deployed-origin
/ostree/deploy/rhel-atomic-host/deploy/335ae3551939b8de8fd6663ec9db834b1d2150fe7a5b9b1017b74c1070006888.0.origin
-bash-4.2# cat /ostree/deploy/rhel-atomic-host/deploy/335ae3551939b8de8fd6663ec9db834b1d2150fe7a5b9b1017b74c1070006888.0.origin
[origin]
refspec=rhel-atomic-host:rhel-atomic-host/7/x86_64/standard
unconfigured-state=This system is not registered to Red Hat Subscription Management. You can use subscription-manager to register.
-bash-4.2# atomic status
  VERSION     ID             OSNAME               REFSPEC                                                 
* 7.0.1.0     335ae35519     rhel-atomic-host     rhel-atomic-host:rhel-atomic-host/7/x86_64/standard     
-bash-4.2# 



AFTER REGISTER AND ATTACH
-bash-4.2# subscription-manager register --serverurl=subscription.rhn.stage.redhat.com --baseurl=https://cdn.stage.redhat.com --auto-attach
Username: atomic_beta_6
Password: 
The system has been registered with ID: d8f1becc-08d5-40c8-8d4c-4742bf176e04 

Installed Product Current Status:
Product Name: Red Hat Enterprise Linux Server
Status:       Subscribed

Product Name: Red Hat Enterprise Linux Atomic Host Beta
Status:       Subscribed

-bash-4.2# cat /ostree/repo/config 
[core]
repo_version=1
mode=bare

[remote "rhel-atomic-host-ostree"]
url = https://cdn.stage.redhat.com/content/dist/rhel/atomic/7/7Server/x86_64/ostree/repo
gpg-verify = false
tls-client-cert-path = /etc/pki/entitlement/4413554849224567690.pem
tls-client-key-path = /etc/pki/entitlement/4413554849224567690-key.pem
tls-ca-path = /etc/rhsm/ca/redhat-uep.pem

[remote "rhel-atomic-host-beta-ostree"]
url = https://cdn.stage.redhat.com/content/beta/rhel/atomic/7/x86_64/ostree/repo
gpg-verify = false
tls-client-cert-path = /etc/pki/entitlement/4413554849224567690.pem
tls-client-key-path = /etc/pki/entitlement/4413554849224567690-key.pem
tls-ca-path = /etc/rhsm/ca/redhat-uep.pem
-bash-4.2# python /usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py --deployed-origin
/ostree/deploy/rhel-atomic-host/deploy/335ae3551939b8de8fd6663ec9db834b1d2150fe7a5b9b1017b74c1070006888.0.origin
-bash-4.2# cat /ostree/deploy/rhel-atomic-host/deploy/335ae3551939b8de8fd6663ec9db834b1d2150fe7a5b9b1017b74c1070006888.0.origin
[origin]
refspec=rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard
-bash-4.2# 
-bash-4.2# atomic status
  VERSION     ID             OSNAME               REFSPEC                                                             
* 7.0.1.0     335ae35519     rhel-atomic-host     rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard     
-bash-4.2# 


ATTEMPTING TO UPGRADE
-bash-4.2# atomic status
  VERSION     ID             OSNAME               REFSPEC                                                             
* 7.0.1.0     335ae35519     rhel-atomic-host     rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard     
-bash-4.2# 
-bash-4.2# 
-bash-4.2# atomic upgrade
Updating from: rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard

474 metadata, 2194 content objects fetched; 89393 KiB transferred in 302 seconds
Copying /etc changes: 11 modified, 4 removed, 35 added
Transaction complete; bootconfig swap: yes deployment count change: 1
Changed:
  docker-storage-setup-0.0.3-1.el7.noarch
  kernel-3.10.0-123.9.2.el7.x86_64
Updates prepared for next boot; run "systemctl reboot" to start a reboot
-bash-4.2# echo $?
0
-bash-4.2# atomic status
  VERSION     ID             OSNAME               REFSPEC                                                             
  7.0.2       9a0dbc159e     rhel-atomic-host     rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard     
* 7.0.1.0     335ae35519     rhel-atomic-host     rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard     
-bash-4.2#


-bash-4.2# 
-bash-4.2# systemctl reboot
Connection to 10.16.6.54 closed by remote host.
Connection to 10.16.6.54 closed.
[jsefler@jseflerT5400 ~]$ ssh -XYC root@10.16.6.54
root@10.16.6.54's password: 
Last login: Wed Oct 29 17:06:54 2014
-bash-4.2# 
-bash-4.2# 
-bash-4.2# atomic status
  VERSION     ID             OSNAME               REFSPEC                                                             
* 7.0.2       9a0dbc159e     rhel-atomic-host     rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard     
  7.0.1.0     335ae35519     rhel-atomic-host     rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard  




ROLLBACK

-bash-4.2# atomic status
  VERSION     ID             OSNAME               REFSPEC                                                             
* 7.0.2       9a0dbc159e     rhel-atomic-host     rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard     
  7.0.1.0     335ae35519     rhel-atomic-host     rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard     
-bash-4.2# python /usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py --deployed-origin
/ostree/deploy/rhel-atomic-host/deploy/9a0dbc159eedc7d3538a84ecedc6d7b6c73489b0f13a2ea043c4646ac9ae443a.0.origin
-bash-4.2# cat /ostree/deploy/rhel-atomic-host/deploy/9a0dbc159eedc7d3538a84ecedc6d7b6c73489b0f13a2ea043c4646ac9ae443a.0.origin
[origin]
refspec=rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard
-bash-4.2# atomic rollback
Moving '335ae3551939b8de8fd6663ec9db834b1d2150fe7a5b9b1017b74c1070006888.0' to be first deployment
Transaction complete; bootconfig swap: yes deployment count change: 0
Changed:
  docker-storage-setup-0.0.2-1.el7.noarch
  kernel-3.10.0-123.8.1.el7.x86_64
Sucessfully reset deployment order; run "systemctl reboot" to start a reboot
-bash-4.2# echo $?
0
-bash-4.2# systemctl reboot
Connection to 10.16.6.54 closed by remote host.
Connection to 10.16.6.54 closed.
[jsefler@jseflerT5400 ~]$ ssh -XYC root@10.16.6.54
root@10.16.6.54's password: 
Last login: Wed Oct 29 17:23:56 2014
-bash-4.2# atomic status
  VERSION     ID             OSNAME               REFSPEC                                                             
* 7.0.1.0     335ae35519     rhel-atomic-host     rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard     
  7.0.2       9a0dbc159e     rhel-atomic-host     rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard     
-bash-4.2# 

NOTE: This sample output shows that rhel-atomic-host d45dfe1... will be booted into on the next restart. The version to be booted on
the next restart is printed first.




UNSUBSCRIBING REMOVES THE REMOTE FROM /etc/ostree/remotes.d
-bash-4.2# subscription-manager unsubscribe --all
1 subscription removed at the server.
1 local certificate has been deleted.
-bash-4.2# atomic upgrade
Updating from: rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard


error: No remote 'remote "rhel-atomic-host-beta-ostree"' found in /etc/ostree/remotes.d
-bash-4.2# python /usr/share/rhsm/subscription_manager/plugin/ostree/gi_wrapper.py --deployed-origin
/ostree/deploy/rhel-atomic-host/deploy/335ae3551939b8de8fd6663ec9db834b1d2150fe7a5b9b1017b74c1070006888.0.origin
-bash-4.2# cat /ostree/deploy/rhel-atomic-host/deploy/335ae3551939b8de8fd6663ec9db834b1d2150fe7a5b9b1017b74c1070006888.0.origin
[origin]
refspec=rhel-atomic-host-beta-ostree:rhel-atomic-host/7/x86_64/standard
-bash-4.2# ls /etc/ostree/remotes.d
-bash-4.2# 



*/


/*
TODO: For testing purposes on RHEL7 (rather than atomic)
Install packages from brew:
  ostree
  libgsystem
  rpm -Uvh http://download.devel.redhat.com/brewroot/packages/ostree/2015.6/4.atomic.el7/x86_64/ostree-2015.6-4.atomic.el7.x86_64.rpm http://download.devel.redhat.com/brewroot/packages/libgsystem/2015.1/1.atomic.el7/x86_64/libgsystem-2015.1-1.atomic.el7.x86_64.rpm
*/
