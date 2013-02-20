package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.EntitlementCert;
import rhsm.data.Manifest;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * Manifest Creation Instructions:
 *   go to rhsm-web for stage, login as a stage_test_# user and create a SAM distributor.
 *   attach subscriptions and then export the manifest.zip to /tmp/manifest.zip
 *   copy the manifest.zip to the sm_manifestsUrl so it will be included in these tests
 */
@Test(groups={"ManifestTests"})
public class ManifestTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************

	@Test(	description="execute rct dump-manifest against all of the test manifest files",
			groups={},
			dataProvider="ManifestFilesData",
			priority=10, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void RCTDumpManifest_Test(Object bugzilla, File manifestFile) throws Exception {
		
		// execute and assert rct dump-manifest MANIFEST_FILE
		SSHCommandResult dumpResult = RemoteFileTasks.runCommandAndAssert(client, "cd "+manifestFile.getParent()+" && rct dump-manifest "+manifestFile, 0);
		Assert.assertEquals(dumpResult.getStdout().trim(), "The manifest has been dumped to the current directory", "stdout from rct dump-manifest");
		
		// get a listing of the files
		List<File> manifestContents = new ArrayList<File>();
		SSHCommandResult findResult = RemoteFileTasks.runCommandAndAssert(client, "find "+manifestFile.getParent(), 0);
		for (String manifestContent : findResult.getStdout().split("\\s*\\n\\s*")) {
			if (manifestContent.isEmpty())  continue;
			if (manifestContent.equals(manifestFile.getParent())) continue;
			if (manifestContent.equals(manifestFile.getPath())) continue;
			manifestContents.add(new File(manifestContent));
		}
		
		// assert the presence of a signature file
		File expectedSignatureFile = new File(manifestFile.getParent()+File.separator+"signature");
		Assert.assertTrue(manifestContents.contains(expectedSignatureFile),"The contents of the rct dump-manifest includes a signature file '"+expectedSignatureFile+"'.");
		
		// assert the presence of the following expected directory/files
		for (String content : Arrays.asList("export/meta.json","export/consumer.json","export/consumer_types","export/rules","export/entitlement_certificates","export/entitlements","export/products")) {
			File expectedExportFile = new File(manifestFile.getParent()+File.separator+content);
			Assert.assertTrue(manifestContents.contains(expectedExportFile),"The contents of the rct dump-manifest includes '"+content+"'.");
		}
	}
	
	@Test(	description="execute rct dump-manifest --destination=/tmp/RCTDumpManifestDestination_Test against all of the test manifest files",
			groups={},
			dataProvider="ManifestFilesData",
			priority=20, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void RCTDumpManifestDestination_Test(Object bugzilla, File manifestFile) throws Exception {
		
		//RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+dumpDestination, Integer.valueOf(0));	// do not do this since we want to use the results in subsequent tests
		String destination = dumpDestination+File.separator+manifestFile.getName().replaceFirst("\\.zip$", "");	// append another subdirectory for uniqueness
		
		// execute and assert rct dump-manifest --destination=/tmp/sm-rctDumpManifestDestination MANIFEST_FILE
		SSHCommandResult dumpManifestResult = RemoteFileTasks.runCommandAndAssert(client, "rct dump-manifest --destination="+destination+" "+manifestFile, 0);
		Assert.assertEquals(dumpManifestResult.getStdout().trim(), String.format("The manifest has been dumped to the %s directory",destination), "stdout from rct dump-manifest with destination");
		
		// get a listing of the files
		List<File> manifestContents = new ArrayList<File>();
		SSHCommandResult findResult = RemoteFileTasks.runCommandAndAssert(client, "find "+destination, 0);
		for (String manifestContent : findResult.getStdout().split("\\s*\\n\\s*")) {
			if (manifestContent.isEmpty())  continue;
			if (manifestContent.equals(destination)) continue;
			manifestContents.add(new File(manifestContent));
		}
		// to save time, store the contents in a map for use by subsequent tests
		manifestFileContentMap.put(manifestFile, manifestContents);
		
		// assert the presence of a signature file
		File expectedSignatureFile = new File(destination+File.separator+"signature");
		Assert.assertTrue(manifestContents.contains(expectedSignatureFile),"The contents of the rct dump-manifest --destination="+destination+" includes a signature file '"+expectedSignatureFile+"'.");
		
		// assert the presence of the following expected directory/files
		for (String content : Arrays.asList("export/meta.json","export/consumer.json","export/consumer_types","export/rules","export/entitlement_certificates","export/entitlements","export/products")) {
			File expectedExportFile = new File(destination+File.separator+content);
			Assert.assertTrue(manifestContents.contains(expectedExportFile),"The contents of the rct dump-manifest --destination="+destination+" includes '"+content+"'.");
		}
	}
	
	
	@Test(	description="execute rct cat-manifest against all of the test manifest files",
			groups={},
			priority=30, //dependsOnMethods={"RCTDumpManifestDestination_Test"}, // to populate manifestFileContentMap
			dataProvider="ManifestFilesData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void RCTCatManifest_Test(Object bugzilla, File manifestFile) throws Exception {
		
		// execute and assert rct cat-manifest MANIFEST_FILE
		SSHCommandResult catManifestResult = RemoteFileTasks.runCommandAndAssert(client, "rct cat-manifest "+manifestFile, 0);
		
		// assert the presence of a banner
		String bannerRegex = "\\+-+\\+\\n\\s*Manifest\\s*\\n\\+-+\\+";
		Assert.assertTrue(Pattern.compile(".*"+bannerRegex+".*",Pattern.DOTALL).matcher(catManifestResult.getStdout()).matches(), "stdout from rct cat-manifest contains a banner matching regex '"+bannerRegex+"'.");
		
		
		// create EntitlementCert objects for all of the entitlements provided by this manifest
		client.runCommandAndWaitWithoutLogging("find "+manifestFileContentMap.get(manifestFile).get(0).getParent()+" -regex \"/.+/[0-9]+.pem\" -exec rct cat-cert {} \\;");
		String rawCertificates = client.getStdout();
		List<EntitlementCert> entitlementCerts = EntitlementCert.parse(rawCertificates);
		if (entitlementCerts.isEmpty()) Assert.fail("Manifest file '"+manifestFile+"' does not provide any entitlements.");
		
		// parse the output from catManifestResult into a Manifest object
		List<Manifest> manifests = Manifest.parse(catManifestResult.getStdout());
		Assert.assertEquals(manifests.size(),1,"Parsed one manifest from '"+manifestFile+"'.");

throw new SkipException("THIS TEST IS STILL UNDER CONSTRUCTION");
//		// prepare a destination to dump the manifest to
//		String destination = "/tmp/sm-rctDumpManifest";
//		RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+destination, Integer.valueOf(0));
//		destination += "/destination";	// append another subdirectory
//		
//		// execute and assert rct dump-manifest --destination=/tmp/sm-rctDumpManifestDestination MANIFEST_FILE
//		SSHCommandResult dumpResult = RemoteFileTasks.runCommandAndAssert(client, "rct dump-manifest --destination="+destination+" "+manifestFile, 0);
//		Assert.assertEquals(dumpResult.getStdout().trim(), String.format("The manifest has been dumped to the %s directory",destination), "stdout from rct dump-manifest with destination");
//		
//		// get a listing of the files
//		List<File> manifestContents = new ArrayList<File>();
//		SSHCommandResult findResult = RemoteFileTasks.runCommandAndAssert(client, "find "+destination, 0);
//		for (String manifestContent : findResult.getStdout().split("\\s*\\n\\s*")) {
//			if (manifestContent.isEmpty())  continue;
//			if (manifestContent.equals(destination)) continue;
//			manifestContents.add(new File(manifestContent));
//		}
//		
//		// assert the presence of a signature file
//		File expectedSignatureFile = new File(destination+File.separator+"signature");
//		Assert.assertTrue(manifestContents.contains(expectedSignatureFile),"The contents of the rct dump-manifest --destination="+destination+" includes a signature file '"+expectedSignatureFile+"'.");
//		
//		// assert the presence of an export directory 
//		File expectedExportDirectory = new File(destination+File.separator+"export");
//		Assert.assertTrue(manifestContents.contains(expectedExportDirectory),"The contents of the rct dump-manifest --destination="+destination+" includes an export directory '"+expectedExportDirectory+"'.");
	}
	
	
	// Candidates for an automated Test:
	// see https://github.com/RedHatQE/rhsm-qe/issues
	
	// Configuration methods ***********************************************************************

	@BeforeClass(groups={"setup"})
	public void fetchManifestsBeforeClass() {
		if (clienttasks==null) return;
		
		RemoteFileTasks.runCommandAndAssert(client, "mkdir -p "+manifestsDir, Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+manifestsDir+"/*", Integer.valueOf(0));
		
		// fetch the manifest files
		if (sm_manifestsUrl.isEmpty()) return; 
		log.info("Fetching test manifests from "+sm_manifestsUrl+" for use by this test class...");
		RemoteFileTasks.runCommandAndAssert(client, "cd "+manifestsDir+" && wget --quiet --recursive --level 1 --no-parent --accept .zip "+sm_manifestsUrl, Integer.valueOf(0)/*,null,"Downloaded: \\d+ files"*/);
		
		// store the manifest files in a list
		SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(client, "find "+manifestsDir+" -name *.zip", 0);
		for (String manifestPathname : result.getStdout().split("\\s*\\n\\s*")) {
			if (manifestPathname.isEmpty()) continue;
			manifestFiles.add(new File(manifestPathname));
		}
	}
	@BeforeClass(groups={"setup"})
	public void cleanDumpDestinationBeforeClass() {
		if (clienttasks==null) return;
		RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+dumpDestination, Integer.valueOf(0));
	}

	
	// Protected methods ***********************************************************************
	
	protected final String dumpDestination = "/tmp/sm-rctDumpManifest";
	protected final String manifestsDir	= "/tmp/sm-testManifestsDir";
	protected List<File> manifestFiles	= new ArrayList<File>();
	protected Map<File,List<File>> manifestFileContentMap = new HashMap<File,List<File>>();
	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="ManifestFilesData")
	public Object[][] getManifestFilesDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getManifestFilesDataAsListOfLists());
	}
	protected List<List<Object>> getManifestFilesDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
		for (File manifestFile : manifestFiles) {
			BlockedByBzBug blockedByBug = null;
			if (manifestFile.getName().equals("stageSamTest20Nov2011.zip")) blockedByBug = new BlockedByBzBug("913187");
			ll.add(Arrays.asList(new Object[] {blockedByBug, manifestFile}));				
		}
		
		return ll;
	}
}
