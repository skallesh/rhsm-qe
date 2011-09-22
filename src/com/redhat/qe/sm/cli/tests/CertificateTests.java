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
@Test(groups={"CertificateTests"})
public class CertificateTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="Verify that a base product cert corresponding to the /etc/redhat-release is installed",
			groups={"AcceptanceTests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyBaseRHELProductCertIsInstalled_Test() {
		
		// check each of the installed product certs in search of one that matches the /etc/redhat-release
		log.info("Checking each installed product cert for one that matches /etc/redhat-release: "+clienttasks.redhatRelease);
		for (ProductCert productCert : clienttasks.getCurrentProductCerts()) {
			log.info("Found installed product cert: "+productCert);
			String productCertName = productCert.productName;
			
			// strip out numbers from the product cert name before checking for a match...  e.g name='Red Hat Enterprise Linux 6 Server' => 'Red Hat Enterprise Linux Server'
			// Example redhatRelease: Red Hat Enterprise Linux Server release 6.2 Beta (Santiago)
			// Example product cert: name="Red Hat Enterprise Linux 6 Server" version="6.2 Beta"
			productCertName = productCertName.replaceAll("\\d", "").replaceAll(" {2,}", " ");
			// adjust the product cert name "for Scientific Computing" => "ComputeNode"
			productCertName = productCertName.replaceFirst("for Scientific Computing","ComputeNode");
			// adjust the product cert name "for IBM POWER" => "Server"
			productCertName = productCertName.replaceFirst("for IBM POWER","Server");
			// adjust the product cert name "Desktop" => "Client"
			productCertName = productCertName.replaceFirst("Desktop","Client");

			if (clienttasks.redhatRelease.startsWith(String.format("%s release %s", productCertName, productCert.productNamespace.version))) {
				Assert.assertTrue(true,"Found the following installed product cert that appears to match the /etc/redhat-release ("+clienttasks.redhatRelease+"): "+productCert);
				return;
			}
		}
		Assert.fail("Could NOT find an installed installed product cert that matches /etc/redhat-release ("+clienttasks.redhatRelease+").");
	}
	
	
	@Test(	description="candidate product cert validity dates",
			groups={"AcceptanceTests"},
			dataProvider="getProductCertFilesData",
			enabled=true)
	@ImplementsNitrateTest(caseId=64656)
	public void VerifyValidityPeriodInProductCerts_Test(File productCertFile) {
		
		ProductCert productCert = clienttasks.getProductCertFromProductCertFile(productCertFile);
		long actualValidityDurationDays = (productCert.validityNotAfter.getTimeInMillis() - productCert.validityNotBefore.getTimeInMillis())/(24*60*60*1000);
		
		log.info("Verifying the validity period in product cert '"+productCertFile+"': "+productCert);
		log.info("The validity period for this product cert is (days): "+actualValidityDurationDays);
		Calendar now = Calendar.getInstance();
		
		// verify that the validity period for this product cert has already begun.
		Assert.assertTrue(productCert.validityNotBefore.before(now),"This validity period for this product cert has already begun.");
	
		// verify that the validity period for this product cert has not yet ended.
		Assert.assertTrue(productCert.validityNotAfter.after(now),"This validity period for this product cert has not yet ended.");

		// verify that the validity period for this product cert is among the expected values.
		List<Long>expectedValidityDurationDaysList=new ArrayList<Long>();
		String productCertValidityDuration = sm_productCertValidityDuration;
		for (String expectedValidityDurationDayOption :  Arrays.asList(productCertValidityDuration.trim().split(" *, *"))) {
			expectedValidityDurationDaysList.add(Long.valueOf(expectedValidityDurationDayOption));
		}
		log.info("Asserting that the the validity period for this product certificate ("+actualValidityDurationDays+") spans one of the expected number of days ("+productCertValidityDuration+")...");
		Assert.assertContains(expectedValidityDurationDaysList, new Long(actualValidityDurationDays));
	}
	
	
	
	// Candidates for an automated Test:
	// TODO https://bugzilla.redhat.com/show_bug.cgi?id=659735
	// TODO https://bugzilla.redhat.com/show_bug.cgi?id=706518
	
	// Configuration methods ***********************************************************************

	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************
	@DataProvider(name="getProductCertFilesData")
	public Object[][] getProductCertFilesDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getProductCertFilesDataAsListOfLists());
	}
	protected List<List<Object>> getProductCertFilesDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		
		if (clienttasks!=null) {
			List<File> productCertFiles = clienttasks.getCurrentProductCertFiles();
			if (productCertFiles.size()==0) log.info("No Product Cert files were found.");
			for (File productCertFile : productCertFiles) {
				ll.add(Arrays.asList(new Object[]{productCertFile}));
			}
		}
		
		return ll;
	}

}
