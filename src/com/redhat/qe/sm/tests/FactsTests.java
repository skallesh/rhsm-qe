package com.redhat.qe.sm.tests;

import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.sm.data.SubscriptionPool;



/**
 * @author jsefler
 */
@Test(groups={"facts"})
public class FactsTests extends SubscriptionManagerTestScript{
	

	@Test(	description="subscription-manager: facts and rules: fact check RHEL distribution",
			groups={"FactCheckRhelDistribution_Test"}, dependsOnGroups={},
			enabled=true)
	@ImplementsTCMS(id="56329")
	public void FactCheckRhelDistribution_Test() {
		if (client2==null) throw new SkipException("This test requires a second consumer.");
		
		// skip if client1 and client2 are not a Server and Workstation distributions
		String client1RedhatRelease = client1tasks.getRedhatRelease();
		if (!client1RedhatRelease.startsWith("Red Hat Enterprise Linux Server"))
			throw new SkipException("This test requires that client1 is a Red Hat Enterprise Linux Server.");
		String client2RedhatRelease = client2tasks.getRedhatRelease();
		if (!client2RedhatRelease.startsWith("Red Hat Enterprise Linux Workstation"))
			throw new SkipException("This test requires that client2 is a Red Hat Enterprise Linux Workstation.");
	
		// start with fresh registrations using the same clientusername user
		client1tasks.unregister();
		client2tasks.unregister();
		client1tasks.register(clientusername, clientpassword, null, null, null, null);
		client2tasks.register(clientusername, clientpassword, null, null, null, null);

		// get all the pools available to each client
		List<SubscriptionPool> client1Pools = client1tasks.getCurrentlyAvailableSubscriptionPools();
		List<SubscriptionPool> client2Pools = client2tasks.getCurrentlyAvailableSubscriptionPools();
	
		
		
		log.info("Verifying that the pools available to the Workstation consumer are not identitcal to those available to the Server consumer...");
		Assert.assertTrue(!(client1Pools.containsAll(client2Pools) && client2Pools.containsAll(client1Pools)),
				"The subscription pools available to the Workstation and Server are NOT identical");

		// FIXME TODO Verify with development that these are valid asserts
		//log.info("Verifying that the pools available to the Workstation consumer do not contain Server in the ProductName...");

		//log.info("Verifying that the pools available to the Server consumer do not contain Workstation in the ProductName...");

	}
	

	
	// Protected Methods ***********************************************************************


	
	
	// Data Providers ***********************************************************************

}
