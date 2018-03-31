package rhsm.cli.tests;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.Repo;
import rhsm.data.SubscriptionPool;
import rhsm.data.YumRepo;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
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
 * Requirement References:
 *   Bug 642761 - (E4) DESIGN OUT As a user, I would like RHSM to select the correct n release of RHEL server to use
 *   https://engineering.redhat.com/trac/Entitlement/wiki/ReleaseVer
 * 
 * Note: To see all of the yum vars values, run this:
[root@jsefler-7 ~]#  python -c 'import yum, pprint; yb = yum.YumBase(); pprint.pprint(yb.conf.yumvar, width=1)'
Loaded plugins: product-id
{'arch': 'amd64',
 'basearch': 'x86_64',
 'releasever': '6.92Server',
 'uuid': '2c985181-c84e-43bf-a913-b240973feead'}

 */

@Test(groups={"ReleaseTests"})
public class ReleaseTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************

	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19997", "RHEL7-51030"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="attempt to get the subscription-manager release when not registered",
			groups={"Tier1Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testGetReleaseWhenNotRegistered() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null, null);
		
		SSHCommandResult result;
		
		result = clienttasks.release_(null, null, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(1), "ExitCode from calling release without being registered");
			Assert.assertEquals(result.getStderr().trim(), clienttasks.msg_ConsumerNotRegistered, "Stderr from release without being registered");
			Assert.assertEquals(result.getStdout().trim(), "", "Stdout from release without being registered");
		} else {
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from calling release without being registered");
			Assert.assertEquals(result.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered, "Stdout from release without being registered");
		}
		
		result = clienttasks.release_(null, true, null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(1), "ExitCode from calling release --list without being registered");
			Assert.assertEquals(result.getStderr().trim(), clienttasks.msg_ConsumerNotRegistered, "Stderr from release without being registered");
			Assert.assertEquals(result.getStdout().trim(), "", "Stdout from release without being registered");
		} else {
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from calling release --list without being registered");
			Assert.assertEquals(result.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered, "Stdout from release without being registered");
		}

		result = clienttasks.release_(null, null, "FOO", null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(1), "ExitCode from calling release --set without being registered");
			Assert.assertEquals(result.getStderr().trim(), clienttasks.msg_ConsumerNotRegistered, "Stderr from release without being registered");
			Assert.assertEquals(result.getStdout().trim(), "", "Stdout from release without being registered");
		} else {
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from calling release --set without being registered");
			Assert.assertEquals(result.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered, "Stdout from release without being registered");
		}
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19998", "RHEL7-51031"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="attempt to get the subscription-manager release when a release has not been set; should be told that the release is not set",
			groups={"Tier1Tests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testGetReleaseWhenReleaseHasNotBeenSet() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true, null, null, null, null, null);		
		SSHCommandResult result = clienttasks.release(null, null, null, null, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "Release not set", "Stdout from release without having set it");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19996", "RHEL7-51029"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="attempt to get the subscription-manager release list without having subscribed to any entitlements",
			groups={"Tier1Tests","blockedByBug-824979"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testGetReleaseListWithoutHavingAnyEntitlements() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true, null, null, null, null, null);
		
		// make sure we are not auto healing ourself with any entitlements
		clienttasks.autoheal(null,null,true,null,null,null, null);
		clienttasks.unsubscribe(true, (List<BigInteger>)null, null, null, null, null, null);
		
		// assert no releases are listed
		Assert.assertTrue(clienttasks.getCurrentlyAvailableReleases(null, null, null, null).isEmpty(), "No releases should be available without having been entitled to anything.");

		// assert feedback from release --list 
		SSHCommandResult result = clienttasks.release_(null,true,null,null,null, null, null, null);
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(78);	// EX_CONFIG	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		String rhelTag = "rhel-"+clienttasks.redhatReleaseX;
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.20.10-1")) {	// commit 76273403e6e3f7fa9811ef252a5a7ce4d84f7aa3	// bug 1510024: Handle rhel-alt product tags properly
			rhelTag = "rhel-"+clienttasks.redhatReleaseX+"|"+"rhel-alt-"+clienttasks.redhatReleaseX;
		}
		List<ProductCert> productCertsProvidingRhelTag = clienttasks.getCurrentProductCerts(rhelTag);
		productCertsProvidingRhelTag = clienttasks.filterTrumpedDefaultProductCerts(productCertsProvidingRhelTag);
		if (productCertsProvidingRhelTag.size()>1) {	// Bug 1506271 - redhat-release is providing more than 1 variant specific product cert
			log.warning("Installed product cert providing rhel tag '"+rhelTag+"': "+productCertsProvidingRhelTag);
			Assert.assertEquals(result.getStdout().trim(), "", "stdout from release --list when more than one product cert with tag '"+rhelTag+"' is installed.");
			String expectedStderr = "Error: More than one release product certificate installed.";
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.20.8-1")) {	// commit 286b4fcdc8dba6e75a7b50e5db912a99c8f7ada2	// bug 1464571: 'sub-man release' prints error for more prod. certs. 
				Assert.assertTrue(result.getStderr().trim().startsWith(expectedStderr), "stderr from release --list when more than one product cert with tag '"+rhelTag+"' is installed should be '"+expectedStderr+"'.");
			} else {
				Assert.assertEquals(result.getStderr().trim(), expectedStderr, "stderr from release --list when more than one product cert with tag '"+rhelTag+"' is installed should be '"+expectedStderr+"'.");
			}
		} else {
			Assert.assertEquals(result.getStdout().trim(), "", "stdout from release --list without any entitlements");
			Assert.assertEquals(result.getStderr().trim(), "No release versions available, please check subscriptions.", "stderr from release --list without any entitlements");
		}
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "exitCode from release --list without any entitlements");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-19999", "RHEL7-51032"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="attempt to set the subscription-manager release value that is not currently available",
			groups={"Tier1Tests","blockedByBug-818205", "blockedByBug-919700"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testSetAnUnavailableReleaseValue() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, null, null, null, null);		
		
		// assert feedback from release --list 
		String unavailableRelease = "Foo_1.0";
		SSHCommandResult result = clienttasks.release_(null, null, unavailableRelease, null, null, null, null, null);
		Integer expectedExitCode = new Integer(255);
		String rhelTag = "rhel-"+clienttasks.redhatReleaseX;
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.20.10-1")) {	// commit 76273403e6e3f7fa9811ef252a5a7ce4d84f7aa3	// bug 1510024: Handle rhel-alt product tags properly
			rhelTag = "rhel-"+clienttasks.redhatReleaseX+"|"+"rhel-alt-"+clienttasks.redhatReleaseX;
		}
		List<ProductCert> productCertsProvidingRhelTag = clienttasks.getCurrentProductCerts(rhelTag);
		productCertsProvidingRhelTag = clienttasks.filterTrumpedDefaultProductCerts(productCertsProvidingRhelTag);
		if (productCertsProvidingRhelTag.size()>1) {	// Bug 1506271 - redhat-release is providing more than 1 variant specific product cert
			log.warning("Installed product cert providing rhel tag '"+rhelTag+"': "+productCertsProvidingRhelTag);
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(78);	// EX_CONFIG	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getStdout().trim(), "", "stdout from --set with an unavailable value when more than one product cert with tag '"+rhelTag+"' is installed.");
			String expectedStderr = "Error: More than one release product certificate installed.";
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.20.8-1")) {	// commit 286b4fcdc8dba6e75a7b50e5db912a99c8f7ada2	// bug 1464571: 'sub-man release' prints error for more prod. certs. 
				Assert.assertTrue(result.getStderr().trim().startsWith(expectedStderr), "stderr from release --set with an unavailable value when more than one product cert with tag '"+rhelTag+"' is installed should be '"+expectedStderr+"'.");
			} else {
				Assert.assertEquals(result.getStderr().trim(), expectedStderr, "stderr from release --set with an unavailable value when more than one product cert with tag '"+rhelTag+"' is installed should be '"+expectedStderr+"'.");
			}
		} else {
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(65);	// EX_DATAERR	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getStdout().trim(), "", "stdout from release --set with an unavailable value");
			Assert.assertEquals(result.getStderr().trim(), String.format("No releases match '%s'.  Consult 'release --list' for a full listing.", unavailableRelease), "stderr from release --set with an unavailable value");
		}
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "exitCode from release --set with an unavailable value");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20001", "RHEL7-55165"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify that the consumer's current subscription-manager release value matches the release value just set",
			groups={"Tier1Tests","blockedByBug-814385", "blockedByBug-919700"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testGetTheReleaseAfterSettingTheRelease() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, null, null, null, null);		
		
		// are any releases available?
		List<String> availableReleases = clienttasks.getCurrentlyAvailableReleases(null, null, null, null);
		if (availableReleases.isEmpty()) throw new SkipException("When no releases are available, this test must be skipped.");
		
		// randomly pick an available release and set it
		String release = availableReleases.get(randomGenerator.nextInt(availableReleases.size()));
		clienttasks.release(null, null, release, null, null, null, null, null);
		Assert.assertEquals(clienttasks.getCurrentRelease(), release, "The release value retrieved after setting the release.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20000", "RHEL7-55164"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="assert that the subscription-manager release can nolonger be unset by setting it to \"\".",
			groups={"Tier1Tests","blockedByBug-807822","blockedByBug-814385", "blockedByBug-919700"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsetTheReleaseWithAnEmptyString() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, null, null, null, null);		
		
		// are any releases available?
		List<String> availableReleases = clienttasks.getCurrentlyAvailableReleases(null, null, null, null);
		if (availableReleases.isEmpty()) throw new SkipException("When no releases are available, this test must be skipped.");
		
		// randomly pick an available release and set it
		String release = availableReleases.get(randomGenerator.nextInt(availableReleases.size()));
		clienttasks.release(null, null, release, null, null, null, null, null);
		
		// attempt to unset by setting an empty string...
		SSHCommandResult result = clienttasks.release_(null, null, "", null, null, null, null, null);
		Integer expectedExitCode = new Integer(255);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) expectedExitCode = new Integer(65);	// EX_DATAERR	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
		Assert.assertEquals(result.getExitCode(), expectedExitCode, "ExitCode when attempting to unset the release with \"\".");
		Assert.assertEquals(result.getStderr().trim(), "No releases match ''.  Consult 'release --list' for a full listing.", "Stderr when attempting to unset the release with \"\".");
		Assert.assertEquals(result.getStdout(), "", "Stdout when attempting to unset the release with \"\".");
		Assert.assertEquals(clienttasks.getCurrentRelease(), release, "The release value should still be set after a failed attempt to unset it with \"\".");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20003", "RHEL7-55167"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="assert that the subscription-manager release can be unset by using the unset option.",
			groups={"Tier1Tests","blockedByBug-807822","blockedByBug-814385", "blockedByBug-919700"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testUnsetTheRelease() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, null, null, null, null);		
		
		// are any releases available?
		List<String> availableReleases = clienttasks.getCurrentlyAvailableReleases(null, null, null, null);
		if (availableReleases.isEmpty()) throw new SkipException("When no releases are available, this test must be skipped.");
		
		// randomly pick an available release and set it
		String release = availableReleases.get(randomGenerator.nextInt(availableReleases.size()));
		clienttasks.release(null, null, release, null, null, null, null, null);
		
		// unset by using the unset option...
		SSHCommandResult result = clienttasks.release(null, null, null, true, null, null, null, null);
		Assert.assertEquals(clienttasks.getCurrentRelease(), "", "The release value retrieved after unsetting should be not set.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20002", "RHEL7-55166"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="assert that subscription-manager release without any options defaults to --show",
			groups={"Tier1Tests","blockedByBug-812153", "blockedByBug-919700"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReleaseShow() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, null, null, null, null);		
		
		// are any releases available?
		List<String> availableReleases = clienttasks.getCurrentlyAvailableReleases(null, null, null, null);
		//if (availableReleases.isEmpty()) availableReleases.add("");	// fake an empty release
		if (availableReleases.isEmpty()) throw new SkipException("When no releases are available, this test must be skipped.");
		
		// randomly pick an available release
		String release = availableReleases.get(randomGenerator.nextInt(availableReleases.size()));
		
		// verify feedback from the release --show results match the default release results (after setting an available release)
		clienttasks.release(null, null, release, null, null, null, null, null);
		SSHCommandResult defaultResult = clienttasks.release(null, null, null, null, null, null, null, null);
		SSHCommandResult showResult = clienttasks.release(true, null, null, null, null, null, null, null);
		Assert.assertEquals(defaultResult.getStdout(), showResult.getStdout(), "stdout feedback comparison between release --show and release without options.");
		Assert.assertEquals(defaultResult.getStderr(), showResult.getStderr(), "stderr feedback comparison between release --show and release without options.");
		Assert.assertEquals(defaultResult.getExitCode(), showResult.getExitCode(), "exitCode feedback comparison between release --show and release without options.");

		// verify feedback from the release --show results match the default release results (after unsetting the release)
		clienttasks.release(null, null, null, true, null, null, null, null);
		defaultResult = clienttasks.release(null, null, null, null, null, null, null, null);
		showResult = clienttasks.release(true, null, null, null, null, null, null, null);
		Assert.assertEquals(defaultResult.getStdout(), showResult.getStdout(), "stdout feedback comparison between release --show and release without options.");
		Assert.assertEquals(defaultResult.getStderr(), showResult.getStderr(), "stderr feedback comparison between release --show and release without options.");
		Assert.assertEquals(defaultResult.getExitCode(), showResult.getExitCode(), "exitCode feedback comparison between release --show and release without options.");

	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20010", "RHEL7-55175"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="after subscribing to all available subscriptions, assert that content with url paths that reference $releasever are substituted with the consumers current release preference",
			groups={"Tier1Tests","blockedByBug-807407","blockedByBug-962520"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReleaseverSubstitutionInRepoLists() throws JSONException, Exception {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true, null, null, null, null, null);

		// subscribe to all available subscriptions
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively();
		
		// get current list of Repos and YumRepos before setting a release version preference
		List<Repo> reposBeforeSettingReleaseVer = clienttasks.getCurrentlySubscribedRepos();
		List<YumRepo> yumReposBeforeSettingReleaseVer = clienttasks.getCurrentlySubscribedYumRepos();
		
		boolean skipTest = true;
		for (Repo repo : reposBeforeSettingReleaseVer) {
			if (repo.repoUrl.contains("$releasever")) {
				skipTest = false; break;
			}
		}
		if (skipTest) throw new SkipException("After subscribing to all available subscriptions, could not find any enabled content with a repoUrl that employs $releasever");

		Assert.assertEquals(reposBeforeSettingReleaseVer.size(), yumReposBeforeSettingReleaseVer.size(), "The subscription-manager repos list count should match the yum reposlist count.");
		
		// now let's set a release version preference
		String releaseVer = "TestRelease-1.0";	// cannot do this anymore, the value must be valid
		List<String> availableReleaseVers = clienttasks.getCurrentlyAvailableReleases(null, null, null, null);
		if (availableReleaseVers.isEmpty()) throw new SkipException("Cannot complete this test when there are no releases available to set.");
		releaseVer = availableReleaseVers.get(randomGenerator.nextInt(availableReleaseVers.size()));	// randomly pick an available release for testing
		clienttasks.release(null, null, releaseVer, null, null, null, null, null);

		// assert that each of the Repos after setting a release version preference substitutes the $releasever
		for (Repo repoAfter : clienttasks.getCurrentlySubscribedRepos()) {
			Repo repoBefore = Repo.findFirstInstanceWithMatchingFieldFromList("repoName", repoAfter.repoName, reposBeforeSettingReleaseVer);
			Assert.assertNotNull(repoBefore,"Found the the same repoName from the subscription-manager repos --list after setting a release version preference.");
			
			if (!repoBefore.repoUrl.contains("$releasever")) {
				Assert.assertEquals(repoAfter.repoUrl, repoBefore.repoUrl,
						"After setting a release version preference, the subscription-manager repos --list reported repoUrl for '"+repoAfter.repoName+"' should remain unchanged since it did not contain the yum $releasever variable.");
			} else {
				Assert.assertEquals(repoAfter.repoUrl, repoBefore.repoUrl.replaceAll("\\$releasever", releaseVer),
						"After setting a release version preference, the subscription-manager repos --list reported repoUrl for '"+repoAfter.repoName+"' should have a variable substitution for $releasever.");
			}
		}
		
		
		// assert that each of the YumRepos after setting a release version preference actually substitutes the $releasever
		for (YumRepo yumRepoAfter : clienttasks.getCurrentlySubscribedYumRepos()) {
			YumRepo yumRepoBefore = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", yumRepoAfter.id, yumReposBeforeSettingReleaseVer);
			Assert.assertNotNull(yumRepoBefore,"Found the the same repo id from the yum repolist after setting a release version preference.");
			
			if (!yumRepoBefore.baseurl.contains("$releasever")) {
				Assert.assertEquals(yumRepoAfter.baseurl, yumRepoBefore.baseurl,
						"After setting a release version preference, the yum repolist reported baseurl for '"+yumRepoAfter.id+"' should remain unchanged since it did not contain the yum $releasever variable.");
			} else {
				Assert.assertEquals(yumRepoAfter.baseurl, yumRepoBefore.baseurl.replaceAll("\\$releasever", releaseVer),
						"After setting a release version preference, the yum repolist reported baseurl for '"+yumRepoAfter.id+"' should have a variable substitution for $releasever.");
			}
		}
		
		// now let's unset the release version preference
		clienttasks.release(null, null, null, true, null, null, null, null);

		// assert that each of the Repos and YumRepos after unsetting the release version preference where restore to their original values (containing $releasever)
		List<Repo> reposAfterSettingReleaseVer = clienttasks.getCurrentlySubscribedRepos();
		List<YumRepo> yumReposAfterSettingReleaseVer = clienttasks.getCurrentlySubscribedYumRepos();
		
		Assert.assertTrue(reposAfterSettingReleaseVer.containsAll(reposBeforeSettingReleaseVer) && reposBeforeSettingReleaseVer.containsAll(reposAfterSettingReleaseVer),
				"After unsetting the release version preference, all of the subscription-manager repos --list were restored to their original values.");
		Assert.assertTrue(yumReposAfterSettingReleaseVer.containsAll(yumReposBeforeSettingReleaseVer) && yumReposBeforeSettingReleaseVer.containsAll(yumReposAfterSettingReleaseVer),
				"After unsetting the release version preference, all of the yum repolist were restored to their original values.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20009", "RHEL7-55174"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="register to a RHEL subscription and verify that release --list matches the expected CDN listing for this x-stream release of RHEL",
			groups={"Tier1Tests","blockedByBug-818298","blockedByBug-820639","blockedByBug-844368","blockedByBug-893746","blockedByBug-904193","blockedByBug-1506271"},
			dataProvider="getCredentialsToVerifyReleaseListMatchesCDN_Test",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReleaseListMatchesCDN(Object bugzilla, String username, String password, String org) throws JSONException, Exception {

		// make sure we are newly registered
		clienttasks.register(username,password,org,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true, null, null, null, null, null);
		
		// get the current base RHEL product cert
		ProductCert rhelProductCert = clienttasks.getCurrentRhelProductCert();
		
		// find an available RHEL subscription pool that provides for this base RHEL product cert
		//List<SubscriptionPool> rhelSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools(rhelProductCert.productId, sm_serverUrl);	// no longer works; encounters "Insufficient permissions"
		List<SubscriptionPool> rhelSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools(rhelProductCert.productId,/*sm_serverAdminUsername*/username,/*sm_serverAdminPassword*/password, sm_serverUrl);
		if (rhelSubscriptionPools.isEmpty()) throw new SkipException("Cannot find an available SubscriptionPool that provides for this installed RHEL Product: "+rhelProductCert);
		SubscriptionPool rhelSubscriptionPool = rhelSubscriptionPools.get(0);	// choose one
		
		// subscribe to the RHEL subscription
		clienttasks.subscribeToSubscriptionPool(rhelSubscriptionPool);
		
		// get the currently expected release listing based on the currently enabled repos
		List<String> expectedReleases = clienttasks.getCurrentlyExpectedReleases();
		
		// get the actual release listing
		List<String> actualReleases = clienttasks.getCurrentlyAvailableReleases(null,null,null, null);
		
		// TEMPORARY WORKAROUND FOR BUG
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1108257"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			if (rhelProductCert.productNamespace.providedTags.contains("rhel-5-client-workstation")) {
				throw new SkipException("Skipping this test while bug '"+bugId+"' is open. (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");			
			}
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND FOR BUG
		if (actualReleases.isEmpty() && !expectedReleases.isEmpty()) {
			invokeWorkaroundWhileBugIsOpen = true;
			bugId="1518886"; // Bug 1518886 - RHEL-ALT-7.5 product certs should also provide tag "rhel-7"
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				if (Arrays.asList(new String[]{ // this is a RHEL-ALT system
						"419",	/* Red Hat Enterprise Linux for ARM 64 */
						"420",	/* Red Hat Enterprise Linux for Power 9 */
						"434",	/* Red Hat Enterprise Linux for IBM System z (Structure A) */
						"363",	/* Red Hat Enterprise Linux for ARM 64 Beta */
						"362",	/* Red Hat Enterprise Linux for Power 9 Beta */
						"433",	/* Red Hat Enterprise Linux for IBM System z (Structure A) Beta */
						}).contains(rhelProductCert.productId)) {
				throw new SkipException("subscription-manager release listings on RHEL-ALT will be empty until bug '"+bugId+"' is fixed.");
				}
			}
		}
		// END OF WORKAROUND
		
		// assert that they are equivalent
		Assert.assertTrue(expectedReleases.containsAll(actualReleases) && actualReleases.containsAll(expectedReleases), "The actual subscription-manager releases list "+actualReleases+" matches the expected consolidated CDN listing "+expectedReleases+" after being granted an entitlement from subscription product: "+rhelSubscriptionPool.productId);
		Assert.assertTrue(expectedReleases.size()==actualReleases.size(), "The actual subscription-manager releases list "+actualReleases+" does not contain any duplicates.  It should be a unique list.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6},
			testCaseID= {"RHEL6-20004"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="register to a RHEL subscription and verify that release --list excludes 6.0",
			groups={"Tier1Tests","blockedByBug-802245"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReleaseListExcludes60OnRHEL6System() throws JSONException, Exception {
		if (!clienttasks.redhatReleaseX.equals("6")) throw new SkipException("This test is only applicable on RHEL6.");
		
		// make sure we are newly registered with autosubscribe
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, null, null, null, null);

		// get the current base RHEL product cert
		/* this block of logic does not account for the presence of a /etc/pki/product-default/ cert
		String providingTag = "rhel-"+clienttasks.redhatReleaseX;
		List<ProductCert> rhelProductCerts = clienttasks.getCurrentProductCerts(providingTag);
		Assert.assertEquals(rhelProductCerts.size(), 1, "Only one product cert is installed that provides RHEL tag '"+providingTag+"'");
		ProductCert rhelProductCert = rhelProductCerts.get(0);
		*/
		ProductCert rhelProductCert = clienttasks.getCurrentRhelProductCert();
		
		// assert that it was autosubscribed
		InstalledProduct rhelInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", rhelProductCert.productId, clienttasks.getCurrentlyInstalledProducts());
		Assert.assertNotNull(rhelInstalledProduct, "Our base installed RHEL product was autosubscribed during registration.");

		// get the actual release listing
		List<String> actualReleases = clienttasks.getCurrentlyAvailableReleases(null, null, null, null);
		
		// assert that the list excludes 6.0, but includes the current X.Y release
		String release60 = "6.0";
		Assert.assertTrue(!actualReleases.contains(release60), "The subscription-manager releases list should exclude '"+release60+"' since '"+clienttasks.command+"' did not exist in RHEL Release '"+release60+"'.");
		//NOT PRACTICAL SINCE CONTENT FROM THIS Y-STREAM MAY NOT BE AVAILABLE UNTIL GA Assert.assertTrue(actualReleases.contains(clienttasks.redhatReleaseXY), "The subscription-manager releases list should include '"+clienttasks.redhatReleaseXY+"' since it is the current RHEL Release under test.");
	}


	@TestDefinition(projectID={/*Project.RHEL5*/},testCaseID={})
	@Test(	description="register to a RHEL subscription and verify that release --list excludes 5.6, 5.5, 5.4, 5.3, 5.2, 5.1, 5.0",
			groups={"Tier1Tests","blockedByBug-785989"/*,"blockedByBug-840509" MOVED TO TEMPORARY WORKAROUND*/,"blockedByBug-919700"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReleaseListExcludes56OnRHEL5System() throws JSONException, Exception {
		if (!clienttasks.redhatReleaseX.equals("5")) throw new SkipException("This test is only applicable on RHEL5.");
		
		// TEMPORARY WORKAROUND
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="840509"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			throw new SkipException("blockedByBug-840509");
		}
		// END OF WORKAROUND
		
		// make sure we are newly registered with autosubscribe
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, null, null, null, null);

		// get the current base RHEL product cert
		String providingTag = "rhel-"+clienttasks.redhatReleaseX;
		List<ProductCert> rhelProductCerts = clienttasks.getCurrentProductCerts(providingTag);
		// EXCEPTION: On RHEL5, could be either Red Hat Enterprise Linux Desktop (68) Tags: rhel-5,rhel-5-client OR Red Hat Enterprise Linux Workstation (71) Tags: rhel-5-client-workstation,rhel-5-workstation
		// TODO: Don't know how to predict which one since I believe Workstation is really born after consuming a child channel of Client rather than a base channel; for now let's just assume the other
		if (clienttasks.releasever.equals("5Client") && rhelProductCerts.isEmpty()) {
			providingTag += "-workstation";
			rhelProductCerts = clienttasks.getCurrentProductCerts(providingTag);
		}
		Assert.assertEquals(rhelProductCerts.size(), 1, "Only one product cert is installed that provides RHEL tag '"+providingTag+"'");
		ProductCert rhelProductCert = rhelProductCerts.get(0);
		
		// assert that it was autosubscribed
		InstalledProduct rhelInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", rhelProductCert.productId, clienttasks.getCurrentlyInstalledProducts());
		Assert.assertNotNull(rhelInstalledProduct, "Our base installed RHEL product was autosubscribed during registration.");

		// get the actual release listing
		List<String> actualReleases = clienttasks.getCurrentlyAvailableReleases(null, null, null, null);
		
		// assert that the list excludes 5.6, 5.5, 5.4, 5.3, 5.2, 5.1, 5.0, but includes the current X.Y release
		for (String release: new String[]{"5.6","5.5","5.4","5.3","5.2","5.1","5.0"}) {
			Assert.assertTrue(!actualReleases.contains(release), "The subscription-manager releases list should exclude '"+release+"' since '"+clienttasks.command+"' did not exist in RHEL Release '"+release+"'.");
		}
		//NOT PRACTICAL SINCE CONTENT FROM THIS Y-STREAM MAY NOT BE AVAILABLE UNTIL GA Assert.assertTrue(actualReleases.contains(clienttasks.redhatReleaseXY), "The subscription-manager releases list should include '"+clienttasks.redhatReleaseXY+"' since it is the current RHEL Release under test.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20007", "RHEL7-55172"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="using a no auth proxy server, register to a RHEL subscription and verify that release --list matches the expected CDN listing for this x-stream release of RHEL",
			groups={"Tier1Tests","blockedByBug-844368","blockedByBug-893746","blockedByBug-904193","blockedByBug-1134963","blockedByBug-1400719","blockedByBug-1438552"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReleaseListMatchesCDNUsingNoAuthProxyCommandLineArgs() throws JSONException, Exception {
		verifyReleaseListMatchesCDN(sm_noauthproxyHostname,sm_noauthproxyPort,null,null);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20005", "RHEL7-55170"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="using a basic auth proxy server, register to a RHEL subscription and verify that release --list matches the expected CDN listing for this x-stream release of RHEL",
			groups={"Tier1Tests","blockedByBug-844368","blockedByBug-893746","blockedByBug-904193","blockedByBug-1134963","blockedByBug-1400719","blockedByBug-1438552"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReleaseListMatchesCDNUsingBasicAuthProxyCommandLineArgs() {
		verifyReleaseListMatchesCDN(sm_basicauthproxyHostname,sm_basicauthproxyPort,sm_basicauthproxyUsername,sm_basicauthproxyPassword);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20008", "RHEL7-55173"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="using a no auth proxy server set within rhsm.conf, register to a RHEL subscription and verify that release --list matches the expected CDN listing for this x-stream release of RHEL",
			groups={"Tier1Tests","blockedByBug-822965","blockedByBug-844368","blockedByBug-893746","blockedByBug-904193","blockedByBug-1400719","blockedByBug-1438552"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReleaseListMatchesCDNUsingNoAuthProxyViaRhsmConfFile() throws JSONException, Exception {
		clienttasks.config(false, false, true, Arrays.asList(
				new String[]{"server","proxy_hostname",sm_noauthproxyHostname},
				new String[]{"server","proxy_port",sm_noauthproxyPort}));
		verifyReleaseListMatchesCDN(null,null,null,null);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20006", "RHEL7-55171"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="using a basic auth proxy server set within rhsm.conf, register to a RHEL subscription and verify that release --list matches the expected CDN listing for this x-stream release of RHEL",
			groups={"Tier1Tests","blockedByBug-822965","blockedByBug-844368","blockedByBug-893746","blockedByBug-904193","blockedByBug-1134963","blockedByBug-1400719","blockedByBug-1438552"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testReleaseListMatchesCDNUsingBasicAuthProxyViaRhsmConfFile() {
		clienttasks.config(false, false, true, Arrays.asList(
				new String[]{"server","proxy_hostname",sm_basicauthproxyHostname},
				new String[]{"server","proxy_port",sm_basicauthproxyPort},
				new String[]{"server","proxy_user",sm_basicauthproxyUsername},
				new String[]{"server","proxy_password",sm_basicauthproxyPassword}));
		verifyReleaseListMatchesCDN(null,null,null,null);
	}
	
	
	
	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-47938", "RHEL7-99713"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.NEGATIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: after using the release module to pin the content set repos paths, use yum repolist to bombard the IT-Candlepin server with GET requests to /subscription/consumers/{uuid}/release so as to generate a RateLimitExceededException",
			groups={"Tier1Tests","blockedByBug-1481384","blockedByBug-1486549"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRateLimitExceededExceptionShouldNotAlterRedhatRepo() throws JSONException, Exception {
		
		// register a new consumer and auto-subscribe to cover the installed RHEL product
		clienttasks.unregister(null, null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, null, false, null, null, null, null);
		ProductCert rhelProductCert=clienttasks.getCurrentRhelProductCert();
		if (rhelProductCert==null) throw new SkipException("This test requires an entitlement to an installed RHEL product");
		InstalledProduct installedRhelProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", rhelProductCert.productId, clienttasks.getCurrentlyInstalledProducts());
		if (installedRhelProduct==null) Assert.fail("Could not find the installed product corresponding to the current RHEL product cert: "+rhelProductCert);
		if (!installedRhelProduct.status.equals("Subscribed")) throw new SkipException("This test requires attachment to a RHEL subscription for the installed RHEL product.");
		
		// assert that there are some occurrences of $releasever in redhat.repo (for non Beta)
		String sshCommandGreppingRedhatRepoForNumberReleaseverOccurrences = "grep '$releasever' "+clienttasks.redhatRepoFile+" | wc --lines";
		Integer numberReleaseverOccurrences = Integer.valueOf(client.runCommandAndWait(sshCommandGreppingRedhatRepoForNumberReleaseverOccurrences).getStdout().trim());
		if (false) {	// NOT A VALID ASSERTION BECAUSE RCM HAS CHOSEN TO USE THE SAME TAGS ON BETA ENG IDS 362,363,433 AS THEIR GA COUNTERPARTS 420,419,434 WHICH MEANS THAT IF A SUBSCRIPTION THAT PROVIDES BOTH BETA AND GA ARE ATTACHED TO A SYSTEM WITH ONLY THE BETA PRODUCT CERT INSTALLED THEN ACCESS TO GA CONTENT WITH $releasever IS GRANTED.
		if (Arrays.asList("362","363","433").contains(installedRhelProduct.productId)) {
			Assert.assertEquals(numberReleaseverOccurrences, Integer.valueOf(0),"Because the currently installed RHEL engineering product '"+installedRhelProduct.productId+"' should only provide content access to beta|htb repositories, none of the current entitled repo urls should contain reference to $releasever.");
			throw new SkipException("This test requires a RHEL entitlement to an engineering product with content sets that can be pinned to a $releasever.");
		}
		}
		if (numberReleaseverOccurrences==0) throw new SkipException("This test requires a RHEL entitlement providing an engineering product with content sets that can be pinned to a $releasever.");
		// NOT A VALID ASSERTION Assert.assertTrue(numberReleaseverOccurrences>0, "The number of occurrences ("+numberReleaseverOccurrences+") for '$releasever' in '"+clienttasks.redhatRepoFile+"' is greater than zero.");
		
		// are any releases available?
		List<String> availableReleases = clienttasks.getCurrentlyAvailableReleases(null, null, null, null);
		if (availableReleases.isEmpty()) throw new SkipException("When no releases are available, this test must be skipped.");
		
		// set a release
		String release = availableReleases.get(0); // assume the first release is good enough for this test	
		if (availableReleases.contains("7.3")) release = "7.3";
		clienttasks.release(null, null, release, null, null, null, null, null);
		
		// assert that no occurrences of $releasever are in redhat.repo
		Assert.assertEquals(client.runCommandAndWait(sshCommandGreppingRedhatRepoForNumberReleaseverOccurrences).getStdout().trim(), "0", "Number of occurances for \"$releasever\" in '"+clienttasks.redhatRepoFile+"' after setting the release to '"+release+"'.");
		
		// remember the number of available packages
		Integer numPackagesAvailableBeforeExceedingRateLimit = clienttasks.getYumListAvailable("--disablerepo=beaker*").size();
		
		// now bombard the server with more than 60 hits to encounter a RateLimitExceededException
		client.runCommandAndWait("for i in {1..60}; do yum repolist --disablerepo=beaker* --quiet; done;");
		String rhsmLogMarker = System.currentTimeMillis()+" testRateLimitExceededExceptionShouldNotAlterRedhatRepo...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, rhsmLogMarker);
		Integer numPackagesAvailableAfterExceedingRateLimit = clienttasks.getYumListAvailable("--disablerepo=beaker*").size();
		
		// assert that there are still no occurrences of $releasever in redhat.repo
		Assert.assertEquals(client.runCommandAndWait(sshCommandGreppingRedhatRepoForNumberReleaseverOccurrences).getStdout().trim(), "0", "Number of occurances for \"$releasever\" in '"+clienttasks.redhatRepoFile+"' after setting the release to '"+release+"' and then bombarding the server via the subscription-manager yum plugin via system invocations of yum repolist.");
		
		// assert that there is an ERROR in the rhsm.log for the RateLimitExceededException
		String rhsmLogStatement = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, rhsmLogMarker, "ERROR").trim();
		// 2017-09-01 16:16:47,199 [ERROR] yum:16448:MainThread @cache.py:235 - Access rate limit exceeded
		if (!rhsmLogStatement.isEmpty()) log.warning(rhsmLogStatement);
		String expectedErrorMsg = "Access rate limit exceeded";		// https://github.com/candlepin/subscription-manager/pull/1694	// commit abca9b07c0cbc852d015dc9316927f8e39d1ba0d 1481384: Do not update redhat.repo at RateLimitExceededException 
		Assert.assertTrue(rhsmLogStatement.contains(expectedErrorMsg),"After bombarding the server to purposefully invoke a RateLimitExceededException, the '"+clienttasks.rhsmLogFile+"' reports expected ERROR '"+expectedErrorMsg+"'.");
		
		// assert that the number of available packages remains the same
		Assert.assertEquals(numPackagesAvailableAfterExceedingRateLimit, numPackagesAvailableBeforeExceedingRateLimit, "The number of yum available packages after exceeding the rate limit ("+numPackagesAvailableAfterExceedingRateLimit+") matches the number before exceeding the rate limit ("+numPackagesAvailableBeforeExceedingRateLimit+").");
	}
	
	
	
	
	
	
	
	

	// Candidates for an automated Test:
	// TODO Bug 861151 - subscription-manager release doesn't take variant into account (SORT OF DONE ALREADY) https://github.com/RedHatQE/rhsm-qe/issues/187
	// TODO	Bug 829111 - Release list command doesn't compare platform between content requried tag and product provided tag (REALLY A DUP OF 861151) https://github.com/RedHatQE/rhsm-qe/issues/187
	
	// Configuration methods ***********************************************************************
	@BeforeClass (groups="setup")
	public void rememberServerProxyConfigs() {
		if (clienttasks==null) return;
		
		serverProxyHostname	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "proxy_hostname");
		serverProxyPort		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "proxy_port");
		serverProxyUser		= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "proxy_user");
		serverProxyPassword	= clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "server", "proxy_password");
	}
	
	@BeforeMethod (groups="setup")
	@AfterClass (groups="setup")
	public void restoreServerProxyConfigs() {
		if (clienttasks==null) return;
		
		clienttasks.config(false, false, true, Arrays.asList(
				new String[]{"server","proxy_hostname",serverProxyHostname},
				new String[]{"server","proxy_port",serverProxyPort},
				new String[]{"server","proxy_user",serverProxyUser},
				new String[]{"server","proxy_password",serverProxyPassword}));
	}

	
	// Protected methods ***********************************************************************
	String serverProxyHostname	= null;
	String serverProxyPort		= null;
	String serverProxyUser		= null;
	String serverProxyPassword	= null;

	
	protected void verifyReleaseListMatchesCDN(String proxy_hostname, String proxy_port, String proxy_username, String proxy_password) {
		
		// assemble the proxy from the proxy_hostname and proxy_port
		String proxy = proxy_hostname;
		if (proxy_hostname!=null && proxy_port!=null) proxy+=":"+proxy_port;
		
		// get the current base RHEL product cert
		ProductCert rhelProductCert = clienttasks.getCurrentRhelProductCert();
		Assert.assertNotNull(rhelProductCert, "Only one RHEL product cert is installed.");
		
		// make sure we are newly registered
		SSHCommandResult registerResult = clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, proxy, proxy_username, proxy_password, null);

		// assert that we are subscribed to RHEL
		//if (!InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", rhelProductCert.productId, clienttasks.getCurrentlyInstalledProducts()).status.equals("Subscribed")) {
		if (!InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productName", rhelProductCert.productName, InstalledProduct.parse(registerResult.getStdout())).status.equals("Subscribed")) {
			clienttasks.listAvailableSubscriptionPools();
			throw new SkipException("Autosubscribe could not find an available subscription that provides RHEL content for installed RHEL product:"+rhelProductCert.productNamespace);
		}
		// ^^ that is faster, but the following is more reliable...
		/*
		// find an available RHEL subscription pool that provides for this base RHEL product cert
		List<SubscriptionPool> rhelSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools(rhelProductCert.productId, sm_serverUrl);
		if (rhelSubscriptionPools.isEmpty()) throw new SkipException("Cannot find an available SubscriptionPool that provides for this installed RHEL Product: "+rhelProductCert);
		SubscriptionPool rhelSubscriptionPool = rhelSubscriptionPools.get(0);	// choose one
		
		// subscribe to the RHEL subscription
		File rhelEntitlementCertFile = clienttasks.subscribeToSubscriptionPool(rhelSubscriptionPool);
		File rhelEntitlementCertKeyFile = clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(rhelEntitlementCertFile);
		*/
		
		// get the currently expected release listing based on the currently enabled repos
		List<String> expectedReleases = clienttasks.getCurrentlyExpectedReleases();
		
		// get the actual release listing
		List<String> actualReleases = clienttasks.getCurrentlyAvailableReleases(proxy, proxy_username, proxy_password, null);
		
		// TEMPORARY WORKAROUND FOR BUG
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1108257"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen) {
			if (rhelProductCert.productNamespace.providedTags.contains("rhel-5-client-workstation")) {
				throw new SkipException("Skipping this test while bug '"+bugId+"' is open. (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");			
			}
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND FOR BUG
		if (actualReleases.isEmpty() && !expectedReleases.isEmpty()) {
			invokeWorkaroundWhileBugIsOpen = true;
			bugId="1518886"; // Bug 1518886 - RHEL-ALT-7.5 product certs should also provide tag "rhel-7"
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				if (Arrays.asList(new String[]{ // this is a RHEL-ALT system
						"419",	/* Red Hat Enterprise Linux for ARM 64 */
						"420",	/* Red Hat Enterprise Linux for Power 9 */
						"434",	/* Red Hat Enterprise Linux for IBM System z (Structure A) */
						"363",	/* Red Hat Enterprise Linux for ARM 64 Beta */
						"362",	/* Red Hat Enterprise Linux for Power 9 Beta */
						"433",	/* Red Hat Enterprise Linux for IBM System z (Structure A) Beta */
						}).contains(rhelProductCert.productId)) {
				throw new SkipException("subscription-manager release listings on RHEL-ALT will be empty until bug '"+bugId+"' is fixed.");
				}
			}
		}
		// END OF WORKAROUND
		
		// assert that they are equivalent
		Assert.assertTrue(expectedReleases.containsAll(actualReleases) && actualReleases.containsAll(expectedReleases), "The actual subscription-manager releases list "+actualReleases+" matches the expected consolidated CDN listing "+expectedReleases+" after successfully autosubscribing to installed RHEL product: "+rhelProductCert.productName);
	}
	
	
	
	
	// Data Providers ***********************************************************************
	
	@DataProvider(name="getCredentialsToVerifyReleaseListMatchesCDN_Test")
	public Object[][] getCredentialsToVerifyReleaseListMatchesCDN_TestDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getCredentialsToVerifyReleaseListMatchesCDN_TestDataAsListOfLists());
	}
	protected List<List<Object>> getCredentialsToVerifyReleaseListMatchesCDN_TestDataAsListOfLists(){
		List<List<Object>> ll = new ArrayList<List<Object>>();

		// Object bugzilla, String username, String password, String org
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"1134963"}),	sm_clientUsername, sm_clientPassword, sm_clientOrg}));

		// add another row only if the RHN credentials are valid against sm_serverHostname...
		//if (sm_serverType.equals(CandlepinType.hosted) && !sm_rhnUsername.isEmpty())
		if (doesStringContainMatches(sm_rhnHostname, "rhn\\.(.+\\.)*redhat\\.com") && doesStringContainMatches(sm_serverHostname, "subscription\\.rhn\\.(.+\\.)*redhat\\.com") && !sm_rhnUsername.isEmpty())
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"861151","870141","1134963"}),	sm_rhnUsername, sm_rhnPassword, null}));

		return ll;
	}
}
