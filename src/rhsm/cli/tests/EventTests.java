package rhsm.cli.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;

import rhsm.base.ConsumerType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ConsumerCert;
import rhsm.data.EntitlementCert;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.tools.SSHCommandResult;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;



/**
 * @author jsefler
 *
 *
 *
 * https://engineering.redhat.com/trac/Entitlement/wiki/CandlepinRequirements
 *    (A means automated, B means blocked)
Events
   1. The following events should be raised
A         1. Consumer Created
A         2. Consumer Updated
A         3. Consumer Deleted
A         4. Pool Created
A         5. Pool Updated
A         6. Pool Deleted
A         7. Entitlement Created
A            Entitlement Modified
A         8. Entitlement Deleted
B         9. Product Created
B        10. Product Deleted
A        11. Owner Created
A        12. Owner Deleted
A        13. Export Created
A        14. Import Done 
A   2. Events should be consumable via RSS based on owner
A   3. Events should be consumable via RSS based on consumer
   4. AMQP
         1. It should be possible to enable each message type to be published to an AMQP Bus.
         2. It should be possible to publish no messages to the AMQP bus. 


jsefler 7/26/2011
TODO CREATE A TEST TO TRIGGER ALL OF THESE EVENTS.  MANY ARE NEWER THAN WHEN THIS SCRIPT WAS ORIGINALLY WRITTEN
EVENTS FOUND IN /candlepin/proxy/src/main/java/org/fedoraproject/candlepin/audit/EventAdapterImpl.java

MESSAGES.put("CONSUMERCREATED", I18n.marktr("{0} created new consumer {1}"));
MESSAGES.put("CONSUMERMODIFIED", I18n.marktr("{0} modified the consumer {1}"));
MESSAGES.put("CONSUMERDELETED", I18n.marktr("{0} deleted the consumer {1}"));
MESSAGES.put("OWNERCREATED", I18n.marktr("{0} created new owner {1}"));
MESSAGES.put("OWNERMODIFIED", I18n.marktr("{0} modified the owner {1}"));
MESSAGES.put("OWNERDELETED", I18n.marktr("{0} deleted the owner {1}"));
MESSAGES.put("ENTITLEMENTCREATED",
    I18n.marktr("{0} consumed a subscription for product {1}"));
MESSAGES.put("ENTITLEMENTMODIFIED",
    I18n.marktr("{0} modified a subscription for product {1}"));
MESSAGES.put("ENTITLEMENTDELETED",
    I18n.marktr("{0} returned the subscription for {1}"));
MESSAGES.put("POOLCREATED", I18n.marktr("{0} created a pool for product {1}"));
MESSAGES.put("POOLMODIFIED", I18n.marktr("{0} modified a pool for product {1}"));
MESSAGES.put("POOLDELETED", I18n.marktr("{0} deleted a pool for product {1}"));
MESSAGES.put("EXPORTCREATED",
    I18n.marktr("{0} created an export for consumer {1}"));
MESSAGES.put("IMPORTCREATED", I18n.marktr("{0} imported a manifest for owner {1}"));
MESSAGES.put("USERCREATED", I18n.marktr("{0} created new user {1}"));
MESSAGES.put("USERMODIFIED", I18n.marktr("{0} modified the user {1}"));
MESSAGES.put("USERDELETED", I18n.marktr("{0} deleted the user {1}"));
MESSAGES.put("ROLECREATED", I18n.marktr("{0} created new role {1}"));
MESSAGES.put("ROLEMODIFIED", I18n.marktr("{0} modified the role {1}"));
MESSAGES.put("ROLEDELETED", I18n.marktr("{0} deleted the role {1}"));
MESSAGES.put("SUBSCRIPTIONCREATED",
    I18n.marktr("{0} created new subscription for product {1}"));
MESSAGES.put("SUBSCRIPTIONMODIFIED",
    I18n.marktr("{0} modified a subscription for product {1}"));
MESSAGES.put("SUBSCRIPTIONDELETED",
    I18n.marktr("{0} deleted a subscription for product {1}"));
MESSAGES.put("ACTIVATIONKEYCREATED",
    I18n.marktr("{0} created the activation key {1}"));
MESSAGES.put("ACTIVATIONKEYDELETED",
    I18n.marktr("{0} deleted the activation key {1}"));
 */


@Test(groups={"EventTests"})
public class EventTests extends SubscriptionManagerCLITestScript{

	// Test methods ***********************************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21840", "RHEL7-51661"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: Consumer Created is sent over an RSS atom feed.",
			groups={"Tier3Tests","ConsumerCreated_Test"},
			dependsOnGroups={},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testConsumerCreated() throws Exception {
		if (sm_serverAdminUsername.equals("")||sm_serverAdminPassword.equals("")) throw new SkipException("This test requires the candlepin server admin username and password credentials.");

		// start fresh by unregistering
		clienttasks.unregister(null, null, null, null);
		
		// get the owner and consumer feeds before we test the firing of a new event
		String ownerKey = sm_clientOrg;
		SyndFeed oldFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
        SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
 
        // fire a register event
		clienttasks.register(sm_clientUsername,sm_clientPassword,ownerKey,null,null,null,null, null, null, null, (String)null, null, null, null, null, null, null, null, null, null);
		String[] newEventTitles = new String[]{"CONSUMER CREATED"};
		newEventTitles = new String[]{"COMPLIANCE CREATED","COMPLIANCE CREATED","CONSUMER CREATED"};	// COMPLIANCE CREATED events were added to support gutterball
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "0.9.37-1"/*candlepin-common-1.0.17-1*/)) {	// commit bb1d2e6184a6cd9b80ff9c9d3045e9d780116226	// Only send Compliance event when compliance changes
			newEventTitles = new String[]{"COMPLIANCE CREATED","CONSUMER CREATED"};
		}
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.1.1-1"/*TODO change to the next tag 2.1.2-1 once it is added */)) {	// commit 1ad3fd6f338d9bbcedc8eba8361d4bc6c807f84d	1474443 compliance.created events now use UUID for 'consumerId' field
			newEventTitles = new String[]{"CONSUMER CREATED"};
		}
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=721136 - jsefler 07/14/2011
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="721136"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			newEventTitles = new String[]{sm_clientUsername+" created new consumer "+clienttasks.hostname};
		}
		// END OF WORKAROUND
		
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		
		// assert the consumer feed...
		//assertTheNewConsumerFeed(ownerKey, consumerCert.consumerid, null, newEventTitles);
		assertTheNewConsumerFeedIgnoringEventTitles(ownerKey, consumerCert.consumerid, null, newEventTitles,new HashSet<String>(){{add("COMPLIANCE CREATED");}});
		
		// assert the owner feed...
		//assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles);
		assertTheNewOwnerFeedIgnoringEventTitles(ownerKey, oldOwnerFeed, newEventTitles,new HashSet<String>(){{add("COMPLIANCE CREATED");}});
       
		// assert the feed...
		//assertTheNewFeed(oldFeed, newEventTitles);
		assertTheNewFeedIgnoringEventTitles(oldFeed, newEventTitles,new HashSet<String>(){{add("COMPLIANCE CREATED");}});
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21842", "RHEL7-51663"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: Enitlement Created is sent over an RSS atom feed.",
			groups={"Tier3Tests","EntitlementCreated_Test"},
			dependsOnGroups={"ConsumerCreated_Test"},
			enabled=true)
	@ImplementsNitrateTest(caseId=50403)
	public void testEntitlementCreated() throws Exception {
		
		// test prerequisites
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		// 7/30/2012 updating consumer's autoheal to prevent an auto 'ENTITLEMENT CREATED' event
		CandlepinTasks.setAutohealForConsumer(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerCert.consumerid, false);

		// get the owner and consumer feeds before we test the firing of a new event
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,consumerCert.consumerid);
        SyndFeed oldFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
        SyndFeed oldConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(/*ownerKey,*/consumerCert.consumerid,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
 
        // fire a subscribe event
		//SubscriptionPool pool = pools.get(0); // pick the first pool
		testPool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
// debugTesting randomly picked standalone non-zero virt_limit pools
//testPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", "awesomeos-virt-4", pools);
//testPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", "awesomeos-virt-unlimited", pools);
//testPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", "awesomeos-server-basic-dc", pools);
//testPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", "8a90860f5eed6282015eed64099a0193", pools);
//testPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("contract", "", pools);
//testPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("subscriptionType", "Standard (Temporary)", pools);

		//clienttasks.subscribeToSubscriptionPoolUsingPoolId(testPool);	// RHEL59: THIS IS GENERATING EXTRA CONSUMER MODIFIED EVENTS THAT WE DON'T REALLY WANT TO TEST 
		clienttasks.subscribe(null, null, testPool.poolId, null, null, null, null, null, null, null, null, null, null);
		List<String> newEventTitles = new ArrayList<String>();
		//newEventTitles.add("COMPLIANCE CREATED");	// COMPLIANCE CREATED events were added to support gutterball
		newEventTitles.add("ENTITLEMENT CREATED");
		//newEventTitles.add("COMPLIANCE CREATED");
		//newEventTitles.add("COMPLIANCE CREATED");
		
		// TEMPORARY WORKAROUND FOR BUG
		boolean invokeWorkaroundWhileBugIsOpen = false;	// Status: 	CLOSED CURRENTRELEASE
		String bugId="721136"; // jsefler 07/14/2011 Bug 721136 - the content of the atom feeds has the same value for title and description
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			newEventTitles.clear(); newEventTitles.add(clienttasks.hostname+" consumed a subscription for product "+testPool.subscriptionName);
		}
		// END OF WORKAROUND
		
		// assert the consumer feed...
        //assertTheNewConsumerFeed(ownerKey, consumerCert.consumerid, oldConsumerFeed, newEventTitles.toArray(new String[]{}));	// worked prior to RHEL59
        assertTheNewConsumerFeedIgnoringEventTitles(ownerKey, consumerCert.consumerid, oldConsumerFeed, newEventTitles.toArray(new String[]{}), new HashSet<String>(){{add("CONSUMER MODIFIED");add("COMPLIANCE CREATED");}});	// TODO Using the IgnoringEventTitles is a workaround bug 838123#c2
               
        // adjust the expected events when the candlepin server is standalone and the pool has a non-zero virt_limit 
        String virt_limit = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, testPool.poolId, "virt_limit");
		if (servertasks.statusStandalone && virt_limit!=null && !virt_limit.equals("0")) {
			newEventTitles.add(1, "POOL CREATED");
		}
		
		// assert the owner feed...
		//assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles.toArray(new String[]{}));	// worked prior to RHEL59
		assertTheNewOwnerFeedIgnoringEventTitles(ownerKey, oldOwnerFeed, newEventTitles.toArray(new String[]{}), new HashSet<String>(){{add("CONSUMER MODIFIED");add("COMPLIANCE CREATED");}});	// TODO Using the IgnoringEventTitles is a workaround bug 838123#c2
		
		// assert the feed...
		//assertTheNewFeed(oldFeed, newEventTitles.toArray(new String[]{}));	// worked prior to RHEL59
		assertTheNewFeedIgnoringEventTitles(oldFeed, newEventTitles.toArray(new String[]{}), new HashSet<String>(){{add("CONSUMER MODIFIED");add("COMPLIANCE CREATED");}});
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-26757", "RHEL7-52091"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: Pool Modified and Entitlement Modified is sent over an RSS atom feed.",
			groups={"Tier3Tests","blockedByBug-721141","PoolModifiedAndEntitlementModified_Test","blockedByBug-645597","blockedByBug-1303242","blockedByBug-1500837","blockedByBug-1500843"},
			dependsOnGroups={"EntitlementCreated_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testPoolModifiedAndEntitlementModified() throws Exception {
		if (server==null) throw new SkipException("This test requires an SSH connection to the candlepin server."); 

		// get the owner and consumer feeds before we test the firing of a new event
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,consumerCert.consumerid);

		// get the number of subscriptions this owner owns
		//JSONArray jsonSubscriptions = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,clientusername,clientpassword,"/owners/"+ownerKey+"/subscriptions"));	
		
		// find the first pool id of a currently consumed product
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		ProductSubscription originalConsumedProductSubscription = consumedProductSubscriptions.get(0);
		testPool = clienttasks.getSubscriptionPoolFromProductSubscription(originalConsumedProductSubscription,sm_clientUsername,sm_clientPassword);
		Calendar originalStartDate = (Calendar) originalConsumedProductSubscription.startDate.clone();
		EntitlementCert originalEntitlementCert = clienttasks.getEntitlementCertCorrespondingToProductSubscription(originalConsumedProductSubscription);
		originalStartDate = (Calendar) originalEntitlementCert.validityNotBefore.clone();
		
        SyndFeed oldFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
        SyndFeed oldConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(/*ownerKey,*/consumerCert.consumerid,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
        
		// fire an modified pool event (and subsequently a modified entitlement event because the pool was modified thereby requiring an entitlement update dropped to the consumer)
		log.info("To fire a modified pool event (and subsequently a modified entitlement event because the pool is already subscribed too), we will modify pool '"+testPool.poolId+"' by subtracting one month from startdate...");
		Calendar newStartDate = (Calendar) originalStartDate.clone(); newStartDate.add(Calendar.MONTH, -1);
		if (false) {	// the following block was used prior to candlepin-2.0.0 and replaced by CandlepinTasks.updateSubscriptionAndRefreshPoolsUsingRESTfulAPI which I think will also work for pre candlepin-2.0.0, but is untested.  <== TODO
		updateSubscriptionPoolDatesOnDatabase(testPool,newStartDate,null);
		log.info("Now let's refresh the subscription pools to expose the POOL MODIFIED event...");
		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,ownerKey);
		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,jobDetail,"FINISHED", 10*1000, 3);
		} else
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// candlepin commit 9c448315c843c0a20167236af7591359d895613a Discontinue ambiguous subscription resources in sharing world
			CandlepinTasks.updateSubscriptionPoolDatesUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl, testPool.poolId,newStartDate,null);
		} else {
			CandlepinTasks.updateSubscriptionDatesAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl, CandlepinTasks.getSubscriptionIdForPoolId(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl, testPool.poolId),newStartDate,null);
		}
		// assert the consumer feed...
		List<String> newEventTitles = new ArrayList<String>();
		//newEventTitles.add("ENTITLEMENT MODIFIED");
        assertTheNewConsumerFeed(ownerKey, consumerCert.consumerid, oldConsumerFeed, newEventTitles);

		// assert the owner feed...
		////assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, new String[]{"ENTITLEMENT MODIFIED", "POOL MODIFIED"});
		//for (int s=0; s<jsonSubscriptions.length(); s++) newEventTitles.add("POOL MODIFIED");		// NOTE: This is troublesome because the number of POOL MODIFIED events is not this predictable especially when the pool (which is randomly chosen) is a virt pool
		//assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles);
        newEventTitles.add("POOL MODIFIED");
        assertTheNewOwnerFeedContains(ownerKey, oldOwnerFeed, newEventTitles);

		// assert the feed...
		////assertTheNewFeed(oldFeed, new String[]{"ENTITLEMENT MODIFIED", "POOL MODIFIED"});
		//assertTheNewFeed(oldFeed, newEventTitles);
        assertTheNewFeedContains(oldFeed, newEventTitles);
        
		log.info("Now let's refresh the client's entitlements to expose the ENTITLEMENT MODIFIED event...");
		clienttasks.refresh(null, null, null, null);
		newEventTitles.add("COMPLIANCE CREATED");	// COMPLIANCE CREATED events were added to support gutterball
		newEventTitles.add("ENTITLEMENT MODIFIED");
		
		// assert the feed...
        assertTheNewFeedContains(oldFeed, newEventTitles);
        
		// assert the owner feed...
        assertTheNewOwnerFeedContains(ownerKey, oldOwnerFeed, newEventTitles);

		// assert the consumer feed...
        newEventTitles.remove("POOL MODIFIED");
        //assertTheNewConsumerFeed(ownerKey, consumerCert.consumerid, oldConsumerFeed, newEventTitles);
        assertTheNewConsumerFeedIgnoringEventTitles(ownerKey, consumerCert.consumerid, oldConsumerFeed, newEventTitles.toArray(new String[]{}), new HashSet<String>(){{add("COMPLIANCE CREATED");}});
        
		// TEMPORARY WORKAROUND FOR BUG	
		boolean invokeWorkaroundWhileBugIsOpen = true;
		Calendar now = Calendar.getInstance();
		try {String bugId = "883486"; if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("The workaround while this bug is open is to compensate the expected consumed product subscription start date for daylight savings.");
			// adjust the expected entitlement dates for daylight savings time (changed by https://github.com/candlepin/subscription-manager/pull/385)
			// now.get(Calendar.DST_OFFSET) will equal 0 in the winter StandardTime; will equal 1000*60*60 in the summer DaylightSavingsTime (when the local time zone observes DST)
			newStartDate.add(Calendar.MILLISECOND, now.get(Calendar.DST_OFFSET)-newStartDate.get(Calendar.DST_OFFSET));
			newStartDate.add(Calendar.MILLISECOND, now.get(Calendar.DST_OFFSET)-newStartDate.get(Calendar.DST_OFFSET));
		}
		// END OF WORKAROUND
        
        //ProductSubscription newConsumedProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("serialNumber", originalConsumedProductSubscription.serialNumber, clienttasks.getCurrentlyConsumedProductSubscriptions());	// can't do this because the serialNumber changes after the pool and entitlement have been modified
        ProductSubscription newConsumedProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productId", originalConsumedProductSubscription.productId, clienttasks.getCurrentlyConsumedProductSubscriptions());
        //AN org.xmlpull.v1.XmlPullParserException IS THROWN WHEN THIS FAILS: Assert.assertEquals(newConsumedProductSubscription.startDate, newStartDate, "After modifying pool '"+testPool.poolId+"' by subtracting one month from startdate and refreshing entitlements, the consumed product subscription now reflects the modified field.");
        Assert.assertEquals(ProductSubscription.formatDateString(newConsumedProductSubscription.startDate), ProductSubscription.formatDateString(newStartDate), "After modifying pool '"+testPool.poolId+"' by subtracting one month from startdate and refreshing entitlements, the consumed product subscription now reflects the modified field.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21844", "RHEL7-51665"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: Entitlement Deleted is sent over an RSS atom feed.",
			groups={"Tier3Tests","EnititlementDeleted_Test"},
			dependsOnGroups={"PoolModifiedAndEntitlementModified_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void testEnititlementDeleted() throws Exception {
		if (sm_serverAdminUsername.equals("")||sm_serverAdminPassword.equals("")) throw new SkipException("This test requires the candlepin server admin username and password credentials.");

		// get the owner and consumer feeds before we test the firing of a new event
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,consumerCert.consumerid);
        SyndFeed oldFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
        SyndFeed oldConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(/*ownerKey,*/consumerCert.consumerid,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
 
        // fire an unsubscribe event
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		String[] newEventTitles = new String[]{"ENTITLEMENT DELETED"};
		newEventTitles = new String[]{"COMPLIANCE CREATED","COMPLIANCE CREATED","ENTITLEMENT DELETED"};	// COMPLIANCE CREATED events were added to support gutterball
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "0.9.37-1"/*candlepin-common-1.0.17-1*/)) {	// commit bb1d2e6184a6cd9b80ff9c9d3045e9d780116226	// Only send Compliance event when compliance changes
			newEventTitles = new String[]{"COMPLIANCE CREATED","ENTITLEMENT DELETED"};
		}
// TODO FIXME 7/21/2016 assertTheNewConsumerFeedIgnoringEventTitles() call below started failing because the actual newEventTitles occur in reverse order from expected
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.16-1")) {	// probably candlepin commit 6beae873733174df24178a552b116fcb8c8876ef Dont trigger async compliance during batch revoke
			// attempting to reversing the order
			// if this does not help, then we might try to alter the assertTheNew*() methods to IgnoreOrder of just call the assertTheNew**Contains() methods .. 
			newEventTitles = new String[]{"ENTITLEMENT DELETED","COMPLIANCE CREATED"};	// switched the expected order; after discussions with vritant and crog who said the order of the expected new event titles may not be a guaranteed and apparently that's okay
		}
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.1.1-1"/*TODO change to the next tag 2.1.2-1 once it is added */)) {	// commit 1ad3fd6f338d9bbcedc8eba8361d4bc6c807f84d	1474443 compliance.created events now use UUID for 'consumerId' field
			newEventTitles = new String[]{"ENTITLEMENT DELETED"};
		}
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=721136 - jsefler 07/14/2011
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="721136"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			newEventTitles = new String[]{clienttasks.hostname+" returned the subscription for "+testPool.subscriptionName};
		}
		// END OF WORKAROUND
		
		// assert the consumer feed...
        //assertTheNewConsumerFeed(ownerKey, consumerCert.consumerid, oldConsumerFeed, newEventTitles);
        assertTheNewConsumerFeedIgnoringEventTitles(ownerKey, consumerCert.consumerid, oldConsumerFeed, newEventTitles, new HashSet<String>(){{add("COMPLIANCE CREATED");}});
        
        // adjust the expected events when the candlepin server is standalone and the pool has a non-zero virt_limit
        String virt_limit = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, testPool.poolId, "virt_limit");
        if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<", "2.0.16-1"))	// DO NOT INTERFERE WITH THE "switched the expected order" FROM ABOVE
        if (servertasks.statusStandalone && virt_limit!=null && !virt_limit.equals("0")) {
			newEventTitles = new String[]{"COMPLIANCE CREATED","ENTITLEMENT DELETED"};
		}
		
		// assert the owner feed...
		//assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles);
		assertTheNewOwnerFeedIgnoringEventTitles(ownerKey, oldOwnerFeed, newEventTitles, new HashSet<String>(){{add("COMPLIANCE CREATED");}});
		
		// assert the feed...
		//assertTheNewFeed(oldFeed, newEventTitles);
		assertTheNewFeedIgnoringEventTitles(oldFeed, newEventTitles, new HashSet<String>(){{add("COMPLIANCE CREATED");}});
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21845", "RHEL7-51666"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: Consumer Modified is sent over an RSS atom feed.",
			groups={"Tier3Tests","blockedByBug-721141","ConsumerModified_Test"}, dependsOnGroups={"EnititlementDeleted_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testConsumerModified() throws Exception {
		
		// get the owner and consumer feeds before we test the firing of a new event
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,consumerCert.consumerid);
        SyndFeed oldFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
        SyndFeed oldConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(/*ownerKey,*/consumerCert.consumerid,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
 
        // fire an facts update event by overriding a fact in /etc/rhsm/facts/event_tests.facts
		Map<String,String> eventFacts = new HashMap<String,String>();
		eventFacts.put("events.test.description", "Testing CONSUMER MODIFIED event fires on facts update.");
		eventFacts.put("events.test.currentTimeMillis", String.valueOf(System.currentTimeMillis()));
		clienttasks.createFactsFileWithOverridingValues(eventFacts);
		clienttasks.facts(null,true, null, null, null, null);
		// FYI: Another way to fire a consumer modified event is to call CandlepinTasks.setAutohealForConsumer(authenticator, password, url, consumerid, autoheal);
		String[] newEventTitles = new String[]{"CONSUMER MODIFIED"};
		newEventTitles = new String[]{"COMPLIANCE CREATED","CONSUMER MODIFIED"};	// COMPLIANCE CREATED events were added to support gutterball
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.1.1-1"/*TODO change to the next tag 2.1.2-1 once it is added */)) {	// commit 1ad3fd6f338d9bbcedc8eba8361d4bc6c807f84d	1474443 compliance.created events now use UUID for 'consumerId' field
			newEventTitles = new String[]{"CONSUMER MODIFIED"};
		}
		
		// assert the consumer feed...
        //assertTheNewConsumerFeed(ownerKey, consumerCert.consumerid, oldConsumerFeed, newEventTitles);
        assertTheNewConsumerFeedIgnoringEventTitles(ownerKey, consumerCert.consumerid, oldConsumerFeed, newEventTitles, new HashSet<String>(){{add("COMPLIANCE CREATED");}});
        
		// assert the owner feed...
		//assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles);
		assertTheNewOwnerFeedIgnoringEventTitles(ownerKey, oldOwnerFeed, newEventTitles, new HashSet<String>(){{add("COMPLIANCE CREATED");}});
		
		// assert the feed...
		//assertTheNewFeed(oldFeed, newEventTitles);
		assertTheNewFeedIgnoringEventTitles(oldFeed, newEventTitles, new HashSet<String>(){{add("COMPLIANCE CREATED");}});
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21846", "RHEL7-51667"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: Consumer Deleted is sent over an RSS atom feed.",
			groups={"Tier3Tests","ConsumerDeleted_Test"}, dependsOnGroups={"ConsumerModified_Test","NegativeConsumerUserPassword_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testConsumerDeleted() throws Exception {
		
		// get the owner and consumer feeds before we test the firing of a new event
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,consumerCert.consumerid);
		SyndFeed oldFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
        SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
 
        // fire an unregister event
		clienttasks.unregister(null, null, null, null);
		String[] newEventTitles = new String[]{"CONSUMER DELETED"};
		newEventTitles = new String[]{"COMPLIANCE CREATED","CONSUMER DELETED"};	// COMPLIANCE CREATED events were added to support gutterball
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "0.9.37-1"/*candlepin-common-1.0.17-1*/)) {	// commit bb1d2e6184a6cd9b80ff9c9d3045e9d780116226	// Only send Compliance event when compliance changes
			newEventTitles = new String[]{"CONSUMER DELETED"};
		}
		
		// assert the owner feed...
		assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles);
		//assertTheNewOwnerFeedIgnoringEventTitles(ownerKey, oldOwnerFeed, newEventTitles, new HashSet<String>(){{add("COMPLIANCE CREATED");}});
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
		//assertTheNewFeedIgnoringEventTitles(oldFeed, newEventTitles, new HashSet<String>(){{add("COMPLIANCE CREATED");}});
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21847", "RHEL7-51668"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: Owner Created is sent over an RSS atom feed.",
			groups={"Tier3Tests","OwnerCreated_Test"}, dependsOnGroups={"ConsumerDeleted_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void testOwnerCreated() throws Exception {
		if (server==null) throw new SkipException("This test requires an SSH connection to the candlepin server.");
		if (sm_serverAdminUsername.equals("")||sm_serverAdminPassword.equals("")) throw new SkipException("This test requires the candlepin server admin username and password credentials.");
		
		// get the owner and consumer feeds before we test the firing of a new event
		SyndFeed oldFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
 
        // do something that will fire a create owner event
		testJSONOwner = servertasks.createOwnerUsingCPC(testOwnerKey);
		String[] newEventTitles = new String[]{"OWNER CREATED"};
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=721136 - jsefler 07/14/2011
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="721136"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			newEventTitles = new String[]{sm_serverAdminUsername+" created new owner "+testOwnerKey};
		}
		// END OF WORKAROUND
		
		// assert the owner feed...
		assertTheNewOwnerFeed(testJSONOwner.getString("key"), null, newEventTitles);
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6},
			testCaseID= {"RHEL6-26758"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: Product Created is sent over an RSS atom feed.",
			groups={"Tier3Tests","ProductCreated_Test"}, dependsOnGroups={"OwnerCreated_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testProductCreated() throws JSONException, IllegalArgumentException, IOException, FeedException {
		if (server==null) throw new SkipException("This test requires an SSH connection to the candlepin server."); 
		
		// get the owner and consumer feeds before we test the firing of a new event
		SyndFeed oldFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		
        // do something that will fire a create product event
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) {
			// candlepin 2.0 requires an owner key when creating products
			testJSONProduct = servertasks.createProductUsingCPC(testOwnerKey, testProductId, testProductId+" Test Product");
		} else {
			testJSONProduct = servertasks.createProductUsingCPC(testProductId, testProductId+" Test Product");	
		}
		
		String[] newEventTitles = new String[]{"PRODUCT CREATED"};
		
		// WORKAROUND
		if (true) throw new SkipException("09/02/2010 Events for PRODUCT CREATED are not yet dev complete.  Agilo Story: http://mgmt1.rhq.lab.eng.bos.redhat.com:8001/web/ticket/3737");
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21848", "RHEL7-51669"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: Pool Created is sent over an RSS atom feed.",
			groups={"Tier3Tests","PoolCreated_Test"}, dependsOnGroups={"ProductCreated_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void testPoolCreated() throws Exception {
		if (server==null) throw new SkipException("This test requires an SSH connection to the candlepin server."); 
		if (sm_serverAdminUsername.equals("")||sm_serverAdminPassword.equals("")) throw new SkipException("This test requires the candlepin server admin username and password credentials.");

		// get the owner and consumer feeds before we test the firing of a new event
		SyndFeed oldFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);

        // do something that will fire a create pool event
/* DELETEME
		if (servertasks.branch.equals("ALPHA") || servertasks.branch.equals("BETA") || servertasks.branch.matches("^candlepin-0\\.[012]\\..*$")) {
			// candlepin branch 0.2-  (createPoolUsingCPC was deprecated in candlepin branch 0.3+)
			testJSONPool = servertasks.createPoolUsingCPC(testJSONProduct.getString("id"), testProductId+" Test Product", testJSONOwner.getString("id"), "99");
		} else {
			// candlepin branch 0.3+
			testJSONPool = servertasks.createSubscriptionUsingCPC(testOwnerKey, testJSONProduct.getString("id"));
			
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<", "2.0.0")){	// no more refresh pools in candlepin 2.0
				JSONObject jobDetail = servertasks.refreshPoolsUsingCPC(testOwnerKey,true);
				CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,jobDetail,"FINISHED", 10*1000, 3);
			}
		}
*/
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.10")){	// candlepin commit 50d3bdcac7c9ad7f94dfd140a6ebfccc2512ee48	 /root/candlepin/server/client/ruby/candlepin_api.rb:926:in `create_subscription': Deprecated API. Please use create_pool or HostedTest resources (RuntimeError)
			//	201602221354:11.019 - FINE: ssh root@jsefler-f22-candlepin.usersys.redhat.com cd /root/candlepin/server/client/ruby; ./cpc create_subscription "newOwner1456166653160" "newProduct1456166653160"
			//	201602221354:11.960 - FINE: Stdout:  
			//	201602221354:11.960 - FINE: Stderr: 
			//	/root/candlepin/server/client/ruby/candlepin_api.rb:926:in `create_subscription': Deprecated API. Please use create_pool or HostedTest resources (RuntimeError)
			//		from ./cpc:130:in `<main>'
			
			// candlepin-2.0.10+  (createSubscriptionUsingCPC deprecation was enforced in candlepin 2.0.10 commit 50d3bdcac7c9ad7f94dfd140a6ebfccc2512ee48 )
			testJSONPool = servertasks.createPoolUsingCPC(testOwnerKey, testJSONProduct.getString("id"));
		} else if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "0.3")) {
			// candlepin branch 0.3+
			testJSONPool = servertasks.createSubscriptionUsingCPC(testOwnerKey, testJSONProduct.getString("id"));
			
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<", "2.0.0")){	// no need to refresh pools in candlepin 2.0
				JSONObject jobDetail = servertasks.refreshPoolsUsingCPC(testOwnerKey,true);
				CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,jobDetail,"FINISHED", 10*1000, 3);
			}
		} else {
			// candlepin branch 0.2-  (createPoolUsingCPC was deprecated in candlepin branch 0.3+)
			testJSONPool = servertasks.createPoolUsingCPC(testJSONProduct.getString("id"), testProductId+" Test Product", testJSONOwner.getString("id"), "99");
		}
		String[] newEventTitles = new String[]{"POOL CREATED"};

		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=721136 - jsefler 07/14/2011
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="721136"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			newEventTitles = new String[]{"System created a pool for product "+testJSONProduct.getString("name")};
		}
		// END OF WORKAROUND
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
		
		// TODO
//		<jsefler> dgoodwin: your timing for the event for POOL CREATED is perfect.  I'll automate a test for it now.  It looks like the event is working.
//		 dgoodwin dgregor_pto dgao
//		<dgoodwin> jsefler: cool, three possible cases, creation via candlepin api (post /pools), refresh pools after creating a subscription, and creation of the rh personal "sub pool"
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21849", "RHEL7-51670"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: Pool Deleted is sent over an RSS atom feed.",
			groups={"Tier3Tests","PoolDeleted_Test"}, dependsOnGroups={"PoolCreated_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void testPoolDeleted() throws Exception {
		if (server==null) throw new SkipException("This test requires an SSH connection to the candlepin server."); 
		if (sm_serverAdminUsername.equals("")||sm_serverAdminPassword.equals("")) throw new SkipException("This test requires the candlepin server admin username and password credentials.");

		// get the owner and consumer feeds before we test the firing of a new event
		SyndFeed oldFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);

        // do something that will fire a delete pool event
/* DELETEME
		if (servertasks.branch.equals("ALPHA") || servertasks.branch.equals("BETA") || servertasks.branch.matches("^candlepin-0\\.[012]\\..*$")) {
			// candlepin branch 0.2-  (deleteSubscriptionUsingCPC was deprecated in candlepin branch 0.3+)
			servertasks.deletePoolUsingCPC(testJSONPool.getString("id"));
		} else {
			// candlepin branch 0.3+
			servertasks.deleteSubscriptionUsingCPC(testJSONPool.getString("id"));
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<", "2.0.0")){	// no more refresh pools in candlepin 2.0
				JSONObject jobDetail = servertasks.refreshPoolsUsingCPC(testOwnerKey,true);
				CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,jobDetail,"FINISHED", 10*1000, 3);
			}
		}
*/
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.10")){	// candlepin commit 50d3bdcac7c9ad7f94dfd140a6ebfccc2512ee48	 /root/candlepin/server/client/ruby/candlepin_api.rb:934:in `delete_subscription': Deprecated API. Please use delete_pool or HostedTest resources (RuntimeError)
			//	201602221609:59.292 - FINE: ssh root@jsefler-f22-candlepin.usersys.redhat.com cd /root/candlepin/server/client/ruby; ./cpc delete_subscription "8a908790530a4f1801530ad04188158b" (com.redhat.qe.tools.SSHCommandRunner.run)
			//	201602221610:00.269 - FINE: Stdout:  (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
			//	201602221610:00.269 - FINE: Stderr: 
			//	/root/candlepin/server/client/ruby/candlepin_api.rb:934:in `delete_subscription': Deprecated API. Please use delete_pool or HostedTest resources (RuntimeError)
			//		from ./cpc:130:in `<main>'
			
			// candlepin-2.0.10+  (createSubscriptionUsingCPC deprecation was enforced in candlepin 2.0.10 commit 50d3bdcac7c9ad7f94dfd140a6ebfccc2512ee48 )
			servertasks.deletePoolUsingCPC(testJSONPool.getString("id"));
		} else if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "0.3")) {
			// candlepin branch 0.3+
			servertasks.deleteSubscriptionUsingCPC(testJSONPool.getString("id"));
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<", "2.0.0")){	// no more refresh pools in candlepin 2.0
				JSONObject jobDetail = servertasks.refreshPoolsUsingCPC(testOwnerKey,true);
				CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,jobDetail,"FINISHED", 10*1000, 3);
			}
		} else {
			// candlepin branch 0.2-  (deleteSubscriptionUsingCPC was deprecated in candlepin branch 0.3+)
			servertasks.deletePoolUsingCPC(testJSONPool.getString("id"));
		}
		String[] newEventTitles = new String[]{"POOL DELETED"};
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=721136 - jsefler 07/14/2011
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="721136"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			newEventTitles = new String[]{"System deleted a pool for product "+testJSONProduct.getString("name")};
		}
		// END OF WORKAROUND
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6},
			testCaseID= {"RHEL6-26759"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: Product Deleted is sent over an RSS atom feed.",
			groups={"Tier3Tests","ProductDeleted_Test"}, dependsOnGroups={"PoolDeleted_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void testProductDeleted() throws JSONException, IllegalArgumentException, IOException, FeedException {
		if (sm_serverAdminUsername.equals("")||sm_serverAdminPassword.equals("")) throw new SkipException("This test requires the candlepin server admin username and password credentials.");

//		IRC CHAT ON 9/3/2010
//		<dgoodwin> that one is worse =]
//		 products are detached in the db (as they can also live externally)
//		<dgoodwin> and deleting them can leave things dangling, we looked at it earlier, saw how hairy it was, and decided to *not* delete them for now
//		 jsefler: so product delete needs to be handled as separate story, if at all IMO
//		<jsefler> ok, so then for now there is no event firing of DELETE PRODUCT to test
//		<dgoodwin> correct
		
		// WORKAROUND
		if (true) throw new SkipException("09/02/2010 Events for PRODUCT DELETED and the cpc delete_product are not yet dev complete.");
		
		// get the owner and consumer feeds before we test the firing of a new event
		SyndFeed oldFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		
        // do something that will fire a delete product event
		//servertasks.cpc_delete_product(testProduct.getString("id"));
		String[] newEventTitles = new String[]{"PRODUCT DELETED"};
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21850", "RHEL7-51671"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: Export Created is sent over an RSS atom feed.",
			groups={"Tier3Tests","ExportCreated_Test"}, dependsOnGroups={"ProductDeleted_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void testExportCreated() throws Exception {
		if (sm_serverAdminUsername.equals("")||sm_serverAdminPassword.equals("")) throw new SkipException("This test requires the candlepin server admin username and password credentials.");
		
		// start fresh by unregistering
		clienttasks.unregister(null, null, null, null);
		
		// register a type=candlepin consumer and subscribe to get an entitlement
		// NOTE: Without the subscribe, this bugzilla is thrown: 
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">="/*TODO CHANGE TO ">" after candlepin 2.1.2-1 is tagged*/, "2.1.1-1")) {	// candlepin commit 739b51a0d196d9d3153320961af693a24c0b826f Bug 1455361: Disallow candlepin consumers to be registered via Subscription Manager
		    clienttasks.registerCandlepinConsumer(sm_clientUsername,sm_clientPassword,sm_clientOrg,sm_serverUrl,"candlepin");
		} else {
		    clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,ConsumerType.candlepin,null,null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		}
		
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		pools.remove( SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", "mktProductId-93x2", pools));	// avoid "Too many content sets..." from Issue/Bug 1455361 - strange pool availability and bind behavior for consumer of type candlepin
		pools.remove( SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", "mktProductId-186", pools));	// avoid "Too many content sets..." from Issue/Bug 1455361 - strange pool availability and bind behavior for consumer of type candlepin
		testPool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribe(null, null, testPool.poolId, null, null, null, null, null, null, null, null, null, null);
		//String consumerKey = result.getStdout().split(" ")[0];
		
		// get the owner and consumer feeds before we test the firing of a new event
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,consumerCert.consumerid);
        SyndFeed oldFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
        SyndFeed oldConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(/*ownerKey,*/consumerCert.consumerid,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
        
        // do something that will fire a exported created event
		CandlepinTasks.exportConsumerUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,consumerCert.consumerid,"/tmp/export.zip");
		String[] newEventTitles = new String[]{"EXPORT CREATED"};
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=721136 - jsefler 07/14/2011
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="721136"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			newEventTitles = new String[]{ownerKey+" created an export for consumer "+consumerCert.name};
		}
		// END OF WORKAROUND
		
		// assert the consumer feed...
		assertTheNewConsumerFeed(ownerKey, consumerCert.consumerid, oldConsumerFeed, newEventTitles);
		
		// assert the owner feed...
		assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles);
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21851", "RHEL7-51672"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: Import Created is sent over an RSS atom feed.",
			groups={"Tier3Tests","blockedByBug-891334","ImportCreated_Test"}, dependsOnGroups={"ExportCreated_Test","OwnerCreated_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void testImportCreated() throws Exception {
		
		// get the owner and consumer feeds before we test the firing of a new event
		String ownerKey = testJSONOwner.getString("key");
        SyndFeed oldFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey, sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
        
        // do something that will fire an import created event
		CandlepinTasks.importConsumerUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,ownerKey,"/tmp/export.zip");
		String[] newEventTitles = new String[]{"IMPORT CREATED", "POOL CREATED", "SUBSCRIPTION CREATED"};  // Note: the POOL CREATED comes from the subscribed pool
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) {
			newEventTitles = new String[]{"IMPORT CREATED", "POOL CREATED"/*, "SUBSCRIPTION CREATED" is no longer created in candlepin-2.0 */};
		}
		List<String> newEventTitlesList = new ArrayList<String>(); newEventTitlesList.addAll(Arrays.asList(newEventTitles));
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=721136 - jsefler 07/14/2011
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="721136"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			newEventTitles = new String[]{
					sm_clientOrg+" imported a manifest for owner "+ownerKey,
					sm_clientOrg+" created a pool for product "+testPool.subscriptionName,
					sm_clientOrg+" created new subscription for product "+testPool.subscriptionName};
		}
		// END OF WORKAROUND
		
		// assert the owner feed...
		//assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, new String[]{"IMPORT CREATED", "POOL CREATED"});
		//assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles);
		assertTheNewOwnerFeedIgnoringEventTitles(ownerKey, oldOwnerFeed, newEventTitles, new HashSet<String>(){{add("POOL CREATED");}});	// could have one or two "POOL CREATED" events, ignore them
		
		// assert the feed...
		//assertTheNewFeed(oldFeed, new String[]{"IMPORT CREATED", "POOL CREATED", "SUBSCRIPTION CREATED"});
		//assertTheNewFeed(oldFeed, newEventTitles);	// TODO 12/6/2012 several POOL MODIFIED may occur between new events POOL CREATED and SUBSCRIPTION CREATED.  Seems to happen when re-running the script after hours of other troubleshooting runs.  Redeploying candlepin and running EventTests does NOT encounter the extraneous POOL MODIFIED event.  We may want change this to...  assertTheNewFeedContains(oldFeed, Arrays.asList(newEventTitles));
		assertTheNewFeedIgnoringEventTitles(oldFeed, newEventTitles, new HashSet<String>(){{add("POOL CREATED");add("POOL MODIFIED");add("RULES MODIFIED");}} );	// TODO 10/24/2013 don't yet understand why "RULES MODIFIED" have randomly started showing up like the POOL MODIFIED events in the comment above
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21852", "RHEL7-51673"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: Owner Deleted is sent over an RSS atom feed.",
			groups={"Tier3Tests","OwnerDeleted_Test"}, dependsOnGroups={"ImportCreated_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void testOwnerDeleted() throws IllegalArgumentException, IOException, FeedException, JSONException {
		if (server==null) throw new SkipException("This test requires an SSH connection to the candlepin server."); 
		if (sm_serverAdminUsername.equals("")||sm_serverAdminPassword.equals("")) throw new SkipException("This test requires the candlepin server admin username and password credentials.");

		String ownerKey = sm_clientOrg;
		
		// get the owner and consumer feeds before we test the firing of a new event
		SyndFeed oldFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
 
		// do something that will fire a delete owner event
		servertasks.deleteOwnerUsingCPC(testOwnerKey);
		String[] newEventTitles = new String[]{"OWNER DELETED"};
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=721136 - jsefler 07/14/2011
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="721136"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			newEventTitles = new String[]{ownerKey+" deleted the owner "+testOwnerKey};
		}
		// END OF WORKAROUND
		
		// assert the feed...
		//assertTheNewFeed(oldFeed, newEventTitles);
		assertTheNewFeedIgnoringEventTitles(oldFeed, newEventTitles, new HashSet<String>(){{add("POOL DELETED");}} );	// TODO 10/24/2013 don't yet understand why "POOL DELETED" sometimes occurs on script re-runs
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21841", "RHEL7-51662"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: negative test for super user/password.",
			groups={"Tier3Tests","NegativeSuperUserPassword_Test"}, dependsOnGroups={},
			enabled=true, alwaysRun=true)
	@ImplementsNitrateTest(caseId=50404)
	public void testNegativeSuperUserPassword() throws IllegalArgumentException, IOException, FeedException {
		if (sm_serverAdminUsername.equals("")||sm_serverAdminPassword.equals("")) throw new SkipException("This test requires the candlepin server admin username and password credentials.");

		String authuser="",authpwd="";
		try {
			// enter the wrong user, correct passwd
			authuser = sm_serverAdminUsername+getRandInt();
			authpwd = sm_serverAdminPassword;
			CandlepinTasks.getSyndFeed(authuser,authpwd,sm_serverUrl);
			Assert.fail("Expected the candlepin server request for a syndication feed to return an HTTP response code 401 Unauthorized due to invalid authorization credentials "+authuser+":"+authpwd);

		} catch (IOException e) {
			Assert.assertContainsMatch(e.getMessage(), "HTTP response code: 401", "Atom feed is unauthorized when attempting to authorize with credentials "+authuser+":"+authpwd);
		}
		
		try {
			// enter the correct user, wrong passwd
			authuser = sm_serverAdminUsername;
			authpwd = sm_serverAdminPassword+getRandInt();
			CandlepinTasks.getSyndFeed(authuser,authpwd,sm_serverUrl);
			Assert.fail("Expected the candlepin server request for a syndication feed to return an HTTP response code 401 Unauthorized due to invalid authorization credentials "+authuser+":"+authpwd);

		} catch (IOException e) {
			Assert.assertContainsMatch(e.getMessage(), "HTTP response code: 401", "Atom feed is unauthorized when attempting to authorize with credentials "+authuser+":"+authpwd);
		}
		
		// finally assert success with valid credentials
		authuser = sm_serverAdminUsername;
		authpwd = sm_serverAdminPassword;
		SyndFeed feed = CandlepinTasks.getSyndFeed(authuser,authpwd,sm_serverUrl);
		Assert.assertTrue(!feed.getEntries().isEmpty(),"Atom feed for all events is successful with valid credentials "+authuser+":"+authpwd);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21843", "RHEL7-51664"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="subscription-manager: events: negative test for consumer user/password.",
			groups={"Tier3Tests","NegativeConsumerUserPassword_Test"}, dependsOnGroups={"ConsumerCreated_Test"},
			enabled=true)
	@ImplementsNitrateTest(caseId=50404)
	public void testNegativeConsumerUserPassword() throws JSONException, Exception {
		String authuser="",authpwd="";
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		String ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, consumerCert.consumerid);

		try {
			// enter the wrong user, correct passwd
			authuser = sm_client1Username+getRandInt();
			authpwd = sm_client1Password;
			CandlepinTasks.getSyndFeedForConsumer(/*ownerKey,*/consumerCert.consumerid,authuser,authpwd,sm_serverUrl);
			Assert.fail("Expected the candlepin server request for a syndication feed to return an HTTP response code 401 Unauthorized due to invalid authorization credentials "+authuser+":"+authpwd);

		} catch (IOException e) {
			Assert.assertContainsMatch(e.getMessage(), "HTTP response code: 401", "Atom feed is unauthorized when attempting to authorize with credentials "+authuser+":"+authpwd);
		}
		
		try {
			// enter the correct user, wrong passwd
			authuser = sm_client1Username;
			authpwd = sm_client1Password+getRandInt();
			CandlepinTasks.getSyndFeedForConsumer(/*ownerKey,*/consumerCert.consumerid,authuser,authpwd,sm_serverUrl);
			Assert.fail("Expected the candlepin server request for a syndication feed to return an HTTP response code 401 Unauthorized due to invalid authorization credentials "+authuser+":"+authpwd);

		} catch (IOException e) {
			Assert.assertContainsMatch(e.getMessage(), "HTTP response code: 401", "Atom feed is unauthorized when attempting to authorize with credentials "+authuser+":"+authpwd);
		}
		
		try {
			// enter the correct user, correct passwd for super admin atom
			authuser = sm_client1Username;
			authpwd = sm_client1Password;
			CandlepinTasks.getSyndFeed(authuser,authpwd,sm_serverUrl);
			Assert.fail("Expected the candlepin server request for a syndication feed to return an HTTP response code 401 Unauthorized due to invalid authorization credentials "+authuser+":"+authpwd);

		} catch (IOException e) {
			Assert.assertContainsMatch(e.getMessage(), "HTTP response code: 403", "Atom feed is forbidden when attempting to authorize with credentials "+authuser+":"+authpwd);
		}
		
		// finally assert success with valid credentials
		authuser = sm_client1Username;
		authpwd = sm_client1Password;
		SyndFeed feed = CandlepinTasks.getSyndFeedForConsumer(/*ownerKey,*/ consumerCert.consumerid, authuser, authpwd, sm_serverUrl);
		Assert.assertTrue(!feed.getEntries().isEmpty(),"Atom feed for consumer events is successful with valid credentials "+authuser+":"+authpwd);
	}

	
	
	// Configuration Methods ***********************************************************************

// TODO I DON'T THINK THIS CONFIGURATION METHOD IS NEEDED ANYMORE, AND I DON'T REMEMBER WHY IT WAS ORIGINALLY NEEDED.  1/2/2013 jsefler
//	@BeforeClass(groups="setup")
//	public void setupBeforeClass() throws Exception {
//		// alternative to dependsOnGroups={"RegisterWithCredentials_Test"}
//		// This allows us to satisfy a dependency on registrationDataList making TestNG add unwanted Test results.
//		// This also allows us to individually run this Test Class on Hudson.
//		RegisterWithCredentials_Test(); // needed to populate registrationDataList
//	}
	
	
	
	// Protected Methods ***********************************************************************
	
	protected void assertTheNewOwnerFeed(String ownerKey, SyndFeed oldOwnerFeed, String[] newEventTitles) throws JSONException, Exception {
		
		// assert the owner feed...
		SyndFeed newOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey, sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		Assert.assertEquals(newOwnerFeed.getTitle(),"Event feed for owner "+CandlepinTasks.getOrgDisplayNameForOrgKey(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,ownerKey));
		assertFeedContainsNoUnknownEvents(newOwnerFeed);
		
		log.info("Expecting the new feed for owner ("+ownerKey+") to have grown by ("+newEventTitles.length+") events:");
		int e=0;
		for (String newEventTitle : newEventTitles) log.info(String.format("  Expecting entry[%d].title %s",e++,newEventTitle));
		
		//Assert.assertEquals(newOwnerFeed.getEntries().size(), oldOwnerFeed_EntriesSize+newEventTitles.length, "The event feed length for owner '"+ownerKey+"' has increased by "+newEventTitles.length+" entries.");
//		if (oldOwnerFeed_EntriesSize+newEventTitles.length <= feedLimit) {
//			Assert.assertEquals(newOwnerFeed.getEntries().size(), oldOwnerFeed_EntriesSize+newEventTitles.length, "The event feed length for owner '"+ownerKey+"' has increased by "+newEventTitles.length+" entries.");
//		} else {
//			Assert.assertEquals(newOwnerFeed.getEntries().size(), feedLimit, "The event feed length for owner '"+ownerKey+"' has hit the max entry count as set by the Candlepin class AtomResource/ConsumerResource/OwnerResource hard-coded variable feedlimit.");			
//		}
		Assert.assertEquals(getFeedGrowthCount(newOwnerFeed,oldOwnerFeed), newEventTitles.length, newEventTitles.length+" new event feed entries for owner '"+ownerKey+"' has been pushed onto the stack.");

		int i=0;
		for (String newEventTitle : newEventTitles) {
			String actualEventTitle = ((SyndEntryImpl) newOwnerFeed.getEntries().get(i)).getTitle();
			Assert.assertEquals(actualEventTitle,newEventTitle, "The next ("+i+") newest event feed entry for owner '"+ownerKey+"' is '"+newEventTitle+"'.");
			i++;
		}
	}
	protected void assertTheNewOwnerFeedIgnoringEventTitles(String ownerKey, SyndFeed oldOwnerFeed, String[] newEventTitles, Set<String> ignoreEventTitles) throws JSONException, Exception {
		
		// assert the owner feed...
		SyndFeed newOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey, sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		Assert.assertEquals(newOwnerFeed.getTitle(),"Event feed for owner "+CandlepinTasks.getOrgDisplayNameForOrgKey(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,ownerKey));
		assertFeedContainsNoUnknownEvents(newOwnerFeed);
		
		log.info("Expecting the new feed for owner ("+ownerKey+") to have grown by at least ("+newEventTitles.length+") events ("+ignoreEventTitles+" events will be ignored):");
		int e=0;
		for (String newEventTitle : newEventTitles) log.info(String.format("  Expecting entry[%d].title %s",e++,newEventTitle));
		
		int feedGrowthCount = getFeedGrowthCount(newOwnerFeed,oldOwnerFeed);
		Assert.assertTrue(feedGrowthCount>=newEventTitles.length, "At least "+newEventTitles.length+" new event feed entries for owner '"+ownerKey+"' has been pushed onto the stack (actual="+feedGrowthCount+").");

		int i=0;
		for (String newEventTitle : newEventTitles) {
			String actualEventTitle = ((SyndEntryImpl) newOwnerFeed.getEntries().get(i)).getTitle();
			if (ignoreEventTitles.contains(actualEventTitle)) {
				log.warning("The next ("+i+") newest event feed entry for owner '"+ownerKey+" is '"+actualEventTitle+"', and will be ignored.");
			} else {
				Assert.assertEquals(actualEventTitle,newEventTitle, "The next ("+i+") newest event feed entry for owner '"+ownerKey+"' is '"+newEventTitle+"'.");
			}
			i++;
		}
	}
	/**
	 * Same as assertTheNewOwnerFeed(String ownerKey, SyndFeed oldOwnerFeed, String[] newEventTitles), but without regard to order of newEventTitles
	 */
	protected void assertTheNewOwnerFeed(String ownerKey, SyndFeed oldOwnerFeed, List<String> newEventTitles) throws JSONException, Exception {
		
		// assert the owner feed...
		SyndFeed newOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey, sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		Assert.assertEquals(newOwnerFeed.getTitle(),"Event feed for owner "+CandlepinTasks.getOrgDisplayNameForOrgKey(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,ownerKey));
		assertFeedContainsNoUnknownEvents(newOwnerFeed);
		
		log.info("Expecting the new feed for owner ("+ownerKey+") to have grown by the following "+newEventTitles.size()+" events (in no particular order): ");
		for (String newEventTitle : newEventTitles) log.info("    "+newEventTitle);
		
		//Assert.assertEquals(newOwnerFeed.getEntries().size(), oldOwnerFeed_EntriesSize+newEventTitles.size(), "The event feed length for owner '"+ownerKey+"' has increased by "+newEventTitles.size()+" entries.");
//		if (oldOwnerFeed_EntriesSize+newEventTitles.size() <= feedLimit) {
//			Assert.assertEquals(newOwnerFeed.getEntries().size(), oldOwnerFeed_EntriesSize+newEventTitles.size(), "The event feed length for owner '"+ownerKey+"' has increased by "+newEventTitles.size()+" entries.");
//		} else {
//			Assert.assertEquals(newOwnerFeed.getEntries().size(), feedLimit, "The event feed length for owner '"+ownerKey+"' has hit the max entry count as set by the Candlepin class AtomResource/ConsumerResource/OwnerResource hard-coded variable feedlimit.");			
//		}
		Assert.assertEquals(getFeedGrowthCount(newOwnerFeed,oldOwnerFeed), newEventTitles.size(), +newEventTitles.size()+" new event feed entries for owner '"+ownerKey+"' has been pushed onto the stack.");

		List<String> newEventTitlesCloned = new ArrayList<String>(); for (String newEventTitle : newEventTitles) newEventTitlesCloned.add(newEventTitle);
		for (int i=0; i<newEventTitles.size(); i++) {
			String actualEventTitle = ((SyndEntryImpl) newOwnerFeed.getEntries().get(i)).getTitle();
			Assert.assertTrue(newEventTitlesCloned.remove(actualEventTitle), "The next ("+i+") newest event feed entry ("+actualEventTitle+") for owner '"+ownerKey+"' is among the expected list of event titles.");
		}
	}

	protected void assertTheNewOwnerFeedContains(String ownerKey, SyndFeed oldOwnerFeed, List<String> newEventTitles) throws JSONException, Exception {
		
		// assert the owner feed...
		SyndFeed newOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey, sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		Assert.assertEquals(newOwnerFeed.getTitle(),"Event feed for owner "+CandlepinTasks.getOrgDisplayNameForOrgKey(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,ownerKey));
		assertFeedContainsNoUnknownEvents(newOwnerFeed);
		
		log.info("Expecting the new feed for owner ("+ownerKey+") to have grown by events that contain (at a minimum) the following events: ");
		for (String newEventTitle : newEventTitles) log.info("    "+newEventTitle);
		
//		int newOwnerFeedGrowthCount = newOwnerFeed.getEntries().size() - oldOwnerFeed_EntriesSize;
		int newOwnerFeedGrowthCount = getFeedGrowthCount(newOwnerFeed,oldOwnerFeed);
		log.info(newOwnerFeedGrowthCount+" new events for owner '"+ownerKey+"' have been pushed onto the atom feed stack.");
		List<String> actualNewEventTitles = new ArrayList<String>();
		for (int i=0; i<newOwnerFeedGrowthCount; i++) {
			actualNewEventTitles.add(((SyndEntryImpl) newOwnerFeed.getEntries().get(i)).getTitle());
		}
		Assert.assertTrue(actualNewEventTitles.containsAll(newEventTitles), "The newest event feed entries for owner '"+ownerKey+"' contains (at a minimum) all of the expected new event titles.");
	}
	
	
	
	
	
	protected void assertTheNewConsumerFeed(String ownerKey, String consumerUuid, SyndFeed oldConsumerFeed, String[] newEventTitles) throws IllegalArgumentException, IOException, FeedException {
		
		// assert the consumer feed...
		SyndFeed newConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(/*ownerKey,*/ consumerUuid, sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		Assert.assertEquals(newConsumerFeed.getTitle(),"Event feed for consumer "+consumerUuid);
		assertFeedContainsNoUnknownEvents(newConsumerFeed);
		
		log.info("Expecting the new feed for consumer ("+consumerUuid+") to have grown by ("+newEventTitles.length+") events:");
		int e=0;
		for (String newEventTitle : newEventTitles) log.info(String.format("  Expecting entry[%d].title %s",e++,newEventTitle));

		//Assert.assertEquals(newConsumerFeed.getEntries().size(), oldConsumerFeed_EntriesSize+newEventTitles.length, "The event feed for consumer "+consumerUuid+" has increased by "+newEventTitles.length+" entries.");
//		if (oldConsumerFeed_EntriesSize+newEventTitles.length <= feedLimit) {
//			Assert.assertEquals(newConsumerFeed.getEntries().size(), oldConsumerFeed_EntriesSize+newEventTitles.length, "The event feed for consumer "+consumerUuid+" has increased by "+newEventTitles.length+" entries.");
//		} else {
//			Assert.assertEquals(newConsumerFeed.getEntries().size(), feedLimit, "The event feed length for consumer '"+consumerUuid+"' has hit the max entry count as set by the Candlepin class AtomResource/ConsumerResource/OwnerResource hard-coded variable feedlimit.");			
//		}
		System.out.println("newEventTitles.length is ...."+ newEventTitles.length);
		Assert.assertEquals(getFeedGrowthCount(newConsumerFeed,oldConsumerFeed), newEventTitles.length, newEventTitles.length+" new event feed entries for consumer '"+consumerUuid+"' has been pushed onto the stack.");

		int i=0;
		for (String newEventTitle : newEventTitles) {
			String actualEventTitle = ((SyndEntryImpl) newConsumerFeed.getEntries().get(i)).getTitle();
			Assert.assertEquals(actualEventTitle,newEventTitle, "The next ("+i+") newest event feed entry for consumer "+consumerUuid+" is '"+newEventTitle+"'.");
			i++;
		}
	}
	protected void assertTheNewConsumerFeedIgnoringEventTitles(String ownerKey, String consumerUuid, SyndFeed oldConsumerFeed, String[] newEventTitles, Set<String> ignoreEventTitles) throws IllegalArgumentException, IOException, FeedException {
		
		// assert the consumer feed...
		SyndFeed newConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(/*ownerKey,*/ consumerUuid, sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		Assert.assertEquals(newConsumerFeed.getTitle(),"Event feed for consumer "+consumerUuid);
		assertFeedContainsNoUnknownEvents(newConsumerFeed);
		
		log.info("Expecting the new feed for consumer ("+consumerUuid+") to have grown by at least ("+newEventTitles.length+") events ("+ignoreEventTitles+" events will be ignored):");
		int e=0;
		for (String newEventTitle : newEventTitles) log.info(String.format("  Expecting entry[%d].title %s",e++,newEventTitle));
		int feedGrowthCount = getFeedGrowthCount(newConsumerFeed,oldConsumerFeed);

		Assert.assertTrue(feedGrowthCount>=newEventTitles.length, "At least "+newEventTitles.length+" new event feed entries for consumer '"+consumerUuid+"' have been pushed onto the stack (actual="+feedGrowthCount+").");

		int i=0;
		for (String newEventTitle : newEventTitles) {
			String actualEventTitle = ((SyndEntryImpl) newConsumerFeed.getEntries().get(i)).getTitle();
			if (ignoreEventTitles.contains(actualEventTitle)) {
				log.warning("The next ("+i+") newest event feed entry for consumer "+consumerUuid+" is '"+actualEventTitle+"', and will be ignored.");
			} else {
				Assert.assertEquals(actualEventTitle,newEventTitle, "The next ("+i+") newest event feed entry for consumer "+consumerUuid+" is '"+newEventTitle+"'.");
			}
			i++;
		}
	}
	/**
	 * Same as assertTheNewConsumerFeed(String ownerKey, String consumerUuid, SyndFeed oldConsumerFeed, String[] newEventTitles), but without regard to order of newEventTitles
	 */
	protected void assertTheNewConsumerFeed(String ownerKey, String consumerUuid, SyndFeed oldConsumerFeed, List<String> newEventTitles) throws IllegalArgumentException, IOException, FeedException {
		
		// assert the consumer feed...
		SyndFeed newConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(/*ownerKey,*/ consumerUuid, sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
		Assert.assertEquals(newConsumerFeed.getTitle(),"Event feed for consumer "+consumerUuid);
		assertFeedContainsNoUnknownEvents(newConsumerFeed);
		
		log.info("Expecting the new feed for consumer ("+consumerUuid+") to have grown by the following "+newEventTitles.size()+" events (in no particular order): ");
		for (String newEventTitle : newEventTitles) log.info("    "+newEventTitle);

		//Assert.assertEquals(newConsumerFeed.getEntries().size(), oldConsumerFeed_EntriesSize+newEventTitles.size(), "The event feed for consumer "+consumerUuid+" has increased by "+newEventTitles.size()+" entries.");
//		if (oldConsumerFeed_EntriesSize+newEventTitles.size() <= feedLimit) {
//			Assert.assertEquals(newConsumerFeed.getEntries().size(), oldConsumerFeed_EntriesSize+newEventTitles.size(), "The event feed for consumer "+consumerUuid+" has increased by "+newEventTitles.size()+" entries.");
//		} else {
//			Assert.assertEquals(newConsumerFeed.getEntries().size(), feedLimit, "The event feed length for consumer '"+consumerUuid+"' has hit the max entry count as set by the Candlepin class AtomResource/ConsumerResource/OwnerResource hard-coded variable feedlimit.");			
//		}
		Assert.assertEquals(getFeedGrowthCount(newConsumerFeed,oldConsumerFeed), newEventTitles.size(), newEventTitles.size()+" new event feed entries for consumer '"+consumerUuid+"' has been pushed onto the stack.");

		List<String> newEventTitlesCloned = new ArrayList<String>(); for (String newEventTitle : newEventTitles) newEventTitlesCloned.add(newEventTitle);
		for (int i=0; i<newEventTitles.size(); i++) {
			String actualEventTitle = ((SyndEntryImpl) newConsumerFeed.getEntries().get(i)).getTitle();
			Assert.assertTrue(newEventTitlesCloned.remove(actualEventTitle), "The next ("+i+") newest event feed entry ("+actualEventTitle+") for consumer "+consumerUuid+" is among the expected list of event titles.");
		}
	}

	

	

	
	protected void assertTheNewFeed(SyndFeed oldFeed, String[] newEventTitles) throws IllegalArgumentException, IOException, FeedException {
		
		// assert the feed...
		SyndFeed newFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);		
		Assert.assertEquals(newFeed.getTitle(),"Event Feed");
		assertFeedContainsNoUnknownEvents(newFeed);

		log.info("Expecting the new feed to have grown by ("+newEventTitles.length+") events:");
		int e=0;
		for (String newEventTitle : newEventTitles) log.info(String.format("  Expecting entry[%d].title %s",e++,newEventTitle));

		//Assert.assertEquals(newConsumerFeed.getEntries().size(), oldFeed_EntriesSize+newEventTitles.length, "The event feed entries has increased by "+newEventTitles.length);
//		if (oldFeed_EntriesSize+newEventTitles.length <= feedLimit) {
//			Assert.assertEquals(newFeed.getEntries().size(), oldFeed_EntriesSize+newEventTitles.length, "The event feed entries has increased by "+newEventTitles.length);
//		} else {
//			Assert.assertEquals(newFeed.getEntries().size(), feedLimit, "The event feed has hit the max entry count as set by the Candlepin class AtomResource/ConsumerResource/OwnerResource hard-coded variable feedlimit.");			
//		}
		Assert.assertEquals(getFeedGrowthCount(newFeed,oldFeed), newEventTitles.length, newEventTitles.length+" new event feed entries has been pushed onto the stack.");

		int i=0;
		for (String newEventTitle : newEventTitles) {
			String actualEventTitle = ((SyndEntryImpl) newFeed.getEntries().get(i)).getTitle();
			Assert.assertEquals(actualEventTitle,newEventTitle, "The next ("+i+") newest event feed entry is '"+newEventTitle+"'.");
			i++;
		}
	}
	protected void assertTheNewFeedIgnoringEventTitles(SyndFeed oldFeed, String[] newEventTitles, Set<String> ignoreEventTitles) throws IllegalArgumentException, IOException, FeedException {
		
		// assert the feed...
		SyndFeed newFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);		
		Assert.assertEquals(newFeed.getTitle(),"Event Feed");
		assertFeedContainsNoUnknownEvents(newFeed);

		log.info("Expecting the new feed to have grown by ("+newEventTitles.length+") events:");
		int e=0;
		for (String newEventTitle : newEventTitles) log.info(String.format("  Expecting entry[%d].title %s",e++,newEventTitle));

		int feedGrowthCount = getFeedGrowthCount(newFeed,oldFeed);
		Assert.assertTrue(feedGrowthCount>=newEventTitles.length, "At least "+newEventTitles.length+" new event feed entries has been pushed onto the stack (actual="+feedGrowthCount+").");

		int i=0;
		for (String newEventTitle : newEventTitles) {
			String actualEventTitle = ((SyndEntryImpl) newFeed.getEntries().get(i)).getTitle();
			if (ignoreEventTitles.contains(actualEventTitle)) {
				log.warning("The next ("+i+") newest event feed entry is '"+actualEventTitle+"', and will be ignored.");
			} else {
				Assert.assertEquals(actualEventTitle,newEventTitle, "The next ("+i+") newest event feed entry is '"+newEventTitle+"'.");
			}
			i++;
		}
	}
	/**
	 * Same as assertTheNewFeed(SyndFeed oldFeed, String[] newEventTitles), but without regard to order of newEventTitles
	 */
	protected void assertTheNewFeed(SyndFeed oldFeed, List<String> newEventTitles) throws IllegalArgumentException, IOException, FeedException {
		
		// assert the feed...
		SyndFeed newFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);		
		Assert.assertEquals(newFeed.getTitle(),"Event Feed");
		assertFeedContainsNoUnknownEvents(newFeed);

		log.info("Expecting the new feed to have grown by the following "+newEventTitles.size()+" events (in no particular order): ");
		for (String newEventTitle : newEventTitles) log.info("    "+newEventTitle);
		
		//Assert.assertEquals(newFeed.getEntries().size(), oldFeed_EntriesSize+newEventTitles.size(), "The event feed entries has increased by "+newEventTitles.size());
//		if (oldFeed_EntriesSize+newEventTitles.size() <= feedLimit) {
//			Assert.assertEquals(newFeed.getEntries().size(), oldFeed_EntriesSize+newEventTitles.size(), "The event feed entries has increased by "+newEventTitles.size());
//		} else {
//			Assert.assertEquals(newFeed.getEntries().size(), feedLimit, "The event feed has hit the max entry count as set by the Candlepin class AtomResource/ConsumerResource/OwnerResource hard-coded variable feedlimit.");			
//		}
		Assert.assertEquals(getFeedGrowthCount(newFeed,oldFeed), newEventTitles.size(), newEventTitles.size()+" new event feed entries has been pushed onto the stack.");

		List<String> newEventTitlesCloned = new ArrayList<String>(); for (String newEventTitle : newEventTitles) newEventTitlesCloned.add(newEventTitle);
		for (int i=0; i<newEventTitles.size(); i++) {
			String actualEventTitle = ((SyndEntryImpl) newFeed.getEntries().get(i)).getTitle();
			Assert.assertTrue(newEventTitlesCloned.remove(actualEventTitle), "The next ("+i+") newest event feed entry ("+actualEventTitle+") is among the expected list of event titles.");
		}
	}
	
	protected void assertTheNewFeedContains(SyndFeed oldFeed, List<String> newEventTitles) throws IllegalArgumentException, IOException, FeedException {
		
		// assert the feed...
		SyndFeed newFeed = CandlepinTasks.getSyndFeed(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);		
		Assert.assertEquals(newFeed.getTitle(),"Event Feed");
		assertFeedContainsNoUnknownEvents(newFeed);
		
		log.info("Expecting the new feed to have grown by events that contain (at a minimum) the following events: ");
		for (String newEventTitle : newEventTitles) log.info("    "+newEventTitle);
		
//		int newFeedGrowthCount = newFeed.getEntries().size() - oldFeed_EntriesSize;
		int newFeedGrowthCount = getFeedGrowthCount(newFeed,oldFeed);
		log.info(newFeedGrowthCount+" new events have been pushed onto the atom feed stack.");
		List<String> actualNewEventTitles = new ArrayList<String>();
		for (int i=0; i<newFeedGrowthCount; i++) {
			actualNewEventTitles.add(((SyndEntryImpl) newFeed.getEntries().get(i)).getTitle());
		}
		Assert.assertTrue(actualNewEventTitles.containsAll(newEventTitles), "The newest event feed entries contains (at a minimum) all of the expected new event titles.");
	}
	
	
	
	
	
	protected void assertFeedContainsNoUnknownEvents(SyndFeed feed) {
	
		// assert that there are no "Unknown event"s in the feed - reference https://bugzilla.redhat.com/show_bug.cgi?id=721141
		for (int i=0;  i<feed.getEntries().size(); i++) {
			String entryTitle = ((SyndEntryImpl) feed.getEntries().get(i)).getTitle();
			String entryDescription = ((SyndEntryImpl) feed.getEntries().get(i)).getDescription()==null?"null":((SyndEntryImpl) feed.getEntries().get(i)).getDescription().getValue();
			String entryAsString = String.format("%s entries[%d].getTitle()=%s   .getDescription()=%s", feed.getTitle(), i, entryTitle, entryDescription);
			
			// TEMPORARY WORKAROUND FOR BUG
			if (entryDescription.equals("Unknown event for user admin and target 4.4")) {
				String bugId = "1023187"; boolean invokeWorkaroundWhileBugIsOpen = true;
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Encountered an atom feed entry with an unknown event: "+entryAsString);
					log.warning("Skipping assertion failure while Bug '"+bugId+"' is already opened to handle this unknown event description.");
					continue;
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG
			if (entryDescription.equals("null")) {
				String bugId = "1578968"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1578968 - candlepin atom feeds are missing the description field 
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Encountered an atom feed entry with a '"+entryDescription+"' event");
					log.warning("Skipping non-null for an atom feed entry description while Bug '"+bugId+"' is open.");
					continue;
				}
			}
			// END OF WORKAROUND
			
			if (entryDescription.toLowerCase().contains("unknown event")) {
				Assert.fail("Encountered an atom feed entry with an unknown event: "+entryAsString);
			}
			if (entryDescription.equalsIgnoreCase("null")) {
				Assert.fail("Encountered an atom feed entry with a null description: "+entryAsString);			
			}
		}
		Assert.assertTrue(true,"None of the entries in feed '"+feed.getTitle()+"' contain an unknown event description.");
	}
	
	
	protected int getFeedGrowthCount(SyndFeed newFeed, SyndFeed oldFeed) {
		int g=0; // growth
		int x=10; // number of entries in a sequential match
		
		if (oldFeed==null || oldFeed.getEntries()==null || oldFeed.getEntries().isEmpty()) {
			g = newFeed.getEntries().size();
		} else {
			x = Math.min(10, oldFeed.getEntries().size());
		}
		
		if (g==0) {
			boolean sequenceMatches;
			do {
				sequenceMatches = true;	// assume the next sequence of x feed entries match between old and new
				for (int i=0; i<x; i++) {
					String newFeedTitle = ((SyndEntryImpl) newFeed.getEntries().get(i+g)).getTitle();
					String newFeedDescription = ((SyndEntryImpl) newFeed.getEntries().get(i+g)).getDescription()==null?"null":((SyndEntryImpl) newFeed.getEntries().get(i+g)).getDescription().getValue();
					String oldFeedTitle = ((SyndEntryImpl) oldFeed.getEntries().get(i)).getTitle();
					String oldFeedDescription = ((SyndEntryImpl) oldFeed.getEntries().get(i)).getDescription()==null?"null":((SyndEntryImpl) oldFeed.getEntries().get(i)).getDescription().getValue();
					if (!(newFeedTitle.equals(oldFeedTitle) && newFeedDescription.equals(oldFeedDescription))) {
						sequenceMatches = false;
						g++;
						break;
					}
				}
			} while (!sequenceMatches);
		}
		
		// log the newest feed entries pushed onto the stack
		for (int i=0; i<g; i++) {
			log.info(String.format("Newest %s entries[%d]: title='%s'  description='%s'", newFeed.getTitle(), i, ((SyndEntryImpl) newFeed.getEntries().get(i)).getTitle(), ((SyndEntryImpl) newFeed.getEntries().get(i)).getDescription()==null?"null":((SyndEntryImpl) newFeed.getEntries().get(i)).getDescription().getValue()));
		}

		return g;
	}
	
	@Deprecated // 10/24/2013 re-implemented this method with a faster algorithm above.  this algorithm is too complicated!
	protected int getFeedGrowthCount_DEPRECATED(SyndFeed newFeed, SyndFeed oldFeed) {
		
		int g=newFeed.getEntries().size();	// assume all of the entries represent new growth
		
		if (oldFeed!=null) {
			// make sure we are not accidently comparing two feeds that represent two different stacks
			if (!newFeed.getTitle().equals(oldFeed.getTitle())) Assert.fail("getFeedGrowthCount(oldFeed,newFeed) do not have equivalent SyndEntryImpl titles.  We should probably not be comparing these two feeds.");

			int n=newFeed.getEntries().size()-1;
			int o=oldFeed.getEntries().size()-1;
			g=0;	// feed growth 
			while (g<o+1) {
				int c=0;	// backward feed index count of newFeed
				while (o-c-g>=0 &&
						// newFeedTitle == oldFeedTitle
						(((SyndEntryImpl) newFeed.getEntries().get(n-c)).getTitle()).equals(((SyndEntryImpl) oldFeed.getEntries().get(o-c-g)).getTitle()) &&
						// newFeedDescription == oldFeedDescription
						(((SyndEntryImpl) newFeed.getEntries().get(n-c)).getDescription()==null?"null":((SyndEntryImpl) newFeed.getEntries().get(n-c)).getDescription().getValue()).equals(((SyndEntryImpl) oldFeed.getEntries().get(o-c-g)).getDescription()==null?"null":((SyndEntryImpl) oldFeed.getEntries().get(o-c-g)).getDescription().getValue()) ) {
/*debugging*/
//					log.info("newFeed entry["+(n-c)+"].getTitle()= "+((SyndEntryImpl) newFeed.getEntries().get(n-c)).getTitle());
//					log.info("oldFeed entry["+(o-c-g)+"].getTitle()= "+((SyndEntryImpl) oldFeed.getEntries().get(o-c-g)).getTitle());
//					log.info("newFeed entry["+(n-c)+"].getDescription()= "+(((SyndEntryImpl) newFeed.getEntries().get(n-c)).getDescription()==null?"null":((SyndEntryImpl) newFeed.getEntries().get(n-c)).getDescription().getValue())  );
//					log.info("oldFeed entry["+(o-c-g)+"].getDescription()= "+(((SyndEntryImpl) oldFeed.getEntries().get(o-c-g)).getDescription()==null?"null":((SyndEntryImpl) oldFeed.getEntries().get(o-c-g)).getDescription().getValue())  );
//					log.info("");
					c++;
				}
				if (o-c-g<0) {
					g+=n-o;
					break;
				}
				g++;
			}
		}
		
		// log the newest feed entries pushed onto the stack
		for (int i=0; i<g; i++) {
			log.info(String.format("Newest %s entries[%d]: title='%s'  description='%s'", newFeed.getTitle(), i, ((SyndEntryImpl) newFeed.getEntries().get(i)).getTitle(), ((SyndEntryImpl) newFeed.getEntries().get(i)).getDescription()==null?"null":((SyndEntryImpl) newFeed.getEntries().get(i)).getDescription().getValue()));
		}

		return g;
	}
	
	protected String testOwnerKey = "newOwner"+System.currentTimeMillis();
	protected JSONObject testJSONOwner;
	protected String testProductId = "newProduct"+System.currentTimeMillis();
	protected JSONObject testJSONProduct;
	protected String testPoolId = "newPool"+System.currentTimeMillis();
	protected JSONObject testJSONPool;
	protected int feedLimit = 1000;
	SubscriptionPool testPool = null;
	
	
	// Data Providers ***********************************************************************

}
