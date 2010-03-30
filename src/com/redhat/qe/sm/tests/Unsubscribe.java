package com.redhat.qe.sm.tests;

import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.tasks.Subscription;

public class Unsubscribe extends Subscribe{
	@Test(description="Verify all subscriptions can be subscribed to",
			dependsOnMethods="EnableYumRepoAndVerifyContentAvailable_Test")
	@ImplementsTCMS(id="41688")
	public void UnsubscribeFromValidSubscriptions_Test(){
		for(Subscription sub:this.consumedSubscriptions)
			this.unsubscribeFromSubscription(sub);
		Assert.assertEquals(this.consumedSubscriptions.size(),
				0,
				"Asserting that all subscriptions are now unsubscribed");
	}
	
	@Test(description="Enable yum repo and verify that content is available for testing",
			dependsOnMethods="UnsubscribeFromValidSubscriptions_Test")
	@ImplementsTCMS(id="41696")
	public void DisableYumRepoAndVerifyContentNotAvailable_Test(){
		this.adjustRHSMYumRepo(false);
		for(Subscription sub:this.availSubscriptions)
			for(String repo:this.getYumRepolist())
				if(repo.contains(sub.productId))
					Assert.fail("After unsubscribe, Yum still has access to repo: "+repo);
	}
}
