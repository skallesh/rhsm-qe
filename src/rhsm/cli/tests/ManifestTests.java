package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.Manifest;
import rhsm.data.ManifestSubscription;
import rhsm.data.ProductNamespace;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author jsefler
 *
 * Manifest Creation Instructions:
 *   go to http://access.stage.redhat.com/management/distributors/ for stage, login as a stage_test_# user
 *   navigate to Subscriptions/Subscription Management Applications
 *   click "Register a Subscription Asset Manager Organization" (if not already done)
 *   click "Register a subscription management application" (aka distributor)
 *     give it a name indicative of the subscriptions added and then
 *     attach subscriptions with a low quantity
 *     click "Download manifest" to /tmp/manifest.zip
 *   copy the manifest.zip to the sm_manifestsUrl so it will be included in these tests
 *
 *
 *
 * If getting a manifest from the qa environments instead of stage, you may also need
 * the correct CA cert from it-pnt.  Here are instructions from an email of how to get that...
 * 
 * [root@localhost ~]# openssl s_client -connect access.qa.redhat.com:443 -showcerts
 * 
 * (You'll see a lot of output and then it will wait for you to type
 *  something to send to the server.  Just hit ctrl-C to quit).
 *  
 *  That command will show you the entire certificate chain when attempting to connect
 *  to access.qa.redhat.com. For this machine, I see

2 s:/O=Red Hat/OU=prod/CN=Intermediate Certificate Authority
   i:/C=US/ST=North Carolina/L=Raleigh/O=Red Hat, Inc./OU=Red Hat
IT/CN=Red Hat IT Root CA/emailAddress=infosec@redhat.com

-----BEGIN CERTIFICATE-----
MIID6DCCAtCgAwIBAgIBFDANBgkqhkiG9w0BAQsFADCBpTELMAkGA1UEBhMCVVMx
FzAVBgNVBAgMDk5vcnRoIENhcm9saW5hMRAwDgYDVQQHDAdSYWxlaWdoMRYwFAYD
ABC123ABC123ABC123ABC123ABC123ABC123ABC123ABC123ABC123ABC123ABC1
3GVzoNtkznqv5ySEj4M9WsdBiG6bm4aBYIOE0XKE6QYtlsjTMB9UTXxmlUvDE0wC
z9YYKfC1vLxL2wAgMhOCdKZM+Qlu1stb0B/EF3oxc/iZrhDvJLjijbMpphw=
-----END CERTIFICATE-----

 * at the root of the chain.  Take that output and save it to "qa.pem".
 * You'll need to put that pem file into the right place though.
 * For manifest imports, I believe it's /etc/candlepin/certs/upstream
 * Regards, Alex
 * 
 */
@Test(groups={"ManifestTests"})
public class ManifestTests extends SubscriptionManagerCLITestScript {
	
	// Test methods ***********************************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36647", "RHEL7-51458"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="execute rct dump-manifest against all of the test manifest files",
			groups={"Tier2Tests","blockedByBug-961124"},
			priority=5, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRCTDumpManifestTwice() throws Exception {
		if (manifestFiles.isEmpty()) throw new SkipException("No manifest files have been loaded.");
		
		// execute and assert rct dump-manifest MANIFEST_FILE
		File manifestFile = manifestFiles.get(randomGenerator.nextInt(manifestFiles.size())); // randomly pick a manifest file to test
		SSHCommandResult dumpResult;
		dumpResult = RemoteFileTasks.runCommandAndAssert(client, "cd "+manifestFile.getParent()+" && rct dump-manifest "+manifestFile, 0);
		Assert.assertEquals(dumpResult.getStdout().trim(), "The manifest has been dumped to the current directory", "stdout from rct dump-manifest");
		dumpResult = RemoteFileTasks.runCommandAndAssert(client, "cd "+manifestFile.getParent()+" && rct dump-manifest "+manifestFile, 0);
		Assert.assertEquals(dumpResult.getStdout().trim(), "File \""+manifestFile.getParent()+"/signature\" exists. Use -f to force overwriting the file.", "stdout from rct dump-manifest after a second call to dump-manifest is attempted");
		dumpResult = RemoteFileTasks.runCommandAndAssert(client, "cd "+manifestFile.getParent()+" && rct dump-manifest -f "+manifestFile, 0);
		Assert.assertEquals(dumpResult.getStdout().trim(), "The manifest has been dumped to the current directory", "stdout from rct dump-manifest");
		dumpResult = RemoteFileTasks.runCommandAndAssert(client, "cd "+manifestFile.getParent()+" && rct dump-manifest --force "+manifestFile, 0);
		Assert.assertEquals(dumpResult.getStdout().trim(), "The manifest has been dumped to the current directory", "stdout from rct dump-manifest");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36648", "RHEL7-51459"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="execute rct dump-manifest against all of the test manifest files",
			groups={"Tier2Tests","blockedByBug-919561"},
			dataProvider="ManifestFilesData",
			priority=10, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRCTDumpManifest(Object bugzilla, File manifestFile) throws Exception {
		
		// execute and assert rct dump-manifest MANIFEST_FILE
		RemoteFileTasks.runCommandAndAssert(client, "cd "+manifestFile.getParent()+" && rm -rf consumer_export.zip export signature", 0);
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

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36649", "RHEL7-51478"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="execute rct dump-manifest --destination=/tmp/RCTDumpManifestDestination_Test against all of the test manifest files",
			groups={"Tier2Tests","blockedByBug-919561"},
			dataProvider="ManifestFilesData",
			priority=20, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRCTDumpManifestDestination(Object bugzilla, File manifestFile) throws Exception {
		
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


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36651", "RHEL7-51483"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="execute rct cat-manifest against all of the test manifest files",
			groups={"Tier2Tests","blockedByBug-919561","blockedByBug-913720","blockedByBug-967137","blockedByBug-914717"},
			dependsOnMethods={"testRCTDumpManifestDestination"}, // to populate manifestFileContentMap
			alwaysRun=true,	// run even when there are failures or skips in RCTDumpManifestDestination_Test
			dataProvider="ManifestFilesData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRCTCatManifest(Object bugzilla, File manifestFile) throws Exception {
		verifyRCTCatManifestWithOptions(manifestFile, null);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36650", "RHEL7-74911"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="execute rct cat-manifest with --no-content option against all of the test manifest files",
			groups={"Tier2Tests","blockedByBug-1336883"},
			dependsOnMethods={"testRCTDumpManifestDestination"}, // to populate manifestFileContentMap
			alwaysRun=true,	// run even when there are failures or skips in RCTDumpManifestDestination_Test
			dataProvider="ManifestFilesData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRCTCatManifestNoContent(Object bugzilla, File manifestFile) throws Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.17.10-1")) { // RHEL7.3 commit 860b178e0eb5b91df01c424dad29c521e1c23767  Bug 1336883 - [RFE] Update the 'rct' command to allow not outputting content-set data
			throw new SkipException("This  version of subscription-manager does not include the --no-content option.  (RFE Bug 1336883 was first inc1uded in subscription-manager-1.17.10-1)");
		}
		verifyRCTCatManifestWithOptions(manifestFile, Arrays.asList("--no-content"));
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36645", "RHEL7-51456"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="execute rct cat-manifest against a non-zip file (e.g. the identity cert)",
			groups={"Tier2Tests","blockedByBug-994344"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRCTCatManifestWithNonZipFile() {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg);
		
		// execute and assert rct cat-manifest against the consumer cert pem file
		SSHCommandResult result = client.runCommandAndWait("rct cat-manifest "+clienttasks.consumerCertFile());
		Assert.assertEquals(result.getStdout().trim(), "Manifest zip is invalid.", "Stdout from rct cat-manifest with a non-zip file.");
		Assert.assertEquals(result.getStderr().trim(), "", "Stderr from rct cat-manifest with a non-zip file.");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(1), "Exitcode from rct cat-manifest with a non-zip file.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-36646", "RHEL7-51457"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="execute rct dump-manifest against a non-zip file (e.g. the identity cert)",
			groups={"Tier2Tests","blockedByBug-994344"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRCTDumpManifestWithNonZipFile() {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg);
		
		// execute and assert rct dump-manifest against the consumer cert pem file
		SSHCommandResult result = client.runCommandAndWait("rct dump-manifest "+clienttasks.consumerCertFile());
		Assert.assertEquals(result.getStdout().trim(), "Manifest zip is invalid.", "Stdout from rct dump-manifest with a non-zip file.");
		Assert.assertEquals(result.getStderr().trim(), "", "Stderr from rct dump-manifest with a non-zip file.");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(1), "Exitcode from rct dump-manifest with a non-zip file.");
	}
	
	
	
	
	
	
	
	
	
	// Candidates for an automated Test:
	// see https://github.com/RedHatQE/rhsm-qe/issues
	
	// Configuration methods ***********************************************************************

	@BeforeClass(groups={"setup"})
	public void fetchManifestsBeforeClass() {
		if (clienttasks==null) return;
		
		// fetch the manifest files
		RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+manifestsDir+" && mkdir -p "+manifestsDir, Integer.valueOf(0));
		if (!sm_manifestsUrl.isEmpty()) {
			log.info("Fetching test manifests from "+sm_manifestsUrl+" for use by this test class...");
			RemoteFileTasks.runCommandAndAssert(client, "cd "+manifestsDir+" && wget --quiet --recursive --level 1 --no-parent --accept .zip "+sm_manifestsUrl, Integer.valueOf(0)/*,null,"Downloaded: \\d+ files"*/);
		}
		
		// store the manifest files in a list
		SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(client, "find "+manifestsDir+" -name \"*.zip\"", 0);
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
	
	
	protected void verifyRCTCatManifestWithOptions(File manifestFile, List<String> options) throws Exception {
		
		// flatten the list of options into a single string for the runCommand
		String optionsAsString = " ";
		if (options!=null) for (String option : options) optionsAsString += option+" ";
		
		// execute and assert rct cat-manifest MANIFEST_FILE
		SSHCommandResult catManifestResult = RemoteFileTasks.runCommandAndAssert(client, "rct cat-manifest"+optionsAsString+manifestFile, 0);
		
		// parse the output from catManifestResult into a Manifest object
		List<Manifest> catManifests = Manifest.parse(catManifestResult.getStdout());
		Assert.assertEquals(catManifests.size(),1,"Parsed one manifest from '"+manifestFile+"'.");
		Manifest catManifest = catManifests.get(0);
		
		// create EntitlementCert objects representing the source for all of the entitlements provided by this manifest
		if (manifestFileContentMap.get(manifestFile)==null) throw new SkipException("Cannot execute this test until manifest file '"+manifestFile+"' has been successfully dumped.");
		client.runCommandAndWaitWithoutLogging("find "+manifestFileContentMap.get(manifestFile).get(0).getParent()+"/export/entitlement_certificates"+" -regex \"/.+/[0-9]+.pem\" -exec rct cat-cert {} \\;");
		String rawCertificates = client.getStdout();
		List<EntitlementCert> entitlementCerts = EntitlementCert.parse(rawCertificates);
		if (entitlementCerts.isEmpty()) Assert.fail("Manifest file '"+manifestFile+"' does not provide any entitlements.");
		
		// loop through the manifest's entitlement certs and assert as much as possible...
		//	[root@jsefler-7 test-manifests]# rct cat-manifest manifest_SYS0395_RH0197181.zip
		//	
		//	+-------------------------------------------+
		//		Manifest
		//	+-------------------------------------------+
		String bannerRegex = "\\+-+\\+\\n\\s*Manifest\\s*\\n\\+-+\\+";
		Assert.assertTrue(Pattern.compile(".*"+bannerRegex+".*",Pattern.DOTALL).matcher(catManifestResult.getStdout()).matches(), "stdout from rct cat-manifest contains a banner matching regex '"+bannerRegex+"'.");
		if (catManifest.server==null) {log.warning("Skipping assertion of non-null General and Consumer for what appears to be a manifest from an older candlepin server");} else {
		//
		//	General:
		//		Server: access.stage.redhat.com/management/distributors/
		//		Server Version: 0.7.13.10-1
		//		Date Created: 2013-01-21T21:24:16.193+0000
		//		Creator: qa@redhat.com
		Assert.assertNotNull(catManifest.server, "General Server value is not null.");
		Assert.assertNotNull(catManifest.serverVersion, "General Server Version value is not null.");
		Assert.assertNotNull(catManifest.dateCreated, "General Date Created value is not null.");
		Assert.assertNotNull(catManifest.creator, "General Creator value is not null.");
		//
		//	Consumer:
		//		Name: jsefler
		//		UUID: b2837b9a-d2d9-4b41-acd9-34bdcf72af66
		//		Type: sam
		Assert.assertNotNull(catManifest.consumerName, "Consumer Name value is not null.");
		Assert.assertNotNull(catManifest.consumerUUID, "Consumer UUID value is not null.");
		String consumerUUIDRegex = "[a-f,0-9,\\-]{36}";
		Assert.assertTrue(Pattern.compile(consumerUUIDRegex/*,Pattern.DOTALL*/).matcher(catManifest.consumerUUID).matches(),"Consumer UUID format matches the expected regex '"+consumerUUIDRegex+"'.");
		Assert.assertNotNull(catManifest.consumerType, "Consumer Type value is not null.");
		Assert.assertTrue(catManifest.consumerType.equals("sam")||catManifest.consumerType.equals("cloudforms")||catManifest.consumerType.equals("satellite"), "Actual Consumer Type value '"+catManifest.consumerType+"' equals \"sam\" or \"cloudforms' or \"satellite\".");	// TODO learn why there is a type distinction
		//		Content Access Mode: entitlement
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.19.6-1")) {
			Assert.assertNotNull(catManifest.consumerContentAccessMode, "Consumer Content Access Mode value is not null.");
			Assert.assertTrue(catManifest.consumerContentAccessMode.equals("entitlement")||catManifest.consumerContentAccessMode.equals("org_environment"), "Actual Consumer Content Access Mode value '"+catManifest.consumerContentAccessMode+"' equals \"entitlement\" or \"org_environment\".");
			if (SubscriptionManagerTasks.isVersion(catManifest.serverVersion, "<", "2.0.22-1")) {	// candlepin commit 18c20d0a1530910d5ca45fecb59f8f7db5e9a04f Golden Ticket
				Assert.assertEquals(catManifest.consumerContentAccessMode, "entitlement", "Since this manifest was created by candlepin version prior to \"2.0.22-1\", the Consumer's Content Access Mode value must be the default value.");
			}
		}
		}
		//	
		//	Subscription:
		//		Name: Red Hat Enterprise Linux Server, Self-support (1-2 sockets) (Up to 1 guest)
		//		Quantity: 2
		//		Created: 2013-01-21T21:22:57.000+0000
		//		Start Date: 2012-12-31T05:00:00.000+0000
		//		End Date: 2013-12-31T04:59:59.000+0000
		//		Suport Level: Self-support
		//		Suport Type: L1-L3
		//		Architectures: x86,x86_64,ia64,s390x,ppc,s390,ppc64
		//		Product Id: RH0197181
		//		Contract: 
		//		Subscription Id: 2677511
		//		Entitlement File: export/entitlements/8a99f9843c401207013c5efe1e1931ce.json
		//		Certificate File: export/entitlement_certificates/4134818306731067736.pem
		//		Certificate Version: 1.0
		//		Provided Products:
		//			69: Red Hat Enterprise Linux Server
		//			180: Red Hat Beta
		//		Content Sets:
		//			/content/beta/rhel/server/5/$releasever/$basearch/cf-tools/1/os
		//			/content/beta/rhel/server/5/$releasever/$basearch/cf-tools/1/source/SRPMS
		//			/content/beta/rhel/server/5/$releasever/$basearch/debug
		//			/content/beta/rhel/server/5/$releasever/$basearch/iso
		//	
		//	Subscription:
		//		Name: Red Hat Employee Subscription
		//		Quantity: 2
		//		Created: 2013-01-21T21:22:56.000+0000
		//		Start Date: 2011-10-08T04:00:00.000+0000
		//		End Date: 2022-01-01T04:59:59.000+0000
		//		Suport Level: None
		//		Suport Type: None
		//		Architectures: x86,x86_64,ia64,s390x,ppc,s390,ppc64
		//		Product Id: SYS0395
		//		Contract: 2596950
		//		Subscription Id: 2252576
		//		Entitlement File: export/entitlements/8a99f9833c400fa5013c5efe1a5a4683.json
		//		Certificate File: export/entitlement_certificates/2571369151493658952.pem
		//		Certificate Version: 1.0
		//		Provided Products:
		//			69: Red Hat Enterprise Linux Server
		//			71: Red Hat Enterprise Linux Workstation
		//			83: Red Hat Enterprise Linux High Availability (for RHEL Server)
		//			85: Red Hat Enterprise Linux Load Balancer (for RHEL Server)
		//			90: Red Hat Enterprise Linux Resilient Storage (for RHEL Server)
		//			180: Red Hat Beta
		//		Content Sets:
		//			/content/beta/rhel/power/5/$releasever/$basearch/highavailability/debug
		//			/content/beta/rhel/power/5/$releasever/$basearch/highavailability/os
		//			/content/beta/rhel/power/5/$releasever/$basearch/highavailability/source/SRPMS
		for (EntitlementCert entitlementCert : entitlementCerts) {
			ManifestSubscription manifestSubscription = ManifestSubscription.findFirstInstanceWithMatchingFieldFromList("certificateFile", entitlementCert.file.toString().replace(manifestFileContentMap.get(manifestFile).get(0).getParent()+File.separator, ""), catManifest.subscriptions);
			if (manifestSubscription==null) Assert.fail("Could not find the ManifestSubscription corresponding to Entitlement '"+entitlementCert.file+"'.");

			Assert.assertEquals(manifestSubscription.name,entitlementCert.orderNamespace.productName, "Subscription Name value comes from entitlementCert.orderNamespace.productName");
			Assert.assertEquals(manifestSubscription.quantity,entitlementCert.orderNamespace.quantityUsed, "Subscription Quantity value comes from entitlementCert.orderNamespace.quantityUsed (ASSUMING NO OTHER UPSTREAM CONSUMERS)");
			// TODO assert Created:
			Assert.assertEquals(manifestSubscription.startDate,entitlementCert.validityNotBefore, "Subscription Start Date comes from entitlementCert.validityNotBefore");
			Assert.assertEquals(manifestSubscription.endDate,entitlementCert.validityNotAfter, "Subscription End Date comes from entitlementCert.validityNotAfter");
			Assert.assertEquals(manifestSubscription.supportLevel,entitlementCert.orderNamespace.supportLevel, "Subscription Service Level value comes from entitlementCert.orderNamespace.supportLevel");
			Assert.assertEquals(manifestSubscription.supportType,entitlementCert.orderNamespace.supportType, "Subscription Service Type value comes from entitlementCert.orderNamespace.supportType");
			List<String> actualArchitectures = new ArrayList<String>(); if (manifestSubscription.architectures!=null) actualArchitectures.addAll(Arrays.asList(manifestSubscription.architectures.split("\\s*,\\s*")));
			List<String> expectedArchitectures = new ArrayList<String>(); for (ProductNamespace productNamespace : entitlementCert.productNamespaces) if (productNamespace.arch!=null) expectedArchitectures.addAll(Arrays.asList(productNamespace.arch.split("\\s*,\\s*")));
			//BAD ASSERT SEE https://bugzilla.redhat.com/show_bug.cgi?id=914799#c3 Assert.assertTrue(actualArchitectures.containsAll(expectedArchitectures)&&expectedArchitectures.containsAll(actualArchitectures), "Subscription Architectures contains the union of providedProduct arches: "+expectedArchitectures);
			Assert.assertEquals(manifestSubscription.productId,entitlementCert.orderNamespace.productId, "Subscription SKU value comes from entitlementCert.orderNamespace.productId");
			Assert.assertEquals(manifestSubscription.contract,entitlementCert.orderNamespace.contractNumber, "Subscription Contract value comes from entitlementCert.orderNamespace.contractNumber");
			Assert.assertEquals(manifestSubscription.subscriptionId,entitlementCert.orderNamespace.orderNumber, "Subscription Order Number value comes from entitlementCert.orderNamespace.orderNumber");
			// TODO assert Entitlement File in json format
			Assert.assertTrue(entitlementCert.file.toString().endsWith(manifestSubscription.certificateFile),"Subscription Certificate File exists");
			Assert.assertEquals(manifestSubscription.certificateVersion,entitlementCert.version, "Subscription Certificate Version value comes from entitlementCert.version");
			List<String> actualProvidedProducts = manifestSubscription.providedProducts;
			List<String> actualDerivedProducts = manifestSubscription.derivedProducts;
			if (false) {
				// This assertion was valid prior to the invention of data center skus that provide no products but do provide derived products to be added to a sub-pool
				List<String> expectedProvidedProducts = new ArrayList<String>(); for (ProductNamespace productNamespace : entitlementCert.productNamespaces) expectedProvidedProducts.add(String.format("%s: %s", productNamespace.id, productNamespace.name));
				Assert.assertTrue(actualProvidedProducts.containsAll(expectedProvidedProducts)&&expectedProvidedProducts.containsAll(actualProvidedProducts), "Manifest Subscription '"+manifestSubscription.name+"' Provided Products "+actualProvidedProducts+" contains all entitlementCert.productNamespaces=>\"id: name\": "+expectedProvidedProducts);
			} else {
				// Instead, let's assume the following assertion is correct... TODO confirm with dev
				// Note: this assertion can only be tested against manifests with data center skus using subscription-manager-1.18.6-1 and higher
				List<String> expectedProducts = new ArrayList<String>(); for (ProductNamespace productNamespace : entitlementCert.productNamespaces) expectedProducts.add(String.format("%s: %s", productNamespace.id, productNamespace.name));
				if (clienttasks.isPackageVersion("subscription-manager",">=","1.18.6-1")) { // subscription-manager commit 23c149907852767e51e7ddea8edf506697827203  Bug 1388207 - [RFE] rct cat-manifest command should show derived products
					List<String> actualProvidedAndDerivedProducts = new ArrayList<String>();
					for (String product : actualProvidedProducts) if (!actualProvidedAndDerivedProducts.contains(product)) actualProvidedAndDerivedProducts.add(product);
					for (String product : actualDerivedProducts) if (!actualProvidedAndDerivedProducts.contains(product)) actualProvidedAndDerivedProducts.add(product);
					Assert.assertTrue(actualProvidedAndDerivedProducts.containsAll(expectedProducts)&&expectedProducts.containsAll(actualProvidedAndDerivedProducts), "The union of all Provided Products "+actualProvidedProducts+" and Derived Products "+actualDerivedProducts+" from manifest Subscription '"+manifestSubscription.name+"' contains all entitlementCert.productNamespaces=>\"id: name\": "+expectedProducts);
				} else {
					log.info("Cannot assert that the union of all provided and derived products from manifest Subscription '"+manifestSubscription.name+"' are represented in this entitlementCert.productNamespaces=>\"id: name\": "+expectedProducts+" because RFE Bug 1388207 is not present in this version of subscription-manager.");
					log.info("We can only assert that the provided products from manifest Subscription '"+manifestSubscription.name+"' are a subset of this entitlementCert.productNamespaces=>\"id: name\": "+expectedProducts+" because RFE Bug 1388207 is not present in this version of subscription-manager to account for derived products.");
					Assert.assertTrue(expectedProducts.containsAll(actualProvidedProducts), "The Provided Products "+actualProvidedProducts+" from manifest Subscription '"+manifestSubscription.name+"' are a subset of all entitlementCert.productNamespaces=>\"id: name\": "+expectedProducts);
				}
			}
			List<String> actualContentSets = new ArrayList<String>(); if (manifestSubscription.contentSets!=null) actualContentSets.addAll(manifestSubscription.contentSets);
			List<String> expectedContentSets = new ArrayList<String>();
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) expectedContentSets.add(contentNamespace.downloadUrl);
			if (options!=null && options.contains("--no-content")) {
				Assert.assertNull(manifestSubscription.contentSets, "The cat-manifest report does not include any Subscription Content Sets when the '--no-content' option is specified.");
			} else {
				Assert.assertTrue(actualContentSets.containsAll(expectedContentSets)&&expectedContentSets.containsAll(actualContentSets), "Subscription Content Sets contains all entitlementCert.contentNamespaces=>downloadUrl: (too long to print)"/* +expectedContentSets */);				
			}
		}
	}
	
	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="ManifestFilesData")
	public Object[][] getManifestFilesDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getManifestFilesDataAsListOfLists());
	}
	protected List<List<Object>> getManifestFilesDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (clienttasks==null) return ll;
		
		for (File manifestFile : manifestFiles) {
			Set<String> bugIds = new HashSet<String>();
///*debugTesting */ if (!manifestFile.getName().equals("manifest_RH00001.zip")) continue;
			
			// Bug 913187 - rct cat-manifest throws Traceback: KeyError: 'webAppPrefix'
			if (manifestFile.getName().equals("stageSamTest20Nov2011.zip")) bugIds.add("913187");
			if (manifestFile.getName().equals("fake-manifest-syncable.zip")) bugIds.add("913187");
			if (manifestFile.getName().equals("manifest-0219-131433-939.zip")) bugIds.add("913187");
			
			// Bug 914717 - rct cat-manifest fails to report Contract from the embedded entitlement cert
			if (manifestFile.getName().equals("manifest_SYS0395_RH0197181.zip")) bugIds.add("914717");
			if (manifestFile.getName().equals("manifest_RH1569626.zip")) bugIds.add("914717");
			
			// Bug 914799 - rct cat-manifest fails to report Architectures from the embedded entitlement cert
			if (manifestFile.getName().equals("stageSamTest20Nov2011.zip")) bugIds.add("914799");

			// Bug 914843 - rct cat-manifest fails to report Provided Products from the embedded entitlement cert
			if (manifestFile.getName().equals("manifest_SYS0395_RH0197181.zip")) bugIds.add("914843");
			
			// Bug 1005430 - rct cat-manifest fails to display all the provided products from the embedded entitlement cert
			if (manifestFile.getName().equals("manifest_SER0406.zip")) bugIds.add("1005430");
			
			// Bug 1388207 - [RFE] rct cat-manifest command should show derived products
			if (manifestFile.getName().equals("manifest_RH00001.zip")) bugIds.add("1388207");
			
			// Bug 1424614 - [RFE] Enhance the 'rct cat-manifest' command to show manifests with the Organization/Environment Content Access Flag
			if (manifestFile.getName().equals("manifest_ORG_ENVIRONMENT.zip")) bugIds.add("1424614");
			
			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[]{}));
			ll.add(Arrays.asList(new Object[] {blockedByBzBug, manifestFile}));				
		}
		
		return ll;
	}
}
