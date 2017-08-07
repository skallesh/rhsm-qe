package rhsm.cli.tests;

import java.util.Arrays;
import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.jul.TestRecords;
import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.LinkedItem;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;

/**
 * @author jsefler
 *
 */
@Test(groups={"VersionTests","Tier2Tests"})
public class VersionTests extends SubscriptionManagerCLITestScript {
	
	// SAMPLE RESULTS
	//	[root@jsefler-rhel59 ~]# subscription-manager version
	//	remote entitlement server: Unknown
	//	remote entitlement server type: subscription management service
	//	subscription-manager: 1.0.9-1.git.37.53fde9a.el5
	//	python-rhsm: 1.0.3-1.git.2.47dc8f4.el5
	
	
	
	// Test methods ***********************************************************************

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-25822", "RHEL7-51282"})
	@Test(	description="assert that the installed version of subscription-manager is reported by the subscription-manager version module ",
			groups={"blockedByBug-1241184","blockedByBug-1284120"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfSubscriptionManager_Test() {
		
		// get the expected results for subscription-manager rpm version
		String expectedReport = client.runCommandAndWait("rpm -q subscription-manager --queryformat '%{NAME}: %{VERSION}-%{RELEASE}'").getStdout().trim();
		
		// get the actual version results from subscription-manager
		SSHCommandResult actualResult = clienttasks.version(null, null, null, null);
		
		// assert results
		Assert.assertTrue(actualResult.getStdout().contains(expectedReport),"The version report contains the expected string '"+expectedReport+"'");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-25819", "RHEL7-51279"})
	@Test(	description="assert that the installed version of python-rhsm is reported by the subscription-manager version module ",
			groups={"blockedByBug-1284120"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfPythonRhsm_Test() {
		
		// get the expected results for python-rhsm rpm version
		String expectedReport = client.runCommandAndWait("rpm -q python-rhsm --queryformat '%{NAME}: %{VERSION}-%{RELEASE}'").getStdout().trim();
		
		// get the actual version results from subscription-manager
		SSHCommandResult actualResult = clienttasks.version(null, null, null, null);

		// assert results
		Assert.assertTrue(actualResult.getStdout().contains(expectedReport),"The version report contains the expected string '"+expectedReport+"'");
	}
	
	
	@TestDefinition(//update= true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-25817", "RHEL7-51277"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28549",	// RHSM-REQ : subscription-manager cli version and help information
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84938",	// RHSM-REQ : subscription-manager cli version and help information
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="assert that the candlepin sever version is reported by the version module (expect a message to indicate this system is not registered)",
			groups={"blockedByBug-862308","blockedByBug-868347","blockedByBug-874623","blockedByBug-1284120"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfCandlepinWhenUnregistered_Test() {

		// make sure we are not registered
		clienttasks.unregister(null, null, null, null);
		
		String expectedType = "Red Hat Subscription Management";
		// override where needed
		/*
		if (sm_serverType==CandlepinType.standalone)	expectedType = "Red Hat Subscription Management";	// "subscription management service"; changed by bug 852328
		if (sm_serverType==CandlepinType.hosted)		expectedType = "Red Hat Subscription Management";	// "subscription management service"; changed by bug 852328
		if (sm_serverType==CandlepinType.sam)			expectedType = "SAM";		// TODO not sure if this is correct
		if (sm_serverType==CandlepinType.katello)		expectedType = "Katello";	// TODO not sure if this is correct
		*/
		//assertServerVersionAndType(servertasks.statusVersion, expectedType);	// valid prior to bug 874623
		assertServerVersionAndType(servertasks.statusVersion,servertasks.statusRelease,"This system is currently not registered.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-25816", "RHEL7-51276"})
	@Test(	description="assert that the candlepin sever version is reported when not registered AND hostname is bogus (expect a message to indicate this system is not registered)",
			groups={"VersionOfCandlepinWhenUnregisteredAndHostnameIsUnknown_Test","blockedByBug-843191","blockedByBug-874623","blockedByBug-1284120"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfCandlepinWhenUnregisteredAndHostnameIsUnknown_Test() {

		// make sure we are not registered
		clienttasks.unregister(null, null, null, null);
		
		// invalidate the server hostname
		server_hostname	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname");
		clienttasks.config(null, null, true, new String[]{"server","hostname","UNKNOWN"});
		
		//assertServerVersionAndType("Unknown","Unknown");	// valid prior to bug 874623
		assertServerVersionAndType("Unknown",null,"This system is currently not registered.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-25818", "RHEL7-51278"})
	@Test(	description="assert that the candlepin sever version is reported as Unknown when registered classically AND hostname is bogus",
			groups={"VersionOfCandlepinWhenUsingRHNClassicAndHostnameIsUnknown_Test","blockedByBug-843191","blockedByBug-1284120"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfCandlepinWhenUsingRHNClassicAndHostnameIsUnknown_Test() {

		// make sure we are not registered
		clienttasks.unregister(null, null, null, null);
		
		// invalidate the server hostname
		server_hostname	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname");
		clienttasks.config(null, null, true, new String[]{"server","hostname","UNKNOWN"});
		
		// simulate registration to RHN Classic by creating a /etc/sysconfig/rhn/systemid
		log.info("Simulating registration to RHN Classic by creating an empty systemid file '"+clienttasks.rhnSystemIdFile+"'...");
		RemoteFileTasks.runCommandAndWait(client, "touch "+clienttasks.rhnSystemIdFile, TestRecords.action());
		Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.rhnSystemIdFile), "RHN Classic systemid file '"+clienttasks.rhnSystemIdFile+"' is in place.");
		
		assertServerVersionAndType("Unknown",null,"RHN Classic");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-25815", "RHEL7-49360"})
	@Test(	description="assert that the candlepin sever version and type are reported by the subscription-manager version module",
			groups={"AcceptanceTests","Tier1Tests","blockedByBug-843649","blockedByBug-1284120"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfCandlepinWhenRegistered_Test() {
		
		// TEMPORARY WORKAROUND FOR BUG
		String bugId="843649"; //  Bug 843649 - subscription-manager server version reports Unknown against prod/stage candlepin
		boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen && sm_serverType==CandlepinType.hosted) {
			throw new SkipException("Skipping this test against a hosted (sharded) Candlepin environment while bug "+bugId+" is open.");
		}
		// END OF WORKAROUND
		
		// make sure we are registered
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (List<String>)null, null, null, null, true, null, null, null, null, null);
		
		String expectedType = "Red Hat Subscription Management";	// default	// "subscription management service"; changed by bug 852328
		// override where needed
		/*
		if (sm_serverType==CandlepinType.standalone)	expectedType = "Red Hat Subscription Management";	// "subscription management service"; changed by bug 852328
		if (sm_serverType==CandlepinType.hosted)		expectedType = "Red Hat Subscription Management";	// "subscription management service"; changed by bug 852328
		if (sm_serverType==CandlepinType.sam)			expectedType = "SAM";		// TODO not sure if this is correct
		if (sm_serverType==CandlepinType.katello)		expectedType = "Katello";	// TODO not sure if this is correct
		*/
		assertServerVersionAndType(servertasks.statusVersion,servertasks.statusRelease,expectedType);
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-25820", "RHEL7-51280"})
	@Test(	description="assert the sever version and type when registered to RHN Classic (and simultaneously NOT registered to Subscription Management)",
			groups={"blockedByBug-852328","VersionOfServerWhenUsingRHNClassic_Test","blockedByBug-1284120"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfServerWhenRegisteredAndUsingRHNClassic_Test() {
		
		if (Arrays.asList("6.1","6.2","6.3","5.7","5.8","5.9").contains(clienttasks.redhatReleaseXY)) {
			throw new SkipException("Blocking bugzilla 852328 was fixed in a subsequent release.  Skipping this test since we already know it will fail in RHEL release '"+clienttasks.redhatReleaseXY+"'.");
		}

		// make sure we are registered
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (List<String>)null, null, null, null, null, null, null, null, null, null);
		
		// simulate registration to RHN Classic by creating a /etc/sysconfig/rhn/systemid
		log.info("Simulating registration to RHN Classic by creating an empty systemid file '"+clienttasks.rhnSystemIdFile+"'...");
		RemoteFileTasks.runCommandAndWait(client, "touch "+clienttasks.rhnSystemIdFile, TestRecords.action());
		Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.rhnSystemIdFile), "RHN Classic systemid file '"+clienttasks.rhnSystemIdFile+"' is in place.");
		
		//assertServerVersion("Unknown","RHN Classic and subsciption management service");	// changed by bug 852328
		assertServerVersionAndType(servertasks.statusVersion,servertasks.statusRelease,"RHN Classic and Red Hat Subscription Management");	// changed by bug 852328
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-25821", "RHEL7-51281"})
	@Test(	description="assert the sever version and type when registered to RHN Classic (and simultaneously registered to Subscription Management)",
			groups={"blockedByBug-852328","VersionOfServerWhenUsingRHNClassic_Test","blockedByBug-1284120"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfServerWhenUnregisteredAndUsingRHNClassic_Test() {

		// make sure we are unregistered
		clienttasks.unregister(null,null,null, null);
		
		// simulate registration to RHN Classic by creating a /etc/sysconfig/rhn/systemid
		log.info("Simulating registration to RHN Classic by creating an empty systemid file '"+clienttasks.rhnSystemIdFile+"'...");
		client.runCommandAndWait("touch "+clienttasks.rhnSystemIdFile);
		Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.rhnSystemIdFile), "RHN Classic systemid file '"+clienttasks.rhnSystemIdFile+"' is in place.");
		
		assertServerVersionAndType(servertasks.statusVersion,servertasks.statusRelease,"RHN Classic");
	}
	@AfterGroups(groups={"setup"},value="VersionOfServerWhenUsingRHNClassic_Test")
	public void afterVersionOfServerWhenUsingRHNClassic_Test() {
		if (clienttasks!=null) {
			clienttasks.removeRhnSystemIdFile();
		}
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-25814", "RHEL7-51275"})
	@Test(	description="assert that no errors are reported while executing version module while registered and unregistered",
			groups={"blockedByBug-848409","blockedByBug-1284120"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyNoErrorWhileCheckingServerVersion_Test() {
		
		// from Bug 848409 - Error while checking server version: No such file or directory
		//	[root@jsefler-59server ~]# subscription-manager version
		//	Error while checking server version: No such file or directory
		//	remote entitlement server: Unknown
		//	remote entitlement server type: Unknown
		//	subscription-manager: 1.0.13-1.git.27.2a76fe7.el5

		// assert results from version do not contain an error (while unregistered)
		String error = "Error";
		clienttasks.unregister(null,null,null, null);
		SSHCommandResult versionResult = clienttasks.version(null, null, null, null);
		Assert.assertTrue(!versionResult.getStdout().contains(error),"Stdout from the version report does NOT contain an '"+error+"' message (while unregistered).");
		Assert.assertTrue(!versionResult.getStderr().contains(error),"Stderr from the version report does NOT contain an '"+error+"' message (while unregistered).");
		
		// assert results from version do not contain an error (while registered)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (List<String>)null, null, null, null, null, null, null, null, null, null);
		versionResult = clienttasks.version(null, null, null, null);
		Assert.assertTrue(!versionResult.getStdout().contains(error),"Stdout from the version report does NOT contain an '"+error+"' message (while registered).");
		Assert.assertTrue(!versionResult.getStderr().contains(error),"Stderr from the version report does NOT contain an '"+error+"' message (while registered).");
	
	}
	
	
	
	// Candidates for an automated Test:
	
	
	
	// Configuration methods ***********************************************************************
	@AfterGroups(value={"VersionOfCandlepinWhenUnregisteredAndHostnameIsUnknown_Test","VersionOfCandlepinWhenUsingRHNClassicAndHostnameIsUnknown_Test"}, groups={"setup"})
	public void afterVersionOfCandlepinWhenUsingRHNClassicAndHostnameIsUnknown_Test() {
		if (server_hostname!=null)	clienttasks.config(null,null,true,new String[]{"server","hostname",server_hostname});
	}
	protected String server_hostname;

	
	// Protected methods ***********************************************************************

	protected void assertServerVersionAndType(String serverVersion, String serverRelease, String serverType) {
		// set the expected results
		String expectedVersion, expectedType;
		expectedVersion = "remote entitlement server: ";	// original label
		expectedVersion = "registered to: ";	// label changed by bug 846834
		expectedVersion = "subscription management server: ";	// label changed by bugs 862308 868347 
		expectedVersion += serverVersion;
		if (serverRelease!=null && !serverRelease.isEmpty()) expectedVersion += "-"+serverRelease;
		
		expectedType = "server type: "+serverType;
		
		// get the actual version results from subscription-manager
		SSHCommandResult actualResult = clienttasks.version(null, null, null, null);
		
		// assert results
		Assert.assertTrue(actualResult.getStdout().contains(expectedVersion),"The version report contains the expected string '"+expectedVersion+"'");
		Assert.assertTrue(actualResult.getStdout().contains(expectedType),"The version report contains the expected string '"+expectedType+"'");
	}

	
	// Data Providers ***********************************************************************


}
