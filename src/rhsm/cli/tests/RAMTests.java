package rhsm.cli.tests;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"RAMTests"})
public class RAMTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************

	@Test(	description="",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void TODO_Test() throws Exception {
		
	}
	
	
	
	
	
	
	// Candidates for an automated Test:
	// TODO http://qe-india.pad.engineering.redhat.com/48?
	/*
	1) Subscribe Ram based subscription 
	Prerequisites : 
	Rhel6.4 with latest subscription manager
	Client should have 1 GB RAM 
	Steps to verify : 
	Register client to candlepin and subscribe RAm based  subscription. 
	Expected Result :
	Single Ram based  subscription should subscribe and no error message should display on console. 
	+1 Looks good.
	https://tcms.engineering.redhat.com/case/221898/?from_plan=5846
	Note : Till 4 GB Consumer Ram , only one Ram based subscription should subscribe. 
	Note : For 5 Gb Consumer RAM , Sm should select (contract list) 2 Ram based subscriotion  to subscribe. 
	I'm not 100% sure what these notes mean.  Important to note that for the initial cut of RAM Subs, stacking/multi-entitlement will not be supported. We are still unsure of how this is supposed to work. From what I understand, this *may not* be decided until RHEL 7.
	Execute the same scenario from CLI. 
	*************************************************************************************************************
	2) Ram info in product and entitlement certificate .
	Prerequisites : 
	Rhel6.4 with latest subscription manager
	Client should have 1 GB RAM 
	Steps to verify : 
	Register client to candlepin and subscribe Ram based subscription .
	Check Product and entitlement cert with rct tool 
	Expected REsult : 
	Ram Info should availbe in product and entitlement cert.
	+1 Looks good.
	https://tcms.engineering.redhat.com/case/221899/?from_plan=5846
	*************************************************************************************************************
	3) Partial subscribe Ram subscription. 
	Prerequisites : 
	Rhel6.4 with latest subscription manager
	Client should have 1 GB RAM 
	Steps to verify : 
	Register client to candlepin and subscribe single RAm based  subscription. 
	Client should be in compliant status. 
	Add 5 GB ram in client . 
	Expected REsult : 
	Now client status should be partial subscribe. 
	+1 Looks good.
	https://tcms.engineering.redhat.com/case/221900/?from_plan=5846
	*************************************************************************************************************
	4) Auto Heal for Ram subscription .
	Prerequisites : 
	Rhel6.4 with latest subscription manager
	Client should have 1 GB RAM and "Ram based subscription" product cert.
	Steps to verify : 
	Register client to candlepin (No auto subscribe).
	Set Autoheal option for 2 min and restart rhsmcertd service. 
	Expected REsult : 
	After 2 min Auto Heal should subscribe single "Ram based subscription".
	+1 Looks good.
	https://tcms.engineering.redhat.com/case/221902/?from_plan=5846
	execute the same scenario for 5 GB Ram.
	Expected Result :  Auto heal should subscribe 2 quantity of Ram based subscription. 
	Stacking/multi-entitlement requirements not yet determined and will likely not be ready for 6.4
	**************************************************************************************************************
	5 : Auto subscribe for RAM based subscription 
	Prerequisites : 
	Rhel6.4 with latest subscription manager
	Client should have 1 GB RAM with Product cert (Ram based subscription)
	Steps to verify : 
	Register client to candlepin using autosubscribe option. 
	Expected REsult : 
	Client should register and subscribe single entitlement of "Ram based subscription"
	+1 Looks good.
	https://tcms.engineering.redhat.com/case/221904/?from_plan=5846
	**************************************************************************************************************
	There are some additional test cases that should be supported regarding client versions and the entitlement certificate versions that they support.
	- Older clients that do not contain RAM supporting code should not be able to consume a RAM subscription from candlepin.
	- Client compatibility is based on the consumer fact 'system.certificate_version'
	6. An older client with 'system.certificate_version' < 3.1 should be able to list RAM entitlements, but not be able to consume one.
	Prerequisites:
	Install an older subscription manager.
	Verify system.certificate_version fact < 3.1
	OR Verify system.certificate_version fact is not present (rhel57,rhel58,rhel61,rhel62,rhel63)
	Steps to Verify:
	Register client to candlepin, do not autosubscribe.
	List subscriptions - RAM Subscriptions should be present.
	Subscribe to RAM subscription.
	Expected Result:
	A message should be presented to the user stating that the client should be updated in order to use this subscription.
	https://tcms.engineering.redhat.com/case/221914/?from_plan=5846
	==============================================
	7. A client with system.certificate_version = 3.0 recieves a version 3.1 certificate when consuming a non-RAM based subscription
	NOTE: This test case is really verifying that candlepin is creating the appropriate certificates and handing them back to subscription-manager. I'm not sure if this test case fits here or not. I'll let you decide that! :)
	Prerequisites:
	Install an older subscription manager.
	Verify system.certificate_version fact == 3.0
	Steps to Verify:
	Register client to candlepin, do not autosubscribe.
	Subscribe to a NON RAM subscription.
	Expected Results:
	Subscribe operation should be successful.
	Running rct cat-cert on the resulting certificate should show a certificate version of 3.1
	jsefler's questions:
	I assume there is a new RAM  attribute that will appear as an OID in the OrderNamespace?  What will  be the subscription attribute name?
	RAM will only be supported on v3.1 certificates.
	Subscription attribute will be 'ram'
	What is the name of the system fact that contains the RAM?
	System fact that is used for determining RAM is 'memory.memtotal'
	If a system has more RAM than the subscription provides AND the subscription is not multi-entitlement=yes, will the subscription show in the "subscription list --available" ?
	- no support for multi-entitlement/stacking at this point (what I hear is it may not be until RHEL7 - but things often change).
	- I would assume that it would be shown. Same as sockets today.
	Will the TESTDATA contain a new Awesome 2Gig RAM Subscription? that requires a tag that is provided by a TESTDATA generated product cert?
	Test data has been added
	- Product: RAM Limiting Product
	- Subscription: RAM Limiting Package (8GB)
	 */
	
	
	
	
	// Configuration methods ***********************************************************************


	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
