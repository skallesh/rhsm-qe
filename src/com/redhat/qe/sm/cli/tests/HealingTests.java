package com.redhat.qe.sm.cli.tests;

import java.util.List;

import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.data.SubscriptionPool;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 *
 */
@Test(groups={"HealingTests"})
public class HealingTests extends SubscriptionManagerCLITestScript {
	
	// There is an autoheal attribute on the consumer
	
//	[root@jsefler-onprem-62server tmp]# curl -k -u testuser1:password --request PUT --data '{"autoheal":false}' --header 'accept:application/json' --header 'content-type: application/json' https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/consumers/562bbb5b-9645-4eb0-8be8-cd0413d531a7
//		[root@jsefler-onprem-62server tmp]# 
//		[root@jsefler-onprem-62server tmp]# curl -k -u testuser1:password --request GET  --header 'accept:application/json' --header 'content-type: application/json' https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/consumers/562bbb5b-9645-4eb0-8be8-cd0413d531a7 | python -mjson.tool |  grep heal
//		  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
//		                                 Dload  Upload   Total   Spent    Left  Speed
//		100 13921    0 13921    0     0  64077      0 --:--:-- --:--:-- --:--:-- 99435
//		    "autoheal": false, 
//		[root@jsefler-onprem-62server tmp]# 



	// Test methods ***********************************************************************
	
	
	
	
	// Configuration methods ***********************************************************************
	
	
	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
