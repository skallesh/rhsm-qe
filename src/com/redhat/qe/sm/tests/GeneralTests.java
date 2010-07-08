package com.redhat.qe.sm.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.tools.RemoteFileTasks;

public class GeneralTests extends SubscriptionManagerTestScript{
	
	@Test(description="subscription-manager-cli: ensure manpages and usage information are accurate",
			dataProvider="HelpTextData",
			groups={"sm_stage1"})
	@ImplementsTCMS(id="41697")
	public void HelpTextPresent_Test(String command, String stdoutGrepExpression, String stderrGrepExpression, int expectedExitCode) {
		log.info("Testing subscription-manager-cli command line options '"+command+"' and verifying the output.");
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, 
				command,
				expectedExitCode,
				stdoutGrepExpression,
				stderrGrepExpression);
	}
	
	
	@Test(description="subscription-manager-cli: attempt to access functionality without registering",
			dataProvider="NegativeFunctionalityData",
			groups={"sm_stage1"})
	@ImplementsTCMS(id="41697")
	public void NegativeFunctionality_Test(String command) {
		log.info("Testing subscription-manager-cli command without registering, expecting it to fail: "+ command);
		RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner, command);
	}
	
	
	
	// Data Providers ***********************************************************************

	@DataProvider(name="HelpTextData")
	public Object[][] getHelpTextDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getHelpTextDataAsListOfLists());
	}
	protected List<List<Object>> getHelpTextDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (String smHelpCommand : new String[]{"subscription-manager-cli -h","subscription-manager-cli --help"}) {
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, "^Usage: subscription-manager-cli \\[options\\] MODULENAME --help", null, 0 }));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, "^\tlist +list available or consumer subscriptions for registered user", null, 0 }));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, "^\tregister +register the client to a Unified Entitlement Platform.", null, 0 }));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, "^\tsubscribe +subscribe the registered user to a specified product or regtoken.", null, 0 }));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, "^\tunsubscribe +unsubscribe the registered user from all or specific subscriptions.", null, 0 }));
		}
		
		return ll;
	}
	
	@DataProvider(name="NegativeFunctionalityData")
	public Object[][] getNegativeFunctionalityDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getNegativeFunctionalityDataAsListOfLists());
	}
	public List<List<Object>> getNegativeFunctionalityDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli list --available"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli list --consumed"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli subscribe --product=FOO"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli subscribe --regtoken=FOO"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli subscribe --pool=FOO"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli unsubscribe --product=FOO"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli unsubscribe --regtoken=FOO"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli unsubscribe --pool=FOO"}));
		
		return ll;
	}
}
