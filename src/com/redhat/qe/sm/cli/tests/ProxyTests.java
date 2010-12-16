package com.redhat.qe.sm.cli.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.AfterGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;

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
			groups={},
			enabled=true)
	@ImplementsNitrateTest(caseId=70133)	
	public void RegisterUsingBasicAuthProxyServer_Test() {
		
		// Start fresh by unregistering and registering...
		log.info("Start fresh by unregistering...");
		clienttasks.unregister();
		
		String proxy = String.format("%s:%s", basicauthproxyHostname,basicauthproxyPort); proxy = proxy.replaceAll(":$", "");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, proxy, basicauthproxyUsername, basicauthproxyPassword);
	}
	
	
	@Test(	description="subscription-manager : register using a no auth proxy server",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void RegisterUsingNoAuthProxyServer_Test() {
		
		// Start fresh by unregistering and registering...
		log.info("Start fresh by unregistering...");
		clienttasks.unregister();
		
		String proxy = String.format("%s:%s", noauthproxyHostname,noauthproxyPort); proxy = proxy.replaceAll(":$", "");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, null, proxy, null, null);
		clienttasks.register(clientusername, clientpassword, null, null, null, null, Boolean.TRUE, proxy, "null", "null");
		clienttasks.register(clientusername, clientpassword, null, null, null, null, Boolean.TRUE, proxy, "null", null);
	}
	
	
	@Test(	description="subscription-manager : register using a proxy server (Negative Tests)",
			groups={},
			dataProvider="getNegativeRegisterAttemptsUsingProxyServerData",
			enabled=true)
	@ImplementsNitrateTest(caseId=70316)	
	public void NegativeRegisterAttemptsUsingProxyServer_Test(String username, String password, ConsumerType type, String name, String consumerId, Boolean autosubscribe, Boolean force, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdoutRegex, String stderrRegex) {
		
		// Start fresh by unregistering and registering...
		log.info("Start fresh by unregistering...");
		clienttasks.unregister();
		
		SSHCommandResult result = clienttasks.register_(username, password, type, name, consumerId, autosubscribe, force, proxy, proxyuser, proxypassword);
		if (exitCode!=null)		Assert.assertEquals(result.getExitCode(), exitCode, "The exit code from a negative attempt to register using a proxy server.");
		if (stdoutRegex!=null)	Assert.assertContainsMatch(result.getStdout().trim(), stdoutRegex, "The stdout from a negative attempt to register using a proxy server.");
		if (stderrRegex!=null)	Assert.assertContainsMatch(result.getStderr().trim(), stderrRegex, "The stderr from a negative attempt to register using a proxy server.");

	}

	
	
	
	// Configuration methods ***********************************************************************
	
	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************
	
	
	@DataProvider(name="getNegativeRegisterAttemptsUsingProxyServerData")
	public Object[][] getNegativeRegisterAttemptsUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getNegativeRegisterAttemptsUsingProxyServerDataAsListOfLists());
	}
	protected List<List<Object>> getNegativeRegisterAttemptsUsingProxyServerDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		String basicauthproxyUrl = String.format("%s:%s", basicauthproxyHostname,basicauthproxyPort); basicauthproxyUrl = basicauthproxyUrl.replaceAll(":$", "");
		String noauthproxyUrl = String.format("%s:%s", noauthproxyHostname,noauthproxyPort); noauthproxyUrl = noauthproxyUrl.replaceAll(":$", "");
		String networkErrorMsg = "Network error, unable to connect to server. Please see "+clienttasks.rhsmLogFile+" for more information.";

		// String username, String password, ConsumerType type, String name, String consumerId, Boolean autosubscribe, Boolean force, String proxy, String proxyuser, String proxypassword, Integer exitCode, String stdoutRegex, String stderrRegex
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,	null,	null,	null,	null,	basicauthproxyUrl,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(0),		null,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,	null,	null,	null,	null,	"bad-proxy",			basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	networkErrorMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,	null,	null,	null,	null,	basicauthproxyUrl+"0",	basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	networkErrorMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,	null,	null,	null,	null,	basicauthproxyUrl,		"bad-username",				basicauthproxyPassword,	Integer.valueOf(255),	networkErrorMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,	null,	null,	null,	null,	basicauthproxyUrl,		basicauthproxyUsername,		"bad-password",			Integer.valueOf(255),	networkErrorMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,	null,	null,	null,	null,	basicauthproxyUrl,		null,						null,					Integer.valueOf(255),	networkErrorMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	"bad-password",	null,	null,	null,	null,	null,	basicauthproxyUrl,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	null,	"Invalid username or password"}));
		ll.add(Arrays.asList(new Object[]{	"bad-username",	clientpassword,	null,	null,	null,	null,	null,	basicauthproxyUrl,		basicauthproxyUsername,		basicauthproxyPassword,	Integer.valueOf(255),	null,	"Invalid username or password"}));

		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,	null,	null,	null,	null,	noauthproxyUrl,		null,		null,	Integer.valueOf(0),		null,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	clientpassword,	null,	null,	null,	null,	null,	noauthproxyUrl+"0",	null,		null,	Integer.valueOf(255),	networkErrorMsg,	null}));
		ll.add(Arrays.asList(new Object[]{	clientusername,	"bad-password",	null,	null,	null,	null,	null,	noauthproxyUrl,		null,		null,	Integer.valueOf(255),	null,	"Invalid username or password"}));
		ll.add(Arrays.asList(new Object[]{	"bad-username",	clientpassword,	null,	null,	null,	null,	null,	noauthproxyUrl,		null,		null,	Integer.valueOf(255),	null,	"Invalid username or password"}));

		return ll;
	}
}
