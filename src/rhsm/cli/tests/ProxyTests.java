package rhsm.cli.tests;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.jul.TestRecords;

import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.SubscriptionManagerTasks;
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
 * @author jsefler
 *
 */
@Test(groups={"ProxyTests"})
public class ProxyTests extends SubscriptionManagerCLITestScript {


	// Test methods ***********************************************************************
	
	
	// REGISTER Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21837", "RHEL7-51656"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager : register using a proxy server (Positive and Negative Variations)",
			groups={"Tier1Tests"},
			dataProvider="getRegisterAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		String moduleTask = "register";
		
		SSHCommandResult attemptResult = clienttasks.register_(username, password, org, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21819", "RHEL7-51638"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : register using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getRegisterAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRegisterAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		String moduleTask = "register";

		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt to register
		SSHCommandResult attemptResult = clienttasks.register_(username, password, org, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from a negative attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern (BASIC AUTH)
		// 1292545301.350    418 10.16.120.247 TCP_MISS/200 1438 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.146 -
		// 1292551602.625      0 10.16.120.247 TCP_DENIED/407 3840 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 - NONE/- text/html

		// assert the tail of proxyLog shows the proxyLogGrepPattern (NO AUTH)
		// CONNECT   Dec 17 18:56:22 [20793]: Connect (file descriptor 7):  [10.16.120.248]
		// CONNECT   Dec 17 18:56:22 [20793]: Request (file descriptor 7): CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 HTTP/1.1
		// INFO      Dec 17 18:56:22 [20793]: No proxy for jsefler-f12-candlepin.usersys.redhat.com
		// CONNECT   Dec 17 18:56:22 [20793]: Established connection to host "jsefler-f12-candlepin.usersys.redhat.com" using file descriptor 8.
		// INFO      Dec 17 18:56:22 [20793]: Not sending client headers to remote machine
		// INFO      Dec 17 18:56:22 [20793]: Closed connection between local client (fd:7) and remote client (fd:8)
		
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	
	
	// UNREGISTER Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21837", "RHEL7-51656"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : unregister using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getUnregisterAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testUnregisterAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "unregister";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.unregister_(proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21836", "RHEL7-51655"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : unregister using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getUnregisterAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testUnregisterAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "unregister";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);

		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.unregister_(proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	
	// IDENTITY Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21811", "RHEL7-51628"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : identity using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getIdentityAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testIdentityAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "identity";
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.identity_(username, password, Boolean.TRUE, Boolean.TRUE, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21810", "RHEL7-51627"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : identity using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getIdentityAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testIdentityAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "identity";
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.identity_(username, password, Boolean.TRUE, Boolean.TRUE, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	
	// ORGS Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21815", "RHEL7-51632"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : orgs using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getOrgsAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testOrgsAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "orgs";
		
		SSHCommandResult attemptResult = clienttasks.orgs_(username, password, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21814", "RHEL7-51631"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : orgs using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getOrgsAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testOrgsAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "orgs";
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.orgs_(username, password, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	
	// SERVICE-LEVEL Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21832", "RHEL7-51651"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : service-level using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getServiceLevelAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testServiceLevelAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "service-level";
		
		SSHCommandResult attemptResult = clienttasks.service_level_(null, true, null, null, username, password, org, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21831", "RHEL7-51650"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : service-level using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getServiceLevelAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testServiceLevelAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "service-level";
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.service_level_(null, true, null, null, username, password, org, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	
	// ENVIRONMENTS Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21807", "RHEL7-51624"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : environments using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-728380","blockedByBug-1254578"/*is a duplicate of*/,"blockedByBug-1254349"},
			dataProvider="getEnvironmentsAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testEnvironmentsAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "environments";
		
		SSHCommandResult attemptResult = clienttasks.environments_(username, password, org, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21806", "RHEL7-51623"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : environments using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-728380","blockedByBug-1254578"/*is a duplicate of*/,"blockedByBug-1254349"},
			dataProvider="getEnvironmentsAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testEnvironmentsAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "environments";
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.environments_(username, password, org, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	
	// LIST Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21813", "RHEL7-51630"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : list using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getListAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testListAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "list";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.list_(null,Boolean.TRUE,null,null,null, null, null, null, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21823", "RHEL7-51642"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : list using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getListAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testListAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "list";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.list_(null,Boolean.TRUE,null,null,null, null, null, null, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	
	// RELEASE Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21822", "RHEL7-51641"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : release using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getReleaseAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testReleaseAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "release";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.release_(null, null, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21821", "RHEL7-51640"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : release using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getReleaseAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testReleaseAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "release";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.release_(null, null, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}



	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21824", "RHEL7-51643"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : release --list using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-822965","blockedByBug-824530"},
			dataProvider="getReleaseAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testReleaseListAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "release --list";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		// alter the expected feedback for negative tests when there is no subscribed RHEL product
		//if (!exitCode.equals(new Integer(255))) {
		if (exitCode.equals(new Integer(0))) {
			if (!clienttasks.isRhelProductCertSubscribed()) {
				log.warning("Altering the expected feedback from release --list when there is no RHEL product installed with status Subscribed");
				exitCode = new Integer(255);
				if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) exitCode = new Integer(78);	// EX_CONFIG	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
				stdout = "";
				stderr = "No release versions available, please check subscriptions.";
			}
		}			
		
		SSHCommandResult attemptResult = clienttasks.release_(null, true, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21823", "RHEL7-51642"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : release using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-822965","blockedByBug-824530"},
			dataProvider="getReleaseAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testReleaseListAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "release --list";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, null, false, null, null, null, null);
	
		// alter the expected feedback when there is no subscribed RHEL product
		//if (!exitCode.equals(new Integer(255))) {
		if (exitCode.equals(new Integer(0))) {
			if (!clienttasks.isRhelProductCertSubscribed()) {
				log.warning("Altering the expected feedback from release --list when there is no RHEL product installed with status Subscribed");
				exitCode = new Integer(255);
				if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) exitCode = new Integer(78);	// EX_CONFIG	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
				stdout = "";
				stderr = "No release versions available, please check subscriptions.";
			}
		}
		
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.release_(null, true, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	// AUTO-HEAL Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21805", "RHEL7-51622"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : auto-heal using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getAutoHealAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testAutoHealAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "autoheal";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.autoheal_(null, null, true, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21804", "RHEL7-51621"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : auto-heal using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getAutoHealAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testAutoHealAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "autoheal";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.autoheal_(null, null, true, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	// STATUS Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21834", "RHEL7-51653"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : status using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-977481"},
			dataProvider="getStatusAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testStatusAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "status";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null, null);
		
		// NOTE: Because the status module should return a cached status report when connectivity has been interrupted, this call should always pass
		SSHCommandResult attemptResult = clienttasks.status_(null, proxy, proxyuser, proxypassword, null);
		// NOTE: Due RFE Bug 1119688, the exit code will no longer indicate a PASS every time.
		if (exitCode.equals(new Integer(0))) {
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit 7957b8df95c575e6e8713c2f1a0f8f754e32aed3 bug 1119688
				// exit code of 0 indicates valid compliance, otherwise exit code is 1
				if (!clienttasks.getFactValue("system.entitlements_valid").equals("valid")) {
					log.warning("Altering the expected from status attempts using a proxy server to 1 due to RFE Bug 1119688 change in exit code when the system's overall status is NOT valid.");
					exitCode = new Integer(1);
				}
			}
		}
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21833", "RHEL7-51652"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : status using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-977481"},
			dataProvider="getStatusAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testStatusAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "status";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.status_(null, proxy, proxyuser, proxypassword, null);
		// NOTE: Due RFE Bug 1119688, the exit code will no longer indicate a PASS every time.
		if (exitCode.equals(new Integer(0))) {
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit 7957b8df95c575e6e8713c2f1a0f8f754e32aed3 bug 1119688
				// exit code of 0 indicates valid compliance, otherwise exit code is 1
				if (!clienttasks.getFactValue("system.entitlements_valid").equals("valid")) {
					log.warning("Altering the expected from status attempts using a proxy server to 1 due to RFE Bug 1119688 change in exit code when the system's overall status is NOT valid.");
					exitCode = new Integer(1);
				}
			}
		}
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	// VERSION Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-25993", "RHEL7-51660"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : version using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-977481","blockedByBug-1284120"},
			dataProvider="getVersionAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testVersionAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "version";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		
		// NOTE: Because the status module should return a cached status report when connectivity has been interrupted, this call should always pass
		SSHCommandResult attemptResult = clienttasks.version_(proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertTrue(attemptResult.getStdout().contains(stdout), "The stdout from an attempt to "+moduleTask+" using a proxy server contains expected report '"+stdout+"'.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-25992", "RHEL7-51659"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : status using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-977481","blockedByBug-1284120"},
			dataProvider="getVersionAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testVersionAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "version";
		
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.version_(proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertTrue(attemptResult.getStdout().contains(stdout), "The stdout from an attempt to "+moduleTask+" using a proxy server contains expected report '"+stdout+"'.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	// REDEEM Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-26755", "RHEL7-51633"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : redeem using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests","ProxyRedeemTests"},
			dataProvider="getRedeemAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRedeemAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "redeem";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.redeem_("proxytester@redhat.com","en-us",null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-26754", "RHEL7-55662"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : redeem using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests","ProxyRedeemTests"},
			dataProvider="getRedeemAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRedeemAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "redeem";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.redeem_("proxytester@redhat.com","en-us",null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	
	// FACTS Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21809", "RHEL7-51626"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : facts using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getFactsAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testFactsAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "facts";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.facts_(null,Boolean.TRUE,proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21808", "RHEL7-51625"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : facts using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getFactsAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testFactsAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "facts";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.facts_(null,Boolean.TRUE,proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	
	// REFRESH Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21817", "RHEL7-51635"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : refresh using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-664548"},
			dataProvider="getRefreshAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRefreshAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "refresh";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.refresh_(proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21816", "RHEL7-51634"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : refresh using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-664548"},
			dataProvider="getRefreshAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRefreshAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "refresh";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.refresh_(proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	
	// REPOS Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21828", "RHEL7-51647"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : repos using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-906642","blockedByBug-909778"},
			dataProvider="getReposAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testReposAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "repos";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribe(null,null,pool.poolId,null,null,null, null, null, null, null, null, null, null);
		
		// TEMPORARY WORKAROUND FOR BUG 1176219 - subscription-manager repos --list with bad proxy options is silently using cache
		String bugId = "1176219"; boolean invokeWorkaroundWhileBugIsOpen = true;
		if (nErrMsg.equals(stderr)) {
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Deleting cache '"+clienttasks.rhsmCacheDir+"', and will assert a slightly different stderr while bug '"+bugId+"' is open.");
				stderr = "Error updating system data on the server, see /var/log/rhsm/rhsm.log for more details.";
				client.runCommandAndWait("rm -rf "+clienttasks.rhsmCacheDir+"/*");
			}
		}
		// END OF WORKAROUND
		
		SSHCommandResult attemptResult = clienttasks.repos_(true,null,null,(List<String>)null, (List<String>)null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" --list using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" --list using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" --list using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21827", "RHEL7-51646"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : repos using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-906642","blockedByBug-909778"},
			dataProvider="getReposAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testReposAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "repos";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
//FIXME THIS COULD LEAD TO Stdout: This system has no repositories available through subscriptions.
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribe(null,null,pool.poolId,null,null,null, null, null, null, null, null, null, null);
//FIXME CHANGE TO GET A RANDOM SUB THAT MATCHES INSTALLED
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" ReposAttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);

		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// TEMPORARY WORKAROUND FOR BUG 1176219 - subscription-manager repos --list with bad proxy options is silently using cache
		String bugId = "1176219"; boolean invokeWorkaroundWhileBugIsOpen = true;
		if (nErrMsg.equals(stderr)) {
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Deleting cache '"+clienttasks.rhsmCacheDir+"', and will assert a slightly different stderr while bug '"+bugId+"' is open.");
				stderr = "Error updating system data on the server, see /var/log/rhsm/rhsm.log for more details.";
				client.runCommandAndWait("rm -rf "+clienttasks.rhsmCacheDir+"/*");
			}
		}
		// END OF WORKAROUND
		
		// test for use of cache (expected stderr=nErrMsg is indicative that cache will now by used when the proxy connection fails)
		boolean assertRhsmLogForCache = false;
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.18.2-1")) {	// not too sure about this version/commit // post commit ad982c13e79917e082f336255ecc42615e1e7707	1176219: Error out if bad proxy settings detected
			if (nErrMsg.equals(stderr)) {	// indicative that cache will be used
				// Bug 1176219 - subscription-manager repos --list with bad proxy options is silently using cache
				// rhsm.log will now report: [WARNING] subscription-manager:27964:MainThread @cache.py:235 - Unable to reach server, using cached status.
				assertRhsmLogForCache = true;
				
				// modify the expected results because cache will now be used
				log.info("Alterring the expected dataProvided parameters for expected exitCode, stdout, stderr to assert use of warnings in the rhsm.log instead the command line result '"+stderr+"'.");
				stderr = "";
				stdout = null;
				exitCode = new Integer(0);
				
				// mark the rhsm.log
				RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, proxyLogMarker);
			}
		}
		
		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.repos_(true,null,null,(List<String>)null, (List<String>)null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" --list using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" --list using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" --list using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
		
		// assert the rhsm.log for use of cache warnings
		if (assertRhsmLogForCache) {
			// 2017-02-02 14:50:20,480 [WARNING] rhsmd:943:MainThread @cache.py:235 - Unable to reach server, using cached status.	
			String rhsmLogResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, proxyLogMarker, "WARNING");
			String cacheMsg = "Unable to reach server, using cached status.";
			Assert.assertTrue(rhsmLogResult.contains(cacheMsg), "The tail of rhsm log '"+clienttasks.rhsmLogFile+"' following marker '"+proxyLogMarker+"' contains expected WARNING '"+cacheMsg+"'.");
		}
	}
	
	
	
	// REPO-OVERRIDE Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21826", "RHEL7-51645"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : repo-override --list using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getRepoOverrideAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRepoOverrideAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "repo-override";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
//		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
//		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
//		clienttasks.subscribe(null,null,pool.poolId,null,null,null, null, null, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.repo_override_(true,null,(List<String>)null,(List<String>)null,null,proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" --list using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" --list using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" --list using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21825", "RHEL7-51644"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : repo-override --list using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests"},
			dataProvider="getRepoOverrideAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRepoOverrideAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "repo-override";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
//		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
//		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
//		clienttasks.subscribe(null,null,pool.poolId,null,null,null, null, null, null, null, null);
		
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" ReposAttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.repo_override_(true,null,(List<String>)null,(List<String>)null,null,proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" --list using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" --list using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" --list using a proxy server.");
		
		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	
	// SUBSCRIBE Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21835", "RHEL7-51654"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : subscribe using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-664603"},
			dataProvider="getSubscribeAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testSubscribeAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "subscribe";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool

		SSHCommandResult attemptResult = clienttasks.subscribe_(null,null,pool.poolId,null,null,null, null, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21838", "RHEL7-51657"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : subscribe using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-664603"},
			dataProvider="getSubscribeAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testSubscribeAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "subscribe";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);

		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.subscribe_(null,null,pool.poolId,null,null,null, null, null, null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	
	// UNSUBSCRIBE Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21839", "RHEL7-51658"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : unsubscribe using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-664603"},
			dataProvider="getUnsubscribeAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testUnsubscribeAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "unsubscribe";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.unsubscribe_(true, (BigInteger)null,null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21838", "RHEL7-51657"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : subscribe using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-664603"},
			dataProvider="getUnsubscribeAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testUnsubscribeAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "unsubscribe";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.unsubscribe_(true, (BigInteger)null,null, proxy, proxyuser, proxypassword, null);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	
	// RHSM-DEBUG SYSTEM Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21830", "RHEL7-51649"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="rhsm-debug : system using a proxy server (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-1070737","blockedByBug-1039653","blockedByBug-1093382"},
			dataProvider="getRhsmDebugSystemAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRhsmDebugSystemAttemptsUsingProxyServer(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "rhsm-debug system";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null, null);
		
		SSHCommandResult attemptResult = client.runCommandAndWait(clienttasks.rhsmDebugSystemCommand(null, null, null, null, null, proxy, proxyuser, proxypassword, null));
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
		
		//	[root@jsefler-7 ~]# rhsm-debug system
		//	Wrote: /tmp/rhsm-debug-system-20150929-039530.tar.gz
		// delete the rhsm-debug-system file for the sake of saving space
		if (attemptResult.getStdout().trim().matches("Wrote: /tmp/rhsm-debug-system-\\d+-\\d+.tar.gz")) {
			File rhsmDebugSystemFile = new File(attemptResult.getStdout().split(":")[1].trim());	// Wrote: /tmp/rhsm-debug-system-20140115-457636.tar.gz
			client.runCommandAndWait("rm -f "+rhsmDebugSystemFile);
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21829", "RHEL7-51648"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="rhsm-debug : system using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-1070737","blockedByBug-1039653","blockedByBug-1093382"},
			dataProvider="getRhsmDebugSystemAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRhsmDebugSystemAttemptsUsingProxyServerViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "rhsm-debug system";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null, null);
		
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = client.runCommandAndWait(clienttasks.rhsmDebugSystemCommand(null, null, null, null, null, proxy, proxyuser, proxypassword, null));
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
		
		//	[root@jsefler-7 ~]# rhsm-debug system
		//	Wrote: /tmp/rhsm-debug-system-20150929-039530.tar.gz
		// delete the rhsm-debug-system file for the sake of saving space
		if (attemptResult.getStdout().trim().matches("Wrote: /tmp/rhsm-debug-system-\\d+-\\d+.tar.gz")) {
			File rhsmDebugSystemFile = new File(attemptResult.getStdout().split(":")[1].trim());	// Wrote: /tmp/rhsm-debug-system-20140115-457636.tar.gz
			client.runCommandAndWait("rm -f "+rhsmDebugSystemFile);
		}
		
		// assert the tail of proxyLog shows the proxyLogGrepPattern
		if (proxyLogGrepPattern!=null) {
			//SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			//SSHCommandResult proxyLogResult = proxyRunner.runCommandAndWait("(LINES=''; IFS=$'\n'; for line in $(tac "+proxyLog+"); do if [[ $line = '"+proxyLogMarker+"' ]]; then break; fi; LINES=${LINES}'\n'$line; done; echo -e $LINES) | grep "+clienttasks.ipaddr);	// accounts for multiple tests hitting the same proxy simultaneously
			//Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			//Assert.assertContainsMatch(proxyLogResult, proxyLogGrepPattern, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");	// TOO MUCH LOGGING
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}
	
	
	
	
	
	
	// More Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21820", "RHEL7-51639"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager :  register with proxy configurations commented out of rhsm.conf",
			groups={"Tier3Tests"},
			dataProvider="getRegisterWithProxyConfigurationsCommentedOutOfRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRegisterWithProxyConfigurationsCommentedOutOfRhsmConfig(Object meta, String[] proxyConfigs) {
		
		// comment out each of the config proxy parameters
		for (String proxyConfig : proxyConfigs) clienttasks.commentConfFileParameter(clienttasks.rhsmConfFile, proxyConfig);
		
		log.info("Following are the current proxy parameters configured in config file: "+clienttasks.rhsmConfFile);
		RemoteFileTasks.runCommandAndWait(client, "grep proxy_ "+clienttasks.rhsmConfFile, TestRecords.action());
		
		log.info("Attempt to register with the above proxy config parameters configured (expecting success)...");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-26756", "RHEL7-98223"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : repos list with proxy set to a real server that is not truely a proxy (e.g. www.redhat.com)",
			groups={"Tier3Tests","blockedByBug-968820","blockedByBug-1301215","blockedByBug-1345962"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testReposListWithProxyTimeoutBug968820() {
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);

		// subscribe
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) log.warning("Cound not find an available pool.");
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size()));	// randomly pick a pool
		//clienttasks.subscribeToSubscriptionPool(pool);
		clienttasks.subscribe(null,null,pool.poolId,null,null,null,null,null,null,null,null, null, null);
		
		// repos --list --proxy=www.redhat.com
		String command = clienttasks.command+" repos --list --proxy=www.redhat.com";
		Long timeoutMS = Long.valueOf(8/*min*/*60*1000);	// do not wait any longer than this many milliseconds
		SSHCommandResult result= client.runCommandAndWait(command, timeoutMS);
		
		
		// expected results
		Integer expectedExitCode = new Integer(255);
		String expectedStdout = nErrMsg;
		String expectedStderr = "";
		
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			 expectedExitCode = new Integer(70);	// EX_SOFTWARE 
		}
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.9-1")) {	// post commit a695ef2d1da882c5f851fde90a24f957b70a63ad
			//	201503301409:07.031 - FINE: ssh root@jsefler-os6.usersys.redhat.com subscription-manager repos --list --proxy=www.redhat.com
			//	201503301410:07.487 - FINE: Stdout: 
			//	201503301410:07.487 - FINE: Stderr: Network error, unable to connect to server. Please see /var/log/rhsm/rhsm.log for more information.
			//	201503301410:07.487 - FINE: ExitCode: 70
			//            ^^ one minute timeout observed (but I have also seen this take 4m16.286s)
			expectedExitCode = new Integer(70);	// EX_SOFTWARE
			expectedStdout = "";
			expectedStderr = nErrMsg;
		}
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.17.6-1")) {	// post commit 7ce6801fc1cc38edcdeb75dfb5f0d1f8a6398c68	1176219: Stop before cache is returned when using bad proxy options	// Bug 1301215 - The cmd "repos --list --proxy" with a fake proxy server url will not stop running.
			//	201612121142:43.748 - FINE: ssh root@jsefler-rhel6.usersys.redhat.com subscription-manager repos --list --proxy=www.redhat.com
			//	201612121142:54.320 - FINE: Stdout: 
			//	201612121142:54.321 - FINE: Stderr: Proxy connection failed, please check your settings.
			//	201612121142:54.321 - FINE: ExitCode: 69
			expectedExitCode = new Integer(69);	// EX_UNAVAILABLE
			expectedStdout = "";
			expectedStderr = pErrMsg;
		} 
		
		// assert results
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "ExitCode from command '"+command+"' with a timeout of '"+timeoutMS+"' MS.");	
		Assert.assertEquals(result.getStdout().trim(), expectedStdout, "Stdout from command '"+command+"' with a timeout of '"+timeoutMS+"' MS.");
		Assert.assertEquals(result.getStderr().trim(), expectedStderr, "Stderr from command '"+command+"' with a timeout of '"+timeoutMS+"' MS.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21818", "RHEL7-51637"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : register using a proxy server defined by an environment variable (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-1031755"},
			dataProvider="getRegisterAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRegisterAttemptsUsingProxyServerDefinedByAnEnvironmentVariable(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		String moduleTask = "register";
		String httpProxyEnvVar;
		String proxyLogMarker;
		SSHCommandResult attemptResult;
		
		// Notes: proxy precedence rules as defined in Sprint Review 68
		// a. proxy values set in the subscription-manager CLI options override all
		// b. proxy values set in the rhsm.conf file override the proxy environment variable
		// c. finally the proxy environment variable applies when no proxy values are set in the CLI options nor rhsm.conf
		
		// randomly test using one of these valid environment variables 
		List<String> validHttpProxyEnvVars = Arrays.asList(new String[]{"HTTPS_PROXY","https_proxy","HTTP_PROXY","http_proxy"});
		
		
		// TEST PART 1: let's assert that setting the proxy via an environment variable works
		//-------------
		if (exitCode!=Integer.valueOf(69)/* EX_UNAVAILABLE is indicative of a bad proxy */) {	// skipping Test 1 for negative bad proxy
		// assemble the value of the httpProxyVar
		// HTTPS_PROXY=https://proxyserver
		// HTTPS_PROXY=https://proxyserver:proxyport
		// HTTPS_PROXY=https://username:password@proxyserver:proxyport
		// proxy configurations intended for CLI proxy options take precedence over the proxy configurations intended for rhsm.conf file
		httpProxyEnvVar = validHttpProxyEnvVars.get(randomGenerator.nextInt(validHttpProxyEnvVars.size())) + "=https://";
		if (proxyuser!=null)							httpProxyEnvVar += proxyuser;			else if (proxy_userConfig!=null)		httpProxyEnvVar += proxy_userConfig;
		if (proxypassword!=null)						httpProxyEnvVar += ":"+proxypassword;	else if (proxy_passwordConfig!=null)	httpProxyEnvVar += ":"+proxy_passwordConfig;
		if (proxyuser!=null || proxy_userConfig!=null)	httpProxyEnvVar += "@";
		if (proxy!=null)								httpProxyEnvVar += proxy;				else {if (proxy_hostnameConfig!=null)	httpProxyEnvVar += proxy_hostnameConfig; if (proxy_portConfig!=null)	httpProxyEnvVar += ":"+proxy_portConfig;}
		
		// reset the config parameters
		updateConfFileProxyParameters("", "", "", "", "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// pad the tail of proxyLog with a message
		proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerDefinedByAnEnvironmentVariable_Test from "+clienttasks.hostname+"...";
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// attempt to register using a proxy server defined by an environment variable (no CLI option nor rhsm.conf [sever] proxy configurations set)
		attemptResult = client.runCommandAndWait(httpProxyEnvVar+" "+clienttasks.registerCommand(username, password, org, null, null, null, null, null, null, null, (List<String>)null, null, null, null, null, null, null, null, null, null));
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server defined by an environment variable '"+httpProxyEnvVar+"'.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server defined by an environment variable '"+httpProxyEnvVar+"'.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server defined by an environment variable '"+httpProxyEnvVar+"'.");
		
		// assert the tail of proxyLog shows the proxyLogGrepPattern (BASIC AUTH)
		// 1292545301.350    418 10.16.120.247 TCP_MISS/200 1438 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.146 -
		// 1292551602.625      0 10.16.120.247 TCP_DENIED/407 3840 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 - NONE/- text/html
		
		// assert the tail of proxyLog shows the proxyLogGrepPattern (NO AUTH)
		// CONNECT   Dec 17 18:56:22 [20793]: Connect (file descriptor 7):  [10.16.120.248]
		// CONNECT   Dec 17 18:56:22 [20793]: Request (file descriptor 7): CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 HTTP/1.1
		// INFO      Dec 17 18:56:22 [20793]: No proxy for jsefler-f12-candlepin.usersys.redhat.com
		// CONNECT   Dec 17 18:56:22 [20793]: Established connection to host "jsefler-f12-candlepin.usersys.redhat.com" using file descriptor 8.
		// INFO      Dec 17 18:56:22 [20793]: Not sending client headers to remote machine
		// INFO      Dec 17 18:56:22 [20793]: Closed connection between local client (fd:7) and remote client (fd:8)
		
		if (proxyLogGrepPattern!=null) {
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
		
		// cleanup
		clienttasks.unregister_(null, null, null, null);
		}	// skipped Test 1 for negative bad proxy
		
		
		// TEST PART 2: now let's assert that setting the proxy via CLI option or rhsm.conf take precedence over the environment variable
		//-------------
		// assemble the value of a httpProxyVar that will get overridden
		httpProxyEnvVar = validHttpProxyEnvVars.get(randomGenerator.nextInt(validHttpProxyEnvVars.size())) + "=https://";
		httpProxyEnvVar += "proxy.example.com:911";	// provided in https://bugzilla.redhat.com/show_bug.cgi?id=1031755#c0	Note: this does not have to be a working proxy to make this a valid test
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// pad the tail of proxyLog with a message
		proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerDefinedByAnEnvironmentVariable_Test from "+clienttasks.hostname+"...";
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// attempt to register using a proxy server defined by an environment variable (no CLI option nor rhsm.conf [sever] proxy configurations set)
		attemptResult = client.runCommandAndWait(httpProxyEnvVar+" "+clienttasks.registerCommand(username, password, org, null, null, null, null, null, null, null, (List<String>)null, null, null, null, null, null, proxy, proxyuser, proxypassword, null));
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server to override environment variable '"+httpProxyEnvVar+"'.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server to override environment variable '"+httpProxyEnvVar+"'.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server to override environment variable '"+httpProxyEnvVar+"'.");
		
		// assert the tail of proxyLog shows the proxyLogGrepPattern (BASIC AUTH)
		// 1292545301.350    418 10.16.120.247 TCP_MISS/200 1438 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.146 -
		// 1292551602.625      0 10.16.120.247 TCP_DENIED/407 3840 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 - NONE/- text/html
		
		// assert the tail of proxyLog shows the proxyLogGrepPattern (NO AUTH)
		// CONNECT   Dec 17 18:56:22 [20793]: Connect (file descriptor 7):  [10.16.120.248]
		// CONNECT   Dec 17 18:56:22 [20793]: Request (file descriptor 7): CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 HTTP/1.1
		// INFO      Dec 17 18:56:22 [20793]: No proxy for jsefler-f12-candlepin.usersys.redhat.com
		// CONNECT   Dec 17 18:56:22 [20793]: Established connection to host "jsefler-f12-candlepin.usersys.redhat.com" using file descriptor 8.
		// INFO      Dec 17 18:56:22 [20793]: Not sending client headers to remote machine
		// INFO      Dec 17 18:56:22 [20793]: Closed connection between local client (fd:7) and remote client (fd:8)
		
		if (proxyLogGrepPattern!=null) {
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
		}
	}





	@SuppressWarnings("unused")
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-22311", "RHEL7-51636"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : register when no_proxy environment variable matches our hostname regardless of proxy configurations and environment variable (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-1266608","blockedByBug-1285010"},
			dataProvider="getRegisterAttemptsToVerifyHonoringNoProxyEnvironmentVariableData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRegisterAttemptsToVerifyHonoringNoProxyEnvironmentVariable(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern, String noProxyEnvVar, Boolean hostnameMatchesNoProxyEnvVar) {
		if (clienttasks.isPackageVersion("python-rhsm", "<", "1.15.1-1")) throw new SkipException("Support for this test does not exist in this version of python-rhsm.  See bugzilla 1266608.");
		
		String moduleTask = "register";
		String httpProxyEnvVar;
		String proxyLogMarker;
		SSHCommandResult attemptResult;		
		
		// Notes: from http://www.gnu.org/software/emacs/manual/html_node/url/Proxies.html
		//	The NO_PROXY environment variable specifies URLs that should be excluded from proxying
		//	(on servers that should be contacted directly). This should be a comma-separated list
		//	of hostnames, domain names, or a mixture of both. Asterisks can be used as wildcards,
		//	but other clients may not support that. Domain names may be indicated by a leading dot.
		//	For example:
		//
		//	NO_PROXY="*.aventail.com,home.com,.seanet.com"
		//
		//	says to contact all machines in the ‘aventail.com’ and ‘seanet.com’ domains directly,
		//	as well as the machine named ‘home.com’. If NO_PROXY isn’t defined, no_PROXY and no_proxy
		//	are also tried, in that order. 
		
		// Notes: from man page for curl
		//	ENVIRONMENT
		//       The environment variables can be specified in lower case or upper case. The lower case version has precedence. http_proxy is an exception as  it
		//       is only available in lower case.
		//
		//       Using an environment variable to set the proxy has the same effect as using the --proxy option.
		//
		//       http_proxy [protocol://]<host>[:port]
		//              Sets the proxy server to use for HTTP.
		//
		//       HTTPS_PROXY [protocol://]<host>[:port]
		//              Sets the proxy server to use for HTTPS.
		//
		//       [url-protocol]_PROXY [protocol://]<host>[:port]
		//              Sets  the  proxy  server  to  use for [url-protocol], where the protocol is a protocol that curl supports and as specified in a URL. FTP,
		//              FTPS, POP3, IMAP, SMTP, LDAP etc.
		//
		//       ALL_PROXY [protocol://]<host>[:port]
		//              Sets the proxy server to use if no protocol-specific proxy is set.
		//
		//       NO_PROXY <comma-separated list of hosts>
		//              list of host names that shouldn't go through any proxy. If set to a asterisk '*' only, it matches all hosts.
		
		// WARNING: The two docs above do not agree on no_Proxy and the use of * 
		
		// Notes: proxy precedence rules as defined in Sprint Review 68
		// a. proxy values set in the subscription-manager CLI options override all
		// b. proxy values set in the rhsm.conf file override the proxy environment variable
		// c. finally the proxy environment variable applies when no proxy values are set in the CLI options nor rhsm.conf
		// d. if hostname matches no_proxy environment variable, then override c.
		
		// Notes Update: 2/8/2017 The precedence rules for no_proxy defined above was flawed and was corrected by
		// Bug 1311429 - no_proxy variable ignored when configured in virt-who config file
		// d. if hostname matches no_proxy environment variable, then override all.  DO NOT USE A PROXY.
		
		// randomly test using one of these valid environment variables 
		List<String> validHttpProxyEnvVars = Arrays.asList(new String[]{"HTTPS_PROXY","https_proxy","HTTP_PROXY","http_proxy"});
		List<String> validHttpNoProxyEnvVars = Arrays.asList(new String[]{"NO_PROXY","no_proxy"/*,"no_PROXY" not supported by curl*/});
		
		// TEST PART 1:
		//-------------
		//		Assert that setting the proxy via environment variables (rhsm.conf and CLI options are void of proxy values)...
		//           A: is ignored when hostname matches no_proxy
		//           B: is NOT ignored when hostname does not match no_proxy
		
		// assemble the value of the httpProxyVar
		// HTTPS_PROXY=https://proxyserver
		// HTTPS_PROXY=https://proxyserver:proxyport
		// HTTPS_PROXY=https://username:password@proxyserver:proxyport
		// proxy configurations intended for CLI proxy options take precedence over the proxy configurations intended for rhsm.conf file
		httpProxyEnvVar = validHttpProxyEnvVars.get(randomGenerator.nextInt(validHttpProxyEnvVars.size())) + "=https://";
		if (proxyuser!=null)							httpProxyEnvVar += proxyuser;			else if (proxy_userConfig!=null)		httpProxyEnvVar += proxy_userConfig;
		if (proxypassword!=null)						httpProxyEnvVar += ":"+proxypassword;	else if (proxy_passwordConfig!=null)	httpProxyEnvVar += ":"+proxy_passwordConfig;
		if (proxyuser!=null || proxy_userConfig!=null)	httpProxyEnvVar += "@";
		if (proxy!=null)								httpProxyEnvVar += proxy;				else {if (proxy_hostnameConfig!=null)	httpProxyEnvVar += proxy_hostnameConfig; if (proxy_portConfig!=null)	httpProxyEnvVar += ":"+proxy_portConfig;}
		
		// assemble the value of noProxyEnvVar
		noProxyEnvVar = validHttpNoProxyEnvVars.get(randomGenerator.nextInt(validHttpNoProxyEnvVars.size())) + "=" +noProxyEnvVar;
		
		// reset the config parameters
		updateConfFileProxyParameters("", "", "", "", "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// pad the tail of proxyLog with a message
		proxyLogMarker = System.currentTimeMillis()+" Testing 1 "+moduleTask+" AttemptsToVerifyHonoringNoProxyEnvironmentVariable_Test from "+clienttasks.hostname+"...";
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// attempt to register using a non-matching no_proxy environment variable and a proxy server defined by an environment variable (no CLI option nor rhsm.conf [sever] proxy configurations set)
		attemptResult = client.runCommandAndWait(noProxyEnvVar+" "+httpProxyEnvVar+" "+clienttasks.registerCommand(username, password, org, null, null, null, null, null, null, null, (List<String>)null, null, null, null, true, null, null, null, null, null));
		if (hostnameMatchesNoProxyEnvVar) {	// A: is ignored when hostname matches no_proxy
			Assert.assertEquals(attemptResult.getExitCode(), Integer.valueOf(0), "The exit code from an attempt to "+moduleTask+" using a matching no_proxy environment variable '"+noProxyEnvVar+"'.");
			Assert.assertEquals(attemptResult.getStderr().trim(), "", "The stderr from an attempt to "+moduleTask+" using a matching no_proxy environment variable '"+noProxyEnvVar+"'.");
		
			if (proxyLogGrepPattern!=null) {
				String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
				Assert.assertTrue(proxyLogResult.isEmpty(), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains NO connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
			}
		} else if (exitCode!=Integer.valueOf(69)/* EX_UNAVAILABLE is indicative of a bad proxy; skip Test 1B for negative bad proxy */) {	// B: is NOT ignored when hostname does not match no_proxy
			if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server defined by an environment variable '"+httpProxyEnvVar+"' that does not match the no_proxy environment variable '"+noProxyEnvVar+"'.");
			if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server defined by an environment variable '"+httpProxyEnvVar+"' that does not match the no_proxy environment variable '"+noProxyEnvVar+"'.");
			if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server defined by an environment variable '"+httpProxyEnvVar+"' that does not match the no_proxy environment variable '"+noProxyEnvVar+"'.");
			
			// assert the tail of proxyLog shows the proxyLogGrepPattern (BASIC AUTH)
			// 1292545301.350    418 10.16.120.247 TCP_MISS/200 1438 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.146 -
			// 1292551602.625      0 10.16.120.247 TCP_DENIED/407 3840 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 - NONE/- text/html
			
			// assert the tail of proxyLog shows the proxyLogGrepPattern (NO AUTH)
			// CONNECT   Dec 17 18:56:22 [20793]: Connect (file descriptor 7):  [10.16.120.248]
			// CONNECT   Dec 17 18:56:22 [20793]: Request (file descriptor 7): CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 HTTP/1.1
			// INFO      Dec 17 18:56:22 [20793]: No proxy for jsefler-f12-candlepin.usersys.redhat.com
			// CONNECT   Dec 17 18:56:22 [20793]: Established connection to host "jsefler-f12-candlepin.usersys.redhat.com" using file descriptor 8.
			// INFO      Dec 17 18:56:22 [20793]: Not sending client headers to remote machine
			// INFO      Dec 17 18:56:22 [20793]: Closed connection between local client (fd:7) and remote client (fd:8)
			
			if (proxyLogGrepPattern!=null) {
				String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
				Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
			}
		}
			
		// TEST PART 2
		//-------------
		if (clienttasks.isPackageVersion("python-rhsm", "<", "1.18.3-1")) { // pre commit 7b1294aa6bceb6734caa2493c54402537f0773a7 for Bug 1311429 - no_proxy variable ignored when configured in virt-who config file
		
			// TEST PART 2: now let's assert that setting the proxy via CLI option or rhsm.conf take precedence over the environment variable...
			//           A: regardless if hostname matches no_proxy
			//           B: regardless if hostname does not matches no_proxy

			// assemble the value of a httpProxyVar that will get overridden
			httpProxyEnvVar = validHttpProxyEnvVars.get(randomGenerator.nextInt(validHttpProxyEnvVars.size())) + "=https://";
			httpProxyEnvVar += "proxy.example.com:911";	// provided in https://bugzilla.redhat.com/show_bug.cgi?id=1031755#c0	Note: this does not have to be a working proxy to make this a valid test
			
			// set the config parameters
			updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
			RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
			
			// pad the tail of proxyLog with a message
			proxyLogMarker = System.currentTimeMillis()+" Testing 2 "+moduleTask+" AttemptsToVerifyHonoringNoProxyEnvironmentVariable_Test from "+clienttasks.hostname+"...";
			RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
			
			// attempt to register using a proxy server defined by an environment variable (no CLI option nor rhsm.conf [sever] proxy configurations set)
			attemptResult = client.runCommandAndWait(noProxyEnvVar+" "+httpProxyEnvVar+" "+clienttasks.registerCommand(username, password, org, null, null, null, null, null, null, null, (List<String>)null, null, null, null, true, null, proxy, proxyuser, proxypassword, null));
			if (hostnameMatchesNoProxyEnvVar || !hostnameMatchesNoProxyEnvVar) {	// A: regardless if hostname matches no_proxy	// B: regardless if hostname does not matches no_proxy
				if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server to override environment variable '"+httpProxyEnvVar+"'.");
				if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server to override environment variable '"+httpProxyEnvVar+"'.");
				if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server to override environment variable '"+httpProxyEnvVar+"'.");
				
				// assert the tail of proxyLog shows the proxyLogGrepPattern (BASIC AUTH)
				// 1292545301.350    418 10.16.120.247 TCP_MISS/200 1438 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.146 -
				// 1292551602.625      0 10.16.120.247 TCP_DENIED/407 3840 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 - NONE/- text/html
				
				// assert the tail of proxyLog shows the proxyLogGrepPattern (NO AUTH)
				// CONNECT   Dec 17 18:56:22 [20793]: Connect (file descriptor 7):  [10.16.120.248]
				// CONNECT   Dec 17 18:56:22 [20793]: Request (file descriptor 7): CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 HTTP/1.1
				// INFO      Dec 17 18:56:22 [20793]: No proxy for jsefler-f12-candlepin.usersys.redhat.com
				// CONNECT   Dec 17 18:56:22 [20793]: Established connection to host "jsefler-f12-candlepin.usersys.redhat.com" using file descriptor 8.
				// INFO      Dec 17 18:56:22 [20793]: Not sending client headers to remote machine
				// INFO      Dec 17 18:56:22 [20793]: Closed connection between local client (fd:7) and remote client (fd:8)
				
				if (proxyLogGrepPattern!=null) {
					String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
					Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
				}

			} else {
				Assert.fail("This line of code should be logically unreachable.");
			}
		
		} else { // post commit 7b1294aa6bceb6734caa2493c54402537f0773a7 for Bug 1311429 - no_proxy variable ignored when configured in virt-who config file
			
			// TEST PART 2:
			//		Assert that setting the no_proxy environment variable is honored when it matches...
			//           A: the proxy hostname specified within the rhsm.conf or the proxy hostname specified on the command line

			// do not assemble an environment variable for httpProxyVar - that was covered in Test 1
			httpProxyEnvVar = "";
			
			// set the config parameters
			updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, "");
			RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
			
			// pad the tail of proxyLog with a message
			proxyLogMarker = System.currentTimeMillis()+" Testing 2 "+moduleTask+" AttemptsToVerifyHonoringNoProxyEnvironmentVariable_Test from "+clienttasks.hostname+"...";
			RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
			
			// attempt to register while a no_proxy environment variable that has been defined with a list of proxy servers to ignore
			attemptResult = client.runCommandAndWait(noProxyEnvVar+" "+httpProxyEnvVar+" "+clienttasks.registerCommand(username, password, org, null, null, null, null, null, null, null, (List<String>)null, null, null, null, true, null, proxy, proxyuser, proxypassword, null));
			if (exitCode==Integer.valueOf(69)) {	// EX_UNAVAILABLE	// indicative of a bad proxy
				// when the proxy is unavailable, subscription-manager now aborts before making any decisions about no_proxy environment variables... Bug 1176219: Error out if bad proxy settings detected
				if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" with an unavailable proxy should abort in honor of bug 1176219 regardless of a no_proxy environment variable setting.");
				if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" with an unavailable proxy should abort in honor of bug 1176219 regardless of a no_proxy environment variable setting.");
				if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" with an unavailable proxy should abort in honor of bug 1176219 regardless of a no_proxy environment variable setting.");
			} else
			if (hostnameMatchesNoProxyEnvVar) {	// no_proxy environment variable matches proxy
				Assert.assertEquals(attemptResult.getExitCode(), Integer.valueOf(0), "The exit code from an attempt to "+moduleTask+" using a no_proxy environment variable '"+noProxyEnvVar+"' that matches the hostname to override both the configured and CLI option proxy.");
				Assert.assertEquals(attemptResult.getStderr().trim(), "", "The stderr from an attempt to "+moduleTask+" using a no_proxy environment variable '"+noProxyEnvVar+"' that matches the hostname to override both the configured and CLI option proxy.");
				Assert.assertNotNull(clienttasks.getCurrentConsumerCert(), "The system has succesfully registered a consumer.");
				
				// assert that no traffic has gone through the proxy logs
				String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
				// TEMPORARY WORKAROUND FOR BUG 1420533 - no_proxy environment variable is ignored by the rhsmd process
				String bugId = "1420533"; boolean invokeWorkaroundWhileBugIsOpen = true;
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Skipping assertion that tail of proxy server log '"+proxyLog+"' contains no attempts from "+ipv4_address+" to the candlepin server while bug '"+bugId+"' is open.");
				} else
				// END OF WORKAROUND
				// PERAMANENT WORKAROUND
				if (true) {
					log.warning("Permanently skipping assertion that tail of proxy server log '"+proxyLog+"' contains no attempts from "+ipv4_address+" to the candlepin server when the NO_PROXY environment variable matches the server.hostname because the solution for bug '"+bugId+"' was to introduce a new server.no_proxy configuration that could be read by the rhsmd process which runs outside the shell that the NO_PROXY environment variable was defined.");
				} else
				// END OF WORKAROUND
				Assert.assertTrue(proxyLogResult.isEmpty(), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains no attempts from "+ipv4_address+" to the candlepin server.");


			} else {	// the proxy does NOT match no_proxy and the no_proxy environment variable should have no effect.  Assert expected results from dataProvider.
				if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" when no_proxy environment variable '"+noProxyEnvVar+"' does not match the hostname.");
				if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" when no_proxy environment variable '"+noProxyEnvVar+"' does not match the hostname.");
				if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" when no_proxy environment variable '"+noProxyEnvVar+"' does not match the hostname.");
				
				// assert the tail of proxyLog shows the proxyLogGrepPattern (BASIC AUTH)
				// 1292545301.350    418 10.16.120.247 TCP_MISS/200 1438 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.146 -
				// 1292551602.625      0 10.16.120.247 TCP_DENIED/407 3840 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 - NONE/- text/html
				
				// assert the tail of proxyLog shows the proxyLogGrepPattern (NO AUTH)
				// CONNECT   Dec 17 18:56:22 [20793]: Connect (file descriptor 7):  [10.16.120.248]
				// CONNECT   Dec 17 18:56:22 [20793]: Request (file descriptor 7): CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 HTTP/1.1
				// INFO      Dec 17 18:56:22 [20793]: No proxy for jsefler-f12-candlepin.usersys.redhat.com
				// CONNECT   Dec 17 18:56:22 [20793]: Established connection to host "jsefler-f12-candlepin.usersys.redhat.com" using file descriptor 8.
				// INFO      Dec 17 18:56:22 [20793]: Not sending client headers to remote machine
				// INFO      Dec 17 18:56:22 [20793]: Closed connection between local client (fd:7) and remote client (fd:8)
				
				if (proxyLogGrepPattern!=null) {
					String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
					Assert.assertTrue(proxyLogResult.contains(proxyLogGrepPattern), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains expected connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
				}
			}
		}
	}
	@DataProvider(name="getRegisterAttemptsToVerifyHonoringNoProxyEnvironmentVariableData")
	public Object[][] getRegisterAttemptsToVerifyHonoringNoProxyEnvironmentVariableDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRegisterAttemptsToVerifyHonoringNoProxyEnvironmentVariableDataAsListOfLists());
	}
	protected List<List<Object>> getRegisterAttemptsToVerifyHonoringNoProxyEnvironmentVariableDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		// TOO EXHAUSTIVE TAKES 40 MINUTES for (List<Object> l : getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
		for (List<Object> l : getRandomSubsetOfList(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists(),3)) {
			
			// append a value for no_proxy environment variable and a boolean to indicate if it should be honored or not
			String noProxyEnvVar;
			Boolean hostnameMatchesNoProxyEnvVar;
			
			// get the existing BlockedByBzBug
			BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			// add more BlockedByBzBug to rows that are expecting a network error
			//bugIds.add("1234");	// Bug 1234

			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			
			bugIds.add("1457197");	// Bug 1457197 - --noproxy option no longer match "*" for host names
			noProxyEnvVar = "*"; hostnameMatchesNoProxyEnvVar=true;	// * matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(bugIds.toArray(new String[]{})),	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxyEnvVar, hostnameMatchesNoProxyEnvVar}));
			bugIds.remove("1457197");
			
			noProxyEnvVar = sm_serverHostname; hostnameMatchesNoProxyEnvVar=true;	// subscription.rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxyEnvVar, hostnameMatchesNoProxyEnvVar}));

			noProxyEnvVar = "*.aventail.com,home.com,.seanet.com,"+sm_serverHostname; hostnameMatchesNoProxyEnvVar=true;	// *.aventail.com,home.com,.seanet.com,subscription.rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxyEnvVar, hostnameMatchesNoProxyEnvVar}));

			noProxyEnvVar = sm_serverHostname.replaceFirst("[^\\.]+", ""); hostnameMatchesNoProxyEnvVar=true;	// .rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxyEnvVar, hostnameMatchesNoProxyEnvVar}));
			
			noProxyEnvVar = "*.aventail.com,home.com,.seanet.com,"+sm_serverHostname.replaceFirst("[^\\.]+", ""); hostnameMatchesNoProxyEnvVar=true;	// *.aventail.com,home.com,.seanet.com,.rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxyEnvVar, hostnameMatchesNoProxyEnvVar}));

			noProxyEnvVar = "*.aventail.com,home.com,.seanet.com"; hostnameMatchesNoProxyEnvVar=false;	// *.aventail.com,home.com,.seanet.com does not match subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxyEnvVar, hostnameMatchesNoProxyEnvVar}));
			
			bugIds.add("1443164");	// Bug 1443164 	no_proxy does not match the host name when *.redhat.com is used	// This case does not work on RHEL since the the only wildcard supported by the library is no_proxy=* which is likely an RFE bug against component X?
			noProxyEnvVar = sm_serverHostname.replaceFirst("[^\\.]+", "*"); hostnameMatchesNoProxyEnvVar=true;	// *.rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(bugIds.toArray(new String[]{})),	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxyEnvVar, hostnameMatchesNoProxyEnvVar}));
			bugIds.remove("1443164");
		}
		return ll;
	}
	
	
	
	
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47906", "RHEL7-96744"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28585",	// RHSM-REQ : Proxy settings
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84941",	// RHSM-REQ : Proxy settings
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.MEDIUM, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : register with noproxy specified via a command line option matching server.hostname should ignore proxy configurations and NOT send traffic through the configured proxy. (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-1420533"/*inspired the implementation of no_proxy configuration in rhsm.conf*/},
			dataProvider="getRegisterAttemptsToVerifyHonoringNoProxyData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRegisterAttemptsToVerifyHonoringNoProxyViaCmdLineOpt(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern, String noProxyOption, Boolean hostnameMatchesNoProxyOption) {
		if (clienttasks.isPackageVersion("python-rhsm", "<", "1.19.4-1")) throw new SkipException("Support for this test does not exist in this version of python-rhsm.  See bugzilla 1420533.");
		
		String moduleTask = "register";
		String proxyLogMarker;
		String no_proxyConfig = "does.not.match.server.hostname";
		SSHCommandResult attemptResult;
		
		//Assert that passing the --no_proxy command line arg is honored when it matches server.hostname
		// regardless if the proxy hostname is specified via rhsm.conf or if the proxy hostname is specified via command line arguments
		
		// set the config parameters
		if (!hostnameMatchesNoProxyOption) no_proxyConfig = sm_serverHostname;	// despite this matching no_proxyConfig, the noProxyOption should take precedence
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, no_proxyConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());	// just to log the proxy configs
		
		// pad the tail of proxyLog with a message
		proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+"AttemptsToVerifyHonoringNoProxyViaCmdLineOpt_Test from "+clienttasks.hostname+"...";
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// attempt to register with a no_proxy configuration declaring a list of proxy servers to ignore
		attemptResult = clienttasks.register_(username, password, org, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, proxy, proxyuser, proxypassword, noProxyOption);
		if (exitCode==Integer.valueOf(69)) {	// EX_UNAVAILABLE	// indicative of a bad proxy
			// when the proxy is unavailable, subscription-manager now aborts before making any decisions about no_proxy environment variables... Bug 1176219: Error out if bad proxy settings detected
			if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" with an unavailable proxy should abort in honor of bug 1176219 regardless of a no_proxy configuration setting.");
			if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" with an unavailable proxy should abort in honor of bug 1176219 regardless of a no_proxy configuration setting.");
			if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" with an unavailable proxy should abort in honor of bug 1176219 regardless of a no_proxy configuration setting.");
		} else
		if (hostnameMatchesNoProxyOption) {
			Assert.assertEquals(attemptResult.getExitCode(), Integer.valueOf(0), "The exit code from an attempt to "+moduleTask+" using a --noproxy option '"+noProxyOption+"' that matches the hostname '"+sm_serverHostname+"' to override both the configured and CLI option proxy.");
			Assert.assertEquals(attemptResult.getStderr().trim(), "", "The stderr from an attempt to "+moduleTask+" using a --noproxy option '"+noProxyOption+"' that matches the server hostname '"+sm_serverHostname+"' to override both the configured and CLI option proxy.");
			Assert.assertNotNull(clienttasks.getCurrentConsumerCert(), "The system has succesfully registered a consumer.");
			
			// assert that no traffic has gone through the proxy logs
			// however... when a valid proxy is specified, there WILL be one connection from commit 7ce6801fc1cc38edcdeb75dfb5f0d1f8a6398c68 bug 1301215 that is testing the validity of the proxy, therefore we must tolerate at most one connection.
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			List <String> proxyLogResultHits = Arrays.asList(proxyLogResult.trim().split("\n"));
			Assert.assertTrue(proxyLogResultHits.size()<=1, "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains zero connections (or at most one for the sake of Bug 1301215: Test proxy connection before making call) from "+ipv4_address+" to candlepin server '"+sm_serverHostname+"'.  (Actual proxy connections was '"+proxyLogResultHits.size()+"').");

		} else {	// the server.hostname does NOT match no_proxy and the no_proxy environment variable should have no effect.  Assert expected results from dataProvider.
			if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" when --noproxy option '"+noProxyOption+"' does not match the server hostname '"+sm_serverHostname+"'.");
			if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" when --noproxy option '"+noProxyOption+"' does not match the server hostname '"+sm_serverHostname+"'.");
			if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" when --noproxy option '"+noProxyOption+"' does not match the server hostname '"+sm_serverHostname+"'.");
			
			// assert the tail of proxyLog shows the proxyLogGrepPattern (BASIC AUTH)
			// 1292545301.350    418 10.16.120.247 TCP_MISS/200 1438 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.146 -
			// 1292551602.625      0 10.16.120.247 TCP_DENIED/407 3840 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 - NONE/- text/html
			
			// assert the tail of proxyLog shows the proxyLogGrepPattern (NO AUTH)
			// CONNECT   Dec 17 18:56:22 [20793]: Connect (file descriptor 7):  [10.16.120.248]
			// CONNECT   Dec 17 18:56:22 [20793]: Request (file descriptor 7): CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 HTTP/1.1
			// INFO      Dec 17 18:56:22 [20793]: No proxy for jsefler-f12-candlepin.usersys.redhat.com
			// CONNECT   Dec 17 18:56:22 [20793]: Established connection to host "jsefler-f12-candlepin.usersys.redhat.com" using file descriptor 8.
			// INFO      Dec 17 18:56:22 [20793]: Not sending client headers to remote machine
			// INFO      Dec 17 18:56:22 [20793]: Closed connection between local client (fd:7) and remote client (fd:8)
			
			if (proxyLogGrepPattern!=null) {
				String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
				List <String> proxyLogResultHits = Arrays.asList(proxyLogResult.trim().split("\n"));
				if (proxyLogResult.contains("TCP_DENIED")) {
					Assert.assertTrue(proxyLogResultHits.size()==1, "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains exactly one failed connection attempt matching '"+proxyLogGrepPattern+"' from "+ipv4_address+" to candlepin server '"+sm_serverHostname+"'.  Because the connection attempt was expected to fail (likely due to invalid proxy credentials), no additional proxy traffic will be attempted (for the sake of Bug 1301215: Test proxy connection before making call) regardless if hostname matches the no_proxy list.  (Actual proxy connections was '"+proxyLogResultHits.size()+"').");
				} else {			
					Assert.assertTrue(proxyLogResultHits.size()>1, "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains connections matching '"+proxyLogGrepPattern+"' from "+ipv4_address+" to candlepin server '"+sm_serverHostname+"'.  Expecting more than one connection for the sake of Bug 1301215: Test proxy connection before making call.  (Actual proxy connections was '"+proxyLogResultHits.size()+"').");
				}
			}
		}
	}
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47907", "RHEL7-96745"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28585",	// RHSM-REQ : Proxy settings
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84941",	// RHSM-REQ : Proxy settings
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.MEDIUM, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager : register with noproxy configured in rhsm.conf matching server.hostname should ignore proxy configurations and NOT send traffic through the configured proxy. (Positive and Negative Variations)",
			groups={"Tier3Tests","blockedByBug-1420533"/*inspired the implementation of no_proxy configuration in rhsm.conf*/},
			dataProvider="getRegisterAttemptsToVerifyHonoringNoProxyData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testRegisterAttemptsToVerifyHonoringNoProxyViaRhsmConfig(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern, String noProxyConfig, Boolean hostnameMatchesNoProxyConfig) {
		if (clienttasks.isPackageVersion("python-rhsm", "<", "1.19.4-1")) throw new SkipException("Support for this test does not exist in this version of python-rhsm.  See bugzilla 1420533.");
		
		String moduleTask = "register";
		String proxyLogMarker;
		SSHCommandResult attemptResult;
		String noproxy=null;
		
		//Assert that setting the no_proxy config is honored when it matches server.hostname
		// regardless if the proxy hostname is specified via rhsm.conf or the proxy hostname is specified via command line arguments
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig, noProxyConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());	// just to log the proxy configs
		
		// pad the tail of proxyLog with a message
		proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+"AttemptsToVerifyHonoringNoProxyViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// attempt to register with a no_proxy configuration declaring a list of proxy servers to ignore
		attemptResult = clienttasks.register_(username, password, org, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, proxy, proxyuser, proxypassword, noproxy);
		if (exitCode==Integer.valueOf(69)) {	// EX_UNAVAILABLE	// indicative of a bad proxy
			// when the proxy is unavailable, subscription-manager now aborts before making any decisions about no_proxy environment variables... Bug 1176219: Error out if bad proxy settings detected
			if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" with an unavailable proxy should abort in honor of bug 1176219 regardless of a no_proxy configuration setting.");
			if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" with an unavailable proxy should abort in honor of bug 1176219 regardless of a no_proxy configuration setting.");
			if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" with an unavailable proxy should abort in honor of bug 1176219 regardless of a no_proxy configuration setting.");
		} else
		if (hostnameMatchesNoProxyConfig) {
			Assert.assertEquals(attemptResult.getExitCode(), Integer.valueOf(0), "The exit code from an attempt to "+moduleTask+" using a no_proxy configuration '"+noProxyConfig+"' that matches the hostname '"+sm_serverHostname+"' to override both the configured and CLI option proxy.");
			Assert.assertEquals(attemptResult.getStderr().trim(), "", "The stderr from an attempt to "+moduleTask+" using a no_proxy configuration '"+noProxyConfig+"' that matches the server hostname '"+sm_serverHostname+"' to override both the configured and CLI option proxy.");
			Assert.assertNotNull(clienttasks.getCurrentConsumerCert(), "The system has succesfully registered a consumer.");
			
			// assert that no traffic has gone through the proxy logs
			// however... when a valid proxy is specified, there WILL be one connection from commit 7ce6801fc1cc38edcdeb75dfb5f0d1f8a6398c68 bug 1301215 that is testing the validity of the proxy, therefore we must tolerate at most one connection.
			String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
			List <String> proxyLogResultHits = Arrays.asList(proxyLogResult.trim().split("\n"));
			Assert.assertTrue(proxyLogResultHits.size()<=1, "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains zero connections (or at most one for the sake of Bug 1301215: Test proxy connection before making call) from "+ipv4_address+" to candlepin server '"+sm_serverHostname+"'.  (Actual proxy connections was '"+proxyLogResultHits.size()+"').");

		} else {	// the server.hostname does NOT match no_proxy and the no_proxy environment variable should have no effect.  Assert expected results from dataProvider.
			if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" when no_proxy configuration '"+noProxyConfig+"' does not match the server hostname '"+sm_serverHostname+"'.");
			if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" when no_proxy configuration '"+noProxyConfig+"' does not match the server hostname '"+sm_serverHostname+"'.");
			if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" when no_proxy configuration '"+noProxyConfig+"' does not match the server hostname '"+sm_serverHostname+"'.");
			
			// assert the tail of proxyLog shows the proxyLogGrepPattern (BASIC AUTH)
			// 1292545301.350    418 10.16.120.247 TCP_MISS/200 1438 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.146 -
			// 1292551602.625      0 10.16.120.247 TCP_DENIED/407 3840 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 - NONE/- text/html
			
			// assert the tail of proxyLog shows the proxyLogGrepPattern (NO AUTH)
			// CONNECT   Dec 17 18:56:22 [20793]: Connect (file descriptor 7):  [10.16.120.248]
			// CONNECT   Dec 17 18:56:22 [20793]: Request (file descriptor 7): CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 HTTP/1.1
			// INFO      Dec 17 18:56:22 [20793]: No proxy for jsefler-f12-candlepin.usersys.redhat.com
			// CONNECT   Dec 17 18:56:22 [20793]: Established connection to host "jsefler-f12-candlepin.usersys.redhat.com" using file descriptor 8.
			// INFO      Dec 17 18:56:22 [20793]: Not sending client headers to remote machine
			// INFO      Dec 17 18:56:22 [20793]: Closed connection between local client (fd:7) and remote client (fd:8)
			
			if (proxyLogGrepPattern!=null) {
				String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
				List <String> proxyLogResultHits = Arrays.asList(proxyLogResult.trim().split("\n"));
				if (proxyLogResult.contains("TCP_DENIED")) {
					Assert.assertTrue(proxyLogResultHits.size()==1, "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains exactly one failed connection attempt matching '"+proxyLogGrepPattern+"' from "+ipv4_address+" to candlepin server '"+sm_serverHostname+"'.  Because the connection attempt was expected to fail (likely due to invalid proxy credentials), no additional proxy traffic will be attempted (for the sake of Bug 1301215: Test proxy connection before making call) regardless if hostname matches the no_proxy list.  (Actual proxy connections was '"+proxyLogResultHits.size()+"').");
				} else {	
					Assert.assertTrue(proxyLogResultHits.size()>1, "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains connections matching '"+proxyLogGrepPattern+"' from "+ipv4_address+" to candlepin server '"+sm_serverHostname+"'.  Expecting more than one connection for the sake of Bug 1301215: Test proxy connection before making call.  (Actual proxy connections was '"+proxyLogResultHits.size()+"').");
				}
			}
		}
	}
	@DataProvider(name="getRegisterAttemptsToVerifyHonoringNoProxyData")
	public Object[][] getRegisterAttemptsToVerifyHonoringNoProxyDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRegisterAttemptsToVerifyHonoringNoProxyDataAsListOfLists());
	}
	protected List<List<Object>> getRegisterAttemptsToVerifyHonoringNoProxyDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		// Notes: from http://www.gnu.org/software/emacs/manual/html_node/url/Proxies.html
		//	The NO_PROXY environment variable specifies URLs that should be excluded from proxying
		//	(on servers that should be contacted directly). This should be a comma-separated list
		//	of hostnames, domain names, or a mixture of both. Asterisks can be used as wildcards,
		//	but other clients may not support that. Domain names may be indicated by a leading dot.
		//	For example:
		//
		//	NO_PROXY="*.aventail.com,home.com,.seanet.com"
		//
		//	says to contact all machines in the ‘aventail.com’ and ‘seanet.com’ domains directly,
		//	as well as the machine named ‘home.com’. If NO_PROXY isn’t defined, no_PROXY and no_proxy
		//	are also tried, in that order. 
		
		///* TOO EXHAUSTIVE TAKES 40 MINUTES */ for (List<Object> l : getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
		for (List<Object> l : getRandomSubsetOfList(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists(),3)) {
			
			// append a value for no_proxy and a boolean to indicate if it should be honored or not
			String noProxy;
			Boolean hostnameMatchesNoProxy;
			
			// get the existing BlockedByBzBug
			BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			// add more BlockedByBzBug to rows that are expecting a network error
			//bugIds.add("1234");	// Bug 1234

			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			// Object blockedByBug (0), String username (1), String password (2), Sring org (3), String proxy (4), String proxyuser (5), String proxypassword (6), String proxy_hostnameConfig (7), String proxy_portConfig (8), String proxy_userConfig (9), String proxy_passwordConfig (10), Integer exitCode (11), String stdout (12), String stderr (13), SSHCommandRunner proxyRunner (14), String proxyLog (15), String proxyLogGrepPattern (16)
			
			bugIds.add("1457197");	// Bug 1457197 - --noproxy option no longer match "*" for host names
			noProxy = "*"; hostnameMatchesNoProxy=true;	// * matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(bugIds.toArray(new String[]{})),	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxy, hostnameMatchesNoProxy}));
			bugIds.remove("1457197");
			
			noProxy = sm_serverHostname; hostnameMatchesNoProxy=true;	// subscription.rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxy, hostnameMatchesNoProxy}));

			noProxy = "*.aventail.com,home.com,.seanet.com,"+sm_serverHostname; hostnameMatchesNoProxy=true;	// *.aventail.com,home.com,.seanet.com,subscription.rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxy, hostnameMatchesNoProxy}));

			noProxy = sm_serverHostname.replaceFirst("[^\\.]+", ""); hostnameMatchesNoProxy=true;	// .rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxy, hostnameMatchesNoProxy}));
			
			noProxy = "*.aventail.com,home.com,.seanet.com,"+sm_serverHostname.replaceFirst("[^\\.]+", ""); hostnameMatchesNoProxy=true;	// *.aventail.com,home.com,.seanet.com,.rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxy, hostnameMatchesNoProxy}));
			
			noProxy = "*.aventail.com,home.com,.seanet.com"; hostnameMatchesNoProxy=false;	// *.aventail.com,home.com,.seanet.com does not match subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxy, hostnameMatchesNoProxy}));
			
			bugIds.add("1443164");	// Bug 1443164 	no_proxy does not match the host name when *.redhat.com is used	// This case does not work on RHEL since the the only wildcard supported by the library is no_proxy=* which is likely an RFE bug against component X?
			noProxy = sm_serverHostname.replaceFirst("[^\\.]+", "*"); hostnameMatchesNoProxy=true;	// *.rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(bugIds.toArray(new String[]{})),	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxy, hostnameMatchesNoProxy}));
			bugIds.remove("1443164");
		}
		return ll;
	}
	
	
	
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47949", "RHEL-112431"},
			linkedWorkItems= {
//				@LinkedItem(
//					workitemId= "",
//					project= Project.RHEL6,
//					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-95148",	// RHSM-REQ : Update 'invalid credentials' error to reflect a warning about network proxies
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.LOW, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="When subscription-manager receives an 401 Unauthorized response while employing a proxy server, a more informative message than \"Invalid Credentials\" should be displayed pointing at a faulty proxy configuration.",
			groups={"Tier3Tests","blockedByBug-1354667","testUnauthorizedResponseFromProxyServer"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testUnauthorizedResponseFromProxyServer() {
		if (clienttasks.isPackageVersion("python-rhsm", "<", "1.20.3-1")) { // commit 33dd2efec9fa0368fe39affdc3f6f19c893ff62c	Bug 1354667: Add identity cert detection to proxy error message generation
			throw new SkipException("Support for this test does not exist in this version of python-rhsm.  See bugzilla 1354667.");
		}
		
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		
		// configure a basic auth proxy
		listOfSectionNameValues.clear();
		listOfSectionNameValues.add(new String[]{"server", "proxy_hostname", sm_basicauthproxyHostname});
		listOfSectionNameValues.add(new String[]{"server", "proxy_port",     sm_basicauthproxyPort});
		listOfSectionNameValues.add(new String[]{"server", "proxy_user",     sm_basicauthproxyUsername});
		listOfSectionNameValues.add(new String[]{"server", "proxy_password", sm_basicauthproxyPassword});
		listOfSectionNameValues.add(new String[]{"server", "insecure", "0"});
		clienttasks.config(null, null, true, listOfSectionNameValues);
		
		// register and remember the consumerId
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null));
		
		// get the current identity
		SSHCommandResult identityResult = clienttasks.identity(null, null, null, null, null, null, null, null);
		
		// assert the identity result includes the consumerId
		Assert.assertTrue(identityResult.getStdout().contains(consumerId), "The identity '"+consumerId+"' was retrieved while employing a basic auth proxy server.");
		
		// now let's try to get the identity through the intercepting proxy...
		
		// configure an intercepting unauthorized proxy
		listOfSectionNameValues.clear();
		listOfSectionNameValues.add(new String[]{"server", "proxy_hostname", sm_unauthproxyHostname});
		listOfSectionNameValues.add(new String[]{"server", "proxy_port",     sm_unauthproxyPort});
		listOfSectionNameValues.add(new String[]{"server", "proxy_user",     sm_unauthproxyUsername});
		listOfSectionNameValues.add(new String[]{"server", "proxy_password", sm_unauthproxyPassword});
		listOfSectionNameValues.add(new String[]{"server", "insecure", "1"});	// avoids the need to fetch a copy of myCA.pem from the intercepting proxy server into /etc/rhsm/ca/
		clienttasks.config(null, null, true, listOfSectionNameValues);
		
		// pad the tail of proxyLog with a message
		String logMarker = System.currentTimeMillis()+" Testing testUnauthorizedResponseFromProxyServer";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);

		// attempt to get the identity through the intercepting proxy
		identityResult = clienttasks.identity_(null, null, null, null, null, null, null, null);

		// assert the newly improved message from RFE Bug 1354667 to help a system admin troubleshoot a
		String expectedFeedback = "Unable to make a connection using SSL client certificate. Please review proxy configuration and connectivity.";
		Assert.assertEquals(identityResult.getStdout().trim(), "", "Stdout from subscription-manager request through an intercepting proxy server.");
		Assert.assertEquals(identityResult.getStderr().trim(), expectedFeedback, "Stderr from subscription-manager request through an intercepting proxy server.");
		Assert.assertEquals(identityResult.getExitCode(), Integer.valueOf(70)/*EX_SOFTWARE=70*/, "ExitCode from subscription-manager request through an intercepting proxy server.");
		// 2017-10-27 12:26:38,427 [DEBUG] subscription-manager:6926:MainThread @connection.py:500 - Using proxy: auto-services.usersys.redhat.com:3130
		// 2017-10-27 12:26:38,427 [DEBUG] subscription-manager:6926:MainThread @connection.py:515 - Making request: GET /candlepin/consumers/342455a8-edf2-40cb-bea5-4623df44509b/owner
		// 2017-10-27 12:26:38,742 [INFO] subscription-manager:6926:MainThread @connection.py:556 - Response: status=401, requestUuid=035c5bae-ab70-445e-b1d4-f6dbcc2fd59b, request="GET /candlepin/consumers/342455a8-edf2-40cb-bea5-4623df44509b/owner"
		// 2017-10-27 12:26:38,745 [ERROR] subscription-manager:6926:MainThread @managercli.py:715 - Unable to make a connection using SSL client certificate. Please review proxy configuration and connectivity.
		String logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Response");
		Assert.assertTrue(logTail.contains("status=401"), "The '"+clienttasks.rhsmLogFile+"' reports an 401 Unauthorized response.");
		logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "ERROR");
		Assert.assertTrue(logTail.contains(expectedFeedback), "The '"+clienttasks.rhsmLogFile+"' reports expected ERROR '"+expectedFeedback+"'.");
		
	}
	@AfterGroups(value={"testUnauthorizedResponseFromProxyServer"}, groups={"setup"}, alwaysRun=true)
	public void unsetProxyConfigAfterGroups () {
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[]{"server", "proxy_hostname", ""});
		listOfSectionNameValues.add(new String[]{"server", "proxy_port", ""});
		listOfSectionNameValues.add(new String[]{"server", "proxy_user", ""});
		listOfSectionNameValues.add(new String[]{"server", "proxy_password", ""});
		listOfSectionNameValues.add(new String[]{"server", "insecure", "0"});
		clienttasks.config(null,null,true,listOfSectionNameValues);
	}
	
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 744504 - [ALL LANG] [RHSM CLI] facts module - Run facts update with incorrect proxy url produces traceback. https://github.com/RedHatQE/rhsm-qe/issues/179
	
	
	// Configuration methods ***********************************************************************
	
	public static SSHCommandRunner basicauthproxy = null;
	public static SSHCommandRunner noauthproxy = null;
	public static SSHCommandRunner unauthproxy = null;
	public static String nErrMsg = null;
	public static String pErrMsg = null;
	public static String pErr407Msg = null;
	public static String pErrConMsg = null;
	public static String rErrMsg = null;
	protected String ipv4_address = null;

	@BeforeClass(groups={"setup"})
	public void setupBeforeClass() throws IOException {
		basicauthproxy = new SSHCommandRunner(sm_basicauthproxyHostname, sm_basicauthproxySSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase, null);
		noauthproxy = new SSHCommandRunner(sm_noauthproxyHostname, sm_noauthproxySSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase, null);
		unauthproxy = new SSHCommandRunner(sm_unauthproxyHostname, sm_unauthproxySSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase, null);
		if (sm_sshEmergenecyTimeoutMS!=null) basicauthproxy.setEmergencyTimeout(Long.valueOf(sm_sshEmergenecyTimeoutMS));
		if (sm_sshEmergenecyTimeoutMS!=null) noauthproxy.setEmergencyTimeout(Long.valueOf(sm_sshEmergenecyTimeoutMS));
		if (sm_sshEmergenecyTimeoutMS!=null) unauthproxy.setEmergencyTimeout(Long.valueOf(sm_sshEmergenecyTimeoutMS));
		if (clienttasks!=null) nErrMsg = clienttasks.msg_NetworkErrorUnableToConnect;
		if (clienttasks!=null) pErrMsg = clienttasks.msg_ProxyConnectionFailed;
		if (clienttasks!=null) pErr407Msg = clienttasks.msg_ProxyConnectionFailed407;
		if (clienttasks!=null) pErrConMsg = clienttasks.msg_ProxyErrorUnableToConnect;
		if (clienttasks!=null) {		
			String hostname = clienttasks.getConfParameter("hostname");
			String prefix = clienttasks.getConfParameter("prefix");
			String port = clienttasks.getConfParameter("port");
			rErrMsg = "Unable to reach the server at "+hostname+":"+port+prefix;	// Unable to reach the server at jsefler-candlepin7.usersys.redhat.com:8443/candlepin	// Last request from /var/log/rhsm/rhsm.log results in: error: Tunnel connection failed: 407 Proxy Authentication Required
		}
		if (clienttasks!=null) ipv4_address = clienttasks.getIPV4Address();
	}
	
	@BeforeMethod(groups={"setup"})
	public void cleanRhsmConfigAndUnregisterBeforeMethod() {
		uncommentConfFileProxyParameters();
		updateConfFileProxyParameters("","","","", "");
		clienttasks.unregister(null, null, null, null);
	}
	
	@AfterClass(groups={"setup"})
	public void cleanRhsmConfigAfterClass() throws IOException {
		cleanRhsmConfigAndUnregisterBeforeMethod();
	}
	
	@BeforeGroups(value={"ProxyRedeemTests"}, groups={"setup"})
	public void createMockAssetFactsFile () {
		// create a facts file with a serialNumber that will clobber the true system facts
		Map<String,String> facts = new HashMap<String,String>();
		facts.put("dmi.system.manufacturer", "Dell Inc.");
		facts.put("dmi.system.serial_number", "5ABCDEF");	// Mock Setup For: "Your subscription activation is being processed and should be available soon. You will be notified via email once it is available. If you have any questions, additional information can be found here: https://access.redhat.com/kb/docs/DOC-53864."
		clienttasks.createFactsFileWithOverridingValues(facts);
	}
	
	@AfterGroups(value={"ProxyRedeemTests"}, groups={"setup"}, alwaysRun=true)
	public void deleteMockAssetFactsFile () {
		clienttasks.deleteFactsFileWithOverridingValues();
	}
	
	
	
	// Protected methods ***********************************************************************
	
	protected void uncommentConfFileProxyParameters() {
		clienttasks.uncommentConfFileParameter(clienttasks.rhsmConfFile, "proxy_hostname");
		clienttasks.uncommentConfFileParameter(clienttasks.rhsmConfFile, "proxy_port");
		clienttasks.uncommentConfFileParameter(clienttasks.rhsmConfFile, "proxy_user");
		clienttasks.uncommentConfFileParameter(clienttasks.rhsmConfFile, "proxy_password");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) {	// implemented by commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9 1420533: Add no_proxy option to API, config, UI
			clienttasks.uncommentConfFileParameter(clienttasks.rhsmConfFile, "no_proxy");
		}
	}
	protected void updateConfFileProxyParameters(String proxy_hostname, String proxy_port, String proxy_user, String proxy_password, String no_proxy) {
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_hostname", proxy_hostname);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_port", proxy_port);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_user", proxy_user);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_password", proxy_password);
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) {	// implemented by commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9 1420533: Add no_proxy option to API, config, UI
			clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "no_proxy", no_proxy);
		}

	}

	
	// Data Providers ***********************************************************************
	
	
	@DataProvider(name="getRegisterWithProxyConfigurationsCommentedOutOfRhsmConfigData")
	public Object[][] getRegisterWithProxyConfigurationsCommentedOutOfRhsmConfigDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRegisterWithProxyConfigurationsCommentedOutOfRhsmConfigDataAsListOfLists());
	}
	protected List<List<Object>> getRegisterWithProxyConfigurationsCommentedOutOfRhsmConfigDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();

		// String[] proxyConfigs

		ll.add(Arrays.asList(new Object[] {null,	new String[]{"proxy_hostname"}  }));
		ll.add(Arrays.asList(new Object[] {null,	new String[]{"proxy_port"}  }));
		ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("667829"), new String[]{"proxy_hostname", "proxy_port"} }));
		ll.add(Arrays.asList(new Object[] {null,	new String[]{"proxy_user"}  }));
		ll.add(Arrays.asList(new Object[] {null,	new String[]{"proxy_password"}  }));
		ll.add(Arrays.asList(new Object[] {null,	new String[]{"proxy_user", "proxy_password"}  }));
		ll.add(Arrays.asList(new Object[] {null,	new String[]{"proxy_hostname", "proxy_port", "proxy_user", "proxy_password"}  }));


		return ll;
	}
	
	
	@DataProvider(name="getRegisterAttemptsUsingProxyServerData")
	public Object[][] getRegisterAttemptsUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRegisterAttemptsUsingProxyServerDataAsListOfLists());
	}
	protected List<List<Object>> getRegisterAttemptsUsingProxyServerDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (clienttasks==null) return ll;
		
		String basicauthproxyUrl = String.format("%s:%s", sm_basicauthproxyHostname,sm_basicauthproxyPort); basicauthproxyUrl = basicauthproxyUrl.replaceAll(":$", "");
		String noauthproxyUrl = String.format("%s:%s", sm_noauthproxyHostname,sm_noauthproxyPort); noauthproxyUrl = noauthproxyUrl.replaceAll(":$", "");
		String uErrMsg = servertasks.invalidCredentialsMsg(); //"Invalid username or password";
		String oErrMsg = /*"Organization/Owner bad-org does not exist."*/"Organization bad-org does not exist.";
		if (sm_serverType.equals(CandlepinType.katello))	oErrMsg = "Couldn't find organization 'bad-org'";
		
		// Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr

		if (clienttasks.isPackageVersion("subscription-manager",">=","1.18.2-1")) {	// post commit ad982c13e79917e082f336255ecc42615e1e7707	1176219: Error out if bad proxy settings detected
			// basic auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","1176219"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	"bad-proxy",			sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(69),	null,		pErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","1301215"}),								sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl+"0",	sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(69),	null,		pErrMsg}));
			if (clienttasks.isPackageVersion("python-rhsm",">=","1.18.5-1") && Integer.valueOf(clienttasks.redhatReleaseX)>=7) {	// post commit 214103dcffce29e31858ffee414d79c1b8063970	Reduce usage of m2crypto https://github.com/candlepin/python-rhsm/pull/184
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),		sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		"bad-username",					sm_basicauthproxyPassword,	Integer.valueOf(69),	null,		rErrMsg}));
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),		sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		"bad-password",				Integer.valueOf(69),	null,		rErrMsg}));
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),		sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		"bad-username",					"bad-password",				Integer.valueOf(69),	null,		rErrMsg}));
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),		sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		null,							null,						Integer.valueOf(69),	null,		rErrMsg}));
			} else {
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),		sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		"bad-username",					sm_basicauthproxyPassword,	Integer.valueOf(69),	null,		pErrMsg}));
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),		sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		"bad-password",				Integer.valueOf(69),	null,		pErrMsg}));
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),		sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		"bad-username",					"bad-password",				Integer.valueOf(69),	null,		pErrMsg}));
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),		sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		null,							null,						Integer.valueOf(69),	null,		pErrMsg}));
			}
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242","1354667"}),	"bad-username",		sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242","1354667"}),	sm_clientUsername,	"bad-password",		sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242"}),			sm_clientUsername,	sm_clientPassword,	"bad-org",		basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		oErrMsg}));
			
			// no auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),						sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			null,							null,						Integer.valueOf(0),		null,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),						sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			"ignored-username",				"ignored-password",			Integer.valueOf(0),		null,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","1301215"}),								sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl+"0",		null,							null,						Integer.valueOf(69),	null,		pErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1354667"}),			"bad-username",		sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			null,							null,						Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1354667"}),			sm_clientUsername,	"bad-password",		sm_clientOrg,	noauthproxyUrl,			null,							null,						Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),						sm_clientUsername,	sm_clientPassword,	"bad-org",		noauthproxyUrl,			null,							null,						Integer.valueOf(70),	null,		oErrMsg}));

		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.9-1")) {	// post commit a695ef2d1da882c5f851fde90a24f957b70a63ad
			// basic auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null}));
		    ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688"}),						sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	"bad-proxy",			sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		nErrMsg}));
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.17.6-1")) {	// post commit 7ce6801fc1cc38edcdeb75dfb5f0d1f8a6398c68	1301215: Test proxy connection before making call	1176219: Stop before cache is returned when using bad proxy options
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","1301215"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl+"0",	sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(69),	null,		pErrMsg}));
			}else {
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688"}),							sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl+"0",	sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		nErrMsg}));
			}
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		"bad-username",					sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		nErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		"bad-password",				Integer.valueOf(70),	null,		nErrMsg}));
			
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		"bad-username",					"bad-password",				Integer.valueOf(70),	null,		nErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		null,							null,						Integer.valueOf(70),	null,		nErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242"}),	"bad-username",		sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242"}),	sm_clientUsername,	"bad-password",		sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242"}),	sm_clientUsername,	sm_clientPassword,	"bad-org",		basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		oErrMsg}));
			
			// no auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			null,							null,						Integer.valueOf(0),		null,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			"ignored-username",				"ignored-password",			Integer.valueOf(0),		null,		null}));
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.17.6-1")) {	// post commit 7ce6801fc1cc38edcdeb75dfb5f0d1f8a6398c68	1301215: Test proxy connection before making call	1176219: Stop before cache is returned when using bad proxy options
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","1301215"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl+"0",		null,							null,						Integer.valueOf(69),	null,		pErrMsg}));			
			} else {
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688"}),							sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl+"0",		null,							null,						Integer.valueOf(70),	null,		nErrMsg}));
			}
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				"bad-username",		sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			null,							null,						Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	"bad-password",		sm_clientOrg,	noauthproxyUrl,			null,							null,						Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	"bad-org",		noauthproxyUrl,			null,							null,						Integer.valueOf(70),	null,		oErrMsg}));

		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			// basic auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258","838242"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	"bad-proxy",			sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	nErrMsg,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl+"0",	sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	nErrMsg,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		"bad-username",					sm_basicauthproxyPassword,	Integer.valueOf(70),	nErrMsg,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		"bad-password",				Integer.valueOf(70),	nErrMsg,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		"bad-username",					"bad-password",				Integer.valueOf(70),	nErrMsg,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		null,							null,						Integer.valueOf(70),	nErrMsg,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258","838242"}),	"bad-username",		sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258","838242"}),	sm_clientUsername,	"bad-password",		sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258","838242"}),	sm_clientUsername,	sm_clientPassword,	"bad-org",		basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		oErrMsg}));
			
			// no auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			null,							null,						Integer.valueOf(0),		null,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			"ignored-username",				"ignored-password",			Integer.valueOf(0),		null,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl+"0",		null,							null,						Integer.valueOf(70),	nErrMsg,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			"bad-username",		sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			null,							null,						Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	"bad-password",		sm_clientOrg,	noauthproxyUrl,			null,							null,						Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	"bad-org",		noauthproxyUrl,			null,							null,						Integer.valueOf(70),	null,		oErrMsg}));

		} else {
			// basic auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"755258","838242"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null}));
			ll.add(Arrays.asList(new Object[]{	null,													sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	"bad-proxy",			sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(255),	nErrMsg,	null}));
			ll.add(Arrays.asList(new Object[]{	null,													sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl+"0",	sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(255),	nErrMsg,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		"bad-username",					sm_basicauthproxyPassword,	Integer.valueOf(255),	nErrMsg,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		"bad-password",				Integer.valueOf(255),	nErrMsg,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		"bad-username",					"bad-password",				Integer.valueOf(255),	nErrMsg,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		null,							null,						Integer.valueOf(255),	nErrMsg,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"755258","838242"}),	"bad-username",		sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(255),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"755258","838242"}),	sm_clientUsername,	"bad-password",		sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(255),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"755258","838242"}),	sm_clientUsername,	sm_clientPassword,	"bad-org",		basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(255),	null,		oErrMsg}));
			
			// no auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			null,						null,					Integer.valueOf(0),		null,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			"ignored-username",			"ignored-password",		Integer.valueOf(0),		null,		null}));
			ll.add(Arrays.asList(new Object[]{	null,													sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl+"0",		null,						null,					Integer.valueOf(255),	nErrMsg,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"755258"}),				"bad-username",		sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			null,						null,					Integer.valueOf(255),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"755258"}),				sm_clientUsername,	"bad-password",		sm_clientOrg,	noauthproxyUrl,			null,						null,					Integer.valueOf(255),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"755258"}),				sm_clientUsername,	sm_clientPassword,	"bad-org",		noauthproxyUrl,			null,						null,					Integer.valueOf(255),	null,		oErrMsg}));
		}
		
		return ll;
	}

	
	@DataProvider(name="getRegisterAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getRegisterAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
	}
	protected List<List<Object>> getRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (clienttasks==null) return ll;
		
		String basicauthproxyUrl = String.format("%s:%s", sm_basicauthproxyHostname,sm_basicauthproxyPort); basicauthproxyUrl = basicauthproxyUrl.replaceAll(":$", "");
		String noauthproxyUrl = String.format("%s:%s", sm_noauthproxyHostname,sm_noauthproxyPort); noauthproxyUrl = noauthproxyUrl.replaceAll(":$", "");
		String uErrMsg = servertasks.invalidCredentialsMsg(); //"Invalid username or password";
		String oErrMsg = "Organization/Owner bad-org does not exist."; oErrMsg = "Organization bad-org does not exist.";
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-1")) {	// post commit 630e1a2eb06e6bfacac669ce11f38e228c907ea9 1507030: RestlibExceptions should show they originate server-side
			uErrMsg = "HTTP error (401 - Unauthorized): "+uErrMsg;
			oErrMsg = "HTTP error (400 - Bad Request): "+oErrMsg;
		}
		if (sm_serverType.equals(CandlepinType.katello))	oErrMsg = "Couldn't find organization 'bad-org'";
//		String hostname = clienttasks.getConfParameter("hostname");
//		String prefix = clienttasks.getConfParameter("prefix");
//		String port = clienttasks.getConfParameter("port");
		
		
		// Object blockedByBug, String username, String password, Sring org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern
		// 									blockedByBug,											username,			password,			org,			proxy,				proxyuser,					proxypassword,				proxy_hostnameConfig,		proxy_portConfig,			proxy_userConfig,			proxy_passwordConfig,		exitCode,				stdout,		stderr,		proxyRunner,	proxyLog,				proxyLogGrepPattern

		if (clienttasks.isPackageVersion("subscription-manager",">=","1.18.2-1")) {	// post commit ad982c13e79917e082f336255ecc42615e1e7707	1176219: Error out if bad proxy settings detected
			// basic auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			if(clienttasks.isPackageVersion("subscription-manager",">=","1.20.1-1")) {	// commit c26af03e547209f216d29d02867d73843e1e5535	// Bug 1392709: Display better error msg., when wrong proxy is set up
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","1392709"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						"bad-proxy",				sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	pErrConMsg,	basicauthproxy,	sm_basicauthproxyLog,	null}));
			} else {
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688"}),						sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						"bad-proxy",				sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	nErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	null}));
			}
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688"}),						sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort+"0",	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	nErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	null}));
if (false) {	// DELETEME in favor of blockedByBadAuthBugs below
			if (clienttasks.isPackageVersion("python-rhsm",">=","1.18.5-1") && Integer.valueOf(clienttasks.redhatReleaseX)>=7) {	// post commit 214103dcffce29e31858ffee414d79c1b8063970	Reduce usage of m2crypto https://github.com/candlepin/python-rhsm/pull/184
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	nErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	"bad-password",				Integer.valueOf(70),	null,	nErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				"bad-password",				Integer.valueOf(70),	null,	nErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		""/*no username*/,			""/*no password*/,			Integer.valueOf(70),	null,	nErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			} else {
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	pErr407Msg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	"bad-password",				Integer.valueOf(70),	null,	pErr407Msg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				"bad-password",				Integer.valueOf(70),	null,	pErr407Msg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		""/*no username*/,			""/*no password*/,			Integer.valueOf(70),	null,	pErr407Msg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			}
}
			Set<String> badAuthBugIds = new HashSet<String>();
			badAuthBugIds.addAll(Arrays.asList("1345962","1119688","755258","1176219"));
			if (Integer.valueOf(clienttasks.redhatReleaseX)>=7) badAuthBugIds.add("1560727");	// Bug 1560727 - Uncaught proxy authentication failure (407) on RHEL7
			BlockedByBzBug blockedByBadAuthBugs = new BlockedByBzBug(badAuthBugIds.toArray(new String[]{}));
			if (clienttasks.isPackageVersion("python-rhsm",">=","1.18.5-1")) {	// post commit 214103dcffce29e31858ffee414d79c1b8063970	Reduce usage of m2crypto https://github.com/candlepin/python-rhsm/pull/184
				ll.add(Arrays.asList(new Object[]{	blockedByBadAuthBugs,	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	pErrConMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
				ll.add(Arrays.asList(new Object[]{	blockedByBadAuthBugs,	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	"bad-password",				Integer.valueOf(70),	null,	pErrConMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
				ll.add(Arrays.asList(new Object[]{	blockedByBadAuthBugs,	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				"bad-password",				Integer.valueOf(70),	null,	pErrConMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
				ll.add(Arrays.asList(new Object[]{	blockedByBadAuthBugs,	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		""/*no username*/,			""/*no password*/,			Integer.valueOf(70),	null,	pErrConMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			} else {
				ll.add(Arrays.asList(new Object[]{	blockedByBadAuthBugs,	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	pErr407Msg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
				ll.add(Arrays.asList(new Object[]{	blockedByBadAuthBugs,	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	"bad-password",				Integer.valueOf(70),	null,	pErr407Msg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
				ll.add(Arrays.asList(new Object[]{	blockedByBadAuthBugs,	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				"bad-password",				Integer.valueOf(70),	null,	pErr407Msg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
				ll.add(Arrays.asList(new Object[]{	blockedByBadAuthBugs,	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		""/*no username*/,			""/*no password*/,			Integer.valueOf(70),	null,	pErr407Msg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));	
			}
			
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	null,						null,						"bad-proxy",				sm_basicauthproxyPort+"0",	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	sm_basicauthproxyUsername,	null,						"bad-proxy",				sm_basicauthproxyPort+"0",	"bad-username",				sm_basicauthproxyPassword,	Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	"bad-proxy",				sm_basicauthproxyPort+"0",	"bad-username",				"bad-password",				Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1354667"}),	"bad-username",		sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	uErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1354667"}),	sm_clientUsername,	"bad-password",		sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	uErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	"bad-org",		null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	oErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			
			// no auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(0),		null,	null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"ignored-username",			"ignored-password",			Integer.valueOf(0),		null,	null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688"}),						sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort+"0",		"",							"",							Integer.valueOf(70),	null,	nErrMsg,	noauthproxy,	sm_noauthproxyLog,		null}));
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.20.1-1")) {	// commit c26af03e547209f216d29d02867d73843e1e5535	// Bug 1392709: Display better error msg., when wrong proxy is set up
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","1392709"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						"bad-proxy",				sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,	pErrConMsg,	noauthproxy,	sm_noauthproxyLog,		null}));
			} else {
				ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688"}),						sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						"bad-proxy",				sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,	nErrMsg,	noauthproxy,	sm_noauthproxyLog,		null}));
			}
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1354667"}),	"bad-username",		sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,	uErrMsg,	noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1354667"}),	sm_clientUsername,	"bad-password",		sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,	uErrMsg,	noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	"bad-org",		null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,	oErrMsg,	noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,		null,						null,						"bad-proxy",				sm_noauthproxyPort+"0",		"",							"",							Integer.valueOf(0),		null,	null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,		"ignored-username",			"ignored-password",			"bad-proxy",				sm_noauthproxyPort+"0",		"bad-username",				"bad-password",				Integer.valueOf(0),		null,	null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","1176219","1403387"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	"bad-proxy",		null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(69),	null,	pErrMsg/*DELETEME - THIS WAS A BUG 1403387 "Unable to reach the server at "+hostname+":"+port+prefix*/,	noauthproxy,	sm_noauthproxyLog,		null}));
			
		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.9-1")) {	// post commit a695ef2d1da882c5f851fde90a24f957b70a63ad
			// basic auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						"bad-proxy",				sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	nErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort+"0",	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	nErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	nErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	"bad-password",				Integer.valueOf(70),	null,	nErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				"bad-password",				Integer.valueOf(70),	null,	nErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		""/*no username*/,			""/*no password*/,			Integer.valueOf(70),	null,	nErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	null,						null,						"bad-proxy",				sm_basicauthproxyPort+"0",	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	sm_basicauthproxyUsername,	null,						"bad-proxy",				sm_basicauthproxyPort+"0",	"bad-username",				sm_basicauthproxyPassword,	Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	"bad-proxy",				sm_basicauthproxyPort+"0",	"bad-username",				"bad-password",				Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			"bad-username",		sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	uErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	"bad-password",		sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	uErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	"bad-org",		null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	oErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			
			// no auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(0),		null,	null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"ignored-username",			"ignored-password",			Integer.valueOf(0),		null,	null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort+"0",		"",							"",							Integer.valueOf(70),	null,	nErrMsg,	noauthproxy,	sm_noauthproxyLog,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						"bad-proxy",				sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,	nErrMsg,	noauthproxy,	sm_noauthproxyLog,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			"bad-username",		sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,	uErrMsg,	noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	"bad-password",		sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,	uErrMsg,	noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	"bad-org",		null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,	oErrMsg,	noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,		null,						null,						"bad-proxy",				sm_noauthproxyPort+"0",		"",							"",							Integer.valueOf(0),		null,	null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,		"ignored-username",			"ignored-password",			"bad-proxy",				sm_noauthproxyPort+"0",		"bad-username",				"bad-password",				Integer.valueOf(0),		null,	null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
		    ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	"bad-proxy",		null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,	nErrMsg,	noauthproxy,	sm_noauthproxyLog,		null}));
		} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688		// EX_SOFTWARE=70
			// basic auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						"bad-proxy",				sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	nErrMsg,	null,		basicauthproxy,	sm_basicauthproxyLog,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort+"0",	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	nErrMsg,	null,		basicauthproxy,	sm_basicauthproxyLog,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				sm_basicauthproxyPassword,	Integer.valueOf(70),	nErrMsg,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	"bad-password",				Integer.valueOf(70),	nErrMsg,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				"bad-password",				Integer.valueOf(70),	nErrMsg,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		""/*no username*/,			""/*no password*/,			Integer.valueOf(70),	nErrMsg,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	null,						null,						"bad-proxy",				sm_basicauthproxyPort+"0",	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	sm_basicauthproxyUsername,	null,						"bad-proxy",				sm_basicauthproxyPort+"0",	"bad-username",				sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258","838242"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	"bad-proxy",				sm_basicauthproxyPort+"0",	"bad-username",				"bad-password",				Integer.valueOf(0),		null,		null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			"bad-username",		sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		uErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	"bad-password",		sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		uErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	"bad-org",		null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		oErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			
			// no auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(0),		null,		null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"ignored-username",			"ignored-password",			Integer.valueOf(0),		null,		null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort+"0",		"",							"",							Integer.valueOf(70),	nErrMsg,	null,		noauthproxy,	sm_noauthproxyLog,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						"bad-proxy",				sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	nErrMsg,	null,		noauthproxy,	sm_noauthproxyLog,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			"bad-username",		sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,		uErrMsg,	noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	"bad-password",		sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,		uErrMsg,	noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	"bad-org",		null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,		oErrMsg,	noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,		null,						null,						"bad-proxy",				sm_noauthproxyPort+"0",		"",							"",							Integer.valueOf(0),		null,		null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","755258"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,		"ignored-username",			"ignored-password",			"bad-proxy",				sm_noauthproxyPort+"0",		"bad-username",				"bad-password",				Integer.valueOf(0),		null,		null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688"}),					sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	"bad-proxy",		null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	nErrMsg,	null,		noauthproxy,	sm_noauthproxyLog,		null}));
			
		} else {
			// basic auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	null,															sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						"bad-proxy",				sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(255),	nErrMsg,	null,		basicauthproxy,	sm_basicauthproxyLog,	null}));
			ll.add(Arrays.asList(new Object[]{	null,															sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort+"0",	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(255),	nErrMsg,	null,		basicauthproxy,	sm_basicauthproxyLog,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				sm_basicauthproxyPassword,	Integer.valueOf(255),	nErrMsg,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	"bad-password",				Integer.valueOf(255),	nErrMsg,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				"bad-password",				Integer.valueOf(255),	nErrMsg,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		""/*no username*/,			""/*no password*/,			Integer.valueOf(255),	nErrMsg,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	null,						null,						"bad-proxy",				sm_basicauthproxyPort+"0",	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	sm_basicauthproxyUsername,	null,						"bad-proxy",				sm_basicauthproxyPort+"0",	"bad-username",				sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"755258","838242"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	"bad-proxy",				sm_basicauthproxyPort+"0",	"bad-username",				"bad-password",				Integer.valueOf(0),		null,		null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									"bad-username",		sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(255),	null,		uErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	"bad-password",		sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(255),	null,		uErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	sm_clientPassword,	"bad-org",		null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(255),	null,		oErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			
			// no auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(0),		null,		null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"ignored-username",			"ignored-password",			Integer.valueOf(0),		null,		null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	null,															sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort+"0",		"",							"",							Integer.valueOf(255),	nErrMsg,	null,		noauthproxy,	sm_noauthproxyLog,		null}));
			ll.add(Arrays.asList(new Object[]{	null,															sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						"bad-proxy",				sm_noauthproxyPort,			"",							"",							Integer.valueOf(255),	nErrMsg,	null,		noauthproxy,	sm_noauthproxyLog,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									"bad-username",		sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(255),	null,		uErrMsg,	noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	"bad-password",		sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(255),	null,		uErrMsg,	noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	sm_clientPassword,	"bad-org",		null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(255),	null,		oErrMsg,	noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,		null,						null,						"bad-proxy",				sm_noauthproxyPort+"0",		"",							"",							Integer.valueOf(0),		null,		null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug("755258"),									sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,		"ignored-username",			"ignored-password",			"bad-proxy",				sm_noauthproxyPort+"0",		"bad-username",				"bad-password",				Integer.valueOf(0),		null,		null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	null,															sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	"bad-proxy",		null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(255),	nErrMsg,	null,		noauthproxy,	sm_noauthproxyLog,		null}));
		}
		
		return ll;
	}
	protected List<List<Object>> getValidRegisterAttemptsUsingProxyServerDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getRegisterAttemptsUsingProxyServerDataAsListOfLists()) {
			// only include dataProvided rows where username, password, and org are valid
			if (!(l.get(1).equals(sm_clientUsername) && l.get(2).equals(sm_clientPassword) && l.get(3)==sm_clientOrg)) continue;
//			if (l.get(8)==nErrMsg) l.set(0,new BlockedByBzBug("838264"));
			if (l.get(8)/*stdout*/==nErrMsg || l.get(9)/*stderr*/==nErrMsg) l.set(0,new BlockedByBzBug(new String[]{"838264","1345962"}));

			ll.add(l);
		}
		return ll;
	}
	protected List<List<Object>> getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
			// only include dataProvided rows where username, password, and org are valid
			if (!(l.get(1).equals(sm_clientUsername) && l.get(2).equals(sm_clientPassword) && l.get(3)==sm_clientOrg)) continue;
//			if (l.get(12)==nErrMsg) l.set(0,new BlockedByBzBug("838264"));
//DELETEME	if (l.get(12)/*stdout*/==nErrMsg || l.get(13)/*stderr*/==nErrMsg) l.set(0,new BlockedByBzBug(new String[]{"838264","1345962"}));
			if (l.get(12)/*stdout*/==nErrMsg || l.get(13)/*stderr*/==nErrMsg || l.get(13)/*stderr*/==pErr407Msg) l.set(0,new BlockedByBzBug(new String[]{"838264","1345962"}));
			
			ll.add(l);
		}
		return ll;
	}
	
	
	
	@DataProvider(name="getIdentityAttemptsUsingProxyServerData")
	public Object[][] getIdentityAttemptsUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getIdentityAttemptsUsingProxyServerDataAsListOfLists());
	}
	protected List<List<Object>> getIdentityAttemptsUsingProxyServerDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getRegisterAttemptsUsingProxyServerDataAsListOfLists()) {
			// only include dataProvided rows where org is valid
			if (l.get(3).equals(sm_clientOrg)) {
				
				// get the existing BlockedByBzBug
				BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
				List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
				// add more BlockedByBzBug to rows that are expecting a network error
				if (l.get(8)==nErrMsg || l.get(8)==pErr407Msg) {
					bugIds.add("848195");	// Bug 848195 	Error while checking server version: Proxy connection failed: 407 
					bugIds.add("848190");	// Bug 848190 	Error while checking server version: (111, 'Connection refused') 
					bugIds.add("848184");	// Bug 848184 	Error while checking server version: (-2, 'Name or service not known') 
				}
				blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
				
				ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9)}));
			}
		}
		return ll;
	}
	
	
	@DataProvider(name="getIdentityAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getIdentityAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getIdentityAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
	}
	protected List<List<Object>> getIdentityAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
			// only include dataProvided rows where org is valid
			if (l.get(3).equals(sm_clientOrg)) {
				
				// get the existing BlockedByBzBug
				BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
				List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
				// add more BlockedByBzBug to rows that are expecting a network error
				if (l.get(12)==nErrMsg || l.get(12)==pErr407Msg) {
					bugIds.add("848195");	// Bug 848195 	Error while checking server version: Proxy connection failed: 407 
					bugIds.add("848190");	// Bug 848190 	Error while checking server version: (111, 'Connection refused') 
					bugIds.add("848184");	// Bug 848184 	Error while checking server version: (-2, 'Name or service not known') 
				}
				blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
				
				ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16)}));
			}
		}
		return ll;
	}
	
	
	@DataProvider(name="getOrgsAttemptsUsingProxyServerData")
	public Object[][] getOrgsAttemptsUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getOrgsAttemptsUsingProxyServerDataAsListOfLists());
	}
	protected List<List<Object>> getOrgsAttemptsUsingProxyServerDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getRegisterAttemptsUsingProxyServerDataAsListOfLists()) {
			// only include dataProvided rows where org is valid
			if (l.get(3).equals(sm_clientOrg)) ll.add(l);
		}
		return ll;
	}
	
	
	@DataProvider(name="getOrgsAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getOrgsAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getOrgsAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
	}
	protected List<List<Object>> getOrgsAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
			// only include dataProvided rows where org is valid
			if (l.get(3).equals(sm_clientOrg)) ll.add(l);
		}
		return ll;
	}
	
	
	@DataProvider(name="getServiceLevelAttemptsUsingProxyServerData")
	public Object[][] getServiceLevelAttemptsUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getServiceLevelAttemptsUsingProxyServerDataAsListOfLists());
	}
	protected List<List<Object>> getServiceLevelAttemptsUsingProxyServerDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getRegisterAttemptsUsingProxyServerDataAsListOfLists()) {
			// only include dataProvided rows where org is valid
			if (l.get(3).equals(sm_clientOrg)) ll.add(l);
		}
		return ll;
	}
	
	
	@DataProvider(name="getServiceLevelAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getServiceLevelAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getServiceLevelAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
	}
	protected List<List<Object>> getServiceLevelAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
			// only include dataProvided rows where org is valid
			if (l.get(3).equals(sm_clientOrg)) ll.add(l);
		}
		return ll;
	}
	
	
	@DataProvider(name="getEnvironmentsAttemptsUsingProxyServerData")
	public Object[][] getEnvironmentsAttemptsUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getEnvironmentsAttemptsUsingProxyServerDataAsListOfLists());
	}
	protected List<List<Object>> getEnvironmentsAttemptsUsingProxyServerDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getRegisterAttemptsUsingProxyServerDataAsListOfLists()) {
			
			// get the existing BlockedByBzBug
			BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			l.set(0, blockedByBzBug);
			
//DELETEME	if (!sm_serverType.equals("katello") && (!nErrMsg.equals(l.get(9))||l.get(9)==null) && clienttasks.isPackageVersion("subscription-manager",">=","1.13.10-1")) {	// post commit 13fe8ffd8f876d27079b961fb6675424e65b9a10 bug 1119688
			if (!sm_serverType.equals("katello") && ((!nErrMsg.equals(l.get(9))&&!pErrMsg.equals(l.get(9))&&!rErrMsg.equals(l.get(9)))||l.get(9)==null) && clienttasks.isPackageVersion("subscription-manager",">=","1.13.10-1")) {	// post commit 13fe8ffd8f876d27079b961fb6675424e65b9a10 bug 1119688
				l.set(7, Integer.valueOf(69));	// exitCode EX_UNAVAILABLE
				l.set(8,"");
				l.set(9,"Error: Server does not support environments.");
			} else 
//DELETEME	if (!sm_serverType.equals("katello") && !l.get(3).equals(sm_clientOrg)) {
			if (!sm_serverType.equals("katello") && (!l.get(1).equals(sm_clientUsername) || !l.get(2).equals(sm_clientPassword) || !l.get(3).equals(sm_clientOrg))) {
				// subscription-manager environments --username=testuser1 --password=password --org=bad-org
				// Stdout: This system does not support environments.
				// Stderr:
				// ExitCode: 0
				l.set(7, Integer.valueOf(0));	// exitCode
				l.set(8,"Error: Server does not support environments.");
				l.set(9,"");
			}
			ll.add(l);
		}
		return ll;
	}
	
	
	@DataProvider(name="getEnvironmentsAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getEnvironmentsAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getEnvironmentsAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
	}
	protected List<List<Object>> getEnvironmentsAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
			
			// get the existing BlockedByBzBug
			BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			l.set(0, blockedByBzBug);

			if (l.get(11).equals(Integer.valueOf(69))) {	// 69 EX_UNAVAILABLE
				// alter nothing; expected exitCode 69 indicates that "Proxy connection failed, please check your settings."
			} else
//DELETEME	if (!sm_serverType.equals("katello") && (!nErrMsg.equals(l.get(13))||l.get(13)==null) && clienttasks.isPackageVersion("subscription-manager",">=","1.13.10-1")) {	// post commit 13fe8ffd8f876d27079b961fb6675424e65b9a10 bug 1119688
			if (!sm_serverType.equals("katello") && ((!nErrMsg.equals(l.get(13))&&!pErr407Msg.equals(l.get(13))&&!pErrConMsg.equals(l.get(13)))||l.get(13)==null) && clienttasks.isPackageVersion("subscription-manager",">=","1.13.10-1")) {	// post commit 13fe8ffd8f876d27079b961fb6675424e65b9a10 bug 1119688
				l.set(11, Integer.valueOf(69));	// exitCode EX_UNAVAILABLE
				l.set(12,"");
				l.set(13,"Error: Server does not support environments.");
			} else 
//DELETEME	if (!sm_serverType.equals("katello") && !l.get(3).equals(sm_clientOrg)) {
			if (!sm_serverType.equals("katello") && (!l.get(1).equals(sm_clientUsername) || !l.get(2).equals(sm_clientPassword) || !l.get(3).equals(sm_clientOrg))) {
				// subscription-manager environments --username=testuser1 --password=password --org=bad-org --proxy=auto-services.usersys.redhat.com:3128 --proxyuser=redhat --proxypassword=redhat
				// Stdout: This system does not support environments.
				// Stderr:
				// ExitCode: 0
				l.set(11, Integer.valueOf(0));	// exitCode
				l.set(12,"Error: Server does not support environments.");
				l.set(13,"");
			}
			ll.add(l);
		}
		return ll;
	}
	
	
	@DataProvider(name="getUnregisterAttemptsUsingProxyServerData")
	public Object[][] getUnregisterAttemptsUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerDataAsListOfLists());
	}
	
	
	@DataProvider(name="getUnregisterAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getUnregisterAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
	}
	
	
	@DataProvider(name="getListAttemptsUsingProxyServerData")
	public Object[][] getListAttemptsUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerDataAsListOfLists());
	}
	
	
	@DataProvider(name="getListAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getListAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
	}
	
	
	@DataProvider(name="getStatusAttemptsUsingProxyServerData")
	public Object[][] getStatusAttemptsUsingProxyServerDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerDataAsListOfLists()) {			

			BlockedByBzBug blockedByBzBug = null;	// nullify the blockedByBug parameter since this function was originally not blocked by any bug			
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			// add BlockedByBzBug to rows that are expecting a proxy error
			if (l.get(8)/*stdout*/==pErrMsg || l.get(9)/*stderr*/==pErrMsg || l.get(8)/*stdout*/==rErrMsg || l.get(9)/*stderr*/==rErrMsg) {
				bugIds.add("1336551");	// Bug 1336551 - status and version modules are not using cache when bad command line proxy is specified
			}
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			// Note: Because the status module should return cached results when it fails to connect to the server, the exitCode should always be 0 and we'll null out the asserts on stdout and stderr
			if (clienttasks.isPackageVersion("subscription-manager","<","1.18.2-1")) {	// pre-commit ad982c13e79917e082f336255ecc42615e1e7707	1176219: Error out if bad proxy settings detected
				ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	Integer.valueOf(0)/*exitCode*/,null/*stdout*/,null/*stderr*/}));
			} else {	// not anymore...  since CLOSED WONFIX Bug 1336551 - status and version modules are not using cache when bad command line proxy is specified
				ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7)/*exitCode*/,l.get(8)/*stdout*/,l.get(9)/*stderr*/}));
			}
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getStatusAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getStatusAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {

			// Object blockedByBug, String username, String password, Sring org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern
			
			BlockedByBzBug blockedByBzBug = null;	// nullify the blockedByBug parameter since this function was originally not blocked by any bug			
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			if (l.get(13)==pErr407Msg)  bugIds.add("1419197");	// Bug 1419197 - subscription-manager status with bad proxy configurations should be using cache
			if (l.get(13)==pErrConMsg)  bugIds.add("1495286");	// Bug 1495286 - subscription-manager status module is no longer using cache when rhsm.conf is configured with a bad proxy
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			// when subscription-manager can not reach the server the actual status cannot be known; no cache will be used
			// this accepted change in behavior was decided in Bug 1419197 - subscription-manager status with bad proxy configurations should be using cache
			if (l.get(13)==pErr407Msg)  {
				ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11)/*exitCode*/,	l.get(12)/*stdout*/,	l.get(13)/*stderr*/,	l.get(14),	l.get(15),	l.get(16)}));
				continue;
			}

			// when l.get(4)!=null, then a proxy will be specified as a command line arg which trumps the rhsm.conf configuration and NO cache will be used
			if (l.get(4)!=null) {
				// when a proxy is specified on the command line (l.get(4)), no cache will be used, simply assert all of the expected results
				ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11)/*exitCode*/,	l.get(12)/*stdout*/,	l.get(13)/*stderr*/,	l.get(14),	l.get(15),	l.get(16)}));
				continue;
			}
			
			// if we get to here, then...
			// the status module should return cached results when it fails to connect to the server,
			// the exitCode should always be 0 and we'll null out the asserts on stdout and stderr
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	Integer.valueOf(0)/*exitCode*/,null/*stdout*/,null/*stderr*/,	l.get(14),	l.get(15),	l.get(16)}));
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getVersionAttemptsUsingProxyServerData")
	public Object[][] getVersionAttemptsUsingProxyServerDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerDataAsListOfLists()) {			

			BlockedByBzBug blockedByBzBug = null;	// nullify the blockedByBug parameter since this function was originally not blocked by any bug
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			// add BlockedByBzBug to rows that are expecting a proxy error
			if (l.get(8)/*stdout*/==pErrMsg || l.get(9)/*stderr*/==pErrMsg || l.get(8)/*stdout*/==rErrMsg || l.get(9)/*stderr*/==rErrMsg) {
				bugIds.add("1336551");	// Bug 1336551 - status and version modules are not using cache when bad command line proxy is specified
			}
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			// Note: The version module should always succeed, yet report subscription management server: Unknown when connection fails to the server
			if (clienttasks.isPackageVersion("subscription-manager","<","1.18.2-1")) {	// post commit ad982c13e79917e082f336255ecc42615e1e7707	1176219: Error out if bad proxy settings detected
				if (l.get(8)/*stdout*/==nErrMsg || l.get(9)/*stderr*/==nErrMsg) {
					ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	Integer.valueOf(0)/*exitCode*/,"subscription management server: Unknown"/*stdout*/,""/*stderr*/}));
				} else {
					ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	Integer.valueOf(0)/*exitCode*/,"subscription management server: "+servertasks.statusVersion/*stdout*/,""/*stderr*/}));
				}
			} else {	// not anymore...  since CLOSED WONFIX Bug 1336551 - status and version modules are not using cache when bad command line proxy is specified
				if (Integer.valueOf(0).equals(l.get(7))) {	// indicates success
					ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	Integer.valueOf(0)/*exitCode*/,"subscription management server: "+servertasks.statusVersion/*stdout*/,""/*stderr*/}));
				} else {
					ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7)/*exitCode*/,l.get(8)/*stdout*/,l.get(9)/*stderr*/}));
				}
		
			}
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getVersionAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getVersionAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		// Note: The version module should always succeed, yet report subscription management server: Unknown when connection fails to the server
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {

			BlockedByBzBug blockedByBzBug = null;	// nullify the blockedByBug parameter since this function was originally not blocked by any bug
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			if (l.get(13)==pErr407Msg)  bugIds.add("1419197");	// Bug 1419197 - subscription-manager status with bad proxy configurations should be using cache
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			// despite a Network or Proxy error, the version module should succeed with an Unknown server version
			if (l.get(12)/*stdout*/==nErrMsg ||
				l.get(13)/*stderr*/==nErrMsg ||
				l.get(13)/*stderr*/==pErr407Msg ||
				l.get(13)/*stderr*/==pErrConMsg) {
				ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	Integer.valueOf(0)/*exitCode*/,"subscription management server: Unknown"/*stdout*/,""/*stderr*/,	l.get(14),	l.get(15),	l.get(16)}));
				continue;
			}

			// when l.get(4)!=null, then a proxy will be specified as a command line arg which trumps the rhsm.conf configuration and NO cache will be used
			if (l.get(4)!=null) {	// assumes get(4) is a bad proxy cli option
				// when a proxy is specified on the command line (l.get(4)), no cache will be used, simply assert all of the expected results
				ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11)/*exitCode*/,	l.get(12)/*stdout*/,	l.get(13)/*stderr*/,	l.get(14),	l.get(15),	l.get(16)}));
				continue;
			}
			
			// if we get to here, then stdout should report the actual server version...
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	Integer.valueOf(0)/*exitCode*/,"subscription management server: "+servertasks.statusVersion/*stdout*/,""/*stderr*/,	l.get(14),	l.get(15),	l.get(16)}));
			
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getAutoHealAttemptsUsingProxyServerData")
	public Object[][] getAutoHealAttemptsUsingProxyServerDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerDataAsListOfLists()) {
			
			BlockedByBzBug blockedByBzBug = null;	// nullify the blockedByBug parameter since this function was originally not blocked by any bug
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
//			// add BlockedByBzBug to rows that are expecting a network error
//			if (l.get(8)==nErrMsg || l.get(8)==pErr407Msg) {
//				bugIds.add("848195");	// Bug 848195 	Error while checking server version: Proxy connection failed: 407 
//				bugIds.add("848190");	// Bug 848190 	Error while checking server version: (111, 'Connection refused') 
//				bugIds.add("848184");	// Bug 848184 	Error while checking server version: (-2, 'Name or service not known') 
//			}
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9)}));
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getAutoHealAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getAutoHealAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
			
			BlockedByBzBug blockedByBzBug = null;	// nullify the blockedByBug parameter since this function was originally not blocked by any bug
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
//			// add BlockedByBzBug to rows that are expecting a network error
//			if (l.get(12)==nErrMsg || l.get(12)==pErr407Msg) {
//				bugIds.add("848195");	// Bug 848195 	Error while checking server version: Proxy connection failed: 407 
//				bugIds.add("848190");	// Bug 848190 	Error while checking server version: (111, 'Connection refused') 
//				bugIds.add("848184");	// Bug 848184 	Error while checking server version: (-2, 'Name or service not known') 
//			}
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16)}));
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getReleaseAttemptsUsingProxyServerData")
	public Object[][] getReleaseAttemptsUsingProxyServerDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerDataAsListOfLists()) {
			
			BlockedByBzBug blockedByBzBug = null;	// nullify the blockedByBug parameter since this function was originally not blocked by any bug
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			// add BlockedByBzBug to rows that are expecting a network error
			if (l.get(8)==nErrMsg || l.get(8)==pErr407Msg) {
				bugIds.add("848195");	// Bug 848195 	Error while checking server version: Proxy connection failed: 407 
				bugIds.add("848190");	// Bug 848190 	Error while checking server version: (111, 'Connection refused') 
				bugIds.add("848184");	// Bug 848184 	Error while checking server version: (-2, 'Name or service not known') 
			}
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9)}));
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getReleaseAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getReleaseAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
			
			BlockedByBzBug blockedByBzBug = null;	// nullify the blockedByBug parameter since this function was originally not blocked by any bug
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			// add BlockedByBzBug to rows that are expecting a network error
			if (l.get(12)==nErrMsg || l.get(12)==pErr407Msg) {
				bugIds.add("848195");	// Bug 848195 	Error while checking server version: Proxy connection failed: 407 
				bugIds.add("848190");	// Bug 848190 	Error while checking server version: (111, 'Connection refused') 
				bugIds.add("848184");	// Bug 848184 	Error while checking server version: (-2, 'Name or service not known') 
			}
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16)}));
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getRedeemAttemptsUsingProxyServerData")
	public Object[][] getRedeemAttemptsUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRedeemAttemptsUsingProxyServerDataAsListOfLists());
	}
	protected List<List<Object>> getRedeemAttemptsUsingProxyServerDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerDataAsListOfLists()) {
			
			// Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr
			
			// alter expected exitCode and stderr for rows that are normally exitCode=0 against standalone candlepin >= 2.0.7-1
			if (CandlepinType.standalone.equals(sm_serverType)) {
				if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.7-1")) {	// candlepin commit 676ce6c2786203a33ec5eedc8dadcd664a62f09e 1263474: Standalone candlepin now returns the expected error message and code
					if (((Integer)l.get(7)).equals(Integer.valueOf(0))) {	// exit code
						// get the existing BlockedByBzBug 
						BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
						List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
						bugIds.add("1263474");	// Bug 1263474 - subscription-manager redeem is not reporting the response to stdout
						bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
						blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
						
						ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	new Integer(70)/*EX_SOFTWARE*/,	l.get(8),	new String("Standalone candlepin does not support redeeming a subscription.")}));
						continue;
					}
				}
			}
			
			// only block rows where stdout != null with BlockedByBzBug("732499")
			if (l.get(8)!=null) {
				// get the existing BlockedByBzBug 
				BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
				List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
				bugIds.add("732499");	// Bug 732499 - 'gaierror' object has no attribute 'code' / 'error' object has no attribute 'code'
				bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
				blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
				
				ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9)}));
			} else {
				ll.add(l);
			}
		}
		return ll;
	}

	
	@DataProvider(name="getRedeemAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getRedeemAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRedeemAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
	}
	protected List<List<Object>> getRedeemAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
			
			// Object blockedByBug, String username, String password, Sring org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern
			
			// alter expected exitCode and stderr for rows that are normally exitCode=0 against standalone candlepin >= 2.0.7-1
			if (CandlepinType.standalone.equals(sm_serverType)) {
				if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.7-1")) {	// candlepin commit 676ce6c2786203a33ec5eedc8dadcd664a62f09e 1263474: Standalone candlepin now returns the expected error message and code
					if (((Integer)l.get(11)).equals(Integer.valueOf(0))) {	// exit code
						// get the existing BlockedByBzBug 
						BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
						List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
						// add Bug 1263474 - subscription-manager redeem is not reporting the response to stdout
						bugIds.add("1263474");
						blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
						
						String expectedStderr = "Standalone candlepin does not support redeeming a subscription.";
						if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-1")) {	// post commit 630e1a2eb06e6bfacac669ce11f38e228c907ea9 1507030: RestlibExceptions should show they originate server-side
							expectedStderr = "HTTP error code 503: "+expectedStderr;	// HTTP error (401 - Unauthorized): Invalid Credentials
						}
						ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	new Integer(70)/*EX_SOFTWARE*/,	l.get(12),	expectedStderr,	l.get(14),	l.get(15),	l.get(16)}));
						continue;
					}
				}
			}
			
			// only block rows where stdout != null with BlockedByBzBug("732499")
			if (l.get(12)!=null) {
				// get the existing BlockedByBzBug 
				BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
				List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
				// add Bug 732499 - 'gaierror' object has no attribute 'code' / 'error' object has no attribute 'code'
				bugIds.add("732499");
				blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
				
				ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16)}));
			} else {
				ll.add(l);
			}
		}
		return ll;
	}
	
	
	@DataProvider(name="getFactsAttemptsUsingProxyServerData")
	public Object[][] getFactsAttemptsUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getFactsAttemptsUsingProxyServerDataAsListOfLists());
	}
	protected List<List<Object>> getFactsAttemptsUsingProxyServerDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerDataAsListOfLists()) {
//			if (l.get(8)!=null) {
			if (l.get(8)/*stdout*/==nErrMsg || l.get(9)/*stderr*/==nErrMsg) {
//				ll.add(Arrays.asList(new Object[]{	l.get(0),	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	null,	"Error updating system data, see /var/log/rhsm/rhsm.log for more details."}));
				ll.add(Arrays.asList(new Object[]{	l.get(0),	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	null,	"Error updating system data on the server, see /var/log/rhsm/rhsm.log for more details."}));
			} else {
				ll.add(l);
			}
		}
		return ll;
	}
	
	@DataProvider(name="getFactsAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getFactsAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getFactsAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
	}
	protected List<List<Object>> getFactsAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
			if (l.get(12)/*stdout*/==nErrMsg || l.get(13)/*stderr*/==nErrMsg || l.get(13)/*stderr*/==pErr407Msg || l.get(13)/*stderr*/==pErrConMsg) {
//OLD				ll.add(Arrays.asList(new Object[]{	l.get(0),	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	null,	"Error updating system data, see /var/log/rhsm/rhsm.log for more details.",	l.get(14),	l.get(15),	l.get(16)}));
				ll.add(Arrays.asList(new Object[]{	l.get(0),	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	null,	"Error updating system data on the server, see /var/log/rhsm/rhsm.log for more details.",	l.get(14),	l.get(15),	l.get(16)}));
			} else {
				ll.add(l);
			}
		}
		return ll;
	}
	
	@DataProvider(name="getRefreshAttemptsUsingProxyServerData")
	public Object[][] getRefreshAttemptsUsingProxyServerDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerDataAsListOfLists()) {
			
			// get the existing BlockedByBzBug
			BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			// add more BlockedByBzBug to rows that are expecting a network error
			if (l.get(8)==nErrMsg) {
				bugIds.add("975164");	// Bug 975164 - subscription-manager refresh with --proxy is silently failing in rhsm.log
				bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			}
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9)}));
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getRefreshAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getRefreshAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
			
			// get the existing BlockedByBzBug
			BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			// add more BlockedByBzBug to rows that are expecting a network error
			if (l.get(12)==nErrMsg) {
				bugIds.add("975164");	// Bug 975164 - subscription-manager refresh with --proxy is silently failing in rhsm.log
				bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			}
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16)}));
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getReposAttemptsUsingProxyServerData")
	public Object[][] getReposAttemptsUsingProxyServerDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerDataAsListOfLists()) {
			
			// get the existing BlockedByBzBug
			BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			// add more BlockedByBzBug to rows that are expecting a network error
			if (l.get(8)==nErrMsg) {
				bugIds.add("919255");	// Bug 919255 - negative proxy testing against subscription-manager repos --list 
			}
			if (l.get(4)!=null && l.get(4).equals("bad-proxy")) {
				bugIds.add("975186");	// Bug 975186 - subscription-manager repos --list is failing when specifying a bad --proxy
			}
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9)}));
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getReposAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getReposAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
			
			// Object blockedByBug, String username, String password, Sring org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern
			
			// get the existing BlockedByBzBug
			BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			// add more BlockedByBzBug to rows that are expecting a network error
			if (l.get(12)==nErrMsg) {
				bugIds.add("919255");	// Bug 919255 - negative proxy testing against subscription-manager repos --list 
			}
			if (l.get(4)==null && l.get(7)!=null && l.get(7).equals("bad-proxy")) {
				bugIds.add("975186");	// Bug 975186 - subscription-manager repos --list is failing when specifying a bad --proxy
			}
			if (l.get(4)!=null && l.get(4).equals("bad-proxy") && l.get(7)!=null && l.get(7).equals(sm_noauthproxyHostname)) {
				bugIds.add("975186");	// Bug 975186 - subscription-manager repos --list is failing when specifying a bad --proxy
			}
			
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16)}));
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getRepoOverrideAttemptsUsingProxyServerData")
	public Object[][] getRepoOverrideAttemptsUsingProxyServerDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerDataAsListOfLists()) {
			
			// get the existing BlockedByBzBug
			BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			// add more BlockedByBzBug to rows that are....
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			blockedByBzBug = null;	// nullify the blockedByBug parameter since this function was originally not blocked by any bug
			bugIds.clear();
			bugIds.add("1034396");	// Bug 1034396 - repo-override command should not require entitlements
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			l.set(0, blockedByBzBug);
			
			// Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr
			
			if (l.get(7).equals(new Integer(0))) {
				/* valid prior to bug 1034396 
				l.set(7,new Integer(1));
				l.set(8,"This system does not have any subscriptions.");
				l.set(9,"");
				*/
				l.set(7,new Integer(0));
				l.set(8,"This system does not have any content overrides applied to it.");
				l.set(9,"");
			}
			ll.add(l);
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getRepoOverrideAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getRepoOverrideAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
			
			// get the existing BlockedByBzBug
			BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			// add more BlockedByBzBug to rows that are....
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			blockedByBzBug = null;	// nullify the blockedByBug parameter since this function was originally not blocked by any bug
			bugIds.clear();
			bugIds.add("1034396");	// Bug 1034396 - repo-override command should not require entitlements
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			l.set(0, blockedByBzBug);
			
			// Object blockedByBug, String username, String password, Sring org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern
			
			if (l.get(11).equals(new Integer(0))) {
				/* valid prior to bug 1034396 
				l.set(11,new Integer(1));
				l.set(12,"This system does not have any subscriptions.");
				l.set(13,"");
				*/
				l.set(11,new Integer(0));
				l.set(12,"This system does not have any content overrides applied to it.");
				l.set(13,"");
			}
			ll.add(l);
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getSubscribeAttemptsUsingProxyServerData")
	public Object[][] getSubscribeAttemptsUsingProxyServerDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerDataAsListOfLists());
		return TestNGUtils.convertListOfListsTo2dArray(getSubscribeAttemptsUsingProxyServerDataAsListOfLists());
	}
	protected List<List<Object>> getSubscribeAttemptsUsingProxyServerDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerDataAsListOfLists()) {
			if (l.get(8)==nErrMsg) l.set(0,new BlockedByBzBug("869046"));
			ll.add(l);
		}
		return ll;
	}
	
	
	@DataProvider(name="getSubscribeAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getSubscribeAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
		return TestNGUtils.convertListOfListsTo2dArray(getSubscribeAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
	}
	protected List<List<Object>> getSubscribeAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
			if (l.get(12)!=null) {
				if (l.get(12)==nErrMsg) l.set(0,new BlockedByBzBug("869046"));
				ll.add(l);
			}
		}
		return ll;
	}
	
	@DataProvider(name="getUnsubscribeAttemptsUsingProxyServerData")
	public Object[][] getUnsubscribeAttemptsUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerDataAsListOfLists());
	}
	
	
	@DataProvider(name="getUnsubscribeAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getUnsubscribeAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
	}
	
	
	@DataProvider(name="getRhsmDebugSystemAttemptsUsingProxyServerData")
	public Object[][] getRhsmDebugSystemAttemptsUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerDataAsListOfLists());
	}
	
	
	@DataProvider(name="getRhsmDebugSystemAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getRhsmDebugSystemAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
	}
	
	

}
