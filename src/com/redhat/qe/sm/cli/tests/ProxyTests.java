package com.redhat.qe.sm.cli.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

/**
 * @author jsefler
 *
 * http://gibson.usersys.redhat.com/agilo/ticket/4618
 * automate tests for subscription manager register/subscribe/unsubscribe/unregister/clean/facts/identity/refresh/list with basic and noauth proxy servers
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
	
	@Test(	description="subscription-manager : register using a basic auth proxy server",
			groups={"CLIProxyTests"},
			enabled=true)
	@ImplementsNitrateTest(caseId=70133)	
	public void RegisterUsingBasicAuthProxyServer_Test() {
		
		String proxy = String.format("%s:%s", basicauthproxyHostname,basicauthproxyPort); proxy = proxy.replaceAll(":$", "");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, proxy, basicauthproxyUsername, basicauthproxyPassword);
	}
	
	
	@Test(	description="subscription-manager : register using a no auth proxy server",
			groups={"CLIProxyTests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RegisterUsingNoAuthProxyServer_Test() {
		
		String proxy = String.format("%s:%s", noauthproxyHostname,noauthproxyPort); proxy = proxy.replaceAll(":$", "");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, proxy, null, null);
		clienttasks.register(clientusername, clientpassword, null, null, null, null, Boolean.TRUE, proxy, "null", "null");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, Boolean.TRUE, proxy, "null", null);
	}
	
	
	@Test(	description="subscription-manager : register using a proxy server (Negative Tests)",
			groups={"CLIProxyTests"},
			dataProvider="getNegativeAttemptsUsingProxyServerData",
			enabled=true)
	@ImplementsNitrateTest(caseId=70316)	
	public void NegativeRegisterAttemptsUsingProxyServer_Test(String username, String password, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdoutRegex, String stderrRegex) {
		
		SSHCommandResult result = clienttasks.register_(username, password, null, null, null, null, null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(result.getExitCode(), exitCode, "The exit code from a negative attempt to register using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(result.getStdout().trim(), stdoutRegex, "The stdout from a negative attempt to register using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(result.getStderr().trim(), stderrRegex, "The stderr from a negative attempt to register using a proxy server.");
	}

	
	@Test(	description="subscription-manager : register using a basic auth proxy server after setting rhsm.config parameters",
			groups={"ConfigProxyTests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RegisterUsingBasicAuthProxyServerViaRhsmConfig_Test() {
		
		// set the config parameters
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_hostname", basicauthproxyHostname);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_port", basicauthproxyPort);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_user", basicauthproxyUsername);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_password", basicauthproxyPassword);

		// pad the tail of basicauthproxyLog with a message
		RemoteFileTasks.runCommandAndAssert(basicauthproxy,"echo 'Testing RegisterUsingBasicAuthProxyServerViaRhsmConfig_Test...'  >> "+basicauthproxyLog, Integer.valueOf(0));

		// register
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null);

		// assert the tail of basicauthproxyLog shows the successful CONNECT
		// 1292545301.350    418 10.16.120.247 TCP_MISS/200 1438 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.146 -
		// 1292551602.625      0 10.16.120.247 TCP_DENIED/407 3840 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 - NONE/- text/html
		SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(basicauthproxy,"tail -1 "+basicauthproxyLog, Integer.valueOf(0));
		Assert.assertContainsMatch(result.getStdout(), String.format("CONNECT %s:%s %s",serverHostname,serverPort,basicauthproxyUsername), "The '"+basicauthproxyHostname+"' proxy server appears to be passing the register connection through to the candlepin server.");
	}
	
	@Test(	description="subscription-manager : register using a proxy server after setting rhsm.config parameters (Negative Tests)",
			groups={"ConfigProxyTests"},
			dataProvider="getNegativeAttemptsUsingProxyServerViaRhsmConfigData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void NegativeRegisterAttemptsUsingProxyServerViaRhsmConfig_Test(String username, String password, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdoutRegex, String stderrRegex, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogRegex) {
		
		// pad the tail of basicauthproxyLog with a message
		RemoteFileTasks.runCommandAndAssert(proxyRunner,"echo 'Testing NegativeRegisterAttemptsUsingProxyServerViaRhsmConfig_Test...'  >> "+proxyLog, Integer.valueOf(0));
		
		// set the config parameters
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_hostname", proxy_hostnameConfig);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_port", proxy_portConfig);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_user", proxy_userConfig);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_password", proxy_passwordConfig);
		
		// attempt to register
		SSHCommandResult attemptResult = clienttasks.register_(username, password, null, null, null, null, null, proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(attemptResult.getExitCode(), exitCode, "The exit code from a negative attempt to register using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(attemptResult.getStdout().trim(), stdoutRegex, "The stdout from a negative attempt to register using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(attemptResult.getStderr().trim(), stderrRegex, "The stderr from a negative attempt to register using a proxy server.");

		// assert the tail of proxyLog shows the proxyLogRegex
		// 1292545301.350    418 10.16.120.247 TCP_MISS/200 1438 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.146 -
		// 1292551602.625      0 10.16.120.247 TCP_DENIED/407 3840 CONNECT jsefler-f12-candlepin.usersys.redhat.com:8443 - NONE/- text/html
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
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_hostname", "");
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_port", "");
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_user", "");
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "proxy_password", "");
		clienttasks.unregister();
	}
	
	@AfterClass(groups={"setup"})
	public void cleanRhsmConfigAfterClass() throws IOException {
		cleanRhsmConfigAndUnregisterBeforeMethod();
	}
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************
	
	
	@DataProvider(name="getNegativeAttemptsUsingProxyServerData")
	public Object[][] getNegativeAttemptsUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getNegativeAttemptsUsingProxyServerDataAsListOfLists());
	}
	protected List<List<Object>> getNegativeAttemptsUsingProxyServerDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		String basicauthproxyUrl = String.format("%s:%s", basicauthproxyHostname,basicauthproxyPort); basicauthproxyUrl = basicauthproxyUrl.replaceAll(":$", "");
		String noauthproxyUrl = String.format("%s:%s", noauthproxyHostname,noauthproxyPort); noauthproxyUrl = noauthproxyUrl.replaceAll(":$", "");
		String networkErrorMsg = "Network error, unable to connect to server. Please see "+clienttasks.rhsmLogFile+" for more information.";

		// String username, String password, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdoutRegex, String stderrRegex

		// basic auth proxy test data...
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(0),		null,				null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	"bad-proxy",			basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	networkErrorMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl+"0",	basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	networkErrorMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,		"bad-username",				basicauthproxyPassword,	Integer.valueOf(255),	networkErrorMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,		basicauthproxyUsername,		"bad-password",			Integer.valueOf(255),	networkErrorMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,		null,						null,					Integer.valueOf(255),	networkErrorMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	"bad-password",	basicauthproxyUrl,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	null,				"Invalid username or password"}));
		ll.add(Arrays.asList(new Object[]{	"bad-username",	clientpassword,	basicauthproxyUrl,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	null,				"Invalid username or password"}));

		// no auth proxy test data...
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	noauthproxyUrl,			null,						null,					Integer.valueOf(0),		null,				null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	noauthproxyUrl+"0",		null,						null,					Integer.valueOf(255),	networkErrorMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	"bad-password",	noauthproxyUrl,			null,						null,					Integer.valueOf(255),	null,				"Invalid username or password"}));
		ll.add(Arrays.asList(new Object[]{	"bad-username",	clientpassword,	noauthproxyUrl,			null,						null,					Integer.valueOf(255),	null,				"Invalid username or password"}));

		return ll;
	}
	
	
	@DataProvider(name="getNegativeAttemptsUsingProxyServerViaRhsmConfigData")
	public Object[][] getNegativeAttemptsUsingProxyServerViaRhsmConfigDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getNegativeAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists());
	}
	protected List<List<Object>> getNegativeAttemptsUsingProxyServerViaRhsmConfigDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		String basicauthproxyUrl = String.format("%s:%s", basicauthproxyHostname,basicauthproxyPort); basicauthproxyUrl = basicauthproxyUrl.replaceAll(":$", "");
		String noauthproxyUrl = String.format("%s:%s", noauthproxyHostname,noauthproxyPort); noauthproxyUrl = noauthproxyUrl.replaceAll(":$", "");
		String networkErrorMsg = "Network error, unable to connect to server. Please see "+clienttasks.rhsmLogFile+" for more information.";

		// String username, String password, String proxy, String proxyuser, String proxypassword, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdoutRegex, String stderrRegex, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogRegex
		// basic auth proxy test data...
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,	null,	null,	basicauthproxyHostname,	basicauthproxyPort,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(0),		null,				null,	basicauthproxy,	basicauthproxyLog,	"TCP_MISS"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,	null,	null,	"bad-proxy",			basicauthproxyPort,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	networkErrorMsg,	null,	basicauthproxy,	basicauthproxyLog,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,	null,	null,	basicauthproxyHostname,	basicauthproxyPort+"0",	basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	networkErrorMsg,	null,	basicauthproxy,	basicauthproxyLog,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,	null,	null,	basicauthproxyHostname,	basicauthproxyPort,		"bad-username",				basicauthproxyPassword,	Integer.valueOf(255),	networkErrorMsg,	null,	basicauthproxy,	basicauthproxyLog,	"TCP_DENIED"}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,	null,	null,	basicauthproxyHostname,	basicauthproxyPort,		basicauthproxyUsername,		"bad-password",			Integer.valueOf(255),	networkErrorMsg,	null,	basicauthproxy,	basicauthproxyLog,	"TCP_DENIED"}));
//		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	"bad-proxy",			basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	networkErrorMsg,	null}));
//		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl+"0",	basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	networkErrorMsg,	null}));
//		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,		"bad-username",				basicauthproxyPassword,	Integer.valueOf(255),	networkErrorMsg,	null}));
//		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,		basicauthproxyUsername,		"bad-password",			Integer.valueOf(255),	networkErrorMsg,	null}));
//		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	basicauthproxyUrl,		null,						null,					Integer.valueOf(255),	networkErrorMsg,	null}));
//		ll.add(Arrays.asList(new Object[]{	clientusername,	"bad-password",	basicauthproxyUrl,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	null,				"Invalid username or password"}));
//		ll.add(Arrays.asList(new Object[]{	"bad-username",	clientpassword,	basicauthproxyUrl,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	null,				"Invalid username or password"}));
//
//		// no auth proxy test data...
//		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	noauthproxyUrl,			null,						null,					Integer.valueOf(0),		null,				null}));
//		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	noauthproxyUrl+"0",		null,						null,					Integer.valueOf(255),	networkErrorMsg,	null}));
//		ll.add(Arrays.asList(new Object[]{	clientusername,	"bad-password",	noauthproxyUrl,			null,						null,					Integer.valueOf(255),	null,				"Invalid username or password"}));
//		ll.add(Arrays.asList(new Object[]{	"bad-username",	clientpassword,	noauthproxyUrl,			null,						null,					Integer.valueOf(255),	null,				"Invalid username or password"}));

		return ll;
	}
}
