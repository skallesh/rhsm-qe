package com.redhat.qe.sm.tests;

import java.util.ArrayList;

import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.abstractions.Pool;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;

public class UnregisterTests extends SubscriptionManagerTestScript {
	
	@Test(description="unregister the client",
			groups={"sm_stage1", "blockedByBug-589626"})
	@ImplementsTCMS(id="46714")
	public void RegisterSubscribeAndUnregisterTest(){
		sm.registerToCandlepin(username, password);
		sm.refreshSubscriptions();
		ArrayList<Pool> beforeSubscribePools = (ArrayList<Pool>)sm.getAvailPools().clone();
		sm.subscribeToAllPools(false);
		sm.unregisterFromCandlepin();
		sm.registerToCandlepin(username, password);
		sm.refreshSubscriptions();
		for (Pool afterPool : sm.getAvailPools()){
			Pool correspondingPool = beforeSubscribePools.get(beforeSubscribePools.indexOf(afterPool));
			Assert.assertEquals(correspondingPool.quantity, afterPool.quantity,
				"The subscription count for Pool '"+correspondingPool.poolName+"' returned to its original count after subscribing to it and then unregistering from the candlepin server.");
		}
	}
}
