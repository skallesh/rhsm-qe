package rhsm.cli.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * Reference:
 * http://stackoverflow.com/questions/9137245/unit-test-for-bash-completion-script
 * https://github.com/lacostej/unity3d-bash-completion
 * https://github.com/lacostej/unity3d-bash-completion/blob/master/lib/completion.py
 * 
 * RHEL5
 * bash-completion-1.3-7.el5.noarch.rpm
 * rpm -Uvh http://dl.fedoraproject.org/pub/epel/5/x86_64/bash-completion-1.3-7.el5.noarch.rpm
 * rpm -Uvh ftp://fr2.rpmfind.net/linux/epel/5/ppc/bash-completion-1.3-7.el5.noarch.rpm
 * 
 * RHEL6
 * rpm -Uvh http://dl.fedoraproject.org/pub/epel/6/x86_64/bash-completion-1.3-7.el6.noarch.rpm
 * rpm -Uvh ftp://fr2.rpmfind.net/linux/epel/6/i386/bash-completion-1.3-7.el6.noarch.rpm
 * 
 */
@Test(groups={"BashCompletionTests"})
public class BashCompletionTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************
	
	@Test(	description="when subscription-manager is run with no args, it should default to the help report",
			groups={"AcceptanceTests"},
			dataProvider="BashCompletionData",
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void BashCompletion_Test(Object bugzilla, String bashCommand, Set<String> expectedCompletions) {
		
		// inspired by https://github.com/lacostej/unity3d-bash-completion/blob/master/lib/completion.py
		List<String> program_args =  Arrays.asList(bashCommand.split("\\s+"));
		String program = program_args.get(0);
		List<String> args = program_args.subList(1, program_args.size());
		String COMP_LINE = bashCommand;
		String COMP_WORDS = COMP_LINE.trim();
		Integer COMP_CWORD = args.size();
		Integer COMP_POINT = COMP_LINE.length();
		
		if (COMP_LINE.endsWith(" ")) {
			COMP_WORDS += " ";
			COMP_CWORD += 1;
		}
		
		//String script = "bash -i -c 'COMP_LINE=\"subscription-manager attach --\" COMP_WORDS=(subscription-manager attach --) COMP_CWORD=2 COMP_POINT=30; $(complete -p subscription-manager | sed \"s/.*-F \\([^ ]*\\) .*/\\1/\") && echo ${COMPREPLY[*]}'";
		String script = String.format("bash -i -c 'COMP_LINE=\"%s\" COMP_WORDS=(%s) COMP_CWORD=%d COMP_POINT=%d; $(complete -p %s | sed \"s/.*-F \\([^ ]*\\) .*/\\1/\") && echo ${COMPREPLY[*]}'",COMP_LINE,COMP_WORDS,COMP_CWORD,COMP_POINT,program);
		SSHCommandResult result = client.runCommandAndWait(script);
		// IGNORE: Stderr: 
		//	bash: cannot set terminal process group (-1): Invalid argument
		//	bash: no job control in this shell
		log.info(result.toString());
		
		Set<String> actualCompletions = new HashSet<String>(Arrays.asList(result.getStdout().trim().split("\\s+")));
		
		// assert all of the expectedOptions were found and that no unexpectedOptions were found
		for (String expectedCompletion : expectedCompletions) {
			if (!actualCompletions.contains(expectedCompletion)) {
				log.warning("Was not presented with the expected bash-completion '"+expectedCompletion+"' for command '"+bashCommand+"'.");
			} else {
				Assert.assertTrue(actualCompletions.contains(expectedCompletion),"The expected command '"+bashCommand+"' option '"+expectedCompletion+"' is available.");
			}
		}
		for (String actualCompletion : actualCompletions) {
			if (!expectedCompletions.contains(actualCompletion))
				log.warning("Was presented with an unexpected bash-completion '"+actualCompletion+"' for command '"+bashCommand+"'.");
		}
		
		// PERMANENT WORKAROUND FOR CLOSED/WONTFIX Bug 1004402 - rhsmd and rhsmcertd-worker does not bash complete its options
		if ((bashCommand.startsWith("/usr/libexec/rhsmcertd-worker ") || bashCommand.startsWith("/usr/libexec/rhsmd ")) &&
			!actualCompletions.containsAll(expectedCompletions)) {
			String bugId="1004402"; 
			try {if (!BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else { }} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			throw new SkipException("Bash completion for '"+bashCommand+"' is broken and bug '"+bugId+"' was CLOSED/WONTFIX.");
		}
		// END OF WORKAROUND
		
		Assert.assertTrue(actualCompletions.containsAll(expectedCompletions), "All of the expected bash-completions for command '"+bashCommand+"' were presented.");
		Assert.assertTrue(expectedCompletions.containsAll(actualCompletions), "All of the presented bash-completions for command '"+bashCommand+"' were expected.");
		
	}

	
	
	// Candidates for an automated Test:
	
	
	// Configuration Methods ***********************************************************************
	
	@BeforeClass(groups={"setup"})
	public void verifyBashCompletionIsInstalledBeforeClass() {
		if (clienttasks==null) return;
		
		if (!clienttasks.isPackageInstalled("bash-completion")) {
			if (clienttasks.redhatReleaseX.equals("5")) {
				client.runCommandAndWait("yum install --assumeyes --quiet http://dl.fedoraproject.org/pub/epel/5/x86_64/bash-completion-1.3-7.el5.noarch.rpm");
			}
			if (clienttasks.redhatReleaseX.equals("6")) {
				client.runCommandAndWait("yum install --assumeyes --quiet http://dl.fedoraproject.org/pub/epel/6/x86_64/bash-completion-1.3-7.el6.noarch.rpm");
			}
		}
		
		if (!clienttasks.isPackageInstalled("bash-completion")) {
			throw new SkipException("This test class requires package bash-completion to be installed.");	
		}
	}
	
	
	// Protected Methods ***********************************************************************

	
	// Data Providers ***********************************************************************
	
	
	@DataProvider(name="BashCompletionData")
	public Object[][] getBashCompletionDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getBashCompletionDataAsListOfLists());
	}
	protected List<List<Object>> getBashCompletionDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
		//ll.add(Arrays.asList(new Object[] {null,	"subscription-manager attach -", new HashSet<String>(Arrays.asList(new String[]{"--pool","--quantity","--servicelevel","--help","--proxy","--proxyuser","--proxypassword"})) }));
		//ll.add(Arrays.asList(new Object[] {null,	"subscription-manager --help", new HashSet<String>(Arrays.asList(new String[]{"--pool","--quantity","--servicelevel","--help","--proxy","--proxyuser","--proxypassword"})) }));
		// LET'S NOT BUILD THE ROWS OF THE DATA PROVIDER THIS^ WAY....
		// LET'S REUSE THE DATA PROVIDER FROM HelpTests.getExpectedCommandLineOptionsDataAsListOfLists() TO MINIMIZE MAINTENANCE.
		
		// interpret the expected bash completion data from the HelpTests dataProvider getExpectedCommandLineOptionsDataAsListOfLists
		for (List<Object> l: HelpTests.getExpectedCommandLineOptionsDataAsListOfLists()) { 
			//Object bugzilla, String helpCommand, Integer exitCode, String stdoutRegex, List<String> expectedOptions
			//BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
			String helpCommand = (String) l.get(1);
			//Integer exitCode = (Integer) l.get(2);
			//String stdoutRegex = (String) l.get(3);
			List<String> expectedHelpOptions = (List<String>) l.get(4);
			
			// skip all the help tests with expectedOptions that do not come from --help
			if (!helpCommand.contains("--help")) continue;
			
			// based on comment https://bugzilla.redhat.com/show_bug.cgi?id=1004385#c7 we will only test rhsm-icon --help (we will skip tests for --help-gtk options or --help-all options)
			if (helpCommand.startsWith("rhsm-icon") && (helpCommand.contains("--help-gtk")||helpCommand.contains("--help-all"))) continue;
			
			// remove "export DISPLAY=localhost:10.0 && " from the helpCommand export DISPLAY=localhost:10.0 && subscription-manager-gui --help
			//helpCommand = helpCommand.replace("export DISPLAY=localhost:10.0 && ","");
			helpCommand = helpCommand.replaceFirst("export DISPLAY=.* && ","");
			
			// transcribe the helpCommand into a bashCommand
			String bashCommand = helpCommand.replaceFirst("\\s*(--help-all|--help-gtk|--help)\\s*", " ").trim();	// strip out "--help"
			bashCommand += " "; // append chars as a prefix to <tab><tab> complete the expected command line options
			
			// transcribe the expectedHelpOptions into expectedCompletions
			Set<String>expectedCompletions = new HashSet<String>();
			for (String expectedHelpOption: expectedHelpOptions) {
				
				// skip all Usage help tests
				if (expectedHelpOption.equals("Attach a specified subscription to the registered system")) continue;	// from the attach module usage test
				if (expectedHelpOption.equals("Remove all or specific subscriptions from this system")) continue;		// from the remove module usage test
				if (expectedHelpOption.startsWith("Deprecated, see")) continue;			// from the subscribe and unsubscribe module usage test
				if (expectedHelpOption.toUpperCase().startsWith("USAGE:")) continue;	// Usage: subscription-manager MODULE-NAME [MODULE-OPTIONS] [--help]
				if (expectedHelpOption.startsWith(bashCommand)) continue;				// Usage:
																						//   rhsm-icon [OPTION...]
				
				// split expectedHelpOption of this form: -h, --help
				// split expectedHelpOption of this form: -s SERVICELEVEL, --servicelevel=SERVICELEVEL
				for (String expectedOption: expectedHelpOption.split("\\s*,\\s*")) {
					// split expectedOption of this form: -s SERVICELEVEL
					// split expectedOption of this form: --servicelevel=SERVICELEVEL
					expectedOption = expectedOption.split("\\s*[= ]\\s*")[0];
					
					expectedCompletions.add(expectedOption);
				}
			}
			// skip this data provided row when there are no expectedCompletions left
			if (expectedCompletions.isEmpty()) continue;
			
			// mark dataProvider rows with a blockedByBzBug where appropriate
			Set<String> bugIds = new HashSet<String>();
			
			// Bug 812104 - Unable to tab complete new subscription-manager modules (release and service-level)
			if (bashCommand.startsWith("subscription-manager release ")) bugIds.add("812104");
			if (bashCommand.startsWith("subscription-manager service-level ")) bugIds.add("812104");
			
			// Bug 817390 - bash-completion of subscription-manager subscribe --<TAB><TAB> is not finding --servicelevel option
			if (bashCommand.startsWith("subscription-manager subscribe ")) bugIds.add("817390");
			if (bashCommand.startsWith("subscription-manager register ")) bugIds.add("817390");
			
			// Bug 817117 - bash-completion of subscription-manager environments --<TAB><TAB> is not working
			if (bashCommand.startsWith("subscription-manager environments ")) bugIds.add("817117");

			// Bug 1011712 - bash-completion of subscription-manager environments --<TAB><TAB> is incomplete
			if (bashCommand.startsWith("subscription-manager environments ")) bugIds.add("1011712");
			
			// Bug 1003010 - --status should be removed from bash-completion of subscription-manager list --<TAB><TAB>
			if (bashCommand.startsWith("subscription-manager list ")) bugIds.add("1003010");
			
			// Bug 1001820 - Tab Completion: subscription-manager attach <tab tab>
			if (bashCommand.startsWith("subscription-manager attach ")) bugIds.add("1001820");
			if (bashCommand.startsWith("subscription-manager auto-attach ")) bugIds.add("1001820");
			if (bashCommand.startsWith("subscription-manager clean ")) bugIds.add("1001820");
			if (bashCommand.startsWith("subscription-manager config ")) bugIds.add("1001820");
			if (bashCommand.startsWith("subscription-manager environments ")) bugIds.add("1001820");
			if (bashCommand.startsWith("subscription-manager import ")) bugIds.add("1001820");
			if (bashCommand.startsWith("subscription-manager list ")) bugIds.add("1001820");
			if (bashCommand.startsWith("subscription-manager orgs ")) bugIds.add("1001820");
			if (bashCommand.startsWith("subscription-manager plugins ")) bugIds.add("1001820");
			if (bashCommand.startsWith("subscription-manager register ")) bugIds.add("1001820");
			if (bashCommand.startsWith("subscription-manager remove ")) bugIds.add("1001820");
			if (bashCommand.startsWith("subscription-manager service-level ")) bugIds.add("1001820");
			if (bashCommand.startsWith("subscription-manager status ")) bugIds.add("1001820");
			if (bashCommand.startsWith("subscription-manager subscribe ")) bugIds.add("1001820");
			if (bashCommand.startsWith("subscription-manager unsubscribe ")) bugIds.add("1001820");

			// Bug 1004341 - subscription-manager-gui does not bash complete its options
			if (bashCommand.startsWith("subscription-manager-gui ")) bugIds.add("1004341");
			
			// Bug 1004318 - rct [cat-cert cat-manifest dump-manifest stat-cert] does not bash complete its options
			if (bashCommand.startsWith("rct ")) bugIds.add("1004318");
			
			// Bug 1004385 - rhsm-icon bash completions should not end with a comma
			if (bashCommand.startsWith("rhsm-icon ")) bugIds.add("1004385");
			
			// Bug 985090 - command "rhsmcertd" options
			if (bashCommand.startsWith("rhsmcertd ")) bugIds.add("985090");
			
			// Bug 1004402 - rhsmd and rhsmcertd-worker does not bash complete its options
			if (bashCommand.startsWith("/usr/libexec/rhsmcertd-worker ")) bugIds.add("1004402");
			if (bashCommand.startsWith("/usr/libexec/rhsmd ")) bugIds.add("1004402");
			
			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			
			// append a new row to the dataProvider
			ll.add(Arrays.asList(new Object[] {blockedByBzBug,	bashCommand, expectedCompletions}));
		}
		
		return ll;
	}
	
}
