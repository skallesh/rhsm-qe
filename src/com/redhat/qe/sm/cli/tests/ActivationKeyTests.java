package com.redhat.qe.sm.cli.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 */
@Test(groups={"ActivationKeyTests"})
public class ActivationKeyTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************

	
	@Test(	description="use the candlepin api to create valid activation keys",
			groups={},
			dataProvider="getRegisterCredentialsExcludingNullOrgData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void ActivationKeyCreationDeletion_Test(String username, String password, String org) throws JSONException, Exception {
		// generate a unique name for this test
		String name = String.format("%s_%s-ActivationKey%s", username,org,System.currentTimeMillis());
		
		// create a JSON object to represent the request body
		Map<String,String> mapActivationKeyRequest = new HashMap<String,String>();
		mapActivationKeyRequest.put("name", name);
		JSONObject jsonActivationKeyRequest = new JSONObject(mapActivationKeyRequest);

		// call the candlepin api to create an activation key
		JSONObject jsonActivationKey = new JSONObject(CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverHostname, sm_serverPort, sm_serverPrefix, username, password, "/owners/" + org + "/activation_keys",  jsonActivationKeyRequest.toString()));

		// assert that the creation was successful (does not contain a displayMessage)
		try {
			String displayMessage = jsonActivationKey.getString("displayMessage");
			Assert.fail("The creation of an activation key appears to have failed: "+displayMessage);
		} catch (JSONException e) {
			Assert.assertTrue(true,"The absense of a displayMessage indicates the activation key creation was probably successful.");
		}
		// assert that the created key is listed
		// process all of the subscriptions belonging to ownerKey
		JSONArray jsonActivationKeys = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverHostname,sm_serverPort,sm_serverPrefix,username,password,"/owners/"+org+"/activation_keys"));	
		JSONObject jsonActivationKeyI = null;
		for (int i = 0; i < jsonActivationKeys.length(); i++) {
			jsonActivationKeyI = (JSONObject) jsonActivationKeys.get(i);
			//{
			//    "created": "2011-08-04T21:38:23.902+0000", 
			//    "id": "8a90f8c63196bb20013196bba01e0008", 
			//    "name": "default_key", 
			//    "owner": {
			//        "displayName": "Admin Owner", 
			//        "href": "/owners/admin", 
			//        "id": "8a90f8c63196bb20013196bb9e210006", 
			//        "key": "admin"
			//    }, 
			//    "pools": [], 
			//    "updated": "2011-08-04T21:38:23.902+0000"
			//}
			
			// break out when the created activation key is found
			if (jsonActivationKeyI.getString("name").equals(name)) break;
		}
		Assert.assertNotNull(jsonActivationKeyI, "Successfully listed keys for owner '"+org+"'.");
		Assert.assertEquals(jsonActivationKey.toString(), jsonActivationKeyI.toString(), "Successfully found newly created activation key with credentials '"+username+"'/'"+password+"' under /owners/"+org+"/activation_keys .");
		
		// now assert that the activation key is found under /candlepin/activation_keys/<id>
		JSONObject jsonActivationKeyJ = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverHostname,sm_serverPort,sm_serverPrefix,sm_serverAdminUsername,sm_serverAdminPassword,"/activation_keys/"+jsonActivationKey.getString("id")));
		Assert.assertEquals(jsonActivationKey.toString(), jsonActivationKeyJ.toString(), "Successfully found newly created activation key among all activation keys under /activation_keys.");

		// now attempt to delete the key
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverHostname, sm_serverPort, sm_serverPrefix, username, password, "/activation_keys/"+jsonActivationKey.getString("id"));
		// assert that it is no longer found under /activation_keys
		jsonActivationKeys = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverHostname,sm_serverPort,sm_serverPrefix,sm_serverAdminUsername,sm_serverAdminPassword,"/activation_keys"));
		jsonActivationKeyI = null;
		for (int i = 0; i < jsonActivationKeys.length(); i++) {
			jsonActivationKeyI = (JSONObject) jsonActivationKeys.get(i);
			if (jsonActivationKeyI.getString("id").equals(jsonActivationKey.getString("id"))) {
				Assert.fail("After attempting to delete activation key id '"+jsonActivationKey.getString("id")+"', it was still found in the /activation_keys list.");
			}
		}
		Assert.assertTrue(true,"Deleted activation key with id '"+jsonActivationKey.getString("id")+"' is no longer found in the /activation_keys list.");
	}

	
	@Test(	description="use the candlepin api to attempt creation of an activation key with a bad name",
			groups={},
			dataProvider="getActivationKeyCreationWithBadNameData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void AttemptActivationKeyCreationWithBadNameData_Test(Object blockedByBug, String badName) throws JSONException, Exception {
		
		// create a JSON object to represent the request body (with bad data)
		Map<String,String> mapActivationKeyRequest = new HashMap<String,String>();
		mapActivationKeyRequest.put("name", badName);
		JSONObject jsonActivationKeyRequest = new JSONObject(mapActivationKeyRequest);
		
		// call the candlepin api to create an activation key
		JSONObject jsonActivationKey = new JSONObject(CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverHostname, sm_serverPort, sm_serverPrefix, sm_clientUsername, sm_clientPassword, "/owners/" + sm_clientOrg + "/activation_keys",  jsonActivationKeyRequest.toString()));

		// assert that the creation was NOT successful (contains a displayMessage)
		try {
			String displayMessage = jsonActivationKey.getString("displayMessage");
			Assert.assertEquals(displayMessage, "Activation key names must be alphanumeric or the characters '-' or '_'. ["+badName+"]","Expected the creation of this activation key named '"+badName+"' to fail.");
		} catch (JSONException e) {
			log.warning("The absense of a displayMessage indicates the activation key creation was probably successful when we expected it to fail due to an invalid name '"+badName+"'.");
			Assert.assertFalse (badName.equals(jsonActivationKey.getString("name")),"The following activation key should not have been created with badName '"+badName+"': "+jsonActivationKey);
		}
	}
	
	
	@Test(	description="use the candlepin api to attempt to create a duplicate activation key",
			groups={/*"blockedByBug-728636"*/},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void AttemptActivationKeyCreationInDuplicate_Test() throws JSONException, Exception {

		// generate a unique name for this test
		String name = String.format("%s_%s-DuplicateActivationKey%s", sm_clientUsername,sm_clientOrg,System.currentTimeMillis());
		
		// create a JSON object to represent the request body
		Map<String,String> mapActivationKeyRequest = new HashMap<String,String>();
		mapActivationKeyRequest.put("name", name);
		JSONObject jsonActivationKeyRequest = new JSONObject(mapActivationKeyRequest);
		
		// call the candlepin api to create an activation key
		JSONObject jsonActivationKey = new JSONObject(CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverHostname, sm_serverPort, sm_serverPrefix, sm_clientUsername, sm_clientPassword, "/owners/" + sm_clientOrg + "/activation_keys",  jsonActivationKeyRequest.toString()));
		Assert.assertEquals(jsonActivationKey.getString("name"), name, "First activation key creation attempt appears successful.  Activation key: "+jsonActivationKey);

		// attempt to create another key by the same name
		jsonActivationKey = new JSONObject(CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverHostname, sm_serverPort, sm_serverPrefix, sm_clientUsername, sm_clientPassword, "/owners/" + sm_clientOrg + "/activation_keys",  jsonActivationKeyRequest.toString()));
		
		// assert that the creation was NOT successful (contains a displayMessage)
		try {
			String displayMessage = jsonActivationKey.getString("displayMessage");
			Assert.assertTrue(displayMessage.startsWith("Runtime Error org.hibernate.exception.ConstraintViolationException: Could not execute JDBC batch update at org.postgresql.jdbc2.AbstractJdbc2Statement$BatchResultHandler.handleError:"),"Expected the creation of a duplicate activation key named '"+name+"' to fail.");
		} catch (JSONException e) {
			log.warning("The absense of a displayMessage indicates the activation key creation was probably successful when we expected it to fail due to a duplicate name '"+name+"'.");
			Assert.assertFalse (name.equals(jsonActivationKey.getString("name")),"The following activation key should not have been created with a duplicate name '"+name+"': "+jsonActivationKey);
		}
	}
	

	@Test(	description="create an activation key, bind it to a pool with a quantity, and then register with the activation key",
			groups={},
			dataProvider="getRegisterWithActivationKeyBoundToPoolWithQuantity_TestData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RegisterWithActivationKeyBoundToPoolWithQuantity_Test(Object blockedByBug, JSONObject jsonPool, Integer bindQuantity) throws JSONException, Exception {
		String poolId = jsonPool.getString("id");
				
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=728721 - jsefler 8/6/2011
		if (CandlepinTasks.isPoolProductConsumableByConsumerType(sm_serverHostname, sm_serverPort, sm_serverPrefix, sm_clientUsername, sm_clientPassword, poolId, ConsumerType.person)) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="728721"; 
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla bug "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("Skipping this test while bug '"+bugId+"' is open. (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
			}
		}
		// END OF WORKAROUND
		
		// generate a unique activation key name for this test
		String name = String.format("ActivationKey%s_ForPool%s", System.currentTimeMillis(), poolId);
		
		// create a JSON object to represent the request body
		Map<String,String> mapActivationKeyRequest = new HashMap<String,String>();
		mapActivationKeyRequest.put("name", name);
		JSONObject jsonActivationKeyRequest = new JSONObject(mapActivationKeyRequest);
		
		// call the candlepin api to create an activation key
		JSONObject jsonActivationKey = new JSONObject(CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverHostname, sm_serverPort, sm_serverPrefix, sm_clientUsername, sm_clientPassword, "/owners/" + sm_clientOrg + "/activation_keys",  jsonActivationKeyRequest.toString()));
		Assert.assertEquals(jsonActivationKey.getString("name"), name, "Activation key creation attempt appears successful.  Activation key: "+jsonActivationKey);

		// bind the activation key to the pool with a random available quantity (?quantity=#)
//		int quantityAvail = jsonPool.getInt("quantity")-jsonPool.getInt("consumed");
//		int bindQuantity = Math.max(1,randomGenerator.nextInt(quantityAvail+1));	// avoid a bindQuantity < 1 see https://bugzilla.redhat.com/show_bug.cgi?id=729125
		JSONObject jsonBoundPool = new JSONObject(CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverHostname, sm_serverPort, sm_serverPrefix, sm_clientUsername, sm_clientPassword, "/activation_keys/" + jsonActivationKey.getString("id") + "/pools/" + poolId +(bindQuantity==null?"":"?quantity="+bindQuantity), null));
		if (bindQuantity==null) bindQuantity=1;

		// handle the case when the pool is NOT multi_entitlement and we tried to bind the pool to the key with a quantity > 1
		if (!CandlepinTasks.isPoolProductMultiEntitlement(sm_serverHostname, sm_serverPort, sm_serverPrefix, sm_clientUsername, sm_clientPassword, poolId) && bindQuantity>1) {

			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=729070 - jsefler 8/8/2011
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="729070"; 
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla bug "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("Skipping this test while bug '"+bugId+"' is open. (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
			}
			// END OF WORKAROUND
			
			// assert that the binding of the pool to the key was NOT successful (contains a displayMessage)
			try {
				String displayMessage = jsonActivationKey.getString("displayMessage");
				Assert.assertTrue(displayMessage.equals("FIXME: Only pools with multi-entitlement product subscriptions can be added to the activation key with a quantity greater than one."),"Expected the addition of a non-multi-entitlement pool '"+poolId+"' to activation key named '"+name+"' with quantity '"+bindQuantity+"' to fail.");
			} catch (JSONException e) {
				log.warning("The absense of a displayMessage indicates the activation key creation was probably successful when we expected it to fail due to greater than one quantity '"+bindQuantity+"'.");
				Assert.assertFalse (name.equals(jsonActivationKey.getString("name")),"Non multi-entitlement pool '"+poolId+"' should NOT have been added to the following activation key with a quantity '"+bindQuantity+"' greater than one: "+jsonActivationKey);
			}
			return;
		}
		
		// handle the case when the quantity is excessive or less than one
		if (bindQuantity > jsonPool.getInt("quantity") || bindQuantity < 1) {

			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=729125 - jsefler 8/8/2011
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="729125"; 
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla bug "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("Skipping this test while bug '"+bugId+"' is open. (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
			}
			// END OF WORKAROUND
			
			// assert that the binding of the pool to the key was NOT successful (contains a displayMessage)
			try {
				String displayMessage = jsonActivationKey.getString("displayMessage");
				Assert.assertTrue(displayMessage.equals("FIXME: Quantity is outside of acceptable range."),"Expected the addition of multi-entitlement pool '"+poolId+"' to activation key named '"+name+"' with an out-of-range quantity '"+bindQuantity+"' to fail.");
			} catch (JSONException e) {
				log.warning("The absense of a displayMessage indicates the activation key creation was probably successful when we expected it to fail due to an out-of-range quantity '"+bindQuantity+"'.");
				Assert.assertFalse (name.equals(jsonActivationKey.getString("name")),"Pool '"+poolId+"' should NOT have been added to the following activation key with an out-of-range quantity '"+bindQuantity+"': "+jsonActivationKey);
			}
			return;
		}
		
		// assert the pool is bound
		jsonActivationKey = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverHostname,sm_serverPort,sm_serverPrefix,sm_serverAdminUsername,sm_serverAdminPassword,"/activation_keys/"+jsonActivationKey.getString("id")));
		//# curl -k -u admin:admin https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/activation_keys/8a90f8c63196bb2001319f66afa83cb4 | python -mjson.tool
		//{
		//    "created": "2011-08-06T14:02:12.264+0000", 
		//    "id": "8a90f8c63196bb2001319f66afa83cb4", 
		//    "name": "ActivationKey1312639332183_ForPool8a90f8c63196bb20013196bc7f6302dc", 
		//    "owner": {
		//        "displayName": "Admin Owner", 
		//        "href": "/owners/admin", 
		//        "id": "8a90f8c63196bb20013196bb9e210006", 
		//        "key": "admin"
		//    }, 
		//    "pools": [
		//        {
		//            "created": "2011-08-06T14:02:12.419+0000", 
		//            "id": "8a90f8c63196bb2001319f66b0433cb6", 
		//            "pool": {
		//                "href": "/pools/8a90f8c63196bb20013196bc7f6302dc", 
		//                "id": "8a90f8c63196bb20013196bc7f6302dc"
		//            }, 
		//            "quantity": 1, 
		//            "updated": "2011-08-06T14:02:12.419+0000"
		//        }
		//    ], 
		//    "updated": "2011-08-06T14:02:12.264+0000"
		//}
		String boundPoolId = ((JSONObject) jsonActivationKey.getJSONArray("pools").get(0)).getJSONObject("pool").getString("id");	// get(0) since there should only be one pool bound
		Assert.assertEquals(boundPoolId, poolId, "The activation key appears to be successfully bound to poolId '"+poolId+"'.  Activation Key: "+jsonActivationKey);
		Integer boundQuantity = ((JSONObject) jsonActivationKey.getJSONArray("pools").get(0)).getInt("quantity");	// get(0) since there should only be one pool bound
		Assert.assertEquals(boundQuantity, bindQuantity, "The activation key appears to be successfully bound to poolId '"+poolId+"' ready to consume a quantity of '"+bindQuantity+"'.  Activation Key: "+jsonActivationKey);

		// register with the activation key
		SSHCommandResult registerResult = clienttasks.register_(null, null, sm_clientOrg, null, null, null, null, null, jsonActivationKey.getString("name"), true, null, null, null);
		
		// handle the case when "Consumers of this type are not allowed to subscribe to the pool with id '"+poolId+"'."
		if (!CandlepinTasks.isPoolProductConsumableByConsumerType(sm_serverHostname, sm_serverPort, sm_serverPrefix, sm_clientUsername, sm_clientPassword, poolId, ConsumerType.system)) {
			Assert.assertEquals(registerResult.getStderr().trim(), "Consumers of this type are not allowed to subscribe to the pool with id '"+poolId+"'.", "Registering a system consumer using an activationKey bound to a pool that requires a non-system consumer type should fail.");
			Assert.assertEquals(registerResult.getExitCode(), Integer.valueOf(255), "The exitCode from registering a system consumer using an activationKey bound to a pool that requires a non-system consumer type should fail.");
			// now register with the same activation key using the needed ConsumerType
			registerResult = clienttasks.register(null, null, sm_clientOrg, null, ConsumerType.valueOf(CandlepinTasks.getPoolProductAttributeValue(sm_serverHostname, sm_serverPort, sm_serverPrefix, sm_clientUsername, sm_clientPassword, poolId, "requires_consumer_type")), null, null, null, jsonActivationKey.getString("name"), false /*was already unregistered by force above*/, null, null, null);
		}
		
		// TODO handle the case when our quantity request exceeds the quantityAvail and there are no Entitlement Certs avail
		
		// assert that only the pool's providedProducts (excluding type=MKT products) are consumed (unless it is a ManagementAddOn product - indicated by no providedProducts)
		JSONArray jsonProvidedProducts = jsonPool.getJSONArray("providedProducts");
		// pluck out the providedProducts that have an atttribute type=MKT products
		for (int j = 0; j < jsonProvidedProducts.length(); j++) {
			JSONObject jsonProvidedProduct = (JSONObject) jsonProvidedProducts.get(j);
			JSONObject jsonProduct = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverHostname,sm_serverPort,sm_serverPrefix,sm_serverAdminUsername,sm_serverAdminPassword,"/products/"+jsonProvidedProduct.getString("productId")));
			JSONArray jsonAttributes = jsonProduct.getJSONArray("attributes");
			for (int k = 0; k < jsonAttributes.length(); k++) {
				JSONObject jsonAttribute = (JSONObject) jsonAttributes.get(k);
				if (jsonAttribute.getString("name").equals("type")) {
					if (jsonAttribute.getString("value").equals("MKT")) {
						log.info("Found a providedProduct '"+jsonProvidedProduct.getString("productName")+"' from the pool bound to the activation key that is actually a Marketing product (type=\"MKT\").  Therefore this provided product will be excluded from the expected consumed ProductSubscriptions assertions that will follow...");
						jsonProvidedProduct/*Plucked*/ = (JSONObject) jsonProvidedProducts.remove(j--);
						break;
					}
				}
			}
		}
		
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		if (jsonProvidedProducts.length()>0) { 
			Assert.assertEquals(consumedProductSubscriptions.size(), jsonProvidedProducts.length(), "The number of providedProducts from the pool bound to the activation key should match the number of consumed product subscriptions.");
			for (int j = 0; j < jsonProvidedProducts.length(); j++) {
				JSONObject jsonProvidedProduct = (JSONObject) jsonProvidedProducts.get(j);
				//{
				//    "created": "2011-08-04T21:39:21.059+0000", 
				//    "id": "8a90f8c63196bb20013196bc7f6402e7", 
				//    "productId": "37060", 
				//    "productName": "Awesome OS Server Bits", 
				//    "updated": "2011-08-04T21:39:21.059+0000"
				//}
				String providedProductName = jsonProvidedProduct.getString("productName");
				ProductSubscription consumedProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName", providedProductName, consumedProductSubscriptions);
				Assert.assertNotNull(consumedProductSubscription,"Found a consumed product subscription whose productName '"+providedProductName+"' is included in the providedProducts bound in the activation key.");
				Assert.assertEquals(consumedProductSubscription.accountNumber.longValue(), jsonPool.getLong("accountNumber"), "The consumed product subscription comes from the same accountNumber as the pool bound in the activation key.");
				Assert.assertEquals(consumedProductSubscription.contractNumber.intValue(), jsonPool.getInt("contractNumber"), "The consumed product subscription comes from the same contractNumber as the pool bound in the activation key.");
				Assert.assertEquals(consumedProductSubscription.quantityUsed, bindQuantity, "The consumed product subscription is using the same quantity as requested by the pool bound in the activation key.");
			}
		} else {	// this pool provides a subscription to a Management AddOn product (indicated by no providedProducts)
			Assert.assertEquals(consumedProductSubscriptions.size(), 1, "When a ManagementAddOn product is bound to the activation key, then the number of consumed product subscriptions should be one.");
			ProductSubscription consumedProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName", jsonPool.getString("productName"), consumedProductSubscriptions);
			Assert.assertNotNull(consumedProductSubscription,"Found a consumed product subscription whose productName '"+jsonPool.getString("productName")+"' matches the pool's productName bound in the activation key.");
			Assert.assertEquals(consumedProductSubscription.accountNumber.longValue(), jsonPool.getLong("accountNumber"), "The consumed product subscription comes from the same accountNumber as the pool bound in the activation key.");
			Assert.assertEquals(consumedProductSubscription.contractNumber.intValue(), jsonPool.getInt("contractNumber"), "The consumed product subscription comes from the same contractNumber as the pool bound in the activation key.");
			Assert.assertEquals(consumedProductSubscription.quantityUsed, bindQuantity, "The consumed product subscription is using the same quantity as requested by the pool bound in the activation key.");
		}
	}
	
	
	@Test(	description="create an activation key, bind it to a pool with an quantity outside the total possible available range",
			groups={"myDevGroup"},
			dataProvider="getAllMultiEntitlementJSONPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RegisterWithActivationKeyBoundToPoolWithQuantityOutsideAvailableQuantity_Test(Object blockedByBug, JSONObject jsonPool) throws JSONException, Exception {

		// choose a random pool quantity > totalPoolQuantity)
		Integer excessiveQuantity = jsonPool.getInt("quantity") + randomGenerator.nextInt(10) +1;
	
		RegisterWithActivationKeyBoundToPoolWithQuantity_Test(blockedByBug, jsonPool, excessiveQuantity);
		RegisterWithActivationKeyBoundToPoolWithQuantity_Test(blockedByBug, jsonPool, 0);
		RegisterWithActivationKeyBoundToPoolWithQuantity_Test(blockedByBug, jsonPool, -1);
		RegisterWithActivationKeyBoundToPoolWithQuantity_Test(blockedByBug, jsonPool, -1*excessiveQuantity);
	}
	
	
	@Test(	description="create an activation key, bind it to a pool (without specifying a quantity), and then register with the activation key",
			groups={},
			dataProvider="getRegisterWithActivationKeyBoundToPool_TestData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RegisterWithActivationKeyBoundToPool_Test(Object blockedByBug, JSONObject jsonPool) throws JSONException, Exception {
		RegisterWithActivationKeyBoundToPoolWithQuantity_Test(blockedByBug, jsonPool, null);
	}
	
	// TODO RegisterWithActivationKeyBoundToPoolToWrongOrg_Test
	// TODO RegisterWithActivationKeyBoundToPoolForWhichNotEnoughQuantityRemains_Test
	// TODO RegisterWithMultipleActivationKeysBoundToPool_Test
	
	// Candidates for an automated Test:
	
	
	// Protected Class Variables ***********************************************************************
	
	
	// Configuration methods ***********************************************************************



	
	// Protected methods ***********************************************************************

	
	
	// Data Providers ***********************************************************************

	@DataProvider(name="getAllMultiEntitlementJSONPoolsData")
	public Object[][] getAllMultiEntitlementJSONPoolsDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAllMultiEntitlementJSONPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAllMultiEntitlementJSONPoolsDataAsListOfLists() throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getAllJSONPoolsDataAsListOfLists()) {
			JSONObject jsonPool = (JSONObject)l.get(0);
			
			if (CandlepinTasks.isPoolProductMultiEntitlement(sm_serverHostname, sm_serverPort, sm_serverPrefix, sm_clientUsername, sm_clientPassword, jsonPool.getString("id"))) {

				// Object blockedByBug, JSONObject jsonPool)
				ll.add(Arrays.asList(new Object[] {null,	jsonPool}));
			}
		}
		return ll;
	}
	
	@DataProvider(name="getRegisterWithActivationKeyBoundToPool_TestData")
	public Object[][] getRegisterWithActivationKeyBoundToPool_TestDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getRegisterWithActivationKeyBoundToPool_TestDataAsListOfLists());
	}
	protected List<List<Object>> getRegisterWithActivationKeyBoundToPool_TestDataAsListOfLists() throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getAllJSONPoolsDataAsListOfLists()) {
			JSONObject jsonPool = (JSONObject)l.get(0);

			// Object blockedByBug, JSONObject jsonPool)
			ll.add(Arrays.asList(new Object[] {null,	jsonPool}));
		}
		return ll;
	}
	
	@DataProvider(name="getRegisterWithActivationKeyBoundToPoolWithQuantity_TestData")
	public Object[][] getRegisterWithActivationKeyBoundToPoolWithQuantity_TestDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getRegisterWithActivationKeyBoundToPoolWithQuantity_TestDataAsListOfLists());
	}
	protected List<List<Object>> getRegisterWithActivationKeyBoundToPoolWithQuantity_TestDataAsListOfLists() throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getAllJSONPoolsDataAsListOfLists()) {
			JSONObject jsonPool = (JSONObject)l.get(0);
			// choose a random valid pool quantity (1<=quantity<=totalPoolQuantity)
			int quantityAvail = jsonPool.getInt("quantity")-jsonPool.getInt("consumed");
			int bindQuantity = Math.max(1,randomGenerator.nextInt(quantityAvail+1));	// avoid a bindQuantity < 1 see https://bugzilla.redhat.com/show_bug.cgi?id=729125

			// Object blockedByBug, JSONObject jsonPool
			ll.add(Arrays.asList(new Object[] {null,	jsonPool,	bindQuantity}));
		}
		return ll;
	}
}
