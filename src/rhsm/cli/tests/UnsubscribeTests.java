package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.jul.TestRecords;
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
@Test(groups={"UnsubscribeTests"})
public class UnsubscribeTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27126", "RHEL7-51409"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager-cli: unsubscribe consumer from an entitlement using product ID",
			groups={"Tier2Tests","blockedByBug-584137", "blockedByBug-602852", "blockedByBug-873791"},
			//dataProvider="getAllConsumedProductSubscriptionsData",	// 06/04/2014 takes too long; rarely reveals a bug
			dataProvider="getRandomSubsetOfConsumedProductSubscriptionsData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41688)
	public void testUnsubscribeFromValidProductIDs(ProductSubscription productSubscription){
//		sm.subscribeToEachOfTheCurrentlyAvailableSubscriptionPools();
//		sm.unsubscribeFromEachOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.unsubscribeFromProductSubscription(productSubscription);
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27124", "RHEL7-51397"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Unsubscribe product entitlement and re-subscribe",
			groups={"Tier2Tests","blockedByBug-584137","blockedByBug-602852","blockedByBug-873791","blockedByBug-979492"},
			//dataProvider="getAllConsumedProductSubscriptionsData",	// 06/04/2014 takes too long; rarely reveals a bug
			dataProvider="getRandomSubsetOfConsumedProductSubscriptionsData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41898)
	public void testResubscribeAfterUnsubscribe(ProductSubscription productSubscription) throws Exception{
///*debugTesting*/if (!productSubscription.productId.equals("awesomeos-unlimited-quantity")) throw new SkipException("debugTesting");
		
		// now loop through each consumed product subscription and unsubscribe/re-subscribe
		SubscriptionPool pool = clienttasks.getSubscriptionPoolFromProductSubscription(productSubscription,sm_clientUsername,sm_clientPassword);
		if (clienttasks.unsubscribeFromProductSubscription(productSubscription)) {
			Assert.assertNotNull(pool, "Successfully determined what SubscriptionPool ProductSubscription '"+productSubscription+"' was consumed from.");
			clienttasks.subscribeToSubscriptionPool/*UsingProductId*/(pool);	// only re-subscribe when unsubscribe was a success
		}
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47922", "RHEL7-97323"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Unsubscribe from a valid entitlement and then maliciously attempt to reuse the revoked entitlement cert.",
			groups={"Tier1Tests","blockedByBug-584137","blockedByBug-602852","blockedByBug-672122","blockedByBug-804227","blockedByBug-871146","blockedByBug-905546","blockedByBug-962520","blockedByBug-822402","blockedByBug-986572","blockedByBug-1000301","blockedByBug-1026435"},
			//dataProvider="getAvailableSubscriptionPoolsData",	// very thorough, but takes too long to execute and rarely finds more bugs
			dataProvider="getRandomSubsetOfAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsNitrateTest(caseId=41903)
	public void testUnsubscribeAndAttemptToReuseTheRevokedEntitlementCert(SubscriptionPool subscriptionPool) throws JSONException, Exception{
		client.runCommandAndWaitWithoutLogging("killall -9 yum");
		
		// choose a quantity before subscribing to avoid Stdout: Quantity '1' is not a multiple of instance multiplier '2'
		String quantity = null;
		/*if (clienttasks.isPackageVersion("subscription-manager",">=","1.10.3-1"))*/ if (subscriptionPool.suggested!=null) {
			if (subscriptionPool.suggested<1) quantity = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, subscriptionPool.poolId, "instance_multiplier"); 	// when the Suggested quantity is 0, let's specify a quantity to avoid Stdout: Quantity '1' is not a multiple of instance multiplier '2'
			if (subscriptionPool.suggested>1 && quantity==null) quantity = subscriptionPool.suggested.toString();
		}

		// subscribe to a pool
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(subscriptionPool,quantity,/*sm_serverAdminUsername*/sm_clientUsername,/*sm_serverAdminPassword*/sm_clientPassword,sm_serverUrl);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		List <EntitlementCert> entitlementCerts = new ArrayList<EntitlementCert>();
		entitlementCerts.add(entitlementCert);

		// assert all of the entitlement certs are reported in the "yum repolist all"
		clienttasks.assertEntitlementCertsInYumRepolist(entitlementCerts,true);
 
		// copy entitlement certificate from location /etc/pki/entitlement/product/ to /tmp
		log.info("Now let's copy the valid entitlement cert to the side so we can maliciously try to reuse it after its serial has been unsubscribed.");
		String randDir = "/tmp/sm-certForSubscriptionPool-"+subscriptionPool.poolId;
		client.runCommandAndWait("rm -rf "+randDir+"; mkdir -p "+randDir);
		client.runCommandAndWait("cp "+entitlementCertFile+" "+randDir+"; cp "+clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(entitlementCertFile)+" "+randDir);
		
		// unsubscribe from the pool (Note: should be the only one subscribed too
		clienttasks.unsubscribeFromSerialNumber(clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCertFile));
		
		// assert all of the entitlement certs are no longer reported in the "yum repolist all"
		clienttasks.assertEntitlementCertsInYumRepolist(entitlementCerts,false);

		/* restarting rhsmcertd takes too long and can screw up the certCheckInterval
		// restart the rhsm cert deamon
		// Note: by passing assertCertificatesUpdate=null, we are assuming that the subsequent assertions will execute within 2 min before the next cert update and waitForRegexInRhsmcertdLog 
		int certFrequency = 2; clienttasks.restart_rhsmcertd(certFrequency, null, null);
		*/
		log.info("Assuming that the currently configured value of certCheckInterval='"+clienttasks.getConfParameter("certCheckInterval")+"' will not interfere with this test.");
		
		// move the copied entitlement certificate from /tmp to location /etc/pki/entitlement/product
		// Note: this is malicious activity (user is trying to continue using entitlement certs that have been unsubscribed)
		client.runCommandAndWait("cp -f "+randDir+"/* "+clienttasks.entitlementCertDir);
		
		// assert all of the entitlement certs are reported in the "yum repolist all" again
		clienttasks.assertEntitlementCertsInYumRepolist(entitlementCerts,true);
		
		// assert that the rhsmcertd will clean up the malicious activity
		/* restarting rhsmcertd takes too long; instead we will call run_rhsmcertd_worker(false)
		log.info("Now let's wait for \"Certificates updated\" by the rhsmcertd and assert that the deamon deletes the copied entitlement certificate since it was put on candlepins certificate revocation list during the unsubscribe.");
		String marker = "Testing UnsubscribeAndAttemptToReuseTheRevokedEntitlementCert_Test..."; // https://tcms.engineering.redhat.com/case/41692/
		RemoteFileTasks.runCommandAndAssert(client,"echo \""+marker+"\" >> "+clienttasks.rhsmcertdLogFile,Integer.valueOf(0));
		clienttasks.waitForRegexInRhsmcertdLog("Certificates updated.", certFrequency);	// https://bugzilla.redhat.com/show_bug.cgi?id=672122
		sleep(10000); // plus a little padding for the client to do it's thing
 		*/clienttasks.run_rhsmcertd_worker(false);
		Assert.assertTrue(!RemoteFileTasks.testExists(client, entitlementCertFile.getPath()),"Entitlement certificate '"+entitlementCertFile+"' was deleted by the rhsm certificate deamon.");
		clienttasks.assertEntitlementCertsInYumRepolist(entitlementCerts,false);
		
		// cleanup
		client.runCommandAndWait("rm -rf "+randDir);
	}

	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-27125", "RHEL7-51946"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: subscribe and then unsubscribe from a future subscription pool",
			groups={"Tier2Tests","blockedByBug-727970","blockedByBug-958775"},
			//dataProvider="getAllFutureSystemSubscriptionPoolsData",	// 06/04/2014 takes too long; rarely reveals a bug
			dataProvider="getRandomSubsetOfFutureSystemSubscriptionPoolsData",
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testUnsubscribeAfterSubscribingToFutureSubscriptionPool(SubscriptionPool pool) throws Exception {
//if (!pool.productId.equals("awesomeos-virt-unlmtd-phys")) throw new SkipException("debugTesting pool productId="+pool.productId);
		
		Calendar now = new GregorianCalendar();
		now.setTimeInMillis(System.currentTimeMillis());
		
		// subscribe to the future SubscriptionPool
		SSHCommandResult subscribeResult = clienttasks.subscribe(null,null,pool.poolId,null,null,null,null,null,null,null, null, null, null);
		// Pool is restricted to virtual guests: '8a90f85734205a010134205ae8d80403'.
		// Pool is restricted to physical systems: '8a9086d3443c043501443c052aec1298'.
		if (subscribeResult.getStdout().startsWith("Pool is restricted")) {
			throw new SkipException("Subscribing to this future subscription is not applicable to this test: "+pool);
		}
		
		// assert that the granted EntitlementCert and its corresponding key exist
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertCorrespondingToSubscribedPool(pool);
		File entitlementCertFile = clienttasks.getEntitlementCertFileFromEntitlementCert(entitlementCert);
		File entitlementCertKeyFile = clienttasks.getEntitlementCertKeyFileFromEntitlementCert(entitlementCert);
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, entitlementCertFile.getPath()), 1,"EntitlementCert file exists after subscribing to future SubscriptionPool.");
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, entitlementCertKeyFile.getPath()), 1,"EntitlementCert key file exists after subscribing to future SubscriptionPool.");

		// find the consumed ProductSubscription from the future SubscriptionPool
		ProductSubscription productSubscription =  ProductSubscription.findFirstInstanceWithMatchingFieldFromList("serialNumber",entitlementCert.serialNumber, clienttasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertNotNull(productSubscription,"Found the newly consumed ProductSubscription after subscribing to future subscription pool '"+pool.poolId+"'.");
		
		// unsubscribe
		clienttasks.unsubscribeFromProductSubscription(productSubscription);
		
		// assert that the EntitlementCert file and its key are removed.
		/* NOTE: this assertion is already built into the unsubscribeFromProductSubscription task above
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, entitlementCertFile.getPath()), 0,"EntitlementCert file has been removed after unsubscribing to future SubscriptionPool.");
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, entitlementCertKeyFile.getPath()), 0,"EntitlementCert key file has been removed after unsubscribing to future SubscriptionPool.");
		*/
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36600", "RHEL7-51408"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Attempt to unsubscribe from a serial when not registered",
			groups={"Tier2Tests","blockedByBug-735338","blockedByBug-838146","blockedByBug-865590","blockedByBug-873791"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsubscribeFromSerialWhenNotRegistered() {
	
		// first make sure we are subscribed to a pool
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null, true, false, null, null, null, null);
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size()));	// random available pool
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.subscribeToSubscriptionPool(pool,/*sm_serverAdminUsername*/sm_clientUsername,/*sm_serverAdminPassword*/sm_clientPassword,sm_serverUrl));
		
		// now remove the consumer cert to simulate an unregister
		clienttasks.removeAllCerts(true,false, false);
		SSHCommandResult identityResult = clienttasks.identity_(null,null,null,null,null,null,null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) { // post commit 5697e3af094be921ade01e19e1dfe7b548fb7d5b bug 1119688
			Assert.assertEquals(identityResult.getStderr().trim(),clienttasks.msg_ConsumerNotRegistered, "stderr");
		} else {
			Assert.assertEquals(identityResult.getStdout().trim(),clienttasks.msg_ConsumerNotRegistered, "stdout");
		}
		
		// now unsubscribe from the serial number (while not registered)
		Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().size()>0, "We should be consuming an entitlement (even while not registered)");
		//clienttasks.unsubscribeFromSerialNumber(entitlementCert.serialNumber);	// this will assert a different stdout message, instead call unsubscribe manually and assert results
		SSHCommandResult result = clienttasks.unsubscribe(null,entitlementCert.serialNumber,null,null,null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "Subscription with serial number "+entitlementCert.serialNumber+" removed from this system", "We should always be able to remove a subscription (even while not registered).");
		Assert.assertEquals(clienttasks.getCurrentlyConsumedProductSubscriptions().size(), 0, "We should not be consuming any entitlements after unsubscribing (while not registered).");
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36598", "RHEL7-51403"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Attempt to unsubscribe when from an invalid serial number",
			groups={"Tier2Tests","blockedByBug-706889","blockedByBug-867766"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsubscribeFromAnInvalidSerial() {
		SSHCommandResult result;
		
		BigInteger serial = BigInteger.valueOf(-123);
		result = clienttasks.unsubscribe_(null, serial, null, null, null, null, null);
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(64);	// EX_USAGE // post commit 5697e3af094be921ade01e19e1dfe7b548fb7d5b bug 1119688
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "Asserting exit code when attempting to unsubscribe from an invalid serial number.");
		//Assert.assertEquals(result.getStderr().trim(), "Error: '-123' is not a valid serial number");
		//Assert.assertEquals(result.getStdout().trim(), "");
		// stderr moved to stdout by Bug 867766 - [RFE] unsubscribe from multiple entitlement certificates using serial numbers 
		Assert.assertEquals(result.getStdout().trim(), String.format("Error: '%s' is not a valid serial number",serial),"Stdout");
		Assert.assertEquals(result.getStderr().trim(), "","Stderr");
		
		List<BigInteger> serials = Arrays.asList(new BigInteger[]{BigInteger.valueOf(123),BigInteger.valueOf(-456),BigInteger.valueOf(789)});
		result = clienttasks.unsubscribe_(null, serials, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "Asserting exit code when attempting to unsubscribe from an invalid serial number.");
		Assert.assertEquals(result.getStdout().trim(), String.format("Error: '%s' is not a valid serial number",serials.get(1)),"Stdout");
		Assert.assertEquals(result.getStderr().trim(), "", "Stderr");
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36597", "RHEL7-51402"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Verify the feedback after unsubscribing from all consumed subscriptions using unsubscribe --all",
			groups={"Tier2Tests","blockedByBug-812388","blockedByBug-844455"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsubscribeFromAll() {
	
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null, true, null, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();
		int numberSubscriptionsConsumed = clienttasks.getCurrentEntitlementCertFiles().size();
		
		// unsubscribe from all and assert # subscriptions are unsubscribed
		SSHCommandResult result = clienttasks.unsubscribe(true, (BigInteger)null, null, null, null, null, null);
		//Assert.assertEquals(result.getStdout().trim(), String.format("This machine has been unsubscribed from %s subscriptions",pools.size()),"Expected feedback when unsubscribing from all the currently consumed subscriptions.");	// 10/18/2013 NOT SURE WHAT COMMIT/BUG CAUSED THIS CHANGE TO THE FOLLOWING...
		Assert.assertEquals(result.getStdout().trim(), String.format("%s subscriptions removed at the server."+"\n"+"%s local certificates have been deleted.",numberSubscriptionsConsumed,numberSubscriptionsConsumed),"Expected feedback when unsubscribing from all the currently consumed subscriptions.");
		
		// now attempt to unsubscribe from all again and assert 0 subscriptions are unsubscribed
		result = clienttasks.unsubscribe(true, (BigInteger)null, null, null, null, null, null);
		//Assert.assertEquals(result.getStdout().trim(), String.format("This machine has been unsubscribed from %s subscriptions",0),"Expected feedback when unsubscribing from all when no subscriptions are currently consumed.");	// 10/18/2013 NOT SURE WHAT COMMIT/BUG CAUSED THIS CHANGE TO THE FOLLOWING...
		Assert.assertEquals(result.getStdout().trim(), String.format("%s subscriptions removed at the server.",0),"Expected feedback when unsubscribing from all when no subscriptions are currently consumed.");
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36596", "RHEL7-51401"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Verify the feedback after unsubscribing from all consumed subscriptions using unsubscribe --serial SERIAL1 --serial SERIAL2 --serial SERIAL3 etc.",
			groups={"Tier2Tests","blockedByBug-867766"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsubscribeFromAllSerials() throws Exception {
	
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null, true, false, null, null, null, null);
		List<SubscriptionPool> pools = clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();
		if (pools.isEmpty()) throw new SkipException("This test requires multiple available pools.");
		
		// unsubscribe from all serials in one call and assert the feedback
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		String expectedStdoutMsgLabel;
		expectedStdoutMsgLabel = "Successfully unsubscribed serial numbers:";
		expectedStdoutMsgLabel = "Successfully removed serial numbers:";	// changed by bug 874749
		expectedStdoutMsgLabel = "Serial numbers successfully removed at the server:";	// changed by bug 895447 subscription-manager commit 8e10e76fb5951e0b5d6c867c6c7209d8ec80dead
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.8-1")) { // commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server
			expectedStdoutMsgLabel = "The entitlement server successfully removed these serial numbers:";
		}
		String expectedStdoutMsg = expectedStdoutMsgLabel;
		for (ProductSubscription productSubscription : productSubscriptions) expectedStdoutMsg+="\n   "+productSubscription.serialNumber;
		SSHCommandResult result = clienttasks.unsubscribeFromTheCurrentlyConsumedProductSubscriptionSerialsCollectively();
		//	201512041150:31.105 - FINE: ssh root@jsefler-6.usersys.redhat.com subscription-manager unsubscribe --serial=4651043328648416651 --serial=2129955862705896392 --serial=6036987963107037829 --serial=6517055606227995394 --serial=2413511261042915625 --serial=926011418395876185 --serial=8453228344236558779 --serial=5066796542261309304 --serial=3293732479098570905 --serial=5415112579595928189 --serial=9157726701938581232 --serial=6182970514936389843 --serial=2260599158127862401 --serial=4271127926386632804 --serial=5634936969630756863 --serial=7207476634289399667 --serial=2228491462979865207 --serial=2386453852224823924 --serial=7223061298534444815 --serial=5297175478219818300 --serial=3982095574288257352 --serial=5040485111524274578 --serial=3366381999267298553 --serial=2205964533240272738 --serial=2009203577428614683 --serial=2151906114081820015 --serial=3936210531886609574 --serial=4672997312714367813 --serial=7537851805340427070 --serial=4060809784799304042 --serial=3484999197148046565 --serial=803523513343380611 --serial=7191534188916803249 --serial=1978078461069342056 --serial=3112667572957043871 --serial=8477444878208748895 --serial=4389608640436010689 --serial=2216833362015010963 --serial=1813156641027923146 --serial=6235363562528759967 --serial=8254500161982005905 --serial=8635854683836611062 --serial=2837535867304046524 --serial=6672137066369518129 --serial=6702191060241838270 --serial=695479724906271580 (com.redhat.qe.tools.SSHCommandRunner.run)
		//	201512041151:20.262 - FINE: Stdout: 
		//	Serial numbers successfully removed at the server:
		//	   4651043328648416651
		//	   2129955862705896392
		//	   6036987963107037829
		//	   6517055606227995394
		//	   2413511261042915625
		//	   926011418395876185
		//	   8453228344236558779
		//	   5066796542261309304
		//	   3293732479098570905
		//	   5415112579595928189
		//	   9157726701938581232
		//	   6182970514936389843
		//	   2260599158127862401
		//	   4271127926386632804
		//	   5634936969630756863
		//	   7207476634289399667
		//	   2228491462979865207
		//	   2386453852224823924
		//	   7223061298534444815
		//	   5297175478219818300
		//	   3982095574288257352
		//	   5040485111524274578
		//	   3366381999267298553
		//	   2205964533240272738
		//	   2009203577428614683
		//	   2151906114081820015
		//	   3936210531886609574
		//	   4672997312714367813
		//	   7537851805340427070
		//	   4060809784799304042
		//	   3484999197148046565
		//	   803523513343380611
		//	   7191534188916803249
		//	   1978078461069342056
		//	   3112667572957043871
		//	   8477444878208748895
		//	   4389608640436010689
		//	   2216833362015010963
		//	   1813156641027923146
		//	   6235363562528759967
		//	   8254500161982005905
		//	   8635854683836611062
		//	   2837535867304046524
		//	   6672137066369518129
		//	   6702191060241838270
		//	   695479724906271580
		//	46 local certificates have been deleted.
		//	201512041151:20.283 - FINE: Stderr: 
		String actualStdoutMsg = result.getStdout().trim();
		actualStdoutMsg = clienttasks.workaroundForBug906550(actualStdoutMsg);
		
		// NOTE: This expectedStdoutMsg makes a huge assumption about the order of the unsubscribed serial numbers printed to stdout
		// NOTE: TIME TO FIX THIS ASSERTION... Assert.assertEquals(result.getStdout().trim(), expectedStdoutMsg, "Stdout feedback when unsubscribing from all the currently consumed subscriptions.");
		List<String> expectedStdoutMsgAsList = new ArrayList<String>(Arrays.asList(expectedStdoutMsg.split("\n"))); expectedStdoutMsgAsList.remove(expectedStdoutMsgLabel);
		List<String> actualStdoutMsgAsList = new ArrayList<String>(Arrays.asList(actualStdoutMsg.split("\n"))); actualStdoutMsgAsList.remove(expectedStdoutMsgLabel);
		Assert.assertTrue(expectedStdoutMsgAsList.containsAll(actualStdoutMsgAsList) && actualStdoutMsgAsList.containsAll(expectedStdoutMsgAsList), "Stdout feedback when unsubscribing from all the currently consumed subscriptions contains all the expected serial numbers:"+expectedStdoutMsg.replace(expectedStdoutMsgLabel, ""));
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36595", "RHEL7-51400"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Verify the feedback after unsubscribing from all consumed subscriptions (including revoked serials) using unsubscribe --serial SERIAL1 --serial SERIAL2 --serial SERIAL3 etc.",
			groups={"Tier2Tests","blockedByBug-867766"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsubscribeFromAllSerialsIncludingRevokedSerials() throws JSONException, Exception {
	
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true,false,null,null,null, null);
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("This test requires multiple available pools.");
		
		// which poolIds are modifiers?
		Set<String> modifierPoolIds = new HashSet<String>();
		for (SubscriptionPool subscriptionPool : pools) if (CandlepinTasks.isPoolAModifier(sm_clientUsername,sm_clientPassword, subscriptionPool.poolId, sm_serverUrl)) modifierPoolIds.add(subscriptionPool.poolId);
		
		// subscribe to all of the available pools
		List<String> poolIds = new ArrayList<String>();
		for (SubscriptionPool pool : pools) poolIds.add(pool.poolId);
		clienttasks.subscribe(null, null, poolIds, null, null, null, null, null, null, null, null, null, null);
		
		// prepare a list of currently consumed serials that we can use to collectively unsubscribe from
		List<BigInteger> serials = new ArrayList<BigInteger>();
		for(ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			// insert modifiers at the head of the serials list so their removal won't cause a cert regeneration
			if (!productSubscription.poolId.equals("Unknown")/*indicative of an older candlepin*/ && modifierPoolIds.contains(productSubscription.poolId))
				serials.add(0,productSubscription.serialNumber);
			else
				serials.add(productSubscription.serialNumber);
		}
		
		// unsubscribe from all serials in one call and assert the feedback;
		SSHCommandResult result = clienttasks.unsubscribe(null,serials,null,null,null, null, null);
		String actualStdoutMsg = result.getStdout().trim();
		actualStdoutMsg = clienttasks.workaroundForBug906550(actualStdoutMsg);
		String expectedStdoutMsg;
		expectedStdoutMsg = "Successfully unsubscribed serial numbers:";	// changed by bug 874749
		expectedStdoutMsg = "Successfully removed serial numbers:";
		expectedStdoutMsg = "Serial numbers successfully removed at the server:";	// changed by bug 895447 subscription-manager commit 8e10e76fb5951e0b5d6c867c6c7209d8ec80dead
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.8-1")) expectedStdoutMsg = "The entitlement server successfully removed these serial numbers:";	// commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server
		for (BigInteger serial : serials) expectedStdoutMsg+="\n   "+serial;	// NOTE: This expectedStdoutMsg makes a huge assumption about the order of the unsubscribed serial numbers printed to stdout
		Assert.assertEquals(actualStdoutMsg, expectedStdoutMsg, "Stdout feedback when unsubscribing from all the currently consumed subscriptions.");
		
		// remember the unsubscribed serials as revoked serials
		List<BigInteger> revokedSerials = new ArrayList<BigInteger>();
		for (BigInteger serial : serials) revokedSerials.add(serial);
		
		// re-subscribe to all the available pools again
		clienttasks.subscribe(null, null, poolIds, null, null, null, null, null, null, null, null, null, null);
		
		// re-prepare a list of currently consumed serials that we can use to collectively unsubscribe from
		// include the revokedSerials by interleaving them into the currently consumed serials
		serials.clear();
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		for (int i=0; i<productSubscriptions.size(); i++) {
			ProductSubscription productSubscription = productSubscriptions.get(i);
			// insert modifiers at the head of the serials list so their removal won't cause a cert regeneration
			if (!productSubscription.poolId.equals("Unknown")/*indicative of an older candlepin*/ && modifierPoolIds.contains(productSubscription.poolId))
				serials.add(0,productSubscription.serialNumber);
			else
				serials.add(productSubscription.serialNumber);
			
			// interleave the former revokedSerials amongst the serials
			serials.add(revokedSerials.get(i));
		}
		
		// now attempt to unsubscribe from both the current serials AND the previously consumed serials in one call and assert the feedback
		result = clienttasks.unsubscribe(null,serials,null,null,null, null, null);
		actualStdoutMsg = result.getStdout().trim();
		actualStdoutMsg = clienttasks.workaroundForBug906550(actualStdoutMsg);
		expectedStdoutMsg = "Successfully unsubscribed serial numbers:";	// added by bug 867766	// changed by bug 874749
		expectedStdoutMsg = "Successfully removed serial numbers:";
		expectedStdoutMsg = "Serial numbers successfully removed at the server:";	// changed by bug 895447 subscription-manager commit 8e10e76fb5951e0b5d6c867c6c7209d8ec80dead
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.8-1")) expectedStdoutMsg = "The entitlement server successfully removed these serial numbers:";	// commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server
		for (BigInteger serial : serials) if (!revokedSerials.contains(serial)) expectedStdoutMsg+="\n   "+serial;	// NOTE: This expectedStdoutMsg makes a huge assumption about the order of the unsubscribed serial numbers printed to stdout
		expectedStdoutMsg +="\n";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.8-1")) {
			expectedStdoutMsg += "The entitlement server failed to remove these serial numbers:";	// commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server
		}
		else {
			//expectedStdoutMsg += "Unsuccessfully unsubscribed serial numbers:";	// added by bug 867766	// changed by bug 874749
			//expectedStdoutMsg += "Unsuccessfully removed serial numbers:";
			expectedStdoutMsg += "Serial numbers unsuccessfully removed at the server:";	// changed by bug 895447 subscription-manager commit 8e10e76fb5951e0b5d6c867c6c7209d8ec80dead		
		}
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.16.6-1")) {	// commit 0d80caacf5e9483d4f10424030d6a5b6f472ed88 1285004: Adds check for access to the required manager capabilty
			for (BigInteger revokedSerial : revokedSerials) expectedStdoutMsg+="\n   "+revokedSerial;	// NOTE: This expectedStdoutMsg makes a huge assumption about the order of the unsubscribed serial numbers printed to stdout			
		} else {
			//for (BigInteger revokedSerial : revokedSerials) expectedStdoutMsg+="\n   "+String.format("Entitlement Certificate with serial number %s could not be found.", revokedSerial);	// NOTE: This expectedStdoutMsg makes a huge assumption about the order of the unsubscribed serial numbers printed to stdout
			for (BigInteger revokedSerial : revokedSerials) expectedStdoutMsg+="\n   "+String.format("Entitlement Certificate with serial number '%s' could not be found.", revokedSerial);	// NOTE: This expectedStdoutMsg makes a huge assumption about the order of the unsubscribed serial numbers printed to stdout			
		}
		Assert.assertEquals(actualStdoutMsg, expectedStdoutMsg, "Stdout feedback when unsubscribing from all the currently consumed subscriptions (including revoked serials).");
	}
//TOO MUCH LOGGING FROM TOO MANY ASSERTIONS;  DELETEME IF ABOVE TEST WORKS WELL
//	public void UnsubscribeFromAllSerialsIncludingRevokedSerials_Test() {
//		
//		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,true, null, null, null, null);
//		List<SubscriptionPool> pools = clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();
//		if (pools.isEmpty()) throw new SkipException("This test requires multiple available pools.");
//		
//		// unsubscribe from all serials in one call and assert the feedback
//		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
//		SSHCommandResult result = clienttasks.unsubscribeFromTheCurrentlyConsumedProductSubscriptionsCollectively();
//		String expectedStdoutMsg = "Successfully unsubscribed serial numbers:";
//		for (ProductSubscription productSubscription : productSubscriptions) expectedStdoutMsg+="\n   "+productSubscription.serialNumber;	// NOTE: This expectedStdoutMsg makes a huge assumption about the order of the unsubscribed serial numbers printed to stdout
//		Assert.assertEquals(result.getStdout().trim(), expectedStdoutMsg, "Stdout feedback when unsubscribing from all the currently consumed subscriptions.");
//
//		List<BigInteger> revokedSerials = new ArrayList<BigInteger>();
//		for (ProductSubscription productSubscription : productSubscriptions) revokedSerials.add(productSubscription.serialNumber);
//		
//		// subscribe to all the available pools again
//		clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();
//		
//		// now attempt to unsubscribe from both the current serials AND the previously consumed serials in one call and assert the feedback
//		productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
//		List<BigInteger> serials = new ArrayList<BigInteger>();
//		for (int i=0; i<productSubscriptions.size(); i++) {
//			serials.add(productSubscriptions.get(i).serialNumber);
//			serials.add(revokedSerials.get(i));
//		}
//		expectedStdoutMsg = "Successfully unsubscribed serial numbers:";
//		for (ProductSubscription productSubscription : productSubscriptions) expectedStdoutMsg+="\n   "+productSubscription.serialNumber;	// NOTE: This expectedStdoutMsg makes a huge assumption about the order of the unsubscribed serial numbers printed to stdout
//		expectedStdoutMsg +="\n";
//		expectedStdoutMsg += "Unsuccessfully unsubscribed serial numbers:";
//		for (BigInteger revokedSerial : revokedSerials) expectedStdoutMsg+="\n   "+String.format("Entitlement Certificate with serial number %s could not be found.", revokedSerial);	// NOTE: This expectedStdoutMsg makes a huge assumption about the order of the unsubscribed serial numbers printed to stdout
//		result = clienttasks.unsubscribe(false,serials,null,null,null);
//		Assert.assertEquals(result.getStdout().trim(), expectedStdoutMsg, "Stdout feedback when unsubscribing from all the currently consumed subscriptions (including revoked serials).");
//	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36592", "RHEL7-51396"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscription-manager: unsubscribe and remove can be used interchangably",
			groups={"Tier2Tests","blockedByBug-874749"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void testRemoveDeprecatesUnsubscribe() throws Exception {
		SSHCommandResult result = client.runCommandAndWait(clienttasks.command+" --help");
		Assert.assertContainsMatch(result.getStdout(), "^\\s*unsubscribe\\s+Deprecated, see remove$");
		
		SSHCommandResult unsubscribeResult;
		SSHCommandResult removeResult;
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		unsubscribeResult = client.runCommandAndWait(clienttasks.command+" unsubscribe --serial=123");
		removeResult = client.runCommandAndWait(clienttasks.command+" remove --serial=123");
		Assert.assertEquals(unsubscribeResult.toString(), removeResult.toString(), "Results from 'unsubscribe' and 'remove' module commands should be identical.");
		unsubscribeResult = client.runCommandAndWait(clienttasks.command+" unsubscribe --all");
		removeResult = client.runCommandAndWait(clienttasks.command+" remove --all");
		Assert.assertEquals(unsubscribeResult.toString(), removeResult.toString(), "Results from 'unsubscribe' and 'remove' module commands should be identical.");

		clienttasks.unregister(null,null,null, null);
		unsubscribeResult = client.runCommandAndWait(clienttasks.command+" unsubscribe --serial=123");
		removeResult = client.runCommandAndWait(clienttasks.command+" remove --serial=123");
		Assert.assertEquals(unsubscribeResult.toString(), removeResult.toString(), "Results from 'unsubscribe' and 'remove' module commands should be identical.");
		unsubscribeResult = client.runCommandAndWait(clienttasks.command+" unsubscribe --all");
		removeResult = client.runCommandAndWait(clienttasks.command+" remove --all");
		Assert.assertEquals(unsubscribeResult.toString(), removeResult.toString(), "Results from 'unsubscribe' and 'remove' module commands should be identical.");
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36591", "RHEL7-51395"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="after attaching many subscriptions (in a different order) to two different consumers, call unsubscribe --all on each consumer",
			groups={"Tier2Tests","blockedByBug-1095939"})
			//@ImplementsNitrateTest(caseId=)
	public void testMultiClientAttemptToDeadLockOnUnsubscribeAll() throws JSONException, Exception {
		if (client2tasks==null) throw new SkipException("This multi-client test requires a second client.");
		
		// register two clients
		String client1ConsumerId = client1tasks.getCurrentConsumerId(client1tasks.register(sm_client1Username, sm_client1Password, sm_client1Org, null, null, null, null, null, null, null, (List<String>)null, null, null, null, true, false, null, null, null, null));
		String client2ConsumerId = client2tasks.getCurrentConsumerId(client2tasks.register(sm_client2Username, sm_client2Password, sm_client2Org, null, null, null, null, null, null, null, (List<String>)null, null, null, null, true, false, null, null, null, null));
		String client1OwnerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_client1Username, sm_client1Password, sm_serverUrl, client1ConsumerId);
		String client2OwnerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_client2Username, sm_client2Password, sm_serverUrl, client2ConsumerId);
		if (!client1OwnerKey.equals(client2OwnerKey)) throw new SkipException("This multi-client test requires that both client registerers belong to the same owner. (client1: username="+sm_client1Username+" ownerkey="+client1OwnerKey+") (client2: username="+sm_client2Username+" ownerkey="+client2OwnerKey+")");
		
		// get a list of all of the available pools for each client
		List<SubscriptionPool> client1pools = client1tasks.getCurrentlyAllAvailableSubscriptionPools();
		List<SubscriptionPool> client2pools = client2tasks.getCurrentlyAllAvailableSubscriptionPools();
		
		// attempt this test more than once
		for (int attempt=1; attempt<5; attempt++) {
			
			// subscribe each client to each of the pools in a different order (this is crucial)
			List<String> client1poolIds = new ArrayList<String>();
			List<String> client2poolIds = new ArrayList<String>();
			for (SubscriptionPool pool : /*getRandomList(*/client1pools/*)*/) client1poolIds.add(pool.poolId);
			for (SubscriptionPool pool : /*getRandomList(*/client2pools/*)*/) client2poolIds.add(0,pool.poolId);
			client1tasks.subscribe_(null, null, client1poolIds, null, null, null, null, null, null, null, null, null, null);
			client2tasks.subscribe_(null, null, client2poolIds, null, null, null, null, null, null, null, null, null, null);
			
			// unsubscribe from all subscriptions on each client simultaneously
			log.info("Simultaneously attempting to unsubscribe all on '"+client1tasks.hostname+"' and '"+client2tasks.hostname+"'...");
			client1.runCommand/*AndWait*/(client1tasks.unsubscribeCommand(true, null, null, null, null, null, null), TestRecords.action());
			client2.runCommand/*AndWait*/(client2tasks.unsubscribeCommand(true, null, null, null, null, null, null), TestRecords.action());
			client1.waitForWithTimeout(new Long(10*60*1000)); // timeout after 10 min
			client2.waitForWithTimeout(new Long(10*60*1000)); // timeout after 10 min
			SSHCommandResult client1Result = client1.getSSHCommandResult();
			SSHCommandResult client2Result = client2.getSSHCommandResult();
			//	201405091632:43.313 - INFO: SSHCommandResult from an attempt to unsubscribe all on 'jsefler-7server.usersys.redhat.com': 
			//		exitCode=255
			//		stdout=''
			//		stderr='Runtime Error ERROR: deadlock detected
			//		  Detail: Process 3247 waits for ShareLock on transaction 45358106; blocked by process 3221.
			//		Process 3221 waits for ShareLock on transaction 45358105; blocked by process 3247.
			//		  Hint: See server log for query details. at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse:2,102'
			
			// assert the results
			log.info("SSHCommandResult from an attempt to unsubscribe all on '"+client1tasks.hostname+"': \n"+client1Result);
			log.info("SSHCommandResult from an attempt to unsubscribe all on '"+client2tasks.hostname+"': \n"+client2Result);
			Assert.assertEquals(client1Result.getExitCode(), Integer.valueOf(0), "The exit code from the unsubscribe all command on '"+client1tasks.hostname+"'.");
			Assert.assertEquals(client1Result.getStderr(), "", "Stderr from the unsubscribe all on '"+client1tasks.hostname+"'.");
			Assert.assertEquals(client2Result.getExitCode(), Integer.valueOf(0), "The exit code from the unsubscribe all command on '"+client2tasks.hostname+"'.");
			Assert.assertEquals(client2Result.getStderr(), "", "Stderr from the unsubscribe all on '"+client2tasks.hostname+"'.");
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47923", "RHEL7-51407"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Attempt to unsubscribe from a pool id when not registered",
			groups={"Tier1Tests","blockedByBug-1198178"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsubscribeFromPoolIdWhenNotRegistered() {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.16.5-1")) throw new SkipException("The unsubscribe --pool function was not implemented in this version of subscription-manager.  See RFE Bug 1198178");
	
		// first make sure we are subscribed to a pool
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null, true, false, null, null, null, null);
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size()));	// random available pool
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(clienttasks.subscribeToSubscriptionPool(pool,/*sm_serverAdminUsername*/sm_clientUsername,/*sm_serverAdminPassword*/sm_clientPassword,sm_serverUrl));
		
		// now remove the consumer cert to simulate an unregister
		clienttasks.removeAllCerts(true,false, false);
		SSHCommandResult identityResult = clienttasks.identity_(null,null,null,null,null,null,null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) { // post commit 5697e3af094be921ade01e19e1dfe7b548fb7d5b bug 1119688
			Assert.assertEquals(identityResult.getStderr().trim(),clienttasks.msg_ConsumerNotRegistered, "stderr");
		} else {
			Assert.assertEquals(identityResult.getStdout().trim(),clienttasks.msg_ConsumerNotRegistered, "stdout");
		}
		
		// now unsubscribe from the pool number (while not registered)
		Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().size()>0, "We should be consuming an entitlement (even while not registered)");
		SSHCommandResult result = clienttasks.unsubscribe_(null,null,pool.poolId,null,null,null, null);
		if (servertasks.statusCapabilities.contains("remove_by_pool_id")) {
			Integer expectedExitCode = new Integer(0);
			Assert.assertEquals(result.getExitCode(), expectedExitCode, "Asserting exit code when attempting to unsubscribe from a valid pool (while not registered).");
			Assert.assertEquals(result.getStdout().trim(), "Subscription with serial number "+entitlementCert.serialNumber+" removed from this system", "We should always be able to remove a subscription (even while not registered).");
			Assert.assertEquals(clienttasks.getCurrentlyConsumedProductSubscriptions().size(), 0, "We should not be consuming any entitlements after unsubscribing (while not registered).");
		} else {	// coverage for Bug 1285004 - subscription-manager remove --pool throws: Runtime Error Could not find resource for relative of full path
			Integer expectedExitCode = new Integer(69);
			Assert.assertEquals(result.getExitCode(), expectedExitCode, "Asserting exit code when attempting to unsubscribe from a valid pool (while not registered) (from an incapable candlepin server).");
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.16.7-1")) { // d9a82d3135a1770f794c2c8181f44e7e4628e0b6 Output of errors now goes to stderr
				Assert.assertEquals(result.getStderr().trim(), "Error: The registered entitlement server does not support remove --pool.\nInstead, use the remove --serial option.","Stderr");
				Assert.assertEquals(result.getStdout().trim(), "","Stdout");
			} else {
				Assert.assertEquals(result.getStdout().trim(), "Error: The registered entitlement server does not support remove --pool.\nInstead, use the remove --serial option.","Stdout");
				Assert.assertEquals(result.getStderr().trim(), "","Stderr");
			}
			Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().size()>0, "After attempts to remove by pool ID against an incapable candlepin should still be consuming an entitlement (even while not registered)");
			throw new SkipException("The registered entitlement server does not support remove --pool");
		}
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47924", "RHEL7-51406"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Attempt to unsubscribe from a valid pool id",
			groups={"Tier1Tests","blockedByBug-1198178"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsubscribeFromPool() {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.16.5-1")) throw new SkipException("The unsubscribe --pool function was not implemented in this version of subscription-manager.  See RFE Bug 1198178");
		
		// register and get available pools
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null, true, false, null, null, null, null);
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		
		// find available multi-entitlement (Stackable) pools
		pools = SubscriptionPool.findAllInstancesWithMatchingFieldFromList("subscriptionType", "Stackable", pools);
		pools = getRandomSubsetOfList(pools, 2);
		List<String> poolIds = new ArrayList<String>();
		
		// attach multiple serials per pool
		for (SubscriptionPool pool : pools) {
			clienttasks.subscribe(null,null,pool.poolId,null,null,null,null,null,null,null,null,null, null);
			clienttasks.subscribe(null,null,pool.poolId,null,null,null,null,null,null,null,null,null, null);
			poolIds.add(pool.poolId);
		}
		
		// choose a poolId and get serials from that pool
		SubscriptionPool pool = pools.get(0);
		List<String> serials = new ArrayList<String>();
		for (ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			if (productSubscription.poolId.equals(pool.poolId)) {
				serials.add(productSubscription.serialNumber.toString());
			}			
		}
		
		// unsubscribe from poolId
		SSHCommandResult result = clienttasks.unsubscribe_(null, null, pool.poolId, null, null, null, null);
		//	201512041602:26.874 - FINE: ssh root@jsefler-6.usersys.redhat.com subscription-manager unsubscribe --pool=8a908790516a011001516a02646b06c3 (com.redhat.qe.tools.SSHCommandRunner.run)
		//	201512041602:29.304 - FINE: Stdout: 
		//	Pools successfully removed at the server:
		//	   8a908790516a011001516a02646b06c3
		//	Serial numbers successfully removed at the server:
		//	   4311923290144349918
		//	   4255296379649237918
		//	2 local certificates have been deleted.
		//	201512041602:29.310 - FINE: Stderr: 
		//	201512041602:29.313 - FINE: ExitCode: 0
		
		if (!servertasks.statusCapabilities.contains("remove_by_pool_id")) {
			Integer expectedExitCode = new Integer(69);
			Assert.assertEquals(result.getExitCode(), expectedExitCode, "Asserting exit code when attempting to unsubscribe from an invalid pool id (from an incapable server).");
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.16.7-1")) { // d9a82d3135a1770f794c2c8181f44e7e4628e0b6 Output of errors now goes to stderr
				Assert.assertEquals(result.getStderr().trim(), "Error: The registered entitlement server does not support remove --pool.\nInstead, use the remove --serial option.","Stderr");
				Assert.assertEquals(result.getStdout().trim(), "","Stdout");
			} else {
				Assert.assertEquals(result.getStdout().trim(), "Error: The registered entitlement server does not support remove --pool.\nInstead, use the remove --serial option.","Stdout");
				Assert.assertEquals(result.getStderr().trim(), "","Stderr");
			}
			throw new SkipException("The registered entitlement server does not support remove --pool");
		}
		
		Integer expectedExitCode = new Integer(0);
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "Exit code when attempting to unsubscribe from a valid pool id.");
		//String expectedStdout = String.format("Pools successfully removed at the server:\n   %s\nSerial numbers successfully removed at the server:\n   %s\n   %s\n%d local certificates have been deleted.", pool.poolId, serials.get(0), serials.get(1), serials.size());
		//Assert.assertEquals(result.getStdout().trim(),expectedStdout,"Stdout when attempting to unsubscribe from a valid pool id.");
		String expectedStdoutRegex = String.format("Pools successfully removed at the server:\n   %s\nSerial numbers successfully removed at the server:\n   %s\n   %s\n%d local certificates have been deleted.", pool.poolId, "("+serials.get(0)+"|"+serials.get(1)+")", "("+serials.get(0)+"|"+serials.get(1)+")", serials.size());
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.8-1")) expectedStdoutRegex = String.format("The entitlement server successfully removed these pools:\n   %s\nThe entitlement server successfully removed these serial numbers:\n   %s\n   %s\n%d local certificates have been deleted.", pool.poolId, "("+serials.get(0)+"|"+serials.get(1)+")", "("+serials.get(0)+"|"+serials.get(1)+")", serials.size());	// commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server
		Assert.assertMatch(result.getStdout().trim(), expectedStdoutRegex);
		Assert.assertEquals(result.getStderr(), "", "Stderr when attempting to unsubscribe from a valid pool id.");
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36363", "RHEL7-51405"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Attempt to unsubscribe from an unknown pool id",
			groups={"Tier1Tests","blockedByBug-1198178","blockedByBug-1298586"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsubscribeFromAnUnknownPoolId() {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.16.5-1")) throw new SkipException("The unsubscribe --pool function was not implemented in this version of subscription-manager.  See RFE Bug 1198178");
		
		// register
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null, true, false, null, null, null, null);
		
		String poolId = "1234567890abcdef1234567890abcdef";
		SSHCommandResult result = clienttasks.unsubscribe_(null, null, poolId, null, null, null, null);
		//	[root@jsefler-6 ~]# subscription-manager unsubscribe --pool=1234567890abcdef1234567890abcdef
		//	Pools unsuccessfully removed at the server:
		//	   1234567890abcdef1234567890abcdef
		
		if (servertasks.statusCapabilities.contains("remove_by_pool_id")) {
			Integer expectedExitCode = new Integer(1);
			String expectedStdout = "";
			expectedStdout = String.format("Pools unsuccessfully removed at the server:\n   %s", poolId);	// commit e418b77ce2a7389e310ac341a6beb46cb7eb3d0f	// Bug 1298586: Message needed for remove only invalid pool
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.8-1")) { // commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server
				expectedStdout = String.format("The entitlement server failed to remove these pools:\n   %s", poolId);
			}
			Assert.assertEquals(result.getExitCode(), expectedExitCode, "Asserting exit code when attempting to unsubscribe from an unknown pool id.");
			Assert.assertEquals(result.getStdout().trim(), expectedStdout,"Stdout");
			Assert.assertEquals(result.getStderr().trim(), "","Stderr");
		} else {	// coverage for Bug 1285004 - subscription-manager remove --pool throws: Runtime Error Could not find resource for relative of full path
			Integer expectedExitCode = new Integer(69);
			Assert.assertEquals(result.getExitCode(), expectedExitCode, "Asserting exit code when attempting to unsubscribe from an invalid pool id (from an incapable server).");
			if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.16.7-1")) { // d9a82d3135a1770f794c2c8181f44e7e4628e0b6 Output of errors now goes to stderr
				Assert.assertEquals(result.getStderr().trim(), "Error: The registered entitlement server does not support remove --pool.\nInstead, use the remove --serial option.","Stderr");
				Assert.assertEquals(result.getStdout().trim(), "","Stdout");
			} else {
				Assert.assertEquals(result.getStdout().trim(), "Error: The registered entitlement server does not support remove --pool.\nInstead, use the remove --serial option.","Stdout");
				Assert.assertEquals(result.getStderr().trim(), "","Stderr");
			}
			throw new SkipException("The registered entitlement server does not support remove --pool");
		}
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36594", "RHEL7-51399"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Verify the feedback after unsubscribing from all consumed subscriptions using unsubscribe --pool POOLID1 --pool POOLID2 --pool POOLID3 etc.",
			groups={"Tier2Tests","blockedByBug-1198178","blockedByBug-1288626"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsubscribeFromAllPoolIds() throws Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.16.5-1")) throw new SkipException("The unsubscribe --pool function was not implemented in this version of subscription-manager.  See RFE Bug 1198178");
		if (!servertasks.statusCapabilities.contains("remove_by_pool_id")) throw new SkipException("The registered entitlement server does not support remove --pool");
		
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null, true, false, null, null, null, null);
		List<SubscriptionPool> pools = clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();
		if (pools.isEmpty()) throw new SkipException("This test requires multiple available pools.");
		List<String> poolIds = new ArrayList<String>(); for (SubscriptionPool pool : pools) poolIds.add(pool.poolId);
		
		// add more serials from multi-entitlement pools (ignoring failures)
		clienttasks.subscribe_(null, null, poolIds, null, null, null, null, null, null, null, null, null, null);
		
		// unsubscribe from all pool in one call and assert the feedback
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		String successfulStdoutSerialsMsgLabel;
		successfulStdoutSerialsMsgLabel = "Successfully unsubscribed serial numbers:";
		successfulStdoutSerialsMsgLabel = "Successfully removed serial numbers:";	// changed by bug 874749
		successfulStdoutSerialsMsgLabel = "Serial numbers successfully removed at the server:";	// changed by bug 895447 subscription-manager commit 8e10e76fb5951e0b5d6c867c6c7209d8ec80dead
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.8-1")) successfulStdoutSerialsMsgLabel = "The entitlement server successfully removed these serial numbers:";	// commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server
		String successfulStdoutPoolIdsMsgLabel;
		successfulStdoutPoolIdsMsgLabel = "Pools successfully removed at the server:";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.8-1")) successfulStdoutPoolIdsMsgLabel = "The entitlement server successfully removed these pools:";	// commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server

		String unsuccessfulStdoutSerialsMsgLabel;
		unsuccessfulStdoutSerialsMsgLabel = "Serial numbers unsuccessfully removed at the server:";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.8-1")) unsuccessfulStdoutSerialsMsgLabel = "The entitlement server failed to remove these serial numbers:";	// commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server
		String unsuccessfulStdoutPoolsMsgLabel;
		unsuccessfulStdoutPoolsMsgLabel = "Pools unsuccessfully removed at the server:";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.8-1")) unsuccessfulStdoutPoolsMsgLabel = "The entitlement server failed to remove these pools:";	// commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server
		
		Set<String>expectedPoolIds = new HashSet<String>();
		Set<String>expectedSerials = new HashSet<String>();
		for (ProductSubscription productSubscription : productSubscriptions) {
			expectedPoolIds.add(productSubscription.poolId);
			expectedSerials.add(productSubscription.serialNumber.toString());
		}
		
		SSHCommandResult result = clienttasks.unsubscribeFromTheCurrentlyConsumedProductSubscriptionPoolIdsCollectively();
		//	201511231656:37.570 - FINE: ssh root@jsefler-6.usersys.redhat.com subscription-manager unsubscribe --pool=8a9087905136504801513651940f049a --pool=8a90879051365048015136519c0d055b --pool=8a9087905136504801513651946b04a7 --pool=8a9087905136504801513651ad77079b --pool=8a9087905136504801513651aa620721 --pool=8a9087905136504801513651ac43076c --pool=8a9087905136504801513651bc6e09aa --pool=8a90879051365048015136519a7c0529 --pool=8a9087905136504801513651a1b1061d --pool=8a9087905136504801513651b9a3094c --pool=8a90879051365048015136519b730556 --pool=8a9087905136504801513651ab5f0741 --pool=8a9087905136504801513651a06705ef --pool=8a9087905136504801513651a51b0689 --pool=8a9087905136504801513651ae9907d4 --pool=8a9087905136504801513651ad370790 --pool=8a9087905136504801513651a86906ed --pool=8a9087905136504801513651b7960927 --pool=8a90879051365048015136519c4c056a --pool=8a9087905136504801513651b4cc08cd --pool=8a9087905136504801513651940f0498 --pool=8a9087905136504801513651a2b7063a --pool=8a9087905136504801513651a79306d5 --pool=8a9087905136504801513651ae6007b9 --pool=8a9087905136504801513651a03305d5 --pool=8a9087905136504801513651b7cf0936 --pool=8a9087905136504801513651a74b06c9 --pool=8a9087905136504801513651946504a4 --pool=8a9087905136504801513651ae8c07c2 --pool=8a9087905136504801513651b56908d7 --pool=8a90879051365048015136519c65056e --pool=8a9087905136504801513651ab860744 --pool=8a90879051365048015136519b510553 --pool=8a90879051365048015136519c7e0572 --pool=8a90879051365048015136519c0c055a --pool=8a9087905136504801513651b21f0872 --pool=8a9087905136504801513651a2430630 --pool=8a9087905136504801513651ae6b07bf --pool=8a9087905136504801513651a222062d --pool=8a9087905136504801513651953b04aa --pool=8a9087905136504801513651bb9f0987 --pool=8a9087905136504801513651b4d708cf --pool=8a9087905136504801513651b892093c --pool=8a9087905136504801513651b3b708bb --pool=8a9087905136504801513651bd1b09b2 --pool=8a9087905136504801513651a88b06f6 --pool=8a9087905136504801513651945604a1 --pool=8a9087905136504801513651940f0499 --pool=8a9087905136504801513651b1320855 --pool=8a9087905136504801513651ba510960 (com.redhat.qe.tools.SSHCommandRunner.run)
		//	201511231657:33.946 - FINE: Stdout: 
		//	Pools successfully removed at the server:
		//	   8a9087905136504801513651940f049a
		//	   8a90879051365048015136519c0d055b
		//	   8a9087905136504801513651946b04a7
		//	Serial numbers successfully removed at the server:
		//	   8865613847264050544
		//	   8937932663056862717
		//	   3352861400531049008
		//	   8107962814745856003
		//	   8514271782573332949
		//	   3379403338434026844
		//	   2230666936775183579
		//	7 local certificates have been deleted.
		//	201511231657:33.989 - FINE: Stderr:
		
		//	201512041343:44.111 - FINE: ssh root@jsefler-6.usersys.redhat.com subscription-manager unsubscribe --pool=8a908790516a011001516a02508b049b --pool=8a908790516a011001516a025891054a --pool=8a908790516a011001516a025dbc05ec --pool=8a908790516a011001516a026b970797 --pool=8a908790516a011001516a0250a1049e --pool=8a908790516a011001516a0264a006d3 --pool=8a908790516a011001516a02646b06c3 --pool=8a908790516a011001516a02511e04a4 --pool=8a908790516a011001516a02762e0904 --pool=8a908790516a011001516a0261720680 --pool=8a908790516a011001516a0265b906e1 --pool=8a908790516a011001516a02714d089b --pool=8a908790516a011001516a0261720680 --pool=8a908790516a011001516a0259dd056e --pool=8a908790516a011001516a02588b0547 --pool=8a908790516a011001516a025a490575 --pool=8a908790516a011001516a02646b06c3 --pool=8a908790516a011001516a0263ac06bb --pool=8a908790516a011001516a026168067d --pool=8a908790516a011001516a025e0e05f9 --pool=8a908790516a011001516a027e6b09db --pool=8a908790516a011001516a0251b504aa --pool=8a908790516a011001516a0274dd08e0 --pool=8a908790516a011001516a026919073f --pool=8a908790516a011001516a02662606f4 --pool=8a908790516a011001516a0278a70937 --pool=8a908790516a011001516a027b7f0984 --pool=8a908790516a011001516a0250650498 --pool=8a908790516a011001516a0264a106d6 --pool=8a908790516a011001516a026b030786 --pool=8a908790516a011001516a0278aa0939 --pool=8a908790516a011001516a0265b906e1 --pool=8a908790516a011001516a02714d089b --pool=8a908790516a011001516a025d6005e8 --pool=8a908790516a011001516a0278a70937 --pool=8a908790516a011001516a0258030542 --pool=8a908790516a011001516a0278aa0939 --pool=8a908790516a011001516a02510504a1 --pool=8a908790516a011001516a0264a106d6 --pool=8a908790516a011001516a02762e0904 --pool=8a908790516a011001516a02588b0547 --pool=8a908790516a011001516a027b930986 --pool=8a908790516a011001516a0260e5066a --pool=8a908790516a011001516a0258e20560 --pool=8a908790516a011001516a025a490575 --pool=8a908790516a011001516a025e0e05f9 --pool=8a908790516a011001516a0269940747 --pool=8a908790516a011001516a026acf0781 --pool=8a908790516a011001516a0251b504aa --pool=8a908790516a011001516a026910073c --pool=8a908790516a011001516a0274c608de --pool=8a908790516a011001516a02710c0899 --pool=8a908790516a011001516a02516304a7 --pool=8a908790516a011001516a0269940747 --pool=8a908790516a011001516a0259b60569 --pool=8a908790516a011001516a0274dd08e0 --pool=8a908790516a011001516a0259dd056e --pool=8a908790516a011001516a02516304a7 --pool=8a908790516a011001516a0269bf074b --pool=8a908790516a011001516a0263ac06bb --pool=8a908790516a011001516a0269bf074b --pool=8a908790516a011001516a026b33078f --pool=8a908790516a011001516a026b030786 --pool=8a908790516a011001516a026acf0781 --pool=8a908790516a011001516a026f8d0869 --pool=8a908790516a011001516a0257ab0529 --pool=8a908790516a011001516a025dbc05ec --pool=8a908790516a011001516a0264a006d3 --pool=8a908790516a011001516a027b930986 (com.redhat.qe.tools.SSHCommandRunner.run)
		//	201512041344:51.518 - FINE: Stdout: 
		//	Pools successfully removed at the server:
		//	   8a908790516a011001516a0269bf074b
		//	   8a908790516a011001516a026b33078f
		//	   8a908790516a011001516a026f8d0869
		//	   8a908790516a011001516a0257ab0529
		//	Pools unsuccessfully removed at the server:
		//	   8a908790516a011001516a0261720680
		//	   8a908790516a011001516a02646b06c3
		//	Serial numbers successfully removed at the server:
		//	   6147826158890249653
		//	   2710596911835822689
		//	   8362938208950031094
		//	   130483993634735580
		//	   2732609281506598523
		//	Serial numbers unsuccessfully removed at the server:
		//	   8a908790516a011001516a0261720680   <=== Bug 1288626
		//	   8a908790516a011001516a02646b06c3   <=== Bug 1288626
		//	5 local certificates have been deleted.
		//	201512041344:51.520 - FINE: Stderr:  (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)		
		
		String actualStdoutMsg = result.getStdout().trim();
		actualStdoutMsg = clienttasks.workaroundForBug906550(actualStdoutMsg);
		Assert.assertTrue(actualStdoutMsg.contains(successfulStdoutPoolIdsMsgLabel), "Stdout from unsubscribing many pool ids contains expected label '"+successfulStdoutPoolIdsMsgLabel+"'.");
		Assert.assertTrue(actualStdoutMsg.contains(successfulStdoutSerialsMsgLabel), "Stdout from unsubscribing many pool ids contains expected label '"+successfulStdoutSerialsMsgLabel+"'.");
		
		String actualStdoutPoolIdsMsg = actualStdoutMsg.split(successfulStdoutSerialsMsgLabel)[0]; actualStdoutPoolIdsMsg = actualStdoutPoolIdsMsg.replace(successfulStdoutPoolIdsMsgLabel+"\n", "").replace(unsuccessfulStdoutPoolsMsgLabel+"\n", "");
		String actualStdoutSerialsMsg = actualStdoutMsg.split(successfulStdoutSerialsMsgLabel)[1]; actualStdoutSerialsMsg = actualStdoutSerialsMsg.replace(successfulStdoutSerialsMsgLabel+"\n", "").replace(unsuccessfulStdoutSerialsMsgLabel+"\n", "");
		
		Set<String> actualStdoutPoolIds = new HashSet<String>();
		actualStdoutPoolIds.addAll(Arrays.asList(actualStdoutPoolIdsMsg.trim().split("\\s*\n\\s*")));
		
		Set<String> actualStdoutSerials = new HashSet<String>();
		actualStdoutSerials.addAll(Arrays.asList(actualStdoutSerialsMsg.trim().split("\\s*\n\\s*")));
		
		// Note: these assertions will pass regardless if the pools/serial removed were successful or unsuccessful
		Assert.assertTrue(actualStdoutPoolIds.containsAll(expectedPoolIds) && expectedPoolIds.containsAll(actualStdoutPoolIds), "Stdout feedback when unsubscribing from all the currently consumed subscriptions pool ids contains all the expected pool ids from the list of consumed Product Subscriptions.");
		Assert.assertTrue(actualStdoutSerials.containsAll(expectedSerials) && expectedSerials.containsAll(actualStdoutSerials), "Stdout feedback when unsubscribing from all the currently consumed subscriptions pool ids contains all the expected serials from the list of consumed Product Subscriptions.");
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36599", "RHEL7-51404"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Attempt to unsubscribe from an unknown pool id and serial",
			groups={"Tier2Tests","blockedByBug-1198178","blockedByBug-1298586"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsubscribeFromAnUnknownPoolIdAndSerial() {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.16.5-1")) throw new SkipException("The unsubscribe --pool function was not implemented in this version of subscription-manager.  See RFE Bug 1198178");
		if (!servertasks.statusCapabilities.contains("remove_by_pool_id")) throw new SkipException("The registered entitlement server does not support remove --pool");
		
		// register
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null, true, false, null, null, null, null);
		
		String unknownPoolId = "1234567890abcdef1234567890abcdef";
		BigInteger unknownSerial = new BigInteger("1234567890");
		SSHCommandResult result = clienttasks.unsubscribe_(null, unknownSerial, unknownPoolId, null, null, null, null);
		//	[root@jsefler-6 ~]# subscription-manager unsubscribe --serial=1234567890 --pool=1234567890abcdef1234567890abcdef
		//	Pools unsuccessfully removed at the server:
		//	   1234567890abcdef1234567890abcdef
		//	Serial numbers unsuccessfully removed at the server:
		//	   1234567890
		
		Integer expectedExitCode = new Integer(1);
		String expectedStdout = "";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.8-1")) { // commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server
			expectedStdout += String.format("The entitlement server failed to remove these pools:\n   %s\n", unknownPoolId);
			expectedStdout += String.format("The entitlement server failed to remove these serial numbers:\n   %s\n", unknownSerial);
		} else {
			expectedStdout += String.format("Pools unsuccessfully removed at the server:\n   %s\n", unknownPoolId);
			expectedStdout += String.format("Serial numbers unsuccessfully removed at the server:\n   %s\n", unknownSerial);
		}
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "Asserting exit code when attempting to unsubscribe from an unknown pool id.");
		Assert.assertEquals(result.getStdout().trim(), expectedStdout.trim(),"Stdout");	// commit e418b77ce2a7389e310ac341a6beb46cb7eb3d0f	// Bug 1298586: Message needed for remove only invalid pool
		Assert.assertEquals(result.getStderr().trim(), "","Stderr");
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36593", "RHEL7-51398"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="Attempt to unsubscribe from an valid pool id and serial",
			groups={"Tier2Tests","blockedByBug-1198178","blockedByBug-1498664"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsubscribeFromAValidPoolIdAndSerial() {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.16.5-1")) throw new SkipException("The unsubscribe --pool function was not implemented in this version of subscription-manager.  See RFE Bug 1198178");
		if (!servertasks.statusCapabilities.contains("remove_by_pool_id")) throw new SkipException("The registered entitlement server does not support remove --pool");
		
		// register
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null, true, false, null, null, null, null);
		
		// get any available pool and subscribe
		List<SubscriptionPool> subscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool subscriptionPool = getRandomListItem(subscriptionPools);
		clienttasks.subscribe(null, null, subscriptionPool.poolId, null, null, null, null, null, null, null, null, null, null);
		
		// unsubscribe from the pool and its serial (actually the same entitlement)
		ProductSubscription productSubscription = getRandomListItem(clienttasks.getCurrentlyConsumedProductSubscriptions());
		SSHCommandResult result = clienttasks.unsubscribe_(null, productSubscription.serialNumber, productSubscription.poolId, null, null, null, null);
		//	[root@jsefler-6 ~]# subscription-manager unsubscribe --serial=1636129384995885268 --pool=8a90879052610a8b0152610bd4e40587
		//	Pools successfully removed at the server:
		//	   8a90879052610a8b0152610bd4e40587
		//	Serial numbers successfully removed at the server:
		//	   1636129384995885268
		//	1 local certificate has been deleted.

		Integer expectedExitCode = new Integer(1);	// NOTE: why exitCode 1?  probably because we are attempting two removes on one entitlement cert causing the second remove to fail after the first succeeds.
		String expectedStdout = "";
		expectedStdout  = String.format("Pools successfully removed at the server:\n   %s", productSubscription.poolId); expectedStdout += "\n";
		expectedStdout += String.format("Serial numbers successfully removed at the server:\n   %s", productSubscription.serialNumber); expectedStdout += "\n";
		expectedStdout += String.format("%d local certificate has been deleted.",1);
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.8-1")) { // commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server
			expectedStdout  = String.format("The entitlement server successfully removed these pools:\n   %s", productSubscription.poolId); expectedStdout += "\n";
			expectedStdout += String.format("The entitlement server successfully removed these serial numbers:\n   %s", productSubscription.serialNumber); expectedStdout += "\n";
			expectedStdout += String.format("%d local certificate has been deleted.",1);
		}
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.20.1-1"/*TODO change to "1.20.2-1"*/)) { // commit d88d09c7060a17fba34a138313e7efd21cc79d02  D-Bus service for removing entitlements (all/ID/serial num.)
			expectedStdout  = String.format("%d local certificate has been deleted.",1); expectedStdout += "\n";
			expectedStdout += String.format("The entitlement server successfully removed these pools:\n   %s", productSubscription.poolId); expectedStdout += "\n";
			expectedStdout += String.format("The entitlement server successfully removed these serial numbers:\n   %s", productSubscription.serialNumber);
		}
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "Asserting exit code when attempting to unsubscribe from a valid pool id and serial (corresponding to the same entitlement).");
		Assert.assertEquals(result.getStdout().trim(), expectedStdout.trim(),"Stdout");
		Assert.assertEquals(result.getStderr().trim(), "","Stderr");
		
		
		// get two available pools and subscribe
		List<SubscriptionPool> subscriptionPoolsSubset = getRandomSubsetOfList(subscriptionPools,2);
		clienttasks.subscribe(null, null, Arrays.asList(subscriptionPoolsSubset.get(0).poolId, subscriptionPoolsSubset.get(1).poolId), null, null, null, null, null, null, null, null, null, null);
		
		// unsubscribe from one pool and one serial
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		ProductSubscription productSubscription1 = productSubscriptions.get(0);
		ProductSubscription productSubscription2 = productSubscriptions.get(1);
		result = clienttasks.unsubscribe_(null, productSubscription1.serialNumber, productSubscription2.poolId, null, null, null, null);
		//	[root@jsefler-6 ~]# subscription-manager unsubscribe --serial=1826457911783374718 --pool=8a90879052610a8b0152610be38f074b (com.redhat.qe.tools.SSHCommandRunner.run)
		//	Pools successfully removed at the server:
		//	   8a90879052610a8b0152610be38f074b
		//	Serial numbers successfully removed at the server:
		//	   6656837828924322952
		//	   1826457911783374718
		//	2 local certificates have been deleted.
		
		expectedExitCode = new Integer(0);
		expectedStdout  = String.format("Pools successfully removed at the server:\n   %s", productSubscription2.poolId); expectedStdout += "\n";
		expectedStdout += String.format("Serial numbers successfully removed at the server:\n   %s\n   %s", productSubscription2.serialNumber, productSubscription1.serialNumber); expectedStdout += "\n";
		expectedStdout += String.format("%d local certificates have been deleted.",2);
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.8-1")) { // commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server
			expectedStdout  = String.format("The entitlement server successfully removed these pools:\n   %s", productSubscription2.poolId); expectedStdout += "\n";
			expectedStdout += String.format("The entitlement server successfully removed these serial numbers:\n   %s\n   %s", productSubscription2.serialNumber, productSubscription1.serialNumber); expectedStdout += "\n";
			expectedStdout += String.format("%d local certificates have been deleted.",2);
		}
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.20.1-1"/*TODO change to "1.20.2-1"*/)) { // commit d88d09c7060a17fba34a138313e7efd21cc79d02  D-Bus service for removing entitlements (all/ID/serial num.)
			expectedStdout  = String.format("%d local certificates have been deleted.",2); expectedStdout += "\n";
			expectedStdout += String.format("The entitlement server successfully removed these pools:\n   %s", productSubscription2.poolId); expectedStdout += "\n";
			expectedStdout += String.format("The entitlement server successfully removed these serial numbers:\n   %s\n   %s", productSubscription2.serialNumber, productSubscription1.serialNumber); expectedStdout += "\n";
		}
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "Asserting exit code when attempting to unsubscribe from from a valid pool id and serial (corresponding to two different entitlements).");
		Assert.assertEquals(result.getStdout().trim(), expectedStdout.trim(),"Stdout");
		Assert.assertEquals(result.getStderr().trim(), "","Stderr");
	}
	
	
	
	
	
	
	// Candidates for an automated Test:
	

	
	// Data Providers ***********************************************************************
	
	
	
	
}
