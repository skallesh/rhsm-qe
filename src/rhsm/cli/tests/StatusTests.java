package rhsm.cli.tests;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductSubscription;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 *  @author jsefler
 *
 */
@Test(groups={"StatusTests"})
public class StatusTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************
	
	
	@Test(	description="when subscription-manager is run with no args, it should default to the status module",
			groups={"Tier2Tests"},
			enabled=false)	// this test was invalided by Bug 974123 - subscription-manager defaults to status and not to help
			//@ImplementsNitrateTest(caseId=)
	public void testStatusIsTheDefault() {
		String overallStatusLabel = "Overall Status:";
		clienttasks.unregister(null,null,null, null);
		SSHCommandResult defaultResult = RemoteFileTasks.runCommandAndAssert(client,clienttasks.command,Integer.valueOf(0));
		SSHCommandResult statusResult = clienttasks.status(null, null, null, null, null);
		Assert.assertTrue(statusResult.getStdout().contains(overallStatusLabel),"Expected status to report '"+overallStatusLabel+"'.");
		Assert.assertTrue(defaultResult.toString().equals(statusResult.toString()), "When not registered, the default output running subscription-manager with no arguments should be identical to output from the status module.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		defaultResult = RemoteFileTasks.runCommandAndAssert(client,clienttasks.command,Integer.valueOf(0));
		statusResult = clienttasks.status(null, null, null, null, null);
		Assert.assertTrue(defaultResult.toString().split(overallStatusLabel)[0].equals(statusResult.toString().split(overallStatusLabel)[0]), "When registered, the default output running subscription-manager with no arguments should default to the status module.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36569", "RHEL7-51357"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="run subscription-manager status without being registered; status should be Unknown",
			groups={"Tier2Tests"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testStatusWithoutBeingRegistered() {
		SSHCommandResult statusResult;
		clienttasks.unregister(null,null,null, null);
		statusResult = clienttasks.status(null, null, null, null, null);
		
		//	[root@jsefler-5 ~]# subscription-manager status
		//	+-------------------------------------------+
		//	   System Status Details
		//	+-------------------------------------------+
		//	Overall Status: Unknown

		String expectedStatus = "Overall Status: Unknown";
		Assert.assertTrue(statusResult.getStdout().contains(expectedStatus), "Expecting '"+expectedStatus+"' when not registered.");
		Assert.assertEquals(statusResult.getStdout().replaceFirst("\\+-+\\+\\n\\s*System Status Details\\s*\\n\\+-+\\+", "").trim(),expectedStatus, "Expecting ONLY '"+expectedStatus+"' to be reported when not registered.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19969", "RHEL7-51008"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="run subscription-manager status when registered without entitlements",
			groups={"Tier1Tests"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testStatusWhileRegisteredWithoutEntitlements() {
		int numberOfInstalledProducts = clienttasks.getCurrentProductCertFiles().size();
		SSHCommandResult statusResult;
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		statusResult = clienttasks.status(null, null, null, null, null);
		
		//	[root@jsefler-5 ~]# subscription-manager status
		//	+-------------------------------------------+
		//	   System Status Details
		//	+-------------------------------------------+
		//	Overall Status: Invalid
		//
		//	Awesome OS Modifier Bits:
		//	- Not covered by a valid subscription.


		// assert the overall status
		String expectedStatus;
 		if (numberOfInstalledProducts==0) {
			expectedStatus = "Overall Status: Current";	// translation for "valid"
		} else {
			expectedStatus = "Overall Status: Invalid";	// translation for "invalid"
		}
		Assert.assertTrue(statusResult.getStdout().contains(expectedStatus), "Expecting '"+expectedStatus+"' when registered without entitlements and '"+numberOfInstalledProducts+"' installed products.");

		// assert the individual installed product status details
		for (InstalledProduct installedProduct : installedProducts) {
			for (String statusDetail : installedProduct.statusDetails) {
				Assert.assertTrue(!getSubstringMatches(statusResult.getStdout(), "^"+installedProduct.productName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")+":(\\n- .*)*?\\n- "+statusDetail.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")).isEmpty(),
						"Expecting the status detail '"+statusDetail+"' of installed product '"+installedProduct.productName+"' to appear in the list of overall status details.");
				Assert.assertTrue(!doesStringContainMatches(statusResult.getStdout(), statusDetail.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")+"(\\n- .*)*?\\n- "+statusDetail.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")),
						"Status detail '"+statusDetail+"' of installed product '"+installedProduct.productName+"' should not appear in duplicate.");
			}
		}
		if (installedProducts.isEmpty()) {
			Assert.assertTrue(statusResult.getStdout().trim().endsWith(expectedStatus), "There should be no report of installed product details when there are no installed products; only expected '"+expectedStatus+"'.");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19968", "RHEL7-51007"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="run subscription-manager status when registered with entitlements",
			groups={"Tier1Tests", "blockedByBug-958827","StatusWhileRegisteredWithEntitlements_Test"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testStatusWhileRegisteredWithEntitlements() throws JSONException, Exception {
		SSHCommandResult statusResult;
		// override the system facts setting the attribute count to a value for which all the stackable subscriptions are needed to achieve compliance
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("memory.memtotal", "75");
		factsMap.put("cpu.cpu_socket(s)", "100");
		factsMap.put("cpu.core(s)_per_socket", "2");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		String systemEntitlementsValid = clienttasks.getFactValue("system.entitlements_valid");
		statusResult = clienttasks.status(null, null, null, null, null);
		
		//	[root@jsefler-5 ~]# subscription-manager status
		//	+-------------------------------------------+
		//	   System Status Details
		//	+-------------------------------------------+
		//	Overall Status: Invalid
		//
		//	Large File Support Bits:
		//	- Not covered by a valid subscription.
		//
		//	Awesome OS for x86 Bits:
		//	- Not covered by a valid subscription.
		//
		//	Stackable Cores Package (8 cores)/Stackable Cores Package (8 cores):
		//	- Only covers 16 of 200 cores.
		//
		//	Awesome OS for S390X Bits:
		//	- Not covered by a valid subscription.
		//
		//	Awesome OS for S390 Bits:
		//	- Not covered by a valid subscription.
		//
		//	Load Balancing Bits:
		//	- Not covered by a valid subscription.
		//
		//	Awesome OS Workstation Bits:
		//	- Not covered by a valid subscription.
		//
		//	Clustering Bits:
		//	- Not covered by a valid subscription.
		//
		//	Awesome OS for i386 Bits:
		//	- Not covered by a valid subscription.
		//
		//	Stackable with Awesome OS for x86_64/Awesome OS for x86_64/Stackable with Awesome OS for x86_64/Awesome OS for x86_64:
		//	- Only covers 4 of 100 sockets.
		//
		//	Multi-Attribute (non-stackable) (6 cores, 8GB):
		//	- Only covers 6 of 200 cores.
		//
		//	Awesome OS for ia64 Bits:
		//	- Not covered by a valid subscription.
		//
		//	Awesome OS Developer Basic:
		//	- Not covered by a valid subscription.
		//
		//	Multi-Attribute (multi-entitlement only) (8 cores, 4GB):
		//	- Only covers 8 of 200 cores.
		//
		//	Cores Package (26 cores):
		//	- Only covers 26 of 200 cores.
		//
		//	Multiplier Product Bits:
		//	- Not covered by a valid subscription.
		//
		//	RAM/Cores Package (8GB, 4 cores):
		//	- Only covers 4 of 200 cores.
		//
		//	Shared Storage Bits:
		//	- Not covered by a valid subscription.
		//
		//	Awesome OS for ppc64 Bits:
		//	- Not covered by a valid subscription.
		//
		//	Awesome OS for i686 Bits:
		//	- Not covered by a valid subscription.
		//
		//	Awesome OS Premium Architecture Bits:
		//	- Not covered by a valid subscription.
		//
		//	Multi-Attribute Stackable (4 cores)/Multi-Attribute Stackable (2 GB, 2 Cores)/Multi-Attribute Stackable (4 cores)/Multi-Attribute Stackable (2 GB, 2 Cores)/Multi-Attribute Stackable (16
		//	cores, 4 sockets, 8GB RAM)/Multi-Attribute Stackable (2 sockets)/Multi-Attribute Stackable (2 GB)/Multi-Attribute Stackable (2 sockets)/Multi-Attribute Stackable (16 cores, 4 sockets, 8GB
		//	RAM)/Multi-Attribute Stackable (2 GB):
		//	- Only covers 44 of 200 cores.
		//	- Only covers 12 of 100 sockets.
		//
		//	Management Bits:
		//	- Not covered by a valid subscription.
		//
		//	Virt Only Awesome OS for i386 Bits:
		//	- Not covered by a valid subscription.
		//
		//	Awesome OS Developer Bits:
		//	- Not covered by a valid subscription.
		//
		//	Awesome OS for x86_64/i686/ia64/ppc/ppc64/s390x/s390 Bits:
		//	- Not covered by a valid subscription.
		
		//	Awesome OS Server Bits:
		//	- Guest has not been reported on any host and is using a temporary unmapped guest subscription.
		
		// assert the overall status
		String expectedStatus = null;
 		if (systemEntitlementsValid.equals("valid")) {
			expectedStatus = "Overall Status: Current";	// translation for "valid"
		} else if (systemEntitlementsValid.equals("invalid")){
			expectedStatus = "Overall Status: Invalid";	// translation for "invalid"
		} else if (systemEntitlementsValid.equals("partial")){
			expectedStatus = "Overall Status: Insufficient";	// translation for "partial"	// Bug 959124 - "Compliant status" of the system set to "Insufficient" if any partial subscription is attached to a product (which is not exsiting )
		} else if (systemEntitlementsValid.equals("unknown")){
			expectedStatus = "Overall Status: Unknown";	// translation for "unknown"
		} else {
			Assert.fail("Encountered an unexpected value for systemEntitlementsValid '"+systemEntitlementsValid+"'.");
		}
		Assert.assertTrue(statusResult.getStdout().contains(expectedStatus), "Expecting '"+expectedStatus+"'.");
		// assert the exit code of 1 or 0 based on a "valid" compliance
//		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {		// post commit 7957b8df95c575e6e8713c2f1a0f8f754e32aed3 bug 1119688
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.10-1")) {	// post commit 13fe8ffd8f876d27079b961fb6675424e65b9a10 bug 1119688
			// exit code of 0 indicates valid compliance, otherwise exit code is 1
			if (systemEntitlementsValid.equals("valid")) {
				Assert.assertEquals(statusResult.getExitCode(), new Integer(0), "When the system's overall status is valid, an exit code of 0 should be returned.");
			} else {
				Assert.assertEquals(statusResult.getExitCode(), new Integer(1), "When the system's overall status is NOT valid, an exit code of 1 should be returned.");				
			}
		}
		
		// assert the individual installed product status details
		for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()) {
			
			// status details from the individual installed products is only included in the status report when the product is Not Subscribed
			if (installedProduct.status.equals("Not Subscribed")) {
				if (installedProduct.statusDetails.isEmpty()) log.warning("Status Details appears empty.  Is your candlepin server older than 0.8.6?");	// expectedDetails "Not covered by a valid subscription."
				Assert.assertTrue(!getSubstringMatches(statusResult.getStdout(), "^"+installedProduct.productName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")+":").isEmpty(),
						"Installed product '"+installedProduct.productName+"' should be included in the overall status details report when its own status is Not Subscribed.");
				for (String statusDetail : installedProduct.statusDetails) {
					Assert.assertTrue(!getSubstringMatches(statusResult.getStdout(), "^"+installedProduct.productName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")+":(\\n- .*)*?\\n- "+statusDetail.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")).isEmpty(),
							"Expecting the status detail '"+statusDetail+"' of installed product '"+installedProduct.productName+"' to appear in the list of overall status details.");
					//Assert.assertTrue(!doesStringContainMatches(listStatusResult.getStdout(), "(\\n^- "+statusDetail+"){2,}"),
					//		"Status detail '"+statusDetail+"' of installed product '"+installedProduct.productName+"' should NOT appear in duplicate.");
					Assert.assertTrue(!doesStringContainMatches(statusResult.getStdout(), statusDetail.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")+"(\\n- .*)*?\\n- "+statusDetail.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")),
							"Status detail '"+statusDetail+"' of installed product '"+installedProduct.productName+"' should not appear in duplicate.");
				}
			} else {
				
				// TEMPORARY WORKAROUND FOR BUG:
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1197897";	// Bug 1197897 - subscription-manager status is yellow due to 24-hour subscription despite redundant coverage from a green subscription
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen  && !getSubstringMatches(statusResult.getStdout(), "^"+installedProduct.productName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")+":").isEmpty()) {
					String expectedReason = "Guest has not been reported on any host and is using a temporary unmapped guest subscription.";
					log.warning("Verifying that the reason product '"+installedProduct.productName+"' appears in the status report is because a temporary 24 hour subscription has been attached since '"+expectedReason+"'.");
					Assert.assertTrue(!getSubstringMatches(statusResult.getStdout(), "^"+installedProduct.productName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")+":"+"\n- "+expectedReason).isEmpty(),
						"Installed product '"+installedProduct.productName+"' appears to be covered by a temporary 24 hours entitlement because the status module reports '"+expectedReason+"'");
				} else	// assert
				// END OF WORKAROUND
				
				Assert.assertTrue(getSubstringMatches(statusResult.getStdout(), "^"+installedProduct.productName.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")+":").isEmpty(),
						"Installed product '"+installedProduct.productName+"' should NOT be included in the overall status details report when its own status '"+installedProduct.status+"' is something other than Not Subscribed.");
			}
		}
		
		// assert the individual consumed subscription status details 
		List <ProductSubscription> currentlyConsumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		for (ProductSubscription productSubscription : currentlyConsumedProductSubscriptions) {
			
			// assert the list status output
//			if (productSubscription.statusDetails.isEmpty()) {	// is not sufficient after bug 1180400 implementation
			if (productSubscription.statusDetails.isEmpty() && clienttasks.isPackageVersion("subscription-manager","<", "1.13.13-1")) {
				// since this productSubscription is empty, it should NOT be reported in the Status report 
				Assert.assertTrue(getSubstringMatches(statusResult.getStdout(), "(^|/)"+productSubscription.productName.replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)")+"(|/.+):").isEmpty(),
						"Expecting the empty status details "+productSubscription.statusDetails+" of consumed subscription '"+productSubscription.productName+"' to NOT appear in the list of overall status details of the installed products.");
			} else if (productSubscription.statusDetails.size()==1 && productSubscription.statusDetails.get(0).trim().equals("Subscription is current") && clienttasks.isPackageVersion("subscription-manager",">=", "1.13.13-1")) {	// commit 252ec4520fb6272b00ae379703cd004f558aac63	// bug 1180400: "Status Details" are now populated on CLI
				
				// since this productSubscription appears to be current, it should NOT be reported in the Status report, UNLESS there is another overconsumed subscription by the same name that is not current. 
				boolean allOtherConsumedProductSubscriptionsWithThisProductNameAreCurrent= true;	//assume
				for (ProductSubscription otherConsumedProductSubscription : currentlyConsumedProductSubscriptions) {
					if (otherConsumedProductSubscription.productName.equals(productSubscription.productName) &&
						!otherConsumedProductSubscription.poolId.equals(productSubscription.poolId) &&
						!otherConsumedProductSubscription.statusDetails.isEmpty() &&
						!otherConsumedProductSubscription.statusDetails.get(0).equals("Subscription is current")) {
						allOtherConsumedProductSubscriptionsWithThisProductNameAreCurrent = false;
						log.warning("There are multiple consumed subscriptions for '"+productSubscription.productName+"'.  Not all of them are current.");
					}
				}
				boolean statusReportIncludesProductSubscriptionProductName = getSubstringMatches(statusResult.getStdout(), "(^|/)"+productSubscription.productName.replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)")+"(|/.+):").isEmpty();
				if (allOtherConsumedProductSubscriptionsWithThisProductNameAreCurrent) {
					Assert.assertTrue(statusReportIncludesProductSubscriptionProductName,
							"Since the status details of consumed subscription '"+productSubscription.productName+"' states Subscription is current, product '"+productSubscription.productName+"' should NOT appear in the list of overall status details of the installed products.");
				} // else the actual status report will be asserted when the outer for loop hits the otherConsumedProductSubscriptionsWithThisProductName
			} else {
				// since this productSubscription is not current, its status details should be reported in the Status report under the subscription's name. 
				for (String statusDetail : productSubscription.statusDetails) {
					
					// TEMPORARY WORKAROUND FOR BUG
					if (clienttasks.redhatReleaseX.equals("8") && statusDetail.equals("Subscription has not begun")) {
						String bugId = "1580996";	// Bug 1580996 - RHEL8 subscription-manager list --consumed shows Status Details: "Subscription has not begun" expected "Subscription is current"
						boolean invokeWorkaroundWhileBugIsOpen = true;
						try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
						if (invokeWorkaroundWhileBugIsOpen) {
							log.warning("Skipping the assert that status detail '"+statusDetail+"' from consumed subscription '"+productSubscription.productName+"' is included in the list of overall status details while bug '"+bugId+"' is open.");
							continue;
						}
					}
					// END OF WORKAROUND
					
					Assert.assertTrue(!getSubstringMatches(statusResult.getStdout(), "(^|/)"+productSubscription.productName.replaceAll("\\(","\\\\(").replaceAll("\\)","\\\\)")+"(|/.+):(\\n- .*)*?\\n- "+statusDetail.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")).isEmpty(),
							"Expecting the status detail '"+statusDetail+"' of consumed subscription '"+productSubscription.productName+"' to appear in the list of overall status details.");
					//Assert.assertTrue(!doesStringContainMatches(listStatusResult.getStdout(), "(\\n^- "+statusDetail+"){2,}"),
					//		"Status detail '"+statusDetail+"' of consumed subscription '"+productSubscription+"' should NOT appear in duplicate.");
					Assert.assertTrue(!doesStringContainMatches(statusResult.getStdout(), statusDetail.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")+"(\\n- .*)*?\\n- "+statusDetail.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)")),
							"Status detail '"+statusDetail+"' of consumed subscription '"+productSubscription.productName+"' should not appear in duplicate.");
				}
			}
		}
	}
	@AfterGroups(groups={"setup"},value="StatusWhileRegisteredWithEntitlements_Test", alwaysRun=true)
	public void afterAttemptListStatusWhileRegisteredWithEntitlements_Test() {
		if (clienttasks==null) return;
		clienttasks.deleteFactsFileWithOverridingValues();
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19967", "RHEL7-33081"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="run subscription-manager status ondate (tomorrow and a future date after one of today's entitlements expire)",
			groups={"Tier1Tests","blockedByBug-1500106"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testStatusOnFutureDate() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.9.2-1")) throw new SkipException("Installed package '"+clienttasks.installedPackageVersionMap.get("subscription-manager")+"' does not support status --ondate option.  It was introduced in subscription-manager-1.9.2-1.");
		
		// register with autosubscribe to establish today's status
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		
		// make sure at least one installed product was subscribed, otherwise this test is not applicable
		List <InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		boolean noInstalledProducts = installedProducts.isEmpty();
		boolean areAnyInstalledProductsSubscribable = false;
		for (InstalledProduct installedProduct : installedProducts) {
			if (installedProduct.status.equals("Subscribed") || installedProduct.status.equals("Partially Subscribed")) {
				areAnyInstalledProductsSubscribable = true; break;
			}
		}
		// get today's status
		SSHCommandResult statusResultToday = clienttasks.status(null,null,null, null, null);
		Map<String,String> statusMapToday = getProductStatusMapFromStatusResult(statusResultToday);
		
		// get tomorrow's status
		DateFormat yyyy_MM_dd_DateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar tomorrow = new GregorianCalendar(); tomorrow.add(Calendar.HOUR, 24);
		String onDateTomorrow = yyyy_MM_dd_DateFormat.format(tomorrow.getTime());
		SSHCommandResult statusResultTomorrow = clienttasks.status(onDateTomorrow,null,null, null, null);
		Map<String,String> statusMapTomorrow = getProductStatusMapFromStatusResult(statusResultTomorrow);
		
		// assert tomorrow's status is identical to today's (assumes no change in coverage)
		Assert.assertTrue(statusMapTomorrow.equals(statusMapToday), "Asserting the assumption that the status --ondate=tomorrow will be identical to the status ondate=today (default).");
		
		// now let's find the most future endDate of all current entitlement and assert that the status on the following day is different and Invalid
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		if (consumedProductSubscriptions.isEmpty()) throw new SkipException("The remainder of this test cannot be executed because we expect this system to have at least one consumed entitlement.");
		Calendar future = consumedProductSubscriptions.get(0).startDate;
		for (ProductSubscription productSubscription : consumedProductSubscriptions) if (productSubscription.endDate.after(future)) future = productSubscription.endDate;
		future.add(Calendar.HOUR, 24);	// add one day
		String onDateFuture = yyyy_MM_dd_DateFormat.format(future.getTime());
		SSHCommandResult statusResultFuture = clienttasks.status(onDateFuture,null,null, null, null);
		Map<String,String> statusMapFuture = getProductStatusMapFromStatusResult(statusResultFuture);
		
		// assert future status is NOT identical to today's (assumes entitlements have expired)
		log.info("Status Map ondate=today: "+statusMapToday);
		log.info("Status Map ondate=future: "+statusMapFuture);
		if (areAnyInstalledProductsSubscribable || noInstalledProducts) {
			Assert.assertTrue(!statusMapFuture.equals(statusMapToday),"Asserting that the status --ondate=future (the day after an entitlement expires) will NOT be identical to the status ondate=today (default).");
		} else {
			Assert.assertTrue(statusMapFuture.equals(statusMapToday),"Asserting that the status --ondate=future (the day after an entitlement expires) will be identical to the status ondate=today (default) because none of the subscriptions applied today provide coverage for the products installed today.");			
		}
		
		// assert future status
		if (noInstalledProducts) {
			String expectedStatus = "Overall Status: Current";
			Assert.assertTrue(statusResultFuture.getStdout().contains(expectedStatus), "Expecting '"+expectedStatus+"' onDate '"+onDateFuture+"' which is one day beyond the most future endDate of the currently consumed subscriptions - when there are no installed products.");
		} else {
			String expectedStatus = "Overall Status: Invalid";
			Assert.assertTrue(statusResultFuture.getStdout().contains(expectedStatus), "Expecting '"+expectedStatus+"' onDate '"+onDateFuture+"' which is one day beyond the most future endDate of the currently consumed subscriptions.");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36568", "RHEL7-51356"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="run subscription-manager status ondate (yesterday)",
			groups={"Tier2Tests","blockedByBug-1092594"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testStatusOnPastDate() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.9.2-1")) throw new SkipException("Installed package '"+clienttasks.installedPackageVersionMap.get("subscription-manager")+"' does not support status --ondate option.  It was introduced in subscription-manager-1.9.2-1.");
		
		// get yeterday's status
		DateFormat yyyy_MM_dd_DateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar yesterday = new GregorianCalendar(); yesterday.add(Calendar.HOUR, -24);
		String onDateYesterday = yyyy_MM_dd_DateFormat.format(yesterday.getTime());
		SSHCommandResult statusResultYesterday = clienttasks.status_(onDateYesterday,null,null, null, null);
		
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(statusResultYesterday.getStderr().trim(), "Past dates are not allowed", "Stderr from call to status ondate yesterday.");
			Assert.assertEquals(statusResultYesterday.getStdout().trim(), "", "Stdout from call to status ondate yesterday.");
			Assert.assertEquals(statusResultYesterday.getExitCode(), Integer.valueOf(64)/*EX_USAGE*/, "ExitCode from call to status ondate yesterday.");
		} else {
			Assert.assertEquals(statusResultYesterday.getStdout().trim(), "Past dates are not allowed", "Stdout from call to status ondate yesterday.");
			Assert.assertEquals(statusResultYesterday.getStderr().trim(), "", "Stderr from call to status ondate yesterday.");
			Assert.assertEquals(statusResultYesterday.getExitCode(), Integer.valueOf(1), "ExitCode from call to status ondate yesterday.");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36567", "RHEL7-51355"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="run subscription-manager status ondate (invalid)",
			groups={"Tier2Tests"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testStatusOnInvalidDate() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.9.2-1")) throw new SkipException("Installed package '"+clienttasks.installedPackageVersionMap.get("subscription-manager")+"' does not support status --ondate option.  It was introduced in subscription-manager-1.9.2-1.");
		
		// call status with an invalid ondate
		SSHCommandResult statusResultYesterday = clienttasks.status_("2000-13-01",null,null, null, null);	// lucky month 13
		
		DateFormat yyyy_MM_dd_DateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String today = yyyy_MM_dd_DateFormat.format(Calendar.getInstance().getTime());
		
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(statusResultYesterday.getStderr().trim(), String.format("Date entered is invalid. Date should be in YYYY-MM-DD format (example: %s )",today), "Stderr from call to status ondate invalid.");
			Assert.assertEquals(statusResultYesterday.getStdout().trim(), "", "Stdout from call to status ondate invalid.");
			Assert.assertEquals(statusResultYesterday.getExitCode(), Integer.valueOf(65)/*EX_DATAERR*/, "ExitCode from call to status ondate invalid.");
		} else {
			Assert.assertEquals(statusResultYesterday.getStdout().trim(), String.format("Date entered is invalid. Date should be in YYYY-MM-DD format (example: %s )",today), "Stdout from call to status ondate invalid.");
			Assert.assertEquals(statusResultYesterday.getStderr().trim(), "", "Stderr from call to status ondate invalid.");
			Assert.assertEquals(statusResultYesterday.getExitCode(), Integer.valueOf(1), "ExitCode from call to status ondate invalid.");
		}
	}
	
	// Candidates for an automated Test:
	
	
	
	
	// Configuration methods ***********************************************************************
	
	
	
	// Protected methods ***********************************************************************
	public static Map<String,String> getProductStatusMapFromStatusResult(SSHCommandResult statusResult) {
		
		//	FINE: ssh root@jsefler-7.usersys.redhat.com subscription-manager status --ondate=2014-11-04
		//	FINE: Stdout: 
		//	+-------------------------------------------+
		//	   System Status Details
		//	+-------------------------------------------+
		//	Overall Status: Invalid
		//
		//	Awesome OS Modifier Bits:
		//	- Not covered by a valid subscription.
		//
		//	Awesome OS Server Bits:
		//	- Not covered by a valid subscription.
		//
		//	Multi-Attribute Stackable (4 cores)/Multi-Attribute Stackable (2 GB, 2 Cores)/Multi-Attribute Stackable (4 cores):
		//	- Only covers 44 of 200 cores.
		//	- Only covers 12 of 100 sockets.
		//
		//	Virt Only Awesome OS for i386 Bits:
		//	- Not covered by a valid subscription.
		//
		//	FINE: Stderr: 
		//	FINE: ExitCode: 0
		Map<String,String> statusMap = new HashMap<String, String>();
		
		for (String productStatus : getSubstringMatches(statusResult.getStdout(), "(.*):(\n- (.*))+")) {
			String product = productStatus.split(":\n- ")[0];
			String status = productStatus.split(":\n- ")[1];	// TODO could be multiple lines
			statusMap.put(product, status);
		}
		return statusMap;
	}
	
	
	// Data Providers ***********************************************************************
	
}
