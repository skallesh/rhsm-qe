package com.redhat.qe.sm.tests;

import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.abstractions.SubscriptionPool;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;

/**
 * @author jsefler
 *
 */
@Test(groups={"multi-client"})
public class MultiClientTests extends SubscriptionManagerTestScript{
	
	
	
	@Test(	description="bind/unbind with two users/consumers",
//			dependsOnGroups={"sm_stage3"},
//			groups={"sm_stage4"},
			groups={},
			dataProvider="getAllAvailableSubscriptionPoolsData")
	@ImplementsTCMS(id="53217")
	public void MultiClientSubscribeToSameSubscriptionPool_Test(SubscriptionPool pool) {
		if (client2==null) throw new SkipException("This test requires a second consumer.");
		
		// assert that the subscriptionPool is available to both consumers with the same quantity
		List<SubscriptionPool> cl1SubscriptionPools = client1tasks.getCurrentlyAvailableSubscriptionPools();
		Assert.assertTrue(cl1SubscriptionPools.contains(pool),"Subscription pool "+pool+" is available to consumer1 ("+client1username+").");
		SubscriptionPool cl1SubscriptionPool = cl1SubscriptionPools.get(cl1SubscriptionPools.indexOf(pool));
		
		List<SubscriptionPool> cl2SubscriptionPools = client2tasks.getCurrentlyAvailableSubscriptionPools();
		Assert.assertTrue(cl2SubscriptionPools.contains(pool),"Subscription pool "+pool+" is available to consumer2 ("+client2username+").");
		SubscriptionPool cl2SubscriptionPool = cl2SubscriptionPools.get(cl2SubscriptionPools.indexOf(pool));

		Assert.assertEquals(cl1SubscriptionPool.quantity, cl2SubscriptionPool.quantity, "The quantity of entitlements from subscription pool id '"+pool.poolId+"' available to both consumers is the same.");

		// subscribe consumer1 to the pool and assert that the available quantity to consumer2 has decremented by one
		client1tasks.subscribeToSubscriptionPoolUsingPoolId(pool);
		cl2SubscriptionPools = client2tasks.getCurrentlyAvailableSubscriptionPools();
		Assert.assertTrue(cl2SubscriptionPools.contains(pool),"Subscription pool id "+pool.poolId+" is still available to consumer2 ("+client2username+").");
		cl2SubscriptionPool = cl2SubscriptionPools.get(cl2SubscriptionPools.indexOf(pool));

		Assert.assertEquals(Integer.valueOf(cl2SubscriptionPool.quantity).intValue(), Integer.valueOf(cl1SubscriptionPool.quantity).intValue()-1, "The quantity of entitlements from subscription pool id '"+pool.poolId+"' available to consumer2 ("+client2username+") has decremented by one.");
	}
	
	
	
	
	// Protected Methods ***********************************************************************

	

	
	
	// Data Providers ***********************************************************************

	

}
