package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.ContentNamespace;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.InstalledProduct;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author ssalevan
 * @author jsefler
 *
 */
@Test(groups={"subscribe"})
public class SubscribeTests extends SubscriptionManagerCLITestScript{
	
	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an expected subscription pool product id",
			dataProvider="getSubscriptionPoolProductIdData",
			groups={"blockedByBug-660713"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void SubscribeToExpectedSubscriptionPoolProductId_Test(String productId, String[] bundledProductNames) {
		List<ProductCert> currentlyInstalledProductCerts = clienttasks.getCurrentProductCerts();

		// begin test with a fresh register
		clienttasks.unregister(null, null, null);
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);

		// assert the subscription pool with the matching productId is available
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", productId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(pool, "Expected SubscriptionPool with ProductId '"+productId+"' is available for subscribing.");

		// assert the status of the installed products
		for (String productName : bundledProductNames) {
			// assert the status of the installed products
			ProductCert productCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productName", productName, currentlyInstalledProductCerts);
			if (productCert!=null) {
				InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", productName, clienttasks.getCurrentlyInstalledProducts());
				Assert.assertNotNull(installedProduct, "The status of product with ProductName '"+productName+"' is reported in the list of installed products.");
				Assert.assertEquals(installedProduct.status, "Not Subscribed", "Before subscribing to ProductId '"+productId+"', the status of Installed Product '"+productName+"' is Not Subscribed.");
			}
		}
		
		// subscribe to the pool
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		
		// assert the expected products are consumed
		for (String productName : bundledProductNames) {
			ProductSubscription productSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName", productName, clienttasks.getCurrentlyConsumedProductSubscriptions());
			Assert.assertNotNull(productSubscription, "Expected ProductSubscription with ProductName '"+productName+"' is consumed after subscribing to pool with ProductId '"+productId+"'.");

			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=660713 - jsefler 12/12/2010
			Boolean invokeWorkaroundWhileBugIsOpen = true;
			try {String bugId="660713"; if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla bug "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("The workaround while this bug is open is to skip the assertion that: Consumed ProductSubscription Expires on the same DAY as the originating subscription pool.");
			} else {
			// END OF WORKAROUND
				
				// OLD CODE THAT I THINK WAS WRONG  jsefler 12/2/2010
//				// assert the dates match
//				Calendar dayBeforeEndDate = (Calendar) entitlementCert.validityNotAfter.clone(); dayBeforeEndDate.add(Calendar.DATE, -1);
////				Calendar dayBeforeStartDate = (Calendar) entitlementCert.validityNotBefore.clone(); dayBeforeStartDate.add(Calendar.DATE, -1);
//				//Assert.assertEquals(productSubscription.endDate, entitlementCert.validityNotAfter, "Consumed ProductSubscription Expires on the same end date as the given entitlement: "+entitlementCert);
//				Assert.assertTrue(productSubscription.endDate.before(entitlementCert.validityNotAfter) && productSubscription.endDate.after(dayBeforeEndDate), "Consumed ProductSubscription Expires on the same end date as the new entitlement: "+entitlementCert);
//				Assert.assertTrue(productSubscription.startDate.before(entitlementCert.validityNotBefore), "Consumed ProductSubscription Began before the validityNotBefore date of the new entitlement: "+entitlementCert);
//				Assert.assertEquals(ProductSubscription.formatDateString(productSubscription.endDate), SubscriptionPool.formatDateString(pool.endDate), "Consumed ProductSubscription Expires on the same date as the originating subscription pool: "+pool);

				// assert the dates match
				//FIXME https://bugzilla.redhat.com/show_bug.cgi?id=660713 UNCOMMENT WHEN YOU GET AN EXPLANATION FROM DEVELOPMENT
//				Assert.assertEquals(ProductSubscription.formatDateString(productSubscription.startDate),ProductSubscription.formatDateString(entitlementCert.startDate),
//						"Consumed ProductSubscription Begins on the same DAY as the new entitlement.");
//				Assert.assertEquals(ProductSubscription.formatDateString(productSubscription.endDate),ProductSubscription.formatDateString(entitlementCert.endDate),
//						"Consumed ProductSubscription Expires on the same DAY as the new entitlement.");
				Assert.assertEquals(ProductSubscription.formatDateString(productSubscription.endDate),ProductSubscription.formatDateString(pool.endDate),
						"Consumed ProductSubscription Expires on the same DAY as the originating subscription pool.");
				//FIXME		Assert.assertTrue(productSubscription.startDate.before(entitlementCert.validityNotBefore), "Consumed ProductSubscription Began before the validityNotBefore date of the new entitlement: "+entitlementCert);
			}
			
			// assert whether or not the product is installed			
			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", productName, clienttasks.getCurrentlyInstalledProducts());
			Assert.assertNotNull(installedProduct, "The status of product with ProductName '"+productName+"' is reported in the list of installed products.");

			// assert the status of the installed products
			ProductCert productCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productName", productName, currentlyInstalledProductCerts);
			if (productCert!=null) {
				Assert.assertEquals(installedProduct.status, "Subscribed", "After subscribing to ProductId '"+productId+"', the status of Installed Product '"+productName+"' is Subscribed since a corresponding product cert was found in "+clienttasks.productCertDir);
				Assert.assertEquals(InstalledProduct.formatDateString(installedProduct.expires), ProductSubscription.formatDateString(productSubscription.endDate), "Installed Product '"+productName+"' expires on the same date as the consumed ProductSubscription: "+productSubscription);
				Assert.assertEquals(installedProduct.subscription, productSubscription.serialNumber, "Installed Product '"+productName+"' subscription matches the serialNumber of the consumed ProductSubscription: "+productSubscription);
			} else {
				Assert.assertEquals(installedProduct.status, "Not Installed", "The status of Entitled Product '"+productName+"' is Not Installed since a corresponding product cert was not found in "+clienttasks.productCertDir);
			}
		}
	}
	

	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using product ID",
			enabled=false,	// Subscribing to a Subscription Pool using --product Id has been removed in subscription-manager-0.71-1.el6.i686.
			groups={"blockedByBug-584137"},
			dataProvider="getAvailableSubscriptionPoolsData")
	@ImplementsNitrateTest(caseId=41680)
	public void SubscribeToValidSubscriptionsByProductID_Test(SubscriptionPool pool){
//		sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using product ID",
			groups={"blockedByBug-584137"},
			enabled=false)
	public void SubscribeToASingleEntitlementByProductID_Test(){
		clienttasks.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		SubscriptionPool MCT0696 = new SubscriptionPool("MCT0696", "696");
		MCT0696.addProductID("Red Hat Directory Server");
		clienttasks.subscribeToSubscriptionPoolUsingProductId(MCT0696);
		//this.refreshSubscriptions();
		for (ProductSubscription pid:MCT0696.associatedProductIDs){
			Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().contains(pid),
					"ProductID '"+pid.productName+"' consumed from Pool '"+MCT0696.subscriptionName+"'");
		}
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using pool ID",
			groups={"blockedByBug-584137"},
			enabled=true,
			dataProvider="getAvailableSubscriptionPoolsData")
	@ImplementsNitrateTest(caseId=41686)
	public void SubscribeToValidSubscriptionsByPoolID_Test(SubscriptionPool pool){
// non-dataProvided test procedure
//		sm.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
		clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to each available subscription pool using pool ID",
			groups={"blockedByBug-584137"},
			dataProvider="getGoodRegistrationData")
	@ImplementsNitrateTest(caseId=41686)
	public void SubscribeConsumerToEachAvailableSubscriptionPoolUsingPoolId_Test(String username, String password){
		clienttasks.unregister(null, null, null);
		clienttasks.register(username, password, ConsumerType.system, null, null, Boolean.FALSE, Boolean.FALSE, null, null, null);
		clienttasks.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
	}
	
	
	// TODO DELETE TEST due to https://bugzilla.redhat.com/show_bug.cgi?id=670823
	@Test(	description="subscription-manager-cli: subscribe consumer to an entitlement using registration token",
			groups={"blockedByBug-584137"},
			enabled=false)
	@ImplementsNitrateTest(caseId=41681)
	public void SubscribeToRegToken_Test(){
		clienttasks.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.subscribeToRegToken(regtoken);
	}
	
	
	@Test(	description="Subscribed for Already subscribed Entitlement.",
			groups={"blockedByBug-584137"},
			dataProvider="getAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41897)
	public void AttemptToSubscribeToAnAlreadySubscribedPool_Test(SubscriptionPool pool){
// non-dataProvided test procedure
//		//sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
//		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
//		for(SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
//			clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
//			clienttasks.subscribeToProduct(pool.subscriptionName);
//		}
		clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
		SSHCommandResult result = clienttasks.subscribe_(pool.poolId,null,null,null,null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "This consumer is already subscribed to the product matching pool with id '"+pool.poolId+"'",
				"subscribe command returns proper message when already subscribed to the requested pool");
	}
	
	
	@Test(	description="subscription-manager-cli: subscribe consumer to multiple/duplicate/bad pools in one call",
			groups={"blockedByBug-622851"},
			enabled=true)
	public void SubscribeToMultipleDuplicateAndBadPools_Test() {
		
		// begin the test with a cleanly registered system
		clienttasks.unregister(null, null, null);
	    clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
	    
		// assemble a list of all the available SubscriptionPool ids with duplicates and bad ids
		List <String> poolIds = new ArrayList<String>();
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			poolIds.add(pool.poolId);
			poolIds.add(pool.poolId); // add a duplicate poolid
		}
		String badPoolId1 = "bad123", badPoolId2 = "bad_POOLID"; 
		poolIds.add(0, badPoolId1); // insert a bad poolid
		poolIds.add(badPoolId2); // append a bad poolid
		
		// subscribe to all pool ids
		log.info("Attempting to subscribe to multiple pools with duplicate and bad pool ids...");
		SSHCommandResult result = clienttasks.subscribe(poolIds, null, null, null, null, null, null, null);
		
		// assert the results
		for (String poolId : poolIds) {
			if (poolId.equals(badPoolId1)) continue; if (poolId.equals(badPoolId2)) continue;
			Assert.assertContainsMatch(result.getStdout(),"^This consumer is already subscribed to the product matching pool with id '"+poolId+"'$","Asserting that already subscribed to pools is noted and skipped during a multiple pool binding.");
		}
		Assert.assertContainsMatch(result.getStdout(),"^No such entitlement pool: "+badPoolId1+"$","Asserting that an invalid pool is noted and skipped during a multiple pool binding.");
		Assert.assertContainsMatch(result.getStdout(),"^No such entitlement pool: "+badPoolId2+"$","Asserting that an invalid pool is noted and skipped during a multiple pool binding.");
		clienttasks.assertNoAvailableSubscriptionPoolsToList("Asserting that no available subscription pools remain after simultaneously subscribing to them all including duplicates and bad pool ids.");
	}
	
	
	@Test(	description="rhsmcertd: change certFrequency",
			dataProvider="getCertFrequencyData",
			groups={"blockedByBug-617703"},
			enabled=true)
	@ImplementsNitrateTest(caseId=41692)
	public void rhsmcertdChangeCertFrequency_Test(int minutes) {
		String errorMsg = "Either the consumer is not registered with candlepin or the certificates are corrupted. Certificate updation using daemon failed.";
		errorMsg = "Either the consumer is not registered or the certificates are corrupted. Certificate update using daemon failed.";
		
		log.info("First test with an unregistered user and verify that the rhsmcertd actually fails since it cannot self-identify itself to the candlepin server.");
		clienttasks.unregister(null, null, null);
		clienttasks.restart_rhsmcertd(minutes, false); sleep(10000); // allow 10sec for the initial update
		log.info("Appending a marker in the '"+clienttasks.rhsmcertdLogFile+"' so we can assert that the certificates are being updated every '"+minutes+"' minutes");
		String marker = "Testing rhsm.conf certFrequency="+minutes+" when unregistered..."; // https://tcms.engineering.redhat.com/case/41692/
		RemoteFileTasks.runCommandAndAssert(client,"echo \""+marker+"\" >> "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),marker,null);
		sleep(minutes*60*1000);	// give the rhsmcertd a chance to check in with the candlepin server and update the certs
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),"update failed \\(\\d+\\), retry in "+minutes+" minutes",null);
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmLogFile,Integer.valueOf(0),errorMsg,null);
		
		
		log.info("Now test with a registered user whose identity is corrupt and verify that the rhsmcertd actually fails since it cannot self-identify itself to the candlepin server.");
		String consumerid = clienttasks.getCurrentConsumerId(clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null));
		log.info("Corrupting the identity cert by borking its content...");
		RemoteFileTasks.runCommandAndAssert(client, "openssl x509 -noout -text -in "+clienttasks.consumerCertFile+" > /tmp/stdout; mv /tmp/stdout -f "+clienttasks.consumerCertFile, 0);
		clienttasks.restart_rhsmcertd(minutes, false); sleep(10000); // allow 10sec for the initial update
		log.info("Appending a marker in the '"+clienttasks.rhsmcertdLogFile+"' so we can assert that the certificates are being updated every '"+minutes+"' minutes");
		marker = "Testing rhsm.conf certFrequency="+minutes+" when identity is corrupted...";
		RemoteFileTasks.runCommandAndAssert(client,"echo \""+marker+"\" >> "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),marker,null);
		sleep(minutes*60*1000);	// give the rhsmcertd a chance to check in with the candlepin server and update the certs
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),"update failed \\(\\d+\\), retry in "+minutes+" minutes",null);
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmLogFile,Integer.valueOf(0),errorMsg,null);

		
		log.info("Finally test with a registered user and verify that the rhsmcertd succeeds because he can identify himself to the candlepin server.");
	    clienttasks.register(clientusername, clientpassword, null, null, consumerid, null, Boolean.TRUE, null, null, null);
		clienttasks.restart_rhsmcertd(minutes, false); sleep(10000); // allow 10sec for the initial update
		log.info("Appending a marker in the '"+clienttasks.rhsmcertdLogFile+"' so we can assert that the certificates are being updated every '"+minutes+"' minutes");
		marker = "Testing rhsm.conf certFrequency="+minutes+" when registered..."; // https://tcms.engineering.redhat.com/case/41692/
		RemoteFileTasks.runCommandAndAssert(client,"echo \""+marker+"\" >> "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),marker,null);
		sleep(minutes*60*1000);	// give the rhsmcertd a chance to check in with the candlepin server and update the certs
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0),"certificates updated",null);

		/* tail -f /var/log/rhsm/rhsm.log
		 * 2010-09-10 12:05:06,338 [ERROR] main() @certmgr.py:75 - Either the consumer is not registered with candlepin or the certificates are corrupted. Certificate updation using daemon failed.
		 */
		
		/* tail -f /var/log/rhsm/rhsmcertd.log
		 * Fri Sep 10 11:59:50 2010: started: interval = 1 minutes
		 * Fri Sep 10 11:59:51 2010: update failed (255), retry in 1 minutes
		 * testing rhsm.conf certFrequency=1 when unregistered.
		 * Fri Sep 10 12:00:51 2010: update failed (255), retry in 1 minutes
		 * Fri Sep 10 12:01:04 2010: started: interval = 1 minutes
		 * Fri Sep 10 12:01:05 2010: certificates updated
		 * testing rhsm.conf certFrequency=1 when registered.
		 * Fri Sep 10 12:02:05 2010: certificates updated
		*/
	}
	
	
	@Test(	description="rhsmcertd: ensure certificates synchronize",
			groups={"blockedByBug-617703"},
			enabled=true)
	@ImplementsNitrateTest(caseId=41694)
	public void rhsmcertdEnsureCertificatesSynchronize_Test(){
		
		// start with a cleanly unregistered system
		clienttasks.unregister(null, null, null);
		
		// register a clean user
	    clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
	    
	    // subscribe to all the available pools
	    clienttasks.subscribeToAllOfTheCurrentlyAvailableSubscriptionPools(ConsumerType.system);
	    
	    // get all of the current entitlement product certs and remember them
	    List<File> entitlementCertFiles = clienttasks.getCurrentEntitlementCertFiles();
	    
	    // delete all of the entitlement cert files
	    client.runCommandAndWait("rm -rf "+clienttasks.entitlementCertDir+"/*");
	    Assert.assertEquals(clienttasks.getCurrentEntitlementCertFiles().size(), 0,
	    		"All the entitlement product certs have been deleted.");
		
	    // restart the rhsmcertd to run every 1 minute and wait for a refresh
		clienttasks.restart_rhsmcertd(1, true);
		
		// assert that rhsmcertd has refreshed the entitled product certs back to the original
	    Assert.assertEquals(clienttasks.getCurrentEntitlementCertFiles(), entitlementCertFiles,
	    		"All the deleted entitlement product certs have been re-synchronized by rhsm cert deamon.");
	}
	
	
	//FIXME THIS TEST WAS REPLACED BY THE TESTS BELOW AND TESTED AGAINST BETA SUBSCRIPTIONS
//	@Test(	description="subscription-manager-cli: autosubscribe consumer and verify expected subscription pool product id are consumed",
//			groups={},
//			enabled=true)
//	//@ImplementsNitrateTest(caseId=)
//	public void AutoSubscribeToExpectedSubscriptionPoolProductId_Test() throws JSONException {
//		// get the expected subscriptionPoolProductIdData
//		List<List<Object>> subscriptionPoolProductIdData = getSubscriptionPoolProductIdDataAsListOfLists();
//
//		// before testing, make sure all the expected subscriptionPoolProductId are available
//		clienttasks.unregister(null, null, null);
//		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
//		List<SubscriptionPool> availableSubscriptionPoolsBeforeAutosubscribe = clienttasks.getCurrentlyAvailableSubscriptionPools();
//		for (List<Object> row : subscriptionPoolProductIdData) {
//			String subscriptionPoolProductId = (String)row.get(0);
//			SubscriptionPool subscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", subscriptionPoolProductId, availableSubscriptionPoolsBeforeAutosubscribe);
//			Assert.assertNotNull(subscriptionPool, "Expecting SubscriptionPool with ProductId '"+subscriptionPoolProductId+"' to be available to '"+clientusername+"' before testing register with autosubscribe.");
//		}
//		
//		// get a list of the product certs on the client 
//		List<ProductCert> productCerts = clienttasks.getCurrentProductCerts();
//		
//		// register with autosubscribe
//		clienttasks.unregister(null, null, null);
//		SSHCommandResult sshCommandResult = clienttasks.register(clientusername, clientpassword, null, null, null, Boolean.TRUE, null, null, null, null);
//
//		/* Example sshCommandResult.getStdout(): 
//			e1aef738-5d03-4a8a-9c87-7a2652e110a8 rh-alpha-qa-105
//			Bind Product  Red Hat Enterprise Linux High Availability (for RHEL 6 Entitlement) 407
//			Bind Product  Red Hat Enterprise Linux Scalable File System (for RHEL 6 Entitlement) 410
//			Bind Product  Red Hat Enterprise Linux Resilient Storage (for RHEL 6 Entitlement) 409
//			Bind Product  Red Hat Enterprise Linux Load Balancer (for RHEL 6 Entitlement) 408
//			Bind Product  Red Hat Enterprise Linux 6 Entitlement Alpha 406
//		*/
//		
//		/* Sample sshCommandResult.getStdout():
//			d67df9c8-f381-4449-9d17-56094ea58092 testuser1
//			Subscribed to Products:
//	    		RHEL for Physical Servers SVC(37060)
//		 */
//
//		// get the state of affairs after having registered with autosubscribe
//		List<SubscriptionPool> availableSubscriptionPoolsAfterAutosubscribe = clienttasks.getCurrentlyAvailableSubscriptionPools();
//		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
//		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
//		
//		// loop through the subscriptionPoolProductIdData and verify...
//		for (List<Object> row : subscriptionPoolProductIdData) {
//			String subscriptionPoolProductId = (String)row.get(0);
//			String[] bundledProductNames = (String[])row.get(1);
//			SubscriptionPool subscriptionPool;
//			
//			// assert that the subscriptionPoolProductIds that are not installed, do not get autosubscribed to
//			subscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", subscriptionPoolProductId, availableSubscriptionPoolsBeforeAutosubscribe);
//			if (ProductCert.findFirstInstanceWithMatchingFieldFromList("productName", subscriptionPool.subscriptionName, productCerts)==null) {
//				log.warning("Note: No product cert with a name matching '"+subscriptionPool.subscriptionName+"' was found in '"+clienttasks.productCertDir+"'.  Therefore this expected product id cannot possibly be autosubscribed to.  Asserting this fact...");
//				Assert.assertNotNull(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", subscriptionPoolProductId, availableSubscriptionPoolsAfterAutosubscribe),
//						"SubscriptionPool with ProductId '"+subscriptionPoolProductId+"' is STILL available after registering with autosubscribe because no corressponding product cert is installed and therefore cannot possibly be autosubscribed to.");
//				continue;
//			}
//			
//			// assert that the subscriptionPoolProductId has been subscribed to...
//			
//			// assert that subscriptionPoolProductId is not available
//			subscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", subscriptionPoolProductId, availableSubscriptionPoolsAfterAutosubscribe);
//			if (subscriptionPool!=null) {
//				String bundledProductNamesAsString = "";
//				for (String bundledProductName : bundledProductNames) bundledProductNamesAsString += bundledProductName+", ";bundledProductNamesAsString = bundledProductNamesAsString.replaceFirst("(?s), (?!.*?, )",""); // this will replaceLast ", " with ""
//				log.warning("SubscriptionPool with ProductId '"+subscriptionPoolProductId+"' is still available after registering with autosubscribe.");
//				log.warning("The probable cause is that the products we expected to be installed, '"+bundledProductNamesAsString+"', are probably not installed and therefore autosubscribing to '"+subscriptionPoolProductId+"' was not performed.");
//			}
//			Assert.assertNull(subscriptionPool, "SubscriptionPool with ProductId '"+subscriptionPoolProductId+"' is NOT available after registering with autosubscribe.");
//
//			for (String bundledProductName : bundledProductNames) {
//				// assert that the sshCommandResult from register indicates the bundledProductName was subscribed
////DELETEME ALPHA ASSERT				Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "^Bind Product  "+bundledProductName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)"), "Expected ProductName '"+bundledProductName+"' was reported as autosubscribed/bound in the output from register with autotosubscribe.");
//				//Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "^Subscribed to Products:", "register with autotosubscribe appears to have subscribed to something");
//				Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "^\\s+"+bundledProductName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)"), "Expected ProductName '"+bundledProductName+"' was reported as autosubscribed in the output from register with autotosubscribe.");
//
//				// assert that the bundledProductName is consumed
//				ProductSubscription productSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName", bundledProductName, consumedProductSubscriptions);
//				Assert.assertNotNull(productSubscription, "Expected ProductSubscription with ProductName '"+bundledProductName+"' is consumed after registering with autosubscribe.");
//	
//				// assert that the bundledProductName is installed and subscribed
//				InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", bundledProductName, installedProducts);
//				Assert.assertNotNull(installedProduct, "The status of expected product with ProductName '"+bundledProductName+"' is reported in the list of installed products.");
//				Assert.assertEquals(installedProduct.status, "Subscribed", "After registering with autosubscribe, the status of Installed Product '"+bundledProductName+"' is Subscribed.");
//			}
//		}
//		
//		// finally assert that no extraneous subscription pools were subscribed to
//		for (SubscriptionPool subscriptionPoolBeforeAutosubscribe : availableSubscriptionPoolsBeforeAutosubscribe) {
//			if (!availableSubscriptionPoolsAfterAutosubscribe.contains(subscriptionPoolBeforeAutosubscribe)) {
//				boolean subscribedPoolWasExpected = false;
//				for (List<Object> row : subscriptionPoolProductIdData) {
//					String subscriptionPoolProductId = (String)row.get(0);
//					if (subscriptionPoolBeforeAutosubscribe.productId.equals(subscriptionPoolProductId)) {
//						subscribedPoolWasExpected = true;
//						break;
//					}
//				}
//				if (!subscribedPoolWasExpected) Assert.fail("Did NOT expect pool '"+subscriptionPoolBeforeAutosubscribe+"' to be removed from the available list following a register with autosubscribe.");
//			}
//		}
//		Assert.assertTrue(true,"No pools were unexpectedly subscribed to following a register with autosubscribe.");
//	}
	@Test(	description="subscription-manager-cli: autosubscribe consumer and verify expected subscription pool product id are consumed",
			groups={"AutoSubscribeAndVerify"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void InititiateAutoSubscribe_Test() throws JSONException {
		// get the expected subscriptionPoolProductIdData
		List<List<Object>> subscriptionPoolProductIdData = getSubscriptionPoolProductIdDataAsListOfLists();

		// before testing, make sure all the expected subscriptionPoolProductId are available
		clienttasks.unregister(null, null, null);
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
		availableSubscriptionPoolsBeforeAutosubscribe = clienttasks.getCurrentlyAvailableSubscriptionPools();
		for (List<Object> row : subscriptionPoolProductIdData) {
			String subscriptionPoolProductId = (String)row.get(0);
			SubscriptionPool subscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", subscriptionPoolProductId, availableSubscriptionPoolsBeforeAutosubscribe);
			Assert.assertNotNull(subscriptionPool, "Expecting SubscriptionPool with ProductId '"+subscriptionPoolProductId+"' to be available to '"+clientusername+"' before testing register with autosubscribe.");
		}
		
		// register with autosubscribe
		clienttasks.unregister(null, null, null);
		sshCommandResultFromAutosubscribe = clienttasks.register(clientusername, clientpassword, null, null, null, Boolean.TRUE, Boolean.TRUE, null, null, null);
	}
	@Test(	description="subscription-manager-cli: autosubscribe consumer and verify expected subscription pool product id are consumed",
			groups={"AutoSubscribeAndVerify"},
			dependsOnMethods={"InititiateAutoSubscribe_Test"},
			dataProvider="getInstalledProductCertsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyInstalledProductCertWasAutoSubscribed_Test(ProductCert productCert) throws JSONException {
		// get the expected subscriptionPoolProductIdData
		List<List<Object>> subscriptionPoolProductIdData = getSubscriptionPoolProductIdDataAsListOfLists();

		// search the subscriptionPoolProductIdData for a bundledProduct matching the productCert's productName
		String subscriptionPoolProductId = null;
		for (List<Object> row : subscriptionPoolProductIdData) {
			String[] bundledProductNames = (String[])row.get(1);
			for (String bundledProductName : bundledProductNames) {
				if (bundledProductName.equals(productCert.productName)) {
					subscriptionPoolProductId = (String)row.get(0); // found
					break;
				}
			}
			if (subscriptionPoolProductId!=null) break;
		}
		
		// determine what autosubscribe results to assert for this installed productCert 
		InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", productCert.productName, clienttasks.getCurrentlyInstalledProducts());
		if (subscriptionPoolProductId!=null) {
			// yes - this productCert should have been autosubscribed
			
			// assert the installed product status is Subscribed
			Assert.assertEquals(installedProduct.status,"Subscribed",
					"As expected, the Installed Product Status reflects that the autosubscribed ProductName '"+productCert.productName+"' is now subscribed.");

			// assert the sshCommandResultOfAutosubscribe shows the productCert was autosubscribed
			Assert.assertContainsMatch(sshCommandResultFromAutosubscribe.getStdout().trim(), "^\\s+"+productCert.productName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)"),
					"As expected, ProductName '"+productCert.productName+"' was reported as autosubscribed in the output from register with autotosubscribe.");
		} else {
			// no - this productCert should not have been autosubscribed
			
			// assert the installed product status is Not Subscribed
			Assert.assertEquals(installedProduct.status,"Not Subscribed",
					"As expected, the Installed Product Status reflects that the autosubscribed ProductName '"+productCert.productName+"' is NOT subscribed.");

			// assert the sshCommandResultOfAutosubscribe does NOT show the productCert was autosubscribed
			Assert.assertContainsNoMatch(sshCommandResultFromAutosubscribe.getStdout().trim(), "^\\s+"+productCert.productName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)"),
					"As expected, ProductName '"+productCert.productName+"' was NOT reported as autosubscribed in the output from register with autotosubscribe.");
		}
	}
	List<SubscriptionPool> availableSubscriptionPoolsBeforeAutosubscribe;
	SSHCommandResult sshCommandResultFromAutosubscribe;

	
	
	
	// TODO Candidates for an automated Test:
	//		https://bugzilla.redhat.com/show_bug.cgi?id=668032
	//		https://bugzilla.redhat.com/show_bug.cgi?id=672438
	
	
	// Configuration Methods ***********************************************************************
	
	
	
	// Protected Methods ***********************************************************************
	

	
	// Data Providers ***********************************************************************

	@DataProvider(name="getInstalledProductCertsData")
	public Object[][] getInstalledProductCertsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getInstalledProductCertsDataAsListOfLists());
	}
	protected List<List<Object>> getInstalledProductCertsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		for (ProductCert productCert: clienttasks.getCurrentProductCerts()) {
			ll.add(Arrays.asList(new Object[]{productCert}));
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
		ll.add(Arrays.asList(new Object[]{1}));
		
		return ll;
	}
}
