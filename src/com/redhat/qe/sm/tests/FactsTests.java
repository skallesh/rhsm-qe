package com.redhat.qe.sm.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testng.BlockedByBzBug;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.sm.tasks.CandlepinTasks;
import com.redhat.qe.sm.tasks.SubscriptionManagerTasks;
import com.redhat.qe.tools.SSHCommandRunner;



/**
 * @author jsefler
 */
@Test(groups={"facts"})
public class FactsTests extends SubscriptionManagerTestScript{
	
	// Configuration Methods ***********************************************************************

	@BeforeClass(groups={"setup"})
	public void registerBeforeFactsTests() {

		// start with fresh registrations using the same clientusername user
		client1tasks.unregister();
		client2tasks.unregister();
		client1tasks.register(clientusername, clientpassword, null, null, null, null);
		client2tasks.register(clientusername, clientpassword, null, null, null, null);

	}
	
	
	// Test Methods ***********************************************************************

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
	
	@Test(	description="subscription-manager: facts and rules: check sockets",
			groups={"myDevGroup","FactCheckRhelDistribution_Test"}, dependsOnGroups={},
			dataProvider="getClientsData",
			enabled=true)
	@ImplementsTCMS(id="56329")
	public void AssertPoolsWithSocketsGreaterThanSystemSocketFactsAreNotAvailable_Test(SSHCommandRunner client) throws JSONException {
		assertPoolsWithSocketsGreaterThanSystemSocketFactsAreNotAvailableOnClient(client);
	}
	

	
	// Protected Methods ***********************************************************************

	public void assertPoolsWithSocketsGreaterThanSystemSocketFactsAreNotAvailableOnClient(SSHCommandRunner client) throws JSONException {
		SubscriptionManagerTasks clienttasks = new com.redhat.qe.sm.tasks.SubscriptionManagerTasks(client);
		boolean foundPoolWithSocketAttributes = false;
		boolean conclusiveTest = false;
		
		// get all the pools available to each client
		List<SubscriptionPool> clientPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		
		// get the number of cpu_sockets for this system consumer
		String factName = "cpu.cpu_socket(s)";
		int systemSockets = Integer.valueOf(clienttasks.getFactValue(factName));
		log.info(factName+" for this system consumer: "+systemSockets);
		
		// loop through the subscriptions
		JSONArray jsonSubscriptions = CandlepinTasks.curl_hateoas_ref(client,serverHostname,serverPort,clientOwnerUsername,clientOwnerPassword,"subscriptions");	
		for (int i = 0; i < jsonSubscriptions.length(); i++) {
			JSONObject jsonSubscription = (JSONObject) jsonSubscriptions.get(i);
			int poolId = jsonSubscription.getInt("id");
			JSONObject jsonProduct = (JSONObject) jsonSubscription.getJSONObject("product");
			String subscriptionName = jsonProduct.getString("name");
			String productId = jsonProduct.getString("id");
			JSONArray jsonAttributes = jsonProduct.getJSONArray("attributes");
			// loop through the attributes of this subscription looking for the "sockets" attribute
			for (int j = 0; j < jsonAttributes.length(); j++) {
				JSONObject jsonAttribute = (JSONObject) jsonAttributes.get(j);
				String attributeName = jsonAttribute.getString("name");
				if (attributeName.equals("sockets")) {
					// found the sockets attribute - get its value
					foundPoolWithSocketAttributes = true;
					int value = jsonAttribute.getInt("value");
					
					// assert that if the maximum cpu_sockets for this subscription pool is greater than the cpu_sockets facts for this consumer, then this product should NOT be available
					log.info("Maximum sockets for this subscriptionPool name="+subscriptionName+": "+value);
					SubscriptionPool pool = new SubscriptionPool(productId,poolId);
					if (value < systemSockets) {
						Assert.assertFalse(clientPools.contains(pool), "Subscription Pool "+pool+" IS NOT available since this system's "+factName+" ("+systemSockets+") exceeds the maximum ("+value+") for this pool to be a candidate for availability.");
						conclusiveTest = true;
					} else {
						log.info("Subscription Pool "+pool+" may or may not be available depending on other facts besides "+factName+".");
					}
					break;
				}
			}
		}
		if (!conclusiveTest) log.warning("The facts for this system did not allow us to perform a conclusive test.");
		Assert.assertTrue(foundPoolWithSocketAttributes,"At least one Subscription Pools was found for which we could attempt this test.");
	}
	
	
	// Data Providers ***********************************************************************

	
	@DataProvider(name="getClientsData")
	public Object[][] getClientsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(ggetClientsDataAsListOfLists());
	}
	protected List<List<Object>> ggetClientsDataAsListOfLists(){
		List<List<Object>> ll = new ArrayList<List<Object>>();

		// SSHCommandRunner client
		if (client1!= null)	ll.add(Arrays.asList(new Object[]{client1}));
		if (client2!= null)	ll.add(Arrays.asList(new Object[]{client2}));

		return ll;
	}
}
