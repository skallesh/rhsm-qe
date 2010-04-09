package com.redhat.qe.sm.tests;

import java.util.ArrayList;
import java.util.HashMap;

import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.tasks.Pool;
import com.redhat.qe.tools.RemoteFileTasks;

public class Subscribe extends Register{
	
	@Test(description="subscription-manager-cli: subscribe client to an entitlement using product ID",
			dependsOnMethods="SubscribeToValidSubscriptionsByPoolID_Test",
			groups={"sm"})
	@ImplementsTCMS(id="41680,41899")
	public void SubscribeToValidSubscriptionsByProductID_Test(){
		this.unsubscribeFromAllSubscriptions(false);
		this.subscribeToAllSubscriptions(false);
	}
	
	@Test(description="subscription-manager-cli: subscribe client to an entitlement using pool ID",
			dependsOnMethods="ValidRegistration_Test",
			groups={"sm"})
	@ImplementsTCMS(id="41686,41899")
	public void SubscribeToValidSubscriptionsByPoolID_Test(){
		this.unsubscribeFromAllSubscriptions(true);
		this.subscribeToAllSubscriptions(true);
	}
	
	@Test(description="subscription-manager-cli: subscribe client to an entitlement using registration token",
			dependsOnMethods="ValidRegistration_Test",
			groups={"sm"})
	@ImplementsTCMS(id="41681")
	public void SubscribeToRegToken_Test(){
		this.unsubscribeFromAllSubscriptions(true);
		this.subscribeToRegToken(regtoken);
	}
	
	@Test(description="Subscribed for Already subscribed Entitlement.",
			dependsOnMethods="ValidRegistration_Test",
			groups={"sm"})
	@ImplementsTCMS(id="41897")
	public void SubscribeAndSubscribeAgain_Test(){
		this.unsubscribeFromAllSubscriptions(true);
		this.refreshSubscriptions();
		for(Pool sub:this.availSubscriptions){
			this.subscribeToPool(sub, false);
			RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,
					RHSM_LOC+"subscribe --product="+sub.productId);
		}
	}
	
	@Test(description="subscription-manager Yum plugin: enable/disable",
			dependsOnMethods="SubscribeToValidSubscriptionsByProductID_Test",
			groups={"sm"})
	@ImplementsTCMS(id="41696")
	public void EnableYumRepoAndVerifyContentAvailable_Test(){
		this.adjustRHSMYumRepo(true);
		for(Pool sub:this.consumedSubscriptions){
			ArrayList<String> repos = this.getYumRepolist();
			Assert.assertTrue(repos.contains(sub.productId),
					"Yum reports product subscribed to repo: " + sub.productId);
		}	
	}
	
	@Test(description="subscription-manager Yum plugin: ensure content can be downloaded/installed",
			dependsOnMethods="EnableYumRepoAndVerifyContentAvailable_Test",
			groups={"sm"})
	@ImplementsTCMS(id="41695")
	public void InstallPackageFromRHSMYumRepo_Test(){
		HashMap<String, String[]> pkgList = this.getPackagesCorrespondingToSubscribedRepos();
		for(Pool sub:this.consumedSubscriptions){
			String pkg = pkgList.get(sub.productId)[0];
			RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner,
					"yum install -y "+pkg,Long.valueOf(10*60000));
			RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner,
					"rpm -q "+pkg, Long.valueOf(2*60000));
		}
	}
	
	@Test(description="subscription-manager Yum plugin: enable/disable",
			dependsOnMethods="EnableYumRepoAndVerifyContentAvailable_Test",
			groups={"sm"})
	@ImplementsTCMS(id="41696")
	public void DisableYumRepoAndVerifyContentNotAvailable_Test(){
		this.adjustRHSMYumRepo(false);
		for(Pool sub:this.availSubscriptions)
			for(String repo:this.getYumRepolist())
				if(repo.contains(sub.productId))
					Assert.fail("After unsubscribe, Yum still has access to repo: "+repo);
	}
	
	@Test(description="rhsmcertd: change certFrequency",
			dependsOnMethods="SubscribeToValidSubscriptionsByProductID_Test",
			groups={"sm"})
	@ImplementsTCMS(id="41692")
	public void certFrequency_Test(){
		this.changeCertFrequency("60");
		this.sleep(70*1000);
		Assert.assertEquals(RemoteFileTasks.grepFile(sshCommandRunner,
				rhsmcertdLogFile,
				"certificates updated"),
				0,
				"rhsmcertd reports that certificates have been updated at new interval");
	}
	
	@Test(description="rhsmcertd: ensure certificates synchronize",
			dependsOnMethods="certFrequency_Test",
			groups={"sm"})
	@ImplementsTCMS(id="41694")
	public void refreshCerts_Test(){
		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/entitlement/*");
		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/entitlement/product/*");
		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/product/*");
		sshCommandRunner.runCommandAndWait("rm -f "+rhsmcertdLogFile);
		
		this.sleep(70*1000);
		
		Assert.assertEquals(RemoteFileTasks.grepFile(sshCommandRunner,
				rhsmcertdLogFile,
				"certificates updated"),
				0,
				"rhsmcertd reports that certificates have been updated");
		
		//verify that PEM files are present in all certificate directories
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, 
				"ls /etc/pki/entitlement",
				0,
				"pem", 
				null);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, 
				"ls /etc/pki/entitlement/product", 
				0,
				"pem", 
				null);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, 
				"ls /etc/pki/product", 
				0,
				"pem", 
				null);
	}
}
