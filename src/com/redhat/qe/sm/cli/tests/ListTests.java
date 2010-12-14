package com.redhat.qe.sm.cli.tests;

import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.InstalledProduct;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 *  @author ssalevan
 *  @author jsefler
 *
 */
@Test(groups={"list"})
public class ListTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************
	
	@Test(	description="subscription-manager-cli: list available subscriptions (when not consuming)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=41678)
	public void EnsureAvailableSubscriptionsListed_Test() {
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
		String availableSubscriptionPools = clienttasks.listAvailableSubscriptionPools().getStdout();
		Assert.assertContainsMatch(availableSubscriptionPools, "Available Subscriptions","" +
				"Available Subscriptions are listed for '"+clientusername+"' to consume.");
		Assert.assertContainsNoMatch(availableSubscriptionPools, "No Available subscription pools to list",
				"Available Subscriptions are listed for '"+clientusername+"' to consume.");

		log.warning("These manual TCMS instructions are not really achievable in this automated test...");
		log.warning(" * List produced matches the known data contained on the Candlepin server");
		log.warning(" * Confirm that the marketing names match.. see prereq link https://engineering.redhat.com/trac/IntegratedMgmtQE/wiki/sm-prerequisites");
		log.warning(" * Match the marketing names w/ https://www.redhat.com/products/");
	}
	
	
	@Test(	description="subscription-manager-cli: list available subscriptions",
			groups={},
			dataProvider="getSubscriptionPoolProductIdData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41678)
	public void EnsureAvailableSubscriptionsListed_Test(String productId, String[] bundledProductNames) {
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
		
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", productId, clienttasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNotNull(pool, "Expected SubscriptionPool with ProductId '"+productId+"' is available for subscribing: "+pool);
	}
	
	
	@Test(	description="subscription-manager-cli: list consumed entitlements (when not consuming)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=41679)
	public void EnsureConsumedEntitlementsListed_Test() {
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
		String consumedProductSubscription = clienttasks.listConsumedProductSubscriptions().getStdout();
		Assert.assertContainsMatch(consumedProductSubscription, "No Consumed subscription pools to list",
				"No Consumed subscription pools listed for '"+clientusername+"' after registering (without autosubscribe).");
	}
	
	
	@Test(	description="subscription-manager-cli: list consumed entitlements",
			groups={},
			dataProvider="getSubscriptionPoolProductIdData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41679)
	public void EnsureConsumedEntitlementsListed_Test(String productId, String[] bundledProductNames) {
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
		
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", productId, clienttasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNotNull(pool, "Expected SubscriptionPool with ProductId '"+productId+"' is available for subscribing: "+pool);
		EntitlementCert  entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool));
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertTrue(!consumedProductSubscriptions.isEmpty(),"The list of Consumed Product Subscription is NOT empty after subscribing to a pool with ProductId '"+productId+"'.");
		for (ProductSubscription productSubscription : consumedProductSubscriptions) {
			Assert.assertEquals(productSubscription.serialNumber, entitlementCert.serialNumber,
					"SerialNumber of Consumed Product Subscription matches the serial number from the current entitlement certificate.");
		}	
	}
	
	
	@Test(	description="subscription-manager-cli: list installed products",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void EnsureInstalledProductsListed_Test() {
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null);

		List <ProductCert> productCerts = clienttasks.getCurrentProductCerts();
		String installedProductsAsString = clienttasks.listInstalledProducts().getStdout();
		//List <InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		List <InstalledProduct> installedProducts = InstalledProduct.parse(installedProductsAsString);

		// assert some stdout
		if (installedProducts.size()>0) {
			Assert.assertContainsMatch(installedProductsAsString, "Installed Product Status");
		}
		
		// assert the number of installed product matches the product certs installed
		Assert.assertEquals(installedProducts.size(), productCerts.size(), "A single product is reported as installed for each product cert found in "+clienttasks.productCertDir);

		// assert that each of the installed product certs are listed in installedProducts as "Not Subscribed"
		for (InstalledProduct installedProduct : installedProducts) {
			boolean foundInstalledProductMatchingProductCert=false;
			for (ProductCert productCert : productCerts) {
				if (installedProduct.productName.equals(productCert.productName)) {
					foundInstalledProductMatchingProductCert = true;
					break;
				}
			}
			Assert.assertTrue(foundInstalledProductMatchingProductCert, "The installed product cert for '"+installedProduct.productName+"' is reported by subscription-manager as installed.");
			Assert.assertEquals(installedProduct.status, "Not Subscribed", "A newly registered system should not be subscribed to installed product '"+installedProduct.productName+"'.");
		}

	}
	
	
	@Test(	description="subscription-manager: ensure list [--installed] produce the same results",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void EnsureListAndListInstalledAreTheSame_Test() {
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null);

		// assert same results when no subscribed to anything...
		log.info("assert list [--installed] produce same results when not subscribed to anything...");
		SSHCommandResult listResult = clienttasks.list_(null, null, null, null);
		SSHCommandResult listInstalledResult = clienttasks.list_(null, null, null, Boolean.TRUE);
		
		Assert.assertEquals(listResult.getStdout(), listInstalledResult.getStdout(), "'list' and 'list --installed' produce the same stdOut results.");
		Assert.assertEquals(listResult.getStderr(), listInstalledResult.getStderr(), "'list' and 'list --installed' produce the same stdErr results.");
		Assert.assertEquals(listResult.getExitCode(), listInstalledResult.getExitCode(), "'list' and 'list --installed' produce the same exitCode results.");
		
		
		// assert same results when subscribed to something...
		log.info("assert list [--installed] produce same results when subscribed to something...");
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribeToSubscriptionPool(pool);
		listResult = clienttasks.list_(null, null, null, null);
		listInstalledResult = clienttasks.list_(null, null, null, Boolean.TRUE);
		
		Assert.assertEquals(listResult.getStdout(), listInstalledResult.getStdout(), "'list' and 'list --installed' produce the same stdOut results.");
		Assert.assertEquals(listResult.getStderr(), listInstalledResult.getStderr(), "'list' and 'list --installed' produce the same stdErr results.");
		Assert.assertEquals(listResult.getExitCode(), listInstalledResult.getExitCode(), "'list' and 'list --installed' produce the same exitCode results.");
	}
	

	@Test(	description="subscription-manager: list of consumed entitlements should display consumed product marketing name",
			groups={},
			enabled=false)
	@ImplementsNitrateTest(caseId=48092, fromPlan=2481)
	public void EnsureListConsumedMatchesProductsListedInTheEntitlementCerts_Test() {
		/*
		Actions:

		Subscribe to an entitlement pool

		Execute following command on console to obtain a list of your consumed entitlements

		subscription-manager-cli list –consumed
			
		Expected Results:

		    * List of consumed entitlements should contain Red Hat marketing names
		    * No Error should record on console or rhsm.log
		    * The list of consumed products matches the products listed in the entitlement cert

		 */
		
		// TODO IMPLEMENTME
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null);
	}
	
	
	@Test(	description="subscription-manager-cli: RHEL Personal should be the only available subscription to a consumer registered as type person",
			groups={"EnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void EnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test() {
		String rhelPersonalProductId = getProperty("sm.rhpersonal.productId", "");
		if (rhelPersonalProductId.equals("")) throw new SkipException("This testcase requires specification of a RHPERSONAL_PRODUCTID.");
		
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, ConsumerType.person, null, null, null, null);
		

		// assert that RHEL Personal is available to this person consumer
		List<SubscriptionPool> subscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool rhelPersonalPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", rhelPersonalProductId, subscriptionPools);
		Assert.assertNotNull(rhelPersonalPool,"RHEL Personal ProductId '"+rhelPersonalProductId+"' is available to this consumer registered as type person");
		
		// assert that RHEL Personal is the only available pool to this person consumer
		for (SubscriptionPool subscriptionPool : subscriptionPools) {
			Assert.assertEquals(subscriptionPool.productId,rhelPersonalPool.productId, "RHEL Personal ProductId '"+rhelPersonalProductId+"' is the ONLY product consumable from an available subscription pool to this consumer registered as type person");
		}
	}
	@AfterGroups(groups={}, value="EnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test", alwaysRun=true)
	public void teardownAfterEnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test() {
		if (clienttasks!=null) clienttasks.unregister_();
	}
	
	
	@Test(	description="subscription-manager-cli: RHEL Personal should not be an available subscription to a consumer registered as type system",
			groups={"EnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void EnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test() {
		String rhelPersonalProductId = getProperty("sm.rhpersonal.productId", "");
		if (rhelPersonalProductId.equals("")) throw new SkipException("This testcase requires specification of a RHPERSONAL_PRODUCTID.");

		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, ConsumerType.system, null, null, null, null);
		SubscriptionPool rhelPersonalPool = null;
		
		// assert that RHEL Personal *is not* included in --available subscription pools
		rhelPersonalPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", rhelPersonalProductId, clienttasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNull(rhelPersonalPool,"RHEL ProductId '"+rhelPersonalProductId+"' is NOT available to this consumer from any available subscription pool when registered as type system");
		
		// also assert that RHEL Personal *is* included in --all --available subscription pools
		rhelPersonalPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", rhelPersonalProductId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(rhelPersonalPool,"RHEL ProductId '"+rhelPersonalProductId+"' is included in --all --available subscription pools when registered as type system");
	}
	@AfterGroups(groups={}, value="EnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test", alwaysRun=true)
	public void teardownAfterEnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test() {
		if (clienttasks!=null) clienttasks.unregister_();
	}
	

	
	// Data Providers ***********************************************************************
	
	
}
