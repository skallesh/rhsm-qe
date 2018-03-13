package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.CandlepinType;
import rhsm.base.ConsumerType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ConsumerCert;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.ProductNamespace;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author ssalevan
 * @author jsefler
 *
 */
@Test(groups={"SubscribeTests"})
public class SubscribeTests extends SubscriptionManagerCLITestScript{

	// Test methods ***********************************************************************


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19980", "RHEL7-33092"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager-cli: subscribe consumer to subscription pool product id",	//; and assert the subscription pool is not available when it does not match the system hardware.",
			dataProvider="getAllSystemSubscriptionPoolProductData",
			groups={"Tier1Tests","blockedByBug-660713","blockedByBug-806986","blockedByBug-878986","blockedByBug-962520","blockedByBug-1008647","blockedByBug-1009600","blockedByBug-996993"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testSubscribeToSubscriptionPoolProductId(String productId, JSONArray bundledProductDataAsJSONArray) throws Exception {
///*debugTesting*/ if (!productId.equals("awesomeos-ul-quantity-virt")) throw new SkipException("debugTesting - Automator should comment out this line."); 		
///*debugTesting*/ if (!productId.equals("awesomeos-onesocketib")) throw new SkipException("debugTesting - Automator should comment out this line."); 		
///*debugTesting*/ if (!productId.equals("awesomeos-virt-4")) throw new SkipException("debugTesting - Automator should comment out this line."); 		
///*debugTesting*/ if (!productId.equals("awesomeos-virt-4")&&!productId.equals("awesomeos-ul-quantity-virt")&&!productId.equals("awesomeos-onesocketib")&&!productId.equals("awesomeos-instancebased")) throw new SkipException("debugTesting - Automator should comment out this line."); 		
///*debugTesting*/ if (!productId.equals("2cores-2ram-multiattr")) throw new SkipException("debugTesting - Automator should comment out this line."); 		
///*debugTesting*/ if (!productId.equals("RH0380468")) throw new SkipException("debugTesting - Automator should comment out this line."); 		
///*debugTesting*/ if (!productId.equals("RH00284")) throw new SkipException("debugTesting - Automator should comment out this line."); 		
///*debugTesting*/ if (!productId.equals("awesomeos-super-hypervisor")) throw new SkipException("debugTesting - Automator should comment out this line.");
///*debugTesting*/ if (!productId.equals("MCT3115")) throw new SkipException("debugTesting - Automator should comment out this line.");
		// is this system a virtual guest system or a physical system
		boolean systemIsGuest = Boolean.valueOf(clienttasks.getFactValue("virt.is_guest"));
		
		// begin test with a fresh register
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		// assert the subscription pool with the matching productId is available
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", productId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());	// clienttasks.getCurrentlyAvailableSubscriptionPools() is tested at the conclusion of this test
///*debugTesting*/pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", "8a9087e34c6b0d69014c6b0ede641f42", clienttasks.getCurrentlyAllAvailableSubscriptionPools());	// awesomeos-onesocketib; Instance Based (Temporary)
///*debugTesting*/ if (!isPoolRestrictedToUnmappedVirtualSystems) throw new SkipException("debugTesting - Automator should comment out this line.");
		// special case...
		if (pool==null) {	// when pool is null, another likely cause is that all of the available subscriptions from the pools are being consumed, let's check...
			for (String poolId: CandlepinTasks.getPoolIdsForProductId(sm_clientUsername, sm_clientPassword, sm_serverUrl, clienttasks.getCurrentlyRegisteredOwnerKey(), productId)) {
				int quantity = (Integer) CandlepinTasks.getPoolValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, poolId, "quantity");
				int consumed = (Integer) CandlepinTasks.getPoolValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, poolId, "consumed");
				if (consumed>=quantity) {
					throw new SkipException("The total quantity '"+quantity+"' of subscriptions from poolId '"+poolId+"' for product '"+productId+"' are being consumed; hence this product subscription is appropriately not available to subscribe.");
				}
			}	
		}
		Assert.assertNotNull(pool, "Expected SubscriptionPool with ProductId '"+productId+"' is available for subscribing.");
		
		// assemble a list of expected bundled product names
		List<String> bundledProductNames = new ArrayList<String>();
		for (int j=0; j<bundledProductDataAsJSONArray.length(); j++) {
			JSONObject bundledProductAsJSONObject = (JSONObject) bundledProductDataAsJSONArray.get(j);
			String bundledProductId = bundledProductAsJSONObject.getString("productId");
			String bundledProductName = bundledProductAsJSONObject.getString("productName");
			bundledProductNames.add(bundledProductName);
		}
		
		// TEMPORARY WORKAROUND
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1016300"; // Bug 1016300 - the "Provides:" field in subscription-manager list --available should exclude "MKT" products.
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("While bug '"+bugId+"' is open, skip assertion that the actual list of SubscriptionPool provided product names "+pool.provides+" matches the expected list of bundledProductDataNames "+bundledProductNames+".");
		} else
		// END OF WORKAROUND
		/*if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.3-1"))*/ if (pool.provides!=null) {
			
			// TEMPORARY WORKAROUND
			invokeWorkaroundWhileBugIsOpen = true;
			bugId="1394401"; // Bug 1394401 - The list of provided products for Temporary Subscriptions is empty
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen && !bundledProductNames.isEmpty() && pool.provides.isEmpty() && pool.subscriptionType.contains("Temporary")) {
				log.warning("While bug '"+bugId+"' is open, skip assertion that the actual list of SubscriptionPool provided product names "+pool.provides+" matches the expected list of bundledProductDataNames "+bundledProductNames+".");
			} else
			// END OF WORKAROUND
			
			// assert that the pool's list of Provides matches the list of bundled product names after implementation of Bug 996993 - [RFE] Search for or list matching providedProducts; subscription-manager commit b8738a74c1109975e387fc51105c8ff58eaa8f01
			Assert.assertTrue(bundledProductNames.containsAll(pool.provides) && pool.provides.containsAll(bundledProductNames), "The actual list of SubscriptionPool provided product names "+pool.provides+" matches the expected list of bundledProductDataNames "+bundledProductNames+".  (If this fails due to provided Product Names changes by Release Engineering, refresh pools for account '"+sm_clientUsername+"' is needed.)");
		}
		
		List<ProductCert> currentlyInstalledProductCerts = clienttasks.getCurrentProductCerts();
		List<InstalledProduct> currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
		
		// assert the installed status of the bundled products
		for (int j=0; j<bundledProductDataAsJSONArray.length(); j++) {
			JSONObject bundledProductAsJSONObject = (JSONObject) bundledProductDataAsJSONArray.get(j);
			String bundledProductId = bundledProductAsJSONObject.getString("productId");
			
			// assert the status of the installed products listed
			for (ProductCert productCert : ProductCert.findAllInstancesWithMatchingFieldFromList("productId", bundledProductId, currentlyInstalledProductCerts)) {
				InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", productCert.productName, currentlyInstalledProducts);
				Assert.assertNotNull(installedProduct, "The status of installed product cert with ProductName '"+productCert.productName+"' is reported in the list of installed products.");
				Assert.assertEquals(installedProduct.status, "Not Subscribed", "Before subscribing to pool for ProductId '"+productId+"', the status of Installed Product '"+productCert.productName+"' is Not Subscribed.");
			}
		}
		
		// when the Suggested quantity is 0, let's specify a quantity to avoid Stdout: Quantity '1' is not a multiple of instance multiplier '2'
		// adjust quantity for instance_multiplier pools
		String instance_multiplier = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "instance_multiplier");
		String quantity = null;
		/*if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.3-1"))*/ if (pool.suggested!=null) {
			if (pool.suggested<1 && instance_multiplier!=null) {
				quantity = instance_multiplier;
			}
		}
		
		boolean isPoolRestrictedToUnmappedVirtualSystems = CandlepinTasks.isPoolRestrictedToUnmappedVirtualSystems(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId);
		
		// subscribe to the pool
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool,quantity,/*sm_serverAdminUsername*/sm_clientUsername,/*sm_serverAdminPassword*/sm_clientPassword,sm_serverUrl);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		
		currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
		List<ProductSubscription> currentlyConsumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		
		// after subscribing to a pool, assert that its corresponding productSubscription is found among the currently consumed productSubscriptions
		ProductSubscription consumedProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productId", pool.productId, currentlyConsumedProductSubscriptions);
		Assert.assertNotNull(consumedProductSubscription, "The consumed ProductSubscription corresponding to the subscribed SubscriptionPool productId '"+pool.productId+"' was found among the list of consumed ProductSubscriptions.");
		
		// assert that the quantityUsed matches the quantitySuggested after implementation of Bug 1008647 - [RFE] bind requests that do not specify a quantity should automatically use the quantity needed to achieve compliance
		if (quantity!=null) {
			Assert.assertEquals(consumedProductSubscription.quantityUsed, Integer.valueOf(quantity), "When the attachment quantity '"+quantity+"' is specified, the quantity used from the consumed product subscription should match.");		
		} else {
			/*if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.3-1"))*/ if (pool.suggested!=null) {
				if (pool.suggested > 1) {
					Assert.assertEquals(consumedProductSubscription.quantityUsed, pool.suggested, "When the suggested consumption quantity '"+pool.suggested+"' from the available pool is greater than one, the quantity used from the consumed product subscription should match.");
				} else {
					Assert.assertEquals(consumedProductSubscription.quantityUsed, Integer.valueOf(1), "When the suggested consumption quantity '"+pool.suggested+"' from the available pool is NOT greater than one, the quantity used from the consumed product subscription should be one.");
				}
			}
		}
		
		// assert that the System Type matches between the available pool and the consumed product subscription after implementation of Bug 1009600 - Show System Type in list --consumed; Show System Type in attach confirmation gui dialog.
		/*if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.3-1"))*/ if (consumedProductSubscription.machineType!=null) {
			Assert.assertEquals(consumedProductSubscription.machineType, pool.machineType, "After subscribing from a pool with a machine type '"+pool.machineType+"', the consumed product subscription's machine type should match.");
		}
		
		// TEMPORARY WORKAROUND
		/*boolean*/ invokeWorkaroundWhileBugIsOpen = true;
		/*String*/ bugId="1204311"; // Bug 1204311 - Refreshing pools causes unexpected temporary pools for unmapped guests to become available 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen && isPoolRestrictedToUnmappedVirtualSystems) {
			log.warning("While bug '"+bugId+"' is open and we have subscrbed to a Temporary pool, skip assertion that the consumed productSubscription provides all of the expected bundled product names "+bundledProductNames+" after subscribing to pool: "+pool);
		} else
		// END OF WORKAROUND
		{
			// TEMPORARY WORKAROUND
			invokeWorkaroundWhileBugIsOpen = true;
			bugId="1394401"; // Bug 1394401 - The list of provided products for Temporary Subscriptions is empty
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen && isPoolRestrictedToUnmappedVirtualSystems) {
				log.warning("While bug '"+bugId+"' is open and we have subscrbed to a Temporary pool, skip assertion that the consumed productSubscription provides all of the expected bundled product names "+bundledProductNames+" after subscribing to pool: "+pool);
			} else
			// END OF WORKAROUND
				
			// assert that the consumed product subscription provides all the expected bundled products.
			Assert.assertTrue(consumedProductSubscription.provides.containsAll(bundledProductNames)&&bundledProductNames.containsAll(consumedProductSubscription.provides),"The consumed productSubscription provides all of the expected bundled product names "+bundledProductNames+" after subscribing to pool: "+pool);
		}
		
		// assert the dates of the consumed product subscription...
		if (isPoolRestrictedToUnmappedVirtualSystems) {
			// ... assert endDate is 24 hours after the date of registration
			ConsumerCert cert = clienttasks.getCurrentConsumerCert();
			Calendar consumerCertStartDate = cert.validityNotBefore;
			int hours = 24;
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.1.0-1"/*FIXME CHANGE TO "2.1.1-1" ONCE TAG EXISTS*/)) {	// commit 0704a73dc0d3bf753351e87ca0b65d85a71acfbe 1450079: virt-who temporary subscription should be 7 days
				hours = 7/*days*/ * 24/*hours per day*/;
				log.info("Due to Candlepin RFE Bug 1450079, the vailidity period for temporary subscription pools has increased from one day to one week.");
			}
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.30-1")) {	// commit 9302c8f57f37dd5ec3c4020770ac1675a87d99ba 1419576: Pre-date certs to ease clock skew issues
				hours+=1;
				log.info("Due to Candlepin RFE Bug 1419576, we need to increment the expected expires_after by one hour to account for pre-dating the consumer identity's validityNotBefore date by one hour.");
			}
			consumerCertStartDate.add(Calendar.HOUR, hours);
			Assert.assertEquals(ProductSubscription.formatDateString(consumedProductSubscription.endDate),ProductSubscription.formatDateString(consumerCertStartDate),
				"Consumed productSubscription (from a unmapped_guests_only pool '"+pool.poolId+"') expires '"+hours+"' hours after the time of consumer registration ("+ConsumerCert.formatDateString(clienttasks.getCurrentConsumerCert().validityNotBefore)+").");
			//TODO Assert the start date after bug 1199670 is resolved
		} else {
			// ... assert endDate matches the originating subscription pool
			Assert.assertEquals(ProductSubscription.formatDateString(consumedProductSubscription.endDate),ProductSubscription.formatDateString(pool.endDate),
				"Consumed productSubscription expires on the same DAY as the originating subscription pool.");
			//FIXME	Assert.assertTrue(productSubscription.startDate.before(entitlementCert.validityNotBefore), "Consumed ProductSubscription Began before the validityNotBefore date of the new entitlement: "+entitlementCert);
		}
		
		// assert the expected products are consumed
		for (int j=0; j<bundledProductDataAsJSONArray.length(); j++) {
			JSONObject bundledProductAsJSONObject = (JSONObject) bundledProductDataAsJSONArray.get(j);
			String bundledProductId = bundledProductAsJSONObject.getString("productId");
			String bundledProductName = bundledProductAsJSONObject.getString("productName");
			bundledProductNames.add(bundledProductName);
			
			// find the corresponding productNamespace from the entitlementCert
			ProductNamespace productNamespace = null;
			for (ProductNamespace pn : entitlementCert.productNamespaces) {
				if (pn.id.equals(bundledProductId)) productNamespace = pn;
			}
			
			// TEMPORARY WORKAROUND
			invokeWorkaroundWhileBugIsOpen = true;
			bugId="1394401"; // Bug 1394401 - The list of provided products for Temporary Subscriptions is empty
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen && !bundledProductNames.isEmpty() && pool.provides.isEmpty() && pool.subscriptionType.contains("Temporary")) {
				log.warning("While bug '"+bugId+"' is open, skip assertion of the consumed entitlement provided products amongst the list of install products.");
			} else
			// END OF WORKAROUND
			
			// assert the installed status of the corresponding product
			if (entitlementCert.productNamespaces.isEmpty()) {
				log.warning("This product '"+productId+"' ("+bundledProductName+") does not appear to grant entitlement to any client side content.  This must be a server side management add-on product. Asserting as such...");

				Assert.assertEquals(entitlementCert.contentNamespaces.size(),0,
						"When there are no productNamespaces in the entitlementCert, there should not be any contentNamespaces.");

				// when there is no corresponding product, then there better not be an installed product status by the same product name
				Assert.assertNull(InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", bundledProductName, currentlyInstalledProducts),
						"Should not find any installed product status matching a server side management add-on productName: "+ bundledProductName);

				// when there is no corresponding product, then there better not be an installed product cert by the same product name
				Assert.assertNull(ProductCert.findFirstInstanceWithMatchingFieldFromList("productName", bundledProductName, currentlyInstalledProductCerts),
						"Should not find any installed product certs matching a server side management add-on productName: "+ bundledProductName);

			} else {
				Assert.assertNotNull(productNamespace, "The new entitlement cert's product namespace corresponding to this expected ProductSubscription with ProductName '"+bundledProductName+"' was found.");
				
				// assert the status of the installed products listed
				List <ProductCert> productCerts = ProductCert.findAllInstancesWithMatchingFieldFromList("productId", productNamespace.id, currentlyInstalledProductCerts);  // should be a list of one or empty
				for (ProductCert productCert : productCerts) {
					List <InstalledProduct> installedProducts = InstalledProduct.findAllInstancesWithMatchingFieldFromList("productName", productCert.productName, currentlyInstalledProducts);
					Assert.assertEquals(installedProducts.size(),1, "The status of installed product '"+productCert.productName+"' should only be reported once in the list of installed products.");
					InstalledProduct installedProduct = installedProducts.get(0);
					
					// TEMPORARY WORKAROUND FOR BUG
					if (installedProduct.arch.contains(",")) {
						/*boolean*/ invokeWorkaroundWhileBugIsOpen = true;
						/*String*/ bugId="951633"; // Bug 951633 - installed product with comma separated arch attribute fails to go green
						try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
						if (invokeWorkaroundWhileBugIsOpen) {
							throw new SkipException("Verification for status of Installed Product name='"+installedProduct.productName+"' with arch='"+installedProduct.arch+"' is blocked by open bugzilla '"+bugId+"'.");
						}
					}
					// END OF WORKAROUND
					
					// decide what the status should be...  "Subscribed" or "Partially Subscribed" (SPECIAL CASE WHEN poolProductSocketsAttribute=0  or "null" SHOULD YIELD Subscribed)
					String poolProductSocketsAttribute = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "sockets");
					String poolProductVcpuAttribute = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "vcpu");	// introduced by 885785 [RFE] Subscription Manager should alert a user if subscription vcpu limits are lower than system vcpu allocation
					String poolProductArchAttribute = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "arch");
					List<String> poolProductArches = new ArrayList<String>();
					if (poolProductArchAttribute!=null) {
						poolProductArches.addAll(Arrays.asList(poolProductArchAttribute.trim().split(" *, *")));	// Note: the arch attribute can be a comma separated list of values
						if (poolProductArches.contains("x86")) {poolProductArches.addAll(Arrays.asList("i386","i486","i586","i686"));}  // Note: x86 is a general arch to cover all 32-bit intel microprocessors 
						if (poolProductArches.contains("ALL")) poolProductArches.add(clienttasks.arch);
					}
					
					// treat a non-numeric poolProductSocketsAttribute as if it was null
					// if the sockets attribute is not numeric (e.g. "null"),  then this subscription should be available to this client
					try {Integer.valueOf(poolProductSocketsAttribute);}
					catch (NumberFormatException e) {
						// do not mark productAttributesPassRulesCheck = false;
						log.warning("Ecountered a non-numeric value for product sockets attribute sockets on productId='"+productId+"' poolId '"+pool.poolId+"'. SIMPLY IGNORING THIS ATTRIBUTE.");
						poolProductSocketsAttribute = null;
					}
					
					// consider the socket/vcpu coverage and assert the installed product's status
					if (isPoolRestrictedToUnmappedVirtualSystems && false/*since 1200882 was CLOSED NOTABUG, never run the assertion in this if condition*/) {
						// TEMPORARY WORKAROUND
						/*boolean*/ invokeWorkaroundWhileBugIsOpen = true;
						/*String*/ bugId="1200882"; // Bug 1200882 - Wrong installed product status is displayed when a unmapped_guests_only pool is attached
						try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
						if (invokeWorkaroundWhileBugIsOpen) {
							log.warning("Skipping the assertion of the installed product status after subscribing to an unmapped_guests_only pool for ProductId '"+productId+"' while bug '"+bugId+"' is open.");
						} else
						// END OF WORKAROUND
						Assert.assertEquals(installedProduct.status, "Partially Subscribed", "After subscribing to an unmapped_guests_only pool for ProductId '"+productId+"', the status of Installed Product '"+bundledProductName+"' should be Partially Subscribed regardless of any hardware socket/vcpu coverage or other subscriptions attached.");	// for more info, see bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=1197897	// CLOSED NOTABUG
					} else if (pool.subscriptionType!=null && pool.subscriptionType.equals("Other")) {
						Assert.fail("Encountered a subscription pool of type '"+pool.subscriptionType+"'.  Do not know how to assert the installedProduct.status after subscribing to pool: "+pool);
				    } else if (pool.multiEntitlement==null && pool.subscriptionType!=null && pool.subscriptionType.isEmpty()) {
				    	log.warning("Encountered a pool with an empty value for subscriptionType (indicative of an older candlepin server): "+pool);
				    	log.warning("After subscribing to a pool for ProductId '"+productId+", skipping assertion of the installed product status for products provided by this pool: "+pool);
					} else if (pool.multiEntitlement!=null && !pool.multiEntitlement/*ADDED AFTER IMPLEMENTATION OF BUG 1008647*/ && !systemIsGuest/*ADDED AFTER IMPLEMENTATION OF BUG 885785*/ && poolProductSocketsAttribute!=null && Integer.valueOf(poolProductSocketsAttribute)<Integer.valueOf(clienttasks.sockets) && Integer.valueOf(poolProductSocketsAttribute)>0) {
						Assert.assertEquals(installedProduct.status, "Partially Subscribed", "After subscribing this physical system to a pool for ProductId '"+productId+"' (covers '"+poolProductSocketsAttribute+"' sockets), the status of Installed Product '"+bundledProductName+"' should be Partially Subscribed since a corresponding product cert was found in "+clienttasks.productCertDir+" and the machine's sockets value ("+clienttasks.sockets+") is greater than what a single subscription from a non-multi-entitlement pool covers.");
					} else if (pool.subscriptionType!=null && (pool.subscriptionType.equals("Standard") || pool.subscriptionType.equals("Stackable only with other subscriptions"))/*ADDED AFTER IMPLEMENTATION OF BUG 1008647 AND 1029968*/ && !systemIsGuest/*ADDED AFTER IMPLEMENTATION OF BUG 885785*/ && poolProductSocketsAttribute!=null && Integer.valueOf(poolProductSocketsAttribute)<Integer.valueOf(clienttasks.sockets) && Integer.valueOf(poolProductSocketsAttribute)>0) {
						Assert.assertEquals(installedProduct.status, "Partially Subscribed", "After subscribing this physical system to a pool for ProductId '"+productId+"' (covers '"+poolProductSocketsAttribute+"' sockets), the status of Installed Product '"+bundledProductName+"' should be Partially Subscribed since a corresponding product cert was found in "+clienttasks.productCertDir+" and the machine's sockets value ("+clienttasks.sockets+") is greater than what a single subscription from a non-multi-entitlement pool covers.");
					} else if (pool.multiEntitlement!=null && !pool.multiEntitlement/*ADDED AFTER IMPLEMENTATION OF BUG 1008647*/ && systemIsGuest/*ADDED AFTER IMPLEMENTATION OF BUG 885785*/ && poolProductVcpuAttribute!=null && Integer.valueOf(poolProductVcpuAttribute)<Integer.valueOf(clienttasks.vcpu) && Integer.valueOf(poolProductVcpuAttribute)>0) {
						Assert.assertEquals(installedProduct.status, "Partially Subscribed", "After subscribing this virtual system to a pool for ProductId '"+productId+"' (covers '"+poolProductVcpuAttribute+"' vcpu), the status of Installed Product '"+bundledProductName+"' should be Partially Subscribed since a corresponding product cert was found in "+clienttasks.productCertDir+" and the machine's vcpu value ("+clienttasks.vcpu+") is greater than what a single subscription from a non-multi-entitlement pool covers.");
					} else if (pool.subscriptionType!=null && (pool.subscriptionType.equals("Standard") || pool.subscriptionType.equals("Stackable only with other subscriptions"))/*ADDED AFTER IMPLEMENTATION OF BUG 1008647 AND 1029968*/ && systemIsGuest/*ADDED AFTER IMPLEMENTATION OF BUG 885785*/ && poolProductVcpuAttribute!=null && Integer.valueOf(poolProductVcpuAttribute)<Integer.valueOf(clienttasks.vcpu) && Integer.valueOf(poolProductVcpuAttribute)>0) {
						Assert.assertEquals(installedProduct.status, "Partially Subscribed", "After subscribing this virtual system to a pool for ProductId '"+productId+"' (covers '"+poolProductVcpuAttribute+"' vcpu), the status of Installed Product '"+bundledProductName+"' should be Partially Subscribed since a corresponding product cert was found in "+clienttasks.productCertDir+" and the machine's vcpu value ("+clienttasks.vcpu+") is greater than what a single subscription from a non-multi-entitlement pool covers.");
					} else if (!poolProductArches.contains(clienttasks.arch)) {
						log.warning("This case is indicative that a pool refresh may be needed for this owner.");
						Assert.assertEquals(installedProduct.status, "Partially Subscribed", "After subscribing this system with arch '"+clienttasks.arch+"' to a pool for ProductId '"+productId+"' (that supports '"+poolProductArchAttribute+"' arch), the status of Installed Product '"+bundledProductName+"' should be Partially Subscribed with a reason stating that the system's arch is not supported by this subscription.");
						// Example Status Details:    Supports architecture x86_64,ppc64,ia64,ppc,x86,s390,s390x but the system is ppc64le.
						String reason = String.format("Supports architecture %s but the system is %s.", poolProductArchAttribute, clienttasks.arch);
						Assert.assertTrue(installedProduct.statusDetails.contains(reason), "Installed Product '"+installedProduct.productName+"' Status Details includes '"+reason+"'.");
					} else {
						Assert.assertEquals(installedProduct.status, "Subscribed", "After subscribing to a pool for ProductId '"+productId+"', the status of Installed Product '"+bundledProductName+"' is Subscribed since a corresponding product cert was found in "+clienttasks.productCertDir+" and the system's sockets/vcpu needs were met.");
					}
					
					// TEMPORARY WORKAROUND
					/*boolean*/ invokeWorkaroundWhileBugIsOpen = true;
					/*String*/ bugId="1199443"; // Bug 1199443 - Wrong "End date" in installed list after attaching 24-hour subscription on a unmapped-guest
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
					if (invokeWorkaroundWhileBugIsOpen && isPoolRestrictedToUnmappedVirtualSystems) {
						log.warning("Skipping the assertion of installed product start-end date range after subscribing to an unmapped_guests_only pool for ProductId '"+productId+"' while bug '"+bugId+"' is open.");
					} else	// call if (installedProduct.status.equals("Subscribed")) {
					// END OF WORKAROUND
					// behavior update after fix from Bug 767619 - Date range for installed products needs to be smarter.
					//Assert.assertEquals(InstalledProduct.formatDateString(installedProduct.startDate), ProductSubscription.formatDateString(productSubscription.startDate), "Installed Product '"+bundledProductName+"' starts on the same DAY as the consumed ProductSubscription: "+productSubscription);					
					if (installedProduct.status.equals("Subscribed")) {
						// assert the valid date range on the installed product match the validity period of the product subscription
						Assert.assertEquals(InstalledProduct.formatDateString(installedProduct.endDate), ProductSubscription.formatDateString(consumedProductSubscription.endDate), "Installed Product '"+bundledProductName+"' expires on the same DAY as the consumed ProductSubscription: "+consumedProductSubscription);
						Assert.assertEquals(InstalledProduct.formatDateString(installedProduct.startDate), ProductSubscription.formatDateString(consumedProductSubscription.startDate), "Installed Product '"+bundledProductName+"' starts on the same DAY as the consumed ProductSubscription: "+consumedProductSubscription);
					} else {
						/* valid assertion before Bug 990639 - Update information displayed on My Installed Products tab when product is partially subscribed
						// assert the date range on the installed product is None
						Assert.assertNull(installedProduct.startDate, "Installed Product '"+bundledProductName+"' start date range should be None/null when today's status '"+installedProduct.status+"' is NOT fully Subscribed.");
						Assert.assertNull(installedProduct.endDate, "Installed Product '"+bundledProductName+"' end date range should be None/null when today's status '"+installedProduct.status+"' is NOT fully Subscribed.");
						*/
						// assert the date range on the installed product is NOT None
						Assert.assertNotNull(installedProduct.startDate, "Installed Product '"+bundledProductName+"' start date range should NOT be None/null even when today's status '"+installedProduct.status+"' is NOT fully Subscribed.");
						Assert.assertNotNull(installedProduct.endDate, "Installed Product '"+bundledProductName+"' end date range should NOT be None/null even when today's status '"+installedProduct.status+"' is NOT fully Subscribed.");
					}
				}
				if (productCerts.isEmpty()) {
					Assert.assertNull(InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", bundledProductName, currentlyInstalledProducts),"There should NOT be an installed status report for '"+bundledProductName+"' since a corresponding product cert was not found in "+clienttasks.productCertDir);
				}
			}
		}
		

		
		// TODO I BELIEVE THIS FINAL BLOCK OF TESTING IS INACCURATE - jsefler 5/27/2012
		// I THINK IT SHOULD BE CHECKING HARDWARE SOCKETS AND NOT INSTALLED SOFTWARE
		/*
		// check if this subscription matches the installed software and then test for availability
		boolean subscriptionProductIdMatchesInstalledSoftware = false;
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, currentlyInstalledProductCerts)) {
				subscriptionProductIdMatchesInstalledSoftware=true; break;
			}
		}
		clienttasks.unsubscribeFromSerialNumber(entitlementCert.serialNumber);
		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", productId, clienttasks.getCurrentlyAvailableSubscriptionPools());
		if (currentlyInstalledProductCerts.isEmpty()) {
			log.info("A final assertion to verify that SubscriptionPool with ProductId '"+productId+"' is available based on matching installed software is not applicable when the list of installed software is empty.");
		} else {
			if (subscriptionProductIdMatchesInstalledSoftware) {
				Assert.assertNotNull(pool, "Expected SubscriptionPool with ProductId '"+productId+"' matches the installed software and is available for subscribing when listing --available.");
			} else {
				Assert.assertNull(pool, "Expected SubscriptionPool with ProductId '"+productId+"' does NOT match the installed software and is only available for subscribing when listing --all --available.");
			}
		}
		*/
		
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using product ID",
			groups={"Tier2Tests","blockedByBug-584137"},
			dataProvider="getAvailableSubscriptionPoolsData",
			enabled=false)	// Subscribing to a Subscription Pool using --product Id has been removed in subscription-manager-0.71-1.el6.i686.)
	@ImplementsNitrateTest(caseId=41680)
	@Deprecated
	public void testSubscribeToValidSubscriptionsByProductId_DEPRECATED(SubscriptionPool pool){
//		sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using product ID",
			groups={"Tier2Tests","blockedByBug-584137"},
			enabled=false)	// old/disabled test from ssalevan
	@Deprecated
	public void testSubscribeToASingleEntitlementByProductId_DEPRECATED(){
		clienttasks.unsubscribeFromTheCurrentlyConsumedProductSubscriptionSerialsIndividually();
		SubscriptionPool MCT0696 = new SubscriptionPool("MCT0696", "696");
		MCT0696.addProductID("Red Hat Directory Server");
		clienttasks.subscribeToSubscriptionPoolUsingProductId(MCT0696);
		//this.refreshSubscriptions();
		for (ProductSubscription pid:MCT0696.associatedProductIDs){
			Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().contains(pid),
					"ProductID '"+pid.productName+"' consumed from Pool '"+MCT0696.subscriptionName+"'");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27120", "RHEL7-51381"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using pool ID",
			groups={"Tier0Tests","Tier2Tests","blockedByBug-584137"},
			//dataProvider="getAvailableSubscriptionPoolsData",	// very thorough, but takes too long to execute and rarely finds more bugs
			dataProvider="getRandomSubsetOfAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41686)
	public void testSubscribeToValidSubscriptionsByPoolId(SubscriptionPool pool){
// non-dataProvided test procedure
//		sm.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to each available subscription pool using pool ID",
			groups={"Tier2Tests","blockedByBug-584137"},
			dataProvider="getGoodRegistrationData",
			enabled=false)	// 6/9/2014 - takes way too long to run and has never revealed a bug
	@ImplementsNitrateTest(caseId=41686)
	public void testSubscribeConsumerToEachAvailableSubscriptionPoolUsingPoolId(String username, String password, String owner){
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(username, password, owner, null, ConsumerType.system, null, null, Boolean.FALSE, null, null, (String)null, null, null, null, Boolean.FALSE, false, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsIndividually();
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using registration token",
			groups={"Tier2Tests","blockedByBug-584137"},
			enabled=false)	// Bug 670823 - if subscribe with regtoken is gone, then it should be removed from cli
	@Deprecated
	@ImplementsNitrateTest(caseId=41681)
	public void testSubscribeToRegToken_DEPRECATED(){
		clienttasks.unsubscribeFromTheCurrentlyConsumedProductSubscriptionSerialsIndividually();
		clienttasks.subscribeToRegToken(sm_regtoken);
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27123", "RHEL7-51392"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Subscribed for Already subscribed Entitlement.",
			groups={"Tier2Tests","blockedByBug-584137","blockedByBug-979492"},
			// dataProvider="getAvailableSubscriptionPoolsData", TAKES TOO LONG AND RARELY REVEALS A BUG - changing to getRandomSubsetOfAvailableSubscriptionPoolsData
			dataProvider="getRandomSubsetOfAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41897)
	public void testAttemptToSubscribeToAnAlreadySubscribedPool(SubscriptionPool pool) throws JSONException, Exception{
		String consumerId = clienttasks.getCurrentConsumerId();
		Assert.assertNull(CandlepinTasks.getConsumersNewestEntitlementSerialCorrespondingToSubscribedPoolId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId, pool.poolId),"The current consumer has not been granted any entitlements from pool '"+pool.poolId+"'.");
		Assert.assertNotNull(clienttasks.subscribeToSubscriptionPool_(pool),"Authenticator '"+sm_clientUsername+"' has been granted an entitlement from pool '"+pool.poolId+"' under organization '"+sm_clientOrg+"'.");
		BigInteger serial1 = CandlepinTasks.getConsumersNewestEntitlementSerialCorrespondingToSubscribedPoolId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId, pool.poolId);
		SSHCommandResult subscribeResult = clienttasks.subscribe_(null,null,pool.poolId,null,null, null, null, null, null, null, null, null, null);
		String subscribeStdout = subscribeResult.getStdout().trim();
		
		subscribeStdout = clienttasks.workaroundForBug906550(subscribeStdout);
		
		if (CandlepinTasks.isPoolProductMultiEntitlement(sm_clientUsername,sm_clientPassword,sm_serverUrl,pool.poolId)) {
			//Assert.assertEquals(subscribeStdout, String.format("Successfully consumed a subscription from the pool with id %s.",pool.poolId),	// Bug 812410 - Subscription-manager subscribe CLI feedback 
			//Assert.assertEquals(subscribeStdout, String.format("Successfully consumed a subscription for: %s",pool.subscriptionName),	// changed by Bug 874804 Subscribe -> Attach
			Assert.assertEquals(subscribeStdout, String.format("Successfully attached a subscription for: %s",pool.subscriptionName),
				"subscribe command allows multi-entitlement pools to be subscribed to by the same consumer more than once.");
			BigInteger serial2 = CandlepinTasks.getConsumersNewestEntitlementSerialCorrespondingToSubscribedPoolId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId, pool.poolId);
			Assert.assertNotSame(serial1,serial2,
				"Upon subscribing to a multi-entitlement pool '"+pool.poolId+"' for the second time, the newly granted entilement's serial '"+serial2+"' number differs from the first '"+serial1+"'.");
			Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.entitlementCertDir+File.separator+serial1+".pem"),
				"After subscribing to multi-entitlement pool '"+pool.poolId+"' for the second time, the first granted entilement cert file still exists.");
			Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.entitlementCertDir+File.separator+serial2+".pem"),
				"After subscribing to multi-entitlement pool '"+pool.poolId+"' for the second time, the second granted entilement cert file exists.");
		} else {
			String expectedStdout = String.format("This consumer is already subscribed to the product matching pool with id '%s'.",pool.poolId);
			if (!clienttasks.workaroundForBug876764(sm_serverType)) expectedStdout = String.format("This unit has already had the subscription matching pool ID '%s' attached.",pool.poolId);
			Assert.assertEquals(subscribeStdout, expectedStdout,
				"subscribe command returns proper message when the same consumer attempts to subscribe to a non-multi-entitlement pool more than once.");
			Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.entitlementCertDir+File.separator+serial1+".pem"),
				"After attempting to subscribe to pool '"+pool.poolId+"' for the second time, the first granted entilement cert file still exists.");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36587", "RHEL7-51390"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: subscribe consumer to multiple/duplicate/bad pools in one call",
			groups={"Tier2Tests","blockedByBug-622851","blockedByBug-995597"},
			enabled=true)
	public void testSubscribeToMultipleDuplicateAndBadPools() throws JSONException, Exception {
		
		// begin the test with a cleanly registered system
		clienttasks.unregister(null, null, null, null);
	    clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
	    
		// assemble a list of all the available SubscriptionPool ids with duplicates and bad ids
		List <String> poolIds = new ArrayList<String>();
		Map <String,String> poolNames= new HashMap<String,String>();
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			poolIds.add(pool.poolId);
			poolIds.add(pool.poolId); // add a duplicate poolid
			poolNames.put(pool.poolId, pool.subscriptionName);
		}
		String badPoolId1 = "bad123", badPoolId2 = "bad_POOLID"; 
		poolIds.add(0, badPoolId1); // insert a bad poolid
		poolIds.add(badPoolId2); // append a bad poolid
		
		// subscribe to all pool ids
		log.info("Attempting to subscribe to multiple pools with duplicate and bad pool ids...");
		SSHCommandResult subscribeResult = clienttasks.subscribe_(null, null, poolIds, null, null, null, null, null, null, null, null, null, null);
		
		/*
		No such entitlement pool: bad123
		Successfully subscribed the system to Pool 8a90f8c63159ce55013159cfd6c40303
		This consumer is already subscribed to the product matching pool with id '8a90f8c63159ce55013159cfd6c40303'.
		Successfully subscribed the system to Pool 8a90f8c63159ce55013159cfea7a06ac
		Successfully subscribed the system to Pool 8a90f8c63159ce55013159cfea7a06ac
		No such entitlement pool: bad_POOLID
		*/
		
		// assert the results...
		Assert.assertEquals(subscribeResult.getExitCode(), Integer.valueOf(0), "The exit code from the subscribe command indicates a success.");
		for (String poolId : poolIds) {
			String subscribeResultMessage;
			if (poolId.equals(badPoolId1) || poolId.equals(badPoolId2)) {
				subscribeResultMessage = String.format("No such entitlement pool: %s",poolId);
				subscribeResultMessage = String.format("Subscription pool %s does not exist.",poolId);
				subscribeResultMessage = String.format("Pool with id %s could not be found.",poolId);
				Assert.assertTrue(subscribeResult.getStdout().contains(subscribeResultMessage),"The subscribe result for an invalid pool '"+poolId+"' contains: "+subscribeResultMessage);
			}
			else if (CandlepinTasks.isPoolProductMultiEntitlement(sm_clientUsername,sm_clientPassword,sm_serverUrl,poolId)) {
				subscribeResultMessage = String.format("Successfully consumed a subscription from the pool with id %s.",poolId);	// Bug 812410 - Subscription-manager subscribe CLI feedback 
				subscribeResultMessage = String.format("Successfully consumed a subscription for: %s",poolNames.get(poolId));	// changed by Bug 874804 Subscribe -> Attach
				subscribeResultMessage = String.format("Successfully attached a subscription for: %s",poolNames.get(poolId));
				subscribeResultMessage += "\n"+subscribeResultMessage;
				Assert.assertTrue(subscribeResult.getStdout().contains(subscribeResultMessage),"The duplicate subscribe result for a multi-entitlement pool '"+poolId+"' contains: "+subscribeResultMessage);
			} else if (false) {
				// TODO case when there are no entitlements remaining for the duplicate subscribe
			} else {
				subscribeResultMessage = String.format("Successfully consumed a subscription from the pool with id %s.",poolId);	// Bug 812410 - Subscription-manager subscribe CLI feedback 
				subscribeResultMessage = String.format("Successfully consumed a subscription for: %s",poolNames.get(poolId));	// changed by Bug 874804 Subscribe -> Attach
				subscribeResultMessage = String.format("Successfully attached a subscription for: %s",poolNames.get(poolId));
				String subscribeResultSubMessage = String.format("This consumer is already subscribed to the product matching pool with id '%s'.",poolId);
				if (!clienttasks.workaroundForBug876764(sm_serverType)) subscribeResultSubMessage = String.format("This unit has already had the subscription matching pool ID '%s' attached.",poolId);
				subscribeResultMessage += "\n"+subscribeResultSubMessage;
				Assert.assertTrue(subscribeResult.getStdout().contains(subscribeResultMessage),"The duplicate subscribe result for pool '"+poolId+"' contains: "+subscribeResultMessage);			
			}
		}
	}
	
	
	protected String certFrequencyString=null;
	@BeforeGroups(groups={"setup"}, value={"rhsmcertdChangeCertFrequency_Test"})
	public void beforeRhsmcertdChangeCertFrequency() {
		if (clienttasks==null) return;
		if (certFrequencyString==null) certFrequencyString = clienttasks.getConfParameter("certCheckInterval");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36590", "RHEL7-51394"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="rhsmcertd: change certFrequency",
			groups={"Tier2Tests","rhsmcertdChangeCertFrequency_Test","blockedByBug-617703","blockedByBug-700952","blockedByBug-708512","blockedByBug-907638","blockedByBug-822402","blockedByBug-986572","blockedByBug-1000301","blockedByBug-1026435"},
			dataProvider="getCertFrequencyData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41692)
	public void testRhsmcertdChangeCertFrequency(int minutes) {
		String errorMsg = "Either the consumer is not registered with candlepin or the certificates are corrupted. Certificate updation using daemon failed.";
		errorMsg = "Either the consumer is not registered or the certificates are corrupted. Certificate update using daemon failed.";
		
		log.info("First test with an unregistered user and verify that the rhsmcertd actually fails since it cannot self-identify itself to the candlepin server.");
		clienttasks.unregister(null, null, null, null);
		clienttasks.restart_rhsmcertd(minutes, null, false); sleep(10000); // allow 10sec for the initial update
		log.info("Appending a marker in the '"+clienttasks.rhsmcertdLogFile+"' so we can assert that the certificates are being updated every '"+minutes+"' minutes");
		String marker = "Testing rhsm.conf certFrequency="+minutes+" when unregistered..."; // https://tcms.engineering.redhat.com/case/41692/
		RemoteFileTasks.runCommandAndAssert(client,"echo \""+marker+"\" >> "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),marker,null);
		sleep(minutes*60*1000);	// give the rhsmcertd a chance to check in with the candlepin server and update the certs
		//RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),"update failed \\(\\d+\\), retry in "+minutes+" minutes",null);
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),"\\(Cert Check\\) Update failed \\(255\\), retry will occur on next run.",null);
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmLogFile,Integer.valueOf(0),errorMsg,null);
		
		
		log.info("Now test with a registered user whose identity is corrupt and verify that the rhsmcertd actually fails since it cannot self-identify itself to the candlepin server.");
		String consumerid = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null));
		log.info("Corrupting the identity cert by borking its content...");
		RemoteFileTasks.runCommandAndAssert(client, "openssl x509 -noout -text -in "+clienttasks.consumerCertFile()+" > /tmp/stdout; mv /tmp/stdout -f "+clienttasks.consumerCertFile(), 0);
		clienttasks.restart_rhsmcertd(minutes, null, false); sleep(10000); // allow 10sec for the initial update
		log.info("Appending a marker in the '"+clienttasks.rhsmcertdLogFile+"' so we can assert that the certificates are being updated every '"+minutes+"' minutes");
		marker = "Testing rhsm.conf certFrequency="+minutes+" when identity is corrupted...";
		RemoteFileTasks.runCommandAndAssert(client,"echo \""+marker+"\" >> "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),marker,null);
		sleep(minutes*60*1000);	// give the rhsmcertd a chance to check in with the candlepin server and update the certs
		//RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),"update failed \\(\\d+\\), retry in "+minutes+" minutes",null);
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),"\\(Cert Check\\) Update failed \\(255\\), retry will occur on next run.",null);
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmLogFile,Integer.valueOf(0),errorMsg,null);

		
		log.info("Finally test with a registered user and verify that the rhsmcertd succeeds because he can identify himself to the candlepin server.");
		if (true) {	// WORKAROUND for new RHEL7 selinux-policy troubles
			// Although the following register with consumerid will successfully re-establish a good /etc/pki/consumer/cert.pem, the selinux-policy on RHEL7 is not happy with the permissions on the file after "Corrupting the identity cert by borking its content..."
			// The result is the following error in rhsm.log and the audit.log
			//
			//	2013-11-21 12:16:54,135 [WARNING] rhsmcertd-worker @identity.py:64 - possible certificate corruption
			//	2013-11-21 12:16:54,136 [ERROR] rhsmcertd-worker @identity.py:65 - [Errno 13] Permission denied: '/etc/pki/consumer/cert.pem'
			//	2013-11-21 12:16:54,136 [ERROR] rhsmcertd-worker @rhsmcertd-worker:43 - Either the consumer is not registered or the certificates are corrupted. Certificate update using daemon failed.
			//
			//	type=AVC msg=audit(1385054754.518:21794): avc:  denied  { open } for  pid=22456 comm="rhsmcertd-worke" path="/etc/pki/consumer/cert.pem" dev="dm-1" ino=25317473 scontext=system_u:system_r:rhsmcertd_t:s0 tcontext=unconfined_u:object_r:user_tmp_t:s0 tclass=file
			//	type=SYSCALL msg=audit(1385054754.518:21794): arch=c000003e syscall=2 success=no exit=-13 a0=147ab10 a1=0 a2=1b6 a3=0 items=0 ppid=20771 pid=22456 auid=4294967295 uid=0 gid=0 euid=0 suid=0 fsuid=0 egid=0 sgid=0 fsgid=0 tty=(none) ses=4294967295 comm="rhsmcertd-worke" exe="/usr/bin/python2.7" subj=system_u:system_r:rhsmcertd_t:s0 key=(null)
			//
			// The WORKAROUND is to tell subscription-manager to clean before registering with the consumerId
			clienttasks.clean();
		}
	    clienttasks.register(sm_clientUsername, sm_clientPassword, null, null, null, null, consumerid, null, null, null, (String)null, null, null, null, /* --force Boolean.TRUE was okay prior to subscription-manager-1.16.2-1 commit f14d2618ea94c18a0295ae3a5526a2ff252a3f99 and 6bd0448c85c10d8a58cae10372f0d4aa323d5c27 changing to */ Boolean.FALSE, false, null, null, null, null);
		clienttasks.restart_rhsmcertd(minutes, null, true); sleep(10000); // allow 10sec for the initial update
		log.info("Appending a marker in the '"+clienttasks.rhsmcertdLogFile+"' so we can assert that the certificates are being updated every '"+minutes+"' minutes");
		marker = "Testing rhsm.conf certFrequency="+minutes+" when registered..."; // https://tcms.engineering.redhat.com/case/41692/
		RemoteFileTasks.runCommandAndAssert(client,"echo \""+marker+"\" >> "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),marker,null);
		sleep(minutes*60*1000);	// give the rhsmcertd a chance to check in with the candlepin server and update the certs
		///RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),"certificates updated",null);
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),"\\(Cert Check\\) Certificates updated.",null);
		
		//	# tail -f /var/log/rhsm/rhsm.log
		//	2010-09-10 12:05:06,338 [ERROR] main() @certmgr.py:75 - Either the consumer is not registered with candlepin or the certificates are corrupted. Certificate updation using daemon failed.
		
		//	# tail -f /var/log/rhsm/rhsmcertd.log
		//	Fri Sep 10 11:59:50 2010: started: interval = 1 minutes
		//	Fri Sep 10 11:59:51 2010: update failed (255), retry in 1 minutes
		//	testing rhsm.conf certFrequency=1 when unregistered.
		//	Fri Sep 10 12:00:51 2010: update failed (255), retry in 1 minutes
		//	Fri Sep 10 12:01:04 2010: started: interval = 1 minutes
		//	Fri Sep 10 12:01:05 2010: certificates updated
		//	testing rhsm.conf certFrequency=1 when registered.
		//	Fri Sep 10 12:02:05 2010: certificates updated
		
		// AFTER CHANGES FROM Bug 708512 - rhsmcertd is logging "certificates updated" when it should be "update failed (255), retry in 1 minutes"
		//	# tail -f /var/log/rhsm/rhsmcertd.log
		//	Testing rhsm.conf certFrequency=2 when unregistered...
		//	Thu Aug  9 18:57:24 2012 [WARN] (Cert Check) Update failed (255), retry will occur on next run.
		//	1344553073761 Testing service rhsmcertd restart...
		//	Thu Aug  9 18:57:54 2012 [INFO] rhsmcertd is shutting down...
		//	Thu Aug  9 18:57:54 2012 [INFO] Starting rhsmcertd...
		//	Thu Aug  9 18:57:54 2012 [INFO] Healing interval: 1440.0 minute(s) [86400 second(s)]
		//	Thu Aug  9 18:57:54 2012 [INFO] Cert check interval: 2.0 minute(s) [120 second(s)]
		//	Thu Aug  9 18:57:54 2012 [INFO] Waiting 120 second(s) [2.0 minute(s)] before running updates.
		//	Thu Aug  9 18:59:54 2012 [WARN] (Healing) Update failed (255), retry will occur on next run.
		//	Thu Aug  9 18:59:54 2012 [WARN] (Cert Check) Update failed (255), retry will occur on next run.
		//	Thu Aug  9 18:59:54 2012 [WARN] (Cert Check) Update failed (255), retry will occur on next run.
		//	Testing rhsm.conf certFrequency=2 when identity is corrupted...
		//	Thu Aug  9 19:01:54 2012 [WARN] (Cert Check) Update failed (255), retry will occur on next run.
		//	1344553342931 Testing service rhsmcertd restart...
		//	Thu Aug  9 19:02:23 2012 [INFO] rhsmcertd is shutting down...
		//	Thu Aug  9 19:02:23 2012 [INFO] Starting rhsmcertd...
		//	Thu Aug  9 19:02:23 2012 [INFO] Healing interval: 1440.0 minute(s) [86400 second(s)]
		//	Thu Aug  9 19:02:23 2012 [INFO] Cert check interval: 2.0 minute(s) [120 second(s)]
		//	Thu Aug  9 19:02:23 2012 [INFO] Waiting 120 second(s) [2.0 minute(s)] before running updates.
		//	Thu Aug  9 19:04:25 2012 [INFO] (Healing) Certificates updated.
		//	Thu Aug  9 19:04:30 2012 [INFO] (Cert Check) Certificates updated.
		//	Thu Aug  9 19:04:35 2012 [INFO] (Cert Check) Certificates updated.
		//	Testing rhsm.conf certFrequency=2 when registered...
		//	Thu Aug  9 19:06:28 2012 [INFO] (Cert Check) Certificates updated.

	}
	@AfterGroups(groups={"setup"}, value={"rhsmcertdChangeCertFrequency_Test"})
	public void afterRhsmcertdChangeCertFrequency() {
		if (certFrequencyString!=null) clienttasks.restart_rhsmcertd(Integer.valueOf(certFrequencyString), null, null);
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36584", "RHEL7-51387"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="rhsmcertd: ensure certificates synchronize",
			groups={"Tier2Tests","rhsmcertdEnsureCertificatesSynchronize_Test","blockedByBug-617703","blockedByBug-907638","blockedByBug-822402","blockedByBug-986572","blockedByBug-1000301","blockedByBug-1026435"},
			enabled=true)
	@ImplementsNitrateTest(caseId=41694)
	public void testRhsmcertdEnsureCertificatesSynchronize() throws JSONException, Exception{
		
		// start with a cleanly unregistered system
		clienttasks.unregister(null, null, null, null);
		
		// register a clean user
	    clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
	    
	    // subscribe to all the available pools
	    clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
	    
	    // get all of the current entitlement certs and remember them
	    List<File> entitlementCertFiles = clienttasks.getCurrentEntitlementCertFiles();
	    
	    // delete all of the entitlement cert files
	    client.runCommandAndWait("rm -rf "+clienttasks.entitlementCertDir+"/*");
	    Assert.assertEquals(clienttasks.getCurrentEntitlementCertFiles().size(), 0,
	    		"All the entitlement certs have been deleted.");
		
	    // restart the rhsmcertd to run every 1 minute and wait for a refresh
	    beforeRhsmcertdChangeCertFrequency();
		clienttasks.restart_rhsmcertd(1, null, true);
		
		// assert that rhsmcertd has refreshed the entitlement certs back to the original
	    Assert.assertEquals(clienttasks.getCurrentEntitlementCertFiles(), entitlementCertFiles,
	    		"All the deleted entitlement certs have been re-synchronized by rhsmcertd (rhsm cert deamon).");
	}
	@AfterGroups(groups={"setup"}, value={"rhsmcertdEnsureCertificatesSynchronize_Test"})
	public void afterRhsmcertdEnsureCertificatesSynchronize() {
		clienttasks.unregister_(null, null, null, null);
		if (certFrequencyString!=null) clienttasks.restart_rhsmcertd(Integer.valueOf(certFrequencyString), null, null);
		sleep(10*1000);
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19982", "RHEL7-51016"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: make sure the normal available pools come from subscriptions that pass the hardware rules for availability.",
			groups={"Tier1Tests"},
			dependsOnGroups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	// Note: The objective if this test is essentially the same as ListTests.EnsureHardwareMatchingSubscriptionsAreListedAsAvailable_Test() and ListTests.EnsureNonHardwareMatchingSubscriptionsAreNotListedAsAvailable_Test(), but its implementation is slightly different
	public void testNormalAvailablePoolsFromSubscriptionsPassTheHardwareRulesCheck() throws Exception {
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		// to prevent the unmapped_guests_only pools from being available for autosubscribe in subsequent VerifyInstalledProductCertWasAutoSubscribed_Test,
		// let's pretend that this system is a host consumer and simulate virt-who by mapping this virtual system as a guest of itself
		if (Boolean.valueOf(clienttasks.getFactValue("virt.is_guest"))) clienttasks.mapSystemAsAGuestOfItself();
		
		subscriptionPoolProductData = getSystemSubscriptionPoolProductDataAsListOfLists(true,false);
		List<SubscriptionPool> availableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		for (List<Object> subscriptionPoolProductDatum : subscriptionPoolProductData) {
			String productId = (String)subscriptionPoolProductDatum.get(0);
			SubscriptionPool subscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", productId, availableSubscriptionPools);
// THIS CASE WAS SOLVED BY REFRESHING THE POOLS AVAILABLE TO THIS OWNER
//			// special case...
//			if (subscriptionPool==null) {	// when pool is null, a likely reason is that the subscription pools arch does not support this system's arch, let's check...
//				for (String poolId: CandlepinTasks.getPoolIdsForProductId(sm_clientUsername, sm_clientPassword, sm_serverUrl, clienttasks.getCurrentlyRegisteredOwnerKey(), productId)) {
//					String poolProductAttributeArchValue= (String) CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, poolId, "arch");
//					if (poolProductAttributeArchValue!=null) {
//						List<String> poolProductAttributesArches = new ArrayList<String>();
//							poolProductAttributesArches.addAll(Arrays.asList(poolProductAttributeArchValue.trim().split(" *, *")));	// Note: the arch attribute can be a comma separated list of values
//						if (poolProductAttributesArches.contains("x86")) {poolProductAttributesArches.addAll(Arrays.asList("i386","i486","i586","i686"));}  // Note: x86 is a general arch to cover all 32-bit intel microprocessors 
//						if (poolProductAttributesArches.contains("ALL")) poolProductAttributesArches.add(clienttasks.arch);
//						if (!poolProductAttributesArches.contains(clienttasks.arch)) {
//							throw new SkipException("Pool product '"+productId+"' supports arches '"+poolProductAttributeArchValue+"'.  This system's arch is '"+clienttasks.arch+"', hence this matching hardware test for list available is not applicable on this system.");
//						}
//					}
//				}	
//			}
			// special case...
			if (subscriptionPool==null) {
				// when pool is null, another likely cause is that all of the available subscriptions from the pools are being consumed, let's check...
				for (String poolId: CandlepinTasks.getPoolIdsForProductId(sm_clientUsername, sm_clientPassword, sm_serverUrl, clienttasks.getCurrentlyRegisteredOwnerKey(), productId)) {
					int quantity = (Integer) CandlepinTasks.getPoolValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, poolId, "quantity");
					int consumed = (Integer) CandlepinTasks.getPoolValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, poolId, "consumed");
					if (consumed>=quantity) {
						throw new SkipException("The total quantity '"+quantity+"' of subscriptions from poolId '"+poolId+"' for product '"+productId+"' are being consumed; hence this pool is appropriately not available despite a match in hardware");
					}
				}
				// another possible cause is that the pool.productAttributes.arch list on the pool is older than the subscription.product.attributes.arch
				SubscriptionPool poolFromAllAvailable = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", productId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
				if (poolFromAllAvailable!=null) {
					String poolArch = (String) CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, poolFromAllAvailable.poolId, "arch");
					List <String> poolArches = new ArrayList<String>(); poolArches.addAll(Arrays.asList(poolArch.trim().split(" *, *")));	// Note: the arch attribute can be a comma separated list of values

					String subscriptionId = CandlepinTasks.getSubscriptionIdForPoolId(sm_clientUsername, sm_clientPassword, sm_serverUrl, poolFromAllAvailable.poolId);
					JSONObject jsonSubscription = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,"/subscriptions/"+subscriptionId));
					String subscriptionArch = CandlepinTasks.getResourceAttributeValue(jsonSubscription.getJSONObject("product"), "arch");
					List <String> subscriptionArches = new ArrayList<String>(); subscriptionArches.addAll(Arrays.asList(subscriptionArch.trim().split(" *, *")));	// Note: the arch attribute can be a comma separated list of values
					
					if (!poolArches.containsAll(subscriptionArches) || !subscriptionArches.containsAll(poolArches)) {
						log.warning("There is an all available pool for product '"+productId+"' whose arch "+poolArches+" does not match its corresponding subscription arch "+subscriptionArches+".  Likely, there was an upstream SKU change by dev-ops that requires an org level pool refresh.");
					}
				}
			}
			
			Assert.assertNotNull(subscriptionPool, "Expecting SubscriptionPool with ProductId '"+productId+"' to be available to registered user '"+sm_clientUsername+"'. (Look for warnings above to explain a failure. A pool refresh may also fix a failure.)");
		}
		for (SubscriptionPool availableSubscriptionPool : availableSubscriptionPools) {
			
			// skip pools that are not NORMAL (eg. BONUS, ENTITLEMENT_DERIVED, STACK_DERIVED)
			String poolType = (String) CandlepinTasks.getPoolValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, availableSubscriptionPool.poolId, "type");
			if (!poolType.equals("NORMAL")) {
				log.warning("Skipping '"+poolType+"' pool: "+availableSubscriptionPool);
				continue;
			}
			
			boolean productIdFound = false;
			for (List<Object> subscriptionPoolProductDatum : subscriptionPoolProductData) {
				if (availableSubscriptionPool.productId.equals((String)subscriptionPoolProductDatum.get(0))) {
					productIdFound = true;
					break;
				}
			}
			Assert.assertTrue(productIdFound, "Available SubscriptionPool '"+availableSubscriptionPool.productId+"' poolId='"+availableSubscriptionPool.poolId+"' passes the hardware rules check.");
		}
	}
	protected List<List<Object>> subscriptionPoolProductData;


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19984", "RHEL7-51018"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager-cli: autosubscribe consumer and verify expected subscription pool product id are consumed",
			groups={"Tier1Tests","AutoSubscribeAndVerify", "blockedByBug-680399", "blockedByBug-734867", "blockedByBug-740877"},
			dependsOnMethods={"testNormalAvailablePoolsFromSubscriptionsPassTheHardwareRulesCheck"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testInititiateAutoSubscribe() throws Exception {
		// cleanup the current consumer
		clienttasks.unregister_(null, null, null, null);
		clienttasks.deleteFactsFileWithOverridingValues();
		
		// re-calculate the subscriptionPoolProductData accounting for a match to installed system software
		subscriptionPoolProductData = getSystemSubscriptionPoolProductDataAsListOfLists(true,true);
		
		// autosubscribe
		sshCommandResultFromAutosubscribe = clienttasks.subscribe(true,null,(String)null,null,null,null,null,null,null,null,null, null, null);
		
		/* RHEL57 RHEL61 Example Results...
		# subscription-manager subscribe --auto
		Installed Products:
		   Multiplier Product Bits - Not Subscribed
		   Load Balancing Bits - Subscribed
		   Awesome OS Server Bits - Subscribed
		   Management Bits - Subscribed
		   Awesome OS Scalable Filesystem Bits - Subscribed
		   Shared Storage Bits - Subscribed
		   Large File Support Bits - Subscribed
		   Awesome OS Workstation Bits - Subscribed
		   Awesome OS Premium Architecture Bits - Not Subscribed
		   Awesome OS for S390X Bits - Not Subscribed
		   Awesome OS Developer Basic - Not Subscribed
		   Clustering Bits - Subscribed
		   Awesome OS Developer Bits - Not Subscribed
		   Awesome OS Modifier Bits - Subscribed
		*/
		
		/* Example Results...
		# subscription-manager subscribe --auto
		Installed Product Current Status:
		
		ProductName:         	Awesome OS for x86_64/ALL Bits for ZERO sockets
		Status:               	Subscribed               
		
		
		ProductName:         	Awesome OS for x86_64/ALL Bits
		Status:               	Subscribed               
		
		
		ProductName:         	Awesome OS for ppc64 Bits
		Status:               	Subscribed               
		
		
		ProductName:         	Awesome OS for i386 Bits 
		Status:               	Subscribed               
		
		
		ProductName:         	Awesome OS for x86 Bits  
		Status:               	Subscribed               
		
		
		ProductName:         	Awesome OS for ia64 Bits 
		Status:               	Subscribed               
		
		
		ProductName:         	Awesome OS Scalable Filesystem Bits
		Status:               	Subscribed               
		*/
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19985", "RHEL7-51019"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager-cli: autosubscribe consumer and verify expected subscription pool product id are consumed",
			groups={"Tier1Tests","AutoSubscribeAndVerify","blockedByBug-672438","blockedByBug-678049","blockedByBug-743082","blockedByBug-865193","blockedByBug-864383","blockedByBug-977321"},
			dependsOnMethods={"testInititiateAutoSubscribe"},
			dataProvider="getInstalledProductCertsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testInstalledProductCertWasAutoSubscribed(Object bugzilla, ProductCert productCert) throws Exception {
		
		// search the subscriptionPoolProductData for a bundledProduct matching the productCert's productName
		// (subscriptionPoolProductData was set in a prior test methods that this test depends on)
		String subscriptionPoolProductId = null;
		for (List<Object> row : subscriptionPoolProductData) {
			JSONArray bundledProductDataAsJSONArray = (JSONArray)row.get(1);
			
			for (int j=0; j<bundledProductDataAsJSONArray.length(); j++) {
				JSONObject bundledProductAsJSONObject = (JSONObject) bundledProductDataAsJSONArray.get(j);
				String bundledProductName = bundledProductAsJSONObject.getString("productName");
				String bundledProductId = bundledProductAsJSONObject.getString("productId");

				if (bundledProductId.equals(productCert.productId)) {
					subscriptionPoolProductId = (String)row.get(0); // found
					break;
				}
			}
			if (subscriptionPoolProductId!=null) break;
		}
		
		// determine what autosubscribe results to assert for this installed productCert 
		InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", productCert.productName, clienttasks.getCurrentlyInstalledProducts());

		// when subscriptionPoolProductId!=null, then this productCert should have been autosubscribed
		String expectedSubscribeStatus = (subscriptionPoolProductId!=null)? "Subscribed":"Not Subscribed";
		
		// assert the installed product status matches the expected status 
		Assert.assertEquals(installedProduct.status,expectedSubscribeStatus,
				"As expected, the Installed Product Status reflects that the autosubscribed ProductName '"+productCert.productName+"' is "+expectedSubscribeStatus.toLowerCase()+".  (Note: a \"Not Subscribed\" status is expected when the subscription does not match the hardware socket requirements or the required tags on all the subscription content is not satisfied by any of the installed software.)");

		// assert that the sshCommandResultOfAutosubscribe showed the expected Subscribe Status for this productCert
		// RHEL57 RHEL61		Assert.assertContainsMatch(sshCommandResultFromAutosubscribe.getStdout().trim(), "^\\s+"+productCert.productName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)"+" - "+expectedSubscribeStatus),
		//		"As expected, ProductName '"+productCert.productName+"' was reported as '"+expectedSubscribeStatus+"' in the output from register with autotosubscribe.");
		List<InstalledProduct> autosubscribedProductStatusList = InstalledProduct.parse(sshCommandResultFromAutosubscribe.getStdout());
		InstalledProduct autosubscribedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", productCert.productName, autosubscribedProductStatusList);
		Assert.assertEquals(autosubscribedProduct.status,expectedSubscribeStatus,
				"As expected, ProductName '"+productCert.productName+"' was reported as '"+expectedSubscribeStatus+"' in the output from register with autotosubscribe.");
	}
	SSHCommandResult sshCommandResultFromAutosubscribe;


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36580", "RHEL7-51382"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: autosubscribe consumer more than once and verify we are not duplicately subscribed",
			groups={"Tier2Tests","blockedByBug-723044","blockedByBug-743082","blockedByBug-977321"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testSubscribeWithAutoMoreThanOnce() throws Exception {

		// before testing, make sure all the expected subscriptionPoolProductId are available
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		// TEMPORARY WORKAROUND
		if (Boolean.valueOf(clienttasks.getFactValue("virt.is_guest"))) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1198494"; // Bug 1198494 - Auto-heal continuously attaches subscriptions to make the system compliant on a guest machine
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				clienttasks.mapSystemAsAGuestOfItself();
			}
		}
		// END OF WORKAROUND
		
		// autosubscribe once
		SSHCommandResult result1 = clienttasks.subscribe(Boolean.TRUE,null,(String)null,null,null,null,null,null,null, null, null, null, null);
		List<File> entitlementCertFiles1 = clienttasks.getCurrentEntitlementCertFiles();
		List<InstalledProduct> autosubscribedProductStatusList1 = InstalledProduct.parse(result1.getStdout());
		
		// autosubscribe twice
		SSHCommandResult result2 = clienttasks.subscribe(Boolean.TRUE,null,(String)null,null,null,null,null,null,null, null, null, null, null);
		List<File> entitlementCertFiles2 = clienttasks.getCurrentEntitlementCertFiles();
		List<InstalledProduct> autosubscribedProductStatusList2 = InstalledProduct.parse(result2.getStdout());
		
		// assert results
		Assert.assertEquals(entitlementCertFiles2.size(), entitlementCertFiles1.size(), "The number of granted entitlement certs is the same after a second autosubscribe.");
		Assert.assertEquals(autosubscribedProductStatusList2.size(), autosubscribedProductStatusList1.size(), "The stdout from autosubscribe reports the same number of installed product status entries after a second autosubscribe.");
		Assert.assertTrue(autosubscribedProductStatusList1.containsAll(autosubscribedProductStatusList2), "The list of installed product status entries from a second autosubscribe is the same as the first.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36588", "RHEL7-51391"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: call the Candlepin API dry_run to get the pools and quantity that would be used to complete an autosubscribe with an unavailable service level",
			groups={"Tier2Tests","blockedByBug-864508"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testCandlepinConsumerEntitlementsDryrunWithUnavailableServiceLevel() throws JSONException, Exception {
		// register with force
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null));

		String serviceLevel = "FOO";
		JSONObject jsonDryrunResult= new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, String.format("/consumers/%s/entitlements/dry-run%s",consumerId, serviceLevel==null?"":String.format("?service_level=%s",serviceLevel))));
		
		Assert.assertTrue(jsonDryrunResult.has("displayMessage"),"The dry-run results threw an error with a displayMessage when attempting to run wirh serviceLevel '"+serviceLevel+"' ");
		
		String expectedDryrunResult = String.format("Service level %s is not available to consumers of organization %s.","FOO",sm_clientOrg);	// valid before bug fix 864508
		expectedDryrunResult = String.format("Service level '%s' is not available to consumers of organization %s.","FOO",sm_clientOrg);
		if (!clienttasks.workaroundForBug876764(sm_serverType)) expectedDryrunResult = String.format("Service level '%s' is not available to units of organization %s.","FOO",sm_clientOrg);
		Assert.assertEquals(jsonDryrunResult.getString("displayMessage"),expectedDryrunResult, "JSON results from a Candlepin Restful API call to dry-run with an unavailable service level.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19983", "RHEL7-51017"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: call the Candlepin API dry_run to get the pools and quantity that would be used to complete an autosubscribe with a valid service level",
			groups={"Tier1Tests","blockedByBug-859652","blockedByBug-962520"},
			dataProvider="getSubscribeWithAutoAndServiceLevelData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testCandlepinConsumerEntitlementsDryrunWithServiceLevel(Object bugzilla, String serviceLevel) throws JSONException, Exception {
		// Reference: https://engineering.redhat.com/trac/Entitlement/wiki/SlaSubscribe
	    //"GET"
	    //"url": "/consumers/{consumer_uuid}/entitlements/dry-run?service_level=#{service_level}", 
		
		String consumerId = clienttasks.getCurrentConsumerId();
		
		//  on the first call to this dataProvided test, unsubscribe all subscriptions OR just unregister to a clean state
		// this will remove any prior subscribed modifier entitlements to avoid test logic errors in this test.
		if (firstcalltoCandlepinConsumerEntitlementsDryrunWithServiceLevel_Test) {
			if (consumerId!=null) clienttasks.unsubscribe(true, (BigInteger)null, null, null, null, null, null);	//OR clienttasks.unregister(null,null,null);
			firstcalltoCandlepinConsumerEntitlementsDryrunWithServiceLevel_Test = false;
		}

		// store the initial state of the system
		if (consumerId==null) consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, "SubscriptionServiceLevelConsumer", null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null));
		String orgKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);	//clienttasks.getCurrentlyRegisteredOwnerKey();
		String initialServiceLevel = clienttasks.getCurrentServiceLevel();
		List<EntitlementCert> initialEntitlementCerts = clienttasks.getCurrentEntitlementCerts();
		List<SubscriptionPool> initialAvailableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		
		// get the current exempt service levels
		List<String> exemptServiceLevels = CandlepinTasks.getServiceLevelsForOrgKey(sm_clientUsername, sm_clientPassword, sm_serverUrl, orgKey, true);
		List<String> exemptServiceLevelsInUpperCase = new ArrayList<String>();
		for (String exemptServiceLevel : exemptServiceLevels) exemptServiceLevelsInUpperCase.add(exemptServiceLevel.toUpperCase());
		
		// call the candlepin API
		// curl --insecure --user testuser1:password --request GET https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/consumers/7033f5c0-c451-4d4c-bf88-c5061dc2c521/entitlements/dry-run?service_level=Premium | python -m simplejson/tool
		JSONArray jsonDryrunResults= new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, String.format("/consumers/%s/entitlements/dry-run%s",consumerId, serviceLevel==null?"":String.format("?service_level=%s",urlEncode(serviceLevel)))));	// urlEncode is needed to handle whitespace in the serviceLevel

		// assert that each of the dry run results match the service level (or is null, or "") and the proposed quantity is available
		//List<SubscriptionPool> dryrunSubscriptionPools = new ArrayList<SubscriptionPool>();
		for (int i = 0; i < jsonDryrunResults.length(); i++) {
			// jsonDryrunResults is an array of two values per entry: "pool" and "quantity"
			JSONObject jsonPool = ((JSONObject) jsonDryrunResults.get(i)).getJSONObject("pool");
			Integer quantity = ((JSONObject) jsonDryrunResults.get(i)).getInt("quantity");
			
			// assert that all of the pools proposed provide the requested service level (or a no support_level is now a valid contender based on Bug 1223560 - Service levels on an activation key prevent custom products from attaching at registration if auto-attach enabled)
			String poolId = jsonPool.getString("id");
			String poolProductAttributeSupportLevel = CandlepinTasks.getPoolProductAttributeValue(jsonPool, "support_level");
			SubscriptionPool subscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", poolId, initialAvailableSubscriptionPools);
			//dryrunSubscriptionPools.add(subscriptionPool);
			if (serviceLevel==null || serviceLevel.isEmpty()) {
				log.info("Without requesting a service-level, pool '"+poolId+"' returned by the dry-run results  has a support_level of '"+poolProductAttributeSupportLevel+"'.");
			} else if (poolProductAttributeSupportLevel==null || poolProductAttributeSupportLevel.isEmpty()) {
				log.info("Despite the requested service-level '"+serviceLevel+"', pool '"+poolId+"' returned by the dry-run results has a support_level of '"+poolProductAttributeSupportLevel+"'.  (Requested behavior from bug https://bugzilla.redhat.com/show_bug.cgi?id=1223560)");	// candlepin commit 9cefb6e23baefcc4ee2e14423f205edd37eecf22
			} else if (exemptServiceLevelsInUpperCase.contains(poolProductAttributeSupportLevel.toUpperCase())) {
				log.info("Pool '"+poolId+"' returned by the dry-run results provides the exempt support_level '"+poolProductAttributeSupportLevel+"'.");
			} else {
				//CASE SENSITIVE ASSERTION Assert.assertEquals(support_level, serviceLevel,"Pool '"+poolId+"' returned by the dry-run results provides the requested service-level '"+serviceLevel+"'.");
				Assert.assertTrue(serviceLevel.equalsIgnoreCase(poolProductAttributeSupportLevel),"Pool '"+poolId+"' returned by the dry-run results provides a case-insensitive support_level '"+poolProductAttributeSupportLevel+"' match to the requested service-level '"+serviceLevel+"'.");
			}
			
			Assert.assertNotNull(subscriptionPool,"Pool '"+poolId+"' returned by the dry-run results for service-level '"+serviceLevel+"' was found in the list --available.");
			Assert.assertTrue(quantity<=(subscriptionPool.quantity.equalsIgnoreCase("unlimited")?quantity+1:Integer.valueOf(subscriptionPool.quantity)),"Pool '"+poolId+"' returned by the dry-run results for service-level '"+serviceLevel+"', will supply a quantity ("+quantity+") that is within the available quantity ("+subscriptionPool.quantity+").");
		}
		// TODO: This assert is not reliable unless there really is a pool that provides a product that is actually installed.
		//Assert.assertTrue(jsonDryrunResults.length()>0, "Dry-run results for service-level '"+serviceLevel+"' are not empty.");
		
		// assert the the dry-run did not change the current service level
		Assert.assertEquals(clienttasks.getCurrentServiceLevel(), initialServiceLevel,"The consumer's current service level setting was not affected by the dry-run query with serviceLevel '"+serviceLevel+"'.");
		clienttasks.identity(null, null, true, null, null, null, null, null);
		Assert.assertEquals(clienttasks.getCurrentServiceLevel(), initialServiceLevel,"The consumer's current service level setting was not affected by the dry-run query with serviceLevel '"+serviceLevel+"' even after an identity regeneration.");
		
		// assert that no new entitlements were actually given
		Assert.assertTrue(clienttasks.getCurrentEntitlementCerts().containsAll(initialEntitlementCerts), "This system's prior entitlements are unchanged after the dry-run.");
		
		// actually autosubscribe with this service-level
		clienttasks.subscribe(true, serviceLevel, (List<String>)null, (List<String>)null, (List<String>)null, null, null, null, null, null, null, null, null);
		//clienttasks.subscribe(true,"".equals(serviceLevel)?String.format("\"%s\"", serviceLevel):serviceLevel, (List<String>)null, (List<String>)null, (List<String>)null, null, null, null, null, null, null);
		
		// determine the newly granted entitlement certs
		List<ProductSubscription> currentlyConsumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
 		List<EntitlementCert> newlyGrantedEntitlementCerts = new ArrayList<EntitlementCert>();
 		List<EntitlementCert> currentlyGrantedEntitlementCerts = clienttasks.getCurrentEntitlementCerts();
		for (EntitlementCert entitlementCert : currentlyGrantedEntitlementCerts) {
			if (!initialEntitlementCerts.contains(entitlementCert)) {
				newlyGrantedEntitlementCerts.add(entitlementCert);
				if (serviceLevel==null || serviceLevel.equals("")) {
					log.info("Without specifying a service level preference, the service level provided by the entitlement cert granted after autosubscribe is '"+entitlementCert.orderNamespace.supportLevel+"'.");
				} else if (entitlementCert.orderNamespace.supportLevel==null || entitlementCert.orderNamespace.supportLevel.isEmpty()) {
					log.info("Despite the requested service-level '"+serviceLevel+"', the entitlement cert granted after autosubscribe has a support_level of '"+entitlementCert.orderNamespace.supportLevel+"'.  (Requested behavior from bug https://bugzilla.redhat.com/show_bug.cgi?id=1223560)");	// candlepin commit 9cefb6e23baefcc4ee2e14423f205edd37eecf22
				} else if (entitlementCert.orderNamespace.supportLevel!=null && exemptServiceLevelsInUpperCase.contains(entitlementCert.orderNamespace.supportLevel.toUpperCase())) {
					log.info("After autosubscribe with service level '"+serviceLevel+"', this autosubscribed entitlement provides an exempt service level '"+entitlementCert.orderNamespace.supportLevel+"' from entitled orderNamespace: "+entitlementCert.orderNamespace);
				} else {
					//CASE SENSITIVE ASSERTION Assert.assertEquals(entitlementCert.orderNamespace.supportLevel,serviceLevel,"The service level provided by the entitlement cert granted after autosubscribe matches the requested servicelevel.");
					Assert.assertTrue(serviceLevel.equalsIgnoreCase(entitlementCert.orderNamespace.supportLevel),"Ignoring case, the service level '"+entitlementCert.orderNamespace.supportLevel+"' provided by the entitlement cert granted after autosubscribe matches the requested servicelevel '"+serviceLevel+"'.");
				}
			}
		}
		
		// calling candlepin API with a service_level="" is actually a special case:   /consumers/<UUID>/entitlements/dry-run?service_level=
		// calling candlepin API with a service_level=null is also a special case:   /consumers/<UUID>/entitlements/dry-run
		// both of these cases will default to use the service_level that is already set on the consumer, however a call to subscribe --auto --service_level="" is not the same as calling the candlepin dry-run API with service_level="" since the CLI will actually UNSET the consumer's current service level (as requested by bug 1001169)
		// [root@jsefler-6 ~]#  curl --stderr /dev/null --insecure --user testuser1:password --request GET https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/consumers/6474c913-4c2f-4283-bcf5-2fc2c44da3ef/entitlements/dry-run?service_level= | python -m simplejson/tool
		if ("".equals(serviceLevel)) {
			//log.warning("When testing dry-run with an empty string for service level, the jsonPools returned should match the service-level that the consumer object already has (unless the service-level granted is exempt).  This is different than calling subscription-manager subscribe --auto --service-level=\"\".");
			log.warning("When testing dry-run with an empty string for service level, the jsonPools returned should match the service-level that the consumer object already has (unless the service-level granted is exempt or null).  This is different than calling subscription-manager subscribe --auto --service-level=\"\".");
			if (!"".equals(initialServiceLevel)) {
				for (int i = 0; i < jsonDryrunResults.length(); i++) {
					// jsonDryrunResults is an array of two values per entry: "pool" and "quantity"
					JSONObject jsonPool = ((JSONObject) jsonDryrunResults.get(i)).getJSONObject("pool");
					Integer quantity = ((JSONObject) jsonDryrunResults.get(i)).getInt("quantity");
					String supportLevelExemptValue = CandlepinTasks.getPoolProductAttributeValue(jsonPool, "support_level_exempt");
					
					// assert that all of the pools proposed provide the consumer's initial service level
					String poolId = jsonPool.getString("id");
					SubscriptionPool dryrunSubscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", poolId, initialAvailableSubscriptionPools);
					
					// check for an exempt service level
					if (supportLevelExemptValue==null || !Boolean.valueOf(supportLevelExemptValue)) {
						// when the support_level_exempt value is absent or true, then either...
						if (dryrunSubscriptionPool.serviceLevel==null || dryrunSubscriptionPool.serviceLevel.isEmpty()) {	// case 1: the serviceLevel from the pool must be null or "" due to changes from Bug 1223560 candlepin commit 9cefb6e23baefcc4ee2e14423f205edd37eecf22; or...
							log.info("When dry-run is called with an empty service-level, an entitlement from a pool with no support_level '"+dryrunSubscriptionPool.serviceLevel+"' was granted from the dry-run pool result: "+dryrunSubscriptionPool+". (Note: This was newly possible by Bug 1223560).");
						} else {	// case 2: the serviceLevel from the pool must match the consumer's initial support_level preference
							Assert.assertTrue(dryrunSubscriptionPool.serviceLevel.equalsIgnoreCase(initialServiceLevel), "When dry-run is called with an empty service-level, the actual consumer's initially set service-level '"+initialServiceLevel+"' matches the service-level '"+dryrunSubscriptionPool.serviceLevel+"' granted from the dry-run pool result: "+dryrunSubscriptionPool+". (EXCEPTION: This is not true when the service-level is exempt.)");
						}
					} else {
						log.info("An exempt service level '"+dryrunSubscriptionPool.serviceLevel+"' was included in the dry-run pool result: "+dryrunSubscriptionPool);
					}
				}
			}
			log.info("Skipping the remaining assertions in this test when the service-level is empty.");
			return;
		}
			
		// assert that one entitlement was granted per dry-run pool result
		//Assert.assertEquals(newlyGrantedEntitlementCerts.size(), jsonDryrunResults.length(),"The autosubscribe results granted the same number of entitlements as the dry-run pools returned.");
		/* Update after Bug 1223560: this is not a valid assertion because one of the newly granted entitlement could
		 * actually be a replacement for an original... e.g. a modifier entitlement might deleted and replaced by a new
		 * one since the modifyee was added.  Therefore it is better to assert that the TOTAL new ents was increased by
		 * the dryrun length.
		 */
		Assert.assertEquals(currentlyGrantedEntitlementCerts.size(), jsonDryrunResults.length()+initialEntitlementCerts.size(), "The total number of entitlement after autosubscribe increased by the number of entitlements returned from the dry-run pools.");
		
		// assert that the newly granted entitlements were actually granted from the dry-run pools
		//for (SubscriptionPool dryrunSubscriptionPool : dryrunSubscriptionPools) {
		for (int i = 0; i < jsonDryrunResults.length(); i++) {
			// jsonDryrunResults is an array of two values per entry: "pool" and "quantity"
			JSONObject jsonPool = ((JSONObject) jsonDryrunResults.get(i)).getJSONObject("pool");
			Integer quantity = ((JSONObject) jsonDryrunResults.get(i)).getInt("quantity");
			
			String poolId = jsonPool.getString("id");
			SubscriptionPool dryrunSubscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", poolId, initialAvailableSubscriptionPools);
			String supportLevelExemptValue = CandlepinTasks.getPoolProductAttributeValue(jsonPool, "support_level_exempt");

			EntitlementCert entitlementCert = clienttasks.getEntitlementCertCorrespondingToSubscribedPool(dryrunSubscriptionPool);
			if (entitlementCert==null) {	// can occur when there are multiple available pools that provide coverage for the same installed product
				log.warning("After actually running auto-subscribe, the predicted dry-run pool '"+dryrunSubscriptionPool.poolId+"' was NOT among the attached subscriptions.  This is probably because there is another available pool that also provides the same provided products '"+dryrunSubscriptionPool.provides+"' (at least one of which is installed) which was granted instead of the dry-run pool.");
				// assert that the warning statement is true
				// the follow assertion may expectedly fail when the provided products exceeds one.
				//ProductSubscription consumedProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("provides", dryrunSubscriptionPool.provides, currentlyConsumedProductSubscriptions);	// THIS IS NOT SMART ENOUGH TO COMPARE THE provides List FOR EQUALITY, INSTEAD SEARCH FOR ANOTHER POOL BY THE SAME SKU
				//Assert.assertNotNull(consumedProductSubscription, "Found a consumed Product Subscription that provides the same products corresponding to dry-run pool: "+dryrunSubscriptionPool+"  (IF THIS FAILS, SEE WARNING ABOVE FOR PROBABLE EXPLANATION)");
				ProductSubscription consumedProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productId", dryrunSubscriptionPool.productId, currentlyConsumedProductSubscriptions);
				Assert.assertNotNull(consumedProductSubscription, "Found a consumed Product Subscription for the same SKU corresponding to dry-run pool: "+dryrunSubscriptionPool+"  (IF THIS FAILS, SEE WARNING ABOVE FOR PROBABLE EXPLANATION)");
				Assert.assertEquals(Integer.valueOf(consumedProductSubscription.quantityUsed), quantity, "The actual entitlement quantityUsed matches the dry-run quantity results for pool :"+dryrunSubscriptionPool);
			} else {
				Assert.assertNotNull(entitlementCert, "Found an entitlement cert corresponding to dry-run pool: "+dryrunSubscriptionPool);
				Assert.assertTrue(newlyGrantedEntitlementCerts.contains(entitlementCert),"This entitlement cert is among the newly granted entitlement from the autosubscribe.");
				Assert.assertEquals(Integer.valueOf(entitlementCert.orderNamespace.quantityUsed), quantity, "The actual entitlement quantityUsed matches the dry-run quantity results for pool :"+dryrunSubscriptionPool);
			}
		}
		
		
		// for the sake of variability, let's unsubscribe from a randomly consumed subscription
		unsubscribeRandomly();
		//clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
	}
	private boolean firstcalltoCandlepinConsumerEntitlementsDryrunWithServiceLevel_Test = true;




	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19981", "RHEL7-33098"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: subscribe using various good and bad values for the --quantity option",
			groups={"Tier1Tests","blockedByBug-962520"},
			dataProvider="getSubscribeWithQuantityData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testSubscribeWithQuantity(Object meta, SubscriptionPool pool, String quantity, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex) {
		log.info("Testing subscription-manager subscribe using various good and bad values for the --quantity option.");
		if(pool==null) throw new SkipException(expectedStderrRegex);	// special case in the dataProvider to identify when a test pool was not available; expectedStderrRegex contains a message for what kind of test pool was being searched for.
	
		// start fresh by returning all entitlements
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		
		// for debugging purposes, list what is currently available so we can see the available quantity before we attempt to attach
		clienttasks.list_(null, true, null, null, null, null, null, null, null, null, null, null, null, null);
		
		// subscribe with quantity
		SSHCommandResult sshCommandResult = clienttasks.subscribe_(null,null,pool.poolId,null,null,quantity,null,null,null, null, null, null, null);
		
		// assert the sshCommandResult here
		if (expectedExitCode!=null) Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode,"ExitCode after subscribe with quantity=\""+quantity+"\" option:");
		if (expectedStdoutRegex!=null) Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), expectedStdoutRegex,"Stdout after subscribe with --quantity=\""+quantity+"\" option:");
		if (expectedStderrRegex!=null) Assert.assertContainsMatch(sshCommandResult.getStderr().trim(), expectedStderrRegex,"Stderr after subscribe with --quantity=\""+quantity+"\" option:");
		
		// when successful, assert that the quantity is correctly reported in the list of consumed subscriptions
		List<ProductSubscription> subscriptionsConsumed = client1tasks.getCurrentlyConsumedProductSubscriptions();
		List<EntitlementCert> entitlementCerts = client1tasks.getCurrentEntitlementCerts();
		if (expectedExitCode==0 && expectedStdoutRegex!=null && expectedStdoutRegex.contains("Successful")) {
			Assert.assertEquals(entitlementCerts.size(), 1, "One EntitlementCert should have been downloaded to "+client1tasks.hostname+" when the attempt to subscribe is successful.");
			Assert.assertEquals(entitlementCerts.get(0).orderNamespace.quantityUsed, quantity.replaceFirst("^\\+",""), "The quantityUsed in the OrderNamespace of the downloaded EntitlementCert should match the quantity requested when we subscribed to pool '"+pool.poolId+"'.  OrderNamespace: "+entitlementCerts.get(0).orderNamespace);
			for (ProductSubscription productSubscription : subscriptionsConsumed) {
				Assert.assertEquals(productSubscription.quantityUsed, Integer.valueOf(quantity.replaceFirst("^\\+","")), "The quantityUsed reported in each consumed ProductSubscription should match the quantity requested when we subscribed to pool '"+pool.poolId+"'.  ProductSubscription: "+productSubscription);
			}
		} else {
			Assert.assertEquals(subscriptionsConsumed.size(), 0, "No subscriptions should be consumed when the attempt to subscribe is not successful.");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36578", "RHEL7-51379"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subscribe using --quantity option and assert the available quantity is properly decremented/incremeneted as multiple consumers subscribe/unsubscribe.",
			groups={"Tier2Tests","blockedByBug-979492"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testMultiConsumerSubscribeWithQuantity() throws NumberFormatException, JSONException, Exception {
		
		// start by calling SubscribeWithQuantity_Test with the row from the dataProvider where quantity=2
		SubscriptionPool consumer1Pool = null;
		int consumer1Quantity=0;
		int totalPoolQuantity=0;
		for (List<Object> row : getSubscribeWithQuantityDataAsListOfLists()) {
			// find the row where quantity.equals("2")
			if (((String)(row.get(2))!=null) && ((String)(row.get(4))!=null)) {
				if (((String)(row.get(2))).equals("2") && ((String)(row.get(4))).startsWith("^Successful")) {
					consumer1Pool = (SubscriptionPool) row.get(1);
					totalPoolQuantity = Integer.valueOf(consumer1Pool.quantity);
					consumer1Quantity = Integer.valueOf((String) row.get(2));
					testSubscribeWithQuantity(row.get(0), (SubscriptionPool)row.get(1), (String)row.get(2), (Integer)row.get(3), (String)row.get(4), (String)row.get(5));
					break;
				}
			}
		}
		if (consumer1Pool==null) Assert.fail("Failed to initiate the first consumer for this test.");
		
		// remember the current consumerId
		String consumer1Id = clienttasks.getCurrentConsumerId(); systemConsumerIds.add(consumer1Id);
		
		// clean the client and register a second consumer
		clienttasks.clean();
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, "SubscriptionQuantityConsumer2", null, null, null, null, (String)null, null, null, null, false, false, null, null, null, null);
		
		// remember the second consumerId
		String consumer2Id = clienttasks.getCurrentConsumerId(); systemConsumerIds.add(consumer2Id);
		
		// find the pool among the available pools
		SubscriptionPool consumer2Pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", consumer1Pool.poolId, clienttasks.getCurrentlyAvailableSubscriptionPools()); 
		Assert.assertNotNull(consumer2Pool,"Consumer2 found the same pool from which consumer1 subscribed a quantity of "+consumer1Quantity);

		// assert that the quantity available to consumer2 is correct
		int consumer2Quantity = totalPoolQuantity-consumer1Quantity;
		Assert.assertEquals(consumer2Pool.quantity, String.valueOf(consumer2Quantity),"The pool quantity available to consumer2 has been decremented by the quantity consumer1 consumed.");
		
		// assert that consumer2 can NOT oversubscribe
		Assert.assertTrue(!clienttasks.subscribe(null,null,consumer2Pool.poolId,null,null,String.valueOf(consumer2Quantity+1),null,null,null, null, null, null, null).getStdout().startsWith("Success"),"An attempt by consumer2 to oversubscribe using the remaining pool quantity+1 should NOT succeed.");

		// assert that consumer2 can successfully consume all the remaining pool quantity
		Assert.assertTrue(clienttasks.subscribe(null,null,consumer2Pool.poolId,null,null,String.valueOf(consumer2Quantity),null,null,null, null, null, null, null).getStdout().startsWith("Success"),"An attempt by consumer2 to exactly consume the remaining pool quantity should succeed.");
		
		// start rolling back the subscribes
		clienttasks.clean();
		
		// restore consumer1, unsubscribe, and assert remaining quantities
		clienttasks.register(sm_clientUsername, sm_clientPassword, null, null, null, null, consumer1Id, null, null, null, (String)null, null, null, null, false, false, null, null, null, null);
		Assert.assertNull(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", consumer1Pool.poolId, clienttasks.getCurrentlyAvailableSubscriptionPools()),"SubscriptionPool '"+consumer1Pool.poolId+"' should NOT be available (because consumer1 is already subscribed to it).");
		clienttasks.unsubscribe(null,clienttasks.getCurrentlyConsumedProductSubscriptions().get(0).serialNumber,null,null,null, null, null);
		consumer1Pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", consumer1Pool.poolId, clienttasks.getCurrentlyAvailableSubscriptionPools()); 
		Assert.assertEquals(consumer1Pool.quantity, String.valueOf(totalPoolQuantity-consumer2Quantity),"The pool quantity available to consumer1 has incremented by the quantity consumer1 consumed.");
		clienttasks.unregister(null,null,null, null);
		
		// restore consumer2, unsubscribe, and assert remaining quantities
		clienttasks.register(sm_clientUsername, sm_clientPassword, null, null, null, null, consumer2Id, null, null, null, (String)null, null, null, null, false, false, null, null, null, null);
		consumer2Pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", consumer2Pool.poolId, clienttasks.getCurrentlyAvailableSubscriptionPools());
		//Assert.assertNull(consumer2Pool,"SubscriptionPool '"+consumer2Pool.poolId+"' should NOT be available (because consumer2 is already subscribed to it).");
		Assert.assertNotNull(consumer2Pool,"SubscriptionPool '"+consumer2Pool.poolId+"' should be available even though consumer2 is already subscribed to it because it is multi-entitleable.");
		Assert.assertEquals(consumer2Pool.quantity, String.valueOf(totalPoolQuantity-consumer2Quantity),"The pool quantity available to consumer2 is still decremented by the quantity consumer2 consumed.");
		clienttasks.unsubscribe(null,clienttasks.getCurrentlyConsumedProductSubscriptions().get(0).serialNumber,null,null,null, null, null);
		consumer2Pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", consumer2Pool.poolId, clienttasks.getCurrentlyAvailableSubscriptionPools()); 
		Assert.assertEquals(consumer2Pool.quantity, String.valueOf(totalPoolQuantity),"The pool quantity available to consumer2 has been restored to its original total quantity");
		clienttasks.unregister(null,null,null, null);
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36585", "RHEL7-51388"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subscribe to multiple pools using --quantity that exceeds some pools and is under other pools.",
			groups={"Tier2Tests","blockedByBug-722975"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testSubscribeWithQuantityToMultiplePools() throws JSONException, Exception {
		
		// is this system virtual
		boolean isSystemVirtual = Boolean.valueOf(clienttasks.getFactValue("virt.is_guest"));
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		// get all the available pools
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		List<String> poolIds = new ArrayList<String>();
		
		// find the poolIds and their quantities
		List<Integer> quantities = new ArrayList<Integer>();
		for (SubscriptionPool pool : pools) {
			poolIds.add(pool.poolId);
			try {Integer.valueOf(pool.quantity);} catch (NumberFormatException e) {continue;}	// ignore  "unlimited" pools
			quantities.add(Integer.valueOf(pool.quantity));
		}
		Collections.sort(quantities);
		int quantity = quantities.get(quantities.size()/2);	// choose the median as the quantity to subscribe with
		
		// collectively subscribe to all pools with --quantity
		SSHCommandResult subscribeResult = clienttasks.subscribe_(null, null, poolIds, null, null, String.valueOf(quantity), null, null, null, null, null, null, null);
		
		/*
		Multi-entitlement not supported for pool with id '8a90f8c6320e9a4401320e9be0e20480'.
		Successfully subscribed the system to Pool 8a90f8c6320e9a4401320e9be196049e
		No free entitlements are available for the pool with id '8a90f8c6320e9a4401320e9be1d404a8'.
		Multi-entitlement not supported for pool with id '8a90f8c6320e9a4401320e9be24004be'.
		Successfully subscribed the system to Pool 8a90f8c6320e9a4401320e9be2e304dd
		No free entitlements are available for the pool with id '8a90f8c6320e9a4401320e9be30c04e8'.
		Multi-entitlement not supported for pool with id '8a90f8c6320e9a4401320e9be3b80505'.
		Multi-entitlement not supported for pool with id '8a90f8c6320e9a4401320e9be4660520'.
		Pool is restricted to physical systems: '8a9086d34470376901447038624d0f87'.
		Pool is restricted to virtual guests: '8a9086d344549b0c0144549bf9ae0dd4'.
		*/
		
		// assert that the expected pools were subscribed to based on quantity
		Assert.assertEquals(subscribeResult.getExitCode(), Integer.valueOf(0), "The exit code from the subscribe command indicates a success.");
		String expectedSubscribeResultStdoutSubString = null;
		for (SubscriptionPool pool : pools) {
			if (quantity>1 && !CandlepinTasks.isPoolProductMultiEntitlement(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId)) {
				expectedSubscribeResultStdoutSubString = String.format("Multi-entitlement not supported for pool with id '%s'.",pool.poolId);
				if (!clienttasks.workaroundForBug876764(sm_serverType)) expectedSubscribeResultStdoutSubString = String.format("Multi-entitlement not supported for pool with ID '%s'.",pool.poolId);
				if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
					expectedSubscribeResultStdoutSubString = String.format("Multi-entitlement not supported for pool with ID \"%s\".",pool.poolId);
				}
				Assert.assertTrue(subscribeResult.getStdout().contains(expectedSubscribeResultStdoutSubString),"Subscribe attempt to non-multi-entitlement pool '"+pool.poolId+"' was NOT successful when subscribing with --quantity greater than one.");				
			} else if (isSystemVirtual && CandlepinTasks.isPoolRestrictedToPhysicalSystems(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId)) {	// Note: the "Multi-entitlement not supported" restriction is thrown before "Pool is restricted to physical systems"
				expectedSubscribeResultStdoutSubString = String.format("Pool is restricted to physical systems: '%s'.",pool.poolId);
				if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
					expectedSubscribeResultStdoutSubString = String.format("Pool is restricted to physical systems: \"%s\".",pool.poolId);
				}
				Assert.assertTrue(subscribeResult.getStdout().contains(expectedSubscribeResultStdoutSubString),"Subscribe attempt to physical_only pool '"+pool.poolId+"' was NOT successful when system is virtual.");				
			} else if (!isSystemVirtual && CandlepinTasks.isPoolRestrictedToVirtualSystems(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId)) {	// Note: the "Multi-entitlement not supported" restriction is thrown before "Pool is restricted to virtual systems"
				expectedSubscribeResultStdoutSubString = String.format("Pool is restricted to virtual guests: '%s'.",pool.poolId);
				if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
					expectedSubscribeResultStdoutSubString = String.format("Pool is restricted to virtual guests: \"%s\".",pool.poolId);
				}
				Assert.assertTrue(subscribeResult.getStdout().contains(expectedSubscribeResultStdoutSubString),"Subscribe attempt to virt_only pool '"+pool.poolId+"' was NOT successful when system is physical.");				
			} else if (pool.quantity.equalsIgnoreCase("unlimited") || quantity <= Integer.valueOf(pool.quantity)) {
				//Assert.assertTrue(subscribeResult.getStdout().contains(String.format("Successfully consumed a subscription from the pool with id %s.",pool.poolId)),"Subscribe to pool '"+pool.poolId+"' was successful when subscribing with --quantity less than or equal to the pool's availability.");	// Bug 812410 - Subscription-manager subscribe CLI feedback 
				//Assert.assertTrue(subscribeResult.getStdout().contains(String.format("Successfully consumed a subscription for: %s",pool.subscriptionName)),"Subscribe to pool '"+pool.poolId+"' was successful when subscribing with --quantity less than or equal to the pool's availability.");	// changed by Bug 874804 Subscribe -> Attach
				Assert.assertTrue(subscribeResult.getStdout().contains(String.format("Successfully attached a subscription for: %s",pool.subscriptionName)),"Subscribe to pool '"+pool.poolId+"' was successful when subscribing with --quantity less than or equal to the pool's availability.");
			} else {
				expectedSubscribeResultStdoutSubString = String.format("No entitlements are available from the pool with id '%s'.",pool.poolId);	// expected string changed by bug 876758
				expectedSubscribeResultStdoutSubString = String.format("No subscriptions are available from the pool with id '%s'.",pool.poolId);
				if (!clienttasks.workaroundForBug876764(sm_serverType)) expectedSubscribeResultStdoutSubString = String.format("No subscriptions are available from the pool with ID '%s'.",pool.poolId);
				if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
					expectedSubscribeResultStdoutSubString = String.format("No subscriptions are available from the pool with ID \"%s\".",pool.poolId);
				}
				Assert.assertTrue(subscribeResult.getStdout().contains(expectedSubscribeResultStdoutSubString),"Subscribe to pool '"+pool.poolId+"' was NOT successful when subscribing with --quantity greater than the pool's availability.");
			}
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27122", "RHEL7-51945"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subscribe to future subscription pool",
			groups={"Tier2Tests","blockedByBug-979492","blockedByBug-1440180"},
			//dataProvider="getAllFutureSystemSubscriptionPoolsData",	// 06/04/2014 takes too long; rarely reveals a bug
			dataProvider="getRandomSubsetOfFutureSystemSubscriptionPoolsData",
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testSubscribeToFutureSubscriptionPool(SubscriptionPool pool) throws Exception {
//if (!pool.productId.equals("awesomeos-virt-unlmtd-phys")) throw new SkipException("debugTesting pool productId="+pool.productId);
		
		Calendar now = new GregorianCalendar();
		now.setTimeInMillis(System.currentTimeMillis());
		
		// subscribe to the future subscription pool
		SSHCommandResult subscribeResult = clienttasks.subscribe(null,null,pool.poolId,null,null,null,null,null,null,null, null, null, null);
		// Pool is restricted to virtual guests: '8a90f85734205a010134205ae8d80403'.
		// Pool is restricted to physical systems: '8a9086d3443c043501443c052aec1298'.
		if (subscribeResult.getStdout().startsWith("Pool is restricted")) {
			throw new SkipException("Subscribing to this future subscription is not applicable to this test: "+pool);
		}

		// assert that the granted entitlement cert begins in the future
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertCorrespondingToSubscribedPool(pool);
		Assert.assertNotNull(entitlementCert,"Found the newly granted EntitlementCert on the client after subscribing to future subscription pool '"+pool.poolId+"'.");
		Assert.assertTrue(entitlementCert.validityNotBefore.after(now), "The newly granted EntitlementCert is not valid until the future.  EntitlementCert: "+entitlementCert);
		Assert.assertTrue(entitlementCert.orderNamespace.startDate.after(now), "The newly granted EntitlementCert's OrderNamespace starts in the future.  OrderNamespace: "+entitlementCert.orderNamespace);	
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36586", "RHEL7-51389"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subscribe and attach can be used interchangably",
			groups={"Tier2Tests","blockedByBug-874804","blockedByBug-981689"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testAttachDeprecatedSubscribe() throws Exception {
		SSHCommandResult result = client.runCommandAndWait(clienttasks.command+" --help");
		Assert.assertContainsMatch(result.getStdout(), "^\\s*subscribe\\s+Deprecated, see attach$");
		
		SSHCommandResult subscribeResult;
		SSHCommandResult attachResult;
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		subscribeResult = client.runCommandAndWait(clienttasks.command+" subscribe --pool=123");
		attachResult = client.runCommandAndWait(clienttasks.command+" attach --pool=123");
		Assert.assertEquals(subscribeResult.toString(), attachResult.toString(), "Results from 'subscribe' and 'attach' module commands should be identical.");
		clienttasks.unregister(null,null,null, null);
		subscribeResult = client.runCommandAndWait(clienttasks.command+" subscribe --pool=123");
		attachResult = client.runCommandAndWait(clienttasks.command+" attach --pool=123");
		Assert.assertEquals(subscribeResult.toString(), attachResult.toString(), "Results from 'subscribe' and 'attach' module commands should be identical.");
	}
	
	
	
	@BeforeGroups(groups={"setup"}, value={"VerifyOlderClientsAreDeniedEntitlementsToRamAndCoresBasedSubscriptions_Test"})
	public void createFactsFileWithOverridingValues() {
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("system.certificate_version", "1.0");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27121", "RHEL7-51386"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Make sure that older subscription-managers are denied attempts to attach a subscription based on: ram, cores",
			groups={"Tier2Tests","VerifyOlderClientsAreDeniedEntitlementsToRamAndCoresBasedSubscriptions_Test","blockedByBug-957218" },
			dataProvider="getAllAvailableRamCoresSubscriptionPoolsData",
			enabled=true)	// TODO THIS TEST IS A CANDIDATE FOR DISABLEMENT AFTER IMPLEMENTATION OF BUG 957218
	//@ImplementsNitrateTest(caseId=)
	public void testOlderClientsAreDeniedEntitlementsToRamAndCoresBasedSubscriptions(Object bugzilla, SubscriptionPool pool) throws JSONException, Exception {
		if (true) {
			log.warning("Effectively, this test is now obsolete due to RFE Bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=888866 which reversed the original intent of this test.");
			String systemCertificateVersion = clienttasks.getFactValue("system.certificate_version");
			EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.subscribeToSubscriptionPool(pool,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl));
			Assert.assertEquals(entitlementCert.version, "1.0", "RAM and Core based subscriptions are now granted to older subscription-manager clients regardless of version.  See RFE bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=888866");
			Assert.assertNull(entitlementCert.orderNamespace.coreLimit, "Core limit included in an entitlement cert when system.certificate_version is old '"+systemCertificateVersion+"'.");
			Assert.assertNull(entitlementCert.orderNamespace.ramLimit, "RAM limit included in an entitlement cert when system.certificate_version is old '"+systemCertificateVersion+"'.");
			return;
		}
		
		/*
		The way that this works is that all attributes that are specified on a
		pool are version checked. Here are the current versions:
		ram: 3.1
		cores: 3.2
		sockets: 1.0

		If a pool has a ram attribute, the minimum required version will be 3.1.
		If a pool has a cores attribute, the minimum required version will be 3.2
		if a pool has cores AND ram attributes, the minimum required version
		will be 3.2

		Again, each attribute on the pool will be checked against the above
		versions, and the largest found will be the minimum required version
		that the client must support in order to attach that sub.

		Supported versions do not change based on stacking... it changes when
		the certificate content changes. i.e when the cores attribute was added
		to the cert. It has no relation to what our rules support.

		So both examples above are correct since the system is 1.0. They are
		dealing with RAM subscriptions, so they should be restricted to clients
		supporting certificate versions >= 3.1

		If the test subs were to include cores, it would require 3.2

		Clear as mud!?

		Ping me if you have more questions about this.

		--mstead
		*/

		//	[root@jsefler-5 ~]# subscription-manager subscribe --pool=8a90f8313e472bce013e472d22150352
		//	The client must support at least v3.1 certificates in order to use subscription: Multi-Attribute (non-stackable) (24 cores, 6 sockets, 8GB RAM). A newer client may be available to address this problem.
		//	[root@jsefler-5 ~]# 
		
		String systemCertificateVersion = clienttasks.getFactValue("system.certificate_version");
		SSHCommandResult subscribeResult = clienttasks.subscribe_(null, null, pool.poolId, null, null, null, null, null, null, null, null, null, null);
		Assert.assertEquals(subscribeResult.getExitCode(), new Integer(255), "Exitcode from an attempt to subscribe to '"+pool.subscriptionName+"' when system.certificate_version is old '"+systemCertificateVersion+"'.");
		
		// CORES-based subscriptions
		if (CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "cores")!=null) {
			Assert.assertEquals(subscribeResult.getStderr().trim(), String.format("The client must support at least v%s certificates in order to use subscription: %s. A newer client may be available to address this problem.","3.2",pool.subscriptionName),
					"Stderr from an attempt to subscribe to '"+pool.subscriptionName+"' a CORES-based subscription when system.certificate_version is < 3.2");
			Assert.assertEquals(subscribeResult.getStdout().trim(), "",
					"Stdout from an attempt to subscribe to '"+pool.subscriptionName+"' a CORES-based subscription when system.certificate_version is < 3.2");
			return;
		}
		
		// stackable RAM-based subscriptions
		/* I assumed this block was a valid test.  I was wrong.  See mstead's notes above...
		if (CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "ram")!=null &&
			CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "stacking_id")!=null) {
			Assert.assertEquals(subscribeResult.getStderr().trim(), String.format("The client must support at least v%s certificates in order to use subscription: %s. A newer client may be available to address this problem.","3.2",pool.subscriptionName),
					"Stderr from an attempt to subscribe to '"+pool.subscriptionName+"' a RAM-based subscription when system.certificate_version is < 3.2");
			Assert.assertEquals(subscribeResult.getStdout().trim(), "",
					"Stdout from an attempt to subscribe to '"+pool.subscriptionName+"' a RAM-based subscription when system.certificate_version is < 3.2");
			return;
		}
		*/

		
		// RAM-based subscriptions
		if (CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "ram")!=null) {
			Assert.assertEquals(subscribeResult.getStderr().trim(), String.format("The client must support at least v%s certificates in order to use subscription: %s. A newer client may be available to address this problem.","3.1",pool.subscriptionName),
					"Stderr from an attempt to subscribe to '"+pool.subscriptionName+"' a RAM-based subscription when system.certificate_version is < 3.1");
			Assert.assertEquals(subscribeResult.getStdout().trim(), "",
					"Stdout from an attempt to subscribe to '"+pool.subscriptionName+"' a RAM-based subscription when system.certificate_version is < 3.1");
			return;
		}
		
		Assert.fail("Do not know how to assert the attempted attachment of '"+pool.subscriptionName+"'.");
	}
	@AfterGroups(groups={"setup"}, value={"VerifyOlderClientsAreDeniedEntitlementsToRamAndCoresBasedSubscriptions_Test","AutoSubscribeOnVirtualSystemsFavorVirtualPools_Test"})
	@AfterClass(groups={"setup"})	// insurance; not really needed
	public void deleteFactsFileWithOverridingValues() {
		clienttasks.deleteFactsFileWithOverridingValues();
	}
	@DataProvider(name="getAllAvailableRamCoresSubscriptionPoolsData")
	public Object[][] getAllAvailableRamCoresSubscriptionPoolsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAllAvailableRamCoresSubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>>getAllAvailableRamCoresSubscriptionPoolsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		for (List<Object> list : getAllAvailableSubscriptionPoolsDataAsListOfLists()) {
			SubscriptionPool pool = (SubscriptionPool) list.get(0);
			
			// include RAM-based subscriptions
			if (CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "ram")!=null) {
				ll.add(Arrays.asList(new Object[] {null,	pool}));
				continue;
			}
			
			// include CORES-based subscriptions
			if (CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "cores")!=null) {
				ll.add(Arrays.asList(new Object[] {null,	pool}));
				continue;
			}
			
		}
		
		return ll;
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36577", "RHEL7-51378"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: autosubscribe on a virtual system should favor virtual pools",
			groups={"Tier2Tests","blockedByBug-927101","AutoSubscribeOnVirtualSystemsFavorVirtualPools_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testAutoSubscribeOnVirtualSystemsFavorVirtualPools() throws JSONException, Exception {
		
		// force the system to be virtual
		Map<String, String> factsMap = new HashMap<String, String>();
		factsMap.put("virt.is_guest", String.valueOf(Boolean.TRUE));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		
		// get the current product certs
		List<ProductCert> productCerts = clienttasks.getCurrentProductCerts();
		if (rhsmProductCertDir==null) {
			rhsmProductCertDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "productCertDir");
		}
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		// get the available pools
		List<SubscriptionPool> availablePools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		
		// find available subscriptions for which both a Virtual and a Physical pool is available
		boolean testAttempted = false;
		for (SubscriptionPool virtualPool : SubscriptionPool.findAllInstancesWithMatchingFieldFromList("machineType", "Virtual", availablePools)) {
			for (SubscriptionPool physicalPool : SubscriptionPool.findAllInstancesWithMatchingFieldFromList("machineType", "Physical", availablePools)) {
				if (virtualPool.productId.equals(physicalPool.productId)) {
					// found a pair of subscriptions that we can test
					
					// get the product certs that are provided by these pools
					List<String> virtualProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, virtualPool.poolId);
					List<String> physicalProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, physicalPool.poolId);
					// TEMPORARY WORKAROUND
					if (virtualPool.subscriptionType.contains("(Temporary)") && virtualProductIds.isEmpty() && !physicalProductIds.isEmpty()) {
						boolean invokeWorkaroundWhileBugIsOpen = true;
						String bugId="1394401"; // Bug 1394401 - The list of provided products for Temporary Subscriptions is empty
						try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
						if (invokeWorkaroundWhileBugIsOpen) {
							log.warning("Virtual pool '"+virtualPool.poolId+"' and and Physical pool '"+physicalPool.poolId+"' are not good candidates for this test while bug '"+bugId+"' is open.  Looking for another pair.");
							continue;
						}
					}
					// END OF WORKAROUND		
					Assert.assertTrue(virtualProductIds.containsAll(physicalProductIds)&&physicalProductIds.containsAll(virtualProductIds), "Provided product ids from virtual pool '"+virtualPool.poolId+"' and physical pool '"+physicalPool.poolId+"' sharing a common productId '"+virtualPool.productId+"' should be the same.");
					
					// configure a temporary product cert directory containing only these provided product certs
					RemoteFileTasks.runCommandAndAssert(client,"mkdir -p "+tmpProductCertDir,Integer.valueOf(0));
					RemoteFileTasks.runCommandAndAssert(client,"rm -f "+tmpProductCertDir+"/*.pem",Integer.valueOf(0));
					List<String> productCertIdsFound = new ArrayList<String>();
					List<String> productCertNamesFound = new ArrayList<String>();
					for (ProductCert productCert : productCerts) {
						if (virtualProductIds.contains(productCert.productId)) {
							RemoteFileTasks.runCommandAndAssert(client,"cp "+productCert.file+" "+tmpProductCertDir,Integer.valueOf(0));
							productCertIdsFound.add(productCert.productId);
							productCertNamesFound.add(productCert.productName);
						}
					}
					clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", tmpProductCertDir);
					
					if (!productCertIdsFound.isEmpty()) {
						
						// start testing... autosubscribe and assert the consumed subscriptions are Virtual
						clienttasks.unsubscribe(true, (BigInteger)null, null, null, null, null, null);
						clienttasks.subscribe(true, null, null, (String)null, null, null, null, null, null, null, null, null, null);
						for (ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
							Assert.assertEquals(productSubscription.machineType, "Virtual", "Autosubscribing a virtual system should favor granting an entitlement from a Virtual pool that provides "+productCertIdsFound+" over a Physical pool that provides "+productCertIdsFound+"'.");
							Assert.assertTrue(productSubscription.provides.containsAll(productCertNamesFound), "The autosubscribed virtual subscription '"+productSubscription+"' provides for all of the installed products "+productCertNamesFound+".  (Note: This could potentially fail when the provided product names are do not exactly match the installed product cert names which is okay since the productIds are what really matter).");	// TODO We may need to comment this out or fix it if it starts failing due to changes in the subscription data.
							testAttempted = true;
						}
						//testAttempted = true;	// relocated to inside of loop above
						
					} else {
						log.info("Did not find any installed product certs provided by Virtual and Physical subscriptions '"+virtualPool.productId+"'.");
					}
				}
			}
		}
		if (!testAttempted) throw new SkipException("Did not find all the resources needed to attempt this test.");
		
	}
	@AfterGroups(groups="setup", value = {"AutoSubscribeOnVirtualSystemsFavorVirtualPools_Test"})
	public void restoreRhsmProductCertDir() {
		if (clienttasks==null) return;
		if (rhsmProductCertDir==null) return;	
		log.info("Restoring the originally configured product cert directory...");
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", rhsmProductCertDir);
	}
	protected String rhsmProductCertDir = null;
	protected final String tmpProductCertDir = "/tmp/sm-tmpProductCertDir";



	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36583", "RHEL7-51385"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subscribe with --file whose contents are a list of poolids",
			groups={"Tier2Tests","blockedByBug-1159974"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testSubscribeWithFileOfPoolIds() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.13.8-1")) throw new SkipException("The attach --file function was not implemented in this version of subscription-manager.");	// commit 3167333fc3a261de939f4aa0799b4283f2b9f4d2 bug 1159974
		
		Boolean all = false;	//getRandomBoolean();
		Boolean matchInstalled = getRandomBoolean();
		Boolean noOverlap = getRandomBoolean();
		
		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
			clienttasks.autoheal(null, null, true, null, null, null, null);
		} else clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
		
		// call list with --pool-only to get a random list of available poolids
		String poolOnlyListCommand = clienttasks.listCommand(all, true, null, null, null, null, matchInstalled, noOverlap, null, true, null, null, null, null);
		String tmpFile = "/tmp/poolIds.txt";
		//RemoteFileTasks.runCommandAndAssert(client, poolOnlyListCommand+" > "+tmpFile, 0);
		RemoteFileTasks.runCommandAndAssert(client, poolOnlyListCommand+" > "+tmpFile+" && echo abc123 >> "+tmpFile, 0);
		SSHCommandResult poolOnlyListResult = client.runCommandAndWait("cat "+tmpFile);
		
		// convert the result to a list
		List<String> poolIdsFromFile = new ArrayList<String>();
		if (!poolOnlyListResult.getStdout().trim().isEmpty()) poolIdsFromFile.addAll(Arrays.asList(poolOnlyListResult.getStdout().trim().split("\n")));
		poolIdsFromFile.remove("abc123");
		
		// subscribe with the --file option
		SSHCommandResult subscribeWithFileResult = clienttasks.subscribe(null, null, (List<String>) null, (List<String>) null, null, null, null, null, tmpFile, null, null, null, null);
		String expectedRejection = String.format("Pool with id %s could not be found.", "abc123");
		Assert.assertTrue(subscribeWithFileResult.getStdout().trim().endsWith(expectedRejection), "The stdout result from subscribe with a file of poolIds ends with '"+expectedRejection+"'.");

		// assert that all of the currently attached pools equal the poolIdsFromFile
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		List<String> poolIdsAttached = new ArrayList<String>();
		for (ProductSubscription consumedProductSubscription : consumedProductSubscriptions) poolIdsAttached.add(consumedProductSubscription.poolId);
		
		// assert the result
		Assert.assertTrue(poolIdsAttached.containsAll(poolIdsFromFile)&&poolIdsFromFile.containsAll(poolIdsAttached), "The list of pool ids in file '"+tmpFile+"' is equivalent to the list of currently attached pool ids.");
		Assert.assertEquals(poolIdsAttached.size(), poolIdsFromFile.size(),"The number of poolIds currently attached matches the number of pool ids read from the file '"+tmpFile+"'.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36582", "RHEL7-51384"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subscribe with --file whose contents are empty.  No pools should be attached.",
			groups={"Tier2Tests","blockedByBug-1175291"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testSubscribeWithFileOfEmptyPoolIds() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.13.8-1")) throw new SkipException("The attach --file function was not implemented in this version of subscription-manager.");	// commit 3167333fc3a261de939f4aa0799b4283f2b9f4d2 bug 1159974
		
		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
			clienttasks.autoheal(null, null, true, null, null, null, null);
		} else clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
		
		// create an empty file of pool ids
		String tmpFile = "/tmp/emptyFile.txt";
		RemoteFileTasks.runCommandAndAssert(client, "rm -f "+tmpFile+" && touch "+tmpFile, 0);
		
		// subscribe with the --file option
		SSHCommandResult subscribeWithFileResult = clienttasks.subscribe_(null, null, (List<String>) null, (List<String>) null, null, null, null, null, tmpFile, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.13-1")) {	// commit 1ec3ee950642b24e6b55a23db10e447bd0fada4f	// Bug 1175291 - subscription-manager attach --file <file> ,with file being empty attaches subscription for installed product
			String expectedStderr = String.format("Error: The file \"%s\" does not contain any pool IDs.",tmpFile);
			Assert.assertEquals(subscribeWithFileResult.getStdout().trim(), "", "The stdout result from subscribe with an empty file of poolIds is empty.");
			Assert.assertEquals(subscribeWithFileResult.getStderr().trim(), expectedStderr, "The stderr result from subscribe with an empty file of poolIds is empty.");
			Assert.assertEquals(subscribeWithFileResult.getExitCode(), new Integer(65)/*EX_DATAERR*/, "The exitCode from subscribe with an empty file of poolIds.");
		} else {
			Assert.assertTrue(subscribeWithFileResult.getStdout().trim().isEmpty(), "The stdout result from subscribe with an empty file of poolIds is empty.");
			Assert.assertEquals(subscribeWithFileResult.getExitCode(), new Integer(1), "The exitCode from subscribe with an empty file of poolIds.");			
		}
		// assert that no subscriptions have been consumed
		Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().isEmpty(), "No subscriptions should be attached after attempting to attach an empty file of pool ids.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36589", "RHEL7-51393"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subscribe with --file=- which indicates that the pools will be read from stdin",
			groups={"Tier2Tests","blockedByBug-1159974"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testSubscribeWithFileOfPoolIdsFromStdin() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.13.8-1")) throw new SkipException("The attach --file function was not implemented in this version of subscription-manager.");	// commit 3167333fc3a261de939f4aa0799b4283f2b9f4d2 bug 1159974
		
		Boolean all = false;	//getRandomBoolean();
		Boolean matchInstalled = getRandomBoolean();
		Boolean noOverlap = getRandomBoolean();
		
		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
			clienttasks.autoheal(null, null, true, null, null, null, null);
		} else clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
		
		// call list with --pool-only to get a random list of available poolids
		String poolOnlyListCommand = clienttasks.listCommand(all, true, null, null, null, null, matchInstalled, noOverlap, null, true, null, null, null, null);
		String tmpFile = "/tmp/poolIds.txt";
		RemoteFileTasks.runCommandAndAssert(client, poolOnlyListCommand+" > "+tmpFile, 0);
		SSHCommandResult poolOnlyListResult = client.runCommandAndWait("cat "+tmpFile);

//NOT NECESSARY
//		// convert the result to a list
//		List<String> poolIdsFromFile = new ArrayList<String>();
//		if (!poolOnlyListResult.getStdout().trim().isEmpty()) poolIdsFromFile = Arrays.asList(poolOnlyListResult.getStdout().trim().split("\n"));
		
		// subscribe with the --file option (to get our expected results)
		SSHCommandResult subscribeWithFileResult = clienttasks.subscribe(null, null, (List<String>) null, (List<String>) null, null, null, null, null, tmpFile, null, null, null, null);
		
		// return the subscriptions...
		clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
		
		// now let's run the same poolOnlyListCommand and pipe the results to subscription-manager attach --file - (the hyphen indicates stdin)
		String stdinFileSubscribeCommand = clienttasks.subscribeCommand(null, null, (List<String>) null, (List<String>) null, null, null, null, null, "-", null, null, null, null);
		SSHCommandResult stdinFileSubscribeCommandResult = client.runCommandAndWait("cat "+tmpFile+" | "+stdinFileSubscribeCommand, (long) (3/*min*/*60*1000/*timeout*/));
				
		// assert the two subscribe results are identical
		Assert.assertEquals(stdinFileSubscribeCommandResult.getExitCode(), subscribeWithFileResult.getExitCode(), "Exit Code comparison between the expected result of subscribing with a file of poolIds and subscribing with the poolIds piped to stdin.");
		Assert.assertEquals(stdinFileSubscribeCommandResult.getStdout(), subscribeWithFileResult.getStdout(), "Stdout comparison between the expected result of subscribing with a file of poolIds and subscribing with the poolIds piped from stdin.");
		Assert.assertEquals(stdinFileSubscribeCommandResult.getStderr(), subscribeWithFileResult.getStderr(), "Stderr comparison between the expected result of subscribing with a file of poolIds and subscribing with the poolIds piped from stdin.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36581", "RHEL7-51383"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subscribe with --file=- which indicates that the pools will be read from stdin",
			groups={"Tier2Tests","blockedByBug-1159974","blockedByBug-1175291"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testSubscribeWithFileOfEmptyPoolIdsFromStdin() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.13.8-1")) throw new SkipException("The attach --file function was not implemented in this version of subscription-manager.");	// commit 3167333fc3a261de939f4aa0799b4283f2b9f4d2 bug 1159974
		
		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
			clienttasks.autoheal(null, null, true, null, null, null, null);
		} else clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
		
		// now let's run the same poolOnlyListCommand and pipe the results to subscription-manager attach --file - (the hyphen indicates stdin)
		String stdinFileSubscribeCommand = clienttasks.subscribeCommand(null, null, (List<String>) null, (List<String>) null, null, null, null, null, "-", null, null, null, null);
		SSHCommandResult stdinFileSubscribeCommandResult = client.runCommandAndWait("echo \"\" | "+stdinFileSubscribeCommand, (long) (3/*min*/*60*1000/*timeout*/));
				
		// assert the two subscribe results are identical
		// if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.13-1")) {	// commit 1ec3ee950642b24e6b55a23db10e447bd0fada4f	// Bug 1175291 - subscription-manager attach --file <file> ,with file being empty attaches subscription for installed product
		Assert.assertEquals(stdinFileSubscribeCommandResult.getExitCode(), new Integer(65)/*EX_DATAERR*/, "Exit Code from subscribing with a file of empty poolIds from stdin.");
		Assert.assertEquals(stdinFileSubscribeCommandResult.getStdout().trim(), "", "Stdout from subscribing with a file of empty poolIds from stdin.");
		Assert.assertEquals(stdinFileSubscribeCommandResult.getStderr().trim(), "Error: Received data does not contain any pool IDs.", "Stderr from subscribing with a file of empty poolIds from stdin.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36579", "RHEL7-51380"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subscribe with no args should now default to --auto",
			groups={"Tier2Tests"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
			public void testSubscribeDefaultsToAutosubscribe() throws JSONException, Exception {
		// commit cb590a75f3a2de921961808d00ab251180c51691 subscription-manager-1.13.13-1)
		if (clienttasks.isPackageVersion("subscription-manager","<","1.14.1-1")) throw new SkipException("Defaulting subscribe/attach to imply option --auto was not implemented in this version of subscription-manager.");	// commit cb590a75f3a2de921961808d00ab251180c51691 Make 'attach' auto unless otherwise specified

		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
			clienttasks.autoheal(null, null, true, null, null, null, null);
		}

		// first let's run subscribe --auto and collect the results.
		clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
		String subscribeWithAutoCommand = clienttasks.subscribeCommand(true, null, (List<String>) null, (List<String>) null, null, null, null, null, null, null, null, null, null);
		//SSHCommandResult subscribeWithAutoCommandResult = client.runCommandAndWait(subscribeWithAutoCommand);
		SSHCommandResult subscribeWithAutoCommandResult = clienttasks.subscribe(true, null, (String)null, null, null, null, null, null, null, null, null, null, null);
		List<InstalledProduct> subscribeWithAutoCommandResultList = InstalledProduct.parse(subscribeWithAutoCommandResult.getStdout());
		int subscribeWithAutoEntitlementCount = clienttasks.getCurrentEntitlementCertFiles().size();

		// second let's run subscribe without --auto and collect the results.
		clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
		String subscribeWithoutAutoCommand = clienttasks.subscribeCommand(null, null, (List<String>) null, (List<String>) null, null, null, null, null, null, null, null, null, null);
		//SSHCommandResult subscribeWithoutAutoCommandResult = client.runCommandAndWait(subscribeWithoutAutoCommand);
		SSHCommandResult subscribeWithoutAutoCommandResult = clienttasks.subscribe(null, null, (String)null, null, null, null, null, null, null, null, null, null, null);
		List<InstalledProduct> subscribeWithoutAutoCommandResultList = InstalledProduct.parse(subscribeWithoutAutoCommandResult.getStdout());
		int subscribeWithoutAutoEntitlementCount = clienttasks.getCurrentEntitlementCertFiles().size();

		// assert the two subscribe results are identical
		Assert.assertEquals(subscribeWithoutAutoCommandResult.getExitCode(), subscribeWithAutoCommandResult.getExitCode(), "eCode from subscribing without --auto should match exitCode from subscribing with --auto");
		Assert.assertTrue(subscribeWithoutAutoCommandResultList.containsAll(subscribeWithAutoCommandResultList) && subscribeWithAutoCommandResultList.containsAll(subscribeWithoutAutoCommandResultList), "The Installed Product status reported is identical when running the subscribe module with or without --auto option.");
		Assert.assertEquals(subscribeWithoutAutoEntitlementCount,subscribeWithAutoEntitlementCount, "The number of entitlements granted is identical when running the subscribe module with or without --auto option.");
	}
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 668032 - rhsm not logging subscriptions and products properly //done --shwetha https://github.com/RedHatQE/rhsm-qe/issues/198
	// TODO Bug 670831 - Entitlement Start Dates should be the Subscription Start Date //Done --shwetha https://github.com/RedHatQE/rhsm-qe/issues/199
	// TODO Bug 664847 - Autobind logic should respect the architecture attribute //working on https://github.com/RedHatQE/rhsm-qe/issues/200
	// TODO Bug 676377 - rhsm-compliance-icon's status can be a day out of sync - could use dbus-monitor to assert that the dbus message is sent on the expected compliance changing events https://github.com/RedHatQE/rhsm-qe/issues/201
	// TODO Bug 739790 - Product "RHEL Workstation" has a valid stacking_id but its socket_limit is 0 https://github.com/RedHatQE/rhsm-qe/issues/202
	// TODO Bug 707641 - CLI auto-subscribe tries to re-use basic auth credentials. https://github.com/RedHatQE/rhsm-qe/issues/203
	
	// TODO Write an autosubscribe bug... 1. Subscribe to all avail and note the list of installed products (Subscribed, Partially, Not) 
	//									  2. Unsubscribe all  3. Autosubscribe and verfy same installed product status (Subscribed, Not)//done --shwetha https://github.com/RedHatQE/rhsm-qe/issues/204
	// TODO Bug 746035 - autosubscribe should NOT consider existing future entitlements when determining what pools and quantity should be autosubscribed //working on https://github.com/RedHatQE/rhsm-qe/issues/205
	// TODO Bug 747399 - if consumer does not have architecture then we should not check for it https://github.com/RedHatQE/rhsm-qe/issues/206
	// TODO Bug 743704 - autosubscribe ignores socket count on non multi-entitle subscriptions //done --shwetha https://github.com/RedHatQE/rhsm-qe/issues/207
	// TODO Bug 740788 - Getting error with quantity subscribe using subscription-assistance page 
	//                   Write an autosubscribe test that mimics partial subscriptions in https://bugzilla.redhat.com/show_bug.cgi?id=740788#c12 https://github.com/RedHatQE/rhsm-qe/issues/208
	// TODO Bug 720360 - subscription-manager: entitlement key files created with weak permissions // done --shwetha https://github.com/RedHatQE/rhsm-qe/issues/209
	// TODO Bug 772218 - Subscription manager silently rejects pools requested in an incorrect format.//done --shwetha https://github.com/RedHatQE/rhsm-qe/issues/211
	// TODO Bug 878994 - 500 errors in stage on subscribe/unsubscribe - NEED TO INSTALL A PRODUCT CERTS FROM TESTDATA AND MAKE SURE THEY DO NOT TRIP UP THE IT PRODUCT ADAPTERS https://github.com/RedHatQE/rhsm-qe/issues/212

	
	// Configuration Methods ***********************************************************************
	@AfterClass(groups={"setup"})
	public void unregisterAllSystemConsumerIds() throws Exception {
		if (clienttasks!=null) {
			for (String systemConsumerId : systemConsumerIds) {
				/* it is faster to call the candlepin API directly
				clienttasks.register_(sm_clientUsername,sm_clientPassword,null,null,null,null,systemConsumerId, null, null, null, (String)null, null, null, null, Boolean.TRUE, null, null, null, null);
				clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null);
				clienttasks.unregister_(null, null, null);
				*/
				CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl, "/consumers/"+systemConsumerId);
			}
			systemConsumerIds.clear();
		}
	}
	

	
	// Protected Methods ***********************************************************************

	protected List<String> systemConsumerIds = new ArrayList<String>();
	
	protected void unsubscribeRandomly() {
		log.info("Unsubscribing from a random selection of entitlements (for the sake of test variability)...");
		for (EntitlementCert entitlementCert: clienttasks.getCurrentEntitlementCerts()) {
			if (randomGenerator.nextInt(2)==1) {
				clienttasks.unsubscribeFromSerialNumber(entitlementCert.serialNumber);
			}
		}
	}
	
	
	// Data Providers ***********************************************************************

	@DataProvider(name="getInstalledProductCertsData")
	public Object[][] getInstalledProductCertsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getInstalledProductCertsDataAsListOfLists());
	}
	protected List<List<Object>> getInstalledProductCertsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		for (ProductCert productCert: clienttasks.getCurrentProductCerts()) {
			BlockedByBzBug blockedByBzBug = null;
			
			// Bug 951633 - installed product with comma separated arch attribute fails to go green 
			if (productCert.productNamespace.arch.contains(",")) blockedByBzBug = new BlockedByBzBug("951633");
			
			ll.add(Arrays.asList(new Object[]{blockedByBzBug, productCert}));
		}
		
		return ll;
	}
	
	
	
	@DataProvider(name="getCertFrequencyData")
	public Object[][] getCertFrequencyDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getCertFrequencyDataAsListOfLists());
	}
	protected List<List<Object>> getCertFrequencyDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		// int minutes
		ll.add(Arrays.asList(new Object[]{2}));
		// takes too long; one is sufficient ll.add(Arrays.asList(new Object[]{1}));
		
		return ll;
	}
	
	
	@DataProvider(name="getSubscribeWithQuantityData")
	public Object[][] getSubscribeWithQuantityDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getSubscribeWithQuantityDataAsListOfLists());
	}
	protected List<List<Object>>getSubscribeWithQuantityDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, "SubscriptionQuantityConsumer", null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		// find pools with a positive quantity that are "multi-entitlement" (and "instance_multiplier" too)
		SubscriptionPool instanceBasedPool = null;
		SubscriptionPool multiEntitlementPool = null;
		SubscriptionPool standardPool = null;
		for (SubscriptionPool pool : getRandomList(clienttasks.getCurrentlyAvailableSubscriptionPools())) {
///* debugTesting all pools */for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
///* debugTesting unlimited pools */if (!pool.quantity.equalsIgnoreCase("unlimited")) continue;
///* debugTesting only these SKUs*/if (!Arrays.asList(new String[]{"RH00076","RH0604852","RH00284"}).contains(pool.productId)) continue;
			if (instanceBasedPool!=null && multiEntitlementPool!=null && standardPool!=null) break;
			if (!pool.quantity.equalsIgnoreCase("unlimited") && Integer.valueOf(pool.quantity)<2) continue;	// skip pools that don't have enough quantity left to consume
			
			if (CandlepinTasks.isPoolProductInstanceBased(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId)) {
				instanceBasedPool = pool; // should also be multiEntitlement
			} else if (CandlepinTasks.isPoolProductMultiEntitlement(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId)) {
				multiEntitlementPool = pool;
			} else {
				standardPool = pool;				
			}
		}
		
		SubscriptionPool pool;
		String expectedStdout;
		if (multiEntitlementPool!=null) {
			pool = multiEntitlementPool;
			
			// Object meta, String poolId, String quantity, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1119688"}),				pool,	"Two",						Integer.valueOf(64)/*EX_USAGE*/,	null,"^Error: Quantity must be a positive number.$".replace("number","integer")/* due to bug 746262*/}));
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1119688","722554"}),	pool,	"-1",						Integer.valueOf(64)/*EX_USAGE*/,	null,"^Error: Quantity must be a positive number.$".replace("number","integer")/* due to bug 746262*/}));
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1119688","722554"}),	pool,	"0",						Integer.valueOf(64)/*EX_USAGE*/,	null,"^Error: Quantity must be a positive number.$".replace("number","integer")/* due to bug 746262*/}));
			} else {
				ll.add(Arrays.asList(new Object[] {null,							pool,	"Two",												Integer.valueOf(255),	"^Error: Quantity must be a positive number.$".replace("number","integer")/* due to bug 746262*/,	null}));
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("722554"),	pool,	"-1",												Integer.valueOf(255),	"^Error: Quantity must be a positive number.$".replace("number","integer")/* due to bug 746262*/,	null}));
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("722554"),	pool,	"0",												Integer.valueOf(255),	"^Error: Quantity must be a positive number.$".replace("number","integer")/* due to bug 746262*/,	null}));
			}
			expectedStdout = "^"+String.format("Successfully consumed a subscription from the pool with id %s.",pool.poolId)+"$";	// Bug 812410 - Subscription-manager subscribe CLI feedback 
			expectedStdout = "^"+String.format("Successfully consumed a subscription for: %s",pool.subscriptionName.replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)"))+"$";	// changed by Bug 874804 Subscribe -> Attach
			expectedStdout = "^"+String.format("Successfully attached a subscription for: %s",pool.subscriptionName.replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)"))+"$";
			ll.add(Arrays.asList(new Object[] {null,								pool,	"1",												Integer.valueOf(0),		expectedStdout,	null}));
			ll.add(Arrays.asList(new Object[] {null,								pool,	"2",												Integer.valueOf(0),		expectedStdout,	null}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("746262"),		pool,	"+2",												Integer.valueOf(0),		expectedStdout,	null}));
			
			if (pool.quantity.equalsIgnoreCase("unlimited")) {
				ll.add(Arrays.asList(new Object[] {null,							pool,	"7000",												Integer.valueOf(0),		expectedStdout,	null}));
			} else {		
				
				if (!CandlepinType.hosted.equals(sm_serverType)) {	// exclude this test from running on a hosted server since parallel running tests often consume available quantities affecting the expected results
					ll.add(Arrays.asList(new Object[] {null,						pool,	pool.quantity,										Integer.valueOf(0),		expectedStdout,	null}));
				}
				
				expectedStdout = String.format("No entitlements are available from the pool with id '%s'.",pool.poolId);	// expected string changed by bug 876758
				expectedStdout = String.format("No subscriptions are available from the pool with id '%s'.",pool.poolId);
				if (!clienttasks.workaroundForBug876764(sm_serverType)) expectedStdout = String.format("No subscriptions are available from the pool with ID '%s'.",pool.poolId);
				if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
					expectedStdout = String.format("No subscriptions are available from the pool with ID \"%s\".",pool.poolId);
				}
				if (!CandlepinType.hosted.equals(sm_serverType)) {	// exclude this test from running on a hosted server since parallel running tests often consume available quantities affecting the expected results
					ll.add(Arrays.asList(new Object[] {null,						pool,	String.valueOf(Integer.valueOf(pool.quantity)+1),	Integer.valueOf(1),		"^"+expectedStdout+"$",	null}));
					ll.add(Arrays.asList(new Object[] {null,						pool,	String.valueOf(Integer.valueOf(pool.quantity)+10),	Integer.valueOf(1),		"^"+expectedStdout+"$",	null}));
				}
			}
		} else {
			ll.add(Arrays.asList(new Object[] {null,	null,	null,	null,	null,	"Could NOT find an available subscription pool with \"multi-entitlement\" product attribute set true."}));
		}
		
		if (standardPool!=null) {
			pool = standardPool;

			// Object meta, String poolId, String quantity, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex
			expectedStdout = "^"+String.format("Successfully consumed a subscription from the pool with id %s.",pool.poolId)+"$";	// Bug 812410 - Subscription-manager subscribe CLI feedback 
			expectedStdout = "^"+String.format("Successfully consumed a subscription for: %s",pool.subscriptionName.replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)"))+"$";	// changed by Bug 874804 Subscribe -> Attach
			expectedStdout = "^"+String.format("Successfully attached a subscription for: %s",pool.subscriptionName.replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)"))+"$";
			ll.add(Arrays.asList(new Object[] {null,								pool,	"1",												Integer.valueOf(0),		expectedStdout,	null}));
			expectedStdout = String.format("Multi-entitlement not supported for pool with id '%s'.",pool.poolId);
			if (!clienttasks.workaroundForBug876764(sm_serverType))  expectedStdout = String.format("Multi-entitlement not supported for pool with ID '%s'.",pool.poolId);
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
				expectedStdout = String.format("Multi-entitlement not supported for pool with ID \"%s\".",pool.poolId);
			}
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("722975"),		pool,	"2",												Integer.valueOf(1),		"^"+expectedStdout+"$",	null}));
		} else {
			ll.add(Arrays.asList(new Object[] {null,	null,	null,	null,	null,	"Could NOT find an available subscription pool with \"multi-entitlement\" product attribute set false (or absent)."}));
		}
		
		if (instanceBasedPool!=null) {
			pool = instanceBasedPool;
			int instanceMultiplier = Integer.valueOf(CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId,"instance_multiplier"));
			boolean isSystemVirtual = Boolean.valueOf(clienttasks.getFactValue("virt.is_guest"));
			if (isSystemVirtual) instanceMultiplier=1;	// a virtual system should be able to attach an instance-based subscription in increments of quantity=1 (this satisfies the Either-Or requirement)
			
			// Object meta, String poolId, String quantity, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1119688"}),				pool,	"Two",																									Integer.valueOf(64)/*EX_USAGE*/,	null,"^Error: Quantity must be a positive number.$".replace("number","integer")/* due to bug 746262*/}));
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1119688","722554"}),	pool,	"-1",																									Integer.valueOf(64)/*EX_USAGE*/,	null,"^Error: Quantity must be a positive number.$".replace("number","integer")/* due to bug 746262*/}));
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1119688","722554"}),	pool,	"0",																									Integer.valueOf(64)/*EX_USAGE*/,	null,"^Error: Quantity must be a positive number.$".replace("number","integer")/* due to bug 746262*/}));
			} else {
				ll.add(Arrays.asList(new Object[] {null,							pool,	"Two",																															Integer.valueOf(255),	"^Error: Quantity must be a positive number.$".replace("number","integer")/* due to bug 746262*/,	null}));
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("722554"),	pool,	"-1",																															Integer.valueOf(255),	"^Error: Quantity must be a positive number.$".replace("number","integer")/* due to bug 746262*/,	null}));
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("722554"),	pool,	"0",																															Integer.valueOf(255),	"^Error: Quantity must be a positive number.$".replace("number","integer")/* due to bug 746262*/,	null}));
			}
			expectedStdout = "^"+String.format("Successfully attached a subscription for: %s",pool.subscriptionName.replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)"))+"$";
			ll.add(Arrays.asList(new Object[] {null,								pool,	String.valueOf(1*instanceMultiplier),																							Integer.valueOf(0),		expectedStdout,	null}));
			ll.add(Arrays.asList(new Object[] {null,								pool,	String.valueOf(2*instanceMultiplier),																							Integer.valueOf(0),		expectedStdout,	null}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("746262"),		pool,	"+"+String.valueOf(2*instanceMultiplier),																						Integer.valueOf(0),		expectedStdout,	null}));
			
			if (pool.quantity.equalsIgnoreCase("unlimited")) {
				ll.add(Arrays.asList(new Object[] {null,							pool,	"7000",												Integer.valueOf(0),		expectedStdout,	null}));
			} else {
				
				if (!CandlepinType.hosted.equals(sm_serverType)) {	// exclude this test from running on a hosted server since parallel running tests often consume available quantities affecting the expected results
					ll.add(Arrays.asList(new Object[] {null,						pool,	String.valueOf(Integer.valueOf(pool.quantity) - Integer.valueOf(pool.quantity)%instanceMultiplier),								Integer.valueOf(0),		expectedStdout,	null})); 	// testing with a quantity one increment within the availability
				}
				expectedStdout = String.format("No subscriptions are available from the pool with id '%s'.",pool.poolId);
				if (!clienttasks.workaroundForBug876764(sm_serverType)) expectedStdout = String.format("No subscriptions are available from the pool with ID '%s'.",pool.poolId);
				if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
					expectedStdout = String.format("No subscriptions are available from the pool with ID \"%s\".",pool.poolId);
				}
				if (!CandlepinType.hosted.equals(sm_serverType)) {	// exclude this test from running on a hosted server since parallel running tests often consume available quantities affecting the expected results
					ll.add(Arrays.asList(new Object[] {null,						pool,	String.valueOf(Integer.valueOf(pool.quantity) - Integer.valueOf(pool.quantity)%instanceMultiplier + instanceMultiplier),		Integer.valueOf(1),		"^"+expectedStdout+"$",	null}));	// testing with a quantity one increment over the availability
					ll.add(Arrays.asList(new Object[] {null,						pool,	String.valueOf(Integer.valueOf(pool.quantity) - Integer.valueOf(pool.quantity)%instanceMultiplier + instanceMultiplier*10),		Integer.valueOf(1),		"^"+expectedStdout+"$",	null}));	// testing with a quantity ten increments over the availability
				}
			}
		} else {
			ll.add(Arrays.asList(new Object[] {null,	null,	null,	null,	null,	"Could NOT find an available subscription pool with \"instance_multipler\" product attribute set."}));
		}
		
		return ll;
	}
	
	
	@DataProvider(name="getSubscribeWithAutoAndServiceLevelData")
	public Object[][] getSubscribeWithAutoAndServiceLevelDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getSubscribeWithAutoAndServiceLevelDataAsListOfLists());
	}
	protected List<List<Object>>getSubscribeWithAutoAndServiceLevelDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		ll = getAllAvailableServiceLevelDataAsListOfLists();
		
		// throw in null and "" as a possible service levels
		// Object bugzilla, String org, String serviceLevel
		ll.add(Arrays.asList(new Object[] {null,	null}));
		ll.add(Arrays.asList(new Object[] {null,	""}));
		
		return ll;
	}
	
}
