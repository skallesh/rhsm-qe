package com.redhat.qe.sm.cli.tests;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.Assert;
import com.redhat.qe.sm.base.SubscriptionManagerCLITestScript;
import com.redhat.qe.sm.cli.tasks.CandlepinTasks;
import com.redhat.qe.sm.data.EntitlementCert;
import com.redhat.qe.sm.data.InstalledProduct;
import com.redhat.qe.sm.data.ProductSubscription;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *
 * Reference: https://engineering.redhat.com/trac/Entitlement/wiki/SlaSubscribe
 */

@Test(groups={"ServiceLevelTests"})
public class ServiceLevelTests extends SubscriptionManagerCLITestScript {

	
	// Test methods ***********************************************************************
	
	@Test(	description="subscription-manager: service-level (when not registered)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ServiceLevelWhenNotRegistered_Test() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null);
		
		SSHCommandResult result;
		
		// with credentials
		result = clienttasks.service_level_(null, null, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level (implies --show) without being registered");
		Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --show without being registered");
		
		// without credentials
		result = clienttasks.service_level_(null, null, null, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level (implies --show) without being registered");
		Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --show without being registered");
	}
	
	
	@Test(	description="subscription-manager: service-level --show (when not registered)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ServiceLevelShowWhenNotRegistered_Test() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null);
		
		SSHCommandResult result;
		
		// with credentials
		result = clienttasks.service_level_(true, null, sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --show without being registered");
		Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --show without being registered");
		
		// without credentials
		result = clienttasks.service_level_(true, null, null, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --show without being registered");
		Assert.assertEquals(result.getStdout().trim(),"Error: This system is currently not registered.", "Stdout from service-level --show without being registered");
	}
	
	
	@Test(	description="subscription-manager: service-level --list (when not registered)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ServiceLevelListWhenNotRegistered_Test() {
		
		// make sure we are not registered
		clienttasks.unregister(null, null, null);
		
		SSHCommandResult result = clienttasks.service_level_(null, true, null, null, null, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --list without being registered");
		Assert.assertEquals(result.getStdout().trim(),"Error: you must register or specify --username and password to list service levels", "Stdout from service-level --list without being registered");
	}
	
	
	@Test(	description="subscription-manager: service-level --list (with invalid credentials)",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ServiceLevelListWithInvalidCredentials_Test() {
		String x = String.valueOf(getRandInt());
		SSHCommandResult result;
				
		// test while unregistered
		clienttasks.unregister(null, null, null);
		result = clienttasks.service_level_(null, true, sm_clientUsername, sm_clientPassword+x, sm_clientOrg, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --list with invalid credentials");
		//if (sm_serverOld) {Assert.assertEquals(result.getStdout().trim(), "Error: you must register or specify --org."); throw new SkipException("service-level --list with invalid credentials against an old candlepin server is not supported.");}
		if (sm_serverOld) {Assert.assertEquals(result.getStderr().trim(), "ERROR: The service-level command is not supported by the server."); throw new SkipException("Skipping this test since service-level is gracefully not supported when configured against an old candlepin server.");}
		Assert.assertEquals(result.getStderr().trim(), servertasks.invalidCredentialsMsg(), "Stderr from service-level --list with invalid credentials");
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from service-level --list with invalid credentials");

		// test while registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,null, null);
		result = clienttasks.service_level_(null, true, sm_clientUsername, sm_clientPassword+x, sm_clientOrg, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --list with invalid credentials");
		Assert.assertEquals(result.getStderr().trim(), servertasks.invalidCredentialsMsg(), "Stderr from service-level --list with invalid credentials");
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from service-level --list with invalid credentials");
	}
	
	
	@Test(	description="subscription-manager: service-level --list (with invalid org)",
			groups={"blockedByBug-796468","blockedByBug-815479"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ServiceLevelListWithInvalidOrg_Test() {
		String x = String.valueOf(getRandInt());
		SSHCommandResult result;
				
		// test while unregistered
		clienttasks.unregister(null, null, null);
		result = clienttasks.service_level_(null, true, sm_clientUsername, sm_clientPassword, sm_clientOrg+x, null, null, null);
		if (sm_serverOld) {Assert.assertEquals(result.getStderr().trim(), "ERROR: The service-level command is not supported by the server."); throw new SkipException("Skipping this test since service-level is gracefully not supported when configured against an old candlepin server.");}
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --list with invalid org");
		Assert.assertEquals(result.getStderr().trim(), String.format("Organization with id %s could not be found",sm_clientOrg+x), "Stderr from service-level --list with invalid org");
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from service-level --list with invalid credentials");

		// test while registered
		clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,null,null,null,null, null);
		result = clienttasks.service_level_(null, true, sm_clientUsername, sm_clientPassword, sm_clientOrg+x, null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "ExitCode from service-level --list with invalid org");
		Assert.assertEquals(result.getStderr().trim(), String.format("Organization with id %s could not be found",sm_clientOrg+x), "Stderr from service-level --list with invalid org");
		Assert.assertEquals(result.getStdout().trim(), "", "Stdout from service-level --list with invalid credentials");
	}
	
	
	@Test(	description="subscription-manager: service-level --show (after registering without a service level)",
			groups={},
			enabled=false) // assertions in this test are already a subset of ServiceLevelShowAvailable_Test
	//@ImplementsNitrateTest(caseId=)
	public void ServiceLevelShowAfterRegisteringWithoutServiceLevel_Test() throws JSONException, Exception  {
		SSHCommandResult result;
				
		// register with no service-level
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername,sm_clientPassword,sm_clientOrg,null,null,null,null,null,null,null,(List<String>)null,true,null,null,null, null));
		
		// get the current consumer object and assert that the serviceLevel persisted
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertEquals(jsonConsumer.get("serviceLevel"), JSONObject.NULL, "The call to register without a servicelevel leaves the current consumer object serviceLevel attribute value null.");
		
		result = clienttasks.service_level(true, false, null, null, null, null, null, null);
		Assert.assertEquals(result.getStdout().trim(), "Current service level:", "When the system has been registered without a service level, the current service level should be null.");
	}
	
	
	@Test(	description="subscription-manager: service-level --show (after registering without a service level)",
			groups={"AcceptanceTests"},
			dataProvider="getRegisterCredentialsExcludingNullOrgData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void ServiceLevelShowAvailable_Test(String username, String password, String org) throws JSONException, Exception  {
		SSHCommandResult result;
				
		// register with no service-level
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(username,password,org,null,null,null,null,null,null,null,(List<String>)null,true,null,null,null, null));
		
		// get the current consumer object and assert that the serviceLevel is empty (value is "")
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(username,password,sm_serverUrl,"/consumers/"+consumerId));
		// Assert.assertEquals(jsonConsumer.get("serviceLevel"), JSONObject.NULL, "The call to register without a servicelevel leaves the current consumer object serviceLevel attribute value null.");	// original value was null
		if (sm_serverOld) {Assert.assertFalse(jsonConsumer.has("serviceLevel"), "Consumer attribute serviceLevel should not exist against an old candlepin server."); throw new SkipException("The service-level command is not supported by the server.");}
		Assert.assertEquals(jsonConsumer.get("serviceLevel"), "", "The call to register without a servicelevel leaves the current consumer object serviceLevel attribute value empty.");
	
		// assert that "Current service level:" is empty
		Assert.assertEquals(clienttasks.getCurrentServiceLevel(), "", "When the system has been registered without a service level, the current service level value should be empty.");
		Assert.assertEquals(clienttasks.service_level(null,null,null,null,null,null,null,null).getStdout().trim(), "Current service level:", "When the system has been registered without a service level, the current service level value should be empty.");

		// get all the valid service levels available to this org	
		List<String> serviceLevelsExpected = CandlepinTasks.getServiceLevelsForOrgKey(/*username or*/sm_serverAdminUsername, /*password or*/sm_serverAdminPassword, sm_serverUrl, org);
		
		// assert that all the valid service levels are returned by service-level --list
		List<String> serviceLevelsActual = clienttasks.getCurrentlyAvailableServiceLevels();		
		Assert.assertTrue(serviceLevelsExpected.containsAll(serviceLevelsActual)&&serviceLevelsActual.containsAll(serviceLevelsExpected), "The actual service levels available to the current consumer "+serviceLevelsActual+" match the expected list of service levels available to the org '"+org+"' "+serviceLevelsExpected+".");

		// assert that exempt service levels do NOT appear as valid service levels
		for (String sm_exemptServiceLevel : sm_exemptServiceLevelsInUpperCase) {
			for (String serviceLevel : serviceLevelsExpected) {
				Assert.assertTrue(!serviceLevel.toUpperCase().equals(sm_exemptServiceLevel.toUpperCase()), "Regardless of case, available service level '"+serviceLevel+"' should NOT match exempt service level '"+sm_exemptServiceLevel+"'.");
			}
		}
		
		// subscribe with each service level and assert that the current service level persists the requested service level
		for (String serviceLevel : serviceLevelsExpected) {
			clienttasks.subscribe_(true, serviceLevel, (String)null, (String)null, (String)null, null, null, null, null, null, null);
			Assert.assertEquals(clienttasks.getCurrentServiceLevel(), serviceLevel, "When the system has auto subscribed specifying a service level, the current service level should be persisted.");

		}
	}
		
	
	
	
	
	
	@Test(	description="subscription-manager-cli: register to a Candlepin server using autosubscribe with an unavailable servicelevel",
			groups={"blockedByBug-795798"},
			enabled=true)
	public void RegisterWithUnavailableServiceLevel_Test() {

		// attempt the registration
		String unavailableServiceLevel = "FOO";
		SSHCommandResult sshCommandResult = clienttasks.register_(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, unavailableServiceLevel, null, (String)null, true, null, null, null, null);
		String msg = "Cannot set a service level for a consumer that is not available to its organization."; // before Bug 795798 - Cannot set a service level for a consumer that is not available to its organization.
		msg = String.format("Service level %s is not available to consumers of organization %s.",unavailableServiceLevel,sm_clientOrg);
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(255));
		Assert.assertTrue(sshCommandResult.getStdout().trim().contains(msg), "Stdout message contains: "+msg);
		Assert.assertEquals(sshCommandResult.getStderr().trim(), "", "Stderr message from an attempt to register with autosubscribe and an unavailable servicelevel.");

		// despite a failed attempt to set a service level, we should still be registered
		Assert.assertNotNull(clienttasks.getCurrentConsumerId(), "Despite a failed attempt to set a service level during register with autosubscribe, we should still be registered");
		
		// since the autosubscribe was aborted, we should not be consuming and entitlements
		Assert.assertTrue(clienttasks.getCurrentEntitlementCerts().isEmpty(), "Due to a failed attempt to set a service level during register with autosubscribe, we should not be consuming any entitlements.");	
	}
	
	
	@Test(	description="subscription-manager: register with autosubscribe while specifying an valid service level; assert the entitlements granted match the requested service level",
			groups={"AcceptanceTests"},
			dataProvider="getAllAvailableServiceLevelData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void RegisterWithAvailableServiceLevel_Test(Object bugzulla, String serviceLevel) throws JSONException, Exception {
		// Reference: https://engineering.redhat.com/trac/Entitlement/wiki/SlaSubscribe

		// register with autosubscribe specifying a valid service level
		String consumerId = clienttasks.getCurrentConsumerId(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, serviceLevel, null, (String)null, true, null, null, null, null));
		
		// get the current consumer object and assert that the serviceLevel persisted
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+consumerId));
		Assert.assertEquals(jsonConsumer.get("serviceLevel"), serviceLevel, "The call to register with autosubscribe and a servicelevel persisted the servicelevel setting on the current consumer object.");
		
		// assert that each of the autosubscribed entitlements come from a pool that supports the specified service level
		clienttasks.listConsumedProductSubscriptions();
		for (EntitlementCert entitlementCert : clienttasks.getCurrentEntitlementCerts()) {
			if (sm_exemptServiceLevelsInUpperCase.contains(entitlementCert.orderNamespace.supportLevel.toUpperCase())) {
				log.warning("After autosubscribed registration with service level '"+serviceLevel+"', this autosubscribed entitlement provides an exempt service level '"+entitlementCert.orderNamespace.supportLevel+"' from entitled orderNamespace: "+entitlementCert.orderNamespace);
			} else {
				Assert.assertEquals(entitlementCert.orderNamespace.supportLevel, serviceLevel,"This autosubscribed entitlement provides the requested service level '"+serviceLevel+"' from entitled orderNamespace: "+entitlementCert.orderNamespace);
			}
		}
	}
	
	
	@Test(	description="subscription-manager: register with autosubscribe while specifying an valid random case SeRviCEleVel; assert the installed product status is independent of the specified service level case.",
			groups={"AcceptanceTests"},
			dataProvider="getAllAvailableServiceLevelData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyRegisterWithServiceLevelIsCaseInsensitive(Object bugzulla, String serviceLevel) {
			
		// register with autosubscribe specifying a valid service level and get the installed product status
		List<InstalledProduct> installedProductsAfterAutosubscribedRegisterWithServiceLevel= InstalledProduct.parse(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, serviceLevel, null, (String)null, true, null, null, null, null).getStdout());
		
		// register with autosubscribe specifying a mixed case service level and get the installed product status
		String mixedCaseServiceLevel = randomizeCaseOfCharactersInString(serviceLevel);
		List<InstalledProduct> installedProductsAfterAutosubscribedRegisterWithMixedCaseServiceLevel= InstalledProduct.parse(clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, mixedCaseServiceLevel, null, (String)null, true, null, null, null, null).getStdout());

		// assert that the two lists are identical (independent of the serviceLevel case specified during registration)
		Assert.assertEquals(installedProductsAfterAutosubscribedRegisterWithMixedCaseServiceLevel.size(), clienttasks.getCurrentProductCertFiles().size(), "The registration output displayed the same number of installed product status's as the number of installed product certs.");
		Assert.assertTrue(installedProductsAfterAutosubscribedRegisterWithServiceLevel.containsAll(installedProductsAfterAutosubscribedRegisterWithMixedCaseServiceLevel) && installedProductsAfterAutosubscribedRegisterWithMixedCaseServiceLevel.containsAll(installedProductsAfterAutosubscribedRegisterWithServiceLevel), "Autosubscribed registration with serviceLevel '"+mixedCaseServiceLevel+"' yielded the same installed product status as autosubscribed registration with serviceLevel '"+serviceLevel+"'.");
		
		// assert that each of the consumed ProductSubscriptions match the specified service level
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		if (consumedProductSubscriptions.isEmpty()) log.warning("No entitlements were granted after registering with autosubscribe and service level '"+mixedCaseServiceLevel+"'."); 
		for (ProductSubscription productSubscription : consumedProductSubscriptions) {
			// tolerate exemptServiceLevels
			if (sm_exemptServiceLevelsInUpperCase.contains(productSubscription.serviceLevel.toUpperCase())) {
				log.warning("After autosubscribed registration with service level '"+mixedCaseServiceLevel+"', this consumed ProductSubscription provides an exempt service level '"+productSubscription.serviceLevel+"'.");
				continue;
			}
			
			Assert.assertTrue(productSubscription.serviceLevel.equalsIgnoreCase(mixedCaseServiceLevel),
					"After autosubscribed registration with service level '"+mixedCaseServiceLevel+"', this consumed ProductSubscription provides a service level '"+productSubscription.serviceLevel+"' that is a case insensitive match to '"+mixedCaseServiceLevel+"'.");
		}
	}
	
	
	
	
	
	
	
	@Test(	description="subscription-manager: subscribe with auto while specifying an unavailable service level",
			groups={"blockedByBug-795798"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AutoSubscribeWithUnavailableServiceLevel_Test() {
		
		// register with force
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, true, false, null, null, null);
		
		// subscribe with auto specifying an unavailable service level
		SSHCommandResult result = clienttasks.subscribe_(true,"FOO",(String)null,null,null,null,null,null,null, null, null);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(255),"Exit code from an attempt to subscribe with auto and an unavailable service level.");
		//Assert.assertEquals(result.getStdout().trim(), "Cannot set a service level for a consumer that is not available to its organization.", "Stdout from an attempt to subscribe with auto and an unavailable service level.");
		Assert.assertEquals(result.getStdout().trim(), String.format("Service level %s is not available to consumers of organization %s.","FOO",sm_clientOrg), "Stdout from an attempt to subscribe with auto and an unavailable service level.");
		Assert.assertEquals(result.getStderr().trim(), "", "Stderr from an attempt to subscribe with auto and an unavailable service level.");
	}
	
	
	@Test(	description="subscription-manager: subscribe with auto while specifying an valid service level; assert the entitlements granted match the requested service level",
			groups={"AcceptanceTests"},
			dataProvider="getAllAvailableServiceLevelData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AutoSubscribeWithServiceLevel_Test(Object bugzulla, String serviceLevel) throws JSONException, Exception {
		// Reference: https://engineering.redhat.com/trac/Entitlement/wiki/SlaSubscribe
		
		// ensure system is registered
		if (clienttasks.getCurrentConsumerId()==null) clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, "AutoSubscribeWithServiceLevelConsumer", null, null, null, null, (String)null, null, false, null, null, null);
		
		// remember this initial service level preference for this consumer
		String initialConsumerServiceLevel = clienttasks.getCurrentServiceLevel();
		
		// start fresh by returning all entitlements
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
		
		// autosubscribe with a valid service level
		clienttasks.subscribe(true,serviceLevel,(String)null,(String)null,(String)null,null,null,null,null, null, null);
		
		// get the current consumer object and assert that the serviceLevel persisted
		JSONObject jsonConsumer = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(sm_clientUsername, sm_clientPassword, sm_serverUrl, "/consumers/"+clienttasks.getCurrentConsumerId()));
		if (serviceLevel==null || serviceLevel.equals("")) {
			Assert.assertEquals(jsonConsumer.get("serviceLevel"), initialConsumerServiceLevel, "The consumer's serviceLevel preference should remain unchanged when calling subscribe with auto and a servicelevel of null or \"\".");
		} else {
			Assert.assertEquals(jsonConsumer.get("serviceLevel"), serviceLevel, "The call to subscribe with auto and a servicelevel persisted the servicelevel setting on the current consumer object.");			
		}

		// assert that each of the autosubscribed entitlements come from a pool that supports the specified service level
		for (EntitlementCert entitlementCert : clienttasks.getCurrentEntitlementCerts()) {

			// tolerate exemptServiceLevels
			if (sm_exemptServiceLevelsInUpperCase.contains(entitlementCert.orderNamespace.supportLevel.toUpperCase())) {
				log.warning("After autosubscribing, this EntitlementCert provides an exempt service level '"+entitlementCert.orderNamespace.supportLevel+"'.");
				continue;
			}
			
			if ((serviceLevel==null || serviceLevel.equals("")) && initialConsumerServiceLevel.equals("")) {
				log.info("When specifying a servicelevel of null or \"\" during an autosubscribe and the current consumer's has no sericelevel preference, then the servicelevel of the granted entitlement certs can be anything.  This one is '"+entitlementCert.orderNamespace.supportLevel+"'.");
			} else if ((serviceLevel==null || serviceLevel.equals("")) && !initialConsumerServiceLevel.equals("")){
				Assert.assertEquals(entitlementCert.orderNamespace.supportLevel,initialConsumerServiceLevel, "When specifying a servicelevel of null or \"\" during an autosubscribe and the current consumer has a sericelevel preference set, then the servicelevel of the granted entitlement certs must match the current consumer's service level preference.");
			} else {
				Assert.assertEquals(entitlementCert.orderNamespace.supportLevel,serviceLevel, "This autosubscribed entitlement was filled from a subscription order that provides the requested service level '"+serviceLevel+"': "+entitlementCert.orderNamespace);
			}
		}
	}
	
	@Test(	description="subscription-manager: after autosubscribing with a service level, assert that another autosubscribe (without specifying service level) uses the service level persisted from the first sutosubscribe",
			groups={},
			dependsOnMethods={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AutoSubscribeAgainAssertingServiceLevelIsPersisted_Test() throws JSONException, Exception {
		// Reference: https://engineering.redhat.com/trac/Entitlement/wiki/SlaSubscribe

		// get all the valid service levels available to this org	
		List<String> serviceLevelsExpected = CandlepinTasks.getServiceLevelsForOrgKey(/*username or*/sm_serverAdminUsername, /*password or*/sm_serverAdminPassword, sm_serverUrl, sm_clientOrg);
		String serviceLevel = serviceLevelsExpected.get(randomGenerator.nextInt(serviceLevelsExpected.size()));
		
		//clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, serviceLevel, null, (String)null, null, false, null, null, null);
		AutoSubscribeWithServiceLevel_Test(null,serviceLevel);

		// return all entitlements
		clienttasks.unsubscribe(true, null, null, null, null);
		
		// autosubscribe again without specifying a service level
		clienttasks.subscribe(true,null,(String)null,(String)null,(String)null,null,null,null,null, null, null);
		
		// get the current consumer object and assert that the serviceLevel persisted even though the subscribe did NOT specify a service level
		Assert.assertEquals(clienttasks.getCurrentServiceLevel(), serviceLevel, "The call to subscribe with auto (without specifying a servicelevel) did not alter current servicelevel.");

		// assert that each of the autosubscribed entitlements come from a pool that supports the original service level
		for (EntitlementCert entitlementCert : clienttasks.getCurrentEntitlementCerts()) {
			
			// tolerate exemptServiceLevels
			if (sm_exemptServiceLevelsInUpperCase.contains(entitlementCert.orderNamespace.supportLevel.toUpperCase())) {
				log.warning("After autosubscribing, this EntitlementCert provides an exempt service level '"+entitlementCert.orderNamespace.supportLevel+"'.");
				continue;
			}

			Assert.assertEquals(entitlementCert.orderNamespace.supportLevel, serviceLevel,"This autosubscribed EntitlementCert was filled from a subscription order that provides the original service level '"+serviceLevel+"': "+entitlementCert.orderNamespace);
		}
	}
	@Test(	description="subscription-manager: subscribe with auto without specifying any service level; assert the service level used matches whatever the consumer's current preference level is set",
			groups={"AcceptanceTests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AutoSubscribeWithNullServiceLevel_Test() throws JSONException, Exception {
		AutoSubscribeWithServiceLevel_Test(null,null);
	}
	@Test(	description="subscription-manager: subscribe with auto specifying a service level of \"\"; assert the service level is unset and the autosubscribe proceeds without any service level preference",
			groups={"AcceptanceTests"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void AutoSubscribeWithBlankServiceLevel_Test() throws JSONException, Exception {
		AutoSubscribeWithServiceLevel_Test(null,"");
	}
	
 
	@Test(	description="subscription-manager: autosubscribe while specifying an valid service level; assert the installed product status is independent of the specified SerViceLeVEL case.",
			groups={"AcceptanceTests"},
			dataProvider="getAllAvailableServiceLevelData",
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyAutoSubscribeWithServiceLevelIsCaseInsensitive_Test(Object bugzulla, String serviceLevel) throws JSONException, Exception {
		
		// system was already registered by dataProvider="getSubscribeWithAutoAndServiceLevelData"
		if (clienttasks.getCurrentConsumerId()==null) {
			clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, null, null, null, (String)null, null, false, null, null, null);
		}
		
		// start fresh by returning all entitlements
		clienttasks.unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions();
	
		// autosubscribe specifying a valid service level and get the installed product status
		List<InstalledProduct> installedProductsAfterAutosubscribedRegisterWithServiceLevel= InstalledProduct.parse(clienttasks.subscribe(true, serviceLevel, (String)null, (String)null, (String)null, null, null, null, null, null, null).getStdout());
		
		// autosubscribe specifying a mixed case service level and get the installed product status
		String mixedCaseServiceLevel = randomizeCaseOfCharactersInString(serviceLevel);
		List<InstalledProduct> installedProductsAfterAutosubscribedRegisterWithMixedCaseServiceLevel= InstalledProduct.parse(clienttasks.subscribe(true, mixedCaseServiceLevel, (String)null, (String)null, (String)null, null, null, null, null, null, null).getStdout());

		// assert that the two lists are identical (independent of the serviceLevel case specified during autosubscribe)
		Assert.assertEquals(installedProductsAfterAutosubscribedRegisterWithMixedCaseServiceLevel.size(), clienttasks.getCurrentProductCertFiles().size(), "The subscribe output displayed the same number of installed product status's as the number of installed product certs.");
		Assert.assertTrue(installedProductsAfterAutosubscribedRegisterWithServiceLevel.containsAll(installedProductsAfterAutosubscribedRegisterWithMixedCaseServiceLevel) && installedProductsAfterAutosubscribedRegisterWithMixedCaseServiceLevel.containsAll(installedProductsAfterAutosubscribedRegisterWithServiceLevel), "Autosubscribe with serviceLevel '"+mixedCaseServiceLevel+"' yielded the same installed product status as autosubscribe with serviceLevel '"+serviceLevel+"'.");
		
		// assert that each of the consumed ProductSubscriptions match the specified service level
		List<ProductSubscription> consumedProductSubscriptions = clienttasks.getCurrentlyConsumedProductSubscriptions();
		if (consumedProductSubscriptions.isEmpty()) log.warning("No entitlements were granted after autosubscribing with service level '"+mixedCaseServiceLevel+"'."); 
		for (ProductSubscription productSubscription : consumedProductSubscriptions) {
			
			// tolerate exemptServiceLevels
			if (sm_exemptServiceLevelsInUpperCase.contains(productSubscription.serviceLevel.toUpperCase())) {
				log.warning("After autosubscribe with service level '"+mixedCaseServiceLevel+"', this consumed ProductSubscription provides an exempt service level '"+productSubscription.serviceLevel+"'.");
				continue;
			}
			
			Assert.assertTrue(productSubscription.serviceLevel.equalsIgnoreCase(mixedCaseServiceLevel),
						"After autosubscribe with service level '"+mixedCaseServiceLevel+"', this consumed ProductSubscription provides a service level '"+productSubscription.serviceLevel+"' that is a case insensitive match to '"+mixedCaseServiceLevel+"'.");
		}
	}
	
	
	
	
	

	// Candidates for an automated Test:
	
	
		
	// Configuration methods ***********************************************************************


	
	
	// Protected methods ***********************************************************************



	
	// Data Providers ***********************************************************************
	

}
