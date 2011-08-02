package com.redhat.qe.sm.cli.tests;

import org.testng.SkipException;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
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

	
	// Configuration methods ***********************************************************************
	
	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
