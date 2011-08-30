package com.redhat.qe.sm.cli.tests;

import java.io.File;
import java.util.List;

import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * Bug 730380 - Can not import a certificate via the cli - https://bugzilla.redhat.com/show_bug.cgi?id=730380
 * Bug 712980 - Import Certificate neglects to also import the corresponding key.pem - https://bugzilla.redhat.com/show_bug.cgi?id=712980
 * Bug 733873 - subscription-manager import --help should not use proxy options - https://bugzilla.redhat.com/show_bug.cgi?id=733873
 */
@Test(groups={"ImportTests"})
public class ImportTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager: attempt an entitlement cert import from a file containing both the cert and key",
			groups={},
			enabled=false)
	//@ImplementsNitrateTest(caseId=)
	public void ImportAnEntitlementCertAndKeyFromFile_Test() {
		clienttasks.importCertificate(importEntitlementCert.getPath());
	}
	
	
	@Test(	description="subscription-manager: attempt an entitlement cert import from a file containing both the key and cert",
			groups={},
			enabled=false)
	//@ImplementsNitrateTest(caseId=)
	public void ImportAnEntitlementKeyAndCertFromFile_Test() {
		
	}
	
	
	@Test(	description="subscription-manager: attempt an entitlement cert import from a valid file in the current directory",
			groups={},
			enabled=false)
	//@ImplementsNitrateTest(caseId=)
	public void ImportAnEntitlementKeyAndCertFromFileInCurrentDirectoty_Test() {
		
	}
	
	
	@Test(	description="subscription-manager: attempt an entitlement cert import from a file containing the cert only",
			groups={},
			enabled=false)
	//@ImplementsNitrateTest(caseId=)
	public void ImportAnEntitlementCertFromFile_Test() {
				
	}
	
	
	@Test(	description="subscription-manager: attempt an entitlement cert import from a file containing only a key (negative test)",
			groups={},
			enabled=false)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptAnEntitlementImportFromAKeyFile_Test() {
				
	}
	
	
	@Test(	description="subscription-manager: attempt an entitlement cert import using an identity cert (negative test)",
			groups={},
			enabled=false)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptAnEntitlementImportFromAnIdentityCertFile_Test() {
		
	}
	
	@Test(	description="subscription-manager: attempt an entitlement cert import using an identity cert (negative test)",
			groups={},
			enabled=false)
	//@ImplementsNitrateTest(caseId=)
	public void AttemptAnEntitlementImportFromAProductCertFile_Test() {
		
	}
	
	
	
	// Protected Class Variables ***********************************************************************
	
	protected final File importEntitlementCert = new File("/tmp/sm-importEntitlementCert");
	protected final File importEntitlementKey = new File("/tmp/sm-importEntitlementKey");

	
	// Configuration methods ***********************************************************************
	

	@BeforeClass(groups={"setup"})
	public void setupImportFilesBeforeClass() {
		
		// register and subscribe to a random pool to get a valid entitlement cert and its key
		
		// store the entitlement cert
		
		// store the entitlement cert key
		
		
		
		
		// delete the entitlement cert and key from /etc/pki/entitlement
		
		
		// create an importFileContainingEntitlementCertAndKey
		
		// create an importFileContainingEntitlementCertOnly
		
		// create an importFileContainingEntitlementKeyOnly
		

	}
	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
