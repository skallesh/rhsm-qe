package rhsm.cli.tests;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.InstalledProduct;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;

import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.github.redhatqe.polarize.metadata.TestDefinition;


/**
 * @author skallesh
 * 
 * References:
 *   Design Doc: http://www.candlepinproject.org/docs/candlepin/storage_band_subscriptions.html
 *   Testcases:  http://etherpad.corp.redhat.com/O7VqZCon7m
 *   
 *   Note: This test script was developed for execution against an standalone Candlepin server
 *   with TESTDATA deployed and assumes that multiple pools are available that provide entitlements
 *   in 1 TB quantity increments up to 256 TB.  In otherwords the pool quantity is 256.  The system under
 *   test will be instrumented with a "band.storage.usage" fact in excess of 256 and less than 512 thereby
 *   permitting tests to stage across multiple pools to achieve compliance.
 */
@Test(groups = { "StorageBandTests","Tier3Tests" })
public class StorageBandTests extends SubscriptionManagerCLITestScript{
	Map<String, String> factsMap = new HashMap<String, String>();
	int bandStorageUsage = 300;	// TB (choose a value greater that 256 and less than 512)

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21462", "RHEL7-51711"})
	@Test(description = "verify that attaching a quantity of 1 entitlement from a pool capable of covering 256TB on a system with 300TB of usage, installed product will be partially subscribed", 
			groups = {"PartiallySubscribeStorageBandSubscription"},dataProvider="getStorageBandSubscriptions", enabled = true)
	public void PartiallySubscribeStorageBandSubscription(Object Bugzilla,SubscriptionPool storagebandpool) throws JSONException, Exception{
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.subscribe(null, null, storagebandpool.poolId, null, null, "1", null, null, null, null, null, null, null);
		
		List<String> providedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, storagebandpool.poolId);
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		for (String providedProductId : providedProductIds) {
			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", providedProductId, installedProducts);
			if (installedProduct!=null) {	
				Assert.assertEquals(installedProduct.status.trim(),"Partially Subscribed","Status of installed product '"+installedProduct.productName+"' provided for by Storage Band entitlement pools that covers only 256TB on a system using '"+bandStorageUsage+"'TBs (that has been subscribed to pool SKU '"+storagebandpool.productId+"' '"+storagebandpool.subscriptionName+"' with quantity 1)");	
				String expectedReason = String.format("Only supports %dTB of %dTB of storage.", 1, bandStorageUsage);
				Assert.assertTrue(installedProduct.statusDetails.contains(expectedReason),"Status Details includes expected reason '"+expectedReason+"'.");	
			}
		}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21463", "RHEL7-51712"})
	@Test(description = "verify if you attach one subscription that covers 256TB on a system with 300TB of usage, the installed product will be partially subscribed", 
			groups = { "SubscribeStorageBandSubscription"},dataProvider="getStorageBandSubscriptions", enabled = true)
	public void SubscribeStorageBandSubscription(Object Bugzilla,SubscriptionPool storagebandpool) throws JSONException, Exception{
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.subscribe(null, null, storagebandpool.poolId, null, null, null, null, null, null, null, null, null, null);
		
		List<String> providedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, storagebandpool.poolId);
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		for (String providedProductId : providedProductIds) {
			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", providedProductId, installedProducts);
			if (installedProduct!=null) {	
				Assert.assertEquals(installedProduct.status.trim(),"Partially Subscribed","Status of installed product '"+installedProduct.productName+"' provided for by Storage Band entitlement pools that covers only 256TB on a system using '"+bandStorageUsage+"'TBs (that has been subscribed to pool SKU '"+storagebandpool.productId+"' '"+storagebandpool.subscriptionName+"')");	
				String expectedReason = String.format("Only supports %dTB of %dTB of storage.", Integer.valueOf(storagebandpool.quantity), bandStorageUsage);
				Assert.assertTrue(installedProduct.statusDetails.contains(expectedReason),"Status Details includes expected reason '"+expectedReason+"'.");	
			}
		}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21460", "RHEL7-51709"})
	@Test(description = "verify after auto-attaching a system using 300TB of storage, installed storage product is fully subscribed from multiple pools that provide 256TB of coverage.", 
			groups = { "AutoAttachStorageBandSubscription"},dataProvider="getStorageBandSubscriptions", enabled = true)
	public void AutoAttachStorageBandSubscription(Object Bugzilla,SubscriptionPool storagebandpool) throws JSONException, Exception{
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.subscribe(true, null, (String)null, null, null, null, null, null, null, null, null, null, null);	
		List<String> providedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, storagebandpool.poolId);
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		for (String providedProductId : providedProductIds) {
			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", providedProductId, installedProducts);
			if (installedProduct!=null) {
				Assert.assertEquals(installedProduct.status.trim(),"Subscribed","Status of installed product '"+installedProduct.productName+"' provided for by Storage Band entitlement pools that covers only 256TB on a system using '"+bandStorageUsage+"'TBs (that has been autosubscribed)");
			}
		}
	}
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21461", "RHEL7-51710"})
	@Test(description = "verify if you auto-heal a system using 300TB of storage, installed storage product is fully subscribed from multiple pools that provide 256TB of coverage.", 
			groups = { "AutoHealStorageBandSubscription"}, dataProvider="getStorageBandSubscriptions",enabled = true)
	public void AutoHealStorageBandSubscription(Object Bugzilla,SubscriptionPool storagebandpool) throws JSONException, Exception{
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.autoheal(null, true, null, null, null, null, null);
		clienttasks.run_rhsmcertd_worker(true);
		List<String> providedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, storagebandpool.poolId);
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		for (String providedProductId : providedProductIds) {
			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", providedProductId, installedProducts);
			if (installedProduct!=null) {
				Assert.assertEquals(installedProduct.status.trim(),"Subscribed","Status of installed product '"+installedProduct.productName+"' provided for by Storage Band entitlement pools that covers only 256TB on a system using '"+bandStorageUsage+"'TBs (that has been autohealed)");	
			}
		}
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
/* unnecessary
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, false, null, null, null);
unnecessary */
		factsMap.clear();
		factsMap.put("band.storage.usage", String.valueOf(bandStorageUsage));
		clienttasks.createFactsFileWithOverridingValues(factsMap);

	}
	@AfterClass(groups={"setup"})
	public void RemoveCephFacts() throws Exception {
/* unnecessary
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, false, null, null, null);
		clienttasks.removeAllFacts();
unnecessary */

		clienttasks.deleteFactsFileWithOverridingValues();
	}
}
