package rhsm.cli.tests;

import java.util.Arrays;

import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.OldBzChecker;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.SubscriptionManagerTasks;

import com.redhat.qe.tools.RemoteFileTasks;

/**
 * @author jsefler
 * 
 * SELinux Troubleshooting Chart
 *   http://wiki.test.redhat.com/mmalik/SELinuxTroubleshootingChart
 *   Includes Overview: http://wiki.test.redhat.com/mmalik/SELinuxTroubleshootingChart?action=AttachFile&do=view&target=detailed-slides.pdf
 *   Includes Tutorial: http://wiki.test.redhat.com/mmalik/SELinuxTroubleshootingChart?action=AttachFile&do=view&target=SELinux-for-QA.pdf
 *
 * SELinux HowToTest Instructions
 *   https://wiki.test.redhat.com/BaseOs/Security/SelinuxTestOnlyBugs#SELinuxHowToTestInstructions
 *
 * Notes: To investigate a denial in the /var/log/audit/audit.log...
 * http://docs.redhat.com/docs/en-US/Red_Hat_Enterprise_Linux/6/html/Security-Enhanced_Linux/sect-Security-Enhanced_Linux-Fixing_Problems-Searching_For_and_Viewing_Denials.html
 * 1. yum install setroubleshoot-server
 * 2. /sbin/aureport -a
 * 3. /sbin/ausearch -m avc
 * 
[root@jsefler-7 ~]# grep setroubleshoot /var/log/messages | tail -1
Apr  4 11:43:33 jsefler-7 setroubleshoot: SELinux is preventing /usr/bin/python2.7 from open access on the file . For complete SELinux messages. run sealert -l b9dbf7e8-023d-499f-afdf-1d38bbebf479
[root@jsefler-7 ~]# sealert -l b9dbf7e8-023d-499f-afdf-1d38bbebf479
SELinux is preventing /usr/bin/python2.7 from open access on the file .

*****  Plugin catchall (100. confidence) suggests   **************************

If you believe that python2.7 should be allowed open access on the  file by default.
Then you should report this as a bug.
You can generate a local policy module to allow this access.
Do
allow this access for now by executing:
# grep rhsmcertd-worke /var/log/audit/audit.log | audit2allow -M mypol
# semodule -i mypol.pp


Additional Information:
Source Context                system_u:system_r:rhsmcertd_t:s0
Target Context                unconfined_u:object_r:user_tmp_t:s0
Target Objects                 [ file ]
Source                        rhsmcertd-worke
Source Path                   /usr/bin/python2.7
Port                          <Unknown>
Host                          jsefler-7.usersys.redhat.com
Source RPM Packages           python-2.7.5-16.el7.x86_64
Target RPM Packages           
Policy RPM                    selinux-policy-3.12.1-147.el7.noarch
Selinux Enabled               True
Policy Type                   targeted
Enforcing Mode                Enforcing
Host Name                     jsefler-7.usersys.redhat.com
Platform                      Linux jsefler-7.usersys.redhat.com
                              3.10.0-105.el7.x86_64 #1 SMP Wed Mar 5 11:48:56
                              EST 2014 x86_64 x86_64
Alert Count                   155
First Seen                    2014-03-13 19:19:13 EDT
Last Seen                     2014-04-04 11:44:33 EDT
Local ID                      b9dbf7e8-023d-499f-afdf-1d38bbebf479

Raw Audit Messages
type=AVC msg=audit(1396626273.740:285703): avc:  denied  { open } for  pid=14966 comm="rhsmcertd-worke" path="/usr/lib/python2.7/site-packages/argparse-1.2.1-py2.7.egg" dev="dm-1" ino=3678793 scontext=system_u:system_r:rhsmcertd_t:s0 tcontext=unconfined_u:object_r:user_tmp_t:s0 tclass=file


type=SYSCALL msg=audit(1396626273.740:285703): arch=x86_64 syscall=open success=no exit=EACCES a0=7fffc43d90a0 a1=0 a2=1b6 a3=1 items=0 ppid=10300 pid=14966 auid=4294967295 uid=0 gid=0 euid=0 suid=0 fsuid=0 egid=0 sgid=0 fsgid=0 tty=(none) ses=4294967295 comm=rhsmcertd-worke exe=/usr/bin/python2.7 subj=system_u:system_r:rhsmcertd_t:s0 key=(null)

Hash: rhsmcertd-worke,rhsmcertd_t,user_tmp_t,file,open

[root@jsefler-7 ~]# 

 * To fix the above avc denial, run the following...   Reference: https://bugzilla.redhat.com/show_bug.cgi?id=1084557#c3
 * 4. restorecon -Rv /usr/lib/python2.7/site-packages
 *
 *
 *
 *
Another example of setting a Temporary Policy is in https://bugzilla.redhat.com/show_bug.cgi?id=1351370#c7
In this example, the context for a /tmp path is changed after setting the rhsm.conf productCertDir to /tmp/product-default/
[root@jsefler-rhel7 ~]# semanage fcontext -a -t cert_t -s system_u "/tmp/product-default(/.*)?"
[root@jsefler-rhel7 ~]# restorecon -R -v /tmp/product-default
restorecon reset /tmp/product-default context unconfined_u:object_r:user_tmp_t:s0->unconfined_u:object_r:cert_t:s0
restorecon reset /tmp/product-default/69.pem context unconfined_u:object_r:user_tmp_t:s0->unconfined_u:object_r:cert_t:s0

Then the test is performed. (semanage fcontext -l  will list the contexts include the newly added one)
After completed, the context is removed...

[root@jsefler-rhel7 ~]# semanage fcontext -d "/tmp/product-default(/.*)?"
[root@jsefler-rhel7 ~]# restorecon -R -v /tmp/product-default
restorecon:  Warning no default label for /tmp/product-default
restorecon:  Warning no default label for /tmp/product-default/69.pem

And then the altered files should be deleted because the will still have the context from the workaround
[root@jsefler-rhel7 ~]# rm -rf /tmp/product-default
 */
@Test(groups={"SELinuxTests","AcceptanceTests","Tier1Tests","Tier2Tests","Tier3Tests","FipsTests"})
public class SELinuxTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="assert that no SELinux denials were logged during this TestSuite",
			groups={"blockedByBug-694879", "blockedByBug-822402"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void verifyNoSELinuxDenialsWereLogged_Test() {
		log.info("Assuming this test is being executed last in the TestNG Suite...");

		// AUTOMATORS NOTE: This test only differs with verifyNoSELinuxDenialsWereLoggedAfterClass() by the value of the marker sent to RemoteFileTasks.getTailFromMarkedFile(...)
		// ANY LOGIC/WORKAROUND CHANGES MADE HERE AND THERE.  TODO, eliminate this dual maintenance 
		for (SubscriptionManagerTasks clienttasks : Arrays.asList(client1tasks,client2tasks)) {
			if (clienttasks!=null) {
				String avcRegex;
				String tailFromMarkedFile = RemoteFileTasks.getTailFromMarkedFile(clienttasks.sshCommandRunner, clienttasks.auditLogFile, selinuxSuiteMarker, "denied").trim();
				
				// TEMPORARY WORKAROUND
				// [root@jsefler-rhel7 ~]# tail -f /var/log/audit/audit.log | grep AVC
				// type=USER_AVC msg=audit(1470087122.008:24063): pid=693 uid=81 auid=4294967295 ses=4294967295 subj=system_u:system_r:system_dbusd_t:s0-s0:c0.c1023 msg='avc:  denied  { 0x2 } for msgtype=signal interface=org.freedesktop.login1.Manager member=SessionNew dest=org.freedesktop.DBus spid=691 tpid=720 scontext=system_u:system_r:systemd_logind_t:s0 tcontext=system_u:system_r:modemmanager_t:s0 tclass=(null)  exe="/usr/bin/dbus-daemon" sauid=81 hostname=? addr=? terminal=?'
				// type=USER_AVC msg=audit(1470087122.226:24068): pid=693 uid=81 auid=4294967295 ses=4294967295 subj=system_u:system_r:system_dbusd_t:s0-s0:c0.c1023 msg='avc:  denied  { 0x2 } for msgtype=signal interface=org.freedesktop.login1.Manager member=SessionRemoved dest=org.freedesktop.DBus spid=691 tpid=720 scontext=system_u:system_r:systemd_logind_t:s0 tcontext=system_u:system_r:modemmanager_t:s0 tclass=(null)  exe="/usr/bin/dbus-daemon" sauid=81 hostname=? addr=? terminal=?'
				avcRegex = "type=USER_AVC .* msgtype=signal interface=org.freedesktop.login1.Manager member=Session(New|Removed) dest=org.freedesktop.DBus .* exe=\"/usr/bin/dbus-daemon\" .*";
				if (!tailFromMarkedFile.isEmpty() && doesStringContainMatches(tailFromMarkedFile, avcRegex)) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="1362273"; // Bug 1362273 - avc denied /var/log/audit/audit.log when "systemd: Started Session # of user root." is written to /var/log/messages every two minutes
					try {if (invokeWorkaroundWhileBugIsOpen&&OldBzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+OldBzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
					if (invokeWorkaroundWhileBugIsOpen) {
						log.warning("Ignoring the presence of AVC denials matching '"+avcRegex+"' while bug '"+bugId+"' is open.");
						tailFromMarkedFile = tailFromMarkedFile.replaceAll(avcRegex, "");
					}
				}
				// END OF WORKAROUND
				
				Assert.assertTrue(tailFromMarkedFile.trim().isEmpty(), "No SELinux denials found in the audit log '"+clienttasks.auditLogFile+"' on client "+clienttasks.sshCommandRunner.getConnection().getHostname()+" while executing this test class.");
			}
		}
	}
	
	
	
	// Configuration methods ***********************************************************************
	
	
	
	// Protected methods ***********************************************************************
	
	
	
	// Data Providers ***********************************************************************

}
