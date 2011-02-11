package com.redhat.qe.sm.cli.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.SkipException;
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
@Test(groups={"help"})
public class HelpTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************
	
	@Test(	description="subscription-manager-cli: man page",
			groups={},
			enabled=true)
	@ImplementsNitrateTest(caseId=41697)
	public void ManPage_Test() {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		RemoteFileTasks.runCommandAndAssert(client,"man -P cat "+clienttasks.command,0);
		RemoteFileTasks.runCommandAndAssert(client,"man -k "+clienttasks.command,0,"^subscription-manager ",null);
		RemoteFileTasks.runCommandAndAssert(client,"man -k "+clienttasks.command,0,"^subscription-manager-gui ",null);
	}
	
	
	@Test(	description="subscription-manager-cli: assert only expected command line options are available",
			groups={"blockedByBug-664581"},
			dataProvider="ExpectedCommandLineOptionsData")
	@ImplementsNitrateTest(caseId=46713)
	public void ExpectedCommandLineOptions_Test(String command, String stdoutRegex, List<String> expectedOptions) {
		log.info("Testing subscription-manager-cli command line options '"+command+"' and verifying that only the expected options are available.");
		SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(client,command,0);
		
		Pattern pattern = Pattern.compile(stdoutRegex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(result.getStdout());
		Assert.assertTrue(matcher.find(),"Available command line options are shown with command: "+command);
		
		// find all the matches to stderrRegex
		List <String> actualOptions = new ArrayList<String>();
		do {
			actualOptions.add(matcher.group().trim());
		} while (matcher.find());
		
		// assert all of the expectedOptions were found and that no unexpectedOptions were found
		for (String expectedOption : expectedOptions) {
			if (!actualOptions.contains(expectedOption)) {
				log.warning("Could not find the expected command '"+command+"' option '"+expectedOption+"'.");
			} else {
				Assert.assertTrue(actualOptions.contains(expectedOption),"The expected command '"+command+"' option '"+expectedOption+"' is available.");
			}
		}
		for (String actualOption : actualOptions) {
			if (!expectedOptions.contains(actualOption))
				log.warning("Found an unexpected command '"+command+"' option '"+actualOption+"'.");
		}
		Assert.assertTrue(actualOptions.containsAll(expectedOptions), "All of the expected command '"+command+"' line options are available.");
		Assert.assertTrue(expectedOptions.containsAll(actualOptions), "All of the available command '"+command+"' line options are expected.");
	}
	
	
	
	
	
	// Data Providers ***********************************************************************

	
	@DataProvider(name="ExpectedCommandLineOptionsData")
	public Object[][] getExpectedCommandLineOptionsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getExpectedCommandLineOptionsDataAsListOfLists());
	}
	protected List<List<Object>> getExpectedCommandLineOptionsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
		// String command, String stdoutRegex, List<String> expectedOptions
		String modulesRegex = "^	\\w+";
		String optionsRegex = "^  --\\w+[(?:=\\w)]*|^  -\\w[(?:=\\w)]*\\, --\\w+[(?:=\\w)]*";
		
		// MODULES
		List <String> modules = new ArrayList<String>();
		modules.add("clean");
		modules.add("facts");
		modules.add("identity");
		modules.add("list");
		modules.add("refresh");
		modules.add("register");
//		modules.add("reregister");
		modules.add("subscribe");
		modules.add("unregister");
		modules.add("unsubscribe");
		for (String smHelpCommand : new String[]{clienttasks.command+" -h",clienttasks.command+" --help"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: "+clienttasks.command+" [options] MODULENAME --help";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, modulesRegex, modules}));
		}
		
		// MODULE: clean
		List <String> cleanOptions = new ArrayList<String>();
		cleanOptions.add("-h, --help");
		cleanOptions.add("--debug=DEBUG");
		// removed in https://bugzilla.redhat.com/show_bug.cgi?id=664581
		//cleanOptions.add("--proxy=PROXY_URL");
		//cleanOptions.add("--proxyuser=PROXY_USER");
		//cleanOptions.add("--proxypassword=PROXY_PASSWORD");
		for (String smHelpCommand : new String[]{clienttasks.command+" -h clean",clienttasks.command+" --help clean"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: "+clienttasks.command+" clean [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, optionsRegex, cleanOptions}));
		}
		
		// MODULE: facts
		List <String> factsOptions = new ArrayList<String>();
		factsOptions.add("-h, --help");
		factsOptions.add("--debug=DEBUG");
//		factsOptions.add("-k, --insecure");
		factsOptions.add("--list");
		factsOptions.add("--update");
		factsOptions.add("--proxy=PROXY_URL");
		factsOptions.add("--proxyuser=PROXY_USER");
		factsOptions.add("--proxypassword=PROXY_PASSWORD");
		for (String smHelpCommand : new String[]{clienttasks.command+" -h facts",clienttasks.command+" --help facts"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: "+clienttasks.command+" facts [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, optionsRegex, factsOptions}));
		}
		
		// MODULE: identity
		List <String> identityOptions = new ArrayList<String>();
		identityOptions.add("-h, --help");
		identityOptions.add("--debug=DEBUG");
		identityOptions.add("--username=USERNAME");
		identityOptions.add("--password=PASSWORD");
		identityOptions.add("--regenerate");
		identityOptions.add("--proxy=PROXY_URL");
		identityOptions.add("--proxyuser=PROXY_USER");
		identityOptions.add("--proxypassword=PROXY_PASSWORD");
		for (String smHelpCommand : new String[]{clienttasks.command+" -h identity",clienttasks.command+" --help identity"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: "+clienttasks.command+" identity [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, optionsRegex, identityOptions}));
		}
		
		// MODULE: list
		List <String> listOptions = new ArrayList<String>();
		listOptions.add("-h, --help");
		listOptions.add("--debug=DEBUG");
//		listOptions.add("-k, --insecure");
		listOptions.add("--installed");
		listOptions.add("--available");
		listOptions.add("--consumed");
		listOptions.add("--all");
		listOptions.add("--proxy=PROXY_URL");
		listOptions.add("--proxyuser=PROXY_USER");
		listOptions.add("--proxypassword=PROXY_PASSWORD");
		for (String smHelpCommand : new String[]{clienttasks.command+" -h list",clienttasks.command+" --help list"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: "+clienttasks.command+" list [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, optionsRegex, listOptions}));
		}
		
		// MODULE: refresh
		List <String> refreshOptions = new ArrayList<String>();
		refreshOptions.add("-h, --help");
		refreshOptions.add("--debug=DEBUG");
		refreshOptions.add("--proxy=PROXY_URL");
		refreshOptions.add("--proxyuser=PROXY_USER");
		refreshOptions.add("--proxypassword=PROXY_PASSWORD");
		for (String smHelpCommand : new String[]{clienttasks.command+" -h refresh",clienttasks.command+" --help refresh"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: "+clienttasks.command+" refresh [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, optionsRegex, refreshOptions}));
		}
		
		// MODULE: register
		List <String> registerOptions = new ArrayList<String>();
		registerOptions.add("-h, --help");
		registerOptions.add("--debug=DEBUG");
//		registerOptions.add("-k, --insecure");
		registerOptions.add("--username=USERNAME");
		registerOptions.add("--type=CONSUMERTYPE");
		registerOptions.add("--name=CONSUMERNAME");
		registerOptions.add("--password=PASSWORD");
		registerOptions.add("--consumerid=CONSUMERID");
		registerOptions.add("--autosubscribe");
		registerOptions.add("--force");
		registerOptions.add("--proxy=PROXY_URL");
		registerOptions.add("--proxyuser=PROXY_USER");
		registerOptions.add("--proxypassword=PROXY_PASSWORD");
		for (String smHelpCommand : new String[]{clienttasks.command+" -h register",clienttasks.command+" --help register"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: "+clienttasks.command+" register [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ new BlockedByBzBug("628589", smHelpCommand, optionsRegex, registerOptions)}));
		}
		
//		// MODULE: reregister
//		List <String> reregisterOptions = new ArrayList<String>();
//		reregisterOptions.add("-h, --help");
//		reregisterOptions.add("--debug=DEBUG");
////		reregisterOptions.add("-k, --insecure");
//		reregisterOptions.add("--username=USERNAME");
//		reregisterOptions.add("--password=PASSWORD");
//		reregisterOptions.add("--consumerid=CONSUMERID");
//		for (String smHelpCommand : new String[]{clienttasks.command+" -h reregister",clienttasks.command+" --help reregister"}) {
//			List <String> usages = new ArrayList<String>();
//			String usage = "Usage: "+clienttasks.command+" reregister [OPTIONS]";
//			usages.add(usage);
//			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
//			ll.add(Arrays.asList(new Object[]{ smHelpCommand, optionsRegex, reregisterOptions}));
//		}
		
		// MODULE: subscribe
		List <String> subscribeOptions = new ArrayList<String>();
		subscribeOptions.add("-h, --help");
		subscribeOptions.add("--debug=DEBUG");
		subscribeOptions.add("--pool=POOL");
//		subscribeOptions.add("-k, --insecure");
//		subscribeOptions.add("--regtoken=REGTOKEN");	// https://bugzilla.redhat.com/show_bug.cgi?id=670823
//		subscribeOptions.add("--email=EMAIL");			// https://bugzilla.redhat.com/show_bug.cgi?id=670823
//		subscribeOptions.add("--locale=LOCALE");		// https://bugzilla.redhat.com/show_bug.cgi?id=670823
		subscribeOptions.add("--proxy=PROXY_URL");
		subscribeOptions.add("--proxyuser=PROXY_USER");
		subscribeOptions.add("--proxypassword=PROXY_PASSWORD");
		for (String smHelpCommand : new String[]{clienttasks.command+" -h subscribe",clienttasks.command+" --help subscribe"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: "+clienttasks.command+" subscribe [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, optionsRegex, subscribeOptions}));
		}
		
		// MODULE: unregister
		List <String> unregisterOptions = new ArrayList<String>();
		unregisterOptions.add("-h, --help");
		unregisterOptions.add("--debug=DEBUG");
//		unregisterOptions.add("-k, --insecure");
		unregisterOptions.add("--proxy=PROXY_URL");
		unregisterOptions.add("--proxyuser=PROXY_USER");
		unregisterOptions.add("--proxypassword=PROXY_PASSWORD");
		for (String smHelpCommand : new String[]{clienttasks.command+" unregister -h",clienttasks.command+" --help unregister"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: "+clienttasks.command+" unregister [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, optionsRegex, unregisterOptions}));
		}
		
		// MODULE: unsubscribe
		List <String> unsubscribeOptions = new ArrayList<String>();
		unsubscribeOptions.add("-h, --help");
		unsubscribeOptions.add("--debug=DEBUG");
//		unsubscribeOptions.add("-k, --insecure");
		unsubscribeOptions.add("--serial=SERIAL");
		unsubscribeOptions.add("--all");
		unsubscribeOptions.add("--proxy=PROXY_URL");
		unsubscribeOptions.add("--proxyuser=PROXY_USER");
		unsubscribeOptions.add("--proxypassword=PROXY_PASSWORD");
		for (String smHelpCommand : new String[]{clienttasks.command+" -h unsubscribe",clienttasks.command+" --help unsubscribe"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: "+clienttasks.command+" unsubscribe [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, optionsRegex, unsubscribeOptions}));
		}
		
		return ll;
	}
}
