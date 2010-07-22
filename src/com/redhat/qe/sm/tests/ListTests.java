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
		String availableSubscriptionPools = clienttasks.listAvailable();
		Assert.assertContainsMatch(availableSubscriptionPools, "Available Subscriptions");
	}
	
	
	@Test(	description="subscription-manager-cli: list consumed entitlements",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4"},
			enabled=false) //not_implemented
	@ImplementsTCMS(id="41679")
	public void EnsureConsumedEntitlementsListed_Test() {
		clienttasks.register(clientusername, clientpassword, null, null, null, null);
		clienttasks.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		String consumedProductSubscriptions = clienttasks.listConsumed();
		Assert.assertContainsMatch(consumedProductSubscriptions, "Consumed Product Subscriptions");
	}
}
