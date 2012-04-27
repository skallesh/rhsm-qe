package com.redhat.qe.sm.cli.tests;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.Repo;
import com.redhat.qe.sm.data.YumRepo;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 * How to get the expected release list
 * http://cdn-internal.rcm-test.redhat.com/content/dist/rhel/server/5/listing
 * http://cdn-internal.rcm-test.redhat.com/content/dist/rhel/server/6/listing
 */

@Test(groups={"ReleaseTests","AcceptanceTest"})
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
		
		result = clienttasks.release_(null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from calling release without being registered");
		Assert.assertEquals(result.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered, "Stdout from release without being registered");

		result = clienttasks.release_(true, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from calling release --list without being registered");
		Assert.assertEquals(result.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered, "Stdout from release without being registered");

		result = clienttasks.release_(null, "FOO", null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from calling release --set without being registered");
		Assert.assertEquals(result.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered, "Stdout from release without being registered");
	}
	
	@Test(	description="attempt to get the subscription-manager release when a release has not been set; should be told that the release is not set",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptToGetReleaseWhenReleaseHasNotBeenSet_Test() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,true,null,null,null, null);		
		SSHCommandResult result = clienttasks.release(null, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "Release not set", "Stdout from release without having set it");
	}
	
	@Test(	description="attempt to get the subscription-manager release list without having subscribed to any entitlements",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptToGetReleaseListWithoutHavingAnyEntitlements_Test() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,true,null,null,null, null);		

		Assert.assertTrue(clienttasks.getCurrentlyAvailableReleases().isEmpty(), "No releases should be available without having been entitled to anything.");
	}
	
	@Test(	description="verify that the consumer's current subscription-manager release value matches the release value just set",
			groups={"blockedByBug-814385"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void GetTheReleaseAfterSettingTheRelease_Test() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,true,null,null,null, null);		

		clienttasks.release(null, "Foo", null, null, null);
		Assert.assertEquals(clienttasks.getCurrentRelease(), "Foo", "The release value retrieved after setting the release.");
	}

	@Test(	description="assert that the subscription-manager release can be unset",
			groups={"blockedByBug-807822","blockedByBug-814385"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void UnsetTheReleaseAfterSettingTheRelease_Test() {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,true,null,null,null, null);		

		clienttasks.release(null, "Foo", null, null, null);
		SSHCommandResult result = clienttasks.release(null, "", null, null, null);
		//Assert.assertEquals(result.getStdout().trim(), "Release not set", "Stdout from release after attempting to unset it.");	// DEV choose to implement this differently https://bugzilla.redhat.com/show_bug.cgi?id=807822#c2
		Assert.assertEquals(clienttasks.getCurrentRelease(), "", "The release value retrieved after attempting to unset it.");
	}
	
	
	@Test(	description="after subscribing to all available subscriptions, assert that content with url paths that reference $releasever are substituted with the consumers current release preference",
			groups={"blockedByBug-807407"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyReleaseverSubstitutionInRepoLists_Test() throws JSONException, Exception {
		
		// make sure we are newly registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,true,null,null,null, null);

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
		String releaseVer = "TestRelease-1.0";
		clienttasks.release(null, releaseVer, null, null, null);

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
		clienttasks.release(null, "", null, null, null);
		
		// assert that each of the Repos and YumRepos after unsetting the release version preference where restore to their original values (containing $releasever)
		List<Repo> reposAfterSettingReleaseVer = clienttasks.getCurrentlySubscribedRepos();
		List<YumRepo> yumReposAfterSettingReleaseVer = clienttasks.getCurrentlySubscribedYumRepos();
		
		Assert.assertTrue(reposAfterSettingReleaseVer.containsAll(reposBeforeSettingReleaseVer) && reposBeforeSettingReleaseVer.containsAll(reposAfterSettingReleaseVer),
				"After unsetting the release version preference, all of the subscription-manager repos --list were restored to their original values.");
		Assert.assertTrue(yumReposAfterSettingReleaseVer.containsAll(yumReposBeforeSettingReleaseVer) && yumReposBeforeSettingReleaseVer.containsAll(yumReposAfterSettingReleaseVer),
				"After unsetting the release version preference, all of the yum repolist were restored to their original values.");
	}

	// TODO
	// Test against stage to ensure releaseVer --list returns all the valid values for rhel5 and rhel6

	
	

	
	
	

	// Candidates for an automated Test:
	
	
		
	// Configuration methods ***********************************************************************


	
	
	// Protected methods ***********************************************************************



	
	// Data Providers ***********************************************************************
	

}
