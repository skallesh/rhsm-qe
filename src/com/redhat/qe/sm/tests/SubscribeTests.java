package com.redhat.qe.sm.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.sm.abstractions.EntitlementCert;
import com.redhat.qe.sm.abstractions.Pool;
import com.redhat.qe.sm.abstractions.ProductID;
import com.redhat.qe.sm.base.SubscriptionManagerTestScript;
import com.redhat.qe.tools.RemoteFileTasks;

public class SubscribeTests extends SubscriptionManagerTestScript{
	
	@Test(description="subscription-manager-cli: subscribe client to an entitlement using product ID",
			dependsOnGroups={"sm_stage3"},
			groups={"sm_stage4", "blockedByBug-584137", "not_implemented"})
	@ImplementsTCMS(id="41680,41899")
	public void SubscribeToValidSubscriptionsByProductID_Test(){
		sm.unsubscribeFromAllProductIDs();
		sm.subscribeToAllPools(false);
	}
	
	
	@Test(description="subscription-manager-cli: subscribe client to an entitlement using product ID",
			dependsOnGroups={"sm_stage3"},
			groups={"sm_stage4", "blockedByBug-584137", "not_implemented"})
	public void SubscribeToASingleEntitlementByProductID_Test(){
		sm.unsubscribeFromAllProductIDs();
		Pool MCT0696 = new Pool("MCT0696", "biteme");
		MCT0696.addProductID("Red Hat Directory Server");
		sm.subscribeToPool(MCT0696, false);
		//this.refreshSubscriptions();
		for (ProductID pid:MCT0696.associatedProductIDs){
			Assert.assertTrue(sm.getConsumedProductIDs().contains(pid),
					"ProductID '"+pid.productId+"' consumed from Pool '"+MCT0696.poolName+"'");
		}
	}
	
	
	@Test(description="subscription-manager-cli: subscribe client to an entitlement using pool ID",
			dependsOnGroups={"sm_stage3"},
			groups={"sm_stage4", "blockedByBug-584137"})
	@ImplementsTCMS(id="41686,41899")
	public void SubscribeToValidSubscriptionsByPoolID_Test(){
		sm.unsubscribeFromAllProductIDs();
		sm.subscribeToAllPools(true);
	}
	
	
	@Test(description="subscription-manager-cli: subscribe client to an entitlement using registration token",
			dependsOnGroups={"sm_stage8"},
			groups={"sm_stage9", "blockedByBug-584137", "not_implemented"})
	@ImplementsTCMS(id="41681")
	public void SubscribeToRegToken_Test(){
		sm.unsubscribeFromAllProductIDs();
		sm.subscribeToRegToken(regtoken);
	}
	
	
	@Test(description="Subscribed for Already subscribed Entitlement.",
			dependsOnGroups={"sm_stage3"},
			groups={"sm_stage4", "blockedByBug-584137", "not_implemented"})
	@ImplementsTCMS(id="41897")
	public void SubscribeAndSubscribeAgain_Test(){
		sm.unsubscribeFromAllProductIDs();
		sm.refreshSubscriptions();
		ArrayList<Pool> availablePools = (ArrayList<Pool>)sm.getAvailPools().clone();
		for(Pool sub : availablePools){
			sm.subscribeToPool(sub, false);
			sm.subscribeToProduct(sub.poolName);
		}
	}
	
	
	@Test(description="subscription-manager Yum plugin: enable/disable",
			dependsOnGroups={"sm_stage5"},
			groups={"sm_stage6"})
	@ImplementsTCMS(id="41696")
	public void EnableYumRepoAndVerifyContentAvailable_Test(){
		sm.subscribeToAllPools(false);
		this.adjustRHSMYumRepo(true);
		/*for(ProductID sub:this.consumedProductIDs){
			ArrayList<String> repos = this.getYumRepolist();
			Assert.assertTrue(repos.contains(sub.productId),
					"Yum reports product subscribed to repo: " + sub.productId);
		}*/
	}
	
	
	@Test(description="subscription-manager Yum plugin: ensure ...",
	        dependsOnGroups={"sm_stage6"},
	        groups={"sm_stage7"})
	@ImplementsTCMS(id="47578")
	public void VerifyReposAvailableForEnabledContent(){
	    ArrayList<String> repos = this.getYumRepolist();
	    
	    for (EntitlementCert cert:sm.getCurrentEntitlementCerts()){
	    	if(cert.enabled.contains("1"))
	    		Assert.assertTrue(repos.contains(cert.label),
	    				"Yum reports enabled content subscribed to repo: " + cert.label);
	    	else
	    		Assert.assertFalse(repos.contains(cert.label),
	    				"Yum reports enabled content subscribed to repo: " + cert.label);
	    }
	}
	
	
	@Test(description="subscription-manager Yum plugin: ensure content can be downloaded/installed",
			dependsOnGroups={"sm_stage6"},
			groups={"sm_stage7", "not_implemented"})
	@ImplementsTCMS(id="41695")
	public void InstallPackageFromRHSMYumRepo_Test(){
		HashMap<String, String[]> pkgList = sm.getPackagesCorrespondingToSubscribedRepos();
		for(ProductID sub:sm.getConsumedProductIDs()){
			String pkg = pkgList.get(sub.productId)[0];
			log.info("timeout of two minutes for next three commands");
			RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner,
					"yum repolist");
			RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner,
					"yum install -y "+pkg);
			RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner,
					"rpm -q "+pkg);
		}
	}
	
	
	@Test(description="subscription-manager Yum plugin: enable/disable",
			dependsOnGroups={"sm_stage7"},
			groups={"sm_stage8", "not_implemented"})
	@ImplementsTCMS(id="41696")
	public void DisableYumRepoAndVerifyContentNotAvailable_Test(){
		this.adjustRHSMYumRepo(false);
		for(Pool sub:sm.getAvailPools())
			for(String repo:this.getYumRepolist())
				if(repo.contains(sub.poolName))
					Assert.fail("After unsubscribe, Yum still has access to repo: "+repo);
	}
	
	
	@Test(description="rhsmcertd: change certFrequency",
			dependsOnGroups={"sm_stage3"},
			groups={"sm_stage4"})
	@ImplementsTCMS(id="41692")
	public void certFrequency_Test(){
		this.changeCertFrequency("1");
		this.sleep(70*1000);
		Assert.assertEquals(RemoteFileTasks.grepFile(sshCommandRunner,
				rhsmcertdLogFile,
				"certificates updated"),
				0,
				"rhsmcertd reports that certificates have been updated at new interval");
	}
	
	
	@Test(description="rhsmcertd: ensure certificates synchronize",
			dependsOnGroups={"sm_stage3"},
			groups={"sm_stage4"})
	@ImplementsTCMS(id="41694")
	public void refreshCerts_Test(){
		sm.subscribeToAllPools(false);
		//SubscribeToASingleEntitlementByProductID_Test();
		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/entitlement/*");
		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/entitlement/product/*");
		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/product/*");
		sshCommandRunner.runCommandAndWait("cat /dev/null > "+rhsmcertdLogFile);
		//sshCommandRunner.runCommandAndWait("rm -f "+rhsmcertdLogFile);
		//sshCommandRunner.runCommandAndWait("/etc/init.d/rhsmcertd restart");
		this.sleep(70*1000);
		
		Assert.assertEquals(RemoteFileTasks.grepFile(sshCommandRunner,
				rhsmcertdLogFile,
				"certificates updated"),
				0,
				"rhsmcertd reports that certificates have been updated");
		
		//verify that PEM files are present in all certificate directories
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, 
				"ls /etc/pki/entitlement | grep pem",
				0,
				"pem", 
				null);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, 
				"ls /etc/pki/entitlement/product | grep pem", 
				0,
				"pem", 
				null);
		// this directory will only be populated if you upload ur own license, not while working w/ candlepin
		/*RemoteFileTasks.runCommandAndAssert(sshCommandRunner, 
				"ls /etc/pki/product", 
				0,
				"pem", 
				null);*/
	}
	
	
	
	// Protected Methods ***********************************************************************

	protected ArrayList<String> getYumRepolist(){
		ArrayList<String> repos = new ArrayList<String>();
		sshCommandRunner.runCommandAndWait("killall -9 yum");
		
		sshCommandRunner.runCommandAndWait("yum repolist");
		String[] availRepos = sshCommandRunner.getStdout().split("\\n");
		
		int repolistStartLn = 0;
		int repolistEndLn = 0;
		
		for(int i=0;i<availRepos.length;i++)
			if (availRepos[i].contains("repo id"))
				repolistStartLn = i + 1;
			else if (availRepos[i].contains("repolist:"))
				repolistEndLn = i;
		
		for(int i=repolistStartLn;i<repolistEndLn;i++)
			repos.add(availRepos[i].split(" ")[0]);
		
		return repos;
	}
	
	protected void adjustRHSMYumRepo(boolean enabled){
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, 
						rhsmYumRepoFile, 
						"^enabled=.*$", 
						"enabled="+(enabled?'1':'0')),
						0,
						"Adjusted RHSM Yum Repo config file, enabled="+(enabled?'1':'0')
				);
	}
}
