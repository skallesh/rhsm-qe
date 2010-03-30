package com.redhat.qe.sm.tests;

import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.tasks.Pool;

public class Unsubscribe extends Subscribe{
	@Test(description="Verify all subscriptions can be subscribed to",
			dependsOnMethods="EnableYumRepoAndVerifyContentAvailable_Test",
			groups={"sm"})
	@ImplementsTCMS(id="41688")
	public void UnsubscribeFromValidSubscriptions_Test(){
		for(Pool sub:this.consumedSubscriptions)
			this.unsubscribeFromPool(sub);
		Assert.assertEquals(this.consumedSubscriptions.size(),
				0,
				"Asserting that all subscriptions are now unsubscribed");
	}
	
	
	
	@Test(description="Unsubscribe product entitlement and re-subscribe",
			dependsOnMethods="UnsubscribeFromValidSubscriptions_Test",
			groups={"sm"})
	@ImplementsTCMS(id="41898")
	public void ResubscribeAfterUnsubscribe_Test(){
		for(Pool sub:this.availSubscriptions)
			this.subscribeToPool(sub, false);
	}
}
