package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.LogMessageUtil;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.InstalledProduct;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.ProductNamespace;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * Note: This scribe depends on register with --autosubscribe working properly
 */


@Test(groups={"ComplianceTests","AcceptanceTests"})
public class ComplianceTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************
	
	@Test(	description="subscription-manager: verify the system.compliant fact is False when some installed products are subscribable",
			groups={"configureProductCertDirForSomeProductsSubscribable","cli.tests"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenSomeProductsAreSubscribable_Test() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,(String)null,Boolean.TRUE,false, null, null, null);
		Assert.assertFalse(clienttasks.getCurrentlyInstalledProducts().isEmpty(),
				"Products are currently installed for which the compliance of only SOME are covered by currently available subscription pools.");
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.FALSE.toString(),
				"Before attempting to subscribe and become compliant for all the currently installed products, the system should be non-compliant (see value for fact '"+factNameForSystemCompliance+"').");
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		clienttasks.listInstalledProducts();
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.FALSE.toString(),
				"When a system has products installed for which only SOME are covered by available subscription pools, the system should NOT become compliant (see value for fact '"+factNameForSystemCompliance+"') even after having subscribed to every available subscription pool.");
	}
	
	@Test(	description="rhsm-complianced: verify rhsm-complianced -d -s reports a non-compliant status when some installed products are subscribable",
			groups={"blockedbyBug-723336","blockedbyBug-691480","cli.tests"},
			dependsOnMethods={"VerifySystemCompliantFactWhenSomeProductsAreSubscribable_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRhsmCompliancedWhenSomeProductsAreSubscribable_Test() {
		String command = clienttasks.rhsmComplianceD+" -s -d";
		RemoteFileTasks.runCommandAndWait(client, "echo 'Testing "+command+"' >> "+clienttasks.varLogMessagesFile, LogMessageUtil.action());

		// verify the stdout message
		RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0), rhsmComplianceDStdoutMessageWhenNonCompliant, null);
		
		// also verify the /var/syslog/messages
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.varLogMessagesFile, null, rhsmComplianceDSyslogMessageWhenNonCompliant, null);
	}
	
	
	
	@Test(	description="subscription-manager: verify the system.compliant fact is True when all installed products are subscribable",
			groups={"configureProductCertDirForAllProductsSubscribable","cli.tests"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenAllProductsAreSubscribable_Test() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,(String)null,Boolean.TRUE,false, null, null, null);
		Assert.assertFalse(clienttasks.getCurrentlyInstalledProducts().isEmpty(),
				"Products are currently installed for which the compliance of ALL are covered by currently available subscription pools.");
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.FALSE.toString(),
				"Before attempting to subscribe and become compliant for all the currently installed products, the system should be non-compliant (see value for fact '"+factNameForSystemCompliance+"').");
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()) {
			Assert.assertEquals(installedProduct.status, "Subscribed","When config rhsm.productcertdir is populated with product certs for which ALL are covered by the currently available subscriptions, then each installed product status should be Subscribed.");
		}
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.TRUE.toString(),
				"When a system has products installed for which ALL are covered by available subscription pools, the system should become compliant (see value for fact '"+factNameForSystemCompliance+"') after having subscribed to every available subscription pool.");
	}
	
	@Test(	description="rhsm-complianced: verify rhsm-complianced -d -s reports a compliant status when all installed products are subscribable",
			groups={"blockedbyBug-723336","cli.tests"},
			dependsOnMethods={"VerifySystemCompliantFactWhenAllProductsAreSubscribable_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRhsmCompliancedWhenAllProductsAreSubscribable_Test() {
		String command = clienttasks.rhsmComplianceD+" -s -d";

		// verify the stdout message
		RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0), rhsmComplianceDStdoutMessageWhenCompliant, null);
	}
	
	
	
	@Test(	description="subscription-manager: verify the system.compliant fact is False when no installed products are subscribable",
			groups={"configureProductCertDirForNoProductsSubscribable","cli.tests"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenNoProductsAreSubscribable_Test() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,(String)null,Boolean.TRUE,false, null, null, null);
		Assert.assertFalse(clienttasks.getCurrentlyInstalledProducts().isEmpty(),
				"Products are currently installed for which the compliance of NONE are covered by currently available subscription pools.");
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.FALSE.toString(),
				"Before attempting to subscribe and become compliant for all the currently installed products, the system should be non-compliant (see value for fact '"+factNameForSystemCompliance+"').");
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()) {
			Assert.assertEquals(installedProduct.status, "Not Subscribed","When config rhsm.productcertdir is populated with product certs for which NONE are covered by the currently available subscriptions, then each installed product status should be Not Subscribed.");
		}
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.FALSE.toString(),
				"When a system has products installed for which NONE are covered by available subscription pools, the system should NOT become compliant (see value for fact '"+factNameForSystemCompliance+"') after having subscribed to every available subscription pool.");
	}
	
	@Test(	description="rhsm-complianced: verify rhsm-complianced -d -s reports a non-compliant status when no installed products are subscribable",
			groups={"blockedbyBug-723336","blockedbyBug-691480","cli.tests"},
			dependsOnMethods={"VerifySystemCompliantFactWhenNoProductsAreSubscribable_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRhsmCompliancedWhenNoProductsAreSubscribable_Test() {
		String command = clienttasks.rhsmComplianceD+" -s -d";
		RemoteFileTasks.runCommandAndWait(client, "echo 'Testing "+command+"' >> "+clienttasks.varLogMessagesFile, LogMessageUtil.action());

		// verify the stdout message
		RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0), rhsmComplianceDStdoutMessageWhenNonCompliant, null);
		
		// also verify the /var/syslog/messages
		RemoteFileTasks.runCommandAndAssert(client,"tail -1 "+clienttasks.varLogMessagesFile, null, rhsmComplianceDSyslogMessageWhenNonCompliant, null);
	}
	
	
	
	@Test(	description="subscription-manager: verify the system.compliant fact is True when no products are installed",
			groups={"configureProductCertDirForNoProductsInstalled","cli.tests"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenNoProductsAreInstalled_Test() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,(String)null,Boolean.TRUE,false, null, null, null);
		Assert.assertTrue(clienttasks.getCurrentlyInstalledProducts().isEmpty(),
				"No products are currently installed.");
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.TRUE.toString(),
				"Because no products are currently installed, the system should inherently be compliant (see value for fact '"+factNameForSystemCompliance+"') even without subscribing to any subscription pools.");
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		clienttasks.listInstalledProducts();
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.TRUE.toString(),
				"Even after subscribing to all the available subscription pools, a system with no products installed should remain compliant (see value for fact '"+factNameForSystemCompliance+"').");
	}
	
	@Test(	description="rhsm-complianced: verify rhsm-complianced -d -s reports a compliant status when no products are installed",
			groups={"blockedbyBug-723336","cli.tests"},
			dependsOnMethods={"VerifySystemCompliantFactWhenNoProductsAreInstalled_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRhsmCompliancedWhenNoProductsAreInstalled_Test() {
		String command = clienttasks.rhsmComplianceD+" -s -d";

		// verify the stdout message
		RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0), rhsmComplianceDStdoutMessageWhenCompliant, null);
	}
	
	
	
	@Test(	description="subscription-manager: verify the system.compliant fact when system is already registered to RHN Classic",
			groups={"blockedByBug-742027","RHNClassicTests","cli.tests"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenRegisteredToRHNClassic_Test() {
		
		// pre-test check for installed products
		clienttasks.unregister(null,null,null);
		configureProductCertDirAfterClass();
		if (clienttasks.getCurrentlyInstalledProducts().isEmpty()) throw new SkipException("This test requires that at least one product cert is installed.");

		// first assert that we are not compliant since we have not yet registered to RHN Classic
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.FALSE.toString(),
				"While at least one product cert is installed and we are NOT registered to RHN Classic, the system should NOT be compliant (see value for fact '"+factNameForSystemCompliance+"').");

		// simulate registration to RHN Classic by creating a /etc/sysconfig/rhn/systemid
		log.info("Simulating registration to RHN Classic by creating an empty systemid file '"+clienttasks.rhnSystemIdFile+"'...");
		RemoteFileTasks.runCommandAndWait(client, "touch "+clienttasks.rhnSystemIdFile, LogMessageUtil.action());
		Assert.assertTrue(RemoteFileTasks.testFileExists(client, clienttasks.rhnSystemIdFile)==1, "RHN Classic systemid file '"+clienttasks.rhnSystemIdFile+"' is in place.");

		// now assert compliance
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.TRUE.toString(),
				"By definition, being registered to RHN Classic implies the system IS compliant no matter what products are installed (see value for fact '"+factNameForSystemCompliance+"').");
	}
	
	@Test(	description="rhsm-complianced: verify rhsm-complianced -d -s reports a compliant status when registered to RHN Classic",
			groups={"RHNClassicTests","cli.tests"},
			dependsOnMethods={"VerifySystemCompliantFactWhenRegisteredToRHNClassic_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRhsmCompliancedWhenRegisteredToRHNClassic_Test() {
		String command = clienttasks.rhsmComplianceD+" -s -d";

		// verify the stdout message
		RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0), rhsmComplianceDStdoutMessageWhenCompliantByRHNClassic, null);
	}
	
	
	
	@Test(	description="subscription-manager: verify the system.compliant fact remains False when all installed products are subscribable in the future",
			groups={"blockedbyBug-737553","blockedbyBug-649068","configureProductCertDirForAllProductsSubscribableInTheFuture","cli.tests"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifySystemCompliantFactWhenAllProductsAreSubscribableInTheFuture_Test() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,(String)null,Boolean.TRUE,false, null, null, null);

		// initial assertions
		Assert.assertFalse(clienttasks.getCurrentlyInstalledProducts().isEmpty(),
				"Products are currently installed for which the compliance of ALL are covered by future available subscription pools.");
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.FALSE.toString(),
				"Before attempting to subscribe to any future subscription, the system should be non-compliant (see value for fact '"+factNameForSystemCompliance+"').");
		
		// incrementally subscribe to each future subscription pool and assert the corresponding installed product's status
		for (SubscriptionPool futureSystemSubscriptionPool : futureSystemSubscriptionPools) {
			
			// subscribe without asserting results (not necessary)
			File entitlementCertFile = clienttasks.subscribeToSubscriptionPool_(futureSystemSubscriptionPool);
			List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
			
			// assert that the Status of the installed product is "Future Subscription"
			for (ProductCert productCert : clienttasks.getCurrentProductCertsCorrespondingToSubscriptionPool(futureSystemSubscriptionPool)) {
				InstalledProduct installedProduct = clienttasks.getInstalledProductCorrespondingToProductCert(productCert,installedProducts);
				Assert.assertEquals(installedProduct.status, "Future Subscription", "Status of the installed product '"+productCert.productName+"' after subscribing to future subscription pool: "+futureSystemSubscriptionPool);
				// TODO assert the installedProduct start/end dates
			}
		}
		
		// simply assert that actually did subscribe every installed product to a future subscription pool
		for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()) {
			Assert.assertEquals(installedProduct.status, "Future Subscription", "Status of every installed product should be a Future Subscription after subscribing all installed products to a future pool.  This Installed Product: "+installedProduct);
		}
		
		// finally assert that the overall system is non-compliant
		Assert.assertEquals(clienttasks.getFactValue(factNameForSystemCompliance).toLowerCase(), Boolean.FALSE.toString(),
				"When a system has products installed for which ALL are covered by future available subscription pools, the system should remain non-compliant (see value for fact '"+factNameForSystemCompliance+"') after having subscribed to every available subscription pool.");
	}
	
	@Test(	description="rhsm-complianced: verify rhsm-complianced -d -s reports a non-compliant status when all installed products are subscribable in the future",
			groups={"cli.tests"},
			dependsOnMethods={"VerifySystemCompliantFactWhenAllProductsAreSubscribableInTheFuture_Test"},
			enabled=true)
	//@ImplementsTCMS(id="")
	public void VerifyRhsmCompliancedWhenAllProductsAreSubscribableInTheFuture_Test() {
		String command = clienttasks.rhsmComplianceD+" -s -d";

		// verify the stdout message
		RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0), rhsmComplianceDStdoutMessageWhenNonCompliant, null);
	}
	
	
	
	// Candidates for an automated Test:
	// TODO INVERSE OF VerifySystemCompliantFactWhenAllProductsAreSubscribableInTheFuture_Test - should not be compliant for an expired subscription
	// TODO Bug 727967 - Compliance Assistant Valid Until Date Detection Not Working
	
	
	
	// Protected Class Variables ***********************************************************************
	
	protected final String productCertDirForSomeProductsSubscribable = "/tmp/sm-someProductsSubscribable";
	protected final String productCertDirForAllProductsSubscribable = "/tmp/sm-allProductsSubscribable";
	protected final String productCertDirForNoProductsSubscribable = "/tmp/sm-noProductsSubscribable";
	protected final String productCertDirForNoProductsinstalled = "/tmp/sm-noProductsInstalled";
	protected final String productCertDirForAllProductsSubscribableInTheFuture = "/tmp/sm-allProductsSubscribableInTheFuture";
	protected String productCertDir = null;
	protected final String factNameForSystemCompliance = "system.entitlements_valid"; // "system.compliant"; // changed with the removal of the word "compliance" 3/30/2011
	protected final String rhsmComplianceDStdoutMessageWhenNonCompliant = "System has one or more certificates that are not valid";
	protected final String rhsmComplianceDStdoutMessageWhenCompliant = "System entitlements appear valid";
	protected final String rhsmComplianceDStdoutMessageWhenCompliantByRHNClassic = "System is already registered to another entitlement system";
	protected final String rhsmComplianceDSyslogMessageWhenNonCompliant = "This system is missing one or more valid entitlement certificates. Please run subscription-manager for more information.";
	protected List<SubscriptionPool> futureSystemSubscriptionPools = null;
	
	// Protected Methods ***********************************************************************
	
	
	
	
	// Configuration Methods ***********************************************************************

	@AfterGroups(groups={"setup"},value="RHNClassicTests")
	public void removeRHNSystemIdFile() {
		client.runCommandAndWait("rm -rf "+clienttasks.rhnSystemIdFile);;
	}
	
	@BeforeClass(groups={"setup"})
	public void setupProductCertDirsBeforeClass() throws ParseException, JSONException, Exception {
		
		// clean out the productCertDirs
		for (String productCertDir : new String[]{productCertDirForSomeProductsSubscribable,productCertDirForAllProductsSubscribable,productCertDirForNoProductsSubscribable,productCertDirForNoProductsinstalled,productCertDirForAllProductsSubscribableInTheFuture}) {
			RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+productCertDir, 0);
			RemoteFileTasks.runCommandAndAssert(client, "mkdir "+productCertDir, 0);
		}

// THIS FORMER IMPLEMENTATION DEPENDS ON AUTOSUBSCRIBE WORKING
//		// autosubscribe
////	clienttasks.unregister(null,null,null);	// avoid Bug 733525 - [Errno 2] No such file or directory: '/etc/pki/entitlement'
//	clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, (String)null, true, null, null, null);
//	
//	// distribute a copy of the product certs amongst the productCertDirs
//	List<EntitlementCert> entitlementCerts = clienttasks.getCurrentEntitlementCerts();
//	for (File productCertFile : clienttasks.getCurrentProductCertFiles()) {
//		ProductCert productCert = clienttasks.getProductCertFromProductCertFile(productCertFile);
//		
//		// WORKAROUND NEEDED FOR Bug 733805 - the name in the subscription-manager installed product listing is changing after a valid subscribe is performed (https://bugzilla.redhat.com/show_bug.cgi?id=733805)
//		List<EntitlementCert> correspondingEntitlementCerts = clienttasks.getEntitlementCertsCorrespondingToProductCert(productCert);
//		
//		if (correspondingEntitlementCerts.isEmpty()) {
//			// "Not Subscribed" case...
//			RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForNoProductsSubscribable, 0);
//			RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForSomeProductsSubscribable, 0);
//		} else {
//			// "Subscribed" case...
//			RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForAllProductsSubscribable, 0);
//			RemoteFileTasks.runCommandAndAssert(client, "cp "+productCertFile+" "+productCertDirForSomeProductsSubscribable, 0);
//		}
//		// TODO "Partially Subscribed" case
//		//InstalledProduct installedProduct = clienttasks.getInstalledProductCorrespondingToEntitlementCert(correspondingEntitlementCert);
//	}
		
		// register and subscribe to all available subscriptions
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, (String)null, true, false, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		
		// distribute a copy of the product certs amongst the productCertDirs based on their status
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		for (ProductCert productCert : clienttasks.getCurrentProductCerts()) {
			InstalledProduct installedProduct = clienttasks.getInstalledProductCorrespondingToProductCert(productCert,installedProducts);
			
			if (installedProduct.status.equals("Not Subscribed")) {
				// "Not Subscribed" case...
				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCert.file+" "+productCertDirForNoProductsSubscribable, 0);
				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCert.file+" "+productCertDirForSomeProductsSubscribable, 0);
			} else if (installedProduct.status.equals("Subscribed")) {
				// "Subscribed" case...
				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCert.file+" "+productCertDirForAllProductsSubscribable, 0);
				RemoteFileTasks.runCommandAndAssert(client, "cp "+productCert.file+" "+productCertDirForSomeProductsSubscribable, 0);
			} else {
				// TODO "Partially Subscribed" case
				//InstalledProduct installedProduct = clienttasks.getInstalledProductCorrespondingToEntitlementCert(correspondingEntitlementCert);
			}
		}
		
		
		// setup for productCertDirForAllProductsSubscribableInTheFuture
		//clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		List<File> productCertFilesCopied = new ArrayList<File>();
		futureSystemSubscriptionPools = new ArrayList<SubscriptionPool>();
		for (List<Object> futureSystemSubscriptionPoolsDataRow : getAllFutureSystemSubscriptionPoolsDataAsListOfLists()) {
			SubscriptionPool futureSystemSubscriptionPool = (SubscriptionPool)futureSystemSubscriptionPoolsDataRow.get(0);
			for (ProductCert productCert : clienttasks.getCurrentProductCertsCorrespondingToSubscriptionPool(futureSystemSubscriptionPool)) {
				if (!productCertFilesCopied.contains(productCert.file)) {
					//RemoteFileTasks.runCommandAndAssert(client, "cp -n "+productCert.file+" "+productCertDirForAllProductsSubscribableInTheFuture, 0);	// RHEL5 does not understand cp -n  
					RemoteFileTasks.runCommandAndAssert(client, "if [ ! -e "+productCertDirForAllProductsSubscribableInTheFuture+File.separator+productCert.file.getName()+" ]; then cp "+productCert.file+" "+productCertDirForAllProductsSubscribableInTheFuture+"; fi;", 0);	// no clobber copy for both RHEL5 ad RHEL6
					productCertFilesCopied.add(productCert.file);
					if (!futureSystemSubscriptionPools.contains(futureSystemSubscriptionPool)) {
						futureSystemSubscriptionPools.add(futureSystemSubscriptionPool);
					}
				}
			}
		}
		
		
		this.productCertDir = clienttasks.productCertDir;
	}
	
	@AfterClass(groups={"setup"},alwaysRun=true)
	public void configureProductCertDirAfterClass() {
		if (clienttasks==null) return;
		if (this.productCertDir!=null) clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", this.productCertDir);
	}
	
	
	@BeforeGroups(groups={"setup"},value="configureProductCertDirForSomeProductsSubscribable")
	protected void configureProductCertDirForSomeProductsSubscribable() {
		clienttasks.unregister(null, null, null);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",productCertDirForSomeProductsSubscribable);
		SSHCommandResult r0 = client.runCommandAndWait("ls -1 "+productCertDirForSomeProductsSubscribable+" | wc -l");
		SSHCommandResult r1 = client.runCommandAndWait("ls -1 "+productCertDirForAllProductsSubscribable+" | wc -l");
		SSHCommandResult r2 = client.runCommandAndWait("ls -1 "+productCertDirForNoProductsSubscribable+" | wc -l");
		if (Integer.valueOf(r1.getStdout().trim())==0) throw new SkipException("Could not find any installed product certs that are subscribable based on the currently available subscriptions.");
		if (Integer.valueOf(r2.getStdout().trim())==0) throw new SkipException("Could not find any installed product certs that are non-subscribable based on the currently available subscriptions.");
		Assert.assertTrue(Integer.valueOf(r0.getStdout().trim())>0 && Integer.valueOf(r1.getStdout().trim())>0 && Integer.valueOf(r2.getStdout().trim())>0,
				"The "+clienttasks.rhsmConfFile+" file is currently configured with a productCertDir that contains some subscribable products based on the currently available subscriptions.");
	}
	@BeforeGroups(groups={"setup"},value="configureProductCertDirForAllProductsSubscribable")
	protected void configureProductCertDirForAllProductsSubscribable() {
		clienttasks.unregister(null, null, null);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",productCertDirForAllProductsSubscribable);	
		SSHCommandResult r = client.runCommandAndWait("ls -1 "+productCertDirForAllProductsSubscribable+" | wc -l");
		if (Integer.valueOf(r.getStdout().trim())==0) throw new SkipException("Could not find any installed product certs that are subscribable based on the currently available subscriptions.");
		Assert.assertTrue(Integer.valueOf(r.getStdout().trim())>0,
				"The "+clienttasks.rhsmConfFile+" file is currently configured with a productCertDir that contains all subscribable products based on the currently available subscriptions.");
	}
	@BeforeGroups(groups={"setup"},value="configureProductCertDirForNoProductsSubscribable")
	protected void configureProductCertDirForNoProductsSubscribable() {
		clienttasks.unregister(null, null, null);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",productCertDirForNoProductsSubscribable);
		SSHCommandResult r = client.runCommandAndWait("ls -1 "+productCertDirForNoProductsSubscribable+" | wc -l");
		if (Integer.valueOf(r.getStdout().trim())==0) throw new SkipException("Could not find any installed product certs that are non-subscribable based on the currently available subscriptions.");
		Assert.assertTrue(Integer.valueOf(r.getStdout().trim())>0,
				"The "+clienttasks.rhsmConfFile+" file is currently configured with a productCertDir that contains all non-subscribable products based on the currently available subscriptions.");
	}
	@BeforeGroups(groups={"setup"},value="configureProductCertDirForNoProductsInstalled")
	protected void configureProductCertDirForNoProductsInstalled() {
		clienttasks.unregister(null, null, null);
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",productCertDirForNoProductsinstalled);
		SSHCommandResult r = client.runCommandAndWait("ls -1 "+productCertDirForNoProductsinstalled+" | wc -l");
		Assert.assertEquals(Integer.valueOf(r.getStdout().trim()),Integer.valueOf(0),
				"The "+clienttasks.rhsmConfFile+" file is currently configured with a productCertDir that contains no products.");
	}
	
	@BeforeGroups(groups={"setup"},value="configureProductCertDirForAllProductsSubscribableInTheFuture")
	protected void configureProductCertDirForAllProductsSubscribableInTheFuture() throws JSONException, Exception {
		clienttasks.unregister(null, null, null);
//		configureProductCertDirAfterClass();
////		for (List<Object> futureJSONPoolsDataRow : getAllFutureJSONPoolsDataAsListOfLists(ConsumerType.system)) {
////			JSONObject futureJSONPool = (JSONObject)futureJSONPoolsDataRow.get(0);
////			for (ProductCert productCert : clienttasks.getCurrentProductCertsCorrespondingToSubscriptionPool(new SubscriptionPool(futureJSONPool.getString("productId"), futureJSONPool.getString("id")))) {
////				RemoteFileTasks.runCommandAndAssert(client, "cp -n "+productCert.file+" "+productCertDirForAllProductsSubscribableInTheFuture, 0);
////			}
////		}
//		List<File> productCertFilesCopied = new ArrayList<File>();
//		futureSystemSubscriptionPools = new ArrayList<SubscriptionPool>();
//		for (List<Object> futureSystemSubscriptionPoolsDataRow : getAllFutureSystemSubscriptionPoolsDataAsListOfLists()) {
//			SubscriptionPool futureSystemSubscriptionPool = (SubscriptionPool)futureSystemSubscriptionPoolsDataRow.get(0);
//			for (ProductCert productCert : clienttasks.getCurrentProductCertsCorrespondingToSubscriptionPool(futureSystemSubscriptionPool)) {
//				if (!productCertFilesCopied.contains(productCert.file)) {
//					RemoteFileTasks.runCommandAndAssert(client, "cp -n "+productCert.file+" "+productCertDirForAllProductsSubscribableInTheFuture, 0);
//					productCertFilesCopied.add(productCert.file);
//					if (!futureSystemSubscriptionPools.contains(futureSystemSubscriptionPool)) {
//						futureSystemSubscriptionPools.add(futureSystemSubscriptionPool);
//					}
//				}
//			}
//		}
		
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",productCertDirForAllProductsSubscribableInTheFuture);	
		SSHCommandResult r = client.runCommandAndWait("ls -1 "+productCertDirForAllProductsSubscribableInTheFuture+" | wc -l");
		if (Integer.valueOf(r.getStdout().trim())==0) throw new SkipException("Could not find any installed product certs that are subscribable to future available subscriptions.");
		Assert.assertTrue(Integer.valueOf(r.getStdout().trim())>0,
				"The "+clienttasks.rhsmConfFile+" file is currently configured with a productCertDir that contains all subscribable products based on future available subscriptions.");
	}
	// Data Providers ***********************************************************************

	

}

