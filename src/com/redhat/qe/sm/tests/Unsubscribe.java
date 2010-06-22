package com.redhat.qe.sm.tests;

import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsTCMS;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.abstractions.Pool;
import com.redhat.qe.tools.RemoteFileTasks;

public class Unsubscribe extends Setup{
	@Test(description="subscription-manager-cli: unsubscribe client to an entitlement using product ID",
			dependsOnGroups={"sm_stage4"},
			groups={"sm_stage5", "blockedByBug-584137", "blockedByBugDISABLED-602852"})
	@ImplementsTCMS(id="41688")
	public void UnsubscribeFromValidProductIDs_Test(){
		this.subscribeToAllPools(false);
		this.unsubscribeFromAllProductIDs();
	}
	
	@Test(description="Copy entitlement certificate in /etc/pki/entitlement/product after unsubscribe",
			dependsOnGroups={"sm_stage4"},
			groups={"sm_stage5", "blockedByBug-584137", "blockedByBugDISABLED-602852"})
	@ImplementsTCMS(id="41903")
	public void UnsubscribeAndReplaceCert_Test(){
		sshCommandRunner.runCommandAndWait("killall -9 yum");
		String randDir = "/tmp/sm-certs-"+Integer.toString(this.getRandInt());
		this.subscribeToAllPools(false);
		
		//copy certs to temp dir
		sshCommandRunner.runCommandAndWait("rm -rf "+randDir);
		sshCommandRunner.runCommandAndWait("mkdir -p "+randDir);
		sshCommandRunner.runCommandAndWait("cp /etc/pki/entitlement/product/* "+randDir);
		
		this.unsubscribeFromAllProductIDs();
		
		sshCommandRunner.runCommandAndWait("cp -f "+randDir+"/* /etc/pki/entitlement/product");
		RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,
				"yum repolist");
	}
	
	@Test(description="Unsubscribe product entitlement and re-subscribe",
			dependsOnGroups={"sm_stage4"},
			groups={"sm_stage5", "blockedByBug-584137", "blockedByBugDISABLED-602852"})
	@ImplementsTCMS(id="41898")
	public void ResubscribeAfterUnsubscribe_Test(){
		this.subscribeToAllPools(false);
		this.unsubscribeFromAllProductIDs();
		this.subscribeToAllPools(false);
	}
}
