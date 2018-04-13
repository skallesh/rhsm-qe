package rhsm.cli.tests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;

import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ProductCert;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.LinkedItem;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;

/**
 * @author jsefler
 *
 *	Migrating Systems Between Subscription Services (from the Customer Portal):
 *		https://access.redhat.com/documentation/en-US/Red_Hat_Subscription_Management/1/html/MigratingRHN/index.html
 *
 *	References:
 *		http://documentation-stage.bne.redhat.com/docs/en-US/Red_Hat_Enterprise_Linux/5/html/Deployment_Guide/rhn-migration.html
 *		https://engineering.redhat.com/trac/PBUPM/browser/trunk/documents/Releases/RHEL6/Variants/RHEL6-Variants.rst
 *		http://linuxczar.net/articles/rhel-installation-numbers
 *		https://docspace.corp.redhat.com/docs/DOC-71135 (PRODUCT CERTS)
 *		https://engineering.redhat.com/trac/rcm/wiki/Projects/CDNBaseline
 *
 *	// OLD LOCATION
 *	git clone git://git.app.eng.bos.redhat.com/rcm/rhn-definitions.git
 *  http://git.app.eng.bos.redhat.com/?p=rcm/rhn-definitions.git;a=tree
 *  
 *  git clone git://git.app.eng.bos.redhat.com/rcm/rcm-metadata.git
 *  http://git.app.eng.bos.redhat.com/?p=rcm/rcm-metadata.git;a=tree
 *  
 *  product 150 is at
 *  http://git.app.eng.bos.redhat.com/?p=rcm/rhn-definitions.git;a=tree;f=product_ids/rhev-3.0;hb=HEAD
 *
 *	Connecting to a Satellite (instead of rhn hosted)
 *	1. Set automation parameters:
 *		sm.rhn.hostname : https://sat-56-server.usersys.redhat.com
 *		sm.rhn.username : admin
 *		sm.rhn.password : *****
 *  2. Use firefox to login to the Satellite account
 *      https://sat-56-server.usersys.redhat.com/rhn/Login.do
 *      do whatever work you need to there
 *  3. Get the CA cert from Satellite and install it onto your client
 *      wget --no-verbose --no-check-certificate --output-document=/usr/share/rhn/RHN-ORG-TRUSTED-SSL-CERT_sat-56-server.usersys.redhat.com https://sat-56-server.usersys.redhat.com/pub/RHN-ORG-TRUSTED-SSL-CERT
 *  4. Update the /etc/sysconfig/rhn/up2date with
 *      sslCACert=RHN-ORG-TRUSTED-SSL-CERT_sat-56-server.usersys.redhat.com
 *      
 *      
 *  When rhnreg_ks fails with: All available subscriptions for the requested channel have been exhausted.
 *  
 *  Getting more RHN subscriptions:
 *  1. Log into https://access.redhat.com as qa@redhat.com
 *  2. Navigate to Subscription Management / Subscription Management Applications / Satellite / rhsm-sat5
 *  3. Attach more subscriptions (Red Hat Employee Subscription)
 *  4. click Download Satellite Certificate and scp the downloaded rhsm-sat5.xml file to the rhsm-sat5.usersys.redhat.com server
 *  5. [root@rhsm-sat5 ~]# rhn-satellite-activate --rhn-cert rhsm-sat5.xml
 *  6. to see the new entitlements, open https://rhsm-sat5.usersys.redhat.com/rhn/channels/software/Entitlements.do
 *     search for the Channel Entitlement by the name of the channel (e.g. Red Hat Enterprise Linux Server (v. 7))
 *     see the Regular Available and Available Flex Guests to get a count
 */
@Test(groups={"MigrationTests"})
public class MigrationTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	
	
	// install-num-migrate-to-rhsm Test methods ***********************************************************************
	
	@TestDefinition(projectID={/*Project.RHEL5*/},testCaseID={})
	@Test(	description="Execute migration tool install-num-migrate-to-rhsm with a known instnumber and assert the expected productCerts are copied",
			groups={"Tier1Tests","InstallNumMigrateToRhsmWithInstNumber_Test","blockedByBug-853187"},
			dependsOnMethods={},
			dataProvider="InstallNumMigrateToRhsmData",
			enabled=true)
	@ImplementsNitrateTest(caseId=131567)
	//@ImplementsNitrateTest(caseId=130760)
	//@ImplementsNitrateTest(caseId=130758)
	public void testInstallNumMigrateToRhsmWithInstNumber(Object bugzilla, String instNumber) throws JSONException {
		InstallNumMigrateToRhsmWithInstNumber(instNumber);
	}
	protected SSHCommandResult InstallNumMigrateToRhsmWithInstNumber(String instNumber) throws JSONException {
		if (!clienttasks.redhatReleaseX.equals("5")) throw new SkipException("This test is applicable to RHEL5 only.");
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">", "1.11.3-4") && clienttasks.redhatReleaseX.equals("5")) {
			throw new SkipException("Due to bug 1092754, the migration tool '"+installNumTool+"' has been removed from RHEL5.");
		}
		String command;
		SSHCommandResult result;
		
		// deleting the currently installed product certs
		clienttasks.removeAllCerts(false, false, true);
		clienttasks.removeAllFacts();
		
		// get the product cert filenames that we should expect install-num-migrate-to-rhsm to copy
		List<String> expectedMigrationProductCertFilenames = getExpectedMappedProductCertFilenamesCorrespondingToInstnumberUsingInstnumTool(instNumber);

		// test --dryrun --instnumber ................................................
		log.info("Testing with the dryrun option...");
		command = installNumTool+" --dryrun --instnumber="+instNumber;
		result = RemoteFileTasks.runCommandAndAssert(client,command,0);
		//[root@jsefler-onprem-5server ~]# install-num-migrate-to-rhsm --dryrun --instnumber 0000000e0017fc01
		//Copying /usr/share/rhsm/product/RHEL-5/Client-Workstation-x86_64-f812997e0eda-71.pem to /etc/pki/product/71.pem
		//Copying /usr/share/rhsm/product/RHEL-5/Client-Client-x86_64-6587edcf1c03-68.pem to /etc/pki/product/68.pem

		// assert the dryrun
		for (String expectedMigrationProductCertFilename : expectedMigrationProductCertFilenames) {
			String pemFilename = MigrationDataTests.getPemFileNameFromProductCertFilename(expectedMigrationProductCertFilename);
			//String expectedStdoutString = "Copying "+baseProductsDir+"/"+expectedMigrationProductCertFilename+" to "+clienttasks.productCertDir+"/"+pemFilename;	// valid prior to Bug 853187 - String Update: install-num-migrate-to-rhsm output
			String expectedStdoutString = "Installing "+baseProductsDir+"/"+expectedMigrationProductCertFilename+" to "+clienttasks.productCertDir+"/"+pemFilename;
			Assert.assertTrue(result.getStdout().contains(expectedStdoutString),"The dryrun output from "+installNumTool+" contains the expected message: "+expectedStdoutString);
		}
		int numProductCertFilenamesToBeCopied=0;
		//for (int fromIndex=0; result.getStdout().indexOf("Copying", fromIndex)>=0&&fromIndex>-1; fromIndex=result.getStdout().indexOf("Copying", fromIndex+1)) numProductCertFilenamesToBeCopied++;	// valid prior to Bug 853187 - String Update: install-num-migrate-to-rhsm output
		for (int fromIndex=0; result.getStdout().indexOf("Installing", fromIndex)>=0&&fromIndex>-1; fromIndex=result.getStdout().indexOf("Installing", fromIndex+1)) numProductCertFilenamesToBeCopied++;	
		Assert.assertEquals(numProductCertFilenamesToBeCopied, expectedMigrationProductCertFilenames.size(),"The number of product certs to be copied.");
		Assert.assertEquals(clienttasks.getCurrentlyInstalledProducts().size(), 0, "A dryrun should NOT install any product certs.");
		Map<String,String> factMap = clienttasks.getFacts();
		
//		// TEMPORARY WORKAROUND FOR BUG
//		String bugId = "840415"; boolean invokeWorkaroundWhileBugIsOpen = true;
//		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
//		if (invokeWorkaroundWhileBugIsOpen) {
//			if (clienttasks.productCertDir.equals(nonDefaultProductCertDir))
//			log.warning("Skipping the removal of the non default productCertDir '"+nonDefaultProductCertDir+"' before Testing without the dryrun option...");
//		} else
//		// END OF WORKAROUND
// TODO This test path is not yet complete - depends on the outcome of bug 840415
		// when testing with the non-default productCertDir, make sure it does not exist (the list --installed call above will create it as a side affect)
		// Note: this if block help reveal bug 840415 - Install-num migration throws traceback for invalid product cert location.
		if (clienttasks.productCertDir.equals(nonDefaultProductCertDir)) {
			client.runCommandAndWait("rm -rf "+clienttasks.productCertDir);
			Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.productCertDir),"The configured rhsm.productCertDir does not exist.");
		}
		
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "783278"; boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		String bugPkg = "subscription-manager-migration";
		String bugVer = "subscription-manager-migration-0.98";	// RHEL58
		try {if (clienttasks.installedPackageVersionMap.get(bugPkg).contains(bugVer) && !invokeWorkaroundWhileBugIsOpen) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+" which has NOT been fixed in this installed version of "+bugVer+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId); invokeWorkaroundWhileBugIsOpen=true;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping the assertion of the fact '"+migrationFromFact+"' fact.");
		} else
		// END OF WORKAROUND
		Assert.assertNull(factMap.get(migrationFromFact), "The migration fact '"+migrationFromFact+"' should NOT be set after running command: "+command);
		Assert.assertNull(factMap.get(migrationSystemIdFact), "The migration fact '"+migrationSystemIdFact+"' should NOT be set after running command: "+command);
		Assert.assertNull(factMap.get(migrationDateFact), "The migration fact '"+migrationDateFact+"' should NOT be set after running command: "+command);
		
		// test --instnumber ................................................
		log.info("Testing without the dryrun option...");
		command = installNumTool+" --instnumber="+instNumber;
		result = RemoteFileTasks.runCommandAndAssert(client,command,0);
		//[root@jsefler-onprem-5server ~]# install-num-migrate-to-rhsm --instnumber 0000000e0017fc01
		//Copying /usr/share/rhsm/product/RHEL-5/Client-Workstation-x86_64-f812997e0eda-71.pem to /etc/pki/product/71.pem
		//Copying /usr/share/rhsm/product/RHEL-5/Client-Client-x86_64-6587edcf1c03-68.pem to /etc/pki/product/68.pem
		List<ProductCert> migratedProductCerts = clienttasks.getCurrentProductCerts();
		Assert.assertEquals(clienttasks.getCurrentlyInstalledProducts().size(), expectedMigrationProductCertFilenames.size(), "The number of productCerts installed after running migration command: "+command);
		for (String expectedMigrationProductCertFilename : expectedMigrationProductCertFilenames) {
			ProductCert expectedMigrationProductCert = clienttasks.getProductCertFromProductCertFile(new File(baseProductsDir+"/"+expectedMigrationProductCertFilename));
			Assert.assertTrue(migratedProductCerts.contains(expectedMigrationProductCert),"The newly installed product certs includes the expected migration productCert: "+expectedMigrationProductCert);
		}
		//	[root@jsefler-rhel59 ~]# subscription-manager facts --list | grep migration
		//	migration.install_number: 0000000e0017fc01
		//	migration.migrated_from: install_number
		//	migration.migration_date: 2012-08-08T11:11:15.818782
		factMap = clienttasks.getFacts();
		Assert.assertEquals(factMap.get(migrationFromFact), "install_number", "The migration fact '"+migrationFromFact+"' should be set after running command: "+command);
		Assert.assertNull(factMap.get(migrationSystemIdFact), "The migration fact '"+migrationSystemIdFact+"' should NOT be set after running command: "+command);
		Assert.assertNotNull(factMap.get(migrationDateFact), "The migration fact '"+migrationDateFact+"' should be set after running command: "+command);
		
		// assert that the migrationDateFact was set within the last few seconds
		int tol = 60; // tolerance in seconds
		Calendar migrationDate = parseDateStringUsingDatePattern(factMap.get(migrationDateFact), "yyyy-MM-dd'T'HH:mm:ss", null);	// NOTE: The .SSS milliseconds was dropped from the date pattern because it was getting confused as seconds from the six digit value in migration.migration_date: 2012-08-08T11:11:15.818782
		long systemTimeInSeconds = Long.valueOf(client.runCommandAndWait("date +%s").getStdout().trim());	// seconds since 1970-01-01 00:00:00 UTC
		long migratTimeInSeconds = migrationDate.getTimeInMillis()/1000;
		Assert.assertTrue(systemTimeInSeconds-tol < migratTimeInSeconds && migratTimeInSeconds < systemTimeInSeconds+tol, "The migration date fact '"+factMap.get(migrationDateFact)+"' was set within the last '"+tol+"' seconds.  Actual diff='"+String.valueOf(systemTimeInSeconds-migratTimeInSeconds)+"' seconds.");
		
		return result;
	}


	@TestDefinition(projectID={/*Project.RHEL5*/},testCaseID={})
	@Test(	description="Execute migration tool install-num-migrate-to-rhsm with install-num used to provision this machine",
			groups={"Tier1Tests","InstallNumMigrateToRhsm_Test","blockedByBug-854879"},
			dependsOnMethods={},
			enabled=true)
	@ImplementsNitrateTest(caseId=130760)
	public void testInstallNumMigrateToRhsm() throws JSONException {
		if (!clienttasks.redhatReleaseX.equals("5")) throw new SkipException("This test is applicable to RHEL5 only.");
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">", "1.11.3-4") && clienttasks.redhatReleaseX.equals("5")) {
			throw new SkipException("Due to bug 1092754, the migration tool '"+installNumTool+"' has been removed from RHEL5.");
		}
		if (!RemoteFileTasks.testExists(client, machineInstNumberFile) &&
			RemoteFileTasks.testExists(client, backupMachineInstNumberFile)	) {
			log.info("Restoring backup of the rhn install-num file...");
			client.runCommandAndWait("mv -f "+backupMachineInstNumberFile+" "+machineInstNumberFile);
		}
		if (!RemoteFileTasks.testExists(client, machineInstNumberFile)) throw new SkipException("This system was NOT provisioned with an install number.");
		
		// get the install number used to provision this machine
		SSHCommandResult result = client.runCommandAndWait("cat "+machineInstNumberFile);
		String installNumber = result.getStdout().trim();
		
		// test this install number explicitly (specifying --instnumber option)
		SSHCommandResult explicitResult = InstallNumMigrateToRhsmWithInstNumber(installNumber);
		
		// now test this install number implicitly (without specifying any options)
		clienttasks.removeAllCerts(false, false, true);
		clienttasks.removeAllFacts();
		SSHCommandResult implicitResult = client.runCommandAndWait(installNumTool);
		// compare implicit to explicit results for verification
		Assert.assertEquals(implicitResult.getStdout().trim(), explicitResult.getStdout().trim(), "Stdout from running :"+installNumTool);
		Assert.assertEquals(implicitResult.getStderr().trim(), explicitResult.getStderr().trim(), "Stderr from running :"+installNumTool);
		Assert.assertEquals(implicitResult.getExitCode(), explicitResult.getExitCode(), "ExitCode from running :"+installNumTool);
		Assert.assertEquals(clienttasks.getFactValue(migrationFromFact), "install_number", "The migration fact '"+migrationFromFact+"' should be set after running command: "+installNumTool);
		Assert.assertNull(clienttasks.getFactValue(migrationSystemIdFact), "The migration fact '"+migrationSystemIdFact+"' should NOT be set after running command: "+installNumTool);
		
		// assert that the migrated product certs provide (at least) the same product tags as originally installed with the install number
		List<ProductCert> migratedProductCerts = clienttasks.getCurrentProductCerts();
		log.info("The following productCerts were originally installed on this machine prior to this migration test:");
		for (ProductCert originalProductCert : originallyInstalledRedHatProductCerts) log.info(originalProductCert.toString());
		log.info("The following productCerts were migrated to the product install directory after running the migration test:");
		for (ProductCert migratedProductCert : migratedProductCerts) log.info(migratedProductCert.toString());
		log.info("Will now verify that all of the productTags from the originally installed productCerts are found among the providedTags of the migrated productCerts...");
		for (ProductCert originalProductCert : originallyInstalledRedHatProductCerts) {
			if (originalProductCert.productNamespace.providedTags==null) continue;
			List<String> originalProvidedTags = Arrays.asList(originalProductCert.productNamespace.providedTags.trim().split(" *, *"));
			for (ProductCert migratedProductCert : migratedProductCerts) {
				List<String> migratedProvidedTags = Arrays.asList(migratedProductCert.productNamespace.providedTags.trim().split(" *, *"));
				if (migratedProvidedTags.containsAll(originalProvidedTags)) {
					Assert.assertTrue(true,"This migrated productCert provides all the same tags as one of the originally installed product certs.\nMigrated productCert: "+migratedProductCert);
					originalProvidedTags = new ArrayList<String>();//originalProvidedTags.clear();
					break;
				}
			}
			if (!originalProvidedTags.isEmpty()) {
				Assert.fail("Failed to find the providedTags from the originally installed productCert among the migrated productCerts.\nOriginal productCert: "+originalProductCert);
			}
		}
	}
	
	
	@TestDefinition(projectID={/*Project.RHEL5*/},testCaseID={})
	@Test(	description="Execute migration tool install-num-migrate-to-rhsm with a non-default rhsm.productcertdir configured",
			groups={"Tier3Tests","blockedByBug-773707","blockedByBug-840415","InstallNumMigrateToRhsmWithNonDefaultProductCertDir_Test"},
			dependsOnMethods={},
			dataProvider="InstallNumMigrateToRhsmData",
			enabled=true)
	public void testInstallNumMigrateToRhsmWithNonDefaultProductCertDir(Object bugzilla, String instNumber) throws JSONException {
		if (!clienttasks.redhatReleaseX.equals("5")) throw new SkipException("This test is applicable to RHEL5 only.");
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">", "1.11.3-4") && clienttasks.redhatReleaseX.equals("5")) {
			throw new SkipException("Due to bug 1092754, the migration tool '"+installNumTool+"' has been removed from RHEL5.");
		}
		
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "773707"; boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		String bugPkg = "subscription-manager-migration";
		String bugVer = "subscription-manager-migration-0.98";	// RHEL58
		try {if (clienttasks.installedPackageVersionMap.get(bugPkg).contains(bugVer) && !invokeWorkaroundWhileBugIsOpen) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+" which has NOT been fixed in this installed version of "+bugVer+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId); invokeWorkaroundWhileBugIsOpen=true;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			throw new SkipException("There is no workaround for this installed version of "+bugVer+".  Blocked by Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
		}
		// END OF WORKAROUND
		
		//if (clienttasks.redhatReleaseXY.equals("5.9")) {
		//if (Arrays.asList("5.7","5.8","5.9").contains(clienttasks.redhatReleaseXY)) {
		if (Float.valueOf(clienttasks.redhatReleaseXY) <= 5.9f) {
			throw new SkipException("Blocking bugzilla 840415 was fixed in a subsequent release.  Skipping this test since we already know it will fail in RHEL release '"+clienttasks.redhatReleaseXY+"'.");
		}
		
		// NOTE: The configNonDefaultRhsmProductCertDir will handle the configuration setting
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "productCertDir"), nonDefaultProductCertDir,"A non-default rhsm.productCertDir has been configured.");
		testInstallNumMigrateToRhsmWithInstNumber(bugzilla,instNumber);
	}
	
	
	@TestDefinition(projectID={/*Project.RHEL5*/},testCaseID={})
	@Test(	description="Execute migration tool install-num-migrate-to-rhsm with a bad length install-num (expecting 16 chars long)",
			groups={"Tier3Tests"},
			dependsOnMethods={},
			dataProvider="InstallNumMigrateToRhsmWithInvalidInstNumberData",
			enabled=true)
	@ImplementsNitrateTest(caseId=130760)
	public void testInstallNumMigrateToRhsmWithInvalidInstNumber(Object bugzilla, String command, Integer expectedExitCode, String expectedStdout, String expectedStderr) {
		if (!clienttasks.redhatReleaseX.equals("5")) throw new SkipException("This test is applicable to RHEL5 only.");
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">", "1.11.3-4") && clienttasks.redhatReleaseX.equals("5")) {
			throw new SkipException("Due to bug 1092754, the migration tool '"+installNumTool+"' has been removed from RHEL5.");
		}
		
		SSHCommandResult result = client.runCommandAndWait(command);
		if (expectedStdout!=null) Assert.assertEquals(result.getStdout().trim(), expectedStdout, "Stdout from running :"+command);
		if (expectedStderr!=null) Assert.assertEquals(result.getStderr().trim(), expectedStderr, "Stderr from running :"+command);
		// TEMPORARY WORKAROUND FOR BUG
		String bugId="783542"; boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		String bugPkg = "subscription-manager-migration";
		String bugVer = "subscription-manager-migration-0.98";	// RHEL58
		try {if (clienttasks.installedPackageVersionMap.get(bugPkg).contains(bugVer) && !invokeWorkaroundWhileBugIsOpen) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+" which has NOT been fixed in this installed version of "+bugVer+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId); invokeWorkaroundWhileBugIsOpen=true;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping the exitCode assertion from running: "+command);
		} else
		// END OF WORKAROUND
		if (expectedExitCode!=null) Assert.assertEquals(result.getExitCode(), expectedExitCode, "ExitCode from running :"+command);
	}


	@TestDefinition(projectID={/*Project.RHEL5*/},testCaseID={})
	@Test(	description="Execute migration tool install-num-migrate-to-rhsm with no install-num found on machine",
			groups={"Tier3Tests"},
			dependsOnMethods={},
			enabled=true)
	public void testInstallNumMigrateToRhsmWithMissingInstNumber() {
		if (!clienttasks.redhatReleaseX.equals("5")) throw new SkipException("This test is applicable to RHEL5 only.");
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">", "1.11.3-4") && clienttasks.redhatReleaseX.equals("5")) {
			throw new SkipException("Due to bug 1092754, the migration tool '"+installNumTool+"' has been removed from RHEL5.");
		}
		
		if (RemoteFileTasks.testExists(client, machineInstNumberFile)) {
			log.info("Backing up the rhn install-num file...");
			client.runCommandAndWait("mv -f "+machineInstNumberFile+" "+backupMachineInstNumberFile);
		}
		testInstallNumMigrateToRhsmWithInvalidInstNumber(null, installNumTool,1,"Could not read installation number from "+machineInstNumberFile+".  Aborting.","");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21885", "RHEL7-51740"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.LOW, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Assert that install-num-migrate-to-rhsm is only installed on RHEL5",
			groups={"Tier3Tests","blockedByBug-790205","blockedByBug-1092754"},
			dependsOnMethods={},
			enabled=true)
	public void testInstallNumMigrateToRhsmShouldOnlyBeInstalledOnRHEL5() {
		// make sure subscription-manager-migration is installed on RHEL5
		SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(client, "rpm -ql "+clienttasks.command+"-migration", 0);
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">", "1.11.3-4") && clienttasks.redhatReleaseX.equals("5")) {
			log.warning("Due to bug 1092754, the migration tool '"+installNumTool+"' has been removed from RHEL5.");
			Assert.assertFalse(result.getStdout().contains(installNumTool), "Due to bug 1092754, the migration tool "+clienttasks.command+"-migration package should no longer provide '"+installNumTool+"' on RHEL5.");
			return;	// end of testing for install-num-migrate-to-rhsm tool
		}
		Assert.assertEquals(result.getStdout().contains(installNumTool), clienttasks.redhatReleaseX.equals("5"), "The "+clienttasks.command+"-migration package should only provide '"+installNumTool+"' on RHEL5.");
	}
	
	
	
	// rhn-migrate-classic-to-rhsm Test methods ***********************************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20111", "RHEL7-51115"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Register system using RHN Classic and then Execute migration tool rhn-migrate-classic-to-rhsm with options after adding RHN Channels",
			groups={"Tier1Tests","RhnMigrateClassicToRhsm_Test","blockedByBug-966745","blockedByBug-840169","blockedbyBug-878986","blockedByBug-1052297","blockedByBug-1111258","blockedByBug-1512948","blockedByBug-1516832"},
			dependsOnMethods={},
			dataProvider="RhnMigrateClassicToRhsmData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=130764,130762) // TODO some expected yum repo assertions are not yet automated
	public void testRhnMigrateClassicToRhsm(Object bugzilla, String rhnreg_ksUsername, String rhnreg_ksPassword, String rhnHostname, List<String> rhnChannelsToAdd, String options, String rhnUsername, String rhnPassword, String rhsmUsername, String rhsmPassword, String rhsmOrg, Integer serviceLevelIndex, String serviceLevelExpected) throws JSONException {
		if (sm_rhnHostname.equals("")) throw new SkipException("This test requires access to RHN Classic or Satellite 5.");
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7 && clienttasks.arch.equals("ppc64le")) throw new SkipException("Use of rhn-migrate-classic-to-rhsm is not necessary on RHEL '"+client1tasks.redhatReleaseX+"' arch '"+clienttasks.arch+"' since this product was not released on RHN Classic.");
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7 && clienttasks.arch.equals("aarch64")) throw new SkipException("Use of rhn-migrate-classic-to-rhsm is not necessary on RHEL '"+client1tasks.redhatReleaseX+"' arch '"+clienttasks.arch+"' since this product was not released on RHN Classic.");
		
		if (false) {	// TODO maybe this should go after the unregister and removeAll commands
		// make sure our serverUrl is configured to it's original good value
		restoreOriginallyConfiguredServerUrl();
		
		// make sure we are NOT registered to RHSM
		clienttasks.unregister(null,null,null, null);

		// deleting the currently installed product certs
		clienttasks.removeAllCerts(false, false, true);
		clienttasks.removeAllFacts();
		} else {	// TODO: 8/12/2013 Attempting the following logic in response to above TODO
		// make sure we are NOT registered to RHSM (and system is clean from prior test) ignoring errors like: 
		//	ssh root@cloud-qe-9.idm.lab.bos.redhat.com subscription-manager unregister
		//	Stdout: Runtime Error Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect): [org.candlepin.model.Pool#8a99f9823fc4919b013fc49408a302b7] at org.hibernate.persister.entity.AbstractEntityPersister.check:1,782
		//	Stderr:
		//	ExitCode: 255
		clienttasks.unregister_(null,null,null, null);
		clienttasks.removeAllCerts(true,true,true);
		clienttasks.removeAllFacts();
		restoreOriginallyConfiguredServerUrl();
		}
		
		// make sure that rhnplugin is enabled /etc/yum/pluginconf.d/rhnplugin.conf
		// NOT NECESSARY! enablement of rhnplugin.conf is done by rhnreg_ks
		
		// randomly remove a benign rhn classic package (just to add a little unnecessary fun)
		if (randomGenerator.nextBoolean() && clienttasks.isPackageInstalled("osad")) clienttasks.yumRemovePackage("osad");
		if (randomGenerator.nextBoolean() && clienttasks.isPackageInstalled("openscap")) clienttasks.yumRemovePackage("openscap");
		
		// register to RHN Classic
		String rhnSystemId = clienttasks.registerToRhnClassic(rhnreg_ksUsername, rhnreg_ksPassword, rhnHostname);
		Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(rhnreg_ksUsername, rhnreg_ksPassword, rhnHostname, rhnSystemId),"Confirmed that rhn systemId '"+rhnSystemId+"' is currently registered.");
		
		// subscribe to more RHN Classic channels
		if (rhnChannelsToAdd.size()>0) addRhnClassicChannels(rhnreg_ksUsername, rhnreg_ksPassword, rhnChannelsToAdd);
		
		// get a list of the consumed RHN Classic channels
		List<String> rhnChannelsConsumed = clienttasks.getCurrentRhnClassicChannels();
		if (rhnChannelsToAdd.size()>0) Assert.assertTrue(rhnChannelsConsumed.containsAll(rhnChannelsToAdd), "All of the RHN Classic channels added appear to be consumed.");
		
		// get a map of the productid.js file before we attempt migration
		Map<String,List<String>> productIdRepoMapBeforeMigration = clienttasks.getProductIdToReposMap();
		
		// get the product cert filenames that we should expect rhn-migrate-classic-to-rhsm to copy (or use the ones supplied to the @Test)
		Set<String> expectedMigrationProductCertFilenames = getExpectedMappedProductCertFilenamesCorrespondingToChannels(rhnChannelsConsumed);
		
		// screw up the currently configured [server]hostname:port/prefix when the command line options specify a new URL
		if (options.contains("--serverurl") || options.contains("--destination-url")) {
			log.info("Configuring a bad server hostname:port/prefix to test that the specified --serverurl can override it...");
			List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
			if (doesStringContainMatches(options, "(--serverurl|--destination-url)=(https://)?[\\w\\.-]+")) {	// hostname
				listOfSectionNameValues.add(new String[]{"server","hostname","bad-hostname.com"});
			}
			if (doesStringContainMatches(options, "(--serverurl|--destination-url)=(https://)?[\\w\\.-]+:\\d+")) {	// port
				listOfSectionNameValues.add(new String[]{"server","port","000"});
			}
			if (doesStringContainMatches(options, "(--serverurl|--destination-url)=(https://)?[\\w\\.-]+(:\\d+)?/\\w+")) {	// prefix
				listOfSectionNameValues.add(new String[]{"server","prefix","/bad-prefix"});
			}
			clienttasks.config(null, null, true, listOfSectionNameValues);
		}
		
		// execute rhn-migrate-classic-to-rhsm with options
		SSHCommandResult sshCommandResult = executeRhnMigrateClassicToRhsm(options,rhnUsername,rhnPassword,rhsmUsername,rhsmPassword,rhsmOrg,null, serviceLevelIndex);
		
		// assert valid usage
		if (sshCommandResult.getStdout().contains("Usage: rhn-migrate-classic-to-rhsms")) {
			Assert.fail("Detected a usage error.");
		}
		
		// get a map of the productid.js file after we attempt migration
		Map<String,List<String>> productIdRepoMapAfterMigration = clienttasks.getProductIdToReposMap();
		
		// assert the exit code
		checkForKnownBug881952(sshCommandResult);
		checkForKnownBug1130637(sshCommandResult);
		String expectedMsg;
		if (!getProductCertFilenamesContainingNonUniqueProductIds(expectedMigrationProductCertFilenames).isEmpty()) {
			log.warning("The RHN Classic channels currently consumed map to multiple product certs that share the same product ID "+getProductCertFilenamesContainingNonUniqueProductIds(expectedMigrationProductCertFilenames)+".  We must abort in this case.  Therefore, the "+rhnMigrateTool+" command should have exited with code 1.");
			// TEMPORARY WORKAROUND FOR BUG
			String bugId = "1006985"; // Bug 1006985 - rhn-migrate-classic-to-rhsm should abort when it encounters RHN channels that map to different products certs that share the same productId
			boolean invokeWorkaroundWhileBugIsOpen = true;
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("The remainder of this test is blocked by bug "+bugId+".  There is no workaround.");
			}
			// END OF WORKAROUND
			
			//	+-----------------------------------------------------+
			//	Unable to continue migration!
			//	+-----------------------------------------------------+
			//	You are subscribed to channels that have conflicting product certificates.
			//	The following channels map to product ID 69:
			//		rhel-x86_64-rhev-agent-6-server
			//		rhel-x86_64-rhev-agent-6-server-beta
			//		rhel-x86_64-rhev-agent-6-server-beta-debuginfo
			//		rhel-x86_64-rhev-agent-6-server-debuginfo
			//		rhel-x86_64-server-6
			//		rhel-x86_64-server-6-cf-tools-1
			//		rhel-x86_64-server-6-cf-tools-1-beta
			//		rhel-x86_64-server-6-cf-tools-1-beta-debuginfo
			//		rhel-x86_64-server-6-cf-tools-1-debuginfo
			//	Reduce the number of channels per product ID to 1 and run migration again.
			//	To remove a channel, use 'rhn-channel --remove --channel=<conflicting_channel>'.
			
			expectedMsg = "Unable to continue migration!"; // TODO Improve the expectedMsg to better assert the list of conflicting channels
			Assert.assertTrue(sshCommandResult.getStdout().contains(expectedMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+expectedMsg);	
			Assert.assertEquals(sshCommandResult.getExitCode(), new Integer(1), "ExitCode from call to '"+rhnMigrateTool+" "+options+"' when currently consumed RHN Classic channels map to multiple productCerts sharing the same productId.");
			Assert.assertTrue(clienttasks.isRhnSystemRegistered(),"This system is still registered using RHN Classic when currently consumed RHN Classic channels map to multiple productCerts sharing the same productId.");
			
			// assert that no product certs have been copied yet
//OLD		Assert.assertEquals(clienttasks.getCurrentlyInstalledProducts().size(), 0, "No productCerts have been migrated when "+rhnMigrateTool+" aborts because the currently consumed RHN Classic channels map to multiple productCerts sharing the same productId.");
			Assert.assertEquals(clienttasks.getProductCerts(clienttasks.productCertDir).size(), 0, "No productCerts have been migrated when "+rhnMigrateTool+" aborts because the currently consumed RHN Classic channels map to multiple productCerts sharing the same productId.");
			
			// assert that we are not yet registered to RHSM
			Assert.assertNull(clienttasks.getCurrentConsumerCert(),"We should NOT be registered to RHSM when "+rhnMigrateTool+" aborts because the currently consumed RHN Classic channels map to multiple productCerts sharing the same productId.");
			
			// assert that we are still registered to RHN
			Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(rhnreg_ksUsername, rhnreg_ksPassword, rhnHostname, rhnSystemId),"Confirmed that rhn systemId '"+rhnSystemId+"' is still registered since our migration attempt aborts because the currently consumed RHN Classic channels map to multiple productCerts sharing the same productId.");
			
			// assert that the rhnplugin is still enabled
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.yumPluginConfFileForRhn, "enabled"),"1","The enabled yum plugin configuration for RHN.");
			
			// assert that productid.js is unchanged
			Assert.assertTrue(productIdRepoMapBeforeMigration.keySet().containsAll(productIdRepoMapAfterMigration.keySet()) && productIdRepoMapAfterMigration.keySet().containsAll(productIdRepoMapBeforeMigration.keySet()),"The '"+clienttasks.productIdJsonFile+"' productIds remain unchanged when "+rhnMigrateTool+" aborts because the currently consumed RHN Classic channels map to multiple productCerts sharing the same productId.");
			for (String productId : productIdRepoMapBeforeMigration.keySet()) {
				Assert.assertTrue(productIdRepoMapBeforeMigration.get(productId).containsAll(productIdRepoMapAfterMigration.get(productId)) && productIdRepoMapAfterMigration.get(productId).containsAll(productIdRepoMapBeforeMigration.get(productId)), "The '"+clienttasks.productIdJsonFile+"' productIds repos for '"+productId+"' remain unchanged when "+rhnMigrateTool+" aborts because the currently consumed RHN Classic channels map to multiple productCerts sharing the same productId.");
			}
			
			return;
		} else
		if (!areAllChannelsMapped(rhnChannelsConsumed) && !options.contains("-f")/*--force*/) {	// when not all of the rhnChannelsConsumed have been mapped to a productCert and no --force has been specified.
			log.warning("Not all of the channels are mapped to a product cert.  Therefore, the "+rhnMigrateTool+" command should have exited with code 1.");
			expectedMsg = "Use --force to ignore these channels and continue the migration.";
			Assert.assertTrue(sshCommandResult.getStdout().contains(expectedMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+expectedMsg);	
			Assert.assertEquals(sshCommandResult.getExitCode(), new Integer(1), "ExitCode from call to '"+rhnMigrateTool+" "+options+"' when any of the channels are not mapped to a productCert.");
			Assert.assertTrue(clienttasks.isRhnSystemRegistered(),"This system is still registered using RHN Classic when rhn-migrate-classic-to-rhsm requires --force to continue.");
			
			// assert that no product certs have been copied yet
			Assert.assertEquals(clienttasks.getCurrentlyInstalledProducts().size(), 0, "No productCerts have been migrated when "+rhnMigrateTool+" requires --force to continue.");
			
			// assert that we are not yet registered to RHSM
			Assert.assertNull(clienttasks.getCurrentConsumerCert(),"We should NOT be registered to RHSM when "+rhnMigrateTool+" requires --force to continue.");
			
			// assert that we are still registered to RHN
			Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(rhnreg_ksUsername, rhnreg_ksPassword, rhnHostname, rhnSystemId),"Confirmed that rhn systemId '"+rhnSystemId+"' is still registered since our migration attempt requires --force to continue.");
			
			// assert that the rhnplugin is still enabled
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.yumPluginConfFileForRhn, "enabled"),"1","The enabled yum plugin configuration for RHN.");
			
			// assert that productid.js is unchanged
			Assert.assertTrue(productIdRepoMapBeforeMigration.keySet().containsAll(productIdRepoMapAfterMigration.keySet()) && productIdRepoMapAfterMigration.keySet().containsAll(productIdRepoMapBeforeMigration.keySet()),"The '"+clienttasks.productIdJsonFile+"' productIds remain unchanged when "+rhnMigrateTool+" requires --force to continue.");
			for (String productId : productIdRepoMapBeforeMigration.keySet()) {
				Assert.assertTrue(productIdRepoMapBeforeMigration.get(productId).containsAll(productIdRepoMapAfterMigration.get(productId)) && productIdRepoMapAfterMigration.get(productId).containsAll(productIdRepoMapBeforeMigration.get(productId)), "The '"+clienttasks.productIdJsonFile+"' productIds repos for '"+productId+"' remain unchanged when "+rhnMigrateTool+" requires --force to continue.");
			}
			
			return;
			
		} else
		if (rhnChannelsConsumed.isEmpty()) {
			log.warning("Modifying expected results when the current RHN Classically registered system is not consuming any RHN channels.");
			String expectedStdout = "Problem encountered getting the list of subscribed channels.  Exiting.";
			if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) expectedStdout = "Problem encountered getting the list of subscribed channels.  See /var/log/rhsm/rhsm.log for more details.";	// changed by commit c0f8052ec2b5b7b5c736eb626e381aef0e5327e5
			Assert.assertTrue(sshCommandResult.getStdout().trim().endsWith(expectedStdout), "The expected stdout result from call to '"+rhnMigrateTool+"' when no RHN Classic channels are being consumed: "+expectedStdout);
			//Assert.assertEquals(sshCommandResult.getExitCode(), new Integer(1), "The expected exit code from call to '"+rhnMigrateTool+"' when no RHN Classic channels are being consumed.");		// the exitCode can be altered by the expect script rhn-migrate-classic-to-rhsm.tcl when the final arg slaIndex is non-null; therefore don't bother asserting exitCode; asserting stdout is sufficient
			Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(rhnreg_ksUsername, rhnreg_ksPassword, rhnHostname, rhnSystemId),"Confirmed that rhn systemId '"+rhnSystemId+"' is still registered after '"+rhnMigrateTool+"' exits due to: "+expectedStdout);

			// assert that productid.js is unchanged
			Assert.assertTrue(productIdRepoMapBeforeMigration.keySet().containsAll(productIdRepoMapAfterMigration.keySet()) && productIdRepoMapAfterMigration.keySet().containsAll(productIdRepoMapBeforeMigration.keySet()),"The '"+clienttasks.productIdJsonFile+"' productIds remain unchanged when "+rhnMigrateTool+" exits due to: "+expectedStdout);
			for (String productId : productIdRepoMapBeforeMigration.keySet()) {
				Assert.assertTrue(productIdRepoMapBeforeMigration.get(productId).containsAll(productIdRepoMapAfterMigration.get(productId)) && productIdRepoMapAfterMigration.get(productId).containsAll(productIdRepoMapBeforeMigration.get(productId)), "The '"+clienttasks.productIdJsonFile+"' productIds repos for '"+productId+"' remain unchanged when "+rhnMigrateTool+" exits due to: "+expectedStdout);
			}
			
			return;
		}
		Assert.assertEquals(sshCommandResult.getExitCode(), new Integer(0), "ExitCode from call to '"+rhnMigrateTool+" "+options+"' when all of the channels are mapped.");
		
		// assert product certificates are copied
		expectedMsg = String.format("Product certificates copied successfully to %s !",	clienttasks.productCertDir);
		expectedMsg = String.format("Product certificates copied successfully to %s",	clienttasks.productCertDir);
		expectedMsg = String.format("Product certificates installed successfully to %s.",	clienttasks.productCertDir);	// Bug 852107 - String Update: rhn-migrate-classic-to-rhsm output
		Assert.assertTrue(sshCommandResult.getStdout().contains(expectedMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+expectedMsg);
		
		// assert that the expected product certs mapped from the consumed RHN Classic channels are now installed
//OLD	List<ProductCert> migratedProductCerts = clienttasks.getCurrentProductCerts();
//OLD	Assert.assertEquals(clienttasks.getCurrentlyInstalledProducts().size(), expectedMigrationProductCertFilenames.size(), "The number of productCerts installed after running "+rhnMigrateTool+" with "+options+".  (If this fails, one of these migration certs may have clobbered the other "+expectedMigrationProductCertFilenames+")");
		List<ProductCert> migratedProductCerts = clienttasks.getProductCerts(clienttasks.productCertDir);
		Assert.assertEquals(migratedProductCerts.size(), expectedMigrationProductCertFilenames.size(), "The number of productCerts in '"+clienttasks.productCertDir+"' after running "+rhnMigrateTool+" with options '"+options+"'.  (If this fails, one of these migration certs may have clobbered the other "+expectedMigrationProductCertFilenames+")");
		for (String expectedMigrationProductCertFilename : expectedMigrationProductCertFilenames) {
			ProductCert expectedMigrationProductCert = clienttasks.getProductCertFromProductCertFile(new File(baseProductsDir+"/"+expectedMigrationProductCertFilename));
			Assert.assertTrue(migratedProductCerts.contains(expectedMigrationProductCert),"The newly migrated product certs in '"+clienttasks.productCertDir+"' includes the expected migration productCert: "+expectedMigrationProductCert);
		}
		
		// assert the expected migration.* facts are set
		//	[root@ibm-x3620m3-01 ~]# subscription-manager facts --list | grep migration
		Map<String,String> factMap = clienttasks.getFacts();
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13")) {
			//	migration.classic_system_id: 1000021964
			//	migration.migrated_from: https://sat-56-server.usersys.redhat.com/XMLRPC
			//	migration.migration_date: 2015-02-07T18:42:18.744943
			
			//	migration.classic_system_id: 1033298347
			//	migration.migrated_from: https://xmlrpc.rhn.code.stage.redhat.com/XMLRPC
			//	migration.migration_date: 2015-02-24T19:52:14.685785
			
			String expectedMigrationFromFact = rhnHostname+"/XMLRPC";
			if (!expectedMigrationFromFact.startsWith("http")) expectedMigrationFromFact = "https://xmlrpc."+expectedMigrationFromFact;
			Assert.assertEquals(factMap.get(migrationFromFact), expectedMigrationFromFact, "The migration fact '"+migrationFromFact+"' should be set after running "+rhnMigrateTool+" with "+options+".");			
		} else {
			//	migration.classic_system_id: 1023061526
			//	migration.migrated_from: rhn_hosted_classic
			//	migration.migration_date: 2012-07-13T18:51:44.254543
			
			Assert.assertEquals(factMap.get(migrationFromFact), "rhn_hosted_classic", "The migration fact '"+migrationFromFact+"' should be set after running "+rhnMigrateTool+" with "+options+".");
		}
		Assert.assertEquals(factMap.get(migrationSystemIdFact), rhnSystemId, "The migration fact '"+migrationSystemIdFact+"' should be set after running "+rhnMigrateTool+" with "+options+".");
		Assert.assertNotNull(factMap.get(migrationDateFact), "The migration fact '"+migrationDateFact+"' should be set after running "+rhnMigrateTool+" with "+options+".");
		int tol = 300; // tolerance in seconds to assert that the migration_date facts was set within the last few seconds
		Calendar migrationDate;
/* 10-23-2015 DELETEME AFTER PROVING THAT migrationDateFactWithGMTOffsetTimeZone WORKS
//		if (client.runCommandAndWait("date +%Z").getStdout().trim().endsWith("DT")) {	// are we currently on daylight summer/savings time?
//			// determined on Mar 9, 2015 (day after Daylight Summer/Savings Time starts), when date +%Z returns EDT, passing passing null for timeZone will correctly pass the migrationDateFact assertion (Note: passing "EDT" will actually screw it up)
//			migrationDate = parseDateStringUsingDatePattern(factMap.get(migrationDateFact), "yyyy-MM-dd'T'HH:mm:ss", null);	// NOTE: The .SSS milliseconds was dropped from the date pattern because it was getting confused as seconds from the six digit value in migration.migration_date: 2012-08-08T11:11:15.818782
//		} else {
//			migrationDate = parseDateStringUsingDatePattern(factMap.get(migrationDateFact), "yyyy-MM-dd'T'HH:mm:ss", client.runCommandAndWait("date +%Z").getStdout().trim());	// NOTE: The .SSS milliseconds was dropped from the date pattern because it was getting confused as seconds from the six digit value in migration.migration_date: 2012-08-08T11:11:15.818782
//		}
*/
		// the time zone is absent from the value of the migration.migration_date fact (arguably an RFE bug), let's append the GMTOffsetTimeZone of the client
		//	[root@ibm-hs22-04 ~]# subscription-manager facts | grep migration_date
		//	migration.migration_date: 2015-10-24T00:01:54.040880
		//	[root@ibm-hs22-04 ~]# date -d "2015-10-24T00:01:54.040880" -Iseconds
		//	2015-10-24T00:01:54+0200
		//	[root@ibm-hs22-04 ~]# date +%Z
		//	CEST
		String migrationDateFactWithGMTOffsetTimeZone = client.runCommandAndWait("date -d \""+factMap.get(migrationDateFact)+"\" -Iseconds").getStdout().trim();
		String systemTimeZone = client.runCommandAndWait("date +%Z").getStdout().trim();
		String systemTimeZoneHHMM = client.runCommandAndWait("date +%:z").getStdout().trim();
		migrationDate = parseDateStringUsingDatePattern(migrationDateFactWithGMTOffsetTimeZone, "yyyy-MM-dd'T'HH:mm:ssZ", systemTimeZone);
		
		long systemTimeInSeconds = Long.valueOf(client.runCommandAndWait("date +%s").getStdout().trim());	// seconds since 1970-01-01 00:00:00 UTC (will be the same value no matter what TimeZone the system is in
		long migratTimeInSeconds = migrationDate.getTimeInMillis()/1000;
		// I do not have a bug number, but on RHEL5 and RHEL6 the migrationDateFactWithGMTOffsetTimeZone is slow as follows	// bug would be buried in rpm -qf `which date` => coreutils-5.97-34.el5_8.1  coreutils-8.4-37.el6
		// RHEL7 EST Timezone is good
		//	[root@jsefler-7 ~]# date -d "2015-01-01T00:00:00.000000" -Iseconds
		//	2015-01-01T00:00:00-0500
		// RHEL6 EST Timezone is 12 hours slow
		//	[root@jsefler-6 ~]# date -d "2015-01-01T00:00:00.000000" -Iseconds
		//	2014-12-31T12:00:00-0500
		// RHEL5 EST Timezone is 12 hours slow
		//	[root@jsefler-5 ~]# date -d "2015-01-01T00:00:00.000000" -Iseconds
		//	2014-12-31T12:00:00-0500
		
		// RHEL7 CET Timezone Time is good
		//	[root@dell-pe1950-05 ~]# date -d "2015-01-01T00:00:00.000000" -Iseconds
		//	2015-01-01T00:00:00+0100
		// RHEL6 CET Timezone is 6 hours slow
		//	[root@qe-testmachine ~]# date -d "2015-01-01T00:00:00.000000" -Iseconds
		//	2014-12-31T18:00:00+0100
		
		// RHEL6 CST Timezone is 1 hour fast
		//	[root@ibm-hs22-01 ~]# date -d "2015-01-01T00:00:00.000000" -Iseconds
		//	2015-01-01T01:00:00+0800

		// workaround on RHEL5 and RHEL6 for coreutils bug
		if (Integer.valueOf(clienttasks.redhatReleaseX) < 7) {
			// bump up the time in seconds by taking 7 hours and subtracting the time zone HH:MM offset)  TODO: determine if this formula works during daylight savings time. 
			migratTimeInSeconds += 7/*hours*/*60*60 - (Integer.parseInt(systemTimeZoneHHMM.split(":")[0])/*hours*/*60*60 + Integer.parseInt(systemTimeZoneHHMM.split(":")[1])/*minutes*/*60);
		}
		
		Assert.assertTrue(systemTimeInSeconds-tol < migratTimeInSeconds && migratTimeInSeconds < systemTimeInSeconds+tol, "The migration date fact '"+factMap.get(migrationDateFact)+"' was set within the last '"+tol+"' seconds (local system time).  Actual diff='"+String.valueOf(systemTimeInSeconds-migratTimeInSeconds)+"' seconds.");
		
		
		// assert we are no longer registered to RHN Classic
		// Two possible results can occur when the rhn-migrate-classic-to-rhsm script attempts to unregister from RHN Classic.  We need to tolerate both cases... 
		String successfulUnregisterMsg = "System successfully unregistered from RHN Classic.";
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13")) successfulUnregisterMsg = "System successfully unregistered from legacy server.";
		String unsuccessfulUnregisterMsg = "Did not receive a completed unregistration message from RHN Classic for system "+rhnSystemId+"."+"\n"+"Please investigate on the Customer Portal at https://access.redhat.com.";
		if (sshCommandResult.getStdout().contains(successfulUnregisterMsg)) {
			// Case 1: number of subscribed channels is low and all communication completes in a timely fashion.  Here is a snippet from stdout:
			//		Preparing to unregister system from RHN Classic ...
			//		System successfully unregistered from RHN Classic.
			Assert.assertTrue(sshCommandResult.getStdout().contains(successfulUnregisterMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+successfulUnregisterMsg);
			Assert.assertTrue(!sshCommandResult.getStdout().contains(unsuccessfulUnregisterMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' does NOT contain message: "+unsuccessfulUnregisterMsg);
			Assert.assertTrue(!clienttasks.isRhnSystemRegistered(),"This system is NOT registered using RHN Classic. Therefore this system will no longer communicate with RHN Classic.");
			Assert.assertTrue(!clienttasks.isRhnSystemIdRegistered(rhnreg_ksUsername, rhnreg_ksPassword, rhnHostname, rhnSystemId), "Confirmed that rhn systemId '"+rhnSystemId+"' is no longer registered on the RHN Classic server.");
		} else {
			// Case 2: number of subscribed channels is high and communication fails in a timely fashion (see bug 881952).  Here is a snippet from stdout:	
			//		Preparing to unregister system from RHN Classic ...
			//		Did not receive a completed unregistration message from RHN Classic for system 1023722557.
			//		Please investigate on the Customer Portal at https://access.redhat.com.
			log.warning("Did not detect expected message '"+successfulUnregisterMsg+"' from "+rhnMigrateTool+" stdout.  Nevertheless, the tool should inform us and continue the migration process.");
			Assert.assertTrue(sshCommandResult.getStdout().contains(unsuccessfulUnregisterMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+unsuccessfulUnregisterMsg);
			Assert.assertTrue(!clienttasks.isRhnSystemRegistered(),"This system is NOT registered using RHN Classic. Therefore this system will no longer communicate with RHN Classic.");
			if (!clienttasks.isRhnSystemIdRegistered(rhnreg_ksUsername, rhnreg_ksPassword, rhnHostname, rhnSystemId)) {
				Assert.assertFalse(false, "Confirmed that rhn systemId '"+rhnSystemId+"' is no longer registered on the RHN Classic server.");
			} else {
				log.warning("The RHN Classic server believes that this system is still registered.  SystemId '"+rhnSystemId+"' should be manually deleted on the Customer Portal.");
			}
		}
		
		// assert that the legacy services have been stopped (introduced by RFE Bug 1185914
		//	Product certificates installed successfully to /etc/pki/product.
		//
		//	Preparing to unregister system from legacy server...
		//	System successfully unregistered from legacy server.
		//	Stopping and disabling legacy services...
		//	osad: unrecognized service
		//	osad: unrecognized service
		// TEMPORARY WORKAROUND FOR BUG
		String bugId1 = "1390341"; // Bug 1390341 - rhn-migrate-classic-to-rhsm is failing to stop and disable services
		String bugId2 = "1432231"; // Bug 1432231 - rhn-migrate-classic-to-rhsm is failing to stop and disable services
		boolean invokeWorkaroundWhileBug1IsOpen = true;
		boolean invokeWorkaroundWhileBug2IsOpen = true;
		try {if (BzChecker.getInstance().isBugOpen(bugId1)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId1).toString()+" Bugzilla "+bugId1+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId1+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId1);} else {invokeWorkaroundWhileBug1IsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		try {if (BzChecker.getInstance().isBugOpen(bugId2)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId2).toString()+" Bugzilla "+bugId2+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId2+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId2);} else {invokeWorkaroundWhileBug2IsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBug1IsOpen||invokeWorkaroundWhileBug2IsOpen) {
			log.warning("Skipping assertion of stopped and disabled services while bug '"+bugId1+"' OR '"+bugId2+"' is open.");;
		} else
		// END OF WORKAROUND
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.18.2-1")) {
			String stoppingServicesMsg = "Stopping and disabling legacy services...";
			Assert.assertTrue(sshCommandResult.getStdout().contains(stoppingServicesMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+stoppingServicesMsg);

			// assert these LEGACY_DAEMONS = ["osad", "rhnsd"] are stopped and disabled
			// taken from https://bugzilla.redhat.com/show_bug.cgi?id=1185914#c0
			if (clienttasks.isPackageInstalled("osad")) {
				if (Integer.valueOf(clienttasks.redhatReleaseX)>=7) {
					//	[root@jsefler-rhel7 ~]# systemctl is-enabled osad.service 
					//	osad.service is not a native service, redirecting to /sbin/chkconfig.
					//	Executing /sbin/chkconfig osad --level=5
					//	disabled
					//	[root@jsefler-rhel7 ~]# echo $?
					//	1
					SSHCommandResult sshSystemctlIsEnabledCommandResult = client.runCommandAndWait("systemctl is-enabled osad.service");
					Assert.assertEquals(sshSystemctlIsEnabledCommandResult.getStdout().trim(),"disabled", "Expected stdout for systemctl is-enabled osad.service");
					Assert.assertEquals(sshSystemctlIsEnabledCommandResult.getExitCode(),Integer.valueOf(1), "Expected exitCode for systemctl is-enabled osad.service");
					//	[root@jsefler-rhel7 ~]# systemctl is-active osad.service
					//	unknown
					//	[root@jsefler-rhel7 ~]# echo $?
					//	3
					SSHCommandResult sshSystemctlIsActiveCommandResult = client.runCommandAndWait("systemctl is-active osad.service");
					Assert.assertEquals(sshSystemctlIsActiveCommandResult.getStdout().trim(),"unknown", "Expected stdout for systemctl is-active osad.service");
					Assert.assertEquals(sshSystemctlIsActiveCommandResult.getExitCode(),Integer.valueOf(3), "Expected exitCode for systemctl is-active osad.service");

				} else {
					//	[root@jsefler-rhel6 ~]# service osad status
					//	osad is stopped
					//	[root@jsefler-rhel6 ~]# echo $?
					//	3
					SSHCommandResult sshServiceCommandResult = client.runCommandAndWait("service osad status");
					Assert.assertEquals(sshServiceCommandResult.getExitCode(),Integer.valueOf(3), "Expected exitCode for service osad status");
					Assert.assertEquals(sshServiceCommandResult.getStdout().trim(),"osad is stopped", "Expected stdout for service osad status");
					//	[root@jsefler-rhel6 ~]# chkconfig --list osad
					//	osad           	0:off	1:off	2:off	3:off	4:off	5:off	6:off
					SSHCommandResult sshChkconfigCommandResult = client.runCommandAndWait("chkconfig --list osad");
					Assert.assertEquals(sshChkconfigCommandResult.getExitCode(),Integer.valueOf(0), "Expected exitCode for chkconfig --list osad");
					String sshChkconfigRegex = "osad\\s+0:off\\s+1:off\\s+2:off\\s+3:off\\s+4:off\\s+5:off\\s+6:off";
					Assert.assertTrue(sshChkconfigCommandResult.getStdout().trim().matches(sshChkconfigRegex), "Expected stdout for chkconfig --list osad to match regex '"+sshChkconfigRegex+"'.");
				}
			}
			if (clienttasks.isPackageInstalled("rhnsd")) {
				if (Integer.valueOf(clienttasks.redhatReleaseX)>=7) {
					//	[root@jsefler-rhel7 ~]# systemctl is-enabled rhnsd.service 
					//	rhnsd.service is not a native service, redirecting to /sbin/chkconfig.
					//	Executing /sbin/chkconfig rhnsd --level=5
					//	disabled
					//	[root@jsefler-rhel7 ~]# echo $?
					//	1
					SSHCommandResult sshSystemctlIsEnabledCommandResult = client.runCommandAndWait("systemctl is-enabled rhnsd.service");
					Assert.assertEquals(sshSystemctlIsEnabledCommandResult.getStdout().trim(),"disabled", "Expected stdout for systemctl is-enabled rhnsd.service");
					Assert.assertEquals(sshSystemctlIsEnabledCommandResult.getExitCode(),Integer.valueOf(1), "Expected exitCode for systemctl is-enabled rhnsd.service");
					//	[root@jsefler-rhel7 ~]# chkconfig --list rhnsd
					//
					//	Note: This output shows SysV services only and does not include native
					//	      systemd services. SysV configuration data might be overridden by native
					//	      systemd configuration.
					//
					//	      If you want to list systemd services use 'systemctl list-unit-files'.
					//	      To see services enabled on particular target use
					//	      'systemctl list-dependencies [target]'.
					//
					//	rhnsd          	0:off	1:off	2:off	3:off	4:off	5:off	6:off
					SSHCommandResult sshChkconfigCommandResult = client.runCommandAndWait("chkconfig --list rhnsd");
					Assert.assertEquals(sshChkconfigCommandResult.getExitCode(),Integer.valueOf(0), "Expected exitCode for chkconfig --list rhnsd");
					String sshChkconfigRegex = "rhnsd\\s+0:off\\s+1:off\\s+2:off\\s+3:off\\s+4:off\\s+5:off\\s+6:off";
					Assert.assertTrue(sshChkconfigCommandResult.getStdout().trim().matches(sshChkconfigRegex), "Expected stdout for chkconfig --list rhnsd to match regex '"+sshChkconfigRegex+"'.");
					//	[root@jsefler-rhel7 ~]# systemctl is-active rhnsd.service
					//	unknown
					//	[root@jsefler-rhel7 ~]# echo $?
					//	3
					SSHCommandResult sshSystemctlIsActiveCommandResult = client.runCommandAndWait("systemctl is-active rhnsd.service");
					Assert.assertEquals(sshSystemctlIsActiveCommandResult.getStdout().trim(),"unknown", "Expected stdout for systemctl is-active rhnsd.service");
					Assert.assertEquals(sshSystemctlIsActiveCommandResult.getExitCode(),Integer.valueOf(3), "Expected exitCode for systemctl is-active rhnsd.service");
				} else {
					//	[root@jsefler-rhel6 ~]# service rhnsd status
					//	[root@jsefler-rhel6 ~]# echo $?
					//	6
					SSHCommandResult sshServiceCommandResult = client.runCommandAndWait("service rhnsd status");
					Assert.assertEquals(sshServiceCommandResult.getExitCode(),Integer.valueOf(6), "Expected exitCode for service rhnsd status - because /etc/init.d/rhnsd is programmed to exit 6 when there is no systemid file");
					Assert.assertEquals(sshServiceCommandResult.getStdout().trim(),"", "Expected stdout for service rhnsd status");
					//	[root@jsefler-rhel6 ~]# chkconfig --list rhnsd
					//	rhnsd           	0:off	1:off	2:off	3:off	4:off	5:off	6:off
					SSHCommandResult sshChkconfigCommandResult = client.runCommandAndWait("chkconfig --list rhnsd");
					Assert.assertEquals(sshChkconfigCommandResult.getExitCode(),Integer.valueOf(0), "Expected exitCode for chkconfig --list rhnsd");
					String sshChkconfigRegex = "rhnsd\\s+0:off\\s+1:off\\s+2:off\\s+3:off\\s+4:off\\s+5:off\\s+6:off";
					Assert.assertTrue(sshChkconfigCommandResult.getStdout().trim().matches(sshChkconfigRegex), "Expected stdout for chkconfig --list rhnsd to match regex '"+sshChkconfigRegex+"'.");
				}
			}
			
			// assert that no FAILED nor Usage errors occurred
			Assert.assertTrue(!sshCommandResult.getStdout().contains("FAILED"), "Should not encounter a FAILED message when Stopping and disabling legacy services");	// Bug 1390341 - rhn-migrate-classic-to-rhsm is failing to stop and disable services
			Assert.assertTrue(!sshCommandResult.getStdout().contains("Usage"), "Should not encounter a Usage error when Stopping and disabling legacy services");	// Bug 1390341 - rhn-migrate-classic-to-rhsm is failing to stop and disable services
		}
		String removingPackagesMsg = "Removing legacy packages...";
		if (options.contains("--remove-rhn-packages")) {
			Assert.assertTrue(sshCommandResult.getStdout().contains(removingPackagesMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+removingPackagesMsg);
		} else {
			Assert.assertTrue(!sshCommandResult.getStdout().contains(removingPackagesMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' does not contain message: "+removingPackagesMsg);	
		}
			
		// assert that when --serverurl is specified, its hostname:port/prefix are preserved into rhsm.conf
		if (options.contains("--serverurl") || options.contains("--destination-url")) {
			// comparing to original configuration values because these are the ones I am using in the dataProvider
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname"),originalServerHostname,"The value of the [server]hostname newly configured in "+clienttasks.rhsmConfFile+" was extracted from the --serverurl option specified in rhn-migrated-classic-to-rhsm options '"+options+"'.");
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port"),originalServerPort,"The value of the [server]port newly configured in "+clienttasks.rhsmConfFile+" was extracted from the --serverurl option specified in rhn-migrated-classic-to-rhsm options '"+options+"'.");
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "prefix"),originalServerPrefix,"The value of the [server]prefix newly configured in "+clienttasks.rhsmConfFile+" was extracted from the --serverurl option specified in rhn-migrated-classic-to-rhsm options '"+options+"'.");
		}
		
		// assert that we are newly registered using rhsm
		clienttasks.identity(null, null, null, null, null, null, null, null);
		Assert.assertNotNull(clienttasks.getCurrentConsumerId(),"The existance of a consumer cert indicates that the system is currently registered using RHSM.");
		expectedMsg = String.format("System '%s' successfully registered to Red Hat Subscription Management.",	clienttasks.hostname);
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) expectedMsg = String.format("System '%s' successfully registered.",	clienttasks.hostname); // changed by commit fad3de89
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "1451003"; // Bug 1451003 - subscription-manager identity reports redundant UUID info in the name field
		boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping assertion that Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+expectedMsg);
		} else
		// END OF WORKAROUND
		Assert.assertTrue(sshCommandResult.getStdout().contains(expectedMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+expectedMsg);

		// assert the the expected service level was set as a preference on the registered consumer
		if (serviceLevelExpected!=null) {
			String serviceLevel = clienttasks.getCurrentServiceLevel();
			Assert.assertTrue(serviceLevelExpected.equalsIgnoreCase(serviceLevel), "Regardless of case, the serviceLevel requested during migration (or possibly the org's defaultServiceLevel) was set as the system's service level preference (serviceLevelExpected='"+serviceLevelExpected+"').");
		}
		
		// assert that when --no-auto is specified, no entitlements were granted during the rhsm registration
		String autosubscribeAttemptedMsg = "Attempting to auto-subscribe to appropriate subscriptions ...";
		autosubscribeAttemptedMsg = "Attempting to auto-attach to appropriate subscriptions ...";	// changed by bug 876294
		autosubscribeAttemptedMsg = "Attempting to auto-attach to appropriate subscriptions...";	// changed by subscription-manager commit 1fba5696
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) autosubscribeAttemptedMsg = "Installed Product Current Status:";	// commit fad3de89779f2217e788b3564ef5dca7f85914fb removed the "Attempting to auto-attach to appropriate subscriptions..." feedback
		String autosubscribeFailedMsg = "Unable to auto-subscribe.  Do your existing subscriptions match the products installed on this system?";
		autosubscribeFailedMsg = "Unable to auto-attach.  Do your existing subscriptions match the products installed on this system?";	// changed by bug 876294
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) autosubscribeFailedMsg = "Unable to find available subscriptions for all your installed products.";	// commit fad3de89779f2217e788b3564ef5dca7f85914fb	// matches functionality from bug 864195
//OLD	if (options.contains("-n")) { // -n, --no-auto   Do not autosubscribe when registering with subscription-manager
		if (options.contains("-n") && !options.contains("--activation-key")) { // -n, --no-auto   Do not autosubscribe when registering with subscription-manager

			// assert that autosubscribe was NOT attempted
			Assert.assertTrue(!sshCommandResult.getStdout().contains(autosubscribeAttemptedMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' does NOT contain message: "+autosubscribeAttemptedMsg);			
			Assert.assertTrue(!sshCommandResult.getStdout().contains(autosubscribeFailedMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' does NOT contain message: "+autosubscribeFailedMsg);			

			// assert that we are NOT registered using rhsm
			/* THIS ASSERTION IS WRONG! DON'T DO IT!  BUG 849644
			clienttasks.identity_(null, null, null, null, null, null, null);
			Assert.assertNull(clienttasks.getCurrentConsumerCert(),"We should NOT be registered to RHSM after a call to "+rhnMigrateTool+" with options "+options+".");
			*/
			
			// assert that we are NOT consuming any entitlements
			Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().isEmpty(),"We should NOT be consuming any RHSM entitlements after call to "+rhnMigrateTool+" with options ("+options+") that indicate no autosubscribe.");
			
//OLD	} else {
		} if (!options.contains("-n") && !options.contains("--activation-key")) {
			
			// assert that autosubscribe was attempted
			Assert.assertTrue(sshCommandResult.getStdout().contains(autosubscribeAttemptedMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+autosubscribeAttemptedMsg);			
			
			// assert that the migrated productCert corresponding to the base channel has been autosubscribed by checking the status on the installedProduct
			// FIXME This assertion is wrong when there are no available subscriptions that provide for the migrated product certs' providesTags; however since we register as qa@redhat.com, I think we have access to all base rhel subscriptions
			// FIXME if a service-level is provided that is not available, then this product may NOT be subscribed
			/* DECIDED NOT TO FIXME SINCE THIS ASSERTION IS THE JOB OF DEDICATED AUTOSUBSCRIBE TESTS IN SubscribeTests.java
			InstalledProduct installedProduct = clienttasks.getInstalledProductCorrespondingToProductCert(clienttasks.getProductCertFromProductCertFile(new File(clienttasks.productCertDir+"/"+getPemFileNameFromProductCertFilename(channelsToProductCertFilenamesMap.get(rhnBaseChannel)))));
			Assert.assertEquals(installedProduct.status, "Subscribed","The migrated product cert corresponding to the RHN Classic base channel '"+rhnBaseChannel+"' was autosubscribed: "+installedProduct);
			*/
			
			// assert that autosubscribe feedback was a success (or not)
			List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
			if (consumedProductSubscriptions.isEmpty()) {
				Assert.assertTrue(sshCommandResult.getStdout().contains(autosubscribeFailedMsg), "Assuming the base RHN channel has been migrated, but no entitlements have been granted, stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+autosubscribeFailedMsg);
			} /*else { // 06/15/2015: This else block is wrong since an RHN Channel could have been successfully migrated, but there is no subscription available to cover it.  Replacing it with a call to get the compliance status
				Assert.assertTrue(!sshCommandResult.getStdout().contains(autosubscribeFailedMsg), "When autosubscribe is successful and entitlements have been granted, stdout from call to '"+rhnMigrateTool+" "+options+"' does NOT contain message: "+autosubscribeFailedMsg);				
			} */
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit 7957b8df95c575e6e8713c2f1a0f8f754e32aed3 bug 1119688
				SSHCommandResult statusResult = clienttasks.status(null, null, null, null, null);
				if (statusResult.getStdout().contains("Overall Status: Invalid")) {
					Assert.assertTrue(sshCommandResult.getStdout().contains(autosubscribeFailedMsg), "Since the subscription-manager overall status appears Invalid, the most likely reason is because at least one of the migrated products could not be auto-subscribed.  Therefore stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+autosubscribeFailedMsg);
				} else {	// Note: "Overall Status: Insufficient" is possible and likely when the auto-subscribed subscription pool is Temporary
					Assert.assertTrue(!sshCommandResult.getStdout().contains(autosubscribeFailedMsg), "Since the subscription-manager overall status does not appear Invalid (overall status of Current or Insufficient are likely), all of the migrated products should have been auto-subscribed.  Therefore stdout from call to '"+rhnMigrateTool+" "+options+"' does NOT contain message: "+autosubscribeFailedMsg);						
				}
			}
			
			// assert that when no --servicelevel is specified, then no service level preference will be set on the registered consumer
			if (!options.contains("-s ") && !options.contains("--servicelevel") && !options.contains("--service-level") && (serviceLevelExpected==null||serviceLevelExpected.isEmpty())) {
				// assert no service level preference was set
				Assert.assertEquals(clienttasks.getCurrentServiceLevel(), "", "No servicelevel preference should be set on the consumer when no service level was requested.");
			}
			
			// assert the service levels consumed from autosubscribe match the requested serviceLevel
			if (serviceLevelExpected!=null && !serviceLevelExpected.isEmpty()) {
	
				// when a valid servicelevel was either specified or chosen
				expectedMsg = String.format("Service level set to: %s",serviceLevelExpected);
				Assert.assertTrue(sshCommandResult.getStdout().toUpperCase().contains(expectedMsg.toUpperCase()), "Regardless of service level case, the stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+expectedMsg);
				
				for (ProductSubscription productSubscription : consumedProductSubscriptions) {
					Assert.assertNotNull(productSubscription.serviceLevel, "When migrating from RHN Classic with a specified service level '"+serviceLevelExpected+"', this auto consumed product subscription's service level should not be null: "+productSubscription);
					if (sm_exemptServiceLevelsInUpperCase.contains(productSubscription.serviceLevel.toUpperCase())) {
						log.info("Exempt service levels: "+sm_exemptServiceLevelsInUpperCase);
						Assert.assertTrue(sm_exemptServiceLevelsInUpperCase.contains(productSubscription.serviceLevel.toUpperCase()),"This auto consumed product subscription's service level is among the exempt service levels: "+productSubscription);
					} else {
						Assert.assertTrue(productSubscription.serviceLevel.equalsIgnoreCase(serviceLevelExpected),"When migrating from RHN Classic with a specified service level '"+serviceLevelExpected+"', this auto consumed product subscription's service level should match: "+productSubscription);
					}
				}
			}
		}
		if (options.contains("--activation-key")) {
			// assert that autosubscribe was NOT attempted
			//Assert.assertTrue(!sshCommandResult.getStdout().contains(autosubscribeAttemptedMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' does NOT contain message: "+autosubscribeAttemptedMsg);	// not a fair assertion since autosubscribeAttemptedMsg was set to "Installed Product Current Status:"
			//Assert.assertTrue(!sshCommandResult.getStdout().contains(autosubscribeFailedMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' does NOT contain message: "+autosubscribeFailedMsg);	// also not a fair assertion since autosubscribeFailedMsg was set to "Unable to find available subscriptions for all your installed products."
			Assert.assertTrue(!sshCommandResult.getStdout().toLowerCase().contains("auto-attach"),"Stdout from an attempt to migrate with options ("+options+") which include an activation key should make no reference to 'auto-attach'");
			Assert.assertTrue(!sshCommandResult.getStdout().toLowerCase().contains("auto-subscribe"),"Stdout from an attempt to migrate with options ("+options+") which include an activation key should make no reference to 'auto-subscribe'");
			
			// assert that we are consuming entitlements, TODO but this is really dependent on the activation key
			Assert.assertTrue(!clienttasks.getCurrentlyConsumedProductSubscriptions().isEmpty(),"We should be consuming some entitlements after a call to "+rhnMigrateTool+" with options ("+options+").  This assumes the activation key contained one or more pools.");
			
			// TODO When an activation key is used, all of the consumed subscriptions should have come from the activation key.
			// It would be best to make these assertion in the test that calls this function.
		}
		
		// assert that the rhnplugin has been disabled
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.yumPluginConfFileForRhn, "enabled"),"0","The enabled yum plugin configuration for RHN.");
		
		// assert that productid.js is updated with productid mappings for all of the rhnChannelsConsumed; coverage for Bug 972883 - rhn-migrate-classic-to-rhsm tool neglects to populate /var/lib/rhsm/productid.js
		client.runCommandAndWait("cat "+clienttasks.productIdJsonFile);
		for (String rhnChannelConsumed : rhnChannelsConsumed) {
			if (channelsToProductCertFilenamesMap.containsKey(rhnChannelConsumed)) {
				String productId = MigrationDataTests.getProductIdFromProductCertFilename(channelsToProductCertFilenamesMap.get(rhnChannelConsumed));
				
				// special case (see RhnMigrateClassicToRhsm_Rhel5ClientDesktopVersusWorkstation_Test)
				if (clienttasks.releasever.equals("5Client")) {
					String productIdForDesktop = "68";
					String productIdForWorkstation = "71";
					if (productId.equals(productIdForDesktop)) {
						log.info("Encountered a special case for migration of a 5Client system from RHN Classic to RHSM...");
						log.info("Red Hat Enterprise Linux Desktop (productId=68) corresponds to the base RHN Channel (rhel-ARCH-client-5) for a 5Client system where ARCH=i386,x86_64.");
						log.info("Red Hat Enterprise Linux Workstation (productId=71) corresponds to child RHN Channel (rhel-ARCH-client-workstation-5) for a 5Client system where ARCH=i386,x86_64.");	
						log.info("After migrating from RHN Classic to RHSM, these two product certs should not be installed at the same time; Workstation should prevail.");
						if (productIdRepoMapAfterMigration.containsKey(productIdForWorkstation)) {
							Assert.assertTrue(!productIdRepoMapAfterMigration.containsKey(productId), "The '"+clienttasks.productIdJsonFile+"' database should NOT contain an entry for productId '"+productId+"' which was migrated for consumption of Classic RHN Channel '"+rhnChannelConsumed+"' when Workstation channels for product '"+productIdForWorkstation+"' have also been migrated (Workstation wins).");
							continue;
						}
					}
				}
				
				if (productId.equalsIgnoreCase("none")) {
					Assert.assertTrue(!productIdRepoMapAfterMigration.containsKey(productId), "The '"+clienttasks.productIdJsonFile+"' database does NOT contain an entry for productId '"+productId+"' after migration while consuming Classic RHN Channel '"+rhnChannelConsumed+"'.");
				} else {
					Assert.assertTrue(productIdRepoMapAfterMigration.containsKey(productId), "The '"+clienttasks.productIdJsonFile+"' database contains an entry for productId '"+productId+"' which was migrated for consumption of Classic RHN Channel '"+rhnChannelConsumed+"'.");
					Assert.assertTrue(productIdRepoMapAfterMigration.get(productId).contains(rhnChannelConsumed), "The '"+clienttasks.productIdJsonFile+"' database entry for productId '"+productId+"' contains Classic RHN Channel/Repo '"+rhnChannelConsumed+"'.");
				}
			}
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20108", "RHEL7-51113"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="With a proxy configured in rhn/up2date, register system using RHN Classic and then Execute migration tool rhn-migrate-classic-to-rhsm with options after adding RHN Channels",
			groups={"Tier1Tests","RhnMigrateClassicToRhsm_Test","RhnMigrateClassicToRhsmUsingProxyServer_Test","blockedbyBug-798015","blockedbyBug-861693","blockedbyBug-878986","blockedbyBug-912776","blockedByBug-1052297","blockedByBug-1111258","blockedByBug-1345962","blockedByBug-1512948","blockedByBug-1516832","blockedByBug-1546351"},
			dependsOnMethods={},
			dataProvider="RhnMigrateClassicToRhsmUsingProxyServerData",
			enabled=true)
	@ImplementsNitrateTest(caseId=130763)
	public void testRhnMigrateClassicToRhsmUsingProxyServer(Object bugzilla, String rhnreg_ksUsername, String rhnreg_ksPassword, String rhnHostname, List<String> rhnChannelsToAdd, String options, String rhnUsername, String rhnPassword, String rhsmUsername, String rhsmPassword, String rhsmOrg, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogRegex) {
		if (sm_rhnHostname.equals("")) throw new SkipException("This test requires access to RHN Classic or Satellite 5.");
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7 && clienttasks.arch.equals("ppc64le")) throw new SkipException("Use of rhn-migrate-classic-to-rhsm is not necessary on RHEL '"+client1tasks.redhatReleaseX+"' arch '"+clienttasks.arch+"' since this product was not released on RHN Classic.");
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7 && clienttasks.arch.equals("aarch64")) throw new SkipException("Use of rhn-migrate-classic-to-rhsm is not necessary on RHEL '"+client1tasks.redhatReleaseX+"' arch '"+clienttasks.arch+"' since this product was not released on RHN Classic.");
		
		// make sure we are NOT registered to RHSM
		clienttasks.unregister_(null,null,null, null);
		clienttasks.removeAllCerts(true, true, true);
		clienttasks.removeAllFacts();
		String candlepinServerPort = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port");
		String candlepinServerHostname = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname");
		
		// remove proxy settings from up2date
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxy", "0");		// enableProxyAuth[comment]=To use an authenticated proxy or not
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "httpProxy", "");			// httpProxy[comment]=HTTP proxy in host:port format, e.g. squid.redhat.com:3128
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxyAuth", "0");	// enableProxyAuth[comment]=To use an authenticated proxy or not
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "proxyUser", "");			// proxyUser[comment]=The username for an authenticated proxy
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "proxyPassword", "");		// proxyPassword[comment]=The password to use for an authenticated proxy
		iptablesAcceptPort(candlepinServerPort);
		
		// enable/set proxy settings for RHN up2date
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxy",		"1");											// enableProxyAuth[comment]=To use an authenticated proxy or not
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "httpProxy",		proxy_hostnameConfig+":"+proxy_portConfig);		// httpProxy[comment]=HTTP proxy in host:port format, e.g. squid.redhat.com:3128
		if (proxy_userConfig.equals("") && proxy_passwordConfig.equals("")) {
			clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxyAuth", "0");	// enableProxyAuth[comment]=To use an authenticated proxy or not
			clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "proxyUser",		"disabled-proxy-user");								// proxyUser[comment]=The username for an authenticated proxy
			clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "proxyPassword",	"disabled-proxy-password");							// proxyPassword[comment]=The password to use for an authenticated proxy
		} else {
			clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxyAuth", "1");	// enableProxyAuth[comment]=To use an authenticated proxy or not
			clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "proxyUser",		proxy_userConfig);								// proxyUser[comment]=The username for an authenticated proxy
			clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "proxyPassword",	proxy_passwordConfig);							// proxyPassword[comment]=The password to use for an authenticated proxy
		}
		
		// mark the tail of proxyLog with a message
		String proxyLogMarker = System.currentTimeMillis()+" Testing RhnMigrateClassicToRhsmUsingProxyServer_Test.registerToRhnClassic from "+clienttasks.hostname+"...";
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// register to RHN Classic
		String rhnSystemId = clienttasks.registerToRhnClassic(rhnreg_ksUsername, rhnreg_ksPassword, rhnHostname);
		Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(rhnreg_ksUsername, rhnreg_ksPassword, rhnHostname, rhnSystemId),"Confirmed that rhn systemId '"+rhnSystemId+"' is currently registered.");
		
		// assert that traffic to RHN went through the proxy
		String proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
		Assert.assertContainsMatch(proxyLogResult, proxyLogRegex, "The proxy server appears to be logging the expected connection attempts to RHN.");
		
		// subscribe to more RHN Classic channels
		if (!rhnChannelsToAdd.isEmpty()) addRhnClassicChannels(rhnreg_ksUsername, rhnreg_ksPassword, rhnChannelsToAdd);
		
		// get a list of the consumed RHN Classic channels
		List<String> rhnChannelsConsumed = clienttasks.getCurrentRhnClassicChannels();
		if (!rhnChannelsToAdd.isEmpty()) Assert.assertTrue(rhnChannelsConsumed.containsAll(rhnChannelsToAdd), "All of the RHN Classic channels added appear to be consumed.");
		
		// reject traffic through the server.port (when not testing with --no-proxy)
		if (!options.contains("--no-proxy")) iptablesRejectPort(candlepinServerPort);
		
		// mark the tail of proxyLog with a message
		proxyLogMarker = System.currentTimeMillis()+" Testing RhnMigrateClassicToRhsmUsingProxyServer_Test.executeRhnMigrateClassicToRhsmWithOptions from "+clienttasks.hostname+"...";
		RemoteFileTasks.markFile(proxyRunner, proxyLog, proxyLogMarker);
		
		// execute rhn-migrate-classic-to-rhsm with options
		SSHCommandResult sshCommandResult = executeRhnMigrateClassicToRhsm(options,rhnUsername,rhnPassword,rhsmUsername,rhsmPassword,rhsmOrg,null, null);
		
		// assert that traffic to RHSM went through the proxy (unless testing --no-proxy)
		proxyLogResult = RemoteFileTasks.getTailFromMarkedFile(proxyRunner, proxyLog, proxyLogMarker, ipv4_address);	// accounts for multiple tests hitting the same proxy server simultaneously
		int numberOfConnectionAttempts;
		String conflictingProductCertsMsg = "You are subscribed to channels that have conflicting product certificates.";	// "Unable to continue migration!";
		if (sshCommandResult.getStdout().contains(conflictingProductCertsMsg)) numberOfConnectionAttempts=3; else numberOfConnectionAttempts=4;
		if (options.contains("--no-proxy"))	{
			//Assert.assertContainsNoMatch(proxyLogResult, proxyLogRegex, "The proxy server should NOT be logging the connection attempts to RHSM when --no-proxy option is used, but should be logging connection attempts to RHN.");

			// /var/log/squid/access.log
			//	1375391532.170    437 10.16.120.123 TCP_MISS/200 1859 CONNECT xmlrpc.rhn.code.stage.redhat.com:443 redhat DIRECT/10.24.127.44 -
			//	1375391532.530    349 10.16.120.123 TCP_MISS/200 2067 CONNECT xmlrpc.rhn.code.stage.redhat.com:443 redhat DIRECT/10.24.127.44 -
			//	1375391532.900    356 10.16.120.123 TCP_MISS/200 3571 CONNECT xmlrpc.rhn.code.stage.redhat.com:443 redhat DIRECT/10.24.127.44 -
			//	1375391533.435    514 10.16.120.123 TCP_MISS/200 1811 CONNECT xmlrpc.rhn.code.stage.redhat.com:443 redhat DIRECT/10.24.127.44 -
			if (proxyLogRegex.equals("TCP_MISS") && !proxyLogResult.trim().isEmpty()) for (String proxyLogEntry : proxyLogResult.trim().split("\n")) {
				Assert.assertTrue(proxyLogEntry.contains(rhnHostname.replaceFirst("https?://", "")), "Running rhn-migrate-classic-to-rhsm --no-proxy while RHN up2date is configured with a proxy should only log proxy attempts to '"+rhnHostname.replaceFirst("https?://", "")+"' from subscription-manager client ip '"+ipv4_address+"'.");
			}
			// /var/log/tinyproxy.log
			//	CONNECT   Aug 01 17:44:56 [10139]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:44:56 [10137]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:44:57 [10138]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:44:57 [10136]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			Assert.assertEquals(proxyLogResult.split("\n").length, numberOfConnectionAttempts, "It was determined during manual testing that running rhn-migrate-classic-to-rhsm --no-proxy while RHN up2date is configured with a proxy will yield this number of connection attempts through the proxy.");
		} else {
			Assert.assertContainsMatch(proxyLogResult, proxyLogRegex, "The proxy server appears to be logging the expected connection attempts to RHSM from the subscription-manager client ip '"+ipv4_address+"'.");

			// /var/log/squid/access.log
			//	1375391369.882     52 10.16.120.123 TCP_MISS/200 1710 CONNECT jsefler-f14-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.202 -
			//	1375391369.940     44 10.16.120.123 TCP_MISS/200 1710 CONNECT jsefler-f14-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.202 -
			//	1375391369.987     32 10.16.120.123 TCP_MISS/200 2110 CONNECT jsefler-f14-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.202 -
			//	1375391370.469    471 10.16.120.123 TCP_MISS/200 1859 CONNECT xmlrpc.rhn.code.stage.redhat.com:443 redhat DIRECT/10.24.127.44 -
			//	1375391370.830    347 10.16.120.123 TCP_MISS/200 2067 CONNECT xmlrpc.rhn.code.stage.redhat.com:443 redhat DIRECT/10.24.127.44 -
			//	1375391371.203    361 10.16.120.123 TCP_MISS/200 3571 CONNECT xmlrpc.rhn.code.stage.redhat.com:443 redhat DIRECT/10.24.127.44 -
			//	1375391371.657    428 10.16.120.123 TCP_MISS/200 1811 CONNECT xmlrpc.rhn.code.stage.redhat.com:443 redhat DIRECT/10.24.127.44 -
			//	1375391372.273     32 10.16.120.123 TCP_MISS/200 2110 CONNECT jsefler-f14-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.202 -
			//	1375391372.330     43 10.16.120.123 TCP_MISS/200 1374 CONNECT jsefler-f14-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.202 -
			//	1375391374.452   1880 10.16.120.123 TCP_MISS/200 8766 CONNECT jsefler-f14-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.202 -
			//	1375391374.517     40 10.16.120.123 TCP_MISS/200 1374 CONNECT jsefler-f14-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.202 -
			//	1375391374.579     51 10.16.120.123 TCP_MISS/200 2110 CONNECT jsefler-f14-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.202 -
			//	1375391374.701    106 10.16.120.123 TCP_MISS/200 1518 CONNECT jsefler-f14-candlepin.usersys.redhat.com:8443 redhat DIRECT/10.16.120.202 -
			if (proxyLogRegex.equals("TCP_MISS") && !proxyLogResult.trim().isEmpty()) {
				Assert.assertTrue(proxyLogResult.contains(rhnHostname.replaceFirst("https?://", "")) && proxyLogResult.contains(candlepinServerHostname), "Running rhn-migrate-classic-to-rhsm while RHN up2date is configured with a proxy should log proxy attempts to '"+rhnHostname.replaceFirst("https?://", "")+"' and '"+candlepinServerHostname+"' from subscription-manager client ip '"+ipv4_address+"'.");
				for (String proxyLogEntry : proxyLogResult.trim().split("\n")) Assert.assertTrue(proxyLogEntry.contains(rhnHostname.replaceFirst("https?://", ""))||proxyLogEntry.contains(candlepinServerHostname), "Running rhn-migrate-classic-to-rhsm while RHN up2date is configured with a proxy should only log proxy attempts to '"+rhnHostname.replaceFirst("https?://", "")+"' or '"+candlepinServerHostname+"' from subscription-manager client ip '"+ipv4_address+"'.");
				// NOTE: The following assert assumes the default "logformat squid" is being used - see http://www.squid-cache.org/Doc/config/logformat/
				Assert.assertEquals(getSubstringMatches(proxyLogResult,rhnHostname.replaceFirst("https?://", "")).size(), numberOfConnectionAttempts, "It was determined during manual testing that running rhn-migrate-classic-to-rhsm while RHN up2date is configured with a proxy will yield exactly this number of connection attempts through the proxy to RHN hostname '"+rhnHostname.replaceFirst("https?://", "")+"'.");
			}
			// /var/log/tinyproxy.log
			//	CONNECT   Aug 01 17:40:55 [10134]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:40:55 [10140]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:40:55 [10133]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:40:55 [10139]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:40:56 [10137]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:40:56 [10138]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:40:57 [10136]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:40:58 [10141]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:40:58 [10135]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:40:58 [10132]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:40:59 [10134]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:40:59 [10140]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			//	CONNECT   Aug 01 17:40:59 [10133]: Connect (file descriptor 7): 10-16-120-123.rhq.lab.eng.bos.redhat.com [10.16.120.123]
			Assert.assertTrue(proxyLogResult.split("\n").length>numberOfConnectionAttempts, "It was determined during manual testing that running rhn-migrate-classic-to-rhsm while RHN up2date is configured with a proxy will yield more than '"+numberOfConnectionAttempts+"' connection attempts through the proxy.  The '"+numberOfConnectionAttempts+"' proxy connection attempts are to RHN.");
		}
		
		// assert the exit code
		checkForKnownBug881952(sshCommandResult);
		checkForKnownBug1130637(sshCommandResult);
		String expectedMsg;
		if (sshCommandResult.getStdout().contains(conflictingProductCertsMsg)) {	// when "You are subscribed to channels that have conflicting product certificates." migration aborts.
			log.warning("You are subscribed to channels that have conflicting product certificates.  Therefore, the "+rhnMigrateTool+" command should have exited with code 1.");
			Assert.assertEquals(sshCommandResult.getExitCode(), new Integer(1), "ExitCode from call to '"+rhnMigrateTool+" "+options+"' when the RHN channels map to conflicting product certs that share the same product ID");
			Assert.assertTrue(clienttasks.isRhnSystemRegistered(),"This system is registered using RHN Classic when the RHN channels map to conflicting product certs that share the same product ID");
			
			// assert that we are not yet registered to RHSM
			Assert.assertNull(clienttasks.getCurrentConsumerCert(),"We should NOT be registered to RHSM when "+rhnMigrateTool+" detects that the RHN channels map to conflicting product certs that share the same product ID");
			
			// assert that we are still registered to RHN
			removeProxyServerConfigurations();	// needed before we can call isRhnSystemIdRegistered(...)
			Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(rhnreg_ksUsername, rhnreg_ksPassword, rhnHostname, rhnSystemId),"Confirmed that rhn systemId '"+rhnSystemId+"' is still registered since our migration attempt aborted after detecting that the RHN channels map to conflicting product certs that share the same product ID");

			return;
		}
		if (!areAllChannelsMapped(rhnChannelsConsumed) && !options.contains("-f")/*--force*/) {	// when not all of the rhnChannelsConsumed have been mapped to a productCert and no --force has been specified.
			log.warning("Not all of the channels are mapped to a product cert.  Therefore, the "+rhnMigrateTool+" command should have exited with code 1.");
			expectedMsg = "Use --force to ignore these channels and continue the migration.";
			Assert.assertTrue(sshCommandResult.getStdout().contains(expectedMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+expectedMsg);	
			Assert.assertEquals(sshCommandResult.getExitCode(), new Integer(1), "ExitCode from call to '"+rhnMigrateTool+" "+options+"' when any of the channels are not mapped to a productCert.");
			Assert.assertTrue(clienttasks.isRhnSystemRegistered(),"This system is registered using RHN Classic when rhn-migrate-classic-to-rhsm requires --force to continue.");
			
			// assert that we are not yet registered to RHSM
			Assert.assertNull(clienttasks.getCurrentConsumerCert(),"We should NOT be registered to RHSM when "+rhnMigrateTool+" requires --force to continue.");
			
			// assert that we are still registered to RHN
			Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(rhnreg_ksUsername, rhnreg_ksPassword, rhnHostname, rhnSystemId),"Confirmed that rhn systemId '"+rhnSystemId+"' is still registered since our migration attempt requires --force to continue.");

			return;
		}
		Assert.assertEquals(sshCommandResult.getExitCode(), new Integer(0), "ExitCode from call to '"+rhnMigrateTool+" "+options+"' when all of the channels are mapped.");
		
		// assert that proxy configurations from RHN up2date have been copied to RHSM rhsm.conf (unless testing --no-proxy)
		if (options.contains("--no-proxy")) {
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "proxy_hostname"),	"", "When --no-proxy is specified, the RHN hostname component from the httpProxy configuration in "+clienttasks.rhnUp2dateFile+" should NOT be copied to the RHSM server.proxy_hostname configuration in "+clienttasks.rhsmConfFile+".");
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "proxy_port"),		"", "When --no-proxy is specified, the RHN port component from the httpProxy configuration in "+clienttasks.rhnUp2dateFile+" should NOT be copied to the RHSM server.proxy_port configuration in "+clienttasks.rhsmConfFile+".");
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "proxy_user"), 	"", "When --no-proxy is specified, the RHN proxyUser configuration in "+clienttasks.rhnUp2dateFile+" should NOT be copied to the RHSM server.proxy_user configuration in "+clienttasks.rhsmConfFile+".");
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "proxy_password"), "", "When --no-proxy is specified, the RHN proxyPassword configuration in "+clienttasks.rhnUp2dateFile+" should NOT be copied to the RHSM server.proxy_password configuration in "+clienttasks.rhsmConfFile+".");
		} else {
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "proxy_hostname"), proxy_hostnameConfig.replace("http://", ""), "The RHN hostname component from the httpProxy configuration in "+clienttasks.rhnUp2dateFile+" has been copied to the RHSM server.proxy_hostname configuration in "+clienttasks.rhsmConfFile+" (with prefix \"http://\" removed; reference bug 798015).");
			Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "proxy_port"), proxy_portConfig, "The RHN port component from the httpProxy configuration in "+clienttasks.rhnUp2dateFile+" has been copied to the RHSM server.proxy_port configuration in "+clienttasks.rhsmConfFile+".");
			if (clienttasks.getConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxyAuth").equals("0") || clienttasks.getConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxyAuth").equalsIgnoreCase("false")) {
				Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "proxy_user"), "", "The RHSM server.proxy_user configuration in "+clienttasks.rhsmConfFile+" is removed when RHN configuration enableProxyAuth is false.");
				Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "proxy_password"), "", "The RHSM server.proxy_password configuration in "+clienttasks.rhsmConfFile+" is removed when RHN configuration enableProxyAuth is false.");
			} else {
				Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "proxy_user"), proxy_userConfig, "The RHN proxyUser configuration in "+clienttasks.rhnUp2dateFile+" has been copied to the RHSM server.proxy_user configuration in "+clienttasks.rhsmConfFile+" when RHN configuration enableProxyAuth is true.");
				Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "proxy_password"), proxy_passwordConfig, "The RHN proxyPassword configuration in "+clienttasks.rhnUp2dateFile+" has been copied to the RHSM server.proxy_password configuration in "+clienttasks.rhsmConfFile+" when RHN configuration enableProxyAuth is true.");
			}
		}
		
		// assert we are no longer registered to RHN Classic
		// Two possible results can occur when the rhn-migrate-classic-to-rhsm script attempts to unregister from RHN Classic.  We need to tolerate both cases... 
		String successfulUnregisterMsg = "System successfully unregistered from RHN Classic.";
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) successfulUnregisterMsg = "System successfully unregistered from legacy server.";	// changed by commit 1355f8a656271ed57d6e35a2020a47cbf4adefd5
		String unsuccessfulUnregisterMsg = "Did not receive a completed unregistration message from RHN Classic for system "+rhnSystemId+"."+"\n"+"Please investigate on the Customer Portal at https://access.redhat.com.";
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) unsuccessfulUnregisterMsg = "Did not receive a completed unregistration message from legacy server for system "+rhnSystemId+".";	// changed by commit 1355f8a656271ed57d6e35a2020a47cbf4adefd5
		if (sshCommandResult.getStdout().contains(successfulUnregisterMsg)) {
			// Case 1: number of subscribed channels is low and all communication completes in a timely fashion.  Here is a snippet from stdout:
			//		Preparing to unregister system from RHN Classic ...
			//		System successfully unregistered from RHN Classic.
			Assert.assertTrue(sshCommandResult.getStdout().contains(successfulUnregisterMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+successfulUnregisterMsg);
			Assert.assertTrue(!sshCommandResult.getStdout().contains(unsuccessfulUnregisterMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' does NOT contain message: "+unsuccessfulUnregisterMsg);
			Assert.assertTrue(!clienttasks.isRhnSystemRegistered(),"This system is NOT registered using RHN Classic. Therefore this system will no longer communicate with RHN Classic.");
			iptablesAcceptPort(candlepinServerPort);	// without this, isRhnSystemIdRegistered(...) fails with Unexpected error: [Errno 111] Connection refused
			Assert.assertTrue(!clienttasks.isRhnSystemIdRegistered(rhnreg_ksUsername, rhnreg_ksPassword, rhnHostname, rhnSystemId), "Confirmed that rhn systemId '"+rhnSystemId+"' is no longer registered on the RHN Classic server.");
		} else {
			// Case 2: number of subscribed channels is high and communication fails in a timely fashion (see bug 881952).  Here is a snippet from stdout:	
			//		Preparing to unregister system from RHN Classic ...
			//		Did not receive a completed unregistration message from RHN Classic for system 1023722557.
			//		Please investigate on the Customer Portal at https://access.redhat.com.
			log.warning("Did not detect expected message '"+successfulUnregisterMsg+"' from "+rhnMigrateTool+" stdout.  Nevertheless, the tool should inform us and continue the migration process.");
			Assert.assertTrue(sshCommandResult.getStdout().contains(unsuccessfulUnregisterMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+unsuccessfulUnregisterMsg);
			Assert.assertTrue(!clienttasks.isRhnSystemRegistered(),"This system is NOT registered using RHN Classic. Therefore this system will no longer communicate with RHN Classic.");
			iptablesAcceptPort(candlepinServerPort);	// without this, isRhnSystemIdRegistered(...) fails with Unexpected error: [Errno 111] Connection refused
			if (!clienttasks.isRhnSystemIdRegistered(rhnreg_ksUsername, rhnreg_ksPassword, rhnHostname, rhnSystemId)) {
				Assert.assertFalse(false, "Confirmed that rhn systemId '"+rhnSystemId+"' is no longer registered on the RHN Classic server.");
			} else {
				log.warning("The RHN Classic server believes that this system is still registered.  SystemId '"+rhnSystemId+"' should be manually deleted on the Customer Portal.");
			}
		}
		
		// assert that we are newly registered using rhsm
		clienttasks.identity(null, null, null, null, null, null, null, null);
		Assert.assertNotNull(clienttasks.getCurrentConsumerId(),"The existance of a consumer cert indicates that the system is currently registered using RHSM.");
		expectedMsg = String.format("System '%s' successfully registered to Red Hat Subscription Management.",	clienttasks.hostname);
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) expectedMsg = String.format("System '%s' successfully registered.",	clienttasks.hostname); // changed by commit fad3de89
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "1451003"; // Bug 1451003 - subscription-manager identity reports redundant UUID info in the name field
		boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping assertion that Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+expectedMsg);
		} else
		// END OF WORKAROUND
		Assert.assertTrue(sshCommandResult.getStdout().contains(expectedMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+expectedMsg);
		
		log.info("No need to assert any more details of the migration since they are covered in the non-proxy test.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21895", "RHEL7-51751"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Execute migration tool rhn-migrate-classic-to-rhsm with a non-default rhsm.productcertdir configured",
			groups={"Tier3Tests","RhnMigrateClassicToRhsmWithNonDefaultProductCertDir_Test","blockedbyBug-878986","blockedByBug-966745","blockedByBug-1111258"},
			dependsOnMethods={},
			dataProvider="RhnMigrateClassicToRhsmWithNonDefaultProductCertDirData",	// dataProvider="RhnMigrateClassicToRhsmData",  IS TOO TIME CONSUMING
			enabled=true)
	@ImplementsNitrateTest(caseId=130765)
	public void testRhnMigrateClassicToRhsmWithNonDefaultProductCertDir(Object bugzilla, String rhnreg_ksUsername, String rhnreg_ksPassword, String rhnServer, List<String> rhnChannelsToAdd, String options, String rhnUsername, String rhnPassword, String rhsmUsername, String rhsmPassword, String rhsmOrg, Integer serviceLevelIndex, String serviceLevelExpected) throws JSONException {
		// NOTE: The configNonDefaultRhsmProductCertDir will handle the configuration setting
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "productCertDir"), nonDefaultProductCertDir,"A non-default rhsm.productCertDir has been configured.");
		testRhnMigrateClassicToRhsm(bugzilla,rhnreg_ksUsername,rhnreg_ksPassword,rhnServer,rhnChannelsToAdd,options,rhnUsername,rhnPassword,rhsmUsername,rhsmPassword,rhsmOrg,serviceLevelIndex,serviceLevelExpected);
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20109", "RHEL7-51742"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Execute migration tool rhn-migrate-classic-to-rhsm with a valid activation-key (and a good org)",
			groups={"Tier1Tests","blockedByBug-1154375","blockedByBug-1512948","blockedByBug-1516832"},
			enabled=true)
	@ImplementsNitrateTest(caseId=130765)
	public void testRhnMigrateClassicToRhsmWithActivationKey() throws Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.14.1-1")) throw new SkipException("The --activation-key option was not implemented in this version of subscription-manager.");
		
		// create a valid activation key
		SubscriptionPool pool = getRandomListItem(availableSubscriptionPools);	// randomly choose a valid available pool for this key
		String activationKeyName = String.format("activationKeyForOrg_%s_Pool_%s", clientOrgKey,pool.productId);	// choose an activationKey name
		JSONObject jsonActivationKey = CandlepinTasks.createActivationKeyUsingRESTfulAPI(sm_clientUsername,sm_clientPassword, sm_serverUrl,clientOrgKey, activationKeyName, Arrays.asList(pool.poolId), null);		
		
		// register with the activation key
		if (false) { // debugTesting
			clienttasks.register_(null, null, clientOrgKey, null, null, null, null, null, null, null, activationKeyName, null, null, null, true, null, null, null, null, null);
			clienttasks.unregister_(null, null, null, null);
		}
		
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "1196416"; // Bug 1196416 - rhn-migrate-classic-to-rhsm with --activation-key option should not prompt for destination credentials 
		boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			testRhnMigrateClassicToRhsm(null,sm_rhnUsername,sm_rhnPassword,sm_rhnHostname,new ArrayList<String>(),"--activation-key="+activationKeyName+" "+"--org="+clientOrgKey, sm_rhnUsername,sm_rhnPassword,sm_clientUsername,sm_clientPassword,null,null,null);
		} else	// call RhnMigrateClassicToRhsm_Test with rhsmUsername=null and rhsmPassword=null
		// END OF WORKAROUND
		
		// migrate from RHN Classic to RHSM using the activation key 
		testRhnMigrateClassicToRhsm(null,sm_rhnUsername,sm_rhnPassword,sm_rhnHostname,new ArrayList<String>(),"--activation-key="+activationKeyName+" "+"--org="+clientOrgKey, sm_rhnUsername,sm_rhnPassword,null,null,null,null,null);
		
		// assert that the system is consuming the pool from the activation key.
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(consumedProductSubscriptions.size(), 1, "Number of consumed subscriptions after migrating from RHN Classic to RHSM with activation key '"+activationKeyName+"'.");
		Assert.assertEquals(consumedProductSubscriptions.get(0).poolId, pool.poolId, "The sole consumed subscription poolId after migrating from RHN Classic to RHSM with activation key '"+activationKeyName+"'.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21887", "RHEL7-51743"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Execute migration tool rhn-migrate-classic-to-rhsm with a bad activation-key (and a good org)",
			groups={"Tier3Tests","blockedByBug-1154375"},
			enabled=true)
	@ImplementsNitrateTest(caseId=130765)
	public void testRhnMigrateClassicToRhsmWithBadActivationKey() throws Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.14.1-1")) throw new SkipException("The --activation-key option was not implemented in this version of subscription-manager.");
		
		if (sm_rhnHostname.equals("")) throw new SkipException("This test requires access to RHN Classic or Satellite 5.");
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7 && clienttasks.arch.equals("ppc64le")) throw new SkipException("Use of rhn-migrate-classic-to-rhsm is not necessary on RHEL '"+client1tasks.redhatReleaseX+"' arch '"+clienttasks.arch+"' since this product was not released on RHN Classic.");
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7 && clienttasks.arch.equals("aarch64")) throw new SkipException("Use of rhn-migrate-classic-to-rhsm is not necessary on RHEL '"+client1tasks.redhatReleaseX+"' arch '"+clienttasks.arch+"' since this product was not released on RHN Classic.");
		
		clienttasks.unregister_(null,null,null, null);
		clienttasks.removeAllCerts(true,true,true);
		clienttasks.removeAllFacts();
		restoreOriginallyConfiguredServerUrl();
		
		// register to RHN Classic
		String rhnSystemId = clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, rhnSystemId),"Confirmed that rhn systemId '"+rhnSystemId+"' is currently registered.");
		
		// choose a bad activation key
		String activationKeyName = "badActivationKey";
		
		// execute rhn-migrate-classic-to-rhsm with options
		SSHCommandResult executeRhnMigrateClassicToRhsmResult;
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "1196416"; // Bug 1196416 - rhn-migrate-classic-to-rhsm with --activation-key option should not prompt for destination credentials 
		boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			executeRhnMigrateClassicToRhsmResult = executeRhnMigrateClassicToRhsm("--activation-key="+activationKeyName+" "+"--org="+clientOrgKey, sm_rhnUsername,sm_rhnPassword,sm_clientUsername,sm_clientPassword,null,null,null);
		} else	// call executeRhnMigrateClassicToRhsm with rhsmUsername=null and rhsmPassword=null
		// END OF WORKAROUND
		executeRhnMigrateClassicToRhsmResult = executeRhnMigrateClassicToRhsm("--activation-key="+activationKeyName+" "+"--org="+clientOrgKey, sm_rhnUsername,sm_rhnPassword,null,null,null,null,null);
		
		//	201502272153:09.897 - FINE: ssh root@jsefler-os6.usersys.redhat.com rhn-migrate-classic-to-rhsm.tcl --activation-key=badActivationKey qa@redhat.com REDACTED testuser1 password admin null null
		//	201502272153:12.049 - FINE: Stdout: 
		//	spawn rhn-migrate-classic-to-rhsm --activation-key=badActivationKey
		//	Legacy username: qa@redhat.com
		//	Legacy password: 
		//	Destination username: testuser1
		//	Destination password: 
		//	Org: admin
		//
		//	Retrieving existing legacy subscription information...
		//
		//	+-----------------------------------------------------+
		//	System is currently subscribed to these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-server-6
		//
		//	+-----------------------------------------------------+
		//	Installing product certificates for these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-server-6
		//
		//	Product certificates installed successfully to /etc/pki/product.
		//
		//	Preparing to unregister system from legacy server...
		//	System successfully unregistered from legacy server.
		//
		//	Attempting to register system to destination server...
		//	None of the activation keys specified exist for this org.
		//
		//	Unable to register.
		//	For further assistance, please contact Red Hat Global Support Services.
		
		// assert the result
		String expectedFailureMessage = String.format("Activation key '%s' not found for organization '%s'.",activationKeyName, clientOrgKey);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">", "0.9.30-1")) expectedFailureMessage = String.format("None of the activation keys specified exist for this org.");	// Follows: candlepin-0.9.30-1	// https://github.com/candlepin/candlepin/commit/bcb4b8fd8ee009e86fc9a1a20b25f19b3dbe6b2a
		expectedFailureMessage += "\n\nUnable to register.\nFor further assistance, please contact Red Hat Global Support Services.";
		Assert.assertTrue(executeRhnMigrateClassicToRhsmResult.getStdout().contains(expectedFailureMessage),"The result from an attempt to migrate from RHN Classic to RHSM with a bad activation key reported this expected messge: \n"+expectedFailureMessage);
		
		// assert that the system is not registered
		Assert.assertNull(clienttasks.getCurrentConsumerCert(),"Confirmed that system is NOT registered with Subscription Manager after an attempt to migrate from RHN Classic using activation key '"+activationKeyName+"'.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21888", "RHEL7-51744"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Execute migration tool rhn-migrate-classic-to-rhsm with valid comma separated keys (and a good org)",
			groups={"Tier3Tests","blockedByBug-1154375"},
			enabled=true)
	@ImplementsNitrateTest(caseId=130765)
	public void testRhnMigrateClassicToRhsmWithCommaSeparatedActivationKeys() throws Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.14.1-1")) throw new SkipException("The --activation-key option was not implemented in this version of subscription-manager.");
		
		// create valid activation keys
		SubscriptionPool pool1 = getRandomListItem(availableSubscriptionPools);	// randomly choose a valid available pool for this key
		String activationKeyName1 = String.format("activationKeyForOrg_%s_Pool_%s", clientOrgKey,pool1.productId);	// choose an activationKey name
		JSONObject jsonActivationKey1 = CandlepinTasks.createActivationKeyUsingRESTfulAPI(sm_clientUsername,sm_clientPassword, sm_serverUrl,clientOrgKey, activationKeyName1, Arrays.asList(pool1.poolId), null);		
		SubscriptionPool pool2 = getRandomListItem(availableSubscriptionPools);	// randomly choose a valid available pool for this key
		String activationKeyName2 = String.format("activationKeyForOrg_%s_Pool_%s", clientOrgKey,pool2.productId);	// choose an activationKey name
		JSONObject jsonActivationKey2 = CandlepinTasks.createActivationKeyUsingRESTfulAPI(sm_clientUsername,sm_clientPassword, sm_serverUrl,clientOrgKey, activationKeyName2, Arrays.asList(pool2.poolId), null);		
		String name = activationKeyName1+","+activationKeyName2;	// comma separated name
		
		// register with the activation key
		if (false) { // debugTesting
			clienttasks.register_(null, null, clientOrgKey, null, null, null, null, null, null, null, name, null, null, null, true, null, null, null, null, null);
			clienttasks.unregister_(null, null, null, null);
		}
		
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "1196416"; // Bug 1196416 - rhn-migrate-classic-to-rhsm with --activation-key option should not prompt for destination credentials 
		boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			testRhnMigrateClassicToRhsm(null,sm_rhnUsername,sm_rhnPassword,sm_rhnHostname,new ArrayList<String>(),"--activation-key="+name+" "+"--org="+clientOrgKey, sm_rhnUsername,sm_rhnPassword,sm_clientUsername,sm_clientPassword,clientOrgKey,null,null);
		} else	// call RhnMigrateClassicToRhsm_Test with rhsmUsername=null and rhsmPassword=null
		// END OF WORKAROUND
		
		// migrate from RHN Classic to RHSM using the activation key 
		testRhnMigrateClassicToRhsm(null,sm_rhnUsername,sm_rhnPassword,sm_rhnHostname,new ArrayList<String>(),"--activation-key="+name+" "+"--org="+clientOrgKey, sm_rhnUsername,sm_rhnPassword,null,null,clientOrgKey,null,null);
		
		// assert that the system is consuming the pools from the activation key.
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertNotNull(ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", pool1.poolId, consumedProductSubscriptions), "Found consumed subscription from pool '"+pool1.poolId+"' after migrating with activation keys '"+name+"'.");
		Assert.assertNotNull(ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", pool2.poolId, consumedProductSubscriptions), "Found consumed subscription from pool '"+pool2.poolId+"' after migrating with activation keys '"+name+"'.");
		int expectedNumberOfConsumedSubscription = 2;
		if (activationKeyName1.equals(activationKeyName2)) expectedNumberOfConsumedSubscription=1;
		Assert.assertEquals(consumedProductSubscriptions.size(), expectedNumberOfConsumedSubscription, "Number of consumed subscriptions after migrating from RHN Classic to RHSM with activation key '"+name+"'.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21893", "RHEL7-51749"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Execute migration tool rhn-migrate-classic-to-rhsm with multiple --activation-key options specified",
			groups={"Tier3Tests","blockedByBug-1154375"},
			enabled=true)
	@ImplementsNitrateTest(caseId=130765)
	public void testRhnMigrateClassicToRhsmWithMultipleActivationKeys() throws Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.14.1-1")) throw new SkipException("The --activation-key option was not implemented in this version of subscription-manager.");
		
		// create valid activation keys
		SubscriptionPool pool1 = getRandomListItem(availableSubscriptionPools);	// randomly choose a valid available pool for this key
		String name1 = String.format("activationKeyForOrg_%s_Pool_%s", clientOrgKey,pool1.productId);	// choose an activationKey name
		JSONObject jsonActivationKey1 = CandlepinTasks.createActivationKeyUsingRESTfulAPI(sm_clientUsername,sm_clientPassword, sm_serverUrl,clientOrgKey, name1, Arrays.asList(pool1.poolId), null);		
		SubscriptionPool pool2 = getRandomListItem(availableSubscriptionPools);	// randomly choose a valid available pool for this key
		String name2 = String.format("activationKeyForOrg_%s_Pool_%s", clientOrgKey,pool2.productId);	// choose an activationKey name
		JSONObject jsonActivationKey2 = CandlepinTasks.createActivationKeyUsingRESTfulAPI(sm_clientUsername,sm_clientPassword, sm_serverUrl,clientOrgKey, name2, Arrays.asList(pool2.poolId), null);		
		
		// register with the activation keys
		if (false) { // debugTesting
			clienttasks.register_(null, null, clientOrgKey, null, null, null, null, null, null, null, Arrays.asList(name1,name2), null, null, null, true, null, null, null, null, null);
			clienttasks.unregister_(null, null, null, null);
		}
		
		// migrate from RHN Classic to RHSM using the activation key 
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "1196416"; // Bug 1196416 - rhn-migrate-classic-to-rhsm with --activation-key option should not prompt for destination credentials 
		boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			testRhnMigrateClassicToRhsm(null,sm_rhnUsername,sm_rhnPassword,sm_rhnHostname,new ArrayList<String>(),"--activation-key="+name1+" "+"--activation-key="+name2,sm_rhnUsername,sm_rhnPassword,sm_clientUsername,sm_clientPassword,clientOrgKey,null,null);
		} else	// call RhnMigrateClassicToRhsm_Test with rhsmUsername=null and rhsmPassword=null
		// END OF WORKAROUND
		testRhnMigrateClassicToRhsm(null,sm_rhnUsername,sm_rhnPassword,sm_rhnHostname,new ArrayList<String>(),"--activation-key="+name1+" "+"--activation-key="+name2+" "+"--org="+clientOrgKey,sm_rhnUsername,sm_rhnPassword,null,null,null,null,null);
		
		// assert that the system is consuming the pools from the activation key.
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertNotNull(ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", pool1.poolId, consumedProductSubscriptions), "Found consumed subscription from pool '"+pool1.poolId+"' after migrating with activation keys '"+name1+"' and '"+name2+"'.");
		Assert.assertNotNull(ProductSubscription.findFirstInstanceWithMatchingFieldFromList("poolId", pool2.poolId, consumedProductSubscriptions), "Found consumed subscription from pool '"+pool2.poolId+"' after migrating with activation keys '"+name1+"' and '"+name2+"'.");
		Assert.assertEquals(consumedProductSubscriptions.size(), 2, "Number of consumed subscriptions after migrating from RHN Classic to RHSM with activation keys '"+name1+"' and '"+name2+"'.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-26764", "RHEL7-55205"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="migrating a RHEL5 Client - Desktop versus Workstation",
			groups={"Tier1Tests","RhnMigrateClassicToRhsm_Test","blockedByBug-786257","blockedByBug-853233","blockedByBug-1111258"},
			dependsOnMethods={},
			enabled=true)
	public void testRhnMigrateClassicToRhsm_Rhel5ClientDesktopVersusWorkstation() throws JSONException {
		if (sm_rhnHostname.equals("")) throw new SkipException("This test requires access to RHN Classic or Satellite 5.");
		
		log.info("Red Hat Enterprise Linux Desktop (productId=68) corresponds to the base RHN Channel (rhel-ARCH-client-5) for a 5Client system where ARCH=i386,x86_64.");
		log.info("Red Hat Enterprise Linux Workstation (productId=71) corresponds to child RHN Channel (rhel-ARCH-client-workstation-5) for a 5Client system where ARCH=i386,x86_64.");	
		log.info("After migrating from RHN Classic to RHSM, these two product certs should not be installed at the same time; Workstation shoul prevail.");

		// when we are migrating away from RHN Classic to a non-hosted candlepin server, choose the credentials that will be used to register
		String rhsmUsername=null, rhsmPassword=null, rhsmOrg=null;
		if (!isCurrentlyConfiguredServerTypeHosted()) {	// or this may work too: if (!sm_serverType.equals(CandlepinType.hosted)) {
			rhsmUsername = sm_clientUsername;
			rhsmPassword = sm_clientPassword;
			rhsmOrg = sm_clientOrg;
		}
		
		//	2273         "Name": "Red Hat Enterprise Linux Desktop", 
		//	2274         "Product ID": "68", 
		//	2275         "RHN Channels": [
		//	2276             "rhel-i386-client-5", 
		//	2277             "rhel-i386-client-5-beta", 
		//	2278             "rhel-i386-client-5-beta-debuginfo", 
		//	2279             "rhel-i386-client-5-debuginfo", 
		//	2280             "rhel-i386-client-6", 
		//	2281             "rhel-i386-client-6-beta", 
		//	2282             "rhel-i386-client-6-beta-debuginfo", 
		//	2283             "rhel-i386-client-6-debuginfo", 
		//	2284             "rhel-i386-client-optional-6", 
		//	2285             "rhel-i386-client-optional-6-beta", 
		//	2286             "rhel-i386-client-optional-6-beta-debuginfo", 
		//	2287             "rhel-i386-client-optional-6-debuginfo", 
		//	2288             "rhel-i386-client-supplementary-5", 
		//	2289             "rhel-i386-client-supplementary-5-beta", 
		//	2290             "rhel-i386-client-supplementary-5-beta-debuginfo", 
		//	2291             "rhel-i386-client-supplementary-5-debuginfo", 
		//	2292             "rhel-i386-client-supplementary-6", 
		//	2293             "rhel-i386-client-supplementary-6-beta", 
		//	2294             "rhel-i386-client-supplementary-6-beta-debuginfo", 
		//	2295             "rhel-i386-client-supplementary-6-debuginfo", 
		//	2296             "rhel-i386-rhev-agent-5-client", 
		//	2297             "rhel-i386-rhev-agent-5-client-beta", 
		//	2298             "rhel-i386-rhev-agent-6-client", 
		//	2299             "rhel-i386-rhev-agent-6-client-beta", 
		//	2300             "rhel-i386-rhev-agent-6-client-beta-debuginfo", 
		//	2301             "rhel-i386-rhev-agent-6-client-debuginfo", 
		//	2302             "rhel-x86_64-client-5", 
		//	2303             "rhel-x86_64-client-5-beta", 
		//	2304             "rhel-x86_64-client-5-beta-debuginfo", 
		//	2305             "rhel-x86_64-client-5-debuginfo", 
		//	2306             "rhel-x86_64-client-6", 
		//	2307             "rhel-x86_64-client-6-beta", 
		//	2308             "rhel-x86_64-client-6-beta-debuginfo", 
		//	2309             "rhel-x86_64-client-6-debuginfo", 
		//	2310             "rhel-x86_64-client-optional-6", 
		//	2311             "rhel-x86_64-client-optional-6-beta", 
		//	2312             "rhel-x86_64-client-optional-6-beta-debuginfo", 
		//	2313             "rhel-x86_64-client-optional-6-debuginfo", 
		//	2314             "rhel-x86_64-client-supplementary-5", 
		//	2315             "rhel-x86_64-client-supplementary-5-beta", 
		//	2316             "rhel-x86_64-client-supplementary-5-beta-debuginfo", 
		//	2317             "rhel-x86_64-client-supplementary-5-debuginfo", 
		//	2318             "rhel-x86_64-client-supplementary-6", 
		//	2319             "rhel-x86_64-client-supplementary-6-beta", 
		//	2320             "rhel-x86_64-client-supplementary-6-beta-debuginfo", 
		//	2321             "rhel-x86_64-client-supplementary-6-debuginfo", 
		//	2322             "rhel-x86_64-rhev-agent-5-client", 
		//	2323             "rhel-x86_64-rhev-agent-5-client-beta", 
		//	2324             "rhel-x86_64-rhev-agent-6-client", 
		//	2325             "rhel-x86_64-rhev-agent-6-client-beta", 
		//	2326             "rhel-x86_64-rhev-agent-6-client-beta-debuginfo", 
		//	2327             "rhel-x86_64-rhev-agent-6-client-debuginfo"
		//	2328         ]
		
		//	10289         "Name": "Red Hat Enterprise Linux Workstation", 
		//	10290         "Product ID": "71", 
		//	10291         "RHN Channels": [
		//	10292             "rhel-i386-client-5", 
		//	10293             "rhel-i386-client-5-beta", 
		//	10294             "rhel-i386-client-5-beta-debuginfo", 
		//	10295             "rhel-i386-client-5-debuginfo", 
		//	10296             "rhel-i386-client-supplementary-5", 
		//	10297             "rhel-i386-client-supplementary-5-beta", 
		//	10298             "rhel-i386-client-supplementary-5-beta-debuginfo", 
		//	10299             "rhel-i386-client-supplementary-5-debuginfo", 
		//	10300             "rhel-i386-client-vt-5", 
		//	10301             "rhel-i386-client-vt-5-beta", 
		//	10302             "rhel-i386-client-vt-5-beta-debuginfo", 
		//	10303             "rhel-i386-client-vt-5-debuginfo", 
		//	10304             "rhel-i386-client-workstation-5", 
		//	10305             "rhel-i386-client-workstation-5-beta", 
		//	10306             "rhel-i386-client-workstation-5-beta-debuginfo", 
		//	10307             "rhel-i386-client-workstation-5-debuginfo", 
		//	10308             "rhel-i386-rhev-agent-6-workstation", 
		//	10309             "rhel-i386-rhev-agent-6-workstation-beta", 
		//	10310             "rhel-i386-rhev-agent-6-workstation-beta-debuginfo", 
		//	10311             "rhel-i386-rhev-agent-6-workstation-debuginfo", 
		//	10312             "rhel-i386-workstation-6", 
		//	10313             "rhel-i386-workstation-6-beta", 
		//	10314             "rhel-i386-workstation-6-beta-debuginfo", 
		//	10315             "rhel-i386-workstation-6-debuginfo", 
		//	10316             "rhel-i386-workstation-optional-6", 
		//	10317             "rhel-i386-workstation-optional-6-beta", 
		//	10318             "rhel-i386-workstation-optional-6-beta-debuginfo", 
		//	10319             "rhel-i386-workstation-optional-6-debuginfo", 
		//	10320             "rhel-i386-workstation-supplementary-6", 
		//	10321             "rhel-i386-workstation-supplementary-6-beta", 
		//	10322             "rhel-i386-workstation-supplementary-6-beta-debuginfo", 
		//	10323             "rhel-i386-workstation-supplementary-6-debuginfo", 
		//	10324             "rhel-x86_64-client-5", 
		//	10325             "rhel-x86_64-client-5-beta", 
		//	10326             "rhel-x86_64-client-5-beta-debuginfo", 
		//	10327             "rhel-x86_64-client-5-debuginfo", 
		//	10328             "rhel-x86_64-client-supplementary-5", 
		//	10329             "rhel-x86_64-client-supplementary-5-beta", 
		//	10330             "rhel-x86_64-client-supplementary-5-beta-debuginfo", 
		//	10331             "rhel-x86_64-client-supplementary-5-debuginfo", 
		//	10332             "rhel-x86_64-client-vt-5", 
		//	10333             "rhel-x86_64-client-vt-5-beta", 
		//	10334             "rhel-x86_64-client-vt-5-beta-debuginfo", 
		//	10335             "rhel-x86_64-client-vt-5-debuginfo", 
		//	10336             "rhel-x86_64-client-workstation-5", 
		//	10337             "rhel-x86_64-client-workstation-5-beta", 
		//	10338             "rhel-x86_64-client-workstation-5-beta-debuginfo", 
		//	10339             "rhel-x86_64-client-workstation-5-debuginfo", 
		//	10340             "rhel-x86_64-rhev-agent-6-workstation", 
		//	10341             "rhel-x86_64-rhev-agent-6-workstation-beta", 
		//	10342             "rhel-x86_64-rhev-agent-6-workstation-beta-debuginfo", 
		//	10343             "rhel-x86_64-rhev-agent-6-workstation-debuginfo", 
		//	10344             "rhel-x86_64-workstation-6", 
		//	10345             "rhel-x86_64-workstation-6-beta", 
		//	10346             "rhel-x86_64-workstation-6-beta-debuginfo", 
		//	10347             "rhel-x86_64-workstation-6-debuginfo", 
		//	10348             "rhel-x86_64-workstation-optional-6", 
		//	10349             "rhel-x86_64-workstation-optional-6-beta", 
		//	10350             "rhel-x86_64-workstation-optional-6-beta-debuginfo", 
		//	10351             "rhel-x86_64-workstation-optional-6-debuginfo", 
		//	10352             "rhel-x86_64-workstation-supplementary-6", 
		//	10353             "rhel-x86_64-workstation-supplementary-6-beta", 
		//	10354             "rhel-x86_64-workstation-supplementary-6-beta-debuginfo", 
		//	10355             "rhel-x86_64-workstation-supplementary-6-debuginfo"
		//	10356         ]
		
		// this test is only applicable on a RHEL 5Client
		final String applicableReleasever = "5Client";
		if (!clienttasks.releasever.equals(applicableReleasever)) throw new SkipException("This test is only executable when the redhat-release is '"+applicableReleasever+"'.");
		
		// decide what product arch applies to our system
		String arch = clienttasks.arch;	// default
		//if (clienttasks.redhatReleaseX.equals("5") && clienttasks.arch.equals("ppc64")) arch = "ppc";	// RHEL5 only supports ppc packages, but can be run on ppc64 hardware
		if (Arrays.asList("i386","i486","i586","i686").contains(clienttasks.arch)) arch = "i386";		// RHEL supports i386 packages, but can be run on all 32-bit arch hardware
		if (!Arrays.asList("i386","x86_64").contains(arch)) Assert.fail("RHEL "+applicableReleasever+" should only be available on i386 and x86_64 arches (not: "+arch+").") ;
		
		
		// Case 1: add RHN Channels for Desktop only; migration should only install Desktop product 68
		List<String> rhnChannelsToAddForDesktop = new ArrayList<String>();
		//rhnChannelsToAdd.add(String.format("rhel-%s-client-5",arch));	// this is the base channel and will already be consumed by rhnreg_ks
		rhnChannelsToAddForDesktop.add(String.format("rhel-%s-client-5-beta",arch));
		rhnChannelsToAddForDesktop.add(String.format("rhel-%s-client-5-beta-debuginfo",arch));
		rhnChannelsToAddForDesktop.add(String.format("rhel-%s-client-5-debuginfo",arch));
		rhnChannelsToAddForDesktop.add(String.format("rhel-%s-client-supplementary-5",arch));
		rhnChannelsToAddForDesktop.add(String.format("rhel-%s-client-supplementary-5-beta",arch));
		rhnChannelsToAddForDesktop.add(String.format("rhel-%s-client-supplementary-5-beta-debuginfo",arch));
		rhnChannelsToAddForDesktop.add(String.format("rhel-%s-client-supplementary-5-debuginfo",arch));
		testRhnMigrateClassicToRhsm(null,	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	rhnChannelsToAddForDesktop, "--no-auto", sm_rhnUsername,sm_rhnPassword,rhsmUsername,rhsmPassword,rhsmOrg,null, null);		
		List<ProductCert> productCertsMigrated = clienttasks.getCurrentProductCerts();
		String productIdForDesktop = "68";
		for (ProductCert productCert : productCertsMigrated) {
			Assert.assertEquals(productCert.productId, productIdForDesktop, "Migration tool "+rhnMigrateTool+" should only install product certificate id '"+productIdForDesktop+"' when consuming RHN Child Channels "+rhnChannelsToAddForDesktop);
		}
		
		// Case 2: add RHN Channels for Workstation only; migration should only install Workstation product 71
		List<String> rhnChannelsToAddForWorkstation = new ArrayList<String>();
		//rhnChannelsToAdd.add(String.format("rhel-%s-client-5",arch));	// this is the base channel and will already be consumed by rhnreg_ks
		/*
		rhnChannelsToAddForWorkstation.add(String.format("rhel-%s-client-vt-5",arch));
		rhnChannelsToAddForWorkstation.add(String.format("rhel-%s-client-vt-5-beta",arch));
		rhnChannelsToAddForWorkstation.add(String.format("rhel-%s-client-vt-5-beta-debuginfo",arch));
		rhnChannelsToAddForWorkstation.add(String.format("rhel-%s-client-vt-5-debuginfo",arch));
		*/
		rhnChannelsToAddForWorkstation.add(String.format("rhel-%s-client-workstation-5",arch));
		rhnChannelsToAddForWorkstation.add(String.format("rhel-%s-client-workstation-5-beta",arch));
		rhnChannelsToAddForWorkstation.add(String.format("rhel-%s-client-workstation-5-beta-debuginfo",arch));
		rhnChannelsToAddForWorkstation.add(String.format("rhel-%s-client-workstation-5-debuginfo",arch));
		testRhnMigrateClassicToRhsm(null,	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	rhnChannelsToAddForWorkstation, "--no-auto", sm_rhnUsername,sm_rhnPassword,rhsmUsername,rhsmPassword,rhsmOrg,null, null);		
		productCertsMigrated = clienttasks.getCurrentProductCerts();
		String productIdForWorkstation = "71";
		for (ProductCert productCert : productCertsMigrated) {
			Assert.assertEquals(productCert.productId, productIdForWorkstation, "Migration tool "+rhnMigrateTool+" should only install product certificate id '"+productIdForWorkstation+"' when consuming RHN Child Channels "+rhnChannelsToAddForWorkstation);
		}
		
		// Case 3: add RHN Channels for Virtualization only; migration should only install Workstation product 71
		// Bug 853233 - rhn-migrate-classic-to-rhsm is installing both Desktop(68) and Workstation(71) when rhel-ARCH-client-vt-5 channel is consumed 
		List<String> rhnChannelsToAddForVirtualization = new ArrayList<String>();
		//rhnChannelsToAdd.add(String.format("rhel-%s-client-5",arch));	// this is the base channel and will already be consumed by rhnreg_ks
		rhnChannelsToAddForVirtualization.add(String.format("rhel-%s-client-vt-5",arch));
		rhnChannelsToAddForVirtualization.add(String.format("rhel-%s-client-vt-5-beta",arch));
		rhnChannelsToAddForVirtualization.add(String.format("rhel-%s-client-vt-5-beta-debuginfo",arch));
		rhnChannelsToAddForVirtualization.add(String.format("rhel-%s-client-vt-5-debuginfo",arch));
		testRhnMigrateClassicToRhsm(null,	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	rhnChannelsToAddForVirtualization, "--no-auto", sm_rhnUsername,sm_rhnPassword,rhsmUsername,rhsmPassword,rhsmOrg,null, null);		
		productCertsMigrated = clienttasks.getCurrentProductCerts();
		/*String*/ productIdForWorkstation = "71";
		for (ProductCert productCert : productCertsMigrated) {
			Assert.assertEquals(productCert.productId, productIdForWorkstation, "Migration tool "+rhnMigrateTool+" should only install product certificate id '"+productIdForWorkstation+"' when consuming RHN Child Channels "+rhnChannelsToAddForVirtualization);
		}
		
		// Case 4: add RHN Channels for both Desktop and Workstation; migration should only install Workstation product 71
		List<String> rhnChannelsToAddForBoth = new ArrayList<String>();
		rhnChannelsToAddForBoth.addAll(rhnChannelsToAddForDesktop);
		rhnChannelsToAddForBoth.addAll(rhnChannelsToAddForWorkstation);
		rhnChannelsToAddForBoth.addAll(rhnChannelsToAddForVirtualization);
		testRhnMigrateClassicToRhsm(null,	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	rhnChannelsToAddForBoth, "--no-auto", sm_rhnUsername,sm_rhnPassword,rhsmUsername,rhsmPassword,rhsmOrg,null, null);		
		productCertsMigrated = clienttasks.getCurrentProductCerts();
		for (ProductCert productCert : productCertsMigrated) {
			Assert.assertEquals(productCert.productId, productIdForWorkstation, "Migration tool "+rhnMigrateTool+" should only install product certificate id '"+productIdForWorkstation+"' when consuming RHN Child Channels "+rhnChannelsToAddForBoth);
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21897", "RHEL7-98222"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="when more than one JBoss Application Enterprise Platform (JBEAP) RHN Channel is currently being consumed classically, rhn-migrate-to-rhsm should abort",
			groups={"Tier3Tests","blockedByBug-852894","blockedByBug-1052297","RhnMigrateClassicToRhsm_Test"},
			dependsOnMethods={},
			enabled=true)
	public void testRhnMigrateClassicToRhsm_MultipleVersionsOfJBEAP
	() {
		if (sm_rhnHostname.equals("")) throw new SkipException("This test requires access to RHN Classic or Satellite 5.");

		log.info("JBoss Enterprise Application Platform (productId=183) is currently provided in 3 versions: 4.3.0, 5.0, 6.0");
		log.info("If RHN Channels providing more than one of these versions is currently being consumed, rhn-migrate-to-rhsm should abort.");

		// when we are migrating away from RHN Classic to a non-hosted candlepin server, choose the credentials that will be used to register
		String rhsmUsername=null, rhsmPassword=null, rhsmOrg=null;
		if (!isCurrentlyConfiguredServerTypeHosted()) {	// or this may work too: if (!sm_serverType.equals(CandlepinType.hosted)) {
			rhsmUsername = sm_clientUsername;
			rhsmPassword = sm_clientPassword;
			rhsmOrg = sm_clientOrg;
		}
		
		//	32298         "Name": "JBoss Enterprise Application Platform", 
		//	32299         "Product ID": "183", 
		//	32300         "RHN Channels": [
		//	32301             "jbappplatform-4.3.0-i386-server-5-rpm", 
		//	32302             "jbappplatform-4.3.0-x86_64-server-5-rpm", 
		//	32303             "jbappplatform-5-i386-server-5-rpm", 
		//	32304             "jbappplatform-5-i386-server-6-rpm", 
		//	32305             "jbappplatform-5-x86_64-server-5-rpm", 
		//	32306             "jbappplatform-5-x86_64-server-6-rpm", 
		//	32307             "jbappplatform-6-i386-server-6-rpm", 
		//	32308             "jbappplatform-6-x86_64-server-6-rpm"
		//	32309         ]
		
		// this test is only applicable on a RHEL 5Server,6Server and arches i386,x86_64
		List<String> applicableReleasevers = Arrays.asList(new String[]{"5Server","6Server"});
		List<String> applicableArchs = Arrays.asList(new String[]{"i386","x86_64"});
		String arch = clienttasks.arch;	// default
		//if (clienttasks.redhatReleaseX.equals("5") && clienttasks.arch.equals("ppc64")) arch = "ppc";	// RHEL5 only supports ppc packages, but can be run on ppc64 hardware
		if (Arrays.asList("i386","i486","i586","i686").contains(clienttasks.arch)) arch = "i386";		// RHEL supports i386 packages, but can be run on all 32-bit arch hardware
		if (!applicableReleasevers.contains(clienttasks.releasever)) throw new SkipException("This test is only executable on redhat-releases "+applicableReleasevers+" arches "+applicableArchs);
		if (!applicableArchs.contains(arch)) throw new SkipException("This test is only executable on redhat-releases "+applicableReleasevers+" arches "+applicableArchs);
		
		List<String> rhnChannelsToAdd = new ArrayList<String>();
		
		// decide what jbappplatform channels to test
		if (clienttasks.redhatReleaseX.equals("5")) {
			rhnChannelsToAdd.add(String.format("jbappplatform-4.3.0-%s-server-5-rpm",arch));
			rhnChannelsToAdd.add(String.format("jbappplatform-5-%s-server-5-rpm",arch));
		} else if (clienttasks.redhatReleaseX.equals("6")) {
			rhnChannelsToAdd.add(String.format("jbappplatform-5-%s-server-6-rpm",arch));
			rhnChannelsToAdd.add(String.format("jbappplatform-6-%s-server-6-rpm",arch));
		} else {
			Assert.fail("This test needs additional RHN Channel information for jbappplatform product 183 on RHEL Release '"+clienttasks.redhatReleaseX+"'.");
		}
			
		// make sure we are NOT registered to RHSM
		clienttasks.unregister(null,null,null, null);
		clienttasks.removeAllCerts(false, false, true);
		clienttasks.removeAllFacts();
		
		// register to RHN Classic
		String rhnSystemId = clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, rhnSystemId),"Confirmed that rhn systemId '"+rhnSystemId+"' is currently registered.");
		
		// subscribe to more RHN Classic channels
		addRhnClassicChannels(sm_rhnUsername, sm_rhnPassword, rhnChannelsToAdd);
		List<String> rhnChannelsConsumed = clienttasks.getCurrentRhnClassicChannels();
		Assert.assertTrue(rhnChannelsConsumed.containsAll(rhnChannelsToAdd), "All of the RHN Classic channels added appear to be consumed.");

		// execute rhn-migrate-classic-to-rhsm and assert the results
		SSHCommandResult sshCommandResult = executeRhnMigrateClassicToRhsm(null,sm_rhnUsername, sm_rhnPassword,rhsmUsername,rhsmPassword,rhsmOrg,null, null);
		String expectedMsg = "You are subscribed to more than one jbappplatform channel.  This script does not support that configuration.  Exiting.";
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) expectedMsg = "You are subscribed to more than one jbappplatform channel.  This script does not support that configuration.";	// changed by commit c0f8052ec2b5b7b5c736eb626e381aef0e5327e5
		Assert.assertTrue(sshCommandResult.getStdout().contains(expectedMsg), "Stdout from call to '"+rhnMigrateTool+" when consuming RHN Channels for multiple versions of JBEAP contains message: "+expectedMsg);	
		Assert.assertEquals(sshCommandResult.getExitCode(), new Integer(1), "ExitCode from call to '"+rhnMigrateTool+" when consuming RHN Channels for multiple versions of JBEAP "+rhnChannelsToAdd);
		
		// assert that no product certs have been copied yet
		//Assert.assertEquals(clienttasks.getCurrentlyInstalledProducts().size(), 0, "No productCerts have been migrated when "+rhnMigrateTool+" was aborted.");	// getCurrentlyInstalledProducts() IS NOW TAINTED BY /etc/pki/product-default CERTS
		Assert.assertEquals(clienttasks.getProductCertFiles(clienttasks.productCertDir).size(), 0, "No productCerts have been migrated when "+rhnMigrateTool+" was aborted.");

		// assert that we are not yet registered to RHSM
		Assert.assertNull(clienttasks.getCurrentConsumerCert(),"We should NOT be registered to RHSM when "+rhnMigrateTool+" was aborted.");
		
		// assert that we are still registered to RHN
		Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, rhnSystemId),"Confirmed that rhn systemId '"+rhnSystemId+"' is still registered when '"+rhnMigrateTool+" was aborted.");
		Assert.assertTrue(clienttasks.isRhnSystemRegistered(),"This system is registered using RHN Classic.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-26763", "RHEL7-98220"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Execute migration tool rhn-migrate-classic-to-rhsm with invalid credentials",
			groups={"Tier3Tests","blockedByBug-789008","blockedByBug-807477","blockedByBug-1052297"},
			dependsOnMethods={},
			enabled=true)
	@ImplementsNitrateTest(caseId=136404)
	public void testRhnMigrateClassicToRhsmWithInvalidCredentials() {
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.14.3-1")) throw new SkipException("This test was implemented for subscription-manager-migration < 1.14.3-1.  See replacement RhnMigrateClassicToRhsmWithInvalidRhsmCredentials_Test.");

		clienttasks.unregister(null,null,null, null);
		String rhsmUsername=null, rhsmPassword=null, rhsmOrg=null;
		if (!sm_serverType.equals(CandlepinType.hosted)) {rhsmUsername="foo"; rhsmPassword="bar";}
		SSHCommandResult sshCommandResult = executeRhnMigrateClassicToRhsm(null,"foo","bar",rhsmUsername,rhsmPassword,rhsmOrg,null, null);
		Assert.assertEquals(sshCommandResult.getExitCode(), new Integer(1), "The expected exit code from call to '"+rhnMigrateTool+"' with invalid credentials.");
		//Assert.assertContainsMatch(sshCommandResult.getStdout(), "Unable to connect to certificate server.  See "+clienttasks.rhsmLogFile+" for more details.", "The expected stdout result from call to "+rhnMigrateTool+" with invalid credentials.");		// valid prior to bug fix 789008
		String expectedStdout = "Unable to connect to certificate server: "+servertasks.invalidCredentialsMsg()+".  See "+clienttasks.rhsmLogFile+" for more details.";
		Assert.assertTrue(sshCommandResult.getStdout().trim().endsWith(expectedStdout), "The expected stdout result from call to '"+rhnMigrateTool+"' with invalid credentials ended with: "+expectedStdout);
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21890", "RHEL7-51746"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Execute migration tool rhn-migrate-classic-to-rhsm with invalid RHSM credentials, but valid RHN credentials",
			groups={"Tier3Tests","blockedByBug-789008","blockedByBug-807477","blockedByBug-1052297"},
			dependsOnMethods={},
			enabled=true)
	@ImplementsNitrateTest(caseId=136404)
	public void testRhnMigrateClassicToRhsmWithInvalidRhsmCredentials() {
		if (clienttasks.isPackageVersion("subscription-manager-migration", "<", "1.14.3-1")) throw new SkipException("This test is implemented for subscription-manager-migration >= 1.14.3-1.  See former RhnMigrateClassicToRhsmWithInvalidCredentials_Test.");
	
		clienttasks.unregister(null,null,null, null);
		// register to RHN Classic
		String rhnSystemId = clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, rhnSystemId),"Confirmed that rhn systemId '"+rhnSystemId+"' is currently registered.");
		String rhsmUsername=null, rhsmPassword=null, rhsmOrg=null;
		if (sm_serverType.equals(CandlepinType.hosted)) throw new SkipException("This test is not functionally possible when migrating from RHN Classic to an RHSM Hosted entitlement server because the credentials are assumed to be equivalent; the customer account is the same.");
		if (!sm_serverType.equals(CandlepinType.hosted)) {rhsmUsername="foo"; rhsmPassword="bar";}
		SSHCommandResult sshCommandResult = executeRhnMigrateClassicToRhsm(null,sm_rhnUsername, sm_rhnPassword,rhsmUsername,rhsmPassword,rhsmOrg,null, null);
		Assert.assertEquals(sshCommandResult.getExitCode(), new Integer(70)/*EX_SOFTWARE*/, "The expected exit code from call to '"+rhnMigrateTool+"' with invalid RHSM credentials.");
		//Assert.assertContainsMatch(sshCommandResult.getStdout(), "Unable to connect to certificate server.  See "+clienttasks.rhsmLogFile+" for more details.", "The expected stdout result from call to "+rhnMigrateTool+" with invalid credentials.");		// valid prior to bug fix 789008
		String expectedStdout = "Unable to connect to certificate server: "+servertasks.invalidCredentialsMsg()+".  See "+clienttasks.rhsmLogFile+" for more details.";
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-1")) {	// post commit 630e1a2eb06e6bfacac669ce11f38e228c907ea9 1507030: RestlibExceptions should show they originate server-side
			expectedStdout = "Unable to connect to certificate server: "+"HTTP error (401 - Unauthorized): "+servertasks.invalidCredentialsMsg()+".  See "+clienttasks.rhsmLogFile+" for more details.";
		}
		Assert.assertTrue(sshCommandResult.getStdout().trim().endsWith(expectedStdout), "The expected stdout result from call to '"+rhnMigrateTool+"' with invalid RHSM credentials ended with: "+expectedStdout);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21889", "RHEL7-51745"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Execute migration tool rhn-migrate-classic-to-rhsm with invalid RHN credentials, but valid RHSM credentials",
			groups={"Tier3Tests","blockedByBug-1052297"},
			dependsOnMethods={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRhnMigrateClassicToRhsmWithInvalidRhnCredentials() {
		if (sm_serverType.equals(CandlepinType.hosted)) throw new SkipException("This test requires that your candlepin server NOT be a hosted RHN Classic system.");

		clienttasks.unregister(null,null,null, null);
		// register to RHN Classic
		String rhnSystemId = clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, rhnSystemId),"Confirmed that rhn systemId '"+rhnSystemId+"' is currently registered.");
		
		SSHCommandResult sshCommandResult = executeRhnMigrateClassicToRhsm(null,"foo","bar",sm_clientUsername,sm_clientPassword,sm_clientOrg,null, null);
		String expectedStdout = "Unable to authenticate to RHN Classic.  See /var/log/rhsm/rhsm.log for more details.";
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1-1")) expectedStdout = "Unable to authenticate to legacy server.  See "+clienttasks.rhsmLogFile+" for more details.";	// changed by commit 20906b8d0a89071529ea41a91356daccb7a4bbf9
		Assert.assertTrue(sshCommandResult.getStdout().trim().endsWith(expectedStdout), "The expected stdout result from call to '"+rhnMigrateTool+"' with invalid rhn credentials and valid subscription-manager credentials ended with: "+expectedStdout);
		//Assert.assertEquals(sshCommandResult.getExitCode(), new Integer(1), "The expected exit code from call to '"+rhnMigrateTool+"' with invalid credentials.");	// I think the exitCode of 1 is getting masked by the rhn-migrate-classic-to-rhsm.tcl script, adding other assertions below...
		String getTracebackCommand = "LINE_NUMBER=$(grep --line-number 'Making request:' "+clienttasks.rhsmLogFile+" | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n \"$LINE_NUMBER\" ]; then tail -n +$LINE_NUMBER "+clienttasks.rhsmLogFile+"; fi;";
		SSHCommandResult getTracebackCommandResult = client.runCommandAndWaitWithoutLogging(getTracebackCommand);
		//	201411251612:59.244 - WARNING: Last request from /var/log/rhsm/rhsm.log:
		//	2014-11-25 16:12:10,083 [DEBUG] rhn-migrate-classic-to-rhsm @connection.py:466 - Making request: GET /candlepin/users/testuser1/owners
		//	2014-11-25 16:12:10,107 [DEBUG] rhn-migrate-classic-to-rhsm @connection.py:489 - Response: status=200
		//	2014-11-25 16:12:12,326 [ERROR] rhn-migrate-classic-to-rhsm @migrate.py:332 - <Fault 2950: 'redstone.xmlrpc.XmlRpcFault: Either the password or username is incorrect.'>
		//	Traceback (most recent call last):
		//	  File "/usr/share/rhsm/subscription_manager/migrate/migrate.py", line 329, in connect_to_rhn
		//	    session_key = rpc_session.auth.login(credentials.username, credentials.password)
		//	  File "/usr/lib/python2.7/site-packages/rhn/rpclib.py", line 652, in __call__
		//	    return self._send(self._name, args)
		//	  File "/usr/lib/python2.7/site-packages/rhn/rpclib.py", line 384, in _request
		//	    self._handler, request, verbose=self._verbose)
		//	  File "/usr/lib/python2.7/site-packages/rhn/transports.py", line 188, in request
		//	    return self._process_response(fd, connection)
		//	  File "/usr/lib/python2.7/site-packages/rhn/transports.py", line 216, in _process_response
		//	    return self.parse_response(fd)
		//	  File "/usr/lib/python2.7/site-packages/rhn/transports.py", line 240, in parse_response
		//	    return u.close()
		//	  File "/usr/lib64/python2.7/xmlrpclib.py", line 793, in close
		//	    raise Fault(**self._stack[0])
		//	  Fault: <Fault 2950: 'redstone.xmlrpc.XmlRpcFault: Either the password or username is incorrect.'>
		log.warning("Last request from "+clienttasks.rhsmLogFile+":\n"+getTracebackCommandResult.getStdout());
		String expectedFault = "Either the password or username is incorrect.";
		Assert.assertTrue(getTracebackCommandResult.getStdout().contains(expectedFault), "The '"+rhnMigrateTool+"' did not complete with expected fault '"+expectedFault+"' written to "+clienttasks.rhsmLogFile+".");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20110", "RHEL7-51114"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Execute migration tool rhn-migrate-classic-to-rhsm without having registered to classic (no /etc/sysconfig/rhn/systemid)",
			groups={"Tier1Tests","blockedByBug-807477","blockedByBug-1052297","blockedByBug-1111258","blockedByBug-1212515"},
			dependsOnMethods={},
			enabled=true)
	public void testRhnMigrateClassicToRhsmWithMissingSystemIdFile() {
	    removeProxyServerConfigurations();	// cleanup from prior tests
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7 && clienttasks.arch.equals("ppc64le")) throw new SkipException("Use of rhn-migrate-classic-to-rhsm is not necessary on RHEL '"+client1tasks.redhatReleaseX+"' arch '"+clienttasks.arch+"' since this product was not released on RHN Classic.");
		if (Integer.valueOf(clienttasks.redhatReleaseX)>=7 && clienttasks.arch.equals("aarch64")) throw new SkipException("Use of rhn-migrate-classic-to-rhsm is not necessary on RHEL '"+client1tasks.redhatReleaseX+"' arch '"+clienttasks.arch+"' since this product was not released on RHN Classic.");
		if (sm_rhnUsername.equals("")) {throw new SkipException("This test requires an RHN Username for authentication.");}
		if (sm_rhnPassword.equals("")) {throw new SkipException("This test requires an RHN Password for authentication.");}
		
		// when we are migrating away from RHN Classic to a non-hosted candlepin server, determine good credentials for rhsm registration
		String rhsmUsername=sm_clientUsername, rhsmPassword=sm_clientPassword, rhsmOrg=sm_clientOrg;	// default
		if (clienttasks.register_(sm_rhnUsername, sm_rhnPassword, null, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null).getExitCode().equals(new Integer(0))) { // try sm_rhnUsername sm_rhnPassword...
			rhsmUsername=sm_rhnUsername; rhsmPassword=sm_rhnPassword; rhsmOrg = null;
		}
	    clienttasks.unregister(null,null,null, null);
	    clienttasks.removeRhnSystemIdFile();
		Assert.assertTrue(!clienttasks.isRhnSystemRegistered(),"This system is NOT registered using RHN Classic.");
		
		// execute rhn-migrate-classic-to-rhsm
		String rhsmServerUrlOption = "--serverurl="+"https://"+originalServerHostname+":"+originalServerPort+originalServerPrefix;	// passing the --serverurl forces prompting for rhsm credentials
		SSHCommandResult sshCommandResult = executeRhnMigrateClassicToRhsm(rhsmServerUrlOption,sm_rhnUsername,sm_rhnPassword,rhsmUsername,rhsmPassword,rhsmOrg,null, null);
		String expectedStdout = "Unable to locate SystemId file. Is this system registered?";
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.8.2-1")) expectedStdout = "Problem encountered getting the list of subscribed channels.  Exiting.";	// changed to this value by subscription-manager commit 53c7f0745d1857cd5e1e080e06d577e67e76ecdd for the benefit of unit testing on Fedora
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) expectedStdout = "Problem encountered getting the list of subscribed channels.  See /var/log/rhsm/rhsm.log for more details.";	// changed by commit c0f8052ec2b5b7b5c736eb626e381aef0e5327e5
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.14.7-1")) expectedStdout = "Could not read legacy systemid at /etc/sysconfig/rhn/systemid";	// changed by commit 49db2c8b60d552837527974150bff77fcf5d51fa
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.16.5-1")) expectedStdout = "Could not read legacy system id at /etc/sysconfig/rhn/systemid";	// changed by subscription-manager commit e323f8a1e11022e29df0b46af50ced451f74b2fe 	// Bug 1233379 - Grammar issue, "systemid" is not a word
		Assert.assertTrue(sshCommandResult.getStdout().trim().endsWith(expectedStdout), "The expected stdout result from call to '"+rhnMigrateTool+"' without an RHN Classic systemid file ended with: "+expectedStdout);
		// Skip ExitCode assertion because it is masked by the rhn-migrate-classic-to-rhsm.tcl wrapper Assert.assertEquals(sshCommandResult.getExitCode(), new Integer(1), "The expected exit code from call to '"+rhnMigrateTool+"' without an RHN Classic systemid file.");
		
		// TODO: We could get the tail of the rhsm.log and assert the expected log messages
		// -from up2date_client import up2dateErrors => "Unable to locate SystemId file. Is this system registered?" when no systemId file is present
		//	2013-03-04 16:52:18,529 [ERROR]  @migrate.py:364 - Traceback (most recent call last):
		//		  File "/usr/share/rhsm/subscription_manager/migrate/migrate.py", line 362, in get_subscribed_channels_list
		//		    subscribedChannels = map(lambda x: x['label'], getChannels().channels())
		//		  File "/usr/share/rhn/up2date_client/rhnChannel.py", line 96, in getChannels
		//		    raise up2dateErrors.NoSystemIdError(_("Unable to Locate SystemId"))
		//		NoSystemIdError: Unable to Locate SystemId
		// -from up2date_client.rhnChannel import getChannels => "This system is not associated with any channel." when rhnBaseChannel==null
		// ????? - Traceback
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21894", "RHEL7-51750"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Attempt to execute migration tool rhn-migrate-classic-to-rhsm with --no-auto and --service-level",
			groups={"Tier3Tests","blockedByBug-850920","blockedByBug-1052297","blockedByBug-1149007"},
			dependsOnMethods={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRhnMigrateClassicToRhsmWithNoAutoAndServiceLevel() {
		clienttasks.unregister(null,null,null, null);
		
		SSHCommandResult sshCommandResult = executeRhnMigrateClassicToRhsm("--no-auto --servicelevel=foo", sm_rhnUsername, sm_rhnPassword,null,null,null,null, null);
		String expectedStdout = "The --servicelevel and --no-auto options cannot be used together.";
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) expectedStdout = "The --service-level and --no-auto options cannot be used together.";
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.8-1")) expectedStdout = "The --servicelevel and --no-auto options cannot be used together.";	// post subscription-manager commit e53f0369b621902b75f2dbe047d97dc9ba3cc1c0
		Assert.assertTrue(sshCommandResult.getStdout().trim().endsWith(expectedStdout), "Stdout from call to '"+rhnMigrateTool+"' specifying both --no-auto and --servicelevel ended with: "+expectedStdout);
		Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "Stderr from call to '"+rhnMigrateTool+"' specifying both --no-auto and --servicelevel.");
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "Exit code from call to '"+rhnMigrateTool+"' specifying both --no-auto and --servicelevel.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21886", "RHEL7-51741"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Execute migration tool rhn-migrate-classic-to-rhsm while already registered to RHSM",
			groups={"Tier3Tests","blockedByBug-807477","blockedByBug-1052297","blockedByBug-1212515"},
			dependsOnMethods={},
			enabled=true)
	public void testRhnMigrateClassicToRhsmWhileAlreadyRegisteredToRhsm() {
		// register to RHN Classic
		String rhnSystemId = clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, rhnSystemId),"Confirmed that rhn systemId '"+rhnSystemId+"' is currently registered.");
		if (clienttasks.isPackageVersion("subscription-manager-migration", "<", "1.14.7-1")) clienttasks.removeRhnSystemIdFile();
		
		// register to RHSM
		String consumerid = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (List<String>)null, null, null, null, true, null, null, null, null, null));
		
		// attempt to migrate
		SSHCommandResult sshCommandResult;
		String options = null;
		String rhnUsername = sm_clientUsername;
		String rhnPassword = sm_clientPassword;
		String rhsmUsername = sm_clientUsername;
		String rhsmPassword = sm_clientPassword;
		String rhsmOrg = sm_clientOrg;
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.14.6-1")) {
			options = "--keep";	// commit 6eded942a7d184ef7ed92bbd94225120ee2f2f20
			rhnUsername = null;
			rhnPassword = null;
		}
		if (isCurrentlyConfiguredServerTypeHosted()) {
			rhsmUsername = null;
			rhsmPassword = null;
			rhsmOrg = null;
		}
		// note that the validity of the username and password really do not matter for this test
		sshCommandResult = executeRhnMigrateClassicToRhsm(options,rhnUsername,rhnPassword,rhsmUsername,rhsmPassword,rhsmOrg,null, null);
		String expectedStdout;
		expectedStdout = "This machine appears to be already registered to Certificate-based RHN.  Exiting."+"\n\n"+"Please visit https://access.redhat.com/management/consumers/"+consumerid+" to view the profile details.";	// changed by bug 847380
		expectedStdout = "This machine appears to be already registered to Red Hat Subscription Management.  Exiting."+"\n\n"+"Please visit https://access.redhat.com/management/consumers/"+consumerid+" to view the profile details.";	// changed by bug 874760
		expectedStdout = "This system appears to be already registered to Red Hat Subscription Management.  Exiting."+"\n\n"+"Please visit https://access.redhat.com/management/consumers/"+consumerid+" to view the profile details.";
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) expectedStdout = "This system appears to be already registered to Red Hat Subscription Management."+"\n\n"+"Please visit https://access.redhat.com/management/consumers/"+consumerid+" to view the profile details.";	// changed by commit c0f8052ec2b5b7b5c736eb626e381aef0e5327e5
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.2")) expectedStdout = "This system appears to already be registered to Red Hat Subscription Management."+"\n"+"Please visit https://access.redhat.com/management/consumers/"+consumerid+" to view the profile details.";	// changed by commit 58ee19c8e5e9f849bf6bed1ba16a6a21fe0d49dc
		Assert.assertTrue(sshCommandResult.getStdout().trim().endsWith(expectedStdout), "The expected stdout result from call to '"+rhnMigrateTool+"' while already registered to RHSM ended with: "+expectedStdout);
		Assert.assertEquals(sshCommandResult.getStderr().trim(),"", "The expected stderr result from call to '"+rhnMigrateTool+"' while already registered to RHSM.");
		// Stdout and Stderr asserts are sufficient Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The expected exit code from call to '"+rhnMigrateTool+"' while already registered to RHSM.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-26762", "RHEL7-55930"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="attempt to execute migration tool rhn-migrate-classic-to-rhsm while while under attack by a man-in-the-middle security vulnerability",
			groups={"Tier3Tests","blockedByBug-966745","blockedByBug-885130","blockedByBug-918967","blockedByBug-918968","blockedByBug-918969","blockedByBug-918970","blockedByBug-1052297","RhnMigrateClassicToRhsmCertificateVerification_Test"},
			dependsOnMethods={},
			enabled=true)
	public void testRhnMigrateClassicToRhsmCertificateVerification() {
		if (sm_rhnHostname.equals("")) throw new SkipException("This test requires access to RHN Classic or Satellite 5.");
		clienttasks.unregister(null, null, null, null);
		// Steps: are outlined in https://bugzilla.redhat.com/show_bug.cgi?id=918967#c1 EMBARGOED CVE-2012-6137 subscription-manager (rhn-migrate-classic-to-rhsm): Absent certificate verification
		
		// Step 0: register to rhn classic
		String rhnSystemId = clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		Assert.assertEquals(clienttasks.identity(null, null, null, null, null, null, null, null).getStdout().trim(),"server type: RHN Classic","Subscription Manager recognizes that we are registered classically.");

		// Step 1: determine ip addresses of our desired address (rhn) and the man-in-the-middle (bugzilla)
		rhnHostnameIPAddress = client.runCommandAndWait("dig +short xmlrpc."+sm_rhnHostname).getStdout().trim();
		bugzillaHostnameIPAddress =  client.runCommandAndWait("dig +short bugzillavip.proxy.prod.ext.phx2.redhat.com").getStdout().trim();	// could really be any known web app; this is the man-in-the-middle attacker
		Assert.assertMatch(rhnHostnameIPAddress, "\\d+\\.\\d+\\.\\d+\\.\\d+", "Validated rhn hostname '"+sm_rhnHostname+"' IPAddress to be '"+rhnHostnameIPAddress+"'.");
		Assert.assertMatch(bugzillaHostnameIPAddress, "\\d+\\.\\d+\\.\\d+\\.\\d+", "Validated bugzilla hostname '"+"bugzillavip.proxy.prod.ext.phx2.redhat.com"+"' IPAddress to be '"+bugzillaHostnameIPAddress+"'.");
		
		// Step 2: add a row to iptables setting bugzilla as the man-in-the-middle attacker of rhn
		client.runCommandAndWait("iptables -t nat -I OUTPUT -d "+rhnHostnameIPAddress+" -j DNAT --to-destination "+bugzillaHostnameIPAddress);
		
		// Step 3: verify the iptable row has been added
		//Assert.assertTrue(client.runCommandAndWait("iptables -t nat -L -v -n").getStdout().contains(rhnHostnameIPAddress+"        to:"+bugzillaHostnameIPAddress),"iptables is configured with a man-in-the-middle security attacker on rhn.");
		Assert.assertContainsMatch(client.runCommandAndWait("iptables -t nat -L -v -n").getStdout().trim(), (rhnHostnameIPAddress+"\\s+to:"+bugzillaHostnameIPAddress).replaceAll("\\.", "\\\\."),"iptables is configured with a man-in-the-middle security attacker on rhn.");
		
		// Step 4: verify that traffic to rhn is being redirected to bugzilla
		Assert.assertEquals(client.runCommandAndWait("curl --stderr /dev/null -k https://xmlrpc."+sm_rhnHostname+" | grep \"<title>\"").getStdout().trim(), "<title>Red Hat Bugzilla Main Page</title>", "curl calls to rhn IPAddress '"+rhnHostnameIPAddress+"' are being re-directed to bugzilla IPAddress '"+bugzillaHostnameIPAddress+"'.");
		
		// Step 5: not neccessary
		
		// Step 6: attempt to run rhn-migrate-classic-to-rhsm should fail with a 'certificate verify failed' error
		SSHCommandResult sshCommandResult;
		sshCommandResult = executeRhnMigrateClassicToRhsm("--serverurl="+originalServerHostname+":"+originalServerPort+originalServerPrefix, sm_rhnUsername, sm_rhnPassword, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null);
		String expectedStdout = "Unable to authenticate to RHN Classic.  See /var/log/rhsm/rhsm.log for more details.";
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1-1")) expectedStdout = "Unable to authenticate to legacy server.  See /var/log/rhsm/rhsm.log for more details.";	// changed by commit 20906b8d0a89071529ea41a91356daccb7a4bbf9
		Assert.assertTrue(sshCommandResult.getStdout().trim().endsWith(expectedStdout), "The expected stdout result from a call to '"+rhnMigrateTool+"' with a man-in-the-middle attacker should be: "+expectedStdout);
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(1), "The expected exitcode from a call to '"+rhnMigrateTool+"' with a man-in-the-middle attacker.");
		sshCommandResult = client.runCommandAndWait("tail "+clienttasks.rhsmLogFile);
		//	[root@rhsm-compat-rhel59 ~]# tail /var/log/rhsm/rhsm.log
		//	  File "/usr/lib64/python2.4/httplib.py", line 804, in endheaders
		//	    self._send_output()
		//	  File "/usr/lib64/python2.4/httplib.py", line 685, in _send_output
		//	    self.send(msg)
		//	  File "/usr/lib64/python2.4/httplib.py", line 664, in send
		//	    self.sock.sendall(str)
		//	  File "/usr/lib/python2.4/site-packages/rhn/SSL.py", line 217, in write
		//	    sent = self._connection.send(data)
		//	Error: [('SSL routines', 'SSL3_GET_SERVER_CERTIFICATE', 'certificate verify failed')]
		String expectedError = "Error: [('SSL routines', 'SSL3_GET_SERVER_CERTIFICATE', 'certificate verify failed')]";
		Assert.assertTrue(sshCommandResult.getStdout().trim().endsWith(expectedError),"Expected reason for '"+rhnMigrateTool+"' failure ends with: "+expectedError);
		
		// Step 7: remove the man-in-the-middle attacker
		deleteManInTheMiddleAttackerFromIptables();
		
		// Step 8: attempt to run rhn-migrate-classic-to-rhsm should now pass
		sshCommandResult = executeRhnMigrateClassicToRhsm("--serverurl="+originalServerHostname+":"+originalServerPort+originalServerPrefix, sm_rhnUsername, sm_rhnPassword, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null);
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "ExitCode from call to '"+rhnMigrateTool+"' after the man-in-the-middle attacker is removed.");
		Assert.assertTrue(!clienttasks.isRhnSystemIdRegistered(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, rhnSystemId), "Confirmed that rhn systemId '"+rhnSystemId+"' is no longer registered on the RHN Classic server.");
		Assert.assertNotNull(clienttasks.getCurrentConsumerCert(),"Confirmed that system is newly registered with Subscription Manager after migrating from RHN Classic using '"+rhnMigrateTool+"'.");
	}
	@AfterGroups(groups="setup", value="RhnMigrateClassicToRhsmCertificateVerification_Test")
	public void deleteManInTheMiddleAttackerFromIptables() {
		// Step 7: determine iptables entry of the man-in-the-middle (bugzilla) and delete it
		if (!bugzillaHostnameIPAddress.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {log.warning("The man-in-the-middle '"+bugzillaHostnameIPAddress+"' is not a valid ip address."); return;}
		String iptablesRow = client.runCommandAndWait("iptables -t nat -L --line-numbers | grep to:"+bugzillaHostnameIPAddress).getStdout().trim().split("\\s")[0];
		if (!isInteger(iptablesRow)) {log.warning("could not find an iptables row for man-in-the-middle "+bugzillaHostnameIPAddress); return;}
		RemoteFileTasks.runCommandAndAssert(client,"iptables -t nat -D OUTPUT "+iptablesRow, null,null,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"iptables -t nat -L --line-numbers | grep \"to:"+bugzillaHostnameIPAddress+"\"", null,null,Integer.valueOf(1));
	}
	protected String rhnHostnameIPAddress="";
	protected String bugzillaHostnameIPAddress="";	// could really be any known web app



	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21896", "RHEL7-51752"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="attempt to execute migration tool rhn-migrate-classic-to-rhsm when subscription-manager-migration-data is not installed",
			groups={"Tier3Tests","blockedByBug-967863","blockedByBug-1052297","RhnMigrateClassicToRhsmWithoutDataInstalled_Test"},
			dependsOnMethods={},
			enabled=true)
	public void testRhnMigrateClassicToRhsmWithoutDataInstalled() {
		if (sm_rhnHostname.equals("")) throw new SkipException("This test requires access to RHN Classic or Satellite 5.");
		clienttasks.unregister(null, null, null, null);
		
		// move the mapping file (to make it appear that subscription-manager-migration-data is not installed)
		log.info("Instead of destructively uninstalling subscription-manager-migration-data, we will simply move the mapping file...");
		client.runCommandAndWait("mv -f "+channelCertMappingFilename+" "+backupChannelCertMappingFilename);
		RemoteFileTasks.runCommandAndAssert(client, "rpm --verify --query subscription-manager-migration-data",1);
		
		// register to rhn classic
		String rhnSystemId = clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		Assert.assertEquals(clienttasks.identity(null, null, null, null, null, null, null, null).getStdout().trim(),"server type: RHN Classic","Subscription Manager recognizes that we are registered classically.");
		
		// attempt to run rhn-migrate-classic-to-rhsm should fail
		SSHCommandResult sshCommandResult;
		sshCommandResult = executeRhnMigrateClassicToRhsm("--serverurl="+originalServerHostname+":"+originalServerPort+originalServerPrefix, sm_rhnUsername, sm_rhnPassword, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null);
		String expectedStdout = "Unable to read mapping file: "+channelCertMappingFilename+"."+"\n"+"Do you have the subscription-manager-migration-data package installed?";
		Integer expectedExitCode = Integer.valueOf(1);
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) expectedStdout = "Unable to read mapping file: "+channelCertMappingFilename+"."+"\n"+"Please check that you have the subscription-manager-migration-data package installed.";	// changed by commit c0f8052ec2b5b7b5c736eb626e381aef0e5327e5
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.14.7")) expectedExitCode = Integer.valueOf(78)/*EX_CONFIG*/;	// changed by commit 270f2a3e5f7d55b69a6f98c160d38362961b3059	 // Specified error codes on system_exit in rhn-migrate-classic-to-rhsm
		Assert.assertTrue(sshCommandResult.getStdout().trim().endsWith(expectedStdout), "The expected stdout result from a call to '"+rhnMigrateTool+"' without subscription-manager-migration-data installed should be: "+expectedStdout);
		Assert.assertEquals(sshCommandResult.getExitCode(), expectedExitCode, "The expected exitcode from a call to '"+rhnMigrateTool+"' without subscription-manager-migration-data installed.");
		
		// verify that we have not yet migrated since subscription-manager-migration-data was not installed
		Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, rhnSystemId), "Confirmed that rhn systemId '"+rhnSystemId+"' is still registered on the RHN Classic server after failing to migrate from RHN Classic using '"+rhnMigrateTool+"'.");
		Assert.assertNull(clienttasks.getCurrentConsumerCert(),"Confirmed that system is NOT registered with Subscription Manager after failing to migrate from RHN Classic using '"+rhnMigrateTool+"'.");
	}
	@AfterGroups(groups="setup", value="RhnMigrateClassicToRhsmWithoutDataInstalled_Test")
	public void restoreChannelCertMappingFileAfterRhnMigrateClassicToRhsmWithoutDataInstalled_Test() {
		
		// backup the channelCertMappingFile
		log.info("Restoring channelCertMappingFile...");
		client.runCommandAndWait("mv -f "+backupChannelCertMappingFilename+" "+channelCertMappingFilename);
		RemoteFileTasks.runCommandAndAssert(client, "rpm --verify --query subscription-manager-migration-data",0);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21892", "RHEL7-51748"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Attempt to execute migration tool rhn-migrate-classic-to-rhsm with --keep classic which implies that we do not want to deregister from classic which will result in dual interoperability registration.",
			groups={"Tier3Tests","blockedByBug-1180273"},
			dependsOnMethods={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRhnMigrateClassicToRhsmWithKeepClassic() {
		if (clienttasks.isPackageVersion("subscription-manager-migration", "<", "1.14.3")) throw new SkipException("This version of subscription-manager does not support 1180273 - [RFE] rhn-migrate-classic-to-rhsm should allow the user to migrate a system without requiring credentials on RHN Classic");	// commit 5df7aaaa69a22b9e3f771971f1aa4e58657c8377
		
		String options = "--registration-state=keep";
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.14.6")) options = "--keep";	// commit 6eded942a7d184ef7ed92bbd94225120ee2f2f20
		
		if (sm_rhnHostname.equals("")) throw new SkipException("This test requires access to RHN Classic or Satellite 5.");
		clienttasks.unregister(null, null, null, null);
		
		// register to rhn classic
		String rhnSystemId = clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		Assert.assertEquals(clienttasks.identity(null, null, null, null, null, null, null, null).getStdout().trim(),"server type: RHN Classic","Subscription Manager recognizes that we are registered classically.");

		// subscribe to more RHN Classic channels (just to add a little unnecessary fun)
		List<String> filteredRhnAvailableChildChannels = new ArrayList<String>(rhnAvailableChildChannels);
		// Avoid adding beta child channels since they map to a different product cert and can cause the following...
		//	+-----------------------------------------------------+
		//	Unable to continue migration!
		//	+-----------------------------------------------------+
		//	You are subscribed to channels that have conflicting product certificates.
		//	The following channels map to product ID 69:
		//	rhel-x86_64-server-7
		//	rhel-x86_64-server-optional-7-beta
		//	Reduce the number of channels per product ID to 1 and run migration again.
		//	To remove a channel, use 'rhn-channel --remove --channel=<conflicting_channel>'.
///*debugTesting*/		addRhnClassicChannels(sm_rhnUsername, sm_rhnPassword, Arrays.asList("rhel-x86_64-server-optional-7-beta"));	// only add 1 child channel to avoid "You are subscribed to channels that have conflicting product certificates."
		for (String rhnAvailableChildChannel : rhnAvailableChildChannels) {
			if (rhnAvailableChildChannel.contains("-beta")) {
				filteredRhnAvailableChildChannels.remove(rhnAvailableChildChannel);
			}
		}
		// Avoid adding unmapped child channels since they can cause the following...
		//	+-----------------------------------------------------+
		//	No product certificates are mapped to these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-server-v2vwin-7-beta-debuginfo
		//	
		//	Use --force to ignore these channels and continue the migration.
///*debugTesting*/		addRhnClassicChannels(sm_rhnUsername, sm_rhnPassword, Arrays.asList("rhel-x86_64-server-v2vwin-7-beta-debuginfo"));
		for (String rhnAvailableChildChannel : rhnAvailableChildChannels) {
			if (!channelsToProductCertFilenamesMap.containsKey(rhnAvailableChildChannel)) {
				filteredRhnAvailableChildChannels.remove(rhnAvailableChildChannel);
			}
		}
		addRhnClassicChannels(sm_rhnUsername, sm_rhnPassword, getRandomSubsetOfList(filteredRhnAvailableChildChannels, /*5*/1));	// only add 1 child channel to reduce the chance of "You are subscribed to channels that have conflicting product certificates."
		
		
		
		// attempt to run rhn-migrate-classic-to-rhsm
		SSHCommandResult sshCommandResult = executeRhnMigrateClassicToRhsm(options+" "+"--destination-url="+originalServerHostname+":"+originalServerPort+originalServerPrefix, null, null, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null);
		//	[root@jsefler-os6 ~]# rhn-migrate-classic-to-rhsm --registration-state=keep --destination-url=subscription.rhn.stage.redhat.com:443/subscription
		//	Destination username: stage_auto_testuser
		//	Destination password: 
		//
		//	Retrieving existing legacy subscription information...
		//
		//	+-----------------------------------------------------+
		//	System is currently subscribed to these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-server-6
		//
		//	+-----------------------------------------------------+
		//	Installing product certificates for these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-server-6
		//
		//	Product certificates installed successfully to /etc/pki/product.
		//
		//	Attempting to register system to destination server...
		//	WARNING
		//
		//	This system has already been registered with Red Hat using RHN Classic.
		//
		//	Your system is being registered again using Red Hat Subscription Management. Red Hat recommends that customers only register once.
		//
		//	To learn how to unregister from either service please consult this Knowledge Base Article: https://access.redhat.com/kb/docs/DOC-45563
		//	The system has been registered with ID: 19ff59fe-c824-4acb-bd8b-ef843dd7d149 
		//
		//	Installed Product Current Status:
		//	Product Name: Red Hat Enterprise Linux Server
		//	Status:       Subscribed
		//
		//	System 'jsefler-os6' successfully registered.
		//
		//	[root@jsefler-os6 ~]# echo $?
		//	0
		
		// verify the migration was successful
		// WARNING: asserting this exit code is misleading because it is the exit code from the rhn-migrate-classic-to-rhsm.tcl script which is a wrapper to rhn-migrate-classic-to-rhsm which could exit with a different code
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "ExitCode from call to '"+rhnMigrateTool+"' after attempting to migrate with options '"+options+"'.");
		
		// verify that we are newly registered to RHSM
		Assert.assertNotNull(clienttasks.getCurrentConsumerCert(),"Confirmed that the system is newly registered with Subscription Manager after migrating from RHN Classic using '"+rhnMigrateTool+"'.");
		
		// verify that we are still classically registered to RHN
		Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, rhnSystemId), "Confirmed that rhn systemId '"+rhnSystemId+"' is STILL registered on the RHN Classic server (because we passed an option to keep the classic registration).");
		
		// verify that we were warned with the interoperability message
		Assert.assertTrue(sshCommandResult.getStdout().trim().contains(clienttasks.msg_InteroperabilityWarning), "The expected stdout result from a call to '"+rhnMigrateTool+"' with the option to keep the classic registration should warn the user with the interoperability message: "+clienttasks.msg_InteroperabilityWarning);
		
		
		// Now attempt the same migration while supplying the destination credentials on the command line instead of being prompted...
		clienttasks.unregister(null, null, null, null);
		sshCommandResult = executeRhnMigrateClassicToRhsm(options+" "+"--destination-url="+originalServerHostname+":"+originalServerPort+originalServerPrefix+" "+"--destination-user="+sm_clientUsername+" "+"--destination-password="+sm_clientPassword+" "+(sm_clientOrg!=null?"--org="+sm_clientOrg:""), null, null, null, null, null, null, null);
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "ExitCode from call to '"+rhnMigrateTool+"' after attempting to use the option to keep the classic registration");
		Assert.assertNotNull(clienttasks.getCurrentConsumerCert(),"Confirmed that the system is newly registered with Subscription Manager after migrating from RHN Classic using '"+rhnMigrateTool+"'.");
		Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, rhnSystemId), "Confirmed that rhn systemId '"+rhnSystemId+"' is STILL registered on the RHN Classic server (because we passed an option to keep the classic registration).");
		Assert.assertTrue(sshCommandResult.getStdout().trim().contains(clienttasks.msg_InteroperabilityWarning), "The expected stdout result from a call to '"+rhnMigrateTool+"' with the option to keep the classic registration should warn the user with the interoperability message: "+clienttasks.msg_InteroperabilityWarning);
	}

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-21891", "RHEL7-51747"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Attempt to execute migration tool rhn-migrate-classic-to-rhsm with --keep-classic AND unnecessary classic credentials --legacy-user/--legacy-password (unnecessary because --keep-classic implies that we do NOT want to deregister from classic)",
			groups={"Tier3Tests","blockedByBug-1180273"},
			dependsOnMethods={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRhnMigrateClassicToRhsmWithKeepClassicAndLegacyCredentials() {
		if (clienttasks.isPackageVersion("subscription-manager-migration", "<", "1.14.3")) throw new SkipException("This version of subscription-manager does not support 1180273 - [RFE] rhn-migrate-classic-to-rhsm should allow the user to migrate a system without requiring credentials on RHN Classic");	// commit 5df7aaaa69a22b9e3f771971f1aa4e58657c8377
		
		String options = "--registration-state=keep";
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.14.6")) options = "--keep";	// commit 6eded942a7d184ef7ed92bbd94225120ee2f2f20
		
		if (sm_rhnHostname.equals("")) throw new SkipException("This test requires access to RHN Classic or Satellite 5.");
		clienttasks.unregister(null, null, null, null);
		
		// register to rhn classic
		String rhnSystemId = clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		Assert.assertEquals(clienttasks.identity(null, null, null, null, null, null, null, null).getStdout().trim(),"server type: RHN Classic","Subscription Manager recognizes that we are registered classically.");

		// subscribe to more RHN Classic channels (just to add a little unnecessary fun)
		List<String> filteredRhnAvailableChildChannels = new ArrayList<String>(rhnAvailableChildChannels);
		// Avoid adding beta child channels since they map to a different product cert and can cause the following...
		//	+-----------------------------------------------------+
		//	Unable to continue migration!
		//	+-----------------------------------------------------+
		//	You are subscribed to channels that have conflicting product certificates.
		//	The following channels map to product ID 69:
		//	rhel-x86_64-server-7
		//	rhel-x86_64-server-optional-7-beta
		//	Reduce the number of channels per product ID to 1 and run migration again.
		//	To remove a channel, use 'rhn-channel --remove --channel=<conflicting_channel>'.
///*debugTesting*/		addRhnClassicChannels(sm_rhnUsername, sm_rhnPassword, Arrays.asList("rhel-x86_64-server-optional-7-beta"));	// only add 1 child channel to avoid "You are subscribed to channels that have conflicting product certificates."
		for (String rhnAvailableChildChannel : rhnAvailableChildChannels) {
			if (rhnAvailableChildChannel.contains("-beta")) {
				filteredRhnAvailableChildChannels.remove(rhnAvailableChildChannel);
			}
		}
		// Avoid adding unmapped child channels since they can cause the following...
		//	+-----------------------------------------------------+
		//	No product certificates are mapped to these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-server-v2vwin-7-beta-debuginfo
		//	
		//	Use --force to ignore these channels and continue the migration.
///*debugTesting*/		addRhnClassicChannels(sm_rhnUsername, sm_rhnPassword, Arrays.asList("rhel-x86_64-server-v2vwin-7-beta-debuginfo"));
		for (String rhnAvailableChildChannel : rhnAvailableChildChannels) {
			if (!channelsToProductCertFilenamesMap.containsKey(rhnAvailableChildChannel)) {
				filteredRhnAvailableChildChannels.remove(rhnAvailableChildChannel);
			}
		}
		addRhnClassicChannels(sm_rhnUsername, sm_rhnPassword, getRandomSubsetOfList(filteredRhnAvailableChildChannels, /*5*/1));	// only add 1 child channel to reduce the chance of "You are subscribed to channels that have conflicting product certificates."

		
		
		// attempt to run rhn-migrate-classic-to-rhsm
//TODO not sure how valid this test is, but it does throw an error.  Maybe I should explicitly call subscription-manager config on the server hostname port prefix
		SSHCommandResult sshCommandResult = executeRhnMigrateClassicToRhsm(options+" "+/*"--destination-url="+originalServerHostname+":"+originalServerPort+originalServerPrefix+" "+*/"--legacy-user="+sm_rhnUsername+" "+"--legacy-password="+sm_rhnPassword, null, null, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null);
		//	[root@jsefler-os6 ~]# rhn-migrate-classic-to-rhsm --registration-state=keep --legacy-user=qa@redhat.com --legacy-password=REDACTED
		//	Unable to connect to certificate server: Invalid credentials..  See /var/log/rhsm/rhsm.log for more details.
		//	[root@jsefler-os6 ~]# echo $?
		//	1
		
		// verify the migration was successful
		// WARNING: asserting this exit code is misleading because it is the exit code from the rhn-migrate-classic-to-rhsm.tcl script which is a wrapper to rhn-migrate-classic-to-rhsm which could exit with a different code
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "ExitCode from call to '"+rhnMigrateTool+"' after attempting to migrate with options '"+options+"'.");

		// verify that we are newly registered to RHSM
		Assert.assertNotNull(clienttasks.getCurrentConsumerCert(),"Confirmed that the system is newly registered with Subscription Manager after migrating from RHN Classic using '"+rhnMigrateTool+"'.");
		
		// verify that we are still classically registered to RHN
		Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, rhnSystemId), "Confirmed that rhn systemId '"+rhnSystemId+"' is STILL registered on the RHN Classic server (because we passed an option to keep the classic registration).");
		
		// verify that we were warned with the interoperability message
		Assert.assertTrue(sshCommandResult.getStdout().trim().contains(clienttasks.msg_InteroperabilityWarning), "The expected stdout result from a call to '"+rhnMigrateTool+"' with the option to keep the classic registration should warn the user with the interoperability message: "+clienttasks.msg_InteroperabilityWarning);
		
		
		// Now attempt the same migration while supplying the destination credentials on the command line instead of being prompted...
		clienttasks.unregister(null, null, null, null);
//TODO not sure how valid this test is.  Maybe I should explicitly call subscription-manager config on the server hostname port prefix
		sshCommandResult = executeRhnMigrateClassicToRhsm(options+" "+/*"--destination-url="+originalServerHostname+":"+originalServerPort+originalServerPrefix+" "+*/"--legacy-user="+sm_rhnUsername+" "+"--legacy-password="+sm_rhnPassword+" "+"--destination-user="+sm_clientUsername+" "+"--destination-password="+sm_clientPassword+" "+(sm_clientOrg!=null?"--org="+sm_clientOrg:""), /*sm_rhnUsername*/null, /*sm_rhnPassword*/null, null, null, null, null, null);
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "ExitCode from call to '"+rhnMigrateTool+"' after attempting to use the option to keep the classic registration");
		Assert.assertNotNull(clienttasks.getCurrentConsumerCert(),"Confirmed that the system is newly registered with Subscription Manager after migrating from RHN Classic using '"+rhnMigrateTool+"'.");
		Assert.assertTrue(clienttasks.isRhnSystemIdRegistered(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, rhnSystemId), "Confirmed that rhn systemId '"+rhnSystemId+"' is STILL registered on the RHN Classic server (because we passed an option to keep the classic registration).");
		Assert.assertTrue(sshCommandResult.getStdout().trim().contains(clienttasks.msg_InteroperabilityWarning), "The expected stdout result from a call to '"+rhnMigrateTool+"' with the option to keep the classic registration should warn the user with the interoperability message: "+clienttasks.msg_InteroperabilityWarning);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-38191", "RHEL7-98219"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Attempt to execute migration tool rhn-migrate-classic-to-rhsm with --remove-rhn-packages which should disable some classic services and remove several classic packages. As a result, subsequent attempts to migrate will be halted with a friendly message.",
			groups={"Tier3Tests","blockedByBug-1185914","blockedByBug-1432642","RhnMigrateClassicToRhsmWithRemoveRhnPackages_Test"},
			dependsOnMethods={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	// TODO WARNING: This test will inadvertently remove package subscription-manager-firstboot and not restore it.
	public void testRhnMigrateClassicToRhsmWithRemoveRhnPackages() {
		if (clienttasks.isPackageVersion("subscription-manager-migration", "<", "1.18.2-1")) throw new SkipException("This version of subscription-manager does not support Bug 1185914 - [RFE] rhn-migrate-classic-to-rhsm should give the option to remove RHN Classic related packages / daemons");	// commit 871264dbb0cc091d3eaefabfdfd2e51d6bbc0a3c
		
		String options = "--remove-rhn-packages";
		
		if (sm_rhnHostname.equals("")) throw new SkipException("This test requires access to RHN Classic or Satellite 5.");
		clienttasks.unregister(null, null, null, null);
		
		// register to rhn classic
		String rhnSystemId = clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		Assert.assertEquals(clienttasks.identity(null, null, null, null, null, null, null, null).getStdout().trim(),"server type: RHN Classic","Subscription Manager recognizes that we are registered classically.");
		
		// randomly remove a benign rhn classic package (just to add a little unnecessary fun)
		if (randomGenerator.nextBoolean() && clienttasks.isPackageInstalled("osad")) clienttasks.yumRemovePackage("osad");
		if (randomGenerator.nextBoolean() && clienttasks.isPackageInstalled("openscap")) clienttasks.yumRemovePackage("openscap");
		
		// subscribe to more RHN Classic channels (just to add a little unnecessary fun)
		List<String> filteredRhnAvailableChildChannels = new ArrayList<String>(rhnAvailableChildChannels);
		// Avoid adding beta child channels since they map to a different product cert and can cause the following...
		//	+-----------------------------------------------------+
		//	Unable to continue migration!
		//	+-----------------------------------------------------+
		//	You are subscribed to channels that have conflicting product certificates.
		//	The following channels map to product ID 69:
		//	rhel-x86_64-server-7
		//	rhel-x86_64-server-optional-7-beta
		//	Reduce the number of channels per product ID to 1 and run migration again.
		//	To remove a channel, use 'rhn-channel --remove --channel=<conflicting_channel>'.
///*debugTesting*/		addRhnClassicChannels(sm_rhnUsername, sm_rhnPassword, Arrays.asList("rhel-x86_64-server-optional-7-beta"));	// only add 1 child channel to avoid "You are subscribed to channels that have conflicting product certificates."
		for (String rhnAvailableChildChannel : rhnAvailableChildChannels) {
			if (rhnAvailableChildChannel.contains("-beta")) {
				filteredRhnAvailableChildChannels.remove(rhnAvailableChildChannel);
			}
		}
		// Avoid adding unmapped child channels since they can cause the following...
		//	+-----------------------------------------------------+
		//	No product certificates are mapped to these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-server-v2vwin-7-beta-debuginfo
		//	
		//	Use --force to ignore these channels and continue the migration.
///*debugTesting*/		addRhnClassicChannels(sm_rhnUsername, sm_rhnPassword, Arrays.asList("rhel-x86_64-server-v2vwin-7-beta-debuginfo"));
		for (String rhnAvailableChildChannel : rhnAvailableChildChannels) {
			if (!channelsToProductCertFilenamesMap.containsKey(rhnAvailableChildChannel)) {
				filteredRhnAvailableChildChannels.remove(rhnAvailableChildChannel);
			}
		}
		addRhnClassicChannels(sm_rhnUsername, sm_rhnPassword, getRandomSubsetOfList(filteredRhnAvailableChildChannels, /*5*/1));	// only add 1 child channel to reduce the chance of "You are subscribed to channels that have conflicting product certificates."
		
		// attempt to run rhn-migrate-classic-to-rhsm
		SSHCommandResult sshCommandResult = executeRhnMigrateClassicToRhsm(options+" "+"--destination-url="+originalServerHostname+":"+originalServerPort+originalServerPrefix, sm_rhnUsername, sm_rhnPassword, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null);
		//	[root@jsefler-rhel6 ~]# rhn-migrate-classic-to-rhsm --remove-rhn-packages --legacy-user=rhsm-client --legacy-password=REDACTED --destination-user=testuser1 --destination-password=REDACTED --org=admin
		//
		//	Retrieving existing legacy subscription information...
		//
		//	+-----------------------------------------------------+
		//	System is currently subscribed to these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-server-6
		//	rhn-tools-rhel-x86_64-server-6-debuginfo
		//
		//	+-----------------------------------------------------+
		//	Installing product certificates for these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-server-6
		//	rhn-tools-rhel-x86_64-server-6-debuginfo
		//
		//	Product certificates installed successfully to /etc/pki/product.
		//
		//	Preparing to unregister system from legacy server...
		//	System successfully unregistered from legacy server.
		//	Stopping and disabling legacy services...                  <===== NOTE: Bug 1390341 - rhn-migrate-classic-to-rhsm is failing to stop and disable services
		//	Shutting down osad:                                        [FAILED]
		//	Usage: /etc/init.d/osad {start|stop|restart|reload|status|condrestart}
		//	Removing legacy packages...
		//	warning: /etc/sysconfig/rhn/up2date saved as /etc/sysconfig/rhn/up2date.rpmsave
		//
		//	Attempting to register system to destination server...
		//	Registering to: jsefler-candlepin.usersys.redhat.com:8443/candlepin
		//	The system has been registered with ID: 376bf523-c75d-4603-b4ed-abea41e02f79 
		//
		//	Installed Product Current Status:
		//	Product Name: Red Hat Enterprise Linux Server
		//	Status:       Not Subscribed
		//
		//	Unable to find available subscriptions for all your installed products.
		//	System 'jsefler-rhel6.usersys.redhat.com' successfully registered.
		//
		//	[root@jsefler-rhel6 ~]# echo $?
		//	0
		
		// verify the migration was successful
		// WARNING: asserting this exit code is misleading because it is the exit code from the rhn-migrate-classic-to-rhsm.tcl script which is a wrapper to rhn-migrate-classic-to-rhsm which could exit with a different code
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "ExitCode from call to '"+rhnMigrateTool+"' after attempting to migrate with options '"+options+"'.");
		
		// verify that we are newly registered to RHSM
		Assert.assertNotNull(clienttasks.getCurrentConsumerCert(),"Confirmed that the system is newly registered with Subscription Manager after migrating from RHN Classic using '"+rhnMigrateTool+"'.");
		
		// verify that we are not classically registered to RHN
		Assert.assertTrue(!clienttasks.isRhnSystemIdRegistered(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, rhnSystemId), "Confirmed that rhn systemId '"+rhnSystemId+"' is NOT registered on the RHN Classic server (because we passed an option to remove the classic registration packages).");
		
		// verify that the following legacy rhn classic packages have been removed
		String removingPackagesMsg = "Removing legacy packages...";
		Assert.assertTrue(sshCommandResult.getStdout().contains(removingPackagesMsg), "Stdout from call to '"+rhnMigrateTool+" "+options+"' contains message: "+removingPackagesMsg);
		for (String legacyPackage : legacyRHNClassicPackages) {
			Assert.assertTrue(!clienttasks.isPackageInstalled(legacyPackage),"RHN Legacy package '"+legacyPackage+"' is not installed after running rhn-migrate-classic-to-rhsm with option '"+options+"'");
		}
		
		// attempt another migration and assert the following 
		//	[root@jsefler-rhel6 ~]# rhn-migrate-classic-to-rhsm
		//	Could not find up2date_client.config module! Perhaps this script was already executed with --remove-rhn-packages?
		String expectedMsg = "Could not find up2date_client.config module! Perhaps this script was already executed with --remove-rhn-packages?";
		sshCommandResult = executeRhnMigrateClassicToRhsm(options+" "+"--destination-url="+originalServerHostname+":"+originalServerPort+originalServerPrefix, sm_rhnUsername, sm_rhnPassword, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null);
		//Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(70), "ExitCode from call to '"+rhnMigrateTool+"' after attempting to use the option to keep the classic registration");
		//Assert.assertEquals(sshCommandResult.getStderr().trim(), expectedMsg, "The expected stderr result from a call to '"+rhnMigrateTool+"' after already having called this tool with options '"+options+"'.");
		//Assert.assertEquals(sshCommandResult.getStdout().trim(), "", "The expected stdout result from a call to '"+rhnMigrateTool+"' after already having called this tool with options '"+options+"'.");
		Assert.assertTrue((sshCommandResult.getStderr()+sshCommandResult.getStdout()).trim().endsWith(expectedMsg), "The result from a call to '"+rhnMigrateTool+"' after already having called this tool with options '"+options+"' ends with: "+expectedMsg);
	}
	// Make sure the RHN Classic Packages are installed
	List<String> legacyRHNClassicPackages = Arrays.asList(new String[]{"osad","rhn-check","rhn-client-tools","rhncfg","rhncfg-actions","rhncfg-client","rhncfg-management","rhn-setup","rhnpush","rhnsd","spacewalk-abrt","spacewalk-oscap","yum-rhn-plugin","rhn-setup-gnome"/*requires rhn-setup and rhn-client-tools and must be installed for subscription-manager-firstboot requires rhn-setup-gnome*/});	// taken from https://github.com/candlepin/subscription-manager/pull/1484/files
	@BeforeGroups(groups="setup",value={"RhnMigrateClassicToRhsmWithRemoveRhnPackages_Test"})
	@AfterGroups(groups="setup",value={"RhnMigrateClassicToRhsmWithRemoveRhnPackages_Test"})
	public void installRhnClassicPackages() throws IOException, JSONException {
		if (clienttasks==null) return;
		
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "1432642";	// Bug 1432642 - released/RHN-Tools-5.7-RHEL-7/<ARCH>/tree/RHNTools/ is missing repodata
		boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			throw new SkipException("Blocked from installing RHN Classic packages on RHEL7 while bug "+bugId+" is open.  There is no workaround.");
		}
		// END OF WORKAROUND
		
		// install the rhn classic packages
		clienttasks.installReleasedRhnClassicPackages(sm_yumInstallOptions, legacyRHNClassicPackages);
		
		//  also need to restore the up2date configurations
		updateSslCaCertConfigInRhnUp2dateFile();
	}
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47943", "RHEL7-109986"},
//			linkedWorkItems= {
//				@LinkedItem(
//					workitemId= "RHEL6-?????",	// TODO
//					project= Project.RHEL6,
//					role= DefTypes.Role.VERIFIES),
//				@LinkedItem(
//					workitemId= "RHEL7-95162",	// RHEL7-95162 - RHSM-REQ : rhn-migrate-classic-to-rhsm should automatically enable yum plugins
//					project= Project.RedHatEnterpriseLinux7,
//					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Verify that the yum plugins for /etc/yum/pluginconf.d/product-id.conf and /etc/yum/pluginconf.d/subscription-manager.conf are automatically enabled by an execution of rhn-migrate-classic-to-rhsm when rhsm.auto_enable_yum_plugins is configured on.",
			groups={"Tier3Tests","blockedByBug-1466453","testRhnMigrateClassicToRhsmShouldAutomaticallyEnableYumPluginsWhenAutoEnableIsOn"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRhnMigrateClassicToRhsmShouldAutomaticallyEnableYumPluginsWhenAutoEnableIsOn() {
		// this bug is specifically designed to test Bug 1466453 - [RFE] rhn-migrate-classic-to-rhsm should ensure subscription-manager yum plugin is enabled
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.20.2-1")) {  // commit 7b2b89c7ba4fbf464dd988e12675b2b763007f2c 	1466453 - [RFE] rhn-migrate-classic-to-rhsm should ensure subscription-manager yum plugin is enabled
			throw new SkipException("This test applies a newer version of subscription-manager that includes an implementation for Bug 1466453 - [RFE] rhn-migrate-classic-to-rhsm should ensure subscription-manager yum plugin is enabled");
		}
		
		if (sm_rhnHostname.equals("")) throw new SkipException("This test requires access to RHN Classic or Satellite 5.");
		clienttasks.unregister(null, null, null, null);
		
		resetDefaultConfigurationsForYumPluginsAndRhsmAutoEnableYumPlugins();
		
		// randomly disable one or both /etc/yum/pluginconf.d/product-id.conf and /etc/yum/pluginconf.d/subscription-manager.conf
		if (getRandomBoolean()) {
			clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled", "0");
			clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled", String.valueOf(getRandInt()%2)/* "0" or "1" */);
		} else {
			clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled", "false");
			clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled", "true");
			// TEMPORARY WORKAROUND
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1489917"; // Bug 1489917 - disabling yum plugins using "false" (rather than "0") causes traceback: invalid literal for int() with base 10: 'false'
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled", "0");
				clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled", "1");
			}
			// END OF WORKAROUND
		}
		
		String yumPluginConfFileForProductIdEnabledString = clienttasks.getConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled");
		String yumPluginConfFileForSubscriptionManagerEnabledString = clienttasks.getConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled");
		Boolean yumPluginConfFileForProductIdEnabled = Boolean.valueOf(yumPluginConfFileForProductIdEnabledString);
		Boolean yumPluginConfFileForSubscriptionManagerEnabled = Boolean.valueOf(yumPluginConfFileForSubscriptionManagerEnabledString);
		if (yumPluginConfFileForProductIdEnabledString.equals("1")) {yumPluginConfFileForProductIdEnabled = true;} else if (yumPluginConfFileForProductIdEnabledString.equals("0")) {yumPluginConfFileForProductIdEnabled = false;}
		if (yumPluginConfFileForSubscriptionManagerEnabledString.equals("1")) {yumPluginConfFileForSubscriptionManagerEnabled = true;} else if (yumPluginConfFileForSubscriptionManagerEnabledString.equals("0")) {yumPluginConfFileForSubscriptionManagerEnabled = false;}
		
		// register to rhn classic
		String rhnSystemId = clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		
		// attempt to run rhn-migrate-classic-to-rhsm
		String options = "";
		SSHCommandResult sshCommandResult = executeRhnMigrateClassicToRhsm(options, sm_rhnUsername, sm_rhnPassword, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null);
		String command = "rhn-migrate-classic-to-rhsm";
		
		//	[root@jsefler-rhel7 ~]# rhn-migrate-classic-to-rhsm --legacy-user=rhsm-client --destination-user=testuser1
		//	Legacy password: 
		//	Destination password: 
		//	Org: admin
		//
		//	Retrieving existing legacy subscription information...
		//
		//	+-----------------------------------------------------+
		//	System is currently subscribed to these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-server-7
		//
		//	+-----------------------------------------------------+
		//	Installing product certificates for these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-server-7
		//
		//	Product certificates installed successfully to /etc/pki/product.
		//
		//	Preparing to unregister system from legacy server...
		//	System successfully unregistered from legacy server.
		//	Stopping and disabling legacy services...
		//	rhnsd.service is not a native service, redirecting to /sbin/chkconfig.
		//	Executing /sbin/chkconfig rhnsd off
		//
		//	Attempting to register system to destination server...
		//	WARNING
		//
		//	The yum plugins: /etc/yum/pluginconf.d/subscription-manager.conf were automatically enabled for the benefit of Red Hat Subscription Management. If not desired, use "subscription-manager config --rhsm.auto_enable_yum_plugins=0" to block this behavior.
		//
		//	Registering to: jsefler-candlepin.usersys.redhat.com:8443/candlepin
		//	The system has been registered with ID: ed2d6c19-bde5-4e8c-b274-109d8d477723
		//	The registered system name is: jsefler-rhel7.usersys.redhat.com
		//	Installed Product Current Status:
		//
		//	Product Name: Red Hat Enterprise Linux Server
		//	Status:       Not Subscribed
		//
		//	Unable to find available subscriptions for all your installed products.
		//	System 'jsefler-rhel7.usersys.redhat.com' successfully registered.
		//
		//	[root@jsefler-rhel7 ~]# echo $?
		//	0
		
		// verify the migration was successful
		// WARNING: asserting this exit code is misleading because it is the exit code from the rhn-migrate-classic-to-rhsm.tcl script which is a wrapper to rhn-migrate-classic-to-rhsm which could exit with a different code
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "ExitCode from call to '"+rhnMigrateTool+"' after attempting to migrate with options '"+options+"'.");
		
		// verify that we are newly registered to RHSM
		Assert.assertNotNull(clienttasks.getCurrentConsumerCert(),"Confirmed that the system is newly registered with Subscription Manager after migrating from RHN Classic using '"+rhnMigrateTool+"'.");
		
		// assert the results contain the expected WARNING message for enabling plugins
		String expectedWarningMsg = "WARNING"+"\n\n"+"The yum plugins: PLUGINS were automatically enabled for the benefit of Red Hat Subscription Management. If not desired, use \"subscription-manager config --rhsm.auto_enable_yum_plugins=0\" to block this behavior.";
		expectedWarningMsg = "\n"+expectedWarningMsg+"\n";	// ensure expectedWarningMsg appears on it's own line
		if (!yumPluginConfFileForSubscriptionManagerEnabled) expectedWarningMsg = expectedWarningMsg.replaceFirst("PLUGINS", clienttasks.yumPluginConfFileForSubscriptionManager+", PLUGINS");
		if (!yumPluginConfFileForProductIdEnabled) expectedWarningMsg = expectedWarningMsg.replaceFirst("PLUGINS", clienttasks.yumPluginConfFileForProductId+", PLUGINS");
		expectedWarningMsg = expectedWarningMsg.replaceFirst(", PLUGINS", "");	// strip out my regex substring
		Assert.assertTrue(sshCommandResult.getStdout().contains(expectedWarningMsg),"The stdout from running '"+command+"' contains the expected warning message '"+expectedWarningMsg+"' when at least one yum plugin is disabled and rhsm.auto_enable_yum_plugins is configured on.");
		
		// assert that both plugins were enabled
		String enabledValue;
		enabledValue = clienttasks.getConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled");
		Assert.assertTrue(enabledValue.toLowerCase().matches("1|true"),"Expecting yum config file '"+clienttasks.yumPluginConfFileForProductId+"' to be enabled after running '"+command+"' with rhsm.auto_enable_yum_plugins configured on.  Actual enabled="+enabledValue);
		enabledValue = clienttasks.getConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled");
		Assert.assertTrue(enabledValue.toLowerCase().matches("1|true"),"Expecting yum config file '"+clienttasks.yumPluginConfFileForSubscriptionManager+"' to be enabled after running '"+command+"' with rhsm.auto_enable_yum_plugins configured on.  Actual enabled="+enabledValue);
	}
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47942", "RHEL7-109985"},
//			linkedWorkItems= {
//				@LinkedItem(
//					workitemId= "RHEL6-?????",	// TODO
//					project= Project.RHEL6,
//					role= DefTypes.Role.VERIFIES),
//				@LinkedItem(
//					workitemId= "RHEL7-95162",	// RHEL7-95162 - RHSM-REQ : rhn-migrate-classic-to-rhsm should automatically enable yum plugins
//					project= Project.RedHatEnterpriseLinux7,
//					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description="Verify that the yum plugins for /etc/yum/pluginconf.d/product-id.conf and /etc/yum/pluginconf.d/subscription-manager.conf are NOT automatically enabled by an execution of rhn-migrate-classic-to-rhsm when rhsm.auto_enable_yum_plugins is configured off.",
			groups={"Tier3Tests","blockedByBug-1466453","testRhnMigrateClassicToRhsmShouldAutomaticallyEnableYumPluginsWhenAutoEnableIsOn"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRhnMigrateClassicToRhsmShouldNotAutomaticallyEnableYumPluginsWhenAutoEnableIsOff() {
		// this bug is specifically designed to test Bug 1466453 - [RFE] rhn-migrate-classic-to-rhsm should ensure subscription-manager yum plugin is enabled
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.20.2-1")) {  // commit 7b2b89c7ba4fbf464dd988e12675b2b763007f2c 	1466453 - [RFE] rhn-migrate-classic-to-rhsm should ensure subscription-manager yum plugin is enabled
			throw new SkipException("This test applies a newer version of subscription-manager that includes an implementation for Bug 1466453 - [RFE] rhn-migrate-classic-to-rhsm should ensure subscription-manager yum plugin is enabled");
		}
		
		if (sm_rhnHostname.equals("")) throw new SkipException("This test requires access to RHN Classic or Satellite 5.");
		clienttasks.unregister(null, null, null, null);
		
		resetDefaultConfigurationsForYumPluginsAndRhsmAutoEnableYumPlugins();
		
		// turn off rhsm configuration for auto_enable_yum_plugins
		clienttasks.config(false,false,true,new String[]{"rhsm","auto_enable_yum_plugins","0"});
		
		// randomly disable one or both /etc/yum/pluginconf.d/product-id.conf and /etc/yum/pluginconf.d/subscription-manager.conf
		if (getRandomBoolean()) {
			clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled", "0");
			clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled", String.valueOf(getRandInt()%2)/* "0" or "1" */);
		} else {
			clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled", "false");
			clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled", "true");
			// TEMPORARY WORKAROUND
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1489917"; // Bug 1489917 - disabling yum plugins using "false" (rather than "0") causes traceback: invalid literal for int() with base 10: 'false'
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled", "0");
				clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled", "1");
			}
			// END OF WORKAROUND
		}
		
		String yumPluginConfFileForProductIdEnabledString = clienttasks.getConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled");
		String yumPluginConfFileForSubscriptionManagerEnabledString = clienttasks.getConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled");
		Boolean yumPluginConfFileForProductIdEnabled = Boolean.valueOf(yumPluginConfFileForProductIdEnabledString);
		Boolean yumPluginConfFileForSubscriptionManagerEnabled = Boolean.valueOf(yumPluginConfFileForSubscriptionManagerEnabledString);
		if (yumPluginConfFileForProductIdEnabledString.equals("1")) {yumPluginConfFileForProductIdEnabled = true;} else if (yumPluginConfFileForProductIdEnabledString.equals("0")) {yumPluginConfFileForProductIdEnabled = false;}
		if (yumPluginConfFileForSubscriptionManagerEnabledString.equals("1")) {yumPluginConfFileForSubscriptionManagerEnabled = true;} else if (yumPluginConfFileForSubscriptionManagerEnabledString.equals("0")) {yumPluginConfFileForSubscriptionManagerEnabled = false;}
		
		// register to rhn classic
		String rhnSystemId = clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		
		// attempt to run rhn-migrate-classic-to-rhsm
		String options = "";
		SSHCommandResult sshCommandResult = executeRhnMigrateClassicToRhsm(options, sm_rhnUsername, sm_rhnPassword, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null);
		String command = "rhn-migrate-classic-to-rhsm";
		
		//	[root@jsefler-rhel7 ~]# rhn-migrate-classic-to-rhsm --legacy-user=rhsm-client --destination-user=testuser1
		//	Legacy password: 
		//	Destination password: 
		//	Org: admin
		//
		//	Retrieving existing legacy subscription information...
		//
		//	+-----------------------------------------------------+
		//	System is currently subscribed to these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-server-7
		//
		//	+-----------------------------------------------------+
		//	Installing product certificates for these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-server-7
		//
		//	Product certificates installed successfully to /etc/pki/product.
		//
		//	Preparing to unregister system from legacy server...
		//	System successfully unregistered from legacy server.
		//	Stopping and disabling legacy services...
		//	rhnsd.service is not a native service, redirecting to /sbin/chkconfig.
		//	Executing /sbin/chkconfig rhnsd off
		//
		//	Attempting to register system to destination server...
		//
		//	Registering to: jsefler-candlepin.usersys.redhat.com:8443/candlepin
		//	The system has been registered with ID: ed2d6c19-bde5-4e8c-b274-109d8d477723
		//	The registered system name is: jsefler-rhel7.usersys.redhat.com
		//	Installed Product Current Status:
		//
		//	Product Name: Red Hat Enterprise Linux Server
		//	Status:       Not Subscribed
		//
		//	Unable to find available subscriptions for all your installed products.
		//	System 'jsefler-rhel7.usersys.redhat.com' successfully registered.
		//
		//	[root@jsefler-rhel7 ~]# echo $?
		//	0
		
		// verify the migration was successful
		// WARNING: asserting this exit code is misleading because it is the exit code from the rhn-migrate-classic-to-rhsm.tcl script which is a wrapper to rhn-migrate-classic-to-rhsm which could exit with a different code
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "ExitCode from call to '"+rhnMigrateTool+"' after attempting to migrate with options '"+options+"'.");
		
		// verify that we are newly registered to RHSM
		Assert.assertNotNull(clienttasks.getCurrentConsumerCert(),"Confirmed that the system is newly registered with Subscription Manager after migrating from RHN Classic using '"+rhnMigrateTool+"'.");		
		
		// assert the results do NOT contain a WARNING message
		String expectedWarningMsg = "WARNING";	// +"\n\n"+"The yum plugins: (PLUGINS) were automatically enabled for the benefit of Red Hat Subscription Management. If not desired, use \"subscription-manager config --rhsm.auto_enable_yum_plugins=0\" to block this behavior.";
		Assert.assertTrue(!sshCommandResult.getStdout().contains(expectedWarningMsg),"The stdout from running '"+command+"' does NOT contain the expected warning message '"+expectedWarningMsg+"' when at least one yum plugin is disabled and rhsm.auto_enable_yum_plugins is configured off.");
		
		// assert that both plugins were untouched
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.yumPluginConfFileForProductId, "enabled"),yumPluginConfFileForProductIdEnabledString,"Enablement of yum config file '"+clienttasks.yumPluginConfFileForProductId+"' should remain unchanged after running '"+command+"' with rhsm.auto_enable_yum_plugins configured off.");
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.yumPluginConfFileForSubscriptionManager, "enabled"),yumPluginConfFileForSubscriptionManagerEnabledString,"Enablement of yum config file '"+clienttasks.yumPluginConfFileForSubscriptionManager+"' should remain unchanged after running '"+command+"' with rhsm.auto_enable_yum_plugins configured off.");
		
		// assert that the yum plugins are/not loaded
		//	[root@jsefler-rhel7 ~]# yum repolist --disablerepo=*
		//	Loaded plugins: langpacks, product-id, search-disabled-repos, subscription-manager
		//	This system is not registered with an entitlement server. You can use subscription-manager to register.
		//	repolist: 0
		String plugin;
		command = "yum repolist --disablerepo=*";
		sshCommandResult = client.runCommandAndWait(command);
		plugin="product-id";
		if (yumPluginConfFileForProductIdEnabled) {
			Assert.assertTrue(sshCommandResult.getStdout().contains(plugin),"The stdout from running '"+command+"' indicated that plugin '"+plugin+"' was loaded.");
		} else {
			Assert.assertTrue(!sshCommandResult.getStdout().contains(plugin),"The stdout from running '"+command+"' indicated that plugin '"+plugin+"' was NOT loaded.");
		}
		plugin="subscription-manager";
		if (yumPluginConfFileForSubscriptionManagerEnabled) {
			Assert.assertTrue(sshCommandResult.getStdout().contains(plugin),"The stdout from running '"+command+"' indicated that plugin '"+plugin+"' was loaded.");
		} else {
			Assert.assertTrue(!sshCommandResult.getStdout().contains(plugin),"The stdout from running '"+command+"' indicated that plugin '"+plugin+"' was NOT loaded.");
		}
	}
	
	
	
	
	
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 789007 - Migrate with normal user (non org admin) user . https://github.com/RedHatQE/rhsm-qe/issues/171
	// TODO Bug 816377 - rhn-migrate-classic-to-rhsm throws traceback when subscription-manager-migration-data is not installed https://github.com/RedHatQE/rhsm-qe/issues/172
	// TODO Bug 786450 - “Install-num-migrate-to-rhsm “ command not working as expected for ppc64 box (TODO FIGURE OUT IF EXISTING AUTOMATION ALREADY COVERS THIS ON PPC64) https://github.com/RedHatQE/rhsm-qe/issues/173
	// TODO Bug 863428 - Migration failed with message Organization A has more than one environment. https://github.com/RedHatQE/rhsm-qe/issues/174
	// TODO Bug 866579 - rhn-migrate-classic-to-rhsm leaves system unregistered when a non-existant environment is specified/mistyped https://github.com/RedHatQE/rhsm-qe/issues/175
	// TODO Bug 881952 - ssl.SSLError: The read operation timed out (during large rhn-migrate-classic-to-rhsm) https://github.com/RedHatQE/rhsm-qe/issues/176
	
	// Configuration methods ***********************************************************************
	
	
	@BeforeClass(groups="setup")
	public void setupBeforeClass() {
		if (clienttasks==null) return;
		
		// determine the full path to the channelCertMappingFile
		baseProductsDir+="-"+clienttasks.redhatReleaseX;
		channelCertMappingFilename = baseProductsDir+"/"+channelCertMappingFilename;
		backupChannelCertMappingFilename = baseProductsDir+"/"+backupChannelCertMappingFilename;
		
		// make sure needed rpms are installed
		for (String pkg : new String[]{"subscription-manager-migration", "subscription-manager-migration-data", "expect"}) {
			Assert.assertTrue(clienttasks.isPackageInstalled(pkg),"Required package '"+pkg+"' is installed for MigrationTests.");
		}
		
		// make sure we have the RHN-ORG-TRUSTED-SSL-CERT for the rhn/satellite server
		updateSslCaCertConfigInRhnUp2dateFile();
	}
	public void updateSslCaCertConfigInRhnUp2dateFile() {

		// make sure we have the RHN-ORG-TRUSTED-SSL-CERT for the rhn/satellite server
		/*
		 * 	1. Set automation parameters:
		 * 		sm.rhn.hostname : https://sat-56-server.usersys.redhat.com
		 *		sm.rhn.username : admin
		 *		sm.rhn.password : *****
		 *  2. Use firefox to login to the Satellite account
		 *      https://sat-56-server.usersys.redhat.com/rhn/Login.do
		 *      do whatever work you need to there
		 *  3. Get the CA cert from Satellite and install it onto your client
		 *      wget --no-verbose --no-check-certificate --output-document=/usr/share/rhn/RHN-ORG-TRUSTED-SSL-CERT_sat-56-server.usersys.redhat.com https://sat-56-server.usersys.redhat.com/pub/RHN-ORG-TRUSTED-SSL-CERT
		 *  4. Update the /etc/sysconfig/rhn/up2date with
		 *      sslCACert=RHN-ORG-TRUSTED-SSL-CERT_sat-56-server.usersys.redhat.com
		 */
		// Get the CA cert from Satellite and install it onto your client
		if (!sm_rhnHostname.isEmpty()) {
//			if (sm_rhnHostname.startsWith("http") { 	// indicates that we are migrating from a non-hosted rhn server - as opposed to rhn.code.stage.redhat.com (stage) or rhn.redhat.com (production)
//			if (!doesStringContainMatches(sm_rhnHostname, "rhn\\.(.+\\.)*redhat\\.com")) {	// indicates that we are migrating from a non-hosted rhn server - as opposed to rhn.code.stage.redhat.com (stage) or rhn.redhat.com (production)
			if (!doesStringContainMatches(sm_rhnHostname, "(rhn|rhsm)\\.(.+\\.)*redhat\\.com")) {	// indicates that we are migrating from a non-hosted rhn server - as opposed to rhn.code.stage.redhat.com (stage) or rhn.redhat.com (production)
				String satHostname = sm_rhnHostname.split("/")[2];	// https://sat-56-server.usersys.redhat.com
				String satCaCertPath = "/usr/share/rhn/RHN-ORG-TRUSTED-SSL-CERT"+"_"+satHostname;
				RemoteFileTasks.runCommandAndAssert(client,"wget --no-verbose --no-check-certificate --output-document="+satCaCertPath+" "+sm_rhnHostname+"/pub/RHN-ORG-TRUSTED-SSL-CERT",Integer.valueOf(0),null,"-> \""+satCaCertPath+"\"");
				
				// Update /etc/sysconfig/rhn/up2date->sslCACert with satCaCertPath
				clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "sslCACert", satCaCertPath);	// sslCACert[comment]=The CA cert used to verify the ssl server
			}
		}
	}
	
	@BeforeClass(groups="setup", dependsOnMethods={/*NOT TRUE "setupBeforeClass"*/})
	public void rememberOriginallyInstalledRedHatProductCertsBeforeClass() {
		if (clienttasks==null) return;
		
		// review the currently installed product certs and filter out the ones from test automation (indicated by suffix "_.pem")
		for (File productCertFile : clienttasks.getCurrentProductCertFiles()) {
			if (!productCertFile.getName().endsWith("_.pem")) {	// The product cert files ending in "_.pem" are not true RedHat products
				originallyInstalledRedHatProductCerts.add(clienttasks.getProductCertFromProductCertFile(productCertFile));
			}
		}
	}
	
	@BeforeClass(groups="setup", dependsOnMethods={/*NOT TRUE "setupBeforeClass"*/})
	public void rememberOriginallyConfiguredServerUrlBeforeClass() {
		if (clienttasks==null) return;
		
		originalServerHostname 	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname");
		originalServerPort		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port");
		originalServerPrefix	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "prefix");
	}
	
	@BeforeClass(groups="setup", dependsOnMethods={/*NOT TRUE "setupBeforeClass"*/})
	public void backupProductCertsBeforeClass() {
		if (clienttasks==null) return;
		
		// determine the original productCertDir value
		//productCertDirRestore = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "productCertDir");
		originalProductCertDir = clienttasks.productCertDir;
		
		log.info("Backing up all the currently installed product certs...");
		client.runCommandAndWait("mkdir -p "+backupProductCertDir+"; rm -f "+backupProductCertDir+"/*.pem");
		client.runCommandAndWait("cp "+originalProductCertDir+"/*.pem "+backupProductCertDir);
	}
	@BeforeClass(groups="setup", dependsOnMethods={/*NOT TRUE "setupBeforeClass"*/})
	public void backupProductIdJsonFileBeforeClass() {
		if (clienttasks==null) return;
		RemoteFileTasks.runCommandAndAssert(client,"cat "+clienttasks.productIdJsonFile+" > "+backupProductIdJsonFile, Integer.valueOf(0));
	}
	
	@BeforeGroups(groups="setup",value={"InstallNumMigrateToRhsmWithInstNumber_Test","InstallNumMigrateToRhsm_Test","RhnMigrateClassicToRhsm_Test"})
	public void configOriginalRhsmProductCertDir() {
		if (clienttasks==null) return;
		
		//clienttasks.config(false, false, true, new String[]{"rhsm","productcertdir",productCertDirOriginal});
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", originalProductCertDir);
	}
	
	@BeforeClass(groups="setup")
	@AfterGroups(groups="setup",value={"RhnMigrateClassicToRhsmUsingProxyServer_Test"})
	public void removeProxyServerConfigurations() {
		if (clienttasks==null) return;
		
		// remove proxy settings from rhsm.conf
		// these will actually remove the value from the config file; don't do this
		//clienttasks.config(false, true, false, new String[]{"server","proxy_hostname"});
		//clienttasks.config(false, true, false, new String[]{"server","proxy_user"});
		//clienttasks.config(false, true, false, new String[]{"server","proxy_password"});
		//clienttasks.config(false, true, false, new String[]{"server","proxy_port"});
		clienttasks.config(false, false, true, new String[]{"server","proxy_hostname",""});
		clienttasks.config(false, false, true, new String[]{"server","proxy_user",""});
		clienttasks.config(false, false, true, new String[]{"server","proxy_password",""});
		clienttasks.config(false, false, true, new String[]{"server","proxy_port",""});
		
		// remove proxy settings from up2date
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxy", "0");		// enableProxyAuth[comment]=To use an authenticated proxy or not
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "httpProxy", "");			// httpProxy[comment]=HTTP proxy in host:port format, e.g. squid.redhat.com:3128
		// Note: On RHEL7, these three configurations will not have been set in /etc/sysconfig/rhn/up2date because a successful rhnreg_ks will not have occurred
		if (clienttasks.getConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxyAuth")==null)	{clienttasks.addConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxyAuth", "0");}	else {clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxyAuth", "0");}	// enableProxyAuth[comment]=To use an authenticated proxy or not
		if (clienttasks.getConfFileParameter(clienttasks.rhnUp2dateFile, "proxyUser")==null)		{clienttasks.addConfFileParameter(clienttasks.rhnUp2dateFile, "proxyUser", "");}		else {clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "proxyUser", "");}		// proxyUser[comment]=The username for an authenticated proxy
		if (clienttasks.getConfFileParameter(clienttasks.rhnUp2dateFile, "proxyPassword")==null)	{clienttasks.addConfFileParameter(clienttasks.rhnUp2dateFile, "proxyPassword", "");}	else {clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "proxyPassword", "");}	// proxyPassword[comment]=The password to use for an authenticated proxy
		iptablesAcceptPort(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port"));
	}
	
	@BeforeClass(groups="setup", dependsOnMethods={/*NOT TRUE "setupBeforeClass"*/})
	public void copyScriptsToClient() throws IOException {
		if (client==null) return;
		
		// copy the rhn-channels.py script to the client
		File rhnChannelsScriptFile = new File(System.getProperty("automation.dir", null)+"/scripts/rhn-channels.py");
		if (!rhnChannelsScriptFile.exists()) Assert.fail("Failed to find expected script: "+rhnChannelsScriptFile);
		RemoteFileTasks.putFile(client, rhnChannelsScriptFile.toString(), "/usr/local/bin/", "0755");
		
		// copy the rhn-is-registered.py script to the client
		File rhnIsRegisteredScriptFile = new File(System.getProperty("automation.dir", null)+"/scripts/rhn-is-registered.py");
		if (!rhnIsRegisteredScriptFile.exists()) Assert.fail("Failed to find expected script: "+rhnIsRegisteredScriptFile);
		RemoteFileTasks.putFile(client, rhnIsRegisteredScriptFile.toString(), "/usr/local/bin/", "0755");
		
		// copy the rhn-delete-systems.py script to the client
		File rhnDeleteSystemsScriptFile = new File(System.getProperty("automation.dir", null)+"/scripts/rhn-delete-systems.py");
		if (!rhnDeleteSystemsScriptFile.exists()) Assert.fail("Failed to find expected script: "+rhnDeleteSystemsScriptFile);
		RemoteFileTasks.putFile(client, rhnDeleteSystemsScriptFile.toString(), "/usr/local/bin/", "0755");
		
		// copy the rhn-migrate-classic-to-rhsm.tcl script to the client
		File expectScriptFile = new File(System.getProperty("automation.dir", null)+"/scripts/rhn-migrate-classic-to-rhsm.tcl");
		if (!expectScriptFile.exists()) Assert.fail("Failed to find expected script: "+expectScriptFile);
		RemoteFileTasks.putFile(client, expectScriptFile.toString(), "/usr/local/bin/", "0755");
	}
	
	@BeforeClass(groups="setup", dependsOnMethods={"setupBeforeClass","copyScriptsToClient"})
	public void determineRhnClassicBaseAndAvailableChildChannels() throws IOException {
		if (sm_rhnUsername.equals("")) {log.warning("Skipping determination of the base and available RHN Classic channels"); return;}
		if (sm_rhnPassword.equals("")) {log.warning("Skipping determination of the base and available RHN Classic channels"); return;}
		if (sm_rhnHostname.equals("")) {log.warning("Skipping determination of the base and available RHN Classic channels"); return;}
		
		// get the base channel
		clienttasks.registerToRhnClassic_(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		List<String> rhnChannels = clienttasks.getCurrentRhnClassicChannels();
		//Assert.assertEquals(rhnChannels.size(), 1, "The number of base RHN Classic base channels this system is consuming.");
		if (rhnChannels.isEmpty()) {
			log.warning("When no RHN channels are available to this classically registered system, no product certs will be migrated to RHSM.");
			return; 
		}
		rhnBaseChannel = clienttasks.getCurrentRhnClassicChannels().get(0);

		// get all of the available RHN Classic child channels available for consumption under this base channel
		rhnAvailableChildChannels.clear();
		String serverUrl = sm_rhnHostname; if (!serverUrl.startsWith("http")) serverUrl="https://"+sm_rhnHostname;
		String command = String.format("rhn-channels.py --username=%s --password=%s --serverurl=%s --basechannel=%s --no-custom --available", sm_rhnUsername, sm_rhnPassword, serverUrl, rhnBaseChannel);
		///*debugTesting RHEL5*/ if (true) command = "echo rhel-x86_64-server-5 && echo rhx-alfresco-enterprise-2.0-rhel-x86_64-server-5 && echo rhx-amanda-enterprise-backup-2.6-rhel-x86_64-server-5";
		///*debugTesting RHEL6*/ if (true) command = "echo rhel-x86_64-server-6 && echo rhel-x86_64-server-rs-6 && echo rhel-x86_64-server-sfs-6 && echo rhel-x86_64-server-lb-6";
		
		SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0));
		rhnChannels = new ArrayList<String>();
		if (!result.getStdout().trim().equals("")) {
			rhnChannels	= Arrays.asList(result.getStdout().trim().split("\\n"));
		}
		for (String rhnChannel : rhnChannels) {
			if (!rhnChannel.equals(rhnBaseChannel)) rhnAvailableChildChannels.add(rhnChannel.trim()); 
		}
		//Assert.assertTrue(rhnAvailableChildChannels.size()>0,"A positive number of child channels under the RHN Classic base channel '"+rhnBaseChannel+"' are available for consumption.");
		if (rhnAvailableChildChannels.isEmpty()) log.warning("Did NOT find any child channels under the RHN Classic base channel '"+rhnBaseChannel+"' available for consumption.");
	}
	
	protected List<SubscriptionPool> availableSubscriptionPools = null;	// used to create activation keys
	protected String clientOrgKey = null;
	@BeforeClass(groups="setup")
	public void determineAvailableSubscriptions() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg,null,null,null,null,false,null,null,(String)null,null,null,null,true,false,null,null,null, null);
		clientOrgKey = clienttasks.getCurrentlyRegisteredOwnerKey();
		availableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		clienttasks.unregister(null, null, null, null);
	}
	
	@BeforeGroups(groups="setup",value={"InstallNumMigrateToRhsmWithNonDefaultProductCertDir_Test","RhnMigrateClassicToRhsmWithNonDefaultProductCertDir_Test"})
	public void configNonDefaultRhsmProductCertDir() {
		if (clienttasks==null) return;
		
		//clienttasks.config(false, false, true, new String[]{"rhsm","productcertdir",productCertDirNonDefault});
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", nonDefaultProductCertDir);
	}

	
	public static SSHCommandRunner basicAuthProxyRunner = null;
	public static SSHCommandRunner noAuthProxyRunner = null;
	protected String ipv4_address = null;
	@BeforeClass(groups={"setup"})
	public void setupProxyRunnersBeforeClass() throws IOException {
		basicAuthProxyRunner = new SSHCommandRunner(sm_basicauthproxyHostname, sm_basicauthproxySSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase, null);
		noAuthProxyRunner = new SSHCommandRunner(sm_noauthproxyHostname, sm_noauthproxySSHUser	, sm_sshKeyPrivate, sm_sshkeyPassphrase, null);
		if (sm_sshEmergenecyTimeoutMS!=null) basicAuthProxyRunner.setEmergencyTimeout(Long.valueOf(sm_sshEmergenecyTimeoutMS));
		if (sm_sshEmergenecyTimeoutMS!=null) noAuthProxyRunner.setEmergencyTimeout(Long.valueOf(sm_sshEmergenecyTimeoutMS));
		if (clienttasks!=null) ipv4_address = clienttasks.getIPV4Address();
	}
	
	@AfterClass(groups="setup",alwaysRun=true)
	public void restoreProductCertsAfterClass() {
		if (clienttasks==null) return;
		if (originalProductCertDir==null) return;
		log.info("Restoring the originally installed product certs...");
		client.runCommandAndWait("rm -f "+originalProductCertDir+"/*.pem");
		client.runCommandAndWait("cp "+backupProductCertDir+"/*.pem "+originalProductCertDir);
		configOriginalRhsmProductCertDir();
	}
	
	@AfterClass(groups="setup",alwaysRun=true)
	public void restoreProductIdJsonFileAfterClass() {
		if (clienttasks==null) return;
		if (!RemoteFileTasks.testExists(client, backupProductIdJsonFile)) return;
		RemoteFileTasks.runCommandAndAssert(client,"cat "+backupProductIdJsonFile+" > "+clienttasks.productIdJsonFile, Integer.valueOf(0));
		clienttasks.yumClean("all");
	}
	
	@AfterClass(groups="setup",alwaysRun=true)
	@AfterGroups(groups="setup",value={"RhnMigrateClassicToRhsm_Test"})
	public void restoreOriginallyConfiguredServerUrl() {
		if (clienttasks==null) return;
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		if (originalServerHostname!=null)	listOfSectionNameValues.add(new String[]{"server","hostname",originalServerHostname});
		if (originalServerPort!=null)		listOfSectionNameValues.add(new String[]{"server","port",originalServerPort});
		if (originalServerPrefix!=null)		listOfSectionNameValues.add(new String[]{"server","prefix",originalServerPrefix});
		log.info("Restoring the originally configured server URL...");
		clienttasks.config(null, null, true, listOfSectionNameValues);
	}
	
	@AfterClass(groups={"setup"},alwaysRun=true)
	public void removeRHNSystemIdFileAfterClass() {
		if (clienttasks!=null) {
			clienttasks.removeRhnSystemIdFile();
		}
	}
	
	@AfterClass(groups={"setup"},alwaysRun=true)
	public void deleteRhnSystemsRegisteredByNameAfterClass() {
		if (clienttasks!=null) {
			clienttasks.deleteRhnSystemsRegisteredByName(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, "rhsm-automation."+clienttasks.hostname);
			clienttasks.removeRhnSystemIdFile();
		}
	}
	
	
	@BeforeClass(groups={"setup"}, dependsOnMethods={"setupBeforeClass"})
	public void buildMapsBeforeClass() throws UnsupportedEncodingException, IOException {

		// Read the channelCertMappingFilename as if they were properties (Warning! this will mask non-unique mappings)
		// [root@jsefler-onprem-5client ~]# cat /usr/share/rhsm/product/RHEL-5/channel-cert-mapping.txt
		// rhn-tools-rhel-x86_64-server-5-beta: none
		// rhn-tools-rhel-x86_64-server-5: Server-Server-x86_64-fbe6b460-a559-4b02-aa3a-3e580ea866b2-69.pem
		// rhn-tools-rhel-x86_64-client-5-beta: none
		// rhn-tools-rhel-x86_64-client-5: Client-Client-x86_64-efe91c1c-78d7-4d19-b2fb-3c88cfc2da35-68.pem
		SSHCommandResult result = client.runCommandAndWait("cat "+channelCertMappingFilename);
		Properties p = new Properties();
		p.load(new ByteArrayInputStream(result.getStdout().getBytes("UTF-8")));
		for (Object key: p.keySet()){
			// load the channelsToProductCertFilesMap
			channelsToProductCertFilenamesMap.put((String)key, p.getProperty((String)(key)));
			// load the mappedProductCertFiles
			if (!channelsToProductCertFilenamesMap.get(key).equalsIgnoreCase("none"))
				mappedProductCertFilenames.add(channelsToProductCertFilenamesMap.get(key));
		}
	}
	
	
	
	
	
	// Protected methods ***********************************************************************
	protected String baseProductsDir = "/usr/share/rhsm/product/RHEL";
	protected String channelCertMappingFilename = "channel-cert-mapping.txt";
	protected String backupChannelCertMappingFilename = channelCertMappingFilename+".bak";
	protected List<String> mappedProductCertFilenames = new ArrayList<String>();	// list of all the mapped product cert file names in the mapping file (e.g. Server-Server-x86_64-fbe6b460-a559-4b02-aa3a-3e580ea866b2-69.pem)
	protected Map<String,String> channelsToProductCertFilenamesMap = new HashMap<String,String>();	// map of all the channels to product cert file names (e.g. key=rhn-tools-rhel-x86_64-server-5 value=Server-Server-x86_64-fbe6b460-a559-4b02-aa3a-3e580ea866b2-69.pem)
	protected List<ProductCert> originallyInstalledRedHatProductCerts = new ArrayList<ProductCert>();
	protected String migrationFromFact				= "migration.migrated_from";
	protected String migrationSystemIdFact			= "migration.classic_system_id";
	protected String migrationDateFact				= "migration.migration_date";
	protected String originalProductCertDir			= null;
	protected final String backupProductCertDir		= "/tmp/backupOfProductCertDir";
	protected final String backupProductIdJsonFile	= "/tmp/backupOfProductIdJsonFile";
	protected String nonDefaultProductCertDir		= "/tmp/sm-migratedProductCertDir";
	protected String machineInstNumberFile			= "/etc/sysconfig/rhn/install-num";
	protected String backupMachineInstNumberFile	= machineInstNumberFile+".bak";
	protected String rhnBaseChannel = null;
	protected List<String> rhnAvailableChildChannels = new ArrayList<String>();
	static public String installNumTool = "install-num-migrate-to-rhsm";
	static public String rhnMigrateTool = "rhn-migrate-classic-to-rhsm";
	protected String originalServerHostname = null;
	protected String originalServerPort = null;
	protected String originalServerPrefix = null;
	
	protected Set<String> getExpectedMappedProductCertFilenamesCorrespondingToChannels(List<String> channels) {
		Set<String> mappedProductCertFilenamesCorrespondingToChannels = new HashSet<String>();
		for (String channel : channels) {
			String mappedProductCertFilename = channelsToProductCertFilenamesMap.get(channel);
			if (mappedProductCertFilename==null) {
				//log.warning("RHN Classic channel '"+channel+"' is NOT mapped in the file '"+channelCertMappingFilename+"'.");
			} else {
				log.info("The mapped product cert filename for RHN Classic channel '"+channel+"' is: "+mappedProductCertFilename);
				if (!mappedProductCertFilename.equalsIgnoreCase("none")) {
					mappedProductCertFilenamesCorrespondingToChannels.add(mappedProductCertFilename);
				}
			}
		}
		// SPECIAL CASE:  Red Hat Enterprise Workstation vs. Red Hat Enterprise Desktop
		// See https://bugzilla.redhat.com/show_bug.cgi?id=786257#c1
		
		//	>	if customer subscribed to rhel-x86_64-client-supplementary-5:
		//	>	   if customer subscribes to rhel-x86_64-client-workstation-5:
		//	>	      install 71.pem
		//	>	   else:
		//	>	      install 68.pem
		
		// is product id 68 for "Red Hat Enterprise Linux Desktop" among the mappedProductCertFilenames
		String mappedProductCertFilenameCorrespondingToBaseChannel = null;
		String productIdForBase = "68";	// Red Hat Enterprise Desktop
		for (String mappedProductCertFilename : mappedProductCertFilenamesCorrespondingToChannels) {
			if (MigrationDataTests.getProductIdFromProductCertFilename(mappedProductCertFilename).equals(productIdForBase)) {
				mappedProductCertFilenameCorrespondingToBaseChannel = mappedProductCertFilename; break;
			}
		}
		if (mappedProductCertFilenameCorrespondingToBaseChannel!=null) {
			File mappedProductCertFileCorrespondingToBaseChannel = new File(baseProductsDir+"/"+mappedProductCertFilenameCorrespondingToBaseChannel);
			for (String productId : Arrays.asList("71"/*Red Hat Enterprise Workstation*/)) {
				for (String mappedProductCertFilename : new HashSet<String>(mappedProductCertFilenamesCorrespondingToChannels)) {
					if (MigrationDataTests.getProductIdFromProductCertFilename(mappedProductCertFilename).equals(productId)) {
						File mappedProductCertFileCorrespondingToAddonChannel = new File(baseProductsDir+"/"+mappedProductCertFilename);
						ProductCert productCertBase = clienttasks.getProductCertFromProductCertFile(mappedProductCertFileCorrespondingToBaseChannel);
						ProductCert productCertAddon = clienttasks.getProductCertFromProductCertFile(mappedProductCertFileCorrespondingToAddonChannel);
						log.warning("SPECIAL CASE ENCOUNTERED: "+rhnMigrateTool+" should NOT install product cert "+productIdForBase+" ["+productCertBase.productName+"] when product cert "+productId+" ["+productCertAddon.productName+"] is also installed.");
						mappedProductCertFilenamesCorrespondingToChannels.remove(mappedProductCertFilenameCorrespondingToBaseChannel);
					}
				}
			}
		}
		
		// SPECIAL CASE:  Red Hat Beta vs. Red Hat Developer Toolset (for RHEL [Server|HPC Node|Client|Workstation])
		// Check for special case!  email thread by dgregor entitled "Product certificates for a few channels"
		// 180.pem is "special".  It's for the "Red Hat Beta" product, which is this generic placeholder
		// that we created and it isn't tied to any specific Red Hat product release.
						
		//	> After the migration tool does it's normal migration logic, there is a hard-coded cleanup to...
		//	>   if both 180.pem (rhel-ARCH-server-dts-5-beta) and 176.pem (rhel-ARCH-server-dts-5) were migrated
		//	>       remove 180.pem from /etc/pki/product
		//	>   if both 180.pem (rhel-ARCH-client-dts-5-beta) and 178.pem (rhel-ARCH-client-dts-5) were migrated
		//	>       remove 180.pem from /etc/pki/product

		// is product id 180 for "Red Hat Beta" among the mappedProductCertFilenames
		mappedProductCertFilenameCorrespondingToBaseChannel = null;
		productIdForBase = "180";	// Red Hat Beta
		for (String mappedProductCertFilename : mappedProductCertFilenamesCorrespondingToChannels) {
			if (MigrationDataTests.getProductIdFromProductCertFilename(mappedProductCertFilename).equals(productIdForBase)) {
				mappedProductCertFilenameCorrespondingToBaseChannel = mappedProductCertFilename; break;
			}
		}
		if (mappedProductCertFilenameCorrespondingToBaseChannel!=null) {
			File mappedProductCertFileCorrespondingToBaseChannel = new File(baseProductsDir+"/"+mappedProductCertFilenameCorrespondingToBaseChannel);
			for (String productId : Arrays.asList("176"/*Red Hat Developer Toolset (for RHEL Server)*/, "177"/*Red Hat Developer Toolset (for RHEL HPC Node)*/, "178"/*Red Hat Developer Toolset (for RHEL Client)*/, "179"/*Red Hat Developer Toolset (for RHEL Workstation)*/)) {
				for (String mappedProductCertFilename : new HashSet<String>(mappedProductCertFilenamesCorrespondingToChannels)) {
					if (MigrationDataTests.getProductIdFromProductCertFilename(mappedProductCertFilename).equals(productId)) {
						File mappedProductCertFileCorrespondingToAddonChannel = new File(baseProductsDir+"/"+mappedProductCertFilename);
						ProductCert productCertBase = clienttasks.getProductCertFromProductCertFile(mappedProductCertFileCorrespondingToBaseChannel);
						ProductCert productCertAddon = clienttasks.getProductCertFromProductCertFile(mappedProductCertFileCorrespondingToAddonChannel);
						log.warning("SPECIAL CASE ENCOUNTERED: "+rhnMigrateTool+" should NOT install product cert "+productIdForBase+" ["+productCertBase.productName+"] when product cert "+productId+" ["+productCertAddon.productName+"] is also installed.");
						mappedProductCertFilenamesCorrespondingToChannels.remove(mappedProductCertFilenameCorrespondingToBaseChannel);
					}
				}
			}
		}
		
		return mappedProductCertFilenamesCorrespondingToChannels;
	}
	
	protected Set<String> getProductCertFilenamesContainingNonUniqueProductIds(Set<String> productCertFilenames) {
		// Given a set of product cert file names, this method will return those that do not have a unique product ID.
		// For example:
		//    input  [Server-Server-x86_64-23d36f276d57-69.pem, product-x86_64-4d1f929972d7-150.pem, Server-Server-x86_64-323beb20e916-69.pem]
		//    return [Server-Server-x86_64-23d36f276d57-69.pem, Server-Server-x86_64-323beb20e916-69.pem]
		Set<String> productCertFilenamesWithDuplicateProductIds = new HashSet<String>();
		Set<String> allProductIds = new HashSet<String>();
		Set<String> dupProductIds = new HashSet<String>();
		for (String productCertFilename: productCertFilenames) {
			String productId = MigrationDataTests.getProductIdFromProductCertFilename(productCertFilename);
			if (allProductIds.contains(productId)) dupProductIds.add(productId);
			allProductIds.add(productId);
		}
		for (String productCertFilename: productCertFilenames) {
			String productId = MigrationDataTests.getProductIdFromProductCertFilename(productCertFilename);
			if (dupProductIds.contains(productId)) productCertFilenamesWithDuplicateProductIds.add(productCertFilename);
		}
		return productCertFilenamesWithDuplicateProductIds;
	}
	
	protected boolean areAllChannelsMapped(List<String> channels) {
		boolean allChannelsAreMapped = true;
		for (String channel : channels) {
			String mappedProductCertFilename = channelsToProductCertFilenamesMap.get(channel);
			if (mappedProductCertFilename==null) {
				allChannelsAreMapped = false;
				log.warning("RHN Classic channel '"+channel+"' is NOT mapped in the file '"+channelCertMappingFilename+"'.");
			}
		}
		return allChannelsAreMapped;
	}
	
	/**
	 * Use the python instnum.py program to determine what mapped product cert filenames from the channel-cert-mapping.txt correspond to this instnumber and should therefore be copied.
	 * @param instnumber
	 * @return
	 * @throws JSONException
	 */
	protected List<String> getExpectedMappedProductCertFilenamesCorrespondingToInstnumberUsingInstnumTool(String instnumber) throws JSONException {
		List<String> mappedProductCertFilenamesCorrespondingToInstnumber = new ArrayList<String>();

		String command = "python /usr/lib/python2.4/site-packages/instnum.py "+instnumber;
		//SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(client,command,0);
		SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(client,command+" | egrep \"^{.*}$\"", 0);
		// [root@jsefler-onprem-5server ~]# python /usr/lib/python2.4/site-packages/instnum.py 0000000e0017fc01 | egrep "^{.*}$"
		// {'Virt': 'VT', 'Workstation': 'Workstation', 'Base': 'Client'}
		
		// decide what product arch applies to our system
		String arch = clienttasks.arch;	// default
		if (clienttasks.redhatReleaseX.equals("5") && clienttasks.arch.equals("ppc64")) arch = "ppc";	// RHEL5 only supports ppc packages, but can be run on ppc64 hardware
		if (Arrays.asList("i386","i486","i586","i686").contains(clienttasks.arch)) arch = "i386";		// RHEL supports i386 packages, but can be run on all 32-bit arch hardware
		
		// process result as a json object
		JSONObject jsonResult = new JSONObject(result.getStdout());
		String base = jsonResult.getString("Base");
		
		// Workstation (71.pem) is a special sub of the base Client (68.pem) - see bug 790217
		// when the Workstation key is present on a base Client install, remove the base key - the effect will be to trump the base product cert with the Workstation product cert
		if (jsonResult.has("Workstation")/* && base.equalsIgnoreCase("client") I DON'T THINK THIS IS NECESSARY*/) {
			log.warning("This appears to be a Workstation install ("+instnumber+"). Therefore we will assume that the Workstation product cert trumps the Base.");
			jsonResult.remove("Base");
		}
		
		for (String mappedProductCertFilename : mappedProductCertFilenames) {
			// example mappedProductCertFilenames:
			// Server-Server-s390x-340665cdadee-72.pem  
			// Server-ClusterStorage-ppc-a3fea9e1dde3-90.pem
			// base-sub-arch-hash-id.pem
			Iterator<?> keys = jsonResult.keys();
			while (keys.hasNext()) {
				String key = (String)keys.next();
				String sub = jsonResult.getString(key);
				if (mappedProductCertFilename.startsWith(base+"-"+sub+"-"+arch+"-")) {
					if (!mappedProductCertFilenamesCorrespondingToInstnumber.contains(mappedProductCertFilename)) {	// make sure the list contains unique filenames
						mappedProductCertFilenamesCorrespondingToInstnumber.add(mappedProductCertFilename);
					}
				}
			}
		}
		
		return mappedProductCertFilenamesCorrespondingToInstnumber;
	}
	
	
	
	/**
	 * Call rhn-migrate-classic-to-rhsm without asserting results.
	 * @param options - command line options understood by rhn-migrate-classic-to-rhsm
	 * @param rhnUsername - enter at the prompt for Red Hat Username
	 * @param rhnPassword - enter at the prompt for Red Hat Password
	 * @param rhsmUsername - enter at the prompt for System Engine Username (will be used for subscription-manager register credentials)
	 * @param rhsmPassword - enter at the prompt for System Engine Password (will be used for subscription-manager register credentials)
	 * @param rhsmOrg - enter at the prompt for Org (will be used for subscription-manager register credentials)
	 * @param rhsmEnv - enter at the prompt for Environment (will be used for subscription-manager register credentials)
	 * @param serviceLevelIndex - index number to enter at the prompt for choosing servicelevel
	 * @return
	 */
	protected SSHCommandResult executeRhnMigrateClassicToRhsm(String options, String rhnUsername, String rhnPassword, String rhsmUsername, String rhsmPassword, String rhsmOrg, String rhsmEnv, Integer serviceLevelIndex) {

		// 8/4/2012 new behavior...
		// the migration tool will always prompt rhn credentials to migrate the system "from"
		// the migration tool will only prompt for destination credentials to migrate the system "to" when the configured hostname does not match subscription.rhn(.*).redhat.com
		// we do this to avoid unnecessary prompting for duplicate credentials.
		// If you try to migrate from a non-hosted rhn (such as satellite) to a hosted rhsm system, you will get this... 
		// Unable to connect to certificate server: Invalid username or password. To create a login, please visit https://www.redhat.com/wapps/ugc/register.html.  See /var/log/rhsm/rhsm.log for more details.
		
		// several command line options were changed by the sat5to6 work by awood and ggainey in bugs 1142436 1123025
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13")) {
			if (options!=null) options=options.replace("--serverurl", "--destination-url");
			if (options!=null) options=options.replace("--redhat-user", "--legacy-user");
			if (options!=null) options=options.replace("--redhat-password", "--legacy-password");
			if (options!=null) options=options.replace("--subscription-service-user", "--destination-user");
			if (options!=null) options=options.replace("--subscription-service-password", "--destination-password");
			if (options!=null) options=options.replace("--servicelevel", "--service-level");
			// completely removed option -g, --gui launch the GUI tool to attach subscriptions, instead of auto-attaching
		}
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.8-1")) {	// post subscription-manager commit e53f0369b621902b75f2dbe047d97dc9ba3cc1c0
			if (options!=null) options=options.replace("--service-level", "--servicelevel");	// 1157761: revert to "--servicelevel"
		}
		
		// surround tcl args containing white space with ticks and call the TCL expect script for rhn-migrate-classic-to-rhsm
		if (options!=null && options.contains(" "))				options			= String.format("'%s'", options);
		if (options!=null && options.isEmpty())					options			= String.format("\"%s\"", options);
		if (rhnUsername!=null && rhnUsername.contains(" "))		rhnUsername		= String.format("\"%s\"", rhnUsername);
		if (rhnUsername!=null && rhnUsername.isEmpty())			rhnUsername		= String.format("\"%s\"", rhnUsername);
		if (rhnPassword!=null && rhnPassword.contains(" "))		rhnPassword		= String.format("\"%s\"", rhnPassword);
		if (rhnPassword!=null && rhnPassword.isEmpty())			rhnPassword		= String.format("\"%s\"", rhnPassword);
		if (rhsmUsername!=null && rhsmUsername.contains(" "))	rhsmUsername	= String.format("\"%s\"", rhsmUsername);
		if (rhsmUsername!=null && rhsmUsername.isEmpty())		rhsmUsername	= String.format("\"%s\"", rhsmUsername);
		if (rhsmPassword!=null && rhsmPassword.contains(" "))	rhsmPassword	= String.format("\"%s\"", rhsmPassword);
		if (rhsmPassword!=null && rhsmPassword.isEmpty())		rhsmPassword	= String.format("\"%s\"", rhsmPassword);
		if (rhsmOrg!=null && rhsmOrg.contains(" ")) 			rhsmOrg			= String.format("\"%s\"", rhsmOrg);
		if (rhsmOrg!=null && rhsmOrg.isEmpty())					rhsmOrg			= String.format("\"%s\"", rhsmOrg);
		if (rhsmEnv!=null && rhsmEnv.contains(" ")) 			rhsmEnv			= String.format("\"%s\"", rhsmEnv);
		if (rhsmEnv!=null && rhsmEnv.isEmpty())					rhsmEnv			= String.format("\"%s\"", rhsmEnv);
		String command = String.format("rhn-migrate-classic-to-rhsm.tcl %s %s %s %s %s %s %s %s", options, rhnUsername, rhnPassword, rhsmUsername, rhsmPassword, rhsmOrg, rhsmEnv, serviceLevelIndex);
		SSHCommandResult result = client.runCommandAndWait(command);
		clienttasks.logRuntimeErrors(result);
		return result;
	}

	

	
	/**
	 * Call rhn-channel --user=$rhnUsername --password=$rhnPassword --add --channel=$rhnChannel to consume RHN channels.
	 * @param rhnUsername
	 * @param rhnPassword
	 * @param rhnChannels
	 */
	protected void addRhnClassicChannels(String rhnUsername, String rhnPassword, List<String> rhnChannels) {
		if (false) {	// THIS APPROACH FAILS WHEN THERE IS AN OFFENDING CHANNEL OR THERE ARE TONS OF CHANNELS; I THINK THERE MIGHT BE AN INPUT LIMIT FOR PYTHON ARGS
			// [root@jsefler-onprem-5server rhn]# rhn-channel --add --user=qa@redhat.com --password=CHANGE-ME --channel=rhel-x86_64-server-sap-5
			String rhnChannelsAsOptions=""; for (String rhnChannel : rhnChannels) rhnChannelsAsOptions+=String.format("--channel=%s ",rhnChannel);
			String command = String.format("rhn-channel --user=%s --password=%s --add %s",rhnUsername,rhnPassword,rhnChannelsAsOptions);
			SSHCommandResult result = client.runCommandAndWait(command);
			Assert.assertEquals(result.getExitCode(), new Integer(0),"Exitcode from attempt to add RHN Classic channel.");
			Assert.assertEquals(result.getStderr(), "","Stderr from attempt to add RHN Classic channel.");
		} else if (false) {	// THIS APPROACH DOES NOT HIDE AN OFFENDING CHANNEL, BUT IT IS SLOWER AND CONSUMES LOTS OF LOG SPACE
			for (String rhnChannel : rhnChannels) {
				String command = String.format("rhn-channel --user=%s --password=%s --add --channel=%s",rhnUsername,rhnPassword,rhnChannel);
				SSHCommandResult result = client.runCommandAndWait(command);
				Assert.assertEquals(result.getExitCode(), new Integer(0),"Exitcode from attempt to add RHN Classic channel.");
				Assert.assertEquals(result.getStderr(), "","Stderr from attempt to add RHN Classic channel.");
			}
		} else if (false) {	// THIS APPROACH WORKS WELL, BUT IT HIDES OFFENDING CHANNELS AND AFTER 3 MINUTES, IT THROWS A rhn-plugin: Error communicating with server. The message was: Connection timed out on readline
			String command="";
			for (String rhnChannel : rhnChannels) {
				//command += String.format(" && rhn-channel --user=%s --password=%s --add --channel=%s",rhnUsername,rhnPassword,rhnChannel);
				command += String.format(" && rhn-channel -u %s -p %s -a -c %s",rhnUsername,rhnPassword,rhnChannel);
			}
			command = command.replaceFirst("^ *&& *", "");
			SSHCommandResult result = client.runCommandAndWait(command);
			Assert.assertEquals(result.getExitCode(), new Integer(0),"Exitcode from attempt to add RHN Classic channels.");
			Assert.assertEquals(result.getStderr(), "","Stderr from attempt to add RHN Classic channel.");
		} else if (false) {	// THIS APPROACH WORKS WELL, BUT IT HIDES OFFENDING CHANNELS AND STILL THROWS AN OCCASIONAL rhn-plugin: Error communicating with server. The message was: Connection timed out on readline
			// add the rhn channels in blocks of 10 to help insure the command is executed under 3 minutes.
			int i = 0;
			String command="";
			for (String rhnChannel : rhnChannels) {
				//command += String.format(" && rhn-channel --user=%s --password=%s --add --channel=%s",rhnUsername,rhnPassword,rhnChannel);
				command += " && "+String.format("rhn-channel -u %s -p %s -a -c %s",rhnUsername,rhnPassword,rhnChannel);
				i++;
				if (i%10==0) {	// add these 10 rhn channels
					SSHCommandResult result = client.runCommandAndWait(command.replaceFirst("^ *&& *", ""));
					if (!result.getExitCode().equals(Integer.valueOf(0))) result = client.runCommandAndWait(command.replaceFirst("^ *&& *", ""));	// sometimes trying again will work
					Assert.assertEquals(result.getExitCode(), Integer.valueOf(0),"Exitcode from attempt to add RHN Classic channels.");
					Assert.assertEquals(result.getStderr(), "","Stderr from attempt to add RHN Classic channel.");
					command = "";
				}
			}
			if (i%10!=0) {	// add the remaining rhn channels
				SSHCommandResult result = client.runCommandAndWait(command.replaceFirst("^ *&& *", ""));
				if (!result.getExitCode().equals(Integer.valueOf(0))) result = client.runCommandAndWait(command.replaceFirst("^ *&& *", ""));	// sometimes trying again will work
				Assert.assertEquals(result.getExitCode(), Integer.valueOf(0),"Exitcode from attempt to add RHN Classic channels.");
				Assert.assertEquals(result.getStderr(), "","Stderr from attempt to add RHN Classic channel.");
			}
		} else {	// THIS APPROACH WORKS WELL, BUT IT HIDES OFFENDING CHANNELS AND AFTER 3 MINUTES, IT THROWS A rhn-plugin: Error communicating with server. The message was: Connection timed out on readline BUT WILL RETRY SEVERAL TIMES UNTIL SUCCESSFUL
			String command="";
			for (String rhnChannel : rhnChannels) {
				//command += String.format(" && rhn-channel --user=%s --password=%s --add --channel=%s",rhnUsername,rhnPassword,rhnChannel);
				command += String.format(" && rhn-channel -u %s -p %s -a -c %s",rhnUsername,rhnPassword,rhnChannel);
			}
			command = command.replaceFirst("^ *&& *", "");
			SSHCommandResult result=null;
			for (int retry=1; retry<=10; retry++) {
				result = client.runCommandAndWait(command);
				if (!result.getStderr().contains("Connection timed out on readline")) break;
				log.info("Encountered a timeout. Re-attempt '"+retry+"' to add RHN Classic channels...");
			}
			Assert.assertEquals(result.getExitCode(), new Integer(0),"Exitcode from attempt to add RHN Classic channels.");
			Assert.assertEquals(result.getStderr(), "","Stderr from attempt to add RHN Classic channel.");
		}
	}

	protected void iptablesRejectPort(String port) {
		//	[root@jsefler-r63-server rhn]# iptables -L OUTPUT
		//	Chain OUTPUT (policy ACCEPT)
		//	target     prot opt source               destination         
		//	[root@jsefler-r63-server rhn]# iptables -A OUTPUT -p tcp --dport 443 -j REJECT
		//	[root@jsefler-r63-server rhn]# iptables -L OUTPUT
		//	Chain OUTPUT (policy ACCEPT)
		//	target     prot opt source               destination         
		//	REJECT     tcp  --  anywhere             anywhere            tcp dpt:https reject-with icmp-port-unreachable 
		RemoteFileTasks.runCommandAndAssert(client, String.format("iptables -A OUTPUT -p tcp --dport %s -j REJECT",port), 0);
	}
	
	protected void iptablesAcceptPort(String port) {
		//	[root@jsefler-r63-server rhn]# iptables -L OUTPUT
		//	Chain OUTPUT (policy ACCEPT)
		//	target     prot opt source               destination         
		//	[root@jsefler-r63-server rhn]# iptables -A OUTPUT -p tcp --dport 443 -j REJECT
		//	[root@jsefler-r63-server rhn]# iptables -A OUTPUT -p tcp --dport 443 -j REJECT
		//	[root@jsefler-r63-server rhn]# iptables -L OUTPUT
		//	Chain OUTPUT (policy ACCEPT)
		//	target     prot opt source               destination         
		//	REJECT     tcp  --  anywhere             anywhere            tcp dpt:https reject-with icmp-port-unreachable 
		//	REJECT     tcp  --  anywhere             anywhere            tcp dpt:https reject-with icmp-port-unreachable 
		//	[root@jsefler-r63-server rhn]# iptables -D OUTPUT -p tcp --dport 443 -j REJECT
		//	[root@jsefler-r63-server rhn]# echo $?
		//	0
		//	[root@jsefler-r63-server rhn]# iptables -D OUTPUT -p tcp --dport 443 -j REJECT
		//	[root@jsefler-r63-server rhn]# echo $?
		//	0
		//	[root@jsefler-r63-server rhn]# iptables -D OUTPUT -p tcp --dport 443 -j REJECT
		//	iptables: No chain/target/match by that name.
		//	[root@jsefler-r63-server rhn]# echo $?
		//	1
		//	[root@jsefler-r63-server rhn]# iptables -L OUTPUT
		//	Chain OUTPUT (policy ACCEPT)
		//	target     prot opt source               destination         
		//	[root@jsefler-r63-server rhn]# 
		do {
			client.runCommandAndWait(String.format("iptables -D OUTPUT -p tcp --dport %s -j REJECT",port));
		} while (client.getExitCode()==0);
	}
	
	protected void checkForKnownBug881952(SSHCommandResult sshCommandResult) {
		
		//	ssh root@jsefler-6server.usersys.redhat.com rhn-migrate-classic-to-rhsm.tcl '--no-auto --force' qa@redhat.com CHANGE-ME testuser1 password admin null
		//	Stdout:
		//	spawn rhn-migrate-classic-to-rhsm --no-auto --force
		//	Red Hat account: qa@redhat.com
		//	Password:
		//	System Engine Username: testuser1
		//	Password:
		//	Org: admin
		//
		//	Retrieving existing RHN Classic subscription information ...
		//
		//			<--- CUT --->
		
		//			<--- CUT --->
		//
		//	Preparing to unregister system from RHN Classic ...
		//	Traceback (most recent call last):
		//	File "/usr/sbin/rhn-migrate-classic-to-rhsm", line 713, in <module>
		//	main()
		//	File "/usr/sbin/rhn-migrate-classic-to-rhsm", line 698, in main
		//	unRegisterSystemFromRhnClassic(sc, sk)
		//	File "/usr/sbin/rhn-migrate-classic-to-rhsm", line 378, in unRegisterSystemFromRhnClassic
		//	result = sc.system.deleteSystems(sk, systemId)
		//	File "/usr/lib64/python2.6/xmlrpclib.py", line 1199, in __call__
		//	return self.__send(self.__name, args)
		//	File "/usr/lib64/python2.6/xmlrpclib.py", line 1489, in __request
		//	verbose=self.__verbose
		//	File "/usr/lib64/python2.6/xmlrpclib.py", line 1237, in request
		//	errcode, errmsg, headers = h.getreply()
		//	File "/usr/lib64/python2.6/httplib.py", line 1064, in getreply
		//	response = self._conn.getresponse()
		//	File "/usr/lib64/python2.6/httplib.py", line 990, in getresponse
		//	response.begin()
		//	File "/usr/lib64/python2.6/httplib.py", line 391, in begin
		//	version, status, reason = self._read_status()
		//	File "/usr/lib64/python2.6/httplib.py", line 349, in _read_status
		//	line = self.fp.readline()
		//	File "/usr/lib64/python2.6/socket.py", line 433, in readline
		//	data = recv(1)
		//	socket.timeout: timed out
		//	Stderr:
		//	ExitCode: 1
		
		// OR 
		
		//	ssh root@jsefler-6server.usersys.redhat.com rhn-migrate-classic-to-rhsm.tcl '--no-auto --force' qa@redhat.com CHANGE-ME testuser1 password admin null
		//	Stdout:
		//	spawn rhn-migrate-classic-to-rhsm --no-auto --force
		//	Red Hat account: qa@redhat.com
		//	Password:
		//	System Engine Username: testuser1
		//	Password:
		//	Org: admin
		//
		//	Retrieving existing RHN Classic subscription information ...
		//
		//			<--- CUT --->
		
		//			<--- CUT --->
		//
		//	Preparing to unregister system from RHN Classic ...
		//	Traceback (most recent call last):
		//	File "/usr/sbin/rhn-migrate-classic-to-rhsm", line 713, in <module>
		//	main()
		//	File "/usr/sbin/rhn-migrate-classic-to-rhsm", line 698, in main
		//	unRegisterSystemFromRhnClassic(sc, sk)
		//	File "/usr/sbin/rhn-migrate-classic-to-rhsm", line 378, in unRegisterSystemFromRhnClassic
		//	result = sc.system.deleteSystems(sk, systemId)
		//	File "/usr/lib64/python2.6/xmlrpclib.py", line 1199, in __call__
		//	return self.__send(self.__name, args)
		//	File "/usr/lib64/python2.6/xmlrpclib.py", line 1489, in __request
		//	verbose=self.__verbose
		//	File "/usr/lib64/python2.6/xmlrpclib.py", line 1237, in request
		//	errcode, errmsg, headers = h.getreply()
		//	File "/usr/lib64/python2.6/httplib.py", line 1064, in getreply
		//	response = self._conn.getresponse()
		//	File "/usr/lib64/python2.6/httplib.py", line 990, in getresponse
		//	response.begin()
		//	File "/usr/lib64/python2.6/httplib.py", line 391, in begin
		//	version, status, reason = self._read_status()
		//	File "/usr/lib64/python2.6/httplib.py", line 349, in _read_status
		//	line = self.fp.readline()
		//	File "/usr/lib64/python2.6/socket.py", line 433, in readline
		//	data = recv(1)
		//	File "/usr/lib64/python2.6/ssl.py", line 215, in recv
		//	return self.read(buflen)
		//	File "/usr/lib64/python2.6/ssl.py", line 136, in read
		//	return self._sslobj.read(len)
		//	ssl.SSLError: The read operation timed out
		//	Stderr:
		//	ExitCode: 1
		
		// TEMPORARY WORKAROUND FOR BUG
		// detect bug 881952 and skip if not fixed in this release
		// Bug 881952 	ssl.SSLError: The read operation timed out (during large rhn-migrate-classic-to-rhsm) 
		String bugId = "881952";
		if (sshCommandResult.getExitCode()==1) {
			if (sshCommandResult.getStdout().trim().endsWith("timed out")) {
				if (Integer.valueOf(clienttasks.redhatReleaseX)==6 && Float.valueOf(clienttasks.redhatReleaseXY)<6.4) {
					throw new SkipException("Caught an SSLError error that is fixed by bug "+bugId+" in a newer rhel release of subscription-manager-migration.");
				}
				if (Integer.valueOf(clienttasks.redhatReleaseX)==5 && Float.valueOf(clienttasks.redhatReleaseXY)<5.10) {
					throw new SkipException("Caught an SSLError error that is fixed by bug "+bugId+" in a newer rhel release of subscription-manager-migration.");
				}
			}
		}
		
		// fix...
		
		//	Preparing to unregister system from RHN Classic ...
		//	Did not receive a completed unregistration message from RHN Classic for system 1023878699.
		//	Please investigate on the Customer Portal at https://access.redhat.com.
		//	
		//	Attempting to register system to Red Hat Subscription Management ...
		//	The system has been registered with id: 9909bb77-d56e-4784-a16f-d4509fd252ce 
		//	System 'dhcp193-87.pnq.redhat.com' successfully registered to Red Hat Subscription Management.
		
		// END OF WORKAROUND
	}
	
	
	protected void checkForKnownBug1130637(SSHCommandResult sshCommandResult) {
		
		
		//	201408151456:16.723 - FINE: ssh root@ibm-x3550m3-07.lab.eng.brq.redhat.com rhn-migrate-classic-to-rhsm.tcl -n qa@redhat.com REDACTED null null null null null
		//	201408151456:25.495 - FINE: Stdout: 
		//	spawn rhn-migrate-classic-to-rhsm -n
		//	Red Hat username: qa@redhat.com
		//	Red Hat password: 
		//
		//	Retrieving existing RHN Classic subscription information...
		//
		//	+-----------------------------------------------------+
		//	System is currently subscribed to these RHN Classic Channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-client-6
		//
		//	+-----------------------------------------------------+
		//	Installing product certificates for these RHN Classic channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-client-6
		//
		//	Product certificates installed successfully to /etc/pki/product.
		//	isfile() takes exactly 1 argument (2 given)
		//	201408151456:25.496 - FINE: Stderr: 
		//	201408151456:25.496 - FINE: ExitCode: 255
		
		// TEMPORARY WORKAROUND FOR BUG
		// detect bug 1130637 and skip if not fixed in this release
		// Bug 1130637 - rhn-migrate-classic-to-rhsm fails on RHEL6 Client with "TypeError: isfile() takes exactly 1 argument (2 given)"
		String tracebackMsg = "isfile() takes exactly 1 argument (2 given)";
		if (sshCommandResult.getStdout().trim().endsWith(tracebackMsg)) {
			String bugId = "1130637";
			boolean invokeWorkaroundWhileBugIsOpen = true;
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("Encountered unexpected traceback message '"+tracebackMsg+"' from open bug "+bugId+".");
			}
		}
		// END OF WORKAROUND
	}

	// Data Providers ***********************************************************************

	@DataProvider(name="InstallNumMigrateToRhsmData")
	public Object[][] getInstallNumMigrateToRhsmDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getInstallNumMigrateToRhsmDataAsListOfLists());
	}
	public List<List<Object>> getInstallNumMigrateToRhsmDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		// if (!clienttasks.redhatReleaseX.equals("5")) return ll;	// prone to improperly unexecuted tests
		if (clienttasks.redhatReleaseX.equals("6")) return ll;
		if (clienttasks.redhatReleaseX.equals("7")) return ll;
		
		// REFRENCE DATA FROM: http://linuxczar.net/articles/rhel-installation-numbers
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1092754","790217"}),	"0000000e0017fc01"}));	// Client
		ll.add(Arrays.asList(new Object[]{null,													"000000990007fc02"}));	// Red Hat Global Desktop
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1092754"}),			"000000e90007fc00"}));	// Server
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1092754"}),			"00000065000bfc00"}));	// Server with Cluster
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1092754"}),			"000000ab000ffc00"}));	// Server with ClusterStorage
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1092754"}),			"000000e30013fc00"}));	// Server with HPC
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1092754"}),			"000000890017fc00"}));	// Server with Directory
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1092754"}),			"00000052001bfc00"}));	// Server with SMB

		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1092754","790217"}),	"000000a4004ffc01"}));	// Product: RHEL Client   Options: Basic FullProd Workstation  {'Workstation': 'Workstation', 'Base': 'Client'}
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1092754"}),			"000000870003fc01"}));	// Product: RHEL Client   Options: NoSLA FullProd  {'Base': 'Client'}
		return ll;
	}
	
	
	@DataProvider(name="InstallNumMigrateToRhsmWithInvalidInstNumberData")
	public Object[][] getInstallNumMigrateToRhsmWithInvalidInstNumberDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getInstallNumMigrateToRhsmWithInvalidInstNumberDataAsListOfLists());
	}
	protected List<List<Object>> getInstallNumMigrateToRhsmWithInvalidInstNumberDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		// if (!clienttasks.redhatReleaseX.equals("5")) return ll;	// prone to improperly unexecuted tests
		if (clienttasks.redhatReleaseX.equals("6")) return ll;
		if (clienttasks.redhatReleaseX.equals("7")) return ll;
		
		// due to design changes, this is a decent place to dump old commands that have been removed
		
		// String command, int expectedExitCode, String expectedStdout, String expectedStderr
		ll.add(Arrays.asList(new Object[]{null, installNumTool+" -d -i 123456789012345",			1,	"Could not parse the installation number: Unsupported string length", ""}));
		ll.add(Arrays.asList(new Object[]{null, installNumTool+" -d -i=12345678901234567",			1,	"Could not parse the installation number: Unsupported string length", ""}));
		ll.add(Arrays.asList(new Object[]{null, installNumTool+"    --instnum 123456789X123456",	1,	"Could not parse the installation number: Not a valid hex string", ""}));
		ll.add(Arrays.asList(new Object[]{null, installNumTool+"    --instnum=1234567890123456",	1,	"Could not parse the installation number: Checksum verification failed", ""}));
		
		return ll;
	}
	
	
	@DataProvider(name="RhnMigrateClassicToRhsmData")
	public Object[][] getRhnMigrateClassicToRhsmDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getRhnMigrateClassicToRhsmDataAsListOfLists());
	}
	public List<List<Object>> getRhnMigrateClassicToRhsmDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (clienttasks==null) return ll;
		if (sm_rhnUsername.equals("")) {log.warning("RHN Username was not provided."); return ll;}
		if (sm_rhnPassword.equals("")) {log.warning("RHN Password was not provided."); return ll;}
		
		int rhnChildChannelSubSize = 40;	// 50;	// used to break down rhnAvailableChildChannels into smaller sub-lists to avoid bugs 818786 881952
///*debugTesting*/ rhnChildChannelSubSize = 0;		
		List<String> rhnAvailableNonBetaChildChannels = new ArrayList<String>();
		for (String rhnChannel: rhnAvailableChildChannels) if (!rhnChannel.contains("-beta")) rhnAvailableNonBetaChildChannels.add(rhnChannel);

		// when we are migrating away from RHN Classic to a non-hosted candlepin server, determine good credentials for rhsm registration
		String rhsmUsername=sm_clientUsername, rhsmPassword=sm_clientPassword, rhsmOrg=sm_clientOrg;	// default
		if (clienttasks.register_(sm_rhnUsername, sm_rhnPassword, null, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null).getExitCode().equals(new Integer(0))) { // try sm_rhnUsername sm_rhnPassword...
			rhsmUsername=sm_rhnUsername; rhsmPassword=sm_rhnPassword; rhsmOrg = null;
		}
		
		// predict the valid service levels that will be available to the migrated consumer
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(rhsmUsername, rhsmPassword, rhsmOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null));
		String orgKey = CandlepinTasks.getOwnerKeyOfConsumerId(rhsmUsername, rhsmPassword, sm_serverUrl, consumerId);
		List<String> rhsmServiceLevels = CandlepinTasks.getServiceLevelsForOrgKey(rhsmUsername, rhsmPassword, sm_serverUrl, orgKey);	
		clienttasks.unregister(null, null, null, null);
		
		// predict the expected service level from the defaultServiceLevel on the Org
		JSONObject jsonOrg = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(rhsmUsername, rhsmPassword, sm_serverUrl, "/owners/"+orgKey));
		String defaultServiceLevel = (jsonOrg.get("defaultServiceLevel").equals(JSONObject.NULL))? "":jsonOrg.getString("defaultServiceLevel");
		
		// Note: To avoid redundant prompting for credentials, rhn-migrate-classic-to-rhsm will NOT prompt for rhsm Username/Password/Org when the rhsm server matches subscription\.rhn\.(.+\.)*redhat\.com
		// This causes a testing problem when migrating from a satellite server to rhsm hosted - adding a valid --serverurl to the options is a good workaround
		String rhsmServerUrlOption="";
		if (!doesStringContainMatches(sm_rhnHostname, "(rhn|rhsm)\\.(.+\\.)*redhat\\.com")) {	// indicates that we are migrating from a non-hosted rhn server - as opposed to rhn.code.stage.redhat.com (stage) or rhn.redhat.com (production)
			if (doesStringContainMatches(sm_serverHostname, "subscription\\.(rhn|rhsm)\\.(.+\\.)*redhat\\.com")) {
				// force a valid --serverurl
				rhsmServerUrlOption = " --serverurl="+"https://"+originalServerHostname+":"+originalServerPort+originalServerPrefix;
			}
		} else {// we are migrating from rhn hosted...
			if (doesStringContainMatches(sm_serverHostname, "subscription\\.(rhn|rhsm)\\.(.+\\.)*redhat\\.com")) {
				// and we are migrating to rhsm hosted...
				// hence we will not be prompted for rhsm credentials,
				// so don't pass them to the rhn-migrate-classic-to-rhsm.tcl script or else you won't get the expected rhn-migrate-classic-to-rhsm exit code because the tcl script will be prematurely exited without getting the actual exit code from rhn-migrate-classic-to-rhsm.
				if (sm_rhnUsername.equals(rhsmUsername)) {
					rhsmUsername = null;
					rhsmPassword = null;
					rhsmOrg = null;
				}
			}
		}
		
		// create some variations on a valid serverUrl to test the --serverurl option
		List<String> rhsmServerUrls = new ArrayList<String>();
		if (rhsmServerUrlOption.isEmpty()) {	// skip testing variations on a valid serverUrl when a valid rhsmServerUrlOption is already being included in all data provided rows
			if (isHostnameHosted(originalServerHostname)) {
				rhsmServerUrls.add(originalServerHostname);
				rhsmServerUrls.add("https://"+originalServerHostname);
				rhsmServerUrls.add("https://"+originalServerHostname+originalServerPrefix);
				rhsmServerUrls.add("https://"+originalServerHostname+":"+originalServerPort);
				rhsmServerUrls.add("https://"+originalServerHostname+":"+originalServerPort+originalServerPrefix);
			} else {	// Note: only a fully qualified server url will work for a non-hosted hostname because otherwise the (missing port/prefix defaults to 443/subscription) results will end up with: Unable to connect to certificate server: (111, 'Connection refused'). See /var/log/rhsm/rhsm.log for more details.
				rhsmServerUrls.add(originalServerHostname+":"+originalServerPort+originalServerPrefix);
				rhsmServerUrls.add("https://"+originalServerHostname+":"+originalServerPort+originalServerPrefix);
			}
		}
		
		// Object bugzilla, String rhnUsername, String rhnPassword, String rhnServer, List<String> rhnChannelsToAdd, String options, String rhsmUsername, String rhsmPassword, String rhsmOrg, Integer serviceLevelIndex, String serviceLevelExpected
		
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("849644"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	new ArrayList<String>(),		"-n"+rhsmServerUrlOption,		sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	null,	defaultServiceLevel}));
		//ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("849644"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	rhnAvailableChildChannels,		"-n"+rhsmServerUrlOption,		sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	null,	defaultServiceLevel}));
		/* 6/5/2014 although valid, all these rows take too long, replacing with a single row and a random subset of rhnAvailableChildChannels
		for (int i=0; i<rhnAvailableChildChannels.size(); i+=rhnChildChannelSubSize) {	// split rhnAvailableChildChannels into sub-lists of 50 channels to avoid bug 818786 - 502 Proxy Error traceback during large rhn-migrate-classic-to-rhsm
			List<String> rhnSubsetOfAvailableChildChannels = rhnAvailableChildChannels.subList(i,i+rhnChildChannelSubSize>rhnAvailableChildChannels.size()?rhnAvailableChildChannels.size():i+rhnChildChannelSubSize);
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"849644","980209"}),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	rhnSubsetOfAvailableChildChannels,	"-n -f"+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	null,	defaultServiceLevel}));		
			List<String> rhnNonBetaSubsetOfAvailableChildChannels = new ArrayList<String>(); for (String rhnChannel: rhnSubsetOfAvailableChildChannels) if (!rhnChannel.contains("-beta")) rhnNonBetaSubsetOfAvailableChildChannels.add(rhnChannel);
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"849644","980209"}),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	rhnNonBetaSubsetOfAvailableChildChannels,	"-n -f"+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	null,	defaultServiceLevel}));		
		}
		*/
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"849644","980209"}),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	getRandomSubsetOfList(rhnAvailableChildChannels,rhnChildChannelSubSize),	"-n -f"+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	null,	defaultServiceLevel}));		
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"849644","980209"}),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	getRandomSubsetOfList(rhnAvailableNonBetaChildChannels,rhnChildChannelSubSize),	"-n -f"+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	null,	defaultServiceLevel}));		
		
		
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("977321"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	new ArrayList<String>(),		""+rhsmServerUrlOption,			sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	null,	defaultServiceLevel}));
		//ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("977321"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	rhnAvailableChildChannels,		""+rhsmServerUrlOption,			sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	/*areAllChannelsMapped(rhnAvailableChildChannels)?noServiceLevelIndex:*/null,	defaultServiceLevel}));
		/* 6/5/2014 although valid, all these rows take too long, replacing with a single row and a random subset of rhnAvailableChildChannels
		for (int i=0; i<rhnAvailableChildChannels.size(); i+=rhnChildChannelSubSize) {	// split rhnAvailableChildChannels into sub-lists of 50 channels to avoid bug 818786 - 502 Proxy Error traceback during large rhn-migrate-classic-to-rhsm
			List<String> rhnSubsetOfAvailableChildChannels = rhnAvailableChildChannels.subList(i,i+rhnChildChannelSubSize>rhnAvailableChildChannels.size()?rhnAvailableChildChannels.size():i+rhnChildChannelSubSize);
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("977321"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	rhnSubsetOfAvailableChildChannels,	"-f"+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	null,	defaultServiceLevel}));		
			List<String> rhnNonBetaSubsetOfAvailableChildChannels = new ArrayList<String>(); for (String rhnChannel: rhnSubsetOfAvailableChildChannels) if (!rhnChannel.contains("-beta")) rhnNonBetaSubsetOfAvailableChildChannels.add(rhnChannel);
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("977321"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	rhnNonBetaSubsetOfAvailableChildChannels,	"-f"+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	null,	defaultServiceLevel}));		
		}
		*/
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("977321"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	getRandomSubsetOfList(rhnAvailableChildChannels,rhnChildChannelSubSize),	"-f"+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	null,	defaultServiceLevel}));		
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("977321"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	getRandomSubsetOfList(rhnAvailableNonBetaChildChannels,rhnChildChannelSubSize),	"-f"+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	null,	defaultServiceLevel}));		
		
		
		// test variations of a valid serverUrl
		for (String serverUrl : rhsmServerUrls) {
			List<String> availableChildChannelList = rhnAvailableChildChannels.isEmpty()? rhnAvailableChildChannels : Arrays.asList(rhnAvailableChildChannels.get(randomGenerator.nextInt(rhnAvailableChildChannels.size())));	// randomly choose an available child channel just to add a little fun
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"977321","1220493"}),		sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	availableChildChannelList,	"-f --serverurl="+serverUrl,		sm_rhnUsername,	sm_rhnPassword,	sm_clientUsername,	sm_clientPassword,	sm_clientOrg,	null,	defaultServiceLevel}));		
		}
		
		// test each servicelevel
		// for (String serviceLevel : rhsmServiceLevels) {	// takes too long
		for (String serviceLevel : getRandomSubsetOfList(rhsmServiceLevels,2)) {
			String options;
			options = String.format("--force --servicelevel=%s",serviceLevel); if (serviceLevel.contains(" ")) options = String.format("--force --servicelevel \"%s\"", serviceLevel);
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"840169","977321","1149007"}),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	getRandomSubsetOfList(rhnAvailableChildChannels,rhnChildChannelSubSize),	options+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	null,	serviceLevel}));	
			options = String.format("-f -s %s",randomizeCaseOfCharactersInString(serviceLevel)); if (serviceLevel.contains(" ")) options = String.format("-f -s \"%s\"", randomizeCaseOfCharactersInString(serviceLevel));
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"840169","841961","977321","1149007"}),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	getRandomSubsetOfList(rhnAvailableChildChannels,rhnChildChannelSubSize),	options+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	null,	serviceLevel}));
		}
		
		// attempt an unavailable servicelevel, then choose an available one from the index table
		if (!rhsmServiceLevels.isEmpty()) {
			int serviceLevelIndex = randomGenerator.nextInt(rhsmServiceLevels.size());
			String serviceLevel = rhsmServiceLevels.get(serviceLevelIndex);
			serviceLevelIndex++;	// since the interactive menu of available service-levels to choose from is indexed starting at 1.
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"840169","977321","1149007"}),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	new ArrayList<String>(),	"--force --servicelevel=UNAVAILABLE-SLA"+rhsmServerUrlOption,				sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	serviceLevelIndex,	serviceLevel}));	
		}
		
		// attempt an unavailable servicelevel, then choose no service level
		if (!rhsmServiceLevels.isEmpty()) {
			int noServiceLevelIndex = rhsmServiceLevels.size()+1;	// since the last item in the interactive menu of available service-levels is "#. No service level preference"
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"840169","977321","1149007"}),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	new ArrayList<String>(),	"--force --servicelevel=UNAVAILABLE-SLA"+rhsmServerUrlOption,				sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	noServiceLevelIndex,	""}));	
		}
		
		// test --org as a command line option
		if (rhsmOrg!=null) {
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"849644","877331"}),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	new ArrayList<String>(),		"--no-auto --org="+rhsmOrg+rhsmServerUrlOption,			sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	null,	null,	defaultServiceLevel}));
		}
		
		// test --redhat-user --redhat-password --subscription-service-user --subscription-service-password as a command line option
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"912375","1087603"}),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	new ArrayList<String>(),		String.format("--redhat-user=%s --redhat-password=%s",sm_rhnUsername,sm_rhnPassword)+rhsmServerUrlOption,			null,	null,	rhsmUsername,	rhsmPassword,	rhsmOrg,	null,	defaultServiceLevel}));
		
		// ...END OF ADDED ROWS
		
		// ADDING BlockedByBzBug TO SELECTED ROWS...
		
		// when rhsmOrg is not null, add bug BlockedByBzBug 849483 to all rows
		if (rhsmOrg!=null) for (List<Object> l : ll) {
			BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);	// get the existing BlockedByBzBug
			List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
			bugIds.add("849483");	// 849483 - rhn-migrate-classic-to-rhsm fails to prompt for needed System Engine org credentials 
			blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			l.set(0, blockedByBzBug);
		}
		
		// when testing with child channels, add bug BlockedByBzBug 1075167 to affected rows
		for (List<Object> l : ll) {
			if (!((List<String>)(l.get(4))).isEmpty()) {	// affected rows have a positive List<String> rhnChannelsToAdd
				BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);	// get the existing BlockedByBzBug
				List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
				bugIds.add("1075167");	// Bug 1075167 - rhn-migrate-classic-to-rhsm throws Traceback KeyError: "Unknown feature: 'IDENTITY'"
				blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
				l.set(0, blockedByBzBug);
			}
		}
		
		// when testing with --serverurl specified, add bug BlockedByBzBug 1131213 to affected rows
		for (List<Object> l : ll) {
			if (((String)(l.get(5))).contains("--serverurl")) {	// affected rows contain --serverurl in the String options parameter
				BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);	// get the existing BlockedByBzBug
				List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
				bugIds.add("1131213");	// Bug 1131213 - rhn-migrate-classic-to-rhsm throws gaierror: [Errno -2] Name or service not known
				bugIds.add("1171808");	// Bug 1171808 - rhn-migrate-classic-to-rhsm throws gaierror: [Errno -2] Name or service not known
				blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
				l.set(0, blockedByBzBug);
			}
		}
		
		// when testing with --serverurl/--destination-url specified, add bug BlockedByBzBug 1157761 to affected rows
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) {
			for (List<Object> l : ll) {
				if (((String)(l.get(5))).contains("--serverurl")) {	// affected rows contain --serverurl in the String options parameter
					BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);	// get the existing BlockedByBzBug
					List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
					bugIds.add("1157761");	// Bug 1157761 - broken --destination-url option in rhn-migrate-rhn-classic-to-rhsm
					blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
					l.set(0, blockedByBzBug);
				}
			}
		}
		
		// many options were changed by the RFE Bug 1123025 sat5to6 development, convert all of the options to their new values
		/* this update is also retrofitted in executeRhnMigrateClassicToRhsm */
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) {
			for (List<Object> l : ll) {
				String options = (String)(l.get(5));
				if (options!=null) {
					options=options.replace("--serverurl", "--destination-url");
					options=options.replace("--redhat-user", "--legacy-user");
					options=options.replace("--redhat-password", "--legacy-password");
					options=options.replace("--subscription-service-user", "--destination-user");
					options=options.replace("--subscription-service-password", "--destination-password");
					options=options.replace("--servicelevel", "--service-level");
				}
				if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.8-1")) {	// post subscription-manager commit e53f0369b621902b75f2dbe047d97dc9ba3cc1c0
					if (options!=null) options=options.replace("--service-level", "--servicelevel");	// 1157761: revert to "--servicelevel"
				}
				l.set(5, options);
			}
		}
		
		return ll;
	}
	
	
	@DataProvider(name="RhnMigrateClassicToRhsmUsingProxyServerData")
	public Object[][] getRhnMigrateClassicToRhsmUsingProxyServerDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRhnMigrateClassicToRhsmUsingProxyServerDataAsListOfLists());
	}
	protected List<List<Object>> getRhnMigrateClassicToRhsmUsingProxyServerDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (clienttasks==null) return ll;
		if (sm_rhnUsername.equals("")) {log.warning("RHN Username was not provided."); return ll;}
		if (sm_rhnPassword.equals("")) {log.warning("RHN Password was not provided."); return ll;}
		
		int rhnChildChannelSubSize = 40;	// 50;	// used to break down rhnAvailableChildChannels into smaller sub-lists to avoid bugs 818786 881952
		
		String basicauthproxyUrl = String.format("%s:%s", sm_basicauthproxyHostname,sm_basicauthproxyPort); basicauthproxyUrl = basicauthproxyUrl.replaceAll(":$", "");
		String noauthproxyUrl = String.format("%s:%s", sm_noauthproxyHostname,sm_noauthproxyPort); noauthproxyUrl = noauthproxyUrl.replaceAll(":$", "");
		
		// when we are migrating away from RHN Classic to a non-hosted candlepin server, determine good credentials for rhsm registration
		String rhsmUsername=sm_clientUsername, rhsmPassword=sm_clientPassword, rhsmOrg=sm_clientOrg;	// default
		if (clienttasks.register_(sm_rhnUsername, sm_rhnPassword, null, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null).getExitCode().equals(new Integer(0))) { // try sm_rhnUsername sm_rhnPassword...
			rhsmUsername=sm_rhnUsername; rhsmPassword=sm_rhnPassword; rhsmOrg = null;
		}
		
		// Note: To avoid redundant prompting for credentials, rhn-migrate-classic-to-rhsm will NOT prompt for rhsm Username/Password/Org when the rhsm server matches subscription\.rhn\.(.+\.)*redhat\.com
		// This causes a testing problem when migrating from a satellite server to rhsm hosted - adding a valid --serverurl to the options is a good workaround
		String rhsmServerUrlOption="";
//		if (sm_rhnHostname.startsWith("http") { 	// indicates that we are migrating from a non-hosted rhn server - as opposed to rhn.code.stage.redhat.com (stage) or rhn.redhat.com (production)
//		if (!doesStringContainMatches(sm_rhnHostname, "rhn\\.(.+\\.)*redhat\\.com")) {	// indicates that we are migrating from a non-hosted rhn server - as opposed to rhn.code.stage.redhat.com (stage) or rhn.redhat.com (production)
		if (!doesStringContainMatches(sm_rhnHostname, "(rhn|rhsm)\\.(.+\\.)*redhat\\.com")) {	// indicates that we are migrating from a non-hosted rhn server - as opposed to rhn.code.stage.redhat.com (stage) or rhsm.stage.redhat.com (stage) or rhn.redhat.com (production) or rhsm.redhat.com (production after RFE 1126501)
//			if (doesStringContainMatches(sm_serverHostname, "subscription\\.rhn\\.(.+\\.)*redhat\\.com")) {
			if (doesStringContainMatches(sm_serverHostname, "subscription\\.(rhn|rhsm)\\.(.+\\.)*redhat\\.com")) {
				// force a valid --serverurl
				rhsmServerUrlOption = " --serverurl="+"https://"+originalServerHostname+":"+originalServerPort+originalServerPrefix;
			}
		} else {// we are migrating from rhn hosted...
//			if (doesStringContainMatches(sm_serverHostname, "subscription\\.rhn\\.(.+\\.)*redhat\\.com")) {
			if (doesStringContainMatches(sm_serverHostname, "subscription\\.(rhn|rhsm)\\.(.+\\.)*redhat\\.com")) {
				// and we are migrating to rhsm hosted...
				// hence we will not be prompted for rhsm credentials,
				// so don't pass them to the rhn-migrate-classic-to-rhsm.tcl script or else you won't get the expected rhn-migrate-classic-to-rhsm exit code because the tcl script will be prematurely exited without getting the actual exit code from rhn-migrate-classic-to-rhsm.
				if (sm_rhnUsername.equals(rhsmUsername)) {
					rhsmUsername = null;
					rhsmPassword = null;
					rhsmOrg = null;
				}
			}
		}
		
		// Object bugzilla, String rhnUsername, String rhnPassword, String rhnServer, List<String> rhnChannelsToAdd, String options, String rhsmUsername, String rhsmPassword, String rhsmOrg, String proxy_hostnameConfig, String proxy_portConfig, String proxy_userConfig, String proxy_passwordConfig, Integer exitCode, String stdout, String stderr, SSHCommandRunner proxyRunner, String proxyLog, String proxyLogRegex

		// basic auth proxy test data...
		ll.add(Arrays.asList(new Object[]{null,							sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	new ArrayList<String>(),		"--no-auto"+rhsmServerUrlOption,				sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicAuthProxyRunner,	sm_basicauthproxyLog,	"TCP_MISS"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("915847"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	new ArrayList<String>(),		"--no-auto --no-proxy"+rhsmServerUrlOption,		sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicAuthProxyRunner,	sm_basicauthproxyLog,	"TCP_MISS"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("798015"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	new ArrayList<String>(),		"--no-auto"+rhsmServerUrlOption,				sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	"http://"+sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicAuthProxyRunner,	sm_basicauthproxyLog,	"TCP_MISS"}));
		//ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("818786"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	rhnAvailableChildChannels,		"--no-auto --force"+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicAuthProxyRunner,	sm_basicauthproxyLog,	"TCP_MISS"}));
		/* 6/5/2014 although valid, all these rows take too long, replacing with a single row and a random subset of rhnAvailableChildChannels
		for (int i=0; i<rhnAvailableChildChannels.size(); i+=rhnChildChannelSubSize) {	// split rhnAvailableChildChannels into sub-lists of 50 channels to avoid bug 818786 - 502 Proxy Error traceback during large rhn-migrate-classic-to-rhsm
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("980209"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	rhnAvailableChildChannels.subList(i, Math.min(i+rhnChildChannelSubSize,rhnAvailableChildChannels.size())),	"--no-auto --force"+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicAuthProxyRunner,	sm_basicauthproxyLog,	"TCP_MISS"}));
		}
		*/
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("980209"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	getRandomSubsetOfList(rhnAvailableChildChannels,rhnChildChannelSubSize),	"--no-auto --force"+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	sm_basicauthproxyHostname,	sm_basicauthproxyPort,		sm_basicauthproxyUsername,	sm_basicauthproxyPassword,	Integer.valueOf(0),		null,		null,		basicAuthProxyRunner,	sm_basicauthproxyLog,	"TCP_MISS"}));

		
		// no auth proxy test data...
		ll.add(Arrays.asList(new Object[]{null,							sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	new ArrayList<String>(),		"--no-auto"+rhsmServerUrlOption,				sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	sm_noauthproxyHostname,	sm_noauthproxyPort,		"",							"",						Integer.valueOf(0),		null,		null,		noAuthProxyRunner,	sm_noauthproxyLog,		"Connect"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("915847"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	new ArrayList<String>(),		"--no-auto --no-proxy"+rhsmServerUrlOption,		sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	sm_noauthproxyHostname,	sm_noauthproxyPort,		"",							"",						Integer.valueOf(0),		null,		null,		noAuthProxyRunner,	sm_noauthproxyLog,		"Connect"}));
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("798015"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	new ArrayList<String>(),		"--no-auto"+rhsmServerUrlOption,				sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	"http://"+sm_noauthproxyHostname,	sm_noauthproxyPort,		"",							"",						Integer.valueOf(0),		null,		null,		noAuthProxyRunner,	sm_noauthproxyLog,		"Connect"}));
		//ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("818786"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	rhnAvailableChildChannels,		"--no-auto --force"+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	sm_noauthproxyHostname,	sm_noauthproxyPort,		"",							"",						Integer.valueOf(0),		null,		null,		noAuthProxyRunner,	sm_noauthproxyLog,		"Connect"}));
		/* 6/5/2014 although valid, all these rows take too long, replacing with a single row and a random subset of rhnAvailableChildChannels
		for (int i=0; i<rhnAvailableChildChannels.size(); i+=rhnChildChannelSubSize) {	// split rhnAvailableChildChannels into sub-lists of 50 channels to avoid bug 818786 - 502 Proxy Error traceback during large rhn-migrate-classic-to-rhsm
			ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("980209"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	rhnAvailableChildChannels.subList(i, Math.min(i+rhnChildChannelSubSize,rhnAvailableChildChannels.size())),	"--no-auto --force"+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	sm_noauthproxyHostname,	sm_noauthproxyPort,		"",							"",						Integer.valueOf(0),		null,		null,		noAuthProxyRunner,	sm_noauthproxyLog,		"Connect"}));
		}
		*/
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug("980209"),	sm_rhnUsername,	sm_rhnPassword,	sm_rhnHostname,	getRandomSubsetOfList(rhnAvailableChildChannels,rhnChildChannelSubSize),	"--no-auto --force"+rhsmServerUrlOption,	sm_rhnUsername,	sm_rhnPassword,	rhsmUsername,	rhsmPassword,	rhsmOrg,	sm_noauthproxyHostname,	sm_noauthproxyPort,		"",							"",						Integer.valueOf(0),		null,		null,		noAuthProxyRunner,	sm_noauthproxyLog,		"Connect"}));

		
		// when testing with child channels, add bug BlockedByBzBug 1075167 to affected rows
		for (List<Object> l : ll) {
			if (!((List<String>)(l.get(4))).isEmpty()) {	// affected rows have a positive List<String> rhnChannelsToAdd
				BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);	// get the existing BlockedByBzBug
				List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
				bugIds.add("1075167");	// Bug 1075167 - rhn-migrate-classic-to-rhsm throws Traceback KeyError: "Unknown feature: 'IDENTITY'"
				blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
				l.set(0, blockedByBzBug);
			}
		}
		
		// when testing with no child channels on Rhel7 ComputeNode, add bug BlockedByBzBug 1078527 to affected rows
		//if (clienttasks.redhatReleaseX.equals("7") && clienttasks.variant.equals("ComputeNode")) {
		if ("rhel-x86_64-hpc-node-7".equals(rhnBaseChannel)) {
			for (List<Object> l : ll) {
				if (((List<String>)(l.get(4))).isEmpty()) {	// affected rows have an empty List<String> rhnChannelsToAdd
					BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);	// get the existing BlockedByBzBug
					List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
					bugIds.add("1078527");	// Bug 1078527 - channel-cert-mapping for ComputeNode rhel-7 product certs are missing and wrong
					blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
					l.set(0, blockedByBzBug);
				}
			}
		}
		
		// when testing with --serverurl specified, add bug BlockedByBzBug 1131213 to affected rows
		for (List<Object> l : ll) {
			if (((String)(l.get(5))).contains("--serverurl")) {	// affected rows contain --serverurl in the String options parameter
				BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);	// get the existing BlockedByBzBug
				List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
				bugIds.add("1131213");	// Bug 1131213 - rhn-migrate-classic-to-rhsm throws gaierror: [Errno -2] Name or service not known
				bugIds.add("1171808");	// Bug 1171808 - rhn-migrate-classic-to-rhsm throws gaierror: [Errno -2] Name or service not known
				blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
				l.set(0, blockedByBzBug);
			}
		}
		
		// when testing with --serverurl/--destination-url specified, add bug BlockedByBzBug 1157761 to affected rows
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) {
			for (List<Object> l : ll) {
				if (((String)(l.get(5))).contains("--serverurl")) {	// affected rows contain --serverurl in the String options parameter
					BlockedByBzBug blockedByBzBug = (BlockedByBzBug) l.get(0);	// get the existing BlockedByBzBug
					List<String> bugIds = blockedByBzBug==null?new ArrayList<String>():new ArrayList<String>(Arrays.asList(blockedByBzBug.getBugIds()));
					bugIds.add("1157761");	// Bug 1157761 - broken --destination-url option in rhn-migrate-rhn-classic-to-rhsm
					blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
					l.set(0, blockedByBzBug);
				}
			}
		}
		
		// many options were changed by the RFE Bug 1123025 sat5to6 development, convert all of the options to their new values
		/* this update is also retrofitted in executeRhnMigrateClassicToRhsm */
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.1")) {
			for (List<Object> l : ll) {
				String options = (String)(l.get(5));
				if (options!=null) {
					options=options.replace("--serverurl", "--destination-url");
					options=options.replace("--redhat-user", "--legacy-user");
					options=options.replace("--redhat-password", "--legacy-password");
					options=options.replace("--subscription-service-user", "--destination-user");
					options=options.replace("--subscription-service-password", "--destination-password");
					options=options.replace("--servicelevel", "--service-level");
					if (clienttasks.isPackageVersion("subscription-manager-migration", ">=", "1.13.8-1")) {	// post subscription-manager commit e53f0369b621902b75f2dbe047d97dc9ba3cc1c0
						if (options!=null) options=options.replace("--service-level", "--servicelevel");	// 1157761: revert to "--servicelevel"
					}
				}
				l.set(5, options);
			}
		}
		
		return ll;
	}
	
	
	
	@DataProvider(name="RhnMigrateClassicToRhsmWithNonDefaultProductCertDirData")
	public Object[][] getRhnMigrateClassicToRhsmWithNonDefaultProductCertDirDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getRhnMigrateClassicToRhsmWithNonDefaultProductCertDirDataAsListOfLists());
	}
	public List<List<Object>> getRhnMigrateClassicToRhsmWithNonDefaultProductCertDirDataAsListOfLists() throws JSONException, Exception {
		List<List<Object>> ll = getRhnMigrateClassicToRhsmDataAsListOfLists();
		
		// simply return a few random rows from getRhnMigrateClassicToRhsmDataAsListOfLists
		while (ll.size()>2) ll.remove(randomGenerator.nextInt(ll.size())); 
		return ll;
	}
	
	
	
	
	
	
	
}




// Notes ***********************************************************************

// EXAMPLE FOR install-num-migrate-to-rhsm TAKEN FROM THE DEPLOYMENT GUIDE http://documentation-stage.bne.redhat.com/docs/en-US/Red_Hat_Enterprise_Linux/5/html/Deployment_Guide/rhn-install-num.html
//	[root@jsefler-onprem-5server ~]# python /usr/lib/python2.4/site-packages/instnum.py da3122afdb7edd23
//	Product: RHEL Client
//	Type: Installer Only
//	Options: Eval FullProd Workstation
//	Allowed CPU Sockets: Unlimited
//	Allowed Virtual Instances: Unlimited
//	Package Repositories: Client Workstation
//
//	key: 14299426 'da3122'
//	checksum: 175 'af'
//	options: 4416 'Eval FullProd Workstation'
//	socklimit: -1 'Unlimited'
//	virtlimit: -1 'Unlimited'
//	type: 2 'Installer Only'
//	product: 1 'client'
//
//	{'Workstation': 'Workstation', 'Base': 'Client'}
//
//	da31-22af-db7e-dd23
//	[root@jsefler-onprem-5server ~]# 
//
//	[root@jsefler-onprem-5server ~]# install-num-migrate-to-rhsm -d -i da3122afdb7edd23
//	Copying /usr/share/rhsm/product/RHEL-5/Client-Workstation-x86_64-efa6382a-44c4-408b-a142-37ad4be54aa6-71.pem to /etc/pki/product/71.pem
//	Copying /usr/share/rhsm/product/RHEL-5/Client-Client-x86_64-efe91c1c-78d7-4d19-b2fb-3c88cfc2da35-68.pem to /etc/pki/product/68.pem
//	[root@jsefler-onprem-5server ~]# 
//	[root@jsefler-onprem-5server ~]# openssl x509 -text -in /usr/share/rhsm/product/RHEL-5/Client-Client-x86_64-efe91c1c-78d7-4d19-b2fb-3c88cfc2da35-68.pem | grep -A1 1.3.6.1.4.1.2312.9.1
//	            1.3.6.1.4.1.2312.9.1.68.1: 
//	                . Red Hat Enterprise Linux Desktop
//	            1.3.6.1.4.1.2312.9.1.68.2: 
//	                ..5.7
//	            1.3.6.1.4.1.2312.9.1.68.3: 
//	                ..x86_64
//	            1.3.6.1.4.1.2312.9.1.68.4: 
//	                ..rhel-5,rhel-5-client
//	[root@jsefler-onprem-5server ~]# openssl x509 -text -in /usr/share/rhsm/product/RHEL-5/Client-Workstation-x86_64-efa6382a-44c4-408b-a142-37ad4be54aa6-71.pem | grep -A1 1.3.6.1.4.1.2312.9.1
//	            1.3.6.1.4.1.2312.9.1.71.1: 
//	                .$Red Hat Enterprise Linux Workstation
//	            1.3.6.1.4.1.2312.9.1.71.2: 
//	                ..5.7
//	            1.3.6.1.4.1.2312.9.1.71.3: 
//	                ..x86_64
//	            1.3.6.1.4.1.2312.9.1.71.4: 
//	                .,rhel-5-client-workstation,rhel-5-workstation
//	[root@jsefler-onprem-5server ~]# 
	
	
// EXAMPLE FOR install-num-migrate-to-rhsm
//	[root@dell-pe1855-01 ~]# ls /etc/pki/product/
//	69.pem
//	[root@dell-pe1855-01 ~]# cat /etc/redhat-release 
//	Red Hat Enterprise Linux Server release 5.8 Beta (Tikanga)
//	[root@dell-pe1855-01 ~]# openssl x509 -text -in /etc/pki/product/69.pem | grep -A1 1.3.6.1.4.1.2312.9.1
//	            1.3.6.1.4.1.2312.9.1.69.1: 
//	                ..Red Hat Enterprise Linux Server
//	            1.3.6.1.4.1.2312.9.1.69.2: 
//	                ..5.8 Beta
//	            1.3.6.1.4.1.2312.9.1.69.3: 
//	                ..x86_64
//	            1.3.6.1.4.1.2312.9.1.69.4: 
//	                ..rhel-5,rhel-5-server
//	
//	[root@dell-pe1855-01 ~]# cat /etc/sysconfig/rhn/install-num 
//	49af89414d147589
//	[root@dell-pe1855-01 ~]# install-num-migrate-to-rhsm -d -i 49af89414d147589
//	Copying /usr/share/rhsm/product/RHEL-5/Server-Server-x86_64-fbe6b460-a559-4b02-aa3a-3e580ea866b2-69.pem to /etc/pki/product/69.pem
//	Copying /usr/share/rhsm/product/RHEL-5/Server-ClusterStorage-x86_64-66e8d727-f5aa-4e37-a04b-787fbbc3430c-90.pem to /etc/pki/product/90.pem
//	Copying /usr/share/rhsm/product/RHEL-5/Server-Cluster-x86_64-bebfe30e-22a5-4788-8611-744ea744bdc0-83.pem to /etc/pki/product/83.pem
//	[root@dell-pe1855-01 ~]# openssl x509 -text -in /usr/share/rhsm/product/RHEL-5/Server-Server-x86_64-fbe6b460-a559-4b02-aa3a-3e580ea866b2-69.pem | grep -A1 1.3.6.1.4.1.2312.9.1
//	            1.3.6.1.4.1.2312.9.1.69.1: 
//	                ..Red Hat Enterprise Linux Server
//	            1.3.6.1.4.1.2312.9.1.69.2: 
//	                ..5.7
//	            1.3.6.1.4.1.2312.9.1.69.3: 
//	                ..x86_64
//	            1.3.6.1.4.1.2312.9.1.69.4: 
//	                ..rhel-5,rhel-5-server
//	
//	[root@dell-pe1855-01 ~]# openssl x509 -text -in /usr/share/rhsm/product/RHEL-5/Server-ClusterStorage-x86_64-66e8d727-f5aa-4e37-a04b-787fbbc3430c-90.pem | grep -A1 1.3.6.1.4.1.2312.9.1
//	            1.3.6.1.4.1.2312.9.1.90.1: 
//	                .<Red Hat Enterprise Linux Resilient Storage (for RHEL Server)
//	            1.3.6.1.4.1.2312.9.1.90.2: 
//	                ..5.7
//	            1.3.6.1.4.1.2312.9.1.90.3: 
//	                ..x86_64
//	            1.3.6.1.4.1.2312.9.1.90.4: 
//	                .2rhel-5-server-clusterstorage,rhel-5-clusterstorage
//	                
//	[root@dell-pe1855-01 ~]# openssl x509 -text -in /usr/share/rhsm/product/RHEL-5/Server-Cluster-x86_64-bebfe30e-22a5-4788-8611-744ea744bdc0-83.pem | grep -A1 1.3.6.1.4.1.2312.9.1
//	            1.3.6.1.4.1.2312.9.1.83.1: 
//	                .<Red Hat Enterprise Linux High Availability (for RHEL Server)
//	            1.3.6.1.4.1.2312.9.1.83.2: 
//	                ..5.7
//	            1.3.6.1.4.1.2312.9.1.83.3: 
//	                ..x86_64
//	            1.3.6.1.4.1.2312.9.1.83.4: 
//	                .$rhel-5-server-cluster,rhel-5-cluster

	

	
// 	EXAMPLE FOR rhn-migrate-classic-to-rhsm
//	[root@jsefler-onprem-5server ~]# rhnreg_ks -v --serverUrl=https://xmlrpc.rhn.code.stage.redhat.com/XMLRPC --username=qa@redhat.com --password=CHANGE-ME --force --norhnsd --nohardware --nopackages --novirtinfo 
//		[root@jsefler-onprem-5server ~]# rhn-migrate-classic-to-rhsm -c
//		RHN Username: qa@redhat.com
//		Password: 
//
//		Retrieving existing RHN classic subscription information ...
//		+----------------------------------+
//		System is currently subscribed to:
//		+----------------------------------+
//		rhel-x86_64-server-5
//
//		List of channels for which certs are being copied
//		rhel-x86_64-server-5
//
//		Product Certificates copied successfully to /etc/pki/product !!
//
//		Preparing to unregister system from RHN classic ...
//		System successfully unregistered from RHN Classic.
//
//		Attempting to register system to Certificate-based RHN ...
//		The system has been registered with id: 78cb5e26-3a5a-459d-848c-d5b3102a864d 
//		System 'jsefler-onprem-5server.usersys.redhat.com' successfully registered to Certificate-based RHN.
//
//		Attempting to auto-subscribe to appropriate subscriptions ...
//		Installed Product Current Status:         
//
//		ProductName:          	Red Hat Enterprise Linux Server
//		Status:               	Subscribed             
//
//
//		Please visit https://access.redhat.com/management/consumers/78cb5e26-3a5a-459d-848c-d5b3102a864d to view the details, and to make changes if necessary.
//		[root@jsefler-onprem-5server ~]# subscription-manager unregister
//		System has been un-registered.
//		[root@jsefler-onprem-5server ~]# 

	
//	EXAMPLE FOR rhn-migrate-classic-to-rhsm
//	[root@jsefler-onprem-5server rhn]# rhnreg_ks  --serverUrl=https://xmlrpc.rhn.code.stage.redhat.com/XMLRPC --username=qa@redhat.com --password=CHANGE-ME --force --norhnsd --nohardware --nopackages --novirtinfo
//		ERROR: refreshing remote package list for System Profile
//		[root@jsefler-onprem-5server rhn]# rhn-channel --list
//		rhel-x86_64-server-5
//		[root@jsefler-onprem-5server rhn]# rhn-channel --user=qa@redhat.com --password=CHANGE-ME --add -c  rhel-x86_64-server-5-debuginfo -c rhx-alfresco-enterprise-2.0-rhel-x86_64-server-5
//		[root@jsefler-onprem-5server rhn]# rhn-channel --list
//		rhel-x86_64-server-5
//		rhel-x86_64-server-5-debuginfo
//		rhx-alfresco-enterprise-2.0-rhel-x86_64-server-5
//		[root@jsefler-onprem-5server rhn]# rhn-migrate-classic-to-rhsm --no-auto
//		RHN Username: qa@redhat.com
//		Password: 
//
//		Retrieving existing RHN classic subscription information ...
//		+----------------------------------+
//		System is currently subscribed to:
//		+----------------------------------+
//		rhel-x86_64-server-5
//		rhel-x86_64-server-5-debuginfo
//		rhx-alfresco-enterprise-2.0-rhel-x86_64-server-5
//
//		+--------------------------------------------------+
//		Below mentioned channels are NOT available on RHSM
//		+--------------------------------------------------+
//		rhx-alfresco-enterprise-2.0-rhel-x86_64-server-5
// ^ THESE CHANNELS ARE IN THE MAP FILE MAPPED TO none
//
//		+---------------------------------------------------------------------------------------+ 
//		Unrecognized channels. Channel to Product Certificate mapping missing for these channels.
//		+---------------------------------------------------------------------------------------+
//		rhel-x86_64-server-5-debuginfo
// ^ THESE CHANNELS ARE NOT IN THE MAP FILE AT ALL
//
//		Use --force to ignore these channels and continue the migration.
//
//		[root@jsefler-onprem-5server rhn]# echo $?
//		1
//		[root@jsefler-onprem-5server rhn]# 


//	RHEL59 EXAMPLE FOR rhn-migrate-classic-to-rhsm --servicelevel=INVALID_SLA
//	[root@jsefler-rhel59 ~]# rhn-migrate-classic-to-rhsm --servicelevel=INVALID_SLA
//	Red Hat account: qa@redhat.com
//	Password: 
//	
//	Retrieving existing RHN Classic subscription information ...
//	+----------------------------------+
//	System is currently subscribed to:
//	+----------------------------------+
//	rhel-x86_64-server-5
//	
//	List of channels for which certs are being copied
//	rhel-x86_64-server-5
//	
//	Product certificates copied successfully to /etc/pki/product
//	
//	Preparing to unregister system from RHN Classic ...
//	System successfully unregistered from RHN Classic.
//	
//	Attempting to register system to Red Hat Subscription Management ...
//	The system has been registered with id: 8fdf28e3-dc3a-44ae-910c-0f57c5187ba4 
//	System 'jsefler-rhel59.usersys.redhat.com' successfully registered to Red Hat Subscription Management.
//	
//	
//	Service level "INVALID_SLA" is not available.
//	Please select a service level agreement for this system.
//	1. SELF-SUPPORT
//	2. PREMIUM
//	3. STANDARD
//	4. NONE
//	5. No service level preference
//	? 2
//	Attempting to auto-subscribe to appropriate subscriptions ...
//	Service level set to: PREMIUM
//	Installed Product Current Status:
//	Product Name:         	Red Hat Enterprise Linux Server
//	Status:               	Not Subscribed
//	
//	
//	Unable to auto-subscribe.  Do your existing subscriptions match the products installed on this system?
//	
//	Please visit https://access.redhat.com/management/consumers/8fdf28e3-dc3a-44ae-910c-0f57c5187ba4 to view the details, and to make changes if necessary.
//	[root@jsefler-rhel59 ~]# 

