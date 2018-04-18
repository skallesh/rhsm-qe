package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

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
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.jul.TestRecords;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.ConsumerCert;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * PREREQUISITE: This test class assumes that the RegisterTests.RegisterWithCredentials_Test is run prior to this class.
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

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36543", "RHEL7-51318"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: identity (when not registered)",
			groups={"Tier2Tests","blockedByBug-654429"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testIdentityWhenNotRegistered() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null, null);
		
		log.info("Assert that one must be registered to query the identity...");
		for (String username : new String[]{null,sm_clientUsername}) {
			for (String password : new String[]{null,sm_clientPassword}) {
				for (Boolean regenerate : new Boolean[]{null,true,false}) {
					SSHCommandResult identityResult = clienttasks.identity_(username,password,regenerate, null, null, null, null, null);
					if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
						Assert.assertEquals(identityResult.getStderr().trim(), clienttasks.msg_ConsumerNotRegistered,"One must be registered to have an identity.");
					} else {
						Assert.assertEquals(identityResult.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered,"One must be registered to have an identity.");
					}
				}
			}
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36545", "RHEL7-51320"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: identity should report RHN Classic remote server type when only registered classically",
			groups={"Tier2Tests","RHNClassicTests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testIdentityWhenRegisteredToRHNClassic() {

		// make sure we are not registered (to Candlepin)
		clienttasks.unregister(null, null, null, null);
		
		// simulate registration to RHN Classic by creating a /etc/sysconfig/rhn/systemid
		log.info("Simulating registration to RHN Classic by creating an empty systemid file '"+clienttasks.rhnSystemIdFile+"'...");
		RemoteFileTasks.runCommandAndWait(client, "touch "+clienttasks.rhnSystemIdFile, TestRecords.action());
		Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.rhnSystemIdFile), "RHN Classic systemid file '"+clienttasks.rhnSystemIdFile+"' is in place.");

		SSHCommandResult identityResult = clienttasks.identity(null,null,null,null,null,null,null, null);
		//Assert.assertEquals(identityResult.getStdout().trim(), "remote entitlement server type: RHN Classic","Stdout when registered to RHN Classic, but not candlepin.");	// changed by bug 846834
		Assert.assertEquals(identityResult.getStdout().trim(), "server type: RHN Classic","Stdout when registered to RHN Classic, but not candlepin.");
		Assert.assertEquals(identityResult.getStderr().trim(), "","Stderr when registered to RHN Classic, but not candlepin.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36544", "RHEL7-51319"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: identity should report RHN Classic remote server type and candlepin when over-registered",
			groups={"Tier2Tests","RHNClassicTests","blockedByBug-846834","blockedByBug-852328"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testIdentityWhenRegisteredToRHNClassicAndCandlepin() {

		// make sure we are registered (to Candlepin)
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg));
		
		// simulate registration to RHN Classic by creating a /etc/sysconfig/rhn/systemid
		log.info("Simulating registration to RHN Classic by creating an empty systemid file '"+clienttasks.rhnSystemIdFile+"'...");
		RemoteFileTasks.runCommandAndWait(client, "touch "+clienttasks.rhnSystemIdFile, TestRecords.action());
		Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.rhnSystemIdFile), "RHN Classic systemid file '"+clienttasks.rhnSystemIdFile+"' is in place.");

		SSHCommandResult identityResult = clienttasks.identity(null,null,null,null,null,null,null, null);
		List<String> expectedStdoutRegexs = new ArrayList<String>();
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.6-1")) expectedStdoutRegexs.add("^system identity: "+consumerId);	// changed to this by commit 7da29583efa091337be233b9795b0157283aad0f Change wording for identity in CLI command.
		else expectedStdoutRegexs.add("^Current identity is: "+consumerId);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.1.4-1")) expectedStdoutRegexs.add("^server type: RHN Classic and Red Hat Subscription Management$");
		else expectedStdoutRegexs.add("^server type: RHN classic and Red Hat Subscription Management$");	// changed by bug 852328
		//expectedStdoutRegexs.add("^remote entitlement server type: RHN classic and subscription management service$");	// changed by bug 846834
		for (String expectedStdoutRegex : expectedStdoutRegexs) {
			Assert.assertTrue(Pattern.compile(expectedStdoutRegex, Pattern.MULTILINE/* | Pattern.DOTALL*/).matcher(identityResult.getStdout()).find(),"Stdout contains expected regex: "+expectedStdoutRegex);
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36547", "RHEL7-51322"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: identity",
			groups={"Tier2Tests","blockedByBug-852001","blockedByBug-1101522","blockedByBug-1451003"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testIdentity() throws JSONException, Exception {
		String identityRegex;
		
		// start fresh by unregistering and registering
		clienttasks.unregister(null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg));
		
		// get the current identity
		SSHCommandResult identityResult = clienttasks.identity(null, null, null, null, null, null, null, null);
		
		// assert the current identity matches what was returned from register
		// ALPHA: Assert.assertEquals(result.getStdout().trim(), "Current identity is "+consumerId);
		// Assert.assertEquals(result.getStdout().trim(), "Current identity is: "+consumerId+" name: "+clientusername);
		// Assert.assertEquals(result.getStdout().trim(), "Current identity is: "+consumerId+" name: "+clienttasks.hostname);	// RHEL61 RHEL57
		identityRegex = String.format("^%s%s$", "Current identity is: ",consumerId);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.6-1")) identityRegex = String.format("^%s%s$", "system identity: ",consumerId);	// changed to this by commit 7da29583efa091337be233b9795b0157283aad0f Change wording for identity in CLI command.
		Assert.assertContainsMatch(identityResult.getStdout().trim(), identityRegex);
		identityRegex = String.format("^%s%s$", "name: ",clienttasks.hostname);
		Assert.assertContainsMatch(identityResult.getStdout().trim(), identityRegex);
		
		// also assert additional output from the new multi-owner function
		JSONObject owner = CandlepinTasks.getOwnerOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);
		Assert.assertContainsMatch(identityResult.getStdout().trim(), String.format("^%s%s$", "org name: ",owner.getString("displayName")));
		//Assert.assertContainsMatch(identityResult.getStdout().trim(), String.format("^%s%s$", "org id: ",owner.getString("id")));	// RHEL63
		//Assert.assertContainsMatch(identityResult.getStdout().trim(), String.format("^%s%s$", "org id: ",owner.getString("key")));	// technically the org id has been changed to display "key" which is more useful (after bug fix 852001)	// msgid changed by bug 878634
		identityRegex = String.format("^%s%s$", "org ID: ",owner.getString("key"));	// technically the org id has been changed to display "key" which is more useful (after bug fix 852001)
		Assert.assertContainsMatch(identityResult.getStdout().trim(), identityRegex);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36546", "RHEL7-51321"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: identity (when the client registered with --name)",
			groups={"Tier2Tests","blockedByBug-647891","blockedByBug-1101522","blockedByBug-1451003"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testIdentityWithName() {
		String identityRegex;
		
		// start fresh by unregistering and registering
		clienttasks.unregister(null, null, null, null);
		String nickname = "Mr_"+sm_clientUsername;
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,nickname,null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null));
		
		// get the current identity
		SSHCommandResult identityResult = clienttasks.identity(null, null, null, null, null, null, null, null);
		
		// assert the current identity matches what was returned from register
		// Assert.assertEquals(result.getStdout().trim(), "Current identity is: "+consumerId+" name: "+nickname);	// RHEL61 RHEL57
		identityRegex = String.format("^%s%s$", "Current identity is: ",consumerId);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.6-1")) identityRegex = String.format("^%s%s$", "system identity: ",consumerId);	// changed to this by commit 7da29583efa091337be233b9795b0157283aad0f Change wording for identity in CLI command.
		Assert.assertContainsMatch(identityResult.getStdout().trim(), identityRegex);
		identityRegex = String.format("^%s%s$", "name: ",nickname);
		Assert.assertContainsMatch(identityResult.getStdout().trim(), identityRegex);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19961", "RHEL7-33075"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager-cli: identity regenerate",
			groups={"Tier1Tests"},
			enabled=true)
	@ImplementsNitrateTest(caseId=64179)
	public void testIdentityRegenerate() {
		
		// start fresh by unregistering and registering
		clienttasks.unregister(null, null, null, null);
		SSHCommandResult registerResult = clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		ConsumerCert origConsumerCert = clienttasks.getCurrentConsumerCert();
		
		// regenerate the identity... and assert
		log.info("Regenerating identity using the current cert for authentication...");
		SSHCommandResult identityResult = clienttasks.identity(null,null,Boolean.TRUE, null, null, null, null, null);
		// RHEL57 RHEL61 Assert.assertEquals(identityResult.getStdout().trim(), registerResult.getStdout().trim(),
		//		"The original registered result is returned from identity regenerate with original authenticator.");
		
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


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36542", "RHEL7-51317"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: identity regenerate with username and password from the same owner",
			groups={"Tier2Tests","blockedByBug-1254349"}, /*dependsOnGroups={"RegisterWithCredentials_Test"},*/
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testIdentityRegenerateWithUsernameAndPaswordFromTheSameOwner() throws Exception {

		// start fresh by unregistering and registering
		clienttasks.unregister(null, null, null, null);
		SSHCommandResult registerResult = clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, clienttasks.getCurrentConsumerId(registerResult));

		// regenerate the identity using the same username and password as used during register... and assert
		log.info("Regenerating identity with the same username and password as used during register...");
		SSHCommandResult identityResult = clienttasks.identity(sm_clientUsername,sm_clientPassword,Boolean.TRUE, Boolean.TRUE, null, null, null, null);
//		Assert.assertEquals(identityResult.getStdout().trim(), registerResult.getStdout().trim(),
//			"The original registered result is returned from identity regenerate with original authenticator.");
		Assert.assertEquals(clienttasks.getCurrentConsumerId(), clienttasks.getCurrentConsumerId(registerResult),
			"The original registered result is returned from identity regenerate with original authenticator.");

		
		// find a different username from the registrationDataList whose owner does match the registerer of this client
		List<RegistrationData> registrationData = findGoodRegistrationData(false,sm_clientUsername,true,sm_clientOrg);
		if (registrationData.isEmpty()) throw new SkipException("Could not find registration data for a different user who does belong to owner '"+ownerKey+"'.");

//		RegistrationData registrationDatum = registrationData.get(0);
		for (RegistrationData registrationDatum : registrationData) {
			
			// regenerate the identity using a different username and password as used during register... and assert
			log.info("Regenerating identity with a different username and password (but belonging to the same owner) than used during register...");
			identityResult = clienttasks.identity(registrationDatum.username,registrationDatum.password,Boolean.TRUE, Boolean.TRUE, null, null, null, null);
//			Assert.assertEquals(result.getStdout().trim(), registerResult.getStdout().trim(),
//				"The original registered result is returned from identity regenerate using a different authenticator who belongs to the same owner/organization.");
			Assert.assertEquals(clienttasks.getCurrentConsumerId(), clienttasks.getCurrentConsumerId(registerResult),
				"The original registered result is returned from identity regenerate with original authenticator.");
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36541", "RHEL7-51316"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: identity regenerate with username and password from a different owner (negative test)",
			groups={"Tier2Tests"}, /*dependsOnGroups={"testRegisterWithCredentials"},*/
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testIdentityRegenerateWithUsernameAndPaswordFromADifferentOwner() throws Exception {

		// start fresh by unregistering and registering
		clienttasks.unregister(null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null));
		//String ownerKey = CandlepinTasks.getOwnerOfConsumerId(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, consumerId).getString("key");
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);

		// find a different username from the registrationDataList whose owner does not match the registerer of this client
		List<RegistrationData> registrationData = findGoodRegistrationData(false,sm_clientUsername,false,sm_clientOrg);
		if (registrationData.isEmpty()) throw new SkipException("Could not find registration data for a different user who does not belong to owner '"+ownerKey+"'.");

		for (RegistrationData registrationDatum : registrationData) {
			// retrieve the identity using the same username and password as used during register... and assert
			log.info("Attempting to regenerate identity with an invalid username and password...");
			SSHCommandResult identityResult = clienttasks.identity_(registrationDatum.username,registrationDatum.password,Boolean.TRUE, Boolean.TRUE, null, null, null, null);
			Assert.assertNotSame(identityResult.getExitCode(), Integer.valueOf(0), "The identify command was NOT a success.");
			//Assert.assertEquals(identityResult.getStderr().trim(),"access denied.");
			//Assert.assertEquals(identityResult.getStderr().trim(),"Insufficient permissions");	// server response 403 Forbidden
			Assert.assertEquals(identityResult.getStderr().trim(), String.format("Consumer with id %s could not be found.",consumerId));	// new server response 404 Not Found from candlepin pull request https://github.com/candlepin/candlepin/pull/444 'Update auth system to allow "my system" administrators'
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36540", "RHEL7-51315"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: identity regenerate with invalid username and password (attempt with and without force) (negative test)",
			groups={"Tier2Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testIdentityRegenerateWithInvalidUsernameAndPasword() {
		
		// start fresh by unregistering and registering
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		// retrieve the identity using the same username and password as used during register... and assert
		log.info("Attempting to regenerate identity with an invalid username and password...");
		// first attempt without --force
		SSHCommandResult identityResult = clienttasks.identity_("FOO","BAR",Boolean.TRUE, null, null, null, null, null);
		Assert.assertNotSame(identityResult.getExitCode(), Integer.valueOf(0), "The identify command was NOT a success.");
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(identityResult.getStderr().trim(),"--username and --password can only be used with --force");
		} else {
			Assert.assertEquals(identityResult.getStdout().trim(),"--username and --password can only be used with --force");
		}
		// now attempt with --force
		identityResult = clienttasks.identity_("FOO","BAR",Boolean.TRUE, Boolean.TRUE, null, null, null, null);
		Assert.assertNotSame(identityResult.getExitCode(), Integer.valueOf(0), "The identify command was NOT a success.");
		Assert.assertContainsMatch(identityResult.getStderr().trim(),servertasks.invalidCredentialsRegexMsg(),"The stderr expresses a message such that authentication credentials are invalid.");
	}
	
	
	
	@BeforeGroups(groups={"setup"}, value={"VerifyIdentityIsBackedUpWhenConsumerIsDeletedServerSide_Test"})
	public void beforeVerifyIdentityIsBackedUpWhenConsumerIsDeletedServerSide_Test() {
		if (clienttasks!=null) {
			origConsumerCertDir = clienttasks.consumerCertDir;
		}
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19962", "RHEL7-51006"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: assert that the consumer cert is backed up when a server-side deletion is detected.",
			groups={"Tier1Tests","VerifyIdentityIsBackedUpWhenConsumerIsDeletedServerSide_Test","blockedByBug-814466","blockedByBug-813296","blockedByBug-838187","blockedByBug-852706","blockedByBug-872847","blockedByBug-894633","blockedByBug-907638","blockedByBug-822402","blockedByBug-986572","blockedByBug-1000301","blockedByBug-1026435","blockedByBug-1019747","blockedByBug-1026501","blockedByBug-1366301","blockedByBug-1397201"},
			dataProvider="getConsumerCertDirData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testIdentityIsBackedUpWhenConsumerIsDeletedServerSide(Object bugzilla, String consumerCertDir) throws Exception {
		
		// set the rhsm.consumerCertDir (DO NOT USE SubscriptionTasks.config(...))
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile,"consumerCertDir",consumerCertDir);
		
		// register and remember the original consumer identity
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		clienttasks.config(null, null, true, new String[]{"rhsmcertd",/*"certFrequency" CHANGED BY BUG 882459 TO*/"certCheckInterval".toLowerCase(),"240"});
		clienttasks.restart_rhsmcertd(null, null, true);	// make sure that rhsmcertd will not interfere with test
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		String consumerCert_md5sum = client.runCommandAndWait("md5sum "+clienttasks.consumerCertFile()).getStdout().trim();
		String consumerKey_md5sum = client.runCommandAndWait("md5sum "+clienttasks.consumerKeyFile()).getStdout().trim();
		
		// Subscribe to a randomly available pool...
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.size()<=0) throw new SkipException("No susbcriptions are available for which an entitlement could be granted.");
		log.info("Subscribe to a randomly available pool...");
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool,/*sm_serverAdminUsername*/sm_clientUsername,/*sm_serverAdminPassword*/sm_clientPassword,sm_serverUrl);
		//EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		
		// do a server-side consumer deletion 
		// # curl -k -u testuser1:password --request DELETE https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/consumers/8511a2a6-c2ec-4612-8186-af932a3b97cf
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerCert.consumerid);
		sleep(60*1000);	// give the server a chance to complete the job request	// TODO change this hard sleep to wait for a finished job status
		
		// assert that all subscription-manager calls are blocked by a message stating that the consumer has been deleted
		// Original Stderr: Consumer with id b0f1ed9f-3dfa-4eea-8e04-72ab8075d533 could not be found
		String expectedMsg = String.format("Consumer %s has been deleted",consumerCert.consumerid);
		if (!clienttasks.workaroundForBug876764(sm_serverType)) expectedMsg = String.format("Unit %s has been deleted",consumerCert.consumerid);
		String ignoreStderr = "stty: standard input: Invalid argument";
		SSHCommandResult result;
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode=new Integer(70/*EX_SOFTWARE*/);	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		result = clienttasks.identity_(null,null,null,null,null,null,null, null);
		Assert.assertEquals(result.getExitCode(),expectedExitCode,	"Exitcode expected after the consumer has been deleted on the server-side.");
		Assert.assertEquals(result.getStdout().trim(),"",			"Stdout expected after the consumer has been deleted on the server-side.");
		Assert.assertEquals(result.getStderr().trim(),expectedMsg,	"Stderr expected after the consumer has been deleted on the server-side.");
		
		result = clienttasks.refresh_(null, null, null, null);
		Assert.assertEquals(result.getExitCode(),expectedExitCode,	"Exitcode expected after the consumer has been deleted on the server-side.");
		Assert.assertEquals(result.getStdout().trim(),"",			"Stdout expected after the consumer has been deleted on the server-side.");
		Assert.assertEquals(result.getStderr().trim(),expectedMsg,	"Stderr expected after the consumer has been deleted on the server-side.");
		
		result = clienttasks.service_level_(null,null,null,null,null,null,null,null, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(),expectedExitCode,	"Exitcode expected after the consumer has been deleted on the server-side.");
		Assert.assertEquals(result.getStdout().trim(),"",			"Stdout expected after the consumer has been deleted on the server-side.");
		Assert.assertEquals(result.getStderr().trim(),expectedMsg,	"Stderr expected after the consumer has been deleted on the server-side.");
		
		result = clienttasks.facts_(null, true, null, null, null, null);	// Bug 798788:  Error updating system data, see /var/log/rhsm/rhsm.log for more details.
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "798788"; boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping stderr assertion from subscription-manager facts --update.");
		} else {
		// END OF WORKAROUND
		Assert.assertEquals(result.getExitCode(),expectedExitCode,	"Exitcode expected after the consumer has been deleted on the server-side.");
		Assert.assertEquals(result.getStdout().trim(),"",			"Stdout expected after the consumer has been deleted on the server-side.");
		Assert.assertEquals(result.getStderr().trim(),expectedMsg,	"Stderr expected after the consumer has been deleted on the server-side.");
		}
		
		result = clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(),expectedExitCode,	"Exitcode expected after the consumer has been deleted on the server-side.");
		//Assert.assertEquals(result.getStdout().trim(),expectedMsg,"Stdout expected after the consumer has been deleted on the server-side.");
		//Assert.assertEquals(result.getStderr().trim(),"",			"Stderr expected after the consumer has been deleted on the server-side.");
		Assert.assertEquals(result.getStdout().trim()+result.getStderr().trim(),expectedMsg, "Feedback expected after the consumer has been deleted on the server-side.");
		
		result = clienttasks.list_(null, true, null, null, null, null, null, null, null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.16.4-1")) expectedExitCode=new Integer(69/*EX_UNAVAILABLE*/);	// post commit 616ecda6db6ae8b054d7bbb8ba278bba242f4fd0 bug 1262989
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.16.3-1")) expectedMsg  = String.format("This consumer's profile has been deleted from the server. You can use command clean or unregister to remove local profile.",consumerCert.consumerid);	// post commit 5c48d059bb07b64b92722f249b38aaee7219ab47 bug 1262989
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.16.5-1")) expectedMsg  = String.format("Consumer profile \"%s\" has been deleted from the server. You can use command clean or unregister to remove local profile.",consumerCert.consumerid);	// post commit 3ad13c20f6ab34cf2621bc48cdd7d15a82791d4f bug 1262989
		Assert.assertEquals(result.getExitCode(),expectedExitCode,	"Exitcode expected after the consumer has been deleted on the server-side.");
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.9-1")) {	// post commit a695ef2d1da882c5f851fde90a24f957b70a63ad
			Assert.assertEquals(result.getStdout().trim(),"",			"Stdout expected after the consumer has been deleted on the server-side.");
			Assert.assertEquals(result.getStderr().trim(),expectedMsg,	"Stderr expected after the consumer has been deleted on the server-side.");
		} else {
			Assert.assertEquals(result.getStdout().trim(),expectedMsg,	"Stdout expected after the consumer has been deleted on the server-side.");
			Assert.assertEquals(result.getStderr().trim(),"",			"Stderr expected after the consumer has been deleted on the server-side.");
		}
		
		result = clienttasks.subscribe_(null, null, pool.poolId, null, null, null, null, null, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(),expectedExitCode,	"Exitcode expected after the consumer has been deleted on the server-side.");
		//Assert.assertEquals(result.getStdout().trim(),expectedMsg,"Stdout expected after the consumer has been deleted on the server-side.");
		//Assert.assertEquals(result.getStderr().trim(),"",			"Stderr expected after the consumer has been deleted on the server-side.");
		Assert.assertEquals(result.getStdout().trim()+result.getStderr().trim(),expectedMsg, "Feedback expected after the consumer has been deleted on the server-side.");
		
		if (clienttasks.isPackageVersion("subscription-manager","<","1.16.3-1")) {	// pre commit 5c48d059bb07b64b92722f249b38aaee7219ab47 bug 1262989 which causes a call to unregister to effectively clean the local consumer cert
			result = clienttasks.unregister_(null, null, null, null);
			Assert.assertEquals(result.getExitCode(),expectedExitCode,	"Exitcode expected after the consumer has been deleted on the server-side.");
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.9-1")) {	// post commit a695ef2d1da882c5f851fde90a24f957b70a63ad
				Assert.assertEquals(result.getStdout().trim(),"",			"Stdout expected after the consumer has been deleted on the server-side.");
				Assert.assertEquals(result.getStderr().trim(),expectedMsg,	"Stderr expected after the consumer has been deleted on the server-side.");
			} else {
				Assert.assertEquals(result.getStdout().trim(),expectedMsg,	"Stdout expected after the consumer has been deleted on the server-side.");
				Assert.assertEquals(result.getStderr().trim(),"",			"Stderr expected after the consumer has been deleted on the server-side.");
			}
			
		}
		
		// restart rhsmcertd
 		clienttasks.restart_rhsmcertd(null, null, false);	// assertCertificatesUpdate=false since the consumer has been deleted server side and the cert updates should fail
		// assert that the consumer has been backed up and assert the md5sum matches
		String consumerCertFileOld = clienttasks.consumerCertFile().replace(clienttasks.consumerCertDir, clienttasks.consumerCertDir+".old");
		String consumerCertKeyOld = clienttasks.consumerKeyFile().replace(clienttasks.consumerCertDir, clienttasks.consumerCertDir+".old");
		Assert.assertTrue(RemoteFileTasks.testExists(client, consumerCertFileOld), "For emergency recovery after rhsmcertd triggers, the server-side deleted consumer cert should be copied to: "+consumerCertFileOld);
		Assert.assertTrue(RemoteFileTasks.testExists(client, consumerCertKeyOld), "For emergency recovery after rhsmcertd triggers, the server-side deleted consumer key should be copied to: "+consumerCertKeyOld);
		Assert.assertEquals(client.runCommandAndWait("md5sum "+consumerCertFileOld).getStdout().replaceAll(consumerCertFileOld, "").trim(), consumerCert_md5sum.replaceAll(clienttasks.consumerCertFile(), "").trim(), "After the deleted consumer cert is backed up, its md5sum matches that from the original consumer cert.");
		Assert.assertEquals(client.runCommandAndWait("md5sum "+consumerCertKeyOld).getStdout().replaceAll(consumerCertKeyOld, "").trim(), consumerKey_md5sum.replaceAll(clienttasks.consumerKeyFile(), "").trim(), "After the deleted consumer key is backed up, its md5sum matches that from the original consumer key.");
		
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.16.3-1")) {	// post commit 5c48d059bb07b64b92722f249b38aaee7219ab47 bug 1262989 which causes a call to unregister to effectively clean the local consumer cert
			clienttasks.unregister(null, null, null, null);	// no longer throws an exception when consumer has been deleted consumer side - no reason to - just cleans the local consumer cert
		}
		
		// assert that the system is no longer registered and no entitlements remain
		result = clienttasks.identity_(null,null,null,null,null,null,null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getStderr().trim(),clienttasks.msg_ConsumerNotRegistered,"The system should no longer be registered after rhsmcertd triggers following a server-side consumer deletion.");
		} else {
			Assert.assertEquals(result.getStdout().trim(),clienttasks.msg_ConsumerNotRegistered,"The system should no longer be registered after rhsmcertd triggers following a server-side consumer deletion.");
		}
		Assert.assertTrue(clienttasks.getCurrentEntitlementCertFiles().isEmpty(),"The system should no longer have any entitlements after rhsmcertd triggers following a server-side consumer deletion.");
		
		// add asserts for Bug 1026501 - deleting consumer will move splice identity cert
		// assert that the clienttasks.consumerCertDir remains, but the cert.pem and key.pem are gone
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.14-1")) {
			Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.consumerCertDir), "The original consumer cert dir '"+clienttasks.consumerCertDir+"' should remain after it has been backed up to: "+clienttasks.consumerCertDir+".old");
			Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.consumerCertFile()), "After rhsmcertd triggers, the consumer cert '"+clienttasks.consumerCertFile()+"' should have been deleted.");
			Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.consumerKeyFile()), "After rhsmcertd triggers, the consumer key '"+clienttasks.consumerKeyFile()+"' should have been deleted.");
		}
	}
	@AfterGroups(groups={"setup"}, value={"VerifyIdentityIsBackedUpWhenConsumerIsDeletedServerSide_Test"})
	public void afterVerifyIdentityIsBackedUpWhenConsumerIsDeletedServerSide_Test() {
		if (clienttasks!=null) {
			clienttasks.unregister_(null,null,null, null);
			clienttasks.clean_();
			if (origConsumerCertDir!=null) clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile,"consumerCertDir",origConsumerCertDir);
		}
	}
	protected String origConsumerCertDir = null;
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 607162 - (FAIL)Testcase : 256969 register to Candlepin, then muck with your identity certs (for fun and profit) https://github.com/RedHatQE/rhsm-qe/issues/160
	// TODO Bug 813296 - Consumer could not get correct info after being deleted from server side https://github.com/RedHatQE/rhsm-qe/issues/161
	// TODO Bug 827035 - Teach rhsmcertd to refresh the identity certificate https://github.com/RedHatQE/rhsm-qe/issues/162
	// TODO Bug 827034 - Teach rhsmcertd to refresh the identity certificate https://github.com/RedHatQE/rhsm-qe/issues/162
	// TODO Bug 834558 - Teach rhsmcertd to refresh the identity certificate https://github.com/RedHatQE/rhsm-qe/issues/162
	// TODO Bug 827032 - Support autoregen of identity certificates https://github.com/RedHatQE/rhsm-qe/issues/163
	// TODO Bug 853876 - After deletion of consumer,subscription-manager --register --force says "Consumer <ConsumerID> has been deleted" //done https://github.com/RedHatQE/rhsm-qe/issues/164
	
	
	
	
	
	
	// Configuration Methods ***********************************************************************
	
	@BeforeClass(groups="setup")
	public void setupBeforeClass() throws Exception {
		// alternative to dependsOnGroups={"RegisterWithCredentials_Test"}
		// This allows us to satisfy a dependency on registrationDataList making TestNG add unwanted Test results.
		// This also allows us to individually run this Test Class on Hudson.
		testRegisterWithCredentials(); // needed to populate registrationDataList
	}
	
	@AfterClass(groups="setup")
	public void cleanAfterClass() {
		// use the following to recover bugs like 814466,813296
		if (clienttasks!=null) {
			clienttasks.clean();
		}
	}
	
	@AfterGroups(groups={"setup"},value="RHNClassicTests")
	public void removeRHNSystemIdFileAfterGroups() {
		if (clienttasks!=null) {
			clienttasks.removeRhnSystemIdFile();
		}
	}
	
	// Protected Methods ***********************************************************************


	
	// Data Providers ***********************************************************************

	
	@DataProvider(name="getConsumerCertDirData")
	public Object[][] getConsumerCertDirDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getConsumerCertDirDataAsListOfLists());
	}
	protected List<List<Object>> getConsumerCertDirDataAsListOfLists(){
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;

		// String consumerCertDir
		ll.add(Arrays.asList(new Object[]{null,	clienttasks.consumerCertDir}));
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7)	{// avoid Bug 1030560 - rhsmcertd fails to update when rhsm.consumerCertDir configuration is changed
			ll.add(Arrays.asList(new Object[]{null,	"/etc/pki/consumer_TestDir"}));
		} else {
			ll.add(Arrays.asList(new Object[]{null,	"/tmp/sm-consumerCertDir"}));
		}
		
		return ll;
	}

}
