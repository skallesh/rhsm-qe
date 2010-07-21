package com.redhat.qe.sm.tests;


import java.util.List;

import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.abstractions.SubscriptionPool;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;

@Test(groups={"unregister"})
public class UnregisterTests extends SubscriptionManagerTestScript {
	
	@Test(description="unregister the consumer",
//			groups={"sm_stage1", "blockedByBug-589626"},
			groups={"blockedByBug-589626"},
			enabled=true)
	@ImplementsTCMS(id="46714")
	public void RegisterSubscribeAndUnregisterTest(){
		c1sm.register(consumer1username, consumer1password, null, null, null, Boolean.TRUE);
		List<SubscriptionPool> availPoolsBeforeSubscribingToAllPools = c1sm.getCurrentlyAvailableSubscriptionPools();
		c1sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		c1sm.unregister();
		c1sm.register(consumer1username, consumer1password, null, null, null, null);
		for (SubscriptionPool afterPool : c1sm.getCurrentlyAvailableSubscriptionPools()) {
			SubscriptionPool correspondingPool = availPoolsBeforeSubscribingToAllPools.get(availPoolsBeforeSubscribingToAllPools.indexOf(afterPool));
			Assert.assertEquals(correspondingPool.quantity, afterPool.quantity,
				"The subscription quantity count for Pool "+correspondingPool.poolId+" returned to its original count after subscribing to it and then unregistering from the candlepin server.");
		}
	}
}
