package rhsm.cli.tests;

import java.util.ArrayList;
import java.util.List;

import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;

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

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-20022", "RHEL7-51039"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager-cli: clean and verify the identity is removed",
			groups={"Tier1Tests","blockedByBug-654429","blockedByBug-962520"},
			enabled=true)
	@ImplementsNitrateTest(caseId=64178)	// http://gibson.usersys.redhat.com/agilo/ticket/4020
	public void testClean() {
		
		// Start fresh by unregistering and registering...
		log.info("Start fresh by unregistering and registering...");
		clienttasks.unregister(null, null, null, null);
		consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null, null, null, false, null, null, null, null));
		
		// Subscribe to a randomly available pool...
		log.info("Subscribe to a randomly available pool...");
		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		if (pools.size()<=0) throw new SkipException("No susbcriptions were available for which an entitlement could be granted and subsequently cleaned by this test.");
		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
		clienttasks.subscribeToSubscriptionPool(pool);

		// Clean
		log.info("Clean...");
		clienttasks.clean();
		
		// Assert the entitlements are removed
		// this was already tested within clienttasks.clean() method
		
		// Assert that because we have run clean, rhsm no longer has an identity and therefore requires us to register to run commands 
		log.info("After running clean, assert that the identity is unknown thereby requiring that we be registered...");
		SSHCommandResult result = clienttasks.identity_(null,null,null, null, null, null, null, null);
		if (clienttasks.isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			Assert.assertEquals(result.getStderr().trim(), clienttasks.msg_ConsumerNotRegistered, "Consumer identity has been removed after clean, therefore we must register to restore our identity.");
		} else {
			Assert.assertEquals(result.getStdout().trim(), clienttasks.msg_ConsumerNotRegistered, "Consumer identity has been removed after clean, therefore we must register to restore our identity.");
		}
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-20021", "RHEL7-51038"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscription-manager: set manage_repos to 0 and assert clean still works.",
			enabled=true,
			groups={"Tier1Tests", "blockedByBug-799394", "CleanAfterRhsmManageReposIsConfigured_Test"})
	//@ImplementsNitrateTest(caseId=)
	public void testCleanAfterRhsmManageReposIsConfigured() throws JSONException, Exception{
		List<String[]> rhsmManageRepo = new ArrayList<String[]>();
		for (String value : new String[]{"1","0"}) {
			rhsmManageRepo.clear();
			rhsmManageRepo.add(new String[]{"rhsm", "manage_repos", value});
			clienttasks.config(null, null, true, rhsmManageRepo);
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null, null);
			clienttasks.clean();
		}
		
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-36617", "RHEL7-51428"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify redhat.repo file is deleted of after calling subscription-manager clean",
			groups={"Tier2Tests","blockedByBug-781510"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=) //TODO Find a tcms caseId for
	public void testRedHatRepoFileIsDeletedAfterClean() {
		
		// Start fresh by registering...
		log.info("Start fresh by registering...");
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null, null, true, false, null, null, null, null);
		client.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError			
	    Assert.assertTrue(RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"Expecting the redhat repo file '"+clienttasks.redhatRepoFile+"' to exist after registering and triggering a yum transacton.");
		clienttasks.clean();
	    Assert.assertTrue(!RemoteFileTasks.testExists(client, clienttasks.redhatRepoFile),"Expecting the redhat repo file '"+clienttasks.redhatRepoFile+"' to NOT exist after running clean.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID = {"RHEL6-36618", "RHEL7-51429"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier2")
	@Test(	description="verify Splice cert/key pair remains after subscription-manager clean",
			groups={"Tier2Tests","blockedByBug-1026501"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testSpliceCertsRemainAfterClean() {
		if (clienttasks.isPackageVersion("subscription-manager","<","1.10.14-1")) throw new SkipException("Installed package '"+clienttasks.installedPackageVersionMap.get("subscription-manager")+"' is blockedByBug https://bugzilla.redhat.com/show_bug.cgi?id=1026501 which is fixed in subscription-manager-1.10.14-1.");
		
		// Start fresh by registering...
		log.info("Start fresh by registering...");
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(String)null,null,null, null, true, false, null, null, null, null);

		// fake a splice consumer cert/key pair
		String consumerCertFile = clienttasks.consumerCertFile();
		String consumerKeyFile = clienttasks.consumerKeyFile();
		String consumerSpliceCertFile = clienttasks.consumerCertDir+"/Splice_identity.cert";
		String consumerSplicetKeyFile = clienttasks.consumerCertDir+"/Splice_identity.key";
		client.runCommandAndWait("cp -u "+consumerCertFile+" "+consumerSpliceCertFile);
		client.runCommandAndWait("cp -u "+consumerKeyFile+" "+consumerSplicetKeyFile);
		Assert.assertTrue(RemoteFileTasks.testExists(client, consumerSpliceCertFile), "Successfully created a fake '"+consumerSpliceCertFile+"'.");
		Assert.assertTrue(RemoteFileTasks.testExists(client, consumerSplicetKeyFile), "Successfully created a fake '"+consumerSplicetKeyFile+"'.");
		
		// clean
		clienttasks.clean();
		
		// assert the fake splice consumer cert/key pair was NOT deleted
		Assert.assertTrue(RemoteFileTasks.testExists(client, consumerSpliceCertFile), "After running clean, '"+consumerSpliceCertFile+"' should still exist.");
		Assert.assertTrue(RemoteFileTasks.testExists(client, consumerSplicetKeyFile), "After running clean, '"+consumerSplicetKeyFile+"' should still exist.");
		
		// cleanup splice files (not absolutely necessary)
		client.runCommandAndWait("rm -f "+consumerSpliceCertFile);
		client.runCommandAndWait("rm -f "+consumerSplicetKeyFile);
	}
	
	
	// Candidates for an automated Test:

	
	
	// Configuration methods ***********************************************************************
	
	String consumerId = null;
	@AfterClass(groups={"setup"})
	public void teardownAfterClass() {
		if (clienttasks!=null) {
			clienttasks.unregister_(null, null, null, null);
			if (consumerId!=null) {
				clienttasks.register_(sm_clientUsername, sm_clientPassword, null, null, null, null, consumerId, null, null, null, (String)null, null, null, null, Boolean.TRUE, false, null, null, null, null);
				clienttasks.unregister_(null, null, null, null);
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
