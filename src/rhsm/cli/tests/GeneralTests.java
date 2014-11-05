package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author ssalevan
 * @author jsefler
 *
 */
@Test(groups={"GeneralTests","Tier2Tests"})
public class GeneralTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************
	
	
	@Test(	description="subscription-manager-cli: attempt to access functionality without registering",
			groups={"blockedByBug-749332","blockedByBug-1119688"},
			dataProvider="UnregisteredCommandData")
	@ImplementsNitrateTest(caseId=50215)
	public void AttemptingCommandsWithoutBeingRegistered_Test(String command) {
		log.info("Testing subscription-manager-cli command without being registered, expecting it to fail: "+ command);
		clienttasks.unregister(null, null, null);
		
		if (clienttasks.isPackageVersion("subscription-manager", ">=",/*FIXME "1.13.8-1"*/"1.13.7-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			RemoteFileTasks.runCommandAndAssert(client,command,1,null,"^"+clienttasks.msg_ConsumerNotRegistered);			
		} else if (clienttasks.isPackageVersion("subscription-manager", ">=","0.98.4-1")) {					// post commit 6241cd1495b9feac2ed123f60405061b03815721 bug 749332
			RemoteFileTasks.runCommandAndAssert(client,command,255,"^"+clienttasks.msg_ConsumerNotRegistered,null);
		} else {
			RemoteFileTasks.runCommandAndAssert(client,command,1,"^Error: You need to register this system by running `register` command before using this option.",null);
		}
	}
	
	
	@Test(	description="subscription-manager-cli: attempt to access functionality that does not exist",
			groups={"blockedByBug-1098308"},
			dataProvider="NegativeFunctionalityData")
	public void AttemptingCommandsThatAreInvalid_Test(Object blockedByBug, String command, Integer expectedExitCode, String expectedStdout, String expectedStderr) {
		log.info("Testing subscription-manager-cli command that is invalid, expecting it to fail: "+ command);
		SSHCommandResult result = client.runCommandAndWait(command);
		if (expectedExitCode!=null)	Assert.assertEquals(result.getExitCode(), expectedExitCode, "The expected exit code.");
		if (expectedStdout!=null)	Assert.assertEquals(result.getStdout().trim(), expectedStdout, "The expected stdout message.");
		if (expectedStderr!=null)	Assert.assertEquals(result.getStderr().trim(), expectedStderr, "The expected stderr message.");
	}
	
	
	@Test(	description="assert the exit code from service rhsmcertd status when running and stopped",
			groups={"blockedByBug-895263","blockedByBug-824680","blockedByBug-842464"})
	public void VerifyExitCodeStatusForRhmscertd_Test() {
		Integer expectedStoppedStatus = new Integer(3);
		Integer expectedRunningStatus = new Integer(0);
		log.info("When service "+clienttasks.rhsmCertD+" is stopped, the expected service status exit code is: "+expectedStoppedStatus);
		log.info("When service "+clienttasks.rhsmCertD+" is running, the expected service status exit code is: "+expectedRunningStatus);
		RemoteFileTasks.runCommandAndAssert(client, "service "+clienttasks.rhsmCertD+" stop  && service "+clienttasks.rhsmCertD+" status", new Integer(3));
		RemoteFileTasks.runCommandAndAssert(client, "service "+clienttasks.rhsmCertD+" start && service "+clienttasks.rhsmCertD+" status", new Integer(0));
	}
	
	
	@Test(	description="assert the exit code from service rhsmcertd status when running and stopped",
			groups={"AcceptanceTests","Tier1Tests","blockedByBug-913118","blockedByBug-912707","blockedByBug-914113"})
	protected void verifyRhsmcertdDoesNotThrowDeprecationWarnings_Test() throws JSONException, Exception {
		clienttasks.unregister(null, null, null);
		String marker = System.currentTimeMillis()+" Testing verifyRhsmcertdDoesNotThrowDeprecationWarnings_Test...";
		RemoteFileTasks.markFile(client, clienttasks.messagesLogFile, marker);
		
		String command = clienttasks.rhsmComplianceD+" -s";
		SSHCommandResult result = client.runCommandAndWait(command);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0),"ExitCode from command '"+command+"'.");
		Assert.assertTrue(result.getStdout().isEmpty(),"Stdout from command '"+command+"' is empty.");
		Assert.assertTrue(result.getStderr().isEmpty(),"Stderr from command '"+command+"' is empty.");
		
		String rhsmcertdLogResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.messagesLogFile, marker, null).trim();
		String expectedMessage = "In order for Subscription Manager to provide your system with updates, your system must be registered with the Customer Portal. Please enter your Red Hat login to ensure your system is up-to-date.";
		Assert.assertTrue(rhsmcertdLogResult.contains(expectedMessage),"Syslog contains expected message '"+expectedMessage+"'.");
		String unexpectedMessage = "DeprecationWarning";
		Assert.assertTrue(!rhsmcertdLogResult.contains(unexpectedMessage),"Syslog does not contain message '"+unexpectedMessage+"'.");
		
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,null,null,null,null,null);
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
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0),"ExitCode from command '"+command+"'.");
		Assert.assertTrue(result.getStdout().isEmpty(),"Stdout from command '"+command+"' is empty.");
		Assert.assertTrue(result.getStderr().isEmpty(),"Stderr from command '"+command+"' is empty.");
	}
	
	
	@Test(	description="assert rhsmd is logged to both /var/log/rhsm/rhsm.log and /var/log/messages",
			groups={"blockedbyBug-976868"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRhsmdForceSignalsToRhsmlogAndSyslog_Test() {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,null,null,null,null,null);
		
		Map<String,String> signalMap = new HashMap<String,String>();
		signalMap.put("valid", "");
		signalMap.put("expired", "This system is missing one or more subscriptions. Please run subscription-manager for more information.");
		signalMap.put("warning", "This system's subscriptions are about to expire. Please run subscription-manager for more information.");
		signalMap.put("partial", "This system is missing one or more subscriptions to fully cover its products. Please run subscription-manager for more information.");
		
		for (String signal : signalMap.keySet()) {
			String command = clienttasks.rhsmComplianceD+" -s -d -i -f "+signal;
			String marker = System.currentTimeMillis()+" Testing VerifyRhsmdLogsToRhsmlogAndSyslog_Test...";
			RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, marker);
			RemoteFileTasks.markFile(client, clienttasks.messagesLogFile, marker);

			// run and verify the command result
			String expectedStdout = "forcing status signal from cli arg";
			RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0), expectedStdout, null);
			
			// verify the logs
			sleep(1000);	// give the message thread time to be logged
			String logResult;
			if (signalMap.get(signal).isEmpty()) {
				String unExpectedMessage = "Please run subscription-manager for more information.";
				logResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.messagesLogFile, marker, null).trim();
				Assert.assertTrue(!logResult.contains(unExpectedMessage),clienttasks.messagesLogFile+" does NOT contain message '"+unExpectedMessage+"'.");
				logResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, marker, null).trim();
				Assert.assertTrue(!logResult.contains(unExpectedMessage),clienttasks.rhsmLogFile+" does NOT contain message '"+unExpectedMessage+"'.");
				Assert.assertTrue(logResult.contains(expectedStdout),clienttasks.rhsmLogFile+" contains expected message '"+expectedStdout+"'.");

			} else {
				String expectedMessage = signalMap.get(signal);
				logResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.messagesLogFile, marker, null).trim();
				Assert.assertTrue(logResult.contains(expectedMessage),clienttasks.messagesLogFile+" contains expected message '"+expectedMessage+"'.");
				logResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, marker, null).trim();
				Assert.assertTrue(logResult.contains(expectedMessage),clienttasks.rhsmLogFile+" contains expected message '"+expectedMessage+"'.");
				Assert.assertTrue(logResult.contains(expectedStdout),clienttasks.rhsmLogFile+" contains expected message '"+expectedStdout+"'.");
			}
		}
	}
	
	
	@Test(	description="assert permissions on /etc/cron.daily/rhsmd",
			groups={"blockedbyBug-1012566"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyPermissionsOnEtcCronDailyRhsmd_Test() {
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
	
	
	@Test(	description="verify enablement of yum plugin for subscription-manager in /etc/yum/pluginconf.d/subscription-manager.conf ",
			groups={"VerifyYumPluginForSubscriptionManagerEnablement_Test", "blockedByBug-1017354","blockedByBug-1087620"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyYumPluginForSubscriptionManagerEnablement_Test() {
		SSHCommandResult sshCommandResult;
		String stdoutRegex;
		
		// test /etc/yum/pluginconf.d/subscription-manager.conf enabled=1 and enabled=0 (UNREGISTERED)
		clienttasks.unregister(null, null, null);
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled", "1");
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled", "1");

		sshCommandResult = client.runCommandAndWait("yum repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		//	Loaded plugins: product-id, refresh-packagekit, security, subscription-manager
		stdoutRegex = "Loaded plugins:.* subscription-manager";
		Assert.assertTrue(doesStringContainMatches(sshCommandResult.getStdout(), stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForSubscriptionManager+" enabled=1 contains expected regex '"+stdoutRegex+"'.");
		stdoutRegex = "Loaded plugins:.* product-id";
		Assert.assertTrue(doesStringContainMatches(sshCommandResult.getStdout(), stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForProductId+" enabled=1 contains expected regex '"+stdoutRegex+"'.");
			
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled", "0");
		sshCommandResult = client.runCommandAndWait("yum repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		//	Loaded plugins: product-id, refresh-packagekit, security
		stdoutRegex = "Loaded plugins:.* subscription-manager";
		Assert.assertFalse(doesStringContainMatches(sshCommandResult.getStdout(), stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForSubscriptionManager+" enabled=0 does NOT contain expected regex '"+stdoutRegex+"'.");
		stdoutRegex = "Loaded plugins:.* product-id";
		Assert.assertTrue(doesStringContainMatches(sshCommandResult.getStdout(), stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForProductId+" enabled=1 contains expected regex '"+stdoutRegex+"'.");
		
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled", "0");
		sshCommandResult = client.runCommandAndWait("yum repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		//	Loaded plugins: product-id, refresh-packagekit, security
		stdoutRegex = "Loaded plugins:.* subscription-manager";
		Assert.assertFalse(doesStringContainMatches(sshCommandResult.getStdout(), stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForSubscriptionManager+" enabled=0 does NOT contain expected regex '"+stdoutRegex+"'.");
		stdoutRegex = "Loaded plugins:.* product-id";
		Assert.assertFalse(doesStringContainMatches(sshCommandResult.getStdout(), stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForProductId+" enabled=0 does NOT contain expected regex '"+stdoutRegex+"'.");
		
		sshCommandResult = client.runCommandAndWait("yum repolist --disableplugin=rhnplugin --enableplugin=subscription-manager --enableplugin=product-id"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		//	Loaded plugins: product-id, refresh-packagekit, security, subscription-manager
		stdoutRegex = "Loaded plugins:.* product-id";
		Assert.assertTrue(doesStringContainMatches(sshCommandResult.getStdout(), "Loaded plugins:.* subscription-manager"),"Yum repolist with "+clienttasks.yumPluginConfFileForSubscriptionManager+" enabled=0 (but--enableplugin=subscription-manager) contains expected stdout regex '"+stdoutRegex+"'.");
		stdoutRegex = "Loaded plugins:.* product-id";
		Assert.assertTrue(doesStringContainMatches(sshCommandResult.getStdout(), stdoutRegex),"Yum repolist with "+clienttasks.yumPluginConfFileForProductId+" enabled=0 (but --enableplugin=product-id) contains expected stdout regex '"+stdoutRegex+"'.");
	}
	@AfterGroups(value="VerifyYumPluginForSubscriptionManagerEnablement_Test", alwaysRun=true)
	protected void afterVerifyYumPluginForSubscriptionManagerEnablement_Test() {
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled", "1");
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled", "1");
	}
	
	
	@Test(	description="check the rpm requires list for changes to python-rhsm",
			groups={"blockedByBug-1006748","blockedByBug-800732","blockedByBug-1096676"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRpmRequireListForPythonRhsm_Test() {
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
			if (clienttasks.isPackageVersion("python-rhsm", ">=", "1.11.5.1")) expectedRequiresList.add("manual: python-dateutil");	// Bug 1090350 - Clock skew detected when the dates of server and client have no big time drift. commit b597dae53aacf2d8a307b77b7f38756ce3ee6860
		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}
	
	
	@Test(	description="check the rpm requires list for changes to subscription-manager",
			groups={"blockedbyBug-801280","blockedByBug-1006748","blockedByBug-800744","blockedByBug-1080531"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRpmRequireListForSubscriptionManager_Test() {
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
			if		(clienttasks.isPackageVersion("subscription-manager",">=","1.11.3-10"))	expectedRequiresList.add("python-rhsm >= 1.11.3-5");	// RHEL5.11	subscription-manager commit 0a0135def87aa2a9c44658b31c705e33247b0560	1104498: Add hack to avoid dateutil from anaconda
			else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.11.3-7"))	expectedRequiresList.add("python-rhsm >= 1.11.3-4");	// RHEL5.11
			else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.11.3-6"))	expectedRequiresList.add("python-rhsm >= 1.11.3-3");	// RHEL5.11
			else if (clienttasks.isPackageVersion("subscription-manager",">=","1.11.3-1"))	expectedRequiresList.add("python-rhsm >= 1.11.3-2");	// RHEL5.11
			else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.8.22-1"))	expectedRequiresList.add("python-rhsm >= 1.8.16-1");	// RHEL5.10
			else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.0.13-1"))	expectedRequiresList.add("python-rhsm >= 1.0.5");		// RHEL5.9
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
			if		(clienttasks.isPackageVersion("subscription-manager",">=","1.10.5-1"))	expectedRequiresList.remove("manual: python-simplejson");		// Bug 1006748 - remove subscription-manager dependency on python-simplejson; subscription-manager commit ee34aef839d0cb367e558f1cd7559590d95cd636
			if		(clienttasks.isPackageVersion("subscription-manager",">=","1.12.3-1"))	expectedRequiresList.add("manual: python-rhsm >= 1.12.3");		// RHEL6.6
			else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.9.2-1"))	expectedRequiresList.add("manual: python-rhsm >= 1.9.1-1");		// RHEL6.5
			else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.8.12-1"))	expectedRequiresList.add("manual: python-rhsm >= 1.8.13-1");	// RHEL6.4

		}
		if (clienttasks.redhatReleaseX.equals("7")) {
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					"post: systemd",	//"post: systemd-units",	// changed for rhel7 by commit f67310381587a96a37933abf22985b97de373887
					"preun: systemd",	//"preun: systemd-units",	// changed for rhel7 by commit f67310381587a96a37933abf22985b97de373887
					"postun: systemd",	//"postun: systemd-units",	// changed for rhel7 by commit f67310381587a96a37933abf22985b97de373887
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
			if		(clienttasks.isPackageVersion("subscription-manager",">=","1.10.5-1"))	expectedRequiresList.remove("manual: python-simplejson");		// Bug 1006748 - remove subscription-manager dependency on python-simplejson; subscription-manager commit ee34aef839d0cb367e558f1cd7559590d95cd636

			if		(clienttasks.isPackageVersion("subscription-manager",">=","1.13.6-1"))	expectedRequiresList.add("manual: python-rhsm >= 1.13.5");		// RHEL7.1
			else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.12.3-1"))	expectedRequiresList.add("manual: python-rhsm >= 1.12.3");		// RHEL7.1
			else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.10.14-6"))	expectedRequiresList.add("manual: python-rhsm >= 1.10.12-2");	// RHEL7.0	// Bug 1080531 - subscription-manager-1.10.14-6 should require python-rhsm >= 1.10.12-2
			else if	(clienttasks.isPackageVersion("subscription-manager",">=","1.10.9-1"))	expectedRequiresList.add("manual: python-rhsm >= 1.10.9");		// RHEL7.0
		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}
	
	
	@Test(	description="check the rpm requires list for changes to subscription-manager-gui",
			groups={"blockedbyBug-1004908"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRpmRequireListForSubscriptionManagerGui_Test() {
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
//			Assert.assertTrue(actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' contains the expected list "+expectedRequiresList);
//			return;
		}
		if (clienttasks.redhatReleaseX.equals("6")) {
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
		}
		if (clienttasks.redhatReleaseX.equals("7")) {
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
		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}
	
	
	@Test(	description="check the rpm requires list for changes to subscription-manager-firstboot",
			groups={"blockedbyBug-1004908"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRpmRequireListForSubscriptionManagerFirstboot_Test() {
		String pkg = "subscription-manager-firstboot";
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
	
	
	@Test(	description="check the rpm requires list for changes to subscription-manager-migration",
			groups={"blockedByBug-1049037"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRpmRequireListForSubscriptionManagerMigration_Test() {
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
	
	
	@Test(	description="check the rpm requires list for changes to subscription-manager-migration-data",
			groups={},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRpmRequireListForSubscriptionManagerMigrationData_Test() {
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
	
	
	@Test(	description="check the rpm requires list for changes to subscription-manager-plugin-ostree",
			groups={},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRpmRequireListForSubscriptionManagerPluginOstree_Test() {
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
					"manual: python-iniparse >= 0.4"
			}));
		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}
	
	
	@Test(	description="check the rpm requires list for changes to subscription-manager-plugin-container",
			groups={},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRpmRequireListForSubscriptionManagerPluginContainer_Test() {
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
		if (Integer.valueOf(clienttasks.redhatReleaseX)<7) {
			Assert.fail("Did not expect package '"+pkg+"' to be installed on RHEL release '"+clienttasks.redhatReleaseX+"'.");
		}
		if (clienttasks.redhatReleaseX.equals("7")) {
			expectedRequiresList.addAll(Arrays.asList(new String[]{
					//none
			}));
		}
		
		for (String expectedRequires : expectedRequiresList) if (!actualRequiresList.contains(expectedRequires)) log.warning("The actual requires list is missing expected requires '"+expectedRequires+"'.");
		for (String actualRequires : actualRequiresList) if (!expectedRequiresList.contains(actualRequires)) log.warning("The expected requires list does not include the actual requires '"+actualRequires+"'  Is this a new requirement?");
		Assert.assertTrue(expectedRequiresList.containsAll(actualRequiresList) && actualRequiresList.containsAll(expectedRequiresList), "The actual requires list of packages for '"+pkg+"' matches the expected list "+expectedRequiresList);
	}
	
	
	@Test(	description="When the client is 1 hour or more (normalized for time zone and daylight savings time) ahead of candlepin's clock, verify that a WARNING is logged to rhsm.log",
			groups={"VerifyPositiveClockSkewDetection_Test","blockedByBug-772936","blockedByBug-1090350"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyPositiveClockSkewDetection_Test() {
		client.runCommandAndWait("rm -f "+clienttasks.rhsmLogFile);	// remove it because it occasionally gets backed up to rhsm.log.1 in the midst of a pair of calls to RemoteFileTasks.markFile(...) and RemoteFileTasks.getTailFromMarkedFile(...)
		clienttasks.unregister(null, null, null);	// do not need to be registered for this test
		
		String rhsmLogMarker = System.currentTimeMillis()+" Testing clock skew detection...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, rhsmLogMarker);
		log.info("Calling subscription-manager version to trigger communication to the currently configured candlepin server...");
		clienttasks.version(null, null, null);
		String rhsmLogResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, rhsmLogMarker, null).trim();
		Assert.assertTrue(!rhsmLogResult.contains(clienttasks.msg_ClockSkewDetection), "Assuming the rhsm client is less than 60 minutes ahead of the candlepin server, WARNING '"+clienttasks.msg_ClockSkewDetection+"' is NOT logged to '"+clienttasks.rhsmLogFile+"'.");

		rhsmLogMarker = System.currentTimeMillis()+" Testing positive clock skew detection...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, rhsmLogMarker);
		positiveClockSkewMinutes = new Integer(119); // to catch bug 1090350, test with skew 1 min less than 1 hour skew limit + 1 hour daylight saving	// the clock skew limit is set to 1 hour in src/rhsm/connection.py def drift_check(utc_time_string, hours=1)
		log.info("Advancing the rhsm client clock ahead by '"+positiveClockSkewMinutes+"' minutes...");
		client.runCommandAndWait(String.format("date -s +%dminutes", positiveClockSkewMinutes));
		log.info("Calling subscription-manager version to trigger communication to the currently configured candlepin server...");
		clienttasks.version(null, null, null);
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
	
	
	@Test(	description="When the client is 1 hour or more (normalized for time zone and daylight savings time) behind candlepin's clock, verify that a WARNING is logged to rhsm.log",
			groups={"VerifyNegativeClockSkewDetection_Test","blockedByBug-772936","blockedByBug-1090350"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyNegativeClockSkewDetection_Test() {
		client.runCommandAndWait("rm -f "+clienttasks.rhsmLogFile);	// remove it because it occasionally gets backed up to rhsm.log.1 in the midst of a pair of calls to RemoteFileTasks.markFile(...) and RemoteFileTasks.getTailFromMarkedFile(...)
		clienttasks.unregister(null, null, null);	// do not need to be registered for this test
		
		String rhsmLogMarker = System.currentTimeMillis()+" Testing clock skew detection...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, rhsmLogMarker);
		log.info("Calling subscription-manager version to trigger communication to the currently configured candlepin server...");
		clienttasks.version(null, null, null);
		String rhsmLogResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, rhsmLogMarker, null).trim();
		Assert.assertTrue(!rhsmLogResult.contains(clienttasks.msg_ClockSkewDetection), "Assuming the rhsm client is less than 60 minutes behind of the candlepin server, WARNING '"+clienttasks.msg_ClockSkewDetection+"' is NOT logged to '"+clienttasks.rhsmLogFile+"'.");

		rhsmLogMarker = System.currentTimeMillis()+" Testing negative clock skew detection...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, rhsmLogMarker);
		negativeClockSkewMinutes = new Integer(119); // to catch bug 1090350, test with skew 1 min less than 1 hour skew limit + 1 hour daylight saving	// the clock skew limit is set to 1 hour in src/rhsm/connection.py def drift_check(utc_time_string, hours=1)
		log.info("Retarding the rhsm client clock behind by '"+negativeClockSkewMinutes+"' minutes...");
		client.runCommandAndWait(String.format("date -s -%dminutes", negativeClockSkewMinutes));
		log.info("Calling subscription-manager version to trigger communication to the currently configured candlepin server...");
		clienttasks.version(null, null, null);
		rhsmLogResult = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, rhsmLogMarker, null).trim();
		// NOTE:  If the candlepin server was newly deployed within the last 119 minutes, then a "SSLError: certificate verify failed" will be thrown.  Skip the test if this happens.
		if (!rhsmLogResult.contains(clienttasks.msg_ClockSkewDetection) && rhsmLogResult.contains("SSLError: certificate verify failed")) throw new SkipException("Assuming the candlepin server was recently deployed within the last hour because an SSLError was thrown instead of clock skew detection.");
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
		
		// String command, int expectedExitCode, String expectedStdoutRegex, String expectedStderrRegex
		
		// negative tests that require the system to be unregistered first...
		// Object blockedByBug, String command, Integer expectedExitCode, String expectedStdout, String expectedStderr
		clienttasks.unregister(null,null,null);
		
		
		if (clienttasks.isPackageVersion("subscription-manager", ">=",/*FIXME "1.13.8-1"*/"1.13.7-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" unsubscribe --product=FOO",							new Integer(2),		clienttasks.command+": error: no such option: --product", "Usage: subscription-manager unsubscribe [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" unsubscribe --regtoken=FOO",							new Integer(2),		clienttasks.command+": error: no such option: --regtoken", "Usage: subscription-manager unsubscribe [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" unsubscribe --pool=FOO",								new Integer(2),		clienttasks.command+": error: no such option: --pool", "Usage: subscription-manager unsubscribe [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1078091","1119688"}),clienttasks.command+" register --serverurl=https://sat6_fqdn/ --insecure",	new Integer(69),	"","Unable to reach the server at sat6_fqdn:443/"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" register --servicelevel=foo",							new Integer(64),	"","Error: Must use --auto-attach with --servicelevel."}));	// changed by bug 874804,876305		ll.add(Arrays.asList(new Object[]{clienttasks.command+" register --servicelevel=foo",				new Integer(255),	"Error: Must use --autosubscribe with --servicelevel.", ""}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"856236","1119688"}),	clienttasks.command+" register --activationkey=foo --org=foo --env=foo",	new Integer(64),	"","Error: Activation keys do not allow environments to be specified."}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" list --installed --servicelevel=foo",					new Integer(64),	"","Error: --servicelevel is only applicable with --available or --consumed"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" subscribe",											new Integer(1),		"","This system is not yet registered. Try 'subscription-manager register --help' for more information."}));
			ll.add(Arrays.asList(new Object[]{null,													clienttasks.rhsmDebugSystemCommand(null,null,null,null,null,null,null),		new Integer(255),	"This system is not yet registered. Try 'subscription-manager register --help' for more information.",""}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" list --no-overlap",									new Integer(64),	"","Error: --no-overlap is only applicable with --available"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" list --match-installed",								new Integer(64),	"","Error: --match-installed is only applicable with --available"}));
			ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" repo-override --repo",								new Integer(2),		clienttasks.command+": error: --repo option requires an argument",	"Usage: subscription-manager repo-override [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" repo-override --remove",								new Integer(2),		clienttasks.command+": error: --remove option requires an argument",	"Usage: subscription-manager repo-override [OPTIONS]"}));
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
			
			// negative tests that require the system to be registered before attempting the test...
			ll.add(Arrays.asList(new Object[]{null,													clienttasks.command+" register --username "+sm_clientUsername+" --password "+sm_clientPassword+(sm_clientOrg==null?"":" --org "+sm_clientOrg),	new Integer(0),	null,	""}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" subscribe",											new Integer(64),	"","Error: This command requires that you specify a pool with --pool or use --auto.".replace("with --pool", "with --pool or --file,")}));	// after bug 1159974 Error: This command requires that you specify a pool with --pool or --file, or use --auto.
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" subscribe --pool=123 --auto",							new Integer(64),	"","Error: --auto may not be used when specifying pools."/*"Error: Only one of --pool or --auto may be used with this command."*/}));	// message changed by commit 3167333fc3a261de939f4aa0799b4283f2b9f4d2
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" subscribe --pool=123 --servicelevel=foo",				new Integer(64),	"","Error: Must use --auto with --servicelevel."}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" subscribe --pool=123 --servicelevel=foo",				new Integer(64),	"","Error: Must use --auto with --servicelevel."}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1119688"}),			clienttasks.command+" subscribe --file=/missing/poolIds.txt",				new Integer(65),	"","Error: The file \"/missing/poolIds.txt\" does not exist or cannot be read."}));	// added by bug 1159974

		} else {	// pre commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" unsubscribe --product=FOO",							new Integer(2),		clienttasks.command+": error: no such option: --product", "Usage: subscription-manager unsubscribe [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" unsubscribe --regtoken=FOO",							new Integer(2),		clienttasks.command+": error: no such option: --regtoken", "Usage: subscription-manager unsubscribe [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" unsubscribe --pool=FOO",								new Integer(2),		clienttasks.command+": error: no such option: --pool", "Usage: subscription-manager unsubscribe [OPTIONS]"}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("1078091"),clienttasks.command+" register --serverurl=https://sat6_fqdn/ --insecure",	new Integer(255),	"Unable to reach the server at sat6_fqdn:443/", ""}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" register --servicelevel=foo",							new Integer(255),	"Error: Must use --auto-attach with --servicelevel.", ""}));	// changed by bug 874804,876305		ll.add(Arrays.asList(new Object[]{clienttasks.command+" register --servicelevel=foo",				new Integer(255),	"Error: Must use --autosubscribe with --servicelevel.", ""}));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("856236"),	clienttasks.command+" register --activationkey=foo --org=foo --env=foo",	new Integer(255),	"Error: Activation keys do not allow environments to be specified.", ""}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" list --installed --servicelevel=foo",					new Integer(255),	"Error: --servicelevel is only applicable with --available or --consumed", ""}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" subscribe",											new Integer(255),	"This system is not yet registered. Try 'subscription-manager register --help' for more information.",	""}));
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.rhsmDebugSystemCommand(null,null,null,null,null,null,null),		new Integer(255),	"This system is not yet registered. Try 'subscription-manager register --help' for more information.",	""}));
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
			ll.add(Arrays.asList(new Object[]{null,							clienttasks.command+" subscribe --pool=123 --servicelevel=foo",				new Integer(255),	"Error: Must use --auto with --servicelevel.", ""}));
		}
		
		return ll;
	}
}
