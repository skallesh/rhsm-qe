package rhsm.cli.tests;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.InstalledProduct;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
/**
 * @author skallesh
 * 
 * 
 */
@Test(groups = { "StorageTests","Tier3Tests" })
public class StorageBandTests extends SubscriptionManagerCLITestScript{
	Map<String, String> factsMap = new HashMap<String, String>();

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify if you attach 1 quantity of subscriptions,each covering 256 GB on a system with 1024 GB subscription , installed product is partially subscribed", 
			groups = { "partiallySubscribeStorageBandSubscription"},dataProvider="getStorageBandSubscriptions", enabled = true)
	public void partiallySubscribeStorageBandSubscription(Object Bugzilla,SubscriptionPool storagebandpool) throws JSONException, Exception{
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
			clienttasks.subscribe(null, null, storagebandpool.poolId, null, null, "1", null, null, null, null, null, null);
			
			List<String> providedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, storagebandpool.poolId);
			List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
			for (String providedProductId : providedProductIds) {
				InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", providedProductId, installedProducts);
				if (installedProduct!=null) {	
			Assert.assertEquals(installedProduct.status,"Partially Subscribed","Status of an installed product provided for by a Storage Band entitlement from a pool that covers only 256GB ");	
		
				}
	}}
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify if you attach 2 quantites of subscriptions , each covering that covers 256 GB on a system with 1024 GB subscription , installed product is fully subscribed", 
			groups = { "FullySubscribeStorageBandSubscription"},dataProvider="getStorageBandSubscriptions", enabled = true)
	public void FullySubscribeStorageBandSubscription(Object Bugzilla,SubscriptionPool storagebandpool) throws JSONException, Exception{
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
			clienttasks.subscribe(null, null, storagebandpool.poolId, null, null, null, null, null, null, null, null, null);
		
			
			List<String> providedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, storagebandpool.poolId);
			List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
			for (String providedProductId : providedProductIds) {
				InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", providedProductId, installedProducts);
				if (installedProduct!=null) {	
			Assert.assertEquals(installedProduct.status.trim()," Subscribed","Status of an installed product provided for by a Storage Band entitlement from a pool that covers only 256GB ");	
		}}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify if you auto-attach , installed product is fully subscribed", 
			groups = { "AutoAttachStorageBandSubscription"},dataProvider="getStorageBandSubscriptions", enabled = true)
	public void AutoAttachStorageBandSubscription(Object Bugzilla,SubscriptionPool storagebandpool) throws JSONException, Exception{
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		clienttasks.subscribe(true, null, (String)null, null, null, null, null, null, null, null, null, null);	
		List<String> providedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, storagebandpool.poolId);
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		for (String providedProductId : providedProductIds) {
			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", providedProductId, installedProducts);
			if (installedProduct!=null) {	
		Assert.assertEquals(installedProduct.status.trim(),"Subscribed","Status of an installed product provided for by a Storage Band entitlement from a pool that covers only 256GB ");	
	}}
	}
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify if you auto-heal , installed product is fully subscribed", 
			groups = { "AutoHealStorageBandSubscription"}, dataProvider="getStorageBandSubscriptions",enabled = true)
	public void AutoHealStorageBandSubscription(Object Bugzilla,SubscriptionPool storagebandpool) throws JSONException, Exception{
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		clienttasks.autoheal(null, true, null, null, null, null);
		clienttasks.run_rhsmcertd_worker(true);
		List<String> providedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, storagebandpool.poolId);
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		for (String providedProductId : providedProductIds) {
			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", providedProductId, installedProducts);
			if (installedProduct!=null) {	
		Assert.assertEquals(installedProduct.status.trim(),"Subscribed","Status of an installed product provided for by a Storage Band entitlement from a pool that covers only 256GB ");	
	}}
	}
	
	
	 @DataProvider(name="getStorageBandSubscriptions")

	 public Object[][] getStorageBandSubscriptionPoolsDataAs2dArray() throws JSONException,Exception {
		 System.out.println("Before return statement");
			return TestNGUtils.convertListOfListsTo2dArray(getStorageBandSubscriptionsPoolsDataAsListOfLists());
	 }
	 

	protected List<List<Object>> getStorageBandSubscriptionsPoolsDataAsListOfLists() throws JSONException, Exception {
			List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
			
			for (List<Object> list : getAvailableSubscriptionPoolsDataAsListOfLists(false)) {
				SubscriptionPool pool = (SubscriptionPool)(list.get(0));
				if (isPoolBandimited(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId)) {
					ll.add(Arrays.asList(new Object[]{null,	pool}));
				}
			}
			return ll;
		}
		/**
		 * A pool that is Storage limited was designed to cover ceph storage
		 * @param authenticator
		 * @param password
		 * @param url
		 * @param poolId
		 * @return
		 * @throws JSONException
		 * @throws Exception
		 */
		public static boolean isPoolBandimited (String authenticator, String password, String url, String poolId) throws JSONException, Exception {
			String value = CandlepinTasks.getPoolProductAttributeValue(authenticator,password,url,poolId,"storage_band");
			if (value==null)  return false;
			return true;
			}
	
	@BeforeClass(groups={"setup"})
	public void CustomiseCephFacts() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, false, null, null, null);
		factsMap.clear();
		factsMap.put("band.storage.usage", "1024");
		clienttasks.createFactsFileWithOverridingValues(factsMap);

	}
	@BeforeClass(groups={"setup"})
	public void RemoveCephFacts() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, false, null, null, null);
		clienttasks.removeAllFacts();

	}
}
