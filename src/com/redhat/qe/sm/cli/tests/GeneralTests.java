package com.redhat.qe.sm.cli.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.BlockedByBzBug;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.SubscriptionManagerTasks;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author ssalevan
 * @author jsefler
 *
 */
@Test(groups={"general"})
public class GeneralTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************
	
	
	@Test(	description="subscription-manager-cli: attempt to access functionality without registering",
			dataProvider="UnregisteredCommandData")
	@ImplementsNitrateTest(caseId=41697)
	public void AttemptingCommandsWithoutBeingRegistered_Test(String command) {
		log.info("Testing subscription-manager-cli command without being registered, expecting it to fail: "+ command);
		clienttasks.unregister(null, null, null);
		//RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner, command);
		RemoteFileTasks.runCommandAndAssert(client,command,1,"^Error: You need to register this system by running `register` command before using this option.",null);

	}
	
	
	@Test(	description="subscription-manager-cli: attempt to access functionality that does not exist",
			dataProvider="NegativeFunctionalityData")
	public void AttemptingCommandsThatDoNotExist_Test(String command, int expectedExitCode, String stderrGrepExpression, String stdoutGrepExpression) {
		log.info("Testing subscription-manager-cli command that does not exist, expecting it to fail: "+ command);
		RemoteFileTasks.runCommandAndAssert(client,command,expectedExitCode,stdoutGrepExpression,stderrGrepExpression);

	}
	
	
	// Data Providers ***********************************************************************
	

	
	@DataProvider(name="UnregisteredCommandData")
	public Object[][] getUnregisteredCommandDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getUnregisteredCommandDataAsListOfLists());
	}
	public List<List<Object>> getUnregisteredCommandDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
		//ll.add(Arrays.asList(new Object[]{clienttasks.command+" facts --update"}));  test moved to FactsTests.FactsWhenNotRegistered_Test()
		//ll.add(Arrays.asList(new Object[]{clienttasks.command+" identity"}));  test moved to IdentityTests.IdentityWhenNotRegistered_Test()
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" list"}));
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" list --available --all"}));
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" list --available"}));
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" list --consumed"}));
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" refresh"}));
// this functionality appears to have been removed: subscription-manager-0.71-1.el6.i686  - jsefler 7/21/2010
//		ll.add(Arrays.asList(new Object[]{clienttasks.command+" subscribe --product=FOO"}));
// this functionality appears to have been removed: subscription-manager-0.93.14-1.el6.x86_64 - jsefler 1/21/2011
//		ll.add(Arrays.asList(new Object[]{clienttasks.command+" subscribe --regtoken=FOO"}));
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" subscribe --pool=FOO"}));
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe"}));
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe --all"}));
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe --serial=FOO"}));
// this functionality appears to have been removed: subscription-manager-0.68-1.el6.i686  - jsefler 7/12/2010
//		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe --product=FOO"}));
//		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe --regtoken=FOO"}));
//		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe --pool=FOO"}));

		return ll;
	}
	
	@DataProvider(name="NegativeFunctionalityData")
	public Object[][] getNegativeFunctionalityDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getNegativeFunctionalityDataAsListOfLists());
	}
	protected List<List<Object>> getNegativeFunctionalityDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
		// due to design changes, this is a decent place to dump old commands that have been removed
		
		// String command, int expectedExitCode, String stderrGrepExpression, String stdoutGrepExpression
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe --product=FOO", 2, clienttasks.command+": error: no such option: --product",null}));
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe --regtoken=FOO", 2, clienttasks.command+": error: no such option: --regtoken",null}));
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe --pool=FOO", 2, clienttasks.command+": error: no such option: --pool",null}));
		
		return ll;
	}
}
