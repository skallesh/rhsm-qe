package com.redhat.qe.sm.cli.tests;

import java.io.File;

import org.testng.SkipException;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.Repo;
import com.redhat.qe.tools.RemoteFileTasks;

/**
 * @author jsefler
 * 
 * Red Hat Update Infrastructure
 * Reference: http://documentation-stage.bne.redhat.com/docs/en-US/Red_Hat_Update_Infrastructure/2.0/html/Installation_Guide/sect-Installation_Guide-Installation_Requirements-Package_Installation.html
 */
@Test(groups={"RHUITests","AcceptanceTests"})
public class RHUITests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="register to the stage/prod environment as a RHUI consumer type",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void RegisterRHUIConsumer_Test() {
		if (sm_rhuiUsername.equals("")) throw new SkipException("Skipping this test when no value was given for the RHUI Username");
		// register the RHUI consumer
		clienttasks.register(sm_rhuiUsername,sm_rhuiPassword,sm_rhuiOrg,null,ConsumerType.RHUI,null,null,null,(String)null,true,null,null,null, null);
		
	}
	
	@Test(	description="after registering to the stage/prod environment as a RHUI consumer, subscribe to the expected RHUI product subscription",
			groups={},
			dependsOnMethods={"RegisterRHUIConsumer_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ConsumeRHUISubscriptionProduct_Test() {
		// Subscribe to the RHUI subscription productId
		entitlementCertFile = clienttasks.subscribeToProductId(sm_rhuiSubscriptionProductId);
	}
	
	@Test(	description="download an expected RHUI iso from an expected repoUrl",
			groups={},
			dependsOnMethods={"ConsumeRHUISubscriptionProduct_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void DownloadRHUIISO_Test() {
		if (sm_rhuiDownloadIso.equals("")) throw new SkipException("Skipping this test when no value was given for the RHUI Download ISO");

		File downloadedIsoFile = new File("/tmp/"+sm_rhuiDownloadIso);
		RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+downloadedIsoFile, 0/*, stdoutRegex, stderrRegex*/);
	
		// find the repo for the isos
		Repo repoForIsos = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", sm_rhuiRepoIdForIsos, clienttasks.getCurrentlySubscribedRepos());
		Assert.assertNotNull(repoForIsos,"Found expected repoId for rhui isos after subscribe to '"+sm_rhuiSubscriptionProductId+"'.");
		String repoUrl = repoForIsos.repoUrl;
		repoUrl = repoUrl.replaceFirst("\\$releasever", clienttasks.releasever);
		repoUrl = repoUrl.replaceFirst("\\$basearch", clienttasks.arch);

		File entitlementKeyFile = clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(entitlementCertFile);
		// $ wget  --certificate=<Content Certificate>	https://cdn.redhat.com/content/dist/rhel/rhui/server/6/6Server/x86_64/rhui/2.0/iso/RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso
		// wget --no-check-certificate --certificate=/etc/pki/entitlement/7658526340059785943.pem --private-key=/etc/pki/entitlement/7658526340059785943-key.pem --output-document=/tmp/RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso -- https://cdn.redhat.com/content/dist/rhel/rhui/server/6/6Server/x86_64/rhui/2.0/iso/RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso
		RemoteFileTasks.runCommandAndAssert(client, "wget --no-check-certificate --certificate="+entitlementCertFile+" --private-key="+entitlementKeyFile+" --output-document="+downloadedIsoFile+" -- "+repoUrl+"/"+sm_rhuiDownloadIso, 0/*, stdoutRegex, stderrRegex*/);
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, downloadedIsoFile.getPath()), 1,"Expected RHUI Download ISO was downloaded.");
	}

	
	
	// Candidates for an automated Test:
	
	
	// Configuration methods ***********************************************************************
	
	
	
	
	// Protected methods ***********************************************************************
	File entitlementCertFile = null;


	
	// Data Providers ***********************************************************************

}
