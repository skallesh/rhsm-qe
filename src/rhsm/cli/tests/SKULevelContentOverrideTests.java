package rhsm.cli.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.Repo;
import rhsm.data.SubscriptionPool;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author skallesh
 *
 *         References: Bug 1179727 - RFE: Support Per-SKU Enabled/Disabled
 *         Content Overrides http://etherpad.corp.redhat.com/jgn9ZrC1uM
 *         https://polarion.engineering.redhat.com/polarion/#/project/
 *         RedHatEnterpriseLinux7/wiki/RHSMQE/RHSM_SKU%20level%20content%
 *         20override?sidebar=approvals
 */
@Test(groups={"SKULevelContentOverrideTests"})
public class SKULevelContentOverrideTests extends SubscriptionManagerCLITestScript {

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37020", "RHEL7-54835"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify content can be overriden at SKU level,content overriden at sku level can be enabled/disabled by using subscription-manager repos --enable/--disable commands and enabled repo is given prefrence over disabled repo",
			groups = {"Tier3Tests","OverrideAtSKULevelTest", "blockedByBug-1403160" },
			dataProvider = "getSubscriptions",
			enabled = true)
	public void testContentOverrideAtSKULevel(Object Bugzilla, SubscriptionPool subscriptionpool) throws JSONException, Exception {
		String resourcePath = null;
		String requestBody = null;
		String consumerId = clienttasks.getCurrentConsumerId(
				clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null,
						null, null, (String) null, null, null, null, true, null, null, null, null, null));
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl,
				consumerId);
		clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null,
				null, null);

		List<Repo> availableRepos = clienttasks.getCurrentlySubscribedRepos();
		List<String> repoIdsDisabledByDefault = new ArrayList<String>();
		List<String> repoIdsEnabledbyDefault = new ArrayList<String>();
		Map<String, String> attributesMap = new HashMap<String, String>();
		String contentIDToEnable = "999999999999999";
		String contentIDToDisable = "00000000000000";

		// remember a list of all the repoIds enable/disabled by default
		// entitled by the subscriptionpool
		for (Repo repo : availableRepos) {
			if (repo.enabled) {
				repoIdsEnabledbyDefault.add(repo.repoId);
			} else {
				repoIdsDisabledByDefault.add(repo.repoId);
			}
		}

		log.info("VERIFICATION 1: Verify that adding a content id for a default disabled"
			+ " repo to a content_override_enabled attribute at the SKU level will make the repo enabled for the consumer"
			+ "To override(enable) a disabled repo");
		if (repoIdsDisabledByDefault.isEmpty()) {
			requestBody = CandlepinTasks.createContentRequestBody("fooname (DisabledByDefault)", contentIDToEnable,
					"foolabel_DisabledByDefault", "yum", "Foo Vendor", "/foo/path", "/foo/path/gpg", null, null, null,
					null).toString();
			resourcePath = "/content";
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
				resourcePath = "/owners/" + ownerKey + resourcePath;
			CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					resourcePath, requestBody);
			String productid = getPoolprovidedProducts(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					subscriptionpool.poolId, "productId");
			CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
					sm_serverUrl, ownerKey, productid, contentIDToEnable, false);
			CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					resourcePath, requestBody);

			clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
			clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null,
					null, null);
			availableRepos = clienttasks.getCurrentlySubscribedRepos();
			for (Repo repo : availableRepos) {
				if (!(repo.enabled)) {
					repoIdsDisabledByDefault.add(repo.repoId);
				}
			}
		}
		String repoIdToEnable = repoIdsDisabledByDefault.get(randomGenerator.nextInt(repoIdsDisabledByDefault.size()));
		String contentIdToEnable = getContent(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				repoIdToEnable);
		resourcePath = "/owners/" + ownerKey + "/products/" + subscriptionpool.productId
				+ "?exclude=id&exclude=name&exclude=multiplier&exclude=productContent&exclude=dependentProductIds&exclude=href&exclude=created&exclude=updated&exclude=attributes.created&exclude=attributes.updated";
		JSONObject jsonPoolToEnable = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, resourcePath));
		resourcePath = "/owners/" + ownerKey + "/products/" + subscriptionpool.productId;
		JSONArray jsonProductAttributesToEnable = jsonPoolToEnable.getJSONArray("attributes");
		JSONObject jsonDataToEnable = new JSONObject();
		attributesMap.clear();
		attributesMap.put("name", "content_override_enabled");
		attributesMap.put("value", contentIdToEnable);
		// WARNING: if jsonProductAttributesToEnable already contains an
		// attribute(s) with the name "content_override_enabled", then duplicate
		// attributes will get PUT on the SKU. That's bad. Candlepin should not
		// allow this, but it does. Avoid this by calling
		// purgeJSONObjectNamesFromJSONArray(...)
		jsonProductAttributesToEnable = purgeJSONObjectNamesFromJSONArray(jsonProductAttributesToEnable,
				"content_override_enabled");
		jsonProductAttributesToEnable.put(attributesMap);
		jsonDataToEnable.put("attributes", jsonProductAttributesToEnable);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath, jsonDataToEnable);
		CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				ownerKey);
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null,
				null, null);
		Assert.assertTrue(
				clienttasks.repos_(null, true, null, (String) null, null, null, null, null, null).getStdout()
						.contains(repoIdToEnable),
				"After subscribing to SKU '" + subscriptionpool.productId + "' pool '" + subscriptionpool.poolId
						+ "' which contains a content_override_enabled for repoId '" + repoIdToEnable + "' (contentid='"
						+ contentIdToEnable + "'), it now appears in the list of enabled subscription-manager repos.");

		log.info("VERIFICATION 2: Verify that the SKU level content_override_enabled"
			+ "repo can be overridden back to disabled at on the consumer level"
			+ " verify repo overridden at sku level can be overridden with subscription-manager repos --disable command");
		 
		clienttasks.repos(null, null, null, null, repoIdToEnable, null, null, null, null);
		Assert.assertTrue(
				clienttasks.repos_(null, null, true, (String) null, null, null, null, null, null).getStdout()
						.contains(repoIdToEnable),
				"After subscribing to SKU '" + subscriptionpool.productId + "' pool '" + subscriptionpool.poolId
						+ "' which contains a content_override_enabled for repoId '" + repoIdToEnable + "' (contentid='"
						+ contentIdToEnable
						+ "'), it can be overriden again (at the consumer level) using subscription-manager repos --disable '"
						+ repoIdToEnable + "'.");

		log.info("VERIFICATION 3: Verify that adding a content id for a default enabled repo to a content_override_disabled "
			+ "attribute at the SKU level will make the repo disabled for the consumerTo override(disable) a enabled repo ");
		if (repoIdsEnabledbyDefault.isEmpty()) {
			requestBody = CandlepinTasks.createContentRequestBody("fooname (EnabledByDefault)", contentIDToDisable,
					"foolabel_EnabledByDefault", "yum", "Foo Vendor", "/foo/path", "/foo/path/gpg", null, null, null,
					null).toString();
			resourcePath = "/content";
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
				resourcePath = "/owners/" + sm_clientOrg + resourcePath;
			CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					resourcePath, requestBody);
			String productid = getPoolprovidedProducts(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					subscriptionpool.poolId, "productId");
			CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
					sm_serverUrl, ownerKey, productid, contentIDToDisable, true);
			CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					resourcePath, requestBody);
			clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
			clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null,
					null, null);
			availableRepos = clienttasks.getCurrentlySubscribedRepos();
			for (Repo repo : availableRepos) {
				if (repo.enabled) {
					repoIdsEnabledbyDefault.add(repo.repoId);

				}
			}

		}
		String repoIdToDisable = repoIdsEnabledbyDefault.get(randomGenerator.nextInt(repoIdsEnabledbyDefault.size()));
		String contentIdToDisable = getContent(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				repoIdToDisable);
		resourcePath = "/owners/" + ownerKey + "/products/" + subscriptionpool.productId
				+ "?exclude=id&exclude=name&exclude=multiplier&exclude=productContent&exclude=dependentProductIds&exclude=href&exclude=created&exclude=updated&exclude=attributes.created&exclude=attributes.updated";
		JSONObject jsonPoolToDisable = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, resourcePath));
		resourcePath = "/owners/" + ownerKey + "/products/" + subscriptionpool.productId;
		JSONArray jsonProductAttributesToDisable = jsonPoolToDisable.getJSONArray("attributes");
		attributesMap.clear();
		attributesMap.put("name", "content_override_disabled");
		attributesMap.put("value", contentIdToDisable);

		// WARNING: if jsonProductAttributesToEnable already contains an
		// attribute(s) with the name "content_override_disabled", then
		// duplicate attributes will get PUT on the SKU. That's bad. Candlepin
		// should not allow this, but it does. Avoid this by calling
		// purgeJSONObjectNamesFromJSONArray(...)
		jsonProductAttributesToDisable = purgeJSONObjectNamesFromJSONArray(jsonProductAttributesToDisable,
				"content_override_disabled");
		jsonProductAttributesToDisable.put(attributesMap);
		JSONObject jsonDataToDisable = new JSONObject();
		jsonDataToDisable.put("attributes", jsonProductAttributesToDisable);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath, jsonDataToDisable);
		CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				ownerKey);
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null,
				null, null);
		Assert.assertTrue(
				clienttasks.repos_(null, null, true, (String) null, null, null, null, null, null).getStdout()
						.contains(repoIdToDisable),
				"After subscribing to SKU '" + subscriptionpool.productId + "' pool '" + subscriptionpool.poolId
						+ "' which contains a content_override_disabled for repoId '" + repoIdToDisable
						+ "' (contentid='" + contentIdToDisable
						+ "'), it now appears in the list of disabled subscription-manager repos.");

		log.info("VERIFICATION 4: Verify that the SKU level content_override_enabled repo can"
			+ " be overridden back to enabled at on the consumer level");
		/*
		 * verify repo overridden at sku level can be overridden with
		 * subscription-manager repos --enable command
		 */
		clienttasks.repos(null, null, null, repoIdToDisable, null, null, null, null, null);
		Assert.assertTrue(
				clienttasks.repos_(null, true, null, (String) null, null, null, null, null, null).getStdout()
						.contains(repoIdToDisable),
				"After subscribing to SKU '" + subscriptionpool.productId + "' pool '" + subscriptionpool.poolId
						+ "' which contains a content_override_disabled for repoId '" + repoIdToDisable
						+ "' (contentid='" + contentIdToDisable
						+ "'), it can be overriden again (at the consumer level) using subscription-manager repos --enable '"
						+ repoIdToDisable + "'.");

		// at this point we have two consumer level repo overrides - get rid of
		// them before proceeding to the next verification...
		clienttasks.repo_override(null, true, (String) null, (String) null, null, null, null, null, null);

		log.info("VERIFICATION 5: Verify that when the same content id exists on both the SKU level content_override_enabled "
			+ "and content_override_disabled list, the enabled value takes precedence");
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null,
				null, null);
		attributesMap.clear();
		attributesMap.put("name", "content_override_disabled");
		attributesMap.put("value", contentIdToDisable);
		// WARNING: if jsonProductAttributesToEnable already contains an
		// attribute(s) with the name "content_override_disabled", then
		// duplicate attributes will get PUT on the SKU. That's bad. Candlepin
		// should not allow this, but it does. Avoid this by calling
		// purgeJSONObjectNamesFromJSONArray(...)
		jsonProductAttributesToDisable = purgeJSONObjectNamesFromJSONArray(jsonProductAttributesToDisable,
				"content_override_disabled");
		jsonProductAttributesToDisable.put(attributesMap);
		JSONObject jsonDataToOverrideDisabledContent = new JSONObject();
		jsonDataToOverrideDisabledContent.put("attributes", jsonProductAttributesToDisable);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath, jsonDataToOverrideDisabledContent);
		CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				ownerKey);
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null,
				null, null);
		Assert.assertTrue(
				clienttasks.repos_(null, null, true, (String) null, null, null, null, null, null).getStdout()
						.contains(repoIdToDisable),
				"After subscribing to SKU '" + subscriptionpool.productId + "' pool '" + subscriptionpool.poolId
						+ "' which contains a content_override_disabled for repoId '" + repoIdToDisable
						+ "' (contentid='" + contentIdToDisable
						+ "'), it now appears in the list of disabled subscription-manager repos.");
		jsonProductAttributesToDisable = purgeJSONObjectNamesFromJSONArray(jsonProductAttributesToDisable,
				"content_override_enabled");
		jsonProductAttributesToDisable.put(attributesMap);
		jsonDataToOverrideDisabledContent.put("attributes", jsonProductAttributesToDisable);
		attributesMap.clear();
		attributesMap.put("name", "content_override_enabled");
		attributesMap.put("value", contentIdToDisable);
		jsonProductAttributesToDisable.put(attributesMap);
		jsonDataToOverrideDisabledContent.put("attributes", jsonProductAttributesToDisable);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath, jsonDataToOverrideDisabledContent);
		CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				ownerKey);
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null,
				null, null);
		Assert.assertTrue(
				clienttasks.repos_(null, true, null, (String) null, null, null, null, null, null).getStdout()
						.contains(repoIdToDisable),
				"After subscribing to SKU '" + subscriptionpool.productId + "' pool '" + subscriptionpool.poolId
						+ "' which contains a content_override_enabed for repoId '" + repoIdToDisable + "' (contentid='"
						+ contentIdToDisable + "'), it now appears in the list of enabled subscription-manager repos.");

		log.info("VERIFICATION 6: Verify that the content_override_enabled and content_override_disabled "
			+ "list can specify a comma delimited string of content ids");
		String RepoId1 = null;
		String RepoId2 = null;
		String contentId1 = null;
		String contentId2 = null;
		if (!(repoIdsDisabledByDefault.isEmpty()) && repoIdsDisabledByDefault.size() >= 1) {
			requestBody = CandlepinTasks.createContentRequestBody("fooname (DisabledByDefault_1)", contentIDToEnable,
					"foolabel_DisabledByDefault_1", "yum", "Foo Vendor", "/foo/path", "/foo/path/gpg", null, null, null,
					null).toString();
			System.out.println("inside verification six if condition");
			resourcePath = "/content";
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
				resourcePath = "/owners/" + ownerKey + resourcePath;
			CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					resourcePath, requestBody);
			String productid = getPoolprovidedProducts(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					subscriptionpool.poolId, "productId");
			CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
					sm_serverUrl, ownerKey, productid, contentIDToEnable, false);
			CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					resourcePath, requestBody);
			requestBody = CandlepinTasks.createContentRequestBody("fooname (DisabledByDefault_2)",
					contentIDToEnable + "1", "foolabel_DisabledByDefault_2", "yum", "Foo Vendor", "/foo/path",
					"/foo/path/gpg", null, null, null, null).toString();
			resourcePath = "/content";
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
				resourcePath = "/owners/" + ownerKey + resourcePath;
			CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					resourcePath, requestBody);
			productid = getPoolprovidedProducts(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					subscriptionpool.poolId, "productId");
			CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
					sm_serverUrl, ownerKey, productid, contentIDToEnable, false);
			CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					resourcePath, requestBody);
			clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
			clienttasks.subscribe(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null,
					null, null);
			availableRepos = clienttasks.getCurrentlySubscribedRepos();
			for (Repo repo : availableRepos) {
				if (!(repo.enabled)) {
					repoIdsDisabledByDefault.add(repo.repoId);
				}
			}
		}

		RepoId1 = repoIdsDisabledByDefault.get(repoIdsDisabledByDefault.size() - 1);
		RepoId2 = repoIdsDisabledByDefault.get(repoIdsDisabledByDefault.size() - 2);
		contentId1 = getContent(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, RepoId1);
		contentId2 = getContent(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, RepoId2);

		resourcePath = "/owners/" + ownerKey + "/products/" + subscriptionpool.productId
				+ "?exclude=id&exclude=name&exclude=multiplier&exclude=productContent&exclude=dependentProductIds&exclude=href&exclude=created&exclude=updated&exclude=attributes.created&exclude=attributes.updated";

		JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, resourcePath));
		resourcePath = "/owners/" + ownerKey + "/products/" + subscriptionpool.productId;
		JSONArray jsonProductAttributes = jsonPool.getJSONArray("attributes");
		jsonProductAttributes = purgeJSONObjectNamesFromJSONArray(jsonProductAttributesToEnable,
				"content_override_enabled");
		jsonDataToEnable = new JSONObject();

		attributesMap.clear();
		attributesMap.put("name", "content_override_enabled");
		attributesMap.put("value", contentId1 + "," + contentId2);

		// WARNING: if jsonProductAttributesToEnable already contains an //
		// attribute(s) with the name "content_override_enabled", then //
		// duplicate attributes will get PUT on the SKU. That's bad. //
		// Candlepin should not allow this, but it does. Avoid this by //
		// calling purgeJSONObjectNamesFromJSONArray(...)
		jsonProductAttributes.put(attributesMap);
		JSONObject jsonData = new JSONObject();
		jsonData.put("attributes", jsonProductAttributes);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath, jsonData);
		CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				ownerKey);
		String path = "/pools/" + subscriptionpool.poolId + "?include=productAttributes";
		String result = CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, path);
		Assert.assertTrue(result.contains(contentId1) && result.contains(contentId2),
				"After overriding the content at SKU level '" + contentId2 + "' and '" + contentId2
						+ "'are present in the product attribute list of the pool");
	}

	/**
	 * cleanup after running OverrideAtSKULevelTest by removing all of the
	 * content overrides for the SKUs tested above
	 *
	 * @throws JSONException
	 * @throws Exception
	 * @author jsefler
	 */
	@BeforeGroups(groups = "setup", value = { "OverrideAtSKULevelTest" }, enabled = true)
	@AfterGroups(groups = "cleaup", value = { "OverrideAtSKULevelTest" }, enabled = true)
	public void removeContentOverridesFromSubscriptionPoolProductIdsTested() throws JSONException, Exception {
		String ownerKey = clienttasks.getCurrentlyRegisteredOwnerKey();
		for (String subscriptionPoolProductIdTested : subscriptionPoolProductIdsTested) {
			String productIdPath = "/owners/" + ownerKey + "/products/" + subscriptionPoolProductIdTested
					+ "?exclude=id&exclude=name&exclude=multiplier&exclude=productContent&exclude=dependentProductIds&exclude=href&exclude=created&exclude=updated&exclude=attributes.created&exclude=attributes.updated";
			JSONObject jsonProductId = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,
					sm_serverAdminPassword, sm_serverUrl, productIdPath));
			JSONArray jsonProductIdAttributes = jsonProductId.getJSONArray("attributes");
			jsonProductIdAttributes = purgeJSONObjectNamesFromJSONArray(jsonProductIdAttributes,
					"content_override_enabled");
			jsonProductIdAttributes = purgeJSONObjectNamesFromJSONArray(jsonProductIdAttributes,
					"content_override_disabled");
			productIdPath = "/owners/" + ownerKey + "/products/" + subscriptionPoolProductIdTested;
			JSONObject jsonAttributes = new JSONObject();
			jsonAttributes.put("attributes", jsonProductIdAttributes);
			CandlepinTasks.putResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					productIdPath, jsonAttributes);
			CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
					ownerKey);
		}
	}

	Set<String> subscriptionPoolProductIdsTested = new HashSet<String>();

	@SuppressWarnings("unused")
	public String getPoolprovidedProducts(String authenticator, String password, String url, String poolId,
			String attributeName) throws JSONException, Exception {
		String attributeValue = null;

		JSONObject jsonPool = new JSONObject(
				CandlepinTasks.getResourceUsingRESTfulAPI(authenticator, password, url, "/pools/" + poolId));
		JSONArray jsonAttributes = jsonPool.getJSONArray("providedProducts");
		// loop through the attributes of this pool looking for the
		// attributeName attribute
		for (int j = 0; j < jsonAttributes.length(); j++) {
			JSONObject jsonAttribute = (JSONObject) jsonAttributes.get(j);
			attributeValue = jsonAttribute.getString(attributeName);
			break;
		}
		if (attributeValue == null) {
			jsonAttributes = jsonPool.getJSONArray("derivedProvidedProducts");
			for (int j = 0; j < jsonAttributes.length(); j++) {
				JSONObject jsonAttribute = (JSONObject) jsonAttributes.get(j);
				attributeValue = jsonAttribute.getString(attributeName);
				break;
			}
		}
		return attributeValue;

	}

	@DataProvider(name = "getSubscriptions")
	public Object[][] getSubscriptionPoolsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getSubscriptionsPoolsDataAsListOfLists());
	}

	protected List<List<Object>> getSubscriptionsPoolsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (!isSetupBeforeSuiteComplete)
			return ll;
		clienttasks.removeAllCerts(true, true, false);
		clienttasks.register(sm_client1Username, sm_client1Password, sm_client1Org);
		for (SubscriptionPool availableSubscriptionPoolsMatchingInstalled : SubscriptionPool.parse(clienttasks
				.list(null, true, null, null, null, null, null, true, null, null, null, null, null, null, null).getStdout())) {

			SubscriptionPool pool = availableSubscriptionPoolsMatchingInstalled;

			ll.add(Arrays.asList(new Object[] { null, pool }));
			subscriptionPoolProductIdsTested.add(pool.productId); 
			// keep a separate list of SKU level product ids upon which content_override attributes will be added

			break; // will only add one row to the dataProvider. This defeats	
					// the purpose of a dataProvider.

		}
		return ll;
	}

	public static String getContent(String authenticator, String password, String serverurl, String repoids)
			throws JSONException, Exception {
		String contentId = null;
		JSONArray jsonPool = new JSONArray(
				CandlepinTasks.getResourceUsingRESTfulAPI(authenticator, password, serverurl, "/content/"));
		for (int j = 0; j < jsonPool.length(); j++) {
			JSONObject jsonContentAttribute = (JSONObject) jsonPool.get(j);
			if (jsonContentAttribute.getString("label").equals(repoids)) {
				// the actual attribute value is null, return null
				contentId = jsonContentAttribute.getString("id");

				break;
			}
		}

		return contentId;
	}

	/**
	 * Given a JSONArray, return a new JSONArray containing the same JSONObjects
	 * excluding those with a "name" key equal to name.
	 *
	 * @param jsonArray
	 * @param name
	 * @return
	 * @throws JSONException
	 * @author jsefler
	 */
	JSONArray purgeJSONObjectNamesFromJSONArray(JSONArray jsonArray, String name) throws JSONException {
		JSONArray purgedJSONArray = new JSONArray();
		for (int j = 0; j < jsonArray.length(); j++) {
			JSONObject jsonObject = (JSONObject) jsonArray.get(j);
			if (!jsonObject.getString("name").equals(name)) {
				purgedJSONArray.put(jsonObject);
			}
		}
		return purgedJSONArray;
	}
}
