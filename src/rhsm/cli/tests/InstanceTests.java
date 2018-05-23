package rhsm.cli.tests;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.SSHCommandResult;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author jsefler
 *
 * Reference Design Doc:
 * https://engineering.redhat.com/trac/Entitlement/wiki/InstanceBasedDesign
 */
@Test(groups={"InstanceTests"})
public class InstanceTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19986", "RHEL7-51020"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="test compliance using variations on sockets and system type when subscribing to an Instance-Based subscription",
			groups={"Tier1Tests","QuantityNeededToAchieveSocketCompliance_Test","blockedByBug-979492"},
			dataProvider="getAvailableInstanceBasedSubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testQuantityNeededToAchieveSocketCompliance(Object bugzilla, Boolean systemIsGuest, Integer systemSockets, SubscriptionPool pool) throws NumberFormatException, JSONException, Exception {
///*debugTest*/if (!pool.productId.equals("RH00003")) throw new SkipException("debugTesting");/*debugTest*/if (systemIsGuest) throw new SkipException("debugTesting");

		// avoid throttling RateLimitExceededException from IT-Candlepin
		if (systemSockets.equals(new Integer(1)) && CandlepinType.hosted.equals(sm_serverType)) {	// strategically get a new consumer to avoid 60 repeated API calls from the same consumer
			// re-register as a new consumer
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
		}
		
		// This dataProvided test was inspired by the following table of scenarios
		// https://engineering.redhat.com/trac/Entitlement/wiki/InstanceBasedDesign#Scenarios
		/*
		+--------------------------------------------------------------------------+
		|                     Quantity needed to Achieve Socket Compliance         |
		|--------------------------------------------------------------------------|
		| Sample Systems |   2010 Pricing Sub  |  2013 Pricing Sub (inst-based)    |
		|                |                     |     instance_multiplier=2         |
		|                |  order quantity=10  |     order quantity=10             |
		|                |  pool quantity=10   |     pool quantity=20              |
		|                |---------------------------------------------------------|
		|                |sockets=2 |sockets=4 | sockets=1 | sockets=2 | sockets=4 | 
		|==========================================================================|
		| Physical       |    1*    |     1*   |     2     |     2*    |    2*     |
		| 1 sockets      |          |          |           |           |           |
		|--------------------------------------------------------------------------|
		| Physical       |    1     |     1*   |     4     |     2     |    2*     |
		| 2 sockets      |          |          |           |           |           |
		|--------------------------------------------------------------------------|
		| Physical       |    4     |     2    |     16    |     8     |    4      |
		| 8 sockets      |          |          |           |           |           |
		|--------------------------------------------------------------------------|
		| Virtual        |    1*    |     1*   |     1     |     1*    |    1*     |
		| 1 sockets      |          |          |           |           |           |
		|--------------------------------------------------------------------------|
		| Virtual        |    1     |     1*   |     1     |     1     |    1*     |
		| 2 sockets      |          |          |           |           |           |
		|--------------------------------------------------------------------------|
		| Virtual        |    4     |     2    |     1     |     1     |    1      |
		| 8 sockets      |          |          |           |           |           |
		+--------------------------------------------------------------------------+
		*/
		
		// make sure we are unsubscribed from all subscriptions
		clienttasks.unsubscribe(true, (BigInteger)null, null, null, null, null, null);
		// NOTE: can throw a Runtime Error No row with the given identifier exists: [org.candlepin.model.PoolAttribute#8a908790535c4e7201535ce8eb4e18fa] at org.hibernate.UnresolvableObjectException.throwIfNull:64
		// when prior dataProvided test fails thereby skipping the last unsubscribe subProductSubscription.serialNumber in this test
		
		// get some attributes from the subscription pool
		List<String> poolProvidedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId);
		Integer poolInstanceMultiplier = Integer.valueOf(CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "instance_multiplier"));
		String poolVirtLimit = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "virt_limit");
		String poolSocketsAsString = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "sockets");
		Integer poolSockets = poolSocketsAsString==null? null:Integer.valueOf(poolSocketsAsString);
		// manipulate a fake value for poolSockets when there is no 'sockets' pool productAttribute
		if (poolSockets==null) {
			// NOTE: Red Hat Enterprise Linux Beta for IBM System z  SKU: RH00071  SubscriptionType: Instance Based  SystemType: Physical  has no "sockets" product attribute
			if (systemIsGuest) {
				// for compliance calculation purposes assume poolSockets is 1 when the pool has no attribute for sockets
				poolSockets = 1;
			} else {
				// for compliance calculation purposes assume poolSockets is systemSockets when the pool has no attribute for sockets	
				poolSockets = systemSockets;
			}
			log.warning("There is no 'sockets' productAttribute for Subscription '"+pool.subscriptionName+"' SKU '"+pool.productId+"'.  Assuming a value of '"+poolSockets+"' for compliance calculations.");
		}
		
		// instrument the system facts from the dataProvider
		Map<String,String> factsMap = new HashMap<String,String>();
		String dmidecodeSystemUuid = client.runCommandAndWait("dmidecode --string=system-uuid").getStdout().trim();
		//	ssh root@celeno.idmqe.lab.eng.bos.redhat.com dmidecode --string=system-uuid
		//	Stdout:
		//	# SMBIOS implementations newer than version 2.8 are not
		//	# fully supported by this version of dmidecode.
		//	174DBEBA-F6BA-1BE1-BAF6-E11BBFBFED17
		//	Stderr:
		//	ExitCode: 0
		dmidecodeSystemUuid = dmidecodeSystemUuid.replaceAll("#.*\n", "").toLowerCase().trim();	// to get rid of comment lines in the dmidecode response
		factsMap.clear();
		factsMap.put("cpu.cpu_socket(s)",String.valueOf(systemSockets));
		factsMap.put("virt.is_guest",Boolean.toString(systemIsGuest));
		factsMap.put("virt.uuid",dmidecodeSystemUuid);	// reset to actual value from dmidecode --string=system-uuid
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		
		// update the facts on the system
		clienttasks.facts(null, true, null, null, null, null);
		
		// predict the quantity needed to achieve compliance
		// think of this using the old 2010 pricing model and then multiply the answer by the poolInstanceMultiplier
		int expectedQuantityToAchieveCompliance = 1;
		while (expectedQuantityToAchieveCompliance*poolSockets < systemSockets) expectedQuantityToAchieveCompliance++;
		expectedQuantityToAchieveCompliance *= poolInstanceMultiplier;
		
		// assert the initial unsubscribed installed product status
		List<InstalledProduct> currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
		List<String> providedProductIdsActuallyInstalled = new ArrayList<String>();
		for (String productId : poolProvidedProductIds) {
			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", productId, currentlyInstalledProducts);
			if (installedProduct!=null) {
				providedProductIdsActuallyInstalled.add(installedProduct.productId);
				List<String> expectedStatusDetails = Arrays.asList(new String[]{"Not supported by a valid subscription."});	// Message changed by candlepin commit 43a17952c724374c3fee735642bce52811a1e386	covers -> supports
				if (installedProduct.statusDetails.isEmpty()) log.warning("Status Details appears empty.  Is your candlepin server older than 0.8.6?");
				Assert.assertEquals(installedProduct.status,"Not Subscribed", "Since we have not yet consumed an instance based entitlement, the status of installed product '"+installedProduct.productName+"' should be this value.");
				Assert.assertEquals(installedProduct.statusDetails,expectedStatusDetails,"Since we have not yet consumed an instance based entitlement, the status details of installed product '"+installedProduct.productName+"' is expected to be: "+expectedStatusDetails);
			}
		}
		
		// start subscribe testing
		if (systemIsGuest) {
			
			// virtual systems -----------------------------------------------------------------------------------

			// virtual systems will be allowed to consume 1 entitlement from the instance based pool and be compliant
			// regardless of sockets (this effectively satisfies the "either-or" behavior when a virtual system
			// consumes from the instance based pool - the quantity consumed decrements by one)

			clienttasks.subscribe(false,null,pool.poolId,null,null,"1",null,null,null,null,null, null, null);
			
			// assert the installed provided products are compliant
			currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
			for (String productId : poolProvidedProductIds) {
				InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", productId, currentlyInstalledProducts);
				if (installedProduct!=null) {
					Assert.assertEquals(installedProduct.status,"Subscribed", "After attaching 1 instance-based subscription to a virtual system, installed product '"+installedProduct.productName+"' should be immediately compliant.");
					Assert.assertTrue(installedProduct.statusDetails.isEmpty(), "Status Details for installed product '"+installedProduct.productName+"' should be empty.  Actual="+installedProduct.statusDetails);
				}
			}
			
			// now let's unsubscribe from all entitlements and attempt auto-subscribing
			clienttasks.unsubscribe(true, (BigInteger)null, null, null, null, null, null);
			
			// but first, let's pretend that this virtual system is mapped so that we can avoid unmapped_guests_only pools during auto-subscribe
			factsMap.put("virt.uuid","avoid-unmapped-guests-only");
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			clienttasks.facts(null,true,null,null,null, null);
			clienttasks.mapSystemAsAGuestOfItself();
			
			// TEMPORARY WORKAROUND FOR BUG
			String bugId = "964332"; boolean invokeWorkaroundWhileBugIsOpen = true;
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				// issue a sacrificial autosubscribe call to get most of the entitlements attached.  If it times out, the post_auto_attach hooks will not get called
				clienttasks.subscribe_(true,null,(String)null,null,null,null,null,null,null,null,null, null, null);
			}
			// END OF WORKAROUND
			
			// attempt auto-subscribing
			clienttasks.subscribe(true,null,(String)null,null,null,null,null,null,null,null,null, null, null);
			
			// assert the quantity of consumption
			if (!providedProductIdsActuallyInstalled.isEmpty()) {
				/* These assertions are valid ONLY when this instance-based subscription pool is the ONLY one available that provides for all of the providedProductIdsActuallyInstalled (Not guarantee-able)
				ProductSubscription productSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName", pool.subscriptionName, clienttasks.getCurrentlyConsumedProductSubscriptions());
				Assert.assertNotNull(productSubscription, "Found a consumed product subscription to '"+pool.subscriptionName+"' after autosubscribing.");
				Assert.assertEquals(productSubscription.quantityUsed,Integer.valueOf(1),"Autosubscribing a virtual system with instance based products installed should only consume 1 quantity from the instance based pool.");
				*/
			} else log.warning("There are no installed product ids '"+poolProvidedProductIds+"' to assert compliance status of instance-based subscription '"+pool.subscriptionName+"'.");
			
			// assert the installed provided products are compliant after auto-subscribing
			currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
			for (String productId : poolProvidedProductIds) {
				InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", productId, currentlyInstalledProducts);
				if (installedProduct!=null) {
					Assert.assertEquals(installedProduct.status,"Subscribed", "After auto-subscribing a virtual system, installed product '"+installedProduct.productName+"' should be immediately compliant.");
					Assert.assertTrue(installedProduct.statusDetails.isEmpty(), "Status Details for installed product '"+installedProduct.productName+"' should be empty.  Actual="+installedProduct.statusDetails);
				}
			}
			
		} else {
			
			// physical systems -----------------------------------------------------------------------------------
			
			// physical systems must consume entitlements from the instance based pool in quantities that are evenly
			// divisible by the instance_multiplier.  Moreover, sockets matter for compliance.
			// In addition (if host_limited with virt_limit), when a physical system consumes from the instance based
			// pool, a subpool with unlimited quantity available only to the guests on this physical system will be generated.
			
			// start by attempting to subscribe in quantities that are NOT evenly divisible by the instance_multiplier
			int quantityAttached=0;
			for (int quantityAttempted=0; quantityAttempted<=poolInstanceMultiplier+1; quantityAttempted++) {
				SSHCommandResult sshCommandResult = clienttasks.subscribe_(false,null,pool.poolId,null,null,String.valueOf(quantityAttempted),null,null,null,null,null, null, null);
				
				// TEMPORARY WORKAROUND FOR BUG: 1183122 - rhsmd/subman dbus traceback on 'attach --pool'
				if (sshCommandResult.getStderr().contains("KeyError: 'product_id'")) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="1183122"; 
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
					if (invokeWorkaroundWhileBugIsOpen) {
						log.warning("Encountered bug '"+bugId+"'. Skipping stdout/stderr/exitCode assertion from the prior subscribe command while bug '"+bugId+"' is open.");
						if (quantityAttempted%poolInstanceMultiplier==0) quantityAttached+=quantityAttempted;
						continue;
					}
				}
				// END OF WORKAROUND
				
				if (quantityAttempted==0) {
					if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
						Assert.assertEquals(sshCommandResult.getStderr().trim(), "Error: Quantity must be a positive integer.", "The stderr from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+quantityAttempted+"' which should be an error.");
						Assert.assertEquals(sshCommandResult.getStdout().trim(), "", "The stdout from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+quantityAttempted+"' which should be an error.");
						Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(64)/*EX_USAGE*/, "The exit code from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+quantityAttempted+"' which should be an error.");
					} else {
						Assert.assertEquals(sshCommandResult.getStdout().trim(), "Error: Quantity must be a positive integer.", "The stdout from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+quantityAttempted+"' which should be an error.");
						Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "The stderr from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+quantityAttempted+"' which should be an error.");
						Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(255), "The exit code from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+quantityAttempted+"' which should be an error.");
					}
				} else if (quantityAttempted%poolInstanceMultiplier!=0) {
					String expectedStdout = String.format("Subscription '%s' must be attached using a quantity evenly divisible by %s",pool.subscriptionName,poolInstanceMultiplier);
					if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
						expectedStdout = String.format("Subscription \"%s\" must be attached using a quantity evenly divisible by %s",pool.subscriptionName,poolInstanceMultiplier);
					}
					Assert.assertEquals(sshCommandResult.getStdout().trim(), expectedStdout, "The stdout from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+quantityAttempted+"' which is not evenly divisible by the instance_multiplier '"+poolInstanceMultiplier+"'.");	// expected stdout message changed by Bug 1033365 - request to improve unfriendly message: Quantity '1' is not a multiple of instance multiplier '2'
					Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "The stderr from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+quantityAttempted+"' which is not evenly divisible by the instance_multiplier '"+poolInstanceMultiplier+"'.");
					Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(1)/* TODO figure out if this is a bug.  should it be 255?*/, "The exit code from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+quantityAttempted+"' which is not evenly divisible by the instance_multiplier '"+poolInstanceMultiplier+"'.");
				} else {
					Assert.assertEquals(sshCommandResult.getStdout().trim(), String.format("Successfully attached a subscription for: %s",pool.subscriptionName), "The stdout from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+quantityAttempted+"' which is evenly divisible by the instance_multiplier '"+poolInstanceMultiplier+"'.");
					Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "The stderr from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+quantityAttempted+"' which is evenly divisible by the instance_multiplier '"+poolInstanceMultiplier+"'.");
					Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from attempt to attach subscription '"+pool.subscriptionName+"' with quantity '"+quantityAttempted+"' which is evenly divisible by the instance_multiplier '"+poolInstanceMultiplier+"'.");
					quantityAttached+=quantityAttempted;
				}
			}
			
			// at this point the attempt to attach the instance based subscription should have been successful when the requested quantity was equal to the instance_multiplier
			ProductSubscription productSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName", pool.subscriptionName, clienttasks.getCurrentlyConsumedProductSubscriptions());
			Assert.assertNotNull(productSubscription, "Found a consumed product subscription to '"+pool.subscriptionName+"' after manually subscribing.");
			Assert.assertEquals(productSubscription.quantityUsed,Integer.valueOf(poolInstanceMultiplier),"The attached quantity of instance based subscription '"+pool.subscriptionName+"' in the list of consumed product subscriptions.");
			if (poolInstanceMultiplier>=expectedQuantityToAchieveCompliance) {	// compliant when true
				List<String> expectedStatusDetails = new ArrayList<String>();	// empty
				if (clienttasks.isPackageVersion("subscription-manager",">=", "1.13.13-1")) {	 // commit 252ec4520fb6272b00ae379703cd004f558aac63	// bug 1180400: "Status Details" are now populated on CLI
					expectedStatusDetails = Arrays.asList(new String[]{"Subscription is current"});	// Bug 1180400 - Status datails is blank in list consumed output
				}
				// TEMPORARY WORKAROUND FOR BUG
				String bugId = "1580996";	// Bug 1580996 - RHEL8 subscription-manager list --consumed shows Status Details: "Subscription has not begun" expected "Subscription is current"
				boolean invokeWorkaroundWhileBugIsOpen = true;
				List<String> unexpectedStatusDetailsFromBug1580996 = Arrays.asList(new String[]{"Subscription has not begun"});
				try {if (clienttasks.redhatReleaseX.equals("8") && productSubscription.statusDetails.equals(unexpectedStatusDetailsFromBug1580996) && invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (clienttasks.redhatReleaseX.equals("8") && productSubscription.statusDetails.equals(unexpectedStatusDetailsFromBug1580996) && invokeWorkaroundWhileBugIsOpen) {
					log.warning("Skipping the assert that status details "+productSubscription.statusDetails+" from consumed product subscription '"+productSubscription.productName+"' equals "+expectedStatusDetails+" while bug '"+bugId+"' is open.");
				} else
				// END OF WORKAROUND
				Assert.assertEquals(productSubscription.statusDetails, expectedStatusDetails, "The statusDetails from the consumed product subscription '"+productSubscription.productName+"' poolId='"+productSubscription.poolId+"' should be "+expectedStatusDetails+" indicating compliance.");
			} else {
				List<String> expectedStatusDetails = Arrays.asList(new String[]{String.format("Only supports %s of %s sockets.",(quantityAttached*poolSockets)/poolInstanceMultiplier,systemSockets)});	// Message changed by candlepin commit 43a17952c724374c3fee735642bce52811a1e386 covers -> supports
				if (productSubscription.statusDetails.isEmpty()) log.warning("Status Details appears empty.  Is your candlepin server older than 0.8.6?");
				Assert.assertEquals(productSubscription.statusDetails,expectedStatusDetails, "Status Details for consumed product subscription '"+productSubscription.productName+"'.  Expected="+expectedStatusDetails);
			}
			
			// at this point the installed product id should either be "Subscribed" or "Partially Subscribed" since one of the quantity attempts should have succeeded (when qty was equal to poolInstanceMultiplier), let's assert based on the system's sockets
			currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
			for (String productId : poolProvidedProductIds) {
				InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", productId, currentlyInstalledProducts);
				if (installedProduct!=null) {
					if (poolInstanceMultiplier>=expectedQuantityToAchieveCompliance) {	// compliant when true
						Assert.assertEquals(installedProduct.status,"Subscribed", "After manually attaching a quantity of '"+poolInstanceMultiplier+"' subscription '"+pool.subscriptionName+"' covering '"+poolSockets+"' sockets with instance_multiplier '"+poolInstanceMultiplier+"', the status of installed product '"+installedProduct.productName+"' on a physical system with '"+systemSockets+"' cpu_socket(s) should be this.");
						Assert.assertTrue(installedProduct.statusDetails.isEmpty(), "Status Details for installed product '"+installedProduct.productName+"' should be empty.  Actual="+installedProduct.statusDetails);
					} else {
						List<String> expectedStatusDetails = Arrays.asList(new String[]{String.format("Only supports %s of %s sockets.",(quantityAttached*poolSockets)/poolInstanceMultiplier,systemSockets)}); // Message changed by candlepin commit 43a17952c724374c3fee735642bce52811a1e386 covers -> supports
						if (installedProduct.statusDetails.isEmpty()) log.warning("Status Details appears empty.  Is your candlepin server older than 0.8.6?");
						Assert.assertEquals(installedProduct.status,"Partially Subscribed", "After manually attaching a quantity of '"+poolInstanceMultiplier+"' subscription '"+pool.subscriptionName+"' covering '"+poolSockets+"' sockets with instance_multiplier '"+poolInstanceMultiplier+"', the status of installed product '"+installedProduct.productName+"' on a physical system with '"+systemSockets+"' cpu_socket(s) should be this.");
						Assert.assertEquals(installedProduct.statusDetails,expectedStatusDetails,"Status Details for installed product '"+installedProduct.productName+" should be this value: "+expectedStatusDetails);
					}
				}
			}
			
			// now let's attempt autosubscribing which should complete the stack
			// CAUTION: attempting to autosubscribe to fill a stack of this instance-based pool will work ONLY when this instance-based subscription pool is the ONLY one available that provides for all of the providedProductIdsActuallyInstalled (Not guarantee-able).  However if a second pool with the same stacking_id is consumed, then this assert may work.
			clienttasks.subscribe(true,null,(String)null,null,null,null,null,null,null,null,null, null, null);
			
			// assert the total quantity of consumption
			if (!providedProductIdsActuallyInstalled.isEmpty()) {
				/* The following algorithm neglects the case when multiple subscriptions by different names provide the installed products providedProductIdsActuallyInstalled.
				List<ProductSubscription> productSubscriptions = ProductSubscription.findAllInstancesWithMatchingFieldFromList("productName", pool.subscriptionName, clienttasks.getCurrentlyConsumedProductSubscriptions());
				Assert.assertTrue(!productSubscriptions.isEmpty(), "Found at least one consumed product subscription to '"+pool.subscriptionName+"' after auto-subscribing.");
				Integer totalQuantityUsed = 0;
				for (ProductSubscription prodSub : productSubscriptions) {
					totalQuantityUsed += prodSub.quantityUsed;
					Assert.assertTrue(prodSub.statusDetails.isEmpty(),"Status Details of auto-attached subscription '"+pool.subscriptionName+"' covering '"+poolSockets+"' sockets with instance_multiplier '"+poolInstanceMultiplier+"' expected to achieve compliance of provided products '"+providedProductIdsActuallyInstalled+"' installed on a physical system with '"+systemSockets+"' cpu_socket(s) should be empty.  Actual="+prodSub.statusDetails);
				}
				Assert.assertEquals(totalQuantityUsed,Integer.valueOf(expectedQuantityToAchieveCompliance),"Quantity of auto-attached subscription '"+pool.subscriptionName+"' covering '"+poolSockets+"' sockets with instance_multiplier '"+poolInstanceMultiplier+"' expected to achieve compliance of provided products '"+providedProductIdsActuallyInstalled+"' installed on a physical system with '"+systemSockets+"' cpu_socket(s) should be this.");
				Re-implementing a new algorithm below to count the number of system sockets covered and then assert that the autosubscribe successfully met coverage without excess over consumption...*/
				boolean assertStackedQuantityOfConsumption = true;	// goal
				String stackingId = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId,"stacking_id");
				int numPoolsWithSameStackingId = 0;
				float totalSocketsCovered = 0;	// among the consumed product subscriptions, this is the total stacked accumulation of socket coverage
				Integer maxIncrementOfPhysicalSocketCoverage = new Integer(0);	// this is the maximum sockets attribute among the pools that provide for the installed products poolProvidedProductIds
//				List<ProductSubscription> productSubscriptions = new ArrayList<ProductSubscription>();
				for (ProductSubscription prodSub : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
					List<String> thisPoolProvidedProductIds = CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword, sm_serverUrl, prodSub.poolId);
					if (doesListOverlapList(thisPoolProvidedProductIds, providedProductIdsActuallyInstalled)) {
//deugTesting
//if (CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, prodSub.poolId, "instance_multiplier")==null)  { // does not have an "instance_multiplier"
//	log.warning("Ignoring this consumed product subscription's contribution to compliance (it has no instance_multiplier): "+prodSub);	//  not sure if this is the right choice
//	continue;
//}	
						// the consumed quantity from this pool contributes to the socket coverage for installed products poolProvidedProductIds
						String thisPoolInstanceMultiplierAsString = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, prodSub.poolId, "instance_multiplier");
						String thisPoolSocketsAsString = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, prodSub.poolId, "sockets");

						// catching CAUTION case when autosubscribe grants a secondary entitlement unrelated to the already subscribed instance-based subscription 
						if (thisPoolInstanceMultiplierAsString==null) {
							assertStackedQuantityOfConsumption = false;
							thisPoolInstanceMultiplierAsString = "1";	// effectively true and a workaround that will prevent a null pointer in the calculations below
						}
						// catching CAUTION case when autosubscribe grants a secondary entitlement unrelated to the already subscribed instance-based subscription 
						if (thisPoolSocketsAsString==null) {
							assertStackedQuantityOfConsumption = false;
							thisPoolSocketsAsString = String.valueOf(systemSockets);	// effectively true and a workaround that will prevent a null pointer in the calculations below
						}
						
						Integer thisPoolInstanceMultiplier = Integer.valueOf(thisPoolInstanceMultiplierAsString);
						Integer thisPoolSockets = Integer.valueOf(thisPoolSocketsAsString);
						String thisPoolStackingId = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, prodSub.poolId,"stacking_id");
						maxIncrementOfPhysicalSocketCoverage=Math.max(maxIncrementOfPhysicalSocketCoverage, thisPoolSockets);
						float socketsCoveredByThisPool = prodSub.quantityUsed.floatValue()*thisPoolSockets.floatValue()/thisPoolInstanceMultiplier.floatValue();
						log.info("Attached product subscription '"+prodSub.productName+"' with quantity '"+prodSub.quantityUsed+"' contributes to '"+socketsCoveredByThisPool+"' cpu_socket(s) of coverage.");
						totalSocketsCovered+=socketsCoveredByThisPool;
						List<String> expectedStatusDetails = new ArrayList<String>();	// empty
						if (clienttasks.isPackageVersion("subscription-manager",">=", "1.13.13-1")) {	 // commit 252ec4520fb6272b00ae379703cd004f558aac63	// bug 1180400: "Status Details" are now populated on CLI
							expectedStatusDetails = Arrays.asList(new String[]{"Subscription is current"});	// Bug 1180400 - Status datails is blank in list consumed output
						}
						Assert.assertEquals(prodSub.statusDetails, expectedStatusDetails, "Status Details of auto-attached subscription '"+prodSub.productName+"' covering '"+thisPoolSockets+"' sockets with instance_multiplier '"+thisPoolInstanceMultiplier+"' expected to contribute to the full compliance of provided products '"+providedProductIdsActuallyInstalled+"' installed on a physical system with '"+systemSockets+"' cpu_socket(s) should indicate compliance.  Actual="+prodSub.statusDetails);
						
						// catching the CAUTION case when a secondary pool may not provide its own completed socket coverage stack for the installed products.
						if (!stackingId.equals(thisPoolStackingId)) {
							log.warning("Cannot assert the attempt to autosubscribe completed the socket stack because it appears that an entitlement from a second pool SKU '"+prodSub.productId+"' was granted that does not share the same stacking_id '"+stackingId+"' as instance-based pool '"+pool.productId+"' '"+pool.subscriptionName+"'.  Consequently, excess entitlement consumption has probably occurred.");
							assertStackedQuantityOfConsumption = false;
							// instead, assert that this product subscription provided it's own full stack
							Assert.assertTrue(systemSockets+maxIncrementOfPhysicalSocketCoverage>socketsCoveredByThisPool && socketsCoveredByThisPool>=systemSockets, "After autosubscribing to complete a stacked quantity of subscriptions providing for installed product ids '"+providedProductIdsActuallyInstalled+"', the total cpu_socket(s) coverage of '"+socketsCoveredByThisPool+"' should minimally satistfy the system's physical socket count of '"+systemSockets+"' cpu_socket(s) within '"+maxIncrementOfPhysicalSocketCoverage+"' sockets of excess coverage (assuming that entitlements from ONLY this product subscription '"+prodSub.productId+"' '"+prodSub.productName+"' contributed to stack '"+thisPoolStackingId+"'.).");
						}
						else numPoolsWithSameStackingId++;
						
					}
				}
				if (assertStackedQuantityOfConsumption) Assert.assertTrue(systemSockets+maxIncrementOfPhysicalSocketCoverage>totalSocketsCovered && totalSocketsCovered>=systemSockets, "After autosubscribing to complete a stacked quantity of subscriptions providing for installed product ids '"+providedProductIdsActuallyInstalled+"', the total cpu_socket(s) coverage of '"+totalSocketsCovered+"' should minimally satistfy the system's physical socket count of '"+systemSockets+"' cpu_socket(s) within '"+maxIncrementOfPhysicalSocketCoverage+"' sockets of excess coverage (entitlements from '"+numPoolsWithSameStackingId+"' pools contributed to this stack).");

			} else log.warning("There are no installed product ids '"+poolProvidedProductIds+"' to assert compliance status of instance-based subscription '"+pool.subscriptionName+"'.");
			
			// assert the installed provided products are compliant
			currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
			for (String productId : poolProvidedProductIds) {
				InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", productId, currentlyInstalledProducts);
				if (installedProduct!=null) {
					Assert.assertEquals(installedProduct.status,"Subscribed", "After auto-subscribing a physical system, installed product '"+installedProduct.productName+"' should be compliant.");
					Assert.assertTrue(installedProduct.statusDetails.isEmpty(), "Status Details for installed product '"+installedProduct.productName+"' should be empty.  Actual="+installedProduct.statusDetails);
				}
			}
			
			// do some more testing when the pool is host limited and virt limited... 
			if (CandlepinTasks.isPoolProductHostLimited(sm_clientUsername,sm_clientPassword, sm_serverUrl, pool.poolId) && CandlepinTasks.isPoolProductVirtLimited(sm_clientUsername,sm_clientPassword, sm_serverUrl, pool.poolId)) {
				
				// now we can assert that a host_limited subpool was generated from consumption of this physical pool and is only available to guests of this physical system
				// first, let's flip the virt.is_guest to true and assert that the virtual guest subpool is not (yet) available since the virtUuid is not on the host consumer's list of guestIds
				// factsMap.clear(); // do not clear since it will already contain cpu.cpu_socket(s)
				factsMap.put("virt.is_guest",String.valueOf(true));
				clienttasks.createFactsFileWithOverridingValues(factsMap);
				clienttasks.facts(null,true,null,null,null, null);
				List<SubscriptionPool> availableInstanceBasedSubscriptionPools = SubscriptionPool.findAllInstancesWithMatchingFieldFromList("productId", pool.productId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
				for (SubscriptionPool availableInstanceBasedSubscriptionPool : availableInstanceBasedSubscriptionPools) {
					if (!CandlepinTasks.isPoolRestrictedToUnmappedVirtualSystems(sm_clientUsername,sm_clientPassword, sm_serverUrl, availableInstanceBasedSubscriptionPool.poolId)) {
						Assert.assertEquals(availableInstanceBasedSubscriptionPool.machineType, "Physical", "Only physical pools to '"+pool.subscriptionName+"' (poolId="+availableInstanceBasedSubscriptionPool.poolId+") should be available to a guest system when its virt_uuid is not on the host's list of guestIds (unless it is an unmapped_guests_only pool).");
					} else {
						Assert.assertEquals(availableInstanceBasedSubscriptionPool.machineType, "Virtual", "Only unmapped_guests_only virtual pools to '"+pool.subscriptionName+"' (poolId="+availableInstanceBasedSubscriptionPool.poolId+") should be available to a guest system when its virt_uuid is not on the host's list of guestIds.");	
					}
				}
				
				// now fake this consumer's facts and guestIds to make it think it is a guest of itself (a trick for testing)
				factsMap.put("virt.uuid","fake-virt-uuid");
				clienttasks.createFactsFileWithOverridingValues(factsMap);
				clienttasks.facts(null,true,null,null,null, null);
				clienttasks.mapSystemAsAGuestOfItself();
				
				// now the host_limited subpool for this virtual system should be available
				availableInstanceBasedSubscriptionPools = SubscriptionPool.findAllInstancesWithMatchingFieldFromList("productId", pool.productId, clienttasks.getCurrentlyAvailableSubscriptionPools());
				availableInstanceBasedSubscriptionPools = SubscriptionPool.findAllInstancesWithMatchingFieldFromList("machineType", "Virtual", availableInstanceBasedSubscriptionPools);
	 			Assert.assertTrue(!availableInstanceBasedSubscriptionPools.isEmpty(),"Host_limited Virtual subpool to instance based subscription '"+pool.subscriptionName+"' is available to its guest.");
				Assert.assertEquals(availableInstanceBasedSubscriptionPools.size(),1,"Only one host_limited Virtual subpool to instance based subscription '"+pool.subscriptionName+"' is available to its guest.");
				Assert.assertEquals(availableInstanceBasedSubscriptionPools.get(0).quantity,poolVirtLimit,"The quantity of entitlements from the host_limited Virtual subpool to instance based subscription '"+pool.subscriptionName+"' should be equal to the subscription's virt_limit '"+poolVirtLimit+"'.");
				
				// consume an entitlement from the subPool so that we can test Bug 1000444
				SubscriptionPool subSubscriptionPool = availableInstanceBasedSubscriptionPools.get(0);
				//clienttasks.subscribeToSubscriptionPool(subSubscriptionPool);
				clienttasks.subscribe_(false,null,subSubscriptionPool.poolId,null,null,"1",null,null,null,null,null, null, null);
				ProductSubscription subProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", subSubscriptionPool.poolId, clienttasks.getCurrentlyConsumedProductSubscriptions());
				// assert Bug 1000444 - Instance based subscription on the guest gets merged with other subscription when a future instance based subscription is added on the host
				// TEMPORARY WORKAROUND
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1256926"; // Bug 1256926 - Instance Based pool appears to be providing extra products than expected
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("while bug '"+bugId+"' is open, skipping assertion: The list of provided products from the consumed subpool '"+subProductSubscription.poolId+"' "+subProductSubscription.provides+" should be a superset of the provided products from the consumed hostpool '"+pool.poolId+"' "+pool.provides+".");
				} else
				// END OF WORKAROUND
				Assert.assertTrue(subProductSubscription.provides.containsAll(pool.provides)/*DELETEME && pool.provides.containsAll(subProductSubscription.provides)*/, "The list of provided products from the consumed subpool '"+subProductSubscription.poolId+"' "+subProductSubscription.provides+" should be a superset of the provided products from the consumed hostpool '"+pool.poolId+"' "+pool.provides+".  (Superset because a another pool with the same stacking_id could have been auto consumed earlier in this test that provides additional products that were added to the one-sub-pool-per-stack subpool '"+subProductSubscription.poolId+"'.)");
				clienttasks.unsubscribe_(false, subProductSubscription.serialNumber, null, null, null, null, null);
			}
		}
	}
	@AfterGroups(value={"QuantityNeededToAchieveSocketCompliance_Test"},groups={"setup"})
	public void afterQuantityNeededToAchieveSocketCompliance_Test() {
		clienttasks.deleteFactsFileWithOverridingValues();
	}
	@DataProvider(name="getAvailableInstanceBasedSubscriptionPoolsData")
	public Object[][] getAvailableInstanceBasedSubscriptionPoolsDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getAvailableInstanceBasedSubscriptionPoolsDataAsListOfLists());
	}
	protected List<List<Object>> getAvailableInstanceBasedSubscriptionPoolsDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		Map<String,Integer> poolProductIdsQuantityMap = new HashMap<String,Integer>();
		for (List<Object> list : getAvailableSubscriptionPoolsDataAsListOfLists(false)) {
			SubscriptionPool pool = (SubscriptionPool)(list.get(0));
			
			// skip unmapped_guests_only pools
			if (CandlepinTasks.isPoolRestrictedToUnmappedVirtualSystems(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId)) {
				continue;
			}
			
			// test auto-subscribing across multiple instance_multiplier pools
			if (poolProductIdsQuantityMap.containsKey(pool.productId)) {
				
				// the fact that we are here means that there are multiple pools available for the same instance-based product subscription
				// let's try testing cpu_sockets size that will require entitlements from both pools when auto-subscribing
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"963227"/*,"964332"*/}),	false,	Integer.valueOf(Math.max(poolProductIdsQuantityMap.get(pool.productId),Integer.valueOf(pool.quantity)))+2,	pool}));
				ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"963227"/*,"964332"*/}),	false,	Integer.valueOf(pool.quantity)+poolProductIdsQuantityMap.get(pool.productId),	pool}));

				poolProductIdsQuantityMap.put(pool.productId, Integer.valueOf(pool.quantity)+poolProductIdsQuantityMap.get(pool.productId));
				continue;
			}
			
			// test instance_multiplier pools
			if (CandlepinTasks.isPoolProductInstanceBased(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId)) {
				BlockedByBzBug blockedByBzBug = null;
				if (pool.productId.equals("RH00073")) blockedByBzBug = new BlockedByBzBug("1046158");	// Bug 1046158 - Attaching quantity=1 of SKU RH00073 on a 2 socket physical system yields "Only covers 0 of 2 sockets."
				
				// Object bugzilla, Boolean is_guest, String cpu_sockets, SubscriptionPool pool
				ll.add(Arrays.asList(new Object[]{blockedByBzBug,	false,	new Integer(1),	pool}));
				ll.add(Arrays.asList(new Object[]{blockedByBzBug,	false,	new Integer(2),	pool}));
				ll.add(Arrays.asList(new Object[]{blockedByBzBug,	false,	new Integer(5),	pool}));
				ll.add(Arrays.asList(new Object[]{null,				true,	new Integer(1),	pool}));
				ll.add(Arrays.asList(new Object[]{null,				true,	new Integer(2),	pool}));
				ll.add(Arrays.asList(new Object[]{null,				true,	new Integer(5),	pool}));
				
				// keep a quantity map of the instance based pools we are testing
				poolProductIdsQuantityMap.put(pool.productId, Integer.valueOf(pool.quantity));
				continue;
			}
		}
		return ll;
	}
	
	
	
	
	
	
	// Candidates for an automated Test:
	
	
	
	
	// Configuration methods ***********************************************************************


	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
