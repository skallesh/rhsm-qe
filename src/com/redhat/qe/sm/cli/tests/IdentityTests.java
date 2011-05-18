package com.redhat.qe.sm.cli.tests;

import java.io.IOException;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.ConsumerCert;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * PREREQUISITE: This test class assumes that the RegisterTests.RegisterWithUsernameAndPassword_Test is run prior to this class.
 *

THIS IS AN EMAIL FROM bkearney@redhat.com INTRODUCING identity

Per Jesus' suggestion, I rolled in the move of re-register to an
identity command. It will now do the following:

subscription-manager-cli identity
Spit out the current identity

subscription-manager-cli identity --regenerate
Create a new certificated based on the UUID in the current cert, and
useing the cert as authenticatoin

subscription-manager-cli identity --regenerate --username foo --password bar
Create a new certificated based on the UUID in the current cert, and
using the username/password as authentication

-- bk
 */
@Test(groups={"IdentityTests"})
public class IdentityTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager-cli: identity (when not registered)",
			groups={"blockedByBug-654429"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void IdentityWhenNotRegistered_Test() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null);
		
		log.info("Assert that one must be registered to query the identity...");
		for (String username : new String[]{null,clientusername}) {
			for (String password : new String[]{null,clientpassword}) {
				for (Boolean regenerate : new Boolean[]{null,true,false}) {
					SSHCommandResult result = clienttasks.identity_(username,password,regenerate, null, null, null, null);
					Assert.assertEquals(result.getStdout().trim(),"Consumer not registered. Please register using --username and --password",
						"One must be registered to have an identity.");
				}
			}
		}
	}
	
	
	@Test(	description="subscription-manager-cli: identity",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void Identity_Test() {
		
		// start fresh by unregistering and registering
		clienttasks.unregister(null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(clientusername,clientpassword,null,null,null,null, null, null, null, null));
		
		// get the current identity
		SSHCommandResult result = clienttasks.identity(null, null, null, null, null, null, null);
		
		// assert the current identity matches what was returned from register
		// ALPHA: Assert.assertEquals(result.getStdout().trim(), "Current identity is "+consumerId);
		// Assert.assertEquals(result.getStdout().trim(), "Current identity is: "+consumerId+" name: "+clientusername);
		Assert.assertEquals(result.getStdout().trim(), "Current identity is: "+consumerId+" name: "+clienttasks.hostname);
	}
	
	
	@Test(	description="subscription-manager-cli: identity (when the client registered with --name)",
			groups={"blockedByBug-647891"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void IdentityWithName_Test() {
		
		// start fresh by unregistering and registering
		clienttasks.unregister(null, null, null);
		String nickname = "Mr_"+clientusername;
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(clientusername,clientpassword,null,nickname,null,null, null, null, null, null));
		
		// get the current identity
		SSHCommandResult result = clienttasks.identity(null, null, null, null, null, null, null);
		
		// assert the current identity matches what was returned from register
		Assert.assertEquals(result.getStdout().trim(), "Current identity is: "+consumerId+" name: "+nickname);
	}
	
	
	@Test(	description="subscription-manager-cli: identity regenerate",
			groups={},
			enabled=true)
	@ImplementsNitrateTest(caseId=64179)
	public void IdentityRegenerate_Test() {
		
		// start fresh by unregistering and registering
		clienttasks.unregister(null, null, null);
		SSHCommandResult registerResult = clienttasks.register(clientusername,clientpassword,null,null,null,null, null, null, null, null);
		ConsumerCert origConsumerCert = clienttasks.getCurrentConsumerCert();
		
		// regenerate the identity... and assert
		log.info("Regenerating identity using the current cert for authentication...");
		SSHCommandResult result = clienttasks.identity(null,null,Boolean.TRUE, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), registerResult.getStdout().trim(),
				"The original registered result is returned from identity regenerate with original authenticator.");
		
		// also assert that the newly regenerated cert matches but is newer than the original cert
		log.info("also asserting that the newly regenerated cert matches but is newer than original cert...");
		ConsumerCert newConsumerCert = clienttasks.getCurrentConsumerCert();
		Assert.assertEquals(newConsumerCert.consumerid, origConsumerCert.consumerid, "The consumerids are a match.");
		Assert.assertEquals(newConsumerCert.issuer, origConsumerCert.issuer, "The issuers are a match.");
		Assert.assertEquals(newConsumerCert.name, origConsumerCert.name, "The usernames are a match.");
		//Assert.assertEquals(newConsumerCert.validityNotAfter, origConsumerCert.validityNotAfter, "The validity end dates are a match."); //Not After : Jan 6 23:59:59 2012 GMT
		Assert.assertTrue(newConsumerCert.validityNotAfter.after(origConsumerCert.validityNotAfter), "The new validity end date is after the original."); // with fix from https://bugzilla.redhat.com/show_bug.cgi?id=660713#c10
		Assert.assertTrue(newConsumerCert.validityNotBefore.after(origConsumerCert.validityNotBefore), "The new validity start date is after the original.");
		Assert.assertNotSame(newConsumerCert.serialNumber, origConsumerCert.serialNumber, "The serial numbers should not match on a regenerated identity cert.");
	}
	
	
	@Test(	description="subscription-manager-cli: identity regenerate with username and password from the same owner",
			groups={}, /*dependsOnGroups={"RegisterWithUsernameAndPassword_Test"},*/
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void IdentityRegenerateWithUsernameAndPaswordFromTheSameOwner_Test() throws Exception {

		// start fresh by unregistering and registering
		clienttasks.unregister(null, null, null);
		SSHCommandResult registerResult = clienttasks.register(clientusername,clientpassword,null,null,null,null, null, null, null, null);
		//String ownerKey = CandlepinTasks.getOwnerOfConsumerId(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, clienttasks.getCurrentConsumerId(registerResult)).getString("key");
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(serverHostname, serverPort, serverPrefix, clientusername, clientpassword, clienttasks.getCurrentConsumerId(registerResult));

		// regenerate the identity using the same username and password as used during register... and assert
		log.info("Regenerating identity with the same username and password as used during register...");
		SSHCommandResult result = clienttasks.identity(clientusername,clientpassword,Boolean.TRUE, Boolean.TRUE, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), registerResult.getStdout().trim(),
				"The original registered result is returned from identity regenerate with original authenticator.");
		
		// find a username from the registrationDataList whose owner does not match the registerer of this client
		RegistrationData registrationData = findRegistrationDataMatchingOwnerKeyButNotMatchingUsername(ownerKey,clientusername);
		if (registrationData==null) throw new SkipException("Could not find registration data for another user who belongs to the same owner '"+ownerKey+"' as '"+clientusername+"'.");

		// regenerate the identity using a different username and password as used during register... and assert
		log.info("Regenerating identity with a different username and password (but belonging to the same owner) than used during register...");
		result = clienttasks.identity(registrationData.username,registrationData.password,Boolean.TRUE, Boolean.TRUE, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), registerResult.getStdout().trim(),
			"The original registered result is returned from identity regenerate using a different authenticator who belongs to the same owner/organization.");
	}
	
	
	@Test(	description="subscription-manager-cli: identity regenerate with username and password from a different owner (negative test)",
			groups={}, /*dependsOnGroups={"RegisterWithUsernameAndPassword_Test"},*/
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void IdentityRegenerateWithUsernameAndPaswordFromADifferentOwner_Test() throws Exception {

		// start fresh by unregistering and registering
		clienttasks.unregister(null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(clientusername,clientpassword,null,null,null,null, null, null, null, null));
		//String ownerKey = CandlepinTasks.getOwnerOfConsumerId(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, consumerId).getString("key");
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(serverHostname, serverPort, serverPrefix, clientusername, clientpassword, consumerId);

		// find a username from the registrationDataList whose owner does not match the registerer of this client
		RegistrationData registrationData = findRegistrationDataNotMatchingOwnerKey(ownerKey);
		if (registrationData==null) throw new SkipException("Could not find registration data for a user who does not belong to owner '"+ownerKey+"'.");

		// retrieve the identity using the same username and password as used during register... and assert
		log.info("Attempting to regenerate identity with an invalid username and password...");
		SSHCommandResult result = clienttasks.identity_(registrationData.username,registrationData.password,Boolean.TRUE, Boolean.TRUE, null, null, null);
		Assert.assertNotSame(result.getExitCode(), Integer.valueOf(0), "The identify command was NOT a success.");
		Assert.assertEquals(result.getStderr().trim(),"access denied.");
	}
	
	
	@Test(	description="subscription-manager-cli: identity regenerate with invalid username and password (attempt with and without force) (negative test)",
			groups={"blockedByBug-678151"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void IdentityRegenerateWithInvalidUsernameAndPasword_Test() {
		
		// start fresh by unregistering and registering
		clienttasks.unregister(null, null, null);
		clienttasks.register(clientusername,clientpassword,null,null,null,null, null, null, null, null);
		
		// retrieve the identity using the same username and password as used during register... and assert
		log.info("Attempting to regenerate identity with an invalid username and password...");
		// first attempt without --force
		SSHCommandResult result = clienttasks.identity_("FOO","BAR",Boolean.TRUE, null, null, null, null);
		Assert.assertNotSame(result.getExitCode(), Integer.valueOf(0), "The identify command was NOT a success.");
		Assert.assertEquals(result.getStdout().trim(),"--username and --password can only be used with --force");
		// now attempt with --force
		result = clienttasks.identity_("FOO","BAR",Boolean.TRUE, Boolean.TRUE, null, null, null);
		Assert.assertNotSame(result.getExitCode(), Integer.valueOf(0), "The identify command was NOT a success.");
		Assert.assertContainsMatch(result.getStderr().trim(),servertasks.invalidCredentialsRegexMsg(),"The stderr expresses a message such that authentication credentials are invalid.");
	}
	
	
	// Candidates for an automated Test:
	// TODO https://bugzilla.redhat.com/show_bug.cgi?id=678151
	
	
	
	// Configuration Methods ***********************************************************************
	
	@BeforeClass(groups="setup")
	public void setupBeforeClass() throws IOException {
		// alternative to dependsOnGroups={"RegisterWithUsernameAndPassword_Test"}
		// This allows us to satisfy a dependency on registrationDataList making TestNG add unwanted Test results.
		// This also allows us to individually run this Test Class on Hudson.
		RegisterWithUsernameAndPassword_Test(); // needed to populate registrationDataList
	}
	
	
	
	// Protected Methods ***********************************************************************


	
	// Data Providers ***********************************************************************


}
