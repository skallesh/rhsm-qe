package com.redhat.qe.sm.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testng.BlockedByBzBug;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author ssalevan
 * @author jsefler
 *
 */
@Test(groups={"general"})
public class GeneralTests extends SubscriptionManagerTestScript{
	
	
	@Test(	description="subscription-manager-cli: assert only expected command line options are available",
			groups={},
			dataProvider="ExpectedCommandLineOptionsData")
	@ImplementsTCMS(id="41697")
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
	
	
	// FIXME This testcase is being replaced by ExpectedCommandLineOptions_Test  jsefler - 8/24/2010
	@Test(	description="subscription-manager-cli: ensure manpages and usage information are accurate",
//			groups={"sm_stage1"},
			dataProvider="CommandLineOptionsSData",
			enabled=false)
	@ImplementsTCMS(id="41697")
	public void CommandLineOptions_Test(String command, int expectedExitCode, String stderrGrepExpression, String stdoutGrepExpression) {
		log.info("Testing subscription-manager-cli command line options '"+command+"' and verifying the output.");
		RemoteFileTasks.runCommandAndAssert(client,command,expectedExitCode,stdoutGrepExpression,stderrGrepExpression);
	}
	
	
	@Test(	description="subscription-manager-cli: attempt to access functionality without registering",
//			groups={"sm_stage1"},
			dataProvider="UnregisteredCommandData")
	@ImplementsTCMS(id="41697")
	public void AttemptingCommandsWithoutBeingRegistered_Test(String command) {
		log.info("Testing subscription-manager-cli command without being registered, expecting it to fail: "+ command);
		clienttasks.unregister();
		//RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner, command);
		RemoteFileTasks.runCommandAndAssert(client,command,1,"^Error: You need to register this system by running `register` command before using this option.",null);

	}
	
	
	@Test(	description="subscription-manager-cli: attempt to access functionality that does not exist",
//			groups={"sm_stage1"},
			dataProvider="NegativeFunctionalityData")
	public void AttemptingCommandsThatDoNotExist_Test(String command, int expectedExitCode, String stderrGrepExpression, String stdoutGrepExpression) {
		log.info("Testing subscription-manager-cli command that does not exist, expecting it to fail: "+ command);
		RemoteFileTasks.runCommandAndAssert(client,command,expectedExitCode,stdoutGrepExpression,stderrGrepExpression);

	}
	
	
	// Data Providers ***********************************************************************

	
	@DataProvider(name="ExpectedCommandLineOptionsData")
	public Object[][] getExpectedCommandLineOptionsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getExpectedCommandLineOptionsDataAsListOfLists());
	}
	protected List<List<Object>> getExpectedCommandLineOptionsDataAsListOfLists() {
		// String command, String stdoutRegex, List<String> expectedOptions
		List<List<Object>> ll = new ArrayList<List<Object>>();
		String modulesRegex = "^	\\w+";
		String optionsRegex = "^  --\\w+[(?:=\\w)]*|^  -\\w[(?:=\\w)]*\\, --\\w+[(?:=\\w)]*";
		
		// MODULES
		List <String> modules = new ArrayList<String>();
		modules.add("facts");
		modules.add("list");
		modules.add("register");
		modules.add("reregister");
		modules.add("subscribe");
		modules.add("unregister");
		modules.add("unsubscribe");
		for (String smHelpCommand : new String[]{"subscription-manager-cli -h","subscription-manager-cli --help"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: subscription-manager-cli [options] MODULENAME --help";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, modulesRegex, modules}));
		}
		
		// MODULE: facts
		List <String> factsOptions = new ArrayList<String>();
		factsOptions.add("-h, --help");
		factsOptions.add("--debug=DEBUG");
//		factsOptions.add("-k, --insecure");
		factsOptions.add("--list");
		factsOptions.add("--update");
		for (String smHelpCommand : new String[]{"subscription-manager-cli -h facts","subscription-manager-cli --help facts"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: subscription-manager-cli facts [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, optionsRegex, factsOptions}));
		}
		
		// MODULE: list
		List <String> listOptions = new ArrayList<String>();
		listOptions.add("-h, --help");
		listOptions.add("--debug=DEBUG");
//		listOptions.add("-k, --insecure");
		listOptions.add("--available");
		listOptions.add("--consumed");
		listOptions.add("--all");
		for (String smHelpCommand : new String[]{"subscription-manager-cli -h list","subscription-manager-cli --help list"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: subscription-manager-cli list [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, optionsRegex, listOptions}));
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
		registerOptions.add("--autosubscribe");
		registerOptions.add("--force");
		for (String smHelpCommand : new String[]{"subscription-manager-cli -h register","subscription-manager-cli --help register"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: subscription-manager-cli register [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ new BlockedByBzBug("628589", smHelpCommand, optionsRegex, registerOptions)}));
		}
		
		// MODULE: reregister
		List <String> reregisterOptions = new ArrayList<String>();
		reregisterOptions.add("-h, --help");
		reregisterOptions.add("--debug=DEBUG");
//		reregisterOptions.add("-k, --insecure");
		reregisterOptions.add("--username=USERNAME");
		reregisterOptions.add("--password=PASSWORD");
		reregisterOptions.add("--consumerid=CONSUMERID");
		for (String smHelpCommand : new String[]{"subscription-manager-cli -h reregister","subscription-manager-cli --help reregister"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: subscription-manager-cli reregister [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, optionsRegex, reregisterOptions}));
		}
		
		// MODULE: subscribe
		List <String> subscribeOptions = new ArrayList<String>();
		subscribeOptions.add("-h, --help");
		subscribeOptions.add("--debug=DEBUG");
//		subscribeOptions.add("-k, --insecure");
		subscribeOptions.add("--regtoken=REGTOKEN");
		subscribeOptions.add("--pool=POOL");
		subscribeOptions.add("--email=EMAIL");
		subscribeOptions.add("--locale=LOCALE");
		for (String smHelpCommand : new String[]{"subscription-manager-cli -h subscribe","subscription-manager-cli --help subscribe"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: subscription-manager-cli subscribe [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, optionsRegex, subscribeOptions}));
		}
		
		// MODULE: unregister
		List <String> unregisterOptions = new ArrayList<String>();
		unregisterOptions.add("-h, --help");
		unregisterOptions.add("--debug=DEBUG");
//		unregisterOptions.add("-k, --insecure");
		for (String smHelpCommand : new String[]{"subscription-manager-cli unregister -h","subscription-manager-cli --help unregister"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: subscription-manager-cli unregister [OPTIONS]";
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
		for (String smHelpCommand : new String[]{"subscription-manager-cli -h unsubscribe","subscription-manager-cli --help unsubscribe"}) {
			List <String> usages = new ArrayList<String>();
			String usage = "Usage: subscription-manager-cli unsubscribe [OPTIONS]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")+"$", usages}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, optionsRegex, unsubscribeOptions}));
		}
		
		return ll;
	}
	
	@DataProvider(name="CommandLineOptionsSData")
	public Object[][] getCommandLineOptionsSDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getCommandLineOptionsSDataAsListOfLists());
	}
	protected List<List<Object>> getCommandLineOptionsSDataAsListOfLists() {
		// String command, int expectedExitCode, String stderrGrepExpression, String stdoutGrepExpression
		List<List<Object>> ll = new ArrayList<List<Object>>();
				
		/* [root@jsefler-rhel6-clientpin tmp]# subscription-manager-cli --help

Usage: subscription-manager-cli [options] MODULENAME --help

Supported modules:

	facts          show information for facts
	list           list available or consumer subscriptions for registered user
	register       register the client to a Unified Entitlement Platform.
	subscribe      subscribe the registered user to a specified product or regtoken.
	unregister     unregister the client from a Unified Entitlement Platform.
	unsubscribe    unsubscribe the registered user from all or specific subscriptions.

		*/
		for (String smHelpCommand : new String[]{"subscription-manager-cli -h","subscription-manager-cli --help"}) {
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Usage: subscription-manager-cli \\[(OPTIONS|options)\\] MODULENAME --help$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Supported modules:$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^\tfacts"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^\tlist"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^\tregister"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^\tsubscribe"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^\tunsubscribe"}));
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^$"}));
		}
		
		/* [root@jsefler-rhel6-clientpin tmp]# subscription-manager-cli facts -h
Usage: subscription-manager-cli facts [options]

facts

Options:
  -h, --help      show this help message and exit
  --debug=DEBUG   debug level
  -k, --insecure  communicate with candlepin server without verifying server's
                  certificate
  --list          list known facts for this system
  --update        update the system facts
		*/
		for (String smHelpCommand : new String[]{"subscription-manager-cli facts -h","subscription-manager-cli facts --help"}) {
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Usage: subscription-manager-cli facts \\[(OPTIONS|options)\\]$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^facts$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Options:$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -h, --help"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --debug=DEBUG"}));
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -k, --insecure"})); // option was removed
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --list"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --update"}));
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^$"}));
		}
		
		/* [root@jsefler-rhel6-clientpin tmp]# subscription-manager-cli list -h
Usage: subscription-manager-cli list [OPTIONS]

list available or consumed Entitlement Pools for this system.

Options:
  -h, --help      show this help message and exit
  --debug=DEBUG   debug level
  -k, --insecure  communicate with candlepin server without verifying server's
                  certificate
  --available     available
  --consumed      consumed
		*/
		for (String smHelpCommand : new String[]{"subscription-manager-cli list -h","subscription-manager-cli list --help"}) {
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Usage: subscription-manager-cli list \\[(OPTIONS|options)\\]$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^list available or consumed Entitlement Pools for this system.$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Options:$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -h, --help"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --debug=DEBUG"}));
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -k, --insecure"})); // option was removed
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --available"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --consumed"}));
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^$"}));
		}
		
		/* [root@jsefler-rhel6-clientpin tmp]# subscription-manager-cli register -h
Usage: subscription-manager-cli register [OPTIONS]

register

Options:
  -h, --help            show this help message and exit
  --debug=DEBUG         debug level
  -k, --insecure        communicate with candlepin server without verifying
                        server's certificate
  --username=USERNAME   Specify a username
  --type=CONSUMERTYPE   The type of consumer to create. Defaults to sytem
  --password=PASSWORD   Specify a password
  --consumerid=CONSUMERID
                        Register to an Existing consumer
  --autosubscribe       Automatically subscribe this system to
                        compatible subscriptions.
  --force               Register the system even if it is already registered
		*/
		for (String smHelpCommand : new String[]{"subscription-manager-cli register -h","subscription-manager-cli register --help"}) {
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Usage: subscription-manager-cli register \\[(OPTIONS|options)\\]$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^register$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Options:$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -h, --help"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --debug=DEBUG"}));
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -k, --insecure"})); // option was removed
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --username=USERNAME"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --type=CONSUMERTYPE"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --password=PASSWORD"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --consumerid=CONSUMERID"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --autosubscribe"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --force"}));
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^$"}));
		}
		
		/* [root@jsefler-rhel6-clientpin tmp]# subscription-manager-cli subscribe -h
Usage: subscription-manager-cli subscribe [OPTIONS]

subscribe

Options:
  -h, --help           show this help message and exit
  --debug=DEBUG        debug level
  -k, --insecure       communicate with candlepin server without verifying
                       server's certificate
  --regtoken=REGTOKEN  regtoken
  --pool=POOL          Subscription Pool Id
  --email=EMAIL        Optional email address to notify when token actication
                       is complete. Used with --regtoken only
  --locale=LOCALE      Optional language to use for email notification when
                       token actication is complete. Used with --regtoken and
                       --email only. Examples: en-us, de-de
		*/
		for (String smHelpCommand : new String[]{"subscription-manager-cli subscribe -h","subscription-manager-cli subscribe --help"}) {
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Usage: subscription-manager-cli subscribe \\[(OPTIONS|options)\\]$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^subscribe$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Options:$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -h, --help"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --debug=DEBUG"}));
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -k, --insecure"})); // option was removed
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --product=PRODUCT"})); // option was removed
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --regtoken=REGTOKEN"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --pool=POOL"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --email=EMAIL"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --locale=LOCALE"}));
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^$"}));
		}
		
		/* [root@jsefler-rhel6-clientpin tmp]# subscription-manager-cli unregister -h
Usage: subscription-manager-cli unregister [OPTIONS]

unregister

Options:
  -h, --help      show this help message and exit
  --debug=DEBUG   debug level
  -k, --insecure  communicate with candlepin server without verifying server's
                  certificate
		*/
		for (String smHelpCommand : new String[]{"subscription-manager-cli unregister -h","subscription-manager-cli unregister --help"}) {
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Usage: subscription-manager-cli unregister \\[(OPTIONS|options)\\]$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^unregister$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Options:$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -h, --help"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --debug=DEBUG"}));
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -k, --insecure"})); // option was removed
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^$"}));
		}
		
		/* [root@jsefler-rhel6-clientpin tmp]# subscription-manager-cli unsubscribe -h
Usage: subscription-manager-cli unsubscribe [OPTIONS]

unsubscribe

Options:
  -h, --help       show this help message and exit
  --debug=DEBUG    debug level
  -k, --insecure   communicate with candlepin server without verifying
                   server's certificate
  --serial=SERIAL  Certificate serial to unsubscribe
		*/
		for (String smHelpCommand : new String[]{"subscription-manager-cli unsubscribe -h","subscription-manager-cli unsubscribe --help"}) {
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Usage: subscription-manager-cli unsubscribe \\[(OPTIONS|options)\\]$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^unsubscribe$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Options:$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -h, --help"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --debug=DEBUG"}));
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -k, --insecure"})); // option was removed
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --serial=SERIAL"}));
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^$"}));
		}
		
		return ll;
	}
	
	@DataProvider(name="UnregisteredCommandData")
	public Object[][] getUnregisteredCommandDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getUnregisteredCommandDataAsListOfLists());
	}
	public List<List<Object>> getUnregisteredCommandDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli facts --update"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli list --available"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli list --consumed"}));
// functionality appears to have been removed: subscription-manager-0.71-1.el6.i686  - jsefler 7/21/2010
//		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli subscribe --product=FOO"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli subscribe --regtoken=FOO"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli subscribe --pool=FOO"}));
// functionality appears to have been removed: subscription-manager-0.68-1.el6.i686  - jsefler 7/12/2010
//		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli unsubscribe --product=FOO"}));
//		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli unsubscribe --regtoken=FOO"}));
//		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli unsubscribe --pool=FOO"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli unsubscribe"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli unsubscribe --serial=FOO"}));

		return ll;
	}
	
	@DataProvider(name="NegativeFunctionalityData")
	public Object[][] getNegativeFunctionalityDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getNegativeFunctionalityDataAsListOfLists());
	}
	protected List<List<Object>> getNegativeFunctionalityDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		// due to design changes, this is a decent place to dump old commands that have been removed
		
		// String command, int expectedExitCode, String stderrGrepExpression, String stdoutGrepExpression
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli unsubscribe --product=FOO", 2, "subscription-manager-cli: error: no such option: --product",null}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli unsubscribe --regtoken=FOO", 2, "subscription-manager-cli: error: no such option: --regtoken",null}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli unsubscribe --pool=FOO", 2, "subscription-manager-cli: error: no such option: --pool",null}));
		
		return ll;
	}
}
