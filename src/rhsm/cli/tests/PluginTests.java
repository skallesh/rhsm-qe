package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author jsefler
 *
 * The strategy for these tests is to exercise the test-plugins which are based on the
 * example-plugins found in the upstream subscription-manager git repository:
 * https://github.com/candlepin/subscription-manager/tree/master/example-plugins
 * 
 * Design Document:
 * https://engineering.redhat.com/trac/Entitlement/wiki/SubscriptionManagerPlugins
 */
@Test(groups={"PluginTests"})
public class PluginTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************
	
	
	// UninstalledPlugin Tests ************************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20038", "RHEL7-51047"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="execute subscription-manager plugins --listslots",
			groups={"Tier1Tests"},
			priority=10, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListslots() {
		List<String> actualSlots = Arrays.asList(clienttasks.plugins(null,true,null,null).getStdout().trim().split("\n"));
		Assert.assertTrue(actualSlots.containsAll(slots), "All of these expected slots are listed: "+slots);
		Assert.assertTrue(slots.containsAll(actualSlots), "Only these expected slots are listed: "+slots);
		actualSlots = Arrays.asList(clienttasks.plugins(null,true,null,true).getStdout().trim().split("\n"));
		Assert.assertTrue(actualSlots.containsAll(slots)&&slots.containsAll(actualSlots), "Including --verbose option should produce the same list of slots.");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20039", "RHEL7-51048"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="execute subscription-manager plugins --list (with no plugins installed)",
			groups={"Tier1Tests"},
			priority=20, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListWithNoPluginsInstalled() {
		Assert.assertEquals(clienttasks.plugins(null,null,null,null).getStdout().trim(), "", "Stdout from the plugins command indicates no plugins are installed.");
		Assert.assertEquals(clienttasks.plugins(true,null,null,null).getStdout().trim(), "", "Stdout from the plugins --list command indicates no plugins are installed.");
		Assert.assertEquals(clienttasks.plugins(null,null,null,true).getStdout().trim(), "", "Stdout from the plugins command indicates no plugins are installed (--verbose has no affect).");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20040", "RHEL7-51049"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="execute subscription-manager plugins --listhooks (with no plugins installed)",
			groups={"Tier1Tests"},
			priority=30, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListhooksWithNoPluginsInstalled() {
		List<String> plugins_listslots			= Arrays.asList(clienttasks.plugins(null,true,null,null).getStdout().trim().split("\n"));
		List<String> plugins_listhooks			= Arrays.asList(clienttasks.plugins(null,null,true,null).getStdout().trim().split("\n"));
		List<String> plugins_listhooks_verbose	= Arrays.asList(clienttasks.plugins(null,null,true,true).getStdout().trim().split("\n"));
		
		Assert.assertTrue(plugins_listslots.containsAll(plugins_listhooks) && plugins_listhooks.containsAll(plugins_listslots),
				"When there are no plugins installed, the expected --listhooks report should be identical to the --listslots.");
		Assert.assertTrue(plugins_listslots.containsAll(plugins_listhooks_verbose) && plugins_listhooks_verbose.containsAll(plugins_listslots),
				"When there are no plugins installed, the expected --listhooks --verbose report should be identical to the --listslots.");
	}
	
	
	
	// DisabledPlugin Tests ***************************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20041", "RHEL7-51050"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="execute subscription-manager plugins --list (with plugins installed and disabled)",
			groups={"Tier1Tests","DisabledPluginTests"},
			priority=110, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListWithDisabledPluginsInstalled() {
		if (installedPlugins.isEmpty()) throw new SkipException("There are no plugins installed to test.");
		
		// assert each of the installed plugins is reported as disabled
		SSHCommandResult pluginsListResult = clienttasks.plugins(null,null,null,null);
		for (Plugin installedPlugin : installedPlugins) {
			String expectedPluginReport = getExpectedPluginListReport(installedPlugin, "disabled", false);
			Assert.assertTrue(pluginsListResult.getStdout().contains(expectedPluginReport),
					"Stdout from the plugins list command reports expected plugin report '"+expectedPluginReport+"'.");
		}
		
		// assert each of the installed plugins is verbosely reported as disabled
		SSHCommandResult pluginsListVerboseResult = clienttasks.plugins(true,null,null,true);
		for (Plugin installedPlugin : installedPlugins) {
			String expectedPluginReport = getExpectedPluginListReport(installedPlugin, "disabled", true);
			Assert.assertTrue(pluginsListVerboseResult.getStdout().contains(expectedPluginReport),
					"Stdout from the verbose plugins list command reports expected plugin report: \n"+expectedPluginReport);
		}

	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20042", "RHEL7-51051"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="execute subscription-manager plugins --listhooks (with plugins installed and disabled)",
			groups={"Tier1Tests","DisabledPluginTests"},
			priority=120, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListhooksWithDisabledPluginsInstalled() {
		List<String> plugins_listslots			= Arrays.asList(clienttasks.plugins(null,true,null,null).getStdout().trim().split("\n"));
		List<String> plugins_listhooks			= Arrays.asList(clienttasks.plugins(null,null,true,null).getStdout().trim().split("\n"));
		List<String> plugins_listhooks_verbose	= Arrays.asList(clienttasks.plugins(null,null,true,true).getStdout().trim().split("\n"));
		
		Assert.assertTrue(plugins_listslots.containsAll(plugins_listhooks) && plugins_listhooks.containsAll(plugins_listslots),
				"When all plugins installed are disabled, the expected --listhooks report should be identical to the --listslots.");
		Assert.assertTrue(plugins_listslots.containsAll(plugins_listhooks_verbose) && plugins_listhooks_verbose.containsAll(plugins_listslots),
				"When all plugins installed are disabled, the expected --listhooks --verbose report should be identical to the --listslots.");
	}
	
	
	
	// RegisterConsumerTestPlugin Tests ***************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20043", "RHEL7-51052"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="enable the RegisterConsumerTestPlugin and assert the plugins list reports enablement",
			groups={"Tier1Tests"},
			priority=210, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListWithEnabledRegisterConsumerTestPlugin_Test1() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("register_consumer_test1.RegisterConsumerTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The RegisterConsumerTestPlugin 1 is installed.");
		
		// enable and verify
		enableTestPluginAndVerifyListReport(installedPlugin);
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20044", "RHEL7-51053"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify plugins listhooks reports all of the expected hooks for RegisterConsumerTestPlugin",
			groups={"Tier1Tests"},
			priority=220, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListhooksWithEnabledRegisterConsumerTestPlugin_Test1() {
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("register_consumer_test1.RegisterConsumerTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The RegisterConsumerTestPlugin 1 is installed.");
		
		// hand populate the expected hooks (look for the python def methods inside the plugin.py file and prefix it with the plugin key.)
		String expectedPreRegisterConsumerHook = installedPlugin.key+".pre_register_consumer_hook";		//    def pre_register_consumer_hook(self, conduit):
		String expectedPostRegisterConsumerHook = installedPlugin.key+".post_register_consumer_hook";	//    def post_register_consumer_hook(self, conduit):

		// verify the plugins --listhooks reports that the pre and post slots contain the expected RegisterConsumerTestPlugin hooks
		SSHCommandResult pluginsListhooksResult = clienttasks.plugins(false, false, true, false);
		List<String> actualPreRegisterConsumerHooks = parseHooksForSlotFromPluginsListhooksResult("pre_register_consumer", pluginsListhooksResult);
		Assert.assertTrue(actualPreRegisterConsumerHooks.contains(expectedPreRegisterConsumerHook),"The plugins listhooks reports expected pre_register_consumer hook '"+expectedPreRegisterConsumerHook+"'.");
		List<String> actualPostRegisterConsumerHooks = parseHooksForSlotFromPluginsListhooksResult("post_register_consumer", pluginsListhooksResult);
		Assert.assertTrue(actualPostRegisterConsumerHooks.contains(expectedPostRegisterConsumerHook),"The plugins listhooks report expected post_register_consumer hook '"+expectedPostRegisterConsumerHook+"'.");
		
		// assert the remaining slots have no hooks because RegisterConsumerTestPlugin is the first enabled plugin
		for (String slot : slots) {
			if (slot.equals("pre_register_consumer")) continue;
			if (slot.equals("post_register_consumer")) continue;
			Assert.assertTrue(parseHooksForSlotFromPluginsListhooksResult(slot, pluginsListhooksResult).isEmpty(),
					"The plugins listhooks reports no hooks for slot '"+slot+"' because RegisterConsumerTestPlugin is the only enabled plugin.");
		}
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20045", "RHEL7-51054"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="execute subscription-manager modules and verify the expected RegisterConsumerTestPlugin hooks are called",
			groups={"Tier1Tests"},
			priority=230, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testEnabledRegisterConsumerTestPluginHooksAreCalled_Test1() {
		truncateRhsmLog();
		
		// get the pre-registered facts on the system
		clienttasks.unregister(null,null,null, null);
		Map<String,String> facts = clienttasks.getFacts();
		
		// get the current rhsm logging level; INFO or DEBUG
		String rhsmLogLevel=null;
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.10-1")) {	// RHEL7.3 commit d84b15f42c2e4521e130b939039960c0846b849c 1334916: Move logging configuration to rhsm.conf
			rhsmLogLevel = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "logging", "default_log_level");
		} else if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.14.2-1")) {	// commit 66aafd77dc629b921379f0e121421c1c21c0b787 Move to fileConfig based logging.
			rhsmLogLevel = clienttasks.getConfFileParameter(clienttasks.rhsmLoggingConfFile, "handler_rhsm_log", "level");
		} else {
			rhsmLogLevel="DEBUG";	 // default
		}
		
		// mark the rhsm.log file
		String logMarker = System.currentTimeMillis()+" Testing verifyEnabledRegisterConsumerTestPluginHooksAreCalled_Test1...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);

		// register and assert the pre and post hooks are logged
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg));
		//sleep(5000);	// give the plugin hooks a chance to be called; I think this is an async process


		// get the tail of the marked rhsm.log file
		//	2013-03-14 13:37:45,277 [DEBUG]  @plugins.py:695 - Running pre_register_consumer_hook in register_consumer1.RegisterConsumerTestPlugin
		//	2013-03-14 13:37:45,277 [INFO]  @register_consumer1.py:30 - Running pre_register_consumer_hook 1: system name jsefler-7.usersys.redhat.com is about to be registered.
		//	2013-03-14 13:37:45,278 [INFO]  @register_consumer1.py:31 - Running pre_register_consumer_hook 1: consumer facts count is 99
		//
		//	2013-03-14 13:37:45,782 [DEBUG]  @plugins.py:695 - Running post_register_consumer_hook in register_consumer1.RegisterConsumerTestPlugin
		//	2013-03-14 13:37:45,782 [INFO]  @register_consumer1.py:39 - Running post_register_consumer_hook 1: consumer uuid 48db2a61-9af2-46ed-966a-374e0a94f9c4 is now registered.
		
		// assert the expected log calls
		String logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Running p").trim();
		List<String> expectedLogInfo = new ArrayList<String>();
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running pre_register_consumer_hook in register_consumer_test1.RegisterConsumerTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running pre_register_consumer_hook 1: system name "+clienttasks.hostname+" is about to be registered.");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running pre_register_consumer_hook 1: consumer facts count is "+facts.values().size());
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running post_register_consumer_hook in register_consumer_test1.RegisterConsumerTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_register_consumer_hook 1: consumer uuid "+consumerId+" is now registered.");
		if (              logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*") == false) RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, null);	// used for debugging
		if (              logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*") == false) clienttasks.plugins_(null,null,null,true);	// used for debugging
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"), "The '"+clienttasks.rhsmLogFile+"' reports expected log messages: "+expectedLogInfo);
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20046", "RHEL7-51055"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="enable another RegisterConsumerTestPlugin and assert the plugins list reports enablement",
			groups={"Tier1Tests"},
			priority=240, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListWithEnabledRegisterConsumerTestPlugin_Test2() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("register_consumer_test2.RegisterConsumerTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The RegisterConsumerTestPlugin 2 is installed.");
		
		// enable and verify
		enableTestPluginAndVerifyListReport(installedPlugin);
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20047", "RHEL7-51056"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify plugins listhooks reports all of the expected hooks for another RegisterConsumerTestPlugin",
			groups={"Tier1Tests"},
			priority=250, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListhooksWithEnabledRegisterConsumerTestPlugin_Test2() {
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("register_consumer_test2.RegisterConsumerTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The RegisterConsumerTestPlugin is installed.");
		
		// hand populate the expected hooks (look for the python def methods inside the plugin.py file and prefix it with the plugin key.)
		String expectedPreRegisterConsumerHook = installedPlugin.key+".pre_register_consumer_hook";		//    def pre_register_consumer_hook(self, conduit):
		String expectedPostRegisterConsumerHook = installedPlugin.key+".post_register_consumer_hook";	//    def post_register_consumer_hook(self, conduit):

		// verify the plugins --listhooks reports that the pre and post slots contain the expected hooks
		SSHCommandResult pluginsListhooksResult = clienttasks.plugins(false, false, true, false);
		List<String> actualPreRegisterConsumerHooks = parseHooksForSlotFromPluginsListhooksResult("pre_register_consumer", pluginsListhooksResult);
		Assert.assertTrue(actualPreRegisterConsumerHooks.contains(expectedPreRegisterConsumerHook),"The plugins listhooks reports expected pre_register_consumer hook '"+expectedPreRegisterConsumerHook+"'.");
		List<String> actualPostRegisterConsumerHooks = parseHooksForSlotFromPluginsListhooksResult("post_register_consumer", pluginsListhooksResult);
		Assert.assertTrue(actualPostRegisterConsumerHooks.contains(expectedPostRegisterConsumerHook),"The plugins listhooks report expected post_register_consumer hook '"+expectedPostRegisterConsumerHook+"'.");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20048", "RHEL7-51057"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="execute subscription-manager modules and verify the expected RegisterConsumerTestPlugin hooks are called",
			groups={"Tier1Tests"},
			priority=260, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testEnabledRegisterConsumerTestPluginHooksAreCalled_Test2() {
		truncateRhsmLog();
		
		// get the pre-registered facts on the system
		clienttasks.unregister(null,null,null, null);
		Map<String,String> facts = clienttasks.getFacts();
		
		// get the current rhsm logging level; INFO or DEBUG
		String rhsmLogLevel=null;
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.10-1")) {	// RHEL7.3 commit d84b15f42c2e4521e130b939039960c0846b849c 1334916: Move logging configuration to rhsm.conf
			rhsmLogLevel = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "logging", "default_log_level");
		} else if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.14.2-1")) {	// commit 66aafd77dc629b921379f0e121421c1c21c0b787 Move to fileConfig based logging.
			rhsmLogLevel = clienttasks.getConfFileParameter(clienttasks.rhsmLoggingConfFile, "handler_rhsm_log", "level");
		} else {
			rhsmLogLevel="DEBUG";	 // default
		}
		
		// mark the rhsm.log file
		String logMarker = System.currentTimeMillis()+" Testing verifyEnabledRegisterConsumerTestPluginHooksAreCalled_Test2...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);

		// register and assert the pre and post hooks are logged
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg));
		//sleep(5000);	// give the plugin hooks a chance to be called; I think this is an async process

		// get the tail of the marked rhsm.log file
		//	2013-03-14 13:37:45,277 [DEBUG]  @plugins.py:695 - Running pre_register_consumer_hook in register_consumer1.RegisterConsumerTestPlugin
		//	2013-03-14 13:37:45,277 [INFO]  @register_consumer1.py:30 - Running pre_register_consumer_hook: system name jsefler-7.usersys.redhat.com is about to be registered.
		//	2013-03-14 13:37:45,278 [INFO]  @register_consumer1.py:31 - Running pre_register_consumer_hook: consumer facts count is 99
		//
		//	2013-03-14 13:37:45,782 [DEBUG]  @plugins.py:695 - Running post_register_consumer_hook in register_consumer1.RegisterConsumerTestPlugin
		//	2013-03-14 13:37:45,782 [INFO]  @register_consumer1.py:39 - Running post_register_consumer_hook: consumer uuid 48db2a61-9af2-46ed-966a-374e0a94f9c4 is now registered.
		
		// assert the expected log calls
		String logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Running p").trim();
		List<String> expectedLogInfo = new ArrayList<String>();
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running pre_register_consumer_hook in register_consumer_test1.RegisterConsumerTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running pre_register_consumer_hook 1: system name "+clienttasks.hostname+" is about to be registered.");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running pre_register_consumer_hook 1: consumer facts count is "+facts.values().size());
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running pre_register_consumer_hook in register_consumer_test2.RegisterConsumerTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running pre_register_consumer_hook 2: system name "+clienttasks.hostname+" is about to be registered.");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running pre_register_consumer_hook 2: consumer facts count is "+facts.values().size());
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running post_register_consumer_hook in register_consumer_test1.RegisterConsumerTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_register_consumer_hook 1: consumer uuid "+consumerId+" is now registered.");
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running post_register_consumer_hook in register_consumer_test2.RegisterConsumerTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_register_consumer_hook 2: consumer uuid "+consumerId+" is now registered.");
		if (              logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*") == false) RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, null);	// used for debugging
		if (              logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*") == false) clienttasks.plugins_(null,null,null,true);	// used for debugging
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"), "The '"+clienttasks.rhsmLogFile+"' reports expected log messages: "+expectedLogInfo);
	}
	
	
	// FactsCollectionTestPlugin Tests ****************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20049", "RHEL7-51058"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="enable FactsCollectionTestPlugin and assert the plugins list reports enablement",
			groups={"Tier1Tests"},
			priority=310, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListWithEnabledFactsCollectionTestPlugin() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("facts_collection_test.FactsCollectionTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The FactsCollectionTestPlugin is installed.");
		
		// enable and verify
		enableTestPluginAndVerifyListReport(installedPlugin);
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20050", "RHEL7-51059"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify plugins listhooks reports all of the expected hooks for FactsCollectionTestPlugin",
			groups={"Tier1Tests"},
			priority=320, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListhooksWithEnabledFactsCollectionTestPlugin() {
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("facts_collection_test.FactsCollectionTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The FactsCollectionTestPlugin is installed.");
		
		// hand populate the expected hooks (look for the python def methods inside the plugin.py file and prefix it with the plugin key.)
		String expectedPostFactsCollectionHook = installedPlugin.key+".post_facts_collection_hook";	//    def post_facts_collection_hook(self, conduit):

		// verify the plugins --listhooks reports that the pre and post slots contain the expected hooks
		SSHCommandResult pluginsListhooksResult = clienttasks.plugins(false, false, true, false);
		List<String> actualPostRegisterConsumerHooks = parseHooksForSlotFromPluginsListhooksResult("post_facts_collection", pluginsListhooksResult);
		Assert.assertTrue(actualPostRegisterConsumerHooks.contains(expectedPostFactsCollectionHook),"The plugins listhooks report expected post_facts_collection hook '"+expectedPostFactsCollectionHook+"'.");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20051", "RHEL7-51060"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="execute subscription-manager modules and verify the expected FactsCollectionTestPlugin hooks are called",
			groups={"Tier1Tests"},
			priority=330, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testEnabledFactsCollectionTestPluginHooksAreCalled() {
		truncateRhsmLog();
		
		// get the pre-registered facts on the system
		clienttasks.unregister(null,null,null, null);
		Map<String,String> facts = clienttasks.getFacts();
		
		// get the current rhsm logging level; INFO or DEBUG
		String rhsmLogLevel=null;
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.10-1")) {	// RHEL7.3 commit d84b15f42c2e4521e130b939039960c0846b849c 1334916: Move logging configuration to rhsm.conf
			rhsmLogLevel = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "logging", "default_log_level");
		} else if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.14.2-1")) {	// commit 66aafd77dc629b921379f0e121421c1c21c0b787 Move to fileConfig based logging.
			rhsmLogLevel = clienttasks.getConfFileParameter(clienttasks.rhsmLoggingConfFile, "handler_rhsm_log", "level");
		} else {
			rhsmLogLevel="DEBUG";	 // default
		}
		
		// mark the rhsm.log file
		String logMarker = System.currentTimeMillis()+" Testing verifyEnabledFactsCollectionTestPluginHooksAreCalled_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);

		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg));
		//sleep(5000);	// give the plugin hooks a chance to be called; I think this is an async process

		// get the tail of the marked rhsm.log file
		//	2013-03-14 18:47:39,595 [DEBUG]  @plugins.py:695 - Running post_facts_collection_hook in facts_collection.FactsCollectionTestPlugin
		//	2013-03-14 18:47:39,595 [INFO]  @facts_collection.py:33 - Running post_facts_collection_hook: consumer facts count is 100
		String logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Running post_facts_collection_hook").trim();

		// assert the expected log calls
		List<String> expectedLogInfo = new ArrayList<String>();
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running post_facts_collection_hook in facts_collection_test.FactsCollectionTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_facts_collection_hook: consumer facts count is "+facts.values().size());
		if (              logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*") == false) RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, null);	// used for debugging
		if (              logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*") == false) clienttasks.plugins_(null,null,null,true);	// used for debugging
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"), "The '"+clienttasks.rhsmLogFile+"' reports expected log messages: "+expectedLogInfo);
		
		// add 1 extra post_facts_collection_hook_fact and run update
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("post_facts_collection_hook_fact", "callback test");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		logMarker = System.currentTimeMillis()+" Testing verifyEnabledFactsCollectionTestPluginHooksAreCalled_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);
		clienttasks.facts(null,true,null,null,null, null);
		sleep(5000);	// give the plugin hooks a chance to be called; I think this is an async process
		
		// assert the expected log calls
		RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Debugging post_facts_collection_hook").trim();	// can be used to troubleshoot a failure in the following consumer facts count assertion
		logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Running post_facts_collection_hook").trim();
		expectedLogInfo.clear();
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running post_facts_collection_hook in facts_collection_test.FactsCollectionTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_facts_collection_hook: consumer facts count is "+(facts.values().size()+1));			
		if (              logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*") == false) RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, null);	// used for debugging
		if (              logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*") == false) clienttasks.plugins_(null,null,null,true);	// used for debugging
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"), "The '"+clienttasks.rhsmLogFile+"' reports expected log messages: "+expectedLogInfo);
	}
	
	
	// SubscribeTestPlugin Tests ********************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20052", "RHEL7-51061"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="enable SubscribeTestPlugin and assert the plugins list reports enablement",
			groups={"Tier1Tests"},
			priority=410, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListWithEnabledSubscribeTestPlugin() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("subscribe_test.SubscribeTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The SubscribePlugintestPlugin is installed.");
		
		// enable and verify
		enableTestPluginAndVerifyListReport(installedPlugin);
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20053", "RHEL7-33083"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify plugins listhooks reports all of the expected hooks for SubscribeTestPlugin",
			groups={"Tier1Tests"},
			priority=420, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListhooksWithEnabledSubscribeTestPlugin() {
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("subscribe_test.SubscribeTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The SubscribeTestPlugin is installed.");
		
		// hand populate the expected hooks (look for the python def methods inside the plugin.py file and prefix it with the plugin key.)
		String expectedPreHook = installedPlugin.key+".pre_subscribe_hook";		//    def pre_subscribe_hook(self, conduit):
		String expectedPostHook = installedPlugin.key+".post_subscribe_hook";	//    def post_subscribe_hook(self, conduit):

		// verify the plugins --listhooks reports that the pre and post slots contain the expected hooks
		SSHCommandResult pluginsListhooksResult = clienttasks.plugins(false, false, true, false);
		List<String> actualPreHooks = parseHooksForSlotFromPluginsListhooksResult("pre_subscribe", pluginsListhooksResult);
		Assert.assertTrue(actualPreHooks.contains(expectedPreHook),"The plugins listhooks report expected pre_subscribe hook '"+expectedPreHook+"'.");
		pluginsListhooksResult = clienttasks.plugins(false, false, true, false);
		List<String> actualPostHooks = parseHooksForSlotFromPluginsListhooksResult("post_subscribe", pluginsListhooksResult);
		Assert.assertTrue(actualPostHooks.contains(expectedPostHook),"The plugins listhooks report expected post_subscribe hook '"+expectedPostHook+"'.");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20054", "RHEL7-51062"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="execute subscription-manager modules and verify the expected SubscribeTestPlugin hooks are called",
			groups={"Tier1Tests"},
			priority=430, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testEnabledSubscribeTestPluginHooksAreCalled() throws JSONException, Exception {
		truncateRhsmLog();
		
		// get the pre-registered facts on the system
		clienttasks.unregister(null,null,null, null);
		Map<String,String> facts = clienttasks.getFacts();
		
		// get the current rhsm logging level; INFO or DEBUG
		String rhsmLogLevel=null;
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.10-1")) {	// RHEL7.3 commit d84b15f42c2e4521e130b939039960c0846b849c 1334916: Move logging configuration to rhsm.conf
			rhsmLogLevel = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "logging", "default_log_level");
		} else if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.14.2-1")) {	// commit 66aafd77dc629b921379f0e121421c1c21c0b787 Move to fileConfig based logging.
			rhsmLogLevel = clienttasks.getConfFileParameter(clienttasks.rhsmLoggingConfFile, "handler_rhsm_log", "level");
		} else {
			rhsmLogLevel="DEBUG";	 // default
		}
		
		// register and get the current available subscription list
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true,null,null,null,null, null));
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("Cannot randomly pick a pool for subscribing when there are no available pools for testing."); 
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		String quantity = null; if (pool.suggested<1) quantity = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "instance_multiplier"); 	// when the Suggested quantity is 0, let's specify a quantity to avoid Stdout: Quantity '1' is not a multiple of instance multiplier '2'

		// mark the rhsm.log file
		String logMarker = System.currentTimeMillis()+" Testing verifyEnabledSubscribeTestPluginHooksAreCalled_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);

		// subscribe to a random pool (to generate calls to pre/post hooks)
		clienttasks.subscribe(null,null,pool.poolId,null,null,quantity,null,null,null,null,null, null, null);
		//sleep(5000);	// give the plugin hooks a chance to be called; I think this is an async process

		// get the tail of the marked rhsm.log file
		String logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Running p").trim();
		
		//	2013-03-16 11:50:13,462 [DEBUG]  @plugins.py:695 - Running post_facts_collection_hook in facts_collection_test.FactsCollectionTestPlugin
		//	2013-03-16 11:50:13,462 [INFO]  @facts_collection_test.py:33 - Running post_facts_collection_hook: consumer facts count is 100
		//	2013-03-16 11:50:13,750 [DEBUG]  @plugins.py:695 - Running pre_subscribe_hook in subscribe_test.SubscribeTestPlugin
		//	2013-03-16 11:50:13,751 [INFO]  @subscribe_test.py:30 - Running pre_subscribe_hook: system is about to subscribe.
		//	2013-03-16 11:50:14,540 [DEBUG]  @plugins.py:695 - Running post_subscribe_hook in subscribe_test.SubscribeTestPlugin
		//	2013-03-16 11:50:14,540 [INFO]  @subscribe_test.py:38 - Running post_subscribe_hook: system just subscribed.
		
		// assert the pre/post_subscribe_hooks are called
		List<String> expectedLogInfo = new ArrayList<String>();
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running post_facts_collection_hook in facts_collection_test.FactsCollectionTestPlugin");	// enabled in prior FactsCollectionTestPlugin Tests 
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_facts_collection_hook: consumer facts count is "+facts.values().size());	// enabled in prior FactsCollectionTestPlugin Tests 
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running pre_subscribe_hook in subscribe_test.SubscribeTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running pre_subscribe_hook: system is about to subscribe");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running pre_subscribe_hook: subscribing consumer is "+consumerId);
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running post_subscribe_hook in subscribe_test.SubscribeTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_subscribe_hook: system just subscribed");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_subscribe_hook: subscribed consumer is "+consumerId);
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_subscribe_hook: subscribed from pool id "+pool.poolId);
		if (              logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*") == false) RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, null);	// used for debugging
		if (              logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*") == false) clienttasks.plugins_(null,null,null,true);	// used for debugging
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"), "The '"+clienttasks.rhsmLogFile+"' reports log messages: "+expectedLogInfo);
	}
	
	
	// AutoAttachTestPlugin Tests ********************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20055", "RHEL7-51063"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="enable AutoAttachTestPlugin and assert the plugins list reports enablement",
			groups={"Tier1Tests"},
			priority=510, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListWithEnabledAutoAttachTestPlugin() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("auto_attach_test.AutoAttachTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The AutoAttachTestPlugin is installed.");
		
		// enable and verify
		enableTestPluginAndVerifyListReport(installedPlugin);
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20056", "RHEL7-51064"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify plugins listhooks reports all of the expected hooks for AutoAttachTestPlugin",
			groups={"Tier1Tests"},
			priority=520, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListhooksWithEnabledAutoAttachTestPlugin() {
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("auto_attach_test.AutoAttachTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The AutoAttachTestPlugin is installed.");
		
		// hand populate the expected hooks (look for the python def methods inside the plugin.py file and prefix it with the plugin key.)
		String expectedPreHook = installedPlugin.key+".pre_auto_attach_hook";	//    def pre_auto_attach_hook(self, conduit):
		String expectedPostHook = installedPlugin.key+".post_auto_attach_hook";	//    def post_auto_attach_hook(self, conduit):

		// verify the plugins --listhooks reports that the pre and post slots contain the expected hooks
		SSHCommandResult pluginsListhooksResult = clienttasks.plugins(false, false, true, false);
		List<String> actualPreHooks = parseHooksForSlotFromPluginsListhooksResult("pre_auto_attach", pluginsListhooksResult);
		Assert.assertTrue(actualPreHooks.contains(expectedPreHook),"The plugins listhooks report expected pre_auto_attach hook '"+expectedPreHook+"'.");
		pluginsListhooksResult = clienttasks.plugins(false, false, true, false);
		List<String> actualPostHooks = parseHooksForSlotFromPluginsListhooksResult("post_auto_attach", pluginsListhooksResult);
		Assert.assertTrue(actualPostHooks.contains(expectedPostHook),"The plugins listhooks report expected post_auto_attach hook '"+expectedPostHook+"'.");
	}
	protected String productCertDirForAutoAttachTestPluginHooks = "/tmp/sm-rhelProductCertDir";
	protected String productCertDirBeforeAttachTestPluginHooks = null;
	@BeforeGroups(groups={"setup"},value="verifyEnabledAutoAttachTestPluginHooksAreCalled_Test")
	public void configureRhelProductCertDirBeforeGroups() {
		// this before groups task is to avoid silent timeout during autosubscribe that prevent the post_auto_attach hooks from being called
		// This BeforeGroups is really a WORKAROUND FOR BUG 964332
		if (clienttasks==null) return;
		productCertDirBeforeAttachTestPluginHooks = clienttasks.productCertDir;
		client.runCommandAndWait("rm -rf "+productCertDirForAutoAttachTestPluginHooks+" && mkdir "+productCertDirForAutoAttachTestPluginHooks);
		for (File productCertFile : clienttasks.getCurrentProductCertFiles(null)) {
			if (!productCertFile.getPath().endsWith("_.pem")) {
				client.runCommandAndWait("cp "+productCertFile+" "+productCertDirForAutoAttachTestPluginHooks);				
			}
		}
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile,"productCertDir",productCertDirForAutoAttachTestPluginHooks);	
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20057", "RHEL7-51065"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="execute subscription-manager modules and verify the expected AutoAttachTestPlugin hooks are called",
			groups={"Tier1Tests","verifyEnabledAutoAttachTestPluginHooksAreCalled_Test"},
			priority=530, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testEnabledAutoAttachTestPluginHooksAreCalled() {
		truncateRhsmLog();
		
		// get the pre-registered facts on the system
		clienttasks.unregister(null,null,null, null);
		Map<String,String> facts = clienttasks.getFacts();
		
		// get the current rhsm logging level; INFO or DEBUG
		String rhsmLogLevel=null;
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.10-1")) {	// RHEL7.3 commit d84b15f42c2e4521e130b939039960c0846b849c 1334916: Move logging configuration to rhsm.conf
			rhsmLogLevel = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "logging", "default_log_level");
		} else if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.14.2-1")) {	// commit 66aafd77dc629b921379f0e121421c1c21c0b787 Move to fileConfig based logging.
			rhsmLogLevel = clienttasks.getConfFileParameter(clienttasks.rhsmLoggingConfFile, "handler_rhsm_log", "level");
		} else {
			rhsmLogLevel="DEBUG";	 // default
		}
		
		// register
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,false,null,null,(List<String>)null,null,null,null,true,null,null,null,null, null));

// A BETTER SOLUTION FOR THIS WORKAROUND IS configureRhelProductCertDirBeforeGroups() unconfigureRhelProductCertDirAfterGroups()
//		// TEMPORARY WORKAROUND FOR BUG
//		String bugId = "964332"; boolean invokeWorkaroundWhileBugIsOpen = true;
//		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
//		if (invokeWorkaroundWhileBugIsOpen) {
//			// issue a sacrificial autosubscribe call to get most of the entitlements attached.  If it times out, the post_auto_attach hooks will not get called
//			clienttasks.subscribe_(true, null, (String)null, null, null, null, null, null, null, null, null);
//		}
//		// END OF WORKAROUND
		
		// mark the rhsm.log file
		String logMarker = System.currentTimeMillis()+" Testing verifyEnabledAutoAttachTestPluginHooksAreCalled_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);
		
		// autosubscribe
		clienttasks.subscribe(true, null, (String)null, null, null, null, null, null, null, null, null, null, null);
		
		// when no products are installed, autosubsribe will be blocked and the auto_attach_hooks will not be called.  take this case into account...
		boolean noProductsInstalled = clienttasks.getCurrentProductCertFiles().isEmpty();
		
		// get the tail of the marked rhsm.log file
		String logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Running p").trim();
		
		//	2013-06-07 17:46:16,503 [DEBUG]  @plugins.py:728 - Running post_facts_collection_hook in facts_collection_test.FactsCollectionTestPlugin
		//	2013-06-07 17:46:16,504 [INFO]  @facts_collection_test.py:33 - Running post_facts_collection_hook: consumer facts count is 83
		//	2013-06-07 17:46:17,129 [DEBUG]  @plugins.py:728 - Running pre_auto_attach_hook in auto_attach_test.AutoAttachTestPlugin
		//	2013-06-07 17:46:17,130 [INFO]  @auto_attach_test.py:32 - Running pre_auto_attach_hook: system is about to auto-attach
		//	2013-06-07 17:46:17,130 [INFO]  @auto_attach_test.py:33 - Running pre_auto_attach_hook: auto-attaching consumer is 3f0fba70-e0c2-423d-b51a-ecdb2411587f
		//	2013-06-07 17:47:13,795 [DEBUG]  @plugins.py:728 - Running post_auto_attach_hook in auto_attach_test.AutoAttachTestPlugin
		//	2013-06-07 17:47:13,795 [INFO]  @auto_attach_test.py:43 - Running post_auto_attach_hook: system just auto-attached
		//	2013-06-07 17:47:13,795 [INFO]  @auto_attach_test.py:44 - Running post_auto_attach_hook: auto-attached consumer is 3f0fba70-e0c2-423d-b51a-ecdb2411587f
		//	2013-06-07 17:47:13,795 [INFO]  @auto_attach_test.py:48 - Running post_auto_attach_hook: auto-attached 15 entitlements
		
		// assert the pre/post_subscribe_hooks are called
		List<String> expectedLogInfo = new ArrayList<String>();
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running post_facts_collection_hook in facts_collection_test.FactsCollectionTestPlugin");	// enabled in prior FactsCollectionTestPlugin Tests 
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_facts_collection_hook: consumer facts count is "+facts.values().size());	// enabled in prior FactsCollectionTestPlugin Tests 
		if (!noProductsInstalled) {
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running pre_auto_attach_hook in auto_attach_test.AutoAttachTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running pre_auto_attach_hook: system is about to auto-attach");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running pre_auto_attach_hook: auto-attaching consumer is "+consumerId);
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running post_auto_attach_hook in auto_attach_test.AutoAttachTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_auto_attach_hook: system just auto-attached");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_auto_attach_hook: auto-attached consumer is "+consumerId);
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_auto_attach_hook: auto-attached \\d+ entitlements");
		}
		if (              logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*") == false) RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, null);	// used for debugging
		if (              logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*") == false) clienttasks.plugins_(null,null,null,true);	// used for debugging
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"), "The '"+clienttasks.rhsmLogFile+"' reports log messages: "+expectedLogInfo);
		if (noProductsInstalled) throw new SkipException("Could not verifyEnabledAutoAttachTestPluginHooksAreCalled because no products are installed making auto_attach a no-op."); 
	}
	@AfterGroups(groups={"setup"},value="verifyEnabledAutoAttachTestPluginHooksAreCalled_Test", alwaysRun=true)
	public void unconfigureRhelProductCertDirAfterGroups() {
		// This AfterGroups is really a WORKAROUND FOR BUG 964332
		if (clienttasks==null) return;
		if (productCertDirBeforeAttachTestPluginHooks==null) return;
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile,"productCertDir",productCertDirBeforeAttachTestPluginHooks);
		productCertDirBeforeAttachTestPluginHooks = null;
	}
	
	// ProductIdInstallTestPlugin Tests ***************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20058", "RHEL7-51066"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="enable ProductIdInstallTestPlugin and assert the plugins list reports enablement",
			groups={"Tier1Tests"},
			priority=610, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListWithEnabledProductIdInstallTestPlugin() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("product_id_install_test.ProductIdInstallTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The ProductIdInstallTestPlugin is installed.");
		
		// enable and verify
		enableTestPluginAndVerifyListReport(installedPlugin);
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20059", "RHEL7-51067"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify plugins listhooks reports all of the expected hooks for ProductIdInstallTestPlugin",
			groups={"Tier1Tests"},
			priority=620, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListhooksWithEnabledProductIdInstallTestPlugin() {
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("product_id_install_test.ProductIdInstallTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The ProductIdInstallTestPlugin is installed.");
		
		// hand populate the expected hooks (look for the python def methods inside the plugin.py file and prefix it with the plugin key.)
		String expectedPreHook = installedPlugin.key+".pre_product_id_install_hook";	//    def pre_product_id_install_hook(self, conduit):
		String expectedPostHook = installedPlugin.key+".post_product_id_install_hook";	//    def post_product_id_install_hook(self, conduit):

		// verify the plugins --listhooks reports that the pre and post slots contain the expected hooks
		SSHCommandResult pluginsListhooksResult = clienttasks.plugins(false, false, true, false);
		List<String> actualPreHooks = parseHooksForSlotFromPluginsListhooksResult("pre_product_id_install", pluginsListhooksResult);
		Assert.assertTrue(actualPreHooks.contains(expectedPreHook),"The plugins listhooks report expected pre_facts_collection hook '"+expectedPreHook+"'.");
		pluginsListhooksResult = clienttasks.plugins(false, false, true, false);
		List<String> actualPostHooks = parseHooksForSlotFromPluginsListhooksResult("post_product_id_install", pluginsListhooksResult);
		Assert.assertTrue(actualPostHooks.contains(expectedPostHook),"The plugins listhooks report expected post_facts_collection hook '"+expectedPostHook+"'.");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20060", "RHEL7-55177"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="execute subscription-manager modules and verify the expected ProductIdInstallTestPlugin hooks are called",
			groups={"Tier1Tests","blockedByBug-859197", "blockedByBug-922871"/*, "blockedByBug-922882"*/,"blockedByBug-1512948"},
			priority=630, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testEnabledProductIdInstallTestPluginHooksAreCalled() {
		if (clienttasks.getCurrentProductCertFiles().isEmpty()) throw new SkipException("This test will install a layered RHEL product which requires a base RHEL product cert to be installed.  Skipping this test because no RHEL product is installed.");
		truncateRhsmLog();
		
		// get the current rhsm logging level; INFO or DEBUG
		String rhsmLogLevel=null;
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.10-1")) {	// RHEL7.3 commit d84b15f42c2e4521e130b939039960c0846b849c 1334916: Move logging configuration to rhsm.conf
			rhsmLogLevel = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "logging", "default_log_level");
		} else if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.14.2-1")) {	// commit 66aafd77dc629b921379f0e121421c1c21c0b787 Move to fileConfig based logging.
			rhsmLogLevel = clienttasks.getConfFileParameter(clienttasks.rhsmLoggingConfFile, "handler_rhsm_log", "level");
		} else {
			rhsmLogLevel="DEBUG";	 // default
		}
		
		// register
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true,null,null,null,null, null));
		
		// mark the rhsm.log file
		String logMarker = System.currentTimeMillis()+" Testing verifyEnabledProductIdInstallTestPluginHooksAreCalled_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);

		// do a yum transaction and assert that the product_id_install hooks are NOT yet called
		clienttasks.getYumRepolist("all --enableplugin=product-id");
		//sleep(5000);	// give the plugin hooks a chance to be called; I think this is an async process

		// get the tail of the marked rhsm.log file
		String logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Running p").trim();
		
		// assert the pre/post_product_id_install_hooks are NOT called
		List<String> notExpectedLogInfo= Arrays.asList(
				"Running pre_product_id_install_hook",
				"Running post_product_id_install_hook",
				"");
		Assert.assertTrue(!logTail.replaceAll("\n","").matches(".*"+joinListToString(notExpectedLogInfo,".*")+".*"),
				"The '"+clienttasks.rhsmLogFile+"' DOES NOT report log messages (becasue no product id should have been installed): "+notExpectedLogInfo);
		
		
		// now login with the HighAvailability credentials and install a package and assert the pre/post_product_id_install_hooks are called
		if (sm_haUsername.equals("")) throw new SkipException("Skipping this test when no value was given for the High Availability username.");
		String haProductId = "83"; // Red Hat Enterprise Linux High Availability (for RHEL Server)
		if (clienttasks.arch.startsWith("s390")) haProductId = "300"; // Red Hat Enterprise Linux High Availability (for IBM z Systems)
		
		// register to an account that offers High Availability subscriptions
		consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_haUsername,sm_haPassword,sm_haOrg,null,null,null,null,null,null,null,(String)null,null,null, null, true, null, null, null, null, null));

		// make sure that there are no ha packages and no productId installed
		List<String> haPackages = HighAvailabilityTests.getHighAvailabilityPackages(clienttasks.redhatReleaseXY, clienttasks.arch);
		for (String haPackage : haPackages) {
			if (clienttasks.isPackageInstalled(haPackage)) clienttasks.yumRemovePackage(haPackage);
		}
		InstalledProduct haInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", haProductId, clienttasks.getCurrentlyInstalledProducts());
		if (haInstalledProduct!=null) {
			ProductCert haInstalledProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", haProductId, clienttasks.getCurrentProductCerts());
			log.warning("Manually removing installed High Availability product cert and restoring '"+clienttasks.productIdJsonFile+"' (you are probably running a RHEL5 client)...");
			client.runCommandAndWait("rm -f "+haInstalledProductCert.file.getPath());
			restoreProductIdJsonFileAfterClass();
			haInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", haProductId, clienttasks.getCurrentlyInstalledProducts());
		}
		Assert.assertNull(haInstalledProduct, "The High Availability product id '"+haProductId+"' should NOT be installed after successful removal of all High Availability packages.");
		
		// Subscribe to the High Availability subscription SKU
		List<SubscriptionPool> availableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		String haSku = HighAvailabilityTests.getHighAvailabilitySku(clienttasks.arch);
		SubscriptionPool haPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", haSku, availableSubscriptionPools);
		if (clienttasks.variant.equals("Server") && getArchesOfferringHighAvailabilityContent().contains(clienttasks.arch)) {
			Assert.assertNotNull(haPool, "A subscription pool for High Availability SKU '"+haSku+"' is available.");
			clienttasks.subscribe(null,null,haPool.poolId, null,null,null,null,null,null,null,null, null, null);
		} else {
			throw new SkipException("Not expecting High Availability subscription SKU '"+haSku+"' to offer content on a RHEL '"+clienttasks.redhatReleaseX+"' '"+clienttasks.variant+"' system with arch '"+clienttasks.arch+"'.");
		}
		// Note: Despite that subscription RH00025 will be available on these arches...
		// There will not be any HA content available unless product cert providing tag rhel-5-server or rhel-6-server or rhel-7-server is installed
		//  "name": "arch", 
		//  "productId": "RH00025", 
		//  "updated": "2015-06-17T10:55:58.000+0000", 
		//	"value": "x86_64,ppc64,ia64,ppc,x86"
		
		// INFO: rhel-ha-for-rhel-7-server-rpms/7Server/x86_64 is enabled by default
		// NOT ANYMORE, WE NOW NEED TO ENABLE THE ADDON REPO (A GOOD CHANGE BY REL-ENG DURING THE RHEL7.4 TEST PHASE, AND APPLIED TO ALL RELEASES)
		if (/*clienttasks.redhatReleaseX.equals("7") && */clienttasks.arch.equals("x86_64")) {
			clienttasks.repos(null, null, null, "rhel-ha-for-rhel-"+clienttasks.redhatReleaseX+"-server-rpms", null, null, null, null, null);
		}
		
		// INFO: rhel-ha-for-rhel-7-for-system-z-rpms/7Server/s390x is NOT enabled by default
		if (clienttasks.redhatReleaseX.equals("7") && clienttasks.arch.equals("s390x")) {
			clienttasks.repos(null, null, null, "rhel-ha-for-rhel-7-for-system-z-rpms", null, null, null, null, null);
		}
		
		// mark the rhsm.log file
		logMarker = System.currentTimeMillis()+" Testing verifyEnabledProductIdInstallTestPluginHooksAreCalled_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);

		// do a yum install of an ha package
		// WARNING! Package ccs also belongs to Resilient Storage which may cause productId 90 to also be installed from one of the beaker repos, therefore --disablerepo=beaker*
		clienttasks.yumInstallPackage(haPackages.get(0),"--disablerepo=beaker-*");	// yum -y install ccs --disablerepo=beaker-*
		//sleep(5000);	// give the plugin hooks a chance to be called; I think this is an async process
		

		// get the tail of the marked rhsm.log file
 		logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Running p").trim();
		
		// assert the pre/post_product_id_install_hooks are called
 		List<String> expectedLogInfo = new ArrayList<String>();
 		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running pre_product_id_install_hook in product_id_install_test.ProductIdInstallTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running pre_product_id_install_hook: yum product-id plugin is about to install a product cert");
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running post_product_id_install_hook in product_id_install_test.ProductIdInstallTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_product_id_install_hook: yum product-id plugin just installed a product cert");
		//if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_product_id_install_hook: 1 product_ids were just installed");	// probably correct, but not necessary to verify post_product_id_install_hook was called
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running post_product_id_install_hook: product_id "+haProductId+" was just installed");
		
		// Product Name:   Red Hat Enterprise Linux High Availability (for RHEL Server)
		// Product ID:     83
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"), "The '"+clienttasks.rhsmLogFile+"' reports log messages: "+expectedLogInfo);	
	}
	
	
	// TODO ProductIdRemoveTestPlugin Tests ***************************************************
	// CURRENTLY BLOCKED BY BUGZILLA 922882
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL7-55178"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="enable ProductIdRemoveTestPlugin and assert the plugins list reports enablement",
			groups={"Tier1Tests","blockedByBug-922882"},
			priority=710, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListWithEnabledProductIdRemoveTestPlugin() {
		
		String haProductId = "83"; // Red Hat Enterprise Linux High Availability (for RHEL Server)
		if (clienttasks.arch.startsWith("s390")) haProductId = "300"; // Red Hat Enterprise Linux High Availability (for IBM z Systems)
		
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "922882"; boolean invokeWorkaroundWhileBugIsOpen = true;
		// RFE Bug 922882 was CLOSED NOTABUG and will be re-opened when actually needed.  However, we still need to invoke this workaround to remove the ccs package from the prior test
		try {if (invokeWorkaroundWhileBugIsOpen/*&&BzChecker.getInstance().isBugOpen(bugId)*/) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			// remove the HA package that was installed by prior test verifyEnabledProductIdInstallTestPluginHooksAreCalled_Test
			List<String> haPackages = HighAvailabilityTests.getHighAvailabilityPackages(clienttasks.redhatReleaseXY, clienttasks.arch);
			if (!haPackages.isEmpty() && clienttasks.isPackageInstalled(haPackages.get(0))) {
				clienttasks.yumRemovePackage(haPackages.get(0));	// yum -y remove ccs
			}
			
			// remove the HA product cert too
			InstalledProduct haInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", haProductId, clienttasks.getCurrentlyInstalledProducts());
			if (haInstalledProduct!=null) {
				ProductCert haInstalledProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", haProductId, clienttasks.getCurrentProductCerts());
				log.warning("Manually removing installed High Availability product cert and restoring '"+clienttasks.productIdJsonFile+"' (you are probably running a RHEL5 client)...");
				client.runCommandAndWait("rm -f "+haInstalledProductCert.file.getPath());
				restoreProductIdJsonFileAfterClass();
				haInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", haProductId, clienttasks.getCurrentlyInstalledProducts());
			}
			Assert.assertNull(haInstalledProduct, "The High Availability product id '"+haProductId+"' should NOT be installed after successful removal of all High Availability packages.");
			
			throw new SkipException("Skipping test while bug '"+bugId+"' is not implemented.");
		}
		// END OF WORKAROUND
		
		
		Assert.fail("This test will be implemented after RFE bug 922882 is implemented.");
	}
	
	

	// AllSlotsTestPlugin Tests ********************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20061", "RHEL7-33087"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="enable AllSlotsTestPlugin and assert the plugins list reports enablement",
			groups={"Tier1Tests"},
			priority=810, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListWithEnabledAllSlotsTestPlugin() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("all_slots_test.AllSlotsTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The AllSlotsTestPlugin is installed.");
		
		// enable and verify
		enableTestPluginAndVerifyListReport(installedPlugin);
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20062", "RHEL7-51068"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify plugins listhooks reports all of the expected hooks for AllSlotsTestPlugin",
			groups={"Tier1Tests"},
			priority=820, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testPluginsListhooksWithEnabledAllSlotsTestPlugin() {
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("all_slots_test.AllSlotsTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The AllSlotsTestPlugin is installed.");
		
		// verify the plugins --listhooks reports that the pre and post slots contain the expected hooks
		SSHCommandResult pluginsListhooksResult = clienttasks.plugins(false, false, true, false);
		for (String slot : slots) {
			String expectedHook = installedPlugin.key+".handler";	//    def handler(self, conduit):
			List<String> actualHooks = parseHooksForSlotFromPluginsListhooksResult(slot, pluginsListhooksResult);
			Assert.assertTrue(actualHooks.contains(expectedHook),"The plugins listhooks report expected '"+slot+"' hook '"+expectedHook+"'.");
		}
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20063", "RHEL7-51069"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="execute subscription-manager modules and verify the expected AllSlotsTestPlugin hooks are called",
			groups={"Tier1Tests"},
			priority=830, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testEnabledAllSlotsTestPluginHooksAreCalled() throws JSONException, Exception {
		truncateRhsmLog();
		
		// get the pre-registered facts on the system
		clienttasks.unregister(null,null,null, null);
		Map<String,String> facts = clienttasks.getFacts();
		
		// get the current rhsm logging level; INFO or DEBUG
		String rhsmLogLevel=null;
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.10-1")) {	// RHEL7.3 commit d84b15f42c2e4521e130b939039960c0846b849c 1334916: Move logging configuration to rhsm.conf
			rhsmLogLevel = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "logging", "default_log_level");
		} else if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.14.2-1")) {	// commit 66aafd77dc629b921379f0e121421c1c21c0b787 Move to fileConfig based logging.
			rhsmLogLevel = clienttasks.getConfFileParameter(clienttasks.rhsmLoggingConfFile, "handler_rhsm_log", "level");
		} else {
			rhsmLogLevel="DEBUG";	 // default
		}
		
		// mark the rhsm.log file
		String logMarker = System.currentTimeMillis()+" Testing verifyEnabledAllSlotsTestPluginHooksAreCalled_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);

		// register and subscribe to random pool
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true,null,null,null,null, null));
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("Cannot randomly pick a pool for subscribing when there are no available pools for testing."); 
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		String quantity = null;
		/*if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.3-1"))*/ if (pool.suggested!=null) if (pool.suggested<1) quantity = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "instance_multiplier"); 	// when the Suggested quantity is 0, let's specify a quantity to avoid Stdout: Quantity '1' is not a multiple of instance multiplier '2'
		clienttasks.subscribe(null,null,pool.poolId,null,null,quantity,null,null,null,null,null, null, null);
		//sleep(5000);	// give the plugin hooks a chance to be called; I think this is an async process

		// get the tail of the marked rhsm.log file
		String logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Running h").trim();
		
		// TODO: also trigger pre/post product_id_install_hooks
		
		//	2013-03-16 11:53:07,272 [DEBUG]  @plugins.py:695 - Running handler in all_slots_test.AllSlotsTestPlugin
		//	2013-03-16 11:53:07,280 [INFO]  @all_slots_test.py:40 - Running handler for post_facts_collection_hook from slot post_facts_collection defined in all_slots_test.
		//	2013-03-16 11:53:07,284 [DEBUG]  @plugins.py:695 - Running handler in all_slots_test.AllSlotsTestPlugin
		//	2013-03-16 11:53:07,285 [INFO]  @all_slots_test.py:40 - Running handler for pre_register_consumer_hook from slot pre_register_consumer defined in all_slots_test.
		//	2013-03-16 11:53:09,759 [DEBUG]  @plugins.py:695 - Running handler in all_slots_test.AllSlotsTestPlugin
		//	2013-03-16 11:53:09,760 [INFO]  @all_slots_test.py:40 - Running handler for post_register_consumer_hook from slot post_register_consumer defined in all_slots_test.
		//	2013-03-16 11:53:16,183 [DEBUG]  @plugins.py:695 - Running handler in all_slots_test.AllSlotsTestPlugin
		//	2013-03-16 11:53:16,186 [INFO]  @all_slots_test.py:40 - Running handler for post_facts_collection_hook from slot post_facts_collection defined in all_slots_test.
		//	2013-03-16 11:53:22,387 [DEBUG]  @plugins.py:695 - Running handler in all_slots_test.AllSlotsTestPlugin
		//	2013-03-16 11:53:22,388 [INFO]  @all_slots_test.py:40 - Running handler for post_facts_collection_hook from slot post_facts_collection defined in all_slots_test.
		//	2013-03-16 11:53:22,756 [DEBUG]  @plugins.py:695 - Running handler in all_slots_test.AllSlotsTestPlugin
		//	2013-03-16 11:53:22,757 [INFO]  @all_slots_test.py:40 - Running handler for pre_subscribe_hook from slot pre_subscribe defined in all_slots_test.
		//	2013-03-16 11:53:23,438 [DEBUG]  @plugins.py:695 - Running handler in all_slots_test.AllSlotsTestPlugin
		//	2013-03-16 11:53:23,438 [INFO]  @all_slots_test.py:40 - Running handler for post_subscribe_hook from slot post_subscribe defined in all_slots_test.

		// assert the pre/post_subscribe_hooks are called
		List<String> expectedLogInfo = new ArrayList<String>();
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running handler in all_slots_test.AllSlotsTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running handler for post_facts_collection_hook from slot post_facts_collection defined in all_slots_test");
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running handler in all_slots_test.AllSlotsTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running handler for pre_register_consumer_hook from slot pre_register_consumer defined in all_slots_test");
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running handler in all_slots_test.AllSlotsTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running handler for post_register_consumer_hook from slot post_register_consumer defined in all_slots_test");
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running handler in all_slots_test.AllSlotsTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running handler for post_facts_collection_hook from slot post_facts_collection defined in all_slots_test");
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running handler in all_slots_test.AllSlotsTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running handler for post_facts_collection_hook from slot post_facts_collection defined in all_slots_test");
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running handler in all_slots_test.AllSlotsTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running handler for pre_subscribe_hook from slot pre_subscribe defined in all_slots_test");
		if (rhsmLogLevel.equals("DEBUG"))								expectedLogInfo.add("Running handler in all_slots_test.AllSlotsTestPlugin");
		if (rhsmLogLevel.equals("DEBUG")||rhsmLogLevel.equals("INFO"))	expectedLogInfo.add("Running handler for post_subscribe_hook from slot post_subscribe defined in all_slots_test");
		if (              logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*") == false) RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, null);	// used for debugging
		if (              logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*") == false) clienttasks.plugins_(null,null,null,true);	// used for debugging
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"), "The '"+clienttasks.rhsmLogFile+"' reports log messages: "+expectedLogInfo);
	}
	
	
	
	
	
	
	
	
	
	
	
	// Candidates for an automated Test:
	// see https://github.com/RedHatQE/rhsm-qe/issues
	
	// Configuration methods ***********************************************************************
	
	//@BeforeClass(groups={"setup"})	// not often enough due to recently added noisy logging:    2013-05-25 02:04:06,091 [DEBUG]  @injection.py:64 - Returning callable provider for feature ENT_DIR: <class 'subscription_manager.certdirectory.EntitlementDirectory'>
	protected void truncateRhsmLog() {
		if (client==null) return;
		
		// truncate the rhsm.log before this class to reduce its size because it occasionally gets backed up to rhsm.log.1
		// in the midst of a pair of calls to RemoteFileTasks.markFile(...) and RemoteFileTasks.getTailFromMarkedFile(...)
		client.runCommandAndWait("truncate --size=0 "+clienttasks.rhsmLogFile);
	}
	
	@BeforeClass(groups={"setup"})
	protected void rememberOriginalPluginDirConfigurationsBeforeClass() {
		if (clienttasks==null) return;
		// get the original plugin configuration directories
		if (originalPluginDir==null) originalPluginDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "pluginDir");
		if (originalPluginConfDir==null) originalPluginConfDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "pluginConfDir");
		Assert.assertNotNull(originalPluginDir, "Expecting rhsm.conf to contain a configuration for [rhsm]pluginDir");
		Assert.assertNotNull(originalPluginDir, "Expecting rhsm.conf to contain a configuration for [rhsm]pluginConfDir");
	}
	
	@BeforeClass(groups={"setup"},dependsOnMethods={"rememberOriginalPluginDirConfigurationsBeforeClass"})
	protected void configureTestPluginDirsBeforeClass() {
		if (clienttasks==null) return;
		
		// create test plugin directory configurations 
		RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+testPluginDir+" && mkdir "+testPluginDir, new Integer(0));
		RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+testPluginConfDir+" && mkdir "+testPluginConfDir, new Integer(0));
		
		// set the plugin configuration directories for this test class
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "pluginDir", testPluginDir);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "pluginConfDir", testPluginConfDir);
	}
	
	@BeforeClass(groups="setup")
	protected void backupProductIdJsonFileBeforeClass() {
		if (clienttasks==null) return;
		RemoteFileTasks.runCommandAndAssert(client,"cat "+clienttasks.productIdJsonFile+" > "+backupProductIdJsonFile, Integer.valueOf(0));
	}
	//AFTERCLASS ANNOTATION SHOULD NOT BE NEEDED SINCE THIS METHOD IS CALLED ON DEMAND	@AfterClass(groups="setup")
	protected void restoreProductIdJsonFileAfterClass() {
		if (clienttasks==null) return;
		if (!RemoteFileTasks.testExists(client, backupProductIdJsonFile)) return;
		RemoteFileTasks.runCommandAndAssert(client,"cat "+backupProductIdJsonFile+" > "+clienttasks.productIdJsonFile, Integer.valueOf(0));
		clienttasks.yumClean("all");
	}
	
	@BeforeClass(groups="setup")
	protected void initializeSlotsBeforeClass() {
		if (clienttasks==null) return;
		slots.add("pre_register_consumer");		slots.add("post_register_consumer");
		slots.add("pre_product_id_install");	slots.add("post_product_id_install");
		if (clienttasks.isPackageVersion("subscription-manager",">","1.10.14-2")) {
			slots.add("pre_product_id_update");		slots.add("post_product_id_update");	// added by bug https://bugzilla.redhat.com/show_bug.cgi?id=1035115#c13; subscription-manager commit 9ee2f98ece8e1f86ed6fa22ee847c8df9814f792;   RHEL7.0 branch commit 985dab22befbc70cf068465e64f3ae4c7a41144e
		}
		slots.add("pre_subscribe");				slots.add("post_subscribe");
		slots.add("pre_auto_attach");			slots.add("post_auto_attach");
		/*slots.add("pre_facts_collection");*/	slots.add("post_facts_collection");
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.12.3-1")) {
			slots.add("update_content");	// added for ostree support; subscription-manager commit b714a21e213aa9b184fdca2c48f8d901cd8b8396
		}
	}
	
	@BeforeGroups(groups={"setup"}, value="DisabledPluginTests")
	protected void fetchAndDisableAllPluginsBeforeGroups() {
		if (clienttasks==null) return;
		SSHCommandResult result;
		
		// fetch the test-plugins files
		if (sm_testpluginsUrl.isEmpty()) return; 
		log.info("Fetching test plugins from "+sm_testpluginsUrl+" for use by this test class...");
		RemoteFileTasks.runCommandAndAssert(client, "cd "+testPluginDir+" && wget --quiet --recursive --no-host-directories --cut-dirs=2 --no-parent --accept .py "+sm_testpluginsUrl, Integer.valueOf(0)/*,null,"Downloaded: \\d+ files"*/);
		// Note: If above fails with exitCode 8, then the auto-services server needs changes to /etc/httpd/conf/httpd.conf  remove  AddHandler .py from mime_module and restart the httpd service
		RemoteFileTasks.runCommandAndAssert(client, "cd "+testPluginConfDir+" && wget --quiet --recursive --no-host-directories --cut-dirs=2 --no-parent --accept .conf "+sm_testpluginsUrl, Integer.valueOf(0)/*,null,"Downloaded: \\d+ files"*/);
		
		// create plugin objects for simplicity
		result = RemoteFileTasks.runCommandAndAssert(client, "find "+testPluginConfDir+" -name *.conf", 0);
		for (String pluginConfPathname : result.getStdout().trim().split("\\s*\\n\\s*")) {
			if (pluginConfPathname.isEmpty()) continue;
			File examplePluginConfFile = new File(testPluginConfDir+"/"+new File(pluginConfPathname).getName());
			File examplePluginFile = new File(testPluginDir+"/"+examplePluginConfFile.getName().split("\\.")[0]+".py");
			installedPlugins.add(new Plugin(examplePluginConfFile,examplePluginFile));
		}
				
		// disable all of the plugin configurations
		for (Plugin installedPlugin : installedPlugins) {
			clienttasks.updateConfFileParameter(installedPlugin.configFile.getPath(), "enabled", "0");
		}

	}
	
	@AfterClass(groups={"setup"})
	protected void restoreOriginalPluginDirConfigurationsAfterClass() {
		if (clienttasks==null) return;
		// set the original plugin directory configurations
		if (originalPluginDir!=null) clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "pluginDir", originalPluginDir);
		if (originalPluginConfDir!=null) clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "pluginConfDir", originalPluginConfDir);
	}
	
	// Protected methods ***********************************************************************

	protected final String backupProductIdJsonFile	= "/tmp/sm-productIdJsonFile";
	protected String originalPluginDir = null;	// read from /etc/rhm/rhsm.conf	=>	/usr/share/rhsm-plugins
	protected String originalPluginConfDir = null;	// read from /etc/rhm/rhsm.conf	=>	/etc/rhsm/pluginconf.d
	protected final String testPluginDir = "/tmp/sm-test-plugins";
	protected final String testPluginConfDir = "/tmp/sm-test-pluginconf.d";
	protected List<Plugin> installedPlugins = new ArrayList<Plugin>();
	protected List<String> slots = new ArrayList<String>();
	
	
	// data object representing an installed plugin
    private class Plugin {
    	public File configFile;	// /etc/rhsm/pluginconf.d/subscribe.SubscribePlugin.conf
    	public File sourceFile;	// /usr/share/rhsm-plugins/subscribe.py
    	public String key;		// subscribe.SubscribePlugin
    	Plugin(File configFile, File sourceFile) {
    		this.configFile = configFile;
    		this.sourceFile = sourceFile;
    		this.key = configFile.getName().replaceFirst("\\.conf$", "");
    	}

    	public String toString() {
    		String string = "";
    		if (key != null)			string += String.format(" %s='%s'", "key",key);
    		if (sourceFile != null)		string += String.format(" %s='%s'", "sourceFile",sourceFile);
    		if (configFile != null)		string += String.format(" %s='%s'", "configFile",configFile);    		
    		return string.trim();
    	}
    }
    
    protected Plugin getPlugin(String key) {
		for (Plugin installedPlugin : installedPlugins) {
			if (installedPlugin.key.equals(key)) return installedPlugin;
		}
		return null;
    }
    
    String getExpectedPluginListReport(Plugin installedPlugin, String enablement, Boolean verbose) {
    	String expectedPluginReport  = null;
		if (verbose) {
	    	//	[root@jsefler-7 ~]# subscription-manager plugins --verbose
			//	register_consumer.RegisterConsumerPlugin: disabled
			//	plugin_key: register_consumer.RegisterConsumerPlugin
			//	config file: /etc/rhsm/pluginconf.d/register_consumer.RegisterConsumerPlugin.conf
			//	[main]
			//	enabled=0
			expectedPluginReport  = String.format("%s: %s\n", installedPlugin.key, enablement);
			expectedPluginReport += String.format("%s: %s\n", "plugin_key", installedPlugin.key);
			expectedPluginReport += String.format("%s: %s\n", "config file", installedPlugin.configFile.getPath());
			expectedPluginReport += String.format("%s\n", client.runCommandAndWaitWithoutLogging("cat "+installedPlugin.configFile.getPath()).getStdout().trim());
		} else {
	    	//	[root@jsefler-7 ~]# subscription-manager plugins --list
			//	register_consumer.RegisterConsumerPlugin: disabled
			expectedPluginReport = String.format("%s: %s", installedPlugin.key, enablement);
		}
		return expectedPluginReport;
    }
    
    protected List<String> parseHooksForSlotFromPluginsListhooksResult(String slot, SSHCommandResult listhooksResult) {
    	List<String> hooks = new ArrayList<String>();
    	
		//	[root@jsefler-7 pluginconf.d]# subscription-manager plugins --listhooks --verbose
		//	pre_register_consumer
		//		register_consumer.RegisterConsumerPlugin.pre_register_consumer_hook
		//		all_slots.AllSlotsPlugin.handler
		//	post_register_consumer
		//		register_consumer.RegisterConsumerPlugin.post_register_consumer_hook
		//		all_slots.AllSlotsPlugin.handler
		//	post_product_id_install
		//		all_slots.AllSlotsPlugin.handler
		//	pre_product_id_install
		//		all_slots.AllSlotsPlugin.handler
		//	post_facts_collection
		//		all_slots.AllSlotsPlugin.handler
		//	pre_subscribe
		//		subscribe.SubscribePlugin.pre_subscribe_hook
		//		all_slots.AllSlotsPlugin.handler
		//	post_subscribe
		//		subscribe.SubscribePlugin.post_subscribe_hook
		//		all_slots.AllSlotsPlugin.handler
    	boolean hookLine=false;
    	for (String line : listhooksResult.getStdout().trim().split("\\n")) {
    		for (String s : slots) if (line.equals(s)) hookLine=false;
    		if (hookLine) hooks.add(line.trim());
			if (line.equals(slot)) hookLine=true;
		}
    	return hooks;
    }
    
	protected void enableTestPluginAndVerifyListReport(Plugin plugin) {
				
		// enable the Plugin
		clienttasks.updateConfFileParameter(plugin.configFile.getPath(), "enabled", "1");
		
		// verify the plugins list reports Plugin is enabled
		String expectedPluginReport = getExpectedPluginListReport(plugin, "enabled", false);
		Assert.assertTrue(clienttasks.plugins(null,null,null,null).getStdout().contains(expectedPluginReport),
				"Stdout from the plugins list command reports expected plugin report '"+expectedPluginReport+"'.");
		
		// verify the verbose plugins list reports Plugin is enabled
		expectedPluginReport  = getExpectedPluginListReport(plugin, "enabled", true);
		Assert.assertTrue(clienttasks.plugins(null,null,null,true).getStdout().contains(expectedPluginReport),
				"Stdout from the verbose plugins list command reports expected plugin report: \n"+expectedPluginReport);
	}
	
	List<String> getArchesOfferringHighAvailabilityContent() {
		// see HighAvailability.assertRhelServerBeforeClass()
		if (clienttasks.redhatReleaseX.equals("5")) {
			return Arrays.asList("x86_64","x86","i386","i686","ia64","ppc","ppc64");
		}
		if (clienttasks.redhatReleaseX.equals("6")) {
			return Arrays.asList("x86_64","x86","i386","i686"/*,"ia64","ppc","ppc64" IS AVAILABLE FOR CONSUMPTION, HOWEVER SUBSCRIPTION-MANAGER REPOS --LIST WILL BE EMPTY*/);
		}
		if (clienttasks.redhatReleaseX.equals("7")) {
			return Arrays.asList("x86_64"/*,"x86","i386","i686","ia64","ppc","ppc64" IS AVAILABLE FOR CONSUMPTION, HOWEVER SUBSCRIPTION-MANAGER REPOS --LIST WILL BE EMPTY*/);
		}
		return Arrays.asList();
	}
    
	// Data Providers ***********************************************************************
	

}
