package rhsm.cli.tests;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
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

import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.github.redhatqe.polarize.metadata.TestDefinition;

/**
 * @author jsefler
 *
 */
@Test(groups={"ProxyTests","Tier3Tests"})
public class ProxyTests extends SubscriptionManagerCLITestScript {


	// Test methods ***********************************************************************
	
	
	// REGISTER Test methods ***********************************************************************

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21837", "RHEL7-51656"})
	@Test(	description="subscription-manager : register using a proxy server (Positive and Negative Variations)",
			groups={"AcceptanceTests","Tier1Tests"},
			dataProvider="getRegisterAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void RegisterAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		String moduleTask = "register";
		
		SSHCommandResult attemptResult = clienttasks.register_(username, password, org, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21819", "RHEL7-51638"})
	@Test(	description="subscription-manager : register using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={},
			dataProvider="getRegisterAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RegisterAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		String moduleTask = "register";

		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt to register
		SSHCommandResult attemptResult = clienttasks.register_(username, password, org, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21837", "RHEL7-51656"})
	@Test(	description="subscription-manager : unregister using a proxy server (Positive and Negative Variations)",
			groups={},
			dataProvider="getUnregisterAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void UnregisterAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "unregister";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.unregister_(proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21836", "RHEL7-51655"})
	@Test(	description="subscription-manager : unregister using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={},
			dataProvider="getUnregisterAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void UnregisterAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "unregister";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);

		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.unregister_(proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21811", "RHEL7-51628"})
	@Test(	description="subscription-manager : identity using a proxy server (Positive and Negative Variations)",
			groups={},
			dataProvider="getIdentityAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void IdentityAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "identity";
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.identity_(username, password, Boolean.TRUE, Boolean.TRUE, proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21810", "RHEL7-51627"})
	@Test(	description="subscription-manager : identity using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={},
			dataProvider="getIdentityAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void IdentityAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "identity";
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.identity_(username, password, Boolean.TRUE, Boolean.TRUE, proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21815", "RHEL7-51632"})
	@Test(	description="subscription-manager : orgs using a proxy server (Positive and Negative Variations)",
			groups={},
			dataProvider="getOrgsAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void OrgsAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "orgs";
		
		SSHCommandResult attemptResult = clienttasks.orgs_(username, password, null, null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21814", "RHEL7-51631"})
	@Test(	description="subscription-manager : orgs using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={},
			dataProvider="getOrgsAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void OrgsAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "orgs";
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.orgs_(username, password, null, null, proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21832", "RHEL7-51651"})
	@Test(	description="subscription-manager : service-level using a proxy server (Positive and Negative Variations)",
			groups={},
			dataProvider="getServiceLevelAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void ServiceLevelAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "service-level";
		
		SSHCommandResult attemptResult = clienttasks.service_level_(null, true, null, null, username, password, org, null, null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21831", "RHEL7-51650"})
	@Test(	description="subscription-manager : service-level using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={},
			dataProvider="getServiceLevelAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void ServiceLevelAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "service-level";
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.service_level_(null, true, null, null, username, password, org, null, null, proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21807", "RHEL7-51624"})
	@Test(	description="subscription-manager : environments using a proxy server (Positive and Negative Variations)",
			groups={"blockedByBug-728380","blockedByBug-1254578"/*is a duplicate of*/,"blockedByBug-1254349"},
			dataProvider="getEnvironmentsAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void EnvironmentsAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "environments";
		
		SSHCommandResult attemptResult = clienttasks.environments_(username, password, org, null, null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21806", "RHEL7-51623"})
	@Test(	description="subscription-manager : environments using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"blockedByBug-728380","blockedByBug-1254578"/*is a duplicate of*/,"blockedByBug-1254349"},
			dataProvider="getEnvironmentsAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void EnvironmentsAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "environments";
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.environments_(username, password, org, null, null, proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21813", "RHEL7-51630"})
	@Test(	description="subscription-manager : list using a proxy server (Positive and Negative Variations)",
			groups={},
			dataProvider="getListAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void ListAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "list";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.list_(null,Boolean.TRUE,null,null,null, null, null, null, null, null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21823", "RHEL7-51642"})
	@Test(	description="subscription-manager : list using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={},
			dataProvider="getListAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void ListAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "list";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.list_(null,Boolean.TRUE,null,null,null, null, null, null, null, null, proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21822", "RHEL7-51641"})
	@Test(	description="subscription-manager : release using a proxy server (Positive and Negative Variations)",
			groups={},
			dataProvider="getReleaseAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void ReleaseAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "release";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.release_(null, null, null, null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21821", "RHEL7-51640"})
	@Test(	description="subscription-manager : release using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={},
			dataProvider="getReleaseAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void ReleaseAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "release";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.release_(null, null, null, null, proxy, proxyuser, proxypassword);
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



	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21824", "RHEL7-51643"})
	@Test(	description="subscription-manager : release --list using a proxy server (Positive and Negative Variations)",
			groups={"blockedByBug-822965","blockedByBug-824530"},
			dataProvider="getReleaseAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void ReleaseListAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "release --list";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, null, false, null, null, null);
		
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
		
		SSHCommandResult attemptResult = clienttasks.release_(null, true, null, null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21823", "RHEL7-51642"})
	@Test(	description="subscription-manager : release using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"blockedByBug-822965","blockedByBug-824530"},
			dataProvider="getReleaseAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void ReleaseListAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "release --list";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, null, false, null, null, null);
	
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
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.release_(null, true, null, null, proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21805", "RHEL7-51622"})
	@Test(	description="subscription-manager : auto-heal using a proxy server (Positive and Negative Variations)",
			groups={},
			dataProvider="getAutoHealAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void AutoHealAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "autoheal";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.autoheal_(null, null, true, proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21804", "RHEL7-51621"})
	@Test(	description="subscription-manager : auto-heal using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={},
			dataProvider="getAutoHealAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void AutoHealAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "autoheal";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.autoheal_(null, null, true, proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21834", "RHEL7-51653"})
	@Test(	description="subscription-manager : status using a proxy server (Positive and Negative Variations)",
			groups={"blockedByBug-977481"},
			dataProvider="getStatusAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void StatusAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "status";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null);
		
		// NOTE: Because the status module should return a cached status report when connectivity has been interrupted, this call should always pass
		SSHCommandResult attemptResult = clienttasks.status/*_*/(null, proxy, proxyuser, proxypassword);
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21833", "RHEL7-51652"})
	@Test(	description="subscription-manager : status using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"blockedByBug-977481"},
			dataProvider="getStatusAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void StatusAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "status";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		// NOTE: Because the status module should return a cached status report when connectivity has been interrupted, this call should always pass
		SSHCommandResult attemptResult = clienttasks.status/*_*/(null, proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-25993", "RHEL7-51660"})
	@Test(	description="subscription-manager : version using a proxy server (Positive and Negative Variations)",
			groups={"blockedByBug-977481","blockedByBug-1284120"},
			dataProvider="getVersionAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void VersionAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "version";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		
		// NOTE: Because the status module should return a cached status report when connectivity has been interrupted, this call should always pass
		SSHCommandResult attemptResult = clienttasks.version/*_*/(proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertTrue(attemptResult.getStdout().contains(stdout), "The stdout from an attempt to "+moduleTask+" using a proxy server contains expected report '"+stdout+"'.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-25992", "RHEL7-51659"})
	@Test(	description="subscription-manager : status using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"blockedByBug-977481","blockedByBug-1284120"},
			dataProvider="getVersionAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void VersionAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "version";
		
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		// NOTE: Because the status module should return a cached status report when connectivity has been interrupted, this call should always pass
		SSHCommandResult attemptResult = clienttasks.version/*_*/(proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-26755", "RHEL7-51633"})
	@Test(	description="subscription-manager : redeem using a proxy server (Positive and Negative Variations)",
			groups={"ProxyRedeemTests"},
			dataProvider="getRedeemAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RedeemAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "redeem";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.redeem_("proxytester@redhat.com","en-us",null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-26754", "RHEL7-55662"})
	@Test(	description="subscription-manager : redeem using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"ProxyRedeemTests"},
			dataProvider="getRedeemAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RedeemAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "redeem";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.redeem_("proxytester@redhat.com","en-us",null, proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21809", "RHEL7-51626"})
	@Test(	description="subscription-manager : facts using a proxy server (Positive and Negative Variations)",
			groups={},
			dataProvider="getFactsAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void FactsAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "facts";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.facts_(null,Boolean.TRUE,proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21808", "RHEL7-51625"})
	@Test(	description="subscription-manager : facts using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={},
			dataProvider="getFactsAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void FactsAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "facts";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.facts_(null,Boolean.TRUE,proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21817", "RHEL7-51635"})
	@Test(	description="subscription-manager : refresh using a proxy server (Positive and Negative Variations)",
			groups={"blockedByBug-664548"},
			dataProvider="getRefreshAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RefreshAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "refresh";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.refresh_(proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21816", "RHEL7-51634"})
	@Test(	description="subscription-manager : refresh using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"blockedByBug-664548"},
			dataProvider="getRefreshAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RefreshAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "refresh";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.refresh_(proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21828", "RHEL7-51647"})
	@Test(	description="subscription-manager : repos using a proxy server (Positive and Negative Variations)",
			groups={"blockedByBug-906642","blockedByBug-909778"},
			dataProvider="getReposAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void ReposAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "repos";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribe(null,null,pool.poolId,null,null,null, null, null, null, null, null, null);
		
		// TEMPORARY WORKAROUND FOR BUG 1176219 - subscription-manager repos --list with bad proxy options is silently using cache
		String bugId = "1176219"; boolean invokeWorkaroundWhileBugIsOpen = true;
		if (nErrMsg.equals(stderr)) {
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Deleting cache '"+clienttasks.rhsmCacheDir+"', and will assert a slightly different stderr while bug '"+bugId+"' is open.");
				stderr = "Error updating system data on the server, see /var/log/rhsm/rhsm.log for more details.";
				client.runCommandAndWait("rm -rf "+clienttasks.rhsmCacheDir+"/*");
			}
		}
		// END OF WORKAROUND
		
		SSHCommandResult attemptResult = clienttasks.repos_(true,null,null,(List<String>)null, (List<String>)null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" --list using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" --list using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" --list using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21827", "RHEL7-51646"})
	@Test(	description="subscription-manager : repos using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"blockedByBug-906642","blockedByBug-909778"},
			dataProvider="getReposAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void ReposAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "repos";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
//FIXME THIS COULD LEAD TO Stdout: This system has no repositories available through subscriptions.
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribe(null,null,pool.poolId,null,null,null, null, null, null, null, null, null);
//FIXME CHANGE TO GET A RANDOM SUB THAT MATCHES INSTALLED
	
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" ReposAttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);

		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// TEMPORARY WORKAROUND FOR BUG 1176219 - subscription-manager repos --list with bad proxy options is silently using cache
		String bugId = "1176219"; boolean invokeWorkaroundWhileBugIsOpen = true;
		if (nErrMsg.equals(stderr)) {
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Deleting cache '"+clienttasks.rhsmCacheDir+"', and will assert a slightly different stderr while bug '"+bugId+"' is open.");
				stderr = "Error updating system data on the server, see /var/log/rhsm/rhsm.log for more details.";
				client.runCommandAndWait("rm -rf "+clienttasks.rhsmCacheDir+"/*");
			}
		}
		// END OF WORKAROUND
		
		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.repos_(true,null,null,(List<String>)null, (List<String>)null, proxy, proxyuser, proxypassword);
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
	
	
	
	// REPO-OVERRIDE Test methods ***********************************************************************

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21826", "RHEL7-51645"})
	@Test(	description="subscription-manager : repo-override --list using a proxy server (Positive and Negative Variations)",
			groups={},
			dataProvider="getRepoOverrideAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RepoOverrideAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "repo-override";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
//		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
//		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
//		clienttasks.subscribe(null,null,pool.poolId,null,null,null, null, null, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.repo_override_(true,null,(List<String>)null,(List<String>)null,null,proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" --list using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" --list using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" --list using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21825", "RHEL7-51644"})
	@Test(	description="subscription-manager : repo-override --list using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={},
			dataProvider="getRepoOverrideAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RepoOverrideAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "repo-override";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
//		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
//		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
//		clienttasks.subscribe(null,null,pool.poolId,null,null,null, null, null, null, null, null);
		
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" ReposAttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.repo_override_(true,null,(List<String>)null,(List<String>)null,null,proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21835", "RHEL7-51654"})
	@Test(	description="subscription-manager : subscribe using a proxy server (Positive and Negative Variations)",
			groups={"blockedByBug-664603"},
			dataProvider="getSubscribeAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void SubscribeAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "subscribe";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool

		SSHCommandResult attemptResult = clienttasks.subscribe_(null,null,pool.poolId,null,null,null, null, null, null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21838", "RHEL7-51657"})
	@Test(	description="subscription-manager : subscribe using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"blockedByBug-664603"},
			dataProvider="getSubscribeAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void SubscribeAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "subscribe";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);

		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.subscribe_(null,null,pool.poolId,null,null,null, null, null, null, proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21839", "RHEL7-51658"})
	@Test(	description="subscription-manager : unsubscribe using a proxy server (Positive and Negative Variations)",
			groups={"blockedByBug-664603"},
			dataProvider="getUnsubscribeAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void UnsubscribeAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "unsubscribe";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
		
		SSHCommandResult attemptResult = clienttasks.unsubscribe_(true, (BigInteger)null,null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)	Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdout!=null)	Assert.assertEquals(attemptResult.getStdout().trim(), stdout, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderr!=null)	Assert.assertEquals(attemptResult.getStderr().trim(), stderr, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21838", "RHEL7-51657"})
	@Test(	description="subscription-manager : subscribe using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"blockedByBug-664603"},
			dataProvider="getUnsubscribeAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void UnsubscribeAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "unsubscribe";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
		
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());

		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = clienttasks.unsubscribe_(true, (BigInteger)null,null, proxy, proxyuser, proxypassword);
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21830", "RHEL7-51649"})
	@Test(	description="rhsm-debug : system using a proxy server (Positive and Negative Variations)",
			groups={"blockedByBug-1070737","blockedByBug-1039653","blockedByBug-1093382"},
			dataProvider="getRhsmDebugSystemAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RhsmDebugSystemAttemptsUsingProxyServer_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdout, String stderr) {
		// setup for test
		String moduleTask = "rhsm-debug system";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null);
		
		SSHCommandResult attemptResult = client.runCommandAndWait(clienttasks.rhsmDebugSystemCommand(null, null, null, null, null, proxy, proxyuser, proxypassword));
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21829", "RHEL7-51648"})
	@Test(	description="rhsm-debug : system using a proxy server after setting rhsm.config parameters (Positive and Negative Variations)",
			groups={"blockedByBug-1070737","blockedByBug-1039653","blockedByBug-1093382"},
			dataProvider="getRhsmDebugSystemAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RhsmDebugSystemAttemptsUsingProxyServerViaRhsmConfig_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
		// setup for test
		String moduleTask = "rhsm-debug system";
		if (!username.equals(sm_clientUsername) || !password.equals(sm_clientPassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null);
		
		// pad the tail of basicauthproxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test from "+clienttasks.hostname+"...";
		//RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo '"+proxyLogMarker+"'  >> "+proxyLog, Integer.valueOf(0));
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// attempt the moduleTask with the proxy options
		SSHCommandResult attemptResult = client.runCommandAndWait(clienttasks.rhsmDebugSystemCommand(null, null, null, null, null, proxy, proxyuser, proxypassword));
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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21820", "RHEL7-51639"})
	@Test(	description="subscription-manager :  register with proxy configurations commented out of rhsm.conf",
			groups={},
			dataProvider="getRegisterWithProxyConfigurationsCommentedOutOfRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RegisterWithProxyConfigurationsCommentedOutOfRhsmConfig_Test(Object meta, String[] proxyConfigs) {
		
		// comment out each of the config proxy parameters
		for (String proxyConfig : proxyConfigs) clienttasks.commentConfFileParameter(clienttasks.rhsmConfFile, proxyConfig);
		
		log.info("Following are the current proxy parameters configured in config file: "+clienttasks.rhsmConfFile);
		RemoteFileTasks.runCommandAndWait(client, "grep proxy_ "+clienttasks.rhsmConfFile, TestRecords.action());
		
		log.info("Attempt to register with the above proxy config parameters configured (expecting success)...");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
	}


	@TestDefinition( projectID = {Project.RHEL6}
			       , testCaseID = {"RHEL6-26756"})
	@Test(	description="subscription-manager : repos list with proxy set to a real server that is not truely a proxy (e.g. www.redhat.com)",
			groups={"blockedByBug-968820","blockedByBug-1301215","blockedByBug-1345962"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void ReposListWithProxyTimeoutBug968820_Test() {
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);

		// subscribe
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) log.warning("Cound not find an available pool.");
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size()));	// randomly pick a pool
		//clienttasks.subscribeToSubscriptionPool(pool);
		clienttasks.subscribe(null,null,pool.poolId,null,null,null,null,null,null,null,null, null);
		
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21818", "RHEL7-51637"})
	@Test(	description="subscription-manager : register using a proxy server defined by an environment variable (Positive and Negative Variations)",
			groups={"blockedByBug-1031755"},
			dataProvider="getRegisterAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RegisterAttemptsUsingProxyServerDefinedByAnEnvironmentVariable_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern) {
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
		updateConfFileProxyParameters("", "", "", "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// pad the tail of proxyLog with a message
		proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerDefinedByAnEnvironmentVariable_Test from "+clienttasks.hostname+"...";
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// attempt to register using a proxy server defined by an environment variable (no CLI option nor rhsm.conf [sever] proxy configurations set)
		attemptResult = client.runCommandAndWait(httpProxyEnvVar+" "+clienttasks.registerCommand(username, password, org, null, null, null, null, null, null, null, (List<String>)null, null, null, null, null, null, null, null, null));
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
		clienttasks.unregister_(null, null, null);
		
		
		// TEST PART 2: now let's assert that setting the proxy via CLI option or rhsm.conf take precedence over the environment variable
		
		// assemble the value of a httpProxyVar that will get overridden
		httpProxyEnvVar = validHttpProxyEnvVars.get(randomGenerator.nextInt(validHttpProxyEnvVars.size())) + "=https://";
		httpProxyEnvVar += "proxy.example.com:911";	// provided in https://bugzilla.redhat.com/show_bug.cgi?id=1031755#c0	Note: this does not have to be a working proxy to make this a valid test
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// pad the tail of proxyLog with a message
		proxyLogMarker = System.currentTimeMillis()+" Testing "+moduleTask+" AttemptsUsingProxyServerDefinedByAnEnvironmentVariable_Test from "+clienttasks.hostname+"...";
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// attempt to register using a proxy server defined by an environment variable (no CLI option nor rhsm.conf [sever] proxy configurations set)
		attemptResult = client.runCommandAndWait(httpProxyEnvVar+" "+clienttasks.registerCommand(username, password, org, null, null, null, null, null, null, null, (List<String>)null, null, null, null, null, null, proxy, proxyuser, proxypassword));
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





	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-22311", "RHEL7-51636"})
	@Test(	description="subscription-manager : register when no_proxy environment variable matches our hostname regardless of proxy configurations and environment variable (Positive and Negative Variations)",
			groups={"blockedByBug-1266608","blockedByBug-1285010"},
			dataProvider="getRegisterAttemptsToVerifyHonoringNoProxyEnvironmentVariableData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RegisterAttemptsToVerifyHonoringNoProxyEnvironmentVariable_Test(Object blockedByBug, String username, String password, String org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern, String noProxyEnvVar, Boolean hostnameMatchesNoProxyEnvVar) {
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
		
		// randomly test using one of these valid environment variables 
		List<String> validHttpProxyEnvVars = Arrays.asList(new String[]{"HTTPS_PROXY","https_proxy","HTTP_PROXY","http_proxy"});
		List<String> validHttpNoProxyEnvVars = Arrays.asList(new String[]{"NO_PROXY","no_proxy"/*,"no_PROXY" not supported by curl*/});
		
		
		// TEST PART 1: let's assert that setting the proxy via an environment variable...
		//           A: is ignored when hostname matches no_proxy
		//           B: is NOT ignored when hostname does not matches no_proxy

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
		updateConfFileProxyParameters("", "", "", "");
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// pad the tail of proxyLog with a message
		proxyLogMarker = System.currentTimeMillis()+" Testing 1 "+moduleTask+" AttemptsToVerifyHonoringNoProxyEnvironmentVariable_Test from "+clienttasks.hostname+"...";
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// attempt to register using a non-matching no_proxy environment variable and a proxy server defined by an environment variable (no CLI option nor rhsm.conf [sever] proxy configurations set)
		attemptResult = client.runCommandAndWait(noProxyEnvVar+" "+httpProxyEnvVar+" "+clienttasks.registerCommand(username, password, org, null, null, null, null, null, null, null, (List<String>)null, null, null, null, true, null, null, null, null));
		if (hostnameMatchesNoProxyEnvVar) {	// A: is ignored when hostname matches no_proxy
			Assert.assertEquals(attemptResult.getExitCode(), Integer.valueOf(0), "The exit code from an attempt to "+moduleTask+" using a matching no_proxy environment variable '"+noProxyEnvVar+"'.");
			Assert.assertEquals(attemptResult.getStderr().trim(), "", "The stderr from an attempt to "+moduleTask+" using a matching no_proxy environment variable '"+noProxyEnvVar+"'.");
		
			if (proxyLogGrepPattern!=null) {
				String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
				Assert.assertTrue(proxyLogResult.isEmpty(), "The tail of proxy server log '"+proxyLog+"' following marker '"+proxyLogMarker+"' contains NO connection '"+proxyLogGrepPattern+"' attempts from "+ipv4_address+" to the candlepin server.");
			}
		} else {	// B: is NOT ignored when hostname does not matches no_proxy
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
		}
		
		
		// TEST PART 2: now let's assert that setting the proxy via CLI option or rhsm.conf take precedence over the environment variable...
		//           A: regardless if hostname matches no_proxy
		//           B: regardless if hostname does not matches no_proxy

		// assemble the value of a httpProxyVar that will get overridden
		httpProxyEnvVar = validHttpProxyEnvVars.get(randomGenerator.nextInt(validHttpProxyEnvVars.size())) + "=https://";
		httpProxyEnvVar += "proxy.example.com:911";	// provided in https://bugzilla.redhat.com/show_bug.cgi?id=1031755#c0	Note: this does not have to be a working proxy to make this a valid test
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		RemoteFileTasks.runCommandAndWait(client,"grep proxy "+clienttasks.rhsmConfFile,TestRecords.action());
		
		// pad the tail of proxyLog with a message
		proxyLogMarker = System.currentTimeMillis()+" Testing 2 "+moduleTask+" AttemptsToVerifyHonoringNoProxyEnvironmentVariable_Test from "+clienttasks.hostname+"...";
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// attempt to register using a proxy server defined by an environment variable (no CLI option nor rhsm.conf [sever] proxy configurations set)
		attemptResult = client.runCommandAndWait(noProxyEnvVar+" "+httpProxyEnvVar+" "+clienttasks.registerCommand(username, password, org, null, null, null, null, null, null, null, (List<String>)null, null, null, null, true, null, proxy, proxyuser, proxypassword));
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
	}
	@DataProvider(name="getRegisterAttemptsToVerifyHonoringNoProxyEnvironmentVariableData")
	public Object[][] getRegisterAttemptsToVerifyHonoringNoProxyEnvironmentVariableDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRegisterAttemptsToVerifyHonoringNoProxyEnvironmentVariableDataAsListOfLists());
	}
	protected List<List<Object>> getRegisterAttemptsToVerifyHonoringNoProxyEnvironmentVariableDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		// TOO EXHAUSTIVE for (List<Object> l : getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {
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
			
			noProxyEnvVar = "*"; hostnameMatchesNoProxyEnvVar=true;	// * matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxyEnvVar, hostnameMatchesNoProxyEnvVar}));

			noProxyEnvVar = sm_serverHostname; hostnameMatchesNoProxyEnvVar=true;	// subscription.rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxyEnvVar, hostnameMatchesNoProxyEnvVar}));

			noProxyEnvVar = "*.aventail.com,home.com,.seanet.com,"+sm_serverHostname; hostnameMatchesNoProxyEnvVar=true;	// *.aventail.com,home.com,.seanet.com,subscription.rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxyEnvVar, hostnameMatchesNoProxyEnvVar}));

			noProxyEnvVar = sm_serverHostname.replaceFirst("[^\\.]+", ""); hostnameMatchesNoProxyEnvVar=true;	// .rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxyEnvVar, hostnameMatchesNoProxyEnvVar}));
			
			/* TODO This case does not work on RHEL since the the only wildcard supported by the library is no_proxy=*
			 * I believe this is an RFE bug against component X?
			noProxyEnvVar = sm_serverHostname.replaceFirst("[^\\.]+", "*"); hostnameMatchesNoProxyEnvVar=true;	// *.rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxyEnvVar, hostnameMatchesNoProxyEnvVar}));
			*/
			
			noProxyEnvVar = "*.aventail.com,home.com,.seanet.com,"+sm_serverHostname.replaceFirst("[^\\.]+", ""); hostnameMatchesNoProxyEnvVar=true;	// *.aventail.com,home.com,.seanet.com,.rhn.redhat.com matches subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxyEnvVar, hostnameMatchesNoProxyEnvVar}));

			noProxyEnvVar = "*.aventail.com,home.com,.seanet.com"; hostnameMatchesNoProxyEnvVar=false;	// *.aventail.com,home.com,.seanet.com does not match subscription.rhn.redhat.com
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	l.get(12),	l.get(13),	l.get(14),	l.get(15),	l.get(16), noProxyEnvVar, hostnameMatchesNoProxyEnvVar}));
			
		}
		return ll;
	}
	
	
	
	
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 744504 - [ALL LANG] [RHSM CLI] facts module - Run facts update with incorrect proxy url produces traceback. https://github.com/RedHatQE/rhsm-qe/issues/179
	
	
	// Configuration methods ***********************************************************************
	
	public static SSHCommandRunner basicauthproxy = null;
	public static SSHCommandRunner noauthproxy = null;
	public static String nErrMsg = null;
	public static String pErrMsg = null;
	public static String pErr407Msg = null;
	protected String ipv4_address = null;

	@BeforeClass(groups={"setup"})
	public void setupBeforeClass() throws IOException {
		basicauthproxy = new SSHCommandRunner(sm_basicauthproxyHostname, sm_basicauthproxySSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase, null);
		noauthproxy = new SSHCommandRunner(sm_noauthproxyHostname, sm_noauthproxySSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase, null);
		if (clienttasks!=null) nErrMsg = clienttasks.msg_NetworkErrorUnableToConnect;
		if (clienttasks!=null) pErrMsg = clienttasks.msg_ProxyConnectionFailed;
		if (clienttasks!=null) pErr407Msg = clienttasks.msg_ProxyConnectionFailed407;
		if (clienttasks!=null) ipv4_address = clienttasks.getIPV4Address();
	}
	
	@BeforeMethod(groups={"setup"})
	public void cleanRhsmConfigAndUnregisterBeforeMethod() {
		uncommentConfFileProxyParameters();
		updateConfFileProxyParameters("","","","");
		clienttasks.unregister(null, null, null);
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
	}
	protected void updateConfFileProxyParameters(String proxy_hostname, String proxy_port, String proxy_user, String proxy_password) {
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_hostname", proxy_hostname);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_port", proxy_port);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_user", proxy_user);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_password", proxy_password);
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
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","1176219"}),			sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	"bad-proxy",			sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(69),	null,		pErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","1301215"}),						sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl+"0",	sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(69),	null,		pErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		"bad-username",					sm_basicauthproxyPassword,	Integer.valueOf(69),	null,		pErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		"bad-password",				Integer.valueOf(69),	null,		pErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		"bad-username",					"bad-password",				Integer.valueOf(69),	null,		pErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		null,							null,						Integer.valueOf(69),	null,		pErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242"}),	"bad-username",		sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242"}),	sm_clientUsername,	"bad-password",		sm_clientOrg,	basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242"}),	sm_clientUsername,	sm_clientPassword,	"bad-org",		basicauthproxyUrl,		sm_basicauthproxyUsername,		sm_basicauthproxyPassword,	Integer.valueOf(70),	null,		oErrMsg}));
			
			// no auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			null,							null,						Integer.valueOf(0),		null,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			"ignored-username",				"ignored-password",			Integer.valueOf(0),		null,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1119688","1301215"}),						sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	noauthproxyUrl+"0",		null,							null,						Integer.valueOf(69),	null,		pErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				"bad-username",		sm_clientPassword,	sm_clientOrg,	noauthproxyUrl,			null,							null,						Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	"bad-password",		sm_clientOrg,	noauthproxyUrl,			null,							null,						Integer.valueOf(70),	null,		uErrMsg}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	"bad-org",		noauthproxyUrl,			null,							null,						Integer.valueOf(70),	null,		oErrMsg}));

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
//		String nErrMsg = "Network error, unable to connect to server. Please see "+clienttasks.rhsmLogFile+" for more information.";
		String uErrMsg = servertasks.invalidCredentialsMsg(); //"Invalid username or password";
		String oErrMsg = /*"Organization/Owner bad-org does not exist."*/"Organization bad-org does not exist.";
		if (sm_serverType.equals(CandlepinType.katello))	oErrMsg = "Couldn't find organization 'bad-org'";
//		String hostname = clienttasks.getConfParameter("hostname");
//		String prefix = clienttasks.getConfParameter("prefix");
//		String port = clienttasks.getConfParameter("port");
		
		
		// Object blockedByBug, String username, String password, Sring org, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogGrepPattern
		// 									blockedByBug,											username,			password,			org,			proxy,				proxyuser,					proxypassword,				proxy_hostnameConfig,		proxy_portConfig,			proxy_userConfig,			proxy_passwordConfig,		exitCode,				stdout,		stderr,		proxyRunner,	proxyLog,				proxyLogGrepPattern

		if (clienttasks.isPackageVersion("subscription-manager",">=","1.18.2-1")) {	// post commit ad982c13e79917e082f336255ecc42615e1e7707	1176219: Error out if bad proxy settings detected
			// basic auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688"}),						sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						"bad-proxy",				sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	nErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688"}),						sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort+"0",	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	nErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	pErr407Msg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	"bad-password",				Integer.valueOf(70),	null,	pErr407Msg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		"bad-username",				"bad-password",				Integer.valueOf(70),	null,	pErr407Msg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","1176219"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		""/*no username*/,			""/*no password*/,			Integer.valueOf(70),	null,	pErr407Msg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_DENIED"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	null,						null,						"bad-proxy",				sm_basicauthproxyPort+"0",	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	sm_basicauthproxyUsername,	null,						"bad-proxy",				sm_basicauthproxyPort+"0",	"bad-username",				sm_basicauthproxyPassword,	Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258","838242"}),	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	basicauthproxyUrl,	sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	"bad-proxy",				sm_basicauthproxyPort+"0",	"bad-username",				"bad-password",				Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,	null,		basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				"bad-username",		sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	uErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	"bad-password",		sm_clientOrg,	null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	uErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	"bad-org",		null,				null,						null,						sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(70),	null,	oErrMsg,	basicauthproxy,	sm_basicauthproxyLog,	"TCP_MISS"}));
			
			// no auth proxy test data...
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(0),		null,	null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"ignored-username",			"ignored-password",			Integer.valueOf(0),		null,	null,		noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688"}),						sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort+"0",		"",							"",							Integer.valueOf(70),	null,	nErrMsg,	noauthproxy,	sm_noauthproxyLog,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688"}),						sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,				null,						null,						"bad-proxy",				sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,	nErrMsg,	noauthproxy,	sm_noauthproxyLog,		null}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				"bad-username",		sm_clientPassword,	sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,	uErrMsg,	noauthproxy,	sm_noauthproxyLog,		"Connect"}));
			ll.add(Arrays.asList(new Object[]{	new BlockedByBzBug(new String[]{"1345962","1119688","755258"}),				sm_clientUsername,	"bad-password",		sm_clientOrg,	null,				null,						null,						sm_noauthproxyHostname,		sm_noauthproxyPort,			"",							"",							Integer.valueOf(70),	null,	uErrMsg,	noauthproxy,	sm_noauthproxyLog,		"Connect"}));
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
			if (!sm_serverType.equals("katello") && ((!nErrMsg.equals(l.get(9))&&!pErrMsg.equals(l.get(9)))||l.get(9)==null) && clienttasks.isPackageVersion("subscription-manager",">=","1.13.10-1")) {	// post commit 13fe8ffd8f876d27079b961fb6675424e65b9a10 bug 1119688
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
			
//DELETEME	if (!sm_serverType.equals("katello") && (!nErrMsg.equals(l.get(13))||l.get(13)==null) && clienttasks.isPackageVersion("subscription-manager",">=","1.13.10-1")) {	// post commit 13fe8ffd8f876d27079b961fb6675424e65b9a10 bug 1119688
			if (!sm_serverType.equals("katello") && ((!nErrMsg.equals(l.get(13))&&!pErr407Msg.equals(l.get(13)))||l.get(13)==null) && clienttasks.isPackageVersion("subscription-manager",">=","1.13.10-1")) {	// post commit 13fe8ffd8f876d27079b961fb6675424e65b9a10 bug 1119688
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
			if (l.get(8)/*stdout*/==pErrMsg || l.get(9)/*stderr*/==pErrMsg) {
				bugIds.add("1336551");	// Bug 1336551 - status and version modules are not using cache when bad command line proxy is specified
			}
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			// Note: Because the status module should return cached results when it fails to connect to the server, the exitCode should always be 0 and we'll null out the asserts on stdout and stderr
			ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	Integer.valueOf(0)/*exitCode*/,null/*stdout*/,null/*stderr*/}));
		}
		return TestNGUtils.convertListOfListsTo2dArray(ll);
	}
	
	
	@DataProvider(name="getStatusAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getStatusAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		//return TestNGUtils.convertListOfListsTo2dArray(getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (List<Object> l : getValidRegisterAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists()) {

			BlockedByBzBug blockedByBzBug = null;	// nullify the blockedByBug parameter since this function was originally not blocked by any bug			
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			// Note: Because the status module should return cached results when it fails to connect to the server, the exitCode should always be 0 and we'll null out the asserts on stdout and stderr
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
			if (l.get(8)/*stdout*/==pErrMsg || l.get(9)/*stderr*/==pErrMsg) {
				bugIds.add("1336551");	// Bug 1336551 - status and version modules are not using cache when bad command line proxy is specified
			}
			bugIds.add("1345962");	// Bug 1345962 - unbound method endheaders() must be called with HTTPSConnection instance as first argument (got RhsmProxyHTTPSConnection instance instead)
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			// Note: The version module should always succeed, yet report subscription management server: Unknown when connection fails to the server
//			if (l.get(8)==nErrMsg) {
			if (l.get(8)/*stdout*/==nErrMsg || l.get(9)/*stderr*/==nErrMsg) {
				ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	Integer.valueOf(0)/*exitCode*/,"subscription management server: Unknown"/*stdout*/,""/*stderr*/}));
			} else {
				ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	Integer.valueOf(0)/*exitCode*/,"subscription management server: "+servertasks.statusVersion/*stdout*/,""/*stderr*/}));
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
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));

			// Note: The version module should always succeed, yet report subscription management server: Unknown when connection fails to the server
//			if (l.get(12)/*stdout*/==nErrMsg) {
			if (l.get(12)/*stdout*/==nErrMsg || l.get(13)/*stderr*/==nErrMsg) {
				ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	Integer.valueOf(0)/*exitCode*/,"subscription management server: Unknown"/*stdout*/,""/*stderr*/,	l.get(14),	l.get(15),	l.get(16)}));
			} else {
				ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	Integer.valueOf(0)/*exitCode*/,"subscription management server: "+servertasks.statusVersion/*stdout*/,""/*stderr*/,	l.get(14),	l.get(15),	l.get(16)}));
			}
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
						
						ll.add(Arrays.asList(new Object[]{	blockedByBzBug,	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	new Integer(70)/*EX_SOFTWARE*/,	l.get(12),	new String("Standalone candlepin does not support redeeming a subscription."),	l.get(14),	l.get(15),	l.get(16)}));
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
//			if (l.get(12)!=null) {
//DELETEME	if (l.get(12)/*stdout*/==nErrMsg || l.get(13)/*stderr*/==nErrMsg) {
			if (l.get(12)/*stdout*/==nErrMsg || l.get(13)/*stderr*/==nErrMsg || l.get(13)/*stderr*/==pErr407Msg) {
//				ll.add(Arrays.asList(new Object[]{	l.get(0),	l.get(1),	l.get(2),	l.get(3),	l.get(4),	l.get(5),	l.get(6),	l.get(7),	l.get(8),	l.get(9),	l.get(10),	l.get(11),	null,	"Error updating system data, see /var/log/rhsm/rhsm.log for more details.",	l.get(14),	l.get(15),	l.get(16)}));
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
