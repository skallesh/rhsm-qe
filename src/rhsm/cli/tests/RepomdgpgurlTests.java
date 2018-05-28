package rhsm.cli.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.redhat.qe.Assert;
import com.redhat.qe.tools.SSHCommandResult;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ProductSubscription;

/**
 * @author skallesh
 *
 *
 */
@Test(groups = {"RepomdgpgurlTests"})
public class RepomdgpgurlTests extends SubscriptionManagerCLITestScript{
    String poolId=null;
    String productId = "Repomd-product";
    String ProductName = "RepomdgpgurlTestSubscription";
    String contentName = "RepomdContent";
    String contentLabel = "RepomdLabel";
    String providedProd="37060";
	private String baseurlBeforeExecution;
	private String repomdgpgurlBeforeExecution;

    
    /**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RedHatEnterpriseLinux7},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify the installaction of packages are prevented without verification of the repository metadata.",
	groups = {"Tier3Tests","repomd_gpg_urlTests" },
	enabled = true)
	public void repomd_gpg_urlTests(){
		Map<String,String> repoOverrideNameValueMap = new HashMap<String,String>();
		client.runCommandAndWait("yum-config-manager --disable beaker*");
		log.info("Case 1: Invalid repomd_gpg_url in rhsm.conf");
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "rhsm", "baseurl", "http://download.devel.redhat.com" });
		listOfSectionNameValues.add(new String[] { "rhsm", "repomd_gpg_url", "foo" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null,(String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.subscribe(null, null, poolId, null, null, null, null, null, null, null, null, null, null);
		repoOverrideNameValueMap.put("gpgcheck", "1");
		repoOverrideNameValueMap.put("repo_gpgcheck", "1");
		clienttasks.repo_override(null, null, contentLabel, null, repoOverrideNameValueMap, null, null, null, null);
		SSHCommandResult repoOverride = clienttasks.repo_override(true, null,(String)null, null, null, null, null, null, null);
		Assert.assertContainsMatch(repoOverride.getStdout(), "repo_gpgcheck", "repoOverride repo_gpgcheck has now been added");
		
		String consumedList=clienttasks.list(null, null, true, null, null, null, null, null, null, null, null, null, null, null, null).getStdout();
		Assert.assertContainsMatch(consumedList, ProductName,  "Consumed list contains desired subscription");
		String reposResult=clienttasks.repos(true, null, null, (String)null, null, null, null, null, null).getStdout();
		Assert.assertContainsMatch(reposResult, contentLabel, "Repo list contains desired Content");
		ProductSubscription consumedSubscription= ProductSubscription.findFirstInstanceWithCaseInsensitiveMatchingFieldFromList("productName", ProductName, clienttasks.getCurrentlyConsumedProductSubscriptions());
		System.out.println(consumedSubscription.serialNumber);
		String command = "rct cat-cert "+clienttasks.entitlementCertDir+"/"+consumedSubscription.serialNumber+".pem";
		SSHCommandResult RCTresult = client.runCommandAndWait(command);
		Assert.assertContainsMatch(RCTresult.getStdout(), contentLabel, "Entitlement cert has the newly added content");
		SSHCommandResult yumRepolistResult = client.runCommandAndWait("yum repolist");
		Assert.assertContainsMatch(yumRepolistResult.getStderr(), "Gpg Keys not imported, cannot verify repomd.xml", "yum repolist fails to download GPG keys");
		yumRepolistResult = client.runCommandAndWait("yum install zsh -y");
		Assert.assertContainsMatch(yumRepolistResult.getStderr(), "Gpg Keys not imported, cannot verify repomd.xml", "yum repolist fails to download GPG keys");

		log.info("Case 2: invalid repomd_gpg_url in rhsm.conf but repo_gpgcheck in /etc/yum.conf is set to 0");
		repoOverrideNameValueMap.clear();
		repoOverrideNameValueMap.put("repo_gpgcheck", "0");
		repoOverrideNameValueMap.put("gpgcheck", "0");

		clienttasks.repo_override(null, null, contentLabel, null, repoOverrideNameValueMap, null, null, null, null);
		client.runCommandAndWait("yum clean all");
		yumRepolistResult = client.runCommandAndWait("yum repolist");
		Assert.assertContainsNoMatch(yumRepolistResult.getStderr(), "Gpg Keys not imported", "yum repolist doesnot throw Gpg Keys not imported error as the repo_gpgcheck is turned off");
		yumRepolistResult = client.runCommandAndWait("yum install zsh -y");
		Assert.assertContainsNoMatch(yumRepolistResult.getStderr(), "Gpg Keys not imported", "yum install doesnot throw Gpg Keys not imported error as the repo_gpgcheck is turned off");
		yumRepolistResult = client.runCommandAndWait("yum remove zsh -y");

		log.info("Case 3 : valid repomd_gpg_url in rhsm.conf and repo_gpgcheck in /etc/yum.conf is set to 1"); 
		
		repoOverrideNameValueMap.clear();
		listOfSectionNameValues.clear();
		listOfSectionNameValues.add(new String[] { "rhsm", "repomd_gpg_url", "http://download.devel.redhat.com/released/CentOS/RPM-GPG-KEY-CentOS-7" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		repoOverrideNameValueMap.put("repo_gpgcheck", "1");
		repoOverrideNameValueMap.put("gpgcheck", "1");
		clienttasks.repo_override(null, null, contentLabel, null, repoOverrideNameValueMap, null, null, null, null);
		client.runCommandAndWait("yum clean all");
		yumRepolistResult = client.runCommandAndWait("yum repolist -y");
		Assert.assertContainsNoMatch(yumRepolistResult.getStderr(), "Gpg Keys not imported", "yum repolist doesnot throw Gpg Keys not imported error as the gpg is an valid one");
		/*Stderr: 
		Importing GPG key 0xF4A80EB5:
			 Userid     : "CentOS-7 Key (CentOS 7 Official Signing Key) <security@centos.org>"
			 Fingerprint: 6341 ab27 53d7 8a78 a7c2 7bb1 24c6 a8a7 f4a8 0eb5
			 From       : http://download.devel.redhat.com/released/CentOS/RPM-GPG-KEY-CentOS-7
*/
		Assert.assertContainsMatch(yumRepolistResult.getStderr(), "Importing GPG key", "yum repolist doesnot throw Gpg Keys not imported error as the gpg is an valid one");
		yumRepolistResult = client.runCommandAndWait("yum install zsh -y");
		Assert.assertContainsNoMatch(yumRepolistResult.getStderr(), "Gpg Keys not imported", "yum install doesnot throw Gpg Keys not imported error as the gpg is an valid one");

	
		
		
	}
	@BeforeGroups(groups = { "setup" }, value = {"repomd_gpg_urlTests" })
	public void copyRHSMConfFileValues() {
		baseurlBeforeExecution = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "baseurl");
		repomdgpgurlBeforeExecution=clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "repomd_gpg_url");
	}
	
	@AfterGroups(groups = { "setup" }, value = {"repomd_gpg_urlTests" })
	public void restoreRHSMConfFileValues() {
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "rhsm", "baseurl".toLowerCase(),baseurlBeforeExecution });
		listOfSectionNameValues.add(new String[] { "rhsm", "baseurl".toLowerCase(),repomdgpgurlBeforeExecution });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
	}


	
	@SuppressWarnings("deprecation")
	@BeforeGroups(groups = "setup", value = {"repomd_gpg_urlTests"}, enabled = true)
	protected void createTestPoolBeforeGroup()	throws JSONException, Exception {
		List<String> providedProduct = new ArrayList<String>();
	    providedProduct.clear();
	    providedProduct.add(providedProd);
	    clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
					null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
	    String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					consumerId);
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.clear();
		attributes.put("version", "1.0");
		attributes.put("variant", "server");
		attributes.put("arch", "ALL");
		attributes.put("warning_period", "30");
		attributes.put("type", "MKT");
		attributes.put("type", "SVC");
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, productId);
		String resourcePath = "/products/" + productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				sm_clientOrg, ProductName, productId, 1, attributes, null);
		poolId= CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, ownerKey, 3, -60 * 24, 60 * 24,
				getRandInt(), getRandInt(), productId, providedProduct, null).getString("id");
		System.out.println("inside creating pool "+ poolId);
	}
	
	@BeforeGroups(groups = "setup", value = {"repomd_gpg_urlTests"}, enabled = true)
	protected void createAndAddContentBeforeGroup() throws Exception {
		String contentId = "1000000000000";
		String resourcePath = "/content/"+contentId;
		String yumVarPath = "$basearch/";
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				consumerId);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		CandlepinTasks.createContentUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, contentName, contentId, contentLabel, "yum", "Repomd Vendor", "/released/CentOS/7.1/updates/x86_64/", null, "0", null, null, null);
		CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, ownerKey,providedProd, contentId, true);
	}
}
