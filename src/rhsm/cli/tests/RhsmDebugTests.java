package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"RhsmDebugTests"})
public class RhsmDebugTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************
	
	@Test(	description="attempt to call rhsm-debug system without being registered",
			groups={},
			enabled=false)	// already covered by a dataProvider row in GeneralTests.getNegativeFunctionalityDataAsListOfLists()
	//@ImplementsNitrateTest(caseId=)
	public void RhsmDebugSystemWithoutBeingRegistered_Test() {
		
		// make sure we are not registered
		clienttasks.unregister_(null, null, null);
		
		// attempt to run rhsm-debug system without being registered
		String rhsmDebugSystemCommand = clienttasks.rhsmDebugSystemCommand(null, null, null, null, null);
		SSHCommandResult result = client.runCommandAndWait(rhsmDebugSystemCommand);
		
		// assert results
		Assert.assertEquals(result.getExitCode(), new Integer(255), "The exit code from an attempt to run '"+rhsmDebugSystemCommand+"' without being registered.");
		Assert.assertEquals(result.getStdout().trim(), "This system is not yet registered. Try 'subscription-manager register --help' for more information.", "The stdout from an attempt to run '"+rhsmDebugSystemCommand+"' without being registered.");
		Assert.assertEquals(result.getStderr().trim(), "", "The stderr from an attempt to run '"+rhsmDebugSystemCommand+"' without being registered.");
	}
	
	
	@Test(	description="after registering and subscribing, call rhsm-debug system and assert the expected contents of the written debug file",
			groups={"blockedByBug-1040338"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void RhsmDebugSystem_Test() {
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, false, null, null, null);
		
		// attach some random entitlements
		List<String> poolIds = new ArrayList<String>(); 
		List<SubscriptionPool> subscriptionPools = getRandomSubsetOfList(clienttasks.getCurrentlyAvailableSubscriptionPools(), 3);	// 3 is sufficient
		for (SubscriptionPool subscriptionPool : subscriptionPools) poolIds.add(subscriptionPool.poolId);
		clienttasks.subscribe(null, null, poolIds, null, null, null, null, null, null, null, null);

		// run rhsm-debug system
		String rhsmDebugSystemCommand = clienttasks.rhsmDebugSystemCommand(null, null, null, null, null);
		SSHCommandResult result = client.runCommandAndWait(rhsmDebugSystemCommand);
		
		// assert results
		Assert.assertEquals(result.getExitCode(), new Integer(0), "The exit code from an attempt to run '"+rhsmDebugSystemCommand+"'.");
		Assert.assertEquals(result.getStderr().trim(), "", "The stderr from an attempt to run '"+rhsmDebugSystemCommand+"'.");
		Assert.assertTrue(result.getStdout().trim().startsWith("Wrote:"), "The stdout from an attempt to run '"+rhsmDebugSystemCommand+"' should indicate the file that it \"Wrote:\".");
		
		// get the rhsmDebugFile from stdout
		File rhsmDebugSystemFile = new File(result.getStdout().split(":")[1].trim());	// Wrote: /tmp/rhsm-debug-system-20140115-457636.tar.gz
		
		// assert the existence of the rhsmDebugFile
		Assert.assertTrue(RemoteFileTasks.testExists(client, rhsmDebugSystemFile.getPath()), "The newly written rhsm debug file '"+rhsmDebugSystemFile+"' exists.");
		
		// untar the rhsmDebugFile
		String untarDir = "/tmp/rhsmDebugSystemTestDir";
		client.runCommandAndWait("rm -rf "+untarDir+" && mkdir -p "+untarDir);
		SSHCommandResult rhsmDebugSystemFileUntarResult = client.runCommandAndWait("tar --verbose --extract --directory="+untarDir+" --file="+rhsmDebugSystemFile);
		Assert.assertEquals(rhsmDebugSystemFileUntarResult.getExitCode(), new Integer(0), "The exit code from extracting '"+rhsmDebugSystemFile+"'.");
		String rhsmDebugSystemDir = rhsmDebugSystemFileUntarResult.getStdout().trim().split("\n")[0].replaceFirst("/$", ""); // rhsm-debug-system-20140115-968966

		// assert the presence of expected files...
		List<String> expectedFiles = new ArrayList<String>();
		//	[root@jsefler-7 ~]# tar --verbose --extract --directory=/tmp --file=/tmp/rhsm-debug-system-20140115-968966.tar.gz
		//	rhsm-debug-system-20140115-968966/
		//	rhsm-debug-system-20140115-968966/compliance.json
		//	rhsm-debug-system-20140115-968966/consumer.json
		//	rhsm-debug-system-20140115-968966/entitlements.json
		//	rhsm-debug-system-20140115-968966/pools.json
		//	rhsm-debug-system-20140115-968966/subscriptions.json
		//	rhsm-debug-system-20140115-968966/version.json
		//	rhsm-debug-system-20140115-968966/etc/
		//	rhsm-debug-system-20140115-968966/etc/pki/
		//	rhsm-debug-system-20140115-968966/etc/pki/consumer/
		//	rhsm-debug-system-20140115-968966/etc/pki/consumer/cert.pem
		//	rhsm-debug-system-20140115-968966/etc/pki/consumer/key.pem
		//	rhsm-debug-system-20140115-968966/etc/pki/entitlement/
		//	rhsm-debug-system-20140115-968966/etc/pki/entitlement/1712352679740801257-key.pem
		//	rhsm-debug-system-20140115-968966/etc/pki/entitlement/1712352679740801257.pem
		//	rhsm-debug-system-20140115-968966/etc/pki/product/
		//	rhsm-debug-system-20140115-968966/etc/pki/product/69.pem
		//	rhsm-debug-system-20140115-968966/etc/rhsm/
		//	rhsm-debug-system-20140115-968966/etc/rhsm/ca/
		//	rhsm-debug-system-20140115-968966/etc/rhsm/ca/candlepin-stage.pem
		//	rhsm-debug-system-20140115-968966/etc/rhsm/ca/jsefler-f14-7candlepin.pem
		//	rhsm-debug-system-20140115-968966/etc/rhsm/ca/jsefler-f14-candlepin.pem
		//	rhsm-debug-system-20140115-968966/etc/rhsm/ca/redhat-uep.pem
		//	rhsm-debug-system-20140115-968966/etc/rhsm/facts/
		//	rhsm-debug-system-20140115-968966/etc/rhsm/pluginconf.d/
		//	rhsm-debug-system-20140115-968966/etc/rhsm/pluginconf.d/all_slots_test.AllSlotsTestPlugin.conf
		//	rhsm-debug-system-20140115-968966/etc/rhsm/pluginconf.d/auto_attach_test.AutoAttachTestPlugin.conf
		//	rhsm-debug-system-20140115-968966/etc/rhsm/pluginconf.d/dbus_event.DbusEventPlugin.conf
		//	rhsm-debug-system-20140115-968966/etc/rhsm/pluginconf.d/facts_collection_test.FactsCollectionTestPlugin.conf
		//	rhsm-debug-system-20140115-968966/etc/rhsm/pluginconf.d/product_id_install_test.ProductIdInstallTestPlugin.conf
		//	rhsm-debug-system-20140115-968966/etc/rhsm/pluginconf.d/register_consumer_test1.RegisterConsumerTestPlugin.conf
		//	rhsm-debug-system-20140115-968966/etc/rhsm/pluginconf.d/register_consumer_test2.RegisterConsumerTestPlugin.conf
		//	rhsm-debug-system-20140115-968966/etc/rhsm/pluginconf.d/subscribe_test.SubscribeTestPlugin.conf
		//	rhsm-debug-system-20140115-968966/etc/rhsm/rhsm.conf
		//	rhsm-debug-system-20140115-968966/etc/rhsm/rhsm.conf.rpmsave
		//	rhsm-debug-system-20140115-968966/var/
		//	rhsm-debug-system-20140115-968966/var/lib/
		//	rhsm-debug-system-20140115-968966/var/lib/rhsm/
		//	rhsm-debug-system-20140115-968966/var/lib/rhsm/branded_name
		//	rhsm-debug-system-20140115-968966/var/lib/rhsm/cache/
		//	rhsm-debug-system-20140115-968966/var/lib/rhsm/cache/content_overrides.json
		//	rhsm-debug-system-20140115-968966/var/lib/rhsm/cache/entitlement_status.json
		//	rhsm-debug-system-20140115-968966/var/lib/rhsm/cache/installed_products.json
		//	rhsm-debug-system-20140115-968966/var/lib/rhsm/cache/product_status.json
		//	rhsm-debug-system-20140115-968966/var/lib/rhsm/facts/
		//	rhsm-debug-system-20140115-968966/var/lib/rhsm/facts/facts.json
		//	rhsm-debug-system-20140115-968966/var/lib/rhsm/packages/
		//	rhsm-debug-system-20140115-968966/var/lib/rhsm/productid.js
		//	rhsm-debug-system-20140115-968966/var/log/
		//	rhsm-debug-system-20140115-968966/var/log/rhsm/
		//	rhsm-debug-system-20140115-968966/var/log/rhsm/.rhsm.log.swp
		//	rhsm-debug-system-20140115-968966/var/log/rhsm/rhsm.log
		//	rhsm-debug-system-20140115-968966/var/log/rhsm/rhsm.log-20131223
		//	rhsm-debug-system-20140115-968966/var/log/rhsm/rhsm.log.1
		//	rhsm-debug-system-20140115-968966/var/log/rhsm/rhsm.log.2
		//	rhsm-debug-system-20140115-968966/var/log/rhsm/rhsm.log.3
		//	rhsm-debug-system-20140115-968966/var/log/rhsm/rhsm.log.4
		//	rhsm-debug-system-20140115-968966/var/log/rhsm/rhsm.log.5
		//	rhsm-debug-system-20140115-968966/var/log/rhsm/rhsmcertd.log
		//	rhsm-debug-system-20140115-968966/var/log/rhsm/rhsmcertd.log-20131223
		
		// current candlepin files
		expectedFiles.add("/compliance.json");
		expectedFiles.add("/consumer.json");
		expectedFiles.add("/entitlements.json");
		expectedFiles.add("/pools.json");
		expectedFiles.add("/subscriptions.json");
		expectedFiles.add("/version.json");
		
		// current /etc/rhsm files
		for (String expectedFile : client.runCommandAndWait("find /etc/rhsm").getStdout().trim().split("\n")) {
			expectedFiles.add(expectedFile);
		}
		
		// current /var/lib/rhsm files
		for (String expectedFile : client.runCommandAndWait("find /var/lib/rhsm").getStdout().trim().split("\n")) {
			expectedFiles.add(expectedFile);
		}
		
		// current /var/log/rhsm files
		for (String expectedFile : client.runCommandAndWait("find /var/log/rhsm").getStdout().trim().split("\n")) {
			expectedFiles.add(expectedFile);
		}
		
		// current consumer cert files
		String consumerCertDir = clienttasks.getConfParameter("consumerCertDir");
		expectedFiles.add(consumerCertDir+"/key.pem");
		expectedFiles.add(consumerCertDir+"/cert.pem");
		
		// current entitlement cert files
		String entitlementCertDir = clienttasks.getConfParameter("entitlementCertDir");
		for (File entitlementCertFile : clienttasks.getCurrentEntitlementCertFiles()) {
			expectedFiles.add(entitlementCertFile.getPath());
			expectedFiles.add(clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(entitlementCertFile).getPath());
		}
		
		// current product cert files
		String productCertDir = clienttasks.getConfParameter("productCertDir");
		for (File productCertFile : clienttasks.getCurrentProductCertFiles()) {
			expectedFiles.add(productCertFile.getPath());
		}
		
		// assert the presence of expected files... (within the verbose output from tar -xvf)
		for (String expectedFile : expectedFiles) {
			Assert.assertTrue(rhsmDebugSystemFileUntarResult.getStdout().contains(expectedFile), "Explosion of '"+rhsmDebugSystemFile+"' appears to contain expected file '"+expectedFile+"'.");
		}
		
		// assert the presence of expected files... (from find untarDir)
		String findCommand = "find "+untarDir;
		SSHCommandResult findResult = client.runCommandAndWait(findCommand);
		for (String expectedFile : expectedFiles) {
			Assert.assertTrue(findResult.getStdout().contains(expectedFile), "'"+findCommand+"' reports expected file '"+expectedFile+"'.");
		}
		
		// check for any unexpected files included in the tar file
		String rhsmDebugTarListing = rhsmDebugSystemFileUntarResult.getStdout();
		for (String expectedFile : expectedFiles) {
			rhsmDebugTarListing = rhsmDebugTarListing.replaceFirst(rhsmDebugSystemDir+expectedFile+"/?\\n", "");
		}
		rhsmDebugTarListing = rhsmDebugTarListing.replaceFirst(rhsmDebugSystemDir+"/?\\n", "");
		rhsmDebugTarListing = rhsmDebugTarListing.replaceFirst(rhsmDebugSystemDir+"/etc/?\\n", "");
		rhsmDebugTarListing = rhsmDebugTarListing.replaceFirst(rhsmDebugSystemDir+"/etc/pki/?\\n", "");
		rhsmDebugTarListing = rhsmDebugTarListing.replaceFirst(rhsmDebugSystemDir+productCertDir+"/?\\n", "");
		rhsmDebugTarListing = rhsmDebugTarListing.replaceFirst(rhsmDebugSystemDir+entitlementCertDir+"/?\\n", "");
		rhsmDebugTarListing = rhsmDebugTarListing.replaceFirst(rhsmDebugSystemDir+consumerCertDir+"/?\\n", "");
		rhsmDebugTarListing = rhsmDebugTarListing.replaceFirst(rhsmDebugSystemDir+"/var/?\\n", "");
		rhsmDebugTarListing = rhsmDebugTarListing.replaceFirst(rhsmDebugSystemDir+"/var/log/?\\n", "");
		rhsmDebugTarListing = rhsmDebugTarListing.replaceFirst(rhsmDebugSystemDir+"/var/lib/?\\n", "");
		rhsmDebugTarListing = rhsmDebugTarListing.trim();
		if (!rhsmDebugTarListing.isEmpty()) Assert.fail("Found the following unexpected files included in the rhsm-debug file '"+rhsmDebugSystemFile+"' :\n"+rhsmDebugTarListing);
	}
	
	
	
	// Candidates for an automated Test:
	
	
	
	// Configuration methods ***********************************************************************
	
	
	
	// Protected methods ***********************************************************************
	
	
	
	// Data Providers ***********************************************************************
	
	
	
}
