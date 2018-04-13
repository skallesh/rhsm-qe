package rhsm.cli.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.jul.TestRecords;

import rhsm.base.CandlepinType;
import rhsm.base.ConsumerType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ConsumerCert;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.LinkedItem;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;

/**
 * @author ssalevan
 * @author jsefler
 *
 */
@Test(groups={"RegisterTests"})
public class RegisterTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************
	
	
	
	@BeforeGroups(value={"RegisterWithCredentials_Test"},alwaysRun=true)
	public void beforeRegisterWithCredentials_Test() {
		if (clienttasks==null) return;
		clienttasks.unregister_(null, null, null, null);
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19951", "RHEL7-33090"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager-cli: register to a Candlepin server",
			groups={"Tier1Tests","RegisterWithCredentials_Test"},
			dataProvider="getRegisterCredentialsData")
	@ImplementsNitrateTest(caseId=41677)
	public void testRegisterWithCredentials(String username, String password, String org) {
		log.info("Testing registration to a Candlepin using username="+username+" password="+password+" org="+org+" ...");
		
		// cleanup from last test when needed
		clienttasks.unregister_(null, null, null, null);
		
		// determine this user's ability to register
		SSHCommandResult registerResult = clienttasks.register_(username, password, org, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
			
		// determine this user's available subscriptions
		List<SubscriptionPool> allAvailableSubscriptionPools=null;
		if (registerResult.getExitCode()==0) {
			allAvailableSubscriptionPools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		}
		
		// determine this user's owner
		String ownerKey = null;
		if (registerResult.getExitCode()==0) {
			String consumerId = clienttasks.getCurrentConsumerId(registerResult);	// c48dc3dc-be1d-4b8d-8814-e594017d63c1 testuser1
			try {
				ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(username,password,sm_serverUrl,consumerId);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		RegistrationData userData = new RegistrationData(username,password,ownerKey,registerResult,allAvailableSubscriptionPools);
		registrationDataList.add(userData);
		clienttasks.unregister_(null, null, null, null);
		
		// when no org was given by the dataprovider, then this user must have READ_ONLY access to any one or more orgs
		if (org==null) {
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=718205 - jsefler 07/01/2011
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="718205"; 
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				// When org==null, then this user has no access to any org/owner
				// 1. the user has only READ_ONLY access to one org:
				//		exitCode=1 stdout= User dopey cannot access organization/owner snowwhite
				// 2. the user has only READ_ONLY access to more than one org:
				//		exitCode=1 stdout= You must specify an organization/owner for new consumers.
				// Once a Candlepin API is in place to figure this out, fix the OR in the Assert.assertContainsMatch(...)
				Assert.assertContainsMatch(registerResult.getStderr().trim(), "User "+username+" cannot access organization/owner \\w+|You must specify an organization/owner for new consumers.");	// User testuser3 cannot access organization/owner admin	// You must specify an organization/owner for new consumers.
				Assert.assertFalse(registerResult.getExitCode()==0, "The exit code indicates that the register attempt was NOT a success for a READ_ONLY user.");
				return;
			}
			// END OF WORKAROUND
			
			//Assert.assertEquals(registerResult.getStderr().trim(), String.format("%s cannot register to any organizations.", username), "Error message when READ_ONLY user attempts to register.");	// Bug 903298 - String Update: "Register to" -> "Register with"
			Assert.assertEquals(registerResult.getStderr().trim(), String.format("%s cannot register with any organizations.", username), "Error message when READ_ONLY user attempts to register.");
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.14.7-1")) {	// post commit 270f2a3e5f7d55b69a6f98c160d38362961b3059 Specified error codes on system_exit in rhn-migrate-classic-to-rhsm
				Assert.assertEquals(registerResult.getExitCode(), Integer.valueOf(1), "The exit code indicates that the register attempt was NOT a success for a READ_ONLY user.");
			} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
				Assert.assertEquals(registerResult.getExitCode(), Integer.valueOf(70)/*EX_SOFTWARE*/, "The exit code indicates that the register attempt was NOT a success for a READ_ONLY user.");
			} else {
				Assert.assertEquals(registerResult.getExitCode(), Integer.valueOf(255), "The exit code indicates that the register attempt was NOT a success for a READ_ONLY user.");
			}
			return;
		}
		Assert.assertEquals(registerResult.getExitCode(), Integer.valueOf(0), "The exit code indicates that the register attempt was a success.");
		//Assert.assertContainsMatch(registerResult.getStdout().trim(), "[a-f,0-9,\\-]{36} "+/*username*/clienttasks.hostname);	// applicable to RHEL61 and RHEL57 
		//Assert.assertContainsMatch(registerResult.getStdout().trim(), "The system has been registered with id: [a-f,0-9,\\-]{36}");	// msgid changed by bug 878634
		Assert.assertContainsMatch(registerResult.getStdout().trim(), "The system has been registered with ID: [a-f,0-9,\\-]{36}");
	}
	@AfterGroups(value={"RegisterWithCredentials_Test"},alwaysRun=true)
	public void generateRegistrationReportTableAfterRegisterWithCredentials_Test() {
		
		// now dump out the list of userData to a file
	    File file = new File("test-output/registration_report.html"); // this will be in the automation.dir directory on hudson (workspace/automatjon/sm)
	    DateFormat dateFormat = new SimpleDateFormat("MMM d HH:mm:ss yyyy z");
	    try {
	    	Writer output = new BufferedWriter(new FileWriter(file));
			
			// write out the rows of the table
			output.write("<html>\n");
			output.write("<table border=1>\n");
			output.write("<h2>Candlepin Registration Report</h2>\n");
			//output.write("<h3>(generated on "+dateFormat.format(System.currentTimeMillis())+")</h3>");
			output.write("Candlepin hostname= <b>"+sm_serverHostname+"</b><br>\n");
			output.write(dateFormat.format(System.currentTimeMillis())+"\n");
			output.write("<tr><th>Username/<BR>Password</th><th>OrgKey</th><th>Register Result</th><th>All Available Subscriptions<BR>(to system consumers)</th></tr>\n");
			for (RegistrationData registeredConsumer : registrationDataList) {
				if (registeredConsumer.ownerKey==null) {
					output.write("<tr bgcolor=#F47777>");
				} else {output.write("<tr>");}
				if (registeredConsumer.username!=null) {
					output.write("<td valign=top>"+registeredConsumer.username+"/<BR>"+registeredConsumer.password+"</td>");
				} else {output.write("<td/>");};
				if (registeredConsumer.ownerKey!=null) {
					output.write("<td valign=top>"+registeredConsumer.ownerKey+"</td>");
				} else {output.write("<td/>");};
				if (registeredConsumer.registerResult!=null) {
					output.write("<td valign=top>"+registeredConsumer.registerResult.getStdout()+registeredConsumer.registerResult.getStderr()+"</td>");
				} else {output.write("<td/>");};
				if (registeredConsumer.allAvailableSubscriptionPools!=null) {
					output.write("<td valign=top><ul>");
					for (SubscriptionPool availableSubscriptionPool : registeredConsumer.allAvailableSubscriptionPools) {
						output.write("<li>"+availableSubscriptionPool+"</li>");
					}
					output.write("</ul></td>");
				} else {output.write("<td/>");};
				output.write("</tr>\n");
			}
			output.write("</table>\n");
			output.write("</html>\n");
		    output.close();
		    //log.info(file.getCanonicalPath()+" exists="+file.exists()+" writable="+file.canWrite());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36518", "RHEL7-51290"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: register to a Candlepin server using bogus credentials",
			groups={"Tier2Tests"},
			dataProvider="getAttemptRegistrationWithInvalidCredentials_Test")
//	@ImplementsNitrateTest(caseId={41691, 47918})
	@ImplementsNitrateTest(caseId=47918)
	public void testRegistrationWithInvalidCredentials(Object meta, String username, String password, String owner, ConsumerType type, String name, String consumerId, Boolean autosubscribe, Boolean force, String debug, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex) {
		log.info("Testing registration to a Candlepin using various options and data and asserting various expected results.");
		
		// ensure we are unregistered
		//DO NOT clienttasks.unregister();
		
		// attempt the registration
		SSHCommandResult sshCommandResult = clienttasks.register_(username, password, owner, null, type, name, consumerId, autosubscribe, null, null, (String)null, null, null, null, force, null, null, null, null, null);
		
		// assert the sshCommandResult here
		if (expectedExitCode!=null) Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode);
		if (expectedStdoutRegex!=null) Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), expectedStdoutRegex);
		if (expectedStderrRegex!=null) 
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-1")) expectedStderrRegex = "HTTP error code 401: "+expectedStderrRegex;	// post commit 630e1a2eb06e6bfacac669ce11f38e228c907ea9 1507030: RestlibExceptions should show they originate server-side	
			Assert.assertContainsMatch(sshCommandResult.getStderr().trim(), expectedStderrRegex);
	}
	@DataProvider(name="getAttemptRegistrationWithInvalidCredentials_Test")
	public Object[][] getAttemptRegistrationWithInvalidCredentials_TestDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getAttemptRegistrationWithInvalidCredentials_TestDataAsListOfLists());
	}
	protected List<List<Object>> getAttemptRegistrationWithInvalidCredentials_TestDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (servertasks==null) return ll;
		if (clienttasks==null) return ll;
		
		String uErrMsg = servertasks.invalidCredentialsRegexMsg();

		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			// Object bugzilla, String username, String password, String owner, String type, String consumerId, Boolean autosubscribe, Boolean force, String debug, Integer exitCode, String stdoutRegex, String stderrRegex
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1119688"}),								sm_clientUsername,					String.valueOf(getRandInt()),	null,				null,	null,	null,	null,		Boolean.TRUE,	null,	Integer.valueOf(70)/*EX_SOFTWARE*/,	null,	uErrMsg}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1119688"}),								sm_clientUsername+getRandInt(),		sm_clientPassword,				null,				null,	null,	null,	null,		Boolean.TRUE,	null,	Integer.valueOf(70)/*EX_SOFTWARE*/,	null,	uErrMsg}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1119688"}),								sm_clientUsername+getRandInt(),		String.valueOf(getRandInt()),	null,				null,	null,	null,	null,		Boolean.TRUE,	null,	Integer.valueOf(70)/*EX_SOFTWARE*/,	null,	uErrMsg}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1119688"}),								sm_clientUsername,					sm_clientPassword,				"foobar",			null,	null,	null,	null,		Boolean.TRUE,	null,	Integer.valueOf(70)/*EX_SOFTWARE*/,	null,	/*"Organization/Owner "+"foobar"+" does not exist."*/"Organization "+"foobar"+" does not exist."}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1119688","616065","734114"}),			sm_clientUsername,					sm_clientPassword,				"\"foo bar\"",		null,	null,	null,	null,		Boolean.TRUE,	null,	Integer.valueOf(70)/*EX_SOFTWARE*/,	null,	/*"Organization/Owner "+"foo bar"+" does not exist."*/"Organization "+"foo bar"+" does not exist."}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1119688","734114","906000","919584"}),	sm_clientUsername,					sm_clientPassword,				"\"富 酒吧\"",		null,	null,	null,	null,		Boolean.TRUE,	null,	Integer.valueOf(70)/*EX_SOFTWARE*/,	null,	/*"Organization/Owner "+"富 酒吧"+" does not exist."*/"Organization "+"富 酒吧"+" does not exist."}));
	
			// force a successful registration, and then...
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1119688","616065","669395"}),			sm_clientUsername,					sm_clientPassword,				sm_clientOrg,		null,	null,	null,	null,		Boolean.TRUE,	null,	Integer.valueOf(0),					"The system has been registered with ID: [a-f,0-9,\\-]{36}",	null}));
	
			// ... try to register again even though the system is already registered
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"1119688"}),								sm_clientUsername,					sm_clientPassword,				null,				null,	null,	null,	null,		Boolean.FALSE,	null,	Integer.valueOf(64)/*EX_USAGE*/,	null,	"This system is already registered. Use --force to override"}));
		} else {
			// Object bugzilla, String username, String password, String owner, String type, String consumerId, Boolean autosubscribe, Boolean force, String debug, Integer exitCode, String stdoutRegex, String stderrRegex
			ll.add(Arrays.asList(new Object[] {null,												sm_clientUsername,					String.valueOf(getRandInt()),	null,							null,	null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[] {null,												sm_clientUsername+getRandInt(),		sm_clientPassword,				null,							null,	null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[] {null,												sm_clientUsername+getRandInt(),		String.valueOf(getRandInt()),	null,							null,	null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,		uErrMsg}));
			// THIS TEST ROW IS DEPRECATED BY INTERACTIVE PROMPTING FOR REGISTRATION ORG IN RHEL7+ ll.add(Arrays.asList(new Object[] {null,							sm_clientUsername,					sm_clientPassword,				null,							null,	null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,		/*"You must specify an organization/owner for new consumers."*/"You must specify an organization for new consumers."}));
			ll.add(Arrays.asList(new Object[] {null,												sm_clientUsername,					sm_clientPassword,				"foobar",						null,	null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,		/*"Organization/Owner "+"foobar"+" does not exist."*/"Organization "+"foobar"+" does not exist."}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("734114"),						sm_clientUsername,					sm_clientPassword,				"\"foo bar\"",					null,	null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,		/*"Organization/Owner "+"foo bar"+" does not exist."*/"Organization "+"foo bar"+" does not exist."}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"734114","906000","919584"}),	sm_clientUsername,					sm_clientPassword,				"\"富 酒吧\"",					null,	null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(255),	null,		/*"Organization/Owner "+"富 酒吧"+" does not exist."*/"Organization "+"富 酒吧"+" does not exist."}));
	
			// force a successful registration, and then...
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"616065","669395"}),	sm_clientUsername,					sm_clientPassword,				sm_clientOrg,					null,	null,	null,		null,			Boolean.TRUE,	null,	Integer.valueOf(0),		"The system has been registered with ID: [a-f,0-9,\\-]{36}",					null}));
	
			// ... try to register again even though the system is already registered
			ll.add(Arrays.asList(new Object[] {null,												sm_clientUsername,					sm_clientPassword,				null,							null,	null,	null,		null,			Boolean.FALSE,	null,	Integer.valueOf(1),		"This system is already registered. Use --force to override",					null}));
		}
		
		return ll;
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19948", "RHEL7-55160"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager-cli: attempt to register a user who has not yet accepted the Red Hat Terms and Conditions",
			groups={"Tier1Tests","blockedByBug-1089034","blockedByBug-1068766","blockedByBug-1458423"},
			enabled=true)
	@ImplementsNitrateTest(caseId=48502)
	public void testRegistrationWithUnacceptedTermsAndConditions() {
		String username = sm_usernameWithUnacceptedTC;
		String password = sm_passwordWithUnacceptedTC;
		if (username.equals("")) throw new SkipException("Must specify a username who has not yet accepted the Red Hat Terms and Conditions before attempting this test.");

		// ensure we are unregistered
		clienttasks.unregister(null, null, null, null);
		
		// QE NOTE: The easiest way to create an account (without accepting terms and conditions) is to
		// use the http://account-manager-stage.app.eng.rdu2.redhat.com/#create to with the Accept Terms and Conditions checkbox unchecked
		// and BE SURE TO INCLUDE A SUBSCRIPTION SKU!  If there is no subscription in the created org, then there will be no requirement to
		// accept terms and conditions.
		
		log.info("Attempting to register to a candlepin server using an account that has not yet accepted the Red Hat Terms and Conditions");
		String stderr = "You must first accept Red Hat's Terms and conditions. Please visit https://www.redhat.com/wapps/ugc .";
		stderr += " You may have to log out of and back into the  Customer Portal in order to see the terms.";	// added by Bug 1068766 - (US48790, US51354, US55017) Subscription-manager register leads to unclear message
		// stderr after fix Bug 1458423 - registration to stage candlepin for an account that has not accepted terms and conditions should block registration
		stderr = "You must first accept Red Hat's Terms and conditions. Please visit https://www.redhat.com/wapps/tnc/termsack?event[]=signIn .";
		stderr += " You may have to log out of and back into the  Customer Portal in order to see the terms.";	// added by Bug 1068766 - (US48790, US51354, US55017) Subscription-manager register leads to unclear message
		String command = clienttasks.registerCommand(username, password, null, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null, null);
		SSHCommandResult result = client.runCommandAndWait(command);
		Integer expectedExitCode = new Integer(255);
		String expectedStdout = "";
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(70);	// EX_SOFTWARE	// post commit df95529a5edd0be456b3528b74344be283c4d258 Bug 1119688
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.15.9-2")) {
			expectedStdout = String.format("Registering to: %s:%s%s",clienttasks.getConfParameter("hostname"),clienttasks.getConfParameter("port"),clienttasks.getConfParameter("prefix"));	// subscription-manager commit d5014cda1c234d36943383b69898f2a651202b89 RHEL7.2 commit 968e6a407054c96291a4e64166c4840529772fff Bug 985157 - [RFE] Specify which username to enter when registering with subscription-manager

			// TEMPORARY WORKAROUND FOR BUG: 1251610 format error in message "Registering to: subscription.rhn.redhat.com/subscription:443"
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1251610"; 
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				expectedStdout = String.format("Registering to: %s%s:%s",clienttasks.getConfParameter("hostname"),clienttasks.getConfParameter("prefix"),clienttasks.getConfParameter("port"));	// subscription-manager commit d5014cda1c234d36943383b69898f2a651202b89 RHEL7.2 commit 968e6a407054c96291a4e64166c4840529772fff Bug 985157 - [RFE] Specify which username to enter when registering with subscription-manager
				log.warning("Altering the expected stdout while bug '"+bugId+"' is open.");
			}
			// END OF WORKAROUND
		}
		Assert.assertEquals(result.getExitCode(), expectedExitCode,"Exitcode from attempt to register a user who has not accepted Terms and Conditions.");
		Assert.assertEquals(result.getStdout().trim(),expectedStdout,"Stdout from attempt to register a user who has not accepted Terms and Conditions.");
		Assert.assertEquals(result.getStderr().trim(),stderr,"Stderr from attempt to register a user who has not accepted Terms and Conditions.");
		
		// assert that a consumer cert and key have NOT been installed
		Assert.assertTrue(!RemoteFileTasks.testExists(client,clienttasks.consumerKeyFile()), "Consumer key file '"+clienttasks.consumerKeyFile()+"' does NOT exist after an attempt to register with invalid credentials.");
		Assert.assertTrue(!RemoteFileTasks.testExists(client,clienttasks.consumerCertFile()), "Consumer cert file '"+clienttasks.consumerCertFile()+" does NOT exist after an attempt to register with invalid credentials.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19947", "RHEL7-55159"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager-cli: attempt to register a user who has been disabled",
			groups={"Tier1Tests"},
			enabled=true)
	@ImplementsNitrateTest(caseId=50210)
	public void testRegistrationWithDisabledUserCredentials() {
		String username = sm_disabledUsername;
		String password = sm_disabledPassword;
		if (username.equals("")) throw new SkipException("Must specify a username who has been disabled before attempting this test.");
		
		// ensure we are unregistered
		clienttasks.unregister(null, null, null, null);
		
		log.info("Attempting to register to a candlepin server using disabled credentials");
		String stderrRegex = "The user has been disabled, if this is a mistake, please contact customer service.";
		String command = String.format("%s register --username=%s --password=%s", clienttasks.command, username, password);
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(70);	// EX_SOFTWARE	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		RemoteFileTasks.runCommandAndAssert(client, command, expectedExitCode, null, stderrRegex);
		
		// assert that a consumer cert and key have NOT been installed
		Assert.assertTrue(!RemoteFileTasks.testExists(client,clienttasks.consumerKeyFile()), "Consumer key file '"+clienttasks.consumerKeyFile()+"' does NOT exist after an attempt to register with disabled credentials.");
		Assert.assertTrue(!RemoteFileTasks.testExists(client,clienttasks.consumerCertFile()), "Consumer cert file '"+clienttasks.consumerCertFile()+" does NOT exist after an attempt to register with disabled credentials.");
	}
	
	
	
	@Test(	description="subscription-manager-cli: register to a Candlepin server using autosubscribe functionality",
			groups={"Tier1Tests","RegisterWithAutosubscribe_Test","blockedByBug-602378", "blockedByBug-616137", "blockedByBug-678049", "blockedByBug-737762", "blockedByBug-743082"},
			enabled=false)	// the strategy for this test has been improved in the new implementation of RegisterWithAutosubscribe_Test() 
	@Deprecated
	public void testRegisterWithAutosubscribe_DEPRECATED() throws JSONException, Exception {

		log.info("RegisterWithAutosubscribe_Test Strategy:");
		log.info(" For DEV and QA testing purposes, we may not have valid products installed on the client, therefore we will fake an installed product by following this strategy:");
		log.info(" 1. Change the rhsm.conf configuration for productCertDir to point to a new temporary product cert directory.");
		log.info(" 2. Register with autosubscribe and assert that no product binding has occurred.");
		log.info(" 3. Subscribe to a randomly available pool");
		log.info(" 4. Copy the downloaded entitlement cert to the temporary product cert directory.");
		log.info("    (this will fake rhsm into believing that the same product is installed)");
		log.info(" 5. Reregister with autosubscribe and assert that a product has been bound.");

		// create a clean temporary productCertDir and change the rhsm.conf to point to it
		RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+tmpProductCertDir, Integer.valueOf(0)); // incase something was leftover from a prior run
		RemoteFileTasks.runCommandAndAssert(client, "mkdir "+tmpProductCertDir, Integer.valueOf(0));
		this.productCertDir = clienttasks.productCertDir;	// store the original productCertDir
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", tmpProductCertDir);

		// Register and assert that no products appear to be installed since we changed the productCertDir to a temporary directory
		clienttasks.unregister(null, null, null, null);
		SSHCommandResult sshCommandResult = clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, Boolean.TRUE, null, null, (String)null, null, null, null, null, false, null, null, null, null);

		//[root@jsefler-r63-server ~]# subscription-manager register --username testuser1 --password password --auto --org admin
		//The system has been registered with id: 243ea73d-01bb-458d-a7a5-2d61fde69494 
		//Installed Product Current Status:
		//ProductName:          	Awesome OS for S390 Bits 
		//Status:               	Not Subscribed           
		//
		//ProductName:          	Stackable with Awesome OS for x86_64 Bits
		//Status:               	Subscribed   
		
		// pre-fix for blockedByBug-678049 Assert.assertContainsNoMatch(sshCommandResult.getStdout().trim(), "^Subscribed to Products:", "register with autosubscribe should NOT appear to have subscribed to something when there are no installed products.");
		Assert.assertTrue(InstalledProduct.parse(sshCommandResult.getStdout()).isEmpty(),
				"The Installed Product Current Status should be empty when attempting to register with autosubscribe without any product certs installed.");
		Assert.assertEquals(clienttasks.list_(null, null, null, Boolean.TRUE, null, null, null, null, null, null, null, null, null, null).getStdout().trim(),"No installed products to list",
				"Since we changed the productCertDir configuration to an empty location, we should not appear to have any products installed.");
		//List <InstalledProduct> currentlyInstalledProducts = InstalledProduct.parse(clienttasks.list_(null, null, null, Boolean.TRUE, null, null, null).getStdout());
		//for (String status : new String[]{"Not Subscribed","Subscribed"}) {
		//	Assert.assertNull(InstalledProduct.findFirstInstanceWithMatchingFieldFromList("status", status, currentlyInstalledProducts),
		//			"When no product certs are installed, then we should not be able to find a installed product with status '"+status+"'.");
		//}

		// subscribe to a randomly available pool
		/* This is too random
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
		*/
		// subscribe to the first available pool that provides one product
		File entitlementCertFile = null;
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+pool.poolId));	
			JSONArray jsonProvidedProducts = jsonPool.getJSONArray("providedProducts");
			if (jsonProvidedProducts.length()==1) {
				entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool,/*sm_serverAdminUsername*/sm_clientUsername,/*sm_serverAdminPassword*/sm_clientPassword,sm_serverUrl);
				break;
			}
		}
		if (entitlementCertFile==null) throw new SkipException("Could not find an available pool that provides only one product with which to test register with --autosubscribe.");
		
		// copy the downloaded entitlement cert to the temporary product cert directory (this will fake rhsm into believing that the same product is installed)
		RemoteFileTasks.runCommandAndAssert(client, "cp "+entitlementCertFile.getPath()+" "+tmpProductCertDir, Integer.valueOf(0));
		File tmpProductCertFile = new File(tmpProductCertDir+File.separator+entitlementCertFile.getName());
		ProductCert fakeProductCert = clienttasks.getProductCertFromProductCertFile(tmpProductCertFile);
		
		// reregister with autosubscribe and assert that the product is bound
		clienttasks.unregister(null, null, null, null);
		sshCommandResult = clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, Boolean.TRUE, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		// assert that the sshCommandResult from register indicates the fakeProductCert was subscribed
		/* # subscription-manager register --username=testuser1 --password=password
		d67df9c8-f381-4449-9d17-56094ea58092 testuser1
		Subscribed to Products:
		     RHEL for Physical Servers SVC(37060)
		     Red Hat Enterprise Linux High Availability (for RHEL Entitlement)(4)
		*/
		
		/* # subscription-manager register --username=testuser1 --password=password
		cadf825a-6695-41e3-b9eb-13d7344159d3 jsefler-onprem03.usersys.redhat.com
		Installed Products:
		    Clustering Bits - Subscribed
		    Awesome OS Server Bits - Not Installed
		*/
		
		/* # subscription-manager register --username=testuser1 --password=password --org=admin --autosubscribe
		The system has been registered with id: f95fd9bb-4cc8-428e-b3fd-d656b14bfb89 
		Installed Product Current Status:

		ProductName:         	Awesome OS for S390X Bits
		Status:               	Subscribed  
		*/

		// assert that our fake product install appears to have been autosubscribed
		InstalledProduct autoSubscribedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("status", "Subscribed", InstalledProduct.parse(clienttasks.list_(null, null, null, Boolean.TRUE, null, null, null, null, null, null, null, null, null, null).getStdout()));
		Assert.assertNotNull(autoSubscribedProduct,	"We appear to have autosubscribed to our fake product install.");
		// pre-fix for blockedByBug-678049 Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "^Subscribed to Products:", "The stdout from register with autotosubscribe indicates that we have subscribed to something");
		// pre-fix for blockedByBug-678049 Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "^\\s+"+autoSubscribedProduct.productName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)"), "Expected ProductName '"+autoSubscribedProduct.productName+"' was reported as autosubscribed in the output from register with autotosubscribe.");
		//Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), ".* - Subscribed", "The stdout from register with autotosubscribe indicates that we have automatically subscribed at least one of this system's installed products to an available subscription pool.");
		List<InstalledProduct> autosubscribedProductStatusList = InstalledProduct.parse(sshCommandResult.getStdout());
		Assert.assertEquals(autosubscribedProductStatusList.size(), 1, "Only one product was autosubscribed."); 
		Assert.assertEquals(autosubscribedProductStatusList.get(0),new InstalledProduct(fakeProductCert.productName,null,null,null,"Subscribed",null,null, null),
				"As expected, ProductName '"+fakeProductCert.productName+"' was reported as subscribed in the output from register with autotosubscribe.");

		// WARNING The following two asserts lead to misleading failures when the entitlementCertFile that we using to fake as a tmpProductCertFile happens to have multiple bundled products inside.  This is why we search for an available pool that provides one product early in this test.
		//Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "^\\s+"+autoSubscribedProduct.productName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")+" - Subscribed", "Expected ProductName '"+autoSubscribedProduct.productName+"' was reported as autosubscribed in the output from register with autotosubscribe.");
		//Assert.assertNotNull(ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName", autoSubscribedProduct.productName, clienttasks.getCurrentlyConsumedProductSubscriptions()),"Expected ProductSubscription with ProductName '"+autoSubscribedProduct.productName+"' is consumed after registering with autosubscribe.");
	}
	@Test(	description="subscription-manager-cli: register to a Candlepin server using autosubscribe functionality",
			groups={"Tier1Tests","RegisterWithAutosubscribe_Test","blockedByBug-602378", "blockedByBug-616137", "blockedByBug-678049", "blockedByBug-737762", "blockedByBug-743082"},
			enabled=false)
	@Deprecated
	public void testRegisterWithAutosubscribe_DEPRECATED_2() throws JSONException, Exception {

		log.info("RegisterWithAutosubscribe_Test Strategy:");
		log.info(" 1. Change the rhsm.conf configuration for productCertDir to point to a new temporary product cert directory.");
		log.info(" 2. Register with autosubscribe and assert that no product binding has occurred.");
		log.info(" 3. Using the candlepin REST API, we will find an available pool that provides a product that we have installed.");
		log.info(" 4. Copy the installed product to a temporary product cert directory so that we can isolate the expected product that will be autosubscribed.");
		log.info(" 5. Reregister with autosubscribe and assert that the temporary product has been bound.");

		// get the product certs that are currently installed
		List<ProductCert> installedProductCerts = clienttasks.getCurrentProductCerts();
		
		// create a clean temporary productCertDir and change the rhsm.conf to point to it
		RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+tmpProductCertDir, Integer.valueOf(0)); // incase something was leftover from a prior run
		RemoteFileTasks.runCommandAndAssert(client, "mkdir "+tmpProductCertDir, Integer.valueOf(0));
		this.productCertDir = clienttasks.productCertDir;	// store the original productCertDir
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", tmpProductCertDir);

		// Register and assert that no products appear to be installed since we changed the productCertDir to a temporary directory
		SSHCommandResult sshCommandResult = clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, Boolean.TRUE, null, null, (String)null, null, null, null, true, false, null, null, null, null);

		//[root@jsefler-r63-server ~]# subscription-manager register --username testuser1 --password password --auto --org admin
		//The system has been registered with id: 243ea73d-01bb-458d-a7a5-2d61fde69494 
		//Installed Product Current Status:
		
		// pre-fix for blockedByBug-678049 Assert.assertContainsNoMatch(sshCommandResult.getStdout().trim(), "^Subscribed to Products:", "register with autosubscribe should NOT appear to have subscribed to something when there are no installed products.");
		Assert.assertTrue(InstalledProduct.parse(sshCommandResult.getStdout()).isEmpty(),
				"The Installed Product Current Status should be empty when attempting to register with autosubscribe without any product certs installed.");
		Assert.assertEquals(clienttasks.list_(null, null, null, Boolean.TRUE, null, null, null, null, null, null, null, null, null, null).getStdout().trim(),"No installed products to list",
				"Since we changed the productCertDir configuration to an empty location, we should not appear to have any products installed.");

		// subscribe to the first available pool that provides one product (whose product cert was also originally installed)
		File tmpProductCertFile = null;
		OUTERLOOP: for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+pool.poolId));	
			JSONArray jsonProvidedProducts = jsonPool.getJSONArray("providedProducts");
			if (jsonProvidedProducts.length()==1) {	// FIXME: I doubt this check is needed anymore
				JSONObject jsonProvidedProduct = jsonProvidedProducts.getJSONObject(0);
				String productId = jsonProvidedProduct.getString("productId");
				
				// now install the product that this pool will cover to our tmpProductCertDir
				/* NOT WORKING IN STAGE SINCE THE /products/{PRODUCT_ID}/certificate PATH APPEARS BLACK-LISTED
				JSONObject jsonProductCert = new JSONObject (CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, "/products/"+productId+"/certificate"));
				String cert = jsonProductCert.getString("cert");
				String key = jsonProductCert.getString("key");
				tmpProductCertFile = new File(tmpProductCertDir+File.separator+"AutosubscribeProduct_"+productId+".pem");
				client.runCommandAndWait("echo \""+cert+"\" > "+tmpProductCertFile);
				break;
				*/
				
				// now search for an existing installed product that matches and install it as our new tmpProductCert
				for (ProductCert productCert : installedProductCerts) {
					if (productCert.productId.equals(productId)) {
						tmpProductCertFile = new File(tmpProductCertDir+File.separator+"AutosubscribeProduct_"+productId+".pem");
						client.runCommandAndWait("cp "+productCert.file+" "+tmpProductCertFile);
						break OUTERLOOP;
					}
				}
			}
		}
		if (tmpProductCertFile==null) throw new SkipException("Could not find an available pool that provides only one product with which to test register with --autosubscribe.");
		ProductCert tmpProductCert = clienttasks.getProductCertFromProductCertFile(tmpProductCertFile);
		
		// reregister with autosubscribe and assert that the product is bound
		sshCommandResult = clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, Boolean.TRUE, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		// assert that the sshCommandResult from register indicates the tmpProductCert was subscribed
		
		/* # subscription-manager register --username=testuser1 --password=password --org=admin --autosubscribe
		The system has been registered with id: f95fd9bb-4cc8-428e-b3fd-d656b14bfb89 
		Installed Product Current Status:

		ProductName:         	Awesome OS for S390X Bits
		Status:               	Subscribed  
		*/

		// assert that our tmp product install appears to have been autosubscribed
		InstalledProduct autoSubscribedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("status", "Subscribed", InstalledProduct.parse(clienttasks.list_(null, null, null, Boolean.TRUE, null, null, null, null, null, null, null, null, null, null).getStdout()));
		Assert.assertNotNull(autoSubscribedProduct,	"We appear to have autosubscribed to our fake product install.");
		// pre-fix for blockedByBug-678049 Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "^Subscribed to Products:", "The stdout from register with autotosubscribe indicates that we have subscribed to something");
		// pre-fix for blockedByBug-678049 Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "^\\s+"+autoSubscribedProduct.productName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)"), "Expected ProductName '"+autoSubscribedProduct.productName+"' was reported as autosubscribed in the output from register with autotosubscribe.");
		//Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), ".* - Subscribed", "The stdout from register with autotosubscribe indicates that we have automatically subscribed at least one of this system's installed products to an available subscription pool.");
		List<InstalledProduct> autosubscribedProductStatusList = InstalledProduct.parse(sshCommandResult.getStdout());
		Assert.assertEquals(autosubscribedProductStatusList.size(), 1, "Only one product appears installed."); 
		Assert.assertEquals(autosubscribedProductStatusList.get(0),new InstalledProduct(tmpProductCert.productName,null,null,null,"Subscribed",null,null, null),
				"As expected, ProductName '"+tmpProductCert.productName+"' was reported as subscribed in the output from register with autotosubscribe.");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19949", "RHEL7-50999"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager-cli: register to a Candlepin server using autosubscribe functionality",
			groups={"Tier1Tests", "blockedByBug-602378", "blockedByBug-616137", "blockedByBug-678049", "blockedByBug-737762", "blockedByBug-743082", "blockedByBug-919700"},
			enabled=true)
	public void testRegisterWithAutosubscribe() throws JSONException, Exception {

		// determine all of the productIds that are provided by available subscriptions
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		Set<String> providedProductIdsFromAvailableUnmappedGuestOnlyPools = new HashSet<String>();
		Set<String> providedProductIdsFromAvailableVirtOnlyPools = new HashSet<String>();	// but not the unmapped_guest_only pools
		Set<String> providedProductIdsFromAvailablePools = new HashSet<String>();	// all the rest of the pools
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+pool.poolId));	
			JSONArray jsonProvidedProducts = jsonPool.getJSONArray("providedProducts");
			for (int k = 0; k < jsonProvidedProducts.length(); k++) {
				JSONObject jsonProvidedProduct = (JSONObject) jsonProvidedProducts.get(k);
				String providedProductName = jsonProvidedProduct.getString("productName");
				String providedProductId = jsonProvidedProduct.getString("productId");
				if (CandlepinTasks.isPoolRestrictedToUnmappedVirtualSystems(sm_clientUsername,sm_clientPassword,sm_serverUrl, pool.poolId)) {
					providedProductIdsFromAvailableUnmappedGuestOnlyPools.add(providedProductId);
				} if (CandlepinTasks.isPoolRestrictedToVirtualSystems(sm_clientUsername,sm_clientPassword,sm_serverUrl, pool.poolId)) {
					providedProductIdsFromAvailableVirtOnlyPools.add(providedProductId);
				} else {
					providedProductIdsFromAvailablePools.add(providedProductId);
				}
			}
		}
		
		// assert that all installed products are "Not Subscribed"
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		for (InstalledProduct installedProduct : installedProducts) {
			Assert.assertEquals(installedProduct.status,"Not Subscribed","Installed product status for productId '"+installedProduct.productId+"' prior to registration with autosubscribe.");
		}
		
		// now register with --autosubscribe
		SSHCommandResult registerResult = clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		//	201208091741:18.316 - FINE: ssh root@pogolinux-1.rhts.eng.rdu.redhat.com subscription-manager register --username=stage_test_12 --password=redhat --autosubscribe --force (com.redhat.qe.tools.SSHCommandRunner.run)
		//	201208091741:50.504 - FINE: Stdout: 
		//	The system with UUID 5e88b5ef-e218-423c-8cf1-380d37476580 has been unregistered
		//	The system has been registered with id: edfabea2-0bd4-4584-bb3e-f3bca9a3a442 
		//	Installed Product Current Status:
		//	Product Name:         	Red Hat Enterprise Linux Server
		//	Status:               	Subscribed
		//
		//	201208091741:50.505 - FINE: Stderr:  
		//	201208091741:50.505 - FINE: ExitCode: 0 
		
		// IF THE INSTALLED PRODUCT ID IS PROVIDED BY AN AVAILABLE SUBSCRIPTION, THEN IT SHOULD GET AUTOSUBSCRIBED! (since no service preference level has been set)
		
		// assert the expected installed product status in the feedback from register with --autosubscribe
		List<InstalledProduct> autosubscribedProducts = InstalledProduct.parse(registerResult.getStdout());
		for (InstalledProduct autosubscribedProduct : autosubscribedProducts) {
			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", autosubscribedProduct.productName, installedProducts);
			if (providedProductIdsFromAvailableUnmappedGuestOnlyPools.contains(installedProduct.productId) && !providedProductIdsFromAvailableVirtOnlyPools.contains(installedProduct.productId)) {
				// must be "Subscribed"
				Assert.assertEquals(autosubscribedProduct.status,"Partially Subscribed","Status for productName '"+autosubscribedProduct.productName+"' in feedback from registration with autosubscribe. (Partial/yellow because this productId was only provided by an available unmapped_guests_only pool.)");
			} else if (providedProductIdsFromAvailableUnmappedGuestOnlyPools.contains(installedProduct.productId) && providedProductIdsFromAvailableVirtOnlyPools.contains(installedProduct.productId)) {
				// could be either "Partially Subscribed" or "Subscribed"
				List <String> eitherPartiallySubscribedOrSubscribed = Arrays.asList("Partially Subscribed","Subscribed");
				Assert.assertTrue(eitherPartiallySubscribedOrSubscribed.contains(autosubscribedProduct.status),"Status for productName '"+autosubscribedProduct.productName+"' in feedback from registration (actual='"+autosubscribedProduct.status+"') with autosubscribe can be either "+eitherPartiallySubscribedOrSubscribed+". (Either because this product id was provided by both an available unmapped_guests_only pool as well as other virt_only pools.  Autosubscribe could randomly choose either.)");		
			} else if (providedProductIdsFromAvailablePools.contains(installedProduct.productId)) {
				
				// TEMPORARY WORKAROUND FOR BUG
				if (installedProduct.arch.contains(",")) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="951633"; // Bug 951633 - installed product with comma separated arch attribute fails to go green
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
					if (invokeWorkaroundWhileBugIsOpen) {
						log.warning("Skipping assertion for autosubscribed status of Installed Product name='"+installedProduct.productName+"' while Bugzilla '"+bugId+"' is open.");
						continue;
					}
				}
				// END OF WORKAROUND
				
				// must be "Subscribed"
				Assert.assertEquals(autosubscribedProduct.status,"Subscribed","Status for productName '"+autosubscribedProduct.productName+"' in feedback from registration with autosubscribe.");
			} else {
				// must be "Not Subscribed"
				Assert.assertEquals(autosubscribedProduct.status,"Not Subscribed","Status for productName '"+autosubscribedProduct.productName+"' in feedback from registration with autosubscribe.");
			}
		}
		
		// assert the expected installed product status in the list --installed after the register with --autosubscribe
		installedProducts = clienttasks.getCurrentlyInstalledProducts();
		for (InstalledProduct installedProduct : installedProducts) {
			if (providedProductIdsFromAvailableUnmappedGuestOnlyPools.contains(installedProduct.productId) && !providedProductIdsFromAvailableVirtOnlyPools.contains(installedProduct.productId)) {
				// must be "Subscribed"
				Assert.assertEquals(installedProduct.status,"Partially Subscribed","Status for Installed Product name='"+installedProduct.productName+"' id='"+installedProduct.productId+"' in list of installed products after registration with autosubscribe (Partial/yellow because this productId '"+installedProduct.productId+"' was only provided by an available unmapped_guests_only pool.)");
			} else if (providedProductIdsFromAvailableUnmappedGuestOnlyPools.contains(installedProduct.productId) && providedProductIdsFromAvailableVirtOnlyPools.contains(installedProduct.productId)) {
				// could be either "Partially Subscribed" or "Subscribed"
				List <String> eitherPartiallySubscribedOrSubscribed = Arrays.asList("Partially Subscribed","Subscribed");
				Assert.assertTrue(eitherPartiallySubscribedOrSubscribed.contains(installedProduct.status),"Status for productName '"+installedProduct.productName+"' in feedback from registration (actual='"+installedProduct.status+"') with autosubscribe can be either "+eitherPartiallySubscribedOrSubscribed+". (Either because this productId '"+installedProduct.productId+"' was provided by both an available unmapped_guests_only pool as well as other virt_only pools.  Autosubscribe could randomly choose either.)");		
			} else if (providedProductIdsFromAvailablePools.contains(installedProduct.productId)) {
				
				// TEMPORARY WORKAROUND FOR BUG
				if (installedProduct.arch.contains(",")) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="951633"; // Bug 951633 - installed product with comma separated arch attribute fails to go green
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
					if (invokeWorkaroundWhileBugIsOpen) {			
						log.warning("Skipping assertion for autosubscribed status of Installed Product name='"+installedProduct.productName+"' while Bugzilla '"+bugId+"' is open.");
						continue;
					}
				}
				// END OF WORKAROUND
				
				// must be "Subscribed"
				Assert.assertEquals(installedProduct.status,"Subscribed","Status for Installed Product name='"+installedProduct.productName+"' id='"+installedProduct.productId+"' in list of installed products after registration with autosubscribe.");
			} else {
				// must be "Not Subscribed"
				Assert.assertEquals(installedProduct.status,"Not Subscribed","Status for Installed Product name='"+installedProduct.productName+"' id='"+installedProduct.productId+"' in list of installed products after registration with autosubscribe.");
			}
		}
		Assert.assertEquals(autosubscribedProducts.size(), installedProducts.size(), "The 'Installed Product Current Status' reported during register --autosubscribe should contain the same number of products as reported by list --installed.");
		
		// assert the feedback from RFE Bug 864195 - New String: Add string to output of 'subscription-manager subscribe --auto' if can't cover all products
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.11.1-1")) {
			String autoAttachFeedbackResultMessage = "Unable to find available subscriptions for all your installed products."; 
			if (InstalledProduct.findAllInstancesWithMatchingFieldFromList("status", "Subscribed", installedProducts).size() == installedProducts.size()) {
				Assert.assertTrue(!registerResult.getStdout().contains(autoAttachFeedbackResultMessage), "When the registration with autosubscribe succeeds in making all the installed products compliant, then feedback '"+autoAttachFeedbackResultMessage+"' is NOT reported.");
			} else {
				Assert.assertTrue(registerResult.getStdout().trim().endsWith(autoAttachFeedbackResultMessage), "When the registration with autosubscribe fails to make all the installed products compliant, then feedback '"+autoAttachFeedbackResultMessage+"' is the final report.");
			}
			
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36522", "RHEL7-51294"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: register with --force",
			groups={"Tier2Tests","blockedByBug-623264"},
			enabled=true)
	public void testRegisterWithForce() {
		
		// start fresh by unregistering
		clienttasks.unregister(null, null, null, null);
		
		// make sure you are first registered
		SSHCommandResult sshCommandResult = clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		String firstConsumerId = clienttasks.getCurrentConsumerId();
		
		// subscribe to a random pool (so as to consume an entitlement)
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("Cannot randomly pick a pool for subscribing when there are no available pools for testing."); 
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribeToSubscriptionPool(pool);
		
		// attempt to register again and assert that you are warned that the system is already registered
		sshCommandResult = clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertTrue(sshCommandResult.getStderr().startsWith("This system is already registered."),"Expecting stderr indication: This system is already registered.");
		} else {
			Assert.assertTrue(sshCommandResult.getStdout().startsWith("This system is already registered."),"Expecting stdout indication: This system is already registered.");
		}
		
		// register with force
		sshCommandResult = clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null);
		String secondConsumerId = clienttasks.getCurrentConsumerId();
		
		// assert the stdout reflects a new consumer
		String msg= "The system with UUID "+firstConsumerId+" has been unregistered";
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.19.11-1")) {	// commit 217c3863448478d06c5008694e327e048cc54f54 Bug 1443101: Provide unregistering feedback when force registering
			Assert.assertTrue(sshCommandResult.getStdout().contains(msg),"Stdout contains '"+msg+"'");
		} else {
			Assert.assertTrue(sshCommandResult.getStdout().startsWith(msg),"Stdout starts with '"+msg+"'");
		}
		Assert.assertTrue(!secondConsumerId.equals(firstConsumerId),"After registering with force, a newly registered consumerid was returned.");

		// assert that the new consumer is not consuming any entitlements
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptions.size(),0,"After registering with force, no product subscriptions should be consumed.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36525", "RHEL7-51298"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: register with --name",
			dataProvider="getRegisterWithName_TestData",
			groups={"Tier2Tests"},
			enabled=true)
	@ImplementsNitrateTest(caseId=62352) // caseIds=81089 81090 81091
	public void testRegisterWithName(Object bugzilla, String name, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex) {
		
		// start fresh by unregistering
		clienttasks.unregister(null, null, null, null);
		
		// register with a name
		SSHCommandResult sshCommandResult = clienttasks.register_(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,name,null, null, null, null, (String)null, null, null, null, null, null, null, null, null, null);
		
		// assert the sshCommandResult here
		if (expectedExitCode!=null) Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode,"ExitCode after register with --name=\""+name+"\" option:");
		if (expectedStdoutRegex!=null) Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), expectedStdoutRegex,"Stdout after register with --name=\""+name+"\" option:");
		if (expectedStderrRegex!=null) Assert.assertContainsMatch(sshCommandResult.getStderr().trim(), expectedStderrRegex,"Stderr after register with --name=\""+name+"\" option:");
		
		// assert that the name is happily placed in the consumer cert
		if (expectedExitCode!=null && expectedExitCode==0) {
			ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
			Assert.assertEquals(consumerCert.name, name, "");
		}
	}
	@DataProvider(name="getRegisterWithName_TestData")
	public Object[][] getRegisterWithName_TestDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRegisterWithName_TestDataAsListOfLists());
	}
	protected List<List<Object>> getRegisterWithName_TestDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
		String invalidNameStderr = "System name must consist of only alphanumeric characters, periods, dashes and underscores.";	// bugzilla 672233
		       invalidNameStderr = "System name cannot contain most special characters.";	// bugzilla 677405
		String maxCharsStdout = null;
		String maxCharsStderr = "Name of the consumer should be shorter than 250 characters\\.";
		if (!clienttasks.workaroundForBug876764(sm_serverType)) maxCharsStderr = "Name of the unit must be shorter than 250 characters\\.";
		       maxCharsStderr = "Problem creating unit Consumer";	// Problem creating unit Consumer [id = 8a9087e3462af2aa01466361ec71037f, type = ConsumerType [id=1000, label=system], getName() = 256_characters_6789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456]	// valid after bug https://bugzilla.redhat.com/show_bug.cgi?id=1094492#c1
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion,">=","2.0.21-1"/*TODO change to "2.0.22-1" once it is tagged*/)) {	// Bug 1371009 - Need clearer error message when register with system name exceeding max characters.
			maxCharsStderr = "Name of the consumer should be shorter than 255 characters\\.";	// candlepin commit 606b9d9d14d1547d9704e6151f873573f68c52b8 1371009:  Need clearer error message when register with system name exceeding max characters.
			maxCharsStderr = "Name of the consumer should be shorter than 256 characters\\.";	// candlepin commit 4e365796eeeea75ad1fc2d35ab4222e0604f1eca 1371009: clearer error message (fixed typo)
		}
		String name;
		String successfulStdout = "The system has been registered with id: [a-f,0-9,\\-]{36}";	// msg changed by bug 878634
		       successfulStdout = "The system has been registered with ID: [a-f,0-9,\\-]{36}";

		// valid names according to bugzilla 672233
		name = "periods...dashes---underscores___alphanumerics123";
										ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} "+name*/,	null}));
		name = "249_characters_678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
										ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} "+name*/,	null}));
		// changed from 249 to 255 chars by candlepin commit a0db7c35f8d7ee71daeabaf39788b3f47206e0e0; 1065369: Use Hibernate Validation to supersede database error reporting.
		name = "255_characters_678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345";
										ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"672233","1065369","1094492","1451107"}),	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} "+name*/,	null}));

		// the tolerable characters has increased due to bugzilla 677405 and agilo task http://gibson.usersys.redhat.com/agilo/ticket/5235 (6.1) As an IT Person, I would like to ensure that user service and candlepin enforce the same valid character rules (QE); Developer beav "Christopher Duryee" <cduryee@redhat.com>
		// https://bugzilla.redhat.com/show_bug.cgi?id=677405#c1
		name = "[openingBracket[";		ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} \\[openingBracket\\["*/,	null}));
		name = "]closingBracket]";		ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} \\]closingBracket\\]"*/,	null}));
		name = "{openingBrace{";		ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} \\{openingBrace\\{"*/,	null}));
		name = "}closingBrace}";		ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} \\}closingBrace\\}"*/,	null}));
		name = "(openingParenthesis(";	ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} \\(openingParenthesis\\("*/,	null}));
		name = ")closingParenthesis)";	ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} \\)closingParenthesis\\)"*/,	null}));
		name = "?questionMark?";		ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} \\?questionMark\\?"*/,	null}));
		name = "@at@";					ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} "+name*/,	null}));
		name = "!exclamationPoint!";	ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} "+name*/,	null}));
		name = "`backTick`";			ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} "+name*/,	null}));
		name = "'singleQuote'";			ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} "+name*/,	null}));
		name = "pound#sign";			ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(0),	successfulStdout/*"[a-f,0-9,\\-]{36} "+name*/,	null}));	// Note: pound signs within the name are acceptable, but not at the beginning

		// invalid names
		// Note: IT Services invalid characters can be tested by trying to Sign Up a new login here: https://www.webqa.redhat.com/wapps/sso/login.html
		// Invalid Chars: (") ($) (^) (<) (>) (|) (+) (%) (/) (;) (:) (,) (\) (*) (=) (~)  // from https://bugzilla.redhat.com/show_bug.cgi?id=677405#c1
		name = "\"doubleQuotes\"";		ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = "$dollarSign$";			ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = "^caret^";				ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = "<lessThan<";			ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = ">greaterThan>";			ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = "|verticalBar|";			ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = "+plus+";				ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = "%percent%";				ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = "/slash/";				ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = ";semicolon;";			ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = ":colon:";				ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = ",comma,";				ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = "\\backslash\\";			ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = "*asterisk*";			ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = "=equal=";				ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		name = "~tilde~";				ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));

		// spaces are also rejected characters from IT Services
		name = "s p a c e s";			ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	invalidNameStderr}));

		// special case (pound sign at the beginning is a limitation in the x509 certificates)
		name = "#poundSign";			ll.add(Arrays.asList(new Object[] {null,	name,	Integer.valueOf(255),	null,	"System name cannot begin with # character"}));

		//	
		// http://www.ascii.cl/htmlcodes.htm
		// TODO
		//name = "é";						ll.add(Arrays.asList(new Object[]{	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		//name = "ë";						ll.add(Arrays.asList(new Object[]{	name,	Integer.valueOf(255),	null,	invalidNameStderr}));
		//name = "â";						ll.add(Arrays.asList(new Object[]{	name,	Integer.valueOf(255),	null,	invalidNameStderr}));



		// names that are too long
		name = "250_characters_6789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
		// changed from 250 to 256 chars by candlepin commit a0db7c35f8d7ee71daeabaf39788b3f47206e0e0; 1065369: Use Hibernate Validation to supersede database error reporting.
		//								ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("672233"),	name,	Integer.valueOf(255),	null,				maxCharsStderr}));
										ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("672233"),	name,	Integer.valueOf(0),		successfulStdout,	null}));
		name = "256_characters_6789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456";
										ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"672233","1065369","1094492","1101552"}),	name,	Integer.valueOf(255), maxCharsStdout, maxCharsStderr}));
		
		// for all rows, change the expected exitCode when testing post subscription-manager-1.13.8-1
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			for (List<Object> l : ll) {
				BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);	// get the existing BlockedByBzBug
				Integer expectedExitCode = (Integer) l.get(2);
				if (expectedExitCode.equals(255)) {
					List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
					bugIds.add("1119688");	// Bug 1119688 - [RFE] subscription-manager better usability for scripts
					blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
					l.set(0, blockedByBzBug);
					l.set(2, new Integer(70));	// EX_SOFTWARE
				}
			}
		}
		
		return ll;
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27114", "RHEL7-51297"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: register with --name and --type",
			dataProvider="getRegisterWithNameAndTypeData",
			groups={"Tier2Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithNameAndType(Object bugzilla, String username, String password, String owner, String name, ConsumerType type, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex) throws Exception {
		
		// start fresh by unregistering
		clienttasks.unregister(null, null, null, null);
		// register with a name
		
		SSHCommandResult sshCommandResult = clienttasks.register_(username,password,owner,null,type,name,null, null, null, null, (String)null, null, null, null, null, null, null, null, null, null);
		
		// assert the sshCommandResult here
		if (expectedExitCode!=null)Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode,"ExitCode after register with --name="+name+" --type="+type+" options:");
		if (expectedStdoutRegex!=null) Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), expectedStdoutRegex,"Stdout after register with --name="+name+" --type="+type+" options:");
		if (expectedStderrRegex!=null) Assert.assertContainsMatch(sshCommandResult.getStderr().trim(), expectedStderrRegex,"Stderr after register with --name="+name+" --type="+type+" options:");
		
		// assert the type
		if (expectedExitCode!=null && expectedExitCode==0) {
			String consumerId = clienttasks.getCurrentConsumerId(sshCommandResult);
			String path = "/consumers/"+consumerId+"?include=type";
			JSONObject jsonConsumerType= new JSONObject(servertasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, path));
			//	{
			//	    "type": {
			//	        "id": "0",
			//	        "label": "system",
			//	        "manifest": false
			//	    }
			//	}
			String actualType = jsonConsumerType.getJSONObject("type").getString("label");
			Assert.assertEquals(actualType, type.toString(), "Consumer type.label returned from Candlepin API for GET on '"+path+"' after registering with --type='"+type+"'");
		}
	}
	

	
	@DataProvider(name="getRegisterWithNameAndTypeData")
	public Object[][] getRegisterWithNameAndTypeDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getRegisterWithNameAndTypeDataAsListOfLists());
	}
	protected List<List<Object>> getRegisterWithNameAndTypeDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		String username=sm_clientUsername;
		String password=sm_clientPassword;
		String owner=sm_clientOrg;

		List <String> registerableConsumerTypes = new ArrayList<String> ();
		JSONArray jsonConsumerTypes = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,"/consumertypes"));	
		for (int i = 0; i < jsonConsumerTypes.length(); i++) {
			JSONObject jsonConsumerType = (JSONObject) jsonConsumerTypes.get(i);
			String consumerType = jsonConsumerType.getString("label");
			registerableConsumerTypes.add(consumerType);
		}
		
		// iterate across all ConsumerType values and append rows to the dataProvider
		for (ConsumerType type : ConsumerType.values()) {
			String name = type.toString()+"_NAME";
			
			// decide what username and password to test with
			if (type.equals(ConsumerType.person) && !getProperty("sm.rhpersonal.username", "").equals("")) {
				username = sm_rhpersonalUsername;
				password = sm_rhpersonalPassword;
				owner = sm_rhpersonalOrg;
			} else {
				username = sm_clientUsername;
				password = sm_clientPassword;
				owner = sm_clientOrg;
			}
			
			// String username, String password, String owner, String name, ConsumerType type, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex
			if (registerableConsumerTypes.contains(type.toString())) {
				/* applicable to RHEL61 and RHEL57
				if (type.equals(ConsumerType.person)) {
					ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("661130"),	username,	password,	name,	type,	Integer.valueOf(0),	"[a-f,0-9,\\-]{36} "+username,	null}));
				} else {
					ll.add(Arrays.asList(new Object[]{null,  							username,	password,	name,	type,	Integer.valueOf(0),	"[a-f,0-9,\\-]{36} "+name,	null}));			
				}
				*/
				Integer expectedExitCode = new Integer(0);
				String expectedStdoutRegex = "The system has been registered with ID: [a-f,0-9,\\-]{36}";
				String expectedStderrRegex = null;
				if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">="/*TODO CHANGE TO ">" after candlepin 2.1.2-1 is tagged*/, "2.1.1-1")) {	// candlepin commit 739b51a0d196d9d3153320961af693a24c0b826f Bug 1455361: Disallow candlepin consumers to be registered via Subscription Manager
					if (ConsumerType.candlepin.equals(type) |
						ConsumerType.headpin.equals(type) |
						ConsumerType.katello.equals(type)) {
						//	FINE: ssh root@jsefler-rhel7.usersys.redhat.com subscription-manager register --username=testuser1 --password=password --org=admin --type=candlepin --name="candlepin_NAME"
						//	FINE: Stdout: Registering to: jsefler-candlepin.usersys.redhat.com:8443/candlepin
						//	FINE: Stderr: You may not create a manifest consumer via Subscription Manager.
						//	FINE: ExitCode: 70
						expectedStdoutRegex = null;
						expectedStderrRegex = "You may not create a manifest consumer via Subscription Manager.";
						expectedExitCode = Integer.valueOf(70);
					}
				}
				if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.1.1-1")) {	// candlepin commit 6976b7c45d48945d6d0bf1118bd1d8edebceb1f0
					if (ConsumerType.share.equals(type)) {
						//	FINE: ssh root@jsefler-rhel7.usersys.redhat.com subscription-manager register --username=testuser1 --password=password --org=admin --type=share --name="share_NAME"
						//	FINE: Stdout: Registering to: jsefler-candlepin.usersys.redhat.com:8443/candlepin
						//	FINE: Stderr: A unit type of "share" cannot have installed products
						//	FINE: ExitCode: 70
						expectedStdoutRegex = null;
						//	Note: assuming there is at least one installed product since our client is a RHEL product afterall
						expectedStderrRegex = String.format("A unit type of \"%s\" cannot have installed products","share");
						expectedExitCode = Integer.valueOf(70);
					}
				}	
				ll.add(Arrays.asList(new Object[]{null,  username,	password,	owner,	name,	type,	expectedExitCode,	expectedStdoutRegex,	expectedStderrRegex}));
			} else {
				String expectedStderrRegex = "No such consumer type: "+type;
				if (!clienttasks.workaroundForBug876764(sm_serverType)) expectedStderrRegex = "No such unit type: "+type;
				expectedStderrRegex = String.format("Unit type '%s' could not be found.",type);	// changed to this by bug 876758 comment 5; https://bugzilla.redhat.com/show_bug.cgi?id=876758#c5
				if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
					expectedStderrRegex = String.format("Unit type \"%s\" could not be found.",type);
				}
				if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.5-1")) {	// commit f87515e457c8b74cfaeaf9c0e47f019c241e8355 Changed Consumer.type to Consumer.typeId
					expectedStderrRegex = String.format("Invalid unit type: %s",type);
				}
				Integer expectedExitCode = new Integer(255);
				if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(70);	// EX_SOFTWARE	// post commit df95529a5edd0be456b3528b74344be283c4d258
				ll.add(Arrays.asList(new Object[]{null,	username,	password,	owner,	name,	type,	expectedExitCode,	null,	expectedStderrRegex}));
			}
		}
		
		// process all of the rows and change the expected results due to 1461003: Deprecate --type option on register command
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.20.2-1")) {	// post commit e0c34a729e9e347ab1e0f4f5fa656c8b20205fdf RFE Bug 1461003: Deprecate --type option on register command
			for (List<Object> l : ll) {
				BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);	// get the existing BlockedByBzBug
				ConsumerType type = (ConsumerType) l.get(5);
				if (!type.equals(ConsumerType.system) && !type.equals(ConsumerType.RHUI)) {
					List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
					bugIds.add("1461003");	// Bug 1461003 - [RFE] Remove --type option from subscription-manager register
					blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
					l.set(0, blockedByBzBug);
					l.set(6, new Integer(64));	// EX_USAGE
					l.set(7, "");	// stdout
					l.set(8, "Error: The --type option has been deprecated and may not be used.");	// stderr
				}
				if (type.equals(ConsumerType.RHUI)) {
					List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
					bugIds.add("1485008");	// Bug 1485008 - subscription-manager register --type="RHUI" or --type="rhui" should both work as documented in various KBase articles
					blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
					l.set(0, blockedByBzBug);
				}
			}
		}
		
		return ll;
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36526", "RHEL7-51299"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="assert that a consumer can register with a release value and that subscription-manager release will return the set value",
			groups={"Tier2Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithRelease() {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,"Foo",(List<String>)null,null,null,null,true, null, null, null, null, null);		
		Assert.assertEquals(clienttasks.getCurrentRelease(), "Foo", "The release value retrieved after registering with the release.");
	}
	
	
	
	/**
	 * https://tcms.engineering.redhat.com/case/56327/?from_plan=2476
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19953", "RHEL7-51002"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager-cli: reregister basic registration\n" +
			"\t\tActions:\n" +
			"\n" +
			"\t \t\t* register a client to candlepin (take note of the uuid returned)\n" +
			"\t \t\t* take note of your identity cert info using openssl x509\n" +
			"\t \t\t* subscribe to a pool\n" +
			"\t \t\t* list consumed\n" +
			"\t \t\t* ls /etc/pki/entitlement/products\n" +
			"\t \t\t* Now.. mess up your identity..  mv /etc/pki/consumer/cert.pem /bak\n" +
			"\t \t\t* run the \"reregister\" command w/ username and passwd AND w/consumerid=<uuid>\n" +
			"\n" +
			"\t\tExpected Results:\n" +
			"\n" +
			"\t \t\t* after running reregister you should have a new identity cert\n" +
			"\t \t\t* after registering you should still the same products consumed (list consumed)\n" +
			"\t \t\t* the entitlement serials should be the same as before the registration",
			groups={"Tier1Tests","blockedByBug-636843","blockedByBug-962520"},
			enabled=true)
	@ImplementsNitrateTest(caseId=56327)
	public void testReregisterBasicRegistration() {
		
		// start fresh by unregistering and registering
		clienttasks.unregister(null, null, null, null);
		String consumerIdBefore = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null, null, null, false, null, null, null, null));
		
		// take note of your identity cert before reregister
		ConsumerCert consumerCertBefore = clienttasks.getCurrentConsumerCert();
		
		// subscribe to a random pool
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("Cannot randomly pick a pool for subscribing when there are no available pools for testing."); 
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribeToSubscriptionPool(pool);
		
		// get a list of the consumed products
		List<ProductSubscription> consumedProductSubscriptionsBefore = clienttasks.getCurrentlyConsumedProductSubscriptions();
		
		// reregister
		//clienttasks.reregister(null,null,null);
		clienttasks.reregisterToExistingConsumer(sm_clientUsername,sm_clientPassword,consumerIdBefore);
		
		// assert that the identity cert has not changed
		ConsumerCert consumerCertAfter = clienttasks.getCurrentConsumerCert();
		Assert.assertEquals(consumerCertBefore, consumerCertAfter, "The consumer identity cert has not changed after reregistering with consumerid.");
		
		// assert that the user is still consuming the same products
		List<ProductSubscription> consumedProductSubscriptionsAfter = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertTrue(
				consumedProductSubscriptionsAfter.containsAll(consumedProductSubscriptionsBefore) &&
				consumedProductSubscriptionsBefore.size()==consumedProductSubscriptionsAfter.size(),
				"The list of consumed products after reregistering is identical.");
	}
	
	
	
	/**
	 * https://tcms.engineering.redhat.com/case/56328/?from_plan=2476
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36529", "RHEL7-51302"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: bad identity cert\n" +
			"\t\tActions:\n" +
			"\n" +
			"\t \t\t* register a client to candlepin (take note of the uuid returned)\n" +
			"\t \t\t* take note of your identity cert info using openssl x509\n" +
			"\t \t\t* subscribe to a pool\n" +
			"\t \t\t* list consumed\n" +
			"\t \t\t* ls /etc/pki/entitlement/products\n" +
			"\t \t\t* Now.. mess up your identity..  mv /etc/pki/consumer/cert.pem /bak\n" +
			"\t \t\t* run the \"reregister\" command w/ username and passwd AND w/consumerid=<uuid>\n" +
			"\n" +
			"\t\tExpected Results:\n" +
			"\n" +
			"\t \t\t* after running reregister you should have a new identity cert\n" +
			"\t \t\t* after registering you should still the same products consumed (list consumed)\n" +
			"\t \t\t* the entitlement serials should be the same as before the registration",
			groups={"Tier2Tests","blockedByBug-624106","blockedByBug-844069","ReregisterWithBadIdentityCert_Test"},
			enabled=true)
	@ImplementsNitrateTest(caseId=56328)
	public void testReregisterWithBadIdentityCert() {
		
		// start fresh by unregistering and registering
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null, null, null, false, null, null, null, null);
		
		// take note of your identity cert
		ConsumerCert consumerCertBefore = clienttasks.getCurrentConsumerCert();
		
		// subscribe to a random pool
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("Cannot randomly pick a pool for subscribing when there are no available pools for testing."); 
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribeToSubscriptionPool(pool);
		
		// get a list of the consumed products
		List<ProductSubscription> consumedProductSubscriptionsBefore = clienttasks.getCurrentlyConsumedProductSubscriptions();
		
		// Now.. mess up your identity..  by borking its content
		log.info("Messing up the identity cert by borking its content...");
		RemoteFileTasks.runCommandAndAssert(client, "openssl x509 -noout -text -in "+clienttasks.consumerCertFile()+" > /tmp/stdout; mv /tmp/stdout -f "+clienttasks.consumerCertFile(), 0);
		
		// reregister w/ username, password, and consumerid
		//clienttasks.reregister(client1username,client1password,consumerCertBefore.consumerid);
		log.warning("The subscription-manager-cli reregister module has been eliminated and replaced by register --consumerid b3c728183c7259841100eeacb7754c727dc523cd...");
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.16.2-1")) {	// commit f14d2618ea94c18a0295ae3a5526a2ff252a3f99 Doesnt allow using --force with --consumerid
			//	[root@jsefler-6 ~]# subscription-manager register --username=testuser1 --password=password --consumerid=fc1b9613-2793-4017-8b9f-a8ab85c5ba96 --force
			//	Error: Can not force registration while attempting to recover registration with consumerid. Please use --force without --consumerid to re-register or use the clean command and try again without --force.
			log.warning("The original point of this test is not really applicable after 1.16.2-1 where registering with --consumerid and --force has been more explicitly divided into two steps... clean and register --consumerid.");
			clienttasks.clean();
			clienttasks.register(sm_clientUsername,sm_clientPassword,null,null,null,null,consumerCertBefore.consumerid, null, null, null, (String)null, null, null, null, Boolean.FALSE, false, null, null, null, null);
		} else {
			clienttasks.register(sm_clientUsername,sm_clientPassword,null,null,null,null,consumerCertBefore.consumerid, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null);
		}
		
		// assert that the identity cert has not changed
		ConsumerCert consumerCertAfter = clienttasks.getCurrentConsumerCert();
		Assert.assertEquals(consumerCertBefore, consumerCertAfter, "The consumer identity cert has not changed after reregistering with consumerid.");
	
		// assert that the user is still consuming the same products
		List<ProductSubscription> consumedProductSubscriptionsAfter = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertTrue(
				consumedProductSubscriptionsAfter.containsAll(consumedProductSubscriptionsBefore) &&
				consumedProductSubscriptionsBefore.size()==consumedProductSubscriptionsAfter.size(),
				"The list of consumed products after reregistering is identical.");
	}
	@AfterGroups(groups={"setup"},value={"ReregisterWithBadIdentityCert_Test"})
	public void afterReregisterWithBadIdentityCert_Test() {
		// needed in case ReregisterWithBadIdentityCert_Test fails to prevent succeeding tests from failing with an "Error loading certificate"
		clienttasks.unregister_(null, null, null, null);	// give system an opportunity to clean it's consumerid with the server
		clienttasks.clean();	// needed in case the system failed to unregister
	}
	
	
	/**
	 * https://tcms.engineering.redhat.com/case/72845/?from_plan=2476
	 *
	 * @throws Exception 
	 * @throws JSONException
	 */
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36530", "RHEL7-51303"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="register with existing consumerid should automatically refresh entitlements\n" +
			"Actions:\n" +
			"\n" +
			"    * register with username and password and remember the consumerid\n" +
			"    * subscribe to one or more subscriptions\n" +
			"    * list the consumed subscriptions and remember them\n" +
			"    * clean system\n" +
			"    * assert that there are no entitlements on the system\n" +
			"    * register with same username, password and existing consumerid\n" +
			"    * assert that originally consumed subscriptions are once again being consumed\n" +
			"\n" +
			"\t\n" +
			"Expected Results:\n" +
			"\n" +
			"    * when registering a new system to an already existing consumer, all of the existing consumers entitlement certs should be downloaded to the new system",
			groups={"Tier2Tests"},
			enabled=true)
	@ImplementsNitrateTest(caseId=72845)
	public void testReregisterWithConsumerIdShouldAutomaticallyRefreshEntitlements() throws JSONException, Exception {
		
		// register with username and password and remember the consumerid
		clienttasks.unregister(null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null));
		
		// subscribe to one or more subscriptions
		//// subscribe to a random pool
		//List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		//SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		//clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();

		// list the consumed subscriptions and remember them
		List <ProductSubscription> originalConsumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		// also remember the current entitlement certs
		List <EntitlementCert> originalEntitlementCerts= clienttasks.getCurrentEntitlementCerts();
		
		// clean system
		clienttasks.clean();
		
		// assert that there are no entitlements on the system
		//Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().isEmpty(),"There are NO consumed Product Subscriptions on this system after running clean");
		Assert.assertTrue(clienttasks.getCurrentEntitlementCerts().isEmpty(),"There are NO Entitlement Certs on this system after running clean");
		
		// register with same username, password and existing consumerid
		// Note: no need to register with force as running clean wipes system of all local registration data
		clienttasks.register(sm_clientUsername,sm_clientPassword,null,null,null,null,consumerId, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);

		// assert that originally consumed subscriptions are once again being consumed
		List <ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(consumedProductSubscriptions.size(),originalConsumedProductSubscriptions.size(), "The number of consumed Product Subscriptions after registering to an existing consumerid matches his original count.");
		for (ProductSubscription productSubscription : consumedProductSubscriptions) {
			Assert.assertContains(originalConsumedProductSubscriptions, productSubscription);
		}
		// assert that original entitlement certs are once on the system
		List <EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
		Assert.assertEquals(entitlementCerts.size(),originalEntitlementCerts.size(), "The number of Entitlement Certs on the system after registering to an existing consumerid matches his original count.");
		for (EntitlementCert entitlementCert : entitlementCerts) {
			Assert.assertContains(originalEntitlementCerts, entitlementCert);
		}
		
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36519", "RHEL7-51291"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="register with an empty /var/lib/rhsm/facts/facts.json file",
			groups={"Tier2Tests","blockedByBug-667953","blockedByBug-669208"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRegisterWithAnEmptyRhsmFactsJsonFile() {
		
		Assert.assertTrue(RemoteFileTasks.testFileExists(client, clienttasks.rhsmFactsJsonFile)==1, "rhsm facts json file '"+clienttasks.rhsmFactsJsonFile+"' exists");
		log.info("Emptying rhsm facts json file '"+clienttasks.rhsmFactsJsonFile+"'...");
		client.runCommandAndWait("echo \"\" > "+clienttasks.rhsmFactsJsonFile, TestRecords.action());
		SSHCommandResult result = client.runCommandAndWait("cat "+clienttasks.rhsmFactsJsonFile, TestRecords.action());
		Assert.assertTrue(result.getStdout().trim().equals(""), "rhsm facts json file '"+clienttasks.rhsmFactsJsonFile+"' is empty.");
		
		log.info("Attempt to register with an empty rhsm facts file (expecting success)...");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36520", "RHEL7-51292"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="register with a missing /var/lib/rhsm/facts/facts.json file",
			groups={"Tier2Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRegisterWithAnMissingRhsmFactsJsonFile() {
		
		Assert.assertTrue(RemoteFileTasks.testFileExists(client, clienttasks.rhsmFactsJsonFile)==1, "rhsm facts json file '"+clienttasks.rhsmFactsJsonFile+"' exists");
		log.info("Deleting rhsm facts json file '"+clienttasks.rhsmFactsJsonFile+"'...");
		RemoteFileTasks.runCommandAndWait(client, "rm -f "+clienttasks.rhsmFactsJsonFile, TestRecords.action());
		Assert.assertTrue(RemoteFileTasks.testFileExists(client, clienttasks.rhsmFactsJsonFile)==0, "rhsm facts json file '"+clienttasks.rhsmFactsJsonFile+"' has been removed");
		
		log.info("Attempt to register with a missing rhsm facts file (expecting success)...");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36524", "RHEL7-51296"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="register with interactive prompting for credentials",
			groups={"Tier2Tests","blockedByBug-678151","blockedbyBug-878986"},
			dataProvider = "getRegisterWithInteractivePromptingForCredentials_TestData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithInteractivePromptingForCredentials(Object bugzilla, String promptedUsername, String promptedPassword, String commandLineUsername, String commandLinePassword, String commandLineOrg, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex) {
		
		// ensure we are unregistered
		clienttasks.unregister(null,null,null, null);

		// call register while providing a valid username at the interactive prompt
		String command;
		if (client.runCommandAndWait("rpm -q expect").getExitCode().intValue()==0) {	// is expect installed?
			// assemble an ssh command using expect to simulate an interactive supply of credentials to the register command
			String promptedUsernames=""; if (promptedUsername!=null) for (String username : promptedUsername.split("\\n")) {
				promptedUsernames += "expect \\\"*Username:\\\"; send "+username+"\\\r;";
			}
			String promptedPasswords=""; if (promptedPassword!=null) for (String password : promptedPassword.split("\\n")) {
				promptedPasswords += "expect \\\"*Password:\\\"; send "+password+"\\\r;";
			}
			// [root@jsefler-onprem-5server ~]# expect -c "spawn subscription-manager register; expect \"*Username:\"; send qa@redhat.com\r; expect \"*Password:\"; send CHANGE-ME\r; expect eof; catch wait reason; exit [lindex \$reason 3]"
			command = String.format("expect -c \"spawn %s register %s %s %s; %s %s expect eof; catch wait reason; exit [lindex \\$reason 3]\"",
					clienttasks.command,
					commandLineUsername==null?"":"--username="+commandLineUsername,
					commandLinePassword==null?"":"--password="+commandLinePassword,
					commandLineOrg==null?"":"--org="+commandLineOrg,
					promptedUsernames,
					promptedPasswords);
		} else {
			// assemble an ssh command using echo and pipe to simulate an interactive supply of credentials to the register command
			// [root@jsefler-stage-6server ~]# echo -e "testuser1" | subscription-manager register --password password --org=admin
			String echoUsername= promptedUsername==null?"":promptedUsername;
			String echoPassword = promptedPassword==null?"":promptedPassword;
			String n = (promptedPassword!=null&&promptedUsername!=null)? "\n":"";	// \n works;  \r does not work
			command = String.format("echo -e \"%s\" | %s register %s %s %s",
					echoUsername+n+echoPassword,
					clienttasks.command,
					commandLineUsername==null?"":"--username="+commandLineUsername,
					commandLinePassword==null?"":"--password="+commandLinePassword,
					commandLineOrg==null?"":"--org="+commandLineOrg);
		}
		// attempt to register with the interactive credentials
		SSHCommandResult sshCommandResult = client.runCommandAndWait(command);
		
		// assert the sshCommandResult here
		if (expectedExitCode!=null) Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode, "The expected exit code from the register attempt.");
		if (expectedStdoutRegex!=null) Assert.assertContainsMatch(sshCommandResult.getStdout(), expectedStdoutRegex, "The expected stdout result from register while supplying interactive credentials.");
		if (expectedStderrRegex!=null)
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-1")) expectedStderrRegex = "HTTP error code 401: "+expectedStderrRegex;	// post commit 630e1a2eb06e6bfacac669ce11f38e228c907ea9 1507030: RestlibExceptions should show they originate server-side
			Assert.assertContainsMatch(sshCommandResult.getStderr(), expectedStderrRegex, "The expected stderr result from register while supplying interactive credentials.");
	}
	@DataProvider(name="getRegisterWithInteractivePromptingForCredentials_TestData")
	public Object[][] getRegisterWithInteractivePromptingForCredentials_TestDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRegisterWithInteractivePromptingForCredentials_TestDataAsListOfLists());
	}
	protected List<List<Object>> getRegisterWithInteractivePromptingForCredentials_TestDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (servertasks==null) return ll;
		if (clienttasks==null) return ll;
		
		String uErrMsg = servertasks.invalidCredentialsRegexMsg();
		String x = String.valueOf(getRandInt());
		if (client.runCommandAndWait("rpm -q expect").getExitCode().intValue()==0) {	// is expect installed?
			// Object bugzilla, String promptedUsername, String promptedPassword, String commandLineUsername, String commandLinePassword, String commandLineOwner, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername,			null,						null,				sm_clientPassword,	sm_clientOrg,	new Integer(0),		"The system has been registered with ID: [a-f,0-9,\\-]{36}",				null}));
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername+x,		null,						null,				sm_clientPassword,	sm_clientOrg,	new Integer(255),	uErrMsg,																	null}));
			ll.add(Arrays.asList(new Object[] {	null,	null,						sm_clientPassword,			sm_clientUsername,	null,				sm_clientOrg,	new Integer(0),		"The system has been registered with ID: [a-f,0-9,\\-]{36}",				null}));
			ll.add(Arrays.asList(new Object[] {	null,	null,						sm_clientPassword+x,		sm_clientUsername,	null,				sm_clientOrg,	new Integer(255),	uErrMsg,																	null}));
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername,			sm_clientPassword,			null,				null,				sm_clientOrg,	new Integer(0),		"The system has been registered with ID: [a-f,0-9,\\-]{36}",				null}));
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername+x,		sm_clientPassword+x,		null,				null,				sm_clientOrg,	new Integer(255),	uErrMsg,																	null}));
			ll.add(Arrays.asList(new Object[] {	null,	"\n\n"+sm_clientUsername,	"\n\n"+sm_clientPassword,	null,				null,				sm_clientOrg,	new Integer(0),		"(\nUsername: ){3}"+sm_clientUsername+"(\nPassword: ){3}"+"\nThe system has been registered with ID: [a-f,0-9,\\-]{36}",	null}));		
		} else {
			// Object bugzilla, String promptedUsername, String promptedPassword, String commandLineUsername, String commandLinePassword, String commandLineOwner, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername,			null,						null,				sm_clientPassword,	sm_clientOrg,	new Integer(0),		"The system has been registered with ID: [a-f,0-9,\\-]{36}",				null}));
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername+x,		null,						null,				sm_clientPassword,	sm_clientOrg,	new Integer(255),	null,																		uErrMsg}));
			ll.add(Arrays.asList(new Object[] {	null,	null,						sm_clientPassword,			sm_clientUsername,	null,				sm_clientOrg,	new Integer(0),		"The system has been registered with ID: [a-f,0-9,\\-]{36}",				null}));
			ll.add(Arrays.asList(new Object[] {	null,	null,						sm_clientPassword+x,		sm_clientUsername,	null,				sm_clientOrg,	new Integer(255),	null,																		uErrMsg}));
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername,			sm_clientPassword,			null,				null,				sm_clientOrg,	new Integer(0),		"The system has been registered with ID: [a-f,0-9,\\-]{36}",				null}));
			ll.add(Arrays.asList(new Object[] {	null,	sm_clientUsername+x,		sm_clientPassword+x,		null,				null,				sm_clientOrg,	new Integer(255),	null,																		uErrMsg}));
			ll.add(Arrays.asList(new Object[] {	null,	"\n\n"+sm_clientUsername,	"\n\n"+sm_clientPassword,	null,				null,				sm_clientOrg,	new Integer(0),		"(Username: ){3}The system has been registered with ID: [a-f,0-9,\\-]{36}",	"(Warning: Password input may be echoed.\nPassword: \n){3}"}));		
		}
		
		// for all rows with failing expectedExitCode, move expectedStdoutRegex to expectedStderrRegex when testing post subscription-manager-1.13.9-1
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258
			for (List<Object> l : ll) {
				// Object bugzilla, String promptedUsername, String promptedPassword, String commandLineUsername, String commandLinePassword, String commandLineOwner, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex
				Integer expectedExitCode = (Integer) l.get(6);
				String expectedStdoutRegex = (String) l.get(7);
				String expectedStderrRegex = (String) l.get(8);
				if (expectedExitCode.equals(255)) {
					l.set(6, new Integer(70));	// EX_SOFTWARE
					/* do not swap since expect stdout will include the stderr from subscription-manager
					if (expectedStdoutRegex!=null && expectedStderrRegex==null) {
						l.set(7, expectedStderrRegex);
						l.set(8, expectedStdoutRegex);
					}
					*/
				}
			}
		}
		return ll;
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36516", "RHEL7-51289"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: attempt register to --environment when the candlepin server does not support environments should fail",
			groups={"Tier2Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterToEnvironmentWhenCandlepinDoesNotSupportEnvironments() throws JSONException, Exception {
		// ask the candlepin server if it supports environment
		boolean supportsEnvironments = CandlepinTasks.isEnvironmentsSupported(sm_clientUsername, sm_clientPassword, sm_serverUrl);
		//boolean supportsEnvironments = CandlepinTasks.isEnvironmentsSupported(null,null, getServerUrl(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile,"hostname"), clienttasks.getConfFileParameter(clienttasks.rhsmConfFile,"port"), clienttasks.getConfFileParameter(clienttasks.rhsmConfFile,"prefix")));		// attempt to fix: INFO: I/O exception (javax.net.ssl.SSLException) caught when processing request: java.lang.RuntimeException: Could not generate DH keypair (org.apache.commons.httpclient.HttpMethodDirector.executeWithRetry)
		
		// skip this test when candlepin supports environments
		if (supportsEnvironments) throw new SkipException("Candlepin server '"+sm_serverHostname+"' appears to support environments, therefore this test is not applicable.");

		SSHCommandResult result = clienttasks.register_(sm_clientUsername,sm_clientPassword,sm_clientOrg,"foo",null,null,null,null,null,null,(String)null,null, null, null, true, null, null, null, null, null);
		
		// assert results
		Assert.assertEquals(result.getStderr().trim(), "Error: Server does not support environments.","Attempt to register to an environment on a server that does not support environments should be blocked.");
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(69);	// EX_UNAVAILABLE	// post commit 5697e3af094be921ade01e19e1dfe7b548fb7d5b bug 1119688
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "Exit code from register to environment when the candlepin server does NOT support environments.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36517", "RHEL7-59316"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: attempt register to --environment without --org option should block on missing org",
			groups={"Tier2Tests","blockedByBug-727092"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterToEnvironmentWithoutOrg() throws JSONException, Exception {
		// ask the candlepin server if it supports environment
		boolean supportsEnvironments = CandlepinTasks.isEnvironmentsSupported(sm_clientUsername, sm_clientPassword, sm_serverUrl);
		
		// This test implementation was valid prior to the implementation of Bug 727092 - [RFE]: Enhance subscription-manager to prompt the user for an Org Name.
		if (false) {
		SSHCommandResult result = clienttasks.register_(sm_clientUsername,sm_clientPassword,null,"foo_env",null,null,null,null,null,null,(String)null,null, null, null, true, null, null, null, null, null);

		// skip this test when candlepin does not support environments
		if (!supportsEnvironments) {
			// but before we skip, we can verify that environments are unsupported by this server
			Integer expectedExitCode = new Integer(255);
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(69);	// EX_UNAVAILABLE	// post commit 5697e3af094be921ade01e19e1dfe7b548fb7d5b bug 1119688
			Assert.assertEquals(result.getStderr().trim(), "Error: Server does not support environments.","Attempt to register to an environment on a server that does not support environments should be blocked.");
			Assert.assertEquals(result.getExitCode(), expectedExitCode,"Exit code from register to environment when the candlepin server does NOT support environments.");
			throw new SkipException("Candlepin server '"+sm_serverHostname+"' does not support environments, therefore this test is not applicable.");
		}

		// assert results when candlepin supports environments
		Assert.assertEquals(result.getStdout().trim(), "Error: Must specify --org to register to an environment.","Registering to an environment requires that the org be specified.");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255),"Exit code from register with environment option and without org option.");
		return;
		}
		
		// This test implementation is valid after the implementation of Bug 727092 - [RFE]: Enhance subscription-manager to prompt the user for an Org Name.
		// [root@10-16-120-133 ~]# expect -c "spawn subscription-manager register --username=testuser1 --password=password --environment=foo_env; expect \"Organization:\"; send foo_org\r; expect eof; catch wait reason; exit [lindex \$reason 3]"
		String command = String.format("expect -c \"spawn %s register %s %s %s; %s expect eof; catch wait reason; exit [lindex \\$reason 3]\"",
				clienttasks.command,
				"--username="+sm_clientUsername,
				"--password="+sm_clientPassword,
				"--environment="+"foo_env",
				"expect \\\"Organization:\\\"; send "+"foo_org"+"\\\r;");
		
		// attempt to register to environment without an org; expect interactive prompting for an organization
		SSHCommandResult result = client.runCommandAndWait(command);

		// skip this test when candlepin does not support environments
		if (!supportsEnvironments) {
			// but before we skip, we can verify that environments are unsupported by this server
			Integer expectedExitCode = new Integer(255);
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(69);	// EX_UNAVAILABLE	// post commit 5697e3af094be921ade01e19e1dfe7b548fb7d5b bug 1119688
			String expectedErrorMsg = "Error: Server does not support environments.";
			Assert.assertTrue(result.getStdout().trim().endsWith(expectedErrorMsg),"An attempt to register to an environment on a server that does not support environments report: "+expectedErrorMsg);
			Assert.assertEquals(result.getExitCode(), expectedExitCode,"Exit code from an attempt to register to an environment on a server that does not support environments.");
			throw new SkipException("Candlepin server '"+sm_serverHostname+"' does not support environments, therefore this test is not applicable.");
		}

		// assert results when candlepin supports environments
		String expectedErrorMsg = String.format("Couldn't find Organization '%s'."+"foo_org");
		Assert.assertTrue(result.getStdout().trim().endsWith(expectedErrorMsg),"An attempt to register with environment option and without org option (but supplied a unknown org at prompt) should report: "+expectedErrorMsg);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255),"Exit code from register with environment option and without org option (but supplied a unknown org at prompt).");
	}
	
	
	protected String rhsm_baseurl = null;
	@BeforeGroups(value={"RegisterWithBaseurl_Test"}, alwaysRun=true)
	public void beforeRegisterWithBaseurl_Test() {
		if (clienttasks==null) return;
		rhsm_baseurl = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "baseurl");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19950", "RHEL7-51000"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager-cli: register with --baseurl",
			dataProvider="getRegisterWithBaseurl_TestData",
			groups={"Tier1Tests","RegisterWithBaseurl_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithBaseurl(Object bugzilla, String baseurl, String baseurlConfigured, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex) {
		// get original baseurl at the beginning of this test
		//String baseurlBeforeTest = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm","baseurl");
		String baseurlBeforeTest = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "baseurl");
		
		// register with a baseurl
		SSHCommandResult sshCommandResult = clienttasks.register_(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null, false, null, null, (String)null, null, null, baseurl, true, null, null, null, null, null);
		
		// assert the sshCommandResult here
		if (expectedExitCode!=null) Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode,"ExitCode after register with --baseurl="+baseurl+" and other options:");
		if (expectedStdoutRegex!=null) Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), expectedStdoutRegex,"Stdout after register with --baseurl="+baseurl+" and other options:");
		if (expectedStderrRegex!=null) Assert.assertContainsMatch(sshCommandResult.getStderr().trim(), expectedStderrRegex,"Stderr after register with --baseurl="+baseurl+" and other options:");
		Assert.assertContainsNoMatch(sshCommandResult.getStderr().trim(), "Traceback.*","Stderr after register with --baseurl="+baseurl+" and other options should not contain a Traceback.");
	
		// negative testcase assertions........
		if (Integer.valueOf(expectedExitCode)>1) {	// formerly if (expectedExitCode.equals(new Integer(255))) {
			// assert that the current config remains unchanged when the expectedExitCode is 255
			//Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm","baseurl"), baseurlBeforeTest, "The rhsm configuration for baseurl should remain unchanged when attempting to register with an invalid baseurl.");
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "baseurl"), baseurlBeforeTest, "The "+clienttasks.rhsmConfFile+" configuration for [rhsm] baseurl should remain unchanged when attempting to register with an invalid baseurl.");
		}
		
		// ignored testcase assertions........
		else if (baseurl.isEmpty()) {
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "baseurl"), baseurlBeforeTest, "The "+clienttasks.rhsmConfFile+" configuration for [rhsm] baseurl should remain unchanged when attempting to register with an empty baseurl.");
		}
		
		// positive testcase assertions........
		else {
			// assert that the current config has been updated to the new expected baseurl
			//Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm","baseurl"), baseurlConfigured, "The rhsm configuration for baseurl has been updated to the new baseurl with correct format (https://hostname:443/prefix).");
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "baseurl"), baseurlConfigured, "The "+clienttasks.rhsmConfFile+" configuration for [rhsm] baseurl has been updated to the new baseurl with correct format (https://hostname:443/prefix).");
		}
		
//		// TODO maybe assert yumRepos  7/24/2012
//		for (YumRepo yumRepo : clienttasks.getCurrentlySubscribedYumRepos()) {
//			Assert.assertTrue(yumRepo.baseurl.matches("^"+baseurlConfigured.replaceFirst("/$","")+"/\\w+.*"),"Newly configured baseurl '"+baseurlConfigured+"' is utilized by yumRepo: "+yumRepo);
//		}
//		
//		// TODO maybe assert repos --list  7/24/2012
//		for (Repo repo : clienttasks.getCurrentlySubscribedRepos()) {
//			Assert.assertTrue(repo.repoUrl.matches("^"+baseurlConfigured.replaceFirst("/$","")+"/\\w+.*"),"Newly configured baseurl '"+baseurlConfigured+"' is utilized by repo: "+repo);
//		}

	}
	@AfterGroups(value={"RegisterWithBaseurl_Test"},alwaysRun=true)
	public void afterRegisterWithBaseurl_Test() {
		if (rhsm_baseurl!=null) clienttasks.config(null,null,true,new String[]{"rhsm","baseurl",rhsm_baseurl});
	}
	
	@DataProvider(name="getRegisterWithBaseurl_TestData")
	public Object[][] getRegisterWithBaseurl_TestDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRegisterWithBaseurl_TestDataAsListOfLists());
	}
	protected List<List<Object>> getRegisterWithBaseurl_TestDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (servertasks==null) return ll;
		if (clienttasks==null) return ll;
		String defaultHostname = "cdn.redhat.com";
		
		//  --baseurl=BASE_URL    base url for content in form of https://hostname:443/prefix

		// Object bugzilla, String baseurl, String baseurlConfigured, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex
		// positive tests
		ll.add(Arrays.asList(new Object[] {	null,													"https://myhost.example.com:900/myapp/",	"https://myhost.example.com:900/myapp/",	new Integer(0),		null,			null}));
		// 842830 CLOSED WONTFIX ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("842830"),	"http://myhost.example.com:900/myapp/",		"http://myhost.example.com:900/myapp/",		new Integer(0),		null,			null}));
		ll.add(Arrays.asList(new Object[] {	null,													"https://https:900/myapp/",					"https://https:900/myapp/",					new Integer(0),		null,			null}));
		ll.add(Arrays.asList(new Object[] {	null,													"https://http:900/myapp/",					"https://http:900/myapp/",					new Integer(0),		null,			null}));
		// 842830 CLOSED WONTFIX ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("842830"),	"http://http:900/myapp/",					"http://http:900/myapp/",					new Integer(0),		null,			null}));
		ll.add(Arrays.asList(new Object[] {	null,													"myhost.example.com:900/myapp/",			"https://myhost.example.com:900/myapp/",	new Integer(0),		null,			null}));
		ll.add(Arrays.asList(new Object[] {	null,													"myhost.example.com:900/myapp",				"https://myhost.example.com:900/myapp",		new Integer(0),		null,			null}));
		ll.add(Arrays.asList(new Object[] {	null,													"myhost.example.com:900/",					/*"https://myhost.example.com:900/"*/"https://myhost.example.com:900",			new Integer(0),		null,			null}));
		ll.add(Arrays.asList(new Object[] {	null,													"myhost.example.com:900",					"https://myhost.example.com:900",			new Integer(0),		null,			null}));
		ll.add(Arrays.asList(new Object[] {	null,													"myhost.example.com/",						/*"https://myhost.example.com/"*/"https://myhost.example.com",					new Integer(0),		null,			null}));
		ll.add(Arrays.asList(new Object[] {	null,													"myhost.example.com/myapp",					"https://myhost.example.com/myapp",			new Integer(0),		null,			null}));
		ll.add(Arrays.asList(new Object[] {	null,													"myhost.example.com/myapp/",				"https://myhost.example.com/myapp/",		new Integer(0),		null,			null}));
		ll.add(Arrays.asList(new Object[] {	null,													"myhost.example.com",						"https://myhost.example.com",				new Integer(0),		null,			null}));
		ll.add(Arrays.asList(new Object[] {	null,													"myhost-examp_e.com",						"https://myhost-examp_e.com",				new Integer(0),		null,			null}));
		ll.add(Arrays.asList(new Object[] {	null,													"myhost",									"https://myhost",							new Integer(0),		null,			null}));
		ll.add(Arrays.asList(new Object[] {	null,													"/myapp",									"https://"+defaultHostname+"/myapp",		new Integer(0),		null,			null}));
		ll.add(Arrays.asList(new Object[] {	null,													":900/myapp",								"https://"+defaultHostname+":900/myapp",	new Integer(0),		null,			null}));

		// ignored tests
		ll.add(Arrays.asList(new Object[] {	null,													"",											/*ll.get(ll.size()-1).get(2) last set, assuming the last test row passes */null,	new Integer(0),		null,			null}));	
	
		// negative tests
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.9-1")) {	// post commit a695ef2d1da882c5f851fde90a24f957b70a63ad
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496","878634"}),				"https://hostname:/prefix",					null,			new Integer(70),	"Error parsing baseurl:",	"Server URL port should be numeric"}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496","878634","842845",}),		"https://hostname:PORT/prefix",				null,			new Integer(70),	"Error parsing baseurl:",	"Server URL port should be numeric"}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						"https:/hostname",							null,			new Integer(70),	"Error parsing baseurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						"https:hostname/prefix",					null,			new Integer(70),	"Error parsing baseurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						"http//hostname/prefix",					null,			new Integer(70),	"Error parsing baseurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						"http/hostname/prefix",						null,			new Integer(70),	"Error parsing baseurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						"ftp://hostname",							null,			new Integer(70),	"Error parsing baseurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						"git://hostname/prefix",					null,			new Integer(70),	"Error parsing baseurl:",	"Server URL has an invalid scheme. http:// and https:// are supported"}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						"https://",									null,			new Integer(70),	"Error parsing baseurl:",	"Server URL is just a schema. Should include hostname, and/or port and path"}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						"http://",									null,			new Integer(70),	"Error parsing baseurl:",	"Server URL is just a schema. Should include hostname, and/or port and path"}));
			//ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						TODO "DON'T KNOW WHAT TO PUT HERE TO INVOKE THE ERROR; see exceptions.py",	null,	new Integer(70),	"Error parsing baseurl:\nServer URL can not be empty",	null}));
			//ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1119688","1044686","1054496"}),						TODO "DON'T KNOW WHAT TO PUT HERE TO INVOKE THE ERROR; see exceptions.py",	null,	new Integer(70),	"Error parsing baseurl:\nServer URL can not be None",	null}));

		} else if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.12-1")) {
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496","878634"}),				"https://hostname:/prefix",					null,			new Integer(255),	"Error parsing baseurl:\nServer URL port should be numeric",											null}));	// "Error parsing baseurl:\nServer URL port could not be parsed",											null}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496","878634","842845",}),	"https://hostname:PORT/prefix",				null,			new Integer(255),	"Error parsing baseurl:\nServer URL port should be numeric",											null}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						"https:/hostname",							null,			new Integer(255),	"Error parsing baseurl:\nServer URL has an invalid scheme. http:// and https:// are supported",			null}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						"https:hostname/prefix",					null,			new Integer(255),	"Error parsing baseurl:\nServer URL has an invalid scheme. http:// and https:// are supported",			null}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						"http//hostname/prefix",					null,			new Integer(255),	"Error parsing baseurl:\nServer URL has an invalid scheme. http:// and https:// are supported",			null}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						"http/hostname/prefix",						null,			new Integer(255),	"Error parsing baseurl:\nServer URL has an invalid scheme. http:// and https:// are supported",			null}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						"ftp://hostname",							null,			new Integer(255),	"Error parsing baseurl:\nServer URL has an invalid scheme. http:// and https:// are supported",			null}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						"git://hostname/prefix",					null,			new Integer(255),	"Error parsing baseurl:\nServer URL has an invalid scheme. http:// and https:// are supported",			null}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						"https://",									null,			new Integer(255),	"Error parsing baseurl:\nServer URL is just a schema. Should include hostname, and/or port and path",	null}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						"http://",									null,			new Integer(255),	"Error parsing baseurl:\nServer URL is just a schema. Should include hostname, and/or port and path",	null}));
			//ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						TODO "DON'T KNOW WHAT TO PUT HERE TO INVOKE THE ERROR; see exceptions.py",	null,	new Integer(255),	"Error parsing baseurl:\nServer URL can not be empty",	null}));
			//ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"1044686","1054496"}),						TODO "DON'T KNOW WHAT TO PUT HERE TO INVOKE THE ERROR; see exceptions.py",	null,	new Integer(255),	"Error parsing baseurl:\nServer URL can not be None",	null}));

		} else {
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"878634"}),				"https://hostname:/prefix",					null,			new Integer(255),	"Error parsing baseurl: Server URL port could not be parsed",											null}));
			ll.add(Arrays.asList(new Object[] {	new BlockedByBzBug(new String[]{"878634","842845"}),	"https://hostname:PORT/prefix",				null,			new Integer(255),	"Error parsing baseurl: Server URL port should be numeric",												null}));
			ll.add(Arrays.asList(new Object[] {	null,													"https:/hostname",							null,			new Integer(255),	"Error parsing baseurl: Server URL has an invalid scheme. http:// and https:// are supported",			null}));
			ll.add(Arrays.asList(new Object[] {	null,													"https:hostname/prefix",					null,			new Integer(255),	"Error parsing baseurl: Server URL has an invalid scheme. http:// and https:// are supported",			null}));
			ll.add(Arrays.asList(new Object[] {	null,													"http//hostname/prefix",					null,			new Integer(255),	"Error parsing baseurl: Server URL has an invalid scheme. http:// and https:// are supported",			null}));
			ll.add(Arrays.asList(new Object[] {	null,													"http/hostname/prefix",						null,			new Integer(255),	"Error parsing baseurl: Server URL has an invalid scheme. http:// and https:// are supported",			null}));
			ll.add(Arrays.asList(new Object[] {	null,													"ftp://hostname",							null,			new Integer(255),	"Error parsing baseurl: Server URL has an invalid scheme. http:// and https:// are supported",			null}));
			ll.add(Arrays.asList(new Object[] {	null,													"git://hostname/prefix",					null,			new Integer(255),	"Error parsing baseurl: Server URL has an invalid scheme. http:// and https:// are supported",			null}));
			ll.add(Arrays.asList(new Object[] {	null,													"https://",									null,			new Integer(255),	"Error parsing baseurl: Server URL is just a schema. Should include hostname, and/or port and path",	null}));
			ll.add(Arrays.asList(new Object[] {	null,													"http://",									null,			new Integer(255),	"Error parsing baseurl: Server URL is just a schema. Should include hostname, and/or port and path",	null}));
		}
		
		// for all rows, change the expected exitCode when testing post subscription-manager-1.13.8-1
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			for (List<Object> l : ll) {
				// Object bugzilla, String baseurl, String baseurlConfigured, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex
				Integer expectedExitCode = (Integer) l.get(3);
				if (expectedExitCode.equals(255)) {
					BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);	// get the existing BlockedByBzBug
					List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
					bugIds.add("1119688");	// Bug 1119688 - [RFE] subscription-manager better usability for scripts
					blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
					l.set(0, blockedByBzBug);
					l.set(3, new Integer(70));	// EX_SOFTWARE
				}
			}
		}
		
		return ll;
	}
	
	
	
	protected String server_hostname = null;
	protected String server_port = null;
	protected String server_prefix = null;
	@BeforeGroups(value={"RegisterWithServerurl_Test","RegisterWithServerurlAutosubscribeAndBadServicelevel_Test"}, groups={"setup"})
	public void beforeRegisterWithServerurl_Test() {
		if (clienttasks==null) return;
		if (server_hostname==null)	server_hostname	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname");
		if (server_port==null)		server_port		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port");
		if (server_prefix==null)	server_prefix	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "prefix");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19952", "RHEL7-51001"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager-cli: register with --serverurl; assert positive registrations persist the serverurl to rhsm.conf, negative registrations do not.",
			dataProvider="getServerurlData",
			groups={"Tier1Tests","RegisterWithServerurl_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithServerurl(Object bugzilla, String serverurl, String expectedHostname, String expectedPort, String expectedPrefix, Integer expectedExitCode, String expectedStdoutRegex, String expectedStderrMatch) {
		// get original server at the beginning of this test
		String hostnameBeforeTest	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname");
		String portBeforeTest		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port");
		String prefixBeforeTest		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix");
		
		// register with a serverurl
		SSHCommandResult sshCommandResult = clienttasks.register_(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null, false, null, null, (String)null, serverurl, null, null, true, null, null, null, null, null);
		
		// assert the sshCommandResult here
		if (expectedExitCode!=null)	Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode,"ExitCode after register with --serverurl="+serverurl+" and other options:");
		if (expectedStdoutRegex!=null)	Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), expectedStdoutRegex,"Stdout after register with --serverurl="+serverurl+" and other options:");
		if (expectedStderrMatch!=null)	Assert.assertContainsMatch(sshCommandResult.getStderr().trim(), expectedStderrMatch,"Stderr after register with --serverurl="+serverurl+" and other options:");
		Assert.assertContainsNoMatch(sshCommandResult.getStderr().trim(), "Traceback.*","Stderr after register with --serverurl="+serverurl+" and other options should not contain a Traceback.");
		
		// negative testcase assertions........
		//if (expectedExitCode.equals(new Integer(255))) {
		if (Integer.valueOf(expectedExitCode)>1) {
			// assert that the current config remains unchanged when the expected registration result is a failure
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname"), hostnameBeforeTest, "The "+clienttasks.rhsmConfFile+" configuration for [server] hostname should remain unchanged when attempting to register with an invalid serverurl.");
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port"),	portBeforeTest, "The "+clienttasks.rhsmConfFile+" configuration for [server] port should remain unchanged when attempting to register with an invalid serverurl.");
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix"), prefixBeforeTest, "The "+clienttasks.rhsmConfFile+" configuration for [server] prefix should remain unchanged when attempting to register with an invalid serverurl.");
			
			return;	// nothing more to do after these negative testcase assertions;
		}
		
		// positive testcase assertions........
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname"), expectedHostname, "The "+clienttasks.rhsmConfFile+" configuration for [server] hostname has been updated from the specified --serverurl "+serverurl);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port"), expectedPort, "The "+clienttasks.rhsmConfFile+" configuration for [server] port has been updated from the specified --serverurl "+serverurl);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix"), expectedPrefix, "The "+clienttasks.rhsmConfFile+" configuration for [server] prefix has been updated from the specified --serverurl "+serverurl);
	}
	@AfterGroups(value={"RegisterWithServerurl_Test","RegisterWithServerurlAutosubscribeAndBadServicelevel_Test"}, groups={"setup"})
	public void afterRegisterWithServerurl_Test() {
		if (server_hostname!=null)	clienttasks.config(null,null,true,new String[]{"server","hostname",server_hostname});
		if (server_port!=null)		clienttasks.config(null,null,true,new String[]{"server","port",server_port});
		if (server_prefix!=null)	clienttasks.config(null,null,true,new String[]{"server","prefix",server_prefix});
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36527", "RHEL7-51300"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: register with good --serverurl --autosubscribe and bad --servicelevel; assert persistance of serverurl from good registration",
			groups={"Tier2Tests","RegisterWithServerurlAutosubscribeAndBadServicelevel_Test","blockedByBug-1221273"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithServerurlAutosubscribeAndBadServicelevel() throws JSONException, Exception {
		clienttasks.unregister(null, null, null, null);
		
		// get the original good serverurl from the rhsm.conf file
		String goodHostname	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname");
		String goodPort		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port");
		String goodPrefix	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix");
		String serverurl=goodHostname+":"+goodPort+goodPrefix;
		
		// bork the serverurl in the rhsm.conf file
		clienttasks.config(null, null, true, Arrays.asList(new String[]{"server","hostname","bad-server.redhat.com"},new String[]{"server","port","1234"},new String[]{"server","prefix","/bad-prefix"}));
		
		SSHCommandResult sshCommandResult = clienttasks.register_(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null, true, "bad-service", null, (String)null, serverurl, null, null, null, null, null, null, null, null);
		//	201505221639:19.676 - FINE: ssh root@jsefler-os6.usersys.redhat.com subscription-manager register --username=testuser1 --password=password --org=admin --autosubscribe --servicelevel=bad-service --serverurl=jsefler-f14-candlepin.usersys.redhat.com:8443/candlepin
		//	201505221639:23.089 - FINE: Stdout: The system has been registered with ID: 4e9e924d-2a5d-42ca-a78f-501716d62f56
		//	201505221639:23.091 - FINE: Stderr: Service level 'bad-service' is not available to units of organization admin.
		//	201505221639:23.094 - FINE: ExitCode: 70
		
		//	2015-08-17 14:29:18.700  FINE: ssh root@jsefler-7.usersys.redhat.com subscription-manager register --username=testuser1 --password=password --org=admin --autosubscribe --servicelevel=bad-service --serverurl=jsefler-f22-candlepin.usersys.redhat.com:8443/candlepin
		//	2015-08-17 14:29:21.558  FINE: Stdout: 
		//	Registering to: jsefler-f22-candlepin.usersys.redhat.com:8443/candlepin
		//	The system has been registered with ID: 8f6a5c10-d9e8-475f-947f-a39531e8681c 
		//	Service level 'bad-service' is not available to units of organization admin.
		//
		//	Product Name: Red Hat Enterprise Linux Server
		//	Status:       Not Subscribed
		//
		//	Unable to find available subscriptions for all your installed products.
		//
		//	2015-08-17 14:29:21.559  FINE: Stderr: 
		//	2015-08-17 14:29:21.559  FINE: ExitCode: 1
		
		// Assert a successful registration identity
		//String registeredOwnerKey = clienttasks.getCurrentlyRegisteredOwnerKey();
		String consumerId = clienttasks.getCurrentConsumerId(sshCommandResult);
		String registeredOwnerKey = CandlepinTasks.getOwnerOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId).getString("key");
		
		// Assert the command returned a error with "Service level 'bad-service' is not available to units of organization admin."
		Integer expectedExitCode = Integer.valueOf(70);/*EX_SOFTWARE*/
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.15.9-5")) expectedExitCode = Integer.valueOf(1);	// subscription-manager RHEL7.2 commit 84340a0acda9f070e3e0b733e4335059b5dc204e 	// post 1221273: Auto-attach failure should not short-circuit other parts of registration
		Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode,"ExitCode after register with --serverurl="+serverurl+" and a bad servicelevel.");
		String expectedStderr = String.format("Service level '%s' is not available to units of organization %s.","bad-service",registeredOwnerKey);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
			expectedStderr = String.format("Service level \"%s\" is not available to units of organization %s.","bad-service",registeredOwnerKey);
		}
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.16.3-1")) {	// subscription-manager master commit 7795df84edcb4f4fef08085548f6c2a23f86ceb4 1262919: Added convenience function for printing to stderr
			Assert.assertEquals(sshCommandResult.getStderr().trim(), expectedStderr, "Stderr after register with --serverurl="+serverurl+" and a bad servicelevel");
		} else if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.15.9-5")) {	// subscription-manager RHEL7.2 commit 84340a0acda9f070e3e0b733e4335059b5dc204e 	// post 1221273: Auto-attach failure should not short-circuit other parts of registration	// subscription-manager master commit fef344066a4d5e40a21188797d6c6197e03a1638 >= subscription-manager-1.16.1-1
			Assert.assertTrue(sshCommandResult.getStdout().trim().contains(expectedStderr), "Stdout after register with --serverurl="+serverurl+" and a bad servicelevel contains expected '"+expectedStderr+"'.");
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "Stderr after register with --serverurl="+serverurl+" and a bad servicelevel");
		} else {
			Assert.assertEquals(sshCommandResult.getStderr().trim(), expectedStderr, "Stderr after register with --serverurl="+serverurl+" and a bad servicelevel");
		}
		
		// Assert the serverurl persisted to rhsm.conf
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname"), goodHostname, "The "+clienttasks.rhsmConfFile+" configuration for [server] hostname has been updated from the specified --serverurl "+serverurl);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port"), goodPort, "The "+clienttasks.rhsmConfFile+" configuration for [server] port has been updated from the specified --serverurl "+serverurl);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix"), goodPrefix, "The "+clienttasks.rhsmConfFile+" configuration for [server] prefix has been updated from the specified --serverurl "+serverurl);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36521", "RHEL7-51293"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: register with --autosubscribe and --auto-attach can be used interchangably",
			groups={"Tier2Tests","blockedByBug-874749","blockedByBug-874804"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithAutoattachDeprecatesAutosubscribe() throws Exception {
		SSHCommandResult result = client.runCommandAndWait(clienttasks.command+" register --help");
		Assert.assertContainsMatch(result.getStdout(), "^\\s*--autosubscribe\\s+Deprecated, see --auto-attach$");
		
		clienttasks.unregister(null,null,null, null);
		SSHCommandResult autosubscribeResult = clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		String autosubscribeResultAsString = autosubscribeResult.toString();
		autosubscribeResultAsString = autosubscribeResultAsString.replaceAll("[a-f,0-9,\\-]{36}", "<UUID>");	// normalize the UUID so we can ignore it when asserting equal results
		
		clienttasks.unregister(null,null,null, null);
		SSHCommandResult autoattachResult = client.runCommandAndWait(clienttasks.command+" register --auto-attach --username=\""+sm_clientUsername+"\" --password=\""+sm_clientPassword+"\" " + (sm_clientOrg==null?"":"--org="+sm_clientOrg));
		String autoattachResultAsString = autoattachResult.toString();
		autoattachResultAsString = autoattachResultAsString.replaceAll("[a-f,0-9,\\-]{36}", "<UUID>");	// normalize the UUID so we can ignore it when asserting equal results
		Assert.assertEquals(autosubscribeResultAsString, autoattachResultAsString, "Results from register with '--autosubscribe' and '--auto-attach' options should be identical.");
	}
	
	
	protected String rhsm_ca_cert_dir = null;
	protected String server_insecure = null;
	@BeforeGroups(value={"RegisterWithInsecure_Test"}, groups={"setup"})
	public void beforeRegisterWithInsecure_Test() {
		if (clienttasks==null) return;
		rhsm_ca_cert_dir	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "ca_cert_dir");
		server_insecure		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "insecure");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36523", "RHEL7-51295"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: register with --insecure",
			groups={"Tier2Tests","RegisterWithInsecure_Test","blockedByBug-844411","blockedByBug-993202"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithInsecure() {
		SSHCommandResult sshCommandResult;
		
		// calling register without insecure should pass
		sshCommandResult = clienttasks.register(sm_clientUsername,sm_clientPassword, sm_clientOrg, null,null,null,null,null,null,null,(String)null,null,false,null,true,null,null,null,null, null);
		clienttasks.unregister(null, null, null, null);
		
		// change the rhsm.ca_cert_dir configuration to simulate a missing candlepin ca cert
		client.runCommandAndWait("mkdir -p /tmp/emptyDir");
		sshCommandResult = clienttasks.config(null, null, true, new String[]{"rhsm","ca_cert_dir","/tmp/emptyDir/"});
		
		// calling register without insecure should now fail (throwing stderr "certificate verify failed")
		sshCommandResult = clienttasks.register_(sm_clientUsername,sm_clientPassword, sm_clientOrg, null,null,null,null,null,null,null,(String)null,null,false,null,null,null,null,null,null, null);
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(70);	// EX_SOFTWARE	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode, "Exitcode from the register command when configuration rhsm.ca_cert_dir has been falsified.");
		
		if (clienttasks.isPackageVersion("python-rhsm",">=","1.18.5-1") && Integer.valueOf(clienttasks.redhatReleaseX)>=7) {	// post python-rhsm commit 214103dcffce29e31858ffee414d79c1b8063970   Reduce usage of m2crypto (#184) (RHEL7+)
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "Unable to verify server's identity: [SSL: CERTIFICATE_VERIFY_FAILED] certificate verify failed (_ssl.c:579)", "Stderr from the register command when configuration rhsm.ca_cert_dir has been falsified.");
			Assert.assertEquals(sshCommandResult.getStdout().trim(), String.format("Registering to: %s:%s%s",clienttasks.getConfParameter("hostname"),clienttasks.getConfParameter("port"),clienttasks.getConfParameter("prefix")), "Stdout from the register command when configuration rhsm.ca_cert_dir has been falsified.");
		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.15.9-2")) {	// post subscription-manager commit d5014cda1c234d36943383b69898f2a651202b89   Bug 985157 - [RFE] Specify which username to enter when registering with subscription-manager
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "Unable to verify server's identity: certificate verify failed", "Stderr from the register command when configuration rhsm.ca_cert_dir has been falsified.");
			Assert.assertEquals(sshCommandResult.getStdout().trim(), String.format("Registering to: %s:%s%s",clienttasks.getConfParameter("hostname"),clienttasks.getConfParameter("port"),clienttasks.getConfParameter("prefix")), "Stdout from the register command when configuration rhsm.ca_cert_dir has been falsified.");
		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.9-1")) {	// post commit a695ef2d1da882c5f851fde90a24f957b70a63ad
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "Unable to verify server's identity: certificate verify failed", "Stderr from the register command when configuration rhsm.ca_cert_dir has been falsified.");
			Assert.assertEquals(sshCommandResult.getStdout().trim(), "", "Stdout from the register command when configuration rhsm.ca_cert_dir has been falsified.");
		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.9-1")) {	// post commit 3366b1c734fd27faf48313adf60cf051836af115
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "Stderr from the register command when configuration rhsm.ca_cert_dir has been falsified.");
			Assert.assertEquals(sshCommandResult.getStdout().trim(), "Unable to verify server's identity: certificate verify failed", "Stdout from the register command when configuration rhsm.ca_cert_dir has been falsified.");
		} else {
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "certificate verify failed", "Stderr from the register command when configuration rhsm.ca_cert_dir has been falsified.");
			Assert.assertEquals(sshCommandResult.getStdout().trim(), "", "Stdout from the register command when configuration rhsm.ca_cert_dir has been falsified.");
		}
		
		// calling register with insecure should now pass
		sshCommandResult = clienttasks.register(sm_clientUsername,sm_clientPassword, sm_clientOrg, null,null,null,null,null,null,null,(String)null,null,true,null,null,null,null,null,null, null);
		
		// assert that option --insecure did persist to rhsm.conf
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "insecure"), "1", "Expected value of "+clienttasks.rhsmConfFile+" server.insecure configuration.  Use of the --insecure option when calling the register module should be persisted to rhsm.conf as true.");
		clienttasks.unregister(null, null, null, null);
	}
	@AfterGroups(value={"RegisterWithInsecure_Test"},groups={"setup"})
	public void afterRegisterWithInsecure_Test() {
		if (rhsm_ca_cert_dir!=null) clienttasks.config(null, null, true, new String[]{"rhsm","ca_cert_dir",rhsm_ca_cert_dir});
		if (server_insecure!=null) clienttasks.config(null, null, true, new String[]{"server","insecure",server_insecure});
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36528", "RHEL7-51301"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: register with leading or trailing whitespace on --username",
			groups={"Tier2Tests","blockedByBug-1023166"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithWhitespaceOnUsername() {
		String usernameWithWhitespace;
		SSHCommandResult sshCommandResult;
		
		// calling register with trailing whitespace in username
		usernameWithWhitespace = sm_clientUsername+"  ";
		sshCommandResult = clienttasks.register_(usernameWithWhitespace,sm_clientPassword, sm_clientOrg, null,null,null,null,null,null,null,(String)null,null,false,null,true,null,null,null,null, null);
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the register command specifying username=\""+usernameWithWhitespace+"\" (with trailing whitespace) indicates a success.  Bug 1023166 requested trimming the username.");

		// calling register with leading whitespace in username
		usernameWithWhitespace = "  "+sm_clientUsername;
		sshCommandResult = clienttasks.register_(usernameWithWhitespace,sm_clientPassword, sm_clientOrg, null,null,null,null,null,null,null,(String)null,null,false,null,true,null,null,null,null, null);
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the register command specifying username=\""+usernameWithWhitespace+"\" (with leading whitespace) indicates a success.  Bug 1023166 requested trimming the username.");
	}
	
	
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47919", "RHEL7-97262"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28485",	// RHSM-REQ : subscription-manager cli registration and deregistration
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-91067",	// RHSM-REQ : subscription-manager cli registration and deregistration
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: register with LC_ALL=C should succeed.",
			groups={"Tier1Tests","blockedByBug-1445387"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterWithLC_ALL_C() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null, null);
		
		// register normally without specifying LC_ALL=C
		SSHCommandResult sshCommandResultWithoutLC_ALL_C = clienttasks.register_(sm_clientUsername,sm_clientPassword, sm_clientOrg, null,null,null,null,null,null,null,(String)null,null,null,null,null,null,null,null,null, null);
		String consumeridWithoutLC_ALL_C = clienttasks.getCurrentConsumerId(sshCommandResultWithoutLC_ALL_C);
		clienttasks.unregister_(null, null, null, null);
		
		// register while specifying LC_ALL=C
		SSHCommandResult sshCommandResultWithLC_ALL_C = client.runCommandAndWait("LC_ALL=C "+clienttasks.registerCommand(sm_clientUsername,sm_clientPassword, sm_clientOrg, null,null,null,null,null,null,null,(String)null,null,null,null,null,null,null,null,null, null));
		String consumeridWithLC_ALL_C = clienttasks.getCurrentConsumerId(sshCommandResultWithLC_ALL_C);
		clienttasks.unregister_(null, null, null, null);
		
		// compared register results should be identical regardless of LC_ALL=C
		Assert.assertEquals(sshCommandResultWithLC_ALL_C.toString().replace(consumeridWithLC_ALL_C, "{UUID}"), sshCommandResultWithoutLC_ALL_C.toString().toString().replace(consumeridWithoutLC_ALL_C, "{UUID}"), "Ignoring the actual consumerid, the results from register with and without LC_ALL=C are identical.");
	}
	
	
	// Candidates for an automated Test:
	// TODO Bug 627685 - subscription-manager-cli reregister without specifying consumerid may unintentionally remove entitlements https://github.com/RedHatQE/rhsm-qe/issues/181
	// TODO Bug 627665 - subscription-manager-cli reregister should not allow a --username to reregister using a --consumerid that belongs to someone else https://github.com/RedHatQE/rhsm-qe/issues/182
	// TODO Bug 668814 - firstboot and subscription-manager display "network error" on server 500 https://github.com/RedHatQE/rhsm-qe/issues/183
	// TODO Bug 669395 - gui defaults to consumer name of the hostname and doesn't let you set it to empty string. cli defaults to username, and does let you set it to empty string https://github.com/RedHatQE/rhsm-qe/issues/184
	// TODO Bug 693896 - subscription-manager does not always reload dbus scripts automatically //done https://github.com/RedHatQE/rhsm-qe/issues/185
	// TODO Bug 719378 - White space in user name causes error //done https://github.com/RedHatQE/rhsm-qe/issues/186
	
	
	// Protected Class Variables ***********************************************************************
	
	protected final String tmpProductCertDir = "/tmp/productCertDir";	// TODO Not being used anymore; DELETEME
	protected String productCertDir = null;	// TODO Not being used anymore; DELETEME


	
	// Configuration methods ***********************************************************************

	@AfterGroups(groups={"setup"}, value={"RegisterWithAutosubscribe_Test"})
	@AfterClass (alwaysRun=true)
	public void cleaupAfterClass() {
		if (clienttasks==null) return;
		
		// restore the originally configured productCertDir
		if (this.productCertDir!=null) clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", this.productCertDir);
		
		// delete temporary files and directories
		client.runCommandAndWait("rm -rf "+tmpProductCertDir);
	}

	// Protected methods ***********************************************************************

	protected void checkInvalidRegistrationStrings(SSHCommandRunner sshCommandRunner, String username, String password){
		sshCommandRunner.runCommandAndWait("subscription-manager-cli register --username="+username+this.getRandInt()+" --password="+password+this.getRandInt()+" --force");
		Assert.assertContainsMatch(sshCommandRunner.getStdout(),
				"Invalid username or password. To create a login, please visit https:\\/\\/www.redhat.com\\/wapps\\/ugc\\/register.html");
	}
	
	
	
	// Data Providers ***********************************************************************

}
