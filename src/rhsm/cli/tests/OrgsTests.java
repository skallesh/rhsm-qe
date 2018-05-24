package rhsm.cli.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.Org;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.SSHCommandResult;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"OrgsTests"})
public class OrgsTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27119", "RHEL7-51361"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: run the orgs module with valid user credentials and verify the expected organizations are listed",
			groups={"Tier2Tests","blockedByBug-719739","blockedByBug-1254353"/*is a duplicate of*/,"blockedByBug-1254349"},
			dataProvider="getCredentialsForOrgsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testOrgsWithCredentials(String username, String password, List<Org> expectedOrgs) {
		log.info("Testing subscription-manager orgs module using username="+username+" password="+password+" and expecting orgs="+expectedOrgs+" ...");
		
		// use subscription-manager to get the organizations for which the user has access
		SSHCommandResult orgsResult = clienttasks.orgs_(username, password, null, null, null, null, null, null);
		
		// when the expectedOrgs is empty, there is a special message, assert it
		if (expectedOrgs.isEmpty()) {
			//Assert.assertEquals(orgsResult.getStdout().trim(), String.format("%s cannot register to any organizations.", username), "Special message when the expectedOrgs is empty.");	// Bug 903298 - String Update: "Register to" -> "Register with" 
			Assert.assertEquals(orgsResult.getStdout().trim(), String.format("%s cannot register with any organizations.", username), "Special message when the expectedOrgs is empty.");
		}
		
		// parse the actual Orgs from the orgsResult
		List<Org> actualOrgs = Org.parse(orgsResult.getStdout());
		
		// assert that all of the expectedOrgs are included in the actualOrgs
		for (Org expectedOrg : expectedOrgs) {
			Assert.assertTrue(actualOrgs.contains(expectedOrg), "The list of orgs returned by subscription-manager for user '"+username+"' includes expected org: "+expectedOrg);
		}
		Assert.assertEquals(actualOrgs.size(), expectedOrgs.size(),"The number of orgs returned by subscription-manager for user '"+username+"'.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36575", "RHEL7-51376"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: run the orgs module with invalid user credentials",
			groups={"Tier2Tests","blockedByBug-1254353"/*is a duplicate of*/,"blockedByBug-1254349"},
			dataProvider="getInvalidCredentialsForOrgsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testOrgsWithInvalidCredentials(String username, String password) {
		log.info("Testing subscription-manager orgs module using username="+username+" password="+password+" ...");
		
		// use subscription-manager to get the organizations for which the user has access
		SSHCommandResult sshCommandResult = clienttasks.orgs_(username, password, null, null, null, null, null, null);
		
		// assert the sshCommandResult here
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(70);	// EX_SOFTWARE // post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode, "The expected exit code from orgs with invalid credentials.");
		Assert.assertEquals(sshCommandResult.getStdout().trim(), "", "The expected stdout result from orgs with invalid credentials.");
		Assert.assertContainsMatch(sshCommandResult.getStderr().trim(), servertasks.invalidCredentialsRegexMsg(), "The expected stderr result from orgs with invalid credentials.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36574", "RHEL7-51375"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: run the orgs module while prompting for user credentials interactively",
			groups={"Tier2Tests","blockedbyBug-878986"},
			dataProvider = "getInteractiveCredentialsForOrgsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testOrgsWithInteractivePromptingForCredentials(Object bugzilla, String promptedUsername, String promptedPassword, String commandLineUsername, String commandLinePassword, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex) {

		// call orgs while providing a valid username at the interactive prompt
		String command;
		if (client.runCommandAndWait("rpm -q expect").getExitCode().intValue()==0) {	// is expect installed?
			// assemble an ssh command using expect to simulate an interactive supply of credentials to the orgs command
			String promptedUsernames=""; if (promptedUsername!=null) for (String username : promptedUsername.split("\\n")) {
				promptedUsernames += "expect \\\"*Username:\\\"; send "+username+"\\\r;";
			}
			String promptedPasswords=""; if (promptedPassword!=null) for (String password : promptedPassword.split("\\n")) {
				promptedPasswords += "expect \\\"*Password:\\\"; send "+password+"\\\r;";
			}
			// [root@jsefler-onprem-5server ~]# expect -c "spawn subscription-manager environments orgs; expect \"*Username:\"; send qa@redhat.com\r; expect \"*Password:\"; send CHANGE-ME\r; expect eof; catch wait reason; exit [lindex \$reason 3]"
			command = String.format("expect -c \"spawn %s orgs %s %s; %s %s expect eof; catch wait reason; exit [lindex \\$reason 3]\"",
					clienttasks.command,
					commandLineUsername==null?"":"--username="+commandLineUsername,
					commandLinePassword==null?"":"--password="+commandLinePassword,
					promptedUsernames,
					promptedPasswords);
		} else {
			// assemble an ssh command using echo and pipe to simulate an interactive supply of credentials to the orgs command
			String echoUsername= promptedUsername==null?"":promptedUsername;
			String echoPassword = promptedPassword==null?"":promptedPassword;
			String n = (promptedPassword!=null&&promptedUsername!=null)? "\n":"";
			command = String.format("echo -e \"%s\" | %s orgs %s %s",
					echoUsername+n+echoPassword,
					clienttasks.command,
					commandLineUsername==null?"":"--username="+commandLineUsername,
					commandLinePassword==null?"":"--password="+commandLinePassword);
		}
		// attempt orgs with the interactive credentials
		SSHCommandResult sshCommandResult = client.runCommandAndWait(command);
		
		// assert the sshCommandResult here
		if (expectedExitCode!=null) Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode, "The expected exit code from the orgs attempt.");
		if (expectedStdoutRegex!=null) Assert.assertContainsMatch(sshCommandResult.getStdout(), expectedStdoutRegex, "The expected stdout result from orgs while supplying interactive credentials.");
		if (expectedStderrRegex!=null) Assert.assertContainsMatch(sshCommandResult.getStderr(), expectedStderrRegex, "The expected stderr result from orgs while supplying interactive credentials.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36571", "RHEL7-51359"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: attempt to get a bogus owner via the candlepin api",
			groups={"Tier2Tests","blockedByBug-729780","blockedByBug-796468"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testAttemptToGetABogusOwnerViaCandlepinApi() throws Exception {
		String bogusOwner = "bogusOwner";
		JSONObject jsonResponse = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, "/owners/"+bogusOwner));
		
		//Assert.assertEquals(jsonResponse.getString("displayMessage"), "Owner with id "+bogusOwner+" could not be found", "The JSON returned from the candlepin API call should contain a displayMessage stating that the owner/org could not be found.");	// valid prior to Bug 796468 - Owner with id FOO could not be found
		Assert.assertEquals(jsonResponse.getString("displayMessage"), String.format("Organization with id %s could not be found.",bogusOwner), "The JSON returned from the candlepin API call should contain a displayMessage stating that the owner/org could not be found.");
		
		// TODO could also use the RemoteFileTasks to mark the catalina.out file and assert that it contains a 404 response instead of the 403 response as reported in:
		// https://bugzilla.redhat.com/show_bug.cgi?id=729780
		// https://bugzilla.redhat.com/attachment.cgi?id=517680
//		2011-08-10 18:00:31,106 INFO  [STDOUT] Aug 10 18:00:31 [http-10.7.13.82-8080-1] DEBUG org.fedoraproject.candlepin.guice.I18nProvider - Getting i18n engine for locale en_US
//		2011-08-10 18:00:31,154 INFO  [STDOUT] Aug 10 18:00:31 [http-10.7.13.82-8080-1] DEBUG org.fedoraproject.candlepin.servlet.filter.logging.LoggingFilter -
//		====Response====
//		  Status: 403
//		  Content-type: application/json
//		====Response====
//		2011-08-10 18:00:31,154 INFO  [STDOUT] Aug 10 18:00:31 [http-10.7.13.82-8080-1] DEBUG org.fedoraproject.candlepin.servlet.filter.logging.LoggingFilter - ====ResponseBody====
//		2011-08-10 18:00:31,154 INFO  [STDOUT] Aug 10 18:00:31 [http-10.7.13.82-8080-1] DEBUG org.fedoraproject.candlepin.servlet.filter.logging.LoggingFilter - {"displayMessage":"Insufficient permissions"}
	}
	
		
	protected String server_hostname = null;
	protected String server_port = null;
	protected String server_prefix = null;
	@BeforeGroups(value={"OrgsWithServerurl_Test"}, groups={"setup"})
	public void beforeOrgsWithServerurl_Test() {
		if (clienttasks==null) return;
		server_hostname	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname");
		server_port		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port");
		server_prefix	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "prefix");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36576", "RHEL7-51377"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: orgs with --serverurl",
			dataProvider="getServerurlData",
			groups={"Tier2Tests","OrgsWithServerurl_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testOrgsWithServerurl(Object bugzilla, String serverurl, String expectedHostname, String expectedPort, String expectedPrefix, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrMatch) {
		// get original server at the beginning of this test
		String hostnameBeforeTest	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname");
		String portBeforeTest		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port");
		String prefixBeforeTest		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix");
		
		// orgs with a serverurl
		SSHCommandResult sshCommandResult = clienttasks.orgs_(sm_clientUsername,sm_clientPassword,serverurl, null, null, null, null, null);
		
		// assert the sshCommandResult here
		if (expectedExitCode!=null)	Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode,"ExitCode after register with --serverurl="+serverurl+" and other options:");
		if (expectedStdoutRegex!=null)	Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), expectedStdoutRegex,"Stdout after register with --serverurl="+serverurl+" and other options:");
		if (expectedStderrMatch!=null)	Assert.assertContainsMatch(sshCommandResult.getStderr().trim(), expectedStderrMatch,"Stderr after register with --serverurl="+serverurl+" and other options:");
		Assert.assertContainsNoMatch(sshCommandResult.getStderr().trim(), "Traceback.*","Stderr after register with --serverurl="+serverurl+" and other options should not contain a Traceback.");
		
		// negative testcase assertions........
		//if (expectedExitCode.equals(new Integer(255))) {
		if (Integer.valueOf(expectedExitCode)>1) {
			// assert that the current config remains unchanged when the expectedExitCode is 255
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname"), hostnameBeforeTest, "The "+clienttasks.rhsmConfFile+" configuration for [server] hostname should remain unchanged when attempting to register with an invalid serverurl.");
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port"),	portBeforeTest, "The "+clienttasks.rhsmConfFile+" configuration for [server] port should remain unchanged when attempting to register with an invalid serverurl.");
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix"), prefixBeforeTest, "The "+clienttasks.rhsmConfFile+" configuration for [server] prefix should remain unchanged when attempting to register with an invalid serverurl.");
						
			return;	// nothing more to do after these negative testcase assertions
		}
		
		// positive testcase assertions........
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname"), expectedHostname, "The "+clienttasks.rhsmConfFile+" configuration for [server] hostname has been updated from the specified --serverurl "+serverurl);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port"), expectedPort, "The "+clienttasks.rhsmConfFile+" configuration for [server] port has been updated from the specified --serverurl "+serverurl);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix"), expectedPrefix, "The "+clienttasks.rhsmConfFile+" configuration for [server] prefix has been updated from the specified --serverurl "+serverurl);
	}
	@AfterGroups(value={"OrgsWithServerurl_Test"},groups={"setup"})
	public void afterOrgsWithServerurl_Test() {
		if (server_hostname!=null)	clienttasks.config(null,null,true,new String[]{"server","hostname",server_hostname});
		if (server_port!=null)		clienttasks.config(null,null,true,new String[]{"server","port",server_port});
		if (server_prefix!=null)	clienttasks.config(null,null,true,new String[]{"server","prefix",server_prefix});
	}
	
	
	protected String rhsm_ca_cert_dir = null;
	@BeforeGroups(value={"OrgsWithInsecure_Test"}, groups={"setup"})
	public void beforeOrgsWithInsecure_Test() {
		if (clienttasks==null) return;
		rhsm_ca_cert_dir	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "ca_cert_dir");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36573", "RHEL7-51374"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: orgs with --insecure",
			groups={"Tier2Tests","OrgsWithInsecure_Test","blockedByBug-844411","blockedByBug-993202","blockedByBug-1254353"/*is a duplicate of*/,"blockedByBug-1254349"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testOrgsWithInsecure() {
		SSHCommandResult sshCommandResult;
		
		// calling orgs without insecure should pass
		sshCommandResult = clienttasks.orgs(sm_clientUsername,sm_clientPassword, null, false, null, null, null, null);
		
		// change the rhsm.ca_cert_dir configuration to simulate a missing candlepin ca cert
		client.runCommandAndWait("mkdir -p /tmp/emptyDir");
		sshCommandResult = clienttasks.config(null, null, true, new String[]{"rhsm","ca_cert_dir","/tmp/emptyDir"});
		
		// calling orgs without insecure should now fail (throwing stderr "certificate verify failed")
		sshCommandResult = clienttasks.orgs_(sm_clientUsername,sm_clientPassword, null, false, null, null, null, null);
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(70);	// EX_SOFTWARE	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode, "Exitcode from the orgs command when configuration rhsm.ca_cert_dir has been falsified.");
		if (clienttasks.isPackageVersion("python-rhsm",">=","1.18.5-1") && Integer.valueOf(clienttasks.redhatReleaseX)>=7) {	// post python-rhsm commit 214103dcffce29e31858ffee414d79c1b8063970   Reduce usage of m2crypto (#184) (RHEL7+)
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "Unable to verify server's identity: [SSL: CERTIFICATE_VERIFY_FAILED] certificate verify failed (_ssl.c:579)", "Stderr from the orgs command when configuration rhsm.ca_cert_dir has been falsified.");
			Assert.assertEquals(sshCommandResult.getStdout().trim(), "", "Stdout from the orgs command when configuration rhsm.ca_cert_dir has been falsified.");
		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.9-1")) {	// post commit a695ef2d1da882c5f851fde90a24f957b70a63ad
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "Unable to verify server's identity: certificate verify failed", "Stderr from the orgs command when configuration rhsm.ca_cert_dir has been falsified.");
			Assert.assertEquals(sshCommandResult.getStdout().trim(), "", "Stdout from the orgs command when configuration rhsm.ca_cert_dir has been falsified.");
		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.9-1")) {	// post commit 3366b1c734fd27faf48313adf60cf051836af115
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "Stderr from the orgs command when configuration rhsm.ca_cert_dir has been falsified.");
			Assert.assertEquals(sshCommandResult.getStdout().trim(), "Unable to verify server's identity: certificate verify failed", "Stdout from the orgs command when configuration rhsm.ca_cert_dir has been falsified.");
		} else {
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "certificate verify failed", "Stderr from the orgs command when configuration rhsm.ca_cert_dir has been falsified.");
			Assert.assertEquals(sshCommandResult.getStdout().trim(), "", "Stdout from the orgs command when configuration rhsm.ca_cert_dir has been falsified.");
		}
		
		// calling orgs with insecure should now pass
		sshCommandResult = clienttasks.orgs(sm_clientUsername,sm_clientPassword, null, true, null, null, null, null);
		
		// assert that option --insecure did NOT persist to rhsm.conf
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "insecure"), "0", "Expected value of "+clienttasks.rhsmConfFile+" server.insecure configuration.  Use of the --insecure option when calling the orgs module should NOT be persisted to rhsm.conf as true.");
	}
	@AfterGroups(value={"OrgsWithInsecure_Test"},groups={"setup"})
	public void afterOrgsWithInsecure_Test() {
		if (rhsm_ca_cert_dir!=null) clienttasks.config(null, null, true, new String[]{"rhsm","ca_cert_dir",rhsm_ca_cert_dir});
	}
	
	
	@BeforeGroups(value={"AttemptToAttachSubscriptionsFromOtherOrgs_Test"}, groups={"setup"})
	public void beforeAttemptToAttachSubscriptionsFromOtherOrgs_Test() throws Exception {
		// alternative to dependsOnGroups={"RegisterWithCredentials_Test"}
		// This allows us to satisfy a dependency on registrationDataList making TestNG add unwanted Test results.
		// This also allows us to individually run this Test Class on Hudson.
		testRegisterWithCredentials(); // needed to populate registrationDataList
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36570", "RHEL7-51358"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="consumers should NOT be able to attach subscriptions from other orgs",
			groups={"Tier2Tests","AttemptToAttachSubscriptionsFromOtherOrgs_Test","blockedByBug-994711"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testAttemptToAttachSubscriptionsFromOtherOrgs() throws JSONException, Exception {
		boolean skipTest = true;
		Set<String> poolIdsTested = new HashSet<String>();
		
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		String ownerKey = clienttasks.getCurrentlyRegisteredOwnerKey();
		for (RegistrationData registrationDatum :  getRandomSubsetOfList(findGoodRegistrationData(false, sm_clientUsername,false, ownerKey),3)) {
			for (SubscriptionPool subscriptionPool : getRandomSubsetOfList(registrationDatum.allAvailableSubscriptionPools,3)) {
				if (!poolIdsTested.contains(subscriptionPool.poolId)) {
					SSHCommandResult sshCommandResult = clienttasks.subscribe_(false, null, subscriptionPool.poolId, null, null, null, null, null, null, null, null, null, null);
					//Assert.assertEquals(sshCommandResult.getStdout().trim(), "Insufficient permissions", "Stdout from an attempt to subscribe to pool id '"+subscriptionPool.poolId+"' belonging to a different org '"+registrationDatum.ownerKey+"'.");	// server response 403 Forbidden
					Assert.assertEquals(sshCommandResult.getStdout().trim(), String.format("Pool with id %s could not be found.",subscriptionPool.poolId), "Stdout from an attempt to subscribe to pool id '"+subscriptionPool.poolId+"' belonging to a different org '"+registrationDatum.ownerKey+"'.");	// new server response 404 Not Found from candlepin pull request https://github.com/candlepin/candlepin/pull/444 'Update auth system to allow "my system" administrators'
					Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "Stderr from an attempt to subscribe to pool id '"+subscriptionPool.poolId+"' belonging to a different org '"+registrationDatum.ownerKey+"'.");
					Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(1), "Exitcode from an attempt to subscribe to pool id '"+subscriptionPool.poolId+"' from a different org '"+registrationDatum.ownerKey+"'.");
					Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().isEmpty(), "The currently consumed subscriptions should be empty.");
					
					poolIdsTested.add(subscriptionPool.poolId);	// remember what pool ids were tested so we only test it once
					skipTest = false;
				}
			}
		}
		if (skipTest) throw new SkipException("Could not find any subscription pools from orgs other than '"+ownerKey+"' to execute this test.");		
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36572", "RHEL7-51360"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="create an owner via the candlepin api and then update fields on the owner",
			groups={"Tier2Tests","CreateAnOwnerAndSetAttributesOnTheOwner_Test", "blockedByBug-1563003","blockedByBug-1565066"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testCreateAnOwnerAndSetAttributesOnTheOwner() throws Exception {
		String mother="mother", daughter="daughter";
		String result;
		JSONObject jsonOwner;
		
		//	Example Owner...
		//	[root@jsefler-7 ~]# curl --stderr /dev/null --insecure --user admin:admin --request GET https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/owners/daughter | python -m simplejson/tool{
		//	    "contentPrefix": null,
		//	    "created": "2014-03-14T12:49:13.558+0000",
		//	    "defaultServiceLevel": null,
		//	    "displayName": "Orphan Annie",
		//	    "href": "/owners/daughter",
		//	    "id": "8a9087e3448ecab80144c0a3a3566696",
		//	    "key": "daughter",
		//	    "logLevel": null,
		//	    "parentOwner": null,
		//	    "updated": "2014-03-14T12:49:13.558+0000",
		//	    "upstreamConsumer": null
		//	}
		
		// an orphan girl is born with no mother...
		jsonOwner = CandlepinTasks.createOwnerUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, daughter, "Orphan Annie",null, null, null, null);
		Assert.assertNotNull(jsonOwner.getString("id"), "The candlepin API appears to have created a new owner: "+jsonOwner);
	
		// a loving mother searches for a daughter...
		jsonOwner = CandlepinTasks.createOwnerUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, mother, "Mrs. Jones","TLC (Tender Loving Care)", null, null, null);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.4-1"/*TODO CHANGE TO "2.3.5-1" ONCE TAGGED*/)) {	// commit 2bfd5488c0566a1b3551007901cb978c37b51a57 Improved error output surrounding owner creation and service levels	// Bug 1563003 - Service level "Merry Maid Service" is not available to units of organization null. 
			// assert we can no longer specify a defaultServiceLevel with owner creation
			Assert.assertTrue(jsonOwner.has("displayMessage"), "Expecting attempt to POST a new owner with any defaultServiceLevel to fail with a display message as decided in https://bugzilla.redhat.com/show_bug.cgi?id=1563003#c1");
			Assert.assertEquals(jsonOwner.getString("displayMessage"), "The default service level cannot be specified during owner creation");
		} else {
			Assert.assertNotNull(jsonOwner.getString("id"), "The candlepin API appears to have created a new owner with a defaultServiceLevel: "+jsonOwner);
		}
		
		// the mother adopts the daughter...
		/* The following throws a Runtime Error; 3/14/2014 Development says the parentOwner attribute was never completely developed.  Do not test it.
		jsonOwner = CandlepinTasks.setAttributeForOrg(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, daughter, "parentOwner", mother);
		[root@jsefler-7 ~]# curl --stderr /dev/null --insecure --user admin:admin --request PUT --data '{"parentOwner":"mother"}' --header 'accept: application/json' --header 'content-type: application/json' https://jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin/owners/daughter | python -m simplejson/tool
		{
		    "displayMessage": "Runtime Error org.hibernate.TransientPropertyValueException: object references an unsaved transient instance - save the transient instance before flushing: org.candlepin.model.Owner.parentOwner -> org.candlepin.model.Owner at org.hibernate.engine.spi.CascadingAction$8.noCascade:380",
		    "requestUuid": "6169b416-6302-4dbd-a412-c8d55a723391"
		}
		Assert.assertEquals(jsonOwner.getString("parentOwner"), "mother", "The candlepin API appears to have updated the parentOwner: "+jsonOwner);
		*/
		jsonOwner = CandlepinTasks.setAttributeForOrg(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, daughter, "displayName", "Annie Jones");
		Assert.assertEquals(jsonOwner.getString("displayName"), "Annie Jones", "The candlepin API appears to have updated the displayName: "+jsonOwner);
		String defaultServiceLevel = "Eternal Gratitude";
		String expectedMsg = String.format("Service level '%s' is not available to units of organization %s.", defaultServiceLevel,daughter);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
			expectedMsg = String.format("Service level \"%s\" is not available to units of organization %s.", defaultServiceLevel,daughter);
		}
		try {
			jsonOwner = CandlepinTasks.setAttributeForOrg(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, daughter, "defaultServiceLevel", defaultServiceLevel);
			Assert.fail("Expected an attempt to update org with an unavailable service level to fail.");
		} catch (AssertionError ae) {
			Assert.assertEquals(ae.getMessage(), "Attempt to update org 'daughter' failed: "+expectedMsg);
		}
	}
	@BeforeGroups(value={"CreateAnOwnerAndSetAttributesOnTheOwner_Test"},groups={"setup"})
	@AfterGroups(value={"CreateAnOwnerAndSetAttributesOnTheOwner_Test"},groups={"setup"})
	public void afterCreateAnOwnerAndSetAttributesOnTheOwner_Test() throws Exception {
		// delete owners mother and daughter after this test because they cause unnecessary failures
		// in ServiceLevelTests because: Service level 'TLC (Tender Loving Care)' is not available to units of organization mother.
		String result;
		result = CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, "/owners/"+"daughter");
		// result is null when successful or {"displayMessage":"owner with key: daughter was not found.","requestUuid":"9bad7da4-7148-40c3-bd2e-77edae19e267"}
		result = CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, "/owners/"+"mother");
		// result is null when successful or {"displayMessage":"owner with key: daughter was not found.","requestUuid":"9bad7da4-7148-40c3-bd2e-77edae19e267"}
	}
	
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 789344 - subscription-manager doesn't support spaces in --org parameter unless --environment parameter is given https://github.com/RedHatQE/rhsm-qe/issues/178
	
	// Configuration methods ***********************************************************************

	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

	
	@DataProvider(name="getCredentialsForOrgsData")
	public Object[][] getCredentialsForOrgsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getCredentialsForOrgsDataAsListOfLists());
	}
	protected List<List<Object>> getCredentialsForOrgsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		// Notes...
		// curl -k -u admin:admin https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/users | python -mjson.tool
		// curl -k -u admin:admin https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/users/testuser1 | python -mjson.tool
		// curl -k -u admin:admin https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/users/testuser1/owners | python -mjson.tool

		// get all of the candlepin users
		// curl -k -u admin:admin https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/users | python -mjson.tool
		JSONArray jsonUsers = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,"/users"));	
		for (int i = 0; i < jsonUsers.length(); i++) {
			JSONObject jsonUser = (JSONObject) jsonUsers.get(i);
			// {
			//   "created": "2011-07-01T06:40:00.951+0000", 
			//   "hashedPassword": "05557a2aaec7cb676df574d2eb080691949a6752", 
			//   "id": "8a90f8c630e46c7e0130e46ce9b70020", 
			//   "superAdmin": false, 
			//   "updated": "2011-07-01T06:40:00.951+0000", 
			//   "username": "minnie"
			// }
			Boolean isSuperAdmin = jsonUser.getBoolean("superAdmin");
			String username = jsonUser.getString("username");
			String password = sm_clientPasswordDefault;
			if (username.equals(sm_serverAdminUsername)) password = sm_serverAdminPassword;
			
			// get the user's owners
			// curl -k -u testuser1:password https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/users/testuser1/owners | python -mjson.tool
			JSONArray jsonUserOwners = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(username,password,sm_serverUrl,"/users/"+username+"/owners"));	
			List<Org> orgs = new ArrayList<Org>();
			for (int j = 0; j < jsonUserOwners.length(); j++) {
				JSONObject jsonOwner = (JSONObject) jsonUserOwners.get(j);
				// {
				//    "contentPrefix": null, 
				//    "created": "2011-07-01T06:39:58.740+0000", 
				//    "displayName": "Snow White", 
				//    "href": "/owners/snowwhite", 
				//    "id": "8a90f8c630e46c7e0130e46ce114000a", 
				//    "key": "snowwhite", 
				//    "parentOwner": null, 
				//    "updated": "2011-07-01T06:39:58.740+0000", 
				//    "upstreamUuid": null
				// }
				String orgKey = jsonOwner.getString("key");
				String orgName = jsonOwner.getString("displayName");
				orgs.add(new Org(orgKey,orgName));
			}
			
			// String username, String password, List<Org> orgs
			ll.add(Arrays.asList(new Object[]{username,password,orgs}));
		}
		
		return ll;
	}
	
	
	@DataProvider(name="getInvalidCredentialsForOrgsData")
	public Object[][] getInvalidCredentialsForOrgsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getInvalidCredentialsForOrgsDataAsListOfLists());
	}
	protected List<List<Object>> getInvalidCredentialsForOrgsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		String x = String.valueOf(getRandInt());
		
		// String username, String password
		ll.add(Arrays.asList(new Object[]{	sm_clientUsername+x,	sm_clientPassword}));
		ll.add(Arrays.asList(new Object[]{	sm_clientUsername,		sm_clientPassword+x}));
		ll.add(Arrays.asList(new Object[]{	sm_clientUsername+x,	sm_clientPassword+x}));
		
		return ll;
	}
	
	
	@DataProvider(name="getInteractiveCredentialsForOrgsData")
	public Object[][] getInteractiveCredentialsForOrgsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getInteractiveCredentialsForOrgsDataAsListOfLists());
	}
	protected List<List<Object>> getInteractiveCredentialsForOrgsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (servertasks==null) return ll;
		if (clienttasks==null) return ll;
		
		String uErrMsg = servertasks.invalidCredentialsRegexMsg();
		String x = String.valueOf(getRandInt());
		if (clienttasks.isPackageInstalled("expect")) {
			// Object bugzilla, String promptedUsername, String promptedPassword, String commandLineUsername, String commandLinePassword, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername,			null,						null,				sm_clientPassword,	new Integer(0),		sm_clientUsername+" Organizations",				null}));
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername+x,		null,						null,				sm_clientPassword,	new Integer(255),	uErrMsg,										null}));
			ll.add(Arrays.asList(new Object[] {	null,	null,						sm_clientPassword,			sm_clientUsername,	null,				new Integer(0),		sm_clientUsername+" Organizations",				null}));
			ll.add(Arrays.asList(new Object[] {	null,	null,						sm_clientPassword+x,		sm_clientUsername,	null,				new Integer(255),	uErrMsg,										null}));
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername,			sm_clientPassword,			null,				null,				new Integer(0),		sm_clientUsername+" Organizations",				null}));
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername+x,		sm_clientPassword+x,		null,				null,				new Integer(255),	uErrMsg,										null}));
			ll.add(Arrays.asList(new Object[] {	null,	"\n\n"+sm_clientUsername,	"\n\n"+sm_clientPassword,	null,				null,				new Integer(0),		"(\nUsername: ){3}"+sm_clientUsername+"(\nPassword: ){3}"+"\\n\\+-+\\+\\n"+" *"+sm_clientUsername+" Organizations",	null}));
		} else {
			// Object bugzilla, String promptedUsername, String promptedPassword, String commandLineUsername, String commandLinePassword, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername,			null,						null,				sm_clientPassword,	new Integer(0),		sm_clientUsername+" Organizations",				null}));
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername+x,		null,						null,				sm_clientPassword,	new Integer(255),	null,																		uErrMsg}));
			ll.add(Arrays.asList(new Object[] {	null,	null,						sm_clientPassword,			sm_clientUsername,	null,				new Integer(0),		sm_clientUsername+" Organizations",				null}));
			ll.add(Arrays.asList(new Object[] {	null,	null,						sm_clientPassword+x,		sm_clientUsername,	null,				new Integer(255),	null,																		uErrMsg}));
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername,			sm_clientPassword,			null,				null,				new Integer(0),		sm_clientUsername+" Organizations",				null}));
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername+x,		sm_clientPassword+x,		null,				null,				new Integer(255),	null,																		uErrMsg}));
			ll.add(Arrays.asList(new Object[] {	null,	"\n\n"+sm_clientUsername,	"\n\n"+sm_clientPassword,	null,				null,				new Integer(0),		"(Username: ){3}.*\\n.*"+sm_clientUsername+" Organizations",	"(Warning: Password input may be echoed.\nPassword: \n){3}"}));
		}
		
		// for all rows with expectedExitCode=255, change the expected exitCode when testing post subscription-manager-1.13.8-1
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			for (List<Object> l : ll) {
				// Object bugzilla, String promptedUsername, String promptedPassword, String commandLineUsername, String commandLinePassword, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex
				if (((Integer)l.get(5)).equals(255)) {
					BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);	// get the existing BlockedByBzBug
					List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
					bugIds.add("1119688");	// Bug 1119688 - [RFE] subscription-manager better usability for scripts
					blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
					l.set(0, blockedByBzBug);
					l.set(5, new Integer(70));	// EX_SOFTWARE
				}
			}
		}
		
		return ll;
	}

}
