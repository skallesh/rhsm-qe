package rhsm.cli.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.SkipException;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.ProductSubscription;
import rhsm.data.SubscriptionPool;
import rhsm.data.YumRepo;

import com.redhat.qe.Assert;
import com.redhat.qe.tools.SSHCommandResult;

/**
 *  @author jsefler
 *
 */
@Test(groups={"RepoOverrideTests"})
public class RepoOverrideTests extends SubscriptionManagerCLITestScript{
	
	
	// Test Methods ***********************************************************************
	
	
	@Test(	description="when subscription-manager repos-override is run with no args, it should default to --list option",
			groups={},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void ListRepoOverridesIsTheDefault_Test() {

		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		
		// without any subscriptions attached...
		SSHCommandResult defaultResult = clienttasks.repo_override_(null,null,(String)null,(String)null,null,null,null,null);
		SSHCommandResult listResult = clienttasks.repo_override_(true,null,(String)null,(String)null,null,null,null,null);
		Assert.assertEquals(listResult.toString().trim(), defaultResult.toString().trim(), "The result from running module repo-override without any options should default to the --list result (with no subscriptions attached)");
		Assert.assertEquals(listResult.getStdout().trim(), "This system does not have any subscriptions.", "Stdout from repo-override --list without any subscriptions attached.");
		Assert.assertEquals(listResult.getStderr().trim(), "", "Stderr from repo-override --list without any subscriptions attached.");
		Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(1), "ExitCode from repo-override --list without any subscriptions attached.");
		
		// subscribe to a random pool (so as to consume an entitlement)
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("Cannot randomly pick a pool for subscribing when there are no available pools for testing."); 
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribeToSubscriptionPool(pool);
		
		// with a subscription attached...
		defaultResult = clienttasks.repo_override_(null,null,(String)null,(String)null,null,null,null,null);
		listResult = clienttasks.repo_override_(true,null,(String)null,(String)null,null,null,null,null);
		Assert.assertEquals(listResult.toString().trim(), defaultResult.toString().trim(), "The result from running module repo-override without any options should default to the --list result (with subscriptions attached)");
		Assert.assertEquals(listResult.getStdout().trim(), "This system does not have any content overrides applied to it.", "Stdout from repo-override --list without any overrides.");
		Assert.assertEquals(listResult.getStderr().trim(), "", "Stderr from repo-override --list without any overrides.");
		Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(0), "ExitCode from repo-override --list without any overrides.");
	}
	
	@Test(	description="attempt to override a baseurl using subscription-manager repos-override",
			groups={"blockedByBug-1030604"},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void AttemptToOverrideBaseurl_Test() {

		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		
		// subscribe to a random pool (so as to consume an entitlement) and remember the original list of YumRepos read from the redhat.repo file
		List<YumRepo> yumRepos = attachRandomSubscriptionThatProvidesYumRepos();
		
		// attempt to override the baseurl
		YumRepo yumRepo = yumRepos.get(randomGenerator.nextInt(yumRepos.size())); // randomly pick a YumRepo
		Map<String,String> repoOverrideNameValueMap = new HashMap<String,String>();
		repoOverrideNameValueMap.put("baseurl", "https://cdn.redhat.com/repo-override-testing/$releasever/$basearch");
		repoOverrideNameValueMap.put("test", "value");
		SSHCommandResult result = clienttasks.repo_override_(null, null, Arrays.asList(yumRepo.id, "foo-bar"), null, repoOverrideNameValueMap, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from an attempt to repo-override the baseurl of yumRepo: "+yumRepo);		
		Assert.assertEquals(result.getStderr().trim(), "The value for name 'baseurl' is not allowed to be overridden.", "Stderr from an attempt to repo-override the baseurl of yumRepo: "+yumRepo);
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from an attempt to repo-override the baseurl of yumRepo: "+yumRepo);
	}
	
	@Test(	description="add yum repo overrides, verify they persist, and remove them one repo at a time",
			groups={},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void AddAndRemoveRepoOverridesOneRepoAtATime_Test() {

		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		
		// subscribe to a random pool (so as to consume an entitlement) and remember the original list of YumRepos read from the redhat.repo file
		List<YumRepo> originalYumRepos = attachRandomSubscriptionThatProvidesYumRepos();
		
		// choose a random small subset of repos to test repo-override
		List<YumRepo> originalYumReposSubset = getRandomSubsetOfList(originalYumRepos, 5);
		
		// add several repo overrides (one repo at a time)
		Map<String,Map<String,String>> repoOverridesMapOfMaps = new HashMap<String,Map<String,String>>();
		for (YumRepo yumRepo : originalYumReposSubset) {
			String repoId = yumRepo.id;
			Map<String,String> repoOverrideNameValueMap = new HashMap<String,String>();
			repoOverrideNameValueMap.put("name", "Repo "+repoId);
			repoOverrideNameValueMap.put("enabled", "true");
			repoOverrideNameValueMap.put("gpgcheck", "false");
			repoOverrideNameValueMap.put("exclude", "foo-bar");
			repoOverrideNameValueMap.put("retries", "5");
			repoOverrideNameValueMap.put("ui_repoid_vars", "releasever basearch foo");
			repoOverrideNameValueMap.put("sslverify", "false");
			repoOverrideNameValueMap.put("sslcacert", "/overridden/candlepin.pem");
			repoOverrideNameValueMap.put("sslclientkey", "/overridden/serial-key.pem");
			repoOverrideNameValueMap.put("sslclientcert", "/overridden/serial.pem");
			repoOverridesMapOfMaps.put(repoId, repoOverrideNameValueMap);
			clienttasks.repo_override(null, null, repoId, null, repoOverrideNameValueMap, null, null, null);
		}

		// verify the current YumRepos read from the redhat.repo file actually contain the overrides
		verifyCurrentYumReposReflectRepoOverrides(originalYumRepos,repoOverridesMapOfMaps, true);
		
		// unsubscribe/resubscribe
		List<String> poolIds = new ArrayList<String>();
		for (ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			clienttasks.unsubscribe(true, productSubscription.serialNumber, null, null, null);
			poolIds.add(productSubscription.poolId);
		}
		SSHCommandResult listResult = clienttasks.repo_override_(true,null,(String)null,(String)null,null,null,null,null);
		Assert.assertEquals(listResult.getStdout().trim(), "This system does not have any subscriptions.", "Stdout from repo-override --list without any subscriptions attached (but should still have overrides cached in the consumer).");
		Assert.assertTrue(clienttasks.getCurrentlySubscribedYumRepos().isEmpty(), "The YumRepos in '"+clienttasks.redhatRepoFile+"' should be empty after unsubscribing from each serial.");
		clienttasks.subscribe(null, null, poolIds, null, null, null, null, null, null, null, null);
		
		// ...and verify the YumRepos read from the redhat.repo file persists the overrides
		verifyCurrentYumReposReflectRepoOverrides(originalYumRepos,repoOverridesMapOfMaps, false/*because after unsubscribe/resubscribe the current yum repos come from a new entitlement and therefore cannot be equal to the originalYumRepo value for sslclientcert and sslclientkey*/);
		
		// remove names from one repo override and verify the list
		String repoId = (String) repoOverridesMapOfMaps.keySet().toArray()[0];	// choose one repoId
		clienttasks.repo_override(null,null,Arrays.asList(new String[]{repoId}),Arrays.asList(new String[]{"name"}),null,null,null,null);
		repoOverridesMapOfMaps.get(repoId).remove("name");
		clienttasks.repo_override(null,null,Arrays.asList(new String[]{repoId}),Arrays.asList(new String[]{"name","enabled","ui_repoid_vars"}),null,null,null,null);
		repoOverridesMapOfMaps.get(repoId).remove("name");
		repoOverridesMapOfMaps.get(repoId).remove("enabled");
		repoOverridesMapOfMaps.get(repoId).remove("ui_repoid_vars");
		//clienttasks.repo_override(null,null,Arrays.asList(new String[]{repoId}),Arrays.asList(new String[]{"gpgcheck","exclude","retries"}),null,null,null,null);	// for test variability, let's not delete these
		//repoOverridesMapOfMaps.get(repoId).remove("gpgcheck");
		//repoOverridesMapOfMaps.get(repoId).remove("exclude");
		//repoOverridesMapOfMaps.get(repoId).remove("retries");
		clienttasks.repo_override(null,null,Arrays.asList(new String[]{repoId}),Arrays.asList(new String[]{"sslverify","sslcacert","sslclientkey","sslclientcert"}),null,null,null,null);
		repoOverridesMapOfMaps.get(repoId).remove("sslverify");
		repoOverridesMapOfMaps.get(repoId).remove("sslcacert");
		repoOverridesMapOfMaps.get(repoId).remove("sslclientkey");
		repoOverridesMapOfMaps.get(repoId).remove("sslclientcert");
		//repoOverridesMapOfMaps.remove(repoid);	// remove one repoid from the overrides Map
		
		// verify the current YumRepos read from the redhat.repo file no longer contains the removed override (the original should be restored)
		verifyCurrentYumReposReflectRepoOverrides(originalYumRepos,repoOverridesMapOfMaps, false);

		// remove all of the repo overrides
		clienttasks.repo_override(null,true,(String)null,(String)null,null,null,null,null);	
		repoOverridesMapOfMaps.clear();
		
		// verify the current YumRepos read from the redhat.repo file no longer contains any overrides (the original should be restored)
		verifyCurrentYumReposReflectRepoOverrides(originalYumRepos,repoOverridesMapOfMaps, false);
	}
	
	@Test(	description="add yum repo overrides, verify they persist, and remove them across multiple repo ids simultaneously (use multiple --repo args)",
			groups={},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void AddAndRemoveRepoOverridesUsingMultipleRepos_Test() {

		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		
		// subscribe to a random pool (so as to consume an entitlement) and remember the original list of YumRepos read from the redhat.repo file
		List<YumRepo> originalYumRepos = attachRandomSubscriptionThatProvidesYumRepos();
		
		// choose a random small subset of repos to test repo-override
		List<YumRepo> originalYumReposSubset = getRandomSubsetOfList(originalYumRepos, 5);
		
		// add several repo overrides (specifying all repos in multiplicity)
		Map<String,Map<String,String>> repoOverridesMapOfMaps = new HashMap<String,Map<String,String>>();
		Map<String,String> repoOverrideNameValueMap = new HashMap<String,String>();
		repoOverrideNameValueMap.put("enabled", "true");
		repoOverrideNameValueMap.put("gpgcheck", "false");
		repoOverrideNameValueMap.put("exclude", "foo-bar");
		repoOverrideNameValueMap.put("retries", "5");
		repoOverrideNameValueMap.put("ui_repoid_vars", "releasever basearch foo");
		repoOverrideNameValueMap.put("sslverify", "false");
		repoOverrideNameValueMap.put("sslcacert", "/overridden/candlepin.pem");
		repoOverrideNameValueMap.put("sslclientkey", "/overridden/serial-key.pem");
		repoOverrideNameValueMap.put("sslclientcert", "/overridden/serial.pem");
		for (YumRepo yumRepo : originalYumReposSubset) repoOverridesMapOfMaps.put(yumRepo.id, repoOverrideNameValueMap);
		List<String> repoIds = new ArrayList<String>(repoOverridesMapOfMaps.keySet());
		clienttasks.repo_override(null, null, repoIds, null, repoOverrideNameValueMap, null, null, null);
		
		// verify the current YumRepos read from the redhat.repo file actually contain the overrides
		verifyCurrentYumReposReflectRepoOverrides(originalYumRepos,repoOverridesMapOfMaps, true);
		
		// unsubscribe/resubscribe
		List<String> poolIds = new ArrayList<String>();
		for (ProductSubscription productSubscription : clienttasks.getCurrentlyConsumedProductSubscriptions()) {
			clienttasks.unsubscribe(true, productSubscription.serialNumber, null, null, null);
			poolIds.add(productSubscription.poolId);
		}
		SSHCommandResult listResult = clienttasks.repo_override_(true,null,(String)null,(String)null,null,null,null,null);
		Assert.assertEquals(listResult.getStdout().trim(), "This system does not have any subscriptions.", "Stdout from repo-override --list without any subscriptions attached (but should still have overrides cached in the consumer).");
		Assert.assertTrue(clienttasks.getCurrentlySubscribedYumRepos().isEmpty(), "The YumRepos in '"+clienttasks.redhatRepoFile+"' should be empty after unsubscribing from each serial.");
		clienttasks.subscribe(null, null, poolIds, null, null, null, null, null, null, null, null);
		
		// ...and verify the YumRepos read from the redhat.repo file persists the overrides
		verifyCurrentYumReposReflectRepoOverrides(originalYumRepos,repoOverridesMapOfMaps, false/*because after unsubscribe/resubscribe the current yum repos come from a new entitlement and therefore cannot be equal to the originalYumRepo value for sslclientcert and sslclientkey*/);
		
		// remove names from multiple repos using repo-override and verify the list
		clienttasks.repo_override(null,null,repoIds,Arrays.asList(new String[]{"name"}),null,null,null,null);
		clienttasks.repo_override(null,null,repoIds,Arrays.asList(new String[]{"name","enabled","ui_repoid_vars"}),null,null,null,null);
		// clienttasks.repo_override(null,null,repoids,Arrays.asList(new String[]{"gpgcheck","exclude","retries"}),null,null,null,null);	// for test variability, let's not delete these
		clienttasks.repo_override(null,null,repoIds,Arrays.asList(new String[]{"sslverify","sslcacert","sslclientkey","sslclientcert"}),null,null,null,null);
		for (String repoId : repoIds) {
			repoOverridesMapOfMaps.get(repoId).remove("name");
			repoOverridesMapOfMaps.get(repoId).remove("enabled");
			repoOverridesMapOfMaps.get(repoId).remove("ui_repoid_vars");
			//repoOverridesMapOfMaps.get(repoId).remove("gpgcheck");
			//repoOverridesMapOfMaps.get(repoId).remove("exclude");
			//repoOverridesMapOfMaps.get(repoId).remove("retries");
			repoOverridesMapOfMaps.get(repoId).remove("sslverify");
			repoOverridesMapOfMaps.get(repoId).remove("sslcacert");
			repoOverridesMapOfMaps.get(repoId).remove("sslclientkey");
			repoOverridesMapOfMaps.get(repoId).remove("sslclientcert");
		}
		
		// verify the current YumRepos read from the redhat.repo file no longer contains the removed override (the original should be restored)
		verifyCurrentYumReposReflectRepoOverrides(originalYumRepos,repoOverridesMapOfMaps, false);

		// remove all of the repo overrides
		clienttasks.repo_override(null,true,(List<String>)null,(List<String>)null,null,null,null,null);
		repoOverridesMapOfMaps.clear();
		
		// verify the current YumRepos read from the redhat.repo file no longer contains any overrides (the original should be restored)
		verifyCurrentYumReposReflectRepoOverrides(originalYumRepos,repoOverridesMapOfMaps, false);
	}
	
	// Candidates for an automated Test:
	
	
	
	
	// Configuration methods ***********************************************************************
	
	
	
	// Protected methods ***********************************************************************
	
	/**
	 * @return a list of YumRepos that result after randomly attaching a subscription
	 */
	protected List<YumRepo> attachRandomSubscriptionThatProvidesYumRepos() {
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.isEmpty()) throw new SkipException("Cannot randomly pick a pool for subscribing when there are no available pools for testing."); 
		List<YumRepo> yumRepos;	// = new ArrayList<YumRepo>();
		do {
			//not important clienttasks.unsubscribe_(true, (BigInteger)null, null, null, null);
			SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
			pools.remove(pool);
			clienttasks.subscribeToSubscriptionPool(pool);
			yumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		} while (yumRepos.isEmpty() && !pools.isEmpty());
		if (yumRepos.isEmpty()) throw new SkipException("Could not find a pool that provided YumRepos after subscribing."); 
		return yumRepos;
	}
	
	protected void verifyCurrentYumReposReflectRepoOverrides(List<YumRepo> originalYumRepos, Map<String,Map<String,String>> repoOverridesMapOfMaps, boolean includeVerificationOfNames_sslclient) {
		
		// verify the current YumRepos read from the redhat.repo file actually contain the overrides
		List<YumRepo> currentYumRepos = clienttasks.getCurrentlySubscribedYumRepos();
		Assert.assertEquals(currentYumRepos.size(), originalYumRepos.size(), "The number of yum repos in "+clienttasks.redhatRepoFile+" should remain the same after applying subscription-manager repo-overrides.");
		for (YumRepo currentYumRepo : currentYumRepos) {
			String name, repoId = currentYumRepo.id;
			YumRepo originalYumRepo = YumRepo.findFirstInstanceWithMatchingFieldFromList("id", repoId, originalYumRepos);
			
			// this list of names should include all the fields from public class YumRepo
			name = "id";					if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.id==null?null:currentYumRepo.id.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.id==null?null:currentYumRepo.id.toString(), originalYumRepo.id==null?null:originalYumRepo.id.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "name";					if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.name==null?null:currentYumRepo.name.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.name==null?null:currentYumRepo.name.toString(), originalYumRepo.name==null?null:originalYumRepo.name.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "baseurl";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.baseurl==null?null:currentYumRepo.baseurl.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.baseurl==null?null:currentYumRepo.baseurl.toString(), originalYumRepo.baseurl==null?null:originalYumRepo.baseurl.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "enabled";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.enabled==null?null:currentYumRepo.enabled.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.enabled==null?null:currentYumRepo.enabled.toString(), originalYumRepo.enabled==null?null:originalYumRepo.enabled.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "gpgcheck";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.gpgcheck==null?null:currentYumRepo.gpgcheck.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.gpgcheck==null?null:currentYumRepo.gpgcheck.toString(), originalYumRepo.gpgcheck==null?null:originalYumRepo.gpgcheck.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "sslcacert";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.sslcacert==null?null:currentYumRepo.sslcacert.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.sslcacert==null?null:currentYumRepo.sslcacert.toString(), originalYumRepo.sslcacert==null?null:originalYumRepo.sslcacert.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "sslverify";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.sslverify==null?null:currentYumRepo.sslverify.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.sslverify==null?null:currentYumRepo.sslverify.toString(), originalYumRepo.sslverify==null?null:originalYumRepo.sslverify.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			/* these two options are verified below to account for boolean includeVerificationOfNames_sslclient
			name = "sslclientcert";			if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.sslclientcert==null?null:currentYumRepo.sslclientcert.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.sslclientcert==null?null:currentYumRepo.sslclientcert.toString(), originalYumRepo.sslclientcert==null?null:originalYumRepo.sslclientcert.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "sslclientkey";			if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.sslclientkey==null?null:currentYumRepo.sslclientkey.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.sslclientkey==null?null:currentYumRepo.sslclientkey.toString(), originalYumRepo.sslclientkey==null?null:originalYumRepo.sslclientkey.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			*/
			name = "metadata_expire";		if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.metadata_expire==null?null:currentYumRepo.metadata_expire.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.metadata_expire==null?null:currentYumRepo.metadata_expire.toString(), originalYumRepo.metadata_expire==null?null:originalYumRepo.metadata_expire.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "metalink";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.metalink==null?null:currentYumRepo.metalink.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.metalink==null?null:currentYumRepo.metalink.toString(), originalYumRepo.metalink==null?null:originalYumRepo.metalink.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "mirrorlist";			if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.mirrorlist==null?null:currentYumRepo.mirrorlist.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.mirrorlist==null?null:currentYumRepo.mirrorlist.toString(), originalYumRepo.mirrorlist==null?null:originalYumRepo.mirrorlist.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "repo_gpgcheck";			if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.repo_gpgcheck==null?null:currentYumRepo.repo_gpgcheck.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.repo_gpgcheck==null?null:currentYumRepo.repo_gpgcheck.toString(), originalYumRepo.repo_gpgcheck==null?null:originalYumRepo.repo_gpgcheck.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "gpgcakey";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.gpgcakey==null?null:currentYumRepo.gpgcakey.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.gpgcakey==null?null:currentYumRepo.gpgcakey.toString(), originalYumRepo.gpgcakey==null?null:originalYumRepo.gpgcakey.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "exclude";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.exclude==null?null:currentYumRepo.exclude.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.exclude==null?null:currentYumRepo.exclude.toString(), originalYumRepo.exclude==null?null:originalYumRepo.exclude.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "includepkgs";			if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.includepkgs==null?null:currentYumRepo.includepkgs.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.includepkgs==null?null:currentYumRepo.includepkgs.toString(), originalYumRepo.includepkgs==null?null:originalYumRepo.includepkgs.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "enablegroups";			if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.enablegroups==null?null:currentYumRepo.enablegroups.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.enablegroups==null?null:currentYumRepo.enablegroups.toString(), originalYumRepo.enablegroups==null?null:originalYumRepo.enablegroups.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "failovermethod";		if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.failovermethod==null?null:currentYumRepo.failovermethod.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.failovermethod==null?null:currentYumRepo.failovermethod.toString(), originalYumRepo.failovermethod==null?null:originalYumRepo.failovermethod.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "keepalive";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.keepalive==null?null:currentYumRepo.keepalive.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.keepalive==null?null:currentYumRepo.keepalive.toString(), originalYumRepo.keepalive==null?null:originalYumRepo.keepalive.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "timeout";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.timeout==null?null:currentYumRepo.timeout.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.timeout==null?null:currentYumRepo.timeout.toString(), originalYumRepo.timeout==null?null:originalYumRepo.timeout.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "http_caching";			if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.http_caching==null?null:currentYumRepo.http_caching.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.http_caching==null?null:currentYumRepo.http_caching.toString(), originalYumRepo.http_caching==null?null:originalYumRepo.http_caching.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "retries";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.retries==null?null:currentYumRepo.retries.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.retries==null?null:currentYumRepo.retries.toString(), originalYumRepo.retries==null?null:originalYumRepo.retries.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "throttle";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.throttle==null?null:currentYumRepo.throttle.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.throttle==null?null:currentYumRepo.throttle.toString(), originalYumRepo.throttle==null?null:originalYumRepo.throttle.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "bandwidth";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.bandwidth==null?null:currentYumRepo.bandwidth.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.bandwidth==null?null:currentYumRepo.bandwidth.toString(), originalYumRepo.bandwidth==null?null:originalYumRepo.bandwidth.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "mirrorlist_expire";		if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.mirrorlist_expire==null?null:currentYumRepo.mirrorlist_expire.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.mirrorlist_expire==null?null:currentYumRepo.mirrorlist_expire.toString(), originalYumRepo.mirrorlist_expire==null?null:originalYumRepo.mirrorlist_expire.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "proxy";					if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.proxy==null?null:currentYumRepo.proxy.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.proxy==null?null:currentYumRepo.proxy.toString(), originalYumRepo.proxy==null?null:originalYumRepo.proxy.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "proxy_username";		if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.proxy_username==null?null:currentYumRepo.proxy_username.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.proxy_username==null?null:currentYumRepo.proxy_username.toString(), originalYumRepo.proxy_username==null?null:originalYumRepo.proxy_username.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "proxy_password";		if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.proxy_password==null?null:currentYumRepo.proxy_password.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.proxy_password==null?null:currentYumRepo.proxy_password.toString(), originalYumRepo.proxy_password==null?null:originalYumRepo.proxy_password.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "username";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.username==null?null:currentYumRepo.username.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.username==null?null:currentYumRepo.username.toString(), originalYumRepo.username==null?null:originalYumRepo.username.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "password";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.password==null?null:currentYumRepo.password.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.password==null?null:currentYumRepo.password.toString(), originalYumRepo.password==null?null:originalYumRepo.password.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "cost";					if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.cost==null?null:currentYumRepo.cost.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.cost==null?null:currentYumRepo.cost.toString(), originalYumRepo.cost==null?null:originalYumRepo.cost.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "skip_if_unavailable";	if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.skip_if_unavailable==null?null:currentYumRepo.skip_if_unavailable.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.skip_if_unavailable==null?null:currentYumRepo.skip_if_unavailable.toString(), originalYumRepo.skip_if_unavailable==null?null:originalYumRepo.skip_if_unavailable.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "ui_repoid_vars";		if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.ui_repoid_vars==null?null:currentYumRepo.ui_repoid_vars.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.ui_repoid_vars==null?null:currentYumRepo.ui_repoid_vars.toString(), originalYumRepo.ui_repoid_vars==null?null:originalYumRepo.ui_repoid_vars.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "priority";				if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.priority==null?null:currentYumRepo.priority.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {Assert.assertEquals(currentYumRepo.priority==null?null:currentYumRepo.priority.toString(), originalYumRepo.priority==null?null:originalYumRepo.priority.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			
			name = "sslclientcert";			if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.sslclientcert==null?null:currentYumRepo.sslclientcert.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {if (includeVerificationOfNames_sslclient) Assert.assertEquals(currentYumRepo.sslclientcert==null?null:currentYumRepo.sslclientcert.toString(), originalYumRepo.sslclientcert==null?null:originalYumRepo.sslclientcert.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
			name = "sslclientkey";			if (repoOverridesMapOfMaps.containsKey(repoId) && repoOverridesMapOfMaps.get(repoId).containsKey(name)) {Assert.assertEquals(currentYumRepo.sslclientkey==null?null:currentYumRepo.sslclientkey.toString(), repoOverridesMapOfMaps.get(repoId).get(name), "The repo-override for repoId='"+repoId+"' name='"+name+"'.");} else {if (includeVerificationOfNames_sslclient) Assert.assertEquals(currentYumRepo.sslclientkey==null?null:currentYumRepo.sslclientkey.toString(), originalYumRepo.sslclientkey==null?null:originalYumRepo.sslclientkey.toString(), "There should NOT be a repo-override for repoId='"+repoId+"' name='"+name+"'.");}
		}
	}
	
	
	// Data Providers ***********************************************************************
	
}
