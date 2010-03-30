package com.redhat.qe.sm.tests;

import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.tools.RemoteFileTasks;

public class List extends Register{
	@Test(description="Verify that list --available works",
			dependsOnMethods="ValidRegistration_Test")
	@ImplementsTCMS(id="41678")
	public void EnsureAvailableEntitlementsListed_Test(){
		RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner,
				RHSM_LOC + "list --available");
	}
	
	@Test(description="Verify that list --available works",
			dependsOnMethods="ValidRegistration_Test")
	@ImplementsTCMS(id="41679")
	public void EnsureConsumedEntitlementsListed_Test(){
		RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner,
				RHSM_LOC + "list --consumed");
	}
}
