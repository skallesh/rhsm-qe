package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;

import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.CertStatistics;
import rhsm.data.ConsumerCert;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.OrderNamespace;
import rhsm.data.ProductCert;
import rhsm.data.ProductNamespace;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * DEV Sprint 76 Demo
 *  Subscription Manager Container Mode (dgoodwin)
 *    Video: https://sas.elluminate.com/p.jnlp?psid=2014-06-11.0638.M.D38450C42DA81F82F8E4981A4E1190.vcr&sid=819
 */
@Test(groups={"DockerTests","Tier3Tests"})
public class DockerTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="Verify that when in container mode, attempts to run subscription-manager are blocked",
			groups={"VerifySubscriptionManagementCommandIsDisabledInContainerMode_Test"/*uncomment after fixed,"blockedbyBug-1114126"*/},
			dataProvider="getSubscriptionManagementCommandData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifySubscriptionManagementCommandIsDisabledInContainerMode_Test(Object bugzilla, String helpCommand) {
		
		SSHCommandResult result = client.runCommandAndWait(helpCommand);
		//TEMPORARY WHILE BUG 1114126 is open
		//Assert.assertEquals(result.getStderr().trim(), clienttasks.msg_ContainerMode, "Stderr from attempting command '"+helpCommand+"' while in container mode.");	
		//Assert.assertEquals(result.getStdout().trim(), "", "Stdout from attempting command '"+helpCommand+"' while in container mode.");	
		Assert.assertEquals(result.getStdout().trim(), clienttasks.msg_ContainerMode, "Stdout from attempting command '"+helpCommand+"' while in container mode.");	
		Assert.assertEquals(result.getStderr().trim(), "", "Stderr from attempting command '"+helpCommand+"' while in container mode.");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from attempting command '"+helpCommand+"' while in container mode.");
	}

	@DataProvider(name="getSubscriptionManagementCommandData")
	public Object[][] getSubscriptionManagementCommandDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getSubscriptionManagementCommandDataAsListOfLists());
	}
	protected List<List<Object>> getSubscriptionManagementCommandDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		Set<String> commands = new HashSet<String>();
		
		for (List<Object> l: HelpTests.getExpectedCommandLineOptionsDataAsListOfLists()) { 
			//Object bugzilla, String helpCommand, Integer exitCode, String stdoutRegex, List<String> expectedOptions
			//BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);
			String helpCommand = (String) l.get(1);
			//Integer exitCode = (Integer) l.get(2);
			//String stdoutRegex = (String) l.get(3);
			//List<String> expectedHelpOptions = (List<String>) l.get(4);
			
			// only process the commands with modules for which --help is an option
			if (!helpCommand.contains("--help")) continue;
				
			// remove the --help option
			String command = helpCommand.replace("--help", "");
			
			// collapse white space and trim
			command = command.replaceAll(" +", " ").trim();
			
			// skip command "subscription-manager"
			if (command.equals(clienttasks.command)) continue;
			
			// skip command "rhsm-debug"
			if (command.equals("rhsm-debug")) continue;
			
			// skip command "rct"
			if (command.startsWith("rct")) continue;
			
			// skip command "rhsm-icon"
			if (command.startsWith("rhsm-icon")) continue;
			
			// skip command "usr/libexec/rhsmd"
			if (command.startsWith("/usr/libexec/rhsmd")) continue;
			
			// skip command "usr/libexec/rhsmcertd-worker"
			if (command.startsWith("/usr/libexec/rhsmcertd-worker")) continue;
			
			// skip duplicate commands
			if (commands.contains(command)) continue; else commands.add(command);
			
			Set<String> bugIds = new HashSet<String>();

			// Bug 1114132 - when in container mode, subscription-manager-gui (and some other tools) should also be disabled
			if (command.contains("subscription-manager-gui"))		bugIds.add("1114132");
			if (command.startsWith("rhn-migrate-classic-to-rhsm"))	bugIds.add("1114132");
			if (command.startsWith("rhsmcertd"))					bugIds.add("1114132");

			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));

			ll.add(Arrays.asList(new Object[]{blockedByBzBug, command}));
		}
		
		return ll;
	}
	
	
	@AfterGroups(groups={"setup"}, value={"VerifySubscriptionManagementCommandIsDisabledInContainerMode_Test"})
	public void teardownContainerMode() {
		if (clienttasks!=null) {
			client.runCommandAndWait("rm -rf "+rhsmHostDir);
		}
	}
	
	@BeforeGroups(groups={"setup"}, value={"VerifySubscriptionManagementCommandIsDisabledInContainerMode_Test"})
	protected void setupContainerMode() {
		if (clienttasks!=null) {
			client.runCommandAndWait("rm -rf "+rhsmHostDir);
			client.runCommandAndWait("mkdir "+rhsmHostDir);
			client.runCommandAndWait("cp -r /etc/rhsm/* "+rhsmHostDir);
			client.runCommandAndWait("rm -rf "+entitlementHostDir);
			client.runCommandAndWait("mkdir "+entitlementHostDir);
			Assert.assertTrue(RemoteFileTasks.testExists(client, rhsmHostDir+"/ca"), "After setting up container mode, directory '"+rhsmHostDir+"/ca"+"' should exist.");
			Assert.assertTrue(RemoteFileTasks.testExists(client, rhsmHostDir+"/rhsm.conf"), "After setting up container mode, file '"+rhsmHostDir+"/rhsm.conf"+"' should exist.");
			Assert.assertTrue(RemoteFileTasks.testExists(client, entitlementHostDir), "After setting up container mode, directory '"+entitlementHostDir+"' should exist.");
		}
	}
	
	protected final String rhsmHostDir = "/etc/rhsm-host";
	protected final String entitlementHostDir = "/etc/pki/entitlement-host";
	
	
	
	
	
	
	
	// Candidates for an automated Test:
	
	
	
	// Configuration methods ***********************************************************************
	@BeforeClass(groups={"setup"})
	public void checkPackageVersionBeforeClass() {
		if (clienttasks!=null) {
			if (clienttasks.isPackageVersion("subscription-manager", "<", "1.12.2-1")) {
				throw new SkipException("Subscription Management compatibility with docker requires subscription-manager-1.12.2-1 or higher.");
			}
		}
	}
	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

	
}
