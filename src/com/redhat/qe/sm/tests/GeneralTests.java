package com.redhat.qe.sm.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.RemoteFileTasks;

public class GeneralTests extends Setup{
	
	@Test(description="Verify subscription-manager-cli command line options for help.",dataProvider="commandLineOptionsData")
	public void CommandLineOptions_Test(String command, String stdoutGrepExpression, String stderrGrepExpression, int expectedExitCode) {
		log.info("Testing subscription-manager-cli command line options '"+command+"' and verifying the output.");
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command,stdoutGrepExpression,stderrGrepExpression,expectedExitCode);
	}
	
	@DataProvider(name="commandLineOptionsData")
	public Object[][] getCommandLineOptionsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getCommandLineOptionsDataAsListOfLists());
	}
	protected List<List<Object>> getCommandLineOptionsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		for (String grinderHelpCommand : new String[]{"subscription-manager-cli -h","subscription-manager-cli --help"}) {
			ll.add(Arrays.asList(new Object[]{ grinderHelpCommand, "Usage: subscription-manager-cli [options] MODULENAME --help", null, 0 }));
			ll.add(Arrays.asList(new Object[]{ grinderHelpCommand, "^[[:tab:]]+list^[[:space:]]+list available or consumer subscriptions for registered user", null, 0 }));
			ll.add(Arrays.asList(new Object[]{ grinderHelpCommand, "^[[:tab:]]+register^[[:space:]]+register the client to a Unified Entitlement Platform.", null, 0 }));
			ll.add(Arrays.asList(new Object[]{ grinderHelpCommand, "^[[:tab:]]+subscribe^[[:space:]]+subscribe the registered user to a specified product or regtoken.", null, 0 }));
			ll.add(Arrays.asList(new Object[]{ grinderHelpCommand, "^[[:tab:]]+unsubscribe^[[:space:]]+unsubscribe the registered user from all or specific subscriptions.", null, 0 }));
		}
		
		return ll;
	}
}
