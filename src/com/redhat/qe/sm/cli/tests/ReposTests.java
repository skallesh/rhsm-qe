package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.List;

import org.json.JSONException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.ContentNamespace;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.Repo;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"ReposTests"})
public class ReposTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************

	
	@Test(	description="subscription-manager: repos",
			enabled=true,
			groups={},
			dataProvider="getAvailableSubscriptionPoolsData")
	//@ImplementsNitrateTest(caseId=)
	public void ReposList_Test(SubscriptionPool pool){

		List<Repo> priorRepos = clienttasks.getCurrentlySubscribedRepos();
		
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPoolUsingProductId(pool);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		
		List<Repo> actualRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// assert that the new contentSets from the entitlementCert are listed in repos
		int numNewRepos=0;
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Repo expectedRepo = new Repo(contentNamespace.name,contentNamespace.label,clienttasks.baseurl+contentNamespace.downloadUrl,contentNamespace.enabled.trim().equals("1")?true:false);
			Assert.assertContains(actualRepos, expectedRepo);
			
			// also count the number of NEW contentNamespaces
			if (!priorRepos.contains(expectedRepo)) numNewRepos++;
		}
		
		
		// assert that the number of repos reported has increased by the number of contentNamespaces in the new entitlementCert (unless the 
		Assert.assertEquals(actualRepos.size(), priorRepos.size()+numNewRepos, "The number of entitled repos has increased by the number of NEW contentNamespaces ("+numNewRepos+") from the newly granted entitlementCert.");
		
	}
	
	
	// Configuration methods ***********************************************************************
	
	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
