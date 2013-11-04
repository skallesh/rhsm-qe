package rhsm.cli.tests;


import java.util.List;

import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.tcms.ImplementsNitrateTest;
import com.redhat.qe.tools.RemoteFileTasks;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.SubscriptionPool;

/**
 * @author ssalevan
 *
 */
@Test(groups={"UnregisterTests"})
public class UnregisterTests extends SubscriptionManagerCLITestScript {
	
	
	// Test Methods ***********************************************************************

	@Test(description="unregister the consumer",
			groups={"blockedByBug-589626"},
			enabled=true)
	@ImplementsNitrateTest(caseId=46714)
	public void RegisterSubscribeAndUnregisterTest() {
		clienttasks.unregister(null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
		List<SubscriptionPool> availPoolsBeforeSubscribingToAllPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		clienttasks.subscribeToTheCurrentlyAvailableSubscriptionPoolsIndividually();
		clienttasks.unregister(null, null, null);
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, null, null, null, false, null, null, null);
		for (SubscriptionPool afterPool : clienttasks.getCurrentlyAvailableSubscriptionPools()) {
			SubscriptionPool originalPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", afterPool.poolId, availPoolsBeforeSubscribingToAllPools);
			Assert.assertEquals(originalPool.quantity, afterPool.quantity,
				"The subscription quantity count for Pool "+originalPool.poolId+" returned to its original count after subscribing to it and then unregistering from the candlepin server.");
		}
	}
	
	
	@Test(description="unregister should not make unauthorized requests",
			groups={"blockedByBug-997935"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void UnregisterShouldNotThrowUnauthorizedRequests_Test() {
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg);
		String logMarker = System.currentTimeMillis()+" Testing UnregisterShouldNotThrowUnauthorizedRequests_Test...";
		RemoteFileTasks.markFile(client, clienttasks.rhsmLogFile, logMarker);
		clienttasks.unregister(null, null, null);
		//	[root@jsefler-6 ~]# grep -A1 "Making request" /var/log/rhsm/rhsm.log
		//	2013-09-16 11:53:26,339 [DEBUG]  @connection.py:441 - Making request: DELETE /subscription/consumers/71775a6e-40e0-4422-8db6-5bff074389ef
		//	2013-09-16 11:53:27,045 [DEBUG]  @connection.py:460 - Response status: 204
		
		//	[root@jsefler-7 ~]# grep -A1 "Making request" /var/log/rhsm/rhsm.log
		//	2013-11-04 18:38:43,279 [DEBUG] subscription-manager @connection.py:442 - Making request: DELETE /candlepin/consumers/2f90f0ff-fca5-4425-8f84-6d3d951ac4a7
		//	2013-11-04 18:38:43,504 [DEBUG] subscription-manager @connection.py:465 - Response: status=204, requestUuid=c68cecf5-3972-494a-b3d1-17d8d044d9b8

		String logTail = RemoteFileTasks.getTailFromMarkedFile(client, clienttasks.rhsmLogFile, logMarker, "Response").trim();
		String unexpectedLogMessage = "Response status: 401";
		unexpectedLogMessage = "Response: status=401";	// changed by python-rhsm commit ce3727f73c5ac6f77db5e52027443ec456a5d733 Log the new requestUuid from candlepin if it is present in the response.
		Assert.assertFalse(logTail.contains(unexpectedLogMessage), "The '"+clienttasks.rhsmLogFile+"' should not encounter unexpected log message '"+unexpectedLogMessage+"' after unregister.");
		String expectedLogMessage = "Response status: 204";
		expectedLogMessage = "Response: status=204";	// changed by python-rhsm commit ce3727f73c5ac6f77db5e52027443ec456a5d733 Log the new requestUuid from candlepin if it is present in the response.
		Assert.assertTrue(logTail.contains(expectedLogMessage), "The '"+clienttasks.rhsmLogFile+"' should encounter expected log message '"+expectedLogMessage+"' after unregister (indicative of a successful DELETE request).");
	}
	
	
	
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 674652 - Subscription Manager Leaves Broken Yum Repos After Unregister https://github.com/RedHatQE/rhsm-qe/issues/218
	// TODO Bug 706853 - SM Gui “unregister” button deletes “consumer” folder for non network host. https://github.com/RedHatQE/rhsm-qe/issues/219
}
