package rhsm.cli.tests;

import java.io.File;
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
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * The strategy for these tests is to exercise the test-plugins which are based on the
 * exmaple-plugins found in the upstream subscription-manager git repository:
 * https://github.com/candlepin/subscription-manager/tree/master/example-plugins
 * 
 * Design Document:
 * https://engineering.redhat.com/trac/Entitlement/wiki/SubscriptionManagerPlugins
 */
@Test(groups={"PluginTests","AcceptanceTests"})
public class PluginTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************
	
	
	// UninstalledPlugin Tests ************************************************************

	@Test(	description="execute subscription-manager plugins --listslots",
			groups={},
			priority=10, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListslots_Test() {
		List<String> actualSlots = Arrays.asList(clienttasks.plugins(null,true,null,null).getStdout().trim().split("\n"));
		Assert.assertTrue(actualSlots.containsAll(slots), "All of these expected slots are listed: "+slots);
		Assert.assertTrue(slots.containsAll(actualSlots), "Only these expected slots are listed: "+slots);
		actualSlots = Arrays.asList(clienttasks.plugins(null,true,null,true).getStdout().trim().split("\n"));
		Assert.assertTrue(actualSlots.containsAll(slots)&&slots.containsAll(actualSlots), "Including --verbose option should produce the same list of slots.");
	}

	@Test(	description="execute subscription-manager plugins --list (with no plugins installed)",
			groups={},
			priority=20, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListWithNoPluginsInstalled_Test() {
		Assert.assertEquals(clienttasks.plugins(null,null,null,null).getStdout().trim(), "", "Stdout from the plugins command indicates no plugins are installed.");
		Assert.assertEquals(clienttasks.plugins(true,null,null,null).getStdout().trim(), "", "Stdout from the plugins --list command indicates no plugins are installed.");
		Assert.assertEquals(clienttasks.plugins(null,null,null,true).getStdout().trim(), "", "Stdout from the plugins command indicates no plugins are installed (--verbose has no affect).");
	}
	
	@Test(	description="execute subscription-manager plugins --listhooks (with no plugins installed)",
			groups={},
			priority=30, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListhooksWithNoPluginsInstalled_Test() {
		List<String> plugins_listslots			= Arrays.asList(clienttasks.plugins(null,true,null,null).getStdout().trim().split("\n"));
		List<String> plugins_listhooks			= Arrays.asList(clienttasks.plugins(null,null,true,null).getStdout().trim().split("\n"));
		List<String> plugins_listhooks_verbose	= Arrays.asList(clienttasks.plugins(null,null,true,true).getStdout().trim().split("\n"));
		
		Assert.assertTrue(plugins_listslots.containsAll(plugins_listhooks) && plugins_listhooks.containsAll(plugins_listslots),
				"When there are no plugins installed, the expected --listhooks report should be identical to the --listslots.");
		Assert.assertTrue(plugins_listslots.containsAll(plugins_listhooks_verbose) && plugins_listhooks_verbose.containsAll(plugins_listslots),
				"When there are no plugins installed, the expected --listhooks --verbose report should be identical to the --listslots.");
	}
	
	
	
	// DisabledPlugin Tests ***************************************************************
	
	@Test(	description="execute subscription-manager plugins --list (with plugins installed and disabled)",
			groups={"DisabledPluginTests"},
			priority=110, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListWithDisabledPluginsInstalled_Test() {
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
	
	@Test(	description="execute subscription-manager plugins --listhooks (with plugins installed and disabled)",
			groups={"DisabledPluginTests"},
			priority=120, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListhooksWithDisabledPluginsInstalled_Test() {
		List<String> plugins_listslots			= Arrays.asList(clienttasks.plugins(null,true,null,null).getStdout().trim().split("\n"));
		List<String> plugins_listhooks			= Arrays.asList(clienttasks.plugins(null,null,true,null).getStdout().trim().split("\n"));
		List<String> plugins_listhooks_verbose	= Arrays.asList(clienttasks.plugins(null,null,true,true).getStdout().trim().split("\n"));
		
		Assert.assertTrue(plugins_listslots.containsAll(plugins_listhooks) && plugins_listhooks.containsAll(plugins_listslots),
				"When all plugins installed are disabled, the expected --listhooks report should be identical to the --listslots.");
		Assert.assertTrue(plugins_listslots.containsAll(plugins_listhooks_verbose) && plugins_listhooks_verbose.containsAll(plugins_listslots),
				"When all plugins installed are disabled, the expected --listhooks --verbose report should be identical to the --listslots.");
	}
	
	
	
	// RegisterConsumerTestPlugin Tests ***************************************************
	
	@Test(	description="enable the RegisterConsumerTestPlugin and assert the plugins list reports enablement",
			groups={},
			priority=210, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListWithEnabledRegisterConsumerTestPlugin_Test1() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("register_consumer_test1.RegisterConsumerTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The RegisterConsumerTestPlugin 1 is installed.");
		
		// enable and verify
		enableTestPluginAndVerifyListReport(installedPlugin);
	}
	@Test(	description="verify plugins listhooks reports all of the expected hooks for RegisterConsumerTestPlugin",
			groups={},
			priority=220, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListhooksWithEnabledRegisterConsumerTestPlugin_Test1() {
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
	@Test(	description="execute subscription-manager modules and verify the expected RegisterConsumerTestPlugin hooks are called",
			groups={},
			priority=230, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyEnabledRegisterConsumerTestPluginHooksAreCalled_Test1() {
		removeRhsmLog();
		
		// get the pre-registered facts on the system
		clienttasks.unregister(null,null,null);
		Map<String,String> facts = clienttasks.getFacts();
		
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
		List<String> expectedLogInfo= Arrays.asList(
				"Running pre_register_consumer_hook in register_consumer_test1.RegisterConsumerTestPlugin",
				"Running pre_register_consumer_hook 1: system name "+clienttasks.hostname+" is about to be registered.",
				"Running pre_register_consumer_hook 1: consumer facts count is "+facts.values().size(),
				"Running post_register_consumer_hook in register_consumer_test1.RegisterConsumerTestPlugin",
				"Running post_register_consumer_hook 1: consumer uuid "+consumerId+" is now registered.",
				"");
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"),
				"The '"+clienttasks.rhsmLogFile+"' reports expected log messages: "+expectedLogInfo);
	}
	
	@Test(	description="enable another RegisterConsumerTestPlugin and assert the plugins list reports enablement",
			groups={},
			priority=240, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListWithEnabledRegisterConsumerTestPlugin_Test2() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("register_consumer_test2.RegisterConsumerTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The RegisterConsumerTestPlugin 2 is installed.");
		
		// enable and verify
		enableTestPluginAndVerifyListReport(installedPlugin);
	}
	@Test(	description="verify plugins listhooks reports all of the expected hooks for another RegisterConsumerTestPlugin",
			groups={},
			priority=250, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListhooksWithEnabledRegisterConsumerTestPlugin_Test2() {
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
	@Test(	description="execute subscription-manager modules and verify the expected RegisterConsumerTestPlugin hooks are called",
			groups={},
			priority=260, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyEnabledRegisterConsumerTestPluginHooksAreCalled_Test2() {
		removeRhsmLog();
		
		// get the pre-registered facts on the system
		clienttasks.unregister(null,null,null);
		Map<String,String> facts = clienttasks.getFacts();
		
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
		List<String> expectedLogInfo= Arrays.asList(
				"Running pre_register_consumer_hook in register_consumer_test1.RegisterConsumerTestPlugin",
				"Running pre_register_consumer_hook 1: system name "+clienttasks.hostname+" is about to be registered.",
				"Running pre_register_consumer_hook 1: consumer facts count is "+facts.values().size(),
				"Running pre_register_consumer_hook in register_consumer_test2.RegisterConsumerTestPlugin",
				"Running pre_register_consumer_hook 2: system name "+clienttasks.hostname+" is about to be registered.",
				"Running pre_register_consumer_hook 2: consumer facts count is "+facts.values().size(),
				"Running post_register_consumer_hook in register_consumer_test1.RegisterConsumerTestPlugin",
				"Running post_register_consumer_hook 1: consumer uuid "+consumerId+" is now registered.",
				"Running post_register_consumer_hook in register_consumer_test2.RegisterConsumerTestPlugin",
				"Running post_register_consumer_hook 2: consumer uuid "+consumerId+" is now registered.",
				"");
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"),
				"The '"+clienttasks.rhsmLogFile+"' reports expected log messages: "+expectedLogInfo);
//		
//		logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Running post_register_consumer_hook").trim();
//		expectedLogInfo= Arrays.asList(
//				"Running post_register_consumer_hook in register_consumer1.RegisterConsumerTestPlugin",
//				"Running post_register_consumer_hook 1: consumer uuid "+consumerId+" is now registered.",
//				"Running post_register_consumer_hook in register_consumer2.RegisterConsumerTestPlugin",
//				"Running post_register_consumer_hook 2: consumer uuid "+consumerId+" is now registered.");
//		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"),
//				"The '"+clienttasks.rhsmLogFile+"' reports expected log messages: "+expectedLogInfo);
	}
	
	
	// FactsCollectionTestPlugin Tests ****************************************************

	@Test(	description="enable FactsCollectionTestPlugin and assert the plugins list reports enablement",
			groups={},
			priority=310, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListWithEnabledFactsCollectionTestPlugin_Test() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("facts_collection_test.FactsCollectionTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The FactsCollectionTestPlugin is installed.");
		
		// enable and verify
		enableTestPluginAndVerifyListReport(installedPlugin);
	}
	@Test(	description="verify plugins listhooks reports all of the expected hooks for FactsCollectionTestPlugin",
			groups={},
			priority=320, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListhooksWithEnabledFactsCollectionTestPlugin_Test() {
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
	@Test(	description="execute subscription-manager modules and verify the expected FactsCollectionTestPlugin hooks are called",
			groups={},
			priority=330, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyEnabledFactsCollectionTestPluginHooksAreCalled_Test() {
		removeRhsmLog();
		
		// get the pre-registered facts on the system
		clienttasks.unregister(null,null,null);
		Map<String,String> facts = clienttasks.getFacts();
		
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
		List<String> expectedLogInfo= Arrays.asList(
				"Running post_facts_collection_hook in facts_collection_test.FactsCollectionTestPlugin",
				"Running post_facts_collection_hook: consumer facts count is "+facts.values().size(),
				"");
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"),
				"The '"+clienttasks.rhsmLogFile+"' reports expected log messages: "+expectedLogInfo);
		
		// add 1 extra post_facts_collection_hook_fact and run update
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("post_facts_collection_hook_fact", "callback test");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		logMarker = System.currentTimeMillis()+" Testing verifyEnabledFactsCollectionTestPluginHooksAreCalled_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);
		clienttasks.facts(null,true,null,null,null);
		sleep(5000);	// give the plugin hooks a chance to be called; I think this is an async process
		
		// assert the expected log calls
		logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Running post_facts_collection_hook").trim();
		expectedLogInfo= Arrays.asList(
				"Running post_facts_collection_hook in facts_collection_test.FactsCollectionTestPlugin",
				"Running post_facts_collection_hook: consumer facts count is "+(facts.values().size()+1),
				"");
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"),
				"The '"+clienttasks.rhsmLogFile+"' reports expected log messages: "+expectedLogInfo);
	}
	
	
	// SubscribeTestPlugin Tests ********************************************************

	@Test(	description="enable SubscribeTestPlugin and assert the plugins list reports enablement",
			groups={},
			priority=410, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListWithEnabledSubscribeTestPlugin_Test() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("subscribe_test.SubscribeTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The SubscribePlugintestPlugin is installed.");
		
		// enable and verify
		enableTestPluginAndVerifyListReport(installedPlugin);
	}
	@Test(	description="verify plugins listhooks reports all of the expected hooks for SubscribeTestPlugin",
			groups={},
			priority=420, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListhooksWithEnabledSubscribeTestPlugin_Test() {
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
	@Test(	description="execute subscription-manager modules and verify the expected SubscribeTestPlugin hooks are called",
			groups={},
			priority=430, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyEnabledSubscribeTestPluginHooksAreCalled_Test() {
		removeRhsmLog();
		
		// get the pre-registered facts on the system
		clienttasks.unregister(null,null,null);
		Map<String,String> facts = clienttasks.getFacts();
		
		// register and get the current available subscription list
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true,null,null,null,null));
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("Cannot randomly pick a pool for subscribing when there are no available pools for testing."); 
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool

		// mark the rhsm.log file
		String logMarker = System.currentTimeMillis()+" Testing verifyEnabledSubscribeTestPluginHooksAreCalled_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);

		// subscribe to a random pool (to generate calls to pre/post hooks)
		clienttasks.subscribe(null,null,pool.poolId,null,null,null,null,null,null,null,null);
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
		List<String> expectedLogInfo= Arrays.asList(
				"Running post_facts_collection_hook in facts_collection_test.FactsCollectionTestPlugin",	// enabled in prior FactsCollectionTestPlugin Tests 
				"Running post_facts_collection_hook: consumer facts count is "+facts.values().size(),	// enabled in prior FactsCollectionTestPlugin Tests 
				"Running pre_subscribe_hook in subscribe_test.SubscribeTestPlugin",
				"Running pre_subscribe_hook: system is about to subscribe",
				"Running pre_subscribe_hook: subscribing consumer is "+consumerId,
				"Running post_subscribe_hook in subscribe_test.SubscribeTestPlugin",
				"Running post_subscribe_hook: system just subscribed",
				"Running post_subscribe_hook: subscribed consumer is "+consumerId,
				"Running post_subscribe_hook: subscribed from pool id "+pool.poolId,
				"");
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"),
				"The '"+clienttasks.rhsmLogFile+"' reports log messages: "+expectedLogInfo);
	}
	
	
	// AutoAttachTestPlugin Tests ********************************************************

	@Test(	description="enable AutoAttachTestPlugin and assert the plugins list reports enablement",
			groups={},
			priority=510, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListWithEnabledAutoAttachTestPlugin_Test() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("auto_attach_test.AutoAttachTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The AutoAttachTestPlugin is installed.");
		
		// enable and verify
		enableTestPluginAndVerifyListReport(installedPlugin);
	}
	@Test(	description="verify plugins listhooks reports all of the expected hooks for AutoAttachTestPlugin",
			groups={},
			priority=520, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListhooksWithEnabledAutoAttachTestPlugin_Test() {
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
	@Test(	description="execute subscription-manager modules and verify the expected AutoAttachTestPlugin hooks are called",
			groups={"verifyEnabledAutoAttachTestPluginHooksAreCalled_Test"},
			priority=530, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyEnabledAutoAttachTestPluginHooksAreCalled_Test() {
		removeRhsmLog();
		
		// get the pre-registered facts on the system
		clienttasks.unregister(null,null,null);
		Map<String,String> facts = clienttasks.getFacts();

		// register
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,false,null,null,(List<String>)null,null,null,null,true,null,null,null,null));

// A BETTER SOLUTION FOR THIS WORKAROUND IS configureRhelProductCertDirBeforeGroups() unconfigureRhelProductCertDirAfterGroups()
//		// TEMPORARY WORKAROUND FOR BUG
//		String bugId = "964332"; boolean invokeWorkaroundWhileBugIsOpen = true;
//		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
//		if (invokeWorkaroundWhileBugIsOpen) {
//			// issue a sacrificial autosubscribe call to get most of the entitlements attached.  If it times out, the post_auto_attach hooks will not get called
//			clienttasks.subscribe_(true, null, (String)null, null, null, null, null, null, null, null, null);
//		}
//		// END OF WORKAROUND
		
		// mark the rhsm.log file
		String logMarker = System.currentTimeMillis()+" Testing verifyEnabledAutoAttachTestPluginHooksAreCalled_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);
		
		// autosubscribe
		clienttasks.subscribe(true, null, (String)null, null, null, null, null, null, null, null, null);
		
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
		List<String> expectedLogInfo= Arrays.asList(
				"Running post_facts_collection_hook in facts_collection_test.FactsCollectionTestPlugin",	// enabled in prior FactsCollectionTestPlugin Tests 
				"Running post_facts_collection_hook: consumer facts count is "+facts.values().size(),	// enabled in prior FactsCollectionTestPlugin Tests 
				"Running pre_auto_attach_hook in auto_attach_test.AutoAttachTestPlugin",
				"Running pre_auto_attach_hook: system is about to auto-attach",
				"Running pre_auto_attach_hook: auto-attaching consumer is "+consumerId,
				"Running post_auto_attach_hook in auto_attach_test.AutoAttachTestPlugin",
				"Running post_auto_attach_hook: system just auto-attached",
				"Running post_auto_attach_hook: auto-attached consumer is "+consumerId,
				"Running post_auto_attach_hook: auto-attached \\d+ entitlements",
				"");
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"),
				"The '"+clienttasks.rhsmLogFile+"' reports log messages: "+expectedLogInfo);
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

	@Test(	description="enable ProductIdInstallTestPlugin and assert the plugins list reports enablement",
			groups={},
			priority=610, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListWithEnabledProductIdInstallTestPlugin_Test() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("product_id_install_test.ProductIdInstallTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The ProductIdInstallTestPlugin is installed.");
		
		// enable and verify
		enableTestPluginAndVerifyListReport(installedPlugin);
	}
	@Test(	description="verify plugins listhooks reports all of the expected hooks for ProductIdInstallTestPlugin",
			groups={},
			priority=620, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListhooksWithEnabledProductIdInstallTestPlugin_Test() {
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
	@Test(	description="execute subscription-manager modules and verify the expected ProductIdInstallTestPlugin hooks are called",
			groups={"blockedByBug-859197", "blockedByBug-922871"/*, "blockedByBug-922882"*/},
			priority=630, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyEnabledProductIdInstallTestPluginHooksAreCalled_Test() {
		removeRhsmLog();
		
		// register
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true,null,null,null,null));
		
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

		// register to an account that offers High Availability subscriptions
		consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_haUsername,sm_haPassword,sm_haOrg,null,null,null,null,null,null,null,(String)null,null,null, null, true, null, null, null, null));

		// make sure that there are no ha packages and no productId installed
		for (String haPackage : sm_haPackages) {
			if (clienttasks.isPackageInstalled(haPackage)) clienttasks.yumRemovePackage(haPackage);
		}
		InstalledProduct haInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", HighAvailabilityTests.haProductId, clienttasks.getCurrentlyInstalledProducts());
		if (haInstalledProduct!=null) {
			ProductCert haInstalledProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", HighAvailabilityTests.haProductId, clienttasks.getCurrentProductCerts());
			log.warning("Manually removing installed High Availability product cert and restoring '"+clienttasks.productIdJsonFile+"' (you are probably running a RHEL5 client)...");
			client.runCommandAndWait("rm -f "+haInstalledProductCert.file.getPath());
			restoreProductIdJsonFileAfterClass();
			haInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", HighAvailabilityTests.haProductId, clienttasks.getCurrentlyInstalledProducts());
		}
		Assert.assertNull(haInstalledProduct, "The High Availability product id '"+HighAvailabilityTests.haProductId+"' should NOT be installed after successful removal of all High Availability packages.");
		
		// subscribe to the High Availability subscription and install an HA package
		List<SubscriptionPool> availableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool haPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", sm_haSku, availableSubscriptionPools);
		if (!clienttasks.variant.equals("Server")) {
			throw new SkipException("High Availability is only available for Server.");
		} else if (!getSupportArchesForHighAvailability().contains(clienttasks.arch)) {
			Assert.assertNull(haPool, "High Availability subscription SKU '"+sm_haSku+"' should NOT be available for consumption on a '"+clienttasks.variant+"' system whose arch '"+clienttasks.arch+"' is NOT among the supported arches "+getSupportArchesForHighAvailability());
			throw new SkipException("Cannot consume High Availability subscription SKU '"+sm_haSku+"' on a '"+clienttasks.variant+"' system whose arch '"+clienttasks.arch+"' is NOT among the supported arches "+getSupportArchesForHighAvailability());
		}
		
		// Subscribe to the High Availability subscription SKU
		clienttasks.subscribe(null,null,haPool.poolId, null,null,null,null,null,null,null,null);

		// mark the rhsm.log file
		logMarker = System.currentTimeMillis()+" Testing verifyEnabledProductIdInstallTestPluginHooksAreCalled_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);

		// do a yum install of an ha package
		clienttasks.yumInstallPackage(sm_haPackages.get(0));	// yum -y install ccs
		//sleep(5000);	// give the plugin hooks a chance to be called; I think this is an async process

		// get the tail of the marked rhsm.log file
 		logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Running p").trim();
		
		// assert the pre/post_product_id_install_hooks are called
		List<String> expectedLogInfo= Arrays.asList(
				"Running pre_product_id_install_hook in product_id_install_test.ProductIdInstallTestPlugin",
				"Running pre_product_id_install_hook: yum product-id plugin is about to install a product cert",
				"Running post_product_id_install_hook in product_id_install_test.ProductIdInstallTestPlugin",
				"Running post_product_id_install_hook: yum product-id plugin just installed a product cert",
				//"Running post_product_id_install_hook: 1 product_ids were just installed",	// probably correct, but not necessary to verify post_product_id_install_hook was called
				"Running post_product_id_install_hook: product_id "+HighAvailabilityTests.haProductId+" was just installed",
				"");
		// Product Name:   Red Hat Enterprise Linux High Availability (for RHEL Server)
		// Product ID:     83
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"),
				"The '"+clienttasks.rhsmLogFile+"' reports log messages: "+expectedLogInfo);	
	}
	
	
	// TODO ProductIdRemoveTestPlugin Tests ***************************************************
	// CURRENTLY BLOCKED BY BUGZILLA 922882
	@Test(	description="enable ProductIdRemoveTestPlugin and assert the plugins list reports enablement",
			groups={/*"blockedByBug-922882"*/},
			priority=710, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListWithEnabledProductIdRemoveTestPlugin_Test() {
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "922882"; boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			// remove the HA package that was installed by prior test verifyEnabledProductIdInstallTestPluginHooksAreCalled_Test
			if (!sm_haPackages.isEmpty() && clienttasks.isPackageInstalled(sm_haPackages.get(0))) {
				clienttasks.yumRemovePackage(sm_haPackages.get(0));	// yum -y remove ccs
			}
			
			// remove the HA product cert too
			InstalledProduct haInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", HighAvailabilityTests.haProductId, clienttasks.getCurrentlyInstalledProducts());
			if (haInstalledProduct!=null) {
				ProductCert haInstalledProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", HighAvailabilityTests.haProductId, clienttasks.getCurrentProductCerts());
				log.warning("Manually removing installed High Availability product cert and restoring '"+clienttasks.productIdJsonFile+"' (you are probably running a RHEL5 client)...");
				client.runCommandAndWait("rm -f "+haInstalledProductCert.file.getPath());
				restoreProductIdJsonFileAfterClass();
				haInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", HighAvailabilityTests.haProductId, clienttasks.getCurrentlyInstalledProducts());
			}
			Assert.assertNull(haInstalledProduct, "The High Availability product id '"+HighAvailabilityTests.haProductId+"' should NOT be installed after successful removal of all High Availability packages.");
			
			throw new SkipException("Skipping test while bug '"+bugId+"' is open.");
		}
		// END OF WORKAROUND
		
		
		Assert.fail("This test will be implemented after RFE bug 922882 is implemented.");
	}
	
	

	// AllSlotsTestPlugin Tests ********************************************************

	@Test(	description="enable AllSlotsTestPlugin and assert the plugins list reports enablement",
			groups={},
			priority=810, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListWithEnabledAllSlotsTestPlugin_Test() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("all_slots_test.AllSlotsTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The AllSlotsTestPlugin is installed.");
		
		// enable and verify
		enableTestPluginAndVerifyListReport(installedPlugin);
	}
	@Test(	description="verify plugins listhooks reports all of the expected hooks for AllSlotsTestPlugin",
			groups={},
			priority=820, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListhooksWithEnabledAllSlotsTestPlugin_Test() {
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
	@Test(	description="execute subscription-manager modules and verify the expected AllSlotsTestPlugin hooks are called",
			groups={},
			priority=830, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyEnabledAllSlotsTestPluginHooksAreCalled_Test() {
		removeRhsmLog();
		
		// get the pre-registered facts on the system
		clienttasks.unregister(null,null,null);
		Map<String,String> facts = clienttasks.getFacts();
		
		// mark the rhsm.log file
		String logMarker = System.currentTimeMillis()+" Testing verifyEnabledAllSlotsTestPluginHooksAreCalled_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);

		// register and subscribe to random pool
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true,null,null,null,null));
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("Cannot randomly pick a pool for subscribing when there are no available pools for testing."); 
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribe(null,null,pool.poolId,null,null,null,null,null,null,null,null);
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
		List<String> expectedLogInfo= Arrays.asList(
				"Running handler for post_facts_collection_hook from slot post_facts_collection defined in all_slots_test",
				"Running handler for pre_register_consumer_hook from slot pre_register_consumer defined in all_slots_test",
				"Running handler for post_register_consumer_hook from slot post_register_consumer defined in all_slots_test",
				"Running handler for pre_subscribe_hook from slot pre_subscribe defined in all_slots_test",
				"Running handler for post_subscribe_hook from slot post_subscribe defined in all_slots_test",
				"");
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"),
				"The '"+clienttasks.rhsmLogFile+"' reports log messages: "+expectedLogInfo);
	}
	
	
	
	
	
	
	
	
	@Test(	description="verify changes to the rhsm.conf / [rhsm]pluginDir configuration",
			groups={},
			priority=1000, enabled=false)	// TODO
	//@ImplementsNitrateTest(caseId=)
	public void verifyRhsmPluginDirConfiguration_Test() {
		

	}
	
	@Test(	description="verify changes to the rhsm.conf / [rhsm]pluginConfDir configuration",
			groups={},
			priority=1000, enabled=false)	// TODO
	//@ImplementsNitrateTest(caseId=)
	public void verifyRhsmPluginConfDirConfiguration_Test() {
		

	}
	
	
	// Candidates for an automated Test:
	// see https://github.com/RedHatQE/rhsm-qe/issues
	
	// Configuration methods ***********************************************************************
	
	//@BeforeClass(groups={"setup"})	// not often enough due to recently added noisy logging:    2013-05-25 02:04:06,091 [DEBUG]  @injection.py:64 - Returning callable provider for feature ENT_DIR: <class 'subscription_manager.certdirectory.EntitlementDirectory'>
	protected void removeRhsmLog() {
		if (client==null) return;
		
		// remove the rhsm.log before this class to effectively reduce its size because it occasionally gets backed up to rhsm.log.1
		// in the midst of a pair of calls to RemoteFileTasks.markFile(...) and RemoteFileTasks.getTailFromMarkedFile(...)
		client.runCommandAndWait("rm -f "+clienttasks.rhsmLogFile);
	}
	
	@BeforeClass(groups={"setup"})
	protected void removeAllPluginsBeforeClass() {
		if (clienttasks==null) return;
		// get the plugin configuration directories
		pluginDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "pluginDir");
		pluginConfDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "pluginConfDir");
		Assert.assertNotNull(pluginDir, "Expecting rhsm.conf to contain a configuration for [rhsm]pluginDir");
		Assert.assertNotNull(pluginConfDir, "Expecting rhsm.conf to contain a configuration for [rhsm]pluginConfDir");
		
		// remove the currently configured plugins
		RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+pluginDir+"/*", Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+pluginConfDir+"/*", Integer.valueOf(0));
	}
		
	@BeforeGroups(groups={"setup"}, value="DisabledPluginTests")
	protected void fetchAndDisableAllPluginsBeforeGroups() {
		if (clienttasks==null) return;
		SSHCommandResult result;
		
		// fetch the test-plugins files
		if (sm_testpluginsUrl.isEmpty()) return; 
		log.info("Fetching test plugins from "+sm_testpluginsUrl+" for use by this test class...");
		RemoteFileTasks.runCommandAndAssert(client, "cd "+pluginDir+" && wget --quiet --recursive --no-host-directories --cut-dirs=2 --no-parent --accept .py "+sm_testpluginsUrl, Integer.valueOf(0)/*,null,"Downloaded: \\d+ files"*/);
		RemoteFileTasks.runCommandAndAssert(client, "cd "+pluginConfDir+" && wget --quiet --recursive --no-host-directories --cut-dirs=2 --no-parent --accept .conf "+sm_testpluginsUrl, Integer.valueOf(0)/*,null,"Downloaded: \\d+ files"*/);
		
		// create plugin objects for simplicity
		result = RemoteFileTasks.runCommandAndAssert(client, "find "+pluginConfDir+" -name *.conf", 0);
		for (String pluginConfPathname : result.getStdout().trim().split("\\s*\\n\\s*")) {
			if (pluginConfPathname.isEmpty()) continue;
			File examplePluginConfFile = new File(pluginConfDir+"/"+new File(pluginConfPathname).getName());
			File examplePluginFile = new File(pluginDir+"/"+examplePluginConfFile.getName().split("\\.")[0]+".py");
			installedPlugins.add(new Plugin(examplePluginConfFile,examplePluginFile));
		}
				
		// disable all of the plugin configurations
		for (Plugin installedPlugin : installedPlugins) {
			clienttasks.updateConfFileParameter(installedPlugin.configFile.getPath(), "enabled", "0");
		}

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


	
	// Protected methods ***********************************************************************

	protected final String backupProductIdJsonFile	= "/tmp/sm-productIdJsonFile";
	protected String pluginDir = null;
	protected String pluginConfDir = null;
	protected List<Plugin> installedPlugins = new ArrayList<Plugin>();
	final protected List<String> slots = Arrays.asList(
			"pre_register_consumer",	"post_register_consumer",
			"pre_product_id_install",	"post_product_id_install",
			"pre_subscribe",			"post_subscribe",
			"pre_auto_attach",			"post_auto_attach",
			/*"pre_facts_collection",*/"post_facts_collection");
	
	
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
    		if (line.startsWith("pre_")||line.startsWith("post_")) hookLine=false;
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
	
	List<String> getSupportArchesForHighAvailability() {
		// see HighAvailability.assertRhelServerBeforeClass()
		if (clienttasks.redhatReleaseX.equals("5")) {
			return Arrays.asList("x86_64","x86","i386","i686","ia64","ppc","ppc64");
		}
		if (clienttasks.redhatReleaseX.equals("6")) {
			return Arrays.asList("x86_64","x86","i386","i686");
		}
		log.warning("FIXME:  Do not know the supported arches are for RHEL "+clienttasks.redhatReleaseX);
		return Arrays.asList();
	}
    
	// Data Providers ***********************************************************************
	

}
