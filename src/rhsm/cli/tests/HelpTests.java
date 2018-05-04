package rhsm.cli.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;
import rhsm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 */
@Test(groups={"HelpTests"})
public class HelpTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************
	
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21764", "RHEL7-51271"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="when subscription-manager is run with no args, it should default to the --help option report",
			groups={"Tier2Tests","blockedByBug-974123"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testStatusIsTheDefault() {
		clienttasks.unregister(null,null,null, null);
		SSHCommandResult helpResult = RemoteFileTasks.runCommandAndAssert(client,clienttasks.command+" --help",Integer.valueOf(0));
		SSHCommandResult defaultResult = RemoteFileTasks.runCommandAndAssert(client,clienttasks.command,Integer.valueOf(0));
		Assert.assertTrue(defaultResult.toString().equals(helpResult.toString()), "When not registered, the default output running subscription-manager with no arguments should be identical to output from running subscription-manager with --help.");

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		defaultResult = RemoteFileTasks.runCommandAndAssert(client,clienttasks.command,Integer.valueOf(0));
		Assert.assertTrue(defaultResult.toString().equals(helpResult.toString()), "When registered, the default output running subscription-manager with no arguments should be identical to output from running subscription-manager with --help.");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21758", "RHEL7-51265"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: assert the existence of a man page for this tool",
			groups={"Tier2Tests"},
			enabled=true)
	@ImplementsNitrateTest(caseId=41697)
	public void testManPageExistanceForSubscriptionManager() {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String cliCommand = clienttasks.command;
		RemoteFileTasks.runCommandAndAssert(client,"man -P cat "+cliCommand,0);
		RemoteFileTasks.runCommandAndAssert(client,"whatis "+cliCommand,0,"^"+cliCommand+" ",null);	// run "mandb" if the result is Stderr: subscription-manager: nothing appropriate.
		log.warning("In this test we only verified the existence of the man page; NOT the contents!");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21759", "RHEL7-51266"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-gui: assert the existence of a man page for this tool",
			groups={"Tier2Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testManPageExistanceForSubscriptionManagerGui() {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String guiCommand = clienttasks.command+"-gui";
		// is the guiCommand installed?
		if (client.runCommandAndWait("rpm -q "+guiCommand).getStdout().contains("is not installed")) {
			RemoteFileTasks.runCommandAndAssert(client,"man -P cat "+guiCommand,1,null,"^No manual entry for "+guiCommand);
			RemoteFileTasks.runCommandAndAssert(client,"whatis "+guiCommand,0,"^"+guiCommand+": nothing appropriate",null);
			log.warning("In this test we only verified the existence of the man page; NOT the contents!");
			throw new SkipException(guiCommand+" is not installed and therefore its man page is also not installed.");
		} else {
			RemoteFileTasks.runCommandAndAssert(client,"man -P cat "+guiCommand,0);
			RemoteFileTasks.runCommandAndAssert(client,"whatis "+guiCommand,0,"^"+guiCommand+" ",null);	// run "mandb" if the result is Stderr: subscription-manager-gui: nothing appropriate.
			log.warning("In this test we only verified the existence of the man page; NOT the contents!");
		}
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21762", "RHEL7-51269"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="rhsm-icon: assert the existence of a man page for this tool",
			groups={"Tier2Tests","blockedByBug-771726"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testManPageExistanceForRhsmIcon() {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String command = "rhsm-icon"; //iconCommand = "rhsm-compliance-icon"; // prior to bug 771726
		// is the command installed?
		if (client.runCommandAndWait("rpm -q "+clienttasks.command+"-gui").getStdout().contains("is not installed")) {
			RemoteFileTasks.runCommandAndAssert(client,"man -P cat "+command,1,null,"^No manual entry for "+command);
			RemoteFileTasks.runCommandAndAssert(client,"whatis "+command,0,"^"+command+": nothing appropriate",null);
			log.warning("In this test we only verified the existence of the man page; NOT the contents!");
			throw new SkipException(command+" is not installed and therefore its man page is also not installed.");
		} else {
			RemoteFileTasks.runCommandAndAssert(client,"man -P cat "+command,0);
			RemoteFileTasks.runCommandAndAssert(client,"whatis "+command,0,"^"+command+" ",null);	// run "mandb" if the result is Stderr: rhsm-icon: nothing appropriate.
			log.warning("In this test we only verified the existence of the man page; NOT the contents!");
		}
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6},
			testCaseID= {"RHEL6-25813"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="install-num-migrate-to-rhsm: assert the existence of a man page for this tool",
			groups={"Tier2Tests",},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testManPageExistanceForInstallNumMigrateToRhsm() {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String command = MigrationTests.installNumTool;
		SSHCommandResult result;
		// is the command installed?
		if (client.runCommandAndWait("rpm -q "+clienttasks.command+"-migration").getStdout().contains("is not installed")) {
			RemoteFileTasks.runCommandAndAssert(client,"man -P cat "+command,1,null,"^No manual entry for "+command);
			RemoteFileTasks.runCommandAndAssert(client,"whatis "+command,0,"^"+command+": nothing appropriate",null);
			log.warning("In this test we only verified the existence of the man page; NOT the contents!");
			throw new SkipException(command+" is not installed and therefore its man page cannot be installed.");
			
		} else if (!clienttasks.redhatReleaseX.equals("5")) {
			log.info("The man page for '"+command+"' should only be installed on RHEL5.");
			//RemoteFileTasks.runCommandAndAssert(client,"man -P cat "+command,1,null,"^No manual entry for "+command);		// exit codes changed on RHEL7
			result = client.runCommandAndWait("man -P cat "+command);
			Assert.assertEquals(result.getStdout()+result.getStderr().trim(),"No manual entry for "+command);
			//RemoteFileTasks.runCommandAndAssert(client,"whatis "+command,0,"^"+command+": nothing appropriate",null);		// exit codes changed on RHEL7
			result = client.runCommandAndWait("whatis "+command);
			Assert.assertEquals(result.getStdout().trim()+result.getStderr().trim(),command+": nothing appropriate"+(Integer.valueOf(clienttasks.redhatReleaseX)>=7?".":""));	// the expected message is appended with a period on RHEL7+
			throw new SkipException("The migration tool '"+command+"' and its man page is only applicable on RHEL5.");
			
		} else if (clienttasks.isPackageVersion("subscription-manager-migration", ">", "1.11.3-4")) {	// install-num-migrate-to-rhsm was removed by bug 1092754
			log.warning("The '"+command+"' was removed as a result of bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=1092754");
			result = client.runCommandAndWait("man -P cat "+command);
			Assert.assertEquals(result.getStdout()+result.getStderr().trim(),"No manual entry for "+command);
			result = client.runCommandAndWait("whatis "+command);
			Assert.assertEquals(result.getStdout().trim()+result.getStderr().trim(),command+": nothing appropriate"+(Integer.valueOf(clienttasks.redhatReleaseX)>=7?".":""));	// the expected message is appended with a period on RHEL7+
			throw new SkipException("Due to bug 1092754, the migration tool '"+command+"' and its man page have been removed from RHEL5.");
			
		} else {
			RemoteFileTasks.runCommandAndAssert(client,"man -P cat "+command,0);
			RemoteFileTasks.runCommandAndAssert(client,"whatis "+command,0,"^"+command+" ",null);	// run "mandb" if the result is Stderr: install-num-migrate-to-rhsm: nothing appropriate.
			log.warning("In this test we only verified the existence of the man page; NOT the contents!");
		}
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21763", "RHEL7-51270"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="rhn-migrate-classic-to-rhsm: assert the existence of a man page for this tool",
			groups={"Tier2Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testManPageExistanceForRhnMigrateClassicToRhsm() {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String command = MigrationTests.rhnMigrateTool;
		// is the command installed?
		if (client.runCommandAndWait("rpm -q "+clienttasks.command+"-migration").getStdout().contains("is not installed")) {
			RemoteFileTasks.runCommandAndAssert(client,"man -P cat "+command,1,null,"^No manual entry for "+command);
			RemoteFileTasks.runCommandAndAssert(client,"whatis "+command,0,"^"+command+": nothing appropriate",null);
			log.warning("In this test we only verified the existence of the man page; NOT the contents!");
			throw new SkipException(command+" is not installed and therefore its man page is also not installed.");
		} else {
			RemoteFileTasks.runCommandAndAssert(client,"man -P cat "+command,0);
			RemoteFileTasks.runCommandAndAssert(client,"whatis "+command,0,"^"+command+" ",null);	// run "mandb" if the result is Stderr: rhn-migrate-classic-to-rhsm: nothing appropriate.
			log.warning("In this test we only verified the existence of the man page; NOT the contents!");
		}
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21760", "RHEL7-51267"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="rct: assert the existence of a man page for this tool",
			groups={"Tier2Tests","blockedByBug-862909"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testManPageExistanceForRCT() {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String rctCommand = "rct";
		RemoteFileTasks.runCommandAndAssert(client,"man -P cat "+rctCommand,0);
		RemoteFileTasks.runCommandAndAssert(client,"whatis "+rctCommand,0,"^"+rctCommand+" ",null);	// run "mandb" if the result is Stderr: rct: nothing appropriate.
		log.warning("In this test we only verified the existence of the man page; NOT the contents!");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21761", "RHEL7-51268"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="rhsm.conf: assert the existence of a man page for this config file",
			groups={"Tier2Tests","blockedByBug-990183"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testManPageExistanceForRhsmConf() {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		if (clienttasks.isPackageVersion("subscription-manager-migration", "<", "1.13.6-1")) throw new SkipException("RFE 990183 for an rhsm.conf man page was not implemented until subscription-manager version 1.13.6-1.");
		String cliCommand = "rhsm.conf";
		RemoteFileTasks.runCommandAndAssert(client,"man -P cat "+cliCommand,0);
		RemoteFileTasks.runCommandAndAssert(client,"whatis "+cliCommand,0,"^"+cliCommand+" ",null);	// run "mandb" if the result is Stderr: subscription-manager: nothing appropriate.
		log.warning("In this test we only verified the existence of the man page; NOT the contents!");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-22220", "RHEL7-51264"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-gui --help with no X-Display",
			groups={"Tier2Tests","blockedByBug-1290885","blockedByBug-976689","blockedByBug-881095"/* ALSO INCLUDED IN ExpectedCommandLineOptionsData */},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testCommandLineHelpForSubscriptionManagerGuiWithoutDisplay() {
		
		SSHCommandResult result = client.runCommandAndWait("subscription-manager-gui --help");
		
		// tolerate historically acceptable behaviors

		//	[jsefler@jseflerT5400 ~]$ ssh root@jsefler-6.usersys.redhat.com subscription-manager-gui --help
		//	Unable to open a display
		//	[jsefler@jseflerT5400 ~]$ echo $?
		//	1
		if (Integer.valueOf(1).equals(result.getExitCode())) {
			Assert.assertEquals(result.getStdout().trim(), "Unable to open a display","Stdout from calling subscription-manager-gui --help with no X-Display");
			Assert.assertEquals(result.getStderr().trim(), "","Stderr from calling subscription-manager-gui --help with no X-Display");
		}
		
		//	[jsefler@jseflerT540p ~]$ ssh root@jsefler-6.usersys.redhat.com subscription-manager-gui --help
		//	Unable to start.  Error: could not open display
		//	[jsefler@jseflerT540p ~]$ echo $?
		//	2
		else if (Integer.valueOf(2).equals(result.getExitCode())) {	// post subscription-manager-1.16.8-3	1303092: GUI issues in Repos and Help
			Assert.assertEquals(result.getStderr().trim(), "Unable to start.  Error: could not open display","Stderr from calling subscription-manager-gui --help with no X-Display");
			Assert.assertEquals(result.getStdout().trim(), "","Stdout from calling subscription-manager-gui --help with no X-Display");
		}

		//	[jsefler@jseflerT5400 ~]$ ssh root@jsefler-7.usersys.redhat.com subscription-manager-gui --help
		//	Usage: subscription-manager-gui [OPTIONS]
		//
		//	Options:
		//	  -h, --help  show this help message and exit
		//	  --register  launches the registration dialog on startup
		//	[jsefler@jseflerT5400 ~]$ echo $?
		//	0
		else {	// started working in RHEL7.2 when gui was updated for gtk3
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(0), "ExitCode from calling subscription-manager-gui --help with no X-Display");
			Assert.assertTrue(!result.getStdout().toLowerCase().contains("traceback"), "Stdout from calling subscription-manager-gui --help with no X-Display appears to display its usage does not report a traceback.");
			Assert.assertTrue(!result.getStderr().toLowerCase().contains("traceback"), "Stderr from calling subscription-manager-gui --help with no X-Display appears to display its usage does not report a traceback.");
			Assert.assertTrue(result.getStdout().startsWith("Usage: subscription-manager-gui"), "Stdout from calling subscription-manager-gui --help with no X-Display appears to display its usage.");
			Assert.assertEquals(result.getStderr(), "","Stderr from calling subscription-manager-gui --help with no X-Display");
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21757", "RHEL7-51263"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager, subscription-manager-gui, rhn-migrate-classic-to-rhsm, and other CLI tools: assert only expected command line options are available",
			groups={"Tier2Tests",},
			dataProvider="ExpectedCommandLineOptionsData")
	@ImplementsNitrateTest(caseId=46713)
	//@ImplementsNitrateTest(caseId=46707)
	public void testCommandLineHelp(Object bugzilla, String helpCommand, Integer exitCode, String stdoutRegex, List<String> expectedOptions) {
		log.info("Testing subscription-manager-cli command line options '"+helpCommand+"' and verifying the exit code and that ONLY the expected options are available.");
		SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(client,helpCommand,exitCode);
		
		Pattern pattern = Pattern.compile(stdoutRegex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(result.getStdout());
		Assert.assertTrue(matcher.find(),"Available command line options matching regex '"+stdoutRegex+"' are shown with command: "+helpCommand);
		
		// find all the matches to stderrRegex
		List <String> actualOptions = new ArrayList<String>();
		do {
			
			// TEMPORARY WORKAROUND FOR BUG
			if (!helpCommand.contains(" register") && !helpCommand.contains(" config") && (matcher.group().contains("--serverurl")||matcher.group().contains("--baseurl"))) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="842768"; 
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring the presence of option '"+matcher.group()+"' for command '"+helpCommand+"' while bug '"+bugId+"' is open.");
					continue;
				}
			}
			// END OF WORKAROUND
			
			actualOptions.add(matcher.group().trim());
		} while (matcher.find());
		
		// assert all of the expectedOptions were found and that no unexpectedOptions were found
		for (String expectedOption : expectedOptions) {
			if (!actualOptions.contains(expectedOption)) {
				log.warning("Could not find the expected command '"+helpCommand+"' option '"+expectedOption+"'.");
			} else {
				Assert.assertTrue(actualOptions.contains(expectedOption),"The expected command '"+helpCommand+"' option '"+expectedOption+"' is available.");
			}
		}
		for (String actualOption : actualOptions) {
			if (!expectedOptions.contains(actualOption))
				log.warning("Found an unexpected command '"+helpCommand+"' option '"+actualOption+"'.");
		}
		Assert.assertTrue(actualOptions.containsAll(expectedOptions), "All of the expected command '"+helpCommand+"' line options are available.");
		Assert.assertTrue(expectedOptions.containsAll(actualOptions), "All of the available command '"+helpCommand+"' line options are expected.");
	}
	
	

	
	
	// Candidates for an automated Test:
	// TODO Bug 694662 - the whitespace in the title line of man subscription-manager-gui is completely consumed https://github.com/RedHatQE/rhsm-qe/issues/151
	// TODO Bug 765905 - add man pages for subscription-manager-migration https://github.com/RedHatQE/rhsm-qe/issues/152
	
	
	// Configuration Methods ***********************************************************************
	
	@BeforeClass(groups={"setup"})
	public void makewhatisBeforeClass() {
		if (clienttasks==null) return;
		
		// ensure that the whatis database is built (often needed on Beaker provisioned systems)
// get rid of this check since whatis subscription-manager may pass while whatis rhn-migrate-classic-to-rhsm may fail thereby needing a call to mandb
//		SSHCommandResult whatisResult = client.runCommandAndWait("whatis "+clienttasks.command);
//		if ((whatisResult.getStdout()+whatisResult.getStderr()).toLowerCase().contains("nothing appropriate")) {

			if (Integer.valueOf(clienttasks.redhatReleaseX)>=7) {
				RemoteFileTasks.runCommandAndAssert(client,"mandb -q",0);	// mandb replaced makewhatis in f14
			} else {
				RemoteFileTasks.runCommandAndAssert(client,"makewhatis",0);
			}
//		}	
	}
	
	
	// Protected Methods ***********************************************************************

	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="ExpectedCommandLineOptionsData")
	public Object[][] getExpectedCommandLineOptionsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getExpectedCommandLineOptionsDataAsListOfLists());
	}
	protected static List<List<Object>> getExpectedCommandLineOptionsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
		// String command, String stdoutRegex, List<String> expectedOptions
		String command;
		List<String> modules = new ArrayList<String>();
		List<String> options = new ArrayList<String>();
		String module;
		String modulesRegex = "^	[\\w-]+";
		       modulesRegex = "^  [\\w-]+";	// valid after bug 848095
		String optionsRegex = "^  --[\\w\\.]+(=[\\w\\.]+)*|^  -\\w(=\\w+)*, --\\w+(=\\w+)*";
		       optionsRegex = "^  --[\\w\\.-]+(=[\\w\\.-]+)*|^  -[\\?\\w]( \\w+)*, --[\\w\\.-]+(=\\w+)*";
		       optionsRegex = "^  --[\\w\\.-]+(=[\\w\\.:-]+)*|^  -[\\?\\w]( \\w+)*, --[\\w\\.:-]+(=\\w+)*";
		       optionsRegex = "^  --[\\w\\.-]+(=[\\w\\.:,\\-]+)*|^  -[\\?\\w]( \\w+)*, --[\\w\\.:,\\-]+(=\\w+)*";		
		// EXAMPLES FOR optionsRegex
		//  -h, --help            show this help message and exit
		//  --list                list the configuration for this system
		//  --remove=REMOVE       remove configuration entry by section.name
		//  --server.hostname=SERVER.HOSTNAME
		//  -?, --help                  Show help options
		//  --help-all                  Show all help options
		//  -f, --force-icon=TYPE       Force display of the icon (expired, partial or warning)
		//  -c, --check-period          How often to check for validity (in seconds)
		//  -i INSTNUMBER, --instnumber=INSTNUMBER
		//	--add=NAME:VALUE			name and value of the option to override separated by
        //  							a colon (can be specified more than once)

		// ========================================================================================
		// subscription-manager MODULES
		modules.clear();
		modules.add("config");
		modules.add("import");
		modules.add("redeem");
		modules.add("orgs");
		modules.add("repos");
		modules.add("clean");
		modules.add("environments");
		modules.add("facts");
		modules.add("identity");
		modules.add("list");
		modules.add("refresh");
		modules.add("register");
		modules.add("subscribe");
		modules.add("unregister");
		modules.add("unsubscribe");
		modules.add("service-level");
		modules.add("release");
		modules.add("version");
		modules.add("attach");	// added by bug 874804
		modules.add("remove");	// added by bug 874749
		modules.add("plugins");	// added by https://engineering.redhat.com/trac/Entitlement/wiki/SubscriptionManagerPlugins
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.7-1")) modules.add("repo-override");	// added as part of bug 803746
		modules.add("status");
		modules.add("auto-attach");	//modules.add("autoheal"); changed by Bug 976867 - subscription-manager autoheal needs feedback and a review of options
		for (String smHelpCommand : new String[]{clienttasks.command+" -h",clienttasks.command+" --help"}) {
			Integer exitCode = smHelpCommand.contains("--help")?0:1;	// coverage for bug 906124; the usage statement permits only "--help" and therefore any differing option (including "-h") should return non-zero exit code
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s [options] MODULENAME --help",clienttasks.command);	// prior to Bug 796730 - subscription-manager usage statement
			usage = String.format("Usage: %s MODULE-NAME [MODULE-OPTIONS] [--help]",clienttasks.command);
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"796730","906124"}),	smHelpCommand, exitCode, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"906124"}),			smHelpCommand, exitCode, modulesRegex, new ArrayList<String>(modules)}));
		}
		
		
		// subscription-manager config OPTIONS
		module = "config";
		options.clear();
		options.add("-h, --help");
		options.add("--list");
		options.add("--remove=REMOVE");
		/*
		// THIS HARD-CODED LIST IS NOT REALLY THE CORRECT TEST APPROACH BECAUSE THIS LIST IS
		// ACTUALLY GENERATED DYNAMICALLY FROM THE UNION OF THE ACTUAL VALUES IN rhsm.conf
		// AND THE DEFAULT VALUES IN defaultConfFileParameterNames().  THIS TEST SHOULD ONLY
		// FAIL WHEN A NEW DEFAULT VALUE IS ADDED OR REMOVED THE SUBSCRIPTION MANAGER DEVELOPERS.
		options.add("--server.ca_cert_dir=SERVER.CA_CERT_DIR");
		options.add("--server.hostname=SERVER.HOSTNAME");
		options.add("--server.insecure=SERVER.INSECURE");
		options.add("--server.port=SERVER.PORT");
		options.add("--server.prefix=SERVER.PREFIX");
		options.add("--server.proxy_hostname=SERVER.PROXY_HOSTNAME");
		options.add("--server.proxy_password=SERVER.PROXY_PASSWORD");
		options.add("--server.proxy_port=SERVER.PROXY_PORT");
		options.add("--server.proxy_user=SERVER.PROXY_USER");
		options.add("--server.repo_ca_cert=SERVER.REPO_CA_CERT");
		options.add("--server.ssl_verify_depth=SERVER.SSL_VERIFY_DEPTH");
		options.add("--rhsm.baseurl=RHSM.BASEURL");
		options.add("--rhsm.ca_cert_dir=RHSM.CA_CERT_DIR");
		options.add("--rhsm.consumercertdir=RHSM.CONSUMERCERTDIR");
		options.add("--rhsm.entitlementcertdir=RHSM.ENTITLEMENTCERTDIR");
		options.add("--rhsm.hostname=RHSM.HOSTNAME");
		options.add("--rhsm.insecure=RHSM.INSECURE");
		options.add("--rhsm.port=RHSM.PORT");
		options.add("--rhsm.prefix=RHSM.PREFIX");
		options.add("--rhsm.productcertdir=RHSM.PRODUCTCERTDIR");
		options.add("--rhsm.proxy_hostname=RHSM.PROXY_HOSTNAME");
		options.add("--rhsm.proxy_password=RHSM.PROXY_PASSWORD");
		options.add("--rhsm.proxy_port=RHSM.PROXY_PORT");
		options.add("--rhsm.proxy_user=RHSM.PROXY_USER");
		options.add("--rhsm.repo_ca_cert=RHSM.REPO_CA_CERT");
		options.add("--rhsm.manage_repos=RHSM.MANAGE_REPOS");	// Bug 797996 - new configuration for rhsm.manage_repos should be exposed
		options.add("--rhsm.ssl_verify_depth=RHSM.SSL_VERIFY_DEPTH");
		options.add("--rhsmcertd.ca_cert_dir=RHSMCERTD.CA_CERT_DIR");
		options.add("--rhsmcertd.certfrequency=RHSMCERTD.CERTFREQUENCY");
		options.add("--rhsmcertd.healfrequency=RHSMCERTD.HEALFREQUENCY");
		options.add("--rhsmcertd.hostname=RHSMCERTD.HOSTNAME");
		options.add("--rhsmcertd.insecure=RHSMCERTD.INSECURE");
		options.add("--rhsmcertd.port=RHSMCERTD.PORT");
		options.add("--rhsmcertd.prefix=RHSMCERTD.PREFIX");
		options.add("--rhsmcertd.proxy_hostname=RHSMCERTD.PROXY_HOSTNAME");
		options.add("--rhsmcertd.proxy_password=RHSMCERTD.PROXY_PASSWORD");
		options.add("--rhsmcertd.proxy_port=RHSMCERTD.PROXY_PORT");
		options.add("--rhsmcertd.proxy_user=RHSMCERTD.PROXY_USER");
		options.add("--rhsmcertd.repo_ca_cert=RHSMCERTD.REPO_CA_CERT");
		options.add("--rhsmcertd.ssl_verify_depth=RHSMCERTD.SSL_VERIFY_DEPTH");
		// after bug 807721, more config options are available
		options.add("--server.certfrequency=SERVER.CERTFREQUENCY");
		options.add("--server.manage_repos=SERVER.MANAGE_REPOS");
		options.add("--server.entitlementcertdir=SERVER.ENTITLEMENTCERTDIR");
		options.add("--server.baseurl=SERVER.BASEURL");
		options.add("--server.productcertdir=SERVER.PRODUCTCERTDIR");
		options.add("--server.consumercertdir=SERVER.CONSUMERCERTDIR");
		options.add("--server.healfrequency=SERVER.HEALFREQUENCY");
		options.add("--rhsm.certfrequency=RHSM.CERTFREQUENCY");
		options.add("--rhsm.healfrequency=RHSM.HEALFREQUENCY");
		options.add("--rhsmcertd.manage_repos=RHSMCERTD.MANAGE_REPOS");
		options.add("--rhsmcertd.entitlementcertdir=RHSMCERTD.ENTITLEMENTCERTDIR");
		options.add("--rhsmcertd.baseurl=RHSMCERTD.BASEURL");
		options.add("--rhsmcertd.productcertdir=RHSMCERTD.PRODUCTCERTDIR");
		options.add("--rhsmcertd.consumercertdir=RHSMCERTD.CONSUMERCERTDIR");
		*/
		// add the expected default configurations
		for (String section : new String[]{"server","rhsm","rhsmcertd","logging"/* added by bug 1334916 */}) {
//			for (String confFileParameterName : clienttasks.defaultConfFileParameterNames(null)) {	// valid before bug 988476
			for (String confFileParameterName : clienttasks.defaultConfFileParameterNames(section,null)) {
				options.add(String.format("--%s.%s=%s.%s",section.toLowerCase(),confFileParameterName.toLowerCase(),section.toUpperCase(),confFileParameterName.toUpperCase()));
			}
		}
		// add the unexpected configurations that were added manually or remain as deprecated configurations from a subscription-manager upgrade
		String confFileContents = RemoteFileTasks.runCommandAndAssert(client, "egrep -v  \"^\\s*(#|$)\" "+clienttasks.rhsmConfFile, 0).getStdout();
		String section=null;
		for (String line : confFileContents.split("\n")) {
			line = line.trim();
			if (line.isEmpty()) continue;
			if (line.matches("\\[\\w+\\]")) {section=line.replaceFirst("\\[","").replaceFirst("\\]",""); continue;}
			String parameterName = line.split("=|:",2)[0].trim();
//			if (clienttasks.defaultConfFileParameterNames(true).contains(parameterName.toLowerCase())) continue;	// valid before bug 988476
			if (clienttasks.defaultConfFileParameterNames(section,true).contains(parameterName.toLowerCase())) continue;
			options.add(String.format("--%s.%s=%s.%s",section.toLowerCase(),parameterName.toLowerCase(),section.toUpperCase(),parameterName.toUpperCase()));
		}
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("919512"), smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager import OPTIONS
		module = "import";
		options.clear();
		options.add("-h, --help");
		//options("--certificate=CERTIFICATE_FILES");	// prior to fix for Bug 735212
		options.add("--certificate=CERTIFICATE_FILE");
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=733873
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="733873"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			options.add("--proxy=PROXY_URL");
			options.add("--proxyuser=PROXY_USER");
			options.add("--proxypassword=PROXY_PASSWORD");
		}
		// END OF WORKAROUND
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}

		// subscription-manager redeem OPTIONS
		module = "redeem";
		options.clear();
		options.add("-h, --help");
		options.add("--email=EMAIL");
		options.add("--locale=LOCALE");
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager orgs OPTIONS
		module = "orgs";
		options.clear();
		options.add("-h, --help");
		options.add("--serverurl=SERVER_URL");
		options.add("--username=USERNAME");
		options.add("--password=PASSWORD");
		options.add("--insecure");		// added by bug 844411
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager repos OPTIONS
		module = "repos";
		options.clear();
		options.add("-h, --help");
		options.add("--list");
		options.add("--enable=REPOID");
		options.add("--disable=REPOID");
		options.add("--proxy=PROXY_URL");		// added by bug 906642
		options.add("--proxyuser=PROXY_USER");		// added by bug 906642
		options.add("--proxypassword=PROXY_PASSWORD");		// added by bug 906642
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.4-1")) {
			options.add("--list-enabled");		// added by bug 1119648
			options.add("--list-disabled");		// added by bug 1119648
		}
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager clean OPTIONS
		module = "clean";
		options.clear();
		options.add("-h, --help");
		// removed in https://bugzilla.redhat.com/show_bug.cgi?id=664581
		//options("--proxy=PROXY_URL");
		//options("--proxyuser=PROXY_USER");
		//options("--proxypassword=PROXY_PASSWORD");
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("664581"), smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager environments OPTIONS
		module = "environments";
		options.clear();
		options.add("-h, --help");
		options.add("--serverurl=SERVER_URL");
		options.add("--username=USERNAME");
		options.add("--password=PASSWORD");
		options.add("--org=ORG_KEY");					// changed by bug 878097	options.add("--org=ORG");
		options.add("--insecure");		// added by bug 844411
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager facts OPTIONS
		module = "facts";
		options.clear();
		options.add("-h, --help");
		options.add("--list");
		options.add("--update");
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager identity OPTIONS
		module = "identity";
		options.clear();
		options.add("-h, --help");
		options.add("--username=USERNAME");
		options.add("--password=PASSWORD");
		options.add("--regenerate");
		options.add("--force");
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager list OPTIONS
		module = "list";
		options.clear();
		options.add("-h, --help");
		options.add("--installed");	// result of https://bugzilla.redhat.com/show_bug.cgi?id=634254
		options.add("--consumed");
		options.add("--available");
		options.add("--all");
		options.add("--servicelevel=SERVICE_LEVEL");	// result of https://bugzilla.redhat.com/show_bug.cgi?id=800999
		options.add("--ondate=ON_DATE");	// result of https://bugzilla.redhat.com/show_bug.cgi?id=672562
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.3-1")) options.add("--no-overlap");		// added by Bug 654501 - [RFE] subscription-manager list should accept filtering
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.3-1")) options.add("--match-installed");	// added by Bug 654501 - [RFE] subscription-manager list should accept filtering
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.6-1")) options.add("--matches=FILTER_STRING");	// added by Bug 1146125 - [RFE] would like a --contains-text option for subscription-manager list module
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) options.add("--pool-only");	// added by Bug 1159974 - RFE: Add a --pool-only option to the list subcommand	// commit 25cb581cb6ebe13063d0f78a5020715a2854d337
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-1")) options.add("--after=AFTER");	// added by Bug 1479353 - [RFE] subscription-manager list should provide an easier means to show subscriptions which start in the future	// commit ae9df2951ae3d17b50dc7c8f1c1ffa04c9edb8fc
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager status OPTIONS
		module = "status";
		options.clear();
		options.add("-h, --help");
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.9.2-1")) options.add("--ondate=ON_DATE");	// subscription-manager commit 957f3f5fb4689f22355e0101185bd560e67f3462
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("977481"), smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager refresh OPTIONS
		module = "refresh";
		options.clear();
		options.add("-h, --help");
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager register OPTIONS
		module = "register";
		options.clear();
		options.add("-h, --help");
		options.add("--baseurl=BASE_URL");
		options.add("--serverurl=SERVER_URL");
		options.add("--username=USERNAME");
		options.add("--type=UNITTYPE");					// changed by bug 874816	options.add("--type=CONSUMERTYPE");
		options.add("--name=SYSTEMNAME");				// changed by bug 874816	options.add("--name=CONSUMERNAME");
		options.add("--password=PASSWORD");
		options.add("--consumerid=SYSTEMID");			// changed by bug 874816	options.add("--consumerid=CONSUMERID");
		options.add("--org=ORG_KEY");					// changed by bug 878097	options.add("--org=ORG");
		options.add("--environment=ENVIRONMENT");
		options.add("--autosubscribe");
		options.add("--auto-attach");					// added by bug 876340
		options.add("--insecure");						// added by bug 844411
		options.add("--force");
		options.add("--activationkey=ACTIVATION_KEYS");	// Bug 874755 - help message terminology for cli options that can be specified in multiplicity 
		options.add("--servicelevel=SERVICE_LEVEL");
		options.add("--release=RELEASE");
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.20.2-1")) options.remove("--type=UNITTYPE");		// removed by bug 1461003 commit e0c34a729e9e347ab1e0f4f5fa656c8b20205fdf
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[]{null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("628589"), smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager unregister OPTIONS
		module = "unregister";
		options.clear();
		options.add("-h, --help");
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager subscribe OPTIONS
		module = "subscribe";
		options.clear();
		options.add("-h, --help");
		options.add("--pool=POOL");
		options.add("--quantity=QUANTITY");
		options.add("--auto");	// result of https://bugzilla.redhat.com/show_bug.cgi?id=680399
		options.add("--servicelevel=SERVICE_LEVEL");
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) options.add("--file=FILE");	// added by Bug 1159974 // commit 3167333fc3a261de939f4aa0799b4283f2b9f4d2
		//options("--regtoken=REGTOKEN");	// https://bugzilla.redhat.com/show_bug.cgi?id=670823
		//options("--email=EMAIL");			// https://bugzilla.redhat.com/show_bug.cgi?id=670823
		//options("--locale=LOCALE");		// https://bugzilla.redhat.com/show_bug.cgi?id=670823
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			String deprecation = "Deprecated, see attach";	// added by bug 874808
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, "^"+deprecation+"$", Arrays.asList(new String[]{deprecation})}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager attach OPTIONS	// added by bug 874804
		module = "attach";
		options.clear();
		options.add("-h, --help");
		options.add("--pool=POOL");
		options.add("--quantity=QUANTITY");
		options.add("--auto");
		options.add("--servicelevel=SERVICE_LEVEL");
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) options.add("--file=FILE");	// added by Bug 1159974 // commit 3167333fc3a261de939f4aa0799b4283f2b9f4d2
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			usages.add(usage);
			String deprecation = "Attach a specified subscription to the registered system";
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, "^"+deprecation+"$", Arrays.asList(new String[]{deprecation})}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager unsubscribe OPTIONS
		module = "unsubscribe";
		options.clear();
		options.add("-h, --help");
		options.add("--serial=SERIAL");	// Bug 874755 - help message terminology for cli options that can be specified in multiplicity 
		options.add("--all");
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.16.5-1")) options.add("--pool=POOL_ID");	// added by Bug 1198178 - [RFE] Subscription-manager unsubscribe command should support --pool option	// commit 3d2eb4b8ef8e2094311e3872cdb9602b84fed9be
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			String deprecation = "Deprecated, see remove";	// added by bug 874749
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, "^"+deprecation+"$", Arrays.asList(new String[]{deprecation})}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager remove OPTIONS	// added by bug 874749
		module = "remove";
		options.clear();
		options.add("-h, --help");
		options.add("--serial=SERIAL");
		options.add("--all");
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.16.5-1")) options.add("--pool=POOL_ID");	// added by Bug 1198178 - [RFE] Subscription-manager unsubscribe command should support --pool option	// commit 3d2eb4b8ef8e2094311e3872cdb9602b84fed9be
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			usages.add(usage);
			String deprecation = "Remove all or specific subscriptions from this system";
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, "^"+deprecation+"$", Arrays.asList(new String[]{deprecation})}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager service-level OPTIONS
		module = "service-level";
		options.clear();
		options.add("-h, --help");
		options.add("--serverurl=SERVER_URL");
		options.add("--username=USERNAME");
		options.add("--password=PASSWORD");
		options.add("--org=ORG_KEY");					// changed by bug 878097	options.add("--org=ORG");
		options.add("--show");
		options.add("--list");
		options.add("--set=SERVICE_LEVEL");
		options.add("--unset");
		options.add("--insecure");		// added by bug 844411
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
				
		// subscription-manager release OPTIONS
		module = "release";
		options.clear();
		options.add("-h, --help");
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		options.add("--set=RELEASE");
		options.add("--list");
		options.add("--unset");
		options.add("--show");	// Bug 812153 - release command should have a --show option which is the default
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager plugins OPTIONS
		module = "plugins";
		options.clear();
		options.add("-h, --help");
		options.add("--list");
		options.add("--listslots");
		options.add("--listhooks");
		options.add("--verbose");
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager repo-override OPTIONS
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.7-1")) {	// introduced by RFE Bug 803746
		module = "repo-override";
		options.clear();
		options.add("-h, --help");
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		options.add("--repo=REPOID");
		options.add("--remove=NAME");
		options.add("--add=NAME:VALUE");
		options.add("--remove-all");
		options.add("--list");
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("977481"), smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		}
		
		// subscription-manager version OPTIONS
		module = "version";
		options.clear();
		options.add("-h, --help");
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("977481"), smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// subscription-manager auto-attach OPTIONS
		module = "auto-attach";	//module = "autoheal";	// changed by Bug 976867 - subscription-manager autoheal needs feedback and a review of options
		options.clear();
		options.add("-h, --help");
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		options.add("--show");
		options.add("--enable");
		options.add("--disable");
		for (String smHelpCommand : new String[]{clienttasks.command+" -h "+module,clienttasks.command+" --help "+module}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",clienttasks.command,module);
			//if (clienttasks.redhatRelease.contains("release 5")) usage = usage.replaceFirst("^Usage", "usage"); // TOLERATE WORKAROUND FOR Bug 693527 ON RHEL5
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("976867"), smHelpCommand, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("976867"), smHelpCommand, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// ========================================================================================
		// subscription-manager-gui OPTIONS
		if (!client.runCommandAndWait("rpm -q "+clienttasks.command+"-gui").getStdout().contains("is not installed")) {	// test only when the rpm is installed
			//	[root@jsefler-6 ~]# subscription-manager-gui --help
			//	Usage: subscription-manager-gui [OPTIONS]
			//
			//	Options:
			//	  -h, --help  show this help message and exit
			//	  --register  launches the registration dialog on startup.
			
			command = clienttasks.command+"-gui"; 
			options.clear();
			options.add("-h, --help");
			options.add("--register");
			for (String commandHelp : new String[]{command+" -h", command+" --help"}) {
				
				// 2013-10-11 update... WONTFIX "Unable to open a display"; see https://bugzilla.redhat.com/show_bug.cgi?id=881095#c7
				// attempt to avoid bug 881095 RuntimeError: could not open display
				/*
				if (Integer.valueOf(clienttasks.redhatReleaseX)==5){
					log.warning("Employing WORKAROUND for https://bugzilla.redhat.com/show_bug.cgi?id=881095#c7 by exporting DISPLAY");
					commandHelp = "export DISPLAY=localhost:2 && "+commandHelp;
				} else
				if (Integer.valueOf(clienttasks.redhatReleaseX)==6){
					log.warning("Employing WORKAROUND for https://bugzilla.redhat.com/show_bug.cgi?id=881095#c7 by exporting DISPLAY");
					commandHelp = "export DISPLAY=localhost:10.0 && "+commandHelp;
				} else
				if (Integer.valueOf(clienttasks.redhatReleaseX)==7){
					log.warning("Employing WORKAROUND for https://bugzilla.redhat.com/show_bug.cgi?id=881095#c7 by exporting DISPLAY");
					//commandHelp = "export DISPLAY=:0 && "+commandHelp;	// worked on RHEL70
					commandHelp = "export DISPLAY=localhost:2 && "+commandHelp;
				}
				*/
				// problem: can't predict which export will work
				// solution: try several until one works and then use it
				for (String exportDisplay : Arrays.asList(new String[]{"export DISPLAY=localhost:2", "export DISPLAY=localhost:10.0", "export DISPLAY=localhost:11.0", "export DISPLAY=:0"})) {
					String commandHelpWithExportDisplay = exportDisplay+" && "+commandHelp;
					if (client.runCommandAndWait(commandHelpWithExportDisplay).getStdout().trim().startsWith("Usage")) {
						commandHelp = commandHelpWithExportDisplay;
						break;
					}
				}
				
				List <String> usages = new ArrayList<String>();
				String usage = String.format("Usage: %s [OPTIONS]",command);
				usages.add(usage);
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"881095","905649"}), commandHelp, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\|", "\\\\|").replaceAll("\\?", "\\\\?")+" *$", usages}));
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"881095","905649"}), commandHelp, 0, optionsRegex, new ArrayList<String>(options)}));
			}
		}
		
		// ========================================================================================
		// rhsm-icon OPTIONS
		if (!client.runCommandAndWait("rpm -q "+clienttasks.command+"-gui").getStdout().contains("is not installed")) {	// test only when the rpm is installed
			//	[root@jsefler-onprem-5server ~]# rhsm-icon -?
			//	Usage:
			//	  rhsm-icon [OPTION...] rhsm icon
			//	
			//	Help Options:
			//	  -?, --help                  Show help options
			//	  --help-all                  Show all help options
			//	  --help-gtk                  Show GTK+ Options
			//	
			//	Application Options:
			//	  -c, --check-period          How often to check for validity (in seconds)
			//	  -d, --debug                 Show debug messages
			//	  -f, --force-icon=TYPE       Force display of the icon (expired, partial or warning)
			//	  -i, --check-immediately     Run the first status check right away
			//	  --display=DISPLAY           X display to use
			command = "rhsm-icon";
			List <String> rhsmIconOptions = new ArrayList<String>();
			if (clienttasks.redhatReleaseX.equals("5"))	rhsmIconOptions.add("-?, --help");	// rhel5
			else										rhsmIconOptions.add("-h, --help");	// rhel6
			rhsmIconOptions.add("--help-all");
			rhsmIconOptions.add("--help-gtk");
			rhsmIconOptions.add("-c, --check-period");
			rhsmIconOptions.add("-d, --debug");
			rhsmIconOptions.add("-f, --force-icon=TYPE");
			rhsmIconOptions.add("-i, --check-immediately");
			rhsmIconOptions.add("--display=DISPLAY");
			for (String helpOption : rhsmIconOptions.get(0).split(" *, *")) {	// "-?, --help"
				String rhsmIconHelpCommand = command+" "+helpOption;
				List <String> usages = new ArrayList<String>();
				String usage = command+" [OPTIONS]";
				usage = command+" [OPTION...]"; // usage = rhsmIconCommand+" [OPTION...] rhsm icon"; // Bug 771756 - rhsm-icon --help usage message is misleading
				usages.add(usage);
				if (!Arrays.asList("6.1","5.7","6.2","5.8","6.3").contains(clienttasks.redhatReleaseXY)) // skip the following rhsmIconHelpCommand usage test since bug 771756 was not fixed until 5.9
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("771756"), rhsmIconHelpCommand, 0, usage.replace("...","(\\.\\.\\.|…)").replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
				ll.add(Arrays.asList(new Object[] {null, rhsmIconHelpCommand, 0, optionsRegex, rhsmIconOptions}));
			}
			List <String> rhsmIconGtkOptions = new ArrayList<String>();
			rhsmIconGtkOptions.add("--screen=SCREEN");
			// GTK options are presented here: http://developer.gnome.org/gtk-tutorial/2.90/c39.html
			rhsmIconGtkOptions.add("--class=CLASS");
			rhsmIconGtkOptions.add("--name=NAME");
			rhsmIconGtkOptions.add("--display=DISPLAY");
			rhsmIconGtkOptions.add("--sync");
			rhsmIconGtkOptions.add("--gtk-module=MODULES");
			rhsmIconGtkOptions.add("--g-fatal-warnings");
			if (!clienttasks.redhatReleaseX.equals("5"))	rhsmIconGtkOptions.add("--gdk-debug=FLAGS");
			if (!clienttasks.redhatReleaseX.equals("5"))	rhsmIconGtkOptions.add("--gdk-no-debug=FLAGS");
			if (!clienttasks.redhatReleaseX.equals("5"))	rhsmIconGtkOptions.add("--gtk-debug=FLAGS");
			if (!clienttasks.redhatReleaseX.equals("5"))	rhsmIconGtkOptions.add("--gtk-no-debug=FLAGS");
			String rpmQueryCommand = "rpm -qf `which rhsm-icon` --requires | egrep '^(gtk|pygtk)'";
			if (clienttasks.isPackageVersion("subscription-manager-gui", ">=", "1.21.4-1")) {	// commit 23b5409c76d586c4e34440788d612e7ed65e2df6 Stop building subscription-manager-gui, when Python 3 is used
				rpmQueryCommand = "rpm -q rhsm-gtk --requires | egrep '^(gtk|pygtk)'";
			}
			if (client.runCommandAndWait(rpmQueryCommand).getStdout().trim().startsWith("gtk3")) {	// if (clienttasks.isPackageVersion("subscription-manager",">=","1.15.3")) {	// commit 3c51d2096ecdd1a7ba6981776bd9aa6959aa2e1e use gtk3 for rhsm-icon
				// effectively this happens on RHEL >= 7.2
				// options not offered by gtk3
				rhsmIconGtkOptions.remove("--screen=SCREEN");
				rhsmIconGtkOptions.remove("--sync");
			}
			ll.add(Arrays.asList(new Object[] {null, command+" --help-gtk", 0, optionsRegex, rhsmIconGtkOptions}));
			List <String> rhsmIconAllOptions = new ArrayList<String>();
			rhsmIconAllOptions.addAll(rhsmIconOptions);
			rhsmIconAllOptions.addAll(rhsmIconGtkOptions);
			ll.add(Arrays.asList(new Object[] {null, command+" --help-all", 0, optionsRegex, rhsmIconAllOptions}));
		}
		
		// ========================================================================================
		// rhn-migrate-classic-to-rhsm OPTIONS
		if (!client.runCommandAndWait("rpm -q "+clienttasks.command+"-migration").getStdout().contains("is not installed")) {	// test only when the rpm is installed
			//	[root@jsefler-onprem-5server ~]# rhn-migrate-classic-to-rhsm -h
			//	usage: /usr/sbin/rhn-migrate-classic-to-rhsm [--force|--cli-only|--help|--no-auto]
			//	
			//	options:
			//	  -f, --force     Ignore Channels not available on RHSM
			//	  -c, --cli-only  Don't launch the GUI tool to subscribe the system, just use
			//	                  the CLI tool which will do it automatically
			//	  -n, --no-auto   Don't launch subscription manager at end of process.
			//	  -h, --help      show this help message and exit
			
			command = MigrationTests.rhnMigrateTool; 
			options.clear();
			options.add("-f, --force");
			options.add("-g, --gui");
			options.add("-n, --no-auto");
			options.add("-s SERVICELEVEL, --servicelevel=SERVICELEVEL");
			options.add("--serverurl=SERVERURL");
			options.add("--no-proxy");	// added by Bug 915847 - rhn-migrate-classic-to-rhsm fails when used with a proxy with an internal SAM
			options.add("--org=ORG");					// added by Bug 877331 - missing --org --environment arguments for migration script
			options.add("--environment=ENVIRONMENT");	// added by Bug 877331 - missing --org --environment arguments for migration script
			if (clienttasks.isPackageVersion("subscription-manager-migration",">=","1.10.3-1")) {
				// added by Bug 912375 - RFE - "rhn-migrate-classic-to-rhsm" migration script to accept the expected parameter either via standard input or the equivalent of an "answer" file"
				options.add("--redhat-user=REDHATUSER");
				options.add("--redhat-password=REDHATPASSWORD");
				options.add("--subscription-service-user=SUBSERVICEUSER");
				options.add("--subscription-service-password=SUBSERVICEPASSWORD");
			}
			if (clienttasks.isPackageVersion("subscription-manager-migration",">=","1.13.1")) {
				options.remove("--redhat-user=REDHATUSER");
				options.remove("--redhat-password=REDHATPASSWORD");
				options.remove("--subscription-service-user=SUBSERVICEUSER");
				options.remove("--subscription-service-password=SUBSERVICEPASSWORD");
				options.remove("--serverurl=SERVERURL");
				options.remove("-s SERVICELEVEL, --servicelevel=SERVICELEVEL");
				options.remove("-g, --gui");
				options.add("--legacy-user=LEGACY_USER");
				options.add("--legacy-password=LEGACY_PASSWORD");
				options.add("--destination-user=DESTINATION_USER");
				options.add("--destination-password=DESTINATION_PASSWORD");
				options.add("--destination-url=DESTINATION_URL");
				options.add("-s SERVICE_LEVEL, --service-level=SERVICE_LEVEL");
			}
			if (clienttasks.isPackageVersion("subscription-manager-migration",">=","1.13.8")) { // commit e53f0369b621902b75f2dbe047d97dc9ba3cc1c0  revert for bug 1157761
				options.remove("-s SERVICE_LEVEL, --service-level=SERVICE_LEVEL");
				options.add("-s SERVICE_LEVEL, --servicelevel=SERVICE_LEVEL");
			}
			if (clienttasks.isPackageVersion("subscription-manager-migration",">=","1.14.1-1")) { // commit 00461f3751f9db182227c9973c41b305e378638a  RFE Bug 1154375: Allow use of activation keys during migration.
				options.add("--activation-key=ACTIVATION_KEYS");
			}
			if (clienttasks.isPackageVersion("subscription-manager-migration",">=","1.14.3-1")) { // commit 5df7aaaa69a22b9e3f771971f1aa4e58657c8377	RFE Bug 1180273 - [RFE] rhn-migrate-classic-to-rhsm should allow the user to migrate a system without requiring credentials on RHN Classic
				options.add("--registration-state=keep,purge");
			}
			if (clienttasks.isPackageVersion("subscription-manager-migration",">=","1.14.6-1")) { // commit 6eded942a7d184ef7ed92bbd94225120ee2f2f20	RFE Bug 1180273 - [RFE] rhn-migrate-classic-to-rhsm should allow the user to migrate a system without requiring credentials on RHN Classic
				options.remove("--registration-state=keep,purge");
				options.add("--keep");
			}
			if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.18.2-1")) {	// commit 871264dbb0cc091d3eaefabfdfd2e51d6bbc0a3c	RFE Bug 1185914 - [RFE] rhn-migrate-classic-to-rhsm should give the option to remove RHN Classic related packages / daemon
				options.add("--remove-rhn-packages");
			}
			options.add("-h, --help");
			for (String commandHelp : new String[]{command+" -h", command+" --help"}) {
				List <String> usages = new ArrayList<String>();
				String usage = String.format("usage: %s [OPTIONS]",command);
				usage = String.format("Usage: %s [OPTIONS]",command);
				usages.add(usage);
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("1052297"), commandHelp, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\|", "\\\\|").replaceAll("\\?", "\\\\?")+" *$", usages}));
				ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("1052297"), commandHelp, 0, optionsRegex, new ArrayList<String>(options)}));
			}
		}
		
		// ========================================================================================
		// install-num-migrate-to-rhsm OPTIONS
		if (!client.runCommandAndWait("rpm -q "+clienttasks.command+"-migration").getStdout().contains("is not installed")) {	// test only when the rpm is installed
			if (clienttasks.redhatReleaseX.equals("5")) {	// test only on RHEL5
				if (clienttasks.isPackageVersion("subscription-manager-migration", "<=", "1.11.3-4")) {	// install-num-migrate-to-rhsm was removed by bug 1092754
					//	[root@jsefler-onprem-5server ~]# install-num-migrate-to-rhsm --help
					//	usage: install-num-migrate-to-rhsm [options]
					//	
					//	options:
					//	  -h, --help            show this help message and exit
					//	  -i INSTNUMBER, --instnumber=INSTNUMBER
					//	                        Install number to run against
					//	  -d, --dryrun          Only print the files which would be copied over
					
					command = MigrationTests.installNumTool; 
					options.clear();
					options.add("-h, --help");
					options.add("-i INSTNUMBER, --instnumber=INSTNUMBER");
					options.add("-d, --dryrun");
					for (String commandHelp : new String[]{command+" -h", command+" --help"}) {
						List <String> usages = new ArrayList<String>();
						String usage = String.format("usage: %s [options]",command);
						usage = String.format("Usage: %s [OPTIONS]",command);	// changed by bug 876692
						usages.add(usage);
						ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\|", "\\\\|").replaceAll("\\?", "\\\\?")+" *$", usages}));
						ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, optionsRegex, new ArrayList<String>(options)}));
					}
				}
			}
		}
		
		// ========================================================================================
		// rhsmcertd OPTIONS
		
		//	[root@nec-em15 ~]# rhsmcertd -?
		//	Usage:
		//	  rhsmcertd [OPTION...] 
		//	
		//	Help Options:
		//	  -?, --help                      Show help options
		//	  --help-all                      Show all help options
		//	  --help-rhsmcertd                rhsmcertd
		//	
		//	Application Options:
		//	  -c, --cert-interval=MINUTES     Interval to run cert check (in minutes)
		//	  -i, --heal-interval=MINUTES     Interval to run healing (in minutes)
		//	  -n, --now                       Run the initial checks immediatly, with no delay.
		//	  -d, --debug                     Show debug messages
		
		command = clienttasks.rhsmCertD; 
		options.clear();
		if (clienttasks.redhatReleaseX.equals("5"))	options.add("-?, --help");	// rhel5	// this logic is also needed in TranslationTests.getTranslatedCommandLineHelpDataAsListOfLists()
		else										options.add("-h, --help");	// rhel6
		//options.add("--help-all");		// removed by Bug 842020 - what is rhsmcertd --help-rhsmcertd? 
		//options.add("--help-rhsmcertd");	// removed by Bug 842020 - what is rhsmcertd --help-rhsmcertd? 
		options.add("-c, --cert-check-interval=MINUTES"); 		// updated by bug 882459	options.add("-c, --cert-interval=MINUTES");
		options.add("--cert-interval=MINUTES");					// added by bug 882459 as a deprecated, see --cert-check-interval
		options.add("-i, --auto-attach-interval=MINUTES");		// updated by bug 876753	options.add("-i, --heal-interval=MINUTES");
		options.add("--heal-interval=MINUTES");					// added by bug 876753 as a deprecated, see --auto-attach-interval
		options.add("-n, --now");
		options.add("-d, --debug");
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.19.8-1")) {	// commit e9f8421285fc6541166065a8b55ee89b9a425246	RFE Bug 1435013: Add splay option to rhsmcertd, randomize over interval
			options.add("-s, --no-splay");	// do not add an offset to the initial checks.
		}
		for (String helpOption : options.get(0).split(" *, *")) {	// "-?, --help"
			String commandHelp = command+" "+helpOption;
			List <String> usages = new ArrayList<String>();
			String usage = command+" [OPTIONS]";
			usage = command+" [OPTION...]";
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, usage.replace("...","(\\.\\.\\.|…)").replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug(new String[]{"876753","882459"}), commandHelp, 0, optionsRegex, new ArrayList<String>(options)}));
		}

		
		// ========================================================================================
		// /usr/libexec/rhsmd OPTIONS
		
		//	[root@jsefler-6 ~]# /usr/libexec/rhsmd --help
		//	Usage: rhsmd [options]
		//
		//	Options:
		//	  -h, --help            show this help message and exit
		//	  -d, --debug           Display debug messages
		//	  -k, --keep-alive      Stay running (don't shut down after the first dbus
		//	                        call)
		//	  -s, --syslog          Run standalone and log result to syslog
		//	  -f FORCE_SIGNAL, --force-signal=FORCE_SIGNAL
		//	                        Force firing of a signal (valid, expired, warning,
		//	                        partial, classic or registration_required)
		//	  -i, --immediate       Fire forced signal immediately (requires --force-
		//	                        signal)
		
		command = clienttasks.rhsmComplianceD; 
		options.clear();
		options.add("-h, --help");
		options.add("-d, --debug");
		options.add("-k, --keep-alive");
		options.add("-s, --syslog");
		options.add("-f FORCE_SIGNAL, --force-signal=FORCE_SIGNAL");
		options.add("-i, --immediate");
		for (String commandHelp : new String[]{command+" -h", command+" --help"}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s [options]",command.replaceFirst("/.+/", ""));
			usage = String.format("Usage: %s [OPTIONS]",command.replaceFirst("/.+/", ""));	// changed by bug 876692
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// ========================================================================================
		// /usr/libexec/rhsmcertd-worker OPTIONS
		
		//	[root@jsefler-6 ~]# /usr/libexec/rhsmcertd-worker --help
		//	Usage: rhsmcertd-worker [options]
		//
		//	Options:
		//	  -h, --help  show this help message and exit
		//	  --autoheal  perform an autoheal check
		
		command = clienttasks.rhsmCertDWorker; 
		options.clear();
		options.add("-h, --help");
		options.add("--autoheal");
		for (String commandHelp : new String[]{command+" -h", command+" --help"}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s [options]",command.replaceFirst("/.+/", ""));
			usage = String.format("Usage: %s [OPTIONS]",command.replaceFirst("/.+/", ""));	// changed by bug 876692
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// ========================================================================================
		// rct MODULES
		
		//	[root@jsefler-rhel59 ~]# rct --help
		//	
		//	Usage: rct MODULE-NAME [MODULE-OPTIONS] [--help]
		//	
		//	
		//	Primary Modules:
		//	
		//		cat-cert       Print certificate info to standard output.
		//	
		//	Other Modules (Please consult documentation):
		
		command = "rct"; 
		modules.clear();
		modules.add("cat-cert");
		modules.add("stat-cert");
		modules.add("cat-manifest");
		modules.add("dump-manifest");
		for (String commandHelp : new String[]{command+" -h",command+" --help"}) {
			Integer exitCode = commandHelp.contains("--help")?0:1;		// coverage for bug 906124; the usage statement permits only "--help" and therefore any differing option (including "-h") should return non-zero exit code
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s MODULE-NAME [MODULE-OPTIONS] [--help]",command);
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("906124"), commandHelp, exitCode, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("906124"), commandHelp, exitCode, modulesRegex, new ArrayList<String>(modules)}));
		}
		
		// rct cat-cert OPTIONS
		
		//	[root@jsefler-rhel59 ~]# rct cat-cert --help
		//	Usage: rct cat-cert [OPTIONS] CERT_FILE
		//	
		//	Print certificate info to standard output.
		//	
		//	options:
		//	  -h, --help     show this help message and exit
		//	  --no-products  do not show the cert's product information
		//	  --no-content   do not show the cert's content info.
		module = "cat-cert";
		options.clear();
		options.add("-h, --help");
		options.add("--no-products");
		options.add("--no-content");
		for (String commandHelp : new String[]{command+" "+module+" -h",command+" "+module+" --help"}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS] CERT_FILE",command,module);
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// rct stat-cert OPTIONS
		
		//	[root@jsefler-6 ~]# rct stat-cert --help
		//	Usage: rct stat-cert [OPTIONS] CERT_FILE
		//
		//	Print certificate statistics and sizes
		//
		//	Options:
		//	  -h, --help  show this help message and exit
		module = "stat-cert";
		options.clear();
		options.add("-h, --help");
		for (String commandHelp : new String[]{command+" "+module+" -h",command+" "+module+" --help"}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS] CERT_FILE",command,module);
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// rct cat-manifest OPTIONS
		
		//	[root@jsefler-7 ~]# rct cat-manifest --help
		//	Usage: rct cat-manifest [OPTIONS] MANIFEST_FILE
		//	
		//	Print manifest information
		//	
		//	Options:
		//	  -h, --help  show this help message and exit
		module = "cat-manifest";
		options.clear();
		options.add("-h, --help");
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.17.10-1")) { // RHEL7.3 commit 860b178e0eb5b91df01c424dad29c521e1c23767  Bug 1336883 - [RFE] Update the 'rct' command to allow not outputting content-set data
			options.add("--no-content");
		}
		for (String commandHelp : new String[]{command+" "+module+" -h",command+" "+module+" --help"}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS] MANIFEST_FILE",command,module);
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		// rct dump-manifest OPTIONS
		
		//	[root@jsefler-7 ~]# rct dump-manifest --help
		//	Usage: rct dump-manifest [OPTIONS] MANIFEST_FILE
		//
		//	Dump the contents of a manifest
		//
		//	Options:
		//	  -h, --help            show this help message and exit
		//	  --destination=DESTINATION
		//	                        directory to extract the manifest to
		module = "dump-manifest";
		options.clear();
		options.add("-h, --help");
		options.add("--destination=DESTINATION");
		options.add("-f, --force");	// added by Bug 961124 - attempt to rct dump-manifest twice throws traceback
		for (String commandHelp : new String[]{command+" "+module+" -h",command+" "+module+" --help"}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS] MANIFEST_FILE",command,module);
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {null, commandHelp, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		
		// ========================================================================================
		// rhsm-debug MODULES
		
		//	[root@jsefler-7 ~]# rhsm-debug --help
		//	Usage: rhsm-debug MODULE-NAME [MODULE-OPTIONS] [--help]
		//	
		//	Other Modules:
		//	
		//	  system         None	// Bug 1039907 - Need Description for rhsm-debug system option

		
		command = "rhsm-debug"; 
		modules.clear();
		modules.add("system");
		for (String commandHelp : new String[]{command+" -h",command+" --help"}) {
			Integer exitCode = commandHelp.contains("--help")?0:1;		// coverage for bug 906124; the usage statement permits only "--help" and therefore any differing option (including "-h") should return non-zero exit code
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s MODULE-NAME [MODULE-OPTIONS] [--help]",command);
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("1039653"), commandHelp, exitCode, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("1039653"), commandHelp, exitCode, modulesRegex, new ArrayList<String>(modules)}));
		}
		
		// rhsm-debug system OPTIONS
		
		//	[root@jsefler-7 ~]# rhsm-debug system --help
		//	Usage: rhsm-debug system [OPTIONS] 
		//
		//	Assemble system information as a tar file or directory
		//
		//	Options:
		//	  -h, --help            show this help message and exit
		//	  --proxy=PROXY_URL     proxy URL in the form of proxy_hostname:proxy_port
		//	  --proxyuser=PROXY_USER
		//	                        user for HTTP proxy with basic authentication
		//	  --proxypassword=PROXY_PASSWORD
		//	                        password for HTTP proxy with basic authentication
		//	  --destination=DESTINATION
		//	                        the destination location of the result; default is
		//	                        /tmp
		//	  --no-archive          data will be in an uncompressed directory
		
		module = "system";
		options.clear();
		options.add("-h, --help");
		options.add("--proxy=PROXY_URL");
		options.add("--proxyuser=PROXY_USER");
		options.add("--proxypassword=PROXY_PASSWORD");
		if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.19.4-1")) options.add("--noproxy=NO_PROXY");		// added by bug 1420533 commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9
		options.add("--destination=DESTINATION");	// https://bugzilla.redhat.com/show_bug.cgi?id=1040338#c2
		options.add("--no-archive");
		options.add("--sos");	// added by Bug 1060727 - rhsm-debug duplicates sos data and may collect secrets
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.12.7-1")) options.add("--no-subscriptions");	// added by Bug 1114117 - rhsm-debug takes forever	// commit 68a1a418c27172c4fb851d536813f8060f4d3d1f
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.14.1-1")) options.add("--subscriptions");		// added by 	1114117: Stop collecting subs info by default.	// commit 029f786999f5b1cd1d9614976fb4544ca6541b3b
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.15.9-12")) {options.remove("--no-subscriptions"); options.remove("--subscriptions");}	// removed by https://bugzilla.redhat.com/show_bug.cgi?id=1246680#c2 	// commit 6bd472d13d88934e3a3069862e26f9e7e27bec8c

		for (String commandHelp : new String[]{command+" "+module+" -h",command+" "+module+" --help"}) {
			List <String> usages = new ArrayList<String>();
			String usage = String.format("Usage: %s %s [OPTIONS]",command,module);
			usages.add(usage);
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("1039653"), commandHelp, 0, usage.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\?", "\\\\?")+" *$", usages}));
			ll.add(Arrays.asList(new Object[] {new BlockedByBzBug("1039653"), commandHelp, 0, optionsRegex, new ArrayList<String>(options)}));
		}
		
		return ll;
	}
}
