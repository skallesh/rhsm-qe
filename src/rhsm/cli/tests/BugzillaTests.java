package rhsm.cli.tests;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
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
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

import rhsm.base.CandlepinType;
import rhsm.base.ConsumerType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ConsumerCert;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.OrderNamespace;
import rhsm.data.ProductCert;
import rhsm.data.ProductSubscription;
import rhsm.data.Repo;
import rhsm.data.RevokedCert;
import rhsm.data.SubscriptionPool;
import rhsm.data.YumRepo;

import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.LinkedItem;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author skallesh
 *
 *
 */
@Test(groups = {"BugzillaTests"})
public class BugzillaTests extends SubscriptionManagerCLITestScript {
	protected String ownerKey = "";
	protected List<String> providedProduct = new ArrayList<String>();
	protected EntitlementCert expiringCert = null;
	protected String EndingDate;
	protected final String importCertificatesDir = "/tmp/sm-importExpiredCertificatesDir".toLowerCase();
	protected final String myEmptyCaCertFile = "/etc/rhsm/ca/myemptycert.pem";
	protected Integer configuredHealFrequency = null;
	protected Integer configuredCertFrequency = null;
	protected String configuredHostname = null;
	protected String factname = "system.entitlements_valid";
	protected String SystemDateOnClient = null;
	protected String SystemDateOnServer = null;
	List<String> providedProducts = new ArrayList<String>();
	protected List<File> entitlementCertFiles = new ArrayList<File>();
	protected final String importCertificatesDir1 = "/tmp/sm-importV1CertificatesDir".toLowerCase();
	SSHCommandRunner sshCommandRunner = null;
	String productId = "BugzillaTest-product";


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-47933", "RHEL7-63527"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description = "Verify that the EUS RHEL product certs on the CDN for each release correctly reflect the release version.  For example, this affects users that want use subcription-manager release --set=6.3 to keep yum updates fixed to an older release.",
			groups = {"Tier1Tests","VerifyEUSRHELProductCertVersionFromEachCDNReleaseVersion_Test"},
			dataProvider = "VerifyEUSRHELProductCertVersionFromEachCDNReleaseVersion_TestData",
			enabled = true)
	public void testEUSRHELProductCertVersionFromEachCDNReleaseVersion(Object blockedByBug, String release, String rhelRepoUrl, File eusEntitlementCertFile) throws JSONException, Exception {
		if (!(sm_serverType.equals(CandlepinType.hosted)))
			throw new SkipException("To be run against Stage only");
		String rhelProductId = null;
		if ((clienttasks.arch.equals("ppc64")) && (clienttasks.variant.equals("Server")))
			rhelProductId = "74";
		else if ((clienttasks.arch.equals("x86_64")) && (clienttasks.variant.equals("Server")))
			rhelProductId = "69";
		else if ((clienttasks.arch.equals("s390x")) && (clienttasks.variant.equals("Server")))
			rhelProductId = "72";
		else if ((clienttasks.arch.equals("x86_64")) && (clienttasks.variant.equals("ComputeNode")))
			rhelProductId = "76";
		else if (clienttasks.variant.equals("Client"))
			throw new SkipException("Test is not supported for this variant");
		else if (clienttasks.variant.equals("Workstation"))
			throw new SkipException("Test is not supported for this variant");

		File certFile = eusEntitlementCertFile;
		File keyFile = clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(eusEntitlementCertFile);

		// Assert that installed product list features a rhelproduct cert in it
		List<InstalledProduct> currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
		InstalledProduct rhelInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId",
				rhelProductId, currentlyInstalledProducts);
		Assert.assertNotNull(rhelInstalledProduct,
				"Expecting the installed RHEL Server product '" + rhelProductId + "' to be installed.");

		// different arch
		String basearch = clienttasks.arch;
		if (basearch.equals("i686") || basearch.equals("i586") || basearch.equals("i486"))
			basearch = "i386";

		// set the release and baseurl
		String rhelRepoUrlToProductId = rhelRepoUrl.replace("$releasever", release).replace("$basearch", basearch)
				+ "/repodata/productid";

		// using the entitlement certificates to get the productid
		File localProductIdFile = new File("/tmp/productid");
		// curl --stderr /dev/null --insecure --tlsv1 --cert /etc/pki/entitlement/3708865569463790383.pem --key /etc/pki/entitlement/3708865569463790383-key.pem https://cdn.redhat.com/content/eus/rhel/server/6/6.1/x86_64/os/repodata/productid  | tee /tmp/productid
		SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(client, "curl --stderr /dev/null --insecure --tlsv1 --cert "+certFile+" --key "+keyFile+" "+rhelRepoUrlToProductId+" | tee "+localProductIdFile, Integer.valueOf(0));
		
		String fileNotFound = "File not found.";
		
		// releases with no EUS support...
		if (release.equals("6.8")) {
			log.warning("There is no EUS support scheduled for RHEL '"+release+"'.  See https://pp.engineering.redhat.com/pp/product/rhel/release/rhel-6-8/schedule/tasks");
			Assert.assertTrue(result.getStdout().contains(fileNotFound), "Expected attempt to fetch the EUS productid results in '"+fileNotFound+"'");
			throw new SkipException("There is no EUS support for RHEL '"+release+"'.  See https://pp.engineering.redhat.com/pp/product/rhel/release/rhel-6-8/schedule/tasks");
		}
		if (release.equals("6.9")) {
			log.warning("There is no EUS support scheduled for RHEL '"+release+"'.  See https://pp.engineering.redhat.com/pp/product/rhel/release/rhel-6-9/schedule/tasks");
			Assert.assertTrue(result.getStdout().contains(fileNotFound), "Expected attempt to fetch the EUS productid results in '"+fileNotFound+"'");
			throw new SkipException("There is no EUS support for RHEL '"+release+"'.  See https://pp.engineering.redhat.com/pp/product/rhel/release/rhel-6-9/schedule/tasks");
		}
		if (release.equals("7.0")) {
			log.warning("There is no EUS support scheduled for RHEL '"+release+"'.  See https://pp.engineering.redhat.com/pp/product/rhel/release/rhel-7-0/schedule/tasks");
			Assert.assertTrue(result.getStdout().contains(fileNotFound), "Expected attempt to fetch the EUS productid results in '"+fileNotFound+"'");
			throw new SkipException("There is no EUS support for RHEL '"+release+"'.  See https://pp.engineering.redhat.com/pp/product/rhel/release/rhel-7-0/schedule/tasks");
		}
		
		// fail the test when productid file is not found
		if (result.getStdout().contains(fileNotFound)) {
			Assert.fail("Failed to find a productid file on the CDN at '"+rhelRepoUrlToProductId+"'.  It is possible that RHEL release '"+release+"' has no EUS support; check the Product Pages for RHEL https://pp.engineering.redhat.com/pp/product/rhel/ to see if EUS is on the Schedule.  If not, then this automated test needs an update the releases with no EUS support.");
		}
		
		// create a ProductCert corresponding to the productid file
		ProductCert productIdCert = clienttasks.getProductCertFromProductCertFile(localProductIdFile);
		log.info("Actual product cert from CDN '" + rhelRepoUrlToProductId + "': " + productIdCert);

		// assert the expected productIdCert release version
		Assert.assertEquals(productIdCert.productNamespace.version, release,
				"Version of the productid on the CDN at '" + rhelRepoUrlToProductId
						+ "' that will be installed by the yum product-id plugin after setting the subscription-manager release to '"
						+ release + "'.");
	}

	@DataProvider(name = "VerifyEUSRHELProductCertVersionFromEachCDNReleaseVersion_TestData")
	public Object[][] getVerifyEUSRHELProductCertVersionFromEachCDNReleaseVersion_TestDataAs2dArray()
			throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(
				getVerifyEUSRHELProductCertVersionFromEachCDNReleaseVersion_TestDataAsListOfLists());
	}

	protected List<List<Object>> getVerifyEUSRHELProductCertVersionFromEachCDNReleaseVersion_TestDataAsListOfLists()
			throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (!isSetupBeforeSuiteComplete)
			return ll;
		if (clienttasks == null)
			return ll;
		String eusProductId = null;
		if ((clienttasks.arch.equals("ppc64")) && (clienttasks.variant.equals("Server")))
			eusProductId = "75";
		else if ((clienttasks.arch.equals("x86_64")) && (clienttasks.variant.equals("Server")))
			eusProductId = "70";
		else if ((clienttasks.arch.equals("x86_64")) && clienttasks.variant.equals("ComputeNode"))
			eusProductId = "217";
		else if ((clienttasks.arch.equals("s390x")) && (clienttasks.variant.equals("Server")))
			eusProductId = "73";
		if (eusProductId == null) {
			log.warning("This test does not yet cover variant '" + clienttasks.variant + "' on arch '"
					+ clienttasks.arch + "'.");
			return ll; // return no rows and no test will be run
		}

		// unregister
		clienttasks.unregister(null, null, null, null);
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, null, null, null, null, null, null);
		// get current product cert and verify that rhelproduct is installed on
		// the system
		ProductCert rhelProductCert = clienttasks.getCurrentRhelProductCert();
		SubscriptionPool pool = null;
		for (SubscriptionPool eusSubscriptionPool : SubscriptionPool.parse(
				clienttasks.list(null, true, null, null, null, null, null, null, null, "*Extended*", null, null, null, null, null)
						.getStdout())) {
			if ((CandlepinTasks.getPoolProvidedProductIds(sm_client1Username, sm_client1Password, sm_serverUrl,
					eusSubscriptionPool.poolId).contains(eusProductId))) {
				// and attach eus subscription
				clienttasks.subscribe(null, null, eusSubscriptionPool.poolId, null, null, null, null, null, null, null,
						null, null, null);
				pool = eusSubscriptionPool;
				break;
			}
		}
		if (pool == null) {
			log.warning("Could not find an available EUS subscription that covers EUS product '" + eusProductId + "'.");
			return ll; // return no rows and no test will be run
		}

		// find the entitlement that provides access to RHEL and EUS
		EntitlementCert eusEntitlementCerts = clienttasks.getEntitlementCertCorrespondingToSubscribedPool(pool);
		if (eusEntitlementCerts == null) {
			return ll;
		}

		// if eus repo is not enabled , enable it
		String rhelRepoUrl = null;
		for (Repo disabledRepo : Repo.parse(
				clienttasks.repos(null, false, true, (String) null, (String) null, null, null, null, null).getStdout()))

		{
			String variant = null;
			if (clienttasks.arch.equals("x86_64")) {
				if (clienttasks.variant.equals("ComputeNode"))
					variant = "hpc-node";
				if (clienttasks.variant.equals("Server"))
					variant = "server";
			} else if (clienttasks.arch.equals("ppc64") || (clienttasks.arch.equals("ppc64le"))) {
				variant = "for-power";
			} else if (clienttasks.arch.equals("s390x"))
				variant = "for-system-z";
			// add repos for eus-source rpms and debug rpms
			if ((disabledRepo.repoId.matches("rhel-[0-9]+-" + variant + "-eus-rpms"))) {
				clienttasks.repos(null, null, null, disabledRepo.repoId, (String) null, null, null, null, null);
			}
		}
		for (
		// if eus repo is enabled then add it to the data provider
		Repo enabledRepo : Repo.parse(
				clienttasks.repos(null, true, false, (String) null, (String) null, null, null, null, null).getStdout())) {
			if (enabledRepo.enabled) {

				String variant = null;
				if (clienttasks.arch.equals("x86_64")) {
					if (clienttasks.variant.equals("ComputeNode"))
						variant = "hpc-node";
					if (clienttasks.variant.equals("Server"))
						variant = "server";
				} else if (clienttasks.arch.equals("ppc64") || (clienttasks.arch.equals("ppc64le"))) {
					variant = "for-power";
				} else if (clienttasks.arch.equals("s390x"))
					variant = "for-system-z";

				if (enabledRepo.repoId.matches("rhel-[0-9]+-" + variant + "-eus-rpms")) {
					rhelRepoUrl = enabledRepo.repoUrl;
				}
			}
		}

		// add each available release as a row to the dataProvider
		// TODO bug 1369516 should be added to the BlockedByBzBug for all rows
		// against ppc64le
		for (String release : clienttasks.getCurrentlyAvailableReleases(null, null, null, null)) {
			// skip the latest releases; e.g. 6Server
			if (!isFloat(release)) continue;
			
			List<String> bugIds = new ArrayList<String>();
			if (release.startsWith("6")) {
				if (release.matches("6.4") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("x86_64"))
					bugIds.add("1357574");	// https://bugzilla.redhat.com/show_bug.cgi?id=1357574#c16
				if (release.matches("6.4") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("i386"))
					bugIds.add("1357574");	// https://bugzilla.redhat.com/show_bug.cgi?id=1357574#c18
				if (release.matches("6.5") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("i386"))
					bugIds.add("1357574");	// https://bugzilla.redhat.com/show_bug.cgi?id=1357574#c18
				if (release.matches("6.6") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("i386"))
					bugIds.add("1357574");	// https://bugzilla.redhat.com/show_bug.cgi?id=1357574#c18
				if (release.matches("6.4") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("s390x"))
					bugIds.add("1357574");	// https://bugzilla.redhat.com/show_bug.cgi?id=1357574#c18
				if (release.matches("6.5") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("s390x"))
					bugIds.add("1357574");	// https://bugzilla.redhat.com/show_bug.cgi?id=1357574#c18
				if (release.matches("6.6") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("s390x"))
					bugIds.add("1357574");	// https://bugzilla.redhat.com/show_bug.cgi?id=1357574#c18
				if (release.matches("6.5") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("ppc64"))
					bugIds.add("1357574");	// https://bugzilla.redhat.com/show_bug.cgi?id=1357574#c19
				if (release.matches("6.6") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("ppc64"))
					bugIds.add("1357574");	// https://bugzilla.redhat.com/show_bug.cgi?id=1357574#c19
				if (release.matches("6.7") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("x86_64"))
					bugIds.add("1352162");
			}
			if (release.startsWith("7")) {
				if (release.matches("7.1")) // && clienttasks.variant.equals("Server"))
					bugIds.add("1369920");
			}
			
			BlockedByBzBug blockedByBzBug = new BlockedByBzBug(bugIds.toArray(new String[] {}));
			ll.add(Arrays.asList(new Object[] { blockedByBzBug, release, rhelRepoUrl, eusEntitlementCerts.file }));
		}

		return ll;

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL7-55663"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify Status Cache not used when listing repos with a bad proxy ",
			groups = {"Tier3Tests","ListingReposWithBadProxy", "blockedByBug-1298327", "blockedByBug-1345962", "blockedByBug-1389794" },
			enabled = false)
	public void testListingReposWithBadProxy() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		SSHCommandResult result = clienttasks.repos(true, null, null, (String) null, null, null, null, null, null);
		String logMessage = "Unable to reach server, using cached status";
		String rhsmLogMarker = System.currentTimeMillis() + " Testing **********************************************";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, rhsmLogMarker);
		clienttasks.repos(true, null, null, (String) null, null,
				sm_basicauthproxyHostname + ":" + sm_basicauthproxyPort, sm_basicauthproxyUsername, "badproxy", null);
		String tailFromRhsmlogFile = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile,
				rhsmLogMarker, "cached");
		Assert.assertContainsNoMatch(result.getStderr(),
				"Network error, unable to connect to server. Please see /var/log/rhsm/rhsm.log for more information.",
				"   Validated that no network is thrown");
		Assert.assertTrue(result.getStdout().contains("Available Repositories in /etc/yum.repos.d/redhat.repo"),
				"Verified that the repo commands succeeds by using the cached status ");
		Assert.assertTrue(tailFromRhsmlogFile.contains(logMessage), "verified that rhsm.log has the message : "
				+ logMessage + "indicating proxy connection has failed and cached status is being used");

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47893", "RHEL7-96267"},
//			linkedWorkItems= {
//				@LinkedItem(
//					workitemId= "",
//					project= Project.RHEL6,
//					role= DefTypes.Role.VERIFIES),
//				@LinkedItem(
//					workitemId= "",
//					project= Project.RedHatEnterpriseLinux7,
//					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT, //"releng" "Unrecognized enumeration value 'releng' for custom field id 'casecomponent'"
			testtype= @TestType(testtype= DefTypes.TestTypes.STRUCTURAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.LOW, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description= "verify Product certs not be generated with a tag value of None",
			groups= {"Tier1Tests","VerifyProductCertWithNoneTag", "blockedByBug-955824" },
			enabled= true)
	public void testProductCertWithNoneTag() throws Exception {
		String baseProductsDir = "/usr/share/rhsm/product/RHEL-" + clienttasks.redhatReleaseX;
		for (ProductCert productCert : clienttasks.getProductCerts(baseProductsDir)) {
			Assert.assertFalse(productCert.productNamespace.providedTags.equals("None"));
		}

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21981", "RHEL7-51843"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify rhsm-debug --no-archive --destination <destination Loc> throws [Errno 18] Invalid cross-device link",
			groups = {"Tier3Tests","VerifyrhsmDebugWithNoArchive", "blockedByBug-1175284" },
			enabled = true)
	public void testRhsmDebugWithNoArchive() throws Exception {
		String path = "/home/tmp-dir";
		client.runCommandAndWait("rm -rf " + path + " && mkdir -p " + path); // pre
		// cleanup
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		SSHCommandResult result = client
				.runCommandAndWait(clienttasks.rhsmDebugSystemCommand(path, true, null, null, null, null, null, null, null));
		String expectedStderr= "To use the no-archive option, the destination directory '"+path+"' must exist on the same file system as the data assembly directory '/var/spool/rhsm/debug'.";
		Assert.assertContainsMatch(result.getStderr(),expectedStderr,path +" is not on the same file system as the data assembly directory '/var/spool/rhsm/debug' , so rhsm-debug --no-archive --destination "+path+ "will not write anything." );
		client.runCommandAndWait("rm -rf " + path);

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21932", "RHEL7-51794"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify subscription-manager attach --file <file> ,with file being empty attaches subscription for installed product",
			groups = {"Tier3Tests","VerifyAttachingEmptyFile", "blockedByBug-1175291" },
			enabled = true)
	public void testAttachingEmptyFile() throws Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.13.8-1"))
			throw new SkipException(
					"The attach --file function was not implemented in this version of subscription-manager.");
		String file = "/tmp/empty_file";
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null); // disable
		client.runCommandAndWait("touch " + file);
		client.runCommandAndWait("cat " + file);
		SSHCommandResult sshCommandResult = clienttasks.subscribe_(null, (String) null, (String) null, (String) null,
				null, null, null, null, file, null, null, null, null);
		String expectedStderr = String.format("Error: The file \"%s\" does not contain any pool IDs.", file);
		Assert.assertEquals(sshCommandResult.getStderr().trim(), expectedStderr,
				"The stderr result from subscribe with an empty file of poolIds.");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21957", "RHEL7-51819"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify subscription-manager repos --list does not delete an imported entitlement certificate on a system",
			groups = {"Tier3Tests","VerifyImportedCertgetsDeletedByRepoCommand", "blockedByBug-1160150" },
			enabled = true)
	public void testImportedCertGetsDeletedByRepoCommand() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, false, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null); // disable
		SubscriptionPool pool = getRandomSubsetOfList(clienttasks.getCurrentlyAvailableSubscriptionPools(), 1).get(0);
		File importEntitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool, sm_clientUsername,
				sm_clientPassword, sm_serverUrl);
		File importEntitlementKeyFile = clienttasks
				.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(importEntitlementCertFile);
		File importCertificateFile = new File(
				importCertificatesDir1 + File.separator + importEntitlementCertFile.getName());
		List<Repo> subscribedReposBefore = clienttasks.getCurrentlySubscribedRepos();
		client.runCommandAndWait("mkdir -p " + importCertificatesDir1);
		client.runCommandAndWait(
				"cat " + importEntitlementCertFile + " " + importEntitlementKeyFile + " > " + importCertificateFile);
		String path = importCertificateFile.getPath();
		clienttasks.clean();
		clienttasks.importCertificate(path);
		int Ceritificate_countBeforeRepoCommand = clienttasks.getCurrentEntitlementCertFiles().size();
		SSHCommandResult Result = clienttasks.repos_(true, null, null, (String) null, null, null, null, null, null);
		int Ceritificate_countAfterRepoCommand = clienttasks.getCurrentEntitlementCertFiles().size();
		List<Repo> subscribedReposAfter = clienttasks.getCurrentlySubscribedRepos();
		Assert.assertEquals(Ceritificate_countBeforeRepoCommand, Ceritificate_countAfterRepoCommand);
		Assert.assertEquals(Result.getExitCode(), new Integer(0));
		Assert.assertTrue(
				subscribedReposBefore.containsAll(subscribedReposAfter)
						&& subscribedReposAfter.containsAll(subscribedReposBefore),
				"The list of subscribed repos is the same before and after importing the entitlement certificate.");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21970", "RHEL7-51832"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify End date and start date of the subscription is appropriate one when you attach a future subscription and then  heal after 1 min",
			groups = {"Tier3Tests","VerifyStartEndDateOfSubscription", "blockedByBug-994853","blockedByBug-1440934" },
			enabled = true)
	public void testStartEndDateOfSubscription() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		Map<String, String> factsMap = new HashMap<String, String>();
		factsMap.put("cpu.cpu_socket(s)", String.valueOf(4));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null); // [jsefler] I believe
		// facts --update
		// should be called
		// after overriding
		// facts
		clienttasks.autoheal(null, null, true, null, null, null, null);
		for (SubscriptionPool AvailablePools : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if (AvailablePools.productId.equals("awesomeos-x86_64")) {
				clienttasks.subscribe(null, null, AvailablePools.poolId, null, null, "1", null, null, null, null, null,
						null, null);
			}
		}
		InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId",
				"100000000000002", clienttasks.getCurrentlyInstalledProducts());

		for (ProductSubscription consumedProductSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			if (consumedProductSubscription.provides.contains(installedProduct.productName)) {
				Assert.assertTrue(!installedProduct.startDate.after(consumedProductSubscription.startDate),
						"Comparing Start Date '" + InstalledProduct.formatDateString(installedProduct.startDate)
								+ "' of Installed Product '" + installedProduct.productName + "' to Start Date '"
								+ InstalledProduct.formatDateString(consumedProductSubscription.startDate)
								+ "' of Consumed Subscription '" + consumedProductSubscription.productName
								+ "'.  (Installed Product startDate should be <= Consumed Subscription startDate)");
			}
		}

		clienttasks.autoheal(null, true, null, null, null, null, null);
		clienttasks.restart_rhsmcertd(null, null, true);

		InstalledProduct installedProductAfterRHSM = InstalledProduct.findFirstInstanceWithMatchingFieldFromList(
				"productId", "100000000000002", clienttasks.getCurrentlyInstalledProducts());

		for (ProductSubscription consumedProductSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			if (consumedProductSubscription.provides.contains(installedProductAfterRHSM.productName)) {
				Assert.assertTrue(!installedProductAfterRHSM.startDate.after(consumedProductSubscription.startDate),
						"Comparing Start Date '"
								+ InstalledProduct.formatDateString(installedProductAfterRHSM.startDate)
								+ "' of Installed Product '" + installedProductAfterRHSM.productName
								+ "' to Start Date '"
								+ InstalledProduct.formatDateString(consumedProductSubscription.startDate)
								+ "' of Consumed Subscription '" + consumedProductSubscription.productName
								+ "'.  (Installed Product startDate should be <= Consumed Subscription startDate)");
				if (!consumedProductSubscription.isActive) {
					Assert.assertEquals(installedProductAfterRHSM.endDate, consumedProductSubscription.endDate);
				}
			}
		}
	}

	/**
	 * @param releases
	 *            - from getCurrentlyAvailableReleases() Example: [6.1, 6.2,
	 *            6.3, 6.4, 6.5, 6.6, 6Server] 6.6 is the newest
	 * @return
	 */
	String getNewestReleaseFromReleases(List<String> releases) {
		String newestRelease = null;
		for (String release : releases) {
			if (isFloat(release)) {
				if (newestRelease == null) {
					newestRelease = release;
				} else if (Float.valueOf(release) > Float.valueOf(newestRelease)) {
					newestRelease = release;
				}
			}
		}
		return newestRelease;
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21971", "RHEL7-51833"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify status check and response from server after addition and deletion of product to/from /etc/pki/product/",
			groups = {"Tier3Tests","VerifyStatusCheck", "blockedByBug-921870", "blockedByBug-1183175" },
			enabled = true)
	public void testStatusAfterProductIdIsAddedOrDeleted() throws Exception {
		String result, expectedStatus;
		Boolean Flag = false;
		ProductCert installedProductCert32060 = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId",
				"32060", clienttasks.getCurrentProductCerts());
		Assert.assertNotNull(installedProductCert32060, "Found installed product cert 32060 needed for this test.");
		configureTmpProductCertDirWithInstalledProductCerts(Arrays.asList(new ProductCert[] {}));
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);
		Assert.assertTrue(clienttasks.getCurrentProductCertFiles().isEmpty(), "No product certs are installed.");
		result = clienttasks.status(null, null, null, null, null).getStdout();
		expectedStatus = "Overall Status: Current";
		Assert.assertTrue(result.contains(expectedStatus),
				"System status displays '" + expectedStatus + "' because no products are installed.");
		client.runCommandAndWait("cp " + installedProductCert32060.file + " " + tmpProductCertDir);
		result = clienttasks.status(null, null, null, null, null).getStdout();
		expectedStatus = "Overall Status: Invalid";
		Assert.assertTrue(result.contains(expectedStatus),
				"System status displays '" + expectedStatus + "' after manully installing a product cert.");
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if (CandlepinTasks.isPoolRestrictedToUnmappedVirtualSystems(sm_clientUsername, sm_clientPassword,
					sm_serverUrl, pool.poolId)) {
				clienttasks.subscribeToSubscriptionPool(pool);
				Flag = true;
				break;
			}
		}
		clienttasks.autoheal(null, true, null, null, null, null, null); // enable
		clienttasks.run_rhsmcertd_worker(true);
		result = clienttasks.status(null, null, null, null, null).getStdout();
		if (Flag) {
			expectedStatus = "Overall Status: Insufficient";
			Assert.assertTrue(result.contains(expectedStatus), "System status displays '" + expectedStatus
					+ "' after finally running rhsmcertd worker with auto-healing.");

		} else {
			expectedStatus = "Overall Status: Current";
			Assert.assertTrue(result.contains(expectedStatus), "System status displays '" + expectedStatus
					+ "' after finally running rhsmcertd worker with auto-healing.");
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21956", "RHEL7-51818"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if Status displays product name multiple times when the system had inactive stack subscriptions",
			groups = {"Tier3Tests","VerifyIfStatusDisplaysProductNameMultipleTimes", "blockedByBug-972752" },
			enabled = true)
	public void testIfStatusDisplaysProductNameMultipleTimes() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		int sockets = 4;
		clienttasks.autoheal(null, null, true, null, null, null, null);
		Map<String, String> factsMap = new HashMap<String, String>();
		factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		for (SubscriptionPool AvailablePools : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if (AvailablePools.productId.equals("awesomeos-x86_64")) {
				clienttasks.subscribe(null, null, AvailablePools.poolId, null, null, "1", null, null, null, null, null,
						null, null);
			}

		}
		clienttasks.status(null, null, null, null, null).getStdout();
		clienttasks.autoheal(null, true, null, null, null, null, null);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@SuppressWarnings("deprecation")
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21947", "RHEL7-51809"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if update facts button won't recreate facts.json file",
			groups = {"Tier3Tests","VerifyFactsFileExistenceAfterUpdate", "blockedByBug-627707" },
			enabled = true)
	public void testFactsFileExistenceAfterUpdate() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		client.runCommandAndWait("rm -rf " + clienttasks.rhsmFactsJsonFile);
		Assert.assertFalse(RemoteFileTasks.testFileExists(client, clienttasks.rhsmFactsJsonFile) == 1,
				"rhsm facts json file '" + clienttasks.rhsmFactsJsonFile + "' exists");
		clienttasks.facts(null, true, null, null, null, null);
		Assert.assertTrue(RemoteFileTasks.testFileExists(client, clienttasks.rhsmFactsJsonFile) == 1,
				"rhsm facts json file '" + clienttasks.rhsmFactsJsonFile + "' exists");

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21992", "RHEL7-51854"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if unsubscribe does not delete entitlement cert from location /etc/pki/entitlement/product for consumer type candlepin ",
			groups = {"Tier3Tests","unsubscribeTheRegisteredConsumerTypeCandlepin", "blockedByBug-621962" },
			enabled = true)
	public void testUnsubscribeTheRegisteredConsumerTypeCandlepin() throws Exception {
	    if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">="/*TODO CHANGE TO ">" after candlepin 2.1.2-1 is tagged*/, "2.1.1-1")) {	// candlepin commit 739b51a0d196d9d3153320961af693a24c0b826f Bug 1455361: Disallow candlepin consumers to be registered via Subscription Manager
		    clienttasks.registerCandlepinConsumer(sm_clientUsername,sm_clientPassword,sm_clientOrg,sm_serverUrl,"candlepin");
	    }else{
		    clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, ConsumerType.candlepin, null,
				null, null, null, null, (String) null, null, null, null, true, null, null, null, null, null); 
	    }
			    clienttasks.subscribe(true, null, (String) null, null, null, null, null, null, null, null, null, null, null);
			    List<File> files = clienttasks.getCurrentEntitlementCertFiles();
			    Assert.assertNotNull(files.size());
			    clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
			    files = clienttasks.getCurrentEntitlementCertFiles();
			    Assert.assertTrue(files.isEmpty());
		
	}
		

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21991", "RHEL7-51853"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if you can unsubscribe from imported cert",
			groups = {"Tier3Tests","unsubscribeImportedcert","blockedByBug-691784" },
			enabled = true)
	public void testUnsubscribeImportedCert() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null); // disable
		SubscriptionPool pool = getRandomSubsetOfList(clienttasks.getCurrentlyAvailableSubscriptionPools(), 1).get(0);
		File importEntitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool, sm_clientUsername,
				sm_clientPassword, sm_serverUrl);
		File importEntitlementKeyFile = clienttasks
				.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(importEntitlementCertFile);
		File importCertificateFile = new File(
				importCertificatesDir1 + File.separator + importEntitlementCertFile.getName());
		client.runCommandAndWait("mkdir -p " + importCertificatesDir1);
		client.runCommandAndWait(
				"cat " + importEntitlementCertFile + " " + importEntitlementKeyFile + " > " + importCertificateFile);
		String path = importCertificateFile.getPath();
		clienttasks.clean();
		clienttasks.importCertificate(path);
		String result = clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null).getStdout();
		String expected_result = "1 subscriptions removed from this system.";
		Assert.assertEquals(result.trim(), expected_result);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21929", "RHEL7-51790"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if subscription manager CLI does not display all facts",
			groups = {"Tier3Tests","SystemFactsInCLI","blockedByBug-722239" },
			enabled = true)
	public void testSystemFactsInCLI() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		Map<String, String> result = clienttasks.getFacts("system");
		Assert.assertNotNull(result.get("system.certificate_version"));
		// Assert.assertNotNull(result.get("system.name"));

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21924", "RHEL7-51785"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if Registering with an activation key which has run out of susbcriptions results in a system, but no identity certificate",
			groups = {"Tier3Tests","RegisterWithActivationKeyWithExpiredPool", "blockedByBug-803814", "blockedByBug-1555582" },
			enabled = true)
	public void testRegisterUsingActivationKeyWithExpiredPool() throws Exception {
		int endingMinutesFromNow = 1;
		Integer addQuantity = 1;
		String name = String.format("%s_%s-ActivationKey%s", sm_clientUsername, sm_clientOrg,
				System.currentTimeMillis());
		Map<String, String> mapActivationKeyRequest = new HashMap<String, String>();
		mapActivationKeyRequest.put("name", name);
		mapActivationKeyRequest.put("autoAttach", "false");
		JSONObject jsonActivationKeyRequest = new JSONObject(mapActivationKeyRequest);
		JSONObject jsonActivationKey = new JSONObject(
				CandlepinTasks.postResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,
						"/owners/" + sm_clientOrg + "/activation_keys", jsonActivationKeyRequest.toString()));

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				consumerId);
		Calendar endCalendar = new GregorianCalendar();
		String expiringPoolId = createTestPool(-60 * 24, endingMinutesFromNow,false);
		Calendar c1 = new GregorianCalendar();
		endCalendar.add(Calendar.MINUTE, endingMinutesFromNow);
		DateFormat yyyy_MM_dd_DateFormat = new SimpleDateFormat("M/d/yy h:mm aaa");
		String EndingDate = yyyy_MM_dd_DateFormat.format(endCalendar.getTime());
		Calendar c2 = new GregorianCalendar();
		sleep(1 * 59 * 1000 - (c2.getTimeInMillis() - c1.getTimeInMillis()));
		new JSONObject(
				CandlepinTasks.postResourceUsingRESTfulAPI(sm_clientUsername,
						sm_clientPassword, sm_serverUrl, "/activation_keys/" + jsonActivationKey.getString("id")
								+ "/pools/" + expiringPoolId + (addQuantity == null ? "" : "?quantity=" + addQuantity),
						null));
		clienttasks.unregister(null, null, null, null);
		SSHCommandResult registerResult = clienttasks.register_(null, null, sm_clientOrg, null, null, null, null, null,
				null, null, name, null, null, null, true, null, null, null, null, null);
		List<ProductSubscription> consumedResult= clienttasks.getCurrentlyConsumedProductSubscriptions();
		SSHCommandResult consumedListResult= clienttasks.list(null, null, true, null, null, null, null, null, null, null, null, null, null, null, null);

		String expected_message = "Unable to attach pool with ID '" + expiringPoolId + "'.: Subscriptions for "
				+ productId + " expired on: " + EndingDate + ".";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.2.0-1")) {
			Assert.assertContainsMatch(registerResult.getStderr().trim(),"None of the subscriptions on the activation key were available for attaching.", "stderr");
			Assert.assertEquals(consumedListResult.getStdout().trim(),"No consumed subscription pools were found.","Expired subscription cannot be attached to activationkey");
		}
		if ((SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">", "0.9.30-1"))&&(SubscriptionManagerTasks.isVersion(servertasks.statusVersion,"<", "2.2.0-1"))) {
			expected_message = "No activation key was applied successfully."; // Follows:
		// candlepin-0.9.30-1
		// //
		// https://github.com/candlepin/candlepin/commit/bcb4b8fd8ee009e86fc9a1a20b25f19b3dbe6b2a
			Assert.assertEquals(registerResult.getStderr().trim(), expected_message);
			Assert.assertEquals(consumedResult.get(randomGenerator.nextInt(consumedResult.size()-1)).statusDetails.toString(),"[Subscription is expired]","Attached subscription is in expired state");

		}
		SSHCommandResult identityResult = clienttasks.identity_(null, null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.9-1")) { // post
			// commit
			// a695ef2d1da882c5f851fde90a24f957b70a63ad
		    Assert.assertEquals(identityResult.getStderr().trim(), clienttasks.msg_ConsumerNotRegistered, "stderr");
		}else {
		    Assert.assertEquals(identityResult.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered, "stdout");
	    
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21983", "RHEL7-51845"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if Wrong DMI structures Error is filtered from the stderr of subscription-manager command line calls",
			groups = {"Tier3Tests","WrongDMIstructuresError","blockedByBug-706552"},
			enabled = true)
	public void testWrongDmiStructuresError() throws Exception {
		String result = clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null,
				null, null, null, (String) null, null, null, null, true, null, null, null, null, null).getStderr();
		Assert.assertContainsNoMatch(result, "Wrong DMI");
		result = clienttasks.facts(true, null, null, null, null, null).getStderr();
		Assert.assertContainsNoMatch(result, "Wrong DMI");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-20114", "RHEL7-55206"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description = "Verify the fix for Bug 709412 - subscription manager cli uses product name comparisons in the list command",
			groups = {"Tier1Tests","InstalledProductMultipliesAfterSubscription", "blockedByBug-709412" },
			enabled = true)
	public void testInstalledProductMultipliesAfterSubscription() throws Exception {
		if (!(sm_serverType.equals(CandlepinType.hosted)))
			throw new SkipException("To be run against Stage only");

		String serverUrl = getServerUrl(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname"),
				clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port"),
				clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix"));
		clienttasks.register(sm_clientUsername, sm_clientPassword, null, null, null, null, null, null, null, null,
				(String) null, serverUrl, null, null, true, null, null, null, null, null);
		productCertDirBeforeInstalledProductMultipliesAfterSubscription = clienttasks.productCertDir;
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",
				"/usr/share/rhsm/product/RHEL-" + clienttasks.redhatReleaseX);
		List<InstalledProduct> InstalledProducts = clienttasks.getCurrentlyInstalledProducts();

		clienttasks.subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively();
		List<InstalledProduct> InstalledProductsAfterSubscribing = clienttasks.getCurrentlyInstalledProducts();
		Assert.assertEquals(InstalledProducts.size(), InstalledProductsAfterSubscribing.size(),
				"The number installed products listed by subscription manager should remained unchanged after attaching all available subscriptions.");
		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
	}

	@AfterGroups(groups = { "setup" }, value = { "InstalledProductMultipliesAfterSubscription" })
	public void afterInstalledProductMultipliesAfterSubscription() throws IOException {
		if (productCertDirBeforeInstalledProductMultipliesAfterSubscription != null)
			clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir",
					productCertDirBeforeInstalledProductMultipliesAfterSubscription);
	}

	String productCertDirBeforeInstalledProductMultipliesAfterSubscription = null;

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21927", "RHEL7-51788"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify Stacking of a future subscription and present subsciption make the product compliant ",
			groups = {"Tier3Tests","StackingFutureSubscriptionWithCurrentSubscription", "blockedByBug-966069" },
			enabled = true)
	public void testStackingFutureSubscriptionWithCurrentSubscription() throws Exception {

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null);
		int sockets = 9;
		int core = 2;
		int ram = 10;

		Map<String, String> factsMap = new HashMap<String, String>();
		factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
		factsMap.put("cpu.core(s)_per_socket", String.valueOf(core));
		factsMap.put("memory.memtotal", String.valueOf(GBToKBConverter(ram)));
		factsMap.put("virt.is_guest", String.valueOf(Boolean.FALSE));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		Boolean nosubscriptionsFound = true;
		Calendar now = new GregorianCalendar();
		DateFormat yyyy_MM_dd_DateFormat = new SimpleDateFormat("yyyy-MM-dd");
		now.add(Calendar.YEAR, 1);
		now.add(Calendar.DATE, 1);
		String onDateToTest = yyyy_MM_dd_DateFormat.format(now.getTime());
		List<String> providedProductId = new ArrayList<String>();
		List<SubscriptionPool> AvailableStackableSubscription = SubscriptionPool
				.findAllInstancesWithMatchingFieldFromList("subscriptionType", "Stackable",
						clienttasks.getAvailableSubscriptionsMatchingInstalledProducts());
		List<SubscriptionPool> futureStackableSubscription = SubscriptionPool.findAllInstancesWithMatchingFieldFromList(
				"subscriptionType", "Stackable", clienttasks.getAvailableFutureSubscriptionsOndate(onDateToTest));
		List<SubscriptionPool> futureSubscription = FindSubscriptionsWithSuggestedQuantityGreaterThanTwo(
				futureStackableSubscription);
		List<SubscriptionPool> AvailableSubscriptions = FindSubscriptionsWithSuggestedQuantityGreaterThanTwo(
				AvailableStackableSubscription);
		for (SubscriptionPool AvailableSubscriptionPools : AvailableSubscriptions) {
			int quantity = AvailableSubscriptionPools.suggested;
			for (SubscriptionPool FutureSubscriptionPools : futureSubscription) {
				if ((AvailableSubscriptionPools.subscriptionName).equals(FutureSubscriptionPools.subscriptionName)) {
					providedProductId = AvailableSubscriptionPools.provides;
					clienttasks.subscribe(null, null, AvailableSubscriptionPools.poolId, null, null,
							Integer.toString(quantity - 1), null, null, null, null, null, null, null);
					nosubscriptionsFound = false;
					InstalledProduct AfterAttachingFutureSubscription = InstalledProduct
							.findFirstInstanceWithMatchingFieldFromList("productName",
									providedProductId.get(providedProductId.size() - 1),
									clienttasks.getCurrentlyInstalledProducts());
					Assert.assertEquals(AfterAttachingFutureSubscription.status, "Partially Subscribed",
							"Verified that installed product is partially subscribed before attaching a future subscription");
					clienttasks.subscribe(null, null, FutureSubscriptionPools.poolId, null, null, "1", null, null, null,
							null, null, null, null);

					break;
				}

			}

		}
		if (nosubscriptionsFound)
			throw new SkipException("no subscriptions found");
		InstalledProduct AfterAttaching = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName",
				providedProductId.get(providedProductId.size() - 1), clienttasks.getCurrentlyInstalledProducts());
		Assert.assertEquals(AfterAttaching.status, "Partially Subscribed",
				"Verified that installed product is partially subscribed even after attaching a future subscription");
		

	}

	public List<SubscriptionPool> FindSubscriptionsWithSuggestedQuantityGreaterThanTwo(
			List<SubscriptionPool> subscriptionList) {
		List<SubscriptionPool> subscriptionPool = new ArrayList<SubscriptionPool>();
		for (SubscriptionPool Subscriptions : subscriptionList) {
			if (Subscriptions.suggested >= 2) {
				subscriptionPool.add(Subscriptions);
			}
		}
		return subscriptionPool;

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21903", "RHEL7-51764"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify the system compliance after deleting the consumer",
			groups = {"Tier3Tests","ComplianceAfterConsumerDeletion" },
			enabled = true)
	public void testComplianceAfterConsumerDeletion() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerid = clienttasks.getCurrentConsumerId();
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				"/consumers/" + consumerid);
		String complianceStatus = CandlepinTasks
				.getConsumerCompliance(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, consumerid)
				.getString("displayMessage");

		String message = "Consumer " + consumerid + " has been deleted";
		if (!clienttasks.workaroundForBug876764(sm_serverType))
			message = "Unit " + consumerid + " has been deleted";

		Assert.assertContainsMatch(message, complianceStatus);

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21955", "RHEL7-51817"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if ipV4_Address is unknown in the facts list",
			groups = {"Tier3Tests","VerifyIfIPV4_AddressIsUnknown", "blockedByBug-694662" },
			enabled = true)
	public void testIfIPV4_AddressIsUnknown() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		Map<String, String> ipv4 = clienttasks.getFacts("ipv4_address");
		Assert.assertFalse(ipv4.containsValue("unknown"));
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21988", "RHEL7-51850"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if configurations like manage_repos have default values",
			groups = {"Tier3Tests","defaultValueForManageRepos", "blockedByBug-807721" },
			enabled = true)
	public void testDefaultValueForManageReposConfiguration() throws Exception {

		String result = clienttasks.config(true, null, null, (String[]) null).getStdout();
		clienttasks.commentConfFileParameter(clienttasks.rhsmConfFile, "manage_repos");
		String resultAfterCommentingtheParameter = clienttasks.config(true, null, null, (String[]) null).getStdout();
		Assert.assertEquals(result, resultAfterCommentingtheParameter);
		clienttasks.uncommentConfFileParameter(clienttasks.rhsmConfFile, "manage_repos");

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@SuppressWarnings("deprecation")
	@Test(	description = "verify if refresh pools will not notice change in provided products",
			groups = {"Tier3Tests","RefreshPoolAfterChangeInProvidedProducts", "blockedByBug-665118" },
			enabled = false)
	public void testRefreshPoolAfterChangeInProvidedProducts() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);

		String consumerId = clienttasks.getCurrentConsumerId();
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				consumerId);
		String name, productId;
		List<String> providedProductIds = new ArrayList<String>();
		name = "Test product to check pool refresh";
		productId = "test-product";
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.clear();
		attributes.put("version", "1.0");
		attributes.put("variant", "server");
		attributes.put("arch", "ALL");
		attributes.put("warning_period", "30");
		attributes.put("type", "MKT");
		attributes.put("type", "SVC");
		providedProductIds.add("37060");
		Integer contractNumber = getRandInt();
		Integer accountNumber = getRandInt();
		Calendar endCalendar = new GregorianCalendar();
		endCalendar.set(Calendar.HOUR_OF_DAY, 0);
		endCalendar.set(Calendar.MINUTE, 0);
		endCalendar.set(Calendar.SECOND, 0); // avoid times in the middle of the
		// day
		endCalendar.add(Calendar.MINUTE, 15 * 24 * 60); // 15 days from today
		Date endDate = endCalendar.getTime();
		Calendar startCalendar = new GregorianCalendar();
		startCalendar.set(Calendar.HOUR_OF_DAY, 0);
		startCalendar.set(Calendar.MINUTE, 0);
		startCalendar.set(Calendar.SECOND, 0); // avoid times in the middle of
		// the day
		startCalendar.add(Calendar.MINUTE, -1 * 24 * 60); // 1 day ago

		Date startDate = startCalendar.getTime();
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, productId);
		String resourcePath = "/products/" + productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);

		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				sm_clientOrg, name, productId, 1, attributes, null);
		@SuppressWarnings("deprecation")
		String requestBody = CandlepinTasks.createSubscriptionRequestBody(sm_serverUrl, 20, startDate, endDate,
				productId, contractNumber, accountNumber, providedProductIds, null).toString();
		JSONObject jsonSubscription = new JSONObject(CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, "/owners/" + ownerKey + "/subscriptions", requestBody));
		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, ownerKey);
		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, jobDetail, "FINISHED", 5 * 1000, 1);
		String poolId = null;
		for (SubscriptionPool pools : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if (pools.productId.equals(productId)) {
				poolId = pools.poolId;
			}
		}
		providedProductIds.remove("37060");
		providedProductIds.add("100000000000002");

		List<JSONObject> pprods = new ArrayList<JSONObject>();
		if (providedProducts != null)
			for (String id : providedProductIds) {
				JSONObject jo = new JSONObject();
				jo.put("id", id);
				pprods.add(jo);
			}
		jsonSubscription.remove("derivedProvidedProducts");
		jsonSubscription.put("providedProducts", pprods);

		// String sub="{\"quantity\":\"8\"}]}";
		// JSONObject jsonData= new JSONObject(jsonSubscription);
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				"/owners/admin/subscriptions/", jsonSubscription);
		jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, ownerKey);
		CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, jobDetail, "FINISHED", 5 * 1000, 1);
		for (SubscriptionPool pools : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if (pools.productId.equals(productId)) {
				poolId = pools.poolId;
			}
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21907", "RHEL7-51768"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify Remote Server Exception is getting displayed for Server 500 Error",
			groups = {"Tier3Tests","DisplayOfRemoteServerExceptionForServer500Error", "blockedByBug-668814" },
			enabled = true)
	public void testDisplayOfRemoteServerExceptionForServer500Error() throws Exception {
		String prefixValueBeforeExecution = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix");
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "server", "hostname".toLowerCase(), configuredHostname });
		listOfSectionNameValues.add(new String[] { "server", "port".toLowerCase(), "8443" });
		listOfSectionNameValues.add(new String[] { "server", "prefix", "/footestprefix" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		SSHCommandResult registerResult = clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg,
				null, null, null, null, null, null, null, (String) null, null, null, null, true, null, null, null,
				null, null);
		String Expected_Message = clienttasks.msg_NetworkErrorCheckConnection;
		// [jsefler 10/30/2014] This test is currently encountering a 404
		// instead of a 500; TODO change this testcase to force a 500 error
		// Error during registration: Server error attempting a GET to
		// /footestprefix/ returned status 404
		Expected_Message = clienttasks.msg_RemoteErrorCheckConnection;
		listOfSectionNameValues.add(new String[] { "server", "prefix", prefixValueBeforeExecution.trim() });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.9-1")) { // post
			// commit
			// a695ef2d1da882c5f851fde90a24f957b70a63ad
			Assert.assertEquals(registerResult.getStderr().trim(), Expected_Message, "stderr");
		} else {
			Assert.assertEquals(registerResult.getStdout().trim(), Expected_Message, "stdout");
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21915", "RHEL7-51776"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify Manual Changes To Redhat.Repo is sticky",
			groups = {"Tier3Tests","ManualChangesToRedhat_Repo","blockedByBug-797243" },
			enabled = true)
	public void testManualChangesToRedhatRepo() throws Exception {
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "rhsmcertd", "autoAttachInterval".toLowerCase(), "1440" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);
		List<SubscriptionPool> Availablepools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool pool = Availablepools.get(randomGenerator.nextInt(Availablepools.size()));
		clienttasks.subscribeToSubscriptionPool(pool);
		for (Repo repo : clienttasks.getCurrentlySubscribedRepos()) {
			if (repo.repoId.equals("always-enabled-content")) {

				Assert.assertTrue(repo.enabled);
			}
		}
		client.runCommandAndWait(
				"sed -i \"/\\[always-enabled-content]/,/\\[/s/^enabled\\s*=.*/Enabled: false/\" /etc/yum.repos.d/redhat.repo");
		for (Repo repo : clienttasks.getCurrentlySubscribedRepos()) {
			if (repo.repoId.equals("always-enabled-content")) {
				Assert.assertFalse(repo.enabled);
			}
		}
		client.runCommandAndWait(" yum repolist enabled");
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		String expected_message = "This system has no repositories available through subscriptions.";
		String reposlist = clienttasks.repos(true, null, null, (String) null, null, null, null, null, null).getStdout();
		Assert.assertEquals(reposlist.trim(), expected_message);
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		reposlist = clienttasks.repos(true, null, null, (String) null, null, null, null, null, null).getStdout();
		Assert.assertEquals(reposlist.trim(), expected_message);
		clienttasks.subscribe(true, null, (String) null, null, null, null, null, null, null, null, null, null, null);
		for (Repo repo : clienttasks.getCurrentlySubscribedRepos()) {
			if (repo.repoId.equals("always-enabled-content")) {
				Assert.assertTrue(repo.enabled);
			}
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21911", "RHEL7-51772"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Extraneous / InRequest urls in the rhsm.log file",
			groups = {"Tier3Tests","ExtraneousSlashInRequesturls","blockedByBug-848836" },
			enabled = true)
	public void testExtraneousSlashInRequestUrls() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		Boolean actual = false;
		String LogMarker = System.currentTimeMillis()
				+ " Testing ***************************************************************";
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "server", "prefix".toLowerCase(), "/candlepin" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		Boolean flag = regexInRhsmLog("//",
				RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, LogMarker, "GET"));
		Assert.assertEquals(flag, actual);

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description = "verify the first system is unregistered when the second system is registered using consumerid of the first", 
			groups = {"Tier3Tests","RegisterTwoClientsUsingSameConsumerId", "blockedByBug-949990" },
			enabled = false)
	public void testRegisterTwoClientsUsingSameConsumerId() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerid = clienttasks.getCurrentConsumerId();
		clienttasks.subscribe(true, null, (String) null, null, null, null, null, null, null, null, null, null, null);
		List<ProductSubscription> consumedSubscriptionOnFirstMachine = clienttasks
				.getCurrentlyConsumedProductSubscriptions();
		client2tasks.register(sm_clientUsername, sm_clientPassword, null, null, null, null, consumerid, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String result = clienttasks.identity(null, null, null, null, null, null, null, null).getStdout();
		List<ProductSubscription> consumedSubscriptionOnSecondMachine = clienttasks
				.getCurrentlyConsumedProductSubscriptions();
		Assert.assertEquals(consumedSubscriptionOnFirstMachine, consumedSubscriptionOnSecondMachine);
		result = clienttasks.getCurrentConsumerId();
		Assert.assertEquals(result.trim(), consumerid);
		Assert.assertEquals(result.trim(), clienttasks.msg_ConsumerNotRegistered);

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21919", "RHEL7-51780"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify proxy option in repos list ",
			groups = {"Tier3Tests","ProxyOptionForRepos" },
			enabled = true)
	public void testProxyOptionForRepos() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.subscribe(true, null, (String) null, null, null, null, null, null, null, null, null, null, null);
		String result = clienttasks.repos_(true, null, null, (String) null, (String) null,
				sm_basicauthproxyHostname + ":" + sm_basicauthproxyPort, null, null, null).getStdout();
		String expectedMessage = "Network error, unable to connect to server." + "\n"
				+ "Please see /var/log/rhsm/rhsm.log for more information.";
		Assert.assertNotSame(result.trim(), expectedMessage);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21898", "RHEL7-51759"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify Future subscription added to the activation key ",
			groups = {"Tier3Tests","AddingFutureSubscriptionToActivationKey","blockedByBug-1440180" ,"blockedByBug-1555582"},
			enabled = true)
	public void testAddingFutureSubscriptionToActivationKey() throws Exception {
		Integer addQuantity = 1;
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();

		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
		consumerId);
		clienttasks.autoheal(null, null, true, null, null, null, null);
		String futurePool = createTestPool(60 * 24 *365, 60 * 24 *(365*2),true);
		String name = String.format("%s_%s-ActivationKey%s", sm_clientUsername, sm_clientOrg,
				System.currentTimeMillis());
		Map<String, String> mapActivationKeyRequest = new HashMap<String, String>();
		mapActivationKeyRequest.put("name", name);
		JSONObject jsonActivationKeyRequest = new JSONObject(mapActivationKeyRequest);
		JSONObject jsonActivationKey = new JSONObject(
		CandlepinTasks.postResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,
						"/owners/" + sm_clientOrg + "/activation_keys", jsonActivationKeyRequest.toString()));
		new JSONObject(CandlepinTasks.postResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword,sm_serverUrl, "/activation_keys/" + 
						jsonActivationKey.getString("id") + "/pools/"+ futurePool + 
						(addQuantity == null ? "" : "?quantity=" + addQuantity),null));
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(null, null, sm_clientOrg, null, null, null, null, null, null, null, name, null, null, null,
				true, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null);
		clienttasks.listConsumedProductSubscriptions();
		InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId",
			"37060", clienttasks.getCurrentlyInstalledProducts());
		Assert.assertEquals(installedProduct.status, "Future Subscription");
		
	}



	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@SuppressWarnings("deprecation")
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21987", "RHEL7-51849"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "create virt-only pool and check if lists on available list of physical and virtual machine",
			groups = {"Tier3Tests","createVirtOnlyPool","blockedByBug-1555582"},
			enabled = true)
	public void testCreateVirtOnlyPool() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);

		String consumerId = clienttasks.getCurrentConsumerId();
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				consumerId);
		String name, productId;
		List<String> providedProductIds = new ArrayList<String>();
		name = "Test product to check virt_only pool";
		productId = "test-product";
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.clear();
		attributes.put("version", "1.0");
		attributes.put("variant", "server");
		attributes.put("arch", "ALL");
		attributes.put("warning_period", "30");
		attributes.put("type", "MKT");
		attributes.put("type", "SVC");
		attributes.put("virt_only", "true");
		providedProductIds.add("37060");
		Calendar endCalendar = new GregorianCalendar();
		endCalendar.set(Calendar.HOUR_OF_DAY, 0);
		endCalendar.set(Calendar.MINUTE, 0);
		endCalendar.set(Calendar.SECOND, 0); // avoid times in the middle of the
		// day
	
		int startingMinutesFromNow = -1 * 24 * 60;
		int endingMinutesFromNow = 15 * 24 * 60;
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, productId);
		String resourcePath = "/products/" + productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);

		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
			sm_clientOrg, name + " BITS", productId, 1, attributes, null);
		
		@SuppressWarnings("deprecation")
		String poolId = CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,
			sm_serverAdminPassword, sm_serverUrl, ownerKey, 3, startingMinutesFromNow,endingMinutesFromNow ,
			getRandInt(), getRandInt(), productId, providedProduct, null).getString("id");
		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,
			sm_serverAdminPassword, sm_serverUrl, ownerKey);
		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
			sm_serverUrl, jobDetail, "FINISHED", 5 * 1000, 1);
		String isGuest = clienttasks.getFactValue("virt.is_guest");
		if (isGuest.equalsIgnoreCase("true")) {
		    Assert.assertContainsMatch(clienttasks.getCurrentlyAllAvailableSubscriptionPools().toString(), poolId);
		} else if (isGuest.equalsIgnoreCase("False")) {
		    Assert.assertContainsNoMatch(clienttasks.getCurrentlyAllAvailableSubscriptionPools().toString(), poolId);
		}

		// Note: After attaching this subscription in the
		// subscription-manager-gui, the date range is yellow and an exclamation
		// point icon is displayed in the "My Subscriptions" tab to show the
		// attached subscription is about to expire.
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21912", "RHEL7-51773"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify Facts for change in OS ",
			groups = {"Tier3Tests","FactsForChangeIn_OS"},
			enabled = true)
	@ImplementsNitrateTest(caseId = 56387)
	public void testFactsForChangeInOS() throws Exception {
		String originalhostname = clienttasks.hostname;
		String changedHostname = "redhat";
		String result = clienttasks.getFactValue("network.hostname");
		Assert.assertEquals(result, originalhostname, " Fact matches the hostname");
		client.runCommandAndWait("hostname " + changedHostname);
		result = clienttasks.getFactValue("network.hostname");
		Assert.assertEquals(result, changedHostname, " Fact matches the hostname(After changing the hostname..)");

	}

	@AfterGroups(groups = { "setup" }, value = "FactsForChangeIn_OS")
	public void restoreHostnameAfterFactsForChangeIn_OS() {
		if (clienttasks != null)
			client.runCommandAndWait("hostname " + clienttasks.hostname);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21902", "RHEL7-51763"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Consumer unsubscribed when Subscription revoked",
			groups = {"Tier3Tests", "CRLTest", "blockedByBug-1389559","blockedByBug-1399356" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 55355)
	public void testCRL() {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);
		List<SubscriptionPool> availPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(
				availPools.get(randomGenerator.nextInt(availPools.size())), sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl);

		BigInteger serialNumber = clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCertFile);

		clienttasks.unsubscribe(null, serialNumber, null, null, null, null, null);
		sleep(2/* min */ * 90 * 1000); // give the server time to update;
		// schedule is set in
		// /etc/candlepin/candlepin.conf
		// pinsetter.org.candlepin.pinsetter.tasks.CertificateRevocationListTask.schedule=0
		// 0/2 * * * ?
		RevokedCert revokedCert = RevokedCert.findFirstInstanceWithMatchingFieldFromList("serialNumber", serialNumber,
				servertasks.getCurrentlyRevokedCerts());
		Assert.assertNotNull(revokedCert,
				"Found expected Revoked Cert on the server's Certificate Revocation List (CRL) after unsubscribing from serial '"
						+ serialNumber + "'.");
		log.info("Verified revoked certificate: " + revokedCert);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@SuppressWarnings("deprecation")
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21905", "RHEL7-51766"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Consumer unsubscribed when Subscription revoked",
			groups = {"Tier3Tests","ConsumerUnsubscribedWhenSubscriptionRevoked", "blockedByBug-947429","blockedByBug-1555582" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 56025)
	public void testConsumerUnsubscribedWhenSubscriptionRevoked() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				consumerId);
		String name, productId;
		List<String> providedProductIds = new ArrayList<String>();
		name = "Test product to check subscription-removal";
		productId = "test-product";
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.clear();
		attributes.put("version", "1.0");
		attributes.put("variant", "server");
		attributes.put("arch", "ALL");
		attributes.put("warning_period", "30");
		attributes.put("type", "MKT");
		attributes.put("type", "SVC");
		File entitlementCertFile = null;
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, productId);
		String resourcePath = "/products/" + productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				sm_clientOrg, name + " BITS", productId, 1, attributes, null);
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, 20, -1 * 24 * 60/* 1 day ago */,
				15 * 24 * 60/* 15 days from now */, getRandInt(), getRandInt(), productId, providedProductIds, null);
		
		server.runCommandAndWait("rm -rf " + CandlepinTasks.candlepinCRLFile);
		for (SubscriptionPool pool : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
			if (pool.productId.equals(productId)) {
				entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool, sm_serverAdminUsername,
						sm_serverAdminPassword, sm_serverUrl);
			}
		}
		Assert.assertNotNull(entitlementCertFile, "Successfully created and subscribed to product subscription '"
				+ productId + "' created by and needed for this test.");
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		List<ProductSubscription> consumedSusbscription = clienttasks.getCurrentlyConsumedProductSubscriptions();

		Assert.assertFalse(consumedSusbscription.isEmpty());
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, productId);
		resourcePath = "/products/" + productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);
		sleep(2/* min */ * 60 * 1000);

		// verify the entitlement serial has been added to the CRL on the server
		List<RevokedCert> revokedCerts = servertasks.getCurrentlyRevokedCerts();
		RevokedCert revokedCert = RevokedCert.findFirstInstanceWithMatchingFieldFromList("serialNumber",
				entitlementCert.serialNumber, revokedCerts);
		Assert.assertNotNull(revokedCert,
				"The Certificate Revocation List file on the candlepin server contains an entitlement serial '"
						+ entitlementCert.serialNumber + "' to the product subscription '" + productId
						+ "' that was just deleted on the candlepin server.");

		// trigger the rhsmcertd on the system and verify the entitlement has
		// been removed
		clienttasks.run_rhsmcertd_worker(false);
		Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().isEmpty(),
				"The revoked entitlement has been removed from the system by rhsmcertd.");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description = "do not persist --serverurl option values to rhsm.conf when calling subscription-manager modules: orgs, environment, service-level",
			groups = {"Tier1Tests", "blockedByBug-889573"},
			enabled = false)
	public void testServerUrlOptionValuesInRHSMFile() throws JSONException, Exception {
		if (!(sm_serverType.equals(CandlepinType.hosted)))
			throw new SkipException("To be run against Stage only");
		String clientUsername = "stage_test_12";
		String serverurl = "subscription.rhn.stage.redhat.com:443/subscription";
		String hostnameBeforeExecution = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname");
		String portBeforeExecution = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port");
		String prefixBeforeExecution = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix");
		clienttasks.register(clientUsername, sm_clientPassword, null, null, null, null, null, null, null, null,
				(String) null, serverurl, null, null, true, null, null, null, null, null).getStdout();
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "server", "hostname".toLowerCase(), hostnameBeforeExecution });
		listOfSectionNameValues.add(new String[] { "server", "port".toLowerCase(), "8443" });
		listOfSectionNameValues.add(new String[] { "server", "prefix".toLowerCase(), "/candlepin" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		clienttasks.orgs(clientUsername, sm_clientPassword, serverurl, null, null, null, null, null);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname"),
				hostnameBeforeExecution);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port"), portBeforeExecution);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix"),
				prefixBeforeExecution);

		clienttasks.service_level(true, null, null, null, clientUsername, sm_clientPassword, null, serverurl, null,
				null, null, null, null);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname"),
				hostnameBeforeExecution);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port"), portBeforeExecution);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix"),
				prefixBeforeExecution);
		clienttasks.environments(clientUsername, sm_clientPassword, null, serverurl, null, null, null, null, null);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname"),
				hostnameBeforeExecution);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port"), portBeforeExecution);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix"),
				prefixBeforeExecution);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21942", "RHEL7-51804"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if CLI lets you set consumer name to empty string and defaults to sm_clientUsername",
			groups = {"Tier3Tests","VerifyConsumerNameTest", "blockedByBug-669395", "blockedByBug-1451003" },
			enabled = true)
	public void testRegisterAttemptsWithVariousConsumerNames() throws JSONException, Exception {
		String consumerName = "tester";
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		SSHCommandResult result = clienttasks.identity(null, null, null, null, null, null, null, null);
		Assert.assertContainsMatch(result.getStdout(), "name: " + clienttasks.hostname);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, consumerName, null, null,
				null, null, (String) null, null, null, null, true, null, null, null, null, null);
		result = clienttasks.identity(null, null, null, null, null, null, null, null);
		String expected = "name: " + consumerName;
		Assert.assertContainsMatch(result.getStdout(), expected);
		String consumerId = clienttasks.getCurrentConsumerId();
		clienttasks.clean();
		consumerName = "consumer";
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, consumerName, consumerId,
				null, null, null, (String) null, null, null, null, null, null, null, null, null, null);
		result = clienttasks.identity(null, null, null, null, null, null, null, null);
		Assert.assertContainsMatch(result.getStdout(), expected);
		clienttasks.clean();
		result = clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, "", consumerId,
				null, null, null, (String) null, null, null, null, true, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.8-1")) { // post
			// commit
			// df95529a5edd0be456b3528b74344be283c4d258
			Assert.assertEquals(result.getStderr().trim(), "Error: system name can not be empty.", "stderr");
		} else {
			Assert.assertEquals(result.getStdout().trim(), "Error: system name can not be empty.", "stdout");
		}
		result = clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, "", null, null,
				null, null, (String) null, null, null, null, true, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.8-1")) { // post
			// commit
			// df95529a5edd0be456b3528b74344be283c4d258
			Assert.assertEquals(result.getStderr().trim(), "Error: system name can not be empty.", "stderr");
		} else {
			Assert.assertEquals(result.getStdout().trim(), "Error: system name can not be empty.", "stdout");
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21939", "RHEL7-51801"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if CLI auto-subscribe tries to re-use basic auth credentials.",
			groups = {"Tier3Tests","VerifyAutosubscribeReuseBasicAuthCredntials", "blockedByBug-707641","blockedByBug-919700" },
			enabled = true)
	public void testAutosubscribeReuseBasicAuthCredentials() throws JSONException, Exception {
		if (!(sm_serverType.equals(CandlepinType.standalone))) throw new SkipException("This test was designed for execution against an opremise candlepin server.");
		servertasks.updateConfFileParameter("log4j.logger.org.candlepin.policy.js.compliance", "DEBUG");
		servertasks.updateConfFileParameter("log4j.logger.org.candlepin", "DEBUG");
		servertasks.restartTomcat();
		sleep(1*50*1000);// adding buffer time for tomcat to be up and running
	   	File tomcatLogFile = servertasks.getTomcatLogFile();
		String LogMarker = System.currentTimeMillis()
				+ " Testing VerifyAutosubscribeReuseBasicAuthCredntials ********************************";
		RemoteFileTasks.markFile(server, tomcatLogFile.getPath(), LogMarker);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String logMessage = " Authentication check for /consumers/" + clienttasks.getCurrentConsumerId() + "/entitlements";
		Assert.assertTrue(RemoteFileTasks
				.getTailFromMarkedFile(server, tomcatLogFile.getPath(), LogMarker, logMessage).trim().equals(""));
		 servertasks.updateConfFileParameter("log4j.logger.org.candlepin.policy.js.compliance", "INFO");
	    	 servertasks.updateConfFileParameter("log4j.logger.org.candlepin", "INFO");
	}
	
	@AfterGroups(groups = { "setup" }, value = { "VerifyAutosubscribeReuseBasicAuthCredntials" })
	@AfterClass(groups = "setup") // called after class for insurance
	public void restoreCandlepinConfFileParameters() {
		if (sm_serverType.equals(CandlepinType.standalone)) {
			servertasks.updateConfFileParameter("log4j.logger.org.candlepin.policy.js.compliance", "INFO");
			servertasks.updateConfFileParameter("log4j.logger.org.candlepin", "INFO");
			servertasks.restartTomcat();
			sleep(200);
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	// To be tested against stage
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-20115", "RHEL7-55207"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description = "verify if 500 errors in stage on subscribe/unsubscribe",
			groups = {"Tier1Tests", "blockedByBug-878994"},
			enabled = true)
	public void test500ErrorOnStage() throws JSONException, Exception {
		if (!(sm_serverType.equals(CandlepinType.hosted)))
			throw new SkipException("To be run against Stage only");
		String logMessage = "remote server status code: 500";
		String serverUrl = getServerUrl(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname"),
				clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "port"),
				clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "prefix"));
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, serverUrl, null, null, null, null, null, null, null, null);
		String LogMarker = System.currentTimeMillis()
				+ " Testing ***************************************************************";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, LogMarker);
		// String
		// result=clienttasks.listAvailableSubscriptionPools().getStdout();
		String result = clienttasks.list_(null, true, null, null, null, null, null, null, null, null, null, null, null, null, null)
				.getStdout();
		Assert.assertTrue(RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, LogMarker, logMessage)
				.trim().equals(""));
		Assert.assertNoMatch(result.trim(), clienttasks.msg_NetworkErrorCheckConnection);
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, LogMarker);
		result = clienttasks.subscribe_(true, (String) null, (String) null, (String) null, null, null, null, null, null,
				null, null, null, null).getStdout();
		Assert.assertTrue(RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, LogMarker, logMessage)
				.trim().equals(""));
		Assert.assertNoMatch(result.trim(), clienttasks.msg_NetworkErrorCheckConnection);
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, LogMarker);
		result = clienttasks.unregister(null, null, null, null).getStdout();
		Assert.assertTrue(RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, LogMarker, logMessage)
				.trim().equals(""));
		Assert.assertNoMatch(result.trim(), clienttasks.msg_NetworkErrorCheckConnection);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21920", "RHEL7-51781"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if redhat repo is created subscription-manager yum plugin when the repo is not present",
			groups = {"Tier3Tests","RedhatrepoNotBeingCreated", "blockedByBug-886992", "blockedByBug-919700" },
			enabled = true)
	public void testRedhatRepoNotBeingCreated() throws JSONException, Exception {
		client.runCommandAndWait("mv /etc/yum.repos.d/redhat.repo /root/").getStdout();
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "rhsm", "manage_repos".toLowerCase(), "1" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.subscribe(true, null, (String) null, null, null, null, null, null, null, null, null, null, null);
		Assert.assertTrue(RemoteFileTasks.testExists(client, "/etc/yum.repos.d/redhat.repo"));
		String result = client.runCommandAndWait("yum repolist all").getStdout();
		Assert.assertContainsMatch(result, "repo id");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-26774", "RHEL7-55317"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if  insecure in rhsm.comf getse updated when using --insecure option if command fails",
			groups = {"Tier3Tests","InsecureValueInRHSMConfAfterRegistrationFailure", "blockedByBug-916369" },
			enabled = true)
	public void testInsecureValueInRHSMConfAfterRegistrationFailure() throws JSONException, Exception {
		String defaultHostname = "rhel7.com";
		String defaultPort = "8443";
		String defaultPrefix = "candlepin";
		String org = "foo";
		String valueBeforeRegister = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "insecure");
		clienttasks.register_(sm_clientUsername, sm_clientPassword, org, null, null, null, null, null, null, null,
				(String) null, defaultHostname + ":" + defaultPort + "/" + defaultPrefix, null, null, null, null, null,
				null, null, null);
		String valueAfterRegister = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "insecure");
		Assert.assertEquals(valueBeforeRegister, valueAfterRegister);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21922", "RHEL7-51783"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if subscription-manager register fails with consumerid and activationkey specified",
			groups = {"Tier3Tests","RegisterActivationKeyAndConsumerID", "blockedByBug-749636" },
			enabled = true)
	public void testRegisterActivationKeyAndConsumerID() throws JSONException, Exception {
		String name = String.format("%s_%s-ActivationKey%s", sm_clientUsername, sm_clientOrg,
				System.currentTimeMillis());
		Map<String, String> mapActivationKeyRequest = new HashMap<String, String>();
		mapActivationKeyRequest.put("name", name);
		JSONObject jsonActivationKeyRequest = new JSONObject(mapActivationKeyRequest);
		JSONObject jsonActivationKey = new JSONObject(
				CandlepinTasks.postResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,
						"/owners/" + sm_clientOrg + "/activation_keys", jsonActivationKeyRequest.toString()));
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerID = clienttasks.getCurrentConsumerId();
		clienttasks.unregister(null, null, null, null);
		SSHCommandResult registerResult = clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg,
				null, null, null, consumerID, null, null, null, jsonActivationKey.get("name").toString(), null, null,
				null, null, null, null, null, null, null);
		String expected = "Error: Activation keys do not require user credentials.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.9-1")) { // post
			// commit
			// a695ef2d1da882c5f851fde90a24f957b70a63ad
			Assert.assertEquals(registerResult.getStderr().trim(), expected, "stderr");
		} else {
			Assert.assertEquals(registerResult.getStdout().trim(), expected, "stdout");
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21918", "RHEL7-51779"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify Product id is displayed in installed list",
			groups = {"Tier3Tests","ProductIdInInstalledList","blockedByBug-803386" },
			enabled = true)
	public void testInstalledListIncludesProductIDs() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		for (InstalledProduct result : clienttasks.getCurrentlyInstalledProducts()) {
			Assert.assertNotNull(result.productId);

		}

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-26775", "RHEL7-55318"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = /*TODO */"please provide a description",
			groups = {"Tier3Tests","ServerURLInRHSMFile", "blockedByBug-916353" },
			enabled = true)
	public void testServerUrlInRhsmFile() throws JSONException, Exception {
		String defaultHostname = "rhel7.com";
		String defaultPort = "8443";
		String defaultPrefix = "/candlepin";
		String org = "foo";
		String valueBeforeRegister = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname");
		clienttasks.register_(sm_clientUsername, sm_clientPassword, org, null, null, null, null, null, null, null,
				(String) null, defaultHostname + ":" + defaultPort + "/" + defaultPrefix, null, null, null, null, null,
				null, null, null);
		String valueAfterRegister = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "hostname");
		Assert.assertEquals(valueBeforeRegister, valueAfterRegister);

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21908", "RHEL7-51769"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Implicitly using the consumer cert from a currently registered system, attempt to query the available service levels on a different candlepin server.",
			groups = {"Tier3Tests","DipslayServicelevelWhenRegisteredToDifferentServer", "blockedByBug-916362" },
			enabled = true)
	public void testDisplayServiceLevelWhenRegisteredToDifferentServer() {
		String defaultHostname = "subscription.rhn.redhat.com";
		String defaultPort = "443";
		String defaultPrefix = "/subscription";
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		SSHCommandResult result = clienttasks.service_level_(null, null, null, null, null, null, null,
				defaultHostname + ":" + defaultPort + defaultPrefix, null, null, null, null, null);
		String expectedResult = "You are already registered to a different system";
		if (/* bug 916362 is CLOSED NOTABUG is */true) {
			log.warning("Altering the original expected result '" + expectedResult
					+ "' since Bug 916362 has been CLOSED NOTABUG.");
			log.warning("For more explanation see https://bugzilla.redhat.com/show_bug.cgi?id=916362#c3");
			expectedResult = "Unable to verify server's identity: tlsv1 alert unknown ca";
		}
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.15.0-1"))
			expectedResult = "Invalid credentials.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.9-1")) { // post
			// commit
			// a695ef2d1da882c5f851fde90a24f957b70a63ad
			Assert.assertEquals(result.getStderr().trim(), expectedResult, "stderr");
		} else {
			Assert.assertEquals(result.getStdout().trim(), expectedResult, "stdout");
		}

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21910", "RHEL7-51771"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify expiration of entitlement certs",
			groups = {"Tier3Tests","ExpirationOfEntitlementCerts","blockedByBug-907638", "blockedByBug-953830" , "blockedByBug-1555582"},
			enabled = true)
	public void testExpirationOfEntitlementCerts() throws JSONException, Exception {
		int endingMinutesFromNow = 1;
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);

		String consumerId = clienttasks.getCurrentConsumerId();
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				consumerId);
		Calendar endCalendar = new GregorianCalendar();
		endCalendar.add(Calendar.MINUTE, endingMinutesFromNow);
		Date endDate = endCalendar.getTime(); // caution - if the next call to
		// createTestPool does not occur
		// within this minute; endDate
		// will be 1 minute behind
		// reality
		String expiringPoolId = createTestPool(-60 * 24, endingMinutesFromNow,false);
		Calendar c1 = new GregorianCalendar();
		SubscriptionPool expiringSubscriptionPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList(
				"poolId", expiringPoolId, clienttasks.getCurrentlyAvailableSubscriptionPools());

		// attaching from the pool that is about to expire should still be
		// successful
		File expiringEntitlementFile = clienttasks.subscribeToSubscriptionPool(expiringSubscriptionPool,
				sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl);
		clienttasks.unsubscribe_(null, clienttasks.getSerialNumberFromEntitlementCertFile(expiringEntitlementFile),
				null, null, null, null, null);
		Calendar c2 = new GregorianCalendar();

		// wait for the pool to expire
		// sleep(endingMinutesFromNow*60*1000);
		// trying to reduce the wait time for the expiration by subtracting off
		// some expensive test time
		sleep(1 * 60 * 1000 - (c2.getTimeInMillis() - c1.getTimeInMillis()));
		// attempt to attach an entitlement from the same pool which is now
		// expired
		String result = clienttasks
				.subscribe_(null, null, expiringPoolId, null, null, null, null, null, null, null, null, null, null)
				.getStdout();
		// Stdout: Unable to attach pool with ID
		// '8a908740438be86501438cd57718376c'.: Subscriptions for
		// awesomeos-onesocketib expired on: 1/3/14 1:21 PM.
		String expiredOnDatePattern = "M/d/yy h:mm a"; // 1/3/14 1:21 PM
		DateFormat expiredOnDateFormat = new SimpleDateFormat(expiredOnDatePattern);
		String expiredOnString = expiredOnDateFormat.format(endDate.getTime());
		String expectedStdout = "Unable to entitle consumer to the pool with id '" + expiringPoolId
				+ "'.: Subscriptions for " + expiringSubscriptionPool.productId + " expired on: " + expiredOnString;
		expectedStdout = String.format("Unable to attach pool with ID '%s'.: Subscriptions for %s expired on: %s.", expiringSubscriptionPool.poolId, expiringSubscriptionPool.productId, expiredOnString);
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
			expectedStdout = expectedStdout.replaceAll("'", "\"");	// replace single quotes with double quotes
		}
		if (!result.trim().equals(expectedStdout)) {
			String alternativeStdout = String.format("Pool with id %s could not be found.", expiringPoolId);
			Assert.assertEquals(result.trim(), alternativeStdout,
					"Normally, when a pool expires and we attempt to attach it, the result will be '" + expectedStdout
							+ "', however if the candlepin certificate revocation job swoops in immediately before our assertion and cleans out the expired pools, then this will be the expected result.");
		} else
			Assert.assertEquals(result.trim(), expectedStdout);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21928", "RHEL7-51789"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if Entitlement certs are downloaded if subscribed to expired pool",
			groups = {"Tier3Tests","SubscribeToexpiredEntitlement", "blockedByBug-907638","blockedByBug-1555582" },
			enabled = true)
	public void testSubscribeToExpiredEntitlement() throws JSONException, Exception {

		int endingMinutesFromNow = 1;
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				consumerId);
		String expiringPoolId = createTestPool(-60 * 24, endingMinutesFromNow,false);
		sleep(1 * 59 * 1000);
		clienttasks.subscribe_(null, null, expiringPoolId, null, null, null, null, null, null, null, null, null, null);
		Assert.assertTrue(clienttasks.getCurrentlyConsumedProductSubscriptions().isEmpty());
		Assert.assertTrue(clienttasks.getCurrentEntitlementCertFiles().isEmpty());
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@SuppressWarnings("unused")
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21916", "RHEL7-51777"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if no multiple repos are created,if subscribed to a product that share one or more engineering subscriptions",
			groups = {"Tier3Tests","NoMultipleReposCreated" },
			enabled = true)
	public void testNoMultipleReposCreated() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				consumerId);
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, "multi-stackable");
		CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				ownerKey);
		String resourcePath = "/products/multi-stackable";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);
		Calendar calendar = new GregorianCalendar(); // right now
		Date todaysDate = calendar.getTime();
		calendar.add(Calendar.YEAR, 1);
		calendar.add(Calendar.DATE, 10);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0); // avoid times in the middle of the
		// day
		Date futureDate = calendar.getTime();
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("sockets", "8");
		attributes.put("arch", "ALL");
		attributes.put("type", "MKT");
		attributes.put("multi-entitlement", "yes");
		attributes.put("stacking_id", "726409");
		List<String> providedProducts = new ArrayList<String>();
		providedProducts.add("100000000000002");
		providedProducts.add("100000000000001");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				sm_clientOrg, "Multi-Stackable", "multi-stackable", 1, attributes, null);
		String requestBody = CandlepinTasks
				.createSubscriptionRequestBody(sm_serverUrl, 20, todaysDate, futureDate,
						"multi-stackable", Integer.valueOf(getRandInt()), Integer.valueOf(getRandInt()), providedProducts, null)
				.toString();
		CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				"/owners/" + ownerKey + "/subscriptions", requestBody);
		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, ownerKey);
		CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, jobDetail, "FINISHED", 5 * 1000, 1);
		sleep(3 * 60 * 1000);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		for (SubscriptionPool pools : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if (pools.subscriptionName.equals("Multi-Stackable")) {
				clienttasks.subscribe_(null, null, pools.poolId, null, null, null, null, null, null, null, null, null, null);
			}
		}
		String productIdOne = null;
		List<Repo> originalRepos = clienttasks.getCurrentlySubscribedRepos();
		for (Repo repo : originalRepos) {
			String productIdTwo = null;
			productIdOne = repo.repoId;
			if (!(productIdTwo == null)) {
				Assert.assertNotSame(repo.repoId, productIdOne);
			}
			productIdTwo = productIdOne;
		}
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, "multi-stackable");
		CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				ownerKey);
		resourcePath = "/products/multi-stackable";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21906", "RHEL7-51767"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify that a content set can be deleted after being added to a product.",
			groups = {"Tier3Tests","DeleteContentSourceFromProduct", "blockedByBug-687970", "blockedByBug-834125" },
			enabled = true)
	public void testDeleteContentSourceFromProduct() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		List<String> modifiedProductIds = null;
		String contentId = "99999";
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("sockets", "8");
		attributes.put("arch", "ALL");
		JSONObject jsonContentResource;
		String requestBody = CandlepinTasks.createContentRequestBody("fooname", contentId, "foolabel", "yum",
				"Foo Vendor", "/foo/path", "/foo/path/gpg", null, null, null, modifiedProductIds).toString();
		String resourcePath = "/content/";
		resourcePath = "/content/" + contentId;
		String contentWithIdMessage = "Content with id " + contentId + " could not be found.";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.7"))
			contentWithIdMessage = "Content with ID \"" + contentId + "\" could not be found."; // commit
		// 6b63e346c61789837211828043ad9576a756d0e8
		resourcePath = "/content/" + contentId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);
		resourcePath = "/content/" + contentId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		jsonContentResource = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, resourcePath));
		Assert.assertEquals(jsonContentResource.getString("displayMessage"), contentWithIdMessage);
		requestBody = CandlepinTasks.createContentRequestBody("fooname", contentId, "foolabel", "yum", "Foo Vendor",
				"/foo/path", "/foo/path/gpg", null, null, null, modifiedProductIds).toString();
		resourcePath = "/content";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath, requestBody);
		resourcePath = "/products/fooproduct";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath); // in case it already exists from prior run
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				sm_clientOrg, "fooname", "fooproduct", null, attributes, null);
		resourcePath = "/products/fooproduct/content/" + contentId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath + "?enabled=false", null);
		resourcePath = "/products/" + "fooproduct";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		String jsonProduct = CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, resourcePath);
		Assert.assertContainsMatch(jsonProduct, contentId,
				"Added content set '" + contentId + "' to product " + "fooproduct");
		resourcePath = "/content/" + contentId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);
		resourcePath = "/content/" + contentId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		jsonContentResource = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, resourcePath));
		/*
		 * This assertion is reinforcing a bug in candlepin. The prior call to
		 * delete content 99999 should always pass even when it has been added
		 * to a product. This was fixed by
		 * https://bugzilla.redhat.com/show_bug.cgi?id=834125#c17 Changing the
		 * assertion to Assert.assertContainsMatch
		 * Assert.assertContainsNoMatch(jsonActivationKey.toString(),
		 * contentWithIdMessage);
		 */
		Assert.assertEquals(jsonContentResource.getString("displayMessage"), contentWithIdMessage);
		resourcePath = "/products/" + "fooproduct";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		jsonProduct = CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, resourcePath);
		Assert.assertContainsNoMatch(jsonProduct, contentId,
				"After deleting content set '" + contentId + "', it was removed from the product " + "fooproduct");
		resourcePath = "/products/fooproduct";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21940", "RHEL7-51802"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify that bind and unbind event is recorded in syslog",
			groups = {"Tier3Tests","VerifyBindAndUnbindInSyslog", "blockedByBug-919700" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 68740)
	public void testBindAndUnbindInSyslog() throws JSONException, Exception {
	    	clienttasks.clean();
	    //    clienttasks.unregister(null, null, null, null);
		String logMarker, expectedSyslogMessage, tailFromSyslogFile;

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);

		logMarker = System.currentTimeMillis() + " Testing Subscribe **********************";
		clienttasks.markSystemLogFile(logMarker);
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		List<String> poolIds = new ArrayList<String>();
		for (SubscriptionPool pool : pools)
			poolIds.add(pool.poolId);
		clienttasks.subscribe(null, null, poolIds, null, null, null, null, null, null, null, null, null, null);
		tailFromSyslogFile = clienttasks.getTailFromSystemLogFile(logMarker,null);
		for (SubscriptionPool pool : pools) {
			expectedSyslogMessage = String.format("%s: Added subscription for '%s' contract '%s'", clienttasks.command,
					pool.subscriptionName, pool.contract.isEmpty() ? "None" : pool.contract);
			Assert.assertTrue(tailFromSyslogFile.contains(expectedSyslogMessage),
					"After subscribing to '" + pool.subscriptionName + "', syslog '" + clienttasks.messagesLogFile
							+ "' contains expected message '" + expectedSyslogMessage + "'.");
			for (String providedProduct : pool.provides) {
				// TEMPORARY WORKAROUND FOR BUG:
				// https://bugzilla.redhat.com/show_bug.cgi?id=1016300
				if (providedProduct.equals("Awesome OS Server Bundled")) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId = "1016300";
					try {
						if (invokeWorkaroundWhileBugIsOpen && BzChecker.getInstance().isBugOpen(bugId)) {
							log.fine("Invoking workaround for " + BzChecker.getInstance().getBugState(bugId).toString()
									+ " Bugzilla " + bugId + ".  (https://bugzilla.redhat.com/show_bug.cgi?id=" + bugId
									+ ")");
							SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
						} else {
							invokeWorkaroundWhileBugIsOpen = false;
						}
					} catch (BugzillaAPIException be) {
						/* ignore exception */} catch (RuntimeException re) {
						/* ignore exception */} 
					if (invokeWorkaroundWhileBugIsOpen) {
						log.warning("Ignoring the provided MKT product '" + providedProduct
								+ "'.  No syslog assertion for this product will be made.");
						continue;
					}
				}
				// END OF WORKAROUND
				expectedSyslogMessage = String.format("%s: Added subscription for product '%s'", clienttasks.command,
						providedProduct);
				Assert.assertTrue(tailFromSyslogFile.contains(expectedSyslogMessage),
						"After subscribing to '" + pool.subscriptionName + "', syslog '" + clienttasks.messagesLogFile
								+ "' contains expected message '" + expectedSyslogMessage + "'.");
			}
		}

		logMarker = System.currentTimeMillis() + " Testing Unsubscribe **********************";
		clienttasks.markSystemLogFile(logMarker);
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
		tailFromSyslogFile = clienttasks.getTailFromSystemLogFile(logMarker,null);
		for (ProductSubscription productSubscription : productSubscriptions) {
			// Feb 3 13:32:34 jsefler-7 subscription-manager: Removed
			// subscription for 'Awesome OS Server Bundled (2 Sockets, Standard
			// Support)' contract '3'
			// Feb 3 13:32:34 jsefler-7 subscription-manager: Removed
			// subscription for product 'Clustering Bits'
			// Feb 3 13:32:34 jsefler-7 subscription-manager: Removed
			// subscription for product 'Awesome OS Server Bits'
			// Feb 3 13:32:34 jsefler-7 subscription-manager: Removed
			// subscription for product 'Load Balancing Bits'
			// Feb 3 13:32:34 jsefler-7 subscription-manager: Removed
			// subscription for product 'Large File Support Bits'
			// Feb 3 13:32:34 jsefler-7 subscription-manager: Removed
			// subscription for product 'Shared Storage Bits'
			// Feb 3 13:32:34 jsefler-7 subscription-manager: Removed
			// subscription for product 'Management Bits'
			expectedSyslogMessage = String.format("%s: Removed subscription for '%s' contract '%s'",
					clienttasks.command, productSubscription.productName,
					productSubscription.contractNumber == null ? "None" : productSubscription.contractNumber); // Note
			// that
			// a
			// null/missing
			// contract
			// will
			// be
			// reported
			// as
			// None.
			// Seems
			// reasonable.
			Assert.assertTrue(tailFromSyslogFile.contains(expectedSyslogMessage),
					"After unsubscribing from '" + productSubscription.productName + "', syslog '"
							+ clienttasks.messagesLogFile + "' contains expected message '" + expectedSyslogMessage
							+ "'.");
			for (String providedProduct : productSubscription.provides) {
				expectedSyslogMessage = String.format("%s: Removed subscription for product '%s'", clienttasks.command,
						providedProduct);
				Assert.assertTrue(tailFromSyslogFile.contains(expectedSyslogMessage),
						"After unsubscribing from '" + productSubscription.productName + "', syslog '"
								+ clienttasks.messagesLogFile + "' contains expected message '" + expectedSyslogMessage
								+ "'.");
			}
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21964", "RHEL7-51826"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if register and unregister event is recorded in syslog",
			groups = {"Tier3Tests","VerifyRegisterAndUnregisterInSyslog" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 68749)
	public void testRegisterAndUnregisterInSyslog() throws JSONException, Exception {
		String logMarker, expectedSyslogMessage, tailFromSyslogFile;

		logMarker = System.currentTimeMillis() + " Testing Register **********************";
		clienttasks.markSystemLogFile(logMarker);
		String identity = clienttasks.getCurrentConsumerId(
				clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null,
						null, null, (String) null, null, null, null, true, false, null, null, null, null));
		tailFromSyslogFile = clienttasks.getTailFromSystemLogFile(logMarker,null);
		// Feb 3 12:50:47 jsefler-7 subscription-manager: Registered system with
		// identity: eddfaf6d-e916-49e3-aa71-e33a2c54e1dd
		expectedSyslogMessage = String.format("%s: Registered system with identity: %s", clienttasks.command, identity);
		Assert.assertTrue(tailFromSyslogFile.contains(expectedSyslogMessage.trim()), "After registering', syslog '"
				+ clienttasks.messagesLogFile + "' contains expected message '" + expectedSyslogMessage + "'.");

		logMarker = System.currentTimeMillis() + " Testing Unregister **********************";
		clienttasks.markSystemLogFile(logMarker);
		clienttasks.unregister(null, null, null, null);
		tailFromSyslogFile = clienttasks.getTailFromSystemLogFile(logMarker,null);
		// Feb 3 13:39:21 jsefler-7 subscription-manager: Unregistered machine
		// with identity: 231c2b52-4bc8-4458-8d0a-252b1dd82877
		expectedSyslogMessage = String.format("%s: Unregistered machine with identity: %s", clienttasks.command,
				identity);
		Assert.assertTrue(tailFromSyslogFile.contains(expectedSyslogMessage.trim()), "After unregistering', syslog '"
				+ clienttasks.messagesLogFile + "' contains expected message '" + expectedSyslogMessage + "'.");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21941", "RHEL7-51803"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify that Consumer Account And Contract Id are Present in the consumed list",
			groups = {"Tier3Tests","VerifyConsumerAccountAndContractIdPresence" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 68738)
	public void testConsumerAccountAndContractIdPresence() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		for (ProductSubscription consumed : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			Assert.assertNotNull(consumed.accountNumber);
			Assert.assertNotNull(consumed.contractNumber);

		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@SuppressWarnings("deprecation")
	@Test(	description = "verify that system should not be compliant for an expired subscription",
			groups = {"Tier3Tests","VerifySubscriptionOf"},
			enabled = false)
	// @ImplementsNitrateTest(caseId=71208)
	public void testSubscriptionOfBestProductWithUnattendedRegistration() throws JSONException, Exception {
		Map<String, String> attributes = new HashMap<String, String>();
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "rhsmcertd", "autoAttachInterval".toLowerCase(), "1440" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, "multi-stackable");
		CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				ownerKey);
		String resourcePath = "/products/multi-stackable";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);

		String consumerId = clienttasks.getCurrentConsumerId();
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				consumerId);
		Calendar cal = new GregorianCalendar();
		Date todaysDate = cal.getTime();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0); // avoid times in the middle of the day
		cal.add(Calendar.YEAR, 1);
		cal.add(Calendar.DATE, 10);
		Date futureDate = cal.getTime(); // one year and ten days from tomorrow
		attributes.put("sockets", "0");
		attributes.put("arch", "ALL");
		attributes.put("type", "MKT");
		attributes.put("multi-entitlement", "yes");
		attributes.put("stacking_id", "726409");
		List<String> providedProducts = new ArrayList<String>();
		providedProducts.add("100000000000002");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				sm_clientOrg, "Multi-Stackable for 100000000000002", "multi-stackable", 1, attributes, null);
		String requestBody = CandlepinTasks
				.createSubscriptionRequestBody(sm_serverUrl, 20, todaysDate, futureDate,
						"multi-stackable", Integer.valueOf(getRandInt()), Integer.valueOf(getRandInt()), providedProducts, null)
				.toString();
		CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				"/owners/" + ownerKey + "/subscriptions", requestBody);
		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, ownerKey);
		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, jobDetail, "FINISHED", 5 * 1000, 1);
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		sleep(3 * 60 * 1000);
		int sockets = 16;
		Map<String, String> factsMap = new HashMap<String, String>();
		factsMap.put("lscpu.cpu_socket(s)", String.valueOf(sockets));
		factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
		clienttasks.createFactsFileWithOverridingValues(factsMap);

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId",
				"100000000000002", clienttasks.getCurrentlyInstalledProducts());

		if (installedProduct.productId.equals("100000000000002"))
			Assert.assertEquals(installedProduct.status, "Subscribed");

		for (ProductSubscription consumed : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			Assert.assertEquals(consumed.productName, "Multi-Stackable for 100000000000002");
		}

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description = "verify that system should not be compliant for an expired subscription",
			groups = {"Tier3Tests","VerifySystemCompliantFact"},
			enabled = false)
	public void testSystemCompliantFactWhenAllProductsAreExpired() throws JSONException, Exception {

		List<ProductCert> productCerts = clienttasks.getCurrentProductCerts();
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);

		File expectCertFile = new File(System.getProperty("automation.dir", null) + "/certs/Expiredcert.pem");
		RemoteFileTasks.putFile(client, expectCertFile.toString(), "/root/", "0755");
		clienttasks.importCertificate_("/root/Expiredcert.pem");
		for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
			if ((installed.status.equals("Expired"))) {
				ProductCert productCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId",
						installed.productId, productCerts);
				configureTmpProductCertDirWithInstalledProductCerts(Arrays.asList(productCert));

			}
		}
		clienttasks.facts(null, true, null, null, null, null);
		List<InstalledProduct> currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
		Assert.assertEquals(currentlyInstalledProducts.size(), 1,
				"Expecting one installed product provided by the expired entitlement just imported.");
		String actual = clienttasks.getFactValue(factname).trim();
		Assert.assertEquals(actual, "invalid", "Value of system fact '" + factname + "'.");
	}

	public String getSubscriptionID(String authenticator, String password, String url, String ownerKey,
			String productId) throws JSONException, Exception {
		String subscriptionId = null;
		JSONArray jsonSubscriptions = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(authenticator, password,
				url, "/owners/" + ownerKey + "/subscriptions"));
		for (int i = 0; i < jsonSubscriptions.length(); i++) {
			JSONObject jsonSubscription = (JSONObject) jsonSubscriptions.get(i);

			JSONObject jsonProduct = jsonSubscription.getJSONObject("product");

			if (productId.equals(jsonProduct.getString("id"))) {
				subscriptionId = jsonSubscription.getString("id");
			}
		}
		return subscriptionId;
	}
	
	// FIXME This method appears to be abandoned
	@AfterGroups(groups = "setup", value = { "VerifyVirtOnlyPoolsRemoved" }, enabled = true)
	public void cleanupAfterVerifyVirtOnlyPoolsRemoved() throws Exception {
		// TODO This is not completely accurate, but it is a good place to
		// cleanup after VerifyVirtOnlyPoolsRemoved...
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				"/subscriptions/" + "virtualPool");

		String resourcePath = "/products/virtual-pool";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);
		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, sm_clientOrg);
		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, jobDetail, "FINISHED", 5 * 1000, 1);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21986", "RHEL7-51848"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if able to entitle consumer to the pool virt_only,pool_derived,bonus pool ",
			groups = {"Tier3Tests","consumeVirtOnlyPool", "blockedByBug-756628" },
			enabled = true)
	public void testConsumeVirtOnlyPool() throws JSONException, Exception {
		String isPool_derived = null;
		Boolean virtonly = false;
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (List<String>) null, (String) null, null, null, true, null, null, null, null, null);
		String isGuest = clienttasks.getFactValue("virt.is_guest");
		if (isGuest.equalsIgnoreCase("true")) {
			Map<String, String> factsMap = new HashMap<String, String>();
			factsMap.put("virt.is_guest", "False");
			clienttasks.facts(null, true, null, null, null, null);
			clienttasks.createFactsFileWithOverridingValues(factsMap);
			for (SubscriptionPool availList : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
				isPool_derived = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername, sm_clientPassword,
						sm_serverUrl, availList.poolId, "pool_derived");
				virtonly = CandlepinTasks.isPoolVirtOnly(sm_clientUsername, sm_clientPassword, availList.poolId,
						sm_serverUrl);
				if (!(isPool_derived == null) || virtonly) {
					String result = clienttasks.subscribe_(null, null, availList.poolId, null, null, null, null, null,
							null, null, null, null, null).getStdout();
					String Expected = "Pool is restricted to virtual guests: " + availList.subscriptionName;
					Assert.assertEquals(result.trim(), Expected);
				}
			}
		}

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6},
			testCaseID = {"RHEL6-26777"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify Product Red Hat Enterprise Linux Workstation has a valid stacking_id and its socket_limit is not 0 so that RHEL Workstation product is not always in a partially subscribed state even though you attach the sufficient quantity required to make system fully complaint",
			groups = {"Tier1Tests","VerifyRHELWorkstationSubscription", "blockedByBug-739790" },
			enabled = true)
	public void testRHELWorkstationSubscription() throws JSONException, Exception {
		InstalledProduct workstation = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", "71",
				clienttasks.getCurrentlyInstalledProducts());
		if (workstation == null)
			throw new SkipException(
					"This test is only applicable on a RHEL Workstation where product 71 is installed.");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (List<String>) null, (String) null, null, null, true, false, null, null, null, null);

		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		boolean assertedWorkstationProduct = false;
		for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
			if (installed.productId.contains("Workstation")) {
				Assert.assertEquals(installed.status, "subscribed");
				assertedWorkstationProduct = true;
			}
		}
		if (!assertedWorkstationProduct)
			throw new SkipException("Installed product to be tested is not available");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@SuppressWarnings("deprecation")
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21985","RHEL7-51847"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = /*TODO */"please provide a description",
			groups = {"Tier3Tests","certificateStacking", "blockedByBug-726409", "blockedByBug-1183175","blockedByBug-1555582" },
			enabled = true)
	public void testCertificateStacking() throws JSONException, Exception {
		Map<String, String> attributes = new HashMap<String, String>();
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (List<String>) null, (String) null, null, null, true, null, null, null, null, null);
		ProductCert installedProductCert100000000000002 = ProductCert.findFirstInstanceWithMatchingFieldFromList(
				"productId", "100000000000002", clienttasks.getCurrentProductCerts());
		Assert.assertNotNull(installedProductCert100000000000002,
				"Found installed product cert 100000000000002 needed for this test.");
		configureTmpProductCertDirWithInstalledProductCerts(
				Arrays.asList(new ProductCert[] { installedProductCert100000000000002 }));

		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, "multi-stackable");
		CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				ownerKey);
		String resourcePath = "/products/multi-stackable";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, "stackable");
		CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				ownerKey);
		resourcePath = "/products/stackable";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);
		int sockets = 14;
		String poolid = null;
		String validity = null;
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "rhsmcertd", "autoAttachInterval".toLowerCase(), "1440" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		Map<String, String> factsMap = new HashMap<String, String>();
		factsMap.put("virt.is_guest", String.valueOf(Boolean.FALSE));
		factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				consumerId);
		attributes.put("sockets", "2");
		attributes.put("arch", "ALL");
		attributes.put("type", "MKT");
		attributes.put("multi-entitlement", "yes");
		attributes.put("stacking_id", "726409");
		List<String> providedProducts = new ArrayList<String>();
		providedProducts.add("100000000000002");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				sm_clientOrg, "Multi-Stackable for 100000000000002", "multi-stackable", 1, attributes, null);
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, 20, -1 * 24 * 60/* 1 day ago */,
				15 * 24 * 60/* 15 days from now */, getRandInt(), getRandInt(), "multi-stackable", providedProducts,
				null);
		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, ownerKey);
		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, jobDetail, "FINISHED", 5 * 1000, 1);
		attributes.put("sockets", "4");
		attributes.put("multi-entitlement", "no");
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				sm_clientOrg, "Stackable for 100000000000002", "stackable", 1, attributes, null);
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, 20, -1 * 24 * 60/* 1 day ago */,
				15 * 24 * 60/* 15 days from now */, getRandInt(), getRandInt(), "stackable", providedProducts, null);
		jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, ownerKey);
		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, jobDetail, "FINISHED", 5 * 1000, 1);

		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
			if (availList.subscriptionName.equals("Multi-Stackable for 100000000000002")) {
				poolid = availList.poolId;
				clienttasks.subscribe_(null, null, availList.poolId, null, null, "3", null, null, null, null, null,
						null, null);
				validity = clienttasks.getFactValue(factname);
				Assert.assertEquals(validity.trim(), "partial");
			} else if (availList.subscriptionName.equals("Stackable for 100000000000002")) {
				clienttasks.subscribe_(null, null, availList.poolId, null, null, null, null, null, null, null, null,
						null, null);
				validity = clienttasks.getFactValue(factname);
				Assert.assertEquals(validity.trim(), "partial");

			}
		}
		clienttasks.subscribe_(null, null, poolid, null, null, "2", null, null, null, null, null, null, null);
		clienttasks.getCurrentlyConsumedProductSubscriptions();
		validity = clienttasks.getFactValue(factname);
		Assert.assertEquals(validity.trim(), "valid");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21917","RHEL7-51778"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify OwnerInfo is displayed only for pools that are active right now, for all the stats",
			groups = {"Tier3Tests","OwnerInfoForActivePools", "blockedByBug-710141", },
			enabled = true)
	public void testOwnerInfoForActivePools() throws JSONException, Exception {
		DateFormat yyyy_MM_dd_DateFormat = new SimpleDateFormat("yyyy-MM-dd");
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (List<String>) null, (String) null, null, null, true, null, null, null, null, null);
		Calendar now = new GregorianCalendar();
		Calendar futureCalendar = new GregorianCalendar();
		futureCalendar.set(Calendar.HOUR_OF_DAY, 0);
		futureCalendar.set(Calendar.MINUTE, 0);
		futureCalendar.set(Calendar.SECOND, 0); // avoid times in the middle of
		// the day
		futureCalendar.add(Calendar.YEAR, 1);

		String futurceDate = yyyy_MM_dd_DateFormat.format(futureCalendar.getTime());
		List<SubscriptionPool> availOnDate = getAvailableFutureSubscriptionsOndate(futurceDate);
		if (availOnDate.size() == 0)
			throw new SkipException("Sufficient future pools are not available");

		JSONArray jsonPools = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, "/owners/" + sm_clientOrg + "/pools"));
		Assert.assertTrue(jsonPools.length() > 0,
				"Successfully got a positive number of /owners/" + sm_clientOrg + "/pools");
		for (int i = 0; i < jsonPools.length(); i++) {
			JSONObject jsonPool = (JSONObject) jsonPools.get(i);
			String subscriptionName = jsonPool.getString("productName");
			String startDate = jsonPool.getString("startDate");
			String endDate = jsonPool.getString("endDate");
			Calendar startCalendar = parseISO8601DateString(startDate, "GMT"); // "startDate":"2014-01-06T00:00:00.000+0000"
			Calendar endCalendar = parseISO8601DateString(endDate, "GMT"); // "endDate":"2015-01-06T00:00:00.000+0000"
			Assert.assertTrue(startCalendar.before(now),
					"Available pool '" + subscriptionName + "' startsDate='" + startDate + "' starts before now.");
			Assert.assertTrue(endCalendar.after(now),
					"Available pool '" + subscriptionName + "' endDate='" + endDate + "' ends after now.");
		}
		jsonPools = new JSONArray(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, "/owners/" + sm_clientOrg + "/pools?activeon=" + futurceDate));
		Assert.assertTrue(jsonPools.length() > 0,
				"Successfully got a positive number of /owners/" + sm_clientOrg + "/pools?activeon=" + futurceDate);
		for (int i = 0; i < jsonPools.length(); i++) {
			JSONObject jsonPool = (JSONObject) jsonPools.get(i);
			String subscriptionName = jsonPool.getString("productName");
			String startDate = jsonPool.getString("startDate");
			String endDate = jsonPool.getString("endDate");
			Calendar startCalendar = parseISO8601DateString(startDate, "GMT"); // "startDate":"2014-01-06T00:00:00.000+0000"
			Calendar endCalendar = parseISO8601DateString(endDate, "GMT"); // "endDate":"2015-01-06T00:00:00.000+0000"
			Assert.assertTrue(startCalendar.before(futureCalendar), "Future available pool '" + subscriptionName
					+ "' startsDate='" + startDate + "' starts before " + futurceDate + ".");
			Assert.assertTrue(endCalendar.equals(futureCalendar) || endCalendar.after(futureCalendar),
					"Future available pool '" + subscriptionName + "' endDate='" + endDate + "' ends on or after "
							+ futurceDate + ".");
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description = "verify if refresh Pools w/ Auto-Create Owner Fails",
			groups = {"Tier3Tests","EnableAndDisableCertV3","blockedByBug-919700" },
			enabled = false)
	public void testEnableAndDisableCertV3() throws JSONException, Exception {
		String version = null;
		servertasks.updateConfFileParameter("candlepin.enable_cert_v3", "false");
		servertasks.restartTomcat();
		SubscriptionManagerCLITestScript.sleep(1 * 60 * 1000);
		clienttasks.restart_rhsmcertd(null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		for (EntitlementCert Cert : clienttasks.getCurrentEntitlementCerts()) {
			version = Cert.version;
			if (version.equals("1.0")) {
				Assert.assertEquals(version, "1.0");
			} else {
				servertasks.updateConfFileParameter("candlepin.enable_cert_v3", "true");
				servertasks.restartTomcat();
				Assert.fail();
			}

		}
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		servertasks.updateConfFileParameter("candlepin.enable_cert_v3", "true");
		servertasks.restartTomcat();
		clienttasks.restart_rhsmcertd(null, null, null);
		SubscriptionManagerCLITestScript.sleep(1 * 60 * 1000);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		for (EntitlementCert Cert : clienttasks.getCurrentEntitlementCerts()) {
			version = Cert.version;
			Assert.assertEquals(version, "3.2");
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21921","RHEL7-51782"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify that refresh pools w/ auto_create_owner succeeds",
			groups = {"Tier3Tests","RefreshPoolsWithAutoCreate", "blockedByBug-720487" },
			enabled = true)
	public void testRefreshPoolsWithAutoCreate() throws JSONException, Exception {
		String org = "newowner";
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				"/owners/" + org); // in case org already exists
		JSONObject jsonOrg;
		jsonOrg = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, "/owners/" + org));
		Assert.assertEquals(jsonOrg.getString("displayMessage"), "Organization with id newowner could not be found.");
		CandlepinTasks.putResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				"/owners/" + org + "/subscriptions?auto_create_owner=true");
		jsonOrg = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, "/owners/" + org));
		Assert.assertNotNull(jsonOrg.get("created"));
		jsonOrg = new JSONObject(CandlepinTasks.putResourceUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, "/owners/AnotherOwner/subscriptions?auto_create_owner=false"));
		Assert.assertEquals(jsonOrg.getString("displayMessage"), "owner with key: AnotherOwner was not found.");
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				"/owners/" + org);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-47932","RHEL7-97447"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify fix for Bug 874147 - python-ethtool api changed causing facts to list ipv4 address as \"unknown\"",
			groups = {"Tier3Tests","VerifyipV4Facts", "blockedByBug-874147" },
			enabled = true)
	public void testIPV4Facts() throws JSONException, Exception {
		Boolean pattern = false;
		Boolean Flag = false;
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String result = clienttasks.facts(true, null, null, null, null, null).getStdout();
		Pattern p = Pattern.compile(result);
		Matcher matcher = p.matcher("Unknown");
		while (matcher.find()) {
			pattern = matcher.find();
		}
		Assert.assertEquals(pattern, Flag);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-47931", "RHEL7-97446"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify fix for Bug 886604 - etc/yum.repos.d/ does not exist, turning manage_repos off.",
			groups = {"Tier3Tests","VerifyRepoFileExistance", "blockedByBug-886604", "blockedByBug-919700" },
			enabled = true)
	public void testRepoFileExistance() throws JSONException, Exception {
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "rhsm", "manage_repos", "1" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		List<YumRepo> originalRepos = clienttasks.getCurrentlySubscribedYumRepos();
		Assert.assertFalse(originalRepos.isEmpty(), "list is not empty after setting manage_repos to 1");
		listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "rhsm", "manage_repos", "0" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		clienttasks.getYumRepolist("all"); // needed to trigger
		// subscription-manager yum plugin
		originalRepos = clienttasks.getCurrentlySubscribedYumRepos();
		Assert.assertTrue(originalRepos.isEmpty(), "list is  empty after setting manage_repos to 0");

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@SuppressWarnings("deprecation")
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21899", "RHEL7-51760"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify fix for Bug 755677 - failing to add a virt unlimited pool to an activation key",
			groups = {"Tier3Tests","AddingVirtualPoolToActivationKey", "blockedByBug-755677","blockedByBug-1555582" },
			enabled = true)
	public void testAddingVirtualPoolToActivationKey() throws JSONException, Exception {
		Integer addQuantity = 1;

		String consumerId = clienttasks.getCurrentConsumerId(
				clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null,
						null, null, (String) null, null, null, null, true, false, null, null, null, null));

		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				consumerId);
		String Productname, productId;
		List<String> providedProductIds = new ArrayList<String>();
		Productname = "virt-only-product to be added to activation key";
		productId = "virt-only-test-product";
		String poolId = null;
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("version", "1.0");
		attributes.put("variant", "server");
		attributes.put("arch", "ALL");
		attributes.put("warning_period", "30");
		attributes.put("type", "MKT");
		attributes.put("type", "SVC");
		attributes.put("virt_limit", "unlimited");
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, productId);
		String resourcePath = "/products/" + productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);

		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				sm_clientOrg, Productname, productId, 1, attributes, null);
		CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, ownerKey, 20, -1 * 24 * 60/* 1 day ago */,
				15 * 24 * 60/* 15 days from now */, getRandInt(), getRandInt(), productId, providedProductIds, null);
		JSONObject jobDetail = CandlepinTasks.refreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,
			sm_serverAdminPassword, sm_serverUrl, ownerKey);
		jobDetail = CandlepinTasks.waitForJobDetailStateUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
			sm_serverUrl, jobDetail, "FINISHED", 5 * 1000, 1);
		String name = String.format("%s_%s-ActivationKey%s", sm_clientUsername, sm_clientOrg,
				System.currentTimeMillis());
		Map<String, String> mapActivationKeyRequest = new HashMap<String, String>();
		mapActivationKeyRequest.put("name", name);
		JSONObject jsonActivationKeyRequest = new JSONObject(mapActivationKeyRequest);
		JSONObject jsonActivationKey = new JSONObject(
				CandlepinTasks.postResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,
						"/owners/" + sm_clientOrg + "/activation_keys", jsonActivationKeyRequest.toString()));

		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
		for (SubscriptionPool availList : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
			if (availList.subscriptionName.equals(Productname)) {
				poolId = availList.poolId;

			}
		}
		new JSONObject(
				CandlepinTasks
						.postResourceUsingRESTfulAPI(sm_clientUsername,
								sm_clientPassword, sm_serverUrl, "/activation_keys/" + jsonActivationKey.getString("id")
										+ "/pools/" + poolId + (addQuantity == null ? "" : "?quantity=" + addQuantity),
								null));

		clienttasks.register(null, null, sm_clientOrg, null, null, null, null, null, null, null, name, null, null, null,
				true, false, null, null, null, null);
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertTrue(
				consumedProductSubscriptions.size() == 1 && consumedProductSubscriptions.get(0).poolId.equals(poolId),
				"Registering with an activationKey named '" + name
						+ "' should grant a single entitlement from subscription pool id '" + poolId + "'.");

		new JSONObject(CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,
				"/activation_keys/" + name));
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21984", "RHEL7-51846"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify tracebacks occur running yum repolist after subscribing to a pool",
			groups = {"Tier3Tests","YumReposListAfterSubscription", "blockedByBug-696786", "blockedByBug-919700" },
			enabled = true)
	public void testYumReposListAfterSubscription() throws JSONException, Exception {
		Boolean pattern = false;
		Boolean Flag = false;
		String yum_cmd = "yum repolist enabled --disableplugin=rhnplugin";
		String result = client.runCommandAndWait(yum_cmd).getStdout();
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		result = client.runCommandAndWait(yum_cmd).getStdout();
		Pattern p = Pattern.compile(result);
		Matcher matcher = p.matcher("Traceback (most recent call last):");
		while (matcher.find()) {
			pattern = matcher.find();

		}
		Assert.assertEquals(Flag, pattern);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@ImplementsNitrateTest(caseId = 50235)
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21931", "RHEL7-51793"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify rhsm log for Update With No Installed Products",
			groups = {"Tier3Tests","UpdateWithNoInstalledProducts", "blockedByBug-746241", "blockedByBug-1389559" },
			enabled = true)
	public void testUpdateWithNoInstalledProducts() throws JSONException, Exception {
		client.runCommandAndWait("rm -f " + clienttasks.rhsmLogFile);
		configureTmpProductCertDirWithOutInstalledProductCerts();
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);

		String LogMarker = System.currentTimeMillis()
				+ " Testing ***************************************************************";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, LogMarker);
		String InstalledProducts = clienttasks.listInstalledProducts().getStdout();
		clienttasks.run_rhsmcertd_worker(null);
		Assert.assertEquals(InstalledProducts.trim(), "No installed products to list");
		String tailFromMarkedFile = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, LogMarker,
				null);
		Assert.assertFalse(
				doesStringContainMatches(tailFromMarkedFile, "Error while updating certificates using daemon"),
				"'Error' messages in rhsm.log"); // "Error while updating

		Assert.assertTrue(doesStringContainMatches(tailFromMarkedFile, "Installed product IDs: \\[\\]"),
				"'Installed product IDs:' list is empty in rhsm.log");
		Assert.assertTrue(doesStringContainMatches(tailFromMarkedFile, "certs updated:"),
				"'certs updated:' in rhsm.log");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21913", "RHEL7-51774"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify Facts Update For Deleted Consumer",
			groups = {"Tier3Tests","FactsUpdateForDeletedConsumer","blockedByBug-798788" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 148216)
	public void testFactsUpdateForDeletedConsumer() throws JSONException, Exception {

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				"/consumers/" + consumerId);
		String result = clienttasks.facts_(null, true, null, null, null, null).getStderr();
		String ExpectedMsg = "Consumer " + consumerId + " has been deleted";
		if (!clienttasks.workaroundForBug876764(sm_serverType))
			ExpectedMsg = "Unit " + consumerId + " has been deleted";
		Assert.assertEquals(result.trim(), ExpectedMsg);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21925", "RHEL7-51786"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify that you blocked when trying to register using the consumerId of a deleted owner",
			groups = {"Tier3Tests","RegisterWithConsumeridOfDeletedOwner" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 148216)
	public void testRegisterWithConsumerIdOfDeletedOwner() throws JSONException, Exception {
		String orgname = "testOwner1";
		servertasks.createOwnerUsingCPC(orgname);
		clienttasks.register_(sm_serverAdminUsername, sm_serverAdminPassword, orgname, null, null, null, null, null,
				null, null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				"/owners/" + orgname);
		clienttasks.clean_();
		SSHCommandResult result = clienttasks.register_(sm_serverAdminUsername, sm_serverAdminPassword, orgname, null,
				null, null, consumerId, null, null, null, (String) null, null, null, null, null, null, null, null,
				null, null);
		String expected = "Consumer " + consumerId + " has been deleted";
		if (!clienttasks.workaroundForBug876764(sm_serverType))
			expected = "Unit " + consumerId + " has been deleted";
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-1")) expected = "HTTP error (410 - Gone): "+expected;	// post commit 630e1a2eb06e6bfacac669ce11f38e228c907ea9 1507030: RestlibExceptions should show they originate server-side
		Assert.assertEquals(result.getStderr().trim(), expected);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21923", "RHEL7-51784"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if register to a deleted owner",
			groups = {"Tier3Tests","RegisterToDeletedOwner" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 148216)
	public void testRegisterToDeletedOwner() throws JSONException, Exception {
		String orgname = "testOwner1";
		servertasks.createOwnerUsingCPC(orgname);
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				"/owners/" + orgname);
		SSHCommandResult result = clienttasks.register_(sm_serverAdminUsername, sm_serverAdminPassword, orgname, null,
				null, null, null, null, null, null, (String) null, null, null, null, true, null, null, null, null, null);
		String expected = "Organization " + orgname + " does not exist.";
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-1")) expected = "HTTP error (400 - Bad Request): "+expected;	// post commit 630e1a2eb06e6bfacac669ce11f38e228c907ea9 1507030: RestlibExceptions should show they originate server-side
		Assert.assertEquals(result.getStderr().trim(), expected);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21909", "RHEL7-51770"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if Repos List is empty for FutureSubscription",
			groups = {"Tier3Tests","EmptyReposListForFutureSubscription", "blockedByBug-958775","blockedByBug-1440180","blockedByBug-1555582" },
			enabled = true)
	public void testEmptyReposListForFutureSubscription() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();

		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
		consumerId);
		clienttasks.autoheal(null, null, true, null, null, null, null);
		String futurePool = createTestPool(60 * 24 *365, 60 * 24 *(365*2),true);
		DateFormat yyyy_MM_dd_DateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar nextYear = new GregorianCalendar();
		nextYear.add(Calendar.YEAR, 1);
		nextYear.add(Calendar.DATE, 1); // one day after a year
		String onDateToTest = yyyy_MM_dd_DateFormat.format(nextYear.getTime());
		clienttasks.subscribe(null, null, futurePool, null, null, null, null, null, null, null, null, null, null);

		// determine if both active and inactive entitlements are being consumed
		boolean activeProductSubscriptionsConsumed = false;
		boolean inactiveProductSubscriptionsConsumed = false;
		List<ProductSubscription> currentlyConsumedProductSubscriptions = clienttasks
				.getCurrentlyConsumedProductSubscriptions();
		for (ProductSubscription subscriptions : currentlyConsumedProductSubscriptions) {
			if (subscriptions.isActive)
				activeProductSubscriptionsConsumed = true;
			if (!subscriptions.isActive)
				inactiveProductSubscriptionsConsumed = true;
		}
		if (activeProductSubscriptionsConsumed && inactiveProductSubscriptionsConsumed) {
			throw new SkipException("This test assumes that both current and future subscriptions are available on '"
					+ onDateToTest + "' which is determined by the subscriptions loaded on the candlepin server.");
		}
		Assert.assertTrue(!clienttasks.getCurrentlyConsumedProductSubscriptions().isEmpty(),
				"We should still be consuming future entitlements (indicated by Active:False).");
		Assert.assertTrue(clienttasks.getCurrentlySubscribedRepos().isEmpty(),
				"There should not be any entitled repos despite the future attached entitlements (indicated by Active:False).");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21933", "RHEL7-51795"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if auto-subscribe and activation-key are mutually exclusive",
			groups = {"Tier3Tests","VerifyAutoSubscribeAndActivationkeyTogether", "blockedByBug-869729" },
			enabled = true)
	public void testAutoSubscribeAndActivationKeyTogether() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String name = String.format("%s_%s-ActivationKey%s", sm_clientUsername, sm_clientOrg,
				System.currentTimeMillis());
		Map<String, String> mapActivationKeyRequest = new HashMap<String, String>();
		mapActivationKeyRequest.put("name", name);
		JSONObject jsonActivationKeyRequest = new JSONObject(mapActivationKeyRequest);
		JSONObject jsonActivationKey = new JSONObject(
				CandlepinTasks.postResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl,
						"/owners/" + sm_clientOrg + "/activation_keys", jsonActivationKeyRequest.toString()));
		SSHCommandResult result = clienttasks.register_(null, null, sm_clientOrg, null, null, null, null, true, null,
				null, jsonActivationKey.get("name").toString(), null, null, null, true, null, null, null, null, null);
		String expected_msg = "Error: Activation keys cannot be used with --auto-attach.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.9-1")) { // post
			// commit
			// a695ef2d1da882c5f851fde90a24f957b70a63ad
			Assert.assertEquals(result.getStderr().trim(), expected_msg, "stderr");
		} else {
			Assert.assertEquals(result.getStdout().trim(), expected_msg, "stdout");
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21963", "RHEL7-51825"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if entitlement certs are regenerated if certs are manually removed",
			groups = {"Tier3Tests","VerifyRegenrateEntitlementCert"},
			enabled = true)
	@ImplementsNitrateTest(caseId = 64181)
	public void testRegenerateEntitlementCert() throws JSONException, Exception {
		String poolId = null;
		int Certfrequeny = 1;
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "rhsmcertd", "autoAttachInterval".toLowerCase(), "1440" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
		for (SubscriptionPool availList : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			poolId = availList.poolId;

		}
		clienttasks.subscribe_(null, null, poolId, null, null, null, null, null, null, null, null, null, null);
		client.runCommandAndWait("rm -rf " + clienttasks.entitlementCertDir + "/*.pem");
		clienttasks.restart_rhsmcertd(Certfrequeny, null, null);
		SubscriptionManagerCLITestScript.sleep(Certfrequeny * 60 * 1000);
		List<File> Cert = clienttasks.getCurrentEntitlementCertFiles();
		Assert.assertNotNull(Cert.size());
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21973", "RHEL7-51835"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if entitlement certs are downloaded if subscribed using bogus poolid",
			groups = {"Tier3Tests","VerifySubscribingTobogusPoolID" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 50223)
	public void testSubscribingToBogusPoolID() throws JSONException, Exception {
		String poolId = null;
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "rhsmcertd", "autoAttachInterval".toLowerCase(), "1440" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
		for (SubscriptionPool availList : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			poolId = availList.poolId;

		}
		String pool = randomizeCaseOfCharactersInString(poolId);
		clienttasks.subscribe_(null, null, pool, null, null, null, null, null, null, null, null, null, null);
		List<File> Cert = clienttasks.getCurrentEntitlementCertFiles();
		Assert.assertEquals(Cert.size(), 0);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21952", "RHEL7-51814"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify Functionality Access After Unregister",
			groups = {"Tier3Tests","VerifyFunctionalityAccessAfterUnregister" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 50215)
	public void testFunctionalityAccessAfterUnregister() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg);
		String availList = clienttasks.listAllAvailableSubscriptionPools().getStdout();
		Assert.assertNotNull(availList);
		clienttasks.unregister(null, null, null, null);
		SSHCommandResult listResult = clienttasks.list_(true, true, null, null, null, null, null, null, null, null,
				null, null, null, null, null);
		String expected = "This system is not yet registered. Try 'subscription-manager register --help' for more information.";
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.8-1")) { // post
			// commit
			// df95529a5edd0be456b3528b74344be283c4d258
			Assert.assertEquals(listResult.getStderr().trim(), expected, "stderr");
		} else {
			Assert.assertEquals(listResult.getStdout().trim(), expected, "stdout");
		}
		ConsumerCert consumercert = clienttasks.getCurrentConsumerCert();
		Assert.assertNull(consumercert);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21960", "RHEL7-51822"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify only One Cert is downloaded Per One Subscription",
			groups = {"Tier3Tests","VerifyOneCertPerOneSubscription" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 50215)
	public void testOneCertPerOneSubscription() {
		int expected = 0;
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);
		for (SubscriptionPool subscriptionpool : getRandomSubsetOfList(
				clienttasks.getCurrentlyAvailableSubscriptionPools(), 5)) {
			clienttasks.subscribe_(null, null, subscriptionpool.poolId, null, null, null, null, null, null, null, null,
					null, null);
			expected = expected + 1;
			List<File> Cert = clienttasks.getCurrentEntitlementCertFiles();
			Assert.assertEquals(Cert.size(), expected,
					"Total number of local entitlement certs after subscribing to '" + expected + "' different pools.");
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description = "verify content set associated with product",
			groups = {"Tier3Tests","VerifyUnsubscribingCertV3","blockedByBug-895447" },
			enabled = false)
	@ImplementsNitrateTest(caseId = 50215)
	public void testUnsubscribingCertV3() throws JSONException, Exception {

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		File expectCertFile = new File(System.getProperty("automation.dir", null) + "/certs/CertV3.pem");
		RemoteFileTasks.putFile(client, expectCertFile.toString(), "/root/", "0755");
		clienttasks.importCertificate_("/root/CertV3.pem");
		String expected = "0 subscriptions removed at the server." + "\n" + "1 local certificate has been deleted.";
		String result = clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null).getStdout();
		Assert.assertEquals(result.trim(), expected);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21962", "RHEL7-51824"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify  rhsmcertd is logging update failed (255)",
			groups = {"Tier3Tests","VerifyRHSMCertdLogging","blockedByBug-708512","blockedByBug-1440934" },
			enabled = true)
	public void testRhsmcertdLogging() throws JSONException, Exception {
		int autoAttachInterval = 1;

		clienttasks.unregister(null, null, null, null);

		clienttasks.restart_rhsmcertd(autoAttachInterval, null, false);
		clienttasks.waitForRegexInRhsmcertdLog("Update failed", 1);

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21961", "RHEL7-51823"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify content set associated with product",
			groups = {"Tier3Tests","VerifycertsAfterUnsubscribeAndunregister"},
			enabled = true)
	@ImplementsNitrateTest(caseId = 50215)
	public void testProductCertsAfterUnsubscribeAndUnregister() throws JSONException, Exception {

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);
		clienttasks.subscribe_(true, null, (String) null, null, null, null, null, null, null, null, null, null, null);
		List<File> ProductCerts = clienttasks.getCurrentProductCertFiles();
		Assert.assertFalse(ProductCerts.isEmpty());
		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
		List<File> certs = clienttasks.getCurrentEntitlementCertFiles();
		Assert.assertTrue(certs.isEmpty());
		ProductCerts.clear();
		ProductCerts = clienttasks.getCurrentProductCertFiles();
		Assert.assertFalse(ProductCerts.isEmpty());
		clienttasks.unregister(null, null, null, null);
		ConsumerCert consumerCerts = clienttasks.getCurrentConsumerCert();
		Assert.assertNull(consumerCerts);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21965", "RHEL7-51827"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify reregister with invalid consumerid",
			groups = {"Tier3Tests","VerifyRegisterUsingInavlidConsumerId" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 61716)
	public void testRegisterUsingInvalidConsumerId() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		String invalidconsumerId = randomGenerator.nextInt() + consumerId;
		log.info("Testing with invalidconsumerId '" + consumerId + "'.");

		String expectedStdout = "The system with UUID " + consumerId + " has been unregistered";
		String expectedStderr = "Consumer with id " + invalidconsumerId + " could not be found.";
		Boolean force = true;
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.16.2-1")) {
			clienttasks.unregister(null, null, null, null);
			force = false;
			expectedStdout = "";
		}
		SSHCommandResult result = clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null,
				null, invalidconsumerId, null, null, null, (String) null, null, null, null, force, null, null, null,
				null, null);
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.15.9-2"))
			expectedStdout += String.format("\n" + "Registering to: %s:%s%s", clienttasks.getConfParameter("hostname"),
					clienttasks.getConfParameter("port"), clienttasks.getConfParameter("prefix"));
		Assert.assertEquals(result.getStdout().trim(), expectedStdout.trim(), "stdout");
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-1")) expectedStderr = "HTTP error (404 - Not Found): "+expectedStderr;	// post commit 630e1a2eb06e6bfacac669ce11f38e228c907ea9 1507030: RestlibExceptions should show they originate server-side
		Assert.assertEquals(result.getStderr().trim(), expectedStderr.trim(), "stderr");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21979", "RHEL7-51841"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify if corrupt identity cert displays a trace back for list command",
			groups = {"Tier3Tests","VerifyCorruptIdentityCert", "blockedByBug-607162" },
			enabled = true)
	public void testCorruptIdentityCert() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		client.runCommandAndWait("cp /etc/pki/consumer/cert.pem /etc/pki/consumer/cert.pem.save");
		RemoteFileTasks.runCommandAndAssert(client, "openssl x509 -noout -text -in " + clienttasks.consumerCertFile()
				+ " > /tmp/stdout; mv /tmp/stdout -f " + clienttasks.consumerCertFile(), 0);
		SSHCommandResult result = clienttasks.list_(null, true, null, null, null, null, null, null, null, null, null,
				null, null, null, null);
		 if(clienttasks.isPackageVersion("subscription-manager", ">=", "1.20.1-1")) {	// commit 79f86e4c043ee751677131ed4e3cf00affd13087
			Assert.assertEquals(result.getStderr().trim(), "Consumer identity either does not exist or is corrupted. Try register --help", "stdout");
			    
		 }else if((clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.8-1"))) { // post
			// commit
			// df95529a5edd0be456b3528b74344be283c4d258
			Assert.assertEquals(result.getStderr().trim(), clienttasks.msg_ConsumerNotRegistered, "stderr");
		 }else{
			Assert.assertEquals(result.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered, "stdout");
		 }

	}
	
	
	@AfterGroups(groups = { "setup" }, value = {"VerifyCorruptIdentityCert"})
	protected void restoreOriginalTests() throws Exception {
	
	client.runCommandAndWait("mv -f /etc/pki/consumer/cert.pem.save /etc/pki/consumer/cert.pem");
	}


	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21982", "RHEL7-51844"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "subscription-manager facts --update changes update date after facts update",
			groups = {"Tier3Tests","VerifyUpdateConsumerFacts", "blockedByBug-700821" },
			enabled = true)
	public void testUpdateConsumerFacts() throws JSONException, Exception {

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerid = clienttasks.getCurrentConsumerId();
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, "/consumers/" + consumerid));
		String createdDateBeforeUpdate = jsonConsumer.getString("created");
		String UpdateDateBeforeUpdate = jsonConsumer.getString("updated");
		clienttasks.facts(null, true, null, null, null, null).getStderr();
		jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, "/consumers/" + consumerid));
		String createdDateAfterUpdate = jsonConsumer.getString("created");
		String UpdateDateAfterUpdate = jsonConsumer.getString("updated");
		Assert.assertEquals(createdDateBeforeUpdate, createdDateAfterUpdate,
				"no changed in date value after facts update");
		Assert.assertNoMatch(UpdateDateBeforeUpdate, UpdateDateAfterUpdate,
				"updated date has been changed after facts update");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21954", "RHEL7-51816"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify healing of installed products without taking future subscriptions into consideration",
			groups = {"Tier3Tests","VerifyHealingForFutureSubscription", "blockedByBug-907638","blockedByBug-1440180","blockedByBug-1555582" },
			enabled = true)
	public void testHealingForFutureSubscription() throws JSONException, Exception {
		String productId = null;

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.run_rhsmcertd_worker(true);
		String consumerId = clienttasks.getCurrentConsumerId();
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
		consumerId);
		clienttasks.unsubscribeFromTheCurrentlyConsumedSerialsCollectively();
		clienttasks.autoheal(null, null, true, null, null, null, null); // disabling autoheal
		String futurePool = createTestPool(60 * 24 *365, 60 * 24 *(365*2),true);
		clienttasks.subscribe(null, null, futurePool, null, null, null, null, null, null,
				null, null, null, null);
		ProductSubscription futureConsumedProductSubscription = ProductSubscription
				.findFirstInstanceWithMatchingFieldFromList("poolId", futurePool,
						clienttasks.getCurrentlyConsumedProductSubscriptions());
		String expectedFutureConsumedProductSubscriptionStatusDetails = "Subscription has not begun";
		Assert.assertTrue(
				futureConsumedProductSubscription.statusDetails
						.contains(expectedFutureConsumedProductSubscriptionStatusDetails),
				"The status details of the future consumed subscription states '"
						+ expectedFutureConsumedProductSubscriptionStatusDetails + "'.");
		List<String> providedProductIdsOfFutureConsumedProductSubscription = CandlepinTasks.getPoolProvidedProductIds(
				sm_clientUsername, sm_clientPassword, sm_serverUrl, futurePool);
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		for (String providedProductId : providedProductIdsOfFutureConsumedProductSubscription) {
			InstalledProduct installedProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId",
					providedProductId, installedProducts);
			if (installedProduct != null) {
				Assert.assertEquals(installedProduct.status, "Future Subscription",
						"Status of an installed product provided by a future consumed subscription.");
				productId = installedProduct.productId;
			}
		}
		if (productId == null)
			throw new SkipException("None of the provided products from consumed future subscription '"
					+ futureConsumedProductSubscription + "' are installed for testing.");
		clienttasks.autoheal(null, true, null, null, null, null, null); // enabling
		// autoheal
		clienttasks.run_rhsmcertd_worker(true);
		boolean assertedFutureSubscriptionIsNowSubscribed = false;
		InstalledProduct installedProductAfterAutoHealing = InstalledProduct.findFirstInstanceWithMatchingFieldFromList(
				"productId", productId, clienttasks.getCurrentlyInstalledProducts());
		List<String> installedProductArches = new ArrayList<String>(
				Arrays.asList(installedProductAfterAutoHealing.arch.trim().split(" *, *"))); // Note:
		// the
		// arch
		// can
		// be
		// a
		// comma
		// separated
		// list
		// of
		// values
		if (installedProductArches.contains("x86")) {
			installedProductArches.addAll(Arrays.asList("i386", "i486", "i586", "i686"));
		} // Note: x86 is a general alias to cover all 32-bit intel
			// microprocessors, expand the x86 alias
		if (installedProductArches.contains(clienttasks.arch) || installedProductArches.contains("ALL")) {
			Assert.assertEquals(installedProductAfterAutoHealing.status.trim(), "Subscribed",
					"Previously installed product '" + installedProductAfterAutoHealing.productName
							+ "' covered by a Future Subscription should now be covered by a current subscription after auto-healing.");
			assertedFutureSubscriptionIsNowSubscribed = true;
		} else {
			Assert.assertEquals(installedProductAfterAutoHealing.status.trim(), "Future Subscription",
					"Mismatching arch installed product '" + installedProductAfterAutoHealing.productName + "' (arch='"
							+ installedProductAfterAutoHealing.arch
							+ "') covered by a Future Subscription should remain unchanged after auto-healing.");
		}

		Assert.assertTrue(assertedFutureSubscriptionIsNowSubscribed,
				"Verified at least one previously installed product covered by a Future Subscription is now covered by a current subscription after auto-healing.");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-22312", "RHEL7-51791"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify unsubscribe from multiple invalid serial numbers",
			groups = {"Tier3Tests","blockedByBug-1268491","UnsubscribeFromInvalidMultipleEntitlements" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 50230)
	public void testUnsubscribeFromInvalidMultipleEntitlements() throws JSONException, Exception {
		List<BigInteger> serialnums = new ArrayList<BigInteger>();
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.unsubscribe_(true, (BigInteger) null, null, null, null, null, null);

		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		if (clienttasks.getCurrentlyConsumedProductSubscriptions().isEmpty())
			throw new SkipException("Sufficient pools are not available");
		for (ProductSubscription consumed : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			serialnums.add(consumed.serialNumber);
		}

		int i = randomGenerator.nextInt(serialnums.size());
		int j = randomGenerator.nextInt(serialnums.size());
		/*
		 * irrelevant for this test case if (i == j) { j =
		 * randomGenerator.nextInt(serialnums.size()); }
		 */

		BigInteger serialOne = serialnums.get(i);
		BigInteger serialTwo = serialnums.get(j);
		String result = unsubscribeFromMultipleEntitlementsUsingSerialNumber(serialOne.multiply(serialTwo),
				serialTwo.multiply(serialOne)).getStdout();
		String expected = "";
		expected += "Serial numbers unsuccessfully removed at the server:" + "\n";
		expected += "   " + serialOne.multiply(serialTwo) + " is not a valid value for serial" + "\n";
		expected += "   " + serialTwo.multiply(serialOne) + " is not a valid value for serial";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.10-1")) {
			expected = "Serial numbers unsuccessfully removed at the server:" + "\n";
			expected += "   " + serialOne.multiply(serialTwo);
		}
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.16-1")) {
			expected = "The entitlement server failed to remove these serial numbers:" + "\n";
			expected += "   " + serialOne.multiply(serialTwo);
		}
		Assert.assertEquals(result.trim(), expected);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21930", "RHEL7-51792"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify unsubscribe from multiple subscriptions",
			groups = {"Tier3Tests","UnsubscribeFromMultipleEntitlementsTest", "blockedByBug-867766", "blockedByBug-906550" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 50230)
	public void testUnsubscribeFromMultipleEntitlements() throws JSONException, Exception {
		int count = 0;
		List<BigInteger> serialnums = new ArrayList<BigInteger>();
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if ((count <= 2)) {
				if (CandlepinTasks.isPoolAModifier(sm_clientUsername, sm_clientPassword, pool.poolId, sm_serverUrl))
					continue; // skip modifier pools
				count++;
				clienttasks.subscribe(null, null, pool.poolId, null, null, null, null, null, null, null, null, null, null);
			}

		}

		for (ProductSubscription consumed : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			serialnums.add(consumed.serialNumber);
		}
		BigInteger serialOne = serialnums.get(randomGenerator.nextInt(serialnums.size())); // serialnums.get(i);
		serialnums.remove(serialOne);
		BigInteger serialTwo = serialnums.get(randomGenerator.nextInt(serialnums.size())); // serialnums.get(j);
		String result = unsubscribeFromMultipleEntitlementsUsingSerialNumber(serialOne, serialTwo).getStdout();

		String expected = "";
		expected += "Serial numbers successfully removed at the server:" + "\n";
		expected += "   " + serialOne + "\n";
		expected += "   " + serialTwo + "\n";
		expected += "2 local certificates have been deleted.";
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.16-1")) {
			expected = "The entitlement server successfully removed these serial numbers:" + "\n";
			expected += "   " + serialOne + "\n";
			expected += "   " + serialTwo + "\n";
			expected += "2 local certificates have been deleted.";
		}
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.2.0-1")) {
		    	expected =  "2 local certificates have been deleted."+ "\n";
			expected += "The entitlement server successfully removed these serial numbers:"+ "\n";
			expected += "   " + serialOne + "\n";
			expected += "   " + serialTwo;
		}
		Assert.assertEquals(result.trim(), expected);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47895", "RHEL7-96269"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28485",	// RHSM-REQ : subscription-manager cli registration and deregistration
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84906",	// RHSM-REQ : subscription-manager cli registration and deregistration
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.LOW, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description= "verify registration fails when specifying a consumerid that has been revoked",
			groups= {"Tier3Tests","VerifyRegisterWithConsumerIdForDifferentUser" },
			enabled= true)
	@ImplementsNitrateTest(caseId = 61710)
	public void testRegisterWithConsumerIdForDifferentUser() throws JSONException, Exception {
		if (sm_client2Username == null)
			throw new SkipException("This test requires valid credentials for a second user.");
		clienttasks.register(sm_client2Username, sm_client2Password, sm_client2Org, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerid = clienttasks.getCurrentConsumerId();
		String result = clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null,
				consumerid, null, null, null, (String) null, null, null, null, true, null, null, null, null, null)
				.getStderr();
		Assert.assertNotNull(result);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21948", "RHEL7-51810"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify content set associated with product",
			groups = {"Tier3Tests","VerifyFactsListByOverridingValues" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 56389)
	public void testFactsListByOverridingValues() throws JSONException, Exception {

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String listBeforeUpdate = clienttasks.facts(true, null, null, null, null, null).getStdout();
		Map<String, String> factsMap = new HashMap<String, String>();
		Integer sockets = 4;
		factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
		factsMap.put("uname.machine", "i386");
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		String listAfterUpdate = clienttasks.facts(true, null, null, null, null, null).getStdout();
		Assert.assertNoMatch(listAfterUpdate, listBeforeUpdate);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21949", "RHEL7-51811"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify content set associated with product",
			groups = {"Tier3Tests","VerifyFactsListWithOutrageousValues" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 56897)
	public void testFactsListWithOutrageousValues() throws JSONException, Exception {

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String listBeforeUpdate = clienttasks.facts(true, null, null, null, null, null).getStdout();

		client.runCommandAndWait("echo '{fuzzing :testing}' >>/var/lib/rhsm/facts/facts.json");
		clienttasks.facts(null, true, null, null, null, null);
		String listAfterUpdate = clienttasks.facts(true, null, null, null, null, null).getStdout();
		Assert.assertFalse(listAfterUpdate.contentEquals("fuzzing"));
		Assert.assertEquals(listAfterUpdate, listBeforeUpdate);
		client.runCommandAndWait("cp /var/lib/rhsm/facts/facts.json /var/lib/rhsm/facts/facts.json.save");
		client.runCommandAndWait("sed /'uname.machine: x86_64'/d /var/lib/rhsm/facts/facts.json");
		clienttasks.facts(null, true, null, null, null, null);
		listAfterUpdate = clienttasks.facts(true, null, null, null, null, null).getStdout();
		Assert.assertFalse(listAfterUpdate.contentEquals("uname.machine: x86_64"));
		client.runCommandAndWait("mv -f /var/lib/rhsm/facts/facts.json.save /var/lib/rhsm/facts/facts.json");
		// Assert.assertEquals(listAfterUpdate, listBeforeUpdate);

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21978", "RHEL7-51840"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify content set associated with product",
			groups = {"Tier3Tests","Verifycontentsetassociatedwithproduct" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 61115)
	public void testContentSetAssociatedWithProduct() throws JSONException, Exception {
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		clienttasks.subscribeToSubscriptionPool(pools.get(randomGenerator.nextInt(pools.size())));
		List<File> certs = clienttasks.getCurrentEntitlementCertFiles();
		RemoteFileTasks.runCommandAndAssert(client,
				"openssl x509 -noout -text -in " + certs.get(randomGenerator.nextInt(certs.size()))
						+ " > /tmp/stdout; mv /tmp/stdout -f " + certs.get(randomGenerator.nextInt(certs.size())),
				0);
		String consumed = clienttasks
				.list_(null, null, true, null, null, null, null, null, null, null, null, null, null, null, null).getStderr();
		String expected = "Error loading certificate";
		// update the test
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.18.4-1")) {
			// post commit b0e877cfb099184f9bab1b681a41df9bdd2fb790 side affect
			// from m2crypto changes
			expected = "System certificates corrupted. Please reregister.";
		}
		Assert.assertTrue(consumed.trim().equals(expected));

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@Test(	description = "verify if rhsmcertd process refresh the identity certificate after every restart",
			groups = {"Tier3Tests","VerifyrhsmcertdRefreshIdentityCert", "blockedByBug-827034", "blockedByBug-923159","blockedByBug-827035" },
			enabled = false)
	// TODO disabling this test for two reasons:
	// 1. it is dangerous to change the system dates
	// 2. the network service seems to stop when the date changes breaking the
	// ability to ssh into the system
	public void testRhsmcertdRefreshIdentityCert() throws JSONException, Exception {

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		DateFormat yyyy_MM_dd_DateFormat = new SimpleDateFormat("yyyy-MM-dd");
		log.info(clienttasks.getCurrentConsumerCert().validityNotBefore.toString() + "   "
				+ clienttasks.getCurrentConsumerCert().validityNotAfter.toString()
				+ " cert validity before regeneration");
		Calendar StartTimeBeforeRHSM = clienttasks.getCurrentConsumerCert().validityNotBefore;
		Calendar EndTimeBeforeRHSM = clienttasks.getCurrentConsumerCert().validityNotAfter;
		String existingCertdate = client.runCommandAndWait("ls -lart /etc/pki/consumer/cert.pem | cut -d ' ' -f6,7,8")
				.getStdout();
		String StartDate = setDate(sm_serverHostname, sm_serverSSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase,
				"date -s '15 year 9 month' +'%F'");
		log.info("Changed the date of candlepin" + client.runCommandAndWait("hostname"));
		setDate(sm_clientHostname, sm_clientSSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase,
				"date -s '15 year 9 month' +'%F'");
		clienttasks.restart_rhsmcertd(null, null, null);
		SubscriptionManagerCLITestScript.sleep(2 * 60 * 1000);
		log.info(clienttasks.getCurrentConsumerCert().validityNotBefore.toString() + "   "
				+ clienttasks.getCurrentConsumerCert().validityNotAfter.toString()
				+ " cert validity After regeneration");
		Calendar StartTimeAfterRHSM = clienttasks.getCurrentConsumerCert().validityNotBefore;
		Calendar EndTimeAfterRHSM = clienttasks.getCurrentConsumerCert().validityNotAfter;
		String EndDateAfterRHSM = yyyy_MM_dd_DateFormat
				.format(clienttasks.getCurrentConsumerCert().validityNotAfter.getTime());
		String StartDateAfterRHSM = yyyy_MM_dd_DateFormat
				.format(clienttasks.getCurrentConsumerCert().validityNotBefore.getTime());
		String updatedCertdate = client.runCommandAndWait("ls -lart /etc/pki/consumer/cert.pem | cut -d ' ' -f6,7,8")
				.getStderr();
		String EndDate = setDate(sm_serverHostname, sm_serverSSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase,
				"date -s '15 year ago 9 month ago' +'%F'");
		log.info("Changed the date of candlepin" + client.runCommandAndWait("hostname"));
		setDate(sm_clientHostname, sm_clientSSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase,
				"date -s '15 year ago 9 month ago' +'%F'");
		Assert.assertEquals(StartDateAfterRHSM, StartDate);
		Assert.assertEquals(EndDateAfterRHSM, EndDate);
		Assert.assertNotSame(StartTimeBeforeRHSM.getTime(), StartTimeAfterRHSM.getTime());
		Assert.assertNotSame(EndTimeBeforeRHSM.getTime(), EndTimeAfterRHSM.getTime());
		Assert.assertNotSame(existingCertdate, updatedCertdate);

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21974", "RHEL7-51836"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "subscription-manager unsubscribe --all on expired subscriptions removes certs from entitlement folder",
			groups = {"Tier3Tests","VerifyUnsubscribeAllForExpiredSubscription", "blockedByBug-852630","blockedByBug-906550","blockedByBug-1555582" },
			enabled = true)
	public void testUnsubscribeAllForExpiredSubscription() throws JSONException, Exception {
	    	clienttasks.clean();
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				consumerId);
		String expiringPoolId = createTestPool(-60 * 24, 1,false);
		Calendar c1 = new GregorianCalendar();
		clienttasks.subscribe(null, null, expiringPoolId, null, null, null, null, null, null, null, null, null, null);
		Calendar c2 = new GregorianCalendar();
		// wait for the pool to expire
		// sleep(endingMinutesFromNow*60*1000);
		// trying to reduce the wait time for the expiration by subtracting off
		// some expensive test time
		sleep(1 * 60 * 1000 - (c2.getTimeInMillis() - c1.getTimeInMillis()));
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		List<ProductSubscription> activeProductSubscriptions = ProductSubscription
				.findAllInstancesWithMatchingFieldFromList("isActive", Boolean.TRUE, consumedProductSubscriptions);
		Set<BigInteger> activeProductSubscriptionSerials = new HashSet<BigInteger>();
		for (ProductSubscription activeProductSubscription : activeProductSubscriptions)
			activeProductSubscriptionSerials.add(activeProductSubscription.serialNumber);
		List<ProductSubscription> expiredProductSubscriptions = ProductSubscription
				.findAllInstancesWithMatchingFieldFromList("isActive", Boolean.FALSE, consumedProductSubscriptions);
		Assert.assertEquals(expiredProductSubscriptions.size(), 1,
				"Found one expired entitlement (indicated by Active:False) among the list of consumed subscriptions.");

		SSHCommandResult result = clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
		String expected = String.format(
				"%d subscriptions removed at the server.\n%d local certificates have been deleted.",
				activeProductSubscriptionSerials.size(),
				activeProductSubscriptionSerials.size() + expiredProductSubscriptions.size());
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.20.2-1")) {	// commit d88d09c7060a17fba34a138313e7efd21cc79d02  D-Bus service for removing entitlements (all/ID/serial num.)
			expected = String.format("%d local certificates have been deleted."+"\n"+"%d subscriptions removed at the server.", activeProductSubscriptionSerials.size()+expiredProductSubscriptions.size(), activeProductSubscriptionSerials.size());
		}
		if (activeProductSubscriptionSerials.size() + expiredProductSubscriptions.size() == 1)
			expected = expected.replace("local certificates have been", "local certificate has been");
		Assert.assertEquals(result.getStdout().trim(), expected);

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21945", "RHEL7-51807"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify One empty certificate file in /etc/rhsm/ca causes registration failure",
			groups = {"Tier3Tests","VerifyEmptyCertCauseRegistrationFailure_Test", "blockedByBug-806958","blockedByBug-1432990" },
			enabled = true)
	public void testEmptyCertCauseRegistrationFailure_Test() throws JSONException, Exception {
		clienttasks.unregister(null, null, null, null);
		String FilePath = myEmptyCaCertFile;
		String command = "touch " + FilePath;
		client.runCommandAndWait(command);
		SSHCommandResult result = clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null,
				null, null, null, null, null, (String) null, null, null, null, null, null, null, null, null, null);
		String Expected = "Bad CA certificate: " + FilePath;
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.9-1")) { // post
			// commit
			// a695ef2d1da882c5f851fde90a24f957b70a63ad
			Assert.assertEquals(result.getStderr().trim(), Expected, "stderr");
		} else {
			Assert.assertEquals(result.getStdout().trim(), Expected, "stdout");
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21950", "RHEL7-51812"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify facts update with incorrect proxy url produces traceback.",
			groups = {"Tier3Tests","VerifyFactsWithIncorrectProxy_Test", "blockedByBug-744504" },
			enabled = true)
	public void testFactsWithIncorrectProxy_Test() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String basicauthproxyUrl = String.format("%s:%s", "testmachine.com", sm_basicauthproxyPort);
		basicauthproxyUrl = basicauthproxyUrl.replaceAll(":$", "");
		SSHCommandResult factsResult = clienttasks.facts_(null, true, basicauthproxyUrl, null, null, null);
		String factsResultExpected = clienttasks.msg_NetworkErrorUnableToConnect;
		factsResultExpected = "Error updating system data on the server, see /var/log/rhsm/rhsm.log for more details.";
		factsResultExpected = clienttasks.msg_ProxyConnectionFailed;

		Assert.assertEquals(factsResult.getStdout().trim() + factsResult.getStderr().trim(), factsResultExpected);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21967", "RHEL7-51829"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify Subscription Manager Leaves Broken Yum Repos After Unregister",
			groups = {"Tier3Tests","ReposListAfterUnregisterTest", "blockedByBug-674652" },
			enabled = true)
	public void testRepoAfterUnregister() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		List<YumRepo> repos = clienttasks.getCurrentlySubscribedYumRepos();
		Assert.assertFalse(repos.isEmpty());
		clienttasks.unregister(null, null, null, null);
		List<YumRepo> repo = clienttasks.getCurrentlySubscribedYumRepos();
		Assert.assertTrue(repo.isEmpty());
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21944", "RHEL7-51806"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify if stacking entitlements reports as distinct entries in cli list --installed",
			groups = {"Tier3Tests","VerifyDistinctStackingEntires", "blockedByBug-733327" },
			enabled = true)
	public void testDistinctStackingEntires() throws Exception {
		String poolId = null;
		String productIds=null;
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null);
		int sockets = 5;
		int core = 2;
		int ram = 10;

		Map<String, String> factsMap = new HashMap<String, String>();
		factsMap.put("cpu.cpu_socket(s)", String.valueOf(sockets));
		factsMap.put("cpu.core(s)_per_socket", String.valueOf(core));
		factsMap.put("memory.memtotal", String.valueOf(GBToKBConverter(ram)));
		factsMap.put("virt.is_guest", String.valueOf(Boolean.FALSE));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		int quantity = 0;

		String providedProductId = null;
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if (pool.subscriptionType.equals("Stackable")) {
				quantity = pool.suggested - 1;
				if (!(pool.suggested == 1)) {
					clienttasks.subscribe(null, null, pool.poolId, null, null, Integer.toString(quantity), null, null,
							null, null, null, null, null);
					 productIds = pool.productId;
					providedProductId = pool.provides.get(randomGenerator.nextInt(pool.provides.size()));
					if (!(providedProductId.isEmpty())) {
					    break;
					}
				}
			}
		}
		InstalledProduct BeforeAttaching = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName",
				providedProductId, clienttasks.getCurrentlyInstalledProducts());
		Assert.assertEquals(BeforeAttaching.status, "Partially Subscribed",
				"Verified that installed product is partially subscribed");
		List<SubscriptionPool> AvailableStackableSubscription = SubscriptionPool
			.findAllInstancesWithMatchingFieldFromList("productId", productIds,
					clienttasks.getAvailableSubscriptionsMatchingInstalledProducts());
		for(SubscriptionPool pools :AvailableStackableSubscription){
			quantity = pools.suggested;
			clienttasks.subscribe(null, null, pools.poolId, null, null, Integer.toString(quantity), null, null,
					null, null, null, null, null);		}
		
		InstalledProduct AfterAttaching = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName",
				providedProductId, clienttasks.getCurrentlyInstalledProducts());
		Assert.assertEquals(AfterAttaching.status, "Subscribed", "Verified that installed product"
				+ AfterAttaching.productName
				+ "is fully subscribed after attaching one more quantity of multi-entitleable stackable subscription");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21943", "RHEL7-51805"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify that Product with UUID '%s' cannot be deleted while subscriptions exist.",
			groups = {"Tier3Tests","DeleteProductTest", "blockedByBug-684941" },
			enabled = true)
	public void testDeletionOfSubscribedProduct_Test() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (List<String>) null, null, null, null, true, null, null, null, null, null);
		clienttasks.subscribe_(true, null, null, (String) null, null, null, null, null, null, null, null, null, null);
		if (clienttasks.getCurrentlyConsumedProductSubscriptions().isEmpty()) {
			throw new SkipException("no installed products are installed");
		} else {
			for (InstalledProduct installed : clienttasks.getCurrentlyInstalledProducts()) {
				if (installed.status.equals("Subscribed")) {
					for (SubscriptionPool AvailSub : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
						if (installed.productName.contains(AvailSub.subscriptionName)) {
							String resourcePath = "/products/" + AvailSub.productId;
							if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
								resourcePath = "/owners/" + sm_clientOrg + resourcePath;
							JSONObject jsonConsumer = new JSONObject(CandlepinTasks.deleteResourceUsingRESTfulAPI(
									sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, resourcePath));
							String expectedDisplayMessage = "Product with UUID '" + AvailSub.productId
									+ "' cannot be deleted while subscriptions exist.";
							if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
								expectedDisplayMessage = "Product with ID '" + AvailSub.productId
										+ "' cannot be deleted while subscriptions exist.";
							if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.1-1")) {	// commit 0d5fefcfa8c1c2485921d2dee6633879b1e06931 Correct incorrect punctuation in user messages
								expectedDisplayMessage = String.format("Product with ID \"%s\" cannot be deleted while subscriptions exist.",AvailSub.productId);
							}	
							Assert.assertEquals(jsonConsumer.getString("displayMessage"), expectedDisplayMessage);
						}
					}
				}
			}
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21951", "RHEL7-51813"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify Force Registration After Consumer is Deleted",
			groups = {"Tier3Tests","ForceRegAfterDEL","blockedByBug-853876" },
			enabled = true)
	public void testForceRegistrationAfterConsumerDeletion() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (List<String>) null, null, null, null, true, null, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				"/consumers/" + consumerId);
		String result = clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null,
				null, null, null, (List<String>) null, null, null, null, true, null, null, null, null, null).getStdout();

		Assert.assertContainsMatch(result.trim(), "The system has been registered with ID: [a-f,0-9,\\-]{36}");

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21904", "RHEL7-51765"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify config Server port with blank or incorrect text produces traceback",
			groups = {"Tier3Tests","configBlankTest", "blockedByBug-744654" },
			enabled = true)
	// @ImplementsNitrateTest(caseId=)
	public void testConfigSetServerPortValueToBlank() {

		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		String section = "server";
		String name = "port";
		String newValue = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, section, name);
		listOfSectionNameValues.add(new String[] { section, name.toLowerCase(), "" });
		SSHCommandResult results = clienttasks.config(null, null, true, listOfSectionNameValues);
		String value = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, section, name);
		Assert.assertEquals("", results.getStdout().trim());
		listOfSectionNameValues.add(new String[] { section, name.toLowerCase(), newValue });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		value = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, section, name);
		Assert.assertEquals(value, newValue);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21990", "RHEL7-51852"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "subscription-manager: register --name , setting consumer name to blank",
			groups = {"Tier3Tests","registerwithname", "blockedByBug-669395" },
			enabled = true)
	public void testRegisterWithNameBlank() throws JSONException, Exception {
		String name = "test";
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, name, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		ConsumerCert consumerCert = clienttasks.getCurrentConsumerCert();
		Assert.assertEquals(consumerCert.name, name);
		name = "";
		SSHCommandResult result = clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null,
				name, null, null, null, null, (String) null, null, null, null, true, null, null, null, null, null);
		String expectedMsg = String.format("Error: system name can not be empty.");
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.8-1")) { // post
			// commit
			// df95529a5edd0be456b3528b74344be283c4d258
			Assert.assertEquals(result.getStderr().trim(), expectedMsg, "stderr");
			Assert.assertEquals(result.getExitCode(), new Integer(64));
		} else {
			Assert.assertEquals(result.getStdout().trim(), expectedMsg, "stdout");
			Assert.assertEquals(result.getExitCode(), new Integer(255));
		}
		consumerCert = clienttasks.getCurrentConsumerCert();
		Assert.assertNotNull(consumerCert.name);

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21989", "RHEL7-51851"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "subscription-manager: register --consumerid  using a different user and valid consumerId",
			groups = {"Tier3Tests","reregister", "blockedByBug-627665" },
			enabled = true)
	public void testRegisterWithConsumerId() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty())
			throw new SkipException(
					"Cannot randomly pick a pool for subscribing when there are no available pools for testing.");
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size()));
		clienttasks.subscribeToSubscriptionPool(pool);
		List<ProductSubscription> consumedSubscriptionsBeforeregister = clienttasks
				.getCurrentlyConsumedProductSubscriptions();
		clienttasks.clean_();
		if (sm_client2Username == null)
			throw new SkipException("This test requires valid credentials for a second user.");
		clienttasks.register_(sm_client2Username, sm_client2Password, sm_client2Org, null, null, null, consumerId, null,
				null, null, (String) null, null, null, null, null, null, null, null, null, null);
		String consumerIdAfter = clienttasks.getCurrentConsumerId();
		Assert.assertEquals(consumerId, consumerIdAfter,
				"The consumer identity  has not changed after registering with consumerid.");
		List<ProductSubscription> consumedscriptionsAfterregister = clienttasks
				.getCurrentlyConsumedProductSubscriptions();
		Assert.assertTrue(
				consumedscriptionsAfterregister.containsAll(consumedSubscriptionsBeforeregister)
						&& consumedSubscriptionsBeforeregister.size() == consumedscriptionsAfterregister.size(),
				"The list of consumed products after reregistering is identical.");
	}

	/**
	 * @author skallesh
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21926", "RHEL7-51787"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "subscription-manager: service-level --org (without --list option)",
			groups = {"Tier3Tests","ServicelevelTest", "blockedByBug-826856" },
			enabled = true)
	public void testServiceLevelWithOrgWithoutList() {

		SSHCommandResult result;
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (List<String>) null, null, null, null, true, null, null, null, null, null);
		result = clienttasks.service_level_(null, false, null, null, sm_clientUsername, sm_clientPassword, "MyOrg",
				null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager", ">=", "1.13.9-1")) { // post
			// commit
			// a695ef2d1da882c5f851fde90a24f957b70a63ad
			Assert.assertEquals(result.getStderr().trim(), "Error: --org is only supported with the --list option",
					"stderr");
		} else {
			Assert.assertEquals(result.getStdout().trim(), "Error: --org is only supported with the --list option",
					"stdout");
		}
	}

	/**
	 * @author skallesh
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21914", "RHEL7-51775"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "subscription-manager: facts --update (when registered)",
			groups = {"Tier3Tests","MyTestFacts","blockedByBug-707525" },
			enabled = true)
	public void testFactsUpdateWhenRegistered() {

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (List<String>) null, null, null, null, true, null, null, null, null, null);
		SSHCommandResult result = clienttasks.facts(null, true, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "Successfully updated the system facts.");
	}

	/**
	 * @author skallesh
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21900", "RHEL7-51761"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "subscription-manager: attempt register to with white space in the user name should fail",
			groups = {"Tier3Tests","testAttemptRegisterWithWhiteSpacesInUsername", "blockedByBug-719378" },
			enabled = true)
	public void testAttemptRegisterWithWhiteSpacesInUsername() {
		SSHCommandResult result = clienttasks.register_("user name", "password", sm_clientOrg, null, null, null, null,
				null, null, null, (String) null, null, null, null, true, null, null, null, null, null);
		String expectedStderr="The expected stdout result when attempting to register with a username containing whitespace.";
		String expectedStderrMsg = servertasks.invalidCredentialsMsg();
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-1")) expectedStderrMsg = "HTTP error (401 - Unauthorized): "+expectedStderrMsg;	// post commit 630e1a2eb06e6bfacac669ce11f38e228c907ea9 1507030: RestlibExceptions should show they originate server-side
		Assert.assertEquals(result.getStderr().trim(), expectedStderrMsg,expectedStderr);
	}

	/**
	 * @author skallesh
	 * @throws JSONException
	 * @throws Exception
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21936", "RHEL7-51798"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Auto-heal for partial subscription",
			groups = {"Tier3Tests","autohealPartial", "blockedByBug-746218","blockedByBug-907638", "blockedByBug-907400" },
			enabled = true)
	public void testAutohealForPartialSubscription() throws Exception {
		Integer moreSockets = 0;
		List<String> productIds = new ArrayList<String>();
		List<String> poolId = new ArrayList<String>();
		Map<String, String> factsMap = new HashMap<String, String>();
		factsMap.put("virt.is_guest", Boolean.FALSE.toString());
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.autoheal(null, null, true, null, null, null, null); // disable
		// autoheal
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {

			if (CandlepinTasks.isPoolProductMultiEntitlement(sm_clientUsername, sm_clientPassword, sm_serverUrl,
					pool.poolId)) {
				String poolProductSocketsAttribute = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername,
						sm_clientPassword, sm_serverUrl, pool.poolId, "stacking_id");
				if ((!(poolProductSocketsAttribute == null)) && (poolProductSocketsAttribute.equals("1"))) {
					String SocketsCount = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername,
							sm_clientPassword, sm_serverUrl, pool.poolId, "sockets");
					poolId.add(pool.poolId);
					moreSockets += Integer.valueOf(SocketsCount);
					productIds.addAll(CandlepinTasks.getPoolProvidedProductIds(sm_clientUsername, sm_clientPassword,
							sm_serverUrl, pool.poolId));
				}
			}
		}
		if (moreSockets == 0)
			throw new SkipException(
					"Expected to find a sockets based multi-entitlement pool with stacking_id 1 for this test.");
		factsMap.put("cpu.cpu_socket(s)", String.valueOf((++moreSockets) + Integer.valueOf(clienttasks.sockets)));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);

		for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()) {
			if (productIds.contains(installedProduct.productId)) {
				Assert.assertEquals(installedProduct.status, "Partially Subscribed");
			}
		}
		Assert.assertTrue(!productIds.isEmpty(), "Found installed products that are partially subscribed after adding "
				+ moreSockets + " more cpu.cpu_socket(s).");
		clienttasks.autoheal(null, true, null, null, null, null, null); // enable
		// autoheal
		clienttasks.run_rhsmcertd_worker(true); // trigger autoheal
		for (InstalledProduct installedProduct : clienttasks.getCurrentlyInstalledProducts()) {
			for (String productId : productIds) {
				if (productId.equals(installedProduct.productId))
					Assert.assertEquals(installedProduct.status, "Subscribed",
							"Status of installed product '" + installedProduct.productName + "' after auto-healing.");
			}
		}

	}

	/**
	 * @author skallesh
	 * @throws JSONException
	 * @throws Exception
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21938", "RHEL7-51800"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify healing only attaches with a service-level that is set on the system's preference",
			groups = {"Tier3Tests","AutoHealWithSLA", "blockedByBug-907638", "blockedByBug-907400" },
			enabled = true)
	public void testAutohealWithSLA() throws JSONException, Exception {
		/*
		 * not necessary; will use clienttasks.run_rhsmcertd_worker(true) to
		 * invoke an immediate autoheal Integer autoAttachInterval = 2;
		 */
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);
		List<String> availableServiceLevelData = clienttasks.getCurrentlyAvailableServiceLevels();

		String randomServiceLevel = null;
		for (String randomAvailableServiceLevel : getRandomSubsetOfList(availableServiceLevelData,
				availableServiceLevelData.size())) {
			randomServiceLevel = randomAvailableServiceLevel;
			clienttasks.subscribe_(true, randomAvailableServiceLevel, (String) null, null, null, null, null, null, null,
					null, null, null, null);
			if (!clienttasks.getCurrentEntitlementCertFiles().isEmpty())
				break;
		}
		if (clienttasks.getCurrentEntitlementCertFiles().isEmpty())
			throw new SkipException(
					"Could not find an available SLA that could be used to auto subscribe coverage for an installed product.");
		String currentServiceLevel = clienttasks.getCurrentServiceLevel();
		Assert.assertEquals(randomServiceLevel, currentServiceLevel,
				"The current service level should report the same value used during autosubscribe.");
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();

		clienttasks.autoheal(null, true, null, null, null, null, null);
		clienttasks.run_rhsmcertd_worker(true);
		List<ProductSubscription> productSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertTrue(!productSubscriptions.isEmpty(), "Autoheal with serviceLevel '" + currentServiceLevel
				+ "' has granted this system some entitlement coverage.");
		for (ProductSubscription productSubscription : productSubscriptions) {
			// TODO Fix the exempt service level logic in this loop after
			// implementation of Bug 1066088 - [RFE] expose an option to the
			// servicelevels api to return exempt service levels
			if (!sm_exemptServiceLevelsInUpperCase.contains("Exempt SLA".toUpperCase()))
				sm_exemptServiceLevelsInUpperCase.add("Exempt SLA".toUpperCase());
			// WORKAROUND for bug 1066088
			if (sm_exemptServiceLevelsInUpperCase.contains(productSubscription.serviceLevel.toUpperCase())) {
				Assert.assertTrue(
						sm_exemptServiceLevelsInUpperCase.contains(productSubscription.serviceLevel.toUpperCase()),
						"Autohealed subscription '" + productSubscription.productName
								+ "' has been granted with an exempt service level '" + productSubscription.serviceLevel
								+ "'.");
			} else if ((productSubscription.serviceLevel == null || productSubscription.serviceLevel.isEmpty())
					&& SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">", "2.0.2-1")) {
				log.info("Due to Bug 1223560, Autoheal with serviceLevel '" + currentServiceLevel
						+ "' granted this system coverage from subscription '" + productSubscription.productName
						+ "' which actually has no service level.");
			} else {
				Assert.assertEquals(productSubscription.serviceLevel, currentServiceLevel, "Autohealed subscription '"
						+ productSubscription.productName + "' has been granted with the expected service level.");
			}
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21901", "RHEL7-51762"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verfying Auto-heal when auto-heal parameter is turned off",
			groups = {"Tier3Tests","AutohealTurnedOff","blockedByBug-726411" },
			enabled = true)
	public void testAutohealTurnedOff() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);

		clienttasks.autoheal(null, null, true, null, null, null, null);
		clienttasks.run_rhsmcertd_worker(true);
		List<EntitlementCert> certs = clienttasks.getCurrentEntitlementCerts();
		Assert.assertTrue((certs.isEmpty()),
				"When autoheal has been disabled, no entitlements should be granted after the rhsmcertd worker has run.");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21972", "RHEL7-51834"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify if Subscription manager displays incorrect status for partially subscribed subscription",
			groups = {"Tier3Tests","VerifyStatusForPartialSubscription", "blockedByBug-743710" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 119327)
	public void testStatusForPartialSubscription() throws JSONException, Exception {

		String Flag = "false";
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);

		Map<String, String> factsMap = new HashMap<String, String>();
		factsMap.put("virt.is_guest", String.valueOf(Boolean.FALSE));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		for (SubscriptionPool SubscriptionPool : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
			if (!CandlepinTasks.isPoolProductMultiEntitlement(sm_clientUsername, sm_clientPassword, sm_serverUrl,
					SubscriptionPool.poolId)) {
				String poolProductSocketsAttribute = CandlepinTasks.getPoolProductAttributeValue(sm_clientUsername,
						sm_clientPassword, sm_serverUrl, SubscriptionPool.poolId, "sockets");
				if ((!(poolProductSocketsAttribute == null)) && (poolProductSocketsAttribute.equals("2"))) {
					clienttasks.subscribeToSubscriptionPool_(SubscriptionPool);
					Flag = "true";
				}
			}
		}
		Assert.assertTrue(Boolean.valueOf(Flag),
				"Found and subscribed to non-multi-entitlement 2 socket subscription pool(s) for this test.");
		Integer moreSockets = 4;
		factsMap.put("cpu.cpu_socket(s)", String.valueOf(moreSockets + Integer.valueOf(clienttasks.sockets)));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		Flag = "false";
		for (InstalledProduct product : clienttasks.getCurrentlyInstalledProducts()) {
			if (!product.status.equals("Not Subscribed") && !product.status.equals("Subscribed")
					&& !product.status.equals("Unknown")) {
				Assert.assertEquals(product.status, "Partially Subscribed",
						"Installed product '" + product.productName + "' status is Partially Subscribed.");
				Flag = "true";
			}
		}
		Assert.assertEquals(Flag, "true", "Verified Partially Subscribed installed product(s).");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-26776", "RHEL7-63526"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Auto-heal for Expired subscription",
			groups = {"Tier3Tests","AutohealForExpired", "blockedByBug-746088","blockedByBug-907638", "blockedByBug-907400","blockedByBug-1555582" },
			enabled = true)
	public void testAutohealForExpiredSubscription() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				consumerId);

		int endingMinutesFromNow=1;
		String expiringPoolId = createTestPool(-60 * 24, endingMinutesFromNow,false);
		Calendar c1 = new GregorianCalendar();
		clienttasks.subscribe(null, null, expiringPoolId, null, null, null, null, null, null, null, null, null, null);		
		Calendar c2 = new GregorianCalendar();
		// wait for the pool to expire
		// sleep(endingMinutesFromNow*60*1000);
		// trying to reduce the wait time for the expiration by subtracting off
		// some expensive test time
		sleep(1 * 60 * 1000 - (c2.getTimeInMillis() - c1.getTimeInMillis()));
		InstalledProduct productCertBeforeHealing = ProductCert.findFirstInstanceWithMatchingFieldFromList("status",
				"Expired", clienttasks.getCurrentlyInstalledProducts());		
		Assert.assertEquals(productCertBeforeHealing.status, "Expired");
		clienttasks.run_rhsmcertd_worker(true);
		InstalledProduct productCertAfterHealing = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId",
				productCertBeforeHealing.productId, clienttasks.getCurrentlyInstalledProducts());
		Assert.assertEquals(productCertAfterHealing.status, "Subscribed");

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21937", "RHEL7-51799"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Auto-heal for subscription",
			groups = {"Tier3Tests","AutoHeal", "blockedByBug-907638","blockedByBug-726411", "blockedByBug-907400" },
			enabled = true)
	@ImplementsNitrateTest(caseId = 119327)
	public void testAutohealForSubscription() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		Assert.assertTrue(clienttasks.getCurrentEntitlementCerts().isEmpty(),
				"After immediately registering with force, there are no entitlements attached.");
		clienttasks.run_rhsmcertd_worker(true);
		List<ProductSubscription> consumed = clienttasks.getCurrentlyConsumedProductSubscriptions();
		log.info("Currently the consumed products are" + consumed.size());
		// this assertion assumes that the currently available subscriptions
		// provide coverage for the currently installed products
		Assert.assertTrue(!clienttasks.getCurrentEntitlementCerts().isEmpty(),
				"Asserting that entitlement certs have been granted to the system indicating that autoheal was successful invoked to cover its currently installed products.");
	}

	/**
	 * @author skallesh
	 * @throws JSONException
	 * @throws Exception
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21935", "RHEL7-51797"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Auto-heal for a system with preference Premium for a product with SLA Standard should fail",
			groups = {"Tier3Tests","AutoHealFailForSLA" },
			enabled = true)
	public void testAutohealFailForSLA() throws JSONException, Exception {

		List<ProductCert> productCerts = clienttasks.getCurrentProductCerts();
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		/*List<String> availableServiceLevelData = clienttasks.getCurrentlyAvailableServiceLevels();
		String availableService = availableServiceLevelData
				.get(randomGenerator.nextInt(availableServiceLevelData.size()));*/
		clienttasks.service_level(null, null, "Standard", null, null, null, null, null, null, null, null, null, null);
		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
		clienttasks.subscribe(true, null, (String) null, null, null, null, null, null, null, null, null, null, null);
		ProductCert productCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId",
						"32060", productCerts);

		configureTmpProductCertDirWithInstalledProductCerts(Arrays.asList(productCert));

		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
		clienttasks.service_level(null, null, "Premium", null, null, null, null, null, null, null, null, null, null);

		clienttasks.run_rhsmcertd_worker(true);
		List<ProductSubscription> consumed = clienttasks.getCurrentlyConsumedProductSubscriptions();
		Assert.assertTrue((consumed.isEmpty()), "autoheal has failed");
	}

	/**
	 * @author skallesh
	 * @throws IOException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21958", "RHEL7-51820"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "subscription-manager: subscribe multiple pools in incorrect format",
			groups = {"Tier3Tests","MysubscribeTest", "blockedByBug-772218" },
			enabled = true)
	public void testSubscribingWithIncorrectFormatForPoolId() throws IOException {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		List<String> poolid = new ArrayList<String>();
		for (SubscriptionPool pool : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
			poolid.add(pool.poolId);
		}
		if (poolid.isEmpty())
			throw new SkipException(
					"Cannot randomly pick a pool for subscribing when there are no available pools for testing.");
		int i = randomGenerator.nextInt(poolid.size());
		int j = randomGenerator.nextInt(poolid.size());
		if (i == j) {
			j = randomGenerator.nextInt(poolid.size());
		}
		SSHCommandResult subscribeResult = subscribeInvalidFormat_(null, null, poolid.get(i), poolid.get(j), null, null,
				null, null, null, null, null, null);
		Assert.assertEquals(subscribeResult.getStdout().trim(), "cannot parse argument: " + poolid.get(j));

	}

	/**
	 * @author skallesh
	 * @throws Exception
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21946", "RHEL7-51808"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify that Entitlement Start Dates is the Subscription Start Date",
			groups = {"Tier3Tests","VerifyEntitlementStartDate_Test", "blockedByBug-670831" },
			enabled = true)
	public void testEntitlementStartDate() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);
		for (SubscriptionPool pool : getRandomSubsetOfList(clienttasks.getCurrentlyAvailableSubscriptionPools(), 5)) {
			JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,
					sm_clientPassword, sm_serverUrl, "/pools/" + pool.poolId));
			Calendar subStartDate = parseISO8601DateString(jsonPool.getString("startDate"), "GMT");
			EntitlementCert entitlementCert = clienttasks
					.getEntitlementCertFromEntitlementCertFile(clienttasks.subscribeToSubscriptionPool_(pool));
			Calendar entStartDate = entitlementCert.validityNotBefore;
			Assert.assertTrue(entStartDate.compareTo(subStartDate) == 0,
					"" + "The entitlement start date granted from pool '" + pool.poolId + "' (" + pool.productId
							+ ") in '" + entitlementCert.file + "', '" + OrderNamespace.formatDateString(entStartDate)
							+ "', " + "should match json start date '" + jsonPool.getString("startDate")
							+ "' of the subscription pool it came from.");
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21975", "RHEL7-51837"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify if architecture for auto-subscribe test",
			groups = {"Tier3Tests","VerifyarchitectureForAutobind_Test", "blockedByBug-664847" },
			enabled = true)
	public void testArchitectureForAutobind() throws Exception {

		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		Map<String, String> result = clienttasks.getFacts();
		String arch = result.get("uname.machine");
		List<String> cpu_arch = new ArrayList<String>();
		String input = "x86_64|i686|ia64|ppc|ppc64|s390x|s390";
		String[] values = input.split("\\|");
		Boolean flag = false;
		Boolean expected = true;
		for (int i = 0; i < values.length; i++) {
			cpu_arch.add(values[i]);
		}

		Pattern p = Pattern.compile(arch);
		Matcher matcher = p.matcher(input);
		while (matcher.find()) {
			String pattern_ = matcher.group();
			cpu_arch.remove(pattern_);

		}
		String architecture = cpu_arch.get(randomGenerator.nextInt(cpu_arch.size()));
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if ((pool.subscriptionName).contains(" " + architecture)) {
				flag = true;
				Assert.assertEquals(flag, expected);
			}

		}

		for (SubscriptionPool pools : clienttasks.getCurrentlyAllAvailableSubscriptionPools()) {
			if ((pools.subscriptionName).contains(architecture)) {
				flag = true;
				Assert.assertEquals(flag, expected);
			}

		}
		Map<String, String> factsMap = new HashMap<String, String>();
		factsMap.put("uname.machine", String.valueOf(architecture));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21969", "RHEL7-51831"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify that rhsm.log reports all products provided by an attached subsubscription.",
			groups = {"Tier3Tests","blockedByBug-668032", "VerifyRhsmLogsProvidedProducts", "blockedByBug-1389559" },
			enabled = true)
	public void testRhsmLogsProvidedProducts() {
		client.runCommandAndWait("rm -f " + clienttasks.rhsmLogFile);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);
		boolean foundSubscriptionProvidingMultipleProducts = false;
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			if (pool.provides.size() > 2) {
				foundSubscriptionProvidingMultipleProducts = true;

				String logMarker = System.currentTimeMillis()
						+ " VerifyRhsmLogsProvidedProducts_Test ****************************************";
				RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);
				File serialFile = clienttasks.subscribeToSubscriptionPool(pool, sm_clientUsername, sm_clientPassword,
						sm_serverUrl);
				BigInteger serialNumber = clienttasks.getSerialNumberFromEntitlementCertFile(serialFile);
				String rhsmLogTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker,
						serialNumber.toString());
				// assert that the rhsm.log reports a message for all of the
				// products provided for by this entitlement
				for (String providedProduct : pool.provides) {
					if (providedProduct.equals("Awesome OS Server Bundled"))
						continue; // avoid Bug 1016300 - the "Provides:" field
					// in subscription-manager list --available
					// should exclude "MKT" products.
					String expectedLogMessage = String.format("[sn:%s (%s,) @ %s]", serialNumber.toString(),
							providedProduct, serialFile.getPath());
					System.out.println(rhsmLogTail + "rhsm tail");
					Assert.assertTrue(rhsmLogTail.contains(expectedLogMessage),
							"Log file '" + clienttasks.rhsmcertdLogFile + "' reports expected message '"
									+ expectedLogMessage + "'.");
				}
			}
		}
		if (!foundSubscriptionProvidingMultipleProducts)
			throw new SkipException(
					"Could not find and available subscriptions providing multiple products to test Bug 668032.");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21953", "RHEL7-51815"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify if future entitlements are disregarded by autosubscribe when determining what should be subscribed to satisfy compliance today",
			groups = {"Tier3Tests","VerifyFutureSubscription_Test", "blockedByBug-746035","blockedByBug-1440180","blockedByBug-1555582" },
			enabled = true)
	public void testFutureSubscription() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		ownerKey = CandlepinTasks.getOwnerKeyOfConsumerId(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
		consumerId);
		String futurePool = createTestPool(60 * 24 *365, 60 * 24 *(365*2),true);
		boolean assertedFutureSubscriptionIsNowSubscribed = false;	
		
		clienttasks.subscribe(null, null, futurePool, null, null, null, null, null, null, null, null, null, null);
		
		InstalledProduct installedProductBeforeAutoAttach = InstalledProduct.findFirstInstanceWithCaseInsensitiveMatchingFieldFromList("status",
									"Future Subscription", clienttasks.getCurrentlyInstalledProducts());
	
		Assert.assertNotNull(installedProductBeforeAutoAttach, "Found installed product that is covered by a inactive subscription");
		
		clienttasks.subscribe(true, null, (String) null, null, null, null, null, null, null, null, null, null, null);
		
		InstalledProduct installedProductAfterAutoAttach = InstalledProduct.findFirstInstanceWithCaseInsensitiveMatchingFieldFromList("productId",
									installedProductBeforeAutoAttach.productId, clienttasks.getCurrentlyInstalledProducts());
		List<String> installedProductArches = new ArrayList<String>(Arrays.asList(installedProductAfterAutoAttach.arch.trim().split(" *, *")));
		// Note: the arch can be a comma separated list of values
		if (installedProductArches.contains("x86")) {
		    installedProductArches.addAll(Arrays.asList("i386", "i486", "i586", "i686"));
		} // Note: x86 is a general alias to cover all 32-bit intel
		// microprocessors, expand the x86 alias
		if (installedProductArches.contains(clienttasks.arch) || installedProductArches.contains("ALL")) {
		    Assert.assertEquals(installedProductAfterAutoAttach.status.trim(), "Subscribed", "Previously installed product '"
		    + installedProductAfterAutoAttach.productName
		    + "' covered by a Future Subscription should now be covered by a current subscription after auto-subscribing.");
		    assertedFutureSubscriptionIsNowSubscribed = true;
		} else {
		    Assert.assertEquals(installedProductAfterAutoAttach.status.trim(), "Future Subscription",
		    "Mismatching arch installed product '" + installedProductAfterAutoAttach.productName + "' (arch='" + installedProductAfterAutoAttach.arch
		    + "') covered by a Future Subscription should remain unchanged after auto-subscribing.");
		}

		Assert.assertTrue(assertedFutureSubscriptionIsNowSubscribed,
		"Verified at least one previously installed product covered by a Future Subscription is now covered by a current subscription after auto-subscribing.");
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21977", "RHEL7-51839"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify if the status of installed products match when autosubscribed,and when you subscribe all the available products",
			groups = {"Tier3Tests","VerifyautosubscribeTest" },
			enabled = true)
	public void testAutosubscribe() throws JSONException, Exception {

		List<String> ProductIdBeforeAuto = new ArrayList<String>();
		List<String> ProductIdAfterAuto = new ArrayList<String>();
		clienttasks.deleteFactsFileWithOverridingValues();
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		for (InstalledProduct installedProductsBeforeAuto : clienttasks.getCurrentlyInstalledProducts()) {
			if (installedProductsBeforeAuto.status.equals("Subscribed"))
				ProductIdBeforeAuto.add(installedProductsBeforeAuto.productId);
		}

		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
		clienttasks.subscribe_(true, null, (String) null, null, null, null, null, null, null, null, null, null, null);
		for (InstalledProduct installedProductsAfterAuto : clienttasks.getCurrentlyInstalledProducts()) {
			if (installedProductsAfterAuto.status.equals("Subscribed"))
				ProductIdAfterAuto.add(installedProductsAfterAuto.productId);
		}
		Assert.assertEquals(ProductIdBeforeAuto.size(), ProductIdAfterAuto.size());
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21976", "RHEL7-51838"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify if autosubscribe ignores socket count on non multi-entitled subscriptions",
			groups = {"Tier3Tests","VerifyautosubscribeIgnoresSocketCount_Test", "blockedByBug-743704" },
			enabled = true)
	public void testAutosubscribeIgnoresSocketCount() throws Exception {
		// InstalledProduct installedProduct =
		// InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId",
		// "1000000000000023", clienttasks.getCurrentlyInstalledProducts());
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);

		Map<String, String> factsMap = new HashMap<String, String>();
		factsMap.put("cpu.cpu_socket(s)", String.valueOf(4));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);

		clienttasks.subscribe(true, null, (String) null, null, null, null, null, null, null, null, null, null, null);
		Boolean Flag = false;

		factsMap.put("cpu.cpu_socket(s)", String.valueOf(1));
		clienttasks.createFactsFileWithOverridingValues(factsMap);
		clienttasks.facts(null, true, null, null, null, null);
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		clienttasks.subscribe(true, null, (String) null, null, null, null, null, null, null, null, null, null, null);

		InstalledProduct installedProductsAfterAuto = InstalledProduct.findFirstInstanceWithMatchingFieldFromList(
				"productId", "1000000000000023", clienttasks.getCurrentlyInstalledProducts());

		if (installedProductsAfterAuto.status.equals("Subscribed")) {
			Flag = true;

			Assert.assertTrue(Flag, "Auto-attach doesnot ignore socket count");
		}
	}

	/**
	 * @author skallesh
	 * @throws Exception
	 * @throws JSONException
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21959", "RHEL7-51821"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "subscription-manager: entitlement key files created with weak permissions",
			groups = {"Tier3Tests","MykeyTest", "blockedByBug-720360" },
			enabled = true)
	public void testKeyFilePermissions() throws JSONException, Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		String subscribeResult = getEntitlementCertFilesWithPermissions();
		Pattern p = Pattern.compile("[,\\s]+");
		String[] result = p.split(subscribeResult);
		String permissions = "-rw-------"; // RHEL5
		if (Integer.valueOf(clienttasks.redhatReleaseX) > 5)
			permissions = "-rw-------.";
		for (int i = 0; i < result.length; i++) {
			Assert.assertEquals(result[i], permissions,
					"permission for etc/pki/entitlement/<serial>-key.pem is -rw-------");
			i++;
		}
	}

	/*
	 * @author redakkan
	 *
	 * @throws exception
	 *
	 * @throws JSONException *
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL7-55664"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify the file permissions on /var/lib/rhsm/cache and facts files",
			groups = {"Tier3Tests","blockedByBug-1297485", "blockedByBug-1297493", "blockedByBug-1340525","blockedByBug-1389449" },
			enabled = true)
	public void testCacheAndFactsFilePermissions() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager", "<", "1.17.7-1")) {
			// subscription-manager commit
			// 9dec31c377b57b4c98f845c018a5372d6f650d88
			// 1297493, 1297485: Restrict visibility of subscription-manager
			// caches.
			throw new SkipException(
					"This test applies a newer version of subscription manager that includes fixes for bugs 1297493 and 1297485.");
		}
		String command = clienttasks.rhsmCacheDir;
		SSHCommandResult result = client.runCommandAndWait("stat -c '%a' " + command); // gets
		// the File /var/lib/rhsm/cache access rights in octal
		Assert.assertEquals(result.getStdout().trim(), "750", "Expected permission on /var/lib/rhsm/cache is 750"); // post
		// commit
		// 9dec31c377b57b4c98f845c018a5372d6f650d88
		SSHCommandResult result1 = client.runCommandAndWait("stat -c '%a' /var/lib/rhsm/facts"); // gets
		// the File /var/lib/rhsm/facts access rights in octal
		Assert.assertEquals(result1.getStdout().trim(), "750", "Expected permission on /var/lib/rhsm/facts is 750"); // post
		// commit
		// 9dec31c377b57b4c98f845c018a5372d6f650d88
	}

	/*
	 * @author redakkan
	 *
	 * @throws exception
	 *
	 * @throws JSONException *
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47894", "RHEL7-96268"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28545",	// RHSM-REQ : subscription-manager cli repo listing and override management
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84934",	// RHSM-REQ : subscription-manager cli repo listing and override management
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.MEDIUM, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description= "verify repo-override --remove='' does not remove the overrides from the given repo",
			groups= {"Tier3Tests","blockedByBug-1331739"},
			enabled= true)
	public void testEmptyRepoOverrideRemove() throws JSONException, Exception {

		  if (clienttasks.isPackageVersion("subscription-manager", "<",
		  "1.19.4-1")) { // fix : https://github.com/candlepin/subscription-manager/pull/1474
		  new SkipException(
		  "This test applies a newer version of subscription manager that includes fixes for bugs 1331739"
		  ); }


		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);
		// subscribe to a random pool
		List<YumRepo> originalYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		if (originalYumRepos.isEmpty())
			throw new SkipException(
					"After registering with auto-subscribe, no yum repos were entitled. This test requires some redhat repos.");

		// choose a random small subset of repos to test repo-override
		List<YumRepo> originalYumReposSubset = getRandomSubsetOfList(originalYumRepos, 3);
		Map<String, Map<String, String>> repoOverridesMapOfMaps = new HashMap<String, Map<String, String>>();
		Map<String, String> repoOverrideNameValueMap = new HashMap<String, String>();
		repoOverrideNameValueMap.put("enabled", "true");
		repoOverrideNameValueMap.put("gpgcheck", "false");
		repoOverrideNameValueMap.put("exclude", "foo-bar");
		for (YumRepo yumRepo : originalYumReposSubset)
			repoOverridesMapOfMaps.put(yumRepo.id, repoOverrideNameValueMap);
		List<String> repoIds = new ArrayList<String>(repoOverridesMapOfMaps.keySet());
		// Creating repo-overrides on the selected repo
		clienttasks.repo_override(null, null, repoIds, null, repoOverrideNameValueMap, null, null, null, null);

		// Gets the current repo-override list from the system
		SSHCommandResult listResultBeforeRemove = clienttasks.repo_override_(true, null, (String) null, (String) null,
				null, null, null, null, null);
		// repo-override remove with empty set of name values
		SSHCommandResult result = clienttasks.repo_override_(null, null, repoIds, Arrays.asList(new String[] { "" }),
				null, null, null, null, null);
		// Gets the current repo-override list AFTER REMOVE from the system
		SSHCommandResult listResultAfterRemove = clienttasks.repo_override_(true, null, (String) null, (String) null,
				null, null, null, null, null);
		Assert.assertEquals(listResultBeforeRemove.getStdout().trim(), listResultAfterRemove.getStdout().trim(),
				"Repo-overrides list After subscription-manager repo-override --repo=<id> --remove='' should be identical to the list before executing the command");
		Assert.assertEquals(result.getStderr().trim(), "Error: You must specify an override name with --remove.");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(64),
                "ExitCode of subscription-manager repo-override --remove without names should be 64");
    }

	/**
	 * @author redakkan
	 * @throws Exception
	 *             JSON Exception
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-38192", "RHEL7-77384"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "Verify the newly added content set is immediatly available on the client",
			groups = {"Tier3Tests","VerifyNewContentAvailability", "blockedByBug-1360909" },
			enabled = true)
	public void testNewContentAvailability() throws JSONException, Exception {
		String resourcePath = null;
		String requestBody = null;
		String ProductId = "32060";
		String contentId = "1234";
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, false, null, null, null, null);
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();

		// checking subscribed repos
		List<Repo> subscribedRepo = clienttasks.getCurrentlySubscribedRepos();
		if (subscribedRepo.isEmpty())
			throw new SkipException("There are no entitled yum repos available for this test.");

		// getting the list of all enabled repos
		//SSHCommandResult sshCommandResult = clienttasks.repos(null, true, false, (String) null, (String) null, null,
		//null, null);
		// in test data verifying that already enabled repos are not available
		//List<Repo> listEnabledRepos = Repo.parse(sshCommandResult.getStdout());
		//Assert.assertTrue(listEnabledRepos.isEmpty(), "No attached subscriptions provides a enabled repos .");

		//Create a new content "Newcontent_foo"
		requestBody = CandlepinTasks.createContentRequestBody("Newcontent_foo", contentId, "Newcontent_foo", "yum",
				"Foo Vendor", "/foo/path", "/foo/path/gpg", null, null, null, null).toString();
		resourcePath = "/content";

		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath, requestBody);
		// Link the newly created content to product id , by default the repo is enabled
		CandlepinTasks.addContentToProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				sm_clientOrg, ProductId, contentId, true);
		CandlepinTasks.postResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath, requestBody);

		// any newly added content to the product should be immediately
		// available when using server > 2.0

		List<Repo> listEnabledRepos;
		SSHCommandResult sshCommandResult;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0")) {
			// look for the newly added content available in repo list-enabled
			// by getting the list of currently enabled repos
			sshCommandResult = clienttasks.repos(null, true, null, (String) null, (String) null, null, null, null, null);
			listEnabledRepos = Repo.parse(sshCommandResult.getStdout());
			for (Repo repo : listEnabledRepos) {
				if (repo.repoId.matches("Newcontent_foo")) {
					Assert.assertTrue(repo.repoId.matches("Newcontent_foo"), "contains newly added repos Newcontent_foo");
					Assert.assertNotNull(listEnabledRepos, "Enabled yum repo [" + repo.repoId + "] is included in the report of repos --list-enabled.");
				}
			}
		} else if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, "<", "2.0.0")
				&& (clienttasks.isPackageVersion("subscription-manager", ">=", "1.17.10-1"))) { // commit
			// c38ae2c2e2f0e59674aa670d8ff3264d66737ede	// Bug // 1360909 // -// Clients unable to access newly released content (Satellite 6.2 GA)

			// remember the currently consumed product subscriptions
			List<ProductSubscription> consumedProductSubscriptions = clienttasks
					.getCurrentlyConsumedProductSubscriptions();

			// refresh to update the entitlement certs on the client
			log.info("Refresh...");
			clienttasks.refresh(null, null, null, null);

			sshCommandResult = clienttasks.repos(null, true, null, (String) null, (String) null, null, null, null, null);
			listEnabledRepos = Repo.parse(sshCommandResult.getStdout());
			for (Repo repo : listEnabledRepos) {
				if (repo.repoId.equals("Newcontent_foo")) {
					Assert.assertTrue(repo.repoId.matches("Newcontent_foo"), "contains newly added repos Newcontent_foo");
					Assert.assertNotNull(listEnabledRepos, "Enabled yum repo [" + repo.repoId + "] is included in the report of repos --list-enabled.");
				}
			}
			// Assert the entitlement certs are restored after the refresh
			log.info("After running refresh, assert that the entitlement certs are restored...");

			Assert.assertEquals(clienttasks.getCurrentlyConsumedProductSubscriptions().size(),
					consumedProductSubscriptions.size(), "all the consumed product subscriptions have been restored.");

		} else {
			throw new SkipException("Bugzilla 1360909 was not fixed in this old version of subscription-manager.");
		}

	}

	/**
	 * @author redakkan
	 * @throws Exception
	 * JSON Exception
	 */
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47892", "RHEL7-96016"},
			linkedWorkItems= {
				@LinkedItem(
					workitemId= "RHEL6-28489",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RHEL6,
					role= DefTypes.Role.VERIFIES),
				@LinkedItem(
					workitemId= "RHEL7-84911",	// RHSM-REQ : subscription-manager cli attaching and removing subscriptions
					project= Project.RedHatEnterpriseLinux7,
					role= DefTypes.Role.VERIFIES)},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.LOW, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description= "subscription-manager: verify subscribe with --file=invalid file from stdin is handled properly",
			groups= {"Tier3Tests","blockedByBug-1350402","testSubscribeWithInvalidFileFromStdin"},
			enabled= true)
	//@ImplementsNitrateTest(caseId=)
	public void testSubscribeWithInvalidFileFromStdin() throws JSONException, Exception {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.13.8-1")) throw new SkipException("The attach --file function was not implemented in this version of subscription-manager.");	// commit 3167333fc3a261de939f4aa0799b4283f2b9f4d2 bug 1159974
		if (clienttasks.isPackageVersion("subscription-manager","<","1.20.1-1")) throw new SkipException("The currently installed version ("+clienttasks.installedPackageVersionMap.get("subscription-manager")+") is blocked by bug 1350402 which is fixed in subscription-manager-1.20.1-1 and newer.");	// commit cda076cce4ac66d09eba31b64454d2780a6d1312 bug 1350402

		if (clienttasks.getCurrentlyRegisteredOwnerKey() == null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		} else clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null, null, null);

		String InvalidFile = "/tmp/invalid.txt";

        /*About the bug : When stdout/stderr output of subscription-manager is redirected using pipe to some other process ... e.g. subscription-manager list --all --available --pool-only | subscription-manager subscribe --file=/tmp/invalid
        and the second process does not read anything from stdout like in this case, then subscription-manager cannot write the buffer anywhere hence the command was terminated with some errors
        The bug is reproducible only with stdin , hence passing the poolOnlyListCommand to pipe the out put

        [root@ibm-x3250m3-01 ~]# subscription-manager list --all --available --pool-only | subscription-manager subscribe --file=/tmp/invalid
		Error: The file "/tmp/invalid" does not exist or cannot be read.
		close failed in file object destructor:
		sys.excepthook is missing
		lost sys.stderr
        */
		String poolOnlyListCommand = clienttasks.listCommand(true, true, null, null, null, null, null, null, null, null, true, null, null, null, null);
		String stdinFileSubscribeCommand = clienttasks.subscribeCommand(null, null, (List<String>) null, (List<String>) null, null, null, null, null,InvalidFile, null, null, null, null);
		SSHCommandResult stdinFileSubscribeCommandResult = client.runCommandAndWait(poolOnlyListCommand +"|" +stdinFileSubscribeCommand, (long) (3/*min*/*60*1000/*timeout*/));

		//Assert the additional error messages no longer appear
		Assert.assertEquals(stdinFileSubscribeCommandResult.getStderr().trim(),"Error: The file \"/tmp/invalid.txt\" does not exist or cannot be read.");
		 if(clienttasks.isPackageVersion("subscription-manager", ">=", "1.20.1-1")) {	// commit 79f86e4c043ee751677131ed4e3cf00affd13087
		     Assert.assertEquals(stdinFileSubscribeCommandResult.getExitCode(),Integer.valueOf(65), "Exit Code comparison between the expected result of subscribing using a list of poolids from stdin along with a invalid file.");
		 }else {
		     Assert.assertEquals(stdinFileSubscribeCommandResult.getExitCode(),Integer.valueOf(64), "Exit Code comparison between the expected result of subscribing using a list of poolids from stdin along with a invalid file.");
 		 }
	}
	
	
	@BeforeGroups(groups = "setup", value = {}, enabled = true)
	public void unsubscribeBeforeGroup() {
		clienttasks.unsubscribe(true, (BigInteger) null, null, null, null, null, null);
	}

	@BeforeGroups(groups = "setup", value = {}, enabled = true)
	public void unsetServicelevelBeforeGroup() {
		clienttasks.service_level_(null, null, null, true, null, null, null, null, null, null, null, null, null);
	}

	@BeforeGroups(groups = "setup", value = {}, enabled = true)
	public void setHealFrequencyGroup() {
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "rhsmcertd", "autoAttachInterval".toLowerCase(), "1440" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		String param = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsmcertd", "autoAttachInterval");

		Assert.assertEquals(param, "1440");
	}

	@AfterGroups(groups = "setup", value = { "VerifyRepoFileExistance" }, enabled = true)
	public void TurnonRepos() {
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "rhsm", "manage_repos", "1" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
	}

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-21934", "RHEL7-51796"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier3")
	@Test(	description = "verify that the autoheal attribute of a new system consumer defaults to true",
			groups = {"Tier3Tests"},
			enabled = true)
	public void testAutohealAttributeDefaultsToTrueForNewSystemConsumer() throws Exception {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
		String consumerId = clienttasks.getCurrentConsumerId();
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername,
				sm_clientPassword, sm_serverUrl, "/consumers/" + consumerId));

		Assert.assertTrue(jsonConsumer.getBoolean("autoheal"),
				"A new system consumer's autoheal attribute value defaults to true.");
	}

	@BeforeClass(groups = "setup")
	public void rememberConfiguredFrequencies() {
		if (clienttasks == null)
			return;
		configuredHealFrequency = Integer
				.valueOf(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsmcertd", "autoAttachInterval"));
		configuredCertFrequency = Integer
				.valueOf(clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsmcertd", "certCheckInterval"));
		configuredHostname = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "hostname");
	}

	@AfterGroups(groups = { "setup" }, value = { "VerifyRHSMCertdLogging" })
	@AfterClass(groups = "setup") // called after class for insurance
	public void restoreConfiguredFrequencies() {
		if (clienttasks == null)
			return;
		clienttasks.restart_rhsmcertd(configuredCertFrequency, configuredHealFrequency, null);
	}

	@BeforeGroups(groups = "setup", value = {}, enabled = true)
	public void configureProductCertDir() {
		if (rhsmProductCertDir == null) {
			rhsmProductCertDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "productCertDir");
			Assert.assertNotNull(rhsmProductCertDir);
		} // remember the original so it can be restored later
		RemoteFileTasks.runCommandAndAssert(client, "mkdir -p " + tmpProductCertDir, Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client, "rm -f " + tmpProductCertDir + "/*.pem", Integer.valueOf(0));
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", tmpProductCertDir);
	}

	@AfterGroups(groups = { "setup" }, value = { /* "VerifyautosubscribeTest", */"VerifyStatusForPartialSubscription",
			"certificateStacking", "VerifyautosubscribeIgnoresSocketCount_Test", "VerifyDistinct", "autohealPartial",
			"VerifyFactsListByOverridingValues" })
	@AfterClass(groups = { "setup" }) // called after class for insurance
	public void deleteFactsFileWithOverridingValues() {
		clienttasks.deleteFactsFileWithOverridingValues();
	}

	protected void configureTmpProductCertDirWithInstalledProductCerts(List<ProductCert> installedProductCerts) {
		if (rhsmProductCertDir == null) {
			rhsmProductCertDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "productCertDir");
			Assert.assertNotNull(rhsmProductCertDir);
		}
		log.info(
				"Initializing a new product cert directory with the currently installed product certs for this test...");
		RemoteFileTasks.runCommandAndAssert(client, "mkdir -p " + tmpProductCertDir, Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client, "rm -f " + tmpProductCertDir + "/*.pem", Integer.valueOf(0));
		for (ProductCert productCert : installedProductCerts) {
			RemoteFileTasks.runCommandAndAssert(client, "cp " + productCert.file + " " + tmpProductCertDir,
					Integer.valueOf(0));
		}

		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", tmpProductCertDir);
	}

	protected void configureTmpProductCertDirWithOutInstalledProductCerts() {
		if (rhsmProductCertDir == null) {
			rhsmProductCertDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "productCertDir");
			Assert.assertNotNull(rhsmProductCertDir);
		}
		RemoteFileTasks.runCommandAndAssert(client, "mkdir -p " + tmpProductCertDir, Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client, "rm -f " + tmpProductCertDir + "/*.pem", Integer.valueOf(0));
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", tmpProductCertDir);
	}

	@BeforeGroups(groups = "setup", value = { "VerifyStatusCheck" })
	@AfterGroups(groups = "setup", value = { "VerifyStatusCheck", "certificateStacking",
			"UpdateWithNoInstalledProducts", "VerifySystemCompliantFact", "AutoHealFailForSLA" })
	@AfterClass(groups = "setup") // called after class for insurance
	public void restoreRhsmProductCertDir() {
		if (clienttasks == null)
			return;
		if (rhsmProductCertDir == null)
			return;
		log.info("Restoring the originally configured product cert directory...");
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", rhsmProductCertDir);
	}

	protected String rhsmProductCertDir = null;
	protected final String tmpProductCertDir = "/tmp/sm-tmpProductCertDir-bugzillatests";

	@BeforeClass(groups = "setup")
	public void getRhsmProductCertDir() {
		rhsmProductCertDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "productCertDir");
		Assert.assertNotNull(rhsmProductCertDir);
	}

	// Protected methods
	// ***********************************************************************

	protected String setDate(String hostname, String user, String passphrase, String privatekey, String datecmd)
			throws IOException {
		SSHCommandRunner sshHostnameCommandRunner = new SSHCommandRunner(hostname, user, passphrase, privatekey, null);
		if (sm_sshEmergenecyTimeoutMS!=null) sshHostnameCommandRunner.setEmergencyTimeout(Long.valueOf(sm_sshEmergenecyTimeoutMS));
		return (sshHostnameCommandRunner.runCommandAndWait(datecmd).getStdout());

	}

	protected String getDate(String hostname, String user, String passphrase, String privatekey, Boolean flag)
			throws IOException, ParseException {
		SSHCommandRunner sshHostnameCommandRunner = new SSHCommandRunner(hostname, user, passphrase, privatekey, null);
		if (sm_sshEmergenecyTimeoutMS!=null) sshHostnameCommandRunner.setEmergencyTimeout(Long.valueOf(sm_sshEmergenecyTimeoutMS));
		if (flag)
			return (sshHostnameCommandRunner.runCommandAndWait("date +\"%F\"").getStdout());
		else
			return (sshHostnameCommandRunner.runCommandAndWait("date --date='yesterday' '+%F'").getStdout());
	}

	protected void moveProductCertFiles(String filename) throws IOException {
		String installDir = "/root/temp1/";
		if (!(RemoteFileTasks.testExists(client, installDir))) {
			client.runCommandAndWait("mkdir " + installDir);
		}

		client.runCommandAndWait("mv " + clienttasks.productCertDir + "/" + filename + " " + installDir);

	}

	protected String getEntitlementCertFilesWithPermissions() throws IOException {
		String lsFiles = client
				.runCommandAndWait(
						"ls -l " + clienttasks.entitlementCertDir + "/*-key.pem" + " | cut -d " + "' '" + " -f1,9")
				.getStdout();
		return lsFiles;
	}

	protected SSHCommandResult unsubscribeFromMultipleEntitlementsUsingSerialNumber(BigInteger SerialNumOne,
			BigInteger SerialNumTwo) throws IOException {
		return clienttasks.unsubscribe_(false, Arrays.asList(new BigInteger[] { SerialNumOne, SerialNumTwo }), null,
				null, null, null, null);
	}

	protected SSHCommandResult subscribeInvalidFormat_(Boolean auto, String servicelevel, String poolIdOne,
			String poolIdTwo, List<String> productIds, List<String> regtokens, String quantity, String email,
			String locale, String proxy, String proxyuser, String proxypassword) throws IOException {
		// client is already instantiated
		String command = clienttasks.command;
		command += " subscribe";
		if (poolIdOne != null && poolIdTwo != null)
			command += " --pool=" + poolIdOne + " " + poolIdTwo;

		// run command without asserting results
		return client.runCommandAndWait(command);
	}

	public Boolean regexInRhsmLog(String logRegex, String input) {

		Pattern pattern = Pattern.compile(logRegex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(input);
		int count = 0;
		Boolean flag = false;
		while (matcher.find()) {
			count++;
		}
		if (count >= 2) {
			flag = true;
		}
		return flag;

	}

	/**
	 * @return list of objects representing the subscription-manager list
	 *         --avail --ondate
	 */
	public List<SubscriptionPool> getAvailableFutureSubscriptionsOndate(String onDateToTest) {
		return SubscriptionPool.parse(
				clienttasks.list_(null, true, null, null, null, onDateToTest, null, null, null, null, null, null, null, null, null)
						.getStdout());
	}

	protected List<String> listFutureSubscription_OnDate(Boolean available, String ondate) {
		List<String> PoolId = new ArrayList<String>();
		SSHCommandResult result = clienttasks.list_(true, true, null, null, null, ondate, null, null, null, null, null,
				null, null, null, null);
		List<SubscriptionPool> Pool = SubscriptionPool.parse(result.getStdout());
		for (SubscriptionPool availablePool : Pool) {
			if (availablePool.multiEntitlement) {
				PoolId.add(availablePool.poolId);
			}
		}

		return PoolId;
	}

	@DataProvider(name = "getPackageFromEnabledRepoAndSubscriptionPoolData")
	public Object[][] getPackageFromEnabledRepoAndSubscriptionPoolDataAs2dArray() throws JSONException, Exception {
		return TestNGUtils.convertListOfListsTo2dArray(getPackageFromEnabledRepoAndSubscriptionPoolDataAsListOfLists());
	}

	protected List<List<Object>> getPackageFromEnabledRepoAndSubscriptionPoolDataAsListOfLists()
			throws JSONException, Exception {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		if (!isSetupBeforeSuiteComplete)
			return ll;
		if (clienttasks == null)
			return ll;
		if (sm_clientUsername == null)
			return ll;
		if (sm_clientPassword == null)
			return ll;

		// get the currently installed product certs to be used when checking
		// for conditional content tagging
		List<ProductCert> currentProductCerts = clienttasks.getCurrentProductCerts();

		// assure we are freshly registered and process all available
		// subscription pools
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, ConsumerType.system, null, null,
				null, null, null, (String) null, null, null, null, Boolean.TRUE, false, null, null, null, null);
		for (SubscriptionPool pool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {

			File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool, sm_serverAdminUsername,
					sm_serverAdminPassword, sm_serverUrl);
			Assert.assertNotNull(entitlementCertFile,
					"Found the entitlement cert file that was granted after subscribing to pool: " + pool);
			EntitlementCert entitlementCert = clienttasks
					.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				if (!contentNamespace.type.equalsIgnoreCase("yum"))
					continue;
				if (contentNamespace.enabled && clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(
						contentNamespace, currentProductCerts)) {
					String repoLabel = contentNamespace.label;

					// find an available package that is uniquely provided by
					// repo
					String pkg = clienttasks.findUniqueAvailablePackageFromRepo(repoLabel);
					if (pkg == null) {
						log.warning("Could NOT find a unique available package from repo '" + repoLabel
								+ "' after subscribing to SubscriptionPool: " + pool);
					}

					ll.add(Arrays.asList(new Object[] { pkg, repoLabel, pool }));
				}
			}
			clienttasks.unsubscribeFromSerialNumber(
					clienttasks.getSerialNumberFromEntitlementCertFile(entitlementCertFile));

			// minimize the number of dataProvided rows (useful during automated
			// testcase development)
			if (Boolean.valueOf(getProperty("sm.debug.dataProviders.minimize", "false")))
				break;
		}

		return ll;
	}

	/**
	 * @param startingMinutesFromNow
	 * @param endingMinutesFromNow
	 * @return poolId to the newly available SubscriptionPool
	 * @throws JSONException
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	protected String createTestPool(int startingMinutesFromNow, int endingMinutesFromNow, Boolean FuturePool)
			throws JSONException, Exception {
	    	String name = "BugzillaTestSubscription";
	    	providedProduct.clear();
		providedProduct.add("37060");
	    	if(FuturePool){
	    	    name = "BugillaTestInactiveSubscription";
	    	}
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.clear();
		attributes.put("version", "1.0");
		attributes.put("variant", "server");
		attributes.put("arch", "ALL");
		attributes.put("warning_period", "30");
		attributes.put("type", "MKT");
		attributes.put("type", "SVC");
		
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.1.1-1")) {
		    CandlepinTasks.deleteSubscriptionPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, sm_clientOrg, productId);
		}else {
		    CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, productId);
		}
		String resourcePath = "/products/" + productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);
		CandlepinTasks.createProductUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				sm_clientOrg, name + " BITS", productId, 1, attributes, null);
		return CandlepinTasks.createSubscriptionAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername,
				sm_serverAdminPassword, sm_serverUrl, ownerKey, 3, startingMinutesFromNow, endingMinutesFromNow,
				getRandInt(), getRandInt(), productId, providedProduct, null).getString("id");
	}

	@SuppressWarnings("deprecation")
	@AfterClass(groups = { "setup" })
	protected void DeleteTestPool() throws Exception {
		if (CandlepinType.hosted.equals(sm_serverType))
			return; // make sure we don't run this against stage/prod
		// environment
		if (sm_clientOrg == null)
			return; // must have an owner when calling candlepin APIs to delete
		// resources
		String productId = "AutoHealForExpiredProduct";
		CandlepinTasks.deleteSubscriptionsAndRefreshPoolsUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword,
				sm_serverUrl, sm_clientOrg, productId);
		String resourcePath = "/products/" + productId;
		if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.0.0"))
			resourcePath = "/owners/" + sm_clientOrg + resourcePath;
		CandlepinTasks.deleteResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl,
				resourcePath);
	}

	@AfterGroups(groups = { "setup" }, value = { "VerifyrhsmcertdRefreshIdentityCert" })
	public void restoreSystemDate() throws IOException, ParseException {
		String ClientDateAfterExecution = getDate(sm_clientHostname, sm_clientSSHUser, sm_sshKeyPrivate,
				sm_sshkeyPassphrase, true);
		String ServerDateAfterExecution = getDate(sm_serverHostname, sm_serverSSHUser, sm_sshKeyPrivate,
				sm_sshkeyPassphrase, true);
		String ClientDateAfterExeceutionOneDayBefore = getDate(sm_clientHostname, sm_clientSSHUser, sm_sshKeyPrivate,
				sm_sshkeyPassphrase, false);
		String ServerDateAfterExeceutionOneDayBefore = getDate(sm_serverHostname, sm_serverSSHUser, sm_sshKeyPrivate,
				sm_sshkeyPassphrase, false);

		if ((!(ClientDateAfterExecution.equals(SystemDateOnClient)))
				&& (!(ClientDateAfterExeceutionOneDayBefore.equals(SystemDateOnClient)))) {

			setDate(sm_clientHostname, sm_clientSSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase,
					"date -s '15 year ago 9 month ago'");
			log.info("Reverted the date of client" + client.runCommandAndWait("hostname"));
		}

		if ((!(ServerDateAfterExecution.equals(SystemDateOnServer)))
				&& ((ServerDateAfterExeceutionOneDayBefore.equals(SystemDateOnServer)))) {
			setDate(sm_serverHostname, sm_serverSSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase,
					"date -s '15 year ago 9 month ago'");
			log.info("Reverted the date of candlepin" + client.runCommandAndWait("hostname"));
		}
		clienttasks.restart_rhsmcertd(null, null, null);
		SubscriptionManagerCLITestScript.sleep(3 * 60 * 1000);
	}

	@BeforeGroups(groups = { "setup" }, value = { "VerifyrhsmcertdRefreshIdentityCert" })
	public void rgetSystemDate() throws IOException, ParseException {
		SystemDateOnClient = getDate(sm_clientHostname, sm_clientSSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase, true);
		SystemDateOnServer = getDate(sm_serverHostname, sm_serverSSHUser, sm_sshKeyPrivate, sm_sshkeyPassphrase, true);
	}

	@AfterGroups(groups = { "setup" }, value = {
			"VerifyEmptyCertCauseRegistrationFailure_Test"/*
															 * ,"BugzillaTests"
															 * CAUSES THIS
															 * METHOD TO RUN
															 * AFTER THE CLASS;
															 * NOT WHAT WE
															 * WANTED
															 */ })
	public void removeMyEmptyCaCertFile() {
		client.runCommandAndWait("rm -f " + myEmptyCaCertFile);
	}

	@AfterGroups(groups = { "setup" }, value = {
			/* "BugzillaTests", */"DisplayOfRemoteServerExceptionForServer500Error", "RHELWorkstationProduct" })
	public void restoreRHSMConfFileValues() {
		clienttasks.unregister(null, null, null, null);
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		listOfSectionNameValues.add(new String[] { "server", "prefix".toLowerCase(), "/candlepin" });
		clienttasks.config(null, null, true, listOfSectionNameValues);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null,
				null, (String) null, null, null, null, true, null, null, null, null, null);
	}



	public static Object getJsonObjectValue(JSONObject json, String jsonName) throws JSONException, Exception {
		if (!json.has(jsonName) || json.isNull(jsonName))
			return null;
		return json.get(jsonName);
	}

	static public int GBToKBConverter(int gb) {
		int value = (int) 1.049e+6; // KB per GB
		int result = (gb * value);
		return result;
	}

	// THE FOLLOWING BEFORE AND AFTER CLASS METHODS ARE USED TO ELIMINATE
	// THE INFLUENCE THAT /etc/pki/product-default/ CERTS HAVE ON THESE TESTS
	// SINCE THESE TESTS PRE-DATE THE INTRODUCTION OF DEFAULT PRODUCT CERTS.
	@BeforeClass(groups = "setup")
	public void backupProductDefaultCerts() {
		log.info(
				"This test class was developed before the addition of /etc/pki/product-default/ certs (Bug 1123029).  Therefore, let's back them up before running this test class.");
		for (File productCertFile : clienttasks.getCurrentProductCertFiles()) {
			if (productCertFile.getPath().startsWith(clienttasks.productCertDefaultDir)) {
				// copy the default installed product cert to the original /etc/pki/product/ dir
				client.runCommandAndWait("cp -n " + productCertFile + " " + clienttasks.productCertDir);
				// move the new default cert to a backup file
				client.runCommandAndWait("mv " + productCertFile + " " + productCertFile + ".bak");
			}
		}
	}


	@AfterClass(groups = "setup", alwaysRun=true)
	public void restoreProductDefaultCerts() {
		client.runCommandAndWait("ls -1 " + clienttasks.productCertDefaultDir + "/*.bak");
		String lsBakFiles = client.getStdout().trim();
		if (!lsBakFiles.isEmpty()) {
			log.info("restoring the default product cert files");
			for (String lsFile : Arrays.asList(lsBakFiles.split("\n"))) {
				// restore the default installed product cert
				client.runCommandAndWait("mv " + lsFile + " " + lsFile.replaceFirst("\\.bak$", ""));
				// remove its copy from /etc/pki/product/ dir that was created in backupProductDefaultCerts()
				client.runCommandAndWait("rm -f " + lsFile.replace(clienttasks.productCertDefaultDir, clienttasks.productCertDir).replaceFirst("\\.bak$", ""));
			}
		}
	}
	
	

}
