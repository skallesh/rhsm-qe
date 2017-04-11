package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.github.redhatqe.polarize.metadata.TestDefinition;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ConsumerCert;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.ProductNamespace;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

/**
 * @author jsefler
 *
 * Reference Design Doc:
 *   Design Doc: https://mojo.redhat.com/docs/DOC-1042464
 *   Implementation doc: https://mojo.redhat.com/docs/DOC-1053482
 *   Manual testcases: http://etherpad.corp.redhat.com/CDK-implementation 
 * 
 * DevSku functionality was implemented in candlepin-2.0.8-1
 * DevSku functionality was back ported to candlepin-0.9.51.13
 * 
 * Candlepin deployment:
 *   For manual testing:
 *     see http://www.candlepinproject.org/docs/candlepin/mode_agnostic_spec_testing.html
 *     set /etc/candlepin/candlepin.conf candlepin.standalone=false
 *     set /etc/candlepin/candlepin.conf module.config.hosted.configuration.module=org.candlepin.hostedtest.AdapterOverrideModule
 *     deploy -g -t -a -h   which is synonymous with   export GENDB=1 && export TESTDATA=1 && export AUTOCONF=1 && export HOSTEDTEST="hostedtest" && bundle exec bin/deploy
 *     
 *   For manual testing:
 *     I will dynamically set candlepin.standalone=false and restart tomcat
 *     BEWARE: any call to refresh pools will remove all subscriptions and pools because there is no subscription adapter configured for a standalone=true deployment unless you redeploy the server with -h
 *     
 * Testing vagrant images:
 *   https://access.redhat.com/documentation/en/red-hat-enterprise-linux-atomic-host/version-7/container-development-kit-guide/#installing_cdk_on_fedora
 *   http://etherpad.corp.redhat.com/8D1JQhUxc0
 *       
 */
@Test(groups={"DevSKUTests","Tier3Tests","AcceptanceTests","Tier1Tests"})
public class DevSKUTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************
	
	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-25335", "RHEL7-52092"})
	@Test(	description="given an available SKU, configure the system with custom facts dev_sku=SKU, register the system with auto-attach and verify several requirements of the attached entitlement",
			groups={},
			dataProvider="getDevSkuData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyDevSku_Test(Object bugzilla, String devSku, String devPlatform) throws JSONException, Exception {
		
		// get the JSON product representation of the devSku 
		String resourcePath = "/products/"+devSku;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		JSONObject jsonDevSkuProduct = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, resourcePath));
		if (jsonDevSkuProduct.has("displayMessage")) {
			// indicative that: // Product with ID 'dev-mkt-product' could not be found.
		}
		
		// instrument the system facts to behave as a vagrant image
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("dev_sku",devSku);
		factsMap.put("dev_platform",devPlatform);
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		
		// mark the rhsm.log file
		String logMarker = System.currentTimeMillis()+" Testing VerifyDevSku_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);
		
		// register with auto subscribe and force (to unregister anyone that is already registered)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		
		// get the tail of the marked rhsm.log file
		String logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, null).trim();

		// assert when /etc/candlepin/candlepin.conf candlepin.standalone = true   (FYI: candlepin.standalone=false is synonymous with a hosted candlepin deployment)
		//	2016-01-05 17:02:34,527 [DEBUG] subscription-manager:20144 @connection.py:530 - Making request: POST /candlepin/consumers/21800967-1d20-43a9-9bf3-07c5c7d41f61/entitlements
		//	2016-01-05 17:02:34,802 [DEBUG] subscription-manager:20144 @connection.py:562 - Response: status=403, requestUuid=b88c0d1c-0816-4097-89d5-114020d86af1
		//	2016-01-05 17:02:34,804 [WARNING] subscription-manager:20144 @managercli.py:201 - Error during auto-attach.
		//	2016-01-05 17:02:34,805 [ERROR] subscription-manager:20144 @managercli.py:202 - Development units may only be used on hosted servers and with orgs that have active subscriptions.
		//	Traceback (most recent call last):
		//	  File "/usr/share/rhsm/subscription_manager/managercli.py", line 197, in autosubscribe
		//	    ents = cp.bind(consumer_uuid)  # new style
		//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 1148, in bind
		//	    return self.conn.request_post(method)
		//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 657, in request_post
		//	    return self._request("POST", method, params)
		//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 571, in _request
		//	    self.validateResponse(result, request_type, handler)
		//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 621, in validateResponse
		//	    raise RestlibException(response['status'], error_msg, response.get('headers'))
		//	RestlibException: Development units may only be used on hosted servers and with orgs that have active subscriptions.
		String expectedError = "RestlibException: Development units may only be used on hosted servers and with orgs that have active subscriptions.";
		if (servertasks.statusStandalone) {
			Assert.assertTrue(logTail.contains(expectedError), "When attempting to autosubscribe a consumer with a dev_sku fact against a candlepin.standalone=true server, an rhsm.log error is thrown stating '"+expectedError+"'.");
			throw new SkipException("Detected that candlepin status standalone=true.  DevSku support is only applicable when /etc/candlepin/candlepin candlepin.standalone=false  (typical of a hosted candlepin server).");
		} else {
			Assert.assertTrue(!logTail.contains(expectedError), "When attempting to autosubscribe a consumer with a dev_sku fact against a candlepin.standalone=false server, an rhsm.log error is NOT thrown stating '"+expectedError+"'.");	
		}
		
		// assert that unrecognized dev_skus are handled gracefully and no entitlements were attached
		//	2016-01-07 12:20:11,117 [DEBUG] subscription-manager:29097 @connection.py:530 - Making request: POST /candlepin/consumers/9b3a5d90-12c7-43a1-8953-4828aaab2a54/entitlements
		//	2016-01-07 12:20:11,584 [DEBUG] subscription-manager:29097 @connection.py:562 - Response: status=403, requestUuid=9fa1fea4-19de-412f-9582-c277968046a8
		//	2016-01-07 12:20:11,586 [WARNING] subscription-manager:29097 @managercli.py:201 - Error during auto-attach.
		//	2016-01-07 12:20:11,591 [ERROR] subscription-manager:29097 @managercli.py:202 - SKU product not available to this development unit: 'dev-mkt-product'
		//	Traceback (most recent call last):
		//	  File "/usr/share/rhsm/subscription_manager/managercli.py", line 197, in autosubscribe
		//	    ents = cp.bind(consumer_uuid)  # new style
		//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 1148, in bind
		//	    return self.conn.request_post(method)
		//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 657, in request_post
		//	    return self._request("POST", method, params)
		//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 571, in _request
		//	    self.validateResponse(result, request_type, handler)
		//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 621, in validateResponse
		//	    raise RestlibException(response['status'], error_msg, response.get('headers'))
		//	RestlibException: SKU product not available to this development unit: 'dev-mkt-product'
		if (jsonDevSkuProduct.has("displayMessage")) {
			expectedError = "RestlibException: SKU product not available to this development unit: '"+devSku+"'";
			Assert.assertTrue(logTail.contains(expectedError), "When attempting to autosubscribe a consumer with an unknown dev_sku fact against a candlepin.standalone=false server, an rhsm.log error is thrown stating '"+expectedError+"'.");
			throw new SkipException("Detected that dev_sku '"+devSku+"' was unknown.  Verified that a graceful error was logged to rhsm.log.");
		}
		
		// assert only one entitlement was granted
		List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
		Assert.assertEquals(entitlementCerts.size(), 1, "After registering (with autosubscribe) a system with dev_sku fact '"+devSku+"', only one entitlement should be granted.");
		EntitlementCert devSkuEntitlement = entitlementCerts.get(0);
		ProductSubscription devSkuProductSubscription = clienttasks.getCurrentlyConsumedProductSubscriptions().get(0);
		
		// assert that all of the known installed products are provided by the consumed entitlement
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		List<ProductCert> productCerts = clienttasks.getCurrentProductCerts();
		Set installedProductIds = new HashSet<String>();
		for (InstalledProduct installedProduct : installedProducts) {
			// ignore installed products that are unknown to the candlepin product layer
			resourcePath = "/products/"+installedProduct.productId;
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
			JSONObject jsonProduct = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, resourcePath));
			if (jsonProduct.has("displayMessage")) {
				// indicative that: // Product with ID '69' could not be found.
				String expectedDisplayMessage = String.format("Product with UUID '%s' could not be found.",installedProduct.productId);
				if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) expectedDisplayMessage = String.format("Product with ID '%s' could not be found.",installedProduct.productId);
				Assert.assertEquals(jsonProduct.getString("displayMessage"),expectedDisplayMessage);
				log.info("Installed Product ID '"+installedProduct.productId+"' ("+installedProduct.productName+") was not recognized by our candlepin server.  Therefore this product will not be entitled by the devSku.");
			} else {
				installedProductIds.add(installedProduct.productId);
			}
		}
		Set entitledProductIds = new HashSet<String>();
		for (ProductNamespace productNamespace : devSkuEntitlement.productNamespaces) entitledProductIds.add(productNamespace.id);
		Assert.assertTrue(entitledProductIds.containsAll(installedProductIds) && entitledProductIds.size()==installedProductIds.size(), "All (and only) of the currently installed products known by the candlepin product layer are entitled by the devSku entitlement.  (Actual entitled product ids "+entitledProductIds+")");

		// assert that all of the entitled product names are shown in the provides list of the consumed devSku product subscription
		for (ProductNamespace productNamespace : devSkuEntitlement.productNamespaces) {
			Assert.assertTrue(devSkuProductSubscription.provides.contains(productNamespace.name), "The consumed devSku Product Subscriptions provides installed product name '"+productNamespace.name+"'.");
		}
		
		// assert that all of the provided product content sets that match this system's arch and installed product tags are available in yum repos
		List<String> yumRepos = clienttasks.getYumRepolist("all");
		for (ContentNamespace contentNamespace : devSkuEntitlement.contentNamespaces) {
			if (contentNamespace.type.equals("yum") &&
				clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, productCerts) &&
				clienttasks.isArchCoveredByArchesInContentNamespace(clienttasks.arch, contentNamespace)) {
				Assert.assertTrue(yumRepos.contains(contentNamespace.label), "Found entitled yum repo '"+contentNamespace.label+"' ("+contentNamespace.name+") (which matches this system arch and installed product tags) among yum repolist all.");
			} else {
				Assert.assertTrue(!yumRepos.contains(contentNamespace.label), "Did NOT find entitled yum repo '"+contentNamespace.label+"' ("+contentNamespace.name+") (which does not match this system arch and installed product tags) among yum repolist all.");				
			}
		}
		
		// assert that the entitled service_level defaults to "Self-Service" when not explicitly set by the dev_sku product
		String devSkuServiceLevel = CandlepinTasks.getResourceAttributeValue(jsonDevSkuProduct, "support_level");
		if (devSkuServiceLevel==null) {
			String defaultServiceLevel = "Self-Service";
			Assert.assertEquals(devSkuEntitlement.orderNamespace.supportLevel, defaultServiceLevel, "When no support_level attribute exists on the devSku product, the entitlement's order service level defaults to '"+defaultServiceLevel+"'.");
			Assert.assertEquals(devSkuProductSubscription.serviceLevel, defaultServiceLevel, "When no support_level attribute exists on the devSku product, the entitled consumed product subscription service level defaults to '"+defaultServiceLevel+"'.");
		} else {
			Assert.assertEquals(devSkuEntitlement.orderNamespace.supportLevel, devSkuServiceLevel, "When a support_level attribute was set on the devSku product, the entitlement's order service level matches '"+devSkuServiceLevel+"'.");			
			Assert.assertEquals(devSkuProductSubscription.serviceLevel, devSkuServiceLevel, "When a support_level attribute was set on the devSku product, the consumed product subscription service level matches '"+devSkuServiceLevel+"'.");			
		}
		
		// assert that the entitled expires_after defaults to 90 days after the registered consumer data when not explicitly set by the dev_sku product
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		Calendar expectedEndDate = Calendar.getInstance(); expectedEndDate.setTimeInMillis(consumerCert.validityNotBefore.getTimeInMillis());
		String devSkuExpiresAfter = CandlepinTasks.getResourceAttributeValue(jsonDevSkuProduct, "expires_after");
		if (devSkuExpiresAfter==null) {
			String defaultExpiresAfter = "90"; // days
			// TEMPORARY WORKAROUND
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1297863"; // Bug 1297863 - to account for daylight savings events, dev_sku (CDK) entitlements should add Calendar.DATE units of expires_after to establish the subscription end date
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen && clienttasks.redhatReleaseX.equals("6") && clienttasks.isPackageVersion("subscription-manager", ">=", "1.15")) {
				// NOTE: This will be an hour off when the duration crosses the "Fall" back or "Spring" forward daylight saving dates.
				expectedEndDate.add(Calendar.HOUR, Integer.valueOf(defaultExpiresAfter)*24);
			} else
			// END OF WORKAROUND
			expectedEndDate.add(Calendar.DATE, Integer.valueOf(defaultExpiresAfter));
///*debugTesting*/expectedEndDate.add(Calendar.SECOND, 20);	// to force an expected failure
			//Assert.assertEquals(ConsumerCert.formatDateString(devSkuEntitlement.validityNotAfter), ConsumerCert.formatDateString(expectedEndDate), "When no expires_after attribute exists on the devSku product, the entitlement's validityNotAfter date defaults to '"+defaultExpiresAfter+"' days after the date the consumer was registered ("+ConsumerCert.formatDateString(consumerCert.validityNotBefore)+").");
			// java.lang.AssertionError: When no expires_after attribute exists on the devSku product, the entitlement's validityNotAfter date defaults to '90' days after the date the consumer was registered (Jul 6 2016 12:19:18 EDT). expected:<Oct 4 2016 12:19:18 EDT> but was:<Oct 4 2016 12:19:17 EDT>
			// allow for a few seconds of tolerance
			Calendar expectedEndDateUpperTolerance = (Calendar) expectedEndDate.clone(); expectedEndDateUpperTolerance.add(Calendar.SECOND, +5);
			Calendar expectedEndDateLowerTolerance = (Calendar) expectedEndDate.clone(); expectedEndDateLowerTolerance.add(Calendar.SECOND, -5);
			Assert.assertTrue(devSkuEntitlement.validityNotAfter.before(expectedEndDateUpperTolerance) && devSkuEntitlement.validityNotAfter.after(expectedEndDateLowerTolerance), "When no expires_after attribute exists on the devSku product, the entitlement's validityNotAfter date defaults to '"+defaultExpiresAfter+"' days after the date the consumer was registered ("+ConsumerCert.formatDateString(consumerCert.validityNotBefore)+"). devSkuEntitlement.validityNotAfter expected: <"+ConsumerCert.formatDateString(expectedEndDate)+"> (withn a few seconds of tolerance of) actual: <"+ConsumerCert.formatDateString(devSkuEntitlement.validityNotAfter)+">");
		} else {
			// TEMPORARY WORKAROUND
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1297863"; // Bug 1297863 - to account for daylight savings events, dev_sku (CDK) entitlements should add Calendar.DATE units of expires_after to establish the subscription end date
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen && clienttasks.redhatReleaseX.equals("6") && clienttasks.isPackageVersion("subscription-manager", ">=", "1.15")) {
				// NOTE: This will be an hour off when the duration crosses the "Fall" back or "Spring" forward daylight saving dates.
				expectedEndDate.add(Calendar.HOUR, Integer.valueOf(devSkuExpiresAfter)*24);
			} else
			// END OF WORKAROUND
			expectedEndDate.add(Calendar.DATE, Integer.valueOf(devSkuExpiresAfter));
///*debugTesting*/expectedEndDate.add(Calendar.SECOND, 20);	// to force an expected failure
			//Assert.assertEquals(ConsumerCert.formatDateString(devSkuEntitlement.validityNotAfter), ConsumerCert.formatDateString(expectedEndDate), "When an expires_after attribute exists on the devSku product, the entitlement's validityNotAfter is '"+devSkuExpiresAfter+"' days after the date the consumer was registered ("+ConsumerCert.formatDateString(consumerCert.validityNotBefore)+").");
			// java.lang.AssertionError: When an expires_after attribute exists on the devSku product, the entitlement's validityNotAfter is '75' days after the date the consumer was registered (Jul 3 2016 21:43:03 EDT). expected:<Sep 16 2016 21:43:03 EDT> but was:<Sep 16 2016 21:43:02 EDT>	
			// allow for a few seconds of tolerance
			Calendar expectedEndDateUpperTolerance = (Calendar) expectedEndDate.clone(); expectedEndDateUpperTolerance.add(Calendar.SECOND, +5);
			Calendar expectedEndDateLowerTolerance = (Calendar) expectedEndDate.clone(); expectedEndDateLowerTolerance.add(Calendar.SECOND, -5);
			Assert.assertTrue(devSkuEntitlement.validityNotAfter.before(expectedEndDateUpperTolerance) && devSkuEntitlement.validityNotAfter.after(expectedEndDateLowerTolerance), "When an expires_after attribute exists on the devSku product, the entitlement's validityNotAfter is '"+devSkuExpiresAfter+"' days after the date the consumer was registered ("+ConsumerCert.formatDateString(consumerCert.validityNotBefore)+"). devSkuEntitlement.validityNotAfter expected: <"+ConsumerCert.formatDateString(expectedEndDate)+"> (withn a few seconds of tolerance of) actual: <"+ConsumerCert.formatDateString(devSkuEntitlement.validityNotAfter)+">");
		}
		
		// assert that the pool consumed exists with quantity 1
		resourcePath = "/pools/"+devSkuProductSubscription.poolId;
		JSONObject jsonDevSkuPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, resourcePath));
		Assert.assertEquals(jsonDevSkuPool.getInt("quantity"), 1, "The quantity on pool '"+devSkuProductSubscription.poolId+"' generated for devSku product '"+devSku+"'.");
		
		// assert that the pool consumed requires_consumer UUID that is currently registered
		String devSkuRequiresConsumer = CandlepinTasks.getPoolAttributeValue(jsonDevSkuPool, "requires_consumer");	// "4a49b1a7-c616-42dd-b96d-62233a4c82b9"
		Assert.assertEquals(devSkuRequiresConsumer, consumerCert.consumerid, "The requires_consumer attribute on pool '"+devSkuProductSubscription.poolId+"' generated for devSku product '"+devSku+"'.");

		// assert that the pool generated has attribute dev_pool: true
		String devSkuDevPool = CandlepinTasks.getPoolAttributeValue(jsonDevSkuPool, "dev_pool");	// "true" or "false"
		Assert.assertEquals(Boolean.valueOf(devSkuDevPool), Boolean.TRUE, "The dev_pool attribute on pool '"+devSkuProductSubscription.poolId+"' generated for devSku product '"+devSku+"'.");
	}



	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-20116", "RHEL7-51857"})
	@Test(	description="configure the system with custom facts for a dev_sku, register the system with auto-subscribe verify the attached entitlement can be removed and re-autoattached",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyDevSkuEntitlementCanBeRemovedAndReAttached_Test() throws JSONException, Exception {
		
		// get a valid dev_sku to test with
		List<Object> l = getRandomValidDevSkuData();
		String devSku = (String) l.get(1);
		String devPlatform = (String) l.get(2);
		
		// instrument the system facts to behave as a vagrant image
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("dev_sku",devSku);
		factsMap.put("dev_platform",devPlatform);
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		
		// register with autosubscribe and force (to unregister anyone that is already registered)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		
		// get the autosubscribed productSubscription
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptions.size(), 1, "After registering (with autosubscribe) a system with dev_sku fact '"+devSku+"', only one product subscription should be consumed.");
		ProductSubscription devSkuProductSubscription1 = productSubscriptions.get(0);
		
		// remove it
		clienttasks.unsubscribe(null, devSkuProductSubscription1.serialNumber, null, null, null, null, null);
		
		// verify that the pool from which the devSku was entitled is no longer consumable after having removed the devSku entitlement
		SSHCommandResult result = clienttasks.subscribe_(null, null, devSkuProductSubscription1.poolId, null, null, null, null, null, null, null, null, null, null);
		String expectedStdout = String.format("Pool with id %s could not be found.",devSkuProductSubscription1.poolId);
		Assert.assertEquals(result.getStdout().trim(), expectedStdout, "After removing a devSku entitlement, its pool should no longer be consumable.");
		
		// re-autosubscribe
		clienttasks.subscribe(true, null, null, null, (String)null, null, null, null, null, null, null, null, null);
		
		// get the re-autosubscribed entitlement
		productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptions.size(), 1, "After re-autosubscribing a system with dev_sku fact '"+devSku+"', only one product subscription should be consumed.");
		ProductSubscription devSkuProductSubscription2 = productSubscriptions.get(0);
		
		// assert the re-autosubscribes product subscriptions match (except for pool id and serial since the pool must go when removed)
		Assert.assertEquals(ProductSubscription.formatDateString(devSkuProductSubscription2.endDate), ProductSubscription.formatDateString(devSkuProductSubscription1.endDate), "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription with the same end date.");
		Assert.assertEquals(devSkuProductSubscription2.serviceLevel, devSkuProductSubscription1.serviceLevel, "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription with the same service level.");
		Assert.assertEquals(devSkuProductSubscription2.quantityUsed, devSkuProductSubscription1.quantityUsed, "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription with the quantity used.");
		Assert.assertTrue(devSkuProductSubscription2.provides.containsAll(devSkuProductSubscription1.provides)&&devSkuProductSubscription1.provides.containsAll(devSkuProductSubscription2.provides), "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription that provides the same products."+"  devSkuProductSubscription1.provides="+devSkuProductSubscription1.provides+"  devSkuProductSubscription2.provides="+devSkuProductSubscription2.provides);
		Assert.assertTrue(!devSkuProductSubscription2.poolId.equals(devSkuProductSubscription1.poolId), "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription from a different pool id.");
		Assert.assertTrue(!devSkuProductSubscription2.serialNumber.equals(devSkuProductSubscription1.serialNumber), "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription with a different serial.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-22313", "RHEL7-51859"})
	@Test(	description="configure the system with custom facts for a dev_sku, register the system with auto-subscribe and verify that an attempt to redundantly autosubscribed will re-issue a replacement entitlement (remove the old and attach a new).",
			groups={"blockedByBug-1292877"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyRedundantAutosubscribesForDevSkuEntitlementWillReissueNewEntitlement_Test() throws JSONException, Exception {

		// get a valid dev_sku to test with
		List<Object> l = getRandomValidDevSkuData();
		String devSku = (String) l.get(1);
		String devPlatform = (String) l.get(2);
		
		// instrument the system facts to behave as a vagrant image
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("dev_sku",devSku);
		factsMap.put("dev_platform",devPlatform);
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		
		// register with autosubscribe and force (to unregister anyone that is already registered)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		
		// are we fully compliant? complianceStatus=="valid"
		String complianceStatus;
		//complianceStatus = clienttasks.getFactValue("system.entitlements_valid");
		complianceStatus = CandlepinTasks.getConsumerComplianceStatus(sm_clientUsername, sm_clientPassword, sm_serverUrl, clienttasks.getCurrentConsumerId());
		
		// get the autosubscribed productSubscription
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptions.size(), 1, "After registering (with autosubscribe) a system with dev_sku fact '"+devSku+"', only one product subscription should be consumed.");
		ProductSubscription devSkuProductSubscription1 = productSubscriptions.get(0);
		
		// re-autosubscribe
		clienttasks.subscribe(true, null, null, null, (String)null, null, null, null, null, null, null, null, null);
		
		// get the re-autosubscribed entitlement
		productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptions.size(), 1, "After re-autosubscribing a system with dev_sku fact '"+devSku+"', only one product subscription should be consumed.");
		ProductSubscription devSkuProductSubscription2 = productSubscriptions.get(0);
		
		// assert the re-autosubscribes product subscriptions match (except for pool id and serial since the pool must go when a new autosubscribe is required)
		Assert.assertEquals(ProductSubscription.formatDateString(devSkuProductSubscription2.endDate), ProductSubscription.formatDateString(devSkuProductSubscription1.endDate), "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription with the same end date.");
		Assert.assertEquals(devSkuProductSubscription2.serviceLevel, devSkuProductSubscription1.serviceLevel, "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription with the same service level.");
		Assert.assertEquals(devSkuProductSubscription2.quantityUsed, devSkuProductSubscription1.quantityUsed, "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription with the quantity used.");
		Assert.assertTrue(devSkuProductSubscription2.provides.containsAll(devSkuProductSubscription1.provides)&&devSkuProductSubscription1.provides.containsAll(devSkuProductSubscription2.provides), "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription that provides the same products."+"  devSkuProductSubscription1.provides="+devSkuProductSubscription1.provides+"  devSkuProductSubscription2.provides="+devSkuProductSubscription2.provides);
		if (complianceStatus.equalsIgnoreCase("valid")) {
			Assert.assertTrue(devSkuProductSubscription2.poolId.equals(devSkuProductSubscription1.poolId), "When system is already compliant, a dev_sku enabled system that has been re-autosubscribed will not be granted another product subscription from a different pool id (should not be needed since system is already green).");
			Assert.assertTrue(devSkuProductSubscription2.serialNumber.equals(devSkuProductSubscription1.serialNumber), "When system is already compliant, a dev_sku enabled system that has been re-autosubscribed will not be granted another product subscription with a different serial (should not be needed since system is already green).");
		} else {
			Assert.assertTrue(!devSkuProductSubscription2.poolId.equals(devSkuProductSubscription1.poolId), "When system is not already compliant, dev_sku enabled system that has been re-autosubscribed is granted another product subscription from a different pool id.");
			Assert.assertTrue(!devSkuProductSubscription2.serialNumber.equals(devSkuProductSubscription1.serialNumber), "When system is not already compliant, a dev_sku enabled system that has been re-autosubscribed is granted another product subscription with a different serial.");
		}
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-26779", "RHEL7-51858"})
	@Test(	description="configure the system with custom facts for a dev_sku, register the system with auto-subscribe and verify that a cost-based subscription can be manually attached without affecting the devSku entitlement",
			groups={"blockedByBug-1298577"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyManualSubscribesWhileConsumingDevSkuEntitlement_Test() throws JSONException, Exception {

		// get a valid dev_sku to test with
		List<Object> l = getRandomValidDevSkuData();
		String devSku = (String) l.get(1);
		String devPlatform = (String) l.get(2);
///*debugTesting*/devSku = "dev-sku-product";devPlatform = "dev-platform";
		
		// instrument the system facts to behave as a vagrant image
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("dev_sku",devSku);
		factsMap.put("dev_platform",devPlatform);
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		
		// register with autosubscribe and force (to unregister anyone that is already registered)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		
		// get the autosubscribed productSubscription
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptions.size(), 1, "After registering (with autosubscribe) a system with dev_sku fact '"+devSku+"', only one product subscription should be consumed.");
		ProductSubscription devSkuProductSubscription1 = productSubscriptions.get(0);
		List<String> devSkuProductSubscriptionProvidedProductModifiedIds = new ArrayList<String>(CandlepinTasks.getPoolProvidedProductModifiedIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, devSkuProductSubscription1.poolId));
		
		// manually subscribe to any available pool and test
		List<SubscriptionPool> availableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		for (SubscriptionPool subscriptionPool : getRandomSubsetOfList(availableSubscriptionPools,3)) {
///*debugTesting*/ subscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName", "Awesome OS with up to 4 virtual guests", availableSubscriptionPools); // causes: 1 local certificate has been deleted.
			// manually subscribe to the available cost-based subscription
			clienttasks.subscribe(null, null, subscriptionPool.poolId, null, null, null, null, null, null, null, null, null, null);
			
			// assert that the consumed subscriptions still includes the consumed devSkuProductSubscription
			productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
			ProductSubscription devSkuProductSubscription2 = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", devSkuProductSubscription1.poolId, productSubscriptions);
			Assert.assertNotNull(devSkuProductSubscription2, "After manually attaching a cost-based subscription, an entitlement for devSku '"+devSku+"' from pool id '"+devSkuProductSubscription1.poolId+"' is still being consumed.");
			//Assert.assertEquals(devSkuProductSubscription2.serialNumber, devSkuProductSubscription1.serialNumber, "After manually attaching a cost-based subscription, the same serial number from an entitlement for devSku '"+devSku+"' from pool id '"+devSkuProductSubscription1.poolId+"' is still being consumed.");
			// The prior assert on equal serial numbers fails when the devSkuProductSubscription1 provides a modifier product that modifies a provided product in subscriptionPool
			// Instead let's determine if devSkuProductSubscription1 does indeed provide modifier products that are modified by products provided by subscriptionPool...
			List<String> subscriptionPoolProvidedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, subscriptionPool.poolId);
			if (!doesListOverlapList(subscriptionPoolProvidedProductIds, devSkuProductSubscriptionProvidedProductModifiedIds)) {
				Assert.assertEquals(devSkuProductSubscription2.serialNumber, devSkuProductSubscription1.serialNumber, "After manually attaching a cost-based subscription, the same serial number from an entitlement for devSku '"+devSku+"' from pool id '"+devSkuProductSubscription1.poolId+"' is still being consumed (because the cost-based subscription does not provide any of the products "+devSkuProductSubscriptionProvidedProductModifiedIds+" that are modified by the provided modifier products of the devSku.");
			} else devSkuProductSubscription1.serialNumber=devSkuProductSubscription2.serialNumber;	// save for the next loop's serial assert
		}
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21993", "RHEL7-51855"})
	@Test(	description="configure the system with custom facts for a dev_sku, register the system with auto-subscribe, alter the dev_sku facts, re-autosubscribe, and verify the initial entitlement was purged",
			groups={"blockedByBug-1295452"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutosubscribeAfterChangingDevSkuFacts_Test() throws JSONException, Exception {
		
		// register with force to get a fresh consumer
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg,null,null,null,null,false,null,null,(List)null,null,null,null,true,false,null,null,null, null);
		
		// find two value SKUs that can be used as a dev_sku
		List <SubscriptionPool> subscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		String devSku1=null, devSku2=null;
		for (SubscriptionPool subscriptionPool : getRandomList(subscriptionPools)) {
			if (devSku1==null) devSku1=subscriptionPool.productId;
			if (devSku2==null && devSku1!=null && devSku1!=subscriptionPool.productId) devSku2=subscriptionPool.productId;
			if (devSku2!=null && devSku1!=null) break;
		}
		if (devSku1==null || devSku2==null) throw new SkipException("Could not find two available SKUs to execute this test.");
		
		// instrument the system facts to behave as a vagrant image with devSku1
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("dev_sku",devSku1);
		factsMap.put("dev_platform","dev_platform_for_"+devSku1);
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		
		// autosubscribe
		clienttasks.subscribe(true, null, null, null, (String)null, null, null, null, null, null, null, null, null);
		
		// get the autosubscribed entitlement
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptions.size(), 1, "After autosubscribing a system with dev_sku fact '"+devSku1+"', only one product subscription should be consumed.");
		ProductSubscription devSkuProductSubscription1 = productSubscriptions.get(0);
		Assert.assertEquals(devSkuProductSubscription1.productId, devSku1, "The consumed entitlement SKU after autosubscribing a system with dev_sku fact '"+devSku1+"'.");

		// instrument the system facts to behave as a vagrant image with devSku2
		factsMap.put("dev_sku",devSku2);
		factsMap.put("dev_platform","dev_platform_for_"+devSku2);
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		
		// workaround for "All installed products are covered by valid entitlements. No need to update subscriptions at this time."
		// which will cause the final assert to fail because the system will have no need to re-autosubscribe to devSku2
		if (clienttasks.getFactValue("system.entitlements_valid").equalsIgnoreCase("valid")) {
			// simply remove the devSkuProductSubscription1 subscription
			clienttasks.unsubscribe_(null, devSkuProductSubscription1.serialNumber, null, null, null, null, null);
		}
		
		// autosubscribe again
		clienttasks.subscribe(true, null, null, null, (String)null, null, null, null, null, null, null, null, null);
		
		// get the autosubscribed entitlement
		productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptions.size(), 1, "After autosubscribing a system with dev_sku fact altered to '"+devSku2+"', only one product subscription should be consumed (the '"+devSku1+"' entitlement should have been purged)");	// fails prior to the fix for Bug 1295452 - After altering the dev_sku value in the facts file , two CDK subscriptions exists on the machine
		ProductSubscription devSkuProductSubscription2 = productSubscriptions.get(0);
		Assert.assertEquals(devSkuProductSubscription2.productId, devSku2, "The consumed entitlement SKU after autosubscribing a system with dev_sku fact altered to '"+devSku2+"'.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21994", "RHEL7-51856"})
	@Test(	description="configure the system with custom facts for a dev_sku, register the system with auto-subscribe, alter the dev_sku facts, re-autosubscribe, and verify the initial entitlement was purged",
			groups={"blockedByBug-1294465","VerifyAutosubscribedDevSkuWithAnUnknownProductInstalled_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutosubscribedDevSkuWithAnUnknownProductInstalled_Test() throws JSONException, Exception {
		// unregister to get rid of current consumer
		clienttasks.unregister(null, null, null, null);
		
		// verify that an unknown product is installed
		String productId = "88888888";
		Assert.assertNotNull(ProductCert.findFirstInstanceWithMatchingFieldFromList("id", productId, clienttasks.getCurrentProductCerts()), "Unknown product cert id '"+productId+"' is installed.");
		
		// get a valid dev_sku to test with
		List<Object> l = getRandomValidDevSkuData();
		String devSku = (String) l.get(1);
		String devPlatform = (String) l.get(2);
		
		// instrument the system facts to behave as a vagrant image
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("dev_sku",devSku);
		factsMap.put("dev_platform",devPlatform);
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		
		// register with autosubscribe and force (to unregister anyone that is already registered)
		SSHCommandResult result = clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		String expectedMsg = "Unable to find available subscriptions for all your installed products.";
		Assert.assertTrue(result.getStdout().trim().endsWith(expectedMsg),"Register with autosubscribe ends with this message when an unknown product is installed '"+expectedMsg+"'.");
		
		// get the autosubscribed productSubscription
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptions.size(), 1, "After registering (with autosubscribe) a system with dev_sku fact '"+devSku+"', only one product subscription should be consumed even though an unknown product id '"+productId+"' is installed.");	// failed prior to the fix for Bug 1294465 - Runtime Error null is observed while registering a client (cdk env) with a unknown product cert installed
		ProductSubscription devSkuProductSubscription = productSubscriptions.get(0);
		Assert.assertEquals(devSkuProductSubscription.productId, devSku, "The consumed entitlement SKU after autosubscribing a system with dev_sku fact '"+devSku+"' while an unknown product id '"+productId+"' is installed.");
	}
	
	@BeforeGroups(groups="setup", value={"VerifyAutosubscribedDevSkuWithAnUnknownProductInstalled_Test"} )
	public void installGenericProductCert() throws JSONException, Exception{
		
		// save time and copy the generic product cert to the client when it already exists within this testware (needed for testing against hosted)
		String filename = "generic.pem";
		File genericFile = new File(System.getProperty("automation.dir", null)+"/certs/"+filename);
		genericCertFilePath = clienttasks.productCertDir.replaceFirst("/*$", "/")+filename;
		genericCertFilePath = genericCertFilePath.replaceFirst("_?\\.pem$", "_.pem");	// make it end in _.pem by convention to help identify fake product certs
		if (genericFile.exists()) {
			RemoteFileTasks.putFile(client.getConnection(), genericFile.toString(), genericCertFilePath, "0644");
			return;
		} // otherwise, create a new generic product cert...
		
		
		if (server==null) {
			log.warning("Skipping createGenericProductCert() when server is null.");
			return;	
		}
		
		String productId = "88888888";
		String name = "Generic OS Product";
		String resourcePath = "/products/"+productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		
		// delete the product
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		
		// create the product
		Map<String,String> attributes = new HashMap<String,String>();
		attributes.put("name", name);
		attributes.put("version", "8.8 Alpha");
		attributes.put("arch", "ALL");
		attributes.put("arch", "x86_64,ia64,x86,ppc,ppc64,ppcle64,s390,s390x,aarch64");	// equivalent to ALL
		attributes.put("tags", "genos");	// TODO	this is not the correct way to set tags
		attributes.put("brand_type", "OS");
		attributes.put("brand_name", "Generic Branding");	// TODO this is not the correct way to set brand name
		attributes.put("type", "SVC");	// indicative of an engineering product
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, name, productId, 1, attributes, null);

		// now install the product certificate
		JSONObject jsonProductCert = new JSONObject (CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath+"/certificate"));
		String cert = jsonProductCert.getString("cert");
		String key = jsonProductCert.getString("key");
		client.runCommandAndWait("echo \""+cert+"\" > "+genericCertFilePath);
		
		// delete the product since our goal was to simply to create and install a generic product cert
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
	}
	protected String genericCertFilePath = null;
	
	@AfterGroups(groups="setup", value={"VerifyAutosubscribedDevSkuWithAnUnknownProductInstalled_Test"} )
	public void removeGenericProductCert() {
		if (genericCertFilePath!=null && RemoteFileTasks.testExists(client, genericCertFilePath)) {
			client.runCommandAndWait("rm -f "+genericCertFilePath);
		}
	}
	
	
	
	
	
	// Candidates for an automated Test:
	// TODO CDK Bugs
	//DONE	// 1295452 	After altering the dev_sku value in the facts file , two CDK subscriptions exists on the machine 
	//DONE	// 1292877 	Runtime Error null is encountered after a secondary auto-attach while consuming a CDK entitlement plus a Standard entitlement 
	//DONE	// 1298577  Manually attaching a subscription on a system that already has a cdk subscription , re-attaches the cdk subscription
	//DONE	// 1297863  to account for daylight savings events, dev_sku (CDK) entitlements should add Calendar.DATE units of expires_after to establish the subscription end date
	//DONE	// 1294465 	Runtime Error null is observed while registering a client (cdk env) with a unknown product cert installed 
	
	
	// Configuration methods ***********************************************************************
	@SuppressWarnings("unused")
	@BeforeClass(groups={"setup"}, dependsOnMethods={"verifyCandlepinVersionBeforeClass"})
	public void setupBeforeClass() throws Exception {
if (false) { // keep for historical reference but never execute
		// restart candlepin in hosted mode (candlepin.standalone=false)
		if (CandlepinType.standalone.equals(sm_serverType)) {	// indicates that we are testing a standalone candlepin server
			servertasks.updateConfFileParameter("candlepin.standalone", "false");
			servertasks.uncommentConfFileParameter("module.config.hosted.configuration.module");
			servertasks.restartTomcat();
			servertasks.initialize(clienttasks.candlepinAdminUsername,clienttasks.candlepinAdminPassword,clienttasks.candlepinUrl);
		}
		// BEWARE: DO NOT RUN servertasks.refreshPoolsUsingRESTfulAPI(user, password, url, owner) OR IT WILL DELETE ALL SUBSCRIPTIONS AND POOLS IN CANDLEPIN 2.0+
} // Replacing code block above with the following redeployment of candlepin to avoid the BEWARE issue

		// re-deploy candlepin in hosted mode (candlepin.standalone=false)
		if (CandlepinType.standalone.equals(sm_serverType)) {	// indicates that we are testing a standalone candlepin server
			// avoid post re-deploy problems like: "System certificates corrupted. Please reregister." and "Unable to verify server's identity: [SSL: SSLV3_ALERT_CERTIFICATE_UNKNOWN] sslv3 alert certificate unknown (_ssl.c:579)"
			if (client1tasks!=null) client1tasks.removeAllCerts(true, true, false);
			if (client2tasks!=null) client2tasks.removeAllCerts(true, true, false);
			// update candlepin.conf and re-deploy
			servertasks.updateConfFileParameter("candlepin.standalone", "false");
			servertasks.addConfFileParameter("module.config.hosted.configuration.module","org.candlepin.hostedtest.AdapterOverrideModule");
			servertasks.redeploy();
			setupBeforeClassRedeployedCandlepin=true;
			// re-initialize after re-deploy
			servertasks.initialize(clienttasks.candlepinAdminUsername,clienttasks.candlepinAdminPassword,clienttasks.candlepinUrl);
			if (client1tasks!=null) client1tasks.installRepoCaCert(fetchServerCaCertFile(), sm_serverHostname.split("\\.")[0]+".pem");
			if (client2tasks!=null) client2tasks.installRepoCaCert(fetchServerCaCertFile(), sm_serverHostname.split("\\.")[0]+".pem");

		}
	}
    private boolean setupBeforeClassRedeployedCandlepin=false;

	@SuppressWarnings("unused")
	@AfterClass(groups={"setup"}, alwaysRun=true)	// dependsOnMethods={"verifyCandlepinVersionBeforeClass"} WILL THROW A TESTNG DEPENDENCY ERROR
	public void teardownAfterClass() throws Exception {
if (false) { // keep for historical reference but never execute
		if (CandlepinType.standalone.equals(sm_serverType)) {	// indicates that we are testing a standalone candlepin server
			servertasks.updateConfFileParameter("candlepin.standalone", "true");
			servertasks.commentConfFileParameter("module.config.hosted.configuration.module");
			servertasks.restartTomcat();
			servertasks.initialize(clienttasks.candlepinAdminUsername,clienttasks.candlepinAdminPassword,clienttasks.candlepinUrl);
			
			// 2/6/2017 BEWARE! toggling between "candlepin.standalone", to "false" and back to "true" will lock
			// engineering products.  Subsequent delete and modify requests will be blocked as occurred in a Tier3
			// run of ServiceLevelTests... 
			// 		SSH alternative to HTTP request: curl --stderr /dev/null --insecure --user admin:admin --request DELETE https://jsefler-candlepin6.usersys.redhat.com:8443/candlepin/owners/admin/products/99000
			// 		Attempt to DELETE resource '/owners/admin/products/99000' failed: product "99000" is locked
			// A. workaround to avoid the subsequent error is to delete the products ahead of time
			// B. another workaround is to re-deploy the database with -g  regenerates the database, -t inserts test data along with it after restarting tomcat.
			// C. most reliable workaround is to update the locked columns for all of the cp2_products and cp2_content tables
			updateProductAndContentLockStateOnDatabase(0); // unlock all product and content after toggling out of hosted mode
		}
} // Replacing code block above with the following redeployment of candlepin (workaround B) to avoid the BEWARE issue
		
		if (CandlepinType.standalone.equals(sm_serverType) && setupBeforeClassRedeployedCandlepin) {	// indicates that we are testing a standalone candlepin server
			// avoid post re-deploy problems like: "System certificates corrupted. Please reregister." and "Unable to verify server's identity: [SSL: SSLV3_ALERT_CERTIFICATE_UNKNOWN] sslv3 alert certificate unknown (_ssl.c:579)"
			if (client1tasks!=null) client1tasks.removeAllCerts(true, true, false);
			if (client2tasks!=null) client2tasks.removeAllCerts(true, true, false);
			// update candlepin.conf and re-deploy
			servertasks.updateConfFileParameter("candlepin.standalone", "true");
			servertasks.removeConfFileParameter("module.config.hosted.configuration.module");   
			servertasks.redeploy();
			// re-initialize after re-deploy
			servertasks.initialize(clienttasks.candlepinAdminUsername,clienttasks.candlepinAdminPassword,clienttasks.candlepinUrl);
			deleteSomeSecondarySubscriptionsBeforeSuite();
    		if (client1tasks!=null) client1tasks.installRepoCaCert(fetchServerCaCertFile(), sm_serverHostname.split("\\.")[0]+".pem");
    		if (client2tasks!=null) client2tasks.installRepoCaCert(fetchServerCaCertFile(), sm_serverHostname.split("\\.")[0]+".pem");
		}
		
		clienttasks.removeAllCerts(true, true, false);	// to force the removal of the consumer and subscriptions if any
		clienttasks.deleteFactsFileWithOverridingValues();	// to get rid of the dev_sku settings
	}

	@BeforeClass(groups="setup")
	public void verifyCandlepinVersionBeforeClass() {
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<", "2.0.8-1") &&
			!SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "==", "0.9.51.13-1") /* hot fix */ &&
			!SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "==", "0.9.51.14-1") /* hot fix */ ){
			throw new SkipException("this candlepin version '"+servertasks.statusVersion+"' does not support DevSku functionality.");
		}
	}
	
	// Protected methods ***********************************************************************


	/**
	 * @return a valid row from getDevSkuDataAsListOfLists() which is a List of Object bugzilla, String devSku, String devPlatform
	 * @throws JSONException
	 * @throws Exception
	 */
	protected List<Object> getRandomValidDevSkuData() throws JSONException, Exception {
		
		for (List<Object> l : getRandomList(getDevSkuDataAsListOfLists())) {
			// get the JSON product representation of the devSku 
			String resourcePath = "/products/"+l.get(1);
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
			JSONObject jsonDevSkuProduct = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, resourcePath));
			// Note on candlepin-2.0.8-1 the above call to getResourceUsingRESTfulAPI fails because candlepin returns an empty string (no JSON)
			if (!jsonDevSkuProduct.has("displayMessage")) {	// "displayMessage": "Product with ID 'MCT3295' could not be found."
				// l: Object bugzilla, String devSku, String devPlatform
				return l;
			}
		}
		throw new SkipException("Could not find a random valid row of DevSkuData to execute this test."); 
	}
	
	
	// Data Providers ***********************************************************************
	@DataProvider(name="getDevSkuData")
	public Object[][] getDevSkuDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getDevSkuDataAsListOfLists());
	}
	protected List<List<Object>> getDevSkuDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// This is a list of valid and invalid SKUs (depending on execution against stage env or onpremise candlepin with TESTDATA deployed)
		// The invalid SKUs are employed to assert graceful negative testing.
		// Object bugzilla, String devSku, String devPlatform
		ll.add(Arrays.asList(new Object[]{null,	"dev-mkt-product"/*Development SKU Product*/, "dev-platform"}));	
		ll.add(Arrays.asList(new Object[]{null,	"dev-sku-product"/*Development SKU Product*/, "dev-platform"}));	
		ll.add(Arrays.asList(new Object[]{null,	"MCT3295"/*Internal Shadow Sku CDK*/, "vagrant"}));
		ll.add(Arrays.asList(new Object[]{null,	"awesomeos-everything"/*Awesome OS for x86_64/i686/ia64/ppc/ppc64/s390x/s390*/, "awesomeos-platform"}));	// chosen because its service_level is not set
		
		return ll;
	}
	
	
}
