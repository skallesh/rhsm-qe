package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testng.TestNgPriority;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.ContentNamespace;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.ProductNamespace;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * References:
 * https://docspace.corp.redhat.com/docs/DOC-60198
 * http://gibson.usersys.redhat.com:9000/Integration-Testing-Issues
 * https://docspace.corp.redhat.com/docs/DOC-63084
 * https://docspace.corp.redhat.com/docs/DOC-67214
 * https://docspace.corp.redhat.com/docs/DOC-68623
 *
 * 
 * Where to look when a product cert does not get installed:
 * e.g.
 * repo [rhel-scalefs-for-rhel-5-server-rpms]  
 * baseurl=https://cdn.redhat.com/content/dist/rhel/server/5/$releasever/$basearch/scalablefilesystem/os
 * if the expected productid has 92 is not getting installed from this repo, browse to:
 * http://download.devel.redhat.com/cds/prod/content/dist/rhel/server/5/5Server/x86_64/scalablefilesystem/os/repodata/
 * if no productid is there, then contact rhel-eng/jgreguske/dgregor
 */

@Test(groups={"IntegrationTests"})
public class IntegrationTests extends SubscriptionManagerCLITestScript{

	
	// Test Methods ***********************************************************************

	@Test(	description="register and subscribe to expected product subscription",
			groups={},
			dataProvider="getSubscribeData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=) //TODO Find a tcms caseId
	public void Subscribe_Test(String username, String password, String productId) {
		clienttasks.register(username, password, null, null, null, null, true, null, null, null);
		File entitlementCertFile = clienttasks.subscribeToProductId(productId);

		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		entitlementCertData.add(Arrays.asList(new Object[]{username, password, productId, entitlementCert}));
	}
	
	@Test(	description="verify the CDN provides packages for the default enabled content set after subscribing to a product subscription",
			groups={"VerifyPackagesAreAvailable"},
			dependsOnMethods={"Subscribe_Test"}, alwaysRun=true,
			dataProvider="getDefaultEnabledContentNamespaceData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=) //TODO Find a tcms caseId
	public void VerifyPackagesAreAvailableForDefaultEnabledContentNamespace_Test(String username, String password, String productId, ContentNamespace contentNamespace) {
		String abled = contentNamespace.enabled.equals("1")? "enabled":"disabled";	// is this an enabled or disabled test?
		Integer packageCount=null;

//if(true) throw new SkipException("debugging");
		// register
		if (!username.equals(currentRegisteredUsername)) { // try to save some time by not re-registering
			clienttasks.register(username, password, null, null, null, null, true, null, null, null);
			currentRegisteredUsername = username;
			currentlySubscribedProductIds.clear();
		} else {
			log.info("Trying to save time by assuming that we are already registered as username='"+username+"'");
		}
		
		// assert that there are not yet any available packages from the default enabled/disabled repo
		// NOT REALLY A VALID ASSERTION WHEN WE COULD ALREADY BE SUBSCRIBED (FOR EFFICIENCY SAKE).  MOREOVER, THE MORE APPROPRIATE ASSERTION COMES AFTER THE SUBSCRIBE)   
		//packageCount = clienttasks.getYumRepolistPackageCount(contentNamespace.label);
		//Assert.assertEquals(packageCount,Integer.valueOf(0),"Before subscribing to product subscription '"+productId+"', the number of available packages '"+packageCount+"' from the default "+abled+" repo '"+contentNamespace.label+"' is zero.");

		// subscribe
		if (!currentlySubscribedProductIds.contains(productId)) { // try to save some time by not re-subscribing
			clienttasks.subscribeToProductId(productId);
			currentlySubscribedProductIds.add(productId);
		} else {
			log.info("Trying to save time by assuming that we are already subscribed to productId='"+productId+"'");
		}

		// Assert that after subscribing, the default enabled/disabled repo is now included in yum repolist
		ArrayList<String> repolist = clienttasks.getYumRepolist(abled);
		if (clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, currentProductCerts)) {
			Assert.assertTrue(repolist.contains(contentNamespace.label),
				"Yum repolist "+abled+" includes "+abled+" repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+productId+"'.");
		} else {
			log.warning("Did not find all the requiredTags '"+contentNamespace.requiredTags+"' for this content namespace amongst the currently installed products.");
			Assert.assertFalse(repolist.contains(contentNamespace.label),
				"Yum repolist "+abled+" excludes "+abled+" repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+productId+"' because not all requiredTags ("+contentNamespace.requiredTags+") in the contentNamespace are provided by the currently installed productCerts.");
			throw new SkipException("This contentNamespace has requiredTags '"+contentNamespace.requiredTags+"' that were not found amongst all of the currently installed products.  Therefore we cannot verify that the CDN is providing packages for repo '"+contentNamespace.label+"'.");
		}

		// verify the yum repolist contentNamespace.label returns more than 0 packages
		String options = contentNamespace.enabled.equals("1")? contentNamespace.label:contentNamespace.label+" --enablerepo="+contentNamespace.label;
		packageCount = clienttasks.getYumRepolistPackageCount(options);
		Assert.assertTrue(packageCount>0,"After subscribing to product subscription '"+productId+"', the number of available packages from the default "+abled+" repo '"+contentNamespace.label+"' is greater than zero (actual packageCount is '"+packageCount+"').");
	}
	
	
	@Test(	description="verify the CDN provides packages for the non-default enabled content set after subscribing to a product subscription",
			groups={"VerifyPackagesAreAvailable"},
			dependsOnMethods={"Subscribe_Test"}, alwaysRun=true,
			dataProvider="getDefaultDisabledContentNamespaceData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=) //TODO Find a tcms caseId
	public void VerifyPackagesAreAvailableForDefaultDisabledContentNamespace_Test(String username, String password, String productId, ContentNamespace contentNamespace) {
		Assert.assertEquals(contentNamespace.enabled,"0","Reconfirming that we are are about to test a default disabled contentNamespace.");
		VerifyPackagesAreAvailableForDefaultEnabledContentNamespace_Test(username, password, productId, contentNamespace);
	}
	
	
	@Test(	description="ensure an available package can be downloaded/installed/removed from the enabled repo ",
			groups={},
			dependsOnMethods={"Subscribe_Test"}, alwaysRun=true,
			dependsOnGroups={"VerifyPackagesAreAvailable"},
			dataProvider="getContentNamespaceWithProductNamespacesData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=) //TODO Find a tcms caseId
	public void InstallAndRemoveAnyPackageFromContentNamespace_Test(String username, String password, String productId, ContentNamespace contentNamespace, List<ProductNamespace> productNamespaces) {

//if (!contentNamespace.label.equals("rhel-6-server-beta-debug-rpms")) throw new SkipException("debugging");
		// register
		if (!username.equals(currentRegisteredUsername)) { // try to save some time by not re-registering
			clienttasks.register(username, password, null, null, null, null, true, null, null, null);
			currentRegisteredUsername = username;
			currentlySubscribedProductIds.clear();
		} else {
			log.info("Trying to save time by assuming that we are already registered as username='"+username+"'");
		}
		
		// subscribe
		if (!currentlySubscribedProductIds.contains(productId)) { // try to save some time by not re-subscribing
			clienttasks.subscribeToProductId(productId);
			currentlySubscribedProductIds.add(productId);
		} else {
			log.info("Trying to save time by assuming that we are already subscribed to productId='"+productId+"'");
		}
		
		
		// make sure that the products required for this repo are installed
		if (!clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, currentProductCerts)) {
			throw new SkipException("This contentNamespace has requiredTags '"+contentNamespace.requiredTags+"' that were not found amongst all of the currently installed products.  Therefore we cannot install and remove any package from repo '"+contentNamespace.label+"'.");
		}
		
		
		// make sure there is a positive package count provided by this repo
		Integer packageCount = clienttasks.getYumRepolistPackageCount(contentNamespace.label+" --enablerepo="+contentNamespace.label);
		if (packageCount==0) {
			throw new SkipException("Cannot install a package from this repo '"+contentNamespace.label+"' since it is not providing any packages.");
		}
		
		// find an available package that is uniquely provided by this repo
		String pkg = clienttasks.findUniqueAvailablePackageFromRepo(contentNamespace.label);
		if (pkg==null) {
			throw new SkipException("Could NOT find a unique available package from this repo '"+contentNamespace.label+"' to attempt an install/remove test.");
		}
//pkg="cairo-spice-debuginfo.x86_64";
		
		// install the package and assert that it is successfully installed
		clienttasks.yumInstallPackageFromRepo(pkg, contentNamespace.label, null); //pkgInstalled = true;


		// 06/09/2011 TODO Would also like to add an assert that the productid.pem file is also installed.
		/* To do this, we need to also include the List of ProductNamepaces from the entitlement cert as another argument to this test,
		 * Then after the yum install, we need to make sure that at least one of the hash values in the list of product ids is
		 * included in the installed products.  If the list of ProductNamespaces from the entitlement cert is one, then this is
		 * a definitive test.  If the list is greater than one, then we don't know for sure if the product hash(s) that is installed is actually the 
		 * right one.  But we do know that if none of the product hashes are installed, then the repo is missing the product ids and this test should fail.
		 * Also note that we should probably make this assertion after removing the package so that we don't over install all the packages in the repo.
		 */
		// 06/10/2011 Mostly Done in the following blocks of code; jsefler
		
		// determine if at least one of the productids from the productNamespaces was found installed on the client after running yumInstallPackageFromRepo(...)
		// ideally there is only one ProductNamespace in productNamespaces in which case we can definitively know that the correct product cert is installed
		// when there are more than one ProductNamespace in productNamespaces, then we can't say for sure if the product cert installed actually corresponds to the repo under test
		// however if none of the productNamespaces ends up installed, then the yum product-id plugin is not installing the expected product cert
		int numberOfProductNamespacesInstalled = 0;
		ProductCert productCertInstalled=null;
		for (ProductCert productCert : clienttasks.getCurrentProductCerts()) {
			for (ProductNamespace productNamespace : productNamespaces) {
				if (productNamespace.hash.equals(productCert.hash)) {
					numberOfProductNamespacesInstalled++;
					productCertInstalled=productCert;
				}
			}
		}

		//FIXME check if the package was obsolete and its replacement was installed instead
		//if (!obsoletedByPkg.isEmpty()) pkg = obsoletedByPkg;
		
		// now remove the package
		clienttasks.yumRemovePackage(pkg);
		
		// assert that a productid.pem is/was installed that covers the product from which this package was installed
		// Note: I am making this assertion after the yumRemovePackage call to avoid leaving packages installed
		if (numberOfProductNamespacesInstalled>1) {
			log.info("Found product certs installed that match the ProductNamespaces from the entitlement cert that provided the right to install package '"+pkg+"' from repo '"+contentNamespace.label+"'.");
		} else if (numberOfProductNamespacesInstalled==1){
			Assert.assertTrue(true,"An installed product cert (productName='"+productCertInstalled.productName+"' hash='"+productCertInstalled.hash+"') corresponding to installed package '"+pkg+"' from repo '"+contentNamespace.label+"' was found after its install.");
		} else {
			Assert.fail("After installing package '"+pkg+"' from repo '"+contentNamespace.label+"', there was no productid cert installed.  Expected one of the following productid certs to get installed via the yum product-id plugin: "+productNamespaces);		
		}
	}
	
	
	
	
//	@Test()
//	@TestNgPriority(400)
//	public void Test400() {}	
//	@Test()
//	@TestNgPriority(500)
//	public void Test500() {}
//	@Test()
//	@TestNgPriority(100)
//	public void Test100() {}
//	@Test()
//	@TestNgPriority(200)
//	public void Test200() {}
//	@Test(dependsOnMethods={"Test200"})	// adding a dependsOn* breaks the priority
//	@TestNgPriority(300)
//	public void Test300() {}

	// Candidates for an automated Test:
	// TODO Bug 689031 - nss needs to be able to use pem files interchangeably in a single process 

	
	
	
	
	
	// Configuration Methods ***********************************************************************
	
	@BeforeClass(groups={"setup"})
	public void getCurrentProductCertsBeforeClass() {
		currentProductCerts = clienttasks.getCurrentProductCerts();
	}
	
//	@BeforeClass(groups={"setup"})
//	public void yumCleanAllBeforeClass() {
//		clienttasks.yumClean("all");
//	}
	
	// Protected Methods ***********************************************************************
	
	List<List<Object>> entitlementCertData = new ArrayList<List<Object>>();
	List<ProductCert> currentProductCerts = new ArrayList<ProductCert>();
	protected String currentRegisteredUsername = null;
	protected List<String> currentlySubscribedProductIds = new ArrayList<String>();;
	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="getSubscribeData")
	public Object[][] getSubscribeDataAs2dArray() throws JSONException {
		return TestNGUtils.convertListOfListsTo2dArray(getSubscribeDataAsListOfLists());
	}
	protected List<List<Object>> getSubscribeDataAsListOfLists() throws JSONException {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		//JSONArray jsonIntegrationTestData = new JSONArray(getProperty("sm.integrationTestData", "<>").replaceAll("<", "[").replaceAll(">", "]")); // hudson parameters use <> instead of []
		JSONArray jsonIntegrationTestData = new JSONArray(getProperty("sm.integrationTestData", "[]").replaceFirst("^\"", "").replaceFirst("\"$", "").replaceAll("<", "[").replaceAll(">", "]")); // hudson JSONArray parameters get surrounded with double quotes that need to be stripped
		for (int i = 0; i < jsonIntegrationTestData.length(); i++) {
			JSONObject jsonIntegrationTestDatum = (JSONObject) jsonIntegrationTestData.get(i);
			String username = jsonIntegrationTestDatum.getString("username");
			String password = jsonIntegrationTestDatum.getString("password");
			String arch = "ALL";
			if (jsonIntegrationTestDatum.has("arch")) arch = jsonIntegrationTestDatum.getString("arch");
			String variant = "ALL";
			if (jsonIntegrationTestDatum.has("variant")) variant = jsonIntegrationTestDatum.getString("variant");
	
			// skip this jsonIntegrationTestDatum when it does not match the client arch
			if (!arch.equals("ALL") && !arch.equals(clienttasks.arch)) continue;
			
			// skip this jsonIntegrationTestDatum when it does not match the client variant
			if (!variant.equals("ALL") && !variant.equals(clienttasks.variant)) continue;
		
			JSONArray jsonProductIdsData = (JSONArray) jsonIntegrationTestDatum.getJSONArray("productIdsData");
			for (int j = 0; j < jsonProductIdsData.length(); j++) {
				JSONObject jsonProductIdsDatum = (JSONObject) jsonProductIdsData.get(j);
				String productId = jsonProductIdsDatum.getString("productId");
				
				// String username, String password, String productId
				ll.add(Arrays.asList(new Object[]{username, password, productId}));
			}
		}
		
		return ll;
	}
	
	@DataProvider(name="getDefaultEnabledContentNamespaceData")
	public Object[][] getDefaultEnabledContentNamespaceDataAs2dArray() throws JSONException {
		return TestNGUtils.convertListOfListsTo2dArray(getContentNamespaceDataAsListOfLists("1", false));
	}
	@DataProvider(name="getDefaultDisabledContentNamespaceData")
	public Object[][] getDefaultDisabledContentNamespaceDataAs2dArray() throws JSONException {
		return TestNGUtils.convertListOfListsTo2dArray(getContentNamespaceDataAsListOfLists("0", false));
	}
	@DataProvider(name="getContentNamespaceData")
	public Object[][] getContentNamespaceDataAs2dArray() throws JSONException {
		return TestNGUtils.convertListOfListsTo2dArray(getContentNamespaceDataAsListOfLists(null, false));
	}
	@DataProvider(name="getContentNamespaceWithProductNamespacesData")
	public Object[][] getContentNamespaceWithProductNamespacesDataAs2dArray() throws JSONException {
		return TestNGUtils.convertListOfListsTo2dArray(getContentNamespaceDataAsListOfLists(null, true));
	}
	protected List<List<Object>> getContentNamespaceDataAsListOfLists(String enabledValue, boolean withProductNamespaces) throws JSONException {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		for (List<Object> row : entitlementCertData) {
			String username = (String) row.get(0);
			String password = (String) row.get(1);
			String productId = (String) row.get(2);
			EntitlementCert entitlementCert = (EntitlementCert) row.get(3);
			
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				if (contentNamespace.enabled.equals(enabledValue) || enabledValue==null) {	// enabled="1", not enabled="0", either=null
					
					// String username, String password, String productId, ContentNamespace contentNamespace, List<ProductNamespaces> productNamespaces
					if (withProductNamespaces)	ll.add(Arrays.asList(new Object[]{username, password, productId, contentNamespace, entitlementCert.productNamespaces}));
					else 						ll.add(Arrays.asList(new Object[]{username, password, productId, contentNamespace}));
				}
			}
		}
		return ll;
	}



}



//[root@jsefler-betastage-server pki]# yum -y install cairo-spice-debuginfo.x86_64 --enablerepo=rhel-6-server-beta-debug-rpms --disableplugin=rhnplugin
//Loaded plugins: product-id, refresh-packagekit, subscription-manager
//No plugin match for: rhnplugin
//Updating Red Hat repositories.
//INFO:rhsm-app.repolib:repos updated: 63
//rhel-6-server-beta-debug-rpms                                                                                                                   |  951 B     00:00     
//rhel-6-server-beta-rpms                                                                                                                         | 3.7 kB     00:00     
//rhel-6-server-rpms                                                                                                                              | 2.1 kB     00:00     
//Setting up Install Process
//Package cairo-spice-debuginfo is obsoleted by spice-server, trying to install spice-server-0.7.3-2.el6.x86_64 instead
//Resolving Dependencies
//--> Running transaction check
//---> Package spice-server.x86_64 0:0.7.3-2.el6 will be installed
//--> Finished Dependency Resolution
//
//Dependencies Resolved
//
//=======================================================================================================================================================================
//Package                                Arch                             Version                               Repository                                         Size
//=======================================================================================================================================================================
//Installing:
//spice-server                           x86_64                           0.7.3-2.el6                           rhel-6-server-beta-rpms                           245 k
//
//Transaction Summary
//=======================================================================================================================================================================
//Install       1 Package(s)
//
//Total download size: 245 k
//Installed size: 913 k
//Downloading Packages:
//spice-server-0.7.3-2.el6.x86_64.rpm                                                                                                             | 245 kB     00:00     
//Running rpm_check_debug
//Running Transaction Test
//Transaction Test Succeeded
//Running Transaction
//Installing : spice-server-0.7.3-2.el6.x86_64                                                                                                                     1/1 
//duration: 297(ms)
//Installed products updated.
//
//Installed:
//spice-server.x86_64 0:0.7.3-2.el6                                                                                                                                    
//
//Complete!
//[root@jsefler-betastage-server pki]# yum remove spice-server.x86_64
//Loaded plugins: product-id, refresh-packagekit, subscription-manager
//Updating Red Hat repositories.
//INFO:rhsm-app.repolib:repos updated: 63
//Setting up Remove Process
//Resolving Dependencies
//--> Running transaction check
//---> Package spice-server.x86_64 0:0.7.3-2.el6 will be erased
//--> Finished Dependency Resolution
//rhel-6-server-beta-rpms                                                                                                                         | 3.7 kB     00:00     
//rhel-6-server-rpms                                                                                                                              | 2.1 kB     00:00     
//
//Dependencies Resolved
//
//=======================================================================================================================================================================
//Package                               Arch                            Version                                 Repository                                         Size
//=======================================================================================================================================================================
//Removing:
//spice-server                          x86_64                          0.7.3-2.el6                             @rhel-6-server-beta-rpms                          913 k
//
//Transaction Summary
//=======================================================================================================================================================================
//Remove        1 Package(s)
//
//Installed size: 913 k
//Is this ok [y/N]: y
//Downloading Packages:
//Running rpm_check_debug
//Running Transaction Test
//Transaction Test Succeeded
//Running Transaction
//Erasing    : spice-server-0.7.3-2.el6.x86_64                                                                                                                     1/1 
//duration: 207(ms)
//Installed products updated.
//
//Removed:
//spice-server.x86_64 0:0.7.3-2.el6                                                                                                                                    
//
//Complete!
//[root@jsefler-betastage-server pki]# 
