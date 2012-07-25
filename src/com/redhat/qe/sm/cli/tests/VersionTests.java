package com.redhat.qe.sm.cli.tests;

import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"VersionTests"})
public class VersionTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************

	@Test(	description="assert that the installed version of subscription-manager is reported by the version module ",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfSubscriptionManager_Test() {
		
		// get the expected results for subscription-manager rpm version
		String expectedReport = client.runCommandAndWait("rpm -q subscription-manager --queryformat '%{NAME}: %{VERSION}-%{RELEASE}'").getStdout().trim();
		
		// get the actual version results from subscription-manager
		SSHCommandResult actualResult = clienttasks.version();
		
		//	[root@jsefler-rhel59 ~]# subscription-manager version
		//	remote entitlement server: Unknown
		//	remote entitlement server type: subscription management service
		//	subscription-manager: 1.0.9-1.git.37.53fde9a.el5
		//	python-rhsm: 1.0.3-1.git.2.47dc8f4.el5

		// assert results
		Assert.assertTrue(actualResult.getStdout().contains(expectedReport),"The version report contains the expected string '"+expectedReport+"'");
	}
	
	
	@Test(	description="assert that the installed version of python-rhsm is reported by the version module ",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VersionOfPythonRhsm_Test() {
		
		// get the expected results for python-rhsm rpm version
		String expectedReport = client.runCommandAndWait("rpm -q python-rhsm --queryformat '%{NAME}: %{VERSION}-%{RELEASE}'").getStdout().trim();
		
		// get the actual version results from subscription-manager
		SSHCommandResult actualResult = clienttasks.version();
		
		//	[root@jsefler-rhel59 ~]# subscription-manager version
		//	remote entitlement server: Unknown
		//	remote entitlement server type: subscription management service
		//	subscription-manager: 1.0.9-1.git.37.53fde9a.el5
		//	python-rhsm: 1.0.3-1.git.2.47dc8f4.el5

		// assert results
		Assert.assertTrue(actualResult.getStdout().contains(expectedReport),"The version report contains the expected string '"+expectedReport+"'");
	}
	

	
	
	// Candidates for an automated Test:
	
	
	
	// Configuration methods ***********************************************************************

	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************


}
