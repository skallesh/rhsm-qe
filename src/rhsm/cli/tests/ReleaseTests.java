package rhsm.cli.tests;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BlockedByBzBug;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.auto.testng.TestNGUtils;
import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.Repo;
import rhsm.data.SubscriptionPool;
import rhsm.data.YumRepo;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 * 
 * References:
 * https://engineering.redhat.com/trac/Entitlement/wiki/ReleaseVer
 * 
 * Note: To see all of the yum vars values, run this:
[root@jsefler-7 ~]#  python -c 'import yum, pprint; yb = yum.YumBase(); pprint.pprint(yb.conf.yumvar, width=1)'
Loaded plugins: product-id
{'arch': 'amd64',
 'basearch': 'x86_64',
 'releasever': '6.92Server',
 'uuid': '2c985181-c84e-43bf-a913-b240973feead'}

 */

@Test(groups={"ReleaseTests","AcceptanceTests","Tier1Tests"})
public class ReleaseTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************
	
	@Test(	description="attempt to get the subscription-manager release when not registered",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptToGetReleaseWhenNotRegistered_Test() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null);
		
		SSHCommandResult result;
		
		result = clienttasks.release_(null, null, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from calling release without being registered");
		Assert.assertEquals(result.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered, "Stdout from release without being registered");

		result = clienttasks.release_(null, true, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from calling release --list without being registered");
		Assert.assertEquals(result.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered, "Stdout from release without being registered");

		result = clienttasks.release_(null, null, "FOO", null, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from calling release --set without being registered");
		Assert.assertEquals(result.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered, "Stdout from release without being registered");
	}
	
	
	@Test(	description="attempt to get the subscription-manager release when a release has not been set; should be told that the release is not set",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptToGetReleaseWhenReleaseHasNotBeenSet_Test() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true, null, null, null, null);		
		SSHCommandResult result = clienttasks.release(null, null, null, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "Release not set", "Stdout from release without having set it");
	}
	
	
	@Test(	description="attempt to get the subscription-manager release list without having subscribed to any entitlements",
			groups={"blockedByBug-824979"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptToGetReleaseListWithoutHavingAnyEntitlements_Test() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true, null, null, null, null);
		
		// make sure we are not auto healing ourself with any entitlements
		clienttasks.autoheal(null,null,true,null,null,null);
		clienttasks.unsubscribe(true, (List<BigInteger>)null, null, null, null);
		
		// assert no releases are listed
		Assert.assertTrue(clienttasks.getCurrentlyAvailableReleases(null, null, null).isEmpty(), "No releases should be available without having been entitled to anything.");

		// assert feedback from release --list 
		SSHCommandResult result = clienttasks.release_(null,true,null,null,null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "", "stdout from release release --list without any entitlements");
		Assert.assertEquals(result.getStderr().trim(), "No release versions available, please check subscriptions.", "stderr from release --list without any entitlements");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "exitCode from release --list without any entitlements");

	}
	
	
	@Test(	description="attempt to set the subscription-manager release value that is not currently available",
			groups={"blockedByBug-818205", "blockedByBug-919700"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptToSetAnUnavailableReleaseValue_Test() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, null, null, null);		
		
		// assert feedback from release --list 
		String unavailableRelease = "Foo_1.0";
		SSHCommandResult result = clienttasks.release_(null, null, unavailableRelease, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "", "stdout from release release --set with an unavailable value");
		Assert.assertEquals(result.getStderr().trim(), String.format("No releases match '%s'.  Consult 'release --list' for a full listing.", unavailableRelease), "stderr from release --set with an unavailable value");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "exitCode from release --set with an unavailable value");
	}
	
	
	@Test(	description="verify that the consumer's current subscription-manager release value matches the release value just set",
			groups={"blockedByBug-814385", "blockedByBug-919700"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void GetTheReleaseAfterSettingTheRelease_Test() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, null, null, null);		
		
		// are any releases available?
		List<String> availableReleases = clienttasks.getCurrentlyAvailableReleases(null, null, null);
		if (availableReleases.isEmpty()) throw new SkipException("When no releases are available, this test must be skipped.");
		
		// randomly pick an available release and set it
		String release = availableReleases.get(randomGenerator.nextInt(availableReleases.size()));
		clienttasks.release(null, null, release, null, null, null, null);
		Assert.assertEquals(clienttasks.getCurrentRelease(), release, "The release value retrieved after setting the release.");
	}
	
	
	@Test(	description="assert that the subscription-manager release can nolonger be unset by setting it to \"\".",
			groups={"blockedByBug-807822","blockedByBug-814385", "blockedByBug-919700"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptToUnsetTheReleaseWithAnEmptyString_Test() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, null, null, null);		
		
		// are any releases available?
		List<String> availableReleases = clienttasks.getCurrentlyAvailableReleases(null, null, null);
		if (availableReleases.isEmpty()) throw new SkipException("When no releases are available, this test must be skipped.");
		
		// randomly pick an available release and set it
		String release = availableReleases.get(randomGenerator.nextInt(availableReleases.size()));
		clienttasks.release(null, null, release, null, null, null, null);
		
		// attempt to unset by setting an empty string...
		SSHCommandResult result = clienttasks.release_(null, null, "", null, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode when attempting to unset the release with \"\".");
		Assert.assertEquals(result.getStderr().trim(), "No releases match ''.  Consult 'release --list' for a full listing.", "Stderr when attempting to unset the release with \"\".");
		Assert.assertEquals(result.getStdout(), "", "Stdout when attempting to unset the release with \"\".");
		Assert.assertEquals(clienttasks.getCurrentRelease(), release, "The release value should still be set after a failed attempt to unset it with \"\".");
	}
	
	
	@Test(	description="assert that the subscription-manager release can be unset by using the unset option.",
			groups={"blockedByBug-807822","blockedByBug-814385", "blockedByBug-919700"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void UnsetTheRelease_Test() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, null, null, null);		
		
		// are any releases available?
		List<String> availableReleases = clienttasks.getCurrentlyAvailableReleases(null, null, null);
		if (availableReleases.isEmpty()) throw new SkipException("When no releases are available, this test must be skipped.");
		
		// randomly pick an available release and set it
		String release = availableReleases.get(randomGenerator.nextInt(availableReleases.size()));
		clienttasks.release(null, null, release, null, null, null, null);
		
		// unset by using the unset option...
		SSHCommandResult result = clienttasks.release(null, null, null, true, null, null, null);
		Assert.assertEquals(clienttasks.getCurrentRelease(), "", "The release value retrieved after unsetting should be not set.");
	}
	
	
	@Test(	description="assert that subscription-manager release without any options defaults to --show",
			groups={"blockedByBug-812153", "blockedByBug-919700"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ReleaseShow_Test() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, null, null, null);		
		
		// are any releases available?
		List<String> availableReleases = clienttasks.getCurrentlyAvailableReleases(null, null, null);
		//if (availableReleases.isEmpty()) availableReleases.add("");	// fake an empty release
		if (availableReleases.isEmpty()) throw new SkipException("When no releases are available, this test must be skipped.");
		
		// randomly pick an available release
		String release = availableReleases.get(randomGenerator.nextInt(availableReleases.size()));
		
		// verify feedback from the release --show results match the default release results (after setting an available release)
		clienttasks.release(null, null, release, null, null, null, null);
		SSHCommandResult defaultResult = clienttasks.release(null, null, null, null, null, null, null);
		SSHCommandResult showResult = clienttasks.release(true, null, null, null, null, null, null);
		Assert.assertEquals(defaultResult.getStdout(), showResult.getStdout(), "stdout feedback comparison between release --show and release without options.");
		Assert.assertEquals(defaultResult.getStderr(), showResult.getStderr(), "stderr feedback comparison between release --show and release without options.");
		Assert.assertEquals(defaultResult.getExitCode(), showResult.getExitCode(), "exitCode feedback comparison between release --show and release without options.");

		// verify feedback from the release --show results match the default release results (after unsetting the release)
		clienttasks.release(null, null, null, true, null, null, null);
		defaultResult = clienttasks.release(null, null, null, null, null, null, null);
		showResult = clienttasks.release(true, null, null, null, null, null, null);
		Assert.assertEquals(defaultResult.getStdout(), showResult.getStdout(), "stdout feedback comparison between release --show and release without options.");
		Assert.assertEquals(defaultResult.getStderr(), showResult.getStderr(), "stderr feedback comparison between release --show and release without options.");
		Assert.assertEquals(defaultResult.getExitCode(), showResult.getExitCode(), "exitCode feedback comparison between release --show and release without options.");

	}
	
	
	@Test(	description="after subscribing to all available subscriptions, assert that content with url paths that reference $releasever are substituted with the consumers current release preference",
			groups={"blockedByBug-807407","blockedByBug-962520"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyReleaseverSubstitutionInRepoLists_Test() throws JSONException, Exception {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true, null, null, null, null);

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
		List<String> availableReleaseVers = clienttasks.getCurrentlyAvailableReleases(null, null, null);
		if (availableReleaseVers.isEmpty()) throw new SkipException("Cannot complete this test when there are no releases available to set.");
		releaseVer = availableReleaseVers.get(randomGenerator.nextInt(availableReleaseVers.size()));	// randomly pick an available release for testing
		clienttasks.release(null, null, releaseVer, null, null, null, null);

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
		clienttasks.release(null, null, null, true, null, null, null);

		// assert that each of the Repos and YumRepos after unsetting the release version preference where restore to their original values (containing $releasever)
		List<Repo> reposAfterSettingReleaseVer = clienttasks.getCurrentlySubscribedRepos();
		List<YumRepo> yumReposAfterSettingReleaseVer = clienttasks.getCurrentlySubscribedYumRepos();
		
		Assert.assertTrue(reposAfterSettingReleaseVer.containsAll(reposBeforeSettingReleaseVer) && reposBeforeSettingReleaseVer.containsAll(reposAfterSettingReleaseVer),
				"After unsetting the release version preference, all of the subscription-manager repos --list were restored to their original values.");
		Assert.assertTrue(yumReposAfterSettingReleaseVer.containsAll(yumReposBeforeSettingReleaseVer) && yumReposBeforeSettingReleaseVer.containsAll(yumReposAfterSettingReleaseVer),
				"After unsetting the release version preference, all of the yum repolist were restored to their original values.");
	}
	
	
	@Test(	description="register to a RHEL subscription and verify that release --list matches the expected CDN listing for this x-stream release of RHEL",
			groups={"blockedByBug-818298","blockedByBug-820639","blockedByBug-844368","blockedByBug-893746"/*,"blockedByBug-904193" Uncomment when we get to RHEL7*/},
			dataProvider="getCredentialsToVerifyReleaseListMatchesCDN_Test",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyReleaseListMatchesCDN_Test(Object bugzilla, String username, String password, String org) throws JSONException, Exception {

		// make sure we are newly registered
		clienttasks.register(username,password,org,null,null,null,null,null,null,null,(List<String>)null,null,null,null,true, null, null, null, null);

		// get the current base RHEL product cert
		String providingTag = "rhel-"+clienttasks.redhatReleaseX;
		List<ProductCert> rhelProductCerts = clienttasks.getCurrentProductCerts(providingTag);
		// special case (rhel5ClientDesktopVersusWorkstation)
		if (rhelProductCerts.isEmpty() && clienttasks.releasever.equals("5Client")) {
			//	Product:
			//		ID: 68
			//		Name: Red Hat Enterprise Linux Desktop
			//		Version: 5.10
			//		Arch: i386
			//		Tags: rhel-5,rhel-5-client

			//	Product:
			//		ID: 71
			//		Name: Red Hat Enterprise Linux Workstation
			//		Version: 5.10
			//		Arch: i386
			//		Tags: rhel-5-client-workstation,rhel-5-workstation
			providingTag = "rhel-5-client-workstation";
			rhelProductCerts = clienttasks.getCurrentProductCerts(providingTag);
		}
		// special case (rhel7)
		if (rhelProductCerts.isEmpty() && clienttasks.redhatReleaseX.equals("7")) {
			//	[root@jsefler-7 rhel-7.0-beta]# pwd
			//	/tmp/rhnDefinitionsDir/product_ids/rhel-7.0-beta
			//	[root@jsefler-7 rhel-7.0-beta]# for f in $(ls *.pem); do echo ''; echo $f; rct cat-cert $f | egrep 'Tags:|ID:'; done;
			//
			//	Client-x86_64-0e02a62335a7-229.pem
			//		ID: 229
			//		Tags: rhel-7-client
			//
			//	ComputeNode-x86_64-58cda7733d76-234.pem
			//		ID: 234
			//		Tags: rhel-7-computenode
			//
			//	Everything-ppc64-f7fe5b9ea798-227.pem
			//		ID: 227
			//		Tags: rhel-7-everything
			//
			//	Everything-s390x-25a8e9a13878-228.pem
			//		ID: 228
			//		Tags: rhel-7-everything
			//
			//	Everything-x86_64-316174883467-226.pem
			//		ID: 226
			//		Tags: rhel-7-everything
			//
			//	Server-HighAvailability-x86_64-7b7efc9479e7-236.pem
			//		ID: 236
			//		Tags: rhel-7-highavailability
			//
			//	Server-LoadBalancer-x86_64-6406500c3a54-237.pem
			//		ID: 237
			//		Tags: rhel-7-loadbalancer
			//
			//	Server-ppc64-56b1260a7102-233.pem
			//		ID: 233
			//		Tags: rhel-7-server
			//
			//	Server-ResilientStorage-x86_64-d4e812911f8e-238.pem
			//		ID: 238
			//		Tags: rhel-7-resilientstorage
			//
			//	Server-s390x-efb367fafc5d-232.pem
			//		ID: 232
			//		Tags: rhel-7-server
			//
			//	Server-x86_64-4f156adfd691-230.pem
			//		ID: 230
			//		Tags: rhel-7-server
			//
			//	Workstation-x86_64-a7ced6b6706e-231.pem
			//		ID: 231
			//		Tags: rhel-7-workstation
			providingTag = "rhel-"+clienttasks.redhatReleaseX+"-.*";	// use a regex for rhel-7
			rhelProductCerts = clienttasks.getCurrentProductCerts(providingTag);
		}
		Assert.assertEquals(rhelProductCerts.size(), 1, "Only one product cert is installed that provides RHEL tag '"+providingTag+"'");
		ProductCert rhelProductCert = rhelProductCerts.get(0);
		
		// find an available RHEL subscription pool that provides for this base RHEL product cert
		//List<SubscriptionPool> rhelSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools(rhelProductCert.productId, sm_serverUrl);	// no longer works; encounters "Insufficient permissions"
		List<SubscriptionPool> rhelSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools(rhelProductCert.productId,/*sm_serverAdminUsername*/username,/*sm_serverAdminPassword*/password, sm_serverUrl);
		if (rhelSubscriptionPools.isEmpty()) throw new SkipException("Cannot find an available SubscriptionPool that provides for this installed RHEL Product: "+rhelProductCert);
		SubscriptionPool rhelSubscriptionPool = rhelSubscriptionPools.get(0);	// choose one
		
		// subscribe to the RHEL subscription
//		File rhelEntitlementCertFile = clienttasks.subscribeToSubscriptionPool(rhelSubscriptionPool,sm_serverAdminUsername,sm_serverAdminPassword,sm_serverUrl);
//		File rhelEntitlementCertKeyFile = clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(rhelEntitlementCertFile);
		clienttasks.subscribeToSubscriptionPool(rhelSubscriptionPool);
		
		// get the currently expected release listing based on the currently enabled repos
		List<String> expectedReleases = clienttasks.getCurrentlyExpectedReleases();
		
		// get the actual release listing
		List<String> actualReleases = clienttasks.getCurrentlyAvailableReleases(null,null,null);
		
		// assert that they are equivalent
		Assert.assertTrue(expectedReleases.containsAll(actualReleases) && actualReleases.containsAll(expectedReleases), "The actual subscription-manager releases list "+actualReleases+" matches the expected consolidated CDN listing "+expectedReleases+" after being granted an entitlement from subscription product: "+rhelSubscriptionPool.productId);
		Assert.assertTrue(expectedReleases.size()==actualReleases.size(), "The actual subscription-manager releases list "+actualReleases+" does not contain any duplicates.  It should be a unique list.");
	}
	
	
	@Test(	description="register to a RHEL subscription and verify that release --list excludes 6.0",
			groups={"blockedByBug-802245"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyReleaseListExcludes60OnRHEL6System_Test() throws JSONException, Exception {
		if (!clienttasks.redhatReleaseX.equals("6")) throw new SkipException("This test is only applicable on RHEL6.");
		
		// make sure we are newly registered with autosubscribe
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, null, null, null);

		// get the current base RHEL product cert
		String providingTag = "rhel-"+clienttasks.redhatReleaseX;
		List<ProductCert> rhelProductCerts = clienttasks.getCurrentProductCerts(providingTag);
		Assert.assertEquals(rhelProductCerts.size(), 1, "Only one product cert is installed that provides RHEL tag '"+providingTag+"'");
		ProductCert rhelProductCert = rhelProductCerts.get(0);
		
		// assert that it was autosubscribed
		InstalledProduct rhelInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", rhelProductCert.productId, clienttasks.getCurrentlyInstalledProducts());
		Assert.assertNotNull(rhelInstalledProduct, "Our base installed RHEL product was autosubscribed during registration.");

		// get the actual release listing
		List<String> actualReleases = clienttasks.getCurrentlyAvailableReleases(null, null, null);
		
		// assert that the list excludes 6.0, but includes the current X.Y release
		String release60 = "6.0";
		Assert.assertTrue(!actualReleases.contains(release60), "The subscription-manager releases list should exclude '"+release60+"' since '"+clienttasks.command+"' did not exist in RHEL Release '"+release60+"'.");
		//NOT PRACTICAL SINCE CONTENT FROM THIS Y-STREAM MAY NOT BE AVAILABLE UNTIL GA Assert.assertTrue(actualReleases.contains(clienttasks.redhatReleaseXY), "The subscription-manager releases list should include '"+clienttasks.redhatReleaseXY+"' since it is the current RHEL Release under test.");
	}
	
	
	@Test(	description="register to a RHEL subscription and verify that release --list excludes 5.6, 5.5, 5.4, 5.3, 5.2, 5.1, 5.0",
			groups={"blockedByBug-785989"/*,"blockedByBug-840509" MOVED TO TEMPORARY WORKAROUND*/,"blockedByBug-919700"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyReleaseListExcludes56OnRHEL5System_Test() throws JSONException, Exception {
		if (!clienttasks.redhatReleaseX.equals("5")) throw new SkipException("This test is only applicable on RHEL5.");
		
		// TEMPORARY WORKAROUND
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="840509"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			throw new SkipException("blockedByBug-840509");
		}
		// END OF WORKAROUND
		
		// make sure we are newly registered with autosubscribe
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, null, null, null);

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
		List<String> actualReleases = clienttasks.getCurrentlyAvailableReleases(null, null, null);
		
		// assert that the list excludes 5.6, 5.5, 5.4, 5.3, 5.2, 5.1, 5.0, but includes the current X.Y release
		for (String release: new String[]{"5.6","5.5","5.4","5.3","5.2","5.1","5.0"}) {
			Assert.assertTrue(!actualReleases.contains(release), "The subscription-manager releases list should exclude '"+release+"' since '"+clienttasks.command+"' did not exist in RHEL Release '"+release+"'.");
		}
		//NOT PRACTICAL SINCE CONTENT FROM THIS Y-STREAM MAY NOT BE AVAILABLE UNTIL GA Assert.assertTrue(actualReleases.contains(clienttasks.redhatReleaseXY), "The subscription-manager releases list should include '"+clienttasks.redhatReleaseXY+"' since it is the current RHEL Release under test.");
	}
	
	
	@Test(	description="using a no auth proxy server, register to a RHEL subscription and verify that release --list matches the expected CDN listing for this x-stream release of RHEL",
			groups={"blockedByBug-844368","blockedByBug-893746"/*,"blockedByBug-904193" Uncomment when we get to RHEL7*/},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyReleaseListMatchesCDNUsingNoAuthProxyCommandLineArgs_Test() throws JSONException, Exception {
		verifyReleaseListMatchesCDN(sm_noauthproxyHostname,sm_noauthproxyPort,null,null);
	}
	
	
	@Test(	description="using a basic auth proxy server, register to a RHEL subscription and verify that release --list matches the expected CDN listing for this x-stream release of RHEL",
			groups={"blockedByBug-844368","blockedByBug-893746"/*,"blockedByBug-904193" Uncomment when we get to RHEL7*/},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyReleaseListMatchesCDNUsingBasicAuthProxyCommandLineArgs_Test() {
		verifyReleaseListMatchesCDN(sm_basicauthproxyHostname,sm_basicauthproxyPort,sm_basicauthproxyUsername,sm_basicauthproxyPassword);
	}
	
	
	@Test(	description="using a no auth proxy server set within rhsm.conf, register to a RHEL subscription and verify that release --list matches the expected CDN listing for this x-stream release of RHEL",
			groups={"blockedByBug-822965","blockedByBug-844368","blockedByBug-893746"/*,"blockedByBug-904193" Uncomment when we get to RHEL7*/},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyReleaseListMatchesCDNUsingNoAuthProxyViaRhsmConfFile_Test() throws JSONException, Exception {
		clienttasks.config(false, false, true, Arrays.asList(
				new String[]{"server","proxy_hostname",sm_noauthproxyHostname},
				new String[]{"server","proxy_port",sm_noauthproxyPort}));
		verifyReleaseListMatchesCDN(null,null,null,null);
	}
	
	
	@Test(	description="using a basic auth proxy server set within rhsm.conf, register to a RHEL subscription and verify that release --list matches the expected CDN listing for this x-stream release of RHEL",
			groups={"blockedByBug-822965","blockedByBug-844368","blockedByBug-893746"/*,"blockedByBug-904193" Uncomment when we get to RHEL7*/},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyReleaseListMatchesCDNUsingBasicAuthProxyViaRhsmConfFile_Test() {
		clienttasks.config(false, false, true, Arrays.asList(
				new String[]{"server","proxy_hostname",sm_basicauthproxyHostname},
				new String[]{"server","proxy_port",sm_basicauthproxyPort},
				new String[]{"server","proxy_user",sm_basicauthproxyUsername},
				new String[]{"server","proxy_password",sm_basicauthproxyPassword}));
		verifyReleaseListMatchesCDN(null,null,null,null);
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
		SSHCommandResult registerResult = clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,true,null,null,(List<String>)null,null,null,null,true, null, proxy, proxy_username, proxy_password);

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
		List<String> actualReleases = clienttasks.getCurrentlyAvailableReleases(proxy, proxy_username, proxy_password);
		
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
		ll.add(Arrays.asList(new Object[]{null,	sm_clientUsername, sm_clientPassword, sm_clientOrg}));

		// add another row only if the RHN credentials are valid against sm_serverHostname...
		//if (sm_serverType.equals(CandlepinType.hosted) && !sm_rhnUsername.isEmpty())
		if (doesStringContainMatches(sm_rhnHostname, "rhn\\.(.+\\.)*redhat\\.com") && doesStringContainMatches(sm_serverHostname, "subscription\\.rhn\\.(.+\\.)*redhat\\.com") && !sm_rhnUsername.isEmpty())
		ll.add(Arrays.asList(new Object[]{new BlockedByBzBug(new String[]{"861151","870141"}),	sm_rhnUsername, sm_rhnPassword, null}));

		return ll;
	}
}
