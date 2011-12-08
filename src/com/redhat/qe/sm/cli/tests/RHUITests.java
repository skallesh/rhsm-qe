package com.redhat.qe.sm.cli.tests;

import java.io.File;

import org.testng.SkipException;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.ConsumerType;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.ContentNamespace;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.Repo;
import com.redhat.qe.tools.RemoteFileTasks;

/**
 * @author jsefler
 * 
 * Red Hat Update Infrastructure
 * Reference:
 * RHEL6 http://documentation-stage.bne.redhat.com/docs/en-US/Red_Hat_Update_Infrastructure/2.0/html/Installation_Guide/sect-Installation_Guide-Installation_Requirements-Package_Installation.html
 * RHEL5 http://docs.redhat.com/docs/en-US/Red_Hat_Update_Infrastructure/1.2/html/Installation_Guide/chap-Installation_Guide-Installation.html
 *		https://cdn.redhat.com/content/dist/rhel/rhui/server/5Server/i386/rhui/1.2/iso/rhel-5.5-rhui-1.2-i386.iso
 *		https://cdn.redhat.com/content/dist/rhel/rhui/server/5Server/x86_64/rhui/1.2/iso/rhel-5.5-rhui-1.2-x86_64.iso
 * 
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
	
	@Test(	description="download an expected RHUI iso from an expected yum repoUrl",
			groups={},
			dependsOnMethods={"ConsumeRHUISubscriptionProduct_Test"},
			enabled=false)
	//@ImplementsNitrateTest(caseId=)
	public void DownloadRHUIISOFromYumRepo_Test() {
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
	
	@Test(	description="download an expected RHUI iso from an expected file repoUrl",
			groups={},
			dependsOnMethods={"ConsumeRHUISubscriptionProduct_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void DownloadRHUIISOFromFileRepo_Test() {
		if (sm_rhuiDownloadIso.equals("")) throw new SkipException("Skipping this test when no value was given for the RHUI Download ISO");

		File downloadedIsoFile = new File("/tmp/"+sm_rhuiDownloadIso);
		RemoteFileTasks.runCommandAndAssert(client, "rm -rf "+downloadedIsoFile, 0/*, stdoutRegex, stderrRegex*/);
	
		// find the repo for the isos
		ContentNamespace contentNamespaceForIso = null;
		for (EntitlementCert entitlementCert : clienttasks.getCurrentEntitlementCerts()) {
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				if (contentNamespace.label.equals(sm_rhuiRepoIdForIsos)) {
					contentNamespaceForIso = contentNamespace;
					break;
				}
			}
		}
		Assert.assertNotNull(contentNamespaceForIso,"Found expected ContentNamespace to repoId '"+sm_rhuiRepoIdForIsos+"' for rhui isos after subscribe to '"+sm_rhuiSubscriptionProductId+"'.");
		String repoUrl = clienttasks.baseurl+contentNamespaceForIso.downloadUrl;
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