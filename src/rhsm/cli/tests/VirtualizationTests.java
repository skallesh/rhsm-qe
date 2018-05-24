package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.bugzilla.IBugzillaAPI.bzState;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.jul.TestRecords;

import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.SubscriptionPool;
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

// Notes...
//<jsefler> I'm trying to strategize an automated test for the virt entitlements stuff you demo'ed on Wednesday.  I got a few questions to start with...
//<jharris> sure
// shoot
//<jsefler> using the RESTapi, if I search through all the owners subscriptions and find one with a virt_limit attribute, then that means that two pools should get created corresponding to it.  correct?
// one pool for the host and one pool fir the guests
//<jharris> yes
// specifically the attribute is on either the product or the pool
//<jsefler> what does that mean?
// the virt_limit is an attribute of the product - that I know
// next I need to figure out what the relevant attributes are on the pool
//<jharris> pools have attributes
// products have attributes
// the two pools are created, as you said
// the physical (host) pool will have no additional attributes
// the virt (guest) pool will have an attribute of "virt_only" set to true
// the candlepin logic should only let virtual machines subscribe to that second pool
// this is done by checking the virt.is_guest fact
// that is set in subscription manager
//<jsefler> yup - that sounds good - that's what I need to get started
//<jharris> excellent
// but the virt_only attribute can also just be used on a product, for example
// so that maybe we want to start selling a product that is like RHEL for virtual machines
// IT can just stick that virt_only attribute on the product directly
// and it should do the same filtering


//10/31/2011 Notes:
//	<jsefler-lt> wottop: previously a subscription with a virt_limit attribute caused two pools to be generated... one for the host and one for the guest (indicated by virt_only=true attribute).  Now I see a third pool with "requires_host" attribute.
//	 the third pool seems new and good.
//	<wottop> hosted or standalone?
//	--- bleanhar_mtg is now known as bleanhar
//	<jsefler-lt> hosted
//	<wottop> there should not be a requires host in hosted
//	<wottop> also the indication is BOTH virt_only and pool_derived
//	<jsefler-lt> yes - that is what I see here....  curl --insecure --user stage_test_12:redhat --request GET http://rubyvip.web.stage.ext.phx2.redhat.com/clonepin/candlepin/owners/6445999/pools | python -mjson.tool
//	 wottop: so in my standalone on premise I'll get only the two pools (the old way)?
//	<wottop> no
//	 hosted: The creation of the bonus pool happens immediately, It is not tied to a specific host consumer. The count is related to the quantity * virt_limit of the physical pool.
//	 standalone: a bonus pool is created each time the physical pool is used for an entitlement. The quantity is based on the quantity of that one entitlement * virt_limit. It IS tied to the host consumers id, and only guests of that Id can use them.
//	 in the latter you might have many pools derived from the original physical pool
//	<jsefler-lt> GOT IT
//	<wottop> and revoking the host consumer entitlement will cause the bonus pool to go away
//	<wottop> jsefler-lt: helpful?
//	<jsefler-lt> wottop: yes
//	 wottop: revoking the host consumer entitlement will cause the bonus pool to go away AND ANY ENTITLEMENTS THE GUESTS MAY BE CONSUMING-NO QUESTIONS ASKED?
//	<wottop> The guest entitlements will get revoked. Yes.
//	<jsefler-lt> wottop: the curl call above is against the hosted STAGE environment and it is seeing the third pool (with "requires_host" attrib).  So is stage considered standalone?   I didn't think so.
//	<wottop> jsefler-lt: the default is standalone
//	<jsefler-lt> wottop: I recall you saying something that a candlpin.conf value needs to be set
//	<wottop> in master. I cannot comment on the state of STAGE
//	<wottop> jsefler-lt: There is an entry in candlepin.conf: candlepin.standalone = [true] is the default
//	<jsefler-lt> wottop: so stage just deployed 0.4.25 and I am seeing the third pool, which means that they are tripping the default "standalone" behavior.  Either the default behavior should not be standalone, or jomara needs to know that a new candlepin.conf value needs to be set.
//	<wottop> jsefler-lt: it is true in the code by default
//	 jsefler-lt: you want hosted?
//	<jsefler-lt> wottop: true in the code by default is fine with me, but then I "think" when jomara deploys candlepin in stage/production, then he needs to set the candlepin.standalone = false.    AM I CORRECT?  I'm just trying to get all on the same page.
//	<wottop> jsefler-lt: if you want hosted yes. Also, I would advise clearing the DB when switching between modes.

// INSTRUCTIONS FOR BUILDING A XEN KERNEL ON A BEAKER PROVISIONED BOX...
// https://docspace.corp.redhat.com/people/ndevos/blog/2011/05/26/how-to-quickly-install-a-rhel-5-system-running-xen-and-install-a-guest


@Test(groups={"VirtualizationTests"})
public class VirtualizationTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20091", "RHEL7-51101"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: facts list should report virt.is_guest and virt.host_type and virt.uuid",
			groups={"Tier1Tests","blockedByBug-1018807","blockedByBug-1242409","blockedByBug-1308732"}, dependsOnGroups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testVirtFactsReportedOnThisClient() {
		
		// make sure the original virt-what is in place 
		RemoteFileTasks.runCommandAndAssert(client, "cp -f "+virtWhatFileBackup+" "+virtWhatFile, 0);
		String virtWhatStdout = client.runCommandAndWait("virt-what").getStdout().trim();
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.11-1")) virtWhatStdout = virtWhatStdout.replaceAll("\\s*\\n\\s*", ", ");	// collapse multi-line values into one line... Bug 1018807 - newline in subscription-manager facts output on xen-hvm guest; subscription-manager commit ed7a68aaac1eeef64936cb5fef6ee6e5eb93b281
		
		log.info("Running virt-what version: "+client.runCommandAndWait("rpm -q virt-what").getStdout().trim());
		Map<String,String> factsMap = clienttasks.getFacts();
		
		// virt.is_guest
		String virtIsGuest = factsMap.get("virt.is_guest");	// = clienttasks.getFactValue("virt.is_guest");
		Assert.assertEquals(Boolean.valueOf(virtIsGuest), virtWhatStdout.equals("")?Boolean.FALSE:Boolean.TRUE, "subscription-manager facts list reports virt.is_guest as true when virt-what returns stdout.");
		
		// virt.host_type
		String virtHostType = factsMap.get("virt.host_type");	// = clienttasks.getFactValue("virt.host_type");
		Assert.assertEquals(virtHostType,virtWhatStdout.equals("")?"Not Applicable":virtWhatStdout,"subscription-manager facts list reports the same virt.host_type as what is returned by the virt-what installed on the client.");
		
		// virt.uuid
		// dev note: calculation for uuid is done in /usr/share/rhsm/subscription_manager/hwprobe.py def _getVirtUUID(self):
		String virtUuid = factsMap.get("virt.uuid");	// = clienttasks.getFactValue("virt.uuid");
		if (Boolean.parseBoolean(virtIsGuest)) {	// system is virtual...
			String expectedUuid = "Unknown";	// if (virtHostType.contains("ibm_systemz") || virtHostType.contains("xen-dom0") || virtHostType.contains("powervm")) expectedUuid = "Unknown";	// HARD CODED in src/subscription_manager/hwprobe.py:        no_uuid_platforms = ['powervm_lx86', 'xen-dom0', 'ibm_systemz']
			if (RemoteFileTasks.testExists(client, "/system/hypervisor/uuid")) expectedUuid = client.runCommandAndWait("cat /system/hypervisor/uuid").getStdout().trim();
			if (RemoteFileTasks.testExists(client, "/proc/device-tree/vm,uuid")) expectedUuid = client.runCommandAndWait("cat /proc/device-tree/vm,uuid").getStdout().trim();	// ppc64
			if (clienttasks.isPackageInstalled("dmidecode")) expectedUuid = client.runCommandAndWait("dmidecode -s system-uuid").getStdout().trim(); // Note: is sometimes a different case than the fact value.  e.g....
			//	[root@ibm-x3650m4-01-vm-14 ~]# hostname; dmidecode -s system-uuid; subscription-manager facts | grep virt.uuid
			//	ibm-x3650m4-01-vm-14.lab.eng.bos.redhat.com
			//	00443A8B-9C7C-4F74-B5F1-A970451078F6
			//	virt.uuid: 00443a8b-9c7c-4f74-b5f1-a970451078f6

			// TEMPORARY WORKAROUND FOR BUG
			if (virtHostType.contains("ibm_systemz") && expectedUuid.equals("Unknown")) {
				String bugId = "815598"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 815598 - [RFE] virt.uuid should not be "Unknown" in s390x when list facts
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Skipping the virt.uuid fact assertion on a '"+clienttasks.arch+"' virtual guest while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			// PERMANENT WORKAROUND FOR BUG CLOSED WONTFIX
			if (virtHostType.contains("ibm_systemz") && expectedUuid.equals("Unknown")) {
				String bugId = "815598"; boolean invokeWorkaroundWhileBugIsClosed = true;	// Bug 815598 - [RFE] virt.uuid should not be "Unknown" in s390x when list facts
				try {if (invokeWorkaroundWhileBugIsClosed&&(BzChecker.getInstance().getBugState(bugId)==bzState.CLOSED)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsClosed=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsClosed) {
					throw new SkipException("Skipping the virt.uuid fact assertion on a '"+clienttasks.arch+"' virtual guest since bug '"+bugId+"' has been CLOSED WONTFIX.");
				}
			}
			// END OF WORKAROUND
			// TEMPORARY WORKAROUND FOR BUG
			if (/*clienttasks.redhatReleaseX.equals("7") && */virtUuid==null && clienttasks.arch.startsWith("ppc")) {
				// APPLIES TO BOTH RHEL6 and RHEL7)
				String bugId = "1372108"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1372108 - facts related to the identification of a virtual/physical system on ppc64le are conflicting
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Skipping the virt.uuid fact assertion on a '"+clienttasks.arch+"' while virt-what bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			Assert.assertTrue(virtUuid.equalsIgnoreCase(expectedUuid), "subscription-manager facts list reports virt.uuid value '"+virtUuid+"' which (ignoring case) is equals to hardware value '"+expectedUuid+"'.  (Candlepin ignores case when comparing virt.uuid.  It also ignores the endianness)"); 
			
		} else {	// system is physical...
			
			// TEMPORARY WORKAROUND FOR BUG
			if (clienttasks.redhatReleaseX.equals("6") && virtUuid!=null && clienttasks.arch.startsWith("ppc")) {
				String bugId = "1312431"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1312431 - Add support for detecting ppc64 LPAR as virt guests
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Skipping the virt.uuid fact assertion on a '"+clienttasks.arch+"' virtual guest while virt-what bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			// TEMPORARY WORKAROUND FOR BUG
			if (clienttasks.redhatReleaseX.equals("7") && virtUuid!=null && clienttasks.arch.startsWith("ppc")) {
				String bugId = "1072524"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1072524 - Add support for detecting ppc64 LPAR as virt guests
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Skipping the virt.uuid fact assertion on a '"+clienttasks.arch+"' while virt-what bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			// TEMPORARY WORKAROUND FOR BUG
			if (/*clienttasks.redhatReleaseX.equals("7") && */virtUuid!=null && clienttasks.arch.startsWith("ppc")) {
				// APPLIES TO BOTH RHEL6 and RHEL7
				String bugId = "1372108"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1372108 - facts related to the identification of a virtual/physical system on ppc64le are conflicting
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Skipping the virt.uuid fact assertion on a '"+clienttasks.arch+"' while virt-what bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			// Note: the following assert caught regression Bug 1308732 - subscription-manager system fact virt.uuid: Unknown is reported on physical systems
			Assert.assertNull(virtUuid, "subscription-manager facts list should NOT report virt.uuid when on a host machine (indicated when virt-what reports an empty stdout).");		
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37714", "RHEL7-51491"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: facts list reports the host hypervisor type and uuid on which the guest client is running",
			dataProvider="getVirtWhatData",
			groups={"Tier2Tests","VirtFactsWhenClientIsAGuest_Test","blockedByBug-1242409"}, dependsOnGroups={},
			enabled=true)
	@ImplementsNitrateTest(caseId=70202)
	public void testVirtFactsWhenClientIsAGuest(Object bugzilla, String host_type) {
		
		log.info("We will fake out the ability of subscription-manager to read virt-what output on a '"+host_type+"' hypervisor by clobbering virt-what with a fake bash script...");
		forceVirtWhatToReturnGuest(host_type);
		
		// assert virt facts
		if (host_type.contains("xen-dom0")) {
			log.warning("A xen-dom0 guest is actually a special case and should be treated by subscription-manager as a host.");
			assertsForVirtFactsWhenClientIsAHost();
		} else {
			assertsForVirtFactsWhenClientIsAGuest(host_type);			
		}
	}
	protected void assertsForVirtFactsWhenClientIsAGuest(String host_type) {
		log.info("Now let's run the subscription-manager facts --list and assert the results...");
		Map<String,String> factsMap = clienttasks.getFacts();
		
		// virt.is_guest
		String virtIsGuest = factsMap.get("virt.is_guest");	// = clienttasks.getFactValue("virt.is_guest");
		Assert.assertEquals(Boolean.valueOf(virtIsGuest),Boolean.TRUE,"subscription-manager facts list reports virt.is_guest as true when the client is running on a '"+host_type+"' hypervisor.");
		
		// virt.host_type
		String virtHostType = factsMap.get("virt.host_type");	// = clienttasks.getFactValue("virt.host_type");
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.11-1")) host_type = host_type.replaceAll("\\s*\\n\\s*", ", ");	// collapse multi-line values into one line... Bug 1018807 - newline in subscription-manager facts output on xen-hvm guest; subscription-manager commit ed7a68aaac1eeef64936cb5fef6ee6e5eb93b281
		Assert.assertEquals(virtHostType,host_type,"subscription-manager facts list reports the same virt.host_type value of as returned by "+virtWhatFile);
		
		// virt.uuid
		String virtUuid = factsMap.get("virt.uuid");	// = clienttasks.getFactValue("virt.uuid");
		if (host_type.contains("ibm_systemz") || host_type.contains("xen-dom0") || host_type.contains("powervm")) {
			
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.16.8-3")) {	// master commit e330f0795879e7aaafac84237cf404aaac11ff7c	// RHEL6.8 commit 2c8c5a0372e9516b0799a31ca6d1b35299d70894	// 1308732: Leave hw fact virt.uuid unset if unknown
				Assert.assertNull(virtUuid,"subscription-manager facts does NOT report a virt.uuid when it is Unknown (expected when hypervisor contains \"ibm_systemz\", \"xen-dom0\", or \"powervm\")");
			} else
			Assert.assertEquals(virtUuid,"Unknown","subscription-manager facts list reports virt.uuid as Unknown when the hypervisor contains \"ibm_systemz\", \"xen-dom0\", or \"powervm\".");
		} else {
			//String expectedUuid = client.runCommandAndWait("if [ -r /system/hypervisor/uuid ]; then cat /system/hypervisor/uuid; else dmidecode -s system-uuid; fi").getStdout().trim().toLowerCase();	// TODO Not sure if the cat /system/hypervisor/uuid is exactly correct
			String expectedUuid = "Unknown";
			if (RemoteFileTasks.testExists(client, "/system/hypervisor/uuid")) expectedUuid = client.runCommandAndWait("cat /system/hypervisor/uuid").getStdout().trim();
			if (RemoteFileTasks.testExists(client, "/proc/device-tree/vm,uuid")) expectedUuid = client.runCommandAndWait("cat /proc/device-tree/vm,uuid").getStdout().trim();	// ppc64
			if (clienttasks.isPackageInstalled("dmidecode")) expectedUuid = client.runCommandAndWait("dmidecode -s system-uuid").getStdout().trim()/*.toLowerCase() TODO CAN'T REMEMBER WHY THIS WAS NEEDED, TAKE IT OUT */;
			Assert.assertEquals(virtUuid,expectedUuid,"subscription-manager facts list reports virt.uuid value to be the /system/hypervisor/uuid or /proc/device-tree/vm,uuid or dmidecode -s system-uuid");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37715", "RHEL7-51492"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: facts list reports when the client is running on bare metal",
			groups={"Tier2Tests","blockedByBug-726440","blockedByBug-1308732","VirtFactsWhenClientIsAHost_Test"}, dependsOnGroups={},
			enabled=true)
	@ImplementsNitrateTest(caseId=70203)
	public void testVirtFactsWhenClientIsAHost() {
		
		log.info("We will fake out the ability of subscription-manager to read virt-what output on bare metal by clobbering virt-what with a fake bash script...");
		forceVirtWhatToReturnHost();
		
		// assert virt facts
		assertsForVirtFactsWhenClientIsAHost();
	}
	protected void assertsForVirtFactsWhenClientIsAHost() {
		log.info("Now let's run the subscription-manager facts --list and assert the results...");
		Map<String,String> factsMap = clienttasks.getFacts();
		
		// virt.is_guest
		String virtIsGuest = factsMap.get("virt.is_guest");	// = clienttasks.getFactValue("virt.is_guest");
		Assert.assertEquals(Boolean.valueOf(virtIsGuest),Boolean.FALSE,"subscription-manager facts list reports virt.is_guest as false when the client is running on bare metal.");
		
		// virt.host_type
		String virtHostType = factsMap.get("virt.host_type");	// = clienttasks.getFactValue("virt.host_type");
		String virtWhatStdout = client.runCommandAndWait("virt-what").getStdout().trim();
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.10.11-1")) virtWhatStdout = virtWhatStdout.replaceAll("\\s*\\n\\s*", ", ");	// collapse multi-line values into one line... Bug 1018807 - newline in subscription-manager facts output on xen-hvm guest; subscription-manager commit ed7a68aaac1eeef64936cb5fef6ee6e5eb93b281
		
		//if (virtWhatStdout.equals("xen\nxen-dom0")) {
		if (virtWhatStdout.contains("xen-dom0")) {
			log.warning("Normally on a bare metal system (indicated by virt.is_guest=false), virt.host_type will be reported as Not Applicable.  An exception to this case occurs when virt-what reports xen-dom0, then virt.host_type will report xen-dem0 and subscription-manager will treat the system as a bare metal system.");
			Assert.assertEquals(virtHostType,virtWhatStdout);			
		} else {
			//Assert.assertEquals(virtHostType,"","subscription-manager facts list reports no value for virt.host_type when run on bare metal.");	// valid assertion prior to bug 726440/722248
			Assert.assertEquals(virtHostType,"Not Applicable","subscription-manager facts list report for virt.host_type when run on bare metal.");
		}
		
		// virt.uuid
		String virtUuid = factsMap.get("virt.uuid");	// = clienttasks.getFactValue("virt.uuid");
		Assert.assertNull(virtUuid,"subscription-manager facts list should NOT report virt.uuid when run on bare metal.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37716", "RHEL7-51493"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: facts list should not crash on virt facts when virt-what fails",
			groups={"Tier2Tests","blockedByBug-668936","blockedByBug-768397","VirtFactsWhenVirtWhatFails_Test"}, dependsOnGroups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testVirtFactsWhenVirtWhatFails() {
		
		log.info("We will fail virt-what by forcing it to return a non-zero value...");
		forceVirtWhatToFail();
		
		log.info("Now let's run the subscription-manager facts --list and assert the results...");
		Map<String,String> factsMap = clienttasks.getFacts();
		
		// virt.is_guest
		String virtIsGuest = factsMap.get("virt.is_guest");	// = clienttasks.getFactValue("virt.is_guest");
		Assert.assertEquals(virtIsGuest,"Unknown","subscription-manager facts list reports virt.is_guest as Unknown when the hypervisor is undeterminable (virt-what fails).");

		// virt.host_type
		String virtHostType = factsMap.get("virt.host_type");	// = clienttasks.getFactValue("virt.host_type");
		Assert.assertNull(virtHostType,"subscription-manager facts list should NOT report a virt.host_type when the hypervisor is undeterminable (virt-what fails).");
		
		// virt.uuid
		String virtUuid = factsMap.get("virt.uuid");	// = clienttasks.getFactValue("virt.uuid");
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.16.8-3")) {	// master commit e330f0795879e7aaafac84237cf404aaac11ff7c	// RHEL6.8 commit 2c8c5a0372e9516b0799a31ca6d1b35299d70894	// 1308732: Leave hw fact virt.uuid unset if unknown
			Assert.assertNull(virtUuid,"subscription-manager facts does NOT report a virt.uuid when it is Unknown (expected when virt-what fails after fix for bug 1308732)");
		} else
		Assert.assertEquals(virtUuid,"Unknown","subscription-manager facts list reports virt.uuid as Unknown when the hypervisor is undeterminable (virt-what fails).");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37717", "RHEL7-51494"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: facts list should report is_guest and uuid as Unknown when virt-what is not installed",
			groups={"Tier2Tests","blockedByBug-768397"}, dependsOnGroups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testVirtFactsWhenVirtWhatIsNotInstalled() {
		
		log.info("We will remove virt-what for this test...");
		
		RemoteFileTasks.runCommandAndWait(client,"rm -f "+virtWhatFile, TestRecords.action());
		log.info("Now let's run the subscription-manager facts --list and assert the results...");
		Map<String,String> factsMap = clienttasks.getFacts();
		
		// virt.is_guest
		String virtIsGuest = factsMap.get("virt.is_guest");	// = clienttasks.getFactValue("virt.is_guest");
		Assert.assertEquals(virtIsGuest,"Unknown","subscription-manager facts list reports virt.is_guest as Unknown when virt-what in not installed.");
		
		// virt.host_type
		String virtHostType = factsMap.get("virt.host_type");	// = clienttasks.getFactValue("virt.host_type");
		Assert.assertNull(virtHostType,"subscription-manager facts list should NOT report a virt.host_type when virt-what in not installed.");
		
		// virt.uuid
		String virtUuid = factsMap.get("virt.uuid");	// = clienttasks.getFactValue("virt.uuid");
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.16.8-3")) {	// master commit e330f0795879e7aaafac84237cf404aaac11ff7c	// RHEL6.8 commit 2c8c5a0372e9516b0799a31ca6d1b35299d70894	// 1308732: Leave hw fact virt.uuid unset if unknown
			Assert.assertNull(virtUuid,"subscription-manager facts does NOT report a virt.uuid when it is Unknown (expected when virt-what is not installed after fix for bug 1308732)");
		} else
		Assert.assertEquals(virtUuid,"Unknown","subscription-manager facts list reports virt.uuid as Unknown when virt-what in not installed.");

	}






	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20090", "RHEL7-59440"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Verify host and guest pools are generated from a virtualization-aware subscription.",
			groups={"Tier1Tests","blockedByBug-750279"},
			dependsOnGroups={},
			dataProvider="getVirtSubscriptionData",
			enabled=true)
	public void testHostAndGuestPoolsAreGeneratedForVirtualizationAwareSubscription(String subscriptionId, String productName, String productId, int quantity, String virtLimit, String hostPoolId, String guestPoolId, Boolean physicalOnly) throws JSONException, Exception {

//		log.info("When an owner has purchased a virtualization-aware subscription ("+productName+"; subscriptionId="+subscriptionId+"), he should have subscription access to two pools: one for the host and one for the guest.");
//
//		// assert that there are two (one for the host and one for the guest)
//		log.info("Using the RESTful Candlepin API, let's find all the pools generated from subscription id: "+subscriptionId);
//		List<String> poolIds = CandlepinTasks.getPoolIdsForSubscriptionId(sm_clientUsername,sm_clientPassword,sm_serverUrl,ownerKey,subscriptionId);
//		Assert.assertEquals(poolIds.size(), 2, "Exactly two pools should be derived from virtualization-aware subscription id '"+subscriptionId+"' ("+productName+").");
//
//		// assert that one pool is for the host and the other is for the guest
//		guestPoolId = null;
//		hostPoolId = null;
//		for (String poolId : poolIds) {
//			if (CandlepinTasks.isPoolVirtOnly (sm_clientUsername,sm_clientPassword,poolId,sm_serverUrl)) {
//				guestPoolId = poolId;
//			} else {
//				hostPoolId = poolId;
//			}
//		}
//		Assert.assertNotNull(guestPoolId, "Found the guest pool id ("+guestPoolId+") with an attribute of virt_only=true");
//		Assert.assertNotNull(hostPoolId, "Found the host pool id ("+hostPoolId+") without an attribute of virt_only=true");	
		
// WHEN candlepin.conf candlepin.standalone = true (IF NOT SPECIFIED, DEFAULTS TO true)
// THE FOLLOWING THREE POOLS SHOULD NEVER OCCUR SINCE ONLY candlepin.standalone SHOULD NOT BE SWITCHED BETWEEN TRUE/FALSE
//		[root@intel-s3ea2-04 ~]# curl --insecure --user stage_test_12:redhat --request GET http://rubyvip.web.stage.ext.phx2.redhat.com/clonepin/candlepin/owners/6445999/pools | python -mjson.tool
//			  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
//			                                 Dload  Upload   Total   Spent    Left  Speed
//			100  8044    0  8044    0     0   8856      0 --:--:-- --:--:-- --:--:--  9980
//			[
//			    {
//			        "accountNumber": "1508113", 
//			        "activeSubscription": true, 
//			        "attributes": [
//			            {
//			                "created": "2011-10-30T05:06:50.000+0000", 
//			                "id": "8a99f9813350d60e0133533919f512f9", 
//			                "name": "requires_consumer_type", 
//			                "updated": "2011-10-30T05:06:50.000+0000", 
//			                "value": "system"
//			            }, 
//			            {
//			                "created": "2011-10-30T05:06:50.000+0000", 
//			                "id": "8a99f9813350d60e0133533919f512fa", 
//			                "name": "requires_host", 
//			                "updated": "2011-10-30T05:06:50.000+0000", 
//			                "value": "c6ec101c-2c6a-4f5d-9161-ac335d309d0e"
//			            }, 
//			            {
//			                "created": "2011-10-30T05:06:50.000+0000", 
//			                "id": "8a99f9813350d60e0133533919f512fc", 
//			                "name": "pool_derived", 
//			                "updated": "2011-10-30T05:06:50.000+0000", 
//			                "value": "true"
//			            }, 
//			            {
//			                "created": "2011-10-30T05:06:50.000+0000", 
//			                "id": "8a99f9813350d60e0133533919f512fb", 
//			                "name": "virt_only", 
//			                "updated": "2011-10-30T05:06:50.000+0000", 
//			                "value": "true"
//			            }
//			        ], 
//			        "consumed": 0, 
//			        "contractNumber": "2635037", 
//			        "created": "2011-10-30T05:06:50.000+0000", 
//			        "endDate": "2012-10-19T03:59:59.000+0000", 
//			        "href": "/pools/8a99f9813350d60e0133533919f512f8", 
//			        "id": "8a99f9813350d60e0133533919f512f8", 
//			        "owner": {
//			            "displayName": "6445999", 
//			            "href": "/owners/6445999", 
//			            "id": "8a85f98432e7376c013302c3a9745c68", 
//			            "key": "6445999"
//			        }, 
//			        "productAttributes": [], 
//			        "productId": "RH0103708", 
//			        "productName": "Red Hat Enterprise Linux Server, Premium (8 sockets) (Up to 4 guests)", 
//			        "providedProducts": [
//			            {
//			                "created": "2011-10-30T05:06:50.000+0000", 
//			                "id": "8a99f9813350d60e0133533919f512fd", 
//			                "productId": "69", 
//			                "productName": "Red Hat Enterprise Linux Server", 
//			                "updated": "2011-10-30T05:06:50.000+0000"
//			            }
//			        ], 
//			        "quantity": 4, 
//			        "restrictedToUsername": null, 
//			        "sourceEntitlement": {
//			            "href": "/entitlements/8a99f9813350d60e0133533919f512fe", 
//			            "id": "8a99f9813350d60e0133533919f512fe"
//			        }, 
//			        "startDate": "2011-10-19T04:00:00.000+0000", 
//			        "subscriptionId": "2272904", 
//			        "updated": "2011-10-30T05:06:50.000+0000"
//			    }, 
//			    {
//			        "accountNumber": "1508113", 
//			        "activeSubscription": true, 
//			        "attributes": [], 
//			        "consumed": 3, 
//			        "contractNumber": "2635037", 
//			        "created": "2011-10-19T19:05:09.000+0000", 
//			        "endDate": "2012-10-19T03:59:59.000+0000", 
//			        "href": "/pools/8a99f98233137a9701331d92a4301203", 
//			        "id": "8a99f98233137a9701331d92a4301203", 
//			        "owner": {
//			            "displayName": "6445999", 
//			            "href": "/owners/6445999", 
//			            "id": "8a85f98432e7376c013302c3a9745c68", 
//			            "key": "6445999"
//			        }, 
//			        "productAttributes": [
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4301204", 
//			                "name": "support_type", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "L1-L3"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4301205", 
//			                "name": "sockets", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "8"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4301206", 
//			                "name": "virt_limit", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "4"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4301207", 
//			                "name": "name", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "Red Hat Enterprise Linux Server, Premium (8 sockets) (Up to 4 guests)"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4301208", 
//			                "name": "type", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "MKT"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4301209", 
//			                "name": "description", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "Red Hat Enterprise Linux"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a430120b", 
//			                "name": "product_family", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "Red Hat Enterprise Linux"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a430120a", 
//			                "name": "option_code", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "1"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a430120c", 
//			                "name": "variant", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "Physical Servers"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a430120d", 
//			                "name": "support_level", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "PREMIUM"
//			            }
//			        ], 
//			        "productId": "RH0103708", 
//			        "productName": "Red Hat Enterprise Linux Server, Premium (8 sockets) (Up to 4 guests)", 
//			        "providedProducts": [
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a430120e", 
//			                "productId": "69", 
//			                "productName": "Red Hat Enterprise Linux Server", 
//			                "updated": "2011-10-19T19:05:09.000+0000"
//			            }
//			        ], 
//			        "quantity": 100, 
//			        "restrictedToUsername": null, 
//			        "sourceEntitlement": null, 
//			        "startDate": "2011-10-19T04:00:00.000+0000", 
//			        "subscriptionId": "2272904", 
//			        "updated": "2011-10-19T19:05:09.000+0000"
//			    }, 
//			    {
//			        "accountNumber": "1508113", 
//			        "activeSubscription": true, 
//			        "attributes": [
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4461210", 
//			                "name": "requires_consumer_type", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "system"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4461211", 
//			                "name": "virt_limit", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "0"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4471213", 
//			                "name": "pool_derived", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "true"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4471212", 
//			                "name": "virt_only", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "true"
//			            }
//			        ], 
//			        "consumed": 3, 
//			        "contractNumber": "2635037", 
//			        "created": "2011-10-19T19:05:09.000+0000", 
//			        "endDate": "2012-10-19T03:59:59.000+0000", 
//			        "href": "/pools/8a99f98233137a9701331d92a446120f", 
//			        "id": "8a99f98233137a9701331d92a446120f", 
//			        "owner": {
//			            "displayName": "6445999", 
//			            "href": "/owners/6445999", 
//			            "id": "8a85f98432e7376c013302c3a9745c68", 
//			            "key": "6445999"
//			        }, 
//			        "productAttributes": [
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4471214", 
//			                "name": "support_type", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "L1-L3"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4471215", 
//			                "name": "sockets", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "8"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4471216", 
//			                "name": "virt_limit", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "4"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4471217", 
//			                "name": "name", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "Red Hat Enterprise Linux Server, Premium (8 sockets) (Up to 4 guests)"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4471218", 
//			                "name": "type", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "MKT"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a4471219", 
//			                "name": "description", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "Red Hat Enterprise Linux"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a447121b", 
//			                "name": "product_family", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "Red Hat Enterprise Linux"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a447121a", 
//			                "name": "option_code", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "1"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a447121c", 
//			                "name": "variant", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "Physical Servers"
//			            }, 
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a447121d", 
//			                "name": "support_level", 
//			                "productId": "RH0103708", 
//			                "updated": "2011-10-19T19:05:09.000+0000", 
//			                "value": "PREMIUM"
//			            }
//			        ], 
//			        "productId": "RH0103708", 
//			        "productName": "Red Hat Enterprise Linux Server, Premium (8 sockets) (Up to 4 guests)", 
//			        "providedProducts": [
//			            {
//			                "created": "2011-10-19T19:05:09.000+0000", 
//			                "id": "8a99f98233137a9701331d92a447121e", 
//			                "productId": "69", 
//			                "productName": "Red Hat Enterprise Linux Server", 
//			                "updated": "2011-10-19T19:05:09.000+0000"
//			            }
//			        ], 
//			        "quantity": 400, 
//			        "restrictedToUsername": null, 
//			        "sourceEntitlement": null, 
//			        "startDate": "2011-10-19T04:00:00.000+0000", 
//			        "subscriptionId": "2272904", 
//			        "updated": "2011-10-19T19:05:09.000+0000"
//			    }
//			]

		log.info("When a hosted owner has purchased a virtualization-aware subscription ("+productName+"; subscriptionId="+subscriptionId+"), he should have subscription access to two pools: one for the host and one for the guest.");

		// assert that there are two (one for the host and one for the guest)
		log.info("Using the RESTful Candlepin API, let's find all the pools generated from subscription id: "+subscriptionId);
		List<JSONObject> jsonPools = CandlepinTasks.getPoolsForSubscriptionId(sm_clientUsername,sm_clientPassword,sm_serverUrl,ownerKey,subscriptionId);

		if (!servertasks.statusStandalone) {
			Assert.assertEquals(jsonPools.size(), 2, "When the candlepin.standalone is false, exactly two pools should be generated from virtualization-aware subscription id '"+subscriptionId+"' ("+productName+").  (one with no attributes, one with virt_only and pool_derived true)");	
		} else {
			// Note: this line of code should not be reached since this test should not be run when servertasks.statusStandalone is true
			Assert.assertTrue(jsonPools.size()>=1, "When the candlepin.standalone is true, one or more pools (actual='"+jsonPools.size()+"') should be generated from virtualization-aware subscription id '"+subscriptionId+"' ("+productName+").  (one with no attributes, the rest with virt_only pool_derived equals true and requires_host)");			
		}

		// assert that one pool is for the host and the other is for the guest
		guestPoolId = null;
		hostPoolId = null;
		for (JSONObject jsonPool : jsonPools) {
			String poolId = jsonPool.getString("id");
//			JSONArray attributes = jsonPool.getJSONArray("attributes");
//			if (attributes.length()==0) {
//				hostPoolId = poolId;
//				continue;
//			}
			
			if (Boolean.TRUE.toString().equalsIgnoreCase(CandlepinTasks.getPoolAttributeValue(jsonPool, "virt_only")) &&
				Boolean.TRUE.toString().equalsIgnoreCase(CandlepinTasks.getPoolAttributeValue(jsonPool, "pool_derived")) &&
				CandlepinTasks.getPoolAttributeValue(jsonPool, "requires_host")==null) {
				guestPoolId = poolId;
			} else if (
				Boolean.TRUE.toString().equalsIgnoreCase(CandlepinTasks.getPoolAttributeValue(jsonPool, "virt_only")) &&
				Boolean.TRUE.toString().equalsIgnoreCase(CandlepinTasks.getPoolAttributeValue(jsonPool, "pool_derived")) &&
				CandlepinTasks.getPoolAttributeValue(jsonPool, "requires_host")!=null) {
				//newGuestPoolId = poolId;  // TODO THIS IS THE NEW VIRT-AWARE MODEL FOR WHICH NEW TESTS SHOULD BE AUTOMATED
			} else if (
				CandlepinTasks.getPoolAttributeValue(jsonPool, "virt_only")==null &&	// TODO or false?
				CandlepinTasks.getPoolAttributeValue(jsonPool, "pool_derived")==null &&	// TODO or false?
				CandlepinTasks.getPoolAttributeValue(jsonPool, "requires_host")==null) {
				hostPoolId = poolId;
			}
		}
		Assert.assertNotNull(guestPoolId, "Found the virt_only/pool_derived guest pool id ("+guestPoolId+") without an attribute of requires_host");
		Assert.assertNotNull(hostPoolId, "Found the host pool id ("+hostPoolId+")");	
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-22223", "RHEL7-59439"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1 Tier2")
	@Test(	description="Verify host and guest pools quantities generated from a virtualization-aware subscription",
			groups={"Tier1Tests","Tier2Tests","VerifyHostAndGuestPoolQuantities_Test"}, // "blockedByBug-679617" indirectly when this script is run as part of the full TestNG suite since this is influenced by other scripts calling refresh pools
			dependsOnGroups={},
			dataProvider="getVirtSubscriptionData",
			enabled=true)
	public void testHostAndGuestPoolQuantities(String subscriptionId, String productName, String productId, int quantity, String virtLimit, String hostPoolId, String guestPoolId, Boolean physicalOnly) throws JSONException, Exception {
		if (hostPoolId==null && guestPoolId==null) throw new SkipException("Failed to find expected host and guest pools derived from virtualization-aware subscription id '"+subscriptionId+"' ("+productName+").");
//if (!productId.equals("awesomeos-virt-unlimited")) throw new SkipException("debugTesting VerifyHostAndGuestPoolQuantities_Test");
		JSONObject jsonHostPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+hostPoolId));	
		int jsonHostPoolQuantity = jsonHostPool.getInt("quantity");
		int jsonHostPoolQuantityConsumed = jsonHostPool.getInt("consumed");
		int jsonHostPoolQuantityExported = jsonHostPool.getInt("exported");
		
		JSONObject jsonGuestPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/pools/"+guestPoolId));	
		int jsonGuestPoolQuantity = jsonGuestPool.getInt("quantity");
		int jsonGuestPoolQuantityConsumed = jsonGuestPool.getInt("consumed");
		int jsonGuestPoolQuantityExported = jsonGuestPool.getInt("exported");

		// trick this system into believing it is a virt guest
		forceVirtWhatToReturnGuest("kvm");
		
		// get the available pools
		List<SubscriptionPool> allAvailablePools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();

		// get the hostPool
		SubscriptionPool hostPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", hostPoolId, allAvailablePools);
		// determine the availability of the hostPool
		if (jsonHostPoolQuantityConsumed > jsonHostPoolQuantity) {
			Assert.fail("Host pool id '"+hostPoolId+"' has a consumed attribute value '"+jsonHostPoolQuantityConsumed+"' that exceeds its total quantity '"+jsonHostPoolQuantity+"'.  This does NOT make sense.");
		} else if (jsonHostPoolQuantityConsumed == jsonHostPoolQuantity) {
			Assert.assertNull(hostPool,"Host pool id '"+hostPoolId+"', derived from the virtualization-aware subscription id '"+subscriptionId+"', is NOT listed in all available subscriptions since all of its quantity are already being consumed by other systems.");
		} else {
			Assert.assertNotNull(hostPool,"Host pool id '"+hostPoolId+"', derived from the virtualization-aware subscription id '"+subscriptionId+"', is listed in all available subscriptions: "+hostPool);

			// assert hostPoolId quantity
			//Assert.assertEquals(Integer.valueOf(hostPool.quantity), Integer.valueOf(quantity-jsonHostPoolQuantityConsumed), "Assuming '"+jsonHostPoolQuantityConsumed+"' entitlements are currently being consumed from this host pool '"+hostPool.poolId+"', the quantity of available entitlements should be '"+quantity+"' minus '"+jsonHostPoolQuantityConsumed+"' (COULD BE DIFFERENT IF ANOTHER PHYSICAL HOST SYSTEM IS SIMULTANEOUSLY SUBSCRIBING TO THE SAME POOL OR JUST RETURNED AN ENTITLEMENT TO THE SAME POOL).");	
			if (!Integer.valueOf(hostPool.quantity).equals(Integer.valueOf(quantity-jsonHostPoolQuantityConsumed))) {
				log.warning("Assuming '"+jsonHostPoolQuantityConsumed+"' entitlements are currently being consumed from this host pool '"+hostPool.poolId+"', the quantity of available entitlements should be '"+quantity+"' minus '"+jsonHostPoolQuantityConsumed+"' (COULD BE DIFFERENT IF ANOTHER PHYSICAL HOST SYSTEM IS SIMULTANEOUSLY SUBSCRIBING TO THE SAME POOL OR JUST RETURNED AN ENTITLEMENT TO THE SAME POOL).");
			}
			if (!CandlepinType.hosted.equals(sm_serverType)) {	// too many false negatives occur on hosted due to simultaneous client consumption from the same pool
				Assert.assertEquals(Integer.valueOf(hostPool.quantity), Integer.valueOf(quantity-jsonHostPoolQuantityConsumed), "Assuming '"+jsonHostPoolQuantityConsumed+"' entitlements are currently being consumed from this host pool '"+hostPool.poolId+"', the quantity of available entitlements should be '"+quantity+"' minus '"+jsonHostPoolQuantityConsumed+"' (COULD BE DIFFERENT IF ANOTHER PHYSICAL HOST SYSTEM IS SIMULTANEOUSLY SUBSCRIBING TO THE SAME POOL OR JUST RETURNED AN ENTITLEMENT TO THE SAME POOL).");	
			}
		}
		
		// get the guestPool
		SubscriptionPool guestPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", guestPoolId, allAvailablePools);
		// determine the availability of the guestPool
		if (jsonGuestPoolQuantity==-1) {
			Assert.assertNotNull(guestPool,"Guest pool id '"+guestPoolId+"', derived from the virtualization-aware subscription id '"+subscriptionId+"', is listed in all available subscriptions: "+guestPool);

			// assert guestPoolId quantity is unlimited
			Assert.assertEquals(guestPool.quantity, "Unlimited", "When the subscription product has a quantity attribute of '-1', then the guest pool's quantity viewed by within the list --all --available should be 'Unlimited'.");	// altered after Bug 862885 - String Update: Capitalize unlimited in the All Available Subscriptions tab 
			Assert.assertEquals(virtLimit, "unlimited", "When the subscription product has a quantity attribute of '-1', then the guest pool's virt_limit attribute should be 'unlimited'.");

		} else if (jsonGuestPoolQuantityConsumed > jsonGuestPoolQuantity) {
			Assert.fail("Guest pool id '"+guestPoolId+"' has a consumed attribute value '"+jsonGuestPoolQuantityConsumed+"' that exceeds its total quantity '"+jsonGuestPoolQuantity+"'.  This does NOT make sense.");
		} else if (jsonGuestPoolQuantityConsumed == jsonGuestPoolQuantity) {
			Assert.assertNull(guestPool,"Guest pool id '"+guestPoolId+"', derived from the virtualization-aware subscription id '"+subscriptionId+"', is NOT listed in all available subscriptions since all of its quantity are already being consumed by other systems.");
		} else {
			Assert.assertNotNull(guestPool,"Guest pool id '"+guestPoolId+"', derived from the virtualization-aware subscription id '"+subscriptionId+"', is listed in all available subscriptions: "+guestPool);

			// assert guestPoolId quantity
			//Assert.assertEquals(Integer.valueOf(guestPool.quantity), Integer.valueOf((quantity-jsonHostPoolQuantityExported)*Integer.valueOf(virtLimit)-jsonGuestPoolQuantityConsumed), "Assuming '"+jsonGuestPoolQuantityConsumed+"' entitlements are currently being consumed from guest pool '"+guestPool.poolId+"', the quantity of available entitlements for this guest pool should be the host pool's virt_limit of '"+virtLimit+"' times (the host's total quantity '"+quantity+"' minus those exported '"+jsonHostPoolQuantityExported+"') minus the number of already consumed from the pool '"+jsonGuestPoolQuantityConsumed+"'  (COULD BE DIFFERENT IF ANOTHER VIRTUAL GUEST SYSTEM IS SIMULTANEOUSLY SUBSCRIBING TO THE SAME POOL).");
			if (!Integer.valueOf(guestPool.quantity).equals(Integer.valueOf((quantity-jsonHostPoolQuantityExported)*Integer.valueOf(virtLimit)-jsonGuestPoolQuantityConsumed))) {
				log.warning("Assuming '"+jsonGuestPoolQuantityConsumed+"' entitlements are currently being consumed from guest pool '"+guestPool.poolId+"', the quantity of available entitlements for this guest pool should be the host pool's virt_limit of '"+virtLimit+"' times (the host's total quantity '"+quantity+"' minus those exported '"+jsonHostPoolQuantityExported+"') minus the number of already consumed from the pool '"+jsonGuestPoolQuantityConsumed+"'  (COULD BE DIFFERENT IF ANOTHER VIRTUAL GUEST SYSTEM IS SIMULTANEOUSLY SUBSCRIBING TO THE SAME POOL).");
			}
			if (!CandlepinType.hosted.equals(sm_serverType)) {	// too many false negatives occur on hosted due to simultaneous client consumption from the same pool
				Assert.assertEquals(Integer.valueOf(guestPool.quantity), Integer.valueOf((quantity-jsonHostPoolQuantityExported)*Integer.valueOf(virtLimit)-jsonGuestPoolQuantityConsumed), "Assuming '"+jsonGuestPoolQuantityConsumed+"' entitlements are currently being consumed from guest pool '"+guestPool.poolId+"', the quantity of available entitlements for this guest pool should be the host pool's virt_limit of '"+virtLimit+"' times (the host's total quantity '"+quantity+"' minus those exported '"+jsonHostPoolQuantityExported+"') minus the number of already consumed from the pool '"+jsonGuestPoolQuantityConsumed+"'  (COULD BE DIFFERENT IF ANOTHER VIRTUAL GUEST SYSTEM IS SIMULTANEOUSLY SUBSCRIBING TO THE SAME POOL).");
			}
		}
	}
		
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47928", "RHEL7-97327"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28576",	// RHSM-REQ : Host-limited Guest Subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.RELATES_TO),
				@LinkedItem(
					workitemId= "RHEL7-84958",	// RHSM-REQ : Host-limited Guest Subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.RELATES_TO)},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Verify the virt_limit multiplier on guest pool quantity is not clobbered by refresh pools",
			groups={"Tier2Tests","blockedByBug-679617"},
			dependsOnGroups={},
			dependsOnMethods={"testHostAndGuestPoolQuantities"},
			dataProvider="getVirtSubscriptionData",
			enabled=true)
	public void testGuestPoolQuantityIsNotClobberedByRefreshPools(String subscriptionId, String productName, String productId, int quantity, String virtLimit, String hostPoolId, String guestPoolId, Boolean physicalOnly) throws JSONException, Exception {
		if (hostPoolId==null && guestPoolId==null) throw new SkipException("Failed to find expected host and guest pools derived from virtualization-aware subscription id '"+subscriptionId+"' ("+productName+").");
		if (servertasks.dbConnection==null) throw new SkipException("This testcase requires a connection to the candlepin database so that it can updateSubscriptionDatesOnDatabase.");

		// get the hostPool
		List<SubscriptionPool> allAvailablePools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		SubscriptionPool hostPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", hostPoolId, allAvailablePools);
		Assert.assertNotNull(hostPool,"A host pool derived from the virtualization-aware subscription id '"+subscriptionId+"' is listed in all available subscriptions: "+hostPool);

		// remember the hostPool quantity before calling refresh pools
		String hostPoolQuantityBefore = hostPool.quantity;
		
		// get the guestPool
		SubscriptionPool guestPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", guestPoolId, allAvailablePools);
		Assert.assertNotNull(guestPool,"A guest pool derived from the virtualization-aware subscription id '"+subscriptionId+"' is listed in all available subscriptions: "+guestPool);

		// remember the hostPool quantity before calling refresh pools
		String guestPoolQuantityBefore = guestPool.quantity;

		log.info("Now let's modify the start date of the virtualization-aware subscription id '"+subscriptionId+"'...");
		/* 7/10/2015 devel consciously decided to drop @Verify(value = Owner.class, subResource = SubResource.SUBSCRIPTIONS) on this GET method starting with candlepin-2.0.
		 * 7/10/2015 modifying this testware to simply raise the authentication credentials to admin
		JSONArray jsonSubscriptions = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/owners/"+ownerKey+"/subscriptions"));	
		 */
		JSONArray jsonSubscriptions = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,"/owners/"+ownerKey+"/subscriptions"));	
		JSONObject jsonSubscription = null;
		for (int i = 0; i < jsonSubscriptions.length(); i++) {
			jsonSubscription = (JSONObject) jsonSubscriptions.get(i);
			if (jsonSubscription.getString("id").equals(subscriptionId)) {break;} else {jsonSubscription=null;}
		}
		Calendar startDate = parseISO8601DateString(jsonSubscription.getString("startDate"),"GMT");	// "startDate":"2012-02-08T00:00:00.000+0000"
		Calendar newStartDate = (Calendar) startDate.clone(); newStartDate.add(Calendar.MONTH, -1);	// subtract a month
		updateSubscriptionDatesOnDatabase(subscriptionId,newStartDate,null);

		log.info("Now let's refresh the subscription pools...");
		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,ownerKey);
		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,jobDetail,"FINISHED", 10*1000, 3);
		allAvailablePools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();

		// retrieve the host pool again and assert the quantity has not changed
		hostPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", hostPoolId, allAvailablePools);
		Assert.assertEquals(hostPool.quantity, hostPoolQuantityBefore, "The quantity of entitlements available from the host pool has NOT changed after refreshing pools.");
		
		// retrieve the guest pool again and assert the quantity has not changed
		guestPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", guestPoolId, allAvailablePools);
		Assert.assertEquals(guestPool.quantity, guestPoolQuantityBefore, "The quantity of entitlements available from the guest pool has NOT changed after refreshing pools.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27131", "RHEL7-64494"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Verify host and guest pools to a virtualization-aware subscription are subscribable on a guest system (unless it is physical_only).",
			groups={"Tier2Tests","VerifyHostAndGuestPoolsAreSubscribableOnGuestSystem_Test"},
			dependsOnGroups={},
			dataProvider="getVirtSubscriptionData",
			enabled=true)
	public void testHostAndGuestPoolsAreSubscribableOnGuestSystem(String subscriptionId, String productName, String productId, int quantity, String virtLimit, String hostPoolId, String guestPoolId, Boolean physicalOnly) throws JSONException, Exception {
		if (hostPoolId==null && guestPoolId==null) throw new SkipException("Failed to find expected host and guest pools derived from virtualization-aware subscription id '"+subscriptionId+"' ("+productName+").");
		
		// trick this system into believing it is a virt guest
		forceVirtWhatToReturnGuest("kvm");
		
		// assert that the hostPoolId is available...
		List<SubscriptionPool> availablePools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool hostPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", hostPoolId, availablePools);
		if (physicalOnly==null || !physicalOnly) {	
			// ...when the originating subscription is not physical_only
			Assert.assertNotNull(hostPool,"A host pool derived from the virtualization-aware subscription id '"+subscriptionId+"' is available on a guest system.  hostPool="+hostPool);
			// attempt to subscribe to the hostPoolId (should succeed)
			//clienttasks.subscribeToSubscriptionPool(hostPool);	// too much overhead
			clienttasks.subscribe(null, null, hostPoolId, null, null, null, null, null, null, null, null, null, null);
		} else {
			// ...but not when the originating subscription is physical_only
			Assert.assertNull(hostPool,"A host pool derived from the virtualization-aware subscription id '"+subscriptionId+"' that is physical_only is NOT available on a guest system.");	// introduced by Bug 1066120
			
			// however, it should still be available from --all --available
			hostPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", hostPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
			Assert.assertNotNull(hostPool,"A host pool derived from the virtualization-aware subscription id '"+subscriptionId+"' that is physical_only is included in --all available on a guest system.");	// introduced by Bug 1066120
			Assert.assertEquals(hostPool.machineType, "Physical", "The machine type for a physical_only pool.");
			// attempt to subscribe to the hostPoolId (should fail)
			//	[root@jsefler-7 ~]# subscription-manager attach --pool 8a9087e3443db08f01443db1810c125e
			//	Pool is restricted to physical systems: '8a9087e3443db08f01443db1810c125e'.
			SSHCommandResult result = clienttasks.subscribe_(null, null, hostPoolId, null, null, null, null, null, null, null, null, null, null);
			String expectedMsg = String.format("Pool is restricted to physical systems: '%s'.", hostPoolId);
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
				expectedMsg = String.format("Pool is restricted to physical systems: \"%s\".",hostPoolId);
			}
			Assert.assertEquals(result.getStdout().trim(), expectedMsg, "Stdout from an attempt to subscribe a virtual system to physical_only pool: "+hostPool);
			Assert.assertEquals(result.getStderr(), "", "Stderr from an attempt to subscribe a virtual system to physical_only pool: "+hostPool);
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(1), "Exitcode from an attempt to subscribe a virtual system to physical_only pool: "+hostPool);
		}
		
		// assert that the guestPoolId is available
		SubscriptionPool guestPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", guestPoolId, availablePools);
		Assert.assertNotNull(guestPool,"A guest pool derived from the virtualization-aware subscription id '"+subscriptionId+"' is available on a guest system: "+guestPool);

		// attempt to subscribe to the guestPoolId
		clienttasks.subscribeToSubscriptionPool(guestPool);
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27132", "RHEL7-64495"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Verify only the derived host pool from a virtualization-aware subscription is subscribable on a host system.  The guest pool should not be available nor subscribable.",
			groups={"Tier2Tests","VerifyHostPoolIsSubscribableOnHostSystemWhileGuestPoolIsNot_Test"},
			dependsOnGroups={},
			dataProvider="getVirtSubscriptionData",
			enabled=true)
	public void testHostPoolIsSubscribableOnHostSystemWhileGuestPoolIsNot(String subscriptionId, String productName, String productId, int quantity, String virtLimit, String hostPoolId, String guestPoolId, Boolean physicalOnly) throws JSONException, Exception {
		if (hostPoolId==null && guestPoolId==null) throw new SkipException("Failed to find expected host and guest pools derived from virtualization-aware subscription id '"+subscriptionId+"' ("+productName+").");

		// trick this system into believing it is a host
		forceVirtWhatToReturnHost();
		
		// assert that the hostPoolId is available
		List<SubscriptionPool> availablePools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool hostPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", hostPoolId, availablePools);
		Assert.assertNotNull(hostPool,"A host pool derived from the virtualization-aware subscription id '"+subscriptionId+"' is available on a host system: "+hostPool);

		// assert that the guestPoolId is NOT available
		SubscriptionPool guestPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", guestPoolId, availablePools);
		Assert.assertNull(guestPool,"A guest pool derived from the virtualization-aware subscription id '"+subscriptionId+"' should NOT be available on a host system: "+guestPool);

		// attempt to subscribe to the hostPoolId
		clienttasks.subscribeToSubscriptionPool(hostPool);

		// attempt to subscribe to the guestPoolId (should be blocked)
		SSHCommandResult result = clienttasks.subscribe(null,null,guestPoolId,null,null,null,null,null, null, null, null, null, null);
		// Unable to entitle consumer to the pool with id '8a90f8b42e3e7f2e012e3e7fc653013e'.: rulefailed.virt.only
		//Assert.assertContainsMatch(result.getStdout(), "^Unable to entitle consumer to the pool with id '"+guestPoolId+"'.:");
		// RHEL58: Pool is restricted to virtual guests: '8a90f85734205a010134205ae8d80403'.
		String expectedStdout = "Pool is restricted to virtual guests: '"+guestPoolId+"'.";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
			expectedStdout = String.format("Pool is restricted to virtual guests: \"%s\".",guestPoolId);
		}
		Assert.assertEquals(result.getStdout().trim(), expectedStdout);

	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37713", "RHEL7-51490"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Verify the subscription-manager list --avail appropriately displays pools with MachineType: virtual",
			groups={"Tier2Tests","VerifyVirtualMachineTypeIsReportedInListAvailablePools_Test"},
			dependsOnGroups={},
			enabled=true)
	public void testVirtualMachineTypeIsReportedInListAvailablePools() throws JSONException, Exception {

		// trick this system into believing it is a virt guest
		forceVirtWhatToReturnGuest("kvm");
		
		boolean poolFound = false;
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if (CandlepinTasks.isPoolVirtOnly (sm_clientUsername,sm_clientPassword,pool.poolId,sm_serverUrl)) {
				//Assert.assertEquals(pool.machineType, "virtual", "MachineType:virtual should be displayed in the available Subscription Pool listing when the pool has a virt_only=true attribute.  Pool: "+pool);
				//Assert.assertEquals(pool.machineType, "Virtual", "MachineType: Virtual should be displayed in the available Subscription Pool listing when the pool has a virt_only=true attribute.  Pool: "+pool);	// updated after Bug 864184 - String Update: Capitalize Machine Type value from 'subscription-manager --available'
				Assert.assertEquals(pool.machineType, "Virtual", "System Type: Virtual should be displayed in the available Subscription Pool listing when the pool has a virt_only=true attribute.  Pool: "+pool);		// changed by bug 874760
				poolFound = true;
			}
		}
		if (!poolFound) throw new SkipException("Could not find an available pool with which to verify the MachineType:virtual is reported in the Subscription Pool listing.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37712", "RHEL7-51489"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Verify the subscription-manager list --avail appropriately displays pools with MachineType: physical",
			groups={"Tier2Tests","VerifyPhysicalMachineTypeValuesInListAvailablePools_Test"},
			dependsOnGroups={},
			enabled=true)
	public void testPhysicalMachineTypeValuesInListAvailablePools() throws JSONException, Exception {

		// trick this system into believing it is a host
		forceVirtWhatToReturnHost();
		
		boolean poolFound = false;
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if (!CandlepinTasks.isPoolVirtOnly (sm_clientUsername,sm_clientPassword,pool.poolId,sm_serverUrl)) {
				//Assert.assertEquals(pool.machineType, "physical", "MachineType:physical should be displayed in the available Subscription Pool listing when the pool has a virt_only=false attribute (or absense of a virt_only attribute).  Pool: "+pool);
				//Assert.assertEquals(pool.machineType, "Physical", "MachineType: Physical should be displayed in the available Subscription Pool listing when the pool has a virt_only=false attribute (or absense of a virt_only attribute).  Pool: "+pool);	// updated after Bug 864184 - String Update: Capitalize Machine Type value from 'subscription-manager --available'
				Assert.assertEquals(pool.machineType, "Physical", "System Type: Physical should be displayed in the available Subscription Pool listing when the pool has a virt_only=false attribute (or absense of a virt_only attribute).  Pool: "+pool);	// changed by bug 874760
				poolFound = true;
			}
		}
		if (!poolFound) throw new SkipException("Could not find an available pool with which to verify the MachineType:physical is reported in the Subscription Pool listing.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37711", "RHEL7-51488"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Verify the Candlepin API accepts PUTting of guestIds onto host consumer (to be used by virt-who)",
			groups={"Tier2Tests","blockedByBug-737935","VerifyGuestIdsCanBePutOntoHostConsumer_Test"},
			dependsOnGroups={},
			enabled=true)
	public void testGuestIdsCanBePutOntoHostConsumer() throws JSONException, Exception {

		int k=1; JSONObject jsonConsumer; List<String> actualGuestIds = new ArrayList<String>(){};
		
		// trick this system into believing it is a host
		forceVirtWhatToReturnHost();
		
		// create host consumer A
		String consumerIdOfHostA = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null));
		
		for (int c=0;c<2;c++) { // run this test twice
			
			// call Candlepin API to PUT some guestIds onto the host consumer A
			JSONObject jsonData = new JSONObject();
			List<String> expectedGuestIdsOnHostA = Arrays.asList(new String[]{"test-guestId"+k++,"test-guestId"+k++}); 
			jsonData.put("guestIds", expectedGuestIdsOnHostA);
			CandlepinTasks.putResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerIdOfHostA, jsonData);
			
			// get the host consumer and assert that it has all the guestIds just PUT
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.29-1")) {	// candlepin commit 9eb578122851bdd3d5bb67f205d29996fc91e0ec Remove guestIds from the consumer json output
				// actual guestIds
				actualGuestIds = CandlepinTasks.getConsumerGuestIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerIdOfHostA);
			} else {
				jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerIdOfHostA));
				// actual guestIds
				//DEBUGGING jsonConsumer.put("guestIds", new JSONArray(expectedGuestIdsOnHostA));
				actualGuestIds.clear();
				for (int g=0; g<jsonConsumer.getJSONArray("guestIds").length(); g++) {
					actualGuestIds.add(jsonConsumer.getJSONArray("guestIds").getJSONObject(g).getString("guestId"));
				}
			}
			// assert expected guestIds
			for (String guestId : expectedGuestIdsOnHostA) Assert.assertContains(actualGuestIds, guestId);
			Assert.assertEquals(actualGuestIds.size(), expectedGuestIdsOnHostA.size(),"All of the expected guestIds PUT on host consumer '"+consumerIdOfHostA+"' using the Candlepin API were verified.");

		
			
			// Now let's create a second host consumer B and add its own guestIds to it and assert the same test
			clienttasks.clean();	// this will keep consumer A registered
			String consumerIdOfHostB = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null, null));

			// call Candlepin API to PUT some guestIds onto the host consumer B
			List<String> expectedGuestIdsOnHostB = Arrays.asList(new String[]{"test-guestId"+k++,"test-guestId"+k++,"test-guestId"+k++,"test-guestId"+k++}); 
			jsonData.put("guestIds", expectedGuestIdsOnHostB);
			CandlepinTasks.putResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerIdOfHostB, jsonData);

			// get the host consumer and assert that it has all the guestIds just PUT
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.29-1")) {	// candlepin commit 9eb578122851bdd3d5bb67f205d29996fc91e0ec Remove guestIds from the consumer json output
				// actual guestIds
				actualGuestIds = CandlepinTasks.getConsumerGuestIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerIdOfHostB);
			} else {
				jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerIdOfHostB));
				// actual guestIds
				//DEBUGGING jsonConsumer.put("guestIds", new JSONArray(expectedGuestIdsOnHostB));
				actualGuestIds.clear();
				//[root@jsefler-stage-6server ~]# curl --insecure --user testuser1:password --request GET https://jsefler-f14-5candlepin.usersys.redhat.com:8443/candlepin/consumers/8b7fe5e5-7178-4bad-b686-2ff8c6c19112 | python -m simplejson/tool
				//  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
				//                                 Dload  Upload   Total   Spent    Left  Speed
				//100 14242    0 14242    0     0  76993      0 --:--:-- --:--:-- --:--:--  135k
				//{
				//<cut>
				//    "guestIds": [
				//        {
				//            "created": "2011-11-23T18:01:15.325+0000", 
				//            "guestId": "test-guestId2", 
				//            "id": "8a90f85733cefc4c0133d196b73d6d26", 
				//            "updated": "2011-11-23T18:01:15.325+0000"
				//        }, 
				//        {
				//            "created": "2011-11-23T18:01:15.293+0000", 
				//            "guestId": "test-guestId1", 
				//            "id": "8a90f85733cefc4c0133d196b71d6d23", 
				//            "updated": "2011-11-23T18:01:15.293+0000"
				//        }
				//    ], 
				//<cut>
				//    "username": "testuser1", 
				//    "uuid": "8b7fe5e5-7178-4bad-b686-2ff8c6c19112"
				//}
				for (int g=0; g<jsonConsumer.getJSONArray("guestIds").length(); g++) {
					actualGuestIds.add(jsonConsumer.getJSONArray("guestIds").getJSONObject(g).getString("guestId"));
				}
			}
			// assert expected guestIds
			for (String guestId : expectedGuestIdsOnHostB) Assert.assertContains(actualGuestIds, guestId);
			Assert.assertEquals(actualGuestIds.size(), expectedGuestIdsOnHostB.size(),"All of the expected guestIds PUT on consumer '"+consumerIdOfHostB+"' using the Candlepin API were verified.");

			
			
			// Now let's re-verify that the guestIds of host consumer A have not changed
			// get the host consumer and assert that it has all the guestIds just PUT
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.29-1")) {	// candlepin commit 9eb578122851bdd3d5bb67f205d29996fc91e0ec Remove guestIds from the consumer json output
				// actual guestIds
				actualGuestIds = CandlepinTasks.getConsumerGuestIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerIdOfHostA);
			} else {
				jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerIdOfHostA));
				// actual guestIds
				//DEBUGGING jsonConsumer.put("guestIds", new JSONArray(expectedGuestIdsOnHostA));
				actualGuestIds.clear();
				for (int g=0; g<jsonConsumer.getJSONArray("guestIds").length(); g++) {
					actualGuestIds.add(jsonConsumer.getJSONArray("guestIds").getJSONObject(g).getString("guestId"));
				}
			}
			// assert expected guestIds
			for (String guestId : expectedGuestIdsOnHostA) Assert.assertContains(actualGuestIds, guestId);
			Assert.assertEquals(actualGuestIds.size(), expectedGuestIdsOnHostA.size(),"All of the expected guestIds PUT on consumer '"+consumerIdOfHostA+"' using the Candlepin API were verified.");

		}
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47927", "RHEL7-97326"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28576",	// RHSM-REQ : Host-limited Guest Subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.RELATES_TO),
				@LinkedItem(
					workitemId= "RHEL7-84958",	// RHSM-REQ : Host-limited Guest Subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.RELATES_TO)},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Verify the Candlepin API denies PUTting of guestIds onto a guest consumer",
			groups={"Tier2Tests","blockedByBug-737935","VerifyGuestIdsCanNOTBePutOntoGuestConsumer_Test"},
			dependsOnGroups={},
			enabled=true)
	public void testGuestIdsCanNOTBePutOntoGuestConsumer() throws JSONException, Exception {

		int k=1; JSONObject jsonConsumer; List<String> actualGuestIds = new ArrayList<String>(){};
		
		// trick this system into believing it is a guest
		forceVirtWhatToReturnGuest("kvm");
		
		// create a guest consumer
		String consumerIdOfGuest = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null));
			
		// call Candlepin API to PUT some guestIds onto the guest consumer
		JSONObject jsonData = new JSONObject();
		List<String> expectedGuestIds = Arrays.asList(new String[]{"test-guestId"+k++,"test-guestId"+k++}); 
		jsonData.put("guestIds", expectedGuestIds);
		String result = CandlepinTasks.putResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerIdOfGuest, jsonData);
		
		// assert that ^ PUT request failed
		// TODO assert the result
		
		// get the consumer and assert that it has None of the guestIds just PUT
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.29-1")) {	// candlepin commit 9eb578122851bdd3d5bb67f205d29996fc91e0ec Remove guestIds from the consumer json output
			// actual guestIds
			actualGuestIds = CandlepinTasks.getConsumerGuestIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerIdOfGuest);
		} else {
			jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerIdOfGuest));
			// actual guestIds
			//DEBUGGING jsonConsumer.put("guestIds", new JSONArray(expectedGuestIds));
			actualGuestIds.clear();
			for (int g=0; g<jsonConsumer.getJSONArray("guestIds").length(); g++) {
				actualGuestIds.add(jsonConsumer.getJSONArray("guestIds").getJSONObject(g).getString("guestId"));
			}
		}
		log.info("Consumer '"+consumerIdOfGuest+"' guestIds: "+actualGuestIds);
		// assert expected guestIds are empty (TODO or NULL?)
		if (actualGuestIds.size()>0) {throw new SkipException("This testcase is effectively a simulation of virt-who running on a guest and reporting that the guest has guests of its own. This is NOT a realistic scenario and Candlepin is currently not programmed to block this PUT.  No bugzilla has been opened.  Skipping this test until needed in the future.");};
		Assert.assertEquals(actualGuestIds, new ArrayList<String>(){},"A guest '"+consumerIdOfGuest+"' consumer should not be allowed to have guestIds PUT on it using the Candlepin API.");
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47926", "RHEL7-97325"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28576",	// RHSM-REQ : Host-limited Guest Subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.RELATES_TO),
				@LinkedItem(
					workitemId= "RHEL7-84958",	// RHSM-REQ : Host-limited Guest Subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.RELATES_TO)},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="When Host B PUTs the same guestId as HostA, the guestId should be removed from HostA (simulation of a guest moved from Host A to Host B)",
			groups={"Tier2Tests","blockedByBug-737935","VerifyGuestIdIsRemovedFromHostConsumerAWhenHostConsumerBPutsSameGuestId_Test"},
			dependsOnGroups={},
			enabled=true)
	public void testGuestIdIsRemovedFromHostConsumerAWhenHostConsumerBPutsSameGuestId() throws JSONException, Exception {

		int k=1; JSONObject jsonConsumer; List<String> actualGuestIds = new ArrayList<String>(){};
		
		// trick this system into believing it is a host
		forceVirtWhatToReturnHost();
		
		// create host consumer A
		String consumerIdOfHostA = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null));
		
		for (int c=0;c<2;c++) { // run this test twice
			
			// call Candlepin API to PUT some guestIds onto the host consumer A
			JSONObject jsonData = new JSONObject();
			ArrayList<String> expectedGuestIdsOnHostA = new ArrayList<String>(){};
			for (String guestId :  Arrays.asList(new String[]{"test-guestId"+k++,"test-guestId"+k++})) expectedGuestIdsOnHostA.add(guestId);

			jsonData.put("guestIds", expectedGuestIdsOnHostA);
			CandlepinTasks.putResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerIdOfHostA, jsonData);
			
			// get the host consumer and assert that it has all the guestIds just PUT
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.29-1")) {	// candlepin commit 9eb578122851bdd3d5bb67f205d29996fc91e0ec Remove guestIds from the consumer json output
				// actual guestIds
				actualGuestIds = CandlepinTasks.getConsumerGuestIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerIdOfHostA);
			} else {
				jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerIdOfHostA));
				// actual guestIds
				//DEBUGGING jsonConsumer.put("guestIds", new JSONArray(expectedGuestIdsOnHostA));
				actualGuestIds.clear();
				for (int g=0; g<jsonConsumer.getJSONArray("guestIds").length(); g++) {
					actualGuestIds.add(jsonConsumer.getJSONArray("guestIds").getJSONObject(g).getString("guestId"));
				}
			}
			// assert expected guestIds
			for (String guestId : expectedGuestIdsOnHostA) Assert.assertContains(actualGuestIds, guestId);
			Assert.assertEquals(actualGuestIds.size(), expectedGuestIdsOnHostA.size(),"All of the expected guestIds PUT on host consumer '"+consumerIdOfHostA+"' using the Candlepin API were verified.");

		
			
			// Now let's create a second host consumer B and add its own guestIds to it and assert the same test
			clienttasks.clean();	// this will keep consumer A registered
			String consumerIdOfHostB = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null, null));

			// call Candlepin API to PUT some guestIds onto the host consumer B
			// NOTE: decrementing k will effectively move the last guestId from HostA to HostB
			k--;
			log.info("Simulating the moving of guestId '"+expectedGuestIdsOnHostA.get(expectedGuestIdsOnHostA.size()-1)+"' from host consumer A to host consumer B by PUTting it on the list of guestIds for host consumer B...");
			List<String> expectedGuestIdsOnHostB = Arrays.asList(new String[]{"test-guestId"+k++,"test-guestId"+k++,"test-guestId"+k++,"test-guestId"+k++}); 
			jsonData.put("guestIds", expectedGuestIdsOnHostB);
			CandlepinTasks.putResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerIdOfHostB, jsonData);

			// get the host consumer and assert that it has all the guestIds just PUT
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.29-1")) {	// candlepin commit 9eb578122851bdd3d5bb67f205d29996fc91e0ec Remove guestIds from the consumer json output
				// actual guestIds
				actualGuestIds = CandlepinTasks.getConsumerGuestIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerIdOfHostB);
			} else {
				jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerIdOfHostB));
				// actual guestIds
				//DEBUGGING jsonConsumer.put("guestIds", new JSONArray(expectedGuestIdsOnHostB));
				actualGuestIds.clear();
				for (int g=0; g<jsonConsumer.getJSONArray("guestIds").length(); g++) {
					actualGuestIds.add(jsonConsumer.getJSONArray("guestIds").getJSONObject(g).getString("guestId"));
				}
			}
			// assert expected guestIds
			for (String guestId : expectedGuestIdsOnHostB) Assert.assertContains(actualGuestIds, guestId);
			Assert.assertEquals(actualGuestIds.size(), expectedGuestIdsOnHostB.size(),"All of the expected guestIds PUT on consumer '"+consumerIdOfHostB+"' using the Candlepin API were verified.");

			
			
			// Now let's re-verify that the guestIds of host consumer A have not changed
			// NOTE: The last guestId SHOULD BE REMOVED since it was most recently reported as a guest on HostB
			log.info("Because guestId '"+expectedGuestIdsOnHostA.get(expectedGuestIdsOnHostA.size()-1)+"' was most recently reported as a guest on host consumer B, it should no longer be on the list of guestIds for host consumer A...");
			expectedGuestIdsOnHostA.remove(expectedGuestIdsOnHostA.size()-1);

			// get the host consumer and assert that it has all the guestIds just PUT
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.29-1")) {	// candlepin commit 9eb578122851bdd3d5bb67f205d29996fc91e0ec Remove guestIds from the consumer json output
				// actual guestIds
				actualGuestIds = CandlepinTasks.getConsumerGuestIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerIdOfHostA);
			} else {
				jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerIdOfHostA));
				// actual guestIds
				//DEBUGGING jsonConsumer.put("guestIds", new JSONArray(expectedGuestIdsOnHostA));
				actualGuestIds.clear();
				for (int g=0; g<jsonConsumer.getJSONArray("guestIds").length(); g++) {
					actualGuestIds.add(jsonConsumer.getJSONArray("guestIds").getJSONObject(g).getString("guestId"));
				}
			}
			// assert expected guestIds
			for (String guestId : expectedGuestIdsOnHostA) Assert.assertContains(actualGuestIds, guestId);
			if (actualGuestIds.size() == expectedGuestIdsOnHostA.size()+c+1) throw new SkipException("Currently Candlepin does NOT purge duplicate guest ids PUT by virt-who onto different host consumers.  The most recently PUT guest id is the winner. Entitlements should be revoked for the older guest id.  Development has decided to keep the stale guest id for potential reporting purposes, hence this test is being skipped until needed in the future."); else
			Assert.assertEquals(actualGuestIds.size(), expectedGuestIdsOnHostA.size(),"All of the expected guestIds PUT on consumer '"+consumerIdOfHostA+"' using the Candlepin API were verified.");

		}
	}
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 683459 - Virt only skus creating two pools https://github.com/RedHatQE/rhsm-qe/issues/220
	// TODO Bug 736436 - virtual subscriptions are not included when the certificates are downloaded https://github.com/RedHatQE/rhsm-qe/issues/221
	// TODO Bug 750659 - candlepin api /consumers/<consumerid>/guests is returning [] https://github.com/RedHatQE/rhsm-qe/issues/222
	// TODO Bug 756628 - Unable to entitle consumer to the pool with id '8a90f85733d31add0133d337f9410c52'.: virt.guest.host.does.not.match.pool.owner https://github.com/RedHatQE/rhsm-qe/issues/223
	// TODO Bug 722977 - virt_only pools are not removed from an owner if the physical pool no longer has a valid virt_limit https://github.com/RedHatQE/rhsm-qe/issues/224
	
	
	
	
	
	
	// Configuration methods ***********************************************************************
		
	@BeforeClass(groups="setup")
	public void backupVirtWhatBeforeClass() {
		// finding location of virt-what...
		SSHCommandResult result = client.runCommandAndWait("which virt-what");
		virtWhatFile = new File(result.getStdout().trim());
		Assert.assertTrue(RemoteFileTasks.testFileExists(client, virtWhatFile.getPath())==1,"virt-what is in the client's path");
		
		// making a backup of virt-what...
		virtWhatFileBackup = new File(virtWhatFile.getPath()+".bak");
		//RemoteFileTasks.runCommandAndAssert(client, "cp -np "+virtWhatFile+" "+virtWhatFileBackup, 0); // cp option -n does not exist on RHEL5 
		if (RemoteFileTasks.testFileExists(client, virtWhatFileBackup.getPath())==0) {
			RemoteFileTasks.runCommandAndAssert(client, "cp -p "+virtWhatFile+" "+virtWhatFileBackup, 0);
		}
		Assert.assertTrue(RemoteFileTasks.testFileExists(client, virtWhatFileBackup.getPath())==1,"successfully made a backup of virt-what to: "+virtWhatFileBackup);

	}
	
	@AfterGroups(groups="setup", value={"VirtFactsWhenClientIsAGuest_Test","VirtFactsWhenClientIsAHost_Test","VirtFactsWhenVirtWhatFails_Test","VerifyHostAndGuestPoolQuantities_Test","VerifyHostAndGuestPoolsAreSubscribableOnGuestSystem_Test","VerifyHostPoolIsSubscribableOnHostSystemWhileGuestPoolIsNot_Test","VerifyVirtualMachineTypeIsReportedInListAvailablePools_Test","VerifyPhysicalMachineTypeValuesInListAvailablePools_Test","VerifyGuestIdsCanBePutOntoHostConsumer_Test","VerifyGuestIdsCanNOTBePutOntoGuestConsumer_Test","VerifyGuestIdIsRemovedFromHostConsumerAWhenHostConsumerBPutsSameGuestId_Test"})
	public void restoreVirtWhatAfterGroups() {
		// restoring backup of virt-what
		if (virtWhatFileBackup!=null && RemoteFileTasks.testExists(client, virtWhatFileBackup.getPath())) {
			RemoteFileTasks.runCommandAndAssert(client, "rm -f "+virtWhatFile+" && cp "+virtWhatFileBackup+" "+virtWhatFile, 0);
		}
	}
	
	@AfterClass(groups="setup")
	public void restoreVirtWhatAfterClass() {
		// restoring backup of virt-what
		if (virtWhatFileBackup!=null && RemoteFileTasks.testFileExists(client, virtWhatFileBackup.getPath())==1) {
			RemoteFileTasks.runCommandAndAssert(client, "mv -f "+virtWhatFileBackup+" "+virtWhatFile, 0);
		}
	}
	
	@BeforeClass(groups="setup")
	public void registerBeforeClass() throws Exception {
		clienttasks.unregister(null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null));
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);
	}
	
	@BeforeMethod(groups="setup")
	public void unsubscribeBeforeMethod() throws Exception {
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
	}
	
	// protected methods ***********************************************************************
	
	protected String ownerKey = "";
	protected File virtWhatFile = null;
	protected File virtWhatFileBackup = null;
	
	protected void forceVirtWhatToReturnGuest(String hypervisorType) {
		// Note: when client is a guest, virt-what returns stdout="<hypervisor type>" and exitcode=0
		RemoteFileTasks.runCommandAndWait(client,"echo '#!/bin/bash - ' > "+virtWhatFile+"; echo -e 'echo -e \""+hypervisorType+"\"' >> "+virtWhatFile+"; chmod a+x "+virtWhatFile, TestRecords.action());
	}
	
	protected void forceVirtWhatToReturnHost() {
		// Note: when client is a host, virt-what returns stdout="" and exitcode=0
		RemoteFileTasks.runCommandAndWait(client,"echo '#!/bin/bash - ' > "+virtWhatFile+"; echo 'exit 0' >> "+virtWhatFile+"; chmod a+x "+virtWhatFile, TestRecords.action());
	}
	
	protected void forceVirtWhatToFail() {
		// Note: when virt-what does not know if the system is on bare metal or on a guest, it returns a non-zero value
		RemoteFileTasks.runCommandAndWait(client,"echo '#!/bin/bash - ' > "+virtWhatFile+"; echo 'echo \"virt-what is about to exit with code 255\"; exit 255' >> "+virtWhatFile+"; chmod a+x "+virtWhatFile, TestRecords.action());
	}
	
	
	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="getVirtWhatData")
	public Object[][] getVirtWhatDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getVirtWhatDataAsListOfLists());
	}
	protected List<List<Object>> getVirtWhatDataAsListOfLists(){
		List<List<Object>> ll = new ArrayList<List<Object>>();

		// man virt-what (virt-what-1.3-4.4.el6.x86_64) shows support for the following hypervisors

		//									Object bugzilla, String host_type
		ll.add(Arrays.asList(new Object[]{null,	"hyperv"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1018807","1438085"}),	"ibm_systemz\nibm_systemz-direct"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1018807","1438085"}),	"ibm_systemz\nibm_systemz-lpar"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1018807","1438085"}),	"ibm_systemz\nibm_systemz-zvm"}));
		ll.add(Arrays.asList(new Object[]{null,	"linux_vserver\nlinux_vserver-host"}));
		ll.add(Arrays.asList(new Object[]{null,	"linux_vserver\nlinux_vserver-guest"}));
		ll.add(Arrays.asList(new Object[]{null,	"lxc"}));
		ll.add(Arrays.asList(new Object[]{null,	"kvm"}));
		ll.add(Arrays.asList(new Object[]{null,	"openvz"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1438085"}),	"powervm_lx86"}));
		ll.add(Arrays.asList(new Object[]{null,	"qemu"}));
		ll.add(Arrays.asList(new Object[]{null,	"uml"}));
		ll.add(Arrays.asList(new Object[]{null,	"virt"}));
		ll.add(Arrays.asList(new Object[]{null,	"virtage"}));
		ll.add(Arrays.asList(new Object[]{null,	"virtualage"}));
		ll.add(Arrays.asList(new Object[]{null,	"virtualbox"}));
		ll.add(Arrays.asList(new Object[]{null,	"virtualpc"}));
		ll.add(Arrays.asList(new Object[]{null,	"vmware"}));
		ll.add(Arrays.asList(new Object[]{null,	"xen"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1018807"}),	"xen\nxen-domU"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1018807"}),	"xen\nxen-hvm"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1018807","757697","1308732"}),	"xen\nxen-dom0"}));

		return ll;
	}
	
	
	@DataProvider(name="getVirtSubscriptionData")
	public Object[][] getVirtSubscriptionDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getVirtSubscriptionDataAsListOfLists());
	}
	protected List<List<Object>> getVirtSubscriptionDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (servertasks.statusStandalone) {log.warning("This candlepin server is configured for standalone operation.  The hosted virtualization model tests will not be executed."); return ll;}
		
		Calendar now = new GregorianCalendar();
		now.setTimeInMillis(System.currentTimeMillis());
		
		/* 7/10/2015 devel consciously decided to drop @Verify(value = Owner.class, subResource = SubResource.SUBSCRIPTIONS) on this GET method starting with candlepin-2.0.
		 * 7/10/2015 modifying this testware to simply raise the authentication credentials to admin
		JSONArray jsonSubscriptions = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,sm_clientPassword,sm_serverUrl,"/owners/"+ownerKey+"/subscriptions"));	
		 */
		JSONArray jsonSubscriptions = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,"/owners/"+ownerKey+"/subscriptions"));	
		LOOP_FOR_ALL_JSON_SUBSCRIPTIONS: for (int i = 0; i < jsonSubscriptions.length(); i++) {
			JSONObject jsonSubscription = (JSONObject) jsonSubscriptions.get(i);
			String subscriptionId = jsonSubscription.getString("id");
			Calendar startDate = parseISO8601DateString(jsonSubscription.getString("startDate"),"GMT");	// "startDate":"2012-02-08T00:00:00.000+0000"
			Calendar endDate = parseISO8601DateString(jsonSubscription.getString("endDate"),"GMT");	// "endDate":"2013-02-07T00:00:00.000+0000"
			int quantity = jsonSubscription.getInt("quantity");
			JSONObject jsonProduct = (JSONObject) jsonSubscription.getJSONObject("product");
			String productName = jsonProduct.getString("name");
			String productId = jsonProduct.getString("id");
			JSONArray jsonAttributes = jsonProduct.getJSONArray("attributes");
			
			// skip host_limited subscriptions (host_limited subscriptions are exercised by InstanceTests and DataCenterTests)
			// loop through the attributes of this jsonProduct looking for the "host_limited" attribute
			for (int j = 0; j < jsonAttributes.length(); j++) {
				JSONObject jsonAttribute = (JSONObject) jsonAttributes.get(j);
				String attributeName = jsonAttribute.getString("name");
				if (attributeName.equals("host_limited")) {
					continue LOOP_FOR_ALL_JSON_SUBSCRIPTIONS;
				}
			}
			
			// loop through the attributes of this jsonProduct looking for the "physical_only" attribute
			// introduced by Bug 1066120 - [RFE] need a way to restrict a subscription pool as being consumable only by a physical system
			Boolean physicalOnly = null;
			for (int j = 0; j < jsonAttributes.length(); j++) {
				JSONObject jsonAttribute = (JSONObject) jsonAttributes.get(j);
				String attributeName = jsonAttribute.getString("name");
				if (attributeName.equals("physical_only")) {
					physicalOnly = Boolean.valueOf(jsonAttribute.getString("value"));
				}
			}
			
			// loop through the attributes of this jsonProduct looking for the "virt_limit" attribute
			for (int j = 0; j < jsonAttributes.length(); j++) {
				JSONObject jsonAttribute = (JSONObject) jsonAttributes.get(j);
				String attributeName = jsonAttribute.getString("name");
				if (attributeName.equals("virt_limit")) {
					// found the virt_limit attribute - get its value
					String virt_limit = jsonAttribute.getString("value");
					
					// only retrieve data that is valid today (at this time)
					if (startDate.before(now) && endDate.after(now)) {

//						// save some computation cycles in the testcases and get the hostPoolId and guestPoolId
//						List<String> poolIds = CandlepinTasks.getPoolIdsForSubscriptionId(sm_clientUsername,sm_clientPassword,sm_serverUrl,ownerKey,subscriptionId);
//
//						// determine which pool is for the guest, the other must be for the host
//						String guestPoolId = null;
//						String hostPoolId = null;
//						for (String poolId : poolIds) {
//							if (CandlepinTasks.isPoolVirtOnly (sm_clientUsername,sm_clientPassword,poolId,sm_serverUrl)) {
//								guestPoolId = poolId;
//							} else {
//								hostPoolId = poolId;
//							}
//						}
//						if (poolIds.size() != 2) {hostPoolId=null; guestPoolId=null;}	// set pools to null if there was a problem
//						ll.add(Arrays.asList(new Object[]{subscriptionId, productName, productId, quantity, virt_limit, hostPoolId, guestPoolId}));

						
						// save some computation cycles in the testcases and get the hostPoolId and guestPoolId
						List<JSONObject> jsonPools = CandlepinTasks.getPoolsForSubscriptionId(sm_clientUsername,sm_clientPassword,sm_serverUrl,ownerKey,subscriptionId);

						// determine which pool is for the guest, and which is for the host
						String guestPoolId = null;
						String hostPoolId = null;
						for (JSONObject jsonPool : jsonPools) {
							String poolId = jsonPool.getString("id");
							
							if (Boolean.TRUE.toString().equalsIgnoreCase(CandlepinTasks.getPoolAttributeValue(jsonPool, "virt_only")) &&
								Boolean.TRUE.toString().equalsIgnoreCase(CandlepinTasks.getPoolAttributeValue(jsonPool, "pool_derived")) &&
								CandlepinTasks.getPoolAttributeValue(jsonPool, "requires_host")==null) {
								guestPoolId = poolId;
							} else if (
								Boolean.TRUE.toString().equalsIgnoreCase(CandlepinTasks.getPoolAttributeValue(jsonPool, "virt_only")) &&
								Boolean.TRUE.toString().equalsIgnoreCase(CandlepinTasks.getPoolAttributeValue(jsonPool, "pool_derived")) &&
								CandlepinTasks.getPoolAttributeValue(jsonPool, "requires_host")!=null) {
								//newGuestPoolId = poolId;  // TODO THIS IS THE NEW VIRT-AWARE MODEL FOR WHICH NEW TESTS SHOULD BE AUTOMATED
							} else if (
								CandlepinTasks.getPoolAttributeValue(jsonPool, "virt_only")==null &&	// TODO or false?
								CandlepinTasks.getPoolAttributeValue(jsonPool, "pool_derived")==null &&	// TODO or false?
								CandlepinTasks.getPoolAttributeValue(jsonPool, "requires_host")==null) {
								hostPoolId = poolId;
							}
						}
						ll.add(Arrays.asList(new Object[]{subscriptionId, productName, productId, quantity, virt_limit, hostPoolId, guestPoolId, physicalOnly}));
					}
				}
			}
		}
		
		return ll;
	}
	
	/* Example jsonSubscription:
	  {
		    "id": "8a90f8b42e398f7a012e398ff0ef0104",
		    "owner": {
		      "href": "/owners/admin",
		      "id": "8a90f8b42e398f7a012e398f8d310005"
		    },
		    "certificate": null,
		    "product": {
		      "name": "Awesome OS with up to 4 virtual guests",
		      "id": "awesomeos-virt-4",
		      "attributes": [
		        {
		          "name": "variant",
		          "value": "ALL",
		          "updated": "2011-02-18T16:17:37.960+0000",
		          "created": "2011-02-18T16:17:37.960+0000"
		        },
		        {
		          "name": "arch",
		          "value": "ALL",
		          "updated": "2011-02-18T16:17:37.960+0000",
		          "created": "2011-02-18T16:17:37.960+0000"
		        },
		        {
		          "name": "type",
		          "value": "MKT",
		          "updated": "2011-02-18T16:17:37.960+0000",
		          "created": "2011-02-18T16:17:37.960+0000"
		        },
		        {
		          "name": "version",
		          "value": "6.1",
		          "updated": "2011-02-18T16:17:37.961+0000",
		          "created": "2011-02-18T16:17:37.961+0000"
		        },
		        {
		          "name": "virt_limit",
		          "value": "4",
		          "updated": "2011-02-18T16:17:37.960+0000",
		          "created": "2011-02-18T16:17:37.960+0000"
		        }
		      ],
		      "multiplier": 1,
		      "productContent": [

		      ],
		      "dependentProductIds": [

		      ],
		      "href": "/products/awesomeos-virt-4",
		      "updated": "2011-02-18T16:17:37.959+0000",
		      "created": "2011-02-18T16:17:37.959+0000"
		    },
		    "providedProducts": [
		      {
		        "name": "Awesome OS Server Bits",
		        "id": "37060",
		        "attributes": [
		          {
		            "name": "variant",
		            "value": "ALL",
		            "updated": "2011-02-18T16:17:22.174+0000",
		            "created": "2011-02-18T16:17:22.174+0000"
		          },
		          {
		            "name": "sockets",
		            "value": "2",
		            "updated": "2011-02-18T16:17:22.175+0000",
		            "created": "2011-02-18T16:17:22.175+0000"
		          },
		          {
		            "name": "arch",
		            "value": "ALL",
		            "updated": "2011-02-18T16:17:22.175+0000",
		            "created": "2011-02-18T16:17:22.175+0000"
		          },
		          {
		            "name": "type",
		            "value": "SVC",
		            "updated": "2011-02-18T16:17:22.175+0000",
		            "created": "2011-02-18T16:17:22.175+0000"
		          },
		          {
		            "name": "warning_period",
		            "value": "30",
		            "updated": "2011-02-18T16:17:22.175+0000",
		            "created": "2011-02-18T16:17:22.175+0000"
		          },
		          {
		            "name": "version",
		            "value": "6.1",
		            "updated": "2011-02-18T16:17:22.175+0000",
		            "created": "2011-02-18T16:17:22.175+0000"
		          }
		        ],
		        "multiplier": 1,
		        "productContent": [
		          {
		            "content": {
		              "name": "always-enabled-content",
		              "id": "1",
		              "type": "yum",
		              "modifiedProductIds": [

		              ],
		              "label": "always-enabled-content",
		              "vendor": "test-vendor",
		              "contentUrl": "/foo/path/always",
		              "gpgUrl": "/foo/path/always/gpg",
		              "metadataExpire": 200,
		              "updated": "2011-02-18T16:17:16.254+0000",
		              "created": "2011-02-18T16:17:16.254+0000"
		            },
		            "flexEntitlement": 0,
		            "physicalEntitlement": 0,
		            "enabled": true
		          },
		          {
		            "content": {
		              "name": "never-enabled-content",
		              "id": "0",
		              "type": "yum",
		              "modifiedProductIds": [

		              ],
		              "label": "never-enabled-content",
		              "vendor": "test-vendor",
		              "contentUrl": "/foo/path/never",
		              "gpgUrl": "/foo/path/never/gpg",
		              "metadataExpire": 600,
		              "updated": "2011-02-18T16:17:16.137+0000",
		              "created": "2011-02-18T16:17:16.137+0000"
		            },
		            "flexEntitlement": 0,
		            "physicalEntitlement": 0,
		            "enabled": false
		          },
		          {
		            "content": {
		              "name": "content",
		              "id": "1111",
		              "type": "yum",
		              "modifiedProductIds": [

		              ],
		              "label": "content-label",
		              "vendor": "test-vendor",
		              "contentUrl": "/foo/path",
		              "gpgUrl": "/foo/path/gpg/",
		              "metadataExpire": 0,
		              "updated": "2011-02-18T16:17:16.336+0000",
		              "created": "2011-02-18T16:17:16.336+0000"
		            },
		            "flexEntitlement": 0,
		            "physicalEntitlement": 0,
		            "enabled": true
		          }
		        ],
		        "dependentProductIds": [

		        ],
		        "href": "/products/37060",
		        "updated": "2011-02-18T16:17:22.174+0000",
		        "created": "2011-02-18T16:17:22.174+0000"
		      }
		    ],
		    "endDate": "2012-02-18T00:00:00.000+0000",
		    "startDate": "2011-02-18T00:00:00.000+0000",
		    "quantity": 5,
		    "contractNumber": "39",
		    "accountNumber": "12331131231",
		    "modified": null,
		    "tokens": [

		    ],
		    "upstreamPoolId": null,
		    "updated": "2011-02-18T16:17:38.031+0000",
		    "created": "2011-02-18T16:17:38.031+0000"
		  }
	  */
	
	

	
	

}
