package rhsm.cli.tests;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.jul.TestRecords;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.EntitlementCert;
import rhsm.data.ProductNamespace;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"InteroperabilityTests"})
public class InteroperabilityTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19954", "RHEL7-51003"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Verify user is warned when attempting to register using subscription-manager while already registered using RHN Classic",
			groups={"Tier1Tests", "InteroperabilityRegister_Test", "blockedByBug-730018", "blockedByBug-755130", "blockedByBug-847795", "blockedByBug-859090", "blockedByBug-877590"},
			enabled=true)
	@ImplementsNitrateTest(caseId=75972)	
	public void testInteroperableRegistrationUsingRhsmAndRhnClassic() {
		
		// ensure we begin in an unregistered rhsm state
		clienttasks.unregister_(null, null, null, null);
		
		// interoperabilityWarningMessage is defined in /usr/share/rhsm/subscription_manager/branding/__init__.py self.REGISTERED_TO_OTHER_WARNING
		String interoperabilityWarningMessage = clienttasks.msg_InteroperabilityWarning;
		
		// query the branding python file directly to get the default interoperabilityWarningMessage (when the subscription-manager rpm came from a git build - this assumes that any build of subscription-manager must have a branding module e.g. redhat_branding.py)
		/* TEMPORARILY COMMENTING OUT SINCE JBOWES IS INCLUDING THIS BRANDING FILE IN THE PUBLIC REPO - jsefler 9/15/2011
		if (client.runCommandAndWait("rpm -q subscription-manager").getStdout().contains(".git.")) {
			interoperabilityWarningMessage = clienttasks.getBrandingString("REGISTERED_TO_OTHER_WARNING");
		}
		*/
		String interoperabilityWarningMessageRegex = "^"+interoperabilityWarningMessage.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)").replaceAll("\\.", "\\\\.");
		Assert.assertTrue(interoperabilityWarningMessage.startsWith("WARNING"), "The expected interoperability message starts with \"WARNING\".");
		
		SSHCommandResult result;
		if (!isRhnClientToolsInstalled) {
			log.warning("Skipping some RHN Classic interoperability test assertions when the '"+rhnClientTools+"' package is not installed.");
		} else {
			
			log.info("Simulating registration to RHN Classic by creating an empty systemid file '"+clienttasks.rhnSystemIdFile+"'...");
			RemoteFileTasks.runCommandAndWait(client, "touch "+clienttasks.rhnSystemIdFile, TestRecords.action());
			Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.rhnSystemIdFile), "RHN Classic systemid file '"+clienttasks.rhnSystemIdFile+"' is in place.");
			
			log.info("Attempt to register while already registered via RHN Classic...");
			result = clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
			Assert.assertTrue(result.getStdout().startsWith(interoperabilityWarningMessage), "subscription-manager warns the registerer when the system is already registered via RHN Classic with this expected message:\n"+interoperabilityWarningMessage+"\n");
			//Assert.assertContainsMatch(result.getStdout(),interoperabilityWarningMessageRegex, "subscription-manager warns the registerer when the system is already registered via RHN Classic with the expected message.");
			//Assert.assertTrue(result.getStdout().contains(interoperabilityWarningMessage), "subscription-manager warns the registerer when the system is already registered via RHN Classic with this expected message:\n"+interoperabilityWarningMessage+"\n");
		}
		
		log.info("Now let's make sure we are NOT warned when we are NOT already registered via RHN Classic...");
		clienttasks.removeRhnSystemIdFile();
		Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.rhnSystemIdFile), "RHN Classic systemid file '"+clienttasks.rhnSystemIdFile+"' is gone.");
		result = clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		
		//Assert.assertFalse(result.getStdout().startsWith(interoperabilityWarningMessage), "subscription-manager does NOT warn registerer when the system is not already registered via RHN Classic.");
		//Assert.assertContainsNoMatch(result.getStdout(),interoperabilityWarningMessageRegex, "subscription-manager does NOT warn registerer when the system is NOT already registered via RHN Classic.");
		Assert.assertTrue(!result.getStdout().contains(interoperabilityWarningMessage), "subscription-manager does NOT warn registerer when the system is NOT already registered via RHN Classic.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36531", "RHEL7-51304"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="When registered to RHSM (and all subscriptions have expired), the subscription-manager yum plugin should inform that: The subscription for following product(s) has expired: etc.",
			groups={"Tier2Tests","YumPluginMessageCase_Tests","blockedByBug-818383","blockedByBug-832119","blockedByBug-871146", "blockedbyBug-901612", "blockedbyBug-1017354","blockedByBug-1087620","blockedByBug-1058380","blockedByBug-1122772"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testYumPluginMessageCase0() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true,false,null,null, null, null);
		
		// import an expired certificate 
		File expiredCertFile = new File(System.getProperty("automation.dir", null)+"/certs/Expiredcert.pem");
		RemoteFileTasks.putFile(client.getConnection(), expiredCertFile.getPath(), "/tmp/Expiredcert.pem", "0644");
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.17.10-1")) {	//	subscription-manager commit b93e59482563b9e3e972a928233bef7ebf885ea1	// Bug 1251516: Disable import when registered
			clienttasks.importCertificate("/tmp/Expiredcert.pem");
		} else {
			// after fix for bug 1251516, I can no longer use importCertificate while registered; instead we'll simply copy the expired cert to /etc/pki/entitlement ...
			client.runCommandAndWait("cp /tmp/Expiredcert.pem "+clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "entitlementCertDir"));
			// ... to avoid this usability error:
			//	201608261038:58.987 - FINE: ssh root@jsefler-rhel7.usersys.redhat.com subscription-manager import --certificate=/tmp/Expiredcert.pem
			//	201608261038:59.747 - FINE: Stdout: 
			//	201608261038:59.747 - FINE: Stderr: Error: You may not import certificates into a system that is registered to a subscription management service.
		}
		EntitlementCert expiredEntitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(new File("/tmp/Expiredcert.pem"));
		
		// assert the interoperable registration message (without any current subscriptions)
		SSHCommandResult result = client.runCommandAndWait("yum repolist --disableplugin=rhnplugin --enableplugin=subscription-manager");
		String expectedMsgRHSM = "This system is registered to Red Hat Subscription Management, but is not receiving updates. You can use subscription-manager to assign subscriptions.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.18.4-1")) expectedMsgRHSM = "This system is registered with an entitlement server, but is not receiving updates. You can use subscription-manager to assign subscriptions.";	// subscription-manager commit 5e4b42e1c99472085a44118dd231e7ddd161937a	// https://github.com/candlepin/subscription-manager/pull/1512
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.12.2-1")) Assert.assertTrue(result.getStdout().contains(expectedMsgRHSM), "When registered to RHSM (and all subscriptions have expired), the subscription-manager yum plugin stdout should inform that:\n"+expectedMsgRHSM+"\n");	// Bug 901612 - Subscription-manager-s yum plugin prints warning to stdout instead of stderr.	// Bug 901612 was reverted by Bug 1017354 
		else if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.12.11-1")) Assert.assertTrue(result.getStdout().contains(expectedMsgRHSM), "When registered to RHSM (and all subscriptions have expired), the subscription-manager yum plugin stdout should inform that:\n"+expectedMsgRHSM+"\n");	// Bug 1058380 was reverted by Bug 1122772 
		else Assert.assertTrue(!(result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When registered to RHSM (and all subscriptions have expired), the subscription-manager yum plugin stdout should NO LONGER inform that:\n"+expectedMsgRHSM+"\nBugzilla https://bugzilla.redhat.com/show_bug.cgi?id=1058380 was used to remove this usability messaging implemented for https://bugzilla.redhat.com/show_bug.cgi?id=818383");
		// assert the expired subscriptions message
		expectedMsgRHSM = "*** WARNING ***\nThe subscription for following product(s) has expired:";
		for (ProductNamespace productNamespace : expiredEntitlementCert.productNamespaces) expectedMsgRHSM += "\n"+"  - "+productNamespace.name;
		expectedMsgRHSM += "\n"+"You no longer have access to the repositories that provide these products.  It is important that you apply an active subscription in order to resume access to security and other critical updates. If you don't have other active subscriptions, you can renew the expired subscription.";
		Assert.assertTrue(result.getStdout().contains(expectedMsgRHSM), "When registered to RHSM (and all subscriptions have expired), the subscription-manager yum plugin stdout should inform that:\n"+expectedMsgRHSM+"\n");	// Bug 901612 - Subscription-manager-s yum plugin prints warning to stdout instead of stderr.	// Bug 901612 was reverted by Bug 1017354 

		// assert the interoperable registration message (with current subscriptions)
		//clienttasks.subscribeToSubscriptionPool(clienttasks.getCurrentlyAvailableSubscriptionPools().get(0));	// will fail with java.lang.AssertionError: The list of consumed products is entitled 'Consumed Subscriptions'. expected:<true> but was:<false>
		clienttasks.subscribe(null, null, clienttasks.getCurrentlyAvailableSubscriptionPools().get(0).poolId, null, null, null, null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.17.10-1")) {	//	subscription-manager commit b93e59482563b9e3e972a928233bef7ebf885ea1	// Bug 1251516: Disable import when registered
			clienttasks.importCertificate("/tmp/Expiredcert.pem");
		} else {
			// after fix for bug 1251516, I can no longer use importCertificate while registered; instead we'll simply copy the expired cert to /etc/pki/entitlement ...
			client.runCommandAndWait("cp /tmp/Expiredcert.pem "+clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "entitlementCertDir"));
			// ... to avoid this usability error:
			//	201608261038:58.987 - FINE: ssh root@jsefler-rhel7.usersys.redhat.com subscription-manager import --certificate=/tmp/Expiredcert.pem
			//	201608261038:59.747 - FINE: Stdout: 
			//	201608261038:59.747 - FINE: Stderr: Error: You may not import certificates into a system that is registered to a subscription management service.
		}
		result = client.runCommandAndWait("yum repolist --disableplugin=rhnplugin --enableplugin=subscription-manager");
		expectedMsgRHSM = "This system is receiving updates from Red Hat Subscription Management.";
		//NOT TRUE ANYMORE Assert.assertTrue(result.getStdout().contains(expectedMsgRHSM), "When registered to RHSM (and some subscriptions have expired), the subscription-manager yum plugin stdout should inform that:\n"+expectedMsgRHSM+"\n");	// Bug 901612 - Subscription-manager-s yum plugin prints warning to stdout instead of stderr.	// Bug 901612 was reverted by Bug 1017354 
		Assert.assertTrue(!(result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When registered to RHSM, the subscription-manager yum plugin NO LONGER informs that:\n"+expectedMsgRHSM+"\nFor justification, see https://bugzilla.redhat.com/show_bug.cgi?id=1017354#c12");	// commit 39eadae14eead4bb79978e52d38da2b3e85cba57 1017354: remove msg printed to stderr via yum

		// assert the expired subscriptions message again
		expectedMsgRHSM = "*** WARNING ***\nThe subscription for following product(s) has expired:";
		for (ProductNamespace productNamespace : expiredEntitlementCert.productNamespaces) expectedMsgRHSM += "\n"+"  - "+productNamespace.name;
		expectedMsgRHSM += "\n"+"You no longer have access to the repositories that provide these products.  It is important that you apply an active subscription in order to resume access to security and other critical updates. If you don't have other active subscriptions, you can renew the expired subscription.";
		Assert.assertTrue(result.getStdout().contains(expectedMsgRHSM), "When registered to RHSM (and some subscriptions have expired), the subscription-manager yum plugin stdout should inform that:\n"+expectedMsgRHSM+"\n");	// Bug 901612 - Subscription-manager-s yum plugin prints warning to stdout instead of stderr.	// Bug 901612 was reverted by Bug 1017354 
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36532", "RHEL7-51305"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="When not registered to either RHN nor RHSM, the subscription-manager yum plugin should inform that: This system is not registered to Red Hat Subscription Management. You can use subscription-manager to register.",
			groups={"Tier2Tests","YumPluginMessageCase_Tests","blockedByBug-818383","blockedByBug-832119","blockedByBug-830193","blockedByBug-830194","blockedByBug-906875","blockedByBug-1058380","blockedByBug-1122772"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testYumPluginMessageCase1() {
		clienttasks.unregister(null,null,null, null);
		clienttasks.removeRhnSystemIdFile();
		SSHCommandResult result = client.runCommandAndWait("yum repolist --enableplugin=rhnplugin --enableplugin=subscription-manager");
		String expectedMsgRHSM = "This system is not registered to Red Hat Subscription Management. You can use subscription-manager to register.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.18.4-1")) expectedMsgRHSM = "This system is not registered with an entitlement server. You can use subscription-manager to register.";	// subscription-manager commit 5e4b42e1c99472085a44118dd231e7ddd161937a	// https://github.com/candlepin/subscription-manager/pull/1512
		String expectedMsgRHN; // comes from /usr/share/yum-plugins/rhnplugin.py (package yum-rhn-plugin)
		expectedMsgRHN = "This system is not registered with RHN Classic or RHN Satellite.\nYou can use rhn_register to register.\nRHN Satellite or RHN Classic support will be disabled.";	// yum-rhn-plugin-0.5.4.1-7.el5		// yum-rhn-plugin-0.9.1-48.el6
		// [root@jsefler-7 ~]# rpm -q --changelog yum-rhn-plugin | more
		// * Wed Jun 12 2013 Tomas Kasparek <tkasparek@redhat.com> 1.10.3-1
		// - rebranding RHN Proxy to Red Hat Proxy in client stuff
		// - rebranding RHN Satellite to Red Hat Satellite in client stuff
		if (clienttasks.isPackageVersion("yum-rhn-plugin", ">=", "2.0")) expectedMsgRHN = "This system is not registered with RHN Classic or Red Hat Satellite.\nYou can use rhn_register to register.\nRed Hat Satellite or RHN Classic support will be disabled.";	// yum-rhn-plugin-2.0.1-4.el7
//FIXME NOT CONVINCED THIS IF IS CORRECT		if (clienttasks.isPackageVersion("yum-rhn-plugin", ">=", "2.0.1-5")) expectedMsgRHN = "This system is not registered with RHN Classic or Red Hat Satellite. SystemId could not be acquired.\nYou can use rhn_register to register.\nRed Hat Satellite or RHN Classic support will be disabled.";	// yum-rhn-plugin-2.0.1-5.el7
		if (Arrays.asList(new String[]{"6.3","5.8","6.2","5.7","6.1"}).contains(clienttasks.redhatReleaseXY)) expectedMsgRHN = "This system is not registered with RHN."+"\n"+"RHN Satellite or RHN Classic support will be disabled.";	
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.12.2-1")) Assert.assertTrue((result.getStdout()/*+result.getStderr()*/).contains(expectedMsgRHSM), "When not registered to either RHN nor RHSM, the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
		else if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.12.11-1")) Assert.assertTrue((result.getStdout()/*+result.getStderr()*/).contains(expectedMsgRHSM), "When not registered to either RHN nor RHSM, the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");  // Bug 1122772 was used to revert Bug 1058380
		else Assert.assertTrue(!(result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When not registered to either RHN nor RHSM, the subscription-manager yum plugin should NO LONGER inform that:\n"+expectedMsgRHSM+"\nBugzilla https://bugzilla.redhat.com/show_bug.cgi?id=1058380 was used to remove this usability messaging implemented for https://bugzilla.redhat.com/show_bug.cgi?id=818383");
		if (isRhnClientToolsInstalled) Assert.assertTrue((/*result.getStdout()+*/result.getStderr()).contains(expectedMsgRHN), "When not registered to either RHN nor RHSM, the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36533", "RHEL7-51306"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="When registered to RHN but not RHSM, the subscription-manager yum plugin should inform that: This system is not registered to Red Hat Subscription Management. You can use subscription-manager to register.",
			groups={"Tier2Tests","YumPluginMessageCase_Tests","blockedByBug-818383","blockedByBug-832119","blockedByBug-830193","blockedByBug-830194","blockedByBug-906875","blockedByBug-924919"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testYumPluginMessageCase2() {
		clienttasks.unregister(null,null,null, null);
		if (!isRhnClientToolsInstalled) throw new SkipException("RHN Classic registration requires package '"+rhnClientTools+"' to be installed.");
		clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		SSHCommandResult result = client.runCommandAndWait("yum repolist --enableplugin=rhnplugin --enableplugin=subscription-manager");
		String expectedMsgRHSM = "This system is not registered to Red Hat Subscription Management. You can use subscription-manager to register.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.18.4-1")) expectedMsgRHSM = "This system is not registered to an entitlement server. You can use subscription-manager to register.";	// subscription-manager commit 5e4b42e1c99472085a44118dd231e7ddd161937a	// https://github.com/candlepin/subscription-manager/pull/1512
		String expectedMsgRHN = null;	// comes from /usr/share/yum-plugins/rhnplugin.py (package yum-rhn-plugin)
		if (Float.valueOf(clienttasks.redhatReleaseXY)>=6.4) expectedMsgRHN = "This system is receiving updates from RHN Classic or RHN Satellite.";
		if (Float.valueOf(clienttasks.redhatReleaseXY)>=7.0) expectedMsgRHN = "This system is receiving updates from RHN Classic or Red Hat Satellite.";
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7 && doesStringContainMatches(sm_rhnHostname, "rhn\\.(.+\\.)*redhat\\.com")) {	// exceptional case when a rhel7 system attempts to register to RHN HOSTED 
			log.warning("With RHEL7 (and beyond), registration to RHN Classic (HOSTED) is no longer supported and therefore no base rhel channel (e.g. rhel-x86_64-server-7) will be available.");
			Assert.assertTrue((result.getStdout()/*+result.getStderr()*/).contains(expectedMsgRHSM), "When registered to RHN but not RHSM, the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
			Assert.assertFalse((result.getStdout()+result.getStderr()).contains(expectedMsgRHN), "When registered to RHN but not RHSM, the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
			Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHN_NoChannels), "On RHEL7... When registered to RHN but not RHSM, the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN_NoChannels+"\n");
			return;
		}
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.12.2-1")) Assert.assertTrue((result.getStdout()/*+result.getStderr()*/).contains(expectedMsgRHSM), "When registered to RHN but not RHSM, the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
		else Assert.assertTrue(!(result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When registered to RHN but not RHSM, the subscription-manager yum plugin should NO LONGER inform that:\n"+expectedMsgRHSM+"\nBugzilla https://bugzilla.redhat.com/show_bug.cgi?id=1058380 was used to remove this usability messaging implemented for https://bugzilla.redhat.com/show_bug.cgi?id=818383");
		if (expectedMsgRHN!=null) Assert.assertTrue((result.getStdout()/*+result.getStderr()*/).contains(expectedMsgRHN), "When registered to RHN but not RHSM, the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36534", "RHEL7-51307"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="When registered to RHSM (but not subscribed) but not RHN, the subscription-manager yum plugin should inform that: This system is registered to Red Hat Subscription Management, but is not receiving updates. You can use subscription-manager to assign subscriptions.",
			groups={"Tier2Tests","YumPluginMessageCase_Tests","blockedByBug-818383","blockedByBug-832119","blockedByBug-830193","blockedByBug-830194","blockedByBug-906875","blockedByBug-1058380","blockedByBug-1122772"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testYumPluginMessageCase3A() {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true,false,null,null, null, null);
		clienttasks.removeRhnSystemIdFile();
		SSHCommandResult result = client.runCommandAndWait("yum repolist --enableplugin=rhnplugin --enableplugin=subscription-manager");
		String expectedMsgRHSM = "This system is registered to Red Hat Subscription Management, but is not receiving updates. You can use subscription-manager to assign subscriptions.";	
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.18.4-1")) expectedMsgRHSM = "This system is registered with an entitlement server, but is not receiving updates. You can use subscription-manager to assign subscriptions.";	// subscription-manager commit 5e4b42e1c99472085a44118dd231e7ddd161937a	// https://github.com/candlepin/subscription-manager/pull/1512
		String expectedMsgRHN;	// comes from /usr/share/yum-plugins/rhnplugin.py (package yum-rhn-plugin)
		expectedMsgRHN = "This system is not registered with RHN Classic or RHN Satellite.\nYou can use rhn_register to register.\nRHN Satellite or RHN Classic support will be disabled.";
		// [root@jsefler-7 ~]# rpm -q --changelog yum-rhn-plugin | more
		// * Wed Jun 12 2013 Tomas Kasparek <tkasparek@redhat.com> 1.10.3-1
		// - rebranding RHN Proxy to Red Hat Proxy in client stuff
		// - rebranding RHN Satellite to Red Hat Satellite in client stuff
		if (clienttasks.isPackageVersion("yum-rhn-plugin", ">=", "2.0")) expectedMsgRHN = "This system is not registered with RHN Classic or Red Hat Satellite.\nYou can use rhn_register to register.\nRed Hat Satellite or RHN Classic support will be disabled.";
		if (Arrays.asList(new String[]{"6.3","5.8","6.2","5.7","6.1"}).contains(clienttasks.redhatReleaseXY)) expectedMsgRHN = "This system is not registered with RHN."+"\n"+"RHN Satellite or RHN Classic support will be disabled.";		
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.12.2-1")) Assert.assertTrue((result.getStdout()/*+result.getStderr()*/).contains(expectedMsgRHSM), "When registered to RHSM (but not subscribed) but not RHN, the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
		else if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.12.11-1")) Assert.assertTrue((result.getStdout()/*+result.getStderr()*/).contains(expectedMsgRHSM), "When registered to RHSM (but not subscribed) but not RHN, the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");  // Bug 1122772 was used to revert Bug 1058380
		else Assert.assertTrue(!(result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When registered to RHSM (but not subscribed) but not RHN, the subscription-manager yum plugin should NO LONGER inform that:\n"+expectedMsgRHSM+"\nBugzilla https://bugzilla.redhat.com/show_bug.cgi?id=1058380 was used to remove this usability messaging implemented for https://bugzilla.redhat.com/show_bug.cgi?id=818383");
		if (isRhnClientToolsInstalled) Assert.assertTrue((/*result.getStdout()+*/result.getStderr()).contains(expectedMsgRHN), "When registered to RHSM (but not subscribed) but not RHN, the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36535", "RHEL7-51308"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="When registered to RHSM (and subscribed) but not RHN, the subscription-manager yum plugin should inform that: This system is receiving updates from Red Hat Subscription Management.",
			groups={"Tier2Tests","YumPluginMessageCase_Tests","blockedByBug-818383","blockedByBug-832119","blockedByBug-830193","blockedByBug-830194","blockedByBug-906875"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testYumPluginMessageCase3B() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true,false,null,null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		clienttasks.removeRhnSystemIdFile();
		SSHCommandResult result = client.runCommandAndWait("yum repolist --enableplugin=rhnplugin --enableplugin=subscription-manager");
		String expectedMsgRHSM = "This system is receiving updates from Red Hat Subscription Management.";
		String expectedMsgRHN;	// comes from /usr/share/yum-plugins/rhnplugin.py (package yum-rhn-plugin)
		expectedMsgRHN = "This system is not registered with RHN Classic or RHN Satellite.\nYou can use rhn_register to register.\nRHN Satellite or RHN Classic support will be disabled.";
		// [root@jsefler-7 ~]# rpm -q --changelog yum-rhn-plugin | more
		// * Wed Jun 12 2013 Tomas Kasparek <tkasparek@redhat.com> 1.10.3-1
		// - rebranding RHN Proxy to Red Hat Proxy in client stuff
		// - rebranding RHN Satellite to Red Hat Satellite in client stuff
		if (clienttasks.isPackageVersion("yum-rhn-plugin", ">=", "2.0")) expectedMsgRHN = "This system is not registered with RHN Classic or Red Hat Satellite.\nYou can use rhn_register to register.\nRed Hat Satellite or RHN Classic support will be disabled.";
		if (Arrays.asList(new String[]{"6.3","5.8","6.2","5.7","6.1"}).contains(clienttasks.redhatReleaseXY)) expectedMsgRHN = "This system is not registered with RHN."+"\n"+"RHN Satellite or RHN Classic support will be disabled.";	
		//NOT TRUE ANYMORE Assert.assertTrue((result.getStdout()/*+result.getStderr()*/).contains(expectedMsgRHSM), "When registered to RHSM (and subscribed) but not RHN, the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
		Assert.assertTrue(!(result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When registered to RHSM (and subscribed) but not RHN, the subscription-manager yum plugin NO LONGER informs that:\n"+expectedMsgRHSM+"\nFor justification, see https://bugzilla.redhat.com/show_bug.cgi?id=1017354#c12");	// commit 39eadae14eead4bb79978e52d38da2b3e85cba57 1017354: remove msg printed to stderr via yum
		if (isRhnClientToolsInstalled) Assert.assertTrue((/*result.getStdout()+*/result.getStderr()).contains(expectedMsgRHN), "When registered to RHSM (and subscribed) but not RHN, the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36536", "RHEL7-51309"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="When registered to both RHN and RHSM (but not subscribed), the subscription-manager yum plugin should inform that: This system is registered to Red Hat Subscription Management, but is not receiving updates. You can use subscription-manager to assign subscriptions.",
			groups={"Tier2Tests","YumPluginMessageCase_Tests","blockedByBug-818383","blockedByBug-832119","blockedByBug-830193","blockedByBug-830194","blockedByBug-871146","blockedByBug-906875","blockedByBug-924919"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testYumPluginMessageCase4A() {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true,false,null,null, null, null);
		if (!isRhnClientToolsInstalled) throw new SkipException("RHN Classic registration requires package '"+rhnClientTools+"' to be installed.");
		clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		SSHCommandResult result = client.runCommandAndWait("yum repolist --enableplugin=rhnplugin --enableplugin=subscription-manager");
		String expectedMsgRHSM = "This system is registered to Red Hat Subscription Management, but is not receiving updates. You can use subscription-manager to assign subscriptions.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.18.4-1")) expectedMsgRHSM = "This system is registered with an entitlement server, but is not receiving updates. You can use subscription-manager to assign subscriptions.";	// subscription-manager commit 5e4b42e1c99472085a44118dd231e7ddd161937a	// https://github.com/candlepin/subscription-manager/pull/1512
		String expectedMsgRHN = null;	// comes from /usr/share/yum-plugins/rhnplugin.py (package yum-rhn-plugin)
		if (Float.valueOf(clienttasks.redhatReleaseXY)>=6.4) expectedMsgRHN = "This system is receiving updates from RHN Classic or RHN Satellite.";
		if (Float.valueOf(clienttasks.redhatReleaseXY)>=7.0) expectedMsgRHN = "This system is receiving updates from RHN Classic or Red Hat Satellite.";
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7 && doesStringContainMatches(sm_rhnHostname, "rhn\\.(.+\\.)*redhat\\.com")) {	// exceptional case when a rhel7 system attempts to register to RHN HOSTED 
			log.warning("With RHEL7 (and beyond), registration to RHN Classic (HOSTED) is no longer supported and therefore no base rhel channel (e.g. rhel-x86_64-server-7) will be available.");
			Assert.assertTrue((result.getStdout()/*+result.getStderr()*/).contains(expectedMsgRHSM), "When registered to both RHN and RHSM (but not subscribed), the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
			Assert.assertFalse((result.getStdout()+result.getStderr()).contains(expectedMsgRHN), "When registered to both RHN and RHSM (but not subscribed), the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
			Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHN_NoChannels), "On RHEL7... When registered to both RHN and RHSM (but not subscribed), the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN_NoChannels+"\n");
			return;
		}
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.12.2-1")) Assert.assertTrue((result.getStdout()/*+result.getStderr()*/).contains(expectedMsgRHSM), "When registered to both RHN and RHSM (but not subscribed), the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
		else Assert.assertTrue(!(result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When registered to both RHN and RHSM (but not subscribed), the subscription-manager yum plugin should NO LONGER inform that:\n"+expectedMsgRHSM+"\nBugzilla https://bugzilla.redhat.com/show_bug.cgi?id=1058380 was used to remove this usability messaging implemented for https://bugzilla.redhat.com/show_bug.cgi?id=818383");
		if (expectedMsgRHN!=null) Assert.assertTrue((result.getStdout()/*+result.getStderr()*/).contains(expectedMsgRHN), "When registered to both RHN and RHSM (but not subscribed), the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36537", "RHEL7-51310"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="When registered to both RHN and RHSM (and subscribed), the subscription-manager yum plugin should inform that: This system is registered to Red Hat Subscription Management, but is not receiving updates. You can use subscription-manager to assign subscriptions.",
			groups={"Tier2Tests","YumPluginMessageCase_Tests","blockedByBug-818383","blockedByBug-832119","blockedByBug-830193","blockedByBug-830194","blockedByBug-906875","blockedByBug-924919"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void testYumPluginMessageCase4B() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true,false,null,null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		if (!isRhnClientToolsInstalled) throw new SkipException("RHN Classic registration requires package '"+rhnClientTools+"' to be installed.");
		clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		SSHCommandResult result = client.runCommandAndWait("yum repolist --enableplugin=rhnplugin --enableplugin=subscription-manager");
		String expectedMsgRHSM = "This system is receiving updates from Red Hat Subscription Management.";
		String expectedMsgRHN = null;	// comes from /usr/share/yum-plugins/rhnplugin.py (package yum-rhn-plugin)
		if (Float.valueOf(clienttasks.redhatReleaseXY)>=6.4) expectedMsgRHN = "This system is receiving updates from RHN Classic or RHN Satellite.";
		if (Float.valueOf(clienttasks.redhatReleaseXY)>=7.0) expectedMsgRHN = "This system is receiving updates from RHN Classic or Red Hat Satellite.";
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7 && doesStringContainMatches(sm_rhnHostname, "rhn\\.(.+\\.)*redhat\\.com")) {	// exceptional case when a rhel7 system attempts to register to RHN HOSTED 
			log.warning("With RHEL7 (and beyond), registration to RHN Classic (HOSTED) is no longer supported and therefore no base rhel channel (e.g. rhel-x86_64-server-7) will be available.");
			//NOT TRUE ANYMORE Assert.assertTrue((result.getStdout()/*+result.getStderr()*/).contains(expectedMsgRHSM), "When registered to both RHN and RHSM (and subscribed), the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
			Assert.assertTrue(!(result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When registered to both RHN and RHSM (and subscribed), the subscription-manager yum plugin NO LONGER informs that:\n"+expectedMsgRHSM+"\nFor justification, see https://bugzilla.redhat.com/show_bug.cgi?id=1017354#c12");	// commit 39eadae14eead4bb79978e52d38da2b3e85cba57 1017354: remove msg printed to stderr via yum
			Assert.assertFalse((result.getStdout()+result.getStderr()).contains(expectedMsgRHN), "When registered to both RHN and RHSM (and subscribed), the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
			Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHN_NoChannels), "On RHEL7... When registered to both RHN and RHSM (and subscribed), the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN_NoChannels+"\n");
			return;
		}
		//NOT TRUE ANYMORE Assert.assertTrue((result.getStdout()/*+result.getStderr()*/).contains(expectedMsgRHSM), "When registered to both RHN and RHSM (and subscribed), the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
		Assert.assertTrue(!(result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When registered to both RHN and RHSM (and subscribed), the subscription-manager yum plugin NO LONGER informs that:\n"+expectedMsgRHSM+"\nFor justification, see https://bugzilla.redhat.com/show_bug.cgi?id=1017354#c12");	// commit 39eadae14eead4bb79978e52d38da2b3e85cba57 1017354: remove msg printed to stderr via yum
		if (expectedMsgRHN!=null) Assert.assertTrue((result.getStdout()/*+result.getStderr()*/).contains(expectedMsgRHN), "When registered to both RHN and RHSM (and subscribed), the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
	}
	
	
	// Candidates for an automated Test:
	// TODO Bug 872310 - yum plugins subscription-manger and rhnplugin should agree to print to stdout or stderr	// when this bug is fixed, simply update the YumPluginMessageCase*_Tests  https://github.com/RedHatQE/rhsm-qe/issues/165
	
	
	// Configuration methods ***********************************************************************
	
	@AfterGroups(groups={"setup"}, value={"InteroperabilityRegister_Test","YumPluginMessageCase_Tests"})
	public void removeRhnSystemIdFileAfterGroups() {
		if (clienttasks==null) return;
		clienttasks.removeRhnSystemIdFile();
	}
	
	@BeforeClass(groups={"setup"})
	public void setupBeforeClass() {
		if (clienttasks==null) return;
		isRhnClientToolsInstalled = clienttasks.isPackageInstalled(rhnClientTools);
		setupRhnCACert();
	}
	
	
	// Protected methods ***********************************************************************
	protected String expectedMsgRHN_NoChannels = "This system is not subscribed to any channels.\nRHN channel support will be disabled.";
	protected final String rhnClientTools = "rhn-client-tools";
	protected boolean isRhnClientToolsInstalled = true;	// assume
	
	
	
	// Data Providers ***********************************************************************
	
}
