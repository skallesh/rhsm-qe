package com.redhat.qe.sm.cli.tests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *	References:
 *		http://documentation-stage.bne.redhat.com/docs/en-US/Red_Hat_Enterprise_Linux/5/html/Deployment_Guide/rhn-migration.html
 *		https://engineering.redhat.com/trac/PBUPM/browser/trunk/documents/Releases/RHEL6/Variants/RHEL6-Variants.rst
 */
@Test(groups={"MigrationTests"})
public class MigrationFileTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="Verify that the channel-cert-mapping.txt exists",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyChannelCertMappingFileExists_Test() throws FileNotFoundException, IOException {
		Assert.assertEquals(RemoteFileTasks.testFileExists(client, channelCertMappingFile),1,"The expected channel cert mapping file '"+channelCertMappingFile+"' exists.");
		
		// [root@jsefler-onprem-5client ~]# cat /usr/share/rhsm/product/RHEL-5/channel-cert-mapping.txt
		// rhn-tools-rhel-x86_64-server-5-beta: none
		// rhn-tools-rhel-x86_64-server-5: Server-Server-x86_64-fbe6b460-a559-4b02-aa3a-3e580ea866b2-69.pem
		// rhn-tools-rhel-x86_64-client-5-beta: none
		// rhn-tools-rhel-x86_64-client-5: Client-Client-x86_64-efe91c1c-78d7-4d19-b2fb-3c88cfc2da35-68.pem
		SSHCommandResult result = client.runCommandAndWait("cat "+channelCertMappingFile);
		Properties p = new Properties();
		p.load(new ByteArrayInputStream(result.getStdout().getBytes("UTF-8")));
		for (Object key: p.keySet()){
			// load the channelsToProductCertFilesMap
			channelsToProductCertFilesMap.put((String)key, p.getProperty((String)(key)));
			// load the mappedProductCertFiles
			if (!channelsToProductCertFilesMap.get(key).equalsIgnoreCase("none"))
				mappedProductCertFiles.add(channelsToProductCertFilesMap.get(key));
		}
	}
	
	@Test(	description="Verify that all product cert files mapped in channel-cert-mapping.txt exist",
			groups={},
			dependsOnMethods={"VerifyChannelCertMappingFileExists_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAllMappedProductCertFilesExists_Test() {

		boolean allMappedProductCertFilesExist = true;
		for (String mappedProductCertFile : mappedProductCertFiles) {
			mappedProductCertFile = baseProductsDir+"/"+mappedProductCertFile;
			if (RemoteFileTasks.testFileExists(client, mappedProductCertFile)==1) {
				log.info("Mapped productCert file '"+mappedProductCertFile+"' exists.");		
			} else {
				log.warning("Mapped productCert file '"+mappedProductCertFile+"' does NOT exist.");
				allMappedProductCertFilesExist = false;
			}
		}
		Assert.assertTrue(allMappedProductCertFilesExist,"All of the productCert files mapped in '"+channelCertMappingFile+"' exist.");
	}
	
	
	@Test(	description="Verify that all existing product cert files are mapped in channel-cert-mapping.txt",
			groups={},
			dependsOnMethods={"VerifyChannelCertMappingFileExists_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAllExistingProductCertFilesAreMapped_Test() {
		
		// get a list of all the existing product cert files
		SSHCommandResult result = client.runCommandAndWait("ls "+baseProductsDir+"/*.pem");
		List<String> existingProductCertFiles = Arrays.asList(result.getStdout().split("\\n"));
		boolean allExitingProductCertFilesAreMapped = true;
		for (String existingProductCertFile : existingProductCertFiles) {
			if (mappedProductCertFiles.contains(new File(existingProductCertFile).getName())) {
				log.info("Existing productCert file '"+existingProductCertFile+"' is mapped in '"+channelCertMappingFile+"'.");

			} else {
				log.warning("Existing productCert file '"+existingProductCertFile+"' is NOT mapped in '"+channelCertMappingFile+"'.");
				allExitingProductCertFilesAreMapped = false;
			}
		}
		Assert.assertTrue(allExitingProductCertFilesAreMapped,"All of the existing productCert files in directory '"+baseProductsDir+"' are mapped to a channel in '"+channelCertMappingFile+"'.");
	}
	
	
	
	
	
	
	// Candidates for an automated Test:
	
	
	
	// Configuration methods ***********************************************************************
	

	@BeforeClass(groups="setup")
	public void setupBeforeClass() {
		if (clienttasks==null) return;
		if (clienttasks.redhatRelease.contains("release 5")) baseProductsDir+="-5";
		if (clienttasks.redhatRelease.contains("release 6")) baseProductsDir+="-6";
		channelCertMappingFile = baseProductsDir+"/"+channelCertMappingFile;
	}
	
	
	
	// Protected methods ***********************************************************************
	protected String baseProductsDir = "/usr/share/rhsm/product/RHEL";
	protected String channelCertMappingFile = "channel-cert-mapping.txt";
	List<String> mappedProductCertFiles = new ArrayList<String>();	// list of all the mapped product cert file names in the mapping file (e.g. Server-Server-x86_64-fbe6b460-a559-4b02-aa3a-3e580ea866b2-69.pem)
	Map<String,String> channelsToProductCertFilesMap = new HashMap<String,String>();	// map of all the channels to product cert file names (e.g. key=rhn-tools-rhel-x86_64-server-5 value=Server-Server-x86_64-fbe6b460-a559-4b02-aa3a-3e580ea866b2-69.pem)

	
	
	// Data Providers ***********************************************************************

	
	/*
	 * 
	 */
	
	// EXAMPLE TAKEN FROM THE DEPLOYMENT GUIDE http://documentation-stage.bne.redhat.com/docs/en-US/Red_Hat_Enterprise_Linux/5/html/Deployment_Guide/rhn-install-num.html
/*
	[root@jsefler-onprem-5server ~]# python /usr/lib/python2.4/site-packages/instnum.py da3122afdb7edd23
	Product: RHEL Client
	Type: Installer Only
	Options: Eval FullProd Workstation
	Allowed CPU Sockets: Unlimited
	Allowed Virtual Instances: Unlimited
	Package Repositories: Client Workstation

	key: 14299426 'da3122'
	checksum: 175 'af'
	options: 4416 'Eval FullProd Workstation'
	socklimit: -1 'Unlimited'
	virtlimit: -1 'Unlimited'
	type: 2 'Installer Only'
	product: 1 'client'

	{'Workstation': 'Workstation', 'Base': 'Client'}

	da31-22af-db7e-dd23
	[root@jsefler-onprem-5server ~]# 

	[root@jsefler-onprem-5server ~]# install-num-migrate-to-rhsm -d -i da3122afdb7edd23
	Copying /usr/share/rhsm/product/RHEL-5/Client-Workstation-x86_64-efa6382a-44c4-408b-a142-37ad4be54aa6-71.pem to /etc/pki/product/71.pem
	Copying /usr/share/rhsm/product/RHEL-5/Client-Client-x86_64-efe91c1c-78d7-4d19-b2fb-3c88cfc2da35-68.pem to /etc/pki/product/68.pem
	[root@jsefler-onprem-5server ~]# 
	[root@jsefler-onprem-5server ~]# openssl x509 -text -in /usr/share/rhsm/product/RHEL-5/Client-Client-x86_64-efe91c1c-78d7-4d19-b2fb-3c88cfc2da35-68.pem | grep -A1 1.3.6.1.4.1.2312.9.1
	            1.3.6.1.4.1.2312.9.1.68.1: 
	                . Red Hat Enterprise Linux Desktop
	            1.3.6.1.4.1.2312.9.1.68.2: 
	                ..5.7
	            1.3.6.1.4.1.2312.9.1.68.3: 
	                ..x86_64
	            1.3.6.1.4.1.2312.9.1.68.4: 
	                ..rhel-5,rhel-5-client
	[root@jsefler-onprem-5server ~]# openssl x509 -text -in /usr/share/rhsm/product/RHEL-5/Client-Workstation-x86_64-efa6382a-44c4-408b-a142-37ad4be54aa6-71.pem | grep -A1 1.3.6.1.4.1.2312.9.1
	            1.3.6.1.4.1.2312.9.1.71.1: 
	                .$Red Hat Enterprise Linux Workstation
	            1.3.6.1.4.1.2312.9.1.71.2: 
	                ..5.7
	            1.3.6.1.4.1.2312.9.1.71.3: 
	                ..x86_64
	            1.3.6.1.4.1.2312.9.1.71.4: 
	                .,rhel-5-client-workstation,rhel-5-workstation
	[root@jsefler-onprem-5server ~]# 
	
*/
}
