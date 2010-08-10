package com.redhat.qe.sm.tests;

import java.util.List;

import org.testng.annotations.AfterGroups;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.abstractions.SubscriptionPool;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.tools.RemoteFileTasks;

@Test(groups={"list"})
public class ListTests extends SubscriptionManagerTestScript{
	
	@Test(	description="subscription-manager-cli: list available entitlements",
//			dependsOnGroups={"sm_stage2"},
//			groups={"sm_stage3"},
			enabled=true)
	@ImplementsTCMS(id="41678")
	public void EnsureAvailableEntitlementsListed_Test() {
		clienttasks.register(clientusername, clientpassword, null, null, null, Boolean.TRUE);
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		String availableSubscriptionPools = clienttasks.listAvailable().getStdout();
		Assert.assertContainsMatch(availableSubscriptionPools, "Available Subscriptions");
		
		// TODO
		log.warning("TODO: Once known, we still need to assert the following expected results:");
		log.warning(" * List produced matches the known data contained on the Candlepin server");
		log.warning(" * Confirm that the marketing names match.. see prereq link https://engineering.redhat.com/trac/IntegratedMgmtQE/wiki/sm-prerequisites");
		log.warning(" * Match the marketing names w/ https://www.redhat.com/products/");
	}
	
	
	@Test(	description="subscription-manager-cli: list consumed entitlements",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4","not_implemented"},
			enabled=false)
	@ImplementsTCMS(id="41679")
	public void EnsureConsumedEntitlementsListed_Test() {
		clienttasks.register(clientusername, clientpassword, null, null, null, Boolean.TRUE);
		clienttasks.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		String consumedProductSubscriptions = clienttasks.listConsumed().getStdout();
		Assert.assertContainsMatch(consumedProductSubscriptions, "Consumed Product Subscriptions");
	}
	
	
	@Test(	description="subscription-manager-cli: RHEL Personal should be the only available subscription to a consumer registered as type person",
			groups={"EnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void EnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test() {
		clienttasks.register(clientusername, clientpassword, "person", null, null, Boolean.TRUE);
		
		List<SubscriptionPool> subscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool rhelPersonalPool = null;
		for (SubscriptionPool subscriptionPool : subscriptionPools) {
			if (subscriptionPool.subscriptionName.equals("RHEL Personal")) rhelPersonalPool = subscriptionPool;
		}
		Assert.assertTrue(rhelPersonalPool!=null,"RHEL Personal is available to this consumer registered as type person");
		Assert.assertEquals(subscriptionPools.size(),1, "RHEL Personal is the ONLY subscription pool available to this consumer registered as type person");
	}
	@AfterGroups(groups={}, value="EnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test")
	public void teardownAfterEnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test() {
		clienttasks.unregister_();
	}
	
	
	@Test(	description="subscription-manager-cli: RHEL Personal should not be an available subscription to a consumer registered as type system",
			groups={"EnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void EnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test() {
		clienttasks.register(clientusername, clientpassword, "system", null, null, Boolean.TRUE);
		SubscriptionPool rhelPersonalPool = null;
		
		rhelPersonalPool = null;
		for (SubscriptionPool subscriptionPool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if (subscriptionPool.subscriptionName.equals("RHEL Personal")) rhelPersonalPool = subscriptionPool;
		}
		Assert.assertTrue(rhelPersonalPool==null,"RHEL Personal is NOT available to this consumer registered as type system");
		
		// also assert that RHEL Personal is included in --all --available subscription pools
		rhelPersonalPool = null;
		for (SubscriptionPool subscriptionPool : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
			if (subscriptionPool.subscriptionName.equals("RHEL Personal")) rhelPersonalPool = subscriptionPool;
		}
		Assert.assertTrue(rhelPersonalPool!=null,"RHEL Personal is included in --all --available subscription pools");
	}
	@AfterGroups(groups={}, value="EnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test")
	public void teardownAfterEnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test() {
		clienttasks.unregister_();
	}
	
	
	// Data Providers ***********************************************************************
	
	
}
