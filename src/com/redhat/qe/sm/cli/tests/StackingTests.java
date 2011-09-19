package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.ContentNamespace;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.Repo;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.sm.data.YumRepo;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"StackingTests"})
public class StackingTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager: subscribe to each pool with the same stacking_id to achieve compliance",
			enabled=true,
			groups={},
			dataProvider="getAvailableStackableSubscriptionPoolsData")
	//@ImplementsNitrateTest(caseId=)
	public void StackEachPoolToAchieveCompliance(List<SubscriptionPool> stackableSubscriptionPools) throws JSONException, Exception{
		
		// loop through the pools to determine the minimum socket count for which one of each stackable pool is needed to achieve compliance
		int minimumSockets=0;
		for (SubscriptionPool pool : stackableSubscriptionPools) {
			String sockets = CandlepinTasks.getPoolProductAttributeValue(sm_serverHostname, sm_serverPort, sm_serverPrefix, sm_clientUsername, sm_clientPassword, pool.poolId, "sockets");
			minimumSockets+=Integer.valueOf(sockets);
		}
		
		// override the system facts setting the socket count to a value for which all the stackable subscriptions are needed to achieve compliance
		
		// loop through the stackable pools until we find the first one that covers product certs that are currently installed (put that subscription at the front of the list) (remember the installed product certs)
		List<ProductCert> installedProductCerts = new ArrayList<ProductCert>();
		for (int i=0; i<stackableSubscriptionPools.size(); i++) {
			SubscriptionPool pool = stackableSubscriptionPools.get(i);
			installedProductCerts = clienttasks.getCurrentProductCertsCorrespondingToSubscriptionPool(pool);
			if (installedProductCerts.size()>0) {
				stackableSubscriptionPools.remove(i);
				stackableSubscriptionPools.add(0, pool);
				break;
			}
		}
		if (installedProductCerts.size()==0) throw new SkipException("Could not find any installed products for which stacking these pools would achieve compliance.");

		// reconfigure such that only these product certs are installed (copy them to a /tmp/sm-stackingProductDir)
		
		// subscribe to each pool and assert "Partially Subscribe" status and overall incompliance untill the final pool is subscribed


		
//		List<Repo> priorRepos = clienttasks.getCurrentlySubscribedRepos();
//		
//		//File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool);	// for this test, we can skip the exhaustive asserts done by this call to clienttasks.subscribeToSubscriptionPool(pool)
//		File entitlementCertFile = clienttasks.subscribeToSubscriptionPool_(pool);
//		Assert.assertEquals(RemoteFileTasks.testFileExists(client, entitlementCertFile.getPath()),1, "Found the EntitlementCert file ("+entitlementCertFile+") that was granted after subscribing to pool id '"+pool.poolId+"'.");
//
//		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
//		
//		
//		// the following block of code was added to account for prior subscribed modifier pools that could provide more repos than expected once this pool is subscribed
//		// check the modifierSubscriptionData for SubscriptionPools that may already have been subscribed too and will modify this pool thereby enabling more repos than expected 
//		for (List<Object> row : modifierSubscriptionData) {
//			// ll.add(Arrays.asList(new Object[]{modifierPool, label, modifiedProductIds, requiredTags, providingPools}));
//			SubscriptionPool modifierPool = (SubscriptionPool)row.get(0);
//			String label = (String)row.get(1);
//			List<String> modifiedProductIds = (List<String>)row.get(2);
//			String requiredTags = (String)row.get(3);
//			List<SubscriptionPool> providingPools = (List<SubscriptionPool>)row.get(4);
//			if (providingPools.contains(pool)) {
//				if (priorSubscribedPools.contains(modifierPool)) {
//					// the modifier's content should now be available in the repos too
//					EntitlementCert modifierEntitlementCert = clienttasks.getEntitlementCertCorrespondingToSubscribedPool(modifierPool);						
//
//					// simply add the contentNamespaces from the modifier to the entitlement cert's contentNamespaces so they will be accounted for in the repos list test below
//					entitlementCert.contentNamespaces.addAll(modifierEntitlementCert.contentNamespaces);
//				}
//			}
//		}
//		priorSubscribedPools.add(pool);
//		
//			
//		List<Repo> actualRepos = clienttasks.getCurrentlySubscribedRepos();
//		
//		// assert that the new contentNamespaces from the entitlementCert are listed in repos
//		int numNewRepos=0;
//		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
//			
//			// instantiate the expected Repo that represents this contentNamespace
//			String expectedRepoUrl;	// the expected RepoUrl is set by joining the rhsm.conf baseurl with the downloadUrl in the contentNamespace which is usually a relative path.  When it is already a full path, leave it!
//			if (contentNamespace.downloadUrl.contains("://")) {
//				expectedRepoUrl = contentNamespace.downloadUrl;
//			} else {
//				expectedRepoUrl = clienttasks.baseurl.replaceFirst("//+$","//")+contentNamespace.downloadUrl.replaceFirst("^//+","");	// join baseurl to downloadUrl with "/"
//			}
//			Repo expectedRepo = new Repo(contentNamespace.name,contentNamespace.label,expectedRepoUrl,contentNamespace.enabled.trim().equals("1")?true:false);
//			
//			// assert the subscription-manager repos --list reports the expectedRepo (unless it requires tags that are not found in the installed product certs)
//			if (clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace,currentProductCerts)) {
//				Assert.assertTrue(actualRepos.contains(expectedRepo),"The newly entitled contentNamespace '"+contentNamespace+"' is represented in the subscription-manager repos --list by: "+expectedRepo);
//				
//				if (!priorRepos.contains(expectedRepo)) numNewRepos++;	// also count the number of NEW contentNamespaces
//				
//			} else {
//				Assert.assertFalse(actualRepos.contains(expectedRepo),"The newly entitled contentNamespace '"+contentNamespace+"' is NOT represented in the subscription-manager repos --list because it requires tags ("+contentNamespace.requiredTags+") that are not provided by the currently installed product certs.");
//			}
//		}
//
//		
//		// assert that the number of repos reported has increased by the number of contentNamespaces in the new entitlementCert (unless the 
//		Assert.assertEquals(actualRepos.size(), priorRepos.size()+numNewRepos, "The number of entitled repos has increased by the number of NEW contentNamespaces ("+numNewRepos+") from the newly granted entitlementCert.");
//		
//		// randomly decide to unsubscribe from the pool only for the purpose of saving on accumulated logging and avoid a java heap memory error
//		//if (randomGenerator.nextInt(2)==1) clienttasks.unsubscribe(null, entitlementCert.serialNumber, null, null, null); AND ALSO REMOVE pool FROM priorSubscribedPools
	}
	protected List<SubscriptionPool> priorSubscribedPools=new ArrayList<SubscriptionPool>();
	
	

		
		
	// Configuration methods ***********************************************************************

//	@BeforeClass(groups={"setup"})
//	public void setupBeforeClass() throws JSONException, Exception {
//		currentProductCerts = clienttasks.getCurrentProductCerts();
//		modifierSubscriptionData = getModifierSubscriptionDataAsListOfLists();
//	}
//	
//	@BeforeGroups(groups={"setup"}, value={"unsubscribeAllBeforeThisTest"})
//	public void unsubscribeAllBeforeGroups() {
//		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
//	}
	
	
	// Protected methods ***********************************************************************

//	List<ProductCert> currentProductCerts=new ArrayList<ProductCert>();
//	List<List<Object>> modifierSubscriptionData = null;

	
	// Data Providers ***********************************************************************
	

	
	
	// FIXME NOT BEING USED
	@DataProvider(name="getAllStackableJSONPoolsData")
	public Object[][] getAllStackableJSONPoolsDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAllStackableJSONPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAllStackableJSONPoolsDataAsListOfLists() throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		Map<String,List<JSONObject>> stackableJSONPoolsMap = new HashMap<String,List<JSONObject>>();
		
		for (List<Object> row : getAllJSONPoolsDataAsListOfLists()) {
			JSONObject jsonPool = (JSONObject) row.get(0);
			
			
			// loop through all the productAttributes looking for stacking_id
			JSONArray jsonProductAttributes = jsonPool.getJSONArray("productAttributes");
			for (int j = 0; j < jsonProductAttributes.length(); j++) {	// loop product attributes to find a stacking_id
				if (((JSONObject) jsonProductAttributes.get(j)).getString("name").equals("stacking_id")) {
					String stacking_id = ((JSONObject) jsonProductAttributes.get(j)).getString("value");
					
					// we found a stackable pool, let's add it to the stackableJSONPoolsMap
					if (!stackableJSONPoolsMap.containsKey(stacking_id)) stackableJSONPoolsMap.put(stacking_id, new ArrayList<JSONObject>());
					stackableJSONPoolsMap.get(stacking_id).add(jsonPool);
					break;
				}
			}
		}
//		// who is the owner of sm_clientUsername
//		String clientOrg = sm_clientOrg;
//		if (clientOrg==null) {
//			List<RegistrationData> registrationData = findGoodRegistrationData(true,sm_clientUsername,false,clientOrg);
//			if (registrationData.isEmpty() || registrationData.size()>1) throw new SkipException("Could not determine unique owner for username '"+sm_clientUsername+"'.  It is needed for a candlepin API call get pools by owner.");
//			clientOrg = registrationData.get(0).ownerKey;
//		}
//		
//		// process all of the pools belonging to ownerKey
//		JSONArray jsonPools = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverHostname,sm_serverPort,sm_serverPrefix,sm_clientUsername,sm_clientPassword,"/owners/"+clientOrg+"/pools?listall=true"));	
//		for (int i = 0; i < jsonPools.length(); i++) {
//			JSONObject jsonPool = (JSONObject) jsonPools.get(i);
//			String id = jsonPool.getString("id");
//			
//			ll.add(Arrays.asList(new Object[]{jsonPool}));
//		}
		
		for (String stacking_id : stackableJSONPoolsMap.keySet()) {
			List<JSONObject> stackableJSONPools = stackableJSONPoolsMap.get(stacking_id);
			ll.add(Arrays.asList(new Object[]{stackableJSONPools}));
		}
		
		return ll;
	}

	
	@DataProvider(name="getAvailableStackableSubscriptionPoolsData")
	public Object[][] getAvailableStackableSubscriptionPoolsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAvailableStackableSubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAvailableStackableSubscriptionPoolsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		Map<String,List<SubscriptionPool>> stackableSubscriptionPoolsMap = new HashMap<String,List<SubscriptionPool>>();
		
		// find all the SubscriptionPools with the same stacking_id
		for (List<Object> l : getAvailableSubscriptionPoolsDataAsListOfLists()) {
			SubscriptionPool pool = (SubscriptionPool)l.get(0);
			String stacking_id = CandlepinTasks.getPoolProductAttributeValue(sm_serverHostname, sm_serverPort, sm_serverPrefix, sm_clientUsername, sm_clientPassword, pool.poolId, "stacking_id");
			
			if (stacking_id==null) continue; // this pool is not stackable
			
			// add this available stackable pool to the stackableSubscriptionPoolsMap
			if (!stackableSubscriptionPoolsMap.containsKey(stacking_id)) stackableSubscriptionPoolsMap.put(stacking_id, new ArrayList<SubscriptionPool>());
			stackableSubscriptionPoolsMap.get(stacking_id).add(pool);
		}
		
		// assemble the rows of data
		for (String stacking_id : stackableSubscriptionPoolsMap.keySet()) {
			List<SubscriptionPool> stackableSubscriptionPools = stackableSubscriptionPoolsMap.get(stacking_id);
			
			// List<SubscriptionPool> stackableSubscriptionPools
			ll.add(Arrays.asList(new Object[]{stackableSubscriptionPools}));
		}
		
		return ll;
	}
}
