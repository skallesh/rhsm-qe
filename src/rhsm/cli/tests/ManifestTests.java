package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;

import com.redhat.qe.Assert;
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
			priority=10,
			enabled=true)
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
		
		// assert the presence of an export directory 
		File expectedExportDirectory = new File(manifestFile.getParent()+File.separator+"export");
		Assert.assertTrue(manifestContents.contains(expectedExportDirectory),"The contents of the rct dump-manifest includes an export directory '"+expectedExportDirectory+"'.");
	}
	
	@Test(	description="execute rct dump-manifest --destination=/tmp/RCTDumpManifestDestination_Test against all of the test manifest files",
			groups={},
			dataProvider="ManifestFilesData",
			priority=20,
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void RCTDumpManifestDestination_Test(Object bugzilla, File manifestFile) throws Exception {
		
		String destination = "/tmp/sm-rctDumpManifest";
		RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+destination, Integer.valueOf(0));
		destination += "/destination";	// append another subdirectory
		
		// execute and assert rct dump-manifest --destination=/tmp/sm-rctDumpManifestDestination MANIFEST_FILE
		SSHCommandResult dumpResult = RemoteFileTasks.runCommandAndAssert(client, "rct dump-manifest --destination="+destination+" "+manifestFile, 0);
		Assert.assertEquals(dumpResult.getStdout().trim(), String.format("The manifest has been dumped to the %s directory",destination), "stdout from rct dump-manifest with destination");
		
		// get a listing of the files
		List<File> manifestContents = new ArrayList<File>();
		SSHCommandResult findResult = RemoteFileTasks.runCommandAndAssert(client, "find "+destination, 0);
		for (String manifestContent : findResult.getStdout().split("\\s*\\n\\s*")) {
			if (manifestContent.isEmpty())  continue;
			if (manifestContent.equals(destination)) continue;
			manifestContents.add(new File(manifestContent));
		}
		
		// assert the presence of a signature file
		File expectedSignatureFile = new File(destination+File.separator+"signature");
		Assert.assertTrue(manifestContents.contains(expectedSignatureFile),"The contents of the rct dump-manifest --destination="+destination+" includes a signature file '"+expectedSignatureFile+"'.");
		
		// assert the presence of an export directory 
		File expectedExportDirectory = new File(destination+File.separator+"export");
		Assert.assertTrue(manifestContents.contains(expectedExportDirectory),"The contents of the rct dump-manifest --destination="+destination+" includes an export directory '"+expectedExportDirectory+"'.");
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

	
	// Protected methods ***********************************************************************
	
	protected final String manifestsDir	= "/tmp/sm-testManifestsDir";
	protected List<File> manifestFiles	= new ArrayList<File>();
	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="ManifestFilesData")
	public Object[][] getManifestFilesDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getManifestFilesDataAsListOfLists());
	}
	protected List<List<Object>> getManifestFilesDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
		for (File manifestFile : manifestFiles) {
			ll.add(Arrays.asList(new Object[] {null, manifestFile}));				
		}
		
		return ll;
	}
}
