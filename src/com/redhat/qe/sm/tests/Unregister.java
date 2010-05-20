package com.redhat.qe.sm.tests;

import java.util.ArrayList;

import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.tasks.Pool;

public class Unregister extends Setup {
	@Test(description="unregister the client",
			groups={"sm_stage1", "blockedByBug-589626"})
	@ImplementsTCMS(id="46714")
	public void RegisterSubscribeAndUnregisterTest(){
		this.registerToCandlepin(username, password);
		this.refreshSubscriptions();
		ArrayList<Pool> beforeSubscribePools = (ArrayList<Pool>)this.availPools.clone();
		this.subscribeToAllPools(false);
		this.unregisterFromCandlepin();
		this.registerToCandlepin(username, password);
		this.refreshSubscriptions();
		for (Pool afterPool: this.availPools){
			Pool correspondingPool = beforeSubscribePools.get(beforeSubscribePools.indexOf(afterPool));
			Assert.assertEquals(correspondingPool.quantity, afterPool.quantity,
					"Pool before unregister \""+correspondingPool.poolName+
					"\" subscription count matches corresponding pool before subscription");
		}
	}
}
