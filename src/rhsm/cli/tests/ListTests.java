package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.CandlepinType;
import rhsm.base.ConsumerType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.OrderNamespace;
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
 *  @author ssalevan
 *  @author jsefler
 *
 */
@Test(groups={"ListTests"})
public class ListTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37701", "RHEL7-51341"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: list available subscriptions (when not consuming)",
			groups={"Tier2Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=41678)
	public void testAvailableSubscriptionsListed() {
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		String availableSubscriptionPools = clienttasks.listAvailableSubscriptionPools().getStdout();
		Assert.assertContainsMatch(availableSubscriptionPools, "Available Subscriptions","" +
				"Available Subscriptions are listed for '"+sm_clientUsername+"' to consume.");
		Assert.assertContainsNoMatch(availableSubscriptionPools, "No available subscription pools to list",
				"Available Subscriptions are listed for '"+sm_clientUsername+"' to consume.");

		log.warning("These manual TCMS instructions are not really achievable in this automated test...");
		log.warning(" * List produced matches the known data contained on the Candlepin server");
		log.warning(" * Confirm that the marketing names match.. see prereq link https://engineering.redhat.com/trac/IntegratedMgmtQE/wiki/sm-prerequisites");
		log.warning(" * Match the marketing names w/ https://www.redhat.com/products/");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19963", "RHEL7-33104"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager-cli: list available subscriptions - verify that among all the subscriptions available to this consumer, those that satisfy the system hardware are listed as available",
			groups={"Tier1Tests", "blockedByBug-712502","unsubscribeBeforeGroup"},
			dataProvider="getAvailableSystemSubscriptionPoolProductData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41678)
	public void testHardwareMatchingSubscriptionsAreListedAsAvailable(String productId, JSONArray bundledProductDataAsJSONArray) throws JSONException, Exception {
///*debugTesting*/if (!productId.equals("awesomeos-virt-unlmtd-phys")) throw new SkipException("debugTesting productId="+productId);
///*debugTesting*/if (!productId.equals("MCT3115")) throw new SkipException("debugTesting productId="+productId);
		// implicitly registered in dataProvider; no need to register with force; saves time
		//clienttasks.register(clientusername, clientpassword, null, null, null, null, true, null, null, null);
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", productId, clienttasks.getCurrentlyAvailableSubscriptionPools());
// THIS CASE WAS SOLVED BY REFRESHING THE POOLS AVAILABLE TO THIS OWNER
//		// special case...
//		if (pool==null) {	// when pool is null, a likely reason is that the subscription pools arch does not support this system's arch, let's check...
//			for (String poolId: CandlepinTasks.getPoolIdsForProductId(sm_clientUsername, sm_clientPassword, sm_serverUrl, clienttasks.getCurrentlyRegisteredOwnerKey(), productId)) {
//				String poolProductAttributeArchValue= (String) CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, poolId, "arch");
//				if (poolProductAttributeArchValue!=null) {
//					List<String> poolProductAttributesArches = new ArrayList<String>();
//						poolProductAttributesArches.addAll(Arrays.asList(poolProductAttributeArchValue.trim().split(" *, *")));	// Note: the arch attribute can be a comma separated list of values
//					if (poolProductAttributesArches.contains("x86")) {poolProductAttributesArches.addAll(Arrays.asList("i386","i486","i586","i686"));}  // Note: x86 is a general arch to cover all 32-bit intel microprocessors 
//					if (poolProductAttributesArches.contains("ALL")) poolProductAttributesArches.add(clienttasks.arch);
//					if (!poolProductAttributesArches.contains(clienttasks.arch)) {
//						throw new SkipException("Pool product '"+productId+"' supports arches '"+poolProductAttributeArchValue+"'.  This system's arch is '"+clienttasks.arch+"', hence this matching hardware test for list available is not applicable on this system.");
//					}
//				}
//			}	
//		}
		// special case...
		if (pool==null) {
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
		Assert.assertNotNull(pool, "Expected SubscriptionPool with ProductId '"+productId+"' is listed as available for subscribing.  (Look for warnings above to explain a failure. A pool refresh may also fix a failure.)");
	}
	
	
	@Test(	description="subscription-manager-cli: list available subscriptions - verify that among all the subscriptions available to this consumer, those that do NOT satisfy the hardware are NOT listed as available",
			groups={"Tier1Tests", "blockedByBug-712502","unsubscribeBeforeGroup"},
			dataProvider="getNonAvailableSystemSubscriptionPoolProductData",
			enabled=false)	// TODO: 4/30/2014 This test is flawed.  The data provider for this test should be based on poolIds, not productIds.  Because a physical_only pool with a virt_limit can create a BONUS pool, the productId can be available to both physical and virtual systems under two different poolIds.  e.g. "productId": "awesomeos-virt-unlmtd-phys" from TESTDATA which causes this test to fail erroneously. 
	@ImplementsNitrateTest(caseId=41678)
	public void EnsureNonHardwareMatchingSubscriptionsAreNotListedAsAvailable_Test(String productId) {
		// implicitly registered in dataProvider; no need to register with force; saves time
		//clienttasks.register(clientusername, clientpassword, null, null, null, null, true, null, null, null);
		
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", productId, clienttasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNull(pool, "As expected, SubscriptionPool with ProductId '"+productId+"' is NOT listed as available for subscribing.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47866", "RHEL7-93038"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: list consumed entitlements (when not consuming)",
			groups={"Tier2Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=41679)
	public void testNoConsumedEntitlementsListed() {
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		String consumedProductSubscription = clienttasks.listConsumedProductSubscriptions().getStdout();
		String expectedMsg = "No consumed subscription pools to list";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.20.2-1")) {	// commit da72dfcbbb2c3a44393edb9e46e1583d05cc140a
			expectedMsg="No consumed subscription pools were found.";
		}
		Assert.assertContainsMatch(consumedProductSubscription, expectedMsg,
				"No consumed subscription pools listed for '"+sm_clientUsername+"' after registering (without autosubscribe).");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27118", "RHEL7-51350"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: list consumed entitlements",
			groups={"Tier2Tests"},
			dataProvider="getAllSystemSubscriptionPoolProductData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41679)
	public void testConsumedEntitlementsListed(String productId, JSONArray bundledProductDataAsJSONArray) throws JSONException, Exception {
//if (!productId.equals("awesomeos-virt-unlmtd-phys")) throw new SkipException("debugTesting productId="+productId);
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", productId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(pool, "SubscriptionPool with ProductId '"+productId+"' is available for subscribing.");
		EntitlementCert  entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.subscribeToSubscriptionPool_(pool));
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertTrue(!consumedProductSubscriptions.isEmpty(),"The list of Consumed Product Subscription is NOT empty after subscribing to a pool with ProductId '"+productId+"'.");
		for (ProductSubscription productSubscription : consumedProductSubscriptions) {
			Assert.assertEquals(productSubscription.serialNumber, entitlementCert.serialNumber,
					"SerialNumber of Consumed Product Subscription matches the serial number from the current entitlement certificate.");
		}	
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37700", "RHEL7-51326"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: list installed products",
			groups={"Tier2Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testInstalledProductsListed() {
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);

		List <ProductCert> productCerts = clienttasks.getCurrentProductCerts();
		Set <String> productCertIds = clienttasks.getCurrentProductIds();
		String installedProductsAsString = clienttasks.listInstalledProducts().getStdout();
		//List <InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		List <InstalledProduct> installedProducts = InstalledProduct.parse(installedProductsAsString);

		// assert some stdout
		if (installedProducts.size()>0) {
			String bannerTitle = "Installed Product Status";
			Assert.assertTrue(installedProductsAsString.contains(bannerTitle), "The list of installed products is entitled '"+bannerTitle+"'.");
		}
		
		// assert the number of installed product matches the unique product certs Ids installed
		Assert.assertEquals(installedProducts.size(), productCertIds.size(), "A single product is reported as installed for each unique product cert ID found in "+clienttasks.productCertDir+" and "+clienttasks.productCertDefaultDir);

		// assert that each of the installed ProductCerts are listed as InstalledProducts with status "Not Subscribed"
		for (ProductCert productCert : productCerts) {
			InstalledProduct installedProduct = clienttasks.getInstalledProductCorrespondingToProductCert(productCert,installedProducts);
			Assert.assertNotNull(installedProduct, "The following installed product cert is included by subscription-manager in the list --installed: "+(installedProduct==null?"null":installedProduct));	
			Assert.assertEquals(installedProduct.status, "Not Subscribed", "The status of installed product when newly registered: "+installedProduct);
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37702", "RHEL7-51347"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: ensure list [--installed] produce the same results",
			groups={"Tier2Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testListAndListInstalledAreTheSame() throws JSONException, Exception {
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);

		// assert same results when no subscribed to anything...
		log.info("assert list [--installed] produce same results when not subscribed to anything...");
		SSHCommandResult listResult = clienttasks.list_(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
		SSHCommandResult listInstalledResult = clienttasks.list_(null, null, null, Boolean.TRUE, null, null, null, null, null, null, null, null, null, null, null);
		
		Assert.assertEquals(listResult.getStdout(), listInstalledResult.getStdout(), "'list' and 'list --installed' produce the same stdOut results.");
		Assert.assertEquals(listResult.getStderr(), listInstalledResult.getStderr(), "'list' and 'list --installed' produce the same stdErr results.");
		Assert.assertEquals(listResult.getExitCode(), listInstalledResult.getExitCode(), "'list' and 'list --installed' produce the same exitCode results.");
		
		
		// assert same results when subscribed to something...
		log.info("assert list [--installed] produce same results when subscribed to something...");
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribeToSubscriptionPool_(pool);
		listResult = clienttasks.list_(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
		listInstalledResult = clienttasks.list_(null, null, null, Boolean.TRUE, null, null, null, null, null, null, null, null, null, null, null);
		
		Assert.assertEquals(listResult.getStdout(), listInstalledResult.getStdout(), "'list' and 'list --installed' produce the same stdOut results.");
		Assert.assertEquals(listResult.getStderr(), listInstalledResult.getStderr(), "'list' and 'list --installed' produce the same stdErr results.");
		Assert.assertEquals(listResult.getExitCode(), listInstalledResult.getExitCode(), "'list' and 'list --installed' produce the same exitCode results.");
	}
	

	@Test(	description="subscription-manager: list of consumed entitlements should display consumed product marketing name",
			groups={"Tier2Tests"},
			dataProvider="getAllEntitlementCertsData",
			enabled=false)	// this test implementation is no longer valid after the change in format for consumed product subscriptions (from many to one) - see bug 806986
	@Deprecated
	@ImplementsNitrateTest(caseId=48092, fromPlan=2481)
	public void EnsureListConsumedMatchesProductsListedInTheEntitlementCerts_Test_DEPRECATED(EntitlementCert entitlementCert) {

		// assert: The list of consumed products matches the products listed in the entitlement cert
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		List<ProductSubscription> productSubscriptionsWithMatchingSerialNumber = ProductSubscription.findAllInstancesWithMatchingFieldFromList("serialNumber", entitlementCert.serialNumber, productSubscriptions);
		//Assert.assertTrue(productSubscriptionsWithMatchingSerialNumber.size()>0, "Found consumed product subscription(s) whose SerialNumber matches this entitlement cert: "+entitlementCert);
		//Assert.assertEquals(productSubscriptionsWithMatchingSerialNumber.size(),entitlementCert.productNamespaces.size(), "Found consumed product subscription(s) for each of the bundleProducts (total of '"+entitlementCert.productNamespaces.size()+"' expected) whose SerialNumber matches this entitlement cert: "+entitlementCert);
		int productSubscriptionsWithMatchingSerialNumberSizeExpected = entitlementCert.productNamespaces.size()==0?1:entitlementCert.productNamespaces.size(); // when there are 0 bundledProducts, we are still consuming 1 ProductSubscription
		Assert.assertEquals(productSubscriptionsWithMatchingSerialNumber.size(),productSubscriptionsWithMatchingSerialNumberSizeExpected, "Found consumed product subscription(s) for each of the bundleProducts (total of '"+productSubscriptionsWithMatchingSerialNumberSizeExpected+"' expected) whose SerialNumber matches this entitlement cert: "+entitlementCert);

		for (ProductNamespace productNamespace : entitlementCert.productNamespaces) {
			List<ProductSubscription> matchingProductSubscriptions = ProductSubscription.findAllInstancesWithMatchingFieldFromList("productName", productNamespace.name, productSubscriptionsWithMatchingSerialNumber);
			Assert.assertEquals(matchingProductSubscriptions.size(), 1, "Found one bundledProduct name '"+productNamespace.name+"' in the list of consumed product subscriptions whose SerialNumber matches this entitlement cert: "+entitlementCert);
			ProductSubscription correspondingProductSubscription = matchingProductSubscriptions.get(0);
			log.info("We are about to assert that this consumed Product Subscription: "+correspondingProductSubscription);
			log.info("...represents this ProductNamespace: "+productNamespace);
			log.info("...corresponding to this OrderNamespace: "+entitlementCert.orderNamespace);
			log.info("...from this EntitlementCert: "+entitlementCert);
			Assert.assertEquals(correspondingProductSubscription.productName, productNamespace.name, "productName from ProductSubscription in list --consumed matches productName from ProductNamespace in EntitlementCert.");
			Assert.assertEquals(correspondingProductSubscription.contractNumber, entitlementCert.orderNamespace.contractNumber, "contractNumber from ProductSubscription in list --consumed matches contractNumber from OrderNamespace in EntitlementCert.");
			Assert.assertEquals(correspondingProductSubscription.accountNumber, entitlementCert.orderNamespace.accountNumber, "accountNumber from ProductSubscription in list --consumed matches accountNumber from OrderNamespace in EntitlementCert.");
			Assert.assertEquals(correspondingProductSubscription.serialNumber, entitlementCert.serialNumber, "serialNumber from ProductSubscription in list --consumed matches serialNumber from EntitlementCert.");
			
			Calendar now = Calendar.getInstance();
			if (now.after(entitlementCert.orderNamespace.startDate) && now.before(entitlementCert.orderNamespace.endDate)) {
				Assert.assertTrue(correspondingProductSubscription.isActive, "isActive is True when the current time ("+EntitlementCert.formatDateString(now)+") is between the start/end dates in the EntitlementCert: "+entitlementCert);
			} else {
				Assert.assertFalse(correspondingProductSubscription.isActive, "isActive is False when the current time ("+EntitlementCert.formatDateString(now)+") is NOT between the start/end dates in the EntitlementCert: "+entitlementCert);
			}
			
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=660713 - jsefler 12/12/2010
			Boolean invokeWorkaroundWhileBugIsOpen = true;
			try {String bugId="660713"; if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("The workaround while this bug is open is to skip the assertion that: startDates and endDates match");
			} else {
			// END OF WORKAROUND
			Assert.assertEquals(ProductSubscription.formatDateString(correspondingProductSubscription.startDate), ProductSubscription.formatDateString(entitlementCert.orderNamespace.startDate), "startDate from ProductSubscription in list --consumed matches startDate from OrderNamespace ("+OrderNamespace.formatDateString(entitlementCert.orderNamespace.startDate)+") after conversion from GMT in EntitlementCert to local time.");
			Assert.assertEquals(ProductSubscription.formatDateString(correspondingProductSubscription.endDate), ProductSubscription.formatDateString(entitlementCert.orderNamespace.endDate), "endDate from ProductSubscription in list --consumed matches endDate from OrderNamespace ("+OrderNamespace.formatDateString(entitlementCert.orderNamespace.endDate)+") after conversion from GMT in EntitlementCert to local time.");
			}
		}
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27117", "RHEL7-51343"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: list of consumed entitlements should display the provided product marketing names",
			groups={"Tier2Tests","blockedByBug-878986","blockedByBug-976924"},
			dataProvider="getAllEntitlementCertsData",
			enabled=true)	// this new test implementation was implemented due to change in list of consumed product subscriptions (from many to one) - see bug 806986
	@ImplementsNitrateTest(caseId=48092, fromPlan=2481)
	public void testListConsumedMatchesProductsListedInTheEntitlementCerts(EntitlementCert entitlementCert) {

		// find the consumed product subscription corresponding to this entitlement cert and assert there is only one found
		List<ProductSubscription> allConsumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		List<ProductSubscription> productSubscriptions = ProductSubscription.findAllInstancesWithMatchingFieldFromList("serialNumber", entitlementCert.serialNumber, allConsumedProductSubscriptions);
		Assert.assertEquals(productSubscriptions.size(),1, "Found a single consumed product subscription with a matching serialNumber from this entitlementCert: "+entitlementCert);
		ProductSubscription productSubscription = productSubscriptions.get(0);
		List<String> providedProductNames = new ArrayList<String>();
		for (ProductNamespace productNamespace : entitlementCert.productNamespaces) providedProductNames.add(productNamespace.name);
		
		// TEMPORARY WORKAROUND FOR BUG
		if (entitlementCert.orderNamespace.supportLevel==null || entitlementCert.orderNamespace.supportType==null) {
			String bugId = "842170";
			boolean invokeWorkaroundWhileBugIsOpen = true;
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("While bug "+bugId+" is open, skipping assertion of consumed product subscription in list for entitlement's with a null support level/type.");
			}
		}
		// END OF WORKAROUND
		
		// when the entitlement OID value parsed was null, it should effectively be reported as ""  (Reference related bugs 842170 847354)
		if (entitlementCert.orderNamespace.supportLevel==null) entitlementCert.orderNamespace.supportLevel="";
		if (entitlementCert.orderNamespace.supportType==null) entitlementCert.orderNamespace.supportType="";
		
		
		//	Subscription Name:    	Awesome OS Server Bundled (2 Sockets, Standard Support)
		//	Provides:             	Clustering Bits
		//	                      	Awesome OS Server Bits
		//	                      	Shared Storage Bits
		//	                      	Management Bits
		//	                      	Large File Support Bits
		//	                      	Load Balancing Bits
		//	SKU:                  	awesomeos-server-2-socket-std
		//	Contract:             	36
		//	Account:              	12331131231
		//	Serial Number:        	6683485045354827351
		//	Active:               	True
		//	Quantity Used:        	1
		//	Service Level:        	Standard
		//	Service Type:         	L1-L3
		//	Starts:               	07/20/2012
		//	Ends:                 	07/20/2013
		
		Calendar now = Calendar.getInstance();
		
		// TEMPORARY WORKAROUND FOR BUG	
		boolean invokeWorkaroundWhileBugIsOpen = true;
		try {String bugId = "883486"; if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("The workaround while this bug is open is to compensate the expected entitlement start/end dates for daylight savings.");
			// adjust the expected entitlement dates for daylight savings time (changed by https://github.com/candlepin/subscription-manager/pull/385)
			// now.get(Calendar.DST_OFFSET) will equal 0 in the winter StandardTime; will equal 1000*60*60 in the summer DaylightSavingsTime (when the local time zone observes DST)
			entitlementCert.orderNamespace.startDate.add(Calendar.MILLISECOND, now.get(Calendar.DST_OFFSET)-entitlementCert.orderNamespace.startDate.get(Calendar.DST_OFFSET));
			entitlementCert.orderNamespace.endDate.add(Calendar.MILLISECOND, now.get(Calendar.DST_OFFSET)-entitlementCert.orderNamespace.endDate.get(Calendar.DST_OFFSET));
		}
		// END OF WORKAROUND
		
		// assert all of the product subscription's fields match the entitlement cert
		Assert.assertEquals(productSubscription.productName, entitlementCert.orderNamespace.productName, "productName from ProductSubscription in list --consumed matches productName from OrderNamespace in this entitlementCert");
		Assert.assertTrue(productSubscription.provides.containsAll(providedProductNames) && providedProductNames.containsAll(productSubscription.provides), "The consumed product subscription provides all the expected products "+providedProductNames+" from the provided ProductNamespaces in the entitlementCert.");
		Assert.assertEquals(productSubscription.productId, entitlementCert.orderNamespace.productId, "productId from ProductSubscription in list --consumed matches productId from OrderNamespace in this entitlementCert");
		Assert.assertEquals(productSubscription.contractNumber, entitlementCert.orderNamespace.contractNumber, "contractNumber from ProductSubscription in list --consumed matches contractNumber from OrderNamespace in this entitlementCert");
		Assert.assertEquals(productSubscription.accountNumber, entitlementCert.orderNamespace.accountNumber, "accountNumber from ProductSubscription in list --consumed matches accountNumber from OrderNamespace in this entitlementCert");
		if (now.after(entitlementCert.orderNamespace.startDate) && now.before(entitlementCert.orderNamespace.endDate)) {
			Assert.assertTrue(productSubscription.isActive, "isActive is True when the current time ("+EntitlementCert.formatDateString(now)+") is between the start/end dates in this entitlementCert");
		} else {
			Assert.assertFalse(productSubscription.isActive, "isActive is False when the current time ("+EntitlementCert.formatDateString(now)+") is NOT between the start/end dates in this entitlementCert");
		}
		Assert.assertEquals(productSubscription.quantityUsed.toString(), entitlementCert.orderNamespace.quantityUsed, "quantityUsed from ProductSubscription in list --consumed matches quantityUsed from OrderNamespace in this entitlementCert");
		Assert.assertEquals(productSubscription.serviceLevel, entitlementCert.orderNamespace.supportLevel, "serviceLevel from ProductSubscription in list --consumed matches supportLevel from OrderNamespace in this entitlementCert");
		Assert.assertEquals(productSubscription.serviceType, entitlementCert.orderNamespace.supportType, "serviceType from ProductSubscription in list --consumed matches serviceType from OrderNamespace in this entitlementCert");
		Assert.assertEquals(ProductSubscription.formatDateString(productSubscription.startDate), ProductSubscription.formatDateString(entitlementCert.orderNamespace.startDate), "startDate from ProductSubscription in list --consumed matches startDate from OrderNamespace ("+OrderNamespace.formatDateString(entitlementCert.orderNamespace.startDate)+") after conversion from GMT in EntitlementCert to local time.");
		Assert.assertEquals(ProductSubscription.formatDateString(productSubscription.endDate), ProductSubscription.formatDateString(entitlementCert.orderNamespace.endDate), "endDate from ProductSubscription in list --consumed matches endDate from OrderNamespace ("+OrderNamespace.formatDateString(entitlementCert.orderNamespace.endDate)+") after conversion from GMT in EntitlementCert to local time.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36551", "RHEL7-51328"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: list of consumed subscriptions should report the poolId from which the entitlement originated",
			groups={"Tier2Tests","blockedByBug-908671"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=, fromPlan=)
	public void testListConsumedReportsOriginatingPoolId() {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		// subscribe to a randomly available pool
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribe(null,null,pool.poolId,null,null,null,null,null,null,null,null, null, null);
		
		// verify the list of consumed subscriptions reports the pool.poolId
		List<ProductSubscription> consumedProductSubscriptionsFromPool = ProductSubscription.findAllInstancesWithMatchingFieldFromList("poolId", pool.poolId, clienttasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertNotNull(consumedProductSubscriptionsFromPool, "Successfully found the consumed subscription reporting the poolId '"+pool.poolId+"' that we just attached.");
		Assert.assertEquals(consumedProductSubscriptionsFromPool.size(), 1, "The number of consumed subscriptions reporting the poolId '"+pool.poolId+"' that we just attached.");
		Assert.assertEquals(consumedProductSubscriptionsFromPool.get(0).poolId, pool.poolId, "Redundant assertion on matching poolId between the attached subscription and the list of consumed subscriptions.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36550", "RHEL7-51327"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: list of available subscriptions should include contract number",
			groups={"Tier2Tests","blockedByBug-1007580","blockedByBug-1088507"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=, fromPlan=)
	public void testListAvailableReportsContract() {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		// assert the contract value in all available pools
		boolean availableSubscriptionPoolsDisplayContract=false;
		for (SubscriptionPool pool : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
			String none = "None";
			if (pool.contract!=null) {
				Assert.assertTrue(!pool.contract.equalsIgnoreCase(none), "The contract '"+pool.contract+"' for subscription pool '"+pool.poolId+"' should not be reported as '"+none+"'.");
				availableSubscriptionPoolsDisplayContract = true;
			}
		}
		Assert.assertTrue(availableSubscriptionPoolsDisplayContract, "Successfully encountered contracts reported in the list of available subscription pools.");
	}
	
	
	@Test(	description="subscription-manager-cli: RHEL Personal should be the only available subscription to a consumer registered as type person",
			groups={"Tier2Tests","EnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test"},
			enabled=false)	// registered consumers type of "person" was originally intended for entitling people to training.  Red Hat Learning Management systems never made use if it, and candlepin has no active requirements for it.  Disabling the personal tests...  Reference https://bugzilla.redhat.com/show_bug.cgi?id=967160#c1
	//@ImplementsNitrateTest(caseId=)
	public void EnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test() throws JSONException {
//		String rhelPersonalProductId = getProperty("sm.rhpersonal.productId", "");
//		if (rhelPersonalProductId.equals("")) throw new SkipException("This testcase requires specification of a RHPERSONAL_PRODUCTID.");
		
		// decide what username and password to test with
		String username = sm_clientUsername;
		String password = sm_clientPassword;
		String owner = sm_clientOrg;
		if (!sm_rhpersonalUsername.equals("")) {
			username = sm_rhpersonalUsername;
			password = sm_rhpersonalPassword;
			owner = sm_rhpersonalOrg;
		}
		
		// register a person
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(username, password, owner, null, ConsumerType.person, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		// assert that subscriptions with personal productIds are available to this person consumer
		List<SubscriptionPool> subscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		for (String personProductId : getPersonProductIds()) {
			SubscriptionPool rhelPersonalPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", personProductId, subscriptionPools);
			Assert.assertNotNull(rhelPersonalPool,"Personal ProductId '"+personProductId+"' is available to this consumer registered as type person");
		}
	
		// assert that personal subscriptions are the only available pools to this person consumer
		for (SubscriptionPool subscriptionPool : subscriptionPools) {
			Assert.assertTrue(getPersonProductIds().contains(subscriptionPool.productId), "This available ProductId '"+subscriptionPool.productId+"' available to the registered person is among the expected list of personal products that we expect to be consumable by this person.");
		}
	}
	@AfterGroups(groups={}, value="EnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test", alwaysRun=true)
	public void teardownAfterEnsureOnlyRHELPersonalIsAvailableToRegisteredPerson_Test() {
		if (clienttasks!=null) clienttasks.unregister_(null, null, null, null);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37703", "RHEL7-51348"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: RHEL Personal should not be an available subscription to a consumer registered as type system",
			groups={"Tier2Tests","EnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRHELPersonalIsNotAvailableToRegisteredSystem() throws JSONException {
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, ConsumerType.system, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		SubscriptionPool rhelPersonalPool = null;
		
		for (String personProductId : getPersonProductIds()) {
			// assert that RHEL Personal *is not* included in --available subscription pools
			rhelPersonalPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", personProductId, clienttasks.getCurrentlyAvailableSubscriptionPools());
			Assert.assertNull(rhelPersonalPool,"Personal ProductId '"+personProductId+"' is NOT available to this consumer from --available subscription pools when registered as type system.");
			
			/* behavior changed on list --all --available  (3/4/2011)
			// also assert that RHEL Personal *is* included in --all --available subscription pools
			rhelPersonalPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", personProductId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
			Assert.assertNotNull(rhelPersonalPool,"Personal ProductId '"+personProductId+"' is included in --all --available subscription pools when registered as type system.");
			*/
			// also assert that RHEL Personal *is not* included in --all --available subscription pools
			rhelPersonalPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", personProductId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
			Assert.assertNull(rhelPersonalPool,"Personal ProductId '"+personProductId+"' is NOT available to this consumer from --all --available subscription pools when registered as type system.");
		}
	}
	@AfterGroups(groups={}, value="EnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test", alwaysRun=true)
	public void teardownAfterEnsureRHELPersonalIsNotAvailableToRegisteredSystem_Test() {
		if (clienttasks!=null) clienttasks.unregister_(null, null, null, null);
	}



	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36548", "RHEL7-51324"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list consumed should be permitted without being registered",
			groups={"Tier2Tests","blockedByBug-725870"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testAttemptListConsumedWithoutBeingRegistered() {
		
		clienttasks.unregister(null,null,null, null);
		SSHCommandResult listResult = clienttasks.listConsumedProductSubscriptions();
		
		// assert redemption results
		String expectedMsg = "No consumed subscription pools to list";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.20.2-1")) {	// commit da72dfcbbb2c3a44393edb9e46e1583d05cc140a
			expectedMsg="No consumed subscription pools were found.";
		}
		Assert.assertEquals(listResult.getStdout().trim(), expectedMsg, "List consumed should NOT require that the system be registered.");
		Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(0),"Exit code from list consumed when executed without being registered.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36549", "RHEL7-51325"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list installed should be permitted without being registered",
			groups={"Tier2Tests","blockedByBug-725870"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testAttemptListInstalledWithoutBeingRegistered() {
		
		clienttasks.unregister(null,null,null, null);
		SSHCommandResult listResult = clienttasks.listInstalledProducts();
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36564", "RHEL7-51346"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list should be permitted without being registered",
			groups={"Tier2Tests","blockedByBug-725870"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testAttemptListWithoutBeingRegistered() {
		
		clienttasks.unregister(null,null,null, null);
		SSHCommandResult listResult = clienttasks.list_(null,null,null,null,null,null,null, null, null, null, null, null, null, null, null);
		
		Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(0), "The exit code from the list command indicates a success.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37699", "RHEL7-51323"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list available should require being registered",
			groups={"Tier2Tests",},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testAttemptListAvailableWithoutBeingRegistered() {
		SSHCommandResult listResult;
		clienttasks.unregister(null,null,null, null);
		
		listResult = clienttasks.list_(null,true,null,null,null,null,null, null, null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(1), "The exit code from the list --available command indicates a problem.");
			Assert.assertEquals(listResult.getStderr().trim(), clienttasks.msg_ConsumerNotRegistered,"stderr while attempting to list --available subscriptions should indicate registration is required");
			Assert.assertEquals(listResult.getStdout().trim(), "","stdout while attempting to list --available subscriptions when not registered");
		} else {
			//Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(1), "The exit code from the list available command indicates a problem.");
			//Assert.assertEquals(listResult.getStdout().trim(), "Error: You need to register this system by running `register` command before using this option.","Attempting to list available subscriptions should require registration.");
			// results changed after bug fix 749332
			Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(255), "The exit code from the list available command indicates a problem.");
			Assert.assertEquals(listResult.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered,"Attempting to list --available subscriptions should require registration.");
		}
		
		listResult = clienttasks.list_(true,true,null,null,null,null,null, null, null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(1), "The exit code from the list --all --available command indicates a problem.");
			Assert.assertEquals(listResult.getStderr().trim(), clienttasks.msg_ConsumerNotRegistered,"stderr while attempting to list --all --available subscriptions should indicate registration is required");
			Assert.assertEquals(listResult.getStdout().trim(), "","stdout while attempting to list --all --available subscriptions when not registered");
		} else {
			//Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(1), "The exit code from the list all available command indicates a problem.");
			//Assert.assertEquals(listResult.getStdout().trim(), "Error: You need to register this system by running `register` command before using this option.","Attempting to list all available subscriptions should require registration.");
			// results changed after bug fix 749332
			Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(255), "The exit code from the list all available command indicates a problem.");
			Assert.assertEquals(listResult.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered,"Attempting to list --all --available subscriptions should require registration.");
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36552", "RHEL7-51329"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list future subscription pools for a system",
			groups={"Tier2Tests","blockedByBug-672562","blockedByBug-1493711"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListAllAvailableWithFutureOnDate() throws Exception {
		
		List<List<Object>> allFutureJSONPoolsDataAsListOfLists = getAllFutureJSONPoolsDataAsListOfLists(ConsumerType.system);
		if (allFutureJSONPoolsDataAsListOfLists.size()<1) throw new SkipException("Cannot find any future subscriptions to test");
		
		Calendar now = new GregorianCalendar();
		now.setTimeInMillis(System.currentTimeMillis());
		List<String> onDatesTested = new ArrayList<String>();
		
		
		// assemble a list of ondate strings to test
		List<String> onDatesToTest = new ArrayList<String>();
		for (List<Object> l : allFutureJSONPoolsDataAsListOfLists) {
			JSONObject futureJSONPool = (JSONObject) l.get(0);
			
			String startDate = futureJSONPool.getString("startDate");
			
			// add one day to this start date to use for subscription-manager list --ondate test (ASSUMPTION: these subscriptions last longer than one day)
			Calendar onDate = parseISO8601DateString(startDate,"GMT"); onDate.add(Calendar.DATE, 1);
			DateFormat yyyy_MM_dd_DateFormat = new SimpleDateFormat("yyyy-MM-dd");
			String onDateToTest = yyyy_MM_dd_DateFormat.format(onDate.getTime());
			
			if (!onDatesToTest.contains(onDateToTest)) onDatesToTest.add(onDateToTest);
		}
		
		// assemble a list of future subscription poolIds to verify in the subscription-manager list --available --ondate
		List<String> futurePoolIds = new ArrayList<String>();
		for (List<Object> l : allFutureJSONPoolsDataAsListOfLists) {
			JSONObject jsonPool = (JSONObject) l.get(0);
			String id = jsonPool.getString("id");
			futurePoolIds.add(id);
		}
		
		// use this list to store all of the poolIds found on a future date listing
		List<String>futurePoolIdsListedOnDate = new ArrayList<String>();
		
		for (List<Object> l : allFutureJSONPoolsDataAsListOfLists) {
			JSONObject futureJSONPool = (JSONObject) l.get(0);
			
			// add one day to this start date to use for subscription-manager list --ondate test
			Calendar onDate = parseISO8601DateString(futureJSONPool.getString("startDate"),"GMT"); onDate.add(Calendar.DATE, 1);
			DateFormat yyyy_MM_dd_DateFormat = new SimpleDateFormat("yyyy-MM-dd");
			String onDateToTest = yyyy_MM_dd_DateFormat.format(onDate.getTime());
			
			// if we already tested with this ondate string, then continue
			if (onDatesTested.contains(onDateToTest)) continue;
			
			// list all available onDateToTest
			SSHCommandResult listResult = clienttasks.list_(true,true,null,null,null,onDateToTest,null, null, null, null, null, null, null, null, null);
			Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(0), "The exit code from the list --all --available --ondate command indicates a success.");

			List<SubscriptionPool> subscriptionPools = SubscriptionPool.parse(listResult.getStdout());
			Assert.assertTrue(subscriptionPools.size()>=1,"A list of SubscriptionPools was returned from the list module using a valid ondate option.");
			
			// assert that each of the SubscriptionPools listed is indeed active on the requested date
			for (SubscriptionPool subscriptionPool : subscriptionPools) {
				JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+subscriptionPool.poolId));
				Calendar startDate = parseISO8601DateString(jsonPool.getString("startDate"),"GMT");	// "startDate":"2012-02-08T00:00:00.000+0000"
				Calendar endDate = parseISO8601DateString(jsonPool.getString("endDate"),"GMT");	// "endDate":"2013-02-07T00:00:00.000+0000"
				Boolean activeSubscription = jsonPool.getBoolean("activeSubscription");	// TODO I don't yet understand how to test this property.  I'm assuming it is true
				Assert.assertTrue(startDate.before(onDate)&&endDate.after(onDate)&&activeSubscription,"SubscriptionPool '"+subscriptionPool.poolId+"' is indeed active and listed as available ondate='"+onDateToTest+"'.");
							
				// for follow-up assertions keep a list of all the futurePoolIds that are found on the listing date (excluding pools that are active now since they are not considered future pools)
				if (startDate.after(now)) {
					if (!futurePoolIdsListedOnDate.contains(subscriptionPool.poolId)) futurePoolIdsListedOnDate.add(subscriptionPool.poolId);
				}
			}
			
			// remember that this date was just tested
			onDatesTested.add(onDateToTest);
		}
		
		Assert.assertEquals(futurePoolIdsListedOnDate.size(), futurePoolIds.size(),"The expected count of all of the expected future subscription pools for systems was listed on future dates.");
		for (String futurePoolId : futurePoolIds) futurePoolIdsListedOnDate.remove(futurePoolId);
		Assert.assertTrue(futurePoolIdsListedOnDate.isEmpty(),"All of the expected future subscription pools for systems were listed on future dates.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"", ""},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list all subscription pools available after a specified date",
			groups={"Tier2Tests","blockedByBug-1479353"},
			dataProvider="getAfterDateData",
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListAllAvailableAfterDate(Object bugzilla, String yyyy_MM_dd) throws Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.21.2-1")) throw new SkipException("Installed package '"+clienttasks.installedPackageVersionMap.get("subscription-manager")+"' does not support the subscription-manager list --after feature which was introduced with RFE Bug https://bugzilla.redhat.com/show_bug.cgi?id=1479353");
		
		Calendar afterDate = parseDateStringUsingDatePattern(yyyy_MM_dd, "yyyy-MM-dd", null);

		// list all available after date yyyy_MM_dd
		SSHCommandResult listResult = clienttasks.list(true,true,null,null,null,null,yyyy_MM_dd, null, null, null, null, null, null, null, null);
		
		// parse the subscription pools from the result of subscription-manager list --all --after=yyyy_MM_dd
		List<SubscriptionPool> subscriptionPools = SubscriptionPool.parse(listResult.getStdout());

		// assert that each of the subscription pools listed does indeed start after yyyy_MM_dd
		for (SubscriptionPool subscriptionPool : subscriptionPools) {
			//JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+subscriptionPool.poolId));
			//Boolean activeSubscription = jsonPool.getBoolean("activeSubscription");	// TODO I don't yet understand how to test this property.  I'm assuming it is true

			Assert.assertTrue(subscriptionPool.startDate.after(afterDate), "The start date is after the requested list date '"+yyyy_MM_dd+"' for all available subscription pool: "+subscriptionPool);
		}
		
		// if the returned list of pools was empty, assert that all future pools start before yyyy_MM_dd
		if (subscriptionPools.isEmpty()) {
			for (List<Object> l : getAllFutureJSONPoolsDataAsListOfLists(ConsumerType.system)) {
				JSONObject futureJSONPool = (JSONObject) l.get(0);
				Calendar startDate = parseISO8601DateString(futureJSONPool.getString("startDate"),"GMT");	// "startDate":"2012-02-08T00:00:00.000+0000"
				Assert.assertTrue(startDate.before(afterDate), "Future available pool productId '"+futureJSONPool.getString("productId")+"' poolId '"+futureJSONPool.getString("id")+"' starts '"+futureJSONPool.getString("startDate")+"' before the list --after="+yyyy_MM_dd+" date.");
			}
		}
		
		// TODO: Should also include some assertion for the --all option.
	}
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"", ""},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list subscription pools available after a specified date",
			groups={"Tier2Tests","blockedByBug-1479353"},
			dataProvider="getAfterDateData",
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListAvailableAfterDate(Object bugzilla, String yyyy_MM_dd) throws Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.21.2-1")) throw new SkipException("Installed package '"+clienttasks.installedPackageVersionMap.get("subscription-manager")+"' does not support the subscription-manager list --after feature which was introduced with RFE Bug https://bugzilla.redhat.com/show_bug.cgi?id=1479353");
		
		Calendar afterDate = parseDateStringUsingDatePattern(yyyy_MM_dd, "yyyy-MM-dd", null);

		// list all available after date yyyy_MM_dd
		SSHCommandResult listResult = clienttasks.list(false,true,null,null,null,null,yyyy_MM_dd, null, null, null, null, null, null, null, null);
		
		// parse the subscription pools from the result of subscription-manager list --all --after=yyyy_MM_dd
		List<SubscriptionPool> subscriptionPools = SubscriptionPool.parse(listResult.getStdout());

		// assert that each of the subscription pools listed does indeed start after yyyy_MM_dd
		for (SubscriptionPool subscriptionPool : subscriptionPools) {
			//JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+subscriptionPool.poolId));
			//Boolean activeSubscription = jsonPool.getBoolean("activeSubscription");	// TODO I don't yet understand how to test this property.  I'm assuming it is true

			Assert.assertTrue(subscriptionPool.startDate.after(afterDate), "The start date is after the requested list date '"+yyyy_MM_dd+"' for all available subscription pool: "+subscriptionPool);
		}
		
		// if the returned list of pools was empty, assert that all future pools start before yyyy_MM_dd
		if (subscriptionPools.isEmpty()) {
			for (List<Object> l : getAllFutureJSONPoolsDataAsListOfLists(ConsumerType.system)) {
				JSONObject futureJSONPool = (JSONObject) l.get(0);
				Calendar startDate = parseISO8601DateString(futureJSONPool.getString("startDate"),"GMT");	// "startDate":"2012-02-08T00:00:00.000+0000"
				Assert.assertTrue(startDate.before(afterDate), "Future available pool productId '"+futureJSONPool.getString("productId")+"' poolId '"+futureJSONPool.getString("id")+"' starts '"+futureJSONPool.getString("startDate")+"' before the list --after="+yyyy_MM_dd+" date.");
			}
		}
	}
	@DataProvider(name="getAfterDateData")
	public Object[][] getAfterDateDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAfterDateDataAsListOfLists());
	}
	protected List<List<Object>>getAfterDateDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		
		DateFormat yyyy_MM_dd_DateFormat = new SimpleDateFormat("yyyy-MM-dd");
	//	String today = yyyy_MM_dd_DateFormat.format(Calendar.getInstance().getTime());
		
	//	Calendar today = Calendar.getInstance();
		Calendar jan1ThisYear = Calendar.getInstance();
		jan1ThisYear.set(Calendar.MONTH, Calendar.JANUARY);
		jan1ThisYear.set(Calendar.DAY_OF_MONTH, 1);
		Calendar jan1NextYear = Calendar.getInstance();
		jan1NextYear.set(Calendar.MONTH, Calendar.JANUARY);
		jan1NextYear.set(Calendar.DAY_OF_MONTH, 1);
		jan1NextYear.add(Calendar.YEAR, 1);
		Calendar jan1TwoYears = Calendar.getInstance();
		jan1TwoYears.set(Calendar.MONTH, Calendar.JANUARY);
		jan1TwoYears.set(Calendar.DAY_OF_MONTH, 1);
		jan1TwoYears.add(Calendar.YEAR, 2);
		Calendar nextWeek = Calendar.getInstance();
		nextWeek.add(Calendar.DATE, 7);
		
		// Object bugzilla, Calendar date
		ll.add(Arrays.asList(new Object[] {null, yyyy_MM_dd_DateFormat.format(jan1ThisYear.getTime())}));
		ll.add(Arrays.asList(new Object[] {null, yyyy_MM_dd_DateFormat.format(nextWeek.getTime())}));
		ll.add(Arrays.asList(new Object[] {null, yyyy_MM_dd_DateFormat.format(jan1NextYear.getTime())}));
		ll.add(Arrays.asList(new Object[] {null, yyyy_MM_dd_DateFormat.format(jan1TwoYears.getTime())}));

		
		return ll;
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36554", "RHEL7-51332"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list with --match-installed option",
			groups={"Tier2Tests","blockedByBug-654501"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListAvailableWithMatchInstalled() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.10.3-1")) throw new SkipException("Installed package '"+clienttasks.installedPackageVersionMap.get("subscription-manager")+"' is blockedByBug https://bugzilla.redhat.com/show_bug.cgi?id=654501 which is fixed in subscription-manager-1.10.3-1.");
		
 		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, false, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null);

		// assemble a list of currently installed product ids
		List<ProductCert> installedProductCerts = clienttasks.getCurrentProductCerts();
		List<String> installedProductIds = new ArrayList<String>(); for (ProductCert productCert : installedProductCerts) installedProductIds.add(productCert.productId);
		
		// get the available subscription pools
		List<SubscriptionPool> availableSubscriptionPools = SubscriptionPool.parse(clienttasks.list(null, true, null, null, null, null, null, false, null, null, null, null, null, null, null).getStdout());
		List<SubscriptionPool> availableSubscriptionPoolsMatchingInstalled = SubscriptionPool.parse(clienttasks.list(null, true, null, null, null, null, null, true, null, null, null, null, null, null, null).getStdout());
		
		// loop through the list of available subscription pools with match-installed and assert they really do provide at least one product that is installed.
		for (SubscriptionPool subscriptionPool : availableSubscriptionPoolsMatchingInstalled) {
			ProductCert matchedInstalledProductCert = null;
			for (String providedProductId : CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, subscriptionPool.poolId)) {
				matchedInstalledProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", providedProductId, installedProductCerts);
				if (matchedInstalledProductCert!=null) break;
			}
			if (matchedInstalledProductCert!=null) Assert.assertTrue(matchedInstalledProductCert!=null,"Available subscription pool '"+subscriptionPool.subscriptionName+"' provides="+subscriptionPool.provides+" includes product id '"+matchedInstalledProductCert.productId+"' which was found among the product ids of the currently installed product certs. "+installedProductIds);
			else Assert.fail("Subscription-manager list available with match-installed option erroneously reported SubscriptionPool '"+subscriptionPool.subscriptionName+"' provides="+subscriptionPool.provides+" which does NOT provide a product whose id was found among the currently installed product certs. "+installedProductIds);
		}
		
		// loop through the list of available subscription without match-installed and make sure those that provide an installed product id are included in the filtered list
		for (SubscriptionPool subscriptionPool : availableSubscriptionPools) {
			boolean providesAnInstalledProductId = false;
			for (String providedProductId : CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, subscriptionPool.poolId)) {
				if (installedProductIds.contains(providedProductId)) providesAnInstalledProductId = true;
			}
			if (providesAnInstalledProductId) Assert.assertTrue(availableSubscriptionPoolsMatchingInstalled.contains(subscriptionPool),"The list of available subscriptions with match-installed option includes '"+subscriptionPool.subscriptionName+"' provides="+subscriptionPool.provides);
			else Assert.assertTrue(!availableSubscriptionPoolsMatchingInstalled.contains(subscriptionPool),"The list of available subscriptions with match-installed option does NOT include '"+subscriptionPool.subscriptionName+"' provides="+subscriptionPool.provides);
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36553", "RHEL7-51330"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list all with --match-installed option",
			groups={"Tier2Tests","blockedByBug-654501","blockedByBug-1493711"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListAllAvailableWithMatchInstalled() throws JSONException, Exception {
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, false, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null);

		// assemble a list of currently installed product ids
		List<ProductCert> installedProductCerts = clienttasks.getCurrentProductCerts();
		List<String> installedProductIds = new ArrayList<String>(); for (ProductCert productCert : installedProductCerts) installedProductIds.add(productCert.productId);
		
		// get all the available subscription pools
		List<SubscriptionPool> allAvailableSubscriptionPools = SubscriptionPool.parse(clienttasks.list(true, true, null, null, null, null, null, false, null, null, null, null, null, null, null).getStdout());
		List<SubscriptionPool> allAvailableSubscriptionPoolsMatchingInstalled = SubscriptionPool.parse(clienttasks.list(true, true, null, null, null, null, null, true, null, null, null, null, null, null, null).getStdout());
		
		// loop through the list of all available subscription pools with match-installed and assert they really do provide at least one product that is installed.
		for (SubscriptionPool subscriptionPool : allAvailableSubscriptionPoolsMatchingInstalled) {
			ProductCert matchedInstalledProductCert = null;
			for (String providedProductId : CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, subscriptionPool.poolId)) {
				matchedInstalledProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", providedProductId, installedProductCerts);
				if (matchedInstalledProductCert!=null) break;
			}
			if (matchedInstalledProductCert!=null) Assert.assertTrue(matchedInstalledProductCert!=null,"Available subscription pool '"+subscriptionPool.subscriptionName+"' provides="+subscriptionPool.provides+" includes product id '"+matchedInstalledProductCert.productId+"' which was found among the product ids of the currently installed product certs. "+installedProductIds);
			else Assert.fail("Subscription-manager list all available with match-installed option erroneously reported SubscriptionPool '"+subscriptionPool.subscriptionName+"' provides="+subscriptionPool.provides+" which does NOT provide a product whose id was found among the currently installed product certs. "+installedProductIds);
		}
		
		// loop through the list of all available subscription without match-installed and make sure those that provide an installed product id are included in the filtered list
		for (SubscriptionPool subscriptionPool : allAvailableSubscriptionPools) {
			boolean providesAnInstalledProductId = false;
			for (String providedProductId : CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, subscriptionPool.poolId)) {
				if (installedProductIds.contains(providedProductId)) providesAnInstalledProductId = true;
			}
			if (providesAnInstalledProductId) Assert.assertTrue(allAvailableSubscriptionPoolsMatchingInstalled.contains(subscriptionPool),"The list of all available subscriptions with match-installed option includes '"+subscriptionPool.subscriptionName+"' provides="+subscriptionPool.provides);
			else Assert.assertTrue(!allAvailableSubscriptionPoolsMatchingInstalled.contains(subscriptionPool),"The list of all available subscriptions with match-installed option does NOT include '"+subscriptionPool.subscriptionName+"' provides="+subscriptionPool.provides);
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36565", "RHEL7-51349"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list all with --match-installed option",
			groups={"Tier2Tests","blockedByBug-654501","blockedByBug-1022622"/*rhel7*/,"blockedByBug-1114717"/*rhel6*/,"blockedByBug-1493711"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListAvailableWithNoOverlap() throws JSONException, Exception {
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, false, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null);
		
		// assemble a list of currently installed product ids
		List<ProductCert> installedProductCerts = clienttasks.getCurrentProductCerts();
		List<String> installedProductIds = new ArrayList<String>(); for (ProductCert productCert : installedProductCerts) installedProductIds.add(productCert.productId);
		
		// get the available subscription pools
		List<SubscriptionPool> availableSubscriptionPools = SubscriptionPool.parse(clienttasks.list(null, true, null, null, null, null, null, null, false, null, null, null, null, null, null).getStdout());
		
		// randomly attach a positive subset of available subscriptions
		List<SubscriptionPool> randomAvailableSubscriptionPools = getRandomSubsetOfList(availableSubscriptionPools, randomGenerator.nextInt(availableSubscriptionPools.size()-1)+1);
		List<String> poolIds = new ArrayList<String>(); for (SubscriptionPool subscriptionPool : randomAvailableSubscriptionPools) poolIds.add(subscriptionPool.poolId);
		if (false) {	// debugTesting will cause test to fail due to bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=1022622#c0
			poolIds.clear();
			poolIds.add(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName","Awesome OS Server Basic",availableSubscriptionPools).poolId);
			poolIds.add(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName","Awesome OS for All Arch (excpt for x86_64 content)",availableSubscriptionPools).poolId);
			poolIds.add(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName","Awesome OS with unlimited virtual guests",availableSubscriptionPools).poolId);
			poolIds.add(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName","Multi-Attribute Stackable (2 GB, 2 Cores)",availableSubscriptionPools).poolId);
			poolIds.add(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName","Awesome OS Server Basic (data center)",availableSubscriptionPools).poolId);
			poolIds.add(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName","Multi-Attribute Stackable (2 sockets)",availableSubscriptionPools).poolId);
			poolIds.add(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName","Awesome OS for systems with no sockets",availableSubscriptionPools).poolId);
			poolIds.add(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName","Awesome OS Server Basic (multi-entitlement)",availableSubscriptionPools).poolId);
			poolIds.add(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionName","Awesome OS Modifier",availableSubscriptionPools).poolId);
		}
		clienttasks.subscribe(null, null, poolIds, null, null, "1", null, null, null, null, null, null, null);
		
		List<InstalledProduct> installedProducts = InstalledProduct.parse(clienttasks.list(null,null,null,true,null,null,null,null,null,null,null, null, null, null, null).getStdout());
		List<SubscriptionPool> availableSubscriptionPoolsWithoutOverlap = SubscriptionPool.parse(clienttasks.list(null, true, null, null, null, null, null, null, true, null, null, null, null, null, null).getStdout());
		//	[root@jsefler-7 ~]# subscription-manager list --help | grep no-overlap -A1
		//	  --no-overlap          shows pools which provide products that are not
		//	                        already covered; only used with --available
		
		// loop through the list of available subscription pools without overlap and assert that at least one of pool's provided products is not fully subscribed.
		for (SubscriptionPool subscriptionPool : availableSubscriptionPoolsWithoutOverlap) {
			boolean noOverlapFound = false;
			List<String> providedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, subscriptionPool.poolId);
			if (providedProductIds.isEmpty()) {
				Assert.assertTrue(providedProductIds.isEmpty(),"Subscription '"+subscriptionPool.subscriptionName+"' provides="+subscriptionPool.provides+" reported in the list available with no-overlap provides no products and therefore does not overlap an already covered product.");
				noOverlapFound = true;
			}
			for (String providedProductId : providedProductIds) {
				InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", providedProductId, installedProducts);
				if (installedProduct!=null) {
					if (!installedProduct.status.equalsIgnoreCase("Subscribed")) {
						Assert.assertTrue(!installedProduct.status.equalsIgnoreCase("Subscribed"),"Subscription '"+subscriptionPool.subscriptionName+"' provides="+subscriptionPool.provides+" reported in the list available with no-overlap provides product id '"+providedProductId+"' which is installed with status '"+installedProduct.status+"' and therefore does not overlap an already covered product.");
						noOverlapFound = true;
					} else {
						log.warning("Subscription '"+subscriptionPool.subscriptionName+"' provides="+subscriptionPool.provides+" reported in the list available with no-overlap provides product id '"+providedProductId+"' which is installed with status '"+installedProduct.status+"'.");
					}
				} else {
					Assert.assertTrue(installedProduct==null,"Subscription '"+subscriptionPool.subscriptionName+"' provides="+subscriptionPool.provides+" reported in the list available with no-overlap provides product id '"+providedProductId+"' which is not installed and therefore does not overlap an already covered product.");
					noOverlapFound = true;
				}
			}
			Assert.assertTrue(noOverlapFound,"Subscription '"+subscriptionPool.subscriptionName+"' provides="+subscriptionPool.provides+" that is reported in the list available with no-overlap provides at least one product that is not fully Subscribed.");
		}
		
		// the remainder of this --no-overlap test has been fixed in subscription-manager-1.12.6-1 and newer; otherwise skip it.
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.12.6-1")) throw new SkipException("The installed version of subscription-manager does not contain the fix for https://bugzilla.redhat.com/show_bug.cgi?id=1022622#c3");
		
		// assert that availableSubscriptionPools that are not filtered out of the availableSubscriptionPoolsWithoutOverlap provide products that are all fully Subscribed
		availableSubscriptionPools = SubscriptionPool.parse(clienttasks.list(null, true, null, null, null, null, null, null, false, null, null, null, null, null, null).getStdout());
		for (SubscriptionPool availableSubscriptionPool : availableSubscriptionPools) {
			if (!availableSubscriptionPoolsWithoutOverlap.contains(availableSubscriptionPool)) {
				for (String providedProductId : CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, availableSubscriptionPool.poolId)) {
					InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", providedProductId, installedProducts);
					if (installedProduct!=null) {
						Assert.assertEquals(installedProduct.status, "Subscribed", "Subscription '"+availableSubscriptionPool.subscriptionName+"' provides="+availableSubscriptionPool.provides+" is excluded from the list available with no-overlap.  It provides product id '"+providedProductId+"' which is installed and covered by an active subscription.");
					} else {
						// skip the providedProductId when not installed
					}
				}
			}
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27115", "RHEL7-51334"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list all available should filter by servicelevel when this option is passed.",
			groups={"Tier2Tests","blockedByBug-800933","blockedByBug-800999"},
			dataProvider="getListAvailableWithServicelevelData",
			enabled=true)
			@ImplementsNitrateTest(caseId=157228)
	public void testListAvailableWithServicelevel(Object bugzilla, String servicelevel) throws Exception {
		SSHCommandResult listResult;
		List<SubscriptionPool> expectedSubscriptionPools, filteredSubscriptionPools;
				
		// list all available (without service level)
		listResult = clienttasks.list_(true,true,null,null,null,null,null,null,null, null, null, null, null, null, null);
		List<SubscriptionPool> allAvailableSubscriptionPools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		
		// determine the subset of expected pools with a case-insensitive matching servicelevel
		expectedSubscriptionPools = SubscriptionPool.findAllInstancesWithCaseInsensitiveMatchingFieldFromList("serviceLevel", servicelevel, allAvailableSubscriptionPools);

		// list all available filtered by servicelevel
		listResult = clienttasks.list_(true,true,null,null,servicelevel,null,null,null,null, null, null, null, null, null, null);
		Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(0), "The exit code from the list --all --available --servicelevel command indicates a success.");
		
		// assert results
		filteredSubscriptionPools = SubscriptionPool.parse(listResult.getStdout());
		Assert.assertTrue(filteredSubscriptionPools.containsAll(expectedSubscriptionPools),"The actual list of --all --available --servicelevel=\""+servicelevel+"\" SubscriptionPools contains all of the expected SubscriptionPools (the expected list contains only pools with ServiceLevel=\""+servicelevel+"\")");
		Assert.assertTrue(expectedSubscriptionPools.containsAll(filteredSubscriptionPools),"The expected list of SubscriptionPools contains all of the actual SubscriptionPools returned by list --all --available --servicelevel=\""+servicelevel+"\".");
		if (expectedSubscriptionPools.isEmpty()) {
			String expectedStdout = "No available subscription pools to list";
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.12-1")) expectedStdout = String.format("No available subscription pools were found matching the service level \"%s\".",servicelevel);	// commit 2884e33acb35ab4e336fe12dc23de7ab26cc0572	// Bug 1159348 - list --matched should show the filter string when warning about empty matches
			Assert.assertEquals(listResult.getStdout().trim(), expectedStdout, "Expected message when no subscription remain after list is filtered by --servicelevel=\""+servicelevel+"\".");
		}
				
		// list all available (without service level)
		listResult = clienttasks.list_(false,true,null,null,null,null,null,null,null, null, null, null, null, null, null);
		List<SubscriptionPool> availableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		
		// determine the subset of expected pools with a matching servicelevel
		expectedSubscriptionPools = SubscriptionPool.findAllInstancesWithCaseInsensitiveMatchingFieldFromList("serviceLevel", servicelevel, availableSubscriptionPools);
		
		// list available filtered by servicelevel
		listResult = clienttasks.list_(false,true,null,null,servicelevel,null,null,null,null, null, null, null, null, null, null);
		Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(0), "The exit code from the list --all --available --servicelevel command indicates a success.");
		
		// assert results
		filteredSubscriptionPools = SubscriptionPool.parse(listResult.getStdout());
		Assert.assertTrue(filteredSubscriptionPools.containsAll(expectedSubscriptionPools),"The actual list of --available --servicelevel=\""+servicelevel+"\" SubscriptionPools contains all of the expected SubscriptionPools (the expected list contains only pools with ServiceLevel=\""+servicelevel+"\")");
		Assert.assertTrue(expectedSubscriptionPools.containsAll(filteredSubscriptionPools),"The expected list of SubscriptionPools contains all of the actual SubscriptionPools returned by list --available --servicelevel=\""+servicelevel+"\".");
		if (expectedSubscriptionPools.isEmpty()) {
			String expectedStdout = "No available subscription pools to list";
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.12-1")) expectedStdout = String.format("No available subscription pools were found matching the service level \"%s\".",servicelevel);	// commit 2884e33acb35ab4e336fe12dc23de7ab26cc0572	// Bug 1159348 - list --matched should show the filter string when warning about empty matches
			Assert.assertEquals(listResult.getStdout().trim(), expectedStdout, "Expected message when no subscription remain after list is filtered by --servicelevel=\""+servicelevel+"\".");
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19964", "RHEL7-51331"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: subcription manager list --available with exact --matches on Subscription Name, Provided Product Name, Contract Number, SKU, Service Level, Provided Product ID.  Note: exact match means no wildcards and is case insensitive.",
			groups={"Tier1Tests","blockedByBug-1146125","blockedByBug-1493711"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListAvailableWithExactMatches() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.6-1")) throw new SkipException("The list --matches function was not implemented in this version of subscription-manager.");
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<", "0.9.33-1")) throw new SkipException("Candlepin support for list --available --matches function was not implemented in server version '"+servertasks.statusVersion+"'.");// candlepin commit e5b6c24f2322b79a7ea8bb1e8c85a8cb86733471
		
		String matchesString;
		List<SubscriptionPool> expectedPools,actualSubscriptionPoolMatches;
		Boolean all = getRandomBoolean();
		Boolean matchInstalled = getRandomBoolean();
		Boolean noOverlap = getRandomBoolean();
///*debugTesting*/ matchInstalled=false; all=false; noOverlap=false;
		log.info("Testing with all="+all);
		log.info("Testing with matchInstalled="+matchInstalled);
		log.info("Testing with noOverlap="+noOverlap);
		
		// register if necessary
		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
			clienttasks.autoheal(null, null, true, null, null, null, null);
		} else clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
		
		// get all the available subscription pools
		SSHCommandResult listResult = clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, null, null, null, null, null, null);
		if (matchInstalled && clienttasks.getCurrentlyInstalledProducts().isEmpty()) {
			String skipCase = "No available subscription pools to list";
			Assert.assertEquals(listResult.getStdout().trim(), skipCase);
			throw new SkipException("Skipping this test when: "+skipCase);
		}
		List<SubscriptionPool> availableSubscriptionPools = SubscriptionPool.parse(listResult.getStdout());
		
		// randomly choose an available pool
		SubscriptionPool randomAvailablePool = getRandomListItem(availableSubscriptionPools);
///*debugTesting*/ randomAvailablePool	= SubscriptionPool.findFirstInstanceWithCaseInsensitiveMatchingFieldFromList("productId", "RH0802940", availableSubscriptionPools);
///*debugTesting*/ randomAvailablePool	= SubscriptionPool.findFirstInstanceWithCaseInsensitiveMatchingFieldFromList("productId", "awesomeos-x86_64", availableSubscriptionPools);
///*debugTesting*/ randomAvailablePool	= SubscriptionPool.findFirstInstanceWithCaseInsensitiveMatchingFieldFromList("poolId", "8a99f9815582f734015585f9a7c952b9", availableSubscriptionPools);
///*debugTesting*/ randomAvailablePool	= SubscriptionPool.findFirstInstanceWithCaseInsensitiveMatchingFieldFromList("productId", "RH0844913", availableSubscriptionPools);	// useful against stage with Test 2: debugTesting matchesString="dotNET on RHEL (for RHEL Server)";
		log.info("Testing with randomAvailablePool="+randomAvailablePool);
		
		// Test 1: test exact --matches on Subscription Name:
		matchesString = randomAvailablePool.subscriptionName;
		actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
		// also test case insensitivity
		matchesString = randomizeCaseOfCharactersInString(matchesString);
		actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
		
		
		// Test 2: test exact --matches on Provides:
		if (!randomAvailablePool.provides.isEmpty()) {
			matchesString = getRandomListItem(randomAvailablePool.provides);
///*debugTesting*/ matchesString="Red Hat Beta";
///*debugTesting*/ matchesString="dotNET on RHEL (for RHEL Server)";	// useful to assert matches on derivedProvidedProducts (finding a match on a data center subscription: RH00001  Red Hat Enterprise Linux for Virtual Datacenters, Premium)
			actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
			// also test case insensitivity
			matchesString = randomizeCaseOfCharactersInString(matchesString);
			actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);

		} else log.warning("Skipping list --available --matches test on a Provides item since the provides list is empty on our random available subscription: "+randomAvailablePool);		
		
		
		// Test 3: test exact --matches on SKU:
		matchesString = randomAvailablePool.productId;
		actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
		// also test case insensitivity
		matchesString = randomizeCaseOfCharactersInString(matchesString);
		actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
		
		
		// Test 4: test exact --matches on Contract:
		if (randomAvailablePool.contract!=null && !randomAvailablePool.contract.isEmpty()) {
			matchesString = randomAvailablePool.contract;
			actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
			// also test case insensitivity
			matchesString = randomizeCaseOfCharactersInString(matchesString);
			actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
		} else log.warning("Skipping list --available --matches test on a Contract item since it is null on our random available subscription: "+randomAvailablePool);

		
		// Test 5: test exact --matches on Service Level:
		if (randomAvailablePool.serviceLevel!=null && !randomAvailablePool.serviceLevel.isEmpty()) {
			matchesString = randomAvailablePool.serviceLevel;
			actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
			// also test case insensitivity
			matchesString = randomizeCaseOfCharactersInString(matchesString);
			actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
		} else log.warning("Skipping list --available --matches test on a Service Level item since it is null on our random available subscription: "+randomAvailablePool);
		
		
		// Test 6: test exact --matches on Provided ProductId:
		if (!randomAvailablePool.provides.isEmpty()) {
			matchesString = getRandomListItem(CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, randomAvailablePool.poolId));
///*debugTesting*/ matchesString	= "180";
			actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
		} else log.warning("Skipping list --available --matches test on a Provides ProductId item since the provides list is empty on our random available subscription: "+randomAvailablePool);		
		
		// TEMPORARY WORKAROUND
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1354665";	// Bug 1354665 - subscription-manager list --available --matches="CONTENT" is not finding matches on valid Repo ID or Name
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping Test 7: test exact --matches on a Content Name provided by a Provided ProductId while bug "+bugId+" is open.");
			log.warning("Skipping Test 8: test exact --matches on a Content Label provided by a Provided ProductId while bug "+bugId+" is open.");
			return;
		}
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<", "2.0.4-1")) {	// candlepin commit db5801ea	https://bugzilla.redhat.com/show_bug.cgi?id=1354665#c1
			log.warning("Skipping Test 7: test exact --matches on a Content Name provided by a Provided ProductId since this was not implemented until candlepin-2.0.4-1");
			log.warning("Skipping Test 8: test exact --matches on a Content Label provided by a Provided ProductId since this was not implemented until candlepin-2.0.4-1");
			return;	
		}
		// END OF WORKAROUND
		
		// Test 7: test exact --matches on a Content Name provided by a Provided ProductId
		if (!randomAvailablePool.provides.isEmpty()) {
			String randomProvidedProductId = getRandomListItem(CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, randomAvailablePool.poolId));
			JSONArray jsonProductContents = CandlepinTasks.getPoolProvidedProductContent(sm_clientUsername, sm_clientPassword, sm_serverUrl, randomAvailablePool.poolId, randomProvidedProductId);
			if (jsonProductContents.length()>0) {
				JSONObject randomjsonProductContent = (JSONObject) jsonProductContents.get(randomGenerator.nextInt(jsonProductContents.length()));
				JSONObject jsonContent = randomjsonProductContent.getJSONObject("content");
				matchesString = jsonContent.getString("name");
///*debugTesting*/matchesString = "Red Hat Satellite Tools 6.1 (for RHEL Server for ARM Development Preview) (RPMs)";
				actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
				assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
			} else log.warning("Skipping list --available --matches test on a Provides ProductId Content Name since the random provided product id '"+randomProvidedProductId+"' content list is empty on our random available subscription: "+randomAvailablePool);
		} else log.warning("Skipping list --available --matches test on a Provides ProductId Content Name since the provides list is empty on our random available subscription: "+randomAvailablePool);
		
		
		// Test 8: test exact --matches on a Content Label provided by a Provided ProductId
		if (!randomAvailablePool.provides.isEmpty()) {
			String randomProvidedProductId = getRandomListItem(CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, randomAvailablePool.poolId));
			JSONArray jsonProductContents = CandlepinTasks.getPoolProvidedProductContent(sm_clientUsername, sm_clientPassword, sm_serverUrl, randomAvailablePool.poolId, randomProvidedProductId);
			if (jsonProductContents.length()>0) {
				JSONObject randomjsonProductContent = (JSONObject) jsonProductContents.get(randomGenerator.nextInt(jsonProductContents.length()));
				JSONObject jsonContent = randomjsonProductContent.getJSONObject("content");
				matchesString = jsonContent.getString("label");
				actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
				assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
			} else log.warning("Skipping list --available --matches test on a Provides ProductId Content Label since the random provided product id '"+randomProvidedProductId+"' content list is empty on our random available subscription: "+randomAvailablePool);
		} else log.warning("Skipping list --available --matches test on a Provides ProductId Content Label since the provides list is empty on our random available subscription: "+randomAvailablePool);

	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19965", "RHEL7-33095"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: subcription manager list --available with wildcard --matches on Subscription Name, Provided Product Name, Contract Number, SKU, Service Level, Provided Product ID.  Note: wildcard match means * matches zero or more char and ? matches one char and is case insensitive.",
			groups={"Tier1Tests","blockedByBug-1146125","blockedByBug-1301696","blockedByBug-1493711"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListAvailableWithWildcardMatches() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.6-1")) throw new SkipException("The list --matches function was not implemented in this version of subscription-manager.");
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<", "0.9.33-1")) throw new SkipException("Candlepin support for list --available --matches function was not implemented in server version '"+servertasks.statusVersion+"'.");// candlepin commit e5b6c24f2322b79a7ea8bb1e8c85a8cb86733471
		
		String matchesString;
		List<SubscriptionPool> expectedPools,actualSubscriptionPoolMatches;
		Boolean all = getRandomBoolean();
		Boolean matchInstalled = getRandomBoolean();
		Boolean noOverlap = getRandomBoolean();
///*debugTesting*/ all=true; noOverlap=true; matchInstalled=false;	//  Bug 1301696 - getting unexpected hits on TESTDATA from subscription-manager list --available --matches=*os*
///*debugTesting*/ all=true; noOverlap=false; matchInstalled=false;
		log.info("Testing with all="+all);
		log.info("Testing with matchInstalled="+matchInstalled);
		log.info("Testing with noOverlap="+noOverlap);
		
		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
			clienttasks.autoheal(null, null, true, null, null, null, null);
		} else clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
		
		// get all the available subscription pools
		SSHCommandResult listResult = clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, null, null, null, null, null, null);
		if (matchInstalled && clienttasks.getCurrentlyInstalledProducts().isEmpty()) {
			String skipCase = "No available subscription pools to list";
			Assert.assertEquals(listResult.getStdout().trim(), skipCase);
			throw new SkipException("Skipping this test when: "+skipCase);
		}
		List<SubscriptionPool> availableSubscriptionPools = SubscriptionPool.parse(listResult.getStdout());
		
		// randomly choose an available pool
		SubscriptionPool randomAvailablePool = getRandomListItem(availableSubscriptionPools);
///*debugTesting*/ randomAvailablePool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", "awesomeos-ostree", availableSubscriptionPools);	//  Bug 1301696 - getting unexpected hits on TESTDATA from subscription-manager list --available --matches=*os*
///*debugTesting*/ randomAvailablePool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", "RH0380468", availableSubscriptionPools);	//  Bug 1301696 - getting unexpected hits on TESTDATA from subscription-manager list --available --matches=*os*
		log.info("Testing with randomAvailablePool="+randomAvailablePool);
		
		//	+-------------------------------------------+
		//	Available Subscriptions
		//	+-------------------------------------------+
		//	Subscription Name: Awesome OS OSTree
		//	Provides:          Awesome OS OStree Bits
		//	SKU:               awesomeos-ostree
		//	Contract:          3
		//	Pool ID:           2c90af8b49435579014943591343172c
		//	Available:         10
		//	Suggested:         1
		//	Service Level:     
		//	Service Type:      
		//	Subscription Type: Standard
		//	Ends:              10/23/2015
		//	System Type:       Physical
		
		
		// Test 1: test wildcard --matches on Subscription Name:
		matchesString = randomAvailablePool.subscriptionName;
		matchesString = matchesString.replaceFirst("^\\S+\\s+","*");	// drop first word
		actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
		// also test case insensitivity
		matchesString = randomizeCaseOfCharactersInString(matchesString);
		matchesString = matchesString.replaceFirst("\\s+\\S+$","*");	// and drop last word
		actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
		
		
		// Test 2: test wildcard --matches on Provides:
		if (!randomAvailablePool.provides.isEmpty()) {
			matchesString = getRandomListItem(randomAvailablePool.provides);
			matchesString = matchesString.replaceFirst("\\s+\\S+$","*");	// drop last word
			actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
			// also test case insensitivity
			matchesString = randomizeCaseOfCharactersInString(matchesString);
			matchesString = matchesString.replaceFirst("^\\S+\\s+","*");	// and drop first word
			actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);

		} else log.warning("Skipping list --available --matches test on a Provides item since the provides list is empty on our random available subscription: "+randomAvailablePool);		
		
		// Test 3: test wildcard --matches on SKU:
		matchesString = randomAvailablePool.productId;
		matchesString = matchesString.replaceFirst("^.","?");	// drop first char
		actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
		// also test case insensitivity
		matchesString = randomizeCaseOfCharactersInString(matchesString);
		matchesString = matchesString.replaceFirst(".$","?");	// and drop last char
		actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
		
		
		// Test 4: test wildcard --matches on Contract:
		if (randomAvailablePool.contract!=null && !randomAvailablePool.contract.isEmpty()) {
			matchesString = randomAvailablePool.contract;
			matchesString = matchesString.replaceFirst(".$","?");	// drop the last char and replace it with a '?' wildcard
			actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
			// also test case insensitivity
			matchesString = randomizeCaseOfCharactersInString(matchesString);
			matchesString = matchesString.replaceFirst("^.","?");	// also drop the first char and replace it with a '?' wildcard
			actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
		} else log.warning("Skipping list --available --matches test on a Contract item since it is null or empty on our random available subscription: "+randomAvailablePool);

		
		// Test 5: test wildcard --matches on Service Level:
		if (randomAvailablePool.serviceLevel!=null && !randomAvailablePool.serviceLevel.isEmpty()) {
			matchesString = randomAvailablePool.serviceLevel;
			matchesString = matchesString.replaceFirst("^.","*");	// drop first char
			matchesString = matchesString.replaceFirst(".$","?");	// drop last char
			actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
			// also test case insensitivity
			matchesString = randomizeCaseOfCharactersInString(matchesString);
			actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
		} else log.warning("Skipping list --available --matches test on a Service Level item since it is null or empty on our random available subscription: "+randomAvailablePool);
		
		
		// Test 6: test wildcard --matches on Provided ProductId:
		if (!randomAvailablePool.provides.isEmpty()) {
			matchesString = getRandomListItem(CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, randomAvailablePool.poolId));
			int i = randomGenerator.nextInt(matchesString.length());
			matchesString = matchesString.replaceAll(String.valueOf(matchesString.charAt(i)), "?");
			actualSubscriptionPoolMatches = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListAvailableWithMatches(matchesString,actualSubscriptionPoolMatches,availableSubscriptionPools);
		} else log.warning("Skipping list --available --matches test on a Provides ProductId item since the provides list is empty on our random available subscription: "+randomAvailablePool);		
		
		// TODO Test 7: test exact --matches on a Content Name provided by a Provided ProductId
		// see ListAvailableWithExactMatches_Test()	
		
		// TODO Test 8: test exact --matches on a Content Label provided by a Provided ProductId
		// see ListAvailableWithExactMatches_Test()
	}
	protected void assertActualResultOfListAvailableWithMatches(String matchesString, List<SubscriptionPool> actualSubscriptionPoolsMatches, List<SubscriptionPool> availableSubscriptionPools) throws JSONException, Exception {
		// translate matchesString into a regexString
		String regexString = matchesString.toLowerCase().replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)"); // escape parentheses
		regexString = regexString.replaceAll("\\*", ".*");	// splat wildcards match any zero or more chars
		regexString = regexString.replaceAll("\\?", ".");	// question mark wildcards match any one char
		
		// search through the available availableSubscriptionPools for expected matches on the matchesString
		// <crog> --matches looks at the following: product name, product id, pool/sub contract number, pool order number, provided product id, provided product name, provided product content name, provided product content label
		// NOTE: exact matches means no wildcards and is case insensitive
		List<SubscriptionPool> expectedSubscriptionPoolMatches = new ArrayList<SubscriptionPool>();
		availableSubscriptionPoolsLoop: for (SubscriptionPool subscriptionPool : availableSubscriptionPools) {	// added label for availableSubscriptionPoolsLoop to make this method faster, otherwise it can be removed for more thoroughness
			
			// Test for match on Subscription Name:
			if (subscriptionPool.subscriptionName.toLowerCase().matches(regexString)) {
				log.info("Found a hit on matches '"+matchesString+"' against the available subscription '"+subscriptionPool.subscriptionName+"' SKU '"+subscriptionPool.productId+"'.  The match is in Subscription Name: "+subscriptionPool.subscriptionName);
				if (!expectedSubscriptionPoolMatches.contains(subscriptionPool)) {
					expectedSubscriptionPoolMatches.add(subscriptionPool);
					continue availableSubscriptionPoolsLoop;
				}
			}
			
			// Test for match on Provides:
			for (String providesName : subscriptionPool.provides) {
				if (providesName.toLowerCase().matches(regexString)) {
					log.info("Found a hit on matches '"+matchesString+"' against the available subscription '"+subscriptionPool.subscriptionName+"' SKU '"+subscriptionPool.productId+"'.  The match is in Provides: "+subscriptionPool.provides);
					if (!expectedSubscriptionPoolMatches.contains(subscriptionPool)) {
						expectedSubscriptionPoolMatches.add(subscriptionPool);
						continue availableSubscriptionPoolsLoop;
					}
				}
			}
			
			// Test for match on SKU:
			if (subscriptionPool.productId.toLowerCase().matches(regexString)) {
				log.info("Found a hit on matches '"+matchesString+"' against the available subscription '"+subscriptionPool.subscriptionName+"' SKU '"+subscriptionPool.productId+"'.  The match is in SKU: "+subscriptionPool.productId);
				if (!expectedSubscriptionPoolMatches.contains(subscriptionPool)) {
					expectedSubscriptionPoolMatches.add(subscriptionPool);
					continue availableSubscriptionPoolsLoop;
				}
			}
			
			// Test for match on Contract:
			if (subscriptionPool.contract.toLowerCase().matches(regexString)) {
				log.info("Found a hit on matches '"+matchesString+"' against the available subscription '"+subscriptionPool.subscriptionName+"' SKU '"+subscriptionPool.productId+"'.  The match is in Contract: "+subscriptionPool.contract);
				if (!expectedSubscriptionPoolMatches.contains(subscriptionPool)) {
					expectedSubscriptionPoolMatches.add(subscriptionPool);
					continue availableSubscriptionPoolsLoop;
				}
			}
			
			// Test for match on Service Level:
			if (subscriptionPool.serviceLevel.toLowerCase().matches(regexString)) {
				log.info("Found a hit on matches '"+matchesString+"' against the available subscription '"+subscriptionPool.subscriptionName+"' SKU '"+subscriptionPool.productId+"'.  The match is in Service Level: "+subscriptionPool.serviceLevel);
				if (!expectedSubscriptionPoolMatches.contains(subscriptionPool)) {
					expectedSubscriptionPoolMatches.add(subscriptionPool);
					continue availableSubscriptionPoolsLoop;
				}
			}
			
			// Test for match on Provided ProductId:
			for (String providedProductId : CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, subscriptionPool.poolId)) {
				if (providedProductId.toLowerCase().matches(regexString)) {
					log.info("Found a hit on matches '"+matchesString+"' against the available subscription '"+subscriptionPool.subscriptionName+"' SKU '"+subscriptionPool.productId+"'.  The match is in Provided Product ID: "+providedProductId);
					if (!expectedSubscriptionPoolMatches.contains(subscriptionPool)) {
						expectedSubscriptionPoolMatches.add(subscriptionPool);		
						continue availableSubscriptionPoolsLoop;
					}
				}
			}
/* 06/08/2017 TODO	NEEDS MORE TROUBLESHOOTING TO DETERMINE IF THIS IS A VALID ASSERT OR WHY IT SOMETIMES APPEARS GOOD AGAINST TESTDATA.
 * WHEN INCLUDED AGAINST STAGE CANDLEPIN, WAS FAILING ON ListAvailableWithExactMatches_Test Test 6 matchesString=180 WITH  randomAvailablePool=RH0844913 and matchInstalled=false; all=false; noOverlap=false;
			// Test for match on Derived Provided ProductId:
			// NOTE: list --available --matches is implemented server-side and appears to be searching the derivedProvidedProducts for product id matches.  Although unexpected, this feature has some benefit.
			// This behavior is in contrast to list --consumed --matches which is implemented client-side and does NOT search the derivedProvidedProducts for match on product id.
			// 10/31/2014 Verbal scrum discussion with devel decided to keep this behavior.
			for (String derivedProvidedProductId : CandlepinTasks.getPoolDerivedProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, subscriptionPool.poolId)) {
				if (derivedProvidedProductId.toLowerCase().matches(regexString)) {
					log.info("Found a hit on matches '"+matchesString+"' against the available subscription '"+subscriptionPool.subscriptionName+"' SKU '"+subscriptionPool.productId+"'.  The match is in Derived Provided Product ID: "+derivedProvidedProductId);
					if (!expectedSubscriptionPoolMatches.contains(subscriptionPool)) {
						expectedSubscriptionPoolMatches.add(subscriptionPool);		
						continue availableSubscriptionPoolsLoop;
					}
				}
			}
			
			// Test for match on Derived Provided ProductName:
			// NOTE: list --available --matches is implemented server-side and appears to be searching the derivedProvidedProducts for product name matches.  Although unexpected, this feature has some benefit.
			// This behavior is in contrast to list --consumed --matches which is implemented client-side and does NOT search the derivedProvidedProducts for match on product name.
			// 10/31/2014 Verbal scrum discussion with devel decided to keep this behavior.
			for (String derivedProvidedProductName : CandlepinTasks.getPoolDerivedProvidedProductNames(sm_clientUsername, sm_clientPassword, sm_serverUrl, subscriptionPool.poolId)) {
				if (derivedProvidedProductName.toLowerCase().matches(regexString)) {
					log.info("Found a hit on matches '"+matchesString+"' against the available subscription '"+subscriptionPool.subscriptionName+"' SKU '"+subscriptionPool.productId+"'.  The match is in Derived Provided Product Name: "+derivedProvidedProductName);
					if (!expectedSubscriptionPoolMatches.contains(subscriptionPool)) {
						expectedSubscriptionPoolMatches.add(subscriptionPool);		
						continue availableSubscriptionPoolsLoop;
					}
				}
			}
*/
			// Test for match on Provided ProductId Content Names and Labels:
			for (String providedProductId : CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, subscriptionPool.poolId)) {
				JSONArray jsonProductContents = CandlepinTasks.getPoolProvidedProductContent(sm_clientUsername, sm_clientPassword, sm_serverUrl, subscriptionPool.poolId, providedProductId);
				for (int j = 0; j < jsonProductContents.length(); j++) {
					JSONObject jsonProductContent = (JSONObject) jsonProductContents.get(j);
					JSONObject jsonContent = jsonProductContent.getJSONObject("content");
					boolean    enabled = jsonProductContent.getBoolean("enabled");
					String name = jsonContent.getString("name");
					String label = jsonContent.getString("label");
					
					// does the content name match the regexString
					if (name.toLowerCase().matches(regexString)) {
						log.info("Found a hit on matches '"+matchesString+"' against the available subscription '"+subscriptionPool.subscriptionName+"' SKU '"+subscriptionPool.productId+"'.  The match is in Provided Product ID '"+providedProductId+"' Content Name: "+name);
						if (!expectedSubscriptionPoolMatches.contains(subscriptionPool)) {
							expectedSubscriptionPoolMatches.add(subscriptionPool);		
							continue availableSubscriptionPoolsLoop;
						}
					}
					
					// does the content label match the regexString
					if (label.toLowerCase().matches(regexString)) {
						log.info("Found a hit on matches '"+matchesString+"' against the available subscription '"+subscriptionPool.subscriptionName+"' SKU '"+subscriptionPool.productId+"'.  The match is in Provided Product ID '"+providedProductId+"' Content Label: "+label);
						if (!expectedSubscriptionPoolMatches.contains(subscriptionPool)) {
							expectedSubscriptionPoolMatches.add(subscriptionPool);		
							continue availableSubscriptionPoolsLoop;
						}
					}
				}
			}
			
			// TODO May want to open an RFE to also search on these other available subscription fields
			// See https://bugzilla.redhat.com/show_bug.cgi?id=1146125#c7
			//	Pool ID:           2c90af8b49435579014943591343172c
			//	Available:         10
			//	Suggested:         1
			//	Service Type:      
			//	Subscription Type: Standard
			//	Ends:              10/23/2015
			//	System Type:       Physical
		}
		
		// assert that all of the expectedSubscriptionPoolMatches is identical to the actualSubscriptionPoolsMatches
		for (SubscriptionPool expectedSubscriptionPool : expectedSubscriptionPoolMatches) {
			if (!actualSubscriptionPoolsMatches.contains(expectedSubscriptionPool)) {
				log.warning("The actual list of subscription pools does NOT include this expected pool that matches '"+matchesString+"': "+expectedSubscriptionPool);
			}
		}
		for (SubscriptionPool actualSubscriptionPool : actualSubscriptionPoolsMatches) {
			if (!expectedSubscriptionPoolMatches.contains(actualSubscriptionPool)) {
				log.warning("The actual list of subscription pools includes this extraneous pool that should not match '"+matchesString+"': "+actualSubscriptionPool);
			}
		}
		Assert.assertTrue(expectedSubscriptionPoolMatches.containsAll(actualSubscriptionPoolsMatches)&&actualSubscriptionPoolsMatches.containsAll(expectedSubscriptionPoolMatches), "All of the expected available pools with an exact match (ignoring case) on '"+matchesString+"' were returned with the list --available --matches option.");

	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36556", "RHEL7-51335"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list --consumed with exact --matches on Subscription Name, Provided Product Name, Contract Number, SKU, Service Level, Provided Product ID.  Note: exact match means no wildcards and is case insensitive.",
			groups={"Tier2Tests","blockedByBug-1146125"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListConsumedWithExactMatches() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.6-1")) throw new SkipException("The list --matches function was not implemented in this version of subscription-manager.");
		
		String matchesString;
		List<ProductSubscription> actualProductSubscriptionMatches;

		// register if necessary
		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
			clienttasks.autoheal(null, null, true, null, null, null, null);
		} else clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
				
		// attach all the currently available subscriptions
		clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();
		
		// get all the consumed product subscriptions
		List<ProductSubscription> consumedProductSubscriptions = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, null, null, null, null, null, null).getStdout());
		
		// randomly choose one of the consumed Product Subscriptions
		ProductSubscription randomConsumedProductSubscription = getRandomListItem(consumedProductSubscriptions);
///*debugTesting*/randomConsumedProductSubscription=ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productId", "exempt-sla-product-sku", consumedProductSubscriptions);
		log.info("Testing with randomConsumedProductSubscription="+randomConsumedProductSubscription);
		
		//	+-------------------------------------------+
		//	   Consumed Subscriptions
		//	+-------------------------------------------+
		//	Subscription Name: Awesome OS for All Arch (excpt for x86_64 content)
		//	Provides:          Awesome OS for All Arch (excpt for x86_64 content) Bits
		//	SKU:               awesomeos-all-no-86_64-cont
		//	Contract:          3
		//	Account:           12331131231
		//	Serial:            2808682313592781316
		//	Pool ID:           2c90af8b494355790149435902da0ee8
		//	Active:            True
		//	Quantity Used:     1
		//	Service Level:     
		//	Service Type:      
		//	Status Details:    
		//	Subscription Type: Stackable
		//	Starts:            10/23/2014
		//	Ends:              10/23/2015
		//	System Type:       Physical
		
		
		// Test 1: test exact --matches on Subscription Name:
		matchesString = randomConsumedProductSubscription.productName;
		actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		// also test case insensitivity
		matchesString = randomizeCaseOfCharactersInString(matchesString);
		actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		
		
		// Test 2: test exact --matches on Provides:
		if (!randomConsumedProductSubscription.provides.isEmpty()) {
			matchesString = getRandomListItem(randomConsumedProductSubscription.provides);
			actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
			// also test case insensitivity
			matchesString = randomizeCaseOfCharactersInString(matchesString);
			actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);

		} else log.warning("Skipping list --consumed --matches test on a Provides item since the provides list is empty on our random consumed subscription: "+randomConsumedProductSubscription);		
		
		// Test 3: test exact --matches on SKU:
		matchesString = randomConsumedProductSubscription.productId;
		actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		// also test case insensitivity
		matchesString = randomizeCaseOfCharactersInString(matchesString);
		actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		
		
		// Test 4: test exact --matches on Contract:
		matchesString = String.valueOf(randomConsumedProductSubscription.contractNumber);
		actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		// also test case insensitivity
		/* not necessary since contractNumber is an Integer
		matchesString = randomizeCaseOfCharactersInString(matchesString);
		actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, matchesString, null, null, null).getStdout());
		assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		*/
		
		// Test 5: test exact --matches on Service Level:
		if (randomConsumedProductSubscription.serviceLevel!=null && !randomConsumedProductSubscription.serviceLevel.isEmpty()) {
			matchesString = randomConsumedProductSubscription.serviceLevel;
			actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
			// also test case insensitivity
			matchesString = randomizeCaseOfCharactersInString(matchesString);
			actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		} else log.warning("Skipping list --consumed --matches test on a Service Level item since it is null on our random consumed subscription: "+randomConsumedProductSubscription);
		
		
		// Test 6: test exact --matches on Provided ProductId:
		if (!randomConsumedProductSubscription.provides.isEmpty()) {
			matchesString = getRandomListItem(CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, randomConsumedProductSubscription.poolId));
			actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		} else log.warning("Skipping list --consumed --matches test on a Provides ProductId item since the provides list is empty on our random consumed subscription: "+randomConsumedProductSubscription);		
		
		// TODO Test 7: test exact --matches on a Content Name provided by a Provided ProductId
		// see ListAvailableWithExactMatches_Test()	
		
		// TODO Test 8: test exact --matches on a Content Label provided by a Provided ProductId
		// see ListAvailableWithExactMatches_Test()		
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36557", "RHEL7-51337"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list --consumed with wildcard --matches on Subscription Name, Provided Product Name, Contract Number, SKU, Service Level, Provided Product ID.  Note: wildcard match means * matches zero or more char and ? matches one char and is case insensitive.",
			groups={"Tier2Tests","blockedByBug-1146125","blockedByBug-1204311"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListConsumedWithWildcardMatches() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.6-1")) throw new SkipException("The list --matches function was not implemented in this version of subscription-manager.");
		
		String matchesString;
		List<ProductSubscription> actualProductSubscriptionMatches;

		// register if necessary
		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
			clienttasks.autoheal(null, null, true, null, null, null, null);
		} else clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
				
		// attach all the currently available subscriptions
		clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();
		
		// get all the consumed product subscriptions
		List<ProductSubscription> consumedProductSubscriptions = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, null, null, null, null, null, null).getStdout());
		
		// randomly choose one of the consumed Product Subscriptions
		ProductSubscription randomConsumedProductSubscription = getRandomListItem(consumedProductSubscriptions);
///*debugTesting 1204311*/randomConsumedProductSubscription=ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", "8a9087e34c4816e8014c48184d1b1f30", consumedProductSubscriptions);	// SKU: awesomeos-server-basic-dc; System Type: Virtual; Subscription Type: Standard (Temporary)
///*debugTesting 1204311*/randomConsumedProductSubscription=ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", "8a9087e34c4816e8014c48184cec1f16", consumedProductSubscriptions);	// SKU: awesomeos-server-basic-dc; System Type: Physical; Subscription Type: Standard
///*debugTesting 1204311*/randomConsumedProductSubscription=ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", "8a9087e34c4816e8014c48180b350dd0", consumedProductSubscriptions);	// SKU: awesomeos-virt-unlmtd-phys; System Type: Virtual; Subscription Type: Standard; Provides Awesome OS Server Bits
		log.info("Testing with randomConsumedProductSubscription="+randomConsumedProductSubscription);
		
		//	+-------------------------------------------+
		//	   Consumed Subscriptions
		//	+-------------------------------------------+
		//	Subscription Name: Awesome OS for All Arch (excpt for x86_64 content)
		//	Provides:          Awesome OS for All Arch (excpt for x86_64 content) Bits
		//	SKU:               awesomeos-all-no-86_64-cont
		//	Contract:          3
		//	Account:           12331131231
		//	Serial:            2808682313592781316
		//	Pool ID:           2c90af8b494355790149435902da0ee8
		//	Active:            True
		//	Quantity Used:     1
		//	Service Level:     
		//	Service Type:      
		//	Status Details:    
		//	Subscription Type: Stackable
		//	Starts:            10/23/2014
		//	Ends:              10/23/2015
		//	System Type:       Physical
		
		
		// Test 1: test --matches with a * wild card on Subscription Name:
		matchesString = randomConsumedProductSubscription.productName;
		matchesString = matchesString.replaceFirst("^\\S+\\s+","*");	// drop first word
		actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		// also test case insensitivity
		matchesString = randomizeCaseOfCharactersInString(matchesString);
		matchesString = matchesString.replaceFirst("\\s+\\S+$","*");	// and drop last word
		actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		
		
		// Test 2: test --matches with a * wild card on Provides:
		if (!randomConsumedProductSubscription.provides.isEmpty()) {
			matchesString = getRandomListItem(randomConsumedProductSubscription.provides);
			matchesString = matchesString.replaceFirst("\\s+\\S+$","*");	// drop last word
			actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
			// also test case insensitivity
			matchesString = randomizeCaseOfCharactersInString(matchesString);
			matchesString = matchesString.replaceFirst("^\\S+\\s+","*");	// and drop first word
			actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);

		} else log.warning("Skipping list --consumed --matches test on a Provides item since the provides list is empty on our random consumed subscription: "+randomConsumedProductSubscription);		
		
		// Test 3: test --matches with a ? wild card on SKU:
		matchesString = randomConsumedProductSubscription.productId;
		matchesString = matchesString.replaceFirst("^.","?");	// drop first char
		actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		// also test case insensitivity
		matchesString = randomizeCaseOfCharactersInString(matchesString);
		matchesString = matchesString.replaceFirst(".$","?");	// and drop last char
		actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		
		
		// Test 4: test --matches with a ? wild card on Contract:
		matchesString = String.valueOf(randomConsumedProductSubscription.contractNumber);
		matchesString = matchesString.replaceFirst(".$","?");	// drop last char
		actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		// also test case insensitivity
		/* not necessary since contractNumber is an Integer
		matchesString = randomizeCaseOfCharactersInString(matchesString);
		matchesString = matchesString.replaceFirst("^.","?");	// and drop first char
		actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, matchesString, null, null, null).getStdout());
		assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		*/
		
		// Test 5: test --matches with a * wild card on Service Level:
		if (randomConsumedProductSubscription.serviceLevel!=null && !randomConsumedProductSubscription.serviceLevel.isEmpty()) {
			matchesString = randomConsumedProductSubscription.serviceLevel;
			matchesString = matchesString.replaceFirst("^.","*");	// drop first char
			matchesString = matchesString.replaceFirst(".$","?");	// drop last char
			actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
			// also test case insensitivity
			matchesString = randomizeCaseOfCharactersInString(matchesString);
			actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		} else log.warning("Skipping list --consumed --matches test on a Service Level item since it is null on our random consumed subscription: "+randomConsumedProductSubscription);
		
		
		// Test 6: test --matches with a ? wild card on Provided ProductId:
		if (!randomConsumedProductSubscription.provides.isEmpty()) {
			matchesString = getRandomListItem(CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, randomConsumedProductSubscription.poolId));
			int i = randomGenerator.nextInt(matchesString.length());
			matchesString = matchesString.replaceAll(String.valueOf(matchesString.charAt(i)), "?");
///*debugTesting 1204311*/matchesString="3?060";		
			actualProductSubscriptionMatches = ProductSubscription.parse(clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
			assertActualResultOfListConsumedWithMatches(matchesString,actualProductSubscriptionMatches,consumedProductSubscriptions);
		} else log.warning("Skipping list --consumed --matches test on a Provides ProductId item since the provides list is empty on our random consumed subscription: "+randomConsumedProductSubscription);		
		
		// TODO Test 7: test exact --matches on a Content Name provided by a Provided ProductId
		// see ListAvailableWithExactMatches_Test()	
		
		// TODO Test 8: test exact --matches on a Content Label provided by a Provided ProductId
		// see ListAvailableWithExactMatches_Test()		
	}
	protected void assertActualResultOfListConsumedWithMatches(String matchesString, List<ProductSubscription> actualProductSubscriptionMatches, List<ProductSubscription> consumedProductSubscriptions) throws JSONException, Exception {
		// translate matchesString into a regexString
		String regexString = matchesString.toLowerCase().replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)"); // escape parentheses
		regexString = regexString.replaceAll("\\*", ".*");	// splat wildcards match any zero or more chars
		regexString = regexString.replaceAll("\\?", ".");	// question mark wildcards match any one char
		
		// search through the available availableSubscriptionPools for expected matches on the matchesString
		// NOTE: exact matches means no wildcards and is case insensitive
		List<ProductSubscription> expectedProductSubscriptionMatches = new ArrayList<ProductSubscription>();
		for (ProductSubscription consumedProductSubscription : consumedProductSubscriptions) {
			
			// Test for match on Subscription Name:
			if (consumedProductSubscription.productName.toLowerCase().matches(regexString)) {
				log.info("Found a hit on matches '"+matchesString+"' against the consumed subscription '"+consumedProductSubscription.productName+"' Subscription Name: "+consumedProductSubscription.productName);
				if (!expectedProductSubscriptionMatches.contains(consumedProductSubscription)) expectedProductSubscriptionMatches.add(consumedProductSubscription);
			}
			
			// Test for match on Provides:
			for (String providesName : consumedProductSubscription.provides) {
				if (providesName.toLowerCase().matches(regexString)) {
					log.info("Found a hit on matches '"+matchesString+"' against the consumed subscription '"+consumedProductSubscription.productName+"' Provides: "+consumedProductSubscription.provides);
					if (!expectedProductSubscriptionMatches.contains(consumedProductSubscription)) expectedProductSubscriptionMatches.add(consumedProductSubscription);
				}
			}
			
			// Test for match on SKU:
			if (consumedProductSubscription.productId.toLowerCase().matches(regexString)) {
				log.info("Found a hit on matches '"+matchesString+"' against the consumed subscription '"+consumedProductSubscription.productName+"' SKU: "+consumedProductSubscription.productId);
				if (!expectedProductSubscriptionMatches.contains(consumedProductSubscription)) expectedProductSubscriptionMatches.add(consumedProductSubscription);
			}
			
			// Test for match on Contract:
			if (String.valueOf(consumedProductSubscription.contractNumber).toLowerCase().matches(regexString)) {
				log.info("Found a hit on matches '"+matchesString+"' against the consumed subscription '"+consumedProductSubscription.productName+"' Contract: "+consumedProductSubscription.contractNumber);
				if (!expectedProductSubscriptionMatches.contains(consumedProductSubscription)) expectedProductSubscriptionMatches.add(consumedProductSubscription);
			}
			
			// Test for match on Service Level:
			if (consumedProductSubscription.serviceLevel.toLowerCase().matches(regexString)) {
				log.info("Found a hit on matches '"+matchesString+"' against the consumed subscription '"+consumedProductSubscription.productName+"' Service Level: "+consumedProductSubscription.serviceLevel);
				if (!expectedProductSubscriptionMatches.contains(consumedProductSubscription)) expectedProductSubscriptionMatches.add(consumedProductSubscription);
			}
			
			// Test for match on Provided ProductId:
			for (String providedProductId : CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumedProductSubscription.poolId)) {
				if (providedProductId.toLowerCase().matches(regexString)) {
					log.info("Found a hit on matches '"+matchesString+"' against the consumed subscription '"+consumedProductSubscription.productName+"' Provided Product ID: "+providedProductId);
					if (!expectedProductSubscriptionMatches.contains(consumedProductSubscription)) expectedProductSubscriptionMatches.add(consumedProductSubscription);		
				}
			}
			
// debugTesting
// THIS TEST IS WRONG. DO NOT CHECK FOR MATCHES ON *DERIVED* PRODUCT IDS
//			// Test for match on Derived Provided ProductId:
//			for (String derivedProvidedProductId : CandlepinTasks.getPoolDerivedProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumedProductSubscription.poolId)) {
//				if (derivedProvidedProductId.toLowerCase().matches(regexString)) {
//					log.info("Found a hit on matches '"+matchesString+"' against the consumed subscription '"+consumedProductSubscription.productName+"' Derived Provided Product ID: "+derivedProvidedProductId);
//					if (!expectedProductSubscriptionMatches.contains(consumedProductSubscription)) expectedProductSubscriptionMatches.add(consumedProductSubscription);		
//				}
//			}
			
			// Test for match on Derived Provided ProductId:
			// NOTE: list --available --matches is implemented server-side and appears to be searching the derivedProvidedProducts for product id matches.  Although unexpected, this feature has some benefit.
			// This behavior is in contrast to list --consumed --matches which is implemented client-side and does NOT search the derivedProvidedProducts for match on product id.
			// 10/31/2014 Verbal scrum discussion with devel decided to keep this behavior.
			/* Not testing for a derivedProvidedProduct ID match against --consumed as a result of discussion above 
			for (String derivedProvidedProductId : CandlepinTasks.getPoolDerivedProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumedProductSubscription.poolId)) {
				if (derivedProvidedProductId.toLowerCase().matches(regexString)) {
					log.info("Found a hit on matches '"+matchesString+"' against the consumed subscription '"+consumedProductSubscription.productName+"' Derived Provided Product ID: "+derivedProvidedProductId);
					if (!expectedProductSubscriptionMatches.contains(consumedProductSubscription)) expectedProductSubscriptionMatches.add(consumedProductSubscription);		
				}
			}
			*/
			
			// TODO May want to open an RFE to also search on these other consumed product fields
			// See https://bugzilla.redhat.com/show_bug.cgi?id=1146125#c7
			//	Account:           12331131231
			//	Serial:            2808682313592781316
			//	Pool ID:           2c90af8b494355790149435902da0ee8
			//	Active:            True
			//	Quantity Used:     1 
			//	Service Type:      
			//	Status Details:    
			//	Subscription Type: Stackable
			//	System Type:       Physical
		}
		
		// assert that all of the expectedProductSubscriptionMatches is identical to the actualProductSubscriptionMatches
		for (ProductSubscription expectedProductSubscription : expectedProductSubscriptionMatches) {
			if (!actualProductSubscriptionMatches.contains(expectedProductSubscription))
				log.warning("The actual list of product subscription matches does NOT contain expected product subscription: "+expectedProductSubscription);
		}
		for (ProductSubscription actualProductSubscription : actualProductSubscriptionMatches) {
			if (!expectedProductSubscriptionMatches.contains(actualProductSubscription))
				// issues with bug 1204311 are showing up here 
				log.warning("The expected list of product subscription matches does NOT contain actual product subscription: "+actualProductSubscription);
		}
		Assert.assertTrue(expectedProductSubscriptionMatches.containsAll(actualProductSubscriptionMatches)&&actualProductSubscriptionMatches.containsAll(expectedProductSubscriptionMatches), "All of the expected consumed subscriptions with an exact match (ignoring case) on '"+matchesString+"' were returned with the list --consumed --matches option.");

	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36563", "RHEL7-51345"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list --installed with exact --matches on Product Name, Product ID.  Note: exact match means no wildcards and is case insensitive.",
			groups={"Tier2Tests","blockedByBug-1146125"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListInstalledWithExactMatches() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.6-1")) throw new SkipException("The list --matches function was not implemented in this version of subscription-manager.");
		
		String matchesString;
		List<InstalledProduct> actualInstalledProductMatches;

		// register if necessary
		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
			clienttasks.autoheal(null, null, true, null, null, null, null);
		} else clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
				
		// attach one of all the currently available subscriptions (attaching only one so that some installed products might remain noncompliant)
		// assemble a list of all the available SubscriptionPool ids
		List<String> poolIds = new ArrayList<String>();
		List<SubscriptionPool> subscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		for (SubscriptionPool pool : subscriptionPools) poolIds.add(pool.poolId);
		if (!poolIds.isEmpty()) clienttasks.subscribe(null,null,poolIds, null, null, "1", null, null,null,null,null, null, null);
		
		// get all the installed products
		List<InstalledProduct> installedProducts = InstalledProduct.parse(clienttasks.list(null, null, null, true, null, null, null, null, null, null, null, null, null, null, null).getStdout());
		
		// randomly choose one of the installed products
		InstalledProduct randomInstalledProduct = getRandomListItem(installedProducts);
		
		//	[root@jsefler-os7 ~]# subscription-manager list --installed
		//	Product Name:   Red Hat Enterprise Linux Server
		//	Product ID:     69
		//	Version:        7.0
		//	Arch:           x86_64
		//	Status:         Not Subscribed
		//	Status Details: Not supported by a valid subscription.
		//	Starts:         
		//	Ends:   
		
		
		// Test 1: test exact --matches on Product Name:
		matchesString = randomInstalledProduct.productName;
		actualInstalledProductMatches = InstalledProduct.parse(clienttasks.list(null, null, null, true, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListInstalledWithMatches(matchesString,actualInstalledProductMatches,installedProducts);
		// also test case insensitivity
		matchesString = randomizeCaseOfCharactersInString(matchesString);
		actualInstalledProductMatches = InstalledProduct.parse(clienttasks.list(null, null, null, true, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListInstalledWithMatches(matchesString,actualInstalledProductMatches,installedProducts);
		
		
		// Test 2: test exact --matches on Product ID:
		matchesString = randomInstalledProduct.productId;
		actualInstalledProductMatches = InstalledProduct.parse(clienttasks.list(null, null, null, true, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListInstalledWithMatches(matchesString,actualInstalledProductMatches,installedProducts);
		// also test case insensitivity
		matchesString = randomizeCaseOfCharactersInString(matchesString);
		actualInstalledProductMatches = InstalledProduct.parse(clienttasks.list(null, null, null, true, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListInstalledWithMatches(matchesString,actualInstalledProductMatches,installedProducts);

	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36559", "RHEL7-51339"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list --installed with wildcard --matches on Product Name, Product ID.  Note: wildcard match means * matches zero or more char and ? matches one char and is case insensitive.",
			groups={"Tier2Tests","blockedByBug-1146125"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListInstalledWithWildcardMatches() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.6-1")) throw new SkipException("The list --matches function was not implemented in this version of subscription-manager.");
		
		String matchesString;
		List<InstalledProduct> actualInstalledProductMatches;

		// register if necessary
		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
			clienttasks.autoheal(null, null, true, null, null, null, null);
		} else clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
				
		// attach one of all the currently available subscriptions (attaching only one so that some installed products might remain noncompliant)
		// assemble a list of all the available SubscriptionPool ids
		List<String> poolIds = new ArrayList<String>();
		List<SubscriptionPool> subscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		for (SubscriptionPool pool : subscriptionPools) poolIds.add(pool.poolId);
		if (!poolIds.isEmpty()) clienttasks.subscribe(null,null,poolIds, null, null, "1", null, null,null,null,null, null, null);
		
		// get all the installed products
		List<InstalledProduct> installedProducts = InstalledProduct.parse(clienttasks.list(null, null, null, true, null, null, null, null, null, null, null, null, null, null, null).getStdout());
		
		// randomly choose one of the installed products
		InstalledProduct randomInstalledProduct = getRandomListItem(installedProducts);
		
		//	[root@jsefler-os7 ~]# subscription-manager list --installed
		//	Product Name:   Red Hat Enterprise Linux Server
		//	Product ID:     69
		//	Version:        7.0
		//	Arch:           x86_64
		//	Status:         Not Subscribed
		//	Status Details: Not supported by a valid subscription.
		//	Starts:         
		//	Ends:   
		
		
		// Test 1: test wildcard --matches on Product Name:
		matchesString = randomInstalledProduct.productName;
		matchesString = matchesString.replaceFirst("^\\S+\\s+","*");	// drop first word
		actualInstalledProductMatches = InstalledProduct.parse(clienttasks.list(null, null, null, true, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListInstalledWithMatches(matchesString,actualInstalledProductMatches,installedProducts);
		// also test case insensitivity
		matchesString = randomizeCaseOfCharactersInString(matchesString);
		matchesString = matchesString.replaceFirst("\\s+\\S+$","*");	// and drop last word
		actualInstalledProductMatches = InstalledProduct.parse(clienttasks.list(null, null, null, true, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListInstalledWithMatches(matchesString,actualInstalledProductMatches,installedProducts);
		
		
		// Test 2: test wildcard --matches on Product ID:
		matchesString = randomInstalledProduct.productId;
		int i = randomGenerator.nextInt(matchesString.length());
		matchesString = matchesString.replaceAll(String.valueOf(matchesString.charAt(i)), "?");
		actualInstalledProductMatches = InstalledProduct.parse(clienttasks.list(null, null, null, true, null, null, null, null, null, matchesString, null, null, null, null, null).getStdout());
		assertActualResultOfListInstalledWithMatches(matchesString,actualInstalledProductMatches,installedProducts);
		// also test case insensitivity
		/* not necessary since productId is an integer
		matchesString = randomizeCaseOfCharactersInString(matchesString);
		actualInstalledProductMatches = InstalledProduct.parse(clienttasks.list(null, null, null, true, null, null, null, null, matchesString, null, null, null).getStdout());
		assertActualResultOfListInstalledWithMatches(matchesString,actualInstalledProductMatches,installedProducts);
		*/
		
	}
	protected void assertActualResultOfListInstalledWithMatches(String matchesString, List<InstalledProduct> actualInstalledProductMatches, List<InstalledProduct> installedProducts) throws JSONException, Exception {
		// translate matchesString into a regexString
		String regexString = matchesString.toLowerCase().replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)"); // escape parentheses
		regexString = regexString.replaceAll("\\*", ".*");	// splat wildcards match any zero or more chars
		regexString = regexString.replaceAll("\\?", ".");	// question mark wildcards match any one char
		
		// search through the available availableSubscriptionPools for expected matches on the matchesString
		// NOTE: exact matches means no wildcards and is case insensitive
		List<InstalledProduct> expectedInstalledProductMatches = new ArrayList<InstalledProduct>();
		for (InstalledProduct installedProduct : installedProducts) {
			
			// Test for match on Product Name:
			if (installedProduct.productName.toLowerCase().matches(regexString)) {
				log.info("Found a hit on matches '"+matchesString+"' against the installed product '"+installedProduct.productName+"' Product Name: "+installedProduct.productName);
				if (!expectedInstalledProductMatches.contains(installedProduct)) expectedInstalledProductMatches.add(installedProduct);
			}
			
			// Test for match on Product ID:
			if (installedProduct.productId.toLowerCase().matches(regexString)) {
				log.info("Found a hit on matches '"+matchesString+"' against the installed product '"+installedProduct.productName+"' Product ID: "+installedProduct.productId);
				if (!expectedInstalledProductMatches.contains(installedProduct)) expectedInstalledProductMatches.add(installedProduct);
			}
			
			// TODO May want to open an RFE to also search on these other installed product fields
			// See https://bugzilla.redhat.com/show_bug.cgi?id=1146125#c7
			//	Version:        7.0
			//	Arch:           x86_64
			//	Status:         Not Subscribed
			//	Status Details: Not supported by a valid subscription.
			//	Starts:         
			//	Ends:   
		}
		
		// assert that all of the expectedProductSubscriptionMatches is identical to the actualProductSubscriptionMatches
		Assert.assertTrue(expectedInstalledProductMatches.containsAll(actualInstalledProductMatches)&&actualInstalledProductMatches.containsAll(expectedInstalledProductMatches), "All of the expected installed products with an exact match (ignoring case) on '"+matchesString+"' were returned with the list --installed --matches option.");

	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36555", "RHEL7-51333"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list --available with --matches='nothing'",
			groups={"Tier2Tests","blockedByBug-1146125","blockedByBug-1493711"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListAvailableWithMatchesNothing() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.6-1")) throw new SkipException("The list --matches function was not implemented in this version of subscription-manager.");
		
		String matchesString = "nothing";
		
		// register if necessary
		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
			clienttasks.autoheal(null, null, true, null, null, null, null);
		} else clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
		
		// test
		SSHCommandResult result = clienttasks.list(null, true, null, null, null, null, null, null, null, matchesString, null, null, null, null, null);
		String expectedStdout = "No available subscription pools matching the specified criteria were found.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.12-1")) expectedStdout = String.format("No available subscription pools were found matching the expression \"%s\".",matchesString);	// commit 2884e33acb35ab4e336fe12dc23de7ab26cc0572	// Bug 1159348 - list --matched should show the filter string when warning about empty matches
		Assert.assertEquals(result.getExitCode(),new Integer(0),		"Exitcode expected from calling list --available --matches with no expected matches.");
		Assert.assertEquals(result.getStdout().trim(),expectedStdout,	"Stdout expected from calling list --consumed --matches with no expected matches.");
		Assert.assertEquals(result.getStderr().trim(),"",				"Stderr expected from calling list --available --matches with no expected matches.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36560", "RHEL7-51340"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list --consumed with --matches='nothing'",
			groups={"Tier2Tests","blockedByBug-1146125","blockedByBug-1493711"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListConsumedWithMatchesNothing() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.6-1")) throw new SkipException("The list --matches function was not implemented in this version of subscription-manager.");
		
		String matchesString = "nothing";
		
		// register if necessary
		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		}
		
		// attach any random pool
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("Cannot randomly pick a pool for subscribing when there are no available pools for testing."); 
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribe_(null,null,pool.poolId,null,null,null,null,null,null,null,null, null, null);
		
		// test
		SSHCommandResult result = clienttasks.list(null, null, true, null, null, null, null, null, null, matchesString, null, null, null, null, null);
		String expectedStdout = "No consumed subscription pools matching the specified criteria were found.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.12-1")) expectedStdout = String.format("No consumed subscription pools were found matching the expression \"%s\".",matchesString);	// commit 2884e33acb35ab4e336fe12dc23de7ab26cc0572	// Bug 1159348 - list --matched should show the filter string when warning about empty matches
		Assert.assertEquals(result.getExitCode(),new Integer(0),		"Exitcode expected from calling list --consumed --matches with no expected matches.");
		Assert.assertEquals(result.getStdout().trim(),expectedStdout,	"Stdout expected from calling list --available --consumed with no expected matches.");
		Assert.assertEquals(result.getStderr().trim(),"",				"Stderr expected from calling list --consumed --matches with no expected matches.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36558", "RHEL7-51338"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list --installed with --matches='nothing'",
			groups={"Tier2Tests","blockedByBug-1146125"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListInstalledWithMatchesNothing() {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.6-1")) throw new SkipException("The list --matches function was not implemented in this version of subscription-manager.");
		
		String matchesString = "nothing";
		
		SSHCommandResult result = clienttasks.list(null, null, null, true, null, null, null, null, null, matchesString, null, null, null, null, null);
		String expectedStdout = "No installed products matching the specified criteria were found.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.12-1")) expectedStdout = String.format("No installed products were found matching the expression \"%s\".",matchesString);	// commit 2884e33acb35ab4e336fe12dc23de7ab26cc0572	// Bug 1159348 - list --matched should show the filter string when warning about empty matches
		Assert.assertEquals(result.getExitCode(),new Integer(0),		"Exitcode expected from calling list --installed --matches with no expected matches.");
		Assert.assertEquals(result.getStdout().trim(), expectedStdout,	"Stdout expected from calling list --installed --consumed with no expected matches.");
		Assert.assertEquals(result.getStderr().trim(),"",				"Stderr expected from calling list --installed --matches with no expected matches.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36561", "RHEL7-51342"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list --available --consumed --installed with --matches='nothing'",
			groups={"Tier2Tests","blockedByBug-1146125","blockedByBug-1493711"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListAvailableConsumedInstalledWithMatchesNothing() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.6-1")) throw new SkipException("The list --matches function was not implemented in this version of subscription-manager.");
		
		String matchesString = "nothing";
		
		// register if necessary
		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		}
		
		// attach any random pool
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("Cannot randomly pick a pool for subscribing when there are no available pools for testing."); 
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribe_(null,null,pool.poolId,null,null,null,null,null,null,null,null, null, null);
		
		// test
		SSHCommandResult result = clienttasks.list(null, true, true, true, null, null, null, null, null, matchesString, null, null, null, null, null);
		String expectedStdout = "No installed products matching the specified criteria were found.\nNo available subscription pools matching the specified criteria were found.\nNo consumed subscription pools matching the specified criteria were found.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.12-1")) expectedStdout = String.format("No installed products were found matching the expression \"%s\".\nNo available subscription pools were found matching the expression \"%s\".\nNo consumed subscription pools were found matching the expression \"%s\".",matchesString,matchesString,matchesString);	// commit 2884e33acb35ab4e336fe12dc23de7ab26cc0572	// Bug 1159348 - list --matched should show the filter string when warning about empty matches
		Assert.assertEquals(result.getExitCode(),new Integer(0),		"Exitcode expected from calling list --installed --available --consumed --matches with no expected matches.");
		Assert.assertEquals(result.getStdout().trim(),expectedStdout,	"Stdout expected from calling list --installed --available --consumed --matches with no expected matches.");
		Assert.assertEquals(result.getStderr().trim(),"",				"Stderr expected from calling list --installed --available --consumed --matches with no expected matches.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36562", "RHEL7-51344"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list --available with --pool-only",
			groups={"Tier2Tests","blockedByBug-1159974"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListAvailableWithPoolOnly() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.13.8-1")) throw new SkipException("The list --pool-only function was not implemented in this version of subscription-manager.");	// commit 25cb581cb6ebe13063d0f78a5020715a2854d337 bug 1159974
		
		Boolean all = getRandomBoolean();
		Boolean matchInstalled = getRandomBoolean();
		Boolean noOverlap = getRandomBoolean();
		
		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
			clienttasks.autoheal(null, null, true, null, null, null, null);
		} else clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
		
		// get available subscription pools
		List<SubscriptionPool> availableSubscriptionPools = SubscriptionPool.parse(clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, null, null, null, null, null, null).getStdout());
		
		// make the same call with --pool-only
		SSHCommandResult poolOnlyResult = clienttasks.list(all, true, null, null, null, null, null, matchInstalled, noOverlap, null, true, null, null, null, null);
		//	[root@jsefler-os7 ~]# subscription-manager list --available --pool-only
		//	2c90af8b4976c7ee014976cb29bf0b02
		//	2c90af8b4976c7ee014976cb388c1256
		//	2c90af8b4976c7ee01497bbfec1837da
		//	2c90af8b4976c7ee014976cb1f0d06f8

		// convert the result to a list
		List<String> actualSubscriptionPoolIds = new ArrayList<String>();
		if (!poolOnlyResult.getStdout().trim().isEmpty()) actualSubscriptionPoolIds = Arrays.asList(poolOnlyResult.getStdout().trim().split("\n"));
		
		// assert the result
		Assert.assertEquals(poolOnlyResult.getExitCode(),new Integer(0),	"Exitcode expected from calling list --available --pool-only");
		for (SubscriptionPool availableSubscriptionPool : availableSubscriptionPools) Assert.assertTrue(actualSubscriptionPoolIds.contains(availableSubscriptionPool.poolId),	"The result of list --available with --pool-only contains expected poolId '"+availableSubscriptionPool.poolId+"'.");
		Assert.assertEquals(actualSubscriptionPoolIds.size(), availableSubscriptionPools.size(),	"The number of poolIds returned from calling list --pool-only should match the number of available SubscriptionPools listed without --pool-only.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27116", "RHEL7-51336"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list consumed should filter by servicelevel when this option is passed.",
			groups={"Tier2Tests","blockedByBug-800933","blockedByBug-800999"},
			dataProvider="getConsumedWithServicelevelData",
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListConsumedWithServicelevel(Object bugzilla, String servicelevel) throws Exception {
		SSHCommandResult listResult;
		List<ProductSubscription> expectedProductSubscriptions, filteredProductSubscriptions;
				
		// list consumed (without service level)
		listResult = clienttasks.list_(false,false,true,null,null,null,null,null,null, null, null, null, null, null, null);
		List<ProductSubscription> allConsumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		
		// determine the subset of expected pools with a matching servicelevel
		// CASE SENSITIVE expectedProductSubscriptions = ProductSubscription.findAllInstancesWithMatchingFieldFromList("serviceLevel", servicelevel, allConsumedProductSubscriptions);
		expectedProductSubscriptions = ProductSubscription.findAllInstancesWithCaseInsensitiveMatchingFieldFromList("serviceLevel", servicelevel, allConsumedProductSubscriptions);

		// list consumed filtered by servicelevel
		listResult = clienttasks.list_(false,false,true,null,servicelevel,null,null,null,null, null, null, null, null, null, null);
		Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(0), "The exit code from the list --consumed --servicelevel command indicates a success.");
		
		// assert results
		filteredProductSubscriptions = ProductSubscription.parse(listResult.getStdout());
		Assert.assertTrue(filteredProductSubscriptions.containsAll(expectedProductSubscriptions),"The actual list of --consumed --servicelevel=\""+servicelevel+"\" ProductSubscriptions contains all of the expected ProductSubscriptions (the expected list contains only consumptions with ServiceLevel=\""+servicelevel+"\")");
		Assert.assertTrue(expectedProductSubscriptions.containsAll(filteredProductSubscriptions),"The expected list of ProductSubscriptions contains all of the actual ProductSubscriptions returned by list --consumed --servicelevel=\""+servicelevel+"\".");
		String expectedStdout = "No consumed subscription pools to list";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.6-1")) expectedStdout = "No consumed subscription pools matching the specified criteria were found.";	// commit be815d04d1722dd8fd40a23c0a7847e97e689f89
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.12-1")) expectedStdout = String.format("No consumed subscription pools were found matching the service level \"%s\".",servicelevel);	// commit 2884e33acb35ab4e336fe12dc23de7ab26cc0572	// Bug 1159348 - list --matched should show the filter string when warning about empty matches
		if (expectedProductSubscriptions.isEmpty()) Assert.assertEquals(listResult.getStdout().trim(), expectedStdout,"Expected stdout message when no consumed subscriptions remain after list is filtered by --servicelevel=\""+servicelevel+"\".");
	}
	
	
    protected String subscriptionNameForSubscriptionContainingUTF8Character = "Subscription name containing UTF–8 character \\u2013";	// the \u2013 character is between "UTF" and "8"
	protected String productIdForSubscriptionContainingUTF8Character = "utf8-subscription-sku";
	protected SubscriptionPool poolForSubscriptionContainingUTF8Character = null;
	@BeforeGroups(groups={"setup"},value="SubscriptionContainingUTF8CharacterTests")
	public void beforeGroupForSubscriptionContainingUTF8CharacterTests() throws JSONException, Exception {
		List<String> providedProductIds = new ArrayList<String>(); providedProductIds.clear();
		Map<String,String> attributes = new HashMap<String,String>(); attributes.clear();
		attributes.put("version", "2013");
		// delete already existing subscription and products
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, productIdForSubscriptionContainingUTF8Character);
		String resourcePath = "/products/"+productIdForSubscriptionContainingUTF8Character;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		// create a new engineering product, marketing product that provides the engineering product, and a subscription for the marketing product
		attributes.put("type", "MKT");
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "919584";	// Bug 919584 - 'ascii' codec can't decode byte 0xc3 in position 3: ordinal not in range(128)
		boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping the creation of subscription name containing UTF8 character: "+subscriptionNameForSubscriptionContainingUTF8Character);
			return;
		}
		// END OF WORKAROUND
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, subscriptionNameForSubscriptionContainingUTF8Character, productIdForSubscriptionContainingUTF8Character, 1, attributes, null);
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, 20, -1*24*60/*1 day ago*/, 15*24*60/*15 days from now*/, getRandInt(), getRandInt(), productIdForSubscriptionContainingUTF8Character, providedProductIds, null);
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36566", "RHEL7-51351"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager list available should display subscriptions containing UTF-8 character(s)",
			groups={"Tier2Tests","SubscriptionContainingUTF8CharacterTests","blockedByBug-880070","blockedByBug-919584","blockedByBug-977535"},
			priority=110,
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListSubscriptionContainingUTF8Character() {
		
		//	201212051805:22.585 - FINE: ssh root@jsefler-6.usersys.redhat.com subscription-manager list --available (com.redhat.qe.tools.SSHCommandRunner.run)
		//	201212051805:24.606 - FINE: Stdout: 
		//	+-------------------------------------------+
		//	    Available Subscriptions
		//	+-------------------------------------------+
		//	201212051805:24.770 - FINE: Stderr: 'ascii' codec can't encode character u'\u2013' in position 55: ordinal not in range(128) (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201212051805:24.773 - FINE: ExitCode: 255 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		
		//	[root@jsefler-6 ~]# LANG=en_US subscription-manager list --avail
		//	+-------------------------------------------+
		//	    Available Subscriptions
		//	+-------------------------------------------+
		//	'latin-1' codec can't encode character u'\u2013' in position 55: ordinal not in range(256)
		//	[root@jsefler-6 ~]# 
		
		
		// 5/29/2013 Notes: the fix for bug 919584 actually reverted the fix in https://bugzilla.redhat.com/show_bug.cgi?id=800323#c7
		// which means this test is failing to run subscription-manager within the ssh sub-shell that we use for cli automation.
		// If I manually edit /usr/sbin/subscription-manager and paste this patch after the imports, then life is good..
		// If dev does not fix this, then I'm not yet sure what a good work-around solution is for this problem.
		/*
		import codecs
		# Change encoding of output streams when no encoding is forced via
		# $PYTHONIOENCODING or setting in lib/python{version}/site-packages
		if sys.getdefaultencoding() == 'ascii':
		    writer_class = codecs.getwriter('utf-8')
		if sys.stdout.encoding == None:
		    sys.stdout = writer_class(sys.stdout)
		if sys.stderr.encoding == None:
		    sys.stderr = writer_class(sys.stderr)
		*/
		
		// register and list --available to find the subscription containing a \u2013 character
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		//List<SubscriptionPool> availableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		//	9/9/2013 Notes: During the RHEL65 test cycle, it appears that the following PYTHONIOENCODING=ascii workaround is
		//	no longer needed (last altered by Bug fix 977535).  In fact if the workaround remains in place, the codec error is thrown:
		//	'ascii' codec can't encode character u'\u2013' in position 51: ordinal not in range(128)
		//	Reverting back to the original method for listing available pools without using PYTHONIOENCODING=ascii on the command line...
		//List<SubscriptionPool> availableSubscriptionPools = SubscriptionPool.parse(clienttasks.runCommandWithLang(null, clienttasks.command+" list --available").getStdout());
		List<SubscriptionPool> availableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		poolForSubscriptionContainingUTF8Character = SubscriptionPool.findFirstInstanceWithCaseInsensitiveMatchingFieldFromList("productId", productIdForSubscriptionContainingUTF8Character, availableSubscriptionPools);
		Assert.assertNotNull(poolForSubscriptionContainingUTF8Character, "Found subscription product '"+productIdForSubscriptionContainingUTF8Character+"' from the list of available subscriptions whose name contains a UTF8 character.");
		Assert.assertEquals(poolForSubscriptionContainingUTF8Character.subscriptionName, subscriptionNameForSubscriptionContainingUTF8Character, "asserting the subscription name.");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37704", "RHEL7-51352"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager attach a subscription containing UTF-8 character(s)",
			groups={"Tier2Tests","SubscriptionContainingUTF8CharacterTests","blockedByBug-889204","blockedByBug-981689"},
			dependsOnMethods={"testListSubscriptionContainingUTF8Character"},
			priority=120,
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testAttachSubscriptionContainingUTF8Character() throws JSONException, Exception {
		// TEMPORARY WORKAROUND FOR BUG
		String bugId="889204";	// Bug 889204 - encountering the following stderr msg when subscription name contains UTF8 chars: [priority,] message string
		Boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			// must cleanup utf8-subscription-sku to avoid contaminating other tests; then skip this test
			afterGroupForSubscriptionContainingUTF8CharacterTests();
			throw new SkipException("Skipping test while bug '"+bugId+"' is open.");
		}
		// END OF WORKAROUND
			
		SSHCommandResult sshCommandResult = clienttasks.runCommandWithLang(null, clienttasks.command+" attach --pool "+poolForSubscriptionContainingUTF8Character.poolId);
		Assert.assertEquals(sshCommandResult.getStdout().trim(), String.format("Successfully attached a subscription for: %s",subscriptionNameForSubscriptionContainingUTF8Character), "Stdout from an attempt to attach '"+subscriptionNameForSubscriptionContainingUTF8Character+"'.");
		Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "Stderr from an attempt to attach '"+subscriptionNameForSubscriptionContainingUTF8Character+"'.");
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "ExitCode from an attempt to attach '"+subscriptionNameForSubscriptionContainingUTF8Character+"'.");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37705", "RHEL7-51353"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="rct: cat-cert an entitlement containing UTF-8 character(s)",
			groups={"Tier2Tests","SubscriptionContainingUTF8CharacterTests","blockedByBug-890296","blockedByBug-1048325"},
			dependsOnMethods={"testAttachSubscriptionContainingUTF8Character"},
			priority=130,
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testCatCertContainingUTF8Character() throws JSONException, Exception {
		// TEMPORARY WORKAROUND FOR BUG
		String bugId="890296";	// Bug 890296 - 'ascii' codec can't encode character u'\u2013'.
		Boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			// must cleanup utf8-subscription-sku to avoid contaminating other tests; then skip this test
			afterGroupForSubscriptionContainingUTF8CharacterTests();
			throw new SkipException("Skipping test while bug '"+bugId+"' is open.");
		}
		// END OF WORKAROUND
		
		List<File> entitlementCertFiles = clienttasks.getCurrentEntitlementCertFiles();
		Assert.assertEquals(entitlementCertFiles.size(), 1, "Expecting only one entitlement cert being consumed.");
		
		//	[root@jsefler-6 ~]# rct cat-cert /etc/pki/entitlement/6487424643396210003.pem 
		//	Traceback (most recent call last):
		//	  File "/usr/bin/rct", line 44, in <module>
		//	    sys.exit(abs(main() or 0))
		//	  File "/usr/bin/rct", line 39, in main
		//	    return RctCLI().main()
		//	  File "/usr/share/rhsm/subscription_manager/cli.py", line 156, in main
		//	    return cmd.main()
		//	  File "/usr/share/rhsm/rct/commands.py", line 44, in main
		//	    return_code = self._do_command()
		//	  File "/usr/share/rhsm/rct/commands.py", line 92, in _do_command
		//	    skip_products=self.options.no_products)
		//	  File "/usr/share/rhsm/rct/printing.py", line 196, in printc
		//	    printer.printc(cert)
		//	  File "/usr/share/rhsm/rct/printing.py", line 105, in printc
		//	    print self.cert_to_str(cert)
		//	  File "/usr/share/rhsm/rct/printing.py", line 166, in cert_to_str
		//	    order_printer.as_str(cert.order), "\n".join(s))
		//	  File "/usr/share/rhsm/rct/printing.py", line 47, in as_str
		//	    s.append("\t%s: %s" % (_("Name"), xstr(order.name)))
		//	  File "/usr/share/rhsm/rct/printing.py", line 26, in xstr
		//	    return str(value)
		//	UnicodeEncodeError: 'ascii' codec can't encode character u'\u2013' in position 32: ordinal not in range(128)
		//	[root@jsefler-6 ~]# echo $?
		//	1
		
		//SSHCommandResult sshCommandResult = clienttasks.runCommandWithLang(null/* null will cause command to be prefixed with PYTHONIOENCODING=ascii */, "rct cat-cert "+entitlementCertFiles.get(0));	// need for PYTHONIOENCODING=ascii workaround was eliminated by bug 1048325
		SSHCommandResult sshCommandResult = client.runCommandAndWait("rct cat-cert "+entitlementCertFiles.get(0));
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "ExitCode from an attempt to run rct cat-cert on an entitlement containing UTF-8 character(s)");
		Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "Stderr from an attempt to run rct cat-cert on an entitlement containing UTF-8 character(s)");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37706", "RHEL7-51354"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subcription manager remove a consumed subscription containing UTF-8 character(s)",
			groups={"Tier2Tests","SubscriptionContainingUTF8CharacterTests","blockedByBug-889204"},
			dependsOnMethods={"testCatCertContainingUTF8Character"},
			priority=140,
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testRemoveSubscriptionContainingUTF8Character() {
		// SURPRISED THAT THIS DID NOT WORK List AND THE FOLLOWING DOES <ProductSubscription> consumedProductSubscriptions = ProductSubscription.parse(clienttasks.runCommandWithLang(null, clienttasks.command+" list --consumed").getStdout());
		List<ProductSubscription> consumedProductSubscriptions = ProductSubscription.parse(client.runCommandAndWait(clienttasks.command+" list --consumed").getStdout());
		ProductSubscription consumedProductSubscription = ProductSubscription.findFirstInstanceWithCaseInsensitiveMatchingFieldFromList("productId", productIdForSubscriptionContainingUTF8Character, consumedProductSubscriptions);
		Assert.assertNotNull(consumedProductSubscription, "Found subscription product '"+productIdForSubscriptionContainingUTF8Character+"' from the list of consumed subscriptions whose name contains a UTF8 character.");
		Assert.assertEquals(consumedProductSubscription.productName, subscriptionNameForSubscriptionContainingUTF8Character, "asserting the consumed subscription name.");
		clienttasks.unsubscribeFromSerialNumber(consumedProductSubscription.serialNumber);
	}
	@AfterClass(groups={"setup"})	// needed since @AfterGroups will skip when some of the tests within the group are skipped even when alwaysRun=true
	@AfterGroups(groups={"setup"},value="SubscriptionContainingUTF8CharacterTests",alwaysRun=true /* does not seem to have any effect */)
	public void afterGroupForSubscriptionContainingUTF8CharacterTests() throws JSONException, Exception {
		clienttasks.unregister_(null, null, null, null);	// to return any consumed subscriptions containing UTF8 characters
		if (productIdForSubscriptionContainingUTF8Character!=null && !sm_serverType.equals(CandlepinType.hosted)/* we do NOT create/delete subscriptions against hosted */) CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, productIdForSubscriptionContainingUTF8Character);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19966", "RHEL7-33101"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: list installed should include product certs in /etc/pki/product-default",  // see description in https://github.com/candlepin/subscription-manager/pull/1009
			groups={"Tier1Tests","ListInstalledWithProductDefault_Test","blockedByBug-1123029"/*1080012*/},	// subscription-manager 1123029 - [RFE] Use default product certificates when they are present COMPLEMENT TO REL-ENG RFE 1080012 - [RFE] Include default product certificate in redhat-release
			priority=150,
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testListInstalledWithProductDefault() {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.12.14-1")) throw new SkipException("The /etc/pki/product-default feature is not implemented in this version of subscription-manager.");
		
		// strategy...
		// copy a random selection of product certs from subscription-manager-migration-data into /etc/pki/product-default
		// make sure that some of the product certs copied include product ids already installed in /etc/pki/product
		// assert that list --installed includes all the product certs but those in /etc/pki/product take precedence over /etc/pki/product-default
		
		// assert the existance of /etc/pki/product-default
		Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.productCertDefaultDir),"Expecting directory '"+clienttasks.productCertDefaultDir+"' to exist.");
		
		// get the original product certs that are currently installed in /etc/pki/product-default
		originalDefaultProductCerts = clienttasks.getProductCerts(clienttasks.productCertDefaultDir);
		
		// get the product certs that are currently installed in /etc/pki/product
		List<ProductCert> productCerts = clienttasks.getProductCerts(clienttasks.productCertDir);
				
		// copy migration-data product certs to /etc/pki/product-default (including productCerts whose product id matches /etc/pki/product cert and does not already exist in /etc/pki/product-default)
		List<File> migrationProductCertsFiles = clienttasks.getProductCertFiles("/usr/share/rhsm/product/RHEL"+"-"+clienttasks.redhatReleaseX);
		Set<String> defaultProductCertProductIds = new HashSet<String>();
		for (ProductCert defaultProductCert : originalDefaultProductCerts) defaultProductCertProductIds.add(defaultProductCert.productId);
		String  migrationProductCertFilesToCopy = "";
		for (File migrationProductCertFile : migrationProductCertsFiles) {
			String migrationProductCertProductId = MigrationDataTests.getProductIdFromProductCertFilename(migrationProductCertFile.getPath());
			
			// if this migrationProductCertProductId is already among the defaultProductCertProductIds, skip it - do not copy another since it does not make sense to have multiple product certs with the same productId in the same directory
			if (defaultProductCertProductIds.contains(migrationProductCertProductId)) {
				continue;
			}
			
			// if this migrationProductCertProductId is installed in /etc/pki/product, copy it to /etc/pki/product-default
			if (ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", migrationProductCertProductId, productCerts) != null) {
				// TOO MUCH LOGGING client.runCommandAndWait("cp -n "+migrationProductCertFile+" "+clienttasks.productCertDefaultDir);
				migrationProductCertFilesToCopy += migrationProductCertFile+" ";
				defaultProductCertProductIds.add(migrationProductCertProductId);
				continue;
			}
			
			// copy this migrationProductCertProductId to /etc/pki/product-default
			// randomly skip 75% of these copies to reduce logging noise
			if (getRandomListItem(Arrays.asList(new Integer[]{1/*,2,3,4*/})).equals(1)) {
				// TOO MUCH LOGGING client.runCommandAndWait("cp -n "+migrationProductCertFile+" "+clienttasks.productCertDefaultDir);
				migrationProductCertFilesToCopy += migrationProductCertFile+" ";
				defaultProductCertProductIds.add(migrationProductCertProductId);
			}
		}
		if (!migrationProductCertFilesToCopy.isEmpty()) client.runCommandAndWait("cp -n "+migrationProductCertFilesToCopy+" "+clienttasks.productCertDefaultDir);
		
		// get the product certs that are currently installed in /etc/pki/product-default
		List<ProductCert> defaultProductCerts = clienttasks.getProductCerts(clienttasks.productCertDefaultDir);
		
		// get the currently InstalledProducts
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		
		// verify that each of the product certs from...
		// etc/pki/product are included in the currently InstalledProducts
		for (ProductCert productCert : productCerts) {
			List<InstalledProduct> installedProductsMatchingProductCertId = InstalledProduct.findAllInstancesWithMatchingFieldFromList("productId", productCert.productId, installedProducts);
			Assert.assertEquals(installedProductsMatchingProductCertId.size(), 1, "The list of Installed Products contains exactly 1 entry with a productId='"+productCert.productId+"' from '"+clienttasks.productCertDir+"'.");
			InstalledProduct installedProductMatchingProductCertId = installedProductsMatchingProductCertId.get(0);
			Assert.assertEquals(installedProductMatchingProductCertId.productName, productCert.productNamespace.name,"The list of Installed Products includes '"+productCert.productNamespace.name+"' from '"+clienttasks.productCertDir+"'.");
			Assert.assertEquals(installedProductMatchingProductCertId.version, productCert.productNamespace.version,"The list of Installed Products includes '"+productCert.productNamespace.name+"' version '"+productCert.productNamespace.version+"' from '"+clienttasks.productCertDir+"'.");
		}
		// etc/pki/product-default are included in the currently InstalledProducts (unless it's productId is already installed in /etc/pki/product which takes precedence over /etc/pki/product-default).
		for (ProductCert defaultProductCert : defaultProductCerts) {
			List<InstalledProduct> installedProductsMatchingProductCertId = InstalledProduct.findAllInstancesWithMatchingFieldFromList("productId", defaultProductCert.productId, installedProducts);
			Assert.assertEquals(installedProductsMatchingProductCertId.size(), 1, "The list of Installed Products contains exactly 1 entry with a productId='"+defaultProductCert.productId+"' from '"+clienttasks.productCertDefaultDir+"'.");
			InstalledProduct installedProductMatchingProductCertId = installedProductsMatchingProductCertId.get(0);
			ProductCert precedentProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", defaultProductCert.productId, productCerts);
			if (precedentProductCert!=null) {
				// verify the precedentProductCert overrides the defaultProductCert
				Assert.assertEquals(installedProductMatchingProductCertId.productName, precedentProductCert.productNamespace.name,"The list of Installed Products includes '"+precedentProductCert.productNamespace.name+"' from '"+clienttasks.productCertDir+"' since it takes precedence over '"+defaultProductCert+"' from '"+clienttasks.productCertDefaultDir+"'.");
				Assert.assertEquals(installedProductMatchingProductCertId.version, precedentProductCert.productNamespace.version,"The list of Installed Products includes '"+precedentProductCert.productNamespace.name+"' version '"+precedentProductCert.productNamespace.version+"' from '"+clienttasks.productCertDir+"' since it takes precedence over '"+defaultProductCert+"' from '"+clienttasks.productCertDefaultDir+"'.");	
				Assert.assertEquals(installedProductMatchingProductCertId.arch, precedentProductCert.productNamespace.arch,"The list of Installed Products includes '"+precedentProductCert.productNamespace.name+"' arch '"+precedentProductCert.productNamespace.arch+"' from '"+clienttasks.productCertDir+"' since it takes precedence over '"+defaultProductCert+"' from '"+clienttasks.productCertDefaultDir+"'.");
			} else {
				// verify that the defaultProductCert is included in list of Installed Products
				Assert.assertEquals(installedProductMatchingProductCertId.productName, defaultProductCert.productNamespace.name,"The list of Installed Products includes '"+defaultProductCert.productNamespace.name+"' from '"+clienttasks.productCertDefaultDir+"' since there is no product cert with ID '"+defaultProductCert.productId+"' in '"+clienttasks.productCertDir+"' that takes precedence.");
				Assert.assertEquals(installedProductMatchingProductCertId.version, defaultProductCert.productNamespace.version,"The list of Installed Products includes '"+defaultProductCert.productNamespace.name+"' version '"+defaultProductCert.productNamespace.version+"' from '"+clienttasks.productCertDefaultDir+"' since there is no product cert with ID '"+defaultProductCert.productId+"' in '"+clienttasks.productCertDir+"' that takes precedence.");
				Assert.assertEquals(installedProductMatchingProductCertId.arch, defaultProductCert.productNamespace.arch,"The list of Installed Products includes '"+defaultProductCert.productNamespace.name+"' arch '"+defaultProductCert.productNamespace.arch+"' from '"+clienttasks.productCertDefaultDir+"' since there is no product cert with ID '"+defaultProductCert.productId+"' in '"+clienttasks.productCertDir+"' that takes precedence.");
			}
		}
	}
	@AfterClass(groups={"setup"})	// needed since @AfterGroups will skip when some of the tests within the group are skipped even when alwaysRun=true
	@AfterGroups(groups={"setup"},value="ListInstalledWithProductDefault_Test",alwaysRun=true /* does not seem to have any effect */)
	public void afterListInstalledWithProductDefault_Test() throws JSONException, Exception {
		// remove product certs that were added to /etc/pki/product-default
		if (originalDefaultProductCerts!=null) {
			/* TOO MUCH LOGGING
			for (ProductCert productCert : clienttasks.getProductCerts(clienttasks.productCertDefaultDir)) {
				if (!originalDefaultProductCerts.contains(productCert)) {
					client.runCommandAndWait("rm -rf "+productCert.file);
				}
			}
			*/
			String productCertFilesAsString = "";
			for (ProductCert productCert : clienttasks.getProductCerts(clienttasks.productCertDefaultDir)) {
				if (!originalDefaultProductCerts.contains(productCert)) {
					productCertFilesAsString += productCert.file.getPath() + " ";
				}
			}
			if (!productCertFilesAsString.isEmpty()) client.runCommandAndWait("rm -rf "+productCertFilesAsString);
		}
	}
	protected List<ProductCert> originalDefaultProductCerts = null;
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 709412 - subscription manager cli uses product name comparisons in the list command https://github.com/RedHatQE/rhsm-qe/issues/166
	// TODO Bug 710141 - OwnerInfo needs to only show info for pools that are active right now, for all the stats https://github.com/RedHatQE/rhsm-qe/issues/167
	// TODO Bug 734880 - subscription-manager list --installed reports differently between LEGACY vs NEW SKU subscriptions  (Note: Bryan says that this had nothing to do with Legacy vs Non Legacy - it was simply a regression in bundled products when stacking was introduced) https://github.com/RedHatQE/rhsm-qe/issues/168
	// TODO Bug 803386 - display product ID in product details pane on sm-gui and cli https://github.com/RedHatQE/rhsm-qe/issues/169
	// TODO Bug 805415 - s390x Partially Subscribed  (THIS IS EFFECTIVELY A COMPLIANCE TEST ON A 0 SOCKET SUBSCRIPTION/PRODUCT) https://github.com/RedHatQE/rhsm-qe/issues/170
	
	
	// Configuration methods ***********************************************************************
	
	
	
	@BeforeClass(groups="setup")
	public void registerBeforeClass() {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
	}
	
	// NOTE: This method is not necessary, but adds a little more spice to ListAvailableWithFutureOnDate_Test
/*debugTest*/	@BeforeClass(groups="setup", dependsOnMethods="registerBeforeClass")
	public void createFutureSubscriptionPoolBeforeClass() throws Exception {
		// don't bother attempting to create a subscription unless onPremises
		//if (!sm_serverType.equals(CandlepinType.standalone)) return;
		if (server==null) {
			log.warning("Skipping createFutureSubscriptionPoolBeforeClass() when server is null.");
			return;	
		}
		
		// find a randomly available product id
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) {
			log.warning("Skipping createFutureSubscriptionPoolBeforeClass() when no pools are available.");
			return;		
		}
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		String randomAvailableProductId = pool.productId;
		
		// create a future subscription and refresh pools for it
		JSONObject futureJSONPool = CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, 15, 5/*years*/*365*24*60, 6/*years*/*365*24*60, getRandInt(), getRandInt(), randomAvailableProductId, null, null);
	}
	
	
/*debugTest*/	@BeforeClass(groups="setup")
	public void createSubscriptionsWithVariationsOnProductAttributeSockets() throws JSONException, Exception {
		String name,productId, resourcePath;
		List<String> providedProductIds = new ArrayList<String>();
		Map<String,String> attributes = new HashMap<String,String>();
		JSONObject jsonEngProduct, jsonMktProduct, jsonSubscription;
		if (server==null) {
			log.warning("Skipping createSubscriptionsWithVariationsOnProductAttributeSockets() when server is null.");
			return;	
		}
	
		// Awesome OS for 0 sockets
		name = "Awesome OS for systems with sockets value=0";
		productId = "0-sockets";
		providedProductIds.clear();
		providedProductIds.add("90001");
		attributes.clear();
		attributes.put("sockets", "0");
		attributes.put("version", "1.0");
		attributes.put("variant", "server");
		attributes.put("arch", "ALL");
		attributes.put("warning_period", "30");
		// delete already existing subscription and products
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, productId);
		resourcePath = "/products/"+productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		resourcePath = "/products/"+providedProductIds.get(0);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		// create a new engineering product, marketing product that provides the engineering product, and a subscription for the marketing product
		attributes.put("type", "SVC");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, name+" BITS", providedProductIds.get(0), 1, attributes, null);
		attributes.put("type", "MKT");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, name, productId, 1, attributes, null);
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, 20, -1*24*60/*1 day ago*/, 15*24*60/*15 days from now*/, getRandInt(), getRandInt(), productId, providedProductIds, null);

	
		// Awesome OS for no sockets
		name = "Awesome OS for systems with no sockets";
		productId = "no-sockets";
		providedProductIds.clear();
		providedProductIds.add("90002");
		attributes.clear();
		attributes.remove("sockets");
		attributes.put("version", "0.0");
		attributes.put("variant", "workstation");
		attributes.put("arch", "ALL");
		attributes.put("warning_period", "30");
		// delete already existing subscription and products
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, productId);
		resourcePath = "/products/"+productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		resourcePath = "/products/"+providedProductIds.get(0);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		// create a new engineering product, marketing product that provides the engineering product, and a subscription for the marketing product
		attributes.put("type", "SVC");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, name+" BITS", providedProductIds.get(0), 1, attributes, null);
		attributes.put("type", "MKT");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, name, productId, 1, attributes, null);
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, 20, -1*24*60/*1 day ago*/, 15*24*60/*15 days from now*/, getRandInt(), getRandInt(), productId, providedProductIds, null);
		
		
		// Awesome OS for "zero" sockets
		name = "Awesome OS for systems with sockets value=\"zero\"";
		productId = "zero-sockets";
		providedProductIds.clear();
		providedProductIds.add("90003");
		attributes.clear();
		attributes.put("sockets", "zero");
		attributes.put("version", "0.0");
		attributes.put("variant", "workstation");
		attributes.put("arch", "ALL");
		attributes.put("warning_period", "30");
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "795552";	// Bug 795552 - invalid literal for int() with base 10: 'null'
		boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping the creation of product: "+name);
		} else {
		// END OF WORKAROUND
		// TEMPORARY WORKAROUND FOR BUG
		bugId = "858286";	// Bug 858286 - Runtime Error For input string: "zero" at java.lang.NumberFormatException.forInputString:65
		invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		BUG_858286:	if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping the creation of product: "+name);
		} else {
		// END OF WORKAROUND
		// delete already existing subscription and products
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, productId);
		resourcePath = "/products/"+productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		resourcePath = "/products/"+providedProductIds.get(0);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		// create a new engineering product, marketing product that provides the engineering product, and a subscription for the marketing product
		attributes.put("type", "SVC");
		
		// after the fix for bug 858286, the sockets attribute value MUST be a non-negative attribute	// 1/25/2013 TODO move this block of code to create 'Awesome OS for "zero" sockets' to its own testcase
		try {
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, name+" BITS", providedProductIds.get(0), 1, attributes, null);
		} catch (java.lang.AssertionError ae) {
			String expectedProductCreationErrorMsg = "The attribute 'sockets' must be an integer value.";
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.24-1")) {	// candlepin commit 92957a3f9eabb699832541031a5ebd15334886ca	// 1416825: Abstracted out property validation to new validator framework
				expectedProductCreationErrorMsg="The attribute \"sockets\" must be a positive, integer value";
			}
			Assert.assertTrue(ae.getMessage().contains(expectedProductCreationErrorMsg), ae.getMessage()+" contains expected product creation failure message '"+expectedProductCreationErrorMsg+"'.");
			break BUG_858286; 
		}
				
		attributes.put("type", "MKT");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, name, productId, 1, attributes, null);
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, 20, -1*24*60/*1 day ago*/, 15*24*60/*15 days from now*/, getRandInt(), getRandInt(), productId, providedProductIds, null);
		}}
		
		
		// Awesome OS for null sockets
		name = "Awesome OS for systems with sockets value=null";
		productId = "null-sockets";
		providedProductIds.clear();
		providedProductIds.add("90004");
		attributes.clear();
		attributes.put("sockets", null);
		attributes.put("version", "0.0");
		attributes.put("variant", "server");
		attributes.put("arch", "ALL");
		attributes.put("warning_period", "30");
		// TEMPORARY WORKAROUND FOR BUG
		bugId = "807452";	// Bug 807452 - refresh pools FAILS WITH: org.quartz.SchedulerException: Job threw an unhandled exception. [See nested exception: org.mozilla.javascript.WrappedException: Wrapped java.lang.NullPointerException (rules#737)]
		invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping the creation of product: "+name);
		} else {
		// END OF WORKAROUND
		// TEMPORARY WORKAROUND FOR BUG
		bugId = "813529";	// Bug 813529 - refresh pools FAILS WITH: org.quartz.SchedulerException: Job threw an unhandled exception. [See nested exception: org.mozilla.javascript.WrappedException: Wrapped java.lang.NullPointerException (rules#846)]
		invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping the creation of product: "+name);
		} else {
		// delete already existing subscription and products
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, productId);
		resourcePath = "/products/"+productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		resourcePath = "/products/"+providedProductIds.get(0);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) resourcePath = "/owners/"+sm_clientOrg+resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath);
		// create a new engineering product, marketing product that provides the engineering product, and a subscription for the marketing product
		attributes.put("type", "SVC");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, name+" BITS", providedProductIds.get(0), 1, attributes, null);
		attributes.put("type", "MKT");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, name, productId, 1, attributes, null);
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, 20, -1*24*60/*1 day ago*/, 15*24*60/*15 days from now*/, getRandInt(), getRandInt(), productId, providedProductIds, null);
		}}
		
		// TODO: To get the product certs, use the CandlepinTasks REST API:
        //"url": "/products/{product_uuid}/certificate", 
        //"GET"
	}	
	
	
	
	
	@BeforeGroups(groups="setup",value="unsubscribeBeforeGroup")
	public void unsubscribeBeforeGroup() {
		//clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
	}
	
	
	
	
	
	// Data Providers ***********************************************************************
	
	
	
	@DataProvider(name="getListAvailableWithServicelevelData")
	public Object[][] getListAvailableWithServicelevelDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getListAvailableWithServicelevelDataAsListOfLists());
	}
	protected List<List<Object>>getListAvailableWithServicelevelDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// register with force (so we can find the org to which the sm_clientUsername belongs in case sm_clientOrg is null)
		String org = sm_clientOrg;
		if (org==null) {
			String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, "SubscriptionServicelevelConsumer", null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null));
			org = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername,sm_clientPassword,sm_serverUrl,consumerId);
		}
		
		// get all the valid service levels available to this org	
		for (String serviceLevel : CandlepinTasks.getServiceLevelsForOrgKey(sm_clientUsername, sm_clientPassword, sm_serverUrl, org)) {
			BlockedByBzBug blockedByBug=null;
			
			// Bug 1011234 - subscription-manager list --avail should catch nil service-levels and report the absense of a service-level rather than "None"
			if (serviceLevel.equalsIgnoreCase("none")) blockedByBug=new BlockedByBzBug("1011234");
			
			ll.add(Arrays.asList(new Object[] {blockedByBug,	serviceLevel}));
			ll.add(Arrays.asList(new Object[] {blockedByBug,	randomizeCaseOfCharactersInString(serviceLevel)}));	// run again with the serviceLevel case randomized
		}
		ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("1011234"),	""}));
		ll.add(Arrays.asList(new Object[] {null,							"FOO"}));

		
		return ll;
	}
	
	
	
	@DataProvider(name="getConsumedWithServicelevelData")
	public Object[][] getConsumedWithServicelevelDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getConsumedWithServicelevelDataAsListOfLists());
	}
	protected List<List<Object>>getConsumedWithServicelevelDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// register with force and subscribe to all available subscriptions collectively
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, "SubscriptionServicelevelConsumer", null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null));
		String org = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername,sm_clientPassword,sm_serverUrl,consumerId);
		clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();
		
		// get all the valid service levels available to this org	
		for (String serviceLevel : CandlepinTasks.getServiceLevelsForOrgKey(sm_clientUsername, sm_clientPassword, sm_serverUrl, org)) {
			Object bugzilla = null;
			if (serviceLevel.equals("None")) bugzilla = new BlockedByBzBug(new String[]{"842170","976924"});
			ll.add(Arrays.asList(new Object[] {bugzilla,	serviceLevel}));
		}
		ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"842170","976924","1156627"}),	""}));
		ll.add(Arrays.asList(new Object[] {null, "FOO"}));

		
		return ll;
	}
}
