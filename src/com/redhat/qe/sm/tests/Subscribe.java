package com.redhat.qe.sm.tests;

import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.tasks.Subscription;

public class Subscribe extends Register{
	
	@Test(description="Verify all subscriptions can be subscribed to",
			dependsOnMethods="ValidRegistration_Test")
	@ImplementsTCMS(id="41680")
	public void SubscribeToValidSubscriptions_Test(){
		this.refreshSubscriptions();
		for (Subscription sub:this.availSubscriptions)
			this.subscribeToSubscription(sub);
	}
	
	@Test(description="Verify all subscriptions can be subscribed to",
			dependsOnMethods="SubscribeToValidSubscriptions_Test")
	@ImplementsTCMS(id="41688")
	public void UnsubscribeFromValidSubscription_Test(){
		Assert.assertEquals(this.getNonSubscribedSubscriptions().size(),
				0,
				"Asserting that all subscriptions are now subscribed");
		for(Subscription sub:this.consumedSubscriptions)
			this.subscribeToSubscription(sub);
	}
}
