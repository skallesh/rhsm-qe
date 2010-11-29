package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.ProductCert;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"certificate"})
public class CertificateTests extends SubscriptionManagerCLITestScript {

	
	
	@Test(	description="candidate product cert validity dates",
			dataProvider="getProductCertFilesData",
			enabled=true)
	@ImplementsNitrateTest(caseId=64656)
	public void VerifyValidityPeriodInProductCerts_Test(File productCertFile) {
		
		ProductCert productCert = clienttasks.getProductCertFromProductCertFile(productCertFile);
		
		log.info("Verifying the validity period in product cert '"+productCertFile+"': "+productCert);
		
		Calendar now = Calendar.getInstance();
		
		// verify that the validity period for this product cert has already begun.
		Assert.assertTrue(productCert.validityNotBefore.before(now),"This validity period for this product cert has already begun.");
	
		// verify that the validity period for this product cert has not yet ended.
		Assert.assertTrue(productCert.validityNotAfter.after(now),"This validity period for this product cert has not yet ended.");

		// verify that the validity period for this product cert is 20 years.
		int actualYears = productCert.validityNotAfter.get(Calendar.YEAR) - productCert.validityNotBefore.get(Calendar.YEAR);
		Assert.assertEquals(actualYears,20,"This validity period for this product spans 20 years.");

	}
	
	
	
	
	
	// Configuration methods ***********************************************************************

	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************
	@DataProvider(name="getProductCertFilesData")
	public Object[][] getProductCertFilesDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getProductCertFilesDataAsListOfLists());
	}
	protected List<List<Object>> getProductCertFilesDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		
		if (clienttasks!=null) {
			for (File productCertFile : clienttasks.getCurrentProductCertFiles()) {
				ll.add(Arrays.asList(new Object[]{productCertFile}));
			}
		}
		
		return ll;
	}

}
