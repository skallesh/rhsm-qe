package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.ContentNamespace;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.ProductCert;
import com.redhat.qe.sm.data.Repo;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"ReposTests"})
public class ReposTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************

	
	@Test(	description="subscription-manager: subscribe to a pool and verify that the newly entitled content namespaces are represented in the repos list",
			enabled=true,
			groups={},
			dataProvider="getAvailableSubscriptionPoolsData")
	//@ImplementsNitrateTest(caseId=)
	public void ReposListReportsGrantedContentNamespacesAfterSubscribing_Test(SubscriptionPool pool){

		List<Repo> priorRepos = clienttasks.getCurrentlySubscribedRepos();
		
		File entitlementCertFile = clienttasks.subscribeToSubscriptionPool(pool);
		EntitlementCert entitlementCert = clienttasks.getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
		
		List<Repo> actualRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// assert that the new contentNamespaces from the entitlementCert are listed in repos
		int numNewRepos=0;
		for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
			Repo expectedRepo = new Repo(contentNamespace.name,contentNamespace.label,clienttasks.baseurl+contentNamespace.downloadUrl,contentNamespace.enabled.trim().equals("1")?true:false);
			if (clienttasks.areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace,currentProductCerts)) {
				Assert.assertTrue(actualRepos.contains(expectedRepo),"The newly entitled contentNamespace '"+contentNamespace+"' is represented in the subscription-manager repos --list.");
				
				if (!priorRepos.contains(expectedRepo)) numNewRepos++;	// also count the number of NEW contentNamespaces
				
			} else {
				Assert.assertFalse(actualRepos.contains(expectedRepo),"The newly entitled contentNamespace '"+contentNamespace+"' is NOT represented in the subscription-manager repos --list because it requires tags ("+contentNamespace.requiredTags+") that are not provided by the currently installed product certs.");
			}
		}
		
		// assert that the number of repos reported has increased by the number of contentNamespaces in the new entitlementCert (unless the 
		Assert.assertEquals(actualRepos.size(), priorRepos.size()+numNewRepos, "The number of entitled repos has increased by the number of NEW contentNamespaces ("+numNewRepos+") from the newly granted entitlementCert.");
	}
	
	
	// Configuration methods ***********************************************************************

	@BeforeClass(groups={"setup"})
	public void setupBeforeClass() {
		currentProductCerts = clienttasks.getCurrentProductCerts();
	}
	
	
	// Protected methods ***********************************************************************

	List<ProductCert> currentProductCerts=new ArrayList<ProductCert>();

	
	// Data Providers ***********************************************************************

}
