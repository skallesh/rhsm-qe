package rhsm.cli.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.RemoteFileTasks;

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
@Test(groups={"DevSKUTests","Tier3Tests","AcceptanceTests"})
public class DevSKUTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************

	@Test(	description="given an available SKU, configure the system with custom facts dev_sku=SKU, register the system with auto-attach and verify several requirements of the attached entitlement",
			groups={""},
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
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, null, null, null, null);
		
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
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen && clienttasks.redhatReleaseX.equals("6") && clienttasks.isPackageVersion("subscription-manager", ">=", "1.15")) {
				// NOTE: This will be an hour off when the duration crosses the "Fall" back or "Spring" forward daylight saving dates.
				expectedEndDate.add(Calendar.HOUR, Integer.valueOf(defaultExpiresAfter)*24);
			} else
			// END OF WORKAROUND
			expectedEndDate.add(Calendar.DATE, Integer.valueOf(defaultExpiresAfter));
			Assert.assertEquals(ConsumerCert.formatDateString(devSkuEntitlement.validityNotAfter), ConsumerCert.formatDateString(expectedEndDate), "When no expires_after attribute exists on the devSku product, the entitlement's validityNotAfter date defaults to '"+defaultExpiresAfter+"' days after the consumer was registered date ("+ConsumerCert.formatDateString(consumerCert.validityNotBefore)+").");
		} else {
			// TEMPORARY WORKAROUND
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1297863"; // Bug 1297863 - to account for daylight savings events, dev_sku (CDK) entitlements should add Calendar.DATE units of expires_after to establish the subscription end date
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen && clienttasks.redhatReleaseX.equals("6") && clienttasks.isPackageVersion("subscription-manager", ">=", "1.15")) {
				// NOTE: This will be an hour off when the duration crosses the "Fall" back or "Spring" forward daylight saving dates.
				expectedEndDate.add(Calendar.HOUR, Integer.valueOf(devSkuExpiresAfter)*24);
			} else
			// END OF WORKAROUND
			expectedEndDate.add(Calendar.DATE, Integer.valueOf(devSkuExpiresAfter));
			Assert.assertEquals(ConsumerCert.formatDateString(devSkuEntitlement.validityNotAfter), ConsumerCert.formatDateString(expectedEndDate), "When an expires_after attribute exists on the devSku product, the entitlement's validityNotAfter is '"+devSkuExpiresAfter+"' days after the consumer was registered date ("+ConsumerCert.formatDateString(consumerCert.validityNotBefore)+").");
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
	
	@DataProvider(name="getDevSkuData")
	public Object[][] getDevSkuDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getDevSkuDataAsListOfLists());
	}
	protected List<List<Object>> getDevSkuDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
//		if (sm_serverType.equals(CandlepinType.standalone)) {
			// Object bugzilla, String devSku, String devPlatform
			ll.add(Arrays.asList(new Object[]{null,	"dev-mkt-product"/*Development SKU Product*/, "vagrant"}));	
			ll.add(Arrays.asList(new Object[]{null,	"dev-sku-product"/*Development SKU Product*/, "vagrant"}));	
			ll.add(Arrays.asList(new Object[]{null,	"MCT3295"/*Internal Shadow Sku CDK*/, "vagrant"}));
			ll.add(Arrays.asList(new Object[]{null,	"awesomeos-everything"/*Awesome OS for x86_64/i686/ia64/ppc/ppc64/s390x/s390*/, "awesomeos-platform"}));	// chose because since it service_level is not set
			
			
//		}

		return ll;
	}
	
	
	
	@Test(	description="configure the system with custom facts for a dev_sku, register the system with auto-subscribe verify the attached entitlement can be removed and re-auto-attached",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyDevSkuEntitlementCanBeRemovedAndReAttached_Test() throws JSONException, Exception {
		
		// choose a valid dev_sku to test with
		String devSku=null, devPlatform=null;
		for (List<Object> l : getRandomList(getDevSkuDataAsListOfLists())) {
			// get the JSON product representation of the devSku 
			String resourcePath = "/products/"+l.get(1);
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
			JSONObject jsonDevSkuProduct = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, resourcePath));
			if (!jsonDevSkuProduct.has("displayMessage")) {
				// l: Object bugzilla, String devSku, String devPlatform
				devSku = (String) l.get(1);
				devPlatform = (String) l.get(2);
				break;
			}
		}
		if (devSku==null) throw new SkipException("Could not find a valid product to execute this test."); 
		
		// instrument the system facts to behave as a vagrant image
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("dev_sku",devSku);
		factsMap.put("dev_platform",devPlatform);
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		
		// register with autosubscribe and force (to unregister anyone that is already registered)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, null, null, null, null);
		
		// get the autosubscribed productSubscription
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptions.size(), 1, "After registering (with autosubscribe) a system with dev_sku fact '"+devSku+"', only one product subscription should be consumed.");
		ProductSubscription devSkuProductSubscription1 = productSubscriptions.get(0);
		
		// remove it
		clienttasks.unsubscribe(null, devSkuProductSubscription1.serialNumber, null, null, null, null);
		
		// re-autosubscribe
		clienttasks.subscribe(true, null, null, null, (String)null, null, null, null, null, null, null, null);
		
		// get the re-autosubscribed entitlement
		productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptions.size(), 1, "After re-autosubscribing a system with dev_sku fact '"+devSku+"', only one product subscription should be consumed.");
		ProductSubscription devSkuProductSubscription2 = productSubscriptions.get(0);
		
		// assert the re-autosubscribes product subscriptions match (except for pool id and serial)
		Assert.assertEquals(ProductSubscription.formatDateString(devSkuProductSubscription2.endDate), ProductSubscription.formatDateString(devSkuProductSubscription1.endDate), "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription with the same end date.");
		Assert.assertEquals(devSkuProductSubscription2.serviceLevel, devSkuProductSubscription1.serviceLevel, "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription with the same service level.");
		Assert.assertEquals(devSkuProductSubscription2.quantityUsed, devSkuProductSubscription1.quantityUsed, "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription with the quantity used.");
		Assert.assertEquals(devSkuProductSubscription2.provides, devSkuProductSubscription1.provides, "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription that provides the same products.");
		Assert.assertTrue(!devSkuProductSubscription2.poolId.equals(devSkuProductSubscription1.poolId), "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription from a different pool id.");
		Assert.assertTrue(!devSkuProductSubscription2.serialNumber.equals(devSkuProductSubscription1.serialNumber), "A dev_sku enabled system that has been re-autosubscribed is granted another product subscription with a different serial.");
	}
	
	
	@Test(	description="configure the system with custom facts for a dev_sku, register the system with auto-subscribe and verify that and attempt to redundantly autosubscribed is handled gracefully",
			groups={"debugTest"},
			enabled=false)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyRedundantAutosubscribesForDevSkuEntitlementAreHandled_Test() throws JSONException, Exception {
		// TODO
	}
	
	// Candidates for an automated Test:
	// TODO CDK Bugs
	// 1295452 	After altering the dev_sku value in the facts file , two CDK subscriptions exists on the machine 
	// 1294465 	Runtime Error null is observed while registering a client (cdk env) with a unknown product cert installed 
	// 1292877 	Runtime Error null is encountered after a secondary auto-attach while consuming a CDK entitlement plus a Standard entitlement 
	// 
	
	
	
	// Configuration methods ***********************************************************************
	@BeforeClass(groups="setup")
	public void setupBeforeClass() throws IOException, JSONException {
		// restart candlepin in hosted mode (candlepin.standalone=false)
		if (CandlepinType.standalone.equals(sm_serverType)) {	// indicates that we are testing a standalone candlepin server
			servertasks.updateConfFileParameter("candlepin.standalone", "false");
			servertasks.uncommentConfFileParameter("module.config.hosted.configuration.module");
			servertasks.restartTomcat();
			servertasks.initializeStatus(sm_serverUrl);
		}
		// BEWARE: DO NOT RUN servertasks.refreshPoolsUsingRESTfulAPI(user, password, url, owner) OR IT WILL DELETE ALL SUBSCRIPTIONS AND POOLS IN CANDLEPIN 2.0+
	}

	@AfterClass(groups="setup")
	public void teardownAfterClass() throws JSONException, IOException {
		if (CandlepinType.standalone.equals(sm_serverType)) {	// indicates that we are testing a standalone candlepin server
			servertasks.updateConfFileParameter("candlepin.standalone", "true");
			servertasks.commentConfFileParameter("module.config.hosted.configuration.module");
			servertasks.restartTomcat();
			servertasks.initializeStatus(sm_serverUrl);
		}
	}

	@BeforeClass(groups="setup")
	public void verifyCandlepinVersionBeforeClass() {
	if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<", "2.0.8-1") &&
		SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "!=", "0.9.51.13")) {
			throw new SkipException("this candlepin version '"+servertasks.statusVersion+"' does not support DevSku functionality.");
		}
	}
	
	// Protected methods ***********************************************************************

}