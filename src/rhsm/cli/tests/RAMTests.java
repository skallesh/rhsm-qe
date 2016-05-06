package rhsm.cli.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;

/**
 * @author skallesh
 *
 *
 */

@Test(groups={ "RAMTests","Tier2Tests"})
public class RAMTests extends SubscriptionManagerCLITestScript {
	Map<String, String> factsMap = new HashMap<String, String>();


	// Test methods ***********************************************************************
	
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify status of partially covered Ram based products", 
			groups = {"PartiallySubscribedRamBasedProducts"}, enabled = true)
	public void PartiallySubscribedRamBasedProducts() throws JSONException,Exception {
		Integer ram = 20; //GB
		factsMap.clear();
		factsMap.put("uname.machine", "x86_64");
		factsMap.put("cpu.core(s)_per_socket", "1");
		factsMap.put("memory.memtotal", String.valueOf(GBToKBConverter(ram)));
		factsMap.put("cpu.cpu_socket(s)", "1");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null);
		
		List<String> ramSubscriptionNames = new ArrayList<String>();
		for(SubscriptionPool pool :getRamBasedSubscriptions()){
			clienttasks.subscribe(null, null, pool.poolId, null, null, "1", null, null, null, null, null, null);
			ramSubscriptionNames.add(pool.subscriptionName);
		}

		for(InstalledProduct installed : getRamBasedProducts()){
			Assert.assertEquals(installed.status.trim(), "Partially Subscribed", "Status of installed product '"+installed.productName+"' on a '"+ram+"' GB system after attaching 1 quantity of each of these subscriptions: "+ramSubscriptionNames);
		}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify healing of partially subscribed RAM products covered by stackable RAM-based subscriptions", 
			groups = { "HealingPartiallySubscribedRamBasedProducts","blockedByBug-907638"}, enabled = true)
	public void HealingPartiallySubscribedRamBasedProducts() throws JSONException,Exception {
		PartiallySubscribedRamBasedProducts();
		clienttasks.autoheal(null, true, null, null, null, null);
		clienttasks.run_rhsmcertd_worker(true);
		
		for(InstalledProduct installed : getRamBasedProducts()){
			Assert.assertEquals(installed.status.trim(), "Subscribed", "Status of installed ram product '"+installed.productName+"' after auto-healing a partially subscribed ram product.");
		}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Ram Subscription with disabled certv3 from candlepin ", 
			groups = { "DisableCertV3ForRamBasedSubscription"}, enabled = false)
	public void DisableCertV3ForRamBasedSubscription() throws JSONException,Exception {
		
		servertasks.updateConfFileParameter("candlepin.enable_cert_v3", "false");
		servertasks.restartTomcat();
		SubscriptionManagerCLITestScript.sleep( 1*60 * 1000);
		clienttasks.restart_rhsmcertd(null, null, null);
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, true, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		
		for(InstalledProduct installed : getRamBasedProducts()){
			Assert.assertEquals(installed.status.trim(), "Not Subscribed", "Status of installed product '"+installed.productName+"'.");
		}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Auto Heal for Ram subscription .", 
			groups = { "AutoHealRamBasedSubscription","blockedByBug-907638","blockedByBug-976867"}, enabled = true)
	public void AutoHealRamBasedSubscription() throws JSONException,Exception {
//DELETEME		
//		int healFrequency=2;
		factsMap.clear();
		factsMap.put("uname.machine", "x86_64");
		factsMap.put("cpu.core(s)_per_socket", "1");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, true, null, null,
				(String) null, null, null, null, true, false, null, null, null);
		
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.autoheal(null, true, null, null, null, null);
//DELETEME
//		clienttasks.restart_rhsmcertd(null, healFrequency, false, null);
//		SubscriptionManagerCLITestScript.sleep( healFrequency* 60 * 1000+ 10*1000);
		clienttasks.run_rhsmcertd_worker(true);
		
		for(InstalledProduct installed : getRamBasedProducts()){
			Assert.assertEquals(installed.status.trim(), "Subscribed", "Status of installed product '"+installed.productName+"'.");
		}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Auto-attach for Ram based subscription", 
			groups = { "AutoSubscribeRamBasedProducts"}, enabled = true)
	public void AutoSubscribeRamBasedProducts() throws JSONException,Exception {
		factsMap.clear();
		factsMap.put("uname.machine", "x86_64");
		factsMap.put("cpu.core(s)_per_socket", "1");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, false, null, null, null);
		
		clienttasks.subscribe_(true, null,(String)null, null, null, null, null, null, null, null, null, null);
		
		for(InstalledProduct installed : getRamBasedProducts()){
			Assert.assertEquals(installed.status.trim(), "Subscribed", "Status of installed product '"+installed.productName+"'.");
		}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Partial subscription of Ram subscription. ", 
			groups = { "PartialSubscriptionOfRamBasedSubscription"}, enabled = true)
	public void PartialSubscriptionOfRamBasedSubscription() throws JSONException,Exception {
// DELETEME
//		clienttasks.register_(sm_clientUsername, sm_clientPassword,
//				sm_clientOrg, null, null, null, null, null, null, null,
//				(String) null, null, null, null, true, null, null, null, null);
//		
//		for(SubscriptionPool pool :getRamBasedSubscriptions()){
//			clienttasks.subscribe_(null, null, pool.poolId, null, null, null, null, null, null, null, null);
//			
//		}clienttasks.subscribe_(true, null,(String)null, null, null, null, null, null, null, null, null);
//		for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
//			if(installed.productId.contains("ram")){
//
//				Assert.assertEquals(installed.status.trim(), "Subscribed");
//		}
//			factsMap.put("memory.memtotal", String.valueOf(value*10));
//			clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
//		}
//		
//		clienttasks.facts_(true, null, null, null, null);
//		clienttasks.subscribe_(true, null,(String)null, null, null, null, null, null, null, null, null);
//		for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
//			if(installed.productId.contains("ram")){
//
//				Assert.assertEquals(installed.status.trim(), "Partially Subscribed");
//			}}
		
		AutoSubscribeRamBasedProducts();
		
		int ram = 100; //GB
		factsMap.put("memory.memtotal", String.valueOf(GBToKBConverter(ram)));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null);
		
		for(InstalledProduct installed : getRamBasedProducts()){
			Assert.assertEquals(installed.status.trim(), "Partially Subscribed", "Status of installed ram product '"+installed.productName+"' on a '"+ram+"' GB system.");
		}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify Ram info in product and entitlement certificate", 
			groups = { "RamBasedSubscriptionInfoInEntitlementCert"}, enabled = true)
	public void RamBasedSubscriptionInfoInEntitlementCert() throws JSONException,Exception {
		factsMap.clear();
		factsMap.put("uname.machine", "x86_64");
		factsMap.put("cpu.core(s)_per_socket", "1");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, false, null, null,
				(String) null, null, null, null, true, null, null, null, null);
		
		for(SubscriptionPool pool :getRamBasedSubscriptions()){
			clienttasks.subscribe_(null, null, pool.poolId, null, null, null, null, null, null, null, null, null);
		}
		
//DELETEME
//		client.runCommandAndWaitWithoutLogging("find "+clienttasks.entitlementCertDir+" -regex \"/.+/[0-9]+.pem\" -exec rct cat-cert {} \\;");
//		String certificates = client.getStdout();
//		List<EntitlementCert> ramInfo =parseRamInfo(certificates);
//		Assert.assertNotNull(ramInfo.size());
		List<EntitlementCert> ramEntitlements = clienttasks.getCurrentEntitlementCerts();
		for (EntitlementCert ramEntitlement : ramEntitlements) {
			Assert.assertTrue(!ramEntitlement.orderNamespace.ramLimit.isEmpty(), "A ram-based entitlement cert contains a non-empty Ram Limit (actual='"+ramEntitlement.orderNamespace.ramLimit+"').");
		}
	}
		
		
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(description = "verify subscription of Ram based subscription", 
			groups = { "SubscribeToRamBasedSubscription","blockedByBug-907315"}, enabled = true)
	public void SubscribeToRamBasedSubscription() throws JSONException,Exception {
//DELETEME
//		int expected=1;
//		clienttasks.register_(sm_clientUsername, sm_clientPassword,
//				sm_clientOrg, null, null, null, null, null, null, null,
//				(String) null, null, null, null, true, null, null, null, null);
//		factsMap.put("memory.memtotal", String.valueOf(value*1));
//		clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
//		int ramvalue=KBToGBConverter(Integer.parseInt(clienttasks.getFactValue("memory.memtotal")));
//		for(SubscriptionPool pool :getRamBasedSubscriptions()){
//			if(pool.subscriptionName.contains("8GB")){
//			clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null);
//			}}
//		for(InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()){
//			if(installed.productId.contains("ram")){
//			if(ramvalue<=4){
//			Assert.assertEquals(installed.status.trim(), "Partially Subscribed");
//			factsMap.put("memory.memtotal", String.valueOf(value*5));
//			clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
//			clienttasks.facts(null, true, null, null, null);
//			ramvalue=KBToGBConverter(Integer.parseInt(clienttasks.getFactValue("memory.memtotal")));
//		}else if(ramvalue>4 && ramvalue<=8){
//			expected=2;
//			Assert.assertEquals(installed.status.trim(), "Partially Subscribed");
//			factsMap.put("memory.memtotal", String.valueOf(value*9));
//			clienttasks.createFactsFileWithOverridingValues("/custom.facts", factsMap);
//			clienttasks.facts(null, true, null, null, null);
//			ramvalue=KBToGBConverter(Integer.parseInt(clienttasks.getFactValue("memory.memtotal")));
//		}
//		}}
		
		factsMap.clear();
		factsMap.put("uname.machine", "x86_64");
		factsMap.put("cpu.core(s)_per_socket", "1");
		factsMap.put("memory.memtotal", String.valueOf(KBToGBConverter(1)));
		factsMap.put("cpu.cpu_socket(s)", "1");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, false, null, null, null);

		int ramvalue=KBToGBConverter(Integer.parseInt(clienttasks.getFactValue("memory.memtotal")));
		
		for(SubscriptionPool pool :getRamBasedSubscriptions()){
			if(pool.subscriptionName.contains("8GB")){
				clienttasks.subscribe(null, null, pool.poolId, null, null, "1", null, null, null, null, null, null);
				for (String productName : pool.provides) {
					for (InstalledProduct installed : InstalledProduct.findAllInstancesWithMatchingFieldFromList("productName", productName, clienttasks.getCurrentlyInstalledProducts())) {
						if(ramvalue<=4){
							Assert.assertEquals(installed.status.trim(), "Subscribed", "Status of installed product '"+installed.productName+"'.");
							factsMap.put("memory.memtotal", String.valueOf(GBToKBConverter(5)));
							clienttasks.createFactsFileWithOverridingValues(factsMap);
							clienttasks.facts(null, true, null, null, null);
							ramvalue=KBToGBConverter(Integer.parseInt(clienttasks.getFactValue("memory.memtotal")));
						}else if(ramvalue>4 && ramvalue<=8){
							Assert.assertEquals(installed.status.trim(), "Subscribed", "Status of installed product '"+installed.productName+"'.");
							factsMap.put("memory.memtotal", String.valueOf(GBToKBConverter(9)));
							clienttasks.createFactsFileWithOverridingValues(factsMap);
							clienttasks.facts(null, true, null, null, null);
							ramvalue=KBToGBConverter(Integer.parseInt(clienttasks.getFactValue("memory.memtotal")));
						} else {
							Assert.assertEquals(installed.status.trim(), "Partially Subscribed", "Status of installed product '"+installed.productName+"'.");	
						}
					}
				}
			}
		}
	}
	
	static public int KBToGBConverter(int memory) {
		int value=(int) 1.049e+6;	// KB per GB
		int result=(memory/value);
		return result;

	}
	static public int GBToKBConverter(int gb) {
		int value=(int) 1.049e+6;	// KB per GB
		int result=(gb*value);
		return result;
	}
	
	 public List<SubscriptionPool> getRamBasedSubscriptions() {
		 List<SubscriptionPool> RAMBasedPools= new ArrayList<SubscriptionPool>();
		 for(SubscriptionPool pool:clienttasks.getCurrentlyAvailableSubscriptionPools()){
			 if(pool.subscriptionName.toLowerCase().contains("ram") || pool.productId.toLowerCase().contains("ram")){
				 RAMBasedPools.add(pool) ;
			 }
		 }
		 if (RAMBasedPools.isEmpty()) throw new SkipException("Could not find any RAM-based subscription pools/SKUs.");
		 
		return RAMBasedPools;
	}
	 
	public List<InstalledProduct> getRamBasedProducts() {
		String ramProductName = "RAM";	// "RAM Limiting Product"
		List<InstalledProduct> RAMBasedProducts= new ArrayList<InstalledProduct>();
		for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()){
			if(installedProduct.productName.contains(ramProductName)){
				RAMBasedProducts.add(installedProduct) ;
			}
		}
		if (RAMBasedProducts.isEmpty()) throw new SkipException("Could not find any installed products containing name '"+ramProductName+"'.");
		 
		return RAMBasedProducts;
	}
	
	// include this AfterClass method as insurance to make sure the next test class
	// in a suite is not affected by the facts used in this test class
	@AfterClass(groups = "setup")
	public void deleteFactsFileWithOverridingValuesAfterClass() throws Exception {
		if (clienttasks!=null) clienttasks.deleteFactsFileWithOverridingValues();
	}
}
