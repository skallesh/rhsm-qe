package com.redhat.qe.sm.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testng.TestNGUtils;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.tools.RemoteFileTasks;

public class Register extends GeneralTests {
	
	@Test(description="Verify invalid subscription-manager-cli fail",
			dataProvider="invalidRegistrationTest",
			expectedExceptions={AssertionError.class})
	@ImplementsTCMS(id="41691")
	public void InvalidRegistration_Test(String username, String password){
		this.registerToCandlepin(username, password);
	}
	
	@Test(description="Verify valid subscription-manager-cli success",
			dependsOnMethods={"InvalidRegistration_Test", "NegativeFunctionality_Test"})
	@ImplementsTCMS(id="41677")
	public void ValidRegistration_Test(){
		this.registerToCandlepin(username, password);
	}
	
	@Test(description="Tests that certificate frequency is updated at appropriate intervals",
			dependsOnMethods="ValidRegistration_Test")
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
	
	@Test(description="Tests that certificates are refreshed appropriately",
			dependsOnMethods="certFrequency_Test")
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
				"pem", 
				null, 
				0);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, 
				"ls /etc/pki/entitlement/product", 
				"pem", 
				null, 
				0);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, 
				"ls /etc/pki/product", 
				"pem", 
				null, 
				0);
	}
	
	@DataProvider(name="invalidRegistrationTest")
	public Object[][] getInvalidRegistrationDataAs2dArray() {
		return TestNGUtils.convertListOfListsTo2dArray(getInvalidRegistrationDataAsListOfLists());
	}
	protected List<List<Object>> getInvalidRegistrationDataAsListOfLists() {
		List<List<Object>> ll = new ArrayList<List<Object>>();
		ll.add(Arrays.asList(new Object[]{"",""}));
		ll.add(Arrays.asList(new Object[]{username,""}));
		ll.add(Arrays.asList(new Object[]{"",password}));
		ll.add(Arrays.asList(new Object[]{username+getRandInt(),password+getRandInt()}));
		return ll;
	}
}
