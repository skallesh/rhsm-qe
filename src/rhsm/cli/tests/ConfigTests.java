package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.testng.TestNGUtils;
import rhsm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * See http://gibson.usersys.redhat.com/agilo/ticket/7219
 * Behavoir of this feature:

"subscription-manager config --list" will display the config values by section with the default values applied. Config file value replaces default value in each section if it exists. This includes a config file value of empty string.

"subscription-manager config --help" will show all available options including the full list of attributes.

"subscription-manager config --remove [section.name]" will remove the value for a specific section and name. If there is no default value for a named attribute, the attribute will be retained with an empty string. This allows for future changes to the attribute. If there is a default value for the attribute, then the config file attribute is removed so the default can be expressed. This again allows future changes to the attribute.

"subscription-manager config --[section.name] [value]" sets the value for an attribute by the section. Only existing attribute names are allowed. Adding new attribute names would not be useful anyway as the python code would be in need of altering to use it.

 */
@Test(groups={"ConfigTests","Tier2Tests"})
public class ConfigTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	
	@Test(	description="subscription-manager: use config --list to get the value of /etc/rhsm.rhsm.conf [server]ca_cert_dir (should not exist)",
			groups={"blockedByBug-993202"},
			priority=5,
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void GetCaCertDirValue_Test() {
		String section, parameter="ca_cert_dir";
		section="server";
		Assert.assertNull(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, section, parameter), "Config file '"+clienttasks.rhsmConfFile+"' section '"+section+"' parameter '"+parameter+"' should NOT be set.");
		section="rhsm";
		Assert.assertNotNull(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, section, parameter), "Config file '"+clienttasks.rhsmConfFile+"' section '"+section+"' parameter '"+parameter+"' should be set.");		
	}
	
	
	@Test(	description="subscription-manager: use config to set each of the rhsm.conf parameter values and verify it is persisted to /etc/rhsm.rhsm.conf",
			groups={"AcceptanceTests","Tier1Tests"},
			dataProvider="getConfigSectionNameData",
			priority=10,
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ConfigSetSectionNameValue_Test(Object bugzilla, String section, String name, String setValue) {
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[]{section, name.toLowerCase(), setValue});	// the config options require lowercase for --section.name=value, but the value written to conf.file may not be lowercase
		clienttasks.config(null,null,true,listOfSectionNameValues);
		
		// assert that the value was written to the config file
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, section, name), setValue, "After executing subscription-manager config to set '"+section+"."+name+"', the value is saved to config file '"+clienttasks.rhsmConfFile+"'.");
		
	}
	
	
	@Test(	description="subscription-manager: use config module to list all of the currently set rhsm.conf parameter values",
			groups={},
			dataProvider="getConfigSectionNameData",
			//dependsOnMethods={"ConfigSetSectionNameValue_Test"}, alwaysRun=true,
			priority=20,
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ConfigGetSectionNameValue_Test(Object bugzilla, String section, String name, String expectedValue) {

		// get a the config list (only once to save some unnecessary logging)
		// SSHCommandResult sshCommandResultFromConfigGetSectionNameValue_Test = clienttasks.config(true,null,null,(String[])null);
		if (sshCommandResultFromConfigGetSectionNameValue_Test == null) {
			sshCommandResultFromConfigGetSectionNameValue_Test = clienttasks.config(true,null,null,(String[])null);
		}
		
		//[root@jsefler-onprem-62server ~]# subscription-manager config --list
		//[server]
		//   ca_cert_dir = [/etc/rhsm/ca/]
		//   hostname = jsefler-onprem-62candlepin.usersys.redhat.com
		//   insecure = [0]
		//   port = [8443]
		//   prefix = [/candlepin]
		//   proxy_hostname = []
		//   proxy_password = []
		//   proxy_port = []
		//   proxy_user = []
		//   repo_ca_cert = [/etc/rhsm/ca/redhat-uep.pem]
		//   ssl_verify_depth = [3]
		//
		//[rhsm]
		//   baseurl = https://cdn.redhat.com
		//   ca_cert_dir = [/etc/rhsm/ca/]
		//   consumercertdir = /etc/pki/consumer
		//   entitlementcertdir = /etc/pki/entitlement
		//   hostname = [localhost]
		//   insecure = [0]
		//   port = [8443]
		//   prefix = [/candlepin]
		//   productcertdir = /etc/pki/product
		//   proxy_hostname = []
		//   proxy_password = []
		//   proxy_port = []
		//   proxy_user = []
		//   repo_ca_cert = [/etc/rhsm/ca/redhat-uep.pem]
		//   ssl_verify_depth = [3]
		//
		//[rhsmcertd]
		//   ca_cert_dir = [/etc/rhsm/ca/]
		//   certfrequency = 240
		//   hostname = [localhost]
		//   insecure = [0]
		//   port = [8443]
		//   prefix = [/candlepin]
		//   proxy_hostname = []
		//   proxy_password = []
		//   proxy_port = []
		//   proxy_user = []
		//   repo_ca_cert = [/etc/rhsm/ca/redhat-uep.pem]
		//   ssl_verify_depth = [3]
		//
		//[] - Default value in use

		// assert that the section name's expectedValue was listed
		String regexForName = "(?:"+name+"|"+name.toLowerCase()+")";	// note: python will write and tolerate all lowercase parameter names
		String regexForValue = "(?:"+expectedValue+"|\\["+expectedValue+"\\])";	// note: the value will be surrounded in square braces if it is identical to the hard-coded dev default
		String regexForSectionNameExpectedValue = "^\\["+section+"\\](?:\\n.*?)+^   "+regexForName+"\\s*[=:]\\s*"+regexForValue+"$";
		log.info("Using regex \""+regexForSectionNameExpectedValue+"\"to assert the expectedValue was listed by config.");	
		Pattern pattern = Pattern.compile(regexForSectionNameExpectedValue, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(sshCommandResultFromConfigGetSectionNameValue_Test.getStdout());

		Assert.assertTrue(matcher.find(),"After executing subscription-manager config to set '"+section+"."+name+"', calling config --list includes the value just set.");
	}
	protected SSHCommandResult sshCommandResultFromConfigGetSectionNameValue_Test = null;
	
	
	@Test(	description="subscription-manager: use config module to remove each of the rhsm.conf parameter values from /etc/rhsm/rhsm.conf",
			groups={},
			dataProvider="getConfigSectionNameData",
			//dependsOnMethods={"ConfigGetSectionNameValue_Test"}, alwaysRun=true,
			priority=30,
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ConfigRemoveSectionNameValue_Test(Object bugzilla, String section, String name, String value) {
		
		// use config to remove the section name value
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[]{section, name.toLowerCase(), value});	// the config options require lowercase for --remove=section.name  (note the value is not needed in the remove)

		clienttasks.config(null,true,null,listOfSectionNameValues);
		
		// assert that the parameter was removed from the config file (only for names in defaultConfFileParameterNames) otherwise the value is blanked
		String newValue = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, section, name);
		//if (clienttasks.defaultConfFileParameterNames(false).contains(name)) {	// RHEL63 behavior change
			Assert.assertNull(newValue, "After executing subscription-manager config to remove '"+section+"."+name.toLowerCase()+"', the parameter is absent from config file '"+clienttasks.rhsmConfFile+"'.");
		//} else {
		//	Assert.assertEquals(newValue, "", "After executing subscription-manager config to remove '"+section+"."+name.toLowerCase()+"', the parameter value is blanked from config file '"+clienttasks.rhsmConfFile+"'. (e.g. parameter_name = )");			
		//}
	}
	
	
	@Test(	description="subscription-manager: after having removed all the config parameters using the config module, assert that the config list shows the default values in use by wrapping them in [] and the others are simply blanked.",
			groups={},
			dataProvider="getConfigSectionNameData",
			//dependsOnMethods={"ConfigRemoveSectionNameValue_Test"}, alwaysRun=true,
			priority=40,
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ConfigGetSectionNameValueAndVerifyDefault_Test(Object bugzilla, String section, String name, String ignoreValue) {

		// get the config list (only once to save some unnecessary logging)
		// SSHCommandResult sshCommandResultFromConfigGetSectionNameValue_Test = clienttasks.config(true,null,null,(String[])null);
		if (sshCommandResultFromConfigGetSectionNameValueAndVerifyDefault_Test == null) {
			sshCommandResultFromConfigGetSectionNameValueAndVerifyDefault_Test = clienttasks.config(true,null,null,(String[])null);
		}
		
		//[root@jsefler-onprem-62server ~]# subscription-manager config --list
		//[server]
		//   ca_cert_dir = [/etc/rhsm/ca/]
		//   hostname =
		//   insecure = [0]
		//   port = [8443]
		//   prefix = [/candlepin]
		//   proxy_hostname = []
		//   proxy_password = []
		//   proxy_port = []
		//   proxy_user = []
		//   repo_ca_cert = [/etc/rhsm/ca/redhat-uep.pem]
		//   ssl_verify_depth = [3]
		//
		//[] - Default value in use

		// there are two cases to test
		// 1. there is a default value hard-coded for the parameter that will be used after having called config --remove section.name
		// 2. there is not a default value for the parameter yet it was set in the rhsm.conf file and is not set to "" after having called config --remove section.name

		// assert that the section name's expectedValue was listed
		String regexForName = "("+name+"|"+name.toLowerCase()+")";	// note: python will write and tolerate all lowercase parameter names
		String regexForValue = null;
		String assertMsg = "";
//		if (clienttasks.defaultConfFileParameterNames(true).contains(name.toLowerCase())) {	// case 1:	// valid before bug 988476
		if (clienttasks.defaultConfFileParameterNames(section,true).contains(name.toLowerCase())) {	// case 1:
			// value listed for name after having removed a parameter that has a default defined
			//   ca_cert_dir = [/etc/rhsm/ca/]
			regexForValue = "\\[.*\\]";
			assertMsg = "After executing subscription-manager config to remove '"+section+"."+name.toLowerCase()+"', calling config --list shows the default value for the parameter surrounded by square brackets[].";
		} else {	// case 2:
			// value listed for name after having removed a parameter that does NOT have a default defined
			//   hostname =
			regexForValue = "";
			assertMsg = "After executing subscription-manager config to remove '"+section+"."+name.toLowerCase()+"', calling config --list shows the default value as an empty string since this parameter has no default.";
		}
		String regexForSectionNameExpectedValue = "^\\["+section+"\\](\\n.*?)+^   "+regexForName+" = "+regexForValue+"$";
		log.info("Using regex \""+regexForSectionNameExpectedValue+"\"to assert the default value was listed by config after having removed the parameter name.");	
		Pattern pattern = Pattern.compile(regexForSectionNameExpectedValue, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(sshCommandResultFromConfigGetSectionNameValueAndVerifyDefault_Test.getStdout());

		Assert.assertTrue(matcher.find(),assertMsg);
	}
	protected SSHCommandResult sshCommandResultFromConfigGetSectionNameValueAndVerifyDefault_Test = null;


	@Test(	description="subscription-manager: use config module to simultaneously remove multiple rhsm.conf parameter values from /etc/rhsm/rhsm.conf",
			groups={"blockedByBug-735695","blockedByBug-927350"},
			//dependsOnMethods={"ConfigGetSectionNameValueAndVerifyDefault_Test"}, alwaysRun=true,
			priority=50,
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ConfigRemoveMultipleSectionNameValues_Test() {
		
		// not necessary but adds a little more to the test
		// restore the backup rhsm.conf file
		if (RemoteFileTasks.testExists(client,rhsmConfigBackupFile.getPath())) {
			log.info("Restoring the original rhsm config file...");
			client.runCommandAndWait("cat "+rhsmConfigBackupFile+" | tee "+clienttasks.rhsmConfFile);
		}
		
		// use config to remove the section name value all in one call
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		for (List<Object> row : getConfigSectionNameDataAsListOfLists()) {
			String section	= (String) row.get(1);
			String name		= (String) row.get(2);
			String value	= (String) row.get(3);
			listOfSectionNameValues.add(new String[]{section, name.toLowerCase(), value});	// the config options require lowercase for --remove=section.name  (note the value is not needed in the remove)
		}

		clienttasks.config(null,true,null,listOfSectionNameValues);
		
		// assert that the parameter was removed from the config file (only for names in defaultConfFileParameterNames) otherwise the value is blanked
		for (List<Object> row : getConfigSectionNameDataAsListOfLists()) {
			String section	= (String) row.get(1);
			String name		= (String) row.get(2);
			String value	= (String) row.get(3);
			String newValue = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, section, name);
//			if (clienttasks.defaultConfFileParameterNames(true).contains(name.toLowerCase())) {	// before bug 988476
			if (clienttasks.defaultConfFileParameterNames(section,true).contains(name.toLowerCase())) {
				Assert.assertNull(newValue, "After executing subscription-manager config to remove '"+section+"."+name.toLowerCase()+"', the parameter is removed from config file '"+clienttasks.rhsmConfFile+"'.");
			} else {
				Assert.assertEquals(newValue, "", "After executing subscription-manager config to remove '"+section+"."+name.toLowerCase()+"', the parameter value is blanked from config file '"+clienttasks.rhsmConfFile+"'. (e.g. parameter_name = )");			
			}
		}
	}
	
	
	@Test(	description="subscription-manager: attempt to use config module to remove a non-existing-section parameter from /etc/rhsm/rhsm.conf (negative test)",
			groups={"blockedByBug-747024","blockedByBug-746264"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ConfigRemoveNonExistingSectionName_Test() {
		String section = "non-existing-section";
		String name = "non-existing-parameter";
		String value = "non-existing-value";

		SSHCommandResult configResult = clienttasks.config_(null,true,null,new String[]{section, name, value});

		// assert results...
		Assert.assertEquals(configResult.getExitCode(), Integer.valueOf(255), "The exit code from a negative test attempt to remove a non-existing-section from the config.");
		//Assert.assertEquals(configResult.getStderr().trim(), String.format("No section: '%s'",section), "Stderr message");
		Assert.assertEquals(configResult.getStderr().trim(), String.format("Error: Section %s and name %s does not exist.",section,name), "Stderr message");	
		Assert.assertEquals(configResult.getStdout().trim(), "", "Stdout message should be empty");
	}
	
	
	@Test(	description="subscription-manager: attempt to use config module to remove a non-existing-parameter from a valid section in /etc/rhsm/rhsm.conf (negative test)",
			groups={"blockedByBug-736784"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ConfigRemoveNonExistingParameterFromValidSection_Test() {
		String section = "server";
		String name = "non-existing-parameter";
		String value = "value";

		SSHCommandResult configResult = clienttasks.config_(null,true,null,new String[]{section, name, value});
		
		// assert results...
		Assert.assertEquals(configResult.getExitCode(), Integer.valueOf(255), "The exit code from a negative test attempt to remove a non-existing-section from the config.");
		Assert.assertEquals(configResult.getStderr().trim(), String.format("Error: Section %s and name %s does not exist.",section,name));
		Assert.assertEquals(configResult.getStdout().trim(), "", "Stdout message should be empty");
		
		// assert that an empty parameter was not added to the config (bug 736784)
		String setValue = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, section, name);
		Assert.assertNull(setValue, "After executing a negative test to subscription-manager config to remove '"+section+"."+name+"', the parameter has not present in config file '"+clienttasks.rhsmConfFile+"'.");

	}
	
	
	@Test(	description="subscription-manager: attempt to use config module to list together with set and/or remove option(s) for config parameters",
			groups={"blockedByBug-730020"},
			dataProvider="getNegativeConfigListSetRemoveData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ConfigAttemptListWithSetAndRemoveOptions_Test(Object blockedByBug, Boolean list, Boolean remove, Boolean set, String[] section_name_value) {
		
		SSHCommandResult configResult = clienttasks.config_(list,remove,set,section_name_value);

		// assert results...
		Assert.assertEquals(configResult.getExitCode(), Integer.valueOf(255), "The exit code from a negative test attempt to combine list with set/remove options.");
		Assert.assertEquals(configResult.getStderr().trim(), "Error: --list should not be used with any other options for setting or removing configurations.", "Stderr message");
		Assert.assertEquals(configResult.getStdout().trim(), "", "Stdout message should be empty");
	}
	
	
	@Test(	description="subscription-manager: config (without any options) should default to --list",
			groups={"blockedByBug-811594"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ConfigDefaultsToConfigList_Test() {
		
		SSHCommandResult listResult = clienttasks.config(true, null, null, (String[])null);
		SSHCommandResult defaultResult = clienttasks.config(null, null, null, (String[])null);
		
		log.info("Asserting that that the default config result without specifying any options is the same as the result from config --list...");
		Assert.assertEquals(defaultResult.getExitCode(), listResult.getExitCode(),
				"exitCode from config without options should be equivalent to exitCode from config --list");
		Assert.assertEquals(defaultResult.getStderr(), listResult.getStderr(),
				"stderr from config without options should be equivalent to stderr from config --list");
		Assert.assertEquals(defaultResult.getStdout(), listResult.getStdout(),
				"stdout from config without options should be equivalent to stdout from config --list");
	}
	
	
	@Test(	description="subscription-manager: config for repo_ca_cert should interpolate the default value for ca_cert_dir",
			groups={"blockedByBug-997194","ConfigForRepoCaCertUsesDefaultCaCertDir_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ConfigForRepoCaCertUsesDefaultCaCertDir_Test() {
		
		// this bug is specifically designed to test Bug 997194 - repo_ca_cert configuration ignored using older configuration
		
		repoCaCertConfigured = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "repo_ca_cert");
		String caCertDirSubString = "%(ca_cert_dir)s";
		Assert.assertTrue(repoCaCertConfigured.startsWith(caCertDirSubString), "Configuration in '"+clienttasks.rhsmConfFile+"' for rhsm.repo_ca_cert ("+repoCaCertConfigured+") should start with '"+caCertDirSubString+"'.");
		
		String caCertDirInterpolated = clienttasks.getConfParameter("ca_cert_dir");
		String repoCaCertInterpolated = clienttasks.getConfParameter("repo_ca_cert");
		Assert.assertTrue(repoCaCertInterpolated.startsWith(caCertDirInterpolated), "subscription-manager config for repo_ca_cert ("+repoCaCertInterpolated+") should start with '"+caCertDirInterpolated+"'.");
		
		// now let's comment out the ca_cert_dir configuration as a alternative way to reproduce bug 997194
		clienttasks.commentConfFileParameter(clienttasks.rhsmConfFile, "ca_cert_dir");
		caCertDirInterpolated = clienttasks.getConfParameter("ca_cert_dir");
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "repo_ca_cert", caCertDirSubString+"candlepin-local.pem");
		Assert.assertEquals(clienttasks.getConfParameter("repo_ca_cert"), caCertDirInterpolated+"candlepin-local.pem", "After commenting out the config file '"+clienttasks.rhsmConfFile+"' parameter for ca_cert_dir, the interpolated value for repo_ca_cert should use the default value for ca_cert_dir.");
	}
	@AfterGroups(value={"ConfigForRepoCaCertUsesDefaultCaCertDir_Test"},groups={"setup"})
	public void afterConfigForRepoCaCertUsesDefaultCaCertDir_Test() {
		clienttasks.uncommentConfFileParameter(clienttasks.rhsmConfFile, "ca_cert_dir");
		if (repoCaCertConfigured!=null) clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "repo_ca_cert", repoCaCertConfigured);
	}
	protected String repoCaCertConfigured = null;
	
	
	@Test(	description="verify the default configurations for server hostname:port/prefix after running config removal",
			groups={"blockedByBug-988085","VerifyDefaultsForServerHostnamePortPrefixAfterConfigRemoval_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyDefaultsForServerHostnamePortPrefixAfterConfigRemoval_Test() {
		
		// this bug is specifically designed to test Bug 988085 - After running subscription-manager config --remove server.hostname, different default values available from GUI and CLI

		serverHostnameConfigured = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname");
		serverPortConfigured = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port");
		serverPrefixConfigured = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "prefix");
		
		// use the config command to remove the server configurations
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "server", "hostname".toLowerCase()});
		listOfSectionNameValues.add(new String[] { "server", "port".toLowerCase()});
		listOfSectionNameValues.add(new String[] { "server", "prefix".toLowerCase()});
		clienttasks.config(null, true, null, listOfSectionNameValues);
		
		// assert that they are removed
		for (String[] sectionNameValues : listOfSectionNameValues) {
			String section = sectionNameValues[0];
			String name = sectionNameValues[1];
			Assert.assertNull(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, section, name),"After using subscription-manager config to remove section '"+section+"' parameter '"+name+"', it should no longer be readable from config file '"+clienttasks.rhsmConfFile+"'.");
		}
		
		// assert the expected interpolated defaults
		Assert.assertEquals(clienttasks.getConfParameter("hostname"),"subscription.rhn.redhat.com","The interpolated default configuration for [server].hostname after it has been deleted from configuration file '"+clienttasks.rhsmConfFile+"'.");
		Assert.assertEquals(clienttasks.getConfParameter("port"),"443","The interpolated default configuration for [server].port after it has been deleted from configuration file '"+clienttasks.rhsmConfFile+"'.");
		Assert.assertEquals(clienttasks.getConfParameter("prefix"),"/subscription","The interpolated default configuration for [server].prefix after it has been deleted from configuration file '"+clienttasks.rhsmConfFile+"'.");
	}
	@AfterGroups(value={"VerifyDefaultsForServerHostnamePortPrefixAfterConfigRemoval_Test"},groups={"setup"})
	public void afterVerifyDefaultsForServerHostnamePortPrefixAfterConfigRemoval_Test() {
		if (serverHostnameConfigured!=null) clienttasks.config(null,null,true, new String[]{"server","hostname",serverHostnameConfigured});
		if (serverPortConfigured!=null) clienttasks.config(null,null,true, new String[]{"server","port",serverPortConfigured});
		if (serverPrefixConfigured!=null) clienttasks.config(null,null,true, new String[]{"server","prefix",serverPrefixConfigured});
	}
	protected String serverHostnameConfigured = null;
	protected String serverPortConfigured = null;
	protected String serverPrefixConfigured = null;
	
	
	@Test(	description="verify that only the expected configration parameters are present in the rhsm config file; useful for detecting newly added configurations by the subscription-manager developers",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyExpectedConfigParameters_Test() {
		
		SSHCommandResult rawConfigList = clienttasks.config(true, null, null, (String[])null);
		//	[root@jsefler-7 ~]# subscription-manager config --list
		//	[server]
		//	   hostname = jsefler-f14-candlepin.usersys.redhat.com
		//	   insecure = [0]
		//	   port = 8443
		//	   prefix = /candlepin
		//	   proxy_hostname = []
		//	   proxy_password = []
		//	   proxy_port = []
		//	   proxy_user = []
		//	   ssl_verify_depth = [3]
		//
		//	[rhsm]
		//	   baseurl = [https://cdn.redhat.com]
		//	   ca_cert_dir = [/etc/rhsm/ca/]
		//	   consumercertdir = [/etc/pki/consumer]
		//	   entitlementcertdir = [/etc/pki/entitlement]
		//	   full_refresh_on_yum = [0]
		//	   manage_repos = [1]
		//	   pluginconfdir = [/etc/rhsm/pluginconf.d]
		//	   plugindir = [/usr/share/rhsm-plugins]
		//	   productcertdir = [/etc/pki/product]
		//	   repo_ca_cert = [/etc/rhsm/ca/redhat-uep.pem]
		//	   report_package_profile = [1]
		//
		//	[rhsmcertd]
		//	   autoattachinterval = [1440]
		//	   certcheckinterval = [240]
		//
		//	[] - Default value in use
		
		// create a list of the actual config file parameter names listed above
		List<String> allActualConfFileParameterNames = new ArrayList<String>();
		for (String regexMatch : getSubstringMatches(rawConfigList.getStdout(),"^   [\\w]+")) {
			allActualConfFileParameterNames.add(regexMatch.trim().toLowerCase());
		}
		
		// create an expected list of the config file parameter names from our hardcoded testware defaultConfFileParameterNames() getter
		List<String> allExpectedConfFileParameterNames = new ArrayList<String>();
		allExpectedConfFileParameterNames.addAll(clienttasks.defaultConfFileParameterNames("server",true));
		allExpectedConfFileParameterNames.addAll(clienttasks.defaultConfFileParameterNames("rhsm",true));
		allExpectedConfFileParameterNames.addAll(clienttasks.defaultConfFileParameterNames("rhsmcertd",true));
		
		// assert that only the expected list of configuration parameters are listed
		boolean allActualConfFileParameterNameAreExpected=true;
		for (String actualConfFileParameterName : allActualConfFileParameterNames) {
			if (allExpectedConfFileParameterNames.contains(actualConfFileParameterName)) {
				Assert.assertTrue(allExpectedConfFileParameterNames.contains(actualConfFileParameterName),"Actual configuration parameter '"+actualConfFileParameterName+"' is among the expected configuration parameters.");
			} else {
				log.warning("Actual configuration parameter '"+actualConfFileParameterName+"' is NOT among the expected configuration parameters.  It is likely that this configuration parameter has recently been added by subscription-manager developers, and new test coverage is needed.");
				allActualConfFileParameterNameAreExpected=false;
			}
		}
		Assert.assertTrue(allActualConfFileParameterNameAreExpected,"All of the actual configuration parameters are among the expected configuration parameters.  If this fails, see the warnings logged above.");
	}
	
	
	
	
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 744654 - [ALL LANG] [RHSM CLI]config module_ config Server port with balnk or incorrect text produces traceback. https://github.com/RedHatQE/rhsm-qe/issues/121
	// TODO Bug 807721 - upgrading subscription-manager to rhel63 does not set a default rhsm.manage_repos configuration https://github.com/RedHatQE/rhsm-qe/issues/122
	
	
	// Protected Class Variables ***********************************************************************
	
	protected File rhsmConfigBackupFile = new File("/tmp/rhsm.conf.backup");
	
	
	
	// Protected methods ***********************************************************************
	
	
	
	
	// Configuration methods ***********************************************************************
	
	@BeforeClass(groups={"setup"})
	public void setupBeforeClass() {
		if (client==null) return;
		
		// unregister
		clienttasks.unregister_(null,null,null);
		
		// backup the current rhsm.conf file
		log.info("Backing up the current rhsm config file before executing this test class...");
		client.runCommandAndWait("cat "+clienttasks.rhsmConfFile+" | tee "+rhsmConfigBackupFile);
	}
	
	@AfterClass(groups={"setup"}, alwaysRun=true)
	public void cleanupAfterClass() {
		if (client==null) return;
		
		// restore the backup rhsm.conf file
		if (RemoteFileTasks.testExists(client,rhsmConfigBackupFile.getPath())) {
			log.info("Restoring the original rhsm config file...");
			client.runCommandAndWait("cat "+rhsmConfigBackupFile+" | tee "+clienttasks.rhsmConfFile+"; rm -f "+rhsmConfigBackupFile);
		}
	}
	
	
	
	// Data Providers ***********************************************************************
	

	@DataProvider(name="getNegativeConfigListSetRemoveData")
	public Object[][] getNegativeConfigListSetRemoveDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getNegativeConfigListSetRemoveDataAsListOfLists());
	}
	protected List<List<Object>> getNegativeConfigListSetRemoveDataAsListOfLists(){
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		// Object blockedByBug, Boolean list, Boolean remove, Boolean set, String[] section_name_value
		ll.add(Arrays.asList(new Object[]{null,	true,	true,	true,	new String[]{"server", "insecure", "1"}}));
		ll.add(Arrays.asList(new Object[]{null,	true,	false,	true,	new String[]{"server", "insecure", "1"}}));
		ll.add(Arrays.asList(new Object[]{null,	true,	true,	false,	new String[]{"server", "insecure", "1"}}));
		
		return ll;
	}
	
	
	

		
	@DataProvider(name="getConfigSectionNameData")
	public Object[][] getConfigSectionNameDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getConfigSectionNameDataAsListOfLists());
	}
	protected List<List<Object>> getConfigSectionNameDataAsListOfLists(){
		List<List<Object>> ll = new ArrayList<List<Object>>();
			
		// Object bugzilla,	String section,	String name, String testValue
//988476ll.add(Arrays.asList(new Object[]{null,							"server",		"ca_cert_dir",			"/tmp/server/ca_cert_dir"}));
		ll.add(Arrays.asList(new Object[]{null,							"server",		"hostname",				"server.hostname.redhat.com"}));
		ll.add(Arrays.asList(new Object[]{null,							"server",		"insecure",				"0"}));
		ll.add(Arrays.asList(new Object[]{null,							"server",		"port",					"2000"}));
		ll.add(Arrays.asList(new Object[]{null,							"server",		"prefix",				"/server/prefix"}));
		ll.add(Arrays.asList(new Object[]{null,							"server",		"proxy_hostname",		"server.proxy.hostname.redhat.com"}));
		ll.add(Arrays.asList(new Object[]{null,							"server",		"proxy_port",			"200"}));
		ll.add(Arrays.asList(new Object[]{null,							"server",		"proxy_password",		"server_proxy_password"}));
		ll.add(Arrays.asList(new Object[]{null,							"server",		"proxy_user",			"server_proxy_user"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"server",		"repo_ca_cert",			"/tmp/server/repo_ca_cert.pem"}));
		ll.add(Arrays.asList(new Object[]{null,							"server",		"ssl_verify_depth",		"2"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("807721"),	"server",		"manage_repos",			"1"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("807721"),	"server",		"baseurl",				"http://server.baseurl.com"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("807721"),	"server",		"entitlementcertdir",	"/tmp/server/entitlementcertdir"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("807721"),	"server",		"productcertdir",		"/tmp/server/productcertdir"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("807721"),	"server",		"consumercertdir",		"/tmp/server/consumercertdir"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"807721","882459"}),	"server",		/*"certFrequency" CHANGED BY BUG 882459 TO*/"certCheckInterval".toLowerCase(),		"200"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"807721","882459"}),	"server",		/*"healFrequency" CHANGED BY BUG 882459 TO*/"autoAttachInterval".toLowerCase(),		"2000"}));
		
		ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"baseurl",				"https://rhsm.baseurl.com"}));
		ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"ca_cert_dir",			"/tmp/rhsm/ca_cert_dir"}));
		ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"consumerCertDir",		"/tmp/rhsm/consumercertdir"}));
		ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"entitlementCertDir",	"/tmp/rhsm/entitlementcertdir"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"hostname",				"rhsm.hostname.redhat.com"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"insecure",				"1"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"port",					"1000"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"prefix",				"/rhsm/prefix"}));
		ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"productCertDir",		"/tmp/rhsm/productcertdir"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"proxy_hostname",		"rhsm.proxy.hostname.redhat.com"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"proxy_password",		"rhsm_proxy_password"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"proxy_port",			"100"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"proxy_user",			"rhsm_proxy_user"}));
		ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"repo_ca_cert",			"/tmp/rhsm/repo_ca_cert.pem"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"ssl_verify_depth",		"1"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("797996"),	"rhsm",			"manage_repos",			"0"}));
		ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"report_package_profile",	"0"}));
		ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"plugindir",				"/tmp/rhsm/plugindir"}));
		ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"pluginconfdir",			"/tmp/rhsm/pluginconfdir"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"807721","882459"}),	"rhsm",			/*"certFrequency" CHANGED BY BUG 882459 TO*/"certCheckInterval".toLowerCase(),		"100"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"807721","882459"}),	"rhsm",			/*"healFrequency" CHANGED BY BUG 882459 TO*/"autoAttachInterval".toLowerCase(),		"1000"}));
		if (clienttasks.isPackageVersion("python-rhsm",">=","1.10.6-1")) ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"full_refresh_on_yum",	"1"}));	// was added as part of RFE Bug 803746  python_rhsm commit 1bbbfad490bb7985a50d80465f726e7514825a1a
		
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsmcertd",	"ca_cert_dir",			"/tmp/rhsmcertd/ca_cert_dir"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("882459"),	"rhsmcertd",	/*"certFrequency" CHANGED BY BUG 882459 TO*/"certCheckInterval".toLowerCase(),		"300"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("882459"),	"rhsmcertd",	/*"healFrequency" CHANGED BY BUG 882459 TO*/"autoAttachInterval".toLowerCase(),		"3000"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsmcertd",	"hostname",				"rhsmcertd.hostname.redhat.com"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsmcertd",	"insecure",				"0"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsmcertd",	"port",					"3000"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsmcertd",	"prefix",				"/rhsmcertd/prefix"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsmcertd",	"proxy_hostname",		"rhsmcertd.proxy.hostname.redhat.com"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsmcertd",	"proxy_password",		"rhsmcertd_proxy_password"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsmcertd",	"proxy_port",			"300"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsmcertd",	"proxy_user",			"rhsmcertd_proxy_user"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsmcertd",	"repo_ca_cert",			"/tmp/rhsmcertd/repo_ca_cert.pem"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsmcertd",	"ssl_verify_depth",		"3"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("807721"),	"rhsmcertd",	"manage_repos",			"1"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("807721"),	"rhsmcertd",	"baseurl",				"http://rhsmcertd.baseurl.com"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("807721"),	"rhsmcertd",	"entitlementcertdir",	"/tmp/rhsmcertd/entitlementcertdir"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("807721"),	"rhsmcertd",	"productcertdir",		"/tmp/rhsmcertd/productcertdir"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("807721"),	"rhsmcertd",	"consumercertdir",		"/tmp/rhsmcertd/consumercertdir"}));
		
		return ll;
	}
}
