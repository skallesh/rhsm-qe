package rhsm.cli.tests;

import java.util.List;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.SubscriptionPool;

/**
 * @author ssalevan
 *
 */
@Test(groups={"UnregisterTests"})
public class UnregisterTests extends SubscriptionManagerCLITestScript {
	
	
	// Test Methods ***********************************************************************
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20392", "RHEL7-51410"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(description="unregister the consumer",
			groups={"Tier2Tests","blockedByBug-589626"},
			enabled=true)
	@ImplementsNitrateTest(caseId=46714)
	public void testRegisterSubscribeAndUnregister() {
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		List<SubscriptionPool> availPoolsBeforeSubscribingToAllPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsIndividually();
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		for (SubscriptionPool afterPool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			SubscriptionPool originalPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", afterPool.poolId, availPoolsBeforeSubscribingToAllPools);
			Assert.assertEquals(originalPool.quantity, afterPool.quantity,
				"The subscription quantity count for Pool "+originalPool.poolId+" returned to its original count after subscribing to it and then unregistering from the candlepin server.");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20016", "RHEL7-51411"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(description="unregister should not make unauthorized requests",
			groups={"Tier1Tests","UnregisterShouldNotThrowUnauthorizedRequests_Test","blockedByBug-997935","blockedByBug-1158578","blockedByBug-1207403","blockedByBug-1389559","blockedByBug-1395794"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnregisterShouldNotThrowUnauthorizedRequests() {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.10.1-1")) throw new SkipException("Installed package '"+clienttasks.installedPackageVersionMap.get("subscription-manager")+"' is blockedByBug https://bugzilla.redhat.com/show_bug.cgi?id=997935 which is fixed in subscription-manager-1.10.1-1.");
		
		// TEMPORARY WORKAROUND FOR BUG
		if (clienttasks.isPackageInstalled("subscription-manager-plugin-ostree") && !clienttasks.isPackageInstalled("ostree")) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1185958"; 	// Bug 1185958 - Error looking up OSTree origin file.
			
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				pluginConfDir_forUnregisterShouldNotThrowUnauthorizedRequests_Test = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile,"pluginConfDir");
				ostreeContentPluginEnabled_forUnregisterShouldNotThrowUnauthorizedRequests_Test = clienttasks.getConfFileParameter((pluginConfDir_forUnregisterShouldNotThrowUnauthorizedRequests_Test+"/"+"ostree_content.OstreeContentPlugin.conf").replaceAll("//+","/"),"enabled");
				log.warning("Avoid bug '"+bugId+"' by disabling the 'ostree_content.OstreeContentPlugin'...");
				clienttasks.updateConfFileParameter((pluginConfDir_forUnregisterShouldNotThrowUnauthorizedRequests_Test+"/"+"ostree_content.OstreeContentPlugin.conf").replaceAll("//+","/"), "enabled", "0");
			}
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1351370";	// Bug 1351370 - [ERROR] subscription-manager:31276 @dbus_interface.py:60 - org.freedesktop.DBus.Python.OSError: Traceback
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			// this is a workaround as shown in the ADDTIONAL INFO of Bug 1351370 TO RECOVER FROM A BAD STATE
			SSHCommandResult selinuxModeResult = client.runCommandAndWait("getenforce");	// Enforcing
			client.runCommandAndWait("setenforce Permissive");
			clienttasks.unregister_(null, null, null, null);
			clienttasks.clean_();
			client.runCommandAndWait("setenforce "+selinuxModeResult.getStdout().trim());
		}
		// END OF WORKAROUND
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg);
		String logMarker = System.currentTimeMillis()+" Testing UnregisterShouldNotThrowUnauthorizedRequests_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);
		clienttasks.unregister(null, null, null, null);
		//	[root@jsefler-6 ~]# grep -A1 "Making request" /var/log/rhsm/rhsm.log
		//	2013-09-16 11:53:26,339 [DEBUG]  @connection.py:441 - Making request: DELETE /subscription/consumers/71775a6e-40e0-4422-8db6-5bff074389ef
		//	2013-09-16 11:53:27,045 [DEBUG]  @connection.py:460 - Response status: 204
		
		//	[root@jsefler-7 ~]# grep -A1 "Making request" /var/log/rhsm/rhsm.log
		//	2013-11-04 18:38:43,279 [DEBUG] subscription-manager @connection.py:442 - Making request: DELETE /candlepin/consumers/2f90f0ff-fca5-4425-8f84-6d3d951ac4a7
		//	2013-11-04 18:38:43,504 [DEBUG] subscription-manager @connection.py:465 - Response: status=204, requestUuid=c68cecf5-3972-494a-b3d1-17d8d044d9b8
		
		//	[root@jsefler-os7 ~]# grep -A1 "Making request" /var/log/rhsm/rhsm.log
		//	2014-12-15 13:18:07,585 [DEBUG] rhsmd @connection.py:466 - Making request: GET /subscription/consumers/None/compliance
		//	2014-12-15 13:18:07,887 [DEBUG] rhsmd @connection.py:489 - Response: status=401
		
		String logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Response").trim();
		String unexpectedLogMessage = "Response status: 401";
		if (clienttasks.isPackageVersion("python-rhsm", ">", "1.10.3-1")) unexpectedLogMessage = "Response: status=401";	// changed by python-rhsm commit ce3727f73c5ac6f77db5e52027443ec456a5d733 Log the new requestUuid from candlepin if it is present in the response.
		Assert.assertTrue(!logTail.contains(unexpectedLogMessage), "The '"+clienttasks.rhsmLogFile+"' should not encounter unexpected log message '"+unexpectedLogMessage+"' after unregister.");
		String expectedLogMessage = "Response status: 204";
		if (clienttasks.isPackageVersion("python-rhsm", ">", "1.10.3-1")) expectedLogMessage = "Response: status=204";	// changed by python-rhsm commit ce3727f73c5ac6f77db5e52027443ec456a5d733 Log the new requestUuid from candlepin if it is present in the response.
		Assert.assertTrue(logTail.contains(expectedLogMessage), "The '"+clienttasks.rhsmLogFile+"' should encounter expected log message '"+expectedLogMessage+"' after unregister (indicative of a successful DELETE request).");

		unexpectedLogMessage = "Traceback";
		logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, unexpectedLogMessage).trim();
		if (logTail.contains(unexpectedLogMessage)) RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, null);
		// TEMPORARY WORKAROUND
		/*boolean*/ invokeWorkaroundWhileBugIsOpen = true;
		/*String*/ bugId="1576962";	// Bug 1576962 - org.freedesktop.DBus.Error.Spawn.ExecFailed: Cannot launch daemon, file not found or permissions invalid
		/*String*/ bugId="1555384";	// Bug 1555384 - Broken Exec= line in com.redhat.SubscriptionManager.service 
		try {if (logTail.contains(unexpectedLogMessage) && invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (logTail.contains(unexpectedLogMessage) && invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping the final assert in '"+clienttasks.rhsmLogFile+"' that unexpected log message '"+unexpectedLogMessage+"' should not be encountered during an unregister while bug '"+bugId+"' is open.");
		} else
		// END OF WORKAROUND
		Assert.assertTrue(!logTail.contains(unexpectedLogMessage), "The '"+clienttasks.rhsmLogFile+"' should not encounter unexpected log message '"+unexpectedLogMessage+"' after unregister.");
	}
	String pluginConfDir_forUnregisterShouldNotThrowUnauthorizedRequests_Test=null;
	String ostreeContentPluginEnabled_forUnregisterShouldNotThrowUnauthorizedRequests_Test=null;
	@AfterGroups(groups={"setup"},value="UnregisterShouldNotThrowUnauthorizedRequests_Test")
	public void afterUnregisterShouldNotThrowUnauthorizedRequests() {
		// restore the the "enabled" parameter in /etc/rhsm/pluginconf.d/ostree_content.OstreeContentPlugin.conf
		if (pluginConfDir_forUnregisterShouldNotThrowUnauthorizedRequests_Test!=null) {
			if (ostreeContentPluginEnabled_forUnregisterShouldNotThrowUnauthorizedRequests_Test!=null) {
				clienttasks.updateConfFileParameter((pluginConfDir_forUnregisterShouldNotThrowUnauthorizedRequests_Test+"/"+"ostree_content.OstreeContentPlugin.conf").replaceAll("//+","/"), "enabled", ostreeContentPluginEnabled_forUnregisterShouldNotThrowUnauthorizedRequests_Test);
			}
		}
	}
	
	
	// Candidates for an automated Test:
	// TODO Bug 674652 - Subscription Manager Leaves Broken Yum Repos After Unregister https://github.com/RedHatQE/rhsm-qe/issues/218
	// TODO Bug 706853 - SM Gui “unregister” button deletes “consumer” folder for non network host. https://github.com/RedHatQE/rhsm-qe/issues/219
}
