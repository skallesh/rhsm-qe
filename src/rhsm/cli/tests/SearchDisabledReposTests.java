package rhsm.cli.tests;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.tools.SSHCommandResult;

import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.Repo;

import com.github.redhatqe.polarize.metadata.DefTypes;
import com.github.redhatqe.polarize.metadata.TestDefinition;
import com.github.redhatqe.polarize.metadata.TestType;
import com.github.redhatqe.polarize.metadata.DefTypes.PosNeg;
import com.github.redhatqe.polarize.metadata.DefTypes.Project;

/**
 * @author jsefler
 * 
 * References:
 * 		Bug 1232232 - [RFE] Provide API call to enable/disable yum repositories  (RHEL7.2)
 * 		Bug 1268376 - [RFE] Provide API call to enable/disable yum repositories  (RHEL6.8)
 * 		http://etherpad.corp.redhat.com/yum-search-disabled-repos-plugin-Testcases
 */
@Test(groups={"SearchDisabledReposTests"})
public class SearchDisabledReposTests extends SubscriptionManagerCLITestScript{

	
	// Test methods ***********************************************************************

	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20088", "RHEL7-51100"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify default configuration for /etc/yum/pluginconf.d/search-disabled-repos.conf; enabled=1 notify_only=1",
			groups={"Tier1Tests","blockedByBug-1232232","blockedByBug-1268376"},
			priority=10, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testDefaultConfiguration() {
		
		//	[root@jsefler-7 ~]# cat /etc/yum/pluginconf.d/search-disabled-repos.conf
		//	[main]
		//	enabled=1
		//
		//	# With notify_only=1 this plugin does not modify yum's behaviour.
		//	# Setting notify_only to 0 will enable yum to try to automatically resolve
		//	# dependency errors by temporarily enabling disabled repos and searching
		//	# for missing dependencies. If that helps resolve dependencies, yum will
		//	# suggest to permanently enable the repositories that have helped find
		//	# missing dependencies.
		//	# IMPORTANT: running yum with --assumeyes (or assumeyes config option)
		//	# will make yum automatically and without prompting the user temporarily
		//	# enable all repositories, and if it helps resolve dependencies yum will
		//	# permanently enable the repos that helped without prompting the user.
		//	notify_only=1
		//
		//	# Repositories matching the patterns listed in ignored_repos will not be enabled by the plugin
		//	ignored_repos=*debug-rpms *source-rpms *beta-rpms
		
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.yumPluginConfFileForSearchDisabledRepos, "main", "enabled"),"1","enabled value in "+clienttasks.yumPluginConfFileForSearchDisabledRepos);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.yumPluginConfFileForSearchDisabledRepos, "main", "notify_only"),"1","notify_only value in "+clienttasks.yumPluginConfFileForSearchDisabledRepos);
		Assert.assertEquals(clienttasks.getConfFileParameter(clienttasks.yumPluginConfFileForSearchDisabledRepos, "main", "ignored_repos"),"*debug-rpms *source-rpms *beta-rpms","ignored_repos value in "+clienttasks.yumPluginConfFileForSearchDisabledRepos);
	}



	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-20089", "RHEL7-55192"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="Verify that we can register with auto-subscribe to cover the base RHEL product cert; assert enablement of base rhel and optional repo",
			groups={"Tier1Tests"},
			priority=20, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testRhelSubscriptionBaseAndOptionalReposAreAvailable() throws JSONException, Exception {
		
		// get the currently installed RHEL product cert
		ProductCert rhelProductCert = clienttasks.getCurrentRhelProductCert();
		Assert.assertNotNull(rhelProductCert, "Expecting a RHEL Product Cert to be installed.");
		log.info("RHEL product cert installed: "+rhelProductCert);
		
		// register with auto-subscribe
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, null, null, null, null, null);
		
		// is rhelProductCert subscribed?
		InstalledProduct rhelInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", rhelProductCert.productId, clienttasks.getCurrentlyInstalledProducts());
		if (!rhelInstalledProduct.status.equals("Subscribed") && sm_serverType.equals(CandlepinType.standalone)) throw new SkipException("Skipping this test against a standalone Candlepin server that has no RHEL subscriptions available.");
		Assert.assertEquals(rhelInstalledProduct.status, "Subscribed","Autosubscribed status of installed RHEL productId '"+rhelProductCert.productId+"'");
		
		// get the yum repos
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// PLATFORM=RedHatEnterpriseLinux7-Server-aarch64
		//	Repo ID: rhel-7-for-arm-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 Server for ARM (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/arm/7/$releasever/$basearch/os
		//	Enabled: 1
		//	
		//	Repo ID: rhel-7-for-arm-optional-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 Server for ARM - Optional (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/arm/7/$releasever/$basearch/optional/os
		//	Enabled: 0
		//	
		//	Repo ID: rhel-7-server-for-arm-beta-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 Server for ARM Beta (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/beta/rhel/arm/7/$basearch/os
		//	Enabled: 1
		if (clienttasks.redhatReleaseX.equals("7") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("aarch64") &&
			rhelProductCert.productId.equals("294")/*Red Hat Enterprise Linux Server for ARM*/) {
			rhelBaseRepoId = "rhel-7-for-arm-rpms";
		}
		
		// PLATFORM=RedHatEnterpriseLinux7-Server-ppc64le
		//	Repo ID: rhel-7-for-power-le-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for IBM Power LE (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/power-le/7/$releasever/$basearch/os
		//	Enabled: 1
		//	
		//	Repo ID: rhel-7-for-power-le-optional-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for IBM Power LE - Optional (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/power-le/7/$releasever/$basearch/optional/os
		//	Enabled: 0
		if (clienttasks.redhatReleaseX.equals("7") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("ppc64le") &&
			rhelProductCert.productId.equals("279")/*Red Hat Enterprise Linux for Power, little endian*/) {
			rhelBaseRepoId = "rhel-7-for-power-le-rpms";
		}

		// PLATFORM=RedHatEnterpriseLinux7-Server-ppc64
		//	Repo ID: rhel-7-for-power-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for IBM Power (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/power/7/$releasever/$basearch/os
		//	Enabled: 1
		//	
		//	Repo ID: rhel-7-for-power-optional-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for IBM Power - Optional (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/power/7/$releasever/$basearch/optional/os
		//	Enabled: 0
		//	
		//	Repo ID: rhel-7-for-power-htb-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for IBM Power HTB (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/htb/rhel/power/7/$basearch/os
		//	Enabled: 1
		if (clienttasks.redhatReleaseX.equals("7") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("ppc64")) {
			rhelBaseRepoId = "rhel-7-for-power-rpms";
		}
		
		// PLATFORM=RedHatEnterpriseLinux7-Server-s390x
		//	Repo ID: rhel-7-for-system-z-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for System Z (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/system-z/7/$releasever/$basearch/os
		//	Enabled: 1
		//	
		//	Repo ID: rhel-7-for-system-z-optional-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for System Z - Optional (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/system-z/7/$releasever/$basearch/optional/os
		//	Enabled: 0
		//	
		//	Repo ID: rhel-7-for-system-z-htb-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for System Z HTB (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/htb/rhel/system-z/7/$basearch/os
		//	Enabled: 1
		if (clienttasks.redhatReleaseX.equals("7") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("s390x")) {
			rhelBaseRepoId = "rhel-7-for-system-z-rpms";
		}
		
		// PLATFORM=RedHatEnterpriseLinux7-Server-x86_64
		//	Repo ID: rhel-7-server-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 Server (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/server/7/$releasever/$basearch/os
		//	Enabled: 1
		//	
		//	Repo ID: rhel-7-server-optional-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 Server - Optional (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/server/7/$releasever/$basearch/optional/os
		//	Enabled: 0
		//	
		//	Repo ID: rhel-7-server-htb-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 Server HTB (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/htb/rhel/server/7/$basearch/os
		//	Enabled: 1
		//	
		//	Repo ID: rhel-7-server-eus-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 Server - Extended Update Support (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/eus/rhel/server/7/$releasever/$basearch/os
		//	Enabled: 1
		if (clienttasks.redhatReleaseX.equals("7") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("x86_64")) {
			rhelBaseRepoId = "rhel-7-server-rpms";
		}
		
		// PLATFORM=RedHatEnterpriseLinux7-Client-x86_64
		//	Repo ID: rhel-7-desktop-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 Desktop (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/client/7/$releasever/$basearch/os
		//	Enabled: 1
		//	
		//	Repo ID: rhel-7-desktop-optional-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 Desktop - Optional (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/client/7/$releasever/$basearch/optional/os
		//	Enabled: 0
		//	
		//	Repo ID: rhel-7-desktop-htb-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 Desktop HTB (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/htb/rhel/client/7/$basearch/os
		//	Enabled: 1
		if (clienttasks.redhatReleaseX.equals("7") && clienttasks.variant.equals("Client") && clienttasks.arch.equals("x86_64")) {
			rhelBaseRepoId = "rhel-7-desktop-rpms";
		}
		
		// PLATFORM=RedHatEnterpriseLinux7-ComputeNode-x86_64
		//	Repo ID: rhel-7-hpc-node-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for Scientific Computing (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/computenode/7/$releasever/$basearch/os
		//	Enabled: 1
		//	
		//	Repo ID: rhel-7-hpc-node-optional-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for Scientific Computing - Optional (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/computenode/7/$releasever/$basearch/optional/os
		//	Enabled: 0
		//	
		//	Repo ID: rhel-7-hpc-node-htb-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for Scientific Computing HTB (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/htb/rhel/computenode/7/$basearch/os
		//	Enabled: 1
		if (clienttasks.redhatReleaseX.equals("7") && clienttasks.variant.equals("ComputeNode") && clienttasks.arch.equals("x86_64")) {
			rhelBaseRepoId = "rhel-7-hpc-node-rpms";
		}
		
		// PLATFORM=RedHatEnterpriseLinux7-Workstation-x86_64
		//	Repo ID: rhel-7-workstation-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 Workstation (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/workstation/7/$releasever/$basearch/os
		//	Enabled: 1
		//	
		//	Repo ID: rhel-7-workstation-optional-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 Workstation - Optional (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/dist/rhel/workstation/7/$releasever/$basearch/optional/os
		//	Enabled: 0
		//	
		//	Repo ID: rhel-7-workstation-htb-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 Workstation HTB (RPMs)
		//	Repo URL: https://cdn.redhat.com/content/htb/rhel/workstation/7/$basearch/os
		//	Enabled: 1
		if (clienttasks.redhatReleaseX.equals("7") && clienttasks.variant.equals("Workstation") && clienttasks.arch.equals("x86_64")) {
			rhelBaseRepoId = "rhel-7-workstation-rpms";
		}
		
		// PLATFORM=RedHatEnterpriseLinuxAlternateArchitectures7-Server-ppc64le
		//	Repo ID:   rhel-7-for-power-9-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for POWER9 (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/dist/rhel-alt/server/7/$releasever/power9/$basearch/os
		//	Enabled:   1
		//
		//	Repo ID:   rhel-7-for-power-9-optional-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for POWER9 - Optional (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/dist/rhel-alt/server/7/$releasever/power9/$basearch/optional/os
		//	Enabled:   0
		//
		//	Repo ID:   rhel-7-for-power-9-extras-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for POWER9 - Extras (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/dist/rhel-alt/server/7/$releasever/power9/$basearch/extras/os
		//	Enabled:   0
		//
		//	Repo ID:   rhel-7-for-power-9-beta-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for POWER9 Beta (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/beta/rhel-alt/server/7/7Server/power9/$basearch/os
		//	Enabled:   0
		// NOTE: BETA PRODUCT 362 AND GA PRODUCT 420 BOTH PROVIDE THE SAME TAGS "rhel-alt-7,rhel-alt-7-power9" WHICH MEANS THEY CAN ACCESS THE SAME CONTENT AND ARE EFFECTIVELY THE SAME PRODUCT (ASSUMING ALL POWER9 SKUS PROVIDE BOTH 362 AND 420)
		if (clienttasks.redhatReleaseX.equals("7") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("ppc64le") &&
			(rhelProductCert.productId.equals("362")/*Red Hat Enterprise Linux for Power 9 Beta*/||rhelProductCert.productId.equals("420")/*Red Hat Enterprise Linux for Power 9*/)) {
			rhelBaseRepoId = "rhel-7-for-power-9-rpms";
		}
		
		// PLATFORM=RedHatEnterpriseLinuxAlternateArchitectures7-Server-s390x
		//	Repo ID:   rhel-7-for-system-z-a-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for IBM System z (Structure A) (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/dist/rhel-alt/server/7/$releasever/system-z-a/$basearch/os
		//	Enabled:   1
		//
		//	Repo ID:   rhel-7-for-system-z-a-optional-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for IBM System z (Structure A) - Optional (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/dist/rhel-alt/server/7/$releasever/system-z-a/$basearch/optional/os
		//	Enabled:   0
		//
		//	Repo ID:   rhel-7-for-system-z-a-extras-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for IBM System z (Structure A) - Extras (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/dist/rhel-alt/server/7/$releasever/system-z-a/$basearch/extras/os
		//	Enabled:   0
		//
		//	Repo ID:   rhel-7-for-system-z-a-beta-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for IBM System z (Structure A) Beta (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/beta/rhel-alt/server/7/7Server/system-z-a/$basearch/os
		//	Enabled:   0
		//
		//	Repo ID:   rhel-7-for-system-z-a-optional-beta-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for IBM System z (Structure A) - Optional Beta (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/beta/rhel-alt/server/7/7Server/system-z-a/$basearch/optional/os
		//	Enabled:   0
		//
		//	Repo ID:   rhel-7-for-system-z-a-extras-beta-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for IBM System z (Structure A) - Extras Beta (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/beta/rhel-alt/server/7/7Server/system-z-a/$basearch/extras/os
		//	Enabled:   0
		//
		// NOTE: BETA PRODUCT 433 AND GA PRODUCT 434 BOTH PROVIDE THE SAME TAGS "rhel-alt-7,rhel-alt-7-system-z-a" WHICH MEANS THEY CAN ACCESS THE SAME CONTENT AND ARE EFFECTIVELY THE SAME PRODUCT (ASSUMING ALL SYSTEMZ SKUS PROVIDE BOTH 433 AND 434)
		if (clienttasks.redhatReleaseX.equals("7") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("s390x") &&
			(rhelProductCert.productId.equals("433")/*Red Hat Enterprise Linux for IBM System z (Structure A) Beta*/||rhelProductCert.productId.equals("434")/*Red Hat Enterprise Linux for IBM System z (Structure A)*/)) {
			rhelBaseRepoId = "rhel-7-for-system-z-a-rpms";
		}
		
		// PLATFORM=RedHatEnterpriseLinuxAlternateArchitectures7-Server-aarch64
		//	Repo ID:   rhel-7-for-arm-64-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for ARM (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/dist/rhel-alt/server/7/$releasever/armv8-a/$basearch/os
		//	Enabled:   1
		//
		//	Repo ID:   rhel-7-for-arm-64-optional-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for ARM - Optional (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/dist/rhel-alt/server/7/$releasever/armv8-a/$basearch/optional/os
		//	Enabled:   0
		//
		//	Repo ID:   rhel-7-for-arm-64-extras-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for ARM - Extras (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/dist/rhel-alt/server/7/$releasever/armv8-a/$basearch/extras/os
		//	Enabled:   0
		//
		//	Repo ID:   rhel-7-for-arm-64-beta-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for ARM Beta (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/beta/rhel-alt/server/7/7Server/armv8-a/$basearch/os
		//	Enabled:   0
		//	
		//	Repo ID:   rhel-7-for-arm-64-optional-beta-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for ARM - Optional Beta (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/beta/rhel-alt/server/7/7Server/armv8-a/$basearch/optional/os
		//	Enabled:   0
		//
		//	Repo ID:   rhel-7-for-arm-64-extras-beta-rpms
		//	Repo Name: Red Hat Enterprise Linux 7 for ARM - Extras Beta (RPMs)
		//	Repo URL:  https://cdn.redhat.com/content/beta/rhel-alt/server/7/7Server/armv8-a/$basearch/extras/os
		//	Enabled:   0
		//
		// NOTE: BETA PRODUCT 363 AND GA PRODUCT 419 BOTH PROVIDE THE SAME TAGS "rhel-alt-7,rhel-alt-7-armv8-a" WHICH MEANS THEY CAN ACCESS THE SAME CONTENT AND ARE EFFECTIVELY THE SAME PRODUCT (ASSUMING ALL SYSTEMZ SKUS PROVIDE BOTH 363 AND 419)
		if (clienttasks.redhatReleaseX.equals("7") && clienttasks.variant.equals("Server") && clienttasks.arch.equals("aarch64") &&
			(rhelProductCert.productId.equals("363")/*Red Hat Enterprise Linux for ARM 64 Beta*/||rhelProductCert.productId.equals("419")/*Red Hat Enterprise Linux for ARM 64*/)) {
			rhelBaseRepoId = "rhel-7-for-arm-64-rpms";
		}
		
		
		// PLATFORM=RedHatEnterpriseLinux6-Server-i386
		// PLATFORM=RedHatEnterpriseLinux6-Server-x86_64
		//	[root@dell-pe1650-01 ~]# subscription-manager repos --list | egrep "rhel-6-server-(|optional-|eus-|beta-|htb-)rpms"
		//	Repo ID:   rhel-6-server-rpms
		//	Repo ID:   rhel-6-server-beta-rpms
		//	Repo ID:   rhel-6-server-eus-rpms
		//	Repo ID:   rhel-6-server-optional-rpms
		if (clienttasks.redhatReleaseX.equals("6") && clienttasks.variant.equals("Server") && (clienttasks.arch.matches("i\\d86|x86_64"))) {
			rhelBaseRepoId = "rhel-6-server-rpms";
		}
		
		// PLATFORM=RedHatEnterpriseLinux6-Server-ppc64
		//	[root@ibm-p8-04-lp1 ~]# subscription-manager repos --list | egrep "rhel-6-for-power-(|optional-|eus-|beta-|htb-)rpms"
		//	Repo ID:   rhel-6-for-power-rpms
		//	Repo ID:   rhel-6-for-power-optional-rpms
		//	Repo ID:   rhel-6-for-power-beta-rpms
		if (clienttasks.redhatReleaseX.equals("6") && clienttasks.variant.equals("Server") && (clienttasks.arch.equals("ppc64"))) {
			rhelBaseRepoId = "rhel-6-for-power-rpms";
		}
		
		// PLATFORM=RedHatEnterpriseLinux6-Server-s390x
		//	[root@ibm-z10-41 ~]# subscription-manager repos --list | egrep "rhel-6-for-system-z-(|optional-|eus-|beta-|htb-)rpms"
		//	Repo ID:   rhel-6-for-system-z-rpms
		//	Repo ID:   rhel-6-for-system-z-optional-rpms
		//	Repo ID:   rhel-6-for-system-z-beta-rpms
		if (clienttasks.redhatReleaseX.equals("6") && clienttasks.variant.equals("Server") && (clienttasks.arch.equals("s390x"))) {
			rhelBaseRepoId = "rhel-6-for-system-z-rpms";
		}
		
		// PLATFORM=RedHatEnterpriseLinux6-Client-i386
		// PLATFORM=RedHatEnterpriseLinux6-Client-x86_64
		//	[root@ibm-x3650m4-01-vm-01 ~]# subscription-manager repos --list | egrep "rhel-6-desktop-(|optional-|eus-|beta-|htb-)rpms"
		//	Repo ID:   rhel-6-desktop-rpms
		//	Repo ID:   rhel-6-desktop-beta-rpms
		//	Repo ID:   rhel-6-desktop-optional-rpms
		if (clienttasks.redhatReleaseX.equals("6") && clienttasks.variant.equals("Client") && (clienttasks.arch.matches("i\\d86|x86_64"))) {
			rhelBaseRepoId = "rhel-6-desktop-rpms";
		}

		// PLATFORM=RedHatEnterpriseLinux6-ComputeNode-x86_64
		//	[root@hp-dl585g5-01 ~]# subscription-manager repos --list | egrep " rhel-6-hpc-node-(|optional-|eus-|beta-|htb-)rpms"
		//	Repo ID:   rhel-6-hpc-node-rpms
		//	Repo ID:   rhel-6-hpc-node-beta-rpms
		//	Repo ID:   rhel-6-hpc-node-optional-rpms
		if (clienttasks.redhatReleaseX.equals("6") && clienttasks.variant.equals("ComputeNode") && (clienttasks.arch.equals("x86_64"))) {
			rhelBaseRepoId = "rhel-6-hpc-node-rpms";
		}
		
		// PLATFORM=RedHatEnterpriseLinux6-Workstation-i386
		// PLATFORM=RedHatEnterpriseLinux6-Workstation-x86_64 
		if (clienttasks.redhatReleaseX.equals("6") && clienttasks.variant.equals("Workstation") && (clienttasks.arch.matches("i\\d86|x86_64"))) {
			rhelBaseRepoId = "rhel-6-workstation-rpms";
		}
		
		
		// predict the disabled optional repo and potential presence of other enabled repos 
		if (rhelBaseRepoId!=null) {
			rhelOptionalRepoId			= rhelBaseRepoId.replaceFirst("-rpms$", "-optional-rpms");
			rhelEusRepoId				= rhelBaseRepoId.replaceFirst("-rpms$", "-eus-rpms");
			rhelBetaRepoId				= rhelBaseRepoId.replaceFirst("-rpms$", "-beta-rpms");
			rhelHtbRepoId				= rhelBaseRepoId.replaceFirst("-rpms$", "-htb-rpms");
			rhelOptionalHtbRepoId		= rhelBaseRepoId.replaceFirst("-rpms$", "-optional-htb-rpms");
		} else {
			Assert.fail("Additional automation development is needed in this test to predict the name of the enabled base RHEL repo for RHEL"+clienttasks.redhatReleaseX+" "+clienttasks.variant+" "+clienttasks.arch+"; Installed Product Cert: "+rhelProductCert);
		}
		
		// Special case: Snapshot composes provide the HTB product as the base product-default rather than the Beta/GA products.
		// Since some subscription SKUs like RH00076 only provide HTB products, let's adjust the
		// expected rhelBaseRepoId and rhelOptionalRepoId to the corresponding HTB repos
		if (rhelProductCert.productId.equals("230")/*Red Hat Enterprise Linux 7 Server High Touch Beta*/||
			rhelProductCert.productId.equals("231")/*Red Hat Enterprise Linux 7 Workstation High Touch Beta)*/) {
			log.info("Adjusting the expected base and optional repos because the default installed RHEL product appears to be High Touch Beta (this is expected for Snapshot composes).");
			rhelBaseRepoId = rhelHtbRepoId;
			rhelOptionalRepoId = rhelOptionalHtbRepoId;
		}
		// assert the base rhel repo is enabled by default
		Repo rhelBaseRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelBaseRepoId, subscribedRepos);
		Assert.assertNotNull(rhelBaseRepo, "RHEL base repo id '"+rhelBaseRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(rhelBaseRepo.enabled, "RHEL base repo id '"+rhelBaseRepoId+"' is enabled by default.");
		
		// assert the optional rhel repo is disabled by default
		Repo rhelOptionalRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelOptionalRepoId, subscribedRepos);
		Assert.assertNotNull(rhelOptionalRepo, "RHEL optional repo id '"+rhelOptionalRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(!rhelOptionalRepo.enabled, "RHEL optional repo id '"+rhelOptionalRepoId+"' is disabled by default.");
		
		// determine if beta rhel repo is entitled; if not then set it to null
		// assert the beta rhel repo is disabled by default
		Repo rhelBetaRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelBetaRepoId, subscribedRepos);
		if (rhelBetaRepo==null) rhelBetaRepoId=null;
		if (rhelBetaRepo!=null) Assert.assertTrue(!rhelBetaRepo.enabled, "RHEL beta repo id '"+rhelBetaRepoId+"' is disabled by default.");
		
		// determine if htb rhel repo is entitled; if not then set it to null
		Repo rhelHtbRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelHtbRepoId, subscribedRepos);
		if (rhelHtbRepo==null) rhelHtbRepoId=null;
		
		// determine if optional htb rhel repo is entitled; if not then set it to null
		Repo rhelOptionalHtbRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelOptionalHtbRepoId, subscribedRepos);
		if (rhelOptionalHtbRepo==null) rhelOptionalHtbRepoId=null;
		
		// determine if eus rhel repo is entitled; if not then set it to null
		Repo rhelEusRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelEusRepoId, subscribedRepos);
		if (rhelEusRepo==null) rhelEusRepoId=null;

	}
	protected String rhelBaseRepoId = null;
	protected String rhelOptionalRepoId = null;
	protected String rhelBetaRepoId = null;
	protected String rhelHtbRepoId = null;
	protected String rhelOptionalHtbRepoId = null;
	protected String rhelEusRepoId = null;


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-22226", "RHEL7-55194"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify that the search-disabled-repos plugin is not triggered when I attempt to install a package from a disabled repo (because search-disabled-repos plugin should only be triggered to resolve missing dependency packages)",
			groups={"Tier1Tests","blockedByBug-1512948"},
			dependsOnMethods={"testRhelSubscriptionBaseAndOptionalReposAreAvailable"},
			priority=25, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testYumSearchDisabledReposTriggersOnlyOnMissingDependencies() {
		// make sure rhelBasePackage and rhelOptionalPackage are not installed
		if (clienttasks.isPackageInstalled(rhelOptionalPackage)) clienttasks.yumRemovePackage(rhelOptionalPackage);
		if (clienttasks.isPackageInstalled(rhelBasePackage)) clienttasks.yumRemovePackage(rhelBasePackage);
		
		// enable rhelBaseRepoId and disable rhelOptionalRepoId
		clienttasks.repos(null, null, null, rhelBaseRepoId, rhelOptionalRepoId, null, null, null, null);
		
		// attempt to install a specific package from a disabled repo
		String disablerepos = "--disablerepo=beaker-*";
		if (!rhelBaseRepoId.endsWith("-beta-rpms") && !rhelOptionalRepoId.endsWith("-beta-rpms")) disablerepos += " --disablerepo=*-beta-rpms";
		if (!rhelBaseRepoId.endsWith("-htb-rpms") && !rhelOptionalRepoId.endsWith("-htb-rpms")) disablerepos += " --disablerepo=*-htb-rpms";
		if (!rhelBaseRepoId.endsWith("-eus-rpms") && !rhelOptionalRepoId.endsWith("-eus-rpms")) disablerepos += " --disablerepo=*-eus-rpms";
		SSHCommandResult result = clienttasks.yumDoPackageFromRepo_("install", rhelOptionalPackage, null, disablerepos);	// disable any other repos that might be enabled to prevent rhelBasePackage from // rhel-7-server-eus-rpms

		// assert results...  should not be able to find package since package is in a disabled repo
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(1),"Exit code from attempt to install '"+rhelOptionalPackage+"' from a disabled repo.");
		String stdoutMessage = "No package "+rhelOptionalPackage+" available.";
		Assert.assertTrue(result.getStdout().contains(stdoutMessage),"Stdout from attempt to install '"+rhelOptionalPackage+"' from a disabled repo contains message:\n"+stdoutMessage);
		String stderrMessage = "Error: Nothing to do";
		Assert.assertTrue(result.getStderr().contains(stderrMessage),"Stderr from attempt to install '"+rhelOptionalPackage+"' from a disabled repo contains message:\n"+stderrMessage);

		
		// enable rhelOptionalRepoId and disable rhelBaseRepoId
		clienttasks.repos(null, null, null, rhelOptionalRepoId, rhelBaseRepoId, null, null, null, null);
		
		// attempt to install a specific package from a disabled repo
		result = clienttasks.yumDoPackageFromRepo_("install", rhelBasePackage, null, disablerepos);	// disable any other repos that might be enabled // rhel-7-server-eus-rpms

		// assert results...  should not be able to find package since package is in a disabled repo
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(1),"Exit code from attempt to install '"+rhelBasePackage+"' from a disabled repo.");
		stdoutMessage = "No package "+rhelBasePackage+" available.";
		Assert.assertTrue(result.getStdout().contains(stdoutMessage),"Stdout from attempt to install '"+rhelBasePackage+"' from a disabled repo contains message:\n"+stdoutMessage);
		stderrMessage = "Error: Nothing to do";
		Assert.assertTrue(result.getStderr().contains(stderrMessage),"Stderr from attempt to install '"+rhelBasePackage+"' from a disabled repo contains message:\n"+stderrMessage);
		
	}
	protected String rhelBasePackage		= "ghostscript";		// assume this package is available from rhelBaseRepoId
	protected String rhelOptionalPackage	= "ghostscript-devel";	// assume this package is available from rhelOptionalRepoId and depends on rhelBasePackage


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-22227", "RHEL7-55193"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify yum usability message is presented when the default notify_only=1 is configured in /etc/yum/pluginconf.d/search-disabled-repos.conf",
			groups={"Tier1Tests","blockedByBug-1232232","blockedByBug-1268376","blockedByBug-1512948"},
			dependsOnMethods={"testRhelSubscriptionBaseAndOptionalReposAreAvailable"},
			priority=30, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testNotifyOnlyFeedbackFromYumSearchDisabledRepos() {
		// make sure rhelBasePackage and rhelOptionalPackage are not installed
		if (clienttasks.isPackageInstalled(rhelOptionalPackage)) clienttasks.yumRemovePackage(rhelOptionalPackage);
		if (clienttasks.isPackageInstalled(rhelBasePackage)) clienttasks.yumRemovePackage(rhelBasePackage);
		
		/* should not be needed
		// manually turn on notify_only 1
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSearchDisabledRepos, "notify_only", "1");
		*/
		
		// enable rhelOptionalRepoId and disable rhelBaseRepoId
		clienttasks.repos(null, null, null, rhelOptionalRepoId, rhelBaseRepoId, null, null, null, null);
		
		// attempt to install a package that requires another package from a disabled repo
		String disablerepos = "--disablerepo=beaker-*";
		if (!rhelBaseRepoId.endsWith("-beta-rpms") && !rhelOptionalRepoId.endsWith("-beta-rpms")) disablerepos += " --disablerepo=*-beta-rpms";
		if (!rhelBaseRepoId.endsWith("-htb-rpms") && !rhelOptionalRepoId.endsWith("-htb-rpms")) disablerepos += " --disablerepo=*-htb-rpms";
		if (!rhelBaseRepoId.endsWith("-eus-rpms") && !rhelOptionalRepoId.endsWith("-eus-rpms")) disablerepos += " --disablerepo=*-eus-rpms";
		SSHCommandResult result = clienttasks.yumDoPackageFromRepo_("install", rhelOptionalPackage, null, disablerepos);	// disable any other repos that might be enabled // rhel-7-server-eus-rpms

		//	2015-10-26 15:26:58.217  FINE: ssh root@jsefler-7.usersys.redhat.com yum -y install ghostscript-devel --disableplugin=rhnplugin --disablerepo=*eus-rpms
		//	2015-10-26 15:27:05.473  FINE: Stdout: 
		//	Loaded plugins: langpacks, product-id, search-disabled-repos, subscription-
		//	              : manager
		//	Resolving Dependencies
		//	--> Running transaction check
		//	---> Package ghostscript-devel.x86_64 0:9.07-18.el7 will be installed
		//	--> Processing Dependency: ghostscript = 9.07-18.el7 for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Processing Dependency: libgs.so.9()(64bit) for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Finished Dependency Resolution
		//	Error: Package: ghostscript-devel-9.07-18.el7.x86_64 (rhel-7-server-optional-rpms)
		//	           Requires: libgs.so.9()(64bit)
		//	Error: Package: ghostscript-devel-9.07-18.el7.x86_64 (rhel-7-server-optional-rpms)
		//	           Requires: ghostscript = 9.07-18.el7
		//	**********************************************************************
		//	yum can be configured to try to resolve such errors by temporarily enabling
		//	disabled repos and searching for missing dependencies.
		//	To enable this functionality please set 'notify_only=0' in /etc/yum/pluginconf.d/search-disabled-repos.conf
		//	**********************************************************************
		//
		//	 You could try using --skip-broken to work around the problem
		//	** Found 1 pre-existing rpmdb problem(s), 'yum check' output follows:
		//	redhat-access-insights-1.0.6-0.el7.noarch has missing requires of libcgroup-tools
		//
		//	2015-10-26 15:27:05.498  FINE: Stderr: 
		//	Error: Package: ghostscript-devel-9.07-18.el7.x86_64 (rhel-7-server-optional-rpms)
		//	           Requires: libgs.so.9()(64bit)
		//	Error: Package: ghostscript-devel-9.07-18.el7.x86_64 (rhel-7-server-optional-rpms)
		//	           Requires: ghostscript = 9.07-18.el7
		//
		//	2015-10-26 15:27:05.504  FINE: ExitCode: 1
		
		// assert results
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(1),"Exit code from attempt to install '"+rhelOptionalPackage+"'.");
		String requiresMessage = "Requires: "+rhelBasePackage;
		Assert.assertTrue(result.getStderr().contains(requiresMessage),"Stderr from attempt to install '"+rhelOptionalPackage+"' contains the require message:\n"+requiresMessage);
		String usabilityMessage = StringUtils.join(new String[]{
				"**********************************************************************",
				"yum can be configured to try to resolve such errors by temporarily enabling",
				"disabled repos and searching for missing dependencies.",
				"To enable this functionality please set 'notify_only=0' in "+clienttasks.yumPluginConfFileForSearchDisabledRepos,
				"**********************************************************************"},
				"\n");
		Assert.assertTrue(result.getStdout().contains(usabilityMessage),"Stdout from attempt to install '"+rhelOptionalPackage+"' contains the usability message:\n"+usabilityMessage);
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-22228", "RHEL7-55195"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify user is prompted to search disabled repos to complete an applicable yum install transaction when notify_only=0 is configured in /etc/yum/pluginconf.d/search-disabled-repos.conf and proceed with --assumeno responses",
			groups={"Tier1Tests","blockedByBug-1232232","blockedByBug-1268376","blockedByBug-1512948"},
			dependsOnMethods={"testRhelSubscriptionBaseAndOptionalReposAreAvailable"},
			priority=40, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testWithNotifyOnlyOffVerifyYumSearchDisabledReposAssumingNoResponses() {
		// make sure rhelBasePackage and rhelOptionalPackage are not installed
		if (clienttasks.isPackageInstalled(rhelOptionalPackage)) clienttasks.yumRemovePackage(rhelOptionalPackage);
		if (clienttasks.isPackageInstalled(rhelBasePackage)) clienttasks.yumRemovePackage(rhelBasePackage);
		
		// manually turn off notify_only 0
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSearchDisabledRepos, "notify_only", "0");
		
		// enable rhelOptionalRepoId and disable rhelBaseRepoId
		clienttasks.repos(null, null, null, rhelOptionalRepoId, rhelBaseRepoId, null, null, null, null);
		
		// attempt to install a package that requires another package from a disabled repo
		String disablerepos = "--disablerepo=beaker-*";
		if (!rhelBaseRepoId.endsWith("-beta-rpms") && !rhelOptionalRepoId.endsWith("-beta-rpms")) disablerepos += " --disablerepo=*-beta-rpms";
		if (!rhelBaseRepoId.endsWith("-htb-rpms") && !rhelOptionalRepoId.endsWith("-htb-rpms")) disablerepos += " --disablerepo=*-htb-rpms";
		if (!rhelBaseRepoId.endsWith("-eus-rpms") && !rhelOptionalRepoId.endsWith("-eus-rpms")) disablerepos += " --disablerepo=*-eus-rpms";
		SSHCommandResult result = clienttasks.yumDoPackageFromRepo_("install", rhelOptionalPackage, null, "--assumeno "+disablerepos);	// disable any other repos that might be enabled to prevent  // rhel-7-server-eus-rpms
		
		//	2015-10-26 15:54:03.983  FINE: ssh root@jsefler-7.usersys.redhat.com yum -y install ghostscript-devel --disableplugin=rhnplugin --assumeno --disablerepo=*eus-rpms
		//	2015-10-26 15:54:10.443  FINE: Stdout: 
		//	Loaded plugins: langpacks, product-id, search-disabled-repos, subscription-
		//	              : manager
		//	Resolving Dependencies
		//	--> Running transaction check
		//	---> Package ghostscript-devel.x86_64 0:9.07-18.el7 will be installed
		//	--> Processing Dependency: ghostscript = 9.07-18.el7 for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Processing Dependency: libgs.so.9()(64bit) for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Finished Dependency Resolution
		//	Error: Package: ghostscript-devel-9.07-18.el7.x86_64 (rhel-7-server-optional-rpms)
		//	           Requires: libgs.so.9()(64bit)
		//	Error: Package: ghostscript-devel-9.07-18.el7.x86_64 (rhel-7-server-optional-rpms)
		//	           Requires: ghostscript = 9.07-18.el7
		//	**********************************************************************
		//	Dependency resolving failed due to missing dependencies.
		//	Some repositories on your system are disabled, but yum can enable them
		//	and search for missing dependencies. This will require downloading
		//	metadata for disabled repositories and may take some time and traffic.
		//	**********************************************************************
		//
		//	 You could try using --skip-broken to work around the problem
		//	** Found 1 pre-existing rpmdb problem(s), 'yum check' output follows:
		//	redhat-access-insights-1.0.6-0.el7.noarch has missing requires of libcgroup-tools
		//
		//	2015-10-26 15:54:10.469  FINE: Stderr: 
		//	Error: Package: ghostscript-devel-9.07-18.el7.x86_64 (rhel-7-server-optional-rpms)
		//	           Requires: libgs.so.9()(64bit)
		//	Error: Package: ghostscript-devel-9.07-18.el7.x86_64 (rhel-7-server-optional-rpms)
		//	           Requires: ghostscript = 9.07-18.el7
		//
		//	2015-10-26 15:54:10.475  FINE: ExitCode: 1
		
		
		// assert results
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(1),"Exit code from attempt to install '"+rhelOptionalPackage+"'.");
		String requiresMessage = "Requires: "+rhelBasePackage;
		Assert.assertTrue(result.getStderr().contains(requiresMessage),"Stderr from attempt to install '"+rhelOptionalPackage+"' contains the require message:\n"+requiresMessage);
		String searchDisabledReposMessage = StringUtils.join(new String[]{
				"**********************************************************************",
				"Dependency resolving failed due to missing dependencies.",
				"Some repositories on your system are disabled, but yum can enable them",
				"and search for missing dependencies. This will require downloading",
				"metadata for disabled repositories and may take some time and traffic.",
				"**********************************************************************"},
				"\n");
		Assert.assertTrue(result.getStdout().contains(searchDisabledReposMessage),"Stdout from attempt to install '"+rhelOptionalPackage+"' contains the prompt message:\n"+searchDisabledReposMessage);
		
		// confirm that the packages are not installed
		Assert.assertTrue(!clienttasks.isPackageInstalled(rhelOptionalPackage),"Package '"+rhelOptionalPackage+"' is NOT installed.");
		Assert.assertTrue(!clienttasks.isPackageInstalled(rhelBasePackage),"Package '"+rhelBasePackage+"' is NOT installed.");
		
		// confirm the repo enablement has not changed
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// assert the base rhel repo is disabled
		Repo rhelBaseRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelBaseRepoId, subscribedRepos);
		Assert.assertNotNull(rhelBaseRepo, "RHEL base repo id '"+rhelBaseRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(!rhelBaseRepo.enabled, "RHEL base repo id '"+rhelBaseRepoId+"' is disabled.");
		
		// assert the optional rhel repo is enabled
		Repo rhelOptionalRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelOptionalRepoId, subscribedRepos);
		Assert.assertNotNull(rhelOptionalRepo, "RHEL optional repo id '"+rhelOptionalRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(rhelOptionalRepo.enabled, "RHEL optional repo id '"+rhelOptionalRepoId+"' is enabled.");

	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-22229", "RHEL7-55196"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify user is prompted to search disabled repos to complete an applicable yum install transaction when notify_only=0 is configured in /etc/yum/pluginconf.d/search-disabled-repos.conf and proceed with --assumeyes responses",
			groups={"Tier1Tests","blockedByBug-1232232","blockedByBug-1268376","blockedByBug-1512948","testWithNotifyOnlyOffVerifyYumSearchDisabledReposAssumingYesResponses"},
			dependsOnMethods={"testRhelSubscriptionBaseAndOptionalReposAreAvailable"},
			priority=50, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testWithNotifyOnlyOffVerifyYumSearchDisabledReposAssumingYesResponses() {
		
		// make sure rhelBasePackage and rhelOptionalPackage are not installed
		if (clienttasks.isPackageInstalled(rhelOptionalPackage)) clienttasks.yumRemovePackage(rhelOptionalPackage);
		if (clienttasks.isPackageInstalled(rhelBasePackage)) clienttasks.yumRemovePackage(rhelBasePackage);
		
		// manually turn off notify_only 0
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSearchDisabledRepos, "notify_only", "0");
		
		// enable rhelOptionalRepoId and disable rhelBaseRepoId,rhelEusRepoId
		List<String> enableRepos = new ArrayList<String>(); enableRepos.add(rhelOptionalRepoId);
		List<String> disableRepos = new ArrayList<String>(); disableRepos.add(rhelBaseRepoId);
		clienttasks.repos(null, null, null, enableRepos, disableRepos, null, null, null, null);
		disableRepos.clear();
		if (!rhelBaseRepoId.endsWith("-beta-rpms") && !rhelOptionalRepoId.endsWith("-beta-rpms")) disableRepos.add("*-beta-rpms");
		if (!rhelBaseRepoId.endsWith("-htb-rpms") && !rhelOptionalRepoId.endsWith("-htb-rpms")) disableRepos.add("*-htb-rpms");
		if (!rhelBaseRepoId.endsWith("-eus-rpms") && !rhelOptionalRepoId.endsWith("-eus-rpms")) disableRepos.add("*-eus-rpms");
		clienttasks.repos_(null, null, null, null, disableRepos, null, null, null, null);
		//	2015-10-29 17:51:58.988  FINE: ssh root@ibm-z10-30.rhts.eng.bos.redhat.com subscription-manager repos --disable=*-beta-rpms --disable=*-htb-rpms --disable=*-eus-rpms
		//	2015-10-29 17:52:08.882  FINE: Stdout: 
		//	Error: *-htb-rpms is not a valid repository ID. Use --list option to see valid repositories.
		//	Error: *-eus-rpms is not a valid repository ID. Use --list option to see valid repositories.
		//	Repository 'rhel-7-for-system-z-satellite-tools-6-beta-rpms' is disabled for this system.
		//	Repository 'rhel-7-for-system-z-rhn-tools-beta-rpms' is disabled for this system.
		//	Repository 'rhel-7-for-system-z-optional-beta-rpms' is disabled for this system.
		//	Repository 'rhel-7-for-system-z-rh-common-beta-rpms' is disabled for this system.
		//	Repository 'rhel-7-for-system-z-supplementary-beta-rpms' is disabled for this system.
		//	Repository 'rhel-7-for-system-z-beta-rpms' is disabled for this system.
		//
		//	2015-10-29 17:52:08.895  FINE: Stderr: 
		//	2015-10-29 17:52:08.896  FINE: ExitCode: 1
				
		// attempt to install a package that requires another package from a disabled repo
		SSHCommandResult result = clienttasks.yumDoPackageFromRepo_("install", rhelOptionalPackage, null, "--assumeyes --disablerepo=beaker-*");
		
		//	2015-10-26 16:53:19.222  FINE: ssh root@jsefler-7.usersys.redhat.com yum -y install ghostscript-devel --disableplugin=rhnplugin --assumeyes
		//	2015-10-26 16:53:55.547  FINE: Stdout: 
		//	Loaded plugins: langpacks, product-id, search-disabled-repos, subscription-
		//	              : manager
		//	Resolving Dependencies
		//	--> Running transaction check
		//	---> Package ghostscript-devel.x86_64 0:9.07-18.el7 will be installed
		//	--> Processing Dependency: ghostscript = 9.07-18.el7 for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Processing Dependency: libgs.so.9()(64bit) for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Finished Dependency Resolution
		//	Error: Package: ghostscript-devel-9.07-18.el7.x86_64 (rhel-7-server-optional-rpms)
		//	           Requires: libgs.so.9()(64bit)
		//	Error: Package: ghostscript-devel-9.07-18.el7.x86_64 (rhel-7-server-optional-rpms)
		//	           Requires: ghostscript = 9.07-18.el7
		//	**********************************************************************
		//	Dependency resolving failed due to missing dependencies.
		//	Some repositories on your system are disabled, but yum can enable them
		//	and search for missing dependencies. This will require downloading
		//	metadata for disabled repositories and may take some time and traffic.
		//	**********************************************************************
		//
		//	--> Running transaction check
		//	---> Package ghostscript-devel.x86_64 0:9.07-18.el7 will be installed
		//	--> Processing Dependency: ghostscript = 9.07-18.el7 for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Processing Dependency: libgs.so.9()(64bit) for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Running transaction check
		//	---> Package ghostscript.x86_64 0:9.07-18.el7 will be installed
		//	--> Finished Dependency Resolution
		//
		//	Dependencies Resolved
		//
		//	================================================================================
		//	 Package             Arch     Version       Repository                     Size
		//	================================================================================
		//	Installing:
		//	 ghostscript-devel   x86_64   9.07-18.el7   rhel-7-server-optional-rpms    49 k
		//	Installing for dependencies:
		//	 ghostscript         x86_64   9.07-18.el7   rhel-7-server-eus-rpms        4.3 M
		//
		//	Transaction Summary
		//	================================================================================
		//	Install  1 Package (+1 Dependent package)
		//
		//	Total download size: 4.4 M
		//	Installed size: 17 M
		//	Downloading packages:
		//	--------------------------------------------------------------------------------
		//	Total                                              3.2 MB/s | 4.4 MB  00:01     
		//	Running transaction check
		//	Running transaction test
		//	Transaction test succeeded
		//	Running transaction
		//	  Installing : ghostscript-9.07-18.el7.x86_64                               1/2 
		//	  Installing : ghostscript-devel-9.07-18.el7.x86_64                         2/2 
		//	  Verifying  : ghostscript-9.07-18.el7.x86_64                               1/2 
		//	  Verifying  : ghostscript-devel-9.07-18.el7.x86_64                         2/2 
		//	*******************************************************************
		//	Dependency resolving was successful thanks to enabling these repositories:
		//	rhel-7-server-eus-rpms
		//	*******************************************************************
		//
		//
		//	Installed:
		//	  ghostscript-devel.x86_64 0:9.07-18.el7                                        
		//
		//	Dependency Installed:
		//	  ghostscript.x86_64 0:9.07-18.el7                                              
		//
		//	Complete!
		//
		//	2015-10-26 16:53:55.548  FINE: Stderr: 
		//	2015-10-26 16:53:55.548  FINE: ExitCode: 0
		
		
		// assert results
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0),"Exit code from attempt to install '"+rhelOptionalPackage+"' that requires '"+rhelBasePackage+"'.");

		// confirm that the packages are now installed
		Assert.assertTrue(clienttasks.isPackageInstalled(rhelOptionalPackage),"Package '"+rhelOptionalPackage+"' is installed.");
		Assert.assertTrue(clienttasks.isPackageInstalled(rhelBasePackage),"Package '"+rhelBasePackage+"' is installed.");
		
		// assert more results
		String requiresMessage = "Requires: "+rhelBasePackage;
		Assert.assertTrue(result.getStdout().contains(requiresMessage),"Stdout from attempt to install '"+rhelOptionalPackage+"' contains the require message:\n"+requiresMessage);
		String searchDisabledReposMessage = StringUtils.join(new String[]{
				"**********************************************************************",
				"Dependency resolving failed due to missing dependencies.",
				"Some repositories on your system are disabled, but yum can enable them",
				"and search for missing dependencies. This will require downloading",
				"metadata for disabled repositories and may take some time and traffic.",
				"**********************************************************************"},
				"\n");
		Assert.assertTrue(result.getStdout().contains(searchDisabledReposMessage),"Stdout from attempt to install '"+rhelOptionalPackage+"' contains the prompt message:\n"+searchDisabledReposMessage);
		String rhelActualRepoId = clienttasks.getYumPackageInfo(rhelBasePackage,"From repo");
		String resolutionMessage = StringUtils.join(new String[]{
				"*******************************************************************",
				"Dependency resolving was successful thanks to enabling these repositories:",
				rhelActualRepoId,
				"*******************************************************************"},
				"\n");
		Assert.assertTrue(result.getStdout().contains(resolutionMessage),"Stdout from attempt to install '"+rhelOptionalPackage+"' which requires '"+rhelBasePackage+"' contains the resolution message:\n"+resolutionMessage);
		
		
		// confirm the repo enablement has not changed
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// assert the optional rhel repo remains enabled
		Repo rhelOptionalRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelOptionalRepoId, subscribedRepos);
		Assert.assertNotNull(rhelOptionalRepo, "RHEL optional repo id '"+rhelOptionalRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(rhelOptionalRepo.enabled, "RHEL optional repo id '"+rhelOptionalRepoId+"' remains enabled.");
		
		// assert the actual rhel repo enablement is persisted.
		Repo rhelActualRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelActualRepoId, subscribedRepos);
		Assert.assertNotNull(rhelActualRepo, "RHEL repo id '"+rhelActualRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(rhelActualRepo.enabled, "RHEL repo id '"+rhelActualRepoId+"' was enabled permanently by the search-disabled-repos plugin.");
		
		// assert that the persisted enabled repos appear in the repo-override list
		SSHCommandResult listResult = clienttasks.repo_override(true,null,(String)null,(String)null,null,null,null,null, null);
		String name= "enabled",value,regex;
		value = "1";
		regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,rhelActualRepoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+rhelActualRepoId+"' name='"+name+"' value='"+value+"'.");
		value = "1";
		regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,rhelOptionalRepoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+rhelOptionalRepoId+"' name='"+name+"' value='"+value+"'.");
		
	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-22230", "RHEL7-55197"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify user is prompted to search disabled repos to complete an applicable yum install transaction when notify_only=0 is configured in /etc/yum/pluginconf.d/search-disabled-repos.conf and proceed with yes response to search disabled repos and install followed by no response to keep repos enabled.",
			groups={"Tier1Tests","blockedByBug-1232232","blockedByBug-1268376","blockedByBug-1512948","testWithNotifyOnlyOffVerifyYumSearchDisabledReposWithYesYesNoResponses"},
			dependsOnMethods={"testRhelSubscriptionBaseAndOptionalReposAreAvailable"},
			priority=60, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testWithNotifyOnlyOffVerifyYumSearchDisabledReposWithYesYesNoResponses() {
		// make sure rhelBasePackage and rhelOptionalPackage are not installed
		if (clienttasks.isPackageInstalled(rhelOptionalPackage)) clienttasks.yumRemovePackage(rhelOptionalPackage);
		if (clienttasks.isPackageInstalled(rhelBasePackage)) clienttasks.yumRemovePackage(rhelBasePackage);
		
		// manually turn off notify_only 0
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSearchDisabledRepos, "notify_only", "0");
		
		// enable rhelOptionalRepoId and disable rhelBaseRepoId,rhelEusRepoId
		List<String> enableRepos = new ArrayList<String>(); enableRepos.add(rhelOptionalRepoId);
		List<String> disableRepos = new ArrayList<String>(); disableRepos.add(rhelBaseRepoId);
		clienttasks.repos(null, null, null, enableRepos, disableRepos, null, null, null, null);
		disableRepos.clear();
		if (!rhelBaseRepoId.endsWith("-beta-rpms") && !rhelOptionalRepoId.endsWith("-beta-rpms")) disableRepos.add("*-beta-rpms");
		if (!rhelBaseRepoId.endsWith("-htb-rpms") && !rhelOptionalRepoId.endsWith("-htb-rpms")) disableRepos.add("*-htb-rpms");
		if (!rhelBaseRepoId.endsWith("-eus-rpms") && !rhelOptionalRepoId.endsWith("-eus-rpms")) disableRepos.add("*-eus-rpms");
		clienttasks.repos_(null, null, null, null, disableRepos, null, null, null, null);
		
		// attempt to install a package that requires another package from a disabled repo
		// responding yes, yes, and then no
		SSHCommandResult result = client.runCommandAndWait("yum install "+rhelOptionalPackage+" --disableplugin=rhnplugin --disablerepo=beaker-* "+" << EOF\ny\ny\nN\nEOF");	// interactive yum responses are:  y y N
		
		//	2015-10-27 14:08:13.820  FINE: ssh root@jsefler-7.usersys.redhat.com yum install ghostscript-devel --disableplugin=rhnplugin  << EOF
		//	y
		//	y
		//	N
		//	EOF
		//	2015-10-27 14:08:48.730  FINE: Stdout: 
		//	Loaded plugins: langpacks, product-id, search-disabled-repos, subscription-
		//	              : manager
		//	Resolving Dependencies
		//	--> Running transaction check
		//	---> Package ghostscript-devel.x86_64 0:9.07-18.el7 will be installed
		//	--> Processing Dependency: ghostscript = 9.07-18.el7 for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Processing Dependency: libgs.so.9()(64bit) for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Finished Dependency Resolution
		//	Error: Package: ghostscript-devel-9.07-18.el7.x86_64 (rhel-7-server-optional-rpms)
		//	           Requires: libgs.so.9()(64bit)
		//	Error: Package: ghostscript-devel-9.07-18.el7.x86_64 (rhel-7-server-optional-rpms)
		//	           Requires: ghostscript = 9.07-18.el7
		//	**********************************************************************
		//	Dependency resolving failed due to missing dependencies.
		//	Some repositories on your system are disabled, but yum can enable them
		//	and search for missing dependencies. This will require downloading
		//	metadata for disabled repositories and may take some time and traffic.
		//	**********************************************************************
		//
		//	Enable all repositories and try again? [y/N]: 
		//	--> Running transaction check
		//	---> Package ghostscript-devel.x86_64 0:9.07-18.el7 will be installed
		//	--> Processing Dependency: ghostscript = 9.07-18.el7 for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Processing Dependency: libgs.so.9()(64bit) for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Running transaction check
		//	---> Package ghostscript.x86_64 0:9.07-18.el7 will be installed
		//	--> Processing Dependency: ghostscript-fonts for package: ghostscript-9.07-18.el7.x86_64
		//	--> Running transaction check
		//	---> Package ghostscript-fonts.noarch 0:5.50-32.el7 will be installed
		//	--> Finished Dependency Resolution
		//
		//	Dependencies Resolved
		//
		//	================================================================================
		//	 Package             Arch     Version       Repository                     Size
		//	================================================================================
		//	Installing:
		//	 ghostscript-devel   x86_64   9.07-18.el7   rhel-7-server-optional-rpms    49 k
		//	Installing for dependencies:
		//	 ghostscript         x86_64   9.07-18.el7   rhel-7-server-eus-rpms        4.3 M
		//	 ghostscript-fonts   noarch   5.50-32.el7   rhel-7-server-eus-rpms        324 k
		//
		//	Transaction Summary
		//	================================================================================
		//	Install  1 Package (+2 Dependent packages)
		//
		//	Total download size: 4.7 M
		//	Installed size: 17 M
		//	Is this ok [y/d/N]: 
		//	Downloading packages:
		//	--------------------------------------------------------------------------------
		//	Total                                              2.7 MB/s | 4.7 MB  00:01     
		//	Running transaction check
		//	Running transaction test
		//	Transaction test succeeded
		//	Running transaction
		//	  Installing : ghostscript-fonts-5.50-32.el7.noarch                         1/3 
		//	  Installing : ghostscript-9.07-18.el7.x86_64                               2/3 
		//	  Installing : ghostscript-devel-9.07-18.el7.x86_64                         3/3 
		//	  Verifying  : ghostscript-fonts-5.50-32.el7.noarch                         1/3 
		//	  Verifying  : ghostscript-9.07-18.el7.x86_64                               2/3 
		//	  Verifying  : ghostscript-devel-9.07-18.el7.x86_64                         3/3 
		//	*******************************************************************
		//	Dependency resolving was successful thanks to enabling these repositories:
		//	rhel-7-server-eus-rpms
		//	*******************************************************************
		//
		//	Would you like to permanently enable these repositories? [y/N]: 
		//	Installed:
		//	  ghostscript-devel.x86_64 0:9.07-18.el7                                        
		//
		//	Dependency Installed:
		//	  ghostscript.x86_64 0:9.07-18.el7    ghostscript-fonts.noarch 0:5.50-32.el7   
		//
		//	Complete!
		//
		//	2015-10-27 14:08:48.797  FINE: Stderr: 
		//	2015-10-27 14:08:48.799  FINE: ExitCode: 0
		
		
		// assert exitCode results
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0),"Exit code from attempt to successfully install '"+rhelOptionalPackage+"' that requires '"+rhelBasePackage+"'.");
		
		// assert stderr results
		String stderrFiltered = result.getStderr();
		// NOTE: The following stderr is possible when new products have not yet been released?
		//       Let's workaround this by filtering it from stderr before asserting an empty stderr

		// 201607291254:11.409 - FINE: Stderr: 
		//	https://cdn.redhat.com/content/dist/rhel/workstation/7/7Workstation/x86_64/insights-client/1/os/repodata/repomd.xml: [Errno 14] HTTPS Error 404 - Not Found
		//	Trying other mirror.
		//	To address this issue please refer to the below knowledge base article 
		//
		//	https://access.redhat.com/articles/1320623
		//
		//	If above article doesn't help to resolve this issue please open a ticket with Red Hat Support.
		//
		//	https://cdn.redhat.com/content/dist/rhel/workstation/7/7Workstation/x86_64/openstack-tools/9/os/repodata/repomd.xml: [Errno 14] HTTPS Error 404 - Not Found
		//	Trying other mirror.
		//	https://cdn.redhat.com/content/dist/rhel/workstation/7/7Workstation/x86_64/ceph-tools/2/os/repodata/repomd.xml: [Errno 14] HTTPS Error 404 - Not Found
		//	Trying other mirror.
		//	https://cdn.redhat.com/content/dist/rhel/workstation/7/7Workstation/x86_64/openstack-tools/10/os/repodata/repomd.xml: [Errno 14] HTTPS Error 404 - Not Found
		//	Trying other mirror.
				
		//	201607121645:50.724 - FINE: Stderr: 
		//	https://cdn.redhat.com/content/dist/rhel/arm/7/7Server/aarch64/sat-tools/6.2/os/repodata/repomd.xml: [Errno 14] HTTPS Error 404 - Not Found
		//	Trying other mirror.
		//	To address this issue please refer to the below knowledge base article 
		//
		//	https://access.redhat.com/articles/1320623
		//
		//	If above article doesn't help to resolve this issue please open a ticket with Red Hat Support.
		
		//	201611081711:35.777 - FINE: Stderr: 
		//	https://cdn.redhat.com/content/dist/rhel/server/6/6Server/x86_64/sat-tools/6.3/os/repodata/repomd.xml: [Errno 14] PYCURL ERROR 22 - "The requested URL returned error: 404 Not Found"
		//	Trying other mirror.
		//	To address this issue please refer to the below knowledge base article 
		//
		//	https://access.redhat.com/articles/1320623
		//
		//	If above article doesn't help to resolve this issue please open a ticket with Red Hat Support.
		//
		//	https://cdn.redhat.com/content/dist/rhel/server/6/6Server/x86_64/insights-client/1/os/repodata/repomd.xml: [Errno 14] PYCURL ERROR 22 - "The requested URL returned error: 404 Not Found"
		//	Trying other mirror.

		//String stderrNotFoundRegex = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "baseurl")+".* HTTPS Error 404 - Not Found\nTrying other mirror.";
		String stderrNotFoundRegex = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "baseurl")+".* (HTTPS Error 404 - Not Found|\"The requested URL returned error: 404 Not Found\")\nTrying other mirror.";
		String stderrNotFoundInfo = "To address this issue please refer to the below knowledge base article \n\nhttps://access.redhat.com/articles/1320623\n\nIf above article doesn't help to resolve this issue please open a ticket with Red Hat Support.";
		if (stderrFiltered.contains(stderrNotFoundInfo)) log.warning("Ignoring stderr for all \"404 - Not Found\" errors since their discovery is not the purpose of this test.");
		stderrFiltered = stderrFiltered.replace(stderrNotFoundInfo, "");
		stderrFiltered = stderrFiltered.replaceAll(stderrNotFoundRegex, "");
		stderrFiltered = stderrFiltered.trim();
		
		// TEMPORARY WORKAROUND
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1276747"; // Bug 1276747 - rhel-7-for-system-z-rpms/7Server/s390x/productid [Errno -1] Metadata file does not match checksum
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
		if (invokeWorkaroundWhileBugIsOpen && rhelBaseRepoId.equals("rhel-7-for-system-z-rpms")) {
			log.warning("Skipping stderr assertion while bugId '"+bugId+"' is open.");
		} else
		// END OF WORKAROUND
		Assert.assertEquals(stderrFiltered,"", "Ignoring all \"404 - Not Found\" errors, stderr from attempt to successfully install '"+rhelOptionalPackage+"' that requires '"+rhelBasePackage+"'.");
		
		// assert stdout results
		String prompt;
		String requiresMessage = "Requires: "+rhelBasePackage;
		Assert.assertTrue(result.getStdout().contains(requiresMessage),"Stdout from attempt to install '"+rhelOptionalPackage+"' contains the require message:\n"+requiresMessage);
		String searchDisabledReposMessage = StringUtils.join(new String[]{
				"**********************************************************************",
				"Dependency resolving failed due to missing dependencies.",
				"Some repositories on your system are disabled, but yum can enable them",
				"and search for missing dependencies. This will require downloading",
				"metadata for disabled repositories and may take some time and traffic.",
				"**********************************************************************"},
				"\n");
		Assert.assertTrue(result.getStdout().contains(searchDisabledReposMessage),"Stdout from attempt to install '"+rhelOptionalPackage+"' which requires '"+rhelBasePackage+"' contains the search disabled repos message:\n"+searchDisabledReposMessage);
		prompt = "Enable all repositories and try again? [y/N]: ";
		Assert.assertTrue(result.getStdout().contains(prompt),"Stdout from attempt to install '"+rhelOptionalPackage+"' contains the prompt: "+prompt);
		prompt = "Is this ok [y/d/N]: ";	// RHEL7
		if (clienttasks.redhatReleaseX.equals("6")) prompt = "Is this ok [y/N]: ";	// RHEL6
		Assert.assertTrue(result.getStdout().contains(prompt),"Stdout from attempt to install '"+rhelOptionalPackage+"' contains the prompt: "+prompt);
		String rhelActualRepoId = clienttasks.getYumPackageInfo(rhelBasePackage,"From repo");
		String resolutionMessage = StringUtils.join(new String[]{
				"*******************************************************************",
				"Dependency resolving was successful thanks to enabling these repositories:",
				rhelActualRepoId,
				"*******************************************************************"},
				"\n");
		Assert.assertTrue(result.getStdout().contains(resolutionMessage),"Stdout from attempt to install '"+rhelOptionalPackage+"' which requires '"+rhelBasePackage+"' contains the resolution message:\n"+resolutionMessage);
		prompt = "Would you like to permanently enable these repositories? [y/N]: ";
		Assert.assertTrue(result.getStdout().contains(prompt),"Stdout from attempt to install '"+rhelOptionalPackage+"' contains the prompt: "+prompt);
		

		// confirm that the packages are now installed
		Assert.assertTrue(clienttasks.isPackageInstalled(rhelOptionalPackage),"Package '"+rhelOptionalPackage+"' is installed.");
		Assert.assertTrue(clienttasks.isPackageInstalled(rhelBasePackage),"Package '"+rhelBasePackage+"' is installed.");
		
		// confirm the repo enablement has not changed
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// assert the optional rhel repo remains enabled
		Repo rhelOptionalRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelOptionalRepoId, subscribedRepos);
		Assert.assertNotNull(rhelOptionalRepo, "RHEL optional repo id '"+rhelOptionalRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(rhelOptionalRepo.enabled, "RHEL optional repo id '"+rhelOptionalRepoId+"' remains enabled.");
		
		// assert the actual rhel repo enablement is  NOT persisted (since we said N to the prompt).
		Repo rhelActualRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelActualRepoId, subscribedRepos);
		Assert.assertNotNull(rhelActualRepo, "RHEL repo id '"+rhelActualRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(!rhelActualRepo.enabled, "RHEL repo id '"+rhelActualRepoId+"' was NOT enabled permanently by the search-disabled-repos plugin.");
		
		// assert that disabled base repo remains disabled in the repo-override list
		SSHCommandResult listResult = clienttasks.repo_override(true,null,(String)null,(String)null,null,null,null,null, null);
		String name= "enabled",value,regex;
		value = "0";
		regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,rhelBaseRepoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+rhelBaseRepoId+"' name='"+name+"' value='"+value+"'.");
		if (rhelBetaRepoId!=null) {
		value = "0";
		regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,rhelBetaRepoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+rhelBetaRepoId+"' name='"+name+"' value='"+value+"'.");
		}
		if (rhelHtbRepoId!=null) {
		value = "0";
		regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,rhelHtbRepoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+rhelHtbRepoId+"' name='"+name+"' value='"+value+"'.");
		}
		if (rhelEusRepoId!=null) {
		value = "0";
		regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,rhelEusRepoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+rhelEusRepoId+"' name='"+name+"' value='"+value+"'.");
		}
		// assert that the enabled optional repo remains enabled in the repo-override list
		value = "1";
		regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,rhelOptionalRepoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+rhelOptionalRepoId+"' name='"+name+"' value='"+value+"'.");

	}


	@TestDefinition(//update=true,	// uncomment to make TestDefinition changes update Polarion testcases through the polarize testcase importer
			projectID=  {Project.RHEL6, Project.RedHatEnterpriseLinux7},
			testCaseID= {"RHEL6-22231", "RHEL7-55198"},
			level= DefTypes.Level.COMPONENT, component= "subscription-manager",
			testtype= @TestType(testtype= DefTypes.TestTypes.FUNCTIONAL, subtype1= DefTypes.Subtypes.RELIABILITY, subtype2= DefTypes.Subtypes.EMPTY),
			posneg= PosNeg.POSITIVE, importance= DefTypes.Importance.HIGH, automation= DefTypes.Automation.AUTOMATED,
			tags= "Tier1")
	@Test(	description="verify user is prompted to search disabled repos to complete an applicable yum install transaction when notify_only=0 is configured in /etc/yum/pluginconf.d/search-disabled-repos.conf and proceed with yes response to search disabled repos and no to the install prompt",
			groups={"Tier1Tests","blockedByBug-1232232","blockedByBug-1268376","blockedByBug-1512948"},
			dependsOnMethods={"testRhelSubscriptionBaseAndOptionalReposAreAvailable"},
			priority=70, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void testWithNotifyOnlyOffVerifyYumSearchDisabledReposWithYesNoResponses() {
		// make sure rhelBasePackage and rhelOptionalPackage are not installed
		if (clienttasks.isPackageInstalled(rhelOptionalPackage)) clienttasks.yumRemovePackage(rhelOptionalPackage);
		if (clienttasks.isPackageInstalled(rhelBasePackage)) clienttasks.yumRemovePackage(rhelBasePackage);
		
		// manually turn off notify_only 0
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSearchDisabledRepos, "notify_only", "0");
		
		// enable rhelOptionalRepoId and disable rhelBaseRepoId,rhelEusRepoId
		List<String> enableRepos = new ArrayList<String>(); enableRepos.add(rhelOptionalRepoId);
		List<String> disableRepos = new ArrayList<String>(); disableRepos.add(rhelBaseRepoId);
		clienttasks.repos(null, null, null, enableRepos, disableRepos, null, null, null, null);
		disableRepos.clear();
		if (!rhelBaseRepoId.endsWith("-beta-rpms") && !rhelOptionalRepoId.endsWith("-beta-rpms")) disableRepos.add("*-beta-rpms");
		if (!rhelBaseRepoId.endsWith("-htb-rpms") && !rhelOptionalRepoId.endsWith("-htb-rpms")) disableRepos.add("*-htb-rpms");
		if (!rhelBaseRepoId.endsWith("-eus-rpms") && !rhelOptionalRepoId.endsWith("-eus-rpms")) disableRepos.add("*-eus-rpms");
		clienttasks.repos_(null, null, null, null, disableRepos, null, null, null, null);
		
		// attempt to install a package that requires another package from a disabled repo
		// responding yes and then no
		SSHCommandResult result = client.runCommandAndWait("yum install "+rhelOptionalPackage+" --disableplugin=rhnplugin --disablerepo=beaker-* "+" << EOF\ny\nN\nEOF");	// interactive yum responses are:  y y N

		//	2015-10-27 18:34:34.097  FINE: ssh root@jsefler-7.usersys.redhat.com yum install ghostscript-devel --disableplugin=rhnplugin  << EOF
		//	y
		//	N
		//	EOF
		//	2015-10-27 18:34:53.829  FINE: Stdout: 
		//	Loaded plugins: langpacks, product-id, search-disabled-repos, subscription-
		//	              : manager
		//	Resolving Dependencies
		//	--> Running transaction check
		//	---> Package ghostscript-devel.x86_64 0:9.07-18.el7 will be installed
		//	--> Processing Dependency: ghostscript = 9.07-18.el7 for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Processing Dependency: libgs.so.9()(64bit) for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Finished Dependency Resolution
		//	Error: Package: ghostscript-devel-9.07-18.el7.x86_64 (rhel-7-server-optional-rpms)
		//	           Requires: libgs.so.9()(64bit)
		//	Error: Package: ghostscript-devel-9.07-18.el7.x86_64 (rhel-7-server-optional-rpms)
		//	           Requires: ghostscript = 9.07-18.el7
		//	**********************************************************************
		//	Dependency resolving failed due to missing dependencies.
		//	Some repositories on your system are disabled, but yum can enable them
		//	and search for missing dependencies. This will require downloading
		//	metadata for disabled repositories and may take some time and traffic.
		//	**********************************************************************
		//
		//	Enable all repositories and try again? [y/N]: 
		//	--> Running transaction check
		//	---> Package ghostscript-devel.x86_64 0:9.07-18.el7 will be installed
		//	--> Processing Dependency: ghostscript = 9.07-18.el7 for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Processing Dependency: libgs.so.9()(64bit) for package: ghostscript-devel-9.07-18.el7.x86_64
		//	--> Running transaction check
		//	---> Package ghostscript.x86_64 0:9.07-18.el7 will be installed
		//	--> Finished Dependency Resolution
		//
		//	Dependencies Resolved
		//
		//	================================================================================
		//	 Package             Arch     Version       Repository                     Size
		//	================================================================================
		//	Installing:
		//	 ghostscript-devel   x86_64   9.07-18.el7   rhel-7-server-optional-rpms    49 k
		//	Installing for dependencies:
		//	 ghostscript         x86_64   9.07-18.el7   rhel-7-server-eus-rpms        4.3 M
		//
		//	Transaction Summary
		//	================================================================================
		//	Install  1 Package (+1 Dependent package)
		//
		//	Total download size: 4.4 M
		//	Installed size: 17 M
		//	Is this ok [y/d/N]:
		//	Exiting on user command
		//	Your transaction was saved, rerun it with:
		//	 yum load-transaction /tmp/yum_save_tx.2015-10-27.18-34.Lbc8K5.yumtx
		//
		//	2015-10-27 18:34:53.873  FINE: Stderr: 
		//	2015-10-27 18:34:53.875  FINE: ExitCode: 1
		
		// assert exitCode results
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(1),"Exit code from attempt to successfully install '"+rhelOptionalPackage+"' that requires '"+rhelBasePackage+"'.");
		
		// assert stderr results
		String stderrFiltered = result.getStderr();
		// NOTE: The following stderr is possible when new products have not yet been released?
		//       Let's workaround this by filtering it from stderr before asserting an empty stderr

		// 201607291254:11.409 - FINE: Stderr: 
		//	https://cdn.redhat.com/content/dist/rhel/workstation/7/7Workstation/x86_64/insights-client/1/os/repodata/repomd.xml: [Errno 14] HTTPS Error 404 - Not Found
		//	Trying other mirror.
		//	To address this issue please refer to the below knowledge base article 
		//
		//	https://access.redhat.com/articles/1320623
		//
		//	If above article doesn't help to resolve this issue please open a ticket with Red Hat Support.
		//
		//	https://cdn.redhat.com/content/dist/rhel/workstation/7/7Workstation/x86_64/openstack-tools/9/os/repodata/repomd.xml: [Errno 14] HTTPS Error 404 - Not Found
		//	Trying other mirror.
		//	https://cdn.redhat.com/content/dist/rhel/workstation/7/7Workstation/x86_64/ceph-tools/2/os/repodata/repomd.xml: [Errno 14] HTTPS Error 404 - Not Found
		//	Trying other mirror.
		//	https://cdn.redhat.com/content/dist/rhel/workstation/7/7Workstation/x86_64/openstack-tools/10/os/repodata/repomd.xml: [Errno 14] HTTPS Error 404 - Not Found
		//	Trying other mirror.
				
		//	201607121645:50.724 - FINE: Stderr: 
		//	https://cdn.redhat.com/content/dist/rhel/arm/7/7Server/aarch64/sat-tools/6.2/os/repodata/repomd.xml: [Errno 14] HTTPS Error 404 - Not Found
		//	Trying other mirror.
		//	To address this issue please refer to the below knowledge base article 
		//
		//	https://access.redhat.com/articles/1320623
		//
		//	If above article doesn't help to resolve this issue please open a ticket with Red Hat Support.
		
		//	201611081658:33.447 - FINE: Stderr: 
		//	https://cdn.redhat.com/content/dist/rhel/server/6/6Server/x86_64/sat-tools/6.3/os/repodata/repomd.xml: [Errno 14] PYCURL ERROR 22 - "The requested URL returned error: 404 Not Found"
		//	Trying other mirror.
		//	To address this issue please refer to the below knowledge base article 
		//	
		//	https://access.redhat.com/articles/1320623
		//	
		//	If above article doesn't help to resolve this issue please open a ticket with Red Hat Support.
		//	
		//	https://cdn.redhat.com/content/dist/rhel/server/6/6Server/x86_64/insights-client/1/os/repodata/repomd.xml: [Errno 14] PYCURL ERROR 22 - "The requested URL returned error: 404 Not Found"
		//	Trying other mirror.
			
		//String stderrNotFoundRegex = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "baseurl")+".* HTTPS Error 404 - Not Found\nTrying other mirror.";
		String stderrNotFoundRegex = clienttasks.getConfFileParameter(clienttasks.rhsmConfFile, "rhsm", "baseurl")+".* (HTTPS Error 404 - Not Found|\"The requested URL returned error: 404 Not Found\")\nTrying other mirror.";
		String stderrNotFoundInfo = "To address this issue please refer to the below knowledge base article \n\nhttps://access.redhat.com/articles/1320623\n\nIf above article doesn't help to resolve this issue please open a ticket with Red Hat Support.";
		if (stderrFiltered.contains(stderrNotFoundInfo)) log.warning("Ignoring stderr for all \"404 - Not Found\" errors since their discovery is not the purpose of this test.");
		stderrFiltered = stderrFiltered.replace(stderrNotFoundInfo, "");
		stderrFiltered = stderrFiltered.replaceAll(stderrNotFoundRegex, "");
		stderrFiltered = stderrFiltered.trim();
		Assert.assertEquals(stderrFiltered,"", "Ignoring all \"404 - Not Found\" errors, stderr from attempt to successfully install '"+rhelOptionalPackage+"' that requires '"+rhelBasePackage+"'.");

		// assert stdout results
		String prompt;
		String requiresMessage = "Requires: "+rhelBasePackage;
		Assert.assertTrue(result.getStdout().contains(requiresMessage),"Stdout from attempt to install '"+rhelOptionalPackage+"' contains the require message:\n"+requiresMessage);
		String searchDisabledReposMessage = StringUtils.join(new String[]{
				"**********************************************************************",
				"Dependency resolving failed due to missing dependencies.",
				"Some repositories on your system are disabled, but yum can enable them",
				"and search for missing dependencies. This will require downloading",
				"metadata for disabled repositories and may take some time and traffic.",
				"**********************************************************************"},
				"\n");
		Assert.assertTrue(result.getStdout().contains(searchDisabledReposMessage),"Stdout from attempt to install '"+rhelOptionalPackage+"' which requires '"+rhelBasePackage+"' contains the search disabled repos message:\n"+searchDisabledReposMessage);
		prompt = "Enable all repositories and try again? [y/N]: ";
		Assert.assertTrue(result.getStdout().contains(prompt),"Stdout from attempt to install '"+rhelOptionalPackage+"' contains the prompt: "+prompt);
		prompt = "Is this ok [y/d/N]: ";	// RHEL7
		if (clienttasks.redhatReleaseX.equals("6")) prompt = "Is this ok [y/N]: ";	// RHEL6
		Assert.assertTrue(result.getStdout().contains(prompt),"Stdout from attempt to install '"+rhelOptionalPackage+"' contains the prompt: "+prompt);
		
		// confirm that the packages are NOT installed
		Assert.assertTrue(!clienttasks.isPackageInstalled(rhelOptionalPackage),"Package '"+rhelOptionalPackage+"' is NOT installed.");
		Assert.assertTrue(!clienttasks.isPackageInstalled(rhelBasePackage),"Package '"+rhelBasePackage+"' is NOT installed.");
		
		// confirm the repo enablement has not changed
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// assert the optional rhel repo remains enabled
		Repo rhelOptionalRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelOptionalRepoId, subscribedRepos);
		Assert.assertNotNull(rhelOptionalRepo, "RHEL optional repo id '"+rhelOptionalRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(rhelOptionalRepo.enabled, "RHEL optional repo id '"+rhelOptionalRepoId+"' remains enabled.");
		
		// assert the base rhel repo enablement remains disabled
		Repo rhelBaseRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelBaseRepoId, subscribedRepos);
		Assert.assertNotNull(rhelBaseRepo, "RHEL repo id '"+rhelBaseRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(!rhelBaseRepo.enabled, "RHEL repo id '"+rhelBaseRepoId+"' remains disabled.");
		
		// assert that disabled base repo remains disabled in the repo-override list
		SSHCommandResult listResult = clienttasks.repo_override(true,null,(String)null,(String)null,null,null,null,null, null);
		String name= "enabled",value,regex;
		value = "0";
		regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,rhelBaseRepoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+rhelBaseRepoId+"' name='"+name+"' value='"+value+"'.");
		if (rhelBetaRepoId!=null) {
		value = "0";
		regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,rhelBetaRepoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+rhelBetaRepoId+"' name='"+name+"' value='"+value+"'.");
		}
		if (rhelHtbRepoId!=null) {
		value = "0";
		regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,rhelHtbRepoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+rhelHtbRepoId+"' name='"+name+"' value='"+value+"'.");
		}
		if (rhelEusRepoId!=null) {
		value = "0";
		regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,rhelEusRepoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+rhelEusRepoId+"' name='"+name+"' value='"+value+"'.");
		}
		// assert that the enabled optional repo remains enabled in the repo-override list
		value = "1";
		regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,rhelOptionalRepoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+rhelOptionalRepoId+"' name='"+name+"' value='"+value+"'.");

	}
	
	
	
	
	// Configuration methods ***********************************************************************
	
/* TODO Commented out until we see what yum supports
 * depends on Bug 1197245 - [RFE] package.xyz from channel_xyz has depsolving problems, --> Missing Dependency [rhel6]
 * depends on Bug 1188960 - [RFE] package.xyz from channel_xyz has depsolving problems, --> Missing Dependency [rhel7]
	@BeforeClass(groups="setup")
	public void determineMinimumYumVersion() throws Exception {
		if (clienttasks!=null) {
			if (clienttasks.isPackageVersion("yum", "<", "3.4.3-126")) {
				throw new SkipException("Minimum required version of yum for search-disabled-repos is yum-3.4.3-126");
			}
		}
	}
*/
	
	@BeforeClass(groups="setup")
	public void determineMinimumSubscriptionManagerVersion() throws Exception {
		if (clienttasks!=null) {
			if (clienttasks.isPackageVersion("subscription-manager", "<", "1.15.9-7")) {
				throw new SkipException("Minimum required version of subscription-manager for search-disabled-repos is subscription-manager-1.15.9-7");
			}
		}
	}
	
	@AfterClass(groups="setup")
	public void restoreDefaultSearchDisabledReposConfValues() throws Exception {
		if (clienttasks!=null) {
			// manually turn on notify_only 1
			clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSearchDisabledRepos, "notify_only", "1");
		}
	}
	
	
}
