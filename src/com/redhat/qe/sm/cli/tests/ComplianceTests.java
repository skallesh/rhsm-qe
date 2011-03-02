package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.LogMessageUtil;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.SubscriptionManagerTasks;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.InstalledProduct;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.ProductNamespace;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */


@Test(groups={"compliance"})
public class ComplianceTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************
	
	@Test(	description="subscription-manager: compliance test",
			groups={"configureProductCertDirForSomeProductsSubscribable"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenSomeProductsAreSubscribable() {
		clienttasks.listInstalledProducts();
	}
	
	@Test(	description="subscription-manager: compliance test",
			groups={"configureProductCertDirForAllProductsSubscribable"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenAllProductsAreSubscribable() {
		clienttasks.listInstalledProducts();
	}
	
	@Test(	description="subscription-manager: compliance test",
			groups={"configureProductCertDirForNoProductsSubscribable"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenNoProductsAreSubscribable() {
		clienttasks.listInstalledProducts();
	}
	
	@Test(	description="subscription-manager: compliance test",
			groups={"configureProductCertDirForNoProductsInstalled"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenNoProductsAreInstalled() {
		clienttasks.listInstalledProducts();
	}
	

	
	
	
	// TODO Candidates for an automated Test:
	
	
	
	
	
	
	
	// Protected Class Variables ***********************************************************************
	
//	protected List<String> systemConsumerIds = new ArrayList<String>();
//	protected SubscriptionPool testPool = null;
//	protected final String registereeName = "Overconsumer";
	protected final String productCertDirForSomeProductsSubscribable = "/tmp/sm-someProductsSubscribable";
	protected final String productCertDirForAllProductsSubscribable = "/tmp/sm-allProductsSubscribable";
	protected final String productCertDirForNoProductsSubscribable = "/tmp/sm-noProductsSubscribable";
	protected final String productCertDirForNoProductsinstalled = "/tmp/sm-noProductsInstalled";

	
	
	// Protected Methods ***********************************************************************
	@BeforeGroups(groups={"setup"},value="configureProductCertDirForSomeProductsSubscribable")
	protected void configureProductCertDirForSomeProductsSubscribable() {
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",productCertDirForSomeProductsSubscribable);
	}
	@BeforeGroups(groups={"setup"},value="configureProductCertDirForAllProductsSubscribable")
	protected void configureProductCertDirForAllProductsSubscribable() {
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",productCertDirForAllProductsSubscribable);
	}
	@BeforeGroups(groups={"setup"},value="configureProductCertDirForNoProductsSubscribable")
	protected void configureProductCertDirForNoProductsSubscribable() {
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",productCertDirForNoProductsSubscribable);
	}
	@BeforeGroups(groups={"setup"},value="configureProductCertDirForNoProductsInstalled")
	protected void configureProductCertDirForNoProductsInstalled() {
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",productCertDirForNoProductsinstalled);
	}
	

	
	// Configuration Methods ***********************************************************************

	@BeforeClass(groups={"setup"},alwaysRun=true)
	public void setupProductCertDirsBeforeClass() {
		
		// clean out the productCertDirs
		for (String productCertDir : new String[]{productCertDirForSomeProductsSubscribable,productCertDirForAllProductsSubscribable,productCertDirForNoProductsSubscribable,productCertDirForNoProductsinstalled}) {
			RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+productCertDir, 0);
			RemoteFileTasks.runCommandAndAssert(client, "mkdir "+productCertDir, 0);
		}
		
		// autosubscribe
		clienttasks.register(clientusername, clientpassword, null, null, null, Boolean.TRUE, Boolean.TRUE, null, null, null);
		
		// distribute a copy of the product certs amongst the productCertDirs
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		for (File productCertFile : clienttasks.getCurrentProductCertFiles()) {
			ProductCert productCert = clienttasks.getProductCertFromProductCertFile(productCertFile);
			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", productCert.productName, installedProducts);
			if (installedProduct.status.equalsIgnoreCase("Not Subscribed")) {
				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForNoProductsSubscribable, 0);
				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForSomeProductsSubscribable, 0);
			} else if (installedProduct.status.equalsIgnoreCase("Subscribed")) {
				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForAllProductsSubscribable, 0);
				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForSomeProductsSubscribable, 0);
			}
		}
		
		
		
		
		
//		// find the corresponding productNamespace from the entitlementCert
//		ProductNamespace productNamespace = null;
//		for (ProductNamespace pn : entitlementCert.productNamespaces) {
//			if (pn.name.equals(productName)) productNamespace = pn;
//		}
//		
//		// assert the installed status of the corresponding product
//		if (entitlementCert.productNamespaces.isEmpty()) {
//			log.warning("This product '"+productId+"' ("+productName+") does not appear to grant entitlement to any client side content.  This must be a server side management add-on product. Asserting as such...");
//
//			Assert.assertEquals(entitlementCert.contentNamespaces.size(),0,
//					"When there are no productNamespaces in the entitlementCert, there should not be any contentNamespaces.");
//
//			// when there is no corresponding product, then there better not be an installed product status by the same product name
//			Assert.assertNull(InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", productName, clienttasks.getCurrentlyInstalledProducts()),
//					"Should not find any installed product status matching a server side management add-on productName: "+ productName);
//
//			// when there is no corresponding product, then there better not be an installed product cert by the same product name
//			Assert.assertNull(ProductCert.findFirstInstanceWithMatchingFieldFromList("productName", productName, currentlyInstalledProductCerts),
//					"Should not find any installed product certs matching a server side management add-on productName: "+ productName);
//
//		} else {
//			Assert.assertNotNull(productNamespace, "The new entitlement cert's product namespace corresponding to this expected ProductSubscription with ProductName '"+productName+"' was found.");
//
//			// assert whether or not the product is installed			
//			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", productName, clienttasks.getCurrentlyInstalledProducts());
//			Assert.assertNotNull(installedProduct, "The status of product with ProductName '"+productName+"' is reported in the list of installed products.");
//
//			// assert the status of the installed product
//			//ProductCert productCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productName", productName, currentlyInstalledProductCerts);
//			ProductCert productCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("id", productNamespace.hash, currentlyInstalledProductCerts);
//			if (productCert!=null) {
//				Assert.assertEquals(installedProduct.status, "Subscribed", "After subscribing to ProductId '"+productId+"', the status of Installed Product '"+productName+"' is Subscribed since a corresponding product cert was found in "+clienttasks.productCertDir);
//				Assert.assertEquals(InstalledProduct.formatDateString(installedProduct.expires), ProductSubscription.formatDateString(productSubscription.endDate), "Installed Product '"+productName+"' expires on the same date as the consumed ProductSubscription: "+productSubscription);
//				Assert.assertEquals(installedProduct.subscription, productSubscription.serialNumber, "Installed Product '"+productName+"' subscription matches the serialNumber of the consumed ProductSubscription: "+productSubscription);
//			} else {
//				Assert.assertEquals(installedProduct.status, "Not Installed", "The status of Entitled Product '"+productName+"' is Not Installed since a corresponding product cert was not found in "+clienttasks.productCertDir);
//			}
//		}
		

	}
	
	
	
	// Data Providers ***********************************************************************

	

}

