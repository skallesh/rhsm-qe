package rhsm.cli.tests;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.DataProvider;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.Repo;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
/**
 * @author skallesh
 * 
 * 
 */
public class SKULevelContentOverrides extends SubscriptionManagerCLITestScript{
	static String label = null;
	String resourcePath=null;
	String requestBody=null;

	@Test(description = "Verify content can be overriden at SKU level,content overriden at sku level can be enabled/disabled by using subscription-manager repos --enable/--disable commands and enabled repo is given prefrence over disabled repo",
			groups = { "OverrideAtSKULevel"},dataProvider="getSubscriptions", enabled = true)
	public void OverrideAtSKULevel(Object Bugzilla,SubscriptionPool subscriptionpool) throws JSONException, Exception{
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null));
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);
		clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null, null);

		List<Repo> availableRepos = clienttasks.getCurrentlySubscribedRepos();	
		List<String> enableRepoIds = new ArrayList<String>();
		List<String> disableRepoIds = new ArrayList<String>();
		Map<String, String> attributesMap = new HashMap<String, String>();
		String contentIDToEnable="999999999999999";
		String contentIDToDisable="00000000000000";

		for (Repo repo : availableRepos) {
			if (repo.enabled) {
				disableRepoIds.add(repo.repoId);
			} else {
				enableRepoIds.add(repo.repoId);
			}
		}


		/*To override(enable) a disabled repo*/
		if(enableRepoIds.isEmpty()){
			requestBody=CandlepinTasks.createContentRequestBody("foonameenable", contentIDToEnable, "foolabelenable", "yum", "Foo Vendor", "/foo/path", "/foo/path/gpg", null, null, null, null).toString();
			resourcePath = "/content";
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+ownerKey+resourcePath;
			CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,resourcePath, requestBody);
			String productid = getPoolprovidedProducts(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, subscriptionpool.poolId, "productId");
			CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, ownerKey,productid, contentIDToEnable, false);
			clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
			clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null, null);
			availableRepos = clienttasks.getCurrentlySubscribedRepos();
			for (Repo repo : availableRepos) {
				if (!(repo.enabled)) {
					enableRepoIds.add(repo.repoId);
				}
			}
		}
		String repoIdToEnable=getContent(sm_serverAdminUsername,sm_serverAdminPassword, sm_serverUrl,enableRepoIds.get(randomGenerator.nextInt(enableRepoIds.size())));
		resourcePath="/owners/"+ownerKey+"/products/"+subscriptionpool.productId+"?exclude=id&exclude=name&exclude=multiplier&exclude=productContent&exclude=dependentProductIds&exclude=href&exclude=created&exclude=updated&exclude=attributes.created&exclude=attributes.updated";
		JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,resourcePath));	
		resourcePath = "/owners/"+ownerKey+"/products/"+subscriptionpool.productId;
		JSONArray jsonProductAttributesToEnable = jsonPool.getJSONArray("attributes");
		JSONObject jsonDataToEnable = new JSONObject();
		attributesMap.put("name", "content_override_enabled");
		attributesMap.put("value", repoIdToEnable);
		jsonProductAttributesToEnable.put(attributesMap);
		jsonDataToEnable.put("attributes",jsonProductAttributesToEnable);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath, jsonDataToEnable);
		CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, ownerKey);
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null, null);
		Assert.assertTrue(clienttasks.repos(null, true, null,(String)null, null, null, null, null).getStdout().contains(label));
		/*verify repo overriden at sku level can be overriden with subscription-manager repos --disable command*/
		clienttasks.repos(null, null, null,null , label, null, null, null);
		Assert.assertTrue(clienttasks.repos(null, null, true,(String)null, null, null, null, null).getStdout().contains(label));

		/*To override(disable) a enabled repo*/
		if(disableRepoIds.isEmpty()){
			requestBody =CandlepinTasks.createContentRequestBody("foonamedisable", contentIDToDisable, "foolabeldisable", "yum", "Foo Vendor", "/foo/path", "/foo/path/gpg", null, null, null, null).toString();
			resourcePath = "/content";
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
			CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,resourcePath, requestBody);
			String productid =getPoolprovidedProducts(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, subscriptionpool.poolId, "productId");
			CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, ownerKey,productid, contentIDToDisable, true);
			clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
			clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null, null);
			availableRepos = clienttasks.getCurrentlySubscribedRepos();
			for (Repo repo : availableRepos) {
				if (repo.enabled) {
					disableRepoIds.add(repo.repoId);

				} 
			}

		}
		String repoIdToDisable=getContent(sm_serverAdminUsername,sm_serverAdminPassword, sm_serverUrl,disableRepoIds.get(randomGenerator.nextInt(disableRepoIds.size())));
		resourcePath="/owners/"+ownerKey+"/products/"+subscriptionpool.productId+"?exclude=id&exclude=name&exclude=multiplier&exclude=productContent&exclude=dependentProductIds&exclude=href&exclude=created&exclude=updated&exclude=attributes.created&exclude=attributes.updated";
		jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,resourcePath));	
		resourcePath = "/owners/"+ownerKey+"/products/"+subscriptionpool.productId;
		JSONArray jsonProductAttributesToDisable = jsonPool.getJSONArray("attributes");
		attributesMap.clear();
		attributesMap.put("name", "content_override_disabled");
		attributesMap.put("value", repoIdToDisable);
		jsonProductAttributesToDisable.put(attributesMap);
		JSONObject jsonDataToDisable = new JSONObject();
		jsonDataToDisable.put("attributes",jsonProductAttributesToDisable);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath, jsonDataToDisable);
		CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, ownerKey);
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null, null);
		Assert.assertTrue(clienttasks.repos(null, null, true,(String)null, null, null, null, null).getStdout().contains(label));

		/*verify repo overriden at sku level can be overriden with subscription-manager repos --enable command*/

		clienttasks.repos(null, null, null, label,null , null, null, null);
		Assert.assertTrue(clienttasks.repos(null, true, null,(String)null, null, null, null, null).getStdout().contains(label));


		/*enabled repo is given preference over disabled repo*/
		List<String> enabledRepoIds = new ArrayList<String>();
		//currently available enabled repos that was overriden at sku level
		for (Repo repo : availableRepos) {
			if ((repo.enabled)) {
				enabledRepoIds.add(repo.repoId);
			}
		}
		String repoId=enabledRepoIds.get(randomGenerator.nextInt(enabledRepoIds.size()));
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		//get all the currently available subscription and attach		
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		for (SubscriptionPool pool : pools)
			//ensure that pool id you are trying attached is different from the pool id attached earlier			
			if(!(pool.productId).equals(subscriptionpool.productId)){
				clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null, null);
				//if the repos availble from the attached subscription doesnot contain the disabled repoid from the earlier subscription				
				if(!(clienttasks.repos(true, null, null,(String) null, null, null, null, null).getStdout().contains(repoId))){

					//if the disabled repo id from earlier subscription was a created and mapped 
					if((repoId.equals("foolabelenable")) ){
						requestBody=CandlepinTasks.createContentRequestBody("foonameenable", contentIDToEnable, "foolabelenable", "yum", "Foo Vendor", "/foo/path", "/foo/path/gpg", null, null, null, null).toString();
						resourcePath = "/content";
						if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+ownerKey+resourcePath;
						CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,resourcePath, requestBody);
						String productid = getPoolprovidedProducts(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, pool.poolId, "productId");
						CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, ownerKey,productid, contentIDToEnable, false);
						clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
						clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null, null);
					}
					//	if the disabled repo that was overriden at sku level id from earlier subscription is not doesnot match with repoids available by current subscription,create a content and map for both subscriptions 			
					else{
						repoId="foolabel";
						requestBody=CandlepinTasks.createContentRequestBody("fooname", "1111111111111111", repoId, "yum", "Foo Vendor", "/foo/path", "/foo/path/gpg", null, null, null, null).toString();
						resourcePath = "/content";
						clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null, null);
						if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+ownerKey+resourcePath;
						CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,resourcePath, requestBody);
						String productid = getPoolprovidedProducts(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, pool.poolId, "productId");
						CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, ownerKey,productid, "1111111111111111", false);
						CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,resourcePath, requestBody);
						productid =getPoolprovidedProducts(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, subscriptionpool.poolId, "productId");
						CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, ownerKey,productid, "1111111111111111", true);
						clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
						clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null, null);
						break;
					}}
			}
		//assert that repo is not available in repos list-enabled
		Assert.assertFalse(clienttasks.repos(null, true, null,(String)null, null, null, null, null).getStdout().contains(repoId));
		//assert that repo is  available in repos list-disabled
		Assert.assertTrue(clienttasks.repos(true, null,null,(String)null, null, null, null, null).getStdout().contains(repoId));
		clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null, null);
		//assert that repo is now available in repos list-enabled after attaching the subscription
		Assert.assertTrue(clienttasks.repos(null, true, null,(String)null, null, null, null, null).getStdout().contains(repoId));


	}

	@SuppressWarnings("unused")
	public String getPoolprovidedProducts(String authenticator, String password, String url, String poolId, String attributeName) throws JSONException, Exception {
		String attributeValue = null;

		JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(authenticator,password,url,"/pools/"+poolId));	
		JSONArray jsonAttributes = jsonPool.getJSONArray("providedProducts");
		// loop through the attributes of this pool looking for the attributeName attribute
		for (int j = 0; j < jsonAttributes.length(); j++) {
			JSONObject jsonAttribute = (JSONObject) jsonAttributes.get(j);
			attributeValue = jsonAttribute.getString(attributeName);
			break;
		}if(attributeValue==null){
			jsonAttributes = jsonPool.getJSONArray("derivedProvidedProducts");
			for (int j = 0; j < jsonAttributes.length(); j++) {
				JSONObject jsonAttribute = (JSONObject) jsonAttributes.get(j);
				attributeValue = jsonAttribute.getString(attributeName);
				break;
			}
		}
		return attributeValue;

	}

	@DataProvider(name="getSubscriptions")
	public Object[][] getSubscriptionPoolsDataAs2dArray() throws JSONException,Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getSubscriptionsPoolsDataAsListOfLists());
	}


	protected List<List<Object>> getSubscriptionsPoolsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		clienttasks.register(sm_client1Username, sm_client1Password, sm_client1Org);
		for (SubscriptionPool availableSubscriptionPoolsMatchingInstalled :SubscriptionPool.parse(clienttasks.list(null, true, null, null, null, null, true, null, null, null, null, null, null).getStdout())){

			SubscriptionPool pool = availableSubscriptionPoolsMatchingInstalled;

			ll.add(Arrays.asList(new Object[]{null,	pool}));
			break;

		}
		return ll;
	}


	public static String  getContent (String authenticator , String password , String serverurl,String repoids ) throws JSONException, Exception{
		String contentId =null;
		JSONArray jsonPool = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(authenticator,password,serverurl,"/content/"));	
		for (int j = 0; j < jsonPool.length(); j++) {
			JSONObject jsonContentAttribute = (JSONObject) jsonPool.get(j);
			if (jsonContentAttribute.getString("label").equals(repoids)) {
				// the actual attribute value is null, return null
				contentId = jsonContentAttribute.getString("id"); 
				label = jsonContentAttribute.getString("label");

				break;
			}
		}

		return contentId;



	}}
