package com.redhat.qe.sm.tests;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.sm.data.ConsumerCert;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.sm.tasks.CandlepinTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.abstraction.AbstractCommandLineData;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;



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
A         14. Import Done 
A   2. Events should be consumable via RSS based on owner
A   3. Events should be consumable via RSS based on consumer
   4. AMQP
         1. It should be possible to enable each message type to be published to an AMQP Bus.
         2. It should be possible to publish no messages to the AMQP bus. 
 */
@Test(groups={"events"})
public class EventTests extends SubscriptionManagerTestScript{
	protected String testOwnerKey = "newOwner"+System.currentTimeMillis();
	protected JSONObject testOwner;
	protected String testProductId = "newProduct"+System.currentTimeMillis();
	protected JSONObject testProduct;
	protected String testPoolId = "newPool"+System.currentTimeMillis();
	protected JSONObject testPool;
	

	@Test(	description="subscription-manager: events: Consumer Created is sent over an RSS atom feed.",
			groups={"ConsumerCreated_Test"}, dependsOnGroups={},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void ConsumerCreated_Test() {
		
		// start fresh by unregistering
		clienttasks.unregister();
		
		// get the owner and consumer feeds before we test the firing of a new event
		String ownerKey = clientOwnerUsername; // FIXME this hard-coded owner key assumes the key is the same as the owner name
        SyndFeed oldFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
 
        // fire a register event
		clienttasks.register(clientusername,clientpassword,null,null,null,null);
		String[] newEventTitles = new String[]{"CONSUMER CREATED"};
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
		
		// assert the owner feed...
		assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles);
        
		// assert the consumer feed...
		assertTheNewConsumerFeed(consumerCert.consumerid, null, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: Enitlement Created is sent over an RSS atom feed.",
			groups={"EnititlementCreated_Test"}, dependsOnGroups={"ConsumerCreated_Test"},
			enabled=true)
	@ImplementsTCMS(id="50403")
	public void EnititlementCreated_Test() {
		
		// get the owner and consumer feeds before we test the firing of a new event
		String ownerKey = clientOwnerUsername; // FIXME this hard-coded owner key assumes the key is the same as the owner name
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
        SyndFeed oldFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        SyndFeed oldConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(consumerCert.consumerid, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
 
        // fire a subscribe event
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		//SubscriptionPool pool = pools.get(0); // pick the first pool
		clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
		String[] newEventTitles = new String[]{"ENTITLEMENT CREATED"};

		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
		
		// assert the owner feed...
		assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles);
       
		// assert the consumer feed...
        assertTheNewConsumerFeed(consumerCert.consumerid, oldConsumerFeed, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: Pool Modified and Entitlement Modified is sent over an RSS atom feed.",
			groups={"PoolModifiedAndEntitlementModified_Test"}, dependsOnGroups={"EnititlementCreated_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void PoolModifiedAndEntitlementModified_Test() throws SQLException, JSONException {
		if (servertasks==null) throw new SkipException("This test requires an SSH connection to the candlepin server."); 
		//FIXME
		if (false) throw new SkipException("I COULD NOT GET THIS TEST TO WORK RELIABLY SINCE THE RSS FEED APPEARS TO BE PRODUCING MORE/LESS EVENTS THAN I EXPECTED.  THIS MAY BE A BUG.  NEEDS MORE INVESTIGATION.");

		// get the owner and consumer feeds before we test the firing of a new event
		String ownerKey = clientOwnerUsername; // FIXME this hard-coded owner key assumes the key is the same as the owner name
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
        SyndFeed oldFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        SyndFeed oldConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(consumerCert.consumerid, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        
        // find the pool id of a currently consumed product
        List<ProductSubscription> products = clienttasks.getCurrentlyConsumedProductSubscriptions();
		SubscriptionPool pool = clienttasks.getSubscriptionPoolFromProductSubscription(products.get(0));
		Calendar originalStartDate = (Calendar) products.get(0).startDate.clone();
		
		// fire an modified pool event (and subsequently a modified entitlement event because the pool was modified thereby requiring an entitlement update dropped to the consumer)
		log.info("To fire a modified pool event (and subsequently a modified entitlement event because the pool is already subscribed too), we will modify pool '"+pool+"' by subtracting one month from startdate...");
		Calendar newStartDate = (Calendar) originalStartDate.clone(); newStartDate.add(Calendar.MONTH, -1);
		updateSubscriptionPoolDatesOnDatabase(pool,newStartDate,null);

		log.info("Now let's refresh the subscription pools...");
		//CandlepinTasks.curl_refresh_pools(client,serverHostname,serverPort,clientOwnerUsername,clientOwnerPassword);
		servertasks.cpc_refresh_pools(ownerKey, true);
		clienttasks.changeCertFrequency(1);
		sleep(1*60*1000);sleep(10000);	// give the rhsmcertd a chance check in with the candlepin server and update the certs

		// assert the feed...
		assertTheNewFeed(oldFeed, new String[]{"ENTITLEMENT MODIFIED", "POOL MODIFIED"});
		
        // assert the owner feed...
		assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, new String[]{"ENTITLEMENT MODIFIED", "POOL MODIFIED"});
 
		// assert the consumer feed...
        assertTheNewConsumerFeed(consumerCert.consumerid, oldConsumerFeed, new String[]{"ENTITLEMENT MODIFIED"});
	}
	
	
	@Test(	description="subscription-manager: events: Entitlement Deleted is sent over an RSS atom feed.",
			groups={"EnititlementDeleted_Test"}, dependsOnGroups={"PoolModifiedAndEntitlementModified_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void EnititlementDeleted_Test() {
		
		// get the owner and consumer feeds before we test the firing of a new event
		String ownerKey = clientOwnerUsername; // FIXME this hard-coded owner key assumes the key is the same as the owner name
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
        SyndFeed oldFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        SyndFeed oldConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(consumerCert.consumerid, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
 
        // fire an unsubscribe event
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		String[] newEventTitles = new String[]{"ENTITLEMENT DELETED"};

		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
		
		// assert the owner feed...
		assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles);
       
		// assert the consumer feed...
        assertTheNewConsumerFeed(consumerCert.consumerid, oldConsumerFeed, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: Consumer Modified is sent over an RSS atom feed.",
			groups={"ConsumerModified_Test"}, dependsOnGroups={"EnititlementDeleted_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void ConsumerModified_Test() {
		
		// get the owner and consumer feeds before we test the firing of a new event
		String ownerKey = clientOwnerUsername; // FIXME this hard-coded owner key assumes the key is the same as the owner name
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
        SyndFeed oldFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        SyndFeed oldConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(consumerCert.consumerid, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
 
        // fire an facts update event by overriding a fact in /etc/rhsm/facts/event_tests.facts
        String factsFile = clienttasks.factsDir+"eventTests.facts";
        client.runCommandAndWait("echo '{\"events.test.description\": \"Testing CONSUMER MODIFIED event fires on facts update.\", \"events.test.currentTimeMillis\": \""+System.currentTimeMillis()+"\"}' > "+factsFile);	// create an override for facts
		clienttasks.facts(null,true);
		String[] newEventTitles = new String[]{"CONSUMER MODIFIED"};

		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
		
		// assert the owner feed...
		assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles);
       
		// assert the consumer feed...
        assertTheNewConsumerFeed(consumerCert.consumerid, oldConsumerFeed, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: Consumer Deleted is sent over an RSS atom feed.",
			groups={"ConsumerDeleted_Test"}, dependsOnGroups={"ConsumerModified_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void ConsumerDeleted_Test() {
		
		// get the owner and consumer feeds before we test the firing of a new event
		String ownerKey = clientOwnerUsername; // FIXME this hard-coded owner key assumes the key is the same as the owner name
        SyndFeed oldFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
 
        // fire an unregister event
		clienttasks.unregister();
		String[] newEventTitles = new String[]{"CONSUMER DELETED"};
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
		
		// assert the owner feed...
		assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: Owner Created is sent over an RSS atom feed.",
			groups={"OwnerCreated_Test"}, dependsOnGroups={"ConsumerDeleted_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void OwnerCreated_Test() throws JSONException {
		if (servertasks==null) throw new SkipException("This test requires an SSH connection to the candlepin server."); 
		
		// get the owner and consumer feeds before we test the firing of a new event
		SyndFeed oldFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
 
        // do something that will fire a create owner event
		testOwner = servertasks.cpc_create_owner(testOwnerKey);
		String[] newEventTitles = new String[]{"OWNER CREATED"};
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
	
		// assert the owner feed...
		assertTheNewOwnerFeed(testOwner.getString("key"), null, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: Product Created is sent over an RSS atom feed.",
			groups={"ProductCreated_Test"}, dependsOnGroups={"OwnerCreated_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void ProductCreated_Test() throws JSONException {
		if (servertasks==null) throw new SkipException("This test requires an SSH connection to the candlepin server."); 
		
		// get the owner and consumer feeds before we test the firing of a new event
		SyndFeed oldFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);

        // do something that will fire a create product event
		testProduct = servertasks.cpc_create_product(testProductId, testProductId+" For Test");
		String[] newEventTitles = new String[]{"PRODUCT CREATED"};

		// WORKAROUND
		if (true) throw new SkipException("09/02/2010 Events for PRODUCT CREATED are not yet dev complete.  Agilo Story: http://mgmt1.rhq.lab.eng.bos.redhat.com:8001/web/ticket/3737");

		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: Pool Created is sent over an RSS atom feed.",
			groups={"PoolCreated_Test"}, dependsOnGroups={"ProductCreated_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void PoolCreated_Test() throws JSONException {
		if (servertasks==null) throw new SkipException("This test requires an SSH connection to the candlepin server."); 
		
		// get the owner and consumer feeds before we test the firing of a new event
		SyndFeed oldFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);

        // do something that will fire a create product event
		testPool = servertasks.cpc_create_pool(testProduct.getString("id"), testOwner.getString("id"), "99");
		String[] newEventTitles = new String[]{"POOL CREATED"};

		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
		
		// TODO
//		<jsefler> dgoodwin: your timing for the event for POOL CREATED is perfect.  I'll automate a test for it now.  It looks like the event is working.
//		 dgoodwin dgregor_pto dgao
//		<dgoodwin> jsefler: cool, three possible cases, creation via candlepin api (post /pools), refresh pools after creating a subscription, and creation of the rh personal "sub pool"
	}
	
	
	@Test(	description="subscription-manager: events: Pool Deleted is sent over an RSS atom feed.",
			groups={"PoolDeleted_Test"}, dependsOnGroups={"PoolCreated_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void PoolDeleted_Test() throws JSONException {
		if (servertasks==null) throw new SkipException("This test requires an SSH connection to the candlepin server."); 
		
		// get the owner and consumer feeds before we test the firing of a new event
		SyndFeed oldFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);

        // do something that will fire a delete pool event
		servertasks.cpc_delete_pool(testPool.getString("id"));
		String[] newEventTitles = new String[]{"POOL DELETED"};
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: Product Deleted is sent over an RSS atom feed.",
			groups={"ProductDeleted_Test"}, dependsOnGroups={"PoolDeleted_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void ProductDeleted_Test() throws JSONException {
		
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
		SyndFeed oldFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);

        // do something that will fire a delete product event
		//servertasks.cpc_delete_product(testProduct.getString("id"));
		String[] newEventTitles = new String[]{"PRODUCT DELETED"};
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: Export Created is sent over an RSS atom feed.",
			groups={"ExportCreated_Test"}, dependsOnGroups={"ProductDeleted_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void ExportCreated_Test() throws JSONException {
		
		// start fresh by unregistering
		clienttasks.unregister();
		
		// register a type=candlepin consumer and subscribe to get an entitlement
		// NOTE: Without the subscribe, this bugzilla is thrown: 
		SSHCommandResult result = clienttasks.register(clientusername,clientpassword,"candlepin",null,null,null);
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribe(pool.poolId, null, null, null, null);
		//String consumerKey = result.getStdout().split(" ")[0];
		
		// get the owner and consumer feeds before we test the firing of a new event
		String ownerKey = clientOwnerUsername; // FIXME this hard-coded owner key assumes the key is the same as the owner name
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
        SyndFeed oldFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        SyndFeed oldConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(consumerCert.consumerid, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        
        // do something that will fire a exported created event
		CandlepinTasks.curl_export_consumer(client,serverHostname,serverPort,clientOwnerUsername,clientOwnerPassword,consumerCert.consumerid,"/tmp/export.zip");
		String[] newEventTitles = new String[]{"EXPORT CREATED"};
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
		
		// assert the owner feed...
		assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles);
        
		// assert the consumer feed...
		assertTheNewConsumerFeed(consumerCert.consumerid, oldConsumerFeed, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: Import Created is sent over an RSS atom feed.",
			groups={"ImportCreated_Test"}, dependsOnGroups={"ExportCreated_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void ImportCreated_Test() throws JSONException {
		
		// get the owner and consumer feeds before we test the firing of a new event
		String ownerKey = testOwner.getString("key");
        SyndFeed oldFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        
        // do something that will fire an import created event
		CandlepinTasks.curl_import_consumer(client,serverHostname,serverPort,clientOwnerUsername,clientOwnerPassword,ownerKey,"/tmp/export.zip");
		String[] newEventTitles = new String[]{"IMPORT CREATED", "POOL CREATED"};  // Note: the POOL CREATED comes from the subscribed pool
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
		
		// assert the owner feed...
		assertTheNewOwnerFeed(ownerKey, oldOwnerFeed, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: Owner Deleted is sent over an RSS atom feed.",
			groups={"OwnerDeleted_Test"}, dependsOnGroups={"ImportCreated_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void OwnerDeleted_Test() {
		if (servertasks==null) throw new SkipException("This test requires an SSH connection to the candlepin server."); 
		
		// get the owner and consumer feeds before we test the firing of a new event
		SyndFeed oldFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
 
		// do something that will fire a delete owner event
		servertasks.cpc_delete_owner(testOwnerKey);
		String[] newEventTitles = new String[]{"OWNER DELETED"};
		
		// assert the feed...
		assertTheNewFeed(oldFeed, newEventTitles);
	}
	
	// Protected Methods ***********************************************************************

	protected void assertTheNewOwnerFeed(String ownerKey, SyndFeed oldOwnerFeed, String[] newEventTitles) {
		int oldOwnerFeed_EntriesSize = oldOwnerFeed==null? 0 : oldOwnerFeed.getEntries().size();

		// assert the owner feed...
		SyndFeed newOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerKey, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
		Assert.assertEquals(newOwnerFeed.getTitle(),"Event feed for owner "+ownerKey);
		Assert.assertEquals(newOwnerFeed.getEntries().size(), oldOwnerFeed_EntriesSize+newEventTitles.length, "The event feed for owner id "+ownerKey+" has increased by "+newEventTitles.length+" entries.");
		int i=0;
		for (String newEventTitle : newEventTitles) {
			String actualEventTitle = ((SyndEntryImpl) newOwnerFeed.getEntries().get(i)).getTitle();
			Assert.assertEquals(actualEventTitle,newEventTitle, "The next ("+i+") newest event feed entry for owner id "+ownerKey+" is '"+newEventTitle+"'.");
			i++;
		}
	}
	
	protected void assertTheNewConsumerFeed(String consumerKey, SyndFeed oldConsumerFeed, String[] newEventTitles) {
		// assert the consumer feed...
		int oldConsumerFeed_EntriesSize = oldConsumerFeed==null? 0 : oldConsumerFeed.getEntries().size();

		SyndFeed newConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(consumerKey, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
		Assert.assertEquals(newConsumerFeed.getTitle(),"Event feed for consumer "+consumerKey);
		Assert.assertEquals(newConsumerFeed.getEntries().size(), oldConsumerFeed_EntriesSize+newEventTitles.length, "The event feed for consumer "+consumerKey+" has increased by "+newEventTitles.length+" entries.");
		int i=0;
		for (String newEventTitle : newEventTitles) {
			String actualEventTitle = ((SyndEntryImpl) newConsumerFeed.getEntries().get(i)).getTitle();
			Assert.assertEquals(actualEventTitle,newEventTitle, "The next ("+i+") newest event feed entry for consumer "+consumerKey+" is '"+newEventTitle+"'.");
			i++;
		}
	}
	
	protected void assertTheNewFeed(SyndFeed oldFeed, String[] newEventTitles) {
		// assert the consumer feed...
		int oldFeed_EntriesSize = oldFeed==null? 0 : oldFeed.getEntries().size();
		
		SyndFeed newConsumerFeed = CandlepinTasks.getSyndFeed(serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);		
		Assert.assertEquals(newConsumerFeed.getTitle(),"Event Feed");
		Assert.assertEquals(newConsumerFeed.getEntries().size(), oldFeed_EntriesSize+newEventTitles.length, "The event feed entries has increased by "+newEventTitles.length);
		int i=0;
		for (String newEventTitle : newEventTitles) {
			String actualEventTitle = ((SyndEntryImpl) newConsumerFeed.getEntries().get(i)).getTitle();
			Assert.assertEquals(actualEventTitle,newEventTitle, "The next ("+i+") newest event feed entry is '"+newEventTitle+"'.");
			i++;
		}
	}
	
	// Data Providers ***********************************************************************

	
	
	
	/**
	 * On the connected candlepin server database, update the startdate and enddate in the cp_subscription table on rows where the pool id is a match.
	 * @param pool
	 * @param startDate
	 * @param endDate
	 * @throws SQLException 
	 */
	protected void updateSubscriptionPoolDatesOnDatabase(SubscriptionPool pool, Calendar startDate, Calendar endDate) throws SQLException {
		//DateFormat dateFormat = new SimpleDateFormat(CandlepinAbstraction.dateFormat);
		String updateSubscriptionPoolEndDateSql = "";
		String updateSubscriptionPoolStartDateSql = "";
		if (endDate!=null) {
			updateSubscriptionPoolEndDateSql = "update cp_subscription set enddate='"+AbstractCommandLineData.formatDateString(endDate)+"' where id=(select pool.subscriptionid from cp_pool pool where pool.id='"+pool.poolId+"');";
		}
		if (startDate!=null) {
			updateSubscriptionPoolStartDateSql = "update cp_subscription set startdate='"+AbstractCommandLineData.formatDateString(startDate)+"' where id=(select pool.subscriptionid from cp_pool pool where pool.id='"+pool.poolId+"');";
		}
		
		Statement sql = dbConnection.createStatement();
		if (endDate!=null) {
			log.fine("Executing SQL: "+updateSubscriptionPoolEndDateSql);
			Assert.assertEquals(sql.executeUpdate(updateSubscriptionPoolEndDateSql), 1, "Updated one row of the cp_subscription table with sql: "+updateSubscriptionPoolEndDateSql);
		}
		if (startDate!=null) {
			log.fine("Executing SQL: "+updateSubscriptionPoolStartDateSql);
			Assert.assertEquals(sql.executeUpdate(updateSubscriptionPoolStartDateSql), 1, "Updated one row of the cp_subscription table with sql: "+updateSubscriptionPoolStartDateSql);
		}
		sql.close();
	}

}
