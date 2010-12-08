package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.BzChecker;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
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


@Test(groups={"rhelPersonal"})
public class RHELPersonalTests extends SubscriptionManagerCLITestScript{
	protected List<String> consumerIds = new ArrayList<String>();
	protected String personConsumerId = null;
	protected int multipleSystems = 4;	// multiple (unlimited)  // multipleSystems is a count of systems that will be used to subscribe to the sub-pool.  Theoretically this number should be very very large to test the unlimited quantity
	
	protected String consumerUsername = getProperty("sm.rhpersonal.username1", "");
	protected String consumerPassword = getProperty("sm.rhpersonal.password1", "");
	protected String anotherConsumerUsername = getProperty("sm.rhpersonal.username2", "");
	protected String anotherConsumerPassword = getProperty("sm.rhpersonal.password2", "");
	protected String personSubscriptionName = null;//getProperty("sm.rhpersonal.productName", "");
	protected String rhpersonalProductId = getProperty("sm.rhpersonal.productId", "");
	protected String systemSubscriptionName = getProperty("sm.rhpersonal.subproductName", "");
	protected String systemConsumedProductName = getProperty("sm.rhpersonal.consumedSubproductNames", "");  //FIXME change to a List


	
	
	// Test Methods ***********************************************************************
	
	@Test(	description="subscription-manager-cli: Ensure RHEL Personal Bits are available and unlimited after a person has subscribed to RHEL Personal",
			groups={"EnsureSubPoolIsAvailableAfterRegisteredPersonSubscribesToRHELPersonal_Test", "RHELPersonal", "blockedByBug-624816", "blockedByBug-641155", "blockedByBug-643405"},
			enabled=true)
	@ImplementsNitrateTest(caseId=55702)
//	@ImplementsNitrateTest(caseId={55702,55718})
	public void EnsureSubPoolIsAvailableAfterRegisteredPersonSubscribesToRHELPersonal_Test() {
//		if (!isServerOnPremises) throw new SkipException("Currently this test is designed only for on-premises.");	//TODO Make this work for IT too.  jsefler 8/12/2010 
		if (client2tasks==null) throw new SkipException("These tests are designed to use a second client.");
		if (consumerUsername.equals("admin")) throw new SkipException("This test requires that the client user ("+consumerUsername+") is NOT admin.");
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=624423 - jsefler 8/16/2010
		Boolean invokeWorkaroundWhileBugIsOpen = false;
		try {String bugId="624423"; if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla bug "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			servertasks.restartTomcat();
		} // END OF WORKAROUND
		
		
		SubscriptionPool pool = null;
		
		
		log.info("Register client2 under username '"+consumerUsername+"' as a system and assert that '"+systemSubscriptionName+"' is NOT yet available...");
		client2tasks.unregister();
		client1tasks.unregister();	// just in case client1 is still registered as the person consumer
		client2tasks.register(consumerUsername, consumerPassword, ConsumerType.system, null, null, null, null);
		List<SubscriptionPool> client2BeforeSubscriptionPools = client2tasks.getCurrentlyAvailableSubscriptionPools();
		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName",systemSubscriptionName,client2BeforeSubscriptionPools);
		Assert.assertNull(pool,systemSubscriptionName+" is NOT yet available to client2 system '"+client2.getConnection().getHostname()+"' registered under user '"+consumerUsername+"'.");

		
//		log.info("Now register client1 under username '"+consumerUsername+"' as a person and subscribe to the '"+personSubscriptionName+"' subscription pool...");
		log.info("Now register client1 under username '"+consumerUsername+"' as a person and subscribe to the personal subscription pool with ProductId '"+rhpersonalProductId+"'...");
		client1tasks.unregister();
		client1tasks.register(consumerUsername, consumerPassword, ConsumerType.person, null, null, null, null);
		personConsumerId = client1tasks.getCurrentConsumerId();
//		pool = client1tasks.findSubscriptionPoolWithMatchingFieldFromList("subscriptionName",personSubscriptionName,client1tasks.getCurrentlyAllAvailableSubscriptionPools());
		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId",rhpersonalProductId,client1tasks.getCurrentlyAvailableSubscriptionPools());
		personSubscriptionName = pool.subscriptionName;
		Assert.assertNotNull(pool,personSubscriptionName+" is available to user '"+consumerUsername+"' registered as a person.");
		List<File> beforeEntitlementCertFiles = client1tasks.getCurrentEntitlementCertFiles();
// DELETEME - was old behavior pre fix for https://bugzilla.redhat.com/show_bug.cgi?id=641155
//		if (isServerOnPremises) {	// needed this special case block to assert that that a new entitlement certificate is NOT dropped
	//		client1tasks.subscribe(pool.poolId, null, null, null, null);
	//		Assert.assertTrue(!client1tasks.getCurrentlyAvailableSubscriptionPools().contains(pool),
	//			"The available subscription pools no longer contains the just subscribed to pool: "+pool);
//			List<File> afterEntitlementCertFiles = client1tasks.getCurrentEntitlementCertFiles();
//			Assert.assertTrue(afterEntitlementCertFiles.equals(beforeEntitlementCertFiles),
//				"Subscribing to subscription pool '"+personSubscriptionName+"' does NOT drop a new entitlement certificate when registered as a person.");
//		} else {
//			client1tasks.subscribeToSubscriptionPoolUsingPoolId(pool);
//		}
			//FIXME Prefer to use this syntax....
		client1tasks.subscribeToSubscriptionPool(pool);
		
		
		log.info("Now client2 (already registered as a system under username '"+consumerUsername+"') should now have '"+systemSubscriptionName+"' available with unlimited quantity...");
		List<SubscriptionPool> client2AfterSubscriptionPools = client2tasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool systemSubscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName",systemSubscriptionName,client2AfterSubscriptionPools);
		Assert.assertNotNull(systemSubscriptionPool,systemSubscriptionName+" is now available to client2 '"+client2.getConnection().getHostname()+"' (registered as a system under username '"+consumerUsername+"')");
		Assert.assertEquals(systemSubscriptionPool.quantity.toLowerCase(),"unlimited","An unlimited quantity of entitlements is available to "+systemSubscriptionName+".");
		
		
		log.info("Verifying that the available subscription pools available to client2 has increased by only the '"+systemSubscriptionName+"' pool...");
		Assert.assertTrue(
				client2AfterSubscriptionPools.containsAll(client2BeforeSubscriptionPools) &&
				client2AfterSubscriptionPools.contains(systemSubscriptionPool) &&
				client2AfterSubscriptionPools.size()==client2BeforeSubscriptionPools.size()+1,
				"The list of available subscription pools seen by client2 increases only by '"+systemSubscriptionName+"' pool: "+systemSubscriptionPool);
	}
	 
	 
	@Test(	description="subscription-manager-cli: Ensure RHEL Personal Bits are consumable after a person has subscribed to RHEL Personal",
			groups={"EnsureSubPoolIsConsumableAfterRegisteredPersonSubscribesToRHELPersonal_Test","RHELPersonal"},
			dependsOnGroups={"EnsureSubPoolIsAvailableAfterRegisteredPersonSubscribesToRHELPersonal_Test"},
			enabled=true)
	@ImplementsNitrateTest(caseId=55702)
//	@ImplementsNitrateTest(caseId={55702,55718})
	public void EnsureSubPoolIsConsumableAfterRegisteredPersonSubscribesToRHELPersonal_Test() {
				
		log.info("Now client2 (already registered as a system under username '"+consumerUsername+"') can now consume '"+systemSubscriptionName+"'...");
		SubscriptionPool systemSubscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName",systemSubscriptionName,client2tasks.getCurrentlyAvailableSubscriptionPools());
		client2tasks.subscribeToSubscriptionPoolUsingPoolId(systemSubscriptionPool);
		
		
		log.info("Now client2 should be consuming the product '"+systemConsumedProductName+"'...");
		ProductSubscription systemProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName",systemConsumedProductName,client2tasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertNotNull(systemProductSubscription,systemConsumedProductName+" is now consumed on client2 system '"+client2.getConnection().getHostname()+"' registered under user '"+consumerUsername+"'.");
		
	}
	
	
	@Test(	description="subscription-manager-cli: Ensure that availability of RHEL Personal Bits is revoked once the person unsubscribes from RHEL Personal",
			groups={"EnsureAvailabilityOfSubPoolIsRevokedOncePersonUnsubscribesFromRHELPersonal_Test","RHELPersonal"},
			dependsOnGroups={"EnsureSubPoolIsConsumableAfterRegisteredPersonSubscribesToRHELPersonal_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void EnsureAvailabilityOfSubPoolIsRevokedOncePersonUnsubscribesFromRHELPersonal_Test() {
		
		log.info("Unsubscribe client2 (already registered as a system under username '"+consumerUsername+"') from all currently consumed product subscriptions...");
		client2tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		
		
		log.info("Unsubscribe client1 (already registered as a person under username '"+consumerUsername+"') from product subscription '"+personSubscriptionName+"'...");
		client1tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();

		
		log.info("Now verify that client2 (already registered as a system under username '"+consumerUsername+"') can no longer subscribe to the '"+systemSubscriptionName+"' pool...");
		SubscriptionPool systemSubscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName",systemSubscriptionName,client2tasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNull(systemSubscriptionPool,systemSubscriptionName+" is no longer available on client2 system '"+client2.getConnection().getHostname()+"' registered under user '"+consumerUsername+"'.");
	}

	
	@Test(	description="subscription-manager-cli: Ensure that multiple (unlimited) systems can subscribe to subpool",
			groups={"SubscribeMultipleSystemsToSubPool_Test","RHELPersonal"},
			dependsOnGroups={"EnsureAvailabilityOfSubPoolIsRevokedOncePersonUnsubscribesFromRHELPersonal_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void SubscribeMultipleSystemsToSubPool_Test() {
		log.info("Making sure the clients are not subscribed to anything...");
//		client2tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
//		client2tasks.unregister();
//		client1tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		teardownAfterGroups();
		client1tasks.register(consumerUsername, consumerPassword, ConsumerType.person, null, null, null, null);
		personConsumerId = client1tasks.getCurrentConsumerId();

		
		log.info("Subscribe client1 (already registered as a person under username '"+consumerUsername+"') to subscription pool '"+personSubscriptionName+"'...");
		SubscriptionPool personSubscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName",personSubscriptionName,client1tasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(personSubscriptionPool,personSubscriptionName+" is available to user '"+consumerUsername+"' registered as a person.");
		client1tasks.subscribe(personSubscriptionPool.poolId, null, null, null, null);

		log.info("Register "+multipleSystems+" new systems under username '"+consumerUsername+"' and subscribe to product subscription '"+systemSubscriptionName+"'...");
		consumerIds = new ArrayList<String>();
		for (int systemNum = 1; systemNum <=multipleSystems; systemNum++) {
			// simulate a clean system
			client2tasks.removeAllCerts(true,true);
			
			String consumerId = client2tasks.getCurrentConsumerId(client2tasks.register(consumerUsername, consumerPassword, ConsumerType.system, null, null, null, Boolean.TRUE));
			consumerIds.add(consumerId);
			SubscriptionPool subPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName",systemSubscriptionName,client2tasks.getCurrentlyAvailableSubscriptionPools());
			log.info("Subscribing system '"+systemNum+"' ('"+consumerId+"' under username '"+consumerUsername+"') to product subscription '"+systemSubscriptionName+"'...");
			client2tasks.subscribeToSubscriptionPoolUsingPoolId(subPool);
			ProductSubscription productSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName",systemConsumedProductName,client2tasks.getCurrentlyConsumedProductSubscriptions());
			Assert.assertNotNull(productSubscription,systemConsumedProductName+" is now consumed by consumer '"+consumerId+"' (registered as a system under username '"+consumerUsername+"')");
		}
	}
	
	
	@Test(	description="subscription-manager-cli: Ensure person consumer cannot unsubscribe while subpools are consumed",
			groups={"EnsurePersonCannotUnsubscribeWhileSubpoolsAreConsumed_Test","RHELPersonal", "blockedByBug-624063", "blockedByBug-639434"/*, "blockedByBug-658283"*/, "blockedByBug-658683"},
			dependsOnGroups={"SubscribeMultipleSystemsToSubPool_Test"},
			enabled=true)
	@ImplementsNitrateTest(caseId=58898)
	// 1) unsubscribe person from personal pool while systems are subscribed to subpool (scenario from calfanso@redhat.com)
	public void EnsurePersonCannotUnsubscribeWhileSubpoolsAreConsumed_Test() {
		log.info("Assuming that multiple systems have subscribed to subpool '"+systemSubscriptionName+"' in prior testcase...");
	
		// REFERENCE FIX TO https://bugzilla.redhat.com/show_bug.cgi?id=624063

		log.info("Now, attempt to unsubscribe the person on client 1 from the "+personSubscriptionName+" pool and assert the unsubscribe is blocked.");
		SSHCommandResult result = client1tasks.unsubscribe_(Boolean.TRUE,null);
		//Assert.assertTrue(result.getStderr().startsWith("Cannot unbind due to outstanding entitlement:"),
		//		"Attempting to unsubscribe the person consumer from all pools is blocked when another system registered by the same consumer is consuming from a subpool."); // stderr: Cannot unregister due to outstanding entitlement: 9
		Assert.assertContainsMatch(result.getStderr(),"Cannot unbind due to outstanding sub-pool entitlements in [a-f,0-9]{32}",
				"Attempting to unsubscribe the person consumer from all pools is blocked when another system registered with the same username is consuming from a subpool."); // stderr: Cannot unbind due to outstanding sub-pool entitlements in ff8080812c9942fa012c994cf1da02a1
	}
	
	
	@Test(	description="subscription-manager-cli: Ensure person consumer cannot unregister while subpools are consumed",
			groups={"EnsurePersonCannotUnregisterWhileSubpoolsAreConsumed_Test","RHELPersonal", "blockedByBug-624063", "blockedByBug-639434", "blockedByBug-658683"},
			dependsOnGroups={"SubscribeMultipleSystemsToSubPool_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void EnsurePersonCannotUnregisterWhileSubpoolsAreConsumed_Test() {
		log.info("Assuming that multiple systems have subscribed to subpool '"+systemSubscriptionName+"' in prior testcase...");
	
		// REFERENCE FIX TO https://bugzilla.redhat.com/show_bug.cgi?id=624063

		log.info("Now, attempt to unregister the person on client 1 from the "+personSubscriptionName+" pool and assert the unregister is blocked.");
		SSHCommandResult result = client1tasks.unregister_();
		//Assert.assertTrue(result.getStderr().startsWith("Cannot unregister due to outstanding entitlement:"),
		//		"Attempting to unregister the person consumer is blocked when another system is register by the same consumer is consuming from a subpool."); // stderr: Cannot unregister due to outstanding entitlement: 9
		Assert.assertContainsMatch(result.getStdout(),"Cannot unregister due to outstanding sub-pool entitlements in [a-f,0-9]{32}",
				"Attempting to unregister the person consumer is blocked when another system registered with the same username is consuming from a subpool."); // stdout: Cannot unregister due to outstanding sub-pool entitlements in ff8080812c9942fa012c994cf1da02a1
	}
	
	
// DUE TO BEHAVIOR CHNAGE, THIS TEST WAS REPLACED BY EnsurePersonCannotUnsubscribeWhileSubpoolsAreConsumed_Test
//	@Test(	description="subscription-manager-cli: Ensure that the entitlement certs for subscribed subpool is revoked once the person unsubscribes from RHEL Personal",
//			groups={"EnsureEntitlementCertForSubPoolIsRevokedOncePersonUnsubscribesFromRHELPersonal_Test","RHELPersonal","blockedByBug-639434"},
//			dependsOnGroups={"SubscribeMultipleSystemsToSubPool_Test","EnsurePersonCannotUnsubscribeWhileSubpoolsAreConsumed_Test","EnsurePersonCannotUnregisterWhileSubpoolsAreConsumed_Test"},
////			dataProvider="getRHELPersonalData",
//			enabled=true)
//	@ImplementsNitrateTest(caseId=58898)
//	// 1) unsubscribe person from personal pool while systems are subscribed to subpool (scenario from calfanso@redhat.com)
//	public void EnsureEntitlementCertForSubPoolIsRevokedOncePersonUnsubscribesFromRHELPersonal_Test(/*String consumerUsername,	String consumerPassword,	String personSubscriptionName,		String systemSubscriptionName,	String systemConsumedProductName*/) {
//		log.info("Assuming that multiple systems have subscribed to subpool '"+systemSubscriptionName+"' in prior testcase...");
//	
//		log.info("Now, unsubscribe the person on client 1 from the '"+personSubscriptionName+"' and assert that the '"+systemConsumedProductName+"' and '"+systemSubscriptionName+"' gets revoked from the system consumers.");
//		client1tasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
//		
//		log.info("Now the the certs for '"+systemConsumedProductName+"' and '"+systemSubscriptionName+"' should be revoked from the system consumers...");
//		for (String consumerId : consumerIds) {
//			//client2tasks.reregister(consumerUsername,consumerPassword,consumerId);
//			client2tasks.reregisterToExistingConsumer(consumerUsername,consumerPassword,consumerId);
//			// 10/11/2010 NOT NEEDED SINCE register --consumerid NOW REFRESHES CERTS			client2tasks.restart_rhsmcertd(1, true);	// give rhsmcertd a chance to download the consumer's certs
//			ProductSubscription productSubscription = client2tasks.findProductSubscriptionWithMatchingFieldFromList("productName",systemConsumedProductName,client2tasks.getCurrentlyConsumedProductSubscriptions());
//			Assert.assertTrue(productSubscription==null,systemConsumedProductName+" is no longer consumed by '"+consumerId+"' (registered as a system under username '"+consumerUsername+"')");
//			SubscriptionPool systemSubscriptionPool = client2tasks.findSubscriptionPoolWithMatchingFieldFromList("subscriptionName",systemSubscriptionName,client2tasks.getCurrentlyAvailableSubscriptionPools());
//			Assert.assertTrue(systemSubscriptionPool==null,systemSubscriptionName+" is no longer available to consumer '"+consumerId+"' (registered as a system under username '"+consumerUsername+"')");
//		}
//	}
	
	
	@Test(	description="subscription-manager-cli: Ensure that unsubscribing system from subpool while other systems are subscribed to subpool does not cause subpool to go away",
			groups={"EnsureEntitlementCertForSubPoolIsNotRevokedOnceAnotherSystemUnsubscribesFromSubPool_Test","RHELPersonal", "blockedByBug-643405"},
//			dependsOnGroups={"EnsureEntitlementCertForSubPoolIsRevokedOncePersonUnsubscribesFromRHELPersonal_Test"},
			dependsOnGroups={"SubscribeMultipleSystemsToSubPool_Test","EnsurePersonCannotUnsubscribeWhileSubpoolsAreConsumed_Test","EnsurePersonCannotUnregisterWhileSubpoolsAreConsumed_Test"},
			enabled=true)
	@ImplementsNitrateTest(caseId=58899)
	// 2) unsubscribe system from subpool while other systems are subscribed to subpool, make sure the subpool doesn't go away (scenario from calfanso@redhat.com)
	public void EnsureEntitlementCertForSubPoolIsNotRevokedOnceAnotherSystemUnsubscribesFromSubPool_Test() {
		
		log.info("Now start unsubscribing each system from the consumed product '"+systemConsumedProductName+"' and assert the sub pool '"+systemSubscriptionName+"' is still available...");
		for (String consumerId : consumerIds) {
			//client2tasks.reregister(consumerUsername,consumerPassword,consumerId);
			client2tasks.reregisterToExistingConsumer(consumerUsername,consumerPassword,consumerId);
			// 10/11/2010 NOT NEEDED SINCE register --consumerid NOW REFRESHES CERTS			client2tasks.restart_rhsmcertd(1, true);	// give rhsmcertd a chance to download the consumer's certs
			ProductSubscription productSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName",systemConsumedProductName,client2tasks.getCurrentlyConsumedProductSubscriptions());
			client2tasks.unsubscribeFromProductSubscription(productSubscription);
			SubscriptionPool systemSubscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName",systemSubscriptionName,client2tasks.getCurrentlyAvailableSubscriptionPools());
			Assert.assertNotNull(systemSubscriptionPool,systemSubscriptionName+" is once again available to consumer '"+consumerId+"' (registered as a system under username '"+consumerUsername+"')");
		}
	}
	
	
	@Test(	description="subscription-manager-cli: Ensure that after unsubscribing all systems from a subpool, the subpool should not get deleted",
			groups={"EnsureSubPoolIsNotDeletedAfterAllOtherSystemsUnsubscribeFromSubPool_Test","RHELPersonal"},
			dependsOnGroups={"EnsureEntitlementCertForSubPoolIsNotRevokedOnceAnotherSystemUnsubscribesFromSubPool_Test"},
			enabled=true)
	@ImplementsNitrateTest(caseId=58907)
	// 3) unsubscribe system from subpool as the last system subscribed, make sure the subpool doesn't get deleted (scenario from calfanso@redhat.com)
	public void EnsureSubPoolIsNotDeletedAfterAllOtherSystemsUnsubscribeFromSubPool_Test() {
		log.info("After having unsubscribed all systems from product '"+systemConsumedProductName+"' in the prior testcase , we will now verify that the subpool '"+systemSubscriptionName+"' has not been deleted and that all systems can still subscribe to it ...");

		for (String consumerId : consumerIds) {
			//client2tasks.reregister(consumerUsername,consumerPassword,consumerId);
			client2tasks.reregisterToExistingConsumer(consumerUsername,consumerPassword,consumerId);
			// 10/11/2010 NOT NEEDED SINCE register --consumerid NOW REFRESHES CERTS			client2tasks.restart_rhsmcertd(1, true);	// give rhsmcertd a chance to download the consumer's certs
			ProductSubscription productSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName",systemConsumedProductName,client2tasks.getCurrentlyConsumedProductSubscriptions());
			Assert.assertNull(productSubscription,systemConsumedProductName+" is not consumed by consumer '"+consumerId+"' (registered as a system under username '"+consumerUsername+"')");
			SubscriptionPool systemSubscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName",systemSubscriptionName,client2tasks.getCurrentlyAvailableSubscriptionPools());
			Assert.assertNotNull(systemSubscriptionPool,systemSubscriptionName+" is still available to consumer '"+consumerId+"' (registered as a system under username '"+consumerUsername+"')");
		}
		
		log.info("Now that all the subscribers of '"+systemSubscriptionName+"' have unsubscribed from '"+systemConsumedProductName+"', the person consumer should be able to unregister without being blocked due to outstanding entitlements...");
		client1tasks.unregister();		
	}
	
	
	@Test(	description="subscription-manager-cli: Ensure system autosubscribe consumes subpool RHEL Personal Bits",
			groups={"EnsureSystemAutosubscribeConsumesSubPool_Test"/*, "RHELPersonal"*/, "blockedByBug-637937"},
//			dependsOnGroups={"EnsureSubPoolIsNotDeletedAfterAllOtherSystemsUnsubscribeFromSubPool_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void EnsureSystemAutosubscribeConsumesSubPool_Test() {
		log.info("Now register client1 under username '"+consumerUsername+"' as a person and subscribe to the '"+personSubscriptionName+"' subscription pool...");
		client1tasks.unregister();
		client1tasks.register(consumerUsername, consumerPassword, ConsumerType.person, null, null, null, null);
		personConsumerId = client1tasks.getCurrentConsumerId();
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName",personSubscriptionName,client1tasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(pool,personSubscriptionName+" is available to user '"+consumerUsername+"' registered as a person.");
		List<File> beforeEntitlementCertFiles = client1tasks.getCurrentEntitlementCertFiles();
// DELETEME - was old behavior pre fix for https://bugzilla.redhat.com/show_bug.cgi?id=641155
//		if (isServerOnPremises) {	// needed this special case block to assert that that a new entitlement certificate is NOT dropped
	//		client1tasks.subscribe(pool.poolId, null, null, null, null);
	//		Assert.assertTrue(!client1tasks.getCurrentlyAvailableSubscriptionPools().contains(pool),
	//			"The available subscription pools no longer contains the just subscribed to pool: "+pool);
//			List<File> afterEntitlementCertFiles = client1tasks.getCurrentEntitlementCertFiles();
//			Assert.assertTrue(afterEntitlementCertFiles.equals(beforeEntitlementCertFiles),
//				"Subscribing to subscription pool '"+personSubscriptionName+"' does NOT drop a new entitlement certificate when registered as a person.");
//		} else {
//			client1tasks.subscribeToSubscriptionPoolUsingPoolId(pool);
//		}
		client1tasks.subscribeToSubscriptionPool(pool);
		
		
		log.info("Now register client2 under username '"+consumerUsername+"' as a system with autosubscribe to assert that '"+systemConsumedProductName+"' gets consumed...");
		client2tasks.unregister();
		client2tasks.register(consumerUsername, consumerPassword, ConsumerType.system, null, null, Boolean.TRUE, null);
		List<ProductSubscription> client2ConsumedProductSubscriptions = client2tasks.getCurrentlyConsumedProductSubscriptions();
		
		
		ProductSubscription consumedProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName",systemConsumedProductName,client2ConsumedProductSubscriptions);
		Assert.assertNotNull(consumedProductSubscription,systemConsumedProductName+" has been autosubscribed by client2 '"+client2.getConnection().getHostname()+"' (registered as a system under username '"+consumerUsername+"')");
	}
	
	
	@Test(	description="subscription-manager-cli: No consumer created by any other user in the same owner can see the sub pool",
			groups={"EnsureUsersSubPoolIsNotAvailableToSystemsRegisterByAnotherUsername_Test"/*, "RHELPersonal"*/, "blockedByBug-643405"},
//			dependsOnGroups={"EnsureSubPoolIsNotDeletedAfterAllOtherSystemsUnsubscribeFromSubPool_Test"},
			enabled=true)
	@ImplementsNitrateTest(caseId=61126)
	public void EnsureUsersSubPoolIsNotAvailableToSystemsRegisterByAnotherUsername_Test() {
		teardownAfterGroups();
		
		log.info("Register client1 under username '"+consumerUsername+"' as a person and subscribe to the '"+personSubscriptionName+"' subscription pool...");
		client1tasks.register(consumerUsername, consumerPassword, ConsumerType.person, null, null, null, null);
		personConsumerId = client1tasks.getCurrentConsumerId();
		SubscriptionPool personSubscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName",personSubscriptionName,client1tasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(personSubscriptionPool,
				personSubscriptionName+" is available to user '"+consumerUsername+"' registered as a person.");
		client1tasks.subscribe(personSubscriptionPool.poolId, null, null, null, null);

		log.info("Now register client2 under username '"+consumerUsername+"' as a system and assert the subpool '"+systemSubscriptionName+"' is available...");
		client2tasks.unregister();
		client2tasks.register(consumerUsername, consumerPassword, ConsumerType.system, null, null, null, null);
		Assert.assertNotNull(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName",systemSubscriptionName,client2tasks.getCurrentlyAvailableSubscriptionPools()),
				systemSubscriptionName+" is available to user '"+consumerUsername+"' registered as a system.");

		log.info("Now register client2 under username '"+anotherConsumerUsername+"' as a system and assert the subpool '"+systemSubscriptionName+"' is NOT available...");
		client2tasks.unregister();
		client2tasks.register(anotherConsumerUsername, anotherConsumerPassword, ConsumerType.system, null, null, null, null);
		Assert.assertNull(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName",systemSubscriptionName,client2tasks.getCurrentlyAvailableSubscriptionPools()),
				systemSubscriptionName+" is NOT available to user '"+anotherConsumerUsername+"' who is under the same owner as '"+consumerUsername+"'.");
	}
	
	
	@Test(	description="subscription-manager-cli: Ensure a system cannot subscribe to a personal subscription pool",
			groups={"EnsureSystemCannotSubscribeToPersonalPool_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void EnsureSystemCannotSubscribeToPersonalPool_Test() {
		teardownAfterGroups();
		
		log.info("Register client1 under username '"+consumerUsername+"' as a system and assert that '"+rhpersonalProductId+"' can NOT be subscribed to...");
		//client1tasks.unregister();
		client1tasks.register(consumerUsername, consumerPassword, ConsumerType.system, null, null, null, null);
		
		SubscriptionPool personSubscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId",rhpersonalProductId,client1tasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(personSubscriptionPool,
				"ProductId '"+rhpersonalProductId+"' is listed as all available to user '"+consumerUsername+"' registered as a system.");
		SSHCommandResult sshComandResult = client1tasks.subscribe(personSubscriptionPool.poolId, null, null, null, null);

		// stdout: Consumers of this type are not allowed to subscribe to the pool with id 'ff8080812c9e72a8012c9e738ce70191'
		Assert.assertContainsMatch(sshComandResult.getStdout().trim(), "Consumers of this type are not allowed to subscribe to the pool with id '"+personSubscriptionPool.poolId+"'",
				"Attempting to subscribe a system consumer to a personal pool is blocked.");
		Assert.assertEquals(client1tasks.listConsumedProductSubscriptions().getStdout().trim(),"No Consumed subscription pools to list",
				"Because the subscribe attempt was blocked, there should still be 'No Consumed subscription pools to list'.");
	}
	
	
	// TODO Candidates for an automated Test:
	// https://bugzilla.redhat.com/show_bug.cgi?id=626509
	
	
	
	// Configuration Methods ***********************************************************************
	
	@BeforeClass(groups={"setup"})
	public void beforeClassSetup() {
		if (rhpersonalProductId.equals("")) {
			throw new SkipException("To enable the RHEL Personal Tests, we need to know the ProductId of a Subscription containing a subpool of personal products.");
		}
	}
	
	// FIXME: I don't believe that this methods is getting called after all tests tagged with "RHELPersonal" have run
	@AfterGroups(groups={"setup"}, value={"RHELPersonal"}, alwaysRun=true)
	public void teardownAfterGroups() {
		if (client2tasks!=null) {
			client2tasks.unsubscribe_(Boolean.TRUE,null);
			client2tasks.unregister_();
		}

		if (client1tasks!=null) {
			
			for (String consumerId : consumerIds) {
				client1tasks.register_(client1username,client1password,null,null,consumerId,null, Boolean.TRUE);
				client1tasks.unsubscribe_(Boolean.TRUE, null);
				client1tasks.unregister_();
			}
			consumerIds.clear();
			
			if (personConsumerId!=null) {
				//client1tasks.reregister_(client1username, client1password, personConsumerId);
				//client1tasks.removeAllCerts(true, true);
				client1tasks.register_(client1username,client1password,null,null,personConsumerId,null,Boolean.TRUE);
			}
			client1tasks.unsubscribe_(Boolean.TRUE,null);
			client1tasks.unregister_();
			personConsumerId=null;
		}
	}
	

	
	
	// Protected Methods ***********************************************************************


	
	// Data Providers ***********************************************************************

	

}

