package rhsm.cli.tests;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Test;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.LinkedItem;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.tools.SSHCommandResult;
import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.EntitlementCert;


import rhsm.data.Repo;
import rhsm.data.SubscriptionPool;

/**
 * @author skallesh
 *
 *         References:https://docs.google.com/document/d/1h-CeEV_FEu885JhZNkydzQQsDkAQ-WtZ5cisCxj4Lao/edit?ts=5873bb00
 */

@Test(groups = {"GoldenTicketTests"})
public class GoldenTicketTests extends SubscriptionManagerCLITestScript {
    protected String attributeName = "contentAccessMode";
    protected String attributeValue = "org_environment";
    protected String org = "snowwhite";
    public static String subscriptionPoolProductId =null;
    private String subscriptionPoolId;
    
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47902", "RHEL7-96740"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-47900",	// RHSM-REQ : Org/Environment Level Content Access
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-85127",	// RHSM-REQ : Org/Environment Level Content Access
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
    @Test(	description = "Verify golden ticket entitlement is granted when system is registered to an org that has contentAccessMode set",
			groups = {"Tier3Tests","blockedByBug-1425438"},
			enabled = true)
    public void testGoldenTicketFunctionality() throws Exception {

	CandlepinTasks.setAttributeForOrg(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, org,
		attributeName, attributeValue);

	clienttasks.register(sm_clientUsername, sm_clientPassword, org, null, null, null, null, null, null, null,
		(String) null, null, null, null, true, null, null, null, null, null);
	clienttasks.autoheal(null, null, true, null, null, null, null);
	String ExpectedRepoMsg = "There were no available repositories matching the specified criteria.";

	// verify the if an extra entitlement is granted upon refresh on subscription-manager version lesser than equal to 1.18.9-1

	if (clienttasks.isPackageVersion("subscription-manager", "<=", "1.18.9-1")) {
	    SSHCommandResult repoResult = clienttasks.repos(false, false, true, (String) null, null, null, null, null, null);
	    Assert.assertEquals(repoResult.getStdout().toString().trim(), ExpectedRepoMsg);
	    clienttasks.refresh(null, null, null, null);
	}

	// verify only the extra entitlement cert granted by or/environment is
	// present

	List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
	Assert.assertTrue(entitlementCerts.size() == 1,
		"Only extra entitlement granted by the org/environment is present");

	// verify repos --list lists all the repos but none of them are enabled when the system has golden ticket certificate access
	SSHCommandResult resultListEnabled = clienttasks.repos(false, true, false, (String) null, null, null, null,
		null, null);
	Assert.assertEquals(resultListEnabled.getStdout().toString().trim(), ExpectedRepoMsg);
	SSHCommandResult resultListDisabled = clienttasks.repos(false, false, true, (String) null, null, null, null,
		null, null);
	Assert.assertNotEquals(resultListDisabled.getStdout().toString().trim(), ExpectedRepoMsg);

	// Verify status cmd message on a system that has golden ticket
	// Todo add assert for golden ticket mode note once fixed
	SSHCommandResult statusResult = clienttasks.status(null, null, null, null, null);
	String expectedStatus = "Overall Status: Invalid";
	Assert.assertTrue(statusResult.getStdout().contains(expectedStatus), "Expecting '" + expectedStatus
		+ "The status of machine is still invalid despite having golden ticket entitlement");

	// verify list --consumed displays the goldenticket entitlement
	SSHCommandResult listConsumedResult = clienttasks.list(null, null, true, null, null, null, null, null, null,
		null, null, null, null, null);
	String expectedMessageForListConsumed = "No consumed subscription pools to list";
	if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.20.2-1")) {	// commit da72dfcbbb2c3a44393edb9e46e1583d05cc140a
		expectedMessageForListConsumed="No consumed subscription pools were found.";
	}
	
	// TEMPORARY WORKAROUND FOR BUG
	String bugId="1425438"; // Bug 1425438 - subscription-manager list --consumed shows the consumption of extra entitlement granted from the organization or environment.
	boolean invokeWorkaroundWhileBugIsOpen = true;
	try {if (invokeWorkaroundWhileBugIsOpen&& BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
	if (invokeWorkaroundWhileBugIsOpen) {
	    log.warning("Skipping assertion: "+"subscription-manager list --consumed does not list golden ticket entitlement");
	} else
	    // END OF WORKAROUND

	    Assert.assertTrue(listConsumedResult.getStdout().trim().equals(expectedMessageForListConsumed),
		    "Expecting '" + expectedMessageForListConsumed
		    + "subscription-manager list --consumed doesnot list golden ticket entitlement");

	// verify after manually deleting the certs from /etc/pki/entitlement dir , refresh command regenerates the entitlement

	clienttasks.removeAllCerts(false, true, false);
	Assert.assertTrue(clienttasks.getCurrentEntitlementCerts().size() == 0,
		"Golden ticket cert is successfully removed");
	clienttasks.refresh(null, null, null, null);
	Assert.assertTrue(clienttasks.getCurrentEntitlementCerts().size() == 1,
		"Golden ticket regenerated successfully");
	resultListDisabled = clienttasks.repos(false, false, true, (String) null, null, null, null, null, null);
	Assert.assertNotEquals(resultListDisabled.getStdout().toString().trim(), ExpectedRepoMsg);

	// Verify remove --all command doesnot remove the golden ticket entitlement along with other subscriptions

	clienttasks.subscribe(true, null, null, (String) null, null, null, null, null, null, null, null, null, null);
	Assert.assertTrue(clienttasks.getCurrentEntitlementCerts().size() > 1,
		"There are more subscriptions attached other than the golden ticket");
	SSHCommandResult AutoAttachlistConsumedResult = clienttasks.list(null, null, true, null, null, null, null, null,
		null, null, null, null, null, null);

	Assert.assertFalse(AutoAttachlistConsumedResult.getStdout().trim().equals(expectedMessageForListConsumed),
		"Expecting'" + expectedMessageForListConsumed
		+ "subscription-manager list --consumed lists the subscriptions consumed after auto-attach command is successful");

	clienttasks.unsubscribe_(true, null, (String) null, null, null, null, null);

	List<EntitlementCert> entitlementCertsAfterRemoveAll = clienttasks.getCurrentEntitlementCerts();
	Assert.assertTrue(entitlementCertsAfterRemoveAll.size() == 1,
		"Only extra entitlement granted by the org/environment is present");

	// verify removing the golden ticekt entitlement by subscription-manager remove --serial <serial_id> fails to remove the golden ticket entitlement

	List<EntitlementCert> entitlementCertsToRemove = EntitlementCert.findAllInstancesWithMatchingFieldFromList(
		"poolId", "Not Available", clienttasks.getCurrentEntitlementCerts());
	for (EntitlementCert entitlementCert : entitlementCertsToRemove) {

	    SSHCommandResult removeResult = clienttasks.unsubscribe_(null, entitlementCert.serialNumber, null, null,
		    null, null, null);
	    String ExpectedMessageForRemove = "The entitlement server failed to remove these serial numbers:" + "\n";
	    ExpectedMessageForRemove += "   " + entitlementCert.serialNumber;
	    Assert.assertEquals(removeResult.getStdout().trim(), ExpectedMessageForRemove);
	}

	clienttasks.repos(false, false, false, "*", null, null, null, null, null);
	resultListEnabled = clienttasks.repos(false, true, false, (String) null, null, null, null, null, null);
	Assert.assertNotEquals(resultListEnabled.getStdout().toString().trim(), ExpectedRepoMsg);

    }




	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47903", "RHEL7-96741"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-47900",	// RHSM-REQ : Org/Environment Level Content Access
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-85127",	// RHSM-REQ : Org/Environment Level Content Access
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify golden ticket entitlement is granted when system is registered using an activationkey belonging to an org that has a contentAccessMode set",
			groups = {"Tier3Tests","testGoldenTicketIsGrantedWhenRegisteredUsingActivationKey"},
			enabled = true)
    public void testGoldenTicketIsGrantedWhenRegisteredUsingActivationKey() throws JSONException, Exception {

	// verify registering the system to activation key belonging to owner(contentAccessmode set) with auto-attach false has access to golden ticket
	CandlepinTasks.setAttributeForOrg(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, org,
		attributeName, attributeValue);
	String activationKeyName = String.format("%s_%s-ActivationKey%s", sm_clientUsername, sm_clientOrg,
		System.currentTimeMillis());
	Map<String, String> mapActivationKeyRequest = new HashMap<String, String>();
	mapActivationKeyRequest.put("name", activationKeyName);
	mapActivationKeyRequest.put("autoAttach", "false");
	JSONObject jsonActivationKeyRequest = new JSONObject(mapActivationKeyRequest);
	CandlepinTasks.postResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,
		"/owners/" + org + "/activation_keys", jsonActivationKeyRequest.toString());

	clienttasks.register(null, null, org, null, null, null, null, null, null, null, activationKeyName, null, null,
		null, true, null, null, null, null, null);
	// verify only the golden entitlement cert granted by org/environment is present
	List<EntitlementCert> entitlementCertsAfterRegisteringToactivationKeyFalse = clienttasks
		.getCurrentEntitlementCerts();
	Assert.assertTrue(entitlementCertsAfterRegisteringToactivationKeyFalse.size() == 1,
		"Only extra entitlement granted by the org/environment is present");

	// verify registering the system to activation key belonging to owner (contentAccessmode set) with auto-attach true also has access to golden ticket
	String activationKeyNameTrue = String.format("%s_%s-ActivationKey%s", sm_clientUsername, org,
		System.currentTimeMillis());
	Map<String, String> mapActivationKeyTrueRequest = new HashMap<String, String>();
	mapActivationKeyTrueRequest.put("name", activationKeyNameTrue);
	mapActivationKeyTrueRequest.put("autoAttach", "true");
	JSONObject jsonActivationKeyTrueRequest = new JSONObject(mapActivationKeyTrueRequest);
	CandlepinTasks.postResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,
		"/owners/" + org + "/activation_keys", jsonActivationKeyTrueRequest.toString());

	clienttasks.register(null, null, org, null, null, null, null, null, null, null, activationKeyNameTrue, null,
		null, null, true, null, null, null, null, null);

	// verify the golden entitlement cert granted by org/environment is present along with other ent certs granted by auto-attach process
	Assert.assertTrue(clienttasks.getCurrentEntitlementCerts().size() > 1,
		"extra entitlement granted by owner is present along with other entitlements attached by auto-attach");

	List<EntitlementCert> extraEntitlementCerts = EntitlementCert.findAllInstancesWithMatchingFieldFromList(
		"poolId", "Not Available", clienttasks.getCurrentEntitlementCerts());
	// verify the golden entitlement cert granted by org/environment is present
	Assert.assertTrue(extraEntitlementCerts.size() == 1,
		"extra entitlement granted by owner is present along with other entitlements attached by auto-attach");
    }




	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47905", "RHEL7-96743"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-47900",	// RHSM-REQ : Org/Environment Level Content Access
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-85127",	// RHSM-REQ : Org/Environment Level Content Access
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify revoking contentAccessMode set on the owner removes extra entitlement",
			groups = {"Tier3Tests","blockedByBug-1448855","testRevokingContentAccessModeOnOwnerRemovesEntitlement" },
			enabled = true)
    public void testRevokingContentAccessModeOnOwnerRemovesEntitlement() throws Exception {

	CandlepinTasks.setAttributeForOrg(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, org,
		attributeName, attributeValue);
	clienttasks.register(sm_clientUsername, sm_clientPassword, org, null, null, null, null, null, null, null,
		(String) null, null, null, null, true, null, null, null, null, null);
	clienttasks.autoheal(null, null, true, null, null, null, null);
	String ExpectedRepoMsg = "There were no available repositories matching the specified criteria.";
	if (clienttasks.isPackageVersion("subscription-manager", "<=", "1.18.9-1")) {
	    SSHCommandResult repoResult = clienttasks.repos_(false, false, true, (String) null, null, null, null, null, null);
	    Assert.assertEquals(repoResult.getStdout().toString().trim(), ExpectedRepoMsg);
	    clienttasks.refresh(null, null, null, null);

	}

	SSHCommandResult repoResult = clienttasks.repos(false, false, true, (String) null, null, null, null, null, null);
	Assert.assertNotEquals(repoResult.getStdout().toString().trim(), ExpectedRepoMsg);
	// verify only the extra entitlement cert granted by or/environment is present
	List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
	Assert.assertTrue(entitlementCerts.size() == 1,
		"Only extra entitlement granted by the org/environment is present");
	// now revoke the contentAccessMode set on the owner
	CandlepinTasks.setAttributeForOrg(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, org,
		attributeName, "entitlement");
	clienttasks.refresh(null, null, null, null);

	repoResult = clienttasks.repos(false, false, true, (String) null, null, null, null, null, null);
	String ExpectedRepoMsgAfterRevoke = "This system has no repositories available through subscriptions.";
	Assert.assertEquals(repoResult.getStdout().toString().trim(), ExpectedRepoMsgAfterRevoke);
	List<EntitlementCert> entitlementCertsAfterRevoke = clienttasks.getCurrentEntitlementCerts();
	// assert that extra entitlement is now revoked
	Assert.assertTrue(entitlementCertsAfterRevoke.size() == 0,
		"No extra entitlement granted by the org/environment is present");
    }




	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47904", "RHEL7-96742"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-47900",	// RHSM-REQ : Org/Environment Level Content Access
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-85127",	// RHSM-REQ : Org/Environment Level Content Access
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.MEDIUM, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
    @Test(	description = "Verify SKU level contentOverride is given priority over default golden ticket one",
    		groups = {"Tier3Tests","blockedByBug-1427069"},
    		enabled = true)
    public void testRepoOverridePreference() throws Exception {
	List<Repo> availableRepos= new ArrayList<Repo>();
	List<String> repoIdsDisabledByDefault = new ArrayList<String>();
	Map<String, String> attributesMap = new HashMap<String, String>();
	String resourcePath = null;
	String ExpectedRepoMsg = "There were no available repositories matching the specified criteria.";
	CandlepinTasks.setAttributeForOrg(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, org,
		attributeName, attributeValue);
	String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null,
		null, null, (String) null, null, null, null, true, null, null, null, null, null));
	String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl,
		consumerId);
	for (SubscriptionPool availsubscriptions : clienttasks.getAvailableSubscriptionsMatchingInstalledProducts()) {
	    subscriptionPoolProductId=availsubscriptions.productId; 
	    subscriptionPoolId= availsubscriptions.poolId;
	    clienttasks.subscribe(null, null,subscriptionPoolId, null, null, null, null, null, null, null, null, null, null);
	    SSHCommandResult repoResult = clienttasks.repos(true, false, false, (String) null, null, null, null, null, null);
	    if(!(repoResult.getStdout().trim().equals("This system has no repositories available through subscriptions."))){
		    break;
	    }
	}
	//clienttasks.subscribe(null, null,subscriptionPoolId, null, null, null, null, null, null, null, null, null, null);
	//List<Repo> 
	
	availableRepos =  clienttasks.getCurrentlySubscribedRepos();
	SSHCommandResult repoResult = clienttasks.repos(false, false, true, (String) null, null, null, null, null, null);
	Assert.assertNotEquals(repoResult.getStdout().toString().trim(), ExpectedRepoMsg);
	SSHCommandResult enabledResult = clienttasks.repos(false, true, false, (String) null, null, null, null, null, null);
	Assert.assertEquals(enabledResult.getStdout().toString().trim(), ExpectedRepoMsg);

	// remember a list of all the repoIds enable/disabled by default
	// entitled by the subscriptionpool
	for (Repo repo : availableRepos) {
	    if (!(repo.enabled)) {
		repoIdsDisabledByDefault.add(repo.repoId);
	    }
	}
	String repoIdToEnable = repoIdsDisabledByDefault.get(randomGenerator.nextInt(repoIdsDisabledByDefault.size()));
	String contentIdToEnable = getContent(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
		repoIdToEnable);
	
	resourcePath = "/owners/" + ownerKey + "/products/" + subscriptionPoolProductId+ "?exclude=id&exclude=name&exclude=multiplier&exclude=productContent&exclude=dependentProductIds&exclude=href&exclude=created&exclude=updated&exclude=attributes.created&exclude=attributes.updated";
	JSONObject jsonPoolToEnable = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,
		sm_serverAdminPassword, sm_serverUrl, resourcePath));
	resourcePath = "/owners/" + ownerKey + "/products/" + subscriptionPoolProductId;
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
	clienttasks.subscribe(null, null, subscriptionPoolId, null, null, null, null, null, null, null, null,null, null);
	Assert.assertTrue(clienttasks.repos_(null, true, null, (String) null, null, null, null, null, null).getStdout()
		.contains(repoIdToEnable),"After subscribing to SKU '" + subscriptionPoolProductId + "' which contains a content_override_enabled for repoId '" + repoIdToEnable + "' (contentid='"
			+ contentIdToEnable + "'), it now appears in the list of enabled subscription-manager repos.");
    }
    
    
    @BeforeClass(groups = "setup")
    public void verifyCandlepinVersionBeforeClass() {
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<", "2.0.25-1")) {
			throw new SkipException("this candlepin version '"+servertasks.statusVersion+"' does not support Golden Ticket functionality.");
		}
    }

    // configuration

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



    @BeforeClass(groups={"setup"}, dependsOnMethods={"verifyCandlepinVersionBeforeClass"})
    public void setupBeforeClass() throws IOException, JSONException, SQLException {
		if (CandlepinType.standalone.equals(sm_serverType)) {
			// avoid post re-deploy problems like: "System certificates corrupted. Please reregister." and "Unable to verify server's identity: [SSL: SSLV3_ALERT_CERTIFICATE_UNKNOWN] sslv3 alert certificate unknown (_ssl.c:579)"
			if (client1tasks!=null) client1tasks.removeAllCerts(true, true, false);
			if (client2tasks!=null) client2tasks.removeAllCerts(true, true, false);
			// update candlepin.conf and re-deploy
			servertasks.uncommentConfFileParameter("candlepin.standalone");
			if (servertasks.getConfFileParameter("candlepin.standalone")==null) servertasks.addConfFileParameter ("\n# standalone true (default) is indicative of a Satellite deployment versus false which is indicative of the Customer Portal (RHSMQE was here)\n"+"candlepin.standalone", "true");	// default - indicative of a Satellite deployment
			servertasks.updateConfFileParameter("candlepin.standalone", "false");	// indicative of a Customer Portal deployment
			servertasks.uncommentConfFileParameter("module.config.hosted.configuration.module");
			if (servertasks.getConfFileParameter("module.config.hosted.configuration.module")==null) servertasks.addConfFileParameter("\n# uncomment when candlepin.standalone=false (RHSMQE was here)\n"+"module.config.hosted.configuration.module","org.candlepin.hostedtest.AdapterOverrideModule");
			servertasks.updateConfFileParameter("module.config.hosted.configuration.module","org.candlepin.hostedtest.AdapterOverrideModule");
			servertasks.redeploy();
			setupBeforeClassRedeployedCandlepin=true;
			// re-initialize after re-deploy
			servertasks.initialize(clienttasks.candlepinAdminUsername,clienttasks.candlepinAdminPassword,clienttasks.candlepinUrl);
			if (client1tasks!=null) client1tasks.installRepoCaCert(fetchServerCaCertFile(), sm_serverHostname.split("\\.")[0]+".pem");
			if (client2tasks!=null) client2tasks.installRepoCaCert(fetchServerCaCertFile(), sm_serverHostname.split("\\.")[0]+".pem");
			updateProductAndContentLockStateOnDatabase(0);
		}
	}


   private boolean setupBeforeClassRedeployedCandlepin=false;

    @AfterClass(groups={"setup"}, alwaysRun=true)	// dependsOnMethods={"verifyCandlepinVersionBeforeClass"} WILL THROW A TESTNG DEPENDENCY ERROR
    public void teardownAfterClass() throws Exception {
		if (CandlepinType.standalone.equals(sm_serverType) && setupBeforeClassRedeployedCandlepin) {
			// avoid post re-deploy problems like: "System certificates corrupted. Please reregister." and "Unable to verify server's identity: [SSL: SSLV3_ALERT_CERTIFICATE_UNKNOWN] sslv3 alert certificate unknown (_ssl.c:579)"
			if (client1tasks!=null) client1tasks.removeAllCerts(true, true, false);
			if (client2tasks!=null) client2tasks.removeAllCerts(true, true, false);
			// update candlepin.conf and re-deploy
			servertasks.updateConfFileParameter("candlepin.standalone", "true");	// default - indicative of a Satellite deployment
			servertasks.commentConfFileParameter("module.config.hosted.configuration.module");
			servertasks.redeploy();
			// re-initialize after re-deploy
			servertasks.initialize(clienttasks.candlepinAdminUsername,clienttasks.candlepinAdminPassword,clienttasks.candlepinUrl);
			deleteSomeSecondarySubscriptionsBeforeSuite();
			if (client1tasks!=null) client1tasks.installRepoCaCert(fetchServerCaCertFile(), sm_serverHostname.split("\\.")[0]+".pem");
			if (client2tasks!=null) client2tasks.installRepoCaCert(fetchServerCaCertFile(), sm_serverHostname.split("\\.")[0]+".pem");
			clienttasks.removeAllCerts(true, true, false);// to force the removal of the consumer and subscriptions if any
		}
    }

}
