package rhsm.cli.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author skallesh
 *
 *
 */

@Test(groups={"RAMTests"})
public class RAMTests extends SubscriptionManagerCLITestScript {
	Map<String, String> factsMap = new HashMap<String, String>();
	protected String productId = "RamTest-product";
	protected List<String> providedProduct = new ArrayList<String>();
	// Test methods ***********************************************************************
	
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36684", "RHEL7-51529"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "verify status of partially covered Ram based products", 
			groups = {"Tier2Tests","PartiallySubscribedRamBasedProducts"},
			enabled = true)
	public void testPartiallySubscribedRamBasedProducts() throws JSONException,Exception {
		Integer ram = 20; //GB
		factsMap.clear();
		factsMap.put("uname.machine", "x86_64");
		factsMap.put("memory.memtotal", String.valueOf(GBToKBConverter(ram)));
		factsMap.put("cpu.cpu_socket(s)", "1");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null);
		
		List<String> ramSubscriptionNames = new ArrayList<String>();
		for(SubscriptionPool pool :getRamBasedSubscriptions()){
			clienttasks.subscribe(null, null, pool.poolId, null, null, "1", null, null, null, null, null, null, null);
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
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36682", "RHEL7-51527"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "verify healing of partially subscribed RAM products covered by stackable RAM-based subscriptions", 
			groups = {"Tier2Tests","HealingPartiallySubscribedRamBasedProducts","blockedByBug-907638"},
			enabled = true)
	public void testHealingPartiallySubscribedRamBasedProducts() throws JSONException,Exception {
		testPartiallySubscribedRamBasedProducts();
		clienttasks.autoheal(null, true, null, null, null, null, null);
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
	@Test(	description = "verify Ram Subscription with disabled certv3 from candlepin ", 
			groups = {"Tier2Tests","DisableCertV3ForRamBasedSubscription"},
			enabled = false)
	public void testDisableCertV3ForRamBasedSubscription() throws JSONException,Exception {
		
		servertasks.updateConfFileParameter("candlepin.enable_cert_v3", "false");
		servertasks.restartTomcat();
		SubscriptionManagerCLITestScript.sleep( 1*60 * 1000);
		clienttasks.restart_rhsmcertd(null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);
		for(InstalledProduct installed : getRamBasedProducts()) {
			Assert.assertEquals(installed.status.trim(), "Not Subscribed", "Status of installed product '"+installed.productName+"'.");
		}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36680", "RHEL7-51525"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "verify Auto Heal for Ram subscription .", 
			groups = {"Tier2Tests","AutoHealRamBasedSubscription","blockedByBug-907638","blockedByBug-976867"},
			enabled = true)
	public void testAutoHealRamBasedSubscription() throws JSONException,Exception {
		factsMap.clear();
		factsMap.put("uname.machine", "x86_64");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, false, null, null, null, null);
		getRamBasedSubscriptions();		
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.autoheal(null, true, null, null, null, null, null);
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
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36681", "RHEL7-51526"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "verify Auto-attach for Ram based subscription", 
			groups = {"Tier2Tests","AutoSubscribeRamBasedProducts"},
			enabled = true)
	public void testAutoSubscribeRamBasedProducts() throws JSONException,Exception {
		factsMap.clear();
		factsMap.put("uname.machine", "x86_64");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.register(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, false, null, null, null, null);
		getRamBasedSubscriptions();
		clienttasks.subscribe(true, null,(String)null, null, null, null, null, null, null, null, null, null, null);
		
		for(InstalledProduct installed : getRamBasedProducts()){
			Assert.assertEquals(installed.status.trim(), "Subscribed", "Status of installed product '"+installed.productName+"'.");
		}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36683", "RHEL7-51528"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "verify Partial subscription of Ram subscription. ", 
			groups = {"Tier2Tests","PartialSubscriptionOfRamBasedSubscription"},
			enabled = true)
	public void testPartialSubscriptionOfRamBasedSubscription() throws JSONException,Exception {
		testAutoSubscribeRamBasedProducts();
		int ram = 100; //GB
		factsMap.put("memory.memtotal", String.valueOf(GBToKBConverter(ram)));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		
		for(InstalledProduct installed : getRamBasedProducts()){
			Assert.assertEquals(installed.status.trim(), "Partially Subscribed", "Status of installed ram product '"+installed.productName+"' on a '"+ram+"' GB system.");
		}
	}
	
	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36685", "RHEL7-51530"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "verify Ram info in product and entitlement certificate", 
			groups = {"Tier2Tests","RamBasedSubscriptionInfoInEntitlementCert"},
			enabled = true)
	public void testRamBasedSubscriptionInfoInEntitlementCert() throws JSONException,Exception {
		factsMap.clear();
		factsMap.put("uname.machine", "x86_64");
		factsMap.put("cpu.core(s)_per_socket", "1");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, false, null, null,
				(String) null, null, null, null, true, null, null, null, null, null);
		
		for(SubscriptionPool pool :getRamBasedSubscriptions()){
			clienttasks.subscribe_(null, null, pool.poolId, null, null, null, null, null, null, null, null, null, null);
		}
		
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
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36686", "RHEL7-51531"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "verify subscription of Ram based subscription", 
			groups = {"Tier2Tests","SubscribeToRamBasedSubscription","blockedByBug-907315"},
			enabled = true)
	public void testSubscribeToRamBasedSubscription() throws JSONException,Exception {

		factsMap.clear();
		factsMap.put("uname.machine", "x86_64");
		factsMap.put("cpu.core(s)_per_socket", "1");
		factsMap.put("memory.memtotal", String.valueOf(KBToGBConverter(1)));
		factsMap.put("cpu.cpu_socket(s)", "1");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.register_(sm_clientUsername, sm_clientPassword,
				sm_clientOrg, null, null, null, null, null, null, null,
				(String) null, null, null, null, true, false, null, null, null, null);

		int ramvalue=KBToGBConverter(Integer.parseInt(clienttasks.getFactValue("memory.memtotal")));
		
		for(SubscriptionPool pool :getRamBasedSubscriptions()){
			if(pool.subscriptionName.contains("8GB")){
				clienttasks.subscribe(null, null, pool.poolId, null, null, "1", null, null, null, null, null, null, null);
				for (String productName : pool.provides) {
					for (InstalledProduct installed : InstalledProduct.findAllInstancesWithMatchingFieldFromList(
						"productName", productName,clienttasks.getCurrentlyInstalledProducts())) {
					    
						if(ramvalue<=4){
							Assert.assertEquals(installed.status.trim(), "Subscribed", 
								"Status of installed product '"+installed.productName+"'.");
							factsMap.put("memory.memtotal", String.valueOf(GBToKBConverter(5)));
							clienttasks.createFactsFileWithOverridingValues(factsMap);
							clienttasks.facts(null, true, null, null, null, null);
							ramvalue=KBToGBConverter(Integer.parseInt(clienttasks.getFactValue("memory.memtotal")));
							
						}else if(ramvalue>4 && ramvalue<=8){
							Assert.assertEquals(installed.status.trim(), "Subscribed", 
								"Status of installed product '"+installed.productName+"'.");
							factsMap.put("memory.memtotal", String.valueOf(GBToKBConverter(9)));
							clienttasks.createFactsFileWithOverridingValues(factsMap);
							clienttasks.facts(null, true, null, null, null, null);
							ramvalue=KBToGBConverter(Integer.parseInt(clienttasks.getFactValue("memory.memtotal")));
							
						} else {
							Assert.assertEquals(installed.status.trim(), "Partially Subscribed", 
								"Status of installed product '"+installed.productName+"'.");	
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * @return List<SubscriptionPool>
	 * @throws JSONException
	 * @throws Exception
	 */
	public List<SubscriptionPool> getRamBasedSubscriptions() throws JSONException, Exception {
		 List<SubscriptionPool> RAMBasedPools= new ArrayList<SubscriptionPool>();
		 for(SubscriptionPool pool:clienttasks.getCurrentlyAvailableSubscriptionPools()){
		     List<String> provides=pool.provides;
		     for(String providedproducts:provides){
			 if(providedproducts.contains("RAM")){
			     RAMBasedPools.add(pool) ;
			 }
		     }
		 }
		 if (RAMBasedPools.isEmpty()) {
			createTestPool(-60 * 24, 60*24*3);
			for(SubscriptionPool pool:clienttasks.getCurrentlyAvailableSubscriptionPools()){
			     for(String providedproducts:pool.provides){
				 if(providedproducts.contains("RAM")){
				     RAMBasedPools.add(pool) ;
				 }
			     }
			}
		 }
		 
		return RAMBasedPools;
	}
	
	 
	 /**
		 * @param startingMinutesFromNow
		 * @param endingMinutesFromNow
		 * @return poolId to the newly available SubscriptionPool
		 * @throws JSONException
		 * @throws Exception
		 */
		protected String createTestPool(int startingMinutesFromNow, int endingMinutesFromNow)
				throws JSONException, Exception {
		    	String name = "RAMTestSubscription";
		    	Map
		    	<String, String> attributes = new HashMap<String, String>();
			attributes.clear();
			attributes.put("version", "1.0");
			attributes.put("variant", "ALL");
			attributes.put("arch", "ALL");
			attributes.put("type", "MKT");
			attributes.put("multi-entitlement", "yes");
			attributes.put("ram", "8");
			attributes.put("stacking_id", "ram-stackable");
			System.out.println(attributes.isEmpty());
			System.out.println(productId + "  "+ name);

			providedProduct.add("801");
			String resourcePath = "/products/" + productId;
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
				resourcePath = "/owners/" + sm_clientOrg + resourcePath;
			CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, productId);
			CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);
			CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				sm_clientOrg, name + " BITS", productId, 1, attributes, null);
			String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, clienttasks.getCurrentConsumerId());
			return CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, ownerKey, 10, startingMinutesFromNow, endingMinutesFromNow,
				getRandInt(), getRandInt(), productId, providedProduct, null).getString("id");
		}

	@AfterClass(groups = { "setup" })
	protected void DeleteTestPool() throws Exception {
		if (CandlepinType.hosted.equals(sm_serverType))
				return; // make sure we don't run this against stage/prod
			// environment
		if (sm_clientOrg == null)
		    return; // must have an owner when calling candlepin APIs to delete
			// resources
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
					sm_serverUrl, sm_clientOrg, productId);
		String resourcePath = "/products/" + productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
				resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,resourcePath);
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
	
}
