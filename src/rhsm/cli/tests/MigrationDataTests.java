package rhsm.cli.tests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.ProductCert;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
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
 */
@Test(groups={"MigrationDataTests"})
public class MigrationDataTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="Verify that the channel-cert-mapping.txt exists",
			groups={"AcceptanceTests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyChannelCertMappingFileExists_Test() {
		Assert.assertTrue(RemoteFileTasks.testExists(client, channelCertMappingFilename),"The expected channel cert mapping file '"+channelCertMappingFilename+"' exists.");
	}
	
	
	@Test(	description="Verify that the channel-cert-mapping.txt contains a unique map of channels to product certs",
			groups={"AcceptanceTests"},
			dependsOnMethods={"VerifyChannelCertMappingFileExists_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyChannelCertMapping_Test() throws FileNotFoundException, IOException {
		Assert.assertTrue(RemoteFileTasks.testExists(client, channelCertMappingFilename),"The expected channel cert mapping file '"+channelCertMappingFilename+"' exists.");
		
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
		
		// Read the channelCertMappingFilename line by line asserting unique mappings
		boolean uniqueChannelsToProductCertFilenamesMap = true;
		for (String line: result.getStdout().trim().split("\\n")){
			if (line.trim().equals("")) continue; // skip blank lines
			if (line.trim().startsWith("#")) continue; // skip comments
			String channel = line.split(":")[0].trim();
			String productCertFilename = line.split(":")[1].trim();
			if (channelsToProductCertFilenamesMap.containsKey(channel)) {
				if (!channelsToProductCertFilenamesMap.get(channel).equals(productCertFilename)) {
					log.warning("RHN Channel '"+channel+"' is already mapped to productFilename '"+productCertFilename+"' while parsing "+channelCertMappingFilename+" line: "+line);
					uniqueChannelsToProductCertFilenamesMap = false;
				}
			} else {
				Assert.fail("Having trouble parsing the following channel:product map from "+channelCertMappingFilename+": "+line);
			}
		}
		Assert.assertTrue(uniqueChannelsToProductCertFilenamesMap, "Each channel in "+channelCertMappingFilename+" maps to a unique product cert filename. (See above warnings for offenders.)");
	}
	
	
	@Test(	description="Verify that all product cert files mapped in channel-cert-mapping.txt exist",
			groups={"AcceptanceTests","blockedByBug-771615"},
			dependsOnMethods={"VerifyChannelCertMapping_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAllMappedProductCertFilesExists_Test() {

		boolean allMappedProductCertFilesExist = true;
		for (String mappedProductCertFilename : mappedProductCertFilenames) {
			String mappedProductCertFile = baseProductsDir+"/"+mappedProductCertFilename;
			if (RemoteFileTasks.testExists(client, mappedProductCertFile)) {
				log.info("Mapped productCert file '"+mappedProductCertFile+"' exists.");		
			} else {
				log.warning("Mapped productCert file '"+mappedProductCertFile+"' does NOT exist.");
				allMappedProductCertFilesExist = false;
			}
		}
		Assert.assertTrue(allMappedProductCertFilesExist,"All of the productCert files mapped in '"+channelCertMappingFilename+"' exist.");
	}
	
	
	@Test(	description="Verify that all existing product cert files are mapped in channel-cert-mapping.txt",
			groups={"AcceptanceTests","blockedByBug-799103","blockedByBug-849274"/*,"blockedByBug-909436"UNCOMMENT FOR RHEL7.0*/},
			dependsOnMethods={"VerifyChannelCertMapping_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAllExistingProductCertFilesAreMapped_Test() {
		
		// get a list of all the existing product cert files
		SSHCommandResult result = client.runCommandAndWait("ls "+baseProductsDir+"/*.pem");
		Assert.assertEquals(result.getExitCode(), new Integer(0), "Exit code from a list of all migration data product certs.");
		List<String> existingProductCertFiles = Arrays.asList(result.getStdout().split("\\n"));
		boolean allExitingProductCertFilesAreMapped = true;
		for (String existingProductCertFile : existingProductCertFiles) {
			if (mappedProductCertFilenames.contains(new File(existingProductCertFile).getName())) {
				log.info("Existing productCert file '"+existingProductCertFile+"' is mapped in '"+channelCertMappingFilename+"'.");
			} else {
				log.warning("Existing productCert file '"+existingProductCertFile+"' is NOT mapped in '"+channelCertMappingFilename+"'.");
				
				// TEMPORARY WORKAROUND FOR BUG
				/* Notes: http://entitlement.etherpad.corp.redhat.com/Entitlement02MAY12 
			    /product_ids/rhel-6.3/ComputeNode-ScalableFileSystem-x86_64-21b36280d242-175.pem  is not mapped to any RHN Channels in /cdn/product-baseline.json  (SEEMS  WRONG)
			    (dgregor) channel won't exist until 6.3 GA.  suggest we pick this up in 6.4
			    (jsefler) TODO update automated test with pre-6.3GA work-around
			    /product_ids/rhel-6.3/Server-HPN-ppc64-fff6dded9725-173.pem  is not mapped to  any RHN Channels in /cdn/product-baseline.json   (SEEMS WRONG)
			    (dgregor) channel won't exist until 6.3 GA.  suggest we pick this up in 6.4
			    (jsefler) TODO update automated test with pre-6.3GA work-around
			    */
				if (existingProductCertFile.endsWith("-173.pem") && clienttasks.redhatReleaseXY.equals("6.3")) {
					log.warning("Ignoring that existing productCert file '"+existingProductCertFile+"' is NOT mapped in '"+channelCertMappingFilename+"' until release 6.4 as recommended by dgregor.");
				} else
				if (existingProductCertFile.endsWith("-175.pem") && clienttasks.redhatReleaseXY.equals("6.3")) {
					log.warning("Ignoring that existing productCert file '"+existingProductCertFile+"' is NOT mapped in '"+channelCertMappingFilename+"' until release 6.4 as recommended by dgregor.");
				} else
				// END OF WORKAROUND
				allExitingProductCertFilesAreMapped = false;
				
			}
		}
		Assert.assertTrue(allExitingProductCertFilesAreMapped,"All of the existing productCert files in directory '"+baseProductsDir+"' are mapped to a channel in '"+channelCertMappingFilename+"'.");
	}
	
	
	@Test(	description="Verify that the migration product certs support this system's RHEL release version",
			groups={"AcceptanceTests","blockedByBug-782208"},
			dependsOnMethods={"VerifyChannelCertMapping_Test"},
			enabled=false)	// 9/12/2013 RHEL65: disabled in favor of new VerifyMigrationProductCertsSupportThisSystemsRhelVersion_Test; this old test was based on the generation of subscription-manager-migration-data from product-baseline.json
	@ImplementsNitrateTest(caseId=130940)
	public void VerifyMigrationProductCertsSupportThisSystemsRhelVersion_Test_OLD() {
		
		// process all the migration product cert files into ProductCerts and assert their version
		boolean verifiedVersionOfAllMigrationProductCertFiles = true;
		for (ProductCert productCert : clienttasks.getProductCerts(baseProductsDir)) {
			if (!productCert.productNamespace.providedTags.toLowerCase().contains("rhel")) {
				log.warning("Migration productCert '"+productCert+"' does not provide RHEL tags.  Skipping assertion that its version matches this system's RHEL version.");
				continue;
			}
			if (productCert.productNamespace.version.equals(clienttasks.redhatReleaseXY)) {
				Assert.assertTrue(true,"Migration productCert '"+productCert+"' supports this version of RHEL '"+clienttasks.redhatReleaseXY+"'.");

			} else {
				log.warning("Migration productCert '"+productCert+"' does NOT support this version of RHEL '"+clienttasks.redhatReleaseXY+"'.");
				verifiedVersionOfAllMigrationProductCertFiles = false;
			}
		}
		Assert.assertTrue(verifiedVersionOfAllMigrationProductCertFiles,"All of the migration productCerts in directory '"+baseProductsDir+"' support this version of RHEL '"+clienttasks.redhatReleaseXY+"'.");
	}
	@Test(	description="Verify that the migration product certs support this system's RHEL release version",
			groups={"AcceptanceTests","blockedByBug-782208","blockedByBug-1006060"},
			dependsOnMethods={"VerifyChannelCertMapping_Test"},
			enabled=true)
	@ImplementsNitrateTest(caseId=130940)
	public void VerifyMigrationProductCertsSupportThisSystemsRhelVersion_Test() {
		
		// process all the migration product cert files into ProductCerts and assert their version
		boolean verifiedVersionOfAllMigrationProductCertFiles = false;
		int numberOfMigrationProductCertsSupportingThisRelease = 0;
		for (ProductCert productCert : clienttasks.getProductCerts(baseProductsDir)) {
			if (!productCert.productNamespace.providedTags.toLowerCase().contains("rhel")) {
				log.info("Migration productCert '"+productCert+"' does not provide RHEL tags.  Skipping assertion that its version matches this system's RHEL version.");
				continue;
			}
			if (productCert.productNamespace.version.equals(clienttasks.redhatReleaseXY)) {
				Assert.assertTrue(true,"Migration productCert '"+productCert+"' supports this version of RHEL '"+clienttasks.redhatReleaseXY+"'.");
				numberOfMigrationProductCertsSupportingThisRelease++;
			} else {
				log.warning("Migration productCert '"+productCert+"' does NOT support this version of RHEL '"+clienttasks.redhatReleaseXY+"'.");
			}
		}
		Assert.assertTrue(numberOfMigrationProductCertsSupportingThisRelease>0,"At least one 'actual="+numberOfMigrationProductCertsSupportingThisRelease+"' migration productCerts in directory '"+baseProductsDir+"' support this version of RHEL '"+clienttasks.redhatReleaseXY+"'.");
	}
	
	
	@Test(	description="Verify that the migration product certs match those from rhn definitions",
			groups={"AcceptanceTests","blockedByBug-799152","blockedByBug-814360","blockedByBug-861420","blockedByBug-861470","blockedByBug-872959","blockedByBug-875760","blockedByBug-875802"},
			enabled=false)	// 9/9/2013 RHEL65: disabled in favor of new VerifyMigrationProductCertsMatchThoseFromRhnDefinitions_Test
	//@ImplementsNitrateTest(caseId=)
	public void VerifyMigrationProductCertsMatchThoseFromRhnDefinitions_Test_OLD() {
		
		// process all the migration product cert files into ProductCerts and assert they match those from the RHN Definitions

		// get all of the rhnDefnition product certs
		List<ProductCert> rhnDefnitionProductCerts = new ArrayList<ProductCert>();
		for (String rhnDefinitionsProductCertsDir : sm_rhnDefinitionsProductCertsDirs) {
			String tmpRhnDefinitionsProductCertsDir = clienttasks.rhnDefinitionsDir+rhnDefinitionsProductCertsDir;
			Assert.assertTrue(RemoteFileTasks.testExists(client, tmpRhnDefinitionsProductCertsDir),"The rhn definitions product certs dir '"+rhnDefinitionsProductCertsDir+"' has been locally cloned to '"+tmpRhnDefinitionsProductCertsDir+"'.");
			rhnDefnitionProductCerts.addAll(clienttasks.getProductCerts(tmpRhnDefinitionsProductCertsDir));
		}
		/* ALTERNATIVE WAY OF GETTING ALL rhnDefnition PRODUCT CERTS FROM ALL DIRECTORIES
		SSHCommandResult result = client.runCommandAndWait("find "+clienttasks.rhnDefinitionsDir+"/product_ids/ -name '*.pem'");
		String[] rhnDefnitionProductCertPaths = result.getStdout().trim().split("\\n");
		if (rhnDefnitionProductCertPaths.length==1 && rhnDefnitionProductCertPaths[0].equals("")) rhnDefnitionProductCertPaths = new String[]{};
		for (String rhnDefnitionProductCertPath : rhnDefnitionProductCertPaths) {
			rhnDefnitionProductCerts.add(clienttasks.getProductCertFromProductCertFile(new File(rhnDefnitionProductCertPath)));
		}
		*/
		
		// get the local migration product certs available for install
		List<ProductCert> migrationProductCerts = clienttasks.getProductCerts(baseProductsDir);

		// test that these local migration product certs came from the current rhnDefinitions structure
		boolean verifiedMatchForAllMigrationProductCertFiles = true;
		for (ProductCert migrationProductCert : migrationProductCerts) {
			if (rhnDefnitionProductCerts.contains(migrationProductCert)) {
				Assert.assertTrue(true, "Migration product cert '"+migrationProductCert.file+"' was found among the product certs declared for this release from ["+sm_rhnDefinitionsGitRepository+"] "+sm_rhnDefinitionsProductCertsDirs);
			} else {
				log.warning("Migration product cert '"+migrationProductCert.file+"' was NOT found among the product certs declared for this release from ["+sm_rhnDefinitionsGitRepository+"] "+sm_rhnDefinitionsProductCertsDirs+".  It may have been re-generated by release engineering.");
				verifiedMatchForAllMigrationProductCertFiles = false;
			}
		}
		
		// now assert that all of product certs from the current rhnDefinitions structure are locally available for install
		for (ProductCert rhnDefinitionProductCert : rhnDefnitionProductCerts) {
			if (migrationProductCerts.contains(rhnDefinitionProductCert)) {
				Assert.assertTrue(true, "CDN product cert ["+sm_rhnDefinitionsGitRepository+"] "+rhnDefinitionProductCert.file.getPath().replaceFirst(clienttasks.rhnDefinitionsDir, "")+" was found among the local migration product certs available for installation.");
			} else {
				
				// determine if the rhnDefinitionProductCert is not mapped to any RHEL [5|6] RHN Channels defined in the product baseline file
				List<String> rhnChannels = cdnProductBaselineProductIdMap.get(rhnDefinitionProductCert.productId);
				if (rhnChannels==null) {
					log.warning("CDN Product Baseline has an empty list of RHN Channels for Product ID '"+rhnDefinitionProductCert.productId+"' Name '"+rhnDefinitionProductCert.productName+"'.  This could be a rel-eng defect.");
					rhnChannels = new ArrayList<String>();
				}
				Set<String> rhnChannelsFilteredForRhelRelease = new HashSet<String>();
				for (String rhnChannel : rhnChannels) {
					// filter out all RHN Channels not associated with this release  (e.g., assume that an rhn channel containing "-5-" or ends in "-5" is only applicable to rhel5 
					if (!(rhnChannel.contains("-"+clienttasks.redhatReleaseX+"-") || rhnChannel.endsWith("-"+clienttasks.redhatReleaseX))) continue;
					rhnChannelsFilteredForRhelRelease.add(rhnChannel);
				}
				if (rhnChannelsFilteredForRhelRelease.isEmpty()) {
					log.info("CDN product cert ["+sm_rhnDefinitionsGitRepository+"] "+rhnDefinitionProductCert.file.getPath().replaceFirst(clienttasks.rhnDefinitionsDir, "")+" was NOT found among the current migration product certs.  No RHEL '"+clienttasks.redhatReleaseX+"' RHN Channels in '"+sm_rhnDefinitionsProductBaselineFile+"' map to Product ID '"+rhnDefinitionProductCert.productId+"' Name '"+rhnDefinitionProductCert.productName+"'.");	
				} else {
					log.warning("CDN product cert ["+sm_rhnDefinitionsGitRepository+"] "+rhnDefinitionProductCert.file.getPath().replaceFirst(clienttasks.rhnDefinitionsDir, "")+" was NOT found among the current migration product certs.  It is probably a new product cert generated by release engineering and therefore subscription-manager-migration-data needs a regeneration.");
					verifiedMatchForAllMigrationProductCertFiles = false;
				}
			}
		}
		
		Assert.assertTrue(verifiedMatchForAllMigrationProductCertFiles,"All of the migration productCerts in directory '"+baseProductsDir+"' match the current ["+sm_rhnDefinitionsGitRepository+"] product certs for this RHEL release '"+clienttasks.redhatReleaseXY+"' ");
	}
	@Test(	description="Verify that the migration product certs match those from rhn definitions",
			groups={"AcceptanceTests"/*,"blockedByBug-799152","blockedByBug-814360","blockedByBug-861420","blockedByBug-861470","blockedByBug-872959","blockedByBug-875760","blockedByBug-875802"*/},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyMigrationProductCertsMatchThoseFromRhnDefinitions_Test() {
		
		// assemble a list of rhnDefinitionsProductCertsDirs that we care about under [rcm/rcm-metadata.git] / product_ids /
		// Note: we care about all of the productCertsDirs
		SSHCommandResult result = client.runCommandAndWait("find "+clienttasks.rhnDefinitionsDir+"/product_ids -maxdepth 1 -type d");
		List<String> rhnDefinitionsProductCertsDirs = new ArrayList<String>();
		for (String productIdsDir : result.getStdout().split("\\n")) {
			if (!productIdsDir.equals(clienttasks.rhnDefinitionsDir+"/product_ids")) {
				// put logic here to exclude specific directories
				
				rhnDefinitionsProductCertsDirs.add(productIdsDir);
			}
		}
		Assert.assertTrue(!rhnDefinitionsProductCertsDirs.isEmpty(),"The "+clienttasks.rhnDefinitionsDir+"/product_ids is not empty.");
		
		
		// process all the migration product cert files into ProductCerts and assert they match those from the RHN Definitions

		// get all of the rhnDefnition product certs
		List<ProductCert> rhnDefnitionProductCerts = new ArrayList<ProductCert>();
		for (String rhnDefinitionsProductCertsDir : /*sm_*/rhnDefinitionsProductCertsDirs) {
			String tmpRhnDefinitionsProductCertsDir = /*clienttasks.rhnDefinitionsDir+*/rhnDefinitionsProductCertsDir;
			Assert.assertTrue(RemoteFileTasks.testExists(client, tmpRhnDefinitionsProductCertsDir),"The rhn definitions product certs dir '"+rhnDefinitionsProductCertsDir+"' has been locally cloned to '"+tmpRhnDefinitionsProductCertsDir+"'.");
			rhnDefnitionProductCerts.addAll(clienttasks.getProductCerts(tmpRhnDefinitionsProductCertsDir));
		}
		/* ALTERNATIVE WAY OF GETTING ALL rhnDefnition PRODUCT CERTS FROM ALL DIRECTORIES
		SSHCommandResult result = client.runCommandAndWait("find "+clienttasks.rhnDefinitionsDir+"/product_ids/ -name '*.pem'");
		String[] rhnDefnitionProductCertPaths = result.getStdout().trim().split("\\n");
		if (rhnDefnitionProductCertPaths.length==1 && rhnDefnitionProductCertPaths[0].equals("")) rhnDefnitionProductCertPaths = new String[]{};
		for (String rhnDefnitionProductCertPath : rhnDefnitionProductCertPaths) {
			rhnDefnitionProductCerts.add(clienttasks.getProductCertFromProductCertFile(new File(rhnDefnitionProductCertPath)));
		}
		*/
		
		// get the local migration product certs available for install
		List<ProductCert> migrationProductCerts = clienttasks.getProductCerts(baseProductsDir);

		// test that these local migration product certs came from the current rhnDefinitions structure
		boolean verifiedMatchForAllMigrationProductCertFiles = true;
		for (ProductCert migrationProductCert : migrationProductCerts) {
			if (rhnDefnitionProductCerts.contains(migrationProductCert)) {
				Assert.assertTrue(true, "Migration product cert '"+migrationProductCert.file+"' was found among the product certs declared for this release from ["+sm_rhnDefinitionsGitRepository+"] "+/*sm_*/rhnDefinitionsProductCertsDirs);
			} else {
				log.warning("Migration product cert '"+migrationProductCert.file+"' was NOT found among the product certs declared for this release from ["+sm_rhnDefinitionsGitRepository+"] "+/*sm_*/rhnDefinitionsProductCertsDirs+".  It may have been re-generated by release engineering.");
				verifiedMatchForAllMigrationProductCertFiles = false;
			}
		}
		
		// now assert that all of product certs from the current rhnDefinitions structure are locally available for install
		if (false) {	// NOT A VALID TEST since all product certs from the current rhnDefinitions structure may not be mapped in product-certs.json file 
		for (ProductCert rhnDefinitionProductCert : rhnDefnitionProductCerts) {
			if (migrationProductCerts.contains(rhnDefinitionProductCert)) {
				Assert.assertTrue(true, "CDN product cert ["+sm_rhnDefinitionsGitRepository+"] "+rhnDefinitionProductCert.file.getPath().replaceFirst(clienttasks.rhnDefinitionsDir, "")+" was found among the local migration product certs available for installation.");
			} else {
				
				// determine if the rhnDefinitionProductCert is not mapped to any RHEL [5|6] RHN Channels defined in the product baseline file
				List<String> rhnChannels = cdnProductBaselineProductIdMap.get(rhnDefinitionProductCert.productId);
				if (rhnChannels==null) {
					log.warning("CDN Product Baseline has an empty list of RHN Channels for Product ID '"+rhnDefinitionProductCert.productId+"' Name '"+rhnDefinitionProductCert.productName+"'.  This could be a rel-eng defect.");
					rhnChannels = new ArrayList<String>();
				}
				Set<String> rhnChannelsFilteredForRhelRelease = new HashSet<String>();
				for (String rhnChannel : rhnChannels) {
					// filter out all RHN Channels not associated with this release  (e.g., assume that an rhn channel containing "-5-" or ends in "-5" is only applicable to rhel5 
					if (!(rhnChannel.contains("-"+clienttasks.redhatReleaseX+"-") || rhnChannel.endsWith("-"+clienttasks.redhatReleaseX))) continue;
					rhnChannelsFilteredForRhelRelease.add(rhnChannel);
				}
				if (rhnChannelsFilteredForRhelRelease.isEmpty()) {
					log.info("CDN product cert ["+sm_rhnDefinitionsGitRepository+"] "+rhnDefinitionProductCert.file.getPath().replaceFirst(clienttasks.rhnDefinitionsDir, "")+" was NOT found among the current migration product certs.  No RHEL '"+clienttasks.redhatReleaseX+"' RHN Channels in '"+sm_rhnDefinitionsProductBaselineFile+"' map to Product ID '"+rhnDefinitionProductCert.productId+"' Name '"+rhnDefinitionProductCert.productName+"'.");	
				} else {
					log.warning("CDN product cert ["+sm_rhnDefinitionsGitRepository+"] "+rhnDefinitionProductCert.file.getPath().replaceFirst(clienttasks.rhnDefinitionsDir, "")+" was NOT found among the current migration product certs.  It is probably a new product cert generated by release engineering and therefore subscription-manager-migration-data needs a regeneration.");
					verifiedMatchForAllMigrationProductCertFiles = false;
				}
			}
		}
		}
		
		Assert.assertTrue(verifiedMatchForAllMigrationProductCertFiles,"All of the migration productCerts in directory '"+baseProductsDir+"' match the current ["+sm_rhnDefinitionsGitRepository+"] product certs for this RHEL release '"+clienttasks.redhatReleaseXY+"' ");
	}
	
	
	@Test(	description="Verify that all of the required RHN Channels in the ProductBaseline file are accounted for in channel-cert-mapping.txt",
			groups={},
			dependsOnMethods={"VerifyChannelCertMapping_Test"},
			dataProvider="RhnChannelFromProductBaselineData",
			enabled=false)	// 9/9/2013 RHEL65: disabling this test in favor of new VerifyChannelCertMappingFileSupportsRhnChannelFromProductCerts_Test
	//@ImplementsNitrateTest(caseId=)
	public void VerifyChannelCertMappingFileSupportsRhnChannelFromProductBaseline_Test_OLD(Object bugzilla, String productBaselineRhnChannel, String productBaselineProductId) throws JSONException {
		
		// does the cdn indicate that this channel maps to more than one product?
		if (cdnProductBaselineChannelMap.get(productBaselineRhnChannel).size()>1) {
			log.warning("According to the CDN Product Baseline, RHN Channel '"+productBaselineRhnChannel+"' maps to more than one product id: "+cdnProductBaselineChannelMap.get(productBaselineRhnChannel));
			// handle special cases to decide what productId should be mapped (see bug https://bugzilla.redhat.com/show_bug.cgi?id=786257)
			// SPECIAL CASE 1:	productId:68  productName:Red Hat Enterprise Linux Desktop
			if (Arrays.asList(
					"rhel-x86_64-client-5",
					"rhel-x86_64-client-5-debuginfo",
					"rhel-x86_64-client-5-beta",
					"rhel-x86_64-client-5-beta-debuginfo",
					"rhel-x86_64-client-supplementary-5",
					"rhel-x86_64-client-supplementary-5-debuginfo",
					"rhel-x86_64-client-supplementary-5-beta",
					"rhel-x86_64-client-supplementary-5-beta-debuginfo",
					"rhel-i386-client-5",
					"rhel-i386-client-5-debuginfo",
					"rhel-i386-client-5-beta",
					"rhel-i386-client-5-beta-debuginfo",
					"rhel-i386-client-supplementary-5",
					"rhel-i386-client-supplementary-5-debuginfo",
					"rhel-i386-client-supplementary-5-beta",
					"rhel-i386-client-supplementary-5-beta-debuginfo").contains(productBaselineRhnChannel)) {
				log.warning("However, RHN Channel '"+productBaselineRhnChannel+"' is a special case.  See https://bugzilla.redhat.com/show_bug.cgi?id=786257#c1 for more details.");
				Set<String> productIdsForDesktopAndWorkstation = new HashSet<String>();
				productIdsForDesktopAndWorkstation.add("68");	// rhel-5,rhel-5-client							Red Hat Enterprise Linux Desktop
				productIdsForDesktopAndWorkstation.add("71");	// rhel-5-client-workstation,rhel-5-workstation	Red Hat Enterprise Linux Workstation
				Assert.assertTrue(cdnProductBaselineChannelMap.get(productBaselineRhnChannel).containsAll(productIdsForDesktopAndWorkstation) && productIdsForDesktopAndWorkstation.containsAll(cdnProductBaselineChannelMap.get(productBaselineRhnChannel)),
						"Expecting RHN Channel '"+productBaselineRhnChannel+"' on the CDN Product Baseline to map only to productIds "+productIdsForDesktopAndWorkstation);
				Assert.assertEquals(getProductIdFromProductCertFilename(channelsToProductCertFilenamesMap.get(productBaselineRhnChannel)),"68",
						"As dictated in the comments of https://bugzilla.redhat.com/show_bug.cgi?id=786257 subscription-manager-migration-data file '"+channelCertMappingFilename+"' should only map RHN Channel '"+productBaselineRhnChannel+"' to productId 68.");
				return;

			// SPECIAL CASE 2:	productId:180  productName:Red Hat Beta rhnChannels:
			} else if (Arrays.asList(	
					"rhel-i386-client-dts-5-beta", 
					"rhel-i386-client-dts-5-beta-debuginfo", 
					"rhel-i386-client-dts-6-beta", 
					"rhel-i386-client-dts-6-beta-debuginfo", 
					"rhel-i386-server-dts-5-beta", 
					"rhel-i386-server-dts-5-beta-debuginfo", 
					"rhel-i386-server-dts-6-beta", 
					"rhel-i386-server-dts-6-beta-debuginfo", 
					"rhel-i386-workstation-dts-6-beta", 
					"rhel-i386-workstation-dts-6-beta-debuginfo", 
					"rhel-x86_64-client-dts-5-beta", 
					"rhel-x86_64-client-dts-5-beta-debuginfo", 
					"rhel-x86_64-client-dts-6-beta", 
					"rhel-x86_64-client-dts-6-beta-debuginfo", 
					"rhel-x86_64-hpc-node-dts-6-beta", 
					"rhel-x86_64-hpc-node-dts-6-beta-debuginfo", 
					"rhel-x86_64-server-dts-5-beta", 
					"rhel-x86_64-server-dts-5-beta-debuginfo", 
					"rhel-x86_64-server-dts-6-beta", 
					"rhel-x86_64-server-dts-6-beta-debuginfo", 
					"rhel-x86_64-workstation-dts-6-beta", 
					"rhel-x86_64-workstation-dts-6-beta-debuginfo").contains(productBaselineRhnChannel)) {
				log.warning("However, RHN Channel '"+productBaselineRhnChannel+"' is a special case.  See https://bugzilla.redhat.com/show_bug.cgi?id=820749#c4 for more details.");
				Assert.assertEquals(getProductIdFromProductCertFilename(channelsToProductCertFilenamesMap.get(productBaselineRhnChannel)),"180",
						"As dictated in the comments of https://bugzilla.redhat.com/show_bug.cgi?id=820749 subscription-manager-migration-data file '"+channelCertMappingFilename+"' should only map RHN Channel '"+productBaselineRhnChannel+"' to productId 180.");
				return;

			// SPECIAL CASE:	placeholder for next special case
			} else if (false) {
				
			} else {
				Assert.fail("Encountered an unexpected case in the CDN Product Baseline where RHN Channel '"+productBaselineRhnChannel+"' maps to more than one product id: "+cdnProductBaselineChannelMap.get(productBaselineRhnChannel)+".  Do not know how to choose which productId channel '"+productBaselineRhnChannel+"' maps to in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			}
		}
		
		// Special case for High Touch Beta productId 135  reference: https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4
		if (productBaselineProductId.equals("135")) {
			log.warning("For product id "+productBaselineProductId+" (Red Hat Enterprise Linux Server HTB), we actually do NOT want a channel cert mapping as instructed in https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(productBaselineRhnChannel),
					"CDN Product Baseline RHN Channel '"+productBaselineRhnChannel+"' supporting productId="+productBaselineProductId+" was NOT mapped to a product certificate in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.  This is a special case (Bugzilla 799152#c4).");
			return;
		}
		
		// Special case for High Touch Beta productId 155  reference: https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4
		if (productBaselineProductId.equals("155")) {
			log.warning("For product id "+productBaselineProductId+" (Red Hat Enterprise Linux Workstation HTB), we actually do NOT want a channel cert mapping as instructed in https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(productBaselineRhnChannel),
					"CDN Product Baseline RHN Channel '"+productBaselineRhnChannel+"' supporting productId="+productBaselineProductId+" was NOT mapped to a product certificate in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.  This is a special case (Bugzilla 799152#c4).");
			return;
		}
		
		// Special case for Red Hat Developer Toolset (for RHEL for IBM POWER) channels *-ppc-*  reference: https://bugzilla.redhat.com/show_bug.cgi?id=869008#c4
		if (productBaselineProductId.equals("195")) {
			if (Arrays.asList(	
					"rhel-ppc-server-dts-5-beta", 
					"rhel-ppc-server-dts-5-beta-debuginfo").contains(productBaselineRhnChannel)) {
				log.warning("DTS for ppc was added at DTS 1.1 Beta but then dropped before 1.1 GA");
				throw new SkipException("DTS for ppc was added at DTS 1.1 Beta but then dropped before 1.1 GA.  Skipping this test for channel '"+productBaselineRhnChannel+"' as instructed in https://bugzilla.redhat.com/show_bug.cgi?id=869008#c4");
			}
		}
		
		// assert that the subscription-manager-migration-data file has a mapping for this RHN Channel found in the CDN Product Baseline
		Assert.assertTrue(channelsToProductCertFilenamesMap.containsKey(productBaselineRhnChannel),
				"CDN Product Baseline RHN Channel '"+productBaselineRhnChannel+"' is accounted for in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
		
		// now assert that the subscription-manager-migration-data mapping for the RHN Channel is to the same productId as mapped in the CDN Product Baseline
		Assert.assertEquals(getProductIdFromProductCertFilename(channelsToProductCertFilenamesMap.get(productBaselineRhnChannel)), productBaselineProductId,
				"The subscription-manager-migration-data file '"+channelCertMappingFilename+"' maps RHN Channel '"+productBaselineRhnChannel+"' to the same productId as dictated in the CDN Product Baseline.");
	}
	@Test(	description="Verify that all of the required RHN Channels in the ProductCerts file are accounted for in channel-cert-mapping.txt",
			groups={},
			dependsOnMethods={"VerifyChannelCertMapping_Test"},
			dataProvider="RhnChannelFromProductCertsData",
			enabled=true) // Starting in RHEL65, we are moving away from product-baseline.json and replacing it with product-certs.json
	//@ImplementsNitrateTest(caseId=)
	public void VerifyChannelCertMappingFileSupportsRhnChannelFromProductCerts_Test(Object bugzilla, String productCertsRhnChannel, File productCertsProductFile) throws JSONException {

// UNDER CONSTRUCTION
//		// does the cdn indicate that this channel maps to more than one product?
//		if (cdnProductBaselineChannelMap.get(productCertsRhnChannel).size()>1) {
//			log.warning("According to the CDN Product Baseline, RHN Channel '"+productCertsRhnChannel+"' maps to more than one product id: "+cdnProductBaselineChannelMap.get(productCertsRhnChannel));
//			// handle special cases to decide what productId should be mapped (see bug https://bugzilla.redhat.com/show_bug.cgi?id=786257)
//			// SPECIAL CASE 1:	productId:68  productName:Red Hat Enterprise Linux Desktop
//			if (Arrays.asList(
//					"rhel-x86_64-client-5",
//					"rhel-x86_64-client-5-debuginfo",
//					"rhel-x86_64-client-5-beta",
//					"rhel-x86_64-client-5-beta-debuginfo",
//					"rhel-x86_64-client-supplementary-5",
//					"rhel-x86_64-client-supplementary-5-debuginfo",
//					"rhel-x86_64-client-supplementary-5-beta",
//					"rhel-x86_64-client-supplementary-5-beta-debuginfo",
//					"rhel-i386-client-5",
//					"rhel-i386-client-5-debuginfo",
//					"rhel-i386-client-5-beta",
//					"rhel-i386-client-5-beta-debuginfo",
//					"rhel-i386-client-supplementary-5",
//					"rhel-i386-client-supplementary-5-debuginfo",
//					"rhel-i386-client-supplementary-5-beta",
//					"rhel-i386-client-supplementary-5-beta-debuginfo").contains(productCertsRhnChannel)) {
//				log.warning("However, RHN Channel '"+productCertsRhnChannel+"' is a special case.  See https://bugzilla.redhat.com/show_bug.cgi?id=786257#c1 for more details.");
//				Set<String> productIdsForDesktopAndWorkstation = new HashSet<String>();
//				productIdsForDesktopAndWorkstation.add("68");	// rhel-5,rhel-5-client							Red Hat Enterprise Linux Desktop
//				productIdsForDesktopAndWorkstation.add("71");	// rhel-5-client-workstation,rhel-5-workstation	Red Hat Enterprise Linux Workstation
//				Assert.assertTrue(cdnProductBaselineChannelMap.get(productCertsRhnChannel).containsAll(productIdsForDesktopAndWorkstation) && productIdsForDesktopAndWorkstation.containsAll(cdnProductBaselineChannelMap.get(productCertsRhnChannel)),
//						"Expecting RHN Channel '"+productCertsRhnChannel+"' on the CDN Product Baseline to map only to productIds "+productIdsForDesktopAndWorkstation);
//				Assert.assertEquals(getProductIdFromProductCertFilename(channelsToProductCertFilenamesMap.get(productCertsRhnChannel)),"68",
//						"As dictated in the comments of https://bugzilla.redhat.com/show_bug.cgi?id=786257 subscription-manager-migration-data file '"+channelCertMappingFilename+"' should only map RHN Channel '"+productCertsRhnChannel+"' to productId 68.");
//				return;
//
//			// SPECIAL CASE 2:	productId:180  productName:Red Hat Beta rhnChannels:
//			} else if (Arrays.asList(	
//					"rhel-i386-client-dts-5-beta", 
//					"rhel-i386-client-dts-5-beta-debuginfo", 
//					"rhel-i386-client-dts-6-beta", 
//					"rhel-i386-client-dts-6-beta-debuginfo", 
//					"rhel-i386-server-dts-5-beta", 
//					"rhel-i386-server-dts-5-beta-debuginfo", 
//					"rhel-i386-server-dts-6-beta", 
//					"rhel-i386-server-dts-6-beta-debuginfo", 
//					"rhel-i386-workstation-dts-6-beta", 
//					"rhel-i386-workstation-dts-6-beta-debuginfo", 
//					"rhel-x86_64-client-dts-5-beta", 
//					"rhel-x86_64-client-dts-5-beta-debuginfo", 
//					"rhel-x86_64-client-dts-6-beta", 
//					"rhel-x86_64-client-dts-6-beta-debuginfo", 
//					"rhel-x86_64-hpc-node-dts-6-beta", 
//					"rhel-x86_64-hpc-node-dts-6-beta-debuginfo", 
//					"rhel-x86_64-server-dts-5-beta", 
//					"rhel-x86_64-server-dts-5-beta-debuginfo", 
//					"rhel-x86_64-server-dts-6-beta", 
//					"rhel-x86_64-server-dts-6-beta-debuginfo", 
//					"rhel-x86_64-workstation-dts-6-beta", 
//					"rhel-x86_64-workstation-dts-6-beta-debuginfo").contains(productCertsRhnChannel)) {
//				log.warning("However, RHN Channel '"+productCertsRhnChannel+"' is a special case.  See https://bugzilla.redhat.com/show_bug.cgi?id=820749#c4 for more details.");
//				Assert.assertEquals(getProductIdFromProductCertFilename(channelsToProductCertFilenamesMap.get(productCertsRhnChannel)),"180",
//						"As dictated in the comments of https://bugzilla.redhat.com/show_bug.cgi?id=820749 subscription-manager-migration-data file '"+channelCertMappingFilename+"' should only map RHN Channel '"+productCertsRhnChannel+"' to productId 180.");
//				return;
//
//			// SPECIAL CASE:	placeholder for next special case
//			} else if (false) {
//				
//			} else {
//				Assert.fail("Encountered an unexpected case in the CDN Product Baseline where RHN Channel '"+productCertsRhnChannel+"' maps to more than one product id: "+cdnProductBaselineChannelMap.get(productCertsRhnChannel)+".  Do not know how to choose which productId channel '"+productCertsRhnChannel+"' maps to in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
//			}
//		}
//		
//		// Special case for High Touch Beta productId 135  reference: https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4
//		if (productCertsProductId.equals("135")) {
//			log.warning("For product id "+productCertsProductId+" (Red Hat Enterprise Linux Server HTB), we actually do NOT want a channel cert mapping as instructed in https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4");
//			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(productCertsRhnChannel),
//					"CDN Product Baseline RHN Channel '"+productCertsRhnChannel+"' supporting productId="+productCertsProductId+" was NOT mapped to a product certificate in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.  This is a special case (Bugzilla 799152#c4).");
//			return;
//		}
//		
//		// Special case for High Touch Beta productId 155  reference: https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4
//		if (productCertsProductId.equals("155")) {
//			log.warning("For product id "+productCertsProductId+" (Red Hat Enterprise Linux Workstation HTB), we actually do NOT want a channel cert mapping as instructed in https://bugzilla.redhat.com/show_bug.cgi?id=799152#c4");
//			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(productCertsRhnChannel),
//					"CDN Product Baseline RHN Channel '"+productCertsRhnChannel+"' supporting productId="+productCertsProductId+" was NOT mapped to a product certificate in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.  This is a special case (Bugzilla 799152#c4).");
//			return;
//		}
		
		// assert that the subscription-manager-migration-data file has a mapping for this RHN Channel found in the CDN Product Certs
		Assert.assertTrue(channelsToProductCertFilenamesMap.containsKey(productCertsRhnChannel),
				"CDN Product Certs RHN Channel '"+productCertsRhnChannel+"' is accounted for in the subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
	
		// now assert that the subscription-manager-migration-data mapping for the RHN Channel is to the same product cert file as mapped in the CDN Product Certs
		if (!channelsToProductCertFilenamesMap.get(productCertsRhnChannel).equals(productCertsProductFile.getName())) {
			ProductCert migrationProductCert = clienttasks.getProductCertFromProductCertFile(new File(baseProductsDir+"/"+channelsToProductCertFilenamesMap.get(productCertsRhnChannel)));
			ProductCert rhnDefinitionsProductCert = clienttasks.getProductCertFromProductCertFile(new File (clienttasks.rhnDefinitionsDir+"/product_ids"+productCertsProductFile));
			log.warning("The subscription-manager-migration-data file '"+channelCertMappingFilename+"' maps RHN Channel '"+productCertsRhnChannel+"' to '"+channelsToProductCertFilenamesMap.get(productCertsRhnChannel)+"' which is different than the rhnDefinitions product-certs.json mapping to '"+productCertsProductFile+"'.  Comparing contents for effective equality...");
			log.info("Migration product cert '"+migrationProductCert.file+"':  "+migrationProductCert.productNamespace);
			log.info("CDN Product Cert '"+rhnDefinitionsProductCert.file+"':  "+rhnDefinitionsProductCert.productNamespace);
			log.info("Expecting those to be effectively equal.  If not, then a release-engineering bug is likely.");
			Assert.assertEquals(migrationProductCert.productNamespace.name, rhnDefinitionsProductCert.productNamespace.name, "Comparing productNamespace.name between '"+rhnDefinitionsProductCert.file+"' and '"+migrationProductCert.file+"'");
			Assert.assertEquals(migrationProductCert.productNamespace.id, rhnDefinitionsProductCert.productNamespace.id, "Comparing productNamespace.id between '"+rhnDefinitionsProductCert.file+"' and '"+migrationProductCert.file+"'");
			Assert.assertEquals(migrationProductCert.productNamespace.arch, rhnDefinitionsProductCert.productNamespace.arch, "Comparing productNamespace.arch between '"+rhnDefinitionsProductCert.file+"' and '"+migrationProductCert.file+"'");
			Assert.assertEquals(migrationProductCert.productNamespace.providedTags, rhnDefinitionsProductCert.productNamespace.providedTags, "Comparing productNamespace.providedTags between '"+rhnDefinitionsProductCert.file+"' and '"+migrationProductCert.file+"'");
			Assert.assertEquals(migrationProductCert.productNamespace.version, rhnDefinitionsProductCert.productNamespace.version, "Comparing productNamespace.version between '"+rhnDefinitionsProductCert.file+"' and '"+migrationProductCert.file+"'");
		} else {
		
			Assert.assertEquals(channelsToProductCertFilenamesMap.get(productCertsRhnChannel), productCertsProductFile.getName(),
				"The subscription-manager-migration-data file '"+channelCertMappingFilename+"' maps RHN Channel '"+productCertsRhnChannel+"' to the same product cert file as dictated in the CDN Product Certs.");
		}
	}
	
	
	@Test(	description="Verify that all of the classic RHN Channels available to a classically registered consumer are accounted for in the in the channel-cert-mapping.txt or is a known exception",
			groups={"AcceptanceTests"},
			dependsOnMethods={"VerifyChannelCertMapping_Test"},
			dataProvider="getRhnClassicBaseAndAvailableChildChannelsData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyChannelCertMappingFileSupportsRhnClassicBaseAndAvailableChildChannel_Test(Object bugzilla, String classicRhnChannel) {
		
		// SPECIAL CASES.....
		
		// 201205032049:22.817 - WARNING: RHN Classic channel 'rhel-x86_64-server-6-cf-ae-1' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.817 - WARNING: RHN Classic channel 'rhel-x86_64-server-6-cf-ae-1-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.817 - WARNING: RHN Classic channel 'rhel-x86_64-server-6-cf-ae-1-beta-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.817 - WARNING: RHN Classic channel 'rhel-x86_64-server-6-cf-ae-1-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// (degregor 5/4/2012) CloudForms Application Engine isn't shipping anytime soon, so we decided to remove the CDN repos.  While the channels are there in RHN, no one has access to them.
		if (classicRhnChannel.matches("rhel-.+-6-cf-ae-1(-.*|$)")) {
			log.warning("(degregor 5/4/2012) CloudForms Application Engine isn't shipping anytime soon, so we decided to remove the CDN repos.  While the channels are there in RHN, no one has access to them.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		// 201205032049:22.817 - WARNING: RHN Classic channel 'rhel-x86_64-server-6-htb' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.817 - WARNING: RHN Classic channel 'rhel-x86_64-server-6-htb-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.827 - WARNING: RHN Classic channel 'sam-rhel-x86_64-server-6-htb' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.828 - WARNING: RHN Classic channel 'sam-rhel-x86_64-server-6-htb-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// (degregor 5/4/2012) We intentionally exclude HTB channels from the migration script.  It's not a supported use case.
		if (classicRhnChannel.matches(".+-htb(-.*|$)")) {
			log.warning("(degregor 5/4/2012) We intentionally exclude HTB channels from the migration script.  It's not a supported use case.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		// 201205032049:22.819 - WARNING: RHN Classic channel 'rhel-x86_64-server-clusteredstorage-6-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.820 - WARNING: RHN Classic channel 'rhel-x86_64-server-ei-replication-6' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.820 - WARNING: RHN Classic channel 'rhel-x86_64-server-ei-replication-6-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.820 - WARNING: RHN Classic channel 'rhel-x86_64-server-ei-replication-6-beta-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.820 - WARNING: RHN Classic channel 'rhel-x86_64-server-ei-replication-6-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'. 
		// (degregor 5/4/2012) The above channels aren't used.
		if (classicRhnChannel.matches("rhel-.+-ei-replication-6(-.*|$)")  || classicRhnChannel.matches("rhel-.+-clusteredstorage-6(-.*|$)")) {
			log.warning("(degregor 5/4/2012) The above channels aren't used.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		// 201205032049:22.827 - WARNING: RHN Classic channel 'rhn-tools-rhel-x86_64-server-6' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.827 - WARNING: RHN Classic channel 'rhn-tools-rhel-x86_64-server-6-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.827 - WARNING: RHN Classic channel 'rhn-tools-rhel-x86_64-server-6-beta-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205032049:22.827 - WARNING: RHN Classic channel 'rhn-tools-rhel-x86_64-server-6-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// (degregor 5/4/2012) RHN Tools content doesn't get delivered through CDN.
		if (classicRhnChannel.startsWith("rhn-tools-rhel-")) {
			log.warning("(degregor 5/4/2012) RHN Tools content doesn't get delivered through CDN.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		// 201205080442:43.007 - WARNING: RHN Classic channel 'rhel-x86_64-server-highavailability-6-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205080442:43.008 - WARNING: RHN Classic channel 'rhel-x86_64-server-largefilesystem-6-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205080442:43.010 - WARNING: RHN Classic channel 'rhel-x86_64-server-loadbalance-6-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// (degregor 5/8/2012) These channels are not used and can be ignored.
		if (classicRhnChannel.matches("rhel-.+-highavailability-6-beta") || classicRhnChannel.matches("rhel-.+-largefilesystem-6-beta") || classicRhnChannel.matches("rhel-.+-loadbalance-6-beta")) {
			log.warning("(degregor 5/8/2012) These channels are not used and can be ignored.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		// 201205080556:10.326 - WARNING: RHN Classic channel 'rhel-x86_64-server-hts-6' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		// 201205080556:10.326 - WARNING: RHN Classic channel 'rhel-x86_64-server-hts-6-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'.
		//	RHN Classic channel 'rhel-x86_64-server-hts-5' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-5/channel-cert-mapping.txt'.
		//	RHN Classic channel 'rhel-x86_64-server-hts-5-beta' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-5/channel-cert-mapping.txt'.
		//	RHN Classic channel 'rhel-x86_64-server-hts-5-beta-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-5/channel-cert-mapping.txt'.
		//	RHN Classic channel 'rhel-x86_64-server-hts-5-debuginfo' is NOT mapped in the file '/usr/share/rhsm/product/RHEL-5/channel-cert-mapping.txt'.
		// (degregor 5/8/2012) We're not delivering Hardware Certification (aka hts) bits through the CDN at this point.
		if (classicRhnChannel.matches("rhel-.+-hts-"+clienttasks.redhatReleaseX+"(-.*|$)")) {
			log.warning("(degregor 5/8/2012) We're not delivering Hardware Certification (aka hts) bits through the CDN at this point.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		// RHN Classic channel 'rhel-x86_64-server-6-rhui-2' is accounted for in subscription-manager-migration-data file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'. expected:<true> but was:<false>
		// RHN Classic channel 'rhel-x86_64-server-6-rhui-2-debuginfo' is accounted for in subscription-manager-migration-data file '/usr/share/rhsm/product/RHEL-6/channel-cert-mapping.txt'. expected:<true> but was:<false>
		// https://bugzilla.redhat.com/show_bug.cgi?id=819089#c2
		// https://bugzilla.redhat.com/show_bug.cgi?id=819089#c3
		if (classicRhnChannel.matches("rhel-.+-rhui-2(-.*|$)")) {
			log.warning("(jgregusk 12/11/2012) Migrating a RHUI installation from Classic is not a supported (or even valid) use case.  See https://bugzilla.redhat.com/show_bug.cgi?id=819089#c3");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		if (classicRhnChannel.matches("rhel-.+-server-5-mrg-.*")) {	// rhel-x86_64-server-5-mrg-grid-1 rhel-x86_64-server-5-mrg-grid-1-beta rhel-x86_64-server-5-mrg-grid-2 rhel-x86_64-server-5-mrg-grid-execute-1 rhel-x86_64-server-5-mrg-grid-execute-1-beta rhel-x86_64-server-5-mrg-grid-execute-2 etc.
			// Bug 840102 - channels for rhel-<ARCH>-server-5-mrg-* are not yet mapped to product certs in rcm/rcm-metadata.git
			log.warning("(degregor 8/4/2012) RHEL 5 MRG isn't currently supported in CDN (outside of RHUI) - https://bugzilla.redhat.com/show_bug.cgi?id=840102#c1");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.matches("rhel-.+-server-hpc-5(-.*|$)")) {	// rhel-x86_64-server-hpc-5-beta
			// Bug 840103 - channel for rhel-x86_64-server-hpc-5-beta is not yet mapped to product cert in rcm/rcm-metadata.git
			log.warning("(degregor 8/4/2012) The RHEL 5 HPC products is not currently supported in CDN - https://bugzilla.redhat.com/show_bug.cgi?id=840103#c1");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.matches("rhel-.+-server-rhev-hdk-2-5(-.+|$)")) {	// rhel-x86_64-server-rhev-hdk-2-5 rhel-x86_64-server-rhev-hdk-2-5-beta
			// Bug 840108 - channels for rhel-<ARCH>-rhev-hdk-2-5-* are not yet mapped to product certs in rcm/rhn-definitions.git
			log.warning("(degregor 8/4/2012) RHEV H Dev Kit is not currently supported in CDN - https://bugzilla.redhat.com/show_bug.cgi?id=840108#c1");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.startsWith("rhx-")) {	// rhx-alfresco-enterprise-2.0-rhel-x86_64-server-5 rhx-amanda-enterprise-backup-2.6-rhel-x86_64-server-5 etcetera
			// Bug 840111 - various rhx channels are not yet mapped to product certs in rcm/rcm-metadata.git 
			log.warning("(degregor 8/4/2012) RHX products are not currently supported in CDN - https://bugzilla.redhat.com/show_bug.cgi?id=840111#c2");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.matches("rhel-.+-client-dts-5(-.*|$)")) {	// rhel-i386-client-dts-5-debuginfo rhel-i386-client-dts-5-beta-debuginfo rhel-i386-client-dts-5-beta rhel-i386-client-dts-5
			// Bug 969160 - rhel-*-client-dts-5* channels are not mapped in channel-cert-mapping.txt
			// Bug 969156 - RHN Channels: [] in product-baseline.json is empty for "Red Hat Developer Toolset (for RHEL Client)"
			log.warning("(degregor 5/31/2013) DTS for Client got dropped.  Those channels shouldn't be available in RHN. - https://bugzilla.redhat.com/show_bug.cgi?id=969156#c1");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		if (classicRhnChannel.matches("rhel-.+-client-multimedia-5(-.*|$)")) {	// rhel-i386-client-multimedia-5 rhel-i386-client-multimedia-5-beta
			log.warning("(degregor 5/31/2013) I don't think we ever added these to CDN.  Please ignore them for now.");
			Assert.assertTrue(!channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "Special case RHN Classic channel '"+classicRhnChannel+"' is NOT accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
			return;
		}
		
		Assert.assertTrue(channelsToProductCertFilenamesMap.containsKey(classicRhnChannel), "RHN Classic channel '"+classicRhnChannel+"' is accounted for in subscription-manager-migration-data file '"+channelCertMappingFilename+"'.");
	}
	
	
	
	
	
	
	
	// Candidates for an automated Test:

	
	
	
	// Configuration methods ***********************************************************************
	

	@BeforeClass(groups="setup")
	public void setupBeforeClass() {
		if (clienttasks==null) return;
		
		// determine the full path to the channelCertMappingFile
		baseProductsDir+="-"+clienttasks.redhatReleaseX;
		channelCertMappingFilename = baseProductsDir+"/"+channelCertMappingFilename;
		
		// make sure needed rpms are installed
		for (String pkg : new String[]{"subscription-manager-migration", "subscription-manager-migration-data", "expect"}) {
			Assert.assertTrue(clienttasks.isPackageInstalled(pkg),"Required package '"+pkg+"' is installed for MigrationTests.");
		}
	}
	
	
//	@BeforeClass(groups="setup", dependsOnMethods={"setupBeforeClass"})
//	public void rememberOriginallyInstalledRedHatProductCertsBeforeClass() {
//		
//		// review the currently installed product certs and filter out the ones from test automation (indicated by suffix "_.pem")
//		for (File productCertFile : clienttasks.getCurrentProductCertFiles()) {
//			if (!productCertFile.getName().endsWith("_.pem")) {	// The product cert files ending in "_.pem" are not true RedHat products
//				originallyInstalledRedHatProductCerts.add(clienttasks.getProductCertFromProductCertFile(productCertFile));
//			}
//		}
//	}
//	
//	@BeforeClass(groups="setup", dependsOnMethods={"setupBeforeClass"})
//	public void rememberOriginallyConfiguredServerUrlBeforeClass() {
//		if (clienttasks==null) return;
//		
//		originalServerHostname 	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname");
//		originalServerPort		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port");
//		originalServerPrefix	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "prefix");
//	}
//	
//	@BeforeClass(groups="setup", dependsOnMethods={"setupBeforeClass"})
//	public void backupProductCertsBeforeClass() {
//		
//		// determine the original productCertDir value
//		//productCertDirRestore = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "productCertDir");
//		originalProductCertDir = clienttasks.productCertDir;
//		
//		log.info("Backing up all the currently installed product certs...");
//		client.runCommandAndWait("mkdir -p "+backupProductCertDir+"; rm -f "+backupProductCertDir+"/*.pem");
//		client.runCommandAndWait("cp "+originalProductCertDir+"/*.pem "+backupProductCertDir);
//	}
//	
//	@BeforeGroups(groups="setup",value={"InstallNumMigrateToRhsmWithInstNumber_Test","InstallNumMigrateToRhsm_Test","RhnMigrateClassicToRhsm_Test"})
//	public void configOriginalRhsmProductCertDir() {
//		if (clienttasks==null) return;
//		
//		//clienttasks.config(false, false, true, new String[]{"rhsm","productcertdir",productCertDirOriginal});
//		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", originalProductCertDir);
//	}
	
	@BeforeClass(groups="setup")
//	@AfterGroups(groups="setup",value={"RhnMigrateClassicToRhsmUsingProxyServer_Test"})
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
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "enableProxyAuth", "0");	// enableProxyAuth[comment]=To use an authenticated proxy or not
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "proxyUser", "");			// proxyUser[comment]=The username for an authenticated proxy
		clienttasks.updateConfFileParameter(clienttasks.rhnUp2dateFile, "proxyPassword", "");		// proxyPassword[comment]=The password to use for an authenticated proxy
		
//		iptablesAcceptPort(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "port"));
	}
	
	@BeforeClass(groups="setup", dependsOnMethods={"setupBeforeClass"})
	public void copyScriptsToClient() throws IOException {
		// copy the rhn-channels.py script to the client
		File rhnChannelsScriptFile = new File(System.getProperty("automation.dir", null)+"/scripts/rhn-channels.py");
		if (!rhnChannelsScriptFile.exists()) Assert.fail("Failed to find expected script: "+rhnChannelsScriptFile);
		RemoteFileTasks.putFile(client.getConnection(), rhnChannelsScriptFile.toString(), "/usr/local/bin/", "0755");
		
		// copy the rhn-is-registered.py script to the client
		File rhnIsRegisteredScriptFile = new File(System.getProperty("automation.dir", null)+"/scripts/rhn-is-registered.py");
		if (!rhnIsRegisteredScriptFile.exists()) Assert.fail("Failed to find expected script: "+rhnIsRegisteredScriptFile);
		RemoteFileTasks.putFile(client.getConnection(), rhnIsRegisteredScriptFile.toString(), "/usr/local/bin/", "0755");

		// copy the rhn-migrate-classic-to-rhsm.tcl script to the client
		File expectScriptFile = new File(System.getProperty("automation.dir", null)+"/scripts/rhn-migrate-classic-to-rhsm.tcl");
		if (!expectScriptFile.exists()) Assert.fail("Failed to find expected script: "+expectScriptFile);
		RemoteFileTasks.putFile(client.getConnection(), expectScriptFile.toString(), "/usr/local/bin/", "0755");
	}
	
	@BeforeClass(groups="setup", dependsOnMethods={"setupBeforeClass","copyScriptsToClient"})
	public void determineRhnClassicBaseAndAvailableChildChannels() throws IOException {
		if (sm_rhnUsername.equals("")) {log.warning("Skipping determination of the base and available RHN Classic channels"); return;}
		if (sm_rhnPassword.equals("")) {log.warning("Skipping determination of the base and available RHN Classic channels"); return;}
		if (sm_rhnHostname.equals("")) {log.warning("Skipping determination of the base and available RHN Classic channels"); return;}

		// get the base channel
		clienttasks.registerToRhnClassic(sm_rhnUsername, sm_rhnPassword, sm_rhnHostname);
		List<String> rhnChannels = clienttasks.getCurrentRhnClassicChannels();
		//Assert.assertEquals(rhnChannels.size(), 1, "The number of base RHN Classic base channels this system is consuming.");
		if (rhnChannels.isEmpty()) {
			log.warning("When no RHN channels are available to this classically registered system, no product certs will be migrated to RHSM.");
			return; 
		}
		rhnBaseChannel = clienttasks.getCurrentRhnClassicChannels().get(0);

		// get all of the available RHN Classic child channels available for consumption under this base channel
		rhnAvailableChildChannels.clear();
		String command = String.format("rhn-channels.py --username=%s --password=%s --server=%s --basechannel=%s --no-custom --available", sm_rhnUsername, sm_rhnPassword, sm_rhnHostname, rhnBaseChannel);
		//debugTesting if (true) command = "echo rhel-x86_64-server-5 && echo rhx-alfresco-enterprise-2.0-rhel-x86_64-server-5 && echo rhx-amanda-enterprise-backup-2.6-rhel-x86_64-server-5";
		
		SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(client, command, Integer.valueOf(0));
		rhnChannels = new ArrayList<String>();
		if (!result.getStdout().trim().equals("")) {
			rhnChannels	= Arrays.asList(result.getStdout().trim().split("\\n"));
		}
		for (String rhnChannel : rhnChannels) {
			if (!rhnChannel.equals(rhnBaseChannel)) rhnAvailableChildChannels.add(rhnChannel.trim()); 
		}
		Assert.assertTrue(rhnAvailableChildChannels.size()>0,"A positive number of child channels under the RHN Classic base channel '"+rhnBaseChannel+"' are available for consumption.");
	}
	
//	@BeforeGroups(groups="setup",value={"InstallNumMigrateToRhsmWithNonDefaultProductCertDir_Test","RhnMigrateClassicToRhsmWithNonDefaultProductCertDir_Test"})
//	public void configNonDefaultRhsmProductCertDir() {
//		if (clienttasks==null) return;
//		
//		//clienttasks.config(false, false, true, new String[]{"rhsm","productcertdir",productCertDirNonDefault});
//		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", nonDefaultProductCertDir);
//	}
//
//	
//	public static SSHCommandRunner basicAuthProxyRunner = null;
//	public static SSHCommandRunner noAuthProxyRunner = null;
//	@BeforeClass(groups={"setup"})
//	public void setupProxyRunnersBeforeClass() throws IOException {
//		basicAuthProxyRunner = new SSHCommandRunner(sm_basicauthproxyHostname, sm_sshUser, sm_sshKeyPrivate, sm_sshkeyPassphrase, null);
//		noAuthProxyRunner = new SSHCommandRunner(sm_noauthproxyHostname, sm_sshUser, sm_sshKeyPrivate, sm_sshkeyPassphrase, null);
//	}
//	
//	@AfterClass(groups="setup")
//	public void restoreProductCertsAfterClass() {
//		if (clienttasks==null) return;
//		
//		log.info("Restoring the originally installed product certs...");
//		client.runCommandAndWait("rm -f "+originalProductCertDir+"/*.pem");
//		client.runCommandAndWait("cp "+backupProductCertDir+"/*.pem "+originalProductCertDir);
//		configOriginalRhsmProductCertDir();
//	}
//	
//	@AfterClass(groups="setup")
//	@AfterGroups(groups="setup",value={"RhnMigrateClassicToRhsm_Test"})
//	public void restoreOriginallyConfiguredServerUrl() {
//		if (clienttasks==null) return;
//		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
//		listOfSectionNameValues.add(new String[]{"server","hostname",originalServerHostname});
//		listOfSectionNameValues.add(new String[]{"server","port",originalServerPort});
//		listOfSectionNameValues.add(new String[]{"server","prefix",originalServerPrefix});
//		log.info("Restoring the originally configured server URL...");
//		clienttasks.config(null, null, true, listOfSectionNameValues);
//	}
	
	@AfterClass(groups={"setup"})
	public void removeRHNSystemIdFileAfterClass() {
		if (clienttasks!=null) {
			clienttasks.removeRhnSystemIdFile();
		}
	}
	
	@BeforeClass(groups={"setup"})
	public void determineCdnProductBaselineMapsBeforeClass() throws IOException, JSONException {

		// Reference: https://engineering.redhat.com/trac/rcm/wiki/Projects/CDNBaseline

		// THE JSON LOOKS LIKE THIS...
		//	[
		//		{
		//			"Content Sets": [
		//				{
		//					"Label": "rhel-hpn-for-rhel-6-server-source-rpms", 
		//					"Repos": [
		//						{
		//							"Relative URL": "/content/dist/rhel/server/6/6.1/i386/hpn/source/SRPMS"
		//						}, 
		//						{
		//							"Relative URL": "/content/dist/rhel/server/6/6.2/x86_64/hpn/source/SRPMS"
		//						}
		//					]
		//				}
		//			], 
		//			"Name": "Red Hat Enterprise Linux High Performance Networking (for RHEL Server)", 
		//			"Product ID": "132", 
		//			"RHN Channels": [
		//				"rhel-x86_64-server-hpn-6", 
		//				"rhel-x86_64-server-hpn-6-beta-debuginfo", 
		//				"rhel-x86_64-server-hpn-6-beta", 
		//				"rhel-x86_64-server-hpn-6-debuginfo"
		//			]
		//		}
		//	]
		client.runCommandAndWaitWithoutLogging("cat "+clienttasks.rhnDefinitionsDir+sm_rhnDefinitionsProductBaselineFile);
		JSONArray jsonProducts = new JSONArray(client.getStdout());	
		for (int p = 0; p < jsonProducts.length(); p++) {
			JSONObject jsonProduct = (JSONObject) jsonProducts.get(p);
			String productName = jsonProduct.getString("Name");
			String productId = jsonProduct.getString("Product ID");
			JSONArray jsonRhnChannels = jsonProduct.getJSONArray("RHN Channels");
			
			// process each of the RHN Channels
			for (int r=0; r<jsonRhnChannels.length(); r++) {
				String rhnChannel = jsonRhnChannels.getString(r);
				
				// store the rhnChannel in the cdnProductBaselineChannelMap
				if (cdnProductBaselineChannelMap.containsKey(rhnChannel)) {
					if (!cdnProductBaselineChannelMap.get(rhnChannel).contains(productId)) {
						cdnProductBaselineChannelMap.get(rhnChannel).add(productId);
					}
				} else {
					List<String> productIds = new ArrayList<String>(); productIds.add(productId);
					cdnProductBaselineChannelMap.put(rhnChannel, productIds);
				}
				
				// also store the inverse of this map into cdnProductBaselineProductIdMap
				if (cdnProductBaselineProductIdMap.containsKey(productId)) {
					if (!cdnProductBaselineProductIdMap.get(productId).contains(rhnChannel)) {
						cdnProductBaselineProductIdMap.get(productId).add(rhnChannel);
					}
				} else {
					List<String> rhnChannels = new ArrayList<String>(); rhnChannels.add(rhnChannel);
					cdnProductBaselineProductIdMap.put(productId, rhnChannels);
				}
			}
		}
	}
	
	@BeforeClass(groups={"setup"})
	public void determineCdnProductCertsMapsBeforeClass() throws IOException, JSONException {

		// Reference: http://git.app.eng.bos.redhat.com/?p=rcm/rcm-metadata.git;a=blob;f=cdn/product-certs.json

		// THE JSON LOOKS LIKE THIS...
		//	{
		//		"rhel-i386-rhev-agent-6-server": {
		//	         "Product Cert CN": "Red Hat Product ID [625d9640-910d-4e56-8fc3-d98163bd81a0]", 
		//	         "Product Cert file": "/rhel-6.3/Server-Server-i386-d98163bd81a0-69.pem", 
		//	         "Product ID": "69"
		//	     }, 
		//	     "rhel-i386-rhev-agent-6-workstation": {
		//	         "Product Cert CN": "Red Hat Product ID [b29e876e-746f-4062-8977-a0bad2ffd9b4]", 
		//	         "Product Cert file": "/rhel-6.3/Workstation-Workstation-i386-a0bad2ffd9b4-71.pem", 
		//	         "Product ID": "71"
		//	     }, 
		//	     "rhel-i386-server-5": {
		//	         "Product Cert CN": "Red Hat Product ID [693973c8-9bb8-4b01-b27d-2445a981e321]", 
		//	         "Product Cert file": "/rhel-5.8/Server-Server-i386-2445a981e321-69.pem", 
		//	         "Product ID": "69"
		//	     }, 
		//	}
		client.runCommandAndWaitWithoutLogging("cat "+clienttasks.rhnDefinitionsDir+sm_rhnDefinitionsProductCertsFile);
		JSONObject jsonChannelCertsMap = new JSONObject(client.getStdout());
		
		Iterator<String> rhnChannels = jsonChannelCertsMap.keys();
		while (rhnChannels.hasNext()) {
			String rhnChannel = rhnChannels.next();
			JSONObject jsonChannelCertMap = jsonChannelCertsMap.getJSONObject(rhnChannel);
			String productCertFile = jsonChannelCertMap.getString("Product Cert file");
			cdnProductCertsChannelMap.put(rhnChannel, new File(productCertFile));
			
//			cdnProductCertsChannelToProductCertMap.put(rhnChannel, clienttasks.getProductCertFromProductCertFile(new File (clienttasks.rhnDefinitionsDir+"/product_ids"+productCertFile)));
		}
	}
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 955824 - Product certs should not be generated with a tag value of "None"
	
	
	
	
	// Protected methods ***********************************************************************
	protected String baseProductsDir = "/usr/share/rhsm/product/RHEL";
	protected String channelCertMappingFilename = "channel-cert-mapping.txt";
	protected List<String> mappedProductCertFilenames = new ArrayList<String>();	// list of all the mapped product cert file names in the mapping file (e.g. Server-Server-x86_64-fbe6b460-a559-4b02-aa3a-3e580ea866b2-69.pem)
	protected Map<String,String> channelsToProductCertFilenamesMap = new HashMap<String,String>();	// map of all the channels to product cert file names (e.g. key=rhn-tools-rhel-x86_64-server-5 value=Server-Server-x86_64-fbe6b460-a559-4b02-aa3a-3e580ea866b2-69.pem)
	protected Map<String,List<String>> cdnProductBaselineChannelMap = new HashMap<String,List<String>>();	// map of all the channels to list of productIds (e.g. key=rhn-tools-rhel-x86_64-server-5 value=[69,169,269])
	protected Map<String,File> cdnProductCertsChannelMap = new HashMap<String,File>();	// map generated from cdn/product-certs.json of all the channels to product cert files (e.g. key=jb-ewp-5-i386-server-5-rpm value=/jbewp-5.0/Server-JBEWP-i386-bca12d9b039b-184.pem)
//	protected Map<String,ProductCert> cdnProductCertsChannelToProductCertMap = new HashMap<String,ProductCert>();	// map generated from cdn/product-certs.json of all the channels to product cert files (e.g. key=jb-ewp-5-i386-server-5-rpm value=/jbewp-5.0/Server-JBEWP-i386-bca12d9b039b-184.pem)
	protected Map<String,List<String>> cdnProductBaselineProductIdMap = new HashMap<String,List<String>>();	// map of all the productIds to list of channels (e.g. key=69 value=[rhn-tools-rhel-x86_64-server-5, rhn-tools-rhel-x86_64-server-5-debug-info])	// inverse of cdnProductBaselineChannelMap
//	protected List<ProductCert> originallyInstalledRedHatProductCerts = new ArrayList<ProductCert>();
//	protected String migrationFromFact				= "migration.migrated_from";
//	protected String migrationSystemIdFact			= "migration.classic_system_id";
//	protected String migrationDateFact				= "migration.migration_date";
//	protected String originalProductCertDir			= null;
//	protected String backupProductCertDir			= "/tmp/backupOfProductCertDir";
//	protected String nonDefaultProductCertDir		= "/tmp/migratedProductCertDir";
//	protected String machineInstNumberFile			= "/etc/sysconfig/rhn/install-num";
//	protected String backupMachineInstNumberFile	= machineInstNumberFile+".bak";
	protected String rhnBaseChannel = null;
	protected List<String> rhnAvailableChildChannels = new ArrayList<String>();
//	static public String installNumTool = "install-num-migrate-to-rhsm";
//	static public String rhnMigrateTool = "rhn-migrate-classic-to-rhsm";
//	protected String originalServerHostname;
//	protected String originalServerPort;
//	protected String originalServerPrefix;
	
	
	
	/**
	 * Extract the suffix pem filename from the long mapped filename.
	 * @param productCertFilename example: Server-ClusterStorage-ppc-a3fea9e1dde3-90.pem
	 * @return example: 90.pem
	 */
	public static String getPemFileNameFromProductCertFilename(String productCertFilename) {
		// Server-ClusterStorage-ppc-a3fea9e1dde3-90.pem
		return productCertFilename.split("-")[productCertFilename.split("-").length-1];
	}
	
	/**
	 * Extract the productId from the long mapped filename.
	 * @param productCertFilename example: Server-ClusterStorage-ppc-a3fea9e1dde3-90.pem
	 * @return example: 90
	 */
	public static String getProductIdFromProductCertFilename(String productCertFilename) {
		// Server-ClusterStorage-ppc-a3fea9e1dde3-90.pem
		String pemFilename = getPemFileNameFromProductCertFilename(productCertFilename);
		return pemFilename.replace(".pem", "");
	}
	
	
	
	// Data Providers ***********************************************************************
	
	
	
	@DataProvider(name="RhnChannelFromProductBaselineData")
	public Object[][] getRhnChannelFromProductBaselineDataAs2dArray() throws JSONException {
		return TestNGUtils.convertListOfListsTo2dArray(getRhnChannelFromProductBaselineDataAsListOfLists());
	}
	public List<List<Object>> getRhnChannelFromProductBaselineDataAsListOfLists() throws JSONException {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
				
		for (String productId : cdnProductBaselineProductIdMap.keySet()) {
			for (String rhnChannel : cdnProductBaselineProductIdMap.get(productId)) {

				// filter out all RHN Channels not associated with this release  (e.g., assume that an rhn channel containing "-5-" or ends in "-5" is only applicable to rhel5 
				if (!(rhnChannel.contains("-"+clienttasks.redhatReleaseX+"-") || rhnChannel.endsWith("-"+clienttasks.redhatReleaseX))) continue;
				
				// skip on these RHN Channels that slip through this ^ filter
				// [root@jsefler-onprem-5server tmp]# grep jboss /tmp/product-baseline.json | grep -v Label
	            // "rhel-x86_64-server-6-rhevm-3-jboss-5", 
	            // "rhel-x86_64-server-6-rhevm-3-jboss-5-beta", 
	            // "rhel-x86_64-server-6-rhevm-3-jboss-5-debuginfo", 
	            // "rhel-x86_64-server-6-rhevm-3-jboss-5-beta-debuginfo"
				List<String> rhnChannelExceptions = Arrays.asList("rhel-x86_64-server-6-rhevm-3-jboss-5","rhel-x86_64-server-6-rhevm-3-jboss-5-beta","rhel-x86_64-server-6-rhevm-3-jboss-5-debuginfo","rhel-x86_64-server-6-rhevm-3-jboss-5-beta-debuginfo");
				if (rhnChannelExceptions.contains(rhnChannel) && !clienttasks.redhatReleaseX.equals(/*"5"*/"6")) continue;
				
				// bugzillas
				Set<String> bugIds = new HashSet<String>();
				if (rhnChannel.contains("-rhev-agent-") && clienttasks.redhatReleaseX.equals("5")/* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
					// Bug 786278 - RHN Channels for -rhev- and -vt- in the channel-cert-mapping.txt are not mapped to a productId
					bugIds.add("786278");
				}
				if (rhnChannel.contains("-vt-")/* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
					// Bug 786278 - RHN Channels for -rhev- and -vt- in the channel-cert-mapping.txt are not mapped to a productId
					bugIds.add("786278");
				}
				if (rhnChannel.startsWith("rhel-i386-rhev-agent-") /* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
					// Bug 816364 - channel-cert-mapping.txt is missing a mapping for product 150 "Red Hat Enterprise Virtualization" on i386
					bugIds.add("816364");
				}
				if (rhnChannel.endsWith("-beta") && clienttasks.redhatReleaseX.equals("5")/* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
					// Bug 786203 - all RHN *beta Channels in channel-cert-mapping.txt are mapped to "none" instead of valid productId
					bugIds.add("786203");
				}			
				if (rhnChannel.endsWith("-debuginfo") && clienttasks.redhatReleaseX.equals("5")) { 
					// Bug 786140 - RHN Channels for "*debuginfo" are missing from the channel-cert-mapping.txt 
					bugIds.add("786140");
				}
				if (rhnChannel.startsWith("rhel-x86_64-server-6-rhevh") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-rhevm-3") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-rhevm-3-jboss-5") ||
					rhnChannel.startsWith("rhel-x86_64-server-sjis-6") ||
					rhnChannel.startsWith("rhel-x86_64-server-sap-6") ||
					/*
					rhnChannel.startsWith("rhel-x86_64-server-optional-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-sfs-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-ha-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-rs-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-server-lb-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-workstation-sfs-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-workstation-6-htb") ||
					rhnChannel.startsWith("rhel-x86_64-workstation-optional-6-htb") ||
					*/
					rhnChannel.startsWith("rhel-x86_64-rhev-mgmt-agent-6") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-cf-tools-1") || rhnChannel.startsWith("rhel-i386-server-6-cf-tools-1") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-cf-ae-1") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-cf-ce-1") ||
					rhnChannel.startsWith("rhel-x86_64-server-6-cf-se-1") ||	
					rhnChannel.startsWith("sam-rhel-x86_64-server-6-htb") || rhnChannel.startsWith("sam-rhel-x86_64-server-6-beta")) { 
					// Bug 799152 - subscription-manager-migration-data is missing some product certs for RHN Channels in product-baseline.json
					bugIds.add("799152");
				}
				if (rhnChannel.equals("rhel-s390x-server-6") ||
					rhnChannel.equals("rhel-s390x-server-optional-6") ||
					rhnChannel.equals("rhel-s390x-server-supplementary-6")) { 
					// Bug 799103 - no mapping for s390x product cert included in the subscription-manager-migration-data
					bugIds.add("799103");
				}
				if (rhnChannel.equals("sam-rhel-x86_64-server-6") ||
					rhnChannel.equals("sam-rhel-x86_64-server-6-debuginfo")) { 
					// Bug 815433 - sam-rhel-x86_64-server-6-beta channel mapping needs replacement in channel-cert-mapping.txt 
					bugIds.add("815433");
				}
				if (productId.equals("167")) {
					// Bug 811633 - channel-cert-mapping.txt is missing a mapping for product 167 "Red Hat CloudForms"
					bugIds.add("811633");
				}
				if (productId.equals("183") || productId.equals("184") || productId.equals("185")) if (clienttasks.redhatReleaseX.equals("6")) {
					// Bug 825603 - channel-cert-mapping.txt is missing a mapping for JBoss product ids 183,184,185
					bugIds.add("825603");
				}
				if (rhnChannel.contains("-dts-")) if (clienttasks.redhatReleaseX.equals("6")) { 
					// Bug 820749 - channel-cert-mapping.txt is missing a mapping for product "Red Hat Developer Toolset"
					//TODO UNCOMMENT AFTER BUG 884688 IS FIXED bugIds.add("820749");
				}
				if (rhnChannel.contains("-dts-")) if (clienttasks.redhatReleaseX.equals("5")) { 
					// Bug 852551 - channel-cert-mapping.txt is missing a mapping for product "Red Hat Developer Toolset"
					bugIds.add("852551");
				}
				if (productId.equals("195")) if (clienttasks.redhatReleaseX.equals("5")) {
					// Bug 869008 - mapping for productId 195 "Red Hat Developer Toolset (for RHEL for IBM POWER)" is missing
					bugIds.add("869008");
				}
				if (productId.equals("195")) if (clienttasks.redhatReleaseX.equals("6")) {
					// Bug 875802 - mapping for productId 195 "Red Hat Developer Toolset (for RHEL for IBM POWER)" is missing
					bugIds.add("875802");
				}
				if (productId.equals("181")) {
					// Bug 840148 - missing product cert corresponding to "Red Hat EUCJP Support (for RHEL Server)"
					bugIds.add("840148");
					// Bug 847069 - Add certificates for rhel-x86_64-server-eucjp-5* channels.
					bugIds.add("847069");
				}
				if (rhnChannel.startsWith("rhel-i386-rhev-agent-5-")) { 
					// Bug 849305 - rhel-i386-rhev-agent-5-* maps in channel-cert-mapping.txt do not match CDN Product Baseline
					bugIds.add("849305");
				}
				if (rhnChannel.startsWith("jbappplatform-4.2-els-")) { 
					// Bug 861470 - JBoss Enterprise Application Platform - ELS (jbappplatform-4.2.0) 192.pem product certs are missing from subscription-manager-migration-data
					bugIds.add("861470");
				}
				if (rhnChannel.startsWith("rhel-x86_64-rhev-mgmt-agent-5")) { 
					// Bug 861420 - Red Hat Enterprise Virtualization (rhev-3.0) 150.pem product certs are missing from subscription-manager-migration-data
					bugIds.add("861420");
				}
				if (rhnChannel.equals("rhel-x86_64-rhev-mgmt-agent-5-debuginfo") || rhnChannel.equals("rhel-x86_64-rhev-mgmt-agent-5-beta-debuginfo")) { 
					// Bug 865566 - RHEL-5/channel-cert-mapping.txt is missing a mapping for two rhev debuginfo channels
					bugIds.add("865566");
				}
				if (productId.equals("167") || productId.equals("155") || productId.equals("186") || productId.equals("191") || productId.equals("188") || productId.equals("172")) if (clienttasks.redhatReleaseX.equals("6")) {
					// Bug 872959 - many product certs and their RHN Channel mappings are missing from the RHEL64 subscription-manager-migration-data
					bugIds.add("872959");
				}
				if (productId.equals("197") || productId.equals("198")) {
					// Bug 875760 - some openshift product certs and their RHN Channel mappings are missing from the RHEL64 subscription-manager-migration-data
					bugIds.add("875760");
				}
				if (rhnChannel.startsWith("rhel-x86_64-server-6-ost-folsom")) { 
					// Bug 884657 - the server-6-ost-folsom channels need to be mapped into channel-cert-mapping.txt
					bugIds.add("884657");
				}
				if (rhnChannel.equals("rhel-x86_64-hpc-node-dts-6") || rhnChannel.equals("rhel-x86_64-hpc-node-dts-6-debuginfo")) {
					// Bug 820749 - channel-cert-mapping.txt is missing a mapping for product "Red Hat Developer Toolset"
					bugIds.add("820749");
					// Bug 884688 - RHN channel "rhel-x86_64-hpc-node-dts-6" is mapped to 177, but the product cert 177.pem is missing 
					bugIds.add("884688");
				}
				if (rhnChannel.startsWith("rhel-x86_64-server-6-rhevm-3.1")) { 
					// Bug 888791 - product cert mappings for RHN Channels rhel-x86_64-server-6-rhevm-3.1* are missing
					bugIds.add("888791");
				}
				if (rhnChannel.startsWith("rhel-i386-server-sjis-6")) {	// rhel-i386-server-sjis-6 rhel-i386-server-sjis-6-debuginfo rhel-i386-server-sjis-6-beta rhel-i386-server-sjis-6-beta-debuginfo
					// Bug 896195 - rhel-i386-server-sjis-6 channels are not yet mapped in channel-cert-mapping.txt
					bugIds.add("896195");
				}
				if (rhnChannel.contains("-dts-5-beta")) {	// rhel-i386-server-dts-5-beta rhel-i386-server-dts-5-beta-debuginfo rhel-x86_64-server-dts-5-beta rhel-x86_64-server-dts-5-beta-debuginfo
					// Bug 966683 - the dts beta channels should be mapped to the RHB product cert 180
					bugIds.add("966683");
				}
				if (rhnChannel.contains("-rhev-mgmt-agent-5")) {	// rhel-x86_64-rhev-mgmt-agent-5 rhel-x86_64-rhev-mgmt-agent-5-beta
					// Bug 966696 - Red Hat Enterprise Virtualization (rhev-3.0) 150.pem product certs are missing from subscription-manager-migration-data 
					bugIds.add("966696");
				}
				
				// Object bugzilla, String productBaselineRhnChannel, String productBaselineProductId
				BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
				ll.add(Arrays.asList(new Object[]{blockedByBzBug,	rhnChannel,	productId}));
			}
		}
		
		return ll;
	}
	
	
	@DataProvider(name="RhnChannelFromProductCertsData")
	public Object[][] getRhnChannelFromProductCertsDataAs2dArray() throws JSONException {
		return TestNGUtils.convertListOfListsTo2dArray(getRhnChannelFromProductCertsDataAsListOfLists());
	}
	public List<List<Object>> getRhnChannelFromProductCertsDataAsListOfLists() throws JSONException {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
			for (String rhnChannel : cdnProductCertsChannelMap.keySet()) {
				File productCertFile = cdnProductCertsChannelMap.get(rhnChannel);
				
				// filter out all RHN Channels that map to a release of rhel that does not equal the current release (clienttasks.redhatReleaseXY)
				String regex="/rhel-(\\d+)\\.(\\d+).*";	// match it against example like: /rhel-5.9-beta/Server-Server-x86_64-4b918bda53c0-69.pem  /rhel-6.3/EUS-HighAvailability-x86_64-51676442768e-84.pem  /mrg-2.1/Server-MRG-R-x86_64-e1d154eaac1f-172.pem
				Pattern pattern = Pattern.compile(regex/*,Pattern.DOTALL, Pattern.MULTILINE*/);
				Matcher matcher = pattern.matcher(productCertFile.getPath());
				if (matcher.find()) {
					if (!clienttasks.redhatReleaseXY.equals(matcher.group(1)+"."+matcher.group(2))) {
						log.fine("Skipping rhnChannel '"+rhnChannel+"' mapping to '"+productCertFile.getPath()+"' because it does not apply to this release of RHEL"+clienttasks.redhatReleaseXY);
						continue;
					}
				}


// UNDER CONSTRUCTION
//				// filter out all RHN Channels not associated with this release  (e.g., assume that an rhn channel containing "-5-" or ends in "-5" is only applicable to rhel5 
//				if (!(rhnChannel.contains("-"+clienttasks.redhatReleaseX+"-") || rhnChannel.endsWith("-"+clienttasks.redhatReleaseX))) continue;
//				
//				// skip on these RHN Channels that slip through this ^ filter
//				// [root@jsefler-onprem-5server tmp]# grep jboss /tmp/product-baseline.json | grep -v Label
//	            // "rhel-x86_64-server-6-rhevm-3-jboss-5", 
//	            // "rhel-x86_64-server-6-rhevm-3-jboss-5-beta", 
//	            // "rhel-x86_64-server-6-rhevm-3-jboss-5-debuginfo", 
//	            // "rhel-x86_64-server-6-rhevm-3-jboss-5-beta-debuginfo"
//				List<String> rhnChannelExceptions = Arrays.asList("rhel-x86_64-server-6-rhevm-3-jboss-5","rhel-x86_64-server-6-rhevm-3-jboss-5-beta","rhel-x86_64-server-6-rhevm-3-jboss-5-debuginfo","rhel-x86_64-server-6-rhevm-3-jboss-5-beta-debuginfo");
//				if (rhnChannelExceptions.contains(rhnChannel) && !clienttasks.redhatReleaseX.equals(/*"5"*/"6")) continue;
				
				// bugzillas
				Set<String> bugIds = new HashSet<String>();
//				if (rhnChannel.contains("-rhev-agent-") && clienttasks.redhatReleaseX.equals("5")/* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
//					// Bug 786278 - RHN Channels for -rhev- and -vt- in the channel-cert-mapping.txt are not mapped to a productId
//					bugIds.add("786278");
//				}
//				if (rhnChannel.contains("-vt-")/* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
//					// Bug 786278 - RHN Channels for -rhev- and -vt- in the channel-cert-mapping.txt are not mapped to a productId
//					bugIds.add("786278");
//				}
//				if (rhnChannel.startsWith("rhel-i386-rhev-agent-") /* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
//					// Bug 816364 - channel-cert-mapping.txt is missing a mapping for product 150 "Red Hat Enterprise Virtualization" on i386
//					bugIds.add("816364");
//				}
//				if (rhnChannel.endsWith("-beta") && clienttasks.redhatReleaseX.equals("5")/* && channelsToProductCertFilenamesMap.get(rhnChannel).equalsIgnoreCase("none")*/) { 
//					// Bug 786203 - all RHN *beta Channels in channel-cert-mapping.txt are mapped to "none" instead of valid productId
//					bugIds.add("786203");
//				}			
//				if (rhnChannel.endsWith("-debuginfo") && clienttasks.redhatReleaseX.equals("5")) { 
//					// Bug 786140 - RHN Channels for "*debuginfo" are missing from the channel-cert-mapping.txt 
//					bugIds.add("786140");
//				}
//				if (rhnChannel.startsWith("rhel-x86_64-server-6-rhevh") ||
//					rhnChannel.startsWith("rhel-x86_64-server-6-rhevm-3") ||
//					rhnChannel.startsWith("rhel-x86_64-server-6-rhevm-3-jboss-5") ||
//					rhnChannel.startsWith("rhel-x86_64-server-sjis-6") ||
//					rhnChannel.startsWith("rhel-x86_64-server-sap-6") ||
//					/*
//					rhnChannel.startsWith("rhel-x86_64-server-optional-6-htb") ||
//					rhnChannel.startsWith("rhel-x86_64-server-sfs-6-htb") ||
//					rhnChannel.startsWith("rhel-x86_64-server-ha-6-htb") ||
//					rhnChannel.startsWith("rhel-x86_64-server-rs-6-htb") ||
//					rhnChannel.startsWith("rhel-x86_64-server-6-htb") ||
//					rhnChannel.startsWith("rhel-x86_64-server-lb-6-htb") ||
//					rhnChannel.startsWith("rhel-x86_64-workstation-sfs-6-htb") ||
//					rhnChannel.startsWith("rhel-x86_64-workstation-6-htb") ||
//					rhnChannel.startsWith("rhel-x86_64-workstation-optional-6-htb") ||
//					*/
//					rhnChannel.startsWith("rhel-x86_64-rhev-mgmt-agent-6") ||
//					rhnChannel.startsWith("rhel-x86_64-server-6-cf-tools-1") || rhnChannel.startsWith("rhel-i386-server-6-cf-tools-1") ||
//					rhnChannel.startsWith("rhel-x86_64-server-6-cf-ae-1") ||
//					rhnChannel.startsWith("rhel-x86_64-server-6-cf-ce-1") ||
//					rhnChannel.startsWith("rhel-x86_64-server-6-cf-se-1") ||	
//					rhnChannel.startsWith("sam-rhel-x86_64-server-6-htb") || rhnChannel.startsWith("sam-rhel-x86_64-server-6-beta")) { 
//					// Bug 799152 - subscription-manager-migration-data is missing some product certs for RHN Channels in product-baseline.json
//					bugIds.add("799152");
//				}
//				if (rhnChannel.equals("rhel-s390x-server-6") ||
//					rhnChannel.equals("rhel-s390x-server-optional-6") ||
//					rhnChannel.equals("rhel-s390x-server-supplementary-6")) { 
//					// Bug 799103 - no mapping for s390x product cert included in the subscription-manager-migration-data
//					bugIds.add("799103");
//				}
//				if (rhnChannel.equals("sam-rhel-x86_64-server-6") ||
//					rhnChannel.equals("sam-rhel-x86_64-server-6-debuginfo")) { 
//					// Bug 815433 - sam-rhel-x86_64-server-6-beta channel mapping needs replacement in channel-cert-mapping.txt 
//					bugIds.add("815433");
//				}
//				if (productId.equals("167")) {
//					// Bug 811633 - channel-cert-mapping.txt is missing a mapping for product 167 "Red Hat CloudForms"
//					bugIds.add("811633");
//				}
//				if (productId.equals("183") || productId.equals("184") || productId.equals("185")) if (clienttasks.redhatReleaseX.equals("6")) {
//					// Bug 825603 - channel-cert-mapping.txt is missing a mapping for JBoss product ids 183,184,185
//					bugIds.add("825603");
//				}
//				if (rhnChannel.contains("-dts-")) if (clienttasks.redhatReleaseX.equals("6")) { 
//					// Bug 820749 - channel-cert-mapping.txt is missing a mapping for product "Red Hat Developer Toolset"
//					//TODO UNCOMMENT AFTER BUG 884688 IS FIXED bugIds.add("820749");
//				}
//				if (rhnChannel.contains("-dts-")) if (clienttasks.redhatReleaseX.equals("5")) { 
//					// Bug 852551 - channel-cert-mapping.txt is missing a mapping for product "Red Hat Developer Toolset"
//					bugIds.add("852551");
//				}
//				if (productId.equals("195")) if (clienttasks.redhatReleaseX.equals("5")) {
//					// Bug 869008 - mapping for productId 195 "Red Hat Developer Toolset (for RHEL for IBM POWER)" is missing
//					bugIds.add("869008");
//				}
//				if (productId.equals("195")) if (clienttasks.redhatReleaseX.equals("6")) {
//					// Bug 875802 - mapping for productId 195 "Red Hat Developer Toolset (for RHEL for IBM POWER)" is missing
//					bugIds.add("875802");
//				}
//				if (productId.equals("181")) {
//					// Bug 840148 - missing product cert corresponding to "Red Hat EUCJP Support (for RHEL Server)"
//					bugIds.add("840148");
//					// Bug 847069 - Add certificates for rhel-x86_64-server-eucjp-5* channels.
//					bugIds.add("847069");
//				}
//				if (rhnChannel.startsWith("rhel-i386-rhev-agent-5-")) { 
//					// Bug 849305 - rhel-i386-rhev-agent-5-* maps in channel-cert-mapping.txt do not match CDN Product Baseline
//					bugIds.add("849305");
//				}
//				if (rhnChannel.startsWith("jbappplatform-4.2-els-")) { 
//					// Bug 861470 - JBoss Enterprise Application Platform - ELS (jbappplatform-4.2.0) 192.pem product certs are missing from subscription-manager-migration-data
//					bugIds.add("861470");
//				}
//				if (rhnChannel.startsWith("rhel-x86_64-rhev-mgmt-agent-5")) { 
//					// Bug 861420 - Red Hat Enterprise Virtualization (rhev-3.0) 150.pem product certs are missing from subscription-manager-migration-data
//					bugIds.add("861420");
//				}
//				if (rhnChannel.equals("rhel-x86_64-rhev-mgmt-agent-5-debuginfo") || rhnChannel.equals("rhel-x86_64-rhev-mgmt-agent-5-beta-debuginfo")) { 
//					// Bug 865566 - RHEL-5/channel-cert-mapping.txt is missing a mapping for two rhev debuginfo channels
//					bugIds.add("865566");
//				}
//				if (productId.equals("167") || productId.equals("155") || productId.equals("186") || productId.equals("191") || productId.equals("188") || productId.equals("172")) if (clienttasks.redhatReleaseX.equals("6")) {
//					// Bug 872959 - many product certs and their RHN Channel mappings are missing from the RHEL64 subscription-manager-migration-data
//					bugIds.add("872959");
//				}
//				if (productId.equals("197") || productId.equals("198")) {
//					// Bug 875760 - some openshift product certs and their RHN Channel mappings are missing from the RHEL64 subscription-manager-migration-data
//					bugIds.add("875760");
//				}
//				if (rhnChannel.startsWith("rhel-x86_64-server-6-ost-folsom")) { 
//					// Bug 884657 - the server-6-ost-folsom channels need to be mapped into channel-cert-mapping.txt
//					bugIds.add("884657");
//				}
//				if (rhnChannel.equals("rhel-x86_64-hpc-node-dts-6") || rhnChannel.equals("rhel-x86_64-hpc-node-dts-6-debuginfo")) {
//					// Bug 820749 - channel-cert-mapping.txt is missing a mapping for product "Red Hat Developer Toolset"
//					bugIds.add("820749");
//					// Bug 884688 - RHN channel "rhel-x86_64-hpc-node-dts-6" is mapped to 177, but the product cert 177.pem is missing 
//					bugIds.add("884688");
//				}
//				if (rhnChannel.startsWith("rhel-x86_64-server-6-rhevm-3.1")) { 
//					// Bug 888791 - product cert mappings for RHN Channels rhel-x86_64-server-6-rhevm-3.1* are missing
//					bugIds.add("888791");
//				}
//				if (rhnChannel.startsWith("rhel-i386-server-sjis-6")) {	// rhel-i386-server-sjis-6 rhel-i386-server-sjis-6-debuginfo rhel-i386-server-sjis-6-beta rhel-i386-server-sjis-6-beta-debuginfo
//					// Bug 896195 - rhel-i386-server-sjis-6 channels are not yet mapped in channel-cert-mapping.txt
//					bugIds.add("896195");
//				}
				
				// Object bugzilla, String productBaselineRhnChannel, String productBaselineProductId
				BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
				ll.add(Arrays.asList(new Object[]{blockedByBzBug,	rhnChannel,	productCertFile}));
			}

		
		return ll;
	}
	
	
	@DataProvider(name="getRhnClassicBaseAndAvailableChildChannelsData")
	public Object[][] getRhnClassicBaseAndAvailableChildChannelsDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getRhnClassicBaseAndAvailableChildChannelsDataAsListOfLists());
	}
	protected List<List<Object>> getRhnClassicBaseAndAvailableChildChannelsDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>(); if (!isSetupBeforeSuiteComplete) return ll;
		if (clienttasks==null) return ll;
		
		// add the base channel
		if (rhnBaseChannel!=null) ll.add(Arrays.asList(new Object[]{null,	rhnBaseChannel}));
		
		// add the child channels
		for (String rhnAvailableChildChannel : rhnAvailableChildChannels) {
			
			// bugzillas
			Set<String> bugIds = new HashSet<String>();
			if (rhnAvailableChildChannel.matches("sam-rhel-.+-server-6-beta.*")) {	// sam-rhel-x86_64-server-6-beta-debuginfo
				// Bug 819092 - channels for sam-rhel-<ARCH>-server-6-beta-* are not yet mapped to product certs in rcm/rhn-definitions.git
				bugIds.add("819092");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-rhui-2(-.*|$)")) {	// rhel-x86_64-server-6-rhui-2 rhel-x86_64-server-6-rhui-2-debuginfo
				// Bug 819089 - channels for rhel-<ARCH>-rhui-2-* are not yet mapped to product certs in rcm/rhn-definitions.git
				bugIds.add("819089");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-6-mrg-.+")) {	// rhel-x86_64-server-6-mrg-grid-execute-2-debuginfo rhel-x86_64-server-6-mrg-messaging-2-debuginfo
				// Bug 819088 - channels for rhel-<ARCH>-server-6-mrg-* are not yet mapped to product certs in rcm/rhn-definitions.git 
				bugIds.add("819088");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-hpc-node-6-mrg-.*")) {	// rhel-x86_64-hpc-node-6-mrg-grid-execute-2  rhel-x86_64-hpc-node-6-mrg-grid-execute-2-debuginfo  rhel-x86_64-hpc-node-6-mrg-management-2  rhel-x86_64-hpc-node-6-mrg-management-2-debuginfo
				// Bug 825608 - channels for rhel-<ARCH>-hpc-node-6-mrg-* are not yet mapped to product certs in rcm/rhn-definitions.git
				bugIds.add("825608");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-v2vwin-6(-.*|$)")) {	// rhel-x86_64-server-v2vwin-6-beta-debuginfo
				// Bug 817791 - v2vwin content does not exist in CDN
				bugIds.add("817791");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-fastrack-6(-.*|$)")) {	// rhel-x86_64-server-ha-fastrack-6-debuginfo
				// Bug 818202 - Using subscription-manager, some repositories like fastrack are not available as they are in rhn.
				bugIds.add("818202");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-eucjp-6(-.+|$)")) {	// rhel-x86_64-server-eucjp-6 rhel-x86_64-server-eucjp-6-beta etc.
				// Bug 840148 - missing product cert corresponding to "Red Hat EUCJP Support (for RHEL Server)"
				bugIds.add("840148");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-fastrack-5(-.*|$)")) {	// rhel-x86_64-server-fastrack-5 rhel-x86_64-server-fastrack-5-debuginfo
				// Bug 818202 - Using subscription-manager, some repositories like fastrack are not available as they are in rhn.
				bugIds.add("818202");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-5-cf-tools-1(-beta)?-debuginfo")) {	// rhel-x86_64-server-5-cf-tools-1-beta-debuginfo, rhel-x86_64-server-5-cf-tools-1-debuginfo
				// Bug 840099 - debug info channels for rhel-x86_64-server-5-cf-tools are not yet mapped to product certs in rcm/rcm-metadata.git
				bugIds.add("840099");	// CLOSED as a dup of bug 818202
				bugIds.add("818202");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-5-mrg-.*")) {	// rhel-x86_64-server-5-mrg-grid-1 rhel-x86_64-server-5-mrg-grid-1-beta rhel-x86_64-server-5-mrg-grid-2 rhel-x86_64-server-5-mrg-grid-execute-1 rhel-x86_64-server-5-mrg-grid-execute-1-beta rhel-x86_64-server-5-mrg-grid-execute-2 etc.
				// Bug 840102 - channels for rhel-<ARCH>-server-5-mrg-* are not yet mapped to product certs in rcm/rcm-metadata.git 
				bugIds.add("840102");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-hpc-5(-.*|$)")) {	// rhel-x86_64-server-hpc-5-beta
				// Bug 840103 - channel for rhel-x86_64-server-hpc-5-beta is not yet mapped to product cert in rcm/rcm-metadata.git
				bugIds.add("840103");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-rhev-hdk-2-5(-.+|$)")) {	// rhel-x86_64-server-rhev-hdk-2-5 rhel-x86_64-server-rhev-hdk-2-5-beta
				// Bug 840108 - channels for rhel-<ARCH>-rhev-hdk-2-5-* are not yet mapped to product certs in rcm/rhn-definitions.git
				bugIds.add("840108");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-productivity-5-beta(-.+|$)")) {	// rhel-x86_64-server-productivity-5-beta rhel-x86_64-server-productivity-5-beta-debuginfo
				// Bug 840136 - various rhel channels are not yet mapped to product certs in rcm/rcm-metadata.git
				bugIds.add("840136");	// CLOSED in favor of bug 840099
				bugIds.add("840099");	// CLOSED as a dup of bug 818202
				bugIds.add("818202");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-rhsclient-5(-.+|$)")) {	// rhel-x86_64-server-rhsclient-5 rhel-x86_64-server-rhsclient-5-debuginfo
				// Bug 840136 - various rhel channels are not yet mapped to product certs in rcm/rcm-metadata.git
				bugIds.add("840136");	// CLOSED in favor of bug 840099
				bugIds.add("840099");	// CLOSED as a dup of bug 818202
				bugIds.add("818202");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-xfs-5(-.+|$)")) {	// rhel-x86_64-server-xfs-5 rhel-x86_64-server-xfs-5-beta
				// Bug 840136 - various rhel channels are not yet mapped to product certs in rcm/rcm-metadata.git
				bugIds.add("840136");	// CLOSED in favor of bug 840099
				bugIds.add("840099");	// CLOSED as a dup of bug 818202
				bugIds.add("818202");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-5-shadow(-.+|$)")) {	// rhel-x86_64-server-5-shadow-debuginfo
				// Bug 840136 - various rhel channels are not yet mapped to product certs in rcm/rcm-metadata.git
				bugIds.add("840136");	// CLOSED in favor of bug 840099
				bugIds.add("840099");	// CLOSED as a dup of bug 818202
				bugIds.add("818202");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-eucjp-5(-.+|$)")) {	// rhel-x86_64-server-eucjp-5 rhel-x86_64-server-eucjp-5-beta etc.
				// Bug 840148 - missing product cert corresponding to "Red Hat EUCJP Support (for RHEL Server)"
				bugIds.add("840148");
				// Bug 847069 - Add certificates for rhel-x86_64-server-eucjp-5* channels.
				bugIds.add("847069");
			}
			if (rhnAvailableChildChannel.startsWith("rhx-")) {	// rhx-alfresco-enterprise-2.0-rhel-x86_64-server-5 rhx-amanda-enterprise-backup-2.6-rhel-x86_64-server-5 etcetera
				// Bug 840111 - various rhx channels are not yet mapped to product certs in rcm/rcm-metadata.git 
				bugIds.add("840111");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-rhsclient-6(-.*|$)")) {	// rhel-x86_64-server-rhsclient-6 rhel-x86_64-server-rhsclient-6-debuginfo
				// Bug 872980 - channels for rhel-<ARCH>-server-rhsclient-6* are not yet mapped to product certs in rcm/rcm-metadata.git
				bugIds.add("872980");	// CLOSED DUPLICATE of bug 818202
				bugIds.add("818202");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-server-6-ost-folsom(-.*|$)")) {	// rhel-x86_64-server-6-ost-folsom  rhel-x86_64-server-6-ost-folsom-debuginfo
				// Bug 872983 - channels for rhel-<ARCH>-server-6-ost-folsom* are not yet mapped to product certs in rcm/rcm-metadata.git
				bugIds.add("872983");
				// Bug 884657 - the server-6-ost-folsom channels need to be mapped into channel-cert-mapping.txt
				bugIds.add("884657");
			}
			if (rhnAvailableChildChannel.startsWith("rhel-i386-server-sjis-6")) {	// rhel-i386-server-sjis-6 rhel-i386-server-sjis-6-debuginfo rhel-i386-server-sjis-6-beta rhel-i386-server-sjis-6-beta-debuginfo
				// Bug 892711 - rhel-i386-server-sjis-6 channels are available, but not accounted for in product-baseline.json
				bugIds.add("892711");
			}
			if (rhnAvailableChildChannel.matches("rhel-.+-client-dts-5(-.*|$)")) {	// rhel-i386-client-dts-5-debuginfo rhel-i386-client-dts-5-beta-debuginfo rhel-i386-client-dts-5-beta rhel-i386-client-dts-5
				// Bug 969156 - RHN Channels: [] in product-baseline.json is empty for "Red Hat Developer Toolset (for RHEL Client)"
				bugIds.add("969156");
				// Bug 969160 - rhel-*-client-dts-5* channels are not mapped in channel-cert-mapping.txt
				bugIds.add("969160");
			}
			
			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			ll.add(Arrays.asList(new Object[]{blockedByBzBug,	rhnAvailableChildChannel}));
		}
		
		return ll;
	}
	
	
}




// Notes ***********************************************************************

