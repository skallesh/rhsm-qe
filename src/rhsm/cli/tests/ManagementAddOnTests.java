package rhsm.cli.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.testng.TestNGUtils;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.SubscriptionPool;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author jsefler
 * </BR>
 * These tests target subscriptions that do not provide any products, but instead are intended to grant an entitlement to a server-side function such as a Management Add-On.
 */
@Test(groups={"ManagementAddOnTests"})
public class ManagementAddOnTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27127", "RHEL7-51412"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier2")
	@Test(	description="verify that the entitlement cert granted by subscribing to a management add-on product does not contain a content namespace.",
			groups={"Tier2Tests"},
			dependsOnGroups={},
			dataProvider="getAddOnSubscriptionData",
			enabled=true)
	public void testManagementAddOnEntitlementsContainNoContentNamespace (Object bugzilla, SubscriptionPool managementAddOnPool) {
		
		// subscribe to a management add-on pool
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.subscribeToSubscriptionPool(managementAddOnPool,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl));

		// assert that there are no content namespaces in the granted entitlement cert
		Assert.assertTrue(entitlementCert.contentNamespaces.isEmpty(),"There are no content namespaces in the entitlement cert granted after subscribing to management add-on subscription pool: "+managementAddOnPool);
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27128", "RHEL7-51413"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier2")
	@Test(	description="verify that the entitlement cert granted by subscribing to a management add-on product does not contain a product namespace.",
			groups={"Tier2Tests"},
			dependsOnGroups={},
			dataProvider="getAddOnSubscriptionData",
			enabled=true)
	public void testManagementAddOnEntitlementsContainNoProductNamespace (Object bugzilla, SubscriptionPool managementAddOnPool) {
		
		// subscribe to a management add-on pool
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.subscribeToSubscriptionPool(managementAddOnPool,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl));

		// assert that there are no product namespaces contained within the granted entitlement cert
		Assert.assertTrue(entitlementCert.productNamespaces.isEmpty(),"There are no product namespaces in the entitlement cert granted after subscribing to management add-on subscription pool: "+managementAddOnPool);
	}
	
	
	// Candidates for an automated Test:
	// 
	
	
	
	
	// Configuration methods ***********************************************************************
		
	@BeforeClass(groups="setup")
	public void registerBeforeClass() throws Exception {
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null));
	}
	
	@AfterClass(groups="setup")
	public void unregisterAfterClass() {
		clienttasks.unregister(null, null, null, null);
	}

	
	// protected methods ***********************************************************************
	
	
	
	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="getAddOnSubscriptionData")
	public Object[][] getAddOnSubscriptionDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAddOnSubscriptionDataAsListOfLists());
	}
	protected List<List<Object>> getAddOnSubscriptionDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		List<SubscriptionPool> allAvailablePools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		
		// iterate through all available pools looking for those that contain no provided products (A Management AddOn Subscription contains no products)
		for (SubscriptionPool pool : allAvailablePools) {
			JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+pool.poolId));	
			JSONArray jsonProvidedProducts = jsonPool.getJSONArray("providedProducts");
			if (jsonProvidedProducts.length()==0) {
				Set<String> bugIds = new HashSet<String>();
				
				// Bug 1204311 - derivedProvidedProducts should not show up on a consumed subscription from a temporary pool
				if (CandlepinTasks.isPoolRestrictedToUnmappedVirtualSystems(sm_clientUsername,sm_clientPassword,sm_serverUrl, pool.poolId)) {
					bugIds.add("1204311");
				}
				
				// found a subscription to a Management Add-on, add it to the list of subscriptions to test
				BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
				ll.add(Arrays.asList(new Object[]{blockedByBzBug, pool}));
			}
		}
				
		return ll;
	}
	
	// Example Management AddOn jsonPool:
	//# curl -k --request GET --user testuser1:password  --header 'accept: application/json' --header 'content-type: application/json'  https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/pools/8a90f8c63196bb20013196bc7c5d0268 | python -mjson.tool
	//{
	//    "accountNumber": "12331131231", 
	//    "activeSubscription": true, 
	//    "attributes": [], 
	//    "consumed": 0, 
	//    "contractNumber": "69", 
	//    "created": "2011-08-04T21:39:20.285+0000", 
	//    "endDate": "2012-09-02T04:00:00.000+0000", 
	//    "href": "/pools/8a90f8c63196bb20013196bc7c5d0268", 
	//    "id": "8a90f8c63196bb20013196bc7c5d0268", 
	//    "owner": {
	//        "displayName": "Admin Owner", 
	//        "href": "/owners/admin", 
	//        "id": "8a90f8c63196bb20013196bb9e210006", 
	//        "key": "admin"
	//    }, 
	//    "productAttributes": [
	//        {
	//            "created": "2011-08-04T21:39:20.286+0000", 
	//            "id": "8a90f8c63196bb20013196bc7c5e026b", 
	//            "name": "management_enabled", 
	//            "productId": "management-100", 
	//            "updated": "2011-08-04T21:39:20.286+0000", 
	//            "value": "1"
	//        }, 
	//        {
	//            "created": "2011-08-04T21:39:20.286+0000", 
	//            "id": "8a90f8c63196bb20013196bc7c5e026a", 
	//            "name": "type", 
	//            "productId": "management-100", 
	//            "updated": "2011-08-04T21:39:20.286+0000", 
	//            "value": "MKT"
	//        }, 
	//        {
	//            "created": "2011-08-04T21:39:20.286+0000", 
	//            "id": "8a90f8c63196bb20013196bc7c5e0269", 
	//            "name": "multi-entitlement", 
	//            "productId": "management-100", 
	//            "updated": "2011-08-04T21:39:20.286+0000", 
	//            "value": "no"
	//        }, 
	//        {
	//            "created": "2011-08-04T21:39:20.286+0000", 
	//            "id": "8a90f8c63196bb20013196bc7c5e026d", 
	//            "name": "arch", 
	//            "productId": "management-100", 
	//            "updated": "2011-08-04T21:39:20.286+0000", 
	//            "value": "ALL"
	//        }, 
	//        {
	//            "created": "2011-08-04T21:39:20.286+0000", 
	//            "id": "8a90f8c63196bb20013196bc7c5e026c", 
	//            "name": "warning_period", 
	//            "productId": "management-100", 
	//            "updated": "2011-08-04T21:39:20.286+0000", 
	//            "value": "90"
	//        }, 
	//        {
	//            "created": "2011-08-04T21:39:20.286+0000", 
	//            "id": "8a90f8c63196bb20013196bc7c5e026e", 
	//            "name": "version", 
	//            "productId": "management-100", 
	//            "updated": "2011-08-04T21:39:20.286+0000", 
	//            "value": "1.0"
	//        }, 
	//        {
	//            "created": "2011-08-04T21:39:20.287+0000", 
	//            "id": "8a90f8c63196bb20013196bc7c5f026f", 
	//            "name": "variant", 
	//            "productId": "management-100", 
	//            "updated": "2011-08-04T21:39:20.287+0000", 
	//            "value": "ALL"
	//        }
	//    ], 
	//    "productId": "management-100", 
	//    "productName": "Management Add-On", 
	//    "providedProducts": [], 
	//    "quantity": 1000, 
	//    "restrictedToUsername": null, 
	//    "sourceEntitlement": null, 
	//    "startDate": "2011-07-03T04:00:00.000+0000", 
	//    "subscriptionId": "8a90f8c63196bb20013196bc7a1f025d", 
	//    "updated": "2011-08-05T07:29:20.043+0000"
	//}
	

}
