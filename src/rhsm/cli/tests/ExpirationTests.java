package rhsm.cli.tests;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.ProductSubscription;
import rhsm.data.RevokedCert;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandRunner;

@Test(groups={"ExpirationTests"})
public class ExpirationTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36638", "RHEL7-51448"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="subscribe to a pool that will expire soon and assert the entitlements are removed after it expires",
			groups={"Tier2Tests","blockedByBug-655835","blockedByBug-660713","blockedByBug-854312","blockedByBug-907638","blockedByBug-994266","blockedByBug-1555582"}, dependsOnGroups={},
			enabled=true)
	public void testEntitlementsAfterSubscriptionExpires() throws Exception{
		clienttasks.restart_rhsmcertd(certFrequency, null, true);

		// create a subscription pool that will expire 2 minutes from now
		int endingMinutesFromNow = 2;
		String expiringPoolId = createTestPool(-60*24,endingMinutesFromNow);
		
		// assert the expiring pool is currently available
		SubscriptionPool expiringPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", expiringPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(expiringPool,"The expiring SubscriptionPool is currently available for subscribing: "+expiringPool);

		// subscribe
		File expiringCertFile = clienttasks.subscribeToSubscriptionPool_(expiringPool);
		/*EntitlementCert*/ expiringCert = clienttasks.getEntitlementCertFromEntitlementCertFile(expiringCertFile);
		List <ProductSubscription> expiringProductSubscriptions = ProductSubscription.findAllInstancesWithMatchingFieldFromList("serialNumber", expiringCert.serialNumber, clienttasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertMore(expiringProductSubscriptions.size(),0, "Found ProductSubscriptions corresponding to the just subscribed SubscriptionPool: "+expiringPool);
		for (ProductSubscription expiringProductSubscription : expiringProductSubscriptions) {
			Assert.assertTrue(expiringProductSubscription.isActive, "Immediately before the consumed subscription '"+expiringProductSubscription.productName+"' is about the expire, the list --consumed should show it as Active.");
		}
		
		// wait for pool to expire
		sleep(endingMinutesFromNow*60*1000 + 0*1000); // plus a 10 sec buffer; changed to 0 sec - the buffer creates too large of an opportunity for rhsmcertd to run and wipe out the expired cert
		String rhsmcertdLogMarker = System.currentTimeMillis()+" Testing VerifyEntitlementsAfterSubscriptionExpires_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmcertdLogFile, rhsmcertdLogMarker);
///*debugTesting*/sleep(certFrequency*60*1000);	// to delay the assert long enough for rhsmcertd to trigger and delete the expired cert; used to force the code path through the "if (expiredProductSubscription==null)" code block that follows
		// verify that the subscription pool has expired and is therefore not listed in all available; coverage for Bug 655835 - Pools are no longer removed after their expiration date https://github.com/RedHatQE/rhsm-qe/issues/132
		SubscriptionPool expiredPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", expiringPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(expiredPool,"Expired Test Pool ID '"+expiringPoolId+"' is no longer available for subscribing.");
		
		/* The following behavior block of assertions was changed for a few reasons...
		 * 1. Bug 994266 - expired entitlement shows in GUI but not in CLI
		 * 2. We felt that expired entitlements should remain on the system for the system user to acknowledge
		 * 3. The system user can simply acknowledge the expiration by clicking Remove in the GUI tab of "My Subscriptions"
		// verify that the expired product subscriptions are not listed among the consumed
		List <ProductSubscription> currentProductSubscriptions = ProductSubscription.parse(clienttasks.list(null,null,true, null, null, null, null, null, null).getStdout());
		for (ProductSubscription p : expiringProductSubscriptions) {
			Assert.assertTrue(!currentProductSubscriptions.contains(p),"The expired ProductSubscription '"+p+"' no longer appears as consumed.");
		}
		* current behavior is asserted here... */
		// verify that the expired product subscriptions is still listed among the consumed, but inactive
		List <ProductSubscription> currentlyConsumedProductSubscriptions = ProductSubscription.parse(clienttasks.list(null,null,true, null, null, null, null, null, null, null, null, null, null, null, null).getStdout());
		for (ProductSubscription expiringProductSubscription : expiringProductSubscriptions) {
			ProductSubscription expiredProductSubscription = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("serialNumber", expiringProductSubscription.serialNumber, currentlyConsumedProductSubscriptions);
			// catch a corner case...
			if (expiredProductSubscription==null) {	// then we were probably too slow and a trigger of rhsmcertd likely deleted the expired cert.
				// Using this if block of code to confirm that assumption and skip the rest of this test
				sleep(1*1000);	// desperate attempt to give rhsmcertdLogFile a chance to be updated so rhsmcertdLogTail will not be empty
				String rhsmcertdLogTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmcertdLogFile, rhsmcertdLogMarker, null);
				// rhsmcertd:
				//	Fri Aug 26 15:42:19 2016 [INFO] (Cert Check) Certificates updated.
				//---------------------------------------------------------------------------------------------------------
				// rhsm.log:
				//	2016-08-26 15:42:17,819 [DEBUG] rhsmcertd-worker:5398:MainThread @connection.py:573 - Making request: GET /candlepin/consumers/c56bfdb6-ff4c-41b6-84c2-4e0179df3a37/certificates/serials
				//	2016-08-26 15:42:18,016 [DEBUG] rhsmcertd-worker:5398:MainThread @connection.py:602 - Response: status=200, requestUuid=bfd74f37-ef19-4516-8a5f-89d44dc9dc56
				//	2016-08-26 15:42:18,018 [INFO] rhsmcertd-worker:5398:MainThread @entcertlib.py:131 - certs updated:
				//	Total updates: 1
				//	Found (local) serial# [1826226756700020146L]
				//	Expected (UEP) serial# []
				//	Added (new)
				//	  <NONE>
				//	Deleted (rogue):
				//	  [sn:1826226756700020146 (Awesome OS Server Bundled) @ /etc/pki/entitlement/1826226756700020146.pem]
				Assert.assertTrue(rhsmcertdLogTail.trim().endsWith("Certificates updated."), "It appears that the rhsmcertd coincidently triggered too quickly on its frequency of '"+certFrequency+"'min after the entitlement expired causing it to be deleted by the rhsmcertd.");
				throw new SkipException("Skipping the rest of this test since rhsmcertd triggered too closely after the expiration of the entitlement cert for us to do all of our test assertions before rhsmcertd ran.  Hopefully the next test run will complete.");
			}
			Assert.assertNotNull(expiredProductSubscription, "Immediately after the consumed subscription '"+expiringProductSubscription.productName+"' serial '"+expiringProductSubscription.serialNumber+"' expires, it should still be found the list of consumed product subscriptions (assuming that rhsmcertd on certFrequency='"+certFrequency+"'min has not yet triggered before this assert).");
			Assert.assertTrue(!expiredProductSubscription.isActive, "Immediately after the consumed subscription '"+expiringProductSubscription.productName+"' serial '"+expiringProductSubscription.serialNumber+"' expires, the list of consumed product subscriptions should show it as inActive.");
		}
		
		
		// verify that the expired entitlement cert file is gone after a trigger of rhsmcertd.certFrequency
		SubscriptionManagerCLITestScript.sleep(certFrequency*60*1000);
		Assert.assertTrue(!RemoteFileTasks.testExists(client,expiringCertFile.getPath()),"The expired entitlement cert file has been cleaned off the system by rhsmcertd");
		
		// final test in honor of bug 854312 - rhsmcertd removes, then puts, then removes, then puts, then removes, etc... an expired entitlement cert 
		// verify that the expired entitlement cert file is still gone after another trigger a rhsmcertd.certFrequency
		SubscriptionManagerCLITestScript.sleep(certFrequency*60*1000);
		Assert.assertTrue(!RemoteFileTasks.testExists(client,expiringCertFile.getPath()),"After another trigger of the cert frequency, the expired entitlement cert file remains cleaned from the system.");
	}
	
	
	@Test(	description="Verify expired entitlement is added to the certifiate revocation list after the subscription expires",
			groups={"Tier2Tests"}, dependsOnMethods={"testEntitlementsAfterSubscriptionExpires"},
			enabled=false)	// TODO Review the validity of this testcase with development (Current behavior is that expiringCert is NOT actually added to list of RevokedCerts.  Instead they remain on the system for acknowledgment and manual removal)
	public void testExpiredEntitlementIsAddedToCertificateRevocationList() throws Exception{
		if (expiringCert==null) throw new SkipException("This test requires a successful run of a prior test whose entitlement cert has expired."); 

		log.info("Check the CRL list on the server and verify the expired entitlement cert serial is revoked...");
		log.info("Waiting 2 minutes...  (Assuming this is the candlepin.conf value set for pinsetter.org.fedoraproject.candlepin.pinsetter.tasks.CertificateRevocationListTask.schedule)");
		sleep(2*60*1000);	// give the CertificateRevocationListTask.schedule 2 minutes to update the list since that is what was set in setupBeforeSuite()
		// NOTE: The refresh schedule should have been set with a call to servertasks.updateConfigFileParameter in the setupBeforeSuite()
		//       Set inside /etc/candlepin/candlepin.conf
		//       pinsetter.org.fedoraproject.candlepin.pinsetter.tasks.CertificateRevocationListTask.schedule=0 0/2 * * * ?
		// NOTE: if not set, the default is  public static final String DEFAULT_SCHEDULE = "0 0 12 * * ?" Fire at 12pm (noon) every day
		RevokedCert revokedCert = RevokedCert.findFirstInstanceWithMatchingFieldFromList("serialNumber",expiringCert.serialNumber,servertasks.getCurrentlyRevokedCerts());
		Assert.assertTrue(revokedCert!=null,"Expiring entitlement certificate serial number '"+expiringCert.serialNumber+"' has been added to the Certificate Revocation List (CRL) as: "+revokedCert);
		Assert.assertEquals(revokedCert.reasonCode, "Privilege Withdrawn","An expired entitlement certificate should be revoked with a reason code of Privilege Withdrawn.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37710", "RHEL7-51450"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="assert that a soon to expire pool is removed from availablity after it expires",
			groups={"Tier2Tests","blockedByBug-1555582"}, dependsOnGroups={},
			enabled=true)
	public void testPoolIsRemovedAfterSubscriptionExpires() throws Exception {
		
		// create a subscription pool that will expire 2 minutes from now
		int endingMinutesFromNow = 2;
		String expiringPoolId = createTestPool(-60*24,endingMinutesFromNow);
		
		// assert the expiring pool is currently available
		SubscriptionPool expiringPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", expiringPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(expiringPool,"The expiring SubscriptionPool is currently available for subscribing: "+expiringPool);

		// wait for pool to expire
		sleep(endingMinutesFromNow*60*1000 + 30*1000); // plus a 30 sec buffer;
		
		// assert pool is no longer available
		SubscriptionPool expiredPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", expiringPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(expiredPool,"The expired SubscriptionPool is no longer available for subscribing");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-37709", "RHEL7-51449"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="assert that a soon to be available pool is not yet available",
			groups={"Tier2Tests","blockedByBug-1555582"}, dependsOnGroups={},
			enabled=true)
	public void testPoolIsNotYetAvailableUntilTheStartDate() throws Exception {
		
		// create a subscription pool that will start 2 minutes from now
		int startingMinutesFromNow = 3;
		String aboutToStartPoolId = createTestPool(startingMinutesFromNow,/*60*24*/startingMinutesFromNow+2);
	
		// assert the starting pool is NOT currently available
		SubscriptionPool startingPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", aboutToStartPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(startingPool,"The about to start SubscriptionPool with id '"+aboutToStartPoolId+"' is currently NOT YET available for subscribing.");

		// wait for pool to start
		sleep((startingMinutesFromNow-1)*60*1000 + 0*1000); // plus a 30 sec buffer;
	
		// assert the starting pool is still NOT currently available (1 minute before its startDate)
		startingPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", aboutToStartPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNull(startingPool,"The about to start SubscriptionPool with id '"+aboutToStartPoolId+"' is still NOT YET available for subscribing (1 minute before its startDate).");

		// wait one more minute for pool to start
		sleep(1*60*1000 + 30*1000); // plus a 30 sec buffer;

		// assert the starting pool is NOW currently available
		startingPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", aboutToStartPoolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(startingPool,"The about to start SubscriptionPool with id '"+aboutToStartPoolId+"' is now available for subscribing: "+startingPool);

	}
	
	
	// Candidates for an automated Test:
	// TODO Bug 852630 - subscription-manager unsubscribe -all on expired subscriptions says "[Errno 2] No such file or directory: '/etc/pki/entitlement/1364069144416875315.pem'"	// DONE by skallesh https://github.com/RedHatQE/rhsm-qe/issues/133
	
	

	
	
	
	// Configuration methods ***********************************************************************
	
	
	@BeforeClass(groups="setup")
	public void skipIfHosted() {
		if (!sm_serverType.equals(CandlepinType.standalone)) throw new SkipException("These tests are only valid for standalone candlepin servers.");
	}
	
	@BeforeClass(groups="setup", dependsOnMethods="skipIfHosted")
	public void assertTimeDifferenceWithServerBeforeClass() throws ParseException{
		// Assert that the difference in the system clocks on runners is less that the specified milliseconds.
		long msTimeDiffAllowed = 60000L;
		if (client1!=null) {
			Assert.assertLess(Math.abs(getTimeDifferenceBetweenCommandRunners(client1, server)), msTimeDiffAllowed, "Time difference between '"+client1.getConnection().getRemoteHostname()+"' and '"+server.getConnection().getRemoteHostname()+"' is less than '"+msTimeDiffAllowed+"' milliseconds");
		}
		if (client2!=null) {
			Assert.assertLess(Math.abs(getTimeDifferenceBetweenCommandRunners(client2, server)), msTimeDiffAllowed, "Time difference between '"+client2.getConnection().getRemoteHostname()+"' and '"+server.getConnection().getRemoteHostname()+"' is less than '"+msTimeDiffAllowed+"' milliseconds");
		}
	}
	
	@BeforeClass(groups="setup", dependsOnMethods="skipIfHosted")
	public void registerBeforeClass() throws Exception {
		clienttasks.unregister(null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null, null));
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);
	}
	
	@BeforeClass(groups="setup", dependsOnMethods="registerBeforeClass")
	public void findRandomAvailableProductIdBeforeClass() throws Exception {

		// find a randomly available product id
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		
		// debugging bugzilla 760162
		//pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", "awesomeos-virt-4", pools);
		
		randomAvailableProductId = pool.productId;
	}
	
	@BeforeClass(groups="setup")
		public void storeCertFrequencyBeforeClass() throws Exception{
		originalCertFrequency = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsmcertd", /*"certFrequency" CHANGED BY BUG 882459 TO*/"certCheckInterval");
	}
	@AfterClass(groups="setup")
	public void restoreCertFrequencyAfterClass() throws Exception{
		if (originalCertFrequency==null) return;
		clienttasks.restart_rhsmcertd(Integer.valueOf(originalCertFrequency), null, true);
	}
	
//	@BeforeMethod
//	public void createTestPoolsBeforeMethod() throws Exception{
//		
//		Calendar cal = new GregorianCalendar();
//		cal.add(Calendar.MINUTE, 2); 	// pool will expire in 2 min from now
//		Date _3min = cal.getTime();
//		cal.add(Calendar.DATE, -21);
//		Date _3weeksago = cal.getTime();
//		/*List<InstalledProduct> clientProds = clienttasks.getCurrentlyInstalledProducts();
//		String product = clientProds.get(0).productName;*/
//		String[] providedProducts = {"37068", "37069", "37060"};
//		String requestBody = CandlepinTasks.createPoolRequest(10, _3weeksago, _3min, "MKT-rhel-server", 123, providedProducts).toString();
//		CandlepinTasks.postResourceUsingRESTfulAPI(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, "/owners/" + owner + "/subscriptions", requestBody);
//		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, owner);
//		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,serverAdminUsername,serverAdminPassword, jobDetail, "FINISHED", 5*1000, 1);
//
//	}
	

	
	
	// protected methods ***********************************************************************
	
	protected String ownerKey = "";
	protected String randomAvailableProductId = null;
	protected EntitlementCert expiringCert = null;
	protected String originalCertFrequency = null;
	protected final int certFrequency = 1;
	
	protected void checkTime(String host, SSHCommandRunner runner)throws Exception {
		//make sure local clock and server clock are synced
		Date localTime = Calendar.getInstance().getTime();
		Date remoteTime; 
		//SSHCommandRunner runner = new SSHCommandRunner("jweiss-rhel6-1.usersys.redhat.com", sshUser, sshKeyPrivate, sshkeyPassphrase, null);
		runner.runCommandAndWait("date");
		String serverDateStr = runner.getStdout();
		SimpleDateFormat unixFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
		remoteTime = unixFormat.parse(serverDateStr);
		long timeDiffms = Math.abs(localTime.getTime() - remoteTime.getTime());
		Assert.assertLess(timeDiffms, 60000L, "Time difference with " + host + " is less than 1 minute");
	}
	
	/**
	 * @param startingMinutesFromNow
	 * @param endingMinutesFromNow
	 * @return poolId to the newly available SubscriptionPool
	 * @throws JSONException
	 * @throws Exception
	 */
	protected String createTestPool(int startingMinutesFromNow, int endingMinutesFromNow) throws JSONException, Exception  {
		
		if (true) return CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, ownerKey, 3, startingMinutesFromNow, endingMinutesFromNow, getRandInt(), getRandInt(), randomAvailableProductId, null, null).getString("id");
// TODO DELETE THE REST OF THIS METHOD'S CODE WHEN WE KNOW THE ABOVE CANDLEPIN TASK IS WORKING 8/12/2011
		
		// set the start and end dates
		Calendar endCalendar = new GregorianCalendar();
		endCalendar.add(Calendar.MINUTE, endingMinutesFromNow);
		Date endDate = endCalendar.getTime();
		Calendar startCalendar = new GregorianCalendar();
		startCalendar.add(Calendar.MINUTE, startingMinutesFromNow);
		Date startDate = startCalendar.getTime();

		
		// randomly choose a contract number
		Integer contractNumber = Integer.valueOf(getRandInt());
		
		// randomly choose an account number
		Integer accountNumber = Integer.valueOf(getRandInt());
		
		// choose a product id for the subscription
		//String productId =  "MKT-rhel-server";  // too hard coded
		//JSONArray jsonProducts = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,serverAdminUsername,serverAdminPassword,"/products"));	
		//String productId = null;
		//do {	// pick a random productId (excluding a personal productId) // too random; could pick a product that is not available to this system
		//	productId =  ((JSONObject) jsonProducts.get(randomGenerator.nextInt(jsonProducts.length()))).getString("id");
		//} while (getPersonProductIds().contains(productId));
		String productId = randomAvailableProductId;

		// choose providedProducts for the subscription
		//String[] providedProducts = {"37068", "37069", "37060"};
		//String[] providedProducts = {
		//	((JSONObject) jsonProducts.get(randomGenerator.nextInt(jsonProducts.length()))).getString("id"),
		//	((JSONObject) jsonProducts.get(randomGenerator.nextInt(jsonProducts.length()))).getString("id"),
		//	((JSONObject) jsonProducts.get(randomGenerator.nextInt(jsonProducts.length()))).getString("id")
		//};
		List<String> providedProducts = null;
		
		// create the subscription
		String requestBody = CandlepinTasks.createSubscriptionRequestBody(sm_serverUrl, 3, startDate, endDate, productId, contractNumber, accountNumber, providedProducts, null).toString();
		JSONObject jsonSubscription = new JSONObject(CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, "/owners/" + ownerKey + "/subscriptions", requestBody));
		
		// refresh the pools
		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, ownerKey);
		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,jobDetail,"FINISHED", 5*1000, 1);
		
		// assemble an activeon parameter set to the start date so we can pass it on to the REST API call to find the created pool
		DateFormat iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");				// "2012-02-08T00:00:00.000+0000"
		String iso8601FormatedDateString = iso8601DateFormat.format(startDate);
		iso8601FormatedDateString = iso8601FormatedDateString.replaceFirst("(..$)", ":$1");				// "2012-02-08T00:00:00.000+00:00"	// see https://bugzilla.redhat.com/show_bug.cgi?id=720493 // http://books.xmlschemata.org/relaxng/ch19-77049.html requires a colon in the time zone for xsd:dateTime
		String urlEncodedActiveOnDate = java.net.URLEncoder.encode(iso8601FormatedDateString, "UTF-8");	// "2012-02-08T00%3A00%3A00.000%2B00%3A00"	encode the string to escape the colons and plus signs so it can be passed as a parameter on an http call

		// loop through all pools available to owner and find the newly created poolid corresponding to the new subscription id activeon startDate
		String poolId = null;
		JSONArray jsonPools = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,"/owners/"+ownerKey+"/pools"+"?activeon="+urlEncodedActiveOnDate));	
		for (int i = 0; i < jsonPools.length(); i++) {
			JSONObject jsonPool = (JSONObject) jsonPools.get(i);
			//if (contractNumber.equals(jsonPool.getInt("contractNumber"))) {
			if (jsonPool.getString("subscriptionId").equals(jsonSubscription.getString("id"))) {
				poolId = jsonPool.getString("id");
				break;
			}
		}
		Assert.assertNotNull(poolId,"Found newly created pool corresponding to the newly created subscription with id: "+jsonSubscription.getString("id"));
		log.info("The newly created subscription pool with id '"+poolId+"' will start '"+startingMinutesFromNow+"' minutes from now.");
		log.info("The newly created subscription pool with id '"+poolId+"' will expire '"+endingMinutesFromNow+"' minutes from now.");
		return poolId; // return poolId to the newly available SubscriptionPool
		
	}
	
	
//	protected Predicate<SubscriptionPool> expToday = new Predicate<SubscriptionPool>(){
//		public boolean apply(SubscriptionPool pool){
//			Calendar cal = pool.endDate;
//			Calendar today = new GregorianCalendar();
//			return (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) && (cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) ;
//		}
//	};
//
//	class ByIdPredicate implements Predicate<SubscriptionPool> {
//		String id;
//		public ByIdPredicate(String id) {
//			this.id=id;
//		}
//		public boolean apply(SubscriptionPool pool) {
//			return this.id.equals(pool.poolId);
//		}
//	}
//	
//	protected SubscriptionPool getPool() {
//		Collection<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
//		
//		Collection<SubscriptionPool> expiresToday = Collections2.filter(pools, expToday);
//		
//		//choose first pool
//		return  expiresToday.iterator().next();
//	}
	
}
