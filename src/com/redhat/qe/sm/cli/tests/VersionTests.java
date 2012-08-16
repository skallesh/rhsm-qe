package com.redhat.qe.sm.cli.tests;

import java.util.List;

import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.jul.TestRecords;
import com.redhat.qe.sm.base.CandlepinType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 */
@Test(groups={"VersionTests"})
public class VersionTests extends SubscriptionManagerCLITestScript {

	// SAMPLE RESULTS
	//	[root@jsefler-rhel59 ~]# subscription-manager version
	//	remote entitlement server: Unknown
	//	remote entitlement server type: subscription management service
	//	subscription-manager: 1.0.9-1.git.37.53fde9a.el5
	//	python-rhsm: 1.0.3-1.git.2.47dc8f4.el5
	
	
	
	// Test methods ***********************************************************************

	@Test(	description="assert that the installed version of subscription-manager is reported by the subscription-manager version module ",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfSubscriptionManager_Test() {
		
		// get the expected results for subscription-manager rpm version
		String expectedReport = client.runCommandAndWait("rpm -q subscription-manager --queryformat '%{NAME}: %{VERSION}-%{RELEASE}'").getStdout().trim();
		
		// get the actual version results from subscription-manager
		SSHCommandResult actualResult = clienttasks.version();
		
		// assert results
		Assert.assertTrue(actualResult.getStdout().contains(expectedReport),"The version report contains the expected string '"+expectedReport+"'");
	}
	
	
	@Test(	description="assert that the installed version of python-rhsm is reported by the subscription-manager version module ",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfPythonRhsm_Test() {
		
		// get the expected results for python-rhsm rpm version
		String expectedReport = client.runCommandAndWait("rpm -q python-rhsm --queryformat '%{NAME}: %{VERSION}-%{RELEASE}'").getStdout().trim();
		
		// get the actual version results from subscription-manager
		SSHCommandResult actualResult = clienttasks.version();

		// assert results
		Assert.assertTrue(actualResult.getStdout().contains(expectedReport),"The version report contains the expected string '"+expectedReport+"'");
	}
	
	
	@Test(	description="assert that the candlepin sever version is reported by the version module (expect Unknown when not registered)",
			groups={"blockedByBug-843191"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfCandlepinWhenUnregistered_Test() {

		// make sure we are not registered
		clienttasks.unregister(null, null, null);
		
		// get the expected results
		String expectedVersion	= "remote entitlement server: "+"Unknown";	// changed by bug 846834
		expectedVersion			= "registered to: "+"Unknown";
		String expectedType		= "remote entitlement server type: "+"Unknown";	// changed by bug 846834
		expectedType			= "server type: "+"Unknown";
		
		// get the actual version results from subscription-manager
		SSHCommandResult actualResult = clienttasks.version();

		// assert results
		Assert.assertTrue(actualResult.getStdout().contains(expectedVersion),"The version report contains the expected string '"+expectedVersion+"'");
		Assert.assertTrue(actualResult.getStdout().contains(expectedType),"The version report contains the expected string '"+expectedType+"'");
	}
	
	
	@Test(	description="assert that the candlepin sever version and type are reported by the subscription-manager version module",
			groups={/*blockedByBug-843649*/"AcceptanceTests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfCandlepinWhenRegistered_Test() {
		
		// make sure we are registered
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (List<String>)null, null, null, null, null, null, null, null);

		// get the expected results
		String expectedVersion, expectedType;
		expectedVersion = "remote entitlement server: "+servertasks.statusVersion;	// changed by bug 846834
		expectedVersion = "registered to: "+servertasks.statusVersion;
		expectedType = "FIXME_TEST: UNKNOWN CANDLEPIN TYPE";
		if (sm_serverType==CandlepinType.standalone)	expectedType = "subscription management service";
		if (sm_serverType==CandlepinType.hosted)		expectedType = "subscription management service";	// TODO not sure if this is correct
		if (sm_serverType==CandlepinType.sam)			expectedType = "SAM";		// TODO not sure if this is correct
		if (sm_serverType==CandlepinType.katello)		expectedType = "Katello";	// TODO not sure if this is correct
		//expectedType = "remote entitlement server type: "+expectedType;	// changed with bug 846834
		expectedType = "server type: "+expectedType;
		
		// get the actual version results from subscription-manager
		SSHCommandResult actualResult = clienttasks.version();
		
		// assert results
		Assert.assertTrue(actualResult.getStdout().contains(expectedVersion),"The version report contains the expected string '"+expectedVersion+"'");
		Assert.assertTrue(actualResult.getStdout().contains(expectedType),"The version report contains the expected string '"+expectedType+"'");
	}
	
	
	@Test(	description="assert the sever version and type when registered to RHN Classic ",
			groups={"VersionOfServerWhenRegisteredUsingRHNClassic_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfServerWhenRegisteredUsingRHNClassic_Test() {

		// make sure we are unregistered
		clienttasks.unregister(null,null,null);
		
		// simulate registration to RHN Classic by creating a /etc/sysconfig/rhn/systemid
		log.info("Simulating registration to RHN Classic by creating an empty systemid file '"+clienttasks.rhnSystemIdFile+"'...");
		RemoteFileTasks.runCommandAndWait(client, "touch "+clienttasks.rhnSystemIdFile, TestRecords.action());
		Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.rhnSystemIdFile), "RHN Classic systemid file '"+clienttasks.rhnSystemIdFile+"' is in place.");
		
		// get the expected results
		String expectedVersion, expectedType;
		expectedVersion	= "remote entitlement server: "+"Unknown";	// changed by bug 846834
		expectedVersion	= "registered to: "+"Unknown";
		expectedType	= "remote entitlement server type: "+"RHN Classic";	// changed by bug 846834
		expectedType	= "server type: "+"RHN Classic";
		
		// get the actual version results from subscription-manager
		SSHCommandResult actualResult = clienttasks.version();
		
		// assert results
		Assert.assertTrue(actualResult.getStdout().contains(expectedVersion),"The version report contains the expected string '"+expectedVersion+"'");
		Assert.assertTrue(actualResult.getStdout().contains(expectedType),"The version report contains the expected string '"+expectedType+"'");
	}
	@AfterGroups(groups={"setup"},value="VersionOfServerWhenRegisteredUsingRHNClassic_Test")
	public void afterVersionOfServerWhenRegisteredUsingRHNClassic_Test() {
		client.runCommandAndWait("rm -rf "+clienttasks.rhnSystemIdFile);;
	}
	
	
	@Test(	description="assert that no errors are reported while executing version module while registered and unregistered",
			groups={"blockedByBug-848409"},
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
		clienttasks.unregister(null,null,null);
		SSHCommandResult versionResult = clienttasks.version();
		Assert.assertTrue(!versionResult.getStdout().contains(error),"Stdout from the version report does NOT contain an '"+error+"' message (while unregistered).");
		Assert.assertTrue(!versionResult.getStderr().contains(error),"Stderr from the version report does NOT contain an '"+error+"' message (while unregistered).");
		
		// assert results from version do not contain an error (while registered)
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (List<String>)null, null, null, null, null, null, null, null);
		versionResult = clienttasks.version();
		Assert.assertTrue(!versionResult.getStdout().contains(error),"Stdout from the version report does NOT contain an '"+error+"' message (while registered).");
		Assert.assertTrue(!versionResult.getStderr().contains(error),"Stderr from the version report does NOT contain an '"+error+"' message (while registered).");
	
	}
	
	
	
	// Candidates for an automated Test:
	
	
	
	// Configuration methods ***********************************************************************

	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************


}
