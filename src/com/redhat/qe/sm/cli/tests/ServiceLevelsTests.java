package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.ContentNamespace;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.Repo;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.sm.data.YumRepo;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * Reference: https://engineering.redhat.com/trac/Entitlement/wiki/SlaSubscribe
 */

//Notes
/*
<jsefler> jbowes, I'd like to get something started today on the new --servicelevel option for subscribe --auto.  Any pointers?
How do I know what the valid values are?  Do I need to search through the attributes on the pools?
<dgoodwin> jsefler: curl /owners/key/servicelevels will show you everything valid for that org
<jsefler> perfect
<jsefler> dgoodwin, so service levels are attached to the owners.  I guess they get added and deleted with some PUT calls?
<dgoodwin> jsefler: nah they're just an attribute on products, so the api call is just scanning for all the service levels available for that orgs subs
<jsefler> ok
<dgoodwin> you'll see some support_level attributes in our json test data
<dgoodwin> that's where they come from
<jsefler> for subscribe --auto, I assume there is an order of preference for choosing a subscription based on service level agreements?  or if I specify --servicelevel then do I ONLY get a matching subscription with that service level?
<dgoodwin> jsefler: you only get subs from that support level
dgoodwin dgregor
jsefler: https://engineering.redhat.com/trac/Entitlement/wiki/SlaSubscribe will help explain some things
<jsefler> thank you dgoodwin - that should get me started
<dgoodwin> jsefler: also note that --servicelevel = set it on the consumer as a part of this operation
<dgoodwin> and once it's set, any autosub/heal will use it
even if it's not specified on the cli
--- mstead is now known as mstead-afk
dgoodwin dgregor
<dgoodwin> i kinda wonder if it should be subscription-manager servicelevel --set=SLA
as it's not entirely clear that i'm persisting something that will stick when i do subscribe --auto --servicelevel=SLA
<jsefler> dgoodwin, so I guess it is also an option on register?   and if I specify it on the subscribe line then does that value override what I set during register?
<dgoodwin> yeah it's there as well
any time you specify it it will override previous value
<jsefler> thanks
*/

@Test(groups={"ServiceLevelsTests"})
public class ServiceLevelsTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager: service-level (when not registered)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ServiceLevelWhenNotRegistered_Test() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null);
		
		SSHCommandResult result;
		
		// with credentials
		result = clienttasks.service_level_(null, null, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level (implies --show) without being registered");
		Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --show without being registered");
		
		// without credentials
		result = clienttasks.service_level_(null, null, null, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level (implies --show) without being registered");
		Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --show without being registered");
	}
	
	
	@Test(	description="subscription-manager: service-level --show (when not registered)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ServiceLevelShowWhenNotRegistered_Test() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null);
		
		SSHCommandResult result;
		
		// with credentials
		result = clienttasks.service_level_(true, null, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --show without being registered");
		Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --show without being registered");
		
		// without credentials
		result = clienttasks.service_level_(true, null, null, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --show without being registered");
		Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --show without being registered");
	}
	
	@Test(	description="subscription-manager: service-level --list (when not registered)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ServiceLevelListWhenNotRegistered_Test() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null);
		
		SSHCommandResult result = clienttasks.service_level_(null, true, null, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --list without being registered");
		Assert.assertEquals(result.getStdout().trim(),"Error: you must register or specify --username and password to list service levels", "Stdout from service-level --list without being registered");
	}
	
	
	@Test(	description="subscription-manager: service-level --list (with invalid credentials)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ServiceLevelListWithInvalidCredentials_Test() {
		String x = String.valueOf(getRandInt());
		SSHCommandResult result;
				
		// test while unregistered
		clienttasks.unregister(null, null, null);
		result = clienttasks.service_level_(null, true, sm_clientUsername, sm_clientPassword+x, sm_clientOrg, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --list with invalid credentials");
		Assert.assertEquals(result.getStderr().trim(), servertasks.invalidCredentialsMsg(), "Stderr from service-level --list with invalid credentials");
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from service-level --list with invalid credentials");

		// test while registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,(List<String>)null,null,null,null,null,null);
		result = clienttasks.service_level_(null, true, sm_clientUsername, sm_clientPassword+x, sm_clientOrg, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --list with invalid credentials");
		Assert.assertEquals(result.getStderr().trim(), servertasks.invalidCredentialsMsg(), "Stderr from service-level --list with invalid credentials");
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from service-level --list with invalid credentials");
	}
	
	
	@Test(	description="subscription-manager: service-level --list (with invalid org)",
			groups={/*"blockedByBug-796468"*/},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ServiceLevelListWithInvalidOrg_Test() {
		String x = String.valueOf(getRandInt());
		SSHCommandResult result;
				
		// test while unregistered
		clienttasks.unregister(null, null, null);
		result = clienttasks.service_level_(null, true, sm_clientUsername, sm_clientPassword, sm_clientOrg+x, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --list with invalid org");
		Assert.assertEquals(result.getStderr().trim(), String.format("Owner with id %s could not be found",sm_clientOrg+x), "Stderr from service-level --list with invalid org");
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from service-level --list with invalid credentials");

		// test while registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,(List<String>)null,null,null,null,null,null);
		result = clienttasks.service_level_(null, true, sm_clientUsername, sm_clientPassword, sm_clientOrg+x, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --list with invalid org");
		Assert.assertEquals(result.getStderr().trim(), String.format("Owner with id %s could not be found",sm_clientOrg+x), "Stderr from service-level --list with invalid org");
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from service-level --list with invalid credentials");
	}
	
	
	
	@Test(	description="subscription-manager: service-level --show (after registering without a service level)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ServiceLevelShowAfterRegisteringWithoutServiceLevel_Test() throws JSONException, Exception  {
		SSHCommandResult result;
				
		// register with no service-level
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,(List<String>)null,true,null,null,null,null));
		
		// get the current consumer object and assert that the serviceLevel persisted
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertEquals(jsonConsumer.get("serviceLevel"), JSONObject.NULL, "The call to register without a servicelevel leaves the current consumer object serviceLevel attribute value null.");
		
		result = clienttasks.service_level(true, false, null, null, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "Current service level:", "When the system has been registered without a service level, the current service level should be null.");
	}
	
	
//	@Test(	description="subscription-manager: service-level --show (after registering with a valid service level)",
//			groups={"debugTest"},
//			enabled=true)
//	//@ImplementsNitrateTest(caseId=)
//	public void ServiceLevelShowAfterRegisteringWithValidServiceLevel_Test(TODO) throws JSONException, Exception  {
//		SSHCommandResult result;
//				
//		// register with no service-level
//		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,(List<String>)null,true,null,null,null,null));
//		
//		// get the current consumer object and assert that the serviceLevel persisted
//		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
//		Assert.assertEquals(jsonConsumer.get("serviceLevel"), JSONObject.NULL, "The call to register without a servicelevel leaves the current consumer object serviceLevel attribute value null.");
//		
//		result = clienttasks.service_level(true, false, null, null, null, null, null, null);
//		Assert.assertEquals(result.getStdout().trim(), "Current service level:", "When the system has been registered without a service level, the current service level should be null.");
//	}
	

	// Candidates for an automated Test:
		
	// Configuration methods ***********************************************************************


	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************
	

}
