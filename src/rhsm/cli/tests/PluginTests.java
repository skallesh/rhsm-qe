package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import java.util.regex.Pattern;

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
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
@Test(groups={"PluginTests","AcceptanceTest"})
public class PluginTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************
	
	
	// UninstalledPluginTests *************************************************************

	@Test(	description="execute subscription-manager plugins --listslots",
			groups={},
			priority=10, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListslots_Test() {
		List<String> actualSlots = Arrays.asList(clienttasks.plugins(null,true,null,null).getStdout().trim().split("\n"));
		Assert.assertTrue(actualSlots.containsAll(slots)&&slots.containsAll(actualSlots), "All of these expected slots are listed: "+slots);
		actualSlots = Arrays.asList(clienttasks.plugins(null,true,null,true).getStdout().trim().split("\n"));
		Assert.assertTrue(actualSlots.containsAll(slots)&&slots.containsAll(actualSlots), "All of these expected slots are listed (--verbose should have no affect): "+slots);
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
		Assert.assertEquals(clienttasks.plugins(null,null,true,null).getStdout(),clienttasks.plugins(null,true,null,null).getStdout(), "When there are no plugins installed, the expected --listhooks report should be identical to the --listslots.");
		Assert.assertEquals(clienttasks.plugins(null,null,true,true).getStdout(),clienttasks.plugins(null,true,null,null).getStdout(), "When there are no plugins installed, the expected --listhooks --verbose report should be identical to the --listslots.");
	}
	
	
	
	// DisabledPluginTests ****************************************************************
	
	@Test(	description="execute subscription-manager plugins --list (with plugins installed and disabled)",
			groups={"DisabledPluginTests"},
			priority=40, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListWithDisabledPluginsInstalled_Test() {
		if (installedPlugins.isEmpty()) throw new SkipException("There are no plugins installed to test.");
		
		// assert each of the installed plugins is reported as disabled
		SSHCommandResult pluginsListResult = clienttasks.plugins(null,null,null,null);
		for (Plugin installedPlugin : installedPlugins) {
			String expectedPluginReport = getExpectedPluginListReport(installedPlugin, "disabled", false);
			Assert.assertTrue(pluginsListResult.getStdout().contains(expectedPluginReport), "Stdout from the plugins list command reports expected plugin report '"+expectedPluginReport+"'.");
		}
		
		// assert each of the installed plugins is verbosely reported as disabled
		SSHCommandResult pluginsListVerboseResult = clienttasks.plugins(true,null,null,true);
		for (Plugin installedPlugin : installedPlugins) {
			String expectedPluginReport = getExpectedPluginListReport(installedPlugin, "disabled", true);
			Assert.assertTrue(pluginsListVerboseResult.getStdout().contains(expectedPluginReport), "Stdout from the verbose plugins list command reports expected plugin report: \n"+expectedPluginReport);
		}

	}
	
	@Test(	description="execute subscription-manager plugins --listhooks (with plugins installed and disabled)",
			groups={"DisabledPluginTests"},
			priority=50, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListhooksWithDisabledPluginsInstalled_Test() {
		Assert.assertEquals(clienttasks.plugins(null,null,true,null).getStdout(),clienttasks.plugins(null,true,null,null).getStdout(), "When all plugins installed are disabled, the expected --listhooks report should be identical to the --listslots.");
		Assert.assertEquals(clienttasks.plugins(null,null,true,true).getStdout(),clienttasks.plugins(null,true,null,null).getStdout(), "When all plugins installed are disabled, the expected --listhooks --verbose report should be identical to the --listslots.");
	}
	
	
	
	// RegisterConsumerTestPluginTests *************************************************
	
	@Test(	description="enable the RegisterConsumerTestPlugin and assert the plugins list reports enablement",
			groups={"RegisterConsumerTestPluginTests"},
			priority=60, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListWithEnabledRegisterConsumerTestPlugin_Test() {
		
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("register_consumer1.RegisterConsumerTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The RegisterConsumerTestPlugin is installed.");
		
		// enable the plugin
		clienttasks.updateConfFileParameter(installedPlugin.configFile.getPath(), "enabled", "1");
		
		// verify the plugins list reports RegisterConsumerTestPlugin is enabled
		String expectedPluginReport = getExpectedPluginListReport(installedPlugin, "enabled", false);
		Assert.assertTrue(clienttasks.plugins(null,null,null,null).getStdout().contains(expectedPluginReport), "Stdout from the plugins list command reports expected plugin report '"+expectedPluginReport+"'.");
		
		// verify the verbose plugins list reports RegisterConsumerTestPlugin is enabled
		expectedPluginReport  = getExpectedPluginListReport(installedPlugin, "enabled", true);
		Assert.assertTrue(clienttasks.plugins(null,null,null,true).getStdout().contains(expectedPluginReport), "Stdout from the verbose plugins list command reports expected plugin report: \n"+expectedPluginReport);
	}
	@Test(	description="verify plugins listhooks reports all of the expected hooks for RegisterConsumerTestPlugin",
			groups={"RegisterConsumerTestPluginTests"},
			dependsOnMethods={"verifyPluginsListWithEnabledRegisterConsumerTestPlugin_Test"},
			priority=70, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListhooksWithEnabledRegisterConsumerTestPlugin_Test() {
		// find the plugin from the list of installed plugins
		Plugin installedPlugin = getPlugin("register_consumer1.RegisterConsumerTestPlugin");
		Assert.assertTrue(installedPlugin!=null, "The RegisterConsumerTestPlugin is installed.");
		
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
			Assert.assertTrue(parseHooksForSlotFromPluginsListhooksResult(slot, pluginsListhooksResult).isEmpty(), "The plugins listhooks reports no hooks for slot '"+slot+"' because RegisterConsumerTestPlugin is the only enabled plugin.");
		}
	}
	@Test(	description="execute subscription-manager modules and verify the expected RegisterConsumerTestPlugin hooks are called",
			groups={"RegisterConsumerTestPluginTests"},
			dependsOnMethods={"verifyPluginsListWithEnabledRegisterConsumerTestPlugin_Test"},
			priority=80, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyEnabledRegisterConsumerTestPluginHooksAreCalled_Test() {
		
		// get the pre-registered facts on the system
		Map<String,String> facts = clienttasks.getFacts();
		
		// mark the rhsm.log file
		String logMarker = System.currentTimeMillis()+" Testing verifyEnabledRegisterConsumerTestPluginHooksAreCalled_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);

		// register and assert the pre and post hooks are logged
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true,null,null,null,null);


		// get the tail of the marked rhsm.log file
		//	2013-03-14 13:37:45,277 [DEBUG]  @plugins.py:695 - Running pre_register_consumer_hook in register_consumer1.RegisterConsumerTestPlugin
		//	2013-03-14 13:37:45,277 [INFO]  @register_consumer1.py:30 - Running pre_register_consumer_hook: consumer name jsefler-7.usersys.redhat.com is about to be registered.
		//	2013-03-14 13:37:45,278 [INFO]  @register_consumer1.py:31 - Running pre_register_consumer_hook: consumer facts count is 99
		//
		//	2013-03-14 13:37:45,782 [DEBUG]  @plugins.py:695 - Running post_register_consumer_hook in register_consumer1.RegisterConsumerTestPlugin
		//	2013-03-14 13:37:45,782 [INFO]  @register_consumer1.py:39 - Running post_register_consumer_hook: consumer uuid 48db2a61-9af2-46ed-966a-374e0a94f9c4 is now registered.

		String logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Running pre_register_consumer_hook").trim();
		// assert the expected log calls
		List<String> expectedLogInfo= Arrays.asList(
				"Running pre_register_consumer_hook in register_consumer1.RegisterConsumerTestPlugin",
				"Running pre_register_consumer_hook: consumer name "+clienttasks.hostname+" is about to be registered.",
				"Running pre_register_consumer_hook: consumer facts count is "+facts.values().size());
		Assert.assertTrue(logTail.replaceAll("\n","").matches(".*"+joinListToString(expectedLogInfo,".*")+".*"), "The '"+clienttasks.rhsmLogFile+"' reports expected log messages: "+expectedLogInfo);
	}
	
	
	
	
	
	
	
	@Test(	description="verify changes to the rhsm.conf / [rhsm]pluginDir configuration",
			groups={},
			priority=1000, enabled=false)
	//@ImplementsNitrateTest(caseId=)
	public void verifyRhsmPluginDirConfiguration_Test() {
		

	}
	
	@Test(	description="verify changes to the rhsm.conf / [rhsm]pluginConfDir configuration",
			groups={},
			priority=1000, enabled=false)
	//@ImplementsNitrateTest(caseId=)
	public void verifyRhsmPluginConfDirConfiguration_Test() {
		

	}
	
	
	// Candidates for an automated Test:
	// see https://github.com/RedHatQE/rhsm-qe/issues
	
	// Configuration methods ***********************************************************************

	@BeforeClass(groups={"setup"})
	public void removeAllPluginsBeforeClass() {
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
	public void fetchAndDisableAllPluginsBeforeGroups() {
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


	
	// Protected methods ***********************************************************************
	
	protected String pluginDir = null;
	protected String pluginConfDir = null;
	protected List<Plugin> installedPlugins = new ArrayList<Plugin>();
	final protected List<String> slots = Arrays.asList(
			"pre_register_consumer",	"post_register_consumer",
			"pre_product_id_install",	"post_product_id_install",
			"pre_subscribe",			"post_subscribe",
			/*"post_facts_collection",*/"post_facts_collection");
	
	
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
    
	// Data Providers ***********************************************************************
	

}
