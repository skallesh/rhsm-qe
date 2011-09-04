package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.SubscriptionPool;
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


 * Reference: https://bugzilla.redhat.com/show_bug.cgi?id=730020
 */
@Test(groups={"ConfigTests"})
public class ConfigTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager: ",
			groups={},
			dataProvider="getConfigSectionNameData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ConfigSetSectionNameValue_Test(Object bugzilla, String section, String name, String value) {
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[]{section, name.toLowerCase(), value});	// the config options require lowercase for --section.name=value, but the value written to conf.file may not be lowercase
		clienttasks.config(null,null,true,listOfSectionNameValues);
		
		// assert that the value was written to the config file
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, section, name), value, "After executing subscription-manager config to set '"+section+"."+name+"', the value is saved to config file '"+clienttasks.rhsmConfFile+"'.");
		
	}
	
	@Test(	description="subscription-manager: ",
			groups={},
			dataProvider="getConfigSectionNameData",
			dependsOnMethods={"ConfigSetSectionNameValue_Test"},
			enabled=false)
	//@ImplementsNitrateTest(caseId=)
	public void ConfigGetSectionNameValue_Test(Object bugzilla, String section, String name, String value) {
		
	}
	
	// Protected Class Variables ***********************************************************************
	
	protected File rhsmConfigBackupFile = new File("/tmp/rhsm.conf.backup");
	
	// Protected methods ***********************************************************************
	
	
	
	
	// Configuration methods ***********************************************************************
	
	@BeforeClass(groups={"setup"})
	public void setupBeforeClass() throws Exception {
		if (client==null) return;
		
		// unregister
		clienttasks.unregister_(null,null,null);
		
		// backup the current rhsm.conf file
		log.info("Backing up the current rhsm config file before executing this test class...");
		client.runCommandAndWait("cat "+clienttasks.rhsmConfFile+" | tee "+rhsmConfigBackupFile);

		
	}
	
	@AfterClass(groups={"setup"}, alwaysRun=true)
	public void cleanupAfterClass() throws Exception {
		if (client==null) return;
		
		// restore the backup rhsm.conf file
		if (RemoteFileTasks.testFileExists(client,rhsmConfigBackupFile.getPath())==1) {
			log.info("Restoring the original rhsm config file...");
			client.runCommandAndWait("cat "+rhsmConfigBackupFile+" | tee "+clienttasks.rhsmConfFile+"; rm -f "+rhsmConfigBackupFile);
		}
	}

	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="getConfigSectionNameData")
	public Object[][] getConfigSectionNameDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getConfigSectionNameDataAsListOfLists());
	}
	protected List<List<Object>> getConfigSectionNameDataAsListOfLists(){
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		// Object bugzilla,	String section,	String name, String testValue
		ll.add(Arrays.asList(new Object[]{null,	"server",	"ca_cert_dir",			"/tmp/server/ca_cert_dir"}));
		ll.add(Arrays.asList(new Object[]{null,	"server",	"hostname",				"server.hostname.redhat.com"}));
		ll.add(Arrays.asList(new Object[]{null,	"server",	"insecure",				"0"}));
		ll.add(Arrays.asList(new Object[]{null,	"server",	"port",					"2000"}));
		ll.add(Arrays.asList(new Object[]{null,	"server",	"prefix",				"/server/prefix"}));
		ll.add(Arrays.asList(new Object[]{null,	"server",	"proxy_hostname",		"server.proxy.hostname.redhat.com"}));
		ll.add(Arrays.asList(new Object[]{null,	"server",	"proxy_port",			"200"}));
		ll.add(Arrays.asList(new Object[]{null,	"server",	"proxy_password",		"server_proxy_password"}));
		ll.add(Arrays.asList(new Object[]{null,	"server",	"proxy_user",			"server_proxy_user"}));
		ll.add(Arrays.asList(new Object[]{null,	"server",	"repo_ca_cert",			"/tmp/server/repo_ca_cert.pem"}));
		ll.add(Arrays.asList(new Object[]{null,	"server",	"ssl_verify_depth",		"2"}));
		
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"baseurl",					"https://baseurl.redhat.com"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"ca_cert_dir",				"/tmp/rhsm/ca_cert_dir"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"consumerCertDir",			"/tmp/rhsm/consumercertdir"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"entitlementCertDir",		"/tmp/rhsm/entitlementcertdir"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"hostname",					"rhsm.hostname.redhat.com"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"insecure",					"1"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"port",						"1000"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"prefix",					"/rhsm/prefix"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"productCertDir",			"/tmp/rhsm/productcertdir"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"proxy_hostname",			"rhsm.proxy.hostname.redhat.com"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"proxy_password",			"rhsm_proxy_password"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"proxy_port",				"100"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"proxy_user",				"rhsm_proxy_user"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"repo_ca_cert",				"/tmp/rhsm/repo_ca_cert.pem"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsm",	"ssl_verify_depth",			"1"}));

		ll.add(Arrays.asList(new Object[]{null,	"rhsmcertd",	"ca_cert_dir",		"/tmp/rhsmcertd/ca_cert_dir"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsmcertd",	"certFrequency",	"300"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsmcertd",	"hostname",			"rhsmcertd.hostname.redhat.com"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsmcertd",	"insecure",			"0"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsmcertd",	"port",				"3000"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsmcertd",	"prefix",			"/rhsmcertd/prefix"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsmcertd",	"proxy_hostname",	"rhsmcertd.proxy.hostname.redhat.com"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsmcertd",	"proxy_password",	"rhsmcertd_proxy_password"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsmcertd",	"proxy_port",		"300"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsmcertd",	"proxy_user",		"rhsmcertd_proxy_user"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsmcertd",	"repo_ca_cert",		"/tmp/rhsmcertd/repo_ca_cert.pem"}));
		ll.add(Arrays.asList(new Object[]{null,	"rhsmcertd",	"ssl_verify_depth",	"3"}));

		
		return ll;
	}
}
