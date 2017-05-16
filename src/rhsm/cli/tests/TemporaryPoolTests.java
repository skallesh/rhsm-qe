package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.SkipException;

import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ConsumerCert;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.github.redhatqe.polarize.metadata.TestDefinition;

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
@Test(groups={"TemporaryPoolTests","Tier3Tests","AcceptanceTests","Tier1Tests"})
public class TemporaryPoolTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-20093", "RHEL7-51735"})
	@Test(	description="given an available unmapped_guests_only pool, assert that it is available only to virtual systems whose host consumer has not yet mapped its virt.uuid as a guestId onto the host consumer.  Moreover, assert that once mapped, the pool is no longer available.",
			groups={},
			dataProvider="getAvailableUnmappedGuestsOnlySubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAvailabilityOfUnmappedGuestsOnlySubpool_Test(Object bugzilla, SubscriptionPool unmappedGuestsOnlyPool) throws JSONException, Exception {
		
		// system facts were overridden in dataProvider with factsMap to fake this system as a guest
		
		// make sure we are freshly registered (to discard a consumer from a former data provided iteration that has mapped guests)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);	
		
		// verify the unmapped_guests_only pool is available for consumption
		List<SubscriptionPool> availableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		Assert.assertNotNull(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId",unmappedGuestsOnlyPool.poolId, availableSubscriptionPools),
				"Temporary pool '"+unmappedGuestsOnlyPool.subscriptionName+"' poolId='"+unmappedGuestsOnlyPool.poolId+"' (for virtual systems whose host consumer has not yet reported this system's virt.uuid as a guest) is available for consumption.");
		
		// verify that it is for Virtual systems
		Assert.assertEquals(unmappedGuestsOnlyPool.machineType, "Virtual","Temporary pools intended for unmapped guests only should indicate that it is for machine type Virtual.");
		
		// verify that the Subscription Type indicates it is temporary
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "0.9.47-1")) {	// commit dfd7e68ae83642f77c80590439353a0d66fe2961	// Bug 1201520 - [RFE] Usability suggestions to better identify a temporary (aka 24 hour) entitlement
			String temporarySuffix = " (Temporary)";
			Assert.assertTrue(unmappedGuestsOnlyPool.subscriptionType.endsWith(temporarySuffix), "The Subscription Type for a temporary pool intended for unmapped guests only should end in suffix '"+temporarySuffix+"' (actual='"+unmappedGuestsOnlyPool.subscriptionType+"').");
		}
		
		// verify that the corresponding Physical pool is also available (when not physical_only)
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
		
		// assert that we are blocked from attempt to attach the temporary pool
		//	201503191231:11.346 - FINE: ssh root@jsefler-os6.usersys.redhat.com subscription-manager subscribe --pool=8a9087e34c2f214a014c2f22a7d11ad0
		//	201503191231:16.011 - FINE: Stdout: Pool is restricted to unmapped virtual guests: '8a9087e34c2f214a014c2f22a7d11ad0'
		//	201503191231:16.014 - FINE: Stderr: 
		//	201503191231:16.016 - FINE: ExitCode: 1
		SSHCommandResult result = clienttasks.subscribe_(null, null, unmappedGuestsOnlyPool.poolId, null, null, null, null, null, null, null, null, null, null);
		String expectedStdout = String.format("Pool is restricted to unmapped virtual guests: '%s'", unmappedGuestsOnlyPool.poolId);
		String expectedStderr = "";
		Assert.assertEquals(result.getStdout().trim(), expectedStdout, "Stdout from an attempt to attach a temporary pool to a virtual guest that has already been mapped.");
		Assert.assertEquals(result.getStderr().trim(), expectedStderr, "Stderr from an attempt to attach a temporary pool to a virtual guest that has already been mapped.");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(1), "Exit code from an attempt to attach a temporary pool to a virtual guest that has already been mapped.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-20094", "RHEL7-51736"})
	@Test(	description="given an available unmapped_guests_only pool, assert that attaching it does not throw any Tracebacks ",
			groups={"blockedByBug-1198369"},
			dataProvider="getAvailableUnmappedGuestsOnlySubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyNoTracebacksAreThrownWhenSubscribingToUnmappedGuestsOnlySubpool_Test(Object bugzilla, SubscriptionPool unmappedGuestsOnlyPool) throws JSONException, Exception {
		
		// system facts were overridden in dataProvider with factsMap to fake this system as a guest
		
		// make sure we are freshly registered (to discard a consumer from a former data provided iteration that has mapped guests)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);	
		
		// subscribe and assert stderr does not report an UnboundLocalError
		SSHCommandResult result = clienttasks.subscribe(null, null, unmappedGuestsOnlyPool.poolId, null, null, null, null, null, null, null, null, null, null);
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-20095", "RHEL7-51737"})
	@Test(	description="given an available unmapped_guests_only pool, attach it and verify the granted entitlement (validityNotAfter date is 24 hours after consumer's registration), installed product (Subscribed), and system status (Insufficient - Guest has not been reported on any host and is using a temporary unmapped guest subscription.)",
			groups={"blockedByBug-1362701"},
			dataProvider="getAvailableUnmappedGuestsOnlySubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyStatusDetailsAfterAttachingUnmappedGuestsOnlySubpool_Test(Object bugzilla, SubscriptionPool unmappedGuestsOnlyPool) throws JSONException, Exception {
		
		// system facts were overridden in dataProvider with factsMap to fake this system as a guest
		
		// make sure we are freshly registered (to discard a consumer from a former data provided iteration that has mapped guests)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);	
		ConsumerCert cert = clienttasks.getCurrentConsumerCert();
		
		// attach the unmapped guests only pool
		//File serialPemFile = clienttasks.subscribeToSubscriptionPool(unmappedGuestsOnlyPool, null, sm_clientUsername, sm_clientPassword, sm_serverUrl);
		//EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(serialPemFile);
		clienttasks.subscribe_(null, null, unmappedGuestsOnlyPool.poolId, null, null, null, null, null, null, null, null, null, null);
		//ProductSubscription consumedUnmappedGuestsOnlyProductSubscription = clienttasks.getCurrentlyConsumedProductSubscriptions().get(0);	// assumes only one consumed entitlement
		ProductSubscription consumedUnmappedGuestsOnlyProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", unmappedGuestsOnlyPool.poolId, clienttasks.getCurrentlyConsumedProductSubscriptions());

		EntitlementCert entitlementCert = clienttasks.getEntitlementCertCorrespondingToProductSubscription(consumedUnmappedGuestsOnlyProductSubscription);

		// assert the expiration is 24 hours post the consumer's registration
		int hours = 24;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.1.1-1")) {	// commit 0704a73dc0d3bf753351e87ca0b65d85a71acfbe 1450079: virt-who temporary subscription should be 7 days
			hours=7/*days*/ * 24/*hours per day*/;
			log.info("Due to Candlepin RFE Bug 1450079, the vailidity period for temporary subscription pools has increased from one day to one week.");
		}
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.30-1")) {	// commit 9302c8f57f37dd5ec3c4020770ac1675a87d99ba 1419576: Pre-date certs to ease clock skew issues
			hours+=1;
			log.info("Due to Candlepin RFE Bug 1419576, we need to increment the expected entitlement validity for the temporary pool by one hour to '"+hours+"' hours after the consumer identity's validityNotBefore date.");
		}
		Calendar expectedEntitlementCertEndDate = (Calendar) cert.validityNotBefore.clone();
		expectedEntitlementCertEndDate.add(Calendar.HOUR, hours);
		//Assert.assertEquals(ConsumerCert.formatDateString(entitlementCert.validityNotAfter), ConsumerCert.formatDateString(expectedEntitlementCertEndDate), "The End Date of the entitlement from a temporary pool should be exactly 24 hours after the registration date of the current consumer '"+ConsumerCert.formatDateString(cert.validityNotBefore)+"'.");
		// allow for a few seconds of tolerance
		Calendar expectedEntitlementCertEndDateUpperTolerance = (Calendar) expectedEntitlementCertEndDate.clone(); expectedEntitlementCertEndDateUpperTolerance.add(Calendar.SECOND, +25);
		Calendar expectedEntitlementCertEndDateLowerTolerance = (Calendar) expectedEntitlementCertEndDate.clone(); expectedEntitlementCertEndDateLowerTolerance.add(Calendar.SECOND, -25);
		Assert.assertTrue(entitlementCert.validityNotAfter.before(expectedEntitlementCertEndDateUpperTolerance) && entitlementCert.validityNotAfter.after(expectedEntitlementCertEndDateLowerTolerance), "The End Date of the entitlement from a temporary pool '"+ConsumerCert.formatDateString(entitlementCert.validityNotAfter)+"' should be '"+hours+"' hours (within several seconds) after the registration date of the current consumer '"+ConsumerCert.formatDateString(cert.validityNotBefore)+"'.");
		
		// assert the Status Details of the attached subscription
		String expectedStatusDetailsForAnUnmappedGuestsOnlyProductSubscription = "Guest has not been reported on any host and is using a temporary unmapped guest subscription.";
		Assert.assertEquals(consumedUnmappedGuestsOnlyProductSubscription.statusDetails, Arrays.asList(new String[]{expectedStatusDetailsForAnUnmappedGuestsOnlyProductSubscription}),"Status Details of a consumed subscription from a temporary pool for unmapped guests only.");
		
		// assert that the temporary subscription appears in the status report
		SSHCommandResult statusResult = clienttasks.status(null, null, null, null, null);
		//	2015-08-13 18:49:28.596  FINE: ssh root@jsefler-7.usersys.redhat.com subscription-manager status
		//	2015-08-13 18:49:30.956  FINE: Stdout: 
		//	+-------------------------------------------+
		//	   System Status Details
		//	+-------------------------------------------+
		//	Overall Status: Insufficient
		//
		//	Red Hat Enterprise Linux for Virtual Datacenters, Premium (DERIVED SKU):
		//	- Guest has not been reported on any host and is using a temporary unmapped guest subscription.
		//
		
		//	2015-08-19 10:33:50.675  FINE: ssh root@ibm-p8-kvm-04-guest-06.rhts.eng.bos.redhat.com subscription-manager status
		//	2015-08-19 10:33:52.757  FINE: Stdout: 
		//	+-------------------------------------------+
		//	   System Status Details
		//	+-------------------------------------------+
		//	Overall Status: Invalid
		//
		//	Red Hat Enterprise Linux for Virtual Datacenters, Premium (DERIVED SKU):
		//	- Guest has not been reported on any host and is using a temporary unmapped guest subscription.
		//
		//	Red Hat Enterprise Linux for Power, little endian:
		//	- Not supported by a valid subscription.
		Map<String,String> statusMap = StatusTests.getProductStatusMapFromStatusResult(statusResult);
		Assert.assertTrue(statusMap.containsKey(consumedUnmappedGuestsOnlyProductSubscription.productName),"The status module reports an incompliance from temporary subscription '"+consumedUnmappedGuestsOnlyProductSubscription.productName+"'.");
		Assert.assertEquals(statusMap.get(consumedUnmappedGuestsOnlyProductSubscription.productName),expectedStatusDetailsForAnUnmappedGuestsOnlyProductSubscription,"The status module reports an incompliance from temporary subscription '"+consumedUnmappedGuestsOnlyProductSubscription.productName+"' for this reason.");
		// assert that the temporary subscription causes an overall status to be Invalid or Insufficient (either is possible and depends on what is installed and if the Temporary pool provides for what is installed)
		Assert.assertTrue(statusResult.getStdout().contains("Overall Status: Invalid")||statusResult.getStdout().contains("Overall Status: Insufficient"), "Expecting Overall Status to be 'Invalid' or 'Insufficient' when a temporary subscription for '"+consumedUnmappedGuestsOnlyProductSubscription.productName+"' is attached (actual value depends on what is installed and if the temporary pool provides for what is installed)");
		
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
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Skipping assertion of the End Date for installed product '"+installedProduct.productName+"' provided by a temporary pool while bug '"+bugId+"' is open.");
				} else	// assert the End Date
				// END OF WORKAROUND			
				Assert.assertEquals(InstalledProduct.formatDateString(installedProduct.endDate), InstalledProduct.formatDateString(entitlementCert.validityNotAfter), "The End Date of coverage for the installed product '"+installedProduct.productName+"' should exactly match the end date from the temporary pool subscription.");

			}
		}
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-20092", "RHEL7-59320"})
	@Test(	description="given an available unmapped_guests_only pool, attach it and attempt to auto-heal - repeated attempts to auto-heal should NOT add more and more entitlements",
			groups={"blockedByBug-1198494"},
			dataProvider="getAvailableUnmappedGuestsOnlySubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutoHealingIsStableAfterAttachingUnmappedGuestsOnlySubpool_Test(Object bugzilla, SubscriptionPool unmappedGuestsOnlyPool) throws JSONException, Exception {
		
		// system facts were overridden in dataProvider with factsMap to fake this system as a guest
		
		// make sure we are freshly registered (to discard a consumer from a former data provided iteration that has mapped guests) and auto-subscribed
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, false, null, null, null, null);	
		
		// attach the unmapped guests only pool
		clienttasks.subscribe_(null, null, unmappedGuestsOnlyPool.poolId, null, null, null, null, null, null, null, null, null, null);
		
		// assert that the temporary subscription appears in the status report
		SSHCommandResult statusResult = clienttasks.status(null, null, null, null, null);
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
		clienttasks.autoheal(null, true, null, null, null, null, null);
		clienttasks.run_rhsmcertd_worker(true);
		
		// get a list of the entitlements after auto-heal
		List<File> entitlementCertFileAfterAutoHeal1 = clienttasks.getCurrentEntitlementCertFiles();
		
		// assert that no additional entitlements were added (test for Bug 1198494 - Auto-heal continuously attaches subscriptions to make the system compliant on a guest machine)
		Assert.assertTrue(entitlementCertFileAfterSubscribed.containsAll(entitlementCertFileAfterAutoHeal1) && entitlementCertFileAfterAutoHeal1.containsAll(entitlementCertFileAfterSubscribed),
				"The entitlement certs remained the same after first attempt to auto-heal despite the attached temporary pool. (Assuming that the initial registration with autosubcribe provided as much coverage as possible for the installed products from subscriptions with plenty of available quantity, repeated attempts to auto-heal should not consume more entitlements.)");

		// trigger a second auto-heal event
		clienttasks.autoheal(null, true, null, null, null, null, null);
		clienttasks.run_rhsmcertd_worker(true);
		
		// get a list of the entitlements after second auto-heal event
		List<File> entitlementCertFileAfterAutoHeal2 = clienttasks.getCurrentEntitlementCertFiles();
		
		// assert that no additional entitlements were added (test for Bug 1198494 - Auto-heal continuously attaches subscriptions to make the system compliant on a guest machine)
		Assert.assertTrue(entitlementCertFileAfterSubscribed.containsAll(entitlementCertFileAfterAutoHeal2) && entitlementCertFileAfterAutoHeal2.containsAll(entitlementCertFileAfterSubscribed),
				"The entitlement certs remained the same after a second attempt to auto-heal despite the attached temporary pool. (Assuming that the initial registration with autosubcribe provided as much coverage as possible for the installed products from subscriptions with plenty of available quantity, repeated attempts to auto-heal should not consume more entitlements.)");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-22237", "RHEL7-59322"})
	@Test(	description="Once a guest is mapped, while consuming a temporary pool entitlement, the entitlement should be removed at the next checkin.  Verify this while autoheal is disabled.",
			groups={"blockedByBug-1198494"},
			dataProvider="getAvailableUnmappedGuestsOnlySubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutomaticRemovalOfAnAttachedUnmappedGuestsOnlySubpoolOnceGuestIsMapped_Test(Object bugzilla, SubscriptionPool unmappedGuestsOnlyPool) throws JSONException, Exception {
		
		// system facts were overridden in dataProvider with factsMap to fake this system as a guest
		
		// make sure we are freshly registered (to discard a consumer from a former data provided iteration that has mapped guests)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);	
		// ensure that auto-healing is off
		clienttasks.autoheal(null, null, true, null, null, null, null);
		
		// attach the unmapped guests only pool
		clienttasks.subscribe_(null, null, unmappedGuestsOnlyPool.poolId, null, null, null, null, null, null, null, null, null, null);
		ProductSubscription consumedUnmappedGuestsOnlyProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", unmappedGuestsOnlyPool.poolId, clienttasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertNotNull(consumedUnmappedGuestsOnlyProductSubscription, "Successfully found the consumed product subscription after attaching temporary pool '"+unmappedGuestsOnlyPool.subscriptionName+"' (poolId='"+unmappedGuestsOnlyPool.poolId+"').");
		
		// map the guest
		clienttasks.mapSystemAsAGuestOfItself();
		
		// trigger a rhsmcertd checkin (either of these calls are valid - randomly choose)
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "1366301";	// Bug 1366301 - Server error attempting a PUT to /subscription/consumers/<UUID>/certificates?lazy_regen=true returned status 404
		boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen && CandlepinType.hosted.equals(sm_serverType)) {
			log.warning("Skipping a random call to refresh local certificates while bug '"+bugId+"' is open.");
			clienttasks.run_rhsmcertd_worker(null);	// no need to pass autoheal option because it is already set true on the consumer
		} else
		// END OF WORKAROUND
		if (getRandomListItem(Arrays.asList(true,false))) 
			clienttasks.refresh(null, null, null, null);
		else
			clienttasks.run_rhsmcertd_worker(null);	// no need to pass autoheal option because it is already set true on the consumer
		
		// verify that the attached temporary subscription has automatically been removed
		List<ProductSubscription> currentlyConsumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertTrue(ProductSubscription.findAllInstancesWithMatchingFieldFromList("poolId", unmappedGuestsOnlyPool.poolId, currentlyConsumedProductSubscriptions).isEmpty(),
				"Now that the guest is mapped, the consumed entitlements from the temporary pool '"+unmappedGuestsOnlyPool.subscriptionName+"' (poolId='"+unmappedGuestsOnlyPool.poolId+"') have automatically been removed.");
		Assert.assertTrue(currentlyConsumedProductSubscriptions.isEmpty(),
				"Now that the guest is mapped (and autoheal was off at the instant the guest was mapped), not only is the temporary entitlement removed, but no new entitlements are granted.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-22236", "RHEL7-59321"})
	@Test(	description="Once a guest is mapped, while consuming a temporary pool entitlement, the entitlement should be removed and the system auto-healed at the next checkin.  Verify it.",
			groups={"blockedByBug-1198494"},
			dataProvider="getAvailableUnmappedGuestsOnlySubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutoHealingOfAnAttachedUnmappedGuestsOnlySubpoolOnceGuestIsMapped_Test(Object bugzilla, SubscriptionPool unmappedGuestsOnlyPool) throws JSONException, Exception {
		
		// system facts were overridden in dataProvider with factsMap to fake this system as a guest
		
		// make sure we are freshly registered (to discard a consumer from a former data provided iteration that has mapped guests)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);	
		// ensure that auto-healing is on
		clienttasks.autoheal(null, true, null, null, null, null, null);
		
		// attach the unmapped guests only pool
		clienttasks.subscribe_(null, null, unmappedGuestsOnlyPool.poolId, null, null, null, null, null, null, null, null, null, null);
		ProductSubscription consumedUnmappedGuestsOnlyProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", unmappedGuestsOnlyPool.poolId, clienttasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertNotNull(consumedUnmappedGuestsOnlyProductSubscription, "Successfully found the consumed product subscription after attaching temporary pool '"+unmappedGuestsOnlyPool.subscriptionName+"' (poolId='"+unmappedGuestsOnlyPool.poolId+"').");
		
		// map the guest
		clienttasks.mapSystemAsAGuestOfItself();
		
		// trigger a rhsmcertd checkin (either of these calls are valid - randomly choose)
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "1366301";	// Bug 1366301 - Server error attempting a PUT to /subscription/consumers/<UUID>/certificates?lazy_regen=true returned status 404
		boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen && CandlepinType.hosted.equals(sm_serverType)) {
			log.warning("Skipping a random call to refresh local certificates while bug '"+bugId+"' is open.");
			clienttasks.run_rhsmcertd_worker(null);	// no need to pass autoheal option because it is already set true on the consumer
		} else
		// END OF WORKAROUND
		if (getRandomListItem(Arrays.asList(true,false))) 
			clienttasks.refresh(null, null, null, null);
		else
			clienttasks.run_rhsmcertd_worker(null);	// no need to pass autoheal option because it is already set true on the consumer
		
		// verify that the attached temporary subscription has automatically been removed
		Assert.assertTrue(ProductSubscription.findAllInstancesWithMatchingFieldFromList("poolId", unmappedGuestsOnlyPool.poolId, clienttasks.getCurrentlyConsumedProductSubscriptions()).isEmpty(),
				"Now that the guest is mapped, the consumed entitlements from the temporary pool '"+unmappedGuestsOnlyPool.subscriptionName+"' (poolId='"+unmappedGuestsOnlyPool.poolId+"') have automatically been removed.");
		
		// assert that we have been autohealed as well as possible
		List<File> entitlementCertFileAfterMapping = clienttasks.getCurrentEntitlementCertFiles();
		clienttasks.subscribe(true, null, (String) null, null, null, null, null, null, null, null, null, null, null);
		List<File> entitlementCertFileAfterAutosubscribing = clienttasks.getCurrentEntitlementCertFiles();
		Assert.assertTrue(entitlementCertFileAfterMapping.containsAll(entitlementCertFileAfterAutosubscribing) && entitlementCertFileAfterAutosubscribing.containsAll(entitlementCertFileAfterMapping),
			"When the entitlement certs on the system are identical after a guest has been mapped and auto-healed and an explicit auto-subscribe is attempted, then we are confident that the guest was auto-healed at the instant the guest was mapped.)");
	}
	
	
	@Test(	description="Consume a temporary pool entitlement and wait a day for it to expire, then assert its removal and assert the pool is not longer available to this consumer.",
			groups={"blockedByBug-1199078","VerifyExpirationOfUnmappedGuestsOnlySubpool_Test"},
			dataProvider="getAvailableUnmappedGuestsOnlySubscriptionPoolsData",
			enabled=false)	// TODO Temporarily disabling this test because changing the system clock during an automated test seems to knock the system off the network during runs on Jenkins thereby loosing ssh connection; service network restarts are needed
	//@ImplementsNitrateTest(caseId=)
	public void VerifyExpirationOfUnmappedGuestsOnlySubpool_Test(Object bugzilla, SubscriptionPool unmappedGuestsOnlyPool) throws JSONException, Exception {
		if (!CandlepinType.standalone.equals(sm_serverType)) throw new SkipException("This automated test should only be attempted on a standalone server.");
		
		// system facts are overridden with factsMap to fake this system as a guest
		
		// reset the date on the client and server
		resetDatesAfterVerifyExpirationOfUnmappedGuestsOnlySubpool_Test();
		
		// make sure we are freshly registered (to discard a consumer from a former data provided iteration that has mapped guests)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null/* autoheal defaults to true*/, null, null, null, null);	
		
		// attach the unmapped guests only pool
		clienttasks.subscribe_(null, null, unmappedGuestsOnlyPool.poolId, null, null, null, null, null, null, null, null, null, null);
		ProductSubscription consumedUnmappedGuestsOnlyProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", unmappedGuestsOnlyPool.poolId, clienttasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertNotNull(consumedUnmappedGuestsOnlyProductSubscription, "Successfully found the consumed product subscription after attaching temporary pool '"+unmappedGuestsOnlyPool.subscriptionName+"' (poolId='"+unmappedGuestsOnlyPool.poolId+"').");

		// advance the date on the client and server
		RemoteFileTasks.runCommandAndAssert(client, String.format("date -s +%dhours",24), 0); clientHoursFastForwarded+=24;
		RemoteFileTasks.runCommandAndAssert(server, String.format("date -s +%dhours",24), 0); serverHoursFastForwarded+=24;
		
		// assert that the list of consumedUnmappedGuestsOnlyProductSubscription now appears expired  (Active: False, Status Details: Subscription is expired)
		ProductSubscription expiredUnmappedGuestsOnlyProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", unmappedGuestsOnlyPool.poolId, clienttasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertFalse(expiredUnmappedGuestsOnlyProductSubscription.isActive, "The value of Active shown on the consumed temporary product subscription 24 hours after pool '"+unmappedGuestsOnlyPool.poolId+"' was attached.");
		Assert.assertEquals(expiredUnmappedGuestsOnlyProductSubscription.statusDetails, Arrays.asList("Subscription is expired"), "The Status Details shown on the consumed temporary product subscription 24 hours after pool '"+unmappedGuestsOnlyPool.poolId+"' was attached.");

		// catch Bug 1201727 - After the 24 hour pool is expired,consumed --list displays a value,even after attaching a subscription
		// TODO
		
		// assert the installed product appears expired (Active: False, Status Details: Subscription is expired)
		// TODO
		
		// trigger an autohealing rhsmcertd checkin (assumes that autoheal defaults to true on a newly registered consumer)
		clienttasks.run_rhsmcertd_worker(true);	// must pass autoheal=true
		
		// assert the expired entitlement is immediately removed when autohealing is run	// Bug 1199078 - expired guest 24 hour subscription not removed on auto-attach
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "0.9.46-1")) {	// commit d24a59b3640aef1acb2b6067100d653fc76636f5	1199078: Remove expired unmapped guest pools on autoheal
			Assert.assertNull(ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", unmappedGuestsOnlyPool.poolId, clienttasks.getCurrentlyConsumedProductSubscriptions()),
				"After an autohealing rhsmcertd checkin, the expired temporary product subscription should be immediately removed from the system.");
		}
		
		// verify that the temporary unmapped_guests_only pool is no longer available for consumption
		Assert.assertNull(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId",unmappedGuestsOnlyPool.poolId, clienttasks.getCurrentlyAvailableSubscriptionPools()),
				"Temporary pool '"+unmappedGuestsOnlyPool.subscriptionName+"' poolId='"+unmappedGuestsOnlyPool.poolId+"' is NO LONGER available for consumption 24 hours after the unmapped guest consumer registered.");
		
		// assert that we are blocked from an attempt to attach the temporary pool 24 hours after the consumer registered
		//	201503191706:00.370 - FINE: ssh root@jsefler-os6.usersys.redhat.com subscription-manager subscribe --pool=8a9087e34c335894014c3359e22517fa
		//	201503191706:03.965 - FINE: Stdout: Pool is restricted to virtual guests in their first day of existence: '8a9087e34c335894014c3359e22517fa'
		//	201503191706:03.968 - FINE: Stderr: 
		//	201503191706:03.970 - FINE: ExitCode: 1
		SSHCommandResult result = clienttasks.subscribe_(null, null, unmappedGuestsOnlyPool.poolId, null, null, null, null, null, null, null, null, null, null);
		String expectedStdout = String.format("Pool is restricted to virtual guests in their first day of existence: '%s'", unmappedGuestsOnlyPool.poolId);
		String expectedStderr = "";
		Assert.assertEquals(result.getStdout().trim(), expectedStdout, "Stdout from an attempt to attach a temporary pool to a virtual guest 24 hours after the guest registered.");
		Assert.assertEquals(result.getStderr().trim(), expectedStderr, "Stderr from an attempt to attach a temporary pool to a virtual guest 24 hours after the guest registered.");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(1), "Exit code from an attempt to attach a temporary pool to a virtual guest 24 hours after the guest registered.");
	}
	protected int clientHoursFastForwarded = 0;
	protected int serverHoursFastForwarded = 0;
	@AfterGroups(value={"VerifyExpirationOfUnmappedGuestsOnlySubpool_Test"},groups={"setup"})
	public void resetDatesAfterVerifyExpirationOfUnmappedGuestsOnlySubpool_Test() {
		// reset the date on the client and server
		if (client!=null) {
			RemoteFileTasks.runCommandAndAssert(client, String.format("date -s -%dhours",clientHoursFastForwarded), 0);
			clientHoursFastForwarded-=clientHoursFastForwarded;
		}
		if (server!=null) {
			RemoteFileTasks.runCommandAndAssert(server, String.format("date -s -%dhours",serverHoursFastForwarded), 0);
			serverHoursFastForwarded-=serverHoursFastForwarded;
		}
	}
	@AfterClass(groups={"setup"})
	public void assertTheClientAndServerDatesAfterClass() {
		Assert.assertEquals(clientHoursFastForwarded, 0, "The net number of hours the client date has been fast forwarded and rewound during expiration testing in this class.  If this fails, the client date is probably wrong");
		Assert.assertEquals(serverHoursFastForwarded, 0, "The net number of hours the server date has been fast forwarded and rewound during expiration testing in this class.  If this fails, the server date is probably wrong");
		if (client!=null) RemoteFileTasks.runCommandAndAssert(client, "service ntpd start", 0);
		if (server!=null) RemoteFileTasks.runCommandAndAssert(server, "service ntpd start", 0);
	}
	@BeforeClass(groups={"setup"})
	public void stopTheTimeServerBeforeClass() {
		if (client!=null) RemoteFileTasks.runCommandAndAssert(client, "service ntpd stop", 0);
		if (server!=null) RemoteFileTasks.runCommandAndAssert(server, "service ntpd stop", 0);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	// Candidates for an automated Test:
	
	
	
	
	// Configuration methods ***********************************************************************

	@AfterClass(groups={"setup"})
	public void afterVerifyAvailabilityOfDerivedProductSubpool_Test() {
//		clienttasks.unsubscribeFromTheCurrentlyConsumedSerialsCollectively();	// will avoid: Runtime Error No row with the given identifier exists: [org.candlepin.model.PoolAttribute#8a99f98a46b4fa990146ba9494032318] at org.hibernate.UnresolvableObjectException.throwIfNull:64
		clienttasks.unregister(null,null,null, null);
		clienttasks.deleteFactsFileWithOverridingValues();
	}

	
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
		
		// note: getAvailableSubscriptionPoolsDataAsListOfLists(...) will ensure the system is newly registered
		for (List<Object> list : getAvailableSubscriptionPoolsDataAsListOfLists(false)) {
			SubscriptionPool pool = (SubscriptionPool)(list.get(0));
			
			if (CandlepinTasks.isPoolRestrictedToUnmappedVirtualSystems(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId)) {
				//									Object bugzilla,	SubscriptionPool unmappedGuestsOnlyPool
				ll.add(Arrays.asList(new Object[]{null,	pool}));
			}
		}
		return ll;
	}
	Map<String,String> factsMap = new HashMap<String,String>();
}

