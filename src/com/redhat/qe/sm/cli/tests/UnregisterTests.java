package com.redhat.qe.sm.cli.tests;


import java.util.List;

import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.sm.data.SubscriptionPool;

/**
 * @author ssalevan
 *
 */
@Test(groups={"unregister"})
public class UnregisterTests extends SubscriptionManagerTestScript {
	
	@Test(description="unregister the consumer",
//			groups={"sm_stage1", "blockedByBug-589626"},
			groups={"blockedByBug-589626"},
			enabled=true)
	@ImplementsTCMS(id="46714")
	public void RegisterSubscribeAndUnregisterTest(){
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null);
		List<SubscriptionPool> availPoolsBeforeSubscribingToAllPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		clienttasks.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null);
		for (SubscriptionPool afterPool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			SubscriptionPool correspondingPool = availPoolsBeforeSubscribingToAllPools.get(availPoolsBeforeSubscribingToAllPools.indexOf(afterPool));
			Assert.assertEquals(correspondingPool.quantity, afterPool.quantity,
				"The subscription quantity count for Pool "+correspondingPool.poolId+" returned to its original count after subscribing to it and then unregistering from the candlepin server.");
		}
	}
}
