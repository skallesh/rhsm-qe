package rhsm.cli.tests;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.testng.SkipException;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import rhsm.base.ConsumerType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.Repo;
import rhsm.data.SubscriptionPool;
import com.redhat.qe.tools.RemoteFileTasks;

/**
 * @author jsefler
 */
@Test(groups={"HighAvailabilityTests","AcceptanceTests"})
public class HighAvailabilityTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************
	
//	sm.ha.username = stage_test_2
//	sm.ha.password = redhat
//	sm.ha.org = 
//	sm.ha.sku = RH1149049
//	# the sm.ha.packages comes from http://download.devel.redhat.com/nightly/latest-RHEL6.4/6.4/Server/x86_64/os/HighAvailability/listing
//	sm.ha.packages = ccs, cluster-cim, cluster-glue, cluster-glue-libs, cluster-glue-libs-devel, cluster-snmp, clusterlib, clusterlib-devel, cman, corosync, corosynclib, corosynclib-devel, fence-virt, fence-virtd-checkpoint, foghorn, libesmtp-devel, libqb, libqb-devel, libtool-ltdl-devel, luci, modcluster, omping, openais, openaislib, openaislib-devel, pacemaker, pacemaker-cli, pacemaker-cluster-libs, pacemaker-cts, pacemaker-doc, pacemaker-libs, pacemaker-libs-devel, pcs, python-repoze-what-plugins-sql, python-repoze-what-quickstart, python-repoze-who-friendlyform, python-repoze-who-plugins-sa, python-tw-forms, resource-agents, rgmanager, ricci

	
	@Test(	description="register to the stage/prod environment and subscribe to the expected HighAvialability product subscription",
			groups={"blockedByBug-885325"},
			dependsOnMethods={"RegisterRHUIConsumer_Test"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void RegisterAndSubscribeToHighAvailabilitySku_Test() {
		
		if (sm_rhuiUsername.equals("")) throw new SkipException("Skipping this test when no value was given for the RHUI Username");
		// register the RHUI consumer
		clienttasks.register(sm_rhuiUsername,sm_rhuiPassword,sm_rhuiOrg,null,ConsumerType.RHUI,null,null,null,null,null,(String)null,null,null, true, null, null, null, null);

		// assert that the RHUI ProductId is found in the all available list
		List<SubscriptionPool> allAvailableSubscriptionPools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		Assert.assertNotNull(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", sm_rhuiSubscriptionProductId, allAvailableSubscriptionPools), "RHUI Product ID '"+sm_rhuiSubscriptionProductId+"' is available for consumption when the client arch is ignored.");
		
		// assert that the RHUI ProductId is found in the available list only on x86_64,x86 arches
		List<String> supportedArches = Arrays.asList("x86_64","x86","i386","i686");
		List<SubscriptionPool> availableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool rhuiPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", sm_rhuiSubscriptionProductId, availableSubscriptionPools);
		if (!supportedArches.contains(clienttasks.arch)) {
			Assert.assertNull(rhuiPool, "RHUI Product ID '"+sm_rhuiSubscriptionProductId+"' should NOT be available for consumption on a system whose arch ("+clienttasks.arch+") is NOT among the supported arches "+supportedArches);
			throw new SkipException("Cannot consume RHUI Product ID '"+sm_rhuiSubscriptionProductId+"' subscription on a system whose arch ("+clienttasks.arch+") is NOT among the supported arches "+supportedArches);
		}
		Assert.assertNotNull(rhuiPool, "RHUI Product ID '"+sm_rhuiSubscriptionProductId+"' is available for consumption on a system whose arch ("+clienttasks.arch+") is among the supported arches "+supportedArches);

		
		// Subscribe to the RHUI subscription productId
		entitlementCertFile = clienttasks.subscribeToSubscriptionPool(rhuiPool);
	}
	

	
	
	// Candidates for an automated Test:
	// TODO Bug 654442 - pidplugin and rhsmplugin should add to the yum "run with pkgs. list"
	// TODO Bug 705068 - product-id plugin displays "duration"
	// TODO Bug 706265 - product cert is not getting removed after removing all the installed packages from its repo using yum
	// TODO Bug 740773 - product cert lost after installing a pkg from cdn-internal.rcm-test.redhat.com
	// TODO Bug 806457 - If yum runs with no enabled or active repo's, we delete the product cert 
	// TODO Bug 859197 - Product ID Cert Deletion Broken Due to Bad Logging Statement  (CONTAINS SCENARIO FOR PRODUCT CERT DELETION OF 69.pem BY product-id PLUGIN)
	
	
	// Configuration methods ***********************************************************************
	
	
	
	
	// Protected methods ***********************************************************************
	File entitlementCertFile = null;


	
	// Data Providers ***********************************************************************

}
