package com.redhat.qe.sm.tests;

import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.tasks.Pool;

public class Unsubscribe extends Subscribe{
	@Test(description="subscription-manager-cli: unsubscribe client to an entitlement using product ID",
			dependsOnMethods="UnsubscribeFromValidSubscriptionsByPoolID_Test",
			groups={"sm"})
	@ImplementsTCMS(id="41688")
	public void UnsubscribeFromValidSubscriptionsByProductID_Test(){
		this.subscribeToAllSubscriptions(false);
		this.unsubscribeFromAllSubscriptions(false);
	}
	
	@Test(description="subscription-manager-cli: unsubscribe client to an entitlement using pool ID",
			dependsOnMethods="EnableYumRepoAndVerifyContentAvailable_Test",
			groups={"sm"})
	@ImplementsTCMS(id="41689")
	public void UnsubscribeFromValidSubscriptionsByPoolID_Test(){
		this.subscribeToAllSubscriptions(true);
		this.unsubscribeFromAllSubscriptions(true);
	}
	
	@Test(description="Unsubscribe product entitlement and re-subscribe",
			dependsOnMethods="UnsubscribeFromValidSubscriptions_Test",
			groups={"sm"})
	@ImplementsTCMS(id="41898")
	public void ResubscribeAfterUnsubscribe_Test(){
		this.subscribeToAllSubscriptions(false);
	}
}
