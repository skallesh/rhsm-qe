package rhsm.cli.tests;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"HealingTests","Tier2Tests"})
public class HealingTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************

	@Test(	description="a new system consumer's autoheal attribute defaults to true (on)",
			groups={},
			priority=100,
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutohealAttributeDefaultsToTrueForNewSystemConsumer_Test() throws Exception {
		
		// register a new consumer
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null,null, true, null, null, null, null));
		
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertTrue(jsonConsumer.getBoolean("autoheal"), "A new system consumer's autoheal attribute value defaults to true.");
	}
	
	@Test(	description="using the candlepin api, a consumer's autoheal attribute can be toggled off/on",
			groups={},
			priority=200, dependsOnMethods={"VerifyAutohealAttributeDefaultsToTrueForNewSystemConsumer_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutohealAttributeCanBeToggledOffForConsumerUsingCandlepinAPI_Test() throws Exception {
		
		// get the current registered consumer's id
		String consumerId = clienttasks.getCurrentConsumerId();
		
		JSONObject jsonConsumer = CandlepinTasks.setAutohealForConsumer(sm_clientUsername,sm_clientPassword, sm_serverUrl, consumerId,false);
		Assert.assertFalse(jsonConsumer.getBoolean("autoheal"), "A consumer's autoheal attribute value can be toggled off (expected value=false).");
		jsonConsumer = CandlepinTasks.setAutohealForConsumer(sm_clientUsername,sm_clientPassword, sm_serverUrl, consumerId,true);
		Assert.assertTrue(jsonConsumer.getBoolean("autoheal"), "A consumer's autoheal attribute value can be toggled on (expected value=true).");
	}
	
	
	@Test(	description="using autoheal module, a consumer's autoheal attribute can be toggled off/on",
			groups={"AcceptanceTests","Tier1Tests","blockedByBug-976867"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutohealAttributeCanBeToggledOffForConsumerUsingCLI_Test() throws Exception {
		SSHCommandResult result;
		JSONObject jsonConsumer;
		
		// register a new consumer
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null,null, true, null, null, null, null));
		
		// assert that the default results are equivalent to the show results
		Assert.assertEquals(
				clienttasks.autoheal(null, null, null, null, null, null).toString(),
				clienttasks.autoheal(true, null, null, null, null, null).toString(),
				"The default behavior should be --show.");
		
		// assert the disable option
		clienttasks.autoheal(null, null, true, null, null, null);
		// assert autoheal on the consumer
		jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertTrue(!jsonConsumer.getBoolean("autoheal"), "As seen by the server, consumer '"+consumerId+"' autoheal is off.");
		result = clienttasks.autoheal(true, null, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "Auto-attach preference: disabled", "Stdout from the auto-attach --show.");
		
		// assert the enable option
		clienttasks.autoheal(null, true, null, null, null, null);
		// assert autoheal on the consumer
		jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertTrue(jsonConsumer.getBoolean("autoheal"), "As seen by the server, consumer '"+consumerId+"' autoheal is on.");
		result = clienttasks.autoheal(true, null, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "Auto-attach preference: enabled", "Stdout from the auto-attach --show.");
	}
	
	
	@Test(	description="run auto-attach module without being registered",
			groups={"blockedByBug-976867"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutohealWithoutBeingRegistered_Test() throws Exception {
		
		// unregister
		clienttasks.unregister(null, null, null);
		
		// assert the disable option
		SSHCommandResult result = clienttasks.autoheal_(null, null, null, null, null, null);
		
		// assert that the default results are equivalent to the show results
		Assert.assertEquals(result.getStdout().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stdout from auto-heal without being registered.");
		Assert.assertEquals(result.getStderr().trim(),"", "Stderr from auto-attach without being registered.");
		Assert.assertEquals(result.getExitCode(),Integer.valueOf(255), "ExitCode from auto-attach without being registered.");
	}
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 744654 - [ALL LANG] [RHSM CLI]config module_ config Server port with blank or incorrect text produces traceback. https://github.com/RedHatQE/rhsm-qe/issues/148
	// TODO Cases in Bug 710172 - [RFE] Provide automated healing of expiring subscriptions//done https://github.com/RedHatQE/rhsm-qe/issues/149
	// TODO   subcase Bug 746088 - autoheal is not super-subscribing on the day the current entitlement cert expires //done
	// TODO   subcase Bug 746218 - auto-heal isn't working for partial subscription //done
	// TODO Cases in Bug 726411 - [RFE] Support for certificate healing https://github.com/RedHatQE/rhsm-qe/issues/150
	
	
	// Configuration methods ***********************************************************************


	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
