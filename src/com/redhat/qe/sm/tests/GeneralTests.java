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
	
	@Test(	description="subscription-manager-cli: ensure manpages and usage information are accurate",
			dataProvider="CommandLineOptionsSData",
			groups={"sm_stage1"})
	@ImplementsTCMS(id="41697")
	public void CommandLineOptions_Test(String command, int expectedExitCode, String stderrGrepExpression, String stdoutGrepExpression) {
		log.info("Testing subscription-manager-cli command line options '"+command+"' and verifying the output.");
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,command,expectedExitCode,stdoutGrepExpression,stderrGrepExpression);
	}
	
	
	@Test(	description="subscription-manager-cli: attempt to access functionality without registering",
			dataProvider="UnregisteredCommandData",
			groups={"sm_stage1"})
	@ImplementsTCMS(id="41697")
	public void AttemptingCommandsWithoutBeingRegistered_Test(String command) {
		log.info("Testing subscription-manager-cli command without being registered, expecting it to fail: "+ command);
		sm.unregisterFromCandlepin();
		//RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner, command);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,command,1,"^Error: You need to register this system by running `register` command before using this option.",null);

	}
	
	
	@Test(	description="subscription-manager-cli: attempt to access functionality that does not exist",
			dataProvider="NegativeFunctionalityData",
			groups={"sm_stage1"})
	public void AttemptingCommandsThatDoNotExist_Test(String command, int expectedExitCode, String stderrGrepExpression, String stdoutGrepExpression) {
		log.info("Testing subscription-manager-cli command that does not exist, expecting it to fail: "+ command);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,command,expectedExitCode,stdoutGrepExpression,stderrGrepExpression);

	}
	
	
	// Data Providers ***********************************************************************

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
		  -h, --help     show this help message and exit
		  --debug=DEBUG  debug level
		  --list         list known facts for this system
		  --update       update the system facts
		*/
		for (String smHelpCommand : new String[]{"subscription-manager-cli facts -h","subscription-manager-cli facts --help"}) {
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Usage: subscription-manager-cli facts \\[(OPTIONS|options)\\]$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^facts$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Options:$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -h, --help"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --debug=DEBUG"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --list"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --update"}));
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^$"}));
		}
		
		/* [root@jsefler-rhel6-clientpin tmp]# subscription-manager-cli list -h
		Usage: subscription-manager-cli list [OPTIONS]

		list available or consumed Entitlement Pools for this system.

		Options:
		  -h, --help     show this help message and exit
		  --debug=DEBUG  debug level
		  --available    available
		  --consumed     consumed
		*/
		for (String smHelpCommand : new String[]{"subscription-manager-cli list -h","subscription-manager-cli list --help"}) {
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Usage: subscription-manager-cli list \\[(OPTIONS|options)\\]$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^list available or consumed Entitlement Pools for this system.$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Options:$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -h, --help"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --debug=DEBUG"}));
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
		  --product=PRODUCT    product ID
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
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --product=PRODUCT"}));
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
		  -h, --help     show this help message and exit
		  --debug=DEBUG  debug level
		*/
		for (String smHelpCommand : new String[]{"subscription-manager-cli unregister -h","subscription-manager-cli unregister --help"}) {
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Usage: subscription-manager-cli unregister \\[(OPTIONS|options)\\]$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^unregister$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Options:$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -h, --help"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --debug=DEBUG"}));
			//ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^$"}));
		}
		
		/* [root@jsefler-rhel6-clientpin tmp]# subscription-manager-cli unsubscribe -h
		Usage: subscription-manager-cli unsubscribe [OPTIONS]

		unsubscribe

		Options:
		  -h, --help       show this help message and exit
		  --debug=DEBUG    debug level
		  --serial=SERIAL  Certificate serial to unsubscribe
		*/
		for (String smHelpCommand : new String[]{"subscription-manager-cli unsubscribe -h","subscription-manager-cli unsubscribe --help"}) {
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Usage: subscription-manager-cli unsubscribe \\[(OPTIONS|options)\\]$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^unsubscribe$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^Options:$"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  -h, --help"}));
			ll.add(Arrays.asList(new Object[]{ smHelpCommand, 0, null, "^  --debug=DEBUG"}));
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
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli subscribe --product=FOO"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli subscribe --regtoken=FOO"}));
		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli subscribe --pool=FOO"}));
// functionality appears to have been removed: subscription-manager-0.68-1.el6.i686  - jsefler 7/12/2010
//		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli unsubscribe --product=FOO"}));
//		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli unsubscribe --regtoken=FOO"}));
//		ll.add(Arrays.asList(new Object[]{"subscription-manager-cli unsubscribe --pool=FOO"}));
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
