package com.redhat.qe.sm.cli.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

/**
 * @author jsefler
 *
 * http://gibson.usersys.redhat.com/agilo/ticket/4618
 * automate tests for subscription manager register_/subscribe/unsubscribe/unregister_/clean/facts/identity_/refresh_/list_ with basic and noauth proxy servers
 */
@Test(groups={"proxy"})
public class ProxyTests extends SubscriptionManagerCLITestScript {

	protected String basicauthproxyHostname = getProperty("sm.basicauthproxy.hostname", "");
	protected String basicauthproxyPort = getProperty("sm.basicauthproxy.port", "");
	protected String basicauthproxyUsername = getProperty("sm.basicauthproxy.username", "");
	protected String basicauthproxyPassword = getProperty("sm.basicauthproxy.password", "");
	protected String basicauthproxyLog = getProperty("sm.basicauthproxy.log", "");
	
	protected String noauthproxyHostname = getProperty("sm.noauthproxy.hostname", "");
	protected String noauthproxyPort = getProperty("sm.noauthproxy.port", "");
	protected String noauthproxyLog = getProperty("sm.noauthproxy.log", "");
	

	// Test methods ***********************************************************************
	
	// REGISTER Test methods ***********************************************************************

//	@Test(	description="subscription-manager : register using a basic auth proxy server",
//			groups={},
//			enabled=true)
//	@ImplementsNitrateTest(caseId=70133)	
//	public void RegisterUsingBasicAuthProxyServer_Test() {
//
//		String proxy = String.format("%s:%s", basicauthproxyHostname,basicauthproxyPort); proxy = proxy.replaceAll(":$", "");
//		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, proxy, basicauthproxyUsername, basicauthproxyPassword);
//	}
//	
//	
//	@Test(	description="subscription-manager : register using a no auth proxy server",
//			groups={},
//			enabled=true)
//	//@ImplementsNitrateTest(caseId=)	
//	public void RegisterUsingNoAuthProxyServer_Test() {
//
//		String proxy = String.format("%s:%s", noauthproxyHostname,noauthproxyPort); proxy = proxy.replaceAll(":$", "");
//		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, proxy, null, null);
//		clienttasks.register(clientusername, clientpassword, null, null, null, null, Boolean.TRUE, proxy, "null", "null");
//		clienttasks.register(clientusername, clientpassword, null, null, null, null, Boolean.TRUE, proxy, "null", null);
//	}
	
	
	@Test(	description="subscription-manager : register using a proxy server (Positive & Negative Variations)",
			groups={},
			dataProvider="getAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void RegisterAttemptsUsingProxyServer_Test(String username, String password, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdoutRegex, String stderrRegex) {
		String moduleTask = "register";
		
		SSHCommandResult result = clienttasks.register_(username, password, null, null, null, null, null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(result.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(result.getStdout().trim(), stdoutRegex, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(result.getStderr().trim(), stderrRegex, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}

	
//	@Test(	description="subscription-manager : register using a basic auth proxy server after setting rhsm.config parameters",
//			groups={},
//			enabled=true)
//	//@ImplementsNitrateTest(caseId=)	
//	public void RegisterUsingBasicAuthProxyServerViaRhsmConfig_Test() {
//		String moduleTask = "register";
//	
//		// set the config parameters
//		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_hostname", basicauthproxyHostname);
//		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_port", basicauthproxyPort);
//		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_user", basicauthproxyUsername);
//		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_password", basicauthproxyPassword);
//
//		// pad the tail of basicauthproxyLog with a message
//		RemoteFileTasks.runCommandAndAssert(basicauthproxy,"echo 'Testing "+moduleTask+" NegativeAttemptsUsingProxyServerViaRhsmConfig_Test...'  >> "+basicauthproxyLog, Integer.valueOf(0));
//
//		// register
//		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
//
//		// assert the tail of basicauthproxyLog shows the successful CONNECT
//		// 1292545301.350    418 10.16.120.247 TCP_MISS/200 1438 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.146 -
//		// 1292551602.625      0 10.16.120.247 TCP_DENIED/407 3840 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 - NONE/- text/html
//		SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(basicauthproxy,"tail -1 "+basicauthproxyLog, Integer.valueOf(0));
//		Assert.assertContainsMatch(result.getStdout(), String.format("CONNECT %s:%s %s",serverHostname,serverPort,basicauthproxyUsername), "The '"+basicauthproxyHostname+"' proxy server appears to be passing the "+moduleTask+" connection through to the candlepin server.");
//	}
	
	@Test(	description="subscription-manager : register using a proxy server after setting rhsm.config parameters (Positive & Negative Variations)",
			groups={},
			dataProvider="getAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RegisterAttemptsUsingProxyServerViaRhsmConfig_Test(String username, String password, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdoutRegex, String stderrRegex, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogRegex) {
		String moduleTask = "register";

		// pad the tail of basicauthproxyLog with a message
		RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo 'Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test...'  >> "+proxyLog, Integer.valueOf(0));
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		
		// attempt to register
		SSHCommandResult attemptResult = clienttasks.register_(username, password, null, null, null, null, null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from a negative attempt to "+moduleTask+" using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(attemptResult.getStdout().trim(), stdoutRegex, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(attemptResult.getStderr().trim(), stderrRegex, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogRegex (BASIC AUTH)
		// 1292545301.350    418 10.16.120.247 TCP_MISS/200 1438 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.146 -
		// 1292551602.625      0 10.16.120.247 TCP_DENIED/407 3840 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 - NONE/- text/html

		// assert the tail of proxyLog shows the proxyLogRegex (NO AUTH)
		// CONNECT   Dec 17 18:56:22 [20793]: Connect (file descriptor 7):  [10.16.120.248]
		// CONNECT   Dec 17 18:56:22 [20793]: Request (file descriptor 7): CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 HTTP/1.1
		// INFO      Dec 17 18:56:22 [20793]: No proxy for jsefler-f12-candlepin.usersys.redhat.com
		// CONNECT   Dec 17 18:56:22 [20793]: Established connection to host "jsefler-f12-candlepin.usersys.redhat.com" using file descriptor 8.
		// INFO      Dec 17 18:56:22 [20793]: Not sending client headers to remote machine
		// INFO      Dec 17 18:56:22 [20793]: Closed connection between local client (fd:7) and remote client (fd:8)
		
		if (proxyLogRegex!=null) {
			SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogRegex, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
		}
	}
	
	
	
	
	// UNREGISTER Test methods ***********************************************************************

	@Test(	description="subscription-manager : unregister using a proxy server (Positive & Negative Variations)",
			groups={},
			dataProvider="getAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void UnregisterAttemptsUsingProxyServer_Test(String username, String password, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdoutRegex, String stderrRegex) {
		// setup for test
		String moduleTask = "unregister";
		if (!username.equals(clientusername) || !password.equals(clientpassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
		
		SSHCommandResult result = clienttasks.unregister_(proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(result.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(result.getStdout().trim(), stdoutRegex, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(result.getStderr().trim(), stderrRegex, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}

	
	@Test(	description="subscription-manager : unregister using a proxy server after setting rhsm.config parameters (Positive & Negative Variations)",
			groups={},
			dataProvider="getAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void UnregisterAttemptsUsingProxyServerViaRhsmConfig_Test(String username, String password, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdoutRegex, String stderrRegex, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogRegex) {
		// setup for test
		String moduleTask = "unregister";
		if (!username.equals(clientusername) || !password.equals(clientpassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo 'Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test...'  >> "+proxyLog, Integer.valueOf(0));
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		
		// attempt to unregister
		SSHCommandResult attemptResult = clienttasks.unregister_(proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(attemptResult.getStdout().trim(), stdoutRegex, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(attemptResult.getStderr().trim(), stderrRegex, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogRegex
		if (proxyLogRegex!=null) {
			SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogRegex, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
		}
	}
	
	
	
	// IDENTITY Test methods ***********************************************************************

	@Test(	description="subscription-manager : identity using a proxy server (Positive & Negative Variations)",
			groups={},
			dataProvider="getAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void IdentityAttemptsUsingProxyServer_Test(String username, String password, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdoutRegex, String stderrRegex) {
		// setup for test
		String moduleTask = "identity";
		//if (!username.equals(clientusername) || !password.equals(clientpassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
		
		SSHCommandResult result = clienttasks.identity_(username, password, Boolean.TRUE, proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(result.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(result.getStdout().trim(), stdoutRegex, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(result.getStderr().trim(), stderrRegex, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}

	
	@Test(	description="subscription-manager : identity using a proxy server after setting rhsm.config parameters (Positive & Negative Variations)",
			groups={},
			dataProvider="getAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void IdentityAttemptsUsingProxyServerViaRhsmConfig_Test(String username, String password, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdoutRegex, String stderrRegex, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogRegex) {
		// setup for test
		String moduleTask = "identity";
		//if (!username.equals(clientusername) || !password.equals(clientpassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo 'Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test...'  >> "+proxyLog, Integer.valueOf(0));
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		
		// attempt to unregister
		SSHCommandResult attemptResult = clienttasks.identity_(username, password, Boolean.TRUE, proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(attemptResult.getStdout().trim(), stdoutRegex, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(attemptResult.getStderr().trim(), stderrRegex, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogRegex
		if (proxyLogRegex!=null) {
			SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogRegex, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
		}
	}
	
	
	// REFRESH Test methods ***********************************************************************

	@Test(	description="subscription-manager : refresh using a proxy server (Positive & Negative Variations)",
			groups={},
			dataProvider="getAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RefreshAttemptsUsingProxyServer_Test(String username, String password, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdoutRegex, String stderrRegex) {
		// setup for test
		String moduleTask = "refresh";
		if (!username.equals(clientusername) || !password.equals(clientpassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
		
		SSHCommandResult result = clienttasks.refresh_(proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(result.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(result.getStdout().trim(), stdoutRegex, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(result.getStderr().trim(), stderrRegex, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}

	
	@Test(	description="subscription-manager : refresh using a proxy server after setting rhsm.config parameters (Positive & Negative Variations)",
			groups={},
			dataProvider="getAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RefreshAttemptsUsingProxyServerViaRhsmConfig_Test(String username, String password, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdoutRegex, String stderrRegex, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogRegex) {
		// setup for test
		String moduleTask = "refresh";
		if (!username.equals(clientusername) || !password.equals(clientpassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo 'Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test...'  >> "+proxyLog, Integer.valueOf(0));
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		
		// attempt to unregister
		SSHCommandResult attemptResult = clienttasks.refresh_(proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(attemptResult.getStdout().trim(), stdoutRegex, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(attemptResult.getStderr().trim(), stderrRegex, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogRegex
		if (proxyLogRegex!=null) {
			SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogRegex, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
		}
	}
	
	
	
	// LIST Test methods ***********************************************************************

	@Test(	description="subscription-manager : list using a proxy server (Positive & Negative Variations)",
			groups={},
			dataProvider="getAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void ListAttemptsUsingProxyServer_Test(String username, String password, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdoutRegex, String stderrRegex) {
		// setup for test
		String moduleTask = "list";
		if (!username.equals(clientusername) || !password.equals(clientpassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
		
		SSHCommandResult result = clienttasks.list_(null,Boolean.TRUE,null,null,proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(result.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(result.getStdout().trim(), stdoutRegex, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(result.getStderr().trim(), stderrRegex, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}

	
	@Test(	description="subscription-manager : list using a proxy server after setting rhsm.config parameters (Positive & Negative Variations)",
			groups={},
			dataProvider="getAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void ListAttemptsUsingProxyServerViaRhsmConfig_Test(String username, String password, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdoutRegex, String stderrRegex, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogRegex) {
		// setup for test
		String moduleTask = "list";
		if (!username.equals(clientusername) || !password.equals(clientpassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo 'Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test...'  >> "+proxyLog, Integer.valueOf(0));
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		
		// attempt to unregister
		SSHCommandResult attemptResult = clienttasks.list_(null,Boolean.TRUE,null,null,proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(attemptResult.getStdout().trim(), stdoutRegex, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(attemptResult.getStderr().trim(), stderrRegex, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogRegex
		if (proxyLogRegex!=null) {
			SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogRegex, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
		}
	}
	
	
	
	// FACTS Test methods ***********************************************************************

	@Test(	description="subscription-manager : facts using a proxy server (Positive & Negative Variations)",
			groups={},
			dataProvider="getAttemptsUsingProxyServerData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void FactsAttemptsUsingProxyServer_Test(String username, String password, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdoutRegex, String stderrRegex) {
		// setup for test
		String moduleTask = "facts";
		if (!username.equals(clientusername) || !password.equals(clientpassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
		
		SSHCommandResult result = clienttasks.facts_(null,Boolean.TRUE,proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(result.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(result.getStdout().trim(), stdoutRegex, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(result.getStderr().trim(), stderrRegex, "The stderr from an attempt to "+moduleTask+" using a proxy server.");
	}

	
	@Test(	description="subscription-manager : facts using a proxy server after setting rhsm.config parameters (Positive & Negative Variations)",
			groups={},
			dataProvider="getAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void FactsAttemptsUsingProxyServerViaRhsmConfig_Test(String username, String password, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdoutRegex, String stderrRegex, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogRegex) {
		// setup for test
		String moduleTask = "facts";
		if (!username.equals(clientusername) || !password.equals(clientpassword)) throw new SkipException("These dataProvided parameters are either superfluous or not meaningful for this test.");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);
	
		// pad the tail of basicauthproxyLog with a message
		RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo 'Testing "+moduleTask+" AttemptsUsingProxyServerViaRhsmConfig_Test...'  >> "+proxyLog, Integer.valueOf(0));
		
		// set the config parameters
		updateConfFileProxyParameters(proxy_hostnameConfig, proxy_portConfig, proxy_userConfig, proxy_passwordConfig);
		
		// attempt to unregister
		SSHCommandResult attemptResult = clienttasks.facts_(null,Boolean.TRUE,proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from an attempt to "+moduleTask+" using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(attemptResult.getStdout().trim(), stdoutRegex, "The stdout from an attempt to "+moduleTask+" using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(attemptResult.getStderr().trim(), stderrRegex, "The stderr from an attempt to "+moduleTask+" using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogRegex
		if (proxyLogRegex!=null) {
			SSHCommandResult proxyLogResult = RemoteFileTasks.runCommandAndAssert(proxyRunner,"tail -1 "+proxyLog, Integer.valueOf(0));
			Assert.assertContainsMatch(proxyLogResult.getStdout(), proxyLogRegex, "The proxy server appears to be logging the expected connection attempts to the candlepin server.");
		}
	}
	
	
	// Configuration methods ***********************************************************************
	
	public static SSHCommandRunner basicauthproxy = null;
	public static SSHCommandRunner noauthproxy = null;
	@BeforeClass(groups={"setup"})
	public void setupBeforeClass() throws IOException {
		basicauthproxy = new SSHCommandRunner(basicauthproxyHostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
		noauthproxy = new SSHCommandRunner(noauthproxyHostname, sshUser, sshKeyPrivate, sshkeyPassphrase, null);
	}
	
	@BeforeMethod(groups={"setup"})
	public void cleanRhsmConfigAndUnregisterBeforeMethod() {
		updateConfFileProxyParameters("","","","");
		clienttasks.unregister(null, null, null);
	}
	
	@AfterClass(groups={"setup"})
	public void cleanRhsmConfigAfterClass() throws IOException {
		cleanRhsmConfigAndUnregisterBeforeMethod();
	}
	
	
	
	// Protected methods ***********************************************************************
	
	protected void updateConfFileProxyParameters(String proxy_hostname, String proxy_port, String proxy_user, String proxy_password) {
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_hostname", proxy_hostname);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_port", proxy_port);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_user", proxy_user);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_password", proxy_password);
	}

	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="getAttemptsUsingProxyServerData")
	public Object[][] getAttemptsUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getAttemptsUsingProxyServerDataAsListOfLists());
	}
	protected List<List<Object>> getAttemptsUsingProxyServerDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		String basicauthproxyUrl = String.format("%s:%s", basicauthproxyHostname,basicauthproxyPort); basicauthproxyUrl = basicauthproxyUrl.replaceAll(":$", "");
		String noauthproxyUrl = String.format("%s:%s", noauthproxyHostname,noauthproxyPort); noauthproxyUrl = noauthproxyUrl.replaceAll(":$", "");
		String nErrMsg = "Network error, unable to connect to server. Please see "+clienttasks.rhsmLogFile+" for more information.";
		String uErrMsg = "Invalid username or password";
		
		// String username, String password, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdoutRegex, String stderrRegex

		// basic auth proxy test data...
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(0),		null,		null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	"bad-proxy",			basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	nErrMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl+"0",	basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	nErrMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,		"bad-username",				basicauthproxyPassword,	Integer.valueOf(255),	nErrMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,		basicauthproxyUsername,		"bad-password",			Integer.valueOf(255),	nErrMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,		"bad-username",				"bad-password",			Integer.valueOf(255),	nErrMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,		null,						null,					Integer.valueOf(255),	nErrMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	"bad-password",	basicauthproxyUrl,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	null,		uErrMsg}));
		ll.add(Arrays.asList(new Object[]{	"bad-username",	clientpassword,	basicauthproxyUrl,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	null,		uErrMsg}));

		// no auth proxy test data...
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	noauthproxyUrl,			null,						null,					Integer.valueOf(0),		null,		null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	noauthproxyUrl,			"ignored-username",			"ignored-password",		Integer.valueOf(0),		null,		null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	noauthproxyUrl+"0",		null,						null,					Integer.valueOf(255),	nErrMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	"bad-password",	noauthproxyUrl,			null,						null,					Integer.valueOf(255),	null,		uErrMsg}));
		ll.add(Arrays.asList(new Object[]{	"bad-username",	clientpassword,	noauthproxyUrl,			null,						null,					Integer.valueOf(255),	null,		uErrMsg}));

		return ll;
	}
	
	
	@DataProvider(name="getAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
	}
	protected List<List<Object>> getAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		String basicauthproxyUrl = String.format("%s:%s", basicauthproxyHostname,basicauthproxyPort); basicauthproxyUrl = basicauthproxyUrl.replaceAll(":$", "");
		String noauthproxyUrl = String.format("%s:%s", noauthproxyHostname,noauthproxyPort); noauthproxyUrl = noauthproxyUrl.replaceAll(":$", "");
		String nErrMsg = "Network error, unable to connect to server. Please see "+clienttasks.rhsmLogFile+" for more information.";
		String uErrMsg = "Invalid username or password";

		// String username, String password, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdoutRegex, String stderrRegex, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogRegex
		// basic auth proxy test data...
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,				null,					null,					basicauthproxyHostname,	basicauthproxyPort,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicauthproxy,	basicauthproxyLog,	"TCP_MISS"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,				null,					null,					"bad-proxy",			basicauthproxyPort,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	nErrMsg,	null,		basicauthproxy,	basicauthproxyLog,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,				null,					null,					basicauthproxyHostname,	basicauthproxyPort+"0",	basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	nErrMsg,	null,		basicauthproxy,	basicauthproxyLog,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,				null,					null,					basicauthproxyHostname,	basicauthproxyPort,		"bad-username",				basicauthproxyPassword,	Integer.valueOf(255),	nErrMsg,	null,		basicauthproxy,	basicauthproxyLog,	"TCP_DENIED"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,				null,					null,					basicauthproxyHostname,	basicauthproxyPort,		basicauthproxyUsername,		"bad-password",			Integer.valueOf(255),	nErrMsg,	null,		basicauthproxy,	basicauthproxyLog,	"TCP_DENIED"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,				null,					null,					basicauthproxyHostname,	basicauthproxyPort,		"bad-username",				"bad-password",			Integer.valueOf(255),	nErrMsg,	null,		basicauthproxy,	basicauthproxyLog,	"TCP_DENIED"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,				null,					null,					basicauthproxyHostname,	basicauthproxyPort,		""/*no username*/,			""/*no password*/,		Integer.valueOf(255),	nErrMsg,	null,		basicauthproxy,	basicauthproxyLog,	"TCP_DENIED"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,	null,					null,					"bad-proxy",			basicauthproxyPort+"0",	basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicauthproxy,	basicauthproxyLog,	"TCP_MISS"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,	basicauthproxyUsername,	null,					"bad-proxy",			basicauthproxyPort+"0",	"bad-username",				basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicauthproxy,	basicauthproxyLog,	"TCP_MISS"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,	basicauthproxyUsername,	basicauthproxyPassword,	"bad-proxy",			basicauthproxyPort+"0",	"bad-username",				"bad-password",			Integer.valueOf(0),		null,		null,		basicauthproxy,	basicauthproxyLog,	"TCP_MISS"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,				null,					null,					basicauthproxyHostname,	basicauthproxyPort,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicauthproxy,	basicauthproxyLog,	"TCP_MISS"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	"bad-password",	null,				null,					null,					basicauthproxyHostname,	basicauthproxyPort,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	null,		uErrMsg,	basicauthproxy,	basicauthproxyLog,	"TCP_MISS"}));
		ll.add(Arrays.asList(new Object[]{	"bad-username",	"bad-password",	null,				null,					null,					basicauthproxyHostname,	basicauthproxyPort,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	null,		uErrMsg,	basicauthproxy,	basicauthproxyLog,	"TCP_MISS"}));

		// no auth proxy test data...
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,				null,					null,					noauthproxyHostname,	noauthproxyPort,		"",							"",						Integer.valueOf(0),		null,		null,		noauthproxy,	noauthproxyLog,		"Closed connection"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,				null,					null,					noauthproxyHostname,	noauthproxyPort,		"ignored-username",			"ignored-password",		Integer.valueOf(0),		null,		null,		noauthproxy,	noauthproxyLog,		"Closed connection"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,				null,					null,					noauthproxyHostname,	noauthproxyPort+"0",	"",							"",						Integer.valueOf(255),	nErrMsg,	null,		noauthproxy,	noauthproxyLog,		null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,				null,					null,					"bad-proxy",			noauthproxyPort,		"",							"",						Integer.valueOf(255),	nErrMsg,	null,		noauthproxy,	noauthproxyLog,		null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	"bad-password",	null,				null,					null,					noauthproxyHostname,	noauthproxyPort,		"",							"",						Integer.valueOf(255),	null,		uErrMsg,	noauthproxy,	noauthproxyLog,		"Closed connection"}));
		ll.add(Arrays.asList(new Object[]{	"bad-username",	"bad-password",	null,				null,					null,					noauthproxyHostname,	noauthproxyPort,		"",							"",						Integer.valueOf(255),	null,		uErrMsg,	noauthproxy,	noauthproxyLog,		"Closed connection"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	noauthproxyUrl,		null,					null,					"bad-proxy",			noauthproxyPort+"0",	"",							"",						Integer.valueOf(0),		null,		null,		noauthproxy,	noauthproxyLog,		"Closed connection"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	noauthproxyUrl,		"ignored-username",		"ignored-password",		"bad-proxy",			noauthproxyPort+"0",	"bad-username",				"bad-password",			Integer.valueOf(0),		null,		null,		noauthproxy,	noauthproxyLog,		"Closed connection"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	"bad-proxy",		null,					null,					noauthproxyHostname,	noauthproxyPort,		"",							"",						Integer.valueOf(255),	nErrMsg,	null,		noauthproxy,	noauthproxyLog,		null}));


		return ll;
	}
}
