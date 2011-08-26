package com.redhat.qe.sm.cli.tests;

import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.tools.RemoteFileTasks;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"SELinuxTests"})
public class SELinuxTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="assert that no SELinux denials were logged during this TestSuite",
			groups={"AcceptanceTests"/*, "blockedByBug-694879"*/},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyNoSELinuxDenialsWereLogged_Test() {
		log.info("Assuming this test is being executed last in the TestNG Suite...");
		if (client1!=null) Assert.assertTrue(RemoteFileTasks.getTailFromMarkedFile(client1, client1tasks.varLogAuditFile, selinuxSuiteMarker, "denied").trim().equals(""), "No SELinux denials found in the audit log on client "+client1.getConnection().getHostname()+" while executing this test suite.");
		if (client2!=null) Assert.assertTrue(RemoteFileTasks.getTailFromMarkedFile(client2, client2tasks.varLogAuditFile, selinuxSuiteMarker, "denied").trim().equals(""), "No SELinux denials found in the audit log on client "+client2.getConnection().getHostname()+" while executing this test suite.");
	}
	
	
	
	// Configuration methods ***********************************************************************
	
	
	
	// Protected methods ***********************************************************************
	
	
	
	// Data Providers ***********************************************************************

}
