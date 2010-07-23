package com.redhat.qe.sm.tests;

import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
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
		clienttasks.register(clientusername, clientpassword, null, null, null, null);
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		String availableSubscriptionPools = clienttasks.listAvailable();
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
		clienttasks.register(clientusername, clientpassword, null, null, null, null);
		clienttasks.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		String consumedProductSubscriptions = clienttasks.listConsumed();
		Assert.assertContainsMatch(consumedProductSubscriptions, "Consumed Product Subscriptions");
	}
}
