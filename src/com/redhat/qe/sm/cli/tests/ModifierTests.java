package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.LogMessageUtil;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.ContentNamespace;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/* Notes...
<jsefler> I'm trying to strategize an automated test for the virt entitlements stuff you demo'ed on Wednesday.  I got a few questions to start with...
<jharris> sure
 shoot
<jsefler> using the RESTapi, if I search through all the owners subscriptions and find one with a virt_limit attribute, then that means that two pools should get created corresponding to it.  correct?
 one pool for the host and one pool fir the guests
<jharris> yes
 specifically the attribute is on either the product or the pool
<jsefler> what does that mean?
 the virt_limit is an attribute of the product - that I know
 next I need to figure out what the relevant attributes are on the pool
<jharris> pools have attributes
 products have attributes
 the two pools are created, as you said
 the physical (host) pool will have no additional attributes
 the virt (guest) pool will have an attribute of "virt_only" set to true
 the candlepin logic should only let virtual machines subscribe to that second pool
 this is done by checking the virt.is_guest fact
 that is set in subscription manager
<jsefler> yup - that sounds good - that's what I need to get started
<jharris> excellent
 but the virt_only attribute can also just be used on a product, for example
 so that maybe we want to start selling a product that is like RHEL for virtual machines
 IT can just stick that virt_only attribute on the product directly
 and it should do the same filtering
 */

/**
 * @author jsefler
 *
 */
@Test(groups="ModifierTests")
public class ModifierTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************
	

	
	
	
	
	@Test(	description="verify content label for modifier subscription (EUS) is only available in yum repolist when providing subscriptions are entitled",
			groups={},
			dependsOnGroups={},
			dataProvider="getModifierSubscriptionData",
			enabled=true)
	public void VerifyContentLabelForModifierSubscriptionIsOnlyAvailableInYumRepoListWhenProvidingPoolsAreSubscribed(SubscriptionPool modifierPool, String label, List<String> modifiedProductIds, String requiredTags, List<SubscriptionPool> providingPools) throws JSONException, Exception {
		
		// make sure we are not subscribed to anything
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();

		boolean areAllRequiredTagsProvided = clienttasks.areAllRequiredTagsProvidedByProductCerts(requiredTags, clienttasks.getCurrentProductCerts());

		log.info("Before subscribing to anything, assert that the label (repo id) '"+label+"' is not available.");
		Assert.assertFalse(clienttasks.yumRepolist("all").contains(label),
				"Before beginning our test, yum repolist all should exclude label (repo id) '"+label+"'.");

		log.info("Now subscribe to the modifier pool and assert that the label (repo id) '"+label+"' is still not available.");
		clienttasks.subscribeToSubscriptionPool(modifierPool);
		Assert.assertFalse(clienttasks.yumRepolist("all").contains(label),
				"After subscribing to modifier pool for productId '"+modifierPool.productId+"', yum repolist all should not include (repo id) '"+label+"' because the subscribing product(s) being modified is not yet subscribed to.");

		log.info("Now individually subscribe to each of the subscribing products being modified and assert that once both the modifier pool and product being modified are both subscribed, then the modifier (repo id) '"+label+"' will become available.");
		for (SubscriptionPool providingPool : providingPools) {
			EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.subscribeToSubscriptionPool(providingPool));
			if (areAllRequiredTagsProvided) {
				Assert.assertTrue(clienttasks.yumRepolist("all").contains(label),
					"Having subscribed to both the modifier pool and its providing pool, now the modifier pool's (repo id) '"+label+"' is available in yum repolist all.");
			} else {
				Assert.assertFalse(clienttasks.yumRepolist("all").contains(label),
						"Because not all of the requiredTags '"+requiredTags+"' for content label '"+label+"' are not 100% provided by the currently installed product certs, we should be blocked from seeing the repo id label '"+label+"' in yum repolist all.");				
			}
			clienttasks.unsubscribeFromSerialNumber(entitlementCert.serialNumber);
			Assert.assertFalse(clienttasks.yumRepolist("all").contains(label),
					"After unsubscribing from the providing pool for productId '"+providingPool.productId+"', yum repolist all should no longer include (repo id) '"+label+"' from modifier productId '"+modifierPool.productId+"'.");
		}
		
		log.info("Now let's subscribe to the providing pools first before subscribing to the modifier.");
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		Assert.assertFalse(clienttasks.yumRepolist("all").contains(label),
				"Yum repolist all should exclude label (repo id) '"+label+"' since we are not subscribed to anything.");
		for (SubscriptionPool providingPool : providingPools) {
			clienttasks.subscribeToSubscriptionPool(providingPool);
		}
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.subscribeToSubscriptionPool(modifierPool));
		if (areAllRequiredTagsProvided) {
			Assert.assertTrue(clienttasks.yumRepolist("all").contains(label),
					"Having subscribed to all of its possible providing pools and the modifier pool, the modifier pool's (repo id) '"+label+"' should immediately be available in yum repolist all.");
		} else {
			Assert.assertFalse(clienttasks.yumRepolist("all").contains(label),
					"Because not all of the requiredTags '"+requiredTags+"' for content label '"+label+"' are not 100% provided by the currently installed product certs, we should be blocked from seeing the repo id label '"+label+"' in yum repolist all.");
		}
		clienttasks.unsubscribeFromSerialNumber(entitlementCert.serialNumber);
		Assert.assertFalse(clienttasks.yumRepolist("all").contains(label),
				"After unsubscribing from the modifier pool, yum repolist all should no longer include (repo id) '"+label+"' from modifier productId '"+modifierPool.productId+"'.");
	
		if (!areAllRequiredTagsProvided) {
			throw new SkipException("We cannot claim success on this test until 100% of the requiredTags '"+requiredTags+"' are provided by the currently install products.");
		}


	}
//	
//	
//	@Test(	description="Verify host and guest pools quantities generated from a virtualization-aware subscription",
//			groups={}, // "blockedByBug-679617" indirectly when this script is run as part of the full TestNG suite since this is influenced by other scripts calling refresh pools
//			dependsOnGroups={},
//			dataProvider="getVirtSubscriptionData",
//			enabled=true)
//	public void VerifyHostAndGuestPoolQuantities_Test(String subscriptionId, String productName, String productId, int quantity, String virtLimit, String hostPoolId, String guestPoolId) throws JSONException, Exception {
//		if (hostPoolId==null && guestPoolId==null) throw new SkipException("Failed to find expected host and guest pools derived from virtualization-aware subscription id '"+subscriptionId+"' ("+productName+").");
//
//		// get the hostPool
//		List<SubscriptionPool> allAvailablePools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
//		SubscriptionPool hostPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", hostPoolId, allAvailablePools);
//		Assert.assertNotNull(hostPool,"A host pool derived from the virtualization-aware subscription id '"+subscriptionId+"' is listed in all available subscriptions: "+hostPool);
//
//		// assert hostPoolId quantity
//		Assert.assertEquals(Integer.valueOf(hostPool.quantity), Integer.valueOf(quantity), "Assuming that nobody else is consuming from this host pool '"+hostPool.poolId+"', the maximum quantity of available entitlements should be "+quantity+".");
//		
//		// get the guestPool
//		SubscriptionPool guestPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", guestPoolId, allAvailablePools);
//		Assert.assertNotNull(guestPool,"A guest pool derived from the virtualization-aware subscription id '"+subscriptionId+"' is listed in all available subscriptions: "+guestPool);
//
//		// assert guestPoolId quantity
//		Assert.assertEquals(Integer.valueOf(guestPool.quantity), Integer.valueOf(quantity*Integer.valueOf(virtLimit)), "Assuming that nobody else is consuming from this guest pool '"+guestPool.poolId+"', the maximum quantity of available entitlements should be the virt_limit of '"+virtLimit+"' times the host quantity '"+quantity+"'.");
//	}
//	
//	
//	@Test(	description="Verify the virt_limit multiplier on guest pool quantity is not clobbered by refresh pools",
//			groups={"blockedByBug-679617"},
//			dependsOnGroups={},
//			dependsOnMethods={"VerifyHostAndGuestPoolQuantities_Test"},
//			dataProvider="getVirtSubscriptionData",
//			enabled=true)
//	public void VerifyGuestPoolQuantityIsNotClobberedByRefreshPools_Test(String subscriptionId, String productName, String productId, int quantity, String virtLimit, String hostPoolId, String guestPoolId) throws JSONException, Exception {
//		if (hostPoolId==null && guestPoolId==null) throw new SkipException("Failed to find expected host and guest pools derived from virtualization-aware subscription id '"+subscriptionId+"' ("+productName+").");
//
//		// get the hostPool
//		List<SubscriptionPool> allAvailablePools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
//		SubscriptionPool hostPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", hostPoolId, allAvailablePools);
//		Assert.assertNotNull(hostPool,"A host pool derived from the virtualization-aware subscription id '"+subscriptionId+"' is listed in all available subscriptions: "+hostPool);
//
//		// remember the hostPool quantity before calling refresh pools
//		String hostPoolQuantityBefore = hostPool.quantity;
//		
//		// get the guestPool
//		SubscriptionPool guestPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", guestPoolId, allAvailablePools);
//		Assert.assertNotNull(guestPool,"A guest pool derived from the virtualization-aware subscription id '"+subscriptionId+"' is listed in all available subscriptions: "+guestPool);
//
//		// remember the hostPool quantity before calling refresh pools
//		String guestPoolQuantityBefore = guestPool.quantity;
//
//		log.info("Now let's modify the start date of the virtualization-aware subscription id '"+subscriptionId+"'...");
//		JSONArray jsonSubscriptions = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,clientusername,clientpassword,"/owners/"+ownerKey+"/subscriptions"));	
//		JSONObject jsonSubscription = null;
//		for (int i = 0; i < jsonSubscriptions.length(); i++) {
//			jsonSubscription = (JSONObject) jsonSubscriptions.get(i);
//			if (jsonSubscription.getString("id").equals(subscriptionId)) {break;} else {jsonSubscription=null;}
//		}
//		Calendar startDate = parseDateString(jsonSubscription.getString("startDate"));	// "startDate":"2012-02-08T00:00:00.000+0000"
//		Calendar newStartDate = (Calendar) startDate.clone(); newStartDate.add(Calendar.MONTH, -1);	// subtract a month
//		updateSubscriptionDatesOnDatabase(subscriptionId,newStartDate,null);
//
//		log.info("Now let's refresh the subscription pools...");
//		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,serverAdminUsername,serverAdminPassword, ownerKey);
//		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,serverAdminUsername,serverAdminPassword, jobDetail, "FINISHED", 10*1000, 3);
//		allAvailablePools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
//
//		// retrieve the host pool again and assert the quantity has not changed
//		hostPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", hostPoolId, allAvailablePools);
//		Assert.assertEquals(hostPool.quantity, hostPoolQuantityBefore, "The quantity of entitlements available from the host pool has NOT changed after refreshing pools.");
//		
//		// retrieve the guest pool again and assert the quantity has not changed
//		guestPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", guestPoolId, allAvailablePools);
//		Assert.assertEquals(guestPool.quantity, guestPoolQuantityBefore, "The quantity of entitlements available from the guest pool has NOT changed after refreshing pools.");
//	}
//	
//	
//	@Test(	description="Verify host and guest pools to a virtualization-aware subscription are subscribable on a guest system.",
//			groups={},
//			dependsOnGroups={},
//			dataProvider="getVirtSubscriptionData",
//			enabled=true)
//	public void VerifyHostAndGuestPoolsAreSubscribableOnGuestSystem_Test(String subscriptionId, String productName, String productId, int quantity, String virtLimit, String hostPoolId, String guestPoolId) throws JSONException, Exception {
//		if (hostPoolId==null && guestPoolId==null) throw new SkipException("Failed to find expected host and guest pools derived from virtualization-aware subscription id '"+subscriptionId+"' ("+productName+").");
//
//		// trick this system into believing it is a virt guest
//		forceVirtWhatToReturnGuest("kvm");
//		
//		// assert that the hostPoolId is available
//		List<SubscriptionPool> availablePools = clienttasks.getCurrentlyAvailableSubscriptionPools();
//		SubscriptionPool hostPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", hostPoolId, availablePools);
//		Assert.assertNotNull(hostPool,"A host pool derived from the virtualization-aware subscription id '"+subscriptionId+"' is available on a guest system: "+hostPool);
//		
//		// attempt to subscribe to the hostPoolId
//		clienttasks.subscribeToSubscriptionPool(hostPool);
//		
//		// assert that the guestPoolId is available
//		SubscriptionPool guestPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", guestPoolId, availablePools);
//		Assert.assertNotNull(guestPool,"A guest pool derived from the virtualization-aware subscription id '"+subscriptionId+"' is available on a guest system: "+guestPool);
//
//		// attempt to subscribe to the guestPoolId
//		clienttasks.subscribeToSubscriptionPool(guestPool);
//	}
//	
//	
//	@Test(	description="Verify only the derived host pool from a virtualization-aware subscription is subscribable on a host system.  The guest pool should not be available nor subscribable.",
//			groups={},
//			dependsOnGroups={},
//			dataProvider="getVirtSubscriptionData",
//			enabled=true)
//	public void VerifyHostPoolIsSubscribableOnHostSystemWhileGuestPoolIsNot_Test(String subscriptionId, String productName, String productId, int quantity, String virtLimit, String hostPoolId, String guestPoolId) throws JSONException, Exception {
//		if (hostPoolId==null && guestPoolId==null) throw new SkipException("Failed to find expected host and guest pools derived from virtualization-aware subscription id '"+subscriptionId+"' ("+productName+").");
//
//		// trick this system into believing it is a host
//		forceVirtWhatToReturnHost();
//		
//		// assert that the hostPoolId is available
//		List<SubscriptionPool> availablePools = clienttasks.getCurrentlyAvailableSubscriptionPools();
//		SubscriptionPool hostPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", hostPoolId, availablePools);
//		Assert.assertNotNull(hostPool,"A host pool derived from the virtualization-aware subscription id '"+subscriptionId+"' is available on a host system: "+hostPool);
//
//		// assert that the guestPoolId is NOT available
//		SubscriptionPool guestPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", guestPoolId, availablePools);
//		Assert.assertNull(guestPool,"A guest pool derived from the virtualization-aware subscription id '"+subscriptionId+"' should NOT be available on a host system: "+guestPool);
//
//		// attempt to subscribe to the hostPoolId
//		clienttasks.subscribeToSubscriptionPool(hostPool);
//
//		// attempt to subscribe to the guestPoolId (should be blocked)
//		SSHCommandResult result = clienttasks.subscribe(null,guestPoolId,null,null,null,null,null,null, null);
//		// Unable to entitle consumer to the pool with id '8a90f8b42e3e7f2e012e3e7fc653013e': rulefailed.virt.only
//		Assert.assertContainsMatch(result.getStdout(), "^Unable to entitle consumer to the pool with id '"+guestPoolId+"':");
//	}
	


	
	
	// Candidates for an automated Test:
	// 
	
	
	
	
	// Configuration methods ***********************************************************************
		
//	@BeforeClass(groups="setup")
//	public void backupVirtWhatBeforeClass() {
//		// finding location of virt-what...
//		SSHCommandResult result = client.runCommandAndWait("which virt-what");
//		virtWhatFile = new File(result.getStdout().trim());
//		Assert.assertTrue(RemoteFileTasks.testFileExists(client, virtWhatFile.getPath())==1,"virt-what is in the client's path");
//		
//		// making a backup of virt-what...
//		virtWhatFileBackup = new File(virtWhatFile.getPath()+".bak");
//		RemoteFileTasks.runCommandAndAssert(client, "cp -n "+virtWhatFile+" "+virtWhatFileBackup, 0);
//		Assert.assertTrue(RemoteFileTasks.testFileExists(client, virtWhatFileBackup.getPath())==1,"successfully made a backup of virt-what to: "+virtWhatFileBackup);
//
//	}
//	
//	@AfterClass(groups="setup")
//	public void restoreVirtWhatAfterClass() {
//		// restoring backup of virt-what
//		if (virtWhatFileBackup!=null && RemoteFileTasks.testFileExists(client, virtWhatFileBackup.getPath())==1) {
//			RemoteFileTasks.runCommandAndAssert(client, "mv -f "+virtWhatFileBackup+" "+virtWhatFile, 0);
//		}
//	}
//	
	@BeforeClass(groups="setup")
	public void registerBeforeClass() throws Exception {
		clienttasks.unregister(null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null));
		
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(serverHostname, serverPort, serverPrefix, clientusername, clientpassword, consumerId);
	}
//	
//	@BeforeMethod(groups="setup")
//	public void unsubscribeBeforeMethod() throws Exception {
//		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
//	}
	
	// protected methods ***********************************************************************
	
	protected String ownerKey = "";
//	protected File virtWhatFile = null;
//	protected File virtWhatFileBackup = null;
//	
//	protected Calendar parseDateString(String dateString){
//		String simpleDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"; //"2012-02-08T00:00:00.000+0000"
//		try{
//			DateFormat dateFormat = new SimpleDateFormat(simpleDateFormat);
//			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
//			Calendar calendar = new GregorianCalendar();
//			calendar.setTimeInMillis(dateFormat.parse(dateString).getTime());
//			return calendar;
//		}
//		catch (ParseException e){
//			log.warning("Failed to parse GMT date string '"+dateString+"' with format '"+simpleDateFormat+"':\n"+e.getMessage());
//			return null;
//		}
//	}

	
//	protected void forceVirtWhatToReturnGuest(String hypervisorType) {
//		// Note: when client is a guest, virt-what returns stdout="<hypervisor type>" and exitcode=0
//		RemoteFileTasks.runCommandAndWait(client,"echo '#!/bin/bash - ' > "+virtWhatFile+"; echo 'echo "+hypervisorType+"' >> "+virtWhatFile+"; chmod a+x "+virtWhatFile, LogMessageUtil.action());
//	}
//	
//	protected void forceVirtWhatToReturnHost() {
//		// Note: when client is a host, virt-what returns stdout="" and exitcode=0
//		RemoteFileTasks.runCommandAndWait(client,"echo '#!/bin/bash - ' > "+virtWhatFile+"; echo 'exit 0' >> "+virtWhatFile+"; chmod a+x "+virtWhatFile, LogMessageUtil.action());
//	}
//	
//	protected void forceVirtWhatToFail() {
//		// Note: when virt-what does not know if the system is on bare metal or on a guest, it returns a non-zero value
//		RemoteFileTasks.runCommandAndWait(client,"echo '#!/bin/bash - ' > "+virtWhatFile+"; echo 'echo \"virt-what is about to exit with code 255\"; exit 255' >> "+virtWhatFile+"; chmod a+x "+virtWhatFile, LogMessageUtil.action());
//	}
	
	
	
	// Data Providers ***********************************************************************
	
//	@DataProvider(name="getVirtWhatData")
//	public Object[][] getVirtWhatDataAs2dArray() {
//		return TestNGUtils.convertListOfListsTo2dArray(getVirtWhatDataAsListOfLists());
//	}
//	protected List<List<Object>> getVirtWhatDataAsListOfLists(){
//		List<List<Object>> ll = new ArrayList<List<Object>>();
//
//		// man virt-what  (virt-what-1.3) shows support for the following hypervisors
//		ll.add(Arrays.asList(new Object[]{"openvz"}));
//		ll.add(Arrays.asList(new Object[]{"kvm"}));
//		ll.add(Arrays.asList(new Object[]{"qemu"}));
//		ll.add(Arrays.asList(new Object[]{"uml"}));
//		ll.add(Arrays.asList(new Object[]{"virtualbox"}));
//		ll.add(Arrays.asList(new Object[]{"virtualpc"}));
//		ll.add(Arrays.asList(new Object[]{"vmware"}));
//		ll.add(Arrays.asList(new Object[]{"xen"}));
//		ll.add(Arrays.asList(new Object[]{"xen-dom0"}));
//		ll.add(Arrays.asList(new Object[]{"xen-domU"}));
//		ll.add(Arrays.asList(new Object[]{"xen-hvm"}));
//
//		return ll;
//	}
	
	
	@DataProvider(name="getModifierSubscriptionData")
	public Object[][] getModifierSubscriptionDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getModifierSubscriptionDataAsListOfLists());
	}
	protected List<List<Object>> getModifierSubscriptionDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		List<SubscriptionPool> allAvailablePools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
//		Calendar now = new GregorianCalendar();
//		now.setTimeInMillis(System.currentTimeMillis());
		
		
		// iterate through all available pools looking for those that contain products with content that modify other products
		for (SubscriptionPool modifierPool : allAvailablePools) {
			JSONObject jsonModifierPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,clientusername,clientpassword,"/pools/"+modifierPool.poolId));	
			
			// iterate through each of the providedProducts
			JSONArray jsonModifierProvidedProducts = jsonModifierPool.getJSONArray("providedProducts");
			for (int i = 0; i < jsonModifierProvidedProducts.length(); i++) {
				JSONObject jsonModifierProvidedProduct = (JSONObject) jsonModifierProvidedProducts.get(i);
				String modifierProvidedProductId = jsonModifierProvidedProduct.getString("productId");
				
				// get the productContents
				JSONObject jsonProduct = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,clientusername,clientpassword,"/products/"+modifierProvidedProductId));	
				JSONArray jsonProductContents = jsonProduct.getJSONArray("productContent");
				for (int j = 0; j < jsonProductContents.length(); j++) {
					JSONObject jsonProductContent = (JSONObject) jsonProductContents.get(j);
					JSONObject jsonContent = jsonProductContent.getJSONObject("content");
					
					// get the label and modifiedProductIds for each of the productContents
					String label = jsonContent.getString("label");
					String requiredTags = jsonContent.getString("requiredTags"); // comma separated string
					JSONArray jsonModifiedProductIds = jsonContent.getJSONArray("modifiedProductIds");
					List<String> modifiedProductIds = new ArrayList<String>();
					for (int k = 0; k < jsonModifiedProductIds.length(); k++) {
						String modifiedProductId = (String) jsonModifiedProductIds.get(k);
						modifiedProductIds.add(modifiedProductId);
					}

					
					
					// does this pool contain productContents that modify other products?
					if (modifiedProductIds.size()>0) {
						
						List<SubscriptionPool> providingPools = new ArrayList<SubscriptionPool>();
						// yes, now its time to find the subscriptions that provide the modifiedProductIds
						for (SubscriptionPool providingPool : allAvailablePools) {
							JSONObject jsonProvidingPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,clientusername,clientpassword,"/pools/"+providingPool.poolId));	
							
							// iterate through each of the providedProducts
							JSONArray jsonProvidingProvidedProducts = jsonProvidingPool.getJSONArray("providedProducts");
							for (int l = 0; l < jsonProvidingProvidedProducts.length(); l++) {
								JSONObject jsonProvidingProvidedProduct = (JSONObject) jsonProvidingProvidedProducts.get(l);
								String providingProvidedProductId = jsonProvidingProvidedProduct.getString("productId");
								if (modifiedProductIds.contains(providingProvidedProductId)) {
									providingPools.add(providingPool);
								}
							}

						}
						
						
						
						ll.add(Arrays.asList(new Object[]{modifierPool, label, modifiedProductIds, requiredTags, providingPools}));
					}
				}
			}
		}
		

		
		return ll;
	}
	
/*
Example jsonPool:
[root@jsefler-onprem04 tmp]# curl -u testuser1:password -k https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/pools/8a90f8b42eb9b5b8012eb9b6439f0219 | python -mjson.tool  
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
103   828    0   828    0     0   3883      0 --:--:-- --:--:-- --:--:--  7527
{
    "accountNumber": "12331131231", 
    "activeSubscription": true, 
    "attributes": [], 
    "consumed": 0, 
    "contractNumber": "0", 
    "created": "2011-03-15T13:30:53.215+0000", 
    "endDate": "2012-03-14T00:00:00.000+0000", 
    "href": "/pools/8a90f8b42eb9b5b8012eb9b6439f0219", 
    "id": "8a90f8b42eb9b5b8012eb9b6439f0219", 
    "owner": {
        "href": "/owners/admin", 
        "id": "8a90f8b42eb9b5b8012eb9b5cb9a0005"
    }, 
    "productId": "awesomeos-modifier", 
    "productName": "Awesome OS Modifier", 
    "providedProducts": [
        {
            "created": "2011-03-15T13:30:53.215+0000", 
            "id": "8a90f8b42eb9b5b8012eb9b6439f021a", 
            "productId": "37080", 
            "productName": "Awesome OS Modifier Bits", 
            "updated": "2011-03-15T13:30:53.215+0000"
        }
    ], 
    "quantity": 5, 
    "restrictedToUsername": null, 
    "sourceEntitlement": null, 
    "startDate": "2011-03-15T00:00:00.000+0000", 
    "subscriptionId": "8a90f8b42eb9b5b8012eb9b6059a0050", 
    "updated": "2011-03-15T13:30:53.215+0000"
}

Example jsonProduct:
[root@jsefler-onprem04 tmp]# curl -u testuser1:password -k https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/products/37080 | python -mjson.tool  
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
102  1130    0  1130    0     0   9955      0 --:--:-- --:--:-- --:--:-- 62777
{
    "attributes": [
        {
            "created": "2011-03-15T13:30:36.413+0000", 
            "name": "variant", 
            "updated": "2011-03-15T13:30:36.413+0000", 
            "value": "ALL"
        }, 
        {
            "created": "2011-03-15T13:30:36.414+0000", 
            "name": "arch", 
            "updated": "2011-03-15T13:30:36.414+0000", 
            "value": "ALL"
        }, 
        {
            "created": "2011-03-15T13:30:36.414+0000", 
            "name": "type", 
            "updated": "2011-03-15T13:30:36.414+0000", 
            "value": "SVC"
        }, 
        {
            "created": "2011-03-15T13:30:36.414+0000", 
            "name": "version", 
            "updated": "2011-03-15T13:30:36.414+0000", 
            "value": "6.1"
        }
    ], 
    "created": "2011-03-15T13:30:36.413+0000", 
    "dependentProductIds": [], 
    "href": "/products/37080", 
    "id": "37080", 
    "multiplier": 1, 
    "name": "Awesome OS Modifier Bits", 
    "productContent": [
        {
            "content": {
                "contentUrl": "http://example.com/awesomeos-modifier", 
                "created": "2011-03-15T13:30:26.233+0000", 
                "gpgUrl": "http://example.com/awesomeos-modifier/gpg", 
                "id": "1112", 
                "label": "awesomeos-modifier", 
                "metadataExpire": null, 
                "modifiedProductIds": [
                    "27060", 
                    "37060"
                ], 
                "name": "awesomeos-modifier", 
                "requiredTags": null, 
                "type": "yum", 
                "updated": "2011-03-15T13:30:26.233+0000", 
                "vendor": "test-vendor"
            }, 
            "enabled": true, 
            "flexEntitlement": 0, 
            "physicalEntitlement": 0
        }
    ], 
    "updated": "2011-03-15T13:30:36.413+0000"
}

		  
*/
	
	

	
	

}
