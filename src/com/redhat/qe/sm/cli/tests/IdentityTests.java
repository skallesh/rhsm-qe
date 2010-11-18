package com.redhat.qe.sm.cli.tests;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.Test;

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
@Test(groups={"identity"})
public class IdentityTests extends SubscriptionManagerCLITestScript {

	
	
	@Test(	description="subscription-manager-cli: identity",
			groups={},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void Identity_Test() {
		
		// start fresh by unregistering and registering
		clienttasks.unregister();
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(clientusername,clientpassword,null,null,null,null, null));
		
		// get the current identity
		SSHCommandResult result = clienttasks.identity(null, null, null);
		
		// assert the current identity matches what was returned from register
		Assert.assertEquals(result.getStdout().trim(), "Current identity is "+consumerId);
	}
	
	
	@Test(	description="subscription-manager-cli: identity regenerate",
			groups={},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void IdentityRegenerate_Test() {
		
		// start fresh by unregistering and registering
		clienttasks.unregister();
		SSHCommandResult registerResult = clienttasks.register(clientusername,clientpassword,null,null,null,null, null);
		ConsumerCert origConsumerCert = clienttasks.getCurrentConsumerCert();
		
		// regenerate the identity... and assert
		log.info("Regenerating identity using the current cert for authentication...");
		SSHCommandResult result = clienttasks.identity(null,null,Boolean.TRUE);
		Assert.assertEquals(result.getStdout().trim(), registerResult.getStdout().trim(),
				"The original registered result is returned from identity regenerate with original authenticator.");
		
		// also assert that the newly regenerated cert matches but is newer than the original cert
		log.info("also asserting that the newly regenerated cert matches but is newer than original cert...");
		ConsumerCert newConsumerCert = clienttasks.getCurrentConsumerCert();
		Assert.assertEquals(newConsumerCert.consumerid, origConsumerCert.consumerid, "The consumerids are a match.");
		Assert.assertEquals(newConsumerCert.issuer, origConsumerCert.issuer, "The issuers are a match.");
		Assert.assertEquals(newConsumerCert.username, origConsumerCert.username, "The usernames are a match.");
		Assert.assertEquals(newConsumerCert.validityNotAfter, origConsumerCert.validityNotAfter, "The validity end dates are a match.");
		Assert.assertTrue(newConsumerCert.validityNotBefore.after(origConsumerCert.validityNotBefore), "The new validity start date is after the original.");
		Assert.assertNotSame(newConsumerCert.serialNumber, origConsumerCert.serialNumber, "The serial numbers should not match.");
	}
	
	
	@Test(	description="subscription-manager-cli: identity regenerate with username and password from the same owner",
			groups={},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void IdentityRegenerateWithUsernameAndPaswordFromTheSameOwner_Test() throws Exception {

		// start fresh by unregistering and registering
		clienttasks.unregister();
		SSHCommandResult registerResult = clienttasks.register(clientusername,clientpassword,null,null,null,null, null);
		String ownerKey = CandlepinTasks.getOwnerOfConsumerId(serverHostname, serverPort, serverPrefix, clientOwnerUsername, clientOwnerPassword, clienttasks.getCurrentConsumerId(registerResult)).getString("key");

		// regenerate the identity using the same username and password as used during register... and assert
		log.info("Regenerating identity with the same username and password as used during register...");
		SSHCommandResult result = clienttasks.identity(clientusername,clientpassword,Boolean.TRUE);
		Assert.assertEquals(result.getStdout().trim(), registerResult.getStdout().trim(),
				"The original registered result is returned from identity regenerate with original authenticator.");
		
		// find a username from the registrationDataList whose owner does not match the registerer of this client
		RegistrationData registrationData = findRegistrationDataMatchingOwnerKeyButNotMatchingUsername(ownerKey,clientusername);
		if (registrationData==null) throw new SkipException("Could not find registration data for another user who belongs to the same owner '"+ownerKey+"' as '"+clientusername+"'.");

		// regenerate the identity using a different username and password as used during register... and assert
		log.info("Regenerating identity with a different username and password (but belonging to the same owner) than used during register...");
		result = clienttasks.identity(registrationData.username,registrationData.password,Boolean.TRUE);
		Assert.assertEquals(result.getStdout().trim(), registerResult.getStdout().trim(),
			"The original registered result is returned from identity regenerate using a different authenticator who belongs to the same owner/organization.");
	}
	
	
	@Test(	description="subscription-manager-cli: identity regenerate with username and password from a different owner (negative test)",
			groups={},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void IdentityRegenerateWithUsernameAndPaswordFromADifferentOwner_Test() throws Exception {

		// start fresh by unregistering and registering
		clienttasks.unregister();
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(clientusername,clientpassword,null,null,null,null, null));
		String consumerOwner = CandlepinTasks.getOwnerOfConsumerId(serverHostname, serverPort, serverPrefix, clientOwnerUsername, clientOwnerPassword, consumerId).getString("key");

		// find a username from the registrationDataList whose owner does not match the registerer of this client
		RegistrationData registrationData = findRegistrationDataNotMatchingOwnerKey(consumerOwner);
		if (registrationData==null) throw new SkipException("Could not find registration data for a user who does not belong to owner '"+consumerOwner+"'.");

		// retrieve the identity using the same username and password as used during register... and assert
		log.info("Attempting to regenerate identity with an invalid username and password...");
		SSHCommandResult result = clienttasks.identity_(registrationData.username,registrationData.password,Boolean.TRUE);
		Assert.assertNotSame(result.getExitCode(), Integer.valueOf(0), "The identify command was NOT a success.");
		Assert.assertEquals(result.getStderr().trim(),"access denied.");
	}
	
	
	@Test(	description="subscription-manager-cli: identity regenerate with invalid username and password (negative test)",
			groups={},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void IdentityRegenerateWithInvalidUsernameAndPasword_Test() {
		
		// start fresh by unregistering and registering
		clienttasks.unregister();
		clienttasks.register(clientusername,clientpassword,null,null,null,null, null);
		
		// retrieve the identity using the same username and password as used during register... and assert
		log.info("Attempting to regenerate identity with an invalid username and password...");
		SSHCommandResult result = clienttasks.identity_("FOO","BAR",Boolean.TRUE);
		Assert.assertNotSame(result.getExitCode(), Integer.valueOf(0), "The identify command was NOT a success.");
		//Assert.assertEquals(result.getStderr().trim(),"Invalid username or password");	// works against on-premises, not hosted
		Assert.assertTrue(result.getStderr().trim().startsWith("Invalid username or password"),"Invalid username or password");
	}
	
	
	
	
	// Configuration methods ***********************************************************************
	

	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************


}
