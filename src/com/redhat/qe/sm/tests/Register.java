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

public class Register extends Setup {
	
	@Test(description="subscription-manager-cli: register to a Candlepin server using bogus credentials",
			dataProvider="invalidRegistrationTest",
			expectedExceptions={AssertionError.class},
			groups={"sm_stage1"})
	@ImplementsTCMS(id="41691")
	public void InvalidRegistration_Test(String username, String password){
		this.registerToCandlepin(username, password);
	}
	
	@Test(description="subscription-manager-cli: register to a Candlepin server",
			dependsOnGroups={"sm_stage1"},
			groups={"sm_stage2"},
			alwaysRun=true)
	@ImplementsTCMS(id="41677")
	public void ValidRegistration_Test(){
		this.registerToCandlepin(username, password);
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
