package com.redhat.qe.sm.cli.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.SubscriptionPool;


/**
 * @author jsefler
 * </BR>
 * These tests target subscriptions that do not provide any products, but instead are intended to grant an entitlement to a server-side function such as a Management Add-On.
 */
@Test(groups="ManagementAddOnTests")
public class ManagementAddOnTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************
	

	@Test(	description="verify that the entitlement cert granted by subscribing to a management add-on product does not contain a content namespace.",
			groups={},
			dependsOnGroups={},
			dataProvider="getAddOnSubscriptionData",
			enabled=true)
	public void VerifyManagementAddOnEntitlementsContainNoContentNamespace (SubscriptionPool managementAddOnPool) {
		
		// subscribe to a management add-on pool
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.subscribeToSubscriptionPool(managementAddOnPool));

		// assert that there are no content namespaces in the granted entitlement cert
		Assert.assertTrue(entitlementCert.contentNamespaces.isEmpty(),"There are no content namespaces in the entitlement cert granted after subscribing to management add-on subscription pool: "+managementAddOnPool);
	}

	
	@Test(	description="verify that the entitlement cert granted by subscribing to a management add-on product does not contain a product namespace.",
			groups={},
			dependsOnGroups={},
			dataProvider="getAddOnSubscriptionData",
			enabled=true)
	public void VerifyManagementAddOnEntitlementsContainNoProductNamespace (SubscriptionPool managementAddOnPool) {
		
		// subscribe to a management add-on pool
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.subscribeToSubscriptionPool(managementAddOnPool));

		// assert that there are no product namespaces contained within the granted entitlement cert
		Assert.assertTrue(entitlementCert.productNamespaces.isEmpty(),"There are no product namespaces in the entitlement cert granted after subscribing to management add-on subscription pool: "+managementAddOnPool);
	}
	
	
	// Candidates for an automated Test:
	// 
	
	
	
	
	// Configuration methods ***********************************************************************
		
	@BeforeClass(groups="setup")
	public void registerBeforeClass() throws Exception {
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(clientusername, clientpassword, null, null, null, null, null, Boolean.TRUE, null, null, null));
//		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(serverHostname, serverPort, serverPrefix, clientusername, clientpassword, consumerId);
	}
	
	@AfterClass(groups="setup")
	public void unregisterAfterClass() {
		clienttasks.unregister(null, null, null);
	}

	
	// protected methods ***********************************************************************
	
//	protected String ownerKey = "";

	
	
	
	// Data Providers ***********************************************************************
	
	
	@DataProvider(name="getAddOnSubscriptionData")
	public Object[][] getAddOnSubscriptionDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAddOnSubscriptionDataAsListOfLists());
	}
	protected List<List<Object>> getAddOnSubscriptionDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		List<SubscriptionPool> allAvailablePools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		
		// iterate through all available pools looking for those that contain no provided products
		for (SubscriptionPool pool : allAvailablePools) {
			JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,clientusername,clientpassword,"/pools/"+pool.poolId));	
			JSONArray jsonProvidedProducts = jsonPool.getJSONArray("providedProducts");
			if (jsonProvidedProducts.length()==0) {
				
				// found a subscription to a Management Add-on, add it to the list of subscriptions to test
				ll.add(Arrays.asList(new Object[]{pool}));
			}
		}
				
		return ll;
	}
	
/*
Example jsonPool:
[root@jsefler-onprem03 ~]# curl -u testuser1:password -k https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/pools/8a90f8b42ec1fd09012ec1fdaaf40125 | python -mjson.tool
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
106   641    0   641    0     0   3014      0 --:--:-- --:--:-- --:--:--  6474
{
    "accountNumber": "12331131231", 
    "activeSubscription": true, 
    "attributes": [], 
    "consumed": 0, 
    "contractNumber": "45", 
    "created": "2011-03-17T04:05:50.452+0000", 
    "endDate": "2012-03-16T00:00:00.000+0000", 
    "href": "/pools/8a90f8b42ec1fd09012ec1fdaaf40125", 
    "id": "8a90f8b42ec1fd09012ec1fdaaf40125", 
    "owner": {
        "href": "/owners/admin", 
        "id": "8a90f8b42ec1fd09012ec1fd34e10005"
    }, 
    "productId": "management-100", 
    "productName": "Management Add-On", 
    "providedProducts": [], 
    "quantity": 500, 
    "restrictedToUsername": null, 
    "sourceEntitlement": null, 
    "startDate": "2011-03-17T00:00:00.000+0000", 
    "subscriptionId": "8a90f8b42ec1fd09012ec1fda8b2011f", 
    "updated": "2011-03-17T04:05:50.452+0000"
}


		  
*/
	

}
