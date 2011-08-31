package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.auto.testng.BzChecker;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * Bug 730380 - Can not import a certificate via the cli - https://bugzilla.redhat.com/show_bug.cgi?id=730380
 * Bug 712980 - Import Certificate neglects to also import the corresponding key.pem - https://bugzilla.redhat.com/show_bug.cgi?id=712980
 * Bug 733873 - subscription-manager import --help should not use proxy options - https://bugzilla.redhat.com/show_bug.cgi?id=733873
 */
@Test(groups={"ImportTests"})
public class ImportTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	
	@Test(	description="subscription-manager: import a valid entitlement cert/key bundle and verify the consumed subscriptions",
			groups={"blockedByBug-730380","blockedByBug-712980"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ImportCertificateAndAssertConsumedSubscriptions_Test() {
		// import from a file whose name matches the original entitlement cert pem file name
		File importCertificateFile = importBundledCertificateFile;
		
		// once imported, what should the entitlement cert file be?
		File expectedEntitlementCertFile = new File (clienttasks.entitlementCertDir+"/"+importCertificateFile.getName());
		File expectedEntitlementKeyFile = clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(expectedEntitlementCertFile);
		
		// make sure the expected entitlement files do not exist before our test
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementCertFile.getPath()),0,"Before attempting the import, asserting that expected destination for the entitlement cert file does NOT yet exist ("+expectedEntitlementCertFile+").");
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementKeyFile.getPath()),0,"Before attempting the import, asserting that expected destination for the entitlement key file does NOT yet exist ("+expectedEntitlementKeyFile+").");

		// show the contents of the file about to be imported
		log.info("Following is the contents of the certificate file about to be imported...");
		client.runCommandAndWait("cat "+importCertificateFile);
		
		// attempt an entitlement cert import from a valid bundled file
		clienttasks.importCertificate(importCertificateFile.getPath());
		
		// verify that the expectedEntitlementCertFile now exists
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementCertFile.getPath()),1,"After attempting the import, the expected destination for the entitlement cert file should now exist ("+expectedEntitlementCertFile+").");

		// verify that the expectedEntitlementKeyFile also exists
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementKeyFile.getPath()),1,"After attempting the import, the expected destination for the entitlement key file should now exist ("+expectedEntitlementKeyFile+").");
		
		// assert that the contents of the imported files match the originals
		log.info("Asserting that the imported entitlement cert file contents match the original...");
		RemoteFileTasks.runCommandAndAssert(client, "diff -w "+expectedEntitlementCertFile+" "+importEntitlementCertFile, 0);
		log.info("Asserting that the imported entitlement key file contents match the original...");
		RemoteFileTasks.runCommandAndAssert(client, "diff -w "+expectedEntitlementKeyFile+" "+importEntitlementKeyFile, 0);
		
		// finally verify that the consumed ProductSubscriptions match those that came from the original entitlement cert
		log.info("Verifying that the consumed ProductSubscriptions match those that came from the original entitlement cert...");
		List <ProductSubscription> productSubscriptionsAfterImport = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptionsAfterImport.size(), productSubscriptions.size(), "After importing a valid certificate, product subscriptions should be consumed.");
		for (ProductSubscription productSubscription : productSubscriptionsAfterImport) {
			Assert.assertContains(productSubscriptions, productSubscription);
		}
	}
	
	@Test(	description="subscription-manager: attempt an entitlement cert import from a file containing both the cert and key (in that order)",
			groups={"blockedByBug-730380","blockedByBug-712980","blockedByBug-734606"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ImportAnEntitlementCertAndKeyFromFile_Test() {
		// create a single file containing both the entitlement and key concatenated together.
		File importCertificateFile = new File("/tmp/sm-importEntitlementCertAndKey.pem");
		client.runCommandAndWait("cat "+importEntitlementCertFile+" "+importEntitlementKeyFile+" > "+importCertificateFile);
		
		// once imported, what should the entitlement cert file be?
		File expectedEntitlementCertFile = new File (clienttasks.entitlementCertDir+"/"+importCertificateFile.getName());
		File expectedEntitlementKeyFile = clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(expectedEntitlementCertFile);
		
		// make sure the expected entitlement files do not exist before our test
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementCertFile.getPath()),0,"Before attempting the import, asserting that expected destination for the entitlement cert file does NOT yet exist ("+expectedEntitlementCertFile+").");
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementKeyFile.getPath()),0,"Before attempting the import, asserting that expected destination for the entitlement key file does NOT yet exist ("+expectedEntitlementKeyFile+").");

		// show the contents of the file about to be imported
		log.info("Following is the contents of the certificate file about to be imported...");
		client.runCommandAndWait("cat "+importCertificateFile);
		
		// attempt an entitlement cert import from a valid file
		clienttasks.importCertificate(importCertificateFile.getPath());
		
		// verify that the expectedEntitlementCertFile now exists
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementCertFile.getPath()),1,"After attempting the import, the expected destination for the entitlement cert file should now exist ("+expectedEntitlementCertFile+").");

		// verify that the expectedEntitlementKeyFile also exists
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementKeyFile.getPath()),1,"After attempting the import, the expected destination for the entitlement key file should now exist ("+expectedEntitlementKeyFile+").");
		
		// assert that the contents of the imported files match the originals
		log.info("Asserting that the imported entitlement cert file contents match the original...");
		RemoteFileTasks.runCommandAndAssert(client, "diff -w "+expectedEntitlementCertFile+" "+importEntitlementCertFile, 0);
		log.info("Asserting that the imported entitlement key file contents match the original...");
		RemoteFileTasks.runCommandAndAssert(client, "diff -w "+expectedEntitlementKeyFile+" "+importEntitlementKeyFile, 0);
		
		// also verify that the consumed ProductSubscriptions match those that came from the original entitlement cert
		log.info("Verifying that the consumed ProductSubscriptions match those that came from the original entitlement cert...");
		List <ProductSubscription> productSubscriptionsAfterImport = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptionsAfterImport.size(), productSubscriptions.size(), "After importing a valid certificate, product subscriptions should be consumed.");
		for (ProductSubscription productSubscription : productSubscriptionsAfterImport) {
			Assert.assertContains(productSubscriptions, productSubscription);
		}
	}
	
	
	@Test(	description="subscription-manager: attempt an entitlement cert import from a file containing both the key and cert (in that order)",
			groups={"blockedByBug-730380","blockedByBug-712980","blockedByBug-734606"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ImportAnEntitlementKeyAndCertFromFile_Test() {
		
		// create a single file containing both the key and the entitlement concatenated together.
		File importCertificateFile = new File("/tmp/sm-importEntitlementKeyAndCert.pem");
		client.runCommandAndWait("cat "+importEntitlementKeyFile+" "+importEntitlementCertFile+" > "+importCertificateFile);

		// once imported, what should the entitlement cert file be?
		File expectedEntitlementCertFile = new File (clienttasks.entitlementCertDir+"/"+importCertificateFile.getName());
		File expectedEntitlementKeyFile = clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(expectedEntitlementCertFile);
		
		// make sure the expected entitlement files do not exist before our test
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementCertFile.getPath()),0,"Before attempting the import, asserting that expected destination for the entitlement cert file does NOT yet exist ("+expectedEntitlementCertFile+").");
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementKeyFile.getPath()),0,"Before attempting the import, asserting that expected destination for the entitlement key file does NOT yet exist ("+expectedEntitlementKeyFile+").");

		// show the contents of the file about to be imported
		log.info("Following is the contents of the certificate file about to be imported...");
		client.runCommandAndWait("cat "+importCertificateFile);
		
		// attempt an entitlement cert import from a valid file
		clienttasks.importCertificate(importCertificateFile.getPath());
		
		// verify that the expectedEntitlementCertFile now exists
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementCertFile.getPath()),1,"After attempting the import, the expected destination for the entitlement cert file should now exist ("+expectedEntitlementCertFile+").");

		// verify that the expectedEntitlementKeyFile also exists
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementKeyFile.getPath()),1,"After attempting the import, the expected destination for the entitlement key file should now exist ("+expectedEntitlementKeyFile+").");

		// assert that the contents of the imported files match the originals
		log.info("Asserting that the imported entitlement cert file contents match the original...");
		RemoteFileTasks.runCommandAndAssert(client, "diff -w "+expectedEntitlementCertFile+" "+importEntitlementCertFile, 0);
		log.info("Asserting that the imported entitlement key file contents match the original...");
		RemoteFileTasks.runCommandAndAssert(client, "diff -w "+expectedEntitlementKeyFile+" "+importEntitlementKeyFile, 0);
		
		// also verify that the consumed ProductSubscriptions match those that came from the original entitlement cert
		log.info("Verifying that the consumed ProductSubscriptions match those that came from the original entitlement cert...");
		List <ProductSubscription> productSubscriptionsAfterImport = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptionsAfterImport.size(), productSubscriptions.size(), "After importing a valid certificate, product subscriptions should be consumed.");
		for (ProductSubscription productSubscription : productSubscriptionsAfterImport) {
			Assert.assertContains(productSubscriptions, productSubscription);
		}
	}
	
	
	@Test(	description="subscription-manager: attempt an entitlement cert import from a file in the current directory",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ImportAnEntitlementKeyAndCertFromFileInCurrentDirectoty_Test() {
		
		// create a single file containing both the key and the entitlement concatenated together.
		File importCertificateFile = new File("/tmp/sm-importEntitlement");
		client.runCommandAndWait("cat "+importEntitlementKeyFile+" "+importEntitlementCertFile+" > "+importCertificateFile);

		// once imported, what should the entitlement cert file be?
		File expectedEntitlementCertFile = new File (clienttasks.entitlementCertDir+"/"+importCertificateFile.getName());
		File expectedEntitlementKeyFile = clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(expectedEntitlementCertFile);
		
		// make sure the expected entitlement files do not exist before our test
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementCertFile.getPath()),0,"Before attempting the import, asserting that expected destination for the entitlement cert file does NOT yet exist ("+expectedEntitlementCertFile+").");
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementKeyFile.getPath()),0,"Before attempting the import, asserting that expected destination for the entitlement key file does NOT yet exist ("+expectedEntitlementKeyFile+").");

		// show the contents of the file about to be imported
		log.info("Following is the contents of the certificate file about to be imported...");
		client.runCommandAndWait("cd "+importCertificateFile.getParent()+"; cat "+importCertificateFile.getName());
		
		// attempt an entitlement cert import from a valid file in the current directory
		SSHCommandResult importResult = client.runCommandAndWait("cd "+importCertificateFile.getParent()+"; "+clienttasks.command+" import --certificate "+importCertificateFile.getName());
		Assert.assertEquals(importResult.getStdout()./*TODO Change to getStderr() after fix for bug 734533*/trim(), "Successfully imported certificate "+importCertificateFile.getName());
		
		// verify that the expectedEntitlementCertFile now exists
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementCertFile.getPath()),1,"After attempting the import, the expected destination for the entitlement cert file should now exist ("+expectedEntitlementCertFile+").");

		// verify that the expectedEntitlementKeyFile also exists
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementKeyFile.getPath()),1,"After attempting the import, the expected destination for the entitlement key file should now exist ("+expectedEntitlementKeyFile+").");

		// assert that the contents of the imported files match the originals
		log.info("Asserting that the imported entitlement cert file contents match the original...");
		RemoteFileTasks.runCommandAndAssert(client, "diff -w "+expectedEntitlementCertFile+" "+importEntitlementCertFile, 0);
		log.info("Asserting that the imported entitlement key file contents match the original...");
		RemoteFileTasks.runCommandAndAssert(client, "diff -w "+expectedEntitlementKeyFile+" "+importEntitlementKeyFile, 0);
	}
	
	
	@Test(	description="subscription-manager: attempt an entitlement cert import from a file containing the cert only",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ImportAnEntitlementCertFromFile_Test() {
		
		// once imported, what should the entitlement cert file be?
		File expectedEntitlementCertFile = new File (clienttasks.entitlementCertDir+"/"+importEntitlementCertFile.getName());
		File expectedEntitlementKeyFile = clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(expectedEntitlementCertFile);
		
		// make sure the expected entitlement files do not exist before our test
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementCertFile.getPath()),0,"Before attempting the import, asserting that expected destination for the entitlement cert file does NOT yet exist ("+expectedEntitlementCertFile+").");
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementKeyFile.getPath()),0,"Before attempting the import, asserting that expected destination for the entitlement key file does NOT yet exist ("+expectedEntitlementKeyFile+").");

		// show the contents of the file about to be imported
		log.info("Following is the contents of the certificate file about to be imported...");
		client.runCommandAndWait("cat "+importEntitlementKeyFile);
		
		// attempt an entitlement cert import from a file containing the cert only
		clienttasks.importCertificate(importEntitlementCertFile.getPath());
		
		// verify that the expectedEntitlementCertFile now exists
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementCertFile.getPath()),1,"After attempting the import, the expected destination for the entitlement cert file should now exist ("+expectedEntitlementCertFile+").");

		// verify that the expectedEntitlementKeyFile does NOT exist
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementKeyFile.getPath()),0,"After attempting the import, the expected destination for the entitlement key file should NOT exist ("+expectedEntitlementKeyFile+") since there was no key in the import file.");
		
		// assert that the contents of the imported files match the originals
		log.info("Asserting that the imported entitlement cert file contents match the original...");
		RemoteFileTasks.runCommandAndAssert(client, "diff -w "+expectedEntitlementCertFile+" "+importEntitlementCertFile, 0);
	}
	
	
	
	
	@Test(	description="subscription-manager: import a certificate for a future entitlemnt",
			groups={},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void ImportAFutureEntitlementCertFromFile_Test() throws Exception {
		
		// pick a random future pool
		Object[][] FutureSystemSubscriptionPoolsData = getAllFutureSystemSubscriptionPoolsDataAs2dArray();
		if (FutureSystemSubscriptionPoolsData.length<=0) throw new SkipException("Cannot find a future subscription.");
		SubscriptionPool pool = (SubscriptionPool) FutureSystemSubscriptionPoolsData[randomGenerator.nextInt(FutureSystemSubscriptionPoolsData.length)][0];
		
		// subscribe to the future subscription pool
		SSHCommandResult subscribeResult = clienttasks.subscribe(null,pool.poolId,null,null,null,null,null,null,null,null);

		// assert that the granted entitlement cert begins in the future
		Calendar now = new GregorianCalendar();	now.setTimeInMillis(System.currentTimeMillis());
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertCorrespondingToSubscribedPool(pool);
		Assert.assertNotNull(entitlementCert,"Found the newly granted EntitlementCert on the client after subscribing to future subscription pool '"+pool.poolId+"'.");
		Assert.assertTrue(entitlementCert.validityNotBefore.after(now), "The newly granted EntitlementCert is not valid until the future.  EntitlementCert: "+entitlementCert);
		Assert.assertTrue(entitlementCert.orderNamespace.startDate.after(now), "The newly granted EntitlementCert's OrderNamespace starts in the future.  OrderNamespace: "+entitlementCert.orderNamespace);	
		
		// create a valid bundled certificate (by the same name as the original entitlement cert) see bug https://bugzilla.redhat.com/show_bug.cgi?id=734606
		File entitlementCertFile = clienttasks.getEntitlementCertFileFromEntitlementCert(entitlementCert);
		File entitlementKeyFile = clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(entitlementCertFile);
		File importBundledCertificateFile = new File("/tmp/"+entitlementCertFile.getName());
		client.runCommandAndWait("cat "+entitlementCertFile+" "+entitlementKeyFile+" > "+importBundledCertificateFile);
		
		// remember the consumed product subscriptions from this subscribe
		List<ProductSubscription> productSubscriptions = new ArrayList<ProductSubscription>();
		for (ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			if (clienttasks.getSubscriptionPoolFromProductSubscription(productSubscription, sm_clientUsername, sm_clientPassword).equals(pool)) {
				productSubscriptions.add(productSubscription);
			}
		}
		
		// delete all of the entitlement cert and key from /etc/pki/entitlement
		clienttasks.removeAllCerts(false, true);
		Assert.assertEquals(clienttasks.getCurrentlyConsumedProductSubscriptions().size(),0);
		
		// attempt an entitlement cert import from a valid bundled file
		clienttasks.importCertificate(importBundledCertificateFile.getPath());
				
		// verify that the consumed ProductSubscriptions match those that came from the original future entitlement cert
		log.info("Verifying that the consumed ProductSubscriptions match those that came from the original future entitlement cert...");
		List <ProductSubscription> productSubscriptionsAfterImport = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(productSubscriptionsAfterImport.size(), productSubscriptions.size(), "After importing a valid certificate, product subscriptions should be consumed.");
		for (ProductSubscription productSubscription : productSubscriptionsAfterImport) {
			Assert.assertContains(productSubscriptions, productSubscription);
		}
	}
	
	@Test(	description="subscription-manager: attempt an entitlement cert import from a file containing only a key (negative test)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptAnEntitlementImportFromAKeyFile_Test() {
		attemptAnEntitlementImportFromAnInvalidFile_Test(importEntitlementKeyFile);
	}
	
	
	@Test(	description="subscription-manager: attempt an entitlement cert import using an identity cert (negative test)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptAnEntitlementImportFromAnIdentityCertFile_Test() {
		attemptAnEntitlementImportFromAnInvalidFile_Test(new File(clienttasks.consumerCertFile));
	}
	
	
	@Test(	description="subscription-manager: attempt an entitlement cert import using non existent file (negative test)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptAnEntitlementImportFromNonExistentFile_Test() {
		String invalidCertificate = "/tmp/NonExistentFile.pem";
		attemptAnEntitlementImportFromAnInvalidFile_Test(new File(invalidCertificate));
	}
	
	
	// Protected Class Variables ***********************************************************************
	
	protected final File importEntitlementCertFile = new File("/tmp/sm-importEntitlementCert");
	protected final File importEntitlementKeyFile = new File("/tmp/sm-importEntitlementKey");
	protected File importBundledCertificateFile = null;
	protected List <ProductSubscription> productSubscriptions = null;
	
	
	
	// Protected methods ***********************************************************************

	protected void attemptAnEntitlementImportFromAnInvalidFile_Test(File invalidCertificate) {
		
		// once imported, what should the entitlement cert file be?
		File expectedEntitlementCertFile = new File (clienttasks.entitlementCertDir+"/"+invalidCertificate.getName());
		File expectedEntitlementKeyFile = clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(expectedEntitlementCertFile);
		
		// make sure the expected entitlement files do not exist before our test
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementCertFile.getPath()),0,"Before attempting the import, asserting that expected destination for the entitlement cert file does NOT yet exist ("+expectedEntitlementCertFile+").");
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementKeyFile.getPath()),0,"Before attempting the import, asserting that expected destination for the entitlement key file does NOT yet exist ("+expectedEntitlementKeyFile+").");
		
		// show the contents of the file about to be imported
		log.info("Following is the contents of the certificate file about to be imported...");
		client.runCommandAndWait("cat "+invalidCertificate);
		
		// attempt an entitlement cert import from a file containing only a key (negative test)
		SSHCommandResult importResult = clienttasks.importCertificate_(invalidCertificate.getPath());
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=734533 - jsefler 08/30/2011
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="734533"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla bug "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			Assert.assertEquals(importResult.getExitCode(), Integer.valueOf(0));
			Assert.assertEquals(importResult.getStdout().trim(), invalidCertificate.getName()+" is not a valid certificate file. Please use a valid certificate.");

		} else {
		// END OF WORKAROUND
		
		// assert the negative results
		Assert.assertEquals(importResult.getExitCode(), Integer.valueOf(255), "The exit code from the import command indicates a failure.");
		
		// {0} is not a valid certificate file. Please use a valid certificate.
		Assert.assertEquals(importResult.getStderr().trim(), invalidCertificate.getName()+" is not a valid certificate file. Please use a valid certificate.");
		}
		
		// verify that the expectedEntitlementCertFile does NOT exist
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementCertFile.getPath()),0,"After attempting the import, the expected destination for the entitlement cert file should NOT exist ("+expectedEntitlementCertFile+") since there was no entitlement in the import file.");

		// verify that the expectedEntitlementKeyFile does NOT exist
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, expectedEntitlementKeyFile.getPath()),0,"After attempting the import, the expected destination for the entitlement key file should NOT exist ("+expectedEntitlementKeyFile+") since there was no key in the import file.");
	}
	
	
	
	// Configuration methods ***********************************************************************
	
	@BeforeMethod(groups={"setup"})
	public void removeAllEntitlementCertsBeforeEachTest() {
		clienttasks.removeAllCerts(false, true);
	}
	
	@BeforeClass(groups={"setup"})
	public void setupImportFilesBeforeClass() {
		
		// restart rhsmcertd on a longer certFrequency
		clienttasks.restart_rhsmcertd(240, false);
		
		// register
		//clienttasks.unregister(null,null,null);	// avoid Bug 733525 - [Errno 2] No such file or directory: '/etc/pki/entitlement'
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, nullString, true, null, null, null);

		// subscribe to a random pool (so as to get a valid entitlement cert and its key)
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("Cannot randomly pick a pool for subscribing when there are no available pools for testing."); 
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPoolUsingPoolId(pool);
		//File entitlementKeyFile = clienttasks.getEntitlementCertKeyFileFromEntitlementCert(clienttasks.getEntitlementCertFromEntitlementCertFile((entitlementCertFile)));
		File entitlementKeyFile = clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(entitlementCertFile);

		// store the entitlement cert
		RemoteFileTasks.runCommandAndAssert(client,"cp -f "+entitlementCertFile+" "+importEntitlementCertFile,Integer.valueOf(0));

		// store the entitlement cert key
		RemoteFileTasks.runCommandAndAssert(client,"cp -f "+entitlementKeyFile+" "+importEntitlementKeyFile,Integer.valueOf(0));

		// create a valid bundled certificate (by the same name as the original entitlement cert) see bug https://bugzilla.redhat.com/show_bug.cgi?id=734606
		importBundledCertificateFile = new File("/tmp/"+entitlementCertFile.getName());
		client.runCommandAndWait("cat "+entitlementCertFile+" "+entitlementKeyFile+" > "+importBundledCertificateFile);

		// store the consumed ProductSubscriptions
		productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		
		// delete the entitlement cert and key from /etc/pki/entitlement
		clienttasks.removeAllCerts(false, true);
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, entitlementCertFile.getPath()),0,"Entitlement Cert file "+entitlementCertFile+" has been removed.");
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, entitlementKeyFile.getPath()),0,"Entitlement Key file "+entitlementCertFile+" has been removed.");
	}
	
	
	// Data Providers ***********************************************************************

}
