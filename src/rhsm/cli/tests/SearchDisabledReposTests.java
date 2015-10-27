package rhsm.cli.tests;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.tools.SSHCommandResult;

import rhsm.base.CandlepinType;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.cli.tasks.SubscriptionManagerTasks;
import rhsm.data.InstalledProduct;
import rhsm.data.ProductCert;
import rhsm.data.Repo;

/**
 * @author jsefler
 *
 * Bug 1232232 - [RFE] Provide API call to enable/disable yum repositories
 */
@Test(groups={"SearchDisabledReposTests","Tier1Tests","AcceptanceTests"})
public class SearchDisabledReposTests extends SubscriptionManagerCLITestScript{

	
	// Test methods ***********************************************************************

	@Test(	description="verify default configuration for /etc/yum/pluginconf.d/search-disabled-repos.conf; enabled=1 notify_only=1",
			groups={},
			priority=10, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyDefaultConfiguration_Test() {
		
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
	
	
	
	@Test(	description="Verify that we can register with auto-subscribe to cover the base RHEL product cert; assert enablement of base rhel and optional repo",
			groups={"debugTest"},
			priority=20, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyRhelSubscriptionBaseAndOptionalReposAreAvailable_Test() throws JSONException, Exception {
		
		// get the currently installed RHEL product cert
		ProductCert rhelProductCert = clienttasks.getCurrentRhelProductCert();
		Assert.assertNotNull(rhelProductCert, "Expecting a RHEL Product Cert to be installed.");
		log.info("RHEL product cert installed: "+rhelProductCert);
		
		// register with auto-subscribe
		clienttasks.register(sm_clientUsername, sm_clientPassword, sm_clientOrg, null, null, null, null, true, null, null, (String)null, null, null, null, true, null, null, null, null);
		
		// is rhelProductCert subscribed?
		InstalledProduct rhelInstalledProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", rhelProductCert.productId, clienttasks.getCurrentlyInstalledProducts());
		if (!rhelInstalledProduct.status.equals("Subscribed") && sm_serverType.equals(CandlepinType.standalone)) throw new SkipException("Skipping this test against a standalone Candlepin server that has no RHEL subscriptions available.");
		Assert.assertEquals(rhelInstalledProduct.status, "Subscribed","Autosubscribed status of installed RHEL productId '"+rhelProductCert.productId+"'");
		
		// get the yum repos
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// determine the base rhel repo
		rhelBaseRepoId = String.format("rhel-%s-%s-rpms", clienttasks.redhatReleaseX, clienttasks.variant.toLowerCase());	// rhel-7-server-rpms
		
		// determine the optional rhel repo
		rhelOptlRepoId = rhelBaseRepoId.replaceFirst("-rpms$", "-optional-rpms");	// rhel-7-server-optional-rpms
		
		// determine the eus rhel repo
		rhelEusRepoId = rhelBaseRepoId.replaceFirst("-rpms$", "-eus-rpms");	// rhel-7-server-eus-rpms
		
		// assert the base rhel repo is enabled by default
		Repo rhelBaseRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelBaseRepoId, subscribedRepos);
		Assert.assertNotNull(rhelBaseRepo, "RHEL base repo id '"+rhelBaseRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(rhelBaseRepo.enabled, "RHEL base repo id '"+rhelBaseRepoId+"' is enabled by default.");
		
		// assert the optional rhel repo is disabled	by default
		Repo rhelOptlRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelOptlRepoId, subscribedRepos);
		Assert.assertNotNull(rhelOptlRepo, "RHEL optional repo id '"+rhelOptlRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(!rhelOptlRepo.enabled, "RHEL optional repo id '"+rhelOptlRepoId+"' is disabled by default.");

	}
	protected String rhelBaseRepoId = null;
	protected String rhelOptlRepoId = null;
	protected String rhelEusRepoId = null;
	
	
	
	@Test(	description="verify yum usability message is presented when the default notify_only=1 is configured in /etc/yum/pluginconf.d/search-disabled-repos.conf",
			groups={},
			dependsOnMethods={"VerifyRhelSubscriptionBaseAndOptionalReposAreAvailable_Test"},
			priority=30, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void VerifyNotifyOnlyFeedbackFromYumSearchDisabledRepos_Test() {
		// make sure rhelBasePackage and rhelOptlPackage are not installed
		if (clienttasks.isPackageInstalled(rhelOptlPackage)) clienttasks.yumRemovePackage(rhelOptlPackage);
		if (clienttasks.isPackageInstalled(rhelBasePackage)) clienttasks.yumRemovePackage(rhelBasePackage);
		
		/* should not be needed
		// manually turn on notify_only 1
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSearchDisabledRepos, "notify_only", "1");
		*/
		
		// enable rhelOptlRepoId and disable rhelBaseRepoId
		clienttasks.repos(null, null, null, rhelOptlRepoId, rhelBaseRepoId, null, null, null);
		
		// attempt to install a package that requires another package from a disabled repo
		SSHCommandResult result = clienttasks.yumDoPackageFromRepo_("install", rhelOptlPackage, null, "--disablerepo=*eus-rpms");	// disable any entitled extended update repos // rhel-7-server-eus-rpms

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
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(1),"Exit code from attempt to install '"+rhelOptlPackage+"'.");
		String requiresMessage = "Requires: "+rhelBasePackage;
		Assert.assertTrue(result.getStderr().contains(requiresMessage),"Stderr from attempt to install '"+rhelOptlPackage+"' contains the require message:\n"+requiresMessage);
		String usabilityMessage = String.join("\n",
				"**********************************************************************",
				"yum can be configured to try to resolve such errors by temporarily enabling",
				"disabled repos and searching for missing dependencies.",
				"To enable this functionality please set 'notify_only=0' in "+clienttasks.yumPluginConfFileForSearchDisabledRepos,
				"**********************************************************************"
				);
		Assert.assertTrue(result.getStdout().contains(usabilityMessage),"Stdout from attempt to install '"+rhelOptlPackage+"' contains the usability message:\n"+usabilityMessage);
	}
	protected String rhelBasePackage = "ghostscript";		// assume this package is available from rhelBaseRepoId
	protected String rhelOptlPackage = "ghostscript-devel";	// assume this package is available from rhelOptlRepoId and depends on rhelBasePackage
	
	
	
	@Test(	description="verify user is prompted to search disabled repos to complete an applicable yum install transaction when notify_only=0 is configured in /etc/yum/pluginconf.d/search-disabled-repos.conf and proceed with --assumeno responses",
			groups={},
			dependsOnMethods={"VerifyRhelSubscriptionBaseAndOptionalReposAreAvailable_Test"},
			priority=40, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void WithNotifyOnlyOffVerifyYumSearchDisabledReposAssumingNoResponses_Test() {
		// make sure rhelBasePackage and rhelOptlPackage are not installed
		if (clienttasks.isPackageInstalled(rhelOptlPackage)) clienttasks.yumRemovePackage(rhelOptlPackage);
		if (clienttasks.isPackageInstalled(rhelBasePackage)) clienttasks.yumRemovePackage(rhelBasePackage);
		
		// manually turn off notify_only 0
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSearchDisabledRepos, "notify_only", "0");
		
		// enable rhelOptlRepoId and disable rhelBaseRepoId
		clienttasks.repos(null, null, null, rhelOptlRepoId, rhelBaseRepoId, null, null, null);
		
		// attempt to install a package that requires another package from a disabled repo
		SSHCommandResult result = clienttasks.yumDoPackageFromRepo_("install", rhelOptlPackage, null, "--assumeno --disablerepo=*eus-rpms");	// disable any entitled extended update repos // rhel-7-server-eus-rpms
		
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
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(1),"Exit code from attempt to install '"+rhelOptlPackage+"'.");
		String requiresMessage = "Requires: "+rhelBasePackage;
		Assert.assertTrue(result.getStderr().contains(requiresMessage),"Stderr from attempt to install '"+rhelOptlPackage+"' contains the require message:\n"+requiresMessage);
		String promptMessage = String.join("\n",
				"**********************************************************************",
				"Dependency resolving failed due to missing dependencies.",
				"Some repositories on your system are disabled, but yum can enable them",
				"and search for missing dependencies. This will require downloading",
				"metadata for disabled repositories and may take some time and traffic.",
				"**********************************************************************"
				);
		Assert.assertTrue(result.getStdout().contains(promptMessage),"Stdout from attempt to install '"+rhelOptlPackage+"' contains the prompt message:\n"+promptMessage);
		
		// confirm that the packages are not installed
		Assert.assertTrue(!clienttasks.isPackageInstalled(rhelOptlPackage),"Package '"+rhelOptlPackage+"' is NOT installed.");
		Assert.assertTrue(!clienttasks.isPackageInstalled(rhelBasePackage),"Package '"+rhelBasePackage+"' is NOT installed.");
		
		// confirm the repo enablement has not changed
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// assert the base rhel repo is disabled
		Repo rhelBaseRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelBaseRepoId, subscribedRepos);
		Assert.assertNotNull(rhelBaseRepo, "RHEL base repo id '"+rhelBaseRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(!rhelBaseRepo.enabled, "RHEL base repo id '"+rhelBaseRepoId+"' is disabled.");
		
		// assert the optional rhel repo is enabled
		Repo rhelOptlRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelOptlRepoId, subscribedRepos);
		Assert.assertNotNull(rhelOptlRepo, "RHEL optional repo id '"+rhelOptlRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(rhelOptlRepo.enabled, "RHEL optional repo id '"+rhelOptlRepoId+"' is enabled.");

	}
	
	
	
	@Test(	description="verify user is prompted to search disabled repos to complete an applicable yum install transaction when notify_only=0 is configured in /etc/yum/pluginconf.d/search-disabled-repos.conf and proceed with --assumeyes responses",
			groups={},
			dependsOnMethods={"VerifyRhelSubscriptionBaseAndOptionalReposAreAvailable_Test"},
			priority=50, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void WithNotifyOnlyOffVerifyYumSearchDisabledReposAssumingYesResponses_Test() {
		
		// make sure rhelBasePackage and rhelOptlPackage are not installed
		if (clienttasks.isPackageInstalled(rhelOptlPackage)) clienttasks.yumRemovePackage(rhelOptlPackage);
		if (clienttasks.isPackageInstalled(rhelBasePackage)) clienttasks.yumRemovePackage(rhelBasePackage);
		
		// manually turn off notify_only 0
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSearchDisabledRepos, "notify_only", "0");
		
		// enable rhelOptlRepoId and disable rhelBaseRepoId,rhelEusRepoId
		clienttasks.repos(null, null, null, Arrays.asList(rhelOptlRepoId), Arrays.asList(rhelBaseRepoId,rhelEusRepoId), null, null, null);
		
		// attempt to install a package that requires another package from a disabled repo
		SSHCommandResult result = clienttasks.yumDoPackageFromRepo_("install", rhelOptlPackage, null, "--assumeyes");
		
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
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0),"Exit code from attempt to install '"+rhelOptlPackage+"' that requires '"+rhelBasePackage+"'.");

		// confirm that the packages are now installed
		Assert.assertTrue(clienttasks.isPackageInstalled(rhelOptlPackage),"Package '"+rhelOptlPackage+"' is installed.");
		Assert.assertTrue(clienttasks.isPackageInstalled(rhelBasePackage),"Package '"+rhelBasePackage+"' is installed.");
		
		// assert more results
		String requiresMessage = "Requires: "+rhelBasePackage;
		Assert.assertTrue(result.getStdout().contains(requiresMessage),"Stdout from attempt to install '"+rhelOptlPackage+"' contains the require message:\n"+requiresMessage);
		String promptMessage = String.join("\n",
				"**********************************************************************",
				"Dependency resolving failed due to missing dependencies.",
				"Some repositories on your system are disabled, but yum can enable them",
				"and search for missing dependencies. This will require downloading",
				"metadata for disabled repositories and may take some time and traffic.",
				"**********************************************************************"
				);
		Assert.assertTrue(result.getStdout().contains(promptMessage),"Stdout from attempt to install '"+rhelOptlPackage+"' contains the prompt message:\n"+promptMessage);
		String rhelActualRepoId = clienttasks.getYumPackageInfo(rhelBasePackage,"From repo");
		String resolutionMessage = String.join("\n",
				"*******************************************************************",
				"Dependency resolving was successful thanks to enabling these repositories:",
				rhelActualRepoId,
				"*******************************************************************"
				);
		Assert.assertTrue(result.getStdout().contains(resolutionMessage),"Stdout from attempt to install '"+rhelOptlPackage+"' which requires '"+rhelBasePackage+"' contains the resolution message:\n"+resolutionMessage);
		
		
		// confirm the repo enablement has not changed
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// assert the optional rhel repo remains enabled
		Repo rhelOptlRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelOptlRepoId, subscribedRepos);
		Assert.assertNotNull(rhelOptlRepo, "RHEL optional repo id '"+rhelOptlRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(rhelOptlRepo.enabled, "RHEL optional repo id '"+rhelOptlRepoId+"' remains enabled.");
		
		// assert the actual rhel repo enablement is persisted.
		Repo rhelActualRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelActualRepoId, subscribedRepos);
		Assert.assertNotNull(rhelActualRepo, "RHEL repo id '"+rhelActualRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(rhelActualRepo.enabled, "RHEL repo id '"+rhelActualRepoId+"' was enabled permanently by the search-disabled-repos plugin.");
		
		
		// assert that the persisted enabled repos appear in the repo-override list
		Map<String,String> repoOverrideNameValueMap = new HashMap<String,String>();
		repoOverrideNameValueMap.put("enabled","1");
		SSHCommandResult listResult = clienttasks.repo_override(true,null,(String)null,(String)null,null,null,null,null);
		for (String repoId : Arrays.asList(rhelActualRepoId,rhelOptlRepoId)) {
			for (String name : repoOverrideNameValueMap.keySet()) {
				String value = repoOverrideNameValueMap.get(name);
				String regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,repoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
				Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+repoId+"' name='"+name+"' value='"+value+"'.");
			}
		}
	}
	
	
	@Test(	description="verify user is prompted to search disabled repos to complete an applicable yum install transaction when notify_only=0 is configured in /etc/yum/pluginconf.d/search-disabled-repos.conf and proceed with yes response to search disabled repos and install followed by no response to keep repos enabled.",
			groups={},
			dependsOnMethods={"VerifyRhelSubscriptionBaseAndOptionalReposAreAvailable_Test"},
			priority=60, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void WithNotifyOnlyOffVerifyYumSearchDisabledReposWithYesYesNoResponses_Test() {
		// make sure rhelBasePackage and rhelOptlPackage are not installed
		if (clienttasks.isPackageInstalled(rhelOptlPackage)) clienttasks.yumRemovePackage(rhelOptlPackage);
		if (clienttasks.isPackageInstalled(rhelBasePackage)) clienttasks.yumRemovePackage(rhelBasePackage);
		
		// manually turn off notify_only 0
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSearchDisabledRepos, "notify_only", "0");
		
		// enable rhelOptlRepoId and disable rhelBaseRepoId,rhelEusRepoId
		clienttasks.repos(null, null, null, Arrays.asList(rhelOptlRepoId), Arrays.asList(rhelBaseRepoId,rhelEusRepoId), null, null, null);
		
		// attempt to install a package that requires another package from a disabled repo
		// responding yes, yes, and then no
		SSHCommandResult result = client.runCommandAndWait("yum install "+rhelOptlPackage+" --disableplugin=rhnplugin "+" << EOF\ny\ny\nN\nEOF");	// interactive yum responses are:  y y N
		
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
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0),"Exit code from attempt to successfully install '"+rhelOptlPackage+"' that requires '"+rhelBasePackage+"'.");
		
		// assert stderr results
		Assert.assertEquals(result.getStderr().trim(),"", "Stderr from attempt to successfully install '"+rhelOptlPackage+"' that requires '"+rhelBasePackage+"'.");

		// assert stdout results
		String prompt;
		String requiresMessage = "Requires: "+rhelBasePackage;
		Assert.assertTrue(result.getStdout().contains(requiresMessage),"Stdout from attempt to install '"+rhelOptlPackage+"' contains the require message:\n"+requiresMessage);
		String searchDisabledReposMessage = String.join("\n",
				"**********************************************************************",
				"Dependency resolving failed due to missing dependencies.",
				"Some repositories on your system are disabled, but yum can enable them",
				"and search for missing dependencies. This will require downloading",
				"metadata for disabled repositories and may take some time and traffic.",
				"**********************************************************************"
				);
		Assert.assertTrue(result.getStdout().contains(searchDisabledReposMessage),"Stdout from attempt to install '"+rhelOptlPackage+"' which requires '"+rhelBasePackage+"' contains the search disabled repos message:\n"+searchDisabledReposMessage);
		prompt = "Enable all repositories and try again? [y/N]: ";
		Assert.assertTrue(result.getStdout().contains(prompt),"Stdout from attempt to install '"+rhelOptlPackage+"' contains the prompt: "+prompt);
		prompt = "Is this ok [y/d/N]: ";
		Assert.assertTrue(result.getStdout().contains(prompt),"Stdout from attempt to install '"+rhelOptlPackage+"' contains the prompt: "+prompt);
		String rhelActualRepoId = clienttasks.getYumPackageInfo(rhelBasePackage,"From repo");
		String resolutionMessage = String.join("\n",
				"*******************************************************************",
				"Dependency resolving was successful thanks to enabling these repositories:",
				rhelActualRepoId,
				"*******************************************************************"
				);
		Assert.assertTrue(result.getStdout().contains(resolutionMessage),"Stdout from attempt to install '"+rhelOptlPackage+"' which requires '"+rhelBasePackage+"' contains the resolution message:\n"+resolutionMessage);
		prompt = "Would you like to permanently enable these repositories? [y/N]: ";
		Assert.assertTrue(result.getStdout().contains(prompt),"Stdout from attempt to install '"+rhelOptlPackage+"' contains the prompt: "+prompt);
		

		// confirm that the packages are now installed
		Assert.assertTrue(clienttasks.isPackageInstalled(rhelOptlPackage),"Package '"+rhelOptlPackage+"' is installed.");
		Assert.assertTrue(clienttasks.isPackageInstalled(rhelBasePackage),"Package '"+rhelBasePackage+"' is installed.");
		
		// confirm the repo enablement has not changed
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// assert the optional rhel repo remains enabled
		Repo rhelOptlRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelOptlRepoId, subscribedRepos);
		Assert.assertNotNull(rhelOptlRepo, "RHEL optional repo id '"+rhelOptlRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(rhelOptlRepo.enabled, "RHEL optional repo id '"+rhelOptlRepoId+"' remains enabled.");
		
		// assert the actual rhel repo enablement is  NOT persisted (since we said N to the prompt).
		Repo rhelActualRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelActualRepoId, subscribedRepos);
		Assert.assertNotNull(rhelActualRepo, "RHEL repo id '"+rhelActualRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(!rhelActualRepo.enabled, "RHEL repo id '"+rhelActualRepoId+"' was NOT enabled permanently by the search-disabled-repos plugin.");
		
		// assert that the temporarily enabled repo remains disabled in the repo-override list
		SSHCommandResult listResult = clienttasks.repo_override(true,null,(String)null,(String)null,null,null,null,null);
		for (String repoId : Arrays.asList(rhelBaseRepoId,rhelEusRepoId,rhelActualRepoId)) {
			String name = "enabled";
			String value = "0";
			String regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,repoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
			Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+repoId+"' name='"+name+"' value='"+value+"'.");
		}
		// assert that the enabled optional repo remains enabled in the repo-override list
		String name = "enabled";
		String value = "1";
		String regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,rhelOptlRepoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+rhelOptlRepoId+"' name='"+name+"' value='"+value+"'.");

	}
	
	@Test(	description="verify user is prompted to search disabled repos to complete an applicable yum install transaction when notify_only=0 is configured in /etc/yum/pluginconf.d/search-disabled-repos.conf and proceed with yes response to search disabled repos and no to the install prompt",
			groups={"debugTest"},
			dependsOnMethods={"VerifyRhelSubscriptionBaseAndOptionalReposAreAvailable_Test"},
			priority=70, enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void WithNotifyOnlyOffVerifyYumSearchDisabledReposWithYesNoResponses_Test() {
		// make sure rhelBasePackage and rhelOptlPackage are not installed
		if (clienttasks.isPackageInstalled(rhelOptlPackage)) clienttasks.yumRemovePackage(rhelOptlPackage);
		if (clienttasks.isPackageInstalled(rhelBasePackage)) clienttasks.yumRemovePackage(rhelBasePackage);
		
		// manually turn off notify_only 0
		clienttasks.updateConfFileParameter(clienttasks.yumPluginConfFileForSearchDisabledRepos, "notify_only", "0");
		
		// enable rhelOptlRepoId and disable rhelBaseRepoId,rhelEusRepoId
		clienttasks.repos(null, null, null, Arrays.asList(rhelOptlRepoId), Arrays.asList(rhelBaseRepoId,rhelEusRepoId), null, null, null);
		
		// attempt to install a package that requires another package from a disabled repo
		// responding yes and then no
		SSHCommandResult result = client.runCommandAndWait("yum install "+rhelOptlPackage+" --disableplugin=rhnplugin "+" << EOF\ny\nN\nEOF");	// interactive yum responses are:  y y N

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
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(1),"Exit code from attempt to successfully install '"+rhelOptlPackage+"' that requires '"+rhelBasePackage+"'.");
		
		// assert stderr results
		Assert.assertEquals(result.getStderr().trim(),"", "Stderr from attempt to successfully install '"+rhelOptlPackage+"' that requires '"+rhelBasePackage+"'.");

		// assert stdout results
		String prompt;
		String requiresMessage = "Requires: "+rhelBasePackage;
		Assert.assertTrue(result.getStdout().contains(requiresMessage),"Stdout from attempt to install '"+rhelOptlPackage+"' contains the require message:\n"+requiresMessage);
		String searchDisabledReposMessage = String.join("\n",
				"**********************************************************************",
				"Dependency resolving failed due to missing dependencies.",
				"Some repositories on your system are disabled, but yum can enable them",
				"and search for missing dependencies. This will require downloading",
				"metadata for disabled repositories and may take some time and traffic.",
				"**********************************************************************"
				);
		Assert.assertTrue(result.getStdout().contains(searchDisabledReposMessage),"Stdout from attempt to install '"+rhelOptlPackage+"' which requires '"+rhelBasePackage+"' contains the search disabled repos message:\n"+searchDisabledReposMessage);
		prompt = "Enable all repositories and try again? [y/N]: ";
		Assert.assertTrue(result.getStdout().contains(prompt),"Stdout from attempt to install '"+rhelOptlPackage+"' contains the prompt: "+prompt);
		prompt = "Is this ok [y/d/N]: ";
		Assert.assertTrue(result.getStdout().contains(prompt),"Stdout from attempt to install '"+rhelOptlPackage+"' contains the prompt: "+prompt);
		
		// confirm that the packages are NOT installed
		Assert.assertTrue(!clienttasks.isPackageInstalled(rhelOptlPackage),"Package '"+rhelOptlPackage+"' is NOT installed.");
		Assert.assertTrue(!clienttasks.isPackageInstalled(rhelBasePackage),"Package '"+rhelBasePackage+"' is NOT installed.");
		
		// confirm the repo enablement has not changed
		List<Repo> subscribedRepos = clienttasks.getCurrentlySubscribedRepos();
		
		// assert the optional rhel repo remains enabled
		Repo rhelOptlRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelOptlRepoId, subscribedRepos);
		Assert.assertNotNull(rhelOptlRepo, "RHEL optional repo id '"+rhelOptlRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(rhelOptlRepo.enabled, "RHEL optional repo id '"+rhelOptlRepoId+"' remains enabled.");
		
		// assert the base rhel repo enablement remains disabled
		Repo rhelBaseRepo = Repo.findFirstInstanceWithMatchingFieldFromList("repoId", rhelBaseRepoId, subscribedRepos);
		Assert.assertNotNull(rhelBaseRepo, "RHEL repo id '"+rhelBaseRepoId+"' was found in subscribed repos.");
		Assert.assertTrue(!rhelBaseRepo.enabled, "RHEL repo id '"+rhelBaseRepoId+"' remains disabled.");
		
		// assert that repo-overrides remain
		SSHCommandResult listResult = clienttasks.repo_override(true,null,(String)null,(String)null,null,null,null,null);
		for (String repoId : Arrays.asList(rhelBaseRepoId,rhelEusRepoId)) {
			String name = "enabled";
			String value = "0";
			String regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,repoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
			Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+repoId+"' name='"+name+"' value='"+value+"'.");
		}
		// assert that the enabled optional repo remains enabled in the repo-override list
		String name = "enabled";
		String value = "1";
		String regex = String.format(SubscriptionManagerTasks.repoOverrideListRepositoryNameValueRegexFormat,rhelOptlRepoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
		Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After the search-disabled-repos yum plugin was exercised, the subscription-manager repo-override list reports override repo='"+rhelOptlRepoId+"' name='"+name+"' value='"+value+"'.");

	}
	
	
	
	
	// Configuration methods ***********************************************************************
	
/* TODO Commented out until we see what yum supports on RHEL6.8
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
