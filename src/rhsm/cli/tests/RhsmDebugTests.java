package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.testng.TestNGUtils;
import rhsm.base.AccessType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"debugTesting","RhsmDebugTests"})
public class RhsmDebugTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************
	
	@Test(	description="attempt to call rhsm-debug system without being registered",
			groups={},
			enabled=false)	// already covered by a dataProvider row in GeneralTests.getNegativeFunctionalityDataAsListOfLists()
	//@ImplementsNitrateTest(caseId=)
	public void RhsmDebugSystemWithoutBeingRegistered_Test() {
		
		// make sure we are not registered
		clienttasks.unregister_(null, null, null);
		
		// attempt to run rhsm-debug system without being registered
		String rhsmDebugSystemCommand = clienttasks.rhsmDebugSystemCommand(null, null, null, null);
		SSHCommandResult result = client.runCommandAndWait(rhsmDebugSystemCommand);
		
		// assert results
		Assert.assertEquals(result.getExitCode(), new Integer(255), "The exit code from an attempt to run '"+rhsmDebugSystemCommand+"' without being registered.");
		Assert.assertEquals(result.getStdout().trim(), "This system is not yet registered. Try 'subscription-manager register --help' for more information.", "The stdout from an attempt to run '"+rhsmDebugSystemCommand+"' without being registered.");
		Assert.assertEquals(result.getStderr().trim(), "", "The stderr from an attempt to run '"+rhsmDebugSystemCommand+"' without being registered.");

	}
	
	
	@Test(	description="after registering call rhsm-debug system",
			groups={/*"blockedByBug-1040338"*/},
			enabled=true)
			//@ImplementsNitrateTest(caseId=)
	public void RhsmDebugSystem_Test() {
		
		// register
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, true, false, null, null, null);
		
		// attempt to run rhsm-debug system without being registered
		String rhsmDebugSystemCommand = clienttasks.rhsmDebugSystemCommand(null, null, null, null);
		SSHCommandResult result = client.runCommandAndWait(rhsmDebugSystemCommand);
		
		// assert results
		Assert.assertEquals(result.getExitCode(), new Integer(0), "The exit code from an attempt to run '"+rhsmDebugSystemCommand+"'.");
//FIXME	after 1040338	Assert.assertEquals(result.getStdout().trim(), "Successfully created: ", "The stdout from an attempt to run '"+rhsmDebugSystemCommand+"'.");
		Assert.assertEquals(result.getStderr().trim(), "", "The stderr from an attempt to run '"+rhsmDebugSystemCommand+"'.");
		
		// get the rhsmDebugFile
		File rhsmDebugSystemFile = new File("/tmp/system-debug-20131213-940752.zip");
		
		// assert the existence of the rhsmDebugFile
		Assert.assertTrue(RemoteFileTasks.testExists(client, rhsmDebugSystemFile.getPath()), "The expected rhsm-debug system file '"+rhsmDebugSystemFile+"' exists.");
		
//		// get a zip file listing of the rhsmDebugFile
//		SSHCommandResult rhsmDebugSystemFileListResult = client.runCommandAndWait("unzip -l "+rhsmDebugSystemFile);
		
		// unzip the rhsmDebugFile
		String unzipDir = "/tmp/rhsmDebugSystemTestDir";
		SSHCommandResult rhsmDebugSystemFileUnzipResult = client.runCommandAndWait("rm -rf "+unzipDir+" && unzip -d "+unzipDir+" "+rhsmDebugSystemFile);
		Assert.assertEquals(rhsmDebugSystemFileUnzipResult.getExitCode(), new Integer(0), "The exit code from unzipping '"+rhsmDebugSystemFile+"'.");

		
		// assert the presence of expected files...
		//	[root@jsefler-7 ~]# rm -rf /tmp/rhsmDebugSystemTestDir && unzip -d /tmp/rhsmDebugSystemTestDir /tmp/system-debug-20131211-948377.zip 
		//	Archive:  /tmp/system-debug-20131211-948377.zip
		//	  inflating: /tmp/rhsmDebugSystemTestDir/consumer.json  
		//	  inflating: /tmp/rhsmDebugSystemTestDir/compliance.json  
		//	  inflating: /tmp/rhsmDebugSystemTestDir/entitlements.json  
		//	  inflating: /tmp/rhsmDebugSystemTestDir/pools.json  
		//	  inflating: /tmp/rhsmDebugSystemTestDir/subscriptions.json  
		//	  inflating: /tmp/rhsmDebugSystemTestDir/etc/rhsm/rhsm.conf  
		//	   creating: /tmp/rhsmDebugSystemTestDir/var/log/rhsm/
		//	  inflating: /tmp/rhsmDebugSystemTestDir/var/log/rhsm/rhsmcertd.log  
		//	  inflating: /tmp/rhsmDebugSystemTestDir/var/log/rhsm/rhsm.log  
		//	   creating: /tmp/rhsmDebugSystemTestDir/etc/pki/product/
		//	  inflating: /tmp/rhsmDebugSystemTestDir/etc/pki/product/230.pem  
		//	   creating: /tmp/rhsmDebugSystemTestDir/etc/pki/entitlement/
		//	   creating: /tmp/rhsmDebugSystemTestDir/etc/pki/consumer/
		//	  inflating: /tmp/rhsmDebugSystemTestDir/etc/pki/consumer/key.pem  
		//	  inflating: /tmp/rhsmDebugSystemTestDir/etc/pki/consumer/cert.pem  
		//	[root@jsefler-7 ~]#
		String expectedFile;
		expectedFile = unzipDir+"/consumer.json";		Assert.assertTrue(!getSubstringMatches(rhsmDebugSystemFileUnzipResult.getStdout(),unzipDir+"/consumer.json"+"\\s*$").isEmpty(),"The rhsmDebugSystemFile '"+rhsmDebugSystemFile+"' appears to contain expected file '"+expectedFile+"'.");
		expectedFile = unzipDir+"/entitlements.json";	Assert.assertTrue(!getSubstringMatches(rhsmDebugSystemFileUnzipResult.getStdout(),unzipDir+"/consumer.json"+"\\s*$").isEmpty(),"The rhsmDebugSystemFile '"+rhsmDebugSystemFile+"' appears to contain expected file '"+expectedFile+"'.");
		
//		// without any subscriptions attached...
//		SSHCommandResult defaultResult = clienttasks.repo_override_(null,null,(String)null,(String)null,null,null,null,null);
//		SSHCommandResult listResult = clienttasks.repo_override_(true,null,(String)null,(String)null,null,null,null,null);
//		Assert.assertEquals(listResult.toString().trim(), defaultResult.toString().trim(), "The result from running module repo-override without any options should default to the --list result (with no subscriptions attached and no overrides)");
//		// valid prior to bug 1034396	Assert.assertEquals(listResult.getStdout().trim(), "This system does not have any subscriptions.", "Stdout from repo-override --list without any subscriptions attached and no overrides.");
//		Assert.assertEquals(listResult.getStdout().trim(), "This system does not have any content overrides applied to it.", "Stdout from repo-override --list without any subscriptions attached and no overrides.");
//		Assert.assertEquals(listResult.getStderr().trim(), "", "Stderr from repo-override --list without any subscriptions attached and no overrides.");
//		Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(/*1*/0), "ExitCode from repo-override --list without any subscriptions attached and no overrides.");
		
//		// subscribe to a random pool (so as to consume an entitlement)
//		List<SubscriptionPool> pools = clienttasks.getCurrentlyAvailableSubscriptionPools();
//		if (pools.isEmpty()) throw new SkipException("Cannot randomly pick a pool for subscribing when there are no available pools for testing."); 
//		SubscriptionPool pool = pools.get(randomGenerator.nextInt(pools.size())); // randomly pick a pool
//		clienttasks.subscribeToSubscriptionPool(pool);
//		
//		// with a subscription attached...
//		defaultResult = clienttasks.repo_override_(null,null,(String)null,(String)null,null,null,null,null);
//		listResult = clienttasks.repo_override_(true,null,(String)null,(String)null,null,null,null,null);
//		Assert.assertEquals(listResult.toString().trim(), defaultResult.toString().trim(), "The result from running module repo-override without any options should default to the --list result (with subscriptions attached)");
//		Assert.assertEquals(listResult.getStdout().trim(), "This system does not have any content overrides applied to it.", "Stdout from repo-override --list without any overrides.");
//		Assert.assertEquals(listResult.getStderr().trim(), "", "Stderr from repo-override --list without any overrides.");
//		Assert.assertEquals(listResult.getExitCode(), Integer.valueOf(0), "ExitCode from repo-override --list without any overrides.");
	}
	
	
	
	// Candidates for an automated Test:
	
	
	
	// Configuration methods ***********************************************************************
	
	
	
	// Protected methods ***********************************************************************
	
	
	
	// Data Providers ***********************************************************************
	
	
	
}
