package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.BzChecker;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 *REFERENCE MATERIAL:
 * https://tcms.engineering.redhat.com/case/55702/
 * https://tcms.engineering.redhat.com/case/55718/
 * https://engineering.redhat.com/trac/IntegratedMgmtQE/wiki/RH-Personal_dev_testplan
 * https://engineering.redhat.com/trac/Entitlement/wiki/RHPersonalDevTools
Data prep
==========================================================
(in candlepin/client/ruby - assuming that the cp_product_utils product data has been imported)

./cpc create_owner john
./cpc create_user 10 john password   (10 happened to be the id returned from creating the john owner)
./cpc create_subscription 10 RH09XYU34  (defaults to a quantity of 1)
./cpc refresh_pools john

RHSM
===========================================================
# Simulating a person accepting RHEL Personal
sudo ./subscription-manager-cli register --username=john --password=password --type=person
sudo ./subscription-manager-cli list --available     # (Should see RHEL Personal)
sudo ./subscription-manager-cli subscribe --pool=13  # (or whatever the pool id is for RHEL Personal)

# AT THIS POINT YOU WILL NOT HAVE ANY ENTITLEMENT CERTS
# THIS DOES NOT MATTER, AS THIS IS NOT HOW A RHEL PERSONAL CUSTOMER WILL ACTUALLY CONSUME THIS ENTITLEMENT

# Stash the consumer certs - this really just simulates using a different machine
sudo mv /etc/pki/consumer/*.pem /tmp

# Now register a system consumer under john's username
sudo ./subscription-manager-cli register --username=john --password=password
sudo ./subscription-manager-cli list --available    # (Should see RHEL Personal Bits - this product actually should create a real entitlement cert and allow this machine access to the actual content, hence the silly name)

sudo ./subscription-manager-cli subscribe --pool=14  # (RHEL Personal Bits pool)
sudo ./subscription-manager-cli list                 # (This should actually show entitlement cert information)

I don't think that we have valid content data currently baked into the certs, so I don't think that you can use this to access protected yum repos yet.



EMAIL FROM dgoodwin@redhat.com:
Sub-pools are something we refer to related to Red Hat Personal.

Customers register as what we call "person consumers" with candlepin
through this Kingpin application.

They will then see a Red Hat Personal subscription and be able to bind
(request an entitlement). This is only for "person consumers", not systems.

Once granted, this entitlement results in a sub-pool being created (of
unlimited size) which they can then subscribe their systems to. It is
tied to an entitlement that created it.

When the person consumer's entitlement is removed, that sub-pool needs
to be cleaned up, including all outstanding entitlements it has given out.

Hope that was clear, it is certainly not the simplest thing in the world. :)

Devan


EMAIL FROM jharris@redhat.com
Subpools are currently something that is currently pretty specific to RH Personal,
but I will try and explain it generally first...

In most cases, a pool is a 1-to-1 match with a subscription - so a subscription with 
quantity 20 for "Super Cool Linux" gives you a pool with the same quantity and product(s).  
There are special cases where the act of consuming an entitlement from one pool actually 
spins off a new pool as a result.  A case for this might be "Developer Tools" where a 
subscription for a 10 person license is purchased (quantity 10), and when someone consumes 
an entitlement from that pool, a new sub-pool is created specifically for that user.  
Each system that this user installs the product on pulls from this sub-pool, and when this 
user gives up his/her seat (unbinds from the original pool), the sub-pool and all of its 
entitlements are removed, which means that any systems that that "Developer Tools" installed 
by this user are no longer in compliance.

We are using this same construct to model the RH Personal case, where the "person" consumes 
an entitlement for RHEL Personal, and any systems that want to install RHEL on are entitled 
off of the created sub-pool.

I'm not really sure if this helps any, but here is the original design doc:  
https://engineering.redhat.com/trac/Entitlement/wiki/RHPersonalDevTools

 - Justin
 */


@Test(groups={"overconsumption"})
public class OverconsumptionTests extends SubscriptionManagerCLITestScript{
	protected List<String> systemConsumerIds = new ArrayList<String>();
	SubscriptionPool testPool = null;
	
	
//	protected String personConsumerId = null;
//	protected int multipleSystems = 4;	// multiple (unlimited)  // multipleSystems is a count of systems that will be used to subscribe to the sub-pool.  Theoretically this number should be very very large to test the unlimited quantity
//	
//	protected String username = getProperty("sm.rhpersonal.username", "");
//	protected String password = getProperty("sm.rhpersonal.password", "");
//	protected String anotherUsername = null;	// under the same ownerkey as username
//	protected String anotherPassword = null;
//	protected String personSubscriptionName = null;
//	protected String systemSubscriptionQuantity = getProperty("sm.rhpersonal.subproductQuantity", "unlimited");


	
	
	// Test Methods ***********************************************************************
	
	@Test(	description="subscription-manager-cli: Attempt to oversubscribe to a pool",
			groups={},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void BasicAttemptToOversubscribe_Test() {
	
		// register the first consumer
		systemConsumerIds.clear();
		systemConsumerIds.add(client1tasks.getCurrentConsumerId(client1tasks.register(client1username, client1password, null, null, null, null, Boolean.TRUE, null, null, null)));
		
		// find the pool with the least quantity available
		int quantity = 10000000;
		for (SubscriptionPool pool: client1tasks.getCurrentlyAvailableSubscriptionPools()) {
			int pool_quantity = Integer.valueOf(pool.quantity);
			if (pool_quantity < quantity && pool_quantity > 0) {
				quantity = pool_quantity;
				testPool = pool;
			}
		}
		Assert.assertNotNull(testPool, "Found an available pool with a positive quantity of available subscriptions.");
		
		// consume each quantity available as a new consumer
		log.info("Now we will register and subscribe new consumers until we exhaust all of the available subscriptions...");
		for (int i=quantity; i>0; i--) {
			// register a new system consumer
			client1tasks.clean(null,null,null);
			systemConsumerIds.add(client1tasks.getCurrentConsumerId(client1tasks.register(client1username, client1password, null, null, null, null, null, null, null, null)));

			// subscribe to the pool
			client1tasks.subscribeToSubscriptionPool(testPool);
		}
		
		log.info("Now we will register and subscribe the final subscriber as an attempt to oversubscribe to original pool: "+testPool);
		client1tasks.clean(null,null,null);
		systemConsumerIds.add(client1tasks.getCurrentConsumerId(client1tasks.register(client1username, client1password, null, null, null, null, null, null, null, null)));
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client1tasks.getCurrentlyAvailableSubscriptionPools());
		//Assert.assertNotNull(pool, "Found the test pool after having consumed all of its available subscriptions.");
		//Assert.assertEquals(pool.quantity, "0", "Asserting the test pool quantity after having consumed all of its available subscriptions.");
		Assert.assertNull(pool, "The test pool after having consumed all of its available subscriptions is no longer available.");

		// now attempt to oversubscribe
		log.info("Now we will attempt to oversubscribe to original pool: "+testPool);
		// No free entitlements are available for the pool with id	'8a9b90882df297d5012df31def5e00bb'
		Assert.assertNull(client1tasks.subscribeToSubscriptionPool(testPool),"No entitlement cert is granted when the pool is already fully subscribed.");
	}
	
	@Test(	description="subscription-manager-cli: Attempt to oversubscribe to a pool",
			groups={"blockedByBug-671195"},
			dependsOnMethods={"BasicAttemptToOversubscribe_Test"},
			enabled=false)
	//@ImplementsTCMS(id="")
	public void ConcurrentAttemptToOversubscribe_Test() {
	
		// reregister the first systemConsumerId and unsubscribe from the test pool
		client1tasks.register(client1username,client1password,null,null,systemConsumerIds.get(0),null, Boolean.TRUE, null, null, null);
		client1tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		
		// assert that the test pool now has 1 quantity available
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", testPool.poolId, client1tasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNotNull(pool, "Found the test pool after having consumed available subscriptions.");
		Assert.assertEquals(pool.quantity, "1", "Asserting the test pool quantity after having consumed almost all of its available subscriptions.");

		// register the first consumer
		systemConsumerIds.clear();
		systemConsumerIds.add(client1tasks.getCurrentConsumerId(client1tasks.register(client1username, client1password, null, null, null, null, Boolean.TRUE, null, null, null)));

	}

	
	
	// TODO Candidates for an automated Test:
	
	
	
	// Configuration Methods ***********************************************************************
	
	
//	@BeforeClass(groups="setup")
//	public void setupBeforeClass() throws IOException, JSONException {
//		// alternative to dependsOnGroups={"RegisterWithUsernameAndPassword_Test"}
//		// This allows us to satisfy a dependency on registrationDataList making TestNG add unwanted Test results.
//		// This also allows us to individually run this Test Class on Hudson.
//		RegisterWithUsernameAndPassword_Test(); // needed to populate registrationDataList
//		
//		// find anotherConsumerUsername under the same owner as consumerUsername
//		RegistrationData registrationDataForSystemUsername = findRegistrationDataMatchingUsername(username);
//		Assert.assertNotNull(registrationDataForSystemUsername, "Found the RegistrationData for username '"+username+"': "+registrationDataForSystemUsername);
//		RegistrationData registrationDataForAnotherSystemUsername = findRegistrationDataMatchingOwnerKeyButNotMatchingUsername(registrationDataForSystemUsername.ownerKey,username);
//		if (registrationDataForAnotherSystemUsername!=null) {
//			anotherUsername = registrationDataForAnotherSystemUsername.username;
//			anotherPassword = registrationDataForAnotherSystemUsername.password;
//		}
//
//	}
//	
//	
//	@BeforeClass(groups={"setup"})
//	public void beforeClassSetup() throws JSONException {
//		if (getPersonProductIds()==null) {
//			throw new SkipException("To enable the RHEL Personal Tests, we need to know the ProductId of a Subscription containing a subpool of personal products.");
//		}
//		
//		// initialize systemSubscriptionQuantity
//		if (!systemSubscriptionQuantity.equalsIgnoreCase("unlimited")) {
//			int quantity = Integer.valueOf(systemSubscriptionQuantity);
//			Assert.assertTrue(quantity>0,"Expecting personal subpool subscription to be available with a positive quantity.");
//			if (multipleSystems>quantity) {
//				multipleSystems = quantity - 1;
//			}
//		}
//
//	}
	
	@AfterClass(groups={"setup"},alwaysRun=true)
	public void unsubscribeAndUnregisterMultipleSystemsAfterClass() {
		if (client2tasks!=null) {
			client2tasks.unsubscribe_(Boolean.TRUE,null, null, null, null);
			client2tasks.unregister_(null, null, null);
		}

		if (client1tasks!=null) {
			
			for (String systemConsumerId : systemConsumerIds) {
				client1tasks.register_(client1username,client1password,null,null,systemConsumerId,null, Boolean.TRUE, null, null, null);
				client1tasks.unsubscribe_(Boolean.TRUE, null, null, null, null);
				client1tasks.unregister_(null, null, null);
			}
			systemConsumerIds.clear();
		}
	}
	


	
	// Protected Methods ***********************************************************************


	
	// Data Providers ***********************************************************************

	

}

