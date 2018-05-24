package rhsm.cli.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.SubscriptionPool;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 * 
 * Primary scenario comes from https://bugzilla.redhat.com/show_bug.cgi?id=859197
 * 
 * Uses automation properties:
 * 	sm.ha.username = stage_test_2
 *	sm.ha.password = 
 *	sm.ha.org = 
 *	sm.ha.sku = RH1149049
 *
 *  # High Availability Packages
 *  
 *  # x86_64
 *  # RHEL70 http://download.devel.redhat.com/rel-eng/RHEL-7.0-Alpha-3/compose/Server/x86_64/os/addons/HighAvailability/
 *  sm.ha.packages = corosync, corosynclib, corosynclib-devel, dlm, dlm-devel, dlm-lib, ipvsadm, ldirectord, libqb, libqb-devel, libtool-ltdl-devel, lvm2-cluster, omping, pacemaker, pacemaker-cli, pacemaker-cluster-libs, pacemaker-cts, pacemaker-doc, pacemaker-libs, pacemaker-libs-devel, pcs, resource-agents
 *  
 *  # x86_64
 *	# RHEL64 http://download.devel.redhat.com/released/RHEL-6/6.4/Server/x86_64/os/HighAvailability/listing
 *	sm.ha.packages = ccs, cluster-cim, cluster-glue, cluster-glue-libs, cluster-glue-libs-devel, cluster-snmp, clusterlib, clusterlib-devel, cman, corosync, corosynclib, corosynclib-devel, fence-virt, fence-virtd-checkpoint, foghorn, libesmtp-devel, libqb, libqb-devel, libtool-ltdl-devel, luci, modcluster, omping, openais, openaislib, openaislib-devel, pacemaker, pacemaker-cli, pacemaker-cluster-libs, pacemaker-cts, pacemaker-doc, pacemaker-libs, pacemaker-libs-devel, pcs, python-repoze-what-plugins-sql, python-repoze-what-quickstart, python-repoze-who-friendlyform, python-repoze-who-plugins-sa, python-tw-forms, resource-agents, rgmanager, ricci
 *	# RHEL63 http://download.devel.redhat.com/released/RHEL-6/6.3/Server/x86_64/os/HighAvailability/listing
 *	sm.ha.packages = ccs, cluster-cim, cluster-glue, cluster-glue-libs, cluster-glue-libs-devel, cluster-snmp, clusterlib, clusterlib-devel, cman, corosync, corosynclib, corosynclib-devel, fence-virt, fence-virtd-checkpoint, foghorn, libesmtp-devel, libqb, libqb-devel, libtool-ltdl-devel, luci, modcluster, omping, openais, openaislib, openaislib-devel, pacemaker, pacemaker-cli, pacemaker-cluster-libs, pacemaker-libs, pacemaker-libs-devel, python-repoze-what-plugins-sql, python-repoze-what-quickstart, python-repoze-who-friendlyform, python-repoze-who-plugins-sa, python-tw-forms, resource-agents, rgmanager, ricci
 *  # RHEL62 http://download.devel.redhat.com/released/RHEL-6/6.2/Server/x86_64/os/HighAvailability/listing
 *	sm.ha.packages = ccs, cluster-cim, cluster-glue, cluster-glue-libs, cluster-glue-libs-devel, cluster-snmp, clusterlib, clusterlib-devel, cman, corosync, corosynclib, corosynclib-devel, fence-agents, fence-virt, fence-virtd-checkpoint, foghorn, libesmtp-devel, libtool-ltdl-devel, luci, modcluster, omping, openais, openaislib, openaislib-devel, pacemaker, pacemaker-cli, pacemaker-cluster-libs, pacemaker-libs, pacemaker-libs-devel, perl-Net-Telnet, python-repoze-what-plugins-sql, python-repoze-what-quickstart, python-repoze-who-friendlyform, python-repoze-who-plugins-sa, python-suds, python-tw-forms, resource, rgmanager, ricci
 *  # RHEL61 http://download.devel.redhat.com/released/RHEL-6/6.1/Server/x86_64/os/HighAvailability/listing
 *	sm.ha.packages = ccs, cluster-cim, cluster-glue, cluster-glue-libs, cluster-glue-libs-devel, cluster-snmp, clusterlib, clusterlib-devel, cman, corosync, corosynclib, corosynclib-devel, fence-agents, fence-virt, fence-virtd-checkpoint, foghorn, libesmtp, libesmtp-devel, luci, modcluster, omping, openais, openaislib, openaislib-devel, pacemaker, pacemaker-libs, pacemaker-libs-devel, perl-Net-Telnet, python-repoze-what-plugins-sql, python-repoze-what-quickstart, python-repoze-who-friendlyform, python-repoze-who-plugins-sa, python-tw-forms, resource-agents, rgmanager, ricci
 *
 *  # i386
 *	# RHEL64 http://download.devel.redhat.com/released/RHEL-6/6.4/Server/i386/os/HighAvailability/listing
 *	sm.ha.packages = ccs, cluster-cim, cluster-glue, cluster-glue-libs, cluster-glue-libs-devel, cluster-snmp, clusterlib, clusterlib-devel, cman, corosync, corosynclib, corosynclib-devel, fence-virt, fence-virtd-checkpoint, foghorn, libesmtp-devel, libqb, libqb-devel, libtool-ltdl-devel, luci, modcluster, omping, openais, openaislib, openaislib-devel, pacemaker, pacemaker-cli, pacemaker-cluster-libs, pacemaker-cts, pacemaker-doc, pacemaker-libs, pacemaker-libs-devel, pcs, python-repoze-what-plugins-sql, python-repoze-what-quickstart, python-repoze-who-friendlyform, python-repoze-who-plugins-sa, python-tw-forms, resource-agents, rgmanager, ricci
 *	# RHEL63 http://download.devel.redhat.com/released/RHEL-6/6.3/Server/i386/os/HighAvailability/listing
 *	sm.ha.packages = ccs, cluster-cim, cluster-glue, cluster-glue-libs, cluster-glue-libs-devel, cluster-snmp, clusterlib, clusterlib-devel, cman, corosync, corosynclib, corosynclib-devel, fence-virt, fence-virtd-checkpoint, foghorn, libesmtp-devel, libqb, libqb-devel, libtool-ltdl-devel, luci, modcluster, omping, openais, openaislib, openaislib-devel, pacemaker, pacemaker-cli, pacemaker-cluster-libs, pacemaker-cluster-libs-devel, python-repoze-what-plugins-sql, python-repoze-what-quickstart, python-repoze-who-friendlyform, python-repoze-who-plugins-sa, python-tw-forms, resource-agents, rgmanager, ricci
 *  # RHEL62 http://download.devel.redhat.com/released/RHEL-6/6.2/Server/i386/os/HighAvailability/listing
 *	sm.ha.packages = ccs, cluster-cim, cluster-glue, cluster-glue-libs, cluster-glue-libs-devel, cluster-snmp, clusterlib, clusterlib-devel, cman, corosync, corosynclib, corosynclib-devel, fence-agents, fence-virt, fence-virtd-checkpoint, foghorn, libesmtp-devel, libtool, luci, modcluster, omping, openais, openaislib, openaislib-devel, pacemaker, pacemaker-cli, pacemaker-cluster-libs, pacemaker-libs, pacemaker-libs-devel, perl-Net-Telnet, python-repoze-what-plugins-sql, python-repoze-what-quickstart, python-repoze-who-friendlyform, python-repoze-who-plugins-sa, python-suds, python-tw-forms, resource-agents, rgmanager, ricci
 *  # RHEL61 http://download.devel.redhat.com/released/RHEL-6/6.1/Server/i386/os/HighAvailability/listing
 *	sm.ha.packages = ccs, cluster-cim, cluster-glue, cluster-glue-libs, cluster-glue-libs-devel, cluster-snmp, clusterlib, clusterlib-devel, cman, corosync, corosynclib, corosynclib-devel, fence-agents, fence-virt, fence-virtd-checkpoint, foghorn, libesmtp, libesmtp-devel, luci, modcluster, omping, openais, openaislib, openaislib-devel, pacemaker, pacemaker-cli, pacemaker-libs, pacemaker-libs-devel, perl-Net-Telnet, python-repoze-what-plugins-sql, python-repoze-what-quickstart, python-repoze-who-friendlyform, python-repoze-who-plugins-sa, python-tw-forms, resource-agents, rgmanager, ricci
 *
 *  # x86_64 i386 ia64 ppc RHEL57 RHEL58 RHEL59
 *  # http://download.devel.redhat.com/released/RHEL-5-Server/U9/x86_64/os/Cluster/
 *  # http://download.devel.redhat.com/released/RHEL-5-Server/U8/x86_64/os/Cluster/
 *  # http://download.devel.redhat.com/released/RHEL-5-Server/U7/x86_64/os/Cluster/
 *  sm.ha.packages = Cluster_Administration-bn-IN, Cluster_Administration-de-DE, Cluster_Administration-en-US, Cluster_Administration-es-ES, Cluster_Administration-fr-FR, Cluster_Administration-gu-IN, Cluster_Administration-hi-IN, Cluster_Administration-it-IT, Cluster_Administration-ja-JP, Cluster_Administration-kn-IN, Cluster_Administration-ko-KR, Cluster_Administration-ml-IN, Cluster_Administration-mr-IN, Cluster_Administration-or-IN, Cluster_Administration-pa-IN, Cluster_Administration-pt-BR, Cluster_Administration-ru-RU, Cluster_Administration-si-LK, Cluster_Administration-ta-IN, Cluster_Administration-te-IN, Cluster_Administration-zh-CN, Cluster_Administration-zh-TW, cluster-cim, cluster-snmp, ipvsadm, luci, modcluster, piranha, rgmanager, ricci, system-config-cluster
 */
@Test(groups={"HighAvailabilityTests"})
public class HighAvailabilityTests extends SubscriptionManagerCLITestScript {

	// Test methods ***********************************************************************


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20070", "RHEL7-55179"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="make sure there are no High Availability packages installed",
			groups={"Tier1Tests","blockedByBug-904193"},
			priority=10,
			dependsOnMethods={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testHighAvailabilityIsNotInstalled() {
		
		// yum clean all to ensure the yum database is reset
		clienttasks.yumClean("all");
		
		// verify no High Availability packages are installed
		boolean haPackagesInstalled = false;
		for (String pkg: /*sm_haPackages*/getHighAvailabilityPackages(clienttasks.redhatReleaseXY, clienttasks.arch)) {
			if (clienttasks.isPackageInstalled(pkg)) {
				haPackagesInstalled = true;
				log.warning("Did not expect High Availability package '"+pkg+"' to be instaled.");				
			}
		}
		Assert.assertTrue(!haPackagesInstalled,"There should NOT be any packages from HighAvialability installed on a fresh install of RHEL '"+clienttasks.releasever+"'.");
		
		// get the currently installed products
		List <InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		
		// verify High Availability product id is not installed
		InstalledProduct haInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", haProductId, installedProducts);
		Assert.assertNull(haInstalledProduct, "The High Availability product id '"+haProductId+"' should NOT be installed.");

		// verify RHEL product server id 69 is installed
		InstalledProduct serverInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", serverProductId, installedProducts);
		Assert.assertNotNull(serverInstalledProduct, "The RHEL Server product id '"+serverProductId+"' should be installed.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20071", "RHEL7-55180"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify product database and installed products are in sync",
			groups={"Tier1Tests"},
			priority=12,
			dependsOnMethods={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testProductDatabaseIsInSyncWithInstalledProducts() throws JSONException {
		
		// get the installed products and product database map
		List<InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();
		Map<String,List<String>> productIdRepoMap = clienttasks.getProductIdToReposMap();
		//List<ProductCert> installedProductCerts = clienttasks.getCurrentProductCerts();	// VALID BEFORE Bugs 1080007 1080012 - [RFE] Include default product certificate in redhat-release
		List<ProductCert> installedProductCerts = clienttasks.getProductCerts(clienttasks.productCertDir);
		
		// assert that product database and installed products are in sync
		int installedProductCertCount=0;
		for (ProductCert installedProductCert: installedProductCerts) {
			// skip productCerts from TESTDATA
			if (installedProductCert.file.getName().endsWith("_.pem")) {
				log.info("Skipping assertion that product cert '"+installedProductCert.file+"' (manually installed from generated candlepin TESTDATA) is accounted for in the product database '"+clienttasks.productIdJsonFile+"'.");
				continue;
			}
			// TEMPORARY WORKAROUND
			if (installedProductCert.productId.equals("135") /* Red Hat Enterprise Linux 6 Server HTB */ || installedProductCert.productId.equals("155") /* Red Hat Enterprise Linux 6 Workstation HTB */) {
				List<ProductCert> installedProductDefaultCerts = clienttasks.getProductCerts(clienttasks.productCertDefaultDir);
				if (ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", installedProductCert.productId, installedProductDefaultCerts) != null) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="1318584"; // Bug 1318584 - /etc/pki/product-default/*.pem missing in certain variants 
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
					if (invokeWorkaroundWhileBugIsOpen) {
						// only skip when the installed HTB product came from the /etc/pki/product-default location due to bug 1318584
						log.warning("Skipping assertion that Database '"+clienttasks.productIdJsonFile+"' maps installed product id '"+installedProductCert.productId+"' while bug '"+bugId+"' is open.");
						continue;
					}
				}
			}
			// END OF WORKAROUND
			
			installedProductCertCount++;
			Assert.assertTrue(productIdRepoMap.containsKey(installedProductCert.productId), "Database '"+clienttasks.productIdJsonFile+"' contains installed product id: "+installedProductCert.productId);
			log.info("Database '"+clienttasks.productIdJsonFile+"' maps installed product id '"+installedProductCert.productId+"' to repo '"+productIdRepoMap.get(installedProductCert.productId)+"'.");
		}
		for (String productId : productIdRepoMap.keySet()) {
			Assert.assertNotNull(InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", productId, installedProducts), "Database '"+clienttasks.productIdJsonFile+"' product id '"+productId+"' is among the installed products.");
		}
		Assert.assertEquals(productIdRepoMap.keySet().size(), installedProductCertCount, "The product id database size matches the number of installed products (excluding TESTDATA products).");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20072", "RHEL7-55181"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="register to the stage/prod environment with credentials to access High Availability product subscription",
			groups={"Tier1Tests"},
			priority=14,
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRegisterToHighAvailabilityAccount() {
		if (sm_haUsername.equals("")) throw new SkipException("Skipping this test when no value was given for the High Availability Username");

		// register the to an account that offers High Availability subscriptions
		clienttasks.register(sm_haUsername,sm_haPassword,sm_haOrg,null,null,null,null,null,null,null,(String)null,null,null, null, true, null, null, null, null, null);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20073", "RHEL7-55183"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify that a local yum install will not delete the product database when repolist is empty",
			groups={"Tier1Tests","blockedByBug-806457"},
			priority=16,
			dependsOnMethods={"testRegisterToHighAvailabilityAccount"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testLocalYumInstallAndRemoveDoesNotAlterInstalledProducts() throws JSONException {
		List<InstalledProduct> originalInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
		String originalProductIdJSONString = client.runCommandAndWait("cat "+clienttasks.productIdJsonFile).getStdout();
		
		// assert that there are no active repos
		Assert.assertEquals(clienttasks.getYumRepolist("enabled").size(), 0, "Expected number of enabled yum repositories.");
		
		// get an rpm to perform a local yum install
		String localRpmFile =  String.format("/tmp/%s.rpm",haPackage1);
		RemoteFileTasks.runCommandAndAssert(client, String.format("wget -O %s %s",localRpmFile,haPackage1Fetch), new Integer(0));
		
		// do a yum local install and assert
		SSHCommandResult yumInstallResult = client.runCommandAndWait(String.format("yum -y --quiet --nogpgcheck localinstall %s",localRpmFile));
		Assert.assertTrue(clienttasks.isPackageInstalled(haPackage1),"Local install of package '"+haPackage1+"' completed successfully.");
		
		// verify that installed products remains unchanged
		List<InstalledProduct> currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
		Assert.assertTrue(currentlyInstalledProducts.containsAll(originalInstalledProducts) && originalInstalledProducts.containsAll(currentlyInstalledProducts), "The installed products remains unchanged after a yum local install of package '"+haPackage1+"'.");
		
		// verify that the current product id database was unchanged
		String currentProductIdJSONString = client.runCommandAndWait("cat "+clienttasks.productIdJsonFile).getStdout();
		Assert.assertEquals(currentProductIdJSONString, originalProductIdJSONString, "The product id to repos JSON database file remains unchanged after a yum local install of package '"+haPackage1+"'.");

		// finally remove the locally installed rpm
		clienttasks.yumRemovePackage(haPackage1);
		
		// verify that installed products remains unchanged
		currentlyInstalledProducts = clienttasks.getCurrentlyInstalledProducts();
		Assert.assertTrue(currentlyInstalledProducts.containsAll(originalInstalledProducts) && originalInstalledProducts.containsAll(currentlyInstalledProducts), "The installed products remains unchanged after a yum removal of locally installed package '"+haPackage1+"'.");

		// verify that the current product id database was unchanged
		currentProductIdJSONString = client.runCommandAndWait("cat "+clienttasks.productIdJsonFile).getStdout();
		Assert.assertEquals(currentProductIdJSONString, originalProductIdJSONString, "The product id to repos JSON database file remains unchanged after a yum removal of locally installed package '"+haPackage1+"'.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20074", "RHEL7-55182"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="subscribe to the expected High Availability product subscription",
			groups={"Tier1Tests"},
			priority=20,
			//dependsOnMethods={"testHighAvailabilityIsNotInstalled"},
			dependsOnMethods={"testLocalYumInstallAndRemoveDoesNotAlterInstalledProducts"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testSubscribeToHighAvailabilitySKU() {
		String haSku = getHighAvailabilitySku(clienttasks.arch);
		
		// assert that the High Availability subscription SKU is found in the all available list
		List<SubscriptionPool> allAvailableSubscriptionPools = clienttasks.getCurrentlyAllAvailableSubscriptionPools();
		Assert.assertNotNull(SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", haSku, allAvailableSubscriptionPools), "High Availability subscription SKU '"+haSku+"' is available for consumption when the client arch is ignored.");
		
		// assert that the High Availability subscription SKU is found in the available list only on x86_64,x86 arches; see https://docspace.corp.redhat.com/docs/DOC-63084
		List<SubscriptionPool> availableSubscriptionPools = clienttasks.getCurrentlyAvailableSubscriptionPools();
		SubscriptionPool haPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", haSku, availableSubscriptionPools);
		if (!haSupportedArches.contains(clienttasks.arch)) {
			Assert.assertNull(haPool, "High Availability subscription SKU '"+haSku+"' should NOT be available for consumption on a system whose arch '"+clienttasks.arch+"' is NOT among the supported arches "+haSupportedArches);
			throw new SkipException("Cannot consume High Availability subscription SKU '"+haSku+"' on a system whose arch '"+clienttasks.arch+"' is NOT among the supported arches "+haSupportedArches);
		}
		Assert.assertNotNull(haPool, "High Availability subscription SKU '"+haSku+"' is available for consumption on a system whose arch '"+clienttasks.arch+"' is among the supported arches "+haSupportedArches);
		
		// Subscribe to the High Availability subscription SKU
		haEntitlementCertFile = clienttasks.subscribeToSubscriptionPool(haPool,/*sm_serverAdminUsername*/sm_haUsername,/*sm_serverAdminPassword*/sm_haPassword,sm_serverUrl);
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20075", "RHEL7-55185"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify the expected High Availability packages are availabile for yum install",
			groups={"Tier1Tests"},
			priority=30,
			dependsOnMethods={"testSubscribeToHighAvailabilitySKU"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testHighAvailabilityPackagesAreAvailabile() {
		
		// INFO: rhel-ha-for-rhel-7-server-rpms/7Server/x86_64 is enabled by default
		// NOT ANYMORE, WE NOW NEED TO ENABLE THE ADDON REPO (A GOOD CHANGE BY REL-ENG DURING THE RHEL7.4 TEST PHASE, AND APPLIED TO ALL RELEASES)
		if (/*clienttasks.redhatReleaseX.equals("7") && */clienttasks.arch.equals("x86_64")) {
			clienttasks.repos(null, null, null, "rhel-ha-for-rhel-"+clienttasks.redhatReleaseX+"-server-rpms", null, null, null, null, null);
		}
		
		// INFO: rhel-ha-for-rhel-7-for-system-z-rpms/7Server/s390x is NOT enabled by default
		if (clienttasks.redhatReleaseX.equals("7") && clienttasks.arch.equals("s390x")) {
			clienttasks.repos(null, null, null, "rhel-ha-for-rhel-7-for-system-z-rpms", null, null, null, null, null);
		}
		
		List<String> availablePackages = clienttasks.getYumListAvailable(null);
		boolean foundAllExpectedPkgs = true;
		for (String expectedPkg: /*sm_haPackages*/getHighAvailabilityPackages(clienttasks.redhatReleaseXY, clienttasks.arch)) {
			boolean foundExpectedPkg = false;
			for (String availablePkg: availablePackages) {	// availablePackages are suffixed by their arch like this: ccs.x86_64 libesmtp.i686 python-tw-forms.noarch
				if (availablePkg.startsWith(expectedPkg+".")) {
					Assert.assertTrue(true, "High Availability package '"+expectedPkg+"' is available for yum install as '"+availablePkg+"'.");
					foundExpectedPkg = true;
				}
			}
			if (!foundExpectedPkg) {
				foundAllExpectedPkgs = false;
				log.warning("Expected High Availability package '"+expectedPkg+"' to be available for yum install.");
			}
		}
		Assert.assertTrue(foundAllExpectedPkgs,"All expected High Availability packages are available for yum install.");	// see top of this file for determining the expected sm_haPackages
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20076", "RHEL7-55186"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="yum install a High Availability package ccs and assert installed products",
			groups={"Tier1Tests","blockedByBug-859197","blockedByBug-958548","blockedByBug-1004893"},
			priority=40,
			//dependsOnMethods={"testHighAvailabilityPackagesAreAvailabile"},
			dependsOnMethods={"testSubscribeToHighAvailabilitySKU"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumInstallFirstHighAvailabilityPackageAndAssertInstalledProductCerts() {
		clienttasks.yumInstallPackage(haPackage1);
		
		// get the currently installed products
		List <InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();

		// verify High Availability product id is now installed and Subscribed
		InstalledProduct haInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", haProductId, installedProducts);
		Assert.assertNotNull(haInstalledProduct, "The High Availability product id '"+haProductId+"' should be installed after successful install of High Availability package '"+haPackage1+"'.");
		Assert.assertEquals(haInstalledProduct.status, "Subscribed", "The status of the installed High Availability product cert.");

		// verify RHEL product server id 69 is installed
		InstalledProduct serverInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", serverProductId, installedProducts);
		Assert.assertNotNull(serverInstalledProduct, "The RHEL Server product id '"+serverProductId+"' should still be installed after successful install of High Availability package '"+haPackage1+"'.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20077", "RHEL7-55184"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="yum install a second High Availability package cman and assert installed products",
			groups={"Tier1Tests","blockedByBug-859197","blockedByBug-958548","blockedByBug-1004893"},
			priority=50,
			//dependsOnMethods={"testYumInstallFirstHighAvailabilityPackageAndAssertInstalledProductCerts"},
			dependsOnMethods={"testSubscribeToHighAvailabilitySKU"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumInstallSecondHighAvailabilityPackageAndAssertInstalledProductCerts() {
		clienttasks.yumInstallPackage(haPackage2);
		
		// get the currently installed products
		List <InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();

		// verify High Availability product id is now installed and Subscribed
		InstalledProduct haInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", haProductId, installedProducts);
		Assert.assertNotNull(haInstalledProduct, "The High Availability product id '"+haProductId+"' should be installed after successful install of High Availability package '"+haPackage2+"'.");
		Assert.assertEquals(haInstalledProduct.status, "Subscribed", "The status of the installed High Availability product cert.");

		// verify RHEL product server id 69 is installed
		InstalledProduct serverInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", serverProductId, installedProducts);
		Assert.assertNotNull(serverInstalledProduct, "The RHEL Server product id '"+serverProductId+"' should still be installed after successful install of High Availability package '"+haPackage2+"'.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20078", "RHEL7-55187"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="yum remove second High Availability package cman and assert installed products",
			groups={"Tier1Tests"},
			priority=60,
			dependsOnMethods={"testYumInstallSecondHighAvailabilityPackageAndAssertInstalledProductCerts"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumRemoveSecondHighAvailabilityPackageAndAssertInstalledProductCerts() {
		clienttasks.yumRemovePackage(haPackage2);
		
		// get the currently installed products
		List <InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();

		// verify High Availability product id remains installed and Subscribed
		InstalledProduct haInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", haProductId, installedProducts);
		Assert.assertNotNull(haInstalledProduct, "The High Availability product id '"+haProductId+"' should remain installed after successful removal of High Availability package '"+haPackage2+"' (because package "+haPackage1+" is still installed).");
		Assert.assertEquals(haInstalledProduct.status, "Subscribed", "The status of the installed High Availability product cert.");

		// verify RHEL product server id 69 is installed
		InstalledProduct serverInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", serverProductId, installedProducts);
		Assert.assertNotNull(serverInstalledProduct, "The RHEL Server product id '"+serverProductId+"' should remain installed after successful removal of High Availability package '"+haPackage2+"'.");
	}


	@TestDefinition(//update=true	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20079", "RHEL7-55188"},
			level= DefTypes.Level.COMPONENT,
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="yum remove first High Availability package cman and assert installed products",
			groups={"Tier1Tests","blockedByBug-859197"},
			priority=70,
			dependsOnMethods={"testYumInstallFirstHighAvailabilityPackageAndAssertInstalledProductCerts"},
			//dependsOnMethods={"testYumRemoveSecondHighAvailabilityPackageAndAssertInstalledProductCerts"},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumRemoveFirstHighAvailabilityPackageAndAssertInstalledProductCerts() {
		
		ProductCert haProductCert = ProductCert.findFirstInstanceWithMatchingFieldFromList("productId",haProductId, clienttasks.getCurrentProductCerts());	
		// assemble all of the provided tags from the haProductCert
		List<String> haProductCertProvidedTags = Arrays.asList(haProductCert.productNamespace.providedTags.split("\\s*,\\s*"));
		boolean haProductCertProvidesATagStartingWithRhel = false;
		for (String tag : haProductCertProvidedTags) {
			if (tag.toLowerCase().startsWith("rhel")) {
				log.info("Found HA ProductCert tag '"+tag+"' that begins with rhel*.");
				haProductCertProvidesATagStartingWithRhel = true;
			}
		}
		
		// remove the final package installed from the ha repo
		clienttasks.yumRemovePackage(haPackage1);
		
		// get the currently installed products
		List <InstalledProduct> installedProducts = clienttasks.getCurrentlyInstalledProducts();

		// verify High Availability product id is uninstalled
		// but it should only be uninstalled if it does not provide an rhel* tags as stated in https://bugzilla.redhat.com/show_bug.cgi?id=859197#c15
		InstalledProduct haInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", haProductId, installedProducts);
		/* valid before https://bugzilla.redhat.com/show_bug.cgi?id=859197#c15
		if (Integer.valueOf(clienttasks.redhatReleaseX) > 5) Assert.assertNull(haInstalledProduct, "The High Availability product id '"+haProductId+"' should no longer be installed after successful removal of High Availability package '"+haPackage1+"' (because no High Availability packages should be installed).");
		else												 Assert.assertNotNull(haInstalledProduct, "The High Availability product id '"+haProductId+"' should no longer be installed after successful removal of High Availability package '"+haPackage1+"' (because no High Availability packages should be installed); HOWEVER on RHEL5 the productId plugin does NOT remove product certs.  This is a known issue.");
		*/
		if (haProductCertProvidesATagStartingWithRhel) {
			Assert.assertNotNull(haInstalledProduct, "The High Availability product id '"+haProductId+"' should remain installed despite successful removal of the final High Availability package '"+haPackage1+"' (because the High Availability product cert provides a tag that starts with rhel* even when the last High Availability package installed was removed).");
		} else {
			Assert.assertNull(haInstalledProduct, "The High Availability product id '"+haProductId+"' should no longer be installed after successful removal of High Availability package '"+haPackage1+"' (because no High Availability packages should be installed AND because none of its provided tags start with rhel*).");			
		}
		
		// verify RHEL product server id 69 remains installed
		InstalledProduct serverInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", serverProductId, installedProducts);
		Assert.assertNotNull(serverInstalledProduct, "The RHEL Server product id '"+serverProductId+"' should remain installed after successful removal of High Availability package '"+haPackage1+"'.");
	}
	
	
	
	
	
	
	
	// Candidates for an automated Test:
	// TODO Bug 654442 - pidplugin and rhsmplugin should add to the yum "run with pkgs. list" https://github.com/RedHatQE/rhsm-qe/issues/156
	// TODO Bug 705068 - product-id plugin displays "duration" https://github.com/RedHatQE/rhsm-qe/issues/157
	// TODO Bug 706265 - product cert is not getting removed after removing all the installed packages from its repo using yum https://github.com/RedHatQE/rhsm-qe/issues/158
	// TODO Bug 740773 - product cert lost after installing a pkg from cdn-internal.rcm-test.redhat.com https://github.com/RedHatQE/rhsm-qe/issues/159
	
	/* 
	------------------------------------------------------------
	Notes from alikins
	summary: if you register, and subscribe to say, openshift
	(but not rhel), and install something from openshift, you
	can have your productid deleted as inactive. This doesn't
	seem to be a new behaviour. If you are subscribe to rhel
	(even with no rhel packages installed), you seem to be okay.
	
	The openshift guys have ran into this with 6.3, and I belive 6.4 betas.
	There scenario was:
	
	install rhel
	register
	subscribe to rhel (I belive...)
	subscribe to openshift
	yum install something from openshift
	*boom* rhel product id goes away
	
	MUST WE BE SUBSCRIBED TO RHEL?
	> Testing this, it seems that if rhel is just subscribe to (no packages
	> installed or updated) it seems to be okay.
	>
	> However, I'm not really sure I understand why...
	> Adrian
	>
	>
	
	Our logic is this:
	
	if you have a subscription, and if you have a product id which is not
	backed by a susbcription repo hen we should remove it. I think we need
	to change this logic. Is there a bug open for this? If not, I will.
	-- bk

	----------------------------------------------------------------------
	A couple of thoughts from dgregor regarding bug 859197:

	* A product cert should only be removed when there are no packages left
	on the system that came from the corresponding repo.  So, in the above
	example, as long as there are packages on the system that came from
	anaconda-RedHatEnterpriseLinux-201211201732.x86_64, the product cert
	should stay regardless of whether
	anaconda-RedHatEnterpriseLinux-201211201732.x86_64 is still a defined
	repository.

	* Do we ever associate a product cert with multiple repos?  Let's say I
	install a package from rhel-ha-for-rhel-6-server-beta-rpms and that
	pulls in product cert 83.  I then install a second package from
	rhel-ha-for-rhel-6-server-rpms, which is also mapped to cert 83.  If I
	remove that first package, we still want cert 83 on disk.

	-- Dennis
	HOW TO DETERMINE WHAT REPO A PACKAGE CAME FROM

	# yum history package-info filesystem
	Transaction ID : 1
	Begin time     : Sat Oct 13 19:38:17 2012
	Package        : filesystem-2.4.30-3.el6.x86_64
	State          : Install
	Size           : 0
	Build host     : x86-004.build.bos.redhat.com
	Build time     : Tue Jun 28 10:13:32 2011
	Packager       : Red Hat, Inc. <http://bugzilla.redhat.com/bugzilla>
	Vendor         : Red Hat, Inc.
	License        : Public Domain
	URL            : https://fedorahosted.org/filesystem
	Source RPM     : filesystem-2.4.30-3.el6.src.rpm
	Commit Time    : Tue Jun 28 08:00:00 2011
	Committer      : Ondrej Vasik <ovasik@redhat.com>
	Reason         : user
	>From repo      : anaconda-RedHatEnterpriseLinux-201206132210.x86_64
	Installed by   : System <unset>
	
	
	*/
	
	// Configuration methods ***********************************************************************
	
	@BeforeClass(groups="setup")
	public void assertRhelServerBeforeClass() {
		if (clienttasks==null) return;
		if (!clienttasks.releasever.contains("Server")) {	// "5Server" or "6Server"
			throw new SkipException("High Availability tests are only executable on a RHEL Server.");
		}
		
		// Skip distributions of RHEL that do not support High Availability
		ProductCert rhelProductCert = clienttasks.getCurrentRhelProductCert();
		if (Arrays.asList(new String[]{
				"294",	/* Red Hat Enterprise Linux Server for ARM */
				"419",	/* Red Hat Enterprise Linux for ARM 64 */
				"420",	/* Red Hat Enterprise Linux for Power 9 */
				"434",	/* Red Hat Enterprise Linux for IBM System z (Structure A) */
				"363",	/* Red Hat Enterprise Linux for ARM 64 Beta */
				"362",	/* Red Hat Enterprise Linux for Power 9 Beta */
				"433",	/* Red Hat Enterprise Linux for IBM System z (Structure A) Beta */
				}).contains(rhelProductCert.productId)) {
			throw new SkipException("High Availability is not offered on '"+rhelProductCert.productName+"'");
		}
		
		//	[root@ibm-js22-vios-02-lp2 ~]# curl -k -u stage_test_2:PASSWORD http://rubyvip.web.stage.ext.phx2.redhat.com:80/clonepin/candlepin/pools/8a99f9843c01ccba013c037a0fa0015a | python -m simplejson/tool
		//    "productAttributes": [
		//                          {
		//                              "created": "2013-07-22T19:47:22.000+0000", 
		//                              "id": "8a99f98340076c17014007ec04435b89", 
		//                              "name": "arch", 
		//                              "productId": "RH1149049", 
		//                              "updated": "2013-07-22T19:47:22.000+0000", 
		//                              "value": "x86,x86_64,ia64,ppc,ppc64"       <====  NOTE THAT RH1149049 WILL BE AVAILABLE FOR CONSUMPTION ON THESE ARCHES FOR ALL RELEASES OF RHEL (RHEL6 Server ppc64 WILL NOT HAVE ANY CONTENT)
		//                          }, 
		
		serverProductId = "69";	// Red Hat Enterprise Linux Server
		haProductId		= "83"; // Red Hat Enterprise Linux High Availability (for RHEL Server)
		
		if (clienttasks.arch.equals("x86_64")) {
			if (clienttasks.redhatReleaseX.equals("5")) haPackage1Fetch = "http://download.devel.redhat.com/released/RHEL-5-Server/U7/x86_64/os/Cluster/ipvsadm-1.24-13.el5.x86_64.rpm";
			if (clienttasks.redhatReleaseX.equals("6")) haPackage1Fetch = "http://download.devel.redhat.com/released/RHEL-6/6.1/Server/x86_64/os/Packages/ccs-0.16.2-35.el6.x86_64.rpm";
			if (clienttasks.redhatReleaseX.equals("7")) haPackage1Fetch = "http://download.devel.redhat.com/released/RHEL-7/7.0/Server/x86_64/os/addons/HighAvailability/omping-0.0.4-6.el7.x86_64.rpm";
			serverProductId = rhelProductCert.productId;	// is usually 69; but could be HTB 230 on RHEL7 or 135 on RHEL6
		}
		if (clienttasks.arch.startsWith("i")) {			// i386 i686
			if (clienttasks.redhatReleaseX.equals("5")) haPackage1Fetch = "http://download.devel.redhat.com/released/RHEL-5-Server/U7/i386/os/Cluster/ipvsadm-1.24-13.el5.i386.rpm";
			if (clienttasks.redhatReleaseX.equals("6")) haPackage1Fetch = "http://download.devel.redhat.com/released/RHEL-6/6.1/Server/i386/os/Packages/ccs-0.16.2-35.el6.i686.rpm";
			serverProductId = rhelProductCert.productId;	// is usually 69;
		}
		if (clienttasks.arch.equals("ia64")) {
			if (clienttasks.redhatReleaseX.equals("5")) haPackage1Fetch = "http://download.devel.redhat.com/released/RHEL-5-Server/U7/ia64/os/Cluster/ipvsadm-1.24-13.el5.ia64.rpm";
		}
		if (clienttasks.arch.startsWith("ppc")) {		// ppc ppc64
			serverProductId = "74";	// Red Hat Enterprise Linux for IBM POWER
			if (clienttasks.redhatReleaseX.equals("5")) haPackage1Fetch = "http://download.devel.redhat.com/released/RHEL-5-Server/U7/ppc/os/Cluster/ipvsadm-1.24-13.el5.ppc.rpm";
			if (clienttasks.redhatReleaseX.equals("6")) throw new SkipException("Although available for consumption, High Availability content is not offered on RHEL6 arch '"+clienttasks.arch+"'.");
			if (clienttasks.redhatReleaseX.equals("7")) throw new SkipException("Although available for consumption, High Availability content is not offered on RHEL7 arch '"+clienttasks.arch+"'.");
		}
		if (clienttasks.arch.startsWith("s390")) {		// s390 s390x
			serverProductId = "72";	// Red Hat Enterprise Linux for IBM System z
			if (clienttasks.redhatReleaseX.equals("5")) throw new SkipException("High Availability is not offered on arch '"+clienttasks.arch+"'.");
			if (clienttasks.redhatReleaseX.equals("6")) throw new SkipException("High Availability is not offered on arch '"+clienttasks.arch+"'.");
			if (clienttasks.redhatReleaseX.equals("7")) {
				haProductId = "300"; // Red Hat Enterprise Linux High Availability (for IBM z Systems)
				haPackage1Fetch = "http://download.devel.redhat.com/released/RHEL-7/7.2/Server/s390x/os/addons/HighAvailability/omping-0.0.4-6.el7.s390x.rpm";
			}
			if (Float.valueOf(clienttasks.redhatReleaseXY)<7.2) throw new SkipException("High Availability is not offered on arch '"+clienttasks.arch+"' release '"+clienttasks.redhatReleaseXY+"'.");
		}
		
		if (clienttasks.redhatReleaseX.equals("5")) {
			haPackage1	= "ipvsadm";	// or  Cluster_Administration-as-IN
			haPackage2	= "system-config-cluster";	// or Cluster_Administration-bn-IN
			haSupportedArches	= Arrays.asList("x86_64","x86","i386","i686","ia64","ppc","ppc64");
		}
		if (clienttasks.redhatReleaseX.equals("6")) {
			haPackage1	= "ccs";
			haPackage2	= "cluster-glue-libs";
			haSupportedArches	= Arrays.asList("x86_64","x86","i386","i686");
		}
		if (clienttasks.redhatReleaseX.equals("7")) {
			haPackage1	= "omping";
			haPackage2	= "resource-agents";	// 03/2017 started failing due to dependency... Error: Package: resource-agents-3.9.5-82.el7_3.6.x86_64 (rhel-ha-for-rhel-7-server-rpms) Requires: /usr/sbin/mount.cifs
			haPackage2	= "libqb";
			haSupportedArches	= Arrays.asList("x86_64","s390x");
		}
	}

	@BeforeClass(groups="setup",dependsOnMethods={"assertRhelServerBeforeClass"})
	public void configProductCertDirBeforeClass() {
		if (clienttasks==null) return;
		originalProductCertDir = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "productCertDir");
		Assert.assertNotNull(originalProductCertDir);
		log.info("Initializing a new product cert directory with the currently installed product certs for this test class...");
		RemoteFileTasks.runCommandAndAssert(client,"mkdir -p "+haProductCertDir,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(client,"rm -f "+haProductCertDir+"/*.pem",Integer.valueOf(0));
		if (!RemoteFileTasks.runCommandAndAssert(client,"ls -A "+clienttasks.productCertDir,Integer.valueOf(0)).getStdout().isEmpty()) {
			RemoteFileTasks.runCommandAndAssert(client,"cp "+clienttasks.productCertDir+"/*.pem "+haProductCertDir,Integer.valueOf(0));
		}
		RemoteFileTasks.runCommandAndAssert(client,"cp --no-clobber "+clienttasks.productCertDefaultDir+"/*.pem "+haProductCertDir,Integer.valueOf(0),Integer.valueOf(1));

		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", haProductCertDir);
	}
	
	@BeforeClass(groups="setup",dependsOnMethods={"assertRhelServerBeforeClass"})
	public void backupProductIdJsonFileBeforeClass() {
		if (clienttasks==null) return;
		RemoteFileTasks.runCommandAndAssert(client,"cat "+clienttasks.productIdJsonFile+" > "+backupProductIdJsonFile, Integer.valueOf(0));
	}
	
	@BeforeClass(groups={"setup"},dependsOnMethods={"backupProductIdJsonFileBeforeClass","configProductCertDirBeforeClass"})
	public void disableAllRepos() {
		// by default on Beaker provisioned hardware, there are numerous beaker-* enabled repos that interfere with this test class
		clienttasks.yumDisableAllRepos("--disableplugin=product-id --disableplugin=subscription-manager");	// disabling these plugins as insurance to avoid product-id contamination before the tests run
	}
	
	
	
	@AfterClass(groups="setup")
	public void unregisterAfterClass() {
		if (clienttasks==null) return;
		client.runCommandAndWait("yum remove "+haPackage1+" "+haPackage2+" -y --disableplugin=rhnplugin"); // or remove all sm_haPackages
		clienttasks.unregister_(null, null, null, null);
	}
	
	@AfterClass(groups="setup", dependsOnMethods={"unregisterAfterClass"})
	public void unconfigProductCertDirAfterClass() {
		if (clienttasks==null) return;
		if (originalProductCertDir==null) return;	
		log.info("Restoring the originally configured product cert directory...");
		clienttasks.updateConfFileParameter(clienttasks.rhsmConfFile, "productCertDir", originalProductCertDir);
	}
	
	@AfterClass(groups="setup", dependsOnMethods={"unregisterAfterClass"})
	public void restoreProductIdJsonFileAfterClass() {
		if (clienttasks==null) return;
		if (!RemoteFileTasks.testExists(client, backupProductIdJsonFile)) return;
		RemoteFileTasks.runCommandAndAssert(client,"cat "+backupProductIdJsonFile+" > "+clienttasks.productIdJsonFile, Integer.valueOf(0));
		clienttasks.yumClean("all");
	}
	
	
	
	
	// Protected methods ***********************************************************************

	protected String originalProductCertDir			= null;
	protected final String haProductCertDir			= "/tmp/sm-haProductCertDir";
	protected final String backupProductIdJsonFile	= "/tmp/sm-productIdJsonFile";
	protected String serverProductId				= null;	// set in assertRhelServerBeforeClass()
	protected String haPackage1						= null;	// set in assertRhelServerBeforeClass()
	protected String haPackage1Fetch				= null;	// set in assertRhelServerBeforeClass()	// released RHEL61 package to wget for testing bug 806457
	protected String haPackage2						= null;	// set in assertRhelServerBeforeClass()
	File haEntitlementCertFile						= null;
	public List<String> haSupportedArches			= null; // set in assertRhelServerBeforeClass()
	protected String haProductId					= null;	// set in assertRhelServerBeforeClass()

	/**
	 * @param redhatReleaseXY
	 * @param arch
	 * @return an expected list of High Availability packages on the CDN for the requested RHEL release and arch 
	 */
	static List<String> getHighAvailabilityPackages(String redhatReleaseXY, String arch) {
		List<String> haPackages=new ArrayList<String>();
		
		Integer redhatReleaseX = Integer.valueOf(redhatReleaseXY.split("\\.")[0]);
		Integer redhatReleaseY = Integer.valueOf(redhatReleaseXY.split("\\.")[1]);
		
		if (redhatReleaseX>=5) {	// rhel5
			if (arch.equals("x86_64")||arch.equals("i386")) {
				haPackages = Arrays.asList(new String[]{"Cluster_Administration-bn-IN", "Cluster_Administration-de-DE", "Cluster_Administration-en-US", "Cluster_Administration-es-ES", "Cluster_Administration-fr-FR", "Cluster_Administration-gu-IN", "Cluster_Administration-hi-IN", "Cluster_Administration-it-IT", "Cluster_Administration-ja-JP", "Cluster_Administration-kn-IN", "Cluster_Administration-ko-KR", "Cluster_Administration-ml-IN", "Cluster_Administration-mr-IN", "Cluster_Administration-or-IN", "Cluster_Administration-pa-IN", "Cluster_Administration-pt-BR", "Cluster_Administration-ru-RU", "Cluster_Administration-si-LK", "Cluster_Administration-ta-IN", "Cluster_Administration-te-IN", "Cluster_Administration-zh-CN", "Cluster_Administration-zh-TW", "cluster-cim", "cluster-snmp", "ipvsadm", "luci", "modcluster", "piranha", "rgmanager", "ricci", "system-config-cluster"});
			}
		}
		if (redhatReleaseX>=6 && redhatReleaseY>=5) {	// rhel6.5
			if (arch.equals("x86_64")||arch.equals("i386")) {
				haPackages = Arrays.asList(new String[]{"ccs", "cluster-cim", "cluster-glue", "cluster-glue-libs", "cluster-glue-libs-devel", "cluster-snmp", "clusterlib", "clusterlib-devel", "cman", "corosync", "corosynclib", "corosynclib-devel", "fence-virt", "fence-virtd-checkpoint", "foghorn", "libesmtp-devel", "libqb", "libqb-devel", "libtool-ltdl-devel", "luci", "modcluster", "omping", "openais", "openaislib", "openaislib-devel", "pacemaker", "pacemaker-cli", "pacemaker-cluster-libs", "pacemaker-cts", "pacemaker-doc", "pacemaker-libs", "pacemaker-libs-devel", "pcs", "python-repoze-what-plugins-sql", "python-repoze-what-quickstart", "python-repoze-who-friendlyform", "python-repoze-who-plugins-sa", "python-tw-forms", "resource-agents", "rgmanager", "ricci"});
			}
		}
		if (redhatReleaseX>=7) {	// rhel7
			if (arch.equals("x86_64")) {
				haPackages = Arrays.asList(new String[]{"corosync", "corosynclib", "corosynclib-devel", "dlm", "dlm-devel", "dlm-lib", "ipvsadm", "ldirectord", "libqb", "libqb-devel", "libtool-ltdl-devel", "lvm2-cluster", "omping", "pacemaker", "pacemaker-cli", "pacemaker-cluster-libs", "pacemaker-cts", "pacemaker-doc", "pacemaker-libs", "pacemaker-libs-devel", "pcs", "resource-agents"});
			}
		}
		if (redhatReleaseX>=7 && redhatReleaseY>=1) {	// rhel7.1
			if (arch.equals("x86_64")) {
				haPackages = Arrays.asList(new String[]{"omping", "clufter-cli", "clufter-lib-ccs", "clufter-lib-general", "clufter-lib-pcs", "corosync", " corosynclib", "corosynclib-devel", "libqb", "libqb-devel", "pacemaker", "pacemaker-cli", "pacemaker-cluster-libs", " pacemaker-cts", "pacemaker-doc", "pacemaker-libs", "pacemaker-libs-devel", "pacemaker-nagios-plugins-metadata", "pacemaker-remote", "pcs", "python-clufter", "resource-agents", "sbd"});
			}
			if (arch.equals("s390x")) {
				haPackages = Arrays.asList(new String[]{"omping", "clufter-cli", "clufter-lib-ccs", "clufter-lib-general", "clufter-lib-pcs", "corosync", " corosynclib", "corosynclib-devel", "libqb", "libqb-devel", "pacemaker", "pacemaker-cli", "pacemaker-cluster-libs", " pacemaker-cts", "pacemaker-doc", "pacemaker-libs", "pacemaker-libs-devel", "pacemaker-nagios-plugins-metadata", "pacemaker-remote", "pcs", "python-clufter", "resource-agents"});
			}
		}
		
		return haPackages;
	}
	
	/**
	 * @param arch
	 * @return a SKU that provides access to High Availability content (assuming the subscription has been granted to the currently registered consumer)
	 */
	static String getHighAvailabilitySku(String arch) {
		String haSku=null;
		if (arch.equals("x86_64")||arch.equals("i386")) {
			haSku = "RH00025"; // High Availability
		}
		if (arch.equals("s390x")) {
			haSku = "RH00546"; // High Availability for IBM System z
		}
		return haSku;
	}
	
	
	
	// Data Providers ***********************************************************************

}
