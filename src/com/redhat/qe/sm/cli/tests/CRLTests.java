package com.redhat.qe.sm.cli.tests;

import java.io.File;
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

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.BzChecker;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.RevokedCert;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author jsefler
 *
 */
@Test(groups={"crl"})
public class CRLTests extends SubscriptionManagerCLITestScript{
	String consumerOwner = "";
	@BeforeGroups(groups={"setup"},value="ChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test")
	protected void getClientOwner() throws JSONException, Exception {
		String consumerId = clienttasks.getCurrentConsumerId();
		consumerOwner = CandlepinTasks.getOwnerOfConsumerId(serverHostname, serverPort, serverPrefix, serverAdminUsername, serverAdminPassword, consumerId).getString("key");

	}
	@Test(	description="subscription-manager-cli: change subscription pool start/end dates and refresh subscription pools",
			groups={"ChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test"},
			dependsOnGroups={},
			dataProvider="getAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsNitrateTest(caseId=56025)
	public void ChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test(SubscriptionPool pool) throws Exception {
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
		
		
		// Before proceeding with this test, determine if the productId provided by this subscription pool has already been entitled.
		// This will happen when more than one pool has been created under a different contract/serial so as to increase the
		// total quantity of entitlements available to the consumers.
		if (alreadySubscribedProductIdsInChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test.contains(pool.productId)) {
			log.info("Because the productId '"+pool.productId+"' from this pool has already been subscribed to via a previously available pool, it only makes sense to skip this iteration of the test.");
			log.info("However, for the sake of testing, let's attempt to subscribe to it anyway and assert that our subscribe request is blocked with an appropriate message...");
			SSHCommandResult sshCommandResult = clienttasks.subscribe(pool.poolId, null, null, null, null, null, null, null);
			Assert.assertEquals(sshCommandResult.getStdout().trim(),"This consumer is already subscribed to the product matching pool with id '"+pool.poolId+"'");
			throw new SkipException("Because this consumer is already subscribed to the product ("+pool.productId+") provided by this pool id '"+pool.poolId+"', this pool is unsubscribeable and therefore we must skip this test iteration.");
		}
		
		
		log.info("Subscribe client (already registered as a system under username '"+clientusername+"') to subscription pool "+pool+"...");
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool);
		Assert.assertNotNull(entitlementCertFile, "Our attempt to subscribe resulted in a new entitlement cert on our system.");
		alreadySubscribedProductIdsInChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test.add(pool.productId);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);

		log.info("Verify that the currently consumed product subscriptions that came from this subscription pool have the same start and end date as the pool...");
		List<ProductSubscription> products = ProductSubscription.findAllInstancesWithMatchingFieldFromList("serialNumber",entitlementCert.serialNumber, clienttasks.getCurrentlyConsumedProductSubscriptions());

		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=660713 - jsefler 12/12/2010
		Boolean invokeWorkaroundWhileBugIsOpen = true;
		try {String bugId="660713"; if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla bug "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("The workaround while this bug is open is to skip the assertion that: The original end date for the subscribed product matches the end date of the subscription pool '"+pool.subscriptionName+"' from where it was entitled.");
		} else {
		// END OF WORKAROUND
		
			for (ProductSubscription product : products) {
				//FIXME Available Subscriptions	does not display start date			Assert.assertEquals(product.startDate, pool.startDate, "The original start date ("+product.startDate+") for the subscribed product '"+product.productName+"' matches the start date ("+pool.startDate+") of the subscription pool '"+pool.subscriptionName+"' from where it was entitled.");
				Assert.assertTrue(product.endDate.compareTo(pool.endDate)==0, "The original end date ("+ProductSubscription.formatDateString(product.endDate)+") for the subscribed product '"+product.productName+"' matches the end date ("+SubscriptionPool.formatDateString(pool.endDate)+") of the subscription pool '"+pool.subscriptionName+"' from where it was entitled.");
			}
		}	
				
		Assert.assertFalse(products.isEmpty(),"After subscribing to a new pool, at least one consumed product subscription is expected.");
		
		Calendar originalStartDate = (Calendar) products.get(0).startDate.clone();
		Calendar originalEndDate = (Calendar) products.get(0).endDate.clone();
		String originalCertFile = clienttasks.entitlementCertDir+"/"+products.get(0).serialNumber+".pem";
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, originalCertFile),1,"Original certificate file '"+originalCertFile+"' exists.");
		
		log.info("Now we will change the start and end date of the subscription pool adding one month to enddate and subtracting one month from startdate...");
		Calendar newStartDate = (Calendar) originalStartDate.clone(); newStartDate.add(Calendar.MONTH, -1);
		Calendar newEndDate = (Calendar) originalEndDate.clone(); newEndDate.add(Calendar.MONTH, 1);
		updateSubscriptionPoolDatesOnDatabase(pool,newStartDate,newEndDate);
		
		log.info("Now let's refresh the subscription pools...");
		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,serverAdminUsername,serverAdminPassword, consumerOwner);
		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(serverHostname,serverPort,serverPrefix,serverAdminUsername,serverAdminPassword, jobDetail, "FINISHED", 10*1000, 3);
//		log.info("Now let's update the certFrequency to 1 minutes so that the rhcertd will pull down the new certFiles");
//		clienttasks.restart_rhsmcertd(1, true);
		log.info("Refresh to make sure the latest certs are on the client...");
		clienttasks.refresh(null, null, null); // make sure the new entitlements are downloaded

		log.info("The updated certs should now be on the client...");

		log.info("First, let's assert that subscription pool reflects the new end date...");
		SubscriptionPool newPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", pool.poolId, clienttasks.getCurrentlyAllAvailableSubscriptionPools());
		Assert.assertNotNull(newPool,"Pool id '"+pool.poolId+"' is still available after changing its the start and end dates and refreshing pools.");
		Assert.assertEquals(SubscriptionPool.formatDateString(newPool.endDate), SubscriptionPool.formatDateString(newEndDate),
				"As seen by the client, the enddate of the subscribed to pool '"+pool.poolId+"' has been changed from '"+SubscriptionPool.formatDateString(originalEndDate)+"' to '"+SubscriptionPool.formatDateString(newEndDate)+"'.");

//FIXME UNCOMMENT WHILE DEBUGGING WITH AJAY
//if (true)return;
		log.info("Second, let's assert that the original cert file '"+originalCertFile+"' is gone...");
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, originalCertFile),0,"Original certificate file '"+originalCertFile+"' has been removed.");

		log.info("Third, let's assert that consumed product certs have been updated...");
		String newCertFile = "";
		for (ProductSubscription product : products) {
			ProductSubscription newProduct = ProductSubscription.findFirstInstanceWithMatchingFieldFromList("productName",product.productName,clienttasks.getCurrentlyConsumedProductSubscriptions());
			Assert.assertEquals(ProductSubscription.formatDateString(newProduct.startDate), ProductSubscription.formatDateString(newStartDate),
					"Rhsmcertd has updated the entitled startdate to '"+ProductSubscription.formatDateString(newStartDate)+"' for consumed product: "+newProduct.productName);
			Assert.assertEquals(ProductSubscription.formatDateString(newProduct.endDate), ProductSubscription.formatDateString(newEndDate),
					"Rhsmcertd has updated the entitled enddate to '"+ProductSubscription.formatDateString(newEndDate)+"' for consumed product: "+newProduct.productName);

			log.info("And, let's assert that consumed product cert serial has been updated...");
			Assert.assertTrue(!newProduct.serialNumber.equals(product.serialNumber), 
					"The consumed product cert serial has been updated from '"+product.serialNumber+"' to '"+newProduct.serialNumber+"' for product: "+newProduct.productName);
			newCertFile = clienttasks.entitlementCertDir+"/"+newProduct.serialNumber+".pem";
		}
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, newCertFile),1,"New certificate file '"+newCertFile+"' exists.");

		log.info("Finally, check the CRL list on the server and verify the original entitlement cert serials are revoked...");
		log.info("Waiting 2 minutes...  (Assuming this is the candlepin.conf value set for pinsetter.org.fedoraproject.candlepin.pinsetter.tasks.CertificateRevocationListTask.schedule)");
		sleep(2*60*1000);	// give the CertificateRevocationListTask.schedule 2 minutes to update the list since that is what was set in setupBeforeSuite()
		// NOTE: The refresh schedule should have been set with a call to servertasks.updateConfigFileParameter in the setupBeforeSuite()
		//       Set inside /etc/candlepin/candlepin.conf
		//       pinsetter.org.fedoraproject.candlepin.pinsetter.tasks.CertificateRevocationListTask.schedule=0 0/2 * * * ?
		// NOTE: if not set, the default is  public static final String DEFAULT_SCHEDULE = "0 0 12 * * ?" Fire at 12pm (noon) every day
		for (ProductSubscription product : products) {
			RevokedCert revokedCert = RevokedCert.findFirstInstanceWithMatchingFieldFromList("serialNumber",product.serialNumber,servertasks.getCurrentlyRevokedCerts());
			Assert.assertTrue(revokedCert!=null,"Original entitlement certificate serial number '"+product.serialNumber+"' for product '"+product.productName+"' has been added to the Certificate Revocation List (CRL) as: "+revokedCert);
			Assert.assertEquals(revokedCert.reasonCode, "Privilege Withdrawn","Expanding the certificate start and end dates should revoke the certificated with a reason code of Privilege Withdrawn");
		}
	}
	protected List<String> alreadySubscribedProductIdsInChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test = new ArrayList<String>();

	
	
	
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
			Class.forName(dbSqlDriver);	//	"org.postgresql.Driver" or "oracle.jdbc.driver.OracleDriver"
			
			// Create a connection to the database
			String url = dbSqlDriver.contains("postgres")? 
					"jdbc:postgresql://" + dbHostname + ":" + dbPort + "/" + dbName :
					"jdbc:oracle:thin:@" + dbHostname + ":" + dbPort + ":" + dbName ;
			log.info(String.format("Attempting to connect to database with url and credentials: url=%s username=%s password=%s",url,dbUsername,dbPassword));
			dbConnection1 = DriverManager.getConnection(url, dbUsername, dbPassword);
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
