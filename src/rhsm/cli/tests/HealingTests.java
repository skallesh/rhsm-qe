package rhsm.cli.tests;

import org.json.JSONObject;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
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
@Test(groups={"HealingTests"})
public class HealingTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21583", "RHEL7-51500"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier2")
	@Test(	description="a new system consumer's autoheal attribute defaults to true (on)",
			groups={"Tier2Tests"},
			priority=100,
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testAutohealAttributeDefaultsToTrueForNewSystemConsumer() throws Exception {
		
		// register a new consumer
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null,null, true, null, null, null, null, null));
		
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertTrue(jsonConsumer.getBoolean("autoheal"), "A new system consumer's autoheal attribute value defaults to true.");
	}
	
	
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21584", "RHEL7-51501"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier2")
	@Test(	description="using the candlepin api, a consumer's autoheal attribute can be toggled off/on",
			groups={"Tier2Tests"},
			priority=200, dependsOnMethods={"testAutohealAttributeDefaultsToTrueForNewSystemConsumer"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testAutohealAttributeCanBeToggledOffForConsumerUsingCandlepinAPI() throws Exception {
		
		// get the current registered consumer's id
		String consumerId = clienttasks.getCurrentConsumerId();
		
		JSONObject jsonConsumer = CandlepinTasks.setAutohealForConsumer(sm_clientUsername,sm_clientPassword, sm_serverUrl, consumerId,false);
		Assert.assertFalse(jsonConsumer.getBoolean("autoheal"), "A consumer's autoheal attribute value can be toggled off (expected value=false).");
		jsonConsumer = CandlepinTasks.setAutohealForConsumer(sm_clientUsername,sm_clientPassword, sm_serverUrl, consumerId,true);
		Assert.assertTrue(jsonConsumer.getBoolean("autoheal"), "A consumer's autoheal attribute value can be toggled on (expected value=true).");
	}
	
	
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20096", "RHEL7-51103"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier1")
	@Test(	description="using autoheal module, a consumer's autoheal attribute can be toggled off/on",
			groups={"Tier1Tests","blockedByBug-976867"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testAutohealAttributeCanBeToggledOffForConsumerUsingCLI() throws Exception {
		SSHCommandResult result;
		JSONObject jsonConsumer;
		
		// register a new consumer
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null,null, true, null, null, null, null, null));
		
		// assert that the default results are equivalent to the show results
		Assert.assertEquals(
				clienttasks.autoheal(null, null, null, null, null, null, null).toString(),
				clienttasks.autoheal(true, null, null, null, null, null, null).toString(),
				"The default behavior should be --show.");
		
		// assert the disable option
		clienttasks.autoheal(null, null, true, null, null, null, null);
		// assert autoheal on the consumer
		jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertTrue(!jsonConsumer.getBoolean("autoheal"), "As seen by the server, consumer '"+consumerId+"' autoheal is off.");
		result = clienttasks.autoheal(true, null, null, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "Auto-attach preference: disabled", "Stdout from the auto-attach --show.");
		
		// assert the enable option
		clienttasks.autoheal(null, true, null, null, null, null, null);
		// assert autoheal on the consumer
		jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertTrue(jsonConsumer.getBoolean("autoheal"), "As seen by the server, consumer '"+consumerId+"' autoheal is on.");
		result = clienttasks.autoheal(true, null, null, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "Auto-attach preference: enabled", "Stdout from the auto-attach --show.");
	}
	
	
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21582", "RHEL7-51499"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags="Tier2")
	@Test(	description="run auto-attach module without being registered",
			groups={"Tier2Tests","blockedByBug-976867"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testAutohealWithoutBeingRegistered() throws Exception {
		
		// unregister
		clienttasks.unregister(null, null, null, null);
		
		// assert the disable option
		SSHCommandResult result = clienttasks.autoheal_(null, null, null, null, null, null, null);
		
		// assert that the default results are equivalent to the show results
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getStderr().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stderr from auto-heal without being registered.");
			Assert.assertEquals(result.getStdout().trim(),"", "Stdout from auto-attach without being registered.");
			Assert.assertEquals(result.getExitCode(),Integer.valueOf(1), "ExitCode from auto-attach without being registered.");
		} else {
			Assert.assertEquals(result.getStdout().trim(),"This system is not yet registered. Try 'subscription-manager register --help' for more information.", "Stdout from auto-heal without being registered.");
			Assert.assertEquals(result.getStderr().trim(),"", "Stderr from auto-attach without being registered.");
			Assert.assertEquals(result.getExitCode(),Integer.valueOf(255), "ExitCode from auto-attach without being registered.");
		}
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
