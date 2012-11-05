package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.EntitlementCert;
import rhsm.data.ProductSubscription;
import rhsm.data.RevokedCert;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author jsefler
 *
 */
@Test(groups={"CRLTests"})
public class CRLTests extends SubscriptionManagerCLITestScript{
	String ownerKey = null;

	// Test Methods ***********************************************************************

	@Test(	description="subscription-manager-cli: change subscription pool start/end dates and refresh subscription pools",
			groups={"ChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test"},
			dependsOnGroups={},
			//dataProvider="getAvailableSubscriptionPoolsData",	// very thorough, but takes too long to execute and rarely finds more bugs
			dataProvider="getRandomSubsetOfAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsNitrateTest(caseId=56025)
	public void ChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test(SubscriptionPool originalPool) throws Exception {
		//		https://tcms.engineering.redhat.com/case/56025/?from_plan=2634
		//		Actions:
		//
		//		    * In the db list the subscription pools, find a unique pool to work with
		//		    * On a sm client register, subscribe to the pool
		//		    * In the db changed the start/end dates for the subscription pool (cp_subscription)
		//		    * using the server api, refresh the subscription pools
		//		    * on the client check the entitlement certificates ls /etc/pki/entitlement/product
		//		    * use openssl x509 to inspect the certs, notice the start / end dates 
		//		    * on the client restart the rhsmcertd service
		//		    * on the client check the entitlement certificates ls /etc/pki/entitlement/product
		//		    * use openssl x509 to inspect the certs, notice the start / end dates
		//		    * check the crl list on the server and verify the original entitlement cert serials are present 
		//
		//		Expected Results:
		//
		//			* the original entitlement certificates on the client should be removed
		//		   	* new certs should be dropped to the client
		//			* the crl list on the server should be poplulated w/ the old entitlement cert serials

		if (dbConnection==null) throw new SkipException("This testcase requires a connection to the candlepin database.");
		
		/* https://bugzilla.redhat.com/show_bug.cgi?id=663455#c1 < DUE TO THESE IMPLEMENTED CHANGES, THE FOLLOWING IS NO LONGER APPROPRIATE...
		// Before proceeding with this test, determine if the productId provided by this subscription pool has already been entitled.
		// This will happen when more than one pool has been created under a different contract/serial so as to increase the
		// total quantity of entitlements available to the consumers.
		if (alreadySubscribedProductIdsInChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test.contains(pool.productId)) {
			log.info("Because the productId '"+pool.productId+"' from this pool has already been subscribed to via a previously available pool, it only makes sense to skip this iteration of the test.");
			log.info("However, for the sake of testing, let's attempt to subscribe to it anyway and assert that our subscribe request is blocked with an appropriate message...");
			SSHCommandResult sshCommandResult = clienttasks.subscribe(pool.poolId, null, null, null, null, null, null, null);
			Assert.assertEquals(sshCommandResult.getStdout().trim(),"This consumer is already subscribed to the product matching pool with id '"+pool.poolId+"'.");
			throw new SkipException("Because this consumer is already subscribed to the product ("+pool.productId+") provided by this pool id '"+pool.poolId+".', this pool is unsubscribeable and therefore we must skip this test iteration.");
		}
		*/
		List<ProductSubscription> originalConsumedProducts = clienttasks.getCurrentlyConsumedProductSubscriptions();

		// With the introduction of virt-guest pools, it is possible that a former invocation of this test has changed the validity dates on this pool,
		// therefore, let's re-fetch the SubscriptionPool object that we are about to test.
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", originalPool.poolId, clienttasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNotNull(pool, "Successfully found the SubscriptionPool with the poolId that we are about to test from list --available.");
	
		log.info("Subscribe client (already registered as a system under username '"+sm_clientUsername+"') to subscription pool "+pool+"...");
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool);
		Assert.assertNotNull(entitlementCertFile, "Our attempt to subscribe resulted in a new entitlement cert on our system.");
		alreadySubscribedProductIdsInChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test.add(pool.productId);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);

		log.info("Verify that the currently consumed product subscriptions that came from this subscription pool have the same start and end date as the pool...");
		List<ProductSubscription> originalProducts = ProductSubscription.findAllInstancesWithMatchingFieldFromList("serialNumber",entitlementCert.serialNumber, clienttasks.getCurrentlyConsumedProductSubscriptions());
		Assert.assertFalse(originalProducts.isEmpty(),"After subscribing to a new pool, at least one consumed product subscription matching the entitlement cert's serial number '"+entitlementCert.serialNumber+"' is expected.");

		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=660713 - jsefler 12/12/2010
		Boolean invokeWorkaroundWhileBugIsOpen = true;
		try {String bugId="660713"; if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("The workaround while this bug is open is to skip the assertion that: The original end date for the subscribed product matches the end date of the subscription pool '"+pool.subscriptionName+"' from where it was entitled.");
		} else {
		// END OF WORKAROUND
		
			for (ProductSubscription originalProduct : originalProducts) {
				Assert.assertEquals(originalProduct.accountNumber,originalProducts.get(0).accountNumber,"All of the consumed product subscription from this pool '"+pool.poolId+"' should have the same accountNumber.");
				Assert.assertEquals(originalProduct.contractNumber,originalProducts.get(0).contractNumber,"All of the consumed product subscription from this pool '"+pool.poolId+"' should have the same contractNumber.");
				Assert.assertEquals(originalProduct.serialNumber,originalProducts.get(0).serialNumber,"All of the consumed product subscription from this pool '"+pool.poolId+"' should have the same serialNumber.");
				Assert.assertEquals(originalProduct.isActive,originalProducts.get(0).isActive,"All of the consumed product subscription from this pool '"+pool.poolId+"' should have the same active status.");
				Assert.assertTrue(originalProduct.startDate.compareTo(originalProducts.get(0).startDate)==0, "All of the consumed product subscription from this pool '"+pool.poolId+"' should have the same startDate.");
				Assert.assertTrue(originalProduct.endDate.compareTo(pool.endDate)==0, "The original end date ("+ProductSubscription.formatDateString(originalProduct.endDate)+") for the subscribed product '"+originalProduct.productName+"' matches the end date ("+SubscriptionPool.formatDateString(pool.endDate)+") of the subscription pool '"+pool.subscriptionName+"' from where it was entitled.");
			}
		}
		
		File originalEntitlementCertFile = entitlementCertFile; //new File(clienttasks.entitlementCertDir+"/"+products.get(0).serialNumber+".pem");
		Calendar originalStartDate = (Calendar) originalProducts.get(0).startDate.clone();
		Calendar originalEndDate = (Calendar) originalProducts.get(0).endDate.clone();
		Integer contractNumber = originalProducts.get(0).contractNumber;
		Assert.assertTrue(RemoteFileTasks.testExists(client, originalEntitlementCertFile.getPath()),"Original entitlement cert file '"+originalEntitlementCertFile+"' exists.");
		
		log.info("Now we will change the start and end date of the subscription pool adding one month to enddate and subtracting one month from startdate...");
		Calendar newStartDate = (Calendar) originalStartDate.clone(); newStartDate.add(Calendar.MONTH, -1);
		Calendar newEndDate = (Calendar) originalEndDate.clone(); newEndDate.add(Calendar.MONTH, 1);
		updateSubscriptionPoolDatesOnDatabase(pool,newStartDate,newEndDate);
		
		log.info("Now let's refresh the subscription pools...");
		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,ownerKey);
		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl,jobDetail,"FINISHED", 10*1000, 3);
		log.info("Refresh to make sure the latest entitlement certs are on the client...");
		clienttasks.refresh(null, null, null); // makes sure the new entitlement is downloaded

		log.info("The updated certs should now be on the client...");

		log.info("First, let's assert that the original entitlement cert file '"+originalEntitlementCertFile+"' is gone...");
		Assert.assertTrue(!RemoteFileTasks.testExists(client, originalEntitlementCertFile.getPath()),"Original certificate file '"+originalEntitlementCertFile+"' has been removed from the system.");

		log.info("Second, let's assert that the consumed products have been refreshed...");
		//List<ProductSubscription> newProducts = ProductSubscription.findAllInstancesWithMatchingFieldFromList("contractNumber",contractNumber, clienttasks.getCurrentlyConsumedProductSubscriptions());
		//Assert.assertEquals(newProducts.size(), originalProducts.size(),"The number of ProductSubscriptions corresponding to this subscription's original contract number ("+contractNumber+") remains the same.");
		List<ProductSubscription> newProducts = new ArrayList<ProductSubscription>();
		for (ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			if (!originalConsumedProducts.contains(productSubscription)) newProducts.add(productSubscription);
		}
		Assert.assertEquals(newProducts.size(), originalProducts.size(),"The number of ProductSubscriptions altered after changing the subscription's start/end dates remains confined to original consumed products.");

		BigInteger newSerialNumber = newProducts.get(0).serialNumber;
		File newCertFile = new File(clienttasks.entitlementCertDir+"/"+newSerialNumber+".pem");
		Assert.assertNotSame(newCertFile, originalEntitlementCertFile,"The newly granted and refresh entitlement cert file should not be the same as the original cert file.");
		Assert.assertTrue(RemoteFileTasks.testExists(client, newCertFile.getPath()),"New entitlement certificate file '"+newCertFile+"' exists.");
		for (ProductSubscription newProduct : newProducts) {
			Assert.assertEquals(ProductSubscription.formatDateString(newProduct.startDate), ProductSubscription.formatDateString(newStartDate),
					"Rhsmcertd has updated the entitled startdate from '"+ProductSubscription.formatDateString(originalStartDate)+"' to '"+ProductSubscription.formatDateString(newStartDate)+"' for consumed product '"+newProduct.productName+"' that originated from poolId '"+pool.poolId+"'.");
			Assert.assertEquals(ProductSubscription.formatDateString(newProduct.endDate), ProductSubscription.formatDateString(newEndDate),
					"Rhsmcertd has updated the entitled enddate from '"+ProductSubscription.formatDateString(originalEndDate)+"' to '"+ProductSubscription.formatDateString(newEndDate)+"' for consumed product '"+newProduct.productName+"' that originated from poolId '"+pool.poolId+"'.");

			log.info("And, let's assert that consumed product entitlement serial has been updated...");
			Assert.assertTrue(!newProduct.serialNumber.equals(originalProducts.get(0).serialNumber) && newProduct.serialNumber.equals(newSerialNumber), 
					"The consumed product entitlement serial has been updated from '"+originalProducts.get(0).serialNumber+"' to '"+newSerialNumber+"' for consumed product '"+newProduct.productName+"' that originated from poolId '"+pool.poolId+"'.");

			log.info("Let's assert that the following consumed product attributes have been left unchanged...");
			Assert.assertEquals(newProduct.accountNumber,originalProducts.get(0).accountNumber,"The consumed product accountNumber remains unchanged for '"+newProduct.productName+"' after its pool (poolId='"+pool.poolId+"') start/end dates have been modified and refreshed.");
			Assert.assertEquals(newProduct.contractNumber,originalProducts.get(0).contractNumber,"The consumed product contractNumber remains unchanged for '"+newProduct.productName+"' after its pool (poolId='"+pool.poolId+"') start/end dates have been modified and refreshed.");
			Assert.assertEquals(newProduct.isActive,originalProducts.get(0).isActive,"The consumed product active status remains unchanged for '"+newProduct.productName+"' after its pool (poolId='"+pool.poolId+"') start/end dates have been modified and refreshed.");
		}

		log.info("Third, let's assert that subscription pool reflects the new end date...");
		clienttasks.unsubscribeFromSerialNumber(newSerialNumber);
		pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", pool.poolId, clienttasks.getCurrentlyAvailableSubscriptionPools());
		Assert.assertNotNull(pool, "Successfully found the SubscriptionPool with the poolId that we just unsubscribed from and searched for in list --available.");
		Assert.assertEquals(SubscriptionPool.formatDateString(pool.endDate), SubscriptionPool.formatDateString(newEndDate),
				"As seen by the client, the enddate of the refreshed pool '"+pool.poolId+"' ("+pool.subscriptionName+") has been changed from '"+SubscriptionPool.formatDateString(originalEndDate)+"' to '"+SubscriptionPool.formatDateString(newEndDate)+"'.");

		log.info("Forth, in honor of bugzillas 682930, 679617 let's assert that after unsubscribing, the subscription pool's original quantity is restored...");
		Assert.assertEquals(pool.quantity,originalPool.quantity,
				"Assuming that nobody else has recently subscribed to this pool, the original pool quantity should be restored after updating the subscription's start/end dates and unsubscribing from the poolId '"+pool.poolId+"' ("+pool.subscriptionName+").");

		log.info("Finally, check the CRL list on the server and verify the original entitlement cert serial is revoked...");
		log.info("Waiting 2 minutes...  (Assuming this is the candlepin.conf value set for pinsetter.org.fedoraproject.candlepin.pinsetter.tasks.CertificateRevocationListTask.schedule)");
		sleep(2*60*1000);	// give the CertificateRevocationListTask.schedule 2 minutes to update the list since that is what was set in setupBeforeSuite()
		// NOTE: The refresh schedule should have been set with a call to servertasks.updateConfigFileParameter in the setupBeforeSuite()
		//       Set inside /etc/candlepin/candlepin.conf
		//       pinsetter.org.fedoraproject.candlepin.pinsetter.tasks.CertificateRevocationListTask.schedule=0 0/2 * * * ?
		// NOTE: if not set, the default is  public static final String DEFAULT_SCHEDULE = "0 0 12 * * ?" Fire at 12pm (noon) every day
		RevokedCert revokedCert = RevokedCert.findFirstInstanceWithMatchingFieldFromList("serialNumber",entitlementCert.serialNumber,servertasks.getCurrentlyRevokedCerts());
		Assert.assertTrue(revokedCert!=null,"Original entitlement certificate serial number '"+entitlementCert.serialNumber+"' granted from pool '"+pool.poolId+"' ("+pool.subscriptionName+") has been added to the Certificate Revocation List (CRL) as: "+revokedCert);
		Assert.assertEquals(revokedCert.reasonCode, "Privilege Withdrawn","Expanding the certificate start and end dates should revoke the certificated with a reason code of Privilege Withdrawn.");

		// just to add some complexity to succeeding dataProvided runs of this test, let's re-subscribe to this pool
		// FIXME The following will cause failures for pools that originate from a virtualization aware subscription.  It would be nice to re-subscribe, but first we need to determine if this pool came from a virt-aware subscription and excude it from the resubscribe
		//clienttasks.subscribeToSubscriptionPool(pool);
	}
	protected List<String> alreadySubscribedProductIdsInChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test = new ArrayList<String>();

	
	
	// Candidates for an automated Test:
//	On 10/07/2011 03:34 PM, Michael Stead wrote:
//		> *Revoking Entitlements When A Guest's Host Changes*
//		>
//		> There has been some discussion on when/where the revoke should take
//		> place, with a concern being the state of the client afterwards, and how
//		> long the client's and CPs entitlements/certs are out of sync.
//		>
//		> As well, do we try and 'replace' the revoked entitlements with one
//		> provided by the new host (if available) at the same time? Or do we leave
//		> the client in the current invalid state and wait for healing to kick in
//		> (if enabled) to fix? or force fixing manually if not?
//		>
//		> I'd like to get some feedback on the discussed solutions below so that
//		> we are all in the loop of how this will be implemented.
//		>
//		>
//		> *1) On certlib check-in *
//		> Assuming that virt-who has run on two hosts, moving a guest from one to
//		> the other, the next time a certlib checkin runs on the guest (client):
//		> - certlib requests the guest's current serials from CP (GET
//		> consumers/{uuid}/serials).
//		> - Here we determine if the host has changed, revoke any old host related
//		> entitlements, and omit the serials from returned serials list.
//		> - certlib determines from the serials what is missing/rogue and will
//		> fetch/delete the missing/rogue certificates (as it works now).
//		>
//		> Edge Case: What happens if the guest was blatantly destroyed on the host
//		> without being unregistered? The guest's entitlements would just sit.
//		> - Could we create a job that cleans this up?
//		>
//		> PROS:
//		> - certs are updated on the client soon after they are revoked.
//		> - provides an opportunity (outside of healing) to migrate the revoked
//		> entitlements with similar ones from the new host), if required.
//		>
//		> CONS:
//		> - we are revoking entitlements as part of a GET request. It feels wrong
//		> and miss-leading in terms of the CP API. But would work.
//		>
//		>
//		> *2) Revoke on hosts updateConsumer triggered by virt-who.*
//		> Determine what guests were removed from the host, and immediately revoke
//		> all 'host related' entitlements from EACH removed guest when virt-who
//		> calls updateConsumer.
//		>
//		> PROS:
//		> - in terms of CP API, this seems like the right place to have this code.
//		> - entitlements are revoked from each guest as soon as virt-who reports it.
//		> - Although an error would be shown in subscription manager when an
//		> unsubscribe was attempted on a revoked cert, all certs would be updated
//		> once the error dialog was closed (via certlib.update()). Can we live
//		> with this?
//		>
//		> CONS:
//		> - client certs are now out of sync until next certlib checkin (this
//		> could be via rhsmd, or a subscribe/unsubscribe, etc.)
//		> - Length of time it may take until client certs are updated on the client.
//		> - no opportunity to migrate the entitlements to the new host (without
//		> healing) as it may not have been reported by virt-who at this point.
//		>
//		> *3) Revoke on guest's updateConsumer call.*
//		> Entitlements get revoked by CP on updateConsumer if the guest's host has
//		> changed, putting the client in an invalid state until next cert check-in.
//		>
//		> PROS:
//		> - same as #2
//		>
//		> CONS:
//		> - same as #2
//		> - Longer update time. Need to wait for guest's updateConsumer to be
//		> called, and then for certlib check-in to update entitlements.
//		>
//		>
//		> I'm sure that I've missed something here, but what do you guys think?
//		>
//		> I'm currently working on doing it via option 1, but doing the revoke in
//		> a GET request feels funny.
//		>
//		> Either way, the code that I've been working on to do the revoke can be
//		> moved around if need be.
//		>
//		> Thoughts?
//		>
//		> Thanks.
//		>
//		> --mstead
//		>
//		>
//		I like (2). The cons are no different than any other
//		subscriprtion--manager case.
//
//		-- bk
	
	// Configuration Methods ***********************************************************************

	@BeforeGroups(groups={"setup"},value="ChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test")
	protected void getClientOwnerBeforeGroups() throws JSONException, Exception {
		String consumerId = clienttasks.getCurrentConsumerId();
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_clientUsername, sm_clientPassword, sm_serverUrl, consumerId);

	}

	
	// Protected Methods ***********************************************************************

	
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
		
		if(endDate != null)
			assertChangesToDb(pool, endDate, "enddate");
		if(startDate != null)
			assertChangesToDb(pool, startDate, "startdate");
	}
	
	// FIXME Used while debugging with AJAY
	private void assertChangesToDb(SubscriptionPool pool, Calendar cal, String field) throws SQLException {
		Connection dbConnection1 = null;
		Statement sql = null;
		try {
			
			// Load the JDBC driver 
			Class.forName(sm_dbSqlDriver);	//	"org.postgresql.Driver" or "oracle.jdbc.driver.OracleDriver"
			
			// Create a connection to the database
			String url = sm_dbSqlDriver.contains("postgres")? 
					"jdbc:postgresql://" + sm_dbHostname + ":" + sm_dbPort + "/" + sm_dbName :
					"jdbc:oracle:thin:@" + sm_dbHostname + ":" + sm_dbPort + ":" + sm_dbName ;
			log.info(String.format("Attempting to connect to database with url and credentials: url=%s username=%s password=%s",url,sm_dbUsername,sm_dbPassword));
			dbConnection1 = DriverManager.getConnection(url, sm_dbUsername, sm_dbPassword);
			sql = dbConnection1.createStatement();
			String getSubscriptionPoolEndDateSql = String
					.format("select %s from cp_subscription where id=(select subscriptionid from cp_pool where id='%s');", field, pool.poolId);
			sql.execute(getSubscriptionPoolEndDateSql);
			ResultSet resultSet = sql.getResultSet();
			if (resultSet.next()) {
				log.fine(String.format("The %s in database is = %s", field, resultSet.getDate(1)));
				Assert.assertEquals(resultSet.getDate(1), cal.getTime(),"The '"+field+"' in the database is what we expect.");
			} else {
				Assert.fail("Did not retrieve any value for '"+field+"' from the database for pool: "+pool);
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}finally{
			try {
				sql.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				dbConnection1.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
