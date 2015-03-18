package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.ConsumerCert;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductNamespace;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * Reference Design Doc:
 * 		none
 * Demoed by Alex on Sprint 86 Demo
 *   24 Hour Temporary Pools for Unmapped Guests (awood)
 *   Video:https://sas.elluminate.com/p.jnlp?psid=2015-02-04.0655.M.C6A830C9254F6A24CCEB96A94F3B5D.vcr&sid=819
 *     
 * Etherpad for 24 Hour Temporary Pools for Unmapped Guests
 *   http://etherpad.corp.redhat.com/MZhnahVIDk  --for review
 */
@Test(groups={"TemporaryPoolTests","Tier3Tests"})
public class TemporaryPoolTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************

	@Test(	description="given an available unmapped_guests_only pool, assert that it is available only to virtual systems whose host consumer has not yet mapped its virt.uuid as a guestId onto the host consumer.  Moreover, assert that once mapped, the pool is no longer available.",
			groups={"VerifyAvailabilityOfUnmappedGuestsOnlySubpool_Test"},
			dataProvider="getAvailableUnmappedGuestsOnlySubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAvailabilityOfUnmappedGuestsOnlySubpool_Test(Object bugzilla, SubscriptionPool unmappedGuestsOnlyPool) throws JSONException, Exception {
		
		// system facts are overridden with factsMap to fake this system as a guest
		
		// make sure we are freshly registered (to discard a consumer from a former data provided iteration that has mapped guests)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);	
		
		// verify the unmapped_guests_only pool is available for consumption
		List<SubscriptionPool> availableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		Assert.assertNotNull(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId",unmappedGuestsOnlyPool.poolId, availableSubscriptionPools),
				"Temporary pool '"+unmappedGuestsOnlyPool.subscriptionName+"' poolId='"+unmappedGuestsOnlyPool.poolId+"' (for virtual systems whose host consumer has not yet reported this system's virt.uuid as a guest) is available for consumption.");
		
		// verify that it is for Virtual systems
		Assert.assertEquals(unmappedGuestsOnlyPool.machineType, "Virtual","Temporary pools intended for unmapped guests only should indicate that it is for machine type Virtual.");
		
		// verify that the corresponding Physical pool is also available (when not physical_only)
//		String subscriptionId = CandlepinTasks.getSubscriptionIdForPoolId(sm_clientUsername, sm_clientPassword, sm_serverUrl, unmappedGuestsOnlyPool.poolId);
//		String ownerKey = clienttasks.getCurrentlyRegisteredOwnerKey();
//		List<String> poolIdsForSubscriptionId = CandlepinTasks.getPoolIdsForSubscriptionId(sm_clientUsername, sm_clientPassword, sm_serverUrl, ownerKey, subscriptionId);
//		String parentPhysicalPoolId = null;
//		for (String poolId : poolIdsForSubscriptionId) {
//			if (!poolId.equals(unmappedGuestsOnlyPool.poolId)) {
//				SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", poolId, availableSubscriptionPools);
//				if (CandlepinTasks.isPoolRestrictedToPhysicalSystems(sm_clientUsername, sm_clientPassword, sm_serverUrl, poolId)) {
//					Assert.assertNull(pool, "Should NOT found parent pool corresponding to temporary unmapped guests only poolId '"+unmappedGuestsOnlyPool.poolId+"' among available pools since it is Physical only.");		
//		
//				} else {
//					Assert.assertNotNull(pool, "Found parent Physical pool corresponding to temporary unmapped guests only poolId '"+unmappedGuestsOnlyPool.poolId+"' among available pools.");		
//					Assert.assertEquals(pool.machineType,"Physical", "The machine type for the parent pool corresponding to temporary unmapped guests only poolId '"+unmappedGuestsOnlyPool.poolId+"'.");
//				}
//				Assert.assertNull(parentPhysicalPoolId, "Found only one parent Physical poolId corresponding to temporary unmapped guests only poolId '"+unmappedGuestsOnlyPool.poolId+"'.");
//				parentPhysicalPoolId = poolId;
//			}
//		}
//		Assert.assertNotNull(parentPhysicalPoolId, "Found parent Physical poolId corresponding to temporary unmapped guests only poolId '"+unmappedGuestsOnlyPool.poolId+"'.");
		String parentPhysicalPoolId = getParentPoolIdCorrespondingToDerivedPoolId(sm_clientUsername, sm_clientPassword, sm_serverUrl, clienttasks.getCurrentlyRegisteredOwnerKey(), unmappedGuestsOnlyPool.poolId);
		Assert.assertNotNull(parentPhysicalPoolId, "Found parent Physical poolId corresponding to temporary unmapped guests only poolId '"+unmappedGuestsOnlyPool.poolId+"'.");
		SubscriptionPool parentPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", parentPhysicalPoolId, availableSubscriptionPools);
		if (CandlepinTasks.isPoolRestrictedToPhysicalSystems(sm_clientUsername, sm_clientPassword, sm_serverUrl, parentPhysicalPoolId)) {
			Assert.assertNull(parentPool, "Should NOT found parent pool corresponding to temporary unmapped guests only poolId '"+unmappedGuestsOnlyPool.poolId+"' among available pools since it is Physical only.");		
		} else {
			Assert.assertNotNull(parentPool, "Found parent Physical pool corresponding to temporary unmapped guests only poolId '"+unmappedGuestsOnlyPool.poolId+"' among available pools.");		
			Assert.assertEquals(parentPool.machineType,"Physical", "The machine type for the parent pool corresponding to temporary unmapped guests only poolId '"+unmappedGuestsOnlyPool.poolId+"'.");
		}
		
		// simulate the actions of virt-who my mapping the virt.uuid as a guestID onto a host consumer
		clienttasks.mapSystemAsAGuestOfItself();
		
		// verify that the unmapped_guests_only pool is no longer available
		Assert.assertNull(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId",unmappedGuestsOnlyPool.poolId, clienttasks.getCurrentlyAvailableSubscriptionPools()),
				"Temporary pool '"+unmappedGuestsOnlyPool.subscriptionName+"' poolId='"+unmappedGuestsOnlyPool.poolId+"' (for virtual systems whose host consumer has not yet reported this system's virt.uuid as a guest) is NO LONGER available for consumption after it's virt.uuid has been mapped to a consumer's guestId list.");
		
	}
	
	
	@Test(	description="given an available unmapped_guests_only pool, assert that attaching it does not throw any Tracebacks ",
			groups={"blockedByBug-1198369","VerifyAvailabilityOfUnmappedGuestsOnlySubpool_Test"},
			dataProvider="getAvailableUnmappedGuestsOnlySubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyNoTracebacksAreThrownWhenSubscribingToUnmappedGuestsOnlySubpool_Test(Object bugzilla, SubscriptionPool unmappedGuestsOnlyPool) throws JSONException, Exception {
		
		// system facts are overridden with factsMap to fake this system as a guest
		
		// make sure we are freshly registered (to discard a consumer from a former data provided iteration that has mapped guests)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);	
		
		// subscribe and assert stderr does not report an UnboundLocalError
		SSHCommandResult result = clienttasks.subscribe(null, null, unmappedGuestsOnlyPool.poolId, null, null, null, null, null, null, null, null, null);
		//	201503161528:29.708 - FINE: ssh root@jsefler-os6.usersys.redhat.com subscription-manager subscribe --pool=8a9087e34c097838014c09799c231f02
		//	201503161528:33.439 - FINE: Stdout: Successfully attached a subscription for: Awesome OS Server Basic (data center)
		//	201503161528:33.442 - FINE: Stderr: 
		//	Traceback (most recent call last):
		//	  File "/usr/share/rhsm/subscription_manager/dbus_interface.py", line 59, in emit_status
		//	    self.validity_iface.emit_status()
		//	  File "/usr/lib/python2.6/site-packages/dbus/proxies.py", line 68, in __call__
		//	    return self._proxy_method(*args, **keywords)
		//	  File "/usr/lib/python2.6/site-packages/dbus/proxies.py", line 140, in __call__
		//	    **keywords)
		//	  File "/usr/lib/python2.6/site-packages/dbus/connection.py", line 630, in call_blocking
		//	    message, timeout)
		//	dbus.exceptions.DBusException: org.freedesktop.DBus.Python.UnboundLocalError: Traceback (most recent call last):
		//	  File "/usr/lib/python2.6/site-packages/dbus/service.py", line 702, in _message_cb
		//	    retval = candidate_method(self, *args, **keywords)
		//	  File "/usr/libexec/rhsmd", line 202, in emit_status
		//	    self._dbus_properties = refresh_compliance_status(self._dbus_properties)
		//	  File "/usr/share/rhsm/subscription_manager/managerlib.py", line 920, in refresh_compliance_status
		//	    entitlements[label] = (name, state, message)
		//	UnboundLocalError: local variable 'state' referenced before assignment
		
		Assert.assertTrue(!result.getStderr().contains("Traceback"), "stderr when subscribing to a temporary unmapped guests only pool should NOT throw a Traceback.");
	}
	
	
	@Test(	description="given an available unmapped_guests_only pool, attach it and verify the status details of the consumed subscription, installed product, and system status.",	// TODO
			groups={"VerifyAvailabilityOfUnmappedGuestsOnlySubpool_Test"},
			dataProvider="getAvailableUnmappedGuestsOnlySubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyStatusDetailsAfterAttachingUnmappedGuestsOnlySubpool_Test(Object bugzilla, SubscriptionPool unmappedGuestsOnlyPool) throws JSONException, Exception {
		
		// system facts are overridden with factsMap to fake this system as a guest
		
		// make sure we are freshly registered (to discard a consumer from a former data provided iteration that has mapped guests)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);	
		ConsumerCert cert = clienttasks.getCurrentConsumerCert();
		
		// attach the unmapped guests only pool
		//File serialPemFile = clienttasks.subscribeToSubscriptionPool(unmappedGuestsOnlyPool, null, sm_clientUsername, sm_clientPassword, sm_serverUrl);
		//EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(serialPemFile);
		clienttasks.subscribe_(null, null, unmappedGuestsOnlyPool.poolId, null, null, null, null, null, null, null, null, null);
		ProductSubscription consumedUnmappedGuestsOnlyProductSubscription = clienttasks.getCurrentlyConsumedProductSubscriptions().get(0);	// assumes only one consumed entitlement
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertCorrespondingToProductSubscription(consumedUnmappedGuestsOnlyProductSubscription);

		// assert the expiration is 24 hours post the consumer's registration
		Calendar expectedEntitlementCertEndDate = (Calendar) cert.validityNotBefore.clone();
		expectedEntitlementCertEndDate.add(Calendar.HOUR, 24);
		Assert.assertEquals(ConsumerCert.formatDateString(entitlementCert.validityNotAfter), ConsumerCert.formatDateString(expectedEntitlementCertEndDate), "The End Date of the entitlement from a temporary pool should be exactly 24 hours after the registration date of the current consumer '"+ConsumerCert.formatDateString(cert.validityNotBefore)+"'.");
		
		// assert the Status Details of the attached subscription
		String expectedStatusDetailsForAnUnmappedGuestsOnlyProductSubscription = "Guest has not been reported on any host and is using a temporary unmapped guest subscription.";
		Assert.assertEquals(consumedUnmappedGuestsOnlyProductSubscription.statusDetails, Arrays.asList(new String[]{expectedStatusDetailsForAnUnmappedGuestsOnlyProductSubscription}),"Status Details of a consumed subscription from a temporary pool for unmapped guests only.");
		
		// assert that the temporary subscription appears in the status report
		SSHCommandResult statusResult = clienttasks.status(null, null, null, null);
		Map<String,String> statusMap = StatusTests.getProductStatusMapFromStatusResult(statusResult);
		Assert.assertTrue(statusMap.containsKey(consumedUnmappedGuestsOnlyProductSubscription.productName),"The status module reports an incompliance from temporary subscription '"+consumedUnmappedGuestsOnlyProductSubscription.productName+"'.");
		Assert.assertEquals(statusMap.get(consumedUnmappedGuestsOnlyProductSubscription.productName),expectedStatusDetailsForAnUnmappedGuestsOnlyProductSubscription,"The status module reports an incompliance from temporary subscription '"+consumedUnmappedGuestsOnlyProductSubscription.productName+"' for this reason.");
		// assert that the temporary subscription causes an overall status is Invalid
		String expectedStatus = "Overall Status: Invalid";
		Assert.assertTrue(statusResult.getStdout().contains(expectedStatus), "Expecting '"+expectedStatus+"' when a temporary subscription for '"+consumedUnmappedGuestsOnlyProductSubscription.productName+"' is attached.");

		// assert the status of installed products provided by the temporary subscription
		List<String> providedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, unmappedGuestsOnlyPool.poolId);
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		for (String providedProductId : providedProductIds) {
			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", providedProductId, installedProducts);
			if (installedProduct!=null) {	// providedProduct is installed
				// assert the status details
				Assert.assertEquals(installedProduct.status,"Subscribed","Status of an installed product provided for by a temporary entitlement from a pool reserved for unmapped guests only.");	// see CLOSED NOTABUG Bug 1200882 - Wrong installed product status is displayed when a unmapped_guests_only pool is attached
				Assert.assertEquals(installedProduct.statusDetails,new ArrayList<String>(),"Status Details of an installed product provided for by a temporary entitlement from a pool reserved for unmapped guests only.");
				// TODO: The above two asserts might be changed by RFE Bug 1201520 - [RFE] Usability suggestions to better identify a temporary (aka 24 hour) entitlement 
				
				// assert the start-end dates
				// TEMPORARY WORKAROUND FOR BUG
				String bugId = "1199443";	// Bug 1199443 - Wrong "End date" in installed list after attaching 24-hour subscription on a unmapped-guest
				boolean invokeWorkaroundWhileBugIsOpen = true;
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Skipping assertion of the End Date for installed product '"+installedProduct.productName+"' provided by a temporary pool while bug '"+bugId+"' is open.");
				} else	// assert the End Date
				// END OF WORKAROUND			
				Assert.assertEquals(InstalledProduct.formatDateString(installedProduct.endDate), InstalledProduct.formatDateString(entitlementCert.validityNotAfter), "The End Date of coverage for the installed product '"+installedProduct.productName+"' should exactly match the end date from the temporary pool subscription.");

			}
		}
	}
	
	
	@Test(	description="given an available unmapped_guests_only pool, attach it and attempt to auto-heal - repeated attempts to auto-heal should NOT add more and more entitlements",
			groups={"blockedByBug-1198494","VerifyAvailabilityOfUnmappedGuestsOnlySubpool_Test"},
			dataProvider="getAvailableUnmappedGuestsOnlySubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutoHealingIsStableAfterAttachingUnmappedGuestsOnlySubpool_Test(Object bugzilla, SubscriptionPool unmappedGuestsOnlyPool) throws JSONException, Exception {
		
		// system facts are overridden with factsMap to fake this system as a guest
		
		// make sure we are freshly registered (to discard a consumer from a former data provided iteration that has mapped guests) and auto-subscribed
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, false, null, null, null);	
		
		// attach the unmapped guests only pool
		clienttasks.subscribe_(null, null, unmappedGuestsOnlyPool.poolId, null, null, null, null, null, null, null, null, null);
		
		// assert that the temporary subscription appears in the status report
		SSHCommandResult statusResult = clienttasks.status(null, null, null, null);
		//	[root@jsefler-os6 ~]# subscription-manager status
		//	+-------------------------------------------+
		//	   System Status Details
		//	+-------------------------------------------+
		//	Overall Status: Invalid
		//
		//	Awesome OS Instance Server Bits:
		//	- Guest has not been reported on any host and is using a temporary unmapped guest subscription.
		//
		//	UNABLE_TO_GET_NAME:
		//	- Guest has not been reported on any host and is using a temporary unmapped guest subscription.
		//
		Map<String,String> statusMap = StatusTests.getProductStatusMapFromStatusResult(statusResult);
		Assert.assertTrue(!statusMap.containsKey("UNABLE_TO_GET_NAME"),"The status module should NOT report 'UNABLE_TO_GET_NAME'.");	// occurred in candlepin 0.9.45-1 // TODO block by a bug
		Assert.assertTrue(statusMap.containsKey(unmappedGuestsOnlyPool.subscriptionName),"The status module reports an incompliance from temporary subscription '"+unmappedGuestsOnlyPool.subscriptionName+"'.");

		// get a list of the current entitlements
		List<File> entitlementCertFileAfterSubscribed = clienttasks.getCurrentEntitlementCertFiles();
		
		// trigger an auto-heal event
		clienttasks.autoheal(null, true, null, null, null, null);
		clienttasks.run_rhsmcertd_worker(true);
		
		// get a list of the entitlements after auto-heal
		List<File> entitlementCertFileAfterAutoHeal1 = clienttasks.getCurrentEntitlementCertFiles();
		
		// assert that no additional entitlements were added (test for Bug 1198494 - Auto-heal continuously attaches subscriptions to make the system compliant on a guest machine)
		Assert.assertTrue(entitlementCertFileAfterSubscribed.containsAll(entitlementCertFileAfterAutoHeal1) && entitlementCertFileAfterAutoHeal1.containsAll(entitlementCertFileAfterSubscribed),
				"The entitlement certs remained the same after first attempt to auto-heal despite the attched temporary pool. (Assuming that the initial registration with autosubcribe provided as much coverage as possible for the installed products from subscriptions with plenty of available quantity, repeated attempts to auto-heal should not consume more entitlements.)");

		// trigger a second auto-heal event
		clienttasks.autoheal(null, true, null, null, null, null);
		clienttasks.run_rhsmcertd_worker(true);
		
		// get a list of the entitlements after second auto-heal event
		List<File> entitlementCertFileAfterAutoHeal2 = clienttasks.getCurrentEntitlementCertFiles();
		
		// assert that no additional entitlements were added (test for Bug 1198494 - Auto-heal continuously attaches subscriptions to make the system compliant on a guest machine)
		Assert.assertTrue(entitlementCertFileAfterSubscribed.containsAll(entitlementCertFileAfterAutoHeal2) && entitlementCertFileAfterAutoHeal2.containsAll(entitlementCertFileAfterSubscribed),
				"The entitlement certs remained the same after a second attempt to auto-heal despite the attched temporary pool. (Assuming that the initial registration with autosubcribe provided as much coverage as possible for the installed products from subscriptions with plenty of available quantity, repeated attempts to auto-heal should not consume more entitlements.)");
	}
	
	
	@Test(	description="given an available unmapped_guests_only pool,...",	// TODO
			groups={"debugTest","VerifyAvailabilityOfUnmappedGuestsOnlySubpool_Test"},
			dataProvider="getAvailableUnmappedGuestsOnlySubscriptionPoolsData",
			enabled=false)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutoHealingAfterAttachingUnmappedGuestsOnlySubpool_Test(Object bugzilla, SubscriptionPool unmappedGuestsOnlyPool) throws JSONException, Exception {
		
		// map the guest
		
		// trigger a rhsmcertd checkin
		
		// verify that the attached temporary subscription is automatically removed
	}

//		// get some attributes from the subscription pool
//		String poolDerivedProductId = (String)CandlepinTasks.getPoolValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "derivedProductId");
//		String poolDerivedProductName = (String)CandlepinTasks.getPoolValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "derivedProductName");
//		List<String> poolDerivedProvidedProductIds = CandlepinTasks.getPoolDerivedProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId);
//		String poolVirtLimit = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "virt_limit");
//		List<String> poolProvidedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId);
//		
//		// assert that the derivedProductId is different from the host pool's productId
//		Assert.assertNotSame(pool.productId, poolDerivedProductId, "The host pool's data center subscription product Id '"+pool.productId+"' should be different than its derived pool's product Id '"+poolDerivedProductId+"'.");
//		
//		// assert that the derivedProductName is different from the host pool's subscription name
//		Assert.assertNotSame(pool.subscriptionName, poolDerivedProductName, "The host pool's data center subscription name '"+pool.subscriptionName+"' should be different than its derived pool's product name '"+poolDerivedProductName+"'.");
//		
//		// instrument the system facts to behave as a physical host
//		factsMap.put("virt.is_guest",String.valueOf(false));
//		clienttasks.createFactsFileWithOverridingValues(factsMap);
//		clienttasks.facts(null,true,null,null,null);	// update facts
//		
//		// reset a few fake guest ids for this host consumer
//		String systemUuid = clienttasks.getCurrentConsumerId();
//		//[root@jsefler-5 ~]# curl -k -u testuser1:password --request PUT --data '{"guestIds":["e6f55b91-aae1-44d6-f0db-c8f25ec73ef5","abcd"]}' --header 'accept:application/json' --header 'content-type: application/json' https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/consumers/d2ee0c6e-a57d-4e37-8be3-228a44ca2739 
//		JSONObject jsonConsumer = CandlepinTasks.setGuestIdsForConsumer(sm_clientUsername,sm_clientPassword, sm_serverUrl, systemUuid,Arrays.asList(new String[]{"abc","def"}));
//		
//		// assert that only Physical pools are available for consumption for this data center sku
//		for (SubscriptionPool subscriptionPool : SubscriptionPool.findAllInstancesWithMatchingFieldFromList("productId", pool.productId, clienttasks.getCurrentlyAllAvailableSubscriptionPools())) {
//			Assert.assertEquals(subscriptionPool.machineType, "Physical", "Only physical pools to '"+pool.productId+"' should be available to a physical host system.");
//		}
//		
//		// subscribe the host to the data center pool
//		File hostEntitlementFile = clienttasks.subscribeToSubscriptionPool(pool,/*sm_serverAdminUsername*/sm_clientUsername,/*sm_serverAdminPassword*/sm_clientPassword,sm_serverUrl);
//		EntitlementCert hostEntitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(hostEntitlementFile); client.runCommandAndWait("rct cat-cert "+hostEntitlementFile);
//		
//		// the following general asserts are not true against a production datacenter SKU...
//		if (false) {
//			//	Subscription Name: Red Hat Enterprise Linux for Virtual Datacenters, Premium
//			//	Provides:          Red Hat Enterprise Linux Atomic Host
//			//	SKU:               RH00001
//			//	Contract:          10472273
//			//	Pool ID:           8a99f98146b4fa9d0146b5d3c0005253
//			//	Available:         98
//			//	Suggested:         1
//			//	Service Level:     Premium
//			//	Service Type:      L1-L3
//			//	Subscription Type: Stackable
//			//	Ends:              12/30/2014
//			//	System Type:       Physical
//			
//			// in general the data center pool will not provide any engineering products
//			Assert.assertTrue(poolProvidedProductIds.isEmpty(), "In general, a data center product subscription will not provide any engineering products (productId= '"+pool.productId+"').  Asserting the providedProducts from the subscription is empty...");		
//			Assert.assertTrue(hostEntitlementCert.productNamespaces.isEmpty(), "In general, a data center product subscription will not provide any engineering products (productId= '"+pool.productId+"').  Asserting the productNamespaces from the granted entitlement are empty...");		
//			
//			// in general the data center pool will not provide any content
//			Assert.assertTrue(hostEntitlementCert.contentNamespaces.isEmpty(), "In general, a data center product subscription will not provide any content sets (productId= '"+pool.productId+"').");		
//		}
//		
//		// assert that the derivedProductId is NOT available to the Physical host system
//		List<SubscriptionPool> availablePoolsForDerivedProductId = SubscriptionPool.findAllInstancesWithMatchingFieldFromList("productId", poolDerivedProductId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
//		Assert.assertTrue(availablePoolsForDerivedProductId.isEmpty(),"A subpool for the derivedProductId '"+poolDerivedProductId+"' should NOT be available to the host after (or before) it consumes the data center product subscription.");
//		
//		// now we can assert that a host_limited subpool was generated from consumption of this physical pool and is only available to guests of this physical system
//		// first, let's flip the virt.is_guest to true and assert that the virtual guest subpool is not (yet) available since the virtUuid is not on the host consumer's list of guestIds
//		factsMap.put("virt.is_guest",String.valueOf(true));
//		clienttasks.createFactsFileWithOverridingValues(factsMap);
//		clienttasks.facts(null,true,null,null,null);	// update facts
//		availablePoolsForDerivedProductId = SubscriptionPool.findAllInstancesWithMatchingFieldFromList("productId", poolDerivedProductId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
//		Assert.assertTrue(availablePoolsForDerivedProductId.isEmpty(),"A subpool for the derivedProductId should NOT be available to a guest system when its virt_uuid is not on the host's list of guestIds.");
//		
//			
//		// now fake this consumer's facts and guestIds to make it think it is a guest of itself (a trick for testing)
//		factsMap.put("virt.uuid",systemUuid);
//		clienttasks.createFactsFileWithOverridingValues(factsMap);
//		clienttasks.facts(null,true,null,null,null);	// update facts
//		//[root@jsefler-5 ~]# curl -k -u testuser1:password --request PUT --data '{"guestIds":["e6f55b91-aae1-44d6-f0db-c8f25ec73ef5","abcd"]}' --header 'accept:application/json' --header 'content-type: application/json' https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/consumers/d2ee0c6e-a57d-4e37-8be3-228a44ca2739 
//		jsonConsumer = CandlepinTasks.setGuestIdsForConsumer(sm_clientUsername,sm_clientPassword, sm_serverUrl, systemUuid,Arrays.asList(new String[]{"abc",systemUuid,"def"}));
//		
//		// now the host_limited subpool to the derivedProductId for this virtual system should be available
//		availablePoolsForDerivedProductId = SubscriptionPool.findAllInstancesWithMatchingFieldFromList("productId", poolDerivedProductId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
//		Assert.assertTrue(!availablePoolsForDerivedProductId.isEmpty(),"Host_limited subpool from data center product id '"+pool.productId+"' to derived product id '"+poolDerivedProductId+"' is available to its guest.");
//		Assert.assertEquals(availablePoolsForDerivedProductId.size(),1,"Only one host_limited subpool to derived product id '"+poolDerivedProductId+"' is available to its guest.");
//		SubscriptionPool derivedPool = availablePoolsForDerivedProductId.get(0);
//		Assert.assertEquals(derivedPool.subscriptionName, poolDerivedProductName, "Subscription name for the derived product id '"+poolDerivedProductId+"'.");
//		Assert.assertEquals(derivedPool.quantity.toLowerCase(),poolVirtLimit,"The quantity of entitlements from the host_limited subpool to derived product subscription '"+poolDerivedProductName+"' should be the same as the host data center subscription's virt_limit '"+poolVirtLimit+"'.");
//		
//		// now subscribe to the derived subpool and we'll assert the entitlement values come from the derived product and not the originating data center subscription
//		
//		// subscribe the guest to the derived product subscription
//		File derivedEntitlementFile = clienttasks.subscribeToSubscriptionPool(derivedPool,/*sm_serverAdminUsername*/sm_clientUsername,/*sm_serverAdminPassword*/sm_clientPassword,sm_serverUrl);
//		EntitlementCert derivedEntitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(derivedEntitlementFile); client.runCommandAndWait("rct cat-cert "+derivedEntitlementFile);
//		
//		// assert all of the derived provided products are included in the entitlement
//		List<String> actualDerivedProvidedProductIds = new ArrayList<String>();
//		for (ProductNamespace productNamespace : derivedEntitlementCert.productNamespaces) {
//			actualDerivedProvidedProductIds.add(productNamespace.id);
//		}
//		Assert.assertTrue(actualDerivedProvidedProductIds.containsAll(poolDerivedProvidedProductIds)&&poolDerivedProvidedProductIds.containsAll(actualDerivedProvidedProductIds),
//			"The actual product ids "+actualDerivedProvidedProductIds+" provided by an entitlement cert from the derived subpool '"+derivedPool.subscriptionName+"' match the expected derivedProvidedProducts "+poolDerivedProvidedProductIds+" from the data center subscription '"+pool.subscriptionName+"'.");
//		
//		// assert the derivedProductAttributes are reflected in the entitlement cert granted from the derived subpool
//		
//		//	Order:											Order:
//		//		Name: Awesome OS Server Basic (data center)		Name: Awesome OS Server Basic (dc-virt)
//		//		Number: order-8675309							Number: order-8675309
//		//		SKU: awesomeos-server-basic-dc					SKU: awesomeos-server-basic-vdc
//		//		Contract: 18									Contract: 18
//		//		Account: 12331131231							Account: 12331131231
//		//		Service Level: None								Service Level: Full-Service
//		//		Service Type: Self-Support						Service Type: Drive-Through
//		//		Quantity: 5										Quantity: 5
//		//		Quantity Used: 1								Quantity Used: 1
//		//		Socket Limit: 4									Socket Limit: 2
//		//		RAM Limit: 										RAM Limit: 2
//		//		Core Limit: 									Core Limit: 4
//		//		Virt Limit: 									Virt Limit: 
//		//		Virt Only: False								Virt Only: True
//		//		Subscription: 									Subscription: 
//		//		Stacking ID: 									Stacking ID: 
//		//		Warning Period: 30								Warning Period: 0
//		//		Provides Management: False						Provides Management: False
//			
//		//		Name: Awesome OS Server Basic (data center)		Name: Awesome OS Server Basic (dc-virt)
//		Assert.assertEquals(hostEntitlementCert.orderNamespace.productName, pool.subscriptionName,																														"hostEntitlementCert.orderNamespace.productName should match the data center pool's subscription name");
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.productName, poolDerivedProductName,																													"derivedEntitlementCert.orderNamespace.productName should match the derivedProductName");
//		
//		//		Number: order-8675309							Number: order-8675309
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.orderNumber, hostEntitlementCert.orderNamespace.orderNumber,																							"Order Number from the derived entitlement should match the host entitlement");
//		
//		//		SKU: awesomeos-server-basic-dc					SKU: awesomeos-server-basic-vdc
//		Assert.assertEquals(hostEntitlementCert.orderNamespace.productId, pool.productId,																																"hostEntitlementCert.orderNamespace.productId should match the data center pool's productId");
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.productId, poolDerivedProductId,																														"derivedEntitlementCert.orderNamespace.productId should match the derivedProductId");
//
//		//		Contract: 18									Contract: 18
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.contractNumber, hostEntitlementCert.orderNamespace.contractNumber,																					"Contract Number from the derived entitlement should match the host entitlement");
//
//		//		Account: 12331131231							Account: 12331131231
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.accountNumber, hostEntitlementCert.orderNamespace.accountNumber,																						"Account Number from the derived entitlement should match the host entitlement");
//
//		//		Service Level: None								Service Level: Full-Service
//		Assert.assertEquals(hostEntitlementCert.orderNamespace.supportLevel, pool.serviceLevel,																															"hostEntitlementCert.orderNamespace.supportLevel should match the data center pool's serviceLevel");
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.supportLevel, CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "support_level"),	"derivedEntitlementCert.orderNamespace.supportLevel should match the derivedProductAttribute support_level");
//
//		//		Service Type: Self-Support						Service Type: Drive-Through
//		Assert.assertEquals(hostEntitlementCert.orderNamespace.supportType, pool.serviceType,																															"hostEntitlementCert.orderNamespace.supportType should match the data center pool's serviceType");
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.supportType, CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "support_type"),		"derivedEntitlementCert.orderNamespace.supportType should match the derivedProductAttribute support_type");
//
//		//		Quantity: 5										Quantity: 5
//		JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,"/pools/"+pool.poolId));
//		Assert.assertEquals(hostEntitlementCert.orderNamespace.quantity, String.valueOf(jsonPool.getInt("quantity")),																													"hostEntitlementCert.orderNamespace.quantity should match the data center subscription pool's total quantity");
//		// TEMPORARY WORKAROUND FOR BUG
//		String bugId = "983193"; boolean invokeWorkaroundWhileBugIsOpen = true;
//		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
//		if (invokeWorkaroundWhileBugIsOpen) {
//			log.warning("Skipping the assertion of quantity while bug '"+bugId+"' is open.");
//		} else {
//		// END OF WORKAROUND
//		if (clienttasks.isPackageVersion("subscription-manager","<","1.10.3-1") && derivedEntitlementCert.orderNamespace.quantity.equals("-1")) {log.warning("The rct cat-cert tool encountered a Quantity of -1 which is fixed in subscription-manager-1.10.3-1.  Skipping assertion.");} else	// Bug 1011961 - rct cat-cert should display "Unlimited" for Quantity instead of "-1";  subscription-manager commit 7554c869608a0276151993d34fee4ddb54185f7a
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.quantity, derivedPool.quantity,																														"derivedEntitlementCert.orderNamespace.quantity should match the derivedPool's quantity");
//		}
//		
//		//		Quantity Used: 1								Quantity Used: 1
//		//TODO for derivedEntitlementCert only
//		
//		//		Socket Limit: 4									Socket Limit: 2
//		Assert.assertEquals(hostEntitlementCert.orderNamespace.socketLimit, CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "sockets"),					"hostEntitlementCert.orderNamespace.socketLimit should match the data center pool's productAttribute sockets");
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.socketLimit, CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "sockets"),			"derivedEntitlementCert.orderNamespace.socketLimit should match the derivedProductAttribute sockets");
//
//		//		RAM Limit: 										RAM Limit: 2
//		Assert.assertEquals(hostEntitlementCert.orderNamespace.ramLimit, CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "ram"),							"hostEntitlementCert.orderNamespace.ramLimit should match the data center pool's productAttribute ram");
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.ramLimit, CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "ram"),					"derivedEntitlementCert.orderNamespace.ramLimit should match the derivedProductAttribute ram");
//
//		//		Core Limit: 									Core Limit: 4
//		Assert.assertEquals(hostEntitlementCert.orderNamespace.coreLimit, CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "cores"),						"hostEntitlementCert.orderNamespace.coreLimit should match the data center pool's productAttribute cores");
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.coreLimit, CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "cores"),				"derivedEntitlementCert.orderNamespace.coreLimit should match the derivedProductAttribute cores");
//
//		//		Virt Limit: 									Virt Limit: 
//		//TODO ignoring for now based on https://bugzilla.redhat.com/show_bug.cgi?id=983193#c2
//		
//		//		Virt Only: False								Virt Only: True
//		String virtOnly = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "virt_only");
//		Assert.assertEquals(hostEntitlementCert.orderNamespace.virtOnly, virtOnly==null?Boolean.valueOf(false):Boolean.valueOf(virtOnly),																				"hostEntitlementCert.orderNamespace.virtOnly should match the data center pool's productAttribute virt_only");
//		virtOnly = String.valueOf(true);/* the derived pool virt_only should ALWAYS be true */	//CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "virt_only");
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.virtOnly, virtOnly==null?Boolean.valueOf(false):Boolean.valueOf(virtOnly),																			"derivedEntitlementCert.orderNamespace.virtOnly should match the derivedProductAttribute virt_only");
//
//		//		Subscription: 									Subscription: 
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.subscriptionNumber, hostEntitlementCert.orderNamespace.subscriptionNumber,																			"Subscription from the derived entitlement should match the host entitlement");
//		
//		//		Stacking ID: 									Stacking ID: 
//		Assert.assertEquals(hostEntitlementCert.orderNamespace.stackingId, CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "stacking_id"),					"hostEntitlementCert.orderNamespace.stackingId should match the data center pool's productAttribute stacking_id");
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.stackingId, CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "stacking_id"),		"derivedEntitlementCert.orderNamespace.stackingId should match the derivedProductAttribute stacking_id");
//		
//		//		Warning Period: 30								Warning Period: 0
//		String warningPeriod = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "warning_period");
//		Assert.assertEquals(hostEntitlementCert.orderNamespace.warningPeriod, warningPeriod==null?"0":warningPeriod,																									"hostEntitlementCert.orderNamespace.warningPeriod should match the data center pool's productAttribute warning_period");
//		warningPeriod = CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "warning_period");
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.warningPeriod, warningPeriod==null?"0":warningPeriod,																									"derivedEntitlementCert.orderNamespace.warningPeriod should match the derivedProductAttribute warning_period");
//		
//		//		Provides Management: False						Provides Management: False
//		String providesManagement = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "management_enabled");
//		Assert.assertEquals(hostEntitlementCert.orderNamespace.providesManagement, providesManagement==null?Boolean.valueOf(false):Boolean.valueOf(providesManagement),													"hostEntitlementCert.orderNamespace.providesManagement should match the data center pool's productAttribute management_enabled");
//		providesManagement = CandlepinTasks.getPoolDerivedProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "management_enabled");
//		Assert.assertEquals(derivedEntitlementCert.orderNamespace.providesManagement, providesManagement==null?Boolean.valueOf(false):Boolean.valueOf(providesManagement),												"derivedEntitlementCert.orderNamespace.providesManagement should match the derivedProductAttribute management_enabled");
//		
//		// for the sake of cleanup and to avoid this candlepin errors, let's unsubscribe from derivedEntitlementCert and then hostEntitlementCert
//		//	ssh root@jsefler-6server.usersys.redhat.com subscription-manager unregister
//		//	Stdout: Runtime Error No row with the given identifier exists: [org.candlepin.model.ProvidedProduct#8a90869341e61f7c0141e84e9ade3efd] at org.hibernate.UnresolvableObjectException.throwIfNull:65
//		//	Stderr:
//		//	ExitCode: 255
//		//	
//		//	ssh root@jsefler-6server.usersys.redhat.com subscription-manager unsubscribe --all
//		//	Stdout:
//		//	Stderr: Runtime Error No row with the given identifier exists: [org.candlepin.model.DerivedProvidedProduct#8a90869341e61f7c0141e84f2b9a3f0e] at org.hibernate.UnresolvableObjectException.throwIfNull:65
//		//	ExitCode: 255
//		clienttasks.unsubscribeFromSerialNumber(derivedEntitlementCert.serialNumber);
//		clienttasks.unsubscribeFromSerialNumber(hostEntitlementCert.serialNumber);
//	}
	
	@AfterGroups(value={"VerifyAvailabilityOfUnmappedGuestsOnlySubpool_Test"},groups={"setup"})
	public void afterVerifyAvailabilityOfDerivedProductSubpool_Test() {
//		clienttasks.unsubscribeFromTheCurrentlyConsumedSerialsCollectively();	// will avoid: Runtime Error No row with the given identifier exists: [org.candlepin.model.PoolAttribute#8a99f98a46b4fa990146ba9494032318] at org.hibernate.UnresolvableObjectException.throwIfNull:64
		clienttasks.unregister(null,null,null);
		clienttasks.deleteFactsFileWithOverridingValues();
	}
	Map<String,String> factsMap = new HashMap<String,String>();
	
	@DataProvider(name="getAvailableUnmappedGuestsOnlySubscriptionPoolsData")
	public Object[][] getAvailableUnmappedGuestsOnlySubscriptionPoolsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAvailableUnmappedGuestsOnlySubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAvailableUnmappedGuestsOnlySubscriptionPoolsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// instrument the system to be a Virtual guest
		factsMap.clear();
		factsMap.put("virt.is_guest",String.valueOf(true));
		factsMap.put("virt.uuid", "1234-5678-ABCD-EFGH");
		// set system facts that will always make the subscription available
		factsMap.put("memory.memtotal", "1");			// ram
		factsMap.put("cpu.cpu_socket(s)", "1");			// sockets
		factsMap.put("cpu.core(s)_per_socket", "1");	// cores

		clienttasks.createFactsFileWithOverridingValues(factsMap);
		
		for (List<Object> list : getAvailableSubscriptionPoolsDataAsListOfLists(false)) {
			SubscriptionPool pool = (SubscriptionPool)(list.get(0));
			
			if (CandlepinTasks.isPoolRestrictedToUnmappedVirtualSystems(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId)) {
				//									Object bugzilla,	SubscriptionPool unmappedGuestsOnlyPool
				ll.add(Arrays.asList(new Object[]{null,	pool}));
			}
		}
		return ll;
	}
	
	
	
	
	
	
	// Candidates for an automated Test:
	
	
	
	
	// Configuration methods ***********************************************************************


	
	// Protected methods ***********************************************************************
	protected String getParentPoolIdCorrespondingToDerivedPoolId(String authenticator, String password, String url, String ownerKey, String derivedPoolId) throws JSONException, Exception {
		String subscriptionId = CandlepinTasks.getSubscriptionIdForPoolId(authenticator, password, url, derivedPoolId);
		List<String> poolIdsForSubscriptionId = CandlepinTasks.getPoolIdsForSubscriptionId(authenticator, password, url, ownerKey, subscriptionId);
		String parentPhysicalPoolId = null;
		for (String poolId : poolIdsForSubscriptionId) {
			if (!poolId.equals(derivedPoolId)) {
				Assert.assertNull(parentPhysicalPoolId, "Found one parent Physical poolId corresponding to derived poolId '"+derivedPoolId+"' (we should find only one).");
				parentPhysicalPoolId = poolId;
			}
		}
		Assert.assertNotNull(parentPhysicalPoolId, "Found parent Physical poolId corresponding to derived poolId '"+derivedPoolId+"'.");
		return parentPhysicalPoolId;
	}
	
	// Data Providers ***********************************************************************

}

