package rhsm.cli.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import org.testng.SkipException;
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
import com.redhat.qe.tools.SSHCommandRunner;

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

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-36675", "RHEL7-51520"})
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-20113", "RHEL7-33091"})
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-36676", "RHEL7-51521"})
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-36677", "RHEL7-51522"})
	@Test(	description="subscription-manager: use config module to remove each of the rhsm.conf parameter values from /etc/rhsm/rhsm.conf",
			groups={"blockedByBug-1223860"},
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
		/* RHEL63 behavior change
		if (clienttasks.defaultConfFileParameterNames(false).contains(name)) {
			Assert.assertNull(newValue, "After executing subscription-manager config to remove '"+section+"."+name.toLowerCase()+"', the parameter is absent from config file '"+clienttasks.rhsmConfFile+"'.");
		} else {
			Assert.assertEquals(newValue, "", "After executing subscription-manager config to remove '"+section+"."+name.toLowerCase()+"', the parameter value is blanked from config file '"+clienttasks.rhsmConfFile+"'. (e.g. parameter_name = )");			
		}
		*/
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.14.8-1")) {	// commit d1160c303a0a015c97a6cf28bc084fc058f0ebf6	// Bug 1223860 - subscription-manager config --rhsmcertd.autoattachinterval adds configuration with incorrect case.
			Assert.assertEquals(newValue, defaultConfFileParameterMap.get(section+"."+name), "After executing subscription-manager config to remove '"+section+"."+name.toLowerCase()+"', the parameter's configuration value is restored to it's hard-coded default value in config file '"+clienttasks.rhsmConfFile+"'.  (Prior to Bug 1223860, it was completely removed from the config file)");
		} else {
			Assert.assertNull(newValue, "After executing subscription-manager config to remove '"+section+"."+name.toLowerCase()+"', the parameter is absent from config file '"+clienttasks.rhsmConfFile+"'.");
		}
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-36678", "RHEL7-51523"})
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
		
		// WORKAROUND FOR https://bugzilla.redhat.com/show_bug.cgi?id=1225600#c5
		if (section.equals("rhsm") && name.equals("repo_ca_cert") && clienttasks.isPackageVersion("python-rhsm", ">=", "1.14.3-1")) {	// commit 09bf7957ac8bfbd6b6fed20b78aa3a8879a2c953	// Bug 1225600 - subscription-manager config --remove=rhsm.repo_ca_cert does not exactly restore the default value 
			Assert.assertTrue(!matcher.find(),"After executing subscription-manager config to remove '"+section+"."+name.toLowerCase()+"', calling config --list DOES NOT show the default value for the parameter surrounded by square brackets[] as explained in https://bugzilla.redhat.com/show_bug.cgi?id=1225600#c5.");
		} else
		
		// assert the default value is indicated in the config --list
		Assert.assertTrue(matcher.find(),assertMsg);
	}
	protected SSHCommandResult sshCommandResultFromConfigGetSectionNameValueAndVerifyDefault_Test = null;


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-36679", "RHEL7-51524"})
	@Test(	description="subscription-manager: use config module to simultaneously remove multiple rhsm.conf parameter values from /etc/rhsm/rhsm.conf",
			groups={"blockedByBug-735695","blockedByBug-927350","blockedByBug-1297337"},
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
				
				if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.14.8-1")) {	// commit d1160c303a0a015c97a6cf28bc084fc058f0ebf6	// Bug 1223860 - subscription-manager config --rhsmcertd.autoattachinterval adds configuration with incorrect case.
					Assert.assertEquals(newValue, defaultConfFileParameterMap.get(section+"."+name), "After executing subscription-manager config to remove '"+section+"."+name.toLowerCase()+"', the parameter's configuration value is restored to it's hard-coded default value in config file '"+clienttasks.rhsmConfFile+"'.  (Prior to Bug 1223860, it was completely removed from the config file)");
				} else {
					Assert.assertNull(newValue, "After executing subscription-manager config to remove '"+section+"."+name.toLowerCase()+"', the parameter is removed from config file '"+clienttasks.rhsmConfFile+"'.");
				}
			} else {
				Assert.assertEquals(newValue, "", "After executing subscription-manager config to remove '"+section+"."+name.toLowerCase()+"', the parameter value is blanked from config file '"+clienttasks.rhsmConfFile+"'. (e.g. parameter_name = )");			
			}
		}
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-36671", "RHEL7-51517"})
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
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(configResult.getExitCode(), Integer.valueOf(78)/*EX_CONFIG*/, "The exit code from a negative test attempt to remove a non-existing-section from the config.");
		} else {
			Assert.assertEquals(configResult.getExitCode(), Integer.valueOf(255), "The exit code from a negative test attempt to remove a non-existing-section from the config.");
		}
		//Assert.assertEquals(configResult.getStderr().trim(), String.format("No section: '%s'",section), "Stderr message");
		Assert.assertEquals(configResult.getStderr().trim(), String.format("Error: Section %s and name %s does not exist.",section,name), "Stderr message");	
		Assert.assertEquals(configResult.getStdout().trim(), "", "Stdout message should be empty");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-36670", "RHEL7-51516"})
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
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(configResult.getExitCode(), Integer.valueOf(78)/*EX_CONFIG*/, "The exit code from a negative test attempt to remove a non-existing-section from the config.");
		} else {
			Assert.assertEquals(configResult.getExitCode(), Integer.valueOf(255), "The exit code from a negative test attempt to remove a non-existing-section from the config.");
		}
		Assert.assertEquals(configResult.getStderr().trim(), String.format("Error: Section %s and name %s does not exist.",section,name));
		Assert.assertEquals(configResult.getStdout().trim(), "", "Stdout message should be empty");
		
		// assert that an empty parameter was not added to the config (bug 736784)
		String setValue = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, section, name);
		Assert.assertNull(setValue, "After executing a negative test to subscription-manager config to remove '"+section+"."+name+"', the parameter has not present in config file '"+clienttasks.rhsmConfFile+"'.");

	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-36667", "RHEL7-51513"})
	@Test(	description="subscription-manager: attempt to use config module to list together with set and/or remove option(s) for config parameters",
			groups={"blockedByBug-730020"},
			dataProvider="getNegativeConfigListSetRemoveData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ConfigAttemptListWithSetAndRemoveOptions_Test(Object blockedByBug, Boolean list, Boolean remove, Boolean set, String[] section_name_value) {
		
		SSHCommandResult configResult = clienttasks.config_(list,remove,set,section_name_value);

		// assert results...
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(configResult.getExitCode(), Integer.valueOf(64)/*EX_USAGE*/, "The exit code from a negative test attempt to combine list with set/remove options.");
		} else {
			Assert.assertEquals(configResult.getExitCode(), Integer.valueOf(255), "The exit code from a negative test attempt to combine list with set/remove options.");
		}
		Assert.assertEquals(configResult.getStderr().trim(), "Error: --list should not be used with any other options for setting or removing configurations.", "Stderr message");
		Assert.assertEquals(configResult.getStdout().trim(), "", "Stdout message should be empty");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-36668", "RHEL7-51514"})
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-36669", "RHEL7-51515"})
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-36673", "RHEL7-51518"})
	@Test(	description="verify the default configurations for server hostname:port/prefix after running config removal",
			groups={"blockedByBug-988085","blockedByBug-1223860","blockedByBug-1297337","VerifyDefaultsForServerHostnamePortPrefixAfterConfigRemoval_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyDefaultsForServerHostnamePortPrefixAfterConfigRemoval_Test() {
		
		// this bug is specifically designed to test Bug 988085 - After running subscription-manager config --remove server.hostname, different default values available from GUI and CLI

		if (serverHostnameConfigured==null) serverHostnameConfigured = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname");
		if (serverPortConfigured==null) serverPortConfigured = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port");
		if (serverPrefixConfigured==null) serverPrefixConfigured = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "prefix");
		
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
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.14.8-1")) {	// commit d1160c303a0a015c97a6cf28bc084fc058f0ebf6	// Bug 1223860 - subscription-manager config --rhsmcertd.autoattachinterval adds configuration with incorrect case.
				Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, section, name),defaultConfFileParameterMap.get("server."+name), "After using subscription-manager config to remove section '"+section+"' parameter '"+name+"', it is restored to its hard-coded default in config file '"+clienttasks.rhsmConfFile+"'.  (Prior to Bug 1223860, it was completely removed from the config file)");
			} else {
				Assert.assertNull(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, section, name),"After using subscription-manager config to remove section '"+section+"' parameter '"+name+"', it should no longer be readable from config file '"+clienttasks.rhsmConfFile+"'.");
			}
		}
		
		// assert the expected interpolated defaults
		Assert.assertEquals(clienttasks.getConfParameter("hostname"),defaultConfFileParameterMap.get("server.hostname"),"The interpolated default configuration for [server].hostname after it has been deleted from configuration file '"+clienttasks.rhsmConfFile+"'.");
		Assert.assertEquals(clienttasks.getConfParameter("port"),defaultConfFileParameterMap.get("server.port"),"The interpolated default configuration for [server].port after it has been deleted from configuration file '"+clienttasks.rhsmConfFile+"'.");
		Assert.assertEquals(clienttasks.getConfParameter("prefix"),defaultConfFileParameterMap.get("server.prefix"),"The interpolated default configuration for [server].prefix after it has been deleted from configuration file '"+clienttasks.rhsmConfFile+"'.");
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-36674", "RHEL7-51519"})
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
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.10-1")) {// RHEL7.3 commit d84b15f42c2e4521e130b939039960c0846b849c 1334916: Move logging configuration to rhsm.conf
		allExpectedConfFileParameterNames.addAll(clienttasks.defaultConfFileParameterNames("logging",true));
		}
		
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-36672", "RHEL7-57876"})
	@Test(	description="verify the [server]server_timeout can be configured and function properly when the server does not respond within the timeout seconds",
			groups={"blockedByBug-1346417","VerifyConfigServerTimeouts_Test"},
			enabled=true)
	// TODO: Verifies: https://polarion.engineering.redhat.com/polarion/#/project/RHEL6/workitem?id=RHEL6-28580
	//@ImplementsNitrateTest(caseId=)
	public void VerifyConfigServerTimeouts_Test() throws IOException {
		// this bug is specifically designed to test Bug 1346417 - [RFE] Allow users to set socket timeout.
		if (clienttasks.isPackageVersion("python-rhsm", "<", "1.17.3-1")) {  // python-rhsm commit 5780140650a59d45a03372a0390f92fd7c3301eb Allow users to set socket timeout.
			throw new SkipException("This test applies a newer version of python-rhsm that includes an implementation for RFE Bug 1346417 - Allow users to set socket timeout.");
		}
		
		// before we test this bug, assert that the manual setup service is running...
		// instructions for setting this up are in scripts/timeout_listener.sh
		// let's use the auto-services.usersys.redhat.com as the timeout listener server sm_basicauthproxyHostname
		SSHCommandRunner timeoutServerCommandRunner = new SSHCommandRunner(sm_basicauthproxyHostname, sm_basicauthproxySSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase, null);
		SSHCommandResult timeoutServerServiceResult = timeoutServerCommandRunner.runCommandAndWait("systemctl is-active timeout_listener.service");
		Assert.assertContainsMatch(timeoutServerServiceResult.getStdout().trim(), "^active$","The timeout_listener.service is running.  If this fails, then a one-time setup of the timeout_listener server on '"+sm_basicauthproxyHostname+"' is needed.  See the instructions in the automation scripts/timeout_listener.sh file.");
		
		// fetch the timeout_listener server  CA Cert
		log.info("Fetching timeout_listener CA cert...");
		File remoteTimeoutServerCaCertFile = new File ("/root/timeout_listener/timeout_listener.pem");	// manually created on the timeoutServer as follows...
		//	[root@auto-services timeout_listener]# openssl genrsa -out timeout_listener.key 4096
		//	[root@auto-services timeout_listener]# openssl req -new -x509 -key timeout_listener.key -out timeout_listener.pem -days 3650 -subj '/CN=auto-services.usersys.redhat.com/C=US/L=Raleigh'
		File localTimeoutServerCaCertFile = new File((getProperty("automation.dir", "/tmp")+"/tmp/"+remoteTimeoutServerCaCertFile.getName().replace("tmp/tmp", "tmp")));
		RemoteFileTasks.getFile(timeoutServerCommandRunner.getConnection(), localTimeoutServerCaCertFile.getParent(), remoteTimeoutServerCaCertFile.getPath());
		RemoteFileTasks.putFile(client.getConnection(), localTimeoutServerCaCertFile.getPath(), clienttasks.caCertDir+"/", "0644");
		
		// remember originally configured server configs
		if (serverHostnameConfigured==null) serverHostnameConfigured = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname");
		if (serverPortConfigured==null) serverPortConfigured = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port");
		
		// make sure server_timeout configuration is absent from the rhsm.conf file
		clienttasks.removeConfFileParameter(clienttasks.rhsmConfFile, "server_timeout");
		
		// use the config command to set rhsm configurations to point to the timeout server
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "server", "hostname", sm_basicauthproxyHostname});
		listOfSectionNameValues.add(new String[] { "server", "port", "8883"});	// manually created on the timeoutServer as follows...
		//	[root@auto-services timeout_listener]# yum install nmap-ncat
		//	[root@auto-services timeout_listener]# nc --ssl --ssl-key ./timeout_listener.key --ssl-cert ./timeout_listener.pem --listen --keep-open 8883
		clienttasks.config(null, null, true, listOfSectionNameValues);
		
		String command;
		Long sshCommandTimeout;
		SSHCommandResult result;
		List<String> realTimeList;
		String expectedStdout = "UNKNOWN STDOUT";
		String expectedStderr = "UNKNOWN STDERR";
		String expectedLogMessage = "UNKNOWN LOG ERROR";
		String marker = System.currentTimeMillis()+" Testing VerifyConfigServerTimeouts_Test...";
		String logResult;
		
		// test the default server_time value of 180 seconds
		if (clienttasks.redhatReleaseX.equals("7")) {
			//	2017-05-12 17:51:10,209 [ERROR] subscription-manager:23673:MainThread @utils.py:274 - Error while checking server version: ('The read operation timed out',)
			//	2017-05-12 17:51:10,209 [ERROR] subscription-manager:23673:MainThread @utils.py:276 - ('The read operation timed out',)
			expectedLogMessage = "Error while checking server version: ('The read operation timed out',)";
			
			//	[root@jsefler-rhel7 ~]# time subscription-manager version 
			//	Unable to verify server's identity: timed out
			//
			//	real	3m0.568s
			//	user	0m0.226s
			//	sys		0m0.036s
			expectedStderr = "Unable to verify server's identity: timed out";
			expectedStdout = "";
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.18.4-1")) { // post commit b0e877cfb099184f9bab1b681a41df9bdd2fb790 m2crypto dependency removal on RHEL7
				//	[root@jsefler-rhel7 ~]# time subscription-manager version
				//	server type: This system is currently not registered.
				//	subscription management server: Unknown
				//	subscription management rules: Unknown
				//	subscription-manager: 1.19.12-1.el7
				//	python-rhsm: 1.19.6-1.el7
				//
				//	real	3m0.482s
				//	user	0m0.215s
				//	sys	0m0.057s
				expectedStderr = "";
				expectedStdout = "server type: This system is currently not registered.\nsubscription management server: Unknown\nsubscription management rules: Unknown";
			}
		}
		if (clienttasks.redhatReleaseX.equals("6")) {
			//	2017-05-12 18:39:05,350 [ERROR] subscription-manager:14672:MainThread @utils.py:259 - Timeout error while checking server version
			//	2017-05-12 18:39:05,351 [ERROR] subscription-manager:14672:MainThread @utils.py:260 -
			expectedLogMessage = "Timeout error while checking server version";
			
			//	[root@jsefler-rhel6 ~]# time subscription-manager version
			//	Unable to verify server's identity: 
			//
			//	real	3m0.555s
			//	user	0m0.287s
			//	sys		0m0.045s
			expectedStderr = "Unable to verify server's identity:";
			expectedStdout = "";
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.18.4-1")) { // post commit b0e877cfb099184f9bab1b681a41df9bdd2fb790 side affect from m2crypto dependency removal on RHEL7
				//	[root@jsefler-rhel6 ~]# subscription-manager version 
				//	System certificates corrupted. Please reregister.
				expectedStderr = "System certificates corrupted. Please reregister.";	
				// Note: This ^ new stderr is not the greatest message; however if you follow the instructions and try to reregister, you will eventually hit the original stderr....
				//	[root@jsefler-rhel6 ~]# subscription-manager register --force
				//	Registering to: auto-services.usersys.redhat.com:8883/candlepin
				//	Username: testuser1
				//	Password: 
				//	Unable to verify server's identity: 
				//	[root@jsefler-rhel6 ~]# 
			}
		}
		
		String serverDefaultTimeout = "180";	// seconds (assumed hard-coded default)
		command = "time "+clienttasks.versionCommand(null, null, null, null);
		sshCommandTimeout = new Long(200); // seconds	// default server_timeout is 180 seconds
		
		marker = System.currentTimeMillis()+" Testing VerifyConfigServerTimeouts_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, marker);
		result = client.runCommandAndWait(command, Long.valueOf(sshCommandTimeout *1000));
		clienttasks.logRuntimeErrors(result);
		logResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, marker, "ERROR").trim();
		Assert.assertTrue(logResult.contains(expectedLogMessage),"Log file '"+clienttasks.rhsmLogFile+"' contains expected message '"+expectedLogMessage+"'.");
		realTimeList = getSubstringMatches(result.getStderr(), "real\\s+.*");	// extract the matches to: real	3m0.568s
		if (realTimeList.size()!=1) Assert.fail("Failed to find the real time it took to run command '"+command+"'.  (The automated test gave up waiting for the server to reply after '"+sshCommandTimeout+"' seconds.  Is the server hung?)");
		Assert.assertTrue(realTimeList.get(0).replaceFirst("real\\s+", "").startsWith("3m0."),"Testing server_timeout="+serverDefaultTimeout+" seconds actually times out at this time.");	// using startsWith() to tolerate fractional seconds
		Assert.assertTrue(result.getStderr().startsWith(expectedStderr),"When a server_timeout occurs, subscription-manager stderr starts with '"+expectedStderr+"'.");
		Assert.assertTrue(result.getStdout().startsWith(expectedStdout),"When a server_timeout occurs, subscription-manager stdout starts with '"+expectedStdout+"'.");
		
		
		// also test a server_time value of N seconds
		for (String server_timeout : Arrays.asList("4","10")) {	// seconds
			listOfSectionNameValues.clear();
			listOfSectionNameValues.add(new String[] { "server", "server_timeout", server_timeout});
			clienttasks.config(null, null, true, listOfSectionNameValues);
			command = "time "+clienttasks.versionCommand(null, null, null, null);
			sshCommandTimeout = new Long(200); // seconds	// default server_timeout is 180 seconds
			marker = System.currentTimeMillis()+" Testing VerifyConfigServerTimeouts_Test...";
			RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, marker);
			result = client.runCommandAndWait(command, Long.valueOf(sshCommandTimeout *1000));
			clienttasks.logRuntimeErrors(result);
			logResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, marker, "ERROR").trim();
			Assert.assertTrue(logResult.contains(expectedLogMessage),"Log file '"+clienttasks.rhsmLogFile+"' contains expected message '"+expectedLogMessage+"'.");
			realTimeList = getSubstringMatches(result.getStderr(), "real\\s+.*");	// extract the matches to: real	3m0.568s
			if (realTimeList.size()!=1) Assert.fail("Failed to find the real time it took to run command '"+command+"'.  (The automated test gave up waiting for the server to reply after '"+sshCommandTimeout+"' seconds.  Is the server hung?)");
			Assert.assertTrue(realTimeList.get(0).replaceFirst("real\\s+", "").startsWith("0m"+server_timeout+"."),"Testing server_timeout="+server_timeout+" seconds actually times out at this time");	// using startsWith() to tolerate fractional seconds
			Assert.assertTrue(result.getStderr().startsWith(expectedStderr),"When a server_timeout occurs, subscription-manager stderr starts with '"+expectedStderr+"'.");
			Assert.assertTrue(result.getStdout().startsWith(expectedStdout),"When a server_timeout occurs, subscription-manager stdout starts with '"+expectedStdout+"'.");
		}
	}
	@AfterGroups(value={"VerifyConfigServerTimeouts_Test"},groups={"setup"})
	public void afterVerifyConfigServerTimeouts_Test() {
		if (serverHostnameConfigured!=null) clienttasks.config(null,null,true, new String[]{"server","hostname",serverHostnameConfigured});
		if (serverPortConfigured!=null) clienttasks.config(null,null,true, new String[]{"server","port",serverPortConfigured});
		//clienttasks.config_(null, true, null, new String[]{"server","server_timeout"});	// will actually leave "server_timeout = 180" set in the rhsm.conf file
		clienttasks.removeConfFileParameter(clienttasks.rhsmConfFile, "server_timeout");
	}
	
	
	// Candidates for an automated Test:
	// TODO Bug 744654 - [ALL LANG] [RHSM CLI]config module_ config Server port with balnk or incorrect text produces traceback. https://github.com/RedHatQE/rhsm-qe/issues/121
	// TODO Bug 807721 - upgrading subscription-manager to rhel63 does not set a default rhsm.manage_repos configuration https://github.com/RedHatQE/rhsm-qe/issues/122
	
	
	// Protected Class Variables ***********************************************************************
	
	protected File rhsmConfigBackupFile = new File("/tmp/rhsm.conf.backup");
	// hard-code defaults
	protected Map<String,String> defaultConfFileParameterMap = new HashMap<String,String>(){
		// [root@jsefler-os6 ~]# cat /etc/rhsm/rhsm.conf
		// # Red Hat Subscription Manager Configuration File:
		//
		// # Unified Entitlement Platform Configuration
		// [server]
		// # Server hostname:
		{put("server.hostname","subscription.rhn.redhat.com");}
		// # Server prefix:
		{put("server.prefix","/subscription");}
		// # Server port:
		{put("server.port","443");}
		// # Set to 1 to disable certificate validation:
		{put("server.insecure","0");}
		// # Set the depth of certs which should be checked when validating a certificate
		{put("server.ssl_verify_depth","3");}
		// # an http proxy server to use
		{put("server.proxy_hostname","");}
		// # port for http proxy server
		{put("server.proxy_port","");}
		// # user name for authenticating to an http proxy, if needed
		{put("server.proxy_user","");}
		// # password for basic http proxy auth, if needed
		{put("server.proxy_password","");}
		//
		// [rhsm]
		// # Content base URL:
		{put("rhsm.baseurl","https://cdn.redhat.com");}
		// # Server CA certificate location:
		{put("rhsm.ca_cert_dir","/etc/rhsm/ca/");}
		// # Where the certificates should be stored
		{put("rhsm.productCertDir","/etc/pki/product");}
		{put("rhsm.entitlementCertDir","/etc/pki/entitlement");}
		{put("rhsm.consumerCertDir","/etc/pki/consumer");}
		// # Refresh repo files with server overrides on every yum command
		{put("rhsm.full_refresh_on_yum","0");}
		// # Manage generation of yum repositories for subscribed content:
		{put("rhsm.manage_repos","1");}
		// # The directory to search for plugin configuration files
		{put("rhsm.pluginConfDir","/etc/rhsm/pluginconf.d");}
		// # The directory to search for subscription manager plugins
		{put("rhsm.pluginDir","/usr/share/rhsm-plugins");}
		// # Default CA cert to use when generating yum repo configs:
		{put("rhsm.repo_ca_cert","%(ca_cert_dir)sredhat-uep.pem");}
		// # If set to zero, the client will not report the package profile to the subscription management service.
		{put("rhsm.report_package_profile","1");}
		//
		// [rhsmcertd]
		// # Interval to run auto-attach (in minutes):
		{put("rhsmcertd.autoAttachInterval","1440");}
		// # Interval to run cert check (in minutes):
		{put("rhsmcertd.certCheckInterval","240");}
	};

	
	
	// Protected methods ***********************************************************************
	
	
	
	
	// Configuration methods ***********************************************************************
	
	@BeforeClass(groups={"setup"})
	public void setupBeforeClass() {
		if (client==null) return;
		
		// unregister
		clienttasks.unregister_(null,null,null, null);
		
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
	
	@BeforeClass(groups={"setup"})
	public void updateDefaultConfFileParameterMapBeforeClass() {
		if (client==null) return;
		
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.16.6-1")) {	// python-rhsm commit be526f9b501b7621e8ed89844f4c6172ef3273c2	// 1297337: change server strings to new default
			// Bug 1297337 - The new default server url "subscription.rhsm.redhat.com" is not provided after clicking "default" button on gui
			// Bug 1278472 - [RFE] change default registration url to subscription.rhsm.redhat.com
			defaultConfFileParameterMap.put("server.hostname","subscription.rhsm.redhat.com");
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
		if (clienttasks==null) return ll;
			
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
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("1225600"),"rhsm",			"repo_ca_cert",			"/tmp/rhsm/repo_ca_cert.pem"}));
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"ssl_verify_depth",		"1"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("797996"),	"rhsm",			"manage_repos",			"0"}));
		ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"report_package_profile",	"0"}));
		ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"pluginDir",				"/tmp/rhsm/plugindir"}));
		ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"pluginConfDir",			"/tmp/rhsm/pluginconfdir"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"807721","882459"}),	"rhsm",			/*"certFrequency" CHANGED BY BUG 882459 TO*/"certCheckInterval".toLowerCase(),		"100"}));
//988476ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"807721","882459"}),	"rhsm",			/*"healFrequency" CHANGED BY BUG 882459 TO*/"autoAttachInterval".toLowerCase(),		"1000"}));
		if (clienttasks.isPackageVersion("python-rhsm",">=","1.10.6-1")) ll.add(Arrays.asList(new Object[]{null,							"rhsm",			"full_refresh_on_yum",	"1"}));	// was added as part of RFE Bug 803746  python_rhsm commit 1bbbfad490bb7985a50d80465f726e7514825a1a
		
//988476ll.add(Arrays.asList(new Object[]{null,							"rhsmcertd",	"ca_cert_dir",			"/tmp/rhsmcertd/ca_cert_dir"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("882459"),	"rhsmcertd",	/*"certFrequency" CHANGED BY BUG 882459 TO*/"certCheckInterval",		"300"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("882459"),	"rhsmcertd",	/*"healFrequency" CHANGED BY BUG 882459 TO*/"autoAttachInterval",		"3000"}));
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
