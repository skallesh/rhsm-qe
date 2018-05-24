package rhsm.cli.tests;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;

import rhsm.base.CandlepinType;
import rhsm.base.ConsumerType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.CandlepinTasks;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.Repo;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author jsefler
 * 
 * Red Hat Update Infrastructure
 * Reference:
 * RHEL6 http://documentation-stage.bne.redhat.com/docs/en-US/Red_Hat_Update_Infrastructure/2.0/html/Installation_Guide/sect-Installation_Guide-Installation_Requirements-Package_Installation.html
 *      https://cdn.rcm-qa.redhat.com/content/dist/rhel/rhui/server/6/6Server/x86_64/rhui/2.0/iso/RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso
 * RHEL5 http://docs.redhat.com/docs/en-US/Red_Hat_Update_Infrastructure/1.2/html/Installation_Guide/chap-Installation_Guide-Installation.html
 *		https://cdn.redhat.com/content/dist/rhel/rhui/server/5Server/i386/rhui/1.2/iso/rhel-5.5-rhui-1.2-i386.iso
 *		https://cdn.redhat.com/content/dist/rhel/rhui/server/5Server/x86_64/rhui/1.2/iso/rhel-5.5-rhui-1.2-x86_64.iso
 * 
 * To see the content available for download (register --type=RHUI and attach MCT2042):
 * RHEL6
[root@storm ~]# curl --cert /etc/pki/entitlement/1126131111567895623.pem --key /etc/pki/entitlement/1126131111567895623-key.pem -k https://cdn.rcm-qa.redhat.com/content/dist/rhel/rhui/server/6/6Server/x86_64/rhui/2.0/iso
RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso
RHEL-6.2-RHUI-2.0.3-20120322.0-Server-x86_64-DVD1.iso
RHEL-6.2-RHUI-2.0.2-20120309.0-Server-x86_64-DVD1.iso
RHEL-6.2-RHUI-2.0.3-20120409.0-Server-x86_64-DVD1.iso
RHEL-6.2-RHUI-2.0.2-20120309.0-Server-x86_64-DVD1.iso.sha1sum
RHEL-6.2-RHUI-2.0.2-20120309.0-Server-x86_64-DVD1.iso.sha256sum
RHEL-6.2-RHUI-2.0.3-20120322.0-Server-x86_64-DVD1.iso.sha1sum
RHEL-6.2-RHUI-2.0.3-20120322.0-Server-x86_64-DVD1.iso.sha256sum
RHEL-6.2-RHUI-2.0.3-20120416.0-Server-x86_64-DVD1.iso
RHEL-6.2-RHUI-2.0.3-20120409.0-Server-x86_64-DVD1.iso.sha1sum
RHEL-6.2-RHUI-2.0.3-20120409.0-Server-x86_64-DVD1.iso.sha256sum
RHEL-6.2-RHUI-2.0.3-20120416.0-Server-x86_64-DVD1.iso.sha1sum
RHEL-6.2-RHUI-2.0.3-20120416.0-Server-x86_64-DVD1.iso.sha256sum
[root@storm ~]# curl --cert /etc/pki/entitlement/1126131111567895623.pem --key /etc/pki/entitlement/1126131111567895623-key.pem -k https://cdn.rcm-qa.redhat.com/content/dist/rhel/rhui/server/6/6Server/i386/rhui/2.0/iso
[root@storm ~]# 


[root@jsefler-6 ~]# curl --stderr /dev/null --cert /etc/pki/entitlement/1011784678244847405.pem --key /etc/pki/entitlement/1011784678244847405-key.pem -k https://cdn.redhat.com/content/dist/rhel/rhui/server/6/6Server/x86_64/rhui/2/iso | grep RHUI
<IMG SRC="/icons/generic.gif" ALT="[FILE]"> <A HREF="iso/RHEL-6-RHUI-2-LATEST-Server-x86_64-DVD.iso">RHEL-6-RHUI-2-LATEST-Server-x8..&gt;</A> 21-Oct-2014 19:21  34.0M  
<IMG SRC="/icons/generic.gif" ALT="[FILE]"> <A HREF="iso/RHEL-6.1-RHUI-2.0-20110727.2-Server-x86_64-DVD1.iso">RHEL-6.1-RHUI-2.0-20110727.2-S..&gt;</A> 29-Jul-2011 14:09  61.3M  
<IMG SRC="/icons/generic.gif" ALT="[FILE]"> <A HREF="iso/RHEL-6.1-RHUI-2.0.1-20111027.1-Server-x86_64-DVD1.iso">RHEL-6.1-RHUI-2.0.1-20111027.1..&gt;</A> 10-Jan-2012 16:29  41.1M  
<IMG SRC="/icons/generic.gif" ALT="[FILE]"> <A HREF="iso/RHEL-6.2-RHUI-2.0.2-20120309.0-Server-x86_64-DVD1.iso">RHEL-6.2-RHUI-2.0.2-20120309.0..&gt;</A> 09-Mar-2012 18:28  33.7M  
<IMG SRC="/icons/generic.gif" ALT="[FILE]"> <A HREF="iso/RHEL-6.2-RHUI-2.0.3-20120416.0-Server-x86_64-DVD1.iso">RHEL-6.2-RHUI-2.0.3-20120416.0..&gt;</A> 01-May-2012 18:32  33.7M  
<IMG SRC="/icons/generic.gif" ALT="[FILE]"> <A HREF="iso/RHEL-6.3-RHUI-2.1-20120827.0-Server-x86_64-DVD1.iso">RHEL-6.3-RHUI-2.1-20120827.0-S..&gt;</A> 23-Apr-2013 14:31  33.9M  
<IMG SRC="/icons/generic.gif" ALT="[FILE]"> <A HREF="iso/RHEL-6.3-RHUI-2.1.1-20130219.0-Server-x86_64-DVD1.iso">RHEL-6.3-RHUI-2.1.1-20130219.0..&gt;</A> 19-Feb-2013 18:56  33.9M  
<IMG SRC="/icons/generic.gif" ALT="[FILE]"> <A HREF="iso/RHEL-6.4-RHUI-2.1.2-20130417.0-Server-x86_64-DVD1.iso">RHEL-6.4-RHUI-2.1.2-20130417.0..&gt;</A> 17-Apr-2013 20:41  33.9M  
<IMG SRC="/icons/generic.gif" ALT="[FILE]"> <A HREF="iso/RHEL-6.4-RHUI-2.1.3-20131212.0-Server-x86_64-DVD1.iso">RHEL-6.4-RHUI-2.1.3-20131212.0..&gt;</A> 12-Dec-2013 15:31  34.1M  
<IMG SRC="/icons/generic.gif" ALT="[FILE]"> <A HREF="iso/RHEL-6.5-RHUI-2.1.3.1-20140912.1-Server-x86_64-DVD1.iso">RHEL-6.5-RHUI-2.1.3.1-20140912..&gt;</A> 12-Sep-2014 17:44  34.0M  
<IMG SRC="/icons/generic.gif" ALT="[FILE]"> <A HREF="iso/RHEL-6.6-RHUI-2.1.3.2-20141021.3-Server-x86_64-DVD1.iso">RHEL-6.6-RHUI-2.1.3.2-20141021..&gt;</A> 21-Oct-2014 19:21  34.0M  

 */
@Test(groups={"RHUITests"})
public class RHUITests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-22224", "RHEL7-55199"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="register to the stage/prod environment as a RHUI consumer type",
			groups={"Tier1Tests", "blockedByBug-1496550", "blockedByBug-1554482"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterRHUIConsumer() throws JSONException, Exception {
		SSHCommandResult result;
		
		// register a RHUI consumer type
		if (sm_serverType.equals(CandlepinType.standalone)) {
			result = clienttasks.register_(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,ConsumerType.RHUI,null,null,null,null,null,(String)null,null,null, null, true, null, null, null, null, null);
			String expectedStderr = String.format("Unit type '%s' could not be found.", ConsumerType.RHUI);
			if (SubscriptionManagerTasks.isVersion(servertasks.statusVersion, ">=", "2.3.5-1")) {	// commit f87515e457c8b74cfaeaf9c0e47f019c241e8355 Changed Consumer.type to Consumer.typeId
				expectedStderr = String.format("Invalid unit type: %s",ConsumerType.RHUI);
			}
			if (clienttasks.isPackageVersion("subscription-manager",">=","1.21.2-1")) {	// post commit 630e1a2eb06e6bfacac669ce11f38e228c907ea9 1507030: RestlibExceptions should show they originate server-side
				expectedStderr = "HTTP error (400 - Bad Request): "+expectedStderr;
			}
			Assert.assertEquals(result.getStderr().trim(), expectedStderr);
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(70));
			throw new SkipException("On a candlpin server of type '"+sm_serverType+"': "+result.getStderr().trim());
		} else
		result = clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,ConsumerType.RHUI,null,null,null,null,null,(String)null,null,null, null, true, null, null, null, null, null);
		
		// assert the RHUI consumer type
		String consumerId = clienttasks.getCurrentConsumerId(result);
		String path = "/consumers/"+consumerId+"?include=type";
		JSONObject jsonConsumerType= new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_serverAdminUsername, sm_serverAdminPassword, sm_serverUrl, path));
		//	{
		//	    "type": {
		//	        "id": "0",
		//	        "label": "RHUI",
		//	        "manifest": false
		//	    }
		//	}
		String actualType = jsonConsumerType.getJSONObject("type").getString("label");
		Assert.assertEquals(actualType, ConsumerType.RHUI.toString(), "Consumer type.label returned from Candlepin API for GET on '"+path+"' after registering with --type='"+ConsumerType.RHUI+"'");
	}

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-22225", "RHEL7-55200"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="after registering to the stage/prod environment as a RHUI consumer, subscribe to the expected RHUI product subscription",
			groups={"Tier1Tests","blockedByBug-885325","blockedByBug-962520"},
			dependsOnMethods={"testRegisterRHUIConsumer"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testConsumeRHUISubscriptionProduct() {
		if (sm_rhuiSubscriptionProductId.isEmpty()) throw new SkipException("Skipping this test when no RHUI Subscription Product ID (SKU) was provided for testing.");	
		
		// assert that the RHUI ProductId is found in the all available list
		List<SubscriptionPool> allAvailableSubscriptionPools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		Assert.assertNotNull(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", sm_rhuiSubscriptionProductId, allAvailableSubscriptionPools), "RHUI Product ID '"+sm_rhuiSubscriptionProductId+"' is available for consumption to a consumer of type RHUI.");
		
		// assert that the RHUI ProductId is found in the available list only on x86_64,x86 arches
		List<String> supportedArches = Arrays.asList("x86_64","x86","i386","i686");
		List<SubscriptionPool> availableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool rhuiPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", sm_rhuiSubscriptionProductId, availableSubscriptionPools);
		if (!supportedArches.contains(clienttasks.arch)) {
			Assert.assertNull(rhuiPool, "RHUI Product ID '"+sm_rhuiSubscriptionProductId+"' should NOT be available for consumption on a system whose arch ("+clienttasks.arch+") is NOT among the supported arches "+supportedArches);
			throw new SkipException("Cannot consume RHUI Product ID '"+sm_rhuiSubscriptionProductId+"' subscription on a system whose arch ("+clienttasks.arch+") is NOT among the supported arches "+supportedArches);
		}
		Assert.assertNotNull(rhuiPool, "RHUI Product ID '"+sm_rhuiSubscriptionProductId+"' is available for consumption on a system whose arch ("+clienttasks.arch+") is among the supported arches "+supportedArches);

		
		// Subscribe to the RHUI subscription productId
		entitlementCertFile = clienttasks.subscribeToSubscriptionPool(rhuiPool,/*sm_serverAdminUsername*/sm_clientUsername,/*sm_serverAdminPassword*/sm_clientPassword,sm_serverUrl);
	}
	
	@Test(	description="download an expected RHUI iso from an expected yum repoUrl",
			groups={"Tier1Tests"},
			dependsOnMethods={"testConsumeRHUISubscriptionProduct"},
			enabled=false)	// this download file methodology will NOT work for a file; replaced by DownloadRHUIISOFromFileRepo_Test()
	@Deprecated
	//@ImplementsNitrateTest(caseId=)
	public void testDownloadRHUIISOFromYumRepo_DEPRECATED() {
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


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-22305", "RHEL7-55202"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="download an expected RHUI iso from an expected file repoUrl",
			groups={"Tier1Tests","blockedByBug-860516","blockedByBug-894184","blockedByBug-1427516"},	// RHEL-6-RHUI-2-LATEST-Server-x86_64-DVD.iso ERROR 404: Not Found. https://projects.engineering.redhat.com/browse/RCMPROJ-6571
			dependsOnMethods={"testConsumeRHUISubscriptionProduct"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testDownloadRHUIISOFromFileRepo() {
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
		
		// assert available repos for "Red Hat Enterprise Linux Server from RHUI" (when not a server, no content should exist)
		List<Repo> repos = clienttasks.getCurrentlySubscribedRepos();
		if (clienttasks.releasever.contains("Server")) {
			Assert.assertMore(repos.size(), 0, "When consuming RHUI Product ID '"+sm_rhuiSubscriptionProductId+"' on a Server '"+clienttasks.releasever+"' system, repo content should be available.");
		} else {
			Assert.assertEquals(repos.size(), 0, "When consuming RHUI Product ID '"+sm_rhuiSubscriptionProductId+"' on a non-Server '"+clienttasks.releasever+"' system, repo content should NOT be available.");
			throw new SkipException("This system release is '"+clienttasks.releasever+"'.  RHUI ISO '"+sm_rhuiDownloadIso+"' requires Server for downloading.");	// RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso
		}
		
		// substitute the yum vars
		// http://www.centos.org/docs/5/html/5.2/Deployment_Guide/s1-yum-useful-variables.html
		String arch =  Arrays.asList("i686","i486","i386").contains(clienttasks.arch)? "i386":clienttasks.arch;	// http://www.centos.org/docs/5/html/5.2/Deployment_Guide/s1-yum-useful-variables.html
		if (!sm_rhuiDownloadIso.contains(arch)) throw new SkipException("When this system's arch ("+arch+") is substituted into the repoUrl ("+repoUrl+"), it will not find RHUI ISO ("+sm_rhuiDownloadIso+") for downloading.");	// RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso
		repoUrl = repoUrl.replaceFirst("\\$releasever", clienttasks.releasever);
		repoUrl = repoUrl.replaceFirst("\\$basearch", arch);

		File entitlementKeyFile = clienttasks.getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(entitlementCertFile);
		// $ wget  --certificate=<Content Certificate>	https://cdn.redhat.com/content/dist/rhel/rhui/server/6/6Server/x86_64/rhui/2.0/iso/RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso
		// wget --no-check-certificate --certificate=/etc/pki/entitlement/7658526340059785943.pem --private-key=/etc/pki/entitlement/7658526340059785943-key.pem --output-document=/tmp/RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso -- https://cdn.redhat.com/content/dist/rhel/rhui/server/6/6Server/x86_64/rhui/2.0/iso/RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso
		RemoteFileTasks.runCommandAndAssert(client, "wget --no-check-certificate --certificate="+entitlementCertFile+" --private-key="+entitlementKeyFile+" --output-document="+downloadedIsoFile+" -- "+repoUrl+"/"+sm_rhuiDownloadIso, 0/*, stdoutRegex, stderrRegex*/);
		Assert.assertTrue(RemoteFileTasks.testExists(client, downloadedIsoFile.getPath()),"Expected RHUI Download ISO was downloaded.");
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-22306", "RHEL7-55201"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="mount the downloaded RHUI iso and list the packages in the iso",
			groups={"Tier1Tests"},
			dependsOnMethods={"testDownloadRHUIISOFromFileRepo"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testListPackagesInMountedRHUIISO() {

		// RHEL63
		//	[root@rhsm-compat-rhel63 ~]# mkdir /tmp/RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso.mount
		//	[root@rhsm-compat-rhel63 ~]# mount -o loop /tmp/RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso /tmp/RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso.mount
		//	[root@rhsm-compat-rhel63 ~]# ls -l /tmp/RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso.mount
		//	total 32
		//	-r-xr-xr-x. 2 root root  581 Feb  1  2012 install_CDS.sh
		//	-r-xr-xr-x. 2 root root 1175 Feb  3  2012 install_RHUA.sh
		//	-r-xr-xr-x. 2 root root  643 Feb  1  2012 install_tools.sh
		//	-r--r--r--. 2 root root  123 Apr 16 09:49 media.repo
		//	dr-xr-xr-x. 2 root root 6144 Apr 16 09:54 Packages
		//	-r--r--r--. 2 root root 9159 Mar 21 15:21 README
		//	dr-xr-xr-x. 2 root root 4096 Apr 16 09:54 repodata
		//	dr-xr-xr-x. 3 root root 2048 Apr 16 09:54 Server
		//	dr-xr-xr-x. 2 root root 4096 Apr 16 09:54 SRPMS
		//	-r--r--r--. 1 root root 2217 Apr 16 09:59 TRANS.TBL
		//	[root@rhsm-compat-rhel63 ~]# ls -l /tmp/RHEL-6.1-RHUI-2.0-LATEST-Server-x86_64-DVD.iso.mount/Packages/
		//	total 19436
		//	-r--r--r--. 13 root root    66312 Feb 29 16:46 gofer-0.64-1.el6.noarch.rpm
		//	-r--r--r--. 13 root root    32976 Feb 29 16:46 gofer-package-0.64-1.el6.noarch.rpm
		//	-r--r--r--. 13 root root   142284 Feb 29 16:46 grinder-0.0.136-1.el6.noarch.rpm
		//	-r--r--r--. 12 root root   374200 Jul 27  2011 js-1.70-12.el6_0.x86_64.rpm
		//	-r--r--r--. 13 root root   488884 Feb 29 16:46 libmongodb-1.8.2-2.el6.x86_64.rpm
		//	-r--r--r--. 25 root root    52828 Jul 27  2011 libyaml-0.1.3-3.el6_1.x86_64.rpm
		//	-r--r--r--. 24 root root   496284 Feb 29 16:46 m2crypto-0.21.1.pulp-7.el6.x86_64.rpm
		//	-r--r--r--. 13 root root    69180 Feb 29 16:46 mod_wsgi-3.3-2.pulp.el6.x86_64.rpm
		//	-r--r--r--. 13 root root 13075612 Feb 29 16:46 mongodb-1.8.2-2.el6.x86_64.rpm
		//	-r--r--r--. 13 root root  2771684 Feb 29 16:46 mongodb-server-1.8.2-2.el6.x86_64.rpm
		//	-r--r--r--. 10 root root   767028 Mar 22 10:46 pulp-0.0.263-19.el6.noarch.rpm
		//	-r--r--r--. 10 root root   153288 Mar 22 10:46 pulp-admin-0.0.263-19.el6.noarch.rpm
		//	-r--r--r--. 10 root root    99268 Mar 22 10:46 pulp-cds-0.0.263-19.el6.noarch.rpm
		//	-r--r--r--. 10 root root   128888 Mar 22 10:46 pulp-client-lib-0.0.263-19.el6.noarch.rpm
		//	-r--r--r--. 10 root root    55492 Mar 22 10:46 pulp-common-0.0.263-19.el6.noarch.rpm
		//	-r--r--r--. 10 root root    72184 Mar 22 10:46 pulp-consumer-0.0.263-19.el6.noarch.rpm
		//	-r--r--r--. 10 root root    53040 Mar 22 10:46 pulp-selinux-server-0.0.263-19.el6.noarch.rpm
		//	-r--r--r--. 21 root root   126268 Jul 27  2011 pymongo-1.9-8.el6_1.x86_64.rpm
		//	-r--r--r--. 25 root root    43788 Jul 27  2011 python-BeautifulSoup-3.0.8.1-3.el6_1.noarch.rpm
		//	-r--r--r--. 21 root root    49676 Jul 27  2011 python-bson-1.9-8.el6_1.x86_64.rpm
		//	-r--r--r--. 13 root root    95800 Feb 29 16:47 python-gofer-0.64-1.el6.noarch.rpm
		//	-r--r--r--. 66 root root    35776 Jul 27  2011 python-httplib2-0.6.0-4.el6_0.noarch.rpm
		//	-r--r--r--. 24 root root    33988 Feb 29 16:47 python-isodate-0.4.4-4.pulp.el6.noarch.rpm
		//	-r--r--r--. 55 root root    26908 Feb 29 16:47 python-oauth2-1.5.170-2.pulp.el6.noarch.rpm
		//	-r--r--r--. 25 root root   172924 Jul 27  2011 python-webpy-0.32-8.el6_0.noarch.rpm
		//	-r--r--r--. 21 root root   161932 Jul 27  2011 PyYAML-3.09-14.el6_1.x86_64.rpm
		//	-r--r--r--. 13 root root    15128 Feb 29 16:47 rh-rhua-selinux-policy-0.0.6-1.el6.noarch.rpm
		//	-r--r--r--.  3 root root   189972 Apr 16 09:38 rh-rhui-tools-2.0.64-1.el6_2.noarch.rpm
		//	-r--r--r--. 13 root root    35632 Feb 29 16:47 ruby-gofer-0.64-1.el6.noarch.rpm
		//	-r--r--r--.  1 root root     7187 Apr 16 09:59 TRANS.TBL

		// RHEL59
		//	[root@pogolinux-1 ~]# mkdir -p /tmp/rhel-5.5-rhui-1.2-x86_64.iso.mount
		//	[root@pogolinux-1 ~]# mount -o loop /tmp/rhel-5.5-rhui-1.2-x86_64.iso /tmp/rhel-5.5-rhui-1.2-x86_64.iso.mount
		//	[root@pogolinux-1 ~]# ls -l /tmp/rhel-5.5-rhui-1.2-x86_64.iso.mount
		//	total 13
		//	-r-xr-xr-x 3 root root  398 Aug 11  2010 install_CDS.sh
		//	-r-xr-xr-x 3 root root  528 Aug 13  2010 install_RHUA.sh
		//	-r-xr-xr-x 3 root root  535 Aug 11  2010 install_tools.sh
		//	-r--r--r-- 3 root root  555 Jul 26  2010 README
		//	dr-xr-xr-x 3 root root 4096 Dec 13  2010 RHUI
		//	dr-xr-xr-x 2 root root 4096 Dec 13  2010 SRPMS
		//	-r--r--r-- 1 root root 1332 Dec 13  2010 TRANS.TBL
		//	[root@pogolinux-1 ~]# ls -l /tmp/rhel-5.5-rhui-1.2-x86_64.iso.mount/RHUI
		//	total 3955
		//	-r--r--r-- 300 root root   60901 Apr 21  2008 createrepo-0.4.11-3.el5.noarch.rpm
		//	-r--r--r-- 107 root root  220771 Nov  4  2008 elfutils-0.137-3.el5.x86_64.rpm
		//	-r--r--r-- 107 root root  187356 Nov  4  2008 elfutils-libs-0.137-3.el5.x86_64.rpm
		//	-r--r--r--   5 root root   94512 Nov 16  2010 grinder-0.0.57-1.el5.noarch.rpm
		//	-r--r--r--  61 root root 1290719 Aug 26  2010 httpd-2.2.3-43.el5_5.3.x86_64.rpm
		//	-r--r--r--  27 root root   54614 Mar 29  2010 libyaml-0.1.2-3.el5.x86_64.rpm
		//	-r--r--r--  45 root root  506199 Jul 21  2009 m2crypto-0.16-6.el5.6.x86_64.rpm
		//	-r--r--r--  25 root root  811728 Aug  5  2010 mod_python-3.3.1-12.el5.x86_64.rpm
		//	-r--r--r--  25 root root   27697 Jun 17  2010 python-hashlib-20081119-5.el5.x86_64.rpm
		//	-r--r--r--  26 root root   74190 Jun 17  2010 python-pycurl-7.15.5.1-4.el5.x86_64.rpm
		//	-r--r--r--  28 root root  179839 Mar 29  2010 PyYAML-3.08-4.el5.x86_64.rpm
		//	dr-xr-xr-x   2 root root    2048 Dec 13  2010 repodata
		//	-r--r--r--  11 root root   36143 Dec 13  2010 rh-cds-0.27-1.el5_5.noarch.rpm
		//	-r--r--r--  11 root root   85775 Dec 13  2010 rh-rhua-0.91-1.el5_5.noarch.rpm
		//	-r--r--r--  10 root root  100913 Dec 13  2010 rh-rhui-tools-0.76-1.el5_5.noarch.rpm
		//	-r--r--r--  42 root root  308686 Sep  2  2010 rpm-build-4.4.2.3-20.el5_5.1.x86_64.rpm
		//	-r--r--r--   1 root root    3905 Dec 13  2010 TRANS.TBL

		File downloadedIsoFile = new File("/tmp/"+sm_rhuiDownloadIso);
		File mountPoint = new File(downloadedIsoFile+".mount");
		client.runCommandAndWait("mkdir -p "+mountPoint+" && umount "+mountPoint);
		RemoteFileTasks.runCommandAndAssert(client, "mount -o loop "+downloadedIsoFile+" "+mountPoint, 0/*, stdoutRegex, stderrRegex*/);
		client.runCommandAndWait("ls -l "+mountPoint);
		String packagesDir = mountPoint+"/Packages";
		if (clienttasks.redhatReleaseX.equals("5")) packagesDir = mountPoint+"/RHUI";
		Assert.assertTrue(RemoteFileTasks.testExists(client, packagesDir), "The expected Packages directory exists after mounting the downloaded iso file.");
		client.runCommandAndWait("ls -l "+packagesDir);
		client.runCommandAndWait("umount "+mountPoint);
		
		// Note: if you get a HTTP request sent, awaiting response... 404 Not Found, then it is probably missing from the CDN
		// Note: if you get a 403 then it may really be a cert issue, try downloading the http:// file (without the "s") to verify that it really is a cert issue
		// https://cdn.rcm-qa.redhat.com/content/dist/rhel/rhui/server/5/5Server/x86_64/rhui/1.2/iso/rhel-5.5-rhui-1.2-x86_64.iso
		// http://cdn.rcm-qa.redhat.com/content/dist/rhel/rhui/server/5/5Server/x86_64/rhui/1.2/iso/rhel-5.5-rhui-1.2-x86_64.iso
	}

	
	
	// Candidates for an automated Test:
	
	
	// Configuration methods ***********************************************************************
	
	
	
	
	// Protected methods ***********************************************************************
	File entitlementCertFile = null;


	
	// Data Providers ***********************************************************************

}
