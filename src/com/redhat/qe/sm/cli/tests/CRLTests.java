package com.redhat.qe.sm.cli.tests;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.RevokedCert;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author jsefler
 *
 */
@Test(groups={"crl"})
public class CRLTests extends SubscriptionManagerTestScript{
	
	
	@Test(	description="subscription-manager-cli: change subscription pool start/end dates and refresh subscription pools",
			groups={"ChangeSubscriptionPoolStartEndDatesAndRefreshSubscriptionPools_Test"},
			dependsOnGroups={},
			dataProvider="getAvailableSubscriptionPoolsData",
			enabled=true)
	@ImplementsTCMS(id="56025")
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
		
		log.info("Subscribe client (already registered as a system under username '"+clientusername+"') to subscription pool "+pool+"...");
		clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);

		log.info("Verify that the currently consumed product subscriptions that came from this subscription pool have the same start and end date as the pool...");
		List<ProductSubscription> products = new ArrayList<ProductSubscription>();
		for (ProductSubscription product : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			if (clienttasks.getSubscriptionPoolFromProductSubscription(product).equals(pool)) {
//FIXME Available Subscriptions	does not display start date			Assert.assertEquals(product.startDate, pool.startDate, "The original start date ("+product.startDate+") for the subscribed product '"+product.productName+"' matches the start date ("+pool.startDate+") of the subscription pool '"+pool.subscriptionName+"' from where it was entitled.");
				Assert.assertTrue(product.endDate.equals(pool.endDate), "The original end date ("+ProductSubscription.formatDateString(product.endDate)+") for the subscribed product '"+product.productName+"' matches the end date ("+SubscriptionPool.formatDateString(pool.endDate)+") of the subscription pool '"+pool.subscriptionName+"' from where it was entitled.");
				products.add(product);
			}
		}
		Calendar originalStartDate = (Calendar) products.get(0).startDate.clone();
		Calendar originalEndDate = (Calendar) products.get(0).endDate.clone();
		String originalCertFile = clienttasks.entitlementCertDir+"/product/"+products.get(0).serialNumber+".pem";
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, originalCertFile),1,"Original certificate file '"+originalCertFile+"' exists.");
		
		log.info("Now we will change the start and end date of the subscription pool adding one month to enddate and subtracting one month from startdate...");
		Calendar newStartDate = (Calendar) originalStartDate.clone(); newStartDate.add(Calendar.MONTH, -1);
		Calendar newEndDate = (Calendar) originalEndDate.clone(); newEndDate.add(Calendar.MONTH, 1);
		updateSubscriptionPoolDatesOnDatabase(pool,newStartDate,newEndDate);
		
		log.info("Now let's refresh the subscription pools...");
		CandlepinTasks.refreshPoolsREST( serverHostname,serverPort,clientOwnerUsername,clientOwnerPassword);
		
		log.info("Now let's update the certFrequency to 1 minutes so that the rhcertd will pull down the new certFiles");
		clienttasks.changeCertFrequency(1, true);

		log.info("The updated certs should now be on the client...");

		log.info("First, let's assert that subscription pool reflects the new end date...");
		List<SubscriptionPool> allSubscriptionPools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		Assert.assertContains(allSubscriptionPools, pool);
		for (SubscriptionPool newPool : allSubscriptionPools) {
			if (newPool.equals(pool)) {
				Assert.assertEquals(SubscriptionPool.formatDateString(newPool.endDate), SubscriptionPool.formatDateString(newEndDate),
						"As seen by the client, the enddate of the subscribed to pool '"+pool.poolId+"' has been changed from '"+SubscriptionPool.formatDateString(originalEndDate)+"' to '"+SubscriptionPool.formatDateString(newEndDate)+"'.");
				break;
			}
		}

		log.info("Second, let's assert that the original cert file '"+originalCertFile+"' is gone...");
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, originalCertFile),0,"Original certificate file '"+originalCertFile+"' has been removed.");

		log.info("Third, let's assert that consumed product certs have been updated...");
		String newCertFile = "";
		for (ProductSubscription product : products) {
			ProductSubscription newProduct = clienttasks.findProductSubscriptionWithMatchingFieldFromList("productName",product.productName,clienttasks.getCurrentlyConsumedProductSubscriptions());
			Assert.assertEquals(ProductSubscription.formatDateString(newProduct.startDate), ProductSubscription.formatDateString(newStartDate),
					"Rhsmcertd has updated the entitled startdate to '"+ProductSubscription.formatDateString(newStartDate)+"' for consumed product: "+newProduct.productName);
			Assert.assertEquals(ProductSubscription.formatDateString(newProduct.endDate), ProductSubscription.formatDateString(newEndDate),
					"Rhsmcertd has updated the entitled enddate to '"+ProductSubscription.formatDateString(newEndDate)+"' for consumed product: "+newProduct.productName);

			log.info("And, let's assert that consumed product cert serial has been updated...");
			Assert.assertTrue(!newProduct.serialNumber.equals(product.serialNumber), 
					"The consumed product cert serial has been updated from '"+product.serialNumber+"' to '"+newProduct.serialNumber+"' for product: "+newProduct.productName);
			newCertFile = clienttasks.entitlementCertDir+"/product/"+newProduct.serialNumber+".pem";
		}
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, newCertFile),1,"New certificate file '"+newCertFile+"' exists.");

		log.info("Finally, check the crl list on the server and verify the original entitlement cert serials are revoked...");
		sleep(1*60*1000);sleep(10000);	// give the CertificateRevocationListTask.schedule another minute to update the list
		// NOTE: The refresh schedule was set with a call to servertasks.updateConfigFileParameter in the setupBeforeSuite()
		for (ProductSubscription product : products) {
			RevokedCert revokedCert = servertasks.findRevokedCertWithMatchingFieldFromList("serialNumber",product.serialNumber,servertasks.getCurrentlyRevokedCerts());
			Assert.assertTrue(revokedCert!=null,"Original entitlement certificate serial number '"+product.serialNumber+"' for product '"+product.productName+"' has been added to the Certificate Revocation List (CRL) as: "+revokedCert);
			Assert.assertEquals(revokedCert.reasonCode, "Privilege Withdrawn","Expanding the certificate start and end dates should revoke the certificated with a reason code of Privilege Withdrawn");
		}
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
	}
	

	
	
	// Data Providers ***********************************************************************

	

}
