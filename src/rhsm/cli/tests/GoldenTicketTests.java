package rhsm.cli.tests;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.redhat.qe.tools.SSHCommandResult;

import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.EntitlementCert;

/**
 * @author skallesh
 *
 *         References:https://docs.google.com/document/d/1h-CeEV_FEu885JhZNkydzQQsDkAQ-WtZ5cisCxj4Lao/edit?ts=5873bb00
 */

@Test(groups = { "GoldenTicketTests", "Tier3Tests" })
public class GoldenTicketTests extends SubscriptionManagerCLITestScript {
    protected String attributeName = "contentAccessMode";
    protected String attributeValue = "org_environment";
    protected String org = "snowwhite";

    @Test(description = "Verify golden ticket entitlement is granted when system is registered to an org that has contentaccessmode set", groups = {
	    "verifyGoldenTicketfunctionality" }, enabled = true)
    public void verifyGoldenTicketfunctionality() throws Exception {

	CandlepinTasks.setAttributeForOrg(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, org,
		attributeName, attributeValue);

	clienttasks.register(sm_clientUsername, sm_clientPassword, org, null, null, null, null, null, null, null,
		(String) null, null, null, null, true, null, null, null, null);
	clienttasks.autoheal(null, null, true, null, null, null);
	String ExpectedRepoMsg = "There were no available repositories matching the specified criteria.";

	// verify the if an extra entitlement is granted upon refresh on
	// subscription-manager version lesser than equal to 1.18.9-1
	if (clienttasks.isPackageVersion("subscription-manager", "<=", "1.18.9-1")) {
	    SSHCommandResult repoResult = clienttasks.repos(false, false, true, (String) null, null, null, null, null);
	    Assert.assertEquals(repoResult.getStdout().toString().trim(), ExpectedRepoMsg);
	    clienttasks.refresh(null, null, null);
	}

	// verify only the extra entitlement cert granted by or/environment is
	// present
	List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
	Assert.assertTrue(entitlementCerts.size() == 1,
		"Only extra entitlement granted by the org/environment is present");

	// verify repos --list lists all the repos but none of them are enabled
	// when the system has golden ticket certificate access
	SSHCommandResult resultListEnabled = clienttasks.repos(false, true, false, (String) null, null, null, null,
		null);
	Assert.assertEquals(resultListEnabled.getStdout().toString().trim(), ExpectedRepoMsg);
	SSHCommandResult resultListDisabled = clienttasks.repos(false, false, true, (String) null, null, null, null,
		null);
	Assert.assertNotEquals(resultListDisabled.getStdout().toString().trim(), ExpectedRepoMsg);

	// Verify status cmd message on a system that has golden ticket
	// Todo add assert for golden ticket mode note once fixed
	SSHCommandResult statusResult = clienttasks.status(null, null, null, null);
	String expectedStatus = "Overall Status: Invalid";
	Assert.assertTrue(statusResult.getStdout().contains(expectedStatus), "Expecting '" + expectedStatus
		+ "The status of machine is still invalid despite having golden ticket entitlement");

	// verify list --consumed displays the goldenticket entitlement
	SSHCommandResult listConsumedResult = clienttasks.list(null, null, true, null, null, null, null, null, null,
		null, null, null, null);
	String expectedMessageForListConsumed = "No consumed subscription pools to list";

	Assert.assertTrue(listConsumedResult.getStdout().trim().equals(expectedMessageForListConsumed),
		"Expecting '" + expectedMessageForListConsumed
			+ "subscription-manager list --consumed doesnot list golden ticket entitlement");

	// verify after manually deleting the certs from /etc/pki/entitlement
	// dir , refresh command regenerates the entitlement
	clienttasks.removeAllCerts(false, true, false);
	Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().size() == 0,
		"Golden ticket cert is successfully removed");
	clienttasks.refresh(null, null, null);
	Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().size() >= 1,
		"Golden ticket regenerated successfully");
	resultListDisabled = clienttasks.repos(false, false, true, (String) null, null, null, null, null);
	Assert.assertNotEquals(resultListDisabled.getStdout().toString().trim(), ExpectedRepoMsg);

	// Verify remove --all command doesnot remove the golden ticket
	// entitlement along with other subscriptions
	clienttasks.subscribe(true, null, null, (String) null, null, null, null, null, null, null, null, null);
	Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().size() > 1,
		"There are more subscriptions attached other than the golden ticket");
	SSHCommandResult AutoAttachlistConsumedResult = clienttasks.list(null, null, true, null, null, null, null, null,
		null, null, null, null, null);

	Assert.assertFalse(AutoAttachlistConsumedResult.getStdout().trim().equals(expectedMessageForListConsumed),
		"Expecting'" + expectedMessageForListConsumed
			+ "subscription-manager list --consumed lists the subscriptions consumed after auto-attach command is successful");

	clienttasks.unsubscribe_(true, null, (String) null, null, null, null);

	List<EntitlementCert> entitlementCertsAfterRemoveAll = clienttasks.getCurrentEntitlementCerts();
	Assert.assertTrue(entitlementCertsAfterRemoveAll.size() == 1,
		"Only extra entitlement granted by the org/environment is present");

	// verify removing the golden ticekt entitlement by subscription-manager
	// remove --serial <serial_id> fails to remove the golden ticket
	// entitlement
	List<EntitlementCert> entitlementCertsToRemove = EntitlementCert.findAllInstancesWithMatchingFieldFromList(
		"poolId", "Not Available", clienttasks.getCurrentEntitlementCerts());
	for (EntitlementCert entitlementCert : entitlementCertsToRemove) {

	    SSHCommandResult removeResult = clienttasks.unsubscribe_(null, entitlementCert.serialNumber, null, null,
		    null, null);
	    String ExpectedMessageForRemove = "The entitlement server failed to remove these serial numbers:" + "\n";
	    ExpectedMessageForRemove += "   " + entitlementCert.serialNumber;
	    Assert.assertEquals(removeResult.getStdout().trim(), ExpectedMessageForRemove);
	}

	clienttasks.repos(false, false, false, "*", null, null, null, null);
	resultListEnabled = clienttasks.repos(false, true, false, (String) null, null, null, null, null);
	Assert.assertNotEquals(resultListEnabled.getStdout().toString().trim(), ExpectedRepoMsg);

    }

    @Test(description = "Verify golden ticket entitlement is granted when system is registered using an activationkey that belongs org that has contentaccessmode set", groups = {
	    "goldenTicketEntitlementIsGrantedWhenRegisteredUsingActivationKey" }, enabled = true)
    public void ExtraEntitlementIsGrantedWhenRegisteredUsingActivationKey() throws JSONException, Exception {
	// verify registering the system to activation key belonging to
	// owner(contentAccessmode set) with auto-attach false has access to
	// golden ticket

	CandlepinTasks.setAttributeForOrg(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, org,
		attributeName, attributeValue);
	String activationKeyName = String.format("%s_%s-ActivationKey%s", sm_clientUsername, sm_clientOrg,
		System.currentTimeMillis());
	Map<String, String> mapActivationKeyRequest = new HashMap<>();
	mapActivationKeyRequest.put("name", activationKeyName);
	mapActivationKeyRequest.put("autoAttach", "false");
	JSONObject jsonActivationKeyRequest = new JSONObject(mapActivationKeyRequest);
	CandlepinTasks.postResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,
		"/owners/" + org + "/activation_keys", jsonActivationKeyRequest.toString());

	clienttasks.register(null, null, org, null, null, null, null, null, null, null, activationKeyName, null, null,
		null, true, null, null, null, null);
	// verify only the extra entitlement cert granted by or/environment is
	// present
	List<EntitlementCert> entitlementCertsAfterRegisteringToactivationKeyFalse = clienttasks
		.getCurrentEntitlementCerts();
	Assert.assertTrue(entitlementCertsAfterRegisteringToactivationKeyFalse.size() == 1,
		"Only extra entitlement granted by the org/environment is present");

	// verify registering the system to activation key belonging to owner
	// (contentAccessmode set) with auto-attach true also has access to
	// golden ticket

	String activationKeyNameTrue = String.format("%s_%s-ActivationKey%s", sm_clientUsername, org,
		System.currentTimeMillis());
	Map<String, String> mapActivationKeyTrueRequest = new HashMap<>();
	mapActivationKeyTrueRequest.put("name", activationKeyNameTrue);
	mapActivationKeyTrueRequest.put("autoAttach", "true");
	JSONObject jsonActivationKeyTrueRequest = new JSONObject(mapActivationKeyTrueRequest);
	CandlepinTasks.postResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,
		"/owners/" + org + "/activation_keys", jsonActivationKeyTrueRequest.toString());

	clienttasks.register(null, null, org, null, null, null, null, null, null, null, activationKeyNameTrue, null,
		null, null, true, null, null, null, null);

	// verify only the extra entitlement cert granted by or/environment is
	// present

	List<EntitlementCert> extraEntitlementCerts = EntitlementCert.findAllInstancesWithMatchingFieldFromList(
		"poolId", "Not Available", clienttasks.getCurrentEntitlementCerts());
	System.out.println(extraEntitlementCerts.size());

	Assert.assertTrue(extraEntitlementCerts.size() == 1,
		"extra entitlement granted by owner is present along with other entitlements attached by auto-attach");
    }

    @Test(description = "Verify revoking contentAccessMode set on the owner removes extra entitlement", groups = {
	    "revokingcontentAccessModeOnOwnerRemovesEntitlement" }, enabled = true)
    public void revokingcontentAccessModeOnOwnerRemovesEntitlement() throws Exception {
	CandlepinTasks.setAttributeForOrg(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, org,
		attributeName, attributeValue);

	clienttasks.register(sm_clientUsername, sm_clientPassword, org, null, null, null, null, null, null, null,
		(String) null, null, null, null, true, null, null, null, null);
	clienttasks.autoheal(null, null, true, null, null, null);
	String ExpectedRepoMsg = "There were no available repositories matching the specified criteria.";
	if (clienttasks.isPackageVersion("subscription-manager", "<=", "1.18.9-1")) {
	    SSHCommandResult repoResult = clienttasks.repos_(false, false, true, (String) null, null, null, null, null);
	    Assert.assertEquals(repoResult.getStdout().toString().trim(), ExpectedRepoMsg);
	    clienttasks.refresh(null, null, null);

	}
	SSHCommandResult repoResult = clienttasks.repos(false, false, true, (String) null, null, null, null, null);
	Assert.assertNotEquals(repoResult.getStdout().toString().trim(), ExpectedRepoMsg);
	// verify only the extra entitlement cert granted by or/environment is
	// present
	List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
	Assert.assertTrue(entitlementCerts.size() == 1,
		"Only extra entitlement granted by the org/environment is present");

	// now revoke the contentAccessMode set on the owner

	CandlepinTasks.setAttributeForOrg(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, org,
		attributeName, "");
	clienttasks.refresh(null, null, null);

	repoResult = clienttasks.repos(false, false, true, (String) null, null, null, null, null);
	String ExpectedRepoMsgAfterRevoke = "This system has no repositories available through subscriptions.";
	Assert.assertEquals(repoResult.getStdout().toString().trim(), ExpectedRepoMsgAfterRevoke);
	List<EntitlementCert> entitlementCertsAfterRevoke = clienttasks.getCurrentEntitlementCerts();
	Assert.assertTrue(entitlementCertsAfterRevoke.size() == 0,
		"No extra entitlement granted by the org/environment is present");

    }

    // configuration

    @BeforeClass(groups = "setup")
    public void verifyCandlepinVersion() {
	if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<", "2.0.25-1")) {
	    throw new SkipException("this candlepin version '" + servertasks.statusVersion
		    + "' does not support Golden Ticket functionality.");
	}

    }

    @BeforeClass(groups = "setup")
    public void BeforeClassSetup() throws IOException, JSONException {
	if (CandlepinType.standalone.equals(sm_serverType)) {
	    servertasks.updateConfFileParameter("candlepin.standalone", "false");
	    servertasks.uncommentConfFileParameter("module.config.hosted.configuration.module");
	    // restart tomcat after setting candlepin.standalone=false

	    servertasks.restartTomcat();
	    servertasks.initialize(clienttasks.candlepinAdminUsername, clienttasks.candlepinAdminPassword,
		    clienttasks.candlepinUrl);
	}

    }

    @AfterClass(groups = "setup")
    public void AfterClassTeardown() throws IOException, JSONException, SQLException {
	if (CandlepinType.standalone.equals(sm_serverType)) {
	    servertasks.updateConfFileParameter("candlepin.standalone", "true");
	    servertasks.commentConfFileParameter("module.config.hosted.configuration.module");
	    // restart tomcat after setting candlepin.standalone=true

	    servertasks.restartTomcat();
	    servertasks.initialize(clienttasks.candlepinAdminUsername, clienttasks.candlepinAdminPassword,
		    clienttasks.candlepinUrl);
	    updateProductAndContentLockStateOnDatabase(0);
	}

    }

}
