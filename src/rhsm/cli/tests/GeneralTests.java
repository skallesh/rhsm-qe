package rhsm.cli.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.LinkedItem;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

import org.apache.xmlrpc.XmlRpcException;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductSubscription;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author ssalevan
 * @author jsefler
 *
 */
@Test(groups={"GeneralTests"})
public class GeneralTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20373", "RHEL7-32155"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: attempt to access functionality without registering",
			groups={"Tier2Tests","blockedByBug-749332","blockedByBug-1119688"},
			dataProvider="UnregisteredCommandData")
	@ImplementsNitrateTest(caseId=50215)
	public void testCommandsWithoutBeingRegistered(String command) {
		log.info("Testing subscription-manager-cli command without being registered, expecting it to fail: "+ command);
		clienttasks.unregister(null, null, null, null);
		
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			RemoteFileTasks.runCommandAndAssert(client,command,1,null,"^"+clienttasks.msg_ConsumerNotRegistered);			
		} else if (clienttasks.isPackageVersion("subscription-manager",">=","0.98.4-1")) {					// post commit 6241cd1495b9feac2ed123f60405061b03815721 bug 749332
			RemoteFileTasks.runCommandAndAssert(client,command,255,"^"+clienttasks.msg_ConsumerNotRegistered,null);
		} else {
			RemoteFileTasks.runCommandAndAssert(client,command,1,"^Error: You need to register this system by running `register` command before using this option.",null);
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20380", "RHEL7-61488"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: attempt to access functionality that does not exist",
			groups={"Tier2Tests","blockedByBug-1098308"},
			dataProvider="NegativeFunctionalityData")
	public void testCommandsThatAreInvalid(Object blockedByBug, String command, Integer expectedExitCode, String expectedStdout, String expectedStderr) {
		log.info("Testing subscription-manager-cli command that is invalid, expecting it to fail: "+ command);
		SSHCommandResult result = client.runCommandAndWait(command);
		if (expectedExitCode!=null)	Assert.assertEquals(result.getExitCode(), expectedExitCode, "The expected exit code.");
		if (expectedStdout!=null)	Assert.assertEquals(result.getStdout().trim(), expectedStdout, "The expected stdout message.");
		if (expectedStderr!=null)	Assert.assertEquals(result.getStderr().trim(), expectedStderr, "The expected stderr message.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20293", "RHEL7-32164"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="assert the exit code from service rhsmcertd status when running and stopped",
			groups={"Tier2Tests","blockedByBug-895263","blockedByBug-824680","blockedByBug-842464"})
	public void testExitCodeStatusForRhmscertd() {
		Integer expectedStoppedStatus = new Integer(3);
		Integer expectedRunningStatus = new Integer(0);
		log.info("When service "+clienttasks.rhsmCertD+" is stopped, the expected service status exit code is: "+expectedStoppedStatus);
		log.info("When service "+clienttasks.rhsmCertD+" is running, the expected service status exit code is: "+expectedRunningStatus);
		RemoteFileTasks.runCommandAndAssert(client, "service "+clienttasks.rhsmCertD+" stop  && service "+clienttasks.rhsmCertD+" status", new Integer(3));
		RemoteFileTasks.runCommandAndAssert(client, "service "+clienttasks.rhsmCertD+" start && service "+clienttasks.rhsmCertD+" status", new Integer(0));
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19943", "RHEL7-32162"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="assert the exit code from service rhsmcertd status when running and stopped",
			groups={"Tier1Tests","blockedByBug-913118","blockedByBug-912707","blockedByBug-914113","blockedByBug-1241247","blockedByBug-1395794"})
	protected void testRhsmcertdDoesNotThrowDeprecationWarnings() throws JSONException, Exception {
		clienttasks.unregister(null, null, null, null);
		String marker = System.currentTimeMillis()+" Testing verifyRhsmcertdDoesNotThrowDeprecationWarnings_Test...";
		clienttasks.markSystemLogFile(marker);
		
		String command = clienttasks.rhsmComplianceD+" -s";
		SSHCommandResult result = client.runCommandAndWait(command);
		Integer expectedExitCode = Integer.valueOf(0);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.1-1")) { // subscription-manager commit cb374ec918c7592aaf1f1aed6d5730d931a7ee4e Generate bin scripts via setuptools entry_points
			// exit code 0 was actually a bug that was inadvertently fixed by commit cb374ec918c7592aaf1f1aed6d5730d931a7ee4e
			// expected exitCode comes from RHSM_REGISTRATION_REQUIRED = 5 in cert_sorter.py https://github.com/candlepin/subscription-manager/blob/d17e16065df81c531bd775a67e7f584b53f99637/src/subscription_manager/cert_sorter.py
			expectedExitCode = Integer.valueOf(5);
		}
		Assert.assertEquals(result.getExitCode(), expectedExitCode,"ExitCode from command '"+command+"'.");
		Assert.assertTrue(result.getStdout().isEmpty(),"Stdout from command '"+command+"' is empty.");
		Assert.assertTrue(result.getStderr().isEmpty(),"Stderr from command '"+command+"' is empty.");
		
		String rhsmcertdLogResult = clienttasks.getTailFromSystemLogFile(marker, null).trim();
		String expectedMessage = "In order for Subscription Manager to provide your system with updates, your system must be registered with the Customer Portal. Please enter your Red Hat login to ensure your system is up-to-date.";
		Assert.assertTrue(rhsmcertdLogResult.contains(expectedMessage),"Syslog contains expected message '"+expectedMessage+"'.");
		String unexpectedMessage = "DeprecationWarning";
		Assert.assertTrue(!rhsmcertdLogResult.contains(unexpectedMessage),"Syslog does not contain message '"+unexpectedMessage+"'.");
		
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,null,null,null,null,null, null);
		/*
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("Cannot randomly pick a pool for subscribing when there are no available pools for testing."); 
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribe(null,null,pool.poolId,null,null,null,null,null,null,null,null);
		*/
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		// TODO: I can only seem to reproduce the error after consuming an entitlement from stage - don't know why - maybe it requires an entitlement with a warning period - let's just subscribe to all the pools.
		result = client.runCommandAndWait(command);
		//	[root@rhsm-accept-rhel5 ~]# /usr/libexec/rhsmd -s
		//	/usr/lib64/python2.4/site-packages/rhsm/certificate.py:123: DeprecationWarning: Call to deprecated function: hasNow
		//	  category=DeprecationWarning)
		//	[root@rhsm-accept-rhel5 ~]#
		expectedExitCode = Integer.valueOf(0);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.1-1")) { // subscription-manager commit cb374ec918c7592aaf1f1aed6d5730d931a7ee4e Generate bin scripts via setuptools entry_points
			// from cert_sorter.py
			//	RHSM_VALID = 0
			//	RHSM_EXPIRED = 1
			//	RHSM_WARNING = 2
			//	RHN_CLASSIC = 3
			//	RHSM_PARTIALLY_VALID = 4
			//	RHSM_REGISTRATION_REQUIRED = 5
			// Note: this expectedExitCode will probably work for RHSM_VALID and RHSM_EXPIRED/INVALID
			expectedExitCode = clienttasks.status(null, null, null, null, null).getExitCode();
			
			// Note: if a temporary SKU is being consumed, then RHSM_PARTIALLY_VALID = 4 will be expectedExitCode...
			for (ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
				if (productSubscription.subscriptionType.contains("(Temporary)")) {
					log.info("Since the system is currently consuming at least one temporary subscription, the expected exitCode from '"+command+"' should match RHSM_PARTIALLY_VALID = 4");
					expectedExitCode = new Integer(4);
					break;
				}
			}
			
			// Note (cont'd): TODO may still want to test for RHSM_WARNING = 2 case
			
			// Note (cont'd): but if any installed product is Not Subscribed, then the system is still INVALID and trumps RHSM_PARTIALLY_VALID
			for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()) {
				if (installedProduct.status.equals("Not Subscribed")) {
					log.info("Since the system has at least one installed product with no subscription coverage, the expected exitCode from '"+command+"' should match RHSM_EXPIRED/INVALID = 1");
					expectedExitCode = new Integer(1);
					break;
				}
			}
		}
		Assert.assertEquals(result.getExitCode(), expectedExitCode,"ExitCode from command '"+command+"'.");
		Assert.assertTrue(result.getStdout().isEmpty(),"Stdout from command '"+command+"' is empty.");
		Assert.assertTrue(result.getStderr().isEmpty(),"Stderr from command '"+command+"' is empty.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20391", "RHEL7-32157"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="assert rhsmd is logged to both /var/log/rhsm/rhsm.log and /var/log/messages",
			groups={"Tier2Tests","blockedbyBug-976868","blockedByBug-1395794","testRhsmdForceSignalsToRhsmlogAndSyslog"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testRhsmdForceSignalsToRhsmlogAndSyslog() {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,null,null,null,null,null, null);
		
		Map<String,String> signalMap = new HashMap<String,String>();
		signalMap.put("valid", "");
		signalMap.put("expired", "This system is missing one or more subscriptions. Please run subscription-manager for more information.");
		signalMap.put("warning", "This system's subscriptions are about to expire. Please run subscription-manager for more information.");
		signalMap.put("partial", "This system is missing one or more subscriptions to fully cover its products. Please run subscription-manager for more information.");
		
		for (String signal : signalMap.keySet()) {
			String command = clienttasks.rhsmComplianceD+" -s -d -i -f "+signal;
			String marker = System.currentTimeMillis()+" Testing VerifyRhsmdLogsToRhsmlogAndSyslog_Test...";
			RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, marker);
			clienttasks.markSystemLogFile(marker);

			// run and verify the command result
			String expectedStdout = "forcing status signal from cli arg";
			Integer expectedExitCode = Integer.valueOf(0);
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.1-1")) { // subscription-manager commit cb374ec918c7592aaf1f1aed6d5730d931a7ee4e Generate bin scripts via setuptools entry_points
				// exit code 0 was actually a bug that was inadvertently fixed by commit cb374ec918c7592aaf1f1aed6d5730d931a7ee4e
				if(signal.equals("expired")) expectedExitCode = Integer.valueOf(1);
				if(signal.equals("warning")) expectedExitCode = Integer.valueOf(2);
				if(signal.equals("partial")) expectedExitCode = Integer.valueOf(4);
			}
			RemoteFileTasks.runCommandAndAssert(client, command, expectedExitCode, expectedStdout, null);
			
			// verify the logs
			sleep(2000);	// give the message thread time to be logged
			String logResult;
			if (signalMap.get(signal).isEmpty()) {
				String unExpectedMessage = "Please run subscription-manager for more information.";
				logResult = clienttasks.getTailFromSystemLogFile(marker, null).trim();
				Assert.assertTrue(!logResult.contains(unExpectedMessage),clienttasks.messagesLogFile+" does NOT contain message '"+unExpectedMessage+"'.");
				logResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, marker, null).trim();
				Assert.assertTrue(!logResult.contains(unExpectedMessage),clienttasks.rhsmLogFile+" does NOT contain message '"+unExpectedMessage+"'.");
				Assert.assertTrue(logResult.contains(expectedStdout),clienttasks.rhsmLogFile+" contains expected message '"+expectedStdout+"'.");

			} else {
				String expectedMessage = signalMap.get(signal);
				logResult = clienttasks.getTailFromSystemLogFile(marker, null).trim();
				Assert.assertTrue(logResult.contains(expectedMessage),clienttasks.messagesLogFile+" contains expected message '"+expectedMessage+"'.");
				logResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, marker, null).trim();
				Assert.assertTrue(logResult.contains(expectedMessage),clienttasks.rhsmLogFile+" contains expected message '"+expectedMessage+"'.");
				Assert.assertTrue(logResult.contains(expectedStdout),clienttasks.rhsmLogFile+" contains expected message '"+expectedStdout+"'.");
			}
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20374", "RHEL7-32169"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="assert permissions on /etc/cron.daily/rhsmd",
			groups={"Tier2Tests","blockedbyBug-1012566"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testPermissionsOnEtcCronDailyRhsmd() {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.10.3-1")) throw new SkipException("Installed package '"+clienttasks.installedPackageVersionMap.get("subscription-manager")+"' is blockedByBug https://bugzilla.redhat.com/show_bug.cgi?id=1012566 which is fixed in subscription-manager-1.10.3-1.");
		
		//	[root@jsefler-6 ~]# ls -l /etc/cron.daily/rhsmd 
		//	-rwxr-xr-x. 1 root root 256 Sep 23 14:53 /etc/cron.daily/rhsmd
		
		// Bug 1012566 - /etc/cron.daily/rhsmd breaks rule GEN003080 in Red Hat security guide
		// It should have permissions 0700.
		// http://people.redhat.com/sgrubb/files/stig-2011/stig-2011-checklist.html#item-SV-978r7_rule
		
		//	[root@jsefler-5 ~]# ls -l /etc/cron.daily/rhsmd
		//	-rwx------ 1 root root 256 Apr 28 11:55 /etc/cron.daily/rhsmd
		//
		//	[root@jsefler-6 ~]# ls -l /etc/cron.daily/rhsmd
		//	-rwx------. 1 root root 256 Oct 16  2013 /etc/cron.daily/rhsmd
		//
		//	[root@jsefler-7 ~]# ls -l /etc/cron.daily/rhsmd
		//	-rwx------. 1 root root 256 Mar 25 13:19 /etc/cron.daily/rhsmd
		
		File cronDailyFile = new File("/etc/cron.daily/rhsmd");
		RemoteFileTasks.runCommandAndAssert(client, "ls -l "+cronDailyFile, Integer.valueOf(0), "-rwx------\\.? 1 root root .* "+cronDailyFile+"\\n", null);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20384", "RHEL7-32171"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify enablement of yum plugin for subscription-manager in /etc/yum/pluginconf.d/subscription-manager.conf ",
			groups={"Tier2Tests","VerifyYumPluginForSubscriptionManagerEnablement_Test", "blockedByBug-1017354","blockedByBug-1087620"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testYumPluginForSubscriptionManagerEnablement() {
		SSHCommandResult sshCommandResult;
		String stdoutRegex,stdout;
		
		// test /etc/yum/pluginconf.d/subscription-manager.conf enabled=1 and enabled=0 (UNREGISTERED)
		clienttasks.unregister(null, null, null, null);
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled", "1");
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled", "1");

		sshCommandResult = client.runCommandAndWait("yum repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		// Observed results
		//	Loaded plugins: product-id, refresh-packagekit, security, subscription-manager
		//
		//	Loaded plugins: langpacks, product-id, search-disabled-repos, subscription-
		//                : manager
		//
		//	Loaded plugins: product-id, refresh-packagekit, search-disabled-repos, security,
		//                : subscription-manager
		stdout = sshCommandResult.getStdout();	// (first observed result)
		stdout = stdout.replaceAll("-\\n\\s+:\\s", "-");	// join multiple lines of Loaded plugins (second observed result)
		stdout = stdout.replaceAll(",\\n\\s+:\\s", ", ");	// join multiple lines of Loaded plugins (third observed result)
		stdoutRegex = "Loaded plugins:.* subscription-manager";
		Assert.assertTrue(doesStringContainMatches(stdout, stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForSubscriptionManager+" enabled=1 appears to have plugin subscription-manager loaded.");
		stdoutRegex = "Loaded plugins:.* product-id";
		Assert.assertTrue(doesStringContainMatches(stdout, stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForProductId+" enabled=1 appears to have plugin product-id loaded.");
			
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled", "0");
		sshCommandResult = client.runCommandAndWait("yum repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		//	Loaded plugins: product-id, refresh-packagekit, security
		stdout = sshCommandResult.getStdout();	// (first observed result)
		stdout = stdout.replaceAll("-\\n\\s+:\\s", "-");	// join multiple lines of Loaded plugins (second observed result)
		stdout = stdout.replaceAll(",\\n\\s+:\\s", ", ");	// join multiple lines of Loaded plugins (third observed result)
		stdoutRegex = "Loaded plugins:.* subscription-manager";
		Assert.assertTrue(!doesStringContainMatches(stdout, stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForSubscriptionManager+" enabled=0 does NOT contain expected regex '"+stdoutRegex+"'.");
		stdoutRegex = "Loaded plugins:.* product-id";
		Assert.assertTrue(doesStringContainMatches(stdout, stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForProductId+" enabled=1 contains expected regex '"+stdoutRegex+"'.");
		
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled", "0");
		sshCommandResult = client.runCommandAndWait("yum repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		//	Loaded plugins: product-id, refresh-packagekit, security
		stdout = sshCommandResult.getStdout();	// (first observed result)
		stdout = stdout.replaceAll("-\\n\\s+:\\s", "-");	// join multiple lines of Loaded plugins (second observed result)
		stdout = stdout.replaceAll(",\\n\\s+:\\s", ", ");	// join multiple lines of Loaded plugins (third observed result)
		stdoutRegex = "Loaded plugins:.* subscription-manager";
		Assert.assertTrue(!doesStringContainMatches(stdout, stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForSubscriptionManager+" enabled=0 does NOT contain expected regex '"+stdoutRegex+"'.");
		stdoutRegex = "Loaded plugins:.* product-id";
		Assert.assertTrue(!doesStringContainMatches(stdout, stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForProductId+" enabled=0 does NOT contain expected regex '"+stdoutRegex+"'.");
		
		sshCommandResult = client.runCommandAndWait("yum repolist --disableplugin=rhnplugin --enableplugin=subscription-manager --enableplugin=product-id"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		//	Loaded plugins: product-id, refresh-packagekit, security, subscription-manager
		stdout = sshCommandResult.getStdout();	// (first observed result)
		stdout = stdout.replaceAll("-\\n\\s+:\\s", "-");	// join multiple lines of Loaded plugins (second observed result)
		stdout = stdout.replaceAll(",\\n\\s+:\\s", ", ");	// join multiple lines of Loaded plugins (third observed result)
		stdoutRegex = "Loaded plugins:.* subscription-manager";
		Assert.assertTrue(doesStringContainMatches(stdout, stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForSubscriptionManager+" enabled=0 (but--enableplugin=subscription-manager) contains expected stdout regex '"+stdoutRegex+"'.");
		stdoutRegex = "Loaded plugins:.* product-id";
		Assert.assertTrue(doesStringContainMatches(stdout, stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForProductId+" enabled=0 (but --enableplugin=product-id) contains expected stdout regex '"+stdoutRegex+"'.");
	}
	@AfterGroups(value="VerifyYumPluginForSubscriptionManagerEnablement_Test", alwaysRun=true)
	protected void afterVerifyYumPluginForSubscriptionManagerEnablement_Test() {
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled", "1");
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled", "1");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36512", "RHEL7-59018"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="check the rpm requires list for changes to python-rhsm-certificates",
			groups={"Tier2Tests","blockedByBug-1104332"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testRpmRequireListForPythonRhsmCertificates() {
		String pkg = "python-rhsm-certificates";
		if (!clienttasks.isPackageInstalled(pkg)) throw new SkipException("This test require that package '"+pkg+"' be installed.");
		String rpmCommand = "rpm --query --requires "+pkg+" --verbose";
		if (Integer.valueOf(clienttasks.redhatReleaseX) == 5) rpmCommand += " | egrep -v '\\(.*\\)'";
		if (Integer.valueOf(clienttasks.redhatReleaseX) > 5) rpmCommand += " | egrep -v '(^auto:|^rpmlib:)'";	// exclude auto: and rpmlib: dependencies
		SSHCommandResult sshCommandResult = client.runCommandAndWait(rpmCommand);
		
		List<String> actualRequiresList = new ArrayList<String>();
		for (String requires : Arrays.asList(sshCommandResult.getStdout().trim().split("\\n"))) {
			if (!requires.trim().isEmpty()) actualRequiresList.add(requires.trim());
		}
		
		List<String> expectedRequiresList = new ArrayList<String>();
		if (clienttasks.redhatReleaseX.equals("6") || clienttasks.redhatReleaseX.equals("7")) {
			// none! there are no dependencies for package python-rhsm-certificates
		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20301", "RHEL7-32161"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="check the rpm requires list for changes to python-rhsm",
			groups={"Tier2Tests","blockedByBug-1006748","blockedByBug-800732","blockedByBug-1096676"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testRpmRequireListForPythonRhsm() {
		String pkg = "python-rhsm";
		if (!clienttasks.isPackageInstalled(pkg)) throw new SkipException("This test require that package '"+pkg+"' be installed.");
		String rpmCommand = "rpm --query --requires "+pkg+" --verbose";
		if (Integer.valueOf(clienttasks.redhatReleaseX) == 5) rpmCommand += " | egrep -v '\\(.*\\)'";
		if (Integer.valueOf(clienttasks.redhatReleaseX) > 5) rpmCommand += " | egrep -v '(^auto:|^rpmlib:)'";
		SSHCommandResult sshCommandResult = client.runCommandAndWait(rpmCommand);
		
		List<String> actualRequiresList = new ArrayList<String>();
		for (String requires : Arrays.asList(sshCommandResult.getStdout().trim().split("\\n"))) {
			if (!requires.trim().isEmpty()) actualRequiresList.add(requires.trim());
		}

		List<String> expectedRequiresList = new ArrayList<String>();
		if (clienttasks.redhatReleaseX.equals("5")) { 
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"m2crypto",
					"python-iniparse",
					"python-simplejson",
					"rpm-python",
			}));
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.5-1")) expectedRequiresList.remove("python-simplejson");	// Bug 1006748 - remove subscription-manager dependency on python-simplejson; subscription-manager commit ee34aef839d0cb367e558f1cd7559590d95cd636
			if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.11.3-3")) expectedRequiresList.add("python-simplejson");	// Bug 1096676 - missing dependency on json; python-rhsm commit 19b9b55404c5a9bf4eb2828692f8a578a7645da1
			if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.11.3-4")) expectedRequiresList.add("python-dateutil");	// Bug 1090350 - Clock skew detected when the dates of server and client have no big time drift. commit 4c7fe4009a7902c236ff8f9445a2505bf0eb94e7
			if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.20.1-1")) expectedRequiresList.add("python-six");	// python-rhsm commit ca7e5d4650b2dabed9fc8daf9c8c7e8c2e2bfb7e	// Require the 'six' Python 2 and 3 compatibility library.
//			for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
//			Assert.assertTrue(actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' contains the expected list "+expectedRequiresList);
//			return;
		}
		if (clienttasks.redhatReleaseX.equals("6") || clienttasks.redhatReleaseX.equals("7")) {
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"manual: m2crypto",
					"manual: python-iniparse",
					"manual: python-simplejson",
					"manual: rpm-python"
			}));
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.5-1")) expectedRequiresList.remove("manual: python-simplejson");	// Bug 1006748 - remove subscription-manager dependency on python-simplejson; subscription-manager commit ee34aef839d0cb367e558f1cd7559590d95cd636
			if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.11.5-1")) expectedRequiresList.add("manual: python-dateutil");	// Bug 1090350 - Clock skew detected when the dates of server and client have no big time drift. commit b597dae53aacf2d8a307b77b7f38756ce3ee6860
			if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.17.5-1")) expectedRequiresList.add("manual: python-rhsm-certificates = "+clienttasks.installedPackageVersionMap.get("python-rhsm").replace("python-rhsm-", "").replaceFirst("\\."+clienttasks.arch, ""));	// Bug 1104332 - [RFE] Separate out the rhsm certs into a separate RPM	// python-rhsm commit 790aa1ddaa20db05c63019fcdd4bd7f5cd2adeb8	// manual: python-rhsm-certificates = 1.17.4-1.git.1.790aa1d.el7
			if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.20.1-1")) expectedRequiresList.add("manual: python-six");	// python-rhsm commit ca7e5d4650b2dabed9fc8daf9c8c7e8c2e2bfb7e	// Require the 'six' Python 2 and 3 compatibility library.
			
			if (clienttasks.redhatReleaseX.equals("7")) {
				if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.18.5-1")) expectedRequiresList.remove("manual: m2crypto");	// python-rhsm commit 214103dcffce29e31858ffee414d79c1b8063970	// Reduce usage of m2crypto (#184) on RHEL7+
			}
		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20299", "RHEL7-32159"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="check the rpm requires list for changes to subscription-manager",
			groups={"Tier2Tests","blockedbyBug-801280","blockedByBug-1006748","blockedByBug-800744","blockedByBug-1080531","blockedByBug-850331"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testRpmRequireListForSubscriptionManager() {
		String pkg = "subscription-manager";
		if (!clienttasks.isPackageInstalled(pkg)) throw new SkipException("This test require that package '"+pkg+"' be installed.");
		String rpmCommand = "rpm --query --requires "+pkg+" --verbose";
		if (Integer.valueOf(clienttasks.redhatReleaseX) == 5) rpmCommand += " | egrep -v '\\(.*\\)'";
		if (Integer.valueOf(clienttasks.redhatReleaseX) > 5) rpmCommand += " | egrep -v '(^auto:|^rpmlib:)'";
		SSHCommandResult sshCommandResult = client.runCommandAndWait(rpmCommand);
		
		List<String> actualRequiresList = new ArrayList<String>();
		for (String requires : Arrays.asList(sshCommandResult.getStdout().trim().split("\\n"))) {
			if (!requires.trim().isEmpty()) actualRequiresList.add(requires.trim());
		}
		
		List<String> expectedRequiresList = new ArrayList<String>();
		if (clienttasks.redhatReleaseX.equals("5")) { 
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"/bin/bash",
					"/usr/bin/python",
					//TODO figure out how to keep this: "config(subscription-manager) = "+clienttasks.installedPackageVersionMap.get("subscription-manager").replace("subscription-manager-", "").replaceFirst("\\."+clienttasks.arch, ""),	// "manual: config(subscription-manager) = 1.9.11-1.el6",
					"/bin/sh",
					"/bin/sh",
					"/bin/sh",
					"chkconfig",
					"chkconfig",
					"dbus-python",
					"initscripts",
					"pygobject2",
					"python-dateutil",
					"python-dmidecode",
					"python-ethtool",
					"python-iniparse",
					"python-simplejson",	// removed by bug 1006748
					"usermode",
					"virt-what",
					"yum >= 3.2.19-15"
			}));
			if		(clienttasks.isPackageVersion("subscription-manager",">=","1.10.5-1"))	expectedRequiresList.remove("python-simplejson");		// Bug 1006748 - remove subscription-manager dependency on python-simplejson; subscription-manager commit ee34aef839d0cb367e558f1cd7559590d95cd636
//			for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
//			Assert.assertTrue(actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' contains the expected list "+expectedRequiresList);
//			return;
		}
		if (clienttasks.redhatReleaseX.equals("6")) {
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"manual: config(subscription-manager) = "+clienttasks.installedPackageVersionMap.get("subscription-manager").replace("subscription-manager-", "").replaceFirst("\\."+clienttasks.arch, ""),	// "manual: config(subscription-manager) = 1.9.11-1.el6",
					"post: /bin/sh",
					"preun: /bin/sh",
					"postun: /bin/sh",
					"post: chkconfig",
					"preun: chkconfig",
					"manual: dbus-python",
					"preun: initscripts",
					"manual: pygobject2",
					"manual: python-dateutil",
					"manual: python-dmidecode",
					"manual: python-ethtool",
					"manual: python-iniparse",
					"manual: python-simplejson",	// removed by bug 1006748
					"manual: usermode",
					"manual: virt-what",
					"manual: yum >= 3.2.19-15"
			}));
			
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.16.8-1")) {	// commit 88f7183a7f0ab2955995e238cc221af5f4eadc3e	1282961: Update yum version to current RHEL 6.8 one
				expectedRequiresList.remove("manual: yum >= 3.2.19-15");
				expectedRequiresList.add("manual: yum >= 3.2.29-73");
			}
			
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.5-1"))	expectedRequiresList.remove("manual: python-simplejson");		// Bug 1006748 - remove subscription-manager dependency on python-simplejson; subscription-manager commit ee34aef839d0cb367e558f1cd7559590d95cd636
			
		}
		if (clienttasks.redhatReleaseX.equals("7")) {
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"post: systemd",	//"post: systemd-units",	// changed for rhel7 by commit f67310381587a96a37933abf22985b97de373887  Bug 850331 - Introduce new systemd-rpm macros in subscription-manager spec file 
					"preun: systemd",	//"preun: systemd-units",	// changed for rhel7 by commit f67310381587a96a37933abf22985b97de373887  Bug 850331 - Introduce new systemd-rpm macros in subscription-manager spec file 
					"postun: systemd",	//"postun: systemd-units",	// changed for rhel7 by commit f67310381587a96a37933abf22985b97de373887  Bug 850331 - Introduce new systemd-rpm macros in subscription-manager spec file 
					"config: config(subscription-manager) = "+clienttasks.installedPackageVersionMap.get("subscription-manager").replace("subscription-manager-", "").replaceFirst("\\."+clienttasks.arch, ""),	//"config: config(subscription-manager) = 1.10.14-7.el7
					"post,interp: /bin/sh",
					"preun,interp: /bin/sh",
					"postun,interp: /bin/sh",
					//"post: chkconfig",	// removed for rhel7 by commit f67310381587a96a37933abf22985b97de373887
					//"preun: chkconfig",	// removed for rhel7 by commit f67310381587a96a37933abf22985b97de373887
					"manual: dbus-python",
					//"preun: initscripts",	// removed for rhel7 by commit f67310381587a96a37933abf22985b97de373887
					"manual: pygobject2",
					"manual: python-dateutil",
					"manual: python-dmidecode",
					"manual: python-ethtool",
					"manual: python-iniparse",
					"manual: python-simplejson",	// removed by bug 1006748
					"manual: usermode",
					"manual: virt-what",
					"manual: yum >= 3.2.19-15"
			}));
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.5-1")) {	// Bug 1006748 - remove subscription-manager dependency on python-simplejson; subscription-manager commit ee34aef839d0cb367e558f1cd7559590d95cd636
				expectedRequiresList.remove("manual: python-simplejson");
			}
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.15.1-1")) {	// commit d30e416b199d9d4d6e22b2ad4cc6515cfcb2069d
				expectedRequiresList.remove("manual: pygobject2");
				expectedRequiresList.add("manual: gobject-introspection");
				expectedRequiresList.add("manual: pygobject3-base");
			}
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.16.8-1")) {	// commit 88f7183a7f0ab2955995e238cc221af5f4eadc3e	1282961: Update yum version to current RHEL 6.8 one
				expectedRequiresList.remove("manual: yum >= 3.2.19-15");
				expectedRequiresList.add("manual: yum >= 3.2.29-73");
			}
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.20.2-1"/*TODO change to 1.20.3-1*/)) {	// commit db7f92dd2a29071eddd3b8de5beedb0fe46352f9	1477958: Use inotify for checking changes of consumer certs
				expectedRequiresList.add("manual: python-inotify");
			}
		
		}
	
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.18.5-1")) {	// commit bb47b2a6b4f3e823240e5f882bd4dc4d57c3b36e	1395794: Include python-decorator as a required dependency
			expectedRequiresList.add("manual: python-decorator");
		}
		
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.19.0-1")) {	// commit 2aa48ef65ec9c98f395abb114285135512325fe3	Provide DBus objects for configuration, facts, and registration
			expectedRequiresList.add("manual: dbus-x11");
		}
		
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.19.2-1")) {	// commit f5eab0e3492469ff4fc01ba19db9e61acfe0bad4	Add missing Requires and BuildRequires needed by F25.
			expectedRequiresList.add("manual: dbus-glib");
		}
		
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.19.11-1")) {	// commit 88e3fdde1417e24c07d0c0b5a56b34ba6f904166  Bug 1446638: Remove dbus-x11 dependency
			expectedRequiresList.remove("manual: dbus-x11");
		}
		
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.20.1-1")) {	// commit ca7e5d4650b2dabed9fc8daf9c8c7e8c2e2bfb7e	Require the 'six' Python 2 and 3 compatibility library.
			expectedRequiresList.add("manual: python-six");
		}
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.20.1-1")) {	// commit f20e28ab12b070095c4045aeecab2ccc9eba31b1	Add preliminary zypper support
			expectedRequiresList.remove("manual: dbus-glib");
		}
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.20.1-1")) {	// commit 21d9b5a6b7b3168046bc498e3db2f0469bb54fc2	Simplify subscription-manager spec file
			expectedRequiresList.add("manual: chkconfig");
			
		}
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-3")) {	// commit 24ae5cd38e36a4ccfb6a3e9daa5f7c8a7f01c324	  1533905: Remove dependency on yum and chkconfig.  
			expectedRequiresList.remove("manual: chkconfig");

		}
		
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-3"/*TODO CHANGE TO 1.21.2-4 WHEN IT IS TAGGED*/)) {	// commit c63a48b81531111cb9fccf69d46e70bb26c2f44e	 1458159: Require latest version of python-dmidecode 
			expectedRequiresList.remove("manual: python-dmidecode");
			expectedRequiresList.add("manual: python-dmidecode >= 3.12.2-2");

		}
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-3")) {	// commit 78da50088f92165fabea0d1a1445baa4d288aac4	 1537473: Subman rpm requires python-setuptools 
			expectedRequiresList.add("manual: python-setuptools");
		}
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-3")) {	// commit 4d2c94e26872863a484a0d37e181154688d350d3	  1547354: Add missing requires for python-kitchen  
			expectedRequiresList.add("manual: python-kitchen");
		}
		// add the expected version of python-rhsm
		if 		(clienttasks.isPackageVersion("subscription-manager",">=","1.20.3-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"subscription-manager-rhsm = "+clienttasks.installedPackageVersionMap.get("subscription-manager").replace("subscription-manager-", "").replaceFirst("-\\d+\\..+", "")/*strips off -1.git.17.485ba1e.el7.x86_64*/);	// commit f445b6486a962d12185a5afe69e768d0a605e175	Move python-rhsm build into subscription-manager
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.20.2-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.20.2");		// RHEL7.5	// commit 00c1100b1fb0cb207be94f95892cfa5c9a9fbfae
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.20.0-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.20.0");		// RHEL7.5	// commit c2383f6cc1745d4d22f83b836bc64b2cd1423b32
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.19.18-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.19.9");		// RHEL7.4	// commit 2ad6cb20a37c7904b67cb8405663ea987c3e50df
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.19.17-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.19.8");		// RHEL7.4	// commit 186a9c1a56fa1115d2eae67f903de6fe9a0e3783
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.19.16-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.19.7");		// RHEL7.4	// commit bea402362f4799189910dc336fe3d3bfd16b4fd2
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.19.12-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.19.6");		// RHEL7.4	// commit bc41af9a25ee39075f3100577ebd2f9cff487048
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.19.8-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.19.5");		// RHEL7.4	// commit 17108bd2e207358d2f7970d0924b51a0c5bb2dc5
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.19.6-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.19.4");		// RHEL7.4	// commit 5164b07d478aa2349b57cbad884f4b18d0203c32
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.19.4-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.19.2");		// RHEL7.4	// commit a40f97e7cc5c5a660e5a25cca417e534d75f0edd
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.19.0-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.19.0");		// RHEL7.4	// commit 3cffd6948f939966774f39c9e79fb3c6b09df61a
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.18.2-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.18.1");		// RHEL6.9	// commit 82f1e7c89a8729ac2c4843922f14921a20f26beb
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.18.1-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.18.0");		// RHEL6.9	// commit 0415e0d3ca2be57253bd79b4a9dc8b5863ca5110
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.17.1-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.17.0");		// RHEL7.3	// commit 18d6aa6889b701288f66c14b2f313f04069aa753
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.15.1-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.15.0");		// RHEL7.2	// commit a2a4794d9eb7b8d74b0eb4bd27d0b6974b87d716
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.16.0-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.16.0");		// RHEL6.8	// commit c52630da1d45aee68c122d39fe92607e9a38ff8e
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.14.3-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.14.2");		// RHEL6.7	// commit 26b7eb90519c5d7f696869344610d49c42dfd918
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.13.13-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.13.10");		// RHEL7.1	// commit 649f5f7a814e05374b5c0ba56f29a59f4925f7ff Use custom JSON encoding function to encode sets.
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.13.6-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.13.5");		// RHEL7.1
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.12.3-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.12.3");		// RHEL7.1
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.11.3-10"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.11.3-5");		// RHEL5.11	subscription-manager commit 0a0135def87aa2a9c44658b31c705e33247b0560	1104498: Add hack to avoid dateutil from anaconda
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.11.3-7"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.11.3-4");		// RHEL5.11
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.11.3-6"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.11.3-3");		// RHEL5.11
		else if (clienttasks.isPackageVersion("subscription-manager",">=","1.11.3-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.11.3-2");		// RHEL5.11
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.10.14-6"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.10.12-2");	// RHEL7.0	// Bug 1080531 - subscription-manager-1.10.14-6 should require python-rhsm >= 1.10.12-2
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.10.9-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.10.9");		// RHEL7.0
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.9.2-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.9.1-1");		// RHEL6.5
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.8.12-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.8.13-1");		// RHEL6.4
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.8.22-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.8.16-1");		// RHEL5.10
		else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.0.13-1"))	expectedRequiresList.add((clienttasks.redhatReleaseX.equals("5")?"":"manual: ")+"python-rhsm >= 1.0.5");		// RHEL5.9
		
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-51164", "RHEL-133334"}, //importReady=false,
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="check the rpm requires list for changes to rhsm-gtk",
			groups={"Tier2Tests"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testRpmRequireListForRhsmGtk() {
		String pkg = "rhsm-gtk";
		if (!clienttasks.isPackageInstalled(pkg)) throw new SkipException("This test require that package '"+pkg+"' be installed.");
		String rpmCommand = "rpm --query --requires "+pkg+" --verbose";
		if (Integer.valueOf(clienttasks.redhatReleaseX) == 5) rpmCommand += " | egrep -v '\\(.*\\)'";
		if (Integer.valueOf(clienttasks.redhatReleaseX) > 5) rpmCommand += " | egrep -v '(^auto:|^rpmlib:)'";
		SSHCommandResult sshCommandResult = client.runCommandAndWait(rpmCommand);
		
		List<String> actualRequiresList = new ArrayList<String>();
		for (String requires : Arrays.asList(sshCommandResult.getStdout().trim().split("\\n"))) {
			if (!requires.trim().isEmpty()) actualRequiresList.add(requires.trim());
		}
		
		List<String> expectedRequiresList = new ArrayList<String>();
		
		if (clienttasks.redhatReleaseX.equals("7")) {
			// rpm --query --requires rhsm-gtk --verbose | egrep -v '(^auto:|^rpmlib:)'
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"manual: font(cantarell)",
					"manual: gtk3",
					"manual: pygobject3",
					"manual: librsvg2("+clienttasks.arch.replace("_","-")+")",	//"manual: librsvg2(x86-64)",
					"post: scrollkeeper",
					"postun: scrollkeeper",
					"manual: usermode-gtk"
			}));
		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}
	
	
	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20376", "RHEL7-32168"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="check the rpm requires list for changes to subscription-manager-gui",
			groups={"Tier2Tests","blockedbyBug-1004908"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testRpmRequireListForSubscriptionManagerGui() {
		String pkg = "subscription-manager-gui";
		if (!clienttasks.isPackageInstalled(pkg)) throw new SkipException("This test require that package '"+pkg+"' be installed.");
		String rpmCommand = "rpm --query --requires "+pkg+" --verbose";
		if (Integer.valueOf(clienttasks.redhatReleaseX) == 5) rpmCommand += " | egrep -v '\\(.*\\)'";
		if (Integer.valueOf(clienttasks.redhatReleaseX) > 5) rpmCommand += " | egrep -v '(^auto:|^rpmlib:)'";
		SSHCommandResult sshCommandResult = client.runCommandAndWait(rpmCommand);
		
		List<String> actualRequiresList = new ArrayList<String>();
		for (String requires : Arrays.asList(sshCommandResult.getStdout().trim().split("\\n"))) {
			if (!requires.trim().isEmpty()) actualRequiresList.add(requires.trim());
		}
		
		List<String> expectedRequiresList = new ArrayList<String>();
		if (clienttasks.redhatReleaseX.equals("5")) {
			// rpm --query --requires subscription-manager-gui --verbose | egrep -v '\(.*\)''
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"subscription-manager = "+clienttasks.installedPackageVersionMap.get("subscription-manager").replace("subscription-manager-", "").replaceFirst("\\."+clienttasks.arch, ""),	//"manual: subscription-manager = 1.9.11-1.el6",
					"librsvg2",
					"/bin/sh",
					"/bin/sh",
					"/bin/sh",
					"/usr/bin/python",
					"dbus-x11",
					"gnome-icon-theme",	// added by Bug 995121 - GUI: calendar icon on s390x and ppc64 machines is not displayed
					"gnome-python2",
					"gnome-python2-canvas",
					"pygtk2",
					"pygtk2-libglade",
					"scrollkeeper",
					"scrollkeeper",
					"usermode-gtk"
			}));
			for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		}
		if (clienttasks.redhatReleaseX.equals("6")) {
			// rpm --query --requires subscription-manager-gui --verbose | egrep -v '(^auto:|^rpmlib:)'
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"manual: subscription-manager = "+clienttasks.installedPackageVersionMap.get("subscription-manager").replace("subscription-manager-", "").replaceFirst("\\."+clienttasks.arch, ""),	//"manual: subscription-manager = 1.9.11-1.el6",
					"manual: librsvg2("+clienttasks.arch.replace("_","-")+")",	//"manual: librsvg2(x86-64)",
					"interp: /bin/sh",
					"post: /bin/sh",
					"postun: /bin/sh",
					"manual: dbus-x11",
					"manual: gnome-icon-theme",	// added by Bug 995121 - GUI: calendar icon on s390x and ppc64 machines is not displayed
					"manual: gnome-python2",
					"manual: gnome-python2-canvas",
					"manual: pygtk2",
					"manual: pygtk2-libglade",
					"post: scrollkeeper",
					"postun: scrollkeeper",
					"manual: usermode-gtk"
			}));
			if (clienttasks.isPackageVersion("subscription-manager-gui",">=","1.19.9-1")) {	// commit 971bb300b1e07d0284c23b1b292a70c674b0037a 1441698: Install missing rpm package with fonts.
				expectedRequiresList.add("manual: dejavu-sans-fonts");	// when gtk3 is NOT installed (yet gtk2 is installed)
			}
		}
		if (clienttasks.redhatReleaseX.equals("7")) {
			// rpm --query --requires subscription-manager-gui --verbose | egrep -v '(^auto:|^rpmlib:)'
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"manual: subscription-manager = "+clienttasks.installedPackageVersionMap.get("subscription-manager").replace("subscription-manager-", "").replaceFirst("\\."+clienttasks.arch, ""),	//"manual: subscription-manager = 1.9.11-1.el6",
					"manual: librsvg2("+clienttasks.arch.replace("_","-")+")",	//"manual: librsvg2(x86-64)",
					"interp,posttrans: /bin/sh",
					"post,interp: /bin/sh",
					"postun,interp: /bin/sh",
					"manual: dbus-x11",
					"manual: gnome-icon-theme",	// added by Bug 995121 - GUI: calendar icon on s390x and ppc64 machines is not displayed
					"manual: gnome-python2",
					"manual: gnome-python2-canvas",
					"manual: pygtk2",
					"manual: pygtk2-libglade",
					"post: scrollkeeper",
					"postun: scrollkeeper",
					"manual: usermode-gtk"
			}));
			if (clienttasks.isPackageVersion("subscription-manager-gui",">=","1.15.1-1")) {	// commit d30e416b199d9d4d6e22b2ad4cc6515cfcb2069d
				expectedRequiresList.remove("manual: pygtk2");
				expectedRequiresList.remove("manual: pygtk2-libglade");
				expectedRequiresList.add("manual: gtk3");
				expectedRequiresList.add("manual: pygobject3");
			}
			if (clienttasks.isPackageVersion("subscription-manager-gui",">=","1.19.9-1")) {	// commit 971bb300b1e07d0284c23b1b292a70c674b0037a 1441698: Install missing rpm package with fonts.
				expectedRequiresList.add("manual: abattis-cantarell-fonts");	// when gtk3 is installed
			}
			if (clienttasks.isPackageVersion("subscription-manager-gui",">=","1.20.1-1")) {	// commit f20e28ab12b070095c4045aeecab2ccc9eba31b1 	// Add preliminary zypper support
				expectedRequiresList.remove("manual: abattis-cantarell-fonts");
				expectedRequiresList.add("manual: font(cantarell)");
			}
			if (clienttasks.isPackageVersion("subscription-manager-gui",">=","1.21.4-1")) {	// commit 23b5409c76d586c4e34440788d612e7ed65e2df6  Stop building subscription-manager-gui, when Python 3 is used
				// let's start over with the introduction of rhsm-gtk
				expectedRequiresList.clear();
				expectedRequiresList.addAll(Arrays.asList(new String[]{
						"manual: rhsm-gtk = "+clienttasks.installedPackageVersionMap.get("rhsm-gtk").replace("rhsm-gtk-", "").replaceFirst("\\."+clienttasks.arch, ""),	//"manual: rhsm-gtk = 1.9.11-1.el6",
						"manual: subscription-manager = "+clienttasks.installedPackageVersionMap.get("subscription-manager").replace("subscription-manager-", "").replaceFirst("\\."+clienttasks.arch, ""),	//"manual: subscription-manager = 1.9.11-1.el6",
						"interp,posttrans: /bin/sh",
						"post,interp: /bin/sh",
						"postun,interp: /bin/sh",
						"manual: gnome-icon-theme"	// added by Bug 995121 - GUI: calendar icon on s390x and ppc64 machines is not displayed
				}));
			}
		}
		if (clienttasks.isPackageVersion("subscription-manager-gui",">=","1.14.8-1")) {		// commit dc727c4adef8cdc49e319f2d90738e848061da78  Adrian says that these imports were never used
			expectedRequiresList.remove("manual: gnome-python2");
			expectedRequiresList.remove("manual: gnome-python2-canvas");
			expectedRequiresList.remove("gnome-python2");
			expectedRequiresList.remove("gnome-python2-canvas");
		}
		if (clienttasks.isPackageVersion("subscription-manager-gui",">=","1.19.11-1")) {	// commit 88e3fdde1417e24c07d0c0b5a56b34ba6f904166  Bug 1446638: Remove dbus-x11 dependency
			expectedRequiresList.remove("manual: dbus-x11");
		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20393", "RHEL7-32175"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="check the rpm requires list for changes to subscription-manager-firstboot",
			groups={"Tier2Tests","blockedbyBug-1004908"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testRpmRequireListForSubscriptionManagerFirstboot() {
		String pkg = "subscription-manager-firstboot";
		if (!clienttasks.isPackageInstalled(pkg)) throw new SkipException("This test requires that package '"+pkg+"' be installed.");
		if (clienttasks.installedPackageVersionMap.get("subscription-manager-gui")==null) clienttasks.isPackageVersion("subscription-manager-gui","==","0.0");	// will populate clienttasks.installedPackageVersionMap.get("subscription-manager-gui")
		String rpmCommand = "rpm --query --requires "+pkg+" --verbose";
		if (Integer.valueOf(clienttasks.redhatReleaseX) == 5) rpmCommand += " | egrep -v '\\(.*\\)'";
		if (Integer.valueOf(clienttasks.redhatReleaseX) > 5) rpmCommand += " | egrep -v '(^auto:|^rpmlib:)'";
		SSHCommandResult sshCommandResult = client.runCommandAndWait(rpmCommand);
		
		List<String> actualRequiresList = new ArrayList<String>();
		for (String requires : Arrays.asList(sshCommandResult.getStdout().trim().split("\\n"))) {
			if (!requires.trim().isEmpty()) actualRequiresList.add(requires.trim());
		}
		
		List<String> expectedRequiresList = new ArrayList<String>();
		if (clienttasks.redhatReleaseX.equals("5")) { 
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"subscription-manager-gui = "+clienttasks.installedPackageVersionMap.get("subscription-manager-gui").replace("subscription-manager-gui-", "").replaceFirst("\\."+clienttasks.arch, ""),	//"manual: subscription-manager-gui = 1.9.11-1.el6",
					"librsvg2",
					"rhn-setup-gnome"
			}));
		}
		if (clienttasks.redhatReleaseX.equals("6")) {
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"manual: subscription-manager-gui = "+clienttasks.installedPackageVersionMap.get("subscription-manager-gui").replace("subscription-manager-gui-", "").replaceFirst("\\."+clienttasks.arch, ""),	//"manual: subscription-manager-gui = 1.9.11-1.el6",
					"manual: librsvg2",
					"manual: rhn-setup-gnome"
			}));
		}
		if (clienttasks.redhatReleaseX.equals("7")) {
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"manual: subscription-manager-gui = "+clienttasks.installedPackageVersionMap.get("subscription-manager-gui").replace("subscription-manager-gui-", "").replaceFirst("\\."+clienttasks.arch, ""),	//"manual: subscription-manager-gui = 1.9.11-1.el6",
					"manual: librsvg2",
					//"manual: rhn-setup-gnome"	// removed by git commit 72e37ab24d5ba1ea9ff8ccc756246df721e204b1 Fix firstboot on Fedora 19. (will help fix Bug 1021013 - RHEL7 firstboot: missing option to skip Subscription Management Registration)
			}));
		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20399", "RHEL7-51274"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="check the rpm requires list for changes to subscription-manager-initial-setup-addon",
			groups={"Tier2Tests","blockedbyBug-1246146","blockedbyBug-1246391"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testRpmRequireListForSubscriptionManagerInitialSetupAddon() {
		String pkg = "subscription-manager-initial-setup-addon";
		if (!clienttasks.isPackageInstalled(pkg)) throw new SkipException("This test require that package '"+pkg+"' be installed.");
		if (clienttasks.installedPackageVersionMap.get("subscription-manager-gui")==null) clienttasks.isPackageVersion("subscription-manager-gui","==","0.0");	// will populate clienttasks.installedPackageVersionMap.get("subscription-manager-gui")
		String rpmCommand = "rpm --query --requires "+pkg+" --verbose";
		if (Integer.valueOf(clienttasks.redhatReleaseX) == 5) rpmCommand += " | egrep -v '\\(.*\\)'";
		if (Integer.valueOf(clienttasks.redhatReleaseX) > 5) rpmCommand += " | egrep -v '(^auto:|^rpmlib:)'";
		SSHCommandResult sshCommandResult = client.runCommandAndWait(rpmCommand);
		
		List<String> actualRequiresList = new ArrayList<String>();
		for (String requires : Arrays.asList(sshCommandResult.getStdout().trim().split("\\n"))) {
			if (!requires.trim().isEmpty()) actualRequiresList.add(requires.trim());
		}
		
		List<String> expectedRequiresList = new ArrayList<String>();
		if (Integer.valueOf(clienttasks.redhatReleaseX)<7) {
			Assert.fail("Did not expect package '"+pkg+"' to be installed on RHEL release '"+clienttasks.redhatReleaseX+"'.");
		}
		if (clienttasks.redhatReleaseX.equals("7")) {
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"manual: initial-setup-gui >= 0.3.9.24-1",
					"manual: subscription-manager-gui = "+clienttasks.installedPackageVersionMap.get("subscription-manager-gui").replace("subscription-manager-gui-", "").replaceFirst("\\."+clienttasks.arch, ""),	//"manual: subscription-manager-gui = 1.15.6-1.el7",
			}));
			if (clienttasks.isPackageVersion("subscription-manager-gui",">=","1.21.4-1")) {	// commit 23b5409c76d586c4e34440788d612e7ed65e2df6  Stop building subscription-manager-gui, when Python 3 is used
				// let's start over with the introduction of rhsm-gtk
				expectedRequiresList.clear();
				expectedRequiresList.addAll(Arrays.asList(new String[]{
						"manual: initial-setup-gui >= 0.3.9.24-1",
						"manual: rhsm-gtk = "+clienttasks.installedPackageVersionMap.get("rhsm-gtk").replace("rhsm-gtk-", "").replaceFirst("\\."+clienttasks.arch, ""),	//"manual: rhsm-gtk = 1.9.11-1.el6",
				}));
			}
		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20390", "RHEL7-32182"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="check the rpm requires list for changes to subscription-manager-migration",
			groups={"Tier2Tests","blockedByBug-1049037"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testRpmRequireListForSubscriptionManagerMigration() {
		String pkg = "subscription-manager-migration";
		if (!clienttasks.isPackageInstalled(pkg)) throw new SkipException("This test require that package '"+pkg+"' be installed.");
		String rpmCommand = "rpm --query --requires "+pkg+" --verbose";
		if (Integer.valueOf(clienttasks.redhatReleaseX) == 5) rpmCommand += " | egrep -v '\\(.*\\)'";
		if (Integer.valueOf(clienttasks.redhatReleaseX) > 5) rpmCommand += " | egrep -v '(^auto:|^rpmlib:)'";
		SSHCommandResult sshCommandResult = client.runCommandAndWait(rpmCommand);
		
		List<String> actualRequiresList = new ArrayList<String>();
		for (String requires : Arrays.asList(sshCommandResult.getStdout().trim().split("\\n"))) {
			if (!requires.trim().isEmpty()) actualRequiresList.add(requires.trim());
		}
		
		List<String> expectedRequiresList = new ArrayList<String>();
		if (clienttasks.redhatReleaseX.equals("5")) { 
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"/usr/bin/python",
					"subscription-manager = "+clienttasks.installedPackageVersionMap.get("subscription-manager").replace("subscription-manager-", "").replaceFirst("\\."+clienttasks.arch, ""),	//"manual: subscription-manager-manager = 1.9.11-1.el6",
					"rhnlib"
			}));
			if		(clienttasks.isPackageVersion("subscription-manager",">=","1.10.10-1"))	expectedRequiresList.add("subscription-manager-migration-data");			// Bug 1049037 - subscription-manager-migration should require subscription-manager-migration-data; subscription-manager commit 8e988551fae03a51a72dd8852066ff9b204d982c
//			for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
//			Assert.assertTrue(actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' contains the expected list "+expectedRequiresList);
//			return;
		}
		if (clienttasks.redhatReleaseX.equals("6") || clienttasks.redhatReleaseX.equals("7")) {
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"manual: subscription-manager = "+clienttasks.installedPackageVersionMap.get("subscription-manager").replace("subscription-manager-", "").replaceFirst("\\."+clienttasks.arch, ""),	// "manual: subscription-manager = 1.9.11-1.el6"
					"manual: rhnlib"
			}));
			if		(clienttasks.isPackageVersion("subscription-manager",">=","1.10.10-1"))	expectedRequiresList.add("manual: subscription-manager-migration-data");			// Bug 1049037 - subscription-manager-migration should require subscription-manager-migration-data; subscription-manager commit 8e988551fae03a51a72dd8852066ff9b204d982c

		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20394", "RHEL7-32174"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="check the rpm requires list for changes to subscription-manager-migration-data",
			groups={"Tier2Tests"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testRpmRequireListForSubscriptionManagerMigrationData() {
		String pkg = "subscription-manager-migration-data";
		if (!clienttasks.isPackageInstalled(pkg)) throw new SkipException("This test require that package '"+pkg+"' be installed.");
		String rpmCommand = "rpm --query --requires "+pkg+" --verbose";
		if (Integer.valueOf(clienttasks.redhatReleaseX) == 5) rpmCommand += " | egrep -v '\\(.*\\)'";
		if (Integer.valueOf(clienttasks.redhatReleaseX) > 5) rpmCommand += " | egrep -v '(^auto:|^rpmlib:)'";
		SSHCommandResult sshCommandResult = client.runCommandAndWait(rpmCommand);
		
		List<String> actualRequiresList = new ArrayList<String>();
		for (String requires : Arrays.asList(sshCommandResult.getStdout().trim().split("\\n"))) {
			if (!requires.trim().isEmpty()) actualRequiresList.add(requires.trim());
		}
		
		List<String> expectedRequiresList = new ArrayList<String>();
		if (clienttasks.redhatReleaseX.equals("5")) { 
			// empty list
//			for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
//			Assert.assertTrue(actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' contains the expected list "+expectedRequiresList);
//			return;
		}
		if (clienttasks.redhatReleaseX.equals("6")) {
			// empty list
		}
		if (clienttasks.redhatReleaseX.equals("7")) {
			// empty list
		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20400", "RHEL7-32167"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="check the rpm requires list for changes to subscription-manager-plugin-ostree",
			groups={"Tier2Tests","blockedByBug-1165771"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testRpmRequireListForSubscriptionManagerPluginOstree() {
		String pkg = "subscription-manager-plugin-ostree";
		if (!clienttasks.isPackageInstalled(pkg)) throw new SkipException("This test require that package '"+pkg+"' be installed.");
		String rpmCommand = "rpm --query --requires "+pkg+" --verbose";
		if (Integer.valueOf(clienttasks.redhatReleaseX) == 5) rpmCommand += " | egrep -v '\\(.*\\)'";
		if (Integer.valueOf(clienttasks.redhatReleaseX) > 5) rpmCommand += " | egrep -v '(^auto:|^rpmlib:)'";
		SSHCommandResult sshCommandResult = client.runCommandAndWait(rpmCommand);
		
		List<String> actualRequiresList = new ArrayList<String>();
		for (String requires : Arrays.asList(sshCommandResult.getStdout().trim().split("\\n"))) {
			if (!requires.trim().isEmpty()) actualRequiresList.add(requires.trim());
		}
		
		List<String> expectedRequiresList = new ArrayList<String>();
		if (Integer.valueOf(clienttasks.redhatReleaseX)<7) {
			Assert.fail("Did not expect package '"+pkg+"' to be installed on RHEL release '"+clienttasks.redhatReleaseX+"'.");
		}
		if (clienttasks.redhatReleaseX.equals("7")) {
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"manual: pygobject3-base",
					"manual: python-iniparse >= 0.4",
					"manual: subscription-manager = "+clienttasks.installedPackageVersionMap.get("subscription-manager").replace("subscription-manager-", "").replaceFirst("\\."+clienttasks.arch, ""),	// "manual: subscription-manager = 1.15.6-1.el7"	// Bug 1165771
					//TODO subscription-manager >= 1.15.9-5  account for Bug 1185958: Make ostree plugin depend on ostree
			}));
		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20294", "RHEL7-32179"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="check the rpm requires list for changes to subscription-manager-plugin-container",
			groups={"Tier2Tests","blockedByBug-1165771"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testRpmRequireListForSubscriptionManagerPluginContainer() {
		// initial version subscription-manager-plugin-container-1.13.7-1.el7.x86_64
		String pkg = "subscription-manager-plugin-container";
		if (!clienttasks.isPackageInstalled(pkg)) throw new SkipException("This test require that package '"+pkg+"' be installed.");
		String rpmCommand = "rpm --query --requires "+pkg+" --verbose";
		if (Integer.valueOf(clienttasks.redhatReleaseX) == 5) rpmCommand += " | egrep -v '\\(.*\\)'";
		if (Integer.valueOf(clienttasks.redhatReleaseX) > 5) rpmCommand += " | egrep -v '(^auto:|^rpmlib:)'";
		SSHCommandResult sshCommandResult = client.runCommandAndWait(rpmCommand);
		
		List<String> actualRequiresList = new ArrayList<String>();
		for (String requires : Arrays.asList(sshCommandResult.getStdout().trim().split("\\n"))) {
			if (!requires.trim().isEmpty()) actualRequiresList.add(requires.trim());
		}
		
		List<String> expectedRequiresList = new ArrayList<String>();
		if (Integer.valueOf(clienttasks.redhatReleaseX)<6) {
			Assert.fail("Did not expect package '"+pkg+"' to be installed on RHEL release '"+clienttasks.redhatReleaseX+"'.");
		}
		if (clienttasks.redhatReleaseX.equals("6")) {
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					//none
					"manual: subscription-manager = "+clienttasks.installedPackageVersionMap.get("subscription-manager").replace("subscription-manager-", "").replaceFirst("\\."+clienttasks.arch, ""),	// "manual: subscription-manager = 1.15.6-1.el7"	// Bug 1165771
			}));
			
			if (clienttasks.isPackageVersion("subscription-manager-plugin-container",">=","1.20.1-1")) {	// commit 76c52b9002906d80b17baf6af4da67e648ce2415 1422196: Update container certs after plugin install
				expectedRequiresList.add("post: /bin/sh");
			}
		}
		if (clienttasks.redhatReleaseX.equals("7")) {
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					//none
					"manual: subscription-manager = "+clienttasks.installedPackageVersionMap.get("subscription-manager").replace("subscription-manager-", "").replaceFirst("\\."+clienttasks.arch, ""),	// "manual: subscription-manager = 1.15.6-1.el7"	// Bug 1165771
			}));
			
			if (clienttasks.isPackageVersion("subscription-manager-plugin-container",">=","1.20.1-1")) {	// commit 76c52b9002906d80b17baf6af4da67e648ce2415 1422196: Update container certs after plugin install
				expectedRequiresList.add("post,interp: /bin/sh");
			}
		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20296", "RHEL7-32158"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="When the client is 1 hour or more (normalized for time zone and daylight savings time) ahead of candlepin's clock, verify that a WARNING is logged to rhsm.log",
			groups={"Tier2Tests","VerifyPositiveClockSkewDetection_Test","blockedByBug-772936","blockedByBug-1090350"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testPositiveClockSkewDetection() {
		//client.runCommandAndWait("rm -f "+clienttasks.rhsmLogFile);	// remove it because it occasionally gets backed up to rhsm.log.1 in the midst of a pair of calls to RemoteFileTasks.markFile(...) and RemoteFileTasks.getTailFromMarkedFile(...)
		client.runCommandAndWait("truncate --size=0 --no-create "+clienttasks.rhsmLogFile);	// truncate it to avoid getting backed up to rhsm.log.1 in the midst of a pair of calls to RemoteFileTasks.markFile(...) and RemoteFileTasks.getTailFromMarkedFile(...)
		clienttasks.unregister(null, null, null, null);	// do not need to be registered for this test
		
		String rhsmLogMarker = System.currentTimeMillis()+" Testing clock skew detection...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, rhsmLogMarker);
		log.info("Calling subscription-manager version to trigger communication to the currently configured candlepin server...");
		clienttasks.version(null, null, null, null);
		String rhsmLogResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, rhsmLogMarker, null).trim();
		Assert.assertTrue(!rhsmLogResult.contains(clienttasks.msg_ClockSkewDetection), "Assuming the rhsm client is less than 60 minutes ahead of the candlepin server, WARNING '"+clienttasks.msg_ClockSkewDetection+"' is NOT logged to '"+clienttasks.rhsmLogFile+"'.");

		rhsmLogMarker = System.currentTimeMillis()+" Testing positive clock skew detection...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, rhsmLogMarker);
		positiveClockSkewMinutes = new Integer(119); // to catch bug 1090350, test with skew 1 min less than 1 hour skew limit + 1 hour daylight saving	// the clock skew limit is set to 1 hour in src/rhsm/connection.py def drift_check(utc_time_string, hours=1)
		log.info("Advancing the rhsm client clock ahead by '"+positiveClockSkewMinutes+"' minutes...");
		client.runCommandAndWait(String.format("date -s +%dminutes", positiveClockSkewMinutes));
		log.info("Calling subscription-manager version to trigger communication to the currently configured candlepin server...");
		clienttasks.version(null, null, null, null);
		rhsmLogResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, rhsmLogMarker, null).trim();
		Assert.assertTrue(rhsmLogResult.contains(clienttasks.msg_ClockSkewDetection), "Assuming the rhsm client is greater than 60 minutes ahead of the candlepin server, then WARNING '"+clienttasks.msg_ClockSkewDetection+"' is logged to '"+clienttasks.rhsmLogFile+"'.");
	}
	@AfterGroups(groups={"setup"}, value={"VerifyPositiveClockSkewDetection_Test"})
	public void afterPositiveClockSkewDetection_Test() {
		if (clienttasks!=null) {
			if (positiveClockSkewMinutes!=null) {
				client.runCommandAndWait(String.format("date -s -%dminutes", positiveClockSkewMinutes));
				positiveClockSkewMinutes = null;
			}
		}
	}
	protected Integer positiveClockSkewMinutes = null;


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20367", "RHEL7-32177"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="When the client is 1 hour or more (normalized for time zone and daylight savings time) behind candlepin's clock, verify that a WARNING is logged to rhsm.log",
			groups={"Tier2Tests","VerifyNegativeClockSkewDetection_Test","blockedByBug-772936","blockedByBug-1090350"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testNegativeClockSkewDetection() {
		//client.runCommandAndWait("rm -f "+clienttasks.rhsmLogFile);	// remove it because it occasionally gets backed up to rhsm.log.1 in the midst of a pair of calls to RemoteFileTasks.markFile(...) and RemoteFileTasks.getTailFromMarkedFile(...)
		client.runCommandAndWait("truncate --size=0 --no-create "+clienttasks.rhsmLogFile);	// truncate it to avoid getting backed up to rhsm.log.1 in the midst of a pair of calls to RemoteFileTasks.markFile(...) and RemoteFileTasks.getTailFromMarkedFile(...)
		clienttasks.unregister(null, null, null, null);	// do not need to be registered for this test
		
		String rhsmLogMarker = System.currentTimeMillis()+" Testing clock skew detection...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, rhsmLogMarker);
		log.info("Calling subscription-manager version to trigger communication to the currently configured candlepin server...");
		clienttasks.version(null, null, null, null);
		String rhsmLogResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, rhsmLogMarker, null).trim();
		Assert.assertTrue(!rhsmLogResult.contains(clienttasks.msg_ClockSkewDetection), "Assuming the rhsm client is less than 60 minutes behind of the candlepin server, WARNING '"+clienttasks.msg_ClockSkewDetection+"' is NOT logged to '"+clienttasks.rhsmLogFile+"'.");

		rhsmLogMarker = System.currentTimeMillis()+" Testing negative clock skew detection...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, rhsmLogMarker);
		negativeClockSkewMinutes = new Integer(119); // to catch bug 1090350, test with skew 1 min less than 1 hour skew limit + 1 hour daylight saving	// the clock skew limit is set to 1 hour in src/rhsm/connection.py def drift_check(utc_time_string, hours=1)
		log.info("Retarding the rhsm client clock behind by '"+negativeClockSkewMinutes+"' minutes...");
		client.runCommandAndWait(String.format("date -s -%dminutes", negativeClockSkewMinutes));
		log.info("Calling subscription-manager version to trigger communication to the currently configured candlepin server...");
		clienttasks.version(null, null, null, null);
		rhsmLogResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, rhsmLogMarker, null).trim();
		// NOTE:  If the candlepin server was newly deployed within the last 119 minutes, then a "SSLError: certificate verify failed" will be thrown.  Skip the test if this happens.
		// NOTE:  If the candlepin server was newly deployed within the last 119 minutes, then a "Error while checking server version: [SSL: CERTIFICATE_VERIFY_FAILED] certificate verify failed" will be thrown.  Skip the test if this happens.
		if (!rhsmLogResult.contains(clienttasks.msg_ClockSkewDetection) && rhsmLogResult.contains("certificate verify failed")) throw new SkipException("Assuming the candlepin server was recently deployed within the last hour because an SSLError was thrown instead of clock skew detection.");
		Assert.assertTrue(rhsmLogResult.contains(clienttasks.msg_ClockSkewDetection), "Assuming the rhsm client is greater than 60 minutes behind of the candlepin server, then WARNING '"+clienttasks.msg_ClockSkewDetection+"' is logged to '"+clienttasks.rhsmLogFile+"'.");
	}
	@AfterGroups(groups={"setup"}, value={"VerifyNegativeClockSkewDetection_Test"})
	public void afterNegativeClockSkewDetection_Test() {
		if (clienttasks!=null) {
			if (negativeClockSkewMinutes!=null) {
				client.runCommandAndWait(String.format("date -s +%dminutes", negativeClockSkewMinutes));
				negativeClockSkewMinutes = null;
			}
		}
	}
	protected Integer negativeClockSkewMinutes = null;



	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20383", "RHEL7-51273"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="check the rpm query list for subscription-manager-migration and verify it does NOT list sat5to6",
			groups={"Tier2Tests","blockedByBug-1145833"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testRpmListForSubscriptionManagerMigrationExcludesSat5to6() {
		// initial version subscription-manager-plugin-container-1.13.7-1.el7.x86_64
		String pkg = "subscription-manager-migration";
		if (!clienttasks.isPackageInstalled(pkg)) throw new SkipException("This test require that package '"+pkg+"' be installed.");
		String rpmCommand = "rpm --query --list "+pkg;
		SSHCommandResult sshCommandResult = client.runCommandAndWait(rpmCommand);
		String sat5to6 = "sat5to6";
		//sat5to6 = pkg;	// debugTesting
		Assert.assertTrue(!sshCommandResult.getStdout().contains(sat5to6), "The rpm query list for package '"+pkg+"' excludes the '"+sat5to6+"' tool.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20292", "RHEL7-51272"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="python-rhsm should not set socket.setdefaulttimeout(60)",
			groups={"Tier2Tests","blockedByBug-1195446"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testPythonRhsmDoesNotSetSocketDefaultTimeout() throws IOException {
		if (clienttasks.isPackageVersion("python-rhsm", "<", "1.14.2-1")) throw new SkipException("Blocking bugzilla 1195446 was not fixed until version python-rhsm-1.14.2-1");	// python-rhsm commit a974e5d636009fa41bec2b4a9d33f853e9e72a2b
		
		// copy the ismanagedtest.py script to the client
		File socketgetdefaulttimeouttestFile = new File(System.getProperty("automation.dir", null)+"/scripts/socketgetdefaulttimeouttest.py");
		if (!socketgetdefaulttimeouttestFile.exists()) Assert.fail("Failed to find expected script: "+socketgetdefaulttimeouttestFile);
		RemoteFileTasks.putFile(client, socketgetdefaulttimeouttestFile.toString(), "/usr/local/bin/", "0755");
		
		// BEFORE FIX
		//	[root@jsefler-71 ~]# rpm -q python-rhsm
		//	python-rhsm-1.13.10-1.el7.x86_64
		//	[root@jsefler-71 ~]# python /usr/local/bin/socketgetdefaulttimeouttest.py
		//	None
		//	Loaded plugins: langpacks, product-id
		//	60.0
		
		// AFTER FIX
		//	[root@jsefler-72 ~]# rpm -q python-rhsm
		//	python-rhsm-1.15.3-1.el7.x86_64
		//	[root@jsefler-72 ~]# python /usr/local/bin/socketgetdefaulttimeouttest.py 
		//	None
		//	Loaded plugins: langpacks, product-id
		//	None
		SSHCommandResult result = client.runCommandAndWait("socketgetdefaulttimeouttest.py");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0), "ExitCode from prior command.");
		// TEMPORARY WORKAROUND
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1282961"; // Bug 1282961 - Plugin "search-disabled-repos" requires API 2.7. Supported API is 2.6.
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen && clienttasks.redhatReleaseX.equals("6") && clienttasks.isPackageVersion("subscription-manager", ">=", "1.15")) {
			String stdout = result.getStdout().replace("Plugin \"search-disabled-repos\" requires API 2.7. Supported API is 2.6.\n", "").trim();
			Assert.assertMatch(stdout, "None\nLoaded plugins:.*\nNone", "Stdout");
		} else
		// END OF WORKAROUND
		Assert.assertMatch(result.getStdout().trim(), "None\nLoaded plugins:.*\nNone", "Stdout");
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-51178", "RHEL-135301"}, //importReady=false, // comment out or delete when this TestDefinition is good and ready to be imported as a testcase into Polarion to retrieve a testCaseID
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28478",	// RHSM-REQ : Subscription-manager Command-Line Tool (subscription-manager)
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84899",	// RHSM-REQ : Subscription-manager Command-Line Tool (subscription-manager)
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.CRITICAL, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="assert that rhsmcertd does not consume 100% CPU",
			groups={"Tier1Tests","blockedByBug-1574529"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testRhsmcertdFor100PercentCPUConsumption() throws IOException {
		
		// get the CPU usage for rhsmcertd
		//	[root@hp-dl380pgen8-02-vm-1 ~]# ps -C rhsmcertd -o %cpu 
		//	%CPU
		//	99.9
		String cpuPercentageCommand = "ps -C rhsmcertd -o %cpu | tail -1";
		SSHCommandResult result = client.runCommandAndWait(cpuPercentageCommand);
		if (!isFloat(result.getStdout().trim())) { // indicator that rhsmcertd service is stopped
			// restart rhsmcertd
			log.info("The rhsmcertd process appears to be stopped, let's restart it....");
			clienttasks.restart_rhsmcertd(null, null, null);	// Note that the splay configuration will be temporarily turned off while restarting rhsmcertd 
			result = client.runCommandAndWait(cpuPercentageCommand);
		}
		Float cpuPercentage = Float.valueOf(result.getStdout().trim());
		
		// fail when cpuPercentage exceeds cpuPercentageTolerated for longer than cpuDuration seconds
		Float cpuPercentageTolerated = 10.0f; // 10.0%
		int duration = 10; // seconds
		if (cpuPercentage>cpuPercentageTolerated) {
			sleep(duration*1000);
			result = client.runCommandAndWait(cpuPercentageCommand);
			cpuPercentage = Float.valueOf(result.getStdout().trim());
		}
		Assert.assertTrue(cpuPercentage<cpuPercentageTolerated, "The actual %CPU of rhsmcertd '"+cpuPercentage+"' is less than '"+cpuPercentageTolerated+"' for more than '"+duration+"' seconds.");
		
		// according to https://bugzilla.redhat.com/show_bug.cgi?id=1574529#c4 we should also test with splay turned on
		// turn on splay
		log.info("Testing again with the rhsmcertd splay configuration turned on....");
		clienttasks.config(null, null, true, new String[] {"rhsmcertd","splay","1"});
		// restart rhsmcertd service
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7)	{	// the RHEL7 / F16+ way...
			RemoteFileTasks.runCommandAndAssert(client, "systemctl restart rhsmcertd.service && systemctl is-active rhsmcertd.service", Integer.valueOf(0), "^active$", null);
		} else {
			// NEW SERVICE RESTART FEEDBACK AFTER IMPLEMENTATION OF Bug 818978 - Missing systemD unit file
			//	[root@jsefler-59server ~]# service rhsmcertd restart
			//	Stopping rhsmcertd...                                      [  OK  ]
			//	Starting rhsmcertd...                                      [  OK  ]
			RemoteFileTasks.runCommandAndAssert(client,"service rhsmcertd restart",Integer.valueOf(0),"^Starting rhsmcertd\\.\\.\\.\\[  OK  \\]$",null);	
			
			// # service rhsmcertd restart
			// rhsmcertd (pid 10172 10173) is running...
			RemoteFileTasks.runCommandAndAssert(client,"service rhsmcertd status",Integer.valueOf(0),"^rhsmcertd \\(pid \\d+\\) is running...$",null);		// master/RHEL58 branch
		}
		// test the cpu percentage again
		result = client.runCommandAndWait(cpuPercentageCommand);
		cpuPercentage = Float.valueOf(result.getStdout().trim());
		if (cpuPercentage>cpuPercentageTolerated) {
			sleep(duration*1000);
			result = client.runCommandAndWait(cpuPercentageCommand);
			cpuPercentage = Float.valueOf(result.getStdout().trim());
		}
		Assert.assertTrue(cpuPercentage<cpuPercentageTolerated, "The actual %CPU of rhsmcertd '"+cpuPercentage+"' is less than '"+cpuPercentageTolerated+"' for more than '"+duration+"' seconds (with rhsm.conf [rhsmcertd]splay=1).");
	
	}
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 688469 - subscription-manager <module> --help does not work in localized environment. https://github.com/RedHatQE/rhsm-qe/issues/144
	// TODO Bug 684941 - Deleting a product with a subscription gives ugly error https://github.com/RedHatQE/rhsm-qe/issues/145
	// TODO Bug 629708 - import/export validation error wrapped https://github.com/RedHatQE/rhsm-qe/issues/146
	// TODO Bug 744536 - [ALL LANG] [RHSM CLI] unsubscribe module _unexpected 'ascii' code can't decode ...message. https://github.com/RedHatQE/rhsm-qe/issues/147
	
	
	
	
	
	
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
		//ll.add(Arrays.asList(new Object[]{clienttasks.command+" list"}));	restriction lifted by https://bugzilla.redhat.com/show_bug.cgi?id=725870
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" list --available --all"}));
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" list --available"}));
		//ll.add(Arrays.asList(new Object[]{clienttasks.command+" list --consumed"}));	restriction lifted by https://bugzilla.redhat.com/show_bug.cgi?id=725870
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" refresh"}));
// this functionality appears to have been removed: subscription-manager-0.71-1.el6.i686  - jsefler 7/21/2010
//		ll.add(Arrays.asList(new Object[]{clienttasks.command+" subscribe --product=FOO"}));
// this functionality appears to have been removed: subscription-manager-0.93.14-1.el6.x86_64 - jsefler 1/21/2011
//		ll.add(Arrays.asList(new Object[]{clienttasks.command+" subscribe --regtoken=FOO"}));
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" subscribe --pool=FOO"}));
// ability to unsubscribe without being registered was added after fix for bug 735338  jsefler 9/13/2011
//		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe"}));
//		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe --all"}));
//		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe --serial=FOO"}));
// this functionality appears to have been removed: subscription-manager-0.68-1.el6.i686  - jsefler 7/12/2010
//		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe --product=FOO"}));
//		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe --regtoken=FOO"}));
//		ll.add(Arrays.asList(new Object[]{clienttasks.command+" unsubscribe --pool=FOO"}));
		ll.add(Arrays.asList(new Object[]{clienttasks.command+" redeem"}));

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
				
		// negative tests that require the system to be unregistered first...
		// Object blockedByBug, String command, Integer expectedExitCode, String expectedStdout, String expectedStderr
		clienttasks.unregister(null,null,null, null);
		
		
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			// python os.EX_CODES used by devel are listed here http://docs.thefoundry.co.uk/nuke/63/pythonreference/os-module.html  EX_USAGE=64  EX_DATAERR=65  EX_UNAVAILABLE=69  EX_SOFTWARE=70
			ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" unsubscribe --product=FOO",							new Integer(2),		clienttasks.command+": error: no such option: --product", "Usage: subscription-manager unsubscribe [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" unsubscribe --regtoken=FOO",							new Integer(2),		clienttasks.command+": error: no such option: --regtoken", "Usage: subscription-manager unsubscribe [OPTIONS]"}));
			if (clienttasks.isPackageVersion("subscription-manager","<","1.16.5-1")) {	// commit 3d2eb4b8ef8e2094311e3872cdb9602b84fed9be     1198178: Adds pool option to remove, unsubscribe command
				ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" unsubscribe --pool=FOO",								new Integer(2),		clienttasks.command+": error: no such option: --pool", "Usage: subscription-manager unsubscribe [OPTIONS]"}));
			} else {
				ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" unsubscribe --pool=FOO",								new Integer(1),		"",""}));			
			}
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" subscribe",											new Integer(1),		"","This system is not yet registered. Try 'subscription-manager register --help' for more information."}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688","1162331"}),clienttasks.rhsmDebugSystemCommand(null,null,null,null,null,null,null,null, null),new Integer(1),		"","This system is not yet registered. Try 'subscription-manager register --help' for more information."}));
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.18.2-1")) {	// post commit ad982c13e79917e082f336255ecc42615e1e7707
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1078091","1119688","1320507"}),	clienttasks.command+" register --serverurl=https://sat6_fqdn/ --insecure",	new Integer(69),	"","Unable to reach the server at sat6_fqdn:"+clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port")}));
			} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.17.5-1")) {	// subscription-manager commit ea10b99095ad58df57ed107e13bf19498e003ae8
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1078091","1119688","1320507"}),	clienttasks.command+" register --serverurl=https://sat6_fqdn/ --insecure",	new Integer(69),	"","Unable to reach the server at sat6_fqdn:"+clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port")+"/"}));
			} else {
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1078091","1119688"}),			clienttasks.command+" register --serverurl=https://sat6_fqdn/ --insecure",	new Integer(69),	"","Unable to reach the server at sat6_fqdn:443/"}));
			}
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" register --servicelevel=foo",							new Integer(64),	"","Error: Must use --auto-attach with --servicelevel."}));	// changed by bug 874804,876305		ll.add(Arrays.asList(new Object[]{clienttasks.command+" register --servicelevel=foo",				new Integer(255),	"Error: Must use --autosubscribe with --servicelevel.", ""}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"856236","1119688"}),	clienttasks.command+" register --activationkey=foo --org=foo --env=foo",	new Integer(64),	"","Error: Activation keys do not allow environments to be specified."}));
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.16.2-1")) {	// subscription-manager commit f14d2618ea94c18a0295ae3a5526a2ff252a3f99	and 6bd0448c85c10d8a58cae10372f0d4aa323d5c27
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1262998"}),		clienttasks.command+" register --consumerid=123 --force",					new Integer(64),	"","Error: Can not force registration while attempting to recover registration with consumerid. Please use --force without --consumerid to re-register or use the clean command and try again without --force."}));
			}
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" list --installed --servicelevel=foo",					new Integer(64),	"","Error: --servicelevel is only applicable with --available or --consumed"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" list --no-overlap",									new Integer(64),	"","Error: --no-overlap is only applicable with --available"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" list --match-installed",								new Integer(64),	"","Error: --match-installed is only applicable with --available"}));
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.9-1")) {
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1162170"}),		clienttasks.command+" list --pool-only",									new Integer(64),	"","Error: --pool-only is only applicable with --available and/or --consumed"}));
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1162170"}),		clienttasks.command+" list --pool-only --installed",						new Integer(64),	"","Error: --pool-only is only applicable with --available and/or --consumed"}));
			}
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-1")) {	// post commit 2e4c2b8ab686bd240b99acb8d9c149e4aa7010d8
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1479353"}),		clienttasks.command+" list --after=2018-01-01",								new Integer(64),	"","Error: --after is only applicable with --available"}));
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1479353"}),		clienttasks.command+" list --after=2018-01-01 --ondate=2018-01-01 --avail",	new Integer(64),	"","Error: --after cannot be used with --ondate"}));	
			}

			ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" repo-override --repo",								new Integer(2),		clienttasks.command+": error: --repo option requires an argument",	"Usage: subscription-manager repo-override [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{null,										 			clienttasks.command+" repo-override --remove",								new Integer(2),		clienttasks.command+": error: --remove option requires an argument",	"Usage: subscription-manager repo-override [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" repo-override --add",									new Integer(2),		clienttasks.command+": error: --add option requires an argument",	"Usage: subscription-manager repo-override [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" repo-override --add=foo",								new Integer(2),		clienttasks.command+": error: --add arguments should be in the form of \"name:value\"",	"Usage: subscription-manager repo-override [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" repo-override --repo=foo",							new Integer(64),	"","Error: The --repo option must be used with --list or --add or --remove."}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" repo-override --remove=foo",							new Integer(64),	"","Error: You must specify a repository to modify"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" repo-override --add=foo:bar",							new Integer(64),	"","Error: You must specify a repository to modify"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" repo-override --list --remove-all",					new Integer(64),	"","Error: You may not use --list with --remove-all"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" repo-override --remove-all --add=foo:bar --repo=fb",	new Integer(64),	"","Error: You may not use --add or --remove with --remove-all and --list"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" repo-override --remove-all --remove=foo --repo=fb",	new Integer(64),	"","Error: You may not use --add or --remove with --remove-all and --list"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" repo-override --list --remove=foo --repo=fb",			new Integer(64),	"","Error: You may not use --add or --remove with --remove-all and --list"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" repo-override --list --remove=foo --repo=fb",			new Integer(64),	"","Error: You may not use --add or --remove with --remove-all and --list"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" repo-override --remove=foo --repo=foobar",			new Integer(1),		"","This system is not yet registered. Try 'subscription-manager register --help' for more information."}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" repo-override --add=foo:bar --repo=foobar",			new Integer(1),		"","This system is not yet registered. Try 'subscription-manager register --help' for more information."}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" repo-override --remove-all",							new Integer(1),		"","This system is not yet registered. Try 'subscription-manager register --help' for more information."}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" repo-override --list",								new Integer(1),		"","This system is not yet registered. Try 'subscription-manager register --help' for more information."}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" repo-override",										new Integer(1),		"","This system is not yet registered. Try 'subscription-manager register --help' for more information."}));
			if (clienttasks.isPackageVersion("subscription-manager-migration",">=","1.14.3-1") && clienttasks.isPackageVersion("subscription-manager-migration","<","1.14.7-1")) {
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1196416"}),				"rhn-migrate-classic-to-rhsm --activation-key=foo",					new Integer(1),		"","The --activation-key option requires that a --org be given."}));	// see https://bugzilla.redhat.com/show_bug.cgi?id=1196416#c5
			}
			if (clienttasks.isPackageVersion("subscription-manager-migration",">=","1.14.7-1")) {
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1196416","1217835"}),	"rhn-migrate-classic-to-rhsm --activation-key=foo",								new Integer(64),	"","The --activation-key option requires that a --org be given."}));	// commit 270f2a3e5f7d55b69a6f98c160d38362961b3059 Bug 1217835 - exit code from rhn-migrate-classic-to-rhsm activation-key without an org should be EX_USAGE
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1196396","1217835"}),	"rhn-migrate-classic-to-rhsm --activation-key=foo --destination-user=foo",		new Integer(64),	"","The --activation-key option precludes the use of --destination-user and --destination-password"}));		// commit e6ee0dac25ac3cf6adc0d52779e6c806e7f62799		// Bug 1196396 - rhn-migrate-classic-to-rhsm should abort when using --activation-key option with --destination-user/password options
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1196396","1217835"}),	"rhn-migrate-classic-to-rhsm --activation-key=foo --destination-password=bar",	new Integer(64),	"","The --activation-key option precludes the use of --destination-user and --destination-password"}));		// commit e6ee0dac25ac3cf6adc0d52779e6c806e7f62799		// Bug 1196396 - rhn-migrate-classic-to-rhsm should abort when using --activation-key option with --destination-user/password options
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1196396","1217835"}),	"rhn-migrate-classic-to-rhsm --activation-key=foo --environment=bar",			new Integer(64),	"","The --activation-key and --environment options cannot be used together."}));		// commit e6ee0dac25ac3cf6adc0d52779e6c806e7f62799	commit 270f2a3e5f7d55b69a6f98c160d38362961b3059
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1217835"}),				"rhn-migrate-classic-to-rhsm --servicelevel=foo --no-auto",						new Integer(64),	"","The --servicelevel and --no-auto options cannot be used together."}));	// commit 270f2a3e5f7d55b69a6f98c160d38362961b3059 Bug 1217835 - exit code from rhn-migrate-classic-to-rhsm activation-key without an org should be EX_USAGE
			}
			if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.18.2")) {
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1390258"}),				"rhn-migrate-classic-to-rhsm --remove-rhn-packages --keep",						new Integer(64),	"","The --remove-rhn-packages and --keep options cannot be used together."}));		//  
			}
			
			
			// negative tests that require the system to be registered before attempting the test...
			ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" register --username "+sm_clientUsername+" --password "+sm_clientPassword+(sm_clientOrg==null?"":" --org "+sm_clientOrg),	new Integer(0),	null,	""}));
			if (clienttasks.isPackageVersion("subscription-manager","<","1.13.13-1")) {	// commit cb590a75f3a2de921961808d00ab251180c51691 Make 'attach' auto unless otherwise specified.
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),		clienttasks.command+" subscribe",											new Integer(64),	"","Error: This command requires that you specify a pool with --pool or use --auto.".replace("with --pool", "with --pool or --file,")}));	// after bug 1159974 Error: This command requires that you specify a pool with --pool or --file, or use --auto.
			}
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" subscribe --pool=123 --auto",							new Integer(64),	"","Error: --auto may not be used when specifying pools."/*"Error: Only one of --pool or --auto may be used with this command."*/}));	// message changed by commit 3167333fc3a261de939f4aa0799b4283f2b9f4d2
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.14.3-1")) {	// commit bb6424e5cac93bfd3dfc9e5163d593b954359f52 1200972: Fixed grammar issue with error message in the attach command
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688","1200972"}),clienttasks.command+" subscribe --pool=123 --servicelevel=foo",				new Integer(64),	"","Error: The --servicelevel option cannot be used when specifying pools."}));		// was "Error: Servicelevel is unused with --pool" for a short time from subscription-manager-1.14.1-1
			} else {
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688","1200972"}),clienttasks.command+" subscribe --pool=123 --servicelevel=foo",				new Integer(64),	"","Error: Must use --auto with --servicelevel."}));
			}
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1275179"}),			clienttasks.command+" subscribe --quantity=2 --auto",						new Integer(64),	"","Error: --quantity may not be used with an auto-attach"}));	// added by bug 1275179 commit 281c1e5818eefa89bc73ba438c75494e16afc698
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1275179"}),			clienttasks.command+" subscribe --quantity=2",								new Integer(64),	"","Error: --quantity may not be used with an auto-attach"}));	// added by bug 1275179 commit 281c1e5818eefa89bc73ba438c75494e16afc698
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.18.2-1")) {	// post commit 0d17fb22898be7932331bffdc8cb3526822a3bf8 Disallow empty name for --add
				ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" repo-override --repo=bar --add=\"\":VALUE",			new Integer(2),		clienttasks.command+": error: --add arguments should be in the form of \"name:value\"",	"Usage: subscription-manager repo-override [OPTIONS]"}));
				ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" repo-override --repo=foo --add=:VALUE",				new Integer(2),		clienttasks.command+": error: --add arguments should be in the form of \"name:value\"",	"Usage: subscription-manager repo-override [OPTIONS]"}));
				ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" repo-override --repo=foobar --add :VALUE",			new Integer(2),		clienttasks.command+": error: --add arguments should be in the form of \"name:value\"",	"Usage: subscription-manager repo-override [OPTIONS]"}));
			} else {
				ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" repo-override --repo=bar --add=\"\":VALUE",			new Integer(1),		"","name: may not be null"}));
				ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" repo-override --repo=foo --add=:VALUE",				new Integer(1),		"","name: may not be null"}));
				ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" repo-override --repo=foobar --add :VALUE",			new Integer(1),		"","name: may not be null"}));
			}
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1275179"}),			clienttasks.command+" attach --auto --quantity=2",							new Integer(64),	"","Error: --quantity may not be used with an auto-attach"}));	// added by bug 1275179 commit 281c1e5818eefa89bc73ba438c75494e16afc698
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" subscribe --file=/missing/poolIds.txt",				new Integer(65),	"","Error: The file \"/missing/poolIds.txt\" does not exist or cannot be read."}));	// added by bug 1159974
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.15.9-12")) {
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1246680"}),		clienttasks.rhsmDebugSystemCommand(null,null,null,true,true,null,null,null, null),new Integer(0),	/*"Wrote: /tmp/rhsm-debug-system-\\d+-\\d+.tar.gz"*/null,""}));	// added by bug 1246680 which trumps bug 1194906
			} else if (clienttasks.isPackageVersion("subscription-manager",">=","1.14.1-1")) {
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1194906"}),		clienttasks.rhsmDebugSystemCommand(null,null,null,true,true,null,null,null, null),new Integer(64),	"","Error: You may not use --subscriptions with --no-subscriptions."}));	// added by bug 1194906
			}
		} else {	// pre commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" unsubscribe --product=FOO",							new Integer(2),		clienttasks.command+": error: no such option: --product", "Usage: subscription-manager unsubscribe [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" unsubscribe --regtoken=FOO",							new Integer(2),		clienttasks.command+": error: no such option: --regtoken", "Usage: subscription-manager unsubscribe [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" unsubscribe --pool=FOO",								new Integer(2),		clienttasks.command+": error: no such option: --pool", "Usage: subscription-manager unsubscribe [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("1078091"),clienttasks.command+" register --serverurl=https://sat6_fqdn/ --insecure",	new Integer(255),	"Unable to reach the server at sat6_fqdn:443/", ""}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" register --servicelevel=foo",							new Integer(255),	"Error: Must use --auto-attach with --servicelevel.", ""}));	// changed by bug 874804,876305		ll.add(Arrays.asList(new Object[]{clienttasks.command+" register --servicelevel=foo",				new Integer(255),	"Error: Must use --autosubscribe with --servicelevel.", ""}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("856236"),	clienttasks.command+" register --activationkey=foo --org=foo --env=foo",	new Integer(255),	"Error: Activation keys do not allow environments to be specified.", ""}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" list --installed --servicelevel=foo",					new Integer(255),	"Error: --servicelevel is only applicable with --available or --consumed", ""}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" subscribe",											new Integer(255),	"This system is not yet registered. Try 'subscription-manager register --help' for more information.",	""}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.rhsmDebugSystemCommand(null,null,null,null,null,null,null,null, null),new Integer(255),	"This system is not yet registered. Try 'subscription-manager register --help' for more information.",	""}));
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.3-1")) {
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" list --no-overlap",									new Integer(255),	"Error: --no-overlap is only applicable with --available", ""}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" list --match-installed",								new Integer(255),	"Error: --match-installed is only applicable with --available", ""}));
			}
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.7-1")) {
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --repo",								new Integer(2),		clienttasks.command+": error: --repo option requires an argument",	"Usage: subscription-manager repo-override [OPTIONS]"}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --remove",								new Integer(2),		clienttasks.command+": error: --remove option requires an argument",	"Usage: subscription-manager repo-override [OPTIONS]"}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --add",									new Integer(2),		clienttasks.command+": error: --add option requires an argument",	"Usage: subscription-manager repo-override [OPTIONS]"}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --add=foo",								new Integer(2),		clienttasks.command+": error: --add arguments should be in the form of \"name:value\"",	"Usage: subscription-manager repo-override [OPTIONS]"}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --repo=foo",							new Integer(255),	"Error: The --repo option must be used with --list or --add or --remove.",	""}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --remove=foo",							new Integer(255),	"Error: You must specify a repository to modify",	""}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --add=foo:bar",							new Integer(255),	"Error: You must specify a repository to modify",	""}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --list --remove-all",					new Integer(255),	"Error: You may not use --list with --remove-all",	""}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --remove-all --add=foo:bar --repo=fb",	new Integer(255),	"Error: You may not use --add or --remove with --remove-all and --list",	""}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --remove-all --remove=foo --repo=fb",	new Integer(255),	"Error: You may not use --add or --remove with --remove-all and --list",	""}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --list --remove=foo --repo=fb",			new Integer(255),	"Error: You may not use --add or --remove with --remove-all and --list",	""}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --list --remove=foo --repo=fb",			new Integer(255),	"Error: You may not use --add or --remove with --remove-all and --list",	""}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --remove=foo --repo=foobar",			new Integer(255),	"This system is not yet registered. Try 'subscription-manager register --help' for more information.",	""}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --add=foo:bar --repo=foobar",			new Integer(255),	"This system is not yet registered. Try 'subscription-manager register --help' for more information.",	""}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --remove-all",							new Integer(255),	"This system is not yet registered. Try 'subscription-manager register --help' for more information.",	""}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override --list",								new Integer(255),	"This system is not yet registered. Try 'subscription-manager register --help' for more information.",	""}));
				ll.add(Arrays.asList(new Object[]{null,						clienttasks.command+" repo-override",										new Integer(255),	"This system is not yet registered. Try 'subscription-manager register --help' for more information.",	""}));
			}
			
			// negative tests that require the system to be registered before attempting the test...
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" register --username "+sm_clientUsername+" --password "+sm_clientPassword+(sm_clientOrg==null?"":" --org "+sm_clientOrg),	new Integer(0),	null,	""}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" subscribe",											new Integer(255),	"Error: This command requires that you specify a pool with --pool or use --auto.",	""}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" subscribe --pool=123 --auto",							new Integer(255),	"Error: Only one of --pool or --auto may be used with this command.", ""}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" subscribe --pool=123 --servicelevel=foo",				new Integer(255),	"Error: Must use --auto with --servicelevel.", ""}));
		}
		
		return ll;
	}
}
