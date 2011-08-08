package com.redhat.qe.sm.cli.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"RedeemTests"})
public class RedeemTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager: verify redeem requires registration",
			groups={},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void AttemptRedeemWithoutBeingRegistered_Test() {
		
		clienttasks.unregister(null,null,null);
		SSHCommandResult redeemResult = clienttasks.redeem_(null,null,null,null,null);
		
		// assert redemption results
		Assert.assertEquals(redeemResult.getStdout().trim(), "Error: You need to register this system by running `register` command before using this option.","Redeem should require that the system be registered.");
		Assert.assertEquals(redeemResult.getExitCode(), Integer.valueOf(1),"Exit code from redeem when executed against a standalone candlepin server.");
	}
	
	@Test(	description="subscription-manager: attempt redeem without --email option",
			groups={"blockedByBug-727600"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptRedeemWithoutEmail_Test() {
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, null);
		SSHCommandResult redeemResult = clienttasks.redeem_(null,null,null,null,null);
		
		// assert redemption results
		//Assert.assertEquals(redeemResult.getStdout().trim(), "email and email_locale are required for notification","Redeem should require that the email option be specified.");
		Assert.assertEquals(redeemResult.getStdout().trim(), "email is required for notification","Redeem should require that the email option be specified.");
		Assert.assertEquals(redeemResult.getExitCode(), Integer.valueOf(255),"Exit code from redeem when executed against a standalone candlepin server.");

	}
	
	@Test(	description="subscription-manager: attempt redeem with --email option (against an onpremises candlepin server)",
			groups={"blockedByBug-726791"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void RedeemWithEmail_Test() {
		String warning = "This test was authored for execution against an on-premises candlepin server.";
		if (sm_isServerOnPremises) {
			log.warning(warning);
		} else {
			throw new SkipException(warning);
		}
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, null);
		SSHCommandResult redeemResult = clienttasks.redeem("tester@redhat.com",null,null,null,null);
		
		// assert redemption results
		//Assert.assertEquals(redeemResult.getStdout().trim(), "Standalone candlepin does not support activation.","Standalone candlepin does not support activation.");
		Assert.assertEquals(redeemResult.getStdout().trim(), "Standalone candlepin does not support redeeming a subscription.","Standalone candlepin does not support redeeming a subscription.");
		Assert.assertEquals(redeemResult.getExitCode(), Integer.valueOf(255),"Exit code from redeem when executed against a standalone candlepin server.");

	}
	
	@Test(	description="subscription-manager: attempt redeem against an onpremises candlepin server that has been patched for mock testing",
			groups={"MockRedeemTests"},
			dataProvider="getOnPremisesMockAttemptToRedeemData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void onPremisesMockAttemptToRedeem_Test(String testDescription, String serialNumber, Integer expectedExitCode, String expectedStdout, String expectedStderr) {
		String warning = "This mock test was authored for execution against an on-premises candlepin server.";
		if (sm_isServerOnPremises) {
			log.warning(warning);
		} else {
			throw new SkipException(warning);
		}
		log.info(testDescription);

		// create a facts file with a serialNumber that will clobber the true system facts
		createMockAssetFactsFile("Dell Inc.",serialNumber);
		
		// attempt redeem
		SSHCommandResult redeemResult = clienttasks.redeem("tester@redhat.com",null,null,null,null);
		
		// assert the redeemResult here
		if (expectedExitCode!=null) Assert.assertEquals(redeemResult.getExitCode(), expectedExitCode);
		if (expectedStdout!=null) Assert.assertEquals(redeemResult.getStdout().trim(), expectedStdout.replaceFirst("\\{0\\}", serialNumber));
		if (expectedStderr!=null) Assert.assertEquals(redeemResult.getStderr().trim(), expectedStderr);

	}
	
	
	
	
	
	
	// Candidates for an automated Test:
	
	
	
	
	// Configuration methods ***********************************************************************
	
	@AfterGroups(value={"MockRedeemTests"}, groups={"setup"}, alwaysRun=true)
	public void deleteMockAssetFactsFile () {
		String systemManufacturerKey="dmi.system.manufacturer";
		String systemSerialNumberKey="dmi.system.serial_number";
		String command = String.format("rm -f %s", clienttasks.factsDir+"/"+mockAssetFactsFileName);
        client.runCommandAndWait(command);	// delete the override for facts
//		clienttasks.facts(null,true, null, null, null);	// update the facts
//		String systemManufacturer = client.runCommandAndWait("dmidecode -s system-manufacturer").getStdout().trim();
//		String systemSerialNumber = client.runCommandAndWait("dmidecode -s system-serial-number").getStdout().trim();
//
//		Assert.assertEquals(clienttasks.getFactValue(systemManufacturerKey), systemManufacturer, "Successfully reverted the mock fact for "+systemManufacturerKey);
//		Assert.assertEquals(clienttasks.getFactValue(systemSerialNumberKey), systemSerialNumber, "Successfully reverted the mock fact for "+systemSerialNumberKey);
	}
	
	
	// Protected methods ***********************************************************************

	protected String mockAssetFactsFileName = "mockAsset.facts";
	
	protected void createMockAssetFactsFile (String systemManufacturer, String systemSerialNumber) {
		log.info("For the sakeof mock testing the system's seriial number, we will create a local facts file on the client to override the true system facts...");
		String systemManufacturerKey="dmi.system.manufacturer";
		String systemSerialNumberKey="dmi.system.serial_number";
		String command = String.format("echo '{\"%s\": \"%s\", \"%s\": \"%s\"}' > %s", systemManufacturerKey, systemManufacturer, systemSerialNumberKey, systemSerialNumber, clienttasks.factsDir+"/"+mockAssetFactsFileName);
        client.runCommandAndWait(command);	// create an override for facts
		clienttasks.facts(null,true, null, null, null);	// update the facts
		//Assert.assertEquals(clienttasks.getFactValue(systemManufacturerKey), systemManufacturer, "Successfully created and updated a mock fact for "+systemManufacturerKey);
		Assert.assertEquals(clienttasks.getFactValue(systemSerialNumberKey), systemSerialNumber, "Successfully created and updated a mock fact for "+systemSerialNumberKey);
	}
	

	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="getOnPremisesMockAttemptToRedeemData")
	public Object[][] getOnPremisesMockAttemptToRedeemDataAs2dArray() throws Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getOnPremisesMockAttemptToRedeemDataAsListOfLists());
	}
	protected List<List<Object>> getOnPremisesMockAttemptToRedeemDataAsListOfLists() throws Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, null);
		
		
		Integer expectedExitCode = new Integer(0);
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=727978 - jsefler 8/03/2011
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="727978"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla bug "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			expectedExitCode = new Integer(255);
		}
		// END OF WORKAROUND
		
		// String testDescription, String serialNumber, Integer expectedExitCode, String expectedStdout, String expectedStderr
		ll.add(Arrays.asList(new Object[]{"This mocked redeem test attempts to redeem a subscription against a standalone candlepin server.",										"0ABCDEF",	expectedExitCode,	"Standalone candlepin does not support redeeming a subscription.",	null}));
		ll.add(Arrays.asList(new Object[]{"This mocked redeem test attempts to redeem a subscription when the system's asset tag has already been used to redeem a subscription.",	"1ABCDEF",	expectedExitCode,	"The Dell service tag: {0}, has already been used to activate a subscription",	null}));
		ll.add(Arrays.asList(new Object[]{"This mocked redeem test attempts to redeem a subscription for which the system's asset tag will not be found for redemption.",			"2ABCDEF",	expectedExitCode,	"A subscription was not found for the given Dell service tag: {0}",	null}));
		ll.add(Arrays.asList(new Object[]{"This mocked redeem test attempts to redeem a subscription for which the system's  service tag is expired.",								"3ABCDEF",	expectedExitCode,	"The Dell service tag: {0}, is expired",	null}));
		ll.add(Arrays.asList(new Object[]{"This mocked redeem test attempts to redeem a subscription at a time when the system is unable to process the request.",					"4ABCDEF",	expectedExitCode,	"The system is unable to process the requested subscription activation {0}",	null}));
		ll.add(Arrays.asList(new Object[]{"This mocked redeem test attempts to redeem a subscription from a system with a valid asset tag.",										"5ABCDEF",	expectedExitCode,	"Your subscription activation is being processed and should be available soon. You will be notified via email once it is available. If you have any questions, additional information can be found here: https://access.redhat.com/kb/docs/DOC-53864.",	null}));
		ll.add(Arrays.asList(new Object[]{"This mocked redeem test attempts to redeem a subscription at a time when the system is unable to process the request.",					"6ABCDEF",	expectedExitCode,	"The system is unable to process the requested subscription activation {0}",	null}));

		return ll;
	}
}
