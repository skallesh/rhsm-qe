package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * The strategy for these tests is to exercise the example-plugins found in the upstream
 * subscription-manager git repository:
 * https://github.com/candlepin/subscription-manager/tree/master/example-plugins
 * 
 * Design Document:
 * https://engineering.redhat.com/trac/Entitlement/wiki/SubscriptionManagerPlugins
 */
@Test(groups={"PluginTests"})
public class PluginTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************

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
	
	
	
	
	@Test(	description="execute subscription-manager plugins --list with all plugins installed and diabled",
			groups={"DisabledPluginTests"},
			priority=40, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyPluginsListWithDisabledPluginsInstalled_Test() {
		if (installedPlugins.isEmpty()) throw new SkipException("There are no plugins installed to test.");
		SSHCommandResult pluginsListResult = clienttasks.plugins(null,null,null,null);
		SSHCommandResult pluginsListVerboseResult = clienttasks.plugins(true,null,null,true);
		
		// assert each of the installed plugins is reported as disabled
		//	[root@jsefler-7 ~]# subscription-manager plugins --list
		//	register_consumer.RegisterConsumerPlugin: disabled
		//	product_install.ProductInstallPlugin: disabled
		//	facts.FactsPlugin: disabled
		//	all_slots.AllSlotsPlugin: disabled
		//	dbus_event.DbusEventPlugin: disabled
		//	subscribe.SubscribePlugin: disabled
		for (Plugin installedPlugin : installedPlugins) {
			String expectedPluginReport = String.format("%s: %s", installedPlugin.key, "disabled");
			Assert.assertTrue(pluginsListResult.getStdout().contains(expectedPluginReport), "Stdout from the plugins list command reports expected plugin report '"+expectedPluginReport+"'.");
		}
		
		// assert each of the installed plugins is verbosely reported as disabled
		//	[root@jsefler-7 ~]# subscription-manager plugins --verbose
		//	register_consumer.RegisterConsumerPlugin: disabled
		//	plugin_key: register_consumer.RegisterConsumerPlugin
		//	config file: /etc/rhsm/pluginconf.d/register_consumer.RegisterConsumerPlugin.conf
		//	[main]
		//	enabled=0

		for (Plugin installedPlugin : installedPlugins) {
			String configFileContents = client.runCommandAndWaitWithoutLogging("cat "+installedPlugin.configFile.getPath()).getStdout().trim();
			String expectedPluginReport = "";
			expectedPluginReport += String.format("%s: %s\n", installedPlugin.key, "disabled");
			expectedPluginReport += String.format("%s: %s\n", "plugin_key", installedPlugin.key);
			expectedPluginReport += String.format("%s: %s\n", "config file", installedPlugin.configFile.getPath());
			expectedPluginReport += String.format("%s\n", configFileContents);
			Assert.assertTrue(pluginsListVerboseResult.getStdout().contains(expectedPluginReport), "Stdout from the verbose plugins list command reports expected plugin report: \n"+expectedPluginReport);
		}

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
		
		// fetch the example-plugins
		if (sm_examplePluginUrls.isEmpty()) return; 
		log.info("Fetching example-plugins for use by this test class...");
		for (String sm_examplePluginUrl : sm_examplePluginUrls) {
			if (sm_examplePluginUrl.endsWith(".py")) {
				RemoteFileTasks.runCommandAndAssert(client, "cd "+pluginDir+" && wget --quiet "+sm_examplePluginUrl, Integer.valueOf(0));
			} else if (sm_examplePluginUrl.endsWith(".conf")) {
				RemoteFileTasks.runCommandAndAssert(client, "cd "+pluginConfDir+" && wget --quiet "+sm_examplePluginUrl, Integer.valueOf(0));
			} else {
				log.warning("Do not know where example-plugins file '"+sm_examplePluginUrl+"' belongs.");
			}
		}
		
		// create plugin objects for simplicity
		for (String sm_examplePluginUrl : sm_examplePluginUrls) {
			if (sm_examplePluginUrl.endsWith(".conf")) {
				// https://github.com/candlepin/subscription-manager/raw/master/example-plugins/all_slots.AllSlotsPlugin.conf
				File examplePluginConfFile = new File(pluginConfDir+"/"+new File(sm_examplePluginUrl).getName());
				File examplePluginFile = new File(pluginDir+"/"+examplePluginConfFile.getName().split("\\.")[0]+".py");
				installedPlugins.add(new Plugin(examplePluginConfFile,examplePluginFile));
			}
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
    }
    
    
	// Data Providers ***********************************************************************
	

}
