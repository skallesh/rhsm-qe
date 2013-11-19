package rhsm.cli.tests;

import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import rhsm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.tools.RemoteFileTasks;

/**
 * @author jsefler
 *
 * Notes: To investigate a denial in the /var/log/audit/audit.log...
 * http://docs.redhat.com/docs/en-US/Red_Hat_Enterprise_Linux/6/html/Security-Enhanced_Linux/sect-Security-Enhanced_Linux-Fixing_Problems-Searching_For_and_Viewing_Denials.html
 * 1. yum install setroubleshoot-server
 * 2. /sbin/aureport -a
 * 3. /sbin/ausearch -m avc
 * 
 */
@Test(groups={"SELinuxTests"})
public class SELinuxTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="assert that no SELinux denials were logged during this TestSuite",
			groups={"AcceptanceTests", "blockedByBug-694879", "blockedByBug-822402"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyNoSELinuxDenialsWereLogged_Test() {
		log.info("Assuming this test is being executed last in the TestNG Suite...");
		if (client1!=null) Assert.assertTrue(RemoteFileTasks.getTailFromMarkedFile(client1, client1tasks.auditLogFile, selinuxSuiteMarker, "denied").trim().equals(""), "No SELinux denials found in the audit log on client "+client1.getConnection().getHostname()+" while executing this test suite.");
		if (client2!=null) Assert.assertTrue(RemoteFileTasks.getTailFromMarkedFile(client2, client2tasks.auditLogFile, selinuxSuiteMarker, "denied").trim().equals(""), "No SELinux denials found in the audit log on client "+client2.getConnection().getHostname()+" while executing this test suite.");
	}
	
	
	
	// Configuration methods ***********************************************************************
	
	
	
	// Protected methods ***********************************************************************
	
	
	
	// Data Providers ***********************************************************************

}
