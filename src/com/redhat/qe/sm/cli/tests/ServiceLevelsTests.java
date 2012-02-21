package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.json.JSONException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.ContentNamespace;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.Repo;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.sm.data.YumRepo;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * Reference: https://engineering.redhat.com/trac/Entitlement/wiki/SlaSubscribe
 */

//Notes
/*
<jsefler> jbowes, I'd like to get something started today on the new --servicelevel option for subscribe --auto.  Any pointers?
How do I know what the valid values are?  Do I need to search through the attributes on the pools?
<dgoodwin> jsefler: curl /owners/key/servicelevels will show you everything valid for that org
<jsefler> perfect
<jsefler> dgoodwin, so service levels are attached to the owners.  I guess they get added and deleted with some PUT calls?
<dgoodwin> jsefler: nah they're just an attribute on products, so the api call is just scanning for all the service levels available for that orgs subs
<jsefler> ok
<dgoodwin> you'll see some support_level attributes in our json test data
<dgoodwin> that's where they come from
<jsefler> for subscribe --auto, I assume there is an order of preference for choosing a subscription based on service level agreements?  or if I specify --servicelevel then do I ONLY get a matching subscription with that service level?
<dgoodwin> jsefler: you only get subs from that support level
dgoodwin dgregor
jsefler: https://engineering.redhat.com/trac/Entitlement/wiki/SlaSubscribe will help explain some things
<jsefler> thank you dgoodwin - that should get me started
<dgoodwin> jsefler: also note that --servicelevel = set it on the consumer as a part of this operation
<dgoodwin> and once it's set, any autosub/heal will use it
even if it's not specified on the cli
--- mstead is now known as mstead-afk
dgoodwin dgregor
<dgoodwin> i kinda wonder if it should be subscription-manager servicelevel --set=SLA
as it's not entirely clear that i'm persisting something that will stick when i do subscribe --auto --servicelevel=SLA
<jsefler> dgoodwin, so I guess it is also an option on register?   and if I specify it on the subscribe line then does that value override what I set during register?
<dgoodwin> yeah it's there as well
any time you specify it it will override previous value
<jsefler> thanks
*/

@Test(groups={"ServiceLevelsTests"})
public class ServiceLevelsTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************
	

	
	
	// Candidates for an automated Test:
		
	// Configuration methods ***********************************************************************


	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************
	

}
