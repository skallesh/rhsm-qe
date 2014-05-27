package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.ProductNamespace;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;

/**
 * @author jsefler
 *
 * Reference Design Doc:
 * https://engineering.redhat.com/trac/Entitlement/wiki/DataCenterSkuDesign
 * More Information :: https://docspace.corp.redhat.com/docs/DOC-145057
 */
@Test(groups={"DataCenterTests","Tier2Tests"})
public class DataCenterTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************

	@Test(	description="given an available data center pool, consume it and assert that a pool for the derivedProduct is generated and available only to its guests",
			groups={"AcceptanceTests","Tier1Tests","VerifyAvailabilityOfDerivedProductSubpools_Test"},
			dataProvider="getAvailableDataCenterSubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAvailabilityOfDerivedProductSubpools_Test(Object bugzilla, /*Boolean systemIsGuest, Integer systemSockets,*/ SubscriptionPool pool) throws NumberFormatException, JSONException, Exception {
		
		// make sure we are unsubscribed from all subscriptions
		clienttasks.unsubscribe(true, (BigInteger)null, null, null, null);
		
		// get some attributes from the subscription pool
		String poolDerivedProductId = (String)CandlepinTasks.getPoolValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "derivedProductId");
		String poolDerivedProductName = (String)CandlepinTasks.getPoolValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "derivedProductName");
		List<String> poolDerivedProvidedProductIds = CandlepinTasks.getPoolDerivedProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId);
		String poolVirtLimit = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "virt_limit");
		List<String> poolProvidedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId);
		
		// assert that the derivedProductId is different from the host pool's productId
		Assert.assertNotSame(pool.productId, poolDerivedProductId, "The host pool's data center subscription product Id '"+pool.productId+"' should be different than its derived pool's product Id '"+poolDerivedProductId+"'.");
		
		// assert that the derivedProductName is different from the host pool's subscription name
		Assert.assertNotSame(pool.subscriptionName, poolDerivedProductName, "The host pool's data center subscription name '"+pool.subscriptionName+"' should be different than its derived pool's product name '"+poolDerivedProductName+"'.");
		
		// instrument the system facts to behave as a physical host
		factsMap.put("virt.is_guest",String.valueOf(false));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null,true,null,null,null);	// update facts
		
		// reset a few fake guest ids for this host consumer
		String systemUuid = clienttasks.getCurrentConsumerId();
		//[root@jsefler-5 ~]# curl -k -u testuser1:password --request PUT --data '{"guestIds":["e6f55b91-aae1-44d6-f0db-c8f25ec73ef5","abcd"]}' --header 'accept:application/json' --header 'content-type: application/json' https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/consumers/d2ee0c6e-a57d-4e37-8be3-228a44ca2739 
		JSONObject jsonConsumer = CandlepinTasks.setGuestIdsForConsumer(sm_clientUsername,sm_clientPassword, sm_serverUrl, systemUuid,Arrays.asList(new String[]{"abc","def"}));
		
		// assert that only Physical pools are available for consumption for this data center sku
		for (SubscriptionPool subscriptionPool : SubscriptionPool.findAllInstancesWithMatchingFieldFromList("productId", pool.productId, clienttasks.getCurrentlyAllAvailableSubscriptionPools())) {
			Assert.assertEquals(subscriptionPool.machineType, "Physical", "Only physical pools to '"+pool.productId+"' should be available to a physical host system.");
		}
		
		// subscribe the host to the data center pool
		File hostEntitlementFile = clienttasks.subscribeToSubscriptionPool(pool,/*sm_serverAdminUsername*/sm_clientUsername,/*sm_serverAdminPassword*/sm_clientPassword,sm_serverUrl);
		EntitlementCert hostEntitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(hostEntitlementFile); client.runCommandAndWait("rct cat-cert "+hostEntitlementFile);
		
		// in general the data center pool will not provide any engineering products
		Assert.assertTrue(poolProvidedProductIds.isEmpty(), "In general, a data center product subscription will not provide any engineering products (productId= '"+pool.productId+"').  Asserting the providedProducts from the subscription is empty...");		
		Assert.assertTrue(hostEntitlementCert.productNamespaces.isEmpty(), "In general, a data center product subscription will not provide any engineering products (productId= '"+pool.productId+"').  Asserting the productNamespaces from the granted entitlement are empty...");		
	
		// in general the data center pool will not provide any content
		Assert.assertTrue(hostEntitlementCert.contentNamespaces.isEmpty(), "In general, a data center product subscription will not provide any content sets (productId= '"+pool.productId+"').");		

		
		// assert that the derivedProductId is NOT available to the Physical host system
		List<SubscriptionPool> availablePoolsForDerivedProductId = SubscriptionPool.findAllInstancesWithMatchingFieldFromList("productId", poolDerivedProductId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertTrue(availablePoolsForDerivedProductId.isEmpty(),"A subpool for the derivedProductId '"+poolDerivedProductId+"' should NOT be available to the host after (or before) it consumes the data center product subscription.");
		
		// now we can assert that a host_limited subpool was generated from consumption of this physical pool and is only available to guests of this physical system
		// first, let's flip the virt.is_guest to true and assert that the virtual guest subpool is not (yet) available since the virtUuid is not on the host consumer's list of guestIds
		factsMap.put("virt.is_guest",String.valueOf(true));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null,true,null,null,null);	// update facts
		availablePoolsForDerivedProductId = SubscriptionPool.findAllInstancesWithMatchingFieldFromList("productId", poolDerivedProductId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertTrue(availablePoolsForDerivedProductId.isEmpty(),"A subpool for the derivedProductId should NOT be available to a guest system when its virt_uuid is not on the host's list of guestIds.");
		
			
		// now fake this consumer's facts and guestIds to make it think it is a guest of itself (a trick for testing)
		factsMap.put("virt.uuid",systemUuid);
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null,true,null,null,null);	// update facts
		//[root@jsefler-5 ~]# curl -k -u testuser1:password --request PUT --data '{"guestIds":["e6f55b91-aae1-44d6-f0db-c8f25ec73ef5","abcd"]}' --header 'accept:application/json' --header 'content-type: application/json' https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/consumers/d2ee0c6e-a57d-4e37-8be3-228a44ca2739 
		jsonConsumer = CandlepinTasks.setGuestIdsForConsumer(sm_clientUsername,sm_clientPassword, sm_serverUrl, systemUuid,Arrays.asList(new String[]{"abc",systemUuid,"def"}));
		
		// now the host_limited subpool to the derivedProductId for this virtual system should be available
		availablePoolsForDerivedProductId = SubscriptionPool.findAllInstancesWithMatchingFieldFromList("productId", poolDerivedProductId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertTrue(!availablePoolsForDerivedProductId.isEmpty(),"Host_limited subpool from data center product id '"+pool.productId+"' to derived product id '"+poolDerivedProductId+"' is available to its guest.");
		Assert.assertEquals(availablePoolsForDerivedProductId.size(),1,"Only one host_limited subpool to derived product id '"+poolDerivedProductId+"' is available to its guest.");
		SubscriptionPool derivedPool = availablePoolsForDerivedProductId.get(0);
		Assert.assertEquals(derivedPool.subscriptionName, poolDerivedProductName, "Subscription name for the derived product id '"+poolDerivedProductId+"'.");
		Assert.assertEquals(derivedPool.quantity.toLowerCase(),poolVirtLimit,"The quantity of entitlements from the host_limited subpool to derived product subscription '"+poolDerivedProductName+"' should be the same as the host data center subscription's virt_limit '"+poolVirtLimit+"'.");
		
		// now subscribe to the derived subpool and we'll assert the entitlement values come from the derived product and not the originating data center subscription
		
		// subscribe the guest to the derived product subscription
		File derivedEntitlementFile = clienttasks.subscribeToSubscriptionPool(derivedPool,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		EntitlementCert derivedEntitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(derivedEntitlementFile); client.runCommandAndWait("rct cat-cert "+derivedEntitlementFile);
		
		// assert all of the derived provided products are included in the entitlement
		List<String> actualDerivedProvidedProductIds = new ArrayList<String>();
		for (ProductNamespace productNamespace : derivedEntitlementCert.productNamespaces) {
			actualDerivedProvidedProductIds.add(productNamespace.id);
		}
		Assert.assertTrue(actualDerivedProvidedProductIds.containsAll(poolDerivedProvidedProductIds)&&poolDerivedProvidedProductIds.containsAll(actualDerivedProvidedProductIds),
			"The actual product ids "+actualDerivedProvidedProductIds+" provided by an entitlement cert from the derived subpool '"+derivedPool.subscriptionName+"' match the expected derivedProvidedProducts "+poolDerivedProvidedProductIds+" from the data center subscription '"+pool.subscriptionName+"'.");
		
		// assert the derivedProductAttributes are reflected in the entitlement cert granted from the derived subpool
		
		//	Order:											Order:
		//		Name: Awesome OS Server Basic (data center)		Name: Awesome OS Server Basic (dc-virt)
		//		Number: order-8675309							Number: order-8675309
		//		SKU: awesomeos-server-basic-dc					SKU: awesomeos-server-basic-vdc
		//		Contract: 18									Contract: 18
		//		Account: 12331131231							Account: 12331131231
		//		Service Level: None								Service Level: Full-Service
		//		Service Type: Self-Support						Service Type: Drive-Through
		//		Quantity: 5										Quantity: 5
		//		Quantity Used: 1								Quantity Used: 1
		//		Socket Limit: 4									Socket Limit: 2
		//		RAM Limit: 										RAM Limit: 2
		//		Core Limit: 									Core Limit: 4
		//		Virt Limit: 									Virt Limit: 
		//		Virt Only: False								Virt Only: True
		//		Subscription: 									Subscription: 
		//		Stacking ID: 									Stacking ID: 
		//		Warning Period: 30								Warning Period: 0
		//		Provides Management: False						Provides Management: False
			
		//		Name: Awesome OS Server Basic (data center)		Name: Awesome OS Server Basic (dc-virt)
		Assert.assertEquals(hostEntitlementCert.orderNamespace.productName, pool.subscriptionName,																														"hostEntitlementCert.orderNamespace.productName should match the data center pool's subscription name");
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.productName, poolDerivedProductName,																													"derivedEntitlementCert.orderNamespace.productName should match the derivedProductName");
		
		//		Number: order-8675309							Number: order-8675309
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.orderNumber, hostEntitlementCert.orderNamespace.orderNumber,																							"Order Number from the derived entitlement should match the host entitlement");
		
		//		SKU: awesomeos-server-basic-dc					SKU: awesomeos-server-basic-vdc
		Assert.assertEquals(hostEntitlementCert.orderNamespace.productId, pool.productId,																																"hostEntitlementCert.orderNamespace.productId should match the data center pool's productId");
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.productId, poolDerivedProductId,																														"derivedEntitlementCert.orderNamespace.productId should match the derivedProductId");

		//		Contract: 18									Contract: 18
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.contractNumber, hostEntitlementCert.orderNamespace.contractNumber,																					"Contract Number from the derived entitlement should match the host entitlement");

		//		Account: 12331131231							Account: 12331131231
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.accountNumber, hostEntitlementCert.orderNamespace.accountNumber,																						"Account Number from the derived entitlement should match the host entitlement");

		//		Service Level: None								Service Level: Full-Service
		Assert.assertEquals(hostEntitlementCert.orderNamespace.supportLevel, pool.serviceLevel,																															"hostEntitlementCert.orderNamespace.supportLevel should match the data center pool's serviceLevel");
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.supportLevel, CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "support_level"),	"derivedEntitlementCert.orderNamespace.supportLevel should match the derivedProductAttribute support_level");

		//		Service Type: Self-Support						Service Type: Drive-Through
		Assert.assertEquals(hostEntitlementCert.orderNamespace.supportType, pool.serviceType,																															"hostEntitlementCert.orderNamespace.supportType should match the data center pool's serviceType");
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.supportType, CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "support_type"),		"derivedEntitlementCert.orderNamespace.supportType should match the derivedProductAttribute support_type");

		//		Quantity: 5										Quantity: 5
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "983193"; boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping the assertion of quantity while bug '"+bugId+"' is open.");
		} else {
		// END OF WORKAROUND
		if (clienttasks.isPackageVersion("subscription-manager","<","1.10.3-1") && hostEntitlementCert.orderNamespace.quantity.equals("-1")) {log.warning("The rct cat-cert tool encountered a Quantity of -1 which is fixed in subscription-manager-1.10.3-1.  Skipping assertion.");} else	// Bug 1011961 - rct cat-cert should display "Unlimited" for Quantity instead of "-1";  subscription-manager commit 7554c869608a0276151993d34fee4ddb54185f7a
		Assert.assertEquals(hostEntitlementCert.orderNamespace.quantity, pool.quantity,																																	"hostEntitlementCert.orderNamespace.quantity should match the data center pool's quantity");
		if (clienttasks.isPackageVersion("subscription-manager","<","1.10.3-1") && derivedEntitlementCert.orderNamespace.quantity.equals("-1")) {log.warning("The rct cat-cert tool encountered a Quantity of -1 which is fixed in subscription-manager-1.10.3-1.  Skipping assertion.");} else	// Bug 1011961 - rct cat-cert should display "Unlimited" for Quantity instead of "-1";  subscription-manager commit 7554c869608a0276151993d34fee4ddb54185f7a
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.quantity, derivedPool.quantity,																														"derivedEntitlementCert.orderNamespace.quantity should match the derivedPool's quantity");
		}
		
		//		Quantity Used: 1								Quantity Used: 1
		//TODO
		
		//		Socket Limit: 4									Socket Limit: 2
		Assert.assertEquals(hostEntitlementCert.orderNamespace.socketLimit, CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "sockets"),					"hostEntitlementCert.orderNamespace.socketLimit should match the data center pool's productAttribute sockets");
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.socketLimit, CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "sockets"),			"derivedEntitlementCert.orderNamespace.socketLimit should match the derivedProductAttribute sockets");

		//		RAM Limit: 										RAM Limit: 2
		Assert.assertEquals(hostEntitlementCert.orderNamespace.ramLimit, CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "ram"),							"hostEntitlementCert.orderNamespace.ramLimit should match the data center pool's productAttribute ram");
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.ramLimit, CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "ram"),					"derivedEntitlementCert.orderNamespace.ramLimit should match the derivedProductAttribute ram");

		//		Core Limit: 									Core Limit: 4
		Assert.assertEquals(hostEntitlementCert.orderNamespace.coreLimit, CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "cores"),						"hostEntitlementCert.orderNamespace.coreLimit should match the data center pool's productAttribute cores");
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.coreLimit, CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "cores"),				"derivedEntitlementCert.orderNamespace.coreLimit should match the derivedProductAttribute cores");

		//		Virt Limit: 									Virt Limit: 
		//TODO ignoring for now based on https://bugzilla.redhat.com/show_bug.cgi?id=983193#c2
		
		//		Virt Only: False								Virt Only: True
		String virtOnly = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "virt_only");
		Assert.assertEquals(hostEntitlementCert.orderNamespace.virtOnly, virtOnly==null?Boolean.valueOf(false):Boolean.valueOf(virtOnly),																				"hostEntitlementCert.orderNamespace.virtOnly should match the data center pool's productAttribute virt_only");
		virtOnly = String.valueOf(true);/* the derived pool virt_only should ALWAYS be true */	//CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "virt_only");
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.virtOnly, virtOnly==null?Boolean.valueOf(false):Boolean.valueOf(virtOnly),																			"derivedEntitlementCert.orderNamespace.virtOnly should match the derivedProductAttribute virt_only");

		//		Subscription: 									Subscription: 
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.subscriptionNumber, hostEntitlementCert.orderNamespace.subscriptionNumber,																			"Subscription from the derived entitlement should match the host entitlement");
		
		//		Stacking ID: 									Stacking ID: 
		Assert.assertEquals(hostEntitlementCert.orderNamespace.stackingId, CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "stacking_id"),					"hostEntitlementCert.orderNamespace.stackingId should match the data center pool's productAttribute stacking_id");
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.stackingId, CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "stacking_id"),		"derivedEntitlementCert.orderNamespace.stackingId should match the derivedProductAttribute stacking_id");
		
		//		Warning Period: 30								Warning Period: 0
		String warningPeriod = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "warning_period");
		Assert.assertEquals(hostEntitlementCert.orderNamespace.warningPeriod, warningPeriod==null?"0":warningPeriod,																									"hostEntitlementCert.orderNamespace.warningPeriod should match the data center pool's productAttribute warning_period");
		warningPeriod = CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "warning_period");
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.warningPeriod, warningPeriod==null?"0":warningPeriod,																									"derivedEntitlementCert.orderNamespace.warningPeriod should match the derivedProductAttribute warning_period");
		
		//		Provides Management: False						Provides Management: False
		String providesManagement = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "management_enabled");
		Assert.assertEquals(hostEntitlementCert.orderNamespace.providesManagement, providesManagement==null?Boolean.valueOf(false):Boolean.valueOf(providesManagement),													"hostEntitlementCert.orderNamespace.providesManagement should match the data center pool's productAttribute management_enabled");
		providesManagement = CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "management_enabled");
		Assert.assertEquals(derivedEntitlementCert.orderNamespace.providesManagement, providesManagement==null?Boolean.valueOf(false):Boolean.valueOf(providesManagement),												"derivedEntitlementCert.orderNamespace.providesManagement should match the derivedProductAttribute management_enabled");
		
		// for the sake of cleanup and to avoid this candlepin errors, let's unsubscribe from derivedEntitlementCert and then hostEntitlementCert
		//	ssh root@jsefler-6server.usersys.redhat.com subscription-manager unregister
		//	Stdout: Runtime Error No row with the given identifier exists: [org.candlepin.model.ProvidedProduct#8a90869341e61f7c0141e84e9ade3efd] at org.hibernate.UnresolvableObjectException.throwIfNull:65
		//	Stderr:
		//	ExitCode: 255
		//	
		//	ssh root@jsefler-6server.usersys.redhat.com subscription-manager unsubscribe --all
		//	Stdout:
		//	Stderr: Runtime Error No row with the given identifier exists: [org.candlepin.model.DerivedProvidedProduct#8a90869341e61f7c0141e84f2b9a3f0e] at org.hibernate.UnresolvableObjectException.throwIfNull:65
		//	ExitCode: 255
		clienttasks.unsubscribeFromSerialNumber(derivedEntitlementCert.serialNumber);
		clienttasks.unsubscribeFromSerialNumber(hostEntitlementCert.serialNumber);
	}
	
	@AfterGroups(value={"VerifyAvailabilityOfDerivedProductSubpools_Test"},groups={"setup"})
	public void afterVerifyAvailabilityOfDerivedProductSubpools_Test() {
		clienttasks.unregister(null,null,null);
		clienttasks.deleteFactsFileWithOverridingValues();
	}
	Map<String,String> factsMap = new HashMap<String,String>();
	
	@DataProvider(name="getAvailableDataCenterSubscriptionPoolsData")
	public Object[][] getAvailableDataCenterSubscriptionPoolsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAvailableDataCenterSubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAvailableDataCenterSubscriptionPoolsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// instrument the system to be a Physical host
		factsMap.clear();
		factsMap.put("virt.is_guest",String.valueOf(false));
		// set system facts that will always make the subscription available
		factsMap.put("memory.memtotal", "1");			// ram
		factsMap.put("cpu.cpu_socket(s)", "1");			// sockets
		factsMap.put("cpu.core(s)_per_socket", "1");	// cores
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		
		for (List<Object> list : getAvailableSubscriptionPoolsDataAsListOfLists(false)) {
			SubscriptionPool pool = (SubscriptionPool)(list.get(0));
			
			if (CandlepinTasks.isPoolADataCenter(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId)) {
				ll.add(Arrays.asList(new Object[]{null,	pool}));
			}
		}
		return ll;
	}
	
	
	
	
	
	
	// Candidates for an automated Test:
	
	
	
	
	// Configuration methods ***********************************************************************


	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}


/*
[root@jsefler-5 ~]# subscription-manager list --avail | grep 8a90f8203fc4ca73013fc4cbed48045a -B2 -A8
Subscription Name: Awesome OS Server Basic (data center)
SKU:               awesomeos-server-basic-dc
Pool ID:           8a90f8203fc4ca73013fc4cbed48045a
Quantity:          10
Service Level:     None
Service Type:      Self-Support
Multi-Entitlement: No
Ends:              07/08/2014
System Type:       Physical

[root@jsefler-5 ~]# curl -k --stderr /dev/null -u admin:admin https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/pools/8a90f8203fc4ca73013fc4cbed48045a | python -m simplejson/tool
{
    "accountNumber": "12331131231", 
    "activeSubscription": true, 
    "attributes": [], 
    "calculatedAttributes": {}, 
    "consumed": 0, 
    "contractNumber": "19", 
    "created": "2013-07-09T18:57:46.312+0000", 
    "derivedProductAttributes": [
        {
            "created": "2013-07-09T18:57:46.312+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed48045b", 
            "name": "support_type", 
            "productId": "awesomeos-server-basic-vdc", 
            "updated": "2013-07-09T18:57:46.312+0000", 
            "value": "Drive-Through"
        }, 
        {
            "created": "2013-07-09T18:57:46.312+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed48045c", 
            "name": "sockets", 
            "productId": "awesomeos-server-basic-vdc", 
            "updated": "2013-07-09T18:57:46.312+0000", 
            "value": "2"
        }, 
        {
            "created": "2013-07-09T18:57:46.312+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed48045d", 
            "name": "skip_subs", 						// <mstead> jsefler: skip_subs was only used when loading test data -- nothing candlepin related. Just ended up in the attributes list. <mstead> jsefler: I've made is so that it will no longer show up in imported data
            "productId": "awesomeos-server-basic-vdc", 
            "updated": "2013-07-09T18:57:46.312+0000", 
            "value": "true"
        }, 
        {
            "created": "2013-07-09T18:57:46.312+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed48045e", 
            "name": "arch", 
            "productId": "awesomeos-server-basic-vdc", 
            "updated": "2013-07-09T18:57:46.312+0000", 
            "value": "x86_64,x86,s390x,ppc64,ia64,arm"
        }, 
        {
            "created": "2013-07-09T18:57:46.312+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed48045f", 
            "name": "cores", 
            "productId": "awesomeos-server-basic-vdc", 
            "updated": "2013-07-09T18:57:46.312+0000", 
            "value": "4"
        }, 
        {
            "created": "2013-07-09T18:57:46.312+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed480460", 
            "name": "support_level", 
            "productId": "awesomeos-server-basic-vdc", 
            "updated": "2013-07-09T18:57:46.312+0000", 
            "value": "Full-Service"
        }, 
        {
            "created": "2013-07-09T18:57:46.312+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed480461", 
            "name": "ram", 
            "productId": "awesomeos-server-basic-vdc", 
            "updated": "2013-07-09T18:57:46.312+0000", 
            "value": "2"
        }, 
        {
            "created": "2013-07-09T18:57:46.312+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed480462", 
            "name": "variant", 
            "productId": "awesomeos-server-basic-vdc", 
            "updated": "2013-07-09T18:57:46.312+0000", 
            "value": "ALL"
        }, 
        {
            "created": "2013-07-09T18:57:46.312+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed480463", 
            "name": "type", 
            "productId": "awesomeos-server-basic-vdc", 
            "updated": "2013-07-09T18:57:46.312+0000", 
            "value": "MKT"
        }, 
        {
            "created": "2013-07-09T18:57:46.312+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed480464", 
            "name": "version", 
            "productId": "awesomeos-server-basic-vdc", 
            "updated": "2013-07-09T18:57:46.312+0000", 
            "value": "0.1"
        }
    ], 
    "derivedProductId": "awesomeos-server-basic-vdc", 
    "derivedProductName": "Awesome OS Server Basic (dc-virt)", 
    "derivedProvidedProducts": [
        {
            "created": "2013-07-09T18:57:46.313+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed490465", 
            "productId": "37060", 
            "productName": "Awesome OS Server Bits", 
            "updated": "2013-07-09T18:57:46.313+0000"
        }
    ], 
    "endDate": "2014-07-09T00:00:00.000+0000", 
    "exported": 0, 
    "href": "/pools/8a90f8203fc4ca73013fc4cbed48045a", 
    "id": "8a90f8203fc4ca73013fc4cbed48045a", 
    "orderNumber": "order-8675309", 
    "owner": {
        "displayName": "Admin Owner", 
        "href": "/owners/admin", 
        "id": "8a90f8203fc4ca73013fc4ca93080002", 
        "key": "admin"
    }, 
    "productAttributes": [
        {
            "created": "2013-07-09T18:57:46.313+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed490466", 
            "name": "version", 
            "productId": "awesomeos-server-basic-dc", 
            "updated": "2013-07-09T18:57:46.313+0000", 
            "value": "1.0"
        }, 
        {
            "created": "2013-07-09T18:57:46.313+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed490467", 
            "name": "arch", 
            "productId": "awesomeos-server-basic-dc", 
            "updated": "2013-07-09T18:57:46.313+0000", 
            "value": "ALL"
        }, 
        {
            "created": "2013-07-09T18:57:46.313+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed490468", 
            "name": "sockets", 
            "productId": "awesomeos-server-basic-dc", 
            "updated": "2013-07-09T18:57:46.313+0000", 
            "value": "4"
        }, 
        {
            "created": "2013-07-09T18:57:46.313+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed490469", 
            "name": "management_enabled", 
            "productId": "awesomeos-server-basic-dc", 
            "updated": "2013-07-09T18:57:46.313+0000", 
            "value": "0"
        }, 
        {
            "created": "2013-07-09T18:57:46.313+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed49046a", 
            "name": "multi-entitlement", 
            "productId": "awesomeos-server-basic-dc", 
            "updated": "2013-07-09T18:57:46.313+0000", 
            "value": "no"
        }, 
        {
            "created": "2013-07-09T18:57:46.313+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed49046b", 
            "name": "variant", 
            "productId": "awesomeos-server-basic-dc", 
            "updated": "2013-07-09T18:57:46.313+0000", 
            "value": "ALL"
        }, 
        {
            "created": "2013-07-09T18:57:46.313+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed49046c", 
            "name": "support_level", 
            "productId": "awesomeos-server-basic-dc", 
            "updated": "2013-07-09T18:57:46.313+0000", 
            "value": "None"
        }, 
        {
            "created": "2013-07-09T18:57:46.313+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed49046d", 
            "name": "warning_period", 
            "productId": "awesomeos-server-basic-dc", 
            "updated": "2013-07-09T18:57:46.313+0000", 
            "value": "30"
        }, 
        {
            "created": "2013-07-09T18:57:46.313+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed49046e", 
            "name": "host_limited", 
            "productId": "awesomeos-server-basic-dc", 
            "updated": "2013-07-09T18:57:46.313+0000", 
            "value": "true"
        }, 
        {
            "created": "2013-07-09T18:57:46.313+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed49046f", 
            "name": "support_type", 
            "productId": "awesomeos-server-basic-dc", 
            "updated": "2013-07-09T18:57:46.313+0000", 
            "value": "Self-Support"
        }, 
        {
            "created": "2013-07-09T18:57:46.313+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed490470", 
            "name": "virt_limit", 
            "productId": "awesomeos-server-basic-dc", 
            "updated": "2013-07-09T18:57:46.313+0000", 
            "value": "unlimited"
        }, 
        {
            "created": "2013-07-09T18:57:46.313+0000", 
            "id": "8a90f8203fc4ca73013fc4cbed490471", 
            "name": "type", 
            "productId": "awesomeos-server-basic-dc", 
            "updated": "2013-07-09T18:57:46.313+0000", 
            "value": "MKT"
        }
    ], 
    "productId": "awesomeos-server-basic-dc", 
    "productName": "Awesome OS Server Basic (data center)", 
    "providedProducts": [], 
    "quantity": 10, 
    "restrictedToUsername": null, 
    "sourceEntitlement": null, 
    "startDate": "2013-07-09T00:00:00.000+0000", 
    "subscriptionId": "8a90f8203fc4ca73013fc4caef1200bf", 
    "subscriptionSubKey": "master", 
    "updated": "2013-07-09T18:57:46.312+0000"
}


[root@jsefler-5 ~]# rct cat-cert /etc/pki/entitlement/4162488095094029002.pem 

+-------------------------------------------+
	Entitlement Certificate
+-------------------------------------------+

Certificate:
	Path: /etc/pki/entitlement/4162488095094029002.pem
	Version: 3.2
	Serial: 4162488095094029002
	Start Date: 2013-07-09 00:00:00+00:00
	End Date: 2014-07-09 00:00:00+00:00
	Pool ID: 8a90f8203fc4ca73013fc4cbed000442

Subject:
	CN: 8a90f8203fc4ca73013fc50ec22915ff

Issuer:
	C: US
	CN: jsefler-f14-candlepin.usersys.redhat.com
	L: Raleigh


Order:
	Name: Awesome OS Server Basic (data center)
	Number: order-8675309
	SKU: awesomeos-server-basic-dc
	Contract: 18
	Account: 12331131231
	Service Level: None
	Service Type: Self-Support
	Quantity: 5
	Quantity Used: 1
	Socket Limit: 4
	RAM Limit: 
	Core Limit: 
	Virt Limit: 
	Virt Only: False
	Subscription: 
	Stacking ID: 
	Warning Period: 30
	Provides Management: False

[root@jsefler-5 ~]# rct cat-cert /etc/pki/entitlement/805878192443409307.pem 

+-------------------------------------------+
	Entitlement Certificate
+-------------------------------------------+

Certificate:
	Path: /etc/pki/entitlement/805878192443409307.pem
	Version: 3.2
	Serial: 805878192443409307
	Start Date: 2013-07-09 00:00:00+00:00
	End Date: 2014-07-09 00:00:00+00:00
	Pool ID: 8a90f8203fc4ca73013fc50ec22b1600

Subject:
	CN: 8a90f8203fc4ca73013fc50f391e161b

Issuer:
	C: US
	CN: jsefler-f14-candlepin.usersys.redhat.com
	L: Raleigh

Product:
	ID: 37060
	Name: Awesome OS Server Bits
	Version: 6.1
	Arch: ALL
	Tags: 

Order:
	Name: Awesome OS Server Basic (dc-virt)
	Number: order-8675309
	SKU: awesomeos-server-basic-vdc
	Contract: 18
	Account: 12331131231
	Service Level: Full-Service
	Service Type: Drive-Through
	Quantity: 5
	Quantity Used: 1
	Socket Limit: 2
	RAM Limit: 2
	Core Limit: 4
	Virt Limit: 
	Virt Only: True
	Subscription: 
	Stacking ID: 
	Warning Period: 0
	Provides Management: False

Content:
	Type: yum
	Name: always-enabled-content
	Label: always-enabled-content
	Vendor: test-vendor
	URL: /foo/path/always/$releasever
	GPG: /foo/path/always/gpg
	Enabled: True
	Expires: 200
	Required Tags: 
	Arches: ALL

Content:
	Type: yum
	Name: content
	Label: content-label
	Vendor: test-vendor
	URL: /foo/path
	GPG: /foo/path/gpg/
	Enabled: True
	Expires: 0
	Required Tags: 
	Arches: ALL

Content:
	Type: yum
	Name: content-emptygpg
	Label: content-label-empty-gpg
	Vendor: test-vendor
	URL: /foo/path
	GPG: 
	Enabled: True
	Expires: 0
	Required Tags: 
	Arches: ALL

Content:
	Type: yum
	Name: content-nogpg
	Label: content-label-no-gpg
	Vendor: test-vendor
	URL: /foo/path
	GPG: 
	Enabled: True
	Expires: 0
	Required Tags: 
	Arches: ALL

Content:
	Type: yum
	Name: never-enabled-content
	Label: never-enabled-content
	Vendor: test-vendor
	URL: /foo/path/never
	GPG: /foo/path/never/gpg
	Enabled: False
	Expires: 600
	Required Tags: 
	Arches: ALL

Content:
	Type: yum
	Name: tagged-content
	Label: tagged-content
	Vendor: test-vendor
	URL: /foo/path/always
	GPG: /foo/path/always/gpg
	Enabled: True
	Expires: 
	Required Tags: TAG1, TAG2
	Arches: ALL
[root@jsefler-5 ~]# 

*/