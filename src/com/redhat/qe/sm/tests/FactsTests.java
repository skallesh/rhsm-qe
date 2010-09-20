package com.redhat.qe.sm.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.base.ConsumerType;
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


	
	
	// Test Methods ***********************************************************************

	
	@Test(	description="subscription-manager: facts and rules: consumer facts list",
			groups={}, dependsOnGroups={},
			dataProvider="getClientsData",
			enabled=true)
	@ImplementsTCMS(id="56386")
	public void ConsumerFactsList_Test(SSHCommandRunner client) {
		SubscriptionManagerTasks clienttasks = new com.redhat.qe.sm.tasks.SubscriptionManagerTasks(client);
		
		// start with fresh registrations using the same clientusername user
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null);
		
		// list the system facts
		clienttasks.facts(true, false);
	}
	
	
	@Test(	description="subscription-manager: facts and rules: fact check RHEL distribution",
			groups={}, dependsOnGroups={},
			enabled=true)
	@ImplementsTCMS(id="56329")
	public void FactCheckRhelDistribution_Test() {
		
		// skip if client1 and client2 are not a Server and Workstation distributions
		SSHCommandRunner workClient = null,servClient = null;
		SubscriptionManagerTasks workClientTasks = null, servClientTasks = null;
		if (client1!=null && client1tasks.getRedhatRelease().startsWith("Red Hat Enterprise Linux Workstation")) {
			workClient = client1; workClientTasks = client1tasks;
		}
		if (client2!=null && client2tasks.getRedhatRelease().startsWith("Red Hat Enterprise Linux Workstation")) {
			workClient = client2; workClientTasks = client2tasks;
		}
		if (client1!=null && client1tasks.getRedhatRelease().startsWith("Red Hat Enterprise Linux Server")) {
			servClient = client1; servClientTasks = client1tasks;
		}
		if (client2!=null && client2tasks.getRedhatRelease().startsWith("Red Hat Enterprise Linux Server")) {
			servClient = client2; servClientTasks = client2tasks;
		}
		if (workClient==null || servClient==null) {
			throw new SkipException("This test requires a RHEL Workstation client and a RHEL Server client.");
		}
		
		// start with fresh registrations using the same clientusername user
		workClientTasks.unregister();
		servClientTasks.unregister();
		workClientTasks.register(clientusername, clientpassword, null, null, null, null);
		servClientTasks.register(clientusername, clientpassword, null, null, null, null);
		

		// get all the pools available to each client
		List<SubscriptionPool> workClientPools = workClientTasks.getCurrentlyAvailableSubscriptionPools();
		List<SubscriptionPool> servClientPools = servClientTasks.getCurrentlyAvailableSubscriptionPools();
		
		log.info("Verifying that the pools available to the Workstation consumer are not identitcal to those available to the Server consumer...");
		Assert.assertTrue(!(workClientPools.containsAll(servClientPools) && servClientPools.containsAll(workClientPools)),
				"The subscription pools available to the Workstation and Server are NOT identical");

		// FIXME TODO Verify with development that these are valid asserts
		//log.info("Verifying that the pools available to the Workstation consumer do not contain Server in the ProductName...");
		//log.info("Verifying that the pools available to the Server consumer do not contain Workstation in the ProductName...");

	}
	
	@Test(	description="subscription-manager: facts and rules: check sockets",
			groups={}, dependsOnGroups={},
			dataProvider="getClientsData",
			enabled=true)
	//@ImplementsTCMS(id="")
	public void AssertPoolsWithSocketsGreaterThanSystemsCpuSocketAreNotAvailable_Test(SSHCommandRunner client) throws Exception {
		SubscriptionManagerTasks clienttasks = new com.redhat.qe.sm.tasks.SubscriptionManagerTasks(client);
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null);
		assertPoolsWithSocketsGreaterThanSystemsCpuSocketAreNotAvailableOnClient(client);
	}
	
	@Test(	description="subscription-manager: facts and rules: check arch",
			groups={}, dependsOnGroups={},
			dataProvider="getClientsData",
			enabled=true)
	//@ImplementsTCMS(id="")
	public void AssertPoolsWithAnArchDifferentThanSystemsArchitectureAreNotAvailable_Test(SSHCommandRunner client) throws Exception {
		SubscriptionManagerTasks clienttasks = new com.redhat.qe.sm.tasks.SubscriptionManagerTasks(client);
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, null, null, null, null);
		assertPoolsWithAnArchDifferentThanSystemsArchitectureAreNotAvailableOnClient(client);
	}
	
	@Test(	description="subscription-manager: facts and rules: bypass rules due to type",
			groups={}, dependsOnGroups={},
			enabled=true)
	@ImplementsTCMS(id="56331")
	public void BypassRulesDueToType_Test() throws JSONException {
		// determine which client is a RHEL Workstation
		SSHCommandRunner client = null;
		SubscriptionManagerTasks clienttasks = null;
		if (client1!=null && client1tasks.getRedhatRelease().startsWith("Red Hat Enterprise Linux Workstation")) {
			client = client1; clienttasks = client1tasks;
		} else if (client2!=null && client2tasks.getRedhatRelease().startsWith("Red Hat Enterprise Linux Workstation")) {
			client = client2; clienttasks = client2tasks;
		} else {
			throw new SkipException("This test requires a RHEL Workstation client.");
		}

		// on a RHEL workstation register to candlepin (as type system)
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, ConsumerType.system, null, null, null);

		// get a list of available pools and all available pools (for this system consumer)
		List<SubscriptionPool> compatiblePoolsAsSystemConsumer = clienttasks.getCurrentlyAvailableSubscriptionPools();
		List<SubscriptionPool> allPoolsAsSystemConsumer = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		
		Assert.assertFalse(compatiblePoolsAsSystemConsumer.containsAll(allPoolsAsSystemConsumer),
				"Without bypassing the rules, not *all* pools are available for subscribing by a type=system consumer.");
		Assert.assertTrue(allPoolsAsSystemConsumer.containsAll(compatiblePoolsAsSystemConsumer),
				"The pools available to a type=system consumer is a subset of --all --available pools.");
		
		// now register to candlepin (as type candlepin)
		clienttasks.unregister();
		clienttasks.register(clientusername, clientpassword, ConsumerType.candlepin, null, null, null);

		// get a list of available pools and all available pools (for this candlepin consumer)
		List<SubscriptionPool> compatiblePoolsAsCandlepinConsumer = clienttasks.getCurrentlyAvailableSubscriptionPools();
		List<SubscriptionPool> allPoolsAsCandlepinConsumer = clienttasks.getCurrentlyAllAvailableSubscriptionPools();

		Assert.assertTrue(compatiblePoolsAsCandlepinConsumer.containsAll(allPoolsAsCandlepinConsumer) && allPoolsAsCandlepinConsumer.containsAll(compatiblePoolsAsCandlepinConsumer),
				"The pools available to a type=candlepin consumer bypass the rules (list --all --available is identical to list --available).");
	
		// now assert that all the pools can be subscribed to by the consumer (registered as type candlepin)
		clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools(ConsumerType.candlepin);
	}
	
	
	// Protected Methods ***********************************************************************

	
//	protected void registerClients(ConsumerType type) {
//
//		// start with fresh registrations using the same clientusername user
//		client1tasks.unregister();
//		client2tasks.unregister();
//		client1tasks.register(clientusername, clientpassword, type, null, null, null);
//		client2tasks.register(clientusername, clientpassword, type, null, null, null);
//	}
	
	
	protected void assertPoolsWithSocketsGreaterThanSystemsCpuSocketAreNotAvailableOnClient(SSHCommandRunner client) throws Exception {
		SubscriptionManagerTasks clienttasks = new com.redhat.qe.sm.tasks.SubscriptionManagerTasks(client);
		boolean foundPoolWithSocketAttributes = false;
		boolean conclusiveTest = false;
		
		// get all the pools available to each client
		List<SubscriptionPool> clientPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		
		// get the number of cpu_sockets for this system consumer
		String factName = "cpu.cpu_socket(s)";
		int systemValue = Integer.valueOf(clienttasks.getFactValue(factName));
		log.info(factName+" for this system consumer: "+systemValue);
		
		// loop through the subscriptions
		JSONArray jsonSubscriptions = new JSONArray(CandlepinTasks.getResourceREST(serverHostname,serverPort,clientOwnerUsername,clientOwnerPassword,"/subscriptions"));	
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
					int poolValue = jsonAttribute.getInt("value");
					
					// assert that if the maximum cpu_sockets for this subscription pool is greater than the cpu_sockets facts for this consumer, then this product should NOT be available
					log.fine("Maximum sockets for this subscriptionPool name="+subscriptionName+": "+poolValue);
					SubscriptionPool pool = new SubscriptionPool(productId,poolId);
					if (poolValue < systemValue) {
						Assert.assertFalse(clientPools.contains(pool), "Subscription Pool "+pool+" IS NOT available since this system's "+factName+" ("+systemValue+") exceeds the maximum ("+poolValue+") for this pool to be a candidate for availability.");
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
	
	
	protected void assertPoolsWithAnArchDifferentThanSystemsArchitectureAreNotAvailableOnClient(SSHCommandRunner client) throws Exception {
		SubscriptionManagerTasks clienttasks = new com.redhat.qe.sm.tasks.SubscriptionManagerTasks(client);
		boolean foundPoolWithArchAttributes = false;
		boolean conclusiveTest = false;
		
		// get all the pools available to each client
		List<SubscriptionPool> clientPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		
		// get the number of cpu_sockets for this system consumer
		String factName = "cpu.architecture";
		String systemValue = clienttasks.getFactValue(factName);
		log.info(factName+" for this system consumer: "+systemValue);
		
		// loop through the subscriptions
		JSONArray jsonSubscriptions = 
			new JSONArray(CandlepinTasks.getResourceREST(serverHostname,serverPort,clientOwnerUsername,clientOwnerPassword,"/subscriptions"));	
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
				if (attributeName.equals("arch")) {
					// found the arch attribute - get its value
					foundPoolWithArchAttributes = true;
					String poolValue = jsonAttribute.getString("value");
					
					// assert that if the maximum cpu_sockets for this subscription pool is greater than the cpu_sockets facts for this consumer, then this product should NOT be available
					log.fine("Arch for this subscriptionPool name="+subscriptionName+": "+poolValue);
					SubscriptionPool pool = new SubscriptionPool(productId,poolId);
					if (!poolValue.equalsIgnoreCase(systemValue) && !poolValue.equalsIgnoreCase("ALL")) {
						Assert.assertFalse(clientPools.contains(pool), "Subscription Pool "+pool+" IS NOT available since this system's "+factName+" ("+systemValue+") does not match ("+poolValue+") for this pool to be a candidate for availability.");
						conclusiveTest = true;
					} else {
						log.info("Subscription Pool "+pool+" may or may not be available depending on other facts besides "+factName+".");
					}
					break;
				}
			}
		}
		if (!conclusiveTest) log.warning("The facts for this system did not allow us to perform a conclusive test.");
		Assert.assertTrue(foundPoolWithArchAttributes,"At least one Subscription Pools was found for which we could attempt this test.");
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
