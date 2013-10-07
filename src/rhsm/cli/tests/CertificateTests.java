package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;
import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.CertStatistics;
import rhsm.data.ConsumerCert;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.OrderNamespace;
import rhsm.data.ProductCert;
import rhsm.data.ProductNamespace;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *	back door to ALWAYS get certv3 entitlements, set fact system.testing: true
 *  http://wiki.samat.org/CheatSheet/OpenSSL
 *  
 *  <jbowes> yeah. DER size is how many bytes the cert takes up 'on the wire' in communication
 *  with the CDN subject key ID size is the size of the subject key id field, which is hacked
 *  in hosted to hold a zipped representation of the content sets for certv1. that's the field
 *  that the CDN sends from the edge server to the java app thingy that does auth so if DER size
 *  is over 120kb or so you have a problem. if subject key id size is over 28kb or so you have a problem.
 */
@Test(groups={"CertificateTests"})
public class CertificateTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="Verify that a base product cert corresponding to the /etc/redhat-release is installed",
			groups={"AcceptanceTests","blockedByBug-706518","blockedByBug-844368"/*,"blockedByBug-904193" Uncomment when we get to RHEL7*/},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyBaseRHELProductCertIsInstalled_Test() {

		// list the currently installed products
		clienttasks.listInstalledProducts();
		
		// list the currently installed product certs
		log.info("All installed product certs: ");
		List <ProductCert> productCerts = clienttasks.getCurrentProductCerts();
		for (ProductCert productCert : productCerts) {
			log.info(productCert.toString());
		}
		
		// assert that a product cert is installed that matches our base RHEL release version
		ProductCert rhelProductCert = null;
		if (clienttasks.releasever.equals("5Server") || clienttasks.releasever.equals("6Server")) {
			if (clienttasks.arch.startsWith("ppc")) {				// ppc ppc64
				rhelProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", "74", productCerts);	// Red Hat Enterprise Linux for IBM POWER
			}
			else if (clienttasks.arch.startsWith("s390")) {			// s390 s390x
				rhelProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", "72", productCerts);	// Red Hat Enterprise Linux for IBM System z
			}
			else { 													// i386 i686 ia64 x86_64
				rhelProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", "69", productCerts);	// Red Hat Enterprise Linux Server
			}
		}
		else if (clienttasks.releasever.equals("5Client")) {		// i386 i686 x86_64
				if (rhelProductCert==null) rhelProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", "68", productCerts);	// Red Hat Enterprise Linux Desktop
				if (rhelProductCert==null) rhelProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", "71", productCerts);	// Red Hat Enterprise Linux Workstation
		}
		else if (clienttasks.releasever.equals("6Client")) {		// i386 i686 x86_64
				rhelProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", "68", productCerts);	// Red Hat Enterprise Linux Desktop
		}
		else if (clienttasks.releasever.equals("6Workstation")) {	// i386 i686 x86_64
				rhelProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", "71", productCerts);	// Red Hat Enterprise Linux Workstation
		}
		else if (clienttasks.releasever.equals("6ComputeNode")) {	// i386 i686 x86_64
				rhelProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", "76", productCerts);	// Red Hat Enterprise Linux for Scientific Computing
		}
		
		Assert.assertNotNull(rhelProductCert,"Found an installed product cert that matches the system's base RHEL release version '"+clienttasks.releasever+"' on arch '"+clienttasks.arch+"':");
		log.info(rhelProductCert.toString());
	}
	
	
	@Test(	description="Verify that no more than one RHEL product cert is ever installed.",
			groups={"AcceptanceTests","blockedByBug-854879"/*,"blockedByBug-904193" Uncomment when we get to RHEL7*/},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyOnlyOneBaseRHELProductCertIsInstalled_Test() {
		
		//  find all installed RHEL product Certs
		List<ProductCert> rhelProductCertsInstalled = new ArrayList<ProductCert>();
		for (ProductCert productCert : clienttasks.getCurrentProductCerts()) {
			if (Arrays.asList("68","69","71","72","74","76").contains(productCert.productId)) { // 70,73,75 are "Extended Update Support"
				rhelProductCertsInstalled.add(productCert);
			}
		}
		if (rhelProductCertsInstalled.size()>1) {
			log.warning("Found multiple installed RHEL product certs:");
			for (ProductCert productCert : rhelProductCertsInstalled) {
				log.warning(productCert.toString());
			}
		}
		Assert.assertTrue(rhelProductCertsInstalled.size()==1,"At most only one RHEL product cert should ever be installed.");
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
	
	
	@BeforeGroups(groups={"setup"}, value={"VerifyEntitlementCertContainsExpectedOIDs_Test"})
	public void createFactsFileWithOverridingValues() {
		Map<String,String> factsMap = new HashMap<String,String>();
		factsMap.put("system.certificate_version", "1.0");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
	}
	@Test(	description="Make sure the entitlement cert contains all expected OIDs",
			groups={"VerifyEntitlementCertContainsExpectedOIDs_Test","AcceptanceTests","blockedByBug-744259","blockedByBug-754426","blockedByBug-962520","blockedByBug-997970"},
			dataProvider="getAllAvailableSubscriptionPoolsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyEntitlementCertContainsExpectedOIDs_Test(SubscriptionPool pool) throws JSONException, Exception {
		
		// skip RAM-based subscriptions since they were are not supported on v1 certificates
		if (CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "ram")!=null) {
			throw new SkipException("This test is not designed for RAM-based product subscriptions.");
		}
		// skip CORES-based subscriptions since they were are not supported on v1 certificates
		if (CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword, sm_serverUrl, pool.poolId, "cores")!=null) {
			throw new SkipException("This test is not designed for CORES-based product subscriptions.");
		}
		
		// subscribe to the pool and get the EntitlementCert
		//File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool);
		//EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		//^ replaced with the following to save logging/assertion time
		clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertCorrespondingToSubscribedPool(pool);
		Assert.assertNotNull(entitlementCert,"Successfully retrieved the entitlement cert granted after subscribing to pool: "+pool);
		// re-scan the entitlement cert using openssl because it better distinguishes between a "" OID value and a null OID value.
		entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFileUsingOpensslX509(entitlementCert.file);
		
		// Commented out the following log of rawCertificate to conserve logFile size
		//log.info("Raw entitlement certificate: \n"+entitlementCert.rawCertificate);
		boolean allMandatoryOIDsFound = true;
		// Reference	https://docspace.corp.redhat.com/docs/DOC-30244
		
		// asserting all expected OIDS in the OrderNamespace
		//		  1.3.6.1.4.1.2312.9.4.1 (Name): Red Hat Enterprise Linux Server
		//		  1.3.6.1.4.1.2312.9.4.2 (Order Number) : ff8080812c3a2ba8012c3a2cbe63005b  
		//		  1.3.6.1.4.1.2312.9.4.3 (SKU) : MCT0982
		//		  1.3.6.1.4.1.2312.9.4.4 (Subscription Number) : abcd-ef12-1234-5678   <- SHOULD ONLY EXIST IF ORIGINATED FROM A REGTOKEN
		//		  1.3.6.1.4.1.2312.9.4.5 (Quantity) : 100
		//		  1.3.6.1.4.1.2312.9.4.6 (Entitlement Start Date) : 2010-10-25T04:00:00Z
		//		  1.3.6.1.4.1.2312.9.4.7 (Entitlement End Date) : 2011-11-05T00:00:00Z
		//		  1.3.6.1.4.1.2312.9.4.8 (Virtualization Limit) : 4						<- ONLY EXISTS WHEN VIRT POOLS ARE TO BE GENERATED
		//		  1.3.6.1.4.1.2312.9.4.9 (Socket Limit) : None							<- ONLY EXISTS WHEN THE SUBSCRIPTION IS USED TO SATISFY HARDWARE
		//		  1.3.6.1.4.1.2312.9.4.10 (Contract Number): 152341643
		//		  1.3.6.1.4.1.2312.9.4.11 (Quantity Used): 4
		//		  1.3.6.1.4.1.2312.9.4.12 (Warning Period): 30
		//		  1.3.6.1.4.1.2312.9.4.13 (Account Number): 9876543210
		//		  1.3.6.1.4.1.2312.9.4.14 (Provides Management): 0 (boolean, 1 for true)<- NEEDINFO
		//		  1.3.6.1.4.1.2312.9.4.15 (Support Level): Premium						<- NEEDINFO
		//		  1.3.6.1.4.1.2312.9.4.16 (Support Type): Level 3						<- NEEDINFO
		//		  1.3.6.1.4.1.2312.9.4.17 (Stacking Id): 23456							<- ONLY EXISTS WHEN SUBSCRIPTION IS STACKABLE
		//		  1.3.6.1.4.1.2312.9.4.18 (Virt Only): 1								<- ONLY EXISTS WHEN POOLS ARE INTENTED FOR VIRT MACHINES ONLY
		OrderNamespace orderNamespace = entitlementCert.orderNamespace;
		if (orderNamespace.productName!=null)			{Assert.assertNotNull(orderNamespace.productName,			"Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.1 (Name) is present with value '"+ orderNamespace.productName+"'");}							else {log.warning("Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.1 (Name) is missing"); allMandatoryOIDsFound = false;}
		if (orderNamespace.orderNumber!=null)			{Assert.assertNotNull(orderNamespace.orderNumber,			"Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.2 (Order Number) is present with value '"+ orderNamespace.orderNumber+"'");}					else {log.warning("Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.2 (Order Number) is missing"); allMandatoryOIDsFound = false;}
		if (orderNamespace.productId!=null)				{Assert.assertNotNull(orderNamespace.productId,				"Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.3 (SKU) is present with value '"+ orderNamespace.productId+"'");}							else {log.warning("Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.3 (SKU) is missing"); allMandatoryOIDsFound = false;}
		if (orderNamespace.subscriptionNumber!=null)	{Assert.assertNotNull(orderNamespace.subscriptionNumber,	"Optional OrderNamespace OID 1.3.6.1.4.1.2312.9.4.4 (Subscription Number) is present with value '"+ orderNamespace.subscriptionNumber+"'");}	else {log.warning("Optional OrderNamespace OID 1.3.6.1.4.1.2312.9.4.4 (Subscription Number) is missing"); /*allOIDSFound = false;*/}
		if (orderNamespace.quantity!=null)				{Assert.assertNotNull(orderNamespace.quantity,				"Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.5 (Quantity) is present with value '"+ orderNamespace.quantity+"'");}						else {log.warning("Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.5 (Quantity) is missing"); allMandatoryOIDsFound = false;}
		if (orderNamespace.startDate!=null)				{Assert.assertNotNull(orderNamespace.startDate,				"Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.6 (Entitlement Start Date) is present with value '"+ orderNamespace.startDate+"'");}			else {log.warning("Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.6 (Entitlement Start Date) is missing"); allMandatoryOIDsFound = false;}
		if (orderNamespace.endDate!=null)				{Assert.assertNotNull(orderNamespace.endDate,				"Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.7 (Entitlement End Date) is present with value '"+ orderNamespace.endDate+"'");}				else {log.warning("Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.7 (Entitlement End Date) is missing"); allMandatoryOIDsFound = false;}
		if (orderNamespace.virtualizationLimit!=null)	{Assert.assertNotNull(orderNamespace.virtualizationLimit,	"Optional OrderNamespace OID 1.3.6.1.4.1.2312.9.4.8 (Virtualization Limit) is present with value '"+ orderNamespace.virtualizationLimit+"'");}	else {log.warning("Optional OrderNamespace OID 1.3.6.1.4.1.2312.9.4.8 (Virtualization Limit) is missing"); /*allOIDSFound = false;*/}
		if (orderNamespace.socketLimit!=null)			{Assert.assertNotNull(orderNamespace.socketLimit,			"Optional OrderNamespace OID 1.3.6.1.4.1.2312.9.4.9 (Socket Limit) is present with value '"+ orderNamespace.socketLimit+"'");}					else {log.warning("Optional OrderNamespace OID 1.3.6.1.4.1.2312.9.4.9 (Socket Limit) is missing"); /*allOIDSFound = false;*/}
		if (orderNamespace.contractNumber!=null)		{Assert.assertNotNull(orderNamespace.contractNumber,		"Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.10 (Contract Number) is present with value '"+ orderNamespace.contractNumber+"'");}			else {log.warning("Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.10 (Contract Number) is missing"); allMandatoryOIDsFound = false;}
		if (orderNamespace.quantityUsed!=null)			{Assert.assertNotNull(orderNamespace.quantityUsed,			"Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.11 (Quantity Used) is present with value '"+ orderNamespace.quantityUsed+"'");}				else {log.warning("Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.11 (Quantity Used) is missing"); allMandatoryOIDsFound = false;}
		if (orderNamespace.warningPeriod!=null)			{Assert.assertNotNull(orderNamespace.warningPeriod,			"Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.12 (Warning Period) is present with value '"+ orderNamespace.warningPeriod+"'");}			else {log.warning("Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.12 (Warning Period) is missing"); allMandatoryOIDsFound = false;}
		if (orderNamespace.accountNumber!=null)			{Assert.assertNotNull(orderNamespace.accountNumber,			"Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.13 (Account Number) is present with value '"+ orderNamespace.accountNumber+"'");}			else {log.warning("Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.13 (Account Number) is missing"); allMandatoryOIDsFound = false;}
		if (orderNamespace.providesManagement!=null)	{Assert.assertNotNull(orderNamespace.providesManagement,	"Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.14 (Provides Management) is present with value '"+ orderNamespace.providesManagement+"'");}	else {log.warning("Mandatory OrderNamespace OID 1.3.6.1.4.1.2312.9.4.14 (Provides Management) is missing"); allMandatoryOIDsFound = false;}
		if (orderNamespace.supportLevel!=null)			{Assert.assertNotNull(orderNamespace.supportLevel,			"Optional OrderNamespace OID 1.3.6.1.4.1.2312.9.4.15 (Support Level) is present with value '"+ orderNamespace.supportLevel+"'");}				else {log.warning("Optional OrderNamespace OID 1.3.6.1.4.1.2312.9.4.15 (Support Level) is missing"); /*allOIDSFound = false;*/}
		if (orderNamespace.supportType!=null)			{Assert.assertNotNull(orderNamespace.supportType,			"Optional OrderNamespace OID 1.3.6.1.4.1.2312.9.4.16 (Support Type) is present with value '"+ orderNamespace.supportType+"'");}					else {log.warning("Optional OrderNamespace OID 1.3.6.1.4.1.2312.9.4.16 (Support Type) is missing"); /*allOIDSFound = false;*/}
		if (orderNamespace.stackingId!=null)			{Assert.assertNotNull(orderNamespace.stackingId,			"Optional OrderNamespace OID 1.3.6.1.4.1.2312.9.4.17 (Stacking Id) is present with value '"+ orderNamespace.stackingId+"'");}					else {log.warning("Optional OrderNamespace OID 1.3.6.1.4.1.2312.9.4.17 (Stacking Id) is missing"); /*allOIDSFound = false;*/}
		if (orderNamespace.virtOnly!=null)				{Assert.assertNotNull(orderNamespace.virtOnly,				"Optional OrderNamespace OID 1.3.6.1.4.1.2312.9.4.18 (Virt Only) is present with value '"+ orderNamespace.virtOnly+"'");}						else {log.warning("Optional OrderNamespace OID 1.3.6.1.4.1.2312.9.4.18 (Virt Only) is missing"); /*allOIDSFound = false;*/}
		
		for (ProductNamespace productNamespace : entitlementCert.productNamespaces) {
			// asserting all expected OIDS in the ProductNamespace
			//    1.3.6.1.4.1.2312.9.1.<product_hash>.1 (Name) : Red Hat Enterprise Linux
			//    1.3.6.1.4.1.2312.9.1.<product_hash>.2 (Version) : 6.0
			//    1.3.6.1.4.1.2312.9.1.<product_hash>.3 (Architecture) : x86_64
			//    1.3.6.1.4.1.2312.9.1.<product_hash>.4 (Provides) : String1, String2, String3
			if (productNamespace.name!=null)			{Assert.assertNotNull(productNamespace.name,			"Mandatory ProductNamespace OID 1.3.6.1.4.1.2312.9.1."+productNamespace.id+".1 (Name) is present with value '"+ productNamespace.name+"'");}			else {log.warning("Mandatory ProductNamespace OID 1.3.6.1.4.1.2312.9.1."+productNamespace.id+".1 (Name) is missing"); allMandatoryOIDsFound = false;}
			//if (productNamespace.version!=null)			{Assert.assertNotNull(productNamespace.version,			"Mandatory ProductNamespace OID 1.3.6.1.4.1.2312.9.1."+productNamespace.id+".2 (Version) is present with value '"+ productNamespace.version+"'");}		else {log.warning("Mandatory ProductNamespace OID 1.3.6.1.4.1.2312.9.1."+productNamespace.id+".2 (Version) is missing"); allMandatoryOIDsFound = false;}
			if (productNamespace.version!=null)			{Assert.assertNotNull(productNamespace.version,			"Optional ProductNamespace OID 1.3.6.1.4.1.2312.9.1."+productNamespace.id+".2 (Version) is present with value '"+ productNamespace.version+"'");}		else {log.warning("Optional ProductNamespace OID 1.3.6.1.4.1.2312.9.1."+productNamespace.id+".2 (Version) is missing"); /*allMandatoryOIDsFound = false;*/}
			if (!sm_serverType.equals(CandlepinType.standalone))	// the TESTDATA imported into a standalone candlepin does not honor the assertion that the product version OID must be blank since rel-eng creates product certs with valid version outside of candlepin
			if (productNamespace.version!=null)			{Assert.assertEquals(productNamespace.version, "",		"Optional ProductNamespace OID 1.3.6.1.4.1.2312.9.1."+productNamespace.id+".2 (Version) can be present but must always be blank.");}
			if (productNamespace.arch!=null)			{Assert.assertNotNull(productNamespace.arch,			"Mandatory ProductNamespace OID 1.3.6.1.4.1.2312.9.1."+productNamespace.id+".3 (Architecture) is present with value '"+ productNamespace.arch+"'");}	else {log.warning("Mandatory ProductNamespace OID 1.3.6.1.4.1.2312.9.1."+productNamespace.id+".3 (Architecture) is missing"); allMandatoryOIDsFound = false;}
			if (productNamespace.providedTags!=null)	{Assert.assertNotNull(productNamespace.providedTags,	"Optional ProductNamespace OID 1.3.6.1.4.1.2312.9.1."+productNamespace.id+".4 (Provides) is present with value '"+ productNamespace.providedTags+"'");}	else {log.warning("Optional ProductNamespace OID 1.3.6.1.4.1.2312.9.1."+productNamespace.id+".4 (Provides) is missing"); /*allOIDSFound = false;*/}
			if (productNamespace.providedTags!=null)	{Assert.assertEquals(productNamespace.providedTags, "",	"Optional ProductNamespace OID 1.3.6.1.4.1.2312.9.1."+productNamespace.id+".4 (Provides) can be present but must always be blank.");}
		}
		
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;

			// asserting all expected OIDS in the ContentNamespace	
			//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.1 (Name) : Red Hat Enterprise Linux (Supplementary)
			//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.2 (Label) : rhel-server-6-supplementary
			//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.3 (Physical Entitlements): 1
			//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.4 (Flex Guest Entitlements): 0
			//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.5 (Vendor ID): %Red_Hat_Id% or %Red_Hat_Label%
			//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.6 (Download URL): content/rhel-server-6-supplementary/$releasever/$basearch
			//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.7 (GPG Key URL): file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release
			//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.8 (Enabled): 1
			//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.9 (Metadata Expire Seconds): 604800
			//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.10 (Required Tags): TAG1,TAG2,TAG3
			if (contentNamespace.name!=null)					{Assert.assertNotNull(contentNamespace.name,					"Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.1 (Name) is present with value '"+ contentNamespace.name+"'");}									else {log.warning("Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.1 (Name) is missing"); allMandatoryOIDsFound = false;}
			if (contentNamespace.label!=null)					{Assert.assertNotNull(contentNamespace.label,					"Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.2 (Label) is present with value '"+ contentNamespace.label+"'");}									else {log.warning("Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.2 (Label) is missing"); allMandatoryOIDsFound = false;}
			//REMOVED						//if (contentNamespace.physicalEntitlement!=null)		{Assert.assertNotNull(contentNamespace.physicalEntitlement,		"Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.3 (Physical Entitlements) is present with value '"+ contentNamespace.physicalEntitlement+"'");}	else {log.warning("Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.3 (Physical Entitlements) is missing"); allOIDSFound = false;}
			Assert.assertNull(contentNamespace.physicalEntitlement,		"ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.3 (Physical Entitlements) has been removed from the generation of entitlements.  Its current value is '"+ contentNamespace.physicalEntitlement+"'");
			//REMOVED blockedByBug-754426	//if (contentNamespace.flexGuestEntitlement!=null)	{Assert.assertNotNull(contentNamespace.flexGuestEntitlement,	"Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.4 (Flex Guest Entitlements) is present with value '"+ contentNamespace.flexGuestEntitlement+"'");}	else {log.warning("Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.4 (Flex Guest Entitlements) is missing"); allOIDSFound = false;}
			Assert.assertNull(contentNamespace.flexGuestEntitlement,	"ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.4 (Flex Guest Entitlements) has been removed from the generation of entitlements.  Its current value is '"+ contentNamespace.flexGuestEntitlement+"'");
			if (contentNamespace.vendorId!=null)				{Assert.assertNotNull(contentNamespace.vendorId,				"Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.5 (Vendor ID) is present with value '"+ contentNamespace.vendorId+"'");}							else {log.warning("Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.5 (Vendor ID) is missing"); allMandatoryOIDsFound = false;}
			if (contentNamespace.downloadUrl!=null)				{Assert.assertNotNull(contentNamespace.downloadUrl,				"Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.6 (Download URL) is present with value '"+ contentNamespace.downloadUrl+"'");}						else {log.warning("Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.6 (Download URL) is missing"); allMandatoryOIDsFound = false;}
			//if (contentNamespace.gpgKeyUrl!=null)				{Assert.assertNotNull(contentNamespace.gpgKeyUrl,				"Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.7 (GPG Key URL) is present with value '"+ contentNamespace.gpgKeyUrl+"'");}						else {log.warning("Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.7 (GPG Key URL) is missing"); allMandatoryOIDsFound = false;}
			//if (contentNamespace.gpgKeyUrl!=null)				{Assert.assertNotNull(contentNamespace.gpgKeyUrl,				"Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.7 (GPG Key URL) is present with value '"+ contentNamespace.gpgKeyUrl+"'");}						else {if ("content-emptygpg".equals(contentNamespace.name)||"content-nogpg".equals(contentNamespace.name)) {log.info("Skipping assertion of Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.7 (GPG Key URL) for negative test content '"+contentNamespace.name+"'");} else {log.warning("Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.7 (GPG Key URL) is missing"); allMandatoryOIDsFound = false;}}
			if (contentNamespace.gpgKeyUrl!=null)				{Assert.assertNotNull(contentNamespace.gpgKeyUrl,				"Optional ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.7 (GPG Key URL) is present with value '"+ contentNamespace.gpgKeyUrl+"'");}							else {log.warning("Optional ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.7 (GPG Key URL) is missing"); /*allMandatoryOIDsFound = false;*/}
			if (contentNamespace.enabled!=null)					{Assert.assertNotNull(contentNamespace.enabled,					"Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.8 (Enabled) is present with value '"+ contentNamespace.enabled+"'");}								else {log.warning("Mandatory ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.8 (Enabled) is missing"); allMandatoryOIDsFound = false;}
			if (contentNamespace.metadataExpire!=null)			{Assert.assertNotNull(contentNamespace.metadataExpire,			"Optional ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.9 (Metadata Expire Seconds) is present with value '"+ contentNamespace.metadataExpire+"'");}		else {log.warning("Optional ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.9 (Metadata Expire Seconds) is missing"); /*allMandatoryOIDsFound = false;*/}
			if (contentNamespace.requiredTags!=null)			{Assert.assertNotNull(contentNamespace.requiredTags,			"Optional ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.10 (Required Tags) is present with value '"+ contentNamespace.requiredTags+"'");}					else {log.warning("Optional ContentNamespace OID 1.3.6.1.4.1.2312.9.2."+contentNamespace.hash+".1.10 (Required Tags) is missing"); /*allOIDSFound = false;*/}
		}
		
		if (!allMandatoryOIDsFound) Assert.fail("Could not find all mandatory entitlement cert OIDs. (see warnings above)");
	}
	@AfterGroups(groups={"setup"}, value={"VerifyEntitlementCertContainsExpectedOIDs_Test"})
	@AfterClass(groups={"setup"})	// insurance; not really needed
	public void deleteFactsFileWithOverridingValues() {
		clienttasks.deleteFactsFileWithOverridingValues();
	}
	
	
	@Test(	description="assert that the rct cat-cert tool reports the currently installed product certs are Certificate: Version: 1.0 (Note: this is not the ProductNamespace.version)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyProductCertsAreV1Certificates_Test() {

		/* installed product certs only
		List <ProductCert> productCerts = clienttasks.getCurrentProductCerts();
		if (productCerts.isEmpty()) throw new SkipException("No testable product certs are currently installed."); 
		for (ProductCert productCert : productCerts) {
			Assert.assertEquals(productCert.version, "1.0", "The rct cat-cert tool reports this product cert to be a V1 Certificate: "+productCert);
		}
		*/
		
		/* installed product certs and migration product certs */
		List<List<Object>> productCertFilesData = getProductCertFilesDataAsListOfLists();
		if (productCertFilesData.isEmpty()) throw new SkipException("No testable product certs are available for this test."); 
		for (List<Object> productCertFilesDatum : productCertFilesData) {
			File productCertFile = (File) productCertFilesDatum.get(0);
			ProductCert productCert = clienttasks.getProductCertFromProductCertFile(productCertFile);
			Assert.assertEquals(productCert.version, "1.0", "The rct cat-cert tool reports this product cert to be a V1 Certificate: "+productCert);
		}
	}
	
	
	@Test(	description="assert that the rct cat-cert tool reports the current consumer cert is a Certificate: Version: 1.0",
			groups={"blockedByBug-863961"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyConsumerCertsAreV1Certificates_Test() {
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null);
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		Assert.assertEquals(consumerCert.version, "1.0", "The rct cat-cert tool reports this consumer cert to be a V1 Certificate: "+consumerCert);
	}
	
	
	@Test(	description="assert that the rct cat-cert tool reports the issuer of consumer/entitlement/product certificates",
			groups={ "AcceptanceTests","blockedByBug-968364"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyIssuerOfConsumerProductAndEntitlementCerts_Test() {
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		
		// assert the issuer of the consumer cert
		Assert.assertNotNull(consumerCert.issuer, "The rct cat-cert tool reports the issuer of the current consumer cert: "+consumerCert);
		if (sm_serverType.equals(CandlepinType.hosted)) Assert.assertEquals(consumerCert.issuer, "Red Hat Candlepin Authority", "Issuer of the current consumer cert: "+consumerCert.file);
		else if (sm_serverType.equals(CandlepinType.standalone)) Assert.assertEquals(consumerCert.issuer, sm_serverHostname, "Issuer of the current consumer cert: "+consumerCert.file);
		else log.warning("Do not know what value to assert for issuer of the current consumer cert from a candlepin type '"+sm_serverType+"'.");
		consumerCert=null;
		
		// assert the issuer of a redhat product cert
		for (ProductCert productCert : clienttasks.getCurrentProductCerts()) {
			Assert.assertNotNull(productCert.issuer, "The rct cat-cert tool reports the issuer of the installed product cert: "+productCert);
			if (!productCert.file.getPath().endsWith("_.pem")) Assert.assertEquals(productCert.issuer, "Red Hat Entitlement Product Authority", "Issuer of the current installed product cert: "+productCert.file);
		}
		
		// assert the issuer of an entitlement cert
		List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
		if (entitlementCerts.isEmpty()) {
			List<SubscriptionPool> pools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
			if (pools.isEmpty()) {
				log.warning("Cound not find any available pool.");
				Assert.fail("Expected at least one available pool.  Maybe all subscriptions available to '"+sm_clientUsername+"' are being utilized.");
			}
			SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size()));	// randomly pick a pool
			clienttasks.subscribeToSubscriptionPool(pool);
			entitlementCerts = clienttasks.getCurrentEntitlementCerts();
		}
		for (EntitlementCert entitlementCert : entitlementCerts) {
			Assert.assertNotNull(entitlementCert.issuer, "The rct cat-cert tool reports the issuer of granted entitlement cert: "+entitlementCert);
			if (sm_serverType.equals(CandlepinType.hosted)) Assert.assertEquals(entitlementCert.issuer, "Red Hat Candlepin Authority", "Issuer of granted entitlement cert: "+entitlementCert.file);
			else if (sm_serverType.equals(CandlepinType.standalone)) Assert.assertEquals(entitlementCert.issuer, sm_serverHostname, "Issuer of granted entitlement cert: "+entitlementCert.file);
			else log.warning("Do not know what value to assert for issuer of an entitlement cert from a candlepin type '"+sm_serverType+"'.");
		}

	}
	
	
	@Test(	description="assert that the rct cat-cert tool reports orders as Unlimited instead of -1",
			groups={"AcceptanceTests","blockedByBug-1011961"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyRctCatCertReportsOrdersWithQuantityUnlimited_Test() {
		int numberOfUnlimitedPools = 0;
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null);
		for (SubscriptionPool pool : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
			if (pool.quantity.equalsIgnoreCase("Unlimited")) {
				numberOfUnlimitedPools++;
				File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
				EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
				Assert.assertEquals(entitlementCert.orderNamespace.quantity, pool.quantity, "The Order:Quantity from the granted entitlement should match the Quantity from the available Subscription Pool: "+pool);
			} else if (pool.quantity.equals("-1")) {
				Assert.fail("Available SubscriptionPools should NOT report a quantity of '"+pool.quantity+"': "+pool);
			}
		}
		if (numberOfUnlimitedPools==0) throw new SkipException("Could not find any available pools with an unlimited quantity for this test.");
	}
	
	
	@Test(	description="assert the statistic values reported by the rct stat-cert tool for the current consumer cert",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AssertConsumerCertStatistics_Test() {
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, false, null, null, null, null);
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		Assert.assertEquals(consumerCert.version, "1.0", "The rct cat-cert tool reports this consumer cert to be a V1 Certificate: "+consumerCert);
		CertStatistics certStatistics = clienttasks.getCertStatisticsFromCertFile(consumerCert.file);
		
		//	[root@jsefler-6 ~]# rct stat-cert /etc/pki/consumer/cert.pem 
		//	Type: Identity Certificate
		//	Version: 1.0
		//	DER size: 925b
		//	Subject Key ID size: 20b

		String expectedDerSize = client.runCommandAndWait("openssl x509 -in "+consumerCert.file.getPath()+" -outform der -out /tmp/cert.der && du -b /tmp/cert.der | cut -f 1").getStdout().trim();
		expectedDerSize+="b";
		
		Assert.assertEquals(certStatistics.type, "Identity Certificate","rct stat-cert reports this Type.");
		Assert.assertEquals(certStatistics.version, consumerCert.version,"rct stat-cert reports this Version.");
		Assert.assertEquals(certStatistics.derSize, expectedDerSize, "rct stat-cert reports this DER size.");
		Assert.assertNotNull(certStatistics.subjectKeyIdSize, "rct stat-cert reports this Subject Key ID size.");	// TODO assert something better than not null
		Assert.assertNull(certStatistics.contentSets, "rct stat-cert does NOT report a number of Content sets.");
	}
	
	
	@Test(	description="assert the statistic values reported by the rct stat-cert tool for currently installed product certs",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AssertProductCertStatistics_Test() {
		
		// get all the product certs on the system
		List<ProductCert> productCerts = new ArrayList();
		List<ProductCert> installedProductCerts = clienttasks.getCurrentProductCerts();
		List<ProductCert> migrationProductCerts = clienttasks.getProductCerts("/usr/share/rhsm/product/RHEL-"+clienttasks.redhatReleaseX);
		productCerts.addAll(installedProductCerts);
		productCerts.addAll(migrationProductCerts);
		
		// loop through all of the current product certs
		if (productCerts.isEmpty()) throw new SkipException("There are currently no installed product certs to assert statistics.");
		for (ProductCert productCert : productCerts) {
			CertStatistics certStatistics = clienttasks.getCertStatisticsFromCertFile(productCert.file);
			if (productCert.file.toString().endsWith("_.pem")) continue; 	// skip the generated TESTDATA productCerts
			
			//	[root@jsefler-6 ~]# rct stat-cert /etc/pki/product/69.pem 
			//	Type: Product Certificate
			//	Version: 1.0
			//	DER size: 1553b
			
			String expectedDerSize = client.runCommandAndWait("openssl x509 -in "+productCert.file.getPath()+" -outform der -out /tmp/cert.der && du -b /tmp/cert.der | cut -f 1").getStdout().trim();
			expectedDerSize+="b";
			
			Assert.assertEquals(certStatistics.type, "Product Certificate","rct stat-cert reports this Type.");
			Assert.assertEquals(certStatistics.version, productCert.version,"rct stat-cert reports this Version.");
			Assert.assertEquals(certStatistics.derSize, expectedDerSize, "rct stat-cert reports this DER size.");
			Assert.assertNull(certStatistics.subjectKeyIdSize, "rct stat-cert does NOT report a Subject Key ID size.");
			Assert.assertNull(certStatistics.contentSets, "rct stat-cert does NOT report a number of Content sets.");
		}
	}
	
	
	@Test(	description="assert the statistic values reported by the rct stat-cert tool for currently subscribed entitlements",
			groups={"AcceptanceTests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AssertEntitlementCertStatistics_Test() {
		
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, false, null, null, null, null);

		// get some entitlements!
		clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();
		List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
		
		// loop through all of the current entitlement certs
		if (entitlementCerts.isEmpty()) throw new SkipException("There are currently no entitlement certs to assert statistics.");
		for (EntitlementCert entitlementCert : entitlementCerts) {
			CertStatistics certStatistics = clienttasks.getCertStatisticsFromCertFile(entitlementCert.file);
			
			//	[root@jsefler-6 ~]# rct stat-cert /etc/pki/entitlement/5254857399115244164.pem 
			//	Type: Entitlement Certificate
			//	Version: 3.0
			//	DER size: 947b
			//	Subject Key ID size: 20b
			//	Content sets: 3

			String expectedDerSize = client.runCommandAndWait("openssl x509 -in "+entitlementCert.file.getPath()+" -outform der -out /tmp/cert.der && du -b /tmp/cert.der | cut -f 1").getStdout().trim();
			expectedDerSize+="b";
			
			Assert.assertEquals(certStatistics.type, "Entitlement Certificate","rct stat-cert reports this Type.");
			Assert.assertEquals(certStatistics.version, entitlementCert.version,"rct stat-cert reports this Version.");
			Assert.assertEquals(certStatistics.derSize, expectedDerSize, "rct stat-cert reports this DER size.");
			Assert.assertNotNull(certStatistics.subjectKeyIdSize, "rct stat-cert reports this Subject Key ID size.");	// TODO assert something better than not null
			Assert.assertEquals(certStatistics.contentSets, Integer.valueOf(entitlementCert.contentNamespaces.size()), "rct stat-cert reports this number of Content sets.");
		}
	}
	
	
	@Test(	description="assert the statistic values reported by the rct stat-cert tool for a zero-content set entitlement",
			groups={"blockedByBug-966137"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AssertEntitlementCertStatisticsForZeroContentSetEntitlement_Test() {
		File zeroContentSetEntitlementCertFile = new File("/tmp/zeroContentSetEntitlementCert.pem");
		client.runCommandAndWait("echo \""+zeroContentSetEntitlementCert.trim()+"\" > "+zeroContentSetEntitlementCertFile);
		
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(zeroContentSetEntitlementCertFile);
		//	[root@jsefler-5 tmp]# rct cat-cert /tmp/8a85f9843ad21fe4013ae6a01d0b5d3c.pem 
		//
		//	+-------------------------------------------+
		//		Entitlement Certificate
		//	+-------------------------------------------+
		//
		//	Certificate:
		//		Path: /tmp/8a85f9843ad21fe4013ae6a01d0b5d3c.pem
		//		Version: 3.0
		//		Serial: 8429484359950764937
		//		Start Date: 2012-11-09 05:00:00+00:00
		//		End Date: 2013-11-09 04:59:59+00:00
		//		Pool ID: Not Available
		//
		//	Subject:
		//		CN: 8a85f9843e139e8c013e1e24d82f1ed0
		//
		//	Issuer:
		//		C: US
		//		CN: Red Hat Candlepin Authority
		//		O: Red Hat, Inc.
		//		OU: Red Hat Network
		//		ST: North Carolina
		//		emailAddress: ca-support@redhat.com
		Assert.assertEquals(entitlementCert.id, "8a85f9843e139e8c013e1e24d82f1ed0","rct cat-cert reports this id.");
		Assert.assertEquals(entitlementCert.issuer, "Red Hat Candlepin Authority","rct cat-cert reports this issuer.");
		Assert.assertEquals(entitlementCert.contentNamespaces.size(), 0, "rct cat-cert reports this number of Content sets.");


		CertStatistics certStatistics = clienttasks.getCertStatisticsFromCertFile(zeroContentSetEntitlementCertFile);
		//	[root@jsefler-5 tmp]# rct stat-cert /tmp/8a85f9843ad21fe4013ae6a01d0b5d3c.pem 
		//	Type: Entitlement Certificate
		//	Version: 3.0
		//	DER size: 1891b
		//	Subject Key ID size: 20b
		//	Content sets: 0
		Assert.assertEquals(certStatistics.type, "Entitlement Certificate","rct stat-cert reports this Type.");
		Assert.assertEquals(certStatistics.version, entitlementCert.version,"rct stat-cert reports this Version.");
		Assert.assertEquals(certStatistics.derSize, "1891b", "rct stat-cert reports this DER size.");
		Assert.assertEquals(certStatistics.subjectKeyIdSize, "20b", "rct stat-cert reports this Subject Key ID size.");	// TODO assert something better than not null
		Assert.assertEquals(certStatistics.contentSets, Integer.valueOf(0), "rct stat-cert reports this number of Content sets.");
	}
	protected static final String zeroContentSetEntitlementCert = 
		"-----BEGIN CERTIFICATE-----"+"\n"+
		"MIIHXzCCBUegAwIBAgIIdPuLXTKTi4kwDQYJKoZIhvcNAQEFBQAwgaQxCzAJBgNV"+"\n"+
		"BAYTAlVTMRcwFQYDVQQIDA5Ob3J0aCBDYXJvbGluYTEWMBQGA1UECgwNUmVkIEhh"+"\n"+
		"dCwgSW5jLjEYMBYGA1UECwwPUmVkIEhhdCBOZXR3b3JrMSQwIgYDVQQDDBtSZWQg"+"\n"+
		"SGF0IENhbmRsZXBpbiBBdXRob3JpdHkxJDAiBgkqhkiG9w0BCQEWFWNhLXN1cHBv"+"\n"+
		"cnRAcmVkaGF0LmNvbTAeFw0xMjExMDkwNTAwMDBaFw0xMzExMDkwNDU5NTlaMCsx"+"\n"+
		"KTAnBgNVBAMTIDhhODVmOTg0M2UxMzllOGMwMTNlMWUyNGQ4MmYxZWQwMIIBIjAN"+"\n"+
		"BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA77qX2x+5FMhbEnVo2jqwKYzyVqcE"+"\n"+
		"pyV1u0smHoNiTOfGJrLSC6lpxzINlOiKns689DGeWgcCR8wyddSYeeoZQezcdXsL"+"\n"+
		"hLpLcyYDZO1Iull0ZYCsu9o7KVtckJoCzChe6SzUp18rF0OMTxiAPbxv+EHw6bre"+"\n"+
		"YS86JHZinz4Nf8pHb3sfckFLqZTTRvZ5ctCetfBLAoArQU1P7DP2cB0ShdKEqN0q"+"\n"+
		"T2V/QCeoR3TFsypgJWCFrxCkMpUfDymQq/z+vFFJlUz2q5sEWFVnY9nAYJysZtFm"+"\n"+
		"3Il4IGruvvp/yerQMkuVACRdiscd5p5frmmysNmZQm1WJUHQgPA/2prfAQIDAQAB"+"\n"+
		"o4IDCzCCAwcwEQYJYIZIAYb4QgEBBAQDAgWgMAsGA1UdDwQEAwIEsDCB3gYDVR0j"+"\n"+
		"BIHWMIHTgBR3LqXNNw2o4dPqYcVWZ0PokcdtHKGBt6SBtDCBsTELMAkGA1UEBhMC"+"\n"+
		"VVMxFzAVBgNVBAgMDk5vcnRoIENhcm9saW5hMRYwFAYDVQQKDA1SZWQgSGF0LCBJ"+"\n"+
		"bmMuMRgwFgYDVQQLDA9SZWQgSGF0IE5ldHdvcmsxMTAvBgNVBAMMKFJlZCBIYXQg"+"\n"+
		"RW50aXRsZW1lbnQgT3BlcmF0aW9ucyBBdXRob3JpdHkxJDAiBgkqhkiG9w0BCQEW"+"\n"+
		"FWNhLXN1cHBvcnRAcmVkaGF0LmNvbYIBPzAdBgNVHQ4EFgQUH4sIAAAAAAAAAAMA"+"\n"+
		"AAAAAAAAAAAwEwYDVR0lBAwwCgYIKwYBBQUHAwIwEgYJKwYBBAGSCAkGBAUMAzMu"+"\n"+
		"MDCCAboGCSsGAQQBkggJBwSCAasEggGneNodj1FuxDAIROcw+5uPtmpvUanqngDb"+"\n"+
		"bGLVMRYQr/b2JfmDYRgeuVXuDt0SEjuBD0MTKoka9cyKp+ifOXmVjizdT/cH3rHr"+"\n"+
		"umgo+zhCk8Io1Rx2JMtax+lfyIx92anTGknb6Lj//nzfMVTKkb3O6i+8YTrkWqCG"+"\n"+
		"Ic+w5sfiIs2CixuMdYYY9VwiKQAKz2vOji98wnLQpsaP2the5rwHxxiN9zCTvqBs"+"\n"+
		"9XrUXDQisNV1o0k11mIQFLdExqR5w025cTTnycLpWFFNIAaTQzPjH6W0bg0z3Ll8"+"\n"+
		"OX35YDK6Acp3nb+jxnS5rsrY6mbu5wvir1K9nrzuurPb6b48qfoU7qhznR4f6zpc"+"\n"+
		"13HUznSz4996J849etV0A5TvO39Hj05nS5rsrY6mboZwraRuXpFtGsV8JaxWzqr6"+"\n"+
		"tpH1bb/W9fMXoVbSL62BXflh4vjldIOU7zo8Ln3ddlbHUznBur0eoc50eFz7uu46"+"\n"+
		"mc6N1e/1LiVWG5adWEy9XldIOU7zo8LmuytjqZzg3V9sv/V9vL+ZbLLtxd/VDg8L"+"\n"+
		"iVXbg8UODwuJVVDhcSqgMA0GCSqGSIb3DQEBBQUAA4ICAQAAyGTZEj3O/lMWMSxA"+"\n"+
		"luoweBactyg1dNm0XgJBzNoZTpQ8zgPBeqU8lJAp4tuwpWHsZG3nK7L5BcPjYl5n"+"\n"+
		"lwKJi82aCEqi3kNaCVFjO9l6UgtLwv56zKxELF/YRHcX8gsVbJPH+UlZ23vqS3MM"+"\n"+
		"lZWFpZK+clmT8ILp+BR+wKz1jpgEXT4Jndjk1pUVuQ7hOJHOLyAsz5j6uGuCTftA"+"\n"+
		"FPxmrIqxbAS3aUInIsBr87n1LjIxwLtest8TxeuVYbdO05cwn9V9ezvP4hBYcZB1"+"\n"+
		"kmigkGzH8sq3YmJWXVynRK48fPr1SZEP6B5Ht1w3VAiAPyQ+mgBUExAbUYch/QJw"+"\n"+
		"N2Q8oemMDWMtPYO66viP09fU1YFfsd3zC7UJNt1v8MJN1f912ppZqGqPwqNmwY/F"+"\n"+
		"zKNfyMj+aL7/UoE/zET1Z5FWAviUjrazWG+j1bJpyjlmgLvaA9H0kMbxEYtbwVJZ"+"\n"+
		"cYZuk530Wk0kbJOpglrGcSmWw9aH8PXrqrEPrUDiDBdORirLs47t6uae5WozTU3k"+"\n"+
		"epJZox2Cf0stOCVZG3az43AR3ELnd808cJQZ3tJ8TvwcxYASjIlmr18IyDuxF3Zq"+"\n"+
		"F9K/67fgw7B/EhPCXgfAnN7Di0jAk40OXjtMMnwDro38biD9+GlMn0tff9wqwKZP"+"\n"+
		"gbNTs6MdV3TLHF7zcOhksJiALQ=="+"\n"+
		"-----END CERTIFICATE-----"+"\n"+
		"-----BEGIN RSA PRIVATE KEY-----"+"\n"+
		"MIIEpQIBAAKCAQEA77qX2x+5FMhbEnVo2jqwKYzyVqcEpyV1u0smHoNiTOfGJrLS"+"\n"+
		"C6lpxzINlOiKns689DGeWgcCR8wyddSYeeoZQezcdXsLhLpLcyYDZO1Iull0ZYCs"+"\n"+
		"u9o7KVtckJoCzChe6SzUp18rF0OMTxiAPbxv+EHw6breYS86JHZinz4Nf8pHb3sf"+"\n"+
		"ckFLqZTTRvZ5ctCetfBLAoArQU1P7DP2cB0ShdKEqN0qT2V/QCeoR3TFsypgJWCF"+"\n"+
		"rxCkMpUfDymQq/z+vFFJlUz2q5sEWFVnY9nAYJysZtFm3Il4IGruvvp/yerQMkuV"+"\n"+
		"ACRdiscd5p5frmmysNmZQm1WJUHQgPA/2prfAQIDAQABAoIBAQDEJvFKIlLWZnne"+"\n"+
		"SKNarNqnpORFcrOj8Eq8zWiLXwV3182SrI6hbCVZD+N9DCkgeBiz1kWzdDixdqtz"+"\n"+
		"Llj0qE+yWZSpq9xo5eYGefwdAOqZT8ilpPuxWYc+97pttxbC5eWC9WOq73vatTKB"+"\n"+
		"JZMI8L7Na2csV/LNok74tsVLdwKko9EPTdrMdyrxwlKcqjUIuryjV0xIm0BttGC5"+"\n"+
		"ocAVW2V4KTdHeq3JKme2qle51RBWciluN1V2JdnE1sD7tTUkScbinCOO8iapMZBJ"+"\n"+
		"RslzHbhtu39UkMcDY+wDjKT2ZIv5jYKF6TV+r27oDGZ2ulbwiWJtS/T5AR1ab/Ca"+"\n"+
		"8QqtCx6BAoGBAPrxu80ys4HKnP7dkUedQeH1e+1PA7LCkHWUsIkQF6n7IOU2Oo3Y"+"\n"+
		"42u9K5QIJ8cR74L/L8sCGc63DpwofgslixjinA1Qbk0t5nbOdru8+yW8hCSzn6HJ"+"\n"+
		"7crygKfxpMZ1nk1mGssl723bm0Ybl2/htaJCcPA3kiLNO5wz78VrVQXFAoGBAPSP"+"\n"+
		"A+dgWe1FOIGAhDyNVn5Hcaf8F1VuLXz45n0dSIlBVWRmXcOq6AtVX7DtszlQXahA"+"\n"+
		"bKxASuUDhi2RllIwLLeeCa+1UUmJgWgsYW0pLyI+aQ1yksKSAItEe7803aMv5Rsq"+"\n"+
		"spYaWHdYHcHbI+W0B6bp2kUvajylTJ2U/FOkEIQNAoGBAKSUgh0UUZKgNdMZsbyM"+"\n"+
		"MLdnbv22wrAs1t2mxRk/iqWa5Hov1LtPMehpSvltV9lBtBnwD4JPQGnIuTZFgFUD"+"\n"+
		"LHUHul0pEQ5hOjNVOZ3rVbPsLaZ8gAd2DhT6ctKysuTOIyKUAqKVoLAmRXH3ipyA"+"\n"+
		"JcXjWXrSl29ntt69WVXbDQoZAoGANoa8hBYDdtd8JcOVuDp7EmSzfpTCTxXlpnuI"+"\n"+
		"SFUDGzcp5ty8AyhT3FMIptYYo6q7dwwGLtGW4UDL5dUUAvciwT3HQxnWKeMyqxta"+"\n"+
		"DZClJalsmsb02dTbsjFutc7/q6a+GjSG5Niy6MkIuXQ2qLxfCGQTemF7+lGQp4HU"+"\n"+
		"UIG//PECgYEArCBreT/TDb4M9tY9K0UoOGr9sfnmkUyenGHkUZ+Z3jaNXTW9TSvD"+"\n"+
		"fZoRA85ol5M1QdmOHKXuOhZXaresFw9E3M92hFKrp5XBo1cTthWbkPsUrj6WmWf6"+"\n"+
		"b2h6lEAZ0zjcWjGzwW+W1N/HVs833/qydQ4heQEC60K/gUreSKekA0I="+"\n"+
		"-----END RSA PRIVATE KEY-----";
	
	// Candidates for an automated Test:
	// TODO https://bugzilla.redhat.com/show_bug.cgi?id=659735
	// TODO Bug 805690 - gpgcheck for repo metadata is ignored https://github.com/RedHatQE/rhsm-qe/issues/117
	// TODO Bug 806958 - One empty certificate file in /etc/rhsm/ca causes registration failure https://github.com/RedHatQE/rhsm-qe/issues/118
	
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
			
			// get all of the installed product certs
			List<File> productCertFiles = clienttasks.getCurrentProductCertFiles();
			if (productCertFiles.size()==0) log.warning("No Product Cert files were found.");
			for (File productCertFile : productCertFiles) {
				if (productCertFile.getName().endsWith("_.pem")) {
					log.info("Skipping the generated product cert '"+productCertFile+"' from those provided by @DataProvider name=getProductCertFilesData.");
					continue;
				}
				ll.add(Arrays.asList(new Object[]{productCertFile}));
			}
			
			// get all of the product certs from the subscription-manager-migration-data
			SSHCommandResult result = client.runCommandAndWait("rpm -ql subscription-manager-migration-data | grep .pem");
			for (String productCertFilePath : result.getStdout().split("\n")) {
				if (productCertFilePath.isEmpty()) continue;
				ll.add(Arrays.asList(new Object[]{new File(productCertFilePath)}));
			}
		}
		
		return ll;
	}
	
}
