package rhsm.cli.tests;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"CleanTests"})
public class CleanTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager-cli: clean and verify the identity is removed",
			groups={"AcceptanceTests","blockedByBug-654429","blockedByBug-962520"},
			enabled=true)
	@ImplementsNitrateTest(caseId=64178)	// http://gibson.usersys.redhat.com/agilo/ticket/4020
	public void Clean_Test() {
		
		// Start fresh by unregistering and registering...
		log.info("Start fresh by unregistering and registering...");
		clienttasks.unregister(null, null, null);
		consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null, null, null, false, null, null, null));
		
		// Subscribe to a randomly available pool...
		log.info("Subscribe to a randomly available pool...");
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.size()<=0) throw new SkipException("No susbcriptions were available for which an entitlement could be granted and subsequently cleaned by this test.");
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribeToSubscriptionPool(pool);

		// Clean
		log.info("Clean...");
		clienttasks.clean(null, null, null);
		
		// Assert the entitlements are removed
		// this was already tested within clienttasks.clean() method
		
		// Assert that because we have run clean, rhsm no longer has an identity and therefore requires us to register to run commands 
		log.info("After running clean, assert that the identity is unknown thereby requiring that we be registered...");
		SSHCommandResult result = clienttasks.identity_(null,null,null, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(),clienttasks.msg_ConsumerNotRegistered, "Consumer identity has been removed after clean, therefore we must register to restore our identity.");
	}
	
	
	@Test(	description="subscription-manager: set manage_repos to 0 and assert clean still works.",
			enabled=true,
			groups={"AcceptanceTests", "blockedByBug-799394", "CleanAfterRhsmManageReposIsConfigured_Test"})
	//@ImplementsNitrateTest(caseId=)
	public void CleanAfterRhsmManageReposIsConfigured_Test() throws JSONException, Exception{
		List<String[]> rhsmManageRepo = new ArrayList<String[]>();
		for (String value : new String[]{"1","0"}) {
			rhsmManageRepo.clear();
			rhsmManageRepo.add(new String[]{"rhsm", "manage_repos", value});
			clienttasks.config(null, null, true, rhsmManageRepo);
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
			clienttasks.clean(null, null, null);
		}
		
	}
	
	@Test(	description="verify redhat.repo file is deleted of after calling subscription-manager clean",
			groups={"blockedByBug-781510"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=) //TODO Find a tcms caseId for
	public void VerifyRedHatRepoFileIsDeletedAfterClean_Test() {
		
		// Start fresh by registering...
		log.info("Start fresh by registering...");
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null, null, true, false, null, null, null);
		client.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError			
	    Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"Expecting the redhat repo file '"+clienttasks.redhatRepoFile+"' to exist after registering and triggering a yum transacton.");
		clienttasks.clean(null, null, null);
	    Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"Expecting the redhat repo file '"+clienttasks.redhatRepoFile+"' to NOT exist after running clean.");
	}
	
	
	// Candidates for an automated Test:

	
	
	// Configuration methods ***********************************************************************
	
	String consumerId = null;
	@AfterClass(groups={"setup"})
	public void teardownAfterClass() {
		if (clienttasks!=null) {
			clienttasks.unregister_(null, null, null);
			if (consumerId!=null) {
				clienttasks.register_(sm_clientUsername, sm_clientPassword, null, null, null, null, consumerId, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null);
				clienttasks.unregister_(null, null, null);
				consumerId = null;
			}
		}
		
	}
	
	@AfterGroups(groups={"setup"}, value={"CleanAfterRhsmManageReposIsConfigured_Test"})
	public void setManageReposConfiguration() {
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "manage_repos", "1");
		//clienttasks.config(null, null, true, Arrays.asList(new String[]{"",""}));
	}
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
