package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"StackingTests"})
public class StackingTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager: subscribe to each pool with the same stacking_id to achieve compliance",
			enabled=false,
			groups={"blockedByBug-739671", "blockedByBug-740377"},
			dataProvider="getAvailableStackableSubscriptionPoolsData")
	//@ImplementsNitrateTest(caseId=)
	public void StackEachPoolToAchieveCompliance_Test(List<SubscriptionPool> stackableSubscriptionPools) throws JSONException, Exception{
		
		// loop through the pools to determine the minimum socket count for which one of each stackable pool is needed to achieve compliance
		int minimumSockets=0;
		for (SubscriptionPool pool : stackableSubscriptionPools) {
			String sockets = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "sockets");
			minimumSockets+=Integer.valueOf(sockets);
		}
		
		// override the system facts setting the socket count to a value for which all the stackable subscriptions are needed to achieve compliance
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("cpu.cpu_socket(s)", String.valueOf(minimumSockets));
		//factsMap.put("lscpu.cpu_socket(s)", String.valueOf(minimumSockets));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null,true,null,null,null);
		
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
		for (ProductCert productCert : installedProductCerts) {
			RemoteFileTasks.runCommandAndAssert(client, "cp "+productCert.file+" "+productCertDirForStacking, 0);
		}
		clienttasks.config(null,null,true,new String[]{"rhsm","productCertDir".toLowerCase(),productCertDirForStacking});
		
		// subscribe to each pool and assert "Partially Subscribe" status and overall incompliance until the final pool is subscribed
		Assert.assertEquals(clienttasks.getFactValue(ComplianceTests.factNameForSystemCompliance), ComplianceTests.factValueForSystemNonCompliance,
			"Prior to subscribing to any of the stackable subscription pools, the overall system entitlement status should NOT be valid/compliant.");
		int s=0;
		for (SubscriptionPool pool : stackableSubscriptionPools) {
			File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool);
			if (++s < stackableSubscriptionPools.size()) {
				
				// assert installed products are Partially Subscribed
				for (ProductCert installedProductCert : installedProductCerts) {
					InstalledProduct installedProduct = clienttasks.getInstalledProductCorrespondingToProductCert(installedProductCert);
					Assert.assertEquals(installedProduct.status, "Partially Subscribed", "After subscribing to stackable subscription pool for ProductId '"+pool.productId+"', the status of Installed Product '"+installedProduct.productName+"' should be Partially Subscribed.");
				}
				
				// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=739671
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="739671"; 
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.info("Skipping the value assertion for fact '"+ComplianceTests.factNameForSystemCompliance+"' while bug '"+bugId+"' is open.");
				} else {
				// END OF WORKAROUND
				
				// assert overall system compliance is not yet valid
				Assert.assertEquals(clienttasks.getFactValue(ComplianceTests.factNameForSystemCompliance), ComplianceTests.factValueForSystemPartialCompliance,
					"The overall system entitlement status should NOT be valid/compliant until we have subscribed to enough stackable subscription pools to meet coverage for the system's cpu.socket(s) '"+minimumSockets+"'.");		
				}
			}
		}
		
		// assert installed products are fully Subscribed
		for (ProductCert installedProductCert : installedProductCerts) {
			InstalledProduct installedProduct = clienttasks.getInstalledProductCorrespondingToProductCert(installedProductCert);
			Assert.assertEquals(installedProduct.status, "Subscribed", "After subscribing to enough stackable subscription pools to cover the systems sockets count ("+minimumSockets+"), the status of Installed Product '"+installedProduct.productName+"' should be fully Subscribed.");
		}
		// assert overall system compliance is now valid
		Assert.assertEquals(clienttasks.getFactValue(ComplianceTests.factNameForSystemCompliance), ComplianceTests.factValueForSystemCompliance,
			"After having subscribed to all the stackable subscription pools needed to meet coverage for the system's cpu.socket(s) '"+minimumSockets+"', the overall system entitlement status should be valid/compliant.");


		
	}
	
	
	@Test(	description="subscription-manager: subscribe to each pool with the same stacking_id to achieve compliance",
			enabled=true,
			groups={"debugTest","blockedByBug-739671", "blockedByBug-740377"},
			dataProvider="getAvailableStackableAttributeSubscriptionPoolsData")
	//@ImplementsNitrateTest(caseId=)
	public void StackEachPoolToAchieveAttributeCompliance_Test(Object bugzilla, String attribute, List<SubscriptionPool> stackableAttributeSubscriptionPools) throws JSONException, Exception{
		
		// loop through the pools to determine the minimum attribute count for which one of each stackable pool is needed to achieve compliance of the provided products
		Integer minimumAttributeValue=0;
		Map<String,Integer> poolProductAttributeValueMap = new HashMap<String,Integer>();
		for (SubscriptionPool pool : stackableAttributeSubscriptionPools) {
			String attributeValue = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, attribute);
			minimumAttributeValue+=Integer.valueOf(attributeValue);
			poolProductAttributeValueMap.put(pool.poolId, Integer.valueOf(attributeValue));
		}
		
		// find all of the currently installed products that these stackable pools commonly provide
		List<ProductCert> currentProductCerts = clienttasks.getCurrentProductCerts();
		List<ProductCert> providedCurrentProductCerts = new ArrayList<ProductCert>(); providedCurrentProductCerts.addAll(currentProductCerts);
		for (SubscriptionPool pool : stackableAttributeSubscriptionPools) {
			List<ProductCert> productCertsProvidedForByThisPool = clienttasks.getCurrentProductCertsCorrespondingToSubscriptionPool(pool);
			for (ProductCert currentProductCert : currentProductCerts) {
				if (!productCertsProvidedForByThisPool.contains(currentProductCert)) providedCurrentProductCerts.remove(currentProductCert);
			}
		}
		if (providedCurrentProductCerts.size()==0) throw new SkipException("Could not find any installed products that these stackable pools commonly provide for.");

		
//		// loop through the stackable pools until we find the first one that covers product certs that are currently installed
//		// (put that subscription at the front of the list) (remember the installed product certs)
//		List<ProductCert> installedProductCerts = new ArrayList<ProductCert>();
//		for (int i=0; i<stackableAttributeSubscriptionPools.size(); i++) {
//			SubscriptionPool pool = stackableAttributeSubscriptionPools.get(i);
//			installedProductCerts = clienttasks.getCurrentProductCertsCorrespondingToSubscriptionPool(pool);
//			if (installedProductCerts.size()>0) {
//				stackableAttributeSubscriptionPools.remove(i);
//				stackableAttributeSubscriptionPools.add(0, pool);
//				break;
//			}
//		}
//		if (installedProductCerts.size()==0) throw new SkipException("Could not find any installed products for which stacking these pools would achieve compliance.");

//		// reconfigure such that only these product certs are installed (copy them to a /tmp/sm-stackingProductDir)
//		for (ProductCert productCert : installedProductCerts) {
//			RemoteFileTasks.runCommandAndAssert(client, "cp "+productCert.file+" "+productCertDirForStacking, 0);
//		}
//		clienttasks.config(null,null,true,new String[]{"rhsm","productCertDir".toLowerCase(),productCertDirForStacking});
		
		
		
		// override the system facts setting the attribute count to a value for which all the stackable subscriptions are needed to achieve compliance
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("memory.memtotal", "1");
		factsMap.put("cpu.cpu_socket(s)", "1");
		factsMap.put("cpu.core(s)_per_socket", "1");
		if (attribute.equals("ram")) {
			factsMap.put("memory.memtotal", String.valueOf(minimumAttributeValue*1048576)); // "memory.memtotal" is stored in Kilobytes; "ram" is specified in Gigabytes; for conversions, see http://www.whatsabyte.com/P1/byteconverter.htm
		}
		if (attribute.equals("sockets")) {
			factsMap.put("cpu.cpu_socket(s)", String.valueOf(minimumAttributeValue));
		}
		if (attribute.equals("cores")) {
			if (Integer.valueOf(minimumAttributeValue)%2==0) {	// configure facts for an even number of cores
				factsMap.put("cpu.core(s)_per_socket", "2");
				factsMap.put("cpu.cpu_socket(s)", String.valueOf(Integer.valueOf(minimumAttributeValue)/2));
			} else {	// configure facts for an odd number of cores
				factsMap.put("cpu.core(s)_per_socket", "1");
				factsMap.put("cpu.cpu_socket(s)",  String.valueOf(minimumAttributeValue));
			}
		}
		clienttasks.createFactsFileWithOverridingValues(factsMap);
//		clienttasks.facts(null,true,null,null,null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null,null,null,null,null,null,null,(List<String>)null,null,null,null,true,false,null,null,null);
		
		
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		for (ProductCert providedCurrentProductCert : providedCurrentProductCerts) {
			InstalledProduct providedInstalledProduct = clienttasks.getInstalledProductCorrespondingToProductCert(providedCurrentProductCert,installedProducts);
			Assert.assertEquals(providedInstalledProduct.status,"Not Subscribed","Prior to subscribing to any of the stackable subscription pools, Installed product '"+providedInstalledProduct.productName+"' which is provided for by the subscription stack should have this status.");
			Assert.assertEquals(providedInstalledProduct.statusDetails,"Not covered by a valid subscription.","Prior to subscribing to any of the stackable subscription pools, Installed product '"+providedInstalledProduct.productName+"' which is provided for by the subscription stack should have this status details.");
		}
		
		
		// subscribe to each pool and assert "Partially Subscribe" status and overall incompliance until the final pool is subscribed
//		Assert.assertEquals(clienttasks.getFactValue(ComplianceTests.factNameForSystemCompliance), ComplianceTests.factValueForSystemNonCompliance,
//			"Prior to subscribing to any of the stackable subscription pools, the overall system entitlement status should NOT be valid/compliant.");

		
		int s=0;
		for (SubscriptionPool pool : stackableAttributeSubscriptionPools) {
//			File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool);
			clienttasks.subscribe(null,null,pool.poolId,null,null,null,null,null,null,null,null);
			Integer attibuteValueStackedThusFar = 0;
			if (++s < stackableAttributeSubscriptionPools.size()) {
				
				attibuteValueStackedThusFar += poolProductAttributeValueMap.get(pool.poolId);
				
//				// assert installed products are Partially Subscribed
//				for (ProductCert installedProductCert : installedProductCerts) {
//					InstalledProduct installedProduct = clienttasks.getInstalledProductCorrespondingToProductCert(installedProductCert);
//					Assert.assertEquals(installedProduct.status, "Partially Subscribed", "After subscribing to stackable subscription pool for ProductId '"+pool.productId+"', the status of Installed Product '"+installedProduct.productName+"' should be Partially Subscribed.");
//
//					// TODO Assert the status details of the consumed subs and/or the installed products and/or the list status
//					//Status Details:    Only covers 8 of 16 cores.
//				}
				// assert provided installed products are Partially Subscribed
				installedProducts = clienttasks.getCurrentlyInstalledProducts();
				for (ProductCert providedCurrentProductCert : providedCurrentProductCerts) {
					InstalledProduct providedInstalledProduct = clienttasks.getInstalledProductCorrespondingToProductCert(providedCurrentProductCert,installedProducts);
					Assert.assertEquals(providedInstalledProduct.status,"Partially Subscribed","After subscribing to stackable subscription pool '"+pool.subscriptionName+"' id="+pool.poolId+", Installed product '"+providedInstalledProduct.productName+"' which is provided for by the subscription stack should have this status.");

					if (attribute.equals("cores")) {
						Assert.assertEquals(providedInstalledProduct.statusDetails,String.format("Only covers %s of %s cores.",attibuteValueStackedThusFar,minimumAttributeValue),"After subscribing to stackable subscription pool '"+pool.subscriptionName+"' poolId="+pool.poolId+", Installed product '"+providedInstalledProduct.productName+"' which is provided for by the subscription stack should have thus far accumulated these status details.");
					}
					if (attribute.equals("ram")) {
						Assert.assertEquals(providedInstalledProduct.statusDetails,String.format("Only covers %sGB of %sGB of RAM.",attibuteValueStackedThusFar,minimumAttributeValue),"After subscribing to stackable subscription pool '"+pool.subscriptionName+"' poolId="+pool.poolId+", Installed product '"+providedInstalledProduct.productName+"' which is provided for by the subscription stack should have thus far accumulated these status details.");
					}
					// TODO Assert the status details of the consumed subs and/or the installed products and/or the list status
					//Status Details:    Only covers 8 of 16 cores.
				}
				
				
//				// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=739671
//				boolean invokeWorkaroundWhileBugIsOpen = true;
//				String bugId="739671"; 
//				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
//				if (invokeWorkaroundWhileBugIsOpen) {
//					log.info("Skipping the value assertion for fact '"+ComplianceTests.factNameForSystemCompliance+"' while bug '"+bugId+"' is open.");
//				} else {
//				// END OF WORKAROUND
//				
//				// assert overall system compliance is not yet valid
//				Assert.assertEquals(clienttasks.getFactValue(ComplianceTests.factNameForSystemCompliance), ComplianceTests.factValueForSystemPartialCompliance,
//					"The overall system entitlement status should NOT be valid/compliant until we have subscribed to enough stackable subscription pools to meet coverage for the system's '"+attribute+"' count of '"+minimumAttributeValue+"'.");		
//				}
			} else {
				
				// assert provided installed products are fully Subscribed
				installedProducts = clienttasks.getCurrentlyInstalledProducts();
				for (ProductCert providedCurrentProductCert : providedCurrentProductCerts) {
					InstalledProduct providedInstalledProduct = clienttasks.getInstalledProductCorrespondingToProductCert(providedCurrentProductCert,installedProducts);
					Assert.assertEquals(providedInstalledProduct.status,"Subscribed","After subscribing to the final stackable subscription pool '"+pool.subscriptionName+"' poolId="+pool.poolId+", Installed product '"+providedInstalledProduct.productName+"' which is provided for by the subscription stack should have this status.");
					Assert.assertEquals(providedInstalledProduct.statusDetails,"","The status details of Installed product '"+providedInstalledProduct.productName+"' which is fully subscribed should now be this.");
				}
			}
		}
		
//		// assert installed products are fully Subscribed
//		for (ProductCert installedProductCert : installedProductCerts) {
//			InstalledProduct installedProduct = clienttasks.getInstalledProductCorrespondingToProductCert(installedProductCert);
//			Assert.assertEquals(installedProduct.status, "Subscribed", "After subscribing to enough stackable subscription pools to cover the system's '"+attribute+"' count ("+minimumAttributeValue+"), the status of Installed Product '"+installedProduct.productName+"' should be fully Subscribed.");
//		}
			

		
//		// assert overall system compliance is now valid
//		Assert.assertEquals(clienttasks.getFactValue(ComplianceTests.factNameForSystemCompliance), ComplianceTests.factValueForSystemCompliance,
//			"After having subscribed to all the stackable subscription pools needed to meet coverage for the system's cpu.socket(s) '"+minimumAttributeValue+"', the overall system entitlement status should be valid/compliant.");
	}
	
	@DataProvider(name="getAvailableStackableAttributeSubscriptionPoolsData")
	public Object[][] getAvailableStackableAttributeSubscriptionPoolsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAvailableStackableAttributeSubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAvailableStackableAttributeSubscriptionPoolsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		Map<String,List<SubscriptionPool>> stackableSubscriptionPoolsMap = new HashMap<String,List<SubscriptionPool>>();
		
		// find all the SubscriptionPools with the same stacking_id
		for (List<Object> l : getAvailableSubscriptionPoolsDataAsListOfLists()) {
			SubscriptionPool pool = (SubscriptionPool)l.get(0);
			String stacking_id = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "stacking_id");
			
			// skip non-stackable pools
			if (stacking_id==null) continue;
			
			// skip instance-based pools (these are covered in InstanceTests.java) 
			if (CandlepinTasks.isPoolProductInstanceBased(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId)) continue;
			
			// add this available stackable pool to the stackableSubscriptionPoolsMap
			if (!stackableSubscriptionPoolsMap.containsKey(stacking_id)) stackableSubscriptionPoolsMap.put(stacking_id, new ArrayList<SubscriptionPool>());
			stackableSubscriptionPoolsMap.get(stacking_id).add(pool);
		}
		
		// assemble the rows of data
		for (String stacking_id : stackableSubscriptionPoolsMap.keySet()) {
			List<SubscriptionPool> stackableSubscriptionPools = stackableSubscriptionPoolsMap.get(stacking_id);
			
			for (String attribute : new String[]{"sockets","cores","ram"}) {
				List<SubscriptionPool> stackableAttributeSubscriptionPools = new ArrayList<SubscriptionPool>();

				for (SubscriptionPool stackableSubscriptionPool : stackableSubscriptionPools) {
					if (CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, stackableSubscriptionPool.poolId, attribute)!=null) {
						stackableAttributeSubscriptionPools.add(stackableSubscriptionPool);
					}
				}
				
				if (!stackableAttributeSubscriptionPools.isEmpty()) {
					// Object bugzilla, String attribute, List<SubscriptionPool> stackableAttributeSubscriptionPools
					ll.add(Arrays.asList(new Object[]{null, attribute, stackableAttributeSubscriptionPools}));					
				}
			}

		}
		
		return ll;
	}
	
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 733327 - stacking entitlements reports as distinct entries in cli list --installed https://github.com/RedHatQE/rhsm-qe/issues/193
	// TODO Bug 740377 - Stacking Partially Compliant / Yellow State is Broken https://github.com/RedHatQE/rhsm-qe/issues/194
	// TODO Bug 743710 - Subscription manager displays incorrect status for partially subscribed subscription https://github.com/RedHatQE/rhsm-qe/issues/195
	//      MAYBE THIS ONE BELONGS IN COMPLIANCE TESTS?
	// TODO Bug 726409 - [RFE] Support for certificate stacking https://github.com/RedHatQE/rhsm-qe/issues/196
	// TODO Bug 845126 - system.entitlements_valid goes from valid to partial after oversubscribing https://github.com/RedHatQE/rhsm-qe/issues/197
		
	// Configuration methods ***********************************************************************

	
	@BeforeClass(groups={"setup"})
	public void setupBeforeClass() {
		
		// clean out the productCertDirs
		RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+productCertDirForStacking, 0);
		RemoteFileTasks.runCommandAndAssert(client, "mkdir "+productCertDirForStacking, 0);
		
		this.productCertDir = clienttasks.productCertDir;
	}
	
//	@BeforeMethod(groups={"setup"})
	@AfterClass(groups={"setup"},alwaysRun=true)
	public void cleanupAfterClass() {
		if (clienttasks==null) return;
		
		// remove overriding test facts from last test
		clienttasks.deleteFactsFileWithOverridingValues();	

//		clienttasks.unsubscribe(true, (BigInteger)null, null, null, null);	// return all entitlements from last test
		
		// restore original productCertDir
		if (this.productCertDir!=null) clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", this.productCertDir);

	}
	
	// Protected methods ***********************************************************************

	protected final String productCertDirForStacking = "/tmp/sm-stackingProductDir";
	protected String productCertDir = null;

	
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
			String stacking_id = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "stacking_id");
			
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
