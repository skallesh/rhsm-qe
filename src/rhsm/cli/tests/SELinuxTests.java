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
 */
@Test(groups={"SELinuxTests","AcceptanceTests","Tier1Tests","Tier2Tests","Tier3Tests"})
public class SELinuxTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="assert that no SELinux denials were logged during this TestSuite",
			groups={"blockedByBug-694879", "blockedByBug-822402"},
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
