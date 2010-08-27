package com.redhat.qe.sm.tests;

import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.testng.annotations.Test;

import com.redhat.qe.auto.selenium.Base64;
import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.abstractions.ConsumerCert;
import com.redhat.qe.sm.abstractions.SubscriptionPool;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.sm.tasks.CandlepinTasks;
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
			groups={"myDevGroup"},
			enabled=true)
	@ImplementsTCMS(id="50403")
	public void BasicEventsFireOnRegister_Test() {
		
		// start fresh by unregistering and registering
		clienttasks.unregister();
		
		// get the owner and consumer feeds before we test the firing of a new event
        SyndFeed ownerFeedBefore = CandlepinTasks.getSyndFeedForOwner(/*FIXME hard-coded owner id*/"1", serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
 
        // fire a register event
		clienttasks.register(clientusername,clientpassword,null,null,null,null);
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		
		// assert the owner feed...
        SyndFeed ownerFeed = CandlepinTasks.getSyndFeedForOwner(/*FIXME hard-coded owner id*/"1", serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        //TODO
        
		// assert the consumer feed...
        SyndFeed consumerFeed = CandlepinTasks.getSyndFeedForConsumer(consumerCert.consumerid, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
        Assert.assertEquals(consumerFeed.getTitle(),"Event feed for consumer "+consumerCert.consumerid);
        Assert.assertEquals(consumerFeed.getEntries().size(),1,"The event feed for a newly registered consumer only contains one entry.");
        Assert.assertEquals(((SyndEntryImpl) consumerFeed.getEntries().get(0)).getTitle(),"CONSUMER CREATED");
	}
	
//	@Test(	description="subscription-manager: events: basic events fire on register",
//			groups={"myDevGroup"},
//			enabled=true)
//	@ImplementsTCMS(id="50403")
//	public void BasicEventsFireOnSubscribe_Test() {
//		
//		// start fresh by unregistering and registering
//		clienttasks.unregister();
//		clienttasks.register(clientusername,clientpassword,null,null,null,null);
//		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
//		
//		// subscribe to a random pool
//		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
//		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
//		clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
//		
//
//        SyndFeed feed = CandlepinTasks.getSyndFeedForConsumer(consumerCert.consumerid, serverHostname, serverPort, clientOwnerUsername, clientOwnerPassword);
//
//        log.info(feed.toString());
//	}
	
	// Protected Methods ***********************************************************************


	
	
	// Data Providers ***********************************************************************

	

}
