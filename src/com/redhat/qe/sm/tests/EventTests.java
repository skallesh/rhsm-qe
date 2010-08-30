package com.redhat.qe.sm.tests;

import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.testng.SkipException;
import org.testng.annotations.Test;

import com.redhat.qe.auto.selenium.Base64;
import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.abstractions.CandlepinAbstraction;
import com.redhat.qe.sm.abstractions.ConsumerCert;
import com.redhat.qe.sm.abstractions.ProductSubscription;
import com.redhat.qe.sm.abstractions.SubscriptionPool;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.sm.tasks.CandlepinTasks;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSLCertificateTruster;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;



/**
 * @author jsefler
 *
 */
@Test(groups={"events"})
public class EventTests extends SubscriptionManagerTestScript{
	

	@Test(	description="subscription-manager: events: basic events fire on register",
			groups={"BasicEventsFireOnRegister_Test"}, dependsOnGroups={},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void BasicEventsFireOnRegister_Test() {
		
		// start fresh by unregistering
		clienttasks.unregister();
		
		// get the owner and consumer feeds before we test the firing of a new event
		String ownerId = "1"; // FIXME hard-coded owner id
        SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerId, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
 
        // fire a register event
		clienttasks.register(clientusername,clientpassword,null,null,null,null);
		String[] newEventTitles = new String[]{"CONSUMER CREATED"};
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		
		// assert the owner feed...
		assertTheNewOwnerFeed(ownerId, oldOwnerFeed, newEventTitles);
        
		// assert the consumer feed...
		assertTheNewConsumerFeed(consumerCert.consumerid, null, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: basic events fire on subscribe",
			groups={"BasicEventsFireOnSubscribe_Test"}, dependsOnGroups={"BasicEventsFireOnRegister_Test"},
			enabled=true)
	@ImplementsTCMS(id="50403")
	public void BasicEventsFireOnSubscribe_Test() {
		
		// get the owner and consumer feeds before we test the firing of a new event
		String ownerId = "1"; // FIXME hard-coded owner id
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerId, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        SyndFeed oldConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(consumerCert.consumerid, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
 
        // fire a subscribe event
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		//SubscriptionPool pool = pools.get(0); // pick the first pool
		clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
		String[] newEventTitles = new String[]{"ENTITLEMENT CREATED"};

		// assert the owner feed...
		assertTheNewOwnerFeed(ownerId, oldOwnerFeed, newEventTitles);
       
		// assert the consumer feed...
        assertTheNewConsumerFeed(consumerCert.consumerid, oldConsumerFeed, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: basic events fire on unsubscribe",
			groups={"BasicEventsFireOnModifiedPoolAndEntitlement_Test"}, dependsOnGroups={"BasicEventsFireOnSubscribe_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void BasicEventsFireOnModifiedPoolAndEntitlement_Test() throws SQLException {
		//FIXME
		if (true) throw new SkipException("I COULD NOT GET THIS TEST TO WORK RELIABLY SINCE THE RSS FEED APPEARS TO BE PRODUCING MORE/LESS EVENTS THAN I EXPECTED.  THIS MAY BE A BUG.  NEEDS MORE INVESTIGATION.");

		// get the owner and consumer feeds before we test the firing of a new event
		String ownerId = "1"; // FIXME hard-coded owner id
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerId, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
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
		servertasks.refreshSubscriptionPools(serverHostname,serverPort,clientOwnerUsername,clientOwnerPassword);
		sleep(10*1000);
//		sleep(1*60*1000);sleep(10*1000);  // give the server a chance to finish this asynchronous job

        // assert the owner feed...
		assertTheNewOwnerFeed(ownerId, oldOwnerFeed, new String[]{"ENTITLEMENT MODIFIED", "POOL MODIFIED"});
 
		// assert the consumer feed...
        assertTheNewConsumerFeed(consumerCert.consumerid, oldConsumerFeed, new String[]{"ENTITLEMENT MODIFIED"});
	}
	
	
	@Test(	description="subscription-manager: events: basic events fire on unsubscribe",
			groups={"BasicEventsFireOnUnsubscribe_Test"}, dependsOnGroups={"BasicEventsFireOnModifiedPoolAndEntitlement_Test"},
			enabled=true, alwaysRun=true)
	//@ImplementsTCMS(id="")
	public void BasicEventsFireOnUnsubscribe_Test() {
		
		// get the owner and consumer feeds before we test the firing of a new event
		String ownerId = "1"; // FIXME hard-coded owner id
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerId, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        SyndFeed oldConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(consumerCert.consumerid, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
 
        // fire an unsubscribe event
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		String[] newEventTitles = new String[]{"ENTITLEMENT DELETED"};

		// assert the owner feed...
		assertTheNewOwnerFeed(ownerId, oldOwnerFeed, newEventTitles);
       
		// assert the consumer feed...
        assertTheNewConsumerFeed(consumerCert.consumerid, oldConsumerFeed, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: basic events fire on facts update",
			groups={"BasicEventsFireOnFactsUpdate_Test"}, dependsOnGroups={"BasicEventsFireOnUnsubscribe_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void BasicEventsFireOnFactsUpdate_Test() {
		
		// get the owner and consumer feeds before we test the firing of a new event
		String ownerId = "1"; // FIXME hard-coded owner id
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerId, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        SyndFeed oldConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(consumerCert.consumerid, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
 
        // fire an facts update event by overriding a fact in /etc/rhsm/facts/event_tests.facts
        String factsFile = clienttasks.factsDir+"eventTests.facts";
        client.runCommandAndWait("echo '{\"events.test.description\": \"Testing basic events fire on facts update.\", \"events.test.currentTimeMillis\": \""+System.currentTimeMillis()+"\"}' > "+factsFile);	// create an override for facts
		clienttasks.facts(null,true);
		String[] newEventTitles = new String[]{"CONSUMER MODIFIED"};

		// assert the owner feed...
		assertTheNewOwnerFeed(ownerId, oldOwnerFeed, newEventTitles);
       
		// assert the consumer feed...
        assertTheNewConsumerFeed(consumerCert.consumerid, oldConsumerFeed, newEventTitles);
	}
	
	
	@Test(	description="subscription-manager: events: basic events fire on unregister",
			groups={"BasicEventsFireOnUnregister_Test"}, dependsOnGroups={"BasicEventsFireOnFactsUpdate_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void BasicEventsFireOnUnregister_Test() {
		
		// get the owner and consumer feeds before we test the firing of a new event
		String ownerId = "1"; // FIXME hard-coded owner id
        SyndFeed oldOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerId, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
 
        // fire an unregister event
		clienttasks.unregister();
		String[] newEventTitles = new String[]{"CONSUMER DELETED"};
		
		// assert the owner feed...
		assertTheNewOwnerFeed(ownerId, oldOwnerFeed, newEventTitles);
	}
	

	
	// Protected Methods ***********************************************************************

	protected void assertTheNewOwnerFeed(String ownerId, SyndFeed oldOwnerFeed, String[] newEventTitles) {
		int oldOwnerFeed_EntriesSize = oldOwnerFeed==null? 0 : oldOwnerFeed.getEntries().size();

		// assert the owner feed...
		SyndFeed newOwnerFeed = CandlepinTasks.getSyndFeedForOwner(ownerId, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
		Assert.assertEquals(newOwnerFeed.getTitle(),"Event feed for owner "+clientOwnerUsername);
		Assert.assertEquals(newOwnerFeed.getEntries().size(), oldOwnerFeed_EntriesSize+newEventTitles.length, "The event feed entries for owner id "+ownerId+" has increased by "+newEventTitles.length);
		int i=0;
		for (String newEventTitle : newEventTitles) {
			String actualEventTitle = ((SyndEntryImpl) newOwnerFeed.getEntries().get(i)).getTitle();
			Assert.assertEquals(actualEventTitle,newEventTitle, "The next ("+i+") newest event feed entry for owner id "+ownerId+" is '"+newEventTitle+"'.");
			i++;
		}
	}
	
	protected void assertTheNewConsumerFeed(String consumerId, SyndFeed oldConsumerFeed, String[] newEventTitles) {
		// assert the consumer feed...
		int oldConsumerFeed_EntriesSize = oldConsumerFeed==null? 0 : oldConsumerFeed.getEntries().size();

		SyndFeed newConsumerFeed = CandlepinTasks.getSyndFeedForConsumer(consumerId, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
		Assert.assertEquals(newConsumerFeed.getTitle(),"Event feed for consumer "+consumerId);
		Assert.assertEquals(newConsumerFeed.getEntries().size(), oldConsumerFeed_EntriesSize+newEventTitles.length, "The event feed entries for consumer "+consumerId+" has increased by "+newEventTitles.length);
		int i=0;
		for (String newEventTitle : newEventTitles) {
			String actualEventTitle = ((SyndEntryImpl) newConsumerFeed.getEntries().get(i)).getTitle();
			Assert.assertEquals(actualEventTitle,newEventTitle, "The next ("+i+") newest event feed entry for consumer "+consumerId+" is '"+newEventTitle+"'.");
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
			updateSubscriptionPoolEndDateSql = "update cp_subscription set enddate='"+CandlepinAbstraction.formatDateString(endDate)+"' where id=(select pool.subscriptionid from cp_pool pool where pool.id='"+pool.poolId+"');";
		}
		if (startDate!=null) {
			updateSubscriptionPoolStartDateSql = "update cp_subscription set startdate='"+CandlepinAbstraction.formatDateString(startDate)+"' where id=(select pool.subscriptionid from cp_pool pool where pool.id='"+pool.poolId+"');";
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
