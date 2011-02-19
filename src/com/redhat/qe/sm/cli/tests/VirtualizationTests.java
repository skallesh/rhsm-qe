package com.redhat.qe.sm.cli.tests;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.SubscriptionPool;

@Test(groups="virtualization")
public class VirtualizationTests extends SubscriptionManagerCLITestScript {



//	<jsefler> I'm trying to strategize an automated test for the virt entitlements stuff you demo'ed on Wednesday.  I got a few questions to start with...
//	<jharris> sure
//	 shoot
//	<jsefler> using the RESTapi, if I search through all the owners subscriptions and find one with a virt_limit attribute, then that means that two pools should get created corresponding to it.  correct?
//	 one pool for the host and one pool fir the guests
//	<jharris> yes
//	 specifically the attribute is on either the product or the pool
//	<jsefler> what does that mean?
//	 the virt_limit is an attribute of the subscription - that I know
//	 next I need to figure out what the relevant attributes are on the pool
//	<jharris> uhhh
//	 afaik subscriptions do not have attributes
//	<jsefler> am i asking the wrong question?
//	<jharris> pools have attributes
//	 products have attributes
//	 the two pools are created, as you said
//	 the physical pool will have no additional attributes
//	 the virt pool will have an attribute of "virt_only" set to true
//	 the candlepin logic should only let virtual machines subscribe to that second pool
//	 this is done by checking the virt.is_guest fact
//	 that is set in subscription manager
//	<jsefler> yup - that sounds good - that's what I need to get started
//	<jharris> excellent
//	 but the virt_only attribute can also just be used on a product, for example
//	 so that maybe we want to start selling a product that is like RHEL for virtual machines
//	 IT can just stick that virt_only attribute on the product directly
//	 and it should do the same filtering
	

	
	
	// Test methods ***********************************************************************
	
	@Test(	description="Verify host and guest pools are generated from a virtualization-aware subscription.",
			groups={},
			dependsOnGroups={},
			dataProvider="getVirtSubscriptionData",
			enabled=true)
	public void VerifyHostAndGuestPoolsAreGeneratedForVirtualizationAwareSubscription(String subscriptionId, String productName, String productId, int quantity, String virtLimit, String hostPoolId, String guestPoolId) throws JSONException, Exception {

		log.info("When an owner has purchased a virtualization-aware subscription ("+productName+"; subscriptionId="+subscriptionId+"), he should have subscription access to two pools: one for the host and one for the guest.");

		// assert that there are two (one for the host and one for the guest)
		log.info("Using the RESTful Candlepin API, let's find all the pools generated from subscription id: "+subscriptionId);
		List<String> poolIds = CandlepinTasks.findPoolIdsFromSubscriptionId(serverHostname,serverPort,serverPrefix,clientusername,clientpassword, ownerKey, subscriptionId);
		Assert.assertEquals(poolIds.size(), 2, "Exactly two pools should be derived from virtualization-aware subscription id '"+subscriptionId+"' ("+productName+").");

		// assert that one pool is for the host and the other is for the guest
		guestPoolId = null;
		hostPoolId = null;
		for (String poolId : poolIds) {
			Boolean virtOnly = CandlepinTasks.isPoolVirtOnly (serverHostname,serverPort,serverPrefix,clientusername,clientpassword, poolId);		
			if (virtOnly!=null && virtOnly) {
				guestPoolId = poolId;
			} else {
				hostPoolId = poolId;
			}
		}
		Assert.assertNotNull(guestPoolId, "Found the guest pool id ("+guestPoolId+") with an attribute of virt_only=true");
		Assert.assertNotNull(hostPoolId, "Found the host pool id ("+hostPoolId+") without an attribute of virt_only=true");	
	}
	
	
	@Test(	description="Verify host and guest pools to a virtualization-aware subscription are subscribable on a guest system.",
			groups={},
			dependsOnGroups={},
			dataProvider="getVirtSubscriptionData",
			enabled=true)
	public void VerifyHostAndGuestPoolsAreSubscribableOnGuestSystem(String subscriptionId, String productName, String productId, int quantity, String virtLimit, String hostPoolId, String guestPoolId) throws JSONException, Exception {
		if (hostPoolId==null && guestPoolId==null) throw new SkipException("Failed to find expected host and guest pools derived from virtualization-aware subscription id '"+subscriptionId+"' ("+productName+").");

		// TODO
		// trick this system into believing it is a virt guest
		
		// assert that the hostPoolId is available
		List<SubscriptionPool> availablePools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool hostPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", hostPoolId, availablePools);
		Assert.assertNotNull(hostPool,"A pool derived from the virtualization-aware subscription id '"+subscriptionId+"' is available on a guest: "+hostPool);

		// assert hostPoolId quantity
		
		// attempt to subscribe to the hostPoolId
		
		
		// assert that the guestPoolId is available
		
		// assert guestPoolId quantity
		
		// attempt to subscribe to the guestPoolId
		
	}
	
	
	@Test(	description="Verify only the derived host pool from a virtualization-aware subscription is subscribable on a host system.  The guest pool should not be available nor subscribable.",
			groups={},
			dependsOnGroups={},
			dataProvider="getVirtSubscriptionData",
			enabled=true)
	public void VerifyHostPoolIsSubscribableOnHostSystemWhileGuestPoolIsNot(String subscriptionId, String productName, String productId, int quantity, String virtLimit, String hostPoolId, String guestPoolId) throws JSONException, Exception {
		if (hostPoolId==null && guestPoolId==null) throw new SkipException("Failed to find expected host and guest pools derived from virtualization-aware subscription id '"+subscriptionId+"' ("+productName+").");

		// TODO
		// trick this system into believing it is a host
		
		// assert that the hostPoolId is available

		// assert that the guestPoolId is NOT available
		
		// attempt to subscribe to the hostPoolId
		
		// attempt to subscribe to the guestPoolId
		
		
	}
	

	
	
	// TODO Candidates for an automated Test:
	//		
	
	
	
	
	// Configuration methods ***********************************************************************
	
	

	@BeforeClass(groups="setup")
	public void registerBeforeClass() throws Exception {
		clienttasks.unregister(null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(clientusername, clientpassword, null, null, null, null, null, null, null, null));
		
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(serverHostname, serverPort, serverPrefix, clientusername, clientpassword, consumerId);
	}
	
	@BeforeMethod(groups="setup")
	public void unsubscribeBeforeMethod() throws Exception {
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
	}
	
	// protected methods ***********************************************************************
	
	protected String ownerKey = "";

	
	protected Calendar parseDateString(String dateString){
		String simpleDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"; //"2012-02-08T00:00:00.000+0000"
		try{
			DateFormat dateFormat = new SimpleDateFormat(simpleDateFormat);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			Calendar calendar = new GregorianCalendar();
			calendar.setTimeInMillis(dateFormat.parse(dateString).getTime());
			return calendar;
		}
		catch (ParseException e){
			log.warning("Failed to parse GMT date string '"+dateString+"' with format '"+simpleDateFormat+"':\n"+e.getMessage());
			return null;
		}
	}
	
	
	// Data Providers ***********************************************************************

	@DataProvider(name="getVirtSubscriptionData")
	public Object[][] getVirtSubscriptionDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getVirtSubscriptionDataAsListOfLists());
	}
	protected List<List<Object>> getVirtSubscriptionDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		Calendar now = new GregorianCalendar();
		now.setTimeInMillis(System.currentTimeMillis());
		
		JSONArray jsonSubscriptions = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,clientusername,clientpassword,"/owners/"+ownerKey+"/subscriptions"));	
		for (int i = 0; i < jsonSubscriptions.length(); i++) {
			JSONObject jsonSubscription = (JSONObject) jsonSubscriptions.get(i);
			String subscriptionId = jsonSubscription.getString("id");
			Calendar startDate = parseDateString(jsonSubscription.getString("startDate"));	// "startDate":"2012-02-08T00:00:00.000+0000"
			Calendar endDate = parseDateString(jsonSubscription.getString("endDate"));	// "endDate":"2013-02-07T00:00:00.000+0000"
			int quantity = jsonSubscription.getInt("quantity");
			JSONObject jsonProduct = (JSONObject) jsonSubscription.getJSONObject("product");
			String productName = jsonProduct.getString("name");
			String productId = jsonProduct.getString("id");
			JSONArray jsonAttributes = jsonProduct.getJSONArray("attributes");
			// loop through the attributes of this subscription looking for the "virt_limit" attribute
			for (int j = 0; j < jsonAttributes.length(); j++) {
				JSONObject jsonAttribute = (JSONObject) jsonAttributes.get(j);
				String attributeName = jsonAttribute.getString("name");
				if (attributeName.equals("virt_limit")) {
					// found the virt_limit attribute - get its value
					String virt_limit = jsonAttribute.getString("value");
					
					// only retrieve data that is valid at this time
					if (startDate.before(now) && endDate.after(now)) {

						// save some time in the testcases and get the hostPoolId and guestPoolId (set to null if there is a problem)
						List<String> poolIds = CandlepinTasks.findPoolIdsFromSubscriptionId(serverHostname,serverPort,serverPrefix,clientusername,clientpassword, ownerKey, subscriptionId);

						// determine which pool is for the guest, the other must be for the host
						String guestPoolId = null;
						String hostPoolId = null;
						for (String poolId : poolIds) {
							Boolean virtOnly = CandlepinTasks.isPoolVirtOnly (serverHostname,serverPort,serverPrefix,clientusername,clientpassword, poolId);		
							if (virtOnly!=null && virtOnly) {
								guestPoolId = poolId;
							} else {
								hostPoolId = poolId;
							}
						}
						if (poolIds.size() != 2) {hostPoolId=null; guestPoolId=null;}	// set pools to null if there was a problem
						
						ll.add(Arrays.asList(new Object[]{subscriptionId, productName, productId, quantity, virt_limit, hostPoolId, guestPoolId}));
					}
				}
			}
		}
		
		return ll;
	}
	
	/* Example jsonSubscription:
	  {
		    "id": "8a90f8b42e398f7a012e398ff0ef0104",
		    "owner": {
		      "href": "/owners/admin",
		      "id": "8a90f8b42e398f7a012e398f8d310005"
		    },
		    "certificate": null,
		    "product": {
		      "name": "Awesome OS with up to 4 virtual guests",
		      "id": "awesomeos-virt-4",
		      "attributes": [
		        {
		          "name": "variant",
		          "value": "ALL",
		          "updated": "2011-02-18T16:17:37.960+0000",
		          "created": "2011-02-18T16:17:37.960+0000"
		        },
		        {
		          "name": "arch",
		          "value": "ALL",
		          "updated": "2011-02-18T16:17:37.960+0000",
		          "created": "2011-02-18T16:17:37.960+0000"
		        },
		        {
		          "name": "type",
		          "value": "MKT",
		          "updated": "2011-02-18T16:17:37.960+0000",
		          "created": "2011-02-18T16:17:37.960+0000"
		        },
		        {
		          "name": "version",
		          "value": "6.1",
		          "updated": "2011-02-18T16:17:37.961+0000",
		          "created": "2011-02-18T16:17:37.961+0000"
		        },
		        {
		          "name": "virt_limit",
		          "value": "4",
		          "updated": "2011-02-18T16:17:37.960+0000",
		          "created": "2011-02-18T16:17:37.960+0000"
		        }
		      ],
		      "multiplier": 1,
		      "productContent": [

		      ],
		      "dependentProductIds": [

		      ],
		      "href": "/products/awesomeos-virt-4",
		      "updated": "2011-02-18T16:17:37.959+0000",
		      "created": "2011-02-18T16:17:37.959+0000"
		    },
		    "providedProducts": [
		      {
		        "name": "Awesome OS Server Bits",
		        "id": "37060",
		        "attributes": [
		          {
		            "name": "variant",
		            "value": "ALL",
		            "updated": "2011-02-18T16:17:22.174+0000",
		            "created": "2011-02-18T16:17:22.174+0000"
		          },
		          {
		            "name": "sockets",
		            "value": "2",
		            "updated": "2011-02-18T16:17:22.175+0000",
		            "created": "2011-02-18T16:17:22.175+0000"
		          },
		          {
		            "name": "arch",
		            "value": "ALL",
		            "updated": "2011-02-18T16:17:22.175+0000",
		            "created": "2011-02-18T16:17:22.175+0000"
		          },
		          {
		            "name": "type",
		            "value": "SVC",
		            "updated": "2011-02-18T16:17:22.175+0000",
		            "created": "2011-02-18T16:17:22.175+0000"
		          },
		          {
		            "name": "warning_period",
		            "value": "30",
		            "updated": "2011-02-18T16:17:22.175+0000",
		            "created": "2011-02-18T16:17:22.175+0000"
		          },
		          {
		            "name": "version",
		            "value": "6.1",
		            "updated": "2011-02-18T16:17:22.175+0000",
		            "created": "2011-02-18T16:17:22.175+0000"
		          }
		        ],
		        "multiplier": 1,
		        "productContent": [
		          {
		            "content": {
		              "name": "always-enabled-content",
		              "id": "1",
		              "type": "yum",
		              "modifiedProductIds": [

		              ],
		              "label": "always-enabled-content",
		              "vendor": "test-vendor",
		              "contentUrl": "/foo/path/always",
		              "gpgUrl": "/foo/path/always/gpg",
		              "metadataExpire": 200,
		              "updated": "2011-02-18T16:17:16.254+0000",
		              "created": "2011-02-18T16:17:16.254+0000"
		            },
		            "flexEntitlement": 0,
		            "physicalEntitlement": 0,
		            "enabled": true
		          },
		          {
		            "content": {
		              "name": "never-enabled-content",
		              "id": "0",
		              "type": "yum",
		              "modifiedProductIds": [

		              ],
		              "label": "never-enabled-content",
		              "vendor": "test-vendor",
		              "contentUrl": "/foo/path/never",
		              "gpgUrl": "/foo/path/never/gpg",
		              "metadataExpire": 600,
		              "updated": "2011-02-18T16:17:16.137+0000",
		              "created": "2011-02-18T16:17:16.137+0000"
		            },
		            "flexEntitlement": 0,
		            "physicalEntitlement": 0,
		            "enabled": false
		          },
		          {
		            "content": {
		              "name": "content",
		              "id": "1111",
		              "type": "yum",
		              "modifiedProductIds": [

		              ],
		              "label": "content-label",
		              "vendor": "test-vendor",
		              "contentUrl": "/foo/path",
		              "gpgUrl": "/foo/path/gpg/",
		              "metadataExpire": 0,
		              "updated": "2011-02-18T16:17:16.336+0000",
		              "created": "2011-02-18T16:17:16.336+0000"
		            },
		            "flexEntitlement": 0,
		            "physicalEntitlement": 0,
		            "enabled": true
		          }
		        ],
		        "dependentProductIds": [

		        ],
		        "href": "/products/37060",
		        "updated": "2011-02-18T16:17:22.174+0000",
		        "created": "2011-02-18T16:17:22.174+0000"
		      }
		    ],
		    "endDate": "2012-02-18T00:00:00.000+0000",
		    "startDate": "2011-02-18T00:00:00.000+0000",
		    "quantity": 5,
		    "contractNumber": "39",
		    "accountNumber": "12331131231",
		    "modified": null,
		    "tokens": [

		    ],
		    "upstreamPoolId": null,
		    "updated": "2011-02-18T16:17:38.031+0000",
		    "created": "2011-02-18T16:17:38.031+0000"
		  }
	  */
	
	

	
	

}
