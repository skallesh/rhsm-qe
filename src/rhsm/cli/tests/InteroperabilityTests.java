package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.jul.TestRecords;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.EntitlementCert;
import rhsm.data.ProductNamespace;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"InteroperabilityTests"})
public class InteroperabilityTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="User is warned when already registered using RHN Classic",
			groups={"InteroperabilityRegister_Test", "AcceptanceTests", "blockedByBug-730018", "blockedByBug-755130", "blockedByBug-847795", "blockedByBug-859090"},
			enabled=true)
	@ImplementsNitrateTest(caseId=75972)	
	public void InteroperabilityRegister_Test() {

		// interoperabilityWarningMessage is defined in /usr/share/rhsm/subscription_manager/branding/__init__.py self.REGISTERED_TO_OTHER_WARNING
		String interoperabilityWarningMessage = 
			"WARNING" +"\n\n"+
			"You have already registered with RHN using RHN Classic technology. This tool requires registration using RHN Certificate-Based Entitlement technology." +"\n\n"+
			"Except for a few cases, Red Hat recommends customers only register with RHN once." +"\n\n"+
			"For more information, including alternate tools, consult this Knowledge Base Article: https://access.redhat.com/kb/docs/DOC-45563";
		// after Bug 730018 - Warning text message is confusing
		interoperabilityWarningMessage = 
			"WARNING" +"\n\n"+
			"This system has already been registered with RHN using RHN Classic technology." +"\n\n"+
			"The tool you are using is attempting to re-register using RHN Certificate-Based technology. Red Hat recommends (except in a few cases) that customers only register with RHN once. " +"\n\n"+
			"To learn more about RHN registration and technologies please consult this Knowledge Base Article: https://access.redhat.com/kb/docs/DOC-45563";
		// during rhel59, terminology changes were made for "RHN Certificate-Based technology"
		interoperabilityWarningMessage = 
			"WARNING" +"\n\n"+
			"This system has already been registered with RHN using RHN Classic technology." +"\n\n"+
			"The tool you are using is attempting to re-register using Red Hat Subscription Management technology. Red Hat recommends (except in a few cases) that customers only register once. " +"\n\n"+
			"To learn more about RHN registration and technologies please consult this Knowledge Base Article: https://access.redhat.com/kb/docs/DOC-45563";
		// after Bug 847795 - String Update: redhat_branding.py Updates
		interoperabilityWarningMessage = 
			"WARNING" +"\n\n"+
			"This system has already been registered with Red Hat using RHN Classic technology." +"\n\n"+
			"The tool you are using is attempting to re-register using Red Hat Subscription Management technology. Red Hat recommends that customers only register once. " +"\n\n"+
			"To learn how to unregister from either service please consult this Knowledge Base Article: https://access.redhat.com/kb/docs/DOC-45563";
		// after Bug 859090 - String Update: redhat_branding.py
		interoperabilityWarningMessage = 
			"WARNING" +"\n\n"+
			"This system has already been registered with Red Hat using RHN Classic." +"\n\n"+
			"The tool you are using is attempting to re-register using Red Hat Subscription Management technology. Red Hat recommends that customers only register once. " +"\n\n"+
			"To learn how to unregister from either service please consult this Knowledge Base Article: https://access.redhat.com/kb/docs/DOC-45563";
		// during RHEL58, DEV trimmed whitespace from strings...
		interoperabilityWarningMessage = interoperabilityWarningMessage.replaceAll(" +(\n|$)", "$1"); 
		
		// query the branding python file directly to get the default interoperabilityWarningMessage (when the subscription-manager rpm came from a git build - this assumes that any build of subscription-manager must have a branding module e.g. redhat_branding.py)
		/* TEMPORARILY COMMENTING OUT SINCE JBOWES IS INCLUDING THIS BRANDING FILE IN THE PUBLIC REPO - jsefler 9/15/2011
		if (client.runCommandAndWait("rpm -q subscription-manager").getStdout().contains(".git.")) {
			interoperabilityWarningMessage = clienttasks.getBrandingString("REGISTERED_TO_OTHER_WARNING");
		}
		*/
		String interoperabilityWarningMessageRegex = "^"+interoperabilityWarningMessage.replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)").replaceAll("\\.", "\\\\.");
		Assert.assertTrue(interoperabilityWarningMessage.startsWith("WARNING"), "The expected interoperability message starts with \"WARNING\".");
		
		log.info("Simulating registration to RHN Classic by creating an empty systemid file '"+clienttasks.rhnSystemIdFile+"'...");
		RemoteFileTasks.runCommandAndWait(client, "touch "+clienttasks.rhnSystemIdFile, TestRecords.action());
		Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.rhnSystemIdFile), "RHN Classic systemid file '"+clienttasks.rhnSystemIdFile+"' is in place.");
		
		log.info("Attempt to register while already registered via RHN Classic...");
		SSHCommandResult result = clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, true, false, null, null, null);
		//Assert.assertTrue(result.getStdout().startsWith(interoperabilityWarningMessage), "subscription-manager warns the registerer when the system is already registered via RHN Classic with this expected message:\n"+interoperabilityWarningMessage);
		//Assert.assertContainsMatch(result.getStdout(),interoperabilityWarningMessageRegex, "subscription-manager warns the registerer when the system is already registered via RHN Classic with the expected message.");
		Assert.assertTrue(result.getStdout().contains(interoperabilityWarningMessage), "subscription-manager warns the registerer when the system is already registered via RHN Classic with this expected message:\n"+interoperabilityWarningMessage+"\n");

		log.info("Now let's make sure we are NOT warned when we are NOT already registered via RHN Classic...");
		clienttasks.removeRhnSystemIdFile();
		Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.rhnSystemIdFile), "RHN Classic systemid file '"+clienttasks.rhnSystemIdFile+"' is gone.");
		result = clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, true, false, null, null, null);
		
		//Assert.assertFalse(result.getStdout().startsWith(interoperabilityWarningMessage), "subscription-manager does NOT warn registerer when the system is not already registered via RHN Classic.");
		//Assert.assertContainsNoMatch(result.getStdout(),interoperabilityWarningMessageRegex, "subscription-manager does NOT warn registerer when the system is NOT already registered via RHN Classic.");
		Assert.assertTrue(!result.getStdout().contains(interoperabilityWarningMessage), "subscription-manager does NOT warn registerer when the system is NOT already registered via RHN Classic.");
	}
	
	
	@Test(	description="When registered to RHSM (and all subscriptions have expired), the subscription-manager yum plugin should inform that: The subscription for following product(s) has expired: etc.",
			groups={"YumPluginMessageCase_Tests","blockedByBug-818383","blockedByBug-832119","blockedByBug-871146", "blockedbyBug-901612"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void YumPluginMessageCase0_Test() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,true,false,null,null,null);
		
		// import an expired certificate 
		File expiredCertFile = new File(System.getProperty("automation.dir", null)+"/expiredcerts/Expiredcert.pem");
		RemoteFileTasks.putFile(client.getConnection(), expiredCertFile.getPath(), "/tmp/Expiredcert.pem", "0644");
		clienttasks.importCertificate("/tmp/Expiredcert.pem");
		EntitlementCert expiredEntitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(new File("/tmp/Expiredcert.pem"));
		
		// assert the registration message (without any current subscriptions)
		SSHCommandResult result = client.runCommandAndWait("yum repolist --disableplugin=rhnplugin --enableplugin=subscription-manager");
		String expectedMsgRHSM = "This system is registered to Red Hat Subscription Management, but is not receiving updates. You can use subscription-manager to assign subscriptions.";
		//Assert.assertTrue(result.getStdout().contains(expectedMsgRHSM), "When registered to RHSM (and all subscriptions have expired), the subscription-manager yum plugin stdout should inform that:\n"+expectedMsgRHSM+"\n");	// Bug 901612 - Subscription-manager-s yum plugin prints warning to stdout instead of stderr.
		Assert.assertTrue(result.getStderr().contains(expectedMsgRHSM), "When registered to RHSM (and all subscriptions have expired), the subscription-manager yum plugin stderr should inform that:\n"+expectedMsgRHSM+"\n");
		// assert the expired subscriptions message
		expectedMsgRHSM = "*** WARNING ***\nThe subscription for following product(s) has expired:";
		for (ProductNamespace productNamespace : expiredEntitlementCert.productNamespaces) expectedMsgRHSM += "\n"+"  - "+productNamespace.name;
		expectedMsgRHSM += "\n"+"You no longer have access to the repositories that provide these products.  It is important that you apply an active subscription in order to resume access to security and other critical updates. If you don't have other active subscriptions, you can renew the expired subscription.";
		//Assert.assertTrue(result.getStdout().contains(expectedMsgRHSM), "When registered to RHSM (and all subscriptions have expired), the subscription-manager yum plugin stdout should inform that:\n"+expectedMsgRHSM+"\n");	// Bug 901612 - Subscription-manager-s yum plugin prints warning to stdout instead of stderr.
		Assert.assertTrue(result.getStderr().contains(expectedMsgRHSM), "When registered to RHSM (and all subscriptions have expired), the subscription-manager yum plugin stderr should inform that:\n"+expectedMsgRHSM+"\n");

		// assert the registration message (with current subscriptions)
		//clienttasks.subscribeToSubscriptionPool(clienttasks.getCurrentlyAvailableSubscriptionPools().get(0));	// will fail with java.lang.AssertionError: The list of consumed products is entitled 'Consumed Subscriptions'. expected:<true> but was:<false>
		clienttasks.subscribe(null, null, clienttasks.getCurrentlyAvailableSubscriptionPools().get(0).poolId, null, null, null, null, null, null, null, null);
		clienttasks.importCertificate("/tmp/Expiredcert.pem");
		result = client.runCommandAndWait("yum repolist --disableplugin=rhnplugin --enableplugin=subscription-manager");
		expectedMsgRHSM = "This system is receiving updates from Red Hat Subscription Management.";
		//Assert.assertTrue(result.getStdout().contains(expectedMsgRHSM), "When registered to RHSM (and some subscriptions have expired), the subscription-manager yum plugin stdout should inform that:\n"+expectedMsgRHSM+"\n");	// Bug 901612 - Subscription-manager-s yum plugin prints warning to stdout instead of stderr.
		Assert.assertTrue(result.getStderr().contains(expectedMsgRHSM), "When registered to RHSM (and some subscriptions have expired), the subscription-manager yum plugin stderr should inform that:\n"+expectedMsgRHSM+"\n");
		// assert the expired subscriptions message again
		expectedMsgRHSM = "*** WARNING ***\nThe subscription for following product(s) has expired:";
		for (ProductNamespace productNamespace : expiredEntitlementCert.productNamespaces) expectedMsgRHSM += "\n"+"  - "+productNamespace.name;
		expectedMsgRHSM += "\n"+"You no longer have access to the repositories that provide these products.  It is important that you apply an active subscription in order to resume access to security and other critical updates. If you don't have other active subscriptions, you can renew the expired subscription.";
		//Assert.assertTrue(result.getStdout().contains(expectedMsgRHSM), "When registered to RHSM (and some subscriptions have expired), the subscription-manager yum plugin stdout should inform that:\n"+expectedMsgRHSM+"\n");	// Bug 901612 - Subscription-manager-s yum plugin prints warning to stdout instead of stderr.
		Assert.assertTrue(result.getStderr().contains(expectedMsgRHSM), "When registered to RHSM (and some subscriptions have expired), the subscription-manager yum plugin stderr should inform that:\n"+expectedMsgRHSM+"\n");
	}
	
	@Test(	description="When not registered to either RHN nor RHSM, the subscription-manager yum plugin should inform that: This system is not registered to Red Hat Subscription Management. You can use subscription-manager to register.",
			groups={"YumPluginMessageCase_Tests","blockedByBug-818383","blockedByBug-832119","blockedByBug-830193","blockedByBug-830194","blockedByBug-906875"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void YumPluginMessageCase1_Test() {
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7) throw new SkipException("With RHEL7 (and beyond), registration to RHN Classic is no longer supported; however RHN Satellite registration is supported.  See bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=906875 ");	// TODO: setup a dedicated Satellite server for this test
		clienttasks.unregister(null,null,null);
		clienttasks.removeRhnSystemIdFile();
		SSHCommandResult result = client.runCommandAndWait("yum repolist --enableplugin=rhnplugin --enableplugin=subscription-manager");
		String expectedMsgRHSM = "This system is not registered to Red Hat Subscription Management. You can use subscription-manager to register.";
		String expectedMsgRHN = "This system is not registered with RHN Classic or RHN Satellite.\nYou can use rhn_register to register.\nRHN Satellite or RHN Classic support will be disabled.";
		Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When not registered to either RHN nor RHSM, the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
		Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHN), "When not registered to either RHN nor RHSM, the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
	}
	
	@Test(	description="When registered to RHN but not RHSM, the subscription-manager yum plugin should inform that: This system is not registered to Red Hat Subscription Management. You can use subscription-manager to register.",
			groups={"YumPluginMessageCase_Tests","blockedByBug-818383","blockedByBug-832119","blockedByBug-830193","blockedByBug-830194","blockedByBug-906875"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void YumPluginMessageCase2_Test() {
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7) throw new SkipException("With RHEL7 (and beyond), registration to RHN Classic is no longer supported; however RHN Satellite registration is supported.  See bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=906875 ");	// TODO: setup a dedicated Satellite server for this test
		clienttasks.unregister(null,null,null);
		clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		SSHCommandResult result = client.runCommandAndWait("yum repolist --enableplugin=rhnplugin --enableplugin=subscription-manager");
		String expectedMsgRHSM = "This system is not registered to Red Hat Subscription Management. You can use subscription-manager to register.";
		String expectedMsgRHN = "This system is receiving updates from RHN Classic or RHN Satellite.";
		Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When registered to RHN but not RHSM, the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
		Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHN), "When registered to RHN but not RHSM, the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
	}
	
	@Test(	description="When registered to RHSM (but not subscribed) but not RHN, the subscription-manager yum plugin should inform that: This system is registered to Red Hat Subscription Management, but is not receiving updates. You can use subscription-manager to assign subscriptions.",
			groups={"YumPluginMessageCase_Tests","blockedByBug-818383","blockedByBug-832119","blockedByBug-830193","blockedByBug-830194","blockedByBug-906875"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void YumPluginMessageCase3A_Test() {
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7) throw new SkipException("With RHEL7 (and beyond), registration to RHN Classic is no longer supported; however RHN Satellite registration is supported.  See bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=906875 ");	// TODO: setup a dedicated Satellite server for this test
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,true,false,null,null,null);
		clienttasks.removeRhnSystemIdFile();
		SSHCommandResult result = client.runCommandAndWait("yum repolist --enableplugin=rhnplugin --enableplugin=subscription-manager");
		String expectedMsgRHSM = "This system is registered to Red Hat Subscription Management, but is not receiving updates. You can use subscription-manager to assign subscriptions.";
		String expectedMsgRHN = "This system is not registered with RHN Classic or RHN Satellite.\nYou can use rhn_register to register.\nRHN Satellite or RHN Classic support will be disabled.";
		Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When registered to RHSM (but not subscribed) but not RHN, the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
		Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHN), "When registered to RHSM (but not subscribed) but not RHN, the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
	}
	
	@Test(	description="When registered to RHSM (and subscribed) but not RHN, the subscription-manager yum plugin should inform that: This system is registered to Red Hat Subscription Management, but is not receiving updates. You can use subscription-manager to assign subscriptions.",
			groups={"YumPluginMessageCase_Tests","blockedByBug-818383","blockedByBug-832119","blockedByBug-830193","blockedByBug-830194","blockedByBug-906875"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void YumPluginMessageCase3B_Test() throws JSONException, Exception {
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7) throw new SkipException("With RHEL7 (and beyond), registration to RHN Classic is no longer supported; however RHN Satellite registration is supported.  See bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=906875 ");	// TODO: setup a dedicated Satellite server for this test
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,true,false,null,null,null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		clienttasks.removeRhnSystemIdFile();
		SSHCommandResult result = client.runCommandAndWait("yum repolist --enableplugin=rhnplugin --enableplugin=subscription-manager");
		String expectedMsgRHSM = "This system is receiving updates from Red Hat Subscription Management.";
		String expectedMsgRHN = "This system is not registered with RHN Classic or RHN Satellite.\nYou can use rhn_register to register.\nRHN Satellite or RHN Classic support will be disabled.";
		Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When registered to RHSM (and subscribed) but not RHN, the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
		Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHN), "When registered to RHSM (and subscribed) but not RHN, the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
	}
	
	@Test(	description="When registered to both RHN and RHSM (but not subscribed), the subscription-manager yum plugin should inform that: This system is registered to Red Hat Subscription Management, but is not receiving updates. You can use subscription-manager to assign subscriptions.",
			groups={"YumPluginMessageCase_Tests","blockedByBug-818383","blockedByBug-832119","blockedByBug-830193","blockedByBug-830194","blockedByBug-871146","blockedByBug-906875"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void YumPluginMessageCase4A_Test() {
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7) throw new SkipException("With RHEL7 (and beyond), registration to RHN Classic is no longer supported; however RHN Satellite registration is supported.  See bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=906875 ");	// TODO: setup a dedicated Satellite server for this test
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,true,false,null,null,null);
		clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		SSHCommandResult result = client.runCommandAndWait("yum repolist --enableplugin=rhnplugin --enableplugin=subscription-manager");
		String expectedMsgRHSM = "This system is registered to Red Hat Subscription Management, but is not receiving updates. You can use subscription-manager to assign subscriptions.";
		String expectedMsgRHN = "This system is receiving updates from RHN Classic or RHN Satellite.";
		Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When registered to both RHN and RHSM (but not subscribed), the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
		Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHN), "When registered to both RHN and RHSM (but not subscribed), the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
	}
	
	@Test(	description="When registered to both RHN and RHSM (and subscribed), the subscription-manager yum plugin should inform that: This system is registered to Red Hat Subscription Management, but is not receiving updates. You can use subscription-manager to assign subscriptions.",
			groups={"YumPluginMessageCase_Tests","blockedByBug-818383","blockedByBug-832119","blockedByBug-830193","blockedByBug-830194","blockedByBug-906875"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)	
	public void YumPluginMessageCase4B_Test() throws JSONException, Exception {
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7) throw new SkipException("With RHEL7 (and beyond), registration to RHN Classic is no longer supported; however RHN Satellite registration is supported.  See bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=906875 ");	// TODO: setup a dedicated Satellite server for this test
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,true,false,null,null,null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		SSHCommandResult result = client.runCommandAndWait("yum repolist --enableplugin=rhnplugin --enableplugin=subscription-manager");
		String expectedMsgRHSM = "This system is receiving updates from Red Hat Subscription Management.";
		String expectedMsgRHN = "This system is receiving updates from RHN Classic or RHN Satellite.";
		Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHSM), "When registered to both RHN and RHSM (and subscribed), the subscription-manager yum plugin should inform that:\n"+expectedMsgRHSM+"\n");
		Assert.assertTrue((result.getStdout()+result.getStderr()).contains(expectedMsgRHN), "When registered to both RHN and RHSM (and subscribed), the rhnplugin yum plugin should inform that:\n"+expectedMsgRHN+"\n");
	}
	
	
	// Candidates for an automated Test:
	
	// All of these are the same...
	// TODO Bug 872310 - yum plugins subscription-manger and rhnplugin should agree to print to stdout or stderr	// when this bug is fixed, simply update the YumPluginMessageCase*_Tests
	
	
	// Configuration methods ***********************************************************************
	
	@AfterGroups(groups={"setup"}, value={"InteroperabilityRegister_Test","YumPluginMessageCase_Tests"})
	public void removeRhnSystemIdFileAfterGroups() {
		if (clienttasks==null) return;
		clienttasks.removeRhnSystemIdFile();
	}
	
	
	
	// Protected methods ***********************************************************************
	
	
	
	// Data Providers ***********************************************************************
	
}
