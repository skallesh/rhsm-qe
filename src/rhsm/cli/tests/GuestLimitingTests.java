package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.LinkedItem;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.redhat.qe.Assert;
import com.redhat.qe.tools.RemoteFileTasks;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ProductCert;
import rhsm.data.SubscriptionPool;

/**
 * @author skallesh
 *
 *
 *
 *         References: Bug 995161 - [RFE] Subscription Manager should report a
 *         machine as "yellow" if it has more guests then the subscriptions
 *         allow. Design Doc: http://www.candlepinproject.org/docs/candlepin/
 *         virt_guest_limit_design.html
 */

@Test(groups = {"GuestLimitingTests"})
public class GuestLimitingTests extends SubscriptionManagerCLITestScript {
	protected String ownerKey = "";
	protected List<String> providedProductIds = new ArrayList<String>();
	protected String factname = "system.entitlements_valid";
	public static final String factValueForSystemCompliance = "valid";
	public static final String factValueForSystemNonCompliance = "invalid";
	public static final String factValueForSystemPartialCompliance = "partial";
	protected String randomAvailableProductId = null;
	protected List<String> providedProduct = new ArrayList<String>();

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37723", "RHEL7-51950"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-30331",	// RHSM-REQ : Guest-limited Subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84959",	// RHSM-REQ : Guest-limited Subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "Verify the compliance status on the server when the host has more than 2 guests",
			groups = {"Tier2Tests"},
			enabled = true)
	public void testComplianceOfHostWithtwoGuestsAndGuestLimitOfFour() throws JSONException, Exception {
		String consumerId = clienttasks.getCurrentConsumerId(
				clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null,
						null, null, (String) null, null, null, null, true, null, null, null, null, null));
		if (clienttasks.getFactValue("virt.is_guest").equals("True")) {
			Map<String, String> factsMap = new HashMap<String, String>();
			factsMap.put("virt.is_guest", "False");
			factsMap.put("virt.uuid", "");
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null, null);
		}
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl,
				consumerId);
		// call Candlepin API to PUT some guestIds onto the host consumer
		JSONObject jsonData = new JSONObject();
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.clear();
		attributes.put("virtWhoType", "libvirt");
		attributes.put("active", "1");
		int guestLimit = 4;
		List<JSONObject> expectedGuestIds = new ArrayList<JSONObject>();
		for (int k = 0; k <= guestLimit - 1; k++) {
			expectedGuestIds.add(createGuestIdRequestBody("test-guestId" + k, attributes));
		}
		jsonData.put("guestIds", expectedGuestIds);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,
				"/consumers/" + consumerId, jsonData);
		String pool = getGuestlimitPool(String.valueOf(guestLimit));
		ProductCert installedProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId",
				providedProductIds.get(randomGenerator.nextInt(providedProductIds.size())),
				clienttasks.getCurrentProductCerts());
		Assert.assertNotNull(installedProductCert, "Found installed product cert needed for this test.");
		clienttasks.subscribe(null, null, pool, null, null, "1", null, null, null, null, null, null, null);
		String compliance = clienttasks.getFactValue(factname);
		// Assert the system compliance
		Assert.assertEquals(compliance, factValueForSystemCompliance);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37721", "RHEL7-51948"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-30331",	// RHSM-REQ : Guest-limited Subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84959",	// RHSM-REQ : Guest-limited Subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "Verify the compliance status on the server when the host has more than 4 guests",
			groups = {"Tier2Tests"},
			enabled = true)
	public void testComplianceOfHostWithFiveGuestsAndGuestLimitOfFour() throws JSONException, Exception {

		String consumerId = clienttasks.getCurrentConsumerId(
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null,
						null, null, (String) null, null, null, null, true, null, null, null, null, null));
		clienttasks.autoheal(null, null, true, null, null, null, null);

		if (clienttasks.getFactValue("virt.is_guest").equals("True")) {
			Map<String, String> factsMap = new HashMap<String, String>();
			factsMap.put("virt.is_guest", "False");
			factsMap.put("virt.uuid", "");
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null, null);
		}
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl,
				consumerId);
		// call Candlepin API to PUT some guestIds onto the host consumer
		JSONObject jsonData = new JSONObject();
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.clear();
		attributes.put("virtWhoType", "libvirt");
		attributes.put("active", "1");
		int guestLimit = 5;
		List<JSONObject> expectedGuestIds = new ArrayList<JSONObject>();
		for (int k = 0; k <= guestLimit; k++) {
			expectedGuestIds.add(createGuestIdRequestBody("test-guestId" + k, attributes));
		}
		jsonData.put("guestIds", expectedGuestIds);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,
				"/consumers/" + consumerId, jsonData);
		String pool = getGuestlimitPool(String.valueOf(guestLimit - 1));
		ProductCert installedProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId",
				providedProductIds.get(randomGenerator.nextInt(providedProductIds.size())),
				clienttasks.getCurrentProductCerts());
		Assert.assertNotNull(installedProductCert, "Found installed product cert needed for this test.");
		clienttasks.subscribe(null, null, pool, null, null, "1", null, null, null, null, null, null, null);
		String compliance = clienttasks.getFactValue(factname);
		// Assert the system compliance
		Assert.assertEquals(compliance, factValueForSystemPartialCompliance);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37720", "RHEL7-51537"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-30331",	// RHSM-REQ : Guest-limited Subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84959",	// RHSM-REQ : Guest-limited Subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "Verify that the guest_limit attribute is global across guest-limit subscriptions",
			groups = {"Tier2Tests","blockedByBug-1555582"},
			enabled = true)
	public void testGuestLimitIsGlobal() throws JSONException, Exception {
		String consumerId = clienttasks.getCurrentConsumerId(
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null,
						null, null, (String) null, null, null, null, true, null, null, null, null, null));
		clienttasks.autoheal(null, null, true, null, null, null, null);

		if (clienttasks.getFactValue("virt.is_guest").equals("True")) {
			Map<String, String> factsMap = new HashMap<String, String>();
			factsMap.put("virt.is_guest", "False");
			factsMap.put("virt.uuid", "");
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null, null);
		}
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl,
				consumerId);
		// call Candlepin API to PUT some guestIds onto the host consumer
		JSONObject jsonData = new JSONObject();
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.clear();
		attributes.put("virtWhoType", "libvirt");
		attributes.put("active", "1");
		int guestLimit = 5;
		List<JSONObject> expectedGuestIds = new ArrayList<JSONObject>();
		for (int k = 0; k <= guestLimit; k++) {
			expectedGuestIds.add(createGuestIdRequestBody("test-guestId" + k, attributes));
		}
		jsonData.put("guestIds", expectedGuestIds);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,
				"/consumers/" + consumerId, jsonData);
		String pool = getGuestlimitPool("-1");
		clienttasks.subscribe(null, null, pool, null, null, "1", null, null, null, null, null, null, null);
		String compliance = clienttasks.getFactValue(factname);
		// Assert the system compliance
		Assert.assertEquals(compliance, factValueForSystemCompliance);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37722", "RHEL7-51949"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-30331",	// RHSM-REQ : Guest-limited Subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84959",	// RHSM-REQ : Guest-limited Subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description = "Verify the compliance status on the server when the host has more than 5 guests and one of the guest is reported to be inactive by virt-who",
			groups = {"Tier2Tests"},
			enabled = true)
	public void testComplianceOfHostWithOneOftheGuestReportedInactive() throws JSONException, Exception {
		String consumerId = clienttasks.getCurrentConsumerId(
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null,
						null, null, (String) null, null, null, null, true, null, null, null, null, null));
		clienttasks.autoheal(null, null, true, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null);
		if (clienttasks.getFactValue("virt.is_guest").equals("True")) {
			Map<String, String> factsMap = new HashMap<String, String>();
			factsMap.put("virt.is_guest", "False");
			factsMap.put("virt.uuid", "");
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null, true, null, null, null, null);
		}
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl,
				consumerId);
		// call Candlepin API to PUT some guestIds onto the host consumer
		JSONObject jsonData = new JSONObject();
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.clear();
		attributes.put("virtWhoType", "libvirt");
		attributes.put("active", "1");
		int guestLimit = 4;
		List<JSONObject> expectedGuestIds = new ArrayList<JSONObject>();
		for (int k = 0; k <= guestLimit - 1; k++) {
			if (k == 3) {
				attributes.put("active", "0");
			}
			expectedGuestIds.add(createGuestIdRequestBody("test-guestId" + k, attributes));
		}
		jsonData.put("guestIds", expectedGuestIds);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,
				"/consumers/" + consumerId, jsonData);
		String pool = getGuestlimitPool(String.valueOf(guestLimit));

		ProductCert installedProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId",
				providedProductIds.get(randomGenerator.nextInt(providedProductIds.size())),
				clienttasks.getCurrentProductCerts());
		Assert.assertNotNull(installedProductCert, "Found installed product cert needed for this test.");
		clienttasks.subscribe(null, null, pool, null, null, "1", null, null, null, null, null, null, null);
		String compliance = clienttasks.getFactValue(factname);
		// Assert the system compliance
		Assert.assertEquals(compliance, factValueForSystemCompliance);
	}

	protected String getGuestlimitPool(String guestLimit) throws JSONException, Exception {
		String poolId = null;
		providedProductIds.clear();
		for (SubscriptionPool pool : clienttasks.getAvailableSubscriptionsMatchingInstalledProducts()) {
			String GuestLimitAttribute = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername,
					sm_clientPassword, sm_serverUrl, pool.poolId, "guest_limit");
			System.out.println(GuestLimitAttribute + "   " +guestLimit);
			if ((!(GuestLimitAttribute == null)) && (GuestLimitAttribute.equals(guestLimit))) {
				poolId = pool.poolId;
				providedProductIds = (CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword,
						sm_serverUrl, pool.poolId));

				log.info("Found the following subscription pool with guest_limit '" + guestLimit
						+ "' that provides at least one product: " + pool);
				if (!providedProductIds.isEmpty())
					return poolId;
			}

		}

		if (providedProductIds.isEmpty()) {
			poolId = createTestPool(-60 * 24, 60 * 24 ,guestLimit);
			providedProductIds = (CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword,
					sm_serverUrl, poolId));
		}
		return poolId;

	}

	protected String rhsmProductCertDir = null;
	protected final String tmpProductCertDir = "/tmp/sm-tmpProductCertDir-guestlimittests";
	
	@BeforeClass(groups = "setup")
	protected void tmpProductCertDirWithInstalledProductCertsConfiguration() {
		if (rhsmProductCertDir == null) {
			rhsmProductCertDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "productCertDir");
			Assert.assertNotNull(rhsmProductCertDir);
		}
		log.info(
				"Initializing a new product cert directory with the currently installed product certs for this test...");
		ProductCert installedProductCert37060 = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId",
				"37060", clienttasks.getCurrentProductCerts());
		RemoteFileTasks.runCommandAndAssert(client, "mkdir -p " + tmpProductCertDir, Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client, "rm -f " + tmpProductCertDir + "/*.pem", Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client, "cp " + installedProductCert37060.file + " " + tmpProductCertDir,
					Integer.valueOf(0));
		

		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", tmpProductCertDir);
	}

	@AfterClass(groups = "setup", alwaysRun=true) // called after class for insurance
	public void restoreRhsmProductCertDir() {
		if (clienttasks == null)
			return;
		if (rhsmProductCertDir == null)
			return;
		log.info("Restoring the originally configured product cert directory...");
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", rhsmProductCertDir);
	}

	
	public static JSONObject createGuestIdRequestBody(String guestId, Map<String, String> attributes)
			throws JSONException {
		JSONObject jsonGuestData = new JSONObject();

		if (guestId != null)
			jsonGuestData.put("guestId", guestId);

		Map<String, String> jsonAttribute = new HashMap<String, String>();
		for (String attributeName : attributes.keySet()) {
			if (attributeName.equals("virtWhoType")) {
				jsonAttribute.put("virtWhoType", attributes.get(attributeName));
			}
			if (attributeName.equals("active")) {
				jsonAttribute.put("active", attributes.get(attributeName));
			}

			jsonGuestData.put("attributes", jsonAttribute);
		}
		return jsonGuestData;
	}

	// THE FOLLOWING BEFORE AND AFTER CLASS METHODS ARE USED TO ELIMINATE
	// THE INFLUENCE THAT /etc/pki/product-default/ CERTS HAVE ON THESE TESTS
	// SINCE THESE TESTS PRE-DATE THE INTRODUCTION OF DEFAULT PRODUCT CERTS.
	@BeforeClass(groups = "setup")
	public void backupProductDefaultCerts() {
		log.info(
				"This test class was developed before the addition of /etc/pki/product-default/ certs (Bug 1123029).  Therefore, let's back them up before running this test class.");
		for (File productCertFile : clienttasks.getCurrentProductCertFiles()) {
			if (productCertFile.getPath().startsWith(clienttasks.productCertDefaultDir)) {
				client.runCommandAndWait("mv " + productCertFile + " " + productCertFile + ".bak");
			}
		}
	}

	/**
	 * @param startingMinutesFromNow
	 * @param endingMinutesFromNow
	 * @return poolId to the newly available SubscriptionPool
	 * @throws JSONException
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	protected String createTestPool(int startingMinutesFromNow, int endingMinutesFromNow,String guest_limit)
			throws JSONException, Exception {
	    String name = "Guest_limit_GlobalTestProduct";
	    String productId ="Guest_limit_GlobalProduct";
	    if(!(guest_limit.equals("4"))){
		 name = "Guest_limit_TestProduct";
		 productId = "Guest_limit_Product";
	    }
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.clear();
		attributes.put("version", "1.0");
		attributes.put("variant", "server");
		attributes.put("arch", "ALL");
		attributes.put("warning_period", "30");
		attributes.put("type", "MKT");
		attributes.put("type", "SVC");
		attributes.put("guest_limit", guest_limit);
		providedProduct.clear();
		providedProduct.add("37060");
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, productId);
		String resourcePath = "/products/" + productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				sm_clientOrg, name + " BITS", productId, 1, attributes, null);
		return CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, ownerKey, 20, startingMinutesFromNow, endingMinutesFromNow,
				getRandInt(), getRandInt(), productId, providedProduct, null).getString("id");
	}

	
	@BeforeClass(groups = "setup")
	public void getRhsmProductCertDir() {
		rhsmProductCertDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "productCertDir");
		Assert.assertNotNull(rhsmProductCertDir);
	}

	@AfterClass(groups = "setup", alwaysRun=true)
	public void restoreProductDefaultCerts() {
		client.runCommandAndWait("ls -1 " + clienttasks.productCertDefaultDir + "/*.bak");
		String lsBakFiles = client.getStdout().trim();
		if (!lsBakFiles.isEmpty()) {
			for (String lsFile : Arrays.asList(lsBakFiles.split("\n"))) {
				client.runCommandAndWait("mv " + lsFile + " " + lsFile.replaceFirst("\\.bak$", ""));
			}
		}
	}
	
	@AfterClass(groups = "setup", alwaysRun=true)
	public void deleteFactsFileWithOverridingValuesAfterClass() {
		// cleanup the facts file created by testcase calls to createFactsFileWithOverridingValues(...)
		clienttasks.deleteFactsFileWithOverridingValues();
	}
}
