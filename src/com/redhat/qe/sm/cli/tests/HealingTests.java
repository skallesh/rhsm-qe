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
// toggle the autoheal attribute.  Something is wrong in this call - not sure what
//		[root@jsefler-onprem-62server tmp]# curl -k -u testuser1:password --request POST --data '{"autoheal":"false"}' --header 'accept: application/json' --header 'content-type: application/json' https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/consumers/2799c6a6-ad05-487c-98fc-6009f89579f5 | python -mjson.tool | grep heal
//			  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
//			                                 Dload  Upload   Total   Spent    Left  Speed
//			100 14047    0 14047    0    20  36722     52 --:--:-- --:--:-- --:--:-- 43027
//			    "autoheal": true, 
//			[root@jsefler-onprem-62server tmp]# 
		
// get the consumer attribue value...
//		[root@jsefler-onprem-62server tmp]# curl -k -u testuser1:password --request GET https://jsefler-onprem-62candlepin.usersys.redhat.com:8443/candlepin/consumers/2799c6a6-ad05-487c-98fc-6009f89579f5 | python -mjson.tool | grep heal
//			  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
//			                                 Dload  Upload   Total   Spent    Left  Speed
//			100 14047    0 14047    0     0  54591      0 --:--:-- --:--:-- --:--:-- 98922
//			    "autoheal": true, 
//			[root@jsefler-onprem-62server tmp]# 


	// Test methods ***********************************************************************
	
	
	
	
	// Configuration methods ***********************************************************************
	
	
	
	
	// Protected methods ***********************************************************************


	
	// Data Providers ***********************************************************************

}
