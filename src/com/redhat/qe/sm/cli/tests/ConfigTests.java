package com.redhat.qe.sm.cli.tests;

import java.util.List;

import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.SubscriptionPool;
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
			enabled=false)
	//@ImplementsNitrateTest(caseId=)
	public void Config_Test() {
		
	}
	
	
	
	// Configuration methods ***********************************************************************
	
	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
