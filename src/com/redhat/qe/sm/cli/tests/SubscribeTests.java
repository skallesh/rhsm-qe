package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.SubscriptionManagerTasks;
import com.redhat.qe.sm.data.ContentNamespace;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.InstalledProduct;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

/**
 * @author ssalevan
 * @author jsefler
 *
 */
@Test(groups={"subscribe"})
public class SubscribeTests extends SubscriptionManagerCLITestScript{
	
	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an expected subscription pool product id",
			dataProvider="getSubscriptionPoolProductIdData",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void SubscribeToExpectedSubscriptionPoolProductId_Test(String productId, String[] entitledProductNames) {
		List<ProductCert> currentlyInstalledProductCerts = clienttasks.getCurrentProductCerts();

		// begin test with a fresh register
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null);

		// assert the subscription pool with the matching productId is available
		SubscriptionPool pool = clienttasks.findSubscriptionPoolWithMatchingFieldFromList("productId", productId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(pool, "Expected SubscriptionPool with ProductId '"+productId+"' is available for subscribing.");

		// assert the status of the installed products
		for (String productName : entitledProductNames) {
			// assert the status of the installed products
			ProductCert productCert = clienttasks.findProductCertWithMatchingFieldFromList("productName", productName, currentlyInstalledProductCerts);
			if (productCert!=null) {
				InstalledProduct installedProduct = clienttasks.findInstalledProductWithMatchingFieldFromList("productName", productName, clienttasks.getCurrentlyInstalledProducts());
				Assert.assertNotNull(installedProduct, "The status of product with ProductName '"+productName+"' is reported in the list of installed products.");
				Assert.assertEquals(installedProduct.status, "Not Subscribed", "Before subscribing to ProductId '"+productId+"', the status of Installed Product '"+productName+"' is Not Subscribed.");
			}
		}
		
		// subscribe to the pool
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		
		// assert the expected products are consumed
		for (String productName : entitledProductNames) {
			ProductSubscription productSubscription = clienttasks.findProductSubscriptionWithMatchingFieldFromList("productName", productName, clienttasks.getCurrentlyConsumedProductSubscriptions());
			Assert.assertNotNull(productSubscription, "Expected ProductSubscription with ProductName '"+productName+"' is consumed after subscribing to pool with ProductId '"+productId+"'.");

			// assert the dates match
			Calendar dayBeforeEndDate = (Calendar) entitlementCert.validityNotAfter.clone(); dayBeforeEndDate.add(Calendar.DATE, -1);
//			Calendar dayBeforeStartDate = (Calendar) entitlementCert.validityNotBefore.clone(); dayBeforeStartDate.add(Calendar.DATE, -1);
			//Assert.assertEquals(productSubscription.endDate, entitlementCert.validityNotAfter, "Consumed ProductSubscription Expires on the same end date as the given entitlement: "+entitlementCert);
			Assert.assertTrue(productSubscription.endDate.before(entitlementCert.validityNotAfter) && productSubscription.endDate.after(dayBeforeEndDate), "Consumed ProductSubscription Expires on the same end date as the new entitlement: "+entitlementCert);
			Assert.assertTrue(productSubscription.startDate.before(entitlementCert.validityNotBefore), "Consumed ProductSubscription Began before the validityNotBefore date of the new entitlement: "+entitlementCert);
			Assert.assertEquals(ProductSubscription.formatDateString(productSubscription.endDate), SubscriptionPool.formatDateString(pool.endDate), "Consumed ProductSubscription Expires on the same date as the originating subscription pool: "+pool);

			// assert whether or not the product is installed			
			InstalledProduct installedProduct = clienttasks.findInstalledProductWithMatchingFieldFromList("productName", productName, clienttasks.getCurrentlyInstalledProducts());
			Assert.assertNotNull(installedProduct, "The status of product with ProductName '"+productName+"' is reported in the list of installed products.");

			// assert the status of the installed products
			ProductCert productCert = clienttasks.findProductCertWithMatchingFieldFromList("productName", productName, currentlyInstalledProductCerts);
			if (productCert!=null) {
				Assert.assertEquals(installedProduct.status, "Subscribed", "After subscribing to ProductId '"+productId+"', the status of Installed Product '"+productName+"' is Subscribed since a corresponding product cert was found in "+clienttasks.productCertDir);
				Assert.assertEquals(InstalledProduct.formatDateString(installedProduct.expires), ProductSubscription.formatDateString(productSubscription.endDate), "Installed Product '"+productName+"' expires on the same date as the consumed ProductSubscription: "+productSubscription);
				Assert.assertEquals(installedProduct.subscription, productSubscription.serialNumber, "Installed Product '"+productName+"' subscription matches the serialNumber of the consumed ProductSubscription: "+productSubscription);
			} else {
				Assert.assertEquals(installedProduct.status, "Not Installed", "The status of Entitled Product '"+productName+"' is Not Installed since a corresponding product cert was not found in "+clienttasks.productCertDir);
			}
		}
	}
	
	
	@Test(	description="subscription-manager-cli: autosubscribe consumer and verify expected subscription pool product id are consumed",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AutoSubscribeToExpectedSubscriptionPoolProductId_Test() throws JSONException {
		// get the expected subscriptionPoolProductIdData
		List<List<Object>> subscriptionPoolProductIdData = getSubscriptionPoolProductIdDataAsListOfLists();

		// before testing, make sure all the expected subscriptionPoolProductId are available
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
		List<SubscriptionPool> availableSubscriptionPoolsBeforeAutosubscribe = clienttasks.getCurrentlyAvailableSubscriptionPools();
		for (List<Object> row : subscriptionPoolProductIdData) {
			String subscriptionPoolProductId = (String)row.get(0);
			SubscriptionPool subscriptionPool = clienttasks.findSubscriptionPoolWithMatchingFieldFromList("productId", subscriptionPoolProductId, availableSubscriptionPoolsBeforeAutosubscribe);
			Assert.assertNotNull(subscriptionPool, "Expecting SubscriptionPool with ProductId '"+subscriptionPoolProductId+"' to be available to '"+clientusername+"' before testing register with autosubscribe.");
		}
		
		// register with autosubscribe
		clienttasks.unregister();
		SSHCommandResult sshCommandResult = clienttasks.register(clientusername, clientpassword, null, null, null, Boolean.TRUE, null);

		/* Example Stdout: 
			e1aef738-5d03-4a8a-9c87-7a2652e110a8 rh-alpha-qa-105
			Bind Product  Red Hat Enterprise Linux High Availability (for RHEL 6 Entitlement) 407
			Bind Product  Red Hat Enterprise Linux Scalable File System (for RHEL 6 Entitlement) 410
			Bind Product  Red Hat Enterprise Linux Resilient Storage (for RHEL 6 Entitlement) 409
			Bind Product  Red Hat Enterprise Linux Load Balancer (for RHEL 6 Entitlement) 408
			Bind Product  Red Hat Enterprise Linux 6 Entitlement Alpha 406
		*/

		// get the state of affairs after having registered with autosubscribe
		List<SubscriptionPool> availableSubscriptionPoolsAfterAutosubscribe = clienttasks.getCurrentlyAvailableSubscriptionPools();
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		
		// loop through the subscriptionPoolProductIdData and verify...
		for (List<Object> row : subscriptionPoolProductIdData) {
			String subscriptionPoolProductId = (String)row.get(0);
			String[] entitledProductNames = (String[])row.get(1);
			
			// assert that the subscriptionPoolProductId has been subscribed to...
			
			// assert that subscriptionPoolProductId is not available
			SubscriptionPool subscriptionPool = clienttasks.findSubscriptionPoolWithMatchingFieldFromList("productId", subscriptionPoolProductId, availableSubscriptionPoolsAfterAutosubscribe);
			if (subscriptionPool!=null) {
				String entitledProductNamesAsString = "";
				for (String entitledProductName : entitledProductNames) entitledProductNamesAsString += entitledProductName+", ";entitledProductNamesAsString = entitledProductNamesAsString.replaceFirst("(?s), (?!.*?, )",""); // this will replaceLast ", " with ""
				log.warning("SubscriptionPool with ProductId '"+subscriptionPoolProductId+"' is still available after registering with autosubscribe.");
				log.warning("The probable cause is that the products we expected to be installed, '"+entitledProductNamesAsString+"', are probably not installed and therefore autosubscribing to '"+subscriptionPoolProductId+"' was not performed.");
			}
			Assert.assertNull(subscriptionPool, "SubscriptionPool with ProductId '"+subscriptionPoolProductId+"' is NOT available after registering with autosubscribe.");

			for (String entitledProductName : entitledProductNames) {
				// assert that the sshCommandResult from register indicates the entitledProductName was subscribed
//DELETEME ALPHA				Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "^Bind Product  "+entitledProductName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)"), "Expected ProductName '"+entitledProductName+"' was reported as autosubscribed/bound in the output from register with autotosubscribe.");
				//Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "^Subscribed to Products:", "register with autotosubscribe appears to have subscribed to something");
				Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "^\\s+"+entitledProductName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)"), "Expected ProductName '"+entitledProductName+"' was reported as autosubscribed in the output from register with autotosubscribe.");

				// assert that the entitledProductName is consumed
				ProductSubscription productSubscription = clienttasks.findProductSubscriptionWithMatchingFieldFromList("productName", entitledProductName, consumedProductSubscriptions);
				Assert.assertNotNull(productSubscription, "Expected ProductSubscription with ProductName '"+entitledProductName+"' is consumed after registering with autosubscribe.");
	
				// assert that the entitledProductName is installed and subscribed
				InstalledProduct installedProduct = clienttasks.findInstalledProductWithMatchingFieldFromList("productName", entitledProductName, installedProducts);
				Assert.assertNotNull(installedProduct, "The status of expected product with ProductName '"+entitledProductName+"' is reported in the list of installed products.");
				Assert.assertEquals(installedProduct.status, "Subscribed", "After registering with autosubscribe, the status of Installed Product '"+entitledProductName+"' is Subscribed.");
			}
		}
		
		// finally assert that no extraneous subscription pools were subscribed to
		for (SubscriptionPool subscriptionPoolBeforeAutosubscribe : availableSubscriptionPoolsBeforeAutosubscribe) {
			if (!availableSubscriptionPoolsAfterAutosubscribe.contains(subscriptionPoolBeforeAutosubscribe)) {
				boolean subscribedPoolWasExpected = false;
				for (List<Object> row : subscriptionPoolProductIdData) {
					String subscriptionPoolProductId = (String)row.get(0);
					if (subscriptionPoolBeforeAutosubscribe.productId.equals(subscriptionPoolProductId)) {
						subscribedPoolWasExpected = true;
						break;
					}
				}
				if (!subscribedPoolWasExpected) Assert.fail("Did NOT expect pool '"+subscriptionPoolBeforeAutosubscribe+"' to be removed from the available list following a register with autosubscribe.");
			}
		}
		Assert.assertTrue(true,"No pools were unexpectedly subscribed to following a register with autosubscribe.");
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using product ID",
			enabled=false,	// Subscribing to a Subscription Pool using --product Id has been removed in subscription-manager-0.71-1.el6.i686.
			groups={"blockedByBug-584137"},
			dataProvider="getAvailableSubscriptionPoolsData")
	@ImplementsNitrateTest(caseId=41680)
	public void SubscribeToValidSubscriptionsByProductID_Test(SubscriptionPool pool){
//		sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using product ID",
			groups={"blockedByBug-584137"},
			enabled=false)
	public void SubscribeToASingleEntitlementByProductID_Test(){
		clienttasks.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		SubscriptionPool MCT0696 = new SubscriptionPool("MCT0696", "696");
		MCT0696.addProductID("Red Hat Directory Server");
		clienttasks.subscribeToSubscriptionPoolUsingProductId(MCT0696);
		//this.refreshSubscriptions();
		for (ProductSubscription pid:MCT0696.associatedProductIDs){
			Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().contains(pid),
					"ProductID '"+pid.productName+"' consumed from Pool '"+MCT0696.subscriptionName+"'");
		}
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using pool ID",
			groups={"blockedByBug-584137"},
			enabled=true,
			dataProvider="getAvailableSubscriptionPoolsData")
	@ImplementsNitrateTest(caseId=41686)
	public void SubscribeToValidSubscriptionsByPoolID_Test(SubscriptionPool pool){
// non-dataProvided test procedure
//		sm.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to each available subscription pool using pool ID",
			groups={"blockedByBug-584137"},
			dataProvider="getGoodRegistrationData")
	@ImplementsNitrateTest(caseId=41686)
	public void SubscribeConsumerToEachAvailableSubscriptionPoolUsingPoolId_Test(String username, String password){
		clienttasks.unregister();
		clienttasks.register(username, password, ConsumerType.system, null, null, Boolean.FALSE, Boolean.FALSE);
		clienttasks.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using registration token",
			groups={"blockedByBug-584137"},
			enabled=false)
	@ImplementsNitrateTest(caseId=41681)
	public void SubscribeToRegToken_Test(){
		clienttasks.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.subscribeToRegToken(regtoken);
	}
	
	
	@Test(	description="Subscribed for Already subscribed Entitlement.",
			groups={"blockedByBug-584137"},
			dataProvider="getAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41897)
	public void AttemptToSubscribeToAnAlreadySubscribedPool_Test(SubscriptionPool pool){
// non-dataProvided test procedure
//		//sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
//		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
//		for(SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
//			clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
//			clienttasks.subscribeToProduct(pool.subscriptionName);
//		}
		clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
		SSHCommandResult result = clienttasks.subscribe_(pool.poolId,null,null,null,null);
		Assert.assertEquals(result.getStdout().trim(), "This consumer is already subscribed to the product matching pool with id '"+pool.poolId+"'",
				"subscribe command returns proper message when already subscribed to the requested pool");
	}


	@Test(	description="subscription-manager Yum plugin: enable/disable",
			groups={"EnableDisableYumRepoAndVerifyContentAvailable_Test"},
			dataProvider="getAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41696)
	public void EnableDisableYumRepoAndVerifyContentAvailable_Test(SubscriptionPool pool) {

		log.info("Before beginning this test, we will stop the rhsmcertd so that it does not interfere with this test..");
		clienttasks.stop_rhsmcertd();
		
		// Edit /etc/yum/pluginconf.d/rhsmplugin.conf and ensure that the enabled directive is set to 1
		log.info("Making sure that the rhsm plugin conf file '"+clienttasks.rhsmPluginConfFile+"' is enabled with enabled=1..");
		clienttasks.updateConfFileParameter(clienttasks.rhsmPluginConfFile, "enabled", "1");
		
		log.info("Subscribe to the pool and start testing that yum repolist reports the expected repo id/labels...");
		clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
		
		// 1. Run a 'yum repolist' and get a list of all of the available repositories corresponding to your entitled products
		// 1. Repolist contains repositories corresponding to your entitled products
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.getCurrentEntitlementCertFiles("-t").get(0)); // newest entitlement cert
		ArrayList<String> repolist = clienttasks.getYumRepolist("enabled");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (contentNamespace.enabled.equals("1")) {
				Assert.assertTrue(repolist.contains(contentNamespace.label),
					"Yum repolist enabled includes enabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
			} else {
				Assert.assertFalse(repolist.contains(contentNamespace.label),
					"Yum repolist enabled excludes disabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
			}
		}
		repolist = clienttasks.getYumRepolist("disabled");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (contentNamespace.enabled.equals("1")) {
				Assert.assertFalse(repolist.contains(contentNamespace.label),
					"Yum repolist disabled excludes enabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
			} else {
				Assert.assertTrue(repolist.contains(contentNamespace.label),
					"Yum repolist disabled includes disabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
			}
		}
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertTrue(repolist.contains(contentNamespace.label),
				"Yum repolist all includes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
		}

		log.info("Unsubscribe from the pool and verify that yum repolist no longer reports the expected repo id/labels...");
		clienttasks.unsubscribeFromSerialNumber(entitlementCert.serialNumber);
		
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertFalse(repolist.contains(contentNamespace.label),
				"Yum repolist all excludes repo id/label '"+contentNamespace.label+"' after having unsubscribed from Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' enabled.");
		}
	
		// Edit /etc/yum/pluginconf.d/rhsmplugin.conf and ensure that the enabled directive is set to 0
		log.info("Now we will disable the rhsm plugin conf file '"+clienttasks.rhsmPluginConfFile+"' with enabled=0..");
		clienttasks.updateConfFileParameter(clienttasks.rhsmPluginConfFile, "enabled", "0");
		
		log.info("Again let's subscribe to the same pool and verify that yum repolist does NOT report any of the entitled repo id/labels since the plugin has been disabled...");
		clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
		
		// 2. Run a 'yum repolist' and get a list of all of the available repositories corresponding to your entitled products
		// 2. Repolist does not contain repositories corresponding to your entitled products
		entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.getCurrentEntitlementCertFiles("-t").get(0));
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertFalse(repolist.contains(contentNamespace.label),
				"Yum repolist all excludes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' disabled.");
		}
		
		log.info("Now we will restart the rhsmcertd and expect the repo list to be updated");
		int minutes = 2;
		clienttasks.restart_rhsmcertd(minutes, false);
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertTrue(repolist.contains(contentNamespace.label),
				"Yum repolist all now includes repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' disabled and run an update with rhsmcertd.");
		}
		
		log.info("Now we will unsubscribe from the pool and verify that yum repolist continues to report the repo id/labels until the next refresh from the rhsmcertd runs...");
		clienttasks.unsubscribeFromSerialNumber(entitlementCert.serialNumber);
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertTrue(repolist.contains(contentNamespace.label),
				"Yum repolist all still includes repo id/label '"+contentNamespace.label+"' despite having unsubscribed from Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' disabled.");
		}
		log.info("Wait for the next refresh by rhsmcertd to remove the repos from the yum repo file '"+clienttasks.redhatRepoFile+"'...");
		sleep(minutes*60*1000);
		repolist = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Assert.assertFalse(repolist.contains(contentNamespace.label),
				"Yum repolist all finally excludes repo id/label '"+contentNamespace.label+"' after having unsubscribed from Subscription ProductId '"+entitlementCert.productId+"' with the rhsmPluginConfFile '"+clienttasks.rhsmPluginConfFile+"' disabled AND waiting for the next refresh by rhsmcertd.");
		}
	}
	@AfterGroups(value="EnableDisableYumRepoAndVerifyContentAvailable_Test", alwaysRun=true)
	protected void teardownAfterEnableDisableYumRepoAndVerifyContentAvailable_Test() {
		clienttasks.updateConfFileParameter(clienttasks.rhsmPluginConfFile, "enabled", "1");
		clienttasks.restart_rhsmcertd(Integer.valueOf(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "certFrequency")), false);
	}
	

	
	@Test(	description="subscription-manager content flag : Default content flag should enable",
			groups={},
	        enabled=true)
	@ImplementsNitrateTest(caseId=47578)
	public void VerifyYumRepoListsEnabledContent(){
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
		
// DELETEME: Alternative to above procedure is:
//		clienttasks.unregister();
//	    clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
//	    clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools(ConsumerType.system);
//	    List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
//	    Assert.assertTrue(!entitlementCerts.isEmpty(),"After subscribing to all available subscription pools, there must be some entitlements."); // or maybe we should skip when nothing is consumed 
//	    clienttasks.assertEntitlementCertsInYumRepolist(entitlementCerts,true);
	    
		clienttasks.unregister();
	    clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
	    clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools(ConsumerType.system);
	    List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
	    Assert.assertTrue(!entitlementCerts.isEmpty(),"After subscribing to all available subscription pools, there must be some entitlements."); // or maybe we should skip when nothing is consumed 
		ArrayList<String> repolist = clienttasks.getYumRepolist("enabled");
		for (EntitlementCert entitlementCert : entitlementCerts) {
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				if (contentNamespace.enabled.equals("1")) {
					Assert.assertTrue(repolist.contains(contentNamespace.label),
						"Yum repolist enabled includes enabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"'.");
				} else {
					Assert.assertFalse(repolist.contains(contentNamespace.label),
						"Yum repolist enabled excludes disabled repo id/label '"+contentNamespace.label+"' after having subscribed to Subscription ProductId '"+entitlementCert.productId+"'.");
				}
			}
		}
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to multiple/duplicate/bad pools in one call",
			groups={"blockedByBug-622851"},
			enabled=true)
	public void SubscribeToMultipleDuplicateAndBadPools_Test() {
		
		// begin the test with a cleanly registered system
		clienttasks.unregister();
	    clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
	    
		// assemble a list of all the available SubscriptionPool ids with duplicates and bad ids
		List <String> poolIds = new ArrayList<String>();
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			poolIds.add(pool.poolId);
			poolIds.add(pool.poolId); // add a duplicate poolid
		}
		String badPoolId1 = "bad123", badPoolId2 = "bad_POOLID"; 
		poolIds.add(0, badPoolId1); // insert a bad poolid
		poolIds.add(badPoolId2); // append a bad poolid
		
		// subscribe to all pool ids
		log.info("Attempting to subscribe to multiple pools with duplicate and bad pool ids...");
		SSHCommandResult result = clienttasks.subscribe(poolIds, null, null, null, null);
		
		// assert the results
		for (String poolId : poolIds) {
			if (poolId.equals(badPoolId1)) continue; if (poolId.equals(badPoolId2)) continue;
			Assert.assertContainsMatch(result.getStdout(),"^This consumer is already subscribed to the product matching pool with id '"+poolId+"'$","Asserting that already subscribed to pools is noted and skipped during a multiple pool binding.");
		}
		Assert.assertContainsMatch(result.getStdout(),"^No such entitlement pool: "+badPoolId1+"$","Asserting that an invalid pool is noted and skipped during a multiple pool binding.");
		Assert.assertContainsMatch(result.getStdout(),"^No such entitlement pool: "+badPoolId2+"$","Asserting that an invalid pool is noted and skipped during a multiple pool binding.");
		clienttasks.assertNoAvailableSubscriptionPoolsToList("Asserting that no available subscription pools remain after simultaneously subscribing to them all including duplicates and bad pool ids.");
	}
	
	
	@Test(	description="subscription-manager Yum plugin: ensure content can be downloaded/installed/removed",
			groups={},
			dataProvider="getAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41695)
	public void InstallAndRemovePackageAfterSubscribingToPool_Test(SubscriptionPool pool) {
		// original implementation by ssalevan
//		HashMap<String, String[]> pkgList = clienttasks.getPackagesCorrespondingToSubscribedRepos();
//		for(ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()){
//			String pkg = pkgList.get(productSubscription.productName)[0];
//			log.info("Attempting to install first pkg '"+pkg+"' from product subscription: "+productSubscription);
//			log.info("timeout of two minutes for next three commands");
//			RemoteFileTasks.runCommandExpectingNoTracebacks(client,
//					"yum repolist");
//			RemoteFileTasks.runCommandExpectingNoTracebacks(client,
//					"yum install -y "+pkg);
//			RemoteFileTasks.runCommandExpectingNoTracebacks(client,
//					"rpm -q "+pkg);
//		}
	
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		boolean pkgInstalled = false;
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (contentNamespace.enabled.equals("1")) {
				String repoLabel = contentNamespace.label;

				// find an available package that is uniquely provided by repo
				String pkg = clienttasks.findUniqueAvailablePackageFromRepo(repoLabel);
				if (pkg==null) {
					log.warning("Could NOT find a unique available package from repo '"+repoLabel+"' after subscribing to SubscriptionPool: "+pool);
					continue;
				}
				
				// install the package and assert that it is successfully installed
				clienttasks.installPackageUsingYumFromRepo(pkg, repoLabel); pkgInstalled = true;
				
				// now remove the package
				clienttasks.removePackageUsingYum(pkg);
			}
		}
		Assert.assertTrue(pkgInstalled,"At least one package was found and installed from entitled repos after subscribing to SubscriptionPool: "+pool);
	}
	
	
	@Test(	description="rhsmcertd: change certFrequency",
			dataProvider="getCertFrequencyData",
			groups={"blockedByBug-617703"},
			enabled=true)
	@ImplementsNitrateTest(caseId=41692)
	public void rhsmcertdChangeCertFrequency_Test(int minutes) {
		String errorMsg = "Either the consumer is not registered with candlepin or the certificates are corrupted. Certificate updation using daemon failed.";
		errorMsg = "Either the consumer is not registered or the certificates are corrupted. Certificate update using daemon failed.";
		
		log.info("First test with an unregistered user and verify that the rhsmcertd actually fails since it cannot self-identify itself to the candlepin server.");
		clienttasks.unregister();
		clienttasks.restart_rhsmcertd(minutes, false); sleep(10000); // allow 10sec for the initial update
		log.info("Appending a marker in the '"+clienttasks.rhsmcertdLogFile+"' so we can assert that the certificates are being updated every '"+minutes+"' minutes");
		String marker = "Testing rhsm.conf certFrequency="+minutes+" when unregistered..."; // https://tcms.engineering.redhat.com/case/41692/
		RemoteFileTasks.runCommandAndAssert(client,"echo \""+marker+"\" >> "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),marker,null);
		sleep(minutes*60*1000);	// give the rhsmcertd a chance to check in with the candlepin server and update the certs
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),"update failed \\(\\d+\\), retry in "+minutes+" minutes",null);
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmLogFile,Integer.valueOf(0),errorMsg,null);
		
		
		log.info("Now test with a registered user whose identity is corrupt and verify that the rhsmcertd actually fails since it cannot self-identify itself to the candlepin server.");
		String consumerid = clienttasks.getCurrentConsumerId(clienttasks.register(clientusername, clientpassword, null, null, null, null, null));
		log.info("Corrupting the identity cert by borking its content...");
		RemoteFileTasks.runCommandAndAssert(client, "openssl x509 -noout -text -in "+clienttasks.consumerCertFile+" > /tmp/stdout; mv /tmp/stdout -f "+clienttasks.consumerCertFile, 0);
		clienttasks.restart_rhsmcertd(minutes, false); sleep(10000); // allow 10sec for the initial update
		log.info("Appending a marker in the '"+clienttasks.rhsmcertdLogFile+"' so we can assert that the certificates are being updated every '"+minutes+"' minutes");
		marker = "Testing rhsm.conf certFrequency="+minutes+" when identity is corrupted...";
		RemoteFileTasks.runCommandAndAssert(client,"echo \""+marker+"\" >> "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),marker,null);
		sleep(minutes*60*1000);	// give the rhsmcertd a chance to check in with the candlepin server and update the certs
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),"update failed \\(\\d+\\), retry in "+minutes+" minutes",null);
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmLogFile,Integer.valueOf(0),errorMsg,null);

		
		log.info("Finally test with a registered user and verify that the rhsmcertd succeeds because he can identify himself to the candlepin server.");
	    clienttasks.register(clientusername, clientpassword, null, null, consumerid, null, Boolean.TRUE);
		clienttasks.restart_rhsmcertd(minutes, false); sleep(10000); // allow 10sec for the initial update
		log.info("Appending a marker in the '"+clienttasks.rhsmcertdLogFile+"' so we can assert that the certificates are being updated every '"+minutes+"' minutes");
		marker = "Testing rhsm.conf certFrequency="+minutes+" when registered..."; // https://tcms.engineering.redhat.com/case/41692/
		RemoteFileTasks.runCommandAndAssert(client,"echo \""+marker+"\" >> "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),marker,null);
		sleep(minutes*60*1000);	// give the rhsmcertd a chance to check in with the candlepin server and update the certs
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),"certificates updated",null);

		/* tail -f /var/log/rhsm/rhsm.log
		 * 2010-09-10 12:05:06,338 [ERROR] main() @certmgr.py:75 - Either the consumer is not registered with candlepin or the certificates are corrupted. Certificate updation using daemon failed.
		 */
		
		/* tail -f /var/log/rhsm/rhsmcertd.log
		 * Fri Sep 10 11:59:50 2010: started: interval = 1 minutes
		 * Fri Sep 10 11:59:51 2010: update failed (255), retry in 1 minutes
		 * testing rhsm.conf certFrequency=1 when unregistered.
		 * Fri Sep 10 12:00:51 2010: update failed (255), retry in 1 minutes
		 * Fri Sep 10 12:01:04 2010: started: interval = 1 minutes
		 * Fri Sep 10 12:01:05 2010: certificates updated
		 * testing rhsm.conf certFrequency=1 when registered.
		 * Fri Sep 10 12:02:05 2010: certificates updated
		*/
	}
	
	
	@Test(	description="rhsmcertd: ensure certificates synchronize",
			groups={"blockedByBug-617703"},
			enabled=true)
	@ImplementsNitrateTest(caseId=41694)
	public void rhsmcertdEnsureCertificatesSynchronize_Test(){
//FIXME Replacing ssalevan's original implementation of this test... 10/5/2010 jsefler
//		clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools(ConsumerType.system);
//		//SubscribeToASingleEntitlementByProductID_Test();
//		client.runCommandAndWait("rm -rf "+clienttasks.entitlementCertDir+"/*");
//		client.runCommandAndWait("rm -rf "+clienttasks.productCertDir+"/*");
//		//certFrequency_Test(1);
//		clienttasks.restart_rhsmcertd(1,true);
////		client.runCommandAndWait("cat /dev/null > "+rhsmcertdLogFile);
////		//sshCommandRunner.runCommandAndWait("rm -f "+rhsmcertdLogFile);
////		//sshCommandRunner.runCommandAndWait("/etc/init.d/rhsmcertd restart");
////		this.sleep(70*1000);
////		
////		Assert.assertEquals(RemoteFileTasks.grepFile(client,
////				rhsmcertdLogFile,
////				"certificates updated"),
////				0,
////				"rhsmcertd reports that certificates have been updated");
//		
//		//verify that PEM files are present in all certificate directories
//		RemoteFileTasks.runCommandAndAssert(client, "ls "+clienttasks.entitlementCertDir+" | grep pem", 0, "pem", null);
//		RemoteFileTasks.runCommandAndAssert(client, "ls "+clienttasks.entitlementCertDir+"/product | grep pem", 0, "pem", null);
//		// this directory will only be populated if you upload ur own license, not while working w/ candlepin
//		/*RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "ls /etc/pki/product", 0, "pem", null);*/
		
		// start with a cleanly unregistered system
		clienttasks.unregister();
		
		// register a clean user
	    clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
	    
	    // subscribe to all the available pools
	    clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools(ConsumerType.system);
	    
	    // get all of the current entitlement product certs and remember them
	    List<File> entitlementCertFiles = clienttasks.getCurrentEntitlementCertFiles();
	    
	    // delete all of the entitlement cert files
	    client.runCommandAndWait("rm -rf "+clienttasks.entitlementCertDir+"/*");
	    Assert.assertEquals(clienttasks.getCurrentEntitlementCertFiles().size(), 0,
	    		"All the entitlement product certs have been deleted.");
		
	    // restart the rhsmcertd to run every 1 minute and wait for a refresh
		clienttasks.restart_rhsmcertd(1, true);
		
		// assert that rhsmcertd has refreshed the entitled product certs back to the original
	    Assert.assertEquals(clienttasks.getCurrentEntitlementCertFiles(), entitlementCertFiles,
	    		"All the deleted entitlement product certs have been re-synchronized by rhsm cert deamon.");
	}
	
	
	// Configuration Methods ***********************************************************************
	
	
	
	// Protected Methods ***********************************************************************
	

	
	// Data Providers ***********************************************************************

	@DataProvider(name="getCertFrequencyData")
	public Object[][] getCertFrequencyDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getCertFrequencyDataAsListOfLists());
	}
	protected List<List<Object>> getCertFrequencyDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		// int minutes
		ll.add(Arrays.asList(new Object[]{2}));
		ll.add(Arrays.asList(new Object[]{1}));
		
		return ll;
	}
}
